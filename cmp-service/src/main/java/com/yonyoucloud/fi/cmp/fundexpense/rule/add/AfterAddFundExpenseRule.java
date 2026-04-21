package com.yonyoucloud.fi.cmp.fundexpense.rule.add;

import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.BillTypeQueryParam;
import com.yonyou.ucf.transtype.model.TransTypeQueryParam;
import com.yonyou.ucf.transtype.service.itf.IBillTypeService;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundexpense.constant.FundexpenseConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.tmsp.openapi.ITmspSystemRespRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemReq;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemResp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.biz.base.BizContext;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("afterAddFundExpenseRule")
public class AfterAddFundExpenseRule extends AbstractCommonRule {

    @Autowired
    private ITmspSystemRespRpcService iTmspSystemRespRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            // 1.查询财资公共-集成参数-现金管理-资金费用-结算处理：0-资金结算 1-应收应付 null-不集成，为“费用参数”赋值
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            // 查询财资公共-集成参数-现金管理-资金费用-结算处理：0-资金结算 1-应收应付 null-不集成，为“费用参数”赋值
            TmspSystemReq tmspSystemReq = new TmspSystemReq();
            tmspSystemReq.setApplyname("6");
            // TODO: 2024/9/4 需要修改为资金费用的servicename 
            tmspSystemReq.setServicename("162");
            List<TmspSystemResp> tmspSystemResp = iTmspSystemRespRpcService.querySystemParameters(tmspSystemReq);
            if (CollectionUtils.isNotEmpty(tmspSystemResp)) {
                TmspSystemResp collectionParam = tmspSystemResp.get(0);
                if (collectionParam.getSettlementprocessingmode() != null && !"".equals(collectionParam.getSettlementprocessingmode())) {
                    fundexpense.set("expenseparam", Short.valueOf(collectionParam.getSettlementprocessingmode()));
                } else {
                    fundexpense.set("expenseparam", (short) 2);
                }
            }
            // 2.查询默认汇率类型，为“本币币种汇率类型”赋值
            Map<String, Object> condition = new HashMap<>();
            condition.put("isDefault", 1);
            List<Map<String, Object>> exchangeRateTypeList = QueryBaseDocUtils.queryExchangeRateTypeByCondition(condition);
            if (exchangeRateTypeList != null) {
                Map<String, Object> defautExchangeRateType = exchangeRateTypeList.get(0);
                if (defautExchangeRateType.get("id") != null) {
                    fundexpense.set("exchangeRateType", defautExchangeRateType.get("id"));
                }
                if (defautExchangeRateType.get("name") != null) {
                    fundexpense.set("exchangeRateType_name", defautExchangeRateType.get("name"));
                }
            }

            if(ObjectUtils.isEmpty(fundexpense.get("bustype")) && ObjectUtils.isEmpty(fundexpense.get("bustype_name"))){
                //根据单据类型编码，设置默认交易类型
                Map<String, Object> tradeType = queryTransTypeByBillTypeCode(billContext.getBillnum());
                if (tradeType != null && !tradeType.isEmpty()) {
                    fundexpense.put("bustype",tradeType.get("id").toString());
                    fundexpense.put("bustype_name",tradeType.get("name"));
                }
            }
            JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
            String json = formatter.toJson(fundexpense, billContext.getFullname(), true).toString();
            return new RuleExecuteResult(json);
        }
        return new RuleExecuteResult();
    }


    public static Map<String, Object> queryBillTypeCode(String billtypecode) throws Exception {
        IBillTypeService iBillTypeService = AppContext.getBean(IBillTypeService.class);
        BillTypeQueryParam billTypeQueryParam = new BillTypeQueryParam();
        billTypeQueryParam.setCode(billtypecode);
        billTypeQueryParam.setTenantId(AppContext.getYhtTenantId());
        List<BdBillType> bdBillTypes = iBillTypeService.queryBillTypes(billTypeQueryParam).stream().
                filter(bdBillType -> bdBillType.getDr().equals(0)).collect(Collectors.toList());
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(bdBillTypes)) {
            return Collections.EMPTY_MAP;
        }
        return obj2Map(bdBillTypes.get(0));
    }

    /**
     * 对象转Map, 仅支持 一层继承
     *
     * @param obj
     * @return
     * @throws IllegalAccessException
     */
    public static Map<String, Object> obj2Map(Object obj) throws IllegalAccessException {
        return CtmJSONObject.toJSON(obj);
    }

    /**
     * 根据单据类型编码查询所属交易类型
     *
     * @param billtypecode 单据类型编码
     * @return
     * @throws Exception
     */
    public static Map<String, Object> queryTransTypeByBillTypeCode(String billtypecode) throws Exception {

        Map<String, Object> billTypeMap = queryBillTypeCode(billtypecode);
        if (MapUtils.isEmpty(billTypeMap)) {
            return Collections.EMPTY_MAP;
        }
        ITransTypeService iTransTypeService = AppContext.getBean(ITransTypeService.class);
        TransTypeQueryParam transTypeQueryParam = new TransTypeQueryParam();
        transTypeQueryParam.setBillTypeId(String.valueOf(billTypeMap.get("id")));
        transTypeQueryParam.setTenantId(AppContext.getYhtTenantId());
        List<BdTransType> bdTransTypes = iTransTypeService.queryTransTypes(transTypeQueryParam).stream()
                .filter(bdTransType -> bdTransType.getDr().equals(0)).collect(Collectors.toList());
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(bdTransTypes)) {
            return Collections.EMPTY_MAP;
        }
        return obj2Map(bdTransTypes.get(0));
    }
}
