package com.yonyoucloud.fi.cmp.accounthistorybalance.rule;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Slf4j
@Component("hisbaActAfterPullRule")
public class HisbaActAfterPullRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    /**
     * check_enterpriseBankAccount_name
     * 银行账户参照check后规则
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        BizObject bill = null;
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills.size() > 0) {
            bill = bills.get(0);
        }
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && bill != null) {
            if (bill.get("currency") != null) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bill.get("currency")));
            }
            billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
        }
        if(bill.get("currency") != null){
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bill.get("currency"));
            if (currencyTenantDTO == null) {
                return new RuleExecuteResult();
            }
            bill.set("currency_name", currencyTenantDTO.getName());
            bill.set("currency_priceDigit",currencyTenantDTO.getPricedigit());
            bill.set("currency_moneyDigit",currencyTenantDTO.getMoneydigit());
        }
        return new RuleExecuteResult();
    }
}
