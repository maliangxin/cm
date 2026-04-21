package com.yonyoucloud.fi.cmp.foreignpayment.rule.business;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.data.service.itf.OrgFuncSharingSettingApi;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.org.dto.OrgFuncSharingSettingQryParam;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.SupplierRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.bd.costcenter.CostCenter;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.*;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.dataSignature.DataSignatureUtil;
import com.yonyoucloud.fi.cmp.util.dataSignature.entity.DataSignatureEntity;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 支付保证金保存前规则*
 *
 * @author xuxbo
 * @date 2023/8/3 10:09
 */

@Slf4j
@Component
public class ForeignPaymentBeforeSaveRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    BillCopyCheckService billCopyCheckService;

    @Autowired
    private CtmSignatureService signatureService;
    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    private BillTypeQueryService billTypeQueryService;
    @Autowired
    private VendorQueryService vendorQueryService;
    @Autowired
    private SupplierRpcService supplierRpcService;
    @Autowired
    private FundBusinessObjectQueryService fundBusinessObjectQueryService;
    @Autowired
    private FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;
    @Autowired
    private SettleMethodService settleMethodService;
    @Autowired
    private CountryQueryService countryQueryService;
    @Autowired
    private TransTypeQueryService transTypeQueryService;
    @Autowired
    private CustomerQueryService customerQueryService;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    private OrgDataPermissionService orgDataPermissionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        BillDataDto bill = (BillDataDto)map.get("param");
        Map<String, Integer> checkCacheMap = new HashMap<>(256);
        String billnum = billContext.getBillnum();
        for (BizObject bizObject : bills) {
            String accentity = bizObject.get(IBussinessConstant.ACCENTITY);
            //CM202400472传结算的现金管理单据控制0金额数据不传给结算
            if (bizObject.getBigDecimal("amount").compareTo(BigDecimal.ZERO) == 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EB04E4005980008", "原币金额为0的单据无法保存！") /* "原币金额为0的单据无法保存！" */);
            }
            //交易类型停用校验
            CmpCommonUtil.checkTradeTypeEnable(bizObject.get("tradetype"));
            //复制功能
            if ("copy".equals(bizObject.get("actionType"))) {
                checkCopy(bizObject, checkCacheMap, billnum, accentity);
            } else {
                ForeignPayment foreignPayment = (ForeignPayment) bizObject;
                //OpenApi过来的数据赋值外部系统
                boolean openApiFlag = bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true);
                boolean fromApi = bill.getFromApi();
                //导入过来的
                boolean importFlag =  "import".equals(bill.getRequestAction());
                if (openApiFlag || importFlag || fromApi) {
                    checkOpenApi(foreignPayment);
                }
                if(importFlag){
                    foreignPayment.setSrcitem(EventSource.Cmpchase.getValue());
                }
                //校验收、付款方国家地区  当“是否跨境=是”时，保存时校验，收款方常驻国家地区≠付款方国家地区；当“是否跨境=否”时，保存时校验，收款方国家=付款方国家
                String paycountry = foreignPayment.getPaycountry();
                String receivecountry = foreignPayment.getReceivecountry();
                if (ObjectUtils.isNotEmpty(paycountry) && ObjectUtils.isNotEmpty(receivecountry)) {
                    if (foreignPayment.getIscrossborder() == 1) {
                        if (paycountry.equals(receivecountry)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100613"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1AA6B12605600006", "付款方与收款方常用国家地区一致，“是否跨境”字段应该为否，请重新核对！") /* "付款方与收款方常用国家地区一致，“是否跨境”字段应该为否，请重新核对！*/);
                        }
                    } else {
                        if (!paycountry.equals(receivecountry)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100614"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1AA6B15405600001", "付款方与收款方常用国家地区不一致，“是否跨境”字段应该为是，请重新核对！") /* "付款方与收款方常用国家地区不一致，“是否跨境”字段应该为是，请重新核对！*/);
                        }
                    }
                }

                //校验交易金额A和B之和是否等于原币金额
                BigDecimal transactionamountB = foreignPayment.getTransactionamountB();
                BigDecimal transactionamountA = foreignPayment.getTransactionamountA();
                BigDecimal amount = foreignPayment.getAmount();
                if (ObjectUtils.isNotEmpty(transactionamountB)) {
                    BigDecimal sumamount = transactionamountA.add(transactionamountB);
                    if (amount.compareTo(sumamount) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100615"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1AA6B1B205600009", "交易金额与原币金额不相等，请重新核对！") /* "交易金额与原币金额不相等，请重新核对！*/);
                    }
                } else {
                    if (amount.compareTo(transactionamountA) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100615"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1AA6B1B205600009", "交易金额与原币金额不相等，请重新核对！") /* "交易金额与原币金额不相等，请重新核对！*/);
                    }
                }

                //对方类型为其他的时候  保存的时候需要赋值 收款方信息
                Short receivetype = foreignPayment.getReceivetype();
                if (ObjectUtils.isNotEmpty(receivetype) && receivetype == CaObject.Other.getValue()) {
                    String othername = foreignPayment.getOthername();
                    String otherbankaccount = foreignPayment.getOtherbankaccount();
                    String otherbankaccountname = foreignPayment.getOtherbankaccountname();
                    foreignPayment.setReceivename(othername);
                    foreignPayment.setReceivebankaccount(otherbankaccount);
                    foreignPayment.setReceivebankaccountname(otherbankaccountname);
                }
                if (ObjectUtils.isEmpty(foreignPayment.getVoucherstatus())) {
                    foreignPayment.setVoucherstatus(VoucherStatus.Empty.getValue());
                }

                if (ObjectUtils.isEmpty(foreignPayment.getVerifystate())) {
                    foreignPayment.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
                }
                //添加签名
                DataSignatureEntity dataSignatureEntity = DataSignatureEntity.builder().opoppositeObjectName(foreignPayment.getReceivename()).
                        oppositeAccountName(foreignPayment.getReceivebankaccountname()).tradeAmount(foreignPayment.getAmount()).build();
                foreignPayment.setSignature(DataSignatureUtil.signMsg(dataSignatureEntity));
                //保存提交后重新匹配预算规则，原预算规则预占删除，新规则占预占
                if (foreignPayment.getVerifystate() != null && foreignPayment.getVerifystate() == VerifyState.SUBMITED.getValue()) {
                    Short occupyBudget = reMatchBudget(foreignPayment);
                    if (occupyBudget != null) {
                        foreignPayment.setIsOccupyBudget(occupyBudget);
                    }
                }
            }
        }

        checkCacheMap.clear();
        return new RuleExecuteResult();
    }


    public Short reMatchBudget(ForeignPayment foreignPayment) throws Exception {
        //1.判断是否是预占成功，如果是删除旧的预占，新增新的预占
        //2.判断是否是预占成功，如果否直接进行新的预占
        ForeignPayment oldBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, foreignPayment.getId(), 2);
        Short budgeted = oldBill.getIsOccupyBudget();
        if (budgeted == null || budgeted == OccupyBudget.UnOccupy.getValue()) {
            log.error("ReMatchBudget ，占用预算");
            boolean budget = cmpBudgetForeignpaymentManagerService.budget(foreignPayment);
            if (budget){
                return OccupyBudget.PreSuccess.getValue();
            }
        } else if (budgeted == OccupyBudget.PreSuccess.getValue()) {
            boolean releaseBudget = cmpBudgetForeignpaymentManagerService.releaseBudget(oldBill);
            if (releaseBudget) {
                log.error("ReMatchBudget ，删除预算成功");
                foreignPayment.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetForeignpaymentManagerService.budget(foreignPayment);
                if (budget) {
                    log.error("ReMatchBudget ，重新占用预算成功");
                    return OccupyBudget.PreSuccess.getValue();
                } else {
                    log.error("ReMatchBudget ，重新占用预算失败");
                    return OccupyBudget.UnOccupy.getValue();
                }
            } else {
                log.error("ReMatchBudget ，删除预算失败");
            }
        }
        return null;
    }

    /**
     * 整单复制功能跳转过来的保存请求，做字段启用性校验
     * *
     *
     * @param bizObject
     * @throws Exception
     */
    private void checkCopy(BizObject bizObject, Map<String, Integer> checkCacheMap, String billnum, String accentity) throws Exception {
        log.error("ForeignPaymentBeforeSaveRule, id = {}, code = {}, BizObject = {}", bizObject.getId(), bizObject.get("code"), bizObject);
        if (ValueUtils.isNotEmptyObj(bizObject.get(IBussinessConstant.ACCENTITY)) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("accentity_" + bizObject.get(IBussinessConstant.ACCENTITY)))) {
            billCopyCheckService.checkAccentity(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("org")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("org_" + bizObject.get("org")))) {
            billCopyCheckService.checkOrg(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("dept")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("dept_" + bizObject.get("dept")))) {
            billCopyCheckService.checkDept(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("project")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("project_" + bizObject.get("project")))) {
            billCopyCheckService.checkProject(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("expenseitem")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("expenseitem_" + bizObject.get("expenseitem")))) {
            billCopyCheckService.checkExpenseitem(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("currency")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("currency_" + bizObject.get("currency")))) {
            billCopyCheckService.checkCurrency(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("tradetype")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("tradetype_" + bizObject.get("tradetype")))) {
            billCopyCheckService.checkTradetype(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("settlemode")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("settlemode_" + bizObject.get("settlemode")))) {
            billCopyCheckService.checkSettlemode(bizObject, checkCacheMap);
        }

        //折本币汇率类型-必输
        if (bizObject.get("currencyexchangeratetype") != null) {
            ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateTypeById(bizObject.getString("currencyexchangeratetype"));
            if (exchangeRateTypeVO != null) {
                String exchangeRateTypeFlag = exchangeRateTypeVO.getEnable();
                if (exchangeRateTypeFlag != null && "2".equals(exchangeRateTypeFlag)) {//1是启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100496"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046C", "汇率类型未启用，保存失败！") /* "汇率类型未启用，保存失败！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100497"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046D", "未查询到对应的汇率类型，保存失败！") /* "未查询到对应的汇率类型，保存失败！" */);
            }
        }

        if (ValueUtils.isNotEmptyObj(bizObject.get("paymenterprisebankaccount")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("paymenterprisebankaccount_" + bizObject.get("paymenterprisebankaccount")))) {
            billCopyCheckService.checkBankaccount(bizObject, checkCacheMap);
        }

        //收款客户-非必输
        if (bizObject.get(IBussinessConstant.CUSTOMER) != null && ValueUtils.isNotEmptyObj(checkCacheMap.get("customer_" + bizObject.get(IBussinessConstant.CUSTOMER)))) {
            billCopyCheckService.checkCustomerByid(bizObject.getString(IBussinessConstant.CUSTOMER), bizObject.getString(IBussinessConstant.ACCENTITY), checkCacheMap);
        }
        if (bizObject.get("customerbankaccount") != null && ValueUtils.isNotEmptyObj(checkCacheMap.get("customerbankaccount_" + bizObject.get("customerbankaccount")))) {
            billCopyCheckService.checkCustomerbankaccountById(bizObject.get("customerbankaccount"), checkCacheMap);
        }

        //供应商-非必输
        if (ValueUtils.isNotEmptyObj(bizObject.get("supplier")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("supplier_" + bizObject.get("supplier")))) {
            billCopyCheckService.checkSupplier(Long.valueOf(bizObject.get("supplier")), accentity, checkCacheMap);
        }
        Map conditon = new HashMap<>();
        conditon.put("id", bizObject.get("supplierbankaccount"));
        if (ValueUtils.isNotEmptyObj(bizObject.get("supplierbankaccount")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("supplierbankaccount_" + (bizObject.get("supplierbankaccount"))))) {
            billCopyCheckService.checkSupplierbankaccountById(conditon, bizObject.get("supplierbankaccount"), checkCacheMap);
        }

        //部门-非必输
        if (ValueUtils.isNotEmptyObj(bizObject.get("dept")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("dept_" + bizObject.get("dept")))) {
            billCopyCheckService.checkDept(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("expenseitem")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("expenseitem_" + bizObject.get("expenseitem")))) {
            billCopyCheckService.checkExpenseitem(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("project")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("project_" + bizObject.get("project")))) {
            billCopyCheckService.checkProject(bizObject, checkCacheMap);
        }
        //校验银行类别
        //银行类别id
        String oppositebankTypeId = bizObject.get("receivebanktype");
        //根据类别id查询 银行类别name
        if (ObjectUtils.isNotEmpty(oppositebankTypeId)) {
            BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(oppositebankTypeId);
            if (ObjectUtils.isEmpty(bankVO)) {
                // 对方银行类别
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100616"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D8B9FBE05080000", "未查询到启用的收款方银行类别，保存失败！") /* "未查询到启用的收款方银行类别，保存失败！" */);
            }
        }

        //对于复制的单据，清空一些业务字段  结算状态 单据状态 凭证状态 结算成功时间 结算成功金额  实际结算汇率类型 实际结算汇率 实际结算金额 结算止付金额

        bizObject.put("settlestatus", FundSettleStatus.WaitSettle.getValue());
        bizObject.put("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
        bizObject.put("voucherstatus", VoucherStatus.Empty.getValue());
        bizObject.put("settlesuccesstime", null);
        bizObject.put("settlesuccessSum", null);
        bizObject.put("settleExchangeRateType", null);
        bizObject.put("settleExchangeRate", null);
        bizObject.put("settleAmount", null);
        ForeignPayment foreignPayment = (ForeignPayment) bizObject;
        foreignPayment.setSrcitem(EventSource.Cmpchase.getValue());
        //对方类型为其他的时候  保存的时候需要赋值 收款方信息
        Short receivetype = foreignPayment.getReceivetype();
        if (ObjectUtils.isNotEmpty(receivetype) && receivetype == CaObject.Other.getValue()) {
            String othername = foreignPayment.getOthername();
            String otherbankaccount = foreignPayment.getOtherbankaccount();
            String otherbankaccountname = foreignPayment.getOtherbankaccountname();
            foreignPayment.setReceivename(othername);
            foreignPayment.setReceivebankaccount(otherbankaccount);
            foreignPayment.setReceivebankaccountname(otherbankaccountname);
        }
        if (ObjectUtils.isEmpty(foreignPayment.getVoucherstatus())) {
            foreignPayment.setVoucherstatus(VoucherStatus.Empty.getValue());
        }

        if (ObjectUtils.isEmpty(foreignPayment.getVerifystate())) {
            foreignPayment.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        }

        //添加签名
        DataSignatureEntity dataSignatureEntity = DataSignatureEntity.builder().opoppositeObjectName(foreignPayment.getReceivename()).
                oppositeAccountName(foreignPayment.getReceivebankaccountname()).tradeAmount(foreignPayment.getAmount()).build();
        foreignPayment.setSignature(DataSignatureUtil.signMsg(dataSignatureEntity));
    }

    private void checkOpenApi(ForeignPayment foreignPayment) throws Exception {

        Short settlestatus = foreignPayment.getSettlestatus();
        if (settlestatus == null
                || (settlestatus != FundSettleStatus.WaitSettle.getValue()
                && settlestatus != FundSettleStatus.SettlementSupplement.getValue())
        ) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102079"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180809", "结算状态不可为待结算或已结算补单外其他值") /* "结算状态不可为待结算或已结算补单外其他值" */);
        }
        //receivetype 收款方类型
        if (foreignPayment.getReceivetype() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B915604B00003", "收款方类型必填！"));
        }
        if (StringUtils.isEmpty(foreignPayment.get("transactioncodeA_code")) && StringUtils.isEmpty(foreignPayment.get("transactioncodeA_trade_code"))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0007B", "交易编码A必填且不能为空！") /* "交易编码A必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getTradepostscriptA())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0004E", "交易附言A必填且不能为空！") /* "交易附言A必填且不能为空！" */);
        }
        if (foreignPayment.getTransactionamountA() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0004F", "交易金额A必填且不能为空！") /* "交易金额A必填且不能为空！" */);
        }
        if (foreignPayment.getReceivetype() != CaObject.Other.getValue() && StringUtils.isEmpty(foreignPayment.getReceivebankaccountid())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00052", "收款方账号必填且不能为空！") /* "收款方账号必填且不能为空！" */);
        }
        if (foreignPayment.getReceivetype() != CaObject.Other.getValue() && StringUtils.isEmpty(foreignPayment.getReceivenameid())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00054", "收款方名称必填且不能为空！") /* "收款方名称必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getAccountcurrency()) && StringUtils.isEmpty(foreignPayment.getAccountcurrency_name())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00056", "账户币必填且不能为空！") /* "账户币必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getCurrency())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00059", "原币币种必填且不能为空！") /* "原币币种必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getPaycountry())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005B", "付款方常驻国家地区必填且不能为空！") /* "付款方常驻国家地区必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getPostscript())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005D", "汇款附言必填且不能为空！") /* "汇款附言必填且不能为空！" */);
        }
        if (foreignPayment.getIscrossborder() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00060", "是否跨境必填且不能为空！") /* "是否跨境必填且不能为空！" */);
        }
        if (foreignPayment.getVouchdate() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00061", "单据日期必填且不能为空！") /* "单据日期必填且不能为空！" */);
        }
        if (foreignPayment.getForeignpaymenttype() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00062", "外汇支付方式必填且不能为空！") /* "外汇支付方式必填且不能为空！" */);
        }
        if (foreignPayment.getAmount() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00063", "原币金额必填且不能为空！") /* "原币金额必填且不能为空！" */);
        }

        //校验金额不能小于0
        if (foreignPayment.getAmount() != null && foreignPayment.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2CAB4A04B80003", "原币金额不能小于0！") /* "原币金额不能小于0！" */);
        }
        if (foreignPayment.getTransactionamountA() != null && foreignPayment.getTransactionamountA().compareTo(BigDecimal.ZERO) < 0) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2CAB8404B80000", "交易金额A不能小于0！") /* "交易金额A不能小于0！" */);
        }
        if (foreignPayment.getTransactionamountB() != null && foreignPayment.getTransactionamountB().compareTo(BigDecimal.ZERO) < 0) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2CABEA05500005", "交易金额B不能小于0！") /* "交易金额B不能小于0！" */);
        }

//        if (foreignPayment.getBankflag() == null) {
//            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00064", "是否跨行必填且不能为空！") /* "是否跨行必填且不能为空！" */);
//        }
        if (foreignPayment.getIsagencybank() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00065", "是否通过代理行必填且不能为空！") /* "是否通过代理行必填且不能为空！" */);
        }
        if (foreignPayment.getPublicorprivate() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00066", "对公/对私必填且不能为空！") /* "对公/对私必填且不能为空！" */);
        }
        if (foreignPayment.getReceivetype() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00067", "收款方类型必填且不能为空！") /* "收款方类型必填且不能为空！" */);
        }
        if (foreignPayment.getEntityStatus() == null || (foreignPayment.getEntityStatus() != EntityStatus.Insert && foreignPayment.getEntityStatus() != EntityStatus.Update)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00068", "_status必填且只能是Insert或者Update！") /* "_status必填且只能是Insert或者Update！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getAccentity())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00069", "资金组织必填且不能为空！") /* "资金组织必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.get("settlemode_code"))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0006A", "结算方式必填且不能为空！") /* "结算方式必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.getPaymenterprisebankaccount())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0006D", "付款方银行账户必填且不能为空！") /* "付款方银行账户必填且不能为空！" */);
        }
        if (StringUtils.isEmpty(foreignPayment.get("tradetype_code"))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0006E", "交易类型编码必填且不能为空！") /* "交易类型编码必填且不能为空！" */);
        }
        //receivecountry
        if (StringUtils.isEmpty(foreignPayment.getReceivecountry())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00070", "收款方常驻国家地区编码必填且不能为空！") /* "收款方常驻国家地区编码必填且不能为空！" */);
        }
        //transactioncodeA
        if (!StringUtils.isEmpty(foreignPayment.get("transactioncodeA_code")) || !StringUtils.isEmpty(foreignPayment.get("transactioncodeA_trade_code"))) {
            String transactioncodeA_trade_code = StringUtils.isEmpty(foreignPayment.get("transactioncodeA_code")) ? foreignPayment.get("transactioncodeA_trade_code") : foreignPayment.get("transactioncodeA_code");
            List<BizObject> transactioncodeA_code = QueryBaseDocUtils.getTradeCodeByIdOrCode(transactioncodeA_trade_code);
            if (CollectionUtils.isEmpty(transactioncodeA_code)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00071", "交易编码A未匹配到！请检查！") /* "交易编码A未匹配到！请检查！" */);
            } else {
                foreignPayment.setTransactioncodeA(transactioncodeA_code.get(0).getId());
            }
        }
        //transactioncodeB
        if (!StringUtils.isEmpty(foreignPayment.get("transactioncodeB_code")) || !StringUtils.isEmpty(foreignPayment.get("transactioncodeB_trade_code"))) {
            String transactioncodeB_trade_code = StringUtils.isEmpty(foreignPayment.get("transactioncodeB_code")) ? foreignPayment.get("transactioncodeB_trade_code") : foreignPayment.get("transactioncodeB_code");
            List<BizObject> transactioncodeB_code = QueryBaseDocUtils.getTradeCodeByIdOrCode(transactioncodeB_trade_code);
            if (CollectionUtils.isEmpty(transactioncodeB_code)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00073", "交易编码B未匹配到！请检查！") /* "交易编码B未匹配到！请检查！" */);
            } else {
                foreignPayment.setTransactioncodeB(transactioncodeB_code.get(0).getId());
            }
        }
        //是否通过代理行为是时，需校验相关字段必填 收款方开户行在其代理行账号、代理行名称、代理行地址、代理行SWIFT
        if (foreignPayment.getIsagencybank() != null && foreignPayment.getIsagencybank() == 1) {
            if (org.apache.commons.lang3.StringUtils.isAnyEmpty(foreignPayment.getAgencybankaccount(), foreignPayment.getAgencybankname(), foreignPayment.getAgencybankaddress(), foreignPayment.getAgencybankswift())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00075", "是否通过代理行为是时,收款方开户行在其代理行账号、代理行名称、代理行地址、代理行SWIFT为必填") /* "是否通过代理行为是时,收款方开户行在其代理行账号、代理行名称、代理行地址、代理行SWIFT为必填" */);
            }
        }
        //accentity
        if (!StringUtils.isEmpty(foreignPayment.getAccentity())) {
            String accentityCode = null;
            List<Map<String, Object>> orgMVByIdOrCode = QueryBaseDocUtils.getOrgMVByIdOrCode(foreignPayment.getAccentity());
            if (CollectionUtils.isEmpty(orgMVByIdOrCode)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005E", "资金组织未匹配到！请检查！") /* "资金组织未匹配到！请检查！" */);
            } else {
                foreignPayment.setAccentity(orgMVByIdOrCode.get(0).get("id").toString());
                if (foreignPayment.getOrg() == null) {
                    foreignPayment.setOrg(orgMVByIdOrCode.get(0).get("id").toString());
                    accentityCode = orgMVByIdOrCode.get(0).get("code").toString();
                } else {
                    accentityCode =  foreignPayment.getString("org_code");
                }

            }

            Set<String> orgOwn = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.FOREIGNPAYMENT);
            if (orgOwn == null || orgOwn.isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102353"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54D39604300021", "请为此用户分配主组织权限！") /* "请为此用户分配主组织权限！" */);
            }

            if (!orgOwn.contains(foreignPayment.getOrg())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102354"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E6", "当前用户无组织【%s】的权限，请检查！") /* "当前用户无组织【%s】的权限，请检查！" */, accentityCode));
            }
        }
        //settlemode
        List<Map<String, Object>> settlemodeList = settleMethodService.getSettleMethodByBankTransSettlemode(foreignPayment.get("settlemode_code").toString());
        if (CollectionUtils.isEmpty(settlemodeList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100617"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B8FF204B00001", "结算方式必填且必须在结算方式档案中存在！"));
        } else {
            foreignPayment.setSettlemode(Long.valueOf(settlemodeList.get(0).get("id").toString()));
        }
        //paymenterprisebankaccount 付款方银行账户
        if (!StringUtils.isEmpty(foreignPayment.getPaymenterprisebankaccount())) {
            EnterpriseBankAcctVO bankAcctVo = getBankAcctVo(foreignPayment.getPaymenterprisebankaccount());
            if (bankAcctVo == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00077", "未匹配到付款方银行账户，请检查！") /* "未匹配到付款方银行账户，请检查！" */);
            } else {
                foreignPayment.setPaymenterprisebankaccount(bankAcctVo.getId());
            }
            //如果导入的结果为  是或者不导入  那么判断结算方式，结算方式为直联 那么是否直联为 是  如果结算方式为非直连  那么是否直联为否
            if (foreignPayment.getIsdirectlyconnected() == null || foreignPayment.getIsdirectlyconnected() == 1) {
                if (settleMethodService.checkSettleMethod(foreignPayment.getSettlemode().toString())) {
                    foreignPayment.setIsdirectlyconnected((short) 1);
                } else {
                    foreignPayment.setIsdirectlyconnected((short) 0);
                }
            } else { //如果导入的结果为 否  那么直接为否
                foreignPayment.setIsdirectlyconnected((short) 0);
            }

            if (settleMethodService.checkSettleMethod(foreignPayment.getSettlemode().toString())) {

                //直联
                List<String> bankAccountList = bankAccountSettingService.queryBankAccountSettingByFlag();
                if (!bankAccountList.contains(bankAcctVo.getId())) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0004D", "结算方式与付款方银行账户不匹配！") /* "结算方式与付款方银行账户不匹配！" */);
                }
            } else {
                //非直联
                if (!bankAcctVo.getOrgid().equals(foreignPayment.getAccentity())) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0004D", "结算方式与付款方银行账户不匹配！") /* "结算方式与付款方银行账户不匹配！" */);
                }
            }
        }
        BdTransType tradetype = queryTransType(foreignPayment.get("tradetype_code").toString());
        if (tradetype == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00051", "交易类型编码不存在，请检查！") /* "交易类型编码不存在，请检查！" */);
        } else {
            boolean enable = !Objects.isNull(tradetype.getEnable()) && 1 == tradetype.getEnable();
            if (!enable) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E026D3005280000", "该交易类型已停用，请重新选择！") /* "该交易类型已停用，请重新选择！" */);
            }
            foreignPayment.setTradetype(tradetype.getId());
        }
        if (StringUtils.isEmpty(foreignPayment.getCurrency())){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00058", "原币币种必填，请检查！") /* "原币币种必填，请检查！" */);
        }

        //accentityRaw
        if (!StringUtils.isEmpty(foreignPayment.getAccentityRaw())) {
            List<Map<String, Object>> orgMVByIdOrCode = QueryBaseDocUtils.getOrgMVByIdOrCode(foreignPayment.getAccentityRaw());
            if (CollectionUtils.isEmpty(orgMVByIdOrCode)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005E", "资金组织未匹配到！请检查！") /* "资金组织未匹配到！请检查！" */);
            } else {
                foreignPayment.setAccentityRaw(orgMVByIdOrCode.get(0).get("id").toString());
            }
        }
        //receivecountry
        if (!StringUtils.isEmpty(foreignPayment.getReceivecountry())) {
            BdCountryVO bdCountryVO = countryQueryService.findById(foreignPayment.getReceivecountry());
            if (bdCountryVO == null) {
                bdCountryVO = countryQueryService.findByCode(foreignPayment.getReceivecountry());
            }
            if (bdCountryVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100618"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B901E04B00002", "收款方常驻国家地区编码错误！请检查！"));
            } else {
                foreignPayment.setReceivecountry(bdCountryVO.getId());
            }
        }
        //paycountry
        if (!StringUtils.isEmpty(foreignPayment.getPaycountry())) {
            BdCountryVO bdCountryVO = countryQueryService.findById(foreignPayment.getPaycountry());
            if (bdCountryVO == null) {
                bdCountryVO = countryQueryService.findByCode(foreignPayment.getPaycountry());
            }
            if (bdCountryVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100618"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B901E04B00002", "付款方常驻国家地区编码错误！请检查！"));
            } else {
                foreignPayment.setPaycountry(bdCountryVO.getId());
            }
        }

        //部门和项目权限校验
        //部门
        if (!StringUtils.isEmpty(foreignPayment.getDept())) {
//            CmpCommonUtil.checkdept(foreignPayment.getString("dept"), foreignPayment.get(IBussinessConstant.ACCENTITY));
        }
        // 校验业务组织权限
        CmpCommonUtil.checkAuthBusOrg(foreignPayment.getString("org"),foreignPayment.get(IBussinessConstant.ACCENTITY));


        //project
        if (!StringUtils.isEmpty(foreignPayment.getProject())) {
            List<Map<String, Object>> projectByIdOrCode = QueryBaseDocUtils.queryProjectByIdOrCode(foreignPayment.getProject());
            if (CollectionUtils.isEmpty(projectByIdOrCode)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0006B", "项目未匹配到，请检查！") /* "项目未匹配到，请检查！" */);
            } else {
                foreignPayment.setProject(projectByIdOrCode.get(0).get("id").toString());
            }
            //项目
            if(!CmpCommonUtil.checkProject(foreignPayment.getString("project"),foreignPayment.get(IBussinessConstant.ACCENTITY))){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D9","导入的项目所属使用组织与导入资金组织不一致！") /* "导入的项目所属使用组织与导入资金组织不一致！" */);
            }
        }
        //expenseitem
        if (!StringUtils.isEmpty(foreignPayment.getString("expenseitem_code"))) {
            List<Map<String, Object>> expenseItemByIdOrCode = QueryBaseDocUtils.queryExpenseItemByIdOrCode(foreignPayment.getString("expenseitem_code"));
            if (CollectionUtils.isEmpty(expenseItemByIdOrCode)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0006F", "费用项目未匹配到，请检查！") /* "费用项目未匹配到，请检查！" */);
            } else {
                foreignPayment.setExpenseitem(Long.valueOf(expenseItemByIdOrCode.get(0).get("id").toString()));
            }
            //校验费用项目权限
            if(foreignPayment.getExpenseitem() != null && !CmpCommonUtil.checkExpenseitem(foreignPayment.getExpenseitem().toString(), foreignPayment.get(IBussinessConstant.ACCENTITY))){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EA3864005680008","所选费用项目无权限或存在未启用财资业务领域的情况，保存失败！ ") /* "所选费用项目无权限或存在未启用财资业务领域的情况，保存失败！ " */);
            }
        }
        //costcenter
        if (!StringUtils.isEmpty(foreignPayment.getString("costcenter_code"))) {
            List<CostCenter> costcenter_code = QueryBaseDocUtils.getCostCenterByIdOrCode(foreignPayment.getString("costcenter_code"));
            if (CollectionUtils.isEmpty(costcenter_code)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00072", "成本中心未匹配到，请检查！") /* "成本中心未匹配到，请检查！" */);
            } else {
                foreignPayment.setCostcenter(Long.valueOf(costcenter_code.get(0).getId()));
            }
        }
        //quickType
        if (!StringUtils.isEmpty(foreignPayment.getString("quickType_code"))) {
            List<Map<String, Object>> quickType_code = QueryBaseDocUtils.getQuickTypeByIdOrCode(foreignPayment.getString("quickType_code"));
            if (CollectionUtils.isEmpty(quickType_code)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00074", "款项类型未匹配到，请检查！") /* "款项类型未匹配到，请检查！" */);
            } else {
                foreignPayment.setCostcenter(Long.valueOf(quickType_code.get(0).get("id").toString()));
            }
        }
        //profitcenter
        if (!StringUtils.isEmpty(foreignPayment.getProfitcenter())) {
            List<Map<String, Object>> profitcenterByIdOrCode = QueryBaseDocUtils.getProfitcenterByIdOrCode(foreignPayment.getProfitcenter());
            if (CollectionUtils.isEmpty(profitcenterByIdOrCode)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00076", "利润中心未匹配到，请检查！") /* "利润中心未匹配到，请检查！" */);
            } else {
                foreignPayment.setProfitcenter(profitcenterByIdOrCode.get(0).get("id").toString());
            }
        }
        //currency  原币币种
        if (!StringUtils.isEmpty(foreignPayment.getCurrency())) {
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(foreignPayment.getCurrency());
            if (currencyTenantDTO == null) {
                currencyTenantDTO = currencyQueryService.findByCode(foreignPayment.getCurrency());
            }
            if (currencyTenantDTO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100619"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B904A04B00000", "原币币种不正确，请检查！"));
            } else {
                foreignPayment.setCurrency(currencyTenantDTO.getId());
                foreignPayment.setTransactioncurrencyA(currencyTenantDTO.getId());
                foreignPayment.setTransactioncurrencyB(currencyTenantDTO.getId());
            }
        }
        //accountcurrency  账户币种
        if (!StringUtils.isEmpty(foreignPayment.getAccountcurrency()) || !StringUtils.isEmpty(foreignPayment.getAccountcurrency_name())) {
            String  accountcurrency = foreignPayment.getAccountcurrency();
            if (StringUtils.isEmpty(accountcurrency)) {
                accountcurrency = foreignPayment.getAccountcurrency_name();
            }

            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(accountcurrency);
            if (currencyTenantDTO == null) {
                currencyTenantDTO = currencyQueryService.findByCode(accountcurrency);
            }
            if (currencyTenantDTO == null){
                currencyTenantDTO = currencyQueryService.findByName(accountcurrency);
            }
            if (currencyTenantDTO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100619"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F2BB9F604C00003", "账户币种不正确，请检查！"));
            } else {
                foreignPayment.setAccountcurrency(currencyTenantDTO.getId());
                foreignPayment.setAccountcurrency_name(currencyTenantDTO.getName());
            }
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0004C", "账户币种不可为空") /* "账户币种不可为空" */);
        }
        //receivebanktype 收款方银行类别
        if (!StringUtils.isEmpty(foreignPayment.getReceivebanktype()) || !StringUtils.isEmpty(foreignPayment.get("receivebanktype_name"))) {
            if (StringUtils.isEmpty(foreignPayment.getReceivebanktype())) {
                foreignPayment.setReceivebanktype(foreignPayment.get("receivebanktype_name"));
            }
            BankVO bankVO = baseRefRpcService.queryBankTypeById(foreignPayment.getReceivebanktype());
            if (bankVO == null) {
                bankVO = baseRefRpcService.queryBankTypeByCode(foreignPayment.getReceivebanktype());
            }
            if (bankVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100633"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B92E204900008", "未匹配到收款方银行类别，请检查！"));
            } else {
                foreignPayment.setReceivebanktype(bankVO.getId());
            }
        }
        //paymentaccount 费用支付账号
        if (!StringUtils.isEmpty(foreignPayment.getPaymentaccount())) {
            EnterpriseBankAcctVO bankAcctVo = getBankAcctVo(foreignPayment.getPaymentaccount());
            if (bankAcctVo == null || !foreignPayment.getAccentity().equals(bankAcctVo.getOrgid())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005C", "未匹配到费用支付账号，请检查！") /* "未匹配到费用支付账号，请检查！" */);
            } else {
                foreignPayment.setPaymentaccount(bankAcctVo.getId());
            }
        }
        //foreignpaymentaccount  费用支付账号(外币)
        if (!StringUtils.isEmpty(foreignPayment.getForeignpaymentaccount())) {
            EnterpriseBankAcctVO bankAcctVo = getBankAcctVo(foreignPayment.getForeignpaymentaccount());
            if (bankAcctVo == null || !foreignPayment.getAccentity().equals(bankAcctVo.getOrgid())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005C", "未匹配到费用支付账号，请检查！") /* "未匹配到费用支付账号，请检查！" */);
            } else {
                foreignPayment.setForeignpaymentaccount(bankAcctVo.getId());
            }
        }
        foreignPayment.setSrcitem(EventSource.ExternalSystem.getValue());
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(foreignPayment.getAccentity());
        //CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
        foreignPayment.setNatCurrency(finOrgDTO.getCurrency());
        BdBillType billType = billTypeQueryService.queryBillTypeId(ICmpConstant.CM_CMP_FOREIGNPAYMENT);
        foreignPayment.setBilltype(billType.getId());
        //'01' 基准汇率
        ExchangeRateTypeVO exchangeRate01 = baseRefRpcService.queryExchangeRateRateTypeByCode("01");
        if (StringUtils.isEmpty(foreignPayment.getCurrencyexchangeratetype())) {
            foreignPayment.setCurrencyexchangeratetype(exchangeRate01.getId());
        }
        if (StringUtils.isEmpty(foreignPayment.getSwapOutExchangeRateType())) {
            foreignPayment.setSwapOutExchangeRateType(exchangeRate01.getId());
        }
        if (foreignPayment.getCurrency().equals(foreignPayment.getNatCurrency())) {
            foreignPayment.setCurrencyexchangeratetype(exchangeRate01.getId());
        }
        if (foreignPayment.getCurrency().equals(foreignPayment.getAccountcurrency())) {
            BigDecimal bigDecimal = new BigDecimal(Double.toString(1));
            foreignPayment.setSwapOutExchangeRateEstimate(bigDecimal);
            foreignPayment.setSwapOutExchangeRateEstimateOps((short) 1);
        }
        List<Map<String, Object>> exchangeRateTypeByIdOrCode = QueryBaseDocUtils.queryExchangeRateTypeByIdOrCode(foreignPayment.getSwapOutExchangeRateType());
        if (CollectionUtils.isEmpty(exchangeRateTypeByIdOrCode)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0006C", "换出汇率类型没有查询到，请重新核对！") /* "换出汇率类型没有查询到，请重新核对！" */);
        } else {
            foreignPayment.setSwapOutExchangeRateType(exchangeRateTypeByIdOrCode.get(0).get("id").toString());
        }
        //原币币种与账户币种相同时，换出汇率类型应按资金组织默认汇率类型走
        if (foreignPayment.getCurrency().equals(foreignPayment.getAccountcurrency())) {
            //根据资金组织accentity查询默认汇率类型
            ExchangeRateTypeVO exchangeRateTypeVOByFundsOrg = CmpExchangeRateUtils.getNewExchangeRateType(foreignPayment.getAccentity(), false);
            if (StringUtils.isEmpty(exchangeRateTypeVOByFundsOrg.getId())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100356"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180657","此会计主体下无默认汇率类型，请检查数据！") /* "此会计主体下无默认汇率类型，请检查数据！" */);
            }
            foreignPayment.setSwapOutExchangeRateType(exchangeRateTypeVOByFundsOrg.getId());
        }

        List<Map<String, Object>> exchangeRateTypeVO = QueryBaseDocUtils.queryExchangeRateTypeByIdOrCode(foreignPayment.getCurrencyexchangeratetype());
        if (CollectionUtils.isEmpty(exchangeRateTypeVO)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100620"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B90B604900004", "折本币汇率类型没有查询到，请重新核对！"));
        } else {
            foreignPayment.setCurrencyexchangeratetype(exchangeRateTypeVO.get(0).get("id").toString());
        }
        //如果Currencyexchangeratetype ！= 01 则需要校验，币种是否一样，一样则= 01 ，不一样则校验Currencyexchangeratetype是否合法
        //02 自定义
        if (!"02".equals(exchangeRateTypeVO.get(0).get("code").toString())) {
            if (foreignPayment.getCurrency().equals(foreignPayment.getNatCurrency())) {
                BigDecimal bigDecimal = new BigDecimal(Double.toString(1));
                foreignPayment.setCurrencyexchRate(bigDecimal);
                foreignPayment.setCurrencyexchRateOps((short) 1);
            } else {
                CmpExchangeRateVO exchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(foreignPayment.getCurrency(), foreignPayment.getNatCurrency(), foreignPayment.getVouchdate(), foreignPayment.getCurrencyexchangeratetype());
                if (exchangeRateVO.getExchangeRate() == null) {
                    throw IMultilangConstant.noRateError/* "未取到汇率" */;
                } else {
                    foreignPayment.setCurrencyexchRate(exchangeRateVO.getExchangeRate());
                    foreignPayment.setCurrencyexchRateOps(exchangeRateVO.getExchangeRateOps());
                }
            }
        }
        if (!"02".equals(exchangeRateTypeByIdOrCode.get(0).get("code").toString())) {
            if (foreignPayment.getCurrency().equals(foreignPayment.getAccountcurrency())) {
                BigDecimal bigDecimal = new BigDecimal(Double.toString(1));
                foreignPayment.setSwapOutExchangeRateEstimate(bigDecimal);
                foreignPayment.setSwapOutExchangeRateEstimateOps((short) 1);
            } else {
                CmpExchangeRateVO exchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(foreignPayment.getCurrency(), foreignPayment.getNatCurrency(), foreignPayment.getVouchdate(), foreignPayment.getSwapOutExchangeRateType());
                if (exchangeRateVO.getExchangeRate() == null) {
                    throw IMultilangConstant.noRateError/* "未取到汇率" */;
                } else {
                    foreignPayment.setSwapOutExchangeRateEstimate(exchangeRateVO.getExchangeRate());
                    foreignPayment.setSwapOutExchangeRateEstimateOps(exchangeRateVO.getExchangeRateOps());
                }
            }
        }
        //计算折本币金额  amount*CurrencyexchRate
        //处理multiply精度为8位
        foreignPayment.setCurrencyamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(foreignPayment.getCurrencyexchRateOps(),
                foreignPayment.getCurrencyexchRate(), foreignPayment.getAmount(), 8));
        //计算换出金额预估  amount*SwapOutExchangeRate
        if (foreignPayment.getSwapOutExchangeRateEstimate() != null) {
            foreignPayment.setSwapOutAmountEstimate(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(foreignPayment.getSwapOutExchangeRateEstimateOps(),
                    foreignPayment.getSwapOutExchangeRateEstimate(), foreignPayment.getAmount(), 8));
        }
        //校验业务组织权限  要根据会计主体查询有核算委托关系的业务组织集合
        String org = foreignPayment.getOrg();
        Set<String> orgList = new HashSet<>();//有核算委托关系的组织id集合
        log.error("1.getOrgList,accentity={}", foreignPayment.getAccentity());
        orgList.add(foreignPayment.getAccentity());
        Set<String> orgidtsTmp = FIDubboUtils.getDelegateHasSelf(foreignPayment.getAccentity());
        log.error("2.getOrgList,delegateHasSelf={}", orgidtsTmp);
        orgList.addAll(orgidtsTmp);
        if (org != null) {
            orgList.add(org);
            orgList.remove(foreignPayment.getAccentity());
            if (org.equals(foreignPayment.getAccentity())) {
                orgList.clear();
                orgList.add(foreignPayment.getAccentity());
            }
        }
        OrgFuncSharingSettingApi orgFuncSharingSettingApi = AppContext.getBean(OrgFuncSharingSettingApi.class);
        List<String> listSharingOrgUnitIdsByServeScopeOrgIdList = orgFuncSharingSettingApi
                .listSharingOrgUnitIdsByServeScopeOrgIdList(
                        Collections.singletonList(foreignPayment.getAccentity()),
                        InvocationInfoProxy.getTenantid(),
                        "diwork",
                        OrgFuncSharingSettingQryParam.queryAll());
        log.error("3.getOrgList,listSharingOrgUnitIdsByServeScopeOrgIdList={}", listSharingOrgUnitIdsByServeScopeOrgIdList);
        orgList.addAll(listSharingOrgUnitIdsByServeScopeOrgIdList);
        if (!StringUtils.isEmpty(org)) {
            if (!orgList.contains(org)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00050", "只允许当前会计主体存在核算委托关系的组织!") /* "只允许当前会计主体存在核算委托关系的组织!" */);
            }
        }
        //银行账号/账户名称/开户行名称
        //收款方开户行名称
        String receivebankaddrName = "";
        //银行账户名称
        String bankAccountName = "";
        //银行账号
        String bankAccount = "";
        if (foreignPayment.getReceivetype() == CaObject.Customer.getValue()) {
            //当收款方类型=客户时，收款方名称传值给客户名称字段，根据传入值与资金组织有权限的客户编码或id进行匹配，如果匹配上则存储，未匹配到则提示：未匹配到客户名称，请检查
            Map<String, Object> customer = QueryBaseDocUtils.getCustomer(foreignPayment.getReceivenameid());
            if (MapUtils.isEmpty(customer)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100622"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B917E04900009", "未匹配到客户名称，请检查！"));
            } else {
                foreignPayment.setCustomer(Long.valueOf(customer.get("id").toString()));
                foreignPayment.setReceivenameid((customer.get("id").toString()));
                foreignPayment.setReceivename(customer.get("name").toString());
            }
//            List<MerchantApplyRangeDTO> customeList = QueryBaseDocUtils.queryMerchantApplyRange(foreignPayment.getCustomer());
//            boolean exist = false;
//            for (MerchantApplyRangeDTO customerMap : customeList) {
//                if (orgList.contains(customerMap.getOrgId())) {
//                    exist = true;
//                    break;
//                }
//            }
//            if (!exist) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100622"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B917E04900009", "未匹配到客户名称，请检查！"));
//            }
            //当收款方类型=客户时，收款银行账号传值给客户银行账号字段，根据传入值与客户的已启用的银行账号编码或id进行匹配，如果匹配上则存储，未匹配到则提示：当前客户未匹配到银行账户，请检查
            AgentFinancialDTO agentFinancialDTO = queryAgentFinancial(foreignPayment.getReceivebankaccountid());
            if (agentFinancialDTO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100623"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91AC04B00000", "当前客户未匹配到银行账户，请检查！"));
            } else {
                foreignPayment.setReceivebankaccountid(agentFinancialDTO.getId().toString());
                foreignPayment.setReceivebankaccountname(agentFinancialDTO.getBankAccountName());
                foreignPayment.setReceivebanktype(agentFinancialDTO.getBank());
                foreignPayment.setReceivebankaddr(agentFinancialDTO.getOpenBank());
            }
            if (!StringUtils.isEmpty(foreignPayment.getReceivebankaccountid())) {
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("id", foreignPayment.getReceivebankaccountid());
                List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryCustomerBankAccountByCondition(condition);
                if (!CollectionUtils.isEmpty(bankAccounts)) {
                    bankAccountName = bankAccounts.get(0).get("bankAccount").toString();
                    bankAccount = bankAccounts.get(0).get("bankAccountName").toString();
                }
            }
            MerchantRequst requst = new MerchantRequst(foreignPayment.getAccentity(), agentFinancialDTO.getBankAccount());
            CtmJSONObject cust2Check = MerchantUtils.cust2Check(requst);
            if (Objects.equals(MerchantConstant.FALSE, cust2Check.getString(MerchantConstant.CUSTOMERFLAG))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100623"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91AC04B00000", "当前客户未匹配到银行账户，请检查！"));
            }
            foreignPayment.setCustomerbankaccount(Long.valueOf(foreignPayment.getReceivebankaccountid()));
        } else if (foreignPayment.getReceivetype() == CaObject.Supplier.getValue()) {
            //当收款方类型=供应商，收款方名称id等于供应商名称id
            //当收款方类型=供应商时，收款银行账号传值给供应商银行账号字段，根据传入值与供应商的已启用的银行账号编码或id进行匹配，如果匹配上则存储，未匹配到则提示：当前供应商未匹配到银行账户，请检查
            List<Map<String, Object>> vendorBankVO = QueryBaseDocUtils.queryVendorBankByIdOrCode(foreignPayment.getReceivebankaccountid());
            if (CollectionUtils.isEmpty(vendorBankVO) ) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100624"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91CC04900000", "当前供应商未匹配到银行账户，请检查！"));
            } else {
                foreignPayment.setReceivebankaccountid(vendorBankVO.get(0).get("id").toString());
                foreignPayment.setReceivebankaccountname(vendorBankVO.get(0).get("accountname").toString());
                foreignPayment.setReceivebanktype(vendorBankVO.get(0).get("bank").toString());
                foreignPayment.setReceivebankaddr(vendorBankVO.get(0).get("openaccountbank").toString());
                bankAccount = vendorBankVO.get(0).get("account").toString();
                bankAccountName = vendorBankVO.get(0).get("accountname").toString();
            }
            MerchantRequst requst = new MerchantRequst(foreignPayment.getAccentity(), vendorBankVO.get(0).get("account").toString());
            CtmJSONObject vendor2Check = MerchantUtils.vendor2Check(requst);
            if (Objects.equals(MerchantConstant.FALSE, vendor2Check.getString(MerchantConstant.CUSTOMERFLAG))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100624"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91CC04900000", "当前供应商未匹配到银行账户，请检查！"));
            }
            foreignPayment.setSupplierbankaccount(Long.valueOf(foreignPayment.getReceivebankaccountid()));
            List<Map<String, Object>> venderByIdOrCode = QueryBaseDocUtils.getVenderByIdOrCode(foreignPayment.getReceivenameid());
            if (CollectionUtils.isEmpty(venderByIdOrCode)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100624"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91CC04900000", "当前供应商未匹配到银行账户，请检查！"));
            }
            foreignPayment.setSupplier(Long.valueOf(venderByIdOrCode.get(0).get("id").toString()));
            foreignPayment.setReceivename(venderByIdOrCode.get(0).get("name").toString());
            foreignPayment.setReceivenameid(venderByIdOrCode.get(0).get("id").toString());
//            List<VendorOrgVO> vendorOrgVOs = vendorQueryService.getVendorOrgByVendorId(Long.valueOf(foreignPayment.getReceivenameid()));
//            boolean exist = false;
//            for (VendorOrgVO vendorOrgVO : vendorOrgVOs) {
//                if (orgList.contains(vendorOrgVO.getOrg())) {
//                    exist = true;
//                    break;
//                }
//            }
//            if (!exist) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100624"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91CC04900000", "当前供应商未匹配到银行账户，请检查！"));
//            }
        } else if (foreignPayment.getReceivetype() == CaObject.Employee.getValue()) {
            //当收款方类型=人员时，收款方名称传值给员工名称字段，根据传入值与资金组织有权限的员工编码或id进行匹配，如果匹配上则存储，未匹配到则提示：未匹配到员工名称，请检查
            List<Map<String, Object>> staffList = QueryBaseDocUtils.queryEmployeeByIdOrCode(foreignPayment.getReceivenameid());
            if (CollectionUtils.isEmpty(staffList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100625"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B91E804900002", "未匹配到员工名称，请检查！"));
            }
//            List<Map<String, Object>> staffList1 = QueryBaseDocUtils.queryOrgByStaffId(staffList.get(0).get("id").toString());
//            boolean staffOrg = false;
//            if (!staffList1.isEmpty()) {
//                for (Map<String, Object> staff : staffList1) {
//                    if (orgList.contains(staff.get("org_id").toString())) {
//                        staffOrg = true;
//                        break;
//                    }
//                }
//            }
//            if (!staffOrg) {
//                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00078", "员工所属组织与资金组织不一致!") /* "员工所属组织与资金组织不一致!" */);
//            }
            foreignPayment.setEmployee(staffList.get(0).get("id").toString());
            foreignPayment.setReceivenameid(staffList.get(0).get("id").toString());
            foreignPayment.setReceivename(staffList.get(0).get("name").toString());
            //当收款方类型=人员时，收款银行账号传值给员工银行账号字段，根据传入值与员工的已启用的银行账号编码或id进行匹配，如果匹配上则存储，未匹配到则提示：当前员工未匹配到银行账户，请检查
            Map<String, Object> staffBankAccount = QueryBaseDocUtils.queryStaffBankAccountByIdOrCode(foreignPayment.getReceivebankaccountid());
            if (staffBankAccount == null || staffBankAccount.get("staff_id") == null || !staffBankAccount.get("staff_id").equals(foreignPayment.getEmployee())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100626"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B920804900005", "当前员工未匹配到银行账户，请检查！"));
            } else {
                foreignPayment.setReceivebankaccountid(staffBankAccount.get("id").toString());
                foreignPayment.setReceivebankaccountname(staffBankAccount.get("accountname").toString());
                foreignPayment.setStaffBankAccount(foreignPayment.getReceivebankaccountid());
                foreignPayment.setReceivebankaddr(staffBankAccount.get("bankname").toString());
                foreignPayment.setReceivebanktype(staffBankAccount.get("bank").toString());
                bankAccount = staffBankAccount.get("account").toString();
                bankAccountName = staffBankAccount.get("accountname").toString();
            }
        } else if (foreignPayment.getReceivetype() == CaObject.Other.getValue()) {
            //当收款方类型=其他时，收款方名称传值给其他名称字段，根据传入值存储
            //当收款方类型=其他时，收款银行账号传值给其他账号字段，根据传入值存储
            //收款方开户行名称、收款方银行类别，调整为非必填，只有对方类型为其他时必填
            if (StringUtils.isEmpty(foreignPayment.getReceivebankaddr()) && StringUtils.isEmpty(foreignPayment.get("receivebankaddr_name"))) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00053", "收款方开户行名称必填") /* "收款方开户行名称必填" */);
            }
            //对方类型为其他时，校验必填【 其他名称、其他账号】
            if (StringUtils.isEmpty(foreignPayment.getOthername())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00055", "其他名称必填") /* "其他名称必填" */);
            }
            if (StringUtils.isEmpty(foreignPayment.getOtherbankaccount())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00057", "其他账号必填") /* "其他账号必填" */);
            }
            if (StringUtils.isEmpty(foreignPayment.getOtherbankaccountname())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005A", "其他账户名称必填") /* "其他账户名称必填" */);
            }
            if (StringUtils.isEmpty(foreignPayment.getReceivebankaddr())) {
                foreignPayment.setReceivebankaddr(foreignPayment.get("receivebankaddr_name"));
            }
            List<Map<String, Object>> receivebankaddrList = QueryBaseDocUtils.queryBankDotByIdOrCode(foreignPayment.getReceivebankaddr());
            if (CollectionUtils.isEmpty(receivebankaddrList)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0005F", "收款方开户行名称未匹配到！") /* "收款方开户行名称未匹配到！" */);
            }
            foreignPayment.setReceivebankaddr(receivebankaddrList.get(0).get("id").toString());
            foreignPayment.setReceivebanktype(receivebankaddrList.get(0).get("bank").toString());
            foreignPayment.setReceivenameid("");
            foreignPayment.setReceivebankaccountid("");
        } else if (foreignPayment.getReceivetype() == CaObject.CapBizObj.getValue()) {
            List<Map<String, Object>> capBizObjbankaccountByIdOrCode = QueryBaseDocUtils.getCapBizObjbankaccountByIdOrCode(foreignPayment.getReceivebankaccountid());
            if (CollectionUtils.isEmpty(capBizObjbankaccountByIdOrCode)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100629"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B926C04900007", "当前资金业务对象未匹配到银行账户，请检查！"));
            } else {
                foreignPayment.setCapBizObjbankaccount(capBizObjbankaccountByIdOrCode.get(0).get("id").toString());
                foreignPayment.setReceivebankaccountid(capBizObjbankaccountByIdOrCode.get(0).get("id").toString());
                foreignPayment.setReceivebankaccountname(capBizObjbankaccountByIdOrCode.get(0).get("accountname").toString());
                bankAccount = capBizObjbankaccountByIdOrCode.get(0).get("bankaccount").toString();
                bankAccountName = capBizObjbankaccountByIdOrCode.get(0).get("accountname").toString();
            }
            List<Map<String, Object>> capBizObj = QueryBaseDocUtils.getCapBizObj(foreignPayment.getReceivenameid());
            if (CollectionUtils.isEmpty(capBizObj)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100628"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B924A04900002", "未匹配到资金业务对象名称，请检查！"));
            } else {
                foreignPayment.setCapBizObj(capBizObj.get(0).get("id").toString());
                foreignPayment.setReceivenameid(capBizObj.get(0).get("id").toString());
                foreignPayment.setReceivename(capBizObj.get(0).get("name").toString());
            }
            //当收款方类型=资金业务伙伴，收款方名称id等于资金业务对象名称id
            //当收款方类型=资金业务伙伴时，收款方名称传值给资金业务对象字段，根据传入值与资金组织有权限的资金业务对象编码或id进行匹配，如果匹配上则存储，未匹配到则提示：未匹配到资金业务对象名称，请检查
            //当收款方类型=资金业务伙伴时，收款银行账号传值给资金业务对象银行账号字段，根据传入值与资金业务对象的已启用的银行账号编码或id进行匹配，如果匹配上则存储，未匹配到则提示：当前资金业务对象未匹配到银行账户，请检查
            CtmJSONObject fundBusinObjArchivesItem = fundBusinessObjectQueryService.queryFundBusinessObjectDataById(foreignPayment.getCapBizObj(), capBizObjbankaccountByIdOrCode.get(0).get("bankaccount").toString(), null);
            if (ObjectUtils.isEmpty(fundBusinObjArchivesItem)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100629"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B926C04900007", "当前资金业务对象未匹配到银行账户，请检查！"));
            }else {
                foreignPayment.setReceivebankaddr(fundBusinObjArchivesItem.get("bopenaccountbankid").toString());
                foreignPayment.setReceivebanktype(fundBusinObjArchivesItem.get("bbankid").toString());
            }
        } else if (foreignPayment.getReceivetype() == CaObject.InnerUnit.getValue()) {
            //当收款方类型=内部单位，收款方名称id等于内部单位名称id
            //当收款方类型=内部单位时，收款方名称传值给内部单位名称字段，根据传入值与资金组织有权限的内部单位编码或id进行匹配，如果匹配上则存储，未匹配到则提示：未匹配到内部单位名称，请检查
            //当收款方类型=内部单位时，收款银行账号传值给内部单位银行账号字段，根据传入值与内部单位的已启用的银行账号编码或id进行匹配，如果匹配上则存储，未匹配到则提示：当前内部单位未匹配到银行账户，请检查
            EnterpriseBankAcctVO bankAcctVo = getBankAcctVo(foreignPayment.getReceivebankaccountid());
            if (bankAcctVo == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100631"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B92AA04B00008", "当前内部单位未匹配到银行账户，请检查！"));
            } else {
                foreignPayment.setOurbankaccount(bankAcctVo.getId());
                foreignPayment.setReceivebankaccountid(bankAcctVo.getId());
                foreignPayment.setReceivebankaccountname(bankAcctVo.getAcctName());
                foreignPayment.setReceivebankaddr(bankAcctVo.getBankNumber());
                foreignPayment.setReceivebanktype(bankAcctVo.getBank());
                bankAccountName = bankAcctVo.getName();
                bankAccount = bankAcctVo.getAccount();
            }
//            boolean receivebankaccountValidate = false;
//            if (CollectionUtils.isNotEmpty(bankAcctVo.getCurrencyList())) {
//                List<String> currencies = bankAcctVo.getCurrencyList().stream().map(BankAcctCurrencyVO::getCurrency).collect(Collectors.toList());
//                if (CollectionUtils.isNotEmpty(currencies) && currencies.contains(foreignPayment.getCurrency())) {
//                    receivebankaccountValidate = true;
//                }
//            }
//            if (!receivebankaccountValidate) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100631"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B92AA04B00008", "当前内部单位未匹配到银行账户，请检查！"));
//            }
            foreignPayment.setOurname(foreignPayment.getReceivenameid());
            ConditionDTO idCondition = ConditionDTO.newCondition().withEnabled();
            List<String> oppositeObjectNames = new ArrayList<>();
            oppositeObjectNames.add(foreignPayment.getOurname());
            idCondition.andIdIn(oppositeObjectNames);
            List<FundsOrgDTO> idFundsOrgDTOS = fundsOrgQueryServiceComponent.getByCondition(idCondition);
            ConditionDTO codeCondition = ConditionDTO.newCondition().withEnabled();
            codeCondition.andCodeIn(oppositeObjectNames);
            List<FundsOrgDTO> codeFundsOrgDTOS = fundsOrgQueryServiceComponent.getByCondition(codeCondition);
            List<Map<String, Object>> innerUnitList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(idFundsOrgDTOS)) {
                Map<String, Object> map1 = new HashMap<>();
                map1.put("id", idFundsOrgDTOS.get(0).getId());
                map1.put("name", idFundsOrgDTOS.get(0).getName());
                innerUnitList.add(map1);
            }
            if (CollectionUtils.isNotEmpty(codeFundsOrgDTOS)) {
                Map<String, Object> map2 = new HashMap<>();
                map2.put("id", codeFundsOrgDTOS.get(0).getId());
                map2.put("name", codeFundsOrgDTOS.get(0).getName());
                innerUnitList.add(map2);
            }
            if (CollectionUtils.isEmpty(innerUnitList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100630"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B928C04B00008", "未匹配到内部单位名称，请检查！"));
            } else {
                foreignPayment.setOurname(innerUnitList.get(0).get("id").toString());
                foreignPayment.setReceivenameid(innerUnitList.get(0).get("id").toString());
                foreignPayment.setReceivename(innerUnitList.get(0).get("name").toString());
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100632"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D9B92C804B00003", "收款方类型不正确，请检查！"));
        }
        if (foreignPayment.getIsdirectlyconnected() == 1) {
            if (StringUtils.isEmpty(foreignPayment.getReceivebankaddress())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00079", "直联时收款方开户行地址必填！") /* "直联时收款方开户行地址必填！" */);
            }
            if (StringUtils.isEmpty(foreignPayment.getReceivebankswift())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B0007A", "收款方开户行SWIFT必填！") /* "收款方开户行SWIFT必填！" */);
            }
        }

        //国内外费用承担方为汇款人时，导入后全额到账应带出为是、费用支付账户带出为付款方银行账户、费用支付账户（外币）带出为付款方银行账户
        if (StringUtils.isNotEmpty(foreignPayment.getCostbearers()) && foreignPayment.getCostbearers().equals("OUR") ) {
            foreignPayment.setIsfullpayment((short) 1);
            foreignPayment.setPaymentaccount(foreignPayment.getPaymenterprisebankaccount());
            foreignPayment.setForeignpaymentaccount(foreignPayment.getPaymenterprisebankaccount());
        }

        //收款方校验条件为空时，保存数据时进行中文、字符校验
        // 20250409,导入导出需求  后端不做特殊字符校验
//        boolean validate = true;
//        Object receivevalidate = foreignPayment.get("receivevalidate");
//        if (receivevalidate != null && receivevalidate instanceof Short) {
//            Short receivevalidateN = (Short) receivevalidate;
//            if (receivevalidateN == 0) {
//                //0：关闭中文校验
//                validate = false;
//            }
//        }
//        if (validate) {
//            if (!StringUtils.isEmpty(foreignPayment.getReceivebankaddr())) {
//                List<Map<String, Object>> receivebankaddrList = QueryBaseDocUtils.queryBankDotByIdOrCode(foreignPayment.getReceivebankaddr());
//                if (!CollectionUtils.isEmpty(receivebankaddrList)) {
//                    receivebankaddrName = receivebankaddrList.get(0).get("name").toString();
//                }
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(bankAccountName)) {
//                throw new CtmException("银行账户名称中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(bankAccount)) {
//                throw new CtmException("银行账号中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(receivebankaddrName)) {
//                throw new CtmException("收款方开户行名称中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getPayernamenocn())) {
//                throw new CtmException("付款方名称（非中文）中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getAddress())) {
//                throw new CtmException("汇款人地址中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndChineseSymbols(foreignPayment.getPostscript())) {
//                throw new CtmException("汇款附言包含中文汉字或者中文符号 请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getReceivenameother())) {
//                throw new CtmException("收款方非中文名称中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getApplicantname())) {
//                throw new CtmException("填报人姓名中包含非法特殊字符:<> \" ' % {} @ ＃ & $ ? ; ° ,  请重新输入！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getReceivebankswift())) {
//                throw new CtmException("收款方开户行SWIFT中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getOtherbankaccount())) {
//                throw new CtmException("银行账号中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getOtherbankaccountname())) {
//                throw new CtmException("银行账户名称中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getReceiveaddress())) {
//                throw new CtmException("收款方地址中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getAgencybankaccount())) {
//                throw new CtmException("收款方开户行在其代理行账号中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getAgencybankname())) {
//                throw new CtmException("代理行名称中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getAgencybankaddress())) {
//                throw new CtmException("代理行地址中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getAgencybankswift())) {
//                throw new CtmException("代理行SWIFT中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//            if (RegexUtil.validateChineseAndIllegalChars(foreignPayment.getReceivebankaddress())) {
//                throw new CtmException("收款方开户行地址中包含中文或者非法字符：<> \" ' % {} @ ＃ & $ ? ; °  请输入英文字符、数字、半角标点符号或空格！");
//            }
//        }
    }

    public BdTransType queryTransType(String code) throws Exception {
        BdTransType bdTransType = transTypeQueryService.queryTransTypes(code);
        if (bdTransType == null) {
            bdTransType = transTypeQueryService.findById(code);
        }
        return bdTransType;
    }

    public AgentFinancialDTO queryAgentFinancial(String code) throws Exception {
        AgentFinancialDTO agentFinancialDTO = null;
        try {
            agentFinancialDTO = customerQueryService.getCustomerAccountByAccountNo(code);
            if (agentFinancialDTO == null) {
                agentFinancialDTO = customerQueryService.getCustomerAccountByAccountId(Long.valueOf(code));
            }
        } catch (Exception e) {
            log.error("queryAgentFinancial", e);
        }
        return agentFinancialDTO;
    }

    public EnterpriseBankAcctVO getBankAcctVo(String code) throws Exception {
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(code);
        if (enterpriseBankAcctVO == null) {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setAccount(code);
            List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = enterpriseBankQueryService.query(enterpriseParams);
            if (CollectionUtils.isNotEmpty(enterpriseBankAcctVOS)) {
                enterpriseBankAcctVO = enterpriseBankAcctVOS.get(0);
            }
        }
        return enterpriseBankAcctVO;
    }



}
