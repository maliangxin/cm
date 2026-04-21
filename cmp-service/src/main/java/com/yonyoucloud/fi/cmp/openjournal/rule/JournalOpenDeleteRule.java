package com.yonyoucloud.fi.cmp.openjournal.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.journal.JournalVo;
import com.yonyoucloud.fi.cmp.openjournal.JournalOpenCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;


/**
 * @author shangxd
 * @description 删除日记账的规则，可解决分布式事务
 * @date 2021-05-21
 */
@Component("journalOpenDeleteRule")
@Slf4j
@RequiredArgsConstructor
public class JournalOpenDeleteRule extends AbstractCommonRule implements ISagaRule {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JournalOpenDeleteRule.class);

    private final JournalOpenCommonService journalForeginCommonService;

    private static final String RULE_JOURNAL_DELETE_ROLLBACK="RULE_JOURNAL_DELETE_ROLLBACK";
    /**
     * 执行登账逻辑的方法，更新日记账明细
     *
     * @author shangxd
     * @date 11:17
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("JournalOpenDeleteRule.execute paramMap:{}", CtmJSONObject.toJSONString(paramMap));
        Object journalObj = paramMap.get("journal");
        if(!Objects.isNull(journalObj)){
            JournalVo journalVo = (JournalVo) journalObj;
            YtsContext.setYtsContext(RULE_JOURNAL_DELETE_ROLLBACK, false);
            journalForeginCommonService.journalDelete(journalVo);
            YtsContext.setYtsContext(RULE_JOURNAL_DELETE_ROLLBACK, true);
            paramMap.put(RULE_JOURNAL_DELETE_ROLLBACK,true);
        }
        return new RuleExecuteResult(paramMap);
    }

    /***
     * 更新日记账接口出现异常的回滚逻辑
     * @author shangxd
     * @date 11:19
     */
    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> map) throws Exception {
        log.error("JournalOpenDeleteRule.cancel异常回滚，参数是:{}" , CtmJSONObject.toJSONString(map));
        Object journalObj = map.get("journal");
        if (!Objects.isNull(journalObj) ) {
            if((null != map.get(RULE_JOURNAL_DELETE_ROLLBACK) && (boolean) map.get(RULE_JOURNAL_DELETE_ROLLBACK))
                    || (boolean) YtsContext.getYtsContext(RULE_JOURNAL_DELETE_ROLLBACK)){
                JournalVo journalVo = (JournalVo) journalObj;
                CtmJSONObject json = new CtmJSONObject();
                journalForeginCommonService.rollbackJournalDelete(journalVo);
                json.put("success", true);
                json.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A3","日记账回滚逻辑执行成功！") /* "日记账回滚逻辑执行成功！" */);
            }
        } else {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A4","JournalOpenDeleteRule.cancel方法参数为空！") /* "JournalOpenDeleteRule.cancel方法参数为空！" */);
        }
        return new RuleExecuteResult(map);
    }


}
