package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.basecom.constant.SystemConst;
import com.yonyoucloud.fi.basecom.itf.ICtmCoreRefCheckService;
import com.yonyoucloud.fi.basecom.service.dto.CoreRefCheckReqDTO;
import com.yonyoucloud.fi.basecom.service.dto.CoreRefCheckResultDTO;
import com.yonyoucloud.fi.cmp.autoorderrule.AutoorderruleConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.constant.FundConstant;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 现金管理基础档案删除引用校验实现
 */
@Service
@Slf4j
public class CtmCmpCoreRefCheckService implements ICtmCoreRefCheckService {

    private static final Map<String, String> merchantMap = new HashMap<>();
    private static final Map<String, String> merchantBankAccMap = new HashMap<>();
    private static final Map<String, String> vendorMap = new HashMap<>();
    private static final Map<String, String> vendorBankAccMap = new HashMap<>();

    static {
        merchantMap.put(FundPayment_b.ENTITY_NAME, "oppositeobjectid");
        merchantMap.put(FundCollection_b.ENTITY_NAME, "oppositeobjectid");
        merchantMap.put(BankReconciliation.ENTITY_NAME, "oppositeobjectid");
        merchantMap.put(AutoorderruleConfig.ENTITY_NAME, "customer");
        merchantMap.put(PayApplicationBill.ENTITY_NAME, "customer");
        merchantMap.put(PayMargin.ENTITY_NAME, "customer");
        merchantMap.put(ReceiveMargin.ENTITY_NAME, "customer");

        merchantBankAccMap.put(FundPayment_b.ENTITY_NAME, "oppositeaccountid");
        merchantBankAccMap.put(FundCollection_b.ENTITY_NAME, "oppositeaccountid");
        merchantBankAccMap.put(PayApplicationBill.ENTITY_NAME, "customerbankaccount");
        merchantBankAccMap.put(PayMargin.ENTITY_NAME, "customerbankaccount");
        merchantBankAccMap.put(ReceiveMargin.ENTITY_NAME, "customerbankaccount");

        vendorMap.put(FundPayment_b.ENTITY_NAME, "oppositeobjectid");
        vendorMap.put(FundCollection_b.ENTITY_NAME, "oppositeobjectid");
        vendorMap.put(BankReconciliation.ENTITY_NAME, "oppositeobjectid");
        vendorMap.put(AutoorderruleConfig.ENTITY_NAME, "supplier");
        vendorMap.put(PayApplicationBill.ENTITY_NAME, "supplier");
        vendorMap.put(PayMargin.ENTITY_NAME, "supplier");
        vendorMap.put(ReceiveMargin.ENTITY_NAME, "supplier");

        vendorBankAccMap.put(FundPayment_b.ENTITY_NAME, "oppositeaccountid");
        vendorBankAccMap.put(FundCollection_b.ENTITY_NAME, "oppositeaccountid");
        vendorBankAccMap.put(PayApplicationBill.ENTITY_NAME, "supplierbankaccount");
        vendorBankAccMap.put(PayMargin.ENTITY_NAME, "supplierbankaccount");
        vendorBankAccMap.put(ReceiveMargin.ENTITY_NAME, "supplierbankaccount");

    }

    @Override
    public CoreRefCheckResultDTO hasMerchantCheckData(CoreRefCheckReqDTO request) {
        return refCheckProcess(request, merchantMap);
    }

    @Override
    public CoreRefCheckResultDTO hasMerchantBankAccCheckData(CoreRefCheckReqDTO request) {
        return refCheckProcess(request, merchantBankAccMap);
    }

    @Override
    public CoreRefCheckResultDTO hasVendorCheckData(CoreRefCheckReqDTO request) {
        return refCheckProcess(request, vendorMap);
    }

    @Override
    public CoreRefCheckResultDTO hasVendorBankAccCheckData(CoreRefCheckReqDTO request) {
        return refCheckProcess(request, vendorBankAccMap);
    }

    /**
     * 校验服务名称
     * @param fullName 实体名称
     * @return
     */
    private String resServiceNameByFullName(String fullName) {
        if (FundPayment_b.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800A8",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C0", "资金付款单") /* "资金付款单" */);
        } else if (FundCollection_b.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800A7",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C1", "资金收款单") /* "资金收款单" */);
        } else if (BankReconciliation.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800A6",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C2", "银行对账单") /* "银行对账单" */);
        } else if (AutoorderruleConfig.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108D8404B803BA",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C3", "自动生单规则") /* "自动生单规则" */);
        } else if (PayApplicationBill.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041800B5",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C4", "付款申请单") /* "付款申请单" */);
        } else if (PayMargin.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18C488C205A80030",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C5", "支付保证金台账管理") /* "支付保证金台账管理" */);
        } else if (ReceiveMargin.ENTITY_NAME.equals(fullName)) {
            return InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18C488C205A80033",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004BE", "收到保证金台账管理") /* "收到保证金台账管理" */);
        }
        return InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE975C041801C6",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004BF", "现金管理") /* "现金管理" */);
    }

    /**
     * 校验处理
     * @param request
     * @param checkData
     * @return
     */
    private CoreRefCheckResultDTO refCheckProcess(CoreRefCheckReqDTO request, Map<String, String> checkData) {
        CoreRefCheckResultDTO resultDTO = new CoreRefCheckResultDTO();
        for (String fullName : checkData.keySet()){
            if (buildQuery(request, fullName, checkData.get(fullName))) {
                resultDTO.setRefCheckResult(false);
                resultDTO.setMessage(resServiceNameByFullName(fullName));
                break;
            }
        }
        return resultDTO;
    }

    public boolean buildQuery(CoreRefCheckReqDTO request, String fullName, String field) {
        try {
            List<String> ids = request.getIds();
            // 财务新架构，不查询付款申请单
            if (fullName.equals(PayApplicationBill.ENTITY_NAME) && InvocationInfoProxy.getNewFi()) {
                return false;
            }
            QuerySchema query = QuerySchema.create().addSelect("id");
            QueryConditionGroup queryCondition = QueryConditionGroup.and(QueryCondition.name(field).in(ids));
            // 这个参数是档案删除（aa.vendor.Vendor）校验独有的场景，此时ids只有一个，但是对应多个使用组织的删除校验。其他情况下这个字段应该是没有值的。
            if (SystemConst.ENTITY_NAME_VENDOR.equals(request.getFullname()) && CollectionUtils.isNotEmpty(request.getOrgIds())) {
                appendCondition(request.getOrgIds(), queryCondition, fullName);
            }
            // 客户档案使用组织
            if (SystemConst.ENTITY_NAME_MERCHANT.equals(request.getFullname()) && StringUtils.isNotEmpty(request.getOrgId())) {
                List<String> orgIds = new ArrayList<>();
                orgIds.add(request.getOrgId());
                appendCondition(orgIds, queryCondition, fullName);
            }
            // 针对结算简强后的单据添加ebiz_obj_type条件
            if(FundPayment_b.ENTITY_NAME.equals(fullName) ){
                queryCondition.addCondition(QueryCondition.name(FundConstant.SUB_EBIZOBJTYPE).eq(FundPayment_b.ENTITY_NAME));
            }
            if(FundCollection_b.ENTITY_NAME.equals(fullName)){
                queryCondition.addCondition(QueryCondition.name(FundConstant.SUB_EBIZOBJTYPE).eq(FundCollection_b.ENTITY_NAME));
            }
            List<Map> resList = MetaDaoHelper.query(fullName, query.addCondition(queryCondition));
            if (resList != null && resList.size() > 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return true;
    }

    private void appendCondition (List<String> orgIds, QueryConditionGroup queryCondition, String fullName) {
        if (BankReconciliation.ENTITY_NAME.equals(fullName) || PayMargin.ENTITY_NAME.equals(fullName) || ReceiveMargin.ENTITY_NAME.equals(fullName)) {
            queryCondition.addCondition(QueryCondition.name(ICmpConstant.ACCENTITY).in(orgIds));
        } else if (FundPayment_b.ENTITY_NAME.equals(fullName) || FundCollection_b.ENTITY_NAME.equals(fullName)) {
            // 处理核算委托关系的情况，即B委托给A，存在单据会计主体A，供应商使用组织为B；可以删除使用组织为A的数据
            QueryConditionGroup queryCondition1 = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.MAINID_ACCENTITY).in(orgIds));
            QueryConditionGroup orgConditionOr = QueryConditionGroup.or(QueryCondition.name(ICmpConstant.MAINID_ORG).in(orgIds),
                    QueryCondition.name(ICmpConstant.MAINID_ORG).is_null());
            queryCondition1.addCondition(orgConditionOr);

            QueryConditionGroup queryCondition2 = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.MAINID_ACCENTITY).not_in(orgIds),
                    QueryCondition.name(ICmpConstant.MAINID_ORG).in(orgIds));

            queryCondition.addCondition(QueryConditionGroup.or(queryCondition1, queryCondition2));
        } else if (PayApplicationBill.ENTITY_NAME.equals(fullName)) {
            QueryConditionGroup queryConditionOr = QueryConditionGroup.or(QueryCondition.name(ICmpConstant.ACCENTITY).in(orgIds),
                    QueryCondition.name(ICmpConstant.ORG).in(orgIds));
            queryCondition.addCondition(queryConditionOr);
        }
    }
}
