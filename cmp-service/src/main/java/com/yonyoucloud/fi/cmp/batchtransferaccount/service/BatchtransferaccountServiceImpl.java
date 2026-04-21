package com.yonyoucloud.fi.cmp.batchtransferaccount.service;

import com.yonyou.business_flow.dto.DomainMakeBillRuleModel;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.vo.EventAsyncResultVO;
import com.yonyou.ucf.basedoc.model.CurrencyDTO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import com.yonyou.yonbip.ctm.settleapply.processor.PushSettleParam;
import com.yonyou.yonbip.ctm.settleapply.processor.PushSettleProcessor;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyou.ypd.bizflow.dto.ConvertParam;
import com.yonyou.ypd.bizflow.dto.ConvertResult;
import com.yonyou.ypd.bizflow.service.BusinessConvertService;
import com.yonyoucloud.ctm.stwb.common.app.Application;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDistribute;
import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettlementResult;
import com.yonyoucloud.ctm.stwb.unifiedsettle.enums.SettleDetailSettleStateEnum;
import com.yonyoucloud.ctm.stwb.unifiedsettle.enums.UnifiedSettleBusiTypeEnum;
import com.yonyoucloud.ctm.stwb.unifiedsettle.event.SettleDetailFinishEvent;
import com.yonyoucloud.ctm.stwb.unifiedsettle.vo.UnifiedSettleDetail;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.EventCenterEnum;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xuxbo
 * @date 2025/6/9 19:17
 */
@Service
@Slf4j
public class BatchtransferaccountServiceImpl implements BatchtransferaccountService {
    @Autowired
    private IApplicationService appService;
    @Autowired
    private ICmpSendEventService cmpSendEventService;
    @Autowired
    private PushSettleProcessor pushSettleProcessor;
    @Autowired
    private BusinessConvertService businessConvertService;
    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;

    @Autowired
    private CmpVoucherService cmpVoucherService;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    /**
     * 更新批量同名账户划转的状态
     *
     * @param businessEvent           业务事件
     * @param unifiedSettleDetail     结算申请数据
     * @param settleDetailFinishEvent 结算申请数据
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventAsyncResultVO updateSettledInfoOfBatchtransferaccount(BusinessEvent businessEvent, UnifiedSettleDetail unifiedSettleDetail, SettleDetailFinishEvent settleDetailFinishEvent) throws Exception {
        EventAsyncResultVO eventAsyncResultVO = new EventAsyncResultVO();
        eventAsyncResultVO.setEventId(businessEvent.getUuid());
        BatchTransferAccount_b batchTransferAccountB = MetaDaoHelper.findById(BatchTransferAccount_b.ENTITY_NAME, unifiedSettleDetail.getBizbilldetailid());
        if (batchTransferAccountB == null) {
            log.error("SettleDetailFinishCallbackServiceImpl.onEvent batchTransferAccountB is null,param BatchTransferAccount_b id:{}", unifiedSettleDetail.getBizbilldetailid());
            throw new com.yonyou.yonbip.ctm.error.CtmException("SettleDetailFinishCallbackServiceImpl.onEvent batchTransferAccountB is null");
        }
        // 非现金柜，付方向不走此事件，防止重发第二笔收方向的结算单
        if (settleDetailFinishEvent.getPushOrder() == PushOrder.FIRST && batchTransferAccountB.getIsCashBusiness() == YesOrNoEnum.NO.getValue()) {
            eventAsyncResultVO.setSuccess(true);
            return eventAsyncResultVO;
        }
        try {
            String lockey = EventCenterEnum.EVENT_SETTLEDETAIL_FINISHEVENT.getEventType() + unifiedSettleDetail.getBizbillid();
            CtmLockTool.executeInOneServiceLock(lockey, 120L, 60L, TimeUnit.SECONDS, (int lockstatus) -> {
                if (lockstatus == LockStatus.GETLOCK_FAIL) {
                    // 加锁失败
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"));
                }
                PushOrder pushOrder = settleDetailFinishEvent.getPushOrder();
                // 现金柜场景  pushOrder 1付；2收
                if (batchTransferAccountB.getIsCashBusiness() == YesOrNoEnum.YES.getValue()) {
                    if (pushOrder == PushOrder.FIRST) {
                        pushOrder = PushOrder.SECOND;
                    } else {
                        pushOrder = PushOrder.FIRST;
                    }
                }
                // 优化查询，在循环体外江主表信息提前查询出
                Long mainId = batchTransferAccountB.getMainid();
                // 查询主子表数据
                BatchTransferAccount batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, mainId);
                switch (pushOrder) {
                    // 第一次办结
                    case FIRST:
                        handleFirstSettle(unifiedSettleDetail, batchTransferAccount, batchTransferAccountB);
                        break;
                    // 第二次办结
                    case SECOND:
                        handleSecondSettle(unifiedSettleDetail, batchTransferAccount, batchTransferAccountB);
                        break;
                    default:
                        break;
                }
                eventAsyncResultVO.setSuccess(true);
            });
        } catch (Exception e) {
            log.error("SettleDetailFinishCallbackServiceImpl.onEvent error:{}", e);
            eventAsyncResultVO.setSuccess(false);
            eventAsyncResultVO.setMsg(e.getMessage());
            throw new CtmException("SettleDetailFinishCallbackServiceImpl.onEvent error:" + e.getMessage(), e);
        }
        return eventAsyncResultVO;
    }

    /**
     * 处理第二次结算 协同生单已结算补单 不存在失败的场景
     *
     * @param unifiedSettleDetail
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void handleSecondSettle(UnifiedSettleDetail unifiedSettleDetail, BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        // 结算成功
        handleSecondSettleSuccess(unifiedSettleDetail, batchTransferAccount, batchTransferAccountB);
    }
    /**
     * 处理第二次结算成功
     *
     * @param unifiedSettleDetail
     * @param batchTransferAccount
     * @param batchTransferAccountB
     */
    private void handleSecondSettleSuccess(UnifiedSettleDetail unifiedSettleDetail, BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (SettleDetailSettleStateEnum.SETTLE_SUCCESS.getValue() == unifiedSettleDetail.getStatementdetailstatus()
                && SettleApplyDetailStateEnum.HANDLING.getValue().equals(batchTransferAccount.getSettlestatus())) {
            // 判断收付方向  待结算数据收付类型：1收/2付
            if (Direction.Credit.getValue() == unifiedSettleDetail.getReceipttypeb().shortValue()) {
                //预算占用
                settleSuccessOccupyBudget(batchTransferAccount, batchTransferAccountB);
                //付款
                updateBatchTaBOfPay(batchTransferAccountB, unifiedSettleDetail);
            } else if (Direction.Debit.getValue() == unifiedSettleDetail.getReceipttypeb().shortValue()) { //收款
                //判断收款结算状态是否办结
                if (!SettleApplyDetailStateEnum.ALL_SUCCESS.getValue().equals(batchTransferAccountB.getRecSettleStatus())) {//收款未办结
                    updateBatchTaBOfReceive(batchTransferAccountB, unifiedSettleDetail);
                }
            }

            // 更新同名账户划转子表的结算状态
            batchTransferAccountB.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountB);
            // 根据子表的收付结算状态更新主表的结算状态
            updateMainBySubs(batchTransferAccount, batchTransferAccountB);
        } else {
            log.error("第二次已结算补单，协同生单失败:{}", CtmJSONObject.toJSONString(unifiedSettleDetail));
        }
    }

    /**
     * 处理第一次办结
     *
     * @param unifiedSettleDetail
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void handleFirstSettle(UnifiedSettleDetail unifiedSettleDetail, BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        // 结算成功
        handleFirstSettleSuccess(unifiedSettleDetail, batchTransferAccount, batchTransferAccountB);
        // 结算失败
        handleSettleFail(unifiedSettleDetail, batchTransferAccount, batchTransferAccountB);

    }

    /**
     * 处理结算失败
     *
     * @param unifiedSettleDetail
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void handleSettleFail(UnifiedSettleDetail unifiedSettleDetail, BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        // 结算止付
        if (SettleDetailSettleStateEnum.STOP_PAY.getValue() == unifiedSettleDetail.getStatementdetailstatus()) {
            if (Objects.equals(batchTransferAccount.getSettlestatus(), SettleApplyDetailStateEnum.HANDLING.getValue())) {
                switch (unifiedSettleDetail.getReceipttypeb()) {
                    // 收款
                    case 1:
                        batchTransferAccountB.setRecSettleStatus(SettleApplyDetailStateEnum.ALL_FAIL.getValue());
                        batchTransferAccountB.setPaySettleStatus(SettleApplyDetailStateEnum.ALL_FAIL.getValue());
                        break;
                    // 付款
                    case 2:
                        // 结算止付金额=转账金额 全部失败
                        if (batchTransferAccountB.getOriSum().compareTo(batchTransferAccountB.getSettleStopPayAmount()) == 0) {
                            batchTransferAccountB.setPaySettleStatus(SettleApplyDetailStateEnum.ALL_FAIL.getValue());
                            batchTransferAccountB.setRecSettleStatus(SettleApplyDetailStateEnum.ALL_FAIL.getValue());
                        }
                        //预算释放
                        stopSettleOccupyBudget(batchTransferAccount, batchTransferAccountB);
                        break;
                    default:
                        break;
                }
                // 更新同名账户划转子表的结算状态
                batchTransferAccountB.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountB);
                // 根据子表的收付结算状态更新主表的结算状态
                updateMainBySubs(batchTransferAccount, batchTransferAccountB);
            }
        }
    }

    /**
     * 处理第一次结算成功
     *
     * @param unifiedSettleDetail
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void handleFirstSettleSuccess(UnifiedSettleDetail unifiedSettleDetail, BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (SettleDetailSettleStateEnum.SETTLE_SUCCESS.getValue() == unifiedSettleDetail.getStatementdetailstatus()
                && SettleApplyDetailStateEnum.HANDLING.getValue().equals(batchTransferAccount.getSettlestatus())) {
            // 判断收付方向  待结算数据收付类型：1收/2付
            // 付款结算成功  转账金额=结算成功金额
            if (Direction.Credit.getValue() == unifiedSettleDetail.getReceipttypeb().shortValue()
                    && batchTransferAccountB.getOriSum().compareTo(batchTransferAccountB.getSettleSucAmount()) == 0) { //付款
                updateBatchTaBOfPay(batchTransferAccountB, unifiedSettleDetail);
                if (!SettleApplyDetailStateEnum.ALL_SUCCESS.getValue().equals(batchTransferAccountB.getRecSettleStatus())) {
                    // 收款结算状态-结算中
                    batchTransferAccountB.setRecSettleStatus(SettleApplyDetailStateEnum.HANDLING.getValue());
                    // 占用预算
                    settleSuccessOccupyBudget(batchTransferAccount, batchTransferAccountB);
                    // 期望结算日期为第一笔结算的成功日期
                    batchTransferAccountB.setExpectSettleDate(DateUtils.dateFormat(batchTransferAccountB.getPaySettleSuccessDate(), DateUtils.DATE_PATTERN));
                    // 更新同名账户划转子表的结算状态
                    batchTransferAccountB.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountB);
                    // 推送第二笔收款方向结算单 正常推送 非现金柜业务
                    log.error("推送第二笔收款方向结算单开始，参数{}:", batchTransferAccount.getId().toString());
                    pushPaymentSettleSecond(batchTransferAccount, unifiedSettleDetail.getBizbilldetailid(), false);
                    log.error("推送第二笔收款方向结算单结束，参数{}:", batchTransferAccount.getId().toString());
                }
            } else if (Direction.Debit.getValue() == unifiedSettleDetail.getReceipttypeb().shortValue()) { //收款
                // 判断收款结算状态是否办结
                if (!SettleApplyDetailStateEnum.ALL_SUCCESS.getValue().equals(batchTransferAccountB.getRecSettleStatus())) {//收款未办结
                    updateBatchTaBOfReceive(batchTransferAccountB, unifiedSettleDetail);
                    if (!Objects.equals(batchTransferAccountB.getPaySettleStatus(), SettleApplyDetailStateEnum.ALL_SUCCESS.getValue())) {
                        // 推送第二笔付款方向结算单 现金柜业务
                        log.error("推送第二笔付款方向结算单开始，参数{}:", batchTransferAccount.getId().toString());
                        // 更新同名账户划转子表的结算状态
                        batchTransferAccountB.setPaySettleStatus(SettleApplyDetailStateEnum.WAIT_HANDLE.getValue());
                        // 期望结算日期为第一笔结算的成功日期
                        batchTransferAccountB.setExpectSettleDate(DateUtils.dateFormat(batchTransferAccountB.getRecSettleSuccessDate(), DateUtils.DATE_PATTERN));
                        batchTransferAccountB.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountB);
                        pushPaymentSettleSecond(batchTransferAccount, unifiedSettleDetail.getBizbilldetailid(), true);
                        log.error("推送第二笔付款方向结算单结束，参数{}:", batchTransferAccount.getId().toString());
                    }
                }
            }
        }
    }

    /**
     * 更新收款方向数据
     *
     * @param batchTransferAccountB
     * @param unifiedSettleDetail
     */
    private void updateBatchTaBOfReceive(BatchTransferAccount_b batchTransferAccountB, UnifiedSettleDetail unifiedSettleDetail) {
        // 收款结算状态
        batchTransferAccountB.setRecSettleStatus(SettleApplyDetailStateEnum.ALL_SUCCESS.getValue());
        // 财资统一对账码
        batchTransferAccountB.setRecSmartCheckNo(unifiedSettleDetail.getCheckIdentificationCode());
        // 收款结算成功日期
        batchTransferAccountB.setRecSettleSuccessDate(unifiedSettleDetail.getOperationDate());
        // 收款结算成功时间
        batchTransferAccountB.setRecSettleSuccessDateTime(unifiedSettleDetail.getOperationTime());
    }

    /**
     * 更新付款方向的数据
     *
     * @param batchTransferAccountB
     * @param unifiedSettleDetail
     */
    private void updateBatchTaBOfPay(BatchTransferAccount_b batchTransferAccountB, UnifiedSettleDetail unifiedSettleDetail) {
        // 付款结算状态
        batchTransferAccountB.setPaySettleStatus(SettleApplyDetailStateEnum.ALL_SUCCESS.getValue());
        // 财资统一对账码
        batchTransferAccountB.setPaySmartCheckNo(unifiedSettleDetail.getCheckIdentificationCode());
        // 付款结算成功日期
        batchTransferAccountB.setPaySettleSuccessDate(unifiedSettleDetail.getOperationDate());
        // 付款结算成功时间
        batchTransferAccountB.setPaySettleSuccessDateTime(unifiedSettleDetail.getOperationTime());
    }

    /**
     * 根据子表的结算状态更新主表的结算状态
     *
     * @param batchTransferAccount  主表
     * @param batchTransferAccountB 子表
     * @throws Exception
     */
    public void updateMainBySubs(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        Long mainid = batchTransferAccountB.getMainid();
        QuerySchema querySchema = QuerySchema.create().addSelect("id, paySettleStatus, recSettleStatus, settleSucAmount, paySettleSuccessDate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.MAINID).eq(mainid));
        querySchema.addCondition(conditionGroup);
        List<BatchTransferAccount_b> batchTransferAccountBList = MetaDaoHelper.queryObject(BatchTransferAccount_b.ENTITY_NAME, querySchema, null);
        log.error("批量划转账户结算单主表更新id:{}，batchTransferAccountBList:{}", batchTransferAccount.getId().toString(), CtmJSONObject.toJSONString(batchTransferAccountBList));
        Map<String, String> successMap = new ConcurrentHashMap<>();
        Map<String, String> failMap = new ConcurrentHashMap<>();
        Map<String, String> partMap = new ConcurrentHashMap<>();
        boolean updateFailFlag = false;
        boolean updateSuccessFlag = false;
        boolean updatePartSuccessFlag = false;
        BigDecimal transferSucTotalAmount = BigDecimal.ZERO;
        Date firstSettleSuccessDate = null;
        int i = 0;
        for (BatchTransferAccount_b transferAccountB : batchTransferAccountBList) {
            // 全部成功
            if (SettleApplyDetailStateEnum.ALL_SUCCESS.getValue().equals(transferAccountB.getRecSettleStatus())
                    && SettleApplyDetailStateEnum.ALL_SUCCESS.getValue().equals(transferAccountB.getPaySettleStatus())) {
                successMap.put(transferAccountB.getId(), transferAccountB.getId());
                transferSucTotalAmount = transferSucTotalAmount.add(transferAccountB.getSettleSucAmount());
                if (i == 0) {
                    firstSettleSuccessDate = transferAccountB.getPaySettleSuccessDate();
                    i++;
                }
                Date tmpDate = transferAccountB.getPaySettleSuccessDate();
                if (tmpDate != null && tmpDate.compareTo(firstSettleSuccessDate) > 0) {
                    firstSettleSuccessDate = tmpDate;
                }
            } else if (SettleApplyDetailStateEnum.ALL_FAIL.getValue().equals(transferAccountB.getRecSettleStatus())
                    || SettleApplyDetailStateEnum.ALL_FAIL.getValue().equals(transferAccountB.getPaySettleStatus())) {
                // 全部失败
                failMap.put(transferAccountB.getId(), transferAccountB.getId());
            } else if (SettleApplyDetailStateEnum.PART_SUCCESS.getValue().equals(transferAccountB.getPaySettleStatus())
                    && SettleApplyDetailStateEnum.ALL_SUCCESS.getValue().equals(transferAccountB.getRecSettleStatus())) {
                // 部分成功
                partMap.put(transferAccountB.getId(), transferAccountB.getId());
                transferSucTotalAmount = transferSucTotalAmount.add(transferAccountB.getSettleSucAmount());
                if (i == 0) {
                    firstSettleSuccessDate = transferAccountB.getPaySettleSuccessDate();
                    i++;
                }
                Date tmpDate = transferAccountB.getPaySettleSuccessDate();
                if (tmpDate != null && tmpDate.compareTo(firstSettleSuccessDate) > 0) {
                    firstSettleSuccessDate = tmpDate;
                }
            }
        }
        int successCount = successMap.size();
        int failCount = failMap.size();
        int partCount = partMap.size();
        int totalCount = batchTransferAccountBList.size();
        log.error("批量划转账户结算单主表更新id:{}:successCount:{},partCount:{},failCount:{}, totalCount:{}:",
                batchTransferAccount.getId(), successCount, partCount, failCount, totalCount);
        if (successCount + failCount + partCount < totalCount) {
            log.error("批量划转账户结算单主表更新失败id:{}:successCount:{},partCount:{},failCount:{}, totalCount:{}:",
                    batchTransferAccount.getId(), successCount, partCount, failCount, totalCount);
            return;
        }
        if (successCount + failCount + partCount == totalCount) {
             if (successCount == totalCount){
                updateSuccessFlag = true;
            } else if (failCount == totalCount) {
                updateFailFlag = true;
            } else {
                updatePartSuccessFlag = true;
            }
        }
        log.error("批量划转账户结算单主表更新成功id:{}:successCount:{},partCount:{},failCount:{}, totalCount:{}:",
                batchTransferAccount.getId(), successCount, partCount, failCount, totalCount);
        BatchTransferAccount dbBatchTransferAccount = new BatchTransferAccount();
        if (updateSuccessFlag) {
            // 生成凭证
            generateVoucher(batchTransferAccount, BatchTransferAccount.ENTITY_NAME);
            dbBatchTransferAccount.setVoucherstatus(VoucherStatus.POSTING.getValue());
            dbBatchTransferAccount.setSettlestatus(SettleApplyDetailStateEnum.ALL_SUCCESS.getValue());
            // 结算成功日期-取本方结算成功日期最大的
            dbBatchTransferAccount.setSettleDate(firstSettleSuccessDate);
        } else {
            if (updateFailFlag) {
                dbBatchTransferAccount.setSettlestatus(SettleApplyDetailStateEnum.ALL_FAIL.getValue());
                dbBatchTransferAccount.setVoucherstatus(VoucherStatus.NONCreate.getValue());
            } else if (updatePartSuccessFlag) {
                generateVoucher(batchTransferAccount, BatchTransferAccount.ENTITY_NAME);
                dbBatchTransferAccount.setSettlestatus(SettleApplyDetailStateEnum.PART_SUCCESS.getValue());
                // 结算成功日期-取本方结算成功日期最大的
                dbBatchTransferAccount.setSettleDate(firstSettleSuccessDate);
            }
        }
        // 转账成功总金额
        dbBatchTransferAccount.setTransferSucSumAmount(transferSucTotalAmount);
        if (batchTransferAccount.getCurrency().equals(batchTransferAccount.getNatCurrency())) {
            dbBatchTransferAccount.setTransferSucSumNamount(transferSucTotalAmount);
        } else {
            // 转账成功本币总金额
            CurrencyDTO currencyDTO = CurrencyUtil.getCurrency(batchTransferAccount.getNatCurrency());
            dbBatchTransferAccount.setTransferSucSumNamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(batchTransferAccount.getExchRateOps(),
                            batchTransferAccount.getExchRate(), transferSucTotalAmount, null)
                    .setScale(currencyDTO.getMoneydigit(), currencyDTO.getMoneyrount()));
        }
        dbBatchTransferAccount.setEntityStatus(EntityStatus.Update);
        dbBatchTransferAccount.setId(batchTransferAccount.getId());
        dbBatchTransferAccount.setPubts(batchTransferAccount.getPubts());
        MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, dbBatchTransferAccount);
    }


    /**
     * 生成凭证
     *
     * @param batchTransferAccount 主表
     * @param entityName           实体名
     * @throws Exception
     */
    @Override
    public void generateVoucher(BatchTransferAccount batchTransferAccount, String entityName) throws Exception {
        log.error("generator Voucher enyityName= {}, batchTransferAccount = {}", entityName, CtmJSONObject.toJSONString(batchTransferAccount));
        String lockKey = "_generateVoucher_" + batchTransferAccount.getId().toString();
        CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 枷锁失败
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102160"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B5", "结算成功后过账加锁失败！") /* "结算成功后过账加锁失败！" */);
            }
            try {
                if (checkAppStatus()) return;

                // 新财务标识
                if (CmpCommonUtil.getNewFiFlag()) {
                    BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
                    CtmJSONObject billClue = new CtmJSONObject();
                    billClue.put("srcBusiId", dbBatchTransferAccount.getId().toString());
                    billClue.put("srcBusiMainId", dbBatchTransferAccount.getId().toString());
                    billClue.put("classifier", null);
                    billClue.put("billVersion", dbBatchTransferAccount.getPubts().getTime());
                    dbBatchTransferAccount.put("_entityName", BatchTransferAccount.ENTITY_NAME);
                    cmpSendEventService.sendSimpleEvent(dbBatchTransferAccount, billClue);
                    log.error("settle write back, send event generate voucher success!, bizObject = {}", batchTransferAccount);
                }
            } catch (Exception e) {
                log.error("update bill settle success time fail, id={}", batchTransferAccount.getId().toString(), e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D7", "批量同名账户划转结算成功后过账异常！") /* "批量同名账户划转结算成功后过账异常！" */, e);
            }
        });
    }

    /**
     * 生成凭证
     *
     * @param businessBillId
     * @throws Exception
     */
    @Override
    public void generateVoucher(String businessBillId) throws Exception {
        log.error("generator Voucher enyityName= {}, batchTransferAccount = {}", businessBillId);
        String lockKey = "_generateVoucher_" + businessBillId;
        CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 加锁失败
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102160"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B5", "结算成功后过账加锁失败！") /* "结算成功后过账加锁失败！" */);
            }
            try {
                // 先校验应用开通情况
                if (checkAppStatus()) {
                    return;
                }
                BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, businessBillId);
                List<BatchTransferAccount_b> batchTransferAccountBs = dbBatchTransferAccount.BatchTransferAccount_b();
                // 只要有一个状态不是完结，就返回
                for (BatchTransferAccount_b batchTransferAccount_b : batchTransferAccountBs) {
                    if (batchTransferAccount_b.getRecSettleStatus() == SettleApplyDetailStateEnum.WAIT_HANDLE.getValue() || batchTransferAccount_b.getRecSettleStatus() == null
                            || batchTransferAccount_b.getPaySettleStatus() == SettleApplyDetailStateEnum.WAIT_HANDLE.getValue() || batchTransferAccount_b.getPaySettleStatus() == null) {
                        return;
                    }
                }
                // 新财务标识
                if (CmpCommonUtil.getNewFiFlag()) {
                    CtmJSONObject billClue = new CtmJSONObject();
                    billClue.put("srcBusiId", dbBatchTransferAccount.getId().toString());
                    billClue.put("srcBusiMainId", dbBatchTransferAccount.getId().toString());
                    billClue.put("classifier", null);
                    billClue.put("billVersion", dbBatchTransferAccount.getPubts().getTime());
                    dbBatchTransferAccount.put("_entityName", BatchTransferAccount.ENTITY_NAME);
                    cmpSendEventService.sendSimpleEvent(dbBatchTransferAccount, billClue);
                    log.error("settle write back, send event generate voucher success!, bizObject = {}", dbBatchTransferAccount);
                }
            } catch (Exception e) {
                log.error("update bill settle success time fail, id={}", businessBillId, e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D7", "批量同名账户划转结算成功后过账异常！") /* "批量同名账户划转结算成功后过账异常！" */, e);
            }
        });
    }

    /**
     * 校验应用的状态
     *
     * @return
     */
    private boolean checkAppStatus() {
        boolean enableFP = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EPUB_FP_APP_CODE);
        if (!enableFP) {
            log.error("客户环境未安装财务公共服务");
            return true;
        }
        boolean enableGL = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EGL_APP_CODE);
        if (!enableGL) {
            log.error("客户环境未安装总账服务");
            return true;
        }
        boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EGL_APP_CODE);
        if (!enableEVNT) {
            log.error("客户环境未安装事项中台服务");
            return true;
        }
        return false;
    }

    /**
     * 第一次推送结算
     *
     * @param batchTransferAccount 主表
     * @param isCashBusiness       是否现金柜
     * @throws Exception
     */
    @Override
    public void pushPaymentSettleFirst(BatchTransferAccount batchTransferAccount, Boolean isCashBusiness) throws Exception {
        List<Map<String, Object>> settleMap = new ArrayList<>();
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
        if (!isCashBusiness) {
            settleMap = buildSettleMap(dbBatchTransferAccount, "batchtransferaccountPay2SettleBench", "ctm-cmp.cmp_batchtransferaccount");
        } else {
            settleMap = buildSettleMap(dbBatchTransferAccount, "batchtransferaccountRecen2SettleBench", "ctm-cmp.cmp_batchtransferaccount");
            // 现金柜场景 交易类型传协同收款
            settleMap.get(0).put("bustype", UnifiedSettleBusiTypeEnum.COLLABORATICE_COLLECTION.getId());
        }
        for (Map<String, Object> map : settleMap) {
            pushSettleBill(buildPushSettleParam(dbBatchTransferAccount, isCashBusiness ? PushOrder.SECOND : PushOrder.FIRST), map);
        }
    }

    /**
     * 构建推送结算参数
     *
     * @param batchTransferAccount 主表
     * @return
     */
    private PushSettleParam buildPushSettleParam(BatchTransferAccount batchTransferAccount, PushOrder pushOrder) {
        Map<String, Date> detailIdMapPubts = new HashMap<>();
        List<BatchTransferAccount_b> batchTransferAccountBList = batchTransferAccount.BatchTransferAccount_b();
        for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccountBList) {
            detailIdMapPubts.put(batchTransferAccountB.getId().toString(), batchTransferAccountB.getPubts());
        }
        return PushSettleParam.builder().pushOrder(pushOrder)
                .billDate(batchTransferAccount.getBillDate())
                .settleApplyUri(BatchTransferAccount.ENTITY_NAME)
                .id(batchTransferAccount.getId().toString())
                .code(batchTransferAccount.getCode())
                .pubts(batchTransferAccount.getPubts())
                .detailIdMapPubts(detailIdMapPubts).build();

    }

    /**
     * 第二次推送结算
     *
     * @param batchTransferAccount 主表
     * @param isCashBusiness       是否现金柜
     * @throws Exception
     */
    @Override
    public void pushPaymentSettleSecond(BatchTransferAccount batchTransferAccount, String bizbilldetailid, Boolean isCashBusiness) throws Exception {
        List<Map<String, Object>> settleMapList;
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());

        // 收款
        int direction = 1;
        if (!isCashBusiness) {
            settleMapList = buildSettleMap(dbBatchTransferAccount, "batchtransferaccountRecen2SettleBench", "ctm-cmp.cmp_batchtransferaccount");
        } else {
            settleMapList = buildSettleMap(dbBatchTransferAccount, "batchtransferaccountPay2SettleBench", "ctm-cmp.cmp_batchtransferaccount");
            // 付款
            direction = 2;
        }

        // 根据办结的结算单过滤同名账户批量划转的子表数据
        filterSubsBySettleDetailId(dbBatchTransferAccount, bizbilldetailid);
        Map<String, Object> settleMap = settleMapList.get(0);
        // 过滤单据转换规则后的数据
        filterSettleMap(settleMap, direction, bizbilldetailid);
        // 构建第二笔推送的额度 取结算成功金额
        buildSecondPushAmount(settleMap, dbBatchTransferAccount, isCashBusiness);
        // 推送第二笔结算
        // pushOrder 1 付，2 收
        pushSettleBill(buildPushSettleParam(dbBatchTransferAccount, isCashBusiness ? PushOrder.FIRST : PushOrder.SECOND), settleMap);
    }

    /**
     * 构建第二笔推送的额度 取结算成功金额-部分结算成功场景
     * @param settleMap
     * @param dbBatchTransferAccount
     * @param isCashBusiness
     * @throws Exception
     */
    private void buildSecondPushAmount(Map<String, Object> settleMap, BatchTransferAccount dbBatchTransferAccount, Boolean isCashBusiness) throws Exception {
        List<BatchTransferAccount_b> batchTransferAccountBList = dbBatchTransferAccount.BatchTransferAccount_b();
        BatchTransferAccount_b accountB = batchTransferAccountBList.get(0);
        // 非部分成功场景直接返回
        if (!SettleApplyDetailStateEnum.PART_SUCCESS.getValue().equals(accountB.getPaySettleStatus())) {
            return;
        }
        // 现金柜直接返回
        if (isCashBusiness) {
            return;
        }

        Map<String, BatchTransferAccount_b> batchTransferAccountBMap = batchTransferAccountBList.stream().collect(Collectors.toMap(BatchTransferAccount_b::getId, Function.identity()));
        List<Map<String, Object>> settleBenchBenchList = (List<Map<String, Object>>) settleMap.get("settleBench_b");
        String bizbilldetailid;
        BatchTransferAccount_b batchTransferAccountB;
        for (Map<String, Object> settleBench : settleBenchBenchList) {
            bizbilldetailid = settleBench.get("bizbilldetailid").toString();
            batchTransferAccountB = batchTransferAccountBMap.get(bizbilldetailid);
            // 预估结算金额
            settleBench.put("exchangePaymentAmount", batchTransferAccountB.getSettleSucAmount());
            // 原币金额
            settleBench.put("originalcurrencyamt", batchTransferAccountB.getSettleSucAmount());
            // 本币金额
            settleBench.put("natAmt", currencyQueryService.getAmountOfCurrencyPrecision(dbBatchTransferAccount.getNatCurrency(), batchTransferAccountB.getSettleSucAmount()));
        }
    }

    /**
     * 撤回
     *
     * @param batchTransferAccountId 主表id
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeBatchTransferAccount(String batchTransferAccountId) throws Exception {
        if (StringUtils.isEmpty(batchTransferAccountId)) {
            throw new CtmException("param error");
        }
        BatchTransferAccount currentBill = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, batchTransferAccountId);
        if (currentBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101247"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029F", "单据不存在 id:") /* "单据不存在 id:" */ + batchTransferAccountId);
        }

        // 删除凭证
        currentBill.set("_entityName", BatchTransferAccount.ENTITY_NAME);
        currentBill.set("tradetype", currentBill.getTradeType());
        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(currentBill);
        if (!deleteResult.getBoolean("dealSucceed")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102498"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180722", "删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
        }
        // 单据数据可能被修改，重新查询数据库
        updateBatchTransferAccountAndBStatus(currentBill);

    }

    /**
     *
     * 更新结算信息
     * @param dataSettledDetail
     * @throws Exception
     */
    @Override
    public void updateSettledInfo(DataSettledDetail dataSettledDetail) throws Exception {
        BatchTransferAccount_b billb = MetaDaoHelper.findById(BatchTransferAccount_b.ENTITY_NAME, dataSettledDetail.getBusinessDetailsId());
        // 同名账户批量划转
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, dataSettledDetail.getBusinessBillId());
        // 主表状态是结算中才更新子表的状态
        if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(dbBatchTransferAccount.getSettlestatus())) {
            return;
        }
        // 更新结算成功的时间和日期
        updateSettleSuccessDateTime(billb, dataSettledDetail);
        // 处理全部成功
        handleAllSuccess(billb, dbBatchTransferAccount, dataSettledDetail);
        // 处理全部失败
        handleAllFail(billb,dbBatchTransferAccount, dataSettledDetail);
        // 处理部分成功
        handlePartSuccess(billb, dbBatchTransferAccount, dataSettledDetail);
        // 根据子表的收付结算状态更新主表的结算状态
        updateMainBySubs(dbBatchTransferAccount, billb);
    }

    /**
     * 处理全部失败
     * @param billb 同名账户批量划转子表
     * @param batchTransferAccount 同名账户划转主表
     * @param dataSettledDetail 待结算明细
     * @throws Exception
     */
    private void handleAllFail(BatchTransferAccount_b billb, BatchTransferAccount batchTransferAccount, DataSettledDetail dataSettledDetail) throws Exception {
        if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {//全部失败
            String settleStatus = SettleApplyDetailStateEnum.ALL_FAIL.getValue();
            // 付款
            if ("2".equals(dataSettledDetail.getRecpaytype())) {
                // 只结算中才更新
                if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(billb.getPaySettleStatus())) {
                    return;
                }
                //预算释放
                stopSettleOccupyBudget(batchTransferAccount, billb);
                billb.setPaySettleStatus(settleStatus);
                // 收款结算状态更新为全部失败
                billb.setRecSettleStatus(settleStatus);
            } else {
                // 只结算中才更新
                if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(billb.getRecSettleStatus())) {
                    return;
                }
                // 收款
                billb.setRecSettleStatus(settleStatus);
                // 付款
                billb.setPaySettleStatus(settleStatus);
            }
            // 更新同名账户划转子表的结算状态
            billb.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, billb);
        }
    }

    /**
     * 处理部分成功
     * @param billb 同名账户批量划转子表
     * @param dbBatchTransferAccount 同名账户批量划转
     * @param dataSettledDetail 待结算明细
     * @throws Exception
     */
    private void handlePartSuccess(BatchTransferAccount_b billb, BatchTransferAccount dbBatchTransferAccount, DataSettledDetail dataSettledDetail) throws Exception {
        if (String.valueOf(WSettlementResult.PartSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //部分成功
            handleSettleDateSuccess(SettleApplyDetailStateEnum.PART_SUCCESS.getValue(), billb, dbBatchTransferAccount, dataSettledDetail);
        }
    }

    /**
     * 处理全部成功
     * @param billb 同名账户批量划转子表
     * @param dbBatchTransferAccount 同名账户批量划转
     * @param dataSettledDetail 待结算明细
     * @throws Exception
     */
    private void handleAllSuccess(BatchTransferAccount_b billb, BatchTransferAccount dbBatchTransferAccount, DataSettledDetail dataSettledDetail) throws Exception {
        if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult()) && 0 ==
                billb.getOriSum().compareTo(new BigDecimal(dataSettledDetail.getSuccesssettlementAmount().toString()))) {// 全部成功
            handleSettleDateSuccess(SettleApplyDetailStateEnum.ALL_SUCCESS.getValue(), billb, dbBatchTransferAccount, dataSettledDetail);
        }
    }

    /**
     * 处理待结算明细办结成功事件
     * @param settleStatus 结算状态
     * @param billb 同名账户批量划转子表
     * @param dbBatchTransferAccount 同名账户批量划转
     * @param dataSettledDetail 结算单
     * @throws Exception
     */
    private void handleSettleDateSuccess(String settleStatus, BatchTransferAccount_b billb, BatchTransferAccount dbBatchTransferAccount, DataSettledDetail dataSettledDetail) throws Exception {
        // 付款
        if ("2".equals(dataSettledDetail.getRecpaytype())) {
            // 只结算中才更新
            if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(billb.getPaySettleStatus())) {
                return;
            }
            billb.setPaySettleStatus(settleStatus);
            // 财资统一对账码
            billb.setPaySmartCheckNo(dataSettledDetail.getCheckIdentificationCode());
            // 收款结算状态-结算中
            billb.setRecSettleStatus(SettleApplyDetailStateEnum.HANDLING.getValue());
            // 期望结算日期=第一笔的结算成功日期
            billb.setExpectSettleDate(DateUtils.dateFormat(billb.getPaySettleSuccessDate(), DateUtils.DATE_PATTERN));
            // 占用预算
            settleSuccessOccupyBudget(dbBatchTransferAccount, billb);
            // 更新同名账户划转子表的结算状态
            billb.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, billb);
            if(!Objects.equals(billb.getRecSettleStatus(), settleStatus)){
                pushPaymentSettleSecond(dbBatchTransferAccount, dataSettledDetail.getBusinessDetailsId(), false);
            }
        } else {
            // 只结算中才更新
            if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(billb.getRecSettleStatus())) {
                return;
            }
            // 收款
            billb.setRecSettleStatus(settleStatus);
            // 财资统一对账码
            billb.setRecSmartCheckNo(dataSettledDetail.getCheckIdentificationCode());
            // 付款结算状态-结算中
            billb.setPaySettleStatus(SettleApplyDetailStateEnum.HANDLING.getValue());
            // 期望结算日期=第一笔的结算成功日期
            billb.setExpectSettleDate(DateUtils.dateFormat(billb.getRecSettleSuccessDate(), DateUtils.DATE_PATTERN));
            // 更新同名账户划转子表的结算状态
            billb.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, billb);
            if(!Objects.equals(billb.getPaySettleStatus(), settleStatus) && billb.getIsCashBusiness() == 1){
                pushPaymentSettleSecond(dbBatchTransferAccount, dataSettledDetail.getBusinessDetailsId(), true);
            }
        }


    }


    /**
     * 更新结算成功的日期和时间
     * @param billb
     * @param dataSettledDetail
     */
    private void updateSettleSuccessDateTime(BatchTransferAccount_b billb, DataSettledDetail dataSettledDetail) {
        // 结算失败 不更新结算成功时间和日期
        if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
            return;
        }
        List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
        if (CollectionUtils.isEmpty(dataSettledDistributes)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102498"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D5", "数据结算信息为空") /* "数据结算信息为空" */);
        }
        String recpaytype = dataSettledDetail.getRecpaytype();
        Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
        try {
            if (dataSettled.isPresent()) {
                // 付款
                if ("2".equals(recpaytype)) {
                    // 只结算中才更新
                    if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(billb.getPaySettleStatus())) {
                        return;
                    }
                    billb.setPaySettleSuccessDateTime(dataSettled.get().getSettleSuccSysTime());
                    billb.setPaySettleSuccessDate(dataSettled.get().getSettleSuccBizTime());
                } else {
                    // 只结算中才更新
                    if (!SettleApplyDetailStateEnum.HANDLING.getValue().equals(billb.getRecSettleStatus())) {
                        return;
                    }
                    // 收款
                    billb.setRecSettleSuccessDateTime(dataSettled.get().getSettleSuccSysTime());
                    billb.setRecSettleSuccessDate(dataSettled.get().getSettleSuccBizTime());
                }
            }
        } catch (Exception e) {
            log.error("get fund payment bill child settle success time fail! id={}, settleSuccessTime={}, message={}"
                    , billb.getId(), dataSettled.get().getSettleSuccBizTime(), e.getMessage());
        }

    }


    /**
     * 更新主表和子表的状态
     *
     * @param currentBill
     * @throws Exception
     */
    public void updateBatchTransferAccountAndBStatus(BatchTransferAccount currentBill) throws Exception {

        List<BatchTransferAccount_b> list = currentBill.BatchTransferAccount_b();
        for (BatchTransferAccount_b item : list) {
            item.setPaySettleStatus(null);
            item.setSettleSucAmount(BigDecimal.ZERO);
            item.setPaySettleSuccessDate(null);
            item.setPaySettleSuccessDateTime(null);
            item.setSettleStopPayAmount(BigDecimal.ZERO);
            item.setRecSettleStatus(null);
            item.setRecSettleSuccessDateTime(null);
            item.setRecSettleSuccessDate(null);
            item.setEntityStatus(EntityStatus.Update);
        }
        // 更新子表
        MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, list);
        currentBill.setEntityStatus(EntityStatus.Update);
        currentBill.setSettleDate(null);
        currentBill.setSettlestatus(null);
        currentBill.setTransferSucSumAmount(null);
        currentBill.setTransferSucSumNamount(null);
        currentBill.setVoucherNo(null);
        currentBill.setVoucherId(null);
        currentBill.setVoucherstatus(VoucherStatus.Empty.getValue());
        currentBill.setVoucherPeriod(null);
        // 更新主表
        MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, currentBill);
    }


    /**
     * 过滤单据转换规则后的数据
     *
     * @param settleMap
     * @param direction
     * @param bizbilldetailid
     */
    private void filterSettleMap(Map<String, Object> settleMap, int direction, String bizbilldetailid) {
        // 协同生单 结算单交易类型bustype
        // 协同收款结算 2298061825635778569
        // 协同付款结算 2298061619473678345 协同的结算单不会回写申请单余额、状态等
        if (direction == 1) {
            settleMap.put("bustype", UnifiedSettleBusiTypeEnum.COLLABORATICE_COLLECTION.getId());
        }
        List<Map<String, Object>> settleBenchBenchList = (List<Map<String, Object>>) settleMap.get("settleBench_b");
        List<Map<String, Object>> needPushSettleBenchList = new ArrayList<>();
        for (Map<String, Object> settleBench : settleBenchBenchList) {
            if (settleBench.get("bizbilldetailid").toString().equals(bizbilldetailid)) {
                needPushSettleBenchList.add(settleBench);
            }
        }
        settleMap.put("settleBench_b", needPushSettleBenchList);
    }

    /**
     * 根据办结的结算单过滤同名账户批量划转的子表数据
     *
     * @param dbBatchTransferAccount 主表
     * @param bizbilldetailid    结账单
     */
    private void filterSubsBySettleDetailId(BatchTransferAccount dbBatchTransferAccount, String bizbilldetailid) {
        List<BatchTransferAccount_b> batchTransferAccountBList = dbBatchTransferAccount.BatchTransferAccount_b();
        List<BatchTransferAccount_b> needPushBatchTransferAccountBList = new ArrayList<>();
        for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccountBList) {
            if (batchTransferAccountB.getId().equals(bizbilldetailid)) {
                needPushBatchTransferAccountBList.add(batchTransferAccountB);
            }
        }
        // 单笔办结单笔推送结算
        dbBatchTransferAccount.setBatchTransferAccount_b(needPushBatchTransferAccountBList);
    }

    /**
     * 构建结算参数
     *
     * @param sourceData       结算申请单
     * @param makeBillRuleCode 单据转换规则编码
     * @param sourceBusiObj    业务对象
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> buildSettleMap(BatchTransferAccount sourceData, String makeBillRuleCode, String sourceBusiObj) throws Exception {
        log.error("start convert BatchTransferAccount!");
        ConvertParam convertParam = buildConvertParam(sourceData, makeBillRuleCode, sourceBusiObj);
        //调用转换规则服务 开始转换单据
        DomainMakeBillRuleModel makeBillRuleModel = businessConvertService.queryMakeBillRule(convertParam);
        if (Objects.isNull(makeBillRuleModel)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D6", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
        }
        ConvertResult convert = businessConvertService.convert(convertParam, makeBillRuleModel);
        if (Objects.isNull(convert) || CollectionUtils.isEmpty(convert.getConvertedBillList())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D6", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
        }
        List<Map<String, Object>> settleMapList = new ArrayList<>();
        convert.getConvertedBillList().forEach(convertedBill -> {
            Map<String, Object> convertedBillData = convertedBill.getTargetData();
            convertedBillData.put("sourceCode", ICmpConstant.BATCH_TRANSFER_ACCOUNT);
            convertedBillData.put("sourceId", sourceData.getId().toString());
            convertedBillData.put("sourceBusiObj", sourceBusiObj);
            settleMapList.add(convertedBillData);
        });
        log.error("convert BatchTransferAccount end !");
        return settleMapList;
    }

    /**
     * 推送结算
     *
     * @param pushSettleParam 推送结算参数
     * @param settleMap       结算参数
     * @throws Exception
     */
    private void pushSettleBill(PushSettleParam pushSettleParam, Map<String, Object> settleMap) throws Exception {
        log.error("batchtransferaccount2SettleBench.pushSettleBill start,pushSettleParam:{}, settleMap:{}",
                CtmJSONObject.toJSONString(pushSettleParam), CtmJSONObject.toJSONString(settleMap));
        settleMap.put("sourceCode", ICmpConstant.BATCH_TRANSFER_ACCOUNT);
        pushSettleProcessor.doProcessor(Application.CMP, pushSettleParam, settleMap);
        log.error("batchtransferaccount2SettleBench.pushSettleBill end");
    }

    /**
     * 构建转换参数
     *
     * @param sourceData       主表
     * @param makeBillRuleCode 单据转换规则编码
     * @param busiObj          业务对象
     * @return
     * @throws CtmException
     */
    private ConvertParam buildConvertParam(BatchTransferAccount sourceData, String makeBillRuleCode, String busiObj) throws CtmException {
        ConvertParam convertParam = new ConvertParam();
        convertParam.setMakeBillRuleCode(makeBillRuleCode);
        convertParam.setRetry(false);
        convertParam.setTenantId(InvocationInfoProxy.getTenantid());
        convertParam.setDomain("ctm-cmp");
        // 取应用编码
        convertParam.setSubId("CM");
        convertParam.setBillNum(busiObj);
        convertParam.setSourceBills(Collections.singletonList(sourceData));
        convertParam.setSourceIds(Collections.singletonList(sourceData.getId()));
        convertParam.setNeedQueryBill(true);
        return convertParam;
    }

    @Override
    public CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception {
        String serviceCode = params.getString("serviceCode");
        String addType = params.getString("addType");
        String tradeType = serviceCode.split("_")[0];
        BdTransType bdTransType = transTypeQueryService.findById(tradeType);
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeTypeCode = (String) jsonObject.get("batchtransferType_ext");
        boolean addTransTypeFlag = false;
        switch (tradeTypeCode) {
            case "yhzz":
                // 银行转账
                if (!"0".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "jcxj":
                // 缴存现金
                if (!"1".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "tqxj":
                // 提取现金
                if (!"2".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "xjhz":
                // 现金互转
                if (!"3".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "dsfzz":
                // 第三方转账
                if (!"4".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "sbqbcz":
                // 数币钱包充值
                if (!"5".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "sbqbtx":
                // 数币钱包提现
                if (!"6".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            case "sbqbhz":
                // 数币钱包互转
                if (!"7".equals(addType)) {
                    addTransTypeFlag = true;
                }
                break;
            default:
                break;
        }
        params.put("addTransTypeFlag", addTransTypeFlag);
        return params;
    }

    /**
     * 结算成功占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void settleSuccessOccupyBudget(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        ResultBudget resultBudget = btaCmpBudgetManagerService.settleSuccessOccupyBudget(batchTransferAccount, Collections.singletonList(batchTransferAccountB), BatchTransferAccountUtil.SETTLE_SUCCESS_SETTLE_SUCCESS);
        if (resultBudget.isSuccess()) {
            batchTransferAccountB.setIsOccupyBudget(resultBudget.getBudgeted());
        }
    }

    /**
     * 结算止付释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void stopSettleOccupyBudget(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        ResultBudget resultBudget = btaCmpBudgetManagerService.stopSettleOccupyBudget(batchTransferAccount, Collections.singletonList(batchTransferAccountB), BatchTransferAccountUtil.SAVE_SUBMIT_AUDIT_SETTLE_SUCCESS_STOP_SETTLE);
        if (resultBudget.isSuccess()) {
            batchTransferAccountB.setIsOccupyBudget(resultBudget.getBudgeted());
        }
    }
}
