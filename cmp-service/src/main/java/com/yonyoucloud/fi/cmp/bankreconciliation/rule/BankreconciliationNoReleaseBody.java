package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankreconciliationNoReleaseBody extends AbstractCommonRule {
    private final BankreconciliationService bankreconciliationService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (!QueryBaseDocUtils.getPeriodByService()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101746"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EF","未开通现金管理服务!") /* "未开通现金管理服务!" */);
        }
        BankreconciliationUtils.checkDataLegalList(bills, BankreconciliationActionEnum.NORELEASE);
        if (bills != null && bills.size()>0) {
            for (BizObject bizobject : bills){
                Long id = bizobject.getId();
                if (!ValueUtils.isNotEmptyObj(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101747"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800F0","ID不能为空！") /* "ID不能为空！" */);
                }
                String bankSeqNo = ValueUtils.isNotEmptyObj(bizobject.get("bank_seq_no")) ? bizobject.get("bank_seq_no").toString() : null;
//                if (StringUtils.isEmpty(bankSeqNo)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101748"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800F1","银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                }
                CtmJSONObject cancelMap = bankreconciliationService.cancelPublish(id,bankSeqNo);
                if(!cancelMap.getBoolean("dealSucceed")){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101749"),cancelMap.getString(ICmpConstant.MSG));
                }
            }
        }
        return new RuleExecuteResult();
    }

}
