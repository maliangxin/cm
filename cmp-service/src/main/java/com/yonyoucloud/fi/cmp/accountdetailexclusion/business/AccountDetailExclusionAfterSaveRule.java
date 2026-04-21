package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 保存后规则
 *
 * @author jpk
 * @version 1.0
 */
@Slf4j
@Component("accountDetailExclusionAfterSaveRule")
public class AccountDetailExclusionAfterSaveRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100367"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180112", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }
        AccountDetailExclusion accountDetailExclusion = (AccountDetailExclusion) bills.get(0);
        List<AccountDetailExclusion_b> accountDetailExclusion_bs = accountDetailExclusion.AccountDetailExclusion_b();
        if (accountDetailExclusion_bs == null) {
            return new RuleExecuteResult();
        }
        //查所有银行对账单
        Long[] bankreconIds = accountDetailExclusion_bs.stream().map(e -> Long.parseLong(e.get(ICmpConstant.BANK_RECONCILIATION_ID).toString())).toArray(Long[]::new);
        QuerySchema querySchema = QuerySchema.create().addSelect(" id ,bank_seq_no, eliminateStatus");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ID).in(bankreconIds));
        querySchema.addCondition(group);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        // 将列表封装成Map
        Map<Object, BankReconciliation> bankReconciliationMap = bankReconciliations.stream().collect(Collectors.toMap(BankReconciliation::getId, e -> e));
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();

        for (AccountDetailExclusion_b accountDetailExclusion_b : accountDetailExclusion_bs) {
            BankReconciliation bankReconciliation = bankReconciliationMap.get(new Long(accountDetailExclusion_b.getBankReconciliationId()));
            if (Objects.isNull(bankReconciliation)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101173"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800FD", "未查询到银行对账单[%s]，请检查该单据是否已删除") /* "未查询到银行对账单[%s]，请检查该单据是否已删除" */, bankReconciliation.getBank_seq_no()));
            }
            if (EntityStatus.Delete.equals(accountDetailExclusion_b.getEntityStatus())) {
                bankReconciliation.setEliminateStatus(null);
                bankReconciliation.setRemovereasons(null);
                bankReconciliation.setEliminate_amt(null);
                bankReconciliation.setAfter_eliminate_amt(null);
                bankReconciliation.setEliminateReasonType(null);
            } else {
                if (EntityStatus.Insert.equals(accountDetailExclusion_b.getEntityStatus()) && bankReconciliation.getEliminateStatus() != null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101174"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800FE", "银行对账单[%s]已占用,不能剔除") /* "银行对账单[%s]已占用,不能剔除" */, bankReconciliation.getBank_seq_no()));
                }
                bankReconciliation.setEliminateStatus(CullingStatus.Excluding.getValue());
                bankReconciliation.setRemovereasons(accountDetailExclusion_b.getRemovereasons());
                bankReconciliation.setEliminate_amt(accountDetailExclusion_b.getEliminate_amt());
                bankReconciliation.setAfter_eliminate_amt(accountDetailExclusion_b.getAfter_eliminate_amt());
                bankReconciliation.setEliminateReasonType(accountDetailExclusion_b.getEliminateReasonType());
            }
            bankReconciliationList.add(bankReconciliation);
        }
        EntityTool.setUpdateStatus(bankReconciliationList);
        CommonSaveUtils.updateBankReconciliation(bankReconciliationList);
        return new RuleExecuteResult();
    }


}