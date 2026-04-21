package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description: 外币兑换申请 撤回单据操作
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/24 16:21
 */

@Slf4j
@Component("currencyApplyUnSubmitRule")
public class CurrencyApplyUnSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }

        //校验审批流状态为初始开立/驳回到制单时，不允许撤回，给出提示“单据【单据编号】为初始开立/驳回到制单状态，不允许撤回”
        if(currentBill.get("verifystate") != null && (VerifyState.INIT_NEW_OPEN.getValue() == bizObject.getShort("verifystate")
                || VerifyState.REJECTED_TO_MAKEBILL.getValue() == bizObject.getShort("verifystate"))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100001"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00041","单据[%s]，为初始开立/驳回到制单状态，不允许撤回") /* "单据[%s]，为初始开立/驳回到制单状态，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString()));
        }
        //校验结算状态为结算成功时，不允许撤回，给出提示“单据【单据编号】，结算完成，不允许撤回”；
        if(currentBill.get("deliverystatus") != null && SettleStatus.alreadySettled.getValue() == bizObject.getShort("deliverystatus")){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101337"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003F","单据[%s]，结算完成，不允许撤回") /* "单据[%s]，结算完成，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString()));
        }

        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED) ) {
            // 未启动审批流，单据直接审批拒绝
            result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, map);
            result.setCancel(true);
            return result;
        }else {
            Short auditStatus = Short.parseShort(currentBill.get("auditstatus").toString());
            if (auditStatus != null && auditStatus.equals(AuditStatus.Complete.getValue())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100004"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00042","单据[%s]已审批，不允许撤回") /* "单据[%s]已审批，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString()));
            }
            return new RuleExecuteResult();
        }
    }
}
