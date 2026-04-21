package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.yonbip.ctm.error.CtmErrorCode;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillBaseNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillDetailNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.SourceBillNode;
import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.ctm.stwb.unifiedsettle.pubitf.IUnifiedSettlePubService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.CancelSettlementServiceEnum;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.ICmpOperationService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class FundPaymentCancelSettleServiceImpl implements ICmpOperationService {

    public static final String FUNDPAYMENT_ENTITY_NAME = "cmp.fundpayment.FundPayment";
    public static final String FUNDPAYMENT_B_ENTITY_NAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUNDCOLLECTION_ENTITY_NAME = "cmp.fundcollection.FundCollection";
    public static final String FUNDCOLLECTION_B_ENTITY_NAME = "cmp.fundcollection.FundCollection_b";

    // 预占
    public static final String PRE = "pre";

    @Autowired
    private BusiSystemConfigService busiSystemConfigService;

    @Override
    public boolean handleCancelSettle(CancelSettlementServiceEnum serviceEnum, BillBaseNode billBaseNode, String reason) throws Exception {
        //不管是哪种场景（一般结算/统收统支/协同生单）,进来的参数都已经是当前要取消结算的资金收/付款单
        log.error("统一结算单取消结算handleCancelSettle（资金付款单），入参：serviceEnum:{},billBaseNode:{},reason:{}", serviceEnum, billBaseNode, reason);
        //回滚资金收/付款的结算状态为“结算中”
        BizObject bizObject = MetaDaoHelper.findById(FUNDPAYMENT_ENTITY_NAME, billBaseNode.getBillId(), 2);
        FundPayment fundPayment = new FundPayment();
        fundPayment.init(bizObject);
        fundPayment.setEntityStatus(EntityStatus.Update);
        List<FundPayment_b> fundPayment_bList = fundPayment.FundPayment_b().stream().filter(item -> billBaseNode.getBillDetailIds().contains(item.getId())).collect(Collectors.toList());

        bizObject.set("_entityName", FundPayment.ENTITY_NAME);

        String eaaiKey = "cancelSettle_lock_fundpayment_id:" + fundPayment.getId();
        CtmLockTool.executeInOneServiceLock(eaaiKey, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_SUCCESS) {
                List<String> fundPaymentIdEaaiList = AppContext.cache().getObject("cancelSettle_fundpayment_id");
                if (fundPaymentIdEaaiList != null && fundPaymentIdEaaiList.contains(fundPayment.getId().toString())) {
                    //delResult = true;
                    log.error("缓存中找到id为："+fundPayment.getId()+"的资金付款单，不会执行删除事项和凭证");
                } else {
                    if (fundPaymentIdEaaiList == null) {
                        fundPaymentIdEaaiList = new ArrayList<>();
                    }
                    fundPaymentIdEaaiList.add(fundPayment.getId().toString());
                    AppContext.getBean(FundCommonCancelSettleServiceImpl.class).dealEaaiVoucherInfo(bizObject);
                    AppContext.cache().setObject("cancelSettle_fundpayment_id", fundPaymentIdEaaiList, 60 * 2);
                }
            }
        });

        Map<String, BigDecimal> setttleSuccessSumMap = billBaseNode.getCancelAmount();
        List<FundPayment_b> updateList = fundPayment_bList.stream().filter(item -> item.getIsOccupyBudget() != null && item.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()).collect(Collectors.toList());

        //因为结算那边取消结算的时候改了结算成功金额，导致调用预算接口修改占用状态的时候报错，之前跟刘研沟通，他们把取消结算金额传递过来，然后咱们先还原金额，调用预算，然后再将金额改回结算一开始改的状态该；
        for (FundPayment_b settleFundPayment_b : updateList) {
            settleFundPayment_b.setSettlesuccessSum(settleFundPayment_b.getSettlesuccessSum().add(setttleSuccessSumMap.get(settleFundPayment_b.getId().toString())));
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            //如果子表中有实占的行，实占改为预占
            ResultBudget resultBudgetDelActual = AppContext.getBean(CmpBudgetManagerService.class).gcExecuteTrueUnAudit(bizObject, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_SETTLE);
            if (resultBudgetDelActual.isSuccess()) {
                List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, IBillNumConstant.FUND_PAYMENT);
                //获取平台配置
                String preAction = null;
                if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(objects)) {
                    preAction = (String) objects.get(0).get(PRE);
                }
                if (!StringUtils.isEmpty(preAction)) {
                    updateList.stream().forEach(fundPayment_b -> {
                        // 实占成功，弃审后变为预占成功
                        fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    });
                } else {//没有配置预占动作
                    updateList.stream().forEach(fundPayment_b -> {
                        // 实占成功，弃审后变为未占用
                        fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    });
                }
            } else {//没有配置预占动作
                updateList.stream().forEach(fundPayment_b -> {
                    // 实占成功，弃审后变为未占用
                    fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                });
            }
        }

        for (FundPayment_b settleFundPayment_b : updateList) {
            //20260104 亓豪反馈预发有并发问题，导致金额不对，所以在这里加上锁
            String key = "cmp_fundpayment_b_budget_:" + settleFundPayment_b.getId();
            CtmLockTool.executeInOneServiceLock(key, 60L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    //加锁失败
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100652"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
                }
                settleFundPayment_b.setSettlesuccessSum(settleFundPayment_b.getSettlesuccessSum().subtract(setttleSuccessSumMap.get(settleFundPayment_b.getId().toString())));
            });
        }

        for (FundPayment_b fundPayment_b : fundPayment_bList) {
            fundPayment_b.setFundSettlestatus(FundSettleStatus.SettleProssing);
            fundPayment_b.set("stwbSettleStatus", SettleApplyDetailStateEnum.HANDLING.getValue());//按照刘研要求，取消结算时同时更新结算申请单表里边的数据为结算中
            fundPayment_b.setSettleSuccessTime(null);
            fundPayment_b.setActualSettlementExchangeRateType(null);
            fundPayment_b.setActualSettlementExchangeRate(null);
            //fundPayment_b.setSettlesuccessSum(null);
            fundPayment_b.setActualSettlementAmount(null);
            fundPayment_b.setEntityStatus(EntityStatus.Update);
        }

        fundPayment.setVoucherstatus(VoucherStatus.find(bizObject.getShort("voucherstatus")));
        fundPayment.setSettleSuccessTime(null);
//        fundPayment.setDescription(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400439", "取消结算：取消时间-") /* "取消结算：取消时间-" */ + DateUtils.dateTimeToDateString(new Date()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400438", "取消原因-") /* "取消原因-" */ + reason);
        fundPayment.setDescription(fundPayment.getDescription() == null ? " " : fundPayment.getDescription() + " ");
        //fundCommonCancelSettleService.dealBuddget(IServicecodeConstant.FUNDPAYMENT, bizObject);

        if (AppContext.getBean(FundCommonCancelSettleServiceImpl.class).isSettleSuccessToPost(bizObject.getString("accentity"))) {
            fundPayment.setVoucherstatus(VoucherStatus.TO_BE_POST);
        }
        fundPayment.setVoucherNo(bizObject.getString("voucherNo"));
        fundPayment.setVoucherPeriod(bizObject.getString("voucherPeriod"));
        fundPayment.setVoucherId(bizObject.getString("voucherId"));

        //判断是否是协同生单，如果是，协同生成的单据应该是保存态，否则需要手工撤回为保存态
        //协同场景，直接把最下游的结算单B查出来，返回给结算（但要注意，在资金收款接收到取消结算消息时，要判断下，如果是协同生成的资金收款单，不再构造上游给结算，因为上游是资金付款单，手动取消结算单A的时候已经发过消息了）
        List<FundPayment_b> synergyFundPaymentBList = fundPayment_bList.stream().filter(item -> Strings.isNotEmpty(item.getSynergybillno())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(synergyFundPaymentBList)) {
            List<String> sysnergyBillIdList = synergyFundPaymentBList.stream().map(FundPayment_b::getSynergybillid).map(Object::toString).collect(Collectors.toList());
            //协同生成的所有下游资金收款单
            QuerySchema querySchema = QuerySchema.create().addSelect("billtype,id,status,code");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(sysnergyBillIdList));
            querySchema.addCondition(group);
            List<FundCollection> fundCollectionList = MetaDaoHelper.queryObject(FUNDCOLLECTION_ENTITY_NAME, querySchema, null);
            List<String> settleApplyIdList = new ArrayList<>();

            for (FundCollection fundCollection : fundCollectionList) {
                if (fundCollection.getBilltype() == EventType.cooperate_fund_collection) {
                    if (fundCollection.getStatus() != Status.newopen.getValue()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105008"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20885D2404700006", "资金收款单：[%s]状态不是开立态，取消结算失败，请先手动撤回到开立态") /* "资金收款单：[%s]状态不是开立态，取消结算失败，请先手动撤回到开立态" */, fundCollection.getCode()));
                    } else {
                        settleApplyIdList.add(fundCollection.getId().toString());
                    }
                    fundCollection.set("voucherstatus_original", fundCollection.get("voucherstatus"));
                }
            }
            //删除下游协同生成的统一结算单
            AppContext.getBean(IUnifiedSettlePubService.class).deleteUnifiedSettle(FUNDCOLLECTION_ENTITY_NAME, settleApplyIdList.toArray(new String[settleApplyIdList.size()]), PushOrder.FIRST);
            //删除所有协同生成的资金收款单
            MetaDaoHelper.delete(FUNDCOLLECTION_ENTITY_NAME, fundCollectionList);
            for (FundPayment_b fundPayment_b : fundPayment_bList) {
                if (fundCollectionList.stream().anyMatch(fundCollection -> fundCollection.getId().toString().equals(fundPayment_b.getSynergybillid()))) {
                    fundPayment_b.setActualSettlementAmount(null);
                    fundPayment_b.setSynergybillno(null);
                    fundPayment_b.setSynergybillid(null);
                    fundPayment_b.setIssynergy(false);
                    fundPayment_b.setEntityStatus(EntityStatus.Update); // 标记为更新状态
                }
            }
        }
        fundPayment.setFundPayment_b(fundPayment_bList);
        fundPayment.remove("pubts");
        MetaDaoHelper.update(FUNDPAYMENT_ENTITY_NAME, fundPayment);
        return true;
    }


    @Override
    public BillDetailNode buildCancelSettleNode(String billTypeId, List<String> billDetailIds) {
        log.error("统一结算单取消结算buildCancelSettleNode（资金付款单），入参：billTypeId:{},billDetailIds:{}", billTypeId, billDetailIds);
        BillDetailNode billDetailNode = BillDetailNode.builder().domainKey(IDomainConstant.MDD_DOMAIN_CMP).build();
        try {
            if (CollectionUtils.isEmpty(billDetailIds)) {
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105002"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208855DE04700003", "取消结算,未返回单据明细ID，请联系研发及时处理！！"));
            }
            //获取单据id
            BizObject bizObject = MetaDaoHelper.findById(FUNDPAYMENT_B_ENTITY_NAME, billDetailIds.get(0));
            if (bizObject == null) {
                billDetailNode.setCheckStatus(false);
                billDetailNode.setCheckMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400436", "未查询到子表数据，子表id:") /* "未查询到子表数据，子表id:" */ + billDetailIds.get(0));
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105003"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208856DC05A80006", "未查询到子表数据，子表id:[%s]"), billDetailIds.get(0)));
            }
            String billId = bizObject.getString("mainid");
            FundPayment fundPayment = MetaDaoHelper.findById(FUNDPAYMENT_ENTITY_NAME, billId, 2);
            List<FundPayment_b> hasSourceFundPayment_bList = fundPayment.FundPayment_b().stream().filter(item -> billDetailIds.contains(item.getId().toString())).filter(item -> Strings.isNotEmpty(item.getSettledId())).collect(Collectors.toList());
            List<SourceBillNode> upperNodes = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(hasSourceFundPayment_bList)) {
                //有上游单据
                for (FundPayment_b fundPayment_b : hasSourceFundPayment_bList) {
                    List<String> sourceAutoIdList = new ArrayList<>();
                    sourceAutoIdList.add(fundPayment_b.getSettledId());
                    SourceBillNode sourceBillNode = SourceBillNode.builder().billTypeId("500").billDetailIds(sourceAutoIdList).build();
                    upperNodes.add(sourceBillNode);
                }
            }
            billDetailNode.setBillTypeId(billTypeId);
            billDetailNode.setBillDetailIds(billDetailIds);
            billDetailNode.setBillId(billId);
            //单据编号
            billDetailNode.setBillCode(fundPayment.getCode());
            //单据名称
            billDetailNode.setBillName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400437", "资金付款单") /* "资金付款单" */);
            String serviceCode = IServicecodeConstant.FUNDPAYMENT;
            billDetailNode.setServiceCode(serviceCode);

            billDetailNode.setUpperNodes(upperNodes);
            billDetailNode.setCheckStatus(true);
            billDetailNode.setBillNo("cmp_fundpayment");
            billDetailNode.setCheckMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540043A", "检查成功") /* "检查成功" */);
        } catch (Exception ex) {
            log.error("构造billDetailNode报错：" + ex.getMessage());
            billDetailNode.setCheckStatus(false);
            billDetailNode.setCheckMsg(ex.getMessage());
        }
        return billDetailNode;
    }
}
