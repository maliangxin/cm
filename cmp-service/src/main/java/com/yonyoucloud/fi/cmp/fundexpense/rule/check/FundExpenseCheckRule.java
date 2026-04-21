package com.yonyoucloud.fi.cmp.fundexpense.rule.check;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import com.yonyoucloud.fi.cmp.enums.SourceBillTypeEnum;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("fundExpenseCheckRule")
public class FundExpenseCheckRule extends AbstractCommonRule {

    @Autowired
    private OrgRpcService orgRpcService;
    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        List<BizObject> bills = getBills(billContext, paramMap);
        Fundexpense fundexpense = (Fundexpense) bills.get(0);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if (billDataDto != null && billDataDto.getItem() != null) {
            CtmJSONObject itemObj = CtmJSONObject.parseObject(billDataDto.getItem());
            String org = fundexpense.get("org");
            if ("org_name".equals(itemObj.getString("key")) && org != null) {
                // 资金组织校验、会计主体赋值、本币币种赋值
                List<Map<String, Object>> initSetting = QueryBaseDocUtils.queryOrgBpOrgConfVO(org, ISystemCodeConstant.ORG_MODULE_CM);
                if (CollectionUtils.isEmpty(initSetting)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100162"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E7B005900015", "当前资金组织的期初期间未启用！") /* "当前资金组织的期初期间未启用！" */);
                }
                fundexpense.put("accentity", org);
                fundexpense.put("accentity_code", fundexpense.get("org_code"));
                fundexpense.put("accentity_name", fundexpense.get("org_name"));
                // 资金组织对应的会计主体组织本币币种
                FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(org);
                if (finOrgDTO != null) {
                    CurrencyTenantDTO natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
                    if (natCurrencyTenantDTO != null) {
                        fundexpense.put("natCurrency", natCurrencyTenantDTO.getId());
                        fundexpense.put("natCurrency_name", natCurrencyTenantDTO.getName());
                        fundexpense.put("natCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
                        fundexpense.put("natCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
                    }
                }
            } else if ("expensedate".equals(itemObj.getString("key")) && fundexpense.get("expensedate") != null) {
                // 费用日期校验
                if (org == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100163"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E7B005900016", "请先录入设置期初期间的资金组织！") /* "请先录入设置期初期间的资金组织！" */);
                } else {
                    Date initDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(org, ISystemCodeConstant.ORG_MODULE_CM);
                    if (DateUtils.dateCompare(initDate, fundexpense.get("expensedate")) > 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100164"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E7B005900014", "费用日期不能小于该模块启用日期:{0}！") /* "费用日期不能小于该模块启用日期:{0}！" */, sdf.format(initDate)));
                    }
                }
            } else if ("exchRate".equals(itemObj.getString("key"))) {
                // 本币币种汇率修改，本币币种汇率类型修改为“用户自定义汇率”
                if (fundexpense.get("exchangeRateType_name") == null || !fundexpense.get("exchangeRateType_name").equals(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C3", "用户自定义汇率") /* "用户自定义汇率" */)) {
                    ExchangeRateTypeVO exchangeRateTypeVO = CmpExchangeRateUtils.getUserDefineExchangeRateType();
                    if (exchangeRateTypeVO != null) {
                        fundexpense.put("exchangeRateType", exchangeRateTypeVO.getId());
                        fundexpense.put("exchangeRateType_code", exchangeRateTypeVO.getCode());
                        fundexpense.put("exchangeRateType_name", exchangeRateTypeVO.getName());
                        fundexpense.put("exchangeRateType_digit", exchangeRateTypeVO.getDigit());
                    }
                }
            } else if ("expense_b_currency_exchRate".equals(itemObj.getString("key"))) {
                // 子表费用币种汇率修改，费用币种汇率类型修改为“用户自定义汇率”
                if (fundexpense.get("detail") != null) {
                    int location = itemObj.getIntValue("location");
                    List<Map<String, Object>> fundexpense_bList = (List<Map<String, Object>>) fundexpense.get("detail");
                    Map<String, Object> fundexpense_b = fundexpense_bList.get(location);
                    if (fundexpense_b.get("expense_b_currency_exchangeRateType_name") == null || !fundexpense_b.get("expense_b_currency_exchangeRateType_name").equals(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C3", "用户自定义汇率") /* "用户自定义汇率" */)) {
                        ExchangeRateTypeVO exchangeRateTypeVO = CmpExchangeRateUtils.getUserDefineExchangeRateType();
                        if (exchangeRateTypeVO != null) {
                            BizObject newDetail = new BizObject();
                            newDetail.put("_realtype", fundexpense_b.get("_realtype"));
                            newDetail.put("_entityName", fundexpense_b.get("_entityName"));
                            newDetail.put("_keyName", fundexpense_b.get("_keyName"));
                            newDetail.put("hasDefaultInit", fundexpense_b.get("hasDefaultInit"));
                            newDetail.put("ttt_id", fundexpense_b.get("ttt_id"));
                            newDetail.put("_status", fundexpense_b.get("_status"));
                            newDetail.put("expense_b_currency_exchangeRateType", exchangeRateTypeVO.getId());
                            newDetail.put("expense_b_currency_exchangeRateType_code", exchangeRateTypeVO.getCode());
                            newDetail.put("expense_b_currency_exchangeRateType_name", exchangeRateTypeVO.getName());
                            fundexpense_bList.set(location, newDetail);
                        }
                    }
                }
            } else if ("srcbillno_name".equals(itemObj.getString("key"))) {
                // 子表根据来源业务带出分摊开始日期、分摊结束日期
                int location = itemObj.getIntValue("location");
                List<Map<String, Object>> fundexpense_bList = (List<Map<String, Object>>) fundexpense.get("detail");
                Map<String, Object> fundexpense_b = fundexpense_bList.get(location);
                if (fundexpense_b.get("srcbillno") == null && fundexpense_b.get("srcbilltype") == null) {
                    String id = fundexpense_b.get("srcbillno").toString();
                    String srcbilltype = fundexpense_b.get("srcbilltype").toString();
                    Map<String, Date> contractDateMap = queryContractDate(id, srcbilltype);
                    if (contractDateMap != null) {
                        fundexpense_bList.get(location).put("share_startdate", contractDateMap.get("startDate") != null ? contractDateMap.get("startDate") : null);
                        fundexpense_bList.get(location).put("share_endate", contractDateMap.get("endDate") != null ? contractDateMap.get("endDate") : null);
                    }
                }
            }
            this.putParam(paramMap, "return", fundexpense);
        }
        return ruleResult;
    }

    /**
     * 根据不同业务来源查询合同开始日期、结束日期
     *
     * @param id
     * @param srcbilltype
     * @return
     * @throws Exception
     */
    private Map<String, Date> queryContractDate(String id, String srcbilltype) throws Exception {
        Map<String, Date> contractDateMap = new HashMap<>();
        SourceBillTypeEnum sourceBillTypeEnum = SourceBillTypeEnum.getSouceBillTypeByValue(srcbilltype);
        if (sourceBillTypeEnum.getStartDateField() != "" && sourceBillTypeEnum.getEndDateField() != "") {
            QuerySchema schema = QuerySchema.create().addSelect(sourceBillTypeEnum.getStartDateField(), sourceBillTypeEnum.getEndDateField());
            schema.appendQueryCondition(QueryCondition.name("id").eq(id));
            List<Map<String, Object>> query_result = MetaDaoHelper.query(sourceBillTypeEnum.getFullName(), schema, sourceBillTypeEnum.getDomain());
            if (CollectionUtils.isNotEmpty(query_result)) {
                Map<String, Object> res = query_result.get(0);
                if (res.get(sourceBillTypeEnum.getStartDateField()) != null) {
                    contractDateMap.put("startDate", (Date) res.get(sourceBillTypeEnum.getStartDateField()));
                }
                if (res.get(sourceBillTypeEnum.getEndDateField()) != null) {
                    contractDateMap.put("endDate", (Date) res.get(sourceBillTypeEnum.getEndDateField()));
                }
            }
        }
        return contractDateMap;
    }
}
