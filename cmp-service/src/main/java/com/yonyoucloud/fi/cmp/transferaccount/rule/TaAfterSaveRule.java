package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.intelligentapproval.CmpIntelligentAudit;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * @ClassName TaAfterSaveRule
 * @Desc 转账单保存rule
 * @Author nayunhao
 * @Date 2019/10/10
 * @Version 1.0
 */
@Component("taAfterSaveRule")
@Slf4j
public class TaAfterSaveRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private CmpVoucherService cmpVoucherService;
    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    private CmpIntelligentAudit cmpIntelligentAudit;
    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Autowired
    private TransTypeQueryService transTypeQueryService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        List<Journal> newJournals = new ArrayList<>(2);
        BdTransType bdTransType = transTypeQueryService.findById(bizObject.get("tradetype"));
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeType = (String) jsonObject.get("transferType_zz");
        //如果推送待结算，则返回true
        Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
        Set<String> tradeTypeSet = new HashSet<String>(){{
            add("yhzz");
            add("sbqbcz");
            add("sbqbhz");
            add("sbqbtx");
        }};
        if (tradeTypeSet.contains(tradeType)) {
            // 银行转账
            if(!pushSettlement){
                newJournals.add(generateJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"), true, bizObject.get("recBankAccount"), true, billContext));
                newJournals.add(generateJournal(bizObject, Direction.Debit, bizObject.get("recBankAccount"), true, bizObject.get("payBankAccount"), true,billContext));
                Journal journal = generateBroJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"), true, bizObject.get("recBankAccount"), true);
                if(journal!=null){
                    newJournals.add(journal);
                }
                journal = generateBroJournal(bizObject, Direction.Debit, bizObject.get("recBankAccount"), true, bizObject.get("payBankAccount"), true);
                if(journal!=null){
                    newJournals.add(journal);
                }
            }
        } else if ("sc".equals(bizObject.get("type")) || "SC".equals(bizObject.get("tradetype_code")) || "jcxj".equals(tradeType)) {
            // 缴存现金
            if(!pushSettlement){
                newJournals.add(generateJournal(bizObject, Direction.Credit, bizObject.get("payCashAccount"), false, bizObject.get("recBankAccount"), true, billContext));
                newJournals.add(generateJournal(bizObject, Direction.Debit, bizObject.get("recBankAccount"), true, bizObject.get("payCashAccount"), false, billContext));
                Journal journal = generateBroJournal(bizObject, Direction.Credit, bizObject.get("payCashAccount"), false, bizObject.get("recBankAccount"), true);
                if(journal!=null){
                    newJournals.add(journal);
                }
                journal = generateBroJournal(bizObject, Direction.Debit, bizObject.get("recBankAccount"), true, bizObject.get("payCashAccount"), false);
                if(journal!=null){
                    newJournals.add(journal);
                }
            }
        } else if ("ct".equals(bizObject.get("type")) || "CT".equals(bizObject.get("tradetype_code")) || "xjhz".equals(tradeType)) {
            // 现金互转
            if(!pushSettlement){
                newJournals.add(generateJournal(bizObject, Direction.Credit, bizObject.get("payCashAccount"), false, bizObject.get("recCashAccount"), false, billContext));
                newJournals.add(generateJournal(bizObject, Direction.Debit, bizObject.get("recCashAccount"), false, bizObject.get("payCashAccount"), false, billContext));
                Journal journal = generateBroJournal(bizObject, Direction.Credit, bizObject.get("payCashAccount"), false, bizObject.get("recCashAccount"), false);
                if(journal!=null){
                    newJournals.add(journal);
                }
                journal = generateBroJournal(bizObject, Direction.Debit, bizObject.get("recCashAccount"), false, bizObject.get("payCashAccount"), false);
                if(journal!=null){
                    newJournals.add(journal);
                }
            }
        } else if ("ec".equals(bizObject.get("type")) || "EC".equals(bizObject.get("tradetype_code")) || "tqxj".equals(tradeType)){
            // 提取现金
            if(!pushSettlement){
                newJournals.add(generateJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"), true, bizObject.get("recCashAccount"), false, billContext));
                newJournals.add(generateJournal(bizObject, Direction.Debit, bizObject.get("recCashAccount"), false, bizObject.get("payBankAccount"), true, billContext));
                Journal journal = generateBroJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"), true, bizObject.get("recCashAccount"), false);
                if(journal!=null){
                    newJournals.add(journal);
                }
                journal = generateBroJournal(bizObject, Direction.Debit, bizObject.get("recCashAccount"), false, bizObject.get("payBankAccount"), true);
                if(journal!=null){
                    newJournals.add(journal);
                }
            }
        }else if("tpt".equals(bizObject.get("type")) || "TPT".equals(bizObject.get("tradetype_code")) || "dsfzz".equals(tradeType)){
            if(!pushSettlement){
                if(bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.BankToVirtual.getValue())){
                    newJournals.add(generateJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"), true, bizObject.get("collVirtualAccount"),true,billContext));
                    Journal journal = generateBroJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"),true, bizObject.get("collVirtualAccount"), true);
                    if(journal!=null){
                        newJournals.add(journal);
                    }
                }else if(bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.VirtualToBank.getValue())){
                    newJournals.add(generateJournal(bizObject, Direction.Debit, bizObject.get("recBankAccount"), true, bizObject.get("payVirtualAccount"),true,billContext));
                    Journal journal = generateBroJournal(bizObject, Direction.Debit, bizObject.get("recBankAccount"), true, bizObject.get("payVirtualAccount"),true);
                    if(journal!=null){
                        newJournals.add(journal);
                    }
                }
            }
        } else {
            log.error("交易类型错误，type : " + bizObject.get("type") + ", tradetype_code : " + bizObject.get("tradetype_code") + ", tradeType : " + tradeType);
        }

        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.getId());
        if(newJournals.size()>0){
            if (bizObject.get("paystatus") != null && bizObject.getShort("paystatus").compareTo(PayStatus.Success.getValue()) == 0) {
                // CZFW-399190 【日常】转账单不推送结算，网银支付-支付变更为成功后，没有更新结算状态、结算日期、结算成功金额
                transferAccount.setSettlestatus(SettleStatus.alreadySettled);
                transferAccount.setSettledate(new Date());
                transferAccount.setSettleSuccessAmount(transferAccount.getOriSum());
                cmpVoucherService.generateVoucherWithResult(bizObject);
                // 更新凭证状态为过账中
                transferAccount.setVoucherstatus(VoucherStatus.POSTING.getValue());
                QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate");
                queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(transferAccount.getId()));
                List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
                Date currentDate = new Date();
                for (Journal journal : journals) {
                    journal.setDzdate(DateUtils.getCurrentDate(null));
                    journal.setDztime(currentDate);
                    journal.setAuditstatus(AuditStatus.Complete);
                    journal.setSettlestatus(SettleStatus.alreadySettled);
                }
                EntityTool.setUpdateStatus(journals);
                MetaDaoHelper.update(Journal.ENTITY_NAME, journals);

                if (cmpBudgetTransferAccountManagerService.implement(transferAccount)){
                    transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
                transferAccount.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
            } else {
                CmpWriteBankaccUtils.delAccountBook(bizObject.get("id").toString());
                for (Journal journal:newJournals){
                    //回退逻辑
                    if (bizObject.get("paystatus") != null && bizObject.getShort("paystatus").compareTo(PayStatus.Success.getValue()) == 0) {
                        if (bizObject.get("dzdate") != null){
                            journal.setDzdate(bizObject.get("dzdate"));
                        }else {
                            journal.setDzdate(DateUtils.getCurrDateOrBusinessDate());
                        }
                    }
                    cmpWriteBankaccUtils.addAccountBook(journal);
                }
            }

        }

        cmpIntelligentAudit.auditStart(bizObject, IBillNumConstant.TRANSFERACCOUNT, ICmpConstant.CM_CMP_TRANSFERACCOUNT, BusinessPart.save.getValue());
        return new RuleExecuteResult();
    }

    /**
     * @return com.yonyoucloud.fi.cmp.journal.Journal
     * @Author tongyd
     * @Description 生成日记账
     * @Date 2019/10/12
     * @Param [bizObject, direction, accountId, isBank]
     **/
    private Journal generateJournal(BizObject bizObject, Direction direction, String accountId, boolean isBank, String otherAccountId, boolean isBankOther, BillContext billContext) throws Exception {
        Journal journal = new Journal();
        journal.setAccentity( bizObject.get(IBussinessConstant.ACCENTITY));
        boolean thirdPartyTransferJournal = bizObject.get("srcitem").equals(EventSource.ThreePartyReconciliation.getValue());
        if (isBank) {
            journal.setBankaccount(accountId);
            if (!thirdPartyTransferJournal){
                if (direction == Direction.Debit) {
                    journal.setBankaccountno(bizObject.get("recBankAccount_account"));
                } else {
                    journal.setBankaccountno(bizObject.get("payBankAccount_account"));
                }
            }
            if (!StringUtils.isEmpty(accountId)) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(accountId);
                if(enterpriseBankAcctVO == null){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100868"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C5", "存在可能销户或非法的银行账户，保存失败请重新检查！") /* "存在可能销户或非法的银行账户，保存失败请重新检查！" */);
                }
                journal.setBanktype(enterpriseBankAcctVO.getBank());
                if (thirdPartyTransferJournal){
                    journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
                }
            }
        } else {
            journal.setCashaccount(accountId);
            if (direction == Direction.Debit) {
                journal.setCashaccountno(bizObject.get("recCashAccount_code"));
            } else {
                journal.setCashaccountno(bizObject.get("payCashAccount_code"));
            }
        }
        if (isBankOther) {
            journal.setOtherbankaccount(otherAccountId);
        } else {
            journal.setOthercashaccount(otherAccountId);
        }
        journal.setNatCurrency(bizObject.get("natCurrency"));
        journal.setCurrency(bizObject.get("currency"));
        journal.setVouchdate(bizObject.get("vouchdate"));
        journal.setDescription(bizObject.get("description"));
        journal.setTradetype(bizObject.get("tradetype"));
        journal.setExchangerate(bizObject.get("exchRate"));
        journal.setExchangerateOps(bizObject.get("exchRateOps"));
        journal.setSettlemode(bizObject.get("settlemode"));
        journal.setNoteno(bizObject.get("noteno"));
        journal.setCheckflag(false);
        journal.setInsidecheckflag(false);
        journal.setProject(bizObject.get("project"));
        journal.setCostproject(bizObject.get("expenseitem"));
        journal.setRefund(false);
        journal.setSrcbillno(bizObject.get("code"));
        journal.setSrcbillitemid(bizObject.get("id").toString());
        journal.setOrg(bizObject.get("org"));
        journal.set("billnum", bizObject.get("code"));
        journal.setCreateDate(new Date());
        journal.setCreateTime(new Date());
        journal.setCreatorId(AppContext.getCurrentUser().getId());
        journal.setCreator(AppContext.getCurrentUser().getName());
        journal.setTenant(bizObject.get("tenant"));
        journal.setDirection(direction);

        journal.setProject(bizObject.get("project"));
        journal.setDept(bizObject.get("dept"));
        //对方类型
        journal.setCaobject(CaObject.InnerUnit);
        journal.setInnerunit(bizObject.get("accentity"));


        if (bizObject.get("transeqno") != null) {
            journal.set("transeqno", bizObject.get("transeqno"));//交易流水号
        }
        if (direction == Direction.Debit) {
            journal.setDebitoriSum(bizObject.get(IBussinessConstant.ORI_SUM));
            journal.setDebitnatSum(currencyQueryService.getAmountOfCurrencyPrecision(journal.getNatCurrency(),
                    new BigDecimal(bizObject.getString(IBussinessConstant.NAT_SUM))));
            journal.setCreditoriSum(BigDecimal.ZERO);
            journal.setCreditnatSum(BigDecimal.ZERO);
            //财资统一对账码传递，借方，传付款的对账码
            journal.setBankcheckno(bizObject.get("paysmartcheckno"));

        } else {
            journal.setDebitoriSum(BigDecimal.ZERO);
            journal.setDebitnatSum(BigDecimal.ZERO);
            journal.setCreditoriSum(bizObject.get(IBussinessConstant.ORI_SUM));
            journal.setCreditnatSum(currencyQueryService.getAmountOfCurrencyPrecision(journal.getNatCurrency(),
                    new BigDecimal(bizObject.getString(IBussinessConstant.NAT_SUM))));
            //财资统一对账码传递，贷方，传收款的对账码
            journal.setBankcheckno(bizObject.get("smartcheckno"));
        }
        journal.set("srcitem", bizObject.get("srcitem"));
        journal.set("billtype", bizObject.get("billtype"));
        journal.set("paymentstatus", bizObject.get("paystatus"));
        journal.set("auditstatus", bizObject.get("auditstatus"));
        journal.set("settlestatus", bizObject.get("settlestatus"));
        journal.setBillno(billContext.getBillnum());
        journal.setServicecode("ficmp0052");
        //添加源头单据信息处理
        if (bizObject.get("srcitem").toString().equals(String.valueOf(EventSource.Drftchase.getValue()))) {
            if (bizObject.get("srcbillno") != null) {
                journal.set("topsrcbillno", bizObject.get("srcbillno").toString());
            }
            if (bizObject.get("srcbillid") != null) {
                journal.set("topsrcbillid", bizObject.get("srcbillid").toString());
            }
        }
        journal.set("topsrcitem", bizObject.get("srcitem"));
        // 转账单
        journal.set("topbilltype", bizObject.get("billtype"));
        return journal;
    }

    /**
     * @return com.yonyoucloud.fi.cmp.journal.Journal
     * @Author nayh
     * @Description 生成手续费日记账
     * @Date 2019/10/12
     * @Param [bizObject, direction, accountId, isBank]
     **/
    private Journal generateBroJournal(BizObject bizObject, Direction direction, String accountId, boolean isBank, String otherAccountId, boolean isBankOther) throws Exception {
        Journal journal = new Journal();
        journal.setAccentity( bizObject.get(IBussinessConstant.ACCENTITY));
        boolean thirdPartyTransferJournal = bizObject.get("srcitem").equals(EventSource.ThreePartyReconciliation.getValue());
        if (isBank) {
            journal.setBankaccount(accountId);
            if (!thirdPartyTransferJournal){
                if (direction == Direction.Debit) {
                    journal.setBankaccountno(bizObject.get("recBankAccount_account"));
                } else {
                    journal.setBankaccountno(bizObject.get("payBankAccount_account"));
                }
            }
            if (!StringUtils.isEmpty(accountId)) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(accountId.toString());
                journal.setBanktype(enterpriseBankAcctVO.getBank());
                if (thirdPartyTransferJournal){
                    journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
                }
            }
        } else {
            journal.setCashaccount(accountId);
            if (direction == Direction.Debit) {
                journal.setCashaccountno(bizObject.get("recCashAccount_code"));
            } else {
                journal.setCashaccountno(bizObject.get("payCashAccount_code"));
            }
        }
        if (isBankOther) {
            journal.setOtherbankaccount(otherAccountId);
        } else {
            journal.setOthercashaccount(otherAccountId);
        }
        journal.setNatCurrency(bizObject.get("natCurrency"));
        journal.setCurrency(bizObject.get("currency"));
        journal.setVouchdate(bizObject.get("vouchdate"));
        journal.setDescription(bizObject.get("description"));
        journal.setTradetype(bizObject.get("tradetype"));
        journal.setExchangerate(bizObject.get("exchRate"));
        journal.setExchangerateOps(bizObject.get("exchRateOps"));
        journal.setSettlemode(bizObject.get("settlemode"));
        journal.setNoteno(bizObject.get("noteno"));
        journal.setCheckflag(false);
        journal.setInsidecheckflag(false);
        journal.setProject(bizObject.get("project"));
        journal.setCostproject(bizObject.get("expenseitem"));
        journal.setRefund(false);
        journal.setSrcbillno(bizObject.get("code"));
        journal.setSrcbillitemid(bizObject.get("id").toString());
        journal.setOrg(bizObject.get("org"));
        journal.set("billnum", bizObject.get("code"));
        journal.setCreateDate(new Date());
        journal.setCreateTime(new Date());
        journal.setCreatorId(AppContext.getCurrentUser().getId());
        journal.setCreator(AppContext.getCurrentUser().getName());
        journal.setTenant(bizObject.get("tenant"));
        journal.setDirection(Direction.Credit);

        journal.setProject(bizObject.get("project"));
        journal.setDept(bizObject.get("dept"));
        //对方类型
        journal.setCaobject(CaObject.InnerUnit);
        journal.setInnerunit(bizObject.get("accentity"));
        if (bizObject.get("transeqno") != null) {
            journal.set("transeqno", bizObject.get("transeqno"));//交易流水号
        }
        //手续费原币本币计算，本币=原币*汇率
        if (direction == Direction.Debit) {
            if(bizObject.get("inBrokerage")!=null){
                journal.setCreditoriSum(bizObject.get("inBrokerage"));
                journal.setCreditnatSum(currencyQueryService.getAmountOfCurrencyPrecision(journal.getNatCurrency(),
                        CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(journal.getExchangerateOps(), journal.getExchangerate(), bizObject.get("inBrokerage"), null)));
            }
        } else {
            if(bizObject.get("outBrokerage")!=null){
                journal.setCreditoriSum(bizObject.get("outBrokerage"));
                journal.setCreditnatSum(currencyQueryService.getAmountOfCurrencyPrecision(journal.getNatCurrency(),
                        CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(journal.getExchangerateOps(), journal.getExchangerate(), bizObject.get("outBrokerage"), null)));
            }
        }

        journal.setDebitoriSum(BigDecimal.ZERO);
        journal.setDebitnatSum(BigDecimal.ZERO);

        if(journal.getCreditoriSum()==null||journal.getCreditoriSum().compareTo(BigDecimal.ZERO)< 1){
            return null;
        }
        journal.setBillno("cm_transfer_account");
        journal.setServicecode("ficmp0052");
        journal.set("srcitem", bizObject.get("srcitem"));
        journal.set("billtype", bizObject.get("billtype"));
        journal.set("topsrcitem", bizObject.get("srcitem"));
        // 转账单
        journal.set("topbilltype", bizObject.get("billtype"));
        journal.set("paymentstatus", bizObject.get("paystatus"));
        journal.set("auditstatus", bizObject.get("auditstatus"));
        journal.set("settlestatus", bizObject.get("settlestatus"));
        return journal;
    }

}
