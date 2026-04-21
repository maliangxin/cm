package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountService;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

//feat(CZFW-313978):银行流水认领-生单-同名账户划转:审核通过-自动线下支付成功
@Component
@Slf4j
public class TransferAutoPayAuditAfterRule extends AbstractCommonRule {

    @Autowired
    private AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bizObjects = getBills(billContext, paramMap);
        BizObject contextData = bizObjects.get(0);
        AssertUtil.isNotNull(contextData, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540078D", "获取审批上下文对象失败....") /* "获取审批上下文对象失败...." */));
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, contextData.getId());
        //如果是已结算补单，则不更新结算状态，银行流水认领生单同名账户划转时，结算状态是：已结算补单-自动线下支付
        //7.16修改 需要判断是否传结算  不传结算的 修改支付状态为 已支付补单
        //判断现金参数是否为是
        Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
        if (transferAccount.getSettlestatus() != null && transferAccount.getSettlestatus().getValue() == SettleStatus.SettledRep.getValue() && !pushSettlement) {
            transferAccount.setPaystatus(PayStatus.SupplPaid);
            transferAccount.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
        }
        return new RuleExecuteResult();
    }
}
