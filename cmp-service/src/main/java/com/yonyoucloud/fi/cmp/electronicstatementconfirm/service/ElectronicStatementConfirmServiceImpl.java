package com.yonyoucloud.fi.cmp.electronicstatementconfirm.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.poi.util.POIUtils;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptHandleDataService;
import com.yonyoucloud.fi.cmp.bankreceipt.service.TaskBankReceiptService;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.ElectronicStatementConfirm;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.oss.OSSPoolClient;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @ClassName ElectronicStatementConfirmServiceImpl
 * @Description 电子对账单服务实现类
 * @Author tongyd
 * @Date 2019/4/28 16:23
 * @Version 1.0
 **/
@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
@RequiredArgsConstructor
public class ElectronicStatementConfirmServiceImpl implements ElectronicStatementConfirmService {

    public static String SERVICE_RESP_CODE = "000000";  //服务响应码   “000000”（6个0）代表成功，如果返回“000000”，则service_status的值一定是“00”
    private static final Cache<String, EnterpriseBankAcctVO> enterpriseBankAcctVOCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    private final CtmThreadPoolExecutor ctmThreadPoolExecutor;
    private final HttpsService httpsService;
    private final OSSPoolClient ossPoolClient;// oss客户端
    private final BankConnectionAdapterContext bankConnectionAdapterContext;// 签名验签
    private final BankAccountSettingService bankAccountSettingService;
    private final BaseRefRpcService baseRefRpcService;// 基础档案查询
    private final CurrencyQueryService currencyQueryService;

    private final CooperationFileUtilService cooperationFileUtilService;
    private final EnterpriseBankQueryService enterpriseBankQueryService;
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private AtomicInteger cardinalNumber = new AtomicInteger(0);
    @Resource
    private YmsOidGenerator ymsOidGenerator;
    private final BankReceiptHandleDataService bankReceiptHandleDataService;
    private final TaskBankReceiptService taskBankReceiptService;

    @Override
    public CtmJSONObject queryElecStatement(CtmJSONObject param) throws Exception {
        List<String> bankAccountIds = (List<String>) param.get("accountId");
        EnterpriseParams params = new EnterpriseParams();
        params.setIdList(bankAccountIds);
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAcctByCondition(params);
        List<String> bankIds = new ArrayList<String>();
        StringBuilder message = new StringBuilder();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            if (!getOpenFlag(bankAccount.getId())) {
                continue;
            }
            bankIds.add(bankAccount.getId());
            for (BankAcctCurrencyVO bankAcctCurrencyVO : bankAccount.getCurrencyList()) {
                String currencyId = bankAcctCurrencyVO.getCurrency();
                CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currencyId);
                buildAndQuery(param, bankAccount, true, message, currencyTenantDTO.getCode());
                if (bankAccounts.size() > 1) {
                    message.append(",").append(message);
                }
            }
        }
        if (bankIds.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100047"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F92F2E0508000B","选择的企业银行账户没有开通银企联，无法查询电子对账单数据") /* "选择的企业银行账户没有开通银企联，无法查询电子对账单数据" */);
        }
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("nextPage", false);
        responseMsg.put("message", message);
        return responseMsg;
    }

    @Override
    public CtmJSONObject queryElecStatementUnNeedUkey(CtmJSONObject param) throws Exception {
        List<String> bankAccountIds = (List<String>) param.get("accountId");
        EnterpriseParams params = new EnterpriseParams();
        params.setIdList(bankAccountIds);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = baseRefRpcService.queryEnterpriseBankAcctByCondition(params);
        List<String> bankIds = new ArrayList<String>();
        StringBuilder message = new StringBuilder();
        queryElecStatement(enterpriseBankAcctVOs, bankIds, param, message);
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("nextPage", false);
        responseMsg.put("message", message);
        return responseMsg;
    }

    private List<String> queryElecStatement(List<EnterpriseBankAcctVO> enterpriseBankAcctVOs, List<String> bankIds,
                                    CtmJSONObject param, StringBuilder message) throws Exception {
        ArrayList<String> errorMessage = new ArrayList<>(enterpriseBankAcctVOs.size());
        List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeysByCurrency(ICmpConstant.QUERYELECSTATEMENT, enterpriseBankAcctVOs);
        log.error("queryElecStatement enterpriseBankAcctVOs.size :{}", enterpriseBankAcctVOs.size());
        CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800ED", "系统正在对此账户拉取中"));
            }
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOs) {
                if (!getOpenFlag(enterpriseBankAcctVO.getId())) {
                    continue;
                }
                bankIds.add(enterpriseBankAcctVO.getId());
                for (BankAcctCurrencyVO bankAcctCurrencyVO : enterpriseBankAcctVO.getCurrencyList()) {
                    String currencyId = bankAcctCurrencyVO.getCurrency();
                    CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currencyId);
                    //以账户+币别加锁，防止调度任务和手动拉取并发出错
                    String key = enterpriseBankAcctVO.getAccount() + currencyTenantDTO.getId();
                    log.error("queryElecStatement buildAndQueryUnNeedUkey key: {}", key);
                    try {
//                        CtmLockTool.executeInOneServiceLock(key, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
//                            if (lockstatus == LockStatus.GETLOCK_FAIL) {
//                                errorMessage.add(String.format("[%s]:系统正在对此账户拉取中", key));
//                                return;
//                            }
//                            log.error("queryElecStatement buildAndQueryUnNeedUkey key: {}, 获取锁成功", key);
                            buildAndQueryUnNeedUkey(param, enterpriseBankAcctVO, message, currencyTenantDTO.getCode());
//                            log.error("queryElecStatement buildAndQueryUnNeedUkey key: {}, 执行完成，释放锁", key);
//                        });
                    } catch (Exception e) {
                        log.error("银行电子对账单查询失败：{}", e);
                        errorMessage.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A0", "[%s]:此账户操作发生异常:[%s]") /* "[%s]:此账户操作发生异常:[%s]" */,key, e.getMessage()));
                        //throw new CtmException(e.getMessage(), e);
                    }
                }
            }
        });

        if (bankIds.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100047"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F92F2E0508000B","选择的企业银行账户没有开通银企联，无法查询电子对账单数据") /* "选择的企业银行账户没有开通银企联，无法查询电子对账单数据" */);
        }
        if (CollectionUtils.isNotEmpty(errorMessage)) {
            throw new CtmException(errorMessage.toString());
        }
        return errorMessage;
    }

    /**
     * 构建电子对账单查询参数 并进行查询
     * 增加参数isNext 判断当总条数大于50时 修改请求交易流水号
     *
     * @param param
     * @throws Exception
     */
    public void buildAndQuery(CtmJSONObject param, EnterpriseBankAcctVO bankAccount, Boolean isNext, StringBuilder message, String currency) throws Exception {
        CtmJSONObject queryMsg = buildElecStatementQueryMsg(param, bankAccount, isNext, currency);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        log.error("======电子对账单拉取请求参数=======>" + queryMsg.toString());
        CtmJSONObject result = CtmJSONObject.toJSON(HttpsUtils.doHttpsPost(ITransCodeConstant.QUERY_ELECTRONIC_STATEMENT_CONFIRM, requestData, bankConnectionAdapterContext.getChanPayUri()));
        CtmJSONObject businessLogJson = new CtmJSONObject();
        businessLogJson.put("request", requestData);
        businessLogJson.put("response", result);
        ctmcmpBusinessLogService.saveBusinessLog(businessLogJson, bankAccount.getCode(), IMsgConstant.BANK_ELEC_RECEIPT, IServicecodeConstant.CMP_ELECTRONICSTATEMENTCONFIRM, IMsgConstant.CMP_ELECTRONIC_STATEMENT_QUERY, IMsgConstant.CMP_ELECTRONIC_STATEMENT_QUERY);
        log.error("=======电子对账单拉取result======>" + CtmJSONObject.toJSONString(result));
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            String service_resp_code = responseHead.getString("service_resp_code");
            String service_resp_desc = responseHead.getString("service_resp_desc");
            if (("00").equals(serviceStatus) && SERVICE_RESP_CODE.equals(service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");

                String accEntityId = bankAccount.getOrgid();
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("accEntityId", accEntityId);
                enterpriseInfo.put("accountId", bankAccount.getId());
                enterpriseInfo.put("accountName", bankAccount.getAcctName());
//                enterpriseInfo.put("currencyId", bankAccount.get("currency"));
                enterpriseInfo.put("customNo", param.get("customNo"));

                insertElecStatementData(enterpriseInfo, responseBody);
                String nextPage = responseBody.getString("next_page");
                if ("1".equals(nextPage)) {
                    int begNum = param.getInteger("begNum");
                    param.put("begNum", begNum + responseBody.getInteger("back_num"));
                    param.put("queryExtend", responseBody.get("query_extend"));
                    param.put("nextPage", true);
                    buildAndQuery(param, bankAccount, true, message, currency);
                } else {
                    param.put("begNum", 1);
                    message.append(bankAccount.getAccount() + ":" + responseHead.getString("service_resp_desc"));
                }
            }else {
                message.append(service_resp_desc);
            }
        }
    }

    /**
     * 构建电子对账单查询参数 并进行查询
     *
     * @param param
     * @throws Exception
     */
    public void buildAndQueryUnNeedUkey(CtmJSONObject param, EnterpriseBankAcctVO bankAccount, StringBuilder message, String currency) throws Exception {
        String customNo = null;
        Map<String, Object> accountSetting = getBankAccountSetting(bankAccount.getId());
        if (accountSetting != null && accountSetting.get("customNo") != null) {
            customNo = accountSetting.get("customNo").toString();
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F92F2E0508000B", "选择的企业银行账户没有开通银企联，无法查询电子对账单数据") /* "选择的企业银行账户没有开通银企联，无法查询电子对账单数据" */);
        }
        param.put("customNo", customNo);
        CtmJSONObject queryMsg = buildElecStatementQueryMsgUnNeedUkey(param, bankAccount, currency);
        log.error("======电子对账单拉取请求参报文=======>" + queryMsg.toString());
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = CtmJSONObject.toJSON(HttpsUtils.doHttpsPost(ITransCodeConstant.QUERY_ELECTRONIC_STATEMENT_CONFIRM, requestData, bankConnectionAdapterContext.getChanPayUri()));
        CtmJSONObject businessLogJson = new CtmJSONObject();
        businessLogJson.put("request", requestData);
        businessLogJson.put("response", result);
        ctmcmpBusinessLogService.saveBusinessLog(businessLogJson, bankAccount.getCode(), IMsgConstant.BANK_ELEC_RECEIPT, IServicecodeConstant.CMP_ELECTRONICSTATEMENTCONFIRM, IMsgConstant.CMP_ELECTRONIC_STATEMENT_QUERY, IMsgConstant.CMP_ELECTRONIC_STATEMENT_QUERY);
        log.error("=======电子对账单拉取反馈报文======>" + CtmJSONObject.toJSONString(result));
        //message.append("==="+result.toString()+"===");
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            String service_resp_code = responseHead.getString("service_resp_code");
            if (("00").equals(serviceStatus) && SERVICE_RESP_CODE.equals(service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                String accEntityId = bankAccount.getOrgid();
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("accEntityId", accEntityId);
                enterpriseInfo.put("accountId", bankAccount.getId());
                enterpriseInfo.put("customNo", customNo);
                enterpriseInfo.put("accountName", bankAccount.getAcctName());
                insertElecStatementData(enterpriseInfo, responseBody);
                String nextPage = responseBody.getString("next_page");
                if ("1".equals(nextPage)) {
                    int begNum = param.getInteger("begNum");
                    param.put("begNum", begNum + responseBody.getInteger("back_num"));
                    param.put("queryExtend", responseBody.get("query_extend"));
                    param.put("nextPage", true);
                    buildAndQueryUnNeedUkey(param, bankAccount, message, currency);
                } else {
                    param.put("begNum", 1);
                    //message.append(bankAccount.getAccount() + ":" + responseHead.getString("service_resp_desc"));
                }
            }else{
                String service_resp_desc  = String.format("%s:%s-%s[%s]%s", bankAccount.getAccount(), param.get("startDate"), param.get("endDate"), service_resp_code, responseHead.getString("service_resp_desc"));
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A9", "银企联返回报错信息:") /* "银企联返回报错信息:" */ + service_resp_desc + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A8", "。请联系银企联顾问处理！") /* "。请联系银企联顾问处理！" */);
            }
        }else{
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049E", "查询银企联报错:") /* "查询银企联报错:" */ + result.getString("message"));
        }
    }


    /**
     * TODO 单笔文件下载
     * 校验勾选的数据是否已下载对账单，如果已下载对账单（即已下载对账单=“是”），则提示“对账单标识：XXXX已下载对账单
     * 去掉该校验逻辑；支持覆盖更新;根据“归档状态”字段判定，如已归档，则进行提示“对账单标识：XXXX已下载对账单并完成归档，请检查！“
     * 是否已下载对账单-statement_download
     * 归档状态-archiving_status
     * 1.点击下载文件时，先判断归档状态，未归档的全都重新发送银企联下载接口
     * 2.调用下载接口后，将“已下载对账单”置为是，上传文件到协同云，同时返回文件流
     * @param param
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject downloadElecStatementFile(CtmJSONObject param, HttpServletResponse response) throws Exception {
        try {
            CtmJSONObject result_down = new CtmJSONObject();
            CtmJSONObject param_down = new CtmJSONObject();
            List<Object> ids = param.getObject("id", List.class);
            Long id = Long.valueOf(ids.get(0).toString());
            Map<String, Object> bankelereceipt = getBankEleStatement(id);
            //// archiving_status类型做下兼容，数据库有时候返回true，有时候返回1，这里判断一下
            //Object archiving_status = bankelereceipt.get("archiving_status");
            //if (archiving_status != null && (archiving_status instanceof Boolean ? (Boolean) archiving_status : (Integer)archiving_status == 1)) {
            //    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080026", "对账单标识：") /* "对账单标识：" */ + bankelereceipt.get("statement_id") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080027", "已下载对账单并完成归档，请检查！") /* "已下载对账单并完成归档，请检查！" */);
            //}

            Short inputType = Short.valueOf(bankelereceipt.get("inputtype").toString());
            // 判断是否已下载（上传文件）statement_download 已下载文件的直接取服务器，不重新调银行
            Boolean downloadFlag = bankelereceipt.get("statement_download") instanceof Boolean ? (Boolean) bankelereceipt.get("statement_download") : (int)bankelereceipt.get("statement_download") == 1 ? true : false;
            if (downloadFlag) {
                // 已下载（导入为已上传文件）
                List<String> newReceiptList = new ArrayList<String>();
                byte[] byteContent = ossPoolClient.download((String) bankelereceipt.get("statementfileid"));
                String bill_file_content = org.apache.commons.codec.binary.Base64.encodeBase64String(byteContent);
                newReceiptList.add(bill_file_content);
                result_down.put("receiptMessage",newReceiptList);
                result_down.put("isBatch",false);
                result_down.put("filename", bankelereceipt.get("statement_name"));
                return result_down;
            }else  if (inputType == DateOrigin.Created.getValue()){
                // 手工导入数据，看是否已上传对账单文件，已上传直接返回，未上传抛错：对账单编号【XXX】、【YYY】未导入电子对账单文件，请先导入！，ElectronicStatementConfirm
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A3", "对账单编号【%s】未导入电子对账单文件，请先导入！") /* "对账单编号【%s】未导入电子对账单文件，请先导入！" */, bankelereceipt.get("statementno")));
            }

            // 未归档的，直接重新下载，没有缓存一说
            param.put("customNo", bankelereceipt.get("custno"));
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceipt.get("bankaccount").toString());
            param.put("startDate", bankelereceipt.get("tranDate"));
            param.put("endDate", bankelereceipt.get("tranDate"));
            param.put("begNum", 1);
            // 全局对账单标识,unique_no
            param_down.put("uniqueCode", bankelereceipt.get("uniqueno"));
            param_down.put("customNo", bankelereceipt.get("custno"));
            param_down.put("billNo", bankelereceipt.get("receiptno"));
            param_down.put("billExtend", bankelereceipt.get("bill_extend"));
            CtmJSONObject postMsg_down = buildElecStatementDownloadMsg(param_down, enterpriseBankAcctVO);
            log.error("==========================电子对账单下载参数=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
            CtmJSONObject postResult_down = httpsService.doHttpsPost(ITransCodeConstant.DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE, postMsg_down, bankConnectionAdapterContext.getChanPayUri(), null);
            // 业务日志拼接
            CtmJSONObject businessLogJson = new CtmJSONObject();
            businessLogJson.put("request", postMsg_down);
            businessLogJson.put("response", postResult_down);
            ctmcmpBusinessLogService.saveBusinessLog(businessLogJson, enterpriseBankAcctVO.getCode(), IMsgConstant.BANK_ELEC_RECEIPT, IServicecodeConstant.CMP_ELECTRONICSTATEMENTCONFIRM, IMsgConstant.CMP_ELECTRONIC_STATEMENT_DOWN, IMsgConstant.CMP_ELECTRONIC_STATEMENT_DOWN);
            log.error("==========================电子对账单下载=====================================>" + CtmJSONObject.toJSONString(postResult_down));
            CtmJSONObject responseHead_down = postResult_down.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus_down = responseHead_down.getString("service_status");
            String  statementno= Objects.toString(bankelereceipt.get("statementno"));
            if (!"0000".equals(postResult_down.getString("code"))) {
                result_down.put("ChanPayMsg",postResult_down.getString("message"));
                throw new CtmException(YQLUtils.getYQLErrorMsqWithBillno(responseHead_down, statementno));
                //return result_down;
            }
            if (!("00").equals(serviceStatus_down)) {
                result_down.put("ChanPayMsg",responseHead_down.getString("service_resp_code") + responseHead_down.getString("service_resp_desc"));
                throw new CtmException(YQLUtils.getYQLErrorMsqWithBillno(responseHead_down, statementno));
                //return result_down;
            }
            String service_resp_code = responseHead_down.getString("service_resp_code");
            if (!HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE,service_resp_code)) {
                //无论是否成功 都把信息提示错出来
                throw new CtmException(YQLUtils.getYQLErrorMsqWithBillno(responseHead_down, statementno));
            }
            CtmJSONObject objs = postResult_down.getJSONObject("data");
            CtmJSONObject responseBody_down = objs.getJSONObject("response_body");
            //新电子对账单文件集合
            List<String> newReceiptList = new ArrayList<String>();
            if (responseBody_down.get("file_record") != null) {
                if (responseBody_down.get("file_record") instanceof CtmJSONObject) {
                    CtmJSONObject string = (CtmJSONObject) responseBody_down.get("file_record");
                    if (string.get("statement_file_content") != null) {
                        String StatementName = (String) string.get("statement_file");
                        String bill_file_content = (String) string.get("statement_file_content");
                        byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                        String fileId = "";
                        try {
                            fileId = ossPoolClient.upload(b, StatementName);//@notranslate
                        } catch (Exception e) {//如果名称重复 重试一次
                            fileId = ossPoolClient.upload(b, StatementName);//@notranslate
                        }
                        newReceiptList.add(bill_file_content);
                        result_down.put("receiptMessage",newReceiptList);
                        result_down.put("isBatch",false);
                        ElectronicStatementConfirm electronicStatementConfirm = new ElectronicStatementConfirm();
                        electronicStatementConfirm.setId(id);
                        // 接口返回实际文件名
                        electronicStatementConfirm.setStatement_name(StatementName);
                        // 协同云返回文件key，预览用
                        electronicStatementConfirm.setStatementfileid(fileId);
                        electronicStatementConfirm.setStatement_download(true);
                        if (StringUtils.isNotBlank((String) responseBody_down.get("unique_no"))) {
                            String uniqueNo = (String) responseBody_down.get("unique_no");
                            electronicStatementConfirm.setUniqueno(uniqueNo);
                            result_down.put("filename", StatementName);
                        }
                        electronicStatementConfirm.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(ElectronicStatementConfirm.ENTITY_NAME, electronicStatementConfirm);
                        return result_down;
                    } else {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A1", "银企连返回数据为空,下载电子对账单失败。") /* "银企连返回数据为空,下载电子对账单失败。" */);
                    }
                } else {
                    // 目前银企联报文格式会进入此分支
                    ArrayList<Map> arrayList = (ArrayList) responseBody_down.get("file_record");
                    if (arrayList != null && arrayList.size() > 0) {
                        Map map = arrayList.get(0);
                        if (map.get("statement_file_content") != null) {
                            String StatementName = (String) map.get("statement_file");
                            String bill_file_content = (String) map.get("statement_file_content");
                            byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                            String fileId = "";
                            try {
                                //todo CZFW-440718
                                //【DSP支持问题】银行电子对账单，拉取对账单后，获取到的文件是pdf格式，银企联老师反馈传到BIP的格式是OFD，请老师帮忙看下什么原因
                                fileId = ossPoolClient.upload(b, StatementName);//@notranslate
                            } catch (Exception e) {//如果名称重复 重试一次
                                fileId = ossPoolClient.upload(b, StatementName);//@notranslate
                            }
                            newReceiptList.add(bill_file_content);
                            result_down.put("receiptMessage",newReceiptList);
                            result_down.put("isBatch",false);
                            ElectronicStatementConfirm electronicStatementConfirm = new ElectronicStatementConfirm();
                            electronicStatementConfirm.setId(id);
                            // 接口返回实际文件名
                            electronicStatementConfirm.setStatement_name(StatementName);
                            // 协同云返回文件key，预览用
                            electronicStatementConfirm.setStatementfileid(fileId);
                            electronicStatementConfirm.setStatement_download(true);
                            if (StringUtils.isNotBlank((String) responseBody_down.get("unique_no"))) {
                                String uniqueNo = (String) responseBody_down.get("unique_no");
                                electronicStatementConfirm.setUniqueno(uniqueNo);
                                result_down.put("filename", StatementName);
                            }
                            electronicStatementConfirm.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(ElectronicStatementConfirm.ENTITY_NAME, electronicStatementConfirm);
                        } else {
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A1", "银企连返回数据为空,下载电子对账单失败。") /* "银企连返回数据为空,下载电子对账单失败。" */);
                        }
                    }
                }
                return result_down;
            } else {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A1", "银企连返回数据为空,下载电子对账单失败。") /* "银企连返回数据为空,下载电子对账单失败。" */);
            }
        } catch (Exception e) {
            log.error("银行电子对账单下载异常",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100053"),e.getMessage(), e);
        }
    }

    @Override
    public void downloadElecStatementFileBatch(CtmJSONObject param, HttpServletResponse response) throws Exception {
        try {
            List<String> ids = param.getObject("ids", List.class);
            //校验数据量 一次不多于100条
            if (ids.size() > 100) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100054"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F0", "选择的数据量需小于") /* "选择的数据量需小于" */ + 100);
            }
            //电子对账单必有值 不需校验空
            List<Map<String, Object>> bankelestatements = getBankEleStatements(ids);
            List<Map<String, Object>> bankelereceiptNew = new ArrayList<Map<String, Object>>();//从未下载过的电子对账单
            //校验是否是同一个账户
            List<String> accounts = new ArrayList<String>();
            for (Map<String, Object> bankelereceipt : bankelestatements) {
                if (accounts.size() > 0 && !(accounts.indexOf(bankelereceipt.get("bankaccount").toString()) > -1)) {
                    throw new CtmException( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049C", "请选则相同银行账户的电子对账单。") /* "请选则相同银行账户的电子对账单。" */);
                }
                accounts.add(bankelereceipt.get("bankaccount").toString());
            }
            //电子对账单文件集合
            List<byte[]> byteList = new ArrayList<byte[]>();
            for (Map<String, Object> bankelereceipt : bankelestatements) {
                if (bankelereceipt.get("statement_name") != null && !StringUtils.isEmpty((String) bankelereceipt.get("statement_name"))) {
                    //银企下载的数据  拿byte返回前端
                    String statement_name = (String) bankelereceipt.get("statement_name");
                    if (statement_name.contains("/")) {
                        byte[] bytes = ossPoolClient.download(statement_name);
                        byteList.add(bytes);
                    } else {
                        List<String> extendssList = new ArrayList<String>();
                        extendssList.add(statement_name);
                        byte[] bytes = cooperationFileUtilService.queryBytesbyFileid(bankelereceipt.get("statement_name").toString());
                        byteList.add(bytes);
                    }
                } else {
                    bankelereceiptNew.add(bankelereceipt);
                }
            }
            ArrayList<String> errorMsgList = new ArrayList<String>();
            //新电子对账单集合
            if (bankelereceiptNew.size() > 0) {
                int bankelereceiptNewSize = bankelereceiptNew.size();
                //判断需要新下载的电子对账单数量 若大于10条 则10条一组重复向银企连请求(请求次数向上取整)
                int httpCount = bankelereceiptNew.size() / 10 + (bankelereceiptNew.size() % 10 != 0 ? 1 : 0);
                CopyOnWriteArrayList<byte[]> receiptByteList = new CopyOnWriteArrayList();
                final List<Future<?>> futures = new ArrayList<>(httpCount);

                for (int i = 0; i < httpCount; i++) {
                    final List<Map<String, Object>> executeList = bankelereceiptNew.subList(bankelereceiptNewSize / httpCount * i, bankelereceiptNewSize / httpCount * (i + 1));
                    Future<?> submit = ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                        try {
                            getElecStatementPdfForPost(executeList, receiptByteList, response);
                        } catch (Exception e) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100056"),"exception when receiptByteList", e);
                        }
                    });
                    futures.add(submit);
                }
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        String msg = "exception when wait for future obj return result";
                        log.error(msg, e);
                        //throw new CtmException(msg, e);
                        errorMsgList.add(e.getMessage());
                    }
                }
                byteList.addAll(receiptByteList);
            }
            if (byteList.size() > 0) {
                //zip压缩流引入
                ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
                String fileNameEncoded = POIUtils.encode(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049F", "银行电子对账单") /* "银行电子对账单" */, "UTF-8");
                response.setHeader("Content-disposition", "attachment; filename=" + fileNameEncoded + "." + "zip" + ";filename*=utf-8''" + fileNameEncoded + "." + "zip");
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/x-msdownload");
                for (byte[] bytes : byteList) {
                    //时间戳用于一个zip内区分多个文件
                    String dateStr = new SimpleDateFormat(DateUtils.MILLISECOND_PATTERN).format(new Date());
                    zos.putNextEntry(new ZipEntry(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049F", "银行电子对账单") /* "银行电子对账单" */ + dateStr + ".pdf"));
                    zos.write(bytes);
                    zos.closeEntry();
                }
                zos.close();
            }
            if (errorMsgList.size() > 0) {
                throw new CtmException(errorMsgList.toString());
            }
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A2", "下载失败:") /* "下载失败:" */ + e.getMessage());
            //sendError(response, e.getMessage());
        }
    }

    @Override
    public CtmJSONObject getIdByFilename(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("code", 200);
        try {
            //电子对账单文件名称
            String fileName = (String) param.get("file_name");
            //mysql数据库不区分大小写，orcale数据库区分大小写。此处不应区分大小写，所以都查下
            //把文件名分为前后两部分，后缀转为大写和小写，然后组装新的两个文件名
            String lowerFileName = CooperationFileUtilService.buildLowerSuffixFileName(fileName);
            String upperFileName = CooperationFileUtilService.buildUpperSuffixFileName(fileName);
            //根据电子对账单文件名称查询对应数据  绑定id
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("statement_name").in(Arrays.asList(lowerFileName, upperFileName, fileName)));
            queryInitDataSchema.addCondition(conditionGroup);
            List<ElectronicStatementConfirm> electronicStatementConfirmList = MetaDaoHelper.queryObject(ElectronicStatementConfirm.ENTITY_NAME, queryInitDataSchema, null);
            if (null == electronicStatementConfirmList || electronicStatementConfirmList.size() < 1) {
                responseMsg.put("message", String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004AA", "对账单文件名称【%s】，在对账单明细数据——对账单文件名中未查找到相同名称的数据，请检查！") /* "对账单文件名称【%s】，在对账单明细数据——对账单文件名中未查找到相同名称的数据，请检查！" */, fileName));
                responseMsg.put("code", 500);
                return responseMsg;
            }
            ElectronicStatementConfirm confirm = electronicStatementConfirmList.get(0);
            if (confirm.getStatement_download() != null && confirm.getStatement_download().booleanValue()) {
                responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D41CEE204200000", "选择导入的电子对账单文件已存在，检查后再导入!") /* "选择导入的电子对账单文件已存在，检查后再导入!" */);
                responseMsg.put("code", 500);
                return responseMsg;
            }
            responseMsg.put("id", confirm.getId());
        } catch (Exception e) {
            log.error("电子对账单文件查询失败：",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100057"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_1D41CDCA04480002","电子对账单文件查询失败") /* "银行交易电子对账单文件查询失败！" */);
        }
        return responseMsg;
    }

    @Override
    public CtmJSONObject receiptAssociationFileId(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("code", 200);
        try {
            //upload关联   delete取消关联
            Object type = param.get("type");
            //上传电子对账单文件的id
            String fileId = (String) param.get("file_id");
            if (type != null && "delete".equals(type)) {
                QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.addCondition(QueryCondition.name("statementfileid").eq(fileId));
                queryInitDataSchema.addCondition(conditionGroup);
                List<ElectronicStatementConfirm> electronicStatementConfirmList = MetaDaoHelper.queryObject(ElectronicStatementConfirm.ENTITY_NAME, queryInitDataSchema, null);
                if (null == electronicStatementConfirmList || electronicStatementConfirmList.size() < 1) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80002", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                //取消关联关系
                ElectronicStatementConfirm electronicStatementConfirm = new ElectronicStatementConfirm();
                electronicStatementConfirm.setId(electronicStatementConfirmList.get(0).getId());
                electronicStatementConfirm.setStatementfileid(null);
                electronicStatementConfirm.setStatement_download(false);
                electronicStatementConfirm.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ElectronicStatementConfirm.ENTITY_NAME, electronicStatementConfirm);
            } else {
                //交易电子对账单的id
                String id = param.getString("id");
                QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.addCondition(QueryCondition.name("id").eq(id));
                queryInitDataSchema.addCondition(conditionGroup);
                List<ElectronicStatementConfirm> electronicStatementConfirmList = MetaDaoHelper.queryObject(ElectronicStatementConfirm.ENTITY_NAME, queryInitDataSchema, null);
                if (null == electronicStatementConfirmList || electronicStatementConfirmList.size() < 1) {
                    responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811486A04B80002", "查询不到对应单据,请确认单据是否存在或刷新后重新操作!") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作!" */);
                    responseMsg.put("code", 500);
                    return responseMsg;
                }
                //更新电子对账单信息
                updateBankElectronicReceipt(id, fileId);
            }
        } catch (Exception e) {
            log.error("银行电子对账单关联失败：",e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A6", "银行电子对账单关联失败！") /* "银行电子对账单关联失败！" */);
        }
        return responseMsg;
    }

    /**
     * 更新银行电子对账单单statement_name
     * @param id
     * @param fileId
     * @throws Exception
     */
    private void updateBankElectronicReceipt(String id,String fileId) throws Exception {
        ElectronicStatementConfirm electronicStatementConfirm = new ElectronicStatementConfirm();
        electronicStatementConfirm.setId(Long.parseLong(id));
        electronicStatementConfirm.setStatementfileid(fileId);
        electronicStatementConfirm.setStatement_download(true);
        electronicStatementConfirm.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(electronicStatementConfirm.ENTITY_NAME, electronicStatementConfirm);
    }

    /**
     * 线程内获取电子对账单下载byte
     *
     * @param executeList
     * @param receiptByteList
     * @param response
     * @return
     * @throws Exception
     */
    private void getElecStatementPdfForPost(List<Map<String, Object>> executeList, CopyOnWriteArrayList<byte[]> receiptByteList, HttpServletResponse response) throws Exception {
        CtmJSONArray param_down = new CtmJSONArray();
        for (Map<String, Object> newReceipt : executeList) {
            CtmJSONObject down = new CtmJSONObject();
            down.put("bill_no", newReceipt.get("receiptno"));
            down.put("bill_extend", newReceipt.get("bill_extend"));
            if (StringUtils.isNotBlank((String) newReceipt.get("uniqueCode"))) {
                down.put("uniqueCode",newReceipt.get("uniqueCode"));
            }
            param_down.add(down);
        }
        Map<String, Object> bankelereceiptFirst = executeList.get(0);
        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankelereceiptFirst.get("bankaccount").toString());
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(enterpriseBankAcctVO.getId());
        CtmJSONObject postMsg_down = buildElecStatementDownloadMsgBatch(param_down, enterpriseBankAcctVO, customNo, (String) bankelereceiptFirst.get("uniqueno"));
        log.error("==========================电子对账单下载参数=====================================>" + CtmJSONObject.toJSONString(postMsg_down));
        CtmJSONObject postResult_down = httpsService.doHttpsPost(ITransCodeConstant.DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE, postMsg_down, bankConnectionAdapterContext.getChanPayUri(), null);
        // 业务日志拼接
        CtmJSONObject businessLogJson = new CtmJSONObject();
        businessLogJson.put("request", postMsg_down);
        businessLogJson.put("response", postResult_down);
        ctmcmpBusinessLogService.saveBusinessLog(businessLogJson, enterpriseBankAcctVO.getCode(), IMsgConstant.BANK_ELEC_RECEIPT, IServicecodeConstant.CMP_ELECTRONICSTATEMENTCONFIRM, IMsgConstant.CMP_ELECTRONIC_STATEMENT_DOWN, IMsgConstant.CMP_ELECTRONIC_STATEMENT_DOWN);
        log.error("==========================电子对账单下载结果=====================================>" + CtmJSONObject.toJSONString(postResult_down));
        if (!"0000".equals(postResult_down.getString("code"))) {
            sendError(response, postResult_down.getString("service_resp_desc"));
        }
        CtmJSONObject responseHead_down = postResult_down.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus_down = responseHead_down.getString("service_status");
        if (!("00").equals(serviceStatus_down)) {
            sendError(response, ResultMessage.error(responseHead_down.getString("service_resp_code") + responseHead_down.getString("service_resp_desc")));
        }
        CtmJSONObject objs = postResult_down.getJSONObject("data");
        CtmJSONObject responseBody_down = objs.getJSONObject("response_body");
        if (objs.getJSONObject("response_body") != null && responseBody_down.get("file_record") != null) {
            //由于批量下载数据 无法与原单据对应 故不写入文件路径等信息
            if (responseBody_down.get("file_record") instanceof CtmJSONObject) {
                CtmJSONObject string = (CtmJSONObject) responseBody_down.get("file_record");
                if (string.get("statement_file_content") != null) {
                    String bill_file_content = (String) string.get("statement_file_content");
                    byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(bill_file_content);
                    receiptByteList.add(b);
                }
            } else {
                ArrayList<Map> arrayList = (ArrayList) responseBody_down.get("file_record");
                for (Map map : arrayList) {
                    String statement_file_content = (String) map.get("statement_file_content");
                    byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(statement_file_content);
                    receiptByteList.add(b);
                }
            }
        } else {
            sendError(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A1", "银企连返回数据为空,下载电子对账单失败。") /* "银企连返回数据为空,下载电子对账单失败。" */);
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        String fileNameEncoded = POIUtils.encode(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049F", "银行电子对账单") /* "银行电子对账单" */, "UTF-8");
        response.setHeader("Content-disposition", "attachment; filename=" + fileNameEncoded + "error.txt" + ";filename*=utf-8''" + fileNameEncoded + "error.txt");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.getOutputStream().write(message.getBytes());
    }

    /**
     * @return void
     * @Description 插入电子对账单数据
     * @Date 2019/9/18
     * @Param [enterpriseInfo, responseBody]
     **/
    private void insertElecStatementData(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody) throws Exception {
        List<ElectronicStatementConfirm> electronicStatementConfirms = new ArrayList<>();
        //获取币种缓存数据
        String currency = currencyQueryService.getCurrencyByAccount((String) enterpriseInfo.get("accountId"));
        int backNum = responseBody.getInteger("back_num");
        if (backNum == 1) {
            CtmJSONObject detailData = responseBody.getJSONObject("record");
            analysisElecStatementData(detailData, enterpriseInfo, electronicStatementConfirms, currency);
        } else if (backNum > 1) {
            CtmJSONArray records = responseBody.getJSONArray("record");
            for (int i = 0; i < records.size(); i++) {
                CtmJSONObject detailData = records.getJSONObject(i);
                analysisElecStatementData(detailData, enterpriseInfo, electronicStatementConfirms, currency);
            }
        }
        if (electronicStatementConfirms.size() == 0) {
            return;
        }
        CmpMetaDaoHelper.insert(ElectronicStatementConfirm.ENTITY_NAME, electronicStatementConfirms);
    }

    /**
     * @return void
     * @Description 解析电子对账单数据
     * @Date 2023/8/12
     * @Param [detailData, enterpriseInfo, electronicStatementConfirms, bankRecords]
     **/
    private void analysisElecStatementData(CtmJSONObject detailData, Map<String, Object> enterpriseInfo,
                                           List<ElectronicStatementConfirm> electronicStatementConfirms,String currency) throws Exception {
        ElectronicStatementConfirm detail = new ElectronicStatementConfirm();
        detail.setId(ymsOidGenerator.nextId());
        detail.setEntityStatus(EntityStatus.Insert);
        detail.setTenant(AppContext.getTenantId());
        detail.setAccentity((String) enterpriseInfo.get("accEntityId"));
        detail.setBankaccount((String) enterpriseInfo.get("accountId"));
        detail.setBank_name((String) enterpriseInfo.get("accountName"));
        String currencyCode = detailData.getString("curr_code");
        if (StringUtils.isNotEmpty(currencyCode)) {
            currency = currencyQueryService.getCurrencyByCode(currencyCode);
            detail.setCurrency(currency);
        } else {
            detail.setCurrency((String) currency);
        }
        // 账单开始日期
        if (detailData.get("beg_date") != null && detailData.getString("beg_date").length() == 8) {
            Date begDate = DateUtils.dateParse(detailData.getString("beg_date"), DateUtils.YYYYMMDD);
            detail.setStartdate(begDate);
        }
        // 账单结束日期
        if (detailData.get("end_date") != null && detailData.getString("end_date").length() == 8) {
            Date endDate =DateUtils.dateParse(detailData.getString("end_date"), DateUtils.YYYYMMDD);
            detail.setEnddate(endDate);
        }
        // 账单生成日期
        if (detailData.get("check_date") != null && detailData.getString("check_date").length() == 8) {
            Date createDate =DateUtils.dateParse(detailData.getString("check_date"), DateUtils.YYYYMMDD);
            detail.setCreateDate(createDate);
            detail.setCreatedate(createDate);
        }
        // 已下载对账单
        detail.setStatement_download(Boolean.FALSE);
        // 对账日期（银企数据）
        if(detailData.get("statement_date") != null && detailData.getString("statement_date").length() == 8){
            Date statementDate = DateUtils.dateParse(detailData.getString("statement_date"), DateUtils.YYYYMMDD);
            detail.setReconciliation_date(statementDate);
        }
        // 对账结果
        if(StringUtils.isNotEmpty(detailData.getString("order_status"))){
            detail.setReconciliation_result(Short.valueOf(detailData.getString("order_status")));
        }
        // 银行余额
        BigDecimal bank_balance = detailData.get("bank_balance") != null ? detailData.getBigDecimal("bank_balance") : null;
        detail.setBank_balance(bank_balance);
        // 全局对账单标识
        if(StringUtils.isNotEmpty(detailData.getString("unique_no"))){
            String uniqueNo = detailData.getString("unique_no");
            detail.setUniqueno(uniqueNo);
            // 验重
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("uniqueno").eq(uniqueNo));
            queryInitDataSchema.addCondition(conditionGroup);
            List<ElectronicStatementConfirm> electronicStatementConfirmList = MetaDaoHelper.queryObject(ElectronicStatementConfirm.ENTITY_NAME, queryInitDataSchema, null);
            if (electronicStatementConfirmList != null && electronicStatementConfirmList.size() > 0) {
                // 根据uniqueno查询，如果存在，则直接跳过，不再插入数据
                return;
            }
        }
        // 银行类别，根据企业银行账户，参照查询回来
        String bankAccountId = (String) enterpriseInfo.get("accountId");
        if (StringUtils.isNotEmpty((String) enterpriseInfo.get("accountId"))) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccountId);
            detail.setBanktype(enterpriseBankAcctVO.getBank());
        }
        // 电子对账单编号
        if(StringUtils.isNotEmpty(detailData.getString("statement_no"))){
            detail.setStatementno(detailData.getString("statement_no"));
        }
        // 账单支出总金额
        BigDecimal statement_d_total_amt = detailData.get("statement_d_total_amt") != null ? detailData.getBigDecimal("statement_d_total_amt") : null;
        detail.setTotal_amount(statement_d_total_amt);
        // 账单收入总金额
        BigDecimal statement_c_total_amt = detailData.get("statement_c_total_amt") != null ? detailData.getBigDecimal("statement_c_total_amt") : null;
        detail.setTotalrecamount(statement_c_total_amt);
        // 数据来源，接口返回则赋值银企联下载类型
        detail.setInputtype(DateOrigin.DownFromYQL.getValue());
        // 数据组装完成，放入集合当中
        electronicStatementConfirms.add(detail);
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
     *@Author tongyd
     *@Description 获取企业银行账户是否开通银企联
     *@Date 2019/6/4 11:29
     *@Param [enterpriseBankAccount]
     *@Return boolean
     **/
    private boolean getOpenFlag(Object enterpriseBankAccount) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (setting != null && setting.size() > 0) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }

    /*
     * @Author liuwtr
     * @Description 获取电子对账单信息
     * @Date 2023/8/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private Map<String, Object> getBankEleStatement(Object id) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryOne(ElectronicStatementConfirm.ENTITY_NAME, schema);
    }

    /*
     * @Author yangjn
     * @Description 获取电子对账单信息
     * @Date 2028/8/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private List<Map<String, Object>> getBankEleStatements(List<String> ids) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        schema.addOrderBy(new QueryOrderby("reconciliation_date", "asc"));
        return MetaDaoHelper.query(ElectronicStatementConfirm.ENTITY_NAME, schema);
    }

    /*
     * @Author tongyd
     * @Description 获取电子对账单实体
     * @Date 2019/9/12
     * @Param [bankAccountId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private Map<String, Object> getBankAccountSetting(Object bankAccountId) throws Exception {
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
     * @Author tongyd
     * @Description 构建电子对账单报文
     * @Date 2023/8/12
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildElecStatementQueryMsg(CtmJSONObject params, EnterpriseBankAcctVO bankAccount, Boolean isNext, String currency) throws Exception {
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(bankAccount.getId());
        String requestseqno = null;
        if (params.getString("requestseqno") != null) {
            requestseqno = buildRequestSeqNo(customNo);
            if (isNext) {//为true说明电子对账单大于50条 需要重复拉取 这时重新构建交易号
                requestseqno = buildRequestSeqNo(customNo);
            }
        } else {
            requestseqno = buildTranSeqNo(customNo);
        }
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.QUERY_ELECTRONIC_STATEMENT_CONFIRM,
                null,
                customNo,
                requestseqno,
                null);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_name", bankAccount.getAcctName());
        requestBody.put("acct_no", bankAccount.getAccount());
        // 币种，新增接口必输项
        requestBody.put("curr_code", currency);
        String startDate = params.getString("startDate").replaceAll("-", "").substring(0, 8);
        String endDate = params.getString("endDate").replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("beg_num", params.get("begNum"));
        requestBody.put("query_num", 100);
        requestBody.put("query_extend", params.get("queryExtend"));
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author yangjn
     * @Description 在无ukey情况下 构建电子对账单报文
     * @Date 2028/8/12
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildElecStatementQueryMsgUnNeedUkey(CtmJSONObject params, EnterpriseBankAcctVO bankAccount, String currency) throws Exception {
        String requestseqno = buildRequestSeqNo(params.get("customNo").toString());
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.QUERY_ELECTRONIC_STATEMENT_CONFIRM,
                null,
                params.get("customNo").toString(),
                requestseqno,
                null);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_name", bankAccount.getAcctName());
        requestBody.put("acct_no", bankAccount.getAccount());
        // 币种，新增接口必输项
        requestBody.put("curr_code", currency);
        String startDate = params.getString("startDate").replaceAll("-", "").substring(0, 8);
        String endDate = params.getString("endDate").replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", params.get("begNum"));
        requestBody.put("query_num", 100);
        requestBody.put("query_extend", params.get("queryExtend"));
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    private CtmJSONObject buildRequloadestHead(String transCode, String oper, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
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
     * @Description 构建查电子对账单下载报文
     * @Date 2019/8/7
     * @Param [params, bankAccount]
     **/
    private CtmJSONObject buildElecStatementDownloadMsg(CtmJSONObject params, EnterpriseBankAcctVO enterpriseBankAcctVO) throws Exception {
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(enterpriseBankAcctVO.getId());
        String requestseqno = buildRequestSeqNo(customNo);
        Map<String, Object> requestHead = buildRequloadestHead(ITransCodeConstant.DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE,
                params.getString("operator"),
                customNo,
                requestseqno,
                params.getString("signature"));
        List<Map<String, String>> record = new ArrayList<Map<String, String>>();
        Map<String, String> record1 = new HashMap<String, String>();
        Map<String, Object> request_body = new HashMap<String, Object>();
        request_body.put("acct_name", enterpriseBankAcctVO.getAcctName());
        request_body.put("acct_no", enterpriseBankAcctVO.getAccount());
        // 全局对账单标识，如果43T26的“unique_no”有传值，则必填
        if (StringUtils.isNotBlank(params.getString("uniqueCode"))) {
            request_body.put("unique_no", params.getString("uniqueCode"));
        }
        record1.put("bill_no", (String) params.get("billNo"));
        record1.put("bill_extend", (String) params.get("billExtend"));
        record.add(record1);
        request_body.put("record", record);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", request_body);
        return queryMsg;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author yangjn
     * @Description 构建查电子对账单批量下载报文
     * @Date 2023/8/12
     * @Param [params, bankAccount, customNo]
     **/
    private CtmJSONObject buildElecStatementDownloadMsgBatch(CtmJSONArray params, EnterpriseBankAcctVO enterpriseBankAcctVO, String customNo, String uniqueNo) throws Exception {
        String requestseqno = buildRequestSeqNo(customNo);
        Map<String, Object> requestHead = buildRequloadestHead(ITransCodeConstant.DOWNLOAD_ELECTRONIC_STATEMENT_CONFIRM_FILE,
                null,
                customNo,
                requestseqno,
                null);
        List<Map<String, String>> record = new ArrayList<Map<String, String>>();
        Map<String, Object> request_body = new HashMap<String, Object>();
        request_body.put("acct_name", enterpriseBankAcctVO.getAcctName());
        request_body.put("acct_no", enterpriseBankAcctVO.getAccount());
        // 全局对账单标志
        if (StringUtils.isNotBlank(uniqueNo)) {
            request_body.put("unique_no", uniqueNo);
        }
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject param = params.getJSONObject(i);
            Map<String, String> recordOne = new HashMap<String, String>();
            recordOne.put("bill_no", (String) param.get("bill_no"));
            recordOne.put("bill_extend", (String) param.get("bill_extend"));
            record.add(recordOne);
        }
        request_body.put("record", record);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", request_body);
        return queryMsg;
    }

    //@Override
    public CtmJSONObject receiptPreviewFile(CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("code", 200);
        try {
            String id = param.getString("id");
            QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.addCondition(QueryCondition.name("id").eq(id));
            queryInitDataSchema.addCondition(conditionGroup);
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
            if(StringUtils.isNotEmpty(bankvo.getStatementfileid())){
                if (bankvo.getStatementfileid().contains("/")) {
                    byte[] bytes = ossPoolClient.download(bankvo.getStatementfileid());
                    if (ArrayUtils.isEmpty(bytes)) {
                        responseMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1723790200993742866") /* "当前单据未查询到附件信息，不能预览!" */);
                        responseMsg.put("code", 500);
                    }
                    responseMsg.put("type", "bytes");
                    //pdf批量预览 转成图片格式返回给前端
                    responseMsg.put("downloadfile", bytes);
                    responseMsg.put("previewfile", PdfToImageUtils.pdfToImage(bytes));
                    //String url = ossPoolClient.getFullUrl(bankvo.getExtendss());
                    String url = cooperationFileUtilService.getFileBucketUrl(bankReceiptHandleDataService.handleExtendss(bankvo.getStatementfileid(), bankvo.getId()));
                    responseMsg.put("fullUrl", url);
                }else{
                    List<String> fileIds = new ArrayList<>();
                    fileIds.add(bankvo.getStatementfileid());
                    String url = cooperationFileUtilService.queryDownloadUrl(fileIds);
                    if (StringUtils.isBlank(url)) {
                        responseMsg.put("message", MessageUtils.getMessage("P_YS_CTM_CM-BE_1723790200993742866") /* "当前单据未查询到附件信息，不能预览!" */);
                        responseMsg.put("code", 500);
                    }
                    responseMsg.put("type", "bytes");
                    String suffix = "";
                    if (bankvo.getStatement_name() != null) {
                        suffix = bankvo.getStatement_name().substring(bankvo.getStatement_name().lastIndexOf("."));
                    } else {
                        suffix = ".pdf";
                    }
                    //pdf批量预览 转成图片格式返回给前端
                    if (".pdf".equalsIgnoreCase(suffix)) {
                        String innerUrl = cooperationFileUtilService.queryInnerDownloadUrl(fileIds);
                        byte[] bytes = cooperationFileUtilService.queryBytesbyFileidNew(innerUrl);
                        responseMsg.put("previewfile", PdfToImageUtils.pdfToImage(bytes));
                    } else {
                        //根据文件id获取预览的url
                        // String previewUrl = cooperationFileUtilService.queryPreviewUrlById(bankvo.getExtendss());
                        String previewUrl = cooperationFileUtilService.getFileBucketUrl(bankvo.getStatementfileid());
                        responseMsg.put("previewUrl", previewUrl);
                    }
                    //根据文件id获取预览的url
                    String previewUrl = cooperationFileUtilService.queryPreviewUrlById(bankvo.getStatementfileid());
                    responseMsg.put("previewUrl", previewUrl);
                    responseMsg.put("fullUrl", url);
                }
            }
        } catch (Exception e) {
            log.error("电子对账单预览打印失败：",e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A5", "电子对账单预览打印失败！") /* "电子对账单预览打印失败！" */);
        }
        return responseMsg;
    }

    /**
     * 调度任务“银行电子对账单查询”，支持按照资金组织、期间、银行类别、银行账号等维度查询，最小时间间隔分钟；调度任务预置，时间间隔15分钟.
     *   参照调度任务“账户电子对账单查询”
     *      1、开始日期、结束日期要不同时为空，要不同时有值，否则提示：“开始日期、结束日期要不同时为空，要不同时有值，请检查”
     *      2、提前天数与开始日期、结束日期互斥，有且只能一个有值；同时有值时，以日期范围为准
     *      3、开始日期、结束日期有值时，查询相应日期范围的对账单（交易日期）
     *      4、通过其他维度（组织、币种、银行类别）获取到企业银行账户时，依据“直联渠道”查找匹配账户管理模块下该账户的直联渠道方式，满足条件的数据发送银企联进行查询；
     *      5、任务执行时，判定该租户是否启用账户管理模块，如无则默认跳过该“直联渠道”参数
     *      6、本任务只针对银企直联银行账号（”开通银企联服务“=是的账户）进行处理，内部账户（开户行=结算中心）不进行获取
     *      7、调度任务获取时，按照手工拉取的更新、判重逻辑进行处理
     *      8、调度任务获取时，调用银企联43T26接口获取数据并存放在"银行电子对账单查询"节点
     *      9、调度任务下载结果支持存储在业务日志中，便于后续排查问题
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> scheduleQueryElecStatement(CtmJSONObject param) throws Exception {
        String logId = param.getString("logId");
        try {
            //任务参数校验
            TaskUtils.dateCheck(param);
            ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = taskBankReceiptService.bankReceipt(param);
                    HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
                    param.put("begNum", 1);
                    if (querydate.isEmpty()) {
                        param.put("startDate", getFirstDayOfLastMonth());
                        param.put("endDate", getLastDayOfLastMonth());
                    } else if (querydate.containsKey(TaskUtils.TASK_NO_DATA)) {
                        TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId,
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049D", "开始日期大于当前日期") /* "开始日期大于当前日期" */,
                                TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    } else {
                        param.put("startDate", querydate.get("startdate"));
                        param.put("endDate", querydate.get("enddate"));
                    }
                    String monthsinadvance = (String) (Optional.ofNullable(param.get("monthsinadvance")).orElse(""));
                    if(StringUtils.isNotEmpty(monthsinadvance)){
                        param.put("startDate", getFirstDayOfMonthsInAdvance(param.get("startDate").toString(), Integer.parseInt(monthsinadvance)));
                        param.put("endDate", getLastDayOfMonthsInAdvance(param.get("endDate").toString(), Integer.parseInt(monthsinadvance)));
                    }
                    List<String> bankIds = new ArrayList();
                    StringBuilder message = new StringBuilder();
                    //直联账户
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = bankAccountsGroup.get("checkSuccess");
                    enterpriseBankAcctVOs = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(param,enterpriseBankAcctVOs);
                    if(CollectionUtils.isNotEmpty(enterpriseBankAcctVOs)){
                        List<String> errorMessage = queryElecStatement(enterpriseBankAcctVOs, bankIds, param, message);
                        if(CollectionUtils.isNotEmpty(errorMessage)){
                            log.error("失败数据错误信息：{}", errorMessage);
                            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId,
                                    String.format(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A4", "失败[%s]条数据。请求和返回报文请查看业务日志。错误详情：[%s]") /* "失败[%s]条数据。请求和返回报文请查看业务日志。错误详情：[%s]" */, errorMessage.size(), errorMessage)),
                                    TaskUtils.UPDATE_TASK_LOG_URL);
                        }
                    }else{
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100047"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F92F2E0508000B","选择的企业银行账户没有开通银企联，无法查询电子对账单数据") /* "选择的企业银行账户没有开通银企联，无法查询电子对账单数据" */);
                    }
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989") /* "执行成功" */ + ":" + message.toString(), TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });
        } catch (Exception e) {
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }

    /**
     * 调度任务“银行电子对账单文件下载”，支持按照资金组织、期间、银行类别、银行账号等维度查询；最小时间间隔分钟；预置调度任务，时间间隔15分钟
     *  （参考调度任务：账户电子对账单文件下载）
     *      1、开始日期、结束日期要不同时为空，要不同时有值，否则提示：“开始日期、结束日期要不同时为空，要不同时有值，请检查”
     *      2、提前天数与开始日期、结束日期互斥，有且只能一个有值；同时有值时，以日期范围为准
     *      3、开始日期、结束日期有值时，查询相应日期范围的对账单（交易日期）
     *      4、通过其他维度（组织、币种、银行类别）获取到企业银行账户时，依据“直联渠道”查找匹配账户管理模块下该账户的直联渠道方式，满足条件的数据发送银企联进行查询；
     *      5、任务执行时，判定该租户是否启用账户管理模块，如无则默认跳过该“直联渠道”参数
     *      6、本任务只针对银企直联银行账号（”开通银企联服务“=是的账户）进行查询，内部账户（开户行=结算中心）不进行获取
     *      7、调度任务获取时，按照手工拉取的更新、判重逻辑进行处理
     *      8、调度任务获取时，调用银企联43T23接口，依据查询到的对账单标识，获取数据并更新"银行电子对账单查询"节点的已下载对账单文件为“是”
     *      9、调度任务下载结果支持存储在业务日志中，便于后续排查问题
     * @throws Exception
     */
    @Override
    public Map<String, Object> scheduleStatementFileDownload(CtmJSONObject param, HttpServletResponse response) throws Exception {
        String logId = param.getString("logId");
        try {
            //任务参数校验
            TaskUtils.dateCheck(param);
            ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = taskBankReceiptService.bankReceipt(param);
                    HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
                    if (querydate.isEmpty()) {
                        param.put("startDate", getFirstDayOfLastMonth());
                        param.put("endDate", getLastDayOfLastMonth());
                    } else if (querydate.containsKey(TaskUtils.TASK_NO_DATA)) {
                        TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId,
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049D", "开始日期大于当前日期") /* "开始日期大于当前日期" */,
                                TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    } else {
                        param.put("startDate", querydate.get("startdate"));
                        param.put("endDate", querydate.get("enddate"));
                    }
                    String monthsinadvance = (String) (Optional.ofNullable(param.get("monthsinadvance")).orElse(""));
                    if(StringUtils.isNotEmpty(monthsinadvance)){
                        param.put("startDate", getFirstDayOfMonthsInAdvance(param.get("startDate").toString(), Integer.parseInt(monthsinadvance)));
                        param.put("endDate", getLastDayOfMonthsInAdvance(param.get("endDate").toString(), Integer.parseInt(monthsinadvance)));
                    }
                    //直联账户
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = bankAccountsGroup.get("checkSuccess");
                    enterpriseBankAcctVOs = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(param,enterpriseBankAcctVOs);
                    if(CollectionUtils.isNotEmpty(enterpriseBankAcctVOs)){
                        List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.STATEMENTFILEDOWMLOAD, enterpriseBankAcctVOs);
                        try {
                            List<EnterpriseBankAcctVO> finalEnterpriseBankAcctVOs = enterpriseBankAcctVOs;
                            CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                                if (lockstatus == LockStatus.GETLOCK_FAIL) {
                                    log.error(String.format("[%s]:系统正在对此账户拉取中", accountInfoLocks));
                                    return;
                                }
                                //加锁成功
                                //获取需要下载对账单的数据
                                QuerySchema schema = QuerySchema.create().addSelect("id");
                                QueryConditionGroup conditionGroup = new QueryConditionGroup();
                                conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("startdate").egt(param.get("startDate"))));
                                conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("enddate").elt(param.get("endDate"))));
                                String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
                                String currency = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
                                String[] accentityArr = null;
                                if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
                                    accentityArr = accentitys.split(";");
                                }
                                if (accentityArr != null && accentityArr.length > 0) {
                                    conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityArr));
                                }
                                if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currency)) {
                                    conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
                                }
                                Object[] accountIdArr = finalEnterpriseBankAcctVOs.stream().map(m->m.getId()).toArray();
                                conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(accountIdArr));
                                conditionGroup.appendCondition(QueryCondition.name("statement_download").eq("0"));
                                schema.addCondition(conditionGroup);
                                List<ElectronicStatementConfirm> statementList = MetaDaoHelper.queryObject(ElectronicStatementConfirm.ENTITY_NAME, schema, null);
                                if(CollectionUtils.isNotEmpty(statementList)){
                                    List<CtmJSONObject> ids = new ArrayList<>(statementList.size());
                                    for(ElectronicStatementConfirm ele : statementList){
                                        List id =  new ArrayList();
                                        id.add(ele.getId());
                                        ids.add(new CtmJSONObject(new HashMap(){{put("id", id);}}));
                                    }
                                    downloadFileByPool(ids, response, logId);
                                }else{
                                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004A7", "选择的企业没有可下载的数据") /* "选择的企业没有可下载的数据" */);
                                }
                                TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989"), TaskUtils.UPDATE_TASK_LOG_URL);
                            });
                        } catch (Exception e) {
                            log.error("银行电子对账单文件下载失败", e);
                            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId,
                                    e.getMessage(),
                                    TaskUtils.UPDATE_TASK_LOG_URL);
                        }
                    }else{
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100047"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F92F2E0508000B","选择的企业银行账户没有开通银企联，无法查询电子对账单数据") /* "选择的企业银行账户没有开通银企联，无法查询电子对账单数据" */);
                    }
                } catch (Exception e) {
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });
        } catch (Exception e) {
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        return retMap;
    }

    private void downloadFileByPool(List<CtmJSONObject> list, HttpServletResponse response, String logId) throws Exception {

        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.bankReceiptfiledownloadTask.thread.param","8,32,1000,cmp-bankReceiptfiledownloadTask-");
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
            log.error("调度任务下载银行电子对账单文件开始，共{}条数据", list.size());
            //启用线程处理 批量进行下载
            int batchcount = Integer.parseInt(AppContext.getEnvConfig("cmp.balance.check.batch","100"));
            ThreadPoolUtil.executeByBatch(executorService,list,batchcount,"调度任务下载银行电子对账单文件",(int fromIndex, int toIndex)-> {//@notranslate
                ArrayList<String> errorMsgList = new ArrayList<>();
                for (int t = fromIndex; t < toIndex; t++) {
                    log.error("调度任务下载银行电子对账单文件第{}条开始，参数：{}", t, list.get(t));
                    try {
                        downloadElecStatementFile(list.get(t), response);
                    } catch (Exception e) {
                        log.error("银行电子对账单文件下载失败", e);
                        errorMsgList.add(e.getMessage());
                        //throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100058","银行电子对账单文件拉取异常:") + e, e);
                    }
                    log.error("调度任务下载银行电子对账单文件第{}条结束", t);
                }
                if (CollectionUtils.isNotEmpty(errorMsgList)) {
                    throw new CtmException(errorMsgList.toString());
                }
                return null;
            });
        } catch (Exception e) {
            log.error("银行电子对账单文件拉取异常" + e.getMessage(), e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6A314805100058","银行电子对账单文件拉取异常:") + e, e);
        }
    }

    private String getFirstDayOfLastMonth(){
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        if(currentDate.getMonth().getValue() == 1){
            year --;
        }
        LocalDate firstDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(1), 1);
        return firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//上个月第一天
    }

    private String getLastDayOfLastMonth(){
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        if(currentDate.getMonth().getValue() == 1){
            year --;
        }
        LocalDate lastDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(1), Month.of(currentDate.getMonth().minus(1).getValue()).length(currentDate.getYear() % 4 == 0));
        return lastDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//上个月最后一天
    }

    private String getFirstDayOfMonthsInAdvance(String date, int monthsInAdvance){
        LocalDate currentDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int year = currentDate.getYear();
        if(monthsInAdvance >= currentDate.getMonth().getValue()){
            year --;
        }
        LocalDate firstDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(monthsInAdvance), 1);
        return firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//date前第monthsInAdvance月第一天
    }

    private String getLastDayOfMonthsInAdvance(String date, int monthsInAdvance){
        LocalDate currentDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int year = currentDate.getYear();
        if(monthsInAdvance >= currentDate.getMonth().getValue()){
            year --;
        }
        LocalDate lastDayOfLastMonth = LocalDate.of(year, currentDate.getMonth().minus(monthsInAdvance), Month.of(currentDate.getMonth().minus(monthsInAdvance).getValue()).length(currentDate.getYear() % 4 == 0));
        return lastDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));//date前第monthsInAdvance月最后一天
    }


}
