package com.yonyoucloud.fi.cmp.bill;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.yonbip.ctm.util.CtmObjectUtils;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.CheckItem;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.arap.ConutMoneyInfo;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import org.imeta.biz.base.BizContext;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
@Component
public class AutoCountRuleToCmp extends AbstractCommonRule {

	@Autowired
	BaseRefRpcService baseRefRpcService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
		if (null == childrenField) {
			return new RuleExecuteResult();
		}else{
			BillDataDto item = (BillDataDto)this.getParam(paramMap);
			CheckItem checkItem = (CheckItem)(new Gson()).fromJson(item.getItem(), CheckItem.class);
			if (null != checkItem && checkItem.getKey() != null) {
				//获取前端传输内容
				List<BizObject> bills = BillInfoUtils.decodeBills(billContext, item.getData());
				if (null != bills && bills.size() != 0) {
					BizObject bill = (BizObject)bills.get(0);
					CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bill.get("currency"));
					if(currencyDTO == null){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101053"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E6", "请先选择币种") /* "请先选择币种" */);
					}
					//获取汇率
					BigDecimal exchRate = bill.get("exchRate");
					//获取表体
					List<BizObject> lines = (List)bill.get(childrenField);
					if (null == lines) {
						return new RuleExecuteResult();
					}else{
						//查看当前行数
						int location = checkItem.getLocation();
						BizObject line = null;
						BizObject one;
						if (location > -1) {
							line = (BizObject)lines.get(location);
							ConutMoneyInfo inf = new ConutMoneyInfo();
							CtmObjectUtils.objValueCopy(line.getClass(), line, inf);

							if(null == inf.getOriSum()){
								return new RuleExecuteResult();
							}
							//获取本币
							BigDecimal oriSum = inf.getOriSum().setScale(currencyDTO.getMoneydigit());
							if(oriSum != null && exchRate !=null){
								inf.setNatSum(oriSum.multiply(exchRate));
							}
							CtmObjectUtils.objValueCopy(inf.getClass(), inf, line);
						}else{ //汇率变表体行
							for (BizObject bizObject : lines) {
								ConutMoneyInfo inf = new ConutMoneyInfo();
								CtmObjectUtils.objValueCopy(bizObject.getClass(), bizObject, inf);
								if(null == inf.getOriSum()){
									return new RuleExecuteResult();
								}
								//获取本币  对金额精度进行处理，直接进行截取，不可进行四舍五入
								BigDecimal oriSum = inf.getOriSum().setScale(currencyDTO.getMoneydigit(), RoundingMode.DOWN);
								if(oriSum != null && exchRate !=null){
									inf.setNatSum(oriSum.multiply(exchRate));
								}
								CtmObjectUtils.objValueCopy(inf.getClass(), inf, bizObject);
							}
						}
						this.putParam(paramMap, "bills", bills);
						JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
						one = null;

						String json;
                        LinkedTreeMap res;
						if (location > -1) {
							String subFullname = MetaDaoHelper.getSubFullname(billContext.getFullname());
							json = formatter.toJson(line, subFullname, 32).toString();
                            res = (LinkedTreeMap)GsonHelper.FromJSon(json, Map.class);


						} else {
							json = formatter.toJson(bill, billContext.getFullname(), 32).toString();
							res = (LinkedTreeMap)GsonHelper.FromJSon(json, Map.class);
//                            String name = (String)MetaDaoHelper.getSubFullname(billContext.getFullname());
//                            String[] fullname = name.split("\\.");
//                            ArrayList<LinkedTreeMap> childs = (ArrayList) res.get(fullname[2]);
//                            for (LinkedTreeMap child : childs) {
//                                child.put("_status","Update");
//                                if(child.get("id") == null){
//                                    child.put("_status","Insert");
//                                }
//                            }
						}

						this.putParam(paramMap, "return", res);
						return new RuleExecuteResult();
					}
				}
			}
		}
		return new RuleExecuteResult();
	}

}
