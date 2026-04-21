package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 对账单/认领单到外币兑换单，关联对账单和认领单的参照过滤rule
 * @author: wanxbo@yonyou.com
 * @date: 2023/7/18 14:18
 */

@Slf4j
@Component("bankToCurrencyExchangeReferRule")
public class BankToCurrencyExchangeReferRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        //获取单据信息
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills == null || bills.size() == 0){
            return new RuleExecuteResult();
        }
        BizObject bill = bills.get(0);
        //只过滤来源单据为对账单和认领单的数据
        String datasource = bill.getString("datasource");
        //16对账单，80认领单
        if (StringUtils.isEmpty(datasource) || (!"16".equals(datasource) && !"80".equals(datasource))){
            return new RuleExecuteResult();
        }

        if (billDataDto.getCondition() == null) {
            billDataDto.setCondition(new FilterVO());
        }
        //交易类型 0买入外汇 1卖出外汇 2外币兑换
        short flag = bill.getShort("flag");

        //会计主体，币种
        String accentity = bill.getString("accentity");
        //会计主体本位币
        String currency = AccentityUtil.getNatCurrencyIdByAccentityId(accentity);

        //对账单参照
        if ("cmp_bankreconciliationlistRef".equals(billDataDto.getrefCode())){
            if (billDataDto.getData() != null) {
                //会计主体当前会计主体
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", "eq", accentity));
                //关联状态：未关联
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("associationstatus", "eq", "0"));
                //发布状态：否
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("ispublish", "eq", "0"));
                //日记账勾兑状态： 否
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkflag", "eq", "0"));
                //凭证勾兑状态： 否
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("other_checkflag", "eq", "0"));
                //对方账号为空
//                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("to_acct_no", "is_null",null));
            }
            //买入关联对账单
            if ("paybankbill.bank_seq_no".equals(billDataDto.getDatasource())){
                if (Bsflag.Buy.getValue() == flag){ //买入外汇
                    //非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dc_flag", "eq", Direction.Credit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "neq", currency));
                    }
                }else if (Bsflag.Sell.getValue() == flag){ //卖出外汇
                    //本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dc_flag", "eq", Direction.Credit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "eq", currency));
                    }
                }else if (Bsflag.Exchange.getValue() == flag){ //外币兑换
                    //非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dc_flag", "eq", Direction.Credit.getValue()));
                        //不能等于卖出币种
                        if (!StringUtils.isEmpty(bill.getString("sellCurrency"))){
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency,bill.getString("sellCurrency")}));
                        }else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency}));
                        }
                    }
                }
            }

            //卖出关联对账单
            if ("collectbankbill.bank_seq_no".equals(billDataDto.getDatasource())){
                if (Bsflag.Buy.getValue() == flag){ //买入外汇
                    //本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dc_flag", "eq", Direction.Debit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "eq", currency));
                    }
                }else if (Bsflag.Sell.getValue() == flag){ //卖出外汇
                    //为贷时，卖出关联对账单要为借，非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dc_flag", "eq", Direction.Debit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "neq", currency));
                    }
                }else if (Bsflag.Exchange.getValue() == flag){ //外币兑换
                    //为贷时，卖出关联对账单要为借，非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dc_flag", "eq", Direction.Debit.getValue()));
                        //不能等于买入币种
                        if (!StringUtils.isEmpty(bill.getString("purchaseCurrency"))){
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency,bill.getString("purchaseCurrency")}));
                        }else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency}));
                        }
                    }
                }
            }
        }

        //认领单参照
        if ("cmp_mybillclaimlistRef".equals(billDataDto.getrefCode())){
            if (billDataDto.getData() != null) {
                //会计主体当前会计主体
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", "eq", accentity));
                //关联状态：未关联
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("associationstatus", "eq", "0"));
                //对方账号为空
//                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("toaccountno", "is_null",null));
                //认领人为当前登录人
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("claimstaff", "eq",AppContext.getCurrentUser().getName()));
                //认领单状态为认领成功 recheckstatus = 1
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("recheckstatus", "eq", "1"));
            }

            //买入关联认领单
            if ("paybillclaim.code".equals(billDataDto.getDatasource())){
                if (Bsflag.Buy.getValue() == flag){ //买入外汇
                    //非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("direction", "eq", Direction.Credit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "neq", currency));
                    }
                }else if (Bsflag.Sell.getValue() == flag){ //卖出外汇
                    //本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("direction", "eq", Direction.Credit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "eq", currency));
                    }
                }else if (Bsflag.Exchange.getValue() == flag){ //外币兑换
                    //非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("direction", "eq", Direction.Credit.getValue()));
                        //不能等于卖出币种
                        if (!StringUtils.isEmpty(bill.getString("sellCurrency"))){
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency,bill.getString("sellCurrency")}));
                        }else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency}));
                        }
                    }
                }
            }

            //卖出关联认领单
            if ("collectbillclaim.code".equals(billDataDto.getDatasource())){
                if (Bsflag.Buy.getValue() == flag){ //买入外汇
                    //本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("direction", "eq", Direction.Debit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "eq", currency));
                    }
                }else if (Bsflag.Sell.getValue() == flag){ //卖出外汇
                    //非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("direction", "eq", Direction.Debit.getValue()));
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "neq", currency));
                    }
                }else if (Bsflag.Exchange.getValue() == flag){ //外币兑换
                    //非本位币
                    if (billDataDto.getData() != null) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("direction", "eq", Direction.Debit.getValue()));
                        //不能等于买入币种
                        if (!StringUtils.isEmpty(bill.getString("purchaseCurrency"))){
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency,bill.getString("purchaseCurrency")}));
                        }else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "nin", new String[]{currency}));
                        }
                    }
                }
            }
        }

        return new RuleExecuteResult();
    }
}
