package com.yonyoucloud.fi.cmp.receivebill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *   收付款单远程审核规则
 * @author maliang
 * @version V1.0
 * @date 2021/4/15 15:19
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpReceiveBillAuditRule extends FiCmpReceiveBillBaseRule implements ISagaRule {
    @Autowired
    CooperationFileService cooperationFileService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103027"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2613604E00004", "在财务新架构环境下，不允许审批收款单。") /* "在财务新架构环境下，不允许审批收款单。" */);
        }
        log.error("##   #####   executing  FiCmpReceiveBillAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));

        List<ReceiveBill> receiveBillList = getReceiveBillFromRequest(billContext,paramMap);
        // 应收单据在现金不存在，直接结束流程。
        if(receiveBillList.size() == 0){
            return new RuleExecuteResult();
        }
        Long receiveBillId = receiveBillList.get(0).getId();
        YmsLock ymsLock = null;
        try {
            if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(receiveBillId.toString()))==null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101261"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418047D","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            audit(receiveBillList.get(0));
            YtsContext.setYtsContext("receiveBillId",receiveBillId);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            try {
                // 附件拷贝：应收应付->收付款工作台
                cooperationFileService.copyFiles("yonbip-fi-arap",receiveBillList.get(0).get("id").toString(),
                        "yonbip-fi-ctmcmp", receiveBillList.get(0).get("id").toString(), null, null);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101262"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508010E", "附件同步异常：") /* "附件同步异常：" */+e.getMessage());
            }
        }catch(Exception e){
            log.error(e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101263"),e.getMessage());
        }
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("##   #####   FiCmpReceiveBillAuditRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        // 审核失败，调用取消审核流程恢复单据状态
        if(YtsContext.getYtsContext("receiveBillId") == null){
            return new RuleExecuteResult();
        }
        Long receiveBillId = (Long) YtsContext.getYtsContext("receiveBillId");
        ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, receiveBillId);
        if(receiveBill != null){
            unaudit(receiveBill);
        }
        return new RuleExecuteResult();
    }


}
