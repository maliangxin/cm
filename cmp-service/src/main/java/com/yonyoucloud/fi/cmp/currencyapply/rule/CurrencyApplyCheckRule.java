package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.FICurrencyRateService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.cmpentity.CurrencyRateTypeCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IMultilangConstant;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.ExchangeRateTypeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @description: 外币兑换申请check规则
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/17 10:31
 */

@Slf4j
@Component("currencyApplyCheckRule")
public class CurrencyApplyCheckRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    FICurrencyRateService currencyRateService;

    //@Autowired
//    OrgRpcService orgRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    ExchangeRateTypeQueryService exchangeRateTypeQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        BizObject bill = getBills(billContext, paramMap).get(0);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        CurrencyApply currencyApply = (CurrencyApply)bill;
        if ("accentity_name".equals(item.get("key"))) {
            if (item.get("value") == null){
                return new RuleExecuteResult();
            }
            Map<String, Object> condition = new HashMap<>();
            String accentity = currencyApply.getAccentity();
            if (Objects.isNull(accentity)){
                return new RuleExecuteResult();
            }
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {//卖出外汇
                bill.set("purchaserate", 1);
                bill.set("purchaseRateOps", 1);
                //交易类型初始化条件
                condition.put("code","APPLY_SFE");
                if (finOrgDTO != null) {
                    String exchangeRateType = currencyApply.getExchangeRateType();
                    //若不存在汇率类型则加载会计主体的汇率类型
                    if (StringUtils.isEmpty(exchangeRateType)){
                        exchangeRateType = finOrgDTO.getExchangerate();
                    }
                    String currId = bill.get("regionCurrency");
                    //若地区币种不为空 则更新
                    if (StringUtils.isEmpty(currId)){
                        currId = finOrgDTO.getCurrency();
                    }
                    CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currId);
                    bill.set("purchaseCurrency", currencyTenantDTO.getId());
                    bill.set("purchaseCurrency_name", currencyTenantDTO.getName());
                    bill.set("purchaseCurrency_priceDigit", currencyTenantDTO.getPricedigit());

                    bill.set("purchaseCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
                    EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accentity, finOrgDTO.getCurrency(), billContext, "purchasebankaccount");
                    if (defaultBankAccount != null ) {
                        bill.set("purchasebankaccount", defaultBankAccount.getId());
                        bill.set("purchasebankaccount_name", defaultBankAccount.getName());
                    }
                    if (bill.get("regionCurrency") != null){
                        if (finOrgDTO.getCurrency().equals(bill.get("sellCurrency"))){
                            bill.set("purchaserate", 1);
                            bill.set("purchaseRateOps", 1);
                        }else {
                            setRate("purchaserate", bill,finOrgDTO.getCurrency(), bill.getString("purchaseCurrency"), currencyApply.getVouchdate(), exchangeRateType);
                        }
                    }
                }
            } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {//买入外汇
                bill.set("sellrate", 1);
                bill.set("sellRateOps", 1);
                //交易类型初始化条件
                condition.put("code","APPLY_BFE");
                if (finOrgDTO != null) {
                    String exchangeRateType = currencyApply.getExchangeRateType();
                    //若不存在汇率类型则加载会计主体的汇率类型
                    if (StringUtils.isEmpty(exchangeRateType)){
                        exchangeRateType = finOrgDTO.getExchangerate();
                    }
                    String currId = bill.get("regionCurrency");
                    //若地区币种不为空 则更新
                    if (StringUtils.isEmpty(currId)){
                        currId = finOrgDTO.getCurrency();
                    }
                    CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currId);
                    bill.set("sellCurrency", currencyTenantDTO.getId());
                    bill.set("sellCurrency_name", currencyTenantDTO.getName());
                    bill.set("sellCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                    bill.set("sellCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());

                    EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accentity, finOrgDTO.getCurrency(), billContext, "sellbankaccount");
                    if (defaultBankAccount != null) {
                        bill.set("sellbankaccount", defaultBankAccount.getId());
                        bill.set("sellbankaccount_name", defaultBankAccount.getName());
                    }
                    //当买入币种=会计本位币时，卖出汇率为1
                    if (bill.get("regionCurrency") != null){
                        if (finOrgDTO.getCurrency().equals(bill.get("purchaseCurrency"))){
                            bill.set("sellrate", 1);
                            bill.set("sellRateOps", 1);
                        }else {
                            setRate("sellrate", bill,finOrgDTO.getCurrency(), bill.getString("sellCurrency"), currencyApply.getVouchdate(), exchangeRateType);
                        }
                    }
                }
            } else {
                //交易类型初始化条件
                condition.put("code","APPLY_BSFE");
            }
            //交易类型赋值
            List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
            if (!transTypes.isEmpty()) {
                bill.set("tradetype", transTypes.get(0).get("id"));
                bill.set("tradetype_name", transTypes.get(0).get("name"));
            }
            if (finOrgDTO != null) {
                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
                bill.set("natCurrency", currencyTenantDTO.getId());
                //默认地区币种取会计本位币币种
                if (bill.get("regionCurrency") == null){
                    bill.set("regionCurrency",currencyTenantDTO.getId());
                    bill.set("regionCurrency_name",currencyTenantDTO.getName());
                }
            }
            return new RuleExecuteResult();
        }

        //需要校验item的value是否是json格式
        if ((StringUtils.isEmpty((String) item.get("value")) || ((String) item.get("value")).length() < 3 || !((String) item.get("value")).startsWith("{"))
                && !Objects.isNull(currencyApply.getAccentity())) {
            String accentity = currencyApply.getAccentity();
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            String currId = currencyApply.getNatCurrency();
            String exchangeRateType = currencyApply.getExchangeRateType();
            if (finOrgDTO !=null){
                if (finOrgDTO.getCurrency()!=null && StringUtils.isEmpty(currId)){
                    currId = finOrgDTO.getCurrency();
                }
                //若不存在汇率类型则加载会计主体的汇率类型
                if (finOrgDTO.getExchangerate()!=null && StringUtils.isEmpty(exchangeRateType)){
                    exchangeRateType = finOrgDTO.getExchangerate();
                }
            }
            //外币兑换申请重新重置汇率
            if (currencyApply.getPurchaseCurrency() != null) {
                if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {
                    setRate("purchaserate", bill, currId, currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), exchangeRateType);
                } else if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {
                    setRate("purchaserate", bill, currId, currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), exchangeRateType);
                } else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {
                    setRate("purchaserate", bill, currId, currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), exchangeRateType);
                    if (currencyApply.getSellCurrency() != null) {
                        setRate("exchangerate", bill, currencyApply.getSellCurrency(), currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    }
                }
            }
            if (currencyApply.getSellCurrency() != null) {
                if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {
                    setRate("sellrate", bill, currId, currencyApply.getSellCurrency(), currencyApply.getVouchdate(), exchangeRateType);
                } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {
                    setRate("sellrate", bill, currId, currencyApply.getSellCurrency(), currencyApply.getVouchdate(), exchangeRateType);
                } else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {
                    setRate("sellrate", bill, currId, currencyApply.getSellCurrency(), currencyApply.getVouchdate(), exchangeRateType);
                    if (currencyApply.getPurchaseCurrency() != null) {
                        setRate("exchangerate", bill, currencyApply.getSellCurrency(), currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    }
                }
            }
            this.putParam(paramMap, "return", bill);
            return new RuleExecuteResult();
        }

        if (item.get("value") == null) {
            return new RuleExecuteResult();
        }
        CtmJSONObject obj = CtmJSONObject.parseObject((String)item.get("value"));
        if (obj==null){
            return new RuleExecuteResult();
        }
        Object currency = bill.get("natCurrency");
        if (currency == null) {
            String accentity = currencyApply.getAccentity();
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
            bill.set("natCurrency", currencyTenantDTO.getId());
            currency = currencyTenantDTO.getId();
            currencyApply.setNatCurrency(currencyTenantDTO.getId());
            //默认地区币种取会计本位币币种
            if (bill.get("regionCurrency") == null){
                bill.set("regionCurrency",currencyTenantDTO.getId());
                bill.set("regionCurrency_name",currencyTenantDTO.getName());
            }
        }
        CurrencyTenantDTO natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(currency.toString());
        if (natCurrencyTenantDTO != null) {
            bill.set("natCurrency", natCurrencyTenantDTO.getId());
            bill.set("natCurrency_name", natCurrencyTenantDTO.getName());
            bill.set("natCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
            bill.set("natCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
            //默认地区币种取会计本位币币种
            if (bill.get("regionCurrency") == null){
                bill.set("regionCurrency",natCurrencyTenantDTO.getId());
                bill.set("regionCurrency_name",natCurrencyTenantDTO.getName());
            }
        }

        //结算属性
        String settlemodeServiceAttr = currencyApply.get("settlemode_serviceAttr");
        if ("purchasebankaccount_name".equals(item.get("key"))) {
            String accentity = currencyApply.getAccentity();
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            String currId = bill.get("regionCurrency");
            if (StringUtils.isEmpty(currId)){
                currId = finOrgDTO.getCurrency();
            }
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currId);

            if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {
                //bill.set("purchaserate", 1);
                if (bill.get("regionCurrency") != null){
                    if (finOrgDTO.getCurrency().equals(bill.get("sellCurrency"))){
                        bill.set("purchaserate", 1);
                    }else {
                        setRate("purchaserate", bill,finOrgDTO.getCurrency(), bill.getString("purchaseCurrency"), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    }
                }
            } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {
                setRate("purchaserate", bill,finOrgDTO.getCurrency(), bill.getString("purchaseCurrency"), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
            } else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {
                Object purchaseCurrency = currencyApply.get("purchaseCurrency");
                Object sellCurrency = currencyApply.get("sellCurrency");
                if (purchaseCurrency != null && sellCurrency != null) {
                    setRate("exchangerate", bill, (String) sellCurrency, (String) purchaseCurrency, currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                }
                if (purchaseCurrency == null) {
                    setRate("purchaserate", bill, currencyTenantDTO.getId(), natCurrencyTenantDTO.getId(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                }
            }
            if (bill.get("regionCurrency") != null){
                bill.set("purchaseCurrency", currencyTenantDTO.getId());
                bill.set("purchaseCurrency_name", currencyTenantDTO.getName());
                bill.set("purchaseCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                bill.set("purchaseCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }else {
                bill.set("purchaseCurrency", natCurrencyTenantDTO.getId());
                bill.set("purchaseCurrency_name", natCurrencyTenantDTO.getName());
                bill.set("purchaseCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
                bill.set("purchaseCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
            }

        } else if ("sellbankaccount_name".equals(item.get("key"))) {
            String accentity = currencyApply.getAccentity();
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            String currId = bill.get("regionCurrency");
            if (StringUtils.isEmpty(currId)){
                currId = finOrgDTO.getCurrency();
            }
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currId);
            if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {
                //bill.set("sellrate", 1);
                if (bill.get("regionCurrency") != null){
                    if (finOrgDTO.getCurrency().equals(bill.get("purchaseCurrency"))){
                        bill.set("sellrate", 1);
                    }else {
                        setRate("sellrate", bill,finOrgDTO.getCurrency(), bill.getString("sellCurrency"), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    }
                }
            } else {
                setRate("sellrate", bill,finOrgDTO.getCurrency(), bill.getString("sellCurrency"), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
            }
            if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {
                Object purchaseCurrency = currencyApply.get("purchaseCurrency");
                Object sellCurrency = currencyApply.get("sellCurrency");
                if (purchaseCurrency != null && sellCurrency != null) {
                    setRate("exchangerate", bill, (String) sellCurrency, (String) purchaseCurrency, currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                }
                if (sellCurrency == null) {
                    setRate("sellrate", bill, natCurrencyTenantDTO.getId(), currencyTenantDTO.getId(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                }
            }
            if (bill.get("regionCurrency") != null){
                bill.set("sellCurrency", currencyTenantDTO.getId());
                bill.set("sellCurrency_name", currencyTenantDTO.getName());
                bill.set("sellCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                bill.set("sellCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }else {
                bill.set("sellCurrency", natCurrencyTenantDTO.getId());
                bill.set("sellCurrency_name", natCurrencyTenantDTO.getName());
                bill.set("sellCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
                bill.set("sellCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
            }

        } else if ("purchaseCurrency_name".equals(item.get("key"))) {
            Object accEntityId = currencyApply.getAccentity();
            Object currencyId = currencyApply.getPurchaseCurrency();
            if (accEntityId != null && currencyId != null && !StringUtils.isEmpty(settlemodeServiceAttr) && "0".equals(settlemodeServiceAttr)) {
                EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accEntityId, currencyId, billContext, "purchasebankaccount");
                if (defaultBankAccount != null) {
                    if (currencyApply.get("sellbankaccount") != null) {
                        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyApply.get("sellbankaccount"));
                        EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
                        if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())) {
                            bill.set("purchasebankaccount", defaultBankAccount.getId());
                            bill.set("purchasebankaccount_name", defaultBankAccount.getName());
                        } else {
                            bill.set("purchasebankaccount", null);
                            bill.set("purchasebankaccount_name", null);
                        }
                    } else if (currencyApply.get("commissionbankaccount") != null) {
                        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyApply.get("commissionbankaccount"));
                        EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
                        if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())) {
                            bill.set("purchasebankaccount", defaultBankAccount.getId());
                            bill.set("purchasebankaccount_name", defaultBankAccount.getName());
                        } else {
                            bill.set("purchasebankaccount", null);
                            bill.set("purchasebankaccount_name", null);
                        }
                    } else {
                        bill.set("purchasebankaccount", defaultBankAccount.getId());
                        bill.set("purchasebankaccount_name", defaultBankAccount.getName());
                    }
                } else {
                    bill.set("purchasebankaccount", null);
                    bill.set("purchasebankaccount_name", null);
                }

            }
            if (currencyApply.getPurchaseCurrency() != null) {
                if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {
                    setRate("purchaserate", bill, currencyApply.getNatCurrency(), currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                } else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {
                    setRate("purchaserate", bill, currencyApply.getNatCurrency(), currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    if (currencyApply.getSellCurrency() != null) {
                        setRate("exchangerate", bill, currencyApply.getSellCurrency(), currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    }
                }
            }
        } else if ("sellCurrency_name".equals(item.get("key"))) {
            Object accEntityId = currencyApply.getAccentity();
            Object currencyId = currencyApply.getSellCurrency();
            if (accEntityId != null && currencyId != null && !StringUtils.isEmpty(settlemodeServiceAttr) && "0".equals(settlemodeServiceAttr)) {
                EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accEntityId, currencyId, billContext, "sellbankaccount");
                if (defaultBankAccount != null) {
                    if (currencyApply.get("purchasebankaccount") != null) {
                        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyApply.get("purchasebankaccount"));
                        EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
                        if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())) {
                            bill.set("sellbankaccount", defaultBankAccount.getId());
                            bill.set("sellbankaccount_name", defaultBankAccount.getName());
                        } else {
                            bill.set("sellbankaccount", null);
                            bill.set("sellbankaccount_name", null);
                        }
                    } else if (currencyApply.get("commissionbankaccount") != null) {
                        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyApply.get("commissionbankaccount"));
                        EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
                        if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())) {
                            bill.set("sellbankaccount", defaultBankAccount.getId());
                            bill.set("sellbankaccount_name", defaultBankAccount.getName());
                        } else {
                            bill.set("sellbankaccount", null);
                            bill.set("sellbankaccount_name", null);
                        }
                    } else {
                        bill.set("sellbankaccount", defaultBankAccount.getId());
                        bill.set("sellbankaccount_name", defaultBankAccount.getName());
                    }
                } else {
                    bill.set("sellbankaccount", null);
                    bill.set("sellbankaccount_name", null);
                }

            }
            if (currencyApply.getSellCurrency() != null) {
                if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {
                    setRate("sellrate", bill, currencyApply.getNatCurrency(), currencyApply.getSellCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                } else if (currencyApply.get("flag").equals(Bsflag.Exchange.getValue())) {
                    setRate("sellrate", bill, currencyApply.getNatCurrency(), currencyApply.getSellCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    if (currencyApply.getPurchaseCurrency() != null) {
                        setRate("exchangerate", bill, currencyApply.getSellCurrency(), currencyApply.getPurchaseCurrency(), currencyApply.getVouchdate(), currencyApply.getExchangeRateType());
                    }
                }
            }
        } else if ("regionCurrency_name".equals(item.get("key"))) {
            if (currencyApply.getRegionCurrency() != null) {
                String regionCurrId = currencyApply.getRegionCurrency();
                CurrencyTenantDTO dto = baseRefRpcService.queryCurrencyById(regionCurrId);
                if (currencyApply.get("flag").equals(Bsflag.Sell.getValue())) {
                    bill.set("purchaseCurrency", dto.getId());
                    bill.set("purchaseCurrency_name", dto.getName());
                    bill.set("purchaseCurrency_priceDigit", dto.getPricedigit());
                    bill.set("purchaseCurrency_moneyDigit", dto.getMoneydigit());
                    bill.set("purchasebankaccount", null);
                } else if (currencyApply.get("flag").equals(Bsflag.Buy.getValue())) {
                    bill.set("sellCurrency", dto.getId());
                    bill.set("sellCurrency_name", dto.getName());
                    bill.set("sellCurrency_priceDigit", dto.getPricedigit());
                    bill.set("sellCurrency_moneyDigit", dto.getMoneydigit());
                    bill.set("sellbankaccount", null);
                }
            }
        }

        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

    private EnterpriseBankAcctVO getDefaultBankAccount(Object accentity,Object currency,BillContext billContext,String fieldName) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setOrgid(accentity.toString());
        List<String> currencyList = new ArrayList<>();
        currencyList.add(currency.toString());
        enterpriseParams.setCurrencyIDList(currencyList);
        enterpriseParams.setCurrencyIsDefault(true);
        List<EnterpriseBankAcctVO> bankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (bankAccounts.size() > 0) {
            return bankAccounts.get(0);
        }
        return null;
    }

    private void setRate(String key, BizObject bill, String natCurrencyId, String currencyId, Object vouchDate, String exchangeRateType) throws Exception {
        if (natCurrencyId != null && currencyId.equals(natCurrencyId)) {
            bill.set(key, 1);
            return;
        }
        if (StringUtils.isEmpty(exchangeRateType) || StringUtils.isEmpty(natCurrencyId)) {
            return;
        }
        String rateTypeName = "";
        String oriCurrencyName = "";
        if ("sellrate".equals(key)){
            exchangeRateType = bill.getString("sellRateType");
            rateTypeName = bill.getString("sellRateType_name");
            oriCurrencyName = bill.getString("sellCurrency_name");
        }else if ("purchaserate".equals(key)){
            exchangeRateType = bill.getString("purchaseRateType");
            rateTypeName = bill.getString("purchaseRateType_name");
            oriCurrencyName = bill.getString("purchaseCurrency_name");
        }else if ("commissionrate".equals(key)){
            exchangeRateType = bill.getString("commissionRateType");
            rateTypeName = bill.getString("commissionRateType_name");
            CurrencyTenantDTO dto = baseRefRpcService.queryCurrencyById(currencyId);
            oriCurrencyName = dto.getName();
        }
        // 自定义汇率直接返回
        ExchangeRateTypeVO exchangeRateTypeVO = exchangeRateTypeQueryService.queryExcahngeRateTypeById(exchangeRateType);
        //v5用户自定义汇率，不进行数据查询
        if (exchangeRateTypeVO == null || CurrencyRateTypeCode.CustomCode.getValue().equals(exchangeRateTypeVO.getCode())){
            return;
        }
        CtmJSONObject params1 = new CtmJSONObject();
        params1.put("currencyId", currencyId);
        params1.put("natCurrencyId", natCurrencyId);//买入银行账户的币种
        params1.put("vouchDate", vouchDate == null ? DateUtils.dateFormat(new Date(), DateUtils.DATE_PATTERN) : vouchDate);//单据日期
        params1.put("exchangeRateType", exchangeRateType);//单据日期
        try {
//            Map<String, Object> periodByParam = getPeriodByParam(params1); // V5新汇率修改后该方法废弃
            //v5新汇率获取方法修改
            CmpExchangeRateVO cmpExchangeRateVO;
            //成交汇率设置值 -> 买入币种等于源币种，卖出币种等于目的币种，取直接汇率，否则取间接汇率
            if ("exchangerate".equals(key)){
                String purchaseCurrencyId = bill.getString("purchaseCurrency");
                String sellCurrencyId = bill.getString("sellCurrency");
                //汇率平台-汇率折算方式 1乘；2除;rateconversiontype 0是直接汇率（对应平台乘法）；1是间接汇率（对应平台除法）
                String exchRateOps = "0".equals(bill.getString("rateconversiontype")) ? "1" :  "2";
                cmpExchangeRateVO = CmpExchangeRateUtils.queryExchangeRateWithModeAndIsReverse(sellCurrencyId, purchaseCurrencyId, vouchDate == null ? new Date() : (Date) vouchDate, exchangeRateType, exchRateOps, true);
                if ( cmpExchangeRateVO.getExchangeRate() == null){
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211EE40805500002", "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！") /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,rateTypeName,bill.getString("sellCurrency_name"),bill.getString("purchaseCurrency_name")));
                }
                bill.set("exchangerate", cmpExchangeRateVO.getExchangeRate());
                bill.set("exchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
            }else {
                //bill.set(key, periodByParam.get("exchangeRate")); //V5新汇率废弃
                //V5新汇率，汇率值和对应的折算方式
                try {
                    cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currencyId, natCurrencyId, vouchDate == null ? new Date() : (Date) vouchDate, exchangeRateType);
                    bill.set(key, cmpExchangeRateVO.getExchangeRate());
                    if ("sellrate".equals(key)){
                        bill.set("sellRateOps", cmpExchangeRateVO.getExchangeRateOps());
                    }else if ("purchaserate".equals(key)){
                        bill.set("purchaseRateOps", cmpExchangeRateVO.getExchangeRateOps());
                    }else if ("commissionrate".equals(key)){
                        bill.set("commissionRateOps", cmpExchangeRateVO.getExchangeRateOps());
                    }
                }catch (Exception e){
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211EE40805500002", "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！") /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,rateTypeName,oriCurrencyName,bill.getString("natCurrency_name")));
                }
            }
        }catch (Exception e){
            //未取到汇率 设置值为空
            bill.set(key, null);
        }

    }

    public Map<String, Object> getPeriodByParam(CtmJSONObject param)
            throws Exception {
        Map resultData = new HashMap<String, Object>();
        //Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, param.getString("exchangeRateType"), param.getString("currencyId"), param.getString("natCurrencyId"), param.getDate("vouchDate"), 6);
        if (StringUtils.isEmpty(param.getString("exchangeRateType")) || param.getDate("vouchDate") == null) {
            return resultData;
        }
        ExchangeRate exchangeRate = baseRefRpcService.queryRateByExchangeType(param.getString("currencyId"), param.getString("natCurrencyId"), param.getDate("vouchDate"), param.getString("exchangeRateType"));
        if (exchangeRate == null) {
            throw IMultilangConstant.noRateError/*未取到汇率 */;
        }
        Double rate = exchangeRate.getExchangerate();
        Double inDirectRate = exchangeRate.getIndirectExchangeRate();
        resultData.put("exchangeRate", rate);
        resultData.put("indirectExchangeRate", inDirectRate);
        return resultData;
    }
}
