package com.yonyoucloud.fi.cmp.fundpayment.check;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_PAYMENT;

/**
 * <h1>资金付款单会计主体检查</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FundPaymentAccentityCheckRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;

    //@Autowired
//    //OrgRpcService orgRpcService;

    private final CmCommonService cmCommonService;
    private final IFundCommonService fundCommonService;

    private static final String QUICK_TYPE_MAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!"accentity_name".equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        String accEntityId = bill.get(IBussinessConstant.ACCENTITY);
        if (accEntityId == null) {
            return new RuleExecuteResult();
        }
        // 设置是否结算成功后过账字段值
        fundCommonService.setSettleSuccessPostValue(bill, accEntityId);
        bill.set("org", accEntityId);
        bill.set("org_name", bill.get("accentity_name"));
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);
        if (finOrgDTO == null) {
            return new RuleExecuteResult();
        }


        // 判断是否启用商业汇票
        CtmJSONObject enableBsdModule = null;
        try {
            enableBsdModule = fundCommonService.isEnableBsdModule(accEntityId);
        } catch (Exception e) {
            log.error("get BSD is enabled fail!, e = {}", e.getMessage());
        }
        boolean flag = false;
        if (ValueUtils.isNotEmptyObj(enableBsdModule)) {
            flag = MapUtils.getBoolean(enableBsdModule, "isEnabled");
        }
        bill.set("isEnabledBsd", flag);
        if (EventType.FundPayment.getValue() == Short.parseShort(bill.get("billtype").toString())) {
            try {
                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
                if (currencyTenantDTO != null) {
                    // 设置币种
                    bill.set("currency", currencyTenantDTO.getId());
                    bill.set("currency_name", currencyTenantDTO.getName());
                    bill.set("currency_priceDigit", currencyTenantDTO.getPricedigit());
                    bill.set("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
                    bill.set("natCurrency", currencyTenantDTO.getId());
                    bill.set("natCurrency_name", currencyTenantDTO.getName());
                    bill.set("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                    bill.set("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
                    bill.set("exchRate", 1);

                    bill.set("settleCurrency", currencyTenantDTO.getId());
                    bill.set("settleCurrency_name", currencyTenantDTO.getName());
                    bill.set("settleCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                    bill.set("settleCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
                    bill.set("swapOutExchangeRateEstimate", 1);
                    // 是否传资金结算如果是空就默认为是
                    if (Objects.isNull(bill.get("settleflag"))) {
                        bill.set("settleflag", 1);
                    }
                }
            } catch (Exception e) {
                log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18D4A5DC05400008","未取到本币币种！ e = {}") /* "未取到本币币种！ e = {}" */, e.getMessage());
            }
        }
        // 会计主体设置汇率类型
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accEntityId,true);
        if (defaultExchangeRateType != null && defaultExchangeRateType.getId() != null) {
            bill.set("exchangeRateType", defaultExchangeRateType.getId());
            bill.set("exchangeRateType_name", defaultExchangeRateType.getName());
            bill.set("exchangeRateType_digit", defaultExchangeRateType.getDigit());

            bill.set("swapOutExchangeRateType", defaultExchangeRateType.getId());
            bill.set("swapOutExchangeRateType_name", defaultExchangeRateType.getName());
            bill.set("swapOutExchangeRateType_digit", defaultExchangeRateType.getDigit());
        }

        //设置汇率
        if(bill.getString("currency")!=null && bill.getString("natCurrency")!=null && bill.getDate("vouchdate")!=null && bill.getString("exchangeRateType")!=null) {
            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bill.getString("currency"),bill.getString("natCurrency"),bill.getDate("vouchdate"),bill.getString("exchangeRateType"));
            bill.set("exchRate", cmpExchangeRateVO.getExchangeRate());
            bill.set("exchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
        }

        //资金组织查询会理替换换出汇率
        ExchangeRateTypeVO DefaultExchangeRateTypeOfFinOrg = CmpExchangeRateUtils.getNewExchangeRateType(accEntityId,false);
        if (DefaultExchangeRateTypeOfFinOrg != null && DefaultExchangeRateTypeOfFinOrg.getId() != null) {
            bill.set("swapOutExchangeRateType", DefaultExchangeRateTypeOfFinOrg.getId());
            bill.set("swapOutExchangeRateType_name", DefaultExchangeRateTypeOfFinOrg.getName());
            bill.set("swapOutExchangeRateType_digit", DefaultExchangeRateTypeOfFinOrg.getDigit());
            bill.set("swapOutExchangeRateType_code", DefaultExchangeRateTypeOfFinOrg.getCode());

        }
        // 设置交易类型
        if (!ValueUtils.isNotEmptyObj(bill.get("tradetype"))) {
            setValueForTransType(bill);
        }
        // 设置子表款项类型和币种
        setValueForQuickType(bill);
        if (EventType.FundPayment.getValue() != Short.parseShort(bill.get("billtype").toString())) {
            this.putParam(paramMap, "return", bill);
            return new RuleExecuteResult();
        }


        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

    private void setValueForQuickType(BizObject bill) {
        if (null != bill.get("FundPayment_b")) {
            try {
                short quickCode = -999;
                HashMap<String, Object> quickCodeMap = SqlHelper.selectOne(QUICK_TYPE_MAPPER + ".getFundPaymentQuickTypeCode", AppContext.getTenantId());
                if (null != quickCodeMap && null != quickCodeMap.get("cDefaultValue") && !"".equals(quickCodeMap.get("cDefaultValue"))) {
                    quickCode = Short.parseShort(String.valueOf(quickCodeMap.get("cDefaultValue")));
                }
                List<FundPayment_b> fundPaymentChildList = bill.getBizObjects("FundPayment_b", FundPayment_b.class);
                if (CollectionUtils.isEmpty(fundPaymentChildList)) {
                    fundPaymentChildQuickTypeSetting(bill, quickCode);
                } else {
                    for (FundPayment_b fundPaymentB : fundPaymentChildList) {
                        if (EventType.FundPayment.getValue() == Short.parseShort(bill.get("billtype").toString())) {
                            setFundPaymentFieldValue(bill, fundPaymentB);
                        }
                        if (quickCode != -999) {
                            List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(quickCode)));
                            if (quickTypeMap.size() > 0) {
                                if (!MapUtils.getBoolean(quickTypeMap.get(0), "stopstatus")) {
                                    fundPaymentB.setQuickType(MapUtils.getLong(quickTypeMap.get(0), "id"));
                                    fundPaymentB.set("quickType_name", MapUtils.getString(quickTypeMap.get(0), "name"));
                                    fundPaymentB.set("quickType_code", MapUtils.getString(quickTypeMap.get(0), "code"));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("未取到默认的款项类型! e = {}", e.getMessage());//@notranslate
            }
        }
    }

    private void setFundPaymentFieldValue(BizObject bill, FundPayment_b fundPaymentB) {
        fundPaymentB.set("currency", bill.get("currency"));
        fundPaymentB.set("currency_name", bill.get("currency_name"));
        fundPaymentB.set("currency_priceDigit", bill.get("currency_priceDigit"));
        fundPaymentB.set("currency_moneyDigit", bill.get("currency_moneyDigit"));
        fundPaymentB.set("exchRate", 1);
        fundPaymentB.set("exchangeRateType", bill.get("exchangeRateType"));
        fundPaymentB.set("exchRate", bill.get("exchRate"));
        fundPaymentB.set("exchangeRateOps", bill.get("exchangeRateOps"));
        fundPaymentB.set("exchangeRateType_name", bill.get("exchangeRateType_name"));
        fundPaymentB.set("exchangeRateType_digit", bill.get("exchangeRateType_digit"));

        fundPaymentB.set("settleCurrency", bill.get("settleCurrency"));
        fundPaymentB.set("settleCurrency_name", bill.get("settleCurrency_name"));
        fundPaymentB.set("settleCurrency_priceDigit", bill.get("settleCurrency_priceDigit"));
        fundPaymentB.set("settleCurrency_moneyDigit", bill.get("settleCurrency_moneyDigit"));
        fundPaymentB.set("swapOutExchangeRateEstimate", 1);
        fundPaymentB.set("swapOutExchangeRateType", bill.get("swapOutExchangeRateType"));
        fundPaymentB.set("swapOutExchangeRateType_name", bill.get("swapOutExchangeRateType_name"));
        fundPaymentB.set("swapOutExchangeRateType_digit", bill.get("swapOutExchangeRateType_digit"));
    }

    private void setValueForTransType(BizObject bill) {
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_FUND_PAYMENT));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
//                    throw new CtmException("查询资金付款单交易类型失败！请检查数据。");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById(billTypeId, "1", null);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                bill.set("tradetype", tradetypeMap.get("id"));
                bill.set("tradetype_name", tradetypeMap.get("name"));
                bill.set("tradetype_code", tradetypeMap.get("code"));
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
//            throw new CtmException("未获取到默认的交易类型！");
        }
    }

    /**
     * <h2>给单据默认添加一行带款项类型得子表</h2>
     *
     * @param bizObject : 单据对象
     * @param quickCode : 款项类型Code
     * @author Sun GuoCai
     * @since 2021/12/13 10:52
     */
    private void fundPaymentChildQuickTypeSetting(BizObject bizObject, short quickCode) throws Exception {
        FundPayment_b fundPaymentB = new FundPayment_b();
        setFundPaymentFieldValue(bizObject, fundPaymentB);
        if (quickCode != -999) {
            List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(quickCode)));
            if (quickTypeMap.size() > 0) {
                if (!MapUtils.getBoolean(quickTypeMap.get(0), "stopstatus")) {
                    fundPaymentB.setQuickType(MapUtils.getLong(quickTypeMap.get(0), "id"));
                    fundPaymentB.set("quickType_name", MapUtils.getString(quickTypeMap.get(0), "name"));
                    fundPaymentB.set("quickType_code", MapUtils.getString(quickTypeMap.get(0), "code"));
                }
            }
        }
        bizObject.set("FundPayment_b", Collections.singletonList(fundPaymentB));

    }

}
