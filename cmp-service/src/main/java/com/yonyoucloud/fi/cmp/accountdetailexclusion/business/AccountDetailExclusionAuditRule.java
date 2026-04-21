package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
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
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 审核后规则
 *
 * @author jpk
 * @version 1.0
 */
@Slf4j
@Component("accountDetailExclusionAuditRule")
@RequiredArgsConstructor
public class AccountDetailExclusionAuditRule extends AbstractCommonRule {
    @Resource
    private IAccountDetailExclusionService accountDetailExclusionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            AccountDetailExclusion accountDetailExclusion = MetaDaoHelper.findById(AccountDetailExclusion.ENTITY_NAME, bizobject.getId());
            if (accountDetailExclusion == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizobject.get("code")));
            }
            if (bizobject.getPubts() != null) {
                if (!bizobject.getPubts().equals(accountDetailExclusion.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100100"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BE", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            if (null != billContext.getDeleteReason()) {
                if (ICmpConstant.DELETEALL.equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            if (BillInfoUtils.getBusinessDate().compareTo(BillInfoUtils.getBusinessDate()) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100415"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BD", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            //审批通过更新银行对账单剔除状态
            accountDetailExclusionService.updateBankreconciliationExclusion(accountDetailExclusion.getId(), CullingStatus.ExclusionCompleted);

            bizobject.set("documentstatus", CullingStatus.ExclusionCompleted.getValue());
            bizobject.set(ICmpConstant.AUDIT_STATUS, AuditStatus.Complete.getValue());
            // bizobject.set(ICmpConstant.VERIFY_STATE, VerifyState.COMPLETED.getValue());
            bizobject.set(ICmpConstant.AUDITORID, AppContext.getCurrentUser().getId());
            bizobject.set(ICmpConstant.AUDITOR, AppContext.getCurrentUser().getName());
            bizobject.set(ICmpConstant.AUDIT_DATE, new Date());
            bizobject.set(ICmpConstant.AUDIT_TIME, new Date());
        }
        return new RuleExecuteResult();
    }


}
