package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.AssertUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 删除前规则
 *
 * @author jpk
 * @version 1.0
 */
@Slf4j
@Component("accountDetailExclusionBeforDeleteRule")
@RequiredArgsConstructor
public class AccountDetailExclusionBeforDeleteRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100367"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180112", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }
        for (BizObject bizObject : bills) {
            if (IBillNumConstant.CMP_ACCOUNT_DETAIL_EXCLUSION.equals(billnum) || IBillNumConstant.CMP_ACCOUNT_DETAIL_EXCLUSIONLIST.equals(billnum)) {
                AccountDetailExclusion accountDetailExclusion = MetaDaoHelper.findById(AccountDetailExclusion.ENTITY_NAME, bizObject.getId(), 3);
                AssertUtils.isTrue(Objects.isNull(accountDetailExclusion), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400395", "账户收支明细剔除已被删除，请刷新页面！") /* "账户收支明细剔除已被删除，请刷新页面！" */);
                if (CullingStatus.Excluding.getValue() != accountDetailExclusion.getDocumentstatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102612"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080112", "单据【%s】状态不是剔除中，不能删除此单据") /* "单据【%s】状态不是剔除中，不能删除此单据" */, bizObject.get(ICmpConstant.CODE).toString()));
                }
                if (accountDetailExclusion.getVerifystate() != null && (VerifyState.INIT_NEW_OPEN.getValue() != accountDetailExclusion.getVerifystate() && VerifyState.REJECTED_TO_MAKEBILL.getValue() != accountDetailExclusion.getVerifystate())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102613"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080113", "单据【%s】审批中，不能删除此单据") /* "单据【%s】审批中，不能删除此单据" */, bizObject.get(ICmpConstant.CODE).toString()));
                }
                List<AccountDetailExclusion_b> accountDetailExclusion_bs = accountDetailExclusion.AccountDetailExclusion_b();
                // 单据删除时，更新银行对账单的剔除状态
                if (CollectionUtils.isNotEmpty(accountDetailExclusion_bs)) {
                    Long[] bankreconIds = accountDetailExclusion_bs.stream().map(e -> Long.parseLong(e.get(ICmpConstant.BANK_RECONCILIATION_ID).toString())).toArray(Long[]::new);
                    QuerySchema bankschema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup condition = new QueryConditionGroup();
                    condition.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ID).in(bankreconIds)));
                    bankschema.addCondition(condition);
                    List<BankReconciliation> bankInfos = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, bankschema, null);
                    bankInfos.stream().forEach(e -> {
                        e.setEliminateStatus(null);
                        e.setRemovereasons(null);
                        e.setEliminate_amt(null);
                        e.setAfter_eliminate_amt(null);
                        e.setEliminateReasonType(null);
                    });
                    EntityTool.setUpdateStatus(bankInfos);
                    CommonSaveUtils.updateBankReconciliation(bankInfos, null);
                }

            }
        }
        return new RuleExecuteResult();
    }


}
