package com.yonyoucloud.fi.cmp.balanceadjustresult.workflow;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description: 余额调节表提交前规则
 * @author: zhoulyu@yonyou.com
 * @date: 2025/03/18 15:49
 */

@Slf4j
@Component("beforeSubmitBalanceAdjustRule")
public class BeforeSubmitBalanceAdjustRule extends AbstractCommonRule {


    @Autowired
    BalanceAdjustResultSerevice balanceAdjustResultSerevice;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bill = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bill.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        short verifyState = ValueUtils.isNotEmptyObj(currentBill.get("verifyState")) ? Short.parseShort(currentBill.get("verifyState").toString()) : (short) -1;
        if (verifyState == VerifyState.SUBMITED.getValue() || verifyState == VerifyState.COMPLETED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100128"), MessageUtils.getMessage("P_YS_CTM_CM-BE_0001604165") /* "单据已提交，不能进行重复提交！" */);
        }
        if (verifyState == VerifyState.TERMINATED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100129"), MessageUtils.getMessage("P_YS_CTM_CM-BE_0001655435") /* "单据已终止流程，不能进行提交！" */);
        }
        if (verifyState == VerifyState.SUBMITED.getValue() || verifyState == VerifyState.COMPLETED.getValue() || verifyState == VerifyState.TERMINATED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100130"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187DB75A05B00001", "单据状态不是已保存/已驳回状态,不允许提交！") /* "单据状态不是已保存/已驳回状态,不允许提交！" */);
        }

        balanceAdjustResultSerevice.beforeSubmitCheck((BalanceAdjustResult) currentBill);
        //审批流传递
        billContext.setCardKey("cmp_balanceadjustresult");
        billContext.setFullname(BalanceAdjustResult.ENTITY_NAME);
        //未开启审批流则直接审核通过
        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
            // 未启动审批流，单据直接审批通过
            result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, map);
            result.setCancel(true);
            return result;
        } else {
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100104"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AA", "流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
            return result;
        }
    }
}
