package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ONE;

/**
 * @description: 外币兑换申请单 新增单据规则 用来处理默认结算方式，交割类型等数据
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/18 16:22
 */

@Slf4j
@Component("currencyApplyAddBillRule")
public class CurrencyApplyAddBillRule extends AbstractCommonRule {

    private static final String SYSTEMCODE = "system_0001";
    private static final String SERVICEATTR = "serviceAttr";

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    private CmCommonService cmCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);

            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100361"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DC","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            //单组织逻辑
            if (FIDubboUtils.isSingleOrg()) {
                BizObject singleOrg = FIDubboUtils.getSingleOrg();
                if (singleOrg != null) {
                    bizobject.set(IBussinessConstant.ACCENTITY, singleOrg.get("id"));
                    bizobject.set("accentity_name", singleOrg.get("name"));
                }
            }
            //设置期初标志
            bizobject.set(IBillConst.INITFLAG, false);
            //汇率
            bizobject.set(IBillConst.EXCHRATE, 1);
            if ("cmp_currencyapply".equals(billnum)) {
                //结算方式
                SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                settleMethodQueryParam.setCode(SYSTEMCODE);
                settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
                settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                if (!CollectionUtils.isEmpty(dataList)) {
                    SettleMethodModel settlementWay = dataList.get(0);
                    bizobject.set("settlemode",settlementWay.getId());
                    String locale = InvocationInfoProxy.getLocale();
                    switch (locale) {
                        case "zh_CN":
                            bizobject.set("settlemode_name", settlementWay.getName());
                            break;
                        case "en_US":
                            bizobject.set("settlemode_name", settlementWay.getName2());
                            break;
                        case "zh_TW":
                            bizobject.set("settlemode_name", settlementWay.getName3());
                            break;
                        default:
                            bizobject.set("settlemode_name", settlementWay.getName());
                    }
                }

                Map<String, Object> condition = new HashMap<>();
                //交易类型标识 billContext.getParameter("cmpTradeTypeFlag")；buy买入外汇；sell卖出外汇；exchange外币兑换
                if ("buy".equals(billContext.getParameter("cmpTradeTypeFlag"))){
                    condition.put("code","APPLY_BFE");
                }else if ("sell".equals(billContext.getParameter("cmpTradeTypeFlag"))){
                    condition.put("code","APPLY_SFE");
                }else if ("exchange".equals(billContext.getParameter("cmpTradeTypeFlag"))){
                    condition.put("code","APPLY_BSFE");
                }
                if (billContext.getParameter("cmpTradeTypeFlag") != null){
                    //交易类型赋值
                    List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
                    if (!transTypes.isEmpty()) {
                        bizobject.set("tradetype", transTypes.get(0).get("id"));
                        bizobject.set("tradetype_name", transTypes.get(0).get("name"));
                    }
                }
            }

            JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
            String json = formatter.toJson(bizobject, billContext.getFullname(), true).toString();
            //putParam(paramMap, json);
            return new RuleExecuteResult(json);
        }

        return new RuleExecuteResult();
    }
}
