package com.yonyoucloud.fi.cmp.checkmanage.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置删除前规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:07
 */
@Component
@Slf4j
public class CheckManageDeleteBeforeRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<CheckManage> bills = getBills(billContext, map);
        for (CheckManage checkManage : bills) {
            Short auditstatus = checkManage.getAuditstatus();
            String code = checkManage.getCode();
            if (auditstatus == VerifyState.COMPLETED.getValue() || auditstatus == VerifyState.SUBMITED.getValue() || auditstatus == VerifyState.TERMINATED.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101284"),code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450001C",";仅允许删除已保存、已驳回状态并且生成方式为手工录入的单据") /* ";仅允许删除已保存、已驳回状态并且生成方式为手工录入的单据" */);
            }
            if (checkManage.getGenerateType() != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101285"),code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450001D","单据状态不是手工录入单据，不允许删除！") /* "单据状态不是手工录入单据，不允许删除！" */);
            }
            CheckManage checkManageQuery = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, checkManage.getId(), 2);
            List<CheckManageDetail> listb = checkManageQuery.CheckManageDetail();
            for (CheckManageDetail checkManageDetail : listb) {
                // 针对删除的支票，恢复支票工作台支票状态为处置前支票状态，同时清空处置前支票状态
                CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME,checkManageDetail.getCheckid());
                // CheckManageDetail checkManageDetailQuery = MetaDaoHelper.findById(CheckManageDetail.ENTITY_NAME, checkManageDetail.getId());
                checkOne.setCheckBillStatus(checkManageDetail.getCheckBillStatus()); // 支票处置子表第一次保存时的支票状态
                checkOne.setDisposer("");
                checkOne.setDisposalDate(null);
                checkOne.setHandletype(null);
                checkOne.setFailReason("");
                EntityTool.setUpdateStatus(checkOne);
                checkStatusService.recordCheckStatusByCheckId(checkOne.getId(),checkOne.getCheckBillStatus());
                MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
            }
        }
        return new RuleExecuteResult();
    }
}
