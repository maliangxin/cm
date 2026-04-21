package com.yonyoucloud.fi.cmp.receivebill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.receivebill.rule.FiCmpReceiveBillBaseRule;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 收款单弃审后规则，主要用于解redis锁
 * 规则序号为40，在入库操作30的后面
 * @author lidchn
 * @version V1.0
 * date 2021年9月2日15:33:43
 * Copyright yonyou
 */
@Slf4j
@Component
public class AfterUnauditUnLockBillRule extends FiCmpReceiveBillBaseRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("##########   executing  ReceiveBillUnauditAfterRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        JedisLockUtils.unlockRuleWithOutTrace(paramMap);
        return new RuleExecuteResult();
    }

}
