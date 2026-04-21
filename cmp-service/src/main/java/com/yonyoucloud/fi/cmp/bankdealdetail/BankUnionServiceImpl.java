package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequest;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequestBodyRecord;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponse;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponseRecord;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.BankDealDetailAccessFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import lombok.NonNull;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * desc:牧原交易明细接收service
 * author:wangqiangac
 * date:2023/12/5 16:47
 */
@Slf4j
@Service
public class BankUnionServiceImpl implements BankUnionService{
    private Integer BathCount = 1000;//批量提交数量
    private static LinkedBlockingQueue<BankUnionRequest> linkedBlockingQueue = new LinkedBlockingQueue<>();
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CtmCmpCheckRepeatDataService checkRepeatDataService;
    @Autowired
    private BankDealDetailServiceImpl bankDealDetailService;
    @Autowired
    ICmpSendEventService cmpSendEventService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Resource
    private BankDealDetailAccessFacade bankDealDetailAccessFacade;


    private String key = "bankUnions";
    /**
     * 银行账号对应的主键和会计主体缓存
     */
    private static final @NonNull Cache<String, String> accountCache = Caffeine.newBuilder()
            .initialCapacity(100)//初始数量
            .maximumSize(10000)//最大条数
            .expireAfterWrite(Duration.ofMinutes(30))//最后一次写操作后经过指定时间过期
            .softValues()
            .build();
    /**
     * 缓存币种主键和编码
     */
    private static final @NonNull Cache<String, String> currencyCodeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .softValues()
            .build();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 交易明细接收后发事件中心及入库
     * @param bankUnionRequests
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public BankUnionResponse insertTransactionDetail4BankUnion(List<BankUnionRequestBodyRecord> bankUnionRequests) throws Exception {
        //走智能流水ods接入
        if(DealDetailUtils.isOpenIntelligentDealDetail()){
            return bankDealDetailAccessFacade.dealDetailAccessByNotice(bankUnionRequests);
        }
        BankUnionResponse response = new BankUnionResponse();
        List<BankUnionResponseRecord> responseRecords = new ArrayList<>();
        /**
         * 1，根据入参组装数据
         * 2，unique_no验重
         * 3，多要素判空
         * 4，对方类型辨识
         * 5，通过后入库
         */
        // 1,根据入参组装数据
        int successCount = 0;
        int failCount = 0;
        List<BankReconciliation> newBankRecords = new ArrayList<>();
        for(BankUnionRequestBodyRecord bankUnionRequest : bankUnionRequests){
            List<BankDealDetail> bankDealDetails = new ArrayList<>();
            List<BankReconciliation> bankRecords = new ArrayList<>();
            BankUnionResponseRecord responseRecord = new BankUnionResponseRecord();
            try {
                this.dealWithBankUnion(bankUnionRequest,bankDealDetails,bankRecords);
            } catch (Exception e) {
                responseRecord.setUnique_no(bankUnionRequest.getUnique_no());
                responseRecord.setCode("010012");
                responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063F", "检查数据准确性：") /* "检查数据准确性：" */+e.getMessage());
                responseRecords.add(responseRecord);
                failCount++;
                continue;
            }

            // 2,验重
            bankDealDetails = checkRepeatDataService.checkRepeatData(bankDealDetails, EventType.BankDealDetail.getValue());
            if (bankDealDetails.size() == 0) {
                responseRecord.setUnique_no(bankUnionRequest.getUnique_no());
                responseRecord.setCode("010100");
                responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063B", "当前交易明细为重复数据") /* "当前交易明细为重复数据" */);
                responseRecords.add(responseRecord);
                failCount++;
                continue;
            }
            bankRecords = checkRepeatDataService.checkRepeatData(bankRecords, EventType.CashMark.getValue());
            if (bankRecords.size() == 0) {
                responseRecord.setUnique_no(bankUnionRequest.getUnique_no());
                responseRecord.setCode("010100");
                responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063B", "当前交易明细为重复数据") /* "当前交易明细为重复数据" */);
                responseRecords.add(responseRecord);
                failCount++;
                continue;
            }
            // 对方类型辨识
            // 4,智能辨识 银行对账单对方类型
            String batchcount = AppContext.getEnvConfig("cmp.analysisDataBatchProcess.batchcount",null);
            if(batchcount==null){
                for (int i = 0; i < bankRecords.size(); i++) {
                    bankDealDetailService.smartClassifyBankreconciliation(bankRecords.get(i));
                }
            }else{
                bankDealDetailService.smartClassifyBatchProcess(bankRecords);
            }
            // 5，入库
            CmpMetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, bankDealDetails);
            log.error("===========入库BankReconciliation======"+ bankRecords.get(0).getCurrency());
            CommonSaveUtils.saveBankReconciliation(bankRecords);
            // 成功
            responseRecord.setUnique_no(bankUnionRequest.getUnique_no());
            responseRecord.setCode("000000");
            responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063D", "入库成功！") /* "入库成功！" */);
            responseRecords.add(responseRecord);
            successCount++;

            newBankRecords.addAll(bankRecords);
        }

        // 6，发送事件
        // 银行流水支持发送事件消息
        cmpSendEventService.sendEventByBankClaimBatch(newBankRecords, EntityStatus.Insert.name());

        if(successCount == bankUnionRequests.size()){
            log.error("====全部成功========");
            response.setCode("1");
            response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063C", "全部成功") /* "全部成功" */);
            response.setData(responseRecords);
        } else if(failCount == bankUnionRequests.size()){
            log.error("====全部失败========");
            response.setCode("2");
            response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063E", "全部失败") /* "全部失败" */);
            response.setData(responseRecords);
        } else {
            log.error("====部分成功========");
            response.setCode("3");
            response.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400640", "部分成功") /* "部分成功" */);
            response.setData(responseRecords);
        }

        return response;
    }
    /**
     * 事物结束后afterCommit开启线程异步执行插入银行对账单
     * @param bankUnionRequest
     * @param bankDealDetail
     */
    private void afterCommitDealBankrecon(BankUnionRequest bankUnionRequest,BankDealDetail bankDealDetail) {
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try {
                    RobotExecutors.runAs(AppContext.getYTenantId(), new Callable() {
                        @Override
                        public Object call() throws Exception {
                            this.insertBankReconciliation(bankUnionRequest,bankDealDetail);
                            return null;
                        }
                        /**
                         * 根据银企联推送的交易明细及bip入库后的交易明细对象组装对账单并插入数据库
                         * @param bankUnionRequest
                         * @param bankDealDetail
                         */
                        private void insertBankReconciliation(BankUnionRequest bankUnionRequest, BankDealDetail bankDealDetail) throws Exception {
                            BankReconciliation bankRecord = new BankReconciliation();
                            bankRecord.setId(bankDealDetail.getId());
                            bankRecord.setInitflag(false);
                            bankRecord.setLibraryflag(false);
                            bankRecord.setDataOrigin(DateOrigin.DownFromYQL);
                            bankRecord.setAccentity(bankDealDetail.getAccentity());
                            bankRecord.setBankaccount(bankDealDetail.getEnterpriseBankAccount());
                            bankRecord.setBanktype(bankDealDetail.getBanktype());
                            bankRecord.setTran_date(bankDealDetail.getTranDate());
                            bankRecord.setDzdate(bankDealDetail.getTranDate());
                            bankRecord.setTran_time(bankDealDetail.getTranTime());
                            bankRecord.setBank_seq_no(bankDealDetail.getBankseqno());
                            bankRecord.setThirdserialno(bankDealDetail.getBankseqno());
                            bankRecord.setTo_acct_no(bankDealDetail.getTo_acct_no());
                            bankRecord.setTo_acct_name(bankDealDetail.getTo_acct_name());
                            bankRecord.setCurrency(bankDealDetail.getCurrency());
                            bankRecord.setTran_amt(bankDealDetail.getTran_amt());
                            bankRecord.setDc_flag(bankDealDetail.getDc_flag());
                            bankRecord.setRemark(bankDealDetail.getRemark());
                            bankRecord.setOper(bankDealDetail.getOper());
                            bankRecord.setUse_name(bankDealDetail.getUse_name());
                            bankRecord.setBankcheckno(bankDealDetail.getBankdetailno());
                            bankRecord.setEntityStatus(EntityStatus.Insert);
                            List<BankReconciliation> bankReconciliations = new ArrayList<>();
                            bankReconciliations.add(bankRecord);
                            CommonSaveUtils.saveBankReconciliation(bankReconciliations);
                        }
                    },ctmThreadPoolExecutor.getThreadPoolExecutor());
                } catch (Exception e) {
                    log.error("BankUnionServiceImpl事物结束后异步插入银行对账单报错："  + e.getMessage());
                }
            }
        });
    }

    /**
     * 处理推过来的交易明细转化为bip交易明细并入库
     * 同时在事物结束后开启线程异步插入银行对账单
     * @param bankUnionRequest
     * @throws Exception
     */
    public  void dealWithBankUnion(BankUnionRequestBodyRecord bankUnionRequest,List<BankDealDetail> bankDealDetails,List<BankReconciliation> bankRecords) throws Exception {
        BankDealDetail bankDealDetail = new BankDealDetail();
        BankReconciliation bankReconciliation = new BankReconciliation();
        // 银行账户
        String acct_no = bankUnionRequest.getAcct_no();
        String acctIdAndOrgID = this.getBankAcctByAccount(acct_no);
        String[] acctIdAndOrgIDs = acctIdAndOrgID.split(",");
        bankDealDetail.setEnterpriseBankAccount(acctIdAndOrgIDs[0]);
        bankReconciliation.setBankaccount(acctIdAndOrgIDs[0]);
        // 所属组织
        bankReconciliation.setOrgid(acctIdAndOrgIDs[1]);
        // id
        Long id = ymsOidGenerator.nextId();
        bankDealDetail.setId(id);
        bankReconciliation.setId(id);
        // 会计主体
        bankDealDetail.setAccentity(acctIdAndOrgIDs[1]);
        bankReconciliation.setAccentity(acctIdAndOrgIDs[1]);
        if(!Objects.isNull(bankReconciliation.getAccentity())){
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
        }
        // 银行类别
        bankDealDetail.setBanktype(acctIdAndOrgIDs[2]);
        bankReconciliation.setBanktype(acctIdAndOrgIDs[2]);
        // 币种
        String currCode = bankUnionRequest.getCurr_code();
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        if (StringUtils.isNotEmpty(currCode)) {
            currencyBdParams.setCode(currCode);
        }
        List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
        if (currencylist == null || currencylist.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101334"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080024", "币种编码不存在！") /* "币种编码不存在！" */);
        }
        String currency =currencylist.get(0).getId();
        bankDealDetail.setCurrency(currency);
        bankReconciliation.setCurrency(currency);
        log.error("=========币种查询===unique_no:=="+bankUnionRequest.getUnique_no()+"==currCode:"+currCode+"==currency:"+currency);
        // 交易流水号
        bankDealDetail.setBankseqno(bankUnionRequest.getBank_seq_no());
        bankReconciliation.setBank_seq_no(bankUnionRequest.getBank_seq_no());
        // 交易日期
        String tran_date = bankUnionRequest.getTran_date();
        if(StringUtils.isNotEmpty(tran_date)){
            Date tranDate = DateUtils.dateParse(tran_date, DateUtils.YYYYMMDD);
            bankDealDetail.setTranDate(tranDate);
            bankReconciliation.setTran_date(tranDate);
            bankReconciliation.setDzdate(tranDate);
        }
        // 交易时间
        String timeStr = bankUnionRequest.getTran_time();
        if (StringUtils.isNotEmpty(tran_date) && StringUtils.isNotEmpty(timeStr)) {
            Date tranTime = DateUtils.dateParse(tran_date + timeStr, DateUtils.YYYYMMDDHHMMSS);
            bankDealDetail.setTranTime(tranTime);
            bankReconciliation.setTran_time(tranTime);
        }
        // 收付方向
        String dc_flag = bankUnionRequest.getDc_flag();
        if ("d".equalsIgnoreCase(dc_flag)) {
            bankDealDetail.setDc_flag(Direction.Debit);
            bankReconciliation.setDc_flag(Direction.Debit);
            //支出赋值借方金额
            bankReconciliation.setDebitamount(bankUnionRequest.getTran_amt());
        } else if ("c".equalsIgnoreCase(dc_flag)) {
            bankDealDetail.setDc_flag(Direction.Credit);
            bankReconciliation.setDc_flag(Direction.Credit);
            //收入赋值贷方金额
            bankReconciliation.setCreditamount(bankUnionRequest.getTran_amt());
        }
        // 交易金额
        bankDealDetail.setTran_amt(bankUnionRequest.getTran_amt());
        bankReconciliation.setTran_amt(bankUnionRequest.getTran_amt());
        // 余额
        bankDealDetail.setAcctbal(bankUnionRequest.getAcct_bal());
        bankReconciliation.setAcct_bal(bankUnionRequest.getAcct_bal());
        // 对方账号
        bankDealDetail.setTo_acct_no(bankUnionRequest.getTo_acct_no());
        bankReconciliation.setTo_acct_no(bankUnionRequest.getTo_acct_no());
        // 对方户名
        bankDealDetail.setTo_acct_name(bankUnionRequest.getTo_acct_name());
        bankReconciliation.setTo_acct_name(bankUnionRequest.getTo_acct_name());
        // 对方账户开户行
        bankDealDetail.setTo_acct_bank(bankUnionRequest.getTo_acct_bank());
        bankReconciliation.setTo_acct_bank(bankUnionRequest.getTo_acct_bank());
        // 对方开户行名
        bankDealDetail.setTo_acct_bank_name(bankUnionRequest.getTo_acct_bank_name());
        bankReconciliation.setTo_acct_bank_name(bankUnionRequest.getTo_acct_bank_name());
        // 钞汇标志
        bankDealDetail.setCashflag(bankUnionRequest.getCash_flag());
        bankReconciliation.setCash_flag(bankUnionRequest.getCash_flag());
        // 操作员
        bankDealDetail.setOper(bankUnionRequest.getOper());
        bankReconciliation.setOper(bankUnionRequest.getOper());
        // 起息日
        String value_date = bankUnionRequest.getValue_date();
        if(StringUtils.isNotEmpty(value_date)){
            Date valueDate = DateUtils.dateParse(value_date, DateUtils.YYYYMMDD);
            bankDealDetail.setValue_date(valueDate);
            bankReconciliation.setValue_date(valueDate);
        }
        // 用途
        bankDealDetail.setUse_name(bankUnionRequest.getUse_name());
        bankReconciliation.setUse_name(bankUnionRequest.getUse_name());
        // 摘要
        bankDealDetail.setRemark(bankUnionRequest.getRemark());
        bankReconciliation.setRemark(bankUnionRequest.getRemark());
        // 附言
        bankDealDetail.setRemark01(bankUnionRequest.getRemark01());
        bankReconciliation.setRemark01(bankUnionRequest.getRemark01());
        // 银行对账编号
        bankDealDetail.setBankcheckno(bankUnionRequest.getBank_check_code());
        bankReconciliation.setBankcheckno(bankUnionRequest.getBank_check_code());
        // 唯一标识码
        bankDealDetail.setUnique_no(bankUnionRequest.getUnique_no());
        bankReconciliation.setUnique_no(bankUnionRequest.getUnique_no());
        // 数据来源
        bankReconciliation.setDataOrigin(DateOrigin.DownFromYQL);
        bankDealDetails.add(bankDealDetail);
        bankRecords.add(bankReconciliation);
    }

    /**
     * 根据币种编码查询对应的主键
     * 如果缓存中存在的话取缓存 没有的话查询
     * @param currCodeList
     * @return HashMap<String, String> key为币种编码 value为币种id
     * @throws Exception
     */
    public HashMap<String, String> queryCurrencyIdByCodes(List<String> currCodeList) throws Exception {
        HashMap<String, String> currencyMap = new HashMap<>();
        List<String> currCodes = new ArrayList<>();
        for (String currCode : currCodeList) {
            String currency = currencyCodeCache.getIfPresent(currCode);
            if(StringUtils.isNotEmpty(currency)){
                currCodes.add(currCode);
                currencyMap.put(currCode,currency);
            }
        }
        if(currCodes.size() > 0){
            return currencyMap;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC); /* 暂不修改 须在BaseRefRpcService中添加通过参数查询币种的方式*/
        //CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").in(currCodeList));
//        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        Map<String, String> map = query.stream().collect(Collectors.toMap(e -> (String)e.get("id"), e -> (String)e.get("code")));
        for (Map.Entry<String,String> entry : map.entrySet()) {
            currencyCodeCache.put(entry.getValue(),entry.getKey());
        }
        currencyMap.putAll(map);
        return currencyMap;
    }

    /**
     * 根据银行账号，获取银行账户id和会计主体信息
     * @param acct_no
     * @return
     * @throws Exception
     */
    private String getBankAcctByAccount(String acct_no) throws Exception {
        String cacheData = accountCache.getIfPresent(acct_no);
        if (StringUtils.isNotEmpty(cacheData)) {
            return cacheData;
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setAccount(acct_no);
        List<EnterpriseBankAcctVO> enterpriseBankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (enterpriseBankAccounts.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101335"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001263400", "未查询到对应的企业银行账户，保存失败！") /* "未查询到对应的企业银行账户，保存失败！" */);
        }
        String bankAcct = enterpriseBankAccounts.get(0).getId();
        String orgId = enterpriseBankAccounts.get(0).getOrgid();
        String bank = enterpriseBankAccounts.get(0).getBank();
        accountCache.put(acct_no, bankAcct + "," + orgId + "," + bank);
        return bankAcct + "," + orgId + "," + bank;
    }
    /**
     *
     * @param bankUnionRequests
     */
    public void insertBatch(List<BankUnionRequest> bankUnionRequests){
//        if (!CollectionUtils.isEmpty(bankUnionRequests)) {
//            SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
//            Connection connection = null;
//            PreparedStatement preparedStatement = null;
//            try {
//                DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
//                connection = DataSourceUtils.getConnection(dataSource);
//                connection.setAutoCommit(false);
//                String insertSql = "insert into cmp_bankdealdetail (id,accentity,enterprisebankaccount,currency," +
//                        "bankseqno,trandate,trantime," +
//                        "dc_flag,tran_amt,acctbal," +
//                        "to_acct_no,to_acct_name,to_acct_bank," +
//                        "to_acct_bank_name,cashflag,oper," +
//                        "value_date,use_name,remark," +
//                        "remark01,bankcheckno,unique_no" +
//                        "tenant_id,ytenant_id)" +
//                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//                preparedStatement = connection.prepareStatement(insertSql);
//                int size = bankUnionRequests.size();
//                int task = 0;
//                for (int i = 0; i < size; i++) {
//                    BankUnionRequest bankUnionRequest = bankUnionRequests.get(i);
//                    String accentity = accountRealtimeBalance.getAccentity();
//                    String enterpriseBankAccount = accountRealtimeBalance.getEnterpriseBankAccount();
//                    String bankType = accountRealtimeBalance.getBanktype();
//                    String currency = accountRealtimeBalance.getCurrency();
//                    Short datasource = accountRealtimeBalance.getDatasource();
//                    String flag = accountRealtimeBalance.getFlag();
//                    Date balancedate = accountRealtimeBalance.getBalancedate();
//                    BigDecimal acctbal = accountRealtimeBalance.getAcctbal();
//                    BigDecimal frzbal = accountRealtimeBalance.getFrzbal();
//                    BigDecimal avlbal = accountRealtimeBalance.getAvlbal();
//                    Long id = accountRealtimeBalance.getId();
//
//                    preparedStatement.setString(1, accentity);
//                    preparedStatement.setString(2, enterpriseBankAccount);
//                    preparedStatement.setString(3, bankType);
//                    preparedStatement.setString(4, currency);
//                    preparedStatement.setShort(5, datasource);
//                    preparedStatement.setString(6, flag);
//                    preparedStatement.setDate(7,new java.sql.Date(balancedate.getTime()));
//                    preparedStatement.setBigDecimal(8,acctbal);
//                    preparedStatement.setBigDecimal(9,frzbal);
//                    preparedStatement.setBigDecimal(10,avlbal);
//                    preparedStatement.setLong(11, id);
//                    preparedStatement.setLong(12, AppContext.getTenantId());
//                    preparedStatement.setString(13, AppContext.getYTenantId());
//
//                    //添加批量sql
//                    preparedStatement.addBatch();
//                    //每400条执行一次，防止内存堆栈溢出
//                    if (i > 0 && i % 200 == 0) {
//                        log.error("第" + task + "批执行插入任务");
//                        preparedStatement.executeBatch();
//                        preparedStatement.clearBatch();
//                        task++;
//                    }
//                }
//                //最后执行剩余不足200条的
//                preparedStatement.executeBatch();
//                //执行完手动提交事务
//                connection.commit();
//                //在把自动提交事务打开
//                connection.setAutoCommit(true);
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//            } finally {
//                try{
//                    if (null != preparedStatement) {
//                        preparedStatement.close();
//                    }
//                    if (null != connection) {
//                        connection.close();
//                    }
//                }catch (SQLException e){
//                    log.error(e.getMessage(),e);
//                }
//            }
//        }
    }


}
