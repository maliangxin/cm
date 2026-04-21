package com.yonyoucloud.fi.cmp.openapi.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.idoc.sdk.files.client.UploadClient;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.rpc.IEventSendService;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.http.YmsHttpMethod;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankdealdetail.TaskBankDealDetailService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankelectronicreceiptfilelogger.BankElectronicReceiptFileLogger;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.bankreceipt.service.TaskBankReceiptService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.ElectronicStatementConfirm;
import com.yonyoucloud.fi.cmp.openapi.service.OpenApiExternalService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.Constant.ThreadConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import com.yonyoucloud.fi.epub.accountbook.AccountBook;
import cn.hutool.core.thread.BlockPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.yonyou.ucf.mdd.ext.poi.constant.POIConstant.Export.EXPORT_EXCEL_OSS_DIR_CONFIG_KEY;

/**
 * @Author: guanqg
 * @Date: 2020/8/25 16:53
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenApiExternalServiceImpl implements OpenApiExternalService {

    private static final int MAX_LOOP_COUNT = 1000;//最大循环次数
    public static final String DOC_PK_LIST = "docPkList";
    @Autowired
    private SalaryPayService salaryService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    private static final Cache<String, CurrencyTenantDTO> currencyTenantDTOCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();
    private static final Cache<String, FinOrgDTO> finOrgDTOCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();
    private static final String STARTDATE = "startDate";
    private static final String ENDDATE = "endDate";
    private static final String RANGE = "range";
    private static final String ACCOUNTYEAR = "accountYear";
    private static final String ACCOUNTMONTH = "accountMonth";
    private static final String ACCOUNTDATE = "accountDate";
    ;
    private static final String ORGINFO = "orgInfo";
    private static final String REMOTEPATH = "remotepath";
    private final YmsOidGenerator ymsOidGenerator;
    private final BaseRefRpcService baseRefRpcService;
//    private final OrgRpcService orgRpcService;
    private final CooperationFileUtilService cooperationFileUtilService;
    private final TaskBankDealDetailService taskBankDealDetailService;
    private final TaskBankReceiptService taskBankReceiptService;
    private final BankAccountSettingService bankAccountSettingService;
    @Autowired
    private BankReceiptService bankReceiptService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BankDealDetailService bankDealDetailService;
    @Resource
    private IEventSendService eventSendService;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    String errorListKey = "errorList";
    String sucessListKey = "sucessList";



    /**
     * 交易流水调用下载银行回单pdf
     *
     * @param bankseq
     * @param response
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBankelectronicreceiptPDFByBillBankSeq(String bankseq, String bankid, HttpServletResponse response){
        String sql = "select cb.id from cmp_bankelectronicreceipt cb where cb.tenant_id = #{0} and cb.bankseqno= #{1} ";
        log.error("==================交易流水调用下载银行回单pdf回单sql=========================:" + sql);
        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("0", AppContext.getTenantId());
        parameters.put("1", bankseq);
        CtmJSONObject result = buildQueryReceiptUrlResult(sql, response, parameters);
        return result;
    }

    /**
     * 根据银行对账单ID获取银行交易回单
     *
     * @param bankBillId
     * @param response
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBankelectronicreceiptPDFByBillBillId(String bankBillId, HttpServletResponse response){
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject recordList = new CtmJSONObject();
        String url = null;
        try {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankBillId);
            if (bankReconciliation == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102215"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */);
            }
            String bankid = bankReconciliation.getId().toString();
            List<BankElectronicReceipt> bankElectronicReceiptList = CmpMetaDaoHelper.queryColByOneEqualCondition(BankElectronicReceipt.ENTITY_NAME, BankElectronicReceipt.BANKRECONCILIATIONID, bankid, BankElectronicReceipt.EXTENDSS, "id");
            if (bankElectronicReceiptList != null && bankElectronicReceiptList.size() > 0) {
                BankElectronicReceipt bankElectronicReceipt = bankElectronicReceiptList.get(0);
                String id = bankElectronicReceipt.getId().toString();
                String fileId = bankElectronicReceipt.getExtendss();
                if (fileId != null && !"".equals(fileId)) {
                    url = cooperationFileUtilService.getFileBucketUrl(fileId);
                } else {
                    CtmJSONObject params = new CtmJSONObject();
                    params.put("ids", id);
                    HttpServletResponse httpServletResponse = null;
                    CtmJSONObject result_down = bankReceiptService.receiptDownForSpecial(params, response);
                    Object urlListObject =  result_down.get("receiptUrl");
                    if (urlListObject != null) {
                        List<String> urlList = (List<String>) urlListObject;
                        if (urlList != null && urlList.size() > 0) {
                            url = urlList.get(0);
                        }
                    } else {
                        Object fileIdObject = result_down.get(ICmpConstant.FILE_ID);
                        if (fileIdObject != null) {
                            url = cooperationFileUtilService.getFileBucketUrl(fileIdObject.toString());
                        }
                    }
                }
            } else {
                //旧逻辑不动，用于兼容
                String bankseq = bankReconciliation.getBank_seq_no();
                BigDecimal tran_amt = bankReconciliation.getTran_amt();
                short dc_flag = bankReconciliation.getDc_flag().getValue();
                String bankaccount = bankReconciliation.getBankaccount();
                //String sql ="select cb.id from cmp_bankelectronicreceipt cb where cb.tenant_id = "+AppContext.getTenantId()+" and cb.bankseqno='"+ bankseq+"' and cb.enterprisebankaccount ='"+bankid+"'";
                //只查询未关联的回单
                String sql = "select cb.id from cmp_bankelectronicreceipt cb where cb.tenant_id = #{0} and cb.bankseqno= #{1} and tran_amt = #{2} and dc_flag = #{3}  and enterpriseBankAccount = #{4}  and associationstatus = 4";
                log.error("==================交易流水调用下载银行回单pdf回单sql=========================:" + sql);
                Map<String, Object> parameters = new java.util.HashMap<>();
                parameters.put("0", AppContext.getTenantId());
                parameters.put("1", bankseq);
                parameters.put("2", tran_amt);
                parameters.put("3", dc_flag);
                parameters.put("4", bankaccount);
                url = executeSqlAndDownReceipt(sql, response, parameters);
            }
            if (url != null) {
                result.put("code", 200);
                result.put("message", "success");
                recordList.put("receiptUrl", url);
                result.put("data", recordList);
            } else {
                result.put("code", 999);
                result.put("message", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */);
            }
        } catch (Exception e) {
            result.put("code", 999);
            log.error(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */ + e.getMessage(), e);
            result.put("message", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */ + e.getMessage());
            return result;
        }
        return result;
    }

    /**
     * 请求流水号调用下载银行回单pdf
     *
     * @param bankseq
     * @param bankid
     * @param response
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBankelectronicreceiptPDFByReqSeq(String bankseq, String bankid, HttpServletResponse response) {
        String sql = " select cb.id from cmp_bankelectronicreceipt cb  left join cmp_bankdealdetail cbd on  cb.bankseqno = cbd.bankseqno where cb.tenant_id = #{0} and cbd.bankdetailno= #{1} and cb.enterprisebankaccount = #{2}";
        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("0", AppContext.getTenantId());
        parameters.put("1", bankseq);
        parameters.put("2", bankid);
        CtmJSONObject result = buildQueryReceiptUrlResult(sql, response, parameters);
        return result;
    }

    /**
     * 构建回单url查询结果
     * @param sql
     * @param response
     * @param parameters
     * @return
     */
    public CtmJSONObject buildQueryReceiptUrlResult(String sql, HttpServletResponse response, Map<String, Object> parameters){
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject recordList = new CtmJSONObject();
        String url = null;
        try {
            url = executeSqlAndDownReceipt(sql, response, parameters);
            if (url != null) {
                result.put("code", 200);
                result.put("message", "success");
                recordList.put("receiptUrl", url);
                result.put("data", recordList);
            } else {
                result.put("code", 999);
                result.put("message", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */);
            }
        } catch (Exception e) {
            result.put("code", 999);
            result.put("message", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */);
        }
        return result;
    }

    /**
     * 执行sql下载回单
     *
     * @param sql
     * @param response
     * @throws Exception
     */
    public String executeSqlAndDownReceipt(String sql, HttpServletResponse response, Map<String, Object> parameters) throws Exception {
        CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());
        List<Map<String, Object>> list = metaDaoSupport.executeSelectSql(sql, parameters);
        if (list != null && list.size() > 0) {
            Map<String, Object> bankelectronicreceipt = list.get(0);
            CtmJSONObject param = new CtmJSONObject();
            param.put("ids", bankelectronicreceipt.get("id") + "");
            String url = bankReceiptService.downloadAccountReceiptDetailUrl(param, response);
            return url;
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102215"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400532", "回单未返回！") /* "回单未返回！" */) /* "回单未返回！" */);
        }
    }

    /**
     * 判断是否用友支付单据
     *
     * @param bankseq
     * @param bankid
     * @return
     */
    @Override
    public boolean isYonyouPay(String bankseq, String bankid) throws Exception {
        String sql = " select  ap.id  from cmp_paybill ap  left join cmp_bankdealdetail cb on cb.bankdetailno = ap.requestseqno  where  cb.bankseqno = #{0} and  ap.tenant_id = #{1} ";
//        log.error("==================是否用友支付单据sql=========================:"+sql);
        CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());
        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("0", bankseq);
        parameters.put("1", AppContext.getTenantId());
        List<Map<String, Object>> list = metaDaoSupport.executeSelectSql(sql, parameters);
        return list != null && list.size() > 0;
    }

    /**
     * 获取交易明细
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public List queryBankTradeDetailElectron(CtmJSONObject param) throws Exception {
        String startDate = param.getString("start_time");
        String endDate = param.getString("end_time");
        QuerySchema schema = QuerySchema.create().addSelect("id,accentity.code,accentity.name,tranDate,tran_amt,to_acct_name,to_acct_bank_name,remark,enterpriseBankAccount.account,to_acct_no,bankseqno,use_name,dc_flag");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq("2"));
        conditionGroup.appendCondition(QueryCondition.name("pubts").egt(startDate));
        conditionGroup.appendCondition(QueryCondition.name("pubts").elt(endDate));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> banklist = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME, schema);
        List result = new ArrayList();
        for (int i = 0; i < banklist.size(); i++) {
            Map<String, Object> map = banklist.get(i);
            Map resMap = new HashMap();
            String url = ReceiptDetail_ADDR + "?bankseq=" + map.get("bankseqno") + "&bankid=" + map.get("enterpriseBankAccount");
            resMap.put("to_acct_bank_name", map.get("to_acct_bank_name"));
            if (map.get("tranDate") != null) {
                Date tranDate = (Date) map.get("tranDate");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String tranDateString = sdf.format(tranDate);
                resMap.put("tran_date", tranDateString);
            }

            resMap.put("accentity_name", map.get("to_acct_name"));
            resMap.put("remark", map.get("use_name") == null ? map.get("remark") : map.get("use_name"));//2021/10/26应四方要求 传递字段由 摘要 改为 用途
            //摘要 Abstract  用途 Usage   原来这个API字段名称改下吧：就叫 摘要用途（专项）
            resMap.put("abstract", map.get("remark"));
            resMap.put("usage", map.get("use_name"));

            resMap.put("tran_amt", map.get("tran_amt"));
            resMap.put("accentity_code", map.get("accentity_code"));
            resMap.put("to_acct_no", map.get("to_acct_no"));
            resMap.put("bankseqno", (String) map.get("bankseqno"));
            resMap.put("dc_flag", map.get("dc_flag"));
            /**
             *  针对传递四方sosp系统时 bankseqno为null导致对方主键重复的问题修改(原设计对方采用 拼接字段billcode为主键)
             *  若有bankseqno字段有值 则按原逻辑处理；若为空，则用ys系统id作为billcode 避免主键重复问题
             */
            String billcode = (String) map.get("bankseqno") == null ? map.get("id").toString() : map.get("enterpriseBankAccount") + (String) map.get("bankseqno");
            resMap.put("billcode", billcode);
            resMap.put("url", url);
            result.add(resMap);
        }
        return result;
    }


    /**
     * 定时任务 查询交易明细；当日和历史都调用这个
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> queryBankTradeDetailElectronList(CtmJSONObject param, Boolean isHistory) throws Exception {
        String logId = param.getString("logId");
        // 接受请求线程池
        ExecutorService executorService =
        ThreadPoolBuilder.buildThreadPoolByYmsParam(ThreadConstant.CMP_BANKDEAL_PULL_TASK_ACCEPT);
        executorService.submit(() -> {
            ThreadResult threadResult = new ThreadResult();
            StringBuilder accountCountMsg = new StringBuilder();
            try {
                Map<String ,List<EnterpriseBankAcctVO>> bankAccountsGroup=taskBankDealDetailService.bankTradeDetailQueryAccounts(param);
                List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
                List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
                List<String> checkSuccessIdList = checkSuccess.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
                BankAccountUtil.refreshEnableDate(checkSuccessIdList);
                threadResult = taskBankDealDetailService.bankTradeDetailAsyncProcess(param, isHistory, logId,bankAccountsGroup);
                List<EnterpriseBankAcctVO> checkSuccessNew = bankAccountsGroup.get("checkSuccess");
                    if(innerAccounts!= null && !innerAccounts.isEmpty()){
                        accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400534", "拉取内部账户数：") /* "拉取内部账户数：" */).append(innerAccounts.size()).append(";");
                    }
                if(checkSuccessNew!= null && !checkSuccessNew.isEmpty()){
                    accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400535", "拉取直联账户数：") /* "拉取直联账户数：" */).append(checkSuccessNew.size()).append(";");
                    }
                    //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, logId,
                    //        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272", "执行成功") /* "执行成功" */ + ":" + accountCountMsg.toString() , TaskUtils.UPDATE_TASK_LOG_URL);
                //});
            } catch (Exception e) {
                log.error("查询账户交易明细失败：",e);
                threadResult.getErrorReturnList().add(e.getMessage());
                //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                //throw new CtmException(new com.yonyou.yonbip.ctm.error.CtmErrorCode("033-502-102216"),e.getMessage());
            }
            TaskUtils.updateTaskLogbyThreadResult((Map<String,String>)param.get("ipaParams"),logId, accountCountMsg.toString() ,threadResult);
        });
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }

    /**
     * 定时任务查询银企联 电子回单(多线程备份用 有问题 暂时不用)
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> ReceiptDetailListThread(CtmJSONObject param) throws Exception {
        List<Map<String, List>> list = queryBankAccountSetting(param);
        String logId = param.getString("logId");
        //CtmJSONObject paramNew = (CtmJSONObject) GsonHelper.FromJSon(GsonHelper.ToJSon(param), CtmJSONObject.class);
        try {
            ExecutorService executorService =
                    ThreadPoolBuilder.buildThreadPoolByYmsParam(ThreadConstant.CMP_BANKRECEIPT_PULL_TASK_ACCEPT);
            executorService.submit(() -> {
                Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = taskBankReceiptService.bankReceipt(param);
                List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
                checkSuccess = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(param, checkSuccess);
                //保证返回的list的size正确
                bankAccountsGroup.put("checkSuccess", checkSuccess);
                ThreadResult threadResult = new ThreadResult();
                StringBuilder accountCountMsg = new StringBuilder();
                try {
                    List<String> checkSuccessIdList = checkSuccess.stream().map(enterpriseBankAcctVO -> enterpriseBankAcctVO.getId().toString()).collect(Collectors.toList());
                    BankAccountUtil.refreshEnableDate(checkSuccessIdList);
                    threadResult = taskBankReceiptService.bankReceiptAsyncProcess(param, bankAccountsGroup);
                    //内部账户
                    List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
                    if (innerAccounts != null && !innerAccounts.isEmpty()) {
                        accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400534", "拉取内部账户数：") /* "拉取内部账户数：" */).append(innerAccounts.size()).append(";");
                    }
                    if (checkSuccess != null && !checkSuccess.isEmpty()) {
                        accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400535", "拉取直联账户数：") /* "拉取直联账户数：" */).append(checkSuccess.size()).append(";");
                    }
                } catch (Exception e) {
                    threadResult.getErrorReturnList().add(e.getMessage());
                    //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
                TaskUtils.updateTaskLogbyThreadResult((Map<String,String>)param.get("ipaParams"),logId, accountCountMsg.toString() ,threadResult);
                return threadResult;
            });
            ////收集线程执行情况并返回
            //List<List> resultList = new ArrayList<>();
            //List errorInfoList = new ArrayList<>();
            //List successsInfoList = new ArrayList<>();
            //List<Throwable> exceptions = new ArrayList<>();
            //try {
            //    successsInfoList.add(ThreadFuture.get());
            //} catch (ExecutionException e) {
            //    exceptions.add(e.getCause());
            //    errorInfoList.add(e.getCause().getMessage());
            //} catch (InterruptedException e) {
            //    Thread.currentThread().interrupt();
            //    exceptions.add(e);
            //    errorInfoList.add(e.getCause().getMessage());
            //}
            //resultList.add(successsInfoList);
            //resultList.add(errorInfoList);
            //if (!exceptions.isEmpty()) {
            //    log.error("Some tasks failed with exceptions: " + exceptions);
            //}
            ////回写调度任务执行情况
            // TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, logId, resultList.toString(), TaskUtils.UPDATE_TASK_LOG_URL);
        } catch (Exception e) {
             TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            //通知任务执行失败
            Map<String, Object> retMap = new HashMap<>();
            retMap.put("status", TaskUtils.TASK_BACK_FAILURE);
            retMap.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D00021", "获取电子回单任务错误：") /* "获取电子回单任务错误：" */ + e.getMessage());
            return retMap;
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }


    /**
     * 定时任务 批量查询支付状态
     *
     * @param param
     * @throws Exception
     */
    @Override
    public List<String> payStatusList(CtmJSONObject param) throws Exception {
        //CtmJSONObject param
        ArrayList<String> retMsg = new ArrayList<String>();
        String sql = "select ap.requestseqno requestseqno,cb.customNo customNo,ap.transeqno transeqno,ap.isBatchPay isBatchPay from cmp_paybill ap left join cmp_bankaccountsetting cb on  cb.ytenant_id=ap.ytenant_id  where ap.ytenant_id = #{0} and cb.ytenant_id = #{1} and ap.enterprisebankaccount = cb.enterprisebankaccount and ap.paystatus=5 and cb.openflag=1 ";
//        log.error("==================定时拉取支付状态sql========================="+sql);
        CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("0", AppContext.getYTenantId());
        parameters.put("1", AppContext.getYTenantId());
        List<Map<String, Object>> list = metaDaoSupport.executeSelectSql(sql, parameters);
        CtmJSONArray jsonArray = new CtmJSONArray();
        for (int i = 0; i < list.size(); i++) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            Map map = list.get(i);
            jsonObject.put("requestseqno", map.get("requestseqno"));
            jsonObject.put("customNo", map.get("customNo"));
            jsonObject.put("transeqno", map.get("transeqno"));
            jsonObject.put("isBatchPay", map.get("isBatchPay"));
            //加入参数判断是否为定时任务 影响后面拼接请求头
            jsonObject.put("isTiming", true);
            jsonArray.add(jsonObject);
        }
        log.error("==================定时支付状态json=========================" + jsonArray.toString());
        if (list.size() > 0) {
            paymentService.queryBatchDetailPayStatusBySifang(jsonArray);
        }
        // 薪资支付工作台 支付明细状态查询
        try {

            //1.支付状态为支付中或者部分成功的需要去执行调度任务
            QuerySchema querySettingSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            // 自动查询支付状态为“支付中、部分成功”的薪资单的支付状态
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("paystatus").in(PayStatus.Paying.getValue(), PayStatus.PartSuccess.getValue())));
            querySettingSchema.addCondition(conditionGroup);
            List<Map<String, Object>> salarypayList = MetaDaoHelper.query(Salarypay.ENTITY_NAME,querySettingSchema);
            if(salarypayList.size() == 0 || salarypayList.isEmpty()){
                return retMsg;
            } else {
                // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
                String threadParam = AppContext.getEnvConfig("cmp.salarypay.thread.param","8,32,1000,cmp-salarypay-query-paystatus-async-");
                String[] threadParamArray = threadParam.split(",");
                int corePoolSize = Integer.parseInt(threadParamArray[0]);
                int maxPoolSize = Integer.parseInt(threadParamArray[1]);
                int queueSize = Integer.parseInt(threadParamArray[2]);
                String threadNamePrefix = threadParamArray[3];
                ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                        .setDaemon(false)
                        .setRejectHandler(new BlockPolicy())
                        .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
                try {
                    // 获取 payBankAccount 属性的列表
                    List<String> payBankAccounts = salarypayList.stream()
                            .map(map -> (String) map.get("payBankAccount")) // 提取 payBankAccount 键的值
                            .collect(Collectors.toList());
                    HashMap<String,String> customNos = bankAccountSettingService.batchGetCustomNoByBankAccountId(payBankAccounts);
                    for (Map<String, Object> salarypayMap : salarypayList) {
                        String payBankAccount = salarypayMap.get("payBankAccount").toString();
                        //获取客户号
                        String customNo = customNos.get(payBankAccount);
                        if(StringUtils.isEmpty(customNo)){
                            retMsg.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400538", "薪资支付单[%s]获取客户号失败！") /* "薪资支付单[%s]获取客户号失败！" */,salarypayMap.get("code")));
                            continue;
                        }
                        // 加线程池 独立事务
                        ThreadPoolUtil.executeBySingleNotShutDown(executorService, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053A", "薪资支付支付状态查询") /* "薪资支付支付状态查询" */, (int fromIndex, int toIndex) -> {
                            salaryService.queryBatchDetailPayStatusByauto(salarypayMap, customNo);
                            return null;
                        },null);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw e;
                } finally {
                    if (executorService != null) {
                        executorService.shutdown();
                    }
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102217"),e.getMessage());
        }
        return retMsg;
    }


    /**
     * 查询单据状态
     * 若外部传入ids不为空，那么只查询ids集合数据，ids为空时，查询srcbillids集合
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public List billStatus(CtmJSONObject params) throws Exception {
        if (params.get("ids") != null) {
            CtmJSONArray ids = params.getJSONArray("ids");
            if (!ids.isEmpty()) {
                List<Object> collect = ids.stream().collect(Collectors.toList());
                return queryPayBillStatusByIds("id", collect);
            }
        }
        if (params.get("srcbillids") != null) {
            CtmJSONArray srcbillids = params.getJSONArray("srcbillids");
            if (!srcbillids.isEmpty()) {
                List<Object> collect = srcbillids.stream().collect(Collectors.toList());
                return queryPayBillStatusByIds("srcbillid", collect);
            }
        }
        return null;
    }

    /**
     * 根据外部传入的参数查询付款工作台单据的支付状态返回
     *
     * @param filterField 查询匹配参数 id（用友单据id），srcbillid（外部来源单据id）
     * @param ids         入参
     * @return
     * @throws Exception
     */
    private List queryPayBillStatusByIds(String filterField, List ids) throws Exception {
        List resultList = new ArrayList();
        QuerySchema schema = QuerySchema.create().addSelect("id,paystatus,bankseqno,transeqno,enterprisebankaccount,srcbillid,paydate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(filterField).in(ids));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(PayBill.ENTITY_NAME, schema);
        String url = null;

        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                CtmJSONObject result = new CtmJSONObject();
                Map<String, Object> map = list.get(i);
                if (map.get("transeqno") != null && !" ".equals(map.get("transeqno"))) {
                    url = ReceiptDetail_ADDR_REQSEQ + "?bankseq=" + map.get("transeqno") + "&bankid=" + map.get("enterprisebankaccount");
                }
                result.put("id", map.get("id") + "");
                result.put("srcbillid", map.get("srcbillid") + "");
                result.put("paystatus", map.get("paystatus") + "");
                if (map.get("paydate") != null) {
                    String payTime = DateUtils.dateFormat((Timestamp) map.get("paydate"), "yyyy-MM-dd HH:mm:ss");
                    result.put("paytime", payTime);
                }
                result.put("url", url);
                resultList.add(result);
            }
        }
        return resultList;
    }

    /**
     * 查询单据状态-转账单
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public List billTransferStatus(CtmJSONObject params) throws Exception {
        List<CtmJSONObject> resultList = new ArrayList<>();
        String[] strs = null;
        if (ValueUtils.isNotEmptyObj(params.get("ids"))) {
            CtmJSONArray ids = params.getJSONArray("ids");
            strs = new String[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                strs[i] = ids.get(i) + "";
            }
        }
        String[] code = null;
        if (ValueUtils.isNotEmptyObj(params.get("codes"))) {
            CtmJSONArray codes = params.getJSONArray("codes");
            code = new String[codes.size()];
            for (int i = 0; i < codes.size(); i++) {
                code[i] = codes.get(i) + "";
            }
        }
        if (!ValueUtils.isNotEmptyObj(params.get("ids")) && !ValueUtils.isNotEmptyObj(params.get("codes"))) {
            return resultList;
        }
        QuerySchema schema = QuerySchema.create().addSelect("id, auditstatus, auditTime, paystatus, settledate, settlestatus");

        QueryConditionGroup queryConditionGroup1 = new QueryConditionGroup(ConditionOperator.or);
        if (ValueUtils.isNotEmptyObj(code)) {
            QueryCondition queryCondition3 = new QueryCondition("code", ConditionOperator.in, code);
            queryConditionGroup1.addCondition(queryCondition3);
        }
        if (ValueUtils.isNotEmptyObj(strs)) {
            QueryCondition queryCondition2 = new QueryCondition("id", ConditionOperator.in, strs);
            queryConditionGroup1.addCondition(queryCondition2);
        }
        schema.addCondition(queryConditionGroup1);
        List<Map<String, Object>> list = MetaDaoHelper.query(TransferAccount.ENTITY_NAME, schema);
        if (list != null && list.size() > 0) {
            for (Map<String, Object> stringObjectMap : list) {
                CtmJSONObject result = new CtmJSONObject();
                result.put("id", stringObjectMap.get("id") + "");
                result.put("auditstatus", stringObjectMap.get("auditstatus") + "");
                result.put("paystatus", stringObjectMap.get("paystatus") + "");
                result.put("settlestatus", stringObjectMap.get("settlestatus")  + "");
                if (stringObjectMap.get("auditTime") != null) {
                    String auditTime = DateUtils.dateFormat((Timestamp) stringObjectMap.get("auditTime"), "yyyy-MM-dd HH:mm:ss");
                    result.put("auditTime", auditTime);
                }
                if (stringObjectMap.get("settledate") != null) {
                    String settledate = DateUtils.dateFormat((Timestamp) stringObjectMap.get("settledate"), "yyyy-MM-dd HH:mm:ss");
                    result.put("paydate", settledate);
                }
                resultList.add(result);
            }
        }

        return resultList;
    }

    /**
     * 查询单据状态-收款单
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public List billReceiveStatus(CtmJSONObject params) throws Exception {
        List<CtmJSONObject> resultList = new ArrayList<>();
        String[] strs = null;
        if (ValueUtils.isNotEmptyObj(params.get("ids"))) {
            CtmJSONArray ids = params.getJSONArray("ids");
            strs = new String[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                strs[i] = ids.get(i) + "";
            }
        }
        String[] code = null;
        if (ValueUtils.isNotEmptyObj(params.get("codes"))) {
            CtmJSONArray codes = params.getJSONArray("codes");
            code = new String[codes.size()];
            for (int i = 0; i < codes.size(); i++) {
                code[i] = codes.get(i) + "";
            }
        }
        if (!ValueUtils.isNotEmptyObj(params.get("ids")) && !ValueUtils.isNotEmptyObj(params.get("codes"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102218"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0019B", "单据id或单据编码不能为空") /* "单据id或单据编码不能为空" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("id, auditstatus, auditTime, settlestatus, settledate");
        QueryConditionGroup queryConditionGroup1 = new QueryConditionGroup(ConditionOperator.or);
        if (ValueUtils.isNotEmptyObj(code)) {
            QueryCondition queryCondition3 = new QueryCondition("code", ConditionOperator.in, code);
            queryConditionGroup1.addCondition(queryCondition3);
        }
        if (ValueUtils.isNotEmptyObj(strs)) {
            QueryCondition queryCondition2 = new QueryCondition("id", ConditionOperator.in, strs);
            queryConditionGroup1.addCondition(queryCondition2);
        }
        schema.addCondition(queryConditionGroup1);
        List<Map<String, Object>> list = MetaDaoHelper.query(ReceiveBill.ENTITY_NAME, schema);
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                CtmJSONObject result = new CtmJSONObject();
                Map<String, Object> map = list.get(i);
                result.put("id", map.get("id") + "");
                result.put("auditstatus", map.get("auditstatus") + "");
                result.put("settlestatus", map.get("settlestatus") + "");
                if (map.get("auditTime") != null) {
                    String auditTime = DateUtils.dateFormat((Timestamp) map.get("auditTime"), "yyyy-MM-dd HH:mm:ss");
                    result.put("auditTime", auditTime);
                }
                if (map.get("settledate") != null) {
                    String settledate = DateUtils.dateFormat((Timestamp) map.get("settledate"), "yyyy-MM-dd HH:mm:ss");
                    result.put("settledate", settledate);
                }
                resultList.add(result);
            }
        }
        return resultList;
    }

    /**
     * 查询银企联
     *
     * @return
     * @throws Exception
     */
    private List queryBankAccountSetting(CtmJSONObject param) throws Exception {
        String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
        String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
        String bankaccounts = (String) (Optional.ofNullable(param.get("bankaccount")).orElse(""));
        QuerySchema schema = QuerySchema.create().addSelect(" id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.acctName as name,"
                + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank,customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.currencyList.currency as currency");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        String[] accentityArr = null;
        if (!StringUtils.isEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        if (accentityArr != null && accentityArr.length > 0) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityArr));
        }
        String[] banktypeArr;
        if (!StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
            if (banktypeArr != null && banktypeArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.bankNumber.bank").in(banktypeArr));
            }
        }
        String[] currencyArr = null;
        if (!StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
            if (currencyArr != null && currencyArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.currencyList.currency").in(currencyArr));
            }
        }
        String[] bankaccountArr = null;
        if (!StringUtils.isEmpty(bankaccounts)) {
            bankaccountArr = bankaccounts.split(";");
            if (bankaccountArr != null && bankaccountArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.id").in(bankaccountArr));
            }
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        return list;
    }


    /**
     * 下载银行回单pdf 后上传到服务器
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject bankTradeDetailElectronTask(CtmJSONObject param) throws Exception {
        String logId = param.getString("logId");
        //银行类别
        String banktypes = param.getString(ICmpConstant.BANKTYPE_LOW);
        List<String> lockKeys = new ArrayList<>();
        if(!StringUtils.isEmpty(banktypes)){
            String[] banktypeArray = banktypes.split(";");
            lockKeys.addAll(Arrays.asList(banktypeArray));
        }else {
            lockKeys.add(ICmpConstant.QUERYBANKRECEIPTFILEKEY);
        }
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 1, 10, "bankTradeDetailElectronTask-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            //包装类，用于lambda表达式内存线程结果
            ThreadResult[] wrapperThreadResult = new ThreadResult[1];
            wrapperThreadResult[0] = new ThreadResult();
            try {
                CtmLockTool.executeInOneServiceExclusivelyBatchLock(lockKeys,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
                    if(lockstatus == LockStatus.GETLOCK_FAIL){
                        //加锁失败
                        wrapperThreadResult[0].getSucessReturnList().add(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400533", "系统正在对此账户拉取中") /* "系统正在对此账户拉取中" */) /* "系统正在对此账户拉取中" */);
                        return ;
                    }
                    //加锁成功
                    wrapperThreadResult[0] = taskBankReceiptService.bankTradeDetailElectronTask(param);
                });
            } catch (Exception e) {
                wrapperThreadResult[0].getErrorReturnList().add(e.getMessage());
            }finally {
                if(autoAssociatedDataExecutor != null){
                    autoAssociatedDataExecutor.shutdown();
                }
            }
            ThreadResult threadResult = wrapperThreadResult[0];
            TaskUtils.updateTaskLogbyThreadResult((Map<String,String>)param.get("ipaParams"), logId, "" ,threadResult);
            return threadResult;
        });
        CtmJSONObject result = new CtmJSONObject();
        result.put("asynchronized", true);
        return result;
    }


    /**
     * 获取需要下载回单的数据
     *
     * @param param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getBankElectronicReceiptList(CtmJSONObject param) throws Exception {
        HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        if (querydate.isEmpty()) {
            Date yesterday = DateUtils.getBeforeDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(dateFormat.format(yesterday))));
        } else if (querydate.containsKey(TaskUtils.TASK_NO_DATA)) {
            return null;
        } else {
            String begDate = querydate.get(TaskUtils.TASK_START_DATE);
            String endDate = querydate.get(TaskUtils.TASK_END_DATE);
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(begDate)));//大于等于开始时间
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(endDate)));
        }
        String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
        String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
        String[] accentityArr = null;
        if (!StringUtils.isEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        if (accentityArr != null && accentityArr.length > 0) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityArr));
        }
        String[] banktypeArr;
        if (!StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
            if (banktypeArr != null && banktypeArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.bankNumber.bank").in(banktypeArr));
            }
        }
        String[] currencyArr = null;
        if (!StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
            if (currencyArr != null && currencyArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("currency").in(currencyArr));
            }
        }
        conditionGroup.addCondition(QueryConditionGroup.or(QueryCondition.name("extendss").eq(""),
                QueryCondition.name("extendss").is_null()));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
    }

    /**
     * 采集任务对外接口
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> bankElectronicReceiptFile(CtmJSONObject param) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        //请求参数校验
        checkParams(param);
        //添加日志表
        saveLogger(param);
        //CtmJSONObject paramNew =new CtmJSONObject((CtmJSONObject) GsonHelper.FromJSon(GsonHelper.ToJSon(param), CtmJSONObject.class));
        CtmJSONObject paramNew = new CtmJSONObject((Map<String, Object>) param.clone());;
        boolean queryNumFlag = (boolean) param.get(ICmpConstant.QUERY_NUM_FLAG);
        if (queryNumFlag) {
            //调用账簿获取会计主体id
            getAccountBook(paramNew);
            //获取总数
            List<Map<String, Object>> bankElectronicReceiptCount = getBankElectronicReceiptInfoes(paramNew, true);
            Long count = (Long) bankElectronicReceiptCount.get(0).get("count");
            CtmJSONObject doc_data = new CtmJSONObject();
            doc_data.put(ICmpConstant.TOTAL, count);
            resultMap.put(ICmpConstant.DOC_DATA, doc_data);
        }else{
            //异步处理
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                //异步处理具体逻辑
                asyncProcess(paramNew);
            });
        }
        resultMap.put(ICmpConstant.CODE, "0000");
        resultMap.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        return resultMap;
    }

    /**
     * 异步处理具体逻辑
     *
     * @param paramNew
     */
    private void asyncProcess(CtmJSONObject paramNew) {
        try {
            if(!usePk(paramNew)){
                //调用账簿获取会计主体id
                getAccountBook(paramNew);
            }
            //获取总数
            List<Map<String, Object>> bankElectronicReceiptCount = getBankElectronicReceiptInfoes(paramNew, true);
            Long count = (Long) bankElectronicReceiptCount.get(0).get("count");
            if (count > 0) {
                paramNew.put(ICmpConstant.PAPERINDEX, 1);
                //获取100条明细
                List<Map<String, Object>> bankElectronicReceiptList = getBankElectronicReceiptInfoes(paramNew, false);//100
//                List bankElectronicReceiptListNew = (List) GsonHelper.FromJSon(GsonHelper.ToJSon(bankElectronicReceiptList), List.class);
                paramNew.put(ICmpConstant.TOTALCOUNT, count.intValue());
                if (!CollectionUtils.isEmpty(bankElectronicReceiptList)) {
                    //循环上传文件
                    uploadElect(bankElectronicReceiptList, paramNew);
                    List<Map<String, Object>> bankElectronicReceiptListNew = getBankElectronicReceiptInfoes(paramNew, false);//100
                    //上传文件与回写归档
                    bankElectronicReceiptUpload(bankElectronicReceiptListNew, paramNew);
                } else {
                    //没用数据情况调用回写接口
                    writeBack(null, paramNew, 0, false);
                }
            } else {
                //没用数据情况调用回写接口
                writeBack(null, paramNew, 0, false);
            }
        } catch (Exception e) {
            String resultMsg = e.getMessage();
            log.error("================bankElectronicReceiptFile executorServicePool getThreadPoolExecutor upload:" + e.getMessage(), e);
            //异常回调
            writeElectronicFail(resultMsg, paramNew.getString("callbackUrl"));
        }
    }

    //private String getStackMsg(Exception e) {
    //    final int MAX_STACK_LENGTH = 1000;
    //    StringWriter sw = new StringWriter();
    //    e.printStackTrace(new PrintWriter(sw));
    //    String stackTrace = sw.toString();
    //    String stackMsg = stackTrace.substring(0, Math.min(stackTrace.length(), MAX_STACK_LENGTH));
    //    return stackMsg;
    //}

    /**
     * 上传文件与回写归档逻辑
     *
     * @param receiptList
     * @param paramNew
     * @throws Exception
     */
    private void bankElectronicReceiptUpload(List<Map<String, Object>> receiptList, CtmJSONObject paramNew) throws Exception {
        //调用归档回调地址处理
        boolean isBackSuccess = writeElectronicSuccess(receiptList, paramNew, paramNew.getInteger("totalCount"));
        if (isBackSuccess) {
            paramNew.put(ICmpConstant.PAPERINDEX, paramNew.getInteger(ICmpConstant.PAPERINDEX) + 1);
            //重新查询 如果还有则重新调用
            List<Map<String, Object>> bankElectronicReceiptListsecond = getBankElectronicReceiptInfoes(paramNew, false);
            if (!CollectionUtils.isEmpty(bankElectronicReceiptListsecond) && bankElectronicReceiptListsecond.size() > 0) {
                bankElectronicReceiptUpload(bankElectronicReceiptListsecond, paramNew);
            }
        }
    }


    /**
     * 循环查询数据上传
     *
     * @param paramNew
     * @return
     */
    private void uploadElect(List<Map<String, Object>> receiptList, CtmJSONObject paramNew) throws Exception {
        //归档要求全部上传完成后在调回写接口
        updateBankElectInfo(paramNew, receiptList);
        paramNew.put(ICmpConstant.PAPERINDEX, paramNew.getInteger(ICmpConstant.PAPERINDEX) + 1);
        //重新查询 如果还有则重新调用
        List<Map<String, Object>> bankElectUploadListsecond = getBankElectronicReceiptInfoes(paramNew, false);
        if (!CollectionUtils.isEmpty(bankElectUploadListsecond) && bankElectUploadListsecond.size() > 0) {
            uploadElect(bankElectUploadListsecond, paramNew);
        } else {
            paramNew.put(ICmpConstant.PAPERINDEX, 1);
        }
    }


    /**
     * 记录上传成功后记录上传位置信息,避免重复上传
     */
    private void writeElectronicFail(String msg, String callbackUrl) {
        try {
            //调用回写接口---通知归档失败原因
            CtmJSONObject request = new CtmJSONObject();
            request.put(ICmpConstant.STATUS, "-1");
            request.put(ICmpConstant.MSG, msg);
            CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(callbackUrl, request);
            if (!(null != jsonObject && "0000".equals(jsonObject.getString(ICmpConstant.CODE)))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102219"),jsonObject.getString(ICmpConstant.MSG));
            }
        } catch (Exception e) {
            log.error("================bankElectronicReceiptFile executorServicePool writeElectronicFail:" + e.getMessage(), e);
        }
    }

    /**
     * 下载pdf 后调用上传接口
     *
     * @param bankElectronicReceiptUpload
     * @throws Exception
     */
    private void uploadElectronic(Map<String, Object> bankElectronicReceiptUpload, CtmJSONObject fileServer) throws Exception {
        //if (null == bankElectronicReceiptUpload.get(REMOTEPATH) || StringUtils.isEmpty(bankElectronicReceiptUpload.get(REMOTEPATH).toString())) {
            byte[] bytes = bankReceiptService.downloadAccountReceipt(bankElectronicReceiptUpload.get(ICmpConstant.EXTENDSS).toString());
            String defaultDir = AppContext.getEnvConfig(EXPORT_EXCEL_OSS_DIR_CONFIG_KEY);
//            String fileName = new SecureRandom().nextInt(100) + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026186") + ".pdf" /* "银行电子回单.pdf" */;
//            String remotePath = defaultDir + DateUtils.getTodayShort() + "/" + fileName;
//            String fileName = UUID.randomUUID() + ".pdf";
            String fileName = Objects.toString(bankElectronicReceiptUpload.get(ICmpConstant.FILENAME), UUID.randomUUID() + "_default" + ".pdf");
            String remotePath = getRemotePath(defaultDir, bankElectronicReceiptUpload, fileName);
            bankElectronicReceiptUpload.put(REMOTEPATH, remotePath);
            bankElectronicReceiptUpload.put(ICmpConstant.FILENAME, fileName);
            bankElectronicReceiptUpload.put(ICmpConstant.FILESIZE, bytes.length);
            InputStream inputStream = new ByteArrayInputStream(bytes);
            UploadClient uploadClient = UploadClient.getInstance(fileServer.getString("serverType"), fileServer.getString("endpoint"), fileServer.getString("accessKey"), fileServer.getString("secretKey"), fileServer.getString("bucketName"));
            uploadClient.upload(remotePath, inputStream);
        //}
    }

    /**
     * 下载pdf 后调用上传接口
     *
     * @param bankElectronicReceiptUpload
     * @throws Exception
     */
    private void uploadElectronicConfirm(Map<String, Object> bankElectronicReceiptUpload, CtmJSONObject fileServer) throws Exception {
//        if (null == bankElectronicReceiptUpload.get(REMOTEPATH) || StringUtils.isEmpty(bankElectronicReceiptUpload.get(REMOTEPATH).toString())) {
        byte[] bytes = bankReceiptService.downloadAccountReceipt(bankElectronicReceiptUpload.get("statementfileid").toString());
        String defaultDir = AppContext.getEnvConfig(EXPORT_EXCEL_OSS_DIR_CONFIG_KEY);
//            String fileName = new SecureRandom().nextInt(100) + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026186") + ".pdf" /* "银行电子回单.pdf" */;
//            String remotePath = defaultDir + DateUtils.getTodayShort() + "/" + fileName;
//        不改原文件名
        String fileName = Objects.toString(bankElectronicReceiptUpload.get("statement_name"), UUID.randomUUID() + "_default" + ".pdf");
        String remotePath = getRemotePath(defaultDir, bankElectronicReceiptUpload, fileName);
        bankElectronicReceiptUpload.put(REMOTEPATH, remotePath);
        bankElectronicReceiptUpload.put(ICmpConstant.FILENAME, fileName);
        bankElectronicReceiptUpload.put(ICmpConstant.FILESIZE, bytes.length);
        InputStream inputStream = new ByteArrayInputStream(bytes);
        UploadClient uploadClient = UploadClient.getInstance(fileServer.getString("serverType"), fileServer.getString("endpoint"), fileServer.getString("accessKey"), fileServer.getString("secretKey"), fileServer.getString("bucketName"));
        uploadClient.upload(remotePath, inputStream);
//        }
    }

    /**
     * 回写交易回单 回写电子归档
     *
     * @param bankElectBackFinal
     * @throws Exception
     */
    private boolean writeElectronicSuccess(List<Map<String, Object>> bankElectBackFinal, CtmJSONObject param, int totalCount) throws Exception {
        //更新交易回单数据
        CopyOnWriteArrayList<BankElectronicReceipt> bankElectronicSaveList = new CopyOnWriteArrayList<>();
        for (int m = 0; m < bankElectBackFinal.size(); m++) {
            Map<String, Object> bankElectronicReceiptMap = bankElectBackFinal.get(m);
            List<Map<String, Object>> bankReconciliationList = updateBankElectronicReceipt(bankElectronicReceiptMap);
            if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                BankElectronicReceipt bankElectronicReceiptUpdate = new BankElectronicReceipt();
                Map<String, Object> bankReconciliationNew = bankReconciliationList.get(0);
                String othercheckno = null != bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO) ? bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO).toString() : null;
                if (!StringUtils.isEmpty(othercheckno)) {
                    bankElectronicReceiptUpdate.setOther_checkno(othercheckno);
                    bankElectronicReceiptMap.put(ICmpConstant.OTHERCHECKNO, othercheckno);
                }
                bankElectronicReceiptUpdate.setId(Long.parseLong(bankElectronicReceiptMap.get(ICmpConstant.ID).toString()));
                bankElectronicReceiptUpdate.setEntityStatus(EntityStatus.Update);
                bankElectronicSaveList.add(bankElectronicReceiptUpdate);
            }
        }
        if (!CollectionUtils.isEmpty(bankElectronicSaveList)) {
            EntityTool.setUpdateStatus(bankElectronicSaveList);
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicSaveList);
        }
        //调用归档回写接口
        return writeBack(bankElectBackFinal, param, totalCount, false);
    }

    /**
     * 调用归档回写接口回写数据
     *
     * @param bankElectronicWriteList
     * @param param
     * @param totalCount
     * @return
     * @throws Exception
     */
    public boolean writeBack(List<Map<String, Object>> bankElectronicWriteList, CtmJSONObject param, int totalCount, boolean Repair) throws Exception {
        //组装回写数据
        CtmJSONObject jsonParam = assemblyParam(bankElectronicWriteList, param, totalCount, Repair);
        CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(param.getString(ICmpConstant.CALLBACKURL), jsonParam);
        if (!(null != jsonObject && "0000".equals(jsonObject.getString(ICmpConstant.CODE)))) {
            log.error("================bankElectronicReceiptFile executorServicePool writeBack:" + CtmJSONObject.toJSONString(jsonObject));
            return false;
        }
        return true;
    }

    /**
     * 封装归档请求参数
     *
     * @param bankElectronicWriteList {
     *                                "taskId":"S202202-708946-b57fbb317187",
     *                                "ticketInfo":"bankreceipt",
     *                                "docList":[
     *                                {
     *                                "bankReceiptInfo":{
     *                                "clientNo":"account/info/a.xml",
     *                                "accountingSubject":"12345",
     *                                "fileUrl":"1",
     *                                "receivePaymentTime":"12312adeexsq"
     *                                }
     *                                }
     *                                ],
     *                                "totalCount":624,
     *                                "orgCode":"org_code_0000L1C4LEJG2CLUYQ0000",
     *                                "accountYear":"2022",
     *                                "accountMonth":"10",
     *                                "docType":"bankReceipt",
     *                                "srcSys":"test",
     *                                "status":1,
     *                                "msg":"success",
     *                                }
     * @return
     */
    private CtmJSONObject assemblyParam(List<Map<String, Object>> bankElectronicWriteList, CtmJSONObject param, int totalCount, boolean Repair) throws Exception {
        List<Map<String, Object>> docList = new ArrayList<>();
        CtmJSONObject json = new CtmJSONObject();
        json.put(ICmpConstant.TASKID, param.getString(ICmpConstant.TASKID));//任务id
        json.put(ICmpConstant.SRCSYS, "ctmcmp");//来源系统编码
        json.put("docList", docList);//档案明细列表，最大100
        json.put(ICmpConstant.TICKETINFO, "bankReceipt");//银行回单传 bankreceipt
        json.put(ICmpConstant.TOTALCOUNT, totalCount);//任务下档案总数
        json.put(ICmpConstant.ORGCODE, param.getString(ICmpConstant.ACCOUNTBOOKCODE));//全宗编码
        if (!Repair) {
            json.put(ACCOUNTYEAR, param.getString(ACCOUNTYEAR));//会计年
            json.put(ACCOUNTMONTH, param.getString(ACCOUNTMONTH));//会计月
        }
        json.put(ICmpConstant.DOCTYPE, param.getString(ICmpConstant.TYPE));//档案类型（即档案目录前缀）、或单据类 型编码
        json.put(ICmpConstant.STATUS, "1");//1为成功，-1为失败
        json.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);//任务失败原因
        if (!CollectionUtils.isEmpty(bankElectronicWriteList)) {
            BigDecimal midOne = new BigDecimal(1);
            for (Map<String, Object> bankElectronicReceiptJson : bankElectronicWriteList) {
                Map<String, Object> bankReceiptInfoMap = new HashMap<>();
                Map<String, Object> bankElectronicReceiptMap = new HashMap<>();
                bankElectronicReceiptMap.put("clientNo", bankElectronicReceiptJson.get("custno"));//客户号

                EnterpriseBankAcctVO enterpriseBankAcctVO = bankReceiptService.getEnterpriseBankAcctVO(bankElectronicReceiptJson);
                if (null != enterpriseBankAcctVO) {
                    bankElectronicReceiptMap.put("paymentAccount", enterpriseBankAcctVO.getAccount());//银行账号（本方账号）
//                    bankElectronicReceiptMap.put("paymentAccount",bankElectronicReceiptJson.get("enterpriseBankAccount"));//银行账号（本方账号）
                }

                if (null != bankElectronicReceiptJson.get(ICmpConstant.TRANDATE)) {
                    bankElectronicReceiptMap.put("receivePaymentDate", DateUtils.dateFormat((Date) bankElectronicReceiptJson.get(ICmpConstant.TRANDATE), null));//交易日期（收付日期）
                }
                if (null != bankElectronicReceiptJson.get("tranTime")) {
                    //bankElectronicReceiptMap.put("receivePaymentTime", DateUtils.getLocalDateTimeString((LocalDateTime) bankElectronicReceiptJson.get("tranTime")).get());//交易时间（收付时间）
                    String timeStr = bankElectronicReceiptJson.get("tranTime").toString();
                    //Date tranTime = DateUtils.dateParse(timeStr, DateUtils.YYYYMMDDHHMMSS);
                    Date tranTime = DateUtils.dateParse(timeStr, DateUtils.HOUR_PATTERN);
                    bankElectronicReceiptMap.put("receivePaymentTime",tranTime);
                }
                //币种
                if (null != bankElectronicReceiptJson.get(ICmpConstant.CURRENCY)) {
                    CurrencyTenantDTO currencyTenantDTO = getCurrencyTenantDTO((String) bankElectronicReceiptJson.get(ICmpConstant.CURRENCY));
                    if (null != currencyTenantDTO) {
                        bankElectronicReceiptMap.put(ICmpConstant.CURRENCY, currencyTenantDTO.getName());
                    }
                }
                bankElectronicReceiptMap.put("borrowingDirection", bankElectronicReceiptJson.get("dc_flag"));//借贷方向：1-借付款；2-贷-收款
                //交易金额
                BigDecimal tranAmt = (BigDecimal) bankElectronicReceiptJson.get("tran_amt");
                bankElectronicReceiptMap.put("moneyAmount", tranAmt == null ? null :tranAmt.divide(midOne, 2, RoundingMode.DOWN));
                bankElectronicReceiptMap.put("receiveAccount", bankElectronicReceiptJson.get("to_acct_no"));//对方账号
                bankElectronicReceiptMap.put("receiveAccountName", bankElectronicReceiptJson.get("to_acct_name"));//对方户名
                bankElectronicReceiptMap.put("receiveBank", bankElectronicReceiptJson.get("to_acct_bank"));//对方开户行（银行类别，如中国银行）
                bankElectronicReceiptMap.put("receiveBankSite", bankElectronicReceiptJson.get("to_acct_bank_name"));//对方开户行名（银行网点，如中国银行北清路支行）
                bankElectronicReceiptMap.put("useage", bankElectronicReceiptJson.get("use_name"));//用途
                bankElectronicReceiptMap.put("abstracts", bankElectronicReceiptJson.get("remark"));//摘要
                bankElectronicReceiptMap.put("bankTransactionSerial", bankElectronicReceiptJson.get("bankseqno"));//银行交易流水号（银行给的回单的唯一号码）
                bankElectronicReceiptMap.put("receiptNo", bankElectronicReceiptJson.get("receiptno"));//回单编号
                bankElectronicReceiptMap.put("ledgerBlendingCode", bankElectronicReceiptJson.get(ICmpConstant.OTHERCHECKNO));//总账勾对码（银行回单与总账凭证关联的标志)
                //会计主体
                if (null != bankElectronicReceiptJson.get(ICmpConstant.ACCENTITY)) {
                    FinOrgDTO finOrgDTO = getFinOrgDTO((String) bankElectronicReceiptJson.get(ICmpConstant.ACCENTITY));
                    if (null != finOrgDTO) {
                        bankElectronicReceiptMap.put("accountingSubject", finOrgDTO.getName());//会计主体
                    }
                }
                //银行交易回单：返回“数据来源”枚举值，并进行转移，如银企联下载、手工导入等
                Object v = bankElectronicReceiptJson.get("dataOrigin");
                if (v != null) {
                    String dataOriginName = DateOrigin.find((Number) v).getName();
                    bankElectronicReceiptMap.put("srcSystem", dataOriginName);
                }
                bankElectronicReceiptMap.put("fileUrl", bankElectronicReceiptJson.get(REMOTEPATH));//文件url
                bankElectronicReceiptMap.put("fileName", bankElectronicReceiptJson.get(ICmpConstant.FILENAME));//文件名称
                bankElectronicReceiptMap.put("fileSize", Long.valueOf((int) bankElectronicReceiptJson.get(ICmpConstant.FILESIZE)));//文件大小
                bankElectronicReceiptMap.put(ICmpConstant.PK, String.valueOf(bankElectronicReceiptJson.get(ICmpConstant.ID)));//id
                bankReceiptInfoMap.put("bankReceiptInfo", bankElectronicReceiptMap);
                docList.add(bankReceiptInfoMap);
            }
        }
        return json;
    }

    /**
     * 万能接口获取币种信息
     *
     * @param currencyId
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO getCurrencyTenantDTO(String currencyId) throws Exception {
        String currencyKey = "assemblyParam" + InvocationInfoProxy.getTenantid() + currencyId;
        CurrencyTenantDTO currencyTenantDTO = currencyTenantDTOCache.getIfPresent(currencyKey);
        if (null == currencyTenantDTO) {
            currencyTenantDTO = baseRefRpcService.queryCurrencyById(currencyId);
            currencyTenantDTOCache.put(currencyKey, currencyTenantDTO);
        }
        return currencyTenantDTO;
    }

    /**
     * 万能接口获取会计主体信息
     *
     * @param accentityId
     * @return
     * @throws Exception
     */
    private FinOrgDTO getFinOrgDTO(String accentityId) throws Exception {
        String accentityKey = "assemblyParam" + InvocationInfoProxy.getTenantid() + accentityId;
        FinOrgDTO finOrgDTO = finOrgDTOCache.getIfPresent(accentityKey);
        if (null == finOrgDTO) {
            finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentityId);
            finOrgDTOCache.put(accentityKey, finOrgDTO);
        }
        return finOrgDTO;
    }

    /**
     * 根据条件获取银行交易回单数据
     *
     * @param param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getBankElectronicReceiptInfoes(CtmJSONObject param, boolean isCount) throws Exception {
        //查询数据
        QuerySchema schema = new QuerySchema();
        if (isCount) {
            schema = QuerySchema.create().addSelect("count(id)");
        } else {
            schema = QuerySchema.create().addSelect("*");
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (usePk(param)) {
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ID).in(param.getJSONArray(DOC_PK_LIST)));
        } else {
            List<String> accountByOrg = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(Arrays.asList(param.getString(ICmpConstant.ACCENTITY)));
            if (CollectionUtils.isEmpty(accountByOrg)) {
                if (isCount) {
                    List<Map<String, Object>> emptyCountList = new ArrayList<>();
                    HashMap<String, Object> emptyCountHashMap = new HashMap<>();
                    emptyCountHashMap.put("count", 0L);
                    emptyCountList.add(emptyCountHashMap);
                    return emptyCountList;
                } else {
                    return new ArrayList<>();
                }
            }
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ENTERPRISE_BANK_ACCOUNT).in(accountByOrg));
            if (null != param && null != param.getDate(ENDDATE)) {
                conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.TRANDATE).egt(DateUtils.dateFormat(param.getDate(STARTDATE), null)));
                conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.TRANDATE).elt(DateUtils.dateFormat(param.getDate(ENDDATE), null)));
            } else {
                conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.TRANDATE).eq(DateUtils.dateFormat(param.getDate(STARTDATE), null)));
            }
        }
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.EXTENDSS).is_not_null());
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.EXTENDSS).not_eq("''"));
        schema.addCondition(conditionGroup);
        if (isCount) {
            return MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
        } else {
            schema.addPager(param.getInteger(ICmpConstant.PAPERINDEX), 100);
            List<Map<String, Object>> bankElectronicReceiptList = MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
            if (bankElectronicReceiptList.size() <= 100) {
                return bankElectronicReceiptList;
            } else {
                return new ArrayList<>();
            }
        }
    }

    /**
     * 根据时间类型拼接时间时间字段
     *
     * @param param
     * @throws ParseException
     */
    private void getStartDate(CtmJSONObject param) throws Exception {
        String range = param.getString(RANGE);
        String accountYear = param.getString(ACCOUNTYEAR);
        String accountMonth = param.getString(ACCOUNTMONTH);
        String accountDate = param.getString(ACCOUNTDATE);
        if (ICmpConstant.YEAR.equals(range) && !StringUtils.isEmpty(accountYear)) {
            param.put(STARTDATE, DateUtils.getYearFirst(Integer.parseInt(accountYear)));
            param.put(ENDDATE, DateUtils.getYearLast(Integer.parseInt(accountYear)));
        } else if (ICmpConstant.MONTH.equals(range) && !StringUtils.isEmpty(accountYear) && !StringUtils.isEmpty(accountMonth)) {
            String monthString = accountYear.concat("-").concat(accountMonth).concat("-01");
            Date monthStartDate = DateUtils.dateParse(monthString, null);
            param.put(STARTDATE, monthStartDate);
            param.put(ENDDATE, DateUtils.maxDateOfMonth(monthStartDate));
        } else if (ICmpConstant.DAY.equals(range) && !StringUtils.isEmpty(accountDate)) {
            param.put(ENDDATE, DateUtils.dateParse(accountDate, null));
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102220"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0019A", "请检查，采集时间范围是否正确。") /* "请检查，采集时间范围是否正确。" */ + range + ";" + accountYear + ";" + accountMonth + ";" + accountDate);
        }
    }

    /**
     * 校验公共参数是否正确
     *
     * @param param
     * @return
     * @throws ParseException
     */
    private void checkCommParams(CtmJSONObject param) throws Exception {
        CtmJSONObject orgInfo = param.getJSONObject(ORGINFO);
        boolean usePk = usePk(param);
        if (!usePk) {
            if (null == orgInfo) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102221"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0019C", "请检查，单位信息是否为空。") /* "请检查，单位信息是否为空。" */);
            }
            if (ICmpConstant.BOOK.equals(orgInfo.getString(ICmpConstant.TYPE))) {
                if (StringUtils.isEmpty(orgInfo.getString(ICmpConstant.PK))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102222"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0019F", "请检查，账簿pk是否为空;") /* "请检查，账簿pk是否为空;" */ + CtmJSONObject.toJSONString(orgInfo));
                }
            }
        }
        CtmJSONObject fileServer = param.getJSONObject(ICmpConstant.FILESERVER);
        if (null == fileServer) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102223"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00196", "请检查，文件服务器配置是否为空。") /* "请检查，文件服务器配置是否为空。" */);
        }
        if (StringUtils.isEmpty(param.getString(ICmpConstant.CALLBACKURL))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102224"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00198", "请检查，档案回调地址是否为空。") /* "请检查，档案回调地址是否为空。" */);
        }
    }

    private static boolean usePk(CtmJSONObject param) {
        //按id查回单
        CtmJSONArray docPkList = param.getJSONArray(DOC_PK_LIST);
        boolean usePk = CollectionUtils.isNotEmpty(docPkList);
        return usePk;
    }

    /**
     * 校验参数是否正确
     *
     * @param param
     * @return
     * @throws ParseException
     */
    private void checkParams(CtmJSONObject param) throws Exception {
        checkCommParams(param);
        getStartDate(param);
    }


    /**
     * 通过账簿id查询会计主体
     *
     * @param param
     */
    private void getAccountBook(CtmJSONObject param) {
        CtmJSONObject orgInfo = param.getJSONObject(ORGINFO);
        Object type = orgInfo.get(ICmpConstant.TYPE);
        if (type != null && "unit".equals(type)) {
            //unit 业务单元 book 核算账簿（默认）
            param.put(ICmpConstant.ACCENTITY, orgInfo.get(ICmpConstant.PK));
            param.put(ICmpConstant.ACCOUNTBOOKCODE, orgInfo.get(ICmpConstant.CODE));
        } else {
            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put(ICmpConstant.ID, orgInfo.get(ICmpConstant.PK));
            List<AccountBook> accountBookList = QueryBaseDocUtils.getAccentityByAccount(accountInfo);
            if (CollectionUtils.isEmpty(accountBookList)) {
                log.error("================bankElectronicReceiptFile QueryBaseDocUtils getAccentityByAccount is empty" + CtmJSONObject.toJSONString(orgInfo));
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102225"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00197", "通过账簿id未查询到对应的账簿。") /* "通过账簿id未查询到对应的账簿。" */);
            } else {
                AccountBook accountBook = accountBookList.get(0);
                param.put(ICmpConstant.ACCENTITY, accountBook.getAccentity());
                param.put(ICmpConstant.ACCOUNTBOOKCODE, accountBook.getCode());
            }
        }
    }

    /**
     * 插入记录信息
     *
     * @param param
     * @throws Exception
     */
    private void saveLogger(CtmJSONObject param) throws Exception {
        BankElectronicReceiptFileLogger bankElectronicReceiptFileLogger = new BankElectronicReceiptFileLogger();
        bankElectronicReceiptFileLogger.setAccountDate(null != param.getString(ACCOUNTDATE) ? param.getString(ACCOUNTDATE) : null);
        bankElectronicReceiptFileLogger.setAccountMonth(null != param.getString(ACCOUNTMONTH) ? param.getString(ACCOUNTMONTH) : null);
        bankElectronicReceiptFileLogger.setAccountYear(null != param.getString(ACCOUNTYEAR) ? param.getString(ACCOUNTYEAR) : null);
        bankElectronicReceiptFileLogger.setArchivesType(null != param.getString(ICmpConstant.TYPE) ? param.getString(ICmpConstant.TYPE) : null);
        bankElectronicReceiptFileLogger.setCallbackUrl(null != param.getString(ICmpConstant.CALLBACKURL) ? param.getString(ICmpConstant.CALLBACKURL) : null);
        bankElectronicReceiptFileLogger.setShowUpper(null != param.getString(ICmpConstant.SHOWUPPER) ? param.getString(ICmpConstant.SHOWUPPER) : null);
        bankElectronicReceiptFileLogger.setIrange(null != param.getString(RANGE) ? param.getString(RANGE) : null);
        bankElectronicReceiptFileLogger.setTaskId(null != param.getString(ICmpConstant.TASKID) ? param.getString(ICmpConstant.TASKID) : null);
        bankElectronicReceiptFileLogger.setTicketInfo(null != param.getString(ICmpConstant.TICKETINFO) ? param.getString(ICmpConstant.TICKETINFO) : null);
        CtmJSONObject fileServer = param.getJSONObject(ICmpConstant.FILESERVER);
        bankElectronicReceiptFileLogger.setSecretKey(null != fileServer.getString(ICmpConstant.SECRETKEY) ? fileServer.getString(ICmpConstant.SECRETKEY) : null);
        bankElectronicReceiptFileLogger.setServerType(null != fileServer.getString(ICmpConstant.SERVERTYPE) ? fileServer.getString(ICmpConstant.SERVERTYPE) : null);
        bankElectronicReceiptFileLogger.setAccessKey(null != fileServer.getString("accessKey") ? fileServer.getString("accessKey") : null);
        bankElectronicReceiptFileLogger.setBucketName(null != fileServer.getString(ICmpConstant.BUCKETNAME) ? fileServer.getString(ICmpConstant.BUCKETNAME) : null);
        bankElectronicReceiptFileLogger.setEndpoint(null != fileServer.getString(ICmpConstant.ENDPOINT) ? fileServer.getString(ICmpConstant.ENDPOINT) : null);
        CtmJSONObject orgInfo = param.getJSONObject(ORGINFO);
        bankElectronicReceiptFileLogger.setPk(null != orgInfo.getString(ICmpConstant.PK) ? orgInfo.getString(ICmpConstant.PK) : null);
        //unit 业务单元 book 核算账簿（默认）
        bankElectronicReceiptFileLogger.setItype(null != orgInfo.getString(ICmpConstant.TYPE) ? orgInfo.getString(ICmpConstant.TYPE) : null);
        bankElectronicReceiptFileLogger.setEntityStatus(EntityStatus.Insert);
        bankElectronicReceiptFileLogger.setId(ymsOidGenerator.nextId());
        CmpMetaDaoHelper.insert(BankElectronicReceiptFileLogger.ENTITY_NAME, bankElectronicReceiptFileLogger);
    }

    /**
     * 更新银行交易回单other_checkno
     *
     * @param bankElectronicReceipt
     * @throws Exception
     */
    private List<Map<String, Object>> updateBankElectronicReceipt(Map<String, Object> bankElectronicReceipt) throws Exception {
        if (bankElectronicReceipt == null) {
            return null;
        }
        //查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(bankElectronicReceipt.get(ICmpConstant.ACCENTITY)));
        //conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(bankElectronicReceipt.get(ICmpConstant.CURRENCY)));
        //conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.BANKACCOUNT).eq(bankElectronicReceipt.get("enterpriseBankAccount")));
        //conditionGroup.appendCondition(QueryCondition.name("bank_seq_no").eq(bankElectronicReceipt.get("bankseqno")));
        //只有勾对过的，才会返回勾对码，才需要查出来
        conditionGroup.appendCondition(QueryCondition.name("other_checkflag").eq("1"));
        //conditionGroup.appendCondition(QueryCondition.name("tran_date").eq(bankElectronicReceipt.get("tranDate")));
        //通过id查询对应的银行对账单，只有关联过回单的才能查到
        //没做关联的，不返回勾对号
        Object bankreconciliationid = bankElectronicReceipt.get("bankreconciliationid");
        if (bankreconciliationid == null) {
            return null;
        }
        conditionGroup.appendCondition(QueryCondition.name("id").eq(bankreconciliationid));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> bankReconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema);
        if (!CollectionUtils.isEmpty(bankReconciliationList) && 1 == bankReconciliationList.size()) {
            return bankReconciliationList;
        } else {
            return null;
        }
    }


    @Override
    public Map<String, Object> repairBankElectFile(CtmJSONObject param) throws Exception {
        //请求参数校验
        checkRepairParams(param);
        //添加日志表
        saveLogger(param);
        //CtmJSONObject paramNew = (CtmJSONObject) GsonHelper.FromJSon(GsonHelper.ToJSon(param), CtmJSONObject.class);
        CtmJSONObject paramNew = new CtmJSONObject((Map<String, Object>) param.clone());;
        //异步处理
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            //一键补采异步处理具体逻辑
            asyncRepair(paramNew);
        });
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(ICmpConstant.CODE, "0000");
        resultMap.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        return resultMap;
    }

    /**
     * 校验参数是否正确
     *
     * @param param
     * @return
     * @throws ParseException
     */
    private void checkRepairParams(CtmJSONObject param) throws Exception {
        checkCommParams(param);
        CtmJSONArray docPkList = param.getJSONArray(ICmpConstant.DOCPKLIST);
        if (docPkList.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102226"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001A1", "请检查，单据、或档案主键列表是否为空。") /* "请检查，单据、或档案主键列表是否为空。" */);
        }
        log.error("=================repairBankElectFile file checkRepairParams:" + CtmJSONObject.toJSONString(docPkList));
        List<String> pkListString = docPkList.toJavaList(String.class);
        List<Long> bankElectIdes = pkListString.stream().map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        List<Map<String, Object>> bankElectUploadCount = getBankElectByIdes(bankElectIdes, true);
        Long count = (Long) bankElectUploadCount.get(0).get("count");
        if (docPkList.size() != count.intValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102227"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00199", "未查询到docPkList部分数据") /* "未查询到docPkList部分数据" */);
        }
    }

    /**
     * 一键补采异步处理具体逻辑
     *
     * @param paramNew
     */
    private void asyncRepair(CtmJSONObject paramNew) {
        try {
            //调用账簿获取会计主体id
            getAccountBook(paramNew);
            //获取总数
            CtmJSONArray docPkList = paramNew.getJSONArray(ICmpConstant.DOCPKLIST);
            int count = docPkList.size();
            if (count > 0) {
                paramNew.put("totalCount", count);
                List<String> docPkListString = docPkList.toJavaList(String.class);
                //一键补采上传文件与回写归档
                uploadElectRepair(docPkListString, paramNew, count);
                //调用回写接口
                repairWriteBackElect(docPkListString, paramNew, count);
            } else {
                //没用数据情况调用回写接口
                writeBack(null, paramNew, 0, true);
            }
        } catch (Exception e) {
            String resultMsg = e.getMessage();
            log.error("================repairBankElectFile executorServicePool getThreadPoolExecutor asyncRepair:" + e.getMessage(), e);
            //异常回调
            writeElectronicFail(resultMsg, paramNew.getString("callbackUrl"));
        }
    }

    /**
     * 一键补采循环查询数据上传
     *
     * @param paramNew
     * @return
     */
    private boolean uploadElectRepair(List<String> docPkListString, CtmJSONObject paramNew, int count) throws Exception {
        if (count > 100) {
            List<List<String>> bankElectIdListes = ListUtils.partition(docPkListString, 100); //分组100条处理一次
            for (int i = 0; i < bankElectIdListes.size(); i++) {
                List<String> bankElectIdFinalIdes = bankElectIdListes.get(i);
                //归档要求全部上传完成后在调回写接口
                updateBankElectRepair(paramNew, bankElectIdFinalIdes);
            }
        } else {
            //归档要求全部上传完成后在调回写接口
            updateBankElectRepair(paramNew, docPkListString);
        }
        return true;
    }

    /**
     * 更新银行交易回单数据
     *
     * @param paramNew
     * @param docPkListString
     * @throws Exception
     */
    public void updateBankElectRepair(CtmJSONObject paramNew, List<String> docPkListString) throws Exception {
        List<Long> bankElectIdes = docPkListString.stream().map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        List<Map<String, Object>> bankElectUploadFinal = getBankElectByIdes(bankElectIdes, false);
        if (!CollectionUtils.isEmpty(bankElectUploadFinal)) {
            updateBankElectInfo(paramNew, bankElectUploadFinal);
        }
    }

    /**
     * 上传后更新数据
     *
     * @param paramNew
     * @param bankElectUploadFinal
     * @throws Exception
     */
    public void updateBankElectInfo(CtmJSONObject paramNew, List<Map<String, Object>> bankElectUploadFinal) throws Exception {
        ArrayList<BankElectronicReceipt> bankElectNewList = new ArrayList<>();
        for (Map<String, Object> bankElectronicReceiptUpload : bankElectUploadFinal) {
            // 下载pdf 后调用上传接口
            // 从yms控制台获取业务配置参数 如果是true 说明是专属化环境
            String specialFlag = AppContext.getEnvConfig("Special_flag");
            if (!StringUtils.isEmpty(specialFlag) && "true".equals(specialFlag)) {
                uploadElectronicForSpecial(bankElectronicReceiptUpload, paramNew.getJSONObject(ICmpConstant.FILESERVER));
            } else {
                uploadElectronic(bankElectronicReceiptUpload, paramNew.getJSONObject(ICmpConstant.FILESERVER));
            }
            //uploadElectronic(bankElectronicReceiptUpload, paramNew.getJSONObject(ICmpConstant.FILESERVER));
            BankElectronicReceipt bankElectronicReceiptPath = new BankElectronicReceipt();
            bankElectronicReceiptPath.setId(bankElectronicReceiptUpload.get(ICmpConstant.ID));
            bankElectronicReceiptPath.setRemotepath(bankElectronicReceiptUpload.get(REMOTEPATH).toString());
            bankElectronicReceiptPath.setFilename(bankElectronicReceiptUpload.get(ICmpConstant.FILENAME).toString());
            bankElectronicReceiptPath.setFilesize((int) bankElectronicReceiptUpload.get(ICmpConstant.FILESIZE));
            bankElectronicReceiptPath.setEntityStatus(EntityStatus.Update);
            bankElectNewList.add(bankElectronicReceiptPath);
        }
        EntityTool.setUpdateStatus(bankElectNewList);
        MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectNewList);
    }

    /**
     * 下载pdf 后调用上传接口 专属化环境
     *
     * @param bankElectronicReceiptUpload
     * @throws Exception
     */
    private void uploadElectronicForSpecial(Map<String, Object> bankElectronicReceiptUpload, CtmJSONObject fileServer) throws Exception {
        //if (null == bankElectronicReceiptUpload.get(REMOTEPATH) || StringUtils.isEmpty(bankElectronicReceiptUpload.get(REMOTEPATH).toString())) {
            // 下载回单文件
            String sourcefileId = bankElectronicReceiptUpload.get(ICmpConstant.EXTENDSS).toString();
            InputStream inputStream = cooperationFileUtilService.queryInputStreamByFileId(sourcefileId);
            // 上传至文件服务器
            String defaultDir = AppContext.getEnvConfig(EXPORT_EXCEL_OSS_DIR_CONFIG_KEY);
//            String fileName = new SecureRandom().nextInt(100) + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026186") + ".pdf" /* "银行电子回单.pdf" */;
//            String remotePath = defaultDir + DateUtils.getTodayShort() + "/" + fileName;
//            String fileName = UUID.randomUUID() + ".pdf";
            String fileName = Objects.toString(bankElectronicReceiptUpload.get(ICmpConstant.FILENAME), UUID.randomUUID() + "_default" + ".pdf");
            String remotePath = getRemotePath(defaultDir, bankElectronicReceiptUpload, fileName);
            bankElectronicReceiptUpload.put(REMOTEPATH, remotePath);
            bankElectronicReceiptUpload.put(ICmpConstant.FILENAME, fileName);
            bankElectronicReceiptUpload.put(ICmpConstant.FILESIZE, 0);
            // InputStream inputStream = new ByteArrayInputStream(bytes);
            UploadClient uploadClient = UploadClient.getInstance(fileServer.getString("serverType"), fileServer.getString("endpoint"), fileServer.getString("accessKey"), fileServer.getString("secretKey"), fileServer.getString("bucketName"));
            uploadClient.upload(remotePath, inputStream);
        //}
    }

    private String getRemotePath(String defaultDir, Map<String, Object> bizObject, String fileName) {
        String fileType = CooperationFileUtilService.getFileType(fileName);
        //需要全路径名，加一级会计主体，否则单个文件夹下文件太多可能会报错；文件名改用uuid，防止不规范的文件名导致报错[比如带空格的]
        String remotePath = defaultDir + DateUtils.getYear(new Date()) + "/" + DateUtils.getMonth(new Date()) + "/" + getAccentity(bizObject) + "/" + UUID.randomUUID() + "." + fileType;
        return remotePath;
    }

    private static String getAccentity(Map<String, Object> bizObject) {
        Object accentity = bizObject.get(ICmpConstant.ACCENTITY);
        if (accentity instanceof String  && StringUtils.isNotEmpty(accentity.toString())) {
            return accentity.toString();
        } else {
            return "defaultAccentity";
        }
    }

    /**
     * 根据条件获取银行交易回单数据
     *
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getBankElectByIdes(List<Long> ides, boolean isCount) throws Exception {
        //查询数据
        QuerySchema schema = new QuerySchema();
        if (isCount) {
            schema = QuerySchema.create().addSelect("count(id)");
        } else {
            schema = QuerySchema.create().addSelect("*");
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ID).in(ides));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(BankElectronicReceipt.ENTITY_NAME, schema);
    }

    /**
     * 调用归档回写逻辑
     *
     * @param paramNew
     */
    private void repairWriteBackElect(List<String> docPkListString, CtmJSONObject paramNew, int totalCount) throws Exception {

        if (totalCount > 100) {
            List<List<String>> bankElectIdListes = ListUtils.partition(docPkListString, 100); //分组100条处理一次
            for (int i = 0; i < bankElectIdListes.size(); i++) {
                //归档要求全部上传完成后在调回写接口
                writeBankElectRepair(paramNew, docPkListString, totalCount);
            }
        } else {
            //归档要求全部上传完成后在调回写接口
            writeBankElectRepair(paramNew, docPkListString, totalCount);
        }
    }

    /**
     * 更新银行交易回单数据
     *
     * @param paramNew
     * @param docPkListString
     * @throws Exception
     */
    public void writeBankElectRepair(CtmJSONObject paramNew, List<String> docPkListString, int totalCount) throws Exception {
        List<Long> bankElectIdes = docPkListString.stream().map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        List<Map<String, Object>> bankElectUploadFinal = getBankElectByIdes(bankElectIdes, false);
        if (!CollectionUtils.isEmpty(bankElectUploadFinal)) {
            CopyOnWriteArrayList<BankElectronicReceipt> bankElectronicSaveList = new CopyOnWriteArrayList<>();
            for (int m = 0; m < bankElectUploadFinal.size(); m++) {
                Map<String, Object> bankElectronicReceiptMap = bankElectUploadFinal.get(m);
                List<Map<String, Object>> bankReconciliationList = updateBankElectronicReceipt(bankElectronicReceiptMap);
                if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                    BankElectronicReceipt bankElectronicReceiptUpdate = new BankElectronicReceipt();
                    Map<String, Object> bankReconciliationNew = bankReconciliationList.get(0);
                    String othercheckno = null != bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO) ? bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO).toString() : null;
                    if (!StringUtils.isEmpty(othercheckno)) {
                        bankElectronicReceiptUpdate.setOther_checkno(othercheckno);
                        bankElectronicReceiptMap.put(ICmpConstant.OTHERCHECKNO, othercheckno);
                    }
//                    bankElectronicReceiptUpdate.setOther_checkno(null != bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO) ? bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO).toString() : null);
                    bankElectronicReceiptUpdate.setId(Long.parseLong(bankElectronicReceiptMap.get(ICmpConstant.ID).toString()));
                    bankElectronicReceiptUpdate.setEntityStatus(EntityStatus.Update);
                    bankElectronicSaveList.add(bankElectronicReceiptUpdate);
                }
            }
            if (!CollectionUtils.isEmpty(bankElectronicSaveList)) {
                EntityTool.setUpdateStatus(bankElectronicSaveList);
                MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicSaveList);
            }
            writeBack(bankElectUploadFinal, paramNew, totalCount, true);
        }
    }

    /**
     * 采集任务对外接口  余额调节表
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> balanceAdjustResultFile(CtmJSONObject param) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        //请求参数校验
        checkParams(param);
        //CtmJSONObject paramNew = (CtmJSONObject) GsonHelper.FromJSon(GsonHelper.ToJSon(param), CtmJSONObject.class);
        CtmJSONObject paramNew = new CtmJSONObject((Map<String, Object>) param.clone());;
        boolean queryNumFlag = (boolean) param.get(ICmpConstant.QUERY_NUM_FLAG);
        if (queryNumFlag) {
            //调用账簿获取会计主体id
            getAccountBook(paramNew);
            //获取总数
            List<Map<String, Object>> dataInfoCount = getBalanceAdjustResultInfoes(paramNew, true);
            Long count = (Long) dataInfoCount.get(0).get("count");
            CtmJSONObject doc_data = new CtmJSONObject();
            doc_data.put(ICmpConstant.TOTAL, count);
            resultMap.put(ICmpConstant.DOC_DATA, doc_data);
        }else{
            //异步处理
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                //异步处理具体逻辑
                asyncbalanceAdjustResultFileProcess(paramNew);
            });
        }
        resultMap.put(ICmpConstant.CODE, "0000");
        resultMap.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        return resultMap;
    }

    /**
     * 异步处理具体逻辑
     *
     * @param paramNew
     */
    private void asyncbalanceAdjustResultFileProcess(CtmJSONObject paramNew) {
        try {
            //调用账簿获取会计主体id
            getAccountBook(paramNew);
            //获取总数
            List<Map<String, Object>> dataInfoCount = getBalanceAdjustResultInfoes(paramNew, true);
            Long count = (Long) dataInfoCount.get(0).get("count");
            if (count > 0) {
                paramNew.put(ICmpConstant.PAPERINDEX, 1);
                //获取100条明细
                List<Map<String, Object>> balanceAdjustResultList = getBalanceAdjustResultInfoes(paramNew, false);//100
                paramNew.put(ICmpConstant.TOTALCOUNT, count.intValue());
                if (!CollectionUtils.isEmpty(balanceAdjustResultList)) {
                    //循环上传文件
                    uploadFile(balanceAdjustResultList, paramNew);
                    //上传文件与回写归档
                    balanceAdjustResultUpload(balanceAdjustResultList, paramNew);
                } else {
                    //没用数据情况调用回写接口
                    writebalanceAdjustResultBack(null, paramNew, 0, false);
                }
            } else {
                //没用数据情况调用回写接口
                writebalanceAdjustResultBack(null, paramNew, 0, false);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            String resultMsg = e.getMessage();
            log.error("================balanceAdjustResultFile executorServicePool getThreadPoolExecutor upload:" + message, e);
            //异常回调
            writeElectronicFail(resultMsg, paramNew.getString("callbackUrl"));
        }
    }

    /**
     * 根据条件获取余额调节表数据
     *
     * @param param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getBalanceAdjustResultInfoes(CtmJSONObject param, boolean isCount) throws Exception {
        //查询数据
        QuerySchema schema = new QuerySchema();
        if (isCount) {
            schema = QuerySchema.create().addSelect("count(id)");
        } else {
            schema = QuerySchema.create().addSelect("*");
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(param.getString(ICmpConstant.ACCENTITY)));
        if (null != param && null != param.getDate(ENDDATE)) {
            conditionGroup.appendCondition(QueryCondition.name("journaldate").egt(DateUtils.dateFormat(param.getDate(STARTDATE), null)));
            conditionGroup.appendCondition(QueryCondition.name("journaldate").elt(DateUtils.dateFormat(param.getDate(ENDDATE), null)));
        } else {
            conditionGroup.appendCondition(QueryCondition.name("journaldate").eq(DateUtils.dateFormat(param.getDate(STARTDATE), null)));
        }
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.AUDIT_STATUS).eq(AuditStatus.Complete.getValue()));
        schema.addCondition(conditionGroup);
        if (isCount) {
            return MetaDaoHelper.query(BalanceAdjustResult.ENTITY_NAME, schema);
        } else {
            schema.addPager(param.getInteger(ICmpConstant.PAPERINDEX), 100);
            List<Map<String, Object>> balanceAdjustResultList = MetaDaoHelper.query(BalanceAdjustResult.ENTITY_NAME, schema);
            if (balanceAdjustResultList.size() <= 100) {
                return balanceAdjustResultList;
            } else {
                return new ArrayList<>();
            }
        }
    }

    /**
     * 循环查询数据上传
     *
     * @param paramNew
     * @return
     */
    private void uploadFile(List<Map<String, Object>> dataList, CtmJSONObject paramNew) throws Exception {
        //归档要求全部上传完成后在调回写接口
        CtmJSONObject fileServer = paramNew.getJSONObject(ICmpConstant.FILESERVER);
        for (Map<String, Object> dataUpload : dataList) {
            // 下载pdf 后调用上传接口
            CtmJSONObject param = new CtmJSONObject();
            param.put("ids", dataUpload.get("id"));
            byte[] bytes = getfilePrintByte(param);
            if (bytes != null) {
                String defaultDir = AppContext.getEnvConfig(EXPORT_EXCEL_OSS_DIR_CONFIG_KEY);
                String fileName = UUID.randomUUID() + ".pdf";
                String remotePath = getRemotePath(defaultDir, dataUpload, fileName);
                dataUpload.put(REMOTEPATH, remotePath);
                dataUpload.put(ICmpConstant.FILENAME, fileName);
                dataUpload.put(ICmpConstant.FILESIZE, bytes.length);
                InputStream inputStream = new ByteArrayInputStream(bytes);
                UploadClient uploadClient = UploadClient.getInstance(fileServer.getString("serverType"), fileServer.getString("endpoint"), fileServer.getString("accessKey"), fileServer.getString("secretKey"), fileServer.getString("bucketName"));
                uploadClient.upload(remotePath, inputStream);
            }
        }
        paramNew.put(ICmpConstant.PAPERINDEX, paramNew.getInteger(ICmpConstant.PAPERINDEX) + 1);
        //重新查询 如果还有则重新调用
        List<Map<String, Object>> UploadListsecond = getBalanceAdjustResultInfoes(paramNew, false);
        if (!CollectionUtils.isEmpty(UploadListsecond) && UploadListsecond.size() > 0) {
            uploadFile(UploadListsecond, paramNew);
        } else {
            paramNew.put(ICmpConstant.PAPERINDEX, 1);
        }
    }

    public byte[] getfilePrintByte(CtmJSONObject param) throws Exception {
        try {
            String[] ids = new String[]{param.get("ids").toString()};
            //String printCode = "u8c1695568606538498050";//余额调节表打印模板
            String printCode = null;
            //String getPrintDataUrl = "/iuap-apcom-print/u8cprint/template/getTemplateListByMetaCondition?applicationCode=" + + "&billNo=" + param.get("ids");
            String billno = "cmp_balanceadjustresult";
            printCode = getDefaultPrintCode(billno);
            if (printCode == null) {
                //找不到默认模板时，换回原来写死的模板
                printCode = "u8c1695568606538498050";//余额调节表打印模板
            }
            if (ids == null) {
                return null;
            }
            String url = AppContext.getEnvConfig("domain.iuap-apcom-print") + "/iuap-print-pdf";
            //日常环境固定
//        String url = "https://bip-daily.yonyoucloud.com/iuap-apcom-print/iuap-print-pdf";
            if (StringUtils.isEmpty(url)) {
                return null;
            }
            url += "/pdf/print";
            CtmJSONObject params = new CtmJSONObject();
            params.put("printcode", printCode);
            //params.put("classifyCode", "CM.cmp_balanceadjustresult");
//        String serverUrl = "https://u8cacc-daily.yyuap.com/ficloud/print/getdata";
            String serverUrl = "";
            //String serverUrl = "https://bip-test.yyuap.com/mdf-node/formdata/bill/getPrintData?domainKey=ctm-cmp&serviceCode=ficmp0042";
            params.put("serverUrl", serverUrl);
            params.put("tenantId", AppContext.getCurrentUser().getYTenantId());
            CtmJSONObject selectParam = new CtmJSONObject();
            selectParam.put("ids", ids);
            selectParam.put("billNo", billno);
            params.put("params", selectParam.toString());
            params.put("lang", "zh_CN");
            params.put("yht_access_token", InvocationInfoProxy.getYhtAccessToken());
            params.put("domainDataBaseByCode", "FI");
            params.put("terminalType", "3");
            //配合云打印调用他们的实例
            params.put("sendType", "7");
            params.put("cookie", "yht_access_token=" + InvocationInfoProxy.getYhtAccessToken());
            params.put("meta", "5");
            URL urla = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urla.openConnection();
            // 设置请求方法为POST
            connection.setRequestMethod("POST");
            // 设置请求头部信息
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //用户通信息，需要上下文传递
            connection.setRequestProperty("yht_access_token",InvocationInfoProxy.getYhtAccessToken());
            // 启用输出流，以便向服务器发送数据
            connection.setDoOutput(true);
            //
            String ret = null;
            StringBuilder requestBody = new StringBuilder();
            // 转换为x-www-form-urlencoded编码方式的请求体数据
            for (String key : params.keySet()) {
                if (requestBody.length() > 0) {
                    requestBody.append("&");
                }
                String value = URLEncoder.encode(params.getString(key), "UTF-8");
                requestBody.append(key).append("=").append(value);
            }
            try (OutputStream outputStream = connection.getOutputStream()){
                outputStream.write(requestBody.toString().getBytes());
                outputStream.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder responsebuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    responsebuilder.append(line);
                }
                connection.disconnect();
                if (responsebuilder == null) {
                    return null;
                }
                ret = responsebuilder.toString();
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            }
//        OutputStream outputStream = connection.getOutputStream();
//        // 转换为x-www-form-urlencoded编码方式的请求体数据
//        StringBuilder requestBody = new StringBuilder();
//        for (String key : params.keySet()) {
//            if (requestBody.length() > 0) {
//                requestBody.append("&");
//            }
//            String value = URLEncoder.encode(params.getString(key), "UTF-8");
//            requestBody.append(key).append("=").append(value);
//        }
//        outputStream.write(requestBody.toString().getBytes());
//        outputStream.flush();
//        outputStream.close();
            // 读取响应内容
//        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//        String line;
//        StringBuilder responsebuilder = new StringBuilder();
//        while ((line = reader.readLine()) != null) {
//            responsebuilder.append(line);
//        }
//        reader.close();
            // 关闭连接
//        connection.disconnect();
//        if (responsebuilder == null) {
//            return null;
//        }
//        String ret = responsebuilder.toString();


            try {
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(ret);
                // 根据key获取value
                String filePath = jsonObject.getString("data");
                URL fileurl = new URL(filePath);
                HttpURLConnection fileconnection = (HttpURLConnection) fileurl.openConnection();
                fileconnection.setRequestProperty("cookie", "yht_access_token=" + InvocationInfoProxy.getYhtAccessToken());
                fileconnection.connect();
                int responseCode = fileconnection.getResponseCode();

                if (filePath != null) {
                    try(InputStream inputStream = new BufferedInputStream(fileconnection.getInputStream());
                        ByteArrayOutputStream fileoutputStream = new ByteArrayOutputStream()) {
                        // 读取字节流数据
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileoutputStream.write(buffer, 0, bytesRead);
                        }
                        byte[] fileData = fileoutputStream.toByteArray();
                        //inputStream.close();
                        fileconnection.disconnect();
                        return fileData;

                    } catch (IOException e) {
                        throw new IOException(e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400537", "根据打印模板[printCode:%s]获取pdf文件失败，请求：[%s]返回:[%s]") /* "根据打印模板[printCode:%s]获取pdf文件失败，请求：[%s]返回:[%s]" */, printCode, requestBody, ret) ,e);
            }
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400539", "根据余额调节表生成pdf文件失败") /* "根据余额调节表生成pdf文件失败" */ + e.getMessage() ,e);
        }
        return null;
    }

    private static String getDefaultPrintCode(String billno) throws Exception {
        String printDataUrl = AppContext.getEnvConfig("domain.iuap-apcom-print") + "/u8cprint/template/getTemplateListByMetaCondition?applicationCode="
                + "CM" + "&billNo=" + billno;
        //{"templates":[{"isDefault":false,"code":"u8c1677826119000","name":"11","id":"1671825632297746433","classifyCode":"PU.st_purchaseorder"},{"isDefault":false,"code":"u8c1493796016630530051","name":"(新)采购订单-业务对象","id":"1493796016630530050","classifyCode":"PU.st_purchaseorder"}],"type":1,"status":1}
        CtmJSONObject printDataCtmJSONObject = HttpsUtils.doCommonRestRequest(printDataUrl, YmsHttpMethod.GET);
        CtmJSONArray templates = printDataCtmJSONObject.getJSONArray("templates");
        if (CollectionUtils.isNotEmpty(templates)) {
            //返回的模板顺序默认的排在第一位
            CtmJSONObject defaultTemplate = templates.getJSONObject(0);
            return defaultTemplate.getString("code");
        }
        return null;
    }

    /**
     * 上传文件与回写归档逻辑
     *
     * @param receiptList
     * @param paramNew
     * @throws Exception
     */
    private void balanceAdjustResultUpload(List<Map<String, Object>> receiptList, CtmJSONObject paramNew) throws Exception {
        //调用归档回调地址处理
        boolean isBackSuccess = writebalanceAdjustResultSuccess(receiptList, paramNew, paramNew.getInteger("totalCount"));
        if (isBackSuccess) {
            paramNew.put(ICmpConstant.PAPERINDEX, paramNew.getInteger(ICmpConstant.PAPERINDEX) + 1);
            //重新查询 如果还有则重新调用
            List<Map<String, Object>> dataListsecond = getBalanceAdjustResultInfoes(paramNew, false);
            if (!CollectionUtils.isEmpty(dataListsecond) && dataListsecond.size() > 0) {
                balanceAdjustResultUpload(dataListsecond, paramNew);
            }
        }
    }


    /**
     * 回写单据 回写电子归档
     *
     * @param dataBackFinal
     * @throws Exception
     */
    private boolean writebalanceAdjustResultSuccess(List<Map<String, Object>> dataBackFinal, CtmJSONObject param, int totalCount) throws Exception {
        //更新余额调节归档状态
        CopyOnWriteArrayList<BalanceAdjustResult> balanceAdjustResultList = new CopyOnWriteArrayList<>();
        for (int m = 0; m < dataBackFinal.size(); m++) {
            Map<String, Object> bankElectronicReceiptMap = dataBackFinal.get(m);
            BalanceAdjustResult bankElectronicReceiptUpdate = new BalanceAdjustResult();
            bankElectronicReceiptUpdate.setArchivingstatus(true);
            bankElectronicReceiptUpdate.setId(Long.parseLong(bankElectronicReceiptMap.get(ICmpConstant.ID).toString()));
            bankElectronicReceiptUpdate.setEntityStatus(EntityStatus.Update);
            balanceAdjustResultList.add(bankElectronicReceiptUpdate);
        }
        if (!CollectionUtils.isEmpty(balanceAdjustResultList)) {
            EntityTool.setUpdateStatus(balanceAdjustResultList);
            MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, balanceAdjustResultList);
        }
        //调用归档回写接口
        return writebalanceAdjustResultBack(dataBackFinal, param, totalCount, false);
    }

    /**
     * 调用归档回写接口回写数据
     *
     * @param balanceAdjustResultList
     * @param param
     * @param totalCount
     * @return
     * @throws Exception
     */
    public boolean writebalanceAdjustResultBack(List<Map<String, Object>> balanceAdjustResultList, CtmJSONObject param, int totalCount, boolean Repair) throws Exception {
        //组装回写数据
        CtmJSONObject jsonParam = balanceAdjustResultassemblyParam(balanceAdjustResultList, param, totalCount, Repair);
        CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(param.getString(ICmpConstant.CALLBACKURL), jsonParam);
        if (!(null != jsonObject && "0000".equals(jsonObject.getString(ICmpConstant.CODE)))) {
            log.error("================balanceAdjustResultListFile executorServicePool writeBack:" + jsonObject.toString());
            return false;
        }
        return true;
    }

    private CtmJSONObject balanceAdjustResultassemblyParam(List<Map<String, Object>> balanceAdjustResultList, CtmJSONObject param, int totalCount, boolean Repair) throws Exception {
        List<Map<String, Object>> docList = new ArrayList<>();
        CtmJSONObject json = new CtmJSONObject();
        json.put(ICmpConstant.TASKID, param.getString(ICmpConstant.TASKID));//任务id
        json.put(ICmpConstant.SRCSYS, "ctmcmp");//来源系统编码
        json.put("docList", docList);//档案明细列表，最大100
        json.put(ICmpConstant.TICKETINFO, "YHYETJB");//银行余额调节表（YHYETJB），银行对账单(YHDZD)
        json.put(ICmpConstant.TOTALCOUNT, totalCount);//任务下档案总数
        json.put(ICmpConstant.ORGCODE, param.getString(ICmpConstant.ACCOUNTBOOKCODE));//全宗编码
        if (!Repair) {
            json.put(ACCOUNTYEAR, param.getString(ACCOUNTYEAR));//会计年
            json.put(ACCOUNTMONTH, param.getString(ACCOUNTMONTH));//会计月
        }
        json.put(ICmpConstant.DOCTYPE, param.getString(ICmpConstant.TYPE));//档案类型（即档案目录前缀）、或单据类 型编码
        json.put(ICmpConstant.STATUS, "1");//1为成功，-1为失败
        json.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);//任务失败原因
        if (!CollectionUtils.isEmpty(balanceAdjustResultList)) {
            BigDecimal midOne = new BigDecimal(1);
            for (Map<String, Object> balanceAdjustResult : balanceAdjustResultList) {
                Map<String, Object> balanceAdjustResultInfoMap = new HashMap<>();
                Map<String, Object> balanceAdjustResultMap = new HashMap<>();
                if (null != balanceAdjustResult.get("dzdate")) {
                    balanceAdjustResultMap.put("docDate", DateUtils.dateFormat((Date) balanceAdjustResult.get("dzdate"), null));//交易日期（收付日期）
                }
                //会计主体
                if (null != balanceAdjustResult.get(ICmpConstant.ACCENTITY)) {
                    FinOrgDTO finOrgDTO = getFinOrgDTO((String) balanceAdjustResult.get(ICmpConstant.ACCENTITY));
                    if (null != finOrgDTO) {
                        balanceAdjustResultMap.put("accountingSubject", finOrgDTO.getName());//会计主体
                    }
                }
                //余额调节表：返回数据中增加银行账号/账户名称，放在摘要字段
                Object bank_account = balanceAdjustResult.get(ICmpConstant.BANK_ACCOUNT);
                Optional<EnterpriseBankAcctVO> enterpriseBankAcctVO = QueryBaseDocUtils.queryEnterpriseBankAccountVOById(bank_account);
                enterpriseBankAcctVO.ifPresent(e -> {
                    String abstracts = e.getAccount() + "/" + e.getAcctName();
                    balanceAdjustResultMap.put(ICmpConstant.ABSTRACTS, abstracts);
                });
                balanceAdjustResultMap.put(ICmpConstant.PK, String.valueOf(balanceAdjustResult.get(ICmpConstant.ID)));//id
                balanceAdjustResultMap.put("ts", balanceAdjustResult.get("pubts"));
                balanceAdjustResultMap.put(ICmpConstant.DOCTYPE, param.getString(ICmpConstant.TYPE));
                balanceAdjustResultMap.put(ICmpConstant.SRCSYS, "ctmcmp");
                balanceAdjustResultMap.put(ACCOUNTYEAR, param.getString(ACCOUNTYEAR));//会计年
                balanceAdjustResultMap.put(ACCOUNTMONTH, param.getString(ACCOUNTMONTH));//会计月
                balanceAdjustResultMap.put("fileUrl", balanceAdjustResult.get(REMOTEPATH));//文件url
                balanceAdjustResultMap.put("srcFileName", balanceAdjustResult.get(ICmpConstant.FILENAME));//文件名称
                balanceAdjustResultMap.put("fileSize", Long.valueOf((int) balanceAdjustResult.get(ICmpConstant.FILESIZE)));//文件大小
                balanceAdjustResultMap.put("title", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400531", "余额调节表") /* "余额调节表" */);
                balanceAdjustResultMap.put("orgInfo", param.getJSONObject(ORGINFO));
                balanceAdjustResultInfoMap.put("descData", balanceAdjustResultMap);
                docList.add(balanceAdjustResultInfoMap);
            }
        }
        return json;
    }

    @Override
    public Map<String, Object> electronicStatementConfirmFile(CtmJSONObject param) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        //请求参数校验
        checkParams(param);
        //CtmJSONObject paramNew = (CtmJSONObject) GsonHelper.FromJSon(GsonHelper.ToJSon(param), CtmJSONObject.class);
        CtmJSONObject paramNew = new CtmJSONObject((Map<String, Object>) param.clone());;
        boolean queryNumFlag = (boolean) param.get(ICmpConstant.QUERY_NUM_FLAG);
        if (queryNumFlag) {
            //调用账簿获取会计主体id
            getAccountBook(paramNew);
            //获取总数
            List<Map<String, Object>> bankElectronicStatementCount = getBankElectronicConfirmInfoes(paramNew, true);
            Long count = (Long) bankElectronicStatementCount.get(0).get("count");
            CtmJSONObject doc_data = new CtmJSONObject();
            doc_data.put(ICmpConstant.TOTAL, count);
            resultMap.put(ICmpConstant.DOC_DATA, doc_data);
        }else{
            //异步处理
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                //异步处理具体逻辑
                asyncProcessElecttronicStatementConfirm(paramNew);
            });
        }

        resultMap.put(ICmpConstant.CODE, "0000");
        resultMap.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        return resultMap;
    }

    /**
     * 异步处理具体逻辑
     *
     * @param paramNew
     */
    private void asyncProcessElecttronicStatementConfirm(CtmJSONObject paramNew) {
        try {
            //调用账簿获取会计主体id
            getAccountBook(paramNew);
            //获取总数
            List<Map<String, Object>> bankElectronicStatementCount = getBankElectronicConfirmInfoes(paramNew, true);
            Long count = (Long) bankElectronicStatementCount.get(0).get("count");
            if (count > 0) {
                paramNew.put(ICmpConstant.PAPERINDEX, 1);
                //获取100条明细
                List<Map<String, Object>> bankElectronicStatementList = getBankElectronicConfirmInfoes(paramNew, false);//100
//                List bankElectronicReceiptListNew = (List) GsonHelper.FromJSon(GsonHelper.ToJSon(bankElectronicReceiptList), List.class);
                paramNew.put(ICmpConstant.TOTALCOUNT, count.intValue());
                if (!CollectionUtils.isEmpty(bankElectronicStatementList)) {
                    //循环上传文件
                    uploadElectStatement(bankElectronicStatementList, paramNew);
                    List<Map<String, Object>> bankElectronicStatementListNew = getBankElectronicConfirmInfoes(paramNew, false);//100
                    //上传文件与回写归档
                    bankElectronicConfirmUpload(bankElectronicStatementListNew, paramNew);
                } else {
                    //没用数据情况调用回写接口
                    writeBackConfirm(null, paramNew, 0, false);
                }
            } else {
                //没用数据情况调用回写接口
                writeBackConfirm(null, paramNew, 0, false);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            String resultMsg = e.getMessage();
            log.error("================bankElectronicReceiptFile executorServicePool getThreadPoolExecutor upload:" + message, e);
            //异常回调
            writeElectronicFail(resultMsg, paramNew.getString("callbackUrl"));
        }
    }

    /**
     * 上传文件与回写归档逻辑
     *
     * @param receiptList
     * @param paramNew
     * @throws Exception
     */
    private void bankElectronicConfirmUpload(List<Map<String, Object>> receiptList, CtmJSONObject paramNew) throws Exception {
        //调用归档回调地址处理
        boolean isBackSuccess = writeElectronicConfirmSuccess(receiptList, paramNew, paramNew.getInteger("totalCount"));
        if (isBackSuccess) {
            paramNew.put(ICmpConstant.PAPERINDEX, paramNew.getInteger(ICmpConstant.PAPERINDEX) + 1);
            //重新查询 如果还有则重新调用
            List<Map<String, Object>> bankElectronicConfirmListsecond = getBankElectronicConfirmInfoes(paramNew, false);
            if (!CollectionUtils.isEmpty(bankElectronicConfirmListsecond) && bankElectronicConfirmListsecond.size() > 0) {
                bankElectronicConfirmUpload(bankElectronicConfirmListsecond, paramNew);
            }
        }
    }

    /**
     * 循环查询数据上传
     *
     * @param paramNew
     * @return
     */
    private void uploadElectStatement(List<Map<String, Object>> receiptList, CtmJSONObject paramNew) throws Exception {
        //归档要求全部上传完成后在调回写接口
        updateBankElecStatement(paramNew, receiptList);
        paramNew.put(ICmpConstant.PAPERINDEX, paramNew.getInteger(ICmpConstant.PAPERINDEX) + 1);
        //重新查询 如果还有则重新调用
        List<Map<String, Object>> bankElectUploadListsecond = getBankElectronicConfirmInfoes(paramNew, false);
        if (!CollectionUtils.isEmpty(bankElectUploadListsecond) && bankElectUploadListsecond.size() > 0) {
            uploadElectStatement(bankElectUploadListsecond, paramNew);
        } else {
            paramNew.put(ICmpConstant.PAPERINDEX, 1);
        }
    }


    /**
     * 根据条件获取电子对账单数据
     *
     * @param param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getBankElectronicConfirmInfoes(CtmJSONObject param, boolean isCount) throws Exception {
        //查询数据
        QuerySchema schema = new QuerySchema();
        if (isCount) {
            schema = QuerySchema.create().addSelect("count(id)");
        } else {
            schema = QuerySchema.create().addSelect("*");
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);

        //conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(param.getString(ICmpConstant.ACCENTITY)));
        List<String> accountByOrg = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(Arrays.asList(param.getString(ICmpConstant.ACCENTITY)));
        if (CollectionUtils.isEmpty(accountByOrg)) {
            if (isCount) {
                List<Map<String, Object>> emptyCountList = new ArrayList<>();
                HashMap<String, Object> emptyCountHashMap = new HashMap<>();
                emptyCountHashMap.put("count", 0L);
                emptyCountList.add(emptyCountHashMap);
                return emptyCountList;
            } else {
                return new ArrayList<>();
            }
        }
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.BANK_ACCOUNT).in(accountByOrg));
        if (null != param && null != param.getDate(ENDDATE)) {
            conditionGroup.appendCondition(QueryCondition.name("startdate").egt(DateUtils.dateFormat(param.getDate(STARTDATE), null)));
            conditionGroup.appendCondition(QueryCondition.name("enddate").elt(DateUtils.dateFormat(param.getDate(ENDDATE), null)));
        } else {
            conditionGroup.appendCondition(QueryCondition.name("startdate").eq(DateUtils.dateFormat(param.getDate(STARTDATE), null)));
        }
        conditionGroup.appendCondition(QueryCondition.name("statementfileid").is_not_null());
        conditionGroup.appendCondition(QueryCondition.name("statementfileid").not_eq("''"));
        schema.addCondition(conditionGroup);
        if (isCount) {
            return MetaDaoHelper.query(ElectronicStatementConfirm.ENTITY_NAME, schema);
        } else {
            schema.addPager(param.getInteger(ICmpConstant.PAPERINDEX), 100);
            List<Map<String, Object>> bankElectronicConfirmList = MetaDaoHelper.query(ElectronicStatementConfirm.ENTITY_NAME, schema);
            if (bankElectronicConfirmList.size() <= 100) {
                return bankElectronicConfirmList;
            } else {
                return new ArrayList<>();
            }
        }
    }


    /**
     * 上传后更新数据
     *
     * @param paramNew
     * @param bankElectUploadFinal
     * @throws Exception
     */
    public void updateBankElecStatement(CtmJSONObject paramNew, List<Map<String, Object>> bankElectUploadFinal) throws Exception {
        ArrayList<ElectronicStatementConfirm> bankElectNewList = new ArrayList<>();
        for (Map<String, Object> bankElectronicReceiptUpload : bankElectUploadFinal) {
            // 下载pdf 后调用上传接口
            uploadElectronicConfirm(bankElectronicReceiptUpload, paramNew.getJSONObject(ICmpConstant.FILESERVER));
            ElectronicStatementConfirm electronicStatementConfirm = new ElectronicStatementConfirm();
            electronicStatementConfirm.setId(bankElectronicReceiptUpload.get(ICmpConstant.ID));
            electronicStatementConfirm.setRemotepath(bankElectronicReceiptUpload.get(REMOTEPATH).toString());
            electronicStatementConfirm.setStatement_name(bankElectronicReceiptUpload.get(ICmpConstant.FILENAME).toString());
            electronicStatementConfirm.setFilesize((int) bankElectronicReceiptUpload.get(ICmpConstant.FILESIZE));
            electronicStatementConfirm.setArchiving_status(true);
            electronicStatementConfirm.setEntityStatus(EntityStatus.Update);
            bankElectNewList.add(electronicStatementConfirm);
        }
        EntityTool.setUpdateStatus(bankElectNewList);
        MetaDaoHelper.update(ElectronicStatementConfirm.ENTITY_NAME, bankElectNewList);
    }

    /**
     * 回写电子对账单 回写电子归档
     *
     * @param bankElectBackFinal
     * @throws Exception
     */
    private boolean writeElectronicConfirmSuccess(List<Map<String, Object>> bankElectBackFinal, CtmJSONObject param, int totalCount) throws Exception {
        //更新电子对账单数据
//        CopyOnWriteArrayList<BankElectronicReceipt> bankElectronicSaveList = new CopyOnWriteArrayList<>();
//        for (int m = 0; m < bankElectBackFinal.size(); m++) {
//            Map<String, Object> bankElectronicReceiptMap = bankElectBackFinal.get(m);
//            List<Map<String, Object>> bankReconciliationList = updateBankElectronicReceipt(bankElectronicReceiptMap);
//            if (!CollectionUtils.isEmpty(bankReconciliationList)) {
//                BankElectronicReceipt bankElectronicReceiptUpdate = new BankElectronicReceipt();
//                Map<String, Object> bankReconciliationNew = bankReconciliationList.get(0);
//                String othercheckno = null != bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO) ? bankReconciliationNew.get(ICmpConstant.OTHERCHECKNO).toString() : null;
//                if (!StringUtils.isEmpty(othercheckno)) {
//                    bankElectronicReceiptUpdate.setOther_checkno(othercheckno);
//                    bankElectronicReceiptMap.put(ICmpConstant.OTHERCHECKNO, othercheckno);
//                }
//                bankElectronicReceiptUpdate.setId(Long.parseLong(bankElectronicReceiptMap.get(ICmpConstant.ID).toString()));
//                bankElectronicReceiptUpdate.setEntityStatus(EntityStatus.Update);
//                bankElectronicSaveList.add(bankElectronicReceiptUpdate);
//            }
//        }
//        if (!CollectionUtils.isEmpty(bankElectronicSaveList)) {
//            EntityTool.setUpdateStatus(bankElectronicSaveList);
//            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicSaveList);
//        }
        //调用归档回写接口
        return writeBackConfirm(bankElectBackFinal, param, totalCount, false);
    }


    /**
     * 调用归档回写接口回写数据
     *
     * @param bankElectronicWriteList
     * @param param
     * @param totalCount
     * @return
     * @throws Exception
     */
    public boolean writeBackConfirm(List<Map<String, Object>> bankElectronicWriteList, CtmJSONObject param, int totalCount, boolean Repair) throws Exception {
        //组装回写数据
        CtmJSONObject jsonParam = assemblyParamConfirm(bankElectronicWriteList, param, totalCount, Repair);
        CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(param.getString(ICmpConstant.CALLBACKURL), jsonParam);
        if (!(null != jsonObject && "0000".equals(jsonObject.getString(ICmpConstant.CODE)))) {
            log.error("================bankElectronicReceiptFile executorServicePool writeBack:" + jsonObject.toString());
            return false;
        }
        return true;
    }

    /**
     * 封装归档请求参数
     *
     * @param bankElectronicWriteList {
     *                                "taskId":"S202202-708946-b57fbb317187",
     *                                "ticketInfo":"bankreceipt",
     *                                "docList":[
     *                                {
     *                                "bankReceiptInfo":{
     *                                "clientNo":"account/info/a.xml",
     *                                "accountingSubject":"12345",
     *                                "fileUrl":"1",
     *                                "receivePaymentTime":"12312adeexsq"
     *                                }
     *                                }
     *                                ],
     *                                "totalCount":624,
     *                                "orgCode":"org_code_0000L1C4LEJG2CLUYQ0000",
     *                                "accountYear":"2022",
     *                                "accountMonth":"10",
     *                                "docType":"bankReceipt",
     *                                "srcSys":"test",
     *                                "status":1,
     *                                "msg":"success",
     *                                }
     * @return
     */
    private CtmJSONObject assemblyParamConfirm(List<Map<String, Object>> bankElectronicWriteList, CtmJSONObject param, int totalCount, boolean Repair) throws Exception {
        List<Map<String, Object>> docList = new ArrayList<>();
        CtmJSONObject json = new CtmJSONObject();
        json.put(ICmpConstant.TASKID, param.getString(ICmpConstant.TASKID));//任务id
        json.put(ICmpConstant.SRCSYS, "ctmcmp");//来源系统编码
        json.put("docList", docList);//档案明细列表，最大100
        json.put(ICmpConstant.TICKETINFO, "YHDZD");//银行余额调节表（YHYETJB），银行对账单(YHDZD)
        json.put(ICmpConstant.TOTALCOUNT, totalCount);//任务下档案总数
        json.put(ICmpConstant.ORGCODE, param.getString(ICmpConstant.ACCOUNTBOOKCODE));//全宗编码
        if (!Repair) {
            json.put(ACCOUNTYEAR, param.getString(ACCOUNTYEAR));//会计年
            json.put(ACCOUNTMONTH, param.getString(ACCOUNTMONTH));//会计月
        }
        json.put(ICmpConstant.DOCTYPE, param.getString(ICmpConstant.TYPE));//档案类型（即档案目录前缀）、或单据类 型编码
        json.put(ICmpConstant.STATUS, "1");//1为成功，-1为失败
        json.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);//任务失败原因
        if (!CollectionUtils.isEmpty(bankElectronicWriteList)) {
            BigDecimal midOne = new BigDecimal(1);
            for (Map<String, Object> balanceAdjustResult : bankElectronicWriteList) {
                Map<String, Object> balanceAdjustResultInfoMap = new HashMap<>();
                Map<String, Object> balanceAdjustResultMap = new HashMap<>();
                if (null != balanceAdjustResult.get("dzdate")) {
                    balanceAdjustResultMap.put("docDate", DateUtils.dateFormat((Date) balanceAdjustResult.get("dzdate"), null));//交易日期（收付日期）
                }
                //对账单取结束日期
                if (null != balanceAdjustResult.get("enddate")) {
                    balanceAdjustResultMap.put("docDate", DateUtils.dateFormat((Date) balanceAdjustResult.get("enddate"), null));
                }
                //会计主体
                if (null != balanceAdjustResult.get(ICmpConstant.ACCENTITY)) {
                    FinOrgDTO finOrgDTO = getFinOrgDTO((String) balanceAdjustResult.get(ICmpConstant.ACCENTITY));
                    if (null != finOrgDTO) {
                        balanceAdjustResultMap.put("accountingSubject", finOrgDTO.getName());//会计主体
                    }
                }

                //余额调节表：返回数据中增加银行账号/账户名称，放在摘要字段
                Object bank_account = balanceAdjustResult.get(ICmpConstant.BANK_ACCOUNT);
                Optional<EnterpriseBankAcctVO> enterpriseBankAcctVO = QueryBaseDocUtils.queryEnterpriseBankAccountVOById(bank_account);
                enterpriseBankAcctVO.ifPresent(e -> {
                    String abstracts = e.getAccount() + "/" + e.getAcctName();
                    balanceAdjustResultMap.put(ICmpConstant.ABSTRACTS, abstracts);
                });
                balanceAdjustResultMap.put(ICmpConstant.PK, String.valueOf(balanceAdjustResult.get(ICmpConstant.ID)));//id
                balanceAdjustResultMap.put("ts", balanceAdjustResult.get("pubts"));
                balanceAdjustResultMap.put(ICmpConstant.DOCTYPE, param.getString(ICmpConstant.TYPE));
                balanceAdjustResultMap.put(ICmpConstant.SRCSYS, "ctmcmp");
                balanceAdjustResultMap.put(ACCOUNTYEAR, param.getString(ACCOUNTYEAR));//会计年
                balanceAdjustResultMap.put(ACCOUNTMONTH, param.getString(ACCOUNTMONTH));//会计月
                balanceAdjustResultMap.put("fileUrl", balanceAdjustResult.get(REMOTEPATH));//文件url
                balanceAdjustResultMap.put("srcFileName", balanceAdjustResult.get(ICmpConstant.FILENAME));//文件名称
                balanceAdjustResultMap.put("fileSize", Long.valueOf((int) balanceAdjustResult.get(ICmpConstant.FILESIZE)));//文件大小
                balanceAdjustResultMap.put("title", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400536", "银行对账单") /* "银行对账单" */);
                balanceAdjustResultMap.put("orgInfo", param.getJSONObject(ORGINFO));
                balanceAdjustResultInfoMap.put("descData", balanceAdjustResultMap);
                docList.add(balanceAdjustResultInfoMap);
            }
        }
        return json;
    }

}
