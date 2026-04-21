package com.yonyoucloud.fi.cmp.billclaim.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.RecheckStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @description: 我的认领撤回前规则
 * @author: zhoulyu@yonyou.com
 * @date: 2024/06/18 15:49
 */

@Slf4j
@Component("beforeUnSubmitBillClaimRule")
public class BeforeUnSubmitBillClaimRule extends AbstractCommonRule {

    @Autowired
    AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }

        //校验审批流状态为初始开立/驳回到制单时，不允许撤回，给出提示“单据【单据编号】为初始开立/驳回到制单状态，不允许撤回”
        if (currentBill.get("verifystate") != null && (VerifyState.INIT_NEW_OPEN.getValue() == bizObject.getShort("verifystate")
                || VerifyState.REJECTED_TO_MAKEBILL.getValue() == bizObject.getShort("verifystate") || VerifyState.TERMINATED.getValue() == bizObject.getShort("verifystate"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100001"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00041", "单据[%s]，为初始开立/驳回到制单状态，不允许撤回") /* "单据[%s]，为初始开立/驳回到制单状态，不允许撤回" */, currentBill.get(ICmpConstant.CODE).toString()));
        }
        //CZFW-342875日常-智能到账改动】我的认领：认领完结状态为已完结或实际认领单位完结状态为已完结时，列表肩部撤回时，应进行校验，不可以撤回
        if ((!Objects.isNull(currentBill.get("associationstatus")) && currentBill.getShort("associationstatus").equals(AssociationStatus.Associated.getValue()))
                || (!Objects.isNull(currentBill.get("refassociationstatus")) && currentBill.getShort(
                "refassociationstatus").equals(AssociationStatus.Associated.getValue()))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100002"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080020", "认领完结状态为已完结或实际认领单位完结状态为已完结时，不允许撤回") /* "认领完结状态为已完结或实际认领单位完结状态为已完结时，不允许撤回" */);
        }
        // 判断现金参数
        Boolean isRecheck = autoConfigService.getIsRecheck();
        //CZFW-343404【日常-智能到账改动】我的认领：开启复核参数时，认领单进行复核成功即认领成功后，肩部按钮-撤回时应不能撤回成功，只能取消复核后才能进行撤回
        if (!Objects.isNull(currentBill.get("recheckstatus")) && currentBill.getShort("recheckstatus").equals(RecheckStatus.Reviewed.getValue()) && isRecheck) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100003"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080021", "开启复核参数时，认领单进行认领成功后，不允许撤回") /* "开启复核参数时，认领单进行认领成功后，不允许撤回" */);
        }
        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
            //撤回成功后，认领单返回“已保存”状态
            //①认领状态='待复核'或无审批流，撤回成功；
            //②有审批流程且认领状态='审批中'，判定审批流中是否已经产生审批行为，如无则可撤回成功，不满足的失败提示：“单据编号XXX、YYY的已有后续审批，不允许撤回，请检查！”"
            //历史数据的处理 没有适配审批流程的历史数据没有审批人等信息 走审批rule会报错 这里直接更新信息就不走审批规则弃审了
            if (Objects.isNull(currentBill.get("auditor"))) {
                bizObject.set("recheckstatus", RecheckStatus.Saved.getValue());
                bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
                bizObject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
                bizObject.set("auditorId", null);
                bizObject.set("auditor", null);
                bizObject.set("auditDate", null);
                bizObject.set("auditTime", null);
                bizObject.setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.updateBillClaim(bizObject);
                result.setCancel(true);
                return result;
            } else {
                // 未启动审批流，单据直接审批拒绝
                result = BillBiz.executeRule(ICmpConstant.UN_AUDIT, billContext, map);
                result.setCancel(true);
                return result;
            }
        } else {
            if (currentBill.get("verifystate") != null) {
                if (VerifyState.INIT_NEW_OPEN.getValue() == bizObject.getShort("verifystate")) {
                    bizObject.set("recheckstatus", RecheckStatus.Saved.getValue());
                } else if (VerifyState.SUBMITED.getValue() == bizObject.getShort("verifystate")) {
                    bizObject.set("recheckstatus", RecheckStatus.Submited.getValue());
                }
            }
            if (currentBill.get("auditstatus") != null && (currentBill.get("auditstatus").toString()).equals(AuditStatus.Complete.getValue())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100004"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00042", "单据[%s]已审批，不允许撤回") /* "单据[%s]已审批，不允许撤回" */, currentBill.get(ICmpConstant.CODE).toString()));
            }
            return new RuleExecuteResult();
        }
    }
}
