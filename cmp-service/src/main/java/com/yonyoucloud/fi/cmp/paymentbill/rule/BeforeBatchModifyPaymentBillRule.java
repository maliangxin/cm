package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>付款工作台批改前校验规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-08-25 12:26
 */
@Slf4j
@Component
public class BeforeBatchModifyPaymentBillRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103014"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C255F404A00006", "在财务新架构环境下，不允许保存付款单。") /* "在财务新架构环境下，不允许保存付款单。" */);
        }
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            Object settleMode = bizObject.get(SETTLE_MODE);
            Object settleModeName = bizObject.get(SETTLE_MODE_NAME);
            Object enterpriseBankAccount = bizObject.get(ENTERPRISE_BANK_ACCOUNT_LOWER);
            Object enterpriseBankAccountName = bizObject.get(ENTERPRISE_BANK_ACCOUNT_LOWER_NAME);
            Object cashAccount = bizObject.get(CASH_ACCOUNT_LOWER);
            Object cashAccountName = bizObject.get(CASH_ACCOUNT_LOWER_NAME);
            log.error("Payment WorkBench Grid Batch Modify Input Parameter, tenantId = {}, id = {}, code = {}, " +
                            "settleMode = {}, settleModeName = {}, enterpriseBankAccount = {}, enterpriseBankAccountName = {}, " +
                            "cashAccount = {}, cashAccountName = {}, remark = {}",
                    InvocationInfoProxy.getTenantid(), bizObject.getId(), bizObject.get(CODE),
                    settleMode, settleModeName, enterpriseBankAccount, enterpriseBankAccountName, cashAccount, cashAccountName, bizObject.get(REMARK));
            BizObject currentBill = MetaDaoHelper.findById(bizObject.getEntityName(), bizObject.getId());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100749"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A1","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            Date currentPubts = bizObject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100750"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049C","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            short payStatus = Short.parseShort(currentBill.get(PAY_STATUS).toString());
            boolean auditStatusFlag = Short.parseShort(bizObject.get(AUDIT_STATUS).toString()) != (AuditStatus.Complete.getValue());
            boolean payStatusFlag = (payStatus != PayStatus.NoPay.getValue() && payStatus != PayStatus.Fail.getValue());
            if (auditStatusFlag || payStatusFlag) {
                String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049F","单据[%s]状态有误，只能对已审核且未支付的单据进行操作") /* "单据[%s]状态有误，只能对已审核且未支付的单据进行操作" */, bizObject.get(CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100751"),message);
            }
            //结算方式
            Map<String, Object> querySettlementWayCon = new HashMap<>(CONSTANT_EIGHT);
            querySettlementWayCon.put(ID, settleMode);
            querySettlementWayCon.put(IS_ENABLED, CONSTANT_ONE);
            querySettlementWayCon.put(TENANT, AppContext.getTenantId());
            List<Map<String, Object>> dataList = QueryBaseDocUtils.querySettlementWayByCondition(querySettlementWayCon);
            Object settleModeOrigin = currentBill.get(SETTLE_MODE);
            Object enterpriseBankAccountOrigin = currentBill.get(ENTERPRISE_BANK_ACCOUNT_LOWER);
            Object cashAccountOrigin = currentBill.get(CASH_ACCOUNT_LOWER);
            if (!Objects.equals(settleMode, settleModeOrigin) || !Objects.equals(enterpriseBankAccount, enterpriseBankAccountOrigin) || !Objects.equals(cashAccount, cashAccountOrigin)) {
                if(dataList != null && dataList.size() > CONSTANT_ZERO){
                    // 银行转账
                    if(dataList.get(CONSTANT_ZERO).get(SERVICE_ATTR).equals(CONSTANT_ZERO)){
                        if (Objects.equals(settleMode, settleModeOrigin)) {
                            if (ValueUtils.isNotEmptyObj(cashAccount)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049D","结算方式业务属性为银行业务，请录入银行账户！") /* "结算方式业务属性为银行业务，请录入银行账户！" */);
                            }
                            if (Objects.equals(enterpriseBankAccount, enterpriseBankAccountOrigin)){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100753"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A3","批改时，银行账户修改前后不能为同一个，请重新操作！") /* "批改时，银行账户修改前后不能为同一个，请重新操作！" */);
                            }
                        } else {
                            if (enterpriseBankAccount == null) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049D","结算方式业务属性为银行业务，请录入银行账户！") /* "结算方式业务属性为银行业务，请录入银行账户！" */);
                            }
                            bizObject.set(CASH_ACCOUNT_LOWER, null);
                        }
                    // 现金业务
                    } else if (dataList.get(CONSTANT_ZERO).get(SERVICE_ATTR).equals(CONSTANT_ONE)) {
                        if (Objects.equals(settleMode, settleModeOrigin)) {
                            if (ValueUtils.isNotEmptyObj(enterpriseBankAccount)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100754"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A0","结算方式业务属性为现金业务，请录入现金账户！") /* "结算方式业务属性为现金业务，请录入现金账户！" */);
                            }
                            if (Objects.equals(cashAccount, cashAccountOrigin)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100755"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A2","批改时，现金账户修改前后不能为同一个，请重新操作！") /* "批改时，现金账户修改前后不能为同一个，请重新操作！" */);
                            }
                        } else {
                            if (cashAccount == null) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100754"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804A0","结算方式业务属性为现金业务，请录入现金账户！") /* "结算方式业务属性为现金业务，请录入现金账户！" */);
                            }
                            bizObject.set(ENTERPRISE_BANK_ACCOUNT_LOWER, null);
                        }
                    } else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100756"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049E","结算方式业务属性只能为银行业务与现金业务！") /* "结算方式业务属性只能为银行业务与现金业务！" */);
                    }
                }
            }
            bizObject.set(SIGNATURE_STR, ICmpConstant.AUTOTEST_SIGNATURE_Constant);
            bizObject.set(BATCH_MODIFY_FLAG, BATCH_MODIFY);
        }
        return new RuleExecuteResult();
    }
}
