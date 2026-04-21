package com.yonyoucloud.fi.cmp.fundcollection.check;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
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
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.QuickTypeVO;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.BizException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 资金收款单会计主体，交易类型等默认值查询校验
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class FundCollectionAccentityCheckRule extends AbstractCommonRule {

    private final CmCommonService<Object> cmCommonService;
    private final IFundCommonService fundCommonService;
    private final BaseRefRpcService baseRefRpcService;
//    private final OrgRpcService orgRpcService;
    private final FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;

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

        FundsOrgDTO fundsOrgDTO = fundsOrgQueryServiceComponent.getById(accEntityId);
        if (!ValueUtils.isNotEmptyObj(fundsOrgDTO)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050006", "根据资金组织id未查询到关联的会计主体！") /* "根据资金组织id未查询到关联的会计主体！" */);
        }

        String accEntityRaw = fundsOrgDTO.getFinorgid();
        String accEntityRawName = fundsOrgDTO.getFinOrgName();
        bill.set("accentityRaw", accEntityRaw);
        bill.set("accentityRaw_name", accEntityRawName);


        // 设置是否结算成功后过账字段值
        fundCommonService.setSettleSuccessPostValue(bill, accEntityId);
        if (ValueUtils.isNotEmptyObj(bill.get("billtype"))
                && Short.parseShort(bill.get("billtype").toString()) != EventType.cooperate_fund_collection.getValue()) {
            bill.set("org", accEntityId);
            bill.set("org_name", bill.get("accentity_name"));
        }
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);
        if (finOrgDTO==null) {
            return new RuleExecuteResult();
        }
        // 判断是否启用商业汇票
        CtmJSONObject enableBsdModule = null;
        try {
            enableBsdModule = fundCommonService.isEnableBsdModule(accEntityId);
        } catch (Exception e) {
            log.error("get BSD is enabled fail!, e = {}", e.getMessage());
        }
        if (EventType.FundCollection.getValue() == Short.parseShort(bill.get("billtype").toString())) {
            try {
                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
                if (currencyTenantDTO!=null) {
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
                    // 是否传资金结算如果是空就默认为是
                    if (Objects.isNull(bill.get("settleflag"))) {
                        bill.set("settleflag", 1);
                    }
                }
            } catch (Exception e) {
                log.error("未取到本币币种！ e = {}", e.getMessage());
            }
        }
        // 设置汇率类型
        Map<String, Object> defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accEntityRaw);
        if (defaultExchangeRateType != null && defaultExchangeRateType.get("id") != null) {
            bill.set("exchangeRateType", defaultExchangeRateType.get("id"));
            bill.set("exchangeRateType_name", defaultExchangeRateType.get("name"));
            bill.set("exchangeRateType_digit", defaultExchangeRateType.get("digit"));
        }
        //换出汇率
        Map<String, Object> swapOutDefaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accEntityId);
        if (swapOutDefaultExchangeRateType != null && swapOutDefaultExchangeRateType.get("id") != null) {
            bill.set("swapOutExchangeRateType", swapOutDefaultExchangeRateType.get("id"));
            bill.set("swapOutExchangeRateType_name", swapOutDefaultExchangeRateType.get("name"));
            bill.set("swapOutExchangeRateType_digit", swapOutDefaultExchangeRateType.get("digit"));
        }
        //换出汇率
        Map<String, Object> DefaultExchangeRateTypeOfFinOrg = cmCommonService.getDefaultExchangeRateTypeByFundsOrgid(accEntityId);
        if (DefaultExchangeRateTypeOfFinOrg != null && DefaultExchangeRateTypeOfFinOrg.get("id") != null) {
            bill.set("swapOutExchangeRateType", DefaultExchangeRateTypeOfFinOrg.get("id"));
            bill.set("swapOutExchangeRateType_name", DefaultExchangeRateTypeOfFinOrg.get("name"));
            bill.set("swapOutExchangeRateType_digit", DefaultExchangeRateTypeOfFinOrg.get("digit"));
            bill.set("swapOutExchangeRateType_code", DefaultExchangeRateTypeOfFinOrg.get("code"));

        }
        //设置汇率
        if(bill.getString("currency")!=null && bill.getString("natCurrency")!=null && bill.getDate("vouchdate")!=null && bill.getString("exchangeRateType")!=null) {
            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bill.getString("currency"),bill.getString("natCurrency"),bill.getDate("vouchdate"),bill.getString("exchangeRateType"));
            bill.set("exchRate", cmpExchangeRateVO.getExchangeRate());
            bill.set("exchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
        }
        // 设置交易类型
        if (!ValueUtils.isNotEmptyObj(bill.get("tradetype"))) {
            setValueForTransType(bill);
        }
        // 设置子表款项类型和币种
        setValueForQuickType(bill);
        boolean flag = false;
        if (ValueUtils.isNotEmptyObj(enableBsdModule)) {
            flag = MapUtils.getBoolean(enableBsdModule, "isEnabled");
        }

        bill.set("isEnabledBsd", flag);
        if (EventType.FundCollection.getValue() != Short.parseShort(bill.get("billtype").toString())) {
            this.putParam(paramMap, "return", bill);
            return new RuleExecuteResult();
        }

        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

    private void setValueForQuickType(BizObject bill) {
        if (null != bill.get("FundCollection_b")) {
            List<FundCollection_b> fundCollectionChildList = bill.getBizObjects("FundCollection_b", FundCollection_b.class);
            try {
                short quickCode = -999;
                HashMap<String, Object> quickCodeMap = SqlHelper.selectOne(QUICK_TYPE_MAPPER + ".getFundCollectionQuickTypeCode", AppContext.getTenantId());
                if (null != quickCodeMap && null != quickCodeMap.get("cDefaultValue") && !"".equals(quickCodeMap.get("cDefaultValue"))) {
                    quickCode = Short.parseShort(String.valueOf(quickCodeMap.get("cDefaultValue")));
                }
                if (CollectionUtils.isEmpty(fundCollectionChildList) ) {
                    fundCollectionChildQuickTypeSetting(bill, quickCode);
                } else {
                    for (FundCollection_b fundCollection_b : fundCollectionChildList) {
                        if (EventType.FundCollection.getValue() == Short.parseShort(bill.get("billtype").toString())) {
                            fundCollection_b.set("currency", bill.get("currency"));
                            fundCollection_b.set("currency_name", bill.get("currency_name"));
                            fundCollection_b.set("currency_priceDigit", bill.get("currency_priceDigit"));
                            fundCollection_b.set("currency_moneyDigit", bill.get("currency_moneyDigit"));
                            fundCollection_b.set("exchRate", 1);
                            fundCollection_b.set("exchangeRateType", bill.get("exchangeRateType"));
                            fundCollection_b.set("exchRate", bill.get("exchRate"));
                            fundCollection_b.set("exchangeRateOps", bill.get("exchangeRateOps"));
                            fundCollection_b.set("exchangeRateType_name", bill.get("exchangeRateType_name"));
                            fundCollection_b.set("exchangeRateType_digit", bill.get("exchangeRateType_digit"));
                            //换出汇率
                            fundCollection_b.set("settleCurrency", bill.get("currency"));
                            fundCollection_b.set("settleCurrency_name", bill.get("currency_name"));
                            fundCollection_b.set("settleCurrency_priceDigit", bill.get("currency_priceDigit"));
                            fundCollection_b.set("settleCurrency_moneyDigit", bill.get("currency_moneyDigit"));
                            fundCollection_b.set("swapOutExchangeRateEstimate", 1);
                            fundCollection_b.set("swapOutExchangeRateType", bill.get("swapOutExchangeRateType"));
                            fundCollection_b.set("swapOutExchangeRateType_name", bill.get("swapOutExchangeRateType_name"));
                            fundCollection_b.set("swapOutExchangeRateType_digit", bill.get("swapOutExchangeRateType_digit"));
                        }
                        if (quickCode != -999){
                            QuickTypeVO quickTypeVO = CmpCommonUtil.queryQuickTypeByCode(String.valueOf(quickCode));
                            if (ValueUtils.isNotEmptyObj(quickTypeVO)) {
                                fundCollection_b.setQuickType(quickTypeVO.getId());
                                fundCollection_b.set("quickType_name", quickTypeVO.getName());
                                fundCollection_b.set("quickType_code", quickTypeVO.getCode());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("未取到默认的款项类型! e = {}", e.getMessage());}
        }
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
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(ICmpConstant.CM_CMP_FUND_COLLECTION));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100393"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418083C","查询资金收款单交易类型失败！请检查数据。") /* "查询资金收款单交易类型失败！请检查数据。" */);
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
    private void fundCollectionChildQuickTypeSetting(BizObject bizObject, short quickCode) throws Exception {

        FundCollection_b fundCollection_b = new FundCollection_b();;
        if (EventType.FundCollection.getValue() == Short.parseShort(bizObject.get("billtype").toString())) {
            fundCollection_b = new FundCollection_b();
            fundCollection_b.set("currency", bizObject.get("currency"));
            fundCollection_b.set("currency_name", bizObject.get("currency_name"));
            fundCollection_b.set("currency_priceDigit", bizObject.get("currency_priceDigit"));
            fundCollection_b.set("currency_moneyDigit", bizObject.get("currency_moneyDigit"));
            fundCollection_b.set("exchRate", 1);
            fundCollection_b.set("exchangeRateType", bizObject.get("exchangeRateType"));
            fundCollection_b.set("exchRate", bizObject.get("exchRate"));
            fundCollection_b.set("exchangeRateOps", bizObject.get("exchangeRateOps"));
            fundCollection_b.set("exchangeRateType_name", bizObject.get("exchangeRateType_name"));
            fundCollection_b.set("exchangeRateType_digit", bizObject.get("exchangeRateType_digit"));

            fundCollection_b.set("settleCurrency", bizObject.get("currency"));
            fundCollection_b.set("settleCurrency_name", bizObject.get("currency_name"));
            fundCollection_b.set("settleCurrency_priceDigit", bizObject.get("currency_priceDigit"));
            fundCollection_b.set("settleCurrency_moneyDigit", bizObject.get("currency_moneyDigit"));
            fundCollection_b.set("swapOutExchangeRateEstimate", 1);
            fundCollection_b.set("swapOutExchangeRateType", bizObject.get("swapOutExchangeRateType"));
            fundCollection_b.set("swapOutExchangeRateType_name", bizObject.get("swapOutExchangeRateType_name"));
            fundCollection_b.set("swapOutExchangeRateType_digit", bizObject.get("swapOutExchangeRateType_digit"));
        }
        if(quickCode != -999){
            QuickTypeVO quickTypeVO = CmpCommonUtil.queryQuickTypeByCode(String.valueOf(quickCode));
            if (ValueUtils.isNotEmptyObj(quickTypeVO)) {
                fundCollection_b.setQuickType(quickTypeVO.getId());
                fundCollection_b.set("quickType_name", quickTypeVO.getName());
                fundCollection_b.set("quickType_code", quickTypeVO.getCode());
            }
        }
        bizObject.set("FundCollection_b", Collections.singletonList(fundCollection_b));

    }

}
