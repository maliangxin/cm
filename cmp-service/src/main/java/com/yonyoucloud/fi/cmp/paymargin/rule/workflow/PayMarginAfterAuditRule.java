package com.yonyoucloud.fi.cmp.paymargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SettleFlagEnum;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付保证金审核后规则
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 14:31
 */

@Slf4j
@Component
public class PayMarginAfterAuditRule extends AbstractCommonRule {
    @Autowired
    private PayMarginService payMarginService;
    @Autowired
    @Qualifier("stwbPayMarginServiceImpl")
    private StwbBillService stwbBillService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;
    @Autowired
    MarginCommonService marginCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            String id = bizObject.getId().toString();
            try {
                log.info("PayMarginAfterAuditRule bizObject, id = {}, pubTs = {}", bizObject.getId(), bizObject.getPubts());
                PayMargin currentBill = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId());
                log.info("PayMarginAfterAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                //如果结算标识是需要结算：推送结算
                if (currentBill.getSettleflag() == 1) {
                    // 设置本方传结算信息
                    if (currentBill.getOurassociationstatus() != null && currentBill.getOurassociationstatus() == 1) {
                        // 本方关联状态为已关联时，设置本方传结算字段
                        currentBill.set("ourcheckno", currentBill.getOurcheckno());         // 本方财资统一对账码
                        currentBill.set("ourbankbillid", currentBill.getOurbankbillid());   // 本方流水ID
                        currentBill.set("ourbillclaimid", currentBill.getOurbillclaimid()); // 本方认领单ID
                    }

                    // 设置对方传结算信息
                    if (currentBill.getOppassociationstatus() != null && currentBill.getOppassociationstatus() == 1) {
                        // 对方关联状态为已关联时，设置对方传结算字段
                        currentBill.set("oppcheckno", currentBill.getOppcheckno());         // 对方财资统一对账码
                        currentBill.set("oppbankbillid", currentBill.getOppbankbillid());   // 对方流水ID
                        currentBill.set("oppbillclaimid", currentBill.getOppbillclaimid()); // 对方认领单ID
                    }
                    //推送资金结算
                    List<BizObject> currentBillList = new ArrayList<>();
                    currentBillList.add(currentBill);
                    stwbBillService.pushBill(currentBillList, false);// 推送资金结算
                    currentBill.set(ICmpConstant.AUDIT_STATUS, AuditStatus.Complete.getValue());
                }

            } catch (Exception e) {
                log.error("catch Exception", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101566"),e.getMessage());
            } finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }
            PayMargin currentBill = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId());
            //当是否结算为否时，结算状态如果选择待结算，审批通过后自动将结算状态由待结算变为结算完成，结算成功日期取审批通过日期，取消审批时候清空结算成功日期与审批通过日期，结算状态变回待结算
            currentBill.put("_entityName","cmp.paymargin.PayMargin");
            payMarginService.changeSettleFlagAfterAudit(currentBill);
            //结算状态为已结算补单/空，是否占预算为预占成功时，删除预占，进行实占；
            if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                payMarginExecuteAudit(currentBill);
            }
            // 判断是否有转换保证金 如果有 需要调用转换保证金保存接口
            if (currentBill.getConversionmarginflag() == 1) {
                PayMargin conversionPayMargin;
                try {
                    conversionPayMargin = marginCommonService.generateConversionPayMargin(currentBill);
                } catch (Exception e) {
                    log.error("catch Conversion Pay Margin Exception", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100070"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC642205080003", "调用转换保证金接口错误") /* "调用转换保证金接口错误" */ +"=>"+ e.getMessage());
                }
                // 生成转换保证金后，需要保存一下原保证金的转换保证金id以及编码
                PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId());
                payMarginNew.setConversionmarginid(conversionPayMargin.getId().toString());
                payMarginNew.setConversionmargincode(conversionPayMargin.getCode());
                payMarginNew.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, payMarginNew);
            }
            //刷新pubts
            PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
            bizObject.setPubts(payMarginNew.getPubts());
        }
        return new RuleExecuteResult();
    }

    /**
     * 实占预算
     *
     * @param payMargin
     * @throws Exception
     */
    private void payMarginExecuteAudit(PayMargin payMargin) throws Exception {
        PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId(), 2);
        Short settleflag = payMarginNew.getSettleflag();
        PayMargin update = new PayMargin();
        //结算状态为已结算补单/空时
        if (settleflag == SettleFlagEnum.NO.getValue() && (payMarginNew.getSettlestatus() == null || payMarginNew.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue())) {
            //实占预算
            //占用成功后更新是否占预算为实占成功
            if (cmpBudgetPaymarginManagerService.budgetSuccess(payMarginNew, true,true)) {
                update.setId(payMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
            }
        }
    }


}
