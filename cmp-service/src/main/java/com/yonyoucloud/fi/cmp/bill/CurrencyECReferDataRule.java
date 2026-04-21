package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.QueryPagerVo;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CurrencyECReferDataRule extends AbstractCommonRule {

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		BillDataDto bill = (BillDataDto) getParam(paramMap);
		FilterVO conditionVO = bill.getCondition();
		if (bill.getPage()!=null&&bill.getPage().getPageSize()==0){
			QueryPagerVo page = bill.getPage();
			page.setPageSize(10);
			bill.setPage(page);
		}
		if (null != bill.getKey() && bill.getKey().equals("purchaseCurrency_name")&&bill.getrefCode().equals("ucfbasedoc.bd_currencytenantref")){
            if (bill.getData()!=null){
                CurrencyExchange currencyExchange = null;
                List<CurrencyExchange> list = (List<CurrencyExchange>)bill.getData();
                if (list!=null) {
                    currencyExchange = list.get(0);
                }
                String accEntityId = currencyExchange.getAccentity();
                if(accEntityId == null){
                    return new RuleExecuteResult();
                }
                List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
				String regionCurrency = currencyExchange.getRegionCurrency();
				if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){
					if (StringUtils.isNotEmpty(regionCurrency)){
						MddBaseUtils.appendCondition(conditionVO, "id", "neq", regionCurrency);
					}
				}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
					MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
				}
				/*if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())||currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())) {//卖出通道
					MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
				}*/
				if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())&&currencyExchange.get("sellCurrency")!=null) {//卖出通道
					MddBaseUtils.appendCondition(conditionVO, "id", "neq", currencyExchange.get("sellCurrency"));
				}
            }
        }else if (null != bill.getKey() && bill.getKey().equals("sellCurrency_name")&&bill.getrefCode().equals("ucfbasedoc.bd_currencytenantref")){
            if (bill.getData()!=null){
                CurrencyExchange currencyExchange = new CurrencyExchange();
                List<CurrencyExchange> list = (List<CurrencyExchange>)bill.getData();
                if (list!=null) {
                    currencyExchange = list.get(0);
                }
                String accEntityId = currencyExchange.getAccentity();
                if(accEntityId == null){
                    return new RuleExecuteResult();
                }
                List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
				String regionCurrency = currencyExchange.getRegionCurrency();
				if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){
					if (StringUtils.isNotEmpty(regionCurrency)){
						MddBaseUtils.appendCondition(conditionVO, "id", "neq", regionCurrency);
					}
				}else if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())){
					MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
				}
                /*if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())||currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())) {//卖出通道
					MddBaseUtils.appendCondition(conditionVO, "id", "neq", accEntity.get(0).get("currency"));
                }*/
				if (currencyExchange.get("flag").equals(Bsflag.Exchange.getValue())&&currencyExchange.get("purchaseCurrency")!=null) {//卖出通道
					MddBaseUtils.appendCondition(conditionVO, "id", "neq", currencyExchange.get("purchaseCurrency"));
				}
            }
        }else if (null != bill.getKey() && (bill.getKey().equals("purchasebankaccount_name")||bill.getKey().equals("sellbankaccount_name"))&&bill.getrefCode().contains("bd_enterprisebankacct")){
			if (bill.getData()!=null){
				CurrencyExchange currencyExchange = null;
				List<CurrencyExchange> list = (List<CurrencyExchange>)bill.getData();
				if (list!=null){
					currencyExchange=list.get(0);
					String accEntityId = currencyExchange.getAccentity();
					if(accEntityId == null){
						return new RuleExecuteResult();
					}
					List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
					String regionCurrency = currencyExchange.getRegionCurrency();
					if (StringUtils.isNotEmpty(regionCurrency)){
						if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){//卖出通道
							if (bill.getKey().equals("purchasebankaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", regionCurrency);
							}else {
								UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getSellCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", regionCurrency);
								}
								if (bill.getKey().equals("sellbankaccount_name")) {
									fullFilterVo(conditionVO, currencyExchange);
								}
							}
						}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){//买入通道
							if (bill.getKey().equals("sellbankaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", regionCurrency);
								fullFilterVo(conditionVO, currencyExchange);
							}else {
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getPurchaseCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", regionCurrency);
								}
							}
						}else{//外币兑换
							MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
							if (bill.getKey().equals("purchasebankaccount_name")){
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getPurchaseCurrency());
								}else if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyExchange.getSellCurrency());
								}
							}else if(bill.getKey().equals("sellbankaccount_name")){
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getSellCurrency());
								}else if (StringUtils.isNotBlank(currencyExchange.getPurchaseCurrency())){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyExchange.getPurchaseCurrency());
								}
								fullFilterVo(conditionVO, currencyExchange);
							}
						}
					}else {
						if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){//卖出通道
							if (bill.getKey().equals("purchasebankaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", accEntity.get(0).get("currency"));
							}else {
								UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getSellCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
								}
								if (bill.getKey().equals("sellbankaccount_name")) {
									fullFilterVo(conditionVO, currencyExchange);
								}
							}
						}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){//买入通道
							if (bill.getKey().equals("sellbankaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", accEntity.get(0).get("currency"));
								fullFilterVo(conditionVO, currencyExchange);
							}else {
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getPurchaseCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));
								}
							}
						}else{//外币兑换
							MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", accEntity.get(0).get("currency"));

							if (bill.getKey().equals("purchasebankaccount_name")){
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getPurchaseCurrency());
								}else if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyExchange.getSellCurrency());
								}
							}else if(bill.getKey().equals("sellbankaccount_name")){
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "eq", currencyExchange.getSellCurrency());
								}else if (StringUtils.isNotBlank(currencyExchange.getPurchaseCurrency())){
									MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_REf, "neq", currencyExchange.getPurchaseCurrency());
								}
								fullFilterVo(conditionVO, currencyExchange);
							}
						}
					}

					// 多币种添加币种启用过滤
					MddBaseUtils.appendCondition(conditionVO, ICmpConstant.CURRENCY_ENABLE_REf, "eq", 1);
					//0为银行开户;2财务公司
					MddBaseUtils.appendCondition(conditionVO, "acctopentype", "in", new String[]{"0", "2", "3"});
				}
			}
		}else if (null != bill.getKey() && (bill.getKey().equals("purchasecashaccount_name")||bill.getKey().equals("sellcashaccount_name"))&&bill.getrefCode().equals("bd_enterprisecashacctref")&&bill.getBillnum().equals("cmp_currencyexchange")){
			if (bill.getData()!=null){
				CurrencyExchange currencyExchange = new CurrencyExchange();
				List<CurrencyExchange> list = (List<CurrencyExchange>)bill.getData();
				if (list!=null){
					currencyExchange=list.get(0);
					String accEntityId = currencyExchange.getAccentity();
					if(accEntityId == null){
						return new RuleExecuteResult();
					}
					List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);/* 暂不修改 已登记*/
					String regionCurrency = currencyExchange.getRegionCurrency();
					if (StringUtils.isNotEmpty(regionCurrency)){
						if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){//卖出通道
							if (bill.getKey().equals("purchasecashaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", regionCurrency);
							}else {
								UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getSellCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", regionCurrency);
								}
								if (bill.getKey().equals("sellbankaccount_name")) {
									fullFilterVo(conditionVO, currencyExchange);
								}
							}
						}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){//买入通道
							if (bill.getKey().equals("sellcashaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", regionCurrency);
							}else {
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getPurchaseCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", regionCurrency);
								}
							}
						}else{//外币兑换
							MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
							if (bill.getKey().equals("purchasecashaccount_name")){
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getPurchaseCurrency());
								}else if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyExchange.getSellCurrency());
								}
							}else if(bill.getKey().equals("sellcashaccount_name")){
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getSellCurrency());
								}else if (StringUtils.isNotBlank(currencyExchange.getPurchaseCurrency())){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyExchange.getPurchaseCurrency());
								}
							}
						}
					}else{
						if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){//卖出通道
							if (bill.getKey().equals("purchasecashaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", accEntity.get(0).get("currency"));
							}else {
								UiMetaDaoHelper.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getSellCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
								}
							}
						}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){//买入通道
							if (bill.getKey().equals("sellcashaccount_name")){
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", accEntity.get(0).get("currency"));
							}else {
								MddBaseUtils.appendCondition(conditionVO, "orgid", "eq", accEntityId);
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getPurchaseCurrency());
								}else {
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
								}
							}
						}else{//外币兑换
							MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", accEntity.get(0).get("currency"));
							if (bill.getKey().equals("purchasecashaccount_name")){
								if (currencyExchange.getPurchaseCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getPurchaseCurrency());
								}else if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyExchange.getSellCurrency());
								}
							}else if(bill.getKey().equals("sellcashaccount_name")){
								if (currencyExchange.getSellCurrency()!=null){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "eq", currencyExchange.getSellCurrency());
								}else if (StringUtils.isNotBlank(currencyExchange.getPurchaseCurrency())){
									MddBaseUtils.appendCondition(conditionVO, IBussinessConstant.CURRENCY, "neq", currencyExchange.getPurchaseCurrency());
								}
							}
						}
					}

					// 多币种添加币种启用过滤
					MddBaseUtils.appendCondition(conditionVO, ICmpConstant.ENABLE, "eq", 1);
				}
			}
		}else if (bill.getBillnum().equals("cmp_currencyexchange")&&bill.getrefCode().equals("transtype.bd_billtyperef")){
			if (bill.getData()!=null){
				CurrencyExchange currencyExchange = null;
				List<CurrencyExchange> list = (List<CurrencyExchange>)bill.getData();
				if (list!=null&&list.size()>0){
					currencyExchange=list.get(0);
					String accEntityId = currencyExchange.getAccentity();
					if(accEntityId == null){
						return new RuleExecuteResult();
					}
					String serviceCode = bill.getParameter("serviceCode");
					if (serviceCode != null && serviceCode.contains("ficmp0040") && !"ficmp0040".equals(serviceCode)) {
						// 外币兑换单，交易类型发布菜单时，serviceCode前面会拼交易类型id，例：1913762050608726018_ficmp0040
						String tradetype = serviceCode.split("_")[0];
						MddBaseUtils.appendCondition(conditionVO, "id", "eq", tradetype);
					} else {
						String transferType;
						if (currencyExchange.get("flag").equals(Bsflag.Sell.getValue())){//卖出通道
							transferType = "sellout";
						}else if (currencyExchange.get("flag").equals(Bsflag.Buy.getValue())){//买入通道
							transferType = "buyin";
						}else {
							transferType = "bs";
						}
						MddBaseUtils.appendCondition(conditionVO, "extend_attrs_json", ICmpConstant.QUERY_LIKE, transferType);
					}
				}
			}
			// 单据类型，只要是外币兑换工作台，就一直要加这个条件的
			MddBaseUtils.appendCondition(conditionVO, "billtype_id", "eq","FICA3");
		}else if (bill.getBillnum().equals("cmp_autoconfig") && bill.getrefCode().equals(IRefCodeConstant.REF_SETTLEMENT)){
			FilterVO conditon = bill.getCondition();
			conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0}));
			bill.setTreeCondition(conditon);
		}else if(bill.getBillnum().equals("cmp_currencyexchange") &&bill.getrefCode().equals(IRefCodeConstant.REF_SETTLEMENT) ){
			FilterVO conditon = bill.getCondition();
			conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0, 1}));
			bill.setTreeCondition(conditon);
		}
		return new RuleExecuteResult();
	}

	private void fullFilterVo(FilterVO conditionVO, CurrencyExchange currencyExchange) throws Exception {
		int deliveryType = currencyExchange.getDeliveryType();
		if(deliveryType == 1){
			List<BankAccountSetting> bankAccountSettingList= queryAutocorrsettingByParam();
			List bankList = bankAccountSettingList.stream().map(BankAccountSetting::getEnterpriseBankAccount).collect(Collectors.toList());
			if(CollectionUtils.isNotEmpty(bankList)) {
				MddBaseUtils.appendCondition(conditionVO, "id", "in", bankList);
			}else{
				MddBaseUtils.appendCondition(conditionVO, "id", "eq", "-1");
			}
		}
	}

	private List<BankAccountSetting> queryAutocorrsettingByParam() throws Exception{
		List<BankAccountSetting> list = new ArrayList<>();
		QuerySchema querySchema = QuerySchema.create().addSelect("id,enterpriseBankAccount");
		QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
		conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
		conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
		querySchema.addCondition(conditionGroup);
		list = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
		return list;
	}

}
