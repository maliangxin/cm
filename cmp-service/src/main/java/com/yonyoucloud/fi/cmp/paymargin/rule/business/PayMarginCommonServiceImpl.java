package com.yonyoucloud.fi.cmp.paymargin.rule.business;

import com.google.common.base.Strings;
import com.yonyou.iuap.billcode.service.v2.IBillCodeGenForBusiObjService;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SettleFlagEnum;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginApiService;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付保证金保存前规则*
 *
 * @author xuxbo
 * @date 2023/8/3 10:09
 */

@Slf4j
@Component
public class PayMarginCommonServiceImpl extends AbstractCommonRule {

    public static final String CHARACTER_DEF = "characterDef";

    @Autowired
    MarginWorkbenchService marginWorkbenchService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BillTypeQueryService billTypeQueryService;

    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    VendorQueryService vendorQueryService;

    @Autowired
    FundBusinessObjectQueryService fundBusinessObjectQueryService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;
    @Autowired
    private PayMarginApiService payMarginApiService;

    @Autowired
    private CmCommonService<Object> cmCommonService;

    @Autowired
    private IBillCodeGenForBusiObjService billCodeGenService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            //和马良沟通的结果，在保存后，如果是是新增的情况，并且是自动情况，并且 原始业务号是否！=单据编号，则重新设置 原始业务号=单据编号
            boolean autoBussinessNo = bizObject.get("autoBusinessNo");
            String code = bizObject.get("code");
            String marginBusinessNo = bizObject.get("marginBusinessNo");
            if (!code.equals(marginBusinessNo) && autoBussinessNo && bizObject.getEntityStatus() == EntityStatus.Insert) {
                bizObject.set("marginbusinessno", bizObject.get("code"));
            }

            PayMargin payMargin = (PayMargin) bizObject;
            //针对投标保证金 进行加锁控制，锁的key为 srcitem121 + srcbillid ；但是需要判断srcbillid不能为空
            if (payMargin.getSrcitem() != null && payMargin.getSrcitem().equals(EventSource.BidMargin.getValue())) {
                String srcbillid = payMargin.getSrcbillid();
                if (StringUtils.isNotEmpty(srcbillid)) {
                    String keyid = payMargin.getSrcitem() + srcbillid;
                    YmsLock ymsLock = JedisLockUtils.lockBillWithOutTraceByTime(keyid, 30);
                    if (null == ymsLock) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100185"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D5", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                    }
                    //只加锁无法解决问题，还需要根据srcbillid查库 如果已经存在一条 则无法继续保存！
                    QuerySchema querySchema = QuerySchema.create().addSelect("id");
                    QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                    queryConditionGroup.addCondition(QueryCondition.name("srcbillid").eq(srcbillid));
                    querySchema.appendQueryCondition(queryConditionGroup);
                    List<PayMargin> paymarginBySrcbillid = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
                    if (paymarginBySrcbillid.size() > 1) {
                        // 查重后需要释放锁
                        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F4BEDA404080002", "保存失败！此投标保证金已经推单，请检查重试！") /* "保存失败！此投标保证金已经推单，请检查重试！" */);
                    }
                }
            }
            //CM202400472传结算的现金管理单据控制0金额数据不传给结算
            if (!Objects.isNull(payMargin.getSettleflag()) && SettleFlagEnum.YES.getValue() == payMargin.getSettleflag()
                    && !Objects.isNull(payMargin.getMarginamount()) && payMargin.getMarginamount().compareTo(BigDecimal.ZERO) == 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E86328005200002", "传结算的单据，保证金金额不可为0！") /* "传结算的单据，保证金金额不可为0！" */);
            }
            //交易类型停用校验
            CmpCommonUtil.checkTradeTypeEnable(payMargin.getTradetype());
            //如果是通过转换保证金过来的  则直接保存
            if (ObjectUtils.isNotEmpty(payMargin.get("isConvert")) && payMargin.get("isConvert").equals(true)) {

                //转换类型的保证金要是审批态
                bizObject.set("verifystate", VerifyState.COMPLETED.getValue());
                Date currentDate = new Date();
                bizObject.set("auditDate", currentDate);
                bizObject.set("auditTime", currentDate);
                bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                bizObject.set("auditor", AppContext.getCurrentUser().getName());
                bizObject.set("status", Status.confirmed.getValue());

                bizObject.remove("pubts");
                bizObject.setEntityStatus(EntityStatus.Update);
                if (bizObject.containsKey("characterDef")) {
                    Object characterDefObj = bizObject.get("characterDef");
                    // instanceOf会判断null，为null则表达式结果是false
                    if (characterDefObj instanceof BizObject) {
                        BizObject characterDefBizObject = (BizObject) characterDefObj;
                        characterDefBizObject.setEntityStatus(EntityStatus.Update);
                    }
                }
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, bizObject);
                return new RuleExecuteResult();
            }
            BillDataDto billDataDto = (BillDataDto) getParam(map);
            //导入
            boolean importFlag = "import".equals(billDataDto.getRequestAction());
            // OpenApi
            boolean openApiFlag = bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true);
            boolean fromApi = billDataDto.getFromApi();
            // 如果是导入进来的
            if (importFlag || openApiFlag || fromApi) {
                // 需要根据资金组织查询本币币种 以及  计算本币金额，并赋值
                // 针对OpenApi请求的数据进行额外处理
                if (openApiFlag) {
                    payMarginApiService.beforeSaveForOpenApi(payMargin);
                }
                // 需要根据会计主体查询本币币种 以及  计算本币金额，并赋值
                // 组织本币
                String natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
                if (StringUtils.isEmpty(natCurrency)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100959"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180807", "资金组织[") /* "资金组织[" */ + bizObject.get("accentity_name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180806", "]组织本币币种为空!") /* "]组织本币币种为空!" */);
                }
                CurrencyTenantDTO natcurrencyTenantDTO = baseRefRpcService.queryCurrencyById(natCurrency);

                if (natcurrencyTenantDTO != null) {
//                    bizObject.set("natCurrency", natcurrencyTenantDTO.getId());
//                    bizObject.set("natCurrency_name", natcurrencyTenantDTO.getName());
//                    bizObject.set("natCurrency_moneyDigit", natcurrencyTenantDTO.getMoneydigit());
//                    bizObject.set("natCurrency_code", natcurrencyTenantDTO.getCode());
                    payMargin.setNatCurrency(natcurrencyTenantDTO.getId());
                }
                //计算本币金额 exchRate
                BigDecimal exchRate = payMargin.getExchRate();
                // 如果没有填则自动计算
                if (exchRate == null) {
                    CtmJSONObject exchRateResult = payMarginApiService.getExchRate(payMargin);
                    payMargin.setExchRate((BigDecimal) exchRateResult.get("exchRate"));
                    payMargin.setExchRateOps((Short) exchRateResult.get("exchRateOp"));
                }
                BigDecimal marginamount = payMargin.getMarginamount();
                Short moneyDigit = Short.valueOf(natcurrencyTenantDTO.getMoneydigit().toString());
                if (ObjectUtils.isNotEmpty(marginamount)) {
                    payMargin.setNatmarginamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(payMargin.getExchRateOps(), payMargin.getExchRate(), marginamount, Integer.valueOf(moneyDigit)));
                }

                // 虚拟户是否为空，为空  事项来源默认 现金管理  单据类型默认 支付保证金台账管理 ；不为空 根据虚拟户查询  事项来源和单据类型
                // 虚拟户 不为空时 需要根据校验与当前的资金组织是否一致
                if (ObjectUtils.isEmpty(payMargin.getMarginvirtualaccount())) {
                    if (payMargin.getSrcitem() == null) {
                        payMargin.setSrcitem(EventSource.Cmpchase.getValue());
                    }
                    //获取单据类型
                    BdBillType billType = billTypeQueryService.queryBillTypeId("CM.cmp_paymargin");
                    payMargin.setBilltype(billType.getId());
                } else {
                    String id = payMargin.getMarginvirtualaccount().toString();
                    MarginWorkbench marginWorkbench = MetaDaoHelper.findById(MarginWorkbench.ENTITY_NAME, id);
                    if (ObjectUtils.isNotEmpty(marginWorkbench)) {
                        payMargin.setSrcitem(marginWorkbench.getSrcItem());
                        payMargin.setBilltype(marginWorkbench.getBillType());
                        if (!payMargin.getAccentity().equals(marginWorkbench.getAccentity())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100960"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227AA79004C00000", "保证金虚拟户的资金组织与当前资金组织不一致！") /* "保证金虚拟户的资金组织与当前资金组织不一致！*/);
                        }
                        //币种 保证金原始业务号
                        payMargin.setCurrency(marginWorkbench.getCurrency());
                        payMargin.setMarginbusinessno(marginWorkbench.getMarginBusinessNo());
                        // 保证金虚拟户 与 原始业务号都不为空的时候  需要校验两者的一致性
                        if (!marginWorkbench.getMarginBusinessNo().equals(payMargin.getMarginbusinessno())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100961"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C64F0005C80007", "保证金虚拟户的原始业务号与当前输入的原始业务号不一致！") /* "保证金虚拟户的原始业务号与当前输入的原始业务号不一致！*/);
                        }
                    }

                }
                // 是否结算 如果是  需要校验必填项：结算方式  结算状态  本方银行账户名称
                if (ObjectUtils.isEmpty(payMargin.getSettleflag())) {
                    payMargin.setSettleflag((short) 0);
                }
                if (payMargin.getSettleflag() == 1) {
                    if (ObjectUtils.isEmpty(payMargin.getSettlemode())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100962"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A48BFA05880001", "需结算时，结算方式必填！") /* "需结算时，结算方式必填！" */);
                    }
                    if (ObjectUtils.isEmpty(payMargin.getSettlestatus())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100963"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A48CA205D00003", "需结算时，结算状态必填！") /* "需结算时，结算状态必填！" */);
                    }
                    if (ObjectUtils.isEmpty(payMargin.getEnterprisebankaccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100964"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A48D0405D00008", "需结算时，本方银行账户必填！") /* "需结算时，本方银行账户必填！ */);
                    }
                }
                //是否转换 交易类型只有为 收到保证金 并且虚拟户有值的时候  才能为是 ；如果为是 需要校验 转换金额必填 本币转换金额赋值 新保证金原始业务号必填 新保证金类型必填
                BdTransType transType = transTypeQueryService.findById(payMargin.getTradetype());
                if (ObjectUtils.isEmpty(transType)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100965"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4937605D00005", "未查到交易类型！") /* "未查到交易类型！*/);
                }
                if (payMargin.getConversionmarginflag() == 1) {
                    if (transType.getCode().equals("cmp_paymargin_payment")) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102085"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4925A05880000", "只有交易类型为取回保证金时，才能进行转换保证金！") /* "只有交易类型为取回保证金时，才能进行转换保证金！*/);
                    }
                    if (ObjectUtils.isEmpty(payMargin.getConversionamount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100967"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4965805880006", "转换保证金为是时，转换金额必填！") /* "转换保证金为是时，转换金额必填！*/);
                    }
                    if (ObjectUtils.isEmpty(payMargin.getNewmarginbusinessno())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100968"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4972405D00008", "转换保证金为是时，新保证金原始业务号必填！") /* "转换保证金为是时，新保证金原始业务号必填！*/);
                    }
                    if (ObjectUtils.isEmpty(payMargin.getNewmargintype())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100969"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4975C05D00005", "转换保证金为是时，新保证金类型必填！") /* "转换保证金为是时，新保证金类型必填！*/);
                    }

                    //计算natmarginamount
                    BigDecimal natconversionamount = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(payMargin.getExchRateOps(), payMargin.getExchRate(), payMargin.getConversionamount(), null);
                    natconversionamount = new BigDecimal(natconversionamount.toString()).setScale(moneyDigit, BigDecimal.ROUND_HALF_UP);
                    payMargin.setNatconversionamount(natconversionamount);
                }

                // 导入取出保证金时，需校验是否有虚拟账户，没有虚拟账户时应不可导入成功
                if (transType.getCode().equals("cmp_paymargin_withdraw") && ObjectUtils.isEmpty(payMargin.getMarginvirtualaccount())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102086"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3B4AE05A80008", "交易类型为取回保证金时，保证金虚拟户不可为空！") /* "交易类型为取回保证金时，保证金虚拟户不可为空！*/);
                }
                //导入时需要校验收付类型的取值
                //  内部单位的  全为付款 else 根据交易类型 支付 ： 付款  取回 ：收款
//                if (ObjectUtils.isNotEmpty(payMargin.getOppositetype()) && payMargin.getOppositetype() == MarginOppositeType.OwnOrg.getValue()) {
//                    payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
//                } else {
//                    if (transType.getCode().equals("cmp_paymargin_payment")) {
//                        payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
//                    } else {
//                        payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
//                    }
//                }
                //2024.10.29  支付保证金 收付类型赋值有优化
                //当交易类型为取回保证金，对方类型为：内部单位时，内部单位是本单位，且对方账户有值的赋值为：付款
                //对方类型为：内部单位时，内部单位是本单位，且对方账户没有值的赋值为：收款  内部单位不是本单位时，赋值为：收款
                if (transType.getCode().equals("cmp_paymargin_withdraw")) {
                    if (ObjectUtils.isNotEmpty(payMargin.getOppositetype()) && payMargin.getOppositetype() == MarginOppositeType.OwnOrg.getValue()) {
                        String accentity = payMargin.getAccentity();
                        String ourbankaccountname = payMargin.getOurname();
                        String ourbankaccount = payMargin.getOurbankaccount();
                        if (ObjectUtils.isNotEmpty(ourbankaccountname) && ourbankaccountname.equals(accentity)) {
                            if (ObjectUtils.isNotEmpty(ourbankaccount)) {
                                payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
                            } else {
                                payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                            }
                        } else {
                            payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                        }
                    } else {
                        payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                    }
                } else {
                    payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
                }

                String currency;
                if (ObjectUtils.isNotEmpty(payMargin.getCurrency())) {
                    currency = payMargin.getCurrency();
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100823"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00057", "币种不可为空!") /* "币种不可为空!" */);
                }
                //todo 导入后对方开户网点、对方银行类别需要根据账户自动带出值
                if (ObjectUtils.isNotEmpty(payMargin.getOppositetype())) {
                    Short oppositetype = payMargin.getOppositetype();
                    String oppositebankaccount = "";
                    switch (oppositetype) {
                        case 1: //客户
                            CustomerQueryService customerQueryService = AppContext.getBean(CustomerQueryService.class);
                            oppositebankaccount = bizObject.get("customerbankaccount_bankAccount");
                            AgentFinancialDTO bankAccount;
                            if (StringUtils.isNotEmpty(oppositebankaccount)) {
                                bankAccount = customerQueryService.getCustomerAccountByAccountNo(oppositebankaccount);
                            } else {// openapi请求的数据没有这个属性，直接通过客户银行账号id查询
                                bankAccount = customerQueryService.getCustomerAccountByAccountId(payMargin.getCustomerbankaccount());
                            }
                            if (ObjectUtils.isNotEmpty(bankAccount)) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100973"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3D3BA04D00000", "未查到客户银行账户信息！") /* "未查到客户银行账户信息！*/);
                                //校验币种
                                if (!currency.equals(bankAccount.getCurrency())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100974"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4219A04800009", "客户银行账户币种与当前币种不一致！") /* "客户银行账户币种与当前币种不一致！*/);
                                }
                                // 赋值开户网点
                                payMargin.setOppositebankNumber(bankAccount.getOpenBank());
                                // 赋值银行类别
                                payMargin.setOppositebankType(bankAccount.getBank());
                            }
                            break;
                        case 2: //供应商
                            oppositebankaccount = ValueUtils.isNotEmptyObj(payMargin.getSupplierbankaccount()) ?
                                    payMargin.getSupplierbankaccount().toString() : null;
                            VendorBankVO supplierbankaccount = vendorQueryService.getVendorBanksByAccountId(oppositebankaccount);
                            if (ObjectUtils.isNotEmpty(supplierbankaccount)) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100975"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3D99404D00002", "未查到供应商银行账户信息！") /* "未查到供应商银行账户信息！*/);
                                //校验币种
                                if (!currency.equals(supplierbankaccount.getCurrency())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100976"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4221A04380007", "供应商银行账户币种与当前币种不一致！") /* "供应商银行账户币种与当前币种不一致！*/);
                                }
                                // 赋值开户网点
                                payMargin.setOppositebankNumber(supplierbankaccount.getOpenaccountbank());
                                // 赋值银行类别
                                payMargin.setOppositebankType(supplierbankaccount.getBank());
                            }
                            break;
                        case 3: //其他
                            oppositebankaccount = null;
                            break;
                        case 4: //内部单位
                            oppositebankaccount = payMargin.getOurbankaccount();
                            // 内部单位银行账号判空
                            EnterpriseBankAcctVO enterpriseBankAcctVO = null;
                            if (!Strings.isNullOrEmpty(oppositebankaccount)) {
                                enterpriseBankAcctVO = enterpriseBankQueryService.findById(oppositebankaccount);
                            }
                            if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100977"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3E10005A80007", "未查到内部单位银行账户信息！") /* "未查到内部单位银行账户信息！*/);
                                //校验币种
                                List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
                                List<String> currencyid = new ArrayList<>();
                                for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                                    currencyid.add(bankAcctCurrencyVO.getCurrency());
                                }
                                if (!currencyid.contains(currency)) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100976"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4221A04380007", "内部单位银行账户币种与当前币种不一致！") /* "内部单位银行账户币种与当前币种不一致！*/);
                                }
                                // 赋值开户网点 bankNumber
                                payMargin.setOppositebankNumber(enterpriseBankAcctVO.getBankNumber());
                                // 赋值银行类别bank
                                payMargin.setOppositebankType(enterpriseBankAcctVO.getBank());
                            }
                            break;
                        case 5: //资金业务对象
//                            oppositebankaccount = payMargin.getCapBizObjbankaccount();
                            oppositebankaccount = bizObject.get("capBizObjbankaccount_bankaccount");
                            String capBizObj = payMargin.getCapBizObj();
                            if (ObjectUtils.isNotEmpty(oppositebankaccount) && ObjectUtils.isNotEmpty(capBizObj)) {
                                //查询资金业务对象的银行信息
                                CtmJSONObject fundBusinObjArchivesItem = fundBusinessObjectQueryService.queryFundBusinessObjectDataById(capBizObj, oppositebankaccount, null);
                                if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem)) {
                                    //校验币种
                                    if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem.get("currency"))) {
                                        if (!currency.equals(fundBusinObjArchivesItem.get("currency"))) {
                                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100978"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4405A04380002", "资金业务对象银行账户币种与当前币种不一致！") /* "资金业务对象银行账户币种与当前币种不一致！*/);
                                        }
                                    }
                                    // 赋值开户网点
                                    if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem.get("bopenaccountbankid"))) {
                                        payMargin.setOppositebankNumber(fundBusinObjArchivesItem.get("bopenaccountbankid").toString());
                                    }

                                    // 赋值银行类别
                                    if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem.get("bbankid"))) {
                                        payMargin.setOppositebankType(fundBusinObjArchivesItem.get("bbankid").toString());
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }

                }

                //todo  本方银行账户要根据币种过滤
                if (ObjectUtils.isNotEmpty(payMargin.getEnterprisebankaccount())) {
                    EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(payMargin.getEnterprisebankaccount());
                    if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100979"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4435A04800001", "未查到本方银行账户信息！") /* "未查到本方银行账户信息！*/);
                    } else {
                        //校验币种
                        List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
                        List<String> currencyid = new ArrayList<>();
                        for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                            currencyid.add(bankAcctCurrencyVO.getCurrency());
                        }
                        if (!currencyid.contains(currency)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100980"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B443D204380008", "本方银行账户币种与当前币种不一致！") /* "本方银行账户币种与当前币种不一致！*/);
                        }

                    }

                }

                //结算状态 只能导入待结算 和 已结算补单
                if (ObjectUtils.isNotEmpty(payMargin.getSettlestatus())) {
                    if (!(payMargin.getSettlestatus().equals(FundSettleStatus.WaitSettle.getValue()) || payMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue()))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100981"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C6570A05C80006", "结算状态只支持导入待结算和已结算补单！") /* "结算状态只支持导入待结算和已结算补单！*/);
                    }
                }

                //付款结算模式
                Short paymentsettlemode = payMargin.getPaymentsettlemode();
                if (ObjectUtils.isEmpty(paymentsettlemode)) {
                    //如果付款结算模式为空，则赋默认值为 主动结算
                    payMargin.setPaymentsettlemode(PaymentSettlemode.ActiveSettlement.getValue());
                }

                // 校验交易类型是否属于当前业务单据
                //不为空需要判断此对象是否是付款单的
                List<Map<String, Object>> transTypeList = cmCommonService.queryTransTypesByForm_ids(ICmpConstant.CM_CMP_PAYMARGIN);
                List<Object> codeList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(transTypeList)) {
                    transTypeList.stream().forEach(transtypemap -> {
                        if (null != transtypemap) {
                            List<Object> codes = transtypemap.entrySet().stream().filter(entry -> entry.getKey().equals("code")).map(Map.Entry::getValue).collect(Collectors.toList());
                            codeList.addAll(codes);
                        }
                    });
                }
                String tradetype_code = bizObject.get("tradetype_code");
                if (null == codeList || !codeList.contains(tradetype_code)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F98FA1204B80002", "填写的交易类型不属于是支付保证金的交易类型，请在重新录入！") /* "填写的交易类型不属于是支付保证金的交易类型，请在重新录入！" */);
                }


            }

            CtmJSONObject params = new CtmJSONObject();
            params.put(ICmpConstant.PAYMARGIN, payMargin);
            try {
                if (payMargin.getEntityStatus() == EntityStatus.Insert) {
                    //调用工作台保存接口，接口判断是否存在虚拟户
                    String marginvirtualaccount = marginWorkbenchService.payMarginWorkbenchSave(params);
                    payMargin.setMarginvirtualaccount(Long.valueOf(marginvirtualaccount));
                } else if (payMargin.getEntityStatus() == EntityStatus.Update) {
                    PayMargin payMargin_old = payMargin.get(PayMarginBeforeSaveRule.OLD_PAYMARGIN);
                    //保存前金额
                    BigDecimal marginamount_old = payMargin_old.getMarginamount();
                    //保存前本币金额
                    BigDecimal natmarginamount_old = payMargin_old.getNatmarginamount();
                    params.put(ICmpConstant.DBMARGINAMOUNT, marginamount_old);
                    params.put(ICmpConstant.DBNATMARGINAMOUNT, natmarginamount_old);
                    //保存前转换金额
                    if (payMargin_old.getConversionmarginflag() == 1) {
                        BigDecimal conversionamount_old = payMargin_old.getConversionamount();
                        BigDecimal natconversionamount_old = payMargin_old.getNatconversionamount();
                        params.put(ICmpConstant.DBCONVERSIONAMOUNT, conversionamount_old);
                        params.put(ICmpConstant.DBNATCONVERSIONAMOUNT, natconversionamount_old);
                    }
                    String marginvirtualaccount = marginWorkbenchService.payMarginWorkbenchSave(params);
                    payMargin.setMarginvirtualaccount(Long.valueOf(marginvirtualaccount));
                }

            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102088"), e.getMessage());
            }
            //校验：对方类型为内部单位 并且 内部单位名称与资金组织一致 此时 是否同名账户划转 设置为 是
            if (payMargin.getOppositetype() == MarginOppositeType.OwnOrg.getValue() && ValueUtils.isNotEmpty(payMargin.getOurname())) {
                String accentity = payMargin.getAccentity();
                String ourbankaccountname = payMargin.getOurname();
                if (accentity.equals(ourbankaccountname)) {
                    payMargin.setSamenametransferflag((short) 1);
                    //校验 如果是同名账户划转的，内部账户必填
                    //2024.10.28 保证金新需求 去掉这个校验
//                    if (payMargin.getSamenametransferflag() == 1) {
//                        if (ValueUtils.isEmpty(payMargin.getOurbankaccount())) {
//                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102089"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194620BE05580002", "不允许保存，同名账户划转时，内部账户必填！") /* "不允许保存，同名账户划转时，内部账户必填！" */);
//                        }
//                    }
                }
            }

            //校验 当轧差识别码不为空时 轧差结算总笔数  轧差后金额  轧差后收付方向 必填
            if (!ObjectUtils.isEmpty(payMargin.getNetIdentificateCode())) {
                if (ObjectUtils.isEmpty(payMargin.getNetSettleCount())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102090"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B81B7FC05480001", "当轧差识别码不为空时，轧差结算总笔数必填！") /* "当轧差识别码不为空时，轧差结算总笔数必填！" */);
                }
                if (ObjectUtils.isEmpty(payMargin.getAfterNetAmt())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102091"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B81B98205200007", "当轧差识别码不为空时，轧差后金额必填！") /* "当轧差识别码不为空时，轧差后金额必填！" */);
                }
                if (ObjectUtils.isEmpty(payMargin.getAfterNetDir())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102092"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B81B9C005480005", "当轧差识别码不为空时，轧差后收付方向必填！") /* "当轧差识别码不为空时，轧差后收付方向必填！" */);
                }
            }

            // 校验 如果单据类型为空时 赋值为：收到保证金台账管理
            if (ValueUtils.isEmpty(payMargin.getBilltype())) {
                //根据 forimid 查询单据类型
                BdBillType bdBillType = baseRefRpcService.queryBillTypeByFormId(ICmpConstant.CM_CMP_PAYMARGIN);
                String billTypeId = bdBillType.getId();
                payMargin.setBilltype(billTypeId);
            }

            // todo 对方类型为资金业务对象类型、同时资金业务对象类型为资金组织时，校验资金组织档案id不能等于单据所属资金组织。
            //对方类型
            Short oppositetype = payMargin.getOppositetype();
            if (oppositetype == MarginOppositeType.CapBizObj.getValue()) {
                //获取资金业务对象id
                String capBizObj = payMargin.getCapBizObj();
                if (ObjectUtils.isNotEmpty(capBizObj)) {
                    //查询资金业务对象
                    BillContext context = new BillContext();
                    context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
                    context.setDomain("yonbip-fi-ctmtmsp");
                    QuerySchema schema = QuerySchema.create();
                    schema.addSelect("id, accentity, fundbusinobjtypename, enabled, fundbusinobjtypeid");
                    schema.appendQueryCondition(QueryCondition.name("id").eq(capBizObj));
                    log.info("getObjectContent, schema = {}", schema);
                    List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);
                    if (CollectionUtils.isNotEmpty(result)) {
                        //获取数据实体
                        CtmJSONObject jsonObject = new CtmJSONObject(result.get(0));
                        //获取资金业务对象类型id
                        String typeId = jsonObject.get("fundbusinobjtypeid").toString();
                        //根据资金业务对象类型id查询业务对象类型数据tmsp.fundbusinobjtype.FundBusinObjType
                        BillContext context_type = new BillContext();
                        context_type.setFullname("tmsp.fundbusinobjtype.FundBusinObjType");
                        context_type.setDomain("yonbip-fi-ctmtmsp");
                        QuerySchema schema_type = QuerySchema.create();
                        schema_type.addSelect("id, code, objectName, enabled");
                        schema_type.appendQueryCondition(QueryCondition.name("id").eq(typeId));
                        log.info("getObjectContent, schema_type = {}", schema_type);
                        List<Map<String, Object>> result_type = MetaDaoHelper.query(context_type, schema_type);
                        String acctyty_type = ValueUtils.isNotEmptyObj(jsonObject.get(ICmpConstant.ACCENTITY)) ? jsonObject.get(ICmpConstant.ACCENTITY).toString() : null;
                        if (CollectionUtils.isNotEmpty(result_type)) {
                            CtmJSONObject jsonObject_type = new CtmJSONObject(result_type.get(0));
                            String type_code = jsonObject_type.get(ICmpConstant.CODE).toString();
                            if (type_code.equals(ICmpConstant.CAPTYPRCODE)) {
                                if (acctyty_type.equals(payMargin.getAccentity())) {
                                    //校验资金组织档案id不能等于单据所属资金组织
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100984"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F715FE04F80003", "对方类型为资金业务对象时，对方档案不允许选择本单位！") /* "对方类型为资金业务对象时，对方档案不允许选择本单位！" */);
                                }
                            }
                        }

                    }
                }

            }
            //todo 校验：是否结算为是时，本对方银行账号不能相等，如果是档案，则校验银行账户档案id不能相等，给出提示“不允许保存，保证金需要结算时，本对方银行账号不能相同”
            Short settleflag = payMargin.getSettleflag();
            if (ObjectUtils.isEmpty(settleflag)) {
                settleflag = (short) 0;
            }
            if (settleflag == 1) {
                //本方银行账户
                String enterprisebankaccount = payMargin.getEnterprisebankaccount();
                EnterpriseBankAcctVO enterpriseBankAcctVO = null;
                if (ObjectUtils.isNotEmpty(enterprisebankaccount)) {
                    enterpriseBankAcctVO = enterpriseBankQueryService.findById(enterprisebankaccount);
                }
                if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100979"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4435A04800001", "未查到本方银行账户信息！") /* "未查到本方银行账户信息！" */);
                }
                String enterprisebankaccountNumber = enterpriseBankAcctVO.getAccount();
                String oppositebankaccount = "";

                switch (oppositetype) {
                    case 1: //客户
                        oppositebankaccount = ValueUtils.isNotEmptyObj(payMargin.getCustomerbankaccount()) ?
                                payMargin.getCustomerbankaccount().toString() : null;
                        break;
                    case 2: //供应商
                        oppositebankaccount = ValueUtils.isNotEmptyObj(payMargin.getSupplierbankaccount()) ?
                                payMargin.getSupplierbankaccount().toString() : null;
                        break;
                    case 3: //其他
                        oppositebankaccount = payMargin.getOppositebankaccount();
                        break;
                    case 4: //内部单位
                        oppositebankaccount = payMargin.getOurbankaccount();
                        break;
                    case 5: //资金业务对象
                        oppositebankaccount = payMargin.getCapBizObjbankaccount();
                        break;
                    default:
                        break;
                }
                if (enterprisebankaccount.equals(oppositebankaccount) || (ObjectUtils.isNotEmpty(enterprisebankaccountNumber) && enterprisebankaccountNumber.equals(oppositebankaccount))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100985"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC827E04F80005", "不允许保存，保证金需要结算时，本对方银行账号不能相同！") /* "不允许保存，保证金需要结算时，本对方银行账号不能相同" */);
                }

            }
            // todo 校验：转换保证金为是时，保证金原始业务号和新保证金原始业务号不能相等，给出提示“转换保证金时，保证金原始业务号和新保证金原始业务号不能相同”。
            Short conversionmarginflag = payMargin.getConversionmarginflag();
            if (conversionmarginflag == 1) {
                String marginbusinessno = payMargin.getMarginbusinessno();
                String newmarginbusinessno = payMargin.getNewmarginbusinessno();
                if (marginbusinessno.equals(newmarginbusinessno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100986"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC841004F80001", "转换保证金时，保证金原始业务号和新保证金原始业务号不能相同！") /* "转换保证金时，保证金原始业务号和新保证金原始业务号不能相同！" */);
                }
            }

            //todo 导入转换保证金为否，录入转换信息，导入后应清空转换信息
            if (conversionmarginflag == 0) {
                payMargin.setConversionamount(null);
                payMargin.setNatconversionamount(null);
                payMargin.setNewmarginbusinessno(null);
                payMargin.setNewmargintype(null);
                payMargin.setNewdept(null);
                payMargin.setNewproject(null);
                payMargin.setConversionmargincode(null);
                payMargin.setNewexpectedretrievaldate(null);
            }

            if (ObjectUtils.isEmpty(payMargin.getVoucherstatus())) {
                payMargin.setVoucherstatus(VoucherStatus.Empty.getValue());
            }

            if (ObjectUtils.isEmpty(payMargin.getVerifystate())) {
                payMargin.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
            }
            //保存提交后重新匹配预算规则，原预算规则预占删除，新规则占预占
            if (payMargin.getVerifystate() != null && payMargin.getVerifystate() == VerifyState.SUBMITED.getValue()) {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    cmpBudgetPaymarginManagerService.reMatchBudget(payMargin);
                    //刷新pubts
                    PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(payMarginNew.getPubts());
                }
            }

            bizObject.remove("pubts");
            bizObject.setEntityStatus(EntityStatus.Update);
            if (bizObject.containsKey("characterDef")) {
                Object characterDefObj = bizObject.get("characterDef");
                // instanceOf会判断null，为null则表达式结果是false
                if (characterDefObj instanceof BizObject) {
                    BizObject characterDefBizObject = (BizObject) characterDefObj;
                    characterDefBizObject.setEntityStatus(EntityStatus.Update);
                }
            }
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, bizObject);
        }
        return new RuleExecuteResult();
    }


}
