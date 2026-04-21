package com.yonyoucloud.fi.cmp.bankbillcheck.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.bankbillcheck.BankBillCheck;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.CheckStatusEnum;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *  撤回前规则
 * @Author zhucongcong
 * @Date 2024/9/25
 */
@Slf4j
@Component("BankBillCheckBeforeUnSubmitRule")
public class BankBillCheckBeforeUnSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("===================================进入银企对账直联确认撤回前规则=============================");
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        if (CollectionUtils.isNotEmpty(bills)) {
            BizObject bizObject = bills.get(0);
            BizObject bankBillCheck = MetaDaoHelper.findById(BankBillCheck.ENTITY_NAME, bizObject.getId());
            if (bankBillCheck == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【%s】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizObject.get("checkBillCode")));
            }
            if (bizObject.getPubts() != null) {
                if (!bizObject.getPubts().equals(bankBillCheck.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100100"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BE", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }

            Short checkStatus = bankBillCheck.getShort("checkStatus");
            Short verifystate = bankBillCheck.getShort("verifystate");
            String checkBillCode = bankBillCheck.getString("checkBillCode");

            if (checkStatus != 0 && checkStatus != 3) {
                //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100101"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003A", "对账单编号【%s】无需对账\\已对账，不允许撤回，请检查！") /* "对账单编号【%s】无需对账\\已对账，不允许撤回，请检查！" */, checkBillCode));
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540061F", "对账单编号【%s】对账状态为%s，不能撤回，请检查！") /* "对账单编号【%s】对账状态为%s，不能撤回，请检查！" */, checkBillCode, CheckStatusEnum.getNameByValue(checkStatus)));
            }
            /*else if (checkResult == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100102"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003C", "对账单编号【%s】尚未确认对账结果，请先确认！") *//* "对账单编号【%s】尚未确认对账结果，请先确认！" *//*, checkBillCode));
            }*/

            if (null == bankBillCheck.get(ICmpConstant.IS_WFCONTROLLED) || !bankBillCheck.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
                // 未启动审批流，审批状态为审批通过才能撤回
                if (verifystate != 2) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100103"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003B", "对账单编号【%s】审批状态不正确，不允许撤回，请检查！") /* "对账单编号【%s】审批状态不正确，不允许撤回，请检查！" */, checkBillCode));
                }
                //单据直接审批拒绝
                result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, paramMap);
                result.setCancel(true);
                return result;
            } else {
                //开启审批流，审批状态为审批中才能撤回
                if (verifystate != 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100103"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003B", "对账单编号【%s】审批状态不正确，不允许撤回，请检查！") /* "对账单编号【%s】审批状态不正确，不允许撤回，请检查！" */, checkBillCode));
                }
                short verifyState = ValueUtils.isNotEmptyObj(bankBillCheck.get("verifystate")) ? Short.parseShort(bankBillCheck.get("verifystate").toString()) : (short) -1;
                if (VerifyState.TERMINATED.getValue() == verifyState) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100104"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AA", "流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
                }
                return result;
            }
        }
        return new RuleExecuteResult();
    }
}
