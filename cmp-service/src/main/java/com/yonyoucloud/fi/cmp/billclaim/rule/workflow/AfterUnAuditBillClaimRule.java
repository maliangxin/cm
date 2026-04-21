package com.yonyoucloud.fi.cmp.billclaim.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.RecheckStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 我的认领弃审后规则
 * @author: zhoulyu@yonyou.com
 * @date: 2024/06/18 15:49
 */

@Slf4j
@Component("afterUnAuditBillClaimRule")
public class AfterUnAuditBillClaimRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject currentBill = MetaDaoHelper.findById(bizObject.getEntityName(), bizObject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102298"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CA", "单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            if (currentBill.get("verifystate") != null) {
                if (VerifyState.INIT_NEW_OPEN.getValue() == currentBill.getShort("verifystate")) {
                    bizObject.set("recheckstatus", RecheckStatus.Saved.getValue());
                    bizObject.set("pubts", null);
                    bizObject.setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(bizObject);
                } else if (VerifyState.SUBMITED.getValue() == currentBill.getShort("verifystate")) {
                    bizObject.set("recheckstatus", RecheckStatus.Submited.getValue());
                    bizObject.set("pubts", null);
                    bizObject.setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(bizObject);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
