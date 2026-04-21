package com.yonyoucloud.fi.cmp.cashinventory.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cashinventory.CashInventory;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @since 2022-01-17
 */
@Slf4j
@Component
public class InventorySubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CashInventory cashInventory = MetaDaoHelper.findById(CashInventory.ENTITY_NAME,bizObject.getId(),3);
            if (null == cashInventory) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101070"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FE","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            Date currentPubts = bizObject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(cashInventory.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101071"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FF","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if(verifystate == VerifyState.INIT_NEW_OPEN.getValue() || verifystate == VerifyState.REJECTED_TO_MAKEBILL.getValue()){
                if(!cashInventory.getIsWfControlled()){
                    // 未启动审批流，单据直接审批通过
                    result = BillBiz.executeRule("audit", billContext, paramMap);
                    result.setCancel(true);
                }
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100308"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00033", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */ );
            }
        }
        return result;
    }

}
