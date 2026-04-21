package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.service.IAccountDetailExclusionService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 弃审后规则
 *
 * @author jpk
 * @version 1.0
 */
@Slf4j
@Component("accountDetailExclusionUnAuditRule")
@RequiredArgsConstructor
public class AccountDetailExclusionUnAuditRule extends AbstractCommonRule {
    @Resource
    private IAccountDetailExclusionService accountDetailExclusionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            AccountDetailExclusion accountDetailExclusion = MetaDaoHelper.findById(AccountDetailExclusion.ENTITY_NAME, bizobject.getId(),2);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            if (accountDetailExclusion == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizobject.get("code")));
            }
            if (accountDetailExclusion.getAuditstatus() != null && AuditStatus.Incomplete.getValue() == accountDetailExclusion.getAuditstatus()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100063"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003B", "单据[%s]未审批不能撤回") /* "单据[%s]未审批不能撤回" */, accountDetailExclusion.get(ICmpConstant.CODE).toString()));
            }
            //审批通过更新银行对账单剔除状态
            accountDetailExclusionService.updateBankreconciliationExclusion(accountDetailExclusion.getId(), CullingStatus.Excluding);
            if (null != accountDetailExclusion.get(ICmpConstant.IS_WFCONTROLLED) && accountDetailExclusion.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
                accountDetailExclusion.setDocumentstatus(CullingStatus.Excluding.getValue());
                accountDetailExclusion.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(AccountDetailExclusion.ENTITY_NAME, accountDetailExclusion);
            }
            bizobject.set("documentstatus", CullingStatus.Excluding.getValue());
            bizobject.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());
            bizobject.set(ICmpConstant.AUDIT_STATUS, AuditStatus.Incomplete.getValue());
            bizobject.set(ICmpConstant.AUDITORID, AppContext.getCurrentUser().getId());
            bizobject.set(ICmpConstant.AUDITOR, AppContext.getCurrentUser().getName());
            bizobject.set(ICmpConstant.AUDIT_DATE, new Date());
            bizobject.set(ICmpConstant.AUDIT_TIME, new Date());
        }
        return new RuleExecuteResult();
    }


}
