package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * @description: 提交规则，未开启审批流则直接审核通过
 * @author: jpk
 * @date: 2023/11/03 16:21
 */

@Slf4j
@Component("accountDetailExclusionSubmitRule")
public class AccountDetailExclusionSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        AccountDetailExclusion accountDetailExclusion = MetaDaoHelper.findById(AccountDetailExclusion.ENTITY_NAME, bizObject.getId(), 3);
        if (null == accountDetailExclusion) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        short verifystate = Short.parseShort(bizObject.get(ICmpConstant.VERIFY_STATE).toString());
        if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
        }
        if (verifystate == VerifyState.TERMINATED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100758"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00140", "单据已终止流程，不能进行提交！") /* "单据已终止流程，不能进行提交！" */);
        }
        //未开启审批流则直接审核通过
        if (null == accountDetailExclusion.getIsWfControlled() || !accountDetailExclusion.getIsWfControlled()) {
            // 未启动审批流，单据直接审批通过
            result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, map);
            result.setCancel(true);
        } else {
            short verifyState = ValueUtils.isNotEmptyObj(accountDetailExclusion.getVerifystate()) ? Short.parseShort(accountDetailExclusion.getVerifystate().toString()) : (short) -1;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100104"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AA", "流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
        }
         return result;
    }
}
