package com.yonyoucloud.fi.cmp.balanceadjustresult.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceAuditStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 余额调节表提交后规则
 * @author: zhoulyu@yonyou.com
 * @date: 2025/03/18 15:49
 */

@Slf4j
@Component("afterSubmitBalanceAdjustRule")
public class AfterSubmitBalanceAdjustRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject currentBill = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bizObject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100254"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CA", "单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
                bizObject.set("auditstatus", BalanceAuditStatus.Complete.getValue());
            } else {
                bizObject.set("auditstatus", BalanceAuditStatus.SUBMITED.getValue());
            }
            bizObject.set("pubts", null);
            bizObject.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, bizObject);
        }
        return new RuleExecuteResult();
    }
}
