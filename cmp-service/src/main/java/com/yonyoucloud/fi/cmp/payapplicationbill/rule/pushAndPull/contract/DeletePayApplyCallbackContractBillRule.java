package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.contract;

import com.google.common.collect.Maps;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h1>保存付款申请单之前的规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-15 16:38
 */
@Slf4j
@Component("deletePayApplyCallbackContractBillRule")
public class DeletePayApplyCallbackContractBillRule extends AbstractCommonRule {

    private final String PAYTOCONTRACT="paytoContract";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

        List<BizObject> bills = getBills(billContext, paramMap);

        String billnum = billContext.getBillnum();
        for (BizObject bizObject : bills) {
            if(!bizObject.get("srcitem").equals(SourceMatters.PurchaseContract.getValue())){
                continue;
            }
            PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME,bizObject.getId());
            List<PayApplicationBill_b> payApplicationBillbes = null;
            if(null != payApplicationBill){
                payApplicationBillbes = payApplicationBill.get("payApplicationBill_b");
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100211"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180104","付款申请表已删除") /* "付款申请表已删除" */);
            }
            if (CollectionUtils.isEmpty(payApplicationBillbes)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100212"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180105","付款申请没有子表数据") /* "付款申请没有子表数据" */);
            }
            Map<String,Object> data=Maps.newHashMap();
            PayApplicationBill_b payApplicationBillb = payApplicationBillbes.get(0);
            PayApplicationBill_b oldPayApplicationBillb = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME,payApplicationBillb.getId());
            data.put("oldaccPayApplyMoney",oldPayApplicationBillb.getPaymentApplyAmount());
            data.put("purstatus",bizObject.getEntityStatus().name());
            data.put("newaccPayApplyMoney", BigDecimal.ZERO);
            Long sourceautoid = payApplicationBillb.getSourceautoid();
            Long sourceid = payApplicationBillb.getSourceid();
            data.put("contractId",payApplicationBillb.getSourceid());
            data.put("prePayId",payApplicationBillb.getSourceautoid());

            Map<String, Object> paraMap = Maps.newHashMap();
            BillDataDto billDataDto = new BillDataDto();
            billDataDto.setData(bizObject);
            paraMap.put("param", billDataDto);
            paraMap.put("data",data);
            BillContext billContextNew = new BillContext();
            billContextNew.setBillnum(billnum);
            billContextNew.setFullname(PayApplicationBill.ENTITY_NAME);
            /*调用远程rule加redis锁防止采购单多次调用*/
            String keyId = AppContext.getTenantId() + String.valueOf(sourceid) + String.valueOf(sourceautoid);
            YmsLock ymsLock = null;
            if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(keyId))==null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100213"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180102","单据：") /* "单据：" */ + String.valueOf(sourceid) + ";" + String.valueOf(sourceautoid) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180103","已被锁定，请稍后重试") /* "已被锁定，请稍后重试" */);
            }
            try {
                log.info("删除 purchaseContract==>调用采购rule");
                RuleExecuteResult ruleResult = BillBiz.executeRule(PAYTOCONTRACT, billContext, paraMap);
                log.info("删除 purchaseContract==>调用采购rule结束");
            } catch (Exception e) {
                throw e;
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }

        return new RuleExecuteResult();
    }
}
