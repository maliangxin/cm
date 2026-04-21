package com.yonyoucloud.fi.cmp.migrade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.iuap.tenant.sdk.TenantCenter;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.payapplicationbill.ApprovalStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.vo.migrade.CmpPreCheckDetailVO;
import com.yonyoucloud.fi.cmp.vo.migrade.CmpPreCheckReqVO;
import com.yonyoucloud.fi.cmp.vo.migrade.MigradeCheckResultEnum;
import com.yonyoucloud.fi.cmp.vo.migrade.MigradeCheckTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import lombok.NonNull;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class CmpNewFiPreCheckServiceImpl implements CmpNewFiPreCheckService {

    private static final String RepairPlan_AuditAndUnSettle = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053D", "当前单据审核状态已审核，结算状态未结算，建议实施联系客户执行线下支付或者网银支付处理。") /* "当前单据审核状态已审核，结算状态未结算，建议实施联系客户执行线下支付或者网银支付处理。" */;
    private static final String RepairPlan_AuditAndSettleIng = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053E", "当前单据在结算状态为结算中，需要实施联系客户执行线下支付或者网银支付处理。") /* "当前单据在结算状态为结算中，需要实施联系客户执行线下支付或者网银支付处理。" */;
    private static final String RepairPlan_VoucherStatusReceived = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053F", "当前单据为往期已结算且凭证状态为已发送待生成，需要实施人员检查“重新生成凭证”节点数据，处理异常数据。") /* "当前单据为往期已结算且凭证状态为已发送待生成，需要实施人员检查“重新生成凭证”节点数据，处理异常数据。" */;
    private static final String RepairPlan_VoucherStatusReceivedThis = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400540", "当前单据为当期已结算凭证状态为已发送待生成，建议实施人员检查“重新生成凭证”节点数据，处理异常数据。") /* "当前单据为当期已结算凭证状态为已发送待生成，建议实施人员检查“重新生成凭证”节点数据，处理异常数据。" */;
    private static final String RepairPlan_SourceFromDrftBill = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400541", "当前单据为商业汇票推式生成单据，需要实施处理为已结算，或者上游商业汇票撤回单据。") /* "当前单据为商业汇票推式生成单据，需要实施处理为已结算，或者上游商业汇票撤回单据。" */;
    private static final String RepairPlan_SourceFromArapBill = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400542", "当前单据来源于应收应付未审核完成单据，需要应收应付升迁处理推送资金结算，本次升迁删除日记账数据。") /* "当前单据来源于应收应付未审核完成单据，需要应收应付升迁处理推送资金结算，本次升迁删除日记账数据。" */;
    private static final String RepairPlan_SourceFromPayapplicationBillApproveEdit = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400544", "付款申请单存在审批过程中的单据，需要审批完成后才能升级，或者撤回审批返回成开立态再升级。") /* "付款申请单存在审批过程中的单据，需要审批完成后才能升级，或者撤回审批返回成开立态再升级。" */;
    private static final String RepairPlan_IsWfControlledApproveEdit = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400545", "当前单据审批状态为审批过程中，需要审批完成后才能升级。") /* "当前单据审批状态为审批过程中，需要审批完成后才能升级。" */;
    private static final String RepairPlan_FundPayBillIsExist = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400546", "本升级租户当前已存在“资金付款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！") /* "本升级租户当前已存在“资金付款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！" */;
    private static final String RepairPlan_FundCollBillIsExist = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400547", "本升级租户当前已存在“资金收款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！") /* "本升级租户当前已存在“资金收款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！" */;

    /**
     * 会计主体信息 ：key会计主体id，value会计主体name
     */
    private static final @NonNull Cache<String, String> accEntityNameCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    @Autowired
    CmpNewFiMigradeUtilService cmpNewFiMigradeUtilService;

    @Override
    public String newFiPreCheck(CtmJSONObject params) throws Exception {

        String yTenantId = AppContext.getCurrentUser().getYTenantId();
        String tenantResult = TenantCenter.getTenantById (yTenantId);
        String tenantName = ((LinkedHashMap)CtmJSONObject.parseObject(tenantResult).get("tenant")).get("tenantName").toString();


        Map<String, String> tenantMap = new HashMap<>();

        tenantMap.put(yTenantId, tenantName);

        CmpPreCheckReqVO preCheckReqVO = new CmpPreCheckReqVO();
        preCheckReqVO.setTenantMap(tenantMap);
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = getAuditAndUnSettleData(preCheckReqVO);
//        cmpPreCheckDetailVO.addAll(getVoucherStatusReceivedDataThis(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getVoucherStatusReceivedData(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getAuditAndSettleIngData(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getSourceFromDrftBill(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getSourceFromArapBill(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getPayApplicationBill(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getIsWfAndApprovalData(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getFundPayBillIsExist(preCheckReqVO));
        cmpPreCheckDetailVO.addAll(getFundCollBillIsExist(preCheckReqVO));
        return ResultMessage.data(cmpPreCheckDetailVO);
    }

    @Override
    public List<CmpPreCheckDetailVO> getAuditAndUnSettleData(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.AuditAndUnSettle.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getIsWfAndApprovalData(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.IsWfControlledApproveEdit.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getAuditAndSettleIngData(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.AuditAndSettleIng.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getVoucherStatusReceivedData(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.VoucherStatusReceived.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getVoucherStatusReceivedDataThis(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.VoucherStatusReceivedThis.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getSourceFromDrftBill(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.SourceFromDrftBill.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getSourceFromArapBill(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.SourceFromArapBill.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQuery(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getPayApplicationBill(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.PayapplicationBillApproveEdit.getCheckType());
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVOS = buildQueryForPayApplicationBill(preCheckReqVO);
        return cmpPreCheckDetailVOS;
    }

    @Override
    public List<CmpPreCheckDetailVO> getFundPayBillIsExist(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        List<CmpPreCheckDetailVO> preCheckDetailVOList = new ArrayList<>();

        preCheckReqVO.setFullName(FundPayment.ENTITY_NAME);
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.FundPayBillIsExist.getCheckType());
        QuerySchema query = QuerySchema.create().addSelect("id,code,auditstatus,accentity,vouchdate");
        QueryConditionGroup queryCondition = new QueryConditionGroup();
        List<Map> resList = MetaDaoHelper.query(preCheckReqVO.getFullName(), query.addCondition(queryCondition));
        queryResultProcess(resList, preCheckReqVO, preCheckDetailVOList);
        return preCheckDetailVOList;
    }

    @Override
    public List<CmpPreCheckDetailVO> getFundCollBillIsExist(CmpPreCheckReqVO preCheckReqVO) throws Exception {
        List<CmpPreCheckDetailVO> preCheckDetailVOList = new ArrayList<>();

        preCheckReqVO.setFullName(FundCollection.ENTITY_NAME);
        preCheckReqVO.setCheckType(MigradeCheckTypeEnum.FundCollBillIsExist.getCheckType());
        QuerySchema query = QuerySchema.create().addSelect("id,code,auditstatus,accentity,vouchdate");
        QueryConditionGroup queryCondition = new QueryConditionGroup();
        List<Map> resList = MetaDaoHelper.query(preCheckReqVO.getFullName(), query.addCondition(queryCondition));
        queryResultProcess(resList, preCheckReqVO, preCheckDetailVOList);
        return preCheckDetailVOList;
    }

    @Override
    public void checkUpgradeDataBack(String fullname,String billId) throws Exception {
        if(fullname.equals(PayBill.ENTITY_NAME)){
            PayBill bill = MetaDaoHelper.findById(fullname, billId);
            //如果付款单结算成功 不能逆操作
            if(bill.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102263"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D3", "当前单据为升级数据，且来源数据已经结算完成，故当前单据不允许逆向操作。") /* "当前单据为升级数据，且来源数据已经结算完成，故当前单据不允许逆向操作。" */);
            }
            if(bill.getBilltype().getValue() == EventType.PayApplyBill.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102264"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D4", "当前单据为升级数据，且来源数据关联了付款申请单，故当前单据不允许逆向操作。") /* "当前单据为升级数据，且来源数据关联了付款申请单，故当前单据不允许逆向操作。" */);
            }
        }else if(fullname.equals(ReceiveBill.ENTITY_NAME)){
            ReceiveBill bill = MetaDaoHelper.findById(fullname, billId);
            //如果付款单结算成功 不能逆操作
            if(bill.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102263"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D3", "当前单据为升级数据，且来源数据已经结算完成，故当前单据不允许逆向操作。") /* "当前单据为升级数据，且来源数据已经结算完成，故当前单据不允许逆向操作。" */);
            }
        }
    }

    /**
     * 数据查询类
     *
     * @param preCheckReqVO
     * @return
     */
    private List<CmpPreCheckDetailVO> buildQuery(CmpPreCheckReqVO preCheckReqVO) throws Exception {

        //获取当前期间启用日期g
        Date periodBeginDate = cmpNewFiMigradeUtilService.periodBeginDate(PayBill.ENTITY_NAME);
        String checkType = preCheckReqVO.getCheckType();
        List<CmpPreCheckDetailVO> preCheckDetailVOList = new ArrayList<>();
        QuerySchema query = QuerySchema.create().addSelect("id,code,auditstatus,accentity,vouchdate");
        QueryConditionGroup queryCondition = new QueryConditionGroup();
        //已审批未结算 提示期间日期前的数据
        if (checkType.equals(MigradeCheckTypeEnum.AuditAndUnSettle.getCheckType())) {
            queryCondition.addCondition(QueryCondition.name("auditstatus").eq("1"), QueryCondition.name("settlestatus").eq("1"),
                    QueryCondition.name("vouchdate").egt(periodBeginDate));
        }else if(checkType.equals(MigradeCheckTypeEnum.AuditAndSettleIng.getCheckType())){
            queryCondition.addCondition(QueryCondition.name("auditstatus").eq("1"), QueryCondition.name("paystatus").in("1","2","4","5","6"));//付款 预下单成功、预下单失败、支付失败、支付中、支付不明
        } else if (checkType.equals(MigradeCheckTypeEnum.VoucherStatusReceived.getCheckType())) {
            queryCondition.addCondition(QueryCondition.name("auditstatus").eq("1"), QueryCondition.name("settlestatus").eq("2"));
            String[] vouchSatus = new String[]{"2", "3"};
            queryCondition.addCondition(QueryCondition.name("voucherstatus").in(vouchSatus));
            queryCondition.addCondition(QueryCondition.name("vouchdate").lt(periodBeginDate));
        }
//        else if (checkType.equals(MigradeCheckTypeEnum.VoucherStatusReceivedThis.getCheckType())) {
//            queryCondition.addCondition(QueryCondition.name("auditstatus").eq("1"), QueryCondition.name("settlestatus").eq("2"));
//            String[] vouchSatus = new String[]{"2", "3"};
//            queryCondition.addCondition(QueryCondition.name("voucherstatus").in(vouchSatus));
//            queryCondition.addCondition(QueryCondition.name("vouchdate").egt(periodBeginDate));
//        }
        else if (checkType.equals(MigradeCheckTypeEnum.SourceFromDrftBill.getCheckType())) {
            queryCondition.addCondition(QueryCondition.name("srcitem").eq("50"));
            String[] vouchSatus = new String[]{"1", "4"};
            QueryConditionGroup or = QueryConditionGroup.or(QueryCondition.name("settlestatus").not_eq("2"), QueryCondition.name("voucherstatus").not_in(vouchSatus));
            queryCondition.addCondition(or);
        } else if (checkType.equals(MigradeCheckTypeEnum.SourceFromArapBill.getCheckType())) {
            queryCondition.addCondition(QueryCondition.name("auditstatus").eq("2"), QueryCondition.name("settlestatus").eq("1"));
            queryCondition.addCondition(QueryCondition.name("srcitem").not_eq("8"));
        }else if(checkType.equals(MigradeCheckTypeEnum.IsWfControlledApproveEdit.getCheckType())){
            //现金管理的审批中数据  审批流状态为1 审批中
            queryCondition.addCondition(QueryCondition.name("isWfControlled").eq(true),QueryCondition.name("verifystate").eq("1"));
            queryCondition.addCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        }

        //收款不校验支付状态
        if(!checkType.equals(MigradeCheckTypeEnum.AuditAndSettleIng.getCheckType())){
            preCheckReqVO.setFullName("cmp.receivebill.ReceiveBill");
            List<Map> resList1 = MetaDaoHelper.query(preCheckReqVO.getFullName(), query.addCondition(queryCondition));
            queryResultProcess(resList1, preCheckReqVO, preCheckDetailVOList);
        }
        preCheckReqVO.setFullName("cmp.paybill.PayBill");
        if(checkType.equals(MigradeCheckTypeEnum.AuditAndUnSettle.getCheckType())){
            queryCondition.addCondition(QueryCondition.name("paystatus").not_in("1","2","4","5","6"));
        }
        List<Map> resList = MetaDaoHelper.query(preCheckReqVO.getFullName(), query.addCondition(queryCondition));
        queryResultProcess(resList, preCheckReqVO, preCheckDetailVOList);

        return preCheckDetailVOList;
    }

    private List<CmpPreCheckDetailVO> buildQueryForPayApplicationBill(CmpPreCheckReqVO preCheckReqVO) throws Exception {

        //获取当前期间启用日期
        List<CmpPreCheckDetailVO> preCheckDetailVOList = new ArrayList<>();
        QueryConditionGroup queryCondition = new QueryConditionGroup();
        preCheckReqVO.setFullName(PayApplicationBill.ENTITY_NAME);
        QuerySchema query = QuerySchema.create().addSelect("id,code,approvalStatus,accentity,vouchdate");
        queryCondition.addCondition(QueryCondition.name("verifystate").eq(VerifyState.SUBMITED.getValue()));
        List<Map> resList = MetaDaoHelper.query(preCheckReqVO.getFullName(), query.addCondition(queryCondition));
        queryResultProcess(resList, preCheckReqVO, preCheckDetailVOList);
        return preCheckDetailVOList;
    }

    /**
     * 条件拼接
     * @param resList
     * @param preCheckReqVO
     * @param preCheckDetailVOList
     */
    private void queryResultProcess(List<Map> resList, CmpPreCheckReqVO preCheckReqVO, List<CmpPreCheckDetailVO> preCheckDetailVOList) throws Exception {
        String checkType = preCheckReqVO.getCheckType();
        Map<String, String> tenantMap = preCheckReqVO.getTenantMap();
        String[] strings = tenantMap.keySet().toArray(new String[0]);

        if (resList != null && resList.size() > 0) {
            //批量查询会计主体那么并缓存 用于后续赋值
            queryAccEntityNameToCache(resList);
            for (Map bill: resList) {
                CmpPreCheckDetailVO preCheckDetailVO = new CmpPreCheckDetailVO();
                preCheckDetailVO.setBillCode(bill.get(ICmpConstant.CODE).toString());
                preCheckDetailVO.setBillid(bill.get(ICmpConstant.ID).toString());
                preCheckDetailVO.setYtenant_id(strings[0]);
                preCheckDetailVO.setCheckType(MigradeCheckTypeEnum.findByKey(checkType).getCheckTypeName());
                preCheckDetailVO.setTenant_name(tenantMap.get(strings[0]));

                preCheckDetailVO.setBillDate(bill.get(ICmpConstant.VOUCHDATE).toString());
                //如果取不到缓存 则进行查询
                setAccEntityName(preCheckDetailVO,bill.get(ICmpConstant.ACCENTITY)!=null?bill.get(ICmpConstant.ACCENTITY).toString():null);
                preCheckDetailVO.setSrcBillNo(bill.get(ICmpConstant.CODE).toString());
                if(bill.get("auditstatus")!=null){
                    preCheckDetailVO.setAuditStatus(AuditStatus.getName(Short.valueOf(bill.get("auditstatus").toString())));
                }else if(bill.get("approvalStatus")!=null){
                    preCheckDetailVO.setAuditStatus(ApprovalStatus.getName(Short.valueOf(bill.get("approvalStatus").toString())));
                }

                if (preCheckReqVO.getFullName().equals("cmp.paybill.PayBill")){
                    preCheckDetailVO.setServiceName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400543", "付款工作台") /* "付款工作台" */);
                    preCheckDetailVO.setBillType(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400543", "付款工作台") /* "付款工作台" */);
                    preCheckDetailVO.setSrcAppName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400543", "付款工作台") /* "付款工作台" */);

                } else if (preCheckReqVO.getFullName().equals("cmp.receivebill.ReceiveBill")) {
                    preCheckDetailVO.setServiceName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400548", "收款工作台") /* "收款工作台" */);
                    preCheckDetailVO.setBillType(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400548", "收款工作台") /* "收款工作台" */);
                    preCheckDetailVO.setSrcAppName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400548", "收款工作台") /* "收款工作台" */);

                }else if(preCheckReqVO.getFullName().equals(PayApplicationBill.ENTITY_NAME)){
                    preCheckDetailVO.setServiceName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400549", "付款申请工作台") /* "付款申请工作台" */);
                    preCheckDetailVO.setBillType(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400549", "付款申请工作台") /* "付款申请工作台" */);
                    preCheckDetailVO.setSrcAppName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400549", "付款申请工作台") /* "付款申请工作台" */);

                }else if(preCheckReqVO.getFullName().equals(FundPayment.ENTITY_NAME)){
                    preCheckDetailVO.setServiceName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053B", "资金付款单") /* "资金付款单" */);
                    preCheckDetailVO.setBillType(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053B", "资金付款单") /* "资金付款单" */);
                    preCheckDetailVO.setSrcAppName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053B", "资金付款单") /* "资金付款单" */);

                }else if(preCheckReqVO.getFullName().equals(FundCollection.ENTITY_NAME)){
                    preCheckDetailVO.setServiceName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053C", "资金收款单") /* "资金收款单" */);
                    preCheckDetailVO.setBillType(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053C", "资金收款单") /* "资金收款单" */);
                    preCheckDetailVO.setSrcAppName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540053C", "资金收款单") /* "资金收款单" */);

                }
                // 根据校验类型，赋值不同操作提
                if (checkType.equals(MigradeCheckTypeEnum.AuditAndUnSettle.getCheckType())) {
                    preCheckDetailVO.setRepairPlan(RepairPlan_AuditAndUnSettle);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.TipOperate.getCheckResult());
                }else if(checkType.equals(MigradeCheckTypeEnum.AuditAndSettleIng.getCheckType())){
                    preCheckDetailVO.setRepairPlan(RepairPlan_AuditAndSettleIng);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.CheckNotPass.getCheckResult());
                }
//                else if (checkType.equals(MigradeCheckTypeEnum.VoucherStatusReceivedThis.getCheckType())) {
//                    preCheckDetailVO.setRepairPlan(RepairPlan_VoucherStatusReceivedThis);
//                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.TipOperate.getCheckResult());
//                }
                else if (checkType.equals(MigradeCheckTypeEnum.VoucherStatusReceived.getCheckType())) {
                    preCheckDetailVO.setRepairPlan(RepairPlan_VoucherStatusReceived);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.CheckNotPass.getCheckResult());
                } else if (checkType.equals(MigradeCheckTypeEnum.SourceFromDrftBill.getCheckType())) {
                    preCheckDetailVO.setRepairPlan(RepairPlan_SourceFromDrftBill);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.CheckNotPass.getCheckResult());
                } else if (checkType.equals(MigradeCheckTypeEnum.SourceFromArapBill.getCheckType())) {
                    preCheckDetailVO.setRepairPlan(RepairPlan_SourceFromArapBill);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.TipOperate.getCheckResult());
                }else if(checkType.equals(MigradeCheckTypeEnum.PayapplicationBillApproveEdit.getCheckType())){
                    preCheckDetailVO.setRepairPlan(RepairPlan_SourceFromPayapplicationBillApproveEdit);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.CheckNotPass.getCheckResult());
                }else if(checkType.equals(MigradeCheckTypeEnum.IsWfControlledApproveEdit.getCheckType())){
                    preCheckDetailVO.setRepairPlan(RepairPlan_IsWfControlledApproveEdit);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.CheckNotPass.getCheckResult());
                }else if(checkType.equals(MigradeCheckTypeEnum.FundPayBillIsExist.getCheckType())){
                    preCheckDetailVO.setRepairPlan(RepairPlan_FundPayBillIsExist);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.TipOperate.getCheckResult());
                }else if(checkType.equals(MigradeCheckTypeEnum.FundCollBillIsExist.getCheckType())){
                    preCheckDetailVO.setRepairPlan(RepairPlan_FundCollBillIsExist);
                    preCheckDetailVO.setCheckResult(MigradeCheckResultEnum.TipOperate.getCheckResult());
                }
                preCheckDetailVOList.add(preCheckDetailVO);
            }
        }
    }

    private void queryAccEntityNameToCache(List<Map> resList) throws Exception {
        Set<String> accentityIdSet = new HashSet<>();
        for(Map map : resList){
            if(map.get(ICmpConstant.ACCENTITY)!=null){
                accentityIdSet.add(map.get(ICmpConstant.ACCENTITY).toString());
            }
        }
        List<Map<String, Object>> accEntityes = QueryBaseDocUtils.queryAccEntityByIds(Arrays.asList(accentityIdSet.toArray(new String[0])));
        //缓存数据加入缓存
        if(CollectionUtils.isNotEmpty(accEntityes)){
            for(Map<String, Object> map : accEntityes){
                accEntityNameCache.put(map.get("id").toString(), map.get("name")!=null?map.get("name").toString():null);
            }
        }
    }

    private void setAccEntityName(CmpPreCheckDetailVO preCheckDetailVO,String accentity) throws Exception {
        if(accentity!=null && accEntityNameCache.getIfPresent(accentity)!=null){
            preCheckDetailVO.setAccEntityName(accEntityNameCache.getIfPresent(accentity));
        }else if(accentity!=null){
            List<Map<String, Object>> accEntityMapList = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
            if(CollectionUtils.isNotEmpty(accEntityMapList)){
                String accentityName = accEntityMapList.get(0).get("name")!=null?accEntityMapList.get(0).get("name").toString():null;
                if(CollectionUtils.isNotEmpty(accEntityMapList) && accentityName!=null){
                    preCheckDetailVO.setAccEntityName(accentityName);
                    accEntityNameCache.put(accentity, accentityName);
                }
            }
        }
    }

    /**
     * 预检租户
     *
     * @param ytenant_ids
     * @return
     */
    private List<String> getPreCheckTenant(String ytenant_ids) {
        List<String> ytenantIds = new ArrayList<>();
        if (StringUtils.isEmpty(ytenant_ids)) {
            ytenantIds.add(AppContext.getCurrentUser().getYTenantId());
            return ytenantIds;
        }
        if (ytenant_ids.contains(",")) {
            ytenantIds = Arrays.asList(ytenant_ids.split(","));
        } else {
            ytenantIds.add(ytenant_ids);
        }
        return ytenantIds;
    }
}
