package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.ResultList;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyoucloud.fi.arap.service.account.IArapSettleAccountService;
import com.yonyoucloud.fi.arap.service.third.IArapBillCommonService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

/**
 * <h1>保存参照银行对账单生成付款单时，当生成应付付款单时，直接保存</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-12-07 16:42
 */
@Slf4j
public class BankreconciliationRuleToSaveArapBillRule extends AbstractCommonRule {
    private static Map<String, PayBill> map_payBill = new HashMap<> ();
    private static Map<String, ReceiveBill> map_receiveBill = new HashMap<> ();
    @Autowired
    JournalService journalService;
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

        billContext.getBillnum();
        List<BizObject> bills = getBills(billContext, paramMap);
        Set<String> setCashMark = new HashSet<>(8);
        Set<String> setCampeche = new HashSet<>(8);
        bills.forEach(e -> {
            if(null != e.get("billtype") && e.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))){
                if (String.valueOf(EventSource.Cmpchase.getValue()).equals(e.get("srcitem").toString())) {
                    setCampeche.add(e.get("srcitem").toString());
                } else {
                    setCashMark.add(e.get("srcitem").toString());
                }
            }
        });

        if (setCashMark.size() > 0 && setCampeche.size() > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100255"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180106","参照银行对账单生单时，不能同时生成现金管理的付款单和应收应付的付款单，请检查所选银行对账单数据的对方账号和对方户名字段是否一致。") /* "参照银行对账单生单时，不能同时生成现金管理的付款单和应收应付的付款单，请检查所选银行对账单数据的对方账号和对方户名字段是否一致。" */));
        }
        if (setCashMark.size() == 0 && setCampeche.size() >= 0) {
            return new RuleExecuteResult();
        }
        List<ReceiveBill> receiveBillList = new ArrayList<>();
        List<PayBill> payBillList = new ArrayList<>();
        // 创建单据
        List<BankReconciliation> bankReconciliationList_tem = new ArrayList<>();
        for (BizObject bizObject : bills) {
            if ("arap.paybill.PayBill".equals(billContext.getFullname())) {
                createPayBillList(bizObject, payBillList,bankReconciliationList_tem);
            }
            if ("arap.receivebill.ReceiveBill".equals(billContext.getFullname())) {
                createReceiveBillList(bizObject, receiveBillList,bankReconciliationList_tem);
            }
        }
        ResultList resultList;
        if (receiveBillList.size() > 0) {
            resultList = new ResultList(receiveBillList.size());
        } else {
            resultList = new ResultList(payBillList.size());
        }
        // 收款单   生成凭证 入库 登记日记账
        if ("arap.paybill.PayBill".equals(billContext.getFullname())) {
            savePayBillPull(payBillList, bankReconciliationList_tem, resultList);
        }
        if ("arap.receivebill.ReceiveBill".equals(billContext.getFullname())) {
            saveReceiveBillPull(receiveBillList, bankReconciliationList_tem, resultList);
        }


        // 生单成功后更新对账单状态 已生单
        for (BankReconciliation bank : bankReconciliationList_tem) {
            bank.setAutobill(true);//已生单
            bank.setCheckflag(true);//已勾兑
            EntityTool.setUpdateStatus(bank);
        }
        // 老架构单据不作处理
        CommonSaveUtils.updateBankReconciliation(bankReconciliationList_tem);

        RuleExecuteResult result = new RuleExecuteResult(resultList);
        result.setCancel(true);
        return result;
    }

    private void saveReceiveBillPull(List<ReceiveBill> receiveBillList, List<BankReconciliation> bankReconciliationList_tem, ResultList resultList) {
        if (CollectionUtils.isNotEmpty(receiveBillList)) {
            for (ReceiveBill receiveBill : receiveBillList) {
                try {
                    fiarap4SettleAlready(receiveBill, "AR");
                    BizObject bizObject = RemoteDubbo.get(IArapBillCommonService.class, IDomainConstant.MDD_DOMAIN_FIARAP).saveCmpAutoBill(receiveBill);
                    Journal receiveBillJournal = createJournal(bizObject, Direction.Debit);
                    journalService.compute4Save(bizObject, receiveBillJournal, Direction.Debit);
                    bizObject.set("_entityName", ReceiveBill.ENTITY_NAME);
                    cmpVoucherService.generateVoucherWithResult(bizObject);
                    resultList.incSucessCount();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    resultList.incFailCount();
                    resultList.addMessage(e.getMessage());
                    BankReconciliation bankReconciliation = null;
                    for (Map.Entry<String, ReceiveBill> mm : map_receiveBill.entrySet()) {
                        if (mm.getValue() == receiveBill) {
                            for (BankReconciliation bankReconciliation1 : bankReconciliationList_tem) {
                                if (bankReconciliation1.getId().equals(mm.getKey())) {
                                    bankReconciliation = bankReconciliation1;
                                    break;
                                }
                            }
                            if (bankReconciliation != null) {
                                bankReconciliationList_tem.remove(bankReconciliation);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void savePayBillPull(List<PayBill> payBillList, List<BankReconciliation> bankReconciliationList_tem, ResultList resultList) {
        if (CollectionUtils.isNotEmpty(payBillList)) {
            for (PayBill payBill : payBillList) {
                try {
                    fiarap4SettleAlready(payBill, "AP");
                    BizObject bizObject = RemoteDubbo.get(IArapBillCommonService.class, IDomainConstant.MDD_DOMAIN_FIARAP).saveCmpAutoBill(payBill);
                    Journal payBillJournal = createJournal(bizObject, Direction.Credit);
                    journalService.compute4Save(bizObject, payBillJournal, Direction.Credit);
                    bizObject.set("_entityName", PayBill.ENTITY_NAME);
                    cmpVoucherService.generateVoucherWithResult(bizObject);
                    resultList.incSucessCount();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    resultList.incFailCount();
                    resultList.addMessage(e.getMessage());
                    BankReconciliation bankReconciliation = null;
                    for (Map.Entry<String, PayBill> mm : map_payBill.entrySet()) {
                        if (mm.getValue() == payBill) {
                            for (BankReconciliation bankReconciliation1 : bankReconciliationList_tem) {
                                if (bankReconciliation1.getId().equals(mm.getKey())) {
                                    bankReconciliation = bankReconciliation1;
                                    break;
                                }
                            }
                            if (bankReconciliation != null) {
                                bankReconciliationList_tem.remove(bankReconciliation);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断应收应付系统是否支持结算
     * @param bizObject
     * @param arap
     * @throws Exception
     */
    private void fiarap4SettleAlready(BizObject bizObject, String arap) throws Exception {
        Boolean settleAccountAlready = RemoteDubbo.get(IArapSettleAccountService.class, IDomainConstant.MDD_DOMAIN_FIARAP).isSettleAccountAlready(bizObject);
        if (settleAccountAlready && Objects.equals("AR", arap)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100256"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180107","应收已月结，无法生成") /* "应收已月结，无法生成" */);
        } else if (settleAccountAlready && Objects.equals("AP", arap)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100257"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180108","应付已月结，无法生成") /* "应付已月结，无法生成" */);
        }
    }

    private void createPayBillList(BizObject bizObject, List<PayBill> payBillList,List<BankReconciliation> bankReconciliationList_tem) throws Exception {
        PayBill payBill = new PayBill();
        payBill.init(bizObject);
        payBill.setAccentity(bizObject.get(IBussinessConstant.ACCENTITY).toString());
        String srcbillid = bizObject.get("srcbillid").toString();
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, Long.parseLong(srcbillid));
        checkSettle(bankReconciliation);
        Long periodID = QueryBaseDocUtils.queryPeriodIdByAccbodyAndDate(bizObject.get(IBussinessConstant.ACCENTITY),bankReconciliation.getTran_date());
        payBill.setSrcitem(EventSource.Manual);
        payBill.setPeriod(periodID);
        payBill.setSrctypeflag(ICmpConstant.AUTO_BILL_FLAG);
        payBill.setCmpflag(true);
        payBill.setAuditstatus(AuditStatus.Complete);
        payBill.setSettlestatus(SettleStatus.alreadySettled);
        payBill.setPaystatus(PayStatus.OfflinePay);
        payBill.setVoucherstatus(VoucherStatus.Empty);
        payBill.setStatus(Status.confirmed);
        Date curDate = new Date();
        payBill.setDescription(bankReconciliation.getUse_name());
        payBill.setWriteoffstatus(WriteOffStatus.Incomplete);
        payBill.setAuditor(AppContext.getCurrentUser().getName());
        payBill.setAuditDate(curDate);
        payBill.setAuditTime(curDate);
        payBill.setSettleuser(AppContext.getCurrentUser().getName());
        payBill.setSettledate(curDate);
        payBill.setCreator(AppContext.getCurrentUser().getName());
        payBill.setCreateDate(curDate);
        payBill.setCreateTime(curDate);
        payBill.setId(ymsOidGenerator.nextId());
        if(null != BillInfoUtils.getBusinessDate()) {
            payBill.setDzdate(BillInfoUtils.getBusinessDate());
        }else {
            payBill.setDzdate(new Date());
        }
        bankReconciliationList_tem.add(bankReconciliation);
        map_payBill.put(bankReconciliation.getId(),payBill);
        payBillList.add(payBill);
    }

    private void createReceiveBillList(BizObject bizObject, List<ReceiveBill> receiveBillList,List<BankReconciliation> bankReconciliationList_tem) throws Exception {
        ReceiveBill receiveBill = new ReceiveBill();
        receiveBill.init(bizObject);
        receiveBill.setAccentity(bizObject.get(IBussinessConstant.ACCENTITY).toString());
        String srcbillid = bizObject.get("srcbillid").toString();
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, Long.parseLong(srcbillid));
        checkSettle(bankReconciliation);
        Long periodID = QueryBaseDocUtils.queryPeriodIdByAccbodyAndDate(bizObject.get(IBussinessConstant.ACCENTITY).toString(),bankReconciliation.getTran_date());
        receiveBill.setPeriod(periodID);
        receiveBill.setBasebilltype("FICA1");
        receiveBill.setBasebilltypecode("arap_receipt");
        receiveBill.setBookAmount(BigDecimal.ZERO);
        receiveBill.set("paytype", 2);
        receiveBill.setBilltype(EventType.CashMark);
        receiveBill.setInitflag(false);
        receiveBill.setAuditstatus(AuditStatus.Complete);
        receiveBill.setSettlestatus(SettleStatus.alreadySettled);
        receiveBill.setStatus(Status.confirmed);
        receiveBill.setCmpflag(true);
        receiveBill.setVoucherstatus(VoucherStatus.Empty);
        Date curDate = new Date();
        receiveBill.setDescription(bankReconciliation.getUse_name());
        receiveBill.setWriteoffstatus(WriteOffStatus.Incomplete);
        receiveBill.setAuditorId(AppContext.getCurrentUser().getId());
        receiveBill.setAuditor(AppContext.getCurrentUser().getName());
        receiveBill.setAuditDate(curDate);
        receiveBill.setAuditTime(curDate);
        receiveBill.setSettleuser(AppContext.getCurrentUser().getName());
        receiveBill.setSettledate(curDate);
        receiveBill.setCreator(AppContext.getCurrentUser().getName());
        receiveBill.put("creatorId", AppContext.getCurrentUser().getId());
        receiveBill.setCreateDate(curDate);
        receiveBill.setCreateTime(curDate);
        receiveBill.setId(ymsOidGenerator.nextId());
        if(null != BillInfoUtils.getBusinessDate()) {
            receiveBill.setDzdate(BillInfoUtils.getBusinessDate());
        }else {
            receiveBill.setDzdate(new Date());
        }
        bankReconciliationList_tem.add(bankReconciliation);
        map_receiveBill.put(bankReconciliation.getId(),receiveBill);
        receiveBillList.add(receiveBill);
    }

    /**
     * 检查当前日期是否日结
     *
     * @param bankReconciliation
     * @throws Exception
     */
    private void checkSettle(BankReconciliation bankReconciliation) throws Exception {
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankReconciliation.getAccentity())));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(DateUtils.strToDate(DateUtils.dateToStr(bankReconciliation.getTran_date())))));
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        querySchema.addCondition(condition);
        List<Map<String, Object>> settlementDetailList = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchema);
        if (CollectionUtils.isNotEmpty(settlementDetailList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100258"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180109","当前日期已经日结，不允许新增单据") /* "当前日期已经日结，不允许新增单据" */);
        }
    }

    /**
     * 登记日记账
     *
     * @param bizObject
     * @param direction
     * @return
     * @throws Exception
     */
    private Journal createJournal(BizObject bizObject, Direction direction) throws Exception {
        Journal journal = new Journal();
        journal.set(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
        journal.set("period", bizObject.get("period"));
        journal.set("bankaccount", bizObject.get("enterprisebankaccount"));
        EnterpriseBankAcctVO enterpriseBankAcctVO= baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));
        if(enterpriseBankAcctVO != null){
            journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
            journal.setBanktype(enterpriseBankAcctVO.getBank());
        }
        journal.set("cashaccount", bizObject.get("cashaccount"));
        journal.set("cashaccountno", "");
        journal.set("currency", bizObject.get("currency"));
        if (Direction.Debit.equals(direction)) {
            journal.set("direction", Direction.Debit.getValue());
            journal.set("debitoriSum", bizObject.get(IBussinessConstant.ORI_SUM));
            journal.set("debitnatSum", bizObject.get(IBussinessConstant.NAT_SUM));
            journal.set("creditoriSum", BigDecimal.ZERO);
            journal.set("creditnatSum", BigDecimal.ZERO);
            journal.set("rptype", RpType.ReceiveBill.getValue());
        }
        if (Direction.Credit.equals(direction)) {
            journal.set("direction", Direction.Credit.getValue());
            journal.set("debitoriSum", BigDecimal.ZERO);
            journal.set("debitnatSum", BigDecimal.ZERO);
            journal.set("creditoriSum", bizObject.get(IBussinessConstant.ORI_SUM));
            journal.set("creditnatSum", bizObject.get(IBussinessConstant.NAT_SUM));
            journal.set("rptype", RpType.PayBill.getValue());
        }
        Date vouchdate = bizObject.get("vouchdate");
        journal.set("vouchdate", vouchdate);
        journal.set("dzdate", vouchdate);
        journal.set("description", bizObject.get("description"));
        journal.set("srcitem", bizObject.get("srcitem"));
        journal.set("billtype", bizObject.get("billtype"));
        journal.set("topsrcitem", bizObject.get("srcitem"));
        journal.set("topbilltype", bizObject.get("billtype"));
        journal.set("tradetype", bizObject.get("tradetype"));
        journal.set("exchangerate", bizObject.get("exchRate"));
        journal.set("settlemode", bizObject.get("settlemode"));
        journal.set("oribalance", bizObject.get(IBussinessConstant.ORI_SUM));
        journal.set("natbalance", bizObject.get(IBussinessConstant.NAT_SUM));
        journal.set("noteno", bizObject.get("noteno"));
        journal.set("bankbilltype", "");
        // journal.set("bankbilldate","");
        journal.set("customerbankaccount", bizObject.get("customerbankaccount"));
        journal.set("customerbankname", bizObject.get("customerbankname"));
        journal.set("supplierbankaccount", bizObject.get("supplierbankaccount"));
        journal.set("supplierbankname", bizObject.get("supplierbankname"));
        journal.set("employeeaccount", bizObject.get("staffBankAccount"));
        journal.set("caobject", bizObject.get("caobject"));
        journal.set("customer", bizObject.get("customer"));
        journal.set("supplier", bizObject.get("supplier"));
        journal.set("employee", bizObject.get("employee"));
        journal.set("dept", bizObject.get("dept"));
        journal.set("checkflag", true);
        journal.set("insidecheckflag", false);
        if (bizObject.get("paystatus") != null) {
            journal.set("paymentstatus", bizObject.get("paystatus"));
        } else {
            journal.set("paymentstatus", PaymentStatus.NoPay.getValue());
        }
        journal.set("auditstatus", bizObject.get("auditstatus"));
        journal.set("settlestatus", bizObject.get("settlestatus"));
        journal.set("project", bizObject.get("project"));
        journal.set("costproject", bizObject.get("expenseitem"));
        journal.set("refund", false);
        journal.set("bookkeeper", AppContext.getCurrentUser().getId());
        journal.set("auditinformation", "");
        journal.set("srcbillno", bizObject.get("code"));
        journal.set("srcbillitemid", bizObject.get("id"));
        journal.set("srcbillitemno", BigDecimal.ONE);
        journal.set("org", bizObject.get("org"));
        journal.set("reconciliation", "");
        journal.set("billnum", bizObject.get("code"));
        journal.set("createTime", new Date());
        journal.set("createDate", new Date());
        journal.set("creator", AppContext.getCurrentUser().getId());
        journal.set("corp", bizObject.get("corp"));
        journal.set("financialOrg", bizObject.get("financialOrg"));
        journal.set("tenant", bizObject.get("tenant"));
        journal.set("transeqno", bizObject.get("transeqno"));
        return journal;
    }

}
