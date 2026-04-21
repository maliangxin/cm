package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   付款单票据生成状态回写规则
 * @author liuttm
 * @version V1.0
 * @date 2021/5/15 14:00
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpPaymentVoucherRule extends FiCmpPaymentBaseRule implements ISagaRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103018"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2586004E00008", "在财务新架构环境下，不允许回写付款单票据生成状态。") /* "在财务新架构环境下，不允许回写付款单票据生成状态。" */);
        }
        if(log.isInfoEnabled()) {
            log.info(" executing  FiCmpPaymentVouderRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        List<BizObject> bills = getBills(billContext, paramMap);
        List oldBills = new ArrayList<PayBill>();
        //  生成凭证回写凭证状态
        String[] ids = new String[1];
        ids[0] = paramMap.get("id").toString();
        List<PayBill>  payBills = getPayBillBySrcbillIds(ids);

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", "cmp_payment");
        params.put("id", payBills.get(0).getId());
        params.put("voucherStatus", paramMap.get("voucherStatus"));
        try {
            SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatus", params);
        } catch (Exception e) {
            log.error("更新凭证状态异常", e);
        }
        paramMap.put("oldBills",payBills);
        return new RuleExecuteResult();
    }

    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("##   #####   FiCmpPaymentVouderRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        // 失败，调用取消审核流程恢复单据状态
        List<PayBill> oldBills =  (List<PayBill>)paramMap.get("oldBills");
        MetaDaoHelper.update(PayBill.ENTITY_NAME,oldBills);
        return new RuleExecuteResult();
    }
}
