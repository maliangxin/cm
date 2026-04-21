package com.yonyoucloud.fi.cmp.bankreconciliation.rule;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 分配业务人员
 * @date 2022-9-30
 * @author xujhn
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankreconciliationDispatchBussiness extends AbstractCommonRule {
    private final BankreconciliationService bankreconciliationService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (!QueryBaseDocUtils.getPeriodByService()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101072"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180100","未开通现金管理服务!") /* "未开通现金管理服务!" */);
        }
        // 对接人
        String[] userids = (String[]) paramMap.get("ids");
        if (bills != null && bills.size()>0) {
            for (BizObject bizobject : bills){
                Long id = bizobject.getId();
                if (!ValueUtils.isNotEmptyObj(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101073"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FE","ID不能为空！") /* "ID不能为空！" */);
                }
                String bankSeqNo = ValueUtils.isNotEmptyObj(bizobject.get("bank_seq_no")) ? bizobject.get("bank_seq_no").toString() : null;
//                if (StringUtils.isEmpty(bankSeqNo)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101074"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FF","银行交易流水号不能为空！") /* "银行交易流水号不能为空！" */);
//                }
                CtmJSONObject publishDispatchMap = bankreconciliationService.dispatchBussiness(id.toString(),userids,false);
                if(!publishDispatchMap.getBoolean("dealSucceed")){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101075"),publishDispatchMap.getString(ICmpConstant.MSG));
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101076"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180101","请选择单据！") /* "请选择单据！" */);
        }
        return new RuleExecuteResult();
    }

}
