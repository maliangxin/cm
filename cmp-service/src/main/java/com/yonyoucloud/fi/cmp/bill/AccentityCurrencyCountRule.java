package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.FICurrencyRateService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.cmpentity.CurrencyRateTypeCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.ExchangeRateTypeQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AccentityCurrencyCountRule extends AbstractCommonRule {

	@Autowired
	BaseRefRpcService baseRefRpcService;

	@Autowired
	private CmCommonService cmCommonService;

    @Autowired
    FICurrencyRateService currencyRateService;

	//@Autowired
//	OrgRpcService orgRpcService;

	@Autowired
	TransTypeQueryService transTypeQueryService;

	@Autowired
	EnterpriseBankQueryService enterpriseBankQueryService;

	@Autowired
	ExchangeRateTypeQueryService exchangeRateTypeQueryService;

	@Override
	public RuleExecuteResult  execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

		BillDataDto dataDto = (BillDataDto) getParam(paramMap);
		CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
		BizObject bill = getBills(billContext, paramMap).get(0);
		CurrencyExchange currencyExchange = (CurrencyExchange)bill;
		//国机：CZFW-127335 认领单和对账单生单过来的数据不对买入和卖出账户进行默认赋值
		String datasource = bill.getString("datasource");
		if (!Objects.isNull(bill.get("natCurrency"))) {
			CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bill.get("natCurrency").toString());
			bill.set("natCurrency_name", currencyTenantDTO.getName());
		}
		if ("accentity_name".equals(item.get("key"))){
			if(item.get("value")==null) return new RuleExecuteResult();
            Map<String, Object> condition = new HashMap<>();
            //bill.set("currency",1);
			String accentity = currencyExchange.getAccentity();
			if (Objects.isNull(accentity)){
				return new RuleExecuteResult();
			}
			FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
			// 切换交易类型时，flag为空，根据交易类型id赋值flag
			if (currencyExchange.get("flag") == null && currencyExchange.getTradetype() != null) {
				Short flag = getFlagByTradeType(currencyExchange.getTradetype());
				currencyExchange.set("flag",flag);
			}
			if (currencyExchange.get("flag") != null && currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){//卖出外汇
				bill.set("purchaserate",1);
				bill.set("purchaseRateOps", 1);
				condition.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418054F","卖出外汇") /* "卖出外汇" */);
				//交易类型初始化条件
				condition.put("code","SFE");
				if(finOrgDTO!=null){
					String exchangeRateType = currencyExchange.getExchangeRateType();
					//若不存在汇率类型则加载资金组织的汇率类型
					if (StringUtils.isEmpty(exchangeRateType)){
						exchangeRateType = finOrgDTO.getExchangerate();
					}
					String currId = bill.get("regionCurrency");
					//若地区币种不为空 则更新
					if (StringUtils.isEmpty(currId)){
						currId = finOrgDTO.getCurrency();
					}
					CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currId);
					bill.set("purchaseCurrency",currencyTenantDTO.getId());
					bill.set("purchaseCurrency_name",currencyTenantDTO.getName());
					bill.set("purchaseCurrency_priceDigit", currencyTenantDTO.getPricedigit());

					bill.set("purchaseCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
					EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accentity,finOrgDTO.getCurrency(), billContext,"purchasebankaccount");
					//来源认领单和对账单的单据，不对买入账户赋默认值
                    if (defaultBankAccount!=null && (!"16".equals(datasource) && !"80".equals(datasource))) {
                        bill.set("purchasebankaccount", defaultBankAccount.getId());
                        bill.set("purchasebankaccount_name", defaultBankAccount.getName());
                    }
					//当卖出币种=会计本位币时，买入汇率为1
					if (currencyTenantDTO.getId().equals(bill.get("sellCurrency"))){
						bill.set("purchaserate", 1);
						bill.set("purchaseRateOps", 1);
					}else {
						setRate("purchaserate", bill,finOrgDTO.getCurrency(), ((CurrencyExchange) bill).getPurchaseCurrency(), currencyExchange.getVouchdate(), exchangeRateType);
					}
				}
			}else if (currencyExchange.get("flag") != null && currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){//买入外汇
				bill.set("sellrate",1);
				bill.set("sellRateOps", 1);
				condition.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180550","买入外汇") /* "买入外汇" */);
				//交易类型初始化条件
				condition.put("code","BFE");
				if(finOrgDTO!=null){
					String exchangeRateType = currencyExchange.getExchangeRateType();
					//若不存在汇率类型则加载资金组织的汇率类型
					if (StringUtils.isEmpty(exchangeRateType)){
						exchangeRateType = finOrgDTO.getExchangerate();
					}
					String currId = bill.get("regionCurrency");
					//若地区币种不为空 则更新
					if (StringUtils.isEmpty(currId)){
						currId = finOrgDTO.getCurrency();
					}
					CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currId);
					bill.set("sellCurrency",currencyTenantDTO.getId());
					bill.set("sellCurrency_name",currencyTenantDTO.getName());
					bill.set("sellCurrency_priceDigit",currencyTenantDTO.getPricedigit());
					bill.set("sellCurrency_moneyDigit",currencyTenantDTO.getMoneydigit());

					EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accentity, finOrgDTO.getCurrency(), billContext,"sellbankaccount");
					//来源认领单和对账单的单据，不对卖出账户赋默认值
                    if (defaultBankAccount!=null && (!"16".equals(datasource) && !"80".equals(datasource))) {
                        bill.set("sellbankaccount", defaultBankAccount.getId());
                        bill.set("sellbankaccount_name", defaultBankAccount.getName());
                    }
					//当买入币种=会计本位币时，卖出汇率为1
					if (currencyTenantDTO.getId().equals(bill.get("purchaseCurrency"))){
						bill.set("sellrate", 1);
						bill.set("sellRateOps", 1);
					}else {
						setRate("sellrate", bill,finOrgDTO.getCurrency(), ((CurrencyExchange) bill).getSellCurrency(), currencyExchange.getVouchdate(), exchangeRateType);
					}
				}
			}else {
				condition.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418054E","外币兑换") /* "外币兑换" */);
				//交易类型初始化条件
				condition.put("code","BSFE");
            }
			String serviceCode = billContext.getParameter("serviceCode");
			if (serviceCode != null && serviceCode.contains("ficmp0040") && !"ficmp0040".equals(serviceCode)) {
				// 外币兑换单，交易类型发布菜单时，serviceCode前面会拼交易类型id，例：1913762050608726018_ficmp0040
				String tradeType = serviceCode.split("_")[0];
				BdTransType bdTransType = transTypeQueryService.findById(tradeType);
				if (bdTransType != null) {
					bill.set("tradetype",bdTransType.getId());
					bill.set("tradetype_name",bdTransType.getName());
                }
			} else {
				if (bill.get("tradetype") == null || StringUtils.isEmpty(bill.get("tradetype").toString())) {
					// 交易类型为空是直接根据flag赋值系统默认交易类型
					List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
					if (!transTypes.isEmpty()){
						bill.set("tradetype",transTypes.get(0).get("id"));
						bill.set("tradetype_name",transTypes.get(0).get("name"));
					}
				} else {
					// 有值，判断与flag是否匹配，如不匹配仍然取系统预置交易类型
					BdTransType bdTransType = transTypeQueryService.findById(bill.get("tradetype"));
					bdTransType.getExtendAttrsJson();
					boolean tradeTypeCheckFlag = false;
					String flagStr = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson()).getString("transferType_wbdh");
					if (currencyExchange.get("flag")!=null) {
						if ("buyin".equals(flagStr) && !currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
							tradeTypeCheckFlag = true;
						}else if ("sellout".equals(flagStr) && !currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){
							tradeTypeCheckFlag = true;
						}else if ("bs".equals(flagStr) && !currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
							tradeTypeCheckFlag = true;
						}
					}
					if (tradeTypeCheckFlag) {
						List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
						if (!transTypes.isEmpty()){
							bill.set("tradetype",transTypes.get(0).get("id"));
							bill.set("tradetype_name",transTypes.get(0).get("name"));
						}
					}
				}
			}
			if(finOrgDTO!=null){
				CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
				bill.set("natCurrency",currencyTenantDTO.getId());
				bill.set("natCurrency_name", currencyTenantDTO.getName());
				//默认地区币种取会计本位币币种
				if (bill.get("regionCurrency") == null){
					bill.set("regionCurrency",currencyTenantDTO.getId());
					bill.set("regionCurrency_name",currencyTenantDTO.getName());
				}
			}
			bill.set("expectedDeliveryDate", new Date());
            return new RuleExecuteResult();
		}
		//对账单，认领单生单外币兑换时买入卖出汇率
		if("16".equals(datasource) || "80".equals(datasource)){
			String accentity = currencyExchange.getAccentity();
			FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
			String currId = currencyExchange.getNatCurrency();
			String exchangeRateType = currencyExchange.getExchangeRateType();
			if (finOrgDTO !=null){
				if (finOrgDTO.getCurrency()!=null && StringUtils.isEmpty(currId)){
					currId = finOrgDTO.getCurrency();
				}
				//若不存在汇率类型则加载资金组织的汇率类型
				if (finOrgDTO.getExchangerate()!=null && StringUtils.isEmpty(exchangeRateType)){
					exchangeRateType = finOrgDTO.getExchangerate();
				}
			}
			if ("purchaseCurrency_name".equals(item.get("key"))){
				if (currencyExchange.getPurchaseCurrency()!=null){
					if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
						setRate("purchaserate",bill,currId,currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),exchangeRateType);
					}else if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){
						setRate("purchaserate",bill,currId,currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),exchangeRateType);
					}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
						setRate("purchaserate",bill,currId,currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),exchangeRateType);
						if (currencyExchange.getSellCurrency()!=null){
							setRate("exchangerate",bill,currencyExchange.getSellCurrency(),currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
						}
					}
				}
			}else if ("sellCurrency_name".equals(item.get("key"))){
				if (currencyExchange.getSellCurrency()!=null){
					if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){
						setRate("sellrate",bill,currId,currencyExchange.getSellCurrency(),currencyExchange.getVouchdate(),exchangeRateType);
					}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
						setRate("sellrate",bill,currId,currencyExchange.getSellCurrency(),currencyExchange.getVouchdate(),exchangeRateType);
					}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
						setRate("sellrate",bill,currId,currencyExchange.getSellCurrency(),currencyExchange.getVouchdate(),exchangeRateType);
						if (currencyExchange.getPurchaseCurrency()!=null){
							setRate("exchangerate",bill,currencyExchange.getSellCurrency(),currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
						}
					}
				}
			}
		}

		//国机生单外币兑换单，需要校验item的value是否是json格式
		if (StringUtils.isEmpty((String)item.get("value"))||((String) item.get("value")).length()<3 || !((String) item.get("value")).startsWith("{")){
			return new RuleExecuteResult();
		}

		CtmJSONObject obj = CtmJSONObject.parseObject((String)item.get("value"));
		if (obj==null){
			return new RuleExecuteResult();
		}
		Object currency = bill.get("natCurrency");
		if (currency==null){
			String accentity = currencyExchange.getAccentity();
			FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
			CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
			currency = currencyTenantDTO.getId();
			bill.set("natCurrency",currency);
			currencyExchange.setNatCurrency(currencyTenantDTO.getId());
			//默认地区币种取会计本位币币种
			if (bill.get("regionCurrency") == null){
				bill.set("regionCurrency",currencyTenantDTO.getId());
				bill.set("regionCurrency_name",currencyTenantDTO.getName());
			}
		}
		CurrencyTenantDTO natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(currency.toString());
		if(natCurrencyTenantDTO!=null){
			bill.set("natCurrency", natCurrencyTenantDTO.getId());
			bill.set("natCurrency_name", natCurrencyTenantDTO.getName());
			bill.set("natCurrency_priceDigit",natCurrencyTenantDTO.getPricedigit());
			bill.set("natCurrency_moneyDigit",natCurrencyTenantDTO.getMoneydigit());
			//默认地区币种取会计本位币币种
			if (bill.get("regionCurrency") == null){
				bill.set("regionCurrency",natCurrencyTenantDTO.getId());
				bill.set("regionCurrency_name",natCurrencyTenantDTO.getName());
			}
		}
		String settlemodeServiceAttr = currencyExchange.get("settlemode_serviceAttr");
		if ("commissionbankaccount_name".equals(item.get("key"))&&currencyExchange.getCommissionbankaccount()!=null){
			CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(obj.get("currency").toString());;
			//适配多币种，赋值手续费币种
			setRate("commissionrate",bill,currencyExchange.getNatCurrency(),currencyTenantDTO.getId(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
			bill.set("commissionCurrency", currencyTenantDTO.getId());
			bill.set("commissionCurrency_name", currencyTenantDTO.getName());
			bill.set("commissionCurrency_priceDigit",currencyTenantDTO.getPricedigit());
			bill.set("commissionCurrency_moneyDigit",currencyTenantDTO.getMoneydigit());
		}else if ("commissioncashaccount_name".equals(item.get("key"))&&currencyExchange.getCommissioncashaccount()!=null){
			CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(obj.get("currency").toString());
			setRate("commissionrate",bill,currencyExchange.getNatCurrency(),currencyTenantDTO.getId(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
			bill.set("commissionCurrency",currencyTenantDTO.getId());
			bill.set("commissionCurrency_name",currencyTenantDTO.getName());
			bill.set("commissionCurrency_priceDigit",currencyTenantDTO.getPricedigit());
			bill.set("commissionCurrency_moneyDigit",currencyTenantDTO.getMoneydigit());
		}else if ("purchasebankaccount_name".equals(item.get("key"))){
			String accentity = currencyExchange.getAccentity();
			FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
			CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());

			if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){
				//bill.set("purchaserate",1);
				//当卖出币种=会计本位币时，买入汇率为1
				if (currencyTenantDTO.getId().equals(bill.get("sellCurrency"))){
					bill.set("purchaserate", 1);
					bill.set("purchaseRateOps", 1);
				}else {
					setRate("purchaserate", bill,finOrgDTO.getCurrency(), ((CurrencyExchange) bill).getPurchaseCurrency(),currencyExchange.getVouchdate(), currencyExchange.getExchangeRateType());
				}
			}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
				//当买入币种=会计本位币时，卖出汇率为1
				if (currencyTenantDTO.getId().equals(bill.get("purchaseCurrency"))){
					bill.set("sellrate", 1);
					bill.set("sellRateOps", 1);
				}else {
					setRate("sellrate", bill,finOrgDTO.getCurrency(), ((CurrencyExchange) bill).getSellCurrency(), currencyExchange.getVouchdate(), currencyExchange.getExchangeRateType());
				}
                //setRate("purchaserate",bill,currencyTenantDTO.getId(),natCurrencyTenantDTO.getId(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
			}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
				Object purchaseCurrency = currencyExchange.get("purchaseCurrency");
				Object sellCurrency = currencyExchange.get("sellCurrency");
				if (purchaseCurrency!=null&&sellCurrency!=null){
                    setRate("exchangerate",bill,(String)sellCurrency,(String)purchaseCurrency,currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				}
				if (purchaseCurrency==null){
                    setRate("purchaserate",bill,currencyTenantDTO.getId(),natCurrencyTenantDTO.getId(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				}
			}
			bill.set("purchaseCurrency", natCurrencyTenantDTO.getId());
			bill.set("purchaseCurrency_name", natCurrencyTenantDTO.getName());
			bill.set("purchaseCurrency_priceDigit",natCurrencyTenantDTO.getPricedigit());
			bill.set("purchaseCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
		}else if ("sellbankaccount_name".equals(item.get("key"))){
			String accentity = currencyExchange.getAccentity();
			FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
			CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
			if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
				bill.set("sellrate",1);
				bill.set("sellRateOps",1);
				//当买入币种=会计本位币时，卖出汇率为1
				if (currencyTenantDTO.getId().equals(bill.get("purchaseCurrency"))){
					bill.set("sellrate", 1);
					bill.set("sellRateOps",1);
				}else {
					setRate("sellrate", bill,finOrgDTO.getCurrency(), ((CurrencyExchange) bill).getSellCurrency(),currencyTenantDTO.getId(),currencyExchange.getExchangeRateType());
				}
			}else {
                setRate("sellrate",bill,currencyTenantDTO.getId(),natCurrencyTenantDTO.getId(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				//当卖出币种=会计本位币时，买入汇率为1
				if (currencyTenantDTO.getId().equals(bill.get("sellCurrency"))){
					bill.set("purchaserate", 1);
				}else {
					setRate("purchaserate", bill,natCurrencyTenantDTO.getId(), ((CurrencyExchange) bill).getPurchaseCurrency(), currencyExchange.getVouchdate(), currencyExchange.getExchangeRateType());
				}
			}
			if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
				Object purchaseCurrency = currencyExchange.get("purchaseCurrency");
				Object sellCurrency = currencyExchange.get("sellCurrency");
				if (purchaseCurrency!=null&&sellCurrency!=null){
                    setRate("exchangerate",bill,(String)sellCurrency,(String)purchaseCurrency,currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				}
				if (sellCurrency==null){
                    setRate("sellrate",bill,natCurrencyTenantDTO.getId(),currencyTenantDTO.getId(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				}
			}
			bill.set("sellCurrency", natCurrencyTenantDTO.getId());
			bill.set("sellCurrency_name", natCurrencyTenantDTO.getName());
			bill.set("sellCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
			bill.set("sellCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
		}else if ("purchaseCurrency_name".equals(item.get("key"))){
			Object accEntityId = currencyExchange.getAccentity();
			Object currencyId = currencyExchange.getPurchaseCurrency();
			if (accEntityId != null &&  currencyId != null && !StringUtils.isEmpty(settlemodeServiceAttr)  && "0".equals(settlemodeServiceAttr)) {
				EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accEntityId, currencyId, billContext,"purchasebankaccount");
                if (defaultBankAccount!=null) {
                	if (currencyExchange.get("sellbankaccount")!=null){
						EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyExchange.get("sellbankaccount"));
						EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
						if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())){
							bill.set("purchasebankaccount", defaultBankAccount.getId());
							bill.set("purchasebankaccount_name", defaultBankAccount.getName());
						}else {
							bill.set("purchasebankaccount", null);
							bill.set("purchasebankaccount_name", null);
						}
					}else if (currencyExchange.get("commissionbankaccount")!=null){
						EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyExchange.get("commissionbankaccount"));
						EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
						if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())){
							bill.set("purchasebankaccount", defaultBankAccount.getId());
							bill.set("purchasebankaccount_name", defaultBankAccount.getName());
						}else {
							bill.set("purchasebankaccount", null);
							bill.set("purchasebankaccount_name", null);
						}
					}else {
						bill.set("purchasebankaccount", defaultBankAccount.getId());
						bill.set("purchasebankaccount_name", defaultBankAccount.getName());
					}
                }else {
                    bill.set("purchasebankaccount", null);
                    bill.set("purchasebankaccount_name", null);
                }

			}
			if (currencyExchange.getPurchaseCurrency()!=null){
				if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
                    setRate("purchaserate",bill,currencyExchange.getNatCurrency(),currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
                    setRate("purchaserate",bill,currencyExchange.getNatCurrency(),currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
					if (currencyExchange.getSellCurrency()!=null){
                        setRate("exchangerate",bill,currencyExchange.getSellCurrency(),currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
					}
				}
			}
		}else if ("sellCurrency_name".equals(item.get("key"))){
			Object accEntityId = currencyExchange.getAccentity();
			Object currencyId = currencyExchange.getSellCurrency();
			if (accEntityId != null &&  currencyId != null && !StringUtils.isEmpty(settlemodeServiceAttr)  && "0".equals(settlemodeServiceAttr)) {
				EnterpriseBankAcctVO defaultBankAccount = getDefaultBankAccount(accEntityId, currencyId, billContext,"sellbankaccount");
                if (defaultBankAccount!=null) {
					if (currencyExchange.get("purchasebankaccount")!=null){
						EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyExchange.get("purchasebankaccount"));
						EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
						if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())){
							bill.set("sellbankaccount", defaultBankAccount.getId());
							bill.set("sellbankaccount_name", defaultBankAccount.getName());
						}else {
							bill.set("sellbankaccount", null);
							bill.set("sellbankaccount_name", null);
						}
					}else if (currencyExchange.get("commissionbankaccount")!=null){
						EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(currencyExchange.get("commissionbankaccount"));
						EnterpriseBankAcctVO enterpriseBankAcctVO1 = baseRefRpcService.queryEnterpriseBankAccountById(defaultBankAccount.getId());
						if (enterpriseBankAcctVO.getBank().equals(enterpriseBankAcctVO1.getBank())){
							bill.set("sellbankaccount", defaultBankAccount.getId());
							bill.set("sellbankaccount_name", defaultBankAccount.getName());
						}else {
							bill.set("sellbankaccount", null);
							bill.set("sellbankaccount_name", null);
						}
					}else {
						bill.set("sellbankaccount", defaultBankAccount.getId());
						bill.set("sellbankaccount_name", defaultBankAccount.getName());
					}
                }else {
                    bill.set("sellbankaccount", null);
                    bill.set("sellbankaccount_name", null);
                }

			}
			if (currencyExchange.getSellCurrency()!=null){
				if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){
                    setRate("sellrate",bill,currencyExchange.getNatCurrency(),currencyExchange.getSellCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
				}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
                    setRate("sellrate",bill,currencyExchange.getNatCurrency(),currencyExchange.getSellCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
					if (currencyExchange.getPurchaseCurrency()!=null){
                        setRate("exchangerate",bill,currencyExchange.getSellCurrency(),currencyExchange.getPurchaseCurrency(),currencyExchange.getVouchdate(),currencyExchange.getExchangeRateType());
					}
				}
			}
		} else if ("regionCurrency_name".equals(item.get("key"))) {
			if (currencyExchange.getRegionCurrency() != null) {
				String regionCurrId = currencyExchange.getRegionCurrency();
				CurrencyTenantDTO dto = baseRefRpcService.queryCurrencyById(regionCurrId);
				if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())) {
					bill.set("purchaseCurrency", dto.getId());
					bill.set("purchaseCurrency_name", dto.getName());
					bill.set("purchaseCurrency_priceDigit", dto.getPricedigit());
					bill.set("purchaseCurrency_moneyDigit", dto.getMoneydigit());
					bill.set("purchasebankaccount", null);
				} else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())) {
					bill.set("sellCurrency", dto.getId());
					bill.set("sellCurrency_name", dto.getName());
					bill.set("sellCurrency_priceDigit", dto.getPricedigit());
					bill.set("sellCurrency_moneyDigit", dto.getMoneydigit());
					bill.set("sellbankaccount", null);
				}
			}
		}
		this.putParam(paramMap, "return", bill);
		return new RuleExecuteResult();
	}

	private void setRate(String key,BizObject bill,String natCurrencyId,String currencyId,Object vouchDate,String exchangeRateType) throws Exception {
		if (natCurrencyId!=null&&currencyId.equals(natCurrencyId)){
			bill.set(key,1);
			return;
		}
		if (StringUtils.isEmpty(exchangeRateType)||StringUtils.isEmpty(natCurrencyId)){
			return;
		}
		String rateTypeName = "";
		String oriCurrencyName = "";
		if ("sellrate".equals(key)){
			exchangeRateType = bill.getString("sellRateType");
			rateTypeName = bill.getString("sellRateType_name");
			oriCurrencyName = bill.getString("sellCurrency_name");
		}else if ("purchaserate".equals(key)){
			exchangeRateType = bill.getString("purchaseRateType");
			rateTypeName = bill.getString("purchaseRateType_name");
			oriCurrencyName = bill.getString("purchaseCurrency_name");
		}else if ("commissionrate".equals(key)){
			exchangeRateType = bill.getString("commissionRateType");
			rateTypeName = bill.getString("commissionRateType_name");
			CurrencyTenantDTO dto = baseRefRpcService.queryCurrencyById(currencyId);
			oriCurrencyName = dto.getName();
		}
		// 自定义汇率直接返回
		ExchangeRateTypeVO exchangeRateTypeVO = exchangeRateTypeQueryService.queryExcahngeRateTypeById(exchangeRateType);
		//v5用户自定义汇率，不进行数据查询
		if (exchangeRateTypeVO == null || CurrencyRateTypeCode.CustomCode.getValue().equals(exchangeRateTypeVO.getCode())){
			return;
		}
        CtmJSONObject params1 = new CtmJSONObject();
        params1.put("currencyId",currencyId);
        params1.put("natCurrencyId",natCurrencyId);//买入银行账户的币种
        params1.put("vouchDate", vouchDate==null? DateUtils.dateFormat(new Date(),DateUtils.DATE_PATTERN):vouchDate);//单据日期
        params1.put("exchangeRateType", exchangeRateType);//单据日期
//        Map<String, Object> periodByParam = getPeriodByParam(params1);
		//v5新汇率获取方法修改
		CmpExchangeRateVO cmpExchangeRateVO ;
		//成交汇率设置值 -> 买入币种等于源币种，卖出币种等于目的币种，取直接汇率，否则取间接汇率
		if ("exchangerate".equals(key)){
			String purchaseCurrencyId = bill.getString("purchaseCurrency");
			String sellCurrencyId = bill.getString("sellCurrency");
			rateTypeName = bill.getString("exchangeRateType_name");
//			if (!StringUtils.isEmpty(purchaseCurrencyId) && !StringUtils.isEmpty(sellCurrencyId) && currencyId.equals(purchaseCurrencyId) && natCurrencyId.equals(sellCurrencyId)){
//				bill.set(key, periodByParam.get("exchangeRate"));
//			}else {
//				bill.set(key, periodByParam.get("indirectExchangeRate"));
//			}
			//汇率平台-汇率折算方式 1乘；2除;rateconversiontype 0是直接汇率（对应平台乘法）；1是间接汇率（对应平台除法）
			String exchRateOps = "0".equals(bill.getString("rateconversiontype")) ? "1" :  "2";
			cmpExchangeRateVO = CmpExchangeRateUtils.queryExchangeRateWithModeAndIsReverse(sellCurrencyId, purchaseCurrencyId, vouchDate == null ? new Date() : (Date) vouchDate, exchangeRateType, exchRateOps, true);
			if ( cmpExchangeRateVO.getExchangeRate() == null){
				throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211EE3880468000C", "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！") /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,rateTypeName,bill.getString("sellCurrency_name"),bill.getString("purchaseCurrency_name")));
			}
			bill.set("exchangerate", cmpExchangeRateVO.getExchangeRate());
			bill.set("exchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
		}else {
			//bill.set(key, periodByParam.get("exchangeRate")); //V5新汇率废弃
			//V5新汇率，汇率值和对应的折算方式
			try {
				cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currencyId, natCurrencyId, vouchDate == null ? new Date() : (Date) vouchDate, exchangeRateType);
				bill.set(key, cmpExchangeRateVO.getExchangeRate());
				if ("sellrate".equals(key)){
					bill.set("sellRateOps", cmpExchangeRateVO.getExchangeRateOps());
				}else if ("purchaserate".equals(key)){
					bill.set("purchaseRateOps", cmpExchangeRateVO.getExchangeRateOps());
				}else if ("commissionrate".equals(key)){
					bill.set("commissionRateOps", cmpExchangeRateVO.getExchangeRateOps());
				}
			}catch (Exception e){
				throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211EE3880468000C", "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！") /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,rateTypeName,oriCurrencyName,bill.getString("natCurrency_name")));
			}
		}
    }

    private EnterpriseBankAcctVO  getDefaultBankAccount(Object accentity,Object currency,BillContext billContext,String fieldName) throws Exception {
		EnterpriseParams enterpriseParams = new EnterpriseParams();
		enterpriseParams.setOrgid(accentity.toString());
		List<String> currencyList = new ArrayList<>();
		currencyList.add(currency.toString());
		enterpriseParams.setCurrencyIDList(currencyList);
		enterpriseParams.setCurrencyIsDefault(true);
		//0为银行开户
		enterpriseParams.setAcctopentype(0);
		List<EnterpriseBankAcctVO> bankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (bankAccounts.size() > 0) {
            return bankAccounts.get(0);
        }
        return null;
    }

    public Map<String, Object> getPeriodByParam(CtmJSONObject param)
            throws Exception {
        Map resultData = new HashMap<String, Object>();
		//Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, param.getString("exchangeRateType"), param.getString("currencyId"), param.getString("natCurrencyId"), param.getDate("vouchDate"), 6);
		if (StringUtils.isEmpty(param.getString("exchangeRateType")) || param.getDate("vouchDate") == null) {
			return resultData;
		}
		ExchangeRate exchangeRate = baseRefRpcService.queryRateByExchangeType(param.getString("currencyId"), param.getString("natCurrencyId"), param.getDate("vouchDate"), param.getString("exchangeRateType"));
		if (exchangeRate == null) {
			resultData.put("exchangeRate", null);
			resultData.put("indirectExchangeRate", null);
		} else {
			resultData.put("exchangeRate", exchangeRate.getExchangerate());
			resultData.put("indirectExchangeRate", exchangeRate.getIndirectExchangeRate());
		}
        return resultData;
    }

	private Short getFlagByTradeType(String tradeType) throws Exception {
		BdTransType bdTransType = transTypeQueryService.findById(tradeType);
		String flagStr = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson()).getString("transferType_wbdh");
        switch (flagStr) {
            case "buyin":
                return 0;
            case "sellout":
                return 1;
            case "bs":
                return 2;
            default:
                return null;
        }
	}

}
