package com.yonyoucloud.fi.cmp.fundcommon.check;

import cn.hutool.core.date.DateUtil;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金收付款单子表结息账户的check事件</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-05-09 15:18
 */
@Component("fundBillInterestAccountCheckRule")
public class FundBillInterestAccountCheckRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT_NAME.equals(item.get(ICmpConstant.WORD_KEY))) {
            return new RuleExecuteResult();
        }
        Integer location = item.getInteger("location");
        BizObject bill = getBills(billContext, paramMap).get(0);
        String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
        List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
        String billnum = billContext.getBillnum();
        BizObject subObj = linesCheck.get(location);
        Object interestSettlementAccount = subObj.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT);
        // 结息账户切换时
        subObj.put(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE, null);
        subObj.put(ICmpConstant.WITHHOLDING_ORI_SUM, null);
        subObj.put(ICmpConstant.WITHHOLDING_NAT_SUM, null);
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            subObj.put("FundPaymentSubWithholdingRelation_Temp", null);
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            subObj.put("FundCollectionSubWithholdingRelation_Temp", null);
        }
        if (!ValueUtils.isNotEmptyObj(interestSettlementAccount)) {
            subObj.put(ICmpConstant.LAST_INTEREST_SETTLEMENT_END_DATE, null);
            subObj.put(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_START_DATE, null);
            return new RuleExecuteResult();
        }
        // 根据币种和结息账户查询预提规则上的上次结息结束日，
        Object currency = subObj.get(ICmpConstant.CURRENCY);
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.LAST_INTEREST_SETTLEMENT_DATE);
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.BANKACCOUNT).eq(interestSettlementAccount),
                QueryCondition.name(ICmpConstant.CURRENCY).eq(currency));
        querySchema.addCondition(queryConditionGroup);
        List<Map<String, Object>> withholdingRuleSettingList = MetaDaoHelper.query(WithholdingRuleSetting.ENTITY_NAME, querySchema, null);
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(withholdingRuleSettingList)) {
            Map<String, Object> withholdingRuleSettingMap = withholdingRuleSettingList.get(ICmpConstant.CONSTANT_ZERO);
            Date lastInterestSettlementDate = (Date) withholdingRuleSettingMap.get(ICmpConstant.LAST_INTEREST_SETTLEMENT_DATE);
            // 上次结息结束日取预提规则设置上的预提结束日
            subObj.put(ICmpConstant.LAST_INTEREST_SETTLEMENT_END_DATE, lastInterestSettlementDate);
            // 本次结息开始日取预提规则设置上的预提结束日+1
            subObj.put(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_START_DATE, DateUtil.offsetDay(lastInterestSettlementDate, ICmpConstant.CONSTANT_ONE).toJdkDate());
            // 如果是银行对账单推单，本次结息结束日取银行对账单上的交易日期-1
            short billType = Short.parseShort(bill.get(ICmpConstant.BILLTYPE).toString());
            if (billType == EventType.CashMark.getValue()) {
                List<Map<String, Object>> bankReconciliation = MetaDaoHelper.queryById(BankReconciliation.ENTITY_NAME, ICmpConstant.TRAN_DATE, subObj.get(ICmpConstant.BANK_RECONCILIATION_ID));
                Date tranDate = (Date) bankReconciliation.get(ICmpConstant.CONSTANT_ZERO).get(ICmpConstant.TRAN_DATE);
                subObj.put(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE, DateUtil.offsetDay(tranDate, ICmpConstant.MINUS_ONE).toJdkDate());
            }
        }
        return new RuleExecuteResult(paramMap);
    }
}
