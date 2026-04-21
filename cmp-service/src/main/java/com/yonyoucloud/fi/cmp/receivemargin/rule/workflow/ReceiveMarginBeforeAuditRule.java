package com.yonyoucloud.fi.cmp.receivemargin.rule.workflow;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 支付保证金审核前规则
 *
 * @author xuxbo
 * @date 2023/8/3 14:31
 */
@Slf4j
@Component
public class ReceiveMarginBeforeAuditRule extends AbstractCommonRule {

    @Autowired
    MarginWorkbenchService marginWorkbenchService;
    @Autowired
    MarginCommonService marginCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            YmsLock ymsLock = null;
            try {
                ymsLock = JedisLockUtils.lockRuleWithOutTrace(id,map);
                if (null == ymsLock) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100065"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051B", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                CtmJSONObject data = new CtmJSONObject();
                log.info("ReceiveMarginBeforeAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ReceiveMargin currentBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizobject.getId());
                log.info("ReceiveMarginBeforeAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100066"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180517", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                Date currentPubts = bizobject.getPubts();
                if (currentPubts != null) {
                    if (!currentPubts.equals(currentBill.getPubts())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100067"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051A", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }
                Date date = currentBill.getVouchdate();
                if (null != billContext.getDeleteReason()) {
                    if (ICmpConstant.DELETEALL.equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                        return new RuleExecuteResult();
                    }
                }
                Date currentDate = BillInfoUtils.getBusinessDate();
                if (currentDate.compareTo(date) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100068"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180518", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
                }

                if (currentBill.getSettlestatus() != null && currentBill.getSettlestatus() == 2) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100069"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180519", "当前单据【%s】已结算，不能进行审批！") /* "当前单据【%s】已结算，不能进行审批！" */, currentBill.getCode()));
                }

                // 判断是否有转换保证金 如果有 需要调用转换保证金保存接口（孙国财提供）
                if (currentBill.getConversionmarginflag() == 1) {
                    try {
                        marginCommonService.generateConversionReceiveMargin(currentBill);
                    } catch (Exception e) {
                        log.error("catch Conversion Receive Margin Exception", e);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100070"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC642205080003", "调用转换保证金接口错误") /* "调用转换保证金接口错误" */ +"=>"+ e.getMessage());
                    }
                }

                // 调用工作台更新接口
                CtmJSONObject params = new CtmJSONObject();
                params.put(ICmpConstant.ACTION, ICmpConstant.AUDIT);
                //保证金原始业务号
                params.put(ICmpConstant.MARGINBUSINESSNO, currentBill.getMarginbusinessno());
                params.put(ICmpConstant.MARGINAMOUNT, currentBill.getMarginamount());
                params.put(ICmpConstant.NATMARGINAMOUNT, currentBill.getNatmarginamount());
                params.put(ICmpConstant.TRADETYPE, currentBill.getTradetype());
                if (ObjectUtils.isNotEmpty(currentBill.getConversionamount())) {
                    params.put(ICmpConstant.CONVERSIONAMOUNT, currentBill.getConversionamount());
                    params.put(ICmpConstant.NATCONVERSIONAMOUNT, currentBill.getNatconversionamount());
                }
                params.put(ICmpConstant.SETTLEFLAG, currentBill.getSettleflag());
                params.put(ICmpConstant.SRC_ITEM, currentBill.getSrcitem());
                //结算状态
                params.put(ICmpConstant.SETTLE_STATUS, currentBill.getSettlestatus());
                params.put(ICmpConstant.RECMARGIN, currentBill);
                marginWorkbenchService.recMarginWorkbenchUpdate(params);

                // 已审核
                bizobject.putAll(currentBill);
//                bizobject.set(ICmpConstant.VERIFY_STATE, VerifyState.COMPLETED.getValue());
                bizobject.set(ICmpConstant.AUDITORID, AppContext.getCurrentUser().getId());
                bizobject.set(ICmpConstant.AUDITOR, AppContext.getCurrentUser().getName());
                bizobject.set(ICmpConstant.AUDIT_DATE, new Date());
                bizobject.set(ICmpConstant.AUDIT_TIME, new Date());
                log.error("receivemargin bill audit, data={}, code={}", CtmJSONObject.toJSONString(data), currentBill.getCode());

            } catch (Exception e) {
                log.error("catch Exception", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100437"),e.getMessage());
            } finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }
        }

        return new RuleExecuteResult();
    }
}
