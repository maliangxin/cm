package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.google.common.base.Strings;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateWithMode;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TranstypeQueryPageParam;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.ctm.stwb.incomeandexpenditure.IncomeAndExpenditure;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.banktorule.BankToCommonRuleService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmcmpReWriteBusRpcServiceImpl;
import com.yonyoucloud.fi.cmp.enums.BusinessModel;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;


@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BankToFundcollectionRule extends AbstractCommonRule {

    private final BaseRefRpcService baseRefRpcService;
    private final String FundCollection_b = "FundCollection_b";
    private static final String SYSTEMCODE = "system_0001";
    private static final String EXCHANGERATETYPE = "exchangeRateType";
    private static final String EXCHANGERATEOPS = "exchangeRateOps";
    private static final String EXCHANGERATETYPENAME = "exchangeRateType_name";
    //汇率精度
    private static final String EXCHANGERATETYPE_DIGIT = "exchangeRateType_digit";

    private final CmCommonService cmCommonService;

    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;

//    //private final OrgRpcService orgRpcService;

    private final BankToCommonRuleService bankToCommonRuleService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //经测试不能用对象接收 对接平台与页面都会报异常
        List<Map<String, Object>> omakes = (List) paramMap.get(ICmpConstant.TARLIST);
        //是否是认领单
        boolean isBillClaim = BillClaim.ENTITY_NAME.equals(paramMap.get("sourceFullName").toString());
        Iterator<Map<String, Object>> iterator = omakes.iterator();
        //查询结算方式
        List<SettleMethodModel> dataList = querySettleMethod();
        while (iterator.hasNext()) {
            Map<String, Object> map = iterator.next();
            if (null == map.get(FundCollection_b)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100039"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418054C", "推单映射异常！") /* "推单映射异常！" */);
            }
            //CZFW-205451 生单时增加交易类型传递
            if (paramMap.containsKey("externalData") && paramMap.get("externalData") != null) {
                Map<String, Object> externalData = (Map<String, Object>) paramMap.get("externalData");
                if (externalData.containsKey("transtypeId") && externalData.get("transtypeId") != null) {
                    TranstypeQueryPageParam params = new TranstypeQueryPageParam();
                    params.setId(externalData.get("transtypeId").toString());
                    params.setTenantId(AppContext.getYTenantId());
                    List<BdTransType> transTypes = baseRefRpcService.queryTransTypeByCondition(params);
                    if (!org.apache.commons.collections4.CollectionUtils.isEmpty(transTypes)) {
                        BdTransType bdTransType = transTypes.get(0);
                        map.put("tradetype", bdTransType.getId());
                        map.put("tradetype_code", bdTransType.getCode());
                        //交易类型为委托付款不支持生单
                        if ("cmp_fundcollection_delegation".equals(bdTransType.getCode())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100040"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0AD24405B8000A", "该交易类型不支持生单！") /* "该交易类型不支持生单！" */);
                        }
                        String locale = InvocationInfoProxy.getLocale();
                        switch (locale) {
                            case "zh_CN":
                                map.put("tradetype_name", bdTransType.getName());
                                break;
                            case "en_US":
                                map.put("tradetype_name", bdTransType.getName2());
                                break;
                            case "zh_TW":
                                map.put("tradetype_name", bdTransType.getName3());
                                break;
                            default:
                                map.put("tradetype_name", bdTransType.getName());
                        }
                    }
                }
            }
            //单据转换规则查交易类型service_code，若是没有则自己拼一个
            if (map.get("transtype_publish_service_code") == null || StringUtils.isEmpty((CharSequence) map.get("transtype_publish_service_code"))) {
                map.put("transtype_publish_service_code", IServicecodeConstant.FUNDCOLLECTION);
            }
            String accentity = map.get(MerchantConstant.ACCENTITY).toString();
            String currency = map.get(MerchantConstant.CURRENCY).toString();
            String oriCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(currency).getName();

            //本币
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(map.get(IBussinessConstant.CURRENCY).toString());
            if (currencyTenantDTO != null) {
                map.put(IBussinessConstant.CURRENCY_PRICEDIGIT, currencyTenantDTO.getPricedigit());
                map.put(IBussinessConstant.CURRENCY_MONEYDIGIT, currencyTenantDTO.getMoneydigit());
            }

            // 本币币种
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            if (finOrgDTO != null) {
                CurrencyTenantDTO natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
                if (currencyTenantDTO != null) {
                    map.put("natCurrency", natCurrencyTenantDTO.getId());
                    map.put("natCurrency_name", natCurrencyTenantDTO.getName());
                    map.put("natCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
                    map.put("natCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
                }
            } else {
                CurrencyTenantDTO natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
                map.put("natCurrency", natCurrencyTenantDTO.getId());
                map.put("natCurrency_name", natCurrencyTenantDTO.getName());
            }

            //汇率类型
            ExchangeRateTypeVO defaultExchangeRateType = null;
            defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accentity, false);
            if (defaultExchangeRateType == null || Strings.isNullOrEmpty(defaultExchangeRateType.getId())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100041"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418054D", "此会计主体下无默认汇率类型，请检查数据！") /* "此会计主体下无默认汇率类型，请检查数据！" */);
            }
            map.put(EXCHANGERATETYPE, defaultExchangeRateType.getId());
            map.put(EXCHANGERATETYPENAME, null != defaultExchangeRateType.getName() ? defaultExchangeRateType.getName() : null);
            map.put(EXCHANGERATETYPE_DIGIT, defaultExchangeRateType.getDigit());


            BigDecimal currencyRate = BigDecimal.valueOf(1);
            short exchangeRateOps = 0;

            if (!StringUtils.isEmpty(accentity) && !StringUtils.isEmpty(currency)) {
                String orgCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(accentity);
                log.info("execute info orgCurrency =======================>" + orgCurrency);
                if (orgCurrency != null && defaultExchangeRateType != null && defaultExchangeRateType.getId() != null) {
                    try {
                        if (map.get(VOUCHDATE) == null) {
                            map.put(VOUCHDATE, new Date());
                        }
                        if (map.get(VOUCHDATE) instanceof String) {
                            map.put(VOUCHDATE, DateUtils.parseDate(map.get(VOUCHDATE).toString(), "yyyy-MM-dd HH:mm:ss"));
                        }
                        CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currency, orgCurrency, (Date) map.get(VOUCHDATE), defaultExchangeRateType.getId());
                        if (cmpExchangeRateVO == null || cmpExchangeRateVO.getExchangeRate() == BigDecimal.ZERO) {
                            throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                    map.get(EXCHANGERATETYPENAME), oriCurrencyName, currencyTenantDTO.getName()));
                        }
                        currencyRate = cmpExchangeRateVO.getExchangeRate();
                        exchangeRateOps = cmpExchangeRateVO.getExchangeRateOps();
                    } catch (Exception e) {
                        log.error("==================》取不到汇率", e);
                        throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                map.get(EXCHANGERATETYPENAME), oriCurrencyName, currencyTenantDTO.getName()));
                    }
                }
                map.put(EXCHRATE, currencyRate);
                map.put(EXCHANGERATEOPS, exchangeRateOps);
            }
            //字段赋值权限检查，无权限的清空
            fieldValueAuthCheck(map);

            //主表本币金额
            BigDecimal creditAmountMain = BigDecimal.ZERO;
            BigDecimal OriCreditAmountMain = BigDecimal.ZERO;
            List<Map<String, Object>> purchaseOrders = (List) map.get(FundCollection_b);
            Iterator<Map<String, Object>> iter = purchaseOrders.iterator();
            Map<Long, BankReconciliation> bankReconciliationMap = new HashMap<>();
            Map<String, BillClaimItem> billClaimItemMap = new HashMap<>();
            if (isBillClaim) {
                billClaimItemMap = queryBillClaim(purchaseOrders);
            } else {
                bankReconciliationMap = queryBankreconciliation(purchaseOrders);
            }
            //查询银行对账单或认领单信息
            while (iter.hasNext()) {
                Map<String, Object> map_sub = iter.next();
                //结算方式
                if (CollectionUtils.isNotEmpty(dataList) && !ValueUtils.isNotEmptyObj(map_sub.get(SETTLE_MODE))) {
                    SettleMethodModel settlementWayMap = dataList.get(0);
                    map_sub.put(SETTLE_MODE, settlementWayMap.getId());
                    map_sub.put(SETTLE_MODE_NAME, settlementWayMap.getName());
                    map_sub.put("settlemode_serviceAttr", settlementWayMap.getServiceAttr());
                }
                //结算状态更改为:已结算补单 1130认领生单修改
                if (map_sub.get("refundstatus") != null && RefundStatus.Refunded.getValue() == Short.parseShort(map_sub.get("refundstatus").toString())) {
                    map_sub.put("settlestatus", FundSettleStatus.Refund.getValue());
                } else {
                    map_sub.put("settlestatus", FundSettleStatus.SettlementSupplement.getValue());
                    map_sub.put("refundSum", null);
                }
                //汇率类型-
                map_sub.put(EXCHANGERATETYPE, defaultExchangeRateType.getId().toString());
                map_sub.put(EXCHANGERATETYPENAME, null != defaultExchangeRateType.getName() ? defaultExchangeRateType.getName() : null);
                // 换出金额预估
                map_sub.put("swapOutAmountEstimate", new BigDecimal(map_sub.get(ORISUM).toString()).abs());
                // 预估换出汇率为1
                map_sub.put("swapOutExchangeRateEstimate", BigDecimal.valueOf(1));
                // 预估结算汇率折算方式
                map_sub.put("swapOutExchangeRateOps", 1);
                // 换出预估汇率为默认汇率
                map_sub.put("swapOutExchangeRateType", defaultExchangeRateType.getId());
                map_sub.put("swapOutExchangeRateType_digit", defaultExchangeRateType.getDigit());
                map_sub.put("swapOutExchangeRateType_name", null != defaultExchangeRateType.getName() ? defaultExchangeRateType.getName() : null);
                //本币金额
                BigDecimal creditamount = BigDecimal.ZERO;
                //汇率
                map_sub.put(EXCHRATE, currencyRate);
                if (null != map_sub.get(ORISUM)) {
                    creditamount = new BigDecimal(map_sub.get(ORISUM).toString());
                    Integer digit;
                    if (map.get(IBussinessConstant.CURRENCY_MONEYDIGIT) != null && map.get(IBussinessConstant.CURRENCY_MONEYDIGIT) instanceof Integer) {
                        digit = (Integer) map.get(IBussinessConstant.CURRENCY_MONEYDIGIT);
                    } else if (map.get(IBussinessConstant.CURRENCY_MONEYDIGIT) != null && map.get(IBussinessConstant.CURRENCY_MONEYDIGIT) instanceof String) {
                        digit = Integer.valueOf(map.get(IBussinessConstant.CURRENCY_MONEYDIGIT).toString());
                    } else {
                        digit = 8;
                    }
                    if (exchangeRateOps == 1) {
                        map_sub.put(NATSUM, creditamount.multiply(currencyRate).setScale(digit, BigDecimal.ROUND_HALF_UP));
                    } else {
                        map_sub.put(NATSUM, BigDecimalUtils.safeDivide(creditamount, currencyRate, digit));
                    }
                }
                map_sub.put(ORISUM, new BigDecimal(map_sub.get(ORISUM).toString()).abs());
                //币种 币种精度
                map_sub.put(IBussinessConstant.CURRENCY, currency);
                map_sub.put(IBussinessConstant.CURRENCY_PRICEDIGIT, map.get(IBussinessConstant.CURRENCY_PRICEDIGIT));
                map_sub.put(IBussinessConstant.CURRENCY_MONEYDIGIT, map.get(IBussinessConstant.CURRENCY_MONEYDIGIT));
                creditAmountMain = BigDecimalUtils.safeAdd(creditAmountMain, new BigDecimal(map_sub.get(NATSUM).toString()));
                OriCreditAmountMain = BigDecimalUtils.safeAdd(OriCreditAmountMain, (BigDecimal) map_sub.get(ORISUM));
                // 根据业务模式 判断往下游带哪个id
                // 判断业务模式，统收统支生成虚拟id
                List<Map<String, Object>> sourceDatas = (List) paramMap.get(ICmpConstant.SOURCEDATAS);// 认领单数据
                if (sourceDatas != null && sourceDatas.size() > 0) {
                    Map<String, Object> sourceData = this.getMap(sourceDatas, map_sub, paramMap);
                    if (sourceData.get("businessmodel") != null &&
                            Short.valueOf(sourceData.get("businessmodel").toString()) == BusinessModel.Unify_InOut.getCode()) {
                        map_sub.put("billClaimId", sourceData.get("refbill"));
                        //会计主体=实际认领单位，本方账户（收款银行账户）为空
                        if (sourceData.get("actualclaimaccentiry") != null) {
                            map.put("accentity", sourceData.get("actualclaimaccentiry"));
                        }
                        map_sub.put("enterprisebankaccount", null);
                        map_sub.put("enterprisebankaccount_name", null);
                        map_sub.put("enterprisebankaccount_account", null);
                        Object incomeAndExpendRelationGroup = map_sub.get("incomeAndExpendRelationGroup");
                        if (ValueUtils.isNotEmptyObj(incomeAndExpendRelationGroup)) {
                            QuerySchema querySchema = QuerySchema.create().addSelect("rsgroupname");
                            querySchema.appendQueryCondition(QueryCondition.name("appagreement_b.id").eq(incomeAndExpendRelationGroup));
                            querySchema.appendQueryCondition(QueryCondition.name("appagreement_b.appagreement_c.marginaccount").eq(map_sub.get("actualSettleAccount")));
                            List<Map<String, Object>> query = MetaDaoHelper.query(IncomeAndExpenditure.ENTITY_NAME, querySchema);
                            if (CollectionUtils.isNotEmpty(query)) {
                                map_sub.put("incomeAndExpendRelationGroup_mainid_rsgroupname", query.get(0).get("rsgroupname"));
                            } else {
                                //若统收统支账号不在统收统支关系中，则需要将账号清空
                                map_sub.put("incomeAndExpendRelationGroup_mainid_rsgroupname", null);
                                map_sub.put("incomeAndExpendRelationGroup", null);
                                map_sub.put("incomeAndExpendBankAccount_account", null);
                                map_sub.put("incomeAndExpendBankAccount", null);
                            }
                        }
                    } else {
                        map_sub.put("isIncomeAndExpenditure", null);
                        map_sub.put("incomeAndExpendRelationGroup_mainid_rsgroupname", null);
                        map_sub.put("incomeAndExpendRelationGroup", null);
                        map_sub.put("incomeAndExpendBankAccount_account", null);
                        map_sub.put("incomeAndExpendBankAccount", null);
                    }
                    // 判断业务模式，资金代理生成虚拟id
                    if (sourceData.get("businessmodel") != null &&
                            Short.valueOf(sourceData.get("businessmodel").toString()) == BusinessModel.FundCenter_Agent.getCode()) {
                        map_sub.put("billClaimId", sourceData.get("refbill"));
                        //会计主体=实际认领单位，本方账户（收款银行账户）为空
                        if (sourceData.get("actualclaimaccentiry") != null) {
                            map.put("accentity", sourceData.get("actualclaimaccentiry"));
                        }
                        // 本方账户 = 认领账户
                        if (sourceData.get("claimaccount") != null) {
                            map_sub.put("enterprisebankaccount", sourceData.get("claimaccount"));
                            map_sub.put("enterprisebankaccount_name", sourceData.get("claimaccount_name"));
                            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById("" + sourceData.get("claimaccount"));
                            map_sub.put("enterprisebankaccount_code", enterpriseBankAcctVO.getCode());
                            map_sub.put("enterprisebankaccount_account", enterpriseBankAcctVO.getAccount());
                        } else {
                            map_sub.put("enterprisebankaccount", null);
                            map_sub.put("enterprisebankaccount_name", null);
                        }
                    }
                }
                // 填写对方信息
                bankToCommonRuleService.fillOppositeInfo(map_sub, paramMap, bankReconciliationMap, billClaimItemMap);
            }
            map.put(NATSUM, creditAmountMain);
            map.put(ORISUM, OriCreditAmountMain);
        }
        paramMap.put(ICmpConstant.TARLIST, omakes);
        StringBuilder code = new StringBuilder();
        String name = IMsgConstant.BANKRECONCILIATION;
        String serviceCode = IServicecodeConstant.CMPBANKRECONCILIATION;
        if (null != paramMap.get("sourceFullName")) {
            if (isBillClaim) {
                name = IMsgConstant.BILLCLAIMCARD;
                serviceCode = IServicecodeConstant.BILLCLAIMCARD;
                if (null != paramMap.get("sourceDatas")) {
                    List<Map<String, Object>> sourceDatas = (List) paramMap.get("sourceDatas");
                    if (!CollectionUtils.isEmpty(sourceDatas)) {
                        for (Map<String, Object> sourceData : sourceDatas) {
                            if (null != sourceData && null != sourceData.get("code") && !StringUtils.isEmpty(sourceData.get("code").toString())) {
                                code.append(sourceData.get("code").toString()).append(",");
                            }
                        }
                        if (StringUtils.isNotEmpty(code.toString())) {
                            code.deleteCharAt(code.length() - 1);
                        }
                    }
                }
            } else if (BankReconciliation.ENTITY_NAME.equals(paramMap.get("sourceFullName").toString())) {
                if (null != paramMap.get("sourceDatas")) {
                    List<Map<String, Object>> sourceDatas = (List) paramMap.get("sourceDatas");
                    if (!CollectionUtils.isEmpty(sourceDatas)) {
                        for (Map<String, Object> sourceData : sourceDatas) {
                            if (null != sourceData && null != sourceData.get("bank_seq_no") && !StringUtils.isEmpty(sourceData.get("bank_seq_no").toString())) {
                                code.append(sourceData.get("bank_seq_no").toString()).append(",");
                            }
                        }
                        if (StringUtils.isNotEmpty(code.toString())) {
                            code.deleteCharAt(code.length() - 1);
                        }
                    }
                }
            }
        }
        // 记录业务日志
        executeSaveBusinessLog(paramMap, code, serviceCode, name);
        return new RuleExecuteResult(paramMap);
    }

    /**
     * 查询结算方式
     *
     * @return
     */
    private List<SettleMethodModel> querySettleMethod() {
        //结算方式
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCode(SYSTEMCODE);
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        settleMethodQueryParam.setServiceAttr(CONSTANT_ZERO);
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        return dataList;
    }

    /**
     * 查询流水信息
     *
     * @param purchaseOrders
     * @throws Exception
     */
    private Map<Long, BankReconciliation> queryBankreconciliation(List<Map<String, Object>> purchaseOrders) throws Exception {
        // 银行流水id集合
        List<Long> bankreconciliationIdList = new ArrayList<>();
        for (Map<String, Object> map_sub : purchaseOrders) {
            // 银行对账单
            bankreconciliationIdList.add(Long.valueOf(map_sub.get("bankReconciliationId").toString()));
        }
        if (CollectionUtils.isNotEmpty(bankreconciliationIdList)) {
            return getBankReconciliationMap(bankreconciliationIdList);
        }
        return new HashMap<>();
    }

    /**
     * 查询流水或认领单信息
     *
     * @param purchaseOrders
     * @throws Exception
     */
    private Map<String, BillClaimItem> queryBillClaim(List<Map<String, Object>> purchaseOrders) throws Exception {
        // 认领单id集合
        Map<String, BillClaimItem> billClaimItemMap = new HashMap<>();
        List<String> billClaimItemIdList = new ArrayList<>();
        for (Map<String, Object> map_sub : purchaseOrders) {
            // 到账认领单
            billClaimItemIdList.add(map_sub.get("srcbillid").toString());
        }
        if (CollectionUtils.isNotEmpty(billClaimItemIdList)) {
            return getBillClaimItemMap(billClaimItemIdList);
        }
        return billClaimItemMap;
    }

    /**
     * 根据流水id查询流水详情
     *
     * @param bankreconciliationIds
     * @return
     */
    public Map<Long, BankReconciliation> getBankReconciliationMap(List<Long> bankreconciliationIds) throws Exception {
        Map<Long, BankReconciliation> bankReconciliationMap = new HashMap<>();
        List<BankReconciliation> bankReconciliations = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankreconciliationIds)) {
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("id").in(bankreconciliationIds));
            bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        }
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                bankReconciliationMap.put(bankReconciliation.getId(), bankReconciliation);
            }
        }
        return bankReconciliationMap;
    }

    /**
     * 根据认领子表id查询认领详情
     *
     * @param billclaimIds
     * @return
     */
    public Map<String, BillClaimItem> getBillClaimItemMap(List<String> billclaimIds) throws Exception {
        Map<String, BillClaimItem> billClaimItemMap = new HashMap<>();
        List<BillClaimItem> billClaimItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(billclaimIds)) {
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("id").in(billclaimIds));
            billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, schema, null);
        }
        if (CollectionUtils.isNotEmpty(billClaimItems)) {
            for (BillClaimItem billClaimItem : billClaimItems) {
                billClaimItemMap.put(billClaimItem.getId().toString(), billClaimItem);
            }
        }
        return billClaimItemMap;
    }

    /**
     * 记录业务日志
     *
     * @param paramMap
     * @param code
     * @param serviceCode
     * @param name
     */
    private void executeSaveBusinessLog(Map<String, Object> paramMap, StringBuilder code, String serviceCode, String name) {
        try {
            Map<String, Object> map = new HashMap();
            map.put(OMAKE, paramMap.get(OMAKE));
            ctmcmpBusinessLogService.saveBusinessLog(map, code.toString(), "", serviceCode, name, IMsgConstant.CMDFUNDCOLLECTION);
        } catch (Exception e) {
            log.error("BankToFundcollectionRule ctmcmpBusinessLogService：", e);
        }
    }


    private Map<String, Object> getMap(List<Map<String, Object>> sourceDatas, Map<String, Object> map_sub, Map<String, Object> paramMap) {
        if (sourceDatas.size() == 1) {
            return sourceDatas.get(0);
        }

        if (BillClaim.ENTITY_NAME.equals(paramMap.get("sourceFullName").toString())) {
            return sourceDatas.stream()
                    .filter(v -> v.get("id").equals(map_sub.get("billClaimId").toString()))
                    .findFirst()
                    .orElse(sourceDatas.get(0));
        } else if (BankReconciliation.ENTITY_NAME.equals(paramMap.get("sourceFullName").toString())) {
            return sourceDatas.stream()
                    .filter(v -> v.get("id").equals(map_sub.get("bankReconciliationId").toString()))
                    .findFirst()
                    .orElse(sourceDatas.get(0));
        }

        return sourceDatas.get(0);
    }

    /**
     * 字段赋值权限检查，无权限的清空
     *
     * @param mapData
     */
    private void fieldValueAuthCheck(Map<String, Object> mapData) throws Exception {
        //项目权限检查
        if (mapData.get("project") != null) {
            if (!CmpCommonUtil.checkProject(mapData.get("project").toString(), mapData.get(ACCENTITY).toString())) {
                mapData.put("project", null);
                mapData.put("project_name", null);
            }
        }
    }
}
