package com.yonyoucloud.fi.cmp.bankreceipt.service;

import cn.hutool.core.map.MapUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.file.rpc.ApFileDeleteService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.api.IBillService;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.poi.util.DateUtil;
import com.yonyou.ucf.mdd.ext.poi.util.POIUtils;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.ctm.stctrpcapi.api.IStctAccountReconciliationApiService;
import com.yonyoucloud.ctm.stctrpcapi.model.request.StctAccountReconciliationReqVo;
import com.yonyoucloud.ctm.stctrpcapi.model.response.StctAccountReconciliationRespVo;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.SignStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.ElectronicStatementConfirm;
import com.yonyoucloud.fi.cmp.enums.YqlDownStatusEnum;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.oss.OSSPoolClient;
import com.yonyoucloud.fi.cmp.paymentbill.service.redis.BankBalanceRedisUtil;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.Constant.ThreadConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import com.yonyoucloud.fi.cmp.vo.BankTranBatchAddVO;
import com.yonyoucloud.fi.cmp.vo.BankTranBatchUpdateVO;
import com.yonyoucloud.fi.cmp.vo.ResultVo;
import cn.hutool.core.thread.BlockPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @ClassName BankReceiptServiceImpl
 * @Description 银行交易回单
 * @Author tongyd
 * @Date 2019/4/28 16:23
 * @Version 1.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class BankReceiptServiceImpl implements BankReceiptService {

    public static String SERVICE_RESP_CODE = "000000";  //服务响应码   “000000”（6个0）代表成功，如果返回“000000”，则service_status的值一定是“00”
    private final CurrencyQueryService currencyQueryService;
    private final YmsOidGenerator ymsOidGenerator;// 生成oid
    private final HttpsService httpsService;
    private final OSSPoolClient ossPoolClient;// oss客户端
    private final BankConnectionAdapterContext bankConnectionAdapterContext;// 签名验签
    private final BankAccountSettingService bankAccountSettingService;
    private AtomicInteger cardinalNumber = new AtomicInteger(0);
    private final BaseRefRpcService baseRefRpcService;// 基础档案查询
    private final CooperationFileUtilService cooperationFileUtilService;
    private final BankreconciliationService bankreconciliationService;
    private final CmCommonService cmCommonService;// 生成oid
    @Autowired
    BankBalanceRedisUtil bankBalanceRedisUtil;
    @Autowired
    BankDealDetailService bankDealDetailService;
    //企业银行账号查询一页默认返回值
    private static int pageSIze = 4999;
    @Resource
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    TaskBankReceiptService taskBankReceiptService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private IEnterpriseBankAcctService iEnterpriseBankAcctService;
    @Autowired
    private IBillService iBillService;
    @Autowired
    private ApFileDeleteService apFileDeleteService;

    private final BankReceiptHandleDataService bankReceiptHandleDataService;

    private final BankReceiptYqlBatchDownService bankReceiptYqlBatchDownService;

    static ExecutorService executorService = null;
    static int batchcount = 10;
    // 流水关联回单是否检查回单文件已下载
    static String receiptRelateCheckFile;
    // 多语-回单编号
    String receiptNoLang = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTM-CMP-MD_1808A9F004D007A4", "回单编号");

    // 处理总数 (2000 +10 ) * 10 = 20100
    static {
        receiptRelateCheckFile = AppContext.getEnvConfig("receipt.relate.check.file", "false");
        int maxSize = Integer.parseInt(AppContext.getEnvConfig("cmp.bankreceipt.max.poolSize", "10"));
        int queueLength = Integer.parseInt(AppContext.getEnvConfig("cmp.bankreceipt.queueCapacity", "2000"));
        int coreSize = 10;
        String threadName = "cmp-bankreceipt-async-";
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setThreadNamePrefix("cmp-bankreceipt-async-")
                .setQueueSize(queueLength)
                .setDaemon(false)
                .setMaximumPoolSize(maxSize)
                .setRejectHandler(new BlockPolicy())
                .builder(coreSize, maxSize, queueLength, threadName);
        ;
    }

    private static final Cache<String, EnterpriseBankAcctVO> enterpriseBankAcctVOCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();
    @Autowired
    private TaskBankReceiptServiceImpl taskBankReceiptServiceImpl;

    @Override
    public CtmJSONObject queryAccountReceiptDetailUnNeedUkey(CtmJSONObject param) throws Exception {
        String uid = param.getString("uid");
        //根据参数查询内部账户
        List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(param);
        //分组查询两种账户
        Map<String, List<EnterpriseBankAcctVO>> bankAcctVOsGroup = EnterpriseBankQueryService.getBankAcctVOsGroup(bankAccounts);
        List<EnterpriseBankAcctVO> yqlAccounts = bankAcctVOsGroup.get(EnterpriseBankQueryService.CHECK_SUCCESS);
        List<EnterpriseBankAcctVO> innerAccounts = bankAcctVOsGroup.get(EnterpriseBankQueryService.INNER_ACCOUNTS);
        int yqlAccountNum = yqlAccounts.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        int innerAccountNum = innerAccounts.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();

        // 组装直联账户查询参数
        List<CtmJSONObject> queryParamList = getExceutAccoutVos(param);
        int listCount = yqlAccountNum + innerAccountNum;

        if (listCount == 0) {
            //进图置为100%
            ProcessUtil.completedResetCount(uid, true);
        }
        // 初始化进度条
        ProcessUtil.initProcessWithAccountNum(uid, listCount);
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 1, 10, "queryAccountReceiptDetailUnNeedUkey-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            //根据参数查询内部账户 获取内部账户交易回单
            if (!innerAccounts.isEmpty()) {
                List<String> accountIdList = innerAccounts.stream().map(EnterpriseBankAcctVO::getId).collect(Collectors.toList());
                List<String> accountIdListLock = BatchLockGetKeysUtils.<EnterpriseBankAcctVO>batchLockCombineKeys(ICmpConstant.QUERYBANKRECEIPTKEY, innerAccounts);
                try {
                    List<EnterpriseBankAcctVO> finalInnerAccounts = innerAccounts;
                    CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountIdListLock, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                        if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            //加锁失败添加报错信息 刷新进度+1
                            ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073B", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountIdListLock));
                            ProcessUtil.refreshAccountProcess(uid, false, finalInnerAccounts.size());
                            return;
                        }
                        // 加锁成功,内部交易回单
                        stctInsertReceiptByinnerAccounts(param, accountIdList, uid);
                        //ProcessUtil.refreshAccountProcess(uid, true, finalInnerAccounts.size());
                    });
                } catch (Exception e) {
                    log.error("queryAccountReceiptDetailUnNeedUkey", e);
                    //加锁失败添加报错信息 刷新进度+1
                    //ProcessUtil.addMessage(uid, String.format("[%s]:此账户操作发生异常", accountIdListLock));
                    ProcessUtil.refreshAccountProcess(uid, false, innerAccounts.size());
                }
            }
            //查询直联账户
            if (null != queryParamList && !queryParamList.isEmpty()) {
                try {
                    queryAccountReceiptDetailExcute(queryParamList, uid);
                    //进度置为100%
                    //ProcessUtil.refreshAccountProcess(uid, true, yqlAccounts.size());
                } catch (Exception e) {
                    log.error("查询银行交易回单失败：", e);
                    ProcessUtil.refreshAccountProcess(uid, false, yqlAccounts.size());
                } finally {
                    if (autoAssociatedDataExecutor != null) {
                        autoAssociatedDataExecutor.shutdown();
                    }
                }
            }
            ProcessUtil.completed(uid, true);
        });
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100067", "账户交易明细开始查询。") /* "账户交易明细开始查询。" */);
        return responseMsg;
    }

    public void queryAccountReceiptDetailExcute(List<CtmJSONObject> queryParamList, String uid) throws Exception {
        Set<String> failAccountSet = ConcurrentHashMap.newKeySet();
        List<Object> results = ThreadPoolUtil.executeByBatchNotShutDown(executorService, queryParamList, batchcount, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005B", "查交易回单") /* "查交易回单" */, (int fromIndex, int toIndex) -> {
            StringBuilder builder = new StringBuilder();
            for (int t = fromIndex; t < toIndex; t++) {
                Map<String, Object> queryParam = queryParamList.get(t);
                // 加锁的账号信息
                String accountId = Objects.toString(queryParam.get("accountId"));
                Object currencyCode = queryParam.get("currencyCode");
                String accountInfo = taskBankReceiptServiceImpl.getAccountInfo(accountId, queryParam.get("startDate"), currencyCode);
                // 加锁信息：账号+行为
                String lockKey = accountInfo + ICmpConstant.QUERYBANKRECEIPTKEY;
                String accountId_currencyCode_key = accountId + "|" + currencyCode;
                try {
                    CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                        if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            //加锁失败添加报错信息 并把进度刷新
                            ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073B", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfo));
                            ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                            return;
                        }
                        // 加锁成功，直连拉取数据并进行验重和入库
                        CtmJSONObject paramNow = new CtmJSONObject(queryParam);
                        buildAndQueryUnNeedUkey(paramNow, uid, failAccountSet);
                    });
                } catch (Exception e) {
                    log.error("queryAccountReceiptDetailExcute", e);
                    ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                    ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400741", "[%s]:此账户操作发生异常:[%s]") /* "[%s]:此账户操作发生异常:[%s]" */, accountInfo, e.getMessage()));
                }
            }
            return null;
        });
    }

    public List<CtmJSONObject> getExceutAccoutVos(CtmJSONObject param) throws Exception {
        List<CtmJSONObject> queryParamList = new ArrayList<>();
        String accEntityParam = param.getString("accEntity");
        List<String> accentitys = Arrays.asList(accEntityParam.split(","));
        //判断前段数据是否为多选
        if (accEntityParam.contains("[")) {
            accentitys = param.getObject("accEntity", List.class);
        }
        // end wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        // 银行账户
        List<String> bankAccountIds = (List<String>) param.get("accountId");
        String date_pattern = param.getString("date_pattern");
        if (StringUtils.isEmpty(date_pattern)) {
            date_pattern = "yyyy-MM-dd";
        }
        //开始时间
        Date startDate = new SimpleDateFormat(date_pattern).parse(param.get("startDate").toString());
        //结束时间
        Date endDate = new SimpleDateFormat(date_pattern).parse(param.get("endDate").toString());
        param.put(DateUtils.START_DATE_DATE_TYPE, startDate);
        param.put(DateUtils.END_DATE_DATE_TYPE, endDate);
        //计算日期差
        int days = DateUtils.dateBetween(startDate, endDate);
        if (days > 31) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100108"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC941404480007", "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!") /* "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!" */);
        }
        //查询银企联账号：过滤出 启用银企联 且状态为启用 且有客户号的数据 用这些数据去查询 询企业银行账户
        CommonRequestDataVo queryVo = new CommonRequestDataVo();

        if (bankAccountIds == null || bankAccountIds.size() < 1) {
//            queryVo.setAccentityList(accentitys);
            if (accentitys != null && accentitys.size() > 0) {
                EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                newEnterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllEnableWithRange(newEnterpriseParams);
                bankAccountIds = new ArrayList<>();
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                    bankAccountIds.add(enterpriseBankAcctVO.getId());
                }
                queryVo.setEnterpriseBankAccountList(bankAccountIds);
            }
        } else {
            queryVo.setEnterpriseBankAccountList(bankAccountIds);
        }
        List<BankAccountSetting> bankAccountSettingList = cmCommonService.queryAutocorrsettingByParam(queryVo);
        if (bankAccountSettingList.isEmpty()) {
            //因为要适配内部账户回单 所以这里注释掉
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100048"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F3", "选择的企业银行账户没有开通银企联，无法查询银行账户回单数据") /* "选择的企业银行账户没有开通银企联，无法查询银行账户回单数据" */);
            return queryParamList;
        }
        String customNo = bankAccountSettingList.get(0).getCustomNo();
        if (bankAccountIds != null) {
            bankAccountIds.clear();
        } else {
            bankAccountIds = new ArrayList<>();
        }
        for (BankAccountSetting vo : bankAccountSettingList) {
            bankAccountIds.add(vo.getEnterpriseBankAccount());
        }
        //查询企业银行账户(启用 且 未删除)
        EnterpriseParams params = new EnterpriseParams();
        if (bankAccountIds != null && !bankAccountIds.isEmpty()) {
            params.setIdList(bankAccountIds);
        }
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = enterpriseBankQueryService.queryAll(params);
        if (enterpriseBankAcctVOs.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100109"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050044", "选择的资金组织，尚未维护银行账户，请检查!") /* "选择的资金组织，尚未维护银行账户，请检查!" */);
        }
        //获取当前之间段内 每一天的日期
        List<String> dateList = DateUtils.getBetweenDateForStrEnd(startDate, endDate);
        String channel = bankConnectionAdapterContext.getChanPayCustomChanel();
        //开启子线程后，ThreadLocal变量不会传递给子线程，所以需要手动传递
        Object serviceCode = InvocationInfoProxy.getExtendAttribute("serviceCode");
        List<BankAcctCurrencyVO> getCurrencyList = new ArrayList<>();
        //List<CtmJSONObject>  queryParamList = new ArrayList<>();
        //根据账户信息 和 日期组装数据，每个账户+一个日期为一个请求
        for (int i = 0; i < enterpriseBankAcctVOs.size(); i++) {
            EnterpriseBankAcctVO evo = enterpriseBankAcctVOs.get(i);
            for (BankAcctCurrencyVO currencyVO : evo.getCurrencyList()) {
                //再加个币种
                getCurrencyList.add(currencyVO);
                for (String date : dateList) {
                    CtmJSONObject queryParam = new CtmJSONObject();
                    queryParam.put("accEntity", enterpriseBankAcctVOs.get(i).getOrgid());
                    queryParam.put("accountId", enterpriseBankAcctVOs.get(i).getId());
                    queryParam.put("startDate", date);
                    queryParam.put("endDate", date);
                    queryParam.put("customNo", customNo);
                    queryParam.put("enterpriseBankAcctVO", enterpriseBankAcctVOs.get(i));
                    queryParam.put("channel", channel);
                    queryParam.put("begNum", 1);
                    queryParam.put("currencyCode", bankDealDetailService.queryCurrencyCode(getCurrencyList).get(currencyVO.getCurrency()));
                    queryParam.put("serviceCode", serviceCode);
                    queryParamList.add(queryParam);
                }
            }
        }
        return queryParamList;
    }


    /**
     * 构建电子回单查询参数 并进行查询
     *
     * @param param
     * @throws Exception
     */
    public void buildAndQueryUnNeedUkey(CtmJSONObject param, String uid, Set<String> failAccountSet) throws Exception {
        CtmJSONObject queryMsg = new CtmJSONObject();
        CtmJSONObject result = new CtmJSONObject();
        String errorInfo = null;
        EnterpriseBankAcctVO bankAccount = (EnterpriseBankAcctVO) param.get("enterpriseBankAcctVO");
        //用于组装报错信息
        param.put(YQLUtils.ACCT_NO , bankAccount.getAccount());
        String bankAccountId = bankAccount.getId();
        String accountId_currencyCode_key = bankAccountId + "_" + param.getString("currencyCode");
        try {
            String customNo = null;
            Map<String, Object> accountSeting = getBankAccountSetting(bankAccountId);
            if (accountSeting != null && accountSeting.get("customNo") != null) {
                customNo = accountSeting.get("customNo").toString();
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100048"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F3", "选择的企业银行账户没有开通银企联，无法查询银行账户回单数据") /* "选择的企业银行账户没有开通银企联，无法查询银行账户回单数据" */);
            }
            param.put("customNo", customNo);
            queryMsg = buildReceiptQueryMsgUnNeedUkey(param, bankAccount);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("======回单拉取请求参数=======>" + queryMsg.toString());
            String transCode = ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL;
            String chanPayUri = bankConnectionAdapterContext.getChanPayUri();
            result = HttpsUtils.doHttpsPostNew(transCode, requestData, chanPayUri);
            HttpsUtils.saveYQLBusinessLog(transCode, chanPayUri, queryMsg, result, param, bankAccount.getAccount());
            log.error("=======回单拉取result======>" + CtmJSONObject.toJSONString(result));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject(YQLConstant.DATA).getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(transCode, service_resp_code)) {
                    CtmJSONObject responseBody = result.getJSONObject(YQLConstant.DATA).getJSONObject(YQLConstant.RESPONSE_BODY);

                    String accEntityId = bankAccount.getOrgid();
                    Map<String, Object> enterpriseInfo = new HashMap<>();
                    enterpriseInfo.put("accEntityId", accEntityId);
                    enterpriseInfo.put("accountId", bankAccountId);
//                enterpriseInfo.put("currencyId", bankAccount.get("currency"));
                    enterpriseInfo.put("customNo", customNo);

                    insertReceiptDetail(enterpriseInfo, responseBody, uid);
                    String nextPage = responseBody.getString("next_page");
                    while ("1".equals(nextPage)) {
                        int begNum = param.getInteger("begNum");
                        param.put("begNum", begNum + ITransCodeConstant.QUERY_NUMBER_50);
                        param.put("queryExtend", responseBody.get("query_extend"));
                        queryMsg = buildReceiptQueryMsgUnNeedUkey(param, bankAccount);
                        signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
                        requestData = new ArrayList<>();
                        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
                        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
                        log.error("===================>回单拉取请求参数存在下一页begNum:" + param.get("begNum") + queryMsg);
                        result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, requestData, chanPayUri);
                        HttpsUtils.saveYQLBusinessLog(transCode, chanPayUri, queryMsg, result, param, bankAccount.getAccount());
                        log.error("===================>回单拉取请求结果存在下一页" + result);
                        String service_resp_code_next = result.getJSONObject(YQLConstant.DATA).getJSONObject("response_head").getString("service_resp_code");
                        if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, service_resp_code_next)) {
                            responseBody = result.getJSONObject(YQLConstant.DATA).getJSONObject(YQLConstant.RESPONSE_BODY);
                            insertReceiptDetail(enterpriseInfo, responseBody, uid);
                            nextPage = responseBody.getString("next_page");
                            param.put("queryExtend", responseBody.get("query_extend"));
                        } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                            // 解决多线程下的并发问题
                            ProcessUtil.dealYQLNodataMessage(param, uid, failAccountSet, accountId_currencyCode_key, responseHead);
                            nextPage = "0";
                        }else {
                            nextPage = "0";
                        }
                    }
                    param.put("begNum", 1);
                    param.put("queryExtend", "");
                    param.put("query_extend", "");
                    param.put("nextPage", false);
                    //ProcessUtil.refreshProcess(uid, true);
                } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                    // 解决多线程下的并发问题
                    ProcessUtil.dealYQLNodataMessage(param, uid, failAccountSet, accountId_currencyCode_key, responseHead);
                } else {
                    param.put("acct_no", bankAccount.getAccount());
                    errorInfo = YQLUtils.getYQLErrorMsqForManual(param, responseHead);
                }
            } else {
                errorInfo = YQLUtils.getYQLErrorMsqOfNetWork(result.getString("message"));
            }
        } catch (Exception e) {
            ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
            log.error("receiptBuildAndQueryUnNeedUkey错误，请求参数 = {},响应参数 = {},报错信息={}", queryMsg.toString(), result.toString(), e);
            errorInfo = YQLUtils.getErrorMsqWithAccount(e, ((EnterpriseBankAcctVO) param.get("enterpriseBankAcctVO")).getAccount());
        } finally {
            if (errorInfo != null) {
                ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                ProcessUtil.addYQLErrorMessage(uid, errorInfo);
            }
        }
    }

    /**
     * 银行交易回单手工下载
     *
     * @param param
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    public String downloadAccountReceiptDetailUrl(CtmJSONObject param, HttpServletResponse response) throws Exception {
        BankReconciliation bankReconciliation = null;
        String extendss = null;
        try {
            CtmJSONObject param_down = new CtmJSONObject();
            Map<String, Object> bankelereceipt = getBankEleReceipt(param.get("ids"));
            String fileNameEncoded = POIUtils.encode(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807EE", "银行电子回单") /* "银行电子回单" */, "UTF-8");
            Object existExtendss = bankelereceipt.get("extendss");
            if (existExtendss != null && !StringUtils.isEmpty((String) existExtendss)) {
                String filename = bankelereceipt.get("filename") == null ? fileNameEncoded + ".pdf" : bankelereceipt.get("filename").toString();
                //触发交易回单事件
                if (bankelereceipt.get("bankreconciliationid") != null) {
                    bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (bankelereceipt.get("bankreconciliationid")));
                    //下载时，重新发送关联事件
                    if (bankReconciliation != null) {
                        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, existExtendss.toString(), 1);
                        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, existExtendss.toString());
                    }
                }
                extendss = bankReceiptHandleDataService.handleExtendss(bankelereceipt);
                String url = cooperationFileUtilService.getFileBucketUrl(extendss);
//                String url = queryUrlByFileId((String) bankelereceipt.get("extendss"));
                return url;
            }
            param.put("customNo", bankelereceipt.get("custno"));
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceipt.get("enterpriseBankAccount").toString());
            if (enterpriseBankAcctVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100110"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100059", "回单下载失败：银行账号") /* "回单下载失败：银行账号" */ + bankelereceipt.get("enterpriseBankAccount").toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005A", "不可用,请检查!") /* "不可用,请检查!" */);
            }
            param.put("startDate", bankelereceipt.get("tranDate"));
            param.put("endDate", bankelereceipt.get("tranDate"));
            param.put("begNum", 1);

            param_down.put("customNo", bankelereceipt.get("custno"));
            param_down.put("billNo", bankelereceipt.get("receiptno"));
            param_down.put("billExtend", bankelereceipt.get("bill_extend"));
            CtmJSONObject postMsg_down = buildReceiptDownloadMsg(bankelereceipt, param_down, enterpriseBankAcctVO);
            log.error("==========================回单下载参数=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
            CtmJSONObject postResult_down = httpsService.doHttpsPost(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, postMsg_down, bankConnectionAdapterContext.getChanPayUri(), null);
            log.error("==========================回单下载返回结果=====================================>" + CtmJSONObject.toJSONString(postResult_down));
            if (!"0000".equals(postResult_down.getString("code"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
//                sendError(response, postResult_down.getString("message"));
            }
            CtmJSONObject responseHead_down = postResult_down.getJSONObject(YQLConstant.DATA).getJSONObject("response_head");
            String serviceStatus_down = responseHead_down.getString("service_status");
            if (!("00").equals(serviceStatus_down)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
//                sendError(response, ResultMessage.error(responseHead_down.getString("service_resp_code") + responseHead_down.getString("service_resp_desc")));
            }
            CtmJSONObject objs = postResult_down.getJSONObject(YQLConstant.DATA);
            CtmJSONObject responseBody_down = objs.getJSONObject(YQLConstant.RESPONSE_BODY);
            if (objs.getJSONObject(YQLConstant.RESPONSE_BODY) != null && responseBody_down.get(YQLConstant.RECORD) != null) {
                CtmJSONObject string = responseBody_down.getJSONObject(YQLConstant.RECORD);
                if (string != null && string.get("bill_file_content") != null) {
                    //为空时返回空字符串
                    String bill_file_content = ObjectUtils.toString(string.get("bill_file_content"));
                    String bill_file = ObjectUtils.toString(string.get("bill_file"));
                    //回单名称：取UUID[去重]+返回文件名[可能有业务含义]
                    //bill_file 去重
                    String bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
                    ////为规避文件名后缀截取问题 这里取返回文件名的最后一位
                    //String fileformat = getFileformat(bill_file);
                    byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                    String fileId = "";
                    try {
                        fileId = ossPoolClient.upload(b, bill_file_uniq);
                    } catch (Exception e) {//如果名称重复 重试一次
                        bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
                        fileId = ossPoolClient.upload(b, bill_file_uniq);
                    }
                    BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                    bankElectronicReceipt.setId(Long.parseLong(param.getString("ids")));
                    bankElectronicReceipt.setExtendss(fileId);
                    bankElectronicReceipt.setIsdown(true);
                    if (string.get("check_flag") != null) {// 校验标识
                        Integer checkFlag = Integer.valueOf(string.get("check_flag").toString());
                        bankElectronicReceipt.setSignStatus(SignStatus.find(checkFlag));
                    }
                    if (string.get("identifying_code") != null) {// 校验码
                        bankElectronicReceipt.setUniqueCode(string.get("identifying_code").toString());
                    }
                    bankElectronicReceipt.setFilename(bill_file_uniq);
                    bankElectronicReceipt.setEntityStatus(EntityStatus.Update);

                    MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                    return ossPoolClient.getFullUrl(fileId);
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
//                    sendError(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
//                sendError(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。") /* "银企连返回数据为空,下载回单失败。" */);
//            sendError(response,e.getMessage());
        } finally {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, extendss, 2);

            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, extendss);
        }
    }


    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setHeader("Content-disposition", "attachment;filename=\"" + new String(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00021", "银行电子回单") /* "银行电子回单" */.getBytes("UTF-8"), "ISO8859-1") + "error.txt\"");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/octet-stream");
        response.getOutputStream().write(message.getBytes());
    }

    /**
     * @return void
     * @Description 插入电子回单数据
     * @Date 2019/9/18
     * @Param [enterpriseInfo, responseBody]
     **/
    @Override
    public int insertReceiptDetail(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody, String uid) throws Exception {
        List<BankElectronicReceipt> bankElectronicReceipts = new ArrayList<>();
        List<BankReconciliation> bankRecords = new ArrayList<>();
        //获取币种缓存数据
        String currency = currencyQueryService.getCurrencyByAccount((String) enterpriseInfo.get("accountId"));
        Integer backNum = responseBody.getInteger("back_num");
        if (backNum == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400749", "必填字段back_num不能为空，请联系银企联补充！") /* "必填字段back_num不能为空，请联系银企联补充！" */);
        }
        ProcessUtil.totalPullCountAdd(uid, backNum);
        if (backNum == 1) {
            CtmJSONObject detailData = responseBody.getJSONObject(YQLConstant.RECORD);
            analysisReceiptDetailData(detailData, enterpriseInfo, bankElectronicReceipts, currency);
        } else if (backNum > 1) {
            CtmJSONArray records = responseBody.getJSONArray(YQLConstant.RECORD);
            for (int i = 0; i < records.size(); i++) {
                CtmJSONObject detailData = records.getJSONObject(i);
                analysisReceiptDetailData(detailData, enterpriseInfo, bankElectronicReceipts, currency);
            }
        }
        if (bankElectronicReceipts.size() == 0) {
            return 0;
        }
        CmpMetaDaoHelper.insert(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipts);
        ProcessUtil.newAddCountAdd(uid, bankElectronicReceipts.size());
        return bankElectronicReceipts.size();
    }

    /**
     * @return void
     * @Description 解析电子回单数据
     * @Date 2019/9/18
     * @Param [detailData, enterpriseInfo, bankDealDetails, bankRecords]
     **/
    private void analysisReceiptDetailData(CtmJSONObject detailData, Map<String, Object> enterpriseInfo,
                                           List<BankElectronicReceipt> bankElectronicReceipts, String currency) throws Exception {
        String bankSeqNo = detailData.getString("bank_seq_no");
        String billno = detailData.getString(YQLConstant.BILL_NO);
        /**
         * 电子回单验重逻辑
         * 原逻辑：本方账号、回单编号、交易流水号、
         * 现在：本方账号、回单编号、交易流水号、回单日期、金额、借贷方向
         */
//        boolean repetition = checkEleReceipt(enterpriseInfo.get("accountId"), bankSeqNo,billno);
        boolean repetition = checkEleReceipt(enterpriseInfo.get("accountId"), detailData);
        if (repetition) {
            return;
        }
        BankElectronicReceipt detail = new BankElectronicReceipt();
        detail.setCustno((String) enterpriseInfo.get("customNo"));
        detail.setTenant(AppContext.getTenantId());
        detail.setAccentity((String) enterpriseInfo.get("accEntityId"));
        detail.setEnterpriseBankAccount((String) enterpriseInfo.get("accountId"));
        Map<String, Object> bankAccountObject = QueryBaseDocUtils.queryEnterpriseBankAccountById((String) enterpriseInfo.get("accountId"));
        detail.setBanktype(bankAccountObject.get("bank").toString());
        String dateStr = detailData.getString("tran_date");
        Date tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
        detail.setTranDate(tranDate);
        String timeStr = detailData.getString("tran_time");
        if (StringUtils.isNotEmpty(timeStr)) {
            Date tranTime = DateUtils.dateParse(dateStr + timeStr, DateUtils.YYYYMMDDHHMMSS);
            detail.setTranTime(tranTime);
        }
        detail.setBankseqno(bankSeqNo);
        String toAcctNo = detailData.getString("to_acct_no");
        detail.setTo_acct_no(toAcctNo);
        String toAcctName = detailData.getString("to_acct_name");
        detail.setTo_acct_name(toAcctName);
        String toAcctBank = detailData.getString("to_acct_bank");
        detail.setTo_acct_bank(toAcctBank);
        String toAcctBankName = detailData.getString("to_acct_bank_name");
        detail.setTo_acct_bank_name(toAcctBankName);
        String currencyCode = detailData.getString("curr_code");
        if (StringUtils.isNotEmpty(currencyCode)) {
            currency = currencyQueryService.getCurrencyByCode(currencyCode);
            detail.setCurrency(currency);
        } else {
            detail.setCurrency(currency);
        }
        BigDecimal tranAmt = detailData.getBigDecimal("tran_amt");
        detail.setTran_amt(tranAmt);
        //新增利息字段
        BigDecimal interest = detailData.getBigDecimal("interest");
        detail.setInterest(interest);

        // 添加创建时间、创建日期
        detail.setCreateDate(new Date());
        detail.setCreateTime(new Date());

        String dcFlag = detailData.getString("dc_flag");
        if ("d".equalsIgnoreCase(dcFlag)) {
            detail.setDc_flag(Direction.Debit);
        } else if ("c".equalsIgnoreCase(dcFlag)) {
            detail.setDc_flag(Direction.Credit);
        } else {
            //CZFW-460731 王东方需求：放开这个限定。原本收付方向是为了进行关联；现在已经适配关联码了，这个限定没啥意义，还会阻断业务。取消此校验后，如果私有云没适配关联码，导致关联不上时，让客户适配关联码。
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100112"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00026", "银企联返回的借贷标识非法，请联系开发人员！") /* "银企联返回的借贷标识非法，请联系开发人员！" */);
        }
        String bill_no = detailData.getString(YQLConstant.BILL_NO);
        detail.setReceiptno(bill_no);
        String useName = detailData.getString("use_name");
        detail.setUse_name(useName);
        String remark = detailData.getString("remark");
        detail.setRemark(remark);
        String bill_extend = detailData.getString("bill_extend");
        detail.setBill_extend(bill_extend);
        detail.setEntityStatus(EntityStatus.Insert);
        detail.setId(ymsOidGenerator.nextId());
        //国际化新增字段
        detail.setRemark01(detailData.getString("remark01"));
        //20231030新增银行对账码
        detail.setBankcheckcode(detailData.getString("bank_check_code"));
        //记录唯一码
        detail.setYqlUniqueNo(detailData.getString(YQLUtils.UNIQUE_NO));
        detail.setDetailReceiptRelationCode(detailData.getString(YQLConstant.DETAIL_RECEIPT_RELATION_CODE));
        bankElectronicReceipts.add(detail);
    }

    /*
     *@Description 校验内部电子回单是否重复
     *@Date 2019/6/4 21:31
     *@Param [bankAccountId, bankSeqNo]
     *@Return boolean
     **/
    private boolean checkEleReceipt(Object bankAccountId, String bankSeqNo, String billno, Date tranDate) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        conditionGroup.appendCondition(QueryCondition.name("tranDate").eq(tranDate));
        if (bankSeqNo != null) {
            conditionGroup.appendCondition(QueryCondition.name("bankseqno").eq(bankSeqNo));
        }
        if (billno != null) {
            conditionGroup.appendCondition(QueryCondition.name("receiptno").eq(billno));
        } else {
            return true;
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> details = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
        if (details != null && details.size() > 0) {
            return true;
        }
        return false;
    }


    /*
     *@Description 校验直联电子回单是否重复
     *@Date 2019/6/4 21:31
     *@Param [bankAccountId, bankSeqNo]
     *@Return boolean
     **/
    private boolean newCheckEleReceipt(Object bankAccountId, BankElectronicReceipt bankElectronicReceipt) throws Exception {
        // 2024/3/28 10:31
        // 本方账号、回单编号、交易流水号、回单日期、金额、借贷方向
        // 本方账号
        String accountId = bankAccountId.toString();
        // 交易流水号
        String bankSeqNo = bankElectronicReceipt.getBankseqno();
        // 回单编号
        String billno = bankElectronicReceipt.getReceiptno();
        // 回单日期
        Date tranDate = bankElectronicReceipt.getTranDate();
        // 金额
        BigDecimal tranAmt = bankElectronicReceipt.getTran_amt();
        // 对方账号
        String toAcctNo = bankElectronicReceipt.getTo_acct_no();
        // 借贷方向
        Short dcFlagShort = bankElectronicReceipt.getDc_flag().getValue();

        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountId));
        if (StringUtils.isNotBlank(bankSeqNo)) {
            conditionGroup.appendCondition(QueryCondition.name("bankseqno").eq(bankSeqNo));
        }
        if (StringUtils.isNotBlank(billno)) {
            conditionGroup.appendCondition(QueryCondition.name("receiptno").eq(billno));
        } else {
            return true;
        }
        if (tranDate != null) {
            conditionGroup.appendCondition(QueryCondition.name("tranDate").eq(tranDate));
        }
        if (tranAmt != null) {
            conditionGroup.appendCondition(QueryCondition.name("tran_amt").eq(tranAmt));
        }
        if (dcFlagShort != null) {
            conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq(dcFlagShort));
        }
        if (toAcctNo != null) {
            conditionGroup.appendCondition(QueryCondition.name("to_acct_no").eq(toAcctNo));
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> details = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
        if (details != null && details.size() > 0) {
            return true;
        }
        return false;
    }

    /*
     *@Description 校验直联电子回单是否重复
     *@Date 2019/6/4 21:31
     *@Param [bankAccountId, bankSeqNo]
     *@Return boolean
     **/
    private boolean checkEleReceipt(Object bankAccountId, CtmJSONObject detailData) throws Exception {
        boolean repeat = false;
        String unique_no = detailData.getString(YQLConstant.UNIQUE_NO);
        Date receiptUniqueNoOpenDate = EnvConstant.RECEIPT_UNIQUE_NO_OPEN_DATE;
        if (StringUtils.isNotEmpty(unique_no)) {
            // 回单日期
            String dateStr = detailData.getString("tran_date");
            Date inputTanDate = null;
            if (StringUtils.isNotBlank(dateStr)) {
                inputTanDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
            }
            if(inputTanDate != null){

                String[] queryFieldNames = {ICmpConstant.UNIQUE_NO,  "id", BankElectronicReceipt.TRAN_DATE,
                        BankElectronicReceipt.TO_ACCT_NAME, BankElectronicReceipt.TO_ACCT_NO, BankElectronicReceipt.REMARK};

                List<BizObject> list = CmpMetaDaoHelper.queryColByOneEqualCondition(BankElectronicReceipt.ENTITY_NAME, ICmpConstant.UNIQUE_NO, unique_no, queryFieldNames);
                if (list != null && list.size() > 1) {
                    String format = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073E", "银企联唯一码[%s]的回单在库中存在重复数据！") /* "银企联唯一码[%s]的回单在库中存在重复数据！" */, unique_no);
                    log.error(format);
                    LogUtil.saveErrorBusinessLog(format, "unique_no", unique_no);
                }


                if (list != null && !list.isEmpty()) {
                    Date existTranDate = null;
                    Map<String, Object> firstItem = list.get(0);
                    if (firstItem != null) {
                        Object dateObj = firstItem.get(BankElectronicReceipt.TRAN_DATE);
                        if (dateObj instanceof Date) {
                            existTranDate = (Date) dateObj;
                        } else {
                            log.error("tranDate is not Date type, tranDate:{}", existTranDate);
                            existTranDate = null;
                        }
                    }
                    if (!Objects.equals(existTranDate, inputTanDate)) {
                        throw new CtmException("existTranDate and inputTanDate of same uniquo_no is not equal!");
                    }
                }

                // 交易日期大于开启唯一码时间，才启用唯一码校验
                if (inputTanDate != null && receiptUniqueNoOpenDate !=null && inputTanDate.after(receiptUniqueNoOpenDate)) {
                    if (CollectionUtils.isNotEmpty(list)) {
                        //只有unique_no有值时，才更新
                        updateBankElectronicReceipt(detailData, list);
                        repeat = true;
                    } else {
                        repeat = false;
                    }
                    return repeat;
                }
            }
        }

        // 2024/3/28 10:31
        // 本方账号、回单编号、交易流水号、回单日期、金额、借贷方向
        // 本方账号
        String accountId = bankAccountId.toString();
        // 交易流水号
        String bankSeqNo = detailData.getString("bank_seq_no");
        // 回单编号
        String billno = detailData.getString(YQLConstant.BILL_NO);
        // 回单日期
        String dateStr = detailData.getString("tran_date");
        Date tranDate = null;
        if (StringUtils.isNotBlank(dateStr)) {
            tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
        }
        // 金额
        BigDecimal tranAmt = detailData.getBigDecimal("tran_amt");
        // 对方账号
        String toAcctNo = detailData.getString("to_acct_no");
        // 借贷方向
        String dcFlag = detailData.getString("dc_flag");
        Short dcFlagShort = null;
        if (StringUtils.isNotBlank(dcFlag)) {
            if ("d".equalsIgnoreCase(dcFlag)) {
                dcFlagShort = 1;
            } else if ("c".equalsIgnoreCase(dcFlag)) {
                dcFlagShort = 2;
            }
        }

        QuerySchema schema = QuerySchema.create().addSelect("id,bankseqno,receiptno");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountId));
        if (StringUtils.isNotBlank(bankSeqNo)) {
            conditionGroup.appendCondition(QueryCondition.name("bankseqno").eq(bankSeqNo));
        }
        if (StringUtils.isNotBlank(billno)) {
            conditionGroup.appendCondition(QueryCondition.name("receiptno").eq(billno));
        } else {
            repeat = true;
        }
        if (tranDate != null) {
            conditionGroup.appendCondition(QueryCondition.name("tranDate").eq(tranDate));
        }
        if (tranAmt != null) {
            conditionGroup.appendCondition(QueryCondition.name("tran_amt").eq(tranAmt));
        }
        if (dcFlagShort != null) {
            conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq(dcFlagShort));
        }
        if (toAcctNo != null) {
            //todo  平台翻译有bug，空字符串eq会被翻译成null，导致重复拉单，在平台发包前，临时这样处理[ .in("")  ,  这样sql就是 where a in ('')]
            conditionGroup.appendCondition(QueryCondition.name("to_acct_no").in(toAcctNo));
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> details = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
        if (!CollectionUtils.isEmpty(details)) {
            // 数据库大小写不敏感，因此再次验重，区分仅大小写不同的数据;只要有一条这两字段都相同，则认为重复
            return details.stream().anyMatch(detail ->
                    Objects.equals(billno, detail.get("receiptno")) &&
                            Objects.equals(bankSeqNo, detail.get("bankseqno"))
            );
        }
        repeat = false;
        return repeat;
    }

    private static void updateBankElectronicReceipt(CtmJSONObject detailData, List<BizObject> list) throws Exception {
        BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
        bankElectronicReceipt.init(list.get(0));
        boolean needChange = false;
        String existToAcctName = bankElectronicReceipt.getTo_acct_name();
        String existToAcctNo = bankElectronicReceipt.getTo_acct_no();
        String existRemark = bankElectronicReceipt.getRemark();
        //如果以下字段在库里不为空，给bankElectronicReceipt给相应的值
        String to_acct_name = detailData.getString(YQLConstant.TO_ACCT_NAME);
        if (StringUtils.isEmpty(existToAcctName)) {
            bankElectronicReceipt.setTo_acct_name(to_acct_name);
            needChange = true;
        }
        String to_acct_no = detailData.getString(YQLConstant.TO_ACCT_NO);
        if (StringUtils.isEmpty(existToAcctNo)) {
            bankElectronicReceipt.setTo_acct_no(to_acct_no);
            needChange = true;
        }
        String remark = detailData.getString(YQLConstant.REMARK);
        if (StringUtils.isEmpty(existRemark)) {
            bankElectronicReceipt.setRemark(remark);
            needChange = true;
        }
        if (needChange) {
            CmpMetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
        }
    }

    /**
     * @return java.lang.String
     * @Author tongyd
     * @Description 构建交易流水号
     * @Date 2019/8/19
     * @Param [customNo]
     **/
    private String buildTranSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("T");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    /*
     * @Author tongyd
     * @Description 构建请求流水号
     * @Date 2019/9/12
     * @Param [customNo]
     * @return java.lang.String
     **/
    private String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    /*
     * @Author tongyd
     * @Description 获取电子回单信息
     * @Date 2019/9/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private Map<String, Object> getBankEleReceipt(Object id) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryOne(BankElectronicReceipt.ENTITY_NAME, schema);
    }

    /*
     * @Author yangjn
     * @Description 获取电子回单信息
     * @Date 2021/5/21
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private List<Map<String, Object>> getBankEleReceipts(List<String> ids) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,enterpriseBankAccount,receiptno,extendss,custno,tranDate,bill_extend,filename,enterpriseBankAccount.account as account");
        schema.addSelect(YQLUtils.YQL_UNIQUE_NO);
        schema.addSelect(BankElectronicReceipt.ISDOWN);
        schema.addSelect(BankElectronicReceipt.BANKRECONCILIATIONID);
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        schema.addOrderBy(new QueryOrderby("tranDate", "asc"));
        return MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
    }

    /*
     * @Author tongyd
     * @Description 获取电子回单实体
     * @Date 2019/9/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    @Override
    public Map<String, Object> getBankAccountSetting(Object bankAccountId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag,customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        //开通银企联服务；1-是
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        //状态[从企业银行账户是否启用同步过来的字段]；0-启用
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
    }


    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author yangjn
     * @Description 在无ukey情况下 构建电子回单报文
     * @Date 2021/5/8
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildReceiptQueryMsgUnNeedUkey(CtmJSONObject params, EnterpriseBankAcctVO bankAccount) throws Exception {
        String requestseqno = buildRequestSeqNo(params.get("customNo").toString());
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL,
                null,
                params.get("customNo").toString(),
                requestseqno,
                null,
                params.getString("channel"),
                false);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("curr_code", params.get("currencyCode"));
        requestBody.put("acct_name", bankAccount.getAcctName());
        requestBody.put("acct_no", bankAccount.getAccount());
        String startDate = params.getString("startDate").replaceAll("-", "").substring(0, 8);
        String endDate = params.getString("endDate").replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", params.get("begNum"));
        requestBody.put("query_num", ITransCodeConstant.QUERY_NUMBER_50);
        requestBody.put("query_extend", params.get("queryExtend"));
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    private CtmJSONObject buildRequloadestHead(String transCode, String oper, String customNo, String requestseqno, String signature, String channel, Boolean isBatch) {
        CtmJSONObject requestHead = new CtmJSONObject();
        if (isBatch) {
            requestHead.put("version", "2.1.1");
        } else
            requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", channel != null ? channel : bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        requestHead.put("oper", oper);
        return requestHead;
    }


    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建查电子回单下载报文
     * @Date 2019/8/7
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildReceiptDownloadMsg(Map<String, Object> bankelereceipt, CtmJSONObject params, EnterpriseBankAcctVO enterpriseBankAcctVO) throws Exception {
        String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(enterpriseBankAcctVO.getId(), params.get("customNo"));
        String requestseqno = buildRequestSeqNo(customNo);
        Map<String, Object> requestHead = buildRequloadestHead(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL,
                params.getString("operator"),
                customNo,
                requestseqno,
                params.getString("signature"),
                null,
                false);
        List<Map<String, String>> record = new ArrayList<Map<String, String>>();
        Map<String, String> record1 = new HashMap<String, String>();
        Map<String, Object> request_body = new HashMap<String, Object>();
        request_body.put("acct_name", enterpriseBankAcctVO.getAcctName());
        request_body.put("acct_no", enterpriseBankAcctVO.getAccount());
        record1.put(YQLConstant.BILL_NO, (String) params.get("billNo"));
        record1.put("bill_extend", (String) params.get("billExtend"));
        String uniqueNoStr = MapUtil.getStr(bankelereceipt, YQLUtils.YQL_UNIQUE_NO);
        if (StringUtils.isNotEmpty(uniqueNoStr)) {
            record1.put(YQLUtils.UNIQUE_NO, uniqueNoStr);
        }
        record.add(record1);
        request_body.put(YQLConstant.RECORD, record);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", request_body);
        return queryMsg;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author yangjn
     * @Description 构建查电子回单批量下载报文
     * @Date 2021/5/26
     * @Param [params, bankAccount, customNo]
     **/
    private CtmJSONObject buildReceiptDownloadMsgBatch(CtmJSONArray params, EnterpriseBankAcctVO enterpriseBankAcctVO, String customNo) throws Exception {
        String requestseqno = buildRequestSeqNo(customNo);
        Map<String, Object> requestHead = buildRequloadestHead(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL,
                null,
                customNo,
                requestseqno,
                null,
                null,
                true);
        List<Map<String, String>> record = new ArrayList<Map<String, String>>();
        Map<String, Object> request_body = new HashMap<String, Object>();
        request_body.put("acct_name", enterpriseBankAcctVO.getAcctName());
        request_body.put("acct_no", enterpriseBankAcctVO.getAccount());
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject param = params.getJSONObject(i);
            Map<String, String> recordOne = new HashMap<String, String>();
            recordOne.put(YQLConstant.BILL_NO, (String) param.get(YQLConstant.BILL_NO));
            recordOne.put("bill_extend", (String) param.get("bill_extend"));
            if (param.get(YQLUtils.UNIQUE_NO) != null) {
                recordOne.put(YQLUtils.UNIQUE_NO, (String) param.get(YQLUtils.UNIQUE_NO));
            }
            record.add(recordOne);
        }
        request_body.put(YQLConstant.RECORD, record);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", request_body);
        return queryMsg;
    }

    @Override
    public CtmJSONObject receiptDownForSpecial(CtmJSONObject param, HttpServletResponse response) throws Exception {
        BankReconciliation bankReconciliation = null;
        String fileId = "";
        try {
            CtmJSONObject result_down = new CtmJSONObject();
            CtmJSONObject param_down = new CtmJSONObject();
            List<String> urlList = new ArrayList<String>();
            Map<String, Object> bankelereceipt = getBankEleReceipt(param.get("ids"));
            String fileNameEncoded = UUID.randomUUID().toString();
            //如果有缓存信息 需要再文件服务器拉取
            if (bankelereceipt.get("extendss") != null && !StringUtils.isEmpty((String) bankelereceipt.get("extendss"))) {
                //触发交易回单事件
                if (bankelereceipt.get("bankreconciliationid") != null) {
                    bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (bankelereceipt.get("bankreconciliationid")));
                    if (bankReconciliation != null && StringUtils.isNotEmpty((String)bankelereceipt.get(BankElectronicReceipt.EXTENDSS))) {
                        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankelereceipt.get("extendss").toString(), 3);

                        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankelereceipt.get("extendss").toString());
                    }
                }
                // todo 取文件名 为空则默认pdf
                String filename = bankelereceipt.get("filename") == null ? fileNameEncoded + ".pdf" : bankelereceipt.get("filename").toString();
                // String extendss = (String) bankelereceipt.get("extendss");
                // 处理老的fileid
                String extendss = bankReceiptHandleDataService.handleExtendss(bankelereceipt);
                // String url = queryUrlByFileId(extendss);
                urlList.add(cooperationFileUtilService.getFileBucketUrl(extendss));
                result_down.put("receiptUrl", urlList);
                result_down.put("filename", filename);
                result_down.put("isBatch", false);
                return result_down;
            }
            param.put("customNo", bankelereceipt.get("custno"));
            EnterpriseBankAcctVO bankAccount = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceipt.get("enterpriseBankAccount").toString());
            if (bankAccount == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100110"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100059", "回单下载失败：银行账号") /* "回单下载失败：银行账号" */ + bankelereceipt.get("enterpriseBankAccount").toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005A", "不可用,请检查!") /* "不可用,请检查!" */);
            }
            param.put("startDate", bankelereceipt.get("tranDate"));
            param.put("endDate", bankelereceipt.get("tranDate"));
            param.put("begNum", 1);

            param_down.put("customNo", bankelereceipt.get("custno"));
            param_down.put("billNo", bankelereceipt.get("receiptno"));
            param_down.put("billExtend", bankelereceipt.get("bill_extend"));
            CtmJSONObject postMsg_down = buildReceiptDownloadMsg(bankelereceipt, param_down, bankAccount);
            log.error("==========================回单下载参数=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(postMsg_down.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", postMsg_down.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            CtmJSONObject postResult_down = HttpsUtils.doHttpsPostNew(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
            HttpsUtils.saveYQLBusinessLog(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, bankConnectionAdapterContext.getChanPayUri(), postMsg_down, postResult_down, param, Objects.toString(bankelereceipt.get(BankElectronicReceipt.RECEIPTNO)));
            log.error("==========================回单下载=====================================>" + CtmJSONObject.toJSONString(postResult_down));
            if (!"1".equals(postResult_down.getString("code"))) {
                result_down.put("ChanPayMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400743", "调用银企联报错：") /* "调用银企联报错：" */ + postResult_down.getString("message"));
                return result_down;
            }
            CtmJSONObject responseHead_down = postResult_down.getJSONObject(YQLConstant.DATA).getJSONObject("response_head");
            String serviceStatus_down = responseHead_down.getString("service_status");
            String service_resp_code = responseHead_down.getString(IStwbConstantForCmp.SERVICE_RESP_CODE);
            String yqlErrorMsqWithBillno = YQLUtils.getYQLErrorMsqWithBillno(responseHead_down, Objects.toString(bankelereceipt.get("receiptno"), ""));
            //增加对service_resp_code的判断
            if (!("00").equals(serviceStatus_down) || !(ITransCodeConstant.SERVICE_RESP_CODE).equals(service_resp_code)) {
                //result_down.put("ChanPayMsg", "银企联返回报错信息：" + responseHead_down.getString("service_resp_code") + responseHead_down.getString("service_resp_desc"));
                result_down.put("ChanPayMsg", yqlErrorMsqWithBillno);
                return result_down;
            }
            CtmJSONObject objs = postResult_down.getJSONObject(YQLConstant.DATA);
            CtmJSONObject responseBody_down = objs.getJSONObject(YQLConstant.RESPONSE_BODY);
            //新回单文件集合
            List<String> newReceiptList = new ArrayList<String>();
            if (responseBody_down != null && responseBody_down.get(YQLConstant.RECORD) != null) {
                CtmJSONObject receiptString = responseBody_down.getJSONObject(YQLConstant.RECORD);
                if (receiptString.get("bill_file_content") != null) {
                    String bill_file_content = (String) receiptString.get("bill_file_content");
                    String bill_file = (String) receiptString.get("bill_file");
                    //回单名称：取UUID[去重]+返回文件名[可能有业务含义]
                    //bill_file 去重
                    String bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
                    ////为规避文件名后缀截取问题 这里取返回文件名的最后一位
                    //String fileformat = getFileformat(bill_file);
                    byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                    newReceiptList.add(bill_file_content);
                    result_down.put("receiptMessage", newReceiptList);
                    result_down.put("isBatch", false);
                    //上传至文件服务器
                    try {
                        fileId = cooperationFileUtilService.uploadOfFileBytes(b, bill_file_uniq);
                    } catch (Exception e) {
                        bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
                        fileId = cooperationFileUtilService.uploadOfFileBytes(b, bill_file_uniq);
                    }
                    //查询银行回单
                    BankElectronicReceipt finalBankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, param.get("ids"));
                    //查询银行对账单
                    if (ObjectUtils.isNotEmpty(finalBankElectronicReceipt.getBankreconciliationid())) {
                        bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (finalBankElectronicReceipt.getBankreconciliationid()));
                        if (null != bankReconciliation && ObjectUtils.isNotEmpty(fileId) && finalBankElectronicReceipt.getIsdown().equals(false)) {
                            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 4);

                            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
                        }
                    }
                    BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                    bankElectronicReceipt.setId(Long.parseLong(param.getString("ids")));
                    bankElectronicReceipt.setExtendss(fileId);
                    bankElectronicReceipt.setFilename(bill_file_uniq);
                    bankElectronicReceipt.setIsdown(true);
                    result_down.put(ICmpConstant.FILE_ID, fileId);
                    result_down.put("filename", bill_file_uniq);
                    if (receiptString.get("check_flag") != null) {// 校验标识
                        Integer checkFlag = Integer.valueOf(receiptString.get("check_flag").toString());
                        bankElectronicReceipt.setSignStatus(SignStatus.find(checkFlag));
                    }
                    if (receiptString.get("identifying_code") != null) {// 校验码
                        bankElectronicReceipt.setUniqueCode(receiptString.get("identifying_code").toString());
                    }
                    bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                    //查询最新的银行回单
                    BankElectronicReceipt newBankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, (bankElectronicReceipt.getId()));
                    //查询银行对账单
                    if (ObjectUtils.isNotEmpty(newBankElectronicReceipt.getBankreconciliationid())) {
                        bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (newBankElectronicReceipt.getBankreconciliationid()));
                        if (null != bankReconciliation && ObjectUtils.isNotEmpty(fileId) &&
                                (newBankElectronicReceipt.getIsdown() == null || newBankElectronicReceipt.getIsdown().equals(false))) {
                            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 5);

                            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
                        }
                    }
                    //返回前端 进行下载操作
                    return result_down;
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100113"), yqlErrorMsqWithBillno + MessageUtils.getMessage("P_YS_FI_CM_1484691029474410548") /* "银企连返回数据为空,下载回单失败。" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100114"), yqlErrorMsqWithBillno + MessageUtils.getMessage("P_YS_FI_CM_1484691029474410548") /* "银企连返回数据为空,下载回单失败。" */);
            }
        } catch (Exception e) {
            log.error("银行电子回单下载异常", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100115"), e.getMessage());
        } finally {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 6);

            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
        }
    }

    /**
     * 获取文件的格式。
     * 此方法通过分割文件名和扩展名来提取文件格式（扩展名）。
     *
     * @param bill_file 需要分析的文件名，包括文件格式（扩展名）。
     * @return 文件的格式（扩展名），以字符串形式返回。
     */
    public String getFileformat(String bill_file) {
        // 使用点号分割文件名和扩展名
        String[] parts = bill_file.split("\\.");
        // 提取并返回扩展名
        String fileformat = "." + parts[parts.length - 1];
        return fileformat;
    }

    @Override
    public CtmJSONObject receiptDownBatchForSpecial(CtmJSONObject param, HttpServletResponse response) throws Exception {
        try {
            String id = ValueUtils.isNotEmptyObj(param.get("ids")) ? param.get("ids").toString() : null;
            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(id)) {
                throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005C", "请先选择具体单据!") /* "请先选择具体单据!" */);
            }
            // 20241101-【RPT0100-2 电子回单获取优化】取消下载条数限制，默认使用前端最大限制1000条兜底。
            List<String> idList = (List<String>) param.get("ids");
            int maxSize = 1000;
            if (idList.size() > maxSize) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100054"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F0", "选择的数据量需小于") /* "选择的数据量需小于" */ + maxSize);
            }
            //电子回单必有值 不需校验空
            List<Map<String, Object>> bankelereceipts = getBankEleReceipts(idList);
            //从未下载过的回单
            List<Map<String, Object>> bankelereceiptNew = new ArrayList<>();
            ////校验是否是同一个账户
            //List<String> accounts = new ArrayList<>();
            //for (Map<String, Object> bankelereceipt : bankelereceipts) {
            //    if (accounts.size() > 0 && !(accounts.contains(bankelereceipt.get("enterpriseBankAccount").toString()))) { //todo
            //        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100116"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_188C86CA05B00008", "请选择相同银行账户的电子回单。") /* "请选择相同银行账户的电子回单。" */);
            //    }
            //    accounts.add(bankelereceipt.get("enterpriseBankAccount").toString());
            //}
            Map<String, String> urlMap = new HashMap<>();
            String fileNameEncoded = UUID.randomUUID().toString();
            // 批量替换文件id和文件名称
            bankelereceipts = bankReceiptHandleDataService.handleBatchExtendss(bankelereceipts);
            Map<String, String> fileIdMap = cooperationFileUtilService.getFileBucketUrls(bankReceiptHandleDataService.getFileIds(bankelereceipts));
            String url;
            // 搜集返回值，总数，成功数，失败数，失败原因
            ResultVo resultVo = new ResultVo();
            resultVo.setCount(idList.size());
            for (Map<String, Object> bankelereceipt : bankelereceipts) {
                if (bankelereceipt.get("extendss") != null && !StringUtils.isEmpty((String) bankelereceipt.get("extendss"))) {
                    ////触发交易回单事件
                    //if (bankelereceipt.get("bankreconciliationid") != null) {
                    //    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (bankelereceipt.get("bankreconciliationid")));
                    //    if (bankReconciliation != null && StringUtils.isNotEmpty((String)bankelereceipt.get(BankElectronicReceipt.EXTENDSS))) {
                    //        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankelereceipt.get("extendss").toString(), 3);
                    //
                    //        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankelereceipt.get("extendss").toString());
                    //    }
                    //}
                    String filename = bankelereceipt.get("filename") == null ? fileNameEncoded + ".pdf" : bankelereceipt.get("filename").toString();
                    String extendss = (String) bankelereceipt.get("extendss");
                    if (fileIdMap == null) {
                        url = queryUrlByFileId(extendss);
                    } else {
                        url = fileIdMap.get(extendss);
                    }
                    String bill_file_uniq = UUID.randomUUID().toString() + "-" + filename;
                    urlMap.put(bill_file_uniq, url);
                } else {
                    //客户号不为空的可以调用银企接口下载回单文件
                    if (bankelereceipt.get("custno") != null) {
                        bankelereceiptNew.add(bankelereceipt);
                    } else {
                        // 多语词条
                        String receiptNoLang = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTM-CMP-MD_1808A9F004D007A4", "回单编号");
                        String customMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_1DDDB99404600006", "数据客户号为空，不支持银企联下载回单文件。");
                        this.addFail(resultVo, receiptNoLang + "：" + bankelereceipt.get("receiptno") + "，" + customMsg);//@notranslate
                    }
                }
            }
            Map<String, String> newReceiptMap = new HashMap<>();
            // 仅针对于走了银企联下载的数据，用于前端更新数据行信息
            Map<String, String> idToFileNameMap = new HashMap<>();
            //新回单集合，走银企联下载逻辑
            if (bankelereceiptNew.size() > 0) {
                //需要分账户向银企联发送请求
                //将bankelereceiptNew按照账户分组
                Map<String, List<Map<String, Object>>> bankelereceiptNewMap = bankelereceiptNew.stream().collect(Collectors.groupingBy(item -> item.get("enterpriseBankAccount").toString()));
                //获取bankelereceiptNewMap中的值，遍历执行银企联下载逻辑
                for (List<Map<String, Object>> list : bankelereceiptNewMap.values()) {
                    this.yqlDownFile(list, newReceiptMap, idToFileNameMap, resultVo, bankelereceipts);
                }
            }
            CtmJSONObject result_down = new CtmJSONObject();
            if (urlMap.size() > 0) {
                result_down.put("receiptUrl", urlMap);
            }
            if (newReceiptMap.size() > 0) {
                result_down.put("receiptMessage", newReceiptMap);
                result_down.put("idToFileNameMap", idToFileNameMap);
            }
            result_down.put("isBatch", true);
            // 将下载失败明细返回
            resultVo.setSucessCount(resultVo.getCount() - resultVo.getFailCount());
            result_down.put("resultVo", resultVo);
            //开新线程异步发送消息，避免前端超时
            ExecutorService service = ThreadPoolBuilder.buildThreadPoolByYmsParam(ThreadConstant.CMP_BANKRECEIPT_BATCHDOWNLOAD_SENDEVENT, ThreadConstant.CMP_BANKRECEIPT_BATCHDOWNLOAD_SENDEVENT_THREAD_YMS_PARAM_DEFAULT_VALUE);
            List<Map<String, Object>> finalBankelereceipts = bankelereceipts;
            service.execute(() -> {
                try {
                    //回单已下载时，仍然发送事件，提供一个补偿发送的时机
                    sendEvent(finalBankelereceipts);
                } catch (Exception e) {
                    log.error("回单下载时，事件发送失败：", e);
                }
            });
            return result_down;
        } catch (Exception e) {
            log.error("回单下载失败：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100117"), e.getMessage(), e);
        }
    }

    private void sendEvent(List<Map<String, Object>> bankelereceipts) throws Exception {
        for (Map<String, Object> bankelereceipt : bankelereceipts) {
            if (bankelereceipt.get("extendss") != null && !StringUtils.isEmpty((String) bankelereceipt.get("extendss"))) {
                //触发交易回单事件
                if (bankelereceipt.get("bankreconciliationid") != null) {
                    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (bankelereceipt.get("bankreconciliationid")));
                    if (bankReconciliation != null && StringUtils.isNotEmpty((String) bankelereceipt.get(BankElectronicReceipt.EXTENDSS))) {
                        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankelereceipt.get("extendss").toString(), 3);

                        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankelereceipt.get("extendss").toString());
                    }
                }
            }
        }
    }

    private void addFail(ResultVo resultVo, String failMsg) {
        resultVo.addFailCount();
        resultVo.getMessages().add(failMsg);
    }

    private void yqlDownFile(List<Map<String, Object>> bankelereceiptNew, Map<String, String> newReceiptMap,
                             Map<String, String> idToFileNameMap, ResultVo resultVo, List<Map<String, Object>> bankelereceipts) throws Exception {
        BankReconciliation bankReconciliation = null;
        String fileId = null;
        try {

            // 银企联下载回单文件
            CtmJSONArray recordArray = this.getYqlDownLoadRecord(bankelereceiptNew, resultVo, receiptNoLang);
            if (recordArray.isEmpty()) {
                // 未返回正常数据，表示全部统一失败（若有具体失败明细，则一定不为空）
                return;
            }

            List<BankElectronicReceipt> bankElectronicReceiptList = new ArrayList<>();
            for (int i = 0; i < bankelereceiptNew.size(); i++) {
                BankElectronicReceipt receiptVo = new BankElectronicReceipt();
                receiptVo.init(bankelereceiptNew.get(i));
                bankElectronicReceiptList.add(receiptVo);
            }
            BiMap<BankElectronicReceipt, CtmJSONObject> receiptId_record_map = matchBankreceiptAndRecord(recordArray, bankElectronicReceiptList);

            // 遍历更新数据并设置返回
            for (int i = 0; i < bankElectronicReceiptList.size(); i++) {
                BankElectronicReceipt receiptVo = bankElectronicReceiptList.get(i);
                CtmJSONObject receiptJson = receiptId_record_map.get(receiptVo);
                // 银企联传回的回单号信息，一定存在
                String bill_no = receiptJson.getString(YQLConstant.BILL_NO);
                if (bill_no == null) {
                    this.addFail(resultVo, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073C", "银企联返回回单编号[bill_no]为空!") /* "银企联返回回单编号[bill_no]为空!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073D", "回单编号:") /* "回单编号:" */ + "[" + receiptVo.getReceiptno() + "]" + YQLUtils.CONTACT_YQL_TIP);
                }
                // 校验数据是否下载成功，不成功直接跳过
                if (this.checkIsSkip(resultVo, bill_no, receiptNoLang, receiptJson)) {
                    continue;
                }

                // 走上传协同云器逻辑，并记录 bill_file_uniq 与 fileId
                String bill_file_content = (String) receiptJson.get("bill_file_content");
                this.uploadToXT(receiptJson, bill_file_content);
                String bill_file_uniq = receiptJson.getString("bill_file_uniq");
                fileId = receiptJson.getString(ICmpConstant.FILE_ID);


                //for (Map<String, Object> bankelereceiptMap : bankelereceipts) {
                //    BankElectronicReceipt receiptVo = new BankElectronicReceipt();
                //    receiptVo.init(bankelereceiptMap);
                //    if (!isMatch(receiptVo, recordArray, receiptJson)) {
                //        continue;
                //    }

                // 更新回单表信息
                // 查询银行对账单
                if (ObjectUtils.isNotEmpty(receiptVo.getBankreconciliationid())) {
                    bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (receiptVo.getBankreconciliationid()));
                    if (null != bankReconciliation && ObjectUtils.isNotEmpty(fileId)) {
                        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 7);

                        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
                    }
                }
                BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                bankElectronicReceipt.setId(receiptVo.getId());
                bankElectronicReceipt.setExtendss(fileId);
                bankElectronicReceipt.setFilename(bill_file_uniq);
                bankElectronicReceipt.setIsdown(true);
                // 批量只选一条的时候 也需要传filename给前端
                newReceiptMap.put(bill_file_uniq, bill_file_content);
                idToFileNameMap.put(bankElectronicReceipt.getId().toString(), bill_file_uniq);
                if (receiptJson.get("identifying_code") != null) {// 校验码
                    bankElectronicReceipt.setUniqueCode(receiptJson.get("identifying_code").toString());
                }
                bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                //}
            }


            //// 遍历更新数据并设置返回
            //for (int i = 0; i < recordArray.size(); i++) {
            //    CtmJSONObject receiptJson = recordArray.getJSONObject(i);
            //    // 银企联传回的回单号信息，一定存在
            //    String bill_no = receiptJson.getString(YQLConstant.BILL_NO);
            //    // 校验数据是否下载成功，不成功直接跳过
            //    if (this.checkIsSkip(resultVo, bill_no, receiptNoLang, receiptJson)) {
            //        continue;
            //    }
            //
            //    // 走上传协同云器逻辑，并记录 bill_file_uniq 与 fileId
            //    String bill_file_content = (String) receiptJson.get("bill_file_content");
            //    this.uploadToXT(receiptJson, bill_file_content);
            //    String bill_file_uniq = receiptJson.getString("bill_file_uniq");
            //    fileId = receiptJson.getString("fileId");
            //
            //    BankElectronicReceipt receiptVo = receiptId_record_map.inverse().get(receiptJson);
            //
            //    //for (Map<String, Object> bankelereceiptMap : bankelereceipts) {
            //    //    BankElectronicReceipt receiptVo = new BankElectronicReceipt();
            //    //    receiptVo.init(bankelereceiptMap);
            //    //    if (!isMatch(receiptVo, recordArray, receiptJson)) {
            //    //        continue;
            //    //    }
            //
            //    // 更新回单表信息
            //    // 查询银行对账单
            //    if (ObjectUtils.isNotEmpty(receiptVo.getBankreconciliationid())) {
            //        bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (receiptVo.getBankreconciliationid()));
            //        if (null != bankReconciliation && ObjectUtils.isNotEmpty(fileId) && receiptVo.getIsdown().equals(false)) {
            //            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
            //        }
            //    }
            //    BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
            //    bankElectronicReceipt.setId(receiptVo.getId());
            //    bankElectronicReceipt.setExtendss(fileId);
            //    bankElectronicReceipt.setFilename(bill_file_uniq);
            //    bankElectronicReceipt.setIsdown(true);
            //    // 批量只选一条的时候 也需要传filename给前端
            //    newReceiptMap.put(bill_file_uniq, bill_file_content);
            //    idToFileNameMap.put(bankElectronicReceipt.getId(), bill_file_uniq);
            //    if (receiptJson.get("identifying_code") != null) {// 校验码
            //        bankElectronicReceipt.setUniqueCode(receiptJson.get("identifying_code").toString());
            //    }
            //    bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
            //    MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
            //    //}
            //}

        } catch (Exception e) {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 8);

            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
            throw new CtmException( e.getMessage(), e);
        }

    }

    private BiMap<BankElectronicReceipt, CtmJSONObject> matchBankreceiptAndRecord(CtmJSONArray recordArray, List<BankElectronicReceipt> bankelereceipts) {
        BiMap<BankElectronicReceipt, CtmJSONObject> matchedMap = HashBiMap.create();

        // 遍历所有电子回单数据
        for (BankElectronicReceipt receiptVo : bankelereceipts) {

            String uniqueCode = receiptVo.getYqlUniqueNo();  // 获取唯一码
            String receiptNo = receiptVo.getReceiptno();      // 获取回单编号
            String billExtend = receiptVo.getBill_extend();    // 获取扩展信息

            List<CtmJSONObject> matchedRecords = new ArrayList<>();

            // Step 1: 使用唯一码匹配
            if (StringUtils.isNotBlank(uniqueCode)) {
                for (int i = 0; i < recordArray.size(); i++) {
                    CtmJSONObject record = recordArray.getJSONObject(i);
                    String recordUniqueCode = record.getString(YQLConstant.UNIQUE_NO);
                    if (uniqueCode.equals(recordUniqueCode)) {
                        matchedRecords.add(record);
                    }
                }

                // 如果唯一码匹配到多条记录，不报错，记录业务日志
                if (matchedRecords.size() > 1) {
                    LogUtil.saveBusinessLog(OperCodeTypes.invalid, "invalid", uniqueCode, "回单下载", "使用唯一码[" + uniqueCode + "]匹配到多条记录，可能存在重复数据！");
                    CtmJSONObject firstMatchRecord = matchedRecords.get(0);
                    //添加随机数，防止重复时，无法存入
                    firstMatchRecord.put(UUID.randomUUID().toString(), UUID.randomUUID());
                    matchedMap.put(receiptVo, firstMatchRecord);
                    continue;
                    //throw new CtmException("使用唯一码匹配到多条记录，请检查数据一致性！" + "唯一码：" + uniqueCode);
                }

                // 如果匹配到一条记录，加入结果 map
                if (matchedRecords.size() == 1) {
                    matchedMap.put(receiptVo, matchedRecords.get(0));
                    continue;
                }
            }

            // Step 2: 没有唯一码时，使用回单编号匹配
            if (StringUtils.isNotBlank(receiptNo)) {
                for (int i = 0; i < recordArray.size(); i++) {
                    CtmJSONObject record = recordArray.getJSONObject(i);
                    String recordReceiptNo = record.getString(YQLConstant.BILL_NO);
                    if (receiptNo.equals(recordReceiptNo)) {
                        matchedRecords.add(record);
                    }
                }

                // 如果回单编号匹配到多条记录，再结合扩展信息进一步筛选
                if (matchedRecords.size() > 1 && StringUtils.isNotBlank(billExtend)) {
                    List<CtmJSONObject> finalMatchedRecords = new ArrayList<>();
                    for (CtmJSONObject record : matchedRecords) {
                        String recordBillExtend = record.getString(YQLConstant.BILL_EXTEND);
                        if (billExtend.equals(recordBillExtend)) {
                            finalMatchedRecords.add(record);
                        }
                    }
                    matchedRecords = finalMatchedRecords;
                }

                // 如果仍然无法唯一匹配，报错
                if (matchedRecords.size() != 1) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074F", "根据") /* "根据" */ +
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074C", "唯一码：") /* "唯一码：" */ + uniqueCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074A", ", 回单编号：") /* ", 回单编号：" */ + receiptNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074B", ", 扩展信息：") /* ", 扩展信息：" */ + billExtend +
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400751", ", 匹配到") /* ", 匹配到" */ + matchedRecords.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400750", "个回单文件。") /* "个回单文件。" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400752", "下载失败！请联系银企联排查！") /* "下载失败！请联系银企联排查！" */);
                }

                // 成功匹配，放入 map
                matchedMap.put(receiptVo, matchedRecords.get(0));
            } else {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400736", "回单编号为空，无法进行匹配！") /* "回单编号为空，无法进行匹配！" */);
            }
        }

        return matchedMap;
    }

    private static boolean isMatch(BankElectronicReceipt receiptVo, CtmJSONArray recordArray, CtmJSONObject record) {
        String uniqueCode = receiptVo.getYqlUniqueNo(); // 获取唯一码
        String receiptNo = receiptVo.getReceiptno();   // 获取回单编号
        String billExtend = receiptVo.getBill_extend(); // 获取扩展信息

        List<CtmJSONObject> matchedRecords = new ArrayList<>();

        // 优先使用唯一码匹配
        if (StringUtils.isNotBlank(uniqueCode)) {
            String recordUniqueCode = record.getString(YQLConstant.UNIQUE_NO);
            if (uniqueCode.equals(recordUniqueCode)) {
                return true;
            }
        }

        if (StringUtils.isNotBlank(receiptNo) && StringUtils.isNotBlank(billExtend)) {
            // 如果没有唯一码，则使用回单编号+唯一码匹配
            String recordReceiptNo = record.getString(YQLConstant.BILL_NO);
            String recordBill_extend = record.getString(YQLConstant.BILL_EXTEND);
            if (receiptNo.equals(recordReceiptNo) && billExtend.equals(recordBill_extend)) {
                return true;
            }
        }

        //只有回单编号匹配时，需要检查是否有重复的
        if (StringUtils.isNotBlank(receiptNo) && StringUtils.isBlank(billExtend) && StringUtils.isBlank(record.getString("bill_extend"))) {

        }


        // 如果唯一码或回单编号匹配后有多条记录，再结合扩展信息进一步筛选
        if (matchedRecords.size() > 1 && StringUtils.isNotBlank(billExtend)) {
            List<CtmJSONObject> finalMatchedRecords = new ArrayList<>();
            for (CtmJSONObject record1 : matchedRecords) {
                String recordBillExtend = record1.getString("bill_extend");
                if (billExtend.equals(recordBillExtend)) {
                    finalMatchedRecords.add(record1);
                }
            }
            matchedRecords = finalMatchedRecords;
        }

        // 如果仍然无法唯一确定一条记录，则报错
        if (matchedRecords.size() != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100121"),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400748", "无法唯一确定回单记录，请检查数据一致性！") /* "无法唯一确定回单记录，请检查数据一致性！" */ +
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074C", "唯一码：") /* "唯一码：" */ + uniqueCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074A", ", 回单编号：") /* ", 回单编号：" */ + receiptNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074B", ", 扩展信息：") /* ", 扩展信息：" */ + billExtend +
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074D", ", 匹配结果数量：") /* ", 匹配结果数量：" */ + matchedRecords.size());
        }

        // 返回最终是否匹配成功
        return matchedRecords.size() == 1;
    }

    private void uploadToXT(CtmJSONObject receiptJson, String bill_file_content) {
        String bill_file = (String) receiptJson.get("bill_file");
        //回单名称：取UUID[去重]+返回文件名[可能有业务含义]
        //bill_file 去重
        String bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
        byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
        String fileId = "";
        try {
            fileId = cooperationFileUtilService.uploadOfFileBytes(b, bill_file_uniq);
        } catch (Exception e) {//如果名称重复 重试一次
            bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
            fileId = cooperationFileUtilService.uploadOfFileBytes(b, bill_file_uniq);
        }

        receiptJson.put("bill_file_uniq", bill_file_uniq);
        receiptJson.put(ICmpConstant.FILE_ID, fileId);
    }

    private boolean checkIsSkip(ResultVo resultVo, String bill_no, String receiptNoLang, CtmJSONObject receiptJson) {
        // 需要判断是失败明细还是成功数据
        // 当前数据下载状态
        String downStatus = receiptJson.getString("down_status");
        String resMessage = receiptJson.getString("res_message");
        //旧版本银企联不返回此字段，需要解析下载内容，不能跳过
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(downStatus)) {
            return false;
        }
        // 0-下载失败，1-下载成功，2-下载中
        if (YqlDownStatusEnum.DOWN_FAIL.getValue().equals(downStatus)) {
            String failMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DFA6AC005E80009", "银企联下载回单文件失败！");
            // 采用银企联返回的失败信息
            if (StringUtils.isEmpty(resMessage)) {
                resMessage = failMsg;
            }
            this.addFail(resultVo, receiptNoLang + "：" + bill_no + "，" + resMessage);//@notranslate
            return true;
        } else if (YqlDownStatusEnum.DOWNLOADING.getValue().equals(downStatus)) {
            // 银企联返回的：res_message = "文件下载中"
            // 下载中提示语：回单编号【XXX】，回单文件正在从银行拉取，请稍后重新下载！
            String failMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E0C471805C00004", "回单文件正在从银行拉取，请稍后重新下载！");
            this.addFail(resultVo, receiptNoLang + "【" + bill_no + "】，" + failMsg);//@notranslate
            return true;
        }

        if (receiptJson.get("bill_file_content") == null) {
            String failMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。");
            this.addFail(resultVo, receiptNoLang + "：" + bill_no + "，" + failMsg);//@notranslate
            return true;
        }

        return false;
    }

    private CtmJSONArray getYqlDownLoadRecord(List<Map<String, Object>> bankelereceiptNew, ResultVo resultVo, String receiptNoLang) throws Exception {
        Map<String, Object> bankelereceiptFirst = bankelereceiptNew.get(0);
        // 账号相同，获取统一客户号
        String customNo = bankelereceiptFirst.get("custno").toString();
        // 获取统一银企联账号
        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceiptFirst.get("enterpriseBankAccount").toString());
        if (enterpriseBankAcctVO == null) {
            // 统一组装异常失败返回值
            String failMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100059", "回单下载失败：银行账号") /* "回单下载失败：银行账号" */ + bankelereceiptFirst.get("enterpriseBankAccount").toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005A", "不可用,请检查!") /* "不可用,请检查!" */;
            this.setAllFail(bankelereceiptNew, resultVo, failMsg, receiptNoLang, null);
            return new CtmJSONArray();
        }

        // 统计所有成功返回或失败明细的record
        CtmJSONArray resArr = new CtmJSONArray();
        if (bankelereceiptNew.size() > 10) {
            // 分组下载, 每组最大10
            List<List<Map<String, Object>>> lists = com.google.common.collect.Lists.partition(bankelereceiptNew, 10);
            List<CompletableFuture<CtmJSONObject>> futureList = lists.stream()
                    .map(v -> CompletableFuture.supplyAsync(() -> {
                        // 获取单次请求流水号
                        String requestseqno = this.buildRequestSeqNo(customNo);
                        return bankReceiptYqlBatchDownService.batchDown(v, requestseqno, enterpriseBankAcctVO);
                    }, executorService))
                    .collect(Collectors.toList());
            // 等待任务执行完成
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();

            // 获取结果 & 解析
            for (int i = 0; i < futureList.size(); i++) {
                CtmJSONObject recordResult = futureList.get(i).get();
                this.analysisRecord(recordResult, resultVo, resArr, lists.get(i), receiptNoLang);
            }
        } else {
            // 数据小于等于10条
            String requestseqno = this.buildRequestSeqNo(customNo);
            CtmJSONObject recordResult = bankReceiptYqlBatchDownService.batchDown(bankelereceiptNew, requestseqno, enterpriseBankAcctVO);
            this.analysisRecord(recordResult, resultVo, resArr, bankelereceiptNew, receiptNoLang);
        }

        return resArr;
    }

    private void setAllFail(List<Map<String, Object>> bankelereceiptNew, ResultVo resultVo, String allFailMsg, String receiptNoLang, CtmJSONObject downResult) {
        Boolean hasDetailMsg = false;

        if (downResult == null) {
            return;
        }

        CtmJSONObject objs = downResult.getJSONObject(YQLConstant.DATA);
        if (objs == null) {
            return;
        }

        CtmJSONObject responseBody_down = objs.getJSONObject(YQLConstant.RESPONSE_BODY);
        if (responseBody_down == null) {
            return;
        }

        CtmJSONArray recordArray = responseBody_down.getJSONArray(YQLConstant.RECORD);
        if (recordArray == null) {
            return;
        }

        String failMsgDefault = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DFA6AC005E80009", "银企联下载回单文件失败！");
        String downloadingMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E0C471805C00004", "回单文件正在从银行拉取，请稍后重新下载！");
        //String emptyContentMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。");

        for (int i = 0; i < recordArray.size(); i++) {
            CtmJSONObject receiptJson = recordArray.getJSONObject(i);
            if (receiptJson == null) {
                continue;
            }

            String downStatus = receiptJson.getString("down_status");
            String resMessage = receiptJson.getString("res_message");
            Object billNo = receiptJson.get(YQLConstant.BILL_NO);

            if (YqlDownStatusEnum.DOWN_FAIL.getValue().equals(downStatus)) {
                String failMsg = StringUtils.isEmpty(resMessage) ? failMsgDefault : resMessage;
                this.addFail(resultVo, String.format("%s【%s】，%s", receiptNoLang, billNo, failMsg));//@notranslate
                hasDetailMsg = true;
            } else if (YqlDownStatusEnum.DOWNLOADING.getValue().equals(downStatus)) {
                this.addFail(resultVo, String.format("%s【%s】，%s", receiptNoLang, billNo, downloadingMsg));//@notranslate
                hasDetailMsg = true;
            }
            if (!hasDetailMsg) {
                //没有详细错误信息时，才返回请求头的错误信息
                this.addFail(resultVo, allFailMsg);
            }

        //if (receiptJson.get("bill_file_content") == null) {
        //    this.addFail(resultVo, String.format("%s【%s】，%s", receiptNoLang, billNo, emptyContentMsg));
        //}
    }
}


    private void analysisRecord(CtmJSONObject downResult, ResultVo resultVo, CtmJSONArray resArr,
                                List<Map<String, Object>> receiptList, String receiptNoLang) {
        if (!"1".equals(downResult.getString("code"))) {
            // 这一批次数据统一失败
            String failMsg = downResult.getString("message");
            this.setAllFail(receiptList, resultVo, failMsg, receiptNoLang, downResult);
            return;
        }

        CtmJSONObject responseHead_down = downResult.getJSONObject(YQLConstant.DATA).getJSONObject("response_head");
        String serviceStatus_down = responseHead_down.getString("service_status");
        if (!("00").equals(serviceStatus_down)) {
            // 这一批次数据统一失败
            String failMsg = responseHead_down.getString("service_resp_code") + responseHead_down.getString("service_resp_desc");
            this.setAllFail(receiptList, resultVo, failMsg, receiptNoLang, downResult);
            return;
        }

        CtmJSONObject objs = downResult.getJSONObject(YQLConstant.DATA);
        CtmJSONObject responseBody_down = objs.getJSONObject(YQLConstant.RESPONSE_BODY);
        if (responseBody_down == null || responseBody_down.getJSONArray(YQLConstant.RECORD) == null) {
            // 未返回数据，这一批次数据统一失败，组装失败数据
            String failMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807ED", "银企连返回数据为空,下载回单失败。");
            this.setAllFail(receiptList, resultVo, failMsg, receiptNoLang, downResult);
            return;
        }
        // 保存成功数据，若有失败明细，也需要放进去，（record包含失败明细）
        resArr.addAll(responseBody_down.getJSONArray(YQLConstant.RECORD));
    }

    /**
     * 定时任务 获取下载银行交易回单 并上传到服务器
     *
     * @param param
     * @throws Exception
     */
    @Override
    public void downloadAccountReceiptTask(Map<String, Object> param, Boolean cooperationFileService) throws Exception {
        if (null != param && null != param.get(ICmpConstant.ID)) {
            String id = param.get(ICmpConstant.ID).toString();
            String key = "downloadAccountReceiptTask" + AppContext.getTenantId() + id;
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
            try {
                if (ymsLock != null) {
                    CtmJSONObject param_down = new CtmJSONObject();
                    Map<String, Object> bankelereceipt = getBankEleReceipt(param.get("id"));
                    if (null != bankelereceipt && (null == bankelereceipt.get("extendss") || StringUtils.isEmpty((String) bankelereceipt.get("extendss")))) {
                        EnterpriseBankAcctVO enterpriseBankAcctVO = getEnterpriseBankAcctVO(bankelereceipt);
                        if (enterpriseBankAcctVO == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100110"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100059", "回单下载失败：银行账号") /* "回单下载失败：银行账号" */ + bankelereceipt.get("enterpriseBankAccount").toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005A", "不可用,请检查!") /* "不可用,请检查!" */);
                        }
                        param_down.put("customNo", bankelereceipt.get("custno"));
                        param_down.put("billNo", bankelereceipt.get("receiptno"));
                        param_down.put("billExtend", bankelereceipt.get("bill_extend"));
                        CtmJSONObject postMsg_down = buildReceiptDownloadMsg(bankelereceipt, param_down, enterpriseBankAcctVO);
                        log.error("==========================downloadAccountReceiptTask down param=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
                        CtmJSONObject postResult_down = httpsService.doHttpsPost(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, postMsg_down, bankConnectionAdapterContext.getChanPayUri(), null);
                        log.error("==========================downloadAccountReceiptTask down=====================================>" + CtmJSONObject.toJSONString(postResult_down));
                        if ("0000".equals(postResult_down.getString("code"))) {
                            CtmJSONObject responseHead_down = postResult_down.getJSONObject(YQLConstant.DATA).getJSONObject("response_head");
                            String serviceStatus_down = responseHead_down.getString("service_status");
                            if (("00").equals(serviceStatus_down)) {
                                CtmJSONObject objs = postResult_down.getJSONObject(YQLConstant.DATA);
                                CtmJSONObject responseBody_down = objs.getJSONObject(YQLConstant.RESPONSE_BODY);
                                if (responseBody_down.get(YQLConstant.RECORD) != null) {
                                    CtmJSONObject receiptString = responseBody_down.getJSONObject(YQLConstant.RECORD);
                                    if (receiptString.get("bill_file_content") != null) {
                                        String bill_file_content = (String) receiptString.get("bill_file_content");
                                        //todo 取文件名上传
                                        String bill_file = (String) receiptString.get("bill_file");
                                        //回单名称：取UUID[去重]+返回文件名[可能有业务含义]
                                        //bill_file 去重
                                        String bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
                                        ////为规避文件名后缀截取问题 这里取返回文件名的最后一位
                                        //String fileformat = getFileformat(bill_file);
                                        byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                                        String fileId = "";
                                        //若为true 说明是专属化文件服务器
                                        if (cooperationFileService) {
                                            fileId = cooperationFileUtilService.uploadOfFileBytes(b, bill_file_uniq);
                                        } else {
                                            try {
                                                fileId = ossPoolClient.upload(b, bill_file_uniq);
                                            } catch (Exception e) {//如果名称重复 重试一次
                                                bill_file_uniq = UUID.randomUUID().toString() + '_' + bill_file;
                                                fileId = ossPoolClient.upload(b, bill_file_uniq);
                                            }
                                        }
                                        updateBankElectronicReceipt(id, fileId, bill_file_uniq, receiptString);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw e;
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
    }

    /**
     * 更新银行交易回单extendss
     *
     * @param id
     * @param fileId
     * @throws Exception
     */
    private void updateBankElectronicReceipt(String id, String fileId, String bill_file_uniq, CtmJSONObject receiptString) throws Exception {
        BankElectronicReceipt finalBankElectronicReceipt = null;
        BankReconciliation bankReconciliation = null;
        try {
            BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
            bankElectronicReceipt.setId(Long.parseLong(id));
            bankElectronicReceipt.setExtendss(fileId);
            bankElectronicReceipt.setIsdown(true);
            //更新文件名
            bankElectronicReceipt.setFilename(bill_file_uniq);
            if (receiptString != null) {
                if (receiptString.get("check_flag") != null) {// 校验标识
                    Integer checkFlag = Integer.valueOf(receiptString.get("check_flag").toString());
                    bankElectronicReceipt.setSignStatus(SignStatus.find(checkFlag));
                }
                if (receiptString.get("identifying_code") != null) {// 校验码
                    bankElectronicReceipt.setUniqueCode(receiptString.get("identifying_code").toString());
                }
            }
            bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
            //查询最新的银行回单
            finalBankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, (bankElectronicReceipt.getId()));
            //查询银行对账单
            if (ObjectUtils.isNotEmpty(finalBankElectronicReceipt.getBankreconciliationid())) {
                bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (finalBankElectronicReceipt.getBankreconciliationid()));
                if (null != bankReconciliation && null != finalBankElectronicReceipt.getExtendss()) {
                    LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, finalBankElectronicReceipt.getExtendss(), 9);

                    bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, finalBankElectronicReceipt.getExtendss());
                }
            }
        } catch (Exception e) {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, finalBankElectronicReceipt.getExtendss(), 10);

            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, finalBankElectronicReceipt.getExtendss());
        }

    }

    /**
     * 获取EnterpriseBankAcctVO
     *
     * @param bankelereceipt
     * @return
     * @throws Exception
     */
    @Override
    public EnterpriseBankAcctVO getEnterpriseBankAcctVO(Map<String, Object> bankelereceipt) throws Exception {
        String enterpriseBankAccount = bankelereceipt.get("enterpriseBankAccount").toString();
        String enterpriseBankAcctKey = "downloadAccountReceiptTask_" + InvocationInfoProxy.getTenantid() + enterpriseBankAccount;
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankAcctVOCache.getIfPresent(enterpriseBankAcctKey);
        if (null == enterpriseBankAcctVO) {
            enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(enterpriseBankAccount);
            if (enterpriseBankAcctVO != null) {
                enterpriseBankAcctVOCache.put(enterpriseBankAcctKey, enterpriseBankAcctVO);
            }
        }
        return enterpriseBankAcctVO;
    }

    @Override
    public byte[] downloadAccountReceipt(String extendss) throws Exception {
//        return ossPoolClient.download(extendss);
        if (extendss != null && extendss.contains("/")) {
            return ossPoolClient.download(extendss);
        } else {
            return cooperationFileUtilService.queryBytesbyFileid(extendss);
        }
    }

    @Override
    public void receiptUploadFile(CtmJSONObject param, HttpServletResponse response) throws Exception {
        try {
            //回单字节流
            String bill_file_content = (String) param.get("bill_file_content");
            //回单文件名称
            String fileName = (String) param.get("file_name");
            //mysql数据库不区分大小写，orcale数据库区分大小写。此处不应区分大小写，所以都查下
            //把文件名分为前后两部分，后缀转为大写和小写，然后组装新的两个文件名
            String lowerFileName = CooperationFileUtilService.buildLowerSuffixFileName(fileName);
            String upperFileName = CooperationFileUtilService.buildUpperSuffixFileName(fileName);
            //根据回单文件名称查询对应数据  绑定id
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("filename").in(Arrays.asList(lowerFileName, upperFileName, fileName)));
            queryInitDataSchema.addCondition(conditionGroup);
            List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryInitDataSchema, null);
            if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100119"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400740", "回单文件名称【%s】，在回单明细数据—电子回单文件中未查找到相同名称的数据，请检查！") /* "回单文件名称【%s】，在回单明细数据—电子回单文件中未查找到相同名称的数据，请检查！" */, fileName));
            }
            BankElectronicReceipt bankvo = bankElectronicReceiptList.get(0);
            if (bankvo.getIsdown() != null && bankvo.getIsdown().booleanValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100120"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00025", "选择导入的回单文件已存在，检查后再导入") /* "选择导入的回单文件已存在，检查后再导入" */);
            }
            byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
            String fileId = "";
            try {
                fileId = ossPoolClient.upload(b, fileName);
            } catch (Exception e) {//如果名称重复 加个前缀
                fileName = UUID.randomUUID().toString() + '_' + fileName;
                fileId = ossPoolClient.upload(b, fileName);
            }
            //更新交易回单信息
            updateBankElectronicReceipt(bankvo.getId(), fileId, fileName, null);
        } catch (Exception e) {
            log.error("导入电子回单文件失败：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100121"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100065", "导入电子回单文件失败！") /* "导入电子回单文件失败！" */);
        }
    }

    @Override
    public CtmJSONObject getReceiptIdByFilename(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("code", 200);
        try {
            //回单文件名称
            String fileName = (String) param.get("file_name");
            //mysql数据库不区分大小写，orcale数据库区分大小写。此处不应区分大小写，所以都查下
            //把文件名分为前后两部分，后缀转为大写和小写，然后组装新的两个文件名
            String lowerFileName = CooperationFileUtilService.buildLowerSuffixFileName(fileName);
            String upperFileName = CooperationFileUtilService.buildUpperSuffixFileName(fileName);
            //根据回单文件名称查询对应数据  绑定id
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("filename").in(Arrays.asList(lowerFileName, upperFileName, fileName)));
            queryInitDataSchema.addCondition(conditionGroup);
            List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryInitDataSchema, null);
            if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                responseMsg.put("message", String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400740", "回单文件名称【%s】，在回单明细数据—电子回单文件中未查找到相同名称的数据，请检查！") /* "回单文件名称【%s】，在回单明细数据—电子回单文件中未查找到相同名称的数据，请检查！" */, fileName));
                responseMsg.put("code", 500);
                return responseMsg;
            }
            BankElectronicReceipt bankvo = bankElectronicReceiptList.get(0);
            if (bankvo.getIsdown() != null && bankvo.getIsdown().booleanValue()) {
                responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00027", "选择导入的回单文件已存在，检查后再导入!") /* "选择导入的回单文件已存在，检查后再导入!" */);
                responseMsg.put("code", 500);
                return responseMsg;
            }
            responseMsg.put("id", bankvo.getId());
        } catch (Exception e) {
            log.error("银行交易回单文件查询失败：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100122"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100064", "银行交易回单文件查询失败！") /* "银行交易回单文件查询失败！" */);
        }
        return responseMsg;
    }

    @Override
    public CtmJSONObject receiptAssociationFileId(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        BankElectronicReceipt bankElectronicReceipt = null;
        BankReconciliation bankReconciliation = null;
        responseMsg.put("code", 200);
        try {
            //upload关联   delete取消关联
            Object type = param.get("type");
            //上传回单文件的id
            String fileId = (String) param.get("file_id");
            if (type != null && "delete".equals(type)) {
                QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.addCondition(QueryCondition.name("extendss").eq(fileId));
                queryInitDataSchema.addCondition(conditionGroup);
                List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryInitDataSchema, null);
                if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80002", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                //取消关联关系
                bankElectronicReceipt = new BankElectronicReceipt();
                bankElectronicReceipt.setId(bankElectronicReceiptList.get(0).getId());
                bankElectronicReceipt.setExtendss("");
                bankElectronicReceipt.setIsdown(false);
                bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                //根据银行交易回单查询银行对账单
                if (bankElectronicReceiptList.get(0).getBankreconciliationid() != null) {
                    bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankElectronicReceiptList.get(0).getBankreconciliationid());
                    if (bankReconciliation != null) {
                        //设置更新状态，设置回单关联状态为未关联
                        if (ObjectUtils.isNotEmpty(bankElectronicReceiptList.get(0).getExtendss())) {
                            bankreconciliationService.cancelUrl(bankReconciliation.getId(), bankElectronicReceiptList.get(0).getExtendss());
                        }
                    }
                }
            } else {
                //交易回单的id
                String id = param.getString("id");
                QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.addCondition(QueryCondition.name("id").eq(id));
                queryInitDataSchema.addCondition(conditionGroup);
                List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryInitDataSchema, null);
                if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80002", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                //filename不改
                String bill_file_uniq = bankElectronicReceiptList.get(0).get("filename");
                //更新交易回单信息
                updateBankElectronicReceipt(id, fileId, bill_file_uniq, null);
                //查出最新的银行交易回单
                bankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, id);
                //根据银行交易回单查询银行对账单
                if (bankElectronicReceipt.getBankreconciliationid() != null) {
                    bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankElectronicReceipt.getBankreconciliationid());
                    if (bankReconciliation != null && ObjectUtils.isNotEmpty(bankElectronicReceipt.getExtendss())) {
                        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankElectronicReceipt.getExtendss(), 11);

                        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankElectronicReceipt.getExtendss());
                    }
                }
            }
        } catch (Exception e) {
            log.error("银行交易回单关联失败：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100058"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100066", "银行交易回单关联失败！") /* "银行交易回单关联失败！" */);
        } finally {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankElectronicReceipt.getExtendss(), 12);

            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankElectronicReceipt.getExtendss());
        }
        return responseMsg;
    }

    @Override
    public CtmJSONObject receiptPreviewFile(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("code", 200);
        try {
            String origin = param.getString("origin");
            String id = param.getString("id");
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("id").eq(id));
            queryInitDataSchema.addCondition(conditionGroup);
            if ("electronicStatementConfirm".equals(origin)) {
                List<ElectronicStatementConfirm> bankElectronicReceiptList = MetaDaoHelper.queryObject(ElectronicStatementConfirm.ENTITY_NAME, queryInitDataSchema, null);
                if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80002", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                ElectronicStatementConfirm bankvo = bankElectronicReceiptList.get(0);
                if (null == bankvo || StringUtils.isBlank(bankvo.getStatementfileid())) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80003", "当前单据未关联附件信息，不能预览!") /* "当前单据未关联附件信息，不能预览!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                if (StringUtils.isNotEmpty(bankvo.getStatementfileid())) {
                    if (bankvo.getStatementfileid().contains("/")) {
                        //String url = ossPoolClient.getFullUrl(bankvo.getExtendss());
                        String url = cooperationFileUtilService.getFileBucketUrl(bankReceiptHandleDataService.handleExtendss(bankvo.getStatementfileid(), bankvo.getId()));
                        responseMsg.put("fullUrl", url);
                        responseMsg.put("filename", bankvo.getStatement_name());
                    } else {
                        List<String> fileIds = new ArrayList<>();
                        fileIds.add(bankvo.getStatementfileid());
                        String url = cooperationFileUtilService.queryDownloadUrl(fileIds);
                        //根据文件id获取预览的url
                        String previewUrl = cooperationFileUtilService.queryPreviewUrlById(bankvo.getStatementfileid());
                        responseMsg.put("previewUrl", previewUrl);
                        responseMsg.put("fullUrl", url);
                        responseMsg.put("filename", bankvo.getStatement_name());
                    }
                }
            } else {
                List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryInitDataSchema, null);
                if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80002", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                BankElectronicReceipt bankvo = bankElectronicReceiptList.get(0);
                if (null == bankvo || StringUtils.isBlank(bankvo.getExtendss())) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80003", "当前单据未关联附件信息，不能预览!") /* "当前单据未关联附件信息，不能预览!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                if (StringUtils.isNotEmpty(bankvo.getExtendss())) {
                    if (bankvo.getExtendss().contains("/")) {
                        //String url = ossPoolClient.getFullUrl(bankvo.getExtendss());
                        String url = cooperationFileUtilService.getFileBucketUrl(bankReceiptHandleDataService.handleExtendss(bankvo.getExtendss(), bankvo.getId()));
                        responseMsg.put("fullUrl", url);
                    } else {
                        List<String> fileIds = new ArrayList<>();
                        fileIds.add(bankvo.getExtendss());
                        String url = cooperationFileUtilService.queryDownloadUrl(fileIds);
                        //根据文件id获取预览的url
                        String previewUrl = cooperationFileUtilService.queryPreviewUrlById(bankvo.getExtendss());
                        responseMsg.put("previewUrl", previewUrl);
                        responseMsg.put("fullUrl", url);
                    }
                }
            }
        } catch (Exception e) {
            log.error("回单预览打印失败：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100120"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005D", "回单预览打印失败！") /* "回单预览打印失败！" */ + e.getMessage(), e);
        }
        return responseMsg;
    }

    @Override
    public CtmJSONArray batchreceiptPreviewFile(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        CtmJSONObject responseMsg = new CtmJSONObject();
        try {
            List<String> ids = (List<String>) param.get("ids");
            String operation = (String) param.get("operation");

            if (!ValueUtils.isNotEmptyObj(ids)) {
                responseMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1713352554276454502") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                responseMsg.put("code", 500);
                records.add(responseMsg);
                return records;
            }
            if (ids.size() > 100) {
                throw new CtmException(CtmExceptionConstant.GREATER_THAN_MAX_NUM + EnvConstant.CMP_PRINT_RECEIPT_MAXNUM);
            }
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("id").in(ids));
            queryInitDataSchema.addCondition(conditionGroup);
            List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryInitDataSchema, null);
            if (null == bankElectronicReceiptList || bankElectronicReceiptList.size() < 1) {
                responseMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1713352554276454502") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                responseMsg.put("code", 500);
                records.add(responseMsg);
                return records;
            }
            List<BankElectronicReceipt> bankReceiptSortList = new ArrayList<>();
            Map<Object, BankElectronicReceipt> bankReceiptMap = bankElectronicReceiptList.stream().collect(Collectors.toMap(BankElectronicReceipt::getId, e -> e));
            for (String id : ids) {
                bankReceiptSortList.add(bankReceiptMap.get(new Long(id)));
            }
            //开多线程处理，防止前端超时
            ExecutorService service = ThreadPoolBuilder.buildThreadPoolByYmsParam("cmpPrintReceiptToImageTask", EnvConstant.CMP_PRINTRECEIPTTOIMAGE_TASK_DEFAULT_VALUE);
            ThreadResult threadResult = ThreadPoolUtil.executeByBatchCollectResultsNoSemaphore(service, bankReceiptSortList, EnvConstant.CMP_PRINT_RECEIPTTOIMAGETASK_BATCH_COUNT, "cmpPrintReceiptToImageTask", true, (int fromIndex, int toIndex) -> {
                ThreadResult subThreadResult = new ThreadResult();
                for (int t = fromIndex; t < toIndex; t++) {
                    BankElectronicReceipt bankvo = bankReceiptSortList.get(t);
                    CtmJSONObject ctmJSONObject = new CtmJSONObject();
                    try {
                        ctmJSONObject = getReceiptPreviewFile(bankvo, operation);
                    } catch (Exception e) {
                        log.error("回单预览打印失败：" + "[receiptno]" + bankvo.getReceiptno() + e.getMessage(), e);
                        subThreadResult.getErrorReturnList().add("[receiptno]" + bankvo.getReceiptno() + e.getMessage());
                    }
                    if (ctmJSONObject.getInteger("code") != null && 200 == ctmJSONObject.getInteger("code")) {
                        subThreadResult.getSucessReturnList().add(ctmJSONObject);
                    } else {
                        subThreadResult.getErrorReturnList().add(ctmJSONObject.getString("message"));
                    }
                }
                return subThreadResult;
            });
            if (threadResult.getErrorReturnList().size() > 0) {
                String errorString = (String) threadResult.getErrorReturnList().stream().collect(Collectors.joining("\n"));
                throw new CtmException(errorString);
            }
            if (threadResult.getSucessReturnList().size() > 0) {
                records = new CtmJSONArray(threadResult.getSucessReturnList());
            }
        } catch (Exception e) {
            log.error("回单批量预览打印失败：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100123"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100063", "回单批量预览打印失败！") /* "回单批量预览打印失败！" */ + e.getMessage(), e);
        }
        return records;
    }

    private CtmJSONObject getReceiptPreviewFile(BankElectronicReceipt bankvo, String operation) throws Exception {
        log.error("回单批量预览打印，回单编号：" + bankvo.getReceiptno());
        CtmJSONObject responsenewMsg = new CtmJSONObject();
        responsenewMsg.put("receiptno", bankvo.getReceiptno());
        responsenewMsg.put("id", bankvo.getId());
        if (null == bankvo || StringUtils.isBlank(bankvo.getExtendss())) {
            responsenewMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1713352554276454436") /* "当前单据未关联附件信息，不能预览!" */);
            responsenewMsg.put("code", 500);
            return responsenewMsg;
        }
        if (bankvo.getIsdown() == null || !bankvo.getIsdown().booleanValue()) {
            responsenewMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1887457005B0000B", "选择的数据无对应的回单文件，请选择有回单文件的数据预览/打印！") /* "选择的数据无对应的回单文件，请选择有回单文件的数据预览/打印！" */);
            responsenewMsg.put("code", 500);

            return responsenewMsg;
        }
        if (bankvo.getExtendss() != null) {
            bankvo.setExtendss(bankReceiptHandleDataService.handleExtendss(bankvo.getExtendss(), bankvo.getId()));
            if ("print".equals(operation)) {
                //银企下载的数据  拿byte返回前端
                if (bankvo.getExtendss().contains("/")) {
                    byte[] bytes = ossPoolClient.download(bankvo.getExtendss());
                    if (ArrayUtils.isEmpty(bytes)) {
                        responsenewMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1723790200993742866") /* "当前单据未查询到附件信息，不能预览!" */);
                        responsenewMsg.put("code", 500);

                        return responsenewMsg;
                    }
                    responsenewMsg.put("type", "bytes");
                    //pdf批量预览 转成图片格式返回给前端
                    responsenewMsg.put("downloadfile", bytes);
                    responsenewMsg.put("previewfile", PdfToImageUtils.pdfToImage(bytes));
                    responsenewMsg.put("receiptno", bankvo.getReceiptno());
                    responsenewMsg.put("filename", bankvo.getFilename());
                } else {
//                            List<String> extendssList = new ArrayList<String>();
//                            extendssList.add(bankvo.getExtendss());
//                            String url = cooperationFileUtilService.queryDownloadUrl(extendssList);
                    String url = cooperationFileUtilService.getFileBucketUrl(bankvo.getExtendss());
                    if (StringUtils.isBlank(url)) {
                        responsenewMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1723790200993742866") /* "当前单据未查询到附件信息，不能预览!" */);
                        responsenewMsg.put("code", 500);

                        return responsenewMsg;
                    }
                    responsenewMsg.put("type", "bytes");
                    responsenewMsg.put("fullUrl", url);
                    String suffix = "";
                    if (bankvo.getFilename() != null) {
                        suffix = bankvo.getFilename().substring(bankvo.getFilename().lastIndexOf("."));
                    } else {
                        suffix = ".pdf";
                    }
                    //pdf批量预览 转成图片格式返回给前端
                    if (".pdf".equalsIgnoreCase(suffix)) {
                        //if (EnvConstant.PDF_TO_IMAGE) {
                        List<String> extendssList = new ArrayList<String>();
                        extendssList.add(bankvo.getExtendss());
                        String innerUrl = cooperationFileUtilService.queryInnerDownloadUrl(extendssList);
                        byte[] bytes = cooperationFileUtilService.queryBytesbyFileidNew(innerUrl);
                        responsenewMsg.put("previewfile", PdfToImageUtils.pdfToImage(bytes));
                        responsenewMsg.put("previewUrl", innerUrl);
                        //} else {
                        //    responsenewMsg.put("previewUrl", url);
                        //    responsenewMsg.put("type", "pdf");
                        //}
                    } else {
                        //根据文件id获取预览的url
                        // String previewUrl = cooperationFileUtilService.queryPreviewUrlById(bankvo.getExtendss());
                        String previewUrl = cooperationFileUtilService.getFileBucketUrl(bankvo.getExtendss());
                        responsenewMsg.put("previewUrl", previewUrl);
                    }
                }

            } else {
                // String url = queryUrlByFileId(bankvo.getExtendss());
                String url = cooperationFileUtilService.getFileBucketUrl(bankvo.getExtendss());
                responsenewMsg.put("fullUrl", url);
                responsenewMsg.put("filename", bankvo.getFilename());
                responsenewMsg.put("receiptno", bankvo.getReceiptno());

            }
        }
        responsenewMsg.put("code", 200);

        return responsenewMsg;
    }

    /**
     * 银行回单关联银行交易明细 更新回单关联状态为自动关联
     *
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject relateBankReceiptDetail(CtmJSONObject paramMap) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        //异步调用，执行回单关联数据
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 1, 10, "relateBankReceiptDetail-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            try {
                //检查日期参数
                TaskUtils.dateCheck(paramMap);
                getBankReceiptRelateData(paramMap);
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, (String) paramMap.get("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("relateBankReceiptDetail exception when batch process executorServicePool", e);
            } finally {
                if (autoAssociatedDataExecutor != null) {
                    autoAssociatedDataExecutor.shutdown();
                }
            }
        });
        //通知调度任务 后端为异步
        jsonObject.put("asynchronized", true);
        return jsonObject;
    }

    /**
     * 根据银行账户查询银行交易回单并更新状态
     *
     * @param receiptList
     * @return
     * @throws Exception
     */
    private void dealBankDetailRelateData(List<BankElectronicReceipt> receiptList) {
        //根据帐户分组
        Map<String, List<BankElectronicReceipt>> groupedReceipts = receiptList.stream().collect(Collectors.groupingBy(BankElectronicReceipt::getEnterpriseBankAccount));
        List<Map<String, Object>> httpList = groupedReceipts.entrySet().stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("enterpriseBankAccount", entry.getKey());
            map.put("receipts", entry.getValue());
            return map;
        }).collect(Collectors.toList());
        //账户分组 10个一组
        List<List<Map<String, Object>>> httpListGroup = ValueUtils.splitList(httpList, 10);
        try {
            //线程启动
            List<Callable<Object>> callables = new ArrayList<>();
            for (List<Map<String, Object>> callableList : httpListGroup) {
                callables.add(() -> {
                    taskBankReceiptService.matchBankreceiptAndBankReconciliation(callableList);
                    return null;
                });
            }
            List<Future<Object>> futures = ctmThreadPoolExecutor.getThreadPoolExecutor().invokeAll(callables);
            // 显式检查每个 Future 的结果或异常
            for (Future<Object> future : futures) {
                try {
                    future.get(); // 这里可以捕获 ExecutionException
                } catch (ExecutionException e) {
                    log.error("任务执行过程中发生异常", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100124"),
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100062",
                                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100062", "根据银行账户查询银行交易回单并更新状态异常:") /* "根据银行账户查询银行交易回单并更新状态异常:" */) + e.getCause().getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("根据银行账户查询银行交易回单并更新状态异常" + e, e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100124"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100062", "根据银行账户查询银行交易回单并更新状态异常:") /* "根据银行账户查询银行交易回单并更新状态异常:" */ + e, e);
        }
    }


    /**
     * 银行回单和银行交易明细关联文件url补偿任务
     *
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject urlCompensation(CtmJSONObject paramMap) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        //异步调用，执行回单关联数据
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 1, 10, "relateBankReceiptDetail-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            try {
                executeUrlCompensation(paramMap);
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, (String) paramMap.get("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("relateBankReceiptDetail exception when batch process executorServicePool", e);
            } finally {
                if (autoAssociatedDataExecutor != null) {
                    autoAssociatedDataExecutor.shutdown();
                }
            }
        });
        //通知调度任务 后端为异步
        jsonObject.put("asynchronized", true);
        return jsonObject;
    }

    /**
     * 执行电子回单文件url补偿
     *
     * @param paramMap
     * @return
     * @throws Exception
     */
    private void executeUrlCompensation(CtmJSONObject paramMap) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(" id,bankseqno,accentity,dc_flag,tran_amt,enterpriseBankAccount ,tranDate,bankreconciliationid ,extendss ");
        QueryConditionGroup condition = new QueryConditionGroup();
        Integer relatedDays = paramMap.getInteger("RelatedDays") == null ? 3 : paramMap.getInteger("RelatedDays");
        Date beforeDate = DateUtils.dateAddDays(DateUtils.getNowDate(), relatedDays * (-1));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(DateUtils.dateFormat(beforeDate, null))));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(DateUtils.getTodayShort())));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("associationstatus").in(0, 1)));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("extendss").is_not_null()));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("isdown").eq(1)));
        schema.addCondition(condition);
        //查询已关联，且已下载文件的回单
        List<BankElectronicReceipt> receiptBankAccountList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema, null);
        if (CollectionUtils.isNotEmpty(receiptBankAccountList)) {
            for (BankElectronicReceipt bankelereceipt : receiptBankAccountList) {
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankelereceipt.getBankreconciliationid());
                LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankelereceipt.getExtendss(), 13);

                bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankelereceipt.getExtendss());
            }
        }
    }

    /**
     * 获取银行回单关联数据
     *
     * @param paramMap
     * @return
     * @throws Exception
     */
    private void getBankReceiptRelateData(CtmJSONObject paramMap) throws Exception {
        QuerySchema queryBankAccountSchema = QuerySchema.create().addSelect(" enterpriseBankAccount ");
        QuerySchema schema = QuerySchema.create().addSelect(" id,bankseqno,accentity,dc_flag,tran_amt,enterpriseBankAccount ,tranDate ,extendss ");
        schema.addSelect(BankElectronicReceipt.ISDOWN);
        //添加时间戳，利用平台锁机制；否则extendss为null时，update不更新，导致数据不一致
        schema.addSelect("pubts");
        schema.addSelect(ICmpConstant.DETAIL_RECEIPT_RELATION_CODE);
        HashMap<String, String> queriedDateProcess = TaskUtils.queryDateProcess(paramMap, null);
        String startDate = queriedDateProcess.get(TaskUtils.TASK_START_DATE);
        String endDate = queriedDateProcess.get(TaskUtils.TASK_END_DATE);
        QueryConditionGroup condition = new QueryConditionGroup();
        //加使用组织条件，需要转化为账户条件
        //会计主体
        List<String> accounts = TaskUtils.getAccountsByAccEntitys(paramMap);
        if (CollectionUtils.isNotEmpty(accounts)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.ENTERPRISE_BANK_ACCOUNT).in(accounts)));
        }
        //加银行类别条件
        String[] banktypes = TaskUtils.getBanktypes(paramMap);
        if (ArrayUtils.isNotEmpty(banktypes)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("banktype").in(banktypes)));
        }
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(startDate)));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(endDate)));
        // 回单关联状态为空是未关联
        condition.addCondition(QueryConditionGroup.and(QueryConditionGroup.or(QueryCondition.name("associationstatus").eq("4"),
                QueryCondition.name("associationstatus").is_null())));
        // 增加参数：没有下载回单文件的不进行关联
        if (receiptRelateCheckFile.equals("true")) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.EXTENDSS).is_not_null()));
        }
        queryBankAccountSchema.addCondition(condition);
        queryBankAccountSchema.distinct();
        schema.addCondition(condition);
        //查询账号
        //todo 大于200001条时，mdd报错
        List<BankElectronicReceipt> receiptBankAccountList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, queryBankAccountSchema, null);
        if (receiptBankAccountList != null && receiptBankAccountList.size() > 1000) {
            //账号大于1000 分区查数据
            List<String> enterpriseBankAccountList = receiptBankAccountList.stream().filter(Objects::nonNull).map(BankElectronicReceipt::getEnterpriseBankAccount).filter(Objects::nonNull).collect(Collectors.toList());
            int batchSize = 200;  // 每批取出的元素个数
            for (int i = 0; i < enterpriseBankAccountList.size(); i += batchSize) {
                QuerySchema querySchemaNew = schema.clone();
                List<String> accountList = enterpriseBankAccountList.subList(i, Math.min(i + batchSize, enterpriseBankAccountList.size()));
                QueryConditionGroup conditionNew = new QueryConditionGroup();
                for (ConditionExpression qCond : condition.getConditions()) {
                    conditionNew.addCondition(qCond);
                }
                conditionNew.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(accountList)));
                querySchemaNew.addCondition(conditionNew);
                List<BankElectronicReceipt> receiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchemaNew, null);
                // 根据银行流水号查询银行交易明细数据 并更新状态
                dealBankDetailRelateData(receiptList);
                //清空 释放
                receiptList.clear();
                receiptList = null;
            }
        } else {
            List<BankElectronicReceipt> receiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema, null);
            if (receiptList != null && receiptList.size() > 0) {
                // 根据银行流水号查询银行交易明细数据 并更新状态
                dealBankDetailRelateData(receiptList);
            }
        }
    }


    /**
     * 银行回单查询统计交易金额和数量
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryElectronicReceiptStatistics(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        BigDecimal debitamount = BigDecimal.ZERO;//借方金额
        BigDecimal creditamount = BigDecimal.ZERO;//贷方金额
        Long amountnum = 0L;
        Long debitanum = 0L;
        Long creditanum = 0L;
        String formatStr = "0.00";
        LinkedHashMap<String, List<Object>> dataMap = new LinkedHashMap<String, List<Object>>();
        ArrayList commonVOs = new ArrayList();
        try {
            dataMap = (LinkedHashMap<String, List<Object>>) param.get("condition");
            commonVOs = (ArrayList) dataMap.get("commonVOs");
            QuerySchema querySchema = QuerySchema.create().addSelect(" count(id) as count,count(case when dc_flag = 1 then 1 end) as count_dc_flag_1 ," +
                    " count(case when dc_flag = 2 then 1 end) as count_dc_flag_2 ," +
                    " sum(case when dc_flag = 1 then tran_amt else 0 end) as tran_amt_dc_flag_1," +
                    " sum(case when dc_flag = 2 then tran_amt else 0 end) as tran_amt_dc_flag_2 ");
            QueryConditionGroup group = new QueryConditionGroup();
            if (commonVOs != null) {
                //遍历查询条件
                for (Object commonVO : commonVOs) {
                    LinkedHashMap<String, String> listLinkedHashMap = (LinkedHashMap<String, String>) commonVO;
                    //会计主体
                    if (!"entercountry".equals(listLinkedHashMap.get("itemName"))
                            && !"cashDirectLink".equals(listLinkedHashMap.get("itemName"))) {
                        if (!"schemeName".equals(listLinkedHashMap.get("itemName")) && !"isDefault".equals(listLinkedHashMap.get("itemName"))) {
                            if ("tranDate".equals(listLinkedHashMap.get("itemName")) && listLinkedHashMap.get("value2") != null) {
                                group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).egt(listLinkedHashMap.get("value1")));
                                group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).elt(listLinkedHashMap.get("value2")));
                            } else {
                                group.addCondition(QueryCondition.name(listLinkedHashMap.get("itemName")).in(listLinkedHashMap.get("value1")));
                            }
                        }
                    }
                }
                // 权限控制
                Set<String> orgsSet = BillInfoUtils.getOrgPermissions("cmp_bankelectronicreceiptlist");
                if (orgsSet != null && orgsSet.size() > 0) {
                    String[] orgs = orgsSet.toArray(new String[orgsSet.size()]);
                    group.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(orgs));
                }
                querySchema.addCondition(group);
                Map<String, Object> map = MetaDaoHelper.queryOne(BankElectronicReceipt.ENTITY_NAME, querySchema);
                amountnum = (Long) map.get("count");
                debitanum = (Long) map.get("count_dc_flag_1");//借方笔数
                debitamount = map.get("tran_amt_dc_flag_1") != null ? (BigDecimal) map.get("tran_amt_dc_flag_1") : BigDecimal.ZERO;
                creditanum = (Long) map.get("count_dc_flag_2");//贷方笔数
                creditamount = map.get("tran_amt_dc_flag_2") != null ? (BigDecimal) map.get("tran_amt_dc_flag_2") : BigDecimal.ZERO;
            }
            jsonObject.put("debitanum", debitanum);//借方笔数
            jsonObject.put("creditanum", creditanum);//贷方笔数
            jsonObject.put("debitamount", new DecimalFormat(formatStr).format(debitamount));//借方金额
            jsonObject.put("creditamount", new DecimalFormat(formatStr).format(creditamount));//贷方金额
            jsonObject.put("amountnum", amountnum);//总计
            jsonObject.put("amountsum", new DecimalFormat(formatStr).format(debitamount.add(creditamount)));//总计金额
            jsonObject.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        } catch (Exception e) {
            log.error("银行回单查询统计交易金额和数量出错" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100125"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005E", "银行回单查询统计交易金额异常！") /* "银行回单查询统计交易金额异常！" */);
        }
        return jsonObject;
    }

    @Override
    public ThreadResult downloadReceiptFileByAccount(List<List<BankElectronicReceipt>> receiptList, ExecutorService executorService) throws Exception {
        ThreadResult threadResult = new ThreadResult();
        try {
            //启用线程处理 批量进行下载
            threadResult = ThreadPoolUtil.executeByBatchCollectResults(executorService, receiptList, 5, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400737", "调度任务下载回单文件") /* "调度任务下载回单文件" */, null, true, (int fromIndex, int toIndex) -> {
                ThreadResult subThreadResult = new ThreadResult();
                for (int t = fromIndex; t < toIndex; t++) {
                    String account = "";
                    List<BankElectronicReceipt> bankelereceiptList = receiptList.get(t);
                    EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceiptList.get(0).getEnterpriseBankAccount());
                    if (enterpriseBankAcctVO != null) {
                        account = enterpriseBankAcctVO.getAccount();
                    }
                    try {
                        String noDataMsg = downloadReceiptFileByAccountTaskExcute(bankelereceiptList);
                        if (noDataMsg != null) {
                            String noDataSuccessMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400739", "[%s]:此账户返回[%s]") /* "[%s]:此账户返回[%s]" */, account, noDataMsg);
                            subThreadResult.getSucessReturnList().add(noDataSuccessMessage);
                        }
                    } catch (Exception e) {
                        log.error("银行回单文件拉取异常：" + e.getMessage(), e);
                        List<String> receiptNoList = bankelereceiptList.stream().map(bankelereceipt -> bankelereceipt.getReceiptno()).collect(Collectors.toList());
                        String errorMessage = YQLUtils.getErrorMsqWithAccountBillno(e, account, receiptNoList.toString());
                        subThreadResult.getErrorReturnList().add(errorMessage);
                    }
                }

                return subThreadResult;
            });
        } catch (Exception e) {
            log.error("银行回单文件拉取异常" + e, e);
            threadResult.getErrorReturnList().add(e.getMessage());
        }
        return threadResult;
    }

    public String downloadReceiptFileByAccountTaskExcute(List<BankElectronicReceipt> bankelereceiptList) throws Exception {
        BankReconciliation bankReconciliation = null;
        CtmJSONObject postMsg_down = new CtmJSONObject();
        CtmJSONObject postResult_down = new CtmJSONObject();
        String fileId = "";
        try {
            //第一条数据
            Map<String, Object> bankelereceiptFirst = bankelereceiptList.get(0);
            //组装下载信息
            CtmJSONArray param_down = new CtmJSONArray();
            for (Map<String, Object> newReceipt : bankelereceiptList) {
                CtmJSONObject down = new CtmJSONObject();
                down.put(YQLConstant.BILL_NO, newReceipt.get("receiptno"));
                down.put("bill_extend", newReceipt.get("bill_extend"));
                String uniqueNoStr = MapUtil.getStr(newReceipt, YQLUtils.YQL_UNIQUE_NO);
                if (StringUtils.isNotEmpty(uniqueNoStr)) {
                    down.put(YQLUtils.UNIQUE_NO, uniqueNoStr);
                }
                param_down.add(down);
            }
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceiptFirst.get("enterpriseBankAccount").toString());
            if (enterpriseBankAcctVO == null) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540074E", "查询不到id为%s的账户") /* "查询不到id为%s的账户" */, bankelereceiptFirst.get("enterpriseBankAccount")));
            }
            postMsg_down = buildReceiptDownloadMsgBatch(param_down, enterpriseBankAcctVO, bankelereceiptFirst.get("custno").toString());
            log.error("==========================回单调度任务下载参数=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(postMsg_down.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", postMsg_down.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            postResult_down = HttpsUtils.doHttpsPostNew(ITransCodeConstant.DOWNLOAD_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("==========================回单调度任务下载结果=====================================>" + CtmJSONObject.toJSONString(postResult_down));
            if ("1".equals(postResult_down.getString("code"))) {
                ResultVo resultVo = new ResultVo();
                CtmJSONObject responseHead_down = postResult_down.getJSONObject(YQLConstant.DATA).getJSONObject("response_head");
                String serviceStatus_down = responseHead_down.getString("service_status");
                CtmJSONObject objs = postResult_down.getJSONObject(YQLConstant.DATA);
                CtmJSONObject responseBody_down = objs.getJSONObject(YQLConstant.RESPONSE_BODY);
                //serviceStatus_down不为00时，responseBody_down可能为null，导致空指针报错
                String service_resp_code = responseHead_down.getString(IStwbConstantForCmp.SERVICE_RESP_CODE);
                String yqlErrorMsq = YQLUtils.getYQLErrorMsq(responseHead_down);
                if (("00").equals(serviceStatus_down) && (ITransCodeConstant.SERVICE_RESP_CODE).equals(service_resp_code) && responseBody_down.get(YQLConstant.RECORD) != null) {
                    CtmJSONArray stringArray = new CtmJSONArray();
                    int backNum = responseBody_down.getInteger("back_num");
//                if(backNum == 1){
//                    CtmJSONObject receiptString =  responseBody_down.getJSONObject("record");
//                    stringArray.add(receiptString);
//                }else
                    stringArray = responseBody_down.getJSONArray(YQLConstant.RECORD);
                    if (!stringArray.isEmpty()) {
                        BiMap<BankElectronicReceipt, CtmJSONObject> receiptId_record_map = matchBankreceiptAndRecord(stringArray, bankelereceiptList);

                        for (int i = 0; i < stringArray.size(); i++) {
                            CtmJSONObject receiptJson = stringArray.getJSONObject(i);
                            if (receiptJson == null) {
                                continue;
                            }
                            //银企联传回的回单号信息
                            String bill_no = receiptJson.getString(YQLConstant.BILL_NO);
                            // 校验数据是否下载成功，不成功直接跳过
                            if (this.checkIsSkip(resultVo, bill_no, receiptNoLang, receiptJson)) {
                                continue;
                            }
                            if (bill_no != null) {
                                //走上传协同云器逻辑
                                if (receiptJson.get("bill_file_content") != null) {
                                    String bill_file_content = (String) receiptJson.get("bill_file_content");
                                    String bill_file = (String) receiptJson.get("bill_file");
                                    String fileformat = getFileformat(bill_file);
                                    byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                                    try {
                                        fileId = cooperationFileUtilService.uploadOfFileBytes(b, UUID.randomUUID().toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807EE", "银行电子回单") /* "银行电子回单" */ + fileformat /* "银行电子回单.pdf" */);
                                    } catch (Exception e) {//如果名称重复 重试一次
                                        fileId = cooperationFileUtilService.uploadOfFileBytes(b, UUID.randomUUID().toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807EE", "银行电子回单") /* "银行电子回单" */ + fileformat  /* "银行电子回单.pdf" */);
                                    }
                                    BankElectronicReceipt receiptVo = receiptId_record_map.inverse().get(receiptJson);

                                    //for (BankElectronicReceipt receiptVo : bankelereceiptList) {
                                    //    if (isMatch(receiptVo, stringArray, receiptJson)) {
                                    //更新回单表信息
                                    //查询银行对账单
                                    //之前需要查，否则getIsdown getBankreconciliationid getReceiptno都没值
                                    if (ObjectUtils.isNotEmpty(receiptVo.getBankreconciliationid())) {
                                        bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, (receiptVo.getBankreconciliationid()));
                                        if (null != bankReconciliation && ObjectUtils.isNotEmpty(fileId) && receiptVo.getIsdown().equals(false)) {
                                            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 14);

                                            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
                                        }
                                    }
                                    BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                                    bankElectronicReceipt.setId(receiptVo.getId());
                                    bankElectronicReceipt.setExtendss(fileId);
                                    //回单名称
                                    //bill_file 去重
                                    String bill_file_uniq = UUID.randomUUID().toString() + "-" + bill_file;
                                    bankElectronicReceipt.setFilename(bill_file_uniq);
                                    bankElectronicReceipt.setIsdown(true);

                                    if (receiptJson.get("identifying_code") != null) {// 校验码
                                        bankElectronicReceipt.setUniqueCode(receiptJson.get("identifying_code").toString());
                                    }
                                    bankElectronicReceipt.setEntityStatus(EntityStatus.Update);
                                    MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
                                    //    }
                                    //
                                    //}
                                } else {
                                    throw new CtmException(yqlErrorMsq + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073A", "银企联返回的回单文件内容为空") /* "银企联返回的回单文件内容为空" */);
                                }

                            } else {
                                throw new CtmException(yqlErrorMsq + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540073F", "银企联返回的回单编号为空") /* "银企联返回的回单编号为空" */);
                            }
                        }
                        //表头返回成功时，详情里有异常也需要抛出
                        List<String> resultVoMessages = resultVo.getMessages();
                        String joinedResultVoMessages = String.join("\n", resultVoMessages);
                        if (CollectionUtils.isNotEmpty(resultVoMessages)) {
                            throw new CtmException(YQLUtils.getYQLErrorMsq(responseHead_down, joinedResultVoMessages));
                        }
                    }
                } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                    String noDataMsg = YQLUtils.getYQLNoDataMsq(responseHead_down);
                    return noDataMsg;
                    //没有数据不报错
                } else {
                    //全部失败时，新版可能会在详情里有异常，所以尝试获取详情里的异常
                    getErrorMsg(responseBody_down, resultVo);
                    List<String> resultVoMessages = resultVo.getMessages();
                    String joinedResultVoMessages = String.join("\n", resultVoMessages);
                    //优先抛出详情里的异常
                    if (CollectionUtils.isNotEmpty(resultVoMessages)) {
                        throw new CtmException(YQLUtils.getYQLErrorMsq(responseHead_down, joinedResultVoMessages));
                    } else {
                        //调度任务时，抛出银企联异常给上层收集后展示
                        throw new CtmException(yqlErrorMsq);
                    }
                }
            } else {
                //调度任务时，抛出银企联请求异常给上层收集后展示
                throw new CtmException(YQLUtils.getYQLErrorMsqOfNetWork(postResult_down.getString("message")));
            }
        } catch (Exception e) {
            LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, fileId, 15);

            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, fileId);
            log.error(String.format("buildAndQueryTask-error，请求参数 = %s,响应参数 = %s,报错信息 = %s", postMsg_down, postResult_down, e.getMessage()), e);
            throw new Exception(e.getMessage(), e);
        }
        return null;
    }

    private void getErrorMsg(CtmJSONObject responseBody_down, ResultVo resultVo) {
        try {
            CtmJSONArray stringArray = new CtmJSONArray();
            stringArray = responseBody_down.getJSONArray(YQLConstant.RECORD);
            if (!stringArray.isEmpty()) {
                for (int i = 0; i < stringArray.size(); i++) {
                    CtmJSONObject receiptJson = stringArray.getJSONObject(i);
                    if (receiptJson == null) {
                        continue;
                    }
                    //银企联传回的回单号信息
                    String bill_no = receiptJson.getString(YQLConstant.BILL_NO);
                    // 校验数据是否下载成功，不成功直接跳过
                    if (this.checkIsSkip(resultVo, bill_no, receiptNoLang, receiptJson)) {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            log.error("batch download receipt file, get errro message from detail.Error!", e);
        }
    }

    /**
     * 批量新增回单信息
     *
     * @param bankTranBatchAddVOs 回单信息
     * @return 回单id列表
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public List<Object> batchInsertReceipt(List<BankTranBatchAddVO> bankTranBatchAddVOs) throws Exception {

        List<String> lockKeys = bankTranBatchAddVOs.stream().map(b -> Optional.ofNullable(b.getBankaccount()).orElse(b.getBankaccount_account()).concat("_").concat(ICmpConstant.BATCHINSERTBANKRECEIPTKEY).concat("_").concat(b.getTran_date())).collect(Collectors.toList());
        AtomicReference<List<BankElectronicReceipt>> bankElectronicReceipts = new AtomicReference<>();
        CtmLockTool.executeInOneServiceExclusivelyBatchLock(lockKeys, 30L, TimeUnit.SECONDS, (int lockstatus) -> {
            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                log.error("批量新增回单,加锁失败");
                return;
            }
            if (lockstatus == LockStatus.GETLOCK_SUCCESS) {
                //换取持久化数据
                attachNullField(bankTranBatchAddVOs);
                Map<String, String> bankaccountIdOrgMap = getBankaccountIdOrgMap(bankTranBatchAddVOs);
                bankElectronicReceipts.set(bankTranBatchAddVOs.stream()
                        .map(b -> {
                            BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                            bankElectronicReceipt.setDetailReceiptRelationCode(b.getDetailReceiptRelationCode());
                            bankElectronicReceipt.setBankseqno(b.getBank_seq_no());
                            bankElectronicReceipt.setBankcheckcode(b.getBankcheckcode());
                            bankElectronicReceipt.setReceiptno(b.getReceiptno());
                            bankElectronicReceipt.setAccentity(bankaccountIdOrgMap.get(b.getBankaccount()));
                            bankElectronicReceipt.setCreateDate(new Date());
                            bankElectronicReceipt.setCreateTime(new Date());
                            try {
                                bankElectronicReceipt.setTranDate(DateUtil.parseDate(b.getTran_date(), "yyyy-MM-dd"));
                                if (null != b.getTranTime())
                                    bankElectronicReceipt.setTranTime(DateUtil.parseDate(b.getTranTime(), "yyyy-MM-dd HH:mm:ss"));
                            } catch (Exception e) {
                                log.error("批量新增回单,日期时间转换失败", e);
                            }
                            String filename = b.getFilename();
                            if(filename != null && !filename.endsWith(".ofd")){
                                bankElectronicReceipt.setSignStatus(SignStatus.UnSupported);
                            }
                            bankElectronicReceipt.setId(ymsOidGenerator.nextId());
                            bankElectronicReceipt.setCurrency(b.getCurrency());
                            bankElectronicReceipt.setCreator(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400754", "默认用户") /* "默认用户" */);
                            //没有这个，后面规则判断时，数据来源会改掉
                            bankElectronicReceipt.put(ICmpConstant.FROM_API, true);
                            bankElectronicReceipt.setDataOrigin(DateOrigin.Created);
                            bankElectronicReceipt.setBanktype(b.getBankTypeId());
                            bankElectronicReceipt.setDc_flag(StringUtils.equalsIgnoreCase(b.getDc_flag(), "1") ? Direction.Debit : Direction.Credit);
                            bankElectronicReceipt.setEnterpriseBankAccount(b.getBankaccount());
                            bankElectronicReceipt.setExtendss(b.getExtendContent());
                            bankElectronicReceipt.setFilename(filename);
                            bankElectronicReceipt.setRemark(b.getRemark());
                            bankElectronicReceipt.setTenant(AppContext.getTenantId());
                            bankElectronicReceipt.setTo_acct_bank(b.getTo_acct_bank());
                            bankElectronicReceipt.setTo_acct_bank_name(b.getTo_acct_bank_name());
                            bankElectronicReceipt.setTo_acct_name(b.getTo_acct_name());
                            bankElectronicReceipt.setTo_acct_no(b.getTo_acct_no());
                            bankElectronicReceipt.setTran_amt(new BigDecimal(b.getTran_amt()));
                            bankElectronicReceipt.setUse_name(b.getUse_name());
                            bankElectronicReceipt.setRemark01(b.getPostscript());
                            bankElectronicReceipt.setBankcheckcode(b.getBankcheckcode());
                            bankElectronicReceipt.setEntityStatus(EntityStatus.Insert);

                            return bankElectronicReceipt;
                        }).filter(b -> {
                            try {
                                CtmJSONObject detailData = new CtmJSONObject();
                                detailData.put("bank_seq_no", b.getBankseqno());
                                detailData.put(YQLConstant.BILL_NO, b.getReceiptno());
                                detailData.put("tran_date", DateUtils.parseDateToStr(b.getTranDate(), DateUtils.YYYYMMDD));
                                detailData.put("tran_amt", b.getTran_amt());
                                detailData.put("to_acct_no", b.getTo_acct_no());
                                detailData.put("dc_flag", b.getDc_flag() == Direction.Debit ? "d" : "c");
                                return !checkEleReceipt(b.getEnterpriseBankAccount(), detailData);
                            } catch (Exception e) {
                                log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FD7C46E05B80000", "批量新增回单,判重发生异常") /* "批量新增回单,判重发生异常:" */, e);
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FD7C46E05B80000", "批量新增回单,判重发生异常") /* "批量新增回单,判重发生异常:" */ + e.getMessage(), e);
                            }
                        }).collect(Collectors.toList()));
                List<BankElectronicReceipt> bankElectronicReceipts1 = bankElectronicReceipts.get();
                if(bankElectronicReceipts1.isEmpty()){
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FD7C4A204700003", "重复数据无需新增") /* "重复数据无需新增" */);
                };
                BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_BANKELECTRONICRECEIPTLIST);
                billDataDto.setData(bankElectronicReceipts1);
                if (!CollectionUtils.isEmpty(bankElectronicReceipts1))
                    iBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
            }
        });
        return bankElectronicReceipts.get().stream().map(BankElectronicReceipt::getId).collect(Collectors.toList());
    }

    private Map<String, String> getBankaccountIdOrgMap(List<BankTranBatchAddVO> bankTranBatchAddVOs) throws Exception {
        List<String> bankaccountIds = bankTranBatchAddVOs.stream().map(bankTranBatchAddVO -> bankTranBatchAddVO.getBankaccount()).collect(Collectors.toList());
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(bankaccountIds);
        List<EnterpriseBankAcctVO> enterpriselist = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        Map<String, String> bankaccountIdOrgMap = enterpriselist.stream().collect(Collectors.toMap(EnterpriseBankAcctVO::getId, EnterpriseBankAcctVO::getOrgid));
        return bankaccountIdOrgMap;
    }

    /**
     * 批量银行回单文件上传
     *
     * @param bankTranBatchUpdateVOS 需更新的回单信息
     * @return 回单id
     */
    @Override
    public List<String> batchUpdateReceipt(List<BankTranBatchUpdateVO> bankTranBatchUpdateVOS) throws Exception {
        if (bankTranBatchUpdateVOS == null || bankTranBatchUpdateVOS.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> ids = bankTranBatchUpdateVOS.stream().map(vo->vo.getId()).collect(Collectors.toList());
        //List<String> hasFileReceiptnoList = new ArrayList<>();
        List<BankElectronicReceipt> hasFileReceipt = new ArrayList<>();
        List<String> notExistIDList = new ArrayList<>();

        // 合并校验和处理流程，避免多次遍历
        Map<String, BankElectronicReceipt> receiptMap = new HashMap<>();
        Map<String, BankElectronicReceipt> updateReceiptMap = new HashMap<>();
        List<BankElectronicReceipt> receiptVOList = CmpMetaDaoHelper.queryByIds(BankElectronicReceipt.ENTITY_NAME, "id,receiptno,extendss", ids);
        receiptVOList.forEach(receipt -> {
            receiptMap.put(receipt.getId().toString(), receipt);});

        for (BankTranBatchUpdateVO vo : bankTranBatchUpdateVOS) {
            String id = vo.getId();
            BankElectronicReceipt receipt = receiptMap.get(id);

            if (receipt == null) {
                notExistIDList.add(id);
                continue; // 忽略无效ID
            }

            // 校验是否存在文件id（即已有文件）；需要删除原文件，并通知下游
            if (StringUtils.isNotEmpty(receipt.getExtendss())) {
                //String receiptNo = receipt.getReceiptno();
                //if (receiptNo != null) {
                    //hasFileReceiptnoList.add(receiptNo);
                    hasFileReceipt.add(receipt);
                //}
            }

            updateReceiptMap.put(id, receipt);


        }

        // 执行上传及更新操作，独立事务，后面的报错不会导致回滚
        List<String> updateIds = updateReceipt(bankTranBatchUpdateVOS, updateReceiptMap, hasFileReceipt);

        StringBuilder errorMsg = new StringBuilder();
        StringBuilder successMsg = new StringBuilder();
        //if (!hasFileReceiptnoList.isEmpty()) {
        //    String hasFileReceiptnoErrorMsg = String.format(
        //            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400745", "回单编号：%s已存在回单文件，如需更新，请先删除原有文件！") /* "回单编号：%s已存在回单文件，如需更新，请先删除原有文件！" */,
        //            String.join(",", hasFileReceiptnoList)
        //    );
        //    errorMsg.append(hasFileReceiptnoErrorMsg);
        //}
        if (!notExistIDList.isEmpty()) {
            String notExistIDErrorMsg = String.format(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400747", "回单id：%s对应的数据不存在！") /* "回单id：%s对应的数据不存在！" */,
                    String.join(",", notExistIDList)
            );
            errorMsg.append(notExistIDErrorMsg);
        }
        if (!updateIds.isEmpty()) {
            String updateIdsMsg = String.format(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400753", "回单id：%s对应的数据更新成功！") /* "回单id：%s对应的数据更新成功！" */,
                    String.join(",", updateIds)
            );
            successMsg.append(updateIdsMsg);
        }
        String errorMsgString = errorMsg.toString();
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isNotEmpty(errorMsgString)) {
            throw new CtmException(errorMsgString + successMsg);
        }
        return updateIds;
    }

    @Override
    public void deleteBankReceipts(List<String> ids) throws Exception {
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_BANKELECTRONICRECEIPTLIST);
        List<BankElectronicReceipt> bankElectronicReceiptList = new ArrayList<>();
        for(String id : ids) {
            BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
            bankElectronicReceipt.setId(id);
            bankElectronicReceiptList.add(bankElectronicReceipt);
        }
        billDataDto.setData(bankElectronicReceiptList);
        if (!CollectionUtils.isEmpty(bankElectronicReceiptList)) {
            iBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        }
    }

    private void dealOldReceiptFile(List<BankElectronicReceipt> hasFileReceipt) throws Exception {
        for (BankElectronicReceipt bankElectronicReceipt : hasFileReceipt) {
            if (bankElectronicReceipt.getBankreconciliationid() != null) {
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankElectronicReceipt.getBankreconciliationid());
                if (bankReconciliation != null ) {
                    //删除前回单关联状态已被改为未关联，有文件id的发送取消关联的事件，不用判断回单关联状态了
                    if(ObjectUtils.isNotEmpty(bankElectronicReceipt.getExtendss())){
                        bankreconciliationService.cancelUrl(bankReconciliation.getId(),bankElectronicReceipt.getExtendss());
                    }
                }
            }
            try {
                if (bankElectronicReceipt.getIsdown() && bankElectronicReceipt.getExtendss() != null) {
                    //删除文件，逻辑删除，会定期自动清理
                    String fieldId = bankReceiptHandleDataService.handleExtendss(bankElectronicReceipt.getExtendss(), bankElectronicReceipt.getId());
                    apFileDeleteService.batchMarkDeleteFiles(ICmpConstant.APPCODE, Arrays.asList(fieldId));
                }
            } catch (Exception e) {
                log.error("回单删除文件失败：", e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2214773A05600007", "回单删除文件失败：") /* "回单删除文件失败：" */ + e.getMessage(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public List<String> updateReceipt(List<BankTranBatchUpdateVO> bankTranBatchUpdateVOS, Map<String, BankElectronicReceipt> receiptMap, List<BankElectronicReceipt> hasFileReceipt) throws Exception {
        //存在文件id（即已有文件）；需要删除原文件，并通知下游
        dealOldReceiptFile(hasFileReceipt);
        List<String> updateIds = new ArrayList<>();
        for (BankTranBatchUpdateVO vo : bankTranBatchUpdateVOS) {
            String id = vo.getId();
            BankElectronicReceipt receipt = receiptMap.get(id);
            if (receipt == null) {
                continue;
            }

            byte[] fileBytes = org.apache.commons.codec.binary.Base64.decodeBase64(vo.getBankpdf());
            String fileName = vo.getFileName();

            String fileId = cooperationFileUtilService.uploadOfFileBytes(fileBytes, fileName);

            updateBankElectronicReceipt(id, fileId, fileName, null);
            updateIds.add(id);
        }

        return updateIds;
    }


    /**
     * 银行回单参数,会计主体/币种/银行账号
     * 如果只传入了对应的code没有传对应的id值
     * 批量查询换出对应的id并回设到入参对象,已备持久化
     *
     * @param bankTranBatchAddVOs 银行回单信息
     * @throws Exception
     */
    private void attachNullField(List<BankTranBatchAddVO> bankTranBatchAddVOs) throws Exception {
        //List<String> accentityCodeNotNull =
        //        bankTranBatchAddVOs.stream().filter(item -> ObjectUtils.isEmpty(item.getAccentity())
        //                        && ObjectUtils.isNotEmpty(item.getAccentity_code())).map(BankTranBatchAddVO::getAccentity_code)
        //                .distinct().collect(Collectors.toList());
        //List<Map<String, Object>> enterpriseInfos;
        ////会计主体
        //if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(accentityCodeNotNull)) {
        //    enterpriseInfos = QueryBaseDocUtils.queryAccEntityByCodes(accentityCodeNotNull);
        //    Asserts.check(org.apache.commons.collections4.CollectionUtils.isNotEmpty(enterpriseInfos), "回单信息中存在未匹配的会计主体编码");
        //    List<Map<String, Object>> finalEnterpriseInfos = enterpriseInfos;
        //    bankTranBatchAddVOs.stream().forEach(b -> {
        //        Map<String, Object> map = finalEnterpriseInfos.stream().filter(item -> item.get("code").equals(b.getAccentity_code())).findFirst().get();
        //        if (MapUtil.isNotEmpty(map)) {
        //            b.setAccentity(map.get("id").toString());
        //        }
        //    });
        //}

        //币种
        List<String> currencyCodeNotNull =
                bankTranBatchAddVOs.stream().filter(item -> ObjectUtils.isEmpty(item.getCurrency())
                                && ObjectUtils.isNotEmpty(item.getCurrency_code())).map(BankTranBatchAddVO::getCurrency_code)
                        .distinct().collect(Collectors.toList());
        List<Map<String, Object>> currencyMaps;
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(currencyCodeNotNull)) {
            currencyMaps = QueryBaseDocUtils.queryCurrencyByCodes(currencyCodeNotNull);
            if(org.apache.commons.collections4.CollectionUtils.isEmpty(currencyMaps)){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400738", "根据币种code未查询到币种id信息") /* "根据币种code未查询到币种id信息" */);
            }
            List<Map<String, Object>> finalCurrencyInfos = currencyMaps;
            bankTranBatchAddVOs.stream().forEach(b -> {
                Map<String, Object> map = finalCurrencyInfos.stream().filter(item -> StringUtils.equalsIgnoreCase(String.valueOf(item.get("code")), b.getCurrency_code())).findFirst().get();
                if (MapUtil.isNotEmpty(map)) {
                    b.setCurrency(map.get("id").toString());
                }
            });
        }

        //银行账户code
        List<String> bankAccountCodeNotNull =
                bankTranBatchAddVOs.stream().filter(item -> ObjectUtils.isEmpty(item.getBankaccount())
                                && ObjectUtils.isNotEmpty(item.getBankaccount_account())).map(BankTranBatchAddVO::getBankaccount_account)
                        .distinct().collect(Collectors.toList());
        //银行账户Id
        List<String> bankAccountIdNotNull =
                bankTranBatchAddVOs.stream().filter(item -> ObjectUtils.isNotEmpty(item.getBankaccount())).map(BankTranBatchAddVO::getBankaccount)
                        .distinct().collect(Collectors.toList());

        List<Map<String, Object>> bankAccountMaps;
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(bankAccountCodeNotNull)) {
            bankAccountMaps = QueryBaseDocUtils.queryBankAccountByAccountsOrIds(bankAccountCodeNotNull, Boolean.TRUE);
            if(org.apache.commons.collections4.CollectionUtils.isEmpty(bankAccountMaps)){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400742", "根据银行code未查询到账号id信息") /* "根据银行code未查询到账号id信息" */);
            }
            List<Map<String, Object>> finalBankAccountInfos = bankAccountMaps;
            bankTranBatchAddVOs.stream().forEach(b -> {
                Map<String, Object> map = finalBankAccountInfos.stream().filter(item -> item.get("account").equals(b.getBankaccount_account())).findFirst().get();
                if (MapUtil.isNotEmpty(map)) {
                    b.setBankaccount(map.get("id").toString());
                    b.setBankTypeId(String.valueOf(map.get("bank")));
                }
            });
        }

        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(bankAccountIdNotNull)) {
            List<Map<String, Object>> bankMaps = QueryBaseDocUtils.queryEnterpriseBankAccountByIdList(bankAccountIdNotNull);
            if(CollectionUtils.isEmpty(bankMaps)){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400744", "根据银行Id未查询到账号信息") /* "根据银行Id未查询到账号信息" */);
            }
            bankTranBatchAddVOs.stream().forEach(b -> {
                Map<String, Object> map = bankMaps.stream().filter(item -> item.get("id").equals(b.getBankaccount())).findFirst().get();
                if (MapUtil.isNotEmpty(map)) {
                    b.setBankaccount_account(String.valueOf(map.get("account")));
                    b.setBankTypeId(String.valueOf(map.get("bank")));
                }
            });
        }
    }


    /**
     * 获取客户号
     *
     * @param bankAccountId
     * @return
     * @throws Exception
     */
    public Boolean getCustomNoByBankAccountId(String bankAccountId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(bankAccountId), QueryCondition.name("openFlag").eq("1"),
                QueryCondition.name("customNo").is_not_null());
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (ValueUtils.isEmpty(setting)) {
            return false;
        }
        return true;
    }

    public void updateBatchBankElectronicReceiptTransactional(List<BankElectronicReceipt> bankElectronicReceipts) throws Exception {
        if (CollectionUtils.isEmpty(bankElectronicReceipts)) {
            return;
        }
        for (BankElectronicReceipt bankElectronicReceipt : bankElectronicReceipts) {
            //查出最新的银行交易回单 因为可能多个任务同步进行 回单信息回改变
            BankElectronicReceipt finalBankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt.getId());
            finalBankElectronicReceipt.setExtendss(bankElectronicReceipt.getExtendss());
            finalBankElectronicReceipt.setIsdown(bankElectronicReceipt.getIsdown());
            finalBankElectronicReceipt.setFilename(bankElectronicReceipt.getFilename());
            finalBankElectronicReceipt.setSignStatus(bankElectronicReceipt.getSignStatus());
            finalBankElectronicReceipt.setUniqueCode(bankElectronicReceipt.getUniqueCode());
            //根据银行交易回单查询银行对账单
            if (finalBankElectronicReceipt.getBankreconciliationid() != null) {
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, finalBankElectronicReceipt.getBankreconciliationid());
                if (bankReconciliation != null && ObjectUtils.isNotEmpty(finalBankElectronicReceipt.getExtendss()) && finalBankElectronicReceipt.getIsdown().equals(true)) {
                    LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankElectronicReceipt.getExtendss(), 16);

                    bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankElectronicReceipt.getExtendss());
                }
            }
            finalBankElectronicReceipt.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, finalBankElectronicReceipt);
        }
    }

    /**
     * 高效率执行批量更新
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED,rollbackFor = RuntimeException.class)
    public void updateBatchBankElectronicReceipt(List<BankElectronicReceipt> bankElectronicReceipts) {
        if (CollectionUtils.isEmpty(bankElectronicReceipts)) {
            return;
        }
        SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
        Connection connection = null;
        String updateSql = "update cmp_bankelectronicreceipt set extendss = ?, isdown = ?, filename = ?, signStatus = ?, uniqueCode = ? where id=?";
        PreparedStatement preparedStatement = null;
        try {
            DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
            connection = DataSourceUtils.getConnection(dataSource);
            preparedStatement = connection.prepareStatement(updateSql);
            connection.setAutoCommit(false);
            int batchSize = 400;
            int count = 0;
            for (BankElectronicReceipt bankElectronicReceipt : bankElectronicReceipts) {
                preparedStatement.setString(1, bankElectronicReceipt.getExtendss());
                preparedStatement.setBoolean(2, true);
                if (bankElectronicReceipt.getFilename() != null) {
                    preparedStatement.setString(3, bankElectronicReceipt.getFilename());
                } else {
                    preparedStatement.setNull(3, Types.VARCHAR);
                }
                if (bankElectronicReceipt.getSignStatus() != null) {
                    preparedStatement.setShort(4, bankElectronicReceipt.getSignStatus().getValue());
                } else {
                    preparedStatement.setNull(4, SignStatus.UnSign.getValue());
                }
                if (bankElectronicReceipt.getUniqueCode() != null) {
                    preparedStatement.setString(5, bankElectronicReceipt.getUniqueCode());
                } else {
                    preparedStatement.setNull(5, Types.VARCHAR);
                }
                preparedStatement.setLong(6, bankElectronicReceipt.getId());
                preparedStatement.addBatch();
                count++;
                if (count % batchSize == 0) {
                    preparedStatement.executeBatch();
                    preparedStatement.clearBatch();
                }
                //查出最新的银行交易回单
                BankElectronicReceipt finalBankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt.getId());
                //根据银行交易回单查询银行对账单
                if (finalBankElectronicReceipt.getBankreconciliationid() != null) {
                    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, finalBankElectronicReceipt.getBankreconciliationid());
                    if (bankReconciliation != null && ObjectUtils.isNotEmpty(bankElectronicReceipt.getExtendss()) && finalBankElectronicReceipt.getIsdown().equals(false)) {
                        LogUtil.saveBankelereceiptSendFileEventlogByDto(bankReconciliation, bankElectronicReceipt.getExtendss(), 17);

                        bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankElectronicReceipt.getExtendss());
                    }
                }
            }
            preparedStatement.executeBatch();
            connection.commit();
        } catch (Exception e) {
            log.error("银行回单批量更新数据异常" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100127"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A31480510005F", "银行回单批量更新数据异常:") /* "银行回单批量更新数据异常:" */ + e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (null != connection) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭PreparedStatement异常" + e);
            }
        }
    }


    /**
     * 根据文件id获取文件url
     *
     * @param fileId
     * @return
     * @throws Exception
     */
    public String queryUrlByFileId(String fileId) throws Exception {
        String url = null;
        if (StringUtils.isNotEmpty(fileId)) {
            if (fileId.contains("/")) {
                return ossPoolClient.getFullUrl(fileId);
            } else {
                List<String> fileIds = new ArrayList<>();
                fileIds.add(fileId);
                return cooperationFileUtilService.queryDownloadUrl(fileIds);
            }
        }
        return url;
    }

    /**
     * * 获取内部账户交易回单
     *
     * @param params
     * @param accountIdList
     * @param uid
     * @return
     * @throws Exception
     */
    @Override
    public int stctInsertReceiptByinnerAccounts(CtmJSONObject params, List<String> accountIdList, String uid) throws Exception {
        if (accountIdList != null && accountIdList.size() > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            StctAccountReconciliationReqVo stctAccountReconciliationReqVo = new StctAccountReconciliationReqVo();
            stctAccountReconciliationReqVo.setAccountId(accountIdList);
            if (params.get("startDate") != null && params.get("endDate") != null) {
                if (params.get(DateUtils.START_DATE_DATE_TYPE) != null) {
                    stctAccountReconciliationReqVo.setBeginDate((Date) params.get(DateUtils.START_DATE_DATE_TYPE));
                } else {
                    stctAccountReconciliationReqVo.setBeginDate(dateFormat.parse(params.get("startDate").toString()));
                }
                if (params.get(DateUtils.END_DATE_DATE_TYPE) != null) {
                    stctAccountReconciliationReqVo.setEndDate(DateUtils.getLastTimeForThisDate(((Date) params.get(DateUtils.END_DATE_DATE_TYPE))));
                } else {
                    stctAccountReconciliationReqVo.setEndDate(DateUtils.getLastTimeForThisDate(dateFormat.parse(params.get("endDate").toString())));
                }
            } else {
                HashMap<String, String> querydate = TaskUtils.queryDateProcess(params, "yyyy-MM-dd");
                if (querydate.isEmpty()) {
                    stctAccountReconciliationReqVo.setBeginDate(DateUtils.getBeforeDate());
                    stctAccountReconciliationReqVo.setEndDate(DateUtils.getLastTimeForThisDate(DateUtils.getBeforeDate()));
                } else {
                    String begDate = querydate.get(TaskUtils.TASK_START_DATE);
                    String endDate = querydate.get(TaskUtils.TASK_END_DATE);
                    stctAccountReconciliationReqVo.setBeginDate(dateFormat.parse(begDate));
                    stctAccountReconciliationReqVo.setEndDate(DateUtils.getLastTimeForThisDate(dateFormat.parse(endDate)));
                }
            }
            log.error("======内部账户回单拉取请求参数=======>" + stctAccountReconciliationReqVo.toString());
            List<StctAccountReconciliationRespVo> stctReconciliationVos = new ArrayList<>();
            stctReconciliationVos = RemoteDubbo.get(IStctAccountReconciliationApiService.class, IDomainConstant.MDD_DOMAIN_STCT).findAccountReconciliation(stctAccountReconciliationReqVo);
            log.error("======内部账户回单拉取请求结果=======>" + stctReconciliationVos.toString());
            ProcessUtil.totalPullCountAdd(uid, stctReconciliationVos.size());
            if (stctReconciliationVos != null && stctReconciliationVos.size() > 0) {
                //先查询账户所属组织
                Set<String> acctidSet = new HashSet<>();
                for (StctAccountReconciliationRespVo stctReconciliationVo : stctReconciliationVos) {
                    acctidSet.add(stctReconciliationVo.getAccount().toString());
                }
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setIdList(new ArrayList<>(acctidSet));
                //List<EnterpriseBankAcctVO> acctVoForOrg = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                List<EnterpriseBankAcctVO> acctVoForOrg = enterpriseBankQueryService.queryAll(enterpriseParams);
                Map<String, String> acctOrgMap = new HashMap<>();
                for (EnterpriseBankAcctVO vo : acctVoForOrg) {
                    acctOrgMap.put(vo.getId(), vo.getOrgid());
                }
                List<BankElectronicReceipt> bankElectronicReceipts = new ArrayList<>();
                for (StctAccountReconciliationRespVo stctReconciliationVo : stctReconciliationVos) {
                    //验重
                    boolean repetition = checkEleReceipt(stctReconciliationVo.getAccount(), stctReconciliationVo.getTransNumber(), stctReconciliationVo.getCode(), stctReconciliationVo.getBillDate());
                    if (repetition) {
                        continue;
                    }
                    BankElectronicReceipt detail = new BankElectronicReceipt();
                    detail.setTenant(AppContext.getTenantId());
                    detail.setAccentity(acctOrgMap.get(stctReconciliationVo.getAccount().toString()));
                    detail.setEnterpriseBankAccount(stctReconciliationVo.getAccount().toString());
                    Map<String, Object> bankAccountObject = QueryBaseDocUtils.queryEnterpriseBankAccountById(stctReconciliationVo.getAccount());
                    if (bankAccountObject != null) {
                        detail.setBanktype(bankAccountObject.get("bank").toString());
                    }
                    detail.setTranDate(stctReconciliationVo.getBillDate());
                    detail.setTranTime(stctReconciliationVo.getBillTime());
                    detail.setBankseqno(stctReconciliationVo.getTransNumber());
                    detail.setTo_acct_no(stctReconciliationVo.getCounterPartyNo());
                    detail.setTo_acct_name(stctReconciliationVo.getCounterPartyName());
                    detail.setTo_acct_bank(stctReconciliationVo.getCounterPartyOpenBankNo());
                    //detail.setTo_acct_bank_name("toAcctBankName");
                    detail.setCurrency(stctReconciliationVo.getCurrency());
                    detail.setTran_amt(stctReconciliationVo.getBillAmt());
                    // 添加创建时间、创建日期
                    detail.setCreateDate(new Date());
                    detail.setCreateTime(new Date());
                    String dcFlag = stctReconciliationVo.getDirection();
                    if ("disburse".equalsIgnoreCase(dcFlag)) {
                        detail.setDc_flag(Direction.Debit);
                    } else if ("income".equalsIgnoreCase(dcFlag)) {
                        detail.setDc_flag(Direction.Credit);
                    }
                    detail.setReceiptno(stctReconciliationVo.getCode());
                    detail.setRemark(stctReconciliationVo.getDigest());
                    detail.setFilename(stctReconciliationVo.getFileId() + ".pdf");
                    detail.setDataOrigin(DateOrigin.DownFromYQL);
                    detail.setIsdown(true);
                    detail.setExtendss(stctReconciliationVo.getFileId());
                    detail.setEntityStatus(EntityStatus.Insert);
                    detail.setId(ymsOidGenerator.nextId());
                    detail.setSignStatus(SignStatus.UnSupported);
                    bankElectronicReceipts.add(detail);
                }
                if (!bankElectronicReceipts.isEmpty()) {
                    CmpMetaDaoHelper.insert(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipts);
                    ProcessUtil.newAddCountAdd(uid, bankElectronicReceipts.size());
                }
                if (stctReconciliationVos.size() != accountIdList.size()) {
                    log.error("======内部账户回单拉取部分无结果=======>");
                    Set<String> returnAccountIdSet = stctReconciliationVos.stream()
                            .map(StctAccountReconciliationRespVo::getAccount)
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .collect(Collectors.toSet());
                    List<String> notReturnAccountIdList = accountIdList.stream()
                            .filter(accountId -> !returnAccountIdSet.contains(accountId))
                            .collect(Collectors.toList());
                    ProcessUtil.addInnerNodataMessage(uid, notReturnAccountIdList, stctAccountReconciliationReqVo.getBeginDate(), stctAccountReconciliationReqVo.getEndDate(),
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400746", "请检查[对账单查询节点]是否有数据且回单生成状态为已生成"));
                }
            } else {
                log.error("======内部账户回单拉取无结果=======>");
                ProcessUtil.addInnerNodataMessage(uid, accountIdList, stctAccountReconciliationReqVo.getBeginDate(), stctAccountReconciliationReqVo.getEndDate(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400746", "请检查[对账单查询节点]是否有数据且回单生成状态为已生成") /* "请检查[对账单查询节点]是否有数据且回单生成状态为已生成" */);
            }
            return stctReconciliationVos.size();
        }
        return 0;
    }


}
