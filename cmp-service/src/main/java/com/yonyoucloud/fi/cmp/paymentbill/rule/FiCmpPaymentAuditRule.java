package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 付款单远程审核规则
 * @author liuttm
 * @version V1.0
 * @date 2021/4/20 16:00
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpPaymentAuditRule extends FiCmpPaymentBaseRule implements ISagaRule {
    @Autowired
    PaymentService paymentService;
    @Autowired
    CooperationFileService cooperationFileService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103015"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2575C04E00004", "在财务新架构环境下，不允许审批付款单。") /* "在财务新架构环境下，不允许审批付款单。" */);
        }
        log.error("executing  FiCmpPaymentAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        List<PayBill> payBillList = getPaymentFromRequest(billContext,paramMap);
        // 应收单据在现金不存在，直接结束流程。
        if(payBillList.size() == 0){
            return new RuleExecuteResult();
        }
        Long payBillId = payBillList.get(0).getId();
        YmsLock ymsLock = null;
        try{
            if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(payBillId.toString()))==null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102149"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D9","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            audit(payBillList.get(0));
            YtsContext.setYtsContext("payBillId",payBillId);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            try {
                // 附件拷贝：应收应付->收付款工作台
                cooperationFileService.copyFiles("yonbip-fi-arap",payBillList.get(0).get("id").toString(),
                        "yonbip-fi-ctmcmp", payBillList.get(0).get("id").toString(), null, null);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102150"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B3", "附件同步异常：") /* "附件同步异常：" */+e.getMessage());
            }
        }catch (Exception e){
            log.error(e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102151"),e.getMessage());
        }
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("FiCmpPaymentAuditRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        // 审核失败，调用取消审核流程恢复单据状态
        if(YtsContext.getYtsContext("payBillId") == null){
            return new RuleExecuteResult();
        }
        Long payBillId =  (Long) YtsContext.getYtsContext("payBillId");
        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, payBillId);
        if(payBill != null){
            unaudit(payBill);
        }
        return new RuleExecuteResult();
    }
}
