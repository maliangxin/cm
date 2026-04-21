package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.util.QueryReconciliation;
import com.yonyoucloud.fi.cmp.common.CtmException;
import groovy.transform.RecordBase;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
public class BankReconciliationSettingDeleteRule extends AbstractCommonRule {
    @Resource
    @Qualifier("busiBaseDAO")
    private IYmsJdbcApi ymsJdbcApi;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BankReconciliationSetting  bizObject =  (BankReconciliationSetting)bills.get(0);
            Long id = bizObject.get("id");
            Short reconciliationdatasource=bizObject.getReconciliationdatasource().getValue();
            List<BankReconciliationSetting_b> bank_b = bizObject.get("bankReconciliationSetting_b");
            if(id != null){
                CtmJSONObject result = QueryReconciliation.queryReconciliation(bank_b, id,reconciliationdatasource.intValue());
                int state= (int) result.get("state");
                if(state > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100362"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E3","该数据已被对账方案引用，无法删除") /* "该数据已被对账方案引用，无法删除" */);
                }
                deleteOpeningoutstanding(id);
                deleteQcBill(id);
                //校验是否有已生成余额调节表数据
                BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, id,3);
                QuerySchema querySchema = QuerySchema.create().addSelect("id,useorg,useorg.name as useorg_name,bankaccount,bankaccount.account as bankaccount_account,currency,mainid");
                QueryConditionGroup condition = new QueryConditionGroup();
                condition.appendCondition(QueryCondition.name("mainid").eq(bankReconciliationSetting.getId()));
                querySchema.addCondition(condition);
                List<BankReconciliationSetting_b> items = MetaDaoHelper.queryObject(BankReconciliationSetting_b.ENTITY_NAME, querySchema, null);
                for (BankReconciliationSetting_b item : items) {
                    //需要校验是否生成了余额调节表
                    QuerySchema querySchemaCheck = QuerySchema.create().addSelect("id");
                    QueryConditionGroup group = QueryConditionGroup.and(
                            QueryCondition.name("currency").eq(item.getCurrency()),//币种
                            QueryCondition.name("bankaccount").eq(item.getBankaccount()),//银行账号
                            QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId())//对账方案id
                    );
                    querySchemaCheck.addCondition(group);
                    List<BalanceAdjustResult> checkList =  MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchemaCheck, null);
                    if (checkList.size() > 0 ){
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F31530005B8000C", "当前对账方案已生成余额调节表，无法删除") /* "当前对账方案已生成余额调节表，无法删除" */);
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 删除期初未达项
     * @param id
     */
    private void deleteOpeningoutstanding(Long id) throws Exception{
        QuerySchema schema = new QuerySchema().addSelect("id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankreconciliationscheme").eq(id));
        schema.addCondition(conditionGroup);

        List<OpeningOutstanding> openingOutstandings = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema,null);

        MetaDaoHelper.delete(OpeningOutstanding.ENTITY_NAME,openingOutstandings);

    }

    /**
     * 删除期初未达项
     * @param id
     */
    private void deleteQcBill(Long id) throws Exception{
        QuerySchema schema = new QuerySchema().addSelect("id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankreconciliationscheme").eq(id));
        schema.addCondition(conditionGroup);

        List<Journal> journalwds = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, schema,null);

        List<BankReconciliation> bankReconciliationwds = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema,null);

        if(journalwds!=null&&journalwds.size()>0){
            MetaDaoHelper.delete(Journal.ENTITY_NAME,journalwds);
        }

        if(bankReconciliationwds!=null&&bankReconciliationwds.size()>0){
            CommonBankReconciliationProcessor.batchReconciliationBeforeDelete(bankReconciliationwds,ymsJdbcApi);
            MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME,bankReconciliationwds);
        }


    }
}
