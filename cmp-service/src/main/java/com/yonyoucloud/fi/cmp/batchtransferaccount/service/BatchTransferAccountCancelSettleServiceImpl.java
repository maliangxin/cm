package com.yonyoucloud.fi.cmp.batchtransferaccount.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.error.CtmErrorCode;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillBaseNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillDetailNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.SourceBillNode;
import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.ctm.stwb.unifiedsettle.pubitf.IUnifiedSettlePubService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.CancelSettlementServiceEnum;
import com.yonyoucloud.fi.cmp.common.service.cancelsettle.ICmpOperationService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class BatchTransferAccountCancelSettleServiceImpl implements ICmpOperationService {

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    /**
     * // 判断主表的状态 结算中不用处理  全部成功和部分成功需要处理(删除凭证)
     * // 非现金柜 付方向取消结算 调用结算删除接口
     * // 现金柜 付方向取消结算  调用结算删除接口
     * @param serviceEnum
     * @param billBaseNode
     * @param reason
     * @return
     * @throws Exception
     */
    @Override
    public boolean handleCancelSettle(CancelSettlementServiceEnum serviceEnum, BillBaseNode billBaseNode, String reason) throws Exception {
        log.error("统一结算单取消结算handleCancelSettle（同名账户批量划转），入参：serviceEnum:{},billBaseNode:{},reason:{}", serviceEnum, CtmJSONObject.toJSONString(billBaseNode), reason);
        BatchTransferAccount batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, billBaseNode.getBillId());
        String settlestatus = batchTransferAccount.getSettlestatus();
        List<BatchTransferAccount_b> batchTransferAccount_bList = batchTransferAccount.BatchTransferAccount_b().stream().filter(item -> billBaseNode.getBillDetailIds().contains(item.getId())).collect(Collectors.toList());
        // 处理预算
        processBudget(batchTransferAccount, billBaseNode, batchTransferAccount_bList);
        if (SettleApplyDetailStateEnum.HANDLING.getValue().equals(settlestatus)) {
            log.error("处理结算中场景");
            // 处理非现金柜
            handleNotIsCashBusiness(batchTransferAccount_bList);
            // 处理现金柜
            handleIsCashBusiness(batchTransferAccount_bList);
        } else {
            log.error("处理结算成功或失败的场景");
            // 处理非现金柜
            handleNotIsCashBusiness(batchTransferAccount_bList);
            // 处理现金柜
            handleIsCashBusiness(batchTransferAccount_bList);
            // 删除凭证
            deleteVoucher(batchTransferAccount);
            // 处理主表
            handleBatchTransferAccount(batchTransferAccount);
        }
        return true;
    }

    /**
     * 处理预算
     *
     * @param batchTransferAccount
     * @param billBaseNode
     * @param batchTransferAccount_bList
     */
    public void processBudget(BatchTransferAccount batchTransferAccount, BillBaseNode billBaseNode, List<BatchTransferAccount_b> batchTransferAccount_bList) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        Map<String, BigDecimal> setttleCancelAmountMap = billBaseNode.getCancelAmount();
        List<BatchTransferAccount_b> budgetSuccessList = batchTransferAccount_bList.stream().filter(item -> item.getIsOccupyBudget() != null && item.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(budgetSuccessList)) {
            return;
        }

        Set<String> budgetIds = budgetSuccessList.stream().map(t-> t.getId().toString()).collect(Collectors.toSet());
        // 释放实占 结算平台会把结算成功金额更新成0，需要把结算成功金额还原取消结算前的金额，才可以进行实占的释放。
        for (BatchTransferAccount_b batchTransferAccountB : budgetSuccessList) {
            batchTransferAccountB.setSettleSucAmount(batchTransferAccountB.getSettleSucAmount().add(setttleCancelAmountMap.get(batchTransferAccountB.getId().toString())));
        }
        ResultBudget resultBudgetDelActual = btaCmpBudgetManagerService.cancelSettleOccupyBudget(batchTransferAccount, budgetSuccessList, BatchTransferAccountUtil.SETTLE_SUCCESS_CANCEL_SETTLE);
        if (resultBudgetDelActual.isSuccess()) {
            batchTransferAccount_bList.forEach(t -> {
                if (budgetIds.contains(t.getId().toString())) {
                    // 实占成功，弃审后变为预占成功
                    t.setIsOccupyBudget(resultBudgetDelActual.getBudgeted());
                }
            });
        }
    }

    /**
     * 删除凭证
     * @param batchTransferAccount
     */
    private void deleteVoucher(BatchTransferAccount batchTransferAccount) throws Exception {
        // 删除凭证
        batchTransferAccount.set("_entityName", BatchTransferAccount.ENTITY_NAME);
        batchTransferAccount.set("tradetype", batchTransferAccount.getTradeType());
        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(batchTransferAccount);
        if (!deleteResult.getBoolean("dealSucceed")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102498"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180722","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
        }
    }

    /**
     * 更新主表
     * @param batchTransferAccount
     */
    private void  handleBatchTransferAccount(BatchTransferAccount batchTransferAccount) throws Exception {
        batchTransferAccount.setEntityStatus(EntityStatus.Update);
        batchTransferAccount.setSettleDate(null);
        batchTransferAccount.setSettlestatus(SettleApplyDetailStateEnum.HANDLING.getValue());
        batchTransferAccount.setTransferSucSumAmount(null);
        batchTransferAccount.setTransferSucSumNamount(null);
        batchTransferAccount.setVoucherNo(null);
        batchTransferAccount.setVoucherId(null);
        batchTransferAccount.setVoucherstatus(VoucherStatus.Empty.getValue());
        batchTransferAccount.setVoucherPeriod(null);
        // 更新主表
        MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, batchTransferAccount);
    }

    /**
     * 处理现金柜场景
     * @param batchTransferAccountBList
     */
    private void handleIsCashBusiness(List<BatchTransferAccount_b> batchTransferAccountBList) throws Exception {
        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBList.get(0);
        if (batchTransferAccountB.getIsCashBusiness() == YesOrNoEnum.NO.getValue()) {
            return;
        }
        for (BatchTransferAccount_b item : batchTransferAccountBList) {
            item.setPaySettleStatus(null);
            item.setSettleSucAmount(BigDecimal.ZERO);
            item.setPaySettleSuccessDate(null);
            item.setPaySettleSuccessDateTime(null);
            item.setSettleStopPayAmount(BigDecimal.ZERO);
            item.setRecSettleStatus(SettleApplyDetailStateEnum.HANDLING.getValue());
            item.setRecSettleSuccessDateTime(null);
            item.setRecSettleSuccessDate(null);
            item.setEntityStatus(EntityStatus.Update);
        }
        MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountBList);
    }

    /**
     * 处理非现金柜的场景
     * @param batchTransferAccountBList 同名账户批量划转子表
     * @throws Exception
     */
    private void handleNotIsCashBusiness(List<BatchTransferAccount_b> batchTransferAccountBList) throws Exception {
        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBList.get(0);
        if (batchTransferAccountB.getIsCashBusiness() == YesOrNoEnum.YES.getValue()) {
            return;
        }
        // 非现金柜
        List<String> batchTransferAccountBIdList = new ArrayList<>();
        for (BatchTransferAccount_b item : batchTransferAccountBList) {
            item.setPaySettleStatus(SettleApplyDetailStateEnum.HANDLING.getValue());
            item.setPaySettleSuccessDateTime(null);
            item.setPaySettleSuccessDate(null);
            item.setRecSettleStatus(null);
            item.setRecSettleSuccessDateTime(null);
            item.setRecSettleSuccessDate(null);
            item.setSettleStopPayAmount(BigDecimal.ZERO);
            item.setSettleSucAmount(BigDecimal.ZERO);
            item.setEntityStatus(EntityStatus.Update);
            batchTransferAccountBIdList.add(item.getId());
        }
        MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountBList);
        // 调用结算删除接口 删除生成的协同收款单
        // 调用删除统一结算单接口
        AppContext.getBean(IUnifiedSettlePubService.class).deleteUnifiedSettleByApplyDetailIds(BatchTransferAccount.ENTITY_NAME, batchTransferAccountBIdList.toArray(new String[0]),
                new PushOrder[]{PushOrder.SECOND});
    }

    @Override
    public BillDetailNode buildCancelSettleNode(String billTypeId, List<String> billDetailIds) {
        log.error("统一结算单取消结算buildCancelSettleNode（同名账户划转），入参：billTypeId:{},billDetailIds:{}", billTypeId, billDetailIds);
        BillDetailNode billDetailNode = BillDetailNode.builder().domainKey(IDomainConstant.MDD_DOMAIN_CMP).build();
        try {
            if (CollectionUtils.isEmpty(billDetailIds)) {
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105002"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208855DE04700003", "取消结算,未返回单据明细ID，请联系研发及时处理！！"));
            }
            //获取单据id
            BizObject bizObject = MetaDaoHelper.findById(BatchTransferAccount_b.ENTITY_NAME, billDetailIds.get(0));
            if (bizObject == null) {
                billDetailNode.setCheckStatus(false);
                billDetailNode.setCheckMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400450", "未查询到子表数据，子表id:") /* "未查询到子表数据，子表id:" */ + billDetailIds.get(0));
                throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105003"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_208856DC05A80006", "未查询到子表数据，子表id:[%s]"), billDetailIds.get(0)));
            }
            String billId = bizObject.getString("mainid");
            BatchTransferAccount batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, billId, 2);
            //List<BatchTransferAccount_b> hasSourceBatchTransferAccount_bList = batchTransferAccount.BatchTransferAccount_b().stream().filter(item -> billDetailIds.contains(item.getId().toString())).filter(item -> Strings.isNotEmpty(item.getSettledId())).collect(Collectors.toList());
            List<SourceBillNode> upperNodes = new ArrayList<>();
            billDetailNode.setBillTypeId(billTypeId);
            billDetailNode.setBillDetailIds(billDetailIds);
            billDetailNode.setBillId(billId);
            //单据编号
            billDetailNode.setBillCode(batchTransferAccount.getCode());
            //单据名称
            billDetailNode.setBillName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540044F", "批量同名账户划转") /* "批量同名账户划转" */);
            String serviceCode = IServicecodeConstant.BATCH_TRANSFERACCOUNT;
            billDetailNode.setServiceCode(serviceCode);

            billDetailNode.setUpperNodes(upperNodes);
            billDetailNode.setCheckStatus(true);
            billDetailNode.setCheckMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400451", "检查成功") /* "检查成功" */);
        } catch (Exception ex) {
            log.error("构造billDetailNode报错：" + ex.getMessage());
            billDetailNode.setCheckStatus(false);
            billDetailNode.setCheckMsg(ex.getMessage());
        }
        return billDetailNode;
    }
}
