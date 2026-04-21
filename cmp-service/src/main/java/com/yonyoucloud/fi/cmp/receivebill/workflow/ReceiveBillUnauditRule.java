package com.yonyoucloud.fi.cmp.receivebill.workflow;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.rule.FiCmpReceiveBillBaseRule;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *   收款单弃审规则
 * @author maliang
 * @version V1.0
 * date 2021/7/12 15:19
 * Copyright yonyou
 */
@Slf4j
@Component
public class ReceiveBillUnauditRule extends FiCmpReceiveBillBaseRule {

    @Autowired
    private JournalService journalService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103030"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C261B404E00007", "在财务新架构环境下，不允许弃审收款单。") /* "在财务新架构环境下，不允许弃审收款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("##   #####   executing  ReceiveBillUnauditRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        List<ReceiveBill> receiveBillList =  this.getBills(billContext, paramMap);
        for(ReceiveBill receiveBill : receiveBillList){
            if (receiveBillList == null || receiveBillList.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101419"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418035A","请选择单据！") /* "请选择单据！" */);
            }
            Date currentPubts = receiveBill.getPubts();
            ReceiveBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), receiveBill.getId());
            //如果输入无效id 返回未查询到收款单
            if (null == currentBill) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101420"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180354","未查询到收款单") /* "未查询到收款单" */);
            }
            //判断 OpenAPI 过来的单据 如果是审批流控制 则不允许审批
            if(ObjectUtils.isNotEmpty(currentBill.getIsWfControlled())){
                String str = paramMap.get("requestData").toString();
                if (str.contains("_fromApi") && currentBill.getIsWfControlled().equals(true)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101421"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180357","当前单据已启用审批流，弃审失败！") /* "当前单据已启用审批流，弃审失败！" */);
                }
            }
            if (currentPubts != null) {
                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101422"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180359","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            if (currentBill.getSrctypeflag() != null && currentBill.getSrctypeflag().equals("auto")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101423"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180352","】是自动生成的单据，不能进行取消审批！") /* "】是自动生成的单据，不能进行取消审批！" */);
            }
            if (currentBill.getSettlestatus() != null && currentBill.getSettlestatus().getValue() == 2) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101424"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180355","】已结算，不能进行取消审批！") /* "】已结算，不能进行取消审批！" */);
            }
            if (currentBill.getAuditstatus() != null && currentBill.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101425"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180356","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
            }
            if (currentBill.getSrcitem() != null && currentBill.getSrcitem().getValue() != EventSource.Cmpchase.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101426"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180358","】不是现金自制单据，不能进行取消审批！") /* "】不是现金自制单据，不能进行取消审批！" */);
            }
            YmsLock ymsLock = null;
            try{
                //和线下支付并发问题，添加pk锁，FIBillController中会解锁
                ymsLock= JedisLockUtils.lockRuleWithOutTrace(currentBill.getId().toString(),paramMap);
                if (null == ymsLock) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101427"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180351","单据【") /* "单据【" */ + currentBill.getCode() +
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180353","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
                }
                receiveBill.setAuditstatus(AuditStatus.Incomplete);
                receiveBill.setEntityStatus(EntityStatus.Update);
                receiveBill.setAuditorId(null);
                receiveBill.setAuditor(null);
                receiveBill.setAuditDate(null);
                receiveBill.setAuditTime(null);
                currentBill.setAuditorId(null);
                currentBill.setAuditstatus(AuditStatus.Incomplete);
                currentBill.setAuditor(null);
                currentBill.setAuditDate(null);
                currentBill.setAuditTime(null);
                journalService.updateJournal(currentBill);
            }catch (Exception e){
                JedisLockUtils.unlockRuleWithOutTrace(paramMap);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101428"),e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }

}
