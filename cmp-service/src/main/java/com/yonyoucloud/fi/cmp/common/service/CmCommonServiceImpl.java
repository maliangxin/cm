package com.yonyoucloud.fi.cmp.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import com.yonyou.cloud.middleware.rpc.RPCStubBeanFactory;
import com.yonyou.diwork.service.IServiceManagerService;
import com.yonyou.einvoice.dto.TaxRateArchiveDto;
import com.yonyou.einvoice.dto.TaxRateQueryCondition;
import com.yonyou.einvoice.service.itf.ITaxRateArchIrisService;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.bizdoc.service.settlemethod.ISettleMethodQueryService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.model.rpcparams.taxrate.TaxRateQueryParam;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleContext;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.core.service.BillExecActionService;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRuleDetail;
import com.yonyou.ucf.mdd.ext.bill.billmake.rule.crud.BeforePullAndPushRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.rule.crud.DivideVoucherForPullRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.rule.crud.QueryPullAndPushRule;
import com.yonyou.ucf.mdd.ext.bill.billmake.service.MakeBillRuleService;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.CommonRuleUtils;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.consts.Constants;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.i18n.utils.MddMultilingualUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TransTypeQueryParam;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import com.yonyou.workbench.model.ServiceVO;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.openapi.IOpenApiForStwbService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyou.yonbip.ctm.ctmpub.bam.api.openapi.IAccountInfoQueryApiService;
import com.yonyou.yonbip.ctm.ctmpub.bam.model.request.BamAccountInfoQueryReq;
import com.yonyou.yonbip.ctm.ctmpub.bam.model.response.BamResult;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.orgs.FinOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.bd.accbooktype.AccBookType;
import com.yonyoucloud.fi.bd.period.Period;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.ExchangeRateTypeQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.uretail.sys.auth.DataPermissionRequestDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResponseDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResultDto;
import com.yonyoucloud.uretail.sys.pubItf.IDataPermissionService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.base.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.AUDIT;
import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;

/**
 * @ClassName CmCommonServiceImpl
 * @Desc 通用服务实现类
 * @Author tongyd
 * @Date 2019/9/9
 * @Version 1.0
 */
@Slf4j
@Service
public class CmCommonServiceImpl<T> implements CmCommonService<T> {

    private static final String PAYBILLMAPPER = "com.yonyoucloud.fi.cmp.mapper.PaybillMapper";
    private static final String SALARYMAPPER = "com.yonyoucloud.fi.cmp.mapper.SalarypayMapper";
    private static final String TRANSFERACCOUNT = "com.yonyoucloud.fi.cmp.mapper.TransferAccountMapper";

    /**
     * 缓存资金组织
     */
    private static final @NonNull Cache<String, List<SettleMethodModel>> settleMethodModelCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Resource
    private ITransTypeService iTransTypeService;

    @Autowired
    private MakeBillRuleService makeBillRuleService;

    @Resource
    private QueryPullAndPushRule queryPullAndPushRule;

    @Resource
    private DivideVoucherForPullRule divideVoucherForPullRule;

    @Resource
    private BeforePullAndPushRule beforePullAndPushRule;

    @Resource
    private IOpenApiForStwbService iOpenApiForStwbService;

    @Autowired
    private ITaxRateArchIrisService iTaxRateArchIrisService;

    @Autowired
    private CooperationFileService cooperationFileService;
    @Autowired
    private BillExecActionService billExecActionService;

    @Autowired
    ExchangeRateTypeQueryService exchangeRateTypeQueryService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;

    @Autowired
    FinOrgQueryServiceComponent finOrgQueryServiceComponent;

    @Autowired
    private IServiceManagerService iServiceManagerService;

    @Override
    public String getNatCurrency(CtmJSONObject param) throws Exception {
        String accEntityId = param.getString("accEntityId");
        ////资金组织id
        //String accEntityIdOld = param.getString("accEntityId");
        ////获得会计主体id，并用他查本位币
        //FundsOrgDTO fundsOrgDTO = fundsOrgQueryServiceComponent.getByIdWithFinOrg(accEntityIdOld);
        //String accEntityId = fundsOrgDTO.getFinorgid();
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId); /* 暂不修改 已登记*/
        if (accEntity.size() == 0) {
            return ResultMessage.success();
        }
        log.error("accEntity.get(0).get(currency) ========== " + accEntity.get(0).get("currency"));
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(String.valueOf(accEntity.get(0).get("currency")));
        if (currencyTenantDTO == null) {
            return ResultMessage.success();
        }
        return ResultMessage.data(currencyTenantDTO);
    }

    @Override
    public String getExchangeRate(CtmJSONObject param) throws Exception {
        Map<String, Object> queryCondition = Maps.newHashMap();
        queryCondition.put("sourceCurrencyId", param.get("currencyId"));
        queryCondition.put("targetCurrencyId", param.get("natCurrencyId"));
        queryCondition.put("quotationDate", param.get("vouchDate"));
        queryCondition.put("dr", 0);
        queryCondition.put("enable", 1);
        queryCondition.put("tenant", InvocationInfoProxy.getTenantid());
        List<Map<String, Object>> rates = QueryBaseDocUtils.queryExchangeRateByCondition(queryCondition); /* 暂不修改 已登记*/
        if (rates.size() > 0) {
            return ResultMessage.data(rates);
        }
        queryCondition.put("sourceCurrencyId", param.get("natCurrencyId"));
        queryCondition.put("targetCurrencyId", param.get("currencyId"));
        rates = QueryBaseDocUtils.queryExchangeRateByCondition(queryCondition); /* 暂不修改 已登记*/
        if (rates.size() > 0) {
            Map<String, Object> rate = rates.get(0);
            if (rate.get("exchangeRate") instanceof Double) {
                BigDecimal calRate = BigDecimal.ONE.divide(BigDecimal.valueOf((Double) rate.get("exchangeRate")), 6, BigDecimal.ROUND_HALF_UP);
                rate.put("exchangeRate", calRate);
            } else {
                BigDecimal calRate = BigDecimal.ONE.divide((BigDecimal) rate.get("exchangeRate"), 6, BigDecimal.ROUND_HALF_UP);
                rate.put("exchangeRate", calRate);
            }

        }
        return ResultMessage.data(rates);
    }


    @Override
    public Date getPubTsById(String entityName, Long id) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("pubts");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        schema.appendQueryCondition(queryConditionGroup);
        return ValueUtils.isNotEmptyObj(MetaDaoHelper.queryOne(entityName, schema)) ?
                (Date) MetaDaoHelper.queryOne(entityName, schema).get("pubts") : null;
    }

    @Override
    public void refreshPubTs(String entityName, List<Object> ids, CtmJSONArray rows) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,pubts");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
        schema.appendQueryCondition(queryConditionGroup);
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(entityName, schema, null);
        Iterator<BizObject> iterator = bizObjects.iterator();
        BizObject bizObject = null;
        while (iterator.hasNext()) {
            bizObject = iterator.next();
            for (int i = 0; i < rows.size(); i++) {
                if (bizObject.getId().equals(((Map) rows.get(i)).get("id"))) {
                    ((Map) rows.get(i)).put("pubts", bizObject.getPubts());
                    break;
                }
            }
        }
    }


    @Override
    public void refreshPubTsNew(String entityName, List<Object> ids, CtmJSONArray rows) throws Exception {

        List<BizObject> bizObjects = new ArrayList<>();
        //遍历ids 使用MetaDaoHelper.findById 去查询数据 追加到 bizObjects
        for (Object id : ids) {
            BizObject bizObjectWithoutChildren = MetaDaoHelper.findById(entityName, id, 1);
            bizObjects.add(bizObjectWithoutChildren);
        }
        Iterator<BizObject> iterator = bizObjects.iterator();
        BizObject bizObject = null;
        while (iterator.hasNext()) {
            bizObject = iterator.next();
            for (int i = 0; i < rows.size(); i++) {
                Object rowItem = rows.get(i);
                if (rowItem instanceof Map) {
                    Map<?, ?> rowMap = (Map<?, ?>) rowItem;
                    Object bizId = bizObject.getId();
                    Object rowId = rowMap.get("id");

                    if (bizId != null && Objects.equals(bizId.toString(), rowId != null ? rowId.toString() : null)) {
                        ((Map) rows.get(i)).put("pubts", bizObject.getPubts());
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param accEntity
     * @param moduleCode
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author tongyd
     * @Description 获取模块默认的业务账簿
     * @Date 2019/10/29
     * @Param [accEntity, moduleCode]
     */
    @Override
    public Map<String, Object> getModuleDefaultAccBook(String accEntity, String moduleCode) throws Exception {
        QuerySchema schema = new QuerySchema().addSelect("*,bdModularEnabletimeVO.*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("owerorg").eq(accEntity),
                QueryCondition.name("stopstatus").eq(0),
                QueryCondition.name("booktype").eq(1),
                QueryCondition.name("systemgene").eq(1),
                QueryCondition.name("accpurposes.isdefault").eq(1),
                QueryCondition.name("bdModularEnabletimeVO.code").eq(moduleCode),
                QueryCondition.name(ICmpConstant.TENANT).eq(AppContext.getCurrentUser().getTenant()));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(AccBookType.ENTITY_NAME, schema, ISchemaConstant.MDD_SCHEMA_FINBD).get(0);
    }

    /**
     * @param condition
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author tongyd
     * @Description 根据条件获取期间方案
     * @Date 2019/10/31
     * @Param [condition]
     */
    @Override
    public Map<String, Object> getPeriodByCondition(Map<String, Object> condition) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            queryConditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryOne(Period.ENTITY_NAME, schema);
    }

    @Override
    public List<Map<String, Object>> getTransTypeByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            if ("extend_attrs_json".equals(entry.getKey())) {
                conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).like(entry.getValue()));
            } else {
                conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
            }
        }
        schema.addCondition(conditionGroup);
        //默认值放在第一个
        schema.addOrderBy(new QueryOrderby("default", "desc"));
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据数据权限获取对应交易类型
     * https://gfwiki.yyrd.com/pages/viewpage.action?pageId=7506343
     *
     * @param condition billtype_id 单据类型id;dr=0;enable=1
     *                  billnum 对应需要过滤数据权限的单据
     */
    @Override
    public List<Map<String, Object>> getTransTypeByDataPermission(Map<String, Object> condition) throws Exception {
        DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
        //不包含直接返回空
        if (!condition.containsKey("billnum") || !condition.containsKey("sbillnum")) {
            return new ArrayList<>();
        }

        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //单据类型id
        if (condition.containsKey("billtype_id")) {
            conditionGroup.appendCondition(QueryCondition.name("billtype_id").eq(condition.get("billtype_id")));
        } else {
            return new ArrayList<>();
        }
        if (IBillNumConstant.BANKRECONCILIATIONLIST.equals(condition.get("billnum").toString()) || IBillNumConstant.CMP_MYBILLCLAIM_LIST.equals(condition.get("billnum").toString())) {
            if (IBillNumConstant.FUND_COLLECTION.equals(condition.get("sbillnum").toString())) {// 资金收款单
                requestDto.setEntityUri(FundCollection.ENTITY_NAME);
                requestDto.setServiceCode(IServicecodeConstant.FUNDCOLLECTION);
                //
                requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{ICmpConstant.TRADE_TYPE});
            } else if (IBillNumConstant.FUND_PAYMENT.equals(condition.get("sbillnum").toString())) {// 资金付款单
                requestDto.setEntityUri(FundPayment.ENTITY_NAME);
                requestDto.setServiceCode(IServicecodeConstant.FUNDPAYMENT);
                //
                requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{ICmpConstant.TRADE_TYPE});
            } else if ("collection".equals(condition.get("sbillnum").toString())) { //收款单
                //应收应付的收款单、付款单和付款退款单在元数据上对交易类型未加数据权限管控标签，导致数据权限控制不生效，与产品沟通后，先预留代码，由项目推动收付方改造
                requestDto.setEntityUri("earap.collection.CollectionHeader");
                requestDto.setServiceCode("ear_bill_collection");
                requestDto.setSysCode("EAR");
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{"bustype"});
            } else if ("payment".equals(condition.get("sbillnum").toString())) { //付款单
                requestDto.setEntityUri("earap.payment.PaymentHeader");
                requestDto.setServiceCode("eap_bill_payment");
                requestDto.setSysCode("EAP");
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{"bustype"});
            } else if ("apRefund".equals(condition.get("sbillnum").toString())) { //付款退款单
                requestDto.setEntityUri("earap.apRefund.PaymentRefundHeader");
                requestDto.setServiceCode("eap_bill_refund");
                requestDto.setSysCode("EAP");
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{"bustype"});
            } else if ("receipt_voucher".equals(condition.get("sbillnum").toString())) { //建筑云-收款单
                requestDto.setEntityUri("receipt_payment_mgmt.receipt_payment_mgmt.receiptVoucher");
                requestDto.setServiceCode("receipt_voucher");
                requestDto.setSysCode("receipt_payment_mgmt");
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{"bustype"});
            } else if ("payment_refund_voucher".equals(condition.get("sbillnum").toString())) { //建筑云-付款退款单
                requestDto.setEntityUri("receipt_payment_mgmt.receipt_payment_mgmt.paymentRefundVoucher");
                requestDto.setServiceCode("payment_refund_voucher");
                requestDto.setSysCode("receipt_payment_mgmt");
                //设置交易类型
                requestDto.setFieldNameArgs(new String[]{"bustype"});
            } else {
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }

        requestDto.setYxyUserId(AppContext.getUserId().toString());
        requestDto.setYhtTenantId(InvocationInfoProxy.getTenantid());
        requestDto.setYxyTenantId(AppContext.getTenantId().toString());
        requestDto.setHaveDetail(true);
        RPCStubBeanFactory rpChainBeanFactory = new RPCStubBeanFactory(IDomainConstant.MDD_DOMAIN_AUTH, "c87e2267-1001-4c70-bb2a-ab41f3b81aa3", null, IDataPermissionService.class);
        rpChainBeanFactory.afterPropertiesSet();
        IDataPermissionService remoteBean = (IDataPermissionService) rpChainBeanFactory.getObject();

        conditionGroup.appendCondition(QueryCondition.name("dr").eq(condition.getOrDefault("dr", "0")));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(condition.getOrDefault("enable", "1")));

        DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
        List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
        for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
            String[] ids = resultDto.getValues();
            conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        }
        schema.addCondition(conditionGroup);
        //默认值放在第一个
        schema.addOrderBy(new QueryOrderby("default", "desc"));
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据汇率类型获取汇率
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String getExchangeRateByRateType(CtmJSONObject param) throws Exception {
        List<Map<String, Object>> rates = new ArrayList<>();
        Date vouchDate = DateUtils.strToDate(param.getString("vouchDate"));
        if (StringUtils.isEmpty(param.getString("exchangeRateType")) || vouchDate == null) {
            return ResultMessage.data(rates);
        }
        Map<String, Object> rateMap = new HashMap<>();
        if (!ObjectUtils.isEmpty(param.getBoolean("isNewExchRate")) && param.getBoolean("isNewExchRate")) {
            CmpExchangeRateVO newExchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode(param.getString("currencyId"), param.getString("natCurrencyId"), vouchDate, param.getString("exchangeRateType"));
            rateMap.put("exchangeRate", newExchangeRateWithMode.getExchangeRate());
            rateMap.put("exchangeRateOps", newExchangeRateWithMode.getExchangeRateOps());
            rates.add(rateMap);
            return ResultMessage.data(rates);
        }
        CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(param.getString("currencyId"), param.getString("natCurrencyId"), vouchDate, param.getString("exchangeRateType"));
        if (cmpExchangeRateVO == null) {
            String oriCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(param.getString("currencyId")).getName();
            String natCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(param.getString("natCurrencyId")).getName();
            String exchangeRateTypeName = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateTypeById(param.getString("exchangeRateType")).getName();
            throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                    exchangeRateTypeName, oriCurrencyName, natCurrencyName));
        }
        BigDecimal rate = cmpExchangeRateVO.getExchangeRate();
        short exchangeRateOps = cmpExchangeRateVO.getExchangeRateOps();
        rateMap.put("exchangeRate", rate);
        rateMap.put("exchangeRateOps", exchangeRateOps);
        rates.add(rateMap);
        return ResultMessage.data(rates);
    }


    /**
     * 跟会计主体查询汇率
     * 根据资金组织去查会计主体，再获取会计主体对应的业务账簿下默认的汇率类型
     * 入参是资金组织和会计主体都行，结果一样，拿到的都是会计主体对应的汇率类型
     *
     * @param orgid
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getDefaultExchangeRateType(String orgid) throws Exception {
        //QuerySchema schema = new QuerySchema().addSelect("exchangerate");
        //QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //conditionGroup.appendCondition(QueryCondition.name("id").eq(orgid));
        //schema.addCondition(conditionGroup);
        //List<Map<String, Object>> query = MetaDaoHelper.query("aa.baseorg.FinanceOrgMV", schema, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
        String accEntityDefaultExchangeRateType = fundsOrgQueryServiceComponent.getAccEntityDefaultExchangeRateType(orgid);
        if (!StringUtils.isEmpty(accEntityDefaultExchangeRateType)) {
            //if (query != null && !query.isEmpty()) {

            //BillContext billContext1 = new BillContext();
            //billContext1.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
            //billContext1.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            //QuerySchema schema1 = QuerySchema.create().addSelect("*");
            //QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            ////conditionGroup1.appendCondition(QueryCondition.name("id").eq(query.get(0).get("exchangerate")));
            //conditionGroup1.appendCondition(QueryCondition.name("id").eq(accEntityDefaultExchangeRateType));
            //conditionGroup1.appendCondition(QueryCondition.name("enable").eq(1));
            //schema1.addCondition(conditionGroup1);
            //List<Map<String, Object>> query1 = MetaDaoHelper.query(billContext1, schema1);
            //if (query1 != null && !query1.isEmpty()) {
            //    return query1.get(0);
            //}
            //工具类
            // TODO: 2024/9/2 DTO和元数据查询后结果是否一致
            ExchangeRateTypeVO exchangeRate = baseRefRpcService.queryExchangeRateById(accEntityDefaultExchangeRateType);
            if (exchangeRate == null) {
                throw IMultilangConstant.noRateError/* "未取到汇率" */;
            }
            ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
            Map<String, Object> map = objectMapper.convertValue(exchangeRate, Map.class);
            return map;
        }
        return Collections.emptyMap();
    }

    /**
     * 新增接口
     * 根据资金组织去查汇率类型
     * 入参是资金组织
     * 汇率类型，资金收款、资金付款、外汇付款的换出汇率类型按照资金组织查询和过滤，其他的都按会计主体
     *
     * @param fundsOrgid
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getDefaultExchangeRateTypeByFundsOrgid(String fundsOrgid) throws Exception {
        if (Objects.isNull(fundsOrgid)) {
            return Collections.emptyMap();
        }
        ////汇率类型，非汇率
        //QuerySchema schema = new QuerySchema().addSelect("exchangerate");
        //QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //conditionGroup.appendCondition(QueryCondition.name("id").eq(orgid));
        //schema.addCondition(conditionGroup);
        //List<Map<String, Object>> query = MetaDaoHelper.query("org.func.FundsOrg", schema, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);

        FundsOrgDTO fundsOrgDTO = fundsOrgQueryServiceComponent.getById(fundsOrgid);
        if (Objects.isNull(fundsOrgDTO)) {
            return Collections.emptyMap();
        }
        //查询资金组织汇率类型
        String accEntityDefaultExchangeRateType = fundsOrgDTO.getExchangerate();
        if (accEntityDefaultExchangeRateType != null) {
            //if (query != null && !query.isEmpty()) {
            //    BillContext billContext1 = new BillContext();
            //    billContext1.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
            //    billContext1.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            //    QuerySchema schema1 = QuerySchema.create().addSelect("*");
            //    QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            //    //conditionGroup1.appendCondition(QueryCondition.name("id").eq(query.get(0).get("exchangerate")));
            //    conditionGroup1.appendCondition(QueryCondition.name("id").eq(accEntityDefaultExchangeRateType));
            //    conditionGroup1.appendCondition(QueryCondition.name("enable").eq(1));
            //    schema1.addCondition(conditionGroup1);
            //    List<Map<String, Object>> query1 = MetaDaoHelper.query(billContext1, schema1);
            //    if (query1 != null && !query1.isEmpty()) {
            //        return query1.get(0);
            //    }

            //工具类
            // TODO: 2024/9/2 DTO和元数据查询后结果是否一致
            ExchangeRateTypeVO exchangeRate = baseRefRpcService.queryExchangeRateById(accEntityDefaultExchangeRateType);
            if (exchangeRate == null) {
                throw IMultilangConstant.noRateError/* "未取到汇率" */;
            }
            ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
            Map<String, Object> map = objectMapper.convertValue(exchangeRate, Map.class);
            return map;
        }
        return Collections.emptyMap();
    }

    @Override
    public ExchangeRateTypeVO getExchangeRateType(String code) throws Exception {
        BdRequestParams params = new BdRequestParams();
        params.setCode(code);
        ExchangeRateTypeVO exchangeRateTypeVO = exchangeRateTypeQueryService.queryExchangeRateTypeByCondition(params);

        return exchangeRateTypeVO;
    }

//    /**
//     * 根据会计主体获取总账下默认的汇率类型
//     *
//     * @param orgid
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public ExchangeRateTypeVO getDefaultExchangeRateTypeFromGl(String orgid) throws Exception {
//        QuerySchema schema = new QuerySchema().addSelect("id,code,name,ratetype");
//        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
//                QueryCondition.name(IBussinessConstant.ACCENTITY).eq(orgid)
//        );
//        schema.addCondition(conditionGroup);
//        List<Map<String, Object>> query = MetaDaoHelper.query("epub.accountbook.AccountBook", schema, ISchemaConstant.MDD_SCHEMA_FIEPUB);
//        if (query != null && !query.isEmpty()) {
//            ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateTypeById(query.get(0).get("ratetype").toString());
//            if (exchangeRateTypeVO != null) {
//                return exchangeRateTypeVO;
//            }
//        }
//        return Collections.emptyMap();
//    }

    @Override
    public Map<String, String> getOrgs() throws Exception {
        ////会计主体
        //BillContext billContextFinanceOrg = new BillContext();
        //billContextFinanceOrg.setFullname("aa.baseorg.FinanceOrgMV");
        //billContextFinanceOrg.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        //QueryConditionGroup groupBankFinanceOrg = QueryConditionGroup.and(
        //        QueryCondition.name("1").eq(1)
        //);
        //List<Map<String, Object>> dataListFinanceOrg = MetaDaoHelper.queryAll(billContextFinanceOrg, "code,name", groupBankFinanceOrg, null);
        //Map<String, String> orgRepeatMap = new HashMap<>();
        //Map<String, String> orgMap = new HashMap<>();
        //if (dataListFinanceOrg != null && dataListFinanceOrg.size() > 0) {
        //    for (int i = 0; i < dataListFinanceOrg.size(); i++) {
        //        Map<String, Object> dataFinanceOrgMap = dataListFinanceOrg.get(i);
        //        String code = (String) dataFinanceOrgMap.get("code");
        //        String name = (String) dataFinanceOrgMap.get("name");
        //        if (!StringUtils.isEmpty(name)) {
        //            //相同name判段码值,码值相同为重复，码值不同逗号拼接
        //            if (orgMap.containsKey(name)) {
        //                String orgMapCode = orgMap.get(name);
        //                if (code.equals(orgMapCode)) {
        //                    orgRepeatMap.put(name, code);
        //                } else {
        //                    orgMapCode = orgMapCode + "," + code;
        //                    orgMap.put(name, orgMapCode);
        //                }
        //            } else {
        //                orgMap.put(name, code);
        //            }
        //
        //        }
        //    }
        //}
        //if (orgMap != null && orgMap.size() > 0) {
        //    log.info("Organizational unit usage value:" + CtmJSONObject.toJSONString(orgMap));
        //}
        //if (orgRepeatMap != null && orgRepeatMap.size() > 0) {
        //    log.debug("Organizational unit  value repeat:" + CtmJSONObject.toJSONString(orgRepeatMap));
        //}
        //
        //return orgMap;

        //资金组织
        // TODO: 2024/8/28 有数量限制吗？
        ConditionDTO condition = ConditionDTO.newCondition();
        List<FundsOrgDTO> fundsOrgDTOS = fundsOrgQueryServiceComponent.getByCondition(condition);
        List<Map<String, Object>> dataListFinanceOrg = new ArrayList<>();
        if (fundsOrgDTOS != null && !fundsOrgDTOS.isEmpty()) {
            for (FundsOrgDTO fundsOrgDTO :
                    fundsOrgDTOS) {
                Map<String, Object> describe = new HashMap<>();
                describe.put("code", fundsOrgDTO.getCode());
                describe.put("name", fundsOrgDTO.getName());
                dataListFinanceOrg.add(describe);

            }
        }


        Map<String, String> orgRepeatMap = new HashMap<>();
        Map<String, String> orgMap = new HashMap<>();
        if (dataListFinanceOrg != null && dataListFinanceOrg.size() > 0) {
            for (int i = 0; i < dataListFinanceOrg.size(); i++) {
                Map<String, Object> dataFinanceOrgMap = dataListFinanceOrg.get(i);
                String code = (String) dataFinanceOrgMap.get("code");
                String name = (String) dataFinanceOrgMap.get("name");
                if (!StringUtils.isEmpty(name)) {
                    //相同name判段码值,码值相同为重复，码值不同逗号拼接
                    if (orgMap.containsKey(name)) {
                        String orgMapCode = orgMap.get(name);
                        if (code.equals(orgMapCode)) {
                            orgRepeatMap.put(name, code);
                        } else {
                            orgMapCode = orgMapCode + "," + code;
                            orgMap.put(name, orgMapCode);
                        }
                    } else {
                        orgMap.put(name, code);
                    }

                }
            }
        }
        if (orgMap != null && orgMap.size() > 0) {
            if (log.isInfoEnabled()) {
                log.info("Organizational unit usage value:" + CtmJSONObject.toJSONString(orgMap));
            }
        }
        if (orgRepeatMap != null && orgRepeatMap.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Organizational unit  value repeat:" + CtmJSONObject.toJSONString(orgRepeatMap));
            }
        }

        return orgMap;
    }

    /**
     * 根据银行流水号查询付款工作台
     *
     * @param dealdetails
     */
    @Override
    public List<Map<String, Object>> getPayBillByBankDealdetails(List<String> dealdetails) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("transeqno,project");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("transeqno").in(dealdetails)
        );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> payBillMaps = MetaDaoHelper.query(PayBill.ENTITY_NAME, schema);
        return payBillMaps;
    }

    @Override
    public List<Map<String, Object>> getProjectVOs(List<String> projectIds) throws Exception {
        QuerySchema schema = new QuerySchema().addSelect("id,code,name");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("id").in(projectIds),
                QueryCondition.name("enable").eq(1),
                QueryCondition.name("dr").eq(0)
        );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> projectVOs = MetaDaoHelper.query("bd.project.ProjectVO", schema, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
        return projectVOs;
    }


    @Override
    public List<Map<String, Object>> getExchangeRateList(CtmJSONObject param) throws Exception {
        Map<String, Object> queryCondition = Maps.newHashMap();
        queryCondition.put("sourceCurrencyId", param.get("currencyId"));
        queryCondition.put("targetCurrencyId", param.get("natCurrencyId"));
        queryCondition.put("quotationDate", param.get("vouchDate"));
        queryCondition.put("dr", 0);
        queryCondition.put("enable", 1);
        queryCondition.put("tenant", InvocationInfoProxy.getTenantid());
        List<Map<String, Object>> rates = QueryBaseDocUtils.queryExchangeRateByCondition(queryCondition);/* 暂不修改 已登记*/
        if (rates.size() > 0) {
            return rates;
        }
        queryCondition.put("sourceCurrencyId", param.get("natCurrencyId"));
        queryCondition.put("targetCurrencyId", param.get("currencyId"));
        rates = QueryBaseDocUtils.queryExchangeRateByCondition(queryCondition);/* 暂不修改 已登记*/
        if (rates.size() > 0) {
            Map<String, Object> rate = rates.get(0);
            if (rate.get("exchangeRate") instanceof Double) {
                BigDecimal calRate = BigDecimal.ONE.divide(BigDecimal.valueOf((Double) rate.get("exchangeRate")), 6, BigDecimal.ROUND_HALF_UP);
                rate.put("exchangeRate", calRate);
            } else {
                BigDecimal calRate = BigDecimal.ONE.divide((BigDecimal) rate.get("exchangeRate"), 6, BigDecimal.ROUND_HALF_UP);
                rate.put("exchangeRate", calRate);
            }

        }
        return rates;
    }

    @Override
    public Boolean getEnabledPeriod(CtmJSONObject param) throws Exception {
//此方法无人调用 先注释掉
//        Date enabledBeginData = FINBDApiUtil.getFI4BDService().getEnabledPeriodBeginDate((String) param.get(IBussinessConstant.ACCENTITY),"STWB");
//
//        if (enabledBeginData != null ){
//            if(enabledBeginData.compareTo((Date) param.get("vouchdate")) < 0){
//                return true;
//            }
//        }else{
//            return false;
//        }
        return false;
    }

    /**
     * 根据银行交易流水号去查交易明细
     *
     * @param bankSqeNos
     * @return
     */
    @Override
    public List<Map<String, Object>> getBankDealdetailByBankSeqNos(List<String> bankSqeNos) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("bankseqno,bankdetailno");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("bankseqno").in(bankSqeNos)
        );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> bankDealDetailMaps = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME, schema);
        return bankDealDetailMaps;
    }

    @Override
    public String getCurrency(CtmJSONObject param) throws Exception {
        String currency = param.getString("currency");
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
        if (currencyTenantDTO == null) {
            return ResultMessage.success();
        }
        return ResultMessage.data(currencyTenantDTO);
    }

    @Override
    public Map<String, Object> queryTransTypeById(String billtype, String def, String code) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,name,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("billtype_id").eq(billtype));
        if ("1".equals(def)) {
            conditionGroup.appendCondition(QueryCondition.name("default").eq(def));
        }
        if (ValueUtils.isNotEmpty(code)) {
            conditionGroup.appendCondition(QueryCondition.name("code").eq(code));
        }
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        if (ValueUtils.isNotEmptyObj(AppContext.getYTenantId())) {
//            conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> queryTransTypes = MetaDaoHelper.query(billContext, schema);
        return CollectionUtils.isNotEmpty(queryTransTypes) ? queryTransTypes.get(0) : null;
    }

    @Override
    public Map<String, Object> queryTransTypeByForm_id(String form_id) throws Exception {
        BillContext bc = new BillContext();
        bc.setFullname("bd.bill.BillTypeVO");
        bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);         /* 暂不修改 已登记*/
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("form_id").eq(form_id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, Object> objectMap = list.get(0);
            if (ValueUtils.isNotEmptyObj(objectMap)) {
                String billTypeId = MapUtils.getString(objectMap, "id");
                return queryTransTypeById(billTypeId, "1", null);
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> queryTransTypesByForm_ids(String form_id) throws Exception {
        BillContext bc = new BillContext();
        bc.setFullname("bd.bill.BillTypeVO");
        bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);         /* 暂不修改 已登记*/
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("form_id").eq(form_id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
        if (CollectionUtils.isNotEmpty(list)) {
            List<Map<String, Object>> resultMap = new ArrayList<>();
            Map<String, Object> objectMap = list.get(0);
            if (ValueUtils.isNotEmptyObj(objectMap)) {
                String billTypeId = MapUtils.getString(objectMap, "id");
                resultMap.addAll(queryTransTypesById(billTypeId));
            }
            return resultMap;
        }
        return null;
    }

    /**
     * 过滤收付款下面的交易类型
     *
     * @param billTypeId
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> queryTransTypesById(String billTypeId) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,name,code");

        QueryConditionGroup condition1 = QueryConditionGroup.and(QueryConditionGroup.or(
                QueryCondition.name("code").eq(null),
                QueryCondition.name("code").eq("''"),
                QueryCondition.name("code").not_eq("cmp_fund_payment_delegation")
        ));
        QueryConditionGroup condition2 = QueryConditionGroup.and(QueryConditionGroup.or(
                QueryCondition.name("code").eq(null),
                QueryCondition.name("code").eq("''"),
                QueryCondition.name("code").not_eq("cmp_fundcollection_delegation")
        ));
        QueryConditionGroup condition = QueryConditionGroup.and(
                QueryCondition.name("dr").in(new String[]{"0", "2"}),
                QueryCondition.name("enable").eq(1),
                QueryCondition.name("billtype_id").eq(billTypeId)
        );
        condition.appendCondition(condition1);
        condition.appendCondition(condition2);

        schema.addCondition(condition);
        List<Map<String, Object>> queryTransTypes = MetaDaoHelper.query(billContext, schema);
        return queryTransTypes;
    }

    @Override
    public String catBillType(String yhtTenantId, String busType) throws Exception {
        String billTypeId;
        BdTransType transTypesById = iTransTypeService.getTransTypesById(yhtTenantId, busType);
        billTypeId = (transTypesById != null ? transTypesById.getBillTypeId() : "");
        return billTypeId;
    }

    /**
     * 获取默认交易类型的编码
     *
     * @param busType
     * @return
     * @throws Exception
     */
    @Override
    public String getDefaultTransTypeCode(String busType) throws Exception {
        TransTypeQueryParam param = new TransTypeQueryParam();
        param.setId(busType);
        param.setTenantId(AppContext.getYTenantId());
        List<BdTransType> transTypes = iTransTypeService.queryTransTypes(param);
        if (CollectionUtils.isEmpty(transTypes)) {
            return null;
        }
        return transTypes.get(0).getCode();
    }

    /**
     * 根据交易类型id查询交易类型档案信息*
     *
     * @param busType
     * @return
     * @throws Exception
     */
    @Override
    public List<BdTransType> getTransTyp(String busType) throws Exception {
        TransTypeQueryParam param = new TransTypeQueryParam();
        param.setId(busType);
        param.setTenantId(AppContext.getYTenantId());
        List<BdTransType> transTypes = iTransTypeService.queryTransTypes(param);
        if (CollectionUtils.isEmpty(transTypes)) {
            return null;
        }
        return transTypes;
    }

    @Override
    public void setTransTypeValueForBizObject(BizObject bill, String formId) {
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(formId));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            Map<String, Object> tradetypeMap = queryTransTypeById(billTypeId, "1", null);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                bill.set("tradetype", tradetypeMap.get("id"));
                bill.set("tradetype_name", tradetypeMap.get("name"));
                bill.set("tradetype_code", tradetypeMap.get("code"));
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
        }
    }

    @Override
    public void setAccentityRawForBizObject(BizObject bill, String formId) {
        String accEntity = bill.get(IBillConst.ACCENTITY);
        if (accEntity == null) {
            return;
        }
        try {
            AccentityUtil.fillAccentityRawFiledsToBizObjectbyAccentityId(bill, accEntity);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100568"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050017", "通过资金组织给会计主体赋值失败") /* "通过资金组织给会计主体赋值失败" */, e);
        }
    }

    /**
     * <h2>根据组织过滤供应商和客户</h2>
     *
     * @param billDataDto :
     * @param bills       :
     * @author Sun GuoCai
     * @since 2022/11/4 10:52
     */
    @Override
    public void filterMerchantRefAndVendorByOrg(BillDataDto billDataDto, List<BizObject> bills) throws Exception {
        FilterVO condition = ValueUtils.isNotEmptyObj(billDataDto.getCondition()) ? billDataDto.getCondition() : new FilterVO();
        BizObject bill = null;
        if (bills.size() > 0) {
            bill = bills.get(0);
        }
        Set<String> orgidts = new HashSet<>();// 控制组织
        assert bill != null;
        if (bill == null || bill.get(IBillConst.ACCENTITY) == null) {
            return;
        }
        String accentity = bill.get(IBillConst.ACCENTITY);
        if (accentity.contains("[")) {
            CtmJSONArray array = CtmJSONArray.parseArray(accentity);
            for (Object obj : array) {
                orgidts.add(String.valueOf(obj));
            }
        } else {
            orgidts.add(accentity);
        }
        // 查询核算委托关系的组织
        String org = null;
        if (bill.get(IBillConst.ORG) != null) {
            org = bill.get(IBillConst.ORG);
        }
        if (org != null && !org.equals(accentity)) {
            orgidts.add(org);
            Set<String> orgidtsTmp = FIDubboUtils.getDelegateHasSelf(org);
            orgidts.addAll(orgidtsTmp);
            orgidts.remove(accentity);
        } else {
            Set<String> orgidtsTmp = FIDubboUtils.getDelegateHasSelf(accentity);
            orgidts.addAll(orgidtsTmp);
        }
        String[] orgIds = orgidts.toArray(new String[0]);

        String refCode = billDataDto.getrefCode();
        switch (refCode) {
            case IRefCodeConstant.AA_MERCHANTREF:
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantAppliedDetail.stopstatus", ICmpConstant.QUERY_IN, new Integer[]{0}));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("merchantAppliedDetail.merchantApplyRangeId.orgId", ICmpConstant.QUERY_IN, orgIds));
                break;
            case IRefCodeConstant.YSSUPPLIER_AA_VENDOR:
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorextends.stopstatus", ICmpConstant.QUERY_EQ, 0));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("vendorApplyRange.org", ICmpConstant.QUERY_IN, orgIds));
                break;
            default:
                break;
        }
        billDataDto.setCondition(condition);
    }

    @Override
    public Map<String, Object> queryAutoConfigByAccentity(String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("accentity,name,settlemode,receiveQuickType,payQuickType,isGenerateFundCollection,isSettleSuccessToPost, isShareVideo,checkFundPlan");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        if (query.size() > 0) {
            return query.get(0);
        }
        return null;
    }

    @Override
    public Map<String, Object> queryAutoConfigTenant() throws Exception {
        return queryAutoConfigByAccentity("666666");
    }

    @Override
    public Map<String, Object> getDefaultBankAccountByOrgId(String orgId, String currency) throws Exception {
        if (orgId == null || currency == null) {
            return null;
        }

        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);        /* 暂不修改 已登记*/
        EnterpriseParams params = new EnterpriseParams();
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("orgid").eq(orgId));
        conditionGroup.appendCondition(QueryConditionGroup.or(QueryCondition.name("dr").eq(0)), QueryCondition.name("dr").is_null());
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY_REf).eq(currency));
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY_DEFAULT_REf).eq(1));
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY_ENABLE_REf).eq(1));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (query.size() > 0) {
            return query.get(0);
        }
        return null;
    }

    @Override
    public EnterpriseBankAcctVO getDefaultBankAccount(Object accentity, Object currency) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setOrgid(accentity.toString());
        List<String> currencyList = new ArrayList<>();
        currencyList.add(currency.toString());
        enterpriseParams.setCurrencyIDList(currencyList);
        enterpriseParams.setCurrencyIsDefault(true);
        List<EnterpriseBankAcctVO> bankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (bankAccounts.size() > 0) {
            return bankAccounts.get(0);
        }
        return null;
    }

    /**
     * 获取税率
     *
     * @param code
     * @return
     */
    @Override
    public String getTaxRate(String code, String name) throws Exception {
        TaxRateQueryParam params = new TaxRateQueryParam();
        params.setCode(code);
        params.setName(name);
        BdTaxRateVO taxRateVO = baseRefRpcService.queryTaxRateByCondition(params);
        if (taxRateVO != null) {
            return ResultMessage.data(taxRateVO);
        }
        return ResultMessage.success();
    }

    /**
     * 获取税率ByID
     *
     * @param id
     * @return
     */
    @Override
    public BdTaxRateVO getTaxRateById(String id) throws Exception {
        BdTaxRateVO taxRateVO = baseRefRpcService.queryTaxRateById(id);
        if (taxRateVO != null) {
            return taxRateVO;
        }
        return null;
    }

    /**
     * 获取税率  税局税种税率档案替换税目税率档案
     *
     * @param id
     * @return
     */
    @Override
    public String getTaxRateArchive(String id) throws Exception {
        TaxRateQueryCondition params = new TaxRateQueryCondition();
        params.setId(id);
        //只查询状态为启用的数据
        params.setEnables(Arrays.asList(1));
        TaxRateArchiveDto taxRateVO = iTaxRateArchIrisService.queryOneByParam(params);
        if (taxRateVO != null) {
            return ResultMessage.data(taxRateVO);
        }
        return ResultMessage.success();
    }

    @Override
    public List<PayApplicationBill> updateStatePayApplyBill(String action, Set<Object> set) throws Exception {
        List<PayApplicationBill> oldPayApplyBillList = new ArrayList<>();
        List<PayApplicationBill> newPayApplyBillList = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (Object o : set) {
            Long srcbillitemid = Long.parseLong(o.toString());
            PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
            if (null != payApplicationBill_b) {
                Long mainid = payApplicationBill_b.getMainid();
                ids.add(mainid);
            }
        }
        List<Map<String, Object>> mapList = MetaDaoHelper.queryByIds(PayApplicationBill.ENTITY_NAME, "*", ids.toArray(new Long[0]));
        for (Map<String, Object> map : mapList) {
            PayApplicationBill payApplicationBill = new PayApplicationBill();
            payApplicationBill.init(map);
            newPayApplyBillList.add(payApplicationBill);
            oldPayApplyBillList.add(payApplicationBill);
        }
        for (PayApplicationBill payApplicationBill : newPayApplyBillList) {
            if (AUDIT.equals(action) && (null == payApplicationBill.getPaidAmountSum()
                    || (BigDecimal.ZERO.compareTo(payApplicationBill.getPaidAmountSum()) == 0))) {
                payApplicationBill.setPayBillStatus(PayBillStatus.PendingPayment);
            } else {
                //下游付款单取消审核，回退到付款金额为0时，支付状态退回为“已审核”
                if (null == payApplicationBill.getPaidAmountSum()
                        || (BigDecimal.ZERO.compareTo(payApplicationBill.getPaidAmountSum()) == 0)) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PendingApproval);
                }
                //下游付款单取消审核，回退到付款金额为部分成功时，支付状态退回为“部分付款”
                if (null != payApplicationBill.getPaidAmountSum()
                        && (payApplicationBill.getPaymentApplyAmountSum().compareTo(payApplicationBill.getPaidAmountSum()) != 0)) {
                    payApplicationBill.setPayBillStatus(PayBillStatus.PartialPayment);
                }
            }
            payApplicationBill.setEntityStatus(EntityStatus.Update);
        }
        log.error("payment audit state back write, newPayApplyBillList = {}", JsonUtils.toJson(newPayApplyBillList));
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, newPayApplyBillList);
        return oldPayApplyBillList;
    }

    /**
     * 启独立事务 更新单据支付状态为：支付中, 不受网络因素影响
     * 只有当前单据状态为1、4、6(预下单成功，支付失败，支付不明) 的可更新
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public int updatePayForPayStatus(PayBill payBill) {
        int num = 0;
        try {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("porderId", payBill.getPorderid());
            param.put("tenant_id", AppContext.getTenantId());
            param.put("id", payBill.getId());
            param.put("paystatus", PayStatus.Paying.getValue());
            param.put("item", PayStatus.PreSuccess.getValue());
            num = SqlHelper.update(PAYBILLMAPPER + ".updatePayStatus", param);
        } catch (Exception e) {

        }
        return num;
    }

    /**
     * 启独立事务 更新单据支付状态为：支付中, 不受网络因素影响
     * 只有当前单据状态为4(预下单成功) 的可更新
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public int updateSalaryForPayStatus(Salarypay Salarypay) throws Exception {
        int num = 0;
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("porderId", Salarypay.getPorderid());
        param.put("tenant_id", AppContext.getTenantId());
        param.put("id", Salarypay.getId());
        param.put("paystatus", PayStatus.Paying.getValue());
        param.put("item", PayStatus.PreSuccess.getValue());
        num = SqlHelper.update(SALARYMAPPER + ".updatePayStatus", param);
        return num;
    }

    /**
     * 启独立事务 更新单据支付状态为：支付中, 不受网络因素影响
     * 只有当前单据状态为4(预下单成功) 的可更新
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public int updateTransferAccountsForPayStatus(TransferAccount transferAccount) throws Exception {
        int num = 0;
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("porderId", transferAccount.getPorderid());
        param.put("tenant_id", AppContext.getTenantId());
        param.put("id", transferAccount.getId());
        param.put("paystatus", PayStatus.Paying.getValue());
        param.put("item", PayStatus.PreSuccess.getValue());
        num = SqlHelper.update(TRANSFERACCOUNT + ".updatePayStatus", param);
        return num;
    }

    @Override
    public Map<String, Object> getVoucherInitBalMes(String accentity, String bankaccount, Long bankreconciliationscheme, String currency, short reconciliationDataSource) throws Exception {
        //币种获取
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);

        BigDecimal coinitloribalance = BigDecimal.ZERO;
        if (reconciliationDataSource == ReconciliationDataSource.Voucher.getValue()) {
            CtmJSONObject ret = CmpCommonUtil.getVoucherBalance(null, true, accentity, bankaccount, bankreconciliationscheme, currency, false);
            coinitloribalance = ret.getBigDecimal("subjectBalance").setScale(currencyDTO.getMoneydigit(), moneyRound);
        } else {
            BizObject bizObject = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, bankreconciliationscheme);
            Date enableDate = (Date) bizObject.get("enableDate");
            Map<String, SettlementDetail> settlementDetailMap = DailyCompute.imitateDailyCompute(accentity, currency, null, bankaccount, "2", "2", DateUtils.dateAddDays(enableDate, -1));
            String key = accentity + bankaccount + currency;
            SettlementDetail settlementDetail = settlementDetailMap.get(key.replace("null", ""));
            if (null == settlementDetail) {
                settlementDetail = new SettlementDetail();
                settlementDetail.setTodayorimoney(BigDecimal.ZERO);
            }
            coinitloribalance = settlementDetail.getTodayorimoney().setScale(currencyDTO.getMoneydigit(), moneyRound);
        }

        Map<String, Object> result = new HashMap<>();
        if (coinitloribalance.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("coinitloribalance", BigDecimalUtils.safeSubtract(false, BigDecimal.ZERO, coinitloribalance));
            result.put("direction", Direction.Credit.getValue());
        } else {
            result.put("coinitloribalance", coinitloribalance);
            result.put("direction", Direction.Debit.getValue());
        }
        return result;

    }

    @Override
    public Map<String, Object> getUserIdByYhtUserId(String yhtUserId) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.Staff");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create().addSelect("id,name");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("user_id").eq(yhtUserId));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (query != null && query.size() > 0) {
            return query.get(0);
        }
        return null;
    }

    /**
     * <h2>根据转换规则转换数据</h2>
     *
     * @param vo : 推单参数
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @throws Exception: java.lang.Exception
     * @author Sun GuoCai
     * @since 2022/10/18 11:11
     */
    @Override
    public List<Map<String, Object>> commonBillConvertRuleHandler(PushAndPullVO vo, String subOriginName, String subTargetName) throws Exception {
        MakeBillRule makeBillRule = this.makeBillRuleService.findDetailListByGroup(vo.getCode(), vo.getGroupCode());
        if (null == makeBillRule) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100569"), MddMultilingualUtil.getFWMessage("P_YS_FW-PUB_MDD-BACK_0001065718", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DD", "生单配置为空!") /* "生单配置为空!" */));
        } else {
            List<Map<String, Object>> tarList = new ArrayList<>();

            List<MakeBillRuleDetail> details = makeBillRule.makeBillRuleDetailList();
            Map<Integer, List<MakeBillRuleDetail>> groupDetailList = details.stream().collect(Collectors.groupingBy(item -> item.getMapped_relation()));
            List<MakeBillRuleDetail> main2main = groupDetailList.get(new Integer(0));//主对主
            List<MakeBillRuleDetail> main2c = groupDetailList.get(new Integer(1));//主对子
            List<MakeBillRuleDetail> c2main = groupDetailList.get(new Integer(2));//子对主
            List<MakeBillRuleDetail> c2c = groupDetailList.get(new Integer(3));//子对子
            List<BizObject> originlist = vo.getSourceData(); //来源单据
//            originlist.stream().forEach(obj ->{ // 遍历原始单据转换为目标单据
            for (BizObject obj : originlist) {

                BizObject mainobj = new BizObject();
                /**
                 * 单据转换时，需要将自由自定义项拍平，才可以转换，否则转换规则不生效。固定自定义项是全量匹配转换，只需要在预置脚本里按对象转换即可，无需拍平。
                 */
                if (obj.containsKey("headfree")) {
                    List<BizObject> headDefines = obj.get("headfree");
                    for (BizObject headDefine : headDefines) {
                        for (int i = 1; i < 61; i++) {
                            if (headDefine.get("define" + i) != null) {
                                obj.put("headfree!define" + i, headDefine.get("define" + i));
                            }
                            if (headDefine.get("define" + i + "_name") != null) {
                                obj.put("headfree!define" + i + "_name", headDefine.get("define" + i + "_name"));
                            }
                        }
                        if (headDefine.get("id") != null) {
                            obj.put("headfree!id", headDefine.get("id"));
                        }
                    }
                }
                if (main2main != null) {
                    for (MakeBillRuleDetail detail : main2main) {
                        if (detail.getMapped_type() == 3) {
                            mainobj.set(detail.getTarget_field(), detail.getOrigin_field());
                        } else if (detail.getMapped_type() == 0) {
                            mainobj.set(detail.getTarget_field(), obj.get(detail.getOrigin_field()));
                        }
                    }
                }

                Object childValue = obj.get(subOriginName);
                List<BizObject> childValues = null;
                List<BizObject> childTargetValues = new ArrayList<>();
                if (childValue != null && childValue instanceof List && c2c != null) {
                    childValues = (List<BizObject>) childValue;
                    for (BizObject child : childValues) {
                        BizObject childObjTar = new BizObject();
                        if (child.containsKey("bodyfree")) {
                            List<BizObject> bodyDefines = child.get("bodyfree");
                            for (BizObject bodyDefine : bodyDefines) {
                                for (int i = 1; i < 61; i++) {
                                    if (bodyDefine.get("define" + i) != null) {
                                        child.put("bodyfree!define" + i, bodyDefine.get("define" + i));
                                    }
                                    if (bodyDefine.get("define" + i + "_name") != null) {
                                        child.put("bodyfree!define" + i + "_name", bodyDefine.get("define" + i + "_name"));
                                    }
                                }
                                if (bodyDefine.get("id") != null) {
                                    child.put("bodyfree!id", bodyDefine.get("id"));
                                }
                            }
                        }
                        for (MakeBillRuleDetail detail : c2c) {
                            if (detail.getMapped_type() == 3) {
                                childObjTar.set(detail.getTarget_field(), detail.getOrigin_field());
                            } else if (detail.getMapped_type() == 0) {
                                childObjTar.set(detail.getTarget_field(), child.get(detail.getOrigin_field()));
                            }
                        }
                        childTargetValues.add(childObjTar);
                    }
                }
                if (childTargetValues != null && childTargetValues.size() > 0 && main2c != null) {
                    for (MakeBillRuleDetail detail : main2c) {
                        for (BizObject child : childTargetValues) {
                            child.set(detail.getTarget_field(), obj.get(detail.getOrigin_field()));
                        }
                    }
                }
                if (childTargetValues != null && childTargetValues.size() > 0 && c2main != null) {
                    for (MakeBillRuleDetail detail : c2main) {
                        BizObject bizObject = childTargetValues.get(0);
                        mainobj.set(detail.getTarget_field(), bizObject.get(detail.getOrigin_field()));

                    }
                }
                mainobj.set("FundCollection_b", childTargetValues);
                tarList.add(mainobj);
            }
//            });
            return tarList;
        }
    }

    @Override
    public Map<String, Object> commonBillConvertRuleHandler(PushAndPullVO pullVO) throws Exception {
        MakeBillRule makeBillRule = makeBillRuleService.findDetailListByGroup(pullVO.getCode(), pullVO.getGroupCode());
        BillContext billMappingOrgin = makeBillRuleService.getBillByCode(makeBillRule.getOrigin_type());
        BillContext billContext = (BillContext) billMappingOrgin.clone();
        Map<String, Object> content = assembleContent(pullVO, makeBillRule, billMappingOrgin, billContext);
        queryPullAndPushRule.execute(billContext, content);//查询转单数据
        divideVoucherForPullRule.execute(billContext, content);//分单子表
        RuleExecuteResult execute = beforePullAndPushRule.execute(billContext, content);//转换规则
        Map<String, Object> data = (Map<String, Object>) execute.getData();
        return data;
    }


    private Map<String, Object> assembleContent(PushAndPullVO pushAndPullVO, MakeBillRule makeBillRule, BillContext billMappingOrgin, BillContext billContext) throws Exception {
        Map<String, Object> content = new HashMap<String, Object>();
        String type = Constants.PULLTYPE;
        String code = pushAndPullVO.getCode();
        boolean isBusiObj = true;
        content.put("isBusiObj", isBusiObj);//用于转单之前处理数据标记
        billContext.setBillnum(code);//用界面传过来的值 ，业务对象的code后面会有_businessobject
        BillContext billMappingTar = makeBillRuleService.getBillByCode(makeBillRule.getTarget_type());
        billContext.setSubid(billMappingTar.getSubid());
        String fullNameTar = billMappingTar.getFullname();
        content.put("pushAndPullVO", pushAndPullVO);
        content.put("makeBillRule", makeBillRule);
        content.put("type", type);
        content.put("fullNameTar", fullNameTar);
        content.put("sourceFullName", billMappingOrgin.getFullname());
        content.put("voList", pushAndPullVO.getList());
        content.put("orignalBillContext", billMappingOrgin);
        content.put("targetBillContext", billMappingTar);
        content.put("sourceDatas", pushAndPullVO.getData());
        content.put("externalData", pushAndPullVO.getExternalData());
        buildRuleContext(billContext, content);//组装规则参数
        return content;
    }

    /**
     * 规则参数组装
     *
     * @param billContext
     * @param param
     * @return
     */
    private RuleContext buildRuleContext(BillContext billContext, Map<String, Object> param) {
        RuleContext ruleContext = new RuleContext();
        ruleContext.setMakeup(false);
        ruleContext.setAction(OperationTypeEnum.PULLANDPUSH.getValue());
        ruleContext.setCustomMap(param);
        ruleContext.setBillContext(billContext);
        if (billContext != null && billContext.getTenant() == null) { // 参照的场景 billContext 是new 出来的，租户为空，需要补充当前租户到ruleContext
            billContext.setTenant(AppContext.getTenantId());
            ruleContext.setTenantId(AppContext.getTenantId());
        }
        return ruleContext;
    }

    @Override
    public List<CapitalPlanExecuteModel> putCheckParameter(List<BizObject> fundBList, String pushType, String billNum, Map<String, Object> map) throws Exception {
        List<CapitalPlanExecuteModel> planList = new ArrayList<>();
        for (BizObject subObj : fundBList) {
            if (!ValueUtils.isNotEmptyObj(subObj.getString("fundPlanProject"))) {
                continue;
            }
            CapitalPlanExecuteModel plan = new CapitalPlanExecuteModel();
            plan.setAccentity(map.get("accentity").toString());
            plan.setCurrency(subObj.get("currency"));
            if (Objects.equals(pushType, IStwbConstant.EMPLOY)) {
                if (ValueUtils.isNotEmptyObj(map.get("settleFailed"))) {
                    plan.setExecuteAmount(subObj.get("settleerrorSum"));
                } else if (ValueUtils.isNotEmptyObj(map.get("reFund"))) {
                    BigDecimal partRefundSum = ValueUtils.isNotEmptyObj(subObj.get("partRefundSum"))
                            ? subObj.get("partRefundSum") : subObj.get("refundSum");
                    plan.setExecuteAmount(partRefundSum);
                } else {
                    plan.setExecuteAmount(subObj.get("oriSum"));
                }
                String fundPlanProject = subObj.getString("fundPlanProject");
                if (ValueUtils.isNotEmptyObj(fundPlanProject)) {
                    plan.setCapitalPlanDrawBId(Long.parseLong(fundPlanProject));
                } else {
                    continue;
                }
                String fundPlanProjectDetail = subObj.getString("fundPlanProjectDetail");
                if (ValueUtils.isNotEmptyObj(fundPlanProjectDetail)) {
                    plan.setCapitalDrawItemId(Long.parseLong(subObj.getString("fundPlanProjectDetail")));
                }
                plan.setExecuteDate((Date) map.get("vouchdate"));
            } else if (Objects.equals(pushType, IStwbConstant.RELEASE)) {
                if (ValueUtils.isNotEmptyObj(map.get("settleFailed"))) {
                    plan.setReleaseAmount(subObj.get("settleerrorSum"));
                } else if (ValueUtils.isNotEmptyObj(map.get("reFund"))) {
                    BigDecimal partRefundSum = ValueUtils.isNotEmptyObj(subObj.get("partRefundSum"))
                            ? subObj.get("partRefundSum") : subObj.get("refundSum");
                    plan.setReleaseAmount(partRefundSum);
                } else {
                    plan.setReleaseAmount(subObj.get("oriSum"));
                }
                String fundPlanProject = subObj.getString("fundPlanProject");
                if (ValueUtils.isNotEmptyObj(fundPlanProject)) {
                    plan.setReleaseCapitalPlanDrawBId(Long.parseLong(fundPlanProject));
                } else {
                    continue;
                }
                String fundPlanProjectDetail = subObj.getString("fundPlanProjectDetail");
                if (ValueUtils.isNotEmptyObj(fundPlanProjectDetail)) {
                    plan.setReleaseCapitalDrawItemId(Long.parseLong(fundPlanProjectDetail));
                }
                plan.setReleaseDate((Date) map.get("vouchdate"));
            }
            plan.setDept(subObj.get("dept"));
            if (IStwbConstantForCmp.CHENK.equals(pushType)) {
                plan.setBizbillno(map.get("code").toString());
                plan.setBillId(subObj.get("id").toString());
                String fundPlanProject = subObj.getString("fundPlanProject");
                if (ValueUtils.isNotEmptyObj(fundPlanProject)) {
                    plan.setCapitalPlanDrawBId(Long.parseLong(fundPlanProject));
                } else {
                    continue;
                }
                String fundPlanProjectDetail = subObj.getString("fundPlanProjectDetail");
                if (ValueUtils.isNotEmptyObj(fundPlanProjectDetail)) {
                    plan.setCapitalDrawItemId(Long.parseLong(subObj.getString("fundPlanProjectDetail")));
                }
                plan.setExecuteDate((Date) map.get("vouchdate"));
                plan.setExecuteAmount(subObj.get("oriSum"));
            } else {
                plan.setBillId(subObj.get("id").toString());
                plan.setBizbillno(map.get("code").toString());
            }

            plan.setSourceSys(IStwbConstantForCmp.CODE_ONE);
            if (IBillNumConstant.FUND_PAYMENT.equals(billNum)
                    || IBillNumConstant.FUND_PAYMENTLIST.equals(billNum)) {
                plan.setSourceBillType((short) 10);
            } else {
                plan.setSourceBillType((short) 7);
            }

            plan.setOppAcc(subObj.get("oppositeaccountno"));
            plan.setOppAccName(subObj.get("oppositeaccountname"));
            plan.setOppType(conversionCaobject(subObj.getShort("caobject")));
            if (!ValueUtils.isNotEmptyObj(subObj.get("settlemode"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100570"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AB", "占用资金计划，明细行结算方式不能为空!") /* "占用资金计划，明细行结算方式不能为空!" */);
            }
            plan.setSettlemode(subObj.get("settlemode"));

            Integer serviceAttr = ValueUtils.isNotEmptyObj(subObj.get("settlemode"))
                    ? getServiceAttr(subObj.get("settlemode")) : null;
            if (serviceAttr != null) {
                switch (serviceAttr) {
                    case 0:
                        if (ValueUtils.isNotEmptyObj(subObj.get("enterprisebankaccount"))) {
                            Map<String, Object> objectBankMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(subObj.get("enterprisebankaccount"));
                            plan.setOurAcc(ValueUtils.isNotEmptyObj(objectBankMap.get("account")) ? objectBankMap.get("account").toString() : null);
                        }
                        break;
                    case 1:
                        if (ValueUtils.isNotEmptyObj(subObj.get("cashaccount"))) {
                            Map<String, Object> objectCashMap = QueryBaseDocUtils.queryCashBankAccountById(subObj.get("cashaccount"));
                            plan.setOurAcc(ValueUtils.isNotEmptyObj(objectCashMap.get("code")) ? objectCashMap.get("code").toString() : null);
                        }
                        break;
                    case 2:
                        plan.setOurAcc(ValueUtils.isNotEmptyObj(subObj.get("noteno")) ? subObj.get("noteno").toString() : null);
                        break;
                    default:
                        plan.setOurAcc("");
                        break;
                }
            }
            plan.setForceEmploy(FundSettleStatus.SettlementSupplement.getValue() == Short.parseShort(subObj.get("settlestatus").toString()));
            planList.add(plan);
        }
        log.error("fund plan release or employ parameter handler,code={}, pushType={}, planList={}",
                map.get("code"), pushType, CtmJSONObject.toJSONString(planList));
        return planList;
    }

    private String conversionCaobject(short caobject){
        switch (caobject) {
            case 1:
                return ICsplConstant.Customer;
            case 2:
                return ICsplConstant.Supplier;
            case 3:
                return ICsplConstant.Employee;
            case 4:
                return ICsplConstant.Other;
            case 5:
                return ICsplConstant.CapBizObj;
            case 6:
                return ICsplConstant.InnerUnit;
            default:
                return "";
        }
    }

    /**
     * 薪资支付适配资金计划项目：组装推送的参数*
     *
     * @param salaryList
     * @param pushType
     * @param billNum
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public List<CapitalPlanExecuteModel> putCheckParameterSalarypay(List<BizObject> salaryList, String pushType, String billNum, Map<String, Object> map) throws Exception {
        List<CapitalPlanExecuteModel> planList = new ArrayList<>();
        for (BizObject subObj : salaryList) {
            CapitalPlanExecuteModel plan = new CapitalPlanExecuteModel();
            plan.setAccentity(map.get("accentity").toString());
            plan.setCurrency(subObj.get("currency"));

            if (Objects.equals(pushType, IStwbConstant.EMPLOY)) {
                // 部分支付失败
                if (ValueUtils.isNotEmptyObj(map.get("settleFailed"))) {
                    // 传入失败金额
                    plan.setExecuteAmount(subObj.get("olcfailmoney"));
                } else {
                    plan.setExecuteAmount(subObj.get("oriSum"));
                }
                plan.setCapitalPlanDrawBId(Long.parseLong(subObj.get("fundPlanProject").toString()));
                plan.setExecuteDate((Date) map.get("vouchdate"));
            } else if (Objects.equals(pushType, IStwbConstant.RELEASE)) {
                // 部分支付失败
                if (ValueUtils.isNotEmptyObj(map.get("settleFailed"))) {
                    // 传入失败金额
                    plan.setReleaseAmount(subObj.get("olcfailmoney"));
                } else {
                    plan.setReleaseAmount(subObj.get("oriSum"));
                }
                plan.setReleaseCapitalPlanDrawBId(Long.parseLong(subObj.get("fundPlanProject").toString()));
                plan.setReleaseDate((Date) map.get("vouchdate"));
            }
            plan.setDept(subObj.get("dept"));
            if (IStwbConstantForCmp.CHENK.equals(pushType)) {
                plan.setBizbillno(map.get("code").toString());
                plan.setBillId(subObj.get("id").toString());
                plan.setCapitalPlanDrawBId(Long.parseLong(subObj.get("fundPlanProject").toString()));
                plan.setExecuteDate((Date) map.get("vouchdate"));
                plan.setExecuteAmount(subObj.get("oriSum"));
            } else {
                plan.setBillId(subObj.get("id").toString());
                plan.setBizbillno(map.get("code").toString());
            }

            plan.setSourceSys(IStwbConstantForCmp.CODE_ONE);
            if (IBillNumConstant.SALARYPAY.equals(billNum)) {
                plan.setSourceBillType((short) 120);
            }
//            plan.setOppAcc(subObj.get("oppositeaccountno"));
//            plan.setOppAccName(subObj.get("oppositeaccountname"));
            plan.setOppType(ICsplConstant.Employee);
            plan.setSettlemode(subObj.get("settlemode"));

            Integer serviceAttr = ValueUtils.isNotEmptyObj(subObj.get("settlemode"))
                    ? getServiceAttr(subObj.get("settlemode")) : null;
            if (serviceAttr != null && serviceAttr == 0) {
                if (ValueUtils.isNotEmptyObj(subObj.get("payBankAccount"))) {
                    Map<String, Object> objectBankMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(subObj.get("payBankAccount"));
                    plan.setOurAcc(objectBankMap.get("account").toString());
                }
            }
            plan.setForceEmploy(FundSettleStatus.SettlementSupplement.getValue() == Short.parseShort(subObj.get("settlestatus").toString()));
            planList.add(plan);
        }
        log.error("fund plan release or employ parameter handler, planList={}", CtmJSONObject.toJSONString(planList));
        return planList;
    }


    @Override
    public List<CapitalPlanExecuteModel> putCheckParameterSalaryPayOldInterface(List<BizObject> salaryList, String pushType, String billNum, Map<String, Object> map) throws Exception {
        List<CapitalPlanExecuteModel> planList = new ArrayList<>();
        for (BizObject subObj : salaryList) {
            CapitalPlanExecuteModel plan = new CapitalPlanExecuteModel();
            plan.setAccentity(map.get("accentity").toString());
            plan.setCurrency(subObj.get("currency"));

            if (Objects.equals(pushType, IStwbConstant.EMPLOY)) {
                // 部分支付失败
                if (ValueUtils.isNotEmptyObj(map.get("settleFailed"))) {
                    // 传入失败金额
                    plan.setExecuteAmount(subObj.get("olcfailmoney"));
                } else {
                    plan.setExecuteAmount(subObj.get("oriSum"));
                }
                plan.setCapitalPlanDrawBId(Long.parseLong(subObj.get("fundPlanProject").toString()));
                plan.setExecuteDate((Date) map.get("vouchdate"));
            } else if (Objects.equals(pushType, IStwbConstant.RELEASE)) {
                // 部分支付失败
                if (ValueUtils.isNotEmptyObj(map.get("settleFailed"))) {
                    // 传入失败金额
                    plan.setReleaseAmount(subObj.get("olcfailmoney"));
                    plan.setExecuteAmount(subObj.get("olcfailmoney"));
                } else {
                    plan.setReleaseAmount(subObj.get("oriSum"));
                    plan.setExecuteAmount(subObj.get("oriSum"));
                }
                plan.setCapitalPlanDrawBId(Long.parseLong(subObj.get("fundPlanProject").toString()));
                plan.setExecuteDate((Date) map.get("vouchdate"));
                plan.setReleaseCapitalPlanDrawBId(Long.parseLong(subObj.get("fundPlanProject").toString()));
                plan.setReleaseDate((Date) map.get("vouchdate"));
            }

            plan.setDept(subObj.get("dept"));
            if (IStwbConstantForCmp.CHENK.equals(pushType)) {
                plan.setBizbillno("111111");
                plan.setBillId("");
            } else {
                plan.setBillId(subObj.get("id").toString());
                plan.setBizbillno(map.get("code").toString());
            }

            plan.setSourceSys(IStwbConstantForCmp.CODE_ONE);
            if (IBillNumConstant.SALARYPAY.equals(billNum)) {
                plan.setSourceBillType((short) 120);
            }
//            plan.setOppAcc(subObj.get("oppositeaccountno"));
//            plan.setOppAccName(subObj.get("oppositeaccountname"));
            plan.setOppType(conversionCaobject(subObj.getShort("caobject")));
            plan.setSettlemode(subObj.get("settlemode"));

            Integer serviceAttr = ValueUtils.isNotEmptyObj(subObj.get("settlemode"))
                    ? getServiceAttr(subObj.get("settlemode")) : null;
            if (serviceAttr != null && serviceAttr == 0) {
                if (ValueUtils.isNotEmptyObj(subObj.get("payBankAccount"))) {
                    Map<String, Object> objectBankMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(subObj.get("payBankAccount"));
                    plan.setOurAcc(objectBankMap.get("account").toString());
                }
            }
            plan.setForceEmploy(FundSettleStatus.SettlementSupplement.getValue() == Short.parseShort(subObj.get("settlestatus").toString()));
            planList.add(plan);
        }
        log.error("fund plan release or employ parameter handler, planList={}", CtmJSONObject.toJSONString(planList));
        return planList;
    }


    /**
     * 获取结算方式
     *
     * @param id 主键
     * @return Integer数量
     * @throws Exception 异常
     */
    @Override
    public Integer getServiceAttr(Long id) {
        ISettleMethodQueryService enterpriseBankAcctService = AppContext.getBean(ISettleMethodQueryService.class);
        SettleMethodQueryParam queryParam = new SettleMethodQueryParam();
        queryParam.setTenantId(AppContext.getTenantId());
        // queryParam.setIsEnabled(1);
        queryParam.setId(id);

        List<SettleMethodModel> settleMethodModelList;
        if (id == null) {
            settleMethodModelList = enterpriseBankAcctService.querySettleMethods(queryParam);
        } else {
            String cacheKey = AppContext.getTenantId().toString() + "_" + id.toString();
            List<SettleMethodModel> settleMethodModelCacheValue = settleMethodModelCache.getIfPresent(cacheKey);
            if (settleMethodModelCacheValue != null) {
                settleMethodModelList = settleMethodModelCacheValue;
            } else {
                settleMethodModelList = enterpriseBankAcctService.querySettleMethods(queryParam);
                settleMethodModelCache.put(cacheKey, settleMethodModelList);
            }
        }

        if (settleMethodModelList.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100571"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DE", "根据结算方式查询不到对应的业务属性,请检查是否停用或者删除!") /* "根据结算方式查询不到对应的业务属性,请检查是否停用或者删除!" */);
        }
        return settleMethodModelList.get(0).getServiceAttr();
    }


    /*
     *@Author rtsungc
     *@Description 根据会计主体，原币币种，查询资金计划参数是否受控
     *@Date 2022/4/27 16:28
     *@Param accentity,currency
     *@Return ControlSet
     **/
    @Override
    public Object queryStrategySetbByCondition(String accentity, String currency, Date expectdate) throws Exception {
        return iOpenApiForStwbService.queryControlDataByAccentity(accentity, currency, expectdate);
    }

    /**
     * 查询开通的服务，专用于银行对账单/认领单 业务关联和业务处理相关
     *
     * @return
     */
    @Override
    public CtmJSONObject queryOpenServiceInfo() {
        CtmJSONObject result = new CtmJSONObject();
        List<String> openList = new ArrayList<>();
        if ("1".equals(checkPaymentService())) {
            openList.add(IServicecodeConstant.PAYMENTBILL);
        }
        if ("1".equals(checkReceiveBillService())) {
            openList.add(IServicecodeConstant.RECEIVEBILL);
        }
        result.put("openList", openList);
        return result;
    }

    /**
     * 判断是否开通了付款工作台服务
     *
     * @return 0未开通；1已开通
     * @throws Exception
     */
    private String checkPaymentService() {
        String reFlag = AppContext.cache().get("checkPaymentService" + "_" + InvocationInfoProxy.getTenantid());
        if ("0".equals(reFlag)) {
            return "0";
        }
        // 开通服务信息,现金管理付款工作台
        ServiceVO serviceVO = iServiceManagerService.findByTenantIdAndServiceCode(InvocationInfoProxy.getTenantid(), IServicecodeConstant.PAYMENTBILL);
        if (serviceVO == null) {
            AppContext.cache().set("checkPaymentService" + "_" + InvocationInfoProxy.getTenantid(), "0", 10 * 60 * 60);//十小时
            return "0";
        } else {
            return "1";
        }
    }

    /**
     * 判断是否开通了付款工作台服务
     *
     * @return 0未开通；1已开通
     * @throws Exception
     */
    private String checkReceiveBillService() {
        String reFlag = AppContext.cache().get("checkReceiveBillService" + "_" + InvocationInfoProxy.getTenantid());
        if ("0".equals(reFlag)) {
            return "0";
        }
        // 开通服务信息,三方对账-银行对账单
        ServiceVO serviceVO = iServiceManagerService.findByTenantIdAndServiceCode(InvocationInfoProxy.getTenantid(), IServicecodeConstant.RECEIVEBILL);
        if (serviceVO == null) {
            AppContext.cache().set("checkReceiveBillService" + "_" + InvocationInfoProxy.getTenantid(), "0", 10 * 60 * 60);//十小时
            return "0";
        } else {
            return "1";
        }
    }

    /**
     * <h2>获取并设置当前单据附件数</h2>
     *
     * @param bizObject : 当前单据数据
     * @author Sun GuoCai
     * @since 2023/7/3 10:27
     */
    @Override
    public void getFilesCount(BizObject bizObject) {
        try {
            String id = ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId().toString() : "-1";
            long countFiles = cooperationFileService.countFiles(ICmpConstant.YONBIP_FI_CTMCMP, id);
            log.error("Get the number of attachments based on the bill ID success, code={}!, id={}, yTenant_id={}, filesCount={}",
                    bizObject.get("code"), bizObject.getId(), InvocationInfoProxy.getTenantid(), countFiles);
            if (countFiles > ICmpConstant.CONSTANT_ZERO) {
                bizObject.set("filesCount", countFiles);
            } else {
                bizObject.set("filesCount", ICmpConstant.CONSTANT_ZERO);
            }
        } catch (Exception e) {
            log.error("Get the number of attachments based on the bill ID fail!, code={}, id={}, yTenant_id={}, errorMsg={}",
                    bizObject.get("code"), bizObject.getId(), InvocationInfoProxy.getTenantid(), e.getMessage());
        }
    }

    /**
     * <h2>保存并提交</h2>
     *
     * @param billDataDto : 实体入参
     * @return com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult
     * @author Sun GuoCai
     * @since 2023/10/13 21:30
     */
    @Override
    public RuleExecuteResult doSaveAndSubmitAction(BillDataDto billDataDto) {
        String[] actionArray = new String[]{OperationTypeEnum.SAVE.getValue(), OperationTypeEnum.SUBMIT.getValue()};
        StringBuilder messageBulilder = new StringBuilder();
        RuleExecuteResult finalReturn = null;
        String blockedAction = null;
        Object finalData = null;
        String[] var7 = actionArray;
        int var8 = actionArray.length;
        for (int var9 = 0; var9 < var8; ++var9) {
            String action = var7[var9];
            try {
                if (null != finalData) {
                    if (OperationTypeEnum.SUBMIT.getValue().equals(action)) {
                        try {
                            String oriId = billDataDto.getId();
                            BizObject detail = null;
                            if (finalData instanceof BizObject) {
                                detail = (BizObject) finalData;
                            } else if (finalData instanceof Map) {
                                detail = new BizObject((Map) finalData);
                            } else {
                                detail = new BizObject();
                                detail.setId(oriId);
                            }

                            billDataDto.setId(null == detail.getId() ? null : detail.getId().toString());
                            Map detailQuery = BillBiz.detail(billDataDto);
                            billDataDto.setId(oriId);
                            if (null != detailQuery) {
                                CommonRuleUtils.clearMapParent(detailQuery);
                            }
                            billDataDto.setData(GsonHelper.ToJSon(detailQuery, "yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception var14) {
                            log.error("补全数据发生异常, e:{0}", var14);
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400610", "补全数据发生异常") /* "补全数据发生异常" */, var14);
                        }
                    } else {
                        if (finalData instanceof Map) {
                            CommonRuleUtils.clearMapParent((Map) finalData);
                        }
                        billDataDto.setData(GsonHelper.ToJSon(finalData, "yyyy-MM-dd HH:mm:ss"));
                    }
                }

                RuleExecuteResult ruleExecuteResult = this.billExecActionService.doExecActionRuleWithNewTranstaction(billDataDto, action);
                if (ruleExecuteResult.isCancel()) {
                    log.error(String.format("执行action=%s的规则 返回结果isCancel=true, message = %s;", action, ruleExecuteResult.getMessage()));
                }

                finalReturn = ruleExecuteResult;
                if (ruleExecuteResult.getMsgCode() != 1 || ruleExecuteResult.getCode() == 100004) {
                    blockedAction = action;
                    this.appendErrorMessage(messageBulilder, action, ruleExecuteResult.getMessage());
                    log.error(String.format("执行action=%s的规则 返回结果MsgCode=%d, message = %s; ", action, ruleExecuteResult.getMsgCode(), ruleExecuteResult.getMessage()));
                    break;
                }

                Object resultObj = ruleExecuteResult.getData();
                if (null != resultObj) {
                    finalData = resultObj;
                }

                if (OperationTypeEnum.SAVE.getValue().equals(action)) {
                    messageBulilder.append(InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A997C05B80010", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540060E", "保存成功") /* "保存成功" */));
                } else if (OperationTypeEnum.SUBMIT.getValue().equals(action)) {
                    messageBulilder.append(InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A997C05B80012", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540060F", "提交成功") /* "提交成功" */));
                }

                messageBulilder.append(",");
            } catch (Exception var15) {
                blockedAction = action;
                finalReturn = new RuleExecuteResult();
                log.error("doSaveAndSubmitAction执行action = {0} 的动作规则发生异常, e: {}", action, var15);
                this.appendErrorMessage(messageBulilder, action, var15.getMessage());
                break;
            }
        }

        finalReturn.setMessage(messageBulilder.toString());
        int msgcode = 1;
        if (OperationTypeEnum.SAVE.getValue().equals(blockedAction)) {
            msgcode = 999;
        } else if (OperationTypeEnum.SUBMIT.getValue().equals(blockedAction)) {
            msgcode = 910;
        }
        finalReturn.setMsgCode(msgcode);
        finalReturn.setData(finalData);
        return finalReturn;
    }

    private void appendErrorMessage(StringBuilder messageBulilder, String action, String errorMessage) {
        if (OperationTypeEnum.SAVE.getValue().equals(action)) {
            messageBulilder.append(InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A997C05B8000F", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400612", "保存失败") /* "保存失败" */));
        } else if (OperationTypeEnum.SUBMIT.getValue().equals(action)) {
            messageBulilder.append(InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A997C05B80011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400613", "提交失败") /* "提交失败" */));
        }

        messageBulilder.append(",");
        messageBulilder.append(InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A997C05B80013", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400611", "失败原因") /* "失败原因" */));
        messageBulilder.append(": ").append(errorMessage);
        messageBulilder.append(";");
    }

    /**
     * 获取结算方式
     *
     * @param ids 主键
     * @return Map<Long, Integer>
     * @throws Exception 异常
     */
    @Override
    public Map<Long, Integer> getServiceAttrs(List<Long> ids) {
        Map<Long, Integer> map = new HashMap<>();
        ISettleMethodQueryService enterpriseBankAcctService = AppContext.getBean(ISettleMethodQueryService.class);
        SettleMethodQueryParam queryParam = new SettleMethodQueryParam();
        queryParam.setTenantId(AppContext.getTenantId());
        queryParam.setIsEnabled(1);
        queryParam.setIds(ids);
        List<SettleMethodModel> settleMethodModelList = enterpriseBankAcctService.querySettleMethods(queryParam);
        if (settleMethodModelList.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100572"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180043", "根据结算方式查询不到对应的业务属性,请检查是否停用或者删除!") /* "根据结算方式查询不到对应的业务属性,请检查是否停用或者删除!" */);
        }
        for (SettleMethodModel settleMethodModel : settleMethodModelList) {
            map.put(settleMethodModel.getId(), settleMethodModel.getServiceAttr());
        }
        return map;
    }

    @Override
    public List<BankAccountSetting> queryAutocorrsettingByParam(CommonRequestDataVo param) throws Exception {
        List<String> accentityList = param.getAccentityList();
        List<String> enterpriseBankAccountList = param.getEnterpriseBankAccountList();
        List<BankAccountSetting> list = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
//        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
//                QueryCondition.name("accentity").in(accentityList));
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (accentityList != null) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityList));
        }
        if (enterpriseBankAccountList != null) {
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccountList));
        }
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        querySchema.addCondition(conditionGroup);
        list = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
        return list;
    }

    //银行账户是否开通银企联服务
    @Override
    public Boolean getOpenFlag(String bankaccount) throws Exception {
        Boolean openFlag = Boolean.FALSE;
        //查询银企联账号 cmp_bankaccountsetting，若此会计主体下有账户开通了银企联 则认为该租户开通了银企联 需要走后续逻辑，如果没记录则直接返回flase不校验
        QuerySchema querySettingSchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup bankAccountGroup = new QueryConditionGroup(ConditionOperator.and);
        bankAccountGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(bankaccount)));
        bankAccountGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("openFlag").eq(1)));
        querySettingSchema.addCondition(bankAccountGroup);
        List<AutoConfig> settingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, querySettingSchema);
        if (settingList != null && settingList.size() > 0) {
            //开通银企联服务
            openFlag = Boolean.TRUE;
        }
        return openFlag;
    }

    /**
     * 根据服务编码集合获取租户开通领域domainkey集合
     *
     * @param param serviceCodeList 待校验的服务编码集合
     *              参数为空时，获取全部开通domainkey*
     * @return 租户开通的领域domainkey结合
     * @throws Exception
     */
    @Override
    public Collection<String> checkOpenServiceList(CtmJSONObject param) throws Exception {
        Set<String> openDomainList = new HashSet<>();
        if (param == null || !param.containsKey("serviceCodeList") || param.getJSONArray("serviceCodeList") == null) {
            List<ServiceVO> serviceVOListAll = iServiceManagerService.findByTenantId(InvocationInfoProxy.getTenantid());
            for (ServiceVO serviceVO : serviceVOListAll) {
                if (!StringUtils.isEmpty(serviceVO.getDomainKey())) {
                    openDomainList.add(serviceVO.getDomainKey());
                }
            }
            return openDomainList;
        }
        CtmJSONArray checkArray = param.getJSONArray("serviceCodeList");
        List<String> checkList = checkArray.toJavaList(String.class);
        List<ServiceVO> serviceVOList = iServiceManagerService.findByTenantIdAndServiceCodeIn(InvocationInfoProxy.getTenantid(), checkList);

        if (serviceVOList != null && serviceVOList.size() > 0) {
            for (ServiceVO serviceVO : serviceVOList) {
                if (!StringUtils.isEmpty(serviceVO.getDomainKey())) {
                    openDomainList.add(serviceVO.getDomainKey());
                }
            }
        }
        return openDomainList;
    }

    @Override
    public void releaseFundPlanByCollection(BizObject bizObject) throws Exception {
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundCollectionBList = bizObject.get("FundCollection_b");
        for (BizObject biz : fundCollectionBList) {
            Object isToPushCspl = biz.get("isToPushCspl");
            if (ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.FUND_PLAN_PROJECT))
                    && ValueUtils.isNotEmptyObj(isToPushCspl)
                    && 1 == Integer.parseInt(isToPushCspl.toString())) {
                biz.set("isToPushCspl", 2);
                releaseFundBillForFundPlanProjectList.add(biz);
            }
        }
        if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("1.fundPayment", "fundPayment");
            Map<String, Object> map = new HashMap<>();
            map.put("accentity", bizObject.get("accentity"));
            map.put("vouchdate", bizObject.get("vouchdate"));
            map.put("code", bizObject.get("code"));
            jsonObject.put("2.map", map);
            List<CapitalPlanExecuteModel> checkObject = putCheckParameter(releaseFundBillForFundPlanProjectList, IStwbConstant.RELEASE, IBillNumConstant.FUND_COLLECTION, map);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                jsonObject.put("3.checkObject", checkObject);
                CapitalPlanExecuteResp capitalPlanExecuteResp;
                try {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    jsonObject.put("4.capitalPlanExecuteResp", capitalPlanExecuteResp);
                } catch (Exception e) {
                    log.error("releaseFundPlanByCollection error, errorMsg={}", e.getMessage());
                    jsonObject.put("5.errorMsg", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100573"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AA", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                } finally {
                    jsonObject.put("6.method",
                            "com.yonyoucloud.fi.cmp.common.service.CmCommonServiceImpl.releaseFundPlanByCollection#release");
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    ctmcmpBusinessLogService.saveBusinessLog(
                            jsonObject,
                            bizObject.getString(ICmpConstant.CODE),
                            IMsgConstant.FUND_COLLECTION_EMPLOY_AND_RELEASE_FUND_PLAN,
                            IServicecodeConstant.FUNDCOLLECTION,
                            IMsgConstant.FUND_COLLECTION,
                            OperCodeTypes.unlock.getDefaultOperateName());
                }
                if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                        && "500".equals(capitalPlanExecuteResp.getCode())
                        && capitalPlanExecuteResp.getSuccessCount() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100573"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AA", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
                }
                EntityTool.setUpdateStatus(releaseFundBillForFundPlanProjectList);
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, releaseFundBillForFundPlanProjectList);
            }
        }
    }


    @Override
    public void releaseFundPlanByPayment(BizObject bizObject) throws Exception {
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundCollectionBList = bizObject.get("FundPayment_b");
        for (BizObject biz : fundCollectionBList) {
            Object isToPushCspl = biz.get(ICmpConstant.IS_TO_PUSH_CSPL);
            if (ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.FUND_PLAN_PROJECT))
                    && ValueUtils.isNotEmptyObj(isToPushCspl)
                    && 1 == Integer.parseInt(isToPushCspl.toString())) {
                biz.set(ICmpConstant.IS_TO_PUSH_CSPL, 2);
                releaseFundBillForFundPlanProjectList.add(biz);
            }
        }
        if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            Map<String, Object> map = new HashMap<>();
            map.put(ICmpConstant.ACCENTITY, bizObject.get(ICmpConstant.ACCENTITY));
            map.put(ICmpConstant.VOUCHDATE, bizObject.get(ICmpConstant.VOUCHDATE));
            map.put(ICmpConstant.CODE, bizObject.get(ICmpConstant.CODE));
            jsonObject.put("2.map", map);
            List<CapitalPlanExecuteModel> checkObject = putCheckParameter(releaseFundBillForFundPlanProjectList, IStwbConstant.RELEASE, IBillNumConstant.FUND_PAYMENT, map);
            jsonObject.put("3.checkObject", checkObject);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                CapitalPlanExecuteResp capitalPlanExecuteResp;
                try {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    jsonObject.put("4.capitalPlanExecuteResp", capitalPlanExecuteResp);
                } catch (Exception e) {
                    log.error("releaseFundPlanByPayment error, errorMsg={}", e.getMessage());
                    jsonObject.put("5.errorMsg", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100573"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AA", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                } finally {
                    jsonObject.put("6.method", "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundPaymentUnSubmitReleaseFundPlan#release");
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    ctmcmpBusinessLogService.saveBusinessLog(
                            jsonObject,
                            bizObject.getString(ICmpConstant.CODE),
                            IMsgConstant.FUND_PAYMENT_EMPLOY_AND_RELEASE_FUND_PLAN,
                            IServicecodeConstant.FUNDPAYMENT,
                            IMsgConstant.FUND_PAYMENT,
                            OperCodeTypes.unlock.getDefaultOperateName());
                }
                if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                        && "500".equals(capitalPlanExecuteResp.getCode())
                        && capitalPlanExecuteResp.getSuccessCount() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100573"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AA", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
                }
                EntityTool.setUpdateStatus(releaseFundBillForFundPlanProjectList);
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, releaseFundBillForFundPlanProjectList);
            }
        }
    }

    @Override
    public List<String> getBankAcctInfos(List<String> orgValueList,
                                         List<String> classValueList, Object enterCountry, Object cashDirectLink) throws Exception {
        BamAccountInfoQueryReq addAccountInfoReq = new BamAccountInfoQueryReq();
        addAccountInfoReq.setAccentityList(orgValueList);
        if (classValueList != null && classValueList.size() > 0) {
            addAccountInfoReq.setAcctQualityCategory(classValueList);
        }
        if (enterCountry != null && !enterCountry.equals("")) {
            addAccountInfoReq.setIsOverseasAcct(enterCountry.toString());
        }
        if (cashDirectLink != null && !cashDirectLink.equals("")) {
            addAccountInfoReq.setCashDirectLink(cashDirectLink.toString());
        }
        BamResult<String> stringBamResult = RemoteDubbo.get(IAccountInfoQueryApiService.class,
                IDomainConstant.MDD_DOMAIN_BAM).queryAccountInfos(addAccountInfoReq);
        if (stringBamResult.getCode() == 200) {
            return stringBamResult.getDataList();
        }
        return null;
    }

    @Override
    public List<String> getValueList(FilterCommonVO vo) {
        List<String> valueList = new ArrayList<>();
        if (vo.getValue1() instanceof ArrayList) {
            valueList = (List<String>) vo.getValue1();
        } else {
            String orgValue = vo.getValue1().toString();
            valueList.add(orgValue);
        }
        return valueList;
    }

    @Override
    public Map<String,Object> getSwapOutExchangeRateName(CtmJSONObject param) throws Exception {
        // 会计主体设置汇率类型
        Map<String,Object> bill = new HashMap<>();
        String accEntityId = null != param.get(IBussinessConstant.ACCENTITY) ? String.valueOf(param.get(IBussinessConstant.ACCENTITY)) : null;
        Map<String, Object> defaultExchangeRateType = getDefaultExchangeRateType(accEntityId);
        if (defaultExchangeRateType != null && defaultExchangeRateType.get("id") != null) {
            bill.put("exchangeRateType", defaultExchangeRateType.get("id"));
            bill.put("exchangeRateType_name", defaultExchangeRateType.get("name"));
            bill.put("exchangeRateType_digit", defaultExchangeRateType.get("digit"));

            bill.put("swapOutExchangeRateType", defaultExchangeRateType.get("id"));
            bill.put("swapOutExchangeRateType_name", defaultExchangeRateType.get("name"));
            bill.put("swapOutExchangeRateType_digit", defaultExchangeRateType.get("digit"));
        }

        //资金组织查询会理替换换出汇率
        Map<String, Object> DefaultExchangeRateTypeOfFinOrg = getDefaultExchangeRateTypeByFundsOrgid(accEntityId);
        if (DefaultExchangeRateTypeOfFinOrg != null && DefaultExchangeRateTypeOfFinOrg.get("id") != null) {
            bill.put("swapOutExchangeRateType", DefaultExchangeRateTypeOfFinOrg.get("id"));
            bill.put("swapOutExchangeRateType_name", DefaultExchangeRateTypeOfFinOrg.get("name"));
            bill.put("swapOutExchangeRateType_digit", DefaultExchangeRateTypeOfFinOrg.get("digit"));
            bill.put("swapOutExchangeRateType_code", DefaultExchangeRateTypeOfFinOrg.get("code"));
        }
        return bill;
    }

    /**
     * 根据银行账号获取账户使用组织
     * @param param bankAccounts 银行账户id的数组
     * @return 账户使用组织集合，orgList
     */
    @Override
    public CtmJSONObject queryUseOrgListByBankAccounts(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //不包含银行账户id时
        if (!param.containsKey("bankAccounts") || StringUtils.isEmpty(param.getString("bankAccounts"))){
            result.put("count",0);
            return result;
        }
        //解析银行账户id
        List<String> bankAccountList = param.getJSONArray("bankAccounts").toJavaList(String.class);
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(bankAccountList);
        //查询授权使用组织
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVoWithRangeList = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
        List<CtmJSONObject> orgList = new ArrayList<>();
        if (CollectionUtils.isEmpty(enterpriseBankAcctVoWithRangeList)) {
            result.put("count",0);
            return result;
        }
        for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVoWithRangeList) {
            List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVOWithRange.getAccountApplyRange();
            if (CollectionUtils.isEmpty(orgRangeVOList)) {
                continue;
            }
            for (OrgRangeVO orgRangeVO : orgRangeVOList) {
                List<Map<String, Object>> accEntityes = AccentityUtil.getFundMapLIstById(orgRangeVO.getRangeOrgId());
                if (CollectionUtils.isEmpty(accEntityes)) {
                    continue;
                }
                String orgId = orgRangeVO.getRangeOrgId();
                // 检查orgList中是否已存在相同的orgid
                boolean exists = orgList.stream().anyMatch(org -> orgId.equals(org.get("orgid")));
                if (!exists) {
                    CtmJSONObject orgInfo = new CtmJSONObject();
                    orgInfo.put("orgid", orgId);
                    orgInfo.put("orgname", accEntityes.get(0).get("name"));
                    orgList.add(orgInfo);
                }
            }
        }
        result.put("count",orgList.size());
        result.put("orgList",orgList);
        return result;
    }

}
