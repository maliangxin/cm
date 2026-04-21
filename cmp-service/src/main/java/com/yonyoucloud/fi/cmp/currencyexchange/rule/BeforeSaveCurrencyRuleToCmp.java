package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.DeliverTime;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillService;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.yonyoucloud.fi.cmp.cmpentity.DeliveryType.DirectDelivery;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ONE;
@Slf4j
@Component
public class BeforeSaveCurrencyRuleToCmp extends AbstractCommonRule {

    @Autowired
    OpenApiService openApiService;
    @Autowired
    ReceiveBillService receiveBillService;
    @Autowired
    PaymentService paymentService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    TransTypeQueryService transTypeQueryService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;
    // 财务公司的银行类别编码
    private final static String FINANCE_COMPANY_BANK_TYPE_CODE = "system-002";


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        billContext.getBillnum();
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100008"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053B","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            BizObject bizObject = (BizObject) bills.get(0);
            //结算方式
            if (bizObject.get("settlemode") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100009"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180523","结算方式不能为空！") /* "结算方式不能为空！" */);
            }
            //交易类型停用校验
            CmpCommonUtil.checkTradeTypeEnable(bizObject.get("tradetype"));
            SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
            settleMethodQueryParam.setId(bizObject.get("settlemode"));
            settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
            settleMethodQueryParam.setTenantId(AppContext.getTenantId());
            List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
            if (!CollectionUtils.isEmpty(dataList)) {
                SettleMethodModel settlemodeMap = dataList.get(0);
                if (settlemodeMap.getServiceAttr().equals(0)) {
                    if (bizObject.get("purchasebankaccount") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100010"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180526","结算方式业务属性为银行业务，请录入买入银行账户！") /* "结算方式业务属性为银行业务，请录入买入银行账户！" */);
                    }
                    if (bizObject.get("sellbankaccount") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100011"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180528","结算方式业务属性为银行业务，请录入卖出银行账户！") /* "结算方式业务属性为银行业务，请录入卖出银行账户！" */);
                    }
                    bizObject.set("purchasecashaccount", null);
                    bizObject.set("sellcashaccount", null);
                    bizObject.set("commissioncashaccount", null);
                    //当 交割方式=直联交割，保存时，校验 买入银行账户、卖出银行账户是否为直联账户（根据银企连账号节点的是否直联）
                    if (null != bizObject.get("deliveryType") && bizObject.getShort("deliveryType").compareTo(DirectDelivery.getValue()) == 0) {
                        if (!getOpenFlag(bizObject.get("purchasebankaccount"))) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100012"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418052A","买入银行账户为非直联账户，不可选择直联交割方式") /* "买入银行账户为非直联账户，不可选择直联交割方式" */);
                        }
                        if (!getOpenFlag(bizObject.get("sellbankaccount"))) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100013"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418052C","卖出银行账户为非直联账户，不可选择直联交割方式") /* "卖出银行账户为非直联账户，不可选择直联交割方式" */);
                        }
                    }
                } else if (settlemodeMap.getServiceAttr().equals(1)) {
                    if (bizObject.get("purchasecashaccount") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100014"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418052D","结算方式业务属性为现金业务，请录入买入现金账户！") /* "结算方式业务属性为现金业务，请录入买入现金账户！" */);
                    }
                    if (bizObject.get("sellcashaccount") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100015"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418052E","结算方式业务属性为现金业务，请录入卖出现金账户！") /* "结算方式业务属性为现金业务，请录入卖出现金账户！" */);
                    }
                    if (((Short)bizObject.get("deliveryType")) == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100016"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_180E028A0578021B","结算方式为现金业务不能选择直联交割") /* "结算方式为现金业务不能选择直联交割" */);
                    }
                    bizObject.set("purchasebankaccount", null);
                    bizObject.set("sellbankaccount", null);
                    bizObject.set("commissionbankaccount", null);
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100017"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180530","结算方式业务属性只能为银行业务与现金业务！") /* "结算方式业务属性只能为银行业务与现金业务！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100018"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180532","未查询到当前结算方式，请刷新后重试！") /* "未查询到当前结算方式，请刷新后重试！" */);
            }

            if (bizObject.get("purchaseamount") != null && ((BigDecimal) bizObject.get("purchaseamount")).compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100019"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180534","买入金额不能小于等于0") /* "买入金额不能小于等于0" */);
            }
            if (bizObject.get("sellamount") != null && ((BigDecimal) bizObject.get("sellamount")).compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100020"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180536","卖出金额不能小于等于0") /* "卖出金额不能小于等于0" */);
            }
            if (bizObject.get("commissionamount") != null && ((BigDecimal) bizObject.get("commissionamount")).compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100216"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180538","手续费金额不能小于等于0") /* "手续费金额不能小于等于0" */);
            }
            //中广核去除对买入卖出汇率的校验，公有云不做修改
            if (bizObject.get("purchaserate") != null && ((BigDecimal) bizObject.get("purchaserate")).compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100021"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180539","买入汇率不能小于等于0") /* "买入汇率不能小于等于0" */);
            }
            if (bizObject.get("sellrate") != null && ((BigDecimal) bizObject.get("sellrate")).compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100022"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053C","卖出汇率不能小于等于0") /* "卖出汇率不能小于等于0" */);
            }
            if (bizObject.get("commissionrate") != null && ((BigDecimal) bizObject.get("commissionrate")).compareTo(BigDecimal.ZERO) <= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100217"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180522","手续费汇率不能小于等于0") /* "手续费汇率不能小于等于0" */);
            }
            if (bizObject.get("commissionamount") != null && bizObject.get("commissionrate") == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FD72D9004700005", "手续费汇率不能为空！") /* "手续费汇率不能为空！" */);
            }
            if (bizObject.get("billtype") == null) {
                bizObject.set("billtype", EventType.CurrencyExchangeBill.getValue());
            }
            bizObject.set("billstatus", BillStatus.Created.getValue());
            bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizObject.set("settlestatus", DeliveryStatus.todoDelivery.getValue());
            bizObject.put("creatorId", AppContext.getCurrentUser().getId());
            // TODO 导入数据特殊处理
            BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
            //导入
            boolean importFlag =  "import".equals(billDataDto.getRequestAction());
            // OpenApi
            boolean openApiFlag = bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true);
            boolean fromApi = billDataDto.getFromApi();
            if (importFlag || openApiFlag || fromApi) {
                if (Objects.isNull(bizObject.get("verifystate"))) {
                    bizObject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
                }
                if (Objects.isNull(bizObject.get("voucherstatus"))) {
                    bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
                }
                // 此处专门处理导入数据，没有flag值的问题
                BdTransType bdTransType = transTypeQueryService.findById(bizObject.get("tradetype"));
                String flagStr = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson()).getString("transferType_wbdh");
                switch (flagStr) {
                    case "buyin":
                        bizObject.set("flag", 0);
                        break;
                    case "sellout":
                        bizObject.set("flag", 1);
                        break;
                    case "bs":
                        bizObject.set("flag", 2);
                        break;
                    default:
                        break;
                }
                // 会计主体默认币种
                String currencyOrg = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get("accentity"));
                CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currencyOrg);
                // 计算本币金额
                //中广核去除对买入卖出汇率的校验,公有云不做修改
                BigDecimal purchaseamount = ((BigDecimal) bizObject.get("purchaseamount")).multiply(bizObject.get("purchaserate"));
                BigDecimal sellloaclamount = ((BigDecimal) bizObject.get("sellamount")).multiply(bizObject.get("sellrate"));
                BigDecimal exchangeloss = ((BigDecimal)bizObject.get("sellloaclamount")).subtract(bizObject.get("purchaselocalamount"));
                bizObject.put("purchaselocalamount", purchaseamount.setScale(currencyDTO.getMoneydigit(), currencyDTO.getMoneyrount()));
                bizObject.put("sellloaclamount", sellloaclamount.setScale(currencyDTO.getMoneydigit(), currencyDTO.getMoneyrount()));
                bizObject.put("exchangeloss", exchangeloss.setScale(currencyDTO.getMoneydigit(), currencyDTO.getMoneyrount()));
                if (bizObject.get("commissionamount") != null) {
                    bizObject.put("commissionlocalamount",
                            ((BigDecimal) bizObject.get("commissionamount")).multiply(bizObject.get("commissionrate")).setScale(currencyDTO.getMoneydigit(), currencyDTO.getMoneyrount()));
                }
                // 直连交割时，不需要手续费相关字段
                if (((Short)bizObject.get("deliveryType")) == DeliveryType.DirectDelivery.getValue()) {
                    bizObject.set("commissionbankaccount", null);
                    bizObject.set("commissionCurrency", null);
                    bizObject.set("commissionamount", null);
                    bizObject.set("commissionrate", null);
                    bizObject.set("commissionlocalamount", null);
                }

                //CZFW-440731【现金管理-回单】货币兑换，通过Excel导入的数据，没有给‘原币’字段赋值，导致单据生成凭证时，取不到字段值（手动新增单据正常）
                bizObject.set("natCurrency", currencyOrg);
                boolean containFlag = false;
                // TODO 以下校验需要等需求输出个逻辑在优化吧 看下是否需要判断结算方式或交割方式 或者挪到上面的账户判空里面？
                // 卖出币种与卖出银行账户，匹配性校验
                if (bizObject.get("sellbankaccount") != null) {
                    EnterpriseBankAcctVO sellBankAccount = enterpriseBankQueryService.findById(bizObject.get("sellbankaccount"));
                    List<BankAcctCurrencyVO> sellBankAccountCurrencyList = sellBankAccount.getCurrencyList();
                    containFlag = false;
                    for (BankAcctCurrencyVO bankAcctCurrencyVO : sellBankAccountCurrencyList) {
                        if (bankAcctCurrencyVO.getCurrency().equals(bizObject.get("sellCurrency"))) {
                            containFlag = true;
                        }
                    }
                    if (!containFlag) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100023"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B80020", "卖出银行账户与卖出币种不匹配") /* "卖出银行账户与卖出币种不匹配" */);
                    }
                }
                // 买入币种，买入银行账户，匹配性校验
                if (bizObject.get("purchasebankaccount") != null) {
                    EnterpriseBankAcctVO purchasebankaccount = enterpriseBankQueryService.findById(bizObject.get("purchasebankaccount"));
                    List<BankAcctCurrencyVO> purchaseBankAccountCurrencyList = purchasebankaccount.getCurrencyList();
                    containFlag = false;
                    for (BankAcctCurrencyVO bankAcctCurrencyVO : purchaseBankAccountCurrencyList) {
                        if (bankAcctCurrencyVO.getCurrency().equals(bizObject.get("purchaseCurrency"))) {
                            containFlag = true;
                        }
                    }
                    if (!containFlag) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100024"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001E", "买入银行账户与买入币种不匹配") /* "买入银行账户与买入币种不匹配" */);
                    }
                }
                // 直连交割，不用校验手续费
                if (((Short)bizObject.get("deliveryType")) != DeliveryType.DirectDelivery.getValue() && bizObject.get("commissionbankaccount") != null) {
                    // 手续费币种，手续费银行账户，匹配性校验
                    EnterpriseBankAcctVO commissionBankAccount = enterpriseBankQueryService.findById(bizObject.get("commissionbankaccount"));
                    List<BankAcctCurrencyVO> commissionBankAccountCurrencyList = commissionBankAccount.getCurrencyList();
                    containFlag = false;
                    for (BankAcctCurrencyVO bankAcctCurrencyVO : commissionBankAccountCurrencyList) {
                        if (bankAcctCurrencyVO.getCurrency().equals(bizObject.get("commissionCurrency"))) {
                            containFlag = true;
                        }
                    }
                    if (!containFlag) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100218"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892F40E05B8000B","手续费银行账户与手续费币种不匹配") /* "手续费银行账户与手续费币种不匹配" */);
                    }
                }
                // 根据买入，卖出方向，确定会计主体本位币币种，校验对应卖出币种，卖出银行账户-币种，是否一致
                // -- 2024-01-16 weizhipeng 增加一块逻辑，若存在地区币种，则判断买入、卖出币种与地区币种的关系，否则保持原有逻辑 --
                String currId = bizObject.get("regionCurrency");
                if (!StringUtils.isEmpty(currId)){
                    currencyOrg = currId;
                }
                if ((int)bizObject.get("flag") == 0) {
                    // 买入外汇，卖出银行账户币种为本位币
                    if (!currencyOrg.equals(bizObject.get("sellCurrency"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100025"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001F","卖出币种错误") /* "卖出币种错误" */);
                    }
                    /*if (currencyOrg.equals(bizObject.get("purchaseCurrency"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100026"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001D","买入币种错误") *//* "买入币种错误" *//*);
                    }*/
                    // 保证金账户，条件赋值
                    if (bizObject.get("marginAccountFlag") != null) {
                        if ((Short)bizObject.get("marginAccountFlag") == 0) {
                            bizObject.set("depositAccountNo", bizObject.get("purchasebankaccount"));
                            bizObject.set("depositCurrency", bizObject.get("purchaseCurrency"));
                        } else if ((Short)bizObject.get("marginAccountFlag") == 1) {
                            bizObject.set("depositAccountNo", bizObject.get("sellbankaccount"));
                            bizObject.set("depositCurrency", bizObject.get("sellCurrency"));
                        }
                    }
                } else if ((int) bizObject.get("flag") == 1) {
                    // 卖出外汇，买入银行账户币种为本位币
                    if (!currencyOrg.equals(bizObject.get("purchaseCurrency"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100026"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001D","买入币种错误") /* "买入币种错误" */);
                    }
                    /*if (currencyOrg.equals(bizObject.get("sellCurrency"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100025"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001F","卖出币种错误") *//* "卖出币种错误" *//*);
                    }*/
                    // 保证金账户，条件赋值
                    if (bizObject.get("marginAccountFlag") != null) {
                        if ((Short)bizObject.get("marginAccountFlag") == 1) {
                            bizObject.set("depositAccountNo", bizObject.get("purchasebankaccount"));
                            bizObject.set("depositCurrency", bizObject.get("purchaseCurrency"));
                        } else if ((Short)bizObject.get("marginAccountFlag") == 0) {
                            bizObject.set("depositAccountNo", bizObject.get("sellbankaccount"));
                            bizObject.set("depositCurrency", bizObject.get("sellCurrency"));
                        }
                    }
                } else if ((int) bizObject.get("flag") == 2) {
                    // 外币兑换
                    if (currencyOrg.equals(bizObject.get("purchaseCurrency"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100026"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001D","买入币种错误") /* "买入币种错误" */);
                    }
                    if (currencyOrg.equals(bizObject.get("sellCurrency"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100025"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1892419205B8001F","卖出币种错误") /* "卖出币种错误" */);
                    }
                    // 保证金账户，条件赋值
                    if (bizObject.get("marginAccountFlag") != null) {
                        if ((Short)bizObject.get("marginAccountFlag") == 0) {
                            bizObject.set("depositAccountNo", bizObject.get("purchasebankaccount"));
                            bizObject.set("depositCurrency", bizObject.get("purchaseCurrency"));
                        } else if ((Short)bizObject.get("marginAccountFlag") == 1) {
                            bizObject.set("depositAccountNo", bizObject.get("sellbankaccount"));
                            bizObject.set("depositCurrency", bizObject.get("sellCurrency"));
                        }
                    }
                }
                //交易编码 transactionCode_code 卖出=结汇=1=SFE            买入=售汇=0=BFE=外币兑换
                if (!Objects.isNull(bizObject.get("transactionCode_code")) && !Objects.isNull(bizObject.get("flag"))) {
                    List<BizObject> tradecode = QueryBaseDocUtils.getTradeCodeByCodeForCurrency(bizObject.get("transactionCode_code"),
                            Short.parseShort(bizObject.get("flag").toString()) != Bsflag.Exchange.getValue() ?
                                    Short.parseShort(bizObject.get("flag").toString()) : Bsflag.Buy.getValue());
                    if (CollectionUtils.isNotEmpty(tradecode)) {
                        bizObject.set("transactionCode", tradecode.get(0).getId());
                    }
                }
            }
            bizObject.set("exchangetype", bizObject.get("flag"));
            /**
             * 直连交割时，买入或卖出银行账户，银行类别为财务公司，根据交易类型买入或卖出，校验来源代码和用途代码必输
             */
            boolean sourceCodeFlag = false;
            if ((Short)bizObject.get("deliveryType") == DeliveryType.DirectDelivery.getValue()) {
                // 默认不校验，财务公司才校验，买或卖银行账户有一个银行类别为财务公司就进行校验
                if (bizObject.get("purchasebankaccount") != null) {
                    EnterpriseBankAcctVO purchasebankaccount = enterpriseBankQueryService.findById(bizObject.get("purchasebankaccount"));
                    if (purchasebankaccount != null) {
                        String bankTypeId = purchasebankaccount.getBank();
                        BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(bankTypeId);
                        if (FINANCE_COMPANY_BANK_TYPE_CODE.equals(bankVO.getCode())) {
                            sourceCodeFlag = true;
                        }
                    }
                }
                if (bizObject.get("sellbankaccount") != null) {
                    EnterpriseBankAcctVO sellBankAccount = enterpriseBankQueryService.findById(bizObject.get("sellbankaccount"));
                    if (sellBankAccount != null) {
                        String bankTypeId = sellBankAccount.getBank();
                        BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(bankTypeId);
                        if (FINANCE_COMPANY_BANK_TYPE_CODE.equals(bankVO.getCode())) {
                            sourceCodeFlag = true;
                        }
                    }
                }
                if (sourceCodeFlag) {
                    if (0 == bizObject.getInteger("flag")) {
                        // 买入，用途代码不能为空
                        if (bizObject.get("purposecode") == null) {
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A8FA83604080012","用途代码不能为空") /* "用途代码不能为空" */);
                        }
                    } else if (1 == bizObject.getInteger("flag")) {
                        // 卖出，来源代码不能为空
                        if (bizObject.get("sourcecode") == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100034"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A8FA83604080013","来源代码不能为空") /* "来源代码不能为空" */);
                        }
                    }
                }
            }
            if (bizObject.get("srcitem") == null) {
                bizObject.set(IBillConst.SRCITEM, EventSource.Cmpchase.getValue());
            }
            if (!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.getEntityStatus().name().equals("Insert")) {
                bizObject.set("srcitem", EventSource.Cmpchase.getValue());
            }
            if (!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.get("settlemode") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100009"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180523","结算方式不能为空！") /* "结算方式不能为空！" */);
            }

            if (bizObject.get("vouchdate") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100027"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180529","单据日期不能为空！") /* "单据日期不能为空！" */);
            }

            //校验现金是否启用
            if (!CmpCommonUtil.checkDateByCmPeriod(bizObject.get(IBussinessConstant.ACCENTITY), bizObject.get("vouchdate"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100028"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418052B","单据日期不能小于现金启用期初期间！") /* "单据日期不能小于现金启用期初期间！" */);
            }
            if (bizObject.get("period") == null) {
                bizObject.set("period", QueryBaseDocUtils.queryPeriodIdByAccbodyAndDate(bizObject.get(IBussinessConstant.ACCENTITY), bizObject.get("vouchdate")));
            }
            //已经日结的单据不能做修改删除
            QuerySchema querySchema = QuerySchema.create().addSelect("1");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY)),
                    QueryCondition.name("settleflag").eq(1), QueryCondition.name("settlementdate").eq(bizObject.get("vouchdate")));
            querySchema.addCondition(group);
            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
            if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100029"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418052F","单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
            }
            //即期结售汇需求 添加校验
            if (bizObject.getString("purchaseamount") == null && bizObject.getString("sellamount") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100030"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180531","外币金额和人民币金额不能同时为空！") /* "外币金额和人民币金额不能同时为空！" */);
            }
            if (bizObject.get("deliverytime") != null) {
                Short deliveryTime = Short.valueOf(bizObject.get("deliverytime").toString());
                if (DeliverTime.T1.getCode() == deliveryTime || DeliverTime.T2.getCode() == deliveryTime) {
                    if (bizObject.get("delayedDate") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100219"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180533","延时交割时，交割日期不能为空！") /* "延时交割时，交割日期不能为空！" */);
                    }
                    if (bizObject.get("delegationType") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100220"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180535","延时交割时，委托类型不能为空！") /* "延时交割时，委托类型不能为空！" */);
                    }
                    if (bizObject.get("inquiryType") != null && Short.parseShort(bizObject.get("inquiryType").toString()) == InquiryType.OfflineInquiry.getValue()) {
                        if (bizObject.get("inquiryExchangerate") == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100221"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180537","线下询价时，询价汇率不能为空！") /* "线下询价时，询价汇率不能为空！" */);
                        }
                    }
                    bizObject.put("isDelayed", Boolean.TRUE);
                } else {
                    bizObject.put("isDelayed", Boolean.FALSE);
                }
            }
            if (bizObject.get("collateralOccupation") != null && Short.parseShort(bizObject.get("collateralOccupation").toString()) == CollateralOccupation.DepositCollateral.getValue()) {
                if (StringUtils.isEmpty(bizObject.get("depositAccountNo"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100222"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418053A","合格质押品占用类型为缴纳保证金，保证金账号不能为空！") /* "合格质押品占用类型为缴纳保证金，保证金账号不能为空！" */);
                }
            }
            if (bizObject.getBoolean("isCheckAccount") != null && bizObject.getBoolean("isCheckAccount")) {
                if (StringUtils.isEmpty(bizObject.getString("sellbankaccount"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100223"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180521","待核查账户不能为空！") /* "待核查账户不能为空！" */);
                }
            }

            if (bizObject.get("projectType") != null && Short.parseShort(bizObject.get("projectType").toString()) == ProjectType.CapitalMargin.getValue()) {
                if (StringUtils.isEmpty(bizObject.get("settlePurposeDetail"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100031"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180525","项目类别为资本项目时，结售汇用途详情不能为空！") /* "项目类别为资本项目时，结售汇用途详情不能为空！" */);
                }
            }
            if (bizObject.get("settlePurposeCode_code") != null && (bizObject.get("settlePurposeCode_code").equals("005") ||
                    bizObject.get("settlePurposeCode_code").equals("006") || bizObject.get("settlePurposeCode_code").equals("099"))) {
                if (StringUtils.isEmpty(bizObject.get("settlePurposeDetail"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100032"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180527","结售汇用途代码为005,006,099时，结售汇用途详情不能为空！") /* "结售汇用途代码为005,006,099时，结售汇用途详情不能为空！" */);
                }
            }
            if (StringUtils.isEmpty(bizObject.get("safeApprovalNo"))) {
                bizObject.set("safeApprovalNo", "N/A");
            }

            if (bizObject.get("deliveryType") != null && Short.parseShort(bizObject.get("deliveryType").toString()) == DeliveryType.DirectDelivery.getValue()) {
                if (bizObject.get("projectType") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100035"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001DD", "直联交割时，项目类别不能为空！") /* "直联交割时，项目类别不能为空！" */);
                }

                if (!importFlag && !openApiFlag) {
                    if (StringUtils.isEmpty(bizObject.get("transactionCode_code"))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100036"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001DE", "直联交割时，交易编码不能为空！") /* "直联交割时，交易编码不能为空！" */);
                    }
                }

                if (StringUtils.isEmpty(bizObject.get("statisticalCode"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100037"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001DB", "直联交割时，统计代码不能为空！") /* "直联交割时，统计代码不能为空！" */);
                }

                //结售汇用途代码，结汇时必输
                if (bizObject.get("exchangetype") != null && Short.parseShort(bizObject.get("exchangetype").toString()) == ExchangeType.Sell.getValue()) {
                    if (bizObject.get("settlePurposeCode") == null && !sourceCodeFlag) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100038"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A9782D405C80000","卖出外汇时，结售汇用途代码不能为空！") /* "卖出外汇时，结售汇用途代码不能为空！" */);
                    }
                }
            }
            // 买入财资统一码
            if (bizObject.get("buysmartcheckno") == null) {
                Object payBankBill = bizObject.get("paybankbill");
                if (payBankBill != null && StringUtils.isNotEmpty(payBankBill.toString())) {
                    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, payBankBill);
                    bizObject.set("buysmartcheckno", bankReconciliation.getSmartcheckno());
                }
                Object payBillClaim = bizObject.get("paybillclaim");
                if (payBillClaim != null && StringUtils.isNotEmpty(payBillClaim.toString()) ) {
                    BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, payBillClaim);
                    bizObject.set("buysmartcheckno", billClaim.getSmartcheckno());
                }
                // 流水或认领单没有财资码，新生成财资统一码
                if (StringUtils.isEmpty(bizObject.get("buysmartcheckno"))) {
                    bizObject.set("buysmartcheckno", RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
                }
            }
            // 卖出财资统一码
            if (bizObject.get("sellsmartcheckno") == null) {
                Object collectBankbill = bizObject.get("collectbankbill");
                if (collectBankbill != null && StringUtils.isNotEmpty(collectBankbill.toString())) {
                    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, collectBankbill );
                    bizObject.set("sellsmartcheckno", bankReconciliation.getSmartcheckno());
                }
                Object collectBillClaim = bizObject.get("collectbillclaim");
                if (collectBillClaim != null && StringUtils.isNotEmpty(collectBillClaim.toString()) ){
                    BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bizObject.get("collectbillclaim") );
                    bizObject.set("sellsmartcheckno", billClaim.getSmartcheckno());
                }
                // 流水或认领单没有财资码，新生成财资统一码
                if (StringUtils.isEmpty(bizObject.get("sellsmartcheckno"))) {
                    bizObject.set("sellsmartcheckno", RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
                }
            }
            //保存提交后重新匹配预算规则，原预算规则预占删除，新规则占预占
            CurrencyExchange currencyExchange = (CurrencyExchange) bizObject;
            if (currencyExchange.getVerifystate() != null && currencyExchange.getVerifystate() == VerifyState.SUBMITED.getValue()) {
                Short occupyBudget = reMatchBudget(currencyExchange);
                if (occupyBudget != null) {
                    currencyExchange.setIsOccupyBudget(occupyBudget);
                }
            }
        }
        return new RuleExecuteResult();

    }

    /**
     * 查询符合条件的直连账户信息
     *
     * @param enterpriseBankAccount
     * @return
     * @throws Exception
     */
    public boolean getOpenFlag(String enterpriseBankAccount) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (setting != null && setting.size() > 0) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }

    public Short reMatchBudget(CurrencyExchange currencyExchange) throws Exception {
        //1.判断是否是预占成功，如果是删除旧的预占，新增新的预占
        //2.判断是否是预占成功，如果否直接进行新的预占
        CurrencyExchange oldBill = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId(), 2);
        Short budgeted = oldBill.getIsOccupyBudget();
        if (budgeted == null || budgeted == OccupyBudget.UnOccupy.getValue()) {
            log.error("ReMatchBudget ，占用预算");
            boolean budget = cmpBudgetCurrencyExchangeManagerService.budget(currencyExchange);
            if (budget) {
                return OccupyBudget.PreSuccess.getValue();
            }
        } else if (budgeted == OccupyBudget.PreSuccess.getValue()) {
            boolean releaseBudget = cmpBudgetCurrencyExchangeManagerService.releaseBudget(oldBill);
            if (releaseBudget) {
                log.error("ReMatchBudget ，删除预算成功");
                currencyExchange.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetCurrencyExchangeManagerService.budget(currencyExchange);
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
}
