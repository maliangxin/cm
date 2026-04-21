package com.yonyoucloud.fi.cmp.checkinventory.workflow;


import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import lombok.RequiredArgsConstructor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票盘点撤回前规则</h1>
 *
 * @author zhaorui
 * @version 1.0
 * @since 2023-05-25
 */
@RequiredArgsConstructor
@Component
public class CheckInventoryUnSubmitRule extends AbstractCommonRule {


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CheckInventory checkInventory = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, bizObject.getId(), null);
            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100246"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001604164") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100247"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1487833382166986752") /* "单据已终止流程，不能进行撤回！" */);
            }
            if (verifystate != VerifyState.SUBMITED.getValue() && verifystate != VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100248"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1487833382166986752") /* "单据状态不是审批中/审批通过状态，不允许撤回！" */);
            }
            if (!checkInventory.getIsWfControlled()) {
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("unaudit", billContext, paramMap);
                result.setCancel(true);
            }
        }
        return result;
    }
}
