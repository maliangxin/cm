package com.yonyoucloud.fi.cmp.bankreconciliation.rule.outsystem;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 结算中心拉取银行对账生单后，单据状态回写
 * @author maliang
 * @version V1.0
 * @date 2021/8/17 11:11
 * @Copyright yonyou
 */
@Slf4j
@Component
public class BillStatusCallbackRule extends AbstractCommonRule implements ISagaRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("==============================   executing  BillStatusCallbackRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        Long billid = Long.valueOf(paramMap.get("id").toString());
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, billid, 1);
        YmsLock buildYmsLock = null;
        YmsLock acentityYmsLock = null;
        if(billid != null){
            try{
                // 针对收款单拉取银行对账单，进行加锁处理，避免同步操作
                if (bankReconciliation != null && (buildYmsLock =JedisLockUtils.lockBillWithOutTrace(billid.toString()))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101682"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180779","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                // 针对银行对账自动生单，对会计主体进行加锁处理，避免同步操作
                if ((acentityYmsLock = JedisLockUtils.lockBillWithOutTrace("autoGenerateBill" + bankReconciliation.getAccentity()))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101683"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077A","当前会计主体上次自动生单流程未结束") /* "当前会计主体上次自动生单流程未结束" */);
                }
                // 校验pubts
                if (paramMap.get("pubts") != null && ((Date) paramMap.get("pubts")).compareTo(bankReconciliation.getPubts()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101684"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077B","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                }
                // 判断生单标识，若生单成功则提示
                boolean autoBill = (boolean) paramMap.get("autoBill");
                if ((bankReconciliation != null && bankReconciliation.getAutobill() && autoBill) || bankReconciliation.getCheckflag()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101685"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180778","该对账单不是最新状态，请重新查询！") /* "该对账单不是最新状态，请重新查询！" */);
                }
                bankReconciliation.setAutobill(autoBill);//已生单
                // 生单成功打标识
                CommonSaveUtils.updateBankReconciliation(bankReconciliation);//更新对账单状态
                YtsContext.setYtsContext("billid",billid);//更新成功，在上下文中加入单据id
                YtsContext.setYtsContext("autoBill",autoBill);//更新成功，在上下文中加入生单状态
            }finally {
                JedisLockUtils.unlockBillWithOutTrace(buildYmsLock);
                JedisLockUtils.unlockBillWithOutTrace(acentityYmsLock);
            }

        }
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("==============================   BillStatusCallbackRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        Long billid = (Long)YtsContext.getYtsContext("billid");//从上下文取单据id
        boolean autoBill = (boolean)YtsContext.getYtsContext("autoBill");//从上下文取生单标识
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, billid, 1);
        bankReconciliation.setAutobill(!autoBill);//逆操作状态相反
        bankReconciliation.setEntityStatus(EntityStatus.Update);
        CommonSaveUtils.updateBankReconciliation(bankReconciliation);//更新对账单状态

        return new RuleExecuteResult();
    }
}
