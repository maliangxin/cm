package com.yonyoucloud.fi.cmp.salarypay;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.hermit.crab.YmsEncryptorProvider;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BusiTypeEnum;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.OtherBankFlag;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.BillNumberEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.salarypay.util.SalaryPayUtil;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.DigitalSignatureUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.SendBizMessageUtils;
import com.yonyoucloud.fi.cmp.util.SettleCheckUtil;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.util.pay.PayUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;

/**
 * 薪资支付实现接口类
 *
 * @author majfd
 */
@Service
@Transactional
@Slf4j
public class SalaryPayServiceImpl implements SalaryPayService {

    private static final String SALARYMAPPER = "com.yonyoucloud.fi.cmp.mapper.SalarypayMapper";

    @Autowired
    private CtmSignatureService digitalSignatureService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private HttpsService httpsService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    @Autowired
    private YmsEncryptorProvider encryptor;

    @Value(value = "1")
    private volatile int cardinalNumber;

    @Autowired
    private CmCommonService commonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;
    /**
     * 网银预下单
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String internetBankPlaceOrder(CtmJSONObject param) throws Exception {
        CtmJSONArray row = param.getJSONArray("row");
        CtmJSONObject lockResult = CacheUtils.lockBill(row);
        if (!lockResult.getBoolean("dealSucceed")) {
            return ResultMessage.error(lockResult.getString("message"));
        }
        // 数据库验签
        List<Object> payBillIds = param.getObject("ids", List.class);
        try {
            //todo 确认触发器  和 多次预下单之前的会不会失效
            List<Salarypay> payBills = queryAggvoByIds(payBillIds);
            // 校验结算方式
            PayUtils.checkSettleType(payBills);
            // ts校验
            checkPubTs(payBills, row);
            // 签名校验
            verifySignature(payBills);

//            // 先将薪资支付状态改为“支付不明”，支付不明的单子可以进行支付变更，确认支付状态后重新发起
//            int count = updateForPreOrder(payBills.get(0).getId());
//            //查看是否更新成功 若成功说单据状态正确
//            if (count == 0) {
//                //若count为0 则当前单据状态有误 不能执行后续操作
//                String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。");
//                return ResultMessage.error(message);
//            }

            String requestType = BATCH_PAY_PRE_ORDER;
            if (payBills.get(0).getBusitype() != null && BusiTypeEnum.PAY_EXPENSE.getValue() == payBills.get(0).getBusitype().getValue()){
                requestType = NOWAGES_PAY_PRE_ORDER;
            }
            CtmJSONObject postMsg = buildBatchPayPreOrderMsg(param, payBills, row, requestType);
            log.error("----------薪资支付网银预下单"+requestType+"-------------postMsg参数:"+postMsg.toString());
            CtmJSONObject postResult = httpsService.doHttpsPost(requestType, postMsg,
                    bankConnectionAdapterContext.getChanPayUri(), true);
            log.error("----------薪资支付网银预下单"+requestType+"-------------postResult结果:"+postResult.toString());

            analysisBatchPayPreOrderRespData(postResult, payBills, row);

//            for (Salarypay payBill : payBills) {
//                Map<String, Object> params = new HashMap();
//                params.put("id", payBill.getId());
//                params.put("tenant_id", AppContext.getTenantId());
//                params.put("porderid", payBill.getPorderid());
//                params.put("paymessage", payBill.getPaymessage());
//                params.put("paystatus", payBill.getPaystatus().getValue());
//                int updateCount = SqlHelper.update(SALARYMAPPER + ".updateAfterPreOrder", param);
//                //查看是否更新成功 若成功说单据状态正确
//                if (updateCount == 0) {
//                    //若count为0 则当前单据状态有误 不能执行后续操作
//                    String message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。");
//                    return ResultMessage.error(message);
//                }
//            }
            //更新子表数据 salarypayBUpdateList
            Object salarypayBUpdateListObj = postMsg.get("salarypayBUpdateList");
            // 更新子表 salarypay_b

            if (salarypayBUpdateListObj instanceof List) {
//                List<List<Salarypay_b>> salarypayBUpdateList = (List<List<Salarypay_b>>) salarypayBUpdateListObj;
                List<Salarypay_b> salarypayBUpdateList =(List<Salarypay_b>) salarypayBUpdateListObj;
                // 分组处理逻辑
                int batchSize = 900;
                for (int i = 0; i < salarypayBUpdateList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, salarypayBUpdateList.size());
                    List<Salarypay_b> batchList = salarypayBUpdateList.subList(i, end);
                    try {
                        for (int j = 0; j < batchList.size(); j++) {
                            Salarypay_b salarypay_b = batchList.get(j);
                            salarypay_b.setEntityStatus(EntityStatus.Update);
                        }
                        MetaDaoHelper.update(Salarypay_b.ENTITY_NAME, batchList);
                    } catch (Exception e) {
                        log.error("薪资支付网银预下单子表分组更新失败", i + 1, end, e);
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EF2474C05700007", "薪资支付子表分组更新失败！"));
                    }
                }

            } else {
                log.error("薪资支付网银预下单子表分组更新失败");
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EF2474C05700007", "薪资支付子表分组更新失败！") /* salarypay 子表分组更新失败！" */);
            }

            // 刷新返回给前端页面的pubts，避免前端提示当前单据不是最新状态
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, param.getObject("ids", List.class), row);
            param.put("row", row);
            param.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418010F","操作成功！") /* "操作成功！" */);
            CtmJSONObject logData = new CtmJSONObject();
            logData.put(IMsgConstant.BILL_DATA,payBills.get(0));
            logData.put(IMsgConstant.BANK_PLACEORDER_REQUEST,postMsg);
            logData.put(IMsgConstant.BANK_PLACEORDER_RESPONSE,postResult);
            ctmcmpBusinessLogService.saveBusinessLog(logData, payBills.get(0).getCode(), "", IServicecodeConstant.SALARYPAY, IMsgConstant.SALARY_PAY, IMsgConstant.BANK_PREORDER);
            return ResultMessage.data(param);
        } catch (Exception e) {
            log.error("网银预下单失败!!!", e.getMessage(), e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F69A8BA04400007","网银预下单失败:") /* "" */ + e.getMessage());
        }
    }

    /**
     * 独立事务，更新薪资支付状态为“支付不明”
     * @param id
     * @return
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public int updateForPreOrder(Long id) throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("id", id);
        param.put("tenant_id", AppContext.getTenantId());
        return SqlHelper.update(SALARYMAPPER + ".updateBeforePreOrder", param);
    }

    /**
     * 预下单交易确认
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String confirmPlaceOrder(CtmJSONObject param) throws Exception {
        CtmJSONArray row = param.getJSONArray("row");
        CtmJSONObject lockResult = CacheUtils.lockBill(row);
        if (!lockResult.getBoolean("dealSucceed")) {
            return ResultMessage.error(lockResult.getString("message"));
        }
		List<Object> payBillIds = param.getObject("ids", List.class);
        List<Salarypay> payBills = queryAggvoByIds(payBillIds);
        try{
            // ts校验
			checkPubTs(payBills, row);
            // 校验结算方式
            PayUtils.checkSettleType(payBills);
			//校验账户余额
			verifyInitData(payBills);
			//数据库验签
			verifySignature(payBills);
            String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(payBills.get(0).getPayBankAccount(), param.get("customNo"));
            param.put("requestseqno", DigitalSignatureUtils.buildRequestNum(customNo));
            param.put("customNo", customNo);
            CtmJSONObject postMsg = buildTransactionConfirmMsg(param, payBills);
            //启动独立事务跟新单据
            int count = cmCommonService.updateSalaryForPayStatus(payBills.get(0));
            //查看是否更新成功 若成功说单据状态正确
            if(count == 0){
                //若count为0 则当前单据状态有误 不能执行后续操作
                String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FE","单据[%s]支付状态有误，请在下载支付状态后，重新进行操作") /* "单据[%s]支付状态有误，请在下载支付状态后，重新进行操作" */, payBills.get(0).getCode());
                return ResultMessage.error(message);
            }
            log.error("----------薪资支付网银支付"+PRE_ORDER_TRANSACTION_CONFIRM+"-------------postMsg参数:"+postMsg.toString());
            CtmJSONObject postResult = httpsService.doHttpsPost(ITransCodeConstant.PRE_ORDER_TRANSACTION_CONFIRM, postMsg,
                    bankConnectionAdapterContext.getChanPayUri(), null);
            log.error("----------薪资支付网银支付"+PRE_ORDER_TRANSACTION_CONFIRM+"-------------postResult结果:"+postResult.toString());
            analysisTransactionConfirmRespData(postResult, payBills, row);
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, param.getObject("ids", List.class), row);
            param.put("row", row);
            param.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418010F","操作成功！") /* "操作成功！" */);
            try {
                CtmJSONObject logData = new CtmJSONObject();
                logData.put(IMsgConstant.BILL_DATA,payBills.get(0));
                logData.put(IMsgConstant.CONFIRM_PLACEORDER_REQUEST,postMsg);
                logData.put(IMsgConstant.CONFIRM_PLACEORDER_RESPONSE,postResult);
                ctmcmpBusinessLogService.saveBusinessLog(logData, payBills.get(0).getCode(), "", IServicecodeConstant.SALARYPAY, IMsgConstant.SALARY_PAY, IMsgConstant.BANK_PAY);
            }catch (Exception e){
                log.error(e.getMessage());
            }
            return ResultMessage.data(param);
        }catch(Exception e){
            log.error(e.getMessage(),e);
            return ResultMessage.error(e.getMessage());
        }
	}

    /**
     * 批量支付明细状态查询
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String queryBatchDetailPayStatus(CtmJSONObject param) throws Exception {
        String requestType = BATCH_PAY_DETAIL_STATUS_QUERY;
        if (BusiTypeEnum.PAY_EXPENSE.getValue() == param.getShortValue("busiType")){
            requestType = NOWAGES_PAY_STATUS_QUERY;
        }
        CtmJSONArray row = param.getJSONArray("row");
        //加单据锁
        String lockkey = ICmpConstant.SERVICECODE_SALPAY +row.getJSONObject(0).get("id");
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(lockkey);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100448"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00185", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(row.getJSONObject(0).get("payBankAccount").toString());
//        String customNo = bankAccountSettingService.getCustomNoAndCheckUKeyByBankAccountId(param.getBoolean("isNeedCheckUkey"), row.getJSONObject(0).get("payBankAccount").toString(), param.get("customNo"));

        param.put("requestseqno", DigitalSignatureUtils.buildRequestNum(customNo));
        param.put("customNo", customNo);
        CtmJSONObject postMsg = buildPayStatusQueryMsg(param, requestType);

        log.error("----------薪资支付状态查询"+requestType+"-------------postMsg参数:"+postMsg.toString());
        CtmJSONObject postResult = httpsService.doHttpsPost(requestType, postMsg,
                bankConnectionAdapterContext.getChanPayUri(), true);
        log.error("----------薪资支付状态查询"+requestType+"-------------postResult结果:"+postResult.toString());
        if (!"0000".equals(postResult.getString("code"))) {
            return ResultMessage.error(postResult.getString("message"));
        }
        CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        if (!("00").equals(serviceStatus)) {
            return ResultMessage
                    .error(responseHead.getString("service_resp_code") + responseHead.getString("service_resp_desc"));
        }

        List<Object> payBillIds = param.getObject("ids", List.class);
        List<Salarypay> payBills = queryAggvoByIds(payBillIds);
        try {
            CtmJSONObject logData = new CtmJSONObject();
            logData.put(IMsgConstant.BILL_DATA,payBills.get(0));
            logData.put(IMsgConstant.CONFIRM_PLACEORDER_REQUEST,postMsg);
            logData.put(IMsgConstant.CONFIRM_PLACEORDER_RESPONSE,postResult);
            ctmcmpBusinessLogService.saveBusinessLog(logData, payBills.get(0).getCode(), "", IServicecodeConstant.SALARYPAY, IMsgConstant.SALARY_PAY, IMsgConstant.BANK_QUERY);
        }catch (Exception e){
            log.error(e.getMessage());
        }
        try {
            analysisPayStatusQueryRespData(postResult.getJSONObject("data").getJSONObject("response_body"), payBills, row);
        } catch (Exception e) {
            log.error("薪资支付查询支付状态异常: " + responseHead.getString("service_resp_code") + ", " + responseHead.getString("service_resp_desc"));
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101697"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "薪资支付查询支付状态异常") /* "薪资支付查询支付状态异常" */ + e.getMessage());
        } finally {
            //释放单据锁
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        param.put("message", postResult.getString("message"));
        param.put("row", row);
        param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180624","查询成功！") /* "查询成功！" */);
        return ResultMessage.data(param);
    }



    /**
     * 批量支付明细状态查询：调度任务
     * @param salarypayMap
     * @param customNo
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    public void queryBatchDetailPayStatusByauto(Map<String, Object> salarypayMap, String customNo) throws Exception {
        //加单据锁
        String lockkey = ICmpConstant.SERVICECODE_SALPAY + salarypayMap.get("id");
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(lockkey);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100448"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00185", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        String requestType = BATCH_PAY_DETAIL_STATUS_QUERY;
        //busitype 业务类型
        Integer busitype = (Integer) salarypayMap.get("busitype");
        if (BusiTypeEnum.PAY_EXPENSE.getValue() == (short) busitype.intValue()) {
            requestType = NOWAGES_PAY_STATUS_QUERY;
        }
        String requestseqno = (String) salarypayMap.get("requestseqno");
        if (!StringUtils.isEmpty(requestseqno)) {
            CtmJSONObject param = new CtmJSONObject();
            param.put("requestseqno", DigitalSignatureUtils.buildRequestNum(customNo));
            param.put("customNo", customNo);
            param.put("oldRequestSeqNo", requestseqno);
            CtmJSONObject postMsg = buildPayStatusQueryMsg(param, requestType);

            log.error("----------薪资支付状态查询" + requestType + "-------------postMsg参数:" + postMsg.toString());
            CtmJSONObject postResult = httpsService.doHttpsPost(requestType, postMsg,
                    bankConnectionAdapterContext.getChanPayUri(), null);
            log.error("----------薪资支付状态查询" + requestType + "-------------postResult结果:" + postResult.toString());
//            if (!"0000".equals(postResult.getString("code"))) {
//                continue;
//            }
            CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
//            String serviceStatus = responseHead.getString("service_status");
//            if (!("00").equals(serviceStatus)) {
//                continue;
//            }

            List<Object> payBillIds = new ArrayList<>();
            payBillIds.add(salarypayMap.get("id"));
            List<Salarypay> payBills = queryAggvoByIds(payBillIds);
            CtmJSONArray ctmJSONArray = new CtmJSONArray();
            ctmJSONArray.add(salarypayMap);
            try {
                //加判空
                if (ObjectUtils.isNotEmpty(postResult.getJSONObject("data"))) {
                      //TODO 预下单超时
//                    String batStatus = postResult.getJSONObject("data").getJSONObject("response_body").getString("bat_status");
//                    if("05".equals(batStatus)){
//                    }
                    if (ObjectUtils.isNotEmpty(postResult.getJSONObject("data").getJSONObject("response_body"))) {
                        analysisPayStatusQueryRespData(postResult.getJSONObject("data").getJSONObject("response_body"), payBills, ctmJSONArray);
                    }
                }
            } catch (Exception e) {
                log.error("薪资支付查询支付状态异常: " + responseHead.getString("service_resp_code") + ", " + responseHead.getString("service_resp_desc"));
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101698"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "薪资支付查询支付状态异常") /* "薪资支付查询支付状态异常" */ + e.getMessage());
            } finally {
                //释放单据锁
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }

        }

    }

    /**
     * 线下支付
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject offLinePay(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        List<Salarypay> updateBills = new ArrayList<>(rows.size());
        long userId = AppContext.getCurrentUser().getId();
        List<String> messages = new ArrayList<>();
        List<Object> ids = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failCount = 0;
        Date date = BillInfoUtils.getBusinessDate();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        List<YmsLock> ymsLockList = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(id.toString());
            if (null==ymsLock) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180398", "】被锁定不能线下支付") /* "】被锁定不能线下支付" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + rowData.getString("code")
                                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EA","】已锁定，请勿操作")); /* "】已锁定，请勿操作" */
                }
                failCount++;
                continue;
            }
            ymsLockList.add(ymsLock);
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, rowData.getLong("id"), 3);
            if (payBill == null) {
                failed.put(id.toString(), id.toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101699"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                failCount++;
                continue;
            }
            //校验该账户是否允许透支
            String payBankAccount = payBill.getPayBankAccount();
            BigDecimal oriSum = payBill.getOriSum();
            if(payBankAccount!=null&&oriSum!=null){
                if(!CmpWriteBankaccUtils.checkAccOverDraft(payBill.getAccentity(),payBankAccount, payBill.getCurrency())){
                    failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EA","账户余额不足") /* "账户余额不足" */);
                    if (rows.size() == 1) {
                        param.put("ymsLockList",ymsLockList);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101700"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EA","账户余额不足") /* "账户余额不足" */);
                    }
                    failCount++;
                    continue;
                }
            }
            if (rowData.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(),
                        rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EB","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101701"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180120","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                }
                failCount++;
                continue;
            }
            Short auditStatus = payBill.getShort("auditstatus");
            if (auditStatus != AuditStatus.Complete.getValue()) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000071358","】的【审核状态】不能进行线下支付操作") /* "】的【审核状态】不能进行线下支付操作" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101702"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001038101","该单据的【审核状态】不能进行线下支付操作") /* "该单据的【审核状态】不能进行线下支付操作" */);
                }
                failCount++;
                continue;
            }

//            if (date != null && payBill.getAuditDate() != null && date.compareTo(payBill.getAuditDate()) < 0) {
//                failed.put(id.toString(), id.toString());
//                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
//                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001066031","】线下支付日期小于审批日期，不能结算！") /* "】线下支付日期小于审批日期，不能结算！" */);
//                if (rows.size() == 1) {
//                    param.put("ymsLockList",ymsLockList);
//                    throw new CtmException(
//                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
//                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001066031","】线下支付日期小于审批日期，不能结算！") /* "】线下支付日期小于审批日期，不能结算！" */);
//                }
//                i++;
//                continue;
//            }
            Short payStatus = payBill.getShort("paystatus");
            if (payStatus != PayStatus.NoPay.getValue() && payStatus != PayStatus.PreFail.getValue()
                    && payStatus != PayStatus.Fail.getValue()) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180329","】的【支付状态】不能进行线下支付操作") /* "】的【支付状态】不能进行线下支付操作" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101703"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032A","该单据的【支付状态】不能进行线下支付操作") /* "该单据的【支付状态】不能进行线下支付操作" */);
                }
                failCount++;
                continue;
            }

            //校验结算日期是否已日结
            Date maxSettleDate = null;
            if(maxSettleDateMaps.containsKey(payBill.getAccentity())){
                maxSettleDate = maxSettleDateMaps.get(payBill.getAccentity());
            }else{
                maxSettleDate = settlementService.getMaxSettleDate(payBill.getAccentity());
                maxSettleDateMaps.put(payBill.getAccentity(), maxSettleDate);
            }
            if(SettleCheckUtil.checkDailySettlement(maxSettleDate, false)){
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F2","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101704"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F2","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
                }
                failCount++;
                continue;
            }

            if (payBill.getInvalidflag() != null && payBill.getInvalidflag().booleanValue()) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F5","】已作废，不能进行线下支付操作") /* "】已作废，不能进行线下支付操作" */);
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101705"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F6","该单据已作废，不能进行线下支付操作") /* "该单据已作废，不能进行线下支付操作" */);
                }
                failCount++;
                continue;
            }

            if (StringUtils.isBlank(payBill.getPayBankAccount())) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032B","】的【付款账户】为空不能进行线下支付操作") /* "】的【付款账户】为空不能进行线下支付操作" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101706"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032E","该单据的【付款账户】为空不能进行线下支付操作") /* "该单据的【付款账户】为空不能进行线下支付操作" */);
                }
                failCount++;
                continue;
            }
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setPaystatus(PayStatus.OfflinePay);

            payBill.setSettleuserId(userId);
            payBill.setSettleuser(AppContext.getCurrentUser().getName());
            if (BillInfoUtils.getBusinessDate() != null) {
                payBill.setSettledate(BillInfoUtils.getBusinessDate());
            } else {
                payBill.setSettledate(AppContext.getLoginDate());
            }
            payBill.setSettlestatus(SettleStatus.alreadySettled);
            payBill.setPubts(rowData.getDate("pubts"));
            payBill.set("_entityName", Salarypay.ENTITY_NAME);
            payBill.set("oldvoucherstatus", payBill.getVoucherstatus().getValue());
            // 凭证模板配置借贷方金额为成功总金额
            payBill.setSuccessmoney(payBill.getOriSum());
            payBill.setOlcsuccessmoney(payBill.getNatSum());
            payBill.setSuccessnum(payBill.getNumline());
            CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(payBill);
            //更新失败金额以及失败条数为null
            payBill.setFailmoney(null);
            payBill.setFailnum(null);
            payBill.setOlcfailmoney(null);
            if (!generateResult.getBoolean("dealSucceed")) {
                failed.put(id.toString(), id.toString());
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FB","发送会计平台失败：") /* "发送会计平台失败：" */
                                    /* "发送会计平台失败：" */ + generateResult.get("message"));
                }
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180332","】发送会计平台失败，线下支付失败！") /* "】发送会计平台失败，线下支付失败！" */);
                failCount++;
                continue;
            }
            if (generateResult.get("genVoucher") != null && !generateResult.getBoolean("genVoucher")) {
                payBill.setVoucherstatus(VoucherStatus.NONCreate);
            }
            journalService.updateJournal(payBill);
            rowData.put("paystatus", PayStatus.OfflinePay.getValue());
            rowData.put("settlestatus", SettleStatus.alreadySettled.getValue());
            if (payBill.getVoucherstatus() != null) {
                rowData.put("voucherstatus", payBill.getVoucherstatus().getValue());
            }
            updatePayStatusAfterOffline(payBill);
            updateBills.add(payBill);
            ids.add(id);
        }
        if (CollectionUtils.isNotEmpty(updateBills)) {
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.SalarypayMapper.batchUpdateSalarypay", updateBills);
        }
        if (updateBills.size() > 0) {
//			MetaDaoHelper.update(Salarypay.ENTITY_NAME, updateBills);
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, ids, rows);
            // 业务处理完成后，发送通知
            payCallback(updateBills);
        }
        //更新子表支付成功日期 如果是手工支付变更为支付成功，取业务日期
        updateSuccessPayDate(updateBills, date, false);
        SendBizMessageUtils.sendBizMessage(updateBills, "cmp_salarypay", "paysucceed");
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180602","该单据线下支付成功") /* "该单据线下支付成功" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805ED","共：") /* "共：" */);
            message.append(rows.size());
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EE","张单据；") /* "张单据；" */);
            message.append(rows.size());
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180603","张线下支付成功；") /* "张线下支付成功；" */);
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180605","0张线下支付失败！") /* "0张线下支付失败！" */);
        }
        param.put("message", message);
        param.put("rows", rows);
        param.put("msgs", messages);
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failCount);
        param.put("failCount", failCount);
        if (failed.size() > 0) {
            param.put("failed", failed);
        }
        for (Salarypay updateBill : updateBills) {
            ctmcmpBusinessLogService.saveBusinessLog(updateBill, updateBill.getCode(), "", IServicecodeConstant.SALARYPAY, IMsgConstant.SALARY_PAY, IMsgConstant.OFFLINE_PAY);
        }
        param.put("ymsLockList",ymsLockList);
        return param;
    }

    public void updateSuccessPayDate(List<Salarypay> updateBills, Date date, boolean rollback) throws Exception {
        if (CollectionUtils.isEmpty(updateBills)) {
            return;
        }
//        List<Salarypay_b> updateList = new ArrayList<>();
        for (Salarypay salarypay : updateBills) {
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId(), 3);
            List<Salarypay_b> salarypay_bs = payBill.Salarypay_b();
            if (salarypay_bs != null && salarypay_bs.size() > 0) {
                for (Salarypay_b salarypay_b : salarypay_bs) {
                    Salarypay_b update = new Salarypay_b();
                    update.setEntityStatus(EntityStatus.Update);
                    update.setId(salarypay_b.getId());
                    update.setPaySuccessDate(date);
                    if (rollback) {
                        update.setTradestatus(PaymentStatus.NoPay);
                    }
//                    updateList.add(update);
                    MetaDaoHelper.update(Salarypay_b.ENTITY_NAME, update);
                }
            }
            if (rollback && cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
                cmpBudgetSalarypayManagerService.executeAuditDeleteReBudget(payBill);
                Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId(), 1);
                salarypay.setIsOccupyBudget(newBill.getIsOccupyBudget());
                salarypay.setPubts(newBill.getPubts());
            }
        }

    }

    /**
     * 取消线下支付
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject cancelOffLinePay(CtmJSONObject param) throws Exception {
        List<String> messages = new ArrayList<>();
        List<Object> ids = new ArrayList<>();
        CtmJSONArray rows = param.getJSONArray("rows");
        List<Salarypay> updateBills = new ArrayList<>(rows.size());
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        int failedCount = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<YmsLock> ymsLockList = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, rowData.getLong("id"), 3);
            Long id = rowData.getLong("id");
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(id.toString());
            if (null == ymsLock) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A2","】被锁定不能取消线下支付") /* "】被锁定不能取消线下支付" */);
                if (rows.size() == 1) {
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                            /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A2","】被锁定不能取消线下支付") /* "】被锁定不能取消线下支付" */);
                }
                failedCount++;
                continue;
            }
            ymsLockList.add(ymsLock);
            if (payBill == null) {
                failed.put(id.toString(), id.toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101699"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                failedCount++;
                continue;
            }
            //取消线下支付不进行pubts校验，因为支付成功后生成凭证，会更新业务单据凭证状态和pubts，导致pubts不一致
//			if (rowData.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
//				failed.put(id.toString(), id.toString());
//				messages.add("数据无效，请刷新后重试");
//				if (rows.size() == 1) {
//					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101707"),com.yonyou.iuap.ucf.common.i18n.MessageUtils);
//							.getMessage("P_YS_FI_CM_0000153380") /* "数据无效，请刷新后重试" */);
//				}
//				failedCount++;
//				continue;
//			}
            if (payBill.getPaystatus().getValue() != PayStatus.OfflinePay.getValue()) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180328","】的【支付状态】不能进行取消线下支付操作") /* "】的【支付状态】不能进行取消线下支付操作" */);
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                            /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180328","】的【支付状态】不能进行取消线下支付操作") /* "】的【支付状态】不能进行取消线下支付操作" */);
                }
                failedCount++;
                continue;
            }

            // 已日结后不能取消线下支付
            Date maxSettleDate = null;
            if(maxSettleDateMaps.containsKey(payBill.getAccentity())){
                maxSettleDate = maxSettleDateMaps.get(payBill.getAccentity());
            }else{
                maxSettleDate = settlementService.getMaxSettleDate(payBill.getAccentity());
                maxSettleDateMaps.put(payBill.getAccentity(), maxSettleDate);
            }
            if(SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, payBill.getSettledate())){
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032D","】已日结，不能取消线下支付！") /* "】已日结，不能取消线下支付！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101708"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180330","该单据已日结，不能取消线下支付！") /* "该单据已日结，不能取消线下支付！" */);
                }
                failedCount++;
                continue;
            }
            // 勾对完成后不能取消线下支付
            Boolean journal = journalService.checkJournal(rowData.getLong("id"));
            if (journal) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180333","】已勾对，不能取消线下支付！") /* "】已勾对，不能取消线下支付！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101709"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180335","该单据已勾对，不能取消线下支付！") /* "该单据已勾对，不能取消线下支付！" */);
                }
                failedCount++;
                continue;
            }
            // 匹配关联银行回单后不能取消线下支付
            Boolean matchjournal = journalService.matchJournal(rowData.getLong("id"));
            if (matchjournal) {
                failed.put(id.toString(), id.toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00037", "单据【%s】已匹配关联银行电子回单，不能取消线下支付！") /* "单据【%s】已匹配关联银行电子回单，不能取消线下支付！" */,payBill.getCode()));
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101710"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B9", "该单据已匹配关联银行电子回单，不能取消线下支付！") /* "该单据已匹配关联银行电子回单，不能取消线下支付！" */);
                }
                failedCount++;
                continue;
            }
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("id", payBill.getId());
            jsonObject.put("billnum", "cmp_salarypay");
            boolean checked = cmpVoucherService.isChecked(jsonObject);
            if (checked) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000BA", "单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180325","】凭证已勾对，不能取消线下支付！") /* "】凭证已勾对，不能取消线下支付！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101711"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180327","该单据凭证已勾对，不能取消线下支付！") /* "该单据凭证已勾对，不能取消线下支付！" */);
                }
                failedCount++;
                continue;
            }
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setPaystatus(PayStatus.NoPay);
            payBill.setSettleuserId(null);
            payBill.setSettleuser(null);
            payBill.setSettledate(null);
            payBill.setSettlestatus(SettleStatus.noSettlement);
            payBill.set("_entityName", Salarypay.ENTITY_NAME);
            payBill.setSuccessmoney(payBill.getOriSum());
            payBill.setSuccessnum(payBill.getNumline());
            payBill.setOlcsuccessmoney(payBill.getNatSum());
            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(payBill);
            payBill.setSuccessmoney(null);
            payBill.setOlcsuccessmoney(null);
            payBill.setSuccessnum(null);
            payBill.setVoucherNo(null);
            payBill.setVoucherId(null);
            payBill.setVoucherPeriod(null);
            if (!deleteResult.getBoolean("dealSucceed")) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000BA", "单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032C","】删除凭证失败，不能取消线下支付！") /* "】删除凭证失败，不能取消线下支付！" */);
                if (rows.size() == 1) {
                    param.put("ymsLockList",ymsLockList);
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000BB", "删除凭证失败：") /* "删除凭证失败：" */
                                    /* "删除凭证失败：" */ + deleteResult.get("message"));
                }
                failedCount++;
                continue;
            }
            // 更新日记账
            journalService.updateJournal(payBill);

            rowData.put("paystatus", PayStatus.NoPay.getValue());
            rowData.put("settlestatus", SettleStatus.noSettlement.getValue());
            if (payBill.getVoucherstatus() != null) {
                rowData.put("voucherstatus", payBill.getVoucherstatus().getValue());
            }

            updateBills.add(payBill);
            ids.add(id);
        }
        //更新子表支付成功日期 如果是手工支付变更为支付成功，取业务日期
        updateSuccessPayDate(updateBills, null, true);
        if (updateBills.size() > 0) {
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, updateBills);
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, ids, rows);
            // 业务处理完成后，发送通知
            payCallback(updateBills);
        }

        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806B8", "该单据取消线下支付成功") /* "该单据取消线下支付成功" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805ED","共：") /* "共：" */);
            message.append(rows.size());
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EE","张单据；") /* "张单据；" */);
            message.append(rows.size() - failedCount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C0","张取消线下支付成功；") /* "张取消线下支付成功；" */);
            message.append(failedCount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C2","张取消线下支付失败！") /* "张取消线下支付失败！" */);
        }
        param.put("message", message);
        param.put("msgs", messages);
        param.put("rows", rows);
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failedCount);
        param.put("failCount", failedCount);
        if (failed.size() > 0) {
            param.put("failed", failed);
        }
        for (Salarypay updateBill : updateBills) {
            ctmcmpBusinessLogService.saveBusinessLog(updateBill, updateBill.getCode(), "", IServicecodeConstant.SALARYPAY, IMsgConstant.SALARY_PAY, IMsgConstant.CANCEL_OFFLINE_PAY);
        }
        param.put("ymsLockList",ymsLockList);
        return param;
    }

    /**
     * 获取银企联渠道号
     *
     * @return
     */
    @Override
    public String getChanPayChanelNo() {
        return bankConnectionAdapterContext.getChanPayCustomChanel();
    }

    @Override
    public CtmJSONObject audit(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        List<String> messages = new ArrayList<>();
        List<Salarypay> salarypays = new ArrayList<>(rows.size());
        List<Object> ids = new ArrayList<>();
        int failedCount = 0;
        CtmJSONObject failed = new CtmJSONObject();
        Date date = BillInfoUtils.getBusinessDate();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, rowData.getLong("id"));
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039E","】被锁定不能审核") /* "】被锁定不能审核" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039E","】被锁定不能审核") /* "】被锁定不能审核" */);
                }
                failedCount++;
                continue;
            }
            if (payBill == null) {
                failed.put(id.toString(), id.toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101699"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                failedCount++;
                continue;
            }
            if (rowData.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EB","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101712"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180120","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                }
                failedCount++;
                continue;
            }
            PayStatus payStatus = payBill.getPaystatus();
            if (date != null && payBill.getVouchdate() != null && date.compareTo(payBill.getVouchdate()) < 0) {
                failed.put(payBill.getId().toString(), payBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A1","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A1","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */);
                }
                i++;
                continue;
            }

            if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.Fail
                    && payStatus != PayStatus.PreFail) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001038117","】支付状态不能进行审批！") /* "】支付状态不能进行审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101713"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062A","该单据支付状态不能进行审批！") /* "该单据支付状态不能进行审批！" */);
                }
                failedCount++;
                continue;
            }
            AuditStatus auditStatus = payBill.getAuditstatus();
            if (auditStatus != null && auditStatus == AuditStatus.Complete) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AD","】已审批，不能进行重复审批！") /* "】已审批，不能进行重复审批！" */);
                failed.put(id.toString(), id.toString());
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101714"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418062C","该单据已审批，不能进行重复审批！") /* "该单据已审批，不能进行重复审批！" */);
                }
                failedCount++;
                continue;
            }
            if (StringUtils.isBlank(payBill.getPayBankAccount())) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E6","该单据的付款账户为空不能进行审核操作") /* "该单据的付款账户为空不能进行审核操作" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101715"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E6","该单据的付款账户为空不能进行审核操作") /* "该单据的付款账户为空不能进行审核操作" */);
                }
                failedCount++;
                continue;
            }
            if (payBill.getSettlemode() == null) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E9","该单据的结算方式为空不能进行审核操作") /* "该单据的结算方式为空不能进行审核操作" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101716"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E9","该单据的结算方式为空不能进行审核操作") /* "该单据的结算方式为空不能进行审核操作" */);
                }
                failedCount++;
                continue;
            }
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setAuditstatus(AuditStatus.Complete);
            payBill.setAuditorId(AppContext.getCurrentUser().getId());
            payBill.setAuditor(AppContext.getCurrentUser().getName());
            payBill.setAuditTime(new Date());
            payBill.setAuditDate(BillInfoUtils.getBusinessDate());
            payBill.setPaystatus(PayStatus.NoPay);
            rowData.put("auditstatus", AuditStatus.Complete.getValue());
            rowData.put("paystatus", PayStatus.NoPay.getValue());
            salarypays.add(payBill);
            ids.add(id);
        }
        if (salarypays.size() > 0) {
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, salarypays);
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, ids, rows);
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EC","审批成功!") /* "审批成功!" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805ED","共：") /* "共：" */);
            message.append(rows.size());
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EE","张单据；") /* "张单据；" */);
            message.append(rows.size() - failedCount);
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EF","张审批通过；") /* "张审批通过；" */);
            message.append(failedCount);
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F0","张审批未通过！") /* "张审批未通过！" */);
        }
        param.put("message", message);
        param.put("rows", rows);
        param.put("msgs", messages);
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failedCount);
        param.put("failCount", failedCount);
        if (failed.size() > 0) {
            param.put("failed", failed);
        }
        return param;
    }

    @Override
    public CtmJSONObject unAudit(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        List<Salarypay> updateBills = new ArrayList<>(rows.size());
        List<Object> ids = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failcount = 0;
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A5","】被锁定不能弃审") /* "】被锁定不能弃审" */);
                if (rows.size() == 1) {

                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A5","】被锁定不能弃审") /* "】被锁定不能弃审" */);
                }
                failcount++;
                continue;
            }
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, rowData.getLong("id"));
            if (payBill == null) {
                failed.put(id.toString(), id.toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101699"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                failcount++;
                continue;
            }
            if (rowData.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EB","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101717"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180120","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                }
                failcount++;
                continue;
            }
            PayStatus payStatus = payBill.getPaystatus();
            if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.PreFail
                    && payStatus != PayStatus.Fail) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E0","】支付状态不能进行取消审批！") /* "】支付状态不能进行取消审批！" */);
                if (rows.size() == 1) {

                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E0","】支付状态不能进行取消审批！") /* "】支付状态不能进行取消审批！" */);
                }
                failcount++;
                continue;
            }
            AuditStatus auditStatus = payBill.getAuditstatus();
            if (auditStatus != null && auditStatus.getValue() == AuditStatus.Incomplete.getValue()) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180356","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
                if (rows.size() == 1) {

                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180356","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
                }
                failcount++;
                continue;
            }
            // 已日结后不能取消审批
            Boolean settlement = settlementService.checkDailySettlement(rowData.getString(IBussinessConstant.ACCENTITY),
                    rowData.getDate("vouchdate"));
            if (settlement) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FE","】已日结，不能取消审批！") /* "】已日结，不能取消审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FE","】已日结，不能取消审批！") /* "】已日结，不能取消审批！" */);

                }
                failcount++;
                continue;
            }
            // 勾对完成后不能取消审批
            Boolean check = journalService.checkJournal(rowData.getLong("id"));
            if (check) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CC","】已勾对，不能取消审批！") /* "】已勾对，不能取消审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CC","】已勾对，不能取消审批！") /* "】已勾对，不能取消审批！" */);

                }
                failcount++;
                continue;
            }
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setAuditstatus(AuditStatus.Incomplete);
            payBill.setAuditorId(null);
            payBill.setAuditor(null);
            payBill.setAuditTime(null);
            payBill.setAuditDate(null);
            payBill.setPubts(rowData.getDate("pubts"));
            rowData.put("auditstatus", AuditStatus.Incomplete.getValue());
            updateBills.add(payBill);
            ids.add(id);
        }
        if (updateBills.size() > 0) {
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, updateBills);
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, ids, rows);
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180604","取消审批成功!") /* "取消审批成功!" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805ED","共：") /* "共：" */);
            message.append(rows.size());
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EE","张单据；") /* "张单据；" */);
            message.append(rows.size());
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180606","张取消审批成功；") /* "张取消审批成功；" */);
            message.append(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180607","0张取消审批失败！") /* "0张取消审批失败！" */);
        }
        param.put("message", message);
        param.put("msgs", messages);
        param.put("rows", rows);
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failcount);
        param.put("failCount", failcount);
        if (failed.size() > 0) {
            param.put("failed", failed);
        }
        return param;
    }

    /**
     * 查询返回主子数据
     */
    @Override
    public List<Salarypay> queryAggvoByIds(List<Object> ids) throws Exception {
        List<String> queryIds = new ArrayList<>();
        for (Object id: ids){
            queryIds.add(id.toString());
        }
        List<Map<String, Object>> payBillListQuery = MetaDaoHelper.queryByIds(Salarypay.ENTITY_NAME, "*", queryIds);
        List<Map<String, Object>> payBListQuery = MetaDaoHelper.query(Salarypay_b.ENTITY_NAME,
                QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").in(queryIds)));
        Map<Long, List<Salarypay_b>> payBMapList = new HashMap<Long, List<Salarypay_b>>();
        List<Salarypay_b> pbyBList = null;
        for (Map<String, Object> map : payBListQuery) {
            Salarypay_b pbyBill_b = new Salarypay_b();
            pbyBill_b.setEntityStatus(EntityStatus.Unchanged);
            pbyBill_b.init(map);
            if (payBMapList.get(map.get("mainid")) == null) {
                pbyBList = new ArrayList<Salarypay_b>();
            } else {
                pbyBList = payBMapList.get(map.get("mainid"));
            }
            pbyBList.add(pbyBill_b);
            payBMapList.put((Long) map.get("mainid"), pbyBList);
        }
        List<Salarypay> payBillList = new ArrayList<Salarypay>();
        for (Map<String, Object> map : payBillListQuery) {
            Salarypay payBill = new Salarypay();
            payBill.init(map);
            payBillList.add(payBill);
            payBill.setSalarypay_b(payBMapList.get(payBill.getId()));
        }
        return payBillList;
    }

    /**
     * ts校验，降低连接数
     *
     * @param paybill
     * @param rows
     * @throws Exception
     */
    private void checkPubTs(List<Salarypay> paybill, CtmJSONArray rows) throws Exception {
        Map<Long, Date> pubts = new HashMap<Long, Date>();
        for (Salarypay object : paybill) {
            pubts.put(object.getId(), object.getPubts());
        }
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            if (row.getDate("pubts").compareTo(pubts.get(row.getLong("id"))) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101718"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180120","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            }
        }
    }

    /*
     * @Author tongyd
     *
     * @Description 获取收款账户信息
     *
     * @Date 2019/9/23
     *
     * @Param [rowData]
     *
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private Map<String, Object> getRecAccInfo(Object payLinenumber, Salarypay_b body, String toBankType) throws Exception {
        Map<String, Object> recAccInfo = new HashMap<>();
        if (toBankType != null) {
            recAccInfo.put("to_bank_type", toBankType);
        } else {
            if (payLinenumber != null && body.getCrtcombine() != null) {
                toBankType = SystemCodeUtil.getToBankType(payLinenumber, body.getCrtcombine());
                recAccInfo.put("to_bank_type", toBankType);
            } else {
                recAccInfo.put("to_bank_type", "01");
            }
        }
        return recAccInfo;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建预下单交易确认报文
     * @Date 2019/10/9
     * @Param [param, transferAccounts]
     **/
    private CtmJSONObject buildTransactionConfirmMsg(CtmJSONObject param, List<Salarypay> transferAccounts) {
        CtmJSONObject requestHead = buildRequestHead(PRE_ORDER_TRANSACTION_CONFIRM, param);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("porder_id", param.get("porderId"));
        requestBody.put("acct_name", param.get("acctName"));
        requestBody.put("acct_no", param.get("account"));
        BigDecimal totalMoney = BigDecimal.ZERO;
        BigDecimal totalLine = BigDecimal.ZERO;
        for (Salarypay transferAccount : transferAccounts) {
            totalMoney = totalMoney.add(transferAccount.getOriSum());
            totalLine = totalLine.add(transferAccount.getNumline());
        }
        requestBody.put("tran_tot_num", new Integer(totalLine.intValue()));
        requestBody.put("tran_tot_amt", totalMoney.setScale(2,BigDecimal.ROUND_HALF_UP));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /*
     * @Author majfd
     *
     * @Description 校验数据签名
     *
     * @Date 2019/6/18 11:23
     *
     * @Param [payBills]
     *
     * @Return void
     **/
    private void verifySignature(List<Salarypay> payBills) throws Exception {
        for (Salarypay bill : payBills) {
            if (bill.getInvalidflag() != null && bill.getInvalidflag().booleanValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101719"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00035", "单据【%s】,已作废，不能进行网银预下单") /* "单据【%s】,已作废，不能进行网银预下单" */,bill.getCode()));
            }
            if (StringUtils.isBlank(bill.getSignature()) || AppContext.getEnvConfig("checksign.tmp.switch","1").equals("0")) {
                continue;
            }
            String originalMsg = DigitalSignatureUtils.getSalaryPayOriginalMsg(bill);
            if (!digitalSignatureService.iTrusVerifySignature(originalMsg, bill.getSignature().replaceFirst(DigitalSignatureUtils.NEW_SIGN,""))) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                /* "单据【" */ + bill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180131","】数据签名验证失败") /* "】数据签名验证失败" */);
            }
        }
    }

    /**
     * 校验账户余额
     * @param salarypays
     * @throws Exception
     */
    private void verifyInitData(List<Salarypay> salarypays) throws Exception{
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        for (Salarypay bill : salarypays) {
            String payBankAccount = bill.getPayBankAccount();
            BigDecimal oriSum = bill.getOriSum();
            if (payBankAccount != null && oriSum != null) {
                if (!CmpWriteBankaccUtils.checkAccOverDraft(bill.getAccentity(),payBankAccount, bill.getCurrency())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101720"),ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F7","账户余额不足") /* "账户余额不足" */) /* "账户余额不足" */);
                }
            }
            //校验结算日期是否已日结
            Date maxSettleDate = null;
            if(maxSettleDateMaps.containsKey(bill.getAccentity())){
                maxSettleDate = maxSettleDateMaps.get(bill.getAccentity());
            }else{
                maxSettleDate = settlementService.getMaxSettleDate(bill.getAccentity());
                maxSettleDateMaps.put(bill.getAccentity(), maxSettleDate);
            }
            if (SettleCheckUtil.checkDailySettlement(maxSettleDate, true)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101704"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F2","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            }
        }
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建批量支付明细状态查询报文
     * @Date 2019/10/9
     * @Param [param]
     **/
    private CtmJSONObject buildPayStatusQueryMsg(CtmJSONObject param, String requestType) {
        CtmJSONObject requestHead = buildRequestHead(requestType, param);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("request_seq_no", param.getString("oldRequestSeqNo"));
        CtmJSONObject batchDetailPayStatusMsg = new CtmJSONObject();
        batchDetailPayStatusMsg.put("request_head", requestHead);
        batchDetailPayStatusMsg.put("request_body", requestBody);
        return batchDetailPayStatusMsg;
    }

    /**
     * @return com.yonyoucloud.fi.cm.transferaccount.TransferAccount
     * @Author tongyd
     * @Description 根据查询明细更新支付状态
     * @Date 2019/10/25
     * @Param [recordData]
     **/
    private void updatePayStatusByRecordData(CtmJSONObject recordData, Salarypay_b billBody) throws Exception {
        String payStatusStr = recordData.getString("pay_status");
        PaymentStatus payStatus = null;
        switch (payStatusStr) {
            case "00":
                payStatus = PaymentStatus.PayDone;
                break;
            case "01":
                payStatus = PaymentStatus.PayFail;
                break;
            case "02":
                payStatus = PaymentStatus.UnkownPay;
                break;
            default:
                payStatus = PaymentStatus.UnkownPay;
                break;
        }
        StringBuilder payMessage = new StringBuilder();
        if (recordData.containsKey("bank_resp_code")) {
            payMessage
                    .append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E7","【") /* "【" */);
            payMessage.append(recordData.get("bank_resp_code"));
            payMessage
                    .append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E8","】") /* "】" */);
        }
        if (recordData.containsKey("bank_resp_desc")) {
            payMessage.append(recordData.get("bank_resp_desc"));
        }

        // 表体全部处理完成后，更新表头状态，然后记账，生凭证
//        billBody.setEntityStatus(EntityStatus.Update);
//        Salarypay_b salarypay_b = MetaDaoHelper.findById(Salarypay_b.ENTITY_NAME, billBody.getId());
        billBody.setTradestatus(payStatus);
        if (payStatus.equals(PaymentStatus.PayFail)) {
            billBody.setInvalidflag(true);
        }
        if (payMessage.length() > 0) {
            billBody.setTrademessage(payMessage.toString());
        }
        if (recordData.containsKey("bank_seq_no")) {
            billBody.setBankseqno(recordData.getString("bank_seq_no"));
        }

        //对支付失败的明细，发送消息
        //对有部分成功的发送消息，在支付变更中处理
//		if(PaymentStatus.PayFail.equals(billBody.getTradestatus()) && billBody.getInvalidflag().booleanValue()) {
//			log.debug("-----对支付失败的明细，发送失败消息通知开始-----", billBody);
//			SendBizMessageUtils.sendBizMessage(billBody, "cmp_salarypaylist", "payfail");
//			log.debug("-----对支付失败的明细，发送失败消息通知结束-----");
//		}
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 解析交易确认返回报文
     * @Date 2019/10/24
     * @Param [postResult, transferAccounts, row]
     **/
    private void analysisTransactionConfirmRespData(CtmJSONObject postResult, List<Salarypay> payBills, CtmJSONArray row)
            throws Exception {
        StringBuilder payMessage = new StringBuilder();
        PayStatus payStatus = null;
        if ("0000".equals(postResult.getString("code"))) {
            CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                CtmJSONObject responseBody = postResult.getJSONObject("data").getJSONObject("response_body");
                // 00：全部成功；01：部分成功；02： 失败；03：正在处理 （原交易为批量 支付/代发工资，返回本字段）
                if ("00".equals(responseBody.get("bat_status"))) {
                    // 网上付款支付确认，若返回全部成功，先暂设置状态为支付中，查询支付状态后修改为支付成功
                    payStatus = PayStatus.Paying;
                } else if ("02".equals(responseBody.get("bat_status"))) {
                    payStatus = PayStatus.Fail;
                } else {
                    payStatus = PayStatus.Paying;
                }
                if (responseBody.containsKey("bank_resp_code")) {
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E7","【") /* "【" */);
                    payMessage.append(responseBody.get("bank_resp_code"));
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E8","】") /* "】" */);
                }
                if (responseBody.containsKey("bank_resp_desc")) {
                    payMessage.append(responseBody.get("bank_resp_desc"));
                }
            } else {
                payStatus = PayStatus.Paying;
                if (responseHead.containsKey("service_resp_code")) {
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E7","【") /* "【" */);
                    payMessage.append(responseHead.getString("service_resp_code"));
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E8","】") /* "】" */);
                }
                if (responseHead.containsKey("service_resp_desc")) {
                    payMessage.append(responseHead.getString("service_resp_desc"));
                }
            }
        } else {
            payStatus = PayStatus.PayUnknown;
            payMessage.append(postResult.get("message"));
        }
        Date currentDate = new Date();
        long userId = AppContext.getCurrentUser().getId();
        for (Salarypay payBill : payBills) {
            CtmJSONObject rowData = null;
            for (int i = 0; i < row.size(); i++) {
                if (row.getJSONObject(i).getLong("id").equals(payBill.getId())) {
                    rowData = row.getJSONObject(i);
                    break;
                }
            }
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setPaystatus(payStatus);
            payBill.setPaymessage(payMessage.toString());
            if (payStatus.equals(PayStatus.Success)) {
                payBill.setSettleuserId(userId);
                payBill.setSettleuser(AppContext.getCurrentUser().getName());
                payBill.setSettledate(new Date());
                payBill.setSettlestatus(SettleStatus.alreadySettled);
                payBill.set("_entityName", Salarypay.ENTITY_NAME);
                paySuccessBudget(payBill);
                cmpVoucherService.generateVoucherWithResult(payBill);
                journalService.updateJournal(payBill);
                if (rowData != null) {
                    rowData.put("settlestatus", SettleStatus.alreadySettled.getValue());
                    rowData.put("voucherstatus", payBill.getVoucherstatus().getValue());
                }
            }
            if (rowData != null) {
                rowData.put("paystatus", payStatus.getValue());
                rowData.put("paymessage", payMessage.toString());
            }
        }
        //如果是直联支付，取支付成功的系统日期
        List<Salarypay> updateBills = new ArrayList<>();
        for (Salarypay payBill : payBills) {
            if (payBill.getPaystatus() != null && payBill.getPaystatus().getValue() == PayStatus.Paying.getValue()) {
                updateBills.add(payBill);
            }
        }
        updateSuccessPayDate(updateBills, new Date(),false);
        MetaDaoHelper.update(Salarypay.ENTITY_NAME, payBills);
        Iterator<Salarypay> iterator = payBills.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getPaystatus() != PayStatus.Success) {
                iterator.remove();
            }
        }
        if (payBills.size() > 0) {
            SendBizMessageUtils.sendBizMessage(payBills, "cmp_salarypay", "paysucceed");
            // 发送通知
            payCallback(payBills);
        }
    }

    /**
     * 支付回调外部系统
     *
     * @param updateBills
     */
    private void payCallback(List<Salarypay> updateBills) throws Exception {
        // 获取hr通知url
        String serverUrl = AppContext.getEnvConfig("hrservice.url");
        if (StringUtils.isBlank(serverUrl)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101721"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F1","hr通知路径【hrservice.url】为空，请检查配置文件") /* "hr通知路径【hrservice.url】为空，请检查配置文件" */);
        }
        String yhtTenantId = InvocationInfoProxy.getTenantid().toString();
        String yhtUserId = AppContext.getCurrentUser().getYhtUserId();

        serverUrl = serverUrl + "/internal/pay/biz/payment/setPaymentStatus" + "?token=" + InvocationInfoProxy.getYhtAccessToken();
        for (Salarypay salarypay : updateBills) {
            // 回调外部系统
            if (EventSource.HRSalaryChase.getValue() != salarypay.getShort("srcitem")) {
                continue;
            }

            CtmJSONObject params = new CtmJSONObject();
            CtmJSONObject param = SalaryPayUtil.formatConversionToHR(salarypay);
            params.put("data", param);
            params.put("yht_tenantid", yhtTenantId);
            params.put("yht_userid", yhtUserId);
            params.put("newArch", true);//新架构标识
            log.error("payCallback通知薪资发放单数据：" + CtmJSONObject.toJSONString(params));
            String responseStr = HttpTookit.doPostWithJson(serverUrl, CtmJSONObject.toJSONString(params), null);
            log.error("payCallback薪资发放单返回数据：", responseStr);
        }
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 解析批量支付预下单反馈数据
     * @Date 2019/10/24
     * @Param [postResult, row]
     **/
    private void analysisBatchPayPreOrderRespData(CtmJSONObject postResult, List<Salarypay> payBills, CtmJSONArray row)
            throws Exception {
        StringBuilder payMessage = new StringBuilder();
        PayStatus payStatus = null;
        String porderId = null;
        if ("0000".equals(postResult.getString("code"))) {
            CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                CtmJSONObject responseBody = postResult.getJSONObject("data").getJSONObject("response_body");
                String porderStatus = responseBody.getString("porder_status");
                if ("00".equals(porderStatus)) {
                    payStatus = PayStatus.PreSuccess;
                } else {
                    payStatus = PayStatus.PreFail;
                }
                porderId = responseBody.getString("porder_id");
                if (responseBody.containsKey("risk_resp_code")) {
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E7","【") /* "【" */);
                    payMessage.append(responseBody.getString("risk_resp_code"));
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E8","】") /* "】" */);
                }
                if (responseBody.containsKey("risk_resp_data")) {
                    payMessage.append(responseBody.getString("risk_resp_data"));
                }
            } else {
                payStatus = PayStatus.PreFail;
                if (responseHead.containsKey("service_resp_code")) {
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E7","【") /* "【" */);
                    payMessage.append(responseHead.getString("service_resp_code"));
                    payMessage.append(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E8","】") /* "】" */);
                }
                if (responseHead.containsKey("service_resp_desc")) {
                    String service_resp_desc = responseHead.getString("service_resp_desc");
                    if (service_resp_desc != null && service_resp_desc.length() > 240){
                        service_resp_desc = service_resp_desc.substring(0,239);
                    }
                    payMessage.append(service_resp_desc);
                }
            }
        } else {
            payStatus = PayStatus.PreFail; // 因网络原因未返回预下单结果时，设置预下单失败，重新预下单，支付不明后续业务操作走不了
            payMessage.append(postResult.get("message"));
        }
        for (int i = 0; i < payBills.size(); i++) {
            CtmJSONObject rowData = row.getJSONObject(i);
            Salarypay paybill = payBills.get(i);
            paybill.setEntityStatus(EntityStatus.Update);
            rowData.put("porderid", porderId);
            paybill.set("porderid", porderId);
            rowData.put("paymessage", payMessage.toString());
            paybill.set("paymessage", payMessage.toString());
            rowData.put("paystatus", payStatus.getValue());
            paybill.set("paystatus", payStatus.getValue());
        }
        //如果是直联支付，取支付成功的系统日期
        List<Salarypay> updateBills = new ArrayList<>();
        for (Salarypay payBill : payBills) {
            if (payBill.getPaystatus() != null && payBill.getPaystatus().getValue() == PayStatus.PreSuccess.getValue()) {
                updateBills.add(payBill);
            }
        }
        updateSuccessPayDate(updateBills, new Date(),false);
        MetaDaoHelper.update(Salarypay.ENTITY_NAME, payBills);
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 解析批量支付明细状态查询返回报文
     * @Date 2019/10/25
     * @Param [responseBody]
     **/
    private void analysisPayStatusQueryRespData(CtmJSONObject responseBody, List<Salarypay> payBills, CtmJSONArray row)
            throws Exception {
        Object object = responseBody.get("record");
        //增加判空
        if (ObjectUtils.isEmpty(object)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103012"),ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_205E6BF204D00007","银企联报文异常：record字段为空，请联系银企联排查！") /* "银企联报文异常：record字段为空，请联系银企联排查！" */) );
        }
        CtmJSONArray record = new CtmJSONArray();
        if (object instanceof ArrayList || object instanceof CtmJSONArray) {
            record = responseBody.getJSONArray("record");
        } else {
            record.add(responseBody.getJSONObject("record"));
        }

        PayStatus payStatus = transPayStatus(responseBody.getString("bat_status"));
        List<Salarypay_b> salarypay_b = new ArrayList<>();
        // 先不支持批量合并支付，赋值失败笔数，不明笔数，合并支付后，这些通过表体计算得出
        if (payBills != null && payBills.size() == 1) {
            Salarypay payBill = payBills.get(0);
            //当前单据支付状态为全部成功时 不更新支付状态
            if(payBill.getPaystatus() == PayStatus.Success){
                return;
            }
            Map<String, Salarypay_b> bodyMap = new HashMap<String, Salarypay_b>();
            salarypay_b = payBill.Salarypay_b();
            for (Salarypay_b body : salarypay_b) {
//                body.setEntityStatus(EntityStatus.Update);
                if (PayStatus.PayUnknown.getValue() == payStatus.getValue()) {
                    body.setTradestatus(PaymentStatus.UnkownPay);
                }
                bodyMap.put(body.getTranseqno(), body);
            }
            CtmJSONObject rowData = row.getJSONObject(0);
            // 循环表体
            for (int j = 0; j < record.size(); j++) {
                String tran_seq_no = record.getJSONObject(j).getString("tran_seq_no");
                // 更新表体支付状态，支付信息
                updatePayStatusByRecordData(record.getJSONObject(j), bodyMap.get(tran_seq_no));
            }

            Short olcmoneyDigit = SalaryPayUtil.getOlcmoneyDigit(payBill);
            BigDecimal exchRate = payBill.getExchRate();
            Short exchRateOps = payBill.getExchRateOps();
            payBill.setEntityStatus(EntityStatus.Update);
            payBill.set("paystatus", payStatus.getValue());
            payBill.setFailmoney(responseBody.getBigDecimal("fail_amt"));
            payBill.setOlcfailmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, responseBody.getBigDecimal("fail_amt"), Integer.valueOf(olcmoneyDigit)));
            payBill.set("failnum", responseBody.getBigDecimal("fail_num"));
            payBill.setSuccessmoney(responseBody.getBigDecimal("succ_amt"));
            payBill.setOlcsuccessmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, responseBody.getBigDecimal("succ_amt"), Integer.valueOf(olcmoneyDigit)));
            payBill.set("successnum", responseBody.getBigDecimal("succ_num"));
            payBill.setUnknownmoney(responseBody.getBigDecimal("unknow_amt"));
            payBill.setOlcunknownmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, responseBody.getBigDecimal("unknow_amt"), Integer.valueOf(olcmoneyDigit)));
            payBill.set("unknownnum", responseBody.getBigDecimal("unknow_num"));
            // 银行明细对账编号
            String bank_check_code = responseBody.getString("bank_check_code");
            if (!StringUtils.isBlank(bank_check_code) && bank_check_code.length() > 59){
                bank_check_code = null; //农行对于本行、跨行数据，走不同批次接口，bank_check_code银企联返回拼接的数据，且农行不支持对账
            }
            payBill.set("bankcheckno", bank_check_code);
            // 对银行返回的部分成功，表体明细无支付不明的单据，直接确认成功处理
            if (payBill.getUnknownnum() != null && payBill.getUnknownnum().intValue() == 0 && BigDecimal.ZERO
                    .compareTo(payBill.getUnknownmoney()) == 0) {
                if (PayStatus.PartSuccess.equals(payBill.getPaystatus())) {
                    payBill.setPaystatus(PayStatus.Success);
                }
            }
            if (PayStatus.Success.equals(payBill.getPaystatus())) {
                payBill.setSettleuserId(AppContext.getCurrentUser().getId());
                payBill.setSettleuser(AppContext.getCurrentUser().getName());
                payBill.setSettledate(new Date());
                payBill.setSettlestatus(SettleStatus.alreadySettled);
                payBill.set("_entityName", Salarypay.ENTITY_NAME);
                paySuccessBudget(payBill);
                cmpVoucherService.generateVoucherWithPay(payBill);
                journalService.updateJournal(payBill);
            }
            // 前端显示赋值
            rowData.put("successnum", responseBody.getBigDecimal("succ_num"));
            rowData.put("successmoney", responseBody.getBigDecimal("succ_amt"));
            rowData.put("unknownnum", responseBody.getBigDecimal("unknow_num"));
            rowData.put("unknownmoney", responseBody.getBigDecimal("unknow_amt"));
            rowData.put("failnum", responseBody.getBigDecimal("fail_num"));
            rowData.put("failmoney", responseBody.getBigDecimal("fail_amt"));
            rowData.put("paystatus", payBill.getPaystatus().getValue());
            rowData.put("paymessage", payBill.getPaymessage());
            rowData.put("settlestatus", payBill.getSettlestatus().getValue());
            if (payBill.getVoucherstatus() != null) {
                rowData.put("voucherstatus", payBill.getVoucherstatus().getValue());
            }
            rowData.put("pubts", payBill.getPubts());
        }

        MetaDaoHelper.update(Salarypay.ENTITY_NAME, payBills);
        // 更新子表 salarypay_b
        if (salarypay_b instanceof List && CollectionUtils.isNotEmpty(salarypay_b)) {

            // 分组处理逻辑
            int batchSize = 900;
            for (int i = 0; i < salarypay_b.size(); i += batchSize) {
                int end = Math.min(i + batchSize, salarypay_b.size());
                List<Salarypay_b> batchList = salarypay_b.subList(i, end);
                try {
                    for (int j = 0; j < batchList.size(); j++) {
                        Salarypay_b salarypay_B = batchList.get(j);
                        salarypay_B.setEntityStatus(EntityStatus.Update);
                    }
                    MetaDaoHelper.update(Salarypay_b.ENTITY_NAME, batchList);
                } catch (Exception e) {
                    log.error("薪资支付查询支付状态子表分组更新失败", i + 1, end, e);
                }
            }
        } else {
            log.error("薪资支付查询支付状态子表分组更新失败");
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EF2474C05700007", "薪资支付子表分组更新失败！") /* salarypay 子表分组更新失败！" */);
        }
        Iterator<Salarypay> iterator = payBills.iterator();
        while (iterator.hasNext()) {
            // 支付成功：发送消息 支付失败：重新发起交易 支付中：再次查询 部分成功：支付确认(确认时发消息)
            if (iterator.next().getPaystatus() != PayStatus.Success) {
                iterator.remove();
            }
        }
        if (payBills.size() > 0) {
            SendBizMessageUtils.sendBizMessage(payBills, "cmp_salarypay", "paysucceed");
            // 后面业务代码报错，事务回滚，下次更新状态会重新发送，所以不能在这通知hr
            payCallback(payBills);
        }

        // 支付失败释放资金计划
        List<Salarypay> salaryPayFailReleaseFundPlanList = new ArrayList<>();
        for (Salarypay payBill : payBills) {
            if (payBill.getPaystatus().getValue() == PayStatus.Success.getValue()
                    && ValueUtils.isNotEmptyObj(payBill.getFundPlanProject())
                    && payBill.getFailnum().compareTo(BigDecimal.ZERO) > 0){
                salaryPayFailReleaseFundPlanList.add(payBill);
            }
        }

        if (CollectionUtils.isNotEmpty(salaryPayFailReleaseFundPlanList)) {
            for (Salarypay payBill : salaryPayFailReleaseFundPlanList) {
            List<BizObject> partReleaseFundBillForFundPlanProjectList = new ArrayList<>();
                partReleaseFundBillForFundPlanProjectList.add(payBill);
                if (CollectionUtils.isNotEmpty(partReleaseFundBillForFundPlanProjectList)) {
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("accentity", payBill.get("accentity"));
                    map2.put("vouchdate", payBill.get("vouchdate"));
                    map2.put("code", payBill.get("code"));
                    map2.put("settleFailed", true);
                    List<CapitalPlanExecuteModel> checkObject = cmCommonService.putCheckParameterSalaryPayOldInterface(partReleaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, IBillNumConstant.SALARYPAY, map2);
                    if (ValueUtils.isNotEmptyObj(checkObject)) {
                        try {
//                            RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject);
//                            RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).releaseAndEmploy(checkObject);
                            RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).releaseExecutionAmount(checkObject);
                        } catch (CtmException e) {
                            log.error("SalaryPay part release fail! data={}, e={}", CtmJSONObject.toJSONString(checkObject), e.getMessage());
                        }
                    }
                }
            }
        }

    }

    private PayStatus transPayStatus(String payStatusStr) {
        if (StringUtils.isBlank(payStatusStr)) {
            return PayStatus.Fail;
        }
        switch (payStatusStr) {
            case "00": // 全部成功
                return PayStatus.Success;
            case "01": // 部分成功
                return PayStatus.PartSuccess;
            case "02": // 失败
                return PayStatus.Fail;
            case "03": // 正在处理
                return PayStatus.Paying;
            case "04": // 预下单
                return PayStatus.PayUnknown;
            case "05": // 预下单失效
                return PayStatus.Fail;
            default:
                return PayStatus.PayUnknown;
        }
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建请求报文头
     * @Date 2019/9/26
     * @Param [transCode, param]
     **/
    private CtmJSONObject buildRequestHead(String transCode, CtmJSONObject param) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", param.get("requestseqno"));
        requestHead.put("cust_no", param.get("customNo"));
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", param.get("operator"));
        requestHead.put("oper_sign", param.get("signature"));
        requestHead.put("tran_code", transCode);
        requestHead.put("Yonsuite_AutoTask", "Y");
        return requestHead;
    }

    /*
     * @Author tongyd
     *
     * @Description 构建批量支付预下单报文
     *
     * @Date 2019/9/23
     *
     * @Param [param, row]
     *
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildBatchPayPreOrderMsg(CtmJSONObject param, List<Salarypay> payBills, CtmJSONArray row, String requestType)
            throws Exception {
        Map<String, Object> payAccount = QueryBaseDocUtils
                .queryEnterpriseBankAccountById(param.getString("payBankAccount"));
        if (!getOpenFlag(param.getString("payBankAccount"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101723"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F44AA05B0000E", "银行账户【%s】没有直联支付权限，网银预下单失败，请前往【银企联账号】检查银行账户直联权限。") /* "银行账户【%s】没有直联支付权限，网银预下单失败，请前往【银企联账号】检查银行账户直联权限。" */, payAccount.get("name")));
        }
        Object payLinenumber = null;
        if (!payAccount.isEmpty()) {
            Map<String, Object> queryBankDotById = QueryBaseDocUtils.queryBankDotById(payAccount.get("bankNumber"));
            payLinenumber = queryBankDotById.get("linenumber");
            if (payLinenumber == null) {
                payLinenumber = payAccount.get("lineNumber");
            }
        }
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(param.getString("payBankAccount"));
//        String customNo = bankAccountSettingService.getCustomNoAndCheckUKeyByBankAccountId(param.getBoolean("isNeedCheckUkey"), param.getString("payBankAccount"), param.getString("customNo"));
        String requestseqno = DigitalSignatureUtils.buildRequestNum(customNo);
        param.put("requestseqno", requestseqno);
        param.put("customNo", customNo);
        CtmJSONObject requestHead = buildRequestHead(requestType, param);
        CtmJSONObject requestBody = new CtmJSONObject();
        String batNo = UUID.randomUUID().toString().replaceAll("-", "");
        requestBody.put("bat_no", batNo); // 批次号
        requestBody.put("acct_no", payAccount.get("account"));
        requestBody.put("acct_name", payAccount.get("acctName"));
        requestBody.put("curr_code", param.get("currencyCode"));
        requestBody.put("send_url",
                AppContext.getEnvConfig("servername") + "/cmp/salarypay/updateBatchPayStatus");
        requestBody.put("porder_validate", "8");

        String backParam = "yht_tenantid:" + InvocationInfoProxy.getTenantid() + ",yht_userid:"
                + AppContext.getCurrentUser().getYhtUserId();
        CtmJSONArray record = new CtmJSONArray();
        BigDecimal totalMoney = BigDecimal.ZERO;
        BigDecimal totalLine = BigDecimal.ZERO;

        List<Salarypay_b> salarypayBUpdateList = new ArrayList<>();

//        List<Salarypay_b> salarypayBUpdate = new ArrayList<Salarypay_b>();
        for (int i = 0; i < payBills.size(); i++) {
            int otherbankline = 0;
            Salarypay salarypay = payBills.get(i);
            CtmJSONObject rowData = row.getJSONObject(i);
            // 总金额，总笔数
            totalMoney = totalMoney.add(salarypay.getOriSum());
            totalLine = totalLine.add(salarypay.getNumline());
            // 设置批次号
            salarypay.set("batno", batNo);
            rowData.put("batno", batNo);
            // 设置请求流水号
            salarypay.set("requestseqno", requestseqno);
            rowData.put("requestseqno", requestseqno);
            // 用途
            String purpose = salarypay.getPurpose();
            // 代发报销时
            if (salarypay.getBusitype() != null && salarypay.getBusitype().getValue() == BusiTypeEnum.PAY_EXPENSE.getValue()) {
                if (StringUtils.isBlank(salarypay.getAgenttype())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101724"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FD","非工资类代发报销时，代发类型不能为空！") /* "非工资类代发报销时，代发类型不能为空！" */);
                }
                // 代发类型编号
                requestBody.put("busi_code", salarypay.getAgenttype());
                // 代发类型名
                requestBody.put("busi_name", SalaryPayUtil.getAgentTypeName(salarypay.getAgenttype()));
            }
            List<Salarypay_b> salarypay_b = salarypay.Salarypay_b();

            for (Salarypay_b body : salarypay_b) {
                CtmJSONObject recordData = new CtmJSONObject();
                // 设置交易流水号
                String tranSeqNo = DigitalSignatureUtils.buildTranSeqNo(customNo);
//                body.setEntityStatus(EntityStatus.Update);
                body.set("transeqno", tranSeqNo);
                recordData.put("tran_seq_no", tranSeqNo);
                Map<String, Object> recAccInfo = getRecAccInfo(payLinenumber, body, salarypay.getTobanktype());
                // 跨行标识
                recordData.put("to_bank_type", recAccInfo.get("to_bank_type"));
                String toBankType = (String) recAccInfo.get("to_bank_type");
                body.setPaymode(toBankType);
                if ("03".equals(toBankType) || "04".equals(toBankType) || "05".equals(toBankType)) {
                    otherbankline++;
                }
                // 员工编号
                recordData.put("to_inner_no", body.get("showpersonnum"));
                // 收方证件类型
                recordData.put("to_id_type", body.get("identitytype"));
                // 收方证件号码
                recordData.put("to_id_code", body.get("identitynum"));
                // 收方账号
                recordData.put("to_acct_no", body.get("crtacc"));
                // 收方户名
                recordData.put("to_acct_name", body.get("crtaccname"));
                // 收方开户行联行号 (非银联卡必输)
                recordData.put("to_bank_no", body.get("crtcombine"));
                // 收方开户行名(非银联必输)
                recordData.put("to_brch_name", body.get("crtbank"));
                // 工资明细
                recordData.put("salary_detail", body.get("salarydetail"));
                // 用户说明
                recordData.put("user_name", "");
                // 金额
                recordData.put("tran_amt", body.getAmount().setScale(2,BigDecimal.ROUND_HALF_UP));
                // 用途
                if (StringUtils.isBlank(body.getPurpose())){
                    recordData.put("use_desc", purpose);
                } else{
                    recordData.put("use_desc", body.getPurpose());
                }
                // 附言
                if (StringUtils.isBlank(body.get("postscript"))){
                    recordData.put("remark", salarypay.getSummary());
                } else {
                    recordData.put("remark", body.get("postscript"));
                }
                recordData.put("back_para", backParam);
                record.add(recordData);
                salarypayBUpdateList.add(body);
            }
            if (salarypay.getOtherbankflag() == null) {
                if (otherbankline == 0) {
                    salarypay.setOtherbankflag(OtherBankFlag.SameBank);
                } else if (otherbankline == salarypay_b.size()) {
                    salarypay.setOtherbankflag(OtherBankFlag.OtherBank);
                } else {
                    salarypay.setOtherbankflag(OtherBankFlag.OtherBank);
//					throw new CtmException(
//							com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001123701") /* "同一批次收款人，必须全部为本行或者全部为他行" */);
                }
            }
            // 本行跨行标志 M
            requestBody.put("other_bank_flag", salarypay.getShort("otherbankflag").toString());
        }
        requestBody.put("tot_num", new Integer(totalLine.intValue()));
        requestBody.put("tran_amt", totalMoney.setScale(2,BigDecimal.ROUND_HALF_UP));
        requestBody.put("record", record);
        CtmJSONObject postMsg = new CtmJSONObject();
        postMsg.put("request_head", requestHead);
        postMsg.put("request_body", requestBody);
        postMsg.put("salarypayBUpdateList",salarypayBUpdateList);
        return postMsg;
    }

    /*
     * @Author majfd
     *
     * @Description 获取企业银行账户是否开通银企联
     *
     * @Date 2019/6/4 11:29
     *
     * @Param [enterpriseBankAccount]
     *
     * @Return boolean
     **/
    private boolean getOpenFlag(Object enterpriseBankAccount) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag,empower");
        QueryConditionGroup conditionGroup = QueryConditionGroup
                .and(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if ((setting != null && setting.size() > 0) && EmpowerConstand.EMPOWER_QUERYANDPAY.equals(setting.get("empower"))) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }

    @Override
    public void hasSalaryPayBySrcbillid(String srcbillid, List<String> listSrcBillid_b) throws Exception {
        // TODO Auto-generated method stub
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").eq(srcbillid));
        // 作废标志 不是
        queryConditionGroup.appendCondition(QueryCondition.name("invalidflag").not_eq(1));
        querySchema.addCondition(queryConditionGroup);
        List<Salarypay> queryObject = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema, null);
        if (queryObject != null && queryObject.size() > 0) {
            List<Long> ids = new ArrayList<Long>();
            for (Salarypay salarypay : queryObject) {
                ids.add(salarypay.getId());
            }
            // 因为薪资传递过来的单据，表头srcbillid和表体srcbillid_b 都是支付单id,值一样，所以使用srcbillid过滤查询
            QueryConditionGroup queryConditionGroupBody = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroupBody.appendCondition(QueryCondition.name("mainid").in(ids));
            queryConditionGroupBody.appendCondition(QueryCondition.name("srcbillid_b").eq(srcbillid));
            queryConditionGroupBody.appendCondition(QueryCondition.name("invalidflag").not_eq(1));
            List<Map<String, Object>> payBListQuery = MetaDaoHelper.query(Salarypay_b.ENTITY_NAME,
                    QuerySchema.create().addSelect("*").addCondition(queryConditionGroupBody));
            if (payBListQuery != null && payBListQuery.size() > 0) {
                for (Map<String, Object> map : payBListQuery) {
                    if (PaymentStatus.NoPay.getValue() == (int) map.get("tradestatus") || PaymentStatus.UnkownPay.getValue() == (int) map
                            .get("tradestatus")) {
                        StringBuffer errmsg = new StringBuffer(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400445", "员工编号为[%s]重复支付，还有未处理完成的薪资支付单，请先在薪资支付工作台处理完毕") /* "员工编号为[%s]重复支付，还有未处理完成的薪资支付单，请先在薪资支付工作台处理完毕" */,map.get("showpersonnum")));
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101725"),errmsg.toString());
                    }
                }

            }
        }
        return;
    }

    /**
     * @param srcBillId
     * @return
     * @throws Exception
     */
    private List<Salarypay> queryAggvoBySrcbillid(List<String> srcBillId) throws Exception {
        // TODO Auto-generated method stub
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("srcbillid").in(srcBillId));
        // 作废标志 不是
        queryConditionGroup.appendCondition(QueryCondition.name("invalidflag").not_eq(1));
        querySchema.addCondition(queryConditionGroup);
        List<Salarypay> queryObject = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema, null);
        if (queryObject != null && queryObject.size() > 0) {
            List<Long> ids = new ArrayList<Long>();
            for (Salarypay salarypay : queryObject) {
                ids.add(salarypay.getId());
            }

            QueryConditionGroup queryConditionGroupBody = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroupBody.appendCondition(QueryCondition.name("mainid").in(ids));
            queryConditionGroupBody.appendCondition(QueryCondition.name("invalidflag").not_eq(1));
            List<Map<String, Object>> payBListQuery = MetaDaoHelper.query(Salarypay_b.ENTITY_NAME,
                    QuerySchema.create().addSelect("*").addCondition(queryConditionGroupBody));

            Map<Long, List<Salarypay_b>> payBMapList = new HashMap<Long, List<Salarypay_b>>();
            if (payBListQuery != null && payBListQuery.size() > 0) {
                List<Salarypay_b> pbyBList = null;
                for (Map<String, Object> map : payBListQuery) {
                    Salarypay_b pbyBill_b = new Salarypay_b();
                    pbyBill_b.setEntityStatus(EntityStatus.Unchanged);
                    pbyBill_b.init(map);
                    if (payBMapList.get(map.get("mainid")) == null) {
                        pbyBList = new ArrayList<Salarypay_b>();
                    } else {
                        pbyBList = payBMapList.get(map.get("mainid"));
                    }
                    pbyBList.add(pbyBill_b);
                    payBMapList.put((Long) map.get("mainid"), pbyBList);
                }
                for (Salarypay list : queryObject) {
                    list.setSalarypay_b(payBMapList.get(list.getId()));
                }
            }
        }
        return queryObject;
    }


    @Override
    public CtmJSONObject invalid(CtmJSONObject param) throws Exception {
        // TODO Auto-generated method stub
        CtmJSONArray rows = param.getJSONArray("rows");
        List<Salarypay> updateBills = new ArrayList<>(rows.size());
        List<Object> ids = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failcount = 0;
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F3","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001155005","】被锁定不能作废！") /* "】被锁定不能作废！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101726"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F3","单据【") /* "单据【" */ + rowData.get("code") +
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001155005","】被锁定不能作废！") /* "】被锁定不能作废！" */);
                }
                failcount++;
                continue;
            }
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, rowData.getLong("id"));
            if (payBill == null) {
                failed.put(id.toString(), id.toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101699"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                failcount++;
                continue;
            }
            if (rowData.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805EB","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101727"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000153380","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                }
                failcount++;
                continue;
            }
            PayStatus payStatus = payBill.getPaystatus();
            if (payStatus != null && payStatus != PayStatus.Fail) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F8","】支付状态不能进行作废！") /* "】支付状态不能进行作废！" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                                    /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F8","】支付状态不能进行作废！") /* "】支付状态不能进行作废！" */);
                }
                failcount++;
                continue;
            }
            if (BooleanUtils.isTrue(payBill.getInvalidflag())) {
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E5","单据【") /* "单据【" */
                        /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805F9","】已作废，不能重复作废！") /* "】已作废，不能重复作废！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101728"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FA","该单据已作废，不能重复作废！") /* "该单据已作废，不能重复作废！" */);
                }
                failcount++;
                continue;
            }
            Boolean check = journalService.checkJournal(payBill.getId());
            if (check) {
                failed.put(id.toString(), id.toString());
                messages.add(payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FC","单据已勾对") /* "单据已勾对" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101729"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FC","单据已勾对") /* "单据已勾对" */);
                }
                failcount++;
                continue;
            }
            //删除日记账
            CmpWriteBankaccUtils.delAccountBook(payBill.getId().toString());

            //释放资金计划
            List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
            Object isToPushCspl = payBill.get(ICmpConstant.IS_TO_PUSH_CSPL);
            if (ValueUtils.isNotEmptyObj(isToPushCspl) && 1 == Integer.parseInt(isToPushCspl.toString())) {
                payBill.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                releaseFundBillForFundPlanProjectList.add(payBill);
            }
            if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
                Map<String, Object> map = new HashMap<>();
                map.put(ICmpConstant.ACCENTITY, payBill.get(ICmpConstant.ACCENTITY));
                map.put(ICmpConstant.VOUCHDATE, payBill.get(ICmpConstant.VOUCHDATE));
                map.put(ICmpConstant.CODE, payBill.get(ICmpConstant.CODE));
                List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(releaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, IBillNumConstant.SALARYPAY, map);
                if (ValueUtils.isNotEmptyObj(checkObject)) {
                    try {
                        RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject);
                        RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101730"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080063", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ +e.getMessage());
                    }

                }
            }

            payBill.setEntityStatus(EntityStatus.Update);
            payBill.setInvalidflag(Boolean.TRUE);
            payBill.setPaymessage(payBill.getPaymessage() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FF","【用户已作废】") /* "【用户已作废】" */);
            rowData.put("invalidflag", true);
            rowData.put("paymessage", payBill.getPaymessage() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805FF","【用户已作废】") /* "【用户已作废】" */);
            updateBills.add(payBill);
            ids.add(id);
        }
        if (updateBills.size() > 0) {
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, updateBills);
            cmCommonService.refreshPubTsNew(Salarypay.ENTITY_NAME, ids, rows);
            //作废：是否占预算更新为未占用，此时清空预占
            if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
                for (Salarypay updateBill : updateBills) {
                    cmpBudgetSalarypayManagerService.executeBudgetDelete(updateBill);
                }
            }
            // 发送通知消息
            payCallback(updateBills);
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180600","作废成功!") /* "作废成功!" */);
        } else {
            message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00036", "共%s张单据作废成功,") /* "共%s张单据作废成功," */,rows.size()));
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180601","0张作废失败！") /* "0张作废失败！" */);
        }
        param.put("message", message);
        param.put("msgs", messages);
        param.put("rows", rows);
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failcount);
        param.put("failCount", failcount);
        if (failed.size() > 0) {
            param.put("failed", failed);
        }
        return param;
    }

    @Override
    public String sendPayStatus(List<String> srcbillid) throws Exception {
        // TODO Auto-generated method stub
        if (srcbillid == null || srcbillid.size() == 0) {
            return null;
        }
        List<Salarypay> aggVO = queryAggvoBySrcbillid(srcbillid);
        CtmJSONArray paramArr = new CtmJSONArray();
        for (Salarypay salarypay : aggVO) {
            CtmJSONObject item = SalaryPayUtil.formatConversionToHR(salarypay);
            paramArr.add(item);
        }
        return CtmJSONObject.toJSONString(paramArr);
    }

    @Override
    public boolean hasUnknownData(CtmJSONObject param) throws Exception {
        // TODO Auto-generated method stub
        List<Object> mainIds = new ArrayList();
        mainIds.add(param.get("id"));
        List<Salarypay> queryAggvoByIds = queryAggvoByIds(mainIds);
        if (queryAggvoByIds == null || queryAggvoByIds.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101731"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180608","根据薪资支付单主键未查询到单据数据！") /* "根据薪资支付单主键未查询到单据数据！" */);
        }
        Salarypay salarypay = queryAggvoByIds.get(0);
        PayStatus paystatus = salarypay.getPaystatus();
        boolean hasUnknownData = false;
        if (PayStatus.PartSuccess.equals(paystatus) || PayStatus.PayUnknown.equals(paystatus) || PayStatus.Paying.equals(paystatus)) {
            List<Salarypay_b> bodys = salarypay.getBizObjects("Salarypay_b", Salarypay_b.class);
            for (int i = 0; i < bodys.size(); i++) {
                PaymentStatus tradestatus = bodys.get(i).getTradestatus();
                if (PaymentStatus.UnkownPay.equals(tradestatus)) {
                    hasUnknownData = true;
                    break;
                }
            }
            if (PayStatus.PayUnknown.equals(paystatus)) {
                hasUnknownData = true;
            }
            return hasUnknownData;
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101732"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180609","当前单据支付状态不容许进行支付变更操作！") /* "当前单据支付状态不容许进行支付变更操作！" */);
        }
    }


    @Override
    public boolean budgetSuccess(Salarypay salarypay) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
            return false;
        }
        // 执行
        ResultBudget resultBudget = cmpBudgetSalarypayManagerService.gcExecuteTrueAudit(salarypay, IBillNumConstant.SALARYPAY, BillAction.APPROVE_PASS, true);
        return resultBudget.isSuccess();
    }

    @Override
    public void updatePayStatusAfterOffline(Salarypay salarypay) throws Exception {
        log.error("修改为线下支付成功");
        //当薪资支付方式为线下支付时，薪资支付单的子表并不会更新支付状态，仍为待支付。本版修正该逻辑，变更为：当薪资支付单完成线下支付操作时，薪资支付单的子表更新支付状态为支付成功
        if (salarypay.getPaystatus() != null && salarypay.getPaystatus().getValue() == PayStatus.OfflinePay.getValue()) {
            Salarypay salarypayNew = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId());
//            salarypayNew.setPaystatus(salarypay.getPaystatus());//支付成功
//            salarypayNew.setSettlestatus(salarypay.getSettlestatus());//已结算
//            salarypayNew.setPaydate(salarypay.getPaydate());
//            salarypayNew.setSettledate(salarypay.getSettledate());
            List<Salarypay_b> updateList = new ArrayList<>();
            List<Salarypay_b> salarypay_bs = salarypayNew.Salarypay_b();
            if (CollectionUtils.isNotEmpty(salarypay_bs)) {
                for (Salarypay_b salarypay_b : salarypay_bs) {
                    Salarypay_b update = new Salarypay_b();
                    update.setId(salarypay_b.getId());
                    update.setTradestatus(PaymentStatus.PayDone);
                    update.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                    update.setEntityStatus(EntityStatus.Update);
//                    updateList.add(update);
                    MetaDaoHelper.update(Salarypay_b.ENTITY_NAME, update);
                }
            }
//            if (CollectionUtils.isNotEmpty(updateList)) {
//                MetaDaoHelper.update(Salarypay_b.ENTITY_NAME, updateList);
//            }
            if (budgetSuccess(salarypay)) {
                Salarypay update = new Salarypay();
                update.setId(salarypay.getId());
                update.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
            }
        }
    }

    public boolean budgetFail(Salarypay_b bill_b) throws Exception {
        Salarypay salarypay = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, bill_b.getMainid());
        Salarypay_b newSalarypay_b = MetaDaoHelper.findById(Salarypay_b.ENTITY_NAME, bill_b.getId());
        if (newSalarypay_b.getIsOccupyBudget() != null && newSalarypay_b.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
            // 已经释放预占成功的单据，不再进行释放预占的接口调用
            return false;
        }
        // 先释放预占
        ResultBudget cancelResultBudget = cmpBudgetSalarypayManagerService.gcExecuteDelete(salarypay, newSalarypay_b, IBillNumConstant.SALARYPAY, BillAction.APPROVE_PASS);
        return cancelResultBudget.isSuccess();
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();

        String entityname = BillNumberEnum.find(billnum);
        if(com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(entityname)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400444", "请求参数缺失") /* "请求参数缺失" */));
        }

        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)){
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if(ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())){
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400444", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if(!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(changeBillno)){
            if(CollectionUtils.isEmpty(bizObjects)){
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400446", "变更金额小于原金额,不需要校验!") /* "变更金额小于原金额,不需要校验!" */));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum =changeBillno;
        }else{
            //非变更单据 自己单据
            if(CollectionUtils.isEmpty(bizObjects)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400444", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetSalarypayManagerService.budgetCheckNew(bizObjects,billnum, BudgetUtils.SUBMIT);
    }
    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        // 查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("Salarypay_b").addSelect("*");
        schema.addCompositionSchema(detailSchema);
        return MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, schema, null);
    }

    //支付成功实占
    public void paySuccessBudget(Salarypay payBill) throws Exception {
        if (payBill.getIsOccupyBudget() != null && payBill.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
            log.error("已经实占跳过");
        } else {
            if (budgetSuccess(payBill)) {
                payBill.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
            }
        }
    }

    @Override
    public CtmJSONObject payBankCheckUKey() {
        // 从YMS取值-payBankCheckUKey, 默认校验-"1"
        String payBankCheckUKey = AppContext.getEnvConfig(IBussinessConstant.PAY_BANK_CHECK_UKEY, ICsplConstant.ONE);

        CtmJSONObject checkUKey = new CtmJSONObject();
        checkUKey.put("checkUkey", ICsplConstant.ONE.equals(payBankCheckUKey));
        return checkUKey;
    }
}
