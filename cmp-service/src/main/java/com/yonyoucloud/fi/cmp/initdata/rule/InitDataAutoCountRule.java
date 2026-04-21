package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InitDataAutoCountRule extends AbstractCommonRule {

	@Autowired
	BaseRefRpcService baseRefRpcService;
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		BillDataDto billDataDto = (BillDataDto)this.getParam(paramMap);
		List<BizObject> bills = getBills(billContext, paramMap);
		CtmJSONObject item = CtmJSONObject.parseObject(billDataDto.getItem());
		if (bills != null && bills.size()>0) {
			BizObject bizObject = (BizObject) bills.get(0);
            String currency = bizObject.getString("currency");
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
			if ("exchangeRateType_name".equals(item.get("key"))){
				if (bizObject.get("exchangeRateType")==null){
					return new RuleExecuteResult();
				}
				if (bizObject.get("currency").equals(bizObject.get("natCurrency"))){
					bizObject.set("exchangerate", 1);
					return new RuleExecuteResult();
				}
                // 汇率类型为自定义汇率不寻汇
                ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateTypeById(bizObject.get("exchangeRateType").toString());
                if ("02".equals(exchangeRateTypeVO.getCode())) {
                    return new RuleExecuteResult();
                }
                Date accountdate = bizObject.getDate("accountdate");
                try {
                    //当原币 本币 不相同时重新查询汇率
                    CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bizObject.get("currency"), bizObject.get("natCurrency"), accountdate, bizObject.get("exchangeRateType"), currencyTenantDTO.getMoneydigit());
                    bizObject.put("exchangerate", cmpExchangeRateVO.getExchangeRate());
                    bizObject.put("exchRateOps", cmpExchangeRateVO.getExchangeRateOps());
                } catch (Exception e) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21BA2B6A05780006", "无法获取币种[%s]到[%s]的汇率：未查询对应币种可用汇率或者汇率超期一年汇率，请检查") /* "无法获取币种[%s]到[%s]的汇率：未查询对应币种可用汇率或者汇率超期一年汇率，请检查" */,
                            bizObject.getString("currency_name"), bizObject.getString("natCurrency_name")));
                }
			}else {
				if ("exchangerate".equals(item.get("key"))){
					if (bizObject.get("exchangeRateType_name") == null || !bizObject.get("exchangeRateType_name").equals(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C3","用户自定义汇率") /* "用户自定义汇率" */)) {
						ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateRateTypeByCode("02");
						if (exchangeRateTypeVO != null){
							bizObject.put("exchangeRateType",exchangeRateTypeVO.getId());
							bizObject.put("exchangeRateType_name",exchangeRateTypeVO.getName());
							bizObject.put("exchangeRateType_digit",exchangeRateTypeVO.getDigit());
						}
					}
				}
			}
			// 获取企业方本币余额
			getCoinitLocalBalance(bizObject);
		}
		return new RuleExecuteResult();
	}

	/**
	 * 获取企业方本币余额
	 * @param bizObject
	 * @throws Exception
	 */
	private void getCoinitLocalBalance(BizObject bizObject) throws Exception {
		String currency = bizObject.getString("currency");
		Object coinitloribalanceObj = bizObject.get("coinitloribalance");
		Object exchangerateObj = bizObject.get("exchangerate");
		if (StringUtils.isEmpty(currency) || coinitloribalanceObj == null || exchangerateObj == null) {
			log.error("getCoinitLocalBalance currency{} or coinitloribalanceObj{} is null or exchangerateObj is null",
					currency, coinitloribalanceObj, exchangerateObj);
			return;
		}
		// 获取汇率
		BigDecimal exchRate = new BigDecimal(exchangerateObj.toString());
		BigDecimal coinitloribalance = new BigDecimal(coinitloribalanceObj.toString());
        // 获取本币币种
		CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bizObject.getString("natCurrency"));
		if (currencyTenantDTO != null) {
            BigDecimal coinitlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(bizObject.getShort("exchRateOps"),  exchRate, coinitloribalance,  currencyTenantDTO.getMoneydigit());
            bizObject.put("coinitlocalbalance", coinitlocalbalance);
		}
	}

}
