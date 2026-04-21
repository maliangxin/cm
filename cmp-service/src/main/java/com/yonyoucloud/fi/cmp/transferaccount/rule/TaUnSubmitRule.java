package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 转账单 撤回后规则 - 31
 */
@Component("taUnSubmitRule")
@RequiredArgsConstructor
public class TaUnSubmitRule extends AbstractCommonRule {

    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private CheckStatusService checkStatusService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        String message = "";
        //校验支付状态为预下单成功/支付成功/线下支付成功/支付中/支付不明时，不允许撤回，给出提示“单据【单据编号】，支付处理中或者已支付完成，不允许撤回”；
        if( currentBill.get("paystatus") != null && (PayStatus.Success.getValue() == currentBill.getShort("paystatus") ||
                PayStatus.OfflinePay.getValue() == currentBill.getShort("paystatus") ||
                PayStatus.Paying.getValue() == currentBill.getShort("paystatus") ||
                PayStatus.PayUnknown.getValue() == currentBill.getShort("paystatus") )){
            message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00040","单据[%s]，支付处理中或者已支付完成，不允许撤回") /* "单据[%s]，支付处理中或者已支付完成，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100403"),message);
        }
        //校验审批流状态为初始开立/驳回到制单时，不允许撤回，给出提示“单据【单据编号】为初始开立/驳回到制单状态，不允许撤回”
        if(currentBill.get("verifystate") != null && (VerifyState.INIT_NEW_OPEN.getValue() == currentBill.getShort("verifystate")
                || VerifyState.REJECTED_TO_MAKEBILL.getValue() == currentBill.getShort("verifystate"))){
            message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00041","单据[%s]，为初始开立/驳回到制单状态，不允许撤回") /* "单据[%s]，为初始开立/驳回到制单状态，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100404"),message);
        }

        Map<String, Object> autoConfigMap = cmCommonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY));
        if (!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")) {
            // 走影像
            BillBiz.executeRule("shareUnSubmit", billContext, map);
        }


        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED) ) {
            // 未启动审批流，单据直接审批拒绝
            result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, map);
            result.setCancel(true);
            return result;
        }else {
            Short auditStatus = Short.parseShort(currentBill.get("auditstatus").toString());
            if (auditStatus != null && auditStatus.equals(AuditStatus.Complete.getValue())) {
                message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00042","单据[%s]已审批，不允许撤回") /* "单据[%s]已审批，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100406"),message);
            }

            // 转账单撤回时，需将支票状态
            if (currentBill.get("checkid") != null) {

                Long transferId = currentBill.get("id");
                TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferId);

                //1。查询现金参数 是否推送结算 是
                Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();

                // 不推结算时，需要回写清空支票的出票信息
                if (!checkFundTransfer) {
                    Long checkId = currentBill.get("checkid");
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                    checkStock.setCheckBillStatus(CmpCheckStatus.Billing.getValue());
                    checkStock.setAmount(null);
                    checkStock.setDrawerDate(null);
                    checkStock.setDrawerName(null);
                    checkStock.setPayeeName(null);
                    checkStock.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }
                //判断是否为银行业务结算
                int settel = cmCommonService.getServiceAttr(transferAccount.getSettlemode());

                int act0 = 0;
                //是否为提取现金银行业务
                boolean check0 = this.check(transferAccount, act0, settel, checkFundTransfer);

                if (check0) {
                    //撤回新旧数据统一处理为开票中
                    Long checkId = currentBill.get("checkid");
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                    // 更新支票状态为“开票中”
                    checkStock.setIsLock(CmpLock.NO.getValue());
                    checkStock.setCheckBillStatus(CmpCheckStatus.Billing.getValue());
                    checkStock.setEntityStatus(EntityStatus.Update);
                    checkStatusService.recordCheckStatusByCheckId(checkStock.getId(),checkStock.getCheckBillStatus());
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }
            }
            return new RuleExecuteResult();
        }
    }

    /**
     * 判断当前转账单业务
     * @param transferAccount 转账单实体
     * @param act 业务类型
     * @return 是否为银行业务或者支票业务
     * @throws Exception
     */
    private boolean check(TransferAccount transferAccount, int act, int settel, boolean checkFundTransfer) throws Exception{
        boolean isCheck = false;
        if (settel == act) {
            isCheck = true;
        }

        //是否为不推资金结算提取现金银行业务结算
        boolean check = ("ec".equals(transferAccount.getType()) || "EC".equals(transferAccount.getTradetype())) && isCheck && !checkFundTransfer;
        return check;
    }
}
