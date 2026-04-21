package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmcmpReWriteBusRpcServiceImpl;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.lang.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务单据操作后，需要删除对账单关联关系规则 （资金收付款单删除时调用）
 */
@Component
@Slf4j
public class DelCorrRule extends AbstractCommonRule {

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Autowired
    CtmcmpReWriteBusRpcServiceImpl ctmcmpReWriteBusRpcService;

    @Override
    @Transactional
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        String billNum = billContext.getBillnum();
        BizObject bizObject = bills.get(0);
        Long busid = Long.valueOf(bizObject.getId().toString());//业务单据id

        List<CtmJSONObject> listreq = new ArrayList();
        //处理需要关联数据
        if ("cmp_fundpayment".equals(billNum) || "cmp_fundpaymentlist".equals(billNum)) {//资金付款单
            QuerySchema querySchema = QuerySchema.create().addSelect("id,billClaimId,bankReconciliationId,associationStatus,pubts");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(busid.toString()));
            querySchema.addCondition(group);
            List<FundPayment_b> fundPaymentBList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(fundPaymentBList)) {
                List delDataList = new ArrayList();
                for (FundPayment_b billb : fundPaymentBList) {
                    CommonRequestDataVo commonRequestDataVo = new CommonRequestDataVo();
                    commonRequestDataVo.setBillnum("DelCorrRule");
                    if (StringUtils.isNotEmpty(billb.getBankReconciliationId())) {
                        if(delDataList.contains(billb.getBankReconciliationId())){
                            continue;
                        }
                        delDataList.add(billb.getBankReconciliationId());
                        commonRequestDataVo.setBusid(billb.getBankReconciliationId().toString());
                        commonRequestDataVo.setStwbbusid(Long.valueOf(billb.getId().toString()));
                        if(bizObject.get("entrytype") != null){
                            Map<String,Object> queryDataForMap = new HashMap<>();
                            queryDataForMap.put("entrytype",bizObject.get("entrytype"));
                            commonRequestDataVo.setQueryDataForMap(queryDataForMap);
                        }
                        ctmcmpReWriteBusRpcService.resDelDataForRpc(commonRequestDataVo);
                    } else if (StringUtils.isNotEmpty(billb.getBillClaimId())) {
                        if(delDataList.contains(billb.getBillClaimId())){
                            continue;
                        }
                        delDataList.add(billb.getBillClaimId());
                        commonRequestDataVo.setClaimId(Long.parseLong(billb.getBillClaimId()));
                        commonRequestDataVo.setStwbbusid(Long.parseLong(billb.getId().toString()));
                        ctmcmpReWriteBusRpcService.resDelDataForRpc(commonRequestDataVo);
                    }
                }
            }
        } else if ("cm_transfer_account".equals(billNum) || "cm_transfer_account_list".equals(billNum)) { //转账单
            List<TransferAccount> transferAccountList;
            // id paybillclaim collectbillclaim paybankbill collectbankbill associationStatusPay associationStatusCollect
            QuerySchema querySchema = QuerySchema.create().addSelect("id, paybillclaim, collectbillclaim, paybankbill, collectbankbill, associationStatusPay, associationStatusCollect, pubts");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
            querySchema.addCondition(group);
            transferAccountList = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
            log.error("删除同名账户划转与银行流水的关联关系:{}", CtmJSONObject.toJSONString(transferAccountList));
            if (CollectionUtils.isNotEmpty(transferAccountList)) {
                for (TransferAccount transferAccount : transferAccountList) {
                    //判断 付款已关联
                    if (transferAccount.getAssociationStatusPay() != null && transferAccount.getAssociationStatusPay().equals(true)) {
                        CommonRequestDataVo commonRequestDataVo = new CommonRequestDataVo();
                        if (ObjectUtils.isNotEmpty(transferAccount.getPaybankbill())) {
                            commonRequestDataVo.setBusid(transferAccount.getPaybankbill().toString());
                        }
                        if (ObjectUtils.isNotEmpty(transferAccount.getPaybillclaim())) {
                            commonRequestDataVo.setClaimId(transferAccount.getPaybillclaim());
                        }
                        commonRequestDataVo.setStwbbusid(transferAccount.getId());
                        ctmcmpReWriteBusRpcService.resDelDataForRpc(commonRequestDataVo);
                    }
                    //判断 收款已关联
                    if (transferAccount.getAssociationStatusCollect() != null && transferAccount.getAssociationStatusCollect().equals(true)) {
                        CommonRequestDataVo commonRequestDataVo = new CommonRequestDataVo();
                        if (ObjectUtils.isNotEmpty(transferAccount.getCollectbankbill())) {
                            commonRequestDataVo.setBusid(transferAccount.getCollectbankbill().toString());
                        }
                        if (ObjectUtils.isNotEmpty(transferAccount.getCollectbillclaim())) {
                            commonRequestDataVo.setClaimId(transferAccount.getCollectbillclaim());
                        }
                        commonRequestDataVo.setStwbbusid(transferAccount.getId());
                        ctmcmpReWriteBusRpcService.resDelDataForRpc(commonRequestDataVo);
                    }
                }

            }
        } else if ("cmp_payment".equals(billNum) || "cmp_paymentlist".equals(billNum)) { //付款单
            PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, busid);
            if (payBill != null && payBill.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == payBill.getAssociationStatus()) {
                CtmJSONObject jsonReq = new CtmJSONObject();
                if (ObjectUtils.isNotEmpty(payBill.getBankReconciliationId())) {
                    jsonReq.put("busid", payBill.getBankReconciliationId());
                }
                if (ObjectUtils.isNotEmpty(payBill.getBillClaimId())) {
                    jsonReq.put("claimid", payBill.getBillClaimId());
                }
                jsonReq.put("stwbbusid", payBill.getId());
                listreq.add(jsonReq);
                if (CollectionUtils.isNotEmpty(listreq)) {
                    for (CtmJSONObject req : listreq) {
                        reWriteBusCorrDataService.resDelData(req);
                    }
                }
            }
        } else if ("cmp_receivebill".equals(billNum) || "cmp_receivebilllist".equals(billNum)) { //收款单
            ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, busid);
            if (receiveBill != null && receiveBill.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == receiveBill.getAssociationStatus()) {
                CtmJSONObject jsonReq = new CtmJSONObject();
                if (ObjectUtils.isNotEmpty(receiveBill.getBankReconciliationId())) {
                    jsonReq.put("busid", receiveBill.getBankReconciliationId());
                }
                if (ObjectUtils.isNotEmpty(receiveBill.getBillClaimId())) {
                    jsonReq.put("claimid", receiveBill.getBillClaimId());
                }
                jsonReq.put("stwbbusid", receiveBill.getId());
                listreq.add(jsonReq);
                if (CollectionUtils.isNotEmpty(listreq)) {
                    for (CtmJSONObject req : listreq) {
                        reWriteBusCorrDataService.resDelData(req);
                    }
                }
            }
        } else {//资金收款单
            QuerySchema querySchema = QuerySchema.create().addSelect("id,billClaimId,bankReconciliationId,associationStatus,pubts");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(busid.toString()));
            querySchema.addCondition(group);
            List<FundCollection_b> fundCollectionBList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(fundCollectionBList)) {
                List delDataList = new ArrayList();
                List<CommonRequestDataVo> commonRequestDataVos4Bankreconciliation = new ArrayList<>();
                List<CommonRequestDataVo> commonRequestDataVos4Billclaim = new ArrayList<>();
                for (FundCollection_b billb : fundCollectionBList) {
                    CommonRequestDataVo commonRequestDataVo = new CommonRequestDataVo();
                    commonRequestDataVo.setBillnum("DelCorrRule");
                    if (StringUtils.isNotEmpty(billb.getBankReconciliationId())) {
                        if(delDataList.contains(billb.getBankReconciliationId())){
                            continue;
                        }
                        delDataList.add(billb.getBankReconciliationId());
                        commonRequestDataVo.setBusid(billb.getBankReconciliationId().toString());
                        commonRequestDataVo.setStwbbusid(Long.valueOf(billb.getId().toString()));
                        if(bizObject.get("entrytype") != null){
                            Map<String,Object> queryDataForMap = new HashMap<>();
                            queryDataForMap.put("entrytype",bizObject.get("entrytype"));
                            commonRequestDataVo.setQueryDataForMap(queryDataForMap);
                        }
                        commonRequestDataVos4Bankreconciliation.add(commonRequestDataVo);
                    } else if (StringUtils.isNotEmpty(billb.getBillClaimId())) {
                        if(delDataList.contains(billb.getBillClaimId())){
                            continue;
                        }
                        delDataList.add(billb.getBillClaimId());
                        commonRequestDataVo.setClaimId(Long.parseLong(billb.getBillClaimId()));
                        commonRequestDataVo.setStwbbusid(Long.valueOf(billb.getId().toString()));
                        commonRequestDataVos4Billclaim.add(commonRequestDataVo);
                    }
                }
                // 流水生单的改成批量接口删除
                if(CollectionUtils.isNotEmpty(commonRequestDataVos4Bankreconciliation)){
                    ctmcmpReWriteBusRpcService.batchResDelDataForBankReconciliation(commonRequestDataVos4Bankreconciliation);
                }
                // 认领单生单的改成批量接口删除
                if(CollectionUtils.isNotEmpty(commonRequestDataVos4Billclaim)){
                    ctmcmpReWriteBusRpcService.batchResDelDataForBillclaim(commonRequestDataVos4Billclaim);
                }
            }
        }
        //如果是业务处理生单 -- 调用Service层进行数据处理，以及关联--回写对账单
        return new RuleExecuteResult();
    }

}
