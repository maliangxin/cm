package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置撤回后规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:28
 */
@Component
public class CheckManageUnSubmitAfterRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizObject.getId(), 2);
            short auditstatus = Short.parseShort(bizObject.get("auditstatus").toString());
            if (auditstatus == VerifyState.COMPLETED.getValue()) { // 审批通过
                // 將审批状态还原为审批中，审批流状态还原为审批中
                 //checkManage.setVerifystate(VerifyState.SUBMITED.getValue());
                checkManage.setAuditstatus(VerifyState.SUBMITED.getValue());
                EntityTool.setUpdateStatus(checkManage);
                MetaDaoHelper.update(CheckManage.ENTITY_NAME, checkManage);
            }
            if (auditstatus == VerifyState.SUBMITED.getValue()) { // 审批中
                // 將审批状态还原为已保存，审批流状态还原为初立
                // checkManage.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
                checkManage.setAuditstatus(VerifyState.INIT_NEW_OPEN.getValue());
                EntityTool.setUpdateStatus(checkManage);
                MetaDaoHelper.update(CheckManage.ENTITY_NAME, checkManage);
            }
            // 更新支票状态为处置中
            List<CheckManageDetail> checkManageDetailList = checkManage.CheckManageDetail();
            for (CheckManageDetail checkManageDetail : checkManageDetailList) {
                CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkManageDetail.getCheckid());
                checkOne.setCheckBillStatus(CmpCheckStatus.Disposal.getValue()); // 处置中
                EntityTool.setUpdateStatus(checkOne);
                checkStatusService.recordCheckStatusByCheckId(checkOne.getId(),checkOne.getCheckBillStatus());
                MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
            }
        }
        return result;
    }
}
