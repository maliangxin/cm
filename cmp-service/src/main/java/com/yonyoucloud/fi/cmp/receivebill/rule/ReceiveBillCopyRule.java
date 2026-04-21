package com.yonyoucloud.fi.cmp.receivebill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 付款单复制规则
 *
 * @author lidchn
 * @version V1.0
 * @date 2021年5月26日11:24:16
 * @Copyright yonyou
 */
@Slf4j
@Component
public class ReceiveBillCopyRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103031"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C261D804E00004", "在财务新架构环境下，不允许复制收款单。") /* "在财务新架构环境下，不允许复制收款单。" */);
        }
        return new RuleExecuteResult();
    }

}
