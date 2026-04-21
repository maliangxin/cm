package com.yonyoucloud.fi.cmp.initprojectdata;


import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

/**
 * <h1>卡片页面为币种赋值</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-13 15:24
 */
public class AccentityForInitProjectDataRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!"accentity_name".equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        String accEntityId = bill.get("accentity");
        if(accEntityId == null){
            return new RuleExecuteResult();
        }
        FundsOrgDTO fundsOrg = AccentityUtil.getFundsOrgDTOByAccentityId(accEntityId);
        if (fundsOrg == null) {
            return new RuleExecuteResult();
        }
        bill.set("accentity_code", fundsOrg.getCode());

        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);
        List<Map<String, Object>> natCurrency = QueryBaseDocUtils.queryCurrencyById(accEntity.get(0).get("currency"));
        if (natCurrency.size() == 0) {
            return new RuleExecuteResult();
        }
        bill.set("currency", natCurrency.get(0).get("id"));
        bill.set("currency_name", natCurrency.get(0).get("name"));
        bill.set("currency_priceDigit", natCurrency.get(0).get("priceDigit"));
        bill.set("currency_moneyDigit", natCurrency.get(0).get("moneyDigit"));
        bill.set("natCurrency", natCurrency.get(0).get("id"));
        bill.set("natCurrency_name", natCurrency.get(0).get("name"));
        bill.set("natCurrency_priceDigit", natCurrency.get(0).get("priceDigit"));
        bill.set("natCurrency_moneyDigit", natCurrency.get(0).get("moneyDigit"));
        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }
}
