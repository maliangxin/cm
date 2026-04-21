package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * desc:银行对账单生单资金收付款单设置对账单的入账类型
 * 没有勾选提前入账直接生单  ----》 正常
 * 勾选提前入账，入账次数为null或0，说明是第一次-----》挂账
 * 勾选提前入账，入账次数为1，说明是第二次---》冲挂账
 * author:wangqiangac
 * date:2023/9/13 16:40
 */
@Slf4j
@Component
public class BankSetEntrytypePullBeforeRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<? extends BizObject> bankReconciliations = (List<? extends BizObject>) paramMap.get("sourceDatas");
        if (bankReconciliations == null) {
            return new RuleExecuteResult();
        }
        //data.put("isautopull",true);//BankSetEntrytypePullBeforeRule类处理入账类型自动生单无需处理，在此做标记用于规则类直接return
        if (bankReconciliations.size() > 0) {
            BizObject bankRec = bankReconciliations.get(0);
            String isAutoPull = bankRec.getString("isAutoPull");
            if ("auto".equals(isAutoPull)) {
                return new RuleExecuteResult();
            }
        }
        String targetUri = (String) paramMap.get("fullNameTar");
        Map params = new HashMap();
        List<BankReconciliation> bankreconciliations = getBankReconciliations(bankReconciliations);
        Short virtualentrytype;
        Boolean isadvanceaccounts;
        if (FundPayment.ENTITY_NAME.equals(targetUri) || FundCollection.ENTITY_NAME.equals(targetUri)) {
            Set<Short> set = bankreconciliations.stream().map(e -> e.getAssociationcount()).collect(Collectors.toSet());//业务关联次数
            isadvanceaccounts = bankReconciliations.get(0).get("isadvanceaccounts");
            Integer associationcount = set.iterator().next() == null ? 0 : Integer.parseInt(String.valueOf(set.iterator().next()));
            if (isadvanceaccounts) {
                if (associationcount == 0) {
                    virtualentrytype = EntryType.Hang_Entry.getValue();
                    bankReconciliations.stream().forEach(e -> e.put("virtualentrytype", EntryType.Hang_Entry.getValue()));
                } else {
                    virtualentrytype = EntryType.CrushHang_Entry.getValue();
                    bankReconciliations.stream().forEach(e -> e.put("virtualentrytype", EntryType.CrushHang_Entry.getValue()));
                }
            } else {
                virtualentrytype = EntryType.Normal_Entry.getValue();
                bankReconciliations.stream().forEach(e -> e.put("virtualentrytype", EntryType.Normal_Entry.getValue()));
            }
            for (BizObject bankReconciliation : bankreconciliations) {
                try {
                    params.put("id", bankReconciliation.get("id"));
//                params.put("isadvanceaccounts", isadvanceaccounts);
                    params.put("virtualentrytype", virtualentrytype);
                    SqlHelper.update("com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper.updateVirtualentrytype", params);
                } catch (Exception e) {
//                    throw new CtmException(String.format("当前流水【%s】正在处理中，建议稍后重试！", bankReconciliation.getString("bank_seq_no")));
                }
            }
        } else {
            //第一次提前入账只能生成收付款单，之后做第二次可以生成其他类型，提前入账肯定为是 然后赋值为冲挂账
            //如果是正常生单，入账类型为正常入账
            isadvanceaccounts = bankReconciliations.get(0).get("isadvanceaccounts");
            if (isadvanceaccounts) {
                virtualentrytype = EntryType.CrushHang_Entry.getValue();
                bankReconciliations.stream().forEach(e -> {
                    e.put("virtualentrytype", virtualentrytype);
                    e.put("isadvanceaccounts", isadvanceaccounts);
                });
            } else {
                virtualentrytype = EntryType.Normal_Entry.getValue();
                bankReconciliations.stream().forEach(e -> {
                    e.put("virtualentrytype", virtualentrytype);
                    e.put("isadvanceaccounts", isadvanceaccounts);
                });
            }

            for (BizObject bankReconciliation : bankreconciliations) {
                try {
                    params.put("id", bankReconciliation.get("id"));
//                params.put("isadvanceaccounts", isadvanceaccounts);
                    params.put("virtualentrytype", virtualentrytype);
                    SqlHelper.update("com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper.updateVirtualentrytype", params);
                } catch (Exception e) {
//                    throw new CtmException(String.format("当前流水【%s】正在处理中，建议稍后重试！", bankReconciliation.getString("bank_seq_no")));
                }
            }


        }
        return new RuleExecuteResult();
    }

    /**
     * 查询银行对账单
     *
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    private static List<BankReconciliation> getBankReconciliations(List<? extends BizObject> bankReconciliations) throws Exception {
        List<String> ids = bankReconciliations.stream().map(e -> e.getString("id")).collect(Collectors.toList());
        QuerySchema schema = QuerySchema.create().addSelect(" * ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        List<BankReconciliation> bankre = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        return bankre;
    }
}