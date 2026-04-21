package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 外币兑换申请参照过滤规则
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/16 15:21
 */
@Slf4j
@Component("currencyApplyReferDataRule")
public class CurrencyApplyReferDataRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        FilterVO conditionVO = bill.getCondition();

        if (bill.getBillnum().equals("cmp_currencyapply") && bill.getrefCode().equals("transtype.bd_billtyperef")) {
            //外币兑换申请，交易类型过滤
            if (bill.getData() != null) {
                CurrencyApply currencyApply = null;
                List<CurrencyApply> list = (List<CurrencyApply>) bill.getData();
                if (list != null && list.size() > 0) {
                    currencyApply = list.get(0);
                    String accEntityId = currencyApply.getAccentity();
                    if (accEntityId == null) {
                        return new RuleExecuteResult();
                    }
                    if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {//卖出外汇
                        MddBaseUtils.appendCondition(conditionVO, "code", "eq", "APPLY_SFE");
                    } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {//买入外汇
                        MddBaseUtils.appendCondition(conditionVO, "code", "eq", "APPLY_BFE");
                    } else {
                        MddBaseUtils.appendCondition(conditionVO, "code", "eq", "APPLY_BSFE");
                    }
                }
            }
            MddBaseUtils.appendCondition(conditionVO, "billtype_id", "eq", "1794911500749504513");
        }else if (bill.getBillnum().equals("cmp_currencyapply") && bill.getrefCode().equals(IRefCodeConstant.REF_SETTLEMENT)) {
            FilterVO conditon = bill.getCondition();
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0, 1}));
            bill.setTreeCondition(conditon);
        }else if (null != bill.getKey() && bill.getKey().equals("purchaseCurrency_name") && bill.getrefCode().equals("ucfbasedoc.bd_currencytenantref")) {
            if (bill.getData() != null) {
                CurrencyApply currencyApply = null;
                List<CurrencyApply> list = (List<CurrencyApply>) bill.getData();
                if (list != null) {
                    currencyApply = list.get(0);
                }
                String accEntityId = currencyApply.getAccentity();
                if (accEntityId == null) {
                    return new RuleExecuteResult();
                }
                List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
                String regionCurrency = currencyApply.getRegionCurrency();
                if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())){
                    if (StringUtils.isNotEmpty(regionCurrency)){
                        MddBaseUtils.appendCondition(conditionVO, "id", "neq", regionCurrency);
                    }
                }else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())){
                    MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
                }
                /*if (currencyApply.get("flag").equals(Bsflag.Buy.getValue()) || currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {//卖出通道
                    MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
                }*/
                if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue()) && currencyApply.get("sellCurrency") != null) {//卖出通道
                    MddBaseUtils.appendCondition(conditionVO, "id", "neq", currencyApply.get("sellCurrency"));
                }
            }
        } else if (null != bill.getKey() && bill.getKey().equals("sellCurrency_name") && bill.getrefCode().equals("ucfbasedoc.bd_currencytenantref")) {
            if (bill.getData() != null) {
                CurrencyApply currencyApply = null;
                List<CurrencyApply> list = (List<CurrencyApply>) bill.getData();
                if (list != null) {
                    currencyApply = list.get(0);
                }
                String accEntityId = currencyApply.getAccentity();
                if (accEntityId == null) {
                    return new RuleExecuteResult();
                }
                List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
                String regionCurrency = currencyApply.getRegionCurrency();
                if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())){
                    if (StringUtils.isNotEmpty(regionCurrency)){
                        MddBaseUtils.appendCondition(conditionVO, "id", "neq", regionCurrency);
                    }
                }else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())){
                    MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
                }
                /*if (currencyApply.get("flag").equals(Bsflag.Sell.getValue()) || currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {//卖出通道
                    MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
                }*/
                if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue()) && currencyApply.get("purchaseCurrency") != null) {//卖出通道
                    MddBaseUtils.appendCondition(conditionVO, "id", "neq", currencyApply.get("purchaseCurrency"));
                }
            }
        } else if (null != bill.getKey() && (bill.getKey().equals("purchasebankaccount_name") || bill.getKey().equals("sellbankaccount_name")) && bill.getrefCode().contains("bd_enterprisebankacct")) {
            if (bill.getData() != null) {
                CurrencyApply currencyApply = null;
                List<CurrencyApply> list = (List<CurrencyApply>) bill.getData();
                if (list != null) {
                    currencyApply = list.get(0);
                    String accEntityId = currencyApply.getAccentity();
                    if (accEntityId == null) {
                        return new RuleExecuteResult();
                    }
                    List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
                    String regionCurrency = currencyApply.getRegionCurrency();
                    if (StringUtils.isNotEmpty(regionCurrency)){
                        if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {//卖出通道
                            if (bill.getKey().equals("purchasebankaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", regionCurrency);
                            } else {
                                UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getSellCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", regionCurrency);
                                }
                            }
                        } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {//买入通道
                            if (bill.getKey().equals("sellbankaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", regionCurrency);
                            } else {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getPurchaseCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getPurchaseCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", regionCurrency);
                                }
                            }
                        } else{//外币兑换
                            MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
                            if (bill.getKey().equals("purchasebankaccount_name")){
                                if (currencyApply.getPurchaseCurrency()!=null){
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getPurchaseCurrency());
                                }else if (currencyApply.getSellCurrency()!=null){
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyApply.getSellCurrency());
                                }
                            }else if(bill.getKey().equals("sellbankaccount_name")){
                                if (currencyApply.getSellCurrency()!=null){
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getSellCurrency());
                                }else if (StringUtils.isNotBlank(currencyApply.getPurchaseCurrency())){
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyApply.getPurchaseCurrency());
                                }
                            }
                        }
                    }else {
                        if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {//卖出通道
                            if (bill.getKey().equals("purchasebankaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", accEntity.get(0).get("currency"));
                            } else {
                                UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getSellCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
                                }
                            }
                        } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {//买入通道
                            if (bill.getKey().equals("sellbankaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", accEntity.get(0).get("currency"));
                            } else {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getPurchaseCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getPurchaseCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
                                }
                            }
                        } else {//外币兑换
                            MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
                            if (bill.getKey().equals("purchasebankaccount_name")) {
                                if (currencyApply.getPurchaseCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getPurchaseCurrency());
                                } else if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyApply.getSellCurrency());
                                }
                            } else if (bill.getKey().equals("sellbankaccount_name")) {
                                if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyApply.getSellCurrency());
                                } else if (StringUtils.isNotBlank(currencyApply.getPurchaseCurrency())) {
                                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyApply.getPurchaseCurrency());
                                }
                            }
                        }
                    }
                    // 多币种添加币种启用过滤
                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_ENABLE_REf, "eq", 1);
                    //0为银行开户;2财务公司
                    MddBaseUtils.appendCondition(conditionVO, "acctopentype", "in", new String[]{"0", "2"});
                }
            }
        } else if (null != bill.getKey() && (bill.getKey().equals("purchasecashaccount_name") || bill.getKey().equals("sellcashaccount_name")) && bill.getrefCode().equals("bd_enterprisecashacctref") && bill.getBillnum().equals("cmp_currencyapply")) {
            if (bill.getData() != null) {
                CurrencyApply currencyApply = null;
                List<CurrencyApply> list = (List<CurrencyApply>) bill.getData();
                if (list != null) {
                    currencyApply = list.get(0);
                    String accEntityId = currencyApply.getAccentity();
                    if (accEntityId == null) {
                        return new RuleExecuteResult();
                    }
                    List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
                    String regionCurrency = currencyApply.getRegionCurrency();
                    if (StringUtils.isNotEmpty(regionCurrency)){
                        if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {//卖出通道
                            if (bill.getKey().equals("purchasecashaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", regionCurrency);
                            } else {
                                UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getSellCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", regionCurrency);
                                }
                            }
                        } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {//买入通道
                            if (bill.getKey().equals("sellcashaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", regionCurrency);
                            } else {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getPurchaseCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getPurchaseCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", regionCurrency);
                                }
                            }
                        } else {//外币兑换
                            MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
                            if (bill.getKey().equals("purchasecashaccount_name")){
                                if (currencyApply.getPurchaseCurrency()!=null){
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getPurchaseCurrency());
                                }else if (currencyApply.getSellCurrency()!=null){
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyApply.getSellCurrency());
                                }
                            }else if(bill.getKey().equals("sellcashaccount_name")){
                                if (currencyApply.getSellCurrency()!=null){
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getSellCurrency());
                                }else if (StringUtils.isNotBlank(currencyApply.getPurchaseCurrency())){
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyApply.getPurchaseCurrency());
                                }
                            }
                        }
                    }else {
                        if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {//卖出通道
                            if (bill.getKey().equals("purchasecashaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", accEntity.get(0).get("currency"));
                            } else {
                                UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getSellCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
                                }
                            }
                        } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {//买入通道
                            if (bill.getKey().equals("sellcashaccount_name")) {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", accEntity.get(0).get("currency"));
                            } else {
                                MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
                                if (currencyApply.getPurchaseCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getPurchaseCurrency());
                                } else {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
                                }
                            }
                        } else {//外币兑换
                            MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
                            if (bill.getKey().equals("purchasecashaccount_name")) {
                                if (currencyApply.getPurchaseCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getPurchaseCurrency());
                                } else if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyApply.getSellCurrency());
                                }
                            } else if (bill.getKey().equals("sellcashaccount_name")) {
                                if (currencyApply.getSellCurrency() != null) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyApply.getSellCurrency());
                                } else if (StringUtils.isNotBlank(currencyApply.getPurchaseCurrency())) {
                                    MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyApply.getPurchaseCurrency());
                                }
                            }
                        }
                    }
                    // 多币种添加币种启用过滤
                    MddBaseUtils.appendCondition(conditionVO, ICmpConstant.ENABLE, "eq", 1);
                }
            }
        }

        return new RuleExecuteResult();
    }
}
