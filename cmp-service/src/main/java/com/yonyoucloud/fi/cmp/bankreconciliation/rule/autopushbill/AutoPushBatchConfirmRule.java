package com.yonyoucloud.fi.cmp.bankreconciliation.rule.autopushbill;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoPushBillService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @description: 自动生单确认页面，异步批量确认接口
 * @author: wanxbo@yonyou.com
 * @date: 2022/8/2 14:15
 */
@Component
public class AutoPushBatchConfirmRule extends AbstractCommonRule {

    @Resource
    private BankAutoPushBillService bankAutoPushBillService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BankReconciliation> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            Long id = bills.get(0).getId();
            if (!ValueUtils.isNotEmptyObj(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100989"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D3","操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
            }
            //start wangdengk 20230719 解决JSON转化死循环导致内存溢出问题
            BankReconciliation cloneBill = new BankReconciliation();
            cloneBill.init(bills.get(0));
            for(BankReconciliationbusrelation_b bankrecb : cloneBill.BankReconciliationbusrelation_b()){ // 删除子表的_parent属性
                bankrecb.remove("_parent");
            }
            JsonNode params = JSONBuilderUtils.beanToJson(cloneBill);
            //end wangdengk 20230719 解决JSON转化死循环导致内存溢出问题
            try {
                bankAutoPushBillService.confirmBill(params);
            }catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100990"),String.format("%s:%s",bills.get(0).getString("bank_seq_no"),e.getMessage()));
            }
        }
        return new RuleExecuteResult();
    }
}
