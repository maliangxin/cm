package com.yonyoucloud.fi.cmp.cashinventory.check;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cashinventory.CashInventory_b;
import com.yonyoucloud.fi.cmp.cashinventory.InventoryCheckService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.denominationSetting.DenominationSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* @version 1.0
* @since 2022-01-17
*/
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCashaccountCheckRule extends AbstractCommonRule {

    private final static String CASH = "CASH";
    private final InventoryCheckService inventoryCheckService;
    private final BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!ICmpConstant.CASH_ACCOUNT_LOWER_NAME.equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        if (!inventoryCheckService.ruleCheck(bill, CASH)) {
            return new RuleExecuteResult();
        }
        String currency = bill.get(IBussinessConstant.CURRENCY);
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
        if (currencyDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101067"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180769","当前币种不存在") /* "当前币种不存在" */);
        }
        bill.set("currency_priceDigit", currencyDTO.getPricedigit());
        bill.set("currency_moneyDigit", currencyDTO.getMoneydigit());
        //获取汇率
        getExchRate(bill, currency);
        List<CashInventory_b> cashInventorybes = new ArrayList<>();
        //查询盘点设置获取数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.CURRENCY).eq(currency));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> listDenominationSetting = MetaDaoHelper.query(DenominationSetting.ENTITY_NAME, schema, null);
        if (!CollectionUtils.isEmpty(listDenominationSetting)) {
            if (listDenominationSetting.size() > 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101068"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418076A","一个币种获取到多条货币面额设置，请查看货币面额设置是否正确。") /* "一个币种获取到多条货币面额设置，请查看货币面额设置是否正确。" */);
            }
            Map<String, Object> denominationSettingMap = listDenominationSetting.get(0);
            DenominationSetting denominationSetting = MetaDaoHelper.findById(DenominationSetting.ENTITY_NAME, denominationSettingMap.get("id"), 3);
            //货币面额组件id
            bill.set("denominationSettingId", denominationSetting.get("id"));
            //基本单位
            bill.set("basicunit", denominationSetting.get("baseUnit"));
            List<Map<String, Object>> denominationSettingblist = denominationSetting.get("DenominationSetting_b");
            if (!CollectionUtils.isEmpty(denominationSettingblist)) {
                for (Map<String, Object> mapSetting : denominationSettingblist) {
                    if (null != mapSetting.get("status") && mapSetting.get("status").toString().equals("1")) {
                        CashInventory_b cashInventoryb = new CashInventory_b();
                        cashInventoryb.setAmountmoney(BigDecimal.ZERO);//金额
                        cashInventoryb.setConvertvalue(null != mapSetting.get("conversionUnit") ? (BigDecimal) mapSetting.get("conversionUnit") : BigDecimal.ZERO);//换算基本单位值
                        cashInventoryb.setDenomination(null != mapSetting.get("currencyDenomination") ? mapSetting.get("currencyDenomination").toString() : "");//货币面额
                        cashInventoryb.setQuantity(0);//数量
                        cashInventoryb.setDenominationSettingbId(Long.parseLong(mapSetting.get("id").toString()));
                        cashInventorybes.add(cashInventoryb);
                    }
                }
            }
        }
        //现金实点金额
        bill.set("actualamount", BigDecimal.ZERO);
        //子表
        if (CollectionUtils.isEmpty(cashInventorybes)) {
            bill.set("CashInventory_b", null);
        } else {
            bill.set("CashInventory_b", cashInventorybes);
        }
        inventoryCheckService.amountCalculation(bill);
        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

   /**
    * 获取汇率
    * @param bill
    * @param currency
    * @throws Exception
    */
   public void getExchRate(BizObject bill, String currency) throws Exception {
       if(!(null == bill.get("natCurrency") || "".equals(bill.get("natCurrency")))){
           String natCurrency = bill.get("natCurrency");
           if(natCurrency.equals(currency)){
               bill.set("exchRate", BigDecimal.valueOf(1));
           }else {
               if (!(null == bill.get("exchangeRateType") || "".equals(bill.get("exchangeRateType")))) {
                   ExchangeRate exchangeRate = baseRefRpcService.queryRateByExchangeType(currency, natCurrency, bill.getDate("vouchdate"),bill.getString("exchangeRateType"));
                   if (exchangeRate == null) {
                       bill.set("exchRate", null);
                   }else{
                       bill.set("exchRate", BigDecimal.valueOf(exchangeRate.getExchangerate()));
                   }
               }
           }
       }
   }

}
