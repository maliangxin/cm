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
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

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
@Component("payApplyCallbackContractBillRule")
public class PayApplyCallbackContractBillRule extends AbstractCommonRule {

    private final String paytocontract="paytoContract";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100676"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D5","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }

        String billnum = billContext.getBillnum();
        for (BizObject bizObject : bills) {
            if(!bizObject.get("srcitem").equals(SourceMatters.PurchaseContract.getValue())){
                continue;
            }

            List<PayApplicationBill_b> payApplicationBillbes = bizObject.get("payApplicationBill_b");
            if (CollectionUtils.isEmpty(payApplicationBillbes)) {
                if ("Insert".equals(bizObject.getEntityStatus().name())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100677"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D2","付款申请没有子表数据") /* "付款申请没有子表数据" */);
                } else {
                    continue;
                }
            }
            Map<String,Object> data=Maps.newHashMap();
            PayApplicationBill_b payApplicationBillb = payApplicationBillbes.get(0);
            if(bizObject.getEntityStatus().name().equals("Update")){
                PayApplicationBill_b oldPayApplicationBillb = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME,payApplicationBillb.getId());
                data.put("oldaccPayApplyMoney",oldPayApplicationBillb.getPaymentApplyAmount());
                payApplicationBillb.setSourceid(oldPayApplicationBillb.getSourceid());
                payApplicationBillb.setSourceautoid(oldPayApplicationBillb.getSourceautoid());
            }else{
                data.put("ts",bizObject.get("pubts"));
            }
            data.put("purstatus",bizObject.getEntityStatus().name());
            data.put("newaccPayApplyMoney",payApplicationBillb.getPaymentApplyAmount());
            Long sourceautoid = payApplicationBillb.getSourceautoid();
            Long sourceid = payApplicationBillb.getSourceid();
            data.put("contractId",sourceid);
            data.put("prePayId",sourceautoid);


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
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(keyId);
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100678"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D3","单据：") /* "单据：" */ + String.valueOf(sourceid) + ";" + String.valueOf(sourceautoid) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D4","已被锁定，请稍后重试") /* "已被锁定，请稍后重试" */);
            }
            try {
                log.info("保存/更新 purchaseContract==>调用采购rule");
                RuleExecuteResult ruleResult = BillBiz.executeRule(paytocontract, billContext, paraMap);
                log.info("保存/更新 purchaseContract==>调用采购rule结束");
            } catch (Exception e) {
                throw e;
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
            bizObject.put("pubts", null);
        }

        return new RuleExecuteResult();
    }
}
