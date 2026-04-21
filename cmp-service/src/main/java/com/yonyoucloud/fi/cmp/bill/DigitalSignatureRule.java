package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.util.DigitalSignatureUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @ClassName DigitalSignatureRule
 * @Description 数据签名规则
 * @Author tongyd
 * @Date 2019/6/27 16:05
 * @Version 1.0
 **/
@Component
public class DigitalSignatureRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills == null || bills.size() == 0) {
            return new RuleExecuteResult();
        }
        BizObject bizObject = bills.get(0);

        //对关键数据项进行数据签名
        String originalMsg = DigitalSignatureUtils.getOriginalMsg(bizObject);
        CtmSignatureService signatureService = AppContext.getBean(CtmSignatureService.class);
        bizObject.set("signature", signatureService.iTrusSignMessage(originalMsg));
        return new RuleExecuteResult();
    }
}
