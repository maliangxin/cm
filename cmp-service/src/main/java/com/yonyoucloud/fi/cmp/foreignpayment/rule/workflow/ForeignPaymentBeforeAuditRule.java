package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.dataSignature.DataSignatureUtil;
import com.yonyoucloud.fi.cmp.util.dataSignature.entity.DataSignatureEntity;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 外汇付款 审核前规则
 *
 * @author xuxbo
 * @date 2023/8/3 14:31
 */
@Slf4j
@Component
public class ForeignPaymentBeforeAuditRule extends AbstractCommonRule {

    @Autowired
    private CtmSignatureService signatureService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            YmsLock ymsLock = null;
            try {
                ymsLock = JedisLockUtils.lockRuleWithOutTrace(id,map);
                if (null == ymsLock) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100065"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051B", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                CtmJSONObject data = new CtmJSONObject();
                log.info("ForeignPaymentBeforeAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizobject.getId());
                log.info("ForeignPaymentBeforeAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100066"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180517", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                Date currentPubts = bizobject.getPubts();
                if (currentPubts != null) {
                    if (!currentPubts.equals(currentBill.getPubts())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100067"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051A", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }
                Date date = currentBill.getVouchdate();
                if (null != billContext.getDeleteReason()) {
                    if (ICmpConstant.DELETEALL.equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                        return new RuleExecuteResult();
                    }
                }
                Date currentDate = BillInfoUtils.getBusinessDate();
                if (currentDate.compareTo(date) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100068"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180518", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
                }

                if (currentBill.getSettlestatus() != null && currentBill.getSettlestatus() == 2) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100069"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180519", "当前单据【%s】已结算，不能进行审批！") /* "当前单据【%s】已结算，不能进行审批！" */, currentBill.getCode()));
                }
                //待结算的资金付款单才进行签名
                if (currentBill.getSettlestatus() == FundSettleStatus.WaitSettle.getValue()) {
                    String oppositeObjectName = currentBill.getReceivename();
                    String oppositeAccountName = currentBill.getReceivebankaccountname();
                    DecimalFormat decimalFormat = new DecimalFormat("0.00#");
                    String oriSum = decimalFormat.format(currentBill.getAmount());
                    CtmJSONObject oriJson = new CtmJSONObject();
                    oriJson.put("oppositeObjectName", oppositeObjectName);
                    oriJson.put("oppositeAccountName", oppositeAccountName);
                    oriJson.put(IBussinessConstant.ORI_SUM, oriSum);
                    //验签
                    DataSignatureEntity dataSignatureEntity = DataSignatureEntity.builder().opoppositeObjectName(currentBill.getReceivename()).
                            oppositeAccountName(currentBill.getReceivebankaccountname()).tradeAmount(currentBill.getAmount()).oriSign(currentBill.getSignature()).build();
                    //验签
                    DataSignatureUtil.unSignMsg(dataSignatureEntity);
                }


                currentBill.setRiskPayType(bizobject.get("riskPayType"));
                currentBill.setRiskPayFlag(bizobject.get("riskPayFlag"));

                // 修改结算状态为结算中
                if (currentBill.getSettlestatus().equals(FundSettleStatus.WaitSettle.getValue())){
                    currentBill.setSettlestatus(FundSettleStatus.SettleProssing.getValue());
                }

                currentBill.setEntityStatus(EntityStatus.Update);
                bizobject.putAll(currentBill);
                bizobject.set(ICmpConstant.AUDITORID, AppContext.getCurrentUser().getId());
                bizobject.set(ICmpConstant.AUDITOR, AppContext.getCurrentUser().getName());
                bizobject.set(ICmpConstant.AUDIT_DATE, new Date());
                bizobject.set(ICmpConstant.AUDIT_TIME, new Date());
                log.error("paymargin bill audit, data={}, code={}", CtmJSONObject.toJSONString(data), bizobject.get(ICmpConstant.CODE));

            } catch (Exception e) {
                JedisLockUtils.unlockRuleWithOutTrace(map);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102364"),e.getMessage());
            }
        }

        return new RuleExecuteResult();
    }
}
