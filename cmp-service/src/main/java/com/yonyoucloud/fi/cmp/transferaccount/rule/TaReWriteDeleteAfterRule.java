package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 银行对账单生成同名账户划转银行转账,银行对账单生成同名账户划转缴存现金,银行对账单生成同名账户划转提取现金,银行对账单生成同名账户划转第三方转账
 * 转账单删除时，调用取消关联接口释放银行对账单\认领单
 */
@Component
@Slf4j
public class TaReWriteDeleteAfterRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //获取单据信息
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills == null || bills.size() == 0) {
            return new RuleExecuteResult();
        }
        List<CtmJSONObject> listreq = new ArrayList<>();
        for (BizObject bill : bills) {
            TransferAccount transferAccount = Objectlizer.convert(JSONBuilderUtils.beanToMap(bill), TransferAccount.ENTITY_NAME);
            //关联【付款】对账单，封装关联删除数据
            if (transferAccount.getPaybankbill() != null) {
                CtmJSONObject jsonReq = new CtmJSONObject();
                jsonReq.put("busid", transferAccount.getPaybankbill());
                jsonReq.put("stwbbusid", transferAccount.getId());
                listreq.add(jsonReq);
            }
            //关联【收款】对账单，封装关联删除数据
            if (transferAccount.getCollectbankbill() != null) {
                CtmJSONObject jsonReq = new CtmJSONObject();
                jsonReq.put("busid", transferAccount.getCollectbankbill());
                jsonReq.put("stwbbusid", transferAccount.getId());
                listreq.add(jsonReq);
            }
            //关联【付款】认领，封装关联删除数据
            if (transferAccount.getPaybillclaim() != null) {
                CtmJSONObject jsonReq = new CtmJSONObject();
                jsonReq.put("claimid", transferAccount.getPaybillclaim());
                jsonReq.put("stwbbusid", transferAccount.getId());
                listreq.add(jsonReq);
            }
            //关联【收款】认领单，封装关联删除数据
            if (transferAccount.getCollectbillclaim() != null) {
                CtmJSONObject jsonReq = new CtmJSONObject();
                jsonReq.put("claimid", transferAccount.getCollectbillclaim());
                jsonReq.put("stwbbusid", transferAccount.getId());
                listreq.add(jsonReq);
            }
        }
        return new RuleExecuteResult();
    }
}
