package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author zhangxiaojun
 * @Date 2022/10/27 17:32
 */

@Slf4j
@Component("newBankreconciliationBeforeDeleteRule")
@RequiredArgsConstructor
public class NewBankreconciliationBeforeDeleteRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BankreconciliationUtils.checkDataLegalList(bills, BankreconciliationActionEnum.DELETE);
        if (bills != null && bills.size()>0) {
            BankReconciliation bankReconciliationReq =  (BankReconciliation)bills.get(0);
            QuerySchema idQuerySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(QueryCondition.name("id").eq(bankReconciliationReq.getId()));
            QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.or);
            group.addCondition(QueryCondition.name("isrepeat").is_null());
            group.addCondition(QueryCondition.name("isrepeat").is_not_null());
            idQuerySchema.appendQueryCondition(group);
            List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,idQuerySchema,null);
            if(CollectionUtils.isEmpty(list)){
                return new RuleExecuteResult();
            }
            BankReconciliation bankReconciliation = list.get(0);
            //在解冻状态的对账单不可删除
            if(bankReconciliation.getFrozenstatus()!=null && bankReconciliation.getFrozenstatus()==1){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102213"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180634","银行交易流水号：") /* "银行交易流水号：" */ + bankReconciliation.getBank_seq_no() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180633","在解冻处理流程中，不能删除！") /* "在解冻处理流程中，不能删除！" */ /* "在解冻处理流程中，不能删除！" */);
            }
            // 银行对账单删除时根据银行账户、交易日期查历史余额，如果已确认则不能删除
            String bankaccount = bankReconciliation.getBankaccount();
            Date tran_date = bankReconciliation.getTran_date();
            if(StringUtils.isNotEmpty(bankaccount) && tran_date != null){
                String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(tran_date);;
                QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                querySchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").eq(bankaccount));
                querySchema.appendQueryCondition(QueryCondition.name("currency").eq(bankReconciliation.getCurrency()));
                querySchema.appendQueryCondition(QueryCondition.name("balancedate").eq(dateStr));
                querySchema.appendQueryCondition(QueryCondition.name("isconfirm").eq(true));
                List<Map<String, Object>> historyBalance = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
                if(CollectionUtils.isNotEmpty(historyBalance)){
                    EnterpriseBankAcctVO enterpriseBankAcctVO= baseRefRpcService.queryEnterpriseBankAccountById(bankaccount);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102214"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1808B3BE04D0000D", "删除失败：银行账户%s交易日期%s的历史余额已经进行确认，不能删除！") /* "删除失败：银行账户%s交易日期%s的历史余额已经进行确认，不能删除！" */, enterpriseBankAcctVO == null ? "" : enterpriseBankAcctVO.getAccount(),dateStr));
                }
            }
        }
        return new RuleExecuteResult();
    }
}
