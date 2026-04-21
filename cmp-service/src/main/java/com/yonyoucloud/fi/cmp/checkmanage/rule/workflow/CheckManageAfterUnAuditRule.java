package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置审批之后撤回后规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-06-19 17:55
 */
@Slf4j
@Component("checkManageAfterUnAuditRule")
public class CheckManageAfterUnAuditRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizObject.getId(), 2);
//            CtmJSONObject jsonObject = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(checkManage));
//            Integer isWfControlled = jsonObject.getInteger("isWfControlled");
//            if (isWfControlled == null) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101617"),checkManage.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450000F","字段isWfControlled为空，请检查！") /* "字段isWfControlled为空，请检查！" */);
//            }
            Boolean isWfControlled = checkManage.getIsWfControlled();
            if (!isWfControlled) {
                // 不走审批流，將审批状态直接还原为已保存
                // checkManage.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
                checkManage.setAuditstatus(VerifyState.INIT_NEW_OPEN.getValue());
                EntityTool.setUpdateStatus(checkManage);
                MetaDaoHelper.update(CheckManage.ENTITY_NAME, checkManage);
            } else {
                // 走审批流，將审批状态还原为审批中
                //checkManage.setVerifystate(VerifyState.SUBMITED.getValue());
                checkManage.setAuditstatus(VerifyState.SUBMITED.getValue());
                EntityTool.setUpdateStatus(checkManage);
                MetaDaoHelper.update(CheckManage.ENTITY_NAME, checkManage);
            }
            // 更新支票状态为处置中
            List<CheckManageDetail> checkManageDetailList = checkManage.CheckManageDetail();
            for (CheckManageDetail checkManageDetail : checkManageDetailList) {
                CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkManageDetail.getCheckid());
                checkOne.setDisposer("");
                checkOne.setDisposalDate(null);
                checkOne.setHandletype(null);
                checkOne.setFailReason("");
                checkOne.setCheckBillStatus("11"); // 处置中
                EntityTool.setUpdateStatus(checkOne);
                checkStatusService.recordCheckStatusByCheckId(checkOne.getId(),checkOne.getCheckBillStatus());
                MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
            }
        }
        return new RuleExecuteResult();
    }

}
