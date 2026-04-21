package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.stwb.StwbBillCheckService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  外汇付款提交规则
 *  *
 * @author xuxbo
 * @date 2023/8/3 15:15
 */
@Component
public class ForeignPaymentSubmitRule extends AbstractCommonRule {

    @Resource
    private StwbBillCheckService stwbBillCheckService;
    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;

    @Autowired
    @Qualifier("stwbForeignPaymentBillServiceImpl")
    private StwbBillService stwbBillService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            ForeignPayment foreignPayment = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(foreignPayment)){
                return result;
            }
           /* if(!foreignPayment.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(foreignPayment);
                stwbBillService.pushBill(currentBillList, true);// 冗余数据调用资金结算校验
            }*/
            short verifystate = bizObject.get(ICmpConstant.VERIFY_STATE);
            if(verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100757"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B0002F", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }
            if(verifystate == VerifyState.TERMINATED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100758"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00140", "单据已终止流程，不能进行提交！") /* "单据已终止流程，不能进行提交！" */);
            }

            if(!foreignPayment.getIsWfControlled()){//先预占
                boolean budget = cmpBudgetForeignpaymentManagerService.budget(foreignPayment);
                if (budget) {
                    ForeignPayment foreignObj = (ForeignPayment) bizObject;
                    cmpBudgetForeignpaymentManagerService.updateOccupyBudget(foreignObj, OccupyBudget.PreSuccess.getValue());
                }
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, paramMap);
                result.setCancel(true);
            }

            //国际相关，新增结算检查,列表提交时
            if (IBillNumConstant.CMP_FOREIGNPAYMENTLIST.equals(billContext.getBillnum())){
//                CtmJSONObject billcheckJson = null;
//                if(null != paramMap.get("requestData")){
//                    List<Object> requestData = Arrays.asList(paramMap.get("requestData"));
//                    String jsonStr = CtmJSONObject.toJSONString(requestData.get(0));
//                    String str = jsonStr.substring(1,jsonStr.length()-1);
//                    billcheckJson = CtmJSONObject.parseObject(str);
//                }
                //CtmJSONObject billcheckJson = CtmJSONObject.parseObject( map.get("requestData") ==null? null :map.get("requestData").toString());
                //需要进行结算检查
//                if (billcheckJson!=null && billcheckJson.get("billCheckFlag") !=null && billcheckJson.getBoolean("billCheckFlag")){
                if (bizObject != null && bizObject.get("billCheckFlag") != null && bizObject.getBoolean("billCheckFlag")) {
                    ForeignPayment foreignPayment1 = new ForeignPayment();
                    foreignPayment1.init(foreignPayment);
                    CtmJSONObject billCheckResult = stwbBillCheckService.foreignpaymentSubmitBillCheck(foreignPayment1);
                    if ("1".equals(billCheckResult.getString("checkFlag"))){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101676"),billCheckResult.getString("checkMsg"));
                    }
                }
            }
        }
        return result;
    }
}
