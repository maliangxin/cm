package com.yonyoucloud.fi.cmp.accountdetailexclusion.business;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 返回默认币种人民币
 *
 * @author jpk
 * @version 1.0
 */
@Component("accountDetailExclusionCheckAccentityRule")
public class AccountdetailexclusionCheckAccentityRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isNotEmpty(bills)) {
            BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
            AccountDetailExclusion AccountDetailExclusion = (AccountDetailExclusion) bills.get(0);
            JSONObject item = JSONObject.parseObject(billDataDto.getItem());
            // 字段名
            String key = (String) item.get("key");
            switch (key) {
                case "accentity_name":
                    if (StringUtils.isEmpty(AccountDetailExclusion.getAccentity())) {
                        AccountDetailExclusion.put("currency", null);
                        AccountDetailExclusion.put("currency_name", null);
                        AccountDetailExclusion.put("currency_moneyDigit", null);
                        return new RuleExecuteResult();
                    }
                    queryCurrency(AccountDetailExclusion);
                    break;
                default:
                    return new RuleExecuteResult();
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 获取币种信息
     *
     * @param AccountDetailExclusion
     * @throws Exception
     */
    private void queryCurrency(AccountDetailExclusion AccountDetailExclusion) throws Exception {
        Map<String, Object> condition = new HashMap<>(8);
        condition.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056D", "人民币") /* "人民币" */);
        List<Map<String, Object>> currencyByName = QueryBaseDocUtils.queryCurrencyByCondition(condition);
        Map<String, Object> map = CollectionUtils.isEmpty(currencyByName) ? Maps.newHashMap() : currencyByName.get(0);
        CurrencyTenantDTO convertCurrency = baseRefRpcService.queryCurrencyById(String.valueOf(map.get("id")));
        if (Objects.isNull(convertCurrency)) {
            return;
        }
        AccountDetailExclusion.put("currency", convertCurrency.getId());
        AccountDetailExclusion.put("currency_name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056D", "人民币") /* "人民币" */);
        AccountDetailExclusion.put("currency_moneyDigit", convertCurrency.getMoneydigit());
    }

}
