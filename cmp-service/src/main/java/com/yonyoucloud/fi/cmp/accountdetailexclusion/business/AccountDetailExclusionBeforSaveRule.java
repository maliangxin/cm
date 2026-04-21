package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 保存前规则
 *
 * @author jpk
 * @version 1.0
 */
@Slf4j
@Component("accountDetailExclusionBeforSaveRule")
@RequiredArgsConstructor
public class AccountDetailExclusionBeforSaveRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100367"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180112", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject bizobject = bills.get(0);
            bizobject.set("documentstatus", CullingStatus.Excluding.getValue());
            bizobject.set(ICmpConstant.AUDITORID, null);
            bizobject.set(ICmpConstant.AUDITOR, null);
            bizobject.set(ICmpConstant.AUDIT_DATE, null);
            bizobject.set(ICmpConstant.AUDIT_TIME, null);
        }
        return new RuleExecuteResult();
    }


}
