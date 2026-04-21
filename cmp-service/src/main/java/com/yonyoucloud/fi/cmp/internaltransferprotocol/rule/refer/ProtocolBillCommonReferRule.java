package com.yonyoucloud.fi.cmp.internaltransferprotocol.rule.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.TransfereeInformation;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>内转协议参照过滤</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-08 17:30
 */
@Component("protocolBillCommonReferRule")
public class ProtocolBillCommonReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        if (!"cmp_internaltransferprotocol".equals(billDataDto.getBillnum())) {
            return new RuleExecuteResult();
        }
        List<BizObject> bizObjectList = getBills(billContext, map);
        BizObject bizObject = null;
        if (!bizObjectList.isEmpty()) {
            bizObject = bizObjectList.get(0);
        }
        assert bizObject != null;
        boolean isFilter = "filter".equals(billDataDto.getExternalData());
        // 如果是过滤区，直接跳过
        if (isFilter) {
            return new RuleExecuteResult();
        }
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && "enterpriseBankAccount_account".equals(billDataDto.getKey())) {
            List<TransfereeInformation> transfereeInformationList = bizObject.get("TransfereeInformation");
            if (!transfereeInformationList.isEmpty()) {
                Object intoAccentity = transfereeInformationList.get(0).get("intoAccentity");
                if (!ValueUtils.isNotEmptyObj(intoAccentity)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100987"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D6F6B9404C00008", "请先选择当前明细行的转入资金组织！") /* "请先选择当前明细行的转入资金组织！" */);
                }
                bizObject.set(ICmpConstant.ACCENTITY, intoAccentity);
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", intoAccentity));
            }
            Object currency = bizObject.get(ICmpConstant.CURRENCY);
            if (!ValueUtils.isNotEmptyObj(currency)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100988"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080085", "请先选择主表的币种！") /* "请先选择主表的币种！" */);
            }
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "eq", currency));
        }
        if ("finbd.bd_expenseitemref".equals(billDataDto.getrefCode())) {
            List<TransfereeInformation> transfereeInformationList = bizObject.get("TransfereeInformation");
            if (!transfereeInformationList.isEmpty()) {
                Object intoAccentity = transfereeInformationList.get(0).get("intoAccentity");
                if (!ValueUtils.isNotEmptyObj(intoAccentity)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100987"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D6F6B9404C00008", "请先选择当前明细行的转入资金组织！") /* "请先选择当前明细行的转入资金组织！" */);
                }
                bizObject.set(ICmpConstant.ACCENTITY, intoAccentity);
            }
        }
        return new RuleExecuteResult();
    }
}
