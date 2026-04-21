package com.yonyoucloud.fi.cmp.stwb.journal.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.stwb.JournalCommonService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author wangshbv
 * @description 现金管理为结算提供的登账的规则，可解决分布式事务
 * @date 2021-05-21
 */

@Component("journalRegisterRuleForStwb")
@Slf4j
public class JournalRegisterRuleForStwb extends AbstractCommonRule implements ISagaRule {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JournalRegisterRuleForStwb.class);

    @Autowired
    private JournalCommonService journalCommonService;

    /**
     * 执行登账逻辑的方法，生成日记账明细
     * journalType   日记账类型 1：新增， 2：变更后提交 3:提交数据后状态直接是已审批, 4 :调本接口直接是结算成功
     * accentity     资金组织
     * srcbillno     单据来源号
     *
     * @author wangshbv
     * @date 11:17
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        CtmJSONObject journalTransferInterfaceData = (CtmJSONObject) paramMap.get("journalTransferInterface");
        if(log.isInfoEnabled()) {
            log.info("----执行journalRegisterRuleForStwb.execute方法，参数是：------", CtmJSONObject.toJSONString(journalTransferInterfaceData));
        }
        journalCommonService.journalRegisterForStwb(journalTransferInterfaceData);
        return new RuleExecuteResult(paramMap);
    }

    /***
     * 登账接口出现异常的回滚逻辑
     * @author wangshbv
     * @date 11:19
     */
    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> map) throws Exception {
        CtmJSONObject param = (CtmJSONObject) map.get("journalTransferInterface");
        if(log.isInfoEnabled()) {
            log.info("---------执行journalRegisterRuleForStwb.cancel异常回滚，参数是：---", CtmJSONObject.toJSONString(param));
        }
        if (param != null) {
            CtmJSONArray data = param.getJSONArray("data");
            CtmJSONObject json = new CtmJSONObject();
            List<String> itemBodyIdList = new ArrayList<>();
            for (int i = 0, size = data.size(); i < size; i++) {
                itemBodyIdList.add(data.getJSONObject(i).get("srcbillitembodyid").toString());
            }
            List<Journal> journalList = journalCommonService.getJournalsByItemBodyIdList(param.getString(IBussinessConstant.ACCENTITY), itemBodyIdList);
            //回滚期初余额,并删除日记账
            journalCommonService.rollbackInitDataAndJournalSecond(journalList, 2);
            json.put("success", true);
            json.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540059D", "日记账回滚逻辑执行成功！") /* "日记账回滚逻辑执行成功！" */);
        } else {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540059E", "JournalRegisterRuleForStwb.cancel方法参数为空！") /* "JournalRegisterRuleForStwb.cancel方法参数为空！" */);
        }
        return new RuleExecuteResult(map);
    }


}
