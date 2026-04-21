package com.yonyoucloud.fi.cmp.balanceadjustresult.workflow;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceAuditStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @description: 余额调节表撤回前规则
 * @author: zhoulyu@yonyou.com
 * @date: 2025/03/18 15:49
 */

@Slf4j
@Component("beforeUnSubmitBalanceAdjustRule")
public class BeforeUnSubmitBalanceAdjustRule extends AbstractCommonRule {

    @Autowired
    BalanceAdjustResultSerevice balanceAdjustResultSerevice;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069A", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        //校验审批流状态为初始开立/驳回到制单时，不允许撤回，给出提示“单据【单据编号】为初始开立/驳回到制单状态，不允许撤回”
        if (currentBill.get("verifystate") != null && (VerifyState.INIT_NEW_OPEN.getValue() == bizObject.getShort("verifystate")
                || VerifyState.REJECTED_TO_MAKEBILL.getValue() == bizObject.getShort("verifystate") || VerifyState.TERMINATED.getValue() == bizObject.getShort("verifystate"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100001"), String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00041", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400698", "单据[%s]，为初始开立/驳回到制单状态，不允许撤回") /* "单据[%s]，为初始开立/驳回到制单状态，不允许撤回" */) /* "单据[%s]，为初始开立/驳回到制单状态，不允许撤回" */, Objects.isNull(currentBill.get(ICmpConstant.CODE))?"":currentBill.get(ICmpConstant.CODE).toString()));
        }
        //取消审批时校验同一方案、同一组织（对账组织）、银行账号、币种，该余额调节表截止日期之后是否存在“审批”的数据
        BalanceAdjustResult audit = balanceAdjustResultSerevice.getAfterAuditData((BalanceAdjustResult) currentBill);
        if (audit != null) {
            throw new CtmException(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080012", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400699", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */,
                    bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), audit.getDzdate()));
        }
        //审批流传递
        billContext.setCardKey("cmp_balanceadjustresult");
        billContext.setFullname(BalanceAdjustResult.ENTITY_NAME);
        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
            if (Objects.isNull(currentBill.get("auditor"))) {
                bizObject.set("auditstatus", BalanceAuditStatus.Incomplete.getValue());
                bizObject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
                bizObject.set("auditorId", null);
                bizObject.set("auditor", null);
                bizObject.set("auditDate", null);
                bizObject.set("auditTime", null);
                bizObject.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, bizObject);
                result.setCancel(true);
                return result;
            } else {
                // 未启动审批流，单据直接审批拒绝
                result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, map);
                result.setCancel(true);
                return result;
            }
        } else {
            bizObject.set("auditstatus", BalanceAuditStatus.SUBMITED.getValue());
            return new RuleExecuteResult();
        }
    }
}
