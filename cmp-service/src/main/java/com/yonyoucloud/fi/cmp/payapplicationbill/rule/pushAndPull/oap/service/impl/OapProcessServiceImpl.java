package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.impl;


import com.yonyou.diwork.service.IServiceManagerService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.workbench.model.ServiceVO;
import com.yonyoucloud.fi.cmp.arap.AuditStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.cmpentity.QuickType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceOrderType;
import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.service.OapProcessService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>应付事项推付款申请业务逻辑处理</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-19 15:57
 */
@Service
@Slf4j
public class OapProcessServiceImpl implements OapProcessService {
    private CmCommonService cmCommonService;
    private BaseRefRpcService baseRefRpcService;
    private VendorQueryService vendorQueryService;

    @Autowired
    public void setCmCommonService(CmCommonService cmCommonService) {
        this.cmCommonService = cmCommonService;
    }

    @Autowired
    public void setBaseRefRpcService(BaseRefRpcService baseRefRpcService) {
        this.baseRefRpcService = baseRefRpcService;
    }

    @Autowired
    public void setVendorQueryService(VendorQueryService vendorQueryService) {
        this.vendorQueryService = vendorQueryService;
    }

    /**
     * <h2>处理子表采购发票id和子表供应商</h2>
     *
     * @param map : 表头数据
     * @param subList : 明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:36
     */
    @Override
    public void invoiceAndSupplierCommonHandler(Map<String, Object> map, List<Map<String, Object>> subList) {
        subList.forEach(mapSub -> {
            // 当子表供应商为空时，则取主表的供应商字段值，原因需要按子表供应商字段分单
            if (!ValueUtils.isNotEmptyObj(mapSub.get(SUPPLIER))) {
                mapSub.put(SUPPLIER, map.get(SUPPLIER));
                mapSub.put(SUPPLIER_NAME, map.get(SUPPLIER_NAME));
            }
            // 为采购发票字段赋值
            if (CONSTANT_SEVEN == Integer.parseInt(map.get(SRC_ITEM).toString())) {
                mapSub.put(SRC_INVOICE_BILL_ITEM_ID, mapSub.get(SRCBILLITEMID));
            }
        });
    }

    /**
     * <h2>处理源头是订单的主表id和子表id</h2>
     *
     * @param subList : 明细数据
     * @author Sun GuoCai
     * @since 2022/11/19 17:34
     */
    @Override
    public void sourceOrderDataHandle(List<Map<String, Object>> subList) {
        subList.forEach(mapSub -> {
            // 源头订单子表id
            if (!ValueUtils.isNotEmptyObj(mapSub.get(TOP_SRC_BILL_ITEM_ID))) {
                mapSub.put(TOP_SRC_BILL_ITEM_ID, mapSub.get(TOP_SRC_BILL_ITEM_NO));
            }
            // 源头订单主表id
            mapSub.put(TOP_SRC_BILL_ID, mapSub.get(TOPSRCBILLID));
        });
    }

    /**
     * <h2>公共数据处理：供应商(默认银行账号，默认开户行地址 .etc),交易类型，款项类型，币种</h2>
     *
     * @param map : 表头数据
     * @param paramsCacheMap : 缓存Map
     * @author Sun GuoCai
     * @since 2022/11/19 17:31
     */
    @Override
    public void processOwnCommonData(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap, String tradeTypeFlag) {
        // 处理交易类型
        tradeTypeProcess(map, paramsCacheMap, tradeTypeFlag);
        // 处理币种
        currencyProcess(map, paramsCacheMap);
        // 处理款项类型
        quickTypeProcess(map, paramsCacheMap);
        // 处理供应商
        supplierProcess(map, paramsCacheMap);
    }

    /**
     * <h2>数据校验</h2>
     *
     * @param omakes : 应付事项推送过来的数据
     * @param messages : 数据校验错误提示信息集合
     * @author Sun GuoCai
     * @since 2022/11/19 17:29
     */
    @Override
    public void verificationParameters(List<Map<String, Object>> omakes, List<String> messages) {
        String reFlag = AppContext.cache().get(CMP_PAYAPPLICATIONBILL_UNDERLINE + InvocationInfoProxy.getTenantid());
        if ("0".equals(reFlag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101139"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180193","该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。") /* "该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。" */);
        }
        // 开通服务信息
        ServiceVO serviceVO = RemoteDubbo.get(IServiceManagerService.class, IDomainConstant.MDD_WORKBENCH_SERVICE).
                findByTenantIdAndServiceCode(InvocationInfoProxy.getTenantid(), IServicecodeConstant.PAYAPPLICATIONBILL);
        if (serviceVO == null) {
            AppContext.cache().set(CMP_PAYAPPLICATIONBILL_UNDERLINE + InvocationInfoProxy.getTenantid(), CONSTANT_STR_ZERO, 10 * 60 * 60);//十小时
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101139"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180193","该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。") /* "该租户未购买现金管理服务，请在业务单元期初设置中，删除现金管理启用期间。" */);
        } else {
            AppContext.cache().set(CMP_PAYAPPLICATIONBILL_UNDERLINE + InvocationInfoProxy.getTenantid(), SELECT_ONE_PARAM, 10 * 60 * 60);//十小时
        }

        for (Map<String, Object> omake : omakes) {
            String code = omake.get(CODE).toString();
            short auditStatus = Short.parseShort(omake.get(AUDIT_STATUS).toString());
            if (auditStatus == AuditStatus.Incomplete.getValue()) {
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002A", "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作") /* "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作" */,code));
                if (omake.size() == CONSTANT_ONE) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101140"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002A", "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作") /* "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作" */,code));
                }
                continue;
            }
            if (ValueUtils.isNotEmptyObj(omake.get(PROVISIONALESTIMATE_FLAG)) ? MapUtils.getBoolean(omake, PROVISIONALESTIMATE_FLAG) : false) {
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002B", "单据【%s】暂估应付不允许推单") /* "单据【%s】暂估应付不允许推单" */,code));
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180192","单据【") /* "单据【" */ + code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180197","】暂估应付不允许推单") /* "】暂估应付不允许推单" */);
                if (omake.size() == CONSTANT_ONE) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101141"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002B", "单据【%s】暂估应付不允许推单") /* "单据【%s】暂估应付不允许推单" */,code));
                }
                continue;
            }
            if (Integer.parseInt(omake.get(BILL_DIRECTION).toString()) == CONSTANT_ONE) {
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002A", "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作") /* "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作" */,code));
                if (omake.size() == CONSTANT_ONE) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101140"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002A", "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作") /* "单据【%s】存在无法操作数据，只能对已审核蓝字确认事项进行操作" */,code));
                }
            }
            short srcItem = Short.parseShort(omake.get(SRC_ITEM).toString());
            if (srcItem == CONSTANT_FOURTEEN) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180195","单据") /* "单据" */ + code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180196","项目供应商结算不支持应付单付款，请在项目服务操作付款") /* "项目供应商结算不支持应付单付款，请在项目服务操作付款" */);
                if (omake.size() == CONSTANT_ONE) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101142"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180195","单据") /* "单据" */ + code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180196","项目供应商结算不支持应付单付款，请在项目服务操作付款") /* "项目供应商结算不支持应付单付款，请在项目服务操作付款" */);
                }
            }
        }
    }

    /**
     * <h2>处理累计付款申请金额</h2>
     *
     * @param fromTopOutsourcingOrderList : 明细数据集合
     * @param map : 表头数据
     * @param paymentApplyAmountSumPurchaseAmount : 累计付款申请金额合计
     * @return boolean
     * @author Sun GuoCai
     * @since 2022/11/19 18:43
     */
    @Override
    public boolean paymentApplyAmountSumProcess(List<Map<String, Object>> fromTopOutsourcingOrderList,
                                                Map<String, Object> map, BigDecimal paymentApplyAmountSumPurchaseAmount) {
        if (!ValueUtils.isNotEmptyObj(fromTopOutsourcingOrderList)) {
            return true;
        }
        // 计算此来源分支的累计付款申请金额
        map.put(PAYMENT_APPLY_AMOUNT_SUM, ValueUtils.isNotEmptyObj(map.get(PAYMENT_APPLY_AMOUNT_SUM))
                ? BigDecimalUtils.safeAdd(paymentApplyAmountSumPurchaseAmount, new BigDecimal(map.get(PAYMENT_APPLY_AMOUNT_SUM).toString()))
                : BigDecimalUtils.safeAdd(paymentApplyAmountSumPurchaseAmount, BigDecimal.ZERO));
        return false;
    }

    /**
     * <h2>设置相关金额字段的值和组织机构字段的值</h2>
     *
     * @param map : 表头数据
     * @param paymentApplyAmountSum : 累计付款申请金额字段
     * @author Sun GuoCai
     * @since 2022/11/19 19:54
     */
    @Override
    public void setAmountDefaultValueAndOrg(Map<String, Object> map, BigDecimal paymentApplyAmountSum) {
        map.put(PAYMENT_APPLY_AMOUNT_SUM, paymentApplyAmountSum);
        map.put(UNPAID_AMOUNT_SUM, paymentApplyAmountSum);
        map.put(PAID_AMOUNT_SUM, BigDecimal.ZERO);
        if (ValueUtils.isNotEmptyObj(map.get(ORG))) {
            map.put(ORG, map.get(ORG));
            map.put(ORG_NAME, map.get(ORG_NAME));
        } else {
            map.put(ORG, map.get(IBussinessConstant.ACCENTITY));
            map.put(ORG_NAME, map.get(ACCENTITY_NAME));
        }
    }

    /**
     * <h2>老数据源头订单类型默认值的处理</h2>
     *
     * @param map : 表头数据
     * @param details : 子表明细数据集合
     * @author Sun GuoCai
     * @since 2022/11/19 19:59
     */
    @Override
    public void sourceOrderTypeValueProcess(Map<String, Object> map, List<Map<String, Object>> details) {
        details.forEach(t -> t.computeIfAbsent(SOURCE_ORDER_TYPE, k -> SourceOrderType.MattersOap.getValue()));
    }

    /**
     * <h2>请求委外订单提供的批量接口，获取可付款申请金额</h2>
     *
     * @param map : 表头数据
     * @param params : 批量入参参数
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2022/11/19 17:04
     */
    @Override
    public CtmJSONObject queryOutsourcingOrderRequestedAmount(Map<String, Object> map, Map<String, Object> params) {
        String json = null;
        try {
            json = CtmJSONObject.toJSONString(params);
            String upuUrl = AppContext.getEnvConfig(YONBIP_MM_MFPO);
            upuUrl += GET_OUTSOURCING_ORDER_REQUEST_AMOUNT;
            Map<String, String> headersMap = new HashMap<>(CONSTANT_EIGHT);
            headersMap.put(YHT_ACCESS_TOKEN, InvocationInfoProxy.getYhtAccessToken());
            headersMap.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            // 执行post请求的方法
            String resultJson = HttpTookit.doPostWithJson(upuUrl, json, headersMap);
            if (ValueUtils.isNotEmptyObj(resultJson)) {
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(resultJson);
                if (!jsonObject.isEmpty()) {
                    CtmJSONObject jsonData = new CtmJSONObject();
                    ArrayList<Map<String , Object>> jsonArray = (ArrayList) CtmJSONObject.parseObject(CtmJSONObject.toJSONString(jsonObject.get(DATA))).get(PRODUCT_RESULTS);
                    for (Map<String , Object> data : jsonArray) {
                        jsonData.put(MapUtils.getString(data, PRIMARY_ID), new BigDecimal(MapUtils.getString(data, CALU_MONEY_TC)));
                    }
                    return jsonData;
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101143"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180192","单据【") /* "单据【" */ + map.get(CODE).toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180194","获取源头委外订单剩余付款申请金额失败！") /* "获取源头委外订单剩余付款申请金额失败！" */);
            }
        } catch (Exception e) {
            log.error("oap push pay apply bill, get outsourcing small balance: json = {}, e = {}", json, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101143"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180192","单据【") /* "单据【" */ + map.get(CODE).toString() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180194","获取源头委外订单剩余付款申请金额失败！") /* "获取源头委外订单剩余付款申请金额失败！" */);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void supplierProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap) {
        try {
            List<Map<String, Object>> oapDetails = (List<Map<String, Object>>) map.get(OAP_DETAIL);
            for (Map<String, Object> oapDetail : oapDetails) {
                Object supplierId = oapDetail.get(SUPPLIER);
                if (supplierId != null) {
                    String supplier = SUPPLIER_UNDERLINE + supplierId;
                    Map<String, Object> supplierCacheValue = paramsCacheMap.get(supplier);
                    if (ValueUtils.isNotEmptyObj(supplierCacheValue)) {
                        oapDetail.put(SUPPLIER_BANK_ACCOUNT, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT));
                        oapDetail.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME));
                        oapDetail.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT_ACCOUNT));
                        oapDetail.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, supplierCacheValue.get(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME));
                    } else {
                        Map<String, Object> conditionSupplierId = new HashMap<>(CONSTANT_EIGHT);
                        conditionSupplierId.put(VENDOR, supplierId);
                        conditionSupplierId.put(DEFAULT_BANK, true);
                        Object currency = map.get(CURRENCY);
                        if (ValueUtils.isNotEmptyObj(currency)) {
                            conditionSupplierId.put(CURRENCY, currency);
                        }
                        conditionSupplierId.put(STOP_STATUS, false);
                        List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(conditionSupplierId);
                        Map<String, Object> supplierCache = new HashMap<>();
                        if (bankAccounts.size() > 0) {
                            oapDetail.put(SUPPLIER_BANK_ACCOUNT, bankAccounts.get(0).getId());
                            oapDetail.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, bankAccounts.get(0).getAccountname());
                            oapDetail.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, bankAccounts.get(0).getAccount());// 供应商银行账号

                            supplierCache.put(SUPPLIER_BANK_ACCOUNT, bankAccounts.get(0).getId());
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, bankAccounts.get(0).getAccountname());
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, bankAccounts.get(0).getAccount());// 供应商银行账号
                            supplierCache.put(SUPPLIER_CORRESPONDENT_CODE, bankAccounts.get(0).getCorrespondentcode()); // 供应商联行号

                            BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
                            if (depositBank != null) {
                                oapDetail.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, depositBank.getName()); // 供应商账户银行网点
                                supplierCache.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, depositBank.getName()); // 供应商账户银行网点
                            } else {
                                oapDetail.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, null); // 供应商账户银行网点
                                supplierCache.put(SUPPLIER_BANK_ACCOUNT_OPEN_ACCOUNT_BANK_NAME, null); // 供应商账户银行网点

                            }

                        } else {
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT, null);
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT_NAME, null);
                            supplierCache.put(SUPPLIER_BANK_ACCOUNT_ACCOUNT, null);
                            supplierCache.put(SUPPLIER_CORRESPONDENT_CODE, null);
                        }
                        paramsCacheMap.put(supplier, supplierCache);
                    }
                }
            }
        } catch (Exception e) {
            log.error("query futureBankAccounts fail!, e = {}", e.getMessage());
        }
    }

    private void quickTypeProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap) {
        try {
            Map<String, Object> quickType = paramsCacheMap.get(QUICK_TYPE);
            if (ValueUtils.isNotEmptyObj(quickType)) {
                map.put(QUICK_TYPE, quickType.get(QUICK_TYPE));
                map.put(QUICKTYPE_NAME, quickType.get(QUICKTYPE_NAME));
                map.put(QUICKTYPE_CODE, quickType.get(QUICKTYPE_CODE));
            } else {
                Map<String, Object> quickTypeCache = new HashMap<>(CONSTANT_EIGHT);
                HashMap<String, Object> quickCode = SqlHelper.selectOne(QUICK_TYPE_MAPPER, AppContext.getTenantId());
                if (null != quickCode && null != quickCode.get(C_DEFAULT_VALUE) && !"".equals(quickCode.get(C_DEFAULT_VALUE))) {
                    List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(quickCode.get(C_DEFAULT_VALUE)));
                    getDefaultQuickType(map, quickTypeCache, quickTypeMap);
                } else {
                    List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(QuickType.accountPayable.getValue()));
                    getDefaultQuickType(map, quickTypeCache, quickTypeMap);
                }
                paramsCacheMap.put(QUICK_TYPE, quickTypeCache);
            }
        } catch (Exception e) {
            log.error("query quickType fail!, e = {}", e.getMessage());
        }
    }

    private void currencyProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap) {
        try {
            Object currency = map.get(CURRENCY);
            String key = CURRENCY_UNDERLINE + currency;
            Map<String, Object> currencyCacheValue = paramsCacheMap.get(key);
            if (ValueUtils.isNotEmptyObj(currencyCacheValue)) {
                map.put(CURRENCY_PRICEDIGIT, currencyCacheValue.get(CURRENCY_PRICEDIGIT));
                map.put(CURRENCY_MONEYDIGIT, currencyCacheValue.get(CURRENCY_MONEYDIGIT));
            } else {
                CurrencyTenantDTO natCurrency = baseRefRpcService.queryCurrencyById(currency.toString());
                if (natCurrency != null) {
                    map.put(CURRENCY_PRICEDIGIT, natCurrency.getPricedigit());
                    map.put(CURRENCY_MONEYDIGIT, natCurrency.getMoneydigit());

                    Map<String, Object> currencyCache = new HashMap<>(CONSTANT_EIGHT);
                    currencyCache.put(CURRENCY_PRICEDIGIT, natCurrency.getPricedigit());
                    currencyCache.put(CURRENCY_MONEYDIGIT, natCurrency.getMoneydigit());
                    paramsCacheMap.put(key, currencyCache);
                }
            }
        } catch (Exception e) {
            log.error("query natCurrency fail!, e = {}", e.getMessage());
        }
    }

    private void tradeTypeProcess(Map<String, Object> map, Map<String, Map<String, Object>> paramsCacheMap, String tradeTypeFlag) {
        try {
            Map<String, Object> tradeType = paramsCacheMap.get(TRADE_TYPE);
            if (ValueUtils.isNotEmptyObj(tradeType)) {
                map.put(DEFAULT_TRADETYPE, tradeType.get(DEFAULT_TRADETYPE));
                map.put(DEFAULT_TRADETYPE_NAME, tradeType.get(DEFAULT_TRADETYPE_NAME));
                map.put(DEFAULT_TRADETYPE_CODE, tradeType.get(DEFAULT_TRADETYPE_CODE));
            } else {
                Map<String, Object> tradeTypeMap = cmCommonService.queryTransTypeById(FICM3, CONSTANT_STR_ZERO, tradeTypeFlag);
                map.put(DEFAULT_TRADETYPE, tradeTypeMap.get(PRIMARY_ID));
                map.put(DEFAULT_TRADETYPE_NAME, tradeTypeMap.get(NAME));
                map.put(DEFAULT_TRADETYPE_CODE, tradeTypeMap.get(CODE));

                Map<String, Object> tradeTypeCache = new HashMap<>(CONSTANT_EIGHT);
                tradeTypeCache.put(DEFAULT_TRADETYPE, tradeTypeMap.get(PRIMARY_ID));
                tradeTypeCache.put(DEFAULT_TRADETYPE_NAME, tradeTypeMap.get(NAME));
                tradeTypeCache.put(DEFAULT_TRADETYPE_CODE, tradeTypeMap.get(CODE));
                paramsCacheMap.put(TRADE_TYPE, tradeTypeCache);
            }
        } catch (Exception e) {
            log.error("query tradeType fail!, e = {}", e.getMessage());
        }
    }

    private void getDefaultQuickType(Map<String, Object> map, Map<String, Object> quickTypeCache, List<Map<String, Object>> quickTypeMap) {
        if (quickTypeMap.size() > CONSTANT_ZERO) {
            map.put(QUICK_TYPE, MapUtils.getLong(quickTypeMap.get(CONSTANT_ZERO), PRIMARY_ID));
            map.put(QUICKTYPE_NAME, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), NAME));
            map.put(QUICKTYPE_CODE, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), CODE));

            quickTypeCache.put(QUICK_TYPE, MapUtils.getLong(quickTypeMap.get(CONSTANT_ZERO), PRIMARY_ID));
            quickTypeCache.put(QUICKTYPE_NAME, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), NAME));
            quickTypeCache.put(QUICKTYPE_CODE, MapUtils.getString(quickTypeMap.get(CONSTANT_ZERO), CODE));
        }
    }

}
