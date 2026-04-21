package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:认领单对账单生成外币兑换单，删除前规则，用来处理生单关联关系删除
 * @author: wanxbo@yonyou.com
 * @date: 2023/7/18 14:19
 */

@Slf4j
@Component("bankToCurrencyExchangeDeleteRule")
public class BankToCurrencyExchangeDeleteRule extends AbstractCommonRule {

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //只处理外币兑换单
        String billNum = billContext.getBillnum();
        if (!("cmp_currencyexchange".equals(billNum) || "cmp_currencyexchangelist".equals(billNum))){
            return new RuleExecuteResult();
        }
        //获取单据信息
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills == null || bills.size() == 0){
            return new RuleExecuteResult();
        }
        BizObject bill = bills.get(0);
        CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME,bill.getId());
        String billType = currencyExchange.getString("billtype");
        if (StringUtils.isEmpty(billType) || EventType.CurrencyExchangeApply.getValue() == Short.parseShort(billType)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101483"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_191359CA04A00008","来源于外币兑换申请的单据，不允许手工进行删除！") /* "来源于外币兑换申请的单据，不允许手工进行删除！" */);
        }
        //只过滤来源单据为对账单和认领单的数据
        String datasource = currencyExchange.getString("datasource");
        //16对账单，80认领单
        if (StringUtils.isEmpty(datasource) || (!"16".equals(datasource) && !"80".equals(datasource))){
            return new RuleExecuteResult();
        }

        List<CtmJSONObject> listreq = new ArrayList<>();

        //买入关联银行对账单，封装关联删除数据
        if (currencyExchange.getPaybankbill() != null){
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("busid", currencyExchange.getPaybankbill());
            if (currencyExchange.getCollectbankbill() == null) {
                //俩都是流水的时候传一个就行了
                jsonReq.put("stwbbusid", currencyExchange.getId());
            }
            listreq.add(jsonReq);
        }

        //卖出关联银行对账单，封装关联删除数据
        if (currencyExchange.getCollectbankbill() != null){
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("busid", currencyExchange.getCollectbankbill());
            jsonReq.put("stwbbusid", currencyExchange.getId());
            listreq.add(jsonReq);
        }

        //买入关联认领单，封装关联删除数据
        if (currencyExchange.getPaybillclaim() != null){
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("claimid", currencyExchange.getPaybillclaim());
            if (currencyExchange.getCollectbillclaim() != null){
                //俩都是认领的时候传一个就行了
                jsonReq.put("stwbbusid", currencyExchange.getId());
            }
            listreq.add(jsonReq);
        }

        //卖出关联认领单，封装关联删除数据
        if (currencyExchange.getCollectbillclaim() != null){
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("claimid", currencyExchange.getCollectbillclaim());
            jsonReq.put("stwbbusid", currencyExchange.getId());
            listreq.add(jsonReq);
        }

        if (listreq.size() > 0){
            for (CtmJSONObject jsonReq : listreq) {
                reWriteBusCorrDataService.resDelData(jsonReq);
            }
        }

        return new RuleExecuteResult();
    }
}
