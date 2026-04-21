package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentServiceImpl;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

@Service
@Slf4j
public class PayBillUpSettleStatusAndGenVouchServiceImpl implements UpSettleStatusAndGenVouchService {
    private static final Logger logger = LoggerFactory.getLogger(PayBillUpSettleStatusAndGenVouchServiceImpl.class);
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    PaymentServiceImpl paymentServiceImpl;

    @Autowired
    private JournalService journalService;

    @Override
    @Transactional
    public void updateRecvSettleNew(List<Long> ids) throws Exception{
        if (ids.size() > 0){
            List<PayBill> receives = new ArrayList<>();
            List<PayBill> payBillList = new ArrayList<>();
            List<PayBill> dataList = paymentServiceImpl.queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            // 处理当付款单已结算，已支付后回写付款申请单金额
            List<Journal> journalList = new ArrayList<Journal>();
            for (PayBill payBillBill : dataList) {
//                journalService.updateJournal(payBillBill);
                if(!SettleStatus.alreadySettled.equals(payBillBill.getSettlestatus())){
                    PayBill re = new PayBill();
                    re.setId(payBillBill.getId());
                    re.setPubts(payBillBill.getPubts());
                    re.setPaystatus(PayStatus.OfflinePay);//支付成功
                    re.setSettlestatus(SettleStatus.alreadySettled);
                    re.setEntityStatus(EntityStatus.Update);
                    if(BillInfoUtils.getBusinessDate() != null) {
                        re.setSettledate(new Date());
                        re.setPaydate(new Date());
                        re.setDzdate(BillInfoUtils.getBusinessDate());
                    }else {
                        re.setPaydate(new Date());
                        re.setSettledate(new Date());
                        re.setDzdate(new Date());
                    }
                    receives.add(re);
                    payBillList.add(payBillBill);
                    payBillBill.setSettlestatus(SettleStatus.alreadySettled);
                    journalList.addAll(journalService.updateJournalByBill(payBillBill));
                }
            }
            if(!CollectionUtils.isEmpty(receives)){
                MetaDaoHelper.update(PayBill.ENTITY_NAME, receives);
                updatePayApplyBillAmount(payBillList);
            }
            if(!CollectionUtils.isEmpty(journalList)){
                MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
            }
        }

    }

    private void updatePayApplyBillAmount(List<PayBill> dataList) {
        for (PayBill payBill : dataList) {
            if (EventType.PayApplyBill.getValue() == payBill.getBilltype().getValue()) {
                log.error("update Pay Bill unSettle pay bill back write Pay Apply Bill, tenant_id = {}, payBills = {}",
                        InvocationInfoProxy.getTenantid(), payBill);
                QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name(MAINID).eq(payBill.getId())));
                try {
                    List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                    mapList.forEach(e -> {
                        try {
                            Long id = Long.valueOf(e.get(SRCBILLITEMID).toString());
                            BigDecimal oriSum = (BigDecimal) e.get(ORISUM);
                            log.error("offLinePay pay bill back write Pay Apply Bill, tenant_id = {}, payBillId = {}, payBillBId = {}, src_bill_item_id = {}",
                                    InvocationInfoProxy.getTenantid(), payBill.getId(), e.get("id"), e.get(SRCBILLITEMID));
                            // 更新子表已付金额和未付金额
                            PayApplicationBill_b payApplicationBillB = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, id);
                            log.info("offLinePay pay bill back write Pay Apply Bill, tenant_id = {}, payApplicationBillB = {}",
                                    InvocationInfoProxy.getTenantid(), payApplicationBillB);
                            if (null != payApplicationBillB) {
                                if (null == payApplicationBillB.getPaidAmount()) {
                                    payApplicationBillB.setPaidAmount(oriSum);
                                } else {
                                    payApplicationBillB.setPaidAmount(BigDecimalUtils.safeAdd(payApplicationBillB.getPaidAmount(),oriSum));
                                }
                                payApplicationBillB.setUnpaidAmount(BigDecimalUtils.safeSubtract(payApplicationBillB.getUnpaidAmount(),oriSum));
                                EntityTool.setUpdateStatus(payApplicationBillB);
                                MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBillB);
                                // 更新主表已付金额总数和未付金额总数
                                Long mainid = payApplicationBillB.getMainid();
                                PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainid);
                                if (null == payApplicationBill.getPaidAmountSum()) {
                                    payApplicationBill.setPaidAmountSum(oriSum);
                                } else {
                                    payApplicationBill.setPaidAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getPaidAmountSum(), oriSum));
                                }
                                payApplicationBill.setUnpaidAmountSum(BigDecimalUtils.safeSubtract(payApplicationBill.getUnpaidAmountSum(), oriSum));
                                EntityTool.setUpdateStatus(payApplicationBill);
                                if (payApplicationBill.getPaidAmountSum().equals(payApplicationBill.getPaymentApplyAmountSum())) {
                                    payApplicationBill.setPayBillStatus(PayBillStatus.PaymentCompleted);
                                } else {
                                    payApplicationBill.setPayBillStatus(PayBillStatus.PartialPayment);
                                }
                                MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill);
                            }
                        } catch (Exception exception) {
                           log.error("调整未付金额和已付金额数据失败!:" + exception.getMessage());
                        }
                    });
                } catch (Exception e)   {
                    log.error("查询付款申请单卡片失败:" + e.getMessage());
                }
            }

        }
    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public Map<String,Object> generateRecvVoucherNew(List<Long> ids) throws Exception{
        Map<String,Object> resMap = new HashMap<>();
        int i = 0;
        if (ids.size() > 0){
            List<PayBill> list =  paymentServiceImpl.queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (PayBill payBillBill : list){
                payBillBill.set("_entityName", PayBill.ENTITY_NAME);
                if(BillInfoUtils.getBusinessDate() != null) {
                    payBillBill.setSettledate(new Date());
                    payBillBill.setPaydate(new Date());
                    payBillBill.setDzdate(BillInfoUtils.getBusinessDate());
                }else {
                    payBillBill.setPaydate(new Date());
                    payBillBill.setSettledate(new Date());
                    payBillBill.setDzdate(new Date());
                }
                CtmJSONObject jsonObject = cmpVoucherService.generateVoucherWithResult(payBillBill);
                if(jsonObject!= null && !(Boolean)jsonObject.get("dealSucceed")){
                    log.error("付款单生成凭证失败！", jsonObject.get("message"));
                    i++;
                    continue;
                }
            }
        }
        resMap.put("failCount",i);
        return resMap;
    }
}
