package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 支票处置审核后规则
 */
@Slf4j
@Component
public class CheckManageAfterAuditRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizobject.getId(),2);
            // 审批流同意完成后，更新审批状态为审批通过
            checkManage.setAuditstatus(VerifyState.COMPLETED.getValue());
            EntityTool.setUpdateStatus(checkManage);
            MetaDaoHelper.update(CheckManage.ENTITY_NAME, checkManage);
            String auditor = bizobject.getString("auditor");// 处置人
            Date auditDate = bizobject.getDate("auditDate");// 处置日期

            // 更新支票状态
            List<CheckManageDetail> checkManageDetailList = checkManage.CheckManageDetail();
            // 处置方式为作废时，更新为已作废；处置方式为挂失时，更新为已挂失
            for (CheckManageDetail checkManageDetail : checkManageDetailList) {
                CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkManageDetail.getCheckid());
                if (checkManageDetail.getHandletypeDetail() == 1) { // 已挂失
                    checkOne.setCheckBillStatus(CmpCheckStatus.Loss.getValue());
                }
                if (checkManageDetail.getHandletypeDetail() == 2) { // 已作废
                    checkOne.setCheckBillStatus(CmpCheckStatus.Cancle.getValue());
                }
                if (checkManageDetail.getHandletypeDetail() == 3) { // 已退回
                    checkOne.setCheckBillStatus(CmpCheckStatus.Cancel.getValue());
                }
                checkOne.setDisposer(auditor);
                checkOne.setDisposalDate(BillInfoUtils.getBusinessDate());
                checkOne.setHandletype(checkManageDetail.getHandletypeDetail());
                checkOne.setFailReason(checkManageDetail.getHandlereason());
                EntityTool.setUpdateStatus(checkOne);
                checkStatusService.recordCheckStatusByCheckId(checkOne.getId(),checkOne.getCheckBillStatus());
                // 更新支票状态
                MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
            }
        }
        return new RuleExecuteResult();
    }

}
