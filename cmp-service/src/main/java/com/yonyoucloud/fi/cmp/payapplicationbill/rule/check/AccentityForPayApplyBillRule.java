package com.yonyoucloud.fi.cmp.payapplicationbill.rule.check;


import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.QuickType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h1>会计主体check规则，并为部分字段赋初始值</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-13 15:24
 */
@Component("accentityForPayApplyBillRule")
public class AccentityForPayApplyBillRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private CmCommonService cmCommonService;
    private static final String QUICKTYPEMAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper";
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!"accentity_name".equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        if (SourceMatters.ManualInput.getValue() != Short.parseShort(bill.get("srcitem").toString())) {
            return new RuleExecuteResult();
        }
        bill.set("org", bill.get(IBussinessConstant.ACCENTITY));
        bill.set("org_name", bill.get("accentity_name"));
        String accEntityId = bill.get(IBussinessConstant.ACCENTITY);
        if (accEntityId == null) {
            return new RuleExecuteResult();
        }
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);
        if (accEntity.size() == 0) {
            return new RuleExecuteResult();
        }
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(accEntity.get(0).get("currency").toString());
        if (currencyTenantDTO == null) {
            return new RuleExecuteResult();
        }

        bill.set("currency", currencyTenantDTO.getId());
        bill.set("currency_name", currencyTenantDTO.getName());
        bill.set("currency_priceDigit", currencyTenantDTO.getPricedigit());
        bill.set("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
        bill.set("natCurrency", currencyTenantDTO.getId());
        bill.set("natCurrency_name", currencyTenantDTO.getName());
        bill.set("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
        bill.set("natCurrency_moneyDigit",currencyTenantDTO.getMoneydigit());
        bill.set("exchRate", 1);
        if (SourceMatters.ManualInput.getValue() == Short.parseShort(bill.get("srcitem").toString())) {
            if (!ValueUtils.isNotEmptyObj(bill.get("tradetype"))) {
                Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById("FICM3", "1", null);
                if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                    bill.set("tradetype", tradetypeMap.get("id"));
                    bill.set("tradetype_name", tradetypeMap.get("name"));
                    bill.set("tradetype_code", tradetypeMap.get("code"));
                }
            }
        }

        if(null != bill.get("payApplicationBill_b")){
            List<PayApplicationBill_b> payApplicationBill_bList = bill.getBizObjects("payApplicationBill_b",PayApplicationBill_b.class);
            if(CollectionUtils.isEmpty(payApplicationBill_bList)){
                CtmJSONObject tpljson = CtmJSONObject.parseObject( paramMap.get("requestData") ==null? null :paramMap.get("requestData").toString());
                Map<String,Object> param = new HashMap<>();
                if (tpljson!=null && tpljson.containsKey("currenttplid")){
                    param.put("tplid", tpljson.getLong("currenttplid"));
                }
                param.put("tenantid", AppContext.getTenantId());
                HashMap<String,Object> quickCode = SqlHelper.selectOne(QUICKTYPEMAPPER+".getPayApplyBillQuickTypeCode",param);
                if(null != quickCode && null != quickCode.get("cDefaultValue") && !"".equals(quickCode.get("cDefaultValue"))){
                    payApplyBillBodyQuickTypeSetting(bill, Short.parseShort(String.valueOf(quickCode.get("cDefaultValue"))));
                }else{
                    payApplyBillBodyQuickTypeSetting(bill, QuickType.sundry.getValue());
                }
            }
        }

        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

    /**
     * 给单据默认添加一行带款项类型得子表
     * @param bizObject
     * @throws Exception
     */
    private void payApplyBillBodyQuickTypeSetting(BizObject bizObject,short quickCode) throws Exception {
        PayApplicationBill_b payApplicationBillB = new PayApplicationBill_b();
        List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(quickCode)));
        if (quickTypeMap.size() > 0) {
            //停用的款项类型不添加
            if(!MapUtils.getBoolean(quickTypeMap.get(0), "stopstatus")){
                payApplicationBillB.setQuickType(MapUtils.getLong(quickTypeMap.get(0), "id"));
                payApplicationBillB.set("quickType_name", MapUtils.getString(quickTypeMap.get(0), "name"));
                payApplicationBillB.set("quickType_code", MapUtils.getString(quickTypeMap.get(0), "code"));
            }
            bizObject.set("payApplicationBill_b", Arrays.asList(payApplicationBillB));
        }
    }
}
