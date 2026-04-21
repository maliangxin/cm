package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.event.constant.IEventCenterConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business.BankRefundMatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankLinkParam;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.stct.api.openapi.IAccountOpenApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.dto.QueryAccountDetailsParam;
import cn.hutool.core.thread.BlockPolicy;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import lombok.NonNull;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankDealDetailServiceImpl implements BankDealDetailService {
    //企业银行账号查询一页默认返回值
    private static int pageSIze = 4999;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BankDealDetailServiceImpl.class);
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BankDetailRelationSettleService bankDetailRelationSettleService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    //单据智能分类service
    @Resource
    private BillSmartClassifyService billSmartClassifyService;

    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;

    private AtomicInteger cardinalNumber = new AtomicInteger();

    @Autowired
    ICmpSendEventService cmpSendEventService;


    @Autowired
    private BankRefundMatchHandler bankRefundMatchHandler;

    @Value("${bank_delete_event_compare:0}")
    String bank_delete_event_compare;

    // 是否开启流水拉取退票
    static String checkRefundFlag;

    // 接受请求线程池
    static ExecutorService executorServiceAccept;
    static ExecutorService executorService;

    static {
        checkRefundFlag = AppContext.getEnvConfig("check.refund.flag","true");
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.bankdetail.thread.param","8,128,1000,cmp-bankdetail-async-");
        String[] threadParamArray = threadParam.split(",");
        int corePoolSize = Integer.parseInt(threadParamArray[0]);
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix = threadParamArray[3];
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
        String acceptThreadNamePrefix = "cmp-bankdetail-accept-async-";
        executorServiceAccept = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,acceptThreadNamePrefix);
    }

    @Autowired
    BankAccountSettingService bankAccountSettingService;
    private static final @NonNull Cache<String, String> currencyCodeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    /**
     * 20231113 需求
     * 目前农信社完成交易后，同一笔交易会返回临时流水（当日）和真实流水（次日）2笔流水，除了流水号不一致外，其他信息均一致。故依据现有的判重规则，导致系统无法进行判重，需对现有的判重规则进行处理，保证业务数据的唯一性
     * 若为此类特殊情况的银行 银企联云增加全局唯一标识：unique_no 字段，依据该字段进行重复数据的判定
     * 这里存储特殊银行的联行号。按银行分类进行判断，若为特殊银行，则启用新版过滤重复逻辑
     * 若是普通银行 或 unique_no为空的银行，用旧版逻辑
     */
    private static final List<String> LINENUMBER_LIST = new ArrayList<>();


    static {
        LINENUMBER_LIST.add("4027");
    }

    @Override
    public CtmJSONObject queryInnerAccountDetails(CtmJSONObject params) throws Exception {
        String accEntityId = params.getString("accEntity");
        String bankAccountId = params.getString("accountId");
        String uId = params.getString("uid");
        String service_resp_desc = null;
        List<String> bankAccountIds = new ArrayList();
        int pageIndex = params.getInteger("pageIndex") == null ? 1 : params.getInteger("pageIndex");
        bankAccountIds.add(bankAccountId);
        CtmJSONObject responseMsg = new CtmJSONObject();
        //int allDbCount = params.get("allDbCount") == null ? 0 : params.getInteger("allDbCount");
        int allAddCount = params.get(ProcessUtil.ALL_ADD_COUNT) == null ? 0 : params.getInteger(ProcessUtil.ALL_ADD_COUNT);
        int allPullCount = params.get(ProcessUtil.ALL_PULL_COUNT) == null ? 0 : params.getInteger(ProcessUtil.ALL_PULL_COUNT);
        try {
            QueryAccountDetailsParam queryAccountDetailsParam = new QueryAccountDetailsParam();
            queryAccountDetailsParam.setAuthAccentity(accEntityId);
            queryAccountDetailsParam.setSourceAccountIds(bankAccountIds);

            if (params.get(DateUtils.START_DATE_DATE_TYPE) != null) {
                queryAccountDetailsParam.setStartDate((Date) params.get(DateUtils.START_DATE_DATE_TYPE));
            } else {
                queryAccountDetailsParam.setStartDate(DateUtils.dateParse(params.get("startDate").toString().split(" ").length > 1 ? params.get("startDate").toString() : params.get("startDate").toString() + " 00:00:00", DateUtils.DATE_TIME_PATTERN));
            }
            if (params.get(DateUtils.END_DATE_DATE_TYPE) != null) {
                queryAccountDetailsParam.setEndDate(DateUtils.getLastTimeForThisDate((Date) params.get(DateUtils.END_DATE_DATE_TYPE)));
            } else {
                queryAccountDetailsParam.setEndDate(DateUtils.dateParse(params.get("endDate").toString().split(" ").length > 1 ? params.get("endDate").toString() : params.get("endDate").toString() + " 23:59:59", DateUtils.DATE_TIME_PATTERN));
            }

            queryAccountDetailsParam.setPageSize(ITransCodeConstant.QUERY_NUMBER_50);
//            queryAccountDetailsParam.setPageSize(10);//测试用
            queryAccountDetailsParam.setPageIndex(pageIndex);
            queryAccountDetailsParam.setTenantId(AppContext.getTenantId().toString());
            log.error("===========查询内部账户交易明细============请求参数：" + queryAccountDetailsParam.toString());
            Result result = RemoteDubbo.get(IAccountOpenApiService.class, IDomainConstant.MDD_DOMAIN_STCT).queryAccountDetails(queryAccountDetailsParam);
            log.error("===========查询内部账户交易明细============结果：" + CtmJSONObject.toJSON(result).toString());
            if (1 == result.getCode().intValue()) {//成功
                CtmJSONObject data = CtmJSONObject.toJSON(result.getData());
                CtmJSONArray resultLines = data.getJSONArray("resultLines");
                if (CollectionUtils.isNotEmpty(resultLines)) {
                    int localInsertCount = insertAccountDetailForInner(resultLines, params);
                    //allDbCount += localInsertCount;
                    //params.put("allDbCount", allDbCount);
                    allAddCount += localInsertCount;
                    params.put(ProcessUtil.ALL_ADD_COUNT, allAddCount);
                    allPullCount += resultLines.size();
                    params.put(ProcessUtil.ALL_PULL_COUNT, allPullCount);
                    if (data.getInteger("nextPage") == 1) {
                        params.put("pageIndex", pageIndex + 1);
                        params.put("nextPage", true);
                        CtmJSONObject ret = queryInnerAccountDetails(params);
                        //allDbCount += ret.getInteger("allDbCount");
                        allAddCount += localInsertCount;
                        params.put(ProcessUtil.ALL_ADD_COUNT, allAddCount);
                        allPullCount += resultLines.size();
                        params.put(ProcessUtil.ALL_PULL_COUNT, allPullCount);
                    }
                } else {
                    ProcessUtil.addInnerNodataMessage(uId, bankAccountIds, queryAccountDetailsParam.getStartDate(), queryAccountDetailsParam.getEndDate(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080C", "请检查[内部账户明细查询]节点是否有数据") /* "请检查[内部账户明细查询]节点是否有数据" */);
                }
            } else {
                log.error(result.getData().toString());
                ProcessUtil.failAccountNumAddOne(uId);
                //网络请求错误
                service_resp_desc = bankAccountId + result.getData().toString();
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100089"),result.get("data").toString());
            }
        } catch (Exception e) {
            //这里为了适配定时任务 不抛错
            log.error("===========查询内部账户交易明细============错误信息：" + e.getMessage(), e);
            if (null != uId) {
                ProcessUtil.addMessage(uId, e.getMessage());
                ProcessUtil.failAccountNumAddOne(uId);
            }
        } finally {
            if (null != uId) {
                if (service_resp_desc != null) {
                    ProcessUtil.addMessage(uId, service_resp_desc);
                } else {
                    ////要么成功 要么失败 不能一直成功
                    //ProcessUtil.refreshProcess(uId, true);
                }
            }
        }
        responseMsg.put("nextPage", false);
        //responseMsg.put("allDbCount", allDbCount);
        responseMsg.put(ProcessUtil.ALL_ADD_COUNT, allAddCount);
        responseMsg.put(ProcessUtil.ALL_PULL_COUNT, allPullCount);
        return responseMsg;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public int insertAccountDetailForInner(CtmJSONArray records, CtmJSONObject params) throws Exception {
        //存储内部交易明细
        List<BankDealDetail> bankDealDetails = new ArrayList<>();
        //存储对账单
        List<BankReconciliation> bankRecords = new ArrayList<>();
        //校验重复数据
        Map<String, CtmJSONArray> allData = checkRepetitionsForBankseqno(records, params, true);
        records = allData.get("insertData");
        int count = 1;
        if (records.size() < 10) {//这里沿用之前的逻辑 若小于10条则逐条验证
            count = 0;
        }
        //插入交易明细、对账单
        for (int i = 0; i < records.size(); i++) {
            CtmJSONObject detailData = records.getJSONObject(i);
            analysisDetailDataForInner(detailData, params, bankDealDetails, bankRecords, count);
        }
        if (bankDealDetails.size() == 0) {
            return !allData.get("updateData").isEmpty() ? allData.get("updateData").size() : 0;
        }
        CommonSaveUtils.saveBankReconciliation(bankRecords);
        CmpMetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, bankDealDetails);

        // 银行流水支持发送事件消息
        cmpSendEventService.sendEventByBankClaimBatch(bankRecords, EntityStatus.Insert.name());

        //过滤支出数据
        List<BankReconciliation> impRecords = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankRecords) {
            if (Direction.Credit.equals(bankReconciliation.getDc_flag())) {
                impRecords.add(bankReconciliation);
            }
        }
        if (params.getBoolean("isRelationSettle") == null || !params.getBoolean("isRelationSettle")) {//内部交易明细下载后时候直接关联结算
            return bankDealDetails.size() + (!allData.get("updateData").isEmpty() ? allData.get("updateData").size() : 0);
        }
        //事物结束后异步执行
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try {
                    RobotExecutors.runAs(AppContext.getYTenantId(), new Callable() {
                        @Override
                        public Object call() throws Exception {
                            bankDetailRelationSettleService.detailRelationSettle(bankRecords);
                            return null;
                        }
                    }, ctmThreadPoolExecutor.getThreadPoolExecutor());
                } catch (Exception e) {
                    // log.error("业务单据：" + dataSettled.getBusinessbillnum() + "提交失败：" + e.getMessage());
                    throw e;
                }
            }
        });
        //return bankDealDetails.size() + (!allData.get("updateData").isEmpty() ? allData.get("updateData").size() : 0);
        return bankDealDetails.size() ;
    }

    /**
     * @return void
     * @Author tongydc
     * @Description 解析交易明细数据
     * @Date 2021/8/25
     * @Param [detailData, enterpriseInfo, bankDealDetails, bankRecords]
     **/
    private void analysisDetailDataForInner(CtmJSONObject detailData, CtmJSONObject params,
                                            List<BankDealDetail> bankDealDetails,
                                            List<BankReconciliation> bankRecords, int count) throws Exception {
        String bankSeqNo = detailData.getString("bankseqno");
        //若bank_seq_no不存在 则在上面的checkRepetitions方法中无法将这条数据过滤掉可能出现重复问题
        if (bankSeqNo == null || "".equals(bankSeqNo)) {
            count = 0;
        }
        if (count == 0) {
            boolean repetition = checkRepetitionForInner(params.get("accountId"), bankSeqNo, detailData);
            if (repetition) {
                return;
            }
        }
        BankDealDetail detail = new BankDealDetail();
        BankReconciliation bankRecord = new BankReconciliation();
        bankRecord.setInitflag(false);
        bankRecord.setLibraryflag(false);
        bankRecord.setDataOrigin(DateOrigin.DownFromYQL);
        detail.setTenant(AppContext.getTenantId());
        bankRecord.setTenant(AppContext.getTenantId());
        detail.setAccentity((String) params.get("accEntity"));
        // 授权使用组织
//        bankRecord.setAccentity((String) params.get("accEntity"));
        detail.setEnterpriseBankAccount((String) params.get("accountId"));
        bankRecord.setBankaccount((String) params.get("accountId"));
//        Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById((String) params.get("accountId"));
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById((String) params.get("accountId"));
        //银行类别
        bankRecord.setBanktype(enterpriseBankAcctVoWithRange.getBank());
        detail.setBanktype(enterpriseBankAcctVoWithRange.getBank());
        // 所属组织
        bankRecord.setOrgid(enterpriseBankAcctVoWithRange.getOrgid());
        List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
        // 针对内部账户交易明细，直接取值即可
        String authAccentity = detailData.getString("authAccentity");
        if (StringUtils.isNotEmpty(authAccentity)) {
            bankRecord.setAccentity(authAccentity);
            // 授权使用组织确认节点 银行对账单
            bankRecord.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
            // 确认状态 已确认
            bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
        } else if (orgRangeVOS != null && orgRangeVOS.size() > 1) {
            // 授权使用组织 多个
            bankRecord.setAccentity(null);
            // 授权使用组织确认节点
            bankRecord.setConfirmbill(null);
            // 确认状态 待确认
            bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
        } else {
            bankRecord.setAccentity(null);
            // 授权使用组织确认节点
            bankRecord.setConfirmbill(null);
            // 确认状态 待确认
            bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
        }
        String dateStr = detailData.getString("tranDate");
        Date tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
        detail.setTranDate(tranDate);
        bankRecord.setTran_date(tranDate);
        bankRecord.setDzdate(tranDate);
        String timeStr = detailData.getString("tranTime");
        if (StringUtils.isNotEmpty(timeStr)) {
            Date tranTime = DateUtils.dateParse(timeStr, DateUtils.YYYYMMDDHHMMSS);
            detail.setTranTime(tranTime);
            bankRecord.setTran_time(tranTime);
        }
        detail.setBankseqno(bankSeqNo);
        bankRecord.setBank_seq_no(bankSeqNo);
        bankRecord.setThirdserialno(bankSeqNo);
        // 内部流水入库时把交易银行流水号写入unique_no，使用unique_no的唯一索引保证
        detail.setUnique_no(bankSeqNo);
        bankRecord.setUnique_no(bankSeqNo);
        String toAcctNo = detailData.getString("to_acct_no");
        detail.setTo_acct_no(toAcctNo);
        bankRecord.setTo_acct_no(toAcctNo);
        String toAcctName = detailData.getString("to_acct_name");
        detail.setTo_acct_name(toAcctName);
        bankRecord.setTo_acct_name(toAcctName);
        String toAcctBank = detailData.getString("to_acct_bank");
        detail.setTo_acct_bank(toAcctBank);
        bankRecord.setTo_acct_bank(toAcctBank);
        String toAcctBankName = detailData.getString("to_acct_bank_name");
        detail.setTo_acct_bank_name(toAcctBankName);
        bankRecord.setTo_acct_bank_name(toAcctBankName);
        detail.setCurrency(detailData.getString("currency"));
        bankRecord.setCurrency(detailData.getString("currency"));
        BigDecimal acctBal = detailData.getBigDecimal("acctbal");
        //因为银企联限制 这里保留2位小数
        detail.setAcctbal(acctBal.setScale(2, BigDecimal.ROUND_HALF_UP));
        bankRecord.setAcct_bal(acctBal);
        BigDecimal tranAmt = detailData.getBigDecimal("tran_amt");
        detail.setTran_amt(tranAmt);
        bankRecord.setTran_amt(tranAmt);
        String dcFlag = detailData.getString("dc_flag");
        if ("d".equalsIgnoreCase(dcFlag)) {
            detail.setDc_flag(Direction.Debit);
            bankRecord.setDc_flag(Direction.Debit);
            bankRecord.setDebitamount(tranAmt);
        } else if ("c".equalsIgnoreCase(dcFlag)) {
            detail.setDc_flag(Direction.Credit);
            bankRecord.setDc_flag(Direction.Credit);
            bankRecord.setCreditamount(tranAmt);
        }
        String remark = detailData.getString("remark");
        String remark01 = detailData.getString("remark01");
        detail.setRemark(remark);
        bankRecord.setRemark(remark);
        bankRecord.setRemark01(remark01);
        // "treasuryReconciliationCode":"_awqciuc_"   "ballowGenBill":true
        // 设置财资统一码
        bankRecord.setSmartcheckno(detailData.getString("treasuryReconciliationCode"));
        if (detailData.getString("ballowGenBill") == null) {
            bankRecord.setIsparsesmartcheckno(false);
        } else {
            // 是否解析出财资统一码  是否允许生单 对应关系是反的，所以进行返回值转化，统一后续判断处理
            bankRecord.setIsparsesmartcheckno(!BooleanUtils.toBoolean(detailData.getString("ballowGenBill")));
        }
        if (StringUtils.isNotEmpty(bankRecord.getAccentity()) && StringUtils.isNotEmpty(detailData.getString("project"))
                && CmpCommonUtil.checkProject(detailData.getString("project"), bankRecord.getAccentity())) {
            bankRecord.setProject(detailData.getString("project"));
        }
        if (StringUtils.isNotEmpty(bankRecord.getAccentity()) && StringUtils.isNotEmpty(detailData.getString("expenseItem"))
                && CmpCommonUtil.checkExpenseitem(detailData.getString("expenseItem"), bankRecord.getAccentity())) {
            bankRecord.setExpenseItem(detailData.getLong("expenseItem"));
        }
        detail.setIsaccountflag(true);
        detail.setEntityStatus(EntityStatus.Insert);
        long id = ymsOidGenerator.nextId();
        detail.setId(id);
        detail.setProject(detailData.getString("project"));
        detail.setExpenseItem(detailData.getLong("expenseItem"));
        bankRecord.setEntityStatus(EntityStatus.Insert);
        bankRecord.setId(id);
        bankRecord.setYtenantId(InvocationInfoProxy.getTenantid().toString());
        // 添加创建时间、创建日期
        detail.setCreateDate(new Date());
        detail.setCreateTime(new Date());
        // 代理银行账户
        detail.setAgentBankAccount(detailData.getString("agentBankAccount"));
        bankRecord.setCreateDate(new Date());
        bankRecord.setCreateTime(new Date());
        // 对方类型
        bankRecord.setOppositetype(this.dealStctOppositeType(detailData.getString("counterpartyType")));
        // 对方单位Id
        bankRecord.setOppositeobjectid(detailData.get("oppId") == null ? "" : detailData.getString("oppId"));
        // 对方单位名称
        bankRecord.setOppositeobjectname(detailData.getString("oppName"));
        bankRecord.setUnique_no(bankSeqNo);
        detail.setUnique_no(bankSeqNo);
        CtmCmpCheckRepeatDataService checkRepeatDataService = AppContext.getBean(CtmCmpCheckRepeatDataService.class);
        checkRepeatDataService.fillBankReconciliationConcatInfo(bankRecord);
        bankDealDetails.add(detail);
        bankRecords.add(bankRecord);
    }

    /**
     * 处理结算中心的对方类型与现金管理的对方类型的映射
     *
     * @param counterpartyType 对方类型
     * @return
     */
    private Short dealStctOppositeType(String counterpartyType) {
        // 内部结息场景 对方类型为空对方类型为其他
        if (StringUtils.isEmpty(counterpartyType)) {
            return OppositeType.Other.getValue();
        }
        Short oppositeType = null;
        switch (counterpartyType) {
            // 客户
            case "1":
                oppositeType = OppositeType.Customer.getValue();
                break;
            // 供应商
            case "2":
                oppositeType = OppositeType.Supplier.getValue();
                break;
            // 员工
            case "3":
                oppositeType = OppositeType.Employee.getValue();
                break;
            // 其他
            case "4":
                oppositeType = OppositeType.Other.getValue();
                break;
            // 资金业务对象
            case "5":
                oppositeType = OppositeType.CapBizObj.getValue();
                break;
            // 内部单位
            case "6":
                oppositeType = OppositeType.InnerOrg.getValue();
                break;
            default:
                log.error("传入的对方类型:{}不存在", counterpartyType);
                break;

        }
        return oppositeType;
    }

    private Map<String, CtmJSONArray> checkRepetitionsForBankseqno(CtmJSONArray records, Map<String, Object> enterpriseInfo, Boolean isInner) throws Exception {
        //内部账户 和 直连账户用不同的关键字过滤
        String bank_seq_no = "bank_seq_no";
        if (isInner) {
            bank_seq_no = "bankseqno";
        }
        List<Map<String, Object>> details = queryDetailsForCheck(enterpriseInfo, isInner);
        CtmJSONArray newRecords = new CtmJSONArray();
        CtmJSONArray updateRecords = new CtmJSONArray();
        Map<String, CtmJSONArray> returnMap = new HashMap<>();
        for (int i = 0; i < records.size(); i++) {
            Boolean flag = false;
            for (Map<String, Object> obj : details) {
                if (records.getJSONObject(i).getString(bank_seq_no) != null && obj.get("bankseqno") != null && records.getJSONObject(i).getString(bank_seq_no).equals(obj.get("bankseqno").toString())) {
                    //CZFW-104041 若余额信息为空，则更新该交易明细新抓取的余额信息 add by wxb
                    boolean updateflag = handleUpdateAccbal(obj, enterpriseInfo.get("accountId"), records.getJSONObject(i), isInner);
                    if (updateflag) {
                        updateRecords.add(records.getJSONObject(i));
                    }
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                newRecords.add(records.getJSONObject(i));
            }
        }
        returnMap.put("insertData", newRecords);
        returnMap.put("updateData", updateRecords);
        return returnMap;
    }

    private List<Map<String, Object>> queryDetailsForCheck(Map<String, Object> enterpriseInfo, Boolean isInner) throws Exception {
        //内部账户 和 直连账户用不同的关键字过滤
        String accEntityId = "accEntityId";
        if (isInner) {
            accEntityId = "accEntity";
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq((String) enterpriseInfo.get("accountId")));
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq((String) enterpriseInfo.get(accEntityId)));
        schema.addCondition(conditionGroup);
        //转换前段传递的日期格式(根据控件不同会有变化)
        SimpleDateFormat format = new SimpleDateFormat(DateUtils.DATE_PATTERN);
        if (enterpriseInfo.get("startDate") != null) {
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(format.parse(enterpriseInfo.get("startDate").toString()))));
        }
        if (enterpriseInfo.get("endDate") != null) {
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(format.parse(enterpriseInfo.get("endDate").toString()))));
        }
        List<Map<String, Object>> details = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME, schema);

        return details;
    }

    private boolean checkRepetitionForInner(Object bankAccountId, String bankSeqNo, CtmJSONObject detailData) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        conditionGroup.appendCondition(QueryCondition.name("isaccountflag").eq(1));
        if (StringUtils.isNotEmpty(bankSeqNo)) {
            conditionGroup.appendCondition(QueryCondition.name("bankseqno").eq(bankSeqNo));
        } else {//若银行交易流水号为空 走另一套判断逻辑
            String tran_time = detailData.getString("trantime");
            String tran_date = detailData.getString("trandate");
            Date checktranTime = DateUtils.dateParse(tran_date + tran_time, DateUtils.YYYYMMDDHHMMSS);
            conditionGroup.appendCondition(QueryCondition.name("tranTime").eq(checktranTime));
            if (detailData.getString("tran_amt") != null) {//交易金额
                conditionGroup.appendCondition(QueryCondition.name("tran_amt").eq(detailData.getBigDecimal("tran_amt")));
            }
            if (detailData.getString("acctbal") != null) {//摘要
                conditionGroup.appendCondition(QueryCondition.name("acctbal").eq(detailData.getString("acctbal")));
            }
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> details = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME, schema);
        if (CollectionUtils.isNotEmpty(details)) {
            //CZFW-104041 若余额信息为空，则更新该交易明细新抓取的余额信息 add by wxb,只更新第一条数据，若存在多条则为脏数据
            handleUpdateAccbal(details.get(0), bankAccountId, detailData, true);
            return true;
        }
        return false;
    }

    /**
     * 处理更新余额信息为空，重新拉取时不为空的数据
     */
    private boolean handleUpdateAccbal(Map<String, Object> detail, Object bankAccountId, CtmJSONObject detailData, Boolean isInner) {
        Boolean updateFlag = false;//更新标识 默认为false 如果一下情况有任意需要更新 则将更新标志置为true
        try {
            BankDealDetail bankDealDetail = new BankDealDetail();
            bankDealDetail.init(detail);
            //1.在唯一流水不为空的情况下 且相同的情况下
            String bank_seq_no = detailData.getString("bank_seq_no");
            if (isInner) {
                bank_seq_no = detailData.getString("bankseqno");
            }
            if (bankDealDetail.getUnique_no() != null && bankDealDetail.getUnique_no().equals(detailData.getString(ITransCodeConstant.UNIQUE_NO))) {
                /**
                 * 如果能找到交易明细unique_no一致的交易明细，则继续判断历史交易流水号：bank_seq_no是否一致：
                 * 如果交易流水号：bank_seq_no一致，不做处理；
                 * 如果交易流水号：bank_seq_no不一致，需用unique_no相同的新交易明细更新原有交易明细，并且用新获取数据的交易流水号：bank_seq_no,银行对账明细bank_check_code，更新原有数据的bank_seq_no,bank_check_code。
                 */
                if (bankDealDetail.getBankseqno() != null && !bankDealDetail.getBankseqno().equals(bank_seq_no)) {
                    bankDealDetail.setBankseqno(bank_seq_no);
                    bankDealDetail.setBankcheckno(detailData.getString("bank_check_code"));
                    updateFlag = true;
                }
            }
            //2.若当前余额为空，且新拉取余额不为空，则更新银行交易明细和对应银行对账单
            BigDecimal acctBal = null;
            if (isInner) {
                acctBal = detailData.getBigDecimal("acctbal");
            } else {
                acctBal = detailData.getBigDecimal("acct_bal");
            }
            if (!BigDecimalUtils.isEqual(bankDealDetail.getAcctbal(), acctBal)) {
                bankDealDetail.setAcctbal(acctBal);
                updateFlag = true;
            }
            if (updateFlag) {
                bankDealDetail.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankDealDetail);
                //查询相关银行对账单
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
                group.addCondition(QueryCondition.name("bankaccount").eq(bankAccountId));
                if (isInner) {
                    //查询相关银行对账单
                    group.addCondition(QueryCondition.name("bank_seq_no").eq(bankDealDetail.getBankseqno()));
                } else {
                    //查询相关银行对账单
                    group.addCondition(QueryCondition.name("id").eq(bankDealDetail.getId().toString()));
                }
                querySchema.addCondition(group);
                List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isNotEmpty(bankReconciliations)) {
                    for (BankReconciliation b : bankReconciliations) {
                        if (b.getDataOrigin().getValue() == DateOrigin.DownFromYQL.getValue()) {
                            b.setAcct_bal(acctBal);
                        }
                        if (b.getDataOrigin().getValue() == DateOrigin.DownFromYQL.getValue()) {
                            b.setBank_seq_no(bank_seq_no);
                        }
                        b.setEntityStatus(EntityStatus.Update);
                    }
                    CommonSaveUtils.updateBankReconciliation(bankReconciliations);
                }
            }
        } catch (Exception e) {
            log.error("银行账户交易明细更细余额报错：" + e.getMessage());
        }
        return updateFlag;
    }

    /**
     * 用于直联联账户交易明细查询条件拼接梳理 通过前端条件查询相关银行账户
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<EnterpriseBankAcctVO> getEnterpriseBankAccountVos(CtmJSONObject params) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        // start wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        String accEntity = params.getString("accEntity");
        List<String> accounts = new ArrayList<>();
        if (accEntity != null) {
            List<String> accentitys = Arrays.asList(accEntity.split(","));
            //判断前段数据是否为多选
            if (accEntity.contains("[")) {
                accentitys = params.getObject("accEntity", List.class);
            }
            if (accentitys != null && !accentitys.isEmpty()) {
//                enterpriseParams.setOrgidList(accentitys);
                // 根据所选组织查询 有权限的账户
                EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                newEnterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(newEnterpriseParams);
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                    accounts.add(enterpriseBankAcctVO.getId());
                }
            }
        }
        // end wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        // 银行账户
        List<String> enterBankAccs = params.getObject("accountId", List.class);
        //如果是智能员工，则只能查询当日的交易明细
        if (StringUtils.isNotEmpty(params.getString("skillId"))) {
            params.put("endDate", DateUtils.getTodayShort());
            params.put("startDate", DateUtils.getTodayShort());
        }

        if (params.get("startDate") != null && params.get("endDate") != null) {
            //开始时间
            Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(params.get("startDate").toString());
            //结束时间
            Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(params.get("endDate").toString());
            int days = DateUtils.dateBetween(startDate, endDate);
            if (days > 31) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100090"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000C", "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!") /* "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!" */);
            }
        }
        String currency = params.getString("currency");
        List<String> currencyids = new ArrayList<>();
        if (currency != null && currency.contains("[")) {
            currencyids = params.getObject("currency", List.class);
        } else if (currency != null) {
            currencyids.add(currency);
        }
        if (StringUtils.isNotEmpty(currency)) {
            enterpriseParams.setCurrencyIDList(currencyids);
        }

        if (enterBankAccs != null && !enterBankAccs.isEmpty()) {
            enterpriseParams.setIdList(enterBankAccs);
        } else {
            enterpriseParams.setIdList(accounts);
        }
        //调度任务传入银行类别(调度任务使用 手动拉取不用)
        String bankType = params.getString("bankType");
        List<String> bankTypes = new ArrayList<>();
        if (bankType != null && bankType.contains("[")) {
            bankTypes = params.getObject("bankType", List.class);
        } else if (bankType != null) {
            bankTypes.add(bankType);
        }
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        //由于账户提供的接口参数 银行类别为string，所以当传入多个银行类别时候需要循环查询
        if (bankTypes != null && !bankTypes.isEmpty()) {
            for (String bank : bankTypes) {
                enterpriseParams.setBank(bank);
                bankAccounts.addAll(enterpriseBankQueryService.queryAll(enterpriseParams));
            }
        } else
            bankAccounts.addAll(enterpriseBankQueryService.queryAll(enterpriseParams));
        return bankAccounts;
    }

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     *
     * @param bankAccounts
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, List<EnterpriseBankAcctVO>> getBankAcctVOsGroup(List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        //直联账户
        List<EnterpriseBankAcctVO> checkSuccess = new ArrayList<>();
        //结算中心 内部账户
        List<EnterpriseBankAcctVO> innerAccounts = new ArrayList<>();
        List<EnterpriseBankAcctVO> failAccounts = new ArrayList<>();

        List<String> bankaccountSettingAccountId = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankAccounts)) {
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                bankaccountSettingAccountId.add(enterpriseBankAcctVO.getId());
                if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
                    innerAccounts.add(enterpriseBankAcctVO);
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100091"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050015", "选择的资金组织，尚未维护银行账户，请检查!") /* "选择的资金组织，尚未维护银行账户，请检查!" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(bankaccountSettingAccountId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if (settings != null && settings.size() > 0) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100092"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000D","选择的会计主体，尚未维护银行账户，请检查!"));
            for (Map<String, Object> map : settings) {
                for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                    if (map.get("enterpriseBankAccount").toString().equals(enterpriseBankAcctVO.getId())) {
                        if (StringUtils.isNotEmpty(map.get("customNo") != null ? map.get("customNo").toString() : null)) {
                            checkSuccess.add(enterpriseBankAcctVO);
                            break;
                        } else {
                            failAccounts.add(enterpriseBankAcctVO);
                            break;
                        }
                    }
                }
            }
        }
        if (checkSuccess.size() == 0 && innerAccounts.size() == 0) {
            if (failAccounts.size() != 0) {
                StringBuilder failNames = new StringBuilder("");
                for (int i = 0; i < failAccounts.size(); i++) {
                    if (i == failAccounts.size() - 1) {
                        failNames.append(failAccounts.get(i).getName());
                    } else {
                        failNames.append(failAccounts.get(i).getName());
                        failNames.append(",");
                    }
                }
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400810", "无法获取企业银行账户相关联的银企联客户号，请在账户直联状态节点维护") /* "无法获取企业银行账户相关联的银企联客户号，请在账户直联状态节点维护" */);
            }
            StringBuilder accountNames = new StringBuilder("");
            for (int i = 0; i < bankAccounts.size(); i++) {
                if (i == bankAccounts.size() - 1) {
                    accountNames.append(bankAccounts.get(i).getName());
                } else {
                    accountNames.append(bankAccounts.get(i).getName());
                    accountNames.append(",");
                }
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA", "【") /* "【" */ + accountNames + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D3", "】的企业银行账户没有开通银企联，无法查询银行账户交易明细") /* "】的企业银行账户没有开通银企联，无法查询银行账户交易明细" */);
        }
        Map<String, List<EnterpriseBankAcctVO>> resultMap = new HashMap<>();
        resultMap.put("innerAccounts", innerAccounts);
        resultMap.put("checkSuccess", checkSuccess);
        return resultMap;
    }

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     *
     * @param bankAccounts
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, List<EnterpriseBankAcctVO>> getBankAcctVOsGroupByTask(List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        //直联账户
        List<EnterpriseBankAcctVO> checkSuccess = new ArrayList<>();
        //结算中心 内部账户
        List<EnterpriseBankAcctVO> innerAccounts = new ArrayList<>();
        List<EnterpriseBankAcctVO> failAccounts = new ArrayList<>();

        List<String> bankaccountSettingAccountId = new ArrayList<>();
        if (bankAccounts != null && bankAccounts.size() > 0) {
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                bankaccountSettingAccountId.add(enterpriseBankAcctVO.getId());
                if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
                    innerAccounts.add(enterpriseBankAcctVO);
                }
            }
        } else {
            throw new CtmException(CtmExceptionConstant.DISPATCH_ACCOUNT_NOT_EXIST);
        }
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(bankaccountSettingAccountId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if (settings != null && settings.size() > 0) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100092"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000D","选择的会计主体，尚未维护银行账户，请检查!"));
            for (Map<String, Object> map : settings) {
                for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                    if (map.get("enterpriseBankAccount").toString().equals(enterpriseBankAcctVO.getId())) {
                        if (StringUtils.isNotEmpty(map.get("customNo") != null ? map.get("customNo").toString() : null)) {
                            checkSuccess.add(enterpriseBankAcctVO);
                            break;
                        } else {
                            failAccounts.add(enterpriseBankAcctVO);
                            break;
                        }
                    }
                }
            }
        }
        if (checkSuccess.size() == 0 && innerAccounts.size() == 0) {
            if (failAccounts.size() != 0) {
                StringBuilder failNames = new StringBuilder("");
                for (int i = 0; i < failAccounts.size(); i++) {
                    if (i == failAccounts.size() - 1) {
                        failNames.append(failAccounts.get(i).getAccount());
                    } else {
                        failNames.append(failAccounts.get(i).getAccount());
                        failNames.append(",");
                    }
                }
                throw new CtmException(String.format(EnterpriseBankQueryService.noCustomNoMessageWithAcct_no,failNames));
            }
            StringBuilder accountNames = new StringBuilder("");
            for (int i = 0; i < bankAccounts.size(); i++) {
                if (i == bankAccounts.size() - 1) {
                    accountNames.append(bankAccounts.get(i).getName());
                } else {
                    accountNames.append(bankAccounts.get(i).getName());
                    accountNames.append(",");
                }
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA", "【") /* "【" */ + accountNames + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D3", "】的企业银行账户没有开通银企联，无法查询银行账户交易明细") /* "】的企业银行账户没有开通银企联，无法查询银行账户交易明细" */);
        }
        Map<String, List<EnterpriseBankAcctVO>> resultMap = new HashMap<>();
        resultMap.put("innerAccounts", innerAccounts);
        resultMap.put("checkSuccess", checkSuccess);
        return resultMap;
    }

    @Override
    public void queryBankAccountTransactionDetail(CtmJSONObject params, List<Map<String, Object>> excuteList, CtmJSONObject responseMsg, String uid, int batchcount, int totalTask) throws Exception {
        try {
            Set<String> failAccountSet = ConcurrentHashMap.newKeySet();
            ThreadPoolUtil.executeByBatchNotShutDown(executorService, excuteList, batchcount, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080F", "查交易明细") /* "查交易明细" */, (int fromIndex, int toIndex) -> {
                String builder = "";
                for (int t = fromIndex; t < toIndex; t++) {
                    Map<String, Object> enterpriseBankAcctVO = excuteList.get(t);
                    String accountId = (String) enterpriseBankAcctVO.get(ProcessUtil.ACCOUNT_ID);
                    // 加锁的账号信息
                    String accountInfo = enterpriseBankAcctVO.get("acct_no") + "|" + enterpriseBankAcctVO.get("startDate") + "|" + enterpriseBankAcctVO.get("currencyCode");
                    // 加锁信息：账号+行为
                    String lockKey = accountInfo + ICmpConstant.QUERYTRANSDETAILKEY;
                    String accountId_currencyCode_key = accountId + "|" + enterpriseBankAcctVO.get("currencyCode");
                    try {
                        CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                                //加锁失败添加报错信息 并把进度刷新
                                ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                                ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080B", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfo));
                                return;
                            }
                            // 加锁成功，直连拉取数据并进行验重和入库
                            CtmJSONObject paramNow = new CtmJSONObject(params);
                            String customNo = getCustomNo(enterpriseBankAcctVO.get("accountId"));
                            exuteHttpQueryDetail(paramNow, enterpriseBankAcctVO, responseMsg, customNo, uid, failAccountSet);
                        });
                    } catch (Exception e) {
                        log.error("queryBankAccountTransactionDetail", e);
                        ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                        ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080E", "[%s]:此账户操作发生异常:") /* "[%s]:此账户操作发生异常:" */, accountInfo) + e.getMessage());
                    }
                }
                return builder;
            });
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180664", "查询成功") /* "查询成功" */);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 实际处理HTTP请求的方法
     *
     * @param params
     * @param enterpriseBankAcctVO
     * @param responseMsg
     * @param customNo
     * @param failAccountSet
     * @throws Exception
     */
    private void exuteHttpQueryDetail(CtmJSONObject params, Map<String, Object> enterpriseBankAcctVO, CtmJSONObject responseMsg, String customNo, String uid, Set<String> failAccountSet) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        String errorInfo = null;
        CtmJSONObject queryMsg = new CtmJSONObject();
        CtmJSONObject result = new CtmJSONObject();
        //int allDbCount = 0;
        int allAddCount = params.get(ProcessUtil.ALL_ADD_COUNT) == null ? 0 : params.getInteger(ProcessUtil.ALL_ADD_COUNT);
        int allPullCount = params.get(ProcessUtil.ALL_PULL_COUNT) == null ? 0 : params.getInteger(ProcessUtil.ALL_PULL_COUNT);
        //若非内部账户 则为引企联账户，不存在同一账户有两种属性的情况，故这里抛错不受影响
        String accountId = enterpriseBankAcctVO.get("accountId").toString();
        String accountId_currencyCode_key = enterpriseBankAcctVO.get("acct_no") + "|" + enterpriseBankAcctVO.get("currencyCode");
        try {
            params.put("customNo", customNo);
            params.put("signature", "");
            params.put("currencyCode", enterpriseBankAcctVO.get("currencyCode").toString());

            Map<String, Object> enterpriseInfo = new HashMap<>();
            enterpriseInfo.put("accEntityId", enterpriseBankAcctVO.get("accEntityId").toString());
            enterpriseInfo.put("accountId", accountId);
            enterpriseInfo.put("startDate", enterpriseBankAcctVO.get("startDate"));
            enterpriseInfo.put("endDate", enterpriseBankAcctVO.get("endDate"));
            enterpriseInfo.put("banktype", enterpriseBankAcctVO.get("banktype"));
            enterpriseInfo.put("account", enterpriseBankAcctVO.get("acct_no"));
            params.put(YQLUtils.ACCT_NO , enterpriseBankAcctVO.get("acct_no"));
            enterpriseInfo.put("lineNumber", enterpriseBankAcctVO.get("lineNumber"));

            queryMsg = buildQueryMsgNew(params, enterpriseBankAcctVO);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("===================>交易明细无Ukey开始拉取" + CtmJSONObject.toJSONString(requestData));
            String chanPayUri = bankConnectionAdapterContext.getChanPayUri();
            String transCode = ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL;
            result = HttpsUtils.doHttpsPostNew(transCode, requestData, chanPayUri);
            HttpsUtils.saveYQLBusinessLog(transCode, chanPayUri, queryMsg, result, params, Objects.toString(enterpriseBankAcctVO.get("acct_no"), null));
            log.error("===================>交易明细无Ukey拉取结束" + CtmJSONObject.toJSONString(result));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(transCode, service_resp_code)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    int backNum = responseBody.getInteger("back_num");
                    if (backNum > 1) {
                        records = responseBody.getJSONArray("record");
                    }
                    int loaclInsertCount = insertTransactionDetail(enterpriseInfo, responseBody);
                    //allDbCount += loaclInsertCount;
                    allAddCount += loaclInsertCount;
                    params.put(ProcessUtil.ALL_ADD_COUNT, allAddCount);
                    allPullCount += backNum;
                    params.put(ProcessUtil.ALL_PULL_COUNT, allPullCount);
                    String nextPage = responseBody.getString("next_page");
                    //前端手动拉取交易明细如果存在下一页直接循环拉取直到结束不在给前端返回nextPage为true进行递归调用
                    while ("1".equals(nextPage)) {
                        int begNum = params.getInteger("begNum");
                        params.put("begNum", begNum + ITransCodeConstant.QUERY_NUMBER_50);
                        params.put("queryExtend", responseBody.get("query_extend"));
                        queryMsg = buildQueryMsgNew(params, enterpriseBankAcctVO);
                        signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
                        requestData = new ArrayList<>();
                        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
                        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
                        log.error("===================>交易明细无Ukey开始拉取存在下一页begNum:" + params.get("begNum") + requestData);
                        result = HttpsUtils.doHttpsPostNew(transCode, requestData, chanPayUri);
                        HttpsUtils.saveYQLBusinessLog(transCode, chanPayUri, queryMsg, result, params, Objects.toString(enterpriseBankAcctVO.get("acct_no"), null));
                        log.error("===================>交易明细无Ukey拉取结束存在下一页" + result);
                        if (HttpServiceInforamtionUtils.httpSuccessByRespCode(transCode, service_resp_code)) {
                            responseBody = result.getJSONObject("data").getJSONObject("response_body");
                            backNum = responseBody.getInteger("back_num");
                            if (backNum > 1) {
                                records = responseBody.getJSONArray("record");
                            }
                            loaclInsertCount = insertTransactionDetail(enterpriseInfo, responseBody);
                            //allDbCount += loaclInsertCount;
                            allAddCount += loaclInsertCount;
                            params.put(ProcessUtil.ALL_ADD_COUNT, allAddCount);
                            allPullCount += backNum;
                            params.put(ProcessUtil.ALL_PULL_COUNT, allPullCount);
                            nextPage = responseBody.getString("next_page");
                            params.put("queryExtend", responseBody.get("query_extend"));
                        }else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                            // 解决多线程下的并发问题
                            ProcessUtil.dealYQLNodataMessage(params, uid, failAccountSet, accountId_currencyCode_key, responseHead);
                            nextPage = "0";
                        } else {
                            nextPage = "0";
                        }
                    }
                    params.put("queryExtend", "");
                    params.put("nextPage", false);
                } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                    // 解决多线程下的并发问题
                    ProcessUtil.dealYQLNodataMessage(params, uid, failAccountSet, accountId_currencyCode_key, responseHead);
                } else {
                    //仅在某账户查询错误时 进行提示信息
                    errorInfo = YQLUtils.getYQLErrorMsqForManual(enterpriseBankAcctVO, responseHead);
                }
            } else{
                    errorInfo = YQLUtils.getYQLErrorMsqOfNetWork(result.getString("message"));
            }
        } catch (Exception e) {
            log.error(String.format("queryAccountBalanceForHttpTask-error，请求参数 = %s,响应参数 = %s,报错信息 = %s,对应账号 = %s", queryMsg, result, e.getMessage(), enterpriseBankAcctVO.get("acct_no")), e);
            errorInfo = YQLUtils.getErrorMsqWithAccount(e, String.valueOf(enterpriseBankAcctVO.get("acct_no")));
        } finally {
            if (errorInfo != null) {
                ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                ProcessUtil.addYQLErrorMessage(uid, errorInfo);
            } else {//要么成功 要么失败 不能一直成功
                //ProcessUtil.refreshProcess(uid, true);
            }
        }
        responseMsg.put("nextPage", false);
        ProcessUtil.newAddCountAdd(uid, (Integer) params.getOrDefault(ProcessUtil.ALL_ADD_COUNT, 0));
        ProcessUtil.totalPullCountAdd(uid, (Integer) params.getOrDefault(ProcessUtil.ALL_PULL_COUNT, 0));
    }

    /*
     *@Author wsl
     *@Description 从银企联设置中获取客户号
     *@Date 2019/6/4 11:29
     *@Param [enterpriseBankAccount]
     *@Return boolean
     **/
    private String getCustomNo(Object enterpriseBankAccount) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (setting != null && setting.size() > 0) {
            return (String) setting.get("customNo");
        }
        return "";
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建查询账户交易明细报文
     * @Date 2019/8/7
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildQueryMsgNew(CtmJSONObject params, Map<String, Object> bankAccount) throws Exception {
        CtmJSONObject requestHead = buildRequestHeadNew(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL,
                params.getString("operator"),
                params.getString("customNo"),
                buildRequestSeqNo(params.getString("customNo")),
                params.getString("signature"),
                bankAccount.get("channel") != null ? bankAccount.get("channel").toString() : null);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_name", bankAccount.get("acct_name"));
        requestBody.put("acct_no", bankAccount.get("acct_no"));

        requestBody.put("curr_code", params.getString("currencyCode"));
        String startDate = bankAccount.get("startDate").toString().replaceAll("-", "").substring(0, 8);
        String endDate = bankAccount.get("endDate").toString().replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", params.get("begNum") == null ? 1 : params.get("begNum"));
        requestBody.put("query_num", ITransCodeConstant.QUERY_NUMBER_50);
        requestBody.put("query_extend", params.get("queryExtend"));
        requestBody.put("bank_country_code", params.get("bank_country_code"));
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建请求报文头
     * @Date 2019/8/19
     * @Param [transCode, customNo, requestseqno, signature]
     **/
    private CtmJSONObject buildRequestHeadNew(String transCode, String operator, String customNo, String requestseqno, String signature, String channel) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", channel != null ? channel : bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        return requestHead;
    }

    /*
     * @Author tongyd
     * @Description 构建请求流水号
     * @Date 2019/9/12
     * @Param [customNo]
     * @return java.lang.String
     **/
    public String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    @Override
    public HashMap<String, String> queryCurrencyCode(List<BankAcctCurrencyVO> currencyList) throws Exception {
        HashMap<String, String> currencyMap = new HashMap<>();
        List<String> currencyIds = currencyList.stream().map(e -> e.getCurrency()).collect(Collectors.toList());
        List<String> ids = new ArrayList<>();
        for (String currency : currencyIds) {
            String currencyCode = currencyCodeCache.getIfPresent(currency);
            if (StringUtils.isEmpty(currencyCode)) {
                ids.add(currency);
            } else {
                currencyMap.put(currency, currencyCode);
            }
        }
        if (ids.size() == 0) {
            return currencyMap;
        }
        CurrencyBdParams params = new CurrencyBdParams();
        if (!ids.isEmpty()) {
            params.setIdList(ids);
        }
        List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(params);
        Map<String, String> map = currencylist.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getCode()));
        for (CurrencyTenantDTO item : currencylist) {
            currencyCodeCache.put(item.getId(), item.getCode());
        }
        currencyMap.putAll(map);
        return currencyMap;
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 插入交易明细数据
     * @Date 2019/8/7
     * @Param [enterpriseInfo, responseBody]
     **/
    @Transactional(rollbackFor = RuntimeException.class)
    public int insertTransactionDetail(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody) throws Exception {
        List<BankDealDetail> bankDealDetails = new ArrayList<>();
        List<BankReconciliation> bankRecords = new ArrayList<>();
        int backNum = responseBody.getInteger("back_num");
        //由于大部分数据都为人民币 这里先查询提升效率
        String currency = currencyQueryService.getCurrencyByAccount((String) enterpriseInfo.get("accountId"));
        if (backNum == 1) {
            CtmJSONObject detailData = responseBody.getJSONObject("record");
            analysisDetailData(detailData, enterpriseInfo, bankDealDetails, bankRecords, currency);
        } else if (backNum > 1) {
            CtmJSONArray records = responseBody.getJSONArray("record");
            for (int i = 0; i < records.size(); i++) {
                CtmJSONObject detailData = records.getJSONObject(i);
                analysisDetailData(detailData, enterpriseInfo, bankDealDetails, bankRecords, currency);
            }
        }

        if (bankDealDetails.isEmpty()) {
            return 0;
        }
        Long startTime = System.currentTimeMillis();
        CtmCmpCheckRepeatDataService checkRepeatDataService = CtmAppContext.getBean(CtmCmpCheckRepeatDataService.class);
        Map<String, List<BankDealDetail>> allmap = checkRepeatDataService.checkBankDealDetailRepeat(bankDealDetails);
        bankDealDetails = allmap.get("insertData");
        //Set<String> concatInfoDefines = new HashSet<>();
        Long endTime = System.currentTimeMillis();
        log.error("=================================checkRepeatDataService=====耗时：" + (endTime - startTime) + "ms======");
        bankRecords = checkRepeatDataService.checkRepeatData(bankRecords, EventType.CashMark.getValue());
        log.error("=================================checkRepeatDataService2=====耗时：" + (System.currentTimeMillis() - endTime) + "ms======");
        //使用去重后的交易明细，将银行对账单去重
        HashMap<String, BankReconciliation> bankMap = new HashMap<>();
        for (BankReconciliation bank : bankRecords) {
            bankMap.put(bank.getId().toString(), bank);
        }
        //非重复的入库数据
        List<BankReconciliation> newBankRecords = new ArrayList<>();
        //过滤支出数据
        List<BankReconciliation> impRecords = new ArrayList<>();
        for (BankDealDetail detail : bankDealDetails) {
            BankReconciliation newBank = bankMap.get(detail.getId().toString());
            // 银行对账单 uniqueno
            if (newBank != null) {
                if (StringUtils.isEmpty(newBank.getUnique_no())) {
                    newBank.setUnique_no(detail.getUnique_no());
                }
                newBankRecords.add(newBank);
                if (Direction.Credit.equals(newBank.getDc_flag())) {
                    impRecords.add(newBank);
                }
            }
        }
        // 智能辨识 银行对账单对方类型
        smartClassifyBatchProcess(newBankRecords);

        //// 退票辨识  RPT0210退票辨识优化
        //bankReconciliationReFundService.checkRefundBankReconciliation(newBankRecords);
        //调用郭扬提供的新方法
        // 流水拉取是否进行退票辨识按照参数配置，默认true
        if (checkRefundFlag.equals("true")) {
            newBankRecords = bankRefundMatchHandler.bankRefundMatchHandler(newBankRecords);
        }
        // 4要素疑重处理
        checkRepeatDataService.deal4FactorsBankDealDetail(newBankRecords, enterpriseInfo);
        CommonSaveUtils.saveBankReconciliation(newBankRecords);
        // 根据银行流水记录删除账户交易流水中的无效数据，保证数据一致
        removedBankDealDetail(newBankRecords, bankDealDetails);
        // 先更新银行流水表，再更新账户交易流水，在银行流水有问题场景下，修复可以再次拉取
        CmpMetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, bankDealDetails);
        // 银行流水支持发送事件消息
        cmpSendEventService.sendEventByBankClaimBatch(newBankRecords, EntityStatus.Insert.name());
        // 银行对账单直联拉取 - 供资金调拨逐笔归集场景使用
        sendDirectPullMsg(newBankRecords);

        //return bankDealDetails.size() + (allmap.get("updateData").isEmpty() ? 0 : allmap.get("updateData").size());
        return bankDealDetails.size();
    }

    /**
     * 根据银行流水记录删除账户交易流水中的无效数据
     * 该方法通过比对unique_no和concat_info字段来判断哪些账户交易流水需要被删除
     *
     * @param newBankRecords 银行流水记录列表，用于构建有效的unique_no和concat_info集合
     * @param bankDealDetails 账户交易流水列表，将根据比对结果删除无效记录
     */
    private static void removedBankDealDetail(List<BankReconciliation> newBankRecords, List<BankDealDetail> bankDealDetails) {
        // 两者集合分支相同，不做处理
        if (newBankRecords.size() == bankDealDetails.size()) {
            return;
        }
        // 银行流水处理删除的数据在账户交易流水也删除，优先unique_no，其次concat_info
        Set<String> uniqueNoSet = newBankRecords.stream()
                .map(BankReconciliation::getUnique_no)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        Set<String> concatInfoSet = newBankRecords.stream()
                .map(BankReconciliation::getConcat_info)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());

        // 根据unique_no和concat_info集合过滤并删除无效的账户交易流水记录
        bankDealDetails.removeIf(bankDealDetail -> {
            String uniqueNo = bankDealDetail.getUnique_no();
            String concatInfo = bankDealDetail.getConcat_info();
            if (StringUtils.isNotEmpty(uniqueNo)) {
                return !uniqueNoSet.contains(uniqueNo);
            } else if (StringUtils.isNotEmpty(concatInfo)) {
                return !concatInfoSet.contains(concatInfo);
            }
            return false;
        });
    }

    /**
     * 银行对账单直联拉取 - 供资金调拨逐笔归集场景使用
     *
     * @param impRecords 收款流水
     * @throws Exception
     */
    private void sendDirectPullMsg(List<BankReconciliation> impRecords) throws Exception {
        if (CollectionUtils.isEmpty(impRecords)) {
            log.error("发送账户直联收款流水数据为空");
            return;
        }
        for(BankReconciliation bankReconciliation : impRecords){
            List<BankReconciliation> impRecordsSingle =  new ArrayList<>();
            impRecordsSingle.add(bankReconciliation);
            BizObject userObject = new BizObject();
            //对账单生单数据
            userObject.put("datalist", impRecordsSingle);
            //发送消息到事件中心
            SendEventMessageUtils.sendEventMessageEos(userObject, IEventCenterConstant.CMP_BANKRECONCILIATION, IEventCenterConstant.CMP_BANKRECONCILIATIONDIRECTPULL);
        }
    }

    /**
     * 根据规则设置的银行类别，日期范围
     * 获取匹配数据集合
     * 匹配对方账号不为空，退票状态为空，日期范围内的数据 默认1天指今天和昨天
     *
     * @return
     * @throws Exception
     */
    public void smartClassifyBankreconciliation(BankReconciliation bankReconciliation) throws Exception {
        String smartClassifySwitch = AppContext.getEnvConfig("cmp.smartClassify.switch", "1");
        if ("1".equals(smartClassifySwitch)) {
            //银行对账单智能分类
            BillSmartClassifyBO classifyBO = null;
            if (bankReconciliation.getDc_flag() != null && (bankReconciliation.getDc_flag().equals(Direction.Debit))) {
                try {
                    classifyBO = billSmartClassifyService.smartClassify(
                            bankReconciliation.getAccentity(), bankReconciliation.getTo_acct_no(), bankReconciliation.getTo_acct_name(), bankReconciliation.getCurrency(), Direction.Debit.getValue());
                } catch (Exception e) {
                    log.error("=====银行对账单智能分类====smartClassifyBankreconciliation====", e);
                }
            } else {
                try {
                    classifyBO = billSmartClassifyService.smartClassify(
                            bankReconciliation.getAccentity(), bankReconciliation.getTo_acct_no(), bankReconciliation.getTo_acct_name(), bankReconciliation.getCurrency(), Direction.Credit.getValue());
                } catch (Exception e) {
                    log.error("=====银行对账单智能分类====smartClassifyBankreconciliation====", e);
                }
            }
            if (classifyBO != null) {
                bankReconciliation.setOppositetype(classifyBO.getOppositetype());
                bankReconciliation.setOppositeobjectid(classifyBO.getOppositeobjectid() == null ? null : classifyBO.getOppositeobjectid().toString());
                bankReconciliation.setOppositeobjectname(classifyBO.getOppositeobjectname());
                if (bankReconciliation.getTo_acct_no() == null && bankReconciliation.getTo_acct_name() == null) {
                    bankReconciliation.setOppositetype(OppositeType.Other.getValue());
                    bankReconciliation.setOppositeobjectname(null);
                    bankReconciliation.setOppositeobjectid(null);
                }
                if (null != classifyBO.getOppositebankacctid()) {
                    bankReconciliation.setTo_acct(classifyBO.getOppositebankacctid());
                }
            } else {
                //未匹配则标记为其他类型
                bankReconciliation.setOppositetype(OppositeType.Other.getValue());
                bankReconciliation.setOppositeobjectname(null);
                bankReconciliation.setOppositeobjectid(null);
            }
        }
    }

    /**
     * 对方类型辨识批量
     *
     * @param bankReconciliations
     * @throws Exception
     */
    public void smartClassifyBatchProcess(List<BankReconciliation> bankReconciliations) throws Exception {
        try {
            String smartClassifySwitch = AppContext.getEnvConfig("cmp.smartClassify.switch", "1");
            if ("1".equals(smartClassifySwitch)) {
                String batchcount = AppContext.getEnvConfig("cmp.analysisDataBatchProcess.batchcount", "10");
                int batchcountInt = Integer.parseInt(batchcount);
                ExecutorService pool = getParallelThreadPool();
                ThreadPoolUtil.executeByBatch(pool, bankReconciliations, batchcountInt, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080A", "交易明细智能分类批处理") /* "交易明细智能分类批处理" */, (int fromIndex, int toIndex) -> {
                    for (int t = fromIndex; t < toIndex; t++) {
                        smartClassifyBankreconciliation(bankReconciliations.get(t));
                    }
                    return null;
                }, false);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private static ExecutorService parallelThreadPool;

    private static ExecutorService getParallelThreadPool() {
        synchronized (BankDealDetailServiceImpl.class) {
            if (parallelThreadPool == null) {

                Integer queueLength = Integer.parseInt(AppContext.getEnvConfig("cmp.bankdetail.queueCapacity", "2000"));

                int coreSize = Integer.parseInt(AppContext.getEnvConfig("cmp.analysisDataBatchProcess.coreSize", "0"));
                if (coreSize == 0) {
                    coreSize = Runtime.getRuntime().availableProcessors() * 2;
                }

                int maxSize = Integer.parseInt(AppContext.getEnvConfig("cmp.analysisDataBatchProcess.maxSize", "0"));
                if (maxSize == 0) {
                    maxSize = Runtime.getRuntime().availableProcessors() * 2;
                }

                if (maxSize < coreSize) {
                    maxSize = coreSize;
                }

                parallelThreadPool = ThreadPoolBuilder.ioThreadPoolBuilder()
                        .setDaemon(false)
                        .setRejectHandler(new BlockPolicy())
                        .builder(coreSize, maxSize, queueLength, "cmp-analysisDataBatchProcess-async-");
            }
        }

        return parallelThreadPool;
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 解析交易明细数据
     * @Date 2019/8/7
     * @Param [detailData, enterpriseInfo, bankDealDetails, bankRecords]
     **/
    private void analysisDetailData(CtmJSONObject detailData, Map<String, Object> enterpriseInfo,
                                    List<BankDealDetail> bankDealDetails,
                                    List<BankReconciliation> bankRecords, String currency) throws Exception {
        String bankSeqNo = detailData.getString("bank_seq_no");
        try {
            BankDealDetail detail = new BankDealDetail();
            BankReconciliation bankRecord = new BankReconciliation();
            bankRecord.setDetailReceiptRelationCode(detailData.getString(YQLConstant.DETAIL_RECEIPT_RELATION_CODE));
            bankRecord.setInitflag(false);
            bankRecord.setLibraryflag(false);
            bankRecord.setDataOrigin(DateOrigin.DownFromYQL);
            detail.setTenant(AppContext.getTenantId());
            bankRecord.setTenant(AppContext.getTenantId());
            detail.setAccentity((String) enterpriseInfo.get("accEntityId"));
//            bankRecord.setAccentity((String) enterpriseInfo.get("accEntityId"));
            EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById((String) enterpriseInfo.get("accountId"));
            //银行类别
            bankRecord.setBanktype(enterpriseBankAcctVoWithRange.getBank());
            // 所属组织
            bankRecord.setOrgid(enterpriseBankAcctVoWithRange.getOrgid());
            List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
            if (orgRangeVOS != null && orgRangeVOS.size() == 1) {
                // 授权使用组织 只有一个
                bankRecord.setAccentity(orgRangeVOS.get(0).getRangeOrgId());
                // 授权使用组织确认节点 银行对账单
                bankRecord.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
                // 确认状态 已确认
                bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
            } else if (orgRangeVOS != null && orgRangeVOS.size() > 1) {
                // 授权使用组织 多个
                bankRecord.setAccentity(null);
                // 授权使用组织确认节点
                bankRecord.setConfirmbill(null);
                // 确认状态 待确认
                bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
            } else {
                bankRecord.setAccentity(null);
                // 授权使用组织确认节点
                bankRecord.setConfirmbill(null);
                // 确认状态 待确认
                bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
            }
            detail.setEnterpriseBankAccount((String) enterpriseInfo.get("accountId"));
            detail.setBanktype(enterpriseBankAcctVoWithRange.getBank());
            bankRecord.setBankaccount((String) enterpriseInfo.get("accountId"));
            String dateStr = detailData.getString("tran_date");
            Date tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
            detail.setTranDate(tranDate);
            bankRecord.setTran_date(tranDate);
            bankRecord.setDzdate(tranDate);
            String timeStr = detailData.getString("tran_time");
            if (StringUtils.isNotEmpty(timeStr)) {
                Date tranTime = DateUtils.dateParse(dateStr + timeStr, DateUtils.YYYYMMDDHHMMSS);
                detail.setTranTime(tranTime);
                bankRecord.setTran_time(tranTime);
            }
            String is_refund = detailData.getString("is_refund");
            if (StringUtils.isNotEmpty(is_refund)) {
                detail.set("is_refund", is_refund);
                bankRecord.set("is_refund", is_refund);
            }
            detail.setBankseqno(bankSeqNo);
            bankRecord.setBank_seq_no(bankSeqNo);
            //bankRecord.setThirdserialno(bankSeqNo);
            String toAcctNo = detailData.getString("to_acct_no");
            detail.setTo_acct_no(toAcctNo);
            bankRecord.setTo_acct_no(toAcctNo);
            //新增利息字段
            BigDecimal interest = detailData.getBigDecimal("interest");
            detail.setInterest(interest);
            bankRecord.setInterest(interest);
            String toAcctName = detailData.getString("to_acct_name");
            detail.setTo_acct_name(toAcctName);
            bankRecord.setTo_acct_name(toAcctName);
            String toAcctBank = detailData.getString("to_acct_bank");
            detail.setTo_acct_bank(toAcctBank);
            bankRecord.setTo_acct_bank(toAcctBank);
            String toAcctBankName = detailData.getString("to_acct_bank_name");
            detail.setTo_acct_bank_name(toAcctBankName);
            bankRecord.setTo_acct_bank_name(toAcctBankName);
            String currencyCode = detailData.getString("curr_code");
            //没有币种 取上面currency中的缓存币种
            if (StringUtils.isNotEmpty(currencyCode)) {
                currency = currencyQueryService.getCurrencyByCode(currencyCode);
            }
            detail.setCurrency(currency);
            bankRecord.setCurrency(currency);

            String cashFlag = detailData.getString("cash_flag");
            detail.setCashflag(cashFlag);
            bankRecord.setCash_flag(cashFlag);
            BigDecimal acctBal = detailData.getBigDecimal("acct_bal");
            detail.setAcctbal(acctBal);
            bankRecord.setAcct_bal(acctBal);
            BigDecimal tranAmt = detailData.getBigDecimal("tran_amt");
            detail.setTran_amt(tranAmt);
            bankRecord.setTran_amt(tranAmt);
            String dcFlag = detailData.getString("dc_flag");
            if ("d".equalsIgnoreCase(dcFlag)) {
                detail.setDc_flag(Direction.Debit);
                bankRecord.setDc_flag(Direction.Debit);
                bankRecord.setDebitamount(tranAmt);
            } else if ("c".equalsIgnoreCase(dcFlag)) {
                detail.setDc_flag(Direction.Credit);
                bankRecord.setDc_flag(Direction.Credit);
                bankRecord.setCreditamount(tranAmt);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100094"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E5", "银企联返回的借贷标识非法，请联系开发人员！") /* "银企联返回的借贷标识非法，请联系开发人员！" */);
            }
            String oper = detailData.getString("oper");
            detail.setOper(oper);
            bankRecord.setOper(oper);
            String valueDateStr = detailData.getString("value_date");
            if (StringUtils.isNotEmpty(valueDateStr)) {
                Date valueDate = DateUtils.dateParse(valueDateStr, DateUtils.YYYYMMDD);
                detail.setValue_date(valueDate);
                bankRecord.setValue_date(valueDate);
            }
            String useName = detailData.getString("use_name");
            detail.setUse_name(useName);
            bankRecord.setUse_name(useName);
            String remark = detailData.getString("remark");
            String remark01 = detailData.getString("remark01");
            detail.setRemark(remark);
            bankRecord.setRemark(remark);
            detail.setRemark01(remark01);
            bankRecord.setRemark01(remark01);
            //设置财资统一对账码
            DealDetailUtils.setSmartCheckNoInfo(bankRecord);
            String bankCheckCode = detailData.getString("bank_check_code");  //添加交易流水号 20201125
            detail.setBankdetailno(bankCheckCode);
            // TODO 银行对账问题调试
            bankRecord.setBankcheckno(bankCheckCode);
            // 处理是否存在客商档案
//            MerchantUtils.dealMerchantFlag(detail);
            //国际化新增字段
            BigDecimal rate = detailData.getBigDecimal("rate");
            detail.setRate(rate);

            BigDecimal fee_amt = detailData.getBigDecimal("fee_amt");
            detail.setFee_amt(fee_amt);

            String fee_amt_cur = detailData.getString("fee_amt_cur");
            //没有币种 取上面currency中的缓存币种
            if (StringUtils.isNotEmpty(fee_amt_cur)) {
                currency = currencyQueryService.getCurrencyByCode(fee_amt_cur);
            }
            detail.setFee_amt_cur(currency);

            String pay_use_desc = detailData.getString("pay_use_desc");
            detail.setPay_use_desc(pay_use_desc);

            BigDecimal corr_fee_amt = detailData.getBigDecimal("corr_fee_amt");
            detail.setCorr_fee_amt(corr_fee_amt);

            String corr_fee_amt_cur = detailData.getString("corr_fee_amt_cur");
            if (StringUtils.isNotEmpty(corr_fee_amt_cur)) {
                currency = currencyQueryService.getCurrencyByCode(corr_fee_amt_cur);
            }
            detail.setCorr_fee_amt_cur(currency);

            String sub_name = detailData.getString("sub_name");
            detail.setSub_name(sub_name);

            String proj_name = detailData.getString("proj_name");
            detail.setProj_name(proj_name);

            String budget_source = detailData.getString("budget_source");
            detail.setBudget_source(budget_source);

            String voucher_type = detailData.getString("voucher_type");
            detail.setVoucher_type(voucher_type);

            String voucher_no = detailData.getString("voucher_no");
            detail.setVoucher_no(voucher_no);

            detail.setEntityStatus(EntityStatus.Insert);
            bankRecord.setEntityStatus(EntityStatus.Insert);
            //20231127
            Long id = ymsOidGenerator.nextId();
            detail.setId(id);
            bankRecord.setId(id);
            // 添加创建时间、创建日期
            detail.setCreateDate(new Date());
            detail.setCreateTime(new Date());
            bankRecord.setCreateDate(new Date());
            bankRecord.setCreateTime(new Date());

            //RPT0210退票辨识优化CZFW-373754
            //【DSP支持问题】银企联已返回退票标识和原交易流水号，但是司库的交易流水界面不显示。麻烦老师帮忙看下。
            // 退票 银企返回的 1：是 2：否
            if (detailData.get("is_refund") != null) {
                detail.setRefundFlag(detailData.get("is_refund").toString().equals("1"));
                bankRecord.setRefundFlag(detailData.get("is_refund").toString().equals("1"));
            }
            // 原交易流水号
            detail.setOrignBankseqno(detailData.getString("refund_original_transaction"));
            bankRecord.setOrignBankseqno(detailData.getString("refund_original_transaction"));

            //对方账号后去空格
            if (!StringUtils.isEmpty(bankRecord.getTo_acct_no())) {
                bankRecord.setTo_acct_no(bankRecord.getTo_acct_no().replaceAll(" ", ""));
            }

            //退票匹配
            if (!Short.valueOf(OppositeType.InnerOrg.getValue() + "").equals(bankRecord.getOppositetype())) {
                //20230517先屏蔽，等辨识规则开关添加后再开启
//            checkRefund(bankRecord);
            }
            //2023-11-30 yangjn 添加唯一交易流水验证
            String unique_no = detailData.getString("unique_no");
            detail.setUnique_no(unique_no);
            bankRecord.setUnique_no(unique_no);
            if (bankSeqNo == null || "".equals(bankSeqNo)) {
                bankDealDetails.add(detail);
                bankRecords.add(bankRecord);
            } else if (bankRecord.getBank_seq_no().equals(detail.getBankseqno())) {
                //保证同一个线程内交易明细与对账单数据一致；若不一致则不插入数据
                bankDealDetails.add(detail);
                bankRecords.add(bankRecord);
            }
            /**
             * 茅台流水删除比对，每次拉取都要发送消息
             */
            if("1".equals(bank_delete_event_compare)){
                for (BankReconciliation bizObject : bankRecords) {
                    cmpSendEventService.sendEventByBankClaim(bizObject, "compare");
                }
            }
            CtmCmpCheckRepeatDataService checkRepeatDataService = AppContext.getBean(CtmCmpCheckRepeatDataService.class);
            checkRepeatDataService.fillBankReconciliationConcatInfo(bankRecord);
        } catch (Exception e) {
            log.error("BankDealDetailServiceImpl-checkRepetition" + e);
            throw new Exception(e);
        }
    }

    @Override
    public CtmJSONObject batchQueryTransactionDetailForRpc(BankLinkParam params) throws Exception {
        String account = params.getAccountId();//银行账号ID
        String acct_no = params.getAcct_no();//银行账号
        String acct_name = params.getAcct_name();//银行账户名称
        String channel = bankConnectionAdapterContext.getChanPayCustomChanel();
        if (StringUtils.isEmpty(account) || StringUtils.isEmpty(acct_no) || StringUtils.isEmpty(acct_name)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100095"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080033", "批量账户交易明细查询入参不能为为空") /* "批量账户交易明细查询入参不能为为空" */);
        }

        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(account);
        if (enterpriseBankAcctVO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100096"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590002D", "没有有效的企业银行信息") /* "没有有效的企业银行信息" */);
        }
        CtmJSONArray records = new CtmJSONArray();
        try {
            if (!bankAccountSettingService.getOpenFlagByBankAccountIdOfQuery(account)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA", "【") /* "【" */ + acct_name + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D3", "】的企业银行账户没有开通银企联，无法查询银行账户交易明细") /* "】的企业银行账户没有开通银企联，无法查询银行账户交易明细" */);
            }
            String customNo = getCustomNo(account);
            if (StringUtils.isEmpty(customNo)) {
                throw new CtmException(String.format(EnterpriseBankQueryService.noCustomNoMessageWithAcct_no,acct_no));
            }
            params.setCustomNo(customNo);
            params.setSignature("");
            params.setChannel(channel);
            CtmJSONObject queryMsg = buildQueryTransactionDetailMsg(params);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("=======================交易明细参数=========================>" + CtmJSONObject.toJSONString(queryMsg));
            CtmJSONObject result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("=======================交易明细结果===========================>" + CtmJSONObject.toJSONString(result));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, service_resp_code)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    int backNum = responseBody.getInteger("back_num");
                    if (backNum > 1) {
                        //records = responseBody.getJSONArray("record");
                    }
                    Map<String, Object> enterpriseInfo = new HashMap<>();
                    enterpriseInfo.put("startDate", params.getStartDate());
                    enterpriseInfo.put("endDate", params.getEndDate());
                    enterpriseInfo.put("accEntityId", enterpriseBankAcctVO.getOrgid());
                    enterpriseInfo.put("accountId", enterpriseBankAcctVO.getId());
                    enterpriseInfo.put("banktype", enterpriseBankAcctVO.getBank());
                    enterpriseInfo.put("account", enterpriseBankAcctVO.getAccount());
                    enterpriseInfo.put("lineNumber", enterpriseBankAcctVO.getLineNumber());
                    insertTransactionDetail(enterpriseInfo, responseBody);
                    String nextPage = responseBody.getString("next_page");
                    if ("1".equals(nextPage)) {
                        int begNum = params.getBegNum();
                        params.setBegNum(begNum + ITransCodeConstant.QUERY_NUMBER_50);
                        params.setQueryExtend(responseBody.getString("query_extend"));
                        params.setNextPage(true);
                        batchQueryTransactionDetailForRpc(params);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("nextPage", false);
        return responseMsg;

    }

    public CtmJSONObject queryAccountTransactionDetail(CtmJSONObject params) throws Exception {

        //根据条件查询银行账户 包含 结算中心户 和 银行账户
        List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(params);
        //通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
        Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = EnterpriseBankQueryService.getBankAcctVOsGroup(bankAccounts);
        LogUtil.saveAccountBussinessLog(params, bankAccountsGroup);
        String date_pattern = params.getString("date_pattern");
        //获取日期时间段
        Date startDate = DateUtils.dateParse(params.getString("startDate"), date_pattern);
        Date endDate = DateUtils.dateParse(params.getString("endDate"), date_pattern);
        List<String> betweendate = DateUtils.getBetweenDateForStrEnd(startDate, endDate);

        //直联账户
        List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
        //将直连账户和币种、日期进行拼接
        List<Map<String, Object>> checkSuccessList = new ArrayList<>();
        String channel = bankConnectionAdapterContext.getChanPayCustomChanel();
        for (EnterpriseBankAcctVO evo : checkSuccess) {
            for (BankAcctCurrencyVO currencyVO : evo.getCurrencyList()) {
                for (String date : betweendate) {
                    Map<String, Object> map = new HashMap<>();
                    List<BankAcctCurrencyVO> getCurrencyList = new ArrayList<>();
                    getCurrencyList.add(currencyVO);
                    map.put("currencyCode", queryCurrencyCode(getCurrencyList).get(currencyVO.getCurrency()));
                    map.put(ProcessUtil.ACCOUNT_ID, evo.getId());
                    map.put("accEntityId", evo.getOrgid());
                    map.put("banktype", evo.getBank());
                    map.put("acct_name", evo.getAcctName());
                    map.put("acct_no", evo.getAccount());
                    map.put("startDate", date);
                    map.put("endDate", date);
                    map.put("lineNumber", evo.getLineNumber());
                    map.put("channel", channel);
                    checkSuccessList.add(map);
                }
            }
        }
        //结算中心 内部账户
        List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
        CtmJSONObject responseMsg = new CtmJSONObject();
        String uid = params.getString("uid");
        int yqlAccountNum = checkSuccess.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        int innerAccountNum = innerAccounts.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        int listSize = yqlAccountNum + innerAccountNum;
        ProcessUtil.initProcessWithAccountNum(uid, listSize);
        //开启子线程后，ThreadLocal变量不会传递给子线程，所以需要手动传递
        Object serviceCode = InvocationInfoProxy.getExtendAttribute("serviceCode");
        executorServiceAccept.submit(() -> {
            params.put("serviceCode", serviceCode);
            //构建进度条信息
            try {
                //拉取内部账户
                if (!innerAccounts.isEmpty()) {
                    for (EnterpriseBankAcctVO enterpriseBankAcctVO : innerAccounts) {
                        CtmJSONObject queryParam = new CtmJSONObject();
                        queryParam.putAll(params);
                        queryParam.put("accEntity", enterpriseBankAcctVO.getOrgid());
                        queryParam.put("accountId", enterpriseBankAcctVO.getId());

                        queryParam.put("allDbCount", 0);
                        queryParam.put("account", enterpriseBankAcctVO.getAccount());
                        for (String date : betweendate) {
                            queryParam.put("startDate", date);
                            queryParam.put("endDate", date);
                            queryParam.put(DateUtils.START_DATE_DATE_TYPE, DateUtils.dateParse(date, DateUtils.DATE_PATTERN));
                            queryParam.put(DateUtils.END_DATE_DATE_TYPE, DateUtils.dateParse(date, DateUtils.DATE_PATTERN));
                            String accountInfo = queryParam.get("account") + "|" + queryParam.get("startDate");
                            try {
                                String lockKey = accountInfo + ICmpConstant.QUERYTRANSDETAILKEY;
                                CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                                        //加锁失败添加报错信息 刷新进度+1
                                        ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080B", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfo));
                                        ProcessUtil.failAccountNumAddOne(uid);
                                        return;
                                    }
                                    // 加锁成功,内部账户查询
                                    CtmJSONObject ctmJSONObject = this.queryInnerAccountDetails(queryParam);
                                    ProcessUtil.newAddCountAdd(uid, (Integer) ctmJSONObject.get(ProcessUtil.ALL_ADD_COUNT));
                                    ProcessUtil.totalPullCountAdd(uid, (Integer) ctmJSONObject.get(ProcessUtil.ALL_PULL_COUNT));
                                });
                            } catch (Exception e) {
                                log.error("queryAccountTransactionDetail", e);
                                //加锁失败添加报错信息 刷新进度+1
                                ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080E", "[%s]:此账户操作发生异常:") /* "[%s]:此账户操作发生异常:" */, accountInfo) + e.getMessage());
                                ProcessUtil.failAccountNumAddOne(uid);
                            }
                        }
                    }
                }
                // 无直连账号
                if (!checkSuccessList.isEmpty()) {
                    int batchcount = Integer.parseInt(AppContext.getEnvConfig("cmp.bankdetail.batchcount", "10"));
                    int totalTask = (checkSuccessList.size() % batchcount == 0 ? checkSuccessList.size() / batchcount : (checkSuccessList.size() / batchcount) + 1);
                    //拉取直连账户
                    queryAccountTransactionDetailExcute(checkSuccess, checkSuccessList, responseMsg, uid, params, batchcount, totalTask);
                }
            } catch (Exception e) {
                log.error("queryAccountTransactionDetail", e);
                ProcessUtil.addMessage(uid, e.getMessage());
                ProcessUtil.failAccountNumAdd(uid, innerAccountNum);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100098"), e.getMessage());
            } finally {
                ProcessUtil.completed(uid, true);
            }
        });
        responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540080D", "账户交易明细开始查询。") /* "账户交易明细开始查询。" */);
        return responseMsg;
    }

    /**
     * 执行拉取账户交易明细
     *
     * @param params
     * @throws Exception
     */
    public void queryAccountTransactionDetailExcute(List<EnterpriseBankAcctVO> checkSuccess, List<Map<String, Object>> checkSuccessList, CtmJSONObject responseMsg, String uid, CtmJSONObject params, int batchcount, int totalTask) throws Exception {
        //拉取直连账户
        if (checkSuccess.size() > 0) {
            this.queryBankAccountTransactionDetail(params, checkSuccessList, responseMsg, uid, batchcount, totalTask);
        }
    }


    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author jiangpengk
     * @Description 构建查询账户交易明细报文
     * @Date 2023/6/7
     * @Param [params, acct_no,acct_name]
     **/
    public CtmJSONObject buildQueryTransactionDetailMsg(CtmJSONObject params) throws Exception {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", buildRequestSeqNo(params.getString("customNo")));
        requestHead.put("cust_no", params.getString("customNo"));
        requestHead.put("cust_chnl", params.getString("channel"));
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", params.getString("operator") == null ? "" : params.getString("operator"));
        requestHead.put("oper_sign", params.getString("signature"));
        requestHead.put("tran_code", ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL);

        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_no", params.getString("acct_no"));
        requestBody.put("acct_name", params.getString("acct_name"));
        String startDate = params.getString("startDate").replaceAll("-", "").substring(0, 8);
        String endDate = params.getString("endDate").replaceAll("-", "").substring(0, 8);
        requestBody.put("curr_code", params.get("currencyCode"));
        requestBody.put("query_extend", params.get("queryExtend"));
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", params.get("begNum") == null ? 1 : params.get("begNum"));
        requestBody.put("query_num", ITransCodeConstant.QUERY_NUMBER_50);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author jiangpengk
     * @Description 构建查询账户交易明细报文
     * @Date 2023/6/7
     * @Param [params, acct_no,acct_name]
     **/
    private CtmJSONObject buildQueryTransactionDetailMsg(BankLinkParam params) throws Exception {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", buildRequestSeqNo(params.getCustomNo()));
        requestHead.put("cust_no", params.getCustomNo());
        requestHead.put("cust_chnl", params.getChannel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", params.getOperator());
        requestHead.put("oper_sign", params.getSignature());
        requestHead.put("tran_code", ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL);

        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_no", params.getAcct_no());
        requestBody.put("acct_name", params.getAcct_name());
        String startDate = params.getStartDate().replaceAll("-", "").substring(0, 8);
        String endDate = params.getEndDate().replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", params.getBegNum());
        requestBody.put("query_num", ITransCodeConstant.QUERY_NUMBER_50);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

}
