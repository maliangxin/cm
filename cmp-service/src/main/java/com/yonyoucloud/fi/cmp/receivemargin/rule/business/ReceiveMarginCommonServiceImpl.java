package com.yonyoucloud.fi.cmp.receivemargin.rule.business;

import com.google.common.base.Strings;
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
import com.yonyoucloud.fi.cmp.budget.CmpBudgetReceivemarginManagerService;
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
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收到保证金保存前规则*
 *
 * @author xuxbo
 * @date 2023/8/3 10:09
 */

@Slf4j
@Component
public class ReceiveMarginCommonServiceImpl extends AbstractCommonRule {

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
    private CmpBudgetReceivemarginManagerService cmpBudgetReceivemarginManagerService;

    @Autowired
    private CmCommonService<Object> cmCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            //和马良沟通的结果，在保存后，如果是是新增的情况，并且是自动情况，并且 原始业务号是否！=单据编号，则重新设置 原始业务号=单据编号
            boolean autoBussinessNo = bizObject.get("autoBusinessNo");
            String code = bizObject.get("code");
            String marginBusinessNo = bizObject.get("marginBusinessNo");
            if(!code.equals(marginBusinessNo) && autoBussinessNo && bizObject.getEntityStatus() == EntityStatus.Insert){
                bizObject.set("marginbusinessno",bizObject.get("code"));
            }

            ReceiveMargin receiveMargin = (ReceiveMargin) bizObject;
            //CM202400472传结算的现金管理单据控制0金额数据不传给结算
            if (!Objects.isNull(receiveMargin.getSettleflag()) && SettleFlagEnum.YES.getValue() == receiveMargin.getSettleflag()
                    && !Objects.isNull(receiveMargin.getMarginamount()) && receiveMargin.getMarginamount().compareTo(BigDecimal.ZERO) == 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E86328005200002", "传结算的单据，保证金金额不可为0！") /* "传结算的单据，保证金金额不可为0！" */);
            }
            //交易类型停用校验
            CmpCommonUtil.checkTradeTypeEnable(receiveMargin.getTradetype());
            //如果是通过保证金过来的  则直接保存
            if(ObjectUtils.isNotEmpty(receiveMargin.get("isConvert")) && receiveMargin.get("isConvert").equals(true)){
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
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, bizObject);
                return new RuleExecuteResult();
            }
            BillDataDto billDataDto = (BillDataDto) getParam(map);
            //导入
            boolean importFlag =  "import".equals(billDataDto.getRequestAction());
            // OpenApi
            boolean openApiFlag = bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true);
            boolean fromApi = billDataDto.getFromApi();
            // 如果是导入进来的
            if (importFlag || openApiFlag || fromApi) {
                // 需要根据资金组织查询本币币种 以及  计算本币金额，并赋值
                // 组织本币
                String natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
                if (StringUtils.isEmpty(natCurrency)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100959"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180807","资金组织[") /* "资金组织[" */ + bizObject.get("accentity_name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180806","]组织本币币种为空!") /* "]组织本币币种为空!" */);
                }
                CurrencyTenantDTO natcurrencyTenantDTO = baseRefRpcService.queryCurrencyById(natCurrency);

                if (natcurrencyTenantDTO != null) {
//                    bizObject.set("natCurrency", natcurrencyTenantDTO.getId());
//                    bizObject.set("natCurrency_name", natcurrencyTenantDTO.getName());
//                    bizObject.set("natCurrency_moneyDigit", natcurrencyTenantDTO.getMoneydigit());
//                    bizObject.set("natCurrency_code", natcurrencyTenantDTO.getCode());
                    receiveMargin.setNatCurrency(natcurrencyTenantDTO.getId());
                }
                //计算本币金额 exchRate
                BigDecimal exchRate = receiveMargin.getExchRate();
                BigDecimal marginamount = receiveMargin.getMarginamount();
                Short moneyDigit = Short.valueOf(natcurrencyTenantDTO.getMoneydigit().toString());
                if (ObjectUtils.isNotEmpty(marginamount)) {
                    //计算natmarginamount
                    //BigDecimal natmarginamount = exchRate.multiply(marginamount);
                    BigDecimal natmarginamount = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(receiveMargin.getExchRateOps(), exchRate, marginamount, null);
                    natmarginamount = new BigDecimal(natmarginamount.toString()).setScale(moneyDigit, BigDecimal.ROUND_HALF_UP);
                    receiveMargin.setNatmarginamount(natmarginamount);
                }

                // 虚拟户是否为空，为空  事项来源默认 现金管理  单据类型默认 支付保证金台账管理 ；不为空 根据虚拟户查询  事项来源和单据类型
                // 虚拟户 不为空时 需要根据校验与当前的资金组织是否一致
                if (ObjectUtils.isEmpty(receiveMargin.getMarginvirtualaccount())) {
                    receiveMargin.setSrcitem(EventSource.Cmpchase.getValue());
                    //获取单据类型
                    BdBillType billType = billTypeQueryService.queryBillTypeId("CM.cmp_receivemargin");
                    receiveMargin.setBilltype(billType.getId());
                } else {
                    String id = receiveMargin.getMarginvirtualaccount().toString();
                    MarginWorkbench marginWorkbench =  MetaDaoHelper.findById(MarginWorkbench.ENTITY_NAME,id);
                    if (ObjectUtils.isNotEmpty(marginWorkbench)) {
                        receiveMargin.setSrcitem(marginWorkbench.getSrcItem());
                        receiveMargin.setBilltype(marginWorkbench.getBillType());
                        if (!receiveMargin.getAccentity().equals(marginWorkbench.getAccentity())){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100960"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227AA79004C00000", "保证金虚拟户的资金组织与当前资金组织不一致！") /* "保证金虚拟户的资金组织与当前资金组织不一致！*/);
                        }
                        //币种 保证金原始业务号
                        receiveMargin.setCurrency(marginWorkbench.getCurrency());
                        receiveMargin.setMarginbusinessno(marginWorkbench.getMarginBusinessNo());
                        // 保证金虚拟户 与 原始业务号都不为空的时候  需要校验两者的一致性
                        if (!marginWorkbench.getMarginBusinessNo().equals(receiveMargin.getMarginbusinessno())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100961"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C64F0005C80007", "保证金虚拟户的原始业务号与当前输入的原始业务号不一致！") /* "保证金虚拟户的原始业务号与当前输入的原始业务号不一致！*/);
                        }
                    }
                }
                // 是否结算 如果是  需要校验必填项：结算方式  结算状态  本方银行账户名称
                if (ObjectUtils.isEmpty(receiveMargin.getSettleflag())) {
                    receiveMargin.setSettleflag((short) 0);
                }
                if (receiveMargin.getSettleflag() == 1) {
                    if (ObjectUtils.isEmpty(receiveMargin.getSettlemode())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100962"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A48BFA05880001", "需结算时，结算方式必填！") /* "需结算时，结算方式必填！" */);
                    }
                    if (ObjectUtils.isEmpty(receiveMargin.getSettlestatus())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100963"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A48CA205D00003", "需结算时，结算状态必填！") /* "需结算时，结算状态必填！" */);
                    }
                    if (ObjectUtils.isEmpty(receiveMargin.getEnterprisebankaccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100964"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A48D0405D00008", "需结算时，本方银行账户必填！") /* "需结算时，本方银行账户必填！ */);
                    }
                }
                //是否转换 交易类型只有为 收到保证金 并且虚拟户有值的时候  才能为是 ；如果为是 需要校验 转换金额必填 本币转换金额赋值 新保证金原始业务号必填 新保证金类型必填
                BdTransType transType = transTypeQueryService.findById(receiveMargin.getTradetype());
                if (ObjectUtils.isEmpty(transType)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100965"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4937605D00005", "未查到交易类型！") /* "未查到交易类型！*/);
                }
                if (receiveMargin.getConversionmarginflag() == 1) {

                    if (!transType.getCode().equals("cmp_receivemargin_return") ) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100966"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19AF72E604D00001", "只有交易类型为退还保证金时，才能进行转换保证金！") /* "只有交易类型为退还保证金时，才能进行转换保证金！*/);
                    }
                    if (ObjectUtils.isEmpty(receiveMargin.getConversionamount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100967"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4965805880006", "转换保证金为是时，转换金额必填！") /* "转换保证金为是时，转换金额必填！*/);
                    }
                    if (ObjectUtils.isEmpty(receiveMargin.getNewmarginbusinessno())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100968"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4972405D00008", "转换保证金为是时，新保证金原始业务号必填！") /* "转换保证金为是时，新保证金原始业务号必填！*/);
                    }
                    if (ObjectUtils.isEmpty(receiveMargin.getNewmargintype())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100969"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19A4975C05D00005", "转换保证金为是时，新保证金类型必填！") /* "转换保证金为是时，新保证金类型必填！*/);
                    }

                    //计算natmarginamount
                    BigDecimal natconversionamount = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(receiveMargin.getExchRateOps(), receiveMargin.getExchRate(), receiveMargin.getConversionamount(), null);

                    natconversionamount = new BigDecimal(natconversionamount.toString()).setScale(moneyDigit, BigDecimal.ROUND_HALF_UP);
                    receiveMargin.setNatconversionamount(natconversionamount);
                }

                // 是否自动退还 如果为是  需要校验自定退还是否结算必填 自动退还日期必填
                if (ObjectUtils.isEmpty(receiveMargin.getAutorefundflag())) {
                    receiveMargin.setAutorefundflag((short) 0);
                }
                if (receiveMargin.getAutorefundflag() == 1) {
                    if (ObjectUtils.isEmpty(receiveMargin.getRefundsettleflag())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100970"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19AF73CC04D00007", "自动退还为是时，退还是否结算必填！") /* "自动退还为是时，退还是否结算必填！*/);
                    }
                    if (ObjectUtils.isEmpty(receiveMargin.getRefunddate())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100971"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19AF743604D00001", "自动退还为是时，退还日期必填！") /* "自动退还为是时，退还日期必填！*/);
                    }
                }

                // 导入退还保证金时，需校验是否有虚拟账户，没有虚拟账户时应不可导入成功
                if (transType.getCode().equals("cmp_receivemargin_return") && ObjectUtils.isEmpty(receiveMargin.getMarginvirtualaccount())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100972"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B456AC04800000", "交易类型为退还保证金时，保证金虚拟户不可为空！") /* "交易类型为退还保证金时，保证金虚拟户不可为空！*/);
                }
                //导入时需要校验收付类型的取值
                //   根据交易类型 退还  付款       收到   收款
                if (transType.getCode().equals("cmp_receivemargin_return")) {
                    receiveMargin.setPaymenttype(PaymentType.FundPayment.getValue());
                } else {
                    receiveMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                }

                String currency;
                if (ObjectUtils.isNotEmpty(receiveMargin.getCurrency())) {
                    currency = receiveMargin.getCurrency();
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100823"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00057", "币种不可为空!") /* "币种不可为空!" */);
                }
                //todo 导入后对方开户网点、对方银行类别需要根据账户自动带出值
                if (ObjectUtils.isNotEmpty(receiveMargin.getOppositetype())) {
                    Short oppositetype = receiveMargin.getOppositetype();
                    String oppositebankaccount = "";
                    switch (oppositetype) {
                        case 1: //客户
//                            oppositebankaccount = com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(receiveMargin.getCustomerbankaccount()) ?
//                                    receiveMargin.getCustomerbankaccount().toString() : null;
                            oppositebankaccount = bizObject.get("customerbankaccount_bankAccount");
                            CustomerQueryService customerQueryService = AppContext.getBean(CustomerQueryService.class);
                            AgentFinancialDTO bankAccount = customerQueryService.getCustomerAccountByAccountNo(oppositebankaccount);
                            if (ObjectUtils.isNotEmpty(bankAccount)) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100973"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3D3BA04D00000", "未查到客户银行账户信息！") /* "未查到客户银行账户信息！*/);
                                //校验币种
                                if (!currency.equals(bankAccount.getCurrency())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100974"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4219A04800009", "客户银行账户币种与当前币种不一致！") /* "客户银行账户币种与当前币种不一致！*/);
                                }
                                // 赋值开户网点
                                receiveMargin.setOppositebankNumber(bankAccount.getOpenBank());
                                // 赋值银行类别
                                receiveMargin.setOppositebankType(bankAccount.getBank());
                            }
                            break;
                        case 2: //供应商
                            oppositebankaccount = ValueUtils.isNotEmptyObj(receiveMargin.getSupplierbankaccount()) ?
                                    receiveMargin.getSupplierbankaccount().toString() : null;
                            VendorBankVO supplierbankaccount = vendorQueryService.getVendorBanksByAccountId(oppositebankaccount);
                            if (ObjectUtils.isNotEmpty(supplierbankaccount)) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100975"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3D99404D00002", "未查到供应商银行账户信息！") /* "未查到供应商银行账户信息！*/);
                                //校验币种
                                if (!currency.equals(supplierbankaccount.getCurrency())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100976"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4221A04380007", "供应商银行账户币种与当前币种不一致！") /* "供应商银行账户币种与当前币种不一致！*/);
                                }
                                // 赋值开户网点
                                receiveMargin.setOppositebankNumber(supplierbankaccount.getOpenaccountbank());
                                // 赋值银行类别
                                receiveMargin.setOppositebankType(supplierbankaccount.getBank());
                            }
                            break;
                        case 3: //其他
                            oppositebankaccount = null;
                            break;
                        case 4: //内部单位
                            oppositebankaccount = receiveMargin.getOurbankaccount();
                            // 内部单位银行账号判空
                            EnterpriseBankAcctVO enterpriseBankAcctVO = null;
                            if(!Strings.isNullOrEmpty(oppositebankaccount)){
                                enterpriseBankAcctVO = enterpriseBankQueryService.findById(oppositebankaccount);
                            }
//                            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(oppositebankaccount);
                            if (!ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100977"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B3E10005A80007", "未查到内部单位银行账户信息！") /* "未查到内部单位银行账户信息！*/);
                                //校验币种
                                List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
                                List <String> currencyid = new ArrayList<>();
                                for (BankAcctCurrencyVO bankAcctCurrencyVO :currencyList) {
                                    currencyid.add(bankAcctCurrencyVO.getCurrency());
                                }
                                if (!currencyid.contains(currency)) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100976"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4221A04380007", "内部单位银行账户币种与当前币种不一致！") /* "内部单位银行账户币种与当前币种不一致！*/);
                                }
                                // 赋值开户网点 bankNumber
                                receiveMargin.setOppositebankNumber(enterpriseBankAcctVO.getBankNumber());
                                // 赋值银行类别bank
                                receiveMargin.setOppositebankType(enterpriseBankAcctVO.getBank());
                            }
                            break;
                        case 5: //资金业务对象
//                            oppositebankaccount = receiveMargin.getCapBizObjbankaccount();
                            oppositebankaccount = bizObject.get("capBizObjbankaccount_bankaccount");
                            String capBizObj = receiveMargin.getCapBizObj();
                            if (ObjectUtils.isNotEmpty(oppositebankaccount) && ObjectUtils.isNotEmpty(capBizObj)) {
                                //查询资金业务对象的银行信息
                                CtmJSONObject fundBusinObjArchivesItem = fundBusinessObjectQueryService.queryFundBusinessObjectDataById(capBizObj,oppositebankaccount,null);
                                if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem)) {
                                    //校验币种
                                    if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem.get("currency"))) {
                                        if (! currency.equals(fundBusinObjArchivesItem.get("currency"))) {
                                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100978"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4405A04380002", "资金业务对象银行账户币种与当前币种不一致！") /* "资金业务对象银行账户币种与当前币种不一致！*/);
                                        }
                                    }
                                    // 赋值开户网点
                                    if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem.get("bopenaccountbankid"))) {
                                        receiveMargin.setOppositebankNumber(fundBusinObjArchivesItem.get("bopenaccountbankid").toString());
                                    }

                                    // 赋值银行类别
                                    if (ObjectUtils.isNotEmpty(fundBusinObjArchivesItem.get("bbankid"))) {
                                        receiveMargin.setOppositebankType(fundBusinObjArchivesItem.get("bbankid").toString());
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }

                }

                //todo  本方银行账户要根据币种过滤
                if (ObjectUtils.isNotEmpty(receiveMargin.getEnterprisebankaccount())) {
                    EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(receiveMargin.getEnterprisebankaccount());
                    if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100979"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4435A04800001", "未查到本方银行账户信息！") /* "未查到本方银行账户信息！*/);
                    } else {
                        //校验币种
                        List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
                        List <String> currencyid = new ArrayList<>();
                        for (BankAcctCurrencyVO bankAcctCurrencyVO :currencyList) {
                            currencyid.add(bankAcctCurrencyVO.getCurrency());
                        }
                        if (!currencyid.contains(currency)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100980"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B443D204380008", "本方银行账户币种与当前币种不一致！") /* "本方银行账户币种与当前币种不一致！*/);
                        }

                    }

                }
                //结算状态 只能导入待结算 和 已结算补单
                if(ObjectUtils.isNotEmpty(receiveMargin.getSettlestatus())){
                    if (!(receiveMargin.getSettlestatus().equals(FundSettleStatus.WaitSettle.getValue()) || receiveMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue()))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100981"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C6570A05C80006", "结算状态只支持导入待结算和已结算补单！") /* "结算状态只支持导入待结算和已结算补单！*/);
                    }
                }
                //付款结算模式
                Short paymentsettlemode = receiveMargin.getPaymentsettlemode();
                if (ObjectUtils.isEmpty(paymentsettlemode)) {
                    //如果付款结算模式为空，则赋默认值为 主动结算
                    receiveMargin.setPaymentsettlemode(PaymentSettlemode.ActiveSettlement.getValue());
                }
                // 校验交易类型是否属于当前业务单据
                //不为空需要判断此对象是否是付款单的
                List<Map<String, Object>> transTypeList = cmCommonService.queryTransTypesByForm_ids(ICmpConstant.CM_CMP_RECEIVEMARGIN);
                List<Object> codeList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(transTypeList)){
                    transTypeList.stream().forEach(transtypemap->{
                        if (null != transtypemap){
                            List<Object> codes = transtypemap.entrySet().stream().filter(entry -> entry.getKey().equals("code")).map(Map.Entry::getValue).collect(Collectors.toList());
                            codeList.addAll(codes);
                        }
                    });
                }
                String tradetype_code= bizObject.get("tradetype_code");
                if (null == codeList || !codeList.contains(tradetype_code) ){
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F98FAAC04B80009", "填写的交易类型不属于是收到保证金的交易类型，请在重新录入！") /* "填写的交易类型不属于是收到保证金的交易类型，请在重新录入！" */);
                }


            }

            //CZFW-455151: 自动退还且自动退还单据需要结算时，本方银行账户、对方名称必须有值
            if (receiveMargin.getAutorefundflag() == 1 && receiveMargin.getRefundsettleflag() == 1) {
                boolean isOppositeNameEmpty = false;
                if (receiveMargin.getOppositetype() == 1 && ObjectUtils.isEmpty(receiveMargin.getCustomer())) {
                    //客户
                    isOppositeNameEmpty = true;
                } else if (receiveMargin.getOppositetype() == 2 && ObjectUtils.isEmpty(receiveMargin.getSupplier())) {
                    //供应商
                    isOppositeNameEmpty = true;
                } else if (receiveMargin.getOppositetype() == 3 && ObjectUtils.isEmpty(receiveMargin.getOppositename())) {
                    //其他
                    isOppositeNameEmpty = true;
                } else if (receiveMargin.getOppositetype() == 4 && ObjectUtils.isEmpty(receiveMargin.getOurname())) {
                    //内部单位
                    isOppositeNameEmpty = true;
                } else if (receiveMargin.getOppositetype() == 5 && ObjectUtils.isEmpty(receiveMargin.getCapBizObj())) {
                    //资金业务对象
                    isOppositeNameEmpty = true;
                }
                if (StringUtils.isEmpty(receiveMargin.getEnterprisebankaccount()) || isOppositeNameEmpty) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103036"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C838BE04480004", "自动退还且自动退还单据需要结算时，本方银行账户、对方名称必须有值") /* "自动退还且自动退还单据需要结算时，本方银行账户、对方名称必须有值" */);
                }
            }

            CtmJSONObject params = new CtmJSONObject();
            params.put(ICmpConstant.RECMARGIN, receiveMargin);
            try {
                if (receiveMargin.getEntityStatus() == EntityStatus.Insert) {
                    //调用工作台保存接口，接口判断是否存在虚拟户
                    String marginvirtualaccount = marginWorkbenchService.recMarginWorkbenchSave(params);
                    receiveMargin.setMarginvirtualaccount(Long.valueOf(marginvirtualaccount));
                } else if (receiveMargin.getEntityStatus() == EntityStatus.Update) {
                    ReceiveMargin receiveMargin_old = receiveMargin.get(ReceiveMarginBeforeSaveRule.OLD_RECEIVEMARGIN);
                    //保存前金额
                    BigDecimal marginamount_old = receiveMargin_old.getMarginamount();
                    //保存前本币金额
                    BigDecimal natmarginamount_old = receiveMargin_old.getNatmarginamount();
                    params.put(ICmpConstant.DBMARGINAMOUNT, marginamount_old);
                    params.put(ICmpConstant.DBNATMARGINAMOUNT, natmarginamount_old);
                    //保存前转换金额
                    if (receiveMargin_old.getConversionmarginflag() == 1) {
                        BigDecimal conversionamount_old = receiveMargin_old.getConversionamount();
                        BigDecimal natconversionamount_old = receiveMargin_old.getNatconversionamount();
                        params.put(ICmpConstant.DBCONVERSIONAMOUNT, conversionamount_old);
                        params.put(ICmpConstant.DBNATCONVERSIONAMOUNT, natconversionamount_old);
                    }
                    String marginvirtualaccount = marginWorkbenchService.recMarginWorkbenchSave(params);
                    receiveMargin.setMarginvirtualaccount(Long.valueOf(marginvirtualaccount));
                }

            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100983"),e.getMessage());
            }

            // 校验 如果单据类型为空时 赋值为：支付保证金台账管理
            if (ValueUtils.isEmpty(receiveMargin.getBilltype())) {
                //根据 forimid 查询单据类型
                BdBillType bdBillType = baseRefRpcService.queryBillTypeByFormId(ICmpConstant.CM_CMP_RECEIVEMARGIN);
                String billTypeId = bdBillType.getId();
                receiveMargin.setBilltype(billTypeId);
            }

            //对方类型
            Short oppositetype = receiveMargin.getOppositetype();
            if (oppositetype == MarginOppositeType.CapBizObj.getValue()) {
                //获取资金业务对象id
                String capBizObj = receiveMargin.getCapBizObj();
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
                        if (CollectionUtils.isNotEmpty(result_type)) {
                            CtmJSONObject jsonObject_type = new CtmJSONObject(result_type.get(0));
                            String type_code = jsonObject_type.get(ICmpConstant.CODE).toString();
                            if (type_code.equals(ICmpConstant.CAPTYPRCODE)) {
                                String acctyty_type = jsonObject.get(ICmpConstant.ACCENTITY).toString();
                                if (acctyty_type.equals(receiveMargin.getAccentity())) {
                                    //校验资金组织档案id不能等于单据所属资金组织
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100984"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F715FE04F80003", "对方类型为资金业务对象时，对方档案不允许选择本单位！") /* "对方类型为资金业务对象时，对方档案不允许选择本单位！" */);
                                }
                            }
                        }

                    }
                }

            }


            //todo 校验：是否结算为是时，本对方银行账号不能相等，如果是档案，则校验银行账户档案id不能相等，给出提示“不允许保存，保证金需要结算时，本对方银行账号不能相同”
            Short settleflag = receiveMargin.getSettleflag();
            if (ObjectUtils.isEmpty(settleflag)) {
                settleflag = (short) 0;
            }
            if (settleflag == 1) {
                //本方银行账户
                String enterprisebankaccount = receiveMargin.getEnterprisebankaccount();
                EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(enterprisebankaccount);
                if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100979"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B4435A04800001", "未查到本方银行账户信息！") /* "未查到本方银行账户信息！" */);
                }
                String enterprisebankaccountNumber = enterpriseBankAcctVO.getAccount();
                String oppositebankaccount = "";

                switch (oppositetype) {
                    case 1: //客户
                        oppositebankaccount = ValueUtils.isNotEmptyObj(receiveMargin.getCustomerbankaccount()) ?
                                receiveMargin.getCustomerbankaccount().toString() : null;
                        break;
                    case 2: //供应商
                        oppositebankaccount = ValueUtils.isNotEmptyObj(receiveMargin.getSupplierbankaccount()) ?
                                receiveMargin.getSupplierbankaccount().toString() : null;
                        break;
                    case 3: //其他
                        oppositebankaccount = receiveMargin.getOppositebankaccount();
                        break;
                    case 4: //内部单位
                        oppositebankaccount = receiveMargin.getOurbankaccount();
                        break;
                    case 5: //资金业务对象
                        oppositebankaccount = receiveMargin.getCapBizObjbankaccount();
                        break;
                    default:
                        break;
                }
                if (enterprisebankaccount.equals(oppositebankaccount) || (ObjectUtils.isNotEmpty(enterprisebankaccountNumber) && enterprisebankaccountNumber.equals(oppositebankaccount))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100985"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC827E04F80005", "不允许保存，保证金需要结算时，本对方银行账号不能相同！") /* "不允许保存，保证金需要结算时，本对方银行账号不能相同" */);
                }

            }
            // todo 校验：转换保证金为是时，保证金原始业务号和新保证金原始业务号不能相等，给出提示“转换保证金时，保证金原始业务号和新保证金原始业务号不能相同”。
            Short conversionmarginflag = receiveMargin.getConversionmarginflag();
            if (conversionmarginflag == 1) {
                String marginbusinessno = receiveMargin.getMarginbusinessno();
                String newmarginbusinessno = receiveMargin.getNewmarginbusinessno();
                if (marginbusinessno.equals(newmarginbusinessno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100986"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC841004F80001", "转换保证金时，保证金原始业务号和新保证金原始业务号不能相同！") /* "转换保证金时，保证金原始业务号和新保证金原始业务号不能相同！" */);
                }
            }

            //todo 导入转换保证金为否，录入转换信息，导入后应清空转换信息
            if (conversionmarginflag == 0) {
                receiveMargin.setConversionamount(null);
                receiveMargin.setNatconversionamount(null);
                receiveMargin.setNewmarginbusinessno(null);
                receiveMargin.setNewmargintype(null);
                receiveMargin.setNewdept(null);
                receiveMargin.setNewproject(null);
                receiveMargin.setConversionmargincode(null);
                receiveMargin.setNewlatestreturndate(null);
            }

            if (ObjectUtils.isEmpty(receiveMargin.getVoucherstatus())) {
                receiveMargin.setVoucherstatus(VoucherStatus.Empty.getValue());
            }

            if (ObjectUtils.isEmpty(receiveMargin.getVerifystate())) {
                receiveMargin.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
            }
            //保存提交后重新匹配预算规则，原预算规则预占删除，新规则占预占
            if (receiveMargin.getVerifystate() != null && receiveMargin.getVerifystate() == VerifyState.SUBMITED.getValue()) {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                    cmpBudgetReceivemarginManagerService.reMatchBudget(receiveMargin);
                    //刷新pubts
                    ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(receiveMarginNew.getPubts());
                }
            }

            bizObject.remove("pubts");
            bizObject.setEntityStatus(EntityStatus.Update);
            if (bizObject.containsKey("characterDef")) {
                Object characterDefObj = bizObject.get("characterDef");
                // instanceOf会判断null，为null则表达式结果是false
                if (characterDefObj instanceof BizObject) {
                    BizObject characterDefBizObject = bizObject.get("characterDef");
                    characterDefBizObject.setEntityStatus(EntityStatus.Update);
                }
            }
            MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME,bizObject);
        }
        return new RuleExecuteResult();
    }

}
