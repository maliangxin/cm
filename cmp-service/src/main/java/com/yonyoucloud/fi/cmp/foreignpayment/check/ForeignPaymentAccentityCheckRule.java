package com.yonyoucloud.fi.cmp.foreignpayment.check;


import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * <h1>外汇付款会计主体检查</h1>
 *
 * @author xuxbo
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ForeignPaymentAccentityCheckRule extends AbstractCommonRule {
    private final CmCommonService cmCommonService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    //@Autowired
//    OrgRpcService orgRpcService;


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
        bill.set("org", accEntityId);
        bill.set("org_name", bill.get("accentity_name"));

        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);
        if (finOrgDTO == null) {
            return new RuleExecuteResult();
        }

        Date periodFirstDate = null;
        try {
            periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accEntityId);
            if (periodFirstDate == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100766"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180483", "模块未启用") /* "模块未启用" */);
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100766"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180483", "模块未启用") /* "模块未启用" */);
        }

        //根据 forimid 查询单据类型
        BdBillType bdBillType = baseRefRpcService.queryBillTypeByFormId(ICmpConstant.CM_CMP_FOREIGNPAYMENT);
        if (Objects.isNull(bdBillType)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100767"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC6B7204F80003", "查询支付保证金台账管理单据类型失败！") /* "查询支付保证金台账管理单据类型失败" */);
        }
        String billTypeId = bdBillType.getId();
        String billTypeName = bdBillType.getName();
        bill.set("billtype", billTypeId);
        bill.set("billtype_name", billTypeName);
        try {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
            if (currencyTenantDTO != null) {
                // 设置币种
//                bill.set("currency", currencyTenantDTO.getId());
//                bill.set("currency_name", currencyTenantDTO.getName());
                //bill.set("currency_priceDigit", currencyTenantDTO.getPricedigit());
                bill.set("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
                bill.set("natCurrency", currencyTenantDTO.getId());
                bill.set("natCurrency_name", currencyTenantDTO.getName());
                //bill.set("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                bill.set("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
//                bill.set("exchRate", 1);
            }
        } catch (Exception e) {
            log.error("未取到本币币种！ e = {}", e.getMessage());
        }

        // 设置汇率类型为会计主体默认汇率
//        ExchangeRateTypeVO exchangeRateTypeVO = cmCommonService.getExchangeRateType(accEntityId);
        ExchangeRateTypeVO exchangeRateTypeVOByFinOrg = CmpExchangeRateUtils.getNewExchangeRateType(accEntityId, true);
        if (StringUtils.isNotEmpty(exchangeRateTypeVOByFinOrg.getId())) {
            bill.set("currencyexchangeratetype", exchangeRateTypeVOByFinOrg.getId());
            bill.set("currencyexchangeratetype_name", exchangeRateTypeVOByFinOrg.getName());
            bill.set("currencyexchangeratetype_digit", exchangeRateTypeVOByFinOrg.getDigit());
            bill.set("currencyexchangeratetype_code", exchangeRateTypeVOByFinOrg.getCode());
        }
        //换出汇率
        ExchangeRateTypeVO exchangeRateTypeVOByFundsOrg = CmpExchangeRateUtils.getNewExchangeRateType(accEntityId, false);
        if (StringUtils.isNotEmpty(exchangeRateTypeVOByFundsOrg.getId())) {
            bill.set("swapOutExchangeRateType", exchangeRateTypeVOByFundsOrg.getId());
            bill.set("swapOutExchangeRateType_name", exchangeRateTypeVOByFundsOrg.getName());
            bill.set("swapOutExchangeRateType_digit", exchangeRateTypeVOByFundsOrg.getDigit());
            bill.set("swapOutExchangeRateType_code", exchangeRateTypeVOByFundsOrg.getCode());

        }

//        if (ObjectUtils.isNotEmpty(exchangeRateTypeVO)) {
//            bill.set("currencyexchangeratetype", exchangeRateTypeVO.getId());
//            bill.set("currencyexchangeratetype_name", exchangeRateTypeVO.getName());
//            bill.set("currencyexchangeratetype_digit", exchangeRateTypeVO.getDigit());
//            bill.set("currencyexchangeratetype_code", exchangeRateTypeVO.getCode());
//
//            bill.set("swapOutExchangeRateType", exchangeRateTypeVO.getId());
//            bill.set("swapOutExchangeRateType_name", exchangeRateTypeVO.getName());
//            bill.set("swapOutExchangeRateType_digit", exchangeRateTypeVO.getDigit());
//            bill.set("swapOutExchangeRateType_code", exchangeRateTypeVO.getCode());
//        }
        // 设置交易类型
        if (!ValueUtils.isNotEmptyObj(bill.get("tradetype"))) {
            setValueForTransType(bill);
        }
//        if (!billTypeId.equals(bill.get("billtype"))) {
//            this.putParam(paramMap, "return", bill);
//            return new RuleExecuteResult();
//        }

        //设置默认值
        //设置是否跨境：否
        if (bill.get("iscrossborder").equals((short)1)) {
            bill.set("iscrossborder", 1);
        } else {
            bill.set("iscrossborder", 0);
        }

        if (bill.get("isurgent").equals((short)1)) {
            bill.set("isurgent", 1);
        } else {
            bill.set("isurgent", 0);
        }
        //是否通过代理行 isagencybank
        if (bill.get("isagencybank").equals((short)1)) {
            bill.set("isagencybank", 1);
        } else {
            bill.set("isagencybank", 0);
        }
        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
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
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(ICmpConstant.CM_CMP_FOREIGNPAYMENT));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100768"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC6B0405080003", "查询支付保证金交易类型失败！请检查数据。") /* "查询支付保证金交易类型失败！请检查数据。" */);
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
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102299"),"未获取到默认的交易类型！");
        }
    }
}
