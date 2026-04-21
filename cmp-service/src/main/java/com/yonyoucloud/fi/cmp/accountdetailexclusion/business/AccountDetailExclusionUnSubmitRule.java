package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description: 撤回单据操作
 * @author: jpk
 * @date: 2023/11/03 16:21
 */

@Slf4j
@Component("accountDetailExclusionUnSubmitRule")
public class AccountDetailExclusionUnSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        bizObject.set("documentstatus", CullingStatus.Excluding.getValue());
        BizObject accountBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == accountBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        short verifystate = Short.parseShort(bizObject.getShort("verifystate").toString());
        if (verifystate == VerifyState.TERMINATED.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102265"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418060E","单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
        }
        if (accountBill.get(ICmpConstant.VERIFY_STATE) != null && (VerifyState.INIT_NEW_OPEN.getValue() == verifystate || VerifyState.REJECTED_TO_MAKEBILL.getValue() == verifystate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101573"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E0","单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
        }
        if (null == accountBill.get(ICmpConstant.IS_WFCONTROLLED) || !accountBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
            // 未启动审批流，单据直接审批拒绝
            result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, map);
            result.setCancel(true);
        } else {
            if (!(verifystate == VerifyState.SUBMITED.getValue())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101574"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DE","单据非审批中，不能进行撤回！") /* "单据非审批中，不能进行撤回！" */);
            }
        }
        return result;
    }
}
