package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyou.cloud.middleware.rpc.RPCStubBeanFactory;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.data.service.itf.FuncOrgShareQryApi;
import com.yonyou.iuap.data.service.itf.OrgFuncSharingSettingApi;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.org.dto.OrgFuncSharingSettingQryParam;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeContext;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.common.BizObjCodeUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.poi.constant.POIConstant;
import com.yonyou.ucf.mdd.ext.poi.dto.ExcelErrorMsgDto;
import com.yonyou.ucf.mdd.ext.poi.importbiz.enums.ErrorMsgRealTypeEnum;
import com.yonyou.ucf.mdd.ext.poi.importbiz.exception.ImportDataSaveException;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.ExchangeRateTypeQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import com.yonyoucloud.fi.cmp.util.business.BillImportCheckUtil;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantApplyRangeDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorExtendVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorOrgVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import com.yonyoucloud.uretail.sys.auth.DataPermissionRequestDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResponseDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResultDto;
import com.yonyoucloud.uretail.sys.pubItf.IDataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
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

// import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * 资金收款单、资金付款单-导入后规则
 *
 * @author majfd
 * <p>
 * 优先取客户档案信息
 * 付票单位（客户档案，供应商档案，输入）
 * 出票人（客户档案，供应商档案，输入）
 * 收款人（客户档案，供应商档案，输入）
 * 承兑人（客户档案，供应商档案，银行类别档案，输入）
 * <p>
 * 出票人银行账号（客户银行账号，供应商银行账号，输入）
 * 收款人银行账号（客户银行账号，供应商银行账号，会计主体的银行账号，输入）
 * 承兑人银行账号（客户银行账号，供应商银行账号，输入）
 */

@Slf4j
@Component("afterImportFundBillRule")
@RequiredArgsConstructor
public class AfterImportFundBillRule extends AbstractCommonRule {

    private final BaseRefRpcService baseRefRpcService;

    private final VendorQueryService vendorQueryService;

    private final CmCommonService<Object> cmCommonService;

    private final IFundCommonService fundCommonService;

    private final EnterpriseBankQueryService enterpriseBankQueryService;

    private final FuncOrgShareQryApi funcOrgShareQryApi;

    private final OrgDataPermissionService orgDataPermissionService;

    private final OrgFuncSharingSettingApi orgFuncSharingSettingApi;

    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;

    @Autowired
    AutoConfigService autoConfigService;

    @Autowired
    ExchangeRateTypeQueryService exchangeRateTypeQueryService;

    // 数据权限缓存
    Map<String, Map<String, Set<String>>> dataPermissionMap = new HashMap<>(256);

    Map<String, Map<String, Object>> tradeTypeMap = new HashMap<>();// 交易类型
    Map<String, CurrencyTenantDTO> currencyCacheMap = new HashMap<>();// 币种档案
    Map<String, String> natCurrencyMap = new HashMap<>();// 组织本币
    Map<String, ExchangeRateTypeVO> defExchangeRateTypeMap = new HashMap<>();// 组织默认汇率类型
    Map<String, ExchangeRateTypeVO> exchangeRateTypeMap = new HashMap<>();// 汇率类型档案
    Map<String, Map<String, Object>> noteNoMap = new HashMap<>();// 票据号档案


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        if (bills != null && !bills.isEmpty()) {
            BizObject bizObject = bills.get(0);
            // 导入数据校验
            boolean noImportFlag = !"import".equals(billDataDto.getRequestAction());
            //begin ctm wangzc 陕建 api 进来的属性平台现在传的是fromapi 属性，进行改动
            boolean noOpenApiFlag = (!bizObject.containsKey("_fromApi") || bizObject.get("_fromApi").equals(false)) && !billDataDto.getFromApi();
            //end
            if (noImportFlag && noOpenApiFlag) {
                return new RuleExecuteResult();
            }
            try {
                if (FIDubboUtils.isSingleOrg()) {
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    bizObject.set(IBussinessConstant.ACCENTITY, singleOrg.get("id"));
                    bizObject.set("accentity_name", singleOrg.get("name"));
                }
            } catch (Exception e) {
                log.error("单组织判断异常!", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102054"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180816", "单组织判断异常！") /* "单组织判断异常！" */ + e.getMessage());
            }

            // 数据权限过滤
            String permissionKey = AppContext.getUserId().toString() + InvocationInfoProxy.getTenantid();
            Map<String, String> fieldName = dataPermissionFilter(billContext, permissionKey);

            beforeCheck(bizObject, billContext.getBillnum(), fieldName, permissionKey);
            // 初始化默认值
            initSaveData(bizObject, billContext.getBillnum());
            // 集中处理客户、供应商、员工参照、金额、税额数据
            dealCustSuppRefAndCalculateAmount(bizObject, billContext.getBillnum(), fieldName, permissionKey);
            // 集中处理客户银行账户、供应商银行账户、员工银行账户参照
            dealCustSuppBankRef(bizObject, billContext.getBillnum());
        }

        return new RuleExecuteResult();
    }

    private Map<String, String> dataPermissionFilter(BillContext billContext, String permissionKey) throws Exception {
        Map<String, String> fieldName;
        DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
        if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {// 资金收款单
            requestDto.setEntityUri(FundCollection.ENTITY_NAME);
            requestDto.setServiceCode(IServicecodeConstant.FUNDCOLLECTION);
        } else if (IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {// 资金付款单
            requestDto.setEntityUri(FundPayment.ENTITY_NAME);
            requestDto.setServiceCode(IServicecodeConstant.FUNDPAYMENT);
        }
        requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
        requestDto.setYxyUserId(AppContext.getUserId().toString());
        requestDto.setYhtTenantId(InvocationInfoProxy.getTenantid());
        requestDto.setYxyTenantId(AppContext.getTenantId().toString());
        requestDto.setHaveDetail(true);
        RPCStubBeanFactory rpChainBeanFactory = new RPCStubBeanFactory("iuap-apcom-auth", "c87e2267-1001-4c70-bb2a-ab41f3b81aa3", null, IDataPermissionService.class);
        rpChainBeanFactory.afterPropertiesSet();
        IDataPermissionService remoteBean = (IDataPermissionService) rpChainBeanFactory.getObject();

        String[] fields = new String[]{"customer", "supplier", "employee", "enterprisebankaccount", "cashaccount", "project", "dept", "operator", "expenseitem"};
        fieldName = new HashMap<>();
        fieldName.put("customer", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180827", "客户") /* "客户" */);
        fieldName.put("supplier", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180828", "供应商") /* "供应商" */);
        fieldName.put("employee", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418082A", "员工") /* "员工" */);
        fieldName.put("enterprisebankaccount", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418082B", "企业银行账号") /* "企业银行账号" */);
        fieldName.put("cashaccount", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418082C", "企业现金账号") /* "企业现金账号" */);
        fieldName.put("project", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418082D", "项目") /* "项目" */);
        fieldName.put("dept", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418082E", "部门") /* "部门" */);
        fieldName.put("operator", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418082F", "业务员") /* "业务员" */);
        fieldName.put("expenseitem", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180830", "费用项目") /* "费用项目" */);
        requestDto.setFieldNameArgs(Arrays.stream(fields).toArray(String[]::new));
        assert remoteBean != null;
        DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
        List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
        Map<String, Set<String>> map = new HashMap<>();
        for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
            String[] ids = resultDto.getValues();
            Set<String> set = Arrays.stream(ids).collect(Collectors.toSet());
            map.put(resultDto.getFieldName(), set);
        }
        dataPermissionMap.put(permissionKey, map);
        return fieldName;
    }

    /**
     * 初始化币种信息
     *
     * @param bizObject
     * @throws Exception
     */
    private void initSaveDataOfCurrency(BizObject bizObject) throws Exception {
        // 组织本币
        String natCurrency = null;
        if (natCurrencyMap != null && natCurrencyMap.get(bizObject.get("currency")) != null) {
            natCurrency = natCurrencyMap.get(bizObject.get(IBussinessConstant.ACCENTITY));
        } else {
            natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
            if (StringUtils.isEmpty(natCurrency)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100959"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180807", "会计主体[") /* "会计主体[" */ + bizObject.get("accentity_name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180806", "]组织本币币种为空!") /* "]组织本币币种为空!" */);
            }
        }
        // 本币
        CurrencyTenantDTO natCurrencyTenantDTO = getCurrencyMapByID(natCurrency);
        if (natCurrencyTenantDTO != null) {
            bizObject.set("natCurrency", natCurrencyTenantDTO.getId());
            bizObject.set("natCurrency_name", natCurrencyTenantDTO.getName());
            bizObject.set("natCurrency_priceDigit", natCurrencyTenantDTO.getPricedigit());
            bizObject.set("natCurrency_moneyDigit", natCurrencyTenantDTO.getMoneydigit());
        }
        // 币种
        CurrencyTenantDTO currencyTenantDTO = getCurrencyMapByID(bizObject.get("currency"));
        if (currencyTenantDTO != null) {
            bizObject.set("currency", currencyTenantDTO.getId());
            bizObject.set("currency_name", currencyTenantDTO.getName());
            bizObject.set("currency_priceDigit", currencyTenantDTO.getPricedigit());
            bizObject.set("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
        }
    }

    /**
     * 导入保存前初始化数据
     *
     * @param bizObject
     * @param billNum
     * @throws Exception
     */
    private void initSaveData(BizObject bizObject, String billNum) throws Exception {
        Map<String, Object> transType = new HashMap<>();
        if (bizObject.get("vouchdate") == null) {
            bizObject.set("vouchdate", BillInfoUtils.getBusinessDate());
        }
        // 初始化币种信息
        initSaveDataOfCurrency(bizObject);

        // 审批状态
        bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
        // 审批流状态
        bizObject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
        bizObject.set("entrytype", EntryType.Normal_Entry.getValue());
        // 凭证状态
        bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
        // 事项来源、事项类型
        bizObject.set("srcitem", EventSource.Cmpchase.getValue());
        if (!ValueUtils.isNotEmptyObj(bizObject.get("settleflag"))) {
            bizObject.set("settleflag", 1);
        }
        if (!ValueUtils.isNotEmptyObj(bizObject.get("autoSettleFlag"))) {
            bizObject.set("autoSettleFlag", 1);
        }
        if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
            bizObject.set("billtype", EventType.FundCollection.getValue());
            if (bizObject.get("tradetype") == null) {
                Map<String, Object> queryTransType = cmCommonService.queryTransTypeByForm_id(ICmpConstant.CM_CMP_FUND_COLLECTION);
                if (MapUtils.isNotEmpty(queryTransType)) {
                    bizObject.set("tradetype", queryTransType.get("id"));
                    bizObject.set("tradetype_code", queryTransType.get("code"));
                    bizObject.set("tradetype_name", queryTransType.get("name"));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102055"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180817", "资金收款单未设置默认交易类型，请在导入模板交易类型编码列中录入！"));
                }
            } else {
                //不为空需要判断此对象是否是付款单的
                List<Map<String, Object>> transTypeList = cmCommonService.queryTransTypesByForm_ids(ICmpConstant.CM_CMP_FUND_COLLECTION);
                List<Object> codeList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(transTypeList)) {
                    transTypeList.stream().forEach(map -> {
                        if (null != map) {
                            List<Object> codes = map.entrySet().stream().filter(entry -> entry.getKey().equals("code")).map(Map.Entry::getValue).collect(Collectors.toList());
                            codeList.addAll(codes);
                        }
                    });
                }
                String tradetype_code = bizObject.get("tradetype_code");
                if (null == codeList || !codeList.contains(tradetype_code)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DEAD02C05A80017", "资金收款单填写的交易类型【") /* "资金收款单填写的交易类型【" */ + tradetype_code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DEAD02C05A80018", "】不是收款单的交易类型，请在导入模板交易类型编码列中录入！") /* "】不是收款单的交易类型，请在导入模板交易类型编码列中录入！" */);
                }
            }
        } else if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
            bizObject.set("billtype", EventType.FundPayment.getValue());
            // 交易类型
            if (bizObject.get("tradetype") == null) {

                Map<String, Object> queryTransType = cmCommonService.queryTransTypeByForm_id(ICmpConstant.CM_CMP_FUND_PAYMENT);
                if (MapUtils.isNotEmpty(queryTransType)) {
                    bizObject.set("tradetype", queryTransType.get("id"));
                    bizObject.set("tradetype_code", queryTransType.get("code"));
                    bizObject.set("tradetype_name", queryTransType.get("name"));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102057"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180823", "资金付款单未设置默认交易类型，请在导入模板交易类型编码列中录入！") /* "资金付款单未设置默认交易类型，请在导入模板交易类型编码列中录入！" */);
                }
            } else {
                //不为空需要判断此对象是否是付款单的
                List<Map<String, Object>> transTypeList = cmCommonService.queryTransTypesByForm_ids(ICmpConstant.CM_CMP_FUND_PAYMENT);
                List<Object> codeList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(transTypeList)) {
                    transTypeList.stream().forEach(map -> {
                        if (null != map) {
                            List<Object> codes = map.entrySet().stream().filter(entry -> entry.getKey().equals("code")).map(Map.Entry::getValue).collect(Collectors.toList());
                            codeList.addAll(codes);
                        }
                    });
                }
                String tradetype_code = bizObject.get("tradetype_code");
                if (null == codeList || !codeList.contains(tradetype_code)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DEAD02C05A80015", "资金付款单填写的交易类型不是【") /* "资金付款单填写的交易类型不是【" */ + tradetype_code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DEAD02C05A80016", "】资金付款单本身的交易类型，请在导入模板交易类型编码列中重新录入！") /* "】资金付款单本身的交易类型，请在导入模板交易类型编码列中重新录入！" */);
                }
            }
        }
        // 汇率类型
        ExchangeRateTypeVO exchangeRateType = null;
        if (bizObject.get("exchangeRateType") == null) {
            if (defExchangeRateTypeMap != null && defExchangeRateTypeMap.get(bizObject.get(IBussinessConstant.ACCENTITY)) != null) {
                exchangeRateType = defExchangeRateTypeMap.get(bizObject.get(IBussinessConstant.ACCENTITY));
            } else {
                exchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(bizObject.get(IBussinessConstant.ACCENTITY), false);
                defExchangeRateTypeMap.put(bizObject.get(IBussinessConstant.ACCENTITY), exchangeRateType);
                exchangeRateTypeMap.put(exchangeRateType.getId(), exchangeRateType);
            }
        } else {
            if (exchangeRateTypeMap != null && exchangeRateTypeMap.get(bizObject.get("exchangeRateType")) != null) {
                exchangeRateType = exchangeRateTypeMap.get(bizObject.get("exchangeRateType"));
            } else {
                Map<String, Object> oriExchangeRateType = QueryBaseDocUtils.queryExchangeRateTypeById(bizObject.get("exchangeRateType")).get(0);
                ObjectMapper mapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
                mapper.convertValue(oriExchangeRateType, ExchangeRateTypeVO.class);
                exchangeRateTypeMap.put(bizObject.get("exchangeRateType"), exchangeRateType);
            }
        }
        if (exchangeRateType != null) {
            bizObject.set("exchangeRateType", exchangeRateType.getId());
            bizObject.set("exchangeRateType_digit", exchangeRateType.getDigit());
            bizObject.set("exchangeRateType_name", exchangeRateType.getName());
        }
        // 汇率（取汇率表中报价日期小于等于单据日期的值）
        if (bizObject.get("natCurrency").equals(bizObject.get("currency"))) {
            bizObject.set(ICmpConstant.EXCHRATE, new BigDecimal("1"));
        }

        boolean notDefineExchangeRateType = bizObject.get("exchangeRateType_code") == null || (bizObject.get("exchangeRateType_code") != null && !bizObject.get("exchangeRateType_code").toString().equals("02"));
        //补充汇率折算方式
        if (bizObject.get("exchangeRateOps") == null && notDefineExchangeRateType ) {
            CmpExchangeRateVO mainCmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bizObject.get(ICmpConstant.CURRENCY),
                    bizObject.get(ICmpConstant.NATCURRENCY), bizObject.get(ICmpConstant.VOUCHDATE), bizObject.get(ICmpConstant.EXCHANGE_RATE_TYPE));
            bizObject.set(ICmpConstant.EXCHRATEOPS, mainCmpExchangeRateVO.getExchangeRateOps());
            bizObject.set("exchRate", mainCmpExchangeRateVO.getExchangeRate());
        }
        if(!notDefineExchangeRateType){
            bizObject.set(ICmpConstant.EXCHRATEOPS, 1);
        }
        boolean openApiFlag = (bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true));
        // 异币种适配导入
        List<BizObject> bizObject_bList = new ArrayList<>();
        if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
            bizObject_bList = bizObject.getBizObjects("FundPayment_b", BizObject.class);
            for (BizObject bizObj : bizObject_bList) {
                Object settlemode = bizObj.get("settlemode");
                Integer serviceAttr = -1;
                if (ValueUtils.isNotEmptyObj(settlemode)) {
                    SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                    settleMethodQueryParam.setId(bizObj.get("settlemode"));
                    settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
                    settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                    List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                    if (CollectionUtils.isNotEmpty(dataList)) {
                        serviceAttr = dataList.get(0).getServiceAttr();
                    }
                }
                String settleCurrency = bizObj.getString("settleCurrency");
                if (StringUtils.isNotEmpty(settleCurrency) && serviceAttr != 0 && !settleCurrency.equals(bizObject.get(ICmpConstant.CURRENCY))) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A1A604C0000B", "结算方式不为银行业务时，结算币种与原币币种需要一致！") /* "结算方式不为银行业务时，结算币种与原币币种需要一致！" */);
                }
                if(Objects.isNull(bizObj.get("swapOutExchangeRateType"))){
                    //换出汇率
                    Map<String, Object> DefaultExchangeRateTypeOfFinOrg = cmCommonService.getDefaultExchangeRateTypeByFundsOrgid(bizObject.get("accentity").toString());
                    if (DefaultExchangeRateTypeOfFinOrg != null && DefaultExchangeRateTypeOfFinOrg.get("id") != null) {
                        bizObj.set("swapOutExchangeRateType", DefaultExchangeRateTypeOfFinOrg.get("id"));
                        bizObj.set("swapOutExchangeRateType_name", DefaultExchangeRateTypeOfFinOrg.get("name"));
                        bizObj.set("swapOutExchangeRateType_digit", DefaultExchangeRateTypeOfFinOrg.get("digit"));
                        bizObj.set("swapOutExchangeRateType_code", DefaultExchangeRateTypeOfFinOrg.get("code"));
                    }
                }
                // 自定义汇率
                ExchangeRateTypeVO exchangeRateTypeVO = exchangeRateTypeQueryService.queryExcahngeRateTypeById(bizObj.get("swapOutExchangeRateType"));
                if (exchangeRateTypeVO != null) {
                    bizObj.set("swapOutExchangeRateType_code", exchangeRateTypeVO.getCode());
                    if (CurrencyRateTypeCode.CustomCode.getValue().equals(exchangeRateTypeVO.getCode()) && Objects.isNull(bizObj.get("swapOutExchangeRateEstimate"))) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2267E2AA04980006", "换出汇率类型为用户自定义时，换出汇率预估不能为空！") /* "换出汇率类型为用户自定义时，换出汇率预估不能为空" */);
                    }
                }
                if (openApiFlag) {
                    if (StringUtils.isNotEmpty(settleCurrency)) {
                        // 处理结算币种信息
                        handleSettleCurrency(bizObject, bizObj);
                    } else {
                        // 处理结算币种信息
                        bizObj.set("settleCurrency", bizObject.get(ICmpConstant.CURRENCY));
                        bizObj.set("swapOutExchangeRateType", bizObject.get(ICmpConstant.EXCHANGE_RATE_TYPE));
                        handleSettleCurrency(bizObject, bizObj);
                    }
                } else {
                    if (!ValueUtils.isNotEmptyObj(settleCurrency) || !ValueUtils.isNotEmptyObj(settlemode) || serviceAttr != 0) {
                        bizObj.set("settleCurrency", bizObject.get(ICmpConstant.CURRENCY));
                        bizObj.set("swapOutExchangeRateType", bizObject.get(ICmpConstant.EXCHANGE_RATE_TYPE));
                        bizObj.set("swapOutAmountEstimate", bizObj.get(ICmpConstant.ORISUM));
                        bizObj.set("swapOutExchangeRateEstimate", new BigDecimal("1"));
                    } else {
                        if (!ValueUtils.isNotEmptyObj(bizObj.get("swapOutExchangeRateType"))) {
                            bizObj.set("swapOutExchangeRateType", bizObject.get(ICmpConstant.EXCHANGE_RATE_TYPE));
                        }
                        if (bizObject.get(ICmpConstant.CURRENCY).equals(settleCurrency)) {
                            bizObj.set("swapOutExchangeRateEstimate", new BigDecimal("1"));
                        } else {
                            boolean notDefineExchangeRateTypeB = bizObj.get("swapOutExchangeRateType_code") == null || (bizObj.get("swapOutExchangeRateType_code") != null && !bizObj.get("swapOutExchangeRateType_code").toString().equals("02"));
                            if(notDefineExchangeRateTypeB){
                                CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(
                                        bizObject.get(ICmpConstant.CURRENCY),
                                        settleCurrency,
                                        bizObject.get(ICmpConstant.VOUCHDATE),
                                        bizObj.get("swapOutExchangeRateType"));
                                if (cmpExchangeRateVO == null) {
                                    String oriCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(bizObject.get(ICmpConstant.CURRENCY)).getName();
                                    String natCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(settleCurrency).getName();
                                    String exchangeRateTypeName = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateTypeById(bizObj.get("swapOutExchangeRateType")).getName();
                                    throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                            exchangeRateTypeName, oriCurrencyName, natCurrencyName));
                                }
                                if (!ValueUtils.isNotEmptyObj(bizObj.get("swapOutExchangeRateEstimate"))) {
                                    bizObj.set("swapOutExchangeRateEstimate", cmpExchangeRateVO.getExchangeRate());
                                    bizObj.set("swapOutExchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
                                    bizObj.set("exchangeRateOps", bizObject.get(ICmpConstant.EXCHRATEOPS));
                                }
                            }else{
                                bizObj.set("exchangeRateOps", 1);
                            }
                        }
//                        if (!ValueUtils.isNotEmptyObj(bizObj.get("swapOutAmountEstimate"))) {
                            BigDecimal oriSum = bizObj.getBigDecimal(ICmpConstant.ORISUM);
                            BigDecimal swapOutAmountEstimate = BigDecimal.ZERO;
                            if (bizObj.getInteger("exchangeRateOps") == 1) {
                                swapOutAmountEstimate = BigDecimalUtils
                                        .safeMultiply(oriSum, bizObj.getBigDecimal("swapOutExchangeRateEstimate"))
                                        .setScale(8, BigDecimal.ROUND_DOWN);
                            } else {
                                swapOutAmountEstimate = BigDecimalUtils
                                        .safeDivide(oriSum, bizObj.getBigDecimal("swapOutExchangeRateEstimate"), ICmpConstant.CONSTANT_TEN)
                                        .setScale(8, BigDecimal.ROUND_DOWN);
                            }
                            bizObj.set("swapOutAmountEstimate", swapOutAmountEstimate);
//                        }
                    }
                }
            }
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
            bizObject_bList = bizObject.getBizObjects("FundCollection_b", BizObject.class);
            for (BizObject bizObj : bizObject_bList) {
                Object settlemode = bizObj.get("settlemode");
                Integer serviceAttr = -1;
                if (ValueUtils.isNotEmptyObj(settlemode)) {
                    SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                    settleMethodQueryParam.setId(bizObj.get("settlemode"));
                    settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
                    settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                    List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                    if (CollectionUtils.isNotEmpty(dataList)) {
                        serviceAttr = dataList.get(0).getServiceAttr();
                    }
                }
                String settleCurrency = bizObj.getString("settleCurrency");
                if (!ValueUtils.isNotEmptyObj(settleCurrency) || !ValueUtils.isNotEmptyObj(settlemode) || serviceAttr != 0) {
                    bizObj.set("settleCurrency", bizObject.get(ICmpConstant.CURRENCY));
                    bizObj.set("swapOutExchangeRateType", bizObject.get("exchangeRateType"));
                    bizObj.set("swapOutAmountEstimate", bizObj.get(ICmpConstant.ORISUM));
                    bizObj.set("swapOutExchangeRateEstimate", new BigDecimal("1"));
                } else {
                    if (!ValueUtils.isNotEmptyObj(bizObj.get("swapOutExchangeRateType"))) {
                        bizObj.set("swapOutExchangeRateType", bizObject.get(ICmpConstant.EXCHANGE_RATE_TYPE));
                    }
                    if (bizObject.get(ICmpConstant.CURRENCY).equals(settleCurrency)) {
                        bizObj.set("swapOutExchangeRateEstimate", new BigDecimal("1"));
                    } else {
                        boolean notDefineExchangeRateTypeB = bizObj.get("swapOutExchangeRateType_code") == null || (bizObj.get("swapOutExchangeRateType_code") != null && !bizObj.get("swapOutExchangeRateType_code").toString().equals("02"));
                        if(notDefineExchangeRateTypeB){
                            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(
                                    bizObject.get(ICmpConstant.CURRENCY),
                                    settleCurrency,
                                    bizObject.get(ICmpConstant.VOUCHDATE),
                                    bizObj.get("swapOutExchangeRateType"));
                            if (cmpExchangeRateVO == null) {
                                String oriCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(bizObject.get(ICmpConstant.CURRENCY)).getName();
                                String natCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(settleCurrency).getName();
                                String exchangeRateTypeName = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateTypeById(bizObj.get("swapOutExchangeRateType")).getName();
                                throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                        exchangeRateTypeName, oriCurrencyName, natCurrencyName));
                            }
                            if (!ValueUtils.isNotEmptyObj(bizObj.get("swapOutExchangeRateEstimate"))) {
                                bizObj.set("swapOutExchangeRateEstimate", cmpExchangeRateVO.getExchangeRate());
                                bizObj.set("swapOutExchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
                                bizObj.set("exchangeRateOps", bizObject.get(ICmpConstant.EXCHRATEOPS));
                            }
                        }else{
                            bizObj.set("exchangeRateOps", 1);
                        }
                    }
//                    if (!ValueUtils.isNotEmptyObj(bizObj.get("swapOutAmountEstimate"))) {
                        BigDecimal oriSum = bizObj.getBigDecimal(ICmpConstant.ORISUM);
                        BigDecimal swapOutAmountEstimate = BigDecimal.ZERO;
                        if (bizObj.getInteger("exchangeRateOps") == 1) {
                            swapOutAmountEstimate = BigDecimalUtils
                                    .safeMultiply(oriSum, bizObj.getBigDecimal("swapOutExchangeRateEstimate"))
                                    .setScale(8, BigDecimal.ROUND_DOWN);
                        } else {
                            swapOutAmountEstimate = BigDecimalUtils
                                    .safeDivide(oriSum, bizObj.getBigDecimal("swapOutExchangeRateEstimate"), ICmpConstant.CONSTANT_TEN)
                                    .setScale(8, BigDecimal.ROUND_DOWN);
                        }

                        bizObj.set("swapOutAmountEstimate", swapOutAmountEstimate);
//                    }
                }
            }
        }
        int lineno = 1;
        for (BizObject bizObj : bizObject_bList) {
            if (bizObj.get("issynergy") == null) {
                bizObj.set("issynergy", false);
            }
            if (bizObj.get("isOccupyBudget") == null) {
                bizObj.set("isOccupyBudget", ICmpConstant.CONSTANT_ZERO);
            }
            if (bizObj.get("isToPushCspl") == null) {
                bizObj.set("isToPushCspl", ICmpConstant.CONSTANT_ZERO);
            }
            if (bizObj.get("whetherSettle") == null) {
                bizObj.set("whetherSettle", ICmpConstant.CONSTANT_ONE);
            }
            if (bizObj.get("lineno") == null && EntityStatus.Insert.equals(bizObject.getEntityStatus())) {
                bizObj.set("lineno", new BigDecimal(lineno * 10));
                lineno++;
            }
        }

        if (EntityStatus.Insert.equals(bizObject.getEntityStatus())) {
            HashMap<String, Object> cloneBizObject = SerializationUtils.clone((HashMap<String, Object>) bizObject);
            IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
            String mdUri = null;
            if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                mdUri = FundPayment.ENTITY_NAME;
            } else if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
                mdUri = FundCollection.ENTITY_NAME;
            }

            BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(
                    BizObjCodeUtils.getBizObjCodeByBillNumAndDomain(billNum, "ctm-ctmp"),
                    billNum,
                    InvocationInfoProxy.getTenantid(),
                    ValueUtils.isNotEmptyObj(bizObject.get("org")) ? bizObject.get("org") : bizObject.get("accentity"),
                    mdUri,
                    new BillCodeObj[]{new BillCodeObj(cloneBizObject)}
            );
            billCodeComponentParam.setIsGetRealNo(false);
            BillCodeContext billCodeContext = billCodeComponentService.getBillCodeContext(billCodeComponentParam);
            Integer billnumMode = billCodeContext.getBillnumMode();
            if (billnumMode == 1) {
                String[] batchBillCodes = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
                bizObject.put("code", batchBillCodes[0]);
            }
        }
        if (EntityStatus.Update.equals(bizObject.getEntityStatus())) {
            BizObject bizObj = MetaDaoHelper.findById(bizObject.getEntityName(), bizObject.getId());
            bizObject.put("code", bizObj.get("code"));
            bizObject.put(IBussinessConstant.ACCENTITY, bizObj.get(IBussinessConstant.ACCENTITY));
        }
    }

    /**
     * 处理结算币种信息
     *
     * @param bizObject
     * @param billb
     * @throws Exception
     */
    private void handleSettleCurrency(BizObject bizObject, BizObject billb) throws Exception {

        if (StringUtils.isNotEmpty(billb.getString("swapOutExchangeRateType"))) {
            if (billb.getString("settleCurrency").equals(bizObject.get("currency"))) {
                billb.set("swapOutExchangeRateEstimate", new BigDecimal(1).setScale(8, BigDecimal.ROUND_DOWN));
                billb.set("swapOutExchangeRateOps", 1);
                billb.set("swapOutAmountEstimate", BigDecimalUtils
                        .safeMultiply(billb.getBigDecimal("oriSum"), billb.getBigDecimal("swapOutExchangeRateEstimate"))
                        .setScale(8, BigDecimal.ROUND_DOWN));
            } else {
                boolean notDefineExchangeRateTypeB = billb.get("swapOutExchangeRateType_code") == null || (billb.get("swapOutExchangeRateType_code") != null && !billb.get("swapOutExchangeRateType_code").toString().equals("02"));
                if(notDefineExchangeRateTypeB){
                    CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(
                            bizObject.get(ICmpConstant.CURRENCY),
                            billb.getString("settleCurrency"),
                            bizObject.get(ICmpConstant.VOUCHDATE),
                            billb.get("swapOutExchangeRateType"));
                    if (cmpExchangeRateVO == null) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A1A604C00009", "未查询到汇率，请检查！") /* "未查询到汇率，请检查！" */);
                    }
                    billb.set("swapOutExchangeRateEstimate", cmpExchangeRateVO.getExchangeRate().setScale(8, BigDecimal.ROUND_DOWN));
                    if (cmpExchangeRateVO.getExchangeRateOps() == 1) {
                        billb.set("swapOutAmountEstimate", BigDecimalUtils
                                .safeMultiply(billb.getBigDecimal("oriSum"), billb.getBigDecimal("swapOutExchangeRateEstimate"))
                                .setScale(8, BigDecimal.ROUND_DOWN));
                    } else {
                        billb.set("swapOutAmountEstimate", BigDecimalUtils
                                .safeDivide(billb.getBigDecimal("oriSum"), billb.getBigDecimal("swapOutExchangeRateEstimate"), 8)
                                .setScale(8, BigDecimal.ROUND_DOWN));
                    }
                    billb.set("swapOutExchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
                }else{
                    billb.set("swapOutExchangeRateOps", 1);
                    if (Objects.nonNull(billb.get("swapOutExchangeRateEstimate"))) {
                        billb.set("swapOutAmountEstimate", BigDecimalUtils
                                .safeMultiply(billb.getBigDecimal("oriSum"), billb.getBigDecimal("swapOutExchangeRateEstimate"))
                                .setScale(8, BigDecimal.ROUND_DOWN));
                    }
                }
            }
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A1A604C0000A", "结算币种不为空时，换出汇率类型不能为空！") /* "结算币种不为空时，换出汇率类型不能为空！" */);
        }
    }


    private CurrencyTenantDTO getCurrencyMapByID(String currency) throws Exception {
        if (currencyCacheMap != null && currencyCacheMap.get(currency) != null) {
            return currencyCacheMap.get(currency);
        } else {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
            if (currencyTenantDTO != null) {
                currencyCacheMap.put(currency, currencyTenantDTO);
                return currencyCacheMap.get(currency);
            }
        }
        return null;
    }

    private void beforeCheck(BizObject bizObject, String billnum, Map<String, String> fieldName, String permissionKey) throws Exception {
        if (StringUtils.isBlank(bizObject.get(IBussinessConstant.ACCENTITY))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102059"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080C", "会计主体字段为必输项，请检查") /* "会计主体字段为必输项，请检查" */);
        }

        String serviceCode = null;
        List<BizObject> bizObject_bList = new ArrayList<>();
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundPayment_b", BizObject.class);
            serviceCode = IServicecodeConstant.FUNDPAYMENT;
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundCollection_b", BizObject.class);
            serviceCode = IServicecodeConstant.FUNDCOLLECTION;
        }

        Set<String> orgPermVO = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
        if (CollectionUtils.isNotEmpty(orgPermVO)) {
            String accentity = bizObject.getString(IBussinessConstant.ACCENTITY);
            if (!orgPermVO.contains(accentity)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102060"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080099", "当前用户没有此会计主体的权限") /* "当前用户没有此会计主体的权限" */);
            }
        }
        // 判断是否启用商业汇票
        CtmJSONObject enableBsdModule = null;
        try {
            enableBsdModule = fundCommonService.isEnableBsdModule(bizObject.get(IBussinessConstant.ACCENTITY));
        } catch (Exception e) {
            log.error("get BSD is enabled fail!, e = {}", e.getMessage());
        }
        boolean flag = false;
        if (ValueUtils.isNotEmptyObj(enableBsdModule)) {
            flag = org.apache.commons.collections4.MapUtils.getBoolean(enableBsdModule, "isEnabled");
        }
        bizObject.set("isEnabledBsd", flag);
        // 校验主表结算方式
        if (ValueUtils.isNotEmptyObj(bizObject.get("settlemode"))) {
            checkFundBillParentSettleMode(bizObject, fieldName, permissionKey);
        }
        // 校验主表数据权限
        checkMainDataPermission(bizObject, fieldName, permissionKey);
        // 校验业务组织权限
        CmpCommonUtil.checkAuthBusOrg(bizObject.getString("org"), bizObject.get(IBussinessConstant.ACCENTITY));
        // 校验主表项目和费用项目职能共享和代理权限
        checkProjectAndExpenseItem(bizObject);
        // 校验主表部门职能共享和代理权限
//        CmpCommonUtil.checkdept(bizObject.getString("dept"), bizObject.get(IBussinessConstant.ACCENTITY));
        // 校验主表业务员 职能共享和代理权限
//        CmpCommonUtil.checkEmployee(bizObject.getString("operator"), bizObject.get(IBussinessConstant.ACCENTITY),bizObject.getString("dept"), true);
        Boolean checkStockCanUse = autoConfigService.getCheckStockCanUse();
        // 子表数据校验
        if (ValueUtils.isNotEmptyObj(bizObject_bList)) {
            Map<String, Integer> cacheMap = new HashMap<>();
            for (BizObject child : bizObject_bList) {
                // 数据权限校验
                Map<String, Set<String>> stringSetMap = dataPermissionMap.get(permissionKey);

                // 1.项目
                if (ValueUtils.isNotEmptyObj(child.get("project")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
                    String caobjectKey = "project";
                    Set<String> set = stringSetMap.get(caobjectKey);
                    if (ValueUtils.isNotEmptyObj(set) && !set.contains(child.get(caobjectKey).toString())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                    }
                }
                // 2.费用项目
                if (ValueUtils.isNotEmptyObj(child.get("expenseitem")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
                    String caobjectKey = "expenseitem";
                    Set<String> set = stringSetMap.get(caobjectKey);
                    if (ValueUtils.isNotEmptyObj(set) && !set.contains(child.get(caobjectKey).toString())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                    }
                }
                // 3.业务员
                if (ValueUtils.isNotEmptyObj(child.get("operator")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
                    String caobjectKey = "operator";
                    Set<String> set = stringSetMap.get(caobjectKey);
                    if (ValueUtils.isNotEmptyObj(set) && !set.contains(child.get(caobjectKey).toString())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                    }
                }
                // 4.部门
                if (ValueUtils.isNotEmptyObj(child.get("dept")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
                    String caobjectKey = "dept";
                    Set<String> set = stringSetMap.get(caobjectKey);
                    if (ValueUtils.isNotEmptyObj(set) && !set.contains(child.get(caobjectKey).toString())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                    }
                }
                //校验子表部门职能共享和代理权限
//                CmpCommonUtil.checkdept(child.get("dept"), bizObject.get(IBussinessConstant.ACCENTITY));
                //校验子表业务员职能共享和代理权限
//                CmpCommonUtil.checkEmployee(child.getString("operator"), bizObject.get(IBussinessConstant.ACCENTITY), child.getString("dept"), true);
                child.set(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
                checkProjectAndExpenseItem(child);
                BillCopyCheckService.checkQuickType(child, cacheMap);
                if (!Objects.isNull(child.get("settlemode")) && child.get("settlemode").toString().trim().length() > 0) {
                    SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                    settleMethodQueryParam.setId(child.get("settlemode"));
                    settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
                    settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                    List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                    //结算方式
                    if (dataList != null && dataList.size() > 0) {
                        if (dataList.get(0).getServiceAttr() == 0) {
                            if (child.get("enterprisebankaccount") != null) {
                                //银行账户
                                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(child.get("enterprisebankaccount"));
                                Integer enable = enterpriseBankAcctVO.getEnable();
                                if (1 != enable) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102062"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FF", "银行账号为:") /* "银行账号为:" */ + enterpriseBankAcctVO.getAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180800", "的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
                                }
                                CmpCommonUtil.checkBankAcctCurrency(child.get("enterprisebankaccount"), child.get("settleCurrency"));
                                EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(child.get("enterprisebankaccount"));
                                if (enterpriseBankAcctVoWithRange != null) {
                                    List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                                    // 使用范围中的组织是否是授权的组织
                                    String accentity = bizObject.getString(IBussinessConstant.ACCENTITY);
                                    Set<String> rangeOrgIdList = new HashSet<>();
                                    for (OrgRangeVO orgRangeVO : orgRangeVOS) {
                                        String rangeOrgId = orgRangeVO.getRangeOrgId();
                                        rangeOrgIdList.add(rangeOrgId);
                                    }
                                    if (!rangeOrgIdList.contains(accentity)) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101898"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180801", "银行账号与会计主体不匹配！") /* "银行账号与会计主体不匹配！" */);
                                    }
                                }
                                if (ValueUtils.isNotEmptyObj(stringSetMap)) {
                                    String caobjectKey = "enterprisebankaccount";
                                    Set<String> set = stringSetMap.get(caobjectKey);
                                    if (ValueUtils.isNotEmptyObj(set) && !set.contains(child.get(caobjectKey).toString())) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                                    }
                                }
                            }
                            child.set("cashaccount", null);
                            child.set("notetype", null);
                            child.set("thirdParVirtAccount", null);
                            child.set("checkId", null);
                            child.set("checkno", null);
                        } else if (dataList.get(0).getServiceAttr() == 1) {
                            if (child.get("cashaccount") != null) {
                                //现金账户
                                EnterpriseCashVO cashAccount = baseRefRpcService.queryEnterpriseCashAcctById(child.get("cashaccount"));
                                if (!cashAccount.getOrgid().equals(bizObject.get(IBussinessConstant.ACCENTITY))) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102063"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180808", "导入的现金账户所属会计主体与导入会计主体不一致！") /* "导入的现金账户所属会计主体与导入会计主体不一致！" */);
                                }
                                if (ValueUtils.isNotEmptyObj(stringSetMap)) {
                                    String caobjectKey = "cashaccount";
                                    Set<String> set = stringSetMap.get(caobjectKey);
                                    if (ValueUtils.isNotEmptyObj(set) && !set.contains(child.get(caobjectKey).toString())) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                                    }
                                }
                            }
                            child.set("enterprisebankaccount", null);
                            child.set("notetype", null);
                            child.set("thirdParVirtAccount", null);
                            child.set("checkId", null);
                            child.set("checkno", null);
                        } else if (dataList.get(0).getServiceAttr() == 2) {
                            child.set("enterprisebankaccount", null);
                            child.set("cashaccount", null);
                            child.set("thirdParVirtAccount", null);
                            child.set("checkId", null);
                            child.set("checkno", null);
                            if (bizObject.getBoolean("isEnabledBsd").booleanValue()) {
                                if (child.get("notetextno") != null || child.get("noteDirection") != null || child.get("noteSum") != null) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102064"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080D", "在启用商业汇票的情况下，票据文本号或票据方向或票据金额不允许填写！") /* "在启用商业汇票的情况下，票据文本号或票据方向或票据金额不允许填写！" */);
                                }
                            } else {
                                if (child.get("notetype") != null || child.get("noteno") != null) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102065"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080E", "在未启用商业汇票的情况下，票据类型或票据号不允许填写！") /* "在未启用商业汇票的情况下，票据类型或票据号不允许填写！" */);
                                }
                            }
                            if (bizObject.getBoolean("isEnabledBsd").booleanValue() && child.get("noteno") != null) {
                                String noteNoKey = child.get("noteno").toString();
                                if (noteNoMap == null || MapUtils.isEmpty(noteNoMap.get(noteNoKey))) {
                                    List<Map<String, Object>> notenoList = QueryBaseDocUtils.getBillNoByNoteNo(child.get("noteno"));
                                    if (notenoList.size() > 0 && ValueUtils.isNotEmptyObj(notenoList.get(0))) {
                                        Map<String, Object> map = new HashMap<>();
                                        Map<String, Object> stringObjectMap = notenoList.get(0);
                                        map.put("noteDirection", stringObjectMap.get("billdirection"));
                                        map.put("noteSum", stringObjectMap.get("notemoney"));
                                        noteNoMap.put(noteNoKey, map);
                                    }
                                }
                                child.set("noteDirection", noteNoMap.get(noteNoKey).get("noteDirection"));
                                child.set("noteSum", noteNoMap.get(noteNoKey).get("noteSum"));
                            }
                        } else if (dataList.get(0).getServiceAttr() == 10) {
                            if (child.get("thirdParVirtAccount") != null) {
                                //第三方虚拟账户
                                BillContext context = new BillContext();
                                context.setFullname("TMSP.threvirtualaccount.VirtualAccount");
                                context.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
                                QuerySchema schema = QuerySchema.create();
                                schema.addSelect("id as id, accentity as accentity");
                                schema.appendQueryCondition(QueryCondition.name("id").eq(child.get("thirdParVirtAccount")));
                                log.error("getObjectContent, schema = {}", schema);
                                List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);
                                //获取虚拟账户信息
                                if (CollectionUtils.isNotEmpty(result)) {
                                    Object accentity = result.get(0).get("accentity");
                                    if (!accentity.equals(bizObject.get(IBussinessConstant.ACCENTITY))) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102066"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418081C", "导入的虚拟账户所属会计主体与导入会计主体不一致！") /* "导入的虚拟账户所属会计主体与导入会计主体不一致！" */);
                                    }
                                }
                            }
                            child.set("cashaccount", null);
                            child.set("enterprisebankaccount", null);
                            child.set("notetype", null);
                            child.set("checkId", null);
                            child.set("checkno", null);
                        } else if (dataList.get(0).getServiceAttr() == 8) {
                            child.set("cashaccount", null);
                            child.set("notetype", null);
                            child.set("noteno", null);
                            String checkno = child.getString("checkno");
                            if (ValueUtils.isNotEmptyObj(checkno)) {
                                // 根据支票id获取对应支票
                                QuerySchema queryCheck = QuerySchema.create().addSelect("amount", "occupy", "id", "checkBillDir", "currency", "checkBillType", "checkBillStatus", "drawerAcct", "payBank");
                                queryCheck.appendQueryCondition(QueryCondition.name("checkBillNo").eq(checkno));
                                List<Map<String, Object>> checkStockList = MetaDaoHelper.query("cmp.checkstock.CheckStock", queryCheck);
                                if (org.apache.commons.collections4.CollectionUtils.isEmpty(checkStockList)) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102067"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080098", "所选支票号在支票档案里未查询到，请检查！") /* "所选支票号在支票档案里未查询到，请检查！" */);
                                }
                                BizObject checkStock = new BizObject(checkStockList.get(0));

                                if (ValueUtils.isNotEmptyObj(checkStock.getShort("occupy")) && checkStock.getShort("occupy") == 1) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102068"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009A", "所选支票号已被占用，请检查！") /* "所选支票号已被占用，请检查！" */);
                                }
                                if (Objects.isNull(checkStock.get("checkBillDir")) || Objects.isNull(checkStock.get("currency")) || Objects.isNull(checkStock.get("checkBillType")) || Objects.isNull(checkStock.get("checkBillStatus") != null)) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102069"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009B", "所选支票不满足业务要求，请检查") /* "所选支票不满足业务要求，请检查" */);
                                }
                                if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                                    if (!"2".equals(checkStock.getString("checkBillDir"))) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102069"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009B", "所选支票不满足业务要求，请检查") /* "所选支票不满足业务要求，请检查" */);
                                    }
                                    //未开启领用参数时，只能导入状态为已入库的支票；开启参数时，只能导入状态为已领用的支票
                                    boolean statusCheck = (checkStockCanUse && "13".equals(checkStock.getString("checkBillStatus"))) || (!checkStockCanUse && "1".equals(checkStock.getString("checkBillStatus")));
                                    if (!statusCheck) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102069"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009B", "所选支票不满足业务要求，请检查") /* "所选支票不满足业务要求，请检查" */);
                                    }
                                    //将支票的付款银行id赋值给企业银行账户.ID
                                    child.set("enterprisebankaccount", checkStock.getString("drawerAcct"));
                                } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                                    if (!"1".equals(checkStock.getString("checkBillDir"))) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102069"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009B", "所选支票不满足业务要求，请检查") /* "所选支票不满足业务要求，请检查" */);
                                    }
                                    BigDecimal amount = checkStock.getBigDecimal("amount");
                                    BigDecimal oriSum = child.getBigDecimal(ICmpConstant.ORISUM);
                                    if (amount.compareTo(oriSum) != 0) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102070"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009C", "资金收款单导入支票时，支票金额应等于单据金额！") /* "资金收款单导入支票时，支票金额应等于单据金额！" */);
                                    }
                                    if (ValueUtils.isEmpty(child.getString("enterprisebankaccount"))) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102071"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009D", "资金收款单导入支票时，收款银行账户必填！") /* "资金收款单导入支票时，收款银行账户必填！" */);
                                    }
                                    //收票时  只允许已入库的
                                    if (!"1".equals(checkStock.getString("checkBillStatus"))) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102069"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009B", "所选支票不满足业务要求，请检查") /* "所选支票不满足业务要求，请检查" */);
                                    }
                                }
                                if (checkStock.getString("currency").equals(bizObject.get(ICmpConstant.CURRENCY))) {
                                    child.set("noteDirection", IBillNumConstant.FUND_COLLECTION.equals(billnum) ? NoteDirection.CollectionCheckNo.getValue() : NoteDirection.PaymentCheckNo.getValue());
                                    child.set("noteSum", child.getBigDecimal(ICmpConstant.ORISUM));
                                    // 支票用途赋值为转账
                                    child.set("checkPurpose", CheckPurpose.BankToVirtual.getValue());
                                    child.set("checkId", checkStock.getId().toString());
                                } else {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102069"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009B", "所选支票不满足业务要求，请检查") /* "所选支票不满足业务要求，请检查" */);
                                }
                            }
                        } else {
                            if (dataList.get(0).getServiceAttr() != 8) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101290"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180822", "结算方式业务属性只能为票据结算、银行业务、现金业务和第三方！") /* "结算方式业务属性只能为票据结算、银行业务、现金业务和第三方！" */);
                            }
                        }
                    }
                }
            }
            cacheMap.clear();
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102072"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080096", "单据的明细行不能为空，请检查手工码是否配置正确！") /* "单据的明细行不能为空，请检查手工码是否配置正确！" */);
        }
    }

    private void checkMainDataPermission(BizObject bizObject, Map<String, String> fieldName, String permissionKey) {
        // 数据权限校验
        Map<String, Set<String>> stringSetMap = dataPermissionMap.get(permissionKey);
        // 1.项目
        if (ValueUtils.isNotEmptyObj(bizObject.get("project")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
            String caobjectKey = "project";
            Set<String> set = stringSetMap.get(caobjectKey);
            if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject.get(caobjectKey).toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
            }
        }
        // 2.费用项目
        if (ValueUtils.isNotEmptyObj(bizObject.get("expenseitem")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
            String caobjectKey = "expenseitem";
            Set<String> set = stringSetMap.get(caobjectKey);
            if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject.get(caobjectKey).toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
            }
        }
        // 3.业务员
        if (ValueUtils.isNotEmptyObj(bizObject.get("operator")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
            String caobjectKey = "operator";
            Set<String> set = stringSetMap.get(caobjectKey);
            if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject.get(caobjectKey).toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
            }
        }
        // 4.部门
        if (ValueUtils.isNotEmptyObj(bizObject.get("dept")) && ValueUtils.isNotEmptyObj(stringSetMap)) {
            String caobjectKey = "dept";
            Set<String> set = stringSetMap.get(caobjectKey);
            if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject.get(caobjectKey).toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
            }
        }
    }

    //TODO 校验主表结算方式的方法里又加了项目，费用项目，业务员，部门的校验
    private void checkFundBillParentSettleMode(BizObject bizObject, Map<String, String> fieldName, String permissionKey) throws Exception {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setId(bizObject.get("settlemode"));
        settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        // 数据权限校验
        Map<String, Set<String>> stringSetMap = dataPermissionMap.get(permissionKey);
        //结算方式
        if (dataList != null && dataList.size() > 0) {
            if (dataList.get(0).getServiceAttr() == 0) {
                if (bizObject.get("enterprisebankaccount") != null) {
                    //银行账户
                    EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));
                    Integer enable = enterpriseBankAcctVO.getEnable();
                    if (1 != enable) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102062"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FF", "银行账号为:") /* "银行账号为:" */ + enterpriseBankAcctVO.getAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180800", "的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
                    }
                    CmpCommonUtil.checkBankAcctCurrency(bizObject.get("enterprisebankaccount"), bizObject.get("currency"));

                    EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.get("enterprisebankaccount"));
                    if (enterpriseBankAcctVoWithRange != null) {
                        List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                        // 使用范围中的组织是否是授权的组织
                        String accentity = bizObject.getString(IBussinessConstant.ACCENTITY);
                        Set<String> rangeOrgIdList = new HashSet<>();
                        for (OrgRangeVO orgRangeVO : orgRangeVOS) {
                            String rangeOrgId = orgRangeVO.getRangeOrgId();
                            rangeOrgIdList.add(rangeOrgId);
                        }
                        if (!rangeOrgIdList.contains(accentity)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101898"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180801", "银行账号与会计主体不匹配！") /* "银行账号与会计主体不匹配！" */);
                        }
                    }

                    if (ValueUtils.isNotEmptyObj(stringSetMap)) {
                        String caobjectKey = "enterprisebankaccount";
                        Set<String> set = stringSetMap.get(caobjectKey);
                        if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject.get(caobjectKey).toString())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                        }
                    }
                }
                bizObject.set("cashaccount", null);
                bizObject.set("notetype", null);
            } else if (dataList.get(0).getServiceAttr() == 1) {
                if (bizObject.get("cashaccount") != null) {
                    //现金账户
                    EnterpriseCashVO cashAccount = baseRefRpcService.queryEnterpriseCashAcctById(bizObject.get("cashaccount"));
                    if (!cashAccount.getOrgid().equals(bizObject.get(IBussinessConstant.ACCENTITY))) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102063"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180808", "导入的现金账户所属会计主体与导入会计主体不一致！") /* "导入的现金账户所属会计主体与导入会计主体不一致！" */);
                    }
                    if (ValueUtils.isNotEmptyObj(stringSetMap)) {
                        String caobjectKey = "cashaccount";
                        Set<String> set = stringSetMap.get(caobjectKey);
                        if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject.get(caobjectKey).toString())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                        }
                    }
                }
                bizObject.set("enterprisebankaccount", null);
                bizObject.set("notetype", null);
            } else if (dataList.get(0).getServiceAttr() == 2) {
                bizObject.set("enterprisebankaccount", null);
                bizObject.set("cashaccount", null);
                if (!(bizObject.getBoolean("isEnabledBsd").booleanValue())) {
                    if (bizObject.get("notetype") != null || bizObject.get("noteno") != null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102065"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080E", "在未启用商业汇票的情况下，票据类型或票据号不允许填写！") /* "在未启用商业汇票的情况下，票据类型或票据号不允许填写！" */);
                    }
                }
            } else {
                if (dataList.get(0).getServiceAttr() != 8) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102073"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180811", "结算方式业务属性只能为票据结算、支票结算、银行业务与现金业务！") /* "结算方式业务属性只能为票据结算、银行业务与现金业务！" */);
                }
            }
        }
    }

    /**
     * 处理收款单位名称id，收款单位名称（客户、供应商、员工参照）
     */
    private void dealCustSuppRefAndCalculateAmount(BizObject bizObject, String billnum, Map<String, String> fieldName, String permissionKey) throws Exception {
        List<BizObject> bizObject_bList = new ArrayList<>();
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundPayment_b", BizObject.class);
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundCollection_b", BizObject.class);
        }
        if (CollectionUtils.isEmpty(bizObject_bList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102075"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080A", "导入数据表体信息不能为空，请检查") /* "导入数据表体信息不能为空，请检查" */);
        }
        // 币种精度
        Short moneyDigit = Short.valueOf(bizObject.get("currency_moneyDigit").toString());
        Short olcmoneyDigit = Short.valueOf(bizObject.get("natCurrency_moneyDigit").toString());

        BigDecimal exchRate = new BigDecimal(bizObject.get("exchRate").toString());
        BigDecimal oriAmount = BigDecimal.ZERO; // 原币总金额
        BigDecimal natAmount = BigDecimal.ZERO; // 本币总金额
        bizObject.set("oriSum", oriAmount);
        bizObject.set("natSum", natAmount);

        //校验业务组织权限  要根据会计主体查询有核算委托关系的业务组织集合
        Set<String> orgList = new HashSet<>();//有核算委托关系的组织id集合
        String accentity = bizObject.get(IBussinessConstant.ACCENTITY).toString();
        log.error("1.AfterImportFundBillRule,accentity={}", accentity);
        orgList.add(accentity);
        Set<String> orgidtsTmp = FIDubboUtils.getDelegateHasSelf(accentity);
        log.error("2.AfterImportFundBillRule,delegateHasSelf={}", orgidtsTmp);
        orgList.addAll(orgidtsTmp);
        String org = bizObject.get(IBillConst.ORG);
        if (org != null) {
            orgList.add(org);
            orgList.remove(accentity);
            if (org.equals(accentity)) {
                orgList.clear();
                orgList.add(accentity);
            }
        }
        List<String> listSharingOrgUnitIdsByServeScopeOrgIdList = orgFuncSharingSettingApi
                .listSharingOrgUnitIdsByServeScopeOrgIdList(
                        Collections.singletonList(bizObject.getString(IBussinessConstant.ACCENTITY)),
                        InvocationInfoProxy.getTenantid(),
                        "diwork",
                        OrgFuncSharingSettingQryParam.queryAll());
        log.error("3.AfterImportFundBillRule,listSharingOrgUnitIdsByServeScopeOrgIdList={}", listSharingOrgUnitIdsByServeScopeOrgIdList);
        orgList.addAll(listSharingOrgUnitIdsByServeScopeOrgIdList);
        orgList.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
        if (!StringUtils.isEmpty(bizObject.get("org"))) {
            if (!orgList.contains(bizObject.get("org").toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102056"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180829", "只允许导入与当前会计主体存在核算委托关系的组织!") /* "只允许导入与当前会计主体存在核算委托关系的组织!" */);
            }
        }

        for (BizObject bizObject_b : bizObject_bList) {
            //计算金额
            calculateAmount(bizObject, bizObject_b, moneyDigit, olcmoneyDigit, exchRate);
            // 赋值表体部门、业务员、项目、费用项目
            if (bizObject_b.get("project") == null) {
                bizObject_b.set("project", bizObject.get("project"));
            }
            if (bizObject_b.get("expenseitem") == null) {
                bizObject_b.set("expenseitem", bizObject.get("expenseitem"));
            }
            if (bizObject_b.get("dept") == null) {
                bizObject_b.set("dept", bizObject.get("dept"));
            }
            if (bizObject_b.get("operator") == null) {
                bizObject_b.set("operator", bizObject.get("operator"));
            }
            collectionToBeTranslated(bizObject, bizObject_b, billnum, orgList);
        }
        // 赋值收款单位id
        processTranslateData(fieldName, permissionKey, bizObject_bList);
    }

    /**
     * 计算金额、税额
     *
     * @param bizObject
     * @param bizObject_b
     * @param moneyDigit
     * @param olcmoneyDigit
     * @param exchRate
     * @param oriAmount
     * @param natAmount
     * @throws Exception
     */
    private BizObject calculateAmount(BizObject bizObject, BizObject bizObject_b, Short moneyDigit, Short olcmoneyDigit, BigDecimal exchRate) throws Exception {
        // 赋值表体字段
        bizObject_b.set("currency", bizObject.get("currency"));
        bizObject_b.set("exchangeRateType", bizObject.get("exchangeRateType"));
        bizObject_b.set("exchRate", bizObject.get("exchRate"));
        bizObject_b.set("isIncomeAndExpenditure", false);
        bizObject_b.set("whetherRefundAndRepayment", 0);
        bizObject_b.set("entrustReject", 0);
        //原币金额
        if (bizObject_b.get(IBussinessConstant.ORI_SUM) == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102076"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F7", "原币金额不能为空！") /* "原币金额不能为空！" */);
        }
        BigDecimal oriSum = new BigDecimal(bizObject_b.get(IBussinessConstant.ORI_SUM).toString()).setScale(moneyDigit, BigDecimal.ROUND_HALF_UP);
        if (StringUtils.isBlank(bizObject_b.get("oppositeobjectname"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102077"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FE", "收付款单位名称不能为空！") /* "收付款单位名称不能为空！" */);
        }
        String dividOriginalcurrencyamt = oriSum.toPlainString();
        int index = dividOriginalcurrencyamt.indexOf(".");
        if (index != -1) {
            String decPointAfterString = dividOriginalcurrencyamt.substring(index + 1, dividOriginalcurrencyamt.length());
            if (decPointAfterString.length() > IStwbConstant.SELECT_TWO_PARAM) {
                String substringOriginalcurrencyamt = decPointAfterString.substring(2, decPointAfterString.length());
                if (new BigDecimal(substringOriginalcurrencyamt).compareTo(BigDecimal.ZERO) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102078"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180802", "原币金额需要满足小数点后第3位（含）均为0") /* "原币金额需要满足小数点后第3位（含）均为0" */);
                }
            }
        }

        // 处理本币金额计算，表头合计金额
        // 本币金额
        short exchangeRateOps = bizObject.getShort("exchangeRateOps");
        BigDecimal natSum;
        if (exchangeRateOps == 1) {
            natSum = BigDecimalUtils.safeMultiply(oriSum, exchRate, olcmoneyDigit);
        } else {
            natSum = BigDecimalUtils.safeDivide(oriSum, exchRate, olcmoneyDigit);
        }
        bizObject_b.set(IBussinessConstant.ORI_SUM, oriSum);
        bizObject_b.set("natSum", natSum);
        // 计算表头原币总金额
        bizObject.set("oriSum", BigDecimalUtils.safeAdd(bizObject.getBigDecimal("oriSum"), oriSum));
        bizObject.set("natSum", BigDecimalUtils.safeAdd(bizObject.getBigDecimal("natSum"), natSum));
        if (!ValueUtils.isNotEmptyObj(bizObject_b.get("settlestatus"))) {
            bizObject_b.put("settlestatus", 1);
        } else {
            if (bizObject_b.get("settlestatus") instanceof String) {
                short settlestatus = Short.parseShort(bizObject_b.get("settlestatus").toString());
                bizObject_b.put("settlestatus", settlestatus);
            }
        }
        // 设置默认的结算状态 不可以设置默认值，支持导入的值为待结算或已结算补单
        short settlestatus = bizObject_b.getShort("settlestatus");
        if (ValueUtils.isNotEmptyObj(settlestatus)
                && settlestatus != FundSettleStatus.WaitSettle.getValue()
                && settlestatus != FundSettleStatus.SettlementSupplement.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102079"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180809", "结算状态不可为待结算或已结算补单外其他值") /* "结算状态不可为待结算或已结算补单外其他值" */);
        }

        // 计算税额
        if (ValueUtils.isNotEmptyObj(bizObject_b.get("taxCategory"))) {
            String taxCategory = bizObject_b.get("taxCategory").toString();
            log.info("bill save by openApi, taxCategory = {}", taxCategory);
            BdTaxRateVO taxRate = cmCommonService.getTaxRateById(taxCategory);
            if (taxRate != null) {
                Double ntaxRate = taxRate.getNtaxRate().doubleValue();
                if (new BigDecimal(ntaxRate).compareTo(BigDecimal.ZERO) == 0) {
                    bizObject_b.set("taxSum", BigDecimal.ZERO);
                    bizObject_b.set("taxRate", BigDecimal.ZERO);
                } else {
                    bizObject_b.set("taxRate", BigDecimal.valueOf(ntaxRate));
                    bizObject_b.set("taxSum", BigDecimalUtils.safeMultiply(BigDecimalUtils.safeMultiply(ntaxRate, ICmpConstant.CONSTANT_ZERO_POINT_ONE), bizObject_b.get("natSum")));
                }
            }
        }
        return bizObject;
    }

    /**
     * 翻译档案信息
     *
     * @param bizObject
     * @param bizObj
     * @param billNum
     * @param orgList
     * @throws Exception
     */
    private void collectionToBeTranslated(BizObject bizObject, BizObject bizObj, String billNum, Set<String> orgList) throws Exception {
        // 翻译资金计划项目
        String fundPlanProjectCode = ValueUtils.isNotEmptyObj(bizObj.get("fundPlanProject_code"))
                ? bizObj.getString("fundPlanProject_code") : null;
        String fundPlanProject = ValueUtils.isNotEmptyObj(bizObj.get("fundPlanProject"))
                ? bizObj.getString("fundPlanProject") : null;
        if (ValueUtils.isNotEmptyObj(fundPlanProjectCode)) {
            // 翻译资金计划档案
            translateFundPlanProject(bizObject, bizObj, fundPlanProjectCode);
        } else if (ValueUtils.isNotEmptyObj(fundPlanProject) && !ValueUtils.isNotEmptyObj(fundPlanProjectCode)) {
            // 校验资金计划项目权限,通过id查询一次
            translateFundPlanProject(bizObject, bizObj, fundPlanProject);
        }

        String oppositeObjectName = ValueUtils.isNotEmptyObj(bizObj.get("oppositeobjectname"))
                ? bizObj.getString("oppositeobjectname") : null;
        // 收方单位
        if (StringUtils.isBlank(oppositeObjectName) || bizObj.getShort("caobject") == null) {
            return;
        }
        if (CaObject.Customer.getValue() == bizObj.getShort("caobject")) {
            translateCustomer(bizObj, oppositeObjectName, orgList);
        } else if (CaObject.Supplier.getValue() == bizObj.getShort("caobject")) {
            translateSupplier(bizObj, oppositeObjectName, orgList);
        } else if (CaObject.Employee.getValue() == bizObj.getShort("caobject")) {
            translateEmployee(bizObj, oppositeObjectName, orgList);
        } else if (CaObject.CapBizObj.getValue() == bizObj.getShort("caobject")) {
            translateFundBusinessObj(bizObj, oppositeObjectName);
        } else if (CaObject.InnerUnit.getValue() == bizObj.getShort("caobject")) {
            innerUnitTranslate(bizObj, oppositeObjectName);
        } else if (CaObject.Other.getValue() == bizObj.getShort("caobject")) {
            String oppositeBankAddr = ValueUtils.isNotEmptyObj(bizObj.get("oppositebankaddr"))
                    ? bizObj.getString("oppositebankaddr") : null;
            translateBankDot(bizObj, oppositeBankAddr);
        }
    }

    private void processTranslateData(Map<String, String> fieldName, String permissionKey, List<BizObject> bizObjectBList) {
        for (BizObject bizObject_b : bizObjectBList) {
            short caObject = bizObject_b.getShort("caobject");
            Map<String, Set<String>> stringSetMap = dataPermissionMap.get(permissionKey);
            if (ValueUtils.isNotEmptyObj(stringSetMap)) {
                Set<String> set = new HashSet<>();
                String caobjectKey = null;
                if (CaObject.Customer.getValue() == caObject) {
                    set = stringSetMap.get(ICmpConstant.CUSTOMER);
                    caobjectKey = ICmpConstant.CUSTOMER;
                }
                if (CaObject.Supplier.getValue() == caObject) {
                    set = stringSetMap.get(IBussinessConstant.SUPPLIER);
                    caobjectKey = IBussinessConstant.SUPPLIER;
                }
                if (CaObject.Employee.getValue() == caObject) {
                    set = stringSetMap.get(ICmpConstant.EMPLOYEE);
                    caobjectKey = ICmpConstant.EMPLOYEE;
                }
                if (ValueUtils.isNotEmptyObj(set) && !set.contains(bizObject_b.get("oppositeobjectid").toString())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102061"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FA", "数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(caobjectKey) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FB", "无权") /* "无权" */);
                }
            }
        }
    }

    private void translateBankDot(BizObject bizObj, String oppositeBankAddr) throws Exception {
        // 查询资金业务对象档案
        if (!ValueUtils.isNotEmptyObj(oppositeBankAddr)) {
            return;
        }
        BillContext othersBillContext = new BillContext();
        othersBillContext.setFullname("bd.bank.BankDotVO");
        othersBillContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QueryConditionGroup bankDotCondition = QueryConditionGroup.and(QueryCondition.name("enable").eq("1"), QueryCondition.name("dr").eq("0"));
        //begin ctm wangzc 20240801 牧原根据导入开户行匹配增加联行号作为条件
        bankDotCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").eq(oppositeBankAddr), QueryCondition.name("name").eq(oppositeBankAddr), QueryCondition.name("linenumber").eq(oppositeBankAddr), QueryCondition.name("id").eq(oppositeBankAddr)));
        //end
        List<Map<String, Object>> bankDotDataList = MetaDaoHelper.queryAll(othersBillContext, "id,name,linenumber,bank.name", bankDotCondition, null);
        if (CollectionUtils.isNotEmpty(bankDotDataList)) {
            Map<String, Object> objectMap = bankDotDataList.get(0);
            // 翻译银行网点档案
            bizObj.set("oppositebankaddrid", objectMap.get("id"));
            bizObj.set("oppositebankaddr", objectMap.get("name"));
            bizObj.set("oppositebanklineno", objectMap.get("linenumber"));
            bizObj.set("oppositebankType", objectMap.get("bank_name"));
        } else {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F9",
                                    "开户行名称入参[%s]未在银行网点档案中查询到，请检查是否存在该银行网点id、编码或已停用") /* "开户行名称入参[%s]未在银行网点档案中查询到，请检查是否存在该银行网点id、编码或已停用" */,//@notranslate
                            oppositeBankAddr)
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
    }

    private void innerUnitTranslate(BizObject bizObj, String oppositeObjectName) throws Exception {
        //// 查询会计主体
        //BillContext innerUnitBillContext = new BillContext();
        //innerUnitBillContext.setFullname("aa.baseorg.FinanceOrgMV");
        //innerUnitBillContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        //QueryConditionGroup innerUnitCondition = QueryConditionGroup.and(QueryCondition.name("stopstatus").eq(0));
        //innerUnitCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").eq(oppositeObjectName),
        //        QueryCondition.name("id").eq(oppositeObjectName)));
        //List<Map<String, Object>> innerUnitList = MetaDaoHelper.queryAll(innerUnitBillContext, "id,name", innerUnitCondition, null);

        //资金组织
        // TODO: 2024/8/28 有数量限制吗？
        ConditionDTO idCondition = ConditionDTO.newCondition().withEnabled();
        List<String> oppositeObjectNames = new ArrayList<>();
        oppositeObjectNames.add(oppositeObjectName);
        idCondition.andIdIn(oppositeObjectNames);
        List<FundsOrgDTO> idFundsOrgDTOS = fundsOrgQueryServiceComponent.getByCondition(idCondition);
        ConditionDTO codeCondition = ConditionDTO.newCondition().withEnabled();
        codeCondition.andCodeIn(oppositeObjectNames);
        List<FundsOrgDTO> codeFundsOrgDTOS = fundsOrgQueryServiceComponent.getByCondition(codeCondition);
        ConditionDTO nameCondition = ConditionDTO.newCondition().withEnabled();
        nameCondition.andNameIn(oppositeObjectNames);
        List<FundsOrgDTO> nameFundsOrgDTOS = fundsOrgQueryServiceComponent.getByCondition(nameCondition);

        List<Map<String, Object>> innerUnitList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(idFundsOrgDTOS)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", idFundsOrgDTOS.get(0).getId());
            map.put("name", idFundsOrgDTOS.get(0).getName());
            innerUnitList.add(map);
        }
        if (CollectionUtils.isNotEmpty(codeFundsOrgDTOS)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", codeFundsOrgDTOS.get(0).getId());
            map.put("name", codeFundsOrgDTOS.get(0).getName());
            innerUnitList.add(map);
        }
        if (CollectionUtils.isNotEmpty(nameFundsOrgDTOS)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", nameFundsOrgDTOS.get(0).getId());
            map.put("name", nameFundsOrgDTOS.get(0).getName());
            innerUnitList.add(map);
        }


        if (CollectionUtils.isNotEmpty(innerUnitList)) {
            Map<String, Object> objectMap = innerUnitList.get(0);
            bizObj.set("oppositeobjectid", ValueUtils.isNotEmptyObj(objectMap.get("id")) ? String.valueOf(objectMap.get("id")) : objectMap.get("id"));
            bizObj.set("oppositeobjectname", objectMap.get("name"));
        } else {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(String.format("内部单位[%s]未在会计主体档案中查询到，请检查是否存在该内部单位编码，或是否已停用", oppositeObjectName)));//@notranslate
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
    }

    private void translateFundBusinessObj(BizObject bizObj, String oppositeObjectName) throws Exception {
        // 查询资金业务对象档案
        BillContext fundobjBillContext = new BillContext();
        fundobjBillContext.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
        fundobjBillContext.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
        QueryConditionGroup fundObjCondition = QueryConditionGroup.and(QueryCondition.name("enabled").eq("1"));
        fundObjCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").eq(oppositeObjectName), QueryCondition.name("id").eq(oppositeObjectName)));
        List<Map<String, Object>> businessDataList = MetaDaoHelper.queryAll(fundobjBillContext, "id,fundbusinobjtypename,fundbusinobjtypeid", fundObjCondition, null);
        if (CollectionUtils.isNotEmpty(businessDataList)) {
            Map<String, Object> objectMap = businessDataList.get(0);
            bizObj.set("oppositeobjectid", ValueUtils.isNotEmptyObj(objectMap.get("id")) ? String.valueOf(objectMap.get("id")) : objectMap.get("id"));
            bizObj.set("oppositeobjectname", objectMap.get("fundbusinobjtypename"));
            bizObj.set("fundbusinobjtypeid", objectMap.get("fundbusinobjtypeid"));
        } else {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18C687920458000A",
                                    "资金业务对象[%s]未在资金业务对象档案中查询到，请检查是否存在该资金业务对象编码，或是否已停用") /* "资金业务对象[%s]未在资金业务对象档案中查询到，请检查是否存在该资金业务对象编码，或是否已停用" */,//@notranslate
                            oppositeObjectName));
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
    }

    private void translateEmployee(BizObject bizObj, String oppositeObjectName, Set<String> orgList) throws Exception {
        BillContext suppBillContext = new BillContext();
        suppBillContext.setFullname("bd.staff.Staff");
        suppBillContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QueryConditionGroup staffCondition = QueryConditionGroup.and(QueryCondition.name("dr").eq("0"), QueryCondition.name("enable").eq("1"));
        Set<String> codeList = new HashSet<>();
        Set<Long> idList = new HashSet<>();
        try {
            Long id = Long.valueOf(oppositeObjectName);
            idList.add(id);
            codeList.add(oppositeObjectName);
        } catch (NumberFormatException e) {
            codeList.add(oppositeObjectName);
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            staffCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").in(codeList.toArray()), QueryCondition.name("id").in(idList.toArray())));
        } else {
            staffCondition.addCondition(QueryCondition.name("code").in(codeList.toArray()));
        }
        List<Map<String, Object>> suppDataList = MetaDaoHelper.queryAll(suppBillContext, "id,name", staffCondition, null);
        if (CollectionUtils.isNotEmpty(suppDataList)) {
            Map<String, Object> objectMap = suppDataList.get(0);
            List<Map<String, Object>> staffList1 = QueryBaseDocUtils.queryOrgByStaffId(objectMap.get("id"));
            boolean staffOrg = false;
            if (!staffList1.isEmpty()) {
                for (Map<String, Object> staff : staffList1) {
                    if (orgList.contains(staff.get("org_id").toString())) {
                        staffOrg = true;
                        break;
                    }
                }
            }
            AutoConfig autoConfig = autoConfigService.getGlobalConfigEntity();
            boolean allowCrossOrgGetPerson = false;
            if (autoConfig != null && autoConfig.getAllowCrossOrgGetPerson() != null && autoConfig.getAllowCrossOrgGetPerson()) {
                allowCrossOrgGetPerson = true;
            }
            if (!staffOrg && !allowCrossOrgGetPerson) {
                ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                        bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                        ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                        InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180826",
                                "导入的员工所属会计主体与导入会计主体不一致！") /* "导入的员工所属会计主体与导入会计主体不一致！" */);//@notranslate

                throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());

            }
            bizObj.set("oppositeobjectid", ValueUtils.isNotEmptyObj(objectMap.get("id")) ? String.valueOf(objectMap.get("id")) : objectMap.get("id"));
            bizObj.set("oppositeobjectname", objectMap.get("name"));
        } else {


            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180820",
                                    "人员[%s]未在员工档案中查询到，请检查是否存在该员工编码，或是否已停用") /* "人员[%s]未在员工档案中查询到，请检查是否存在该员工编码，或是否已停用" */,//@notranslate
                            oppositeObjectName));

            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
    }

    private void translateSupplier(BizObject bizObj, String oppositeObjectName, Set<String> orgList) throws Exception {
        Set<String> codeList = new HashSet<>();
        Set<Long> idList = new HashSet<>();
        try {
            Long id = Long.valueOf(oppositeObjectName);
            idList.add(id);
            codeList.add(oppositeObjectName);
        } catch (NumberFormatException e) {
            codeList.add(oppositeObjectName);
        }
        VendorVO info = null;
        if (CollectionUtils.isNotEmpty(idList)) {
            List<VendorVO> vendorFieldByIdList = vendorQueryService.getVendorFieldByIdList(new ArrayList<>(idList));
            if (CollectionUtils.isNotEmpty(vendorFieldByIdList)) {
                info = vendorFieldByIdList.get(0);
            }
        }
        //begin ctm wangzc 20240801 牧原根据导入收款单位名称匹配数据
        if (CollectionUtils.isNotEmpty(codeList)) {
            List<VendorVO> vendorFieldByCodeList = vendorQueryService.getVendorFieldByCodeList(new ArrayList<>(codeList));
            if (CollectionUtils.isNotEmpty(vendorFieldByCodeList)) {
                info = vendorFieldByCodeList.get(0);
            } else {
                List<VendorVO> vendorFieldByNameList = vendorQueryService.getVendorFieldByNameList(new ArrayList<>(codeList));
                if (CollectionUtils.isNotEmpty(vendorFieldByNameList)) {
                    if (vendorFieldByNameList.size() > 1) {
                        ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                                bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                                ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                                "导入供应商" + oppositeObjectName + "存在多条数据，导入失败，请检查！"//@notranslate
                        );
                        throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                    }
                    info = vendorFieldByNameList.get(0);
                }
            }
        }
        //end
        //供应商不存在或者停用状态不是“启用”
        List<VendorExtendVO> voList = null;
        if (info != null) {
            voList = vendorQueryService.getVendorExtendFieldByVendorIdListAndOrgIdList(Arrays.asList(new Long[]{info.getId()}), null);
        }
        if (info == null || (!CollectionUtils.isEmpty(voList) && voList.get(0).get("stopstatus") != null && (Boolean) voList.get(0).get("stopstatus"))) {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180815",
                                    "供应商[%s]未在供应商档案中查询到，请检查是否存在该供应商编码，或是否已停用") /* "供应商[%s]未在供应商档案中查询到，请检查是否存在该供应商编码，或是否已停用" */,//@notranslate
                            oppositeObjectName)
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
        List<VendorOrgVO> vendorOrgVOs = vendorQueryService.getVendorOrgByVendorId(info.getId());
        boolean exist = false;
        for (VendorOrgVO vendorOrgVO : vendorOrgVOs) {
            if (orgList.contains(vendorOrgVO.getOrg())) {
                exist = true;
            }
        }
        if (!exist) {

            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418081A",
                            "导入的供应商所属使用组织与导入会计主体不一致！") /* "导入的供应商所属使用组织与导入会计主体不一致！" *///@notranslate
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
        bizObj.set("oppositeobjectid", ValueUtils.isNotEmptyObj(info.getId()) ? String.valueOf(info.getId()) : info.getId());
        bizObj.set("oppositeobjectname", info.getName());
    }

    private void translateCustomer(BizObject bizObj, String oppositeObjectName, Set<String> orgList) throws Exception {
        // 查询客户档案
        BillContext custBillContext = new BillContext();
        custBillContext.setFullname("aa.merchant.Merchant");
        custBillContext.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
        QueryConditionGroup custCondition = new QueryConditionGroup();
        Set<String> codeList = new HashSet<>();
        Set<Long> idList = new HashSet<>();

        try {
            Long id = Long.valueOf(oppositeObjectName);
            idList.add(id);
            codeList.add(oppositeObjectName);
        } catch (NumberFormatException e) {
            codeList.add(oppositeObjectName);
        }
        //begin ctm wangzc 20240801 牧原根据导入收款单位名称匹配数据
        if (CollectionUtils.isNotEmpty(idList)) {
            custCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").in(codeList.toArray()), QueryCondition.name("name").in(codeList.toArray()), QueryCondition.name("id").in(idList.toArray())));
        } else {
            custCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").in(codeList.toArray()), QueryCondition.name("name").in(codeList.toArray())));
        }
        QuerySchema schema = QuerySchema.create().addSelect("id, name");
        schema.distinct();

        custCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("merchantAppliedDetail.stopstatus").eq("0")));
        schema.addCondition(custCondition);
        List<Map<String, Object>> custDataList = MetaDaoHelper.query(custBillContext, schema);
        if (CollectionUtils.isNotEmpty(custDataList)) {
            Map<String, Object> objectMap = custDataList.get(0);
            //查询超出一条报错,代表命中多条数据报错；
            if (custDataList.size() > 1) {

                ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                        bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                        ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                        "导入的客户存在多条：" + oppositeObjectName + "导入失败！"//@notranslate
                );
                throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
            }
            //end ctm wangzc 20240801 牧原根据导入收款单位名称匹配数据
            List<MerchantApplyRangeDTO> customeList = QueryBaseDocUtils.queryMerchantApplyRange(Long.valueOf(custDataList.get(0).get("id").toString()));
            boolean exist = false;
            for (MerchantApplyRangeDTO customeMap : customeList) {
                if (orgList.contains(customeMap.getOrgId())) {
                    exist = true;
                }
            }
            if (!exist) {

                ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                        bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                        ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180812",
                                "导入的客户所属使用组织与导入会计主体不一致！") /* "导入的客户所属使用组织与导入会计主体不一致！" *///@notranslate
                );
                throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
            }
            bizObj.set("oppositeobjectid", ValueUtils.isNotEmptyObj(objectMap.get("id")) ? String.valueOf(objectMap.get("id")) : objectMap.get("id"));
            bizObj.set("oppositeobjectname", objectMap.get("name"));
        } else {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObj.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObj.get(POIConstant.ExcelField.ROW_NUM)) ? bizObj.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080F",
                                    "客户[%s]未在客户档案中查询到，请检查是否存在该客户编码，或是否已停用") /* "客户[%s]未在客户档案中查询到，请检查是否存在该客户编码，或是否已停用" */,//@notranslate
                            oppositeObjectName)
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
    }

    private void translateFundPlanProject(BizObject bizObject, BizObject bizObject_b, String fundPlanProjectCode) throws Exception {
        // 查询资金计划档案
        String accentity = bizObject.getString(ICmpConstant.ACCENTITY);
        String currency = bizObject.getString(ICmpConstant.CURRENCY);
        Date expectdate = bizObject.get("vouchdate");
        String dept = bizObject_b.get("dept");
        List<Long> resultData = QueryBaseDocUtils.queryPlanStrategyIsEnable(accentity, currency, expectdate, dept);
        if (CollectionUtils.isEmpty(resultData)) {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format("资金计划项目编码[%s]未查询到符合条件的计划编辑明细", fundPlanProjectCode)//@notranslate
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
        BillContext fundPlanBillContext = new BillContext();
        fundPlanBillContext.setFullname("cspl.plansummary.PlanSummaryB");
        fundPlanBillContext.setDomain(IDomainConstant.MDD_DOMAIN_CTMCSPL);
        QueryConditionGroup fundPlanCondition = new QueryConditionGroup();
        fundPlanCondition.addCondition(QueryCondition.name("id").in(resultData));

        Set<String> codeList = new HashSet<>();
        Set<Long> idList = new HashSet<>();
        try {
            Long id = Long.valueOf(fundPlanProjectCode);
            idList.add(id);
            codeList.add(fundPlanProjectCode);
        } catch (NumberFormatException e) {
            codeList.add(fundPlanProjectCode);
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            fundPlanCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("capitalPlanProjectNo").in(codeList.toArray()), QueryCondition.name("id").in(idList.toArray())));
        } else {
            fundPlanCondition.addCondition(QueryCondition.name("capitalPlanProjectNo").in(codeList.toArray()));
        }
        List<Map<String, Object>> fundPlanList = MetaDaoHelper.queryAll(fundPlanBillContext,
                "id",
                fundPlanCondition, null);
        if (CollectionUtils.isNotEmpty(fundPlanList)) {
            Map<String, Object> objectMap = fundPlanList.get(0);
            bizObject_b.set("fundPlanProject", objectMap.get("id"));
        } else {

            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080B",
                                    "资金计划项目编码[%s]未在资金计划项目档案中查询到，请检查是否存在该编码，或是否已停用") /* "资金计划项目编码[%s]未在资金计划项目档案中查询到，请检查是否存在该编码，或是否已停用" *///@notranslate
                            , fundPlanProjectCode)
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }

        // 翻译资金计划明细
        String fundPlanProjectDetailCode = ValueUtils.isNotEmptyObj(bizObject_b.get("fundPlanProjectDetail_code"))
                ? bizObject_b.getString("fundPlanProjectDetail_code") : null;
        if (ValueUtils.isNotEmptyObj(fundPlanProjectDetailCode)) {
            translateFundPlanDetail(bizObject_b, fundPlanProjectDetailCode, fundPlanList);
        }

    }

    private void translateFundPlanDetail(BizObject bizObject_b, String fundPlanProjectDetailCode, List<Map<String, Object>> fundPlanList) throws Exception {
        String fundPlanProject = bizObject_b.getString("fundPlanProject");
        long fundPlanProjectLong = Long.parseLong(fundPlanProject);
        Map<Long, List<Long>> fundPlanProjectDetailMap = RemoteDubbo.get(CapitalPlanExecuteService.class,
                        IDomainConstant.MDD_DOMAIN_CTMCSPL)
                .queryDetailBySummaryBIds(Collections.singletonList(fundPlanProjectLong));
        List<Long> fundPlanProjectDetailIdsList = fundPlanProjectDetailMap.get(fundPlanProjectLong);
        if (ValueUtils.isEmpty(fundPlanProjectDetailMap) || ValueUtils.isEmpty(fundPlanProjectDetailIdsList)) {

            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080B",
                                    "该资金计划项目编码或id[%s]没有查询到计划明细!")//@notranslate
                            , bizObject_b.getString("fundPlanProject_code"))
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }

        BillContext fundPlanDetailBillContext = new BillContext();
        fundPlanDetailBillContext.setFullname("cspl.plansummary.PlanSummaryDetail");
        fundPlanDetailBillContext.setDomain(IDomainConstant.MDD_DOMAIN_CTMCSPL);
        QueryConditionGroup fundPlanDetailCondition = new QueryConditionGroup();
        fundPlanDetailCondition.addCondition(QueryCondition.name("id").in(fundPlanProjectDetailIdsList));

        Set<String> codeFundPlanDetailList = new HashSet<>();
        Set<Long> idFundPlanDetailList = new HashSet<>();
        try {
            Long id = Long.valueOf(fundPlanProjectDetailCode);
            idFundPlanDetailList.add(id);
            codeFundPlanDetailList.add(fundPlanProjectDetailCode);
        } catch (NumberFormatException e) {
            codeFundPlanDetailList.add(fundPlanProjectDetailCode);
        }
        if (CollectionUtils.isNotEmpty(idFundPlanDetailList)) {
            fundPlanDetailCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("code").in(codeFundPlanDetailList.toArray()), QueryCondition.name("id").in(idFundPlanDetailList.toArray())));
        } else {
            fundPlanDetailCondition.addCondition(QueryCondition.name("code").in(codeFundPlanDetailList.toArray()));
        }
        List<Map<String, Object>> fundPlanDetailList = MetaDaoHelper.queryAll(fundPlanDetailBillContext,
                "id",
                fundPlanDetailCondition, null);
        if (CollectionUtils.isNotEmpty(fundPlanDetailList)) {
            Map<String, Object> objectMap = fundPlanDetailList.get(0);
            bizObject_b.set("fundPlanProjectDetail", objectMap.get("id"));
        } else {
            ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                    bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                    ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                    String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080B",
                                    "资金计划明细编码[%s]未在资金计划明细档案中查询到，请检查是否存在该编码，或是否已停用")//@notranslate
                            , fundPlanProjectDetailCode)
            );
            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
        }
    }

    @Autowired
    IEnterpriseBankAcctService enterpriseBankAcctService;

    /**
     * 处理收款方账户号、收款方账户名称（客户银行账户参照、供应商银行账户参照、员工银行账户参照）
     */
    private void dealCustSuppBankRef(BizObject bizObject, String billnum) throws Exception {
        List<BizObject> bizObject_bList = new ArrayList<>();
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundPayment_b", BizObject.class);
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundCollection_b", BizObject.class);
        }
        if (CollectionUtils.isEmpty(bizObject_bList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102075"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080A", "导入数据表体信息不能为空，请检查") /* "导入数据表体信息不能为空，请检查" */);
        }
        // 收集需要翻译的银行账户号
        for (BizObject bizObject_b : bizObject_bList) {
            short caobject = bizObject_b.getShort("caobject").shortValue();
            if (CaObject.Other.getValue() == caobject) {
                continue;
            }
            // 校验收款方账户名称
            Object oppositeaccountname = bizObject_b.get("oppositeaccountname");
            //begin ctm wangzc 2024-06-16 牧原 导入资金付款单 对方银行账号匹配修改
            Object oppositeaccountno = bizObject_b.get("oppositeaccountno");
            if (ValueUtils.isNotEmptyObj(oppositeaccountname)) {
                parseAccountCode(bizObject, bizObject_b, caobject, oppositeaccountname, oppositeaccountno);
            }
            //end
            if (ValueUtils.isNotEmptyObj(bizObject_b.get("oppositeaccountid"))) {
                continue;
            }

            if (!ValueUtils.isNotEmptyObj(bizObject_b.get("oppositeaccountno"))) {
                continue;
            }

            // 查询账户档案，根据客户档案id，币种等过滤，后续不用校验银行账户和档案id是否匹配
            if (CaObject.Customer.getValue() == caobject) {

                boolean isStrictlyControl = ValueUtils.isNotEmptyObj(bizObject.get("isStrictlyControl"))
                        ? bizObject.getBoolean("isStrictlyControl") : true;
                Short settleStatus = bizObject_b.getShort("settlestatus");
                AgentFinancialDTO custAccMap = BillImportCheckUtil.queryCustomerBankAccountByCondition(bizObject_b.getLong("oppositeobjectid"), bizObject_b.getString("oppositeaccountno"), bizObject.getString("currency"));
                if (!isStrictlyControl
                        && settleStatus == FundSettleStatus.SettlementSupplement.getValue()
                        && custAccMap == null) {
                    bizObject_b.set("oppositeaccountno", bizObject_b.getString("oppositeaccountno"));
                } else {
                    if (custAccMap == null) {

                        ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                                bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                                ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                                String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180813",
                                                "收款方账户号[%s]未在客户档案银行账户中查询到，请检查") /* "收款方账户号[%s]未在客户档案银行账户中查询到，请检查" */,//@notranslate
                                        bizObject_b.getString("oppositeaccountno"))
                        );
                        throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                    }

                    // 赋值
                    bizObject_b.set("oppositeaccountid", custAccMap.getId().toString());
                    bizObject_b.set("oppositeaccountno", custAccMap.getBankAccount());
                    bizObject_b.set("oppositeaccountname", custAccMap.getBankAccountName());
                    // 查询开户行
                    if (ValueUtils.isNotEmptyObj(custAccMap.getOpenBank())) {
                        BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(custAccMap.getOpenBank());
                        if (depositBank != null) {
                            bizObject_b.set("oppositebankaddr", depositBank.getName()); // 客户账户银行网点
                            bizObject_b.set("oppositebanklineno", depositBank.getLinenumber()); // 客户账户开户行联行号
                            bizObject_b.set("oppositebankType", depositBank.getBankName()); // 客户账户银行类别
                            bizObject_b.set("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                        } else {
                            bizObject_b.set("oppositebankaddr", null); // 客户账户银行网点
                            bizObject_b.set("oppositebanklineno", null); // 客户账户开户行联行号
                            bizObject_b.set("oppositebankType", null); // 客户账户银行类别
                            bizObject_b.set("receivePartySwift", null); // 收款方swift码
                        }
                    }
                }
            } else if (CaObject.Supplier.getValue() == caobject) {
                boolean isStrictlyControl = ValueUtils.isNotEmptyObj(bizObject.get("isStrictlyControl"))
                        ? bizObject.getBoolean("isStrictlyControl") : true;
                Short settleStatus = bizObject_b.getShort("settlestatus");
                Map<String, Object> suppAccMap = BillImportCheckUtil.querySupplierBankAccountByCondition(bizObject_b.get("oppositeobjectid"), bizObject_b.get("oppositeaccountno"), bizObject.get("currency"));

                if (!isStrictlyControl
                        && settleStatus == FundSettleStatus.SettlementSupplement.getValue()
                        && MapUtils.isEmpty(suppAccMap)) {
                    bizObject_b.set("oppositeaccountno", bizObject_b.getString("oppositeaccountno"));
                } else {
                    if (!MapUtils.isNotEmpty(suppAccMap)) {

                        ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                                bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                                ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                                String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418081E",
                                                "收款方账户号[%s]未在供应商档案银行账户中查询到，请检查") /* "收款方账户号[%s]未在供应商档案银行账户中查询到，请检查" */,//@notranslate
                                        bizObject_b.getString("oppositeaccountno"))
                        );
                        throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                    }

                    // 赋值
                    bizObject_b.set("oppositeaccountid", suppAccMap.get("id").toString());
                    bizObject_b.set("oppositeaccountno", suppAccMap.get("account"));
                    bizObject_b.set("oppositeaccountname", suppAccMap.get("accountname"));
                    // 查询开户行
                    if (ValueUtils.isNotEmptyObj(suppAccMap.get("openaccountbank"))) {
                        BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(suppAccMap.get("openaccountbank").toString());
                        if (depositBank != null) {
                            bizObject_b.set("oppositebankaddr", depositBank.getName()); // 供应商账户银行网点
                            bizObject_b.set("oppositebanklineno", depositBank.getLinenumber()); // 供应商账户开户行联行号
                            bizObject_b.set("oppositebankType", depositBank.getBankName()); // 供应商账户银行类别
                            bizObject_b.set("receivePartySwift", depositBank.getSwiftCode()); // 供应商收款方swift码
                        } else {
                            bizObject_b.set("oppositebankaddr", null); // 供应商账户银行网点
                            bizObject_b.set("oppositebanklineno", null); // 供应商账户开户行联行号
                            bizObject_b.set("oppositebankType", null); // 供应商账户银行类别
                            bizObject_b.set("receivePartySwift", null); // 供应商收款方swift码
                        }
                    }
                }
            } else if (CaObject.Employee.getValue() == caobject) {
                Map<String, Object> staffAccMap = BillImportCheckUtil.queryStaffBankAccountByCondition(bizObject_b.get("oppositeobjectid"), bizObject_b.get("oppositeaccountno"), bizObject.get("currency"));
                if (!MapUtils.isNotEmpty(staffAccMap)) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180825",
                                            "收款方账户号[%s]未在员工档案银行账户中查询到，请检查") /* "收款方账户号[%s]未在员工档案银行账户中查询到，请检查" */,//@notranslate
                                    bizObject_b.getString("oppositeaccountno"))
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }

                // 赋值
                bizObject_b.set("oppositeaccountid", staffAccMap.get("id").toString());
                if (Objects.nonNull(staffAccMap.get("accountname"))) {
                    bizObject_b.set("oppositeaccountname", staffAccMap.get("accountname").toString());
                }
                if (ValueUtils.isNotEmptyObj(staffAccMap.get("bankname"))) {
                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(staffAccMap.get("bankname").toString());
                    if (depositBank != null) {
                        bizObject_b.set("oppositebankaddr", depositBank.getName());// 员工账户银行网点
                        bizObject_b.set("oppositebanklineno", depositBank.getLinenumber()); // 员工账户开户行联行号
                        bizObject_b.set("oppositebankType", depositBank.getBankName()); // 员工账户银行类别
                        bizObject_b.set("receivePartySwift", depositBank.getSwiftCode()); // 员工收款方swift码
                    } else {
                        bizObject_b.set("oppositebankaddr", null);// 员工账户银行网点
                        bizObject_b.set("oppositebanklineno", null); // 员工账户开户行联行号
                        bizObject_b.set("oppositebankType", null); // 员工账户银行类别
                        bizObject_b.set("receivePartySwift", null); // 员工收款方swift码
                    }
                }
            }
            if (CaObject.CapBizObj.getValue() == caobject) {
                CtmJSONObject fundBusinObjArchivesItem;
                try {
                    // 获取账户信息对象
                    fundBusinObjArchivesItem = queryFundBusinessObjectDataById(bizObject_b.get("oppositeobjectid"), bizObject_b.get("oppositeaccountno"), bizObject_b.get("oppositeaccountname"));
                } catch (Exception e) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180804",
                                    "调用资金业务对象所属服务异常！请检查相应服务！") /* "调用资金业务对象所属服务异常！请检查相应服务！" *///@notranslate
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
                if (ValueUtils.isNotEmptyObj(fundBusinObjArchivesItem)) {
                    // 查询银行类别
                    String bbankid = fundBusinObjArchivesItem.getString("bbankid");
                    if (ValueUtils.isNotEmptyObj(bbankid)) {
                        BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
                        bizObject_b.set("oppositebankType", bankVO.getName());
                    }
                    bizObject_b.set("oppositeaccountid", fundBusinObjArchivesItem.get("id"));
                    bizObject_b.set("oppositeaccountname", fundBusinObjArchivesItem.get("accountname"));
                    bizObject_b.set("oppositebanklineno", fundBusinObjArchivesItem.get("linenumber"));
                    // 查询银行网点
                    String bopenaccountbankid = fundBusinObjArchivesItem.getString("bopenaccountbankid");
                    if (ValueUtils.isNotEmptyObj(bopenaccountbankid)) {
                        BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
                        bizObject_b.set("oppositebankaddr", bankdotVO.getName());
                    }
                } else {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180803",
                                            "收款方账户号[%s]未在资金业务对象档案中查询到，请检查") /* "收款方账户号[%s]未在资金业务对象档案中查询到，请检查" */,//@notranslate
                                    bizObject_b.getString("oppositeaccountno"))
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }

            }

            if (CaObject.InnerUnit.getValue() == caobject) {
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                if (ValueUtils.isNotEmptyObj(oppositeaccountno)) {
                    enterpriseParams.setAccount(oppositeaccountno.toString());
                }
                if (ValueUtils.isNotEmptyObj(oppositeaccountname)) {
                    enterpriseParams.setAcctName(oppositeaccountname.toString());
                }
                enterpriseParams.setOrgid(bizObject_b.get("oppositeobjectid"));
                enterpriseParams.setCurrencyIDList(Collections.singletonList(bizObject.getString(ICmpConstant.CURRENCY)));
                //支持共享
                List<EnterpriseBankAcctVOWithRange> bankAccounts = baseRefRpcService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
                if (CollectionUtils.isNotEmpty(bankAccounts)) {
                    EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                    bizObject_b.set("oppositeaccountid", enterpriseBankAcctVO.getId());
                    bizObject_b.set("oppositeaccountname", enterpriseBankAcctVO.getAcctName());
                    bizObject_b.set("oppositeaccountno", enterpriseBankAcctVO.getAccount());
                    bizObject_b.set("oppositebankaddrid", enterpriseBankAcctVO.getBankNumber());
                    bizObject_b.set("oppositebankaddr", enterpriseBankAcctVO.getBankNumberName());
                    bizObject_b.set("oppositebankType", enterpriseBankAcctVO.getBankName());
                    bizObject_b.set("oppositebanklineno", enterpriseBankAcctVO.getLineNumber());
                } else {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format("收款方账户名称[%s]未在企业银行账户档案中查询到，请检查", Objects.isNull(bizObject_b.get("oppositeaccountname")) ? bizObject_b.get("oppositeaccountno").toString() : bizObject_b.get("oppositeaccountname").toString())//@notranslate
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
            }

        }
    }

    public CtmJSONObject queryFundBusinessObjectDataById(String id, String account, String accountname) throws Exception {
        CtmJSONObject fundBusinObjArchivesItem = new CtmJSONObject();
        BillContext context = new BillContext();
        context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
        context.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id as oppositeobjectid, fundbusinobjtypename as oppositeobjectname, " +
                "fundbusinobjtypeid,fundBusinObjArchivesItem.benabled as benabled, " +
                "fundBusinObjArchivesItem.isdefaultaccount as isdefaultaccount, fundBusinObjArchivesItem.bbankid as bbankid, " +
                "fundBusinObjArchivesItem.bopenaccountbankid as bopenaccountbankid, fundBusinObjArchivesItem.id as id, " +
                "fundBusinObjArchivesItem.bankaccount as bankaccount,fundBusinObjArchivesItem.accountname as accountname ," +
                "fundBusinObjArchivesItem.linenumber as linenumber, fundBusinObjArchivesItem.bbankAccountId as bbankAccountId");

        schema.appendQueryCondition(QueryCondition.name("id").eq(id));
        if (ValueUtils.isNotEmptyObj(account)) {
            schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.bankaccount").eq(account));
        }
        if (ValueUtils.isNotEmptyObj(accountname)) {
            schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.accountname").eq(accountname));
        }
        if (!ValueUtils.isNotEmptyObj(account) && !ValueUtils.isNotEmptyObj(accountname)) {
            schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.isdefaultaccount").eq(Boolean.TRUE));
        }
        schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.benabled").eq(Boolean.TRUE));

        log.info("getObjectContent, schema = {}", schema);
        List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);

        //获取账户信息对象
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(result)) {
            //获取数据实体
            CtmJSONObject jsonObject = CtmJSONObject.toJSON(result.get(0));
            //查询银行类别
            String bbankid = jsonObject.getString("bbankid");
            BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
            if (!ValueUtils.isNotEmptyObj(bankVO)) {
                return new CtmJSONObject();
            }
            fundBusinObjArchivesItem.put("bbankid_name", bankVO.getName());

            fundBusinObjArchivesItem.put("id", jsonObject.get("id"));
            fundBusinObjArchivesItem.put("bankaccount", jsonObject.get("bankaccount"));
            fundBusinObjArchivesItem.put("accountname", jsonObject.get("accountname"));
            fundBusinObjArchivesItem.put("linenumber", jsonObject.get("linenumber"));
            fundBusinObjArchivesItem.put("fundbusinobjtypeid", jsonObject.get("fundbusinobjtypeid"));
            fundBusinObjArchivesItem.put("oppositeobjectid", ValueUtils.isNotEmptyObj(jsonObject.get("oppositeobjectid"))
                    ? String.valueOf(jsonObject.get("oppositeobjectid")) : jsonObject.get("oppositeobjectid"));
            fundBusinObjArchivesItem.put("oppositeobjectname", jsonObject.get("oppositeobjectname"));
            fundBusinObjArchivesItem.put("bbankAccountId", jsonObject.get("bbankAccountId"));
            fundBusinObjArchivesItem.put("bbankid", jsonObject.get("bbankid"));
            fundBusinObjArchivesItem.put("bopenaccountbankid", jsonObject.get("bopenaccountbankid"));
            //查询银行网点
            String bopenaccountbankid = jsonObject.getString("bopenaccountbankid");
            if (ValueUtils.isNotEmptyObj(bopenaccountbankid)) {
                BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
                fundBusinObjArchivesItem.put("bopenaccountbankid_name", bankdotVO.getName());
            }
            return fundBusinObjArchivesItem;
        }
        return new CtmJSONObject();
    }

    private void parseAccountCode(BizObject bizObject, BizObject bizObject_b, short caobject, Object oppositeaccountname, Object oppositeaccountno) throws Exception {
        Object currency = bizObject.get("currency");
        boolean isStrictlyControl = ValueUtils.isNotEmptyObj(bizObject.get("isStrictlyControl"))
                ? bizObject.getBoolean("isStrictlyControl") : true;
        Short settleStatus = bizObject_b.getShort("settlestatus");
        if (CaObject.Customer.getValue() == caobject) {
            Map<String, Object> condition1 = new HashMap<>();
            condition1.put("stopstatus", "0");
            condition1.put("bankAccountName", oppositeaccountname);
            condition1.put("currency", currency);
            List<Map<String, Object>> custList = QueryBaseDocUtils.queryCustomerBankAccountByCondition(condition1);
            if (!isStrictlyControl
                    && settleStatus == FundSettleStatus.SettlementSupplement.getValue()
                    && CollectionUtils.isEmpty(custList)) {
                bizObject_b.set("oppositeaccountname", oppositeaccountname);
            } else {
                if (!CollectionUtils.isNotEmpty(custList)) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418081F",
                                            "收款方账户名称[%s]未在客户账户档案中查询到，请检查是否存在或已停用") /* "收款方账户名称[%s]未在客户账户档案中查询到，请检查是否存在或已停用" */,//@notranslate
                                    bizObject_b.get("oppositeaccountname").toString())
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
                Map<String, Object> stringObjectMap = null;
                if (custList.size() > 1) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180821",
                                            "收款方账户名称[%s]在客户账户档案中不唯一") /* "收款方账户名称[%s]在客户账户档案中不唯一" */,//@notranslate
                                    bizObject_b.get("oppositeaccountname").toString())
                    );
                    List<Map<String, Object>> newBankAccountsList = null;
                    if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty((String) oppositeaccountno)) {
                        newBankAccountsList = custList.stream().filter(e -> oppositeaccountno.equals(e.get("bankAccount"))).collect(Collectors.toList());
                        if (newBankAccountsList.size() > 1) {
                            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                        }
                        stringObjectMap = newBankAccountsList.get(0);
                    } else {
                        throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                    }
                }
                if(stringObjectMap == null){
                    stringObjectMap = custList.get(0);
                }
                String merchantId = stringObjectMap.get("merchantId").toString();
                if (!merchantId.equals(bizObject_b.get("oppositeobjectid"))) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180824",
                                            "收款方账户名称[%s]未在客户档案中查询到，请检查") /* "收款方账户名称[%s]未在客户档案中查询到，请检查" */,//@notranslate
                                    bizObject_b.get("oppositeaccountname").toString())
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
                bizObject_b.set("oppositeaccountid", stringObjectMap.get("id").toString());
                bizObject_b.set("oppositeaccountno", stringObjectMap.get("bankAccount"));
                bizObject_b.set("oppositeaccountname", stringObjectMap.get("bankAccountName"));
                // 查询开户行
                if (ValueUtils.isNotEmptyObj(stringObjectMap.get("openBank"))) {
                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(stringObjectMap.get("openBank").toString());
                    if (depositBank != null) {
                        bizObject_b.set("oppositebankaddr", depositBank.getName()); // 客户账户银行网点
                    }
                }
            }
        } else if (CaObject.Supplier.getValue() == caobject) {
            Map<String, Object> condition1 = new HashMap<>();
            condition1.put("stopstatus", "0");
            condition1.put("accountname", oppositeaccountname);
            condition1.put("currency", currency);
            List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition1);

            if (!isStrictlyControl
                    && settleStatus == FundSettleStatus.SettlementSupplement.getValue()
                    && CollectionUtils.isEmpty(bankAccounts)
            ) {
                bizObject_b.set("oppositeaccountname", oppositeaccountname);
            } else {
                if (!CollectionUtils.isNotEmpty(bankAccounts)) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F5",
                                            "收款方账户名称[%s]未在供应商账户档案中查询到，请检查是否存在或已停用") /* "收款方账户名称[%s]未在供应商账户档案中查询到，请检查是否存在或已停用" */,//@notranslate
                                    bizObject_b.get("oppositeaccountname").toString())
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
                VendorBankVO vendorBankVO = null;
                if (bankAccounts.size() > 1) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807F8",
                                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A1A604C0000C", "收款方账户名称[%s]在供应商账户档案中不唯一") /* "收款方账户名称[%s]在供应商账户档案中不唯一" */) /* "收款方账户名称[%s]在供应商账户档案中不唯一" */,
                                    bizObject_b.get("oppositeaccountname").toString())
                    );
                    List<VendorBankVO> newBankAccountsList = null;
                    if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty((String) oppositeaccountno)) {
                        newBankAccountsList = bankAccounts.stream().filter(e -> oppositeaccountno.equals(e.getAccount())).collect(Collectors.toList());
                        if (newBankAccountsList.size() > 1) {
                            throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                        }
                        vendorBankVO = newBankAccountsList.get(0);
                    } else {
                        throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                    }
                }
                if (vendorBankVO == null) {
                    vendorBankVO = bankAccounts.get(0);
                }
                String vendorId = vendorBankVO.getVendor().toString();
                if (!vendorId.equals(bizObject_b.get("oppositeobjectid"))) {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807FD",
                                            "收款方账户名称[%s]未在供应商档案中查询到，请检查") /* "收款方账户名称[%s]未在供应商档案中查询到，请检查" */,//@notranslate
                                    bizObject_b.get("oppositeaccountname").toString())
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
                bizObject_b.set("oppositeaccountid", vendorBankVO.getId().toString());
                bizObject_b.set("oppositeaccountno", vendorBankVO.getAccount());
                bizObject_b.set("oppositeaccountname", vendorBankVO.getAccountname());
                // 查询开户行
                if (ValueUtils.isNotEmptyObj(vendorBankVO.getOpenaccountbank())) {
                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(vendorBankVO.getOpenaccountbank());
                    if (depositBank != null) {
                        bizObject_b.set("oppositebankaddr", depositBank.getName()); // 客户账户银行网点
                    }
                }
            }
        } else if (CaObject.Employee.getValue() == caobject) {
            Object oppositeobjectname = bizObject_b.get("oppositeobjectname");
//            if (!oppositeobjectname.equals(oppositeaccountname)) {
//                ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
//                        bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
//                        ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
//                        String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180805",
//                                        "收款方账户名称[%s]与员工的名称不匹配") /* "收款方账户名称[%s]与员工的名称不匹配" */,//@notranslate
//                                bizObject_b.get("oppositeaccountname").toString())
//                );
//                throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
//            }
            Map<String, Object> staffAccMap = BillImportCheckUtil.queryStaffBankAccountByCondition(bizObject_b.get("oppositeobjectid"), bizObject_b.get("oppositeaccountno"), bizObject.get("currency"));
            if (MapUtils.isNotEmpty(staffAccMap)) {
                // 赋值
                bizObject_b.set("oppositeaccountid", staffAccMap.get("id").toString());
                if (Objects.nonNull(staffAccMap.get("accountname"))) {
                    bizObject_b.set("oppositeaccountname", staffAccMap.get("accountname").toString());
                }
                if (ValueUtils.isNotEmptyObj(staffAccMap.get("bankname"))) {
                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(staffAccMap.get("bankname").toString());
                    if (depositBank != null) {
                        bizObject_b.set("oppositebankaddr", depositBank.getName());// 员工账户银行网点
                        bizObject_b.set("oppositebanklineno", depositBank.getLinenumber()); // 员工账户开户行联行号
                        bizObject_b.set("oppositebankType", depositBank.getBankName()); // 员工账户银行类别
                        bizObject_b.set("receivePartySwift", depositBank.getSwiftCode()); // 员工收款方swift码
                    } else {
                        bizObject_b.set("oppositebankaddr", null);// 员工账户银行网点
                        bizObject_b.set("oppositebanklineno", null); // 员工账户开户行联行号
                        bizObject_b.set("oppositebankType", null); // 员工账户银行类别
                        bizObject_b.set("receivePartySwift", null); // 员工收款方swift码
                    }
                }
            }
        } else if (CaObject.InnerUnit.getValue() == caobject) {
            // 1.查询共享组织
            EnterpriseParams enterpriseParamsQueryOrg = new EnterpriseParams();
            enterpriseParamsQueryOrg.setOrgidList(Collections.singletonList(bizObject_b.get("oppositeobjectid")));
            List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOWithRanges = baseRefRpcService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParamsQueryOrg);
            List<String> orgIdList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(enterpriseBankAcctVOWithRanges)) {
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOWithRanges) {
                    List<OrgRangeVO> accountApplyRange = enterpriseBankAcctVOWithRange.getAccountApplyRange();
                    for (OrgRangeVO orgRangeVO : accountApplyRange) {
                        String orgId = orgRangeVO.getRangeOrgId();
                        orgIdList.add(orgId);
                    }
                }
            }
            // 2.根据共享组织过滤账号
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            if (ValueUtils.isNotEmptyObj(oppositeaccountno)) {
                enterpriseParams.setAccount(oppositeaccountno.toString());
            }
            if (ValueUtils.isNotEmptyObj(oppositeaccountname)) {
                enterpriseParams.setAcctName(oppositeaccountname.toString());
            }
            enterpriseParams.setOrgidList(orgIdList);
            enterpriseParams.setCurrencyIDList(Collections.singletonList(currency.toString()));
            List<EnterpriseBankAcctVOWithRange> bankAccounts = baseRefRpcService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
            if (!bankAccounts.isEmpty()) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                bizObject_b.set("oppositeaccountid", enterpriseBankAcctVO.getId());
                bizObject_b.set("oppositeaccountname", enterpriseBankAcctVO.getAcctName());
                bizObject_b.set("oppositeaccountno", enterpriseBankAcctVO.getAccount());
                bizObject_b.set("oppositebankaddrid", enterpriseBankAcctVO.getBankNumber());
                bizObject_b.set("oppositebankaddr", enterpriseBankAcctVO.getBankNumberName());
                bizObject_b.set("oppositebankType", enterpriseBankAcctVO.getBankName());
                bizObject_b.set("oppositebanklineno", enterpriseBankAcctVO.getLineNumber());
            } else {
                ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                        bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                        ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                        String.format("收款方账户名称[%s]未在企业银行账户档案中查询到，请检查", Objects.isNull(bizObject_b.get("oppositeaccountname")) ? bizObject_b.get("oppositeaccountno").toString() : bizObject_b.get("oppositeaccountname").toString())//@notranslate
                );
                throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
            }
        } else if (CaObject.CapBizObj.getValue() == caobject) {
            try {
                // 获取账户信息对象
                CtmJSONObject fundBusinObjArchivesItem = queryFundBusinessObjectDataById(bizObject_b.get("oppositeobjectid"), bizObject_b.get("oppositeaccountno"), bizObject_b.get("oppositeaccountname"));
                if (ValueUtils.isNotEmptyObj(fundBusinObjArchivesItem)) {
                    // 查询银行类别
                    String bbankid = fundBusinObjArchivesItem.getString("bbankid");
                    if (ValueUtils.isNotEmptyObj(bbankid)) {
                        BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
                        bizObject_b.set("oppositebankType", bankVO.getName());
                    }
                    bizObject_b.set("oppositeaccountid", fundBusinObjArchivesItem.get("id"));
                    bizObject_b.set("oppositeaccountname", fundBusinObjArchivesItem.get("accountname"));
                    bizObject_b.set("oppositebanklineno", fundBusinObjArchivesItem.get("linenumber"));
                    // 查询银行网点
                    String bopenaccountbankid = fundBusinObjArchivesItem.getString("bopenaccountbankid");
                    if (ValueUtils.isNotEmptyObj(bopenaccountbankid)) {
                        BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
                        bizObject_b.set("oppositebankaddr", bankdotVO.getName());
                    }
                } else {
                    ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                            bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                            ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180803", "收款方账户号[%s]未在资金业务对象档案中查询到，请检查") /* "收款方账户号[%s]未在资金业务对象档案中查询到，请检查" */, bizObject_b.get("oppositeaccountno").toString())
                    );
                    throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
                }
            } catch (Exception e) {
                ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                        bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                        ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180804",
                                "调用资金业务对象所属服务异常！请检查相应服务！") /* "调用资金业务对象所属服务异常！请检查相应服务！" *///@notranslate
                );
                throw new ImportDataSaveException("999", CtmJSONObject.toJSONString(excelErrorMsgDto), ErrorMsgRealTypeEnum.MAP.getCode());
            }
        }
    }

    public void checkProjectAndExpenseItem(BizObject bizObject) throws Exception {
        if (bizObject.get("project") != null) {
            List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectById(bizObject.get("project"));
            if (!CollectionUtils.isEmpty(projectList)) {
                Object projectFlag = projectList.get(0).get("enable");
                if (projectFlag != null && "2".equals(projectFlag.toString())) {//1是启用，2是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101923"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180810", "项目未启用，保存失败！") /* "项目未启用，保存失败！" */);
                }
                Object orgId = projectList.get(0).get("orgid");
                if ("666666".equals(orgId)) {
                    return;
                }
                List<String> project = new ArrayList<>();
                project.add(bizObject.get("project"));
                List<String> orgids = BillImportCheckUtil.queryOrgRangeSByProjectIds(project);
                // 核算委托关系的没有校验，后续优化，该逻辑后续调整为批量处理，要优化
                assert orgids != null;
                if (!orgids.contains(bizObject.get(IBussinessConstant.ACCENTITY))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101924"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180814", "导入的项目所属使用组织与导入会计主体不一致！") /* "导入的项目所属使用组织与导入会计主体不一致！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101925"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180819", "未查询到对应的项目，保存失败！") /* "未查询到对应的项目，保存失败！" */);
            }
        }
        if (bizObject.get("expenseitem") != null) {
            List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(bizObject.get("expenseitem"));
            if (!CollectionUtils.isEmpty(expenseItemList)) {
                Object expenseItemFlag = expenseItemList.get(0).get("enabled");
                if (expenseItemFlag != null && !(boolean) expenseItemFlag) {//true是启用，false是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102080"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418081B", "费用项目未启用，保存失败！") /* "费用项目未启用，保存失败！" */);
                }
                //费用项目存在时的权限校验,返回false意味校验不通用，抛出提示
                if (!CmpCommonUtil.checkExpenseitem(bizObject.get("expenseitem").toString(), bizObject.get(IBussinessConstant.ACCENTITY))) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EA3864005680008", "所选费用项目无权限或存在未启用财资业务领域的情况，保存失败！ ") /* "所选费用项目无权限或存在未启用财资业务领域的情况，保存失败！ " */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102081"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418081D", "未查询到对应的费用项目，保存失败！") /* "未查询到对应的费用项目，保存失败！" */);
            }

        }
    }

}
