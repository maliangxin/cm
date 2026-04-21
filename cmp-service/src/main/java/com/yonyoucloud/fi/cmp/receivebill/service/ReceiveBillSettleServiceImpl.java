package com.yonyoucloud.fi.cmp.receivebill.service;


import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.arap.service.account.IArapSettleAccountService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillSettleBO;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillUnsettleBO;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.SettleCheckUtil;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ONE;

@Slf4j
@Service
public class ReceiveBillSettleServiceImpl implements ReceiveBillSettleService {


    @Autowired
    YTSReceiveBillSettleServiceImpl ytsReceiveBillSettleServiceImpl;
    @Autowired
    private JournalService journalService;
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Transactional
    @Override
    public ReceiveBillSettleBO processSettleItem(Long receiveBillId, boolean batchProcess, Map<Long, String> codeMap, Date date, Map<String, Date> maxSettleDateMaps) throws Exception {
        ReceiveBillSettleBO.ReceiveBillSettleBOBuilder builder = ReceiveBillSettleBO.builder();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(receiveBillId.toString());
        if (null == ymsLock) {
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + codeMap.get(receiveBillId)
                            + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B0","】已锁定，请稍后重试！") /* "】已锁定，请稍后重试！" */);
            if (!batchProcess) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100586"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B1","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            return builder.build();
        }
        ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, receiveBillId, 3);
        if (null == receiveBill) {
            builder.failedId(receiveBillId.toString())
                    .message(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00153", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, codeMap.get(receiveBillId)));
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100587"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00153", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, codeMap.get(receiveBillId)));
            }
            return builder.build();
        }
        if (receiveBill.getAuditstatus() != null && receiveBill.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802BB","】未审批，不能进行结算！") /* "】未审批，不能进行结算！" */);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100588"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802BC","该单据未审批，不能进行结算！") /* "该单据未审批，不能进行结算！" */);
            }
            return builder.build();
        }
        if (date != null && receiveBill.getAuditDate() != null && date.compareTo(receiveBill.getAuditDate()) < 0) {
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802BD","】结算日期小于审批日期，不能结算！") /* "】结算日期小于审批日期，不能结算！" */);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100589"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【" + receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802BD","】结算日期小于审批日期，不能结算！") /* "】结算日期小于审批日期，不能结算！" */));
            }
            return builder.build();
        }

        if (receiveBill.get("enterprisebankaccount") != null && receiveBill.get("cashaccount") != null) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C0","】银行账户与现金账户不能同时存在！") /* "】银行账户与现金账户不能同时存在！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100590"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C1","银行账户与现金账户不能同时存在！") /* "银行账户与现金账户不能同时存在！" */);
            }
            return builder.build();
        }
        if (receiveBill.get("enterprisebankaccount") == null && receiveBill.get("cashaccount") == null) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                            .getMessage("P_YS_FI_CM_0001038104") /* "】银行账户与现金账户必须录入其中一个！" */);
            if (!batchProcess) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C4","银行账户与现金账户必须录入其中一个！") /* "银行账户与现金账户必须录入其中一个！" */);
            }
            return builder.build();
        }

        if (receiveBill.get("settlemode") == null) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A4","】结算方式为空！") /* "】结算方式为空！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100591"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A7","结算方式为空！") /* "结算方式为空！" */);
            }
            return builder.build();
        }
        //最大日结日期
        Date maxSettleDate = maxSettleDateMaps.get(receiveBill.getAccentity());
        if(SettleCheckUtil.checkDailySettlement(maxSettleDate, false)){
            builder.failedId(receiveBill.getId().toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill.getCode() +
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A9","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100592"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A9","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            }
            return builder.build();
        }
        if (receiveBill.getSettlestatus() != null && receiveBill.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802AC","】已经结算不能重复结算！") /* "】已经结算不能重复结算！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100593"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802AD","单据已经结算不能重复结算！") /* "单据已经结算不能重复结算！" */);
            }
            return builder.build();
        }
        //结算方式
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setId((Long) receiveBill.get("settlemode"));
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        if (dataList != null && dataList.size() > 0) {
            if (dataList.get(0).getServiceAttr().equals(0)) {
                if (receiveBill.get("enterprisebankaccount") == null) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                    builder.failedId(receiveBillId.toString())
                            .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                                    .getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                    .getMessage("P_YS_FI_CM_0001038089") /* "】结算方式业务属性为银行业务，请录入银行账户！" */);
                    if (!batchProcess) {
                        throw new CtmException(
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802AF","结算方式业务属性为银行业务，请录入银行账户！") /* "结算方式业务属性为银行业务，请录入银行账户！" */);
                    }
                    return builder.build();
                }
            } else if (dataList.get(0).getServiceAttr().equals(1)) {
                if (receiveBill.get("cashaccount") == null) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                    builder.failedId(receiveBillId.toString())
                            .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                                    .getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                    .getMessage("P_YS_FI_CM_0001038109") /* "】结算方式业务属性为现金业务，请录入现金账户！" */);
                    if (!batchProcess) {

                        throw new CtmException(
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B2","结算方式业务属性为现金业务，请录入现金账户！") /* "结算方式业务属性为现金业务，请录入现金账户！" */);
                    }
                    return builder.build();
                }
            } else {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                builder.failedId(receiveBillId.toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                                .getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0001038090") /* "】结算方式业务属性只能为银行业务与现金业务！" */);
                if (!batchProcess) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B7","结算方式业务属性只能为银行业务与现金业务！") /* "结算方式业务属性只能为银行业务与现金业务！" */);
                }
                return builder.build();
            }
        }
        receiveBill.setSettlestatus(SettleStatus.alreadySettled);
        receiveBill.setSettleuser(AppContext.getCurrentUser().getId().toString());
        if (BillInfoUtils.getBusinessDate() != null) {
            receiveBill.setSettledate(new Date());
            receiveBill.setDzdate(BillInfoUtils.getBusinessDate());
        } else {
            receiveBill.setSettledate(new Date());
            receiveBill.setDzdate(new Date());
        }
        try {
            String systemCode = SystemCodeUtil.getSystemCode(receiveBill);
            if(receiveBill.getSrcitem()!= null){
                //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                if(receiveBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() ||
                        "fiar".equals(systemCode) || "fiap".equals(systemCode)){
                    Map<String,Object> paramMap = new HashMap<>();
                    paramMap.put("settledate",receiveBill.getSettledate());
                    paramMap.put("settlestatus",receiveBill.getSettlestatus().getValue());
                    paramMap.put("settlemode",receiveBill.getSettlemode());
                    paramMap.put("enterprisebankaccount",receiveBill.getEnterprisebankaccount());
                    paramMap.put("cashaccount",receiveBill.getCashaccount());
                    paramMap.put("srcbillid",receiveBill.getSrcbillid());
                    paramMap.put("dzdate", receiveBill.getDzdate());
                    ytsReceiveBillSettleServiceImpl.settleBill(receiveBill,paramMap);
                }
            }
        }catch (Exception e){
            log.error("##   #####   单据结算同步应收异常   ##  ######"+e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100594"),e.getMessage());
        }
        // 凭证处理结果
        CtmJSONObject generateResult = new CtmJSONObject();
        try {
            // 来源单据为空则由现金生成凭证，否则调用应收接口，由应收生成凭证
            receiveBill.set("_entityName", ReceiveBill.ENTITY_NAME);
            receiveBill.set("oldvoucherstatus", receiveBill.getVoucherstatus().getValue());
            generateResult = cmpVoucherService.generateVoucherWithResult(receiveBill);
        } catch (Exception e) {
            log.info("单据【" + receiveBill.getCode() + "】调用生成凭证接口报错：" + e.getMessage());
            builder.failedId(receiveBillId.toString());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100589"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill.getCode() +
                MessageUtils.getMessage("P_YS_FI_CM_0001123697") /* "】调用生成凭证接口报错" */);
            }
            builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill.getCode() + MessageUtils.getMessage("P_YS_FI_CM_0001123697") /* "】调用生成凭证接口报错" */ + e.getMessage());
            return builder.build();
        }
        if (!generateResult.getBoolean("dealSucceed")) {
            builder.failedId(receiveBillId.toString());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100595"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A5","发送会计平台失败：" +
                        generateResult.get("message")));
            }
            builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                    .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A8","】发送会计平台失败，不能进行结算！") /* "】发送会计平台失败，不能进行结算！" */);
            return builder.build();
        }
        if (generateResult.get("genVoucher") != null && !generateResult.getBoolean("genVoucher")) {
            receiveBill.setVoucherstatus(VoucherStatus.NONCreate);
        }
        journalService.updateJournal(receiveBill);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.ReceivebillMapper.updateReceivebill", receiveBill);
        receiveCallback(receiveBill);
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        ctmcmpBusinessLogService.saveBusinessLog(receiveBill, receiveBill.getCode(), "", IServicecodeConstant.RECEIVEBILL, IMsgConstant.RECEIVE_BILL, IMsgConstant.SETTLE);
        return builder.build();
    }

    private void receiveCallback(ReceiveBill receiveBill) {
        if (receiveBill.getCallback() == null) {
            return;
        }
        CtmJSONObject paramsCallback = new CtmJSONObject();
        paramsCallback.put("pk", receiveBill.getSrcbillid());
        paramsCallback.put("settlestatus", String.valueOf(receiveBill.getSettlestatus().getValue()));
        paramsCallback.put("headvo", CtmJSONObject.toJSONString(receiveBill));
        String callbackUrl = receiveBill.getCallback() + "&token=" + InvocationInfoProxy.getYhtAccessToken();
        String responseStr = HttpTookit.doPostWithJson(callbackUrl, CtmJSONObject.toJSONString(paramsCallback), null);
        CtmJSONObject result = CtmJSONObject.parseObject(responseStr);
        if (!"0".equals(result.getString("code"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100596"),result.getString("msg"));
        }
    }


    /**
     * 收款单-取消结算-单条
     * @param receiveBill   收款单
     * @param batchProcess  是否批量操作
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    @Transactional
    @Override
    public ReceiveBillUnsettleBO processUnsettleItem(ReceiveBill receiveBill, boolean batchProcess, Map<String, Date> maxSettleDateMaps) throws Exception {
        ReceiveBillUnsettleBO.ReceiveBillUnsettleBOBuilder builder = ReceiveBillUnsettleBO.builder().bill(receiveBill);

        Long receiveBillId = receiveBill.getId();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(receiveBillId.toString());
        if (null == ymsLock) {
            builder.failedId(receiveBillId.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill.getCode()
                            + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B0","】已锁定，请稍后重试！") /* "】已锁定，请稍后重试！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100586"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B1","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            return builder.build();
        }
        if (receiveBill.getSettlestatus() != null && receiveBill.getSettlestatus().getValue() == SettleStatus.noSettlement.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B4","】未结算，不能进行取消结算！") /* "】未结算，不能进行取消结算！" */);

            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100597"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B6","该单据未结算，不能进行取消结算！") /* "该单据未结算，不能进行取消结算！" */);
            }
            return builder.build();
        }
        if (receiveBill.getAuditstatus() != null && receiveBill.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B8","】未审批，不能进行取消结算！") /* "】未审批，不能进行取消结算！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100598"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B9","该单据未审批，不能进行取消结算！") /* "该单据未审批，不能进行取消结算！" */);
            }
            return builder.build();
        }
        if (receiveBill.getBilltype() != null && receiveBill.getBilltype().equals(EventType.CashMark)) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E7","单据") /* "单据" */ + receiveBill.getCode() + MessageUtils
                            .getMessage("P_YS_CTM_CM-BE_0001424585") /* "银行对账单拉单生成的单据，不能取消结算" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100599"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E7","单据") /* "单据" */ + receiveBill.getCode() +
                        MessageUtils.getMessage("P_YS_CTM_CM-BE_0001424585") /* "银行对账单拉单生成的单据，不能取消结算" */);
            }
            return builder.build();
        }

        //应收应付单据取消结算校验
        if (receiveBill.getSrcitem() != null && receiveBill.getSrcitem() == EventSource.Manual) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            Boolean settleAccountAlready = RemoteDubbo.get(IArapSettleAccountService.class, "fiarap").isSettleAccountAlready(receiveBill);
            if (!batchProcess && settleAccountAlready) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802BE","该单据已月结，不能进行取消结算！") /* "该单据已月结，不能进行取消结算！" */);
            }
            if (settleAccountAlready) {
                builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802BF","】已月结，不能进行取消结算！") /* "】已月结，不能进行取消结算！" */);
                builder.failedId(receiveBill.getId().toString());
                return builder.build();
            }
        }
        //已日结后不能修改或删除期初数据
        //最大日结日期
        Date maxSettleDate = maxSettleDateMaps.get(receiveBill.getAccentity());
        if(SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, receiveBill.getSettledate())){
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                            .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C2","】已日结，不能取消结算！") /* "】已日结，不能取消结算！" */);
            if (!batchProcess) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C3","该单据已日结，不能取消结算！") /* "该单据已日结，不能取消结算！" */);
            }
            return builder.build();
        }
        if (journalService.checkJournal(receiveBill.getId())) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString());
            builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                    .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C5","】已勾对，不能取消结算！") /* "】已勾对，不能取消结算！" */);
            if (!batchProcess) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A2","该单据已勾对，不能取消结算！") /* "该单据已勾对，不能取消结算！" */);
            }
            return builder.build();
        }
        Boolean matchJournal = journalService.matchJournal(receiveBill.getId());
        if (matchJournal) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString());
            builder.message(receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A6","单据已经关联匹配银行交易回单，不能取消结算") /* "单据已经关联匹配银行交易回单，不能取消结算" */);
            if (!batchProcess) {
                builder.message(receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A6","单据已经关联匹配银行交易回单，不能取消结算") /* "单据已经关联匹配银行交易回单，不能取消结算" */);
            }
            return builder.build();
        }

        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put("id", receiveBill.getId());
        jsonObject.put("billnum", IBillNumConstant.RECEIVE_BILL);
        log.error("=======================================>>>>>测试多线程交易类型错误"+CtmJSONObject.toJSONString(jsonObject));
        boolean checked = cmpVoucherService.isChecked(jsonObject);
        if (checked) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString());
            builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                    .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802AA","】凭证已勾对，不能取消结算！") /* "】凭证已勾对，不能取消结算！" */);
            if (!batchProcess) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802AB","该单据凭证已勾对，不能取消结算！") /* "该单据凭证已勾对，不能取消结算！" */);
            }
            return builder.build();
        }
        boolean match = journalService.matchJournal(receiveBill.getId());
        if (match) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString());
            builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill.getCode() + MessageUtils
                    .getMessage("P_YS_FI_CM_0001155040") /* "】该单据已经进行回单关联，不能取消结算！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100600"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802AE","该单据已经进行回单关联，不能取消结算！") /* "该单据已经进行回单关联，不能取消结算！" */);
            }
            return builder.build();
        }
        receiveBill.set("_entityName", ReceiveBill.ENTITY_NAME);
        try {
            String systemCode = SystemCodeUtil.getSystemCode(receiveBill);
            if(receiveBill.getSrcitem()!= null){
                //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                if(receiveBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)){
                    // 调用应收结算规则
                    Map<String,Object> paramMap = new HashMap<>();
                    paramMap.put("settledate",null);
                    paramMap.put("settlestatus",SettleStatus.noSettlement.getValue());
                    paramMap.put("settlemode",receiveBill.getSettlemode());
                    paramMap.put("enterprisebankaccount",receiveBill.getEnterprisebankaccount());
                    paramMap.put("cashaccount",receiveBill.getCashaccount());
                    paramMap.put("srcbillid",receiveBill.getSrcbillid());
                    paramMap.put("dzdate", null);
                    ytsReceiveBillSettleServiceImpl.unSettleBill(receiveBill,paramMap);
                }
            }
        }catch (Exception e){
            log.error("##   #####   单据取消结算同步应收异常   ##  ######");
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100601"),e.getMessage());
        }
        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(receiveBill);
        if (!deleteResult.getBoolean("dealSucceed")) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            builder.failedId(receiveBill.getId().toString());
            builder.message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802A3","单据【") /* "单据【" */ + receiveBill
                    .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B3","】删除凭证失败，不能取消结算！") /* "】删除凭证失败，不能取消结算！" */);
            if (!batchProcess) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802B5","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult
                                .get("message"));
            }
            return builder.build();
        }

        receiveBill.setSettlestatus(SettleStatus.noSettlement);
        builder.journalList(journalService.updateJournalByBill(receiveBill));

        receiveBill.setSettleuser(null);
        receiveBill.setSettledate(null);
        receiveBill.setDzdate(null);
        // 列表走的新逻辑，异步结算
        receiveBill.setVoucherNo(null);
        receiveBill.setVoucherPeriod(null);
        receiveBill.setVoucherId(null);
        receiveBill.setVoucherstatus(VoucherStatus.Empty);
        receiveBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME,receiveBill);
        receiveCallback(receiveBill);
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        builder.bill(receiveBill);
        ctmcmpBusinessLogService.saveBusinessLog(receiveBill, receiveBill.getCode(), "", IServicecodeConstant.RECEIVEBILL, IMsgConstant.RECEIVE_BILL, IMsgConstant.UNSETTLE);
        return builder.build();
    }
}
