package com.yonyoucloud.fi.cmp.modifyschema;

import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.plugin.base.QuerySchemaExecutorPlugin;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.CtmDealDetailCheckMayRepeatUtils;
import org.imeta.core.model.Entity;
import org.imeta.orm.schema.ConditionExpression;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author guoyangy
 * @Date 2024/5/9 14:12
 * @Description https://gfwiki.yyrd.com/x/S_OGAg
 * @Version 1.0
 */
@Component
public class CmpQuerySchemaExecutorPlugin implements QuerySchemaExecutorPlugin {
    @Override
    public void extendQuerySchema(Entity entity, QuerySchema schema, BillContext billContext) {

        String refCode =  billContext.getEntityCode();
        if("cmp_exchangesettlement_tradecode_ref".equals(refCode) || "cmp_sourcecodeRef".equals(refCode) || "cmp_exchangesettlement_purpose_ref".equals(refCode)
                ||("detail".equals(billContext.getAction()) && "cmp_foreignpayment".equals(billContext.getEntityCode())&&"cmp_foreignpayment".equals(billContext.getBillnum()))){
            schema.setPartitionable(false);
        }
        if("cmp_exchangesettlement_tradecode_ref".equals(refCode) || "cmp_sourcecodeRef".equals(refCode) || "cmp_exchangesettlement_purpose_ref".equals(refCode)
                ||("query".equals(billContext.getAction()) && "cmp_foreignpaymentlist".equals(billContext.getEntityCode())&&"cmp_foreignpaymentlist".equals(billContext.getBillnum()))){
            schema.setPartitionable(false);
        }
        // 外币兑换单，交易编码,来源代码,用途代码
        if("cmp_exchangesettlement_tradecode_ref".equals(refCode) || "cmp_sourcecodeRef".equals(refCode) || "cmp_exchangesettlement_purpose_ref".equals(refCode)
                ||("detail".equals(billContext.getAction()) && "cmp_currencyexchange".equals(billContext.getEntityCode())&&"cmp_currencyexchange".equals(billContext.getBillnum()))){
            schema.setPartitionable(false);
        }
        // 货币兑换申请，交易编码,来源代码,用途代码
        if("cmp_exchangesettlement_tradecode_ref".equals(refCode) || "cmp_sourcecodeRef".equals(refCode) || "cmp_exchangesettlement_purpose_ref".equals(refCode)
                ||("detail".equals(billContext.getAction()) && "cmp_currencyapply".equals(billContext.getEntityCode())&&"cmp_currencyapply".equals(billContext.getBillnum()))){
            schema.setPartitionable(false);
        }
        //导入翻译时，结售汇相关字段去除租户数据隔离
        if ("cmp.exchangesourcecode.ExchangeSourceCode".equals(billContext.getFullname()) || "cmp.exchangesettlementpurpose.ExchangeSettlementPurpose".equals(billContext.getFullname())
        || "cmp.exchangesettlementtradecode.ExchangeSettlementTradeCode".equals(billContext.getFullname())) {
            schema.setPartitionable(false);
        }
        // 疑重开启：对账单数据业务查询默认不需要疑重的数据，增加【正常】【确认正常】的条件过滤; 如果指定了疑重状态，按照疑重状态处理
        if (CtmDealDetailCheckMayRepeatUtils.isRepeatCheck && schema.queryConditionGroup()!=null &&
        "cmp.bankreconciliation.BankReconciliation".equals(billContext.getFullname())
                // 查detail不需要增加处理
                && !"detail".equals(billContext.getAction())
                && "cmp.bankreconciliation.BankReconciliation".equals(entity.fullname())
                && !schema.queryConditionGroup().conditions().stream().anyMatch(i->i.toString().contains("isrepeat"))) {
            QueryConditionGroup or = QueryConditionGroup.or(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_INIT, BankDealDetailConst.REPEAT_NORMAL), QueryCondition.name("isrepeat").is_null());
            // 对于外部增加的条件通过and关联，处理or的场景
            QueryConditionGroup conditionGroup = QueryConditionGroup.and(or,schema.queryConditionGroup());
            schema.queryConditionGroup(conditionGroup);
        }

        // CZFW-380158 mdd8.4升级后，余额调节表详情查询schema多出来bankaccount等字段，导致查询不到数据，这里去掉
        if (("cmp.balanceadjustresult.BalanceadjustBankreconciliation".equals(billContext.getFullname()) || "cmp.balanceadjustresult.BalanceadjustJournal".equals(billContext.getFullname()))
                && ("cmp_balanceadjustjournal".equals(billContext.getBillnum()) || "cmp_balanceadjustbankreconciliation".equals(billContext.getBillnum()))) {
            if (schema.queryConditionGroup()  != null && schema.queryConditionGroup().conditions() !=null & schema.queryConditionGroup().conditions().size() > 1){
                handleSchema(schema, "bankaccount");
            }
        }
    }

    @Override
    public int order() {
        return 10;
    }

    /**
     * 具体处理所需排除的查询条件
     * @param schema
     * @param fieldName
     */
    private static void handleSchema(QuerySchema schema, String fieldName) {
        QueryConditionGroup group = Optional.ofNullable(schema.queryConditionGroup()).orElse(new QueryConditionGroup());
        List<ConditionExpression> conditionExpressionList = Optional.ofNullable(group.conditions()).orElseGet(ArrayList::new);
        ConditionExpression expression = conditionExpressionList.stream().filter(e -> e.toString().contains(fieldName)).findAny().orElse(null);
        conditionExpressionList.remove(expression);
        group.addConditions(conditionExpressionList);
        schema.queryConditionGroup(group);
    }
}
