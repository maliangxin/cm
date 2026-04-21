package com.yonyoucloud.fi.cmp.stwb.journal.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.stwb.JournalCommonService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @author wangshbv
 * @description 现金管理为结算提供的登账的规则，可解决分布式事务
 * @date 2021-05-21
 */

@Component("journalApproveRuleForStwb")
@Slf4j
public class JournalApproveRuleForStwb extends AbstractCommonRule implements ISagaRule {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JournalApproveRuleForStwb.class);


    @Autowired
    private JournalCommonService journalCommonService;
    /**
     * 日记账审核接口
     * journalType   日记账类型 1：新增， 2：变更后提交 3:提交数据后状态直接是已审批, 4 :调本接口直接是结算成功
     * accentity     资金组织
     * srcbillno     单据来源号
     *
     * @author wangshbv
     * @date 11:17
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        CtmJSONObject approveData = (CtmJSONObject) paramMap.get("approveData");
        if(log.isInfoEnabled()) {
            log.info("**********JournalApproveRuleForStwb.execute={}", approveData);
        }
        journalCommonService.journalApproveForStwb(approveData);
        return new RuleExecuteResult(paramMap);
    }

    /***
     * 登账接口出现异常的回滚逻辑
     * @author wangshbv
     * @date 11:19
     */
    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> map) throws Exception {
        CtmJSONObject param = (CtmJSONObject) map.get("approveData");
        if(log.isInfoEnabled()) {
            log.info("---------执行JournalApproveRuleForStwb.cancel现金审批接口异常回滚 -------------", CtmJSONObject.toJSONString(param));
        }
        String type = param.getString("type");
        String accentity = param.getString(IBussinessConstant.ACCENTITY);
        CtmJSONArray srcbillitembodyidArray = param.getJSONArray("srcbillitembodyid");
        if (Objects.isNull(type)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101568"),MessageUtils.getMessage("P_YS_FI_CM_0001186847") /* "参数type不能为空！" */);
        }
        if (srcbillitembodyidArray == null || srcbillitembodyidArray.size() < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101569"),MessageUtils.getMessage("P_YS_FI_CM_0001215791") /* "单据明细id不能为空！" */);
        }
        List<Journal> updateList = new ArrayList<>();
        List<String> srcBillItemIdList = getSrcbillitemidFromParam(srcbillitembodyidArray);
        //如果是驳回，止付
        if("2".equals(type) || "5".equals(type)){
            //回滚驳回，止付的逻辑
            journalCommonService.rollbackJournalDataFromRedis(srcBillItemIdList, param.getString("requestId"));
        }else{
            List<Journal> journalList = journalCommonService.getJournalsByItemBodyIdList(accentity, srcBillItemIdList);
            if (CollectionUtils.isNotEmpty(journalList)) {
                switch (type) {
                    case "1":
                        //审核通过
                        updateList = journalCommonService.setJournalInfoByType(journalList, AuditStatus.Complete, AuditStatus.Incomplete);
                        break;
                    case "3":
                        //回滚 日记账成功的代码
                        updateList = journalCommonService.settleSuccessCancel(journalList);
                        break;
                    case "4":
                        //弃审
                        updateList = journalCommonService.setJournalInfoByType(journalList, AuditStatus.Incomplete, AuditStatus.Complete);
                        break;
                    default:
                        break;
                }
                if (CollectionUtils.isNotEmpty(updateList)) {
                    EntityTool.setUpdateStatus(updateList);
                    MetaDaoHelper.update(Journal.ENTITY_NAME, updateList);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101570"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080054", "明细id:") /* "明细id:" */ + srcBillItemIdList.toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080055", "对应的日记账数据为空！") /* "对应的日记账数据为空！" */);
            }
        }
        if(log.isInfoEnabled()) {
            log.info("**********JournalRegisterRuleForStwb.execute={}", map);
        }
        return new RuleExecuteResult(map);
    }



    /**
     * 获取结算明细的id
     * @author wangshbv
     * @date 10:03
     */
    private List<String> getSrcbillitemidFromParam(CtmJSONArray srcbillitemidArray) {
        List<String> resultList = new ArrayList<>();
        for (int i = 0, size = srcbillitemidArray.size(); i < size; i++) {
            resultList.add(srcbillitemidArray.getJSONObject(i).get("id").toString());
        }
        return resultList;
    }






}
