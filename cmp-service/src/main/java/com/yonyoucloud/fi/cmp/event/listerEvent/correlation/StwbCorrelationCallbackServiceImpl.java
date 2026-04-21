package com.yonyoucloud.fi.cmp.event.listerEvent.correlation;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.CallBackBusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonServiceImpl;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description: 资金调度关联回调监听，用来处理待结算数据关联对账单 和 退票相关处理
 * @author: wanxbo@yonyou.com
 * @date: 2023/5/9 14:57
 * "eventType": "relationSettle"
 */
@Slf4j
@Service
public class StwbCorrelationCallbackServiceImpl implements IEventReceiveService {

    public static final String FUND_PAYMENT_B_FULLNAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUND_COLLECTION_B_FULLNAME = "cmp.fundcollection.FundCollection_b";

    @Autowired
    private FundCommonServiceImpl fundCommonService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    static final String CANCEL_RELATION_SETTLE = "cancelRelationSettle";

    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws BusinessException {
        String callInfo = businessEvent.getUserObject();
        CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
        try {
            if (!ICmpConstant.CMDOMAIN.equals(s)) { // 非现金管理的单据不进行处理
                return JsonUtils.toJsonString(EventResponseVO.success());
            }
            DataSettledDetail dataSettledDetail = CtmJSONObject.parseObject(callInfo, DataSettledDetail.class);
            Long dataSettledId = dataSettledDetail.getDataSettledId();
            String lockKey = "SettledDataWriteBack_" + dataSettledId;
            return CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
                CtmJSONObject logData = new CtmJSONObject();
                try {
                    if (lockStatus == LockStatus.GETLOCK_FAIL) {
                        // 加锁失败
                        return JsonUtils.toJsonString(EventResponseVO.fail("GET_LOCK_FAIL"));
                    }

                    String action = businessEvent.getAction();
                    // 获锁成功
                    if ((String.valueOf(BusinessBillType.CashPayBill.getValue()).equals(dataSettledDetail.getBusinessBillType()))||ICmpConstant.PAYMENTBILLTYPEID.equals(dataSettledDetail.getBusinessBillType())) {// 资金付款单
                        List<FundPayment_b> billbs = getFundPayment_b(dataSettledDetail.getDataSettledId(), dataSettledDetail.getBusinessDetailsId());
                        String code="";
                        if (CollectionUtils.isEmpty(billbs)) {
                            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, dataSettledDetail.getBusinessBillId(), 3);
                            if (fundPayment != null) {
                                code = fundPayment.getCode();
                                billbs = fundPayment.FundPayment_b();
                            }
                        }
                        if (CollectionUtils.isNotEmpty(billbs)) {
                            // 根据资金付款单主表id查询是否有关联关系
                            String mainid = billbs.get(0).getMainid();
                            QuerySchema querySchema = QuerySchema.create().addSelect("*");
                            QueryConditionGroup group = QueryConditionGroup.and(
                                    QueryCondition.name("srcbillid").eq(mainid)
                            );
                            querySchema.addCondition(group);
                            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
                            CtmJSONObject ctmJSONObject = new CtmJSONObject();
                            ctmJSONObject.put("businessEvent", businessEvent);
                            ctmJSONObject.put("dataSettledDetail", dataSettledDetail);
                            ctmJSONObject.put("bankReconciliationbusrelation_bs", bankReconciliationbusrelation_bs);
                            ctmcmpBusinessLogService.saveBusinessLog(
                                    ctmJSONObject, code, IMsgConstant.FUND_PAYMENT_RELATION_SETTLES, IServicecodeConstant.FUNDPAYMENT,  IMsgConstant.FUND_PAYMENT,
                                    OperCodeTypes.update.getDefaultOperateName());
                            if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                logData.put("billcode",bankReconciliationbusrelation_bs.get(0).getBillcode());
                            }
                            // 关联或取消关联时，需要判断流水关联的是什么单据，如果是关联的结算明细，则需要更新资金收付款的关联关系，否则不处理关联关系
                            if (CANCEL_RELATION_SETTLE.equals(action)) {
                                if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                    return JsonUtils.toJsonString(EventResponseVO.success());
                                } else {
                                    for (FundPayment_b fundPayment_b : billbs){
                                        List<FundPayment_b> tempList = new ArrayList<>();
                                        tempList.add(fundPayment_b);
                                        //只有在存在关联关系，且不是统收统支模式下，认领单生成的第一个资金付款单时，才清空关联关系
                                        if (fundPayment_b.getAssociationStatus() == AssociationStatus.Associated.getValue()
                                                && !extractedClaim(fundPayment_b.getBillClaimId() != null ? Long.parseLong(fundPayment_b.getBillClaimId()) : null)){
                                            clearFundPaymentRelationStatus(tempList);
                                        }
                                    }
                                }
                            } else {
                                if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                    return JsonUtils.toJsonString(EventResponseVO.success());
                                } else {
                                    fundCommonService.updateRefundSettledInfoOfFundPayment(dataSettledDetail, billbs);
                                }
                            }
                        }
                    } else if ((String.valueOf(BusinessBillType.CashRecBill.getValue()).equals(dataSettledDetail.getBusinessBillType()))||ICmpConstant.COLLECTIONBILLTYPEID.equals(dataSettledDetail.getBusinessBillType())) { // 资金收款单
                        // 更新资金收款单关联数
                        List<FundCollection_b> billbToUpdateRelation = getFundCollection_b(dataSettledDetail.getDataSettledId(), dataSettledDetail.getBusinessDetailsId());
                        String code="";
                        if (CollectionUtils.isEmpty(billbToUpdateRelation)) {
                            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME,
                                    dataSettledDetail.getBusinessBillId(), 3);
                            if (fundCollection != null) {
                                code=fundCollection.getCode();
                                billbToUpdateRelation = fundCollection.FundCollection_b();
                            }
                        }
                        // 如果为取消关联，则需要判断单据关联的是什么单据，如果是关联的结算明细，则将关联信息清空就行，否则不能清空关联关系
                        if(CollectionUtils.isNotEmpty(billbToUpdateRelation)){
                            // 根据资金付款单主表id查询是否有关联关系
                            String mainid = billbToUpdateRelation.get(0).getMainid();
                            QuerySchema querySchema = QuerySchema.create().addSelect("*");
                            QueryConditionGroup group = QueryConditionGroup.and(
                                    QueryCondition.name("srcbillid").eq(mainid)
                            );
                            querySchema.addCondition(group);
                            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
                            CtmJSONObject ctmJSONObject = new CtmJSONObject();
                            ctmJSONObject.put("businessEvent", businessEvent);
                            ctmJSONObject.put("dataSettledDetail", dataSettledDetail);
                            ctmJSONObject.put("bankReconciliationbusrelation_bs", bankReconciliationbusrelation_bs);
                            ctmcmpBusinessLogService.saveBusinessLog(
                                    ctmJSONObject, code, IMsgConstant.FUND_COLLECTION_RELATION_SETTLES,
                                    IServicecodeConstant.FUNDCOLLECTION,  IMsgConstant.FUND_COLLECTION,
                                    OperCodeTypes.update.getDefaultOperateName());
                            if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                logData.put("billcode",bankReconciliationbusrelation_bs.get(0).getBillcode());
                            }
                            // 关联或取消关联时，需要判断流水关联的是什么单据，如果是关联的结算明细，则需要更新资金收付款的关联关系，否则不处理关联关系
                            if (CANCEL_RELATION_SETTLE.equals(action)) {
                                if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                    return JsonUtils.toJsonString(EventResponseVO.success());
                                } else {
                                    for (FundCollection_b fundCollection_b : billbToUpdateRelation){
                                        List<FundCollection_b> tempList = new ArrayList<>();
                                        tempList.add(fundCollection_b);
                                        //只有在存在关联关系，且不是统收统支模式下，认领单生成的第一个资金付款单时，才清空关联关系
                                        if (fundCollection_b.getAssociationStatus() == AssociationStatus.Associated.getValue()
                                                && !extractedClaim(fundCollection_b.getBillClaimId() != null ? Long.parseLong(fundCollection_b.getBillClaimId()) : null)){
                                            clearFundCollectionRelationStatus(tempList);
                                        }
                                    }
                                }
                            } else {
                                if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                    return JsonUtils.toJsonString(EventResponseVO.success());
                                } else {
                                    fundCommonService.updateFundCollectionRelationInfo(dataSettledDetail, billbToUpdateRelation);
                                }
                            }
                        }
                    } else if (String.valueOf(BusinessBillType.TransferBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {
                        if (CANCEL_RELATION_SETTLE.equals(action)) {
                            // 待结算数据收付类型：1收/2付
                            if ("2".equals(dataSettledDetail.getRecpaytype())) {
                                // 根据待结算数据id，查询转账单
    //                        List<TransferAccount> transferAccountList = getTransferAccountPayByTransNumber(dataSettledDetail.getDataSettledId());
                                TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, dataSettledDetail.getBusinessBillId());
                                if (transferAccount != null) {
                                    // 一个结算单只能对应一个转账单，清空付方关联信息
    //                            TransferAccount transferAccount = transferAccountList.get(0);
                                    if (SettleStatus.SettledRep != transferAccount.getSettlestatus()) {
                                        transferAccount.setAssociationStatusPay(false);
                                        transferAccount.setPaybankbill(null);// 付款_银行对账单ID
                                        transferAccount.setPaybillclaim(null);// 付款_认领单ID
                                        transferAccount.setEntityStatus(EntityStatus.Update);
                                        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                                    }

                                }
                            } else {
                                List<TransferAccount> transferAccountList = getTransferAccountRecByTransNumber(dataSettledDetail.getDataSettledId());
                                if (transferAccountList != null && transferAccountList.size() > 0) {
                                    // 一个结算单只能对应一个转账单，清空收方关联信息
                                    TransferAccount transferAccount = transferAccountList.get(0);
                                    if (SettleStatus.SettledRep != transferAccount.getSettlestatus()) {
                                        transferAccount.setAssociationStatusCollect(false);
                                        transferAccount.setCollectbankbill(null);// 收款_银行对账单ID
                                        transferAccount.setCollectbillclaim(null);// 收款_认领单ID
                                        transferAccount.setEntityStatus(EntityStatus.Update);
                                        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                                    }

                                }
                            }
                        }

                    }
                    return JsonUtils.toJsonString(EventResponseVO.success());
                }catch (Exception e){
                    // 关联关系绑定
                    if (!StringUtils.isEmpty(logData.toString())) {
                        ctmcmpBusinessLogService.saveBusinessLog(logData, "", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400592", "关联或取消关联异常时的数据") /* "关联或取消关联异常时的数据" */, IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.RECONCILIATE, IMsgConstant.DELETE);
                    }
                    throw new BusinessException(e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("StwbCorrelationCallbackServiceImpl.error: userObject = {}, errorMsg = {}", callInfo, e.getMessage());
            return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
        }
    }

    private static boolean extractedClaim(Long claimid) throws Exception {
        if (claimid != null) {
            // 认领单关联关系处理
            //回写认领单
            QuerySchema bquery = QuerySchema.create().addSelect("*");
            QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").eq(claimid));
            bquery.addCondition(querygroup);
            List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
            if (CollectionUtils.isEmpty(billClaims)) {
                QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(claimid));
                bquery2.addCondition(querygroup2);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                if (CollectionUtils.isNotEmpty(billClaims)){
                    return true;
                }
            }else {
                return false;
            }
        }
        return false;
    }

    /**
     * 清空资金付款单关联关系
     *
     * @param billbs
     */
    private void clearFundPaymentRelationStatus(List<FundPayment_b> billbs) throws Exception {
        String tablename = "cmp_fundpayment_b";
        for (FundPayment_b billb : billbs) {
            QuerySchema bquery2 = QuerySchema.create().addSelect("*");
            QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(billb.getBillClaimId()));
            bquery2.addCondition(querygroup2);
            List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
            // 如果资金收付款是走的参照关联，则清空的时候只清空流水id，参照id不能清空
            if(CollectionUtils.isNotEmpty(billClaims)){
                Map<String, Object> params = new HashMap<>();
                params.put("id", billb.getId());
                params.put("tableName", tablename);
                params.put("ytenantId", InvocationInfoProxy.getTenantid());
                log.error("updateFundCollectionRelationInfo params={}", params);
                SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.clearRelationBankreconciliationId", params);
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("id", billb.getId());
            params.put("tableName", tablename);
            params.put("ytenantId", InvocationInfoProxy.getTenantid());
            log.error("updateRefundSettledInfoOfFundPayment params={}", params);
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.clearRelationFundBillSubById", params);
        }
    }

    /**
     * 清空资金付款单关联关系
     *
     * @param billbs
     */
    private void clearFundCollectionRelationStatus(List<FundCollection_b> billbs) throws Exception {
        String tablename = "cmp_fundcollection_b";
        for (FundCollection_b billb : billbs) {
            if(billb.getBillClaimId() != null){
                QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(billb.getBillClaimId()));
                bquery2.addCondition(querygroup2);
                List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                // 如果资金收付款是走的参照关联，则清空的时候只清空流水id，参照id不能清空
                if(CollectionUtils.isNotEmpty(billClaims)){
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", billb.getId());
                    params.put("tableName", tablename);
                    params.put("ytenantId", InvocationInfoProxy.getTenantid());
                    log.error("updateFundCollectionRelationInfo params={}", params);
                    SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.clearRelationBankreconciliationId", params);
                    return;
                }
            }
            Map<String, Object> params = new HashMap<>();
            params.put("id", billb.getId());
            params.put("tableName", tablename);
            params.put("ytenantId", InvocationInfoProxy.getTenantid());
            log.error("updateFundCollectionRelationInfo params={}", params);
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.clearRelationFundBillSubById", params);
        }
    }


    @Override
    public String onCallBack(CallBackBusinessEvent callBackBusinessEvent) throws BusinessException {
        return IEventReceiveService.super.onCallBack(callBackBusinessEvent);
    }

    /**
     * 根据 待结算数据ID 查询转账单
     *
     * @param transNumber
     * @return
     * @throws Exception
     */
    public List<TransferAccount> getTransferAccountPayByTransNumber(Long transNumber) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        // 要么收方要么付方，都对应同一笔转账单，直接 or 查询了
        queryConditionGroup.appendCondition(QueryCondition.name("paymentid").eq(transNumber));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
    }

    /**
     * 根据 待结算数据ID 查询转账单
     *
     * @param transNumber
     * @return
     * @throws Exception
     */
    public List<TransferAccount> getTransferAccountRecByTransNumber(Long transNumber) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        // 要么收方要么付方，都对应同一笔转账单，直接 or 查询了
        queryConditionGroup.appendCondition(QueryCondition.name("collectid").eq(transNumber));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
    }

    /**
     * 根据交易流水号查询资金收款单
     *
     * @param transNumber
     * @return【
     * @throws Exception
     */
    public List<FundCollection_b> getFundCollection_b(Long transNumber, String businessDetailsId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("transNumber").eq(String.valueOf(transNumber)));
        querySchema.addCondition(queryConditionGroup);
        List<FundCollection_b> bList= MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
        if (bList.isEmpty()) {
            QuerySchema query = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryCondition = new QueryConditionGroup(ConditionOperator.and);
            queryCondition.appendCondition(QueryCondition.name("id").eq(businessDetailsId));
            query.addCondition(queryCondition);
            bList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, query, null);
        }
        return bList;
    }

    /**
     * 根据交易流水号查询资金付款单
     *
     * @param transNumber
     * @return
     * @throws Exception
     */
    public List<FundPayment_b> getFundPayment_b(Long transNumber, String businessDetailsId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("transNumber").eq(String.valueOf(transNumber)));
        querySchema.addCondition(queryConditionGroup);
        List<FundPayment_b> bList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
        if (bList.isEmpty()) {
            QuerySchema query = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryCondition = new QueryConditionGroup(ConditionOperator.and);
            queryCondition.appendCondition(QueryCondition.name("id").eq(businessDetailsId));
            query.addCondition(queryCondition);
            bList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, query, null);
        }
        return bList;
    }
}
