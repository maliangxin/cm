package com.yonyoucloud.fi.cmp.foreignpayment.rule.business;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 外汇付款 删除规则*
 *
 * @author xuxbo
 * @date 2024/1/3 9:49
 */

@Slf4j
@Component
public class ForeignPaymentDeleteRule extends AbstractCommonRule {



    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            YmsLock ymsLock = null;
            try {
                if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(id))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100185"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D5", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                log.info("ForeignPaymentDeleteRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100186"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D1", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                log.info("ForeignPaymentDeleteRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                Date currentPubts = bizobject.getPubts();
                if (currentPubts != null) {
                    if (!currentPubts.equals(currentBill.getPubts())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100187"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D4", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }

                if (!ObjectUtils.isEmpty(currentBill.get(ICmpConstant.VERIFY_STATE))) {
                    short verifystate = Short.parseShort(currentBill.get(ICmpConstant.VERIFY_STATE).toString());
                    if (verifystate == VerifyState.SUBMITED.getValue()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100188"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D2", "单据审批中，无法删除！") /* "单据审批中，无法删除！" */);
                    }
                    if (verifystate == VerifyState.COMPLETED.getValue()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100189"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D3", "单据已审批，无法删除！") /* "单据已审批，无法删除！" */);
                    }
                    //终止的不让删除
//                    if (verifystate == VerifyState.TERMINATED.getValue()) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100190"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_192DE09204E00006", "单据已流程终止，无法删除！") /* "单据已流程终止，无法删除！" */);
//                    }
                }
                //OpenApi过来的数据赋值外部系统
                boolean openApiFlag = (bizobject.containsKey("_fromApi") && bizobject.get("_fromApi").equals(true)) || billDataDto.getFromApi();
                if (!openApiFlag && currentBill.getSrcitem() != null && currentBill.getSrcitem() == EventSource.ExternalSystem.getValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101161"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DA81BA405900007", "单据事项来源为外部系统，无法删除！") /* "单据事项来源为外部系统，无法删除！" */);
                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101162"),e.getMessage());
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        return new RuleExecuteResult();
    }

}
