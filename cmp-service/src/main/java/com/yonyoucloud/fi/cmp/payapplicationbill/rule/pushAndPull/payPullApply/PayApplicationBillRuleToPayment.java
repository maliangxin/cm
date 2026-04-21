package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.payPullApply;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.arap.EventSource;
import com.yonyoucloud.fi.cmp.arap.EventType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IMultilangConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IBussinessConstant.ACCENTITY;

/**
 * <h1>付款申请工作台拉取付款申请单前置规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/7/9 13:55
 */
@Slf4j
@Component("payApplicationBillRuleToPayment")
public class PayApplicationBillRuleToPayment extends AbstractCommonRule {
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> omakes = (List) paramMap.get("omake");
        log.info("PayApplicationBillRuleToPayment, omakes = {}", omakes);
        Iterator<Map<String, Object>> iterator = omakes.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> map = iterator.next();
            assembleInfo(map);
            //事项类型
            String accentity = map.get(ACCENTITY).toString();
            map.put("billtype", EventType.PayApplyBill.getValue());
            Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);
            if (enabledBeginData == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100355"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180656","该会计主体现金管理模块未启用，不能保存单据！") /* "该会计主体现金管理模块未启用，不能保存单据！" */);
            }
            List<Map<String, Object>> payApplicationBill_b = (List) map.get("payApplicationBill_b");
            Iterator<Map<String, Object>> iter = payApplicationBill_b.iterator();
            while (iter.hasNext()) {
                Map<String, Object> map_sub = iter.next();
                BigDecimal paymentPreemptAmount = (BigDecimal) map_sub.get("paymentPreemptAmount");
                BigDecimal paymentApplyAmount = (BigDecimal) map_sub.get("paymentApplyAmount");
                if (null == map_sub.get("unpaidAmount")) {
                    iter.remove();
                }
                if (null != paymentPreemptAmount && paymentPreemptAmount.equals(paymentApplyAmount)) {
                    iter.remove();
                }
                if (null != map_sub.get("paymentPreemptAmount")) {
                    map_sub.put("paymentApplyAmount", paymentApplyAmount.subtract(paymentPreemptAmount));
                }
            }
            //不是自动生单 autobill
            map.put("autobill", false);
            //来源单据id
            map.put("srcbillid", map.get("id"));
        }
        paramMap.put("omake", omakes);
        return new RuleExecuteResult();
    }

    /**
     * 组装数据
     *
     * @param map
     * @throws Exception
     */
    private void assembleInfo(Map<String, Object> map) throws Exception {
        String accentity = map.get(IBussinessConstant.ACCENTITY).toString();
        String currency = map.get("currency").toString();

        //事项来源
        map.put("srcitem", EventSource.Cmpchase.getValue());

        //交易类型id 	如果生成应收收款单，取应收收款单默认交易类型； 如果生成现金收款单，取现金收款单默认交易类型； 可改
        Map<String, Object> queryTransType = cmCommonService.queryTransTypeById("FICM2", "1", null);
        if (ValueUtils.isNotEmptyObj(queryTransType)) {
            map.put("tradetype_id", queryTransType.get("id"));
            map.put("tradetype_name", queryTransType.get("name"));
            map.put("tradetype_code", queryTransType.get("code"));
        } else {
            map.put("tradetype_id", null);
            map.put("tradetype_name", null);
            map.put("tradetype_code", null);
        }

        //本币币种
        //本币币种
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
        if (accEntity.size() != 0) {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(accEntity.get(0).get("currency").toString());
            if (currencyTenantDTO != null) {
                map.put("natCurrency", currencyTenantDTO.getId());
                map.put("natCurrency_name", currencyTenantDTO.getName());
                map.put("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                map.put("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }
        } else {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
            map.put("natCurrency", currencyTenantDTO.getId());
            map.put("natCurrency_name", currencyTenantDTO.getName());
        }

        //汇率类型-----前端js事件 人民币变化是修改汇率类型
        Map<String, Object> defaultExchangeRateType = null;
        defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accentity);
        if (defaultExchangeRateType.isEmpty() || defaultExchangeRateType.get("id") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100356"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180657","此会计主体下无默认汇率类型，请检查数据！") /* "此会计主体下无默认汇率类型，请检查数据！" */);
        }
        map.put("exchangeRateType", defaultExchangeRateType.get("id").toString());

        //汇率
        getExchRate(map, defaultExchangeRateType);

        //结算方式
        if (! ValueUtils.isNotEmptyObj(map.get("settlemode"))) {
            QuerySchema querySchema = QuerySchema.create().addSelect("settlemode,settlemode.name as settlemodeName ");
            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity)));
            List<Map<String, Object>> autoConfigList = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema);
            if (!CollectionUtils.isEmpty(autoConfigList)) {
                map.put("settlemode", autoConfigList.get(0).get("settlemode"));
                map.put("settlemode_name", autoConfigList.get(0).get("settlemodeName"));
            }
        }
    }

    /**
     * 获取汇率
     *
     * @param map
     * @param defaultExchangeRateType
     * @throws Exception
     */
    private void getExchRate(Map<String, Object> map, Map<String, Object> defaultExchangeRateType) throws Exception {
        String accentityes = map.get(IBussinessConstant.ACCENTITY).toString();
        String currencyes = map.get("currency").toString();
        //本币金额
        BigDecimal creditamount = BigDecimal.ZERO;
        if (!StringUtils.isEmpty(accentityes) && !StringUtils.isEmpty(currencyes)) {
            String orgCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(accentityes);
            if (orgCurrency != null) {
                if (currencyes.equals(orgCurrency)) {
                    map.put("exchRate", BigDecimal.valueOf(1));
                    if (null != map.get(IBussinessConstant.ORI_SUM)) {
                        creditamount = new BigDecimal(map.get(IBussinessConstant.ORI_SUM).toString());
                        map.put("natSum", creditamount.multiply(BigDecimal.valueOf(1)));
                    }
                } else {
                    if (defaultExchangeRateType != null && defaultExchangeRateType.get("id") != null) {
                        try {
                            Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get("id").toString(), currencyes, orgCurrency, new Date(), 6);
                            if (currencyRateNew == null || currencyRateNew == 0) {
                                throw IMultilangConstant.noRateError/*未取到汇率 */;
                            }
                            map.put("exchRate", BigDecimal.valueOf(currencyRateNew));
                            if (null != map.get(IBussinessConstant.ORI_SUM)) {
                                creditamount = new BigDecimal(map.get(IBussinessConstant.ORI_SUM).toString());
                                map.put("natSum", creditamount.multiply(BigDecimal.valueOf(currencyRateNew)));
                            }
                        } catch (Exception e) {
                            log.error("==================》取不到汇率", e);
                            throw IMultilangConstant.noRateError/*未取到汇率 */;
                        }
                    }
                }
            }
        }
    }

}
