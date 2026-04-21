package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.business.JournalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 付款单远程删除规则
 * @author liuttm
 * @version V1.0
 * @date 2021/4/20 16:00
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpPaymentDeleteRule extends FiCmpPaymentBaseRule implements ISagaRule {
    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    //530需求，删除付款工作台关联的银行对账单关联关系
    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103016"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2579404E00007", "在财务新架构环境下，不允许删除付款单。") /* "在财务新架构环境下，不允许删除付款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("##   #####   executing  FiCmpPaymentDeleteRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        List<BizObject> bills= getBills(billContext, paramMap);


        // 查询现金单据
        PayBill bill =  getPayBill(bills.get(0).getId());
        // 应收单据在现金不存在，直接结束流程。
        if(bill == null){
            return new RuleExecuteResult();
        }
        Long payBillId = bill.getId();
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(payBillId.toString());
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101023"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418061A","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            YtsContext.setYtsContext("bill",bill);
            //删除日记账，更新账面余额
            CmpWriteBankaccUtils.delAccountBook(payBillId.toString());
            //删除银行对账单关联关系
            if (bill.getAssociationStatus()!=null  && AssociationStatus.Associated.getValue() == bill.getAssociationStatus()){
                CtmJSONObject jsonReq = new CtmJSONObject();
                if (ObjectUtils.isNotEmpty(bill.getBankReconciliationId())) {
                    jsonReq.put("busid", bill.getBankReconciliationId());
                }
                if (ObjectUtils.isNotEmpty(bill.getBillClaimId())){
                    jsonReq.put("claimid", bill.getBillClaimId());
                }
                jsonReq.put("stwbbusid", bill.getId());
                reWriteBusCorrDataService.resDelData(jsonReq);
            }
            // 批量删除单据
            MetaDaoHelper.delete(PayBill.ENTITY_NAME,bill);
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }catch(Exception e){
            log.error(e.getMessage());
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101023"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418061A","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
        }
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("##   #####   FiCmpPaymentDeleteRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        // 异常回滚，对删除的数据重新插入数据库
        if(YtsContext.getYtsContext("bill") == null){
            return new RuleExecuteResult();
        }
        PayBill oldbill = (PayBill) YtsContext.getYtsContext("bill");
        oldbill.setEntityStatus(EntityStatus.Insert);
        // 对于已删除的数据需重新记账并存入库中
        billContext.setFullname(PayBill.ENTITY_NAME);
        Journal journal = JournalUtil.createJounal(oldbill,billContext);
        cmpWriteBankaccUtils.addAccountBook(journal);
        CmpMetaDaoHelper.insert(PayBill.ENTITY_NAME, oldbill);
        return new RuleExecuteResult();
    }

}
