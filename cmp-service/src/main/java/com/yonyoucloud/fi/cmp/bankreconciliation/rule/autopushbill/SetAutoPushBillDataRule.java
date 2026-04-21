package com.yonyoucloud.fi.cmp.bankreconciliation.rule.autopushbill;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: 自动生单确认页面查询后置规则
 * @author: wanxbo@yonyou.com
 * @date: 2022/8/1 12:07
 */
@Component("setAutoPushBillDataRule")
public class SetAutoPushBillDataRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        Long ytenantid = AppContext.getTenantId();
        Long userid = AppContext.getUserId();
        if (map.get("return") != null && map.get("return") instanceof Pager && ((Pager) (map.get("return"))).getRecordList().size() > 0) {
            /**
             * 取出对账单数据格式
             */
            Pager pager = (Pager) (map.get("return"));
            List<Map<String, Object>> bankreconciliations = pager.getRecordList();//查回的，未插入关联表数据的对账单列表。
            List<Map<String, Object>> newBankreconciliations = new ArrayList<Map<String,Object>>();//插入关联表数据后的对账单列表
            for (Map<String, Object> b : bankreconciliations){
                List<Map<String, Object>> bs = (List<Map<String, Object>>) b.get("BankReconciliationbusrelation_b");
                if (bs !=null){
                    if (bs.size() == 1){
                        b.put("autocreatebillcode",bs.get(0).get("billcode"));
                        b.put("ischoosebill",true);
                    }else {
                        b.put("autocreatebillcode",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804FB","去确认") /* "去确认" */);
                        b.put("ischoosebill",false);
                    }
                    newBankreconciliations.add(b);
                }
            }
            //将插入关联表数据的新对账单列表放入RecordList
            pager.setRecordList(newBankreconciliations);
            map.put("return", pager);
        }
        return new RuleExecuteResult();
    }
}
