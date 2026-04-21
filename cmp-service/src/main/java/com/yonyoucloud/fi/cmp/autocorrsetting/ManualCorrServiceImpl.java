package com.yonyoucloud.fi.cmp.autocorrsetting;


import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.reqvo.SettleDeatailRelBankBillReqVO;
import com.yonyoucloud.ctm.stwb.respvo.ResultStrRespVO;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.BillClaimType;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationSupportWayEnum;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.tlm.openapi.BillCheckRelateTLMRPC;
import com.yonyoucloud.fi.tlm.request.CommonInfoReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 手动关联操作
 * 现金管理 -->资金结算
 * 1，回写资金结算单关联关系
 *
 * @author msc
 */


@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class ManualCorrServiceImpl {


    @Autowired
    CorrOperationService corrOperationService;//写入关联关系

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    private static final String fundsettlement = "cmp_bankreconciliationlist_fundsettlement";
    //融资还本单
    private static final String tlm_repayment = "cmp_bankreconciliationlist_tlm_repayment";
    //融资付息单
    private static final String tlm_payinterest = "cmp_bankreconciliationlist_tlm_payinterest";
    //融资付费单
    private static final String tlm_financepayfee = "cmp_bankreconciliationlist_tlm_financepayfee";
    //融入登记单
    private static final String tlm_financeinStatement = "cmp_bankreconciliationlist_tlm_financeinStatement";

    //投资收息单
    private static final String tlm_interestcollection = "cmp_bankreconciliationlist_tlm_interestcollection";
    //投资管理-申购登记单
    private static final String tlm_purchaseregister = "cmp_bankreconciliationlist_tlm_purchasepay";
    //投资赎回单
    private static final String tlm_investredem = "cmp_bankreconciliationlist_tlm_investredem";
    //投资付费单
    private static final String tlm_investpayment = "cmp_bankreconciliationlist_tlm_investpayment";
    //投资分红单
    private static final String tlm_investprofitsharing = "cmp_bankreconciliationlist_tlm_investprofitsharing";
    //衍生品交易交割单
    private static final String tlm_tradedelivery = "cmp_bankreconciliationlist_tlm_tradedelivery";
    //衍生品交易平仓单
    private static final String tlm_derivativesclose = "cmp_bankreconciliationlist_tlm_derivativesclose";
    //衍生品交易展期登记单
    private static final String tlm_traderolloverregister = "cmp_bankreconciliationlist_tlm_traderolloverregister";
    //衍生品交易保证金登记单
    private static final String tlm_addbond = "cmp_bankreconciliationlist_tlm_addbond";

    private static final String refundstatusFalseStr = "0";
    private static final String refundstatusTrueStr = "1";

    /**
     * 手动关联 - 回写业务单据
     *
     * @param corrDataEntities
     * @param
     * @return
     */
    public void manualCorrBill(List<CorrDataEntity> corrDataEntities, Map<String, Object> paramMap) throws Exception {

        Long bid = Long.valueOf(paramMap.get("bid").toString());
        //子表id,兼容融入登记单子表id可能为空的情况
        Long busid = paramMap.get("busid") == null ? null : Long.valueOf(paramMap.get("busid").toString());
        //借贷方向
        Short dcFlag = paramMap.get("dcFlag") == null ? null : Short.valueOf(paramMap.get("dcFlag").toString());
        //流水号
        String bank_seq_no = paramMap.get("bank_seq_no") == null ? null : paramMap.get("bank_seq_no").toString();
        String isClaim = (String) paramMap.get("isClaim");
        String billType = (String) paramMap.get("billType");
        String pubts = paramMap.get("pubts") + "";
        //CZFW-507469 处理部分认领场景下，关联单据唯一性索引赋值
        boolean isPartClaim = false;
        String claimId = null;
        if (!"Claim".equals(isClaim)) {
            List<BankReconciliation> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            List<BizObject> bizObjects = new ArrayList<>(list);
            BankreconciliationUtils.checkDataLegalList(bizObjects, BankreconciliationActionEnum.BUSSASSOCIATION);
            BankReconciliation bankReconciliation1 = list.get(0);
            Date date = bankReconciliation1.getPubts();
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String pus = sf.format(date);
            if (!pubts.equals(pus)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102052"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024C", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
        } else {
            List<BillClaim> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
            if (list != null && list.size() > 0) {
                BillClaim billClaim = list.get(0);
                isPartClaim = billClaim.getClaimtype() == BillClaimType.Part.getValue();
                claimId = billClaim.getId().toString();
                Date date = billClaim.getPubts();
                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String pus = sf.format(date);
                if (!pubts.equals(pus)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102052"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024C", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
                List<BillClaimItem> billClaimItems = billClaim.items();
                if (billClaimItems != null && billClaimItems.size() > 1) {
                    for (int i = 1; i < billClaimItems.size(); i++) {
                        if (!billClaimItems.get(i).getTo_acct_no().equals(billClaimItems.get(i - 1).getTo_acct_no())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102053"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1853354C05380007", "对账认领单中的对方账号不一致，不允许进行业务关联操作，请检查！") /* "对账认领单中的对方账号不一致，不允许进行业务关联操作，请检查！" */);
                        }
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102052"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024C", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
        }

        if (corrDataEntities == null || corrDataEntities.size() < 1) {
            return;
        }
        //202408财资统一对账码修改，需要先进行资金结算明细的关联确认，有返回财资统一对账码则用资金结算的财资统一对账码 赋值到smartcheckno
        // CZFW-128424 转账单只需要回写一次
        if ("3".equals(corrDataEntities.get(0).getBillType())) { //转账单回写
            //调用回写接口
            reWriteBusCorrDataService.reWriteTransferAccountData(corrDataEntities.get(0));
        }
        if ("Claim".equals(isClaim)) {//认领单
            Short associationStatus = 1;
            //财资统一对账码修改，认领单回写要放在对账单之后，为了获取结算返回的财资统一对账码
            //step1:处理关联的对账单
            if (fundsettlement.equals(billType)) {
                writeBackStwb(true, busid, bid, corrDataEntities);
            }
            //step2:处理认领单
            reWriteBusCorrDataService.reWriteBillClaimData(bid,associationStatus,
                    corrDataEntities.get(0).getSmartcheckno(), ClaimCompleteType.RecePayAssociated.getValue());
            //如果包含投融资，则回写投融资，后续流程需要考虑
            if (billType.contains("tlm")) {
                //回写投融资
                writeBackTlm(true, billType, busid, bid, dcFlag, bank_seq_no, corrDataEntities, paramMap);
            }
        } else {//银行对账单
            if (fundsettlement.equals(billType)) {
                writeBackStwb(false, busid, bid, corrDataEntities);
            }
            //如果包含投融资，则回写投融资，后续流程需要考虑
            if (billType.contains("tlm")) {
                //回写投融资
                writeBackTlm(false, billType, busid, bid, dcFlag, bank_seq_no, corrDataEntities, paramMap);
            }
        }
        int ordernum = 1;
        for (CorrDataEntity corrEntity : corrDataEntities) {
            //CZFW-507469 处理部分认领场景下，关联单据唯一性索引赋值
            if (isPartClaim){
                HashMap<String, Object> extendParam = new HashMap<>();
                extendParam.put("isPartClaim",true);
                extendParam.put("claimId",claimId);
                corrEntity.setExtendFields(extendParam);
            }
            //处理关联数据信息，将关联单据对应关系翻译
            corrOperationService.corrOpration(corrEntity,ordernum);
            ordernum++;
            //处理银行对账单关联关系
            reWriteBusCorrDataService.reWriteBankReconciliationData(corrEntity, false);
            if ("1".equals(corrEntity.getBillType())) {
                reWriteBusCorrDataService.reWritePayMentData(corrEntity);
            } else if ("0".equals(corrEntity.getBillType())) {
                reWriteBusCorrDataService.reWriteFundCollectionData(corrEntity);
            } else if ("4".equals(corrEntity.getBillType())) {
                //外币兑换单回写
                reWriteBusCorrDataService.reWriteCurrencyexchangeData(corrEntity);
            } else if ("5".equals(corrEntity.getBillType())) { //付款单回写
                reWriteBusCorrDataService.reWritePayBillData(corrEntity);
            } else if ("6".equals(corrEntity.getBillType())) { //收款单回写
                reWriteBusCorrDataService.reWriteReceiveBillData(corrEntity);
            }
        }

    }

    /**
     * 回写资金结算
     *
     * @param isClaim
     * @param busid
     * @param bid
     * @param corrDataEntities
     * @throws Exception
     */
    void writeBackStwb(boolean isClaim, Long busid, Long bid, List<CorrDataEntity> corrDataEntities) throws Exception {
        if (isClaim) {
            SettleDeatailRelBankBillReqVO json = new SettleDeatailRelBankBillReqVO();
            json.setClaim_id(String.valueOf(bid));
            json.setSettleBenchB_id(String.valueOf(busid));
            //智能对账：新增勾兑号传递
            json.setCheck_identification_code(corrDataEntities.get(0).getSmartcheckno());
            //认领单，非退票.必传，不然结算会报错
            //json.setIsrefund(String.valueOf(0));
            List<BankReconciliation> bankReconciliations = getBankReconciliationsByClaimId(bid);
            //认领单，必传，不然结算会报错
            json.setIsrefund(getRefundByBankReconciliations(bankReconciliations));
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bid);
            //关联接口传递认领银行对账单最大交易日期
            Date maxTransTime = getmaxTransTimeByBankReconciliations(bankReconciliations);
            json.setSettlesuccessdate(DateUtils.dateFormat(maxTransTime, "yyyy-MM-dd"));
            json.setSettlesuccesstime(DateUtils.dateFormat(maxTransTime, DateUtils.DATE_TIME_PATTERN));
            if (refundstatusTrueStr.equals(json.getIsrefund())) {
                json.setRefundAmt(billClaim.getTotalamount());
                json.setRefundDate(maxTransTime);//退票时间
            } else {
                json.setRefundAmt(new BigDecimal(String.valueOf(0)));
            }
            //关联确认接口增加金额传递
            json.setActualExchangePaymentAmount(billClaim.getTotalamount());
            //202408 资金结算返回财资统一对账码
            log.error("SettleBenchBRPCService newRelationSettleBench请求参数 = {}",json);
            ResultStrRespVO respVO = RemoteDubbo.get(SettleBenchBRPCService.class, IDomainConstant.MDD_DOMAIN_STWB).newRelationSettleBench(json);
            log.error("SettleBenchBRPCService newRelationSettleBench请求结束");
            if(!StringUtils.isEmpty(respVO.getCheckIdentificationCode())){
                corrDataEntities.forEach(corrDataEntity -> corrDataEntity.setSmartcheckno(respVO.getCheckIdentificationCode()));
            }
        } else {
            SettleDeatailRelBankBillReqVO json = new SettleDeatailRelBankBillReqVO();
            json.setBankCheck_id(String.valueOf(bid));
            json.setSettleBenchB_id(String.valueOf(busid));
            // 根据id查询到银行对账单
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrDataEntities.get(0).getBankReconciliationId());
            // 1为退票
            short isrefund_status = 1;
            SimpleDateFormat outputFormat = new SimpleDateFormat(DateUtils.DATE_TIME_PATTERN);
            Date tranTime = bankReconciliation.getTran_time() == null ? bankReconciliation.getTran_date() : bankReconciliation.getTran_time();
            //如果退票状态不为空，并且为退票
            if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
                //传入退票状态和退票金额
                json.setIsrefund(String.valueOf(isrefund_status));
                json.setRefundAmt(bankReconciliation.getTran_amt());
                // 将日期字符串解析为 Date 对象，带有时分秒
                json.setRefundDate(tranTime);//退票时间
            } else { //非退票.必传，不然结算会报错
                json.setIsrefund(String.valueOf(0));
                json.setRefundAmt(new BigDecimal(String.valueOf(0)));
            }
            // 流水与结算明细关联：依据“流水支持处理方式”进行判定，如为“仅关联”，仅按照财资统一对账码
            if (bankReconciliation.getIsparsesmartcheckno() != null && bankReconciliation.getIsparsesmartcheckno().equals(BooleanUtils.toBoolean(ReconciliationSupportWayEnum.ONLY_ASSOCIATION.getValue()))) {
                //智能对账：新增勾兑号传递
                json.setCheck_identification_code(corrDataEntities.get(0).getSmartcheckno());
            }
            // 将日期字符串解析为 Date 对象，带有时分秒
            //CZFW-273088 关联接口传递交易时间
            json.setSettlesuccesstime(outputFormat.format(tranTime));
            //CZFW-273088 关联接口传递交易日期
            json.setSettlesuccessdate(DateUtils.dateFormat(tranTime, DateUtils.DATE_TIME_PATTERN));
            //关联确认接口增加金额传递
            json.setActualExchangePaymentAmount(bankReconciliation.getTran_amt());
            //202408 资金结算返回财资统一对账码
            //加日志
            log.error("SettleBenchBRPCService newRelationSettleBench请求参数 = {}",json);
            ResultStrRespVO respVO = RemoteDubbo.get(SettleBenchBRPCService.class, IDomainConstant.MDD_DOMAIN_STWB).newRelationSettleBench(json);
            log.error("SettleBenchBRPCService newRelationSettleBench请求结束");
            if(!StringUtils.isEmpty(respVO.getCheckIdentificationCode())){
                for(CorrDataEntity entity:corrDataEntities){
                    entity.setSmartcheckno(respVO.getCheckIdentificationCode());
                }
            }
        }

    }

    private Date getmaxTransTimeByBankReconciliations(List<BankReconciliation> bankReconciliations) {
        return bankReconciliations.stream()
                .map(this::getTransactionTime)  // 提取时间获取逻辑
                .filter(Objects::nonNull)  // 过滤空值
                .max(Comparator.nullsFirst(Comparator.naturalOrder()))  // 显式处理null
                .orElse(null);
    }
    // 新增辅助方法
    private Date getTransactionTime(BankReconciliation bankReconciliation) {
        return bankReconciliation.getTran_time() != null
                ? bankReconciliation.getTran_time()
                : bankReconciliation.getTran_date();
    }

    //认领单关联结算明细时，查找认领单对应的流水是否为退票，如果为是，需把退票标识传递给结算明细
    private static List<BankReconciliation> getBankReconciliationsByClaimId(Long bid) throws Exception {
        List<BillClaimItem> billClaimItems;
        //查询认领单子表中的对账单id字段
        QuerySchema querySchema = QuerySchema.create().addSelect("bankbill");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").in(bid));
        querySchema.addCondition(group);
        billClaimItems =  MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME,querySchema,null);
        if (billClaimItems == null || billClaimItems.size() < 1 || billClaimItems.stream().anyMatch(item -> item.getBankbill() == null)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004B", "根据认领单查找流水异常，认领单的流水id找不到！") /* "根据认领单查找流水异常，认领单的流水id找不到！" */);
        }
        List<BankReconciliation> bankReconciliations;
        //查询对账单的退票字段
        QuerySchema querySchema2 = QuerySchema.create().addSelect("id","refundstatus", "tran_date", "tran_time");
        List<Long> bankbills = billClaimItems.stream().map(item -> item.getBankbill()).collect(Collectors.toList());
        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(bankbills));
        querySchema2.addCondition(group2);
        bankReconciliations =  MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema2,null);
        if (bankReconciliations == null || bankReconciliations.size() != bankbills.size()) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004A", "根据认领单查找流水异常，对应的流水不存在！") /* "根据认领单查找流水异常，对应的流水不存在！" */);
        }
        return bankReconciliations;
    }

    //认领单关联结算明细时，查找认领单对应的流水是否为退票，如果为是，需把退票标识传递给结算明细
    private static String getRefundByBankReconciliations(List<BankReconciliation> bankReconciliations) throws Exception {
        Short init_refundstatus = bankReconciliations.get(0).getRefundstatus();
        //此时要么是退票，要么是null——非退票
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            Short cur_refundstatus = bankReconciliation.getRefundstatus();
            if (!Objects.equals(cur_refundstatus, init_refundstatus)) {
                log.error("历史数据，退票状态不同时，默认为否. 当前状态:{}，初始状态:{}", cur_refundstatus, init_refundstatus);
                return refundstatusFalseStr;
            }
        }
        //都相同时，返回实际退票标识
        return Objects.equals(init_refundstatus, RefundStatus.Refunded.getValue())
                ? refundstatusTrueStr
                : refundstatusFalseStr;
    }


    /**
     * 回写投融资
     *
     * @param isClaim
     * @param billType
     * @param busid
     * @param bid
     * @param dcFlag
     * @param bank_seq_no
     * @param corrDataEntities
     * @param paramMap
     * @throws Exception
     */
    void writeBackTlm(boolean isClaim, String billType, Long busid, Long bid, Short dcFlag, String bank_seq_no, List<CorrDataEntity> corrDataEntities, Map<String, Object> paramMap) throws Exception {
        CommonInfoReq commonInfoReq = new CommonInfoReq();
        Map<String, String> extParam = new HashMap<>();
        if (isClaim) {//认领单
            extParam.put("claim_id", bid.toString());
            extParam.put("tlm_bus_id", busid.toString());
            extParam.put("bank_seq_no", bank_seq_no);
            extParam.put("smartCheckNo", corrDataEntities.get(0).getSmartcheckno());
            if (tlm_repayment.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_repaymentRef");
                commonInfoReq.setExtParam(extParam);
                commonInfoReq.setBillNum("tlm_repaymentRef");
            } else if (tlm_payinterest.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_payinterestref");
                commonInfoReq.setExtParam(extParam);
                commonInfoReq.setBillNum("tlm_payinterestref");
            } else if (tlm_financepayfee.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_financepayfeeRef");
                commonInfoReq.setExtParam(extParam);
                commonInfoReq.setBillNum("tlm_financepayfeeRef");
            } else if (tlm_financeinStatement.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_financeinStatementRef");
                //融入登记子表ID
                if(paramMap != null && paramMap.get("tlm_idunderwriter") != null) {
                    extParam.put("tlm_bus_idunderwriter", String.valueOf(paramMap.get("tlm_idunderwriter")));
                }
                commonInfoReq.setExtParam(extParam);
                commonInfoReq.setBillNum("tlm_financeinStatementRef");
            } else if (tlm_interestcollection.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_interestcollectionRef");
                commonInfoReq.setBillNum("tlm_interestcollectionRef");
            } else if (tlm_purchaseregister.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_purchaseregisterRef");
                commonInfoReq.setBillNum("tlm_purchaseregisterRef");
            } else if (tlm_investredem.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_investredemRef");
                commonInfoReq.setBillNum("tlm_investredemRef");
            } else if (tlm_investpayment.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_investpaymentRef");
                commonInfoReq.setBillNum("tlm_investpaymentRef");
            } else if (tlm_investprofitsharing.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_investprofitsharingRef");
                commonInfoReq.setBillNum("tlm_investprofitsharingRef");
            } else if (tlm_tradedelivery.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_tradedeliveryRef");
                //借对应付款
                //tradeDirection 1(付款方向使用String 必填)
                //tradeDirection 0（收款方向使用String 必填）
                if (dcFlag.equals(Direction.Debit.getValue())) {
                    extParam.put("tradeDirection", "1");
                } else {
                    extParam.put("tradeDirection", "0");
                }
                commonInfoReq.setBillNum("tlm_tradedeliveryRef");
            } else if (tlm_derivativesclose.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_derivativescloseRef");
                commonInfoReq.setBillNum("tlm_derivativescloseRef");
            } else if (tlm_traderolloverregister.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_traderolloverregisterRef");
                commonInfoReq.setBillNum("tlm_traderolloverregisterRef");
            } else if (tlm_addbond.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_addbondRef");
                commonInfoReq.setBillNum("tlm_addbondRef");
            }
        } else {//银行对账单
            extParam.put("bankbill_id", bid.toString());
            //融入登记单有自己的赋值逻辑
            if (!tlm_financeinStatement.equals(billType)) {
                extParam.put("tlm_bus_id", busid.toString());
            }
            extParam.put("bank_seq_no", bank_seq_no);
            extParam.put("smartCheckNo", corrDataEntities.get(0).getSmartcheckno());
            if (tlm_repayment.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_repaymentRef");
                commonInfoReq.setBillNum("tlm_repaymentRef");
            } else if (tlm_payinterest.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_payinterestref");
                commonInfoReq.setBillNum("tlm_payinterestref");
            } else if (tlm_financepayfee.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_financepayfeeRef");
                commonInfoReq.setBillNum("tlm_financepayfeeRef");
            } else if (tlm_financeinStatement.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_financeinStatementRef");
                extParam.put("tlm_bus_id", paramMap.get("mainid").toString());
                //融入登记子表ID
                // CZFW-182438
                if (paramMap.get("tlm_idunderwriter") != null) {
                    extParam.put("tlm_bus_idunderwriter", paramMap.get("tlm_idunderwriter").toString());
                }
                commonInfoReq.setBillNum("tlm_financeinStatementRef");
            } else if (tlm_interestcollection.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_interestcollectionRef");
                commonInfoReq.setBillNum("tlm_interestcollectionRef");
            } else if (tlm_purchaseregister.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_purchaseregisterRef");
                commonInfoReq.setBillNum("tlm_purchaseregisterRef");
            } else if (tlm_investredem.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_investredemRef");
                commonInfoReq.setBillNum("tlm_investredemRef");
            } else if (tlm_investpayment.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_investpaymentRef");
                commonInfoReq.setBillNum("tlm_investpaymentRef");
            } else if (tlm_investprofitsharing.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_investprofitsharingRef");
                commonInfoReq.setBillNum("tlm_investprofitsharingRef");
            } else if (tlm_tradedelivery.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_tradedeliveryRef");
                //借对应付款,贷对应收款
                //tradeDirection 1(付款方向使用String 必填)
                //tradeDirection 0（收款方向使用String 必填）
                if (dcFlag.equals(Direction.Debit.getValue())) {
                    extParam.put("tradeDirection", "1");
                } else {
                    extParam.put("tradeDirection", "0");
                }
                commonInfoReq.setBillNum("tlm_tradedeliveryRef");
            } else if (tlm_derivativesclose.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_derivativescloseRef");
                commonInfoReq.setBillNum("tlm_derivativescloseRef");
            } else if (tlm_traderolloverregister.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_traderolloverregisterRef");
                commonInfoReq.setBillNum("tlm_traderolloverregisterRef");
            } else if (tlm_addbond.equals(billType)) {
                extParam.put("refer_bill_no", "tlm_addbondRef");
                commonInfoReq.setBillNum("tlm_addbondRef");
            }
        }
        if (StringUtils.isNotEmpty(commonInfoReq.getBillNum())) {//投融资的billnum不为空，则代表需要调用投融资进行回写
            commonInfoReq.setExtParam(extParam);
            RemoteDubbo.get(BillCheckRelateTLMRPC.class, IDomainConstant.MDD_DOMAIN_TLM).billCheckRelationTLM(commonInfoReq);
        }
    }
}
