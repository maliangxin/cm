package com.yonyoucloud.fi.cmp.receivebill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class ReceiveBillUnSubmitRule extends AbstractCommonRule{

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103035"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2628404E00001", "在财务新架构环境下，不允许撤回收款单。") /* "在财务新架构环境下，不允许撤回收款单。" */);
        }
        List<BizObject> receiveBillList = getBills(billContext, paramMap);
        if (receiveBillList == null || receiveBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101198"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180115","请选择单据！") /* "请选择单据！" */);
        }
        for (BizObject bizObject : receiveBillList) {
            Date currentPubts = bizObject.getPubts();
            ReceiveBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId());
            if (currentPubts != null) {
                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101199"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180113","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            if (currentBill.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101200"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180114","单据已审批") /* "单据已审批" */);
            }
        }
        return new RuleExecuteResult();
    }

}
