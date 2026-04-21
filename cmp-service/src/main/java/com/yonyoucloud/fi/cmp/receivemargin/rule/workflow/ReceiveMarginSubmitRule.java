package com.yonyoucloud.fi.cmp.receivemargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetReceivemarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.receivemargin.service.ReceiveMarginService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付保证金提交规则
 *  *
 * @author xuxbo
 * @date 2023/8/3 15:15
 */
@Component
public class ReceiveMarginSubmitRule extends AbstractCommonRule {
    @Autowired
    private ReceiveMarginService receiveMarginService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetReceivemarginManagerService cmpBudgetReceivemarginManagerService;

    @Autowired
    @Qualifier("stwbReceiveMarginServiceImpl")
    private StwbBillService stwbBillService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            ReceiveMargin receiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(receiveMargin)){
                return result;
            }

            /*if(!receiveMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(receiveMargin);
                stwbBillService.pushBill(currentBillList, true);// 冗余数据调用资金结算校验
            }*/

            short verifystate = bizObject.get(ICmpConstant.VERIFY_STATE);
            if(verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100757"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B0002F", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }
            if(verifystate == VerifyState.TERMINATED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100757"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B0002F", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }

            if(!receiveMargin.getIsWfControlled()){
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                    cmpBudgetReceivemarginManagerService.executeSubmit(receiveMargin);
                    //刷新pubts
                    ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(receiveMarginNew.getPubts());
                }
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, paramMap);
                result.setCancel(true);
            } else {
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                    cmpBudgetReceivemarginManagerService.executeSubmit(receiveMargin);
                    //刷新pubts
                    ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(receiveMarginNew.getPubts());
                    ReceiveMargin receiveMarginObj = (ReceiveMargin) bizObject;
                    receiveMarginObj.setIsOccupyBudget(receiveMarginNew.getIsOccupyBudget());
                }
            }

        }
        return result;
    }
}
