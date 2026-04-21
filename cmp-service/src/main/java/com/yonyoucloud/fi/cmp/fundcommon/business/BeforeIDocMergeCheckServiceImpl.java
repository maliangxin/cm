package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.google.gson.JsonObject;
import com.yonyou.iuap.upc.merge.model.*;
import com.yonyou.iuap.upc.merge.service.IDocMergeCheckService;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.autoorderrule.Autoorderrule;
import com.yonyoucloud.fi.cmp.autoorderrule.AutoorderruleConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户或供应商合并检查接口，根据类型
 */

@Slf4j
@Service
@Primary
public class BeforeIDocMergeCheckServiceImpl implements IDocMergeCheckService {


    /**
     * @param docType     合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @return
     */
    @Override
    public List<MergeResult> checkBeforeMerge(MergeDocTypeEnum docType, String sourceDocId) {
        List<MergeResult> mergeResultList = new ArrayList<>();
        //资金收款单
        getFundCollectionB(mergeResultList, docType, sourceDocId);
        //资金付款单
        getFundPaymentB(mergeResultList, docType, sourceDocId);
        //银行流水认领
        getBankReconciliation(mergeResultList, docType, sourceDocId);
        //我的认领
        getBillClaim(mergeResultList, docType, sourceDocId);
        //外汇付款
        getForeignPayment(mergeResultList, docType, sourceDocId);
        //支付保证金业务台账
        getPayMargin(mergeResultList, docType, sourceDocId);
        //收到保证金业务台账
        getReceiveMargin(mergeResultList, docType, sourceDocId);
        //自动生单规则
        getAutoorderrule(mergeResultList, docType, sourceDocId);
        return mergeResultList;
    }

    @Override
    public List<BillInfo> getExportToDoItem(MergeDocTypeEnum docType, String sourceDocId, MergeResult mergeResult) {
        List<BillInfo> mergeResultList = new ArrayList<>();
        //资金收款单
        getFundCollectionBExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //资金付款单
        getFundPaymentBExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //银行流水认领
        getBankReconciliationExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //我的认领
        getBillClaimExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //外汇付款
        getForeignPaymentExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //支付保证金业务台账
        getPayMarginExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //收到保证金业务台账
        getReceiveMarginExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        //自动生单规则
        getAutoorderruleExportToDoItem(mergeResultList, docType, sourceDocId, mergeResult);
        return mergeResultList;
    }


    /**
     * （资金收款单）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getFundCollectionB(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //设置主表-审批状态：“初始开立、审批中、驳回到制单”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};

        try {
            //第一步，查询主表,获取收款单
            QuerySchema schema = QuerySchema.create().addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("verifystate").in(verifystates));
            schema.addCondition(conditionGroup);
            List<FundCollection> fundCollections = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, schema, null);
            //第二步判断主表是否存在，存在则根据主表ID进行匹配子表
            List<FundCollection_b> fundCollectionList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(fundCollections)) {
                List<Object> mainids = fundCollections.stream().map(p -> p.getId()).collect(Collectors.toList());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("id");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").in(mainids));
                group1.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
                querySchema1.addCondition(group1);
                //获取复核要求的收款单的子表对象
                fundCollectionList.addAll(MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema1, null));
            }
            //无论主表存不存在数据都会走
            QuerySchema querySchema2 = QuerySchema.create().addSelect("id");
            QueryConditionGroup group2 = new QueryConditionGroup(ConditionOperator.and);
            group2.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
            group2.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            querySchema2.addCondition(group2);
            fundCollectionList.addAll(MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema2, null));
            //无论上述查询结果是否存在，
            MergeResult mergeResult = getMergeResult();
            if (!CollectionUtils.isEmpty(fundCollectionList)) {
                //去重
                Map<Long, FundCollection_b> fundCollectionbMap = fundCollectionList.stream().collect(Collectors.toMap(FundCollection_b::getId, fundCollection_b -> fundCollection_b, (k1, k2) -> k1));
                mergeResult.setStatus(CheckStatusEnum.hasAgent);
                mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064D", "请手工处理完成在途的资金收款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的资金收款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */ + fundCollectionbMap.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064E", "条在途的资金收款数据") /* "条在途的资金收款数据" */);
            } else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064F", "存在0条在途的资金收款数据") /* "存在0条在途的资金收款数据" */);
            }
            mergeResult.setServiceCode("ficmp0024");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400652", "资金收款") /* "资金收款" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064B", "未完成状态：") /* "未完成状态：" */ +
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064C", "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”") /* "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("资金收款单的客户或供应商合并检查接口异常：", e);
        }
    }


    /**
     * （资金付款单）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getFundPaymentB(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //设置主表-审批状态：“初始开立、审批中、驳回到制单”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        //第一步，查询主表
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        schema.addCondition(conditionGroup);
        try {
            //第二步获取付款单
            List<FundPayment> fundPayments = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, schema, null);
            List<FundPayment_b> fundPaymentBList = new ArrayList<>();
            if (null != fundPayments && fundPayments.size() > 0) {
                List<Object> ids = fundPayments.stream().map(p -> p.getId()).collect(Collectors.toList());
                QuerySchema querySchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").in(ids));
                group.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
                querySchema.addCondition(group);
                //获取复核要求的收款单的子表对象
                fundPaymentBList.addAll(MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null));
            }
            //无论主表存不存在数据都会走
            QuerySchema querySchema2 = QuerySchema.create().addSelect("id");
            QueryConditionGroup group2 = new QueryConditionGroup(ConditionOperator.and);
            group2.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
            group2.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            querySchema2.addCondition(group2);
            fundPaymentBList.addAll(MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema2, null));
            MergeResult mergeResult = getMergeResult();
            if (!CollectionUtils.isEmpty(fundPaymentBList)) {
                //去重
                Map<Long, FundPayment_b> fundPayment_bMap = fundPaymentBList.stream().collect(Collectors.toMap(FundPayment_b::getId, fundPayment_b -> fundPayment_b, (k1, k2) -> k1));
                mergeResult.setStatus(CheckStatusEnum.hasAgent);
                mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400660", "请手工处理完成在途的资金付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的资金付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */ + fundPayment_bMap.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400661", "条在途的资金付款数据") /* "条在途的资金付款数据" */);
            } else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400662", "存在0条在途的资金付款数据") /* "存在0条在途的资金付款数据" */);
            }
            mergeResult.setServiceCode("ficmp0026");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400664", "资金付款") /* "资金付款" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064B", "未完成状态：") /* "未完成状态：" */ +
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064C", "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”") /* "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("资金付款单的客户或供应商合并检查接口异常：", e);
        }
    }


    /**
     * （银行流水认领）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getBankReconciliation(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //未完成状态：业务关联状态为“未关联”
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        querySchema.addCondition(group);
        try {
            group.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
            querySchema.addCondition(group);
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            MergeResult mergeResult = getMergeResult();
            if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                mergeResult.setStatus(CheckStatusEnum.hasAgent);
                mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400659", "请手工处理完成在途的银行流水数据，业务关联状态包含“未关联”") /* "请手工处理完成在途的银行流水数据，业务关联状态包含“未关联”" */);
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */ + bankReconciliationList.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400669", "条在途的银行流水认领数据") /* "条在途的银行流水认领数据" */);
            } else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400662", "存在0条在途的资金付款数据") /* "存在0条在途的资金付款数据" */);
            }
            mergeResult.setServiceCode("ficmp0006");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540066C", "银行流水认领") /* "银行流水认领" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540066D", "未完成状态：业务关联状态为“未关联”") /* "未完成状态：业务关联状态为“未关联”" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("银行流水认领的客户或供应商合并检查接口异常：", e);
        }
    }


    /**
     * （我的认领）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getBillClaim(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //未完成状态：认领完结状态为“未完结”
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("associationstatus").eq(ClaimCompleteStatus.Uncompleted.getValue()));
        querySchema.addCondition(group);
        try {
            MergeResult mergeResult = getMergeResult();
            List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
            if (!CollectionUtils.isEmpty(billClaimList)) {
                List<Object> ids = billClaimList.stream().map(BillClaim::getId).collect(Collectors.toList());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("id");
                QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
                group1.appendCondition(QueryCondition.name("mainid").in(ids));
                group1.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
                querySchema1.addCondition(group1);
                List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);
                if (!CollectionUtils.isEmpty(billClaimItems)){
                    mergeResult.setStatus(CheckStatusEnum.hasAgent);
                    mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400650", "请手工处理完成在途的认领数据，认领完结状态包含“未完结”") /* "请手工处理完成在途的认领数据，认领完结状态包含“未完结”" */);
                    mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */ + billClaimItems.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400651", "条在途的我的认领数据") /* "条在途的我的认领数据" */);
                }else {
                    mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400653", "存在0条在途的我的认领数据") /* "存在0条在途的我的认领数据" */);
                }
            }else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400653", "存在0条在途的我的认领数据") /* "存在0条在途的我的认领数据" */);
            }
            mergeResult.setServiceCode("ficmp0034");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400654", "我的认领") /* "我的认领" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400656", "未完成状态：认领完结状态为“未完结”") /* "未完成状态：认领完结状态为“未完结”" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("我的认领的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     * （外汇付款）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getForeignPayment(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //未完成状态：业务关联状态为“未关联”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        List<ForeignPayment> foreignPaymentList =new ArrayList<>();
        try {
            if (MergeDocTypeEnum.Merchant==docType) {
                group.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102274"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DA", "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema.addCondition(group);
            foreignPaymentList.addAll(MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema, null));

            QuerySchema querySchema1 = QuerySchema.create().addSelect("id");
            QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
            group1.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            if (MergeDocTypeEnum.Merchant==docType) {
                group1.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group1.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102274"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DA", "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema1.addCondition(group1);
            foreignPaymentList.addAll(MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema1, null));
            MergeResult mergeResult = getMergeResult();
            if (!CollectionUtils.isEmpty(foreignPaymentList)) {
                //去重
                Map<Long, ForeignPayment> payMarginMap = foreignPaymentList.stream().collect(Collectors.toMap(ForeignPayment::getId, foreignPayment -> foreignPayment, (k1, k2) -> k1));
                mergeResult.setStatus(CheckStatusEnum.hasAgent);
                mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400665", "请手工处理完成在途的外汇付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的外汇付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */+foreignPaymentList.size()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400666", "条在途的外汇付款数据") /* "条在途的外汇付款数据" */);
            } else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400667", "存在0条在途的外汇付款数据") /* "存在0条在途的外汇付款数据" */);
            }
            mergeResult.setServiceCode("cmp_foreignpayment");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400668", "外汇付款") /* "外汇付款" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064B", "未完成状态：") /* "未完成状态：" */ +
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064C", "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”") /* "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("外汇付款的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     * （支付保证金业务台账）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getPayMargin(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //未完成状态：业务关联状态为“未关联”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        List<PayMargin> payMarginList = new ArrayList<>();
        try {
            if (MergeDocTypeEnum.Merchant==docType) {
                group.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema.addCondition(group);
            payMarginList.addAll(MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null));
            QuerySchema querySchema1 = QuerySchema.create().addSelect("id");
            QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
            group1.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            if (MergeDocTypeEnum.Merchant==docType) {
                group1.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group1.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema1.addCondition(group1);
            payMarginList.addAll(MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema1, null));
            MergeResult mergeResult = getMergeResult();
            if (!CollectionUtils.isEmpty(payMarginList)) {
                //去重
                Map<Long, PayMargin> payMarginMap = payMarginList.stream().collect(Collectors.toMap(PayMargin::getId, payMargin -> payMargin, (k1, k2) -> k1));
                mergeResult.setStatus(CheckStatusEnum.hasAgent);
                mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400646", "请手工处理完成在途的支付保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的支付保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */+payMarginMap.size()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400647", "条在途的支付保证金数据") /* "条在途的支付保证金数据" */);
            } else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400649", "存在0条在途的支付保证金数") /* "存在0条在途的支付保证金数" */);
            }
            mergeResult.setServiceCode("cmp_paymargin");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064A", "支付保证金") /* "支付保证金" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064B", "未完成状态：") /* "未完成状态：" */ +
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064C", "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”") /* "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("支付保证金业务台账的客户或供应商合并检查接口异常：", e);
        }
    }


    /**
     * （收到保证金业务台账）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getReceiveMargin(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //未完成状态：业务关联状态为“未关联”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        List<ReceiveMargin> receiveMarginList = new ArrayList<>();
        try{
            if (MergeDocTypeEnum.Merchant==docType) {
                group.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema.addCondition(group);
            receiveMarginList.addAll(MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null));
            QuerySchema querySchema1 = QuerySchema.create().addSelect("id");
            QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
            group1.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            if (MergeDocTypeEnum.Merchant==docType) {
                group1.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group1.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema1.addCondition(group1);
            receiveMarginList.addAll(MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema1, null));
            MergeResult mergeResult = getMergeResult();
            if (!CollectionUtils.isEmpty(receiveMarginList)) {
                //去重
                Map<Long, ReceiveMargin> payMarginMap = receiveMarginList.stream().collect(Collectors.toMap(ReceiveMargin::getId, receiveMargin -> receiveMargin, (k1, k2) -> k1));
                mergeResult.setStatus(CheckStatusEnum.hasAgent);
                mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065C", "请手工处理完成在途的收到保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的收到保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */+payMarginMap.size()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065D", "条在途的收到保证金数据") /* "条在途的收到保证金数据" */);
            } else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065E", "存在0条在途的支付保证金数据") /* "存在0条在途的支付保证金数据" */);
            }
            mergeResult.setServiceCode("cmp_receivemargin");
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065F", "收到保证金") /* "收到保证金" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064B", "未完成状态：") /* "未完成状态：" */ +
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064C", "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”") /* "审批状态：“初始开立、审批中、驳回到制单”或结算状态：“结算中”" */);
            mergeResultList.add(mergeResult);
        }catch (Exception e) {
            log.error("收到保证金业务台账的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     * （自动生单规则）
     *
     * @param mergeResultList 返回的结果集
     * @param docType         合并档案类型的枚举，客户、供应商
     * @param sourceDocId     被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     */
    private void getAutoorderrule(List<MergeResult> mergeResultList, MergeDocTypeEnum docType, String sourceDocId) {
        //启停状态为启用，并引用了客商检查的数据
        MergeResult mergeResult = getMergeResult();
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("isEnable").eq(IsEnable.ENABLE.getValue()));
        try {
            querySchema.addCondition(group);
            List<Autoorderrule> autoorderrules = MetaDaoHelper.queryObject(Autoorderrule.ENTITY_NAME, querySchema, null);
            if (!CollectionUtils.isEmpty(autoorderrules)){
                List<Object> ids = autoorderrules.stream().map(Autoorderrule::getId).collect(Collectors.toList());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("id");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.addCondition(QueryCondition.name("mainid").in(ids));
                if (MergeDocTypeEnum.Merchant==docType) {
                    conditionGroup.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
                } else if (MergeDocTypeEnum.Vendor==docType) {
                    conditionGroup.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102276"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DB", "自动生单规则的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "自动生单规则的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
                }
                querySchema1.addCondition(conditionGroup);
                List<AutoorderruleConfig> autoorderruleConfigList = MetaDaoHelper.queryObject(AutoorderruleConfig.ENTITY_NAME, querySchema1, null);
                if (!CollectionUtils.isEmpty(autoorderruleConfigList)) {
                    mergeResult.setStatus(CheckStatusEnum.hasAgent);
                    mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400657", "存在检查的客商数据设置了自动生单规则，，请手工处理") /* "存在检查的客商数据设置了自动生单规则，，请手工处理" */);
                    mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */+autoorderruleConfigList.size()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065A", "条数据，引用了检查的客商数据") /* "条数据，引用了检查的客商数据" */);
                }else {
                    mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540066B", "存在0条数据，引用了检查的客商数据") /* "存在0条数据，引用了检查的客商数据" */);
                }
            }else {
                mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065E", "存在0条在途的支付保证金数据") /* "存在0条在途的支付保证金数据" */);
            }
            mergeResult.setServiceCode("cmp_autoorderrule_list");
            mergeResult.setCheckTypeEnum(CheckTypeEnum.other);
            mergeResult.setCheckItem(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540066E", "自动生单规则") /* "自动生单规则" */);
            mergeResult.setCheckStandard(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540066F", "启停状态为启用，并引用了客商检查的数据") /* "启停状态为启用，并引用了客商检查的数据" */);
            mergeResultList.add(mergeResult);
        } catch (Exception e) {
            log.error("自动生单规则的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     *  资金收款
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getFundCollectionBExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult){
        if (!"ficmp0024".equals(mergeResult.getServiceCode())){
            return;
        }
        //设置主表-审批状态：“初始开立、审批中、驳回到制单”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};

        try {
            //第一步，查询主表,获取收款单
            QuerySchema schema = QuerySchema.create().addSelect("id,code");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("verifystate").in(verifystates));
            schema.addCondition(conditionGroup);
            List<FundCollection> fundCollections = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, schema, null);
            //第二步判断主表是否存在，存在则根据主表ID进行匹配子表
            List<FundCollection_b> fundCollectionList = new ArrayList<>();
            Map<Long, String> fundCodeList;
            if (!CollectionUtils.isEmpty(fundCollections)) {
                fundCodeList = fundCollections.stream().collect(Collectors.toMap(FundCollection::getId,fundCollection->fundCollection.getCode(),(k1,k2)->k1));
                List<Object> mainids = fundCollections.stream().map(p -> p.getId()).collect(Collectors.toList());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("id,mainid");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("mainid").in(mainids));
                group1.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
                querySchema1.addCondition(group1);
                //获取复核要求的收款单的子表对象
                fundCollectionList.addAll(MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema1, null));
            } else {
                fundCodeList = new HashMap<>();
            }
            //无论主表存不存在数据都会走
            QuerySchema querySchema2 = QuerySchema.create().addSelect("id,mainid");
            QueryConditionGroup group2 = new QueryConditionGroup(ConditionOperator.and);
            group2.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
            group2.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            querySchema2.addCondition(group2);
            fundCollectionList.addAll(MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema2, null));
            //无论上述查询结果是否存在，
            if (!CollectionUtils.isEmpty(fundCollectionList)) {
                //去重
                String[] mainIds = new String[fundCollectionList.size()];
                for (int i=0;i<fundCollectionList.size();i++){
                    mainIds[i]=String.valueOf(fundCollectionList.get(i).getMainid());
                }
                QuerySchema schemaNew = QuerySchema.create().addSelect("id,code");
                QueryConditionGroup conditionGroupNew = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupNew.appendCondition(QueryCondition.name("id").in(mainIds));
                schemaNew.addCondition(conditionGroupNew);
                List<FundCollection> fundCollectionsNew = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, schemaNew, null);
                if (fundCollectionsNew !=null && fundCollectionsNew.size()>0){
                    fundCollectionsNew.stream().forEach(p -> {
                        BillInfo billInfo = new BillInfo();
                        billInfo.setCode(p.getCode());
                        billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400652", "资金收款") /* "资金收款" */);
                        billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540064D", "请手工处理完成在途的资金收款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的资金收款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                        mergeResultList.add(billInfo);
                    });
                }
            }
        } catch (Exception e) {
            log.error("资金收款单的客户或供应商合并检查接口异常：", e);
        }
    }
    /**
     *  资金付款
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getFundPaymentBExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult){
        if (!"ficmp0026".equals(mergeResult.getServiceCode())){
            return;
        }
        //设置主表-审批状态：“初始开立、审批中、驳回到制单”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        //第一步，查询主表
        QuerySchema schema = QuerySchema.create().addSelect("id,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        schema.addCondition(conditionGroup);

        try {
            //第二步获取付款单
            List<FundPayment> fundPayments = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, schema, null);
            List<FundPayment_b> fundPaymentBList = new ArrayList<>();
            Map<Long, String> fundPaymentList;
            if (null != fundPayments && fundPayments.size() > 0) {
                fundPaymentList = fundPayments.stream().collect(Collectors.toMap(FundPayment::getId,p->p.getCode(),(k1,k2)->k1));
                List<Object> ids = fundPayments.stream().map(p -> p.getId()).collect(Collectors.toList());
                QuerySchema querySchema = QuerySchema.create().addSelect("id,mainid");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").in(ids));
                group.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
                querySchema.addCondition(group);
                //获取复核要求的收款单的子表对象
                fundPaymentBList.addAll(MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null));
            }else {
                fundPaymentList = new HashMap<>();
            }
            //无论主表存不存在数据都会走
            QuerySchema querySchema2 = QuerySchema.create().addSelect("id,mainid");
            QueryConditionGroup group2 = new QueryConditionGroup(ConditionOperator.and);
            group2.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
            group2.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            querySchema2.addCondition(group2);
            fundPaymentBList.addAll(MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema2, null));
            if (!CollectionUtils.isEmpty(fundPaymentBList)) {
                //去重
                //去重
                String[] mainIds = new String[fundPaymentBList.size()];
                for (int i=0;i<fundPaymentBList.size();i++){
                    mainIds[i]=String.valueOf(fundPaymentBList.get(i).getMainid());
                }
                QuerySchema schemaNew = QuerySchema.create().addSelect("id,code");
                QueryConditionGroup conditionGroupNew = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupNew.appendCondition(QueryCondition.name("id").in(mainIds));
                schemaNew.addCondition(conditionGroupNew);
                List<FundPayment> fundPaymentsNew = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, schemaNew, null);
                if (fundPaymentsNew != null && fundPaymentsNew.size()>0){
                    fundPaymentsNew.stream().forEach(p->{
                        BillInfo billInfo = new BillInfo();
                        billInfo.setCode(p.getCode());
                        billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400664", "资金付款") /* "资金付款" */);
                        billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400660", "请手工处理完成在途的资金付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的资金付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                        mergeResultList.add(billInfo);
                    });
                }
            }
        } catch (Exception e) {
            log.error("资金付款单的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     *  银行流水认领
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getBankReconciliationExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult){
        if (!"ficmp0006".equals(mergeResult.getServiceCode())){
            return;
        }
        //未完成状态：业务关联状态为“未关联”
        QuerySchema querySchema = QuerySchema.create().addSelect("id,bank_seq_no");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        querySchema.addCondition(group);
        try {
            group.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
            querySchema.addCondition(group);
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                bankReconciliationList.stream().forEach(p->{
                    BillInfo billInfo = new BillInfo();
                    billInfo.setCode(p.getBank_seq_no());
                    billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400658", "银行流水") /* "银行流水" */);
                    billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400659", "请手工处理完成在途的银行流水数据，业务关联状态包含“未关联”") /* "请手工处理完成在途的银行流水数据，业务关联状态包含“未关联”" */);
                    mergeResultList.add(billInfo);
                });
            }
        } catch (Exception e) {
            log.error("银行流水认领的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     *  我的认领单
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getBillClaimExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult){
        if (!"ficmp0034".equals(mergeResult.getServiceCode())){
            return;
        }
        //未完成状态：认领完结状态为“未完结”
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("associationstatus").eq(ClaimCompleteStatus.Uncompleted.getValue()));
        querySchema.addCondition(group);
        try {
            List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
            Map<Long, String> fundPaymentList = new HashMap<>();
            if (!CollectionUtils.isEmpty(billClaimList)) {
                fundPaymentList = billClaimList.stream().collect(Collectors.toMap(BillClaim::getId,billClaim->billClaim.getCode(),(k1,k2)->k1));
                List<Object> ids = billClaimList.stream().map(BillClaim::getId).collect(Collectors.toList());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("id,mainid");
                QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
                group1.appendCondition(QueryCondition.name("mainid").in(ids));
                group1.appendCondition(QueryCondition.name("oppositeobjectid").eq(sourceDocId));
                querySchema1.addCondition(group1);
                List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema1, null);
                if (!CollectionUtils.isEmpty(billClaimItems)){
                    Map<Long, String> finalFundPaymentList = fundPaymentList;
                    billClaimItems.stream().forEach(p->{
                        BillInfo billInfo = new BillInfo();
                        billInfo.setCode(finalFundPaymentList.get(p.getMainid()));
                        billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400663", "我的认领单") /* "我的认领单" */);
                        billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400650", "请手工处理完成在途的认领数据，认领完结状态包含“未完结”") /* "请手工处理完成在途的认领数据，认领完结状态包含“未完结”" */);
                        mergeResultList.add(billInfo);
                    });
                }
            }
        } catch (Exception e) {
            log.error("我的认领的客户或供应商合并检查接口异常：", e);
        }
    }
    /**
     *  外汇付款
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getForeignPaymentExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult){
        if (!"cmp_foreignpayment".equals(mergeResult.getServiceCode())){
            return;
        }
        //未完成状态：业务关联状态为“未关联”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        List<ForeignPayment> foreignPaymentList =new ArrayList<>();
        try {
            if (MergeDocTypeEnum.Merchant==docType) {
                group.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102274"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DA", "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema.addCondition(group);
            foreignPaymentList.addAll(MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema, null));

            QuerySchema querySchema1 = QuerySchema.create().addSelect("id,code");
            QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
            group1.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            if (MergeDocTypeEnum.Merchant==docType) {
                group1.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group1.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102274"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DA", "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "外汇付款的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema1.addCondition(group1);
            foreignPaymentList.addAll(MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema1, null));
            if (!CollectionUtils.isEmpty(foreignPaymentList)) {
                //去重
                foreignPaymentList.stream().forEach(p->{
                    BillInfo billInfo = new BillInfo();
                    billInfo.setCode(p.getCode());
                    billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400668", "外汇付款") /* "外汇付款" */);
                    billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400665", "请手工处理完成在途的外汇付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的外汇付款单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                    mergeResultList.add(billInfo);
                });

            }
        } catch (Exception e) {
            log.error("外汇付款的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     *  支付保证金业务台账
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getPayMarginExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult){
        if (!"cmp_paymargin".equals(mergeResult.getServiceCode())){
            return;
        }
        //未完成状态：业务关联状态为“未关联”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        List<PayMargin> payMarginList = new ArrayList<>();
        try {
            if (MergeDocTypeEnum.Merchant==docType) {
                group.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema.addCondition(group);
            payMarginList.addAll(MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null));
            QuerySchema querySchema1 = QuerySchema.create().addSelect("id,code");
            QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
            group1.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            if (MergeDocTypeEnum.Merchant==docType) {
                group1.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor==docType) {
                group1.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema1.addCondition(group1);
            payMarginList.addAll(MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema1, null));
            if (!CollectionUtils.isEmpty(payMarginList)) {
                //去重
                payMarginList.stream().forEach(p->{
                    BillInfo billInfo = new BillInfo();
                    billInfo.setCode(p.getCode());
                    billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065B", "支付保证金业务台账") /* "支付保证金业务台账" */);
                    billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400646", "请手工处理完成在途的支付保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的支付保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                    mergeResultList.add(billInfo);
                });
            }
        } catch (Exception e) {
            log.error("支付保证金业务台账的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     *  收到保证金业务台账
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getReceiveMarginExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult) {
        if (!"cmp_receivemargin".equals(mergeResult.getServiceCode())) {
            return;
        }
        //未完成状态：业务关联状态为“未关联”
        short[] verifystates = new short[]{VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.SUBMITED.getValue(), VerifyState.REJECTED_TO_MAKEBILL.getValue()};
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.appendCondition(QueryCondition.name("verifystate").in(verifystates));
        List<ReceiveMargin> receiveMarginList = new ArrayList<>();
        try {
            if (MergeDocTypeEnum.Merchant == docType) {
                group.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor == docType) {
                group.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema.addCondition(group);
            receiveMarginList.addAll(MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null));
            QuerySchema querySchema1 = QuerySchema.create().addSelect("id,code");
            QueryConditionGroup group1 = new QueryConditionGroup(ConditionOperator.and);
            group1.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.SettleProssing.getValue()));
            if (MergeDocTypeEnum.Merchant == docType) {
                group1.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
            } else if (MergeDocTypeEnum.Vendor == docType) {
                group1.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102275"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D9", "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "支付保证金业务台账的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
            }
            querySchema1.addCondition(group1);
            receiveMarginList.addAll(MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema1, null));
            if (!CollectionUtils.isEmpty(receiveMarginList)) {
                //去重
                receiveMarginList.stream().forEach(p->{
                    BillInfo billInfo = new BillInfo();
                    billInfo.setCode(p.getCode());
                    billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540066A", "收到保证金业务台账") /* "收到保证金业务台账" */);
                    billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065C", "请手工处理完成在途的收到保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”") /* "请手工处理完成在途的收到保证金业务单据，审批状态包含“初始开立、审批中、驳回到制单”或结算状态为“结算中”" */);
                    mergeResultList.add(billInfo);
                });
            }
        } catch (Exception e) {
        log.error("收到保证金业务台账的客户或供应商合并检查接口异常：", e);
    }
    }

    /**
     *  自动生单导出待办
     * @param mergeResultList 返回的结果集
     * @param docType 合并档案类型的枚举，客户、供应商
     * @param sourceDocId 被合并的档案ID，可能是客户档案ID，也可能是供应商档案ID，目前合并的处理规则是停用数据，且将原档案的子表数据合并到目标档案上
     * @param mergeResult 检查项具体信息，里面有服务编码，只需要查询该服务下数据信息即可
     */
    private void getAutoorderruleExportToDoItem(List<BillInfo> mergeResultList, MergeDocTypeEnum docType, String sourceDocId,MergeResult mergeResult) {
        if (!"cmp_autoorderrule_list".equals(mergeResult.getServiceCode())) {
            return;
        }
        //启停状态为启用，并引用了客商检查的数据
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("isEnable").eq(IsEnable.ENABLE.getValue()));
        try {
            querySchema.addCondition(group);
            List<Autoorderrule> autoorderrules = MetaDaoHelper.queryObject(Autoorderrule.ENTITY_NAME, querySchema, null);
            if (!CollectionUtils.isEmpty(autoorderrules)){
                List<Object> ids = autoorderrules.stream().map(Autoorderrule::getId).collect(Collectors.toList());
                QuerySchema querySchema1 = QuerySchema.create().addSelect("id,oppositeCode");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.addCondition(QueryCondition.name("mainid").in(ids));
                if (MergeDocTypeEnum.Merchant==docType) {
                    conditionGroup.appendCondition(QueryCondition.name("customer").eq(sourceDocId));
                } else if (MergeDocTypeEnum.Vendor==docType) {
                    conditionGroup.appendCondition(QueryCondition.name("supplier").eq(sourceDocId));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102276"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DB", "自动生单规则的客户或供应商合并检查接口的档案ID不存在，请检查!") /* "自动生单规则的客户或供应商合并检查接口的档案ID不存在，请检查!" */);
                }
                querySchema1.addCondition(conditionGroup);
                List<AutoorderruleConfig> autoorderruleConfigList = MetaDaoHelper.queryObject(AutoorderruleConfig.ENTITY_NAME, querySchema1, null);
                if (!CollectionUtils.isEmpty(autoorderruleConfigList)) {
                    autoorderruleConfigList.stream().forEach(p->{
                        BillInfo billInfo = new BillInfo();
                        billInfo.setCode(p.getOppositeCode());
                        billInfo.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400655", "自动生单") /* "自动生单" */);
                        billInfo.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400657", "存在检查的客商数据设置了自动生单规则，，请手工处理") /* "存在检查的客商数据设置了自动生单规则，，请手工处理" */);
                        mergeResultList.add(billInfo);
                    });

                    mergeResult.setStatus(CheckStatusEnum.hasAgent);
                    mergeResult.setSuggestion(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400657", "存在检查的客商数据设置了自动生单规则，，请手工处理") /* "存在检查的客商数据设置了自动生单规则，，请手工处理" */);
                    mergeResult.setResult(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400648", "存在") /* "存在" */+autoorderruleConfigList.size()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540065A", "条数据，引用了检查的客商数据") /* "条数据，引用了检查的客商数据" */);
                }
            }
        } catch (Exception e) {
            log.error("自动生单规则的客户或供应商合并检查接口异常：", e);
        }
    }

    /**
     * 抽取共享的返回值配置，可以覆盖进行修改
     *yonbip-fi-ctmcmp
     * @return
     */
    private MergeResult getMergeResult() {
        MergeResult mergeResult = new MergeResult();
        mergeResult.setServiceCode("yonbip-fi-ctmcmp");
        mergeResult.setCheckTypeEnum(CheckTypeEnum.inTransit);
        mergeResult.setStatus(CheckStatusEnum.noAgent);
        return mergeResult;
    }
}
