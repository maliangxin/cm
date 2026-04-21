package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckRpcService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkStock.service.enums.CashType;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpInputBillDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CheckOperationType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.SystemType;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.BillType;
import com.yonyoucloud.fi.cmp.transferaccount.service.ITransferAccountPushService;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 转账单，审批后规则 - 31
 *
 * @author yanglu
 */
@Component
@Slf4j
public class TransferAuditAfterRule extends AbstractCommonRule {

    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    private JournalService journalService;
    @Autowired
    private CmpVoucherService cmpVoucherService;
    @Autowired
    private ITransferAccountPushService iTransferAccountPushService;
    @Autowired
    private CtmCmpCheckRpcService ctmCmpCheckRpcService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private CheckStatusService checkStatusService;

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    /**
     * 转账单审批后规则，审批后的单据根据结算状态以及是否推送资金结算开关，判断单据是直接进行记账生成凭证还是推送资金结算。
     *
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bizObjects = getBills(billContext, map);
        BizObject contextData = bizObjects.get(0);
        AssertUtil.isNotNull(contextData, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400391", "获取审批上下文对象失败....") /* "获取审批上下文对象失败...." */));
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, contextData.getId());
        //判断现金参数是否为是
        Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
        log.info("TransferAuditAfterRule->autoConfigService.getCheckFundTransfer:{}", pushSettlement);
        //如果不推送资金结算则需要判断单据是否是已结算补单的，如果是已结算补单的则需要进行日记账实占和生成凭证
        if (!pushSettlement) {
            //如果属于第三方支付补单的情况
            short payStatus = transferAccount.getPaystatus().getValue();
            if (payStatus == PayStatus.SupplPaid.getValue()) {
                //实占日记账 生成总账凭证
                execJournal(contextData);
            }
            if (!(null == transferAccount.getIsWfControlled() || !transferAccount.getIsWfControlled()) && transferAccount.getCheckid() != null) {
                // 未启动审批流，单据直接审批通过,支票锁定并开票完
                Date today = new Date();
                //根据会计主体id 获取会计主体name
                String accEntityName = (String) QueryBaseDocUtils.queryAccRawEntityByAccEntityId(transferAccount.getAccentity()).get(0).get("name");
                List<CheckDTO> checkDTOChange = this.setValue(transferAccount, today, accEntityName);
                ctmCmpCheckRpcService.checkOperation(checkDTOChange);
            }
            PayStatus paystatus = transferAccount.getPaystatus();
            SettleStatus settlestatus = transferAccount.getSettlestatus();
            if (paystatus != null && paystatus.getValue() == PayStatus.SupplPaid.getValue() && settlestatus != null && settlestatus.getValue() == SettleStatus.SettledRep.getValue()) {
                if (transferAccount.getSettledate() != null) {
                    boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                    if (implement) {
                        transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                        cmpBudgetTransferAccountManagerService.updateOccupyBudget(transferAccount,OccupyBudget.ActualSuccess.getValue());
                        contextData.setPubts(transferAccount.getPubts());
                    }
                }
                // 已结算补单 审批通过即即结算完成，支票状态更新为已兑付
                if (transferAccount.getCheckid() != null) {
                    Long checkId = transferAccount.getCheckid();
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                    // 更新支票状态为“已兑付”
                    checkStock.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());
                    checkStock.setCashDate(transferAccount.getDate("vouchdate"));
                    checkStock.setCashPerson(AppContext.getCurrentUser().getName());//兑付人
                    checkStock.setCashType(CashType.Business.getIndex());//兑付方式
                    checkStock.setCheckpurpose(CheckPurpose.VirtualToBank.getValue());
                    checkStock.setEntityStatus(EntityStatus.Update);
                    checkStatusService.recordCheckStatusByCheckStock(checkStock.getId(), checkStock);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }
            }
            // 更新日记账审批通过状态
            journalService.updateAuditStatusOfJournal(transferAccount.getId(), AuditStatus.Complete);
            //结束流程
            return new RuleExecuteResult();
        }
        //3。现金参数如果为是 则开始调用推送待结算数据
        //组装参数，推送待结算数据
        List<BizObject> bills = new ArrayList<>();
        bills.add(transferAccount);
        log.info("推送待结算数据入参:{}", bills);
        //已传待结算数据的，是否结算处理字段更新为“是”
        transferAccount.setIsSettlement(true);//是否结算处理为是
        //如果是已结算补单，则不更新结算状态，银行流水认领生单同名账户划转时，结算状态是：已结算补单-自动线下支付zxl
        if (transferAccount.getSettlestatus().getValue() != 8) {
            transferAccount.setSettlestatus(SettleStatus.SettleProssing);
        }
        transferAccount.setEntityStatus(EntityStatus.Update);
        String checkBillNo = null;
        if (transferAccount.getCheckid() != null) {
            CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid());
            //支票编号
            checkBillNo = checkStock.getCheckBillNo();
        }

        //判断是否为支票业务
        int settlemode = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
        PayStatus paystatus = transferAccount.getPaystatus();
        SettleStatus settlestatus = transferAccount.getSettlestatus();
        if (paystatus != null && paystatus.getValue() == PayStatus.SupplPaid.getValue() && settlestatus != null && settlestatus.getValue() == SettleStatus.SettledRep.getValue()) {
            if (transferAccount.getSettledate() != null) {
                boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                if (implement) {
                    transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
            }
        }
        // fukk 推结算单
        iTransferAccountPushService.pushBill(bills, true, settlemode, checkBillNo);//推送资金结算
        transferAccount.setIsfirsthandler((short)1);
        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
        //4。结束
        return new RuleExecuteResult();
    }

    /**
     * 实际占用日记账 生成总账凭证
     *
     * @param contextData
     * @throws Exception
     */
    private void execJournal(BizObject contextData) throws Exception {
        Long id = contextData.getId();
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id, 3);
        //这里进行实际的日记账
        transferAccount.setEntityStatus(EntityStatus.Update);
        transferAccount.setPubts(contextData.getDate(IBussinessConstant.PUBTS));
        transferAccount.setPaystatus(PayStatus.SupplPaid);
        transferAccount.setSettleuser(AppContext.getCurrentUser().getId());
//        if (BillInfoUtils.getBusinessDate() != null) {
//            transferAccount.setSettledate(BillInfoUtils.getBusinessDate());
//        } else {
//            transferAccount.setSettledate(AppContext.getLoginDate());
//        }
        transferAccount.setSettlestatus(SettleStatus.SettledRep);
        transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
        journalService.updateJournal(transferAccount);
        CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(transferAccount);
        if (!generateResult.getBoolean("dealSucceed")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102391"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180618", "单据【") /* "单据【" */ + contextData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180617", "】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateResult.get("message"));
        }
        // 更新转账单状态为过账中
        TransferAccount dbTransferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id);
        dbTransferAccount.setVoucherstatus(VoucherStatus.POSTING.getValue());
        dbTransferAccount.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, dbTransferAccount);
    }

    private List<CheckDTO> setValue(TransferAccount transferAccount, Date today, String accEntityName) {
        List<CheckDTO> checkDTOS = new ArrayList<>();

        CheckDTO checkDTO = new CheckDTO();
        //操作类型： 1.支票锁定/解锁接口、2.支票付票接口、3.支票兑付/背书接口、4.支票作废接口
        checkDTO.setOperationType(CheckOperationType.Pay);
        //锁定类型  锁定状态 1锁定 0解锁
        checkDTO.setLock(CmpLock.YES);
        //支票编号ID
        checkDTO.setCheckBillNo(String.valueOf(transferAccount.getCheckid()));
        //业务系统
        checkDTO.setSystem(SystemType.CashManager);
        //业务单据类型
        checkDTO.setBillType(BillType.TransferAccount);
        //业务单据明细ID
        checkDTO.setInputBillNo(transferAccount.getCode());
        //单据方向 2：付款 1：收款(按照结算的枚举来)
        checkDTO.setInputBillDir(CmpInputBillDir.Pay);
        //单据方向，字符串类型
        checkDTO.setInputBillDirString(CmpInputBillDir.Pay.getName());
        //支票状态
        checkDTO.setCheckBillStatus(CmpCheckStatus.BillOver.getValue());
        //支票方向
        checkDTO.setCheckBillDir(CheckDirection.Pay.getIndex());
        //支票用途 默认0提现，1转账
        checkDTO.setCheckPurpose(CheckPurpose.VirtualToBank.getValue());
        //结算日期
        checkDTO.setSettlementDate(today);
        //收款人姓名 = 会计主体
        checkDTO.setPayeeName(accEntityName);
        //金额 = 转账金额
        checkDTO.setAmount(transferAccount.getOriSum());
        //出票人员 = 创建人姓名
        checkDTO.setDrawerName(transferAccount.getCreator());
        //出票日期 = 创建时间
        checkDTO.setDrawerDate(transferAccount.getCreateDate());

        checkDTOS.add(checkDTO);
        return checkDTOS;
    }
}
