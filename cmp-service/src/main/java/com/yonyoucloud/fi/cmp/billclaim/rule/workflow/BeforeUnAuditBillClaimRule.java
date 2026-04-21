package com.yonyoucloud.fi.cmp.billclaim.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.RecheckStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description: 我的认领弃审前规则
 * @author: zhoulyu@yonyou.com
 * @date: 2024/06/18 15:49
 */

@Slf4j
@Component("beforeUnAuditBillClaimRule")
public class BeforeUnAuditBillClaimRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100061"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CA", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            //CZFW-342875日常-智能到账改动】我的认领：认领完结状态为已完结或实际认领单位完结状态为已完结时，列表肩部撤回时，应进行校验，不可以撤回
            if ((!Objects.isNull(currentBill.get("associationstatus")) && currentBill.getShort("associationstatus").equals(AssociationStatus.Associated.getValue()))
                    || (!Objects.isNull(currentBill.get("refassociationstatus")) && currentBill.getShort(
                    "refassociationstatus").equals(AssociationStatus.Associated.getValue()))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102273"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D8", "认领完结状态为已完结或实际认领单位完结状态为已完结时，不允许撤回") /* "认领完结状态为已完结或实际认领单位完结状态为已完结时，不允许撤回" */);
            }
            if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
                //撤回成功后，认领单返回“已保存”状态
                //①认领状态='待复核'或无审批流，撤回成功；
                //②有审批流程且认领状态='审批中'，判定审批流中是否已经产生审批行为，如无则可撤回成功，不满足的失败提示：“单据编号XXX、YYY的已有后续审批，不允许撤回，请检查！”"
                //历史数据的处理 没有适配审批流程的历史数据没有审批人等信息 走审批rule会报错 这里直接更新信息就不走审批规则弃审了
                if (Objects.isNull(currentBill.get("auditor"))) {
                    bizobject.set("recheckstatus", RecheckStatus.Saved.getValue());
                    bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
                    bizobject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
                    bizobject.set("auditorId", null);
                    bizobject.set("auditor", null);
                    bizobject.set("auditDate", null);
                    bizobject.set("auditTime", null);
                    bizobject.setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(bizobject);
                }
            } else {
                if (bizobject.get("verifystate") != null) {
                    if (VerifyState.INIT_NEW_OPEN.getValue() == bizobject.getShort("verifystate")) {
                        bizobject.set("recheckstatus", RecheckStatus.Saved.getValue());
                    } else if (VerifyState.SUBMITED.getValue() == bizobject.getShort("verifystate")) {
                        bizobject.set("recheckstatus", RecheckStatus.Submited.getValue());
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
