package com.yonyoucloud.fi.cmp.withholdingrulesetting.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.cmpentity.WithholdingRuleStatus;
import com.yonyoucloud.fi.cmp.interestratesetting.InterestRateSetting;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.withholding.InterestCalculation;
import com.yonyoucloud.fi.cmp.withholding.InterestRateSettingHistory;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 预提规则设置删除规则*
 *
 * @author xuxbo
 * @date 2023/5/17 16:54
 */
@Component
public class WithholdingRuleSettingDeleteRule extends AbstractCommonRule {

    //todo 目前值处理单条删除 批量删除后期再说 2023/5/18
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        //1.待设置状态的直接删除
        //2.停用状态的  需要查询预提记录是否为空 为空 可以删除 同时删除子表数据 并且还需要删除银行账户利率设置数据；  如果不为空 则不允许删除
        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isNotEmpty(bills)) {
            WithholdingRuleSetting withholdingRuleSetting = (WithholdingRuleSetting) bills.get(0);
            String id = withholdingRuleSetting.getId().toString();
            WithholdingRuleSetting currentBill = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id);
            if (ValueUtils.isEmpty(currentBill)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102403"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B3", "删除失败，数据不存在！") /* "删除失败，数据不存在！" */));
            }
            //只有当规则设置状态为待设置的时候 才生成第一条利率变更历史记录 同时更新规则设置的状态为启用
            Short ruleStatus = currentBill.getRuleStatus();
            if (ruleStatus.equals(WithholdingRuleStatus.Tobeset.getValue())) {
                //1.待设置状态的直接删除
                return new RuleExecuteResult();
            } else if (ruleStatus.equals(WithholdingRuleStatus.Deactivate.getValue())) {
                //2.停用状态的  需要查询预提记录是否为空 为空 可以删除 同时删除子表数据 并且还需要删除银行账户利率设置数据；  如果不为空 则不允许删除
                //查询当前数据的预提记录是否为空
                Long id1 = currentBill.getId(); //预提规则id
                QuerySchema querySchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                queryConditionGroup.addCondition(QueryCondition.name("withholdingRuleId").eq(id1));
                querySchema.appendQueryCondition(queryConditionGroup);
                List<AccrualsWithholding> accrualsWithholdingList = MetaDaoHelper.queryObject(AccrualsWithholding.ENTITY_NAME, querySchema, null);
                if (accrualsWithholdingList.size() > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102404"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B4", "银行账户已存在预提记录，不允许删除！") /* "银行账户已存在预提记录，不允许删除！" */));
                } else {
//                    MetaDaoHelper.delete();
                    //查询子表数据 两个子表 并删除
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup queryConditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
                    queryConditionGroup1.addCondition(QueryCondition.name("mainid").eq(id1));
                    querySchema1.appendQueryCondition(queryConditionGroup1);
                    List<InterestCalculation> interestCalculationList = MetaDaoHelper.queryObject(InterestCalculation.ENTITY_NAME, querySchema1, null);
                    if (interestCalculationList.size() > 0) {
                        MetaDaoHelper.delete(InterestCalculation.ENTITY_NAME, interestCalculationList);
                    }
                    QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup queryConditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
                    queryConditionGroup2.addCondition(QueryCondition.name("mainid").eq(id1));
                    querySchema2.appendQueryCondition(queryConditionGroup2);
                    List<InterestRateSettingHistory> interestRateSettingHistoryList = MetaDaoHelper.queryObject(InterestRateSettingHistory.ENTITY_NAME, querySchema2, null);
                    if (interestRateSettingHistoryList.size() > 0) {
                        MetaDaoHelper.delete(InterestRateSettingHistory.ENTITY_NAME, interestRateSettingHistoryList);
                    }
                    //查询银行账户利率设置数据 并删除
                    QuerySchema querySchema3 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup queryConditionGroup3 = new QueryConditionGroup(ConditionOperator.and);
                    queryConditionGroup3.addCondition(QueryCondition.name("accountNumberId").eq(id1));
                    querySchema3.appendQueryCondition(queryConditionGroup3);
                    List<InterestRateSetting> interestRateSettingList = MetaDaoHelper.queryObject(InterestRateSetting.ENTITY_NAME, querySchema3, null);
                    if (interestRateSettingList.size() > 0) {
                        MetaDaoHelper.delete(InterestRateSetting.ENTITY_NAME, interestRateSettingList);
                    }
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102403"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B3", "删除失败，数据不存在！") /* "删除失败，数据不存在！" */));
        }

        return new RuleExecuteResult();
    }
}
