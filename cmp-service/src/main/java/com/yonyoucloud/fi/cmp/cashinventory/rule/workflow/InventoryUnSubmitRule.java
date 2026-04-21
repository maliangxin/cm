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
public class InventoryUnSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CashInventory cashInventory = MetaDaoHelper.findById(CashInventory.ENTITY_NAME,bizObject.getId(),null);
            if (null == cashInventory) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101571"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DD","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            Date currentPubts = bizObject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(cashInventory.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101572"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DF","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
            if(!cashInventory.getIsWfControlled()){
                if(verifystate == VerifyState.INIT_NEW_OPEN.getValue()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101573"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E0","单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
                }
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("unaudit", billContext, paramMap);
                result.setCancel(true);
            }else{
                if(!(verifystate == VerifyState.SUBMITED.getValue())){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101574"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DE","单据非审批中，不能进行撤回！") /* "单据非审批中，不能进行撤回！" */);
                }
            }
        }
        return result;
    }

}
