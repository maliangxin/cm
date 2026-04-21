package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckRpcService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpInputBillDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckPurpose;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ICurrencyExchangeNoticeMsgConstant;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CheckOperationType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.SystemType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.BillType;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 转账单，审批前规则 - 29
 */
@Component
public class TransferAuditRule extends AbstractCommonRule {

    @Autowired
    private CtmCmpCheckRpcService ctmCmpCheckRpcService;

    @Autowired
    private AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            Date date = BillInfoUtils.getBusinessDate();
            BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizobject.getId(), 3);
            if(currentBill==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizobject.get("code")));
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100100"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            EventSource eventSource =EventSource.find(currentBill.get(IBillConst.SRCITEM));
            if(currentBill.get(IBillConst.SRCITEM) != null && !EventSource.Cmpchase.equals(eventSource)&&!EventSource.ManualImport.equals(eventSource)&&!EventSource.ThreePartyReconciliation.equals(eventSource)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101281"),MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SCRIME_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006FB", "该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */));
            }
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) == -1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100415"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BD","审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            // 导入历史数据，审批流状态为空，赋默认值初始开立
            if (currentBill.get(ICmpConstant.VERIFY_STATE) == null || ObjectUtils.isEmpty(currentBill.get(ICmpConstant.VERIFY_STATE))) {
                currentBill.put(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());
            }
            bizobject.set("auditstatus", AuditStatus.Complete.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", BillInfoUtils.getBusinessDate());
            bizobject.set("auditTime", new Date());
            Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
            //当不传结算且结算状态为已结算补单的时候，单据审批通过后要给结算日期、结算成功金额赋值，结算日期取单据日期，结算成功金额取转账金额；
            TransferAccount transfer = (TransferAccount) bizobject;
            TransferAccount account = (TransferAccount) currentBill;
            SettleStatus settlestatus = account.getSettlestatus();
            if (!pushSettlement && settlestatus != null && settlestatus.getValue() == SettleStatus.SettledRep.getValue()) {
                transfer.setSettledate(account.getVouchdate());
                transfer.setSettleSuccessAmount(account.getOriSum());
            }

            // 更新支票状态并赋值
            if (currentBill.get("checkid")!= null) {
                Long checkId = currentBill.get("checkid");
                CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                if (checkStock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101282"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BC","支票不存在，请重新选择支票") /* "支票不存在，请重新选择支票" */);
                }
                Long transferId = bizobject.get("id");
                TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferId);
                //1。查询现金参数 是否推送结算 是
                boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();

                // 更新支票状态为“已开票”
                if (currentBill.get("isWfControlled")) {
                    // 走审批流
                } else {
                    if (!checkFundTransfer) {
                    // 不走审批流
                        Date today = new Date();
                        //根据会计主体id 获取会计主体name
                        String accEntityName = (String) QueryBaseDocUtils.queryAccRawEntityByAccEntityId(transferAccount.getAccentity()).get(0).get("name");
                        List<CheckDTO> checkDTOChange = this.setValue(transferAccount, today, accEntityName);
                        ctmCmpCheckRpcService.checkOperation(checkDTOChange);
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

    private List<CheckDTO> setValue(TransferAccount transferAccount, Date today, String accEntityName) {
        List<CheckDTO> checkDTOS = new ArrayList<>();

        CheckDTO checkDTO = new CheckDTO();
        //操作类型： 1.支票锁定/解锁接口、2.支票付票接口、3.支票兑付/背书接口、4.支票作废接口
        checkDTO.setOperationType(CheckOperationType.Pay);
        //锁定类型  锁定状态 1锁定 0解锁
        checkDTO.setLock(CmpLock.YES);
        //支票编号ID
        checkDTO.setCheckBillNo(String.valueOf(transferAccount.getCheckid()));
        //业务系统
        checkDTO.setSystem(SystemType.CashManager);
        //业务单据类型
        checkDTO.setBillType(BillType.TransferAccount);
        //业务单据明细
        checkDTO.setInputBillNo(transferAccount.getCode());
        //单据方向 2：付款 1：收款(按照结算的枚举来)
        checkDTO.setInputBillDir(CmpInputBillDir.Pay);
        //单据方向，字符串类型
        checkDTO.setInputBillDirString(CmpInputBillDir.Pay.getName());
        //支票状态
        checkDTO.setCheckBillStatus(CmpCheckStatus.BillOver.getValue());
        //支票方向
        checkDTO.setCheckBillDir(CheckDirection.Pay.getIndex());
        //支票用途 默认0提现，1转账
        checkDTO.setCheckPurpose(CheckPurpose.VirtualToBank.getValue());
        //结算日期
        checkDTO.setSettlementDate(today);
        //收款人姓名 = 会计主体
        checkDTO.setPayeeName(accEntityName);
        //金额 = 转账金额
        checkDTO.setAmount(transferAccount.getOriSum());
        //出票人员 = 创建人姓名
        checkDTO.setDrawerName(transferAccount.getCreator());
        //出票日期 = 单据日期
        checkDTO.setDrawerDate(transferAccount.getVouchdate());

        checkDTOS.add(checkDTO);
        return checkDTOS;
    }
}
