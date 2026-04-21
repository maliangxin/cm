package com.yonyoucloud.fi.cmp.checkinventory.workflow;


import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票盘点提交规则</h1>
 *
 * @author zhaorui
 * @version 1.0
 * @since 2023-05-24
 */
@Component
public class CheckInventorySubmitRule extends AbstractCommonRule {


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CheckInventory checkInventory = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, bizObject.getId(), 2);
            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102035"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001604165") /* "单据已提交，不能进行重复提交！" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102036"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_CTM_CM-BE_0001604165","单据已提交，不能进行重复提交！") /* "单据已提交，不能进行重复提交！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102037"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001655435") /* "单据已终止流程，不能进行提交！" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102038"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_CTM_CM-BE_0001655435","单据已终止流程，不能进行提交！") /* "单据已终止流程，不能进行提交！" */);
            }
            if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()|| verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100130"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187DB75A05B00001","单据状态不是已保存/已驳回状态,不允许提交！") /* "单据状态不是已保存/已驳回状态,不允许提交！" */);
            }
            if (null != checkInventory && (null == checkInventory.getIsWfControlled() || !checkInventory.getIsWfControlled())) {
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("audit", billContext, paramMap);
                result.setCancel(true);
            }
        }
        return result;
    }
}
