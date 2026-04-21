package com.yonyoucloud.fi.cmp.intelligentapproval;

import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.ResultState;
import com.yonyoucloud.fi.cmp.util.basedoc.BillTypeQueryService;
import com.yonyoucloud.ssc.intelligent.audit.sdk.IIntelligentAuditCheckService;
import com.yonyoucloud.ssc.intelligent.audit.sdk.dto.SyncExecuteResultDTO;
import com.yonyoucloud.ssc.intelligent.audit.sdk.dto.SyncSolutionStartDTO;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CmpIntelligentAuditImpl implements CmpIntelligentAudit {

    @Autowired
    private IIntelligentAuditCheckService iIntelligentAuditCheckService;
    @Autowired
    private BillTypeQueryService billTypeQueryService;

    @Override
    public SyncExecuteResultDTO auditStart(BizObject bizObject, String billNum, String formId, Short businessPart) throws Exception {
        BdBillType billType = billTypeQueryService.queryBillTypeId(formId);
        SyncSolutionStartDTO syncSolutionStartDTO = new SyncSolutionStartDTO();
        if (bizObject.getId() instanceof Long) {
            syncSolutionStartDTO.setBillId(Long.toString(bizObject.getId()));
        } else {
            syncSolutionStartDTO.setBillId(bizObject.getId());
        }
        //CZFW-487874 跟共享同事沟通后，他们反馈 保存传bizData,提交不传bizData；已经现场验证过，没问题
        if (businessPart != 1) {
            syncSolutionStartDTO.setBizData(bizObject);
        }
        syncSolutionStartDTO.setBillTypeId(billType.getId());
        Object tradetype = bizObject.get("tradetype");
        if (tradetype != null) {
            syncSolutionStartDTO.setTransTypeId(tradetype.toString());
        }
        syncSolutionStartDTO.setBusinessPart(businessPart);
        syncSolutionStartDTO.setBusinessLine("business-filling");
        SyncExecuteResultDTO syncExecuteResultDTO = iIntelligentAuditCheckService.auditStart(syncSolutionStartDTO);
        log.error("billNum：{},智能审核结果：{}", billNum, syncExecuteResultDTO);
        if (syncExecuteResultDTO.getSuccess()) {
            if (syncExecuteResultDTO.getResultState() != null && syncExecuteResultDTO.getResultState() == ResultState.RigidNotPassed.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102634"),syncExecuteResultDTO.getMessage());
            }
        } else {
            log.error("billNum：{},智能审核调用失败", billNum);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102635"),syncExecuteResultDTO.getMessage());
        }
        return syncExecuteResultDTO;
    }
}
