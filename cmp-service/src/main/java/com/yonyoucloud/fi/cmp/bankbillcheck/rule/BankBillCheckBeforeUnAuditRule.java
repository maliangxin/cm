package com.yonyoucloud.fi.cmp.bankbillcheck.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.bankbillcheck.BankBillCheck;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.enums.CheckStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Author zhucongcong
 * @Date 2024/12/6
 */
@Component("BankBillCheckBeforeUnAuditRule")
@Slf4j
public class BankBillCheckBeforeUnAuditRule extends AbstractCommonRule {


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("=================================进入银企对账直联确认审批撤回前规则=================================");
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isNotEmpty(bills)) {
            BizObject bizObject = bills.get(0);
            BizObject bankBillCheck = MetaDaoHelper.findById(BankBillCheck.ENTITY_NAME, bizObject.getId());
            if (bankBillCheck == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【%s】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizObject.get("checkBillCode")));
            }
            if (bizObject.getPubts() != null) {
                if (!bizObject.getPubts().equals(bankBillCheck.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100100"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BE", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            Short checkStatus = bankBillCheck.getShort("checkStatus");
            String checkBillCode = bankBillCheck.getString("checkBillCode");

            if (checkStatus != 0 && checkStatus != 3) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400479", "对账单编号【%s】对账状态为%s，不能撤回，请检查！") /* "对账单编号【%s】对账状态为%s，不能撤回，请检查！" */, checkBillCode, CheckStatusEnum.getNameByValue(checkStatus)));
            }

            bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizObject.set("auditorId", null);
            bizObject.set("auditor", null);
            bizObject.set("auditDate", null);
            bizObject.set("auditTime", null);
        }
        return new RuleExecuteResult();
    }
}
