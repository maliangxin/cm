package com.yonyoucloud.fi.cmp.cashinventory.check;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TranstypeQueryPageParam;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @since 2022-01-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAccentityCheckRule extends AbstractCommonRule {

    //@Autowired
//    OrgRpcService orgRpcService;

    private final CmCommonService cmCommonService;

    private final BaseRefRpcService baseRefRpcService;


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
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);
        if (finOrgDTO==null) {
            return new RuleExecuteResult();
        }

        try {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
            if (currencyTenantDTO != null) {
                // 设置币种
                bill.set("natCurrency", currencyTenantDTO.getId());
                bill.set("natCurrency_name",currencyTenantDTO.getName());
                bill.set("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                bill.set("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101804"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418055D","未取到本币币种！") /* "未取到本币币种！" */);
        }

        // 设置汇率类型
        Map<String, Object> defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accEntityId);
//        ExchangeRateTypeVO defaultExchangeRateType = baseRefRpcService.queryDefaultExchangeRateType();
        if (defaultExchangeRateType != null && defaultExchangeRateType.get("id") != null) {
            bill.set("exchangeRateType", defaultExchangeRateType.get("id"));
            bill.set("exchangeRateType_name", defaultExchangeRateType.get("name"));
            bill.set("exchangeRateType_digit", defaultExchangeRateType.get("digit"));
        }

        // 设置交易类型
        BdBillType billType = baseRefRpcService.queryBillTypeByFormId(ICmpConstant.CM_CMP_INVENTORY);
        String billTypeId = null;
        if (billType != null ) {
            billTypeId = billType.getId();
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101805"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418055E","查询现金盘点交易类型失败！请检查数据。") /* "查询现金盘点交易类型失败！请检查数据。" */);
        }
        TranstypeQueryPageParam params = new TranstypeQueryPageParam();
        params.setBillTypeId(billTypeId);
        params.setIsDefault(1);
        params.setTenantId(AppContext.getYTenantId());
        List<BdTransType> transTypes= baseRefRpcService.queryTransTypeByCondition(params);
        if(transTypes!=null && transTypes.size()>0){
            BdTransType transType = transTypes.get(0);
            bill.set("tradetype", transType.getId());
            bill.set("tradetype_name", transType.getName());
            bill.set("tradetype_code", transType.getCode());
        }

        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

}
