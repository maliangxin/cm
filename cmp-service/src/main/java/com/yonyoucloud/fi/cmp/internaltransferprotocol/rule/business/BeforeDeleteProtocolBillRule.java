package com.yonyoucloud.fi.cmp.internaltransferprotocol.rule.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.IsEnable;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocol;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.ProtocolCallLogs;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.DataSources;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>内转协议查询前置规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-18 7:44
 */
@Component("beforeDeleteProtocolBillRule")
public class BeforeDeleteProtocolBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);

        BizObject bizObject = bills.get(0);
        InternalTransferProtocol currentBill = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, bizObject.getId(), 2);
        List<ProtocolCallLogs> protocolCallLogs = currentBill.ProtocolCallLogs();
        if (CollectionUtils.isNotEmpty(protocolCallLogs)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101258"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194060E204F0000A", "当前内转协议已被引用，不支持删除，如不再使用，请停用!") /* "当前内转协议已被引用，不支持删除，如不再使用，请停用!" */);
        }
        Short dataSources = bizObject.getShort(ICmpConstant.DATA_SOURCES);
        if (dataSources != DataSources.MANUALLY_ADD.getValue()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101259"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_195226D80558000D","非手工新增的单据，不允许删除！") /* "非手工新增的单据，不允许删除！" */);
        }

        Short isEnabledType = bizObject.getShort(ICmpConstant.IS_ENABLED_TYPE);
        if (isEnabledType == IsEnable.ENABLE.getValue()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101260"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_197C7F0604D0000D","启用中的内转协议，不允许删除！") /* "启用中的内转协议，不允许删除！" */);
        }

        return new RuleExecuteResult();
    }
}
