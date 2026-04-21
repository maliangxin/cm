package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.BpmRequestBody;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.IBpmForceWithdraw;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * <h1>流程强制删除业务实现</h1>
 *
 * @author rtsungc
 * @version 1.0
 * @file CmpBillBpmForceDeleteImpl
 * @date 2024/4/29 15:50
 */
@Service
public class CmpBillBpmForceDeleteImpl implements IBpmForceWithdraw {
    @Autowired
    private CmCommonService commonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService stwbBillPaymentService;

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    private StwbBillService stwbBillCollectionService;
    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;
    private static final Map<String, String> TABLE_NAME_MAP = new HashMap<>();

    static {
        TABLE_NAME_MAP.put(IBillNumConstant.FUND_PAYMENT, IBillNumConstant.FUND_TABLE_NAME);
        TABLE_NAME_MAP.put(IBillNumConstant.FUND_COLLECTION, IBillNumConstant.FUND_TABLE_NAME);
    }

    private static final Map<String, String> FULL_NAME_MAP = new HashMap<>();

    static {
        FULL_NAME_MAP.put(IBillNumConstant.FUND_PAYMENT, FundPayment.ENTITY_NAME);
        FULL_NAME_MAP.put(IBillNumConstant.FUND_COLLECTION, FundCollection.ENTITY_NAME);
        FULL_NAME_MAP.put(IBillNumConstant.CMP_MYBILLCLAIM_LIST, BillClaim.ENTITY_NAME);
        FULL_NAME_MAP.put(IBillNumConstant.CMP_BILLCLAIM_CARD, BillClaim.ENTITY_NAME);
    }

    @Override
    public void forcewithdraw(BpmRequestBody bpmRequestBody, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String businessKey = bpmRequestBody.getBusinessKey();
        BillDataDto bill = new BillDataDto();
        int pos = businessKey.lastIndexOf("_");
        String billNum;
        String id;
        if (pos <= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100744"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B8F0F9C05E00016", "流程强制删除发生异常,入参处理出错！") /* "流程强制删除发生异常,入参处理出错！" */);
        }
        try {
            billNum = businessKey.substring(0, pos);
            id = businessKey.substring(pos + 1);
            String fullName = FULL_NAME_MAP.get(billNum);
            if (ValueUtils.isEmpty(fullName) || ValueUtils.isEmpty(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100745"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B8F0F9C05E00017", "参数错误,当前单据类型不存在或已被删除【%s】)") /* "参数错误,当前单据类型不存在或已被删除【%s】)" */, billNum));
            }
            BizObject bizObject = MetaDaoHelper.findById(fullName, Long.parseLong(id), 2);
            if (ValueUtils.isEmpty(bizObject)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100746"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B8F0F9C05E00018", "当前单据数据不存在或已被删除【%s】)") /* "当前单据数据不存在或已被删除【%s】)" */, billNum));
            }
            // 增加单据状态查询，如果已经是待提交直接返回
            short vouchStatus = bizObject.getShort(ICmpConstant.VERIFY_STATE);
            if (VerifyState.INIT_NEW_OPEN.getValue() == vouchStatus) {
                return;
            }

            //增加校验，对于认领单，已关联/生单的情况，审批完成的不允许删除流程
            if (BillClaim.ENTITY_NAME.equals(fullName)) {
                if ((bizObject.getShort("associationstatus") != null && bizObject.getShort("associationstatus") == AssociationStatus.Associated.getValue()) ||
                        (bizObject.getShort("refassociationstatus") != null && bizObject.getShort("refassociationstatus") == AssociationStatus.Associated.getValue())){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100746"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21D49E0005400001", "当前认领单已经关联或者生单，不允许该操作【%s】)") /* "当前认领单已经关联或者生单，不允许该操作【%s】)" */, billNum));
                }else {
                    bizObject.set("auditorId", null);
                    bizObject.set("auditor", null);
                    bizObject.set("auditDate", null);
                    bizObject.set("auditTime", null);
                    bizObject.set("recheckstatus", RecheckStatus.Saved.getValue());
                    bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
                    bizObject.set("verifystate", null);
                    EntityTool.setUpdateStatus(bizObject);
                    MetaDaoHelper.update(fullName, bizObject);
                    return;
                }
            }

            // 单据弃审
            if (FundPayment.ENTITY_NAME.equals(fullName)) {
                bizObject.set("_entityName", FundPayment.ENTITY_NAME);
                // 释放资金计划
                commonService.releaseFundPlanByPayment(bizObject);
                // 释放预占
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                    FundPayment fundPayment = new FundPayment();
                    fundPayment.init(bizObject);
                    cmpBudgetManagerService.fundPaymentExecuteAuditDeleteReleaseActual(fundPayment);
                    BizObject bizObjectNew = MetaDaoHelper.findById(fullName, Long.parseLong(id), 2);
                    cmpBudgetManagerService.fundPaymentExecuteAuditDeleteReleasePre(bizObjectNew);
                }
                stwbBillPaymentService.deleteBill(Collections.singletonList(bizObject));
                List<FundPayment_b> fundPaymentSubList = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                for (FundPayment_b fundPaymentB : fundPaymentSubList) {
                    if (fundPaymentB.getFundSettlestatus() != FundSettleStatus.SettlementSupplement){
                        fundPaymentB.setFundSettlestatus(FundSettleStatus.WaitSettle);
                        fundPaymentB.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPaymentB.getFundSettlestatus()));
                        fundPaymentB.setEntityStatus(EntityStatus.Update);
                    }
                    fundPaymentB.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                }
            } else if (FundCollection.ENTITY_NAME.equals(fullName)) {
                bizObject.set("_entityName", FundCollection.ENTITY_NAME);
                // 释放资金计划
                commonService.releaseFundPlanByCollection(bizObject);
                stwbBillCollectionService.deleteBill(Collections.singletonList(bizObject));
                List<FundCollection_b> fundCollectionSubList = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                for (FundCollection_b fundCollectionB : fundCollectionSubList) {
                    if (fundCollectionB.getFundSettlestatus() != FundSettleStatus.SettlementSupplement){
                        fundCollectionB.setFundSettlestatus(FundSettleStatus.WaitSettle);
                        fundCollectionB.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollectionB.getFundSettlestatus()));
                        fundCollectionB.setEntityStatus(EntityStatus.Update);
                    }
                }
            }

            // 删除凭证
            short voucherStatus = bizObject.getShort(ICmpConstant.VOUCHER_STATUS);
            bizObject.put("voucherstatus_original", voucherStatus);
            if (voucherStatus == VoucherStatus.POSTING.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100747"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180775","过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
            }
            if (voucherStatus != VoucherStatus.NONCreate.getValue()
                    && voucherStatus != VoucherStatus.NO_POST.getValue()
                    && voucherStatus != VoucherStatus.TO_BE_POST.getValue()){
                CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResultTry(bizObject);
                if (!deleteResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100358"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803BD","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                }
            }

            bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            bizObject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
            bizObject.set("status", Status.newopen.getValue());
            bizObject.set("voucherNo", null);
            bizObject.set("voucherPeriod", null);
            bizObject.set("voucherId", null);
            bizObject.set("auditorId", null);
            bizObject.set("auditor", null);
            bizObject.set("auditDate", new Date());
            bizObject.set("auditTime", new Date());
            bizObject.set("settleSuccessTime", null);
            bizObject.set("fiEventDataVersion", null);
            bizObject.set("voucherVersion", null);
            bizObject.set("postingMsg", null);
            EntityTool.setUpdateStatus(bizObject);
            MetaDaoHelper.update(fullName, bizObject);

        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100748"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B8F0F9C05E00019", "流程强制删除发生异常, errorMsg：") /* "流程强制删除发生异常, errorMsg：" */ + e.getMessage());
        }

    }

}
