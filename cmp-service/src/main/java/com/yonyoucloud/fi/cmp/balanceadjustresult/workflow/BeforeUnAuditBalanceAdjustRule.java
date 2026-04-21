package com.yonyoucloud.fi.cmp.balanceadjustresult.workflow;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceAuditStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description: 余额调节表弃审后规则
 * @author: zhoulyu@yonyou.com
 * @date: 2025/03/18 15:49
 */

@Slf4j
@Component("beforeUnAuditBalanceAdjustRule")
public class BeforeUnAuditBalanceAdjustRule extends AbstractCommonRule {

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
            BizObject bizObject = bills.get(0);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject currentBill = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bizObject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CA", "单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            Date currentPubts = bizObject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            //取消审批时校验同一方案、同一组织（对账组织）、银行账号、币种，该余额调节表截止日期之后是否存在“审批”的数据
            BalanceAdjustResult audit = balanceAdjustResultSerevice.getAfterAuditData((BalanceAdjustResult) currentBill);
            if (audit != null) {
                EnterpriseBankAcctVO enterpriseBankAcctVO= enterpriseBankQueryService.findById(currentBill.get(ICmpConstant.BANKACCOUNT));
                CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currentBill.get(ICmpConstant.CURRENCY));
                throw new CtmException(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080012", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400673", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */,
                        enterpriseBankAcctVO.getAccount(), currencyTenantDTO.getName(), audit.getDzdate()));
            }
            if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
                if (Objects.isNull(currentBill.get("auditor"))) {
                    bizObject.set("auditstatus", BalanceAuditStatus.Incomplete.getValue());
                    bizObject.set("auditorId", null);
                    bizObject.set("auditor", null);
                    bizObject.set("auditDate", null);
                    bizObject.set("auditTime", null);
                    bizObject.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, bizObject);
                }
            } else {
                bizObject.set("auditstatus", BalanceAuditStatus.SUBMITED.getValue());
            }
        }
        return new RuleExecuteResult();
    }
}
