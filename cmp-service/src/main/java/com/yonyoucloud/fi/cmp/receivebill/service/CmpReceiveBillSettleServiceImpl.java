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
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.SettleCheckUtil;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
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
public class CmpReceiveBillSettleServiceImpl {

    @Autowired
    private JournalService journalService;
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    private SettlementService settlementService;
    @Autowired
    ReceiveBillSettleServiceImpl receiveBillSettleServiceImpl;
    @Autowired
    YTSReceiveBillSettleServiceImpl ytsReceiveBillSettleServiceImpl;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    BaseRefRpcService baseRefRpcService;



    @Transactional
    public void settle(ReceiveBill bill) throws Exception {
        Long billid = bill.getId();
        String code = bill.getCode();
        Date date = BillInfoUtils.getBusinessDate();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(billid.toString());
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【"
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E3","】已锁定，请稍后重试！") /* "】已锁定，请稍后重试！" */));
        }
        ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, billid, 3);
        if (null == receiveBill) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101688"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0005D", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, code));
        }
        if (receiveBill.getAuditstatus() != null && receiveBill.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【"
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E9","】未审批，不能进行结算！") /* "】未审批，不能进行结算！" */)));
        }
        if (date != null && receiveBill.getAuditDate() != null && date.compareTo(receiveBill.getAuditDate()) < 0) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801EA","】结算日期小于审批日期，不能结算！") /* "】结算日期小于审批日期，不能结算！" */)));
        }
        if (receiveBill.get("enterprisebankaccount") != null && receiveBill.get("cashaccount") != null) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801EC","】银行账户与现金账户不能同时存在！") /* "】银行账户与现金账户不能同时存在！" */)));
        }
        if (receiveBill.get("enterprisebankaccount") == null && receiveBill.get("cashaccount") == null) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801ED","】银行账户与现金账户必须录入其中一个！") /* "】银行账户与现金账户必须录入其中一个！" */)));
        }

        if (receiveBill.get("settlemode") == null) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F0","】结算方式为空！") /* "】结算方式为空！" */)));
        }

        if (receiveBill.getSettlestatus() != null && receiveBill.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F3","】已经结算不能重复结算！") /* "】已经结算不能重复结算！" */)));
        }

        //最大日结日期校验
        Date maxSettleDate = settlementService.getMaxSettleDate(receiveBill.getAccentity());
        if (SettleCheckUtil.checkDailySettlement(maxSettleDate, false)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F4","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate))));
        }
        //结算方式
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setId(receiveBill.get("settlemode"));
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        if (dataList != null && dataList.size() > 0) {
            if (dataList.get(0).getServiceAttr().equals(0)) {
                if (receiveBill.get("enterprisebankaccount") == null) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                            receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F7","】结算方式业务属性为银行业务，请录入银行账户！") /* "】结算方式业务属性为银行业务，请录入银行账户！" */)));
                }
            } else if (dataList.get(0).getServiceAttr().equals(1)) {
                if (receiveBill.get("cashaccount") == null) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                            receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E2","】结算方式业务属性为现金业务，请录入现金账户！") /* "】结算方式业务属性为现金业务，请录入现金账户！" */)));
                }
            } else {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                        receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E6","】结算方式业务属性只能为银行业务与现金业务！") /* "】结算方式业务属性只能为银行业务与现金业务！" */)));
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

        //调用应收应付结算
        settleArap(receiveBill,ymsLock);

        // 凭证处理结果
        CtmJSONObject generateResult = new CtmJSONObject();
        try {
            // 来源单据为空则由现金生成凭证，否则调用应收接口，由应收生成凭证
            receiveBill.set("_entityName", ReceiveBill.ENTITY_NAME);
            receiveBill.set("oldvoucherstatus", receiveBill.getVoucherstatus().getValue());
            generateResult = cmpVoucherService.generateVoucherWithResult(receiveBill);
        } catch (Exception e) {
            log.error("单据【" + code + "】调用生成凭证接口报错：" + e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + MessageUtils.getMessage("P_YS_FI_CM_0001123697") /* "】调用生成凭证接口报错" */ + e.getMessage())));
        }
        if (!generateResult.getBoolean("dealSucceed")) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F2","】发送会计平台失败，不能进行结算！") /* "】发送会计平台失败，不能进行结算！" */));
        }
        if (generateResult.get("genVoucher") != null && !generateResult.getBoolean("genVoucher")) {
            receiveBill.setVoucherstatus(VoucherStatus.NONCreate);
        }

        journalService.updateJournal(receiveBill);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.ReceivebillMapper.updateReceivebill", receiveBill);
        receiveCallback(receiveBill,ymsLock);
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        ctmcmpBusinessLogService.saveBusinessLog(receiveBill, code, "", IServicecodeConstant.RECEIVEBILL, IMsgConstant.RECEIVE_BILL, IMsgConstant.SETTLE);
    }

    public void unsettle(ReceiveBill bill) throws Exception{
        Long billid = bill.getId();
        String code = bill.getCode();
        ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, billid, 3);
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(billid.toString());
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E3","】已锁定，请稍后重试！") /* "】已锁定，请稍后重试！" */));
        }
        if (receiveBill.getSettlestatus() != null && receiveBill.getSettlestatus().getValue() == SettleStatus.noSettlement.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E1","】未结算，不能进行取消结算！") /* "】未结算，不能进行取消结算！" */));

        }
        if (receiveBill.getAuditstatus() != null && receiveBill.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E5","】未审批，不能进行取消结算！") /* "】未审批，不能进行取消结算！" */));
        }
        if (receiveBill.getBilltype() != null && receiveBill.getBilltype().equals(EventType.CashMark)) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100599"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E7","单据" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E8","银行对账单拉单生成的单据，不能取消结算") /* "银行对账单拉单生成的单据，不能取消结算" */));
        }

        //应收应付单据取消结算校验
        if (receiveBill.getSrcitem() != null && receiveBill.getSrcitem() == EventSource.Manual) {
            Boolean settleAccountAlready = RemoteDubbo.get(IArapSettleAccountService.class, "fiarap").isSettleAccountAlready(receiveBill);
            if (settleAccountAlready) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                        receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801EB","】已月结，不能进行取消结算！") /* "】已月结，不能进行取消结算！" */));
            }
        }
        //已日结后不能修改或删除期初数据
        //最大日结日期校验
        Date maxSettleDate = settlementService.getMaxSettleDate(receiveBill.getAccentity());
        if(SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, receiveBill.getSettledate())){
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801EE","】已日结，不能取消结算！") /* "】已日结，不能取消结算！" */));
        }
        if (journalService.checkJournal(receiveBill.getId())) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801EF","】已勾对，不能取消结算！") /* "】已勾对，不能取消结算！" */));
        }
        Boolean matchJournal = journalService.matchJournal(receiveBill.getId());
        if (matchJournal) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101689"),receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F1","单据已经关联匹配银行交易回单，不能取消结算") /* "单据已经关联匹配银行交易回单，不能取消结算" */);
        }


        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put("id", receiveBill.getId());
        jsonObject.put("billnum", IBillNumConstant.RECEIVE_BILL);
        log.error("=======================================>>>>>测试多线程交易类型错误"+CtmJSONObject.toJSONString(jsonObject));
        boolean checked = cmpVoucherService.isChecked(jsonObject);
        if (checked) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F5","】凭证已勾对，不能取消结算！") /* "】凭证已勾对，不能取消结算！" */));
        }
        boolean match = journalService.matchJournal(receiveBill.getId());
        if (match) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801F6","】该单据已经进行回单关联，不能取消结算！") /* "】该单据已经进行回单关联，不能取消结算！" */));
        }
        receiveBill.set("_entityName", ReceiveBill.ENTITY_NAME);

        //调用应收应付取消结算
        unSettleArap(receiveBill,ymsLock);

        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(receiveBill);
        if (!deleteResult.getBoolean("dealSucceed")) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101687"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E0","单据【" +
            receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801E4","】删除凭证失败，不能取消结算！") /* "】删除凭证失败，不能取消结算！" */));
        }

        receiveBill.setSettlestatus(SettleStatus.noSettlement);
        MetaDaoHelper.update(Journal.ENTITY_NAME, journalService.updateJournalByBill(receiveBill));
        receiveBill.setSettleuser(null);
        receiveBill.setSettledate(null);
        receiveBill.setDzdate(null);
        receiveBill.setVoucherstatus(VoucherStatus.Empty);
        receiveBill.setEntityStatus(EntityStatus.Update);
        // 卡片走的旧逻辑
        receiveBill.setVoucherNo(null);
        receiveBill.setVoucherPeriod(null);
        receiveBill.setVoucherId(null);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME,receiveBill);
        receiveCallback(receiveBill,ymsLock);
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        ctmcmpBusinessLogService.saveBusinessLog(receiveBill, receiveBill.getCode(), "", IServicecodeConstant.RECEIVEBILL, IMsgConstant.RECEIVE_BILL, IMsgConstant.UNSETTLE);
    }

    private void unSettleArap(ReceiveBill receiveBill,YmsLock ymsLock){
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101690"),e.getMessage());
        }
    }

    private void settleArap(ReceiveBill receiveBill,YmsLock ymsLock){
        try {
            String systemCode = SystemCodeUtil.getSystemCode(receiveBill);
            if(receiveBill.getSrcitem()!= null){
                //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                if(receiveBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)){
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101691"),e.getMessage());
        }
    }

    private void receiveCallback(ReceiveBill receiveBill,YmsLock ymsLock) {
        if (receiveBill.getCallback() == null) {
            return;
        }
        CtmJSONObject paramsCallback = new CtmJSONObject();
        paramsCallback.put("pk", receiveBill.getSrcbillid());
        paramsCallback.put("settlestatus", String.valueOf(receiveBill.getSettlestatus().getValue()));
        paramsCallback.put("headvo", CtmJSONObject.toJSONString(new CtmJSONObject(receiveBill)));
        String callbackUrl = receiveBill.getCallback() + "&token=" + InvocationInfoProxy.getYhtAccessToken();
        String responseStr = HttpTookit.doPostWithJson(callbackUrl, CtmJSONObject.toJSONString(paramsCallback), null);
        CtmJSONObject result = CtmJSONObject.parseObject(responseStr);
        if (!"0".equals(result.getString("code"))) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101692"),result.getString("msg"));
        }
    }

}
