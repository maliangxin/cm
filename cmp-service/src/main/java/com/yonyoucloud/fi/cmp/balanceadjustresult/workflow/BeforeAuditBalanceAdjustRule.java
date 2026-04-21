package com.yonyoucloud.fi.cmp.balanceadjustresult.workflow;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceAuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ICurrencyExchangeNoticeMsgConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description: 余额调节表审批前规则
 * @author: zhoulyu@yonyou.com
 * @date: 2025/03/18 15:49
 */

@Slf4j
@Component("beforeAuditBalanceAdjustRule")
public class BeforeAuditBalanceAdjustRule extends AbstractCommonRule {


    @Autowired
    BalanceAdjustResultSerevice balanceAdjustResultSerevice;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CurrencyQueryService currencyQueryService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            Date date = BillInfoUtils.getBusinessDate();
            BizObject currentBill = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bizobject.getId(), 3);
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizobject.get("code")));
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100100"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BE", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }

            //校验最早的数据之前是否存在未审批数据
            BalanceAdjustResult unaudit = balanceAdjustResultSerevice.getEarlyUnauditData((BalanceAdjustResult) currentBill);
            if (unaudit != null) {
                EnterpriseBankAcctVO enterpriseBankAcctVO= enterpriseBankQueryService.findById(currentBill.get(ICmpConstant.BANKACCOUNT));
                CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currentBill.get(ICmpConstant.CURRENCY));
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */,
                        enterpriseBankAcctVO.getAccount(), currencyTenantDTO.getName(), unaudit.getDzdate()));
            }

            EventSource eventSource = EventSource.find(currentBill.get(IBillConst.SRCITEM));
            if (currentBill.get(IBillConst.SRCITEM) != null && !EventSource.Cmpchase.equals(eventSource) && !EventSource.ManualImport.equals(eventSource)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100743"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SCRIME_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400714", "该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */));
            }
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) == -1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100415"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807BD", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            bizobject.set("auditstatus", BalanceAuditStatus.Complete.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", BillInfoUtils.getBusinessDate());
            bizobject.set("auditTime", new Date());
        }
        return new RuleExecuteResult();
    }
}
