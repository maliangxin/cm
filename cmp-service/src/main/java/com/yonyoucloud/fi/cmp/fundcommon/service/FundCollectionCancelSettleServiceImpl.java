package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.error.CtmErrorCode;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillBaseNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillDetailNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.SourceBillNode;
import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;
import com.yonyoucloud.fi.basecom.util.DateUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.CancelSettlementServiceEnum;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.ICmpOperationService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.util.BillAction;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class FundCollectionCancelSettleServiceImpl implements ICmpOperationService {
    public static final String FUNDPAYMENT_ENTITY_NAME = "cmp.fundpayment.FundPayment";
    public static final String FUNDPAYMENT_B_ENTITY_NAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUNDCOLLECTION_ENTITY_NAME = "cmp.fundcollection.FundCollection";
    public static final String FUNDCOLLECTION_B_ENTITY_NAME = "cmp.fundcollection.FundCollection_b";

    @Override
    public boolean handleCancelSettle(CancelSettlementServiceEnum serviceEnum, BillBaseNode billBaseNode, String reason) throws Exception {
        //不管是哪种场景（一般结算/统收统支/协同生单）,进来的参数都已经是当前要取消结算的资金收/付款单

        log.error("统一结算单取消结算handleCancelSettle（资金收款单），入参：serviceEnum:{},billBaseNode:{},reason:{}", serviceEnum, billBaseNode, reason);
        //回滚资金收/付款的结算状态为“结算中”
        BizObject bizObject = MetaDaoHelper.findById(FUNDCOLLECTION_ENTITY_NAME, billBaseNode.getBillId(), 2);
        FundCollection fundCollection = new FundCollection();
        fundCollection.init(bizObject);
        fundCollection.setEntityStatus(EntityStatus.Update);
        List<FundCollection_b> fundCollection_bList = fundCollection.FundCollection_b().stream().filter(item -> billBaseNode.getBillDetailIds().contains(item.getId())).collect(Collectors.toList());

        //validate(fundCollection_bList);
        bizObject.set("_entityName", FundCollection.ENTITY_NAME);

        String eaaiKey = "cancelSettle_lock_fundcollection_id:" + fundCollection.getId();
        CtmLockTool.executeInOneServiceLock(eaaiKey, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_SUCCESS) {
                List<String> fundCollectionIdEaaiList = AppContext.cache().getObject("cancelSettle_fundcollection_id");
                if (fundCollectionIdEaaiList != null && fundCollectionIdEaaiList.contains(fundCollection.getId().toString())) {
                    //delResult = true;
                    log.error("缓存中找到id为："+fundCollection.getId()+"的资金收款单，不会执行删除事项和凭证");
                } else {
                    if (fundCollectionIdEaaiList == null) {
                        fundCollectionIdEaaiList = new ArrayList<>();
                    }
                    fundCollectionIdEaaiList.add(fundCollection.getId().toString());
                    AppContext.getBean(FundCommonCancelSettleServiceImpl.class).dealEaaiVoucherInfo(bizObject);
                    AppContext.cache().setObject("cancelSettle_fundcollection_id", fundCollectionIdEaaiList, 60 * 2);
                }
            }
        });

        Map<String, BigDecimal> setttleSuccessSumMap = billBaseNode.getCancelAmount();
        //实占变未占（资金付款没有预占）
        List<FundCollection_b> updateList = fundCollection_bList.stream().filter(item -> item.getIsOccupyBudget() != null && item.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()).collect(Collectors.toList());


        //因为结算那边取消结算的时候改了结算成功金额，导致调用预算接口修改占用状态的时候报错，之前跟刘研沟通，他们把取消结算金额传递过来，然后咱们先还原金额，调用预算，然后再将金额改回结算一开始改的状态该；
        for (FundCollection_b settleFundCollection_b : updateList) {
            settleFundCollection_b.setSettlesuccessSum(settleFundCollection_b.getSettlesuccessSum().add(setttleSuccessSumMap.get(settleFundCollection_b.getId().toString())));
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            //如果子表中有实占的行，实占改为未占
            ResultBudget resultBudgetDelActual = AppContext.getBean(CmpBudgetManagerService.class).fundCollectionReleaseActualOccupySuccessUnAudit(bizObject, fundCollection_bList, IBillNumConstant.FUND_COLLECTION, BillAction.CANCEL_SETTLE);
            if (resultBudgetDelActual.isSuccess()) {
                updateList.stream().forEach(fundCollection_b -> {
                    // 实占成功，弃审后变为预占成功
                    fundCollection_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                });
            }
        }
        for (FundCollection_b settleFundCollection_b : updateList) {
            //20260104 亓豪反馈预发有并发问题，导致金额不对，所以在这里加上锁
            String key = "cmp_fundcollection_b_budget_:" + settleFundCollection_b.getId();
            CtmLockTool.executeInOneServiceLock(key, 60L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    //加锁失败
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100652"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
                }
                settleFundCollection_b.setSettlesuccessSum(settleFundCollection_b.getSettlesuccessSum().subtract(setttleSuccessSumMap.get(settleFundCollection_b.getId().toString())));
            });
        }

        for (FundCollection_b fundCollection_b : fundCollection_bList) {
            fundCollection_b.setFundSettlestatus(FundSettleStatus.SettleProssing);
            fundCollection_b.set("stwbSettleStatus", SettleApplyDetailStateEnum.HANDLING.getValue());//按照刘研要求，取消结算时同时更新结算申请单表里边的数据为结算中
            fundCollection_b.setSettleSuccessTime(null);
            fundCollection_b.setActualSettlementExchangeRateType(null);
            fundCollection_b.setActualSettlementExchangeRate(null);
            //fundCollection_b.setSettlesuccessSum(null);
            fundCollection_b.setActualSettlementAmount(null);
            fundCollection_b.setEntityStatus(EntityStatus.Update);
        }

        fundCollection.setVoucherstatus(VoucherStatus.find(bizObject.getShort("voucherstatus")));
        fundCollection.setSettleSuccessTime(null);
/*        fundCollection.setVoucherNo(null);
        fundCollection.setVoucherPeriod(null);
        fundCollection.setVoucherId(null);*/
//        fundCollection.setDescription(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400644", "取消结算：取消时间-") /* "取消结算：取消时间-" */ + DateUtils.dateTimeToDateString(new Date()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400643", "取消原因-") /* "取消原因-" */ + reason);
        fundCollection.setDescription(fundCollection.getDescription() == null ? " " : fundCollection.getDescription() + " ");
        if (AppContext.getBean(FundCommonCancelSettleServiceImpl.class).isSettleSuccessToPost(bizObject.getString("accentity"))) {
            fundCollection.setVoucherstatus(VoucherStatus.TO_BE_POST);
        }
        fundCollection.setVoucherNo(bizObject.getString("voucherNo"));
        fundCollection.setVoucherPeriod(bizObject.getString("voucherPeriod"));
        fundCollection.setVoucherId(bizObject.getString("voucherId"));

        fundCollection.setFundCollection_b(fundCollection_bList);
        fundCollection.remove("pubts");
        MetaDaoHelper.update(FUNDCOLLECTION_ENTITY_NAME, fundCollection);
        return true;
    }

    @Override
    public BillDetailNode buildCancelSettleNode(String billTypeId, List<String> billDetailIds) {
        log.error("统一结算单取消结算buildCancelSettleNode（资金收款单），入参：billTypeId:{},billDetailIds:{}", billTypeId, billDetailIds);
        BillDetailNode billDetailNode = BillDetailNode.builder().domainKey(IDomainConstant.MDD_DOMAIN_CMP).build();
        try {
            if (CollectionUtils.isEmpty(billDetailIds)) {
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105002"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208855DE04700003", "取消结算,未返回单据明细ID，请联系研发及时处理！！"));
            }
            //获取单据id
            BizObject bizObject = MetaDaoHelper.findById(FUNDCOLLECTION_B_ENTITY_NAME, billDetailIds.get(0));
            if (bizObject == null) {
                billDetailNode.setCheckStatus(false);
                billDetailNode.setCheckMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400642", "未查询到子表数据，子表id:") /* "未查询到子表数据，子表id:" */ + billDetailIds.get(0));
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105003"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208856DC05A80006", "未查询到子表数据，子表id:[%s]"), billDetailIds.get(0)));
            }
            String billId = bizObject.getString("mainid");
            FundCollection fundCollection = MetaDaoHelper.findById(FUNDCOLLECTION_ENTITY_NAME, billId, 2);
            List<FundCollection_b> hasSourceFundCollection_bList = fundCollection.FundCollection_b().stream().filter(item -> billDetailIds.contains(item.getId().toString())).filter(item -> Strings.isNotEmpty(item.getSettledId())).collect(Collectors.toList());
            List<SourceBillNode> upperNodes = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(hasSourceFundCollection_bList)) {
                //有上游单据
                for (FundCollection_b fundCollection_b : hasSourceFundCollection_bList) {
                    List<String> sourceAutoIdList = new ArrayList<>();
                    sourceAutoIdList.add(fundCollection_b.getSettledId());
                    SourceBillNode sourceBillNode = SourceBillNode.builder().billTypeId("500").billDetailIds(sourceAutoIdList).build();
                    upperNodes.add(sourceBillNode);
                }
            }
            billDetailNode.setBillTypeId(billTypeId);
            billDetailNode.setBillDetailIds(billDetailIds);
            billDetailNode.setBillId(billId);
            //单据编号
            billDetailNode.setBillCode(fundCollection.getCode());
            //单据名称
            billDetailNode.setBillName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400645", "资金收款单") /* "资金收款单" */);
            String serviceCode = IServicecodeConstant.FUNDCOLLECTION;
            billDetailNode.setServiceCode(serviceCode);

            billDetailNode.setUpperNodes(upperNodes);
            billDetailNode.setCheckStatus(true);
            billDetailNode.setBillNo("cmp_fundcollection");
            billDetailNode.setCheckMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400641", "检查成功") /* "检查成功" */);
        } catch (Exception ex) {
            log.error("构造billDetailNode报错：" + ex.getMessage());
            billDetailNode.setCheckStatus(false);
            billDetailNode.setCheckMsg(ex.getMessage());
        }
        return billDetailNode;
    }

    private void validate(List<FundCollection_b> fundcollection_bList) throws Exception {
        List<String> billClaimIdList = fundcollection_bList.stream().map(FundCollection_b::getBillClaimId).map(Object::toString).collect(Collectors.toList());
        QuerySchema querySchema = QuerySchema.create().addSelect("id, code, isfundsplit, settlestatus,refbill");
        QueryConditionGroup queryConditionGroupById = new QueryConditionGroup();
        queryConditionGroupById.addCondition(QueryCondition.name("id").in(billClaimIdList));

        List<Map<String, Object>> result = MetaDaoHelper.query(BillClaim.ENTITY_NAME, querySchema);
        for (Map<String, Object> resultMap : result) {
            short settleStatus = Short.parseShort(resultMap.get(ICmpConstant.SETTLE_STATUS).toString());
            boolean isFundSplit = Boolean.parseBoolean(resultMap.get("isfundsplit").toString());
            if (isFundSplit && settleStatus == FundSettleStatus.SettleSuccess.getValue()) {
                String errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5B02804380033", "认领单【%s】已完成资金切分，并产生了后续业务，暂不支持取消结算，请检查！") /* "认领单【%s】已完成资金切分，并产生了后续业务，暂不支持取消结算，请检查！" */,
                        resultMap.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105009"), errorMsg);
            }
        }
    }
}
