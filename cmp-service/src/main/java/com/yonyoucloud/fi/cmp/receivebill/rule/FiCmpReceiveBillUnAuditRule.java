package com.yonyoucloud.fi.cmp.receivebill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *   收款单远程弃审规则
 * @author maliang
 * @version V1.0
 * @date 2021/4/15 15:19
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpReceiveBillUnAuditRule extends FiCmpReceiveBillBaseRule implements ISagaRule {

    @Autowired
    ReceiveBillService receiveBillService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103030"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C261B404E00007", "在财务新架构环境下，不允许弃审收款单。") /* "在财务新架构环境下，不允许弃审收款单。" */);
        }
        log.error("##   #####   executing  FiCmpReceiveBillUnAuditRule , request paramMap = {}", JsonUtils.toJson(paramMap));

        List<ReceiveBill> receiveBillList =  this.getReceiveBillFromRequest(billContext,paramMap);
        // 应收单据在现金不存在，直接结束流程。
        if(receiveBillList.size() == 0){
            return new RuleExecuteResult();
        }
        Long receiveBillId = receiveBillList.get(0).getId();
        YmsLock ymsLock = null;
        try {
            if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(receiveBillId.toString()))==null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100177"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800F7","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            unaudit(receiveBillList.get(0));
            YtsContext.setYtsContext("receiveBillId",receiveBillId);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }catch (Exception e){
            log.error(e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100178"),e.getMessage());
        }
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("##   #####   FiCmpReceiveBillUnAuditRule cancel, request paramMap = {}", new Object[]{JsonUtils.toJson(paramMap)});
        // 取消审核失败，调用审核流程重置单据状态
        if(YtsContext.getYtsContext("receiveBillId") == null){
            return new RuleExecuteResult();
        }
        Long receiveBillId =  (Long) YtsContext.getYtsContext("receiveBillId");
        ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, receiveBillId);
        if(receiveBill != null ){
            audit(receiveBill);
        }

        return new RuleExecuteResult();
    }



}
