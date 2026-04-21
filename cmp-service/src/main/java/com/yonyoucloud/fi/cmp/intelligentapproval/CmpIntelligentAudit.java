package com.yonyoucloud.fi.cmp.intelligentapproval;

import com.yonyoucloud.ssc.intelligent.audit.sdk.dto.SyncExecuteResultDTO;
import org.imeta.orm.base.BizObject;

public interface CmpIntelligentAudit {
    SyncExecuteResultDTO auditStart(BizObject bizObject, String billNum, String formId, Short businessPart) throws Exception;
}
