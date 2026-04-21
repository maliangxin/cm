package com.yonyoucloud.fi.cmp.ctmrpc.settleverify;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.BusinessException;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.settle.itf.param.*;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.api.settleverify.CmpBankReconciliationAndClaimSettleVerifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BillClaimType;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.BusinessModel;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h1>认领单与对账单结算检查接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-06-29 10:00
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CmpBankReconciliationAndClaimSettleVerifyServiceImpl implements CmpBankReconciliationAndClaimSettleVerifyService {

    private final Short delcorrStatus = 0;
    private final Short delcorrRowStatus = 1;


    private final YmsOidGenerator ymsOidGenerator;

    @Override
    public SettleOperResult[] validate(SettleOperType operType, SettleOperContext context) throws BusinessException {
        List<SettleOperResult> settleOperatorResultList = new ArrayList<>();
        Settlement settlement = context.getSettlement();
        if (settlement != null && settlement.getBodys() != null) {
            SettleBody[] settlementBody = settlement.getBodys();

            // 收集认领单id
            Set<String> claimOrDzdIds = new HashSet<>();
            // 收集对账单id
            Map<String, String> settleIdReleationMap = new HashMap<>();

            // 认领单或对账单标识，传过来的认领单id可能为认领单id或对账单id或参照关联id
            boolean claimFlag = false;
            for (SettleBody settleBody : settlementBody) {
                String claimId = settleBody.getPk_claim();
                if (ValueUtils.isNotEmpty(claimId)) {
                    QuerySchema querySchema = QuerySchema.create().addSelect("id, bank_seq_no, checkflag, other_checkflag");
                    QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
                    queryConditionGroup.addCondition(QueryCondition.name("id").in(claimId));
                    querySchema.addCondition(queryConditionGroup);
                    try {
                        List<Map<String, Object>> result = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
                        //根据id没有查询到对账单，则认为是认领单
                        if (CollectionUtils.isEmpty(result)) {
                            claimFlag = true;

                        }
                        claimOrDzdIds.add(claimId);
                        settleIdReleationMap.put(claimId, settleBody.getId());
                    } catch (Exception e) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540059B", "查询对账单失败") /* "查询对账单失败" */);
                    }
                }
            }
            if (claimFlag) {
                SettleOperResult[] settleOperatorResultClaimList = cancelSettleClaimHandle(claimOrDzdIds, settlement, settleIdReleationMap, settleOperatorResultList);
                if (settleOperatorResultClaimList != null && settleOperatorResultClaimList.length != 0) {
                    return settleOperatorResultClaimList;
                }
            } else {
                for (SettleBody settleBody : settlementBody) {
                    String bankReconciliationId = settleBody.getPk_dzd();
                    if (ValueUtils.isNotEmpty(bankReconciliationId)) {
                        claimOrDzdIds.add(bankReconciliationId);
                        settleIdReleationMap.put(bankReconciliationId, settleBody.getId());
                    }
                }
                // 校验银行对账单
                SettleOperResult[] settleOperatorResultBankReconciliationList = cancelSettleBankReconciliationHandle(claimOrDzdIds, settlement, settleIdReleationMap, settleOperatorResultList);
                if (settleOperatorResultBankReconciliationList != null && settleOperatorResultBankReconciliationList.length != 0) {
                    return settleOperatorResultBankReconciliationList;
                }
            }
        }
        return new SettleOperResult[0];
    }

    private static SettleOperResult @Nullable [] cancelSettleBankReconciliationHandle(Set<String> bankReconciliationIds, Settlement settlement, Map<String, String> settleIdReleationbankReconciliationIDMap, List<SettleOperResult> settleOperatorResultList) {
        if (CollectionUtils.isNotEmpty(bankReconciliationIds)) {
            QuerySchema querySchema = QuerySchema.create().addSelect("id, bank_seq_no, checkflag, other_checkflag");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
            queryConditionGroup.addCondition(QueryCondition.name("id").in(bankReconciliationIds));
            querySchema.addCondition(queryConditionGroup);
            try {
                List<Map<String, Object>> result = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
                if (CollectionUtils.isEmpty(result)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100574"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800054", "根据结算明细pk_dzd未查询银行交易流水数据!") /* "根据结算明细pk_dzd未查询银行交易流水数据!" */);
                }
                for (Map<String, Object> resultMap : result) {
                    SettleOperResult settleOperResult =
                            new SettleOperResult(settlement.getId(), settleIdReleationbankReconciliationIDMap.get(resultMap.get(ICmpConstant.PRIMARY_ID).toString()));
                    boolean isCheckFlag =
                            ValueUtils.isNotEmptyObj(resultMap.get("checkflag")) && Boolean.parseBoolean(resultMap.get("checkflag").toString());
                    boolean isOtherCheckFlag =
                            ValueUtils.isNotEmptyObj(resultMap.get("other_checkflag")) && Boolean.parseBoolean(resultMap.get("other_checkflag").toString());
                    if (isOtherCheckFlag) {
                        settleOperResult.setPass(false);
                        String errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800055", "银行交易流水【%s】已与总账凭证对账完成，请先取消对账！") /* "银行交易流水【%s】已与总账凭证对账完成，请先取消对账！" */,
                                resultMap.get("bank_seq_no").toString());
                        settleOperResult.setErrorMessage(errorMsg);
                    } else if (isCheckFlag) {
                        settleOperResult.setPass(false);
                        String errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800050", "银行交易流水【%s】已与银行日记账对账完成，请先取消对账!") /* "银行交易流水【%s】已与银行日记账对账完成，请先取消对账!" */,
                                resultMap.get("bank_seq_no").toString());
                        settleOperResult.setErrorMessage(errorMsg);
                    } else {
                        settleOperResult.setPass(true);
                    }
                    settleOperatorResultList.add(settleOperResult);
                }
                return settleOperatorResultList.toArray(new SettleOperResult[0]);

            } catch (Exception e) {
                log.error("CmpBankReconciliationAndClaimSettleVerifyServiceImpl validate bankReconciliationIds={}, settlement={}, errorMsg={}", bankReconciliationIds, settlement, e.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100575"), e.getMessage());
            }
        }
        return null;
    }

    private static SettleOperResult @Nullable [] cancelSettleClaimHandle(Set<String> claimIds, Settlement settlement, Map<String, String> settleIdReleationRelaimIDMap, List<SettleOperResult> settleOperatorResultList) {
        if (CollectionUtils.isNotEmpty(claimIds)) {
            QuerySchema querySchema = QuerySchema.create().addSelect("id, code, isfundsplit, settlestatus,refbill");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
            QueryConditionGroup queryConditionGroupById = new QueryConditionGroup();
            queryConditionGroupById.addCondition(QueryCondition.name("id").in(claimIds));
            QueryConditionGroup queryConditionGroupByRefBill = new QueryConditionGroup();
            queryConditionGroupByRefBill.addCondition(QueryCondition.name("refbill").in(claimIds));
            queryConditionGroup.addCondition(queryConditionGroupById, queryConditionGroupByRefBill);
            querySchema.addCondition(queryConditionGroup);
            try {
                List<Map<String, Object>> result = MetaDaoHelper.query(BillClaim.ENTITY_NAME, querySchema);
                if (CollectionUtils.isEmpty(result)) {
                    QuerySchema querySchema2 = QuerySchema.create().addSelect("id");
                    querySchema2.appendQueryCondition(QueryCondition.name("id").in(claimIds));
                    result = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema2);
                    if (CollectionUtils.isEmpty(result)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100576"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800051", "根据结算明细pk_claim未查询认领单数据!") /* "根据结算明细pk_claim未查询认领单数据!" */);
                    } else {
                        return null;
                    }
                }
                for (Map<String, Object> resultMap : result) {
                    SettleOperResult settleOperResult = null;
                    //认领单若是refbill不为空的时候，则使用refbill进行校验认领单，否则会找不到认领单，因为结算单撤回处存储的是认领单的refbill
                    if (!StringUtils.isEmpty(settleIdReleationRelaimIDMap.get(resultMap.get(ICmpConstant.PRIMARY_ID).toString()))){
                        settleOperResult =
                                new SettleOperResult(settlement.getId(), settleIdReleationRelaimIDMap.get(resultMap.get(ICmpConstant.PRIMARY_ID).toString()));
                    } else {
                        settleOperResult =
                                new SettleOperResult(settlement.getId(), settleIdReleationRelaimIDMap.get(resultMap.get("refbill")));
                    }
                    if (!ValueUtils.isNotEmptyObj(resultMap.get(ICmpConstant.SETTLE_STATUS))
                            || !ValueUtils.isNotEmptyObj(resultMap.get("isfundsplit"))) {
                        settleOperResult.setPass(true);
                    } else {
                        short settleStatus = Short.parseShort(resultMap.get(ICmpConstant.SETTLE_STATUS).toString());
                        boolean isFundSplit = Boolean.parseBoolean(resultMap.get("isfundsplit").toString());
                        if (isFundSplit && settleStatus == FundSettleStatus.SettleSuccess.getValue()) {
                            settleOperResult.setPass(false);
                            String errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5B02804380033", "认领单【%s】已完成资金切分，并产生了后续业务，暂不支持取消结算，请检查！") /* "认领单【%s】已完成资金切分，并产生了后续业务，暂不支持取消结算，请检查！" */,
                                    resultMap.get(ICmpConstant.CODE).toString());
                            settleOperResult.setErrorMessage(errorMsg);
                        } else {
                            settleOperResult.setPass(true);
                        }
                    }
                    settleOperatorResultList.add(settleOperResult);
                }
                return settleOperatorResultList.toArray(new SettleOperResult[0]);

            } catch (Exception e) {
                log.error("CmpBankReconciliationAndClaimSettleVerifyServiceImpl validate claimIds={}, settlement={}, errorMsg={}", claimIds, settlement, e.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100577"), e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void handle(SettleOperType operType, SettleOperContext context) throws Exception {
        SettleOperResult[] validate = validate(operType, context);
        for (SettleOperResult settleOperResult : validate) {
            if (!settleOperResult.isPass())
                throw new Exception(settleOperResult.getErrorMessage());
        }
        String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        Settlement settlement = context.getSettlement();
        if (settlement != null && settlement.getBodys() != null) {
            SettleBody[] settlementBody = settlement.getBodys();
            for (SettleBody settleBody : settlementBody) {
                // 银行对账单id
                Long busid = ValueUtils.isNotEmptyObj(settleBody.getPk_dzd()) ? Long.valueOf(settleBody.getPk_dzd()) : null;
                // 业务单据id 资金结算单
                Long stwbbusid = ValueUtils.isNotEmptyObj(settleBody.getId()) ? Long.valueOf(settleBody.getId()) : null;
                // 认领单id
                Long claimid = ValueUtils.isNotEmptyObj(settleBody.getPk_claim()) ? Long.valueOf(settleBody.getPk_claim()) : null;

                List<BankReconciliationbusrelation_b> bs = new ArrayList<>();
                try {
                    // 关联认领单
                    if (claimid != null) {
                        // 回写认领单
                        QuerySchema bquery = QuerySchema.create().addSelect("*");
                        QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").eq(claimid));
                        bquery.addCondition(querygroup);
                        List<BillClaim> billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
                        // 根据参照关联id未查询到认领单，则应该是流水id回写
                        if (CollectionUtils.isNotEmpty(billClaims)) {
                            //是否处理了关联关系
                            boolean dealFlag = false;
                            Short businessmodel = billClaims.get(0).getBusinessmodel();
                            // 首先判断认领单的认领类型
                            // 如果是部分认领 首先通过认领单子表获取到银行对账单，然后获取银行对账单的关联关系，查询关联关系表中数据的billid，判断是否包含stwbbusid
                            // 如果包含，则删除关联stwbbusid的关联关系数据，并且修改银行对账单和认领单的关联状态为未关联，清空勾兑码；如果不是，则直接返回。
                            if (billClaims.get(0).getClaimtype() != null && billClaims.get(0).getClaimtype() == BillClaimType.Part.getValue()) {

                                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaims.get(0).getId()));
                                querySchema.addCondition(group);
                                List<BillClaimItem> claimItemList = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                                if (claimItemList != null && claimItemList.size() > 0) {
                                    // 银行对账单id
                                    long bankreconciliationId = claimItemList.get(0).getBankbill();
                                    // 根据银行对账单id查询到银行对账单的关联关系数据
                                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");// 业务单据子表id、单据类型
                                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
                                    querySchema1.addCondition(group1);
                                    List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
                                    if (bankReconciliationbusrelation_bs != null && bankReconciliationbusrelation_bs.size() > 0) {
                                        // 判断银行对账单关联关系数据中的billid中是否包含stwbbusid，如果包含，则进行删除次关联关系，并且
                                        for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                                            if (b.getBillid().equals(stwbbusid)) {
                                                bs.add(b);
                                                YtsContext.setYtsContext("corrData", bs);
                                                //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, b);

                                                // 同时修改认领单的关联状态
                                                billClaims.get(0).setAssociationstatus(delcorrStatus);
                                                billClaims.get(0).setClaimcompletetype(null);
                                                billClaims.get(0).setAssociatedoperator(null);
                                                billClaims.get(0).setAssociateddate(null);
                                                billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                                // 智能对账：认领单，智能勾兑码删除
                                                YtsContext.setYtsContext("billclaim" + billClaims.get(0).getId(), billClaims.get(0).getSmartcheckno());
                                                billClaims.get(0).setSmartcheckno(smartCheckNo);
                                                dealFlag = true;
                                            }
                                        }

                                        billClaims.get(0).setSettlestatus(null);
                                        CommonSaveUtils.updateBillClaim(billClaims.get(0));

                                        // 修改 银行对账单 的关联状态为未关联，清空勾兑码
                                        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                                        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankreconciliationId));
                                        querySchema2.addCondition(group2);
                                        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                                        boolean needUpdate = false;
                                        //认领单关联关系删除对账单一定为未完结 ;CZFW-417440 必须是结算单删除才去掉完结状态
                                        if (bankReconciliations != null && bs.size() > 0) {
                                            /*bankReconciliations.get(0).setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
                                            bankReconciliations.get(0).setSerialdealtype(null);
                                            needUpdate = true;
                                        }
                                        if (bankReconciliations != null && bankReconciliations.size() > 0 && bankReconciliationbusrelation_bs.size() < 2) {
                                            /*bankReconciliations.get(0).setAssociationstatus(delcorrStatus);*/
                                            // 智能对账：银行对账单，智能勾兑码删除
                                            YtsContext.setYtsContext("bank" + bankReconciliations.get(0).getId(), bankReconciliations.get(0).getSmartcheckno());
                                            //财资统一对账码非解析来的可以清空
                                            if (!bankReconciliations.get(0).getIsparsesmartcheckno()) {
                                                bankReconciliations.get(0).setSmartcheckno(smartCheckNo);
                                            }
                                            // 将银行对账单确认状态置空
                                            bankReconciliations.get(0).setRelationstatus(null);
                                        }
                                        if (needUpdate) {
                                            bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
                                            CommonSaveUtils.updateBankReconciliation(bankReconciliations.get(0));
                                        }
                                        if (bs.size() > 0) {
                                            CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bs);
                                        }
                                    } else {
                                        // 银行对账单关联关系为空，只回写认领单参照关联状态
                                        // 同时修改认领单的关联状态
                                        billClaims.get(0).setAssociationstatus(delcorrStatus);
                                        billClaims.get(0).setClaimcompletetype(null);
                                        billClaims.get(0).setAssociatedoperator(null);
                                        billClaims.get(0).setAssociateddate(null);
                                        billClaims.get(0).setSettlestatus(null);
                                        billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                        // 智能对账：认领单，智能勾兑码删除
                                        YtsContext.setYtsContext("billclaim" + billClaims.get(0).getId(), billClaims.get(0).getSmartcheckno());
                                        billClaims.get(0).setSmartcheckno(smartCheckNo);
                                        dealFlag = true;
                                        CommonSaveUtils.updateBillClaim(billClaims.get(0));
                                    }
                                }
                            } else {

                                // 如果是整单认领和合并认领，首先通过认领单子表获取到银行对账单，然后获取银行对账单的关联关系，然后查询关联关系数据中的第一条数据中的业务单据类型billtype
                                // 如果是资金结算单，则进行全部删除关联关系数据，并修改认领单的关联状态为未关联，清空勾兑码；如果不是，则直接返回。
                                // 根据认领单id查认领单子表id和以及子表中存的银行对账单id
                                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaims.get(0).getId()));
                                querySchema.addCondition(group);
                                List<BillClaimItem> claimItemList = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                                if (claimItemList != null && claimItemList.size() > 0) {
                                    for (BillClaimItem item : claimItemList) {
                                        // 银行对账单id
                                        long bankreconciliationId = item.getBankbill();
                                        // 根据银行对账单id查询到银行对账单的关联关系数据
                                        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");// 业务单据子表id、单据类型
                                        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
                                        querySchema1.addCondition(group1);
                                        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
                                        if (bankReconciliationbusrelation_bs != null && bankReconciliationbusrelation_bs.size() > 0) {
                                            for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                                                if (b.getBillid().equals(stwbbusid)) {
                                                    bs.add(b);
                                                    YtsContext.setYtsContext("corrData", bs);
                                                    //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, b);

                                                    // 修改 认领单 的关联状态为未关联，清空勾兑码
                                                    billClaims.get(0).setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                                                    billClaims.get(0).setClaimcompletetype(null);
                                                    billClaims.get(0).setAssociatedoperator(null);
                                                    billClaims.get(0).setAssociateddate(null);
                                                    billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                                    // 智能对账：认领单，智能勾兑码删除
                                                    YtsContext.setYtsContext("billclaim" + billClaims.get(0).getId(), billClaims.get(0).getSmartcheckno());
                                                    billClaims.get(0).setSmartcheckno(smartCheckNo);
                                                    dealFlag = true;
                                                }
                                            }

                                            billClaims.get(0).setSettlestatus(null);
                                            billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                            CommonSaveUtils.updateBillClaim(billClaims.get(0));

                                            if (bs.isEmpty()) {
                                                if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540059C", "取消结算") /* "取消结算" */.equals(operType.getDisplay())) {
                                                    billClaims.get(0).setSettlestatus(null);
                                                    billClaims.get(0).setSmartcheckno(smartCheckNo);
                                                    billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                                    CommonSaveUtils.updateBillClaim(billClaims.get(0));
                                                }
                                                // 不删除关联关系就不需要走下面的逻辑了
                                                continue;
                                            }
                                            // 修改 银行对账单 的关联状态为未关联，清空勾兑码
                                            QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                                            QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankreconciliationId));
                                            querySchema2.addCondition(group2);
                                            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                                            for (BankReconciliation bankReconciliation : bankReconciliations) {
                                                Short entryType = bankReconciliation.getEntrytype();
                                                if (EntryType.CrushHang_Entry.getValue() == entryType) {
                                                    bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());
                                                } else {
                                                    bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                                                    //业务完结流程状态清空
                                                    bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
                                                    bankReconciliation.setSerialdealtype(null);
                                                    //财资统一对账码非解析来的可以清空
                                                    if (!bankReconciliation.getIsparsesmartcheckno()) {
                                                        bankReconciliation.setSmartcheckno(smartCheckNo);
                                                    }
                                                    // 将银行对账单确认状态置空
                                                    bankReconciliation.setRelationstatus(null);
                                                }
                                                bankReconciliation.setEntityStatus(EntityStatus.Update);
                                                // 智能对账：银行对账单，智能勾兑码删除
                                                YtsContext.setYtsContext("bank" + bankReconciliations.get(0).getId(), bankReconciliations.get(0).getSmartcheckno());
                                                CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                                            }
                                        } else {
                                            // 银行对账单关联关系为空，只回写认领单参照关联状态
                                            // 同时修改认领单的关联状态
                                            billClaims.get(0).setAssociationstatus(delcorrStatus);
                                            billClaims.get(0).setClaimcompletetype(null);
                                            billClaims.get(0).setAssociatedoperator(null);
                                            billClaims.get(0).setAssociateddate(null);
                                            billClaims.get(0).setSettlestatus(null);
                                            billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                            // 智能对账：认领单，智能勾兑码删除
                                            YtsContext.setYtsContext("billclaim" + billClaims.get(0).getId(), billClaims.get(0).getSmartcheckno());
                                            billClaims.get(0).setSmartcheckno(smartCheckNo);
                                            dealFlag = true;
                                        }
                                    }
                                }
                                // 在所有流水子表中删除关联关系
                                if (CollectionUtils.isNotEmpty(bs)) {
                                    CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bs);
                                }
                                billClaims.get(0).setSettlestatus(null);
                                CommonSaveUtils.updateBillClaim(billClaims.get(0));
                            }
                            if (dealFlag && businessmodel == BusinessModel.General_Settlement.getCode()) {
                                // 修改资金付款单明细表关联状态
                                updateFundCollectionItemsByBillClaimsId(claimid);
                                updateFundPaymentItemsByBillClaimsId(claimid);
                            }
                        } else {
                            busid = claimid;
                            handleBankReconciliation(busid, stwbbusid);
                        }
                    } else { // 银行对账单
                        handleBankReconciliation(busid, stwbbusid);
                    }
                } catch (Exception e) {
                    log.error("删除回写报错：：" + e);
                }
            }
        }
    }

    @Override
    public Object rollBackClaimHandle(SettleOperType operType, SettleOperContext context) throws Exception {
        CommonResponseDataVo resJson = new CommonResponseDataVo();
        Settlement settlement = context.getSettlement();
        if (settlement != null && settlement.getBodys() != null) {
            SettleBody[] settlementBody = settlement.getBodys();
            for (SettleBody settleBody : settlementBody) {
                // 银行对账单id
                Long busid = ValueUtils.isNotEmptyObj(settleBody.getPk_dzd()) ? Long.parseLong(settleBody.getPk_dzd()) : null;
                // 业务单据id 资金结算单
                Long stwbbusid = ValueUtils.isNotEmptyObj(settleBody.getId()) ? Long.parseLong(settleBody.getId()) : null;
                // 认领单id
                Long claimid = ValueUtils.isNotEmptyObj(settleBody.getPk_claim()) ? Long.parseLong(settleBody.getPk_claim()) : null;

                try {
                    List<BankReconciliationbusrelation_b> listbs = (List<BankReconciliationbusrelation_b>) YtsContext.getYtsContext("corrData");

                    if (claimid != null) {// 关联认领单

                        // 回写认领单
                        List<BillClaim> billClaims;
                        QuerySchema bquery = QuerySchema.create().addSelect("*");
                        QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").eq(claimid));
                        bquery.addCondition(querygroup);
                        billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
                        if (billClaims != null && billClaims.size() > 0) {
                            billClaims.get(0).setAssociationstatus(delcorrRowStatus);
                            billClaims.get(0).setEntityStatus(EntityStatus.Update);
                            // 智能对账：回滚认领单的智能勾兑码
                            String smartcheckno = YtsContext.getYtsContext("billclaim" + billClaims.get(0).getId()).toString();
                            billClaims.get(0).setSmartcheckno(smartcheckno);
                            CommonSaveUtils.updateBillClaim(billClaims.get(0));
                        } else {
                            // 结算中心代理模式下 CZFW-191992
                            QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                            QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(claimid));
                            bquery2.addCondition(querygroup2);
                            billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                            if (billClaims != null && billClaims.size() > 0) {
                                billClaims.get(0).setRefassociationstatus(AssociationStatus.NoAssociated.getValue());
                                billClaims.get(0).setEntityStatus(EntityStatus.Update);
                                // 智能对账：回滚认领单的智能勾兑码
                                String smartcheckno = YtsContext.getYtsContext("billclaim" + billClaims.get(0).getId()).toString();
                                billClaims.get(0).setSmartcheckno(smartcheckno);
                                CommonSaveUtils.updateBillClaim(billClaims.get(0));
                            }
                        }

                        // 查找子表数据取出对账单id
                        List<BillClaimItem> list;
                        QuerySchema querySchema = QuerySchema.create().addSelect("*");
                        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(billClaims.get(0).getId()));
                        querySchema.addCondition(group);
                        list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                        if (list != null && list.size() > 0) {
                            for (BillClaimItem bill : list) {
                                busid = bill.getBankbill();
                                QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                                QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                                querySchema2.addCondition(group2);
                                List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                                if (bankReconciliations != null && bankReconciliations.size() > 0) {
                                    /*bankReconciliations.get(0).setAssociationstatus(delcorrRowStatus);*/
                                    bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
                                    // 智能对账：回滚银行对账单的智能勾兑码
                                    String smartcheckno = YtsContext.getYtsContext("bank" + bankReconciliations.get(0).getId()) == null ? null : YtsContext.getYtsContext("bank" + bankReconciliations.get(0).getId()).toString();
                                    bankReconciliations.get(0).setSmartcheckno(smartcheckno);
                                    CommonSaveUtils.updateBankReconciliation(bankReconciliations.get(0));
                                }
                            }
                        } else {
                            busid = claimid;
                            rollbackBankReconciliation(busid);
                        }
                    } else {
                        rollbackBankReconciliation(busid);
                    }
                    if (listbs != null && listbs.size() > 0) {
                        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = listbs.get(0);
                        bankReconciliationbusrelation_b.setId(ymsOidGenerator.nextId());
                        bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Insert);
                        //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_b);
                        List<BizObject> relationList = new ArrayList<>();
                        relationList.add(bankReconciliationbusrelation_b);
                        CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
                    }
                } catch (Exception e) {
                    log.error("删除回写报错：：" + e);
                    resJson.setCode("411");
                    resJson.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059F", "执行程序出错") /* "执行程序出错" */);
                    return resJson;
                }

            }
        }

        resJson.setCode("200");
        resJson.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A0", "执行成功") /* "执行成功" */);
        return resJson;
    }

    /**
     * 如果根据bankreconciliationid和stwbbusid查询到关联关系，则删除关联关系，否则不删除关联关系
     * @param bankreconciliationid
     * @param stwbbusid
     * @throws Exception
     */
    private void handleBankReconciliation(Long bankreconciliationid, Long stwbbusid) throws Exception {

        // 根据流水和结算明细id查询关联关系，如果没查到关联关系就流程终止
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(bankreconciliationid), QueryCondition.name("billid").eq(stwbbusid));
        querySchema1.addCondition(group1);
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
        if (CollectionUtils.isEmpty(bankReconciliationbusrelation_bs)) {
            log.error("根据bankreconciliationid和stwbbusid未查询到关联关系！");
            return;
        }else{
            YtsContext.setYtsContext("corrData", bankReconciliationbusrelation_bs);
        }
        String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        // 根据流水id查询流水信息，然后处理相关状态
        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankreconciliationid));
        querySchema2.addCondition(group2);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            BankReconciliation bankReconciliation = bankReconciliations.get(0);
            // 如果当前流水为冲挂账，则需要改成挂账，否则为挂账或一般入账，则设置为空
            if(bankReconciliation.getEntrytype() != null && EntryType.CrushHang_Entry.getValue() == bankReconciliation.getEntrytype()){
                bankReconciliation.setEntrytype(EntryType.Hang_Entry.getValue());
                bankReconciliation.setVirtualEntryType(EntryType.Hang_Entry.getValue());
            }else{
                bankReconciliation.setEntrytype(null);
                bankReconciliation.setVirtualEntryType(null);
                bankReconciliation.setSerialdealtype(null);
                bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                bankReconciliation.setRelationstatus(null);
            }
            bankReconciliation.setSmartcheckno(smartCheckNo);
            //业务完结流程状态清空
            bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
        }
        CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs);
        YtsContext.setYtsContext("corrData", bankReconciliationbusrelation_bs);

        // 修改资金收付款单明细表关联状态
//        updateFundPaymentItemsByBankReconciliationId(bankreconciliationid);
//        updateFundCollectionItemsByBankReconciliationId(bankreconciliationid);
    }

    private void rollbackBankReconciliation(Long busid) throws Exception {
        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
        querySchema2.addCondition(group2);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
        if (bankReconciliations != null && bankReconciliations.size() > 0) {
            /*bankReconciliations.get(0).setAssociationstatus(delcorrRowStatus);*/
            Short entryType = bankReconciliations.get(0).getEntrytype();
            if (entryType != null && EntryType.CrushHang_Entry.getValue() == entryType) {
                bankReconciliations.get(0).setEntrytype(EntryType.Hang_Entry.getValue());
            } else {
                bankReconciliations.get(0).setEntrytype(null);
                bankReconciliations.get(0).setVirtualEntryType(null);
            }
            bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
            // 智能对账：回滚银行对账单的智能勾兑码
            String smartcheckno = YtsContext.getYtsContext("bank" + bankReconciliations.get(0).getId()).toString();
            bankReconciliations.get(0).setSmartcheckno(smartcheckno);
            CommonSaveUtils.updateBankReconciliation(bankReconciliations.get(0));
        }
    }


    private void updateFundCollectionItemsByBankReconciliationId(Long busid) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(busid));
        querySchema.addCondition(group);

        List<FundCollection_b> fundCollectionItems = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
        if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundCollectionItems)) {
            for (FundCollection_b item : fundCollectionItems) {
                if (AssociationStatus.Associated.getValue() == item.getAssociationStatus()) {
                    item.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                    item.setBankReconciliationId(null);
                    item.setEntityStatus(EntityStatus.Update);
                } else {
                    fundCollectionItems.remove(item);
                }
            }

            if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundCollectionItems)) {
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionItems);
            }
        }
    }

    private void updateFundCollectionItemsByBillClaimsId(Long busid) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("billClaimId").eq(busid));
        querySchema.addCondition(group);

        List<FundCollection_b> fundCollectionItems = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
        if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundCollectionItems)) {
            for (FundCollection_b item : fundCollectionItems) {
                if (AssociationStatus.Associated.getValue() == item.getAssociationStatus()) {
                    item.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                    item.setBillClaimId(null);
                    item.setEntityStatus(EntityStatus.Update);
                } else {
                    fundCollectionItems.remove(item);
                }
            }

            if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundCollectionItems)) {
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionItems);
            }
        }
    }

    private void updateFundPaymentItemsByBankReconciliationId(Long busid) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankReconciliationId").eq(busid));
        querySchema.addCondition(group);

        List<FundPayment_b> fundPaymentItems = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
        if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundPaymentItems)) {
            for (FundPayment_b item : fundPaymentItems) {
                if (AssociationStatus.Associated.getValue() == item.getAssociationStatus()) {
                    item.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                    item.setBankReconciliationId(null);
                    item.setEntityStatus(EntityStatus.Update);
                } else {
                    fundPaymentItems.remove(item);
                }
            }

            if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundPaymentItems)) {
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentItems);
            }
        }
    }


    private void updateFundPaymentItemsByBillClaimsId(Long busid) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("billClaimId").eq(String.valueOf(busid)));
        querySchema.addCondition(group);

        List<FundPayment_b> fundPaymentItems = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
        if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundPaymentItems)) {
            for (FundPayment_b item : fundPaymentItems) {
                if (AssociationStatus.Associated.getValue() == item.getAssociationStatus()) {
                    item.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                    item.setBillClaimId(null);
                    item.setEntityStatus(EntityStatus.Update);
                } else {
                    fundPaymentItems.remove(item);
                }
            }

            if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(fundPaymentItems)) {
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentItems);
            }
        }
    }

}