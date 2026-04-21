package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.openapi.service.BankreconciliationOpenApiService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


/**
 * @description: 认领单保存后规则
 * 整单认领生成来款记录 --【心连心项目使用】
 * @author: liuwt@yonyou.com
 * @date: 2023/11/08 15:49
 */

@Slf4j
@Component("afterSaveBillClaimRule")
public class AfterSaveBillClaimRule extends AbstractCommonRule {

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Resource
    BankreconciliationOpenApiService bankreconciliationOpenApiService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BizObject bill = getBills(billContext, map).get(0);
        BillClaim billClaim = (BillClaim) bill;
        List<BillClaimItem> billClaimItemList = billClaim.get("items");
        BankReconciliation bankReconciliation = new BankReconciliation();
        if(billClaimItemList != null && billClaimItemList.size() > 0){
            bankReconciliation.setOppositeobjectid(billClaimItemList.get(0).getOppositeobjectid());
        }

        // 获取认领单信息
//        String result = bankreconciliationOpenApiService.claimPullAndPushToReceipt((BillClaim) bill,bankReconciliation);
        // 事务提交
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try{
                    BillClaim dbBillClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, billClaim.getId(), 3);
                    String result = bankreconciliationOpenApiService.claimPullAndPushToReceipt(dbBillClaim,bankReconciliation);
                    if(!"success".equals(result)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101831"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00067", "认领单生成来款记录失败") /* "认领单生成来款记录失败" */);
                    }
                }catch (Exception e){
                    log.error("事务提交后，调用单据转换规则及来款记录保存失败！"+e.getMessage(),e);
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049B", "事务提交后，调用单据转换规则及来款记录保存失败！") /* "事务提交后，调用单据转换规则及来款记录保存失败！" */+e.getMessage(),e);
                }
            }
        });

        return new RuleExecuteResult();
    }
}
