package com.yonyoucloud.fi.cmp.event.listerEvent.settlement;

import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettled;
import com.yonyoucloud.ctm.stwb.openapi.*;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.reqvo.AgentPaymentReqVO;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettlementResult;
import com.yonyoucloud.ctm.stwb.unifiedsettle.enums.SettleDetailSettleStateEnum;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckManageRpcService;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckRpcService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchtransferaccountService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.service.BillClaimService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.checkStock.service.enums.CashType;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonServiceImpl;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.*;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.transferaccount.util.BaseDocUtils;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.lang.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 资金结算单结算后回调接口
 * 待结算数据办结事件、dSettleDoneEvent
 */
@Slf4j
@Service
@Transactional
public class FundBillCallbackProxyServiceImpl {
    private static final short ISREFUND = 1; //是否退票，1-退票
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private FundCommonServiceImpl fundCommonService;
    @Autowired
    private MarginCommonService marginCommonService;
    @Autowired
    private JournalService journalService;
    @Autowired
    private CmpVoucherService cmpVoucherService;
    @Autowired
    private CtmCmpCheckRpcService ctmCmpCheckRpcService;
    @Autowired
    private CtmCmpCheckManageRpcService ctmCmpCheckManageRpcService;
    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;
    @Autowired
    private ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Autowired
    private BillClaimService billClaimService;
    @Autowired
    BatchtransferaccountService batchtransferaccountService;
    @Autowired
    TransTypeQueryService transTypeQueryService;
    private static BaseRefRpcService refRpcService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;
    @Autowired
    BankAccountSettingService bankaccountSettingService;
    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private static final String PARAM_TRUE = "1";
    private static final String PARAM_FALSE = "0";
    private static final List<String> DELEGATION_BILL_TRADE_TYPE = Arrays.asList("cmp_fund_payment_delegation", "cmp_fundcollection_delegation");

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void proxyOnEvent(BusinessEvent businessEvent, String s) throws Exception {
        // 资金付款单，结算成功后，需要占用预算，调用接口
        String callInfo = businessEvent.getUserObject();
        DataSettledDetail dataSettledDetail = CtmJSONObject.parseObject(callInfo, DataSettledDetail.class);
        CtmJSONObject logData = new CtmJSONObject();
        String businessBillNum = dataSettledDetail.getBusinessBillNum();
        logData.put("dataSettledDetail", dataSettledDetail);
        ctmcmpBusinessLogService.saveBusinessLog(logData, businessBillNum, "dataSettledDone", IServicecodeConstant.BANKJOURNAL, IMsgConstant.SETTLE, IMsgConstant.SETTLE);
        String tradeTypeCode = dataSettledDetail.getTradeTypeCode();
        boolean isDelegationBillTradeType = (ValueUtils.isNotEmpty(tradeTypeCode) && DELEGATION_BILL_TRADE_TYPE.contains(tradeTypeCode));
        boolean isIncomeAndExpenditure = String.valueOf(BusinessBillType.IncomeAndExpenditure.getValue()).equals(dataSettledDetail.getBusinessBillType());
        boolean Unified_Synergy = false;
        if (ICmpConstant.PAYMENTBILLTYPEID.equals(dataSettledDetail.getBusinessBillType())) {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").eq(dataSettledDetail.getBusinessBillId()));
            querySchema.addCondition(queryConditionGroup);
            List<FundPayment> fundPayments = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(fundPayments) && fundPayments.get(0).getBilltype() == EventType.Unified_Synergy) {
                Unified_Synergy = true;
            }
        } else if (ICmpConstant.COLLECTIONBILLTYPEID.equals(dataSettledDetail.getBusinessBillType())) {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").eq(dataSettledDetail.getBusinessBillId()));
            querySchema.addCondition(queryConditionGroup);
            List<FundCollection> fundCollections = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(fundCollections) && fundCollections.get(0).getBilltype() == EventType.Unified_Synergy) {
                Unified_Synergy = true;
            }
        }
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (isDelegationBillTradeType || isIncomeAndExpenditure || Unified_Synergy) {
            String lockKey = dataSettledDetail.getBusinessDetailsId();
            CtmLockTool.executeInOneServiceLock(lockKey, 120L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    // 加锁失败
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"));
                }
                log.error(" 统收统支待结算数据办结 lockKey :" + lockKey);
                log.error("统收统支-委托收款逻辑进入");
                try {
                    writingTheSettlementStatusOfDelegationBill(dataSettledDetail);
                } catch (Exception e) {
                    log.error("统收统支-委托首付款逻辑报错！" + e.getMessage());
                }
                if (dataSettledDetail.getRelateClaimBillId() != null) {
                    beforeBillclaimFundSegmentation(dataSettledDetail);
                }
                log.error("统收统支-委托收款逻辑完成");
            });
        } else if ((enableSimplify && "2571640684663808".equals(dataSettledDetail.getBusinessBillType())) || String.valueOf(BusinessBillType.CashRecBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {// 资金收款单
            fundcollectionSettleFinishDeal(dataSettledDetail);
        } else if ((enableSimplify && "2553141119111680".equals(dataSettledDetail.getBusinessBillType())) || String.valueOf(BusinessBillType.CashPayBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {// 资金付款单
            fundpaymentSettleFinishDeal(dataSettledDetail);
        } else if (String.valueOf(BusinessBillType.ForeignPayment.getValue()).equals(dataSettledDetail.getBusinessBillType())) { //外汇付款单
            String lockKey = dataSettledDetail.getBusinessBillId();
            CtmLockTool.executeInOneServiceLock(lockKey, 120L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    // 加锁失败
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"));
                }
                // 根据id查询外汇付款
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                queryConditionGroup.appendCondition(QueryCondition.name("id").eq(dataSettledDetail.getBusinessBillId()));
                querySchema.addCondition(queryConditionGroup);
                List<ForeignPayment> foreignPaymentList = MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isNotEmpty(foreignPaymentList)) {
                    fundCommonService.updateSettledInfoOfForeignPayment(dataSettledDetail, foreignPaymentList.get(0));
                }
            });
        } else if (String.valueOf(BusinessBillType.PaymentMarginManagement.getValue()).equals(dataSettledDetail.getBusinessBillType())) { //支付保证金
            // 根据id查询支付保证金
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").eq(dataSettledDetail.getBusinessBillId()));
            querySchema.addCondition(queryConditionGroup);
            List<PayMargin> payMarginList = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(payMarginList)) {
                marginCommonService.updateSettledInfoOfPayMargin(dataSettledDetail, payMarginList.get(0));
            }
        } else if (String.valueOf(BusinessBillType.ReceiptMarginManagement.getValue()).equals(dataSettledDetail.getBusinessBillType())) { //收到保证金
            // 根据id查询收到保证金
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").eq(dataSettledDetail.getBusinessBillId()));
            querySchema.addCondition(queryConditionGroup);
            List<ReceiveMargin> receiveMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(receiveMarginList)) {
                marginCommonService.updateSettledInfoOfReceiveMargin(dataSettledDetail, receiveMarginList.get(0));
            }
        } else if (String.valueOf(BusinessBillType.TransferBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {//转账单
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, dataSettledDetail.getBusinessBillId());
            if (transferAccount == null) {
                log.error("根据id没有找到对应的TransferAccount，请查询数据库确认，id:{}", dataSettledDetail.getBusinessBillId());
                JsonUtils.toJsonString(EventResponseVO.success());
            }
            transferAccount.setEntityStatus(EntityStatus.Update);
            transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
            // 根据结算状态，判断是否再次办结
            if (transferAccount.getSettlestatus() == SettleStatus.alreadySettled) {
                // 转账单主表新增字段，再次办结标志（boolean）
                transferAccount.setIsSettleAgain(true);
            }
            //结算状态为待结算
            if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())
                    || dataSettledDetail.getSuccesssettlementAmount().compareTo(BigDecimal.ZERO) == 1
                    && transferAccount.getSettlestatus().getValue() == 3) {
                //todo 2024.12.11需求 结算成功办结，转账单的单据信息（结算方式、银行账号等）要按照结算返回的数据来更新
                transferAccountChange(transferAccount, dataSettledDetail);
                //如果转账工作台的单据状态为已结算补单，则保留为已结算补单
                if (transferAccount.getSettlestatus().getValue() == SettleStatus.SettledRep.getValue()) {
                    transferAccount.setSettlestatus(SettleStatus.SettledRep);
                } else {
                    transferAccount.setSettlestatus(SettleStatus.alreadySettled);
                }
                //财资统一对账码（付款）未空时，赋值结算返回的
                if ("2".equals(dataSettledDetail.getRecpaytype()) && StringUtils.isEmpty(transferAccount.getPaysmartcheckno())) {
                    transferAccount.setPaysmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                }
                //财资统一对账码（收款）
                if ("1".equals(dataSettledDetail.getRecpaytype()) && StringUtils.isEmpty(transferAccount.getSmartcheckno())) {
                    transferAccount.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                }
                transferAccount.setSettleSuccessAmount(dataSettledDetail.getSuccesssettlementAmount());
                transferAccount.setSettleuser(AppContext.getCurrentUser().getId());
                // CZFW-505136 填写现金存入业务时，系统根据审批通过的存入单自动生成付款的统一结算单，统一结算单支付成功后，会自动生成收方的结算成功统一结算单；
                // 问题是自动生成的收方的统一结算单的结算成功日期没有跟付款方保持一致，而是取得当前日期
                if(dataSettledDetail.getDataSettledDistribute().get(0).getSettleSuccBizTime() != null){
                    transferAccount.setSettledate(dataSettledDetail.getDataSettledDistribute().get(0).getSettleSuccBizTime());
                }else{
                    transferAccount.setSettledate(DateUtils.dateParse(dataSettledDetail.getPayDownData(), DateUtils.DATE_TIME_PATTERN));
                }

                //如果交易类型为提取现金，支票编号有值，调用支票工作台-支票兑付/背书接口，单据类型=付款
                if ("ec".equals(dataSettledDetail.getBusinessBillType()) && transferAccount.getCheckid() != null) {
                    Date date = new Date();
                    List<CheckDTO> checkDTOChange = this.setValueSuccess(transferAccount, date);
                    ctmCmpCheckRpcService.checkOperation(checkDTOChange);
                }
                //转账单付款单预占/释放支票
                List<CheckStock> stockList = getCheckStock(dataSettledDetail.getSwbillno(), transferAccount.getAccentity());//查询待结算数据的支票
                if (CollectionUtils.isNotEmpty(stockList)) {
                    CheckStock checkStock = stockList.get(0);
                    CheckStock transferCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid()); //查询转账工作台的支票
                    if (transferCheckStock != null) {
                        if ((checkStock.getId().toString()).equals(transferAccount.getCheckid())) {
                            transferCheckStock.setOccupy((short) 1);//预占
                        } else {
                            transferCheckStock.setOccupy((short) 0);//释放
                        }
                        transferCheckStock.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, transferCheckStock);
                    }
                }
                // 生成收款方式的转账单
                if (ObjectUtils.isEmpty(transferAccount.getCollectid())) {
                    //1030需求：资金结算回调接口逻辑调整
                    //待结算数据收付类型：1收/2付
                    if ("1".equals(dataSettledDetail.getRecpaytype())) {
                        // 2 现金账户
                        if (ObjectUtils.isNotEmpty(dataSettledDetail.getOppAccType()) && dataSettledDetail.getOppAccType() == 2) {
                            //付款现金账户id
                            transferAccount.setPayCashAccount(dataSettledDetail.getCounterpartybankaccount());
                        } else if (ObjectUtils.isNotEmpty(dataSettledDetail.getOppAccType()) && dataSettledDetail.getOppAccType() == 3) {
                            //付款银行账户id
                            transferAccount.setPayBankAccount(dataSettledDetail.getCounterpartybankaccount());
                        }
                    } else {
                        //付款银行账户id
                        if (ObjectUtils.isNotEmpty(dataSettledDetail.getSettlemetBankAccountId()) && ObjectUtils.isNotEmpty(transferAccount.getPayBankAccount())
                                && !dataSettledDetail.getSettlemetBankAccountId().equals(transferAccount.getPayBankAccount())) {
                            transferAccount.setPayBankAccount(dataSettledDetail.getSettlemetBankAccountId());
                        }

                        //付款现金账户id
                        if (ObjectUtils.isNotEmpty(dataSettledDetail.getSettlemetCashAccountId()) && ObjectUtils.isNotEmpty(transferAccount.getPayCashAccount())
                                && !dataSettledDetail.getSettlemetCashAccountId().equals(transferAccount.getPayCashAccount())) {
                            transferAccount.setPayCashAccount(dataSettledDetail.getSettlemetCashAccountId());
                        }
                    }
                    //付款虚拟账户id
                    transferAccount.setPayVirtualAccount(dataSettledDetail.getThirdParVirtAccount());
                    //根据支票编号查支票id
                    List<CheckStock> list = getCheckStock(dataSettledDetail.getSwbillno(), transferAccount.getAccentity());
                    if (list.size() > 0) {
                        CheckStock checkStock = list.get(0);
                        //票据号、支票号
                        transferAccount.setCheckid((checkStock.getId()));
                    }
                    if (transferAccount.getSettlestatus() != null && (transferAccount.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue() || transferAccount.getSettlestatus().getValue() == SettleStatus.SettledRep.getValue())) {
                        boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                        if (implement) {
                            transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                        }
                    }
                    // fukk  推补单
                    if (judgePushSecondRecBillAndUpdate(transferAccount)) {
                        pushTransferBill(transferAccount);
                    }
                } else {
                    if (transferAccount.getSettlestatus() != null && (transferAccount.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue() || transferAccount.getSettlestatus().getValue() == SettleStatus.SettledRep.getValue())) {
                        boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                        if (implement) {
                            transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                        }
                    }
                    MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                }

                // 二次办结成功，需要删除之前凭证，再重新生成
                CtmJSONObject generateResult = null;
                // 是否再次办结,待结算数据收付类型：1收/2付；收方的都不处理凭证
                boolean endFlag = false;
                if (BaseDocUtils.isCashBox(transferAccount)) {
                    if ("2".equals(dataSettledDetail.getRecpaytype())) {
                        endFlag = true;
                    }
                } else {
                    if ("1".equals(dataSettledDetail.getRecpaytype())) {
                        endFlag = true;
                    }
                }
                if (endFlag) {
                    if (transferAccount.getIsSettleAgain() != null && transferAccount.getIsSettleAgain()) {
                        // 已生成凭证才删除，已生成、过账中、过账成功（正常流程都在这三种状态中）
                        if (transferAccount.getVoucherstatus() != null && (transferAccount.getVoucherstatus() == VoucherStatus.Created || transferAccount.getVoucherstatus() == VoucherStatus.POSTING || transferAccount.getVoucherstatus() == VoucherStatus.POST_SUCCESS)) {
                            // 先删除上次办结成功的旧凭证
                            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResultWithOutException(transferAccount);
                            if (!deleteResult.getBoolean("dealSucceed")) {
                                // 删除旧凭证失败，记录失败信息，不再生成新凭证
                                String exceptionMessage = (String) deleteResult.get("message");
                                if (exceptionMessage != null) {
                                    if (exceptionMessage.length() < 255) {
                                        transferAccount.setVoucherMessage(exceptionMessage);
                                    } else {
                                        transferAccount.setVoucherMessage(exceptionMessage.substring(0, 250));
                                    }
                                }
                            } else {
                                // 再次生成正向新凭证-凭证模板匹配规则为“是否再次办结”为true匹配红冲凭证，不为true，则匹配正向凭证
                                transferAccount.setVoucherstatus(VoucherStatus.Empty.getValue());
                                generateResult = cmpVoucherService.generateVoucherWithResult(transferAccount);
                            }
                        } else if (transferAccount.getVoucherstatus() == VoucherStatus.Empty || transferAccount.getVoucherstatus() == VoucherStatus.NO_POST) {
                            // 再次办结时，期间可能修改核算规则，比如启停用，故重新办结的时候也需要尝试再次生成凭证；此状态中之前定未生成过凭证，故无需删除旧凭证
                            generateResult = cmpVoucherService.generateVoucherWithResult(transferAccount);
                        }
                    } else {
                        /**
                         * 为保证事件幂等，需要判断凭证状态
                         * 凭证状态VoucherStatus.Empty时，之前未发过生成凭证接口，可以调用
                         * 凭证状态VoucherStatus.NO_POST时，之前生成过且未匹配上，但期间可能修改核算规则，比如启停用，故重新办结的时候也需要尝试再次生成凭证
                         */
                        if (transferAccount.getVoucherstatus() == VoucherStatus.Empty || transferAccount.getVoucherstatus() == VoucherStatus.NO_POST) {
                            // 首次办结，直接生成新凭证即可
                            generateResult = cmpVoucherService.generateVoucherWithResult(transferAccount);
                        }
                    }
                }
                if (generateResult != null) {
                    // 删除旧凭证成功，生成了新凭证
                    if (!generateResult.getBoolean("dealSucceed")) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102474"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00252", "单据【") /* "单据【" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00253", "】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateResult.get("message"));
                    }
                    if (generateResult.get("genVoucher") != null && !generateResult.getBoolean("genVoucher")) {
                        transferAccount.setVoucherstatus(VoucherStatus.NONCreate.getValue());
                    } else {
                        transferAccount.setVoucherstatus(VoucherStatus.POSTING.getValue());
                    }

                    TransferAccount dbTransferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferAccount.getId());
                    dbTransferAccount.setVoucherstatus(transferAccount.getVoucherstatus().getValue());
                    dbTransferAccount.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(TransferAccount.ENTITY_NAME, dbTransferAccount);
                }
            } else if (dataSettledDetail.getWsettlementResult().equals(WSettlementResult.AllFaital.getValue()) || dataSettledDetail.getStoppedamount().compareTo(BigDecimal.ZERO) == 1) {
//                      结算止付or结算止付金额>0
                // 转账单结算状态为：结算中 Or 结算成功
                if (transferAccount.getSettlestatus().getValue() == SettleStatus.SettleProssing.getValue() || transferAccount.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                    transferAccount.setSettlestatus(SettleStatus.SettleFailed);
                    Short budgeted = transferAccount.getIsOccupyBudget();
                    if (budgeted != null && budgeted == OccupyBudget.PreSuccess.getValue()) {
                        if (cmpBudgetTransferAccountManagerService.releaseBudget(transferAccount)) {
                            transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                        }
                    } else if (budgeted != null && budgeted == OccupyBudget.ActualSuccess.getValue()) {
                        if (cmpBudgetTransferAccountManagerService.releaseImplement(transferAccount)) {
                            transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                        }
                    }
                    transferAccount.setSettleStopPayAmount(dataSettledDetail.getStoppedamount());
                    transferAccount.setSettleuser(AppContext.getCurrentUser().getId());
                    transferAccount.setSettleSuccessAmount(null);
                    // 第一次结算止付，凭证状态为不生成
                    if (transferAccount.getIsSettleAgain() == null || !transferAccount.getIsSettleAgain()) {
                        transferAccount.setVoucherstatus(VoucherStatus.NONCreate.getValue());
                    }
                    // 删除待结算数据：判断是否已经生成收方结算单
                    if (transferAccount.getCollectid() != null) {
                        QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
                        querySettledDetailModel.setWdataorigin(8);// 来源业务系统，现金管理
                        querySettledDetailModel.setTransNumber(transferAccount.getCollectid().toString());
                        RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).datasettledDelete(querySettledDetailModel);
                    }
                    // 转账单凭证状态：过账成功 or 已生成 or 过账中
                    if ("2".equals(dataSettledDetail.getRecpaytype()) && transferAccount.getIsSettleAgain() != null && transferAccount.getIsSettleAgain()) {
                        // 已生成凭证才删除，已生成、过账中、过账成功（正常流程都在这三种状态中）
                        if (transferAccount.getVoucherstatus() != null && (transferAccount.getVoucherstatus() == VoucherStatus.Created || transferAccount.getVoucherstatus() == VoucherStatus.POSTING || transferAccount.getVoucherstatus() == VoucherStatus.POST_SUCCESS)) {
                            // 直接生成止付红冲凭证
                            transferAccount.setVoucherstatus(VoucherStatus.Empty.getValue());
                            CtmJSONObject generateRedResult = cmpVoucherService.generateRedVoucherWithResult(transferAccount, "RedVoucher");
                            if (!generateRedResult.getBoolean("dealSucceed")) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102474"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00252", "单据【") /* "单据【" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00253", "】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateRedResult.get("message"));
                            }
                            if (generateRedResult.get("genVoucher") != null && !generateRedResult.getBoolean("genVoucher")) {
                                transferAccount.setVoucherstatus(VoucherStatus.NONCreate.getValue());
                            } else {
                                transferAccount.setVoucherstatus(VoucherStatus.POSTING.getValue());
                            }

                        }
                    }
//                    日记账的释放
                    CmpWriteBankaccUtils.delAccountBook(transferAccount.getId().toString(), false);

                    //如果交易类型为提取现金，支票编号有值，调用支票工作台-支票处置单生成接口
                    if ("ec".equals(dataSettledDetail.getBusinessBillType()) && transferAccount.getCheckid() != null) {
                        CheckManage checkManage = this.setValueFail(transferAccount);

                        ctmCmpCheckManageRpcService.checkSave(checkManage);
                    }
                    transferAccount.setEntityStatus(EntityStatus.Update);
                    transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
                    if (transferAccount.getSettlestatus() != null && transferAccount.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                        boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                        if (implement) {
                            transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                        }
                    }
                    MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                }
            }
        } else if (String.valueOf(BusinessBillType.RecBill.getValue()).equals(dataSettledDetail.getBusinessBillType()) || String.valueOf(BusinessBillType.AgentRecBill.getValue()).equals(dataSettledDetail.getBusinessBillType()) || "320".equals(dataSettledDetail.getBusinessBillType())) {
            //收款单结算成功  认领单进行资金切块
            log.error("应收收款单逻辑切块进入");
            if (dataSettledDetail.getRelateClaimBillId() != null) {
                beforeBillclaimFundSegmentation(dataSettledDetail);
            }
            log.error("应收收款单逻辑切块完成");
        } else if (String.valueOf(BusinessBillType.AgentRecBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {
            //代理收款单  认领单进行资金切块
            log.error("代理收款单逻辑切块进入");
            if (dataSettledDetail.getRelateClaimBillId() != null) {
                beforeBillclaimFundSegmentation(dataSettledDetail);
            }
            log.error("代理收款单逻辑切块完成");
        } else if (String.valueOf(BusinessBillType.AgentPayBill.getValue()).equals(dataSettledDetail.getBusinessBillType())) {
            //代理收款单  认领单进行资金切块
            log.error("代理付款单逻辑切块进入");
            if (dataSettledDetail.getRelateClaimBillId() != null) {
                beforeBillclaimFundSegmentation(dataSettledDetail);
            }
            log.error("代理付款单逻辑切块完成");
        } else if ("320".equals(dataSettledDetail.getBusinessBillType())) {
            //其他单据  资金切块
            log.error("320其他单据逻辑切块进入");
            if (dataSettledDetail.getRelateClaimBillId() != null) {
                beforeBillclaimFundSegmentation(dataSettledDetail);
            }
            log.error("其他单据逻辑切块完成");
        } else if ("2283268970412769285".equals(dataSettledDetail.getBusinessBillType())) {
            log.error("同名账户批量划转监听待结算数据办结事件");
            batchTransferSettleFinishDeal(dataSettledDetail);
        }

    }

    /**
     * 判断是否需要推送收款单并更新转账单的collectid
     * 如不加此判断结算的疑重会拦截住，事件不能重试成功
     * 解决的问题：二次推送收款结算单接口超时，现金回滚，结算未回滚。导致结算状态不一致的问题。
     * @param transferAccount
     * @return
     * @throws Exception
     */
    private boolean judgePushSecondRecBillAndUpdate(TransferAccount transferAccount) throws Exception {
        QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
        querySettledDetailModel.setWdataorigin(8); // 8-现金管理
        querySettledDetailModel.setBusinessDetailsId(transferAccount.getId().toString());
        List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);
        List<DataSettledDetail> receDataSettledDetailList;
        // 判断是否是现金柜
        if (BaseDocUtils.isCashBox(transferAccount)) {
            // 过滤出生成付款结算单
            receDataSettledDetailList = dataSettledDetailList.stream().filter(dataSettledDetail -> "2".equals(dataSettledDetail.getRecpaytype())).collect(Collectors.toList());
        } else {
            // 过滤出生成的收款结算单
            receDataSettledDetailList = dataSettledDetailList.stream().filter(dataSettledDetail -> "1".equals(dataSettledDetail.getRecpaytype())).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(receDataSettledDetailList)) {
            return true;
        }
        DataSettledDetail receDataSettledDetail = receDataSettledDetailList.get(0);
        transferAccount.setCollectid(receDataSettledDetail.getDataSettledId());
        transferAccount.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
        return false;
    }

    /**
     * 更新资金付款单结算信息
     *
     * @param dataSettledDetail
     * @throws Exception
     */
    private void fundpaymentSettleFinishDeal(DataSettledDetail dataSettledDetail) throws Exception {
        List<FundPayment_b> billbs = getFundPayment_b(dataSettledDetail.getDataSettledId(), dataSettledDetail.getBusinessDetailsId());
        if (CollectionUtils.isNotEmpty(billbs)) {
            String lockKey = billbs.get(0).getId().toString();
            CtmLockTool.executeInOneServiceLock(lockKey, 120L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    // 加锁失败
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"));
                }
                log.error(" 资金付款单待结算数据办结 lockKey :" + lockKey);
                executeFundpaymentSettleFinishDeal(dataSettledDetail, billbs);
            });
        } else {
            log.error("根据dataSettledId/transNumber没有找到对应的FundPayment_b，请查询数据库确认，transNumber:" + dataSettledDetail.getDataSettledId());
        }
    }

    /**
     * 执行更新资金付款单结算信息
     * @param dataSettledDetail
     * @param billbs
     * @throws Exception
     */
    private void executeFundpaymentSettleFinishDeal(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs) throws Exception {
        List<FundPayment_b> generateVoucherList = fundCommonService.updateSettledInfoOfFundPayment(dataSettledDetail, billbs, true);
        //更新资金付款单关联数据，由于结算单将id合并，资金收付回写的时候不再更新流水和认领单id
//        fundCommonService.updateFundPaymentRelationInfo(dataSettledDetail, generateVoucherList);
        if (dataSettledDetail.getRelateClaimBillId() != null) {
            log.error("资金付款逻辑切块进入");
            beforeBillclaimFundSegmentation(dataSettledDetail);
            log.error("资金付款逻辑切块完成");
        }
        if (!CollectionUtils.isEmpty(generateVoucherList)) {
            List<String> subIds = generateVoucherList.stream().map(billb -> billb.getId().toString()).collect(Collectors.toList());
            List<Map<String, Object>> retList = MetaDaoHelper.queryByIds(FundPayment_b.ENTITY_NAME, "id,pubts", subIds);
            HashMap<String, Date> idTsMap = new HashMap<String, Date>();
            for (Map<String, Object> map : retList) {
                idTsMap.put(map.get("id").toString(), (Date) map.get("pubts"));
            }
            for (FundPayment_b billb : generateVoucherList) {
                billb.setPubts(idTsMap.get(billb.getId().toString()));
            }
            EntityTool.setUpdateStatus(generateVoucherList);
        }
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (!CollectionUtils.isEmpty(generateVoucherList)) {
            EntityTool.setUpdateStatus(generateVoucherList);
            if (enableSimplify) {//结算简强场景不更新结算成功金额
                for (FundPayment_b fundBill : generateVoucherList) {
                    fundBill.remove(ICmpConstant.SUCCESS_SUM_FIELD);
                    fundBill.remove(ICmpConstant.REMAIN_AMOUNT_FIELD);
                    fundBill.remove(ICmpConstant.PAY_TRANSIT_AMOUNT_FIELD);
                }
            }
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, generateVoucherList);
        }
        //获取资金收款主表
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, billbs.get(0).get("mainid"));

        //BIP-BUG-00067394 和明琴沟通，再次结算的时候要把缓存的单子id拿掉，否则会导致二次结算，二次撤销的时候没有办法把事项和凭证删掉
        List<String> fundPaymentIdEaaiList = AppContext.cache().getObject("cancelSettle_fundpayment_id");
        if(fundPaymentIdEaaiList != null && fundPaymentIdEaaiList.contains(fundPayment.getId().toString())){
            fundPaymentIdEaaiList.remove(fundPayment.getId().toString());
            AppContext.cache().setObject("cancelSettle_fundpayment_id", fundPaymentIdEaaiList, 60 * 2);
        }
        if (CollectionUtils.isNotEmpty(generateVoucherList)) {
            for (FundPayment_b billb : generateVoucherList) {
                // 结算成功时过账时才会在此时生成凭证
                if (fundPayment.getSettleSuccessPost() != null && fundPayment.getSettleSuccessPost() == 1) {
                    fundCommonService.generateVoucher(billb, FundPayment.ENTITY_NAME, false);
                }
                //支票预占、释放
                List<CheckStock> stockList = getCheckStock(dataSettledDetail.getSwbillno(), fundPayment.getAccentity());//查询待结算数据的支票
                if (CollectionUtils.isNotEmpty(stockList)) {
                    CheckStock checkStock = stockList.get(0);
                    CheckStock fundPaymentCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, billb.get("checkId"));//查询资金收款单支票
                    if (Objects.isNull(billb.get("checkId")) || Objects.isNull(fundPaymentCheckStock)) {
                        checkStock.setOccupy((short) 1);//预占
                        checkStock.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                    } else if ((checkStock.getId().toString()).equals(billb.get("checkId"))) {
                        fundPaymentCheckStock.setOccupy((short) 1);//预占
                        fundPaymentCheckStock.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, fundPaymentCheckStock);
                    } else {
                        fundPaymentCheckStock.setOccupy((short) 0);//释放
                        fundPaymentCheckStock.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, fundPaymentCheckStock);
                    }
                }
            }
        }
        CtmJSONObject logFundData = new CtmJSONObject();
        String businessBillId = dataSettledDetail.getBusinessBillId();
        logFundData.put("updateFundPaymentb", generateVoucherList);
        ctmcmpBusinessLogService.saveBusinessLog(logFundData, businessBillId, "updateFundPaymentb", IServicecodeConstant.BANKJOURNAL, IMsgConstant.SETTLE, IMsgConstant.SETTLE);
    }

    /**
     * 更新资金收款单结算信息
     *
     * @param dataSettledDetail
     * @throws Exception
     */
    private void fundcollectionSettleFinishDeal(DataSettledDetail dataSettledDetail) throws Exception {
        log.error("资金收款逻辑进入");
        List<FundCollection_b> billbs = getFundCollection_b(dataSettledDetail.getDataSettledId(), dataSettledDetail.getBusinessDetailsId());
        String lockKey = billbs.get(0).getId().toString();
        CtmLockTool.executeInOneServiceLock(lockKey, 120L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 加锁失败
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"));
            }
            log.error(" 资金收款单待结算数据办结 lockKey :" + lockKey);
            executeFundcollectionSettleFinishDeal(dataSettledDetail, billbs);
        });
    }


    /**
     * 执行更新资金收款单结算信息
     * @param dataSettledDetail
     * @param billbs
     * @throws Exception
     */
    private void executeFundcollectionSettleFinishDeal(DataSettledDetail dataSettledDetail, List<FundCollection_b> billbs) throws Exception {
        List<FundCollection_b> generateVoucherList = fundCommonService.updateSettledInfoOfFundCollection(dataSettledDetail, billbs, true);
        //更新资金收款单关联数据
        fundCommonService.updateFundCollectionRelationInfo(dataSettledDetail, generateVoucherList);
        log.error("资金收款逻辑切块进入");
        if (dataSettledDetail.getRelateClaimBillId() != null) {
            beforeBillclaimFundSegmentation(dataSettledDetail);
        }
        log.error("资金收款逻辑切块完成");
        if (!CollectionUtils.isEmpty(generateVoucherList)) {
            List<String> subIds = generateVoucherList.stream().map(billb -> billb.getId().toString()).collect(Collectors.toList());
            List<Map<String, Object>> retList = MetaDaoHelper.queryByIds(FundCollection_b.ENTITY_NAME, "id,pubts", subIds);
            HashMap<String, Date> idTsMap = new HashMap<String, Date>();
            for (Map<String, Object> map : retList) {
                idTsMap.put(map.get("id").toString(), (Date) map.get("pubts"));
            }
            for (FundCollection_b billb : generateVoucherList) {
                billb.setPubts(idTsMap.get(billb.getId().toString()));
            }
            EntityTool.setUpdateStatus(generateVoucherList);
        }
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (!CollectionUtils.isEmpty(generateVoucherList)) {
            EntityTool.setUpdateStatus(generateVoucherList);
            if (enableSimplify) {//结算简强场景不更新结算成功金额
                for (FundCollection_b fundBill : generateVoucherList) {
                    fundBill.remove(ICmpConstant.SUCCESS_SUM_FIELD);
                    fundBill.remove(ICmpConstant.REMAIN_AMOUNT_FIELD);
                    fundBill.remove(ICmpConstant.COL_TRANSIT_AMOUNT_FIELD);
                }
            }
            MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, generateVoucherList);
        }
        //获取资金收款主表
        FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, billbs.get(0).get("mainid"));
        //BIP-BUG-00067394 和明琴沟通，再次结算的时候要把缓存的单子id拿掉，否则会导致二次结算，二次撤销的时候没有办法把事项和凭证删掉
        List<String> fundCollectionIdEaaiList = AppContext.cache().getObject("cancelSettle_fundcollection_id");
        if(fundCollectionIdEaaiList != null && fundCollectionIdEaaiList.contains(fundCollection.getId().toString())){
            fundCollectionIdEaaiList.remove(fundCollection.getId().toString());
            AppContext.cache().setObject("cancelSettle_fundcollection_id", fundCollectionIdEaaiList, 60 * 2);
        }
        for (FundCollection_b billb : billbs) {
            // 结算成功时过账时才会在此时生成凭证
            if (fundCollection.getSettleSuccessPost() != null && fundCollection.getSettleSuccessPost() == 1) {
                fundCommonService.generateVoucher(billb, FundCollection.ENTITY_NAME, false);
            }
            //支票预占、释放
            List<CheckStock> stockList = getCheckStock(dataSettledDetail.getSwbillno(), fundCollection.getAccentity());//查询待结算数据的支票
            if (CollectionUtils.isNotEmpty(stockList)) {
                CheckStock checkStock = stockList.get(0);
                CheckStock fundCollectionCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, billb.get("checkId"));//查询资金收款单支票
                if (Objects.isNull(billb.get("checkId")) || Objects.isNull(fundCollectionCheckStock)) {
                    checkStock.setOccupy((short) 1);//预占
                    checkStock.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                } else if ((checkStock.getId().toString()).equals(billb.get("checkId"))) {
                    fundCollectionCheckStock.setOccupy((short) 1);//预占
                    fundCollectionCheckStock.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, fundCollectionCheckStock);
                } else {
                    fundCollectionCheckStock.setOccupy((short) 0);//释放
                    fundCollectionCheckStock.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, fundCollectionCheckStock);
                }
            }
        }
        CtmJSONObject logFundData = new CtmJSONObject();
        String businessBillId = dataSettledDetail.getBusinessBillId();
        logFundData.put("updateFundCollectionb", generateVoucherList);
        ctmcmpBusinessLogService.saveBusinessLog(logFundData, businessBillId, "updateFundCollectionb", IServicecodeConstant.BANKJOURNAL, IMsgConstant.SETTLE, IMsgConstant.SETTLE);
    }

    /**
     * 批量同名账户划转办结逻辑
     *
     * @param dataSettledDetail
     * @throws Exception
     */
    private void batchTransferSettleFinishDeal(DataSettledDetail dataSettledDetail) throws Exception {
        if (!"2283268970412769285".equals(dataSettledDetail.getBusinessBillType())) {
            return;
        }
        log.error("同名账户批量划转监听待结算明细办结内容:{}", CtmJSONObject.toJSONString(dataSettledDetail));
        String lockKey = dataSettledDetail.getBusinessBillId();
        CtmLockTool.executeInOneServiceLock(lockKey, 120L, 60L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 加锁失败
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"));
            }
            log.error("同名账户批量划转监听待结算数据办结 lockKey :{}", lockKey);
            //更新同名账户划转状态
            batchtransferaccountService.updateSettledInfo(dataSettledDetail);
            CtmJSONObject logFundData = new CtmJSONObject();
            String businessBillId = dataSettledDetail.getBusinessBillId();
            ctmcmpBusinessLogService.saveBusinessLog(logFundData, businessBillId, "updateBatchTransfer", IServicecodeConstant.BATCH_TRANSFERACCOUNT, IMsgConstant.SETTLE, IMsgConstant.SETTLE);
        });


    }

    private void beforeBillclaimFundSegmentation(DataSettledDetail dataSettledDetail) throws Exception {
        log.error("beforeBillclaimFundSegmentation开始");
        QuerySchema qs = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryConditionGroup.and(
                QueryConditionGroup.or(QueryCondition.name("id").eq(dataSettledDetail.getRelateClaimBillId().toString()),
                        QueryCondition.name("refbill").eq(dataSettledDetail.getRelateClaimBillId().toString()))));
        qs.addCondition(conditionGroup);
        log.error("beforeBillclaimFundSegmentation参数：" + dataSettledDetail.getRelateClaimBillId().toString());
        List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, qs, null);
        if (billClaimList != null && billClaimList.size() > 0) {
            BillClaim billClaim = billClaimList.get(0);
            if (billClaim.getSettlestatus() == null || FundSettleStatus.SettleSuccess.getValue() != billClaim.getSettlestatus()) {
                billClaim.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());//结算成功
                billClaim.setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.updateBillClaim(billClaim);
                try {
                    billClaimService.billclaimFundSegmentation(dataSettledDetail);
                } catch (Exception e) {
                    log.error("切块失败，认领单信息：" + CtmJSONObject.toJSONString(billClaim));
                }
            }
        }
        log.error("beforeBillclaimFundSegmentation结束");
    }

    private void pushTransferBill(TransferAccount transferAccount) {
        try {
            List<DataSettled> dataSettleds = new ArrayList<>();
            DataSettled dataSettled = new DataSettled();
            // 主表数据
            dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
            dataSettled.setAccentity(transferAccount.getAccentity());//资金组织
            dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.TransferBill.getValue())); //转账单类型
            dataSettled.setBusinessbillnum(transferAccount.getCode());//转账单单据编号
            dataSettled.setBusinessId(transferAccount.getId().toString());//业务单据ID -> 转账单-ID
            dataSettled.setBusinessdetailsid(transferAccount.getId().toString());//业务单据明细ID -> 转账单-ID
            dataSettled.setTradetype(transferAccount.getTradetype());//交易类型
            dataSettled.setOribilldate(transferAccount.getVouchdate());//转账单单据日期
            dataSettled.setExpectpaydate(transferAccount.getSettledate());//第二笔的期望结算日期传结算成功日期 CZFW-315555
            dataSettled.setOricurrency(transferAccount.getCurrency());//原币币种
            DecimalFormat decimalFormat = new DecimalFormat("0.00#");
            String oriSum = decimalFormat.format(transferAccount.getOriSum());
            dataSettled.setOricurrencyamount(new BigDecimal(oriSum));//原币金额
            String natSum = decimalFormat.format(transferAccount.getNatSum());
            dataSettled.setNatSum(new BigDecimal(natSum));//本币金额
            dataSettled.setNatcurrency(transferAccount.getNatCurrency());//本币币种
            dataSettled.setExchangePaymentRateType(transferAccount.getExchangeRateType());//汇率类型
            dataSettled.setExchangerate(transferAccount.getExchRate());//汇率
            dataSettled.setIssinglebatch(PARAM_FALSE);//单笔 0:单笔 1:批量
            dataSettled.setExpectsettlemethod(transferAccount.getCollectsettlemode());//期望结算方式-----收款方结算方式
            dataSettled.setDept(transferAccount.getDept());//部门
            dataSettled.setProject(transferAccount.getProject());//项目
            dataSettled.setCheckIdentificationCode(transferAccount.getSmartcheckno());//收款勾对号
            dataSettled.setRemark(transferAccount.getDescription());//备注对备注
            dataSettled.setPostscript(transferAccount.getPurpose());//用途对附言
            //国机相关：传递支付扩展信息
            dataSettled.setPayExtend(transferAccount.getPayExtend());
            //1030需求：对方类型--》转账工作台.内部单位
            dataSettled.setToaccnttype("6");//对方类型 4->其他，6->内部单位
            //1030需求：对方档案id--》转账工作台.资金组织id
            dataSettled.setCounterpartyid(transferAccount.getAccentity()); //对方档案id
            //对方账户名称 转账单交易类型为银行转账、现金缴存时：转账单-收款银行账户
            String tradeType = transferAccount.getType();//交易类型
            //查询账户信息接口
            // 交易类型
            String type = transferAccount.getType();
            BdTransType bdTransType = transTypeQueryService.findById(transferAccount.get("tradetype"));
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
            String tradeTypeCode = (String) jsonObject.get("transferType_zz");
            // 数币钱包充值、数币钱包提现、数币钱包互转
            List<String> dwtradeTypeCodeList = Arrays.asList("WCZ", "WTX", "WHZ");
            List<String> dwtradeTypeExtList = Arrays.asList("sbqbcz", "sbqbtx", "sbqbhz");
            if ("BT".equalsIgnoreCase(tradeType) || "yhzz".equalsIgnoreCase(tradeTypeCode)) {//BT：银行转账
                //1030需求:转账单交易类型为银行转账时：
                dataSettled.setCounterpartybankaccount(transferAccount.getPayBankAccount());//对方银行账户档案id-----付款银行id
            } else if ("SC".equalsIgnoreCase(tradeType) || "jcxj".equalsIgnoreCase(tradeTypeCode)
                    || "CT".equalsIgnoreCase(tradeType) || "xjhz".equals(tradeTypeCode)) { //现金缴存 现金互转
                dataSettled.setCounterpartybankaccount(transferAccount.getPayCashAccount());//对方银行账户档案id-----付款现金账号id
            } else if ("EC".equalsIgnoreCase(tradeType) || "tqxj".equalsIgnoreCase(tradeTypeCode)) { //提取现金
                dataSettled.setCounterpartybankaccount(transferAccount.getPayBankAccount());//对方银行账户档案id-----付款银行id
            } else if ("TPT".equalsIgnoreCase(tradeType) || "dsfzz".equals(tradeTypeCode)) { //转账单交易类型为第三方转账时：a.若三方交易类型为银行账户转虚拟户时：转账单_收款虚拟账户
                if (transferAccount.getVirtualBank() == 1) {//银行账户转虚拟户
                    dataSettled.setCounterpartybankaccount(transferAccount.getPayBankAccount());//付款银行账户id
                }
            } else if (dwtradeTypeCodeList.contains(tradeType) || dwtradeTypeExtList.contains(tradeTypeCode)) {
                dataSettled.setCounterpartybankaccount(transferAccount.getPayBankAccount());
            }

            dataSettled.setPaySettlementMode(null);//付款结算模式为空
            dataSettled.setOpenwsettlestatus("2");//2表示已结算补单
            dataSettled.setIssettlementcanmodified(PARAM_FALSE);//是否结算方式可修改 否
            dataSettled.setIsmerge(PARAM_FALSE);//是否可合并结算 否
            dataSettled.setIssplit(PARAM_FALSE);//是否可拆分结算 否
            //1030需求：是否登记日记账---->是
            dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账 是
            dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成结算凭证 否
            // 不占用资金计划
            dataSettled.setIsToPushCspl((short)0);
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 结算系统是否接入为是
            //1030需求：结算方式=银行，必填，生成付款→收款的资金结算明细时：转账单-付款银行账户（账号）
            if ("BT".equalsIgnoreCase(tradeType) || "yhzz".equalsIgnoreCase(tradeTypeCode)) {
                dataSettled.setShowourbankaccount(String.valueOf(transferAccount.getRecBankAccount()));//本方银行账号----收款方银行账号
            }
            //1030需求：生成付款→收款的资金结算明细时：待结算数据.是否关联对账单=转账单.付款是否关联
            if (transferAccount.getAssociationStatusCollect() != null && transferAccount.getAssociationStatusCollect() == true) {
                dataSettled.setIsRelateCheckBill(PARAM_TRUE);//是否关联对账单
            } else {
                dataSettled.setIsRelateCheckBill(PARAM_FALSE);//是否关联对账单
            }

            //1030需求：是否关联对账单=是时，必传①生成付款→收款的资金结算明细时：待结算数据.关联银行对账单id=转账单.付款银行对账单id
            dataSettled.setRelateBankCheckBillId(transferAccount.getCollectbankbill());//关联银行对账单id
            //1030需求：是否关联对账单=是时，必传①生成付款→收款的资金结算明细时：待结算数据.关联认领单id=转账单.付款认领单id
            dataSettled.setRelateClaimBillId(transferAccount.getCollectbillclaim());//关联认领单id
            //请求流水id(未赋值) 2025.2.20 修改为固定值 避免重复推送第二笔
            dataSettled.setSerialNumber("pushTransferSecond");

            // ===========fukk start =====================
            // 同名账户划转的交易类型=缴存现金时，改为先传收款方向的待结算数据  结算成功/部分成功后，再传已结算补单的付款方向的待结算数据。
            // （本逻辑属于补单场景，如果 交易类型=缴存现金时 传 付款，其他逻辑保持不变） 【陕建】
            String recpaytype = "1"; // 转账单收付类型
            EnterpriseBankAcctVO bankAcctVO = new EnterpriseBankAcctVO();
            if (ObjectUtils.isNotEmpty(transferAccount.getRecBankAccount())) {
                bankAcctVO = enterpriseBankQueryService.findById(transferAccount.getRecBankAccount());
            }
            if ("sc".equals(type) || "jcxj".equalsIgnoreCase(tradeTypeCode)) { //现金缴存
                if (ObjectUtils.isNotEmpty(bankAcctVO) && bankAcctVO.getAcctopentype().equals(1)) {
                    recpaytype = "2";//付款方向 -----
                    dataSettled.setCashaccount(transferAccount.getPayCashAccount()); // 待结算本方银行账户
                    dataSettled.setCounterpartybankaccount(transferAccount.getRecBankAccount());
                    dataSettled.setShowoppositebankaccountname(bankAcctVO.getAcctName()); // 对方账户 franyi
                    dataSettled.setShowoppositebankaccount(bankAcctVO.getAccount()); //
                    dataSettled.setExpectsettlemethod(transferAccount.getSettlemode()); //期望结算方式
                    dataSettled.setInoutFlag((short) 0);
                } else {
                    recpaytype = "1";
                    dataSettled.setExpectsettlemethod(transferAccount.getCollectsettlemode()); //期望结算方式
                    if (null != transferAccount.getRecBankAccount()) {
                        dataSettled.setEnterpriseBankAccount(transferAccount.getRecBankAccount());//本方银行账号->收款银行账户（账号）
                    }
                    //1030需求生成付款→收款的资金结算明细时：转账单-付款现金账户（账号）
                    if (null != transferAccount.getRecCashAccount()) {
                        dataSettled.setCashaccount(transferAccount.getRecCashAccount());//本方现金账号->收款现金账户（账号）
                    }
                    //1030需求生成付款→收款的资金结算明细时：转账单-收款虚拟账户（账号）
                    if (null != transferAccount.getCollVirtualAccount()) {
                        dataSettled.setThirdParVirtAccount(transferAccount.getCollVirtualAccount());

                    }
                }

            } else {
                recpaytype = "1";
                dataSettled.setExpectsettlemethod(transferAccount.getCollectsettlemode()); //期望结算方式
                if (null != transferAccount.getRecBankAccount()) {
                    dataSettled.setEnterpriseBankAccount(transferAccount.getRecBankAccount());//本方银行账号->收款银行账户（账号）
                }
                //1030需求生成付款→收款的资金结算明细时：转账单-付款现金账户（账号）
                if (null != transferAccount.getRecCashAccount()) {
                    dataSettled.setCashaccount(transferAccount.getRecCashAccount());//本方现金账号->收款现金账户（账号）
                }
                //1030需求生成付款→收款的资金结算明细时：转账单-收款虚拟账户（账号）
                if (null != transferAccount.getCollVirtualAccount()) {
                    dataSettled.setThirdParVirtAccount(transferAccount.getCollVirtualAccount());

                }
            }
            dataSettled.setRecpaytype(recpaytype); //转账单收付类型 1-收款，2-付款
            // ===========fukk end =====================

            // ============fukk start ======================
            // 对方账户类型赋值 【陕建】
            Short oppAccType = null;   // 现金账户=2    银行账户=3  虚拟账户=4
            if ("ec".equals(type) || "tqxj".equals(tradeTypeCode)) {//现金提取
                if (org.apache.commons.lang3.StringUtils.equals("2", recpaytype)) {
                    oppAccType = 2;
                }
                if (org.apache.commons.lang3.StringUtils.equals("1", recpaytype)) {
                    oppAccType = 3;
                }
            } else if ("sc".equals(type) || "jcxj".equalsIgnoreCase(tradeTypeCode)) {//现金缴存
                //转账单收付类型 1-收款，2-付款
                if (org.apache.commons.lang3.StringUtils.equals("2", recpaytype)) {
                    oppAccType = 3;
                }
                if (org.apache.commons.lang3.StringUtils.equals("1", recpaytype)) {
                    oppAccType = 2;
                }
            } else if ("tpt".equals(type) || "dsfzz".equals(tradeTypeCode)) {//三方转账
                if (transferAccount.getVirtualBank() == 0) { // 虚拟账户转银行账户=0  银行账户转虚拟账户=1
                    if (org.apache.commons.lang3.StringUtils.equals("2", recpaytype)) {
                        oppAccType = 3;
                    }
                    if (org.apache.commons.lang3.StringUtils.equals("1", recpaytype)) {
                        oppAccType = 4;
                    }
                } else {
                    if (org.apache.commons.lang3.StringUtils.equals("2", recpaytype)) {
                        oppAccType = 4;
                    }
                    if (org.apache.commons.lang3.StringUtils.equals("1", recpaytype)) {
                        oppAccType = 3;
                    }
                }
            } else if ("ct".equals(type) || "xjhz".equals(tradeTypeCode)) {//现金互转
                //  交易类型=现金互转，付款方向、收款方向的待结算数据对方账户类型都传“现金账户”；
                oppAccType = 2;
            } else if ("bt".equals(type) || "yhzz".equals(tradeTypeCode)) {//银行转账
                // 交易类型=银行转账时，付款方向、收款方向的待结算数据对方账户类型都传“银行账户”；
                oppAccType = 3;
            } else if (dwtradeTypeCodeList.contains(tradeType) || dwtradeTypeExtList.contains(tradeTypeCode)) {
                // 数币交易类型 对方账户类型为银行账户
                oppAccType = 3;
            }
            dataSettled.setOppAccType(oppAccType);
            // ============fukk end ======================
            dataSettled.setStctAcceptType((short) 3);
            dataSettleds.add(dataSettled);
            ResponseResult responseResult = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).builtSystem(dataSettleds);
            if (responseResult.getCode() == 200) {
                log.info("转账单推送成功");
                //1030需求：资金结算会返回id,此时需要将第二次返回的待结算id写入到当前转账单
                updateTransferBillTwo(transferAccount, responseResult.getSuccessList());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102475"), responseResult.getMessage());
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102212"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180388", "推送结算单据失败：") /* "推送结算单据失败：" */ + e.getMessage());
        }
    }

    /**
     * 根据交易流水号查询资金收款单
     *
     * @param transNumber
     * @throws Exception
     * @return【
     */
    public List<FundCollection_b> getFundCollection_b(Long transNumber, String businessDetailsId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("transNumber").eq(String.valueOf(transNumber)));
        querySchema.addCondition(queryConditionGroup);
        List<FundCollection_b> bList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
        if (bList.isEmpty()) {
            QuerySchema query = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryCondition = new QueryConditionGroup(ConditionOperator.and);
            queryCondition.appendCondition(QueryCondition.name("id").eq(businessDetailsId));
            query.addCondition(queryCondition);
            bList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, query, null);
        }
        //汇总子表和主表映射，剔除非审批通过的子表
        //获取去重后的主表数据
        Set<String> mainIds = bList.stream().map(FundCollection_b::getMainid).collect(Collectors.toSet());
        //查询主表数据
        QuerySchema query = QuerySchema.create().addSelect("id,verifystate");
        QueryConditionGroup queryCondition = new QueryConditionGroup(ConditionOperator.and);
        queryCondition.appendCondition(QueryCondition.name("id").in(mainIds));
        queryCondition.appendCondition(QueryCondition.name("verifystate").eq(VerifyState.COMPLETED.getValue()));
        query.addCondition(queryCondition);
        List<FundCollection> mainList = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, query, null);
        Set<String> ids = mainList.stream().map(bizObject -> bizObject.getId().toString()).collect(Collectors.toSet());
        //剔除非审批通过的子表
        bList = bList.stream().filter(e -> ids.contains(e.getMainid())).collect(Collectors.toList());
        return bList;
    }


    /**
     * 根据交易流水号查询资金付款单
     *
     * @param transNumber
     * @return
     * @throws Exception
     */
    public List<FundPayment_b> getFundPayment_b(Long transNumber, String businessDetailsId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("transNumber").eq(String.valueOf(transNumber)));
        querySchema.addCondition(queryConditionGroup);
        List<FundPayment_b> bList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
        if (bList.isEmpty()) {
            QuerySchema query = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryCondition = new QueryConditionGroup(ConditionOperator.and);
            queryCondition.appendCondition(QueryCondition.name("id").eq(businessDetailsId));
            query.addCondition(queryCondition);
            bList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, query, null);
        }
        //汇总子表和主表映射，剔除非审批通过的子表
        //获取去重后的主表数据
        Set<String> mainIds = bList.stream().map(FundPayment_b::getMainid).collect(Collectors.toSet());
        //查询主表数据
        QuerySchema query = QuerySchema.create().addSelect("id,verifystate");
        QueryConditionGroup queryCondition = new QueryConditionGroup(ConditionOperator.and);
        queryCondition.appendCondition(QueryCondition.name("id").in(mainIds));
        queryCondition.appendCondition(QueryCondition.name("verifystate").eq(VerifyState.COMPLETED.getValue()));
        query.addCondition(queryCondition);
        List<FundPayment> mainList = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, query, null);
        Set<String> ids = mainList.stream().map(bizObject -> bizObject.getId().toString()).collect(Collectors.toSet());
        //剔除非审批通过的子表
        bList = bList.stream().filter(e -> ids.contains(e.getMainid())).collect(Collectors.toList());
        return bList;
    }

    private void updateTransferBillTwo(TransferAccount transferAccount, List<Object> successList) throws Exception {
        for (Object obj : successList) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof Result) {
                Result result = ((Result) obj);
                String transNumber = result.getTransNumber();
                transferAccount.setCollectid(Long.parseLong(transNumber));
                transferAccount.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
            }
        }
    }

    public static Map<String, String> getBankAccountInfo(Map<String, Object> oppositebankaccount) throws Exception {
        Map<String, String> map = new HashMap<>();
        BaseRefRpcService baseRefRpcService = AppContext.getBean(BaseRefRpcService.class);
        if (oppositebankaccount != null) {
            String acctName = (String) oppositebankaccount.get("acctName");//对方账户名称
            String account = (String) oppositebankaccount.get("account");//对方银行账号
            String bankNumberName = "";
            String bankType = "";
            String lineNumber = "";
            BankdotVO bankdotVO = baseRefRpcService.queryBandDotById((String) oppositebankaccount.get("bankNumber"));
            if (ObjectUtils.isNotEmpty(bankdotVO)) {
                bankNumberName = bankdotVO.getName();//对方开户行名
                lineNumber = bankdotVO.getLinenumber();//对方开户行联行号
            }
            BankVO bankVO = baseRefRpcService.queryBankTypeById((String) oppositebankaccount.get("bank"));
            if (ObjectUtils.isNotEmpty(bankVO)) {
                bankType = bankVO.getName();//对方银行类别
            }
            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(lineNumber)) {
                if (oppositebankaccount.get("lineNumber") != null) {
                    lineNumber = (String) oppositebankaccount.get("lineNumber");//对方开户行联行号
                }
            }
            map.put("acctName", acctName);
            map.put("account", account);
            map.put("bankNumberName", bankNumberName);
            map.put("bankType", bankType);
            map.put("lineNumber", lineNumber);
        }
        return map;
    }


    private void writingTheSettlementStatusOfDelegationBill(DataSettledDetail dataSettledDetail) throws Exception {
        List<CtmJSONObject> listreq = new ArrayList<>();
        if ((ValueUtils.isNotEmpty(dataSettledDetail.getBizObjType()) && FundCollection_b.ENTITY_NAME.equals(dataSettledDetail.getBizObjType()))
                || (ValueUtils.isNotEmpty(dataSettledDetail.getTradeTypeCode()) && "cmp_fundcollection_delegation".equals(dataSettledDetail.getTradeTypeCode()))
                || (ValueUtils.isNotEmpty(dataSettledDetail.getBusinessBillType()) && String.valueOf(BusinessBillType.CashRecBill.getValue()).equals(dataSettledDetail.getBusinessBillType()))) {
            //委托收款
            List<FundCollection_b> fcBillbs = getFundCollection_b(dataSettledDetail.getDataSettledId(), dataSettledDetail.getBusinessDetailsId());
            if (fcBillbs.size() > 0) {
                List<FundCollection_b> fundCollectionBs = fundCommonService.updateSettledInfoOfFundCollection(dataSettledDetail, fcBillbs, true);
                for (FundCollection_b billb : fundCollectionBs) {
                    //调结算工作台
                    AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
                    agentPaymentReqVO.setSettleDetailAId(billb.get("settledId").toString());
                    if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.AllSuccess.getValue()))) {
                        agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleSuccess.getValue()));
                    } else if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.AllFaital.getValue()))) {
                        agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleFailed.getValue()));
                    } else if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.PartSuccess.getValue()))) {
                        agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(SettleDetailSettleStateEnum.PART_SUCCESS.getValue()));
                        agentPaymentReqVO.setFailTotalAmt(dataSettledDetail.getStoppedamount());
                        agentPaymentReqVO.setSuccessTotalAmt(dataSettledDetail.getSuccesssettlementAmount());
                    }
                    // 获取业务单据的结算成功的时间
                    List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
                    Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                    if (dataSettled.isPresent()) {
                        agentPaymentReqVO.setSettlesuccessdate(DateUtils.convertToStr(dataSettled.get().getSettleSuccBizTime(), DateUtils.DATE_PATTERN));
                    }
                    //
                    fundCommonService.sendEventToSettleBenchDetail(agentPaymentReqVO);
                    //RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).newUpdateSettlementDetail(agentPaymentReqVO);
                    // 委托单据-止付时，解除与银行对账单的关联关系
                    if (dataSettledDetail.getWsettlementResult().equals(FundSettleStatus.SettleFailed.getValue())) {
                        // 1.解除与银行对账单的关联关系
                        if (billb.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == billb.getAssociationStatus().shortValue()) {
                            CtmJSONObject jsonReq = new CtmJSONObject();
                            if (StringUtils.isNotEmpty(billb.getBankReconciliationId())) {
                                jsonReq.put("busid", Long.parseLong(billb.getBankReconciliationId()));
                            }
                            if (StringUtils.isNotEmpty(billb.getBillClaimId())) {
                                jsonReq.put("claimid", Long.parseLong(billb.getBillClaimId()));
                            }
                            jsonReq.put("stwbbusid", billb.getId());
                            listreq.add(jsonReq);

                            // 2.清空对账单ID和认领单ID
                            billb.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                            billb.setBankReconciliationId(null);
                            billb.setBillClaimId(null);
                        }
                    }
                }
                EntityTool.setUpdateStatus(fundCollectionBs);
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionBs);
                if (CollectionUtils.isNotEmpty(listreq)) {
                    for (CtmJSONObject jsonReq : listreq) {
                        reWriteBusCorrDataService.resDelData(jsonReq);
                    }
                }
                if (CollectionUtils.isNotEmpty(fundCollectionBs)) {
                    for (FundCollection_b billb : fundCollectionBs) {
                        fundCommonService.generateVoucher(billb, FundCollection.ENTITY_NAME, false);
                    }
                }
            }
        } else if ((ValueUtils.isNotEmpty(dataSettledDetail.getBizObjType()) && FundPayment_b.ENTITY_NAME.equals(dataSettledDetail.getBizObjType()))
                || (ValueUtils.isNotEmpty(dataSettledDetail.getTradeTypeCode()) && "cmp_fund_payment_delegation".contains(dataSettledDetail.getTradeTypeCode()))
                || (ValueUtils.isNotEmpty(dataSettledDetail.getBusinessBillType()) && String.valueOf(BusinessBillType.CashPayBill.getValue()).equals(dataSettledDetail.getBusinessBillType()))) {
            //委托付款
            List<FundPayment_b> fpBillbs = getFundPayment_b(dataSettledDetail.getDataSettledId(), dataSettledDetail.getBusinessDetailsId());
            if (fpBillbs.size() > 0) {
                List<FundPayment_b> fundPaymentBs = fundCommonService.updateSettledInfoOfFundPayment(dataSettledDetail, fpBillbs, true);
                for (FundPayment_b billb : fundPaymentBs) {
                    if (billb.get("settledId").equals(dataSettledDetail.getDataSettledId())) {
                        if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.AllSuccess.getValue()))) {
                            billb.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                        } else {
                            billb.setFundSettlestatus(FundSettleStatus.SettleFailed);
                        }
                        billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                    }
                    //调结算工作台
                    AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
                    agentPaymentReqVO.setSettleDetailAId(billb.get("settledId").toString());
                    if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.AllSuccess.getValue()))) {
                        agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleSuccess.getValue()));
                    } else if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.AllFaital.getValue()))) {
                        agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(FundSettleStatus.SettleFailed.getValue()));
                    } else if (dataSettledDetail.getWsettlementResult().equals(String.valueOf(WSettlementResult.PartSuccess.getValue()))) {
                        agentPaymentReqVO.setStatementdetailstatus(Integer.valueOf(SettleDetailSettleStateEnum.PART_SUCCESS.getValue()));
                        agentPaymentReqVO.setFailTotalAmt(dataSettledDetail.getStoppedamount());
                        agentPaymentReqVO.setSuccessTotalAmt(dataSettledDetail.getSuccesssettlementAmount());
                    }
                    // 获取业务单据的结算成功的时间
                    List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
                    Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                    if (dataSettled.isPresent()) {
                        agentPaymentReqVO.setSettlesuccessdate(DateUtils.convertToStr(dataSettled.get().getSettleSuccBizTime(), DateUtils.DATE_PATTERN));
                    }
                    fundCommonService.sendEventToSettleBenchDetail(agentPaymentReqVO);
                    //RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).newUpdateSettlementDetail(agentPaymentReqVO);
                    // 委托单据-止付时，解除与银行对账单的关联关系
                    if (dataSettledDetail.getWsettlementResult().equals(FundSettleStatus.SettleFailed.getValue())) {
                        // 1.解除与银行对账单的关联关系
                        if (billb.getAssociationStatus() != null && AssociationStatus.Associated.getValue() == billb.getAssociationStatus().shortValue()) {
                            CtmJSONObject jsonReq = new CtmJSONObject();
                            if (StringUtils.isNotEmpty(billb.getBankReconciliationId())) {
                                jsonReq.put("busid", Long.parseLong(billb.getBankReconciliationId()));
                            }
                            if (StringUtils.isNotEmpty(billb.getBillClaimId())) {
                                jsonReq.put("claimid", Long.parseLong(billb.getBillClaimId()));
                            }
                            jsonReq.put("stwbbusid", billb.getId());
                            listreq.add(jsonReq);

                            // 2.清空对账单ID和认领单ID
                            billb.setAssociationStatus(AssociationStatus.NoAssociated.getValue());
                            billb.setBankReconciliationId(null);
                            billb.setBillClaimId(null);
                        }
                    }
                }
                EntityTool.setUpdateStatus(fundPaymentBs);
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentBs);
                if (CollectionUtils.isNotEmpty(listreq)) {
                    for (CtmJSONObject jsonReq : listreq) {
                        reWriteBusCorrDataService.resDelData(jsonReq);
                    }
                }
                if (CollectionUtils.isNotEmpty(fundPaymentBs)) {
                    for (FundPayment_b billb : fundPaymentBs) {
                        fundCommonService.generateVoucher(billb, FundPayment.ENTITY_NAME, false);
                    }
                }
            }
        }
    }

    private List<CheckDTO> setValueSuccess(TransferAccount transferAccount, Date today) {
        List<CheckDTO> checkDTOS = new ArrayList<>();

        CheckDTO checkDTO = new CheckDTO();
        //操作类型： 1.支票锁定/解锁接口、2.支票付票接口、3.支票兑付/背书接口、4.支票作废接口
        checkDTO.setOperationType(CheckOperationType.Cash);
        //锁定类型  锁定状态 1锁定 0解锁
        checkDTO.setLock(CmpLock.YES);
        //支票编号ID
        checkDTO.setCheckBillNo(String.valueOf(transferAccount.getCheckid()));
        //业务系统
        checkDTO.setSystem(SystemType.CashManager);
        //业务单据类型
        checkDTO.setBillType(BillType.TransferAccount);
        Long transferId = transferAccount.getId();
        //业务单据明细ID
        checkDTO.setInputBillNo(String.valueOf(transferId));
        //单据方向 2：付款 1：收款(按照结算的枚举来)
        checkDTO.setInputBillDir(CmpInputBillDir.Pay);
        //单据方向，字符串类型
        checkDTO.setInputBillDirString(CmpInputBillDir.Pay.getName());
        //支票状态
        checkDTO.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());
        //兑付方式
        checkDTO.setCashType(CashType.Business.getIndex());
        //兑付人
        checkDTO.setCashPerson(transferAccount.getCreator());
        //兑付日期
        checkDTO.setCashDate(new Date());
        //支票方向
        checkDTO.setCheckBillDir(CheckDirection.Pay.getIndex());
        //支票用途 默认0提现，1转账
        checkDTO.setCheckPurpose(CheckPurpose.VirtualToBank.getValue());
        //结算日期
        checkDTO.setSettlementDate(new Date());

        checkDTOS.add(checkDTO);
        return checkDTOS;
    }

    private CheckManage setValueFail(TransferAccount transferAccount) {
        CheckManage checkManage = new CheckManage();
        CheckManageDetail checkManageDetail = new CheckManageDetail();

        checkManage.setAccentity(transferAccount.getAccentity());

        List<CheckManageDetail> checkManageDetailList = new ArrayList<>();

        checkManage.setCheckManageDetail(checkManageDetailList);

        //转账工作台生成
        //生成方式
        //必填
        //枚举：资金结算生成、转账工作台生成 需要校验枚举值的必填性、准确性，否则接口调用失败
        checkManage.setGenerateType(GenerateType.TransferAccountGeneration.getValue());

        //转账工作台单据
        //id 业务单据 id
        //必填
        //需要校验枚举值的必填性，否则接口调用失败,生成方式+业务单据id唯一
        checkManage.setCode(transferAccount.getCode());

        //结算止付作废
        //处置说明(汇总)
        //非必填
        //长度200,需要校验枚举值的必填性及长度，否则接口调,用失败,不上送，默认赋值支票作废


        //作废
        //处置类型,(明细)
        //必填
        //枚举：作废、挂失、退回,需要校验枚举值的必填性、准确性，否则接口,调用失败
        checkManageDetail.setHandletypeDetail(HandleType.Delete.getValue());

        //结算止付作废
        //处置原因(明细)
        //非必填 长度200
        //需要校验枚举值的必填性及长度，否则接口调,用失败,不上送，默认赋值支票作废

        //支票id
        //支票id
        //必填
        //需要校验支票id的必填性、真实性，否则接口调用失败
        checkManageDetail.setCheckBillNo("");
        checkManageDetail.setCheckid(transferAccount.getCheckid());

        //创建人
        //处置人
        //必填
        checkManage.setCreatorId(transferAccount.getCreatorId());

        //支票状态
        checkManageDetail.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());

        checkManageDetailList.add(checkManageDetail);
        return checkManage;
    }

    //查询支票
    private List<CheckStock> getCheckStock(String swbillno, String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("checkBillNo").eq(swbillno));
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, schema, null);
    }

    private void transferAccountChange(TransferAccount transferAccount, DataSettledDetail dataSettledDetail) throws Exception {
        //todo 2024.12.11需求 结算成功办结，转账单的单据信息（结算方式、银行账号等）要按照结算返回的数据来更新
        //待结算数据收付类型：1收/2付
        if ("1".equals(dataSettledDetail.getRecpaytype())) {
            // 账户类型OppAccType = 2 是现金账户
            if (ObjectUtils.isNotEmpty(dataSettledDetail.getOppAccType()) && dataSettledDetail.getOppAccType() == 2) {
                //付款现金账户id
                transferAccount.setPayCashAccount(dataSettledDetail.getCounterpartybankaccount());
            } else if (ObjectUtils.isNotEmpty(dataSettledDetail.getOppAccType()) && dataSettledDetail.getOppAccType() == 3) {// 账户类型OppAccType = 3 是银行账户
                //付款银行账户id
                transferAccount.setPayBankAccount(dataSettledDetail.getCounterpartybankaccount());
            }
            //收款银行账户id
            transferAccount.setRecBankAccount(dataSettledDetail.getSettlemetBankAccountId());
            //收款结算方式
            transferAccount.setCollectsettlemode(dataSettledDetail.getExpectsettlemethodId());
        } else {//正常逻辑 第一笔推送的是付款的时候
            //付款银行账户id
            if (ObjectUtils.isNotEmpty(dataSettledDetail.getSettlemetBankAccountId()) && ObjectUtils.isNotEmpty(transferAccount.getPayBankAccount())
                    && !dataSettledDetail.getSettlemetBankAccountId().equals(transferAccount.getPayBankAccount())) {
                transferAccount.setPayBankAccount(dataSettledDetail.getSettlemetBankAccountId());
            }

            //付款现金账户id
            if (ObjectUtils.isNotEmpty(dataSettledDetail.getSettlemetCashAccountId()) && ObjectUtils.isNotEmpty(transferAccount.getPayCashAccount())
                    && !dataSettledDetail.getSettlemetCashAccountId().equals(transferAccount.getPayCashAccount())) {
                transferAccount.setPayCashAccount(dataSettledDetail.getSettlemetCashAccountId());
            }

            // 账户类型OppAccType = 2 是现金账户
            if (ObjectUtils.isNotEmpty(dataSettledDetail.getOppAccType()) && dataSettledDetail.getOppAccType() == 2) {
                if (ObjectUtils.isNotEmpty(dataSettledDetail.getCounterpartybankaccount()) && ObjectUtils.isNotEmpty(transferAccount.getRecCashAccount())
                        && !dataSettledDetail.getCounterpartybankaccount().equals(transferAccount.getRecCashAccount())) {
                    //收款现金账户id
                    transferAccount.setRecCashAccount(dataSettledDetail.getCounterpartybankaccount());
                }

            } else if (ObjectUtils.isNotEmpty(dataSettledDetail.getOppAccType()) && dataSettledDetail.getOppAccType() == 3) { // 账户类型OppAccType = 3 是银行账户
                if (ObjectUtils.isNotEmpty(dataSettledDetail.getCounterpartybankaccount()) && ObjectUtils.isNotEmpty(transferAccount.getRecBankAccount())
                        && !dataSettledDetail.getCounterpartybankaccount().equals(transferAccount.getRecBankAccount())) {
                    //收款银行账户id
                    transferAccount.setRecBankAccount(dataSettledDetail.getCounterpartybankaccount());
                }

            }
            if (ObjectUtils.isNotEmpty(transferAccount.getVirtualBank()) && transferAccount.getVirtualBank() == VirtualBank.VirtualToBank.getValue()) {
                //付款虚拟户
                transferAccount.setPayVirtualAccount(dataSettledDetail.getCounterpartybankaccount());
            } else if (ObjectUtils.isNotEmpty(transferAccount.getVirtualBank()) && transferAccount.getVirtualBank() == VirtualBank.BankToVirtual.getValue()) {
                //收款虚拟户
                transferAccount.setCollVirtualAccount(dataSettledDetail.getCounterpartybankaccount());
            }
            //付款结算方式id
            transferAccount.setSettlemode(dataSettledDetail.getExpectsettlemethodId());
        }

        //更新直联标识
        if (ObjectUtils.isNotEmpty(transferAccount.getPayBankAccount())) {
            String data = bankaccountSettingService.getOpenFlag(transferAccount.getPayBankAccount());
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
            if (null != jsonObject) {
                CtmJSONObject jsonData = jsonObject.getJSONObject("data");
                if (null != jsonData) {
                    if (("true").equals(jsonData.get("openFlag").toString())) {
                        transferAccount.setIsdirectconn(true);
                    } else {
                        transferAccount.setIsdirectconn(false);
                    }
                }
            }
        }

        //更新Swif码
        if (ObjectUtils.isNotEmpty(transferAccount.getRecBankAccount())) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(transferAccount.getRecBankAccount());
            if (enterpriseBankAcctVO != null && ObjectUtils.isNotEmpty(enterpriseBankAcctVO.getBankNumber())) {
                String bankNumber = enterpriseBankAcctVO.getBankNumber();
                if(bankNumber != null && bankNumber.length() > 0){
                    BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(bankNumber);
                    if (bankdotVO != null) {
                        transferAccount.setSwiftCode(bankdotVO.getSwiftCode());
                    }
                }
            }
        }

    }

}
