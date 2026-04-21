package com.yonyoucloud.fi.cmp.receivebill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.rule.FiCmpReceiveBillBaseRule;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *   收款单审批规则
 * @author maliang
 * @version V1.0
 * @date 2021/7/12 15:19
 * @Copyright yonyou
 */
@Slf4j
@Component
public class ReceiveBillAuditRule extends FiCmpReceiveBillBaseRule {

    @Autowired
    private JournalService journalService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103027"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2613604E00004", "在财务新架构环境下，不允许审批收款单。") /* "在财务新架构环境下，不允许审批收款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("##   #####   executing  ReceiveBillAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }

        List<ReceiveBill> receiveBillList =  this.getBills(billContext, paramMap);

        if (receiveBillList == null || receiveBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100578"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A6","请选择单据！") /* "请选择单据！" */);
        }

        for (ReceiveBill receiveBill : receiveBillList){
            ReceiveBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), receiveBill.getId());
            //如果输入无效id 返回未查询到收款单
            if (null == currentBill) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100579"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A9","未查询到收款单") /* "未查询到收款单" */);
            }
            //判断 OpenAPI 过来的单据 如果是审批流控制 则不允许审批
            if(ObjectUtils.isNotEmpty(currentBill.getIsWfControlled())){
                String str = paramMap.get("requestData").toString();
                if (str.contains("_fromApi") && currentBill.getIsWfControlled().equals(true)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100580"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AC", "当前单据已启用审批流，审核失败！") /* "当前单据已启用审批流，审核失败！" */);
                }
            }

            List<Journal> journalList = new ArrayList<Journal>();
            Date date = BillInfoUtils.getBusinessDate();
            Date currentPubts = receiveBillList.get(0).getPubts();
            if (currentPubts != null) {
                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100581"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A8","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            if (date != null && currentBill.getVouchdate() != null && date.compareTo(currentBill.getVouchdate()) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100582"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AA","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */);
            }
            if (currentBill.getSettlestatus() != null && currentBill.getSettlestatus().getValue() == 2) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100583"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AB","】已结算，不能进行审批！") /* "】已结算，不能进行审批！" */);
            }
            if (currentBill.getAuditstatus() != null && currentBill.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100584"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805AD","】已审批，不能进行重复审批！") /* "】已审批，不能进行重复审批！" */);
            }
            if (currentBill.getSrcitem() != null && currentBill.getSrcitem().getValue() != EventSource.Cmpchase.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100585"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A7","】不是现金自制单据，不能进行审批！") /* "】不是现金自制单据，不能进行审批！" */);
            }

            receiveBill.setEntityStatus(EntityStatus.Update);
            receiveBill.setAuditstatus(AuditStatus.Complete);
            receiveBill.setAuditorId(AppContext.getCurrentUser().getId());
            receiveBill.setAuditor(AppContext.getCurrentUser().getName());
            receiveBill.setAuditTime(new Date());
            receiveBill.setAuditDate(BillInfoUtils.getBusinessDate());

            currentBill.setAuditstatus(AuditStatus.Complete);
            currentBill.setAuditorId(AppContext.getCurrentUser().getId());
            currentBill.setAuditor(AppContext.getCurrentUser().getName());
            currentBill.setAuditTime(new Date());
            currentBill.setAuditDate(BillInfoUtils.getBusinessDate());
            journalService.updateJournal(currentBill);
        }


        return new RuleExecuteResult();
    }

}
