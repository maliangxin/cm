package com.yonyoucloud.fi.cmp.journalbill.rule.audit;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.ResultPager;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.model.rpcparams.project.ProjectQueryParam;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalVo;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.sdk.journal.service.CmpJournalService;
import com.yonyoucloud.fi.cmp.sdk.journal.utils.CmpJournalUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import io.edap.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JournalBillAfterAuditRule extends AbstractCommonRule {

    public static String serverUrl = AppContext.getEnvConfig("fifrontservername");

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    IProjectService projectService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CmpJournalService cmpJournalService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            JournalBill journalBillDB = MetaDaoHelper.findById(JournalBill.ENTITY_NAME, bizObject.getId(), 2);
            generateInitDataIfNotPresent(journalBillDB);
            setUpdateEntityStatus(journalBillDB);
            JournalVo journalVo = generateJournal(journalBillDB, billContext);
            cmpJournalService.insert(journalVo);
            generateSmartCheckCode(journalBillDB);
            generateVoucher(journalBillDB);
            MetaDaoHelper.update(JournalBill.ENTITY_NAME, journalBillDB);
        }
        return new RuleExecuteResult();
    }

    private void generateInitDataIfNotPresent(JournalBill journalBill) throws Exception {
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            String accountId = StringUtils.isNotEmpty(journalBillB.getBankaccount()) ? journalBillB.getBankaccount() : journalBillB.getCashaccount();
            InitData initDataByAccid = CmpJournalUtils.getInitDataByAccid(journalBill.getAccentity(), accountId, journalBillB.getCurrency());
            if (initDataByAccid == null) {
                Journal journal = new Journal();
                journal.setAccentity(journalBill.getAccentity());
                journal.setAccentityRaw(journalBill.getAccentityRaw());
                journal.setDzdate(journalBillB.getDzdate());
                journal.setDztime(journalBillB.getDztime());
                journal.setBankaccount(journalBillB.getBankaccount());
                journal.setCashaccount(journalBillB.getCashaccount());
                journal.setCurrency(journalBillB.getCurrency());
                journal.setDebitnatSum(journalBillB.getDebitnatSum());
                journal.setDebitoriSum(journalBillB.getDebitoriSum());
                journal.setCreditoriSum(journalBillB.getCreditoriSum());
                journal.setCreditnatSum(journalBillB.getCreditnatSum());
                CmpJournalUtils.createInitDataVO(journal);
            }
        }
    }

    private void setUpdateEntityStatus(JournalBill journalBillDB) {
        journalBillDB.setEntityStatus(EntityStatus.Update);
        journalBillDB.JournalBill_b().forEach(item -> item.setEntityStatus(EntityStatus.Update));
    }

    private JournalVo generateJournal(JournalBill journalBill, BillContext billContext) {
        JournalVo journalVo = new JournalVo();
        journalVo.setUniqueIdentification(EventSource.Cmpchase.getValue() + "-" + journalBill.getAccentity() + "-" + journalBill.getId());
        Map<String, EnterpriseBankAcctVO> bankIdTOBankAcctVOMap = new HashMap<>();
        List<JournalBill_b> journalBillBList = journalBill.JournalBill_b();
        try {
            List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = EnterpriseBankQueryService.findByIdList(journalBillBList.stream().map(JournalBill_b::getBankaccount).filter(ObjectUtils::isNotEmpty).distinct().collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(enterpriseBankAcctVOList)) {
                bankIdTOBankAcctVOMap = enterpriseBankAcctVOList.stream().collect(Collectors.toMap(EnterpriseBankAcctVO::getId, Function.identity()));
            }
        } catch (Exception e) {
            log.error("query enterprise bank failed, ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105047"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AA", "登账失败：获取银行信息失败") /* "登账失败：获取银行信息失败" */);/* 登账失败：获取银行信息失败 */
        }
        Map<String, String> projectIdToCodeMap = new HashMap<>();
        try {
            ProjectQueryParam projectQueryParam = new ProjectQueryParam();
            projectQueryParam.setPageIndex(5000);
            projectQueryParam.setIdList(journalBillBList.stream().map(JournalBill_b::getProjectId).filter(ObjectUtils::isNotEmpty).distinct().collect(Collectors.toList()));
            ResultPager resultPager = projectService.queryProjectListByPage(projectQueryParam, new String[]{"id", "code"});
            List<Map<String, String>> recordList = resultPager.getRecordList();
            if (!CollectionUtils.isEmpty(recordList)) {
                projectIdToCodeMap = recordList.stream().collect(Collectors.toMap(item -> item.get("id"), item -> item.get("code")));
            }
        } catch (Exception e) {
            log.error("query project info failed, ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105048"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A9", "登账失败：获取项目信息失败") /* "登账失败：获取项目信息失败" */);/* 登账失败：获取项目信息失败 */
        }
        Map<String, EnterpriseCashVO> cashAccountIdToAccountInfoMap = new HashMap<>();
        try {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setIdList(journalBillBList.stream().map(JournalBill_b::getCashaccount).filter(ObjectUtils::isNotEmpty).distinct().collect(Collectors.toList()));
            List<EnterpriseCashVO> enterpriseCashVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams);
            if (!CollectionUtils.isEmpty(enterpriseCashVOS)) {
                cashAccountIdToAccountInfoMap = enterpriseCashVOS.stream().collect(Collectors.toMap(EnterpriseCashVO::getId, Function.identity()));
            }
        } catch (Exception e) {
            log.error("query cash info failed, ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105049"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A8", "登账失败：获取现金账户信息失败") /* "登账失败：获取现金账户信息失败" */);/* 登账失败：获取现金账户信息失败 */
        }

        List<Journal> journalsList = new ArrayList<>();
        journalVo.setJournalList(journalsList);
        for (JournalBill_b journalBillB : journalBillBList) {
            Journal journal = new Journal();
            journal.setBillnum(journalBill.getCode());
            journal.setSrcbillno(journalBill.getCode());
            journal.setSrcbillitemno(journalBillB.getId().toString());
            journal.setSrcbillitemid(journalBill.getId().toString());
            journal.setServicecode(ICmpConstant.JOURNAL_BILL_SERVICE_CODE);
            journal.setTopbilltype(EventType.JournalBill);
            journal.setTopsrcitem(EventSource.Cmpchase);
            journal.setPaymentstatus(PaymentStatus.PayDone); //TODO
            journal.setAuditstatus(AuditStatus.Complete);
            journal.setSettlestatus(SettleStatus.alreadySettled);
            journal.setBankaccount(journalBillB.getBankaccount());
            journal.setBankaccountno(Optional.ofNullable(bankIdTOBankAcctVOMap.get(journalBillB.getBankaccount())).map(EnterpriseBankAcctVO::getAccount).orElse(null));
            journal.setBanktype(Optional.ofNullable(bankIdTOBankAcctVOMap.get(journalBillB.getBankaccount())).map(EnterpriseBankAcctVO::getBank).orElse(null));
            journal.setProjectCode(projectIdToCodeMap.get(journalBillB.getProjectId()));
            journal.setProject(journalBillB.getProjectId());
            journal.setCashaccount(journalBillB.getCashaccount());
            journal.setCashaccountno(Optional.ofNullable(cashAccountIdToAccountInfoMap.get(journalBillB.getCashaccount())).map(EnterpriseCashVO::getAccount).orElse(null));
            if (ObjectUtils.isNotEmpty(journalBillB.getBankaccount())) {
                journal.setParentAccentity(bankIdTOBankAcctVOMap.get(journalBillB.getBankaccount()).getOrgid());
            } else {
                journal.setParentAccentity(cashAccountIdToAccountInfoMap.get(journalBillB.getCashaccount()).getOrgid());
            }
            journal.setSrcitem(EventSource.Cmpchase);
            journal.setAuditstatus(AuditStatus.Complete);
            journal.setInitflag(false);
            journal.setTargeturl(serverUrl + "/meta/ArchiveList/" + billContext.getBillnum());
            setJournalOppositeInfo(journalBillB, journal);
            journal.setDztime(journalBillB.getDztime());
            journal.setDzdate(journalBillB.getDzdate());
            journal.setAccentity(journalBill.getAccentity());
            journal.setAccentityRaw(journalBill.getAccentityRaw());
            journal.setDescription(journalBillB.getDescription());
            if (journalBillB.getPaymenttype() == JournalBillPaymentType.DEBIT.getValue()) {
                journal.setRptype(RpType.ReceiveBill);
                journal.setDirection(Direction.Debit);
                journal.setDebitoriSum(journalBillB.getDebitoriSum());
                journal.setDebitnatSum(journalBillB.getDebitnatSum());
                journal.setCreditoriSum(BigDecimal.ZERO);
                journal.setCreditnatSum(BigDecimal.ZERO);
            } else {
                journal.setRptype(RpType.PayBill);
                journal.setDirection(Direction.Credit);
                journal.setDebitoriSum(BigDecimal.ZERO);
                journal.setDebitnatSum(BigDecimal.ZERO);
                journal.setCreditoriSum(journalBillB.getCreditoriSum());
                journal.setCreditnatSum(journalBillB.getCreditnatSum());
            }
            journal.setBilltype(EventType.JournalBill);
            journal.setBankbilldate(journalBillB.getNotedate());
            journal.setBillno(IBillNumConstant.JOURNAL_BILL);
            journal.setBankbilltype(journalBillB.getNotetype());

            journal.setSettlemode(journalBillB.getSettlemode());
            journal.setTradetype(journalBill.getTradetype());
            journal.setExchangerate(journalBillB.getExchangeRate());
            journal.setExchangerateOps(journalBillB.getExchangeRateOps());
            journal.setCostproject(Optional.ofNullable(journalBillB.getCostproject()).map(Long::valueOf).orElse(null));
            journal.setVouchdate(journalBillB.getDzdate());
            journal.setVouchcreatedate(journalBill.getCreateDate());
            journal.setVouchcreatetime(journalBill.getCreateTime());
            journal.setOrderno(journalBill.getCode()); // TODO??
            journal.setCreatorId(journalBill.getCreatorId());
            journal.setUnireconciliationcode(journalBillB.getUnireconciliationcode());
            journal.setDept(journalBill.getDept());
            journal.setCurrency(journalBillB.getCurrency());
            journal.setNatCurrency(journalBill.getNatCurrency());
            journal.setCaobject(CaObject.find(journalBillB.getOppositetype()));
            journalsList.add(journal);
        }
        return journalVo;
    }

    private void setJournalOppositeInfo(JournalBill_b journalBillB, Journal journal) {
        if (ObjectUtils.isEmpty(journalBillB.getOppositetype())) {
            return;
        }
        if (journalBillB.getOppositetype() == CaObject.Customer.getValue()) {
            journal.setCustomerbankaccount(Optional.ofNullable(journalBillB.getOppositeaccountid()).filter(StringUtils::isNotEmpty).map(Long::valueOf).orElse(null));
            journal.setCustomer(Optional.ofNullable(journalBillB.getOppositeid()).filter(StringUtils::isNotEmpty).map(Long::valueOf).orElse(null));
            journal.setCustomerbankname(journalBillB.getOppositeaccountname());
        } else if (journalBillB.getOppositetype() == CaObject.Supplier.getValue()) {
            journal.setSupplier(Optional.ofNullable(journalBillB.getOppositeid()).filter(StringUtils::isNotEmpty).map(Long::valueOf).orElse(null));
            journal.setSupplierbankaccount(Optional.ofNullable(journalBillB.getOppositeaccountid()).filter(StringUtils::isNotEmpty).map(Long::valueOf).orElse(null));
            journal.setSupplierbankname(journalBillB.getOppositeaccountname());
        } else if (journalBillB.getOppositetype() == CaObject.InnerUnit.getValue()) {
            journal.setInnerunitbankaccount(journalBillB.getOppositeaccountid());
            journal.setInnerunit(journalBillB.getOppositeid());
        } else if (journalBillB.getOppositetype() == CaObject.CapBizObj.getValue()) {
            journal.setCapBizObj(journalBillB.getOppositeid());
            journal.setCapBizObjbankaccount(journalBillB.getOppositeaccountid());
        } else if (journalBillB.getOppositetype() == CaObject.Other.getValue()) {
            journal.setOtherbankaccount(journalBillB.getOppositeaccountid());
            journal.setOtherbankaccountname(journalBillB.getOppositeaccountname());
            journal.setOtherbankaccountno(journalBillB.getOppositebankaccountno());
            journal.setOthername(journalBillB.getOppositename()); // TODO
            journal.setOthertitle(journalBillB.getOppositename());// TODO
        } else if (journalBillB.getOppositetype() == CaObject.Employee.getValue()) {
            journal.setEmployee(journalBillB.getOppositeid());
            journal.setEmployeeaccount(journalBillB.getOppositeaccountid());
        }
    }

    private void generateVoucher(JournalBill journalBillDB) throws Exception {
        journalBillDB.JournalBill_b().forEach(journalBillB -> {
            journalBillB.setVoucherstatus(VoucherStatus.POSTING.getValue());
            CtmJSONObject generateRedResult = null;
            try {
                JournalBill journalBillToVoucher = new JournalBill();
                journalBillToVoucher.set(ICmpConstant.ENTITYNAME, JournalBill.ENTITY_NAME);
                journalBillToVoucher.init(journalBillDB);
                journalBillToVoucher.put("voucherstatus", journalBillB.getVoucherstatus());
                journalBillToVoucher.setJournalBill_b(Collections.singletonList(journalBillB));
                generateRedResult = cmpVoucherService.generateRedVoucherWithResult(journalBillToVoucher, journalBillB.getId().toString());
                if (!generateRedResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102474"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00252", "单据【") /* "单据【" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00253", "】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateRedResult.get("message"));
                }
            } catch (Exception e) {
                log.error("generate voucher failed, ", e);
                journalBillB.setVoucherstatus(VoucherStatus.POST_FAIL.getValue());
                return;
            }

            if (generateRedResult.get("genVoucher") != null && !generateRedResult.getBoolean("genVoucher")) {
                journalBillB.setVoucherstatus(VoucherStatus.NONCreate.getValue());
            } else {
                journalBillB.setVoucherstatus(VoucherStatus.POSTING.getValue());
            }
        });
    }

    private void generateSmartCheckCode(JournalBill journalBill) {
        if (CollectionUtils.isEmpty(journalBill.JournalBill_b())) {
            return;
        }
        long count = journalBill.JournalBill_b().stream().map(JournalBill_b::getUnireconciliationcode).filter(org.apache.commons.lang3.StringUtils::isEmpty).count();
        String[] generatedSmartCheckCode = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).batchGenerate((int) count);
        for (int i = 0, checkCodeIndex = 0; i < journalBill.JournalBill_b().size(); i++) {
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(journalBill.JournalBill_b().get(i).getUnireconciliationcode())) {
                continue;
            }
            journalBill.JournalBill_b().get(i).setUnireconciliationcode(generatedSmartCheckCode[checkCodeIndex++]);
        }
    }

}
