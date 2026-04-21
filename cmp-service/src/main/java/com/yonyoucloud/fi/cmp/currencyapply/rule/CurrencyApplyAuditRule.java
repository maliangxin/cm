package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICurrencyExchangeNoticeMsgConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description: 外币兑换申请审核前规则 -29
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/24 16:44
 */

@Slf4j
@Component("currencyApplyAuditRule")
public class CurrencyApplyAuditRule extends AbstractCommonRule {

    @Resource
    private CurrencyExchangeService currencyExchangeService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            Date date = BillInfoUtils.getBusinessDate();
            BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizobject.getId(), 3);
            if(currentBill==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizobject.get("code")));
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100100"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            EventSource eventSource =EventSource.find(currentBill.get(IBillConst.SRCITEM));
            if(currentBill.get(IBillConst.SRCITEM) != null && !EventSource.Cmpchase.equals(eventSource)&&!EventSource.ManualImport.equals(eventSource)){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101296"),MessageUtils.getMessage(ICurrencyExchangeNoticeMsgConstant.AUDIT_SCRIME_NOTICE));
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101297"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SCRIME_NOTICE,"该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！“ */);
            }
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (!Objects.isNull(currentDate) && !Objects.isNull(date) && currentDate.compareTo(date) == -1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100415"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BD","审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            bizobject.set("auditstatus", AuditStatus.Complete.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", BillInfoUtils.getBusinessDate());
            bizobject.set("auditTime", new Date());

        }
        return new RuleExecuteResult();
    }
}
