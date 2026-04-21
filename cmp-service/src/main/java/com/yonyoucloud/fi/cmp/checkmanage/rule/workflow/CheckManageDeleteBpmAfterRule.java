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
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支票处置驳回和撤销驳回以及终止规则
 *
 * @Author: 闫小凯
 * @ClassName: CheckManageDeleteBpmAfterRule
 * @Date: 2023/06/19 9:00
 **/
@Component("checkManageDeleteBpmAfterRule")
public class CheckManageDeleteBpmAfterRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    /**
     * 驳回
     */
    private static final String ACTION_REJECT = "REJECTTOSTART";

    /**
     * 撤销驳回
     */
    private static final String ACTION_WITHDRAW_REJECT = "WITHDRAWREJECTTOSTART";

    /**
     * 终止
     */
    private static final String ACTION_TERMINATED = "ACTIVITI_DELETED";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isNotEmpty(bills)) {
            BizObject bill = bills.get(0);
            /**
             * 此处使用BizObject而不是CheckManage实体，原因是中广核对CheckManage表进行了扩展，有平行表；
             * BizObject继承Map可以拿到平行表字段，转成CheckManage强类型则会丢失，导致后续MetaDaoHelper.update更新失败
             */
            BizObject checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bill.getId(), 2);
            if (ACTION_REJECT.equalsIgnoreCase(billContext.getDeleteReason())) {
                //驳回
                checkManage.set("auditstatus",VerifyState.REJECTED_TO_MAKEBILL.getValue());
                checkManage.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(billContext.getFullname(), checkManage);
            } else if (ACTION_WITHDRAW_REJECT.equalsIgnoreCase(billContext.getDeleteReason())) {
                //撤销驳回
                checkManage.set("auditstatus",VerifyState.SUBMITED.getValue());
                checkManage.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(billContext.getFullname(), checkManage);
            } else if (ACTION_TERMINATED.equalsIgnoreCase(billContext.getDeleteReason())) {
                //终止
                checkManage.set("auditstatus",VerifyState.TERMINATED.getValue());
                checkManage.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(billContext.getFullname(), checkManage);

                // 单据审批状态变更为已终止，支票状态已入库；
                List<CheckManageDetail> listb = ((CheckManage) checkManage).CheckManageDetail();
                for (CheckManageDetail checkManageDetail : listb) {
                    CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkManageDetail.getCheckid());
                    checkOne.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
                    EntityTool.setUpdateStatus(checkOne);
                    checkStatusService.recordCheckStatusByCheckId(checkOne.getId(),checkOne.getCheckBillStatus());
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
                }
            }

        }
        return new RuleExecuteResult();
    }
}
