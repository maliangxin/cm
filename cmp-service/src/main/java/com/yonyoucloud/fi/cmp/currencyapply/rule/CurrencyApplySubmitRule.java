package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description: 外币兑换申请单提交规则，未开启审批流则直接审核通过
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/24 16:15
 */

@Slf4j
@Component("currencyApplySubmitRule")
public class CurrencyApplySubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
        if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100634"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001604165") /* "单据已提交，不能进行重复提交！" */);
        }
        if (verifystate == VerifyState.TERMINATED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100635"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001655435") /* "单据已终止流程，不能进行提交！" */);
        }
        if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()|| verifystate == VerifyState.TERMINATED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100130"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187DB75A05B00001","单据状态不是已保存/已驳回状态,不允许提交！") /* "单据状态不是已保存/已驳回状态,不允许提交！" */);
        }

        //未开启审批流则直接审核通过
        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED) ) {
            CurrencyApply currencyApply = MetaDaoHelper.findById(CurrencyApply.ENTITY_NAME, bizObject.getId());
            if (currencyApply == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
            // 未启动审批流，单据直接审批通过
            result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, map);
            result.setCancel(true);
            return result;
        }else {
            short verifyState = ValueUtils.isNotEmptyObj(currentBill.get("verifystate")) ? Short.parseShort(currentBill.get("verifystate").toString()) : (short) -1;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100104"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AA", "流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
            return result;
        }
    }
}
