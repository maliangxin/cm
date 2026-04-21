package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.constant.ICurrencyExchangeNoticeMsgConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description: 外币兑换申请弃审规则
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/24 16:54
 */

@Slf4j
@Component("currencyApplyUnAuditRule")
public class CurrencyApplyUnAuditRule extends AbstractCommonRule {

    @Resource
    private CurrencyExchangeService currencyExchangeService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100061"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CA","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
//            Short auditStatus = Short.parseShort(currentBill.get("auditstatus").toString());
//            if (auditStatus != null && auditStatus.equals(AuditStatus.Incomplete.getValue())) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100063"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003B","单据[%s]未审批不能撤回") /* "单据[%s]未审批不能撤回" */,currentBill.get(ICmpConstant.CODE).toString()));
//            }

            if(currentBill.get("deliverystatus") != null &&  (currentBill.get("deliverystatus").equals(2)
                    || currentBill.get("deliverystatus").equals(3) || currentBill.get("deliverystatus").equals(4) ||currentBill.get("deliverystatus").equals(5) )){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100064"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_SETTLED_NOTICE));
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100064"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_SETTLED_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400786", "该单据已结算，不能进行取消审批！") /* "该单据已结算，不能进行取消审批！" */));
            }

            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);

            //撤回后删除对应的外币兑换单
            currencyExchangeService.deleteCurrencyApply(currentBill.getId());
        }
        return new RuleExecuteResult();
    }
}
