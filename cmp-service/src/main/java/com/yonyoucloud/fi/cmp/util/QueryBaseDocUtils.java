package com.yonyoucloud.fi.cmp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.yonyou.diwork.service.IServiceManagerService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.BaseDeptDTO;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.org.dto.bp.SimpleBeginningPeriod;
import com.yonyou.iuap.org.service.itf.bp.IBeginningPeriodService;
import com.yonyou.iuap.org.service.itf.core.IBizDeptQueryService;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.api.YmsJdbcApiProvider;
import com.yonyou.iuap.yms.factory.YmsJdbcApiProviderFactory;
import com.yonyou.iuap.yms.processor.MapListProcessor;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IBankService;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.ServiceVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.cspl.openapi.IOpenApiService;
import com.yonyou.yonbip.ctm.cspl.vo.request.MinPeriodCapitalPlanDrawQueryVo;
import com.yonyou.yonbip.ctm.cspl.vo.response.ResponseResult;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.api.IPeriodOptimizeService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.bd.costcenter.CostCenter;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.exchangesettlement.ExchangeSettlementTradeCode;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.epub.accountbook.AccountBook;
import com.yonyoucloud.fi.epub.accountbook.AccountBookPeriod;
import com.yonyoucloud.fi.epub.accountbook.EnableModelParam;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Supplier;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;
import static com.yonyoucloud.fi.cmp.util.TaskUtils.enterpriseBankQueryService;

/**
 * @ClassName QueryBaseDocUtils
 * @Description 基础档案查询工具类
 * @Author tongyd
 * @Date 2019/6/17 13:37
 * @Version 1.0
 **/
@Slf4j
public class QueryBaseDocUtils {

    private static final String QUERY_ALL_TENANT = "SELECT DISTINCT ytenant_id FROM tenant";

    static FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent = AppContext.getBean(FundsOrgQueryServiceComponent.class);
    static IEnterpriseBankAcctService iEnterpriseBankAcctService = AppContext.getBean(IEnterpriseBankAcctService.class);

    public static List<MerchantDTO> getMerchantByIds(List<Long> merchantIds) throws Exception {
        if (merchantIds == null || merchantIds.size() == 0) {
            return null;
        }
        String[] fields = {MerchantFieldKeyConstant.CODE, MerchantFieldKeyConstant.NAME, MerchantFieldKeyConstant.ID};
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.getMerchantByIds(merchantIds, fields);
    }

    /**
     * 根据客户银行ids 批量查询客户银行账户
     *
     * @param ids
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountByIds(List<Long> ids) throws Exception {
        if (ids == null) {
            return null;
        }
        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
        agentFinancialQryDTO.setIds(ids);
        return queryCustomerBankAccountByCondition(agentFinancialQryDTO);
    }

    /*
     *@Author tongyd
     *@Description 根据ID获取企业银行账户
     *@Date 2019/5/14 16:28
     *@Param [id]
     *@Return void
     **/
    public static Map<String, Object> queryEnterpriseBankAccountById(Object id) throws Exception {
        if (null == id) {
            return null;
        }

        //改用公共接口
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(id.toString());
        //启用状态集合:0-未启用  1-启用  2-停用
        enterpriseParams.setEnables(new ArrayList<>(Arrays.asList(0, 1, 2)));
        //TODO 缺少字段： settleorgid，待平台补充
        EnterpriseBankAcctVO enterpriseBankAcctVO = iEnterpriseBankAcctService.queryByUniqueParam(enterpriseParams);
        if (enterpriseBankAcctVO == null) {
            return null;
        }
        ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
        Map<String, Object> accEntityMap = objectMapper.convertValue(enterpriseBankAcctVO, Map.class);
        return accEntityMap;

        //BillContext billContext = new BillContext();
        //billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        //billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        //QuerySchema schema = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,lineNumber,enable,acctType,tenant,acctName,acctopentype,settleorgid");
        //QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        //schema.addCondition(conditionGroup);
        //schema.addCompositionSchema(QuerySchema.create().name("currencyList").addSelect("*"));
        //List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        //if (CollectionUtils.isEmpty(query)){
        //    return null;
        //}
        //return MetaDaoHelper.query(billContext, schema).get(0);
    }

    public static Optional<EnterpriseBankAcctVO> queryEnterpriseBankAccountVOById(Object id) throws Exception {
        if (null == id) {
            return null;
        }

        //改用公共接口
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(id.toString());
        //启用状态集合:0-未启用  1-启用  2-停用
        enterpriseParams.setEnables(new ArrayList<>(Arrays.asList(0,1,2)));
        EnterpriseBankAcctVO enterpriseBankAcctVO = iEnterpriseBankAcctService.queryByUniqueParam(enterpriseParams);
        if (enterpriseBankAcctVO == null) {
            return null;
        }else {
            return Optional.ofNullable(enterpriseBankAcctVO);
        }
    }

    public static List<EnterpriseBankAcctVO> queryEnterpriseBankAccountVOListByIds(List<String > ids) throws Exception {
        ArrayList<EnterpriseBankAcctVO> enterpriseBankAcctVOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(ids)) {
            return enterpriseBankAcctVOList;
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(ids);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAll(enterpriseParams);
        return enterpriseBankAcctVOS;
    }


    /*
     *@Author yangjn
     *@Description 根据ID数组获取企业银行账户
     *@Date 2021/1/25 16:28
     *@Param [id]
     *@Return void
     **/
    public static List<Map<String, Object>> queryEnterpriseBankAccountByIdList(List<String> id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,lineNumber,enable,acctType,tenant,acctName,acctopentype");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("id").in(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     *@Author yangjn
     *@Description 根据资金组织id获取内部银行账户
     *@Date 2021/9/1 16:28
     *@Param orgId
     *@Return List<Map<String, Object>>
     **/
    public static List<Map<String, Object>> queryInnerBankAccountByParam(CtmJSONObject param) throws Exception {
        String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
        String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
        String[] accentityArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("acctopentype").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        if (accentityArr != null && accentityArr.length > 0) {
            conditionGroup.appendCondition(QueryCondition.name("orgid").in(accentityArr));
        }
        String[] banktypeArr;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
            if (banktypeArr != null && banktypeArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("bank").in(banktypeArr));
            }
        }
        String[] currencyArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
            if (currencyArr != null && currencyArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY_REf).in(currencyArr));
            }
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     *@Author tongyd
     *@Description 根据资金组织ID查询企业银行账户
     *@Date 2019/5/31 14:03
     *@Param [orgId]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> getEnterpriseBankAccountByOrgId(String orgId) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,lineNumber,enable,acctType,tenant,acctName,acctopentype");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("orgid").eq(orgId));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     *@Author lichaor
     *@Description 根据资金组织ID查询企业银行账户
     *@Date 2025/8/4 14:03
     *@Param [orgId]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
   /* public static List<Map<String, Object>> getEnterpriseBankAccountByAcctOpenType(Integer AcctOpenType) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,lineNumber,enable,acctType,tenant,acctName,acctopentype");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("acctopentype").not_eq(AcctOpenType));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }*/

    /**
     *@Author tongyd
     *@Description 根据资金组织ID查询企业银行账户
     *@Date 2019/5/31 14:03
     *@Param [orgId]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> getEnterpriseBankAccountByOrgIdList(List<String> orgIdArr) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,code,name,orgid,account,acctName,acctType");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("orgid").in(orgIdArr));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据自定义查询条件查询企业银行账户
     *@Date 2019/7/11 16:34
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> queryEnterpriseBankAccountByCondition(Map<String, Object> condition, BillContext oldBillContext) throws Exception {
        List<Object> accountIds = queryPermissionBankAccountId(oldBillContext, "enterprisebankaccount");
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        if (CollectionUtils.isNotEmpty(accountIds)) {
            conditionGroup.appendCondition(QueryCondition.name("id").in(accountIds));
        } else {
            return new ArrayList<Map<String, Object>>();
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }


    /*
     *@Author
     *@Description 根据自定义查询条件查询企业银行账户,增加自定义fieldname
     *@Date 2019/7/11 16:34
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> queryEnterpriseBankAccountByCondition(Map<String, Object> condition, BillContext oldBillContext, String fieldName) throws Exception {
        List<Object> accountIds = queryPermissionBankAccountId(oldBillContext, fieldName);
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        if (CollectionUtils.isNotEmpty(accountIds)) {
            conditionGroup.appendCondition(QueryCondition.name("id").in(accountIds));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据自定义查询条件查询企业现金账户
     *@Date 2019/7/11 16:34
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> queryEnterpriseCashAccountByCondition(Map<String, Object> condition, BillContext oldBillContext) throws Exception {
        List<Object> accountIds = queryPermissionBankAccountId(oldBillContext, "cashaccount");
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinCashacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        if (CollectionUtils.isNotEmpty(accountIds)) {
            conditionGroup.appendCondition(QueryCondition.name("id").in(accountIds));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    public static List<Map<String, Object>> queryEnterpriseCashAccountByIds(List<String> idArr) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinCashacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,code,name,orgid,account,accountOpenDate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("id").in(idArr));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }


    public static List<Map<String, Object>> queryEnterpriseCashAccountByAccounts(List<String> accountArr) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinCashacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,code,name,orgid,account");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("code").in(accountArr));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据ID获取现金银行账户
     *@Date 2019/5/14 16:28
     *@Param [id]
     *@Return void
     **/
    public static Map<String, Object> queryCashBankAccountById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinCashacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema).get(0);
    }

    /**
     * 根据权限过滤银行账户
     *
     * @param oldBillContext
     * @return
     * @throws Exception
     */
    private static List<Object> queryPermissionBankAccountId(BillContext oldBillContext, String fieldName) throws Exception {
        Map<String, List<Object>> accounts = AuthUtil.dataPermission("CM", oldBillContext.getFullname(), null, new String[]{fieldName});
        if (accounts == null || accounts.size() == 0) {
            return null;
        }
        return accounts.get(fieldName);
    }

    /**
     * 根据客户档案id查询客户基本信息
     *
     * @param merchantId
     * @return
     * @throws Exception
     */
    public static MerchantDTO getMerchantById(Long merchantId) throws Exception {
        if (merchantId == null) {
            return null;
        }
        String[] fields = {MerchantFieldKeyConstant.CODE, MerchantFieldKeyConstant.NAME,
                MerchantFieldKeyConstant.ID, MerchantFieldKeyConstant.PROFESS_SALESMAN_ID, MerchantFieldKeyConstant.RANGES_ORG_ID};
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.getMerchantById(merchantId, fields);
    }

    /**
     * 根据客户名称查询客户档案信息
     *
     * @param name
     * @return
     * @throws Exception
     */
    public static List<MerchantDTO> queryMerchantByName(String name) throws Exception {
        MerchantQryDTO merchantQryDTO = new MerchantQryDTO();
        String[] fields = {MerchantFieldKeyConstant.CODE, MerchantFieldKeyConstant.NAME, MerchantFieldKeyConstant.ID};
        merchantQryDTO.setFields(fields);
        merchantQryDTO.setName(name);
        HashSet<String> ascField = new HashSet<>();
        ascField.add(MerchantFieldKeyConstant.CODE);
        merchantQryDTO.setAscField(ascField);
        return listMerchantByQryDTO(merchantQryDTO);
    }

    /**
     * 根据客户编码查询客户档案信息
     *
     * @param code
     * @return
     * @throws Exception
     */
    public static List<MerchantDTO> queryMerchantByCode(String code) throws Exception {
        MerchantQryDTO merchantQryDTO = new MerchantQryDTO();
        String[] fields = {MerchantFieldKeyConstant.CODE, MerchantFieldKeyConstant.NAME, MerchantFieldKeyConstant.ID};
        merchantQryDTO.setFields(fields);
        merchantQryDTO.setCode(code);
        HashSet<String> ascField = new HashSet<>();
        ascField.add(MerchantFieldKeyConstant.CODE);
        merchantQryDTO.setAscField(ascField);
        return listMerchantByQryDTO(merchantQryDTO);
    }

    /**
     * 根据客户名称查询客户档案信息
     *
     * @param name
     * @return
     * @throws Exception
     */
    public static List<MerchantDTO> queryMerchantByNameAndOrg(String name, List<String> orgIdListNew) throws Exception {
        MerchantQryDTO merchantQryDTO = new MerchantQryDTO();
        String[] fields = {MerchantFieldKeyConstant.CODE, MerchantFieldKeyConstant.NAME, MerchantFieldKeyConstant.ID,MerchantFieldKeyConstant.DETAIL_STOP_STATUS};
        merchantQryDTO.setFields(fields);
        merchantQryDTO.setName(name);
        HashSet<String> ascField = new HashSet<>();
        ascField.add(MerchantFieldKeyConstant.CODE);
        merchantQryDTO.setAscField(ascField);
        merchantQryDTO.setOrgIdListNew(orgIdListNew);
        return listMerchantByQryDTO(merchantQryDTO);
    }


    /**
     * 根据条件查询客户信息，如根据客户名称查询
     *
     * @param merchantQryDTO 客户查询对象,集合最大1000条
     * @return
     * @throws Exception
     */
    public static List<MerchantDTO> listMerchantByQryDTO(MerchantQryDTO merchantQryDTO) throws Exception {
        if (merchantQryDTO.getFields() == null || merchantQryDTO.getFields().length == 0) {
            String[] fields = {MerchantFieldKeyConstant.CODE, MerchantFieldKeyConstant.NAME, MerchantFieldKeyConstant.ID};
            merchantQryDTO.setFields(fields);
        }
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.listMerchantByQryDTO(merchantQryDTO);
    }

    /**
     * 根据客户id查询客户适用范围
     *
     * @param merchantId 客户id
     * @return
     * @throws Exception
     */
    public static List<MerchantApplyRangeDTO> queryMerchantApplyRange(Long merchantId) throws Exception {
        if (merchantId == null) {
            return null;
        }
        MerchantApplyRangeQryDTO qryDTO = new MerchantApplyRangeQryDTO();
        qryDTO.setMerchantId(merchantId);
        qryDTO.setFields(new String[]{MerchantApplyRangeFieldKeyConstant.MERCHANT_ID, MerchantApplyRangeFieldKeyConstant.DETAIL_STOP_STATUS, MerchantApplyRangeFieldKeyConstant.ORG_ID, MerchantApplyRangeFieldKeyConstant.ID});
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.listMerchantApplyRange(qryDTO);
    }

    /**
     * 根据银行id列表或客户id或银行id和客户id分别查询银行账户
     *
     * @param id         银行账号id
     * @param merchantId 客户id
     * @param stopStatus 停用状态
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountById(Long id, Long merchantId, Boolean stopStatus) throws Exception {
        List<Long> ids = new ArrayList<>();
        List<Long> merchantIds = new ArrayList<>();
        if (id != null) {
            ids.add(id);
        }
        if (merchantId != null) {
            merchantIds.add(merchantId);
        }
        if (stopStatus == null) {
            stopStatus = Boolean.FALSE;
        }
        return queryCustomerBankAccountByIds(ids, merchantIds, stopStatus);
    }

    /**
     * 根据客户银行id查询客户银行账户
     *
     * @param id 客户银行账号id
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountById(Long id) throws Exception {
        if (id == null) {
            return null;
        }
        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
        agentFinancialQryDTO.setId(id);
        return queryCustomerBankAccountByCondition(agentFinancialQryDTO);
    }

    /**
     * 根据银行id列表或客户id或银行id和客户id分别查询银行账户
     *
     * @param ids         银行账号ids
     * @param merchantIds 客户ids
     * @param stopStatus  停用状态
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountByIds(List<Long> ids, List<Long> merchantIds, Boolean stopStatus) throws Exception {
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.listMerchantAgentFinancial(ids, merchantIds, stopStatus);
    }

    /**
     * 根据条件查询客户银行账户
     *
     * @param agentFinancialQryDTO 查询条件实体
     * @return
     * @throws Exception
     */
    public static List<AgentFinancialDTO> queryCustomerBankAccountByCondition(AgentFinancialQryDTO agentFinancialQryDTO) throws Exception {
        if (agentFinancialQryDTO.getFields() == null || agentFinancialQryDTO.getFields().length == 0) {
            agentFinancialQryDTO.setFields(new String[]{AgentFinancialFieldKeyConstant.MERCHANT_ID, AgentFinancialFieldKeyConstant.ID, AgentFinancialFieldKeyConstant.STOP_STATUS, AgentFinancialFieldKeyConstant.BANK_ACCOUNT,
                    AgentFinancialFieldKeyConstant.BANK_ACCOUNT_NAME, AgentFinancialFieldKeyConstant.OPEN_BANK, AgentFinancialFieldKeyConstant.MERCHANT_NAME, AgentFinancialFieldKeyConstant.IS_DEFAULT});
        }
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        return merchantService.listMerchantAgentFinancial(agentFinancialQryDTO);
    }

    /*
     *@Author tongyd
     *@Description 根据条件查询客户银行账户
     *@Date 2019/7/2 17:01
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> queryCustomerBankAccountByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.merchant.AgentFinancial");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询员工
     *@Date 2019/6/17 14:21
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryStaffById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.Staff");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> staffs = MetaDaoHelper.query(billContext, schema);
        if (staffs != null && staffs.size() > 0) {
            return staffs.get(0);
        }
        return null;
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询员工
     *@Date 2019/6/17 14:21
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static List<Map<String, Object>> queryStaffByIds(List<Object> ids) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.Staff");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,code,name");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * @param id
     * @return
     * @throws Exception
     * @Author xudya
     * *@Description 根据员工id查询员工所属组织
     */
    public static Map<String, Object> queryMainJobOrgByStaffId(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffMainJob");//员工任职（主职）
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("staff_id").eq(id));
        conditionGroup.appendCondition(QueryCondition.name("lastestjob").eq(1));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema).get(0);
    }

    /**
     * *@Description 根据员工id或者code查询员工
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryEmployeeByIdOrCode(Object id) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.staff.Staff");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(id));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(id));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("queryEmployeeByIdOrCode", e);
        }
        return query;
    }


    /**
     * *@Description 根据员工id或者code查询VendorBank
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryVendorBankByIdOrCode(Object id) throws Exception {
        List<Map<String, Object>> vendorBankVO = null;
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.vendor.VendorBank");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_YSSUPPLIER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("account").eq(id));
        schema.addCondition(conditionGroup);
        vendorBankVO = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(vendorBankVO)) {
            QuerySchema schemaId = QuerySchema.create();
            schemaId.addSelect("*");
            QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
            conditionGroupId.appendCondition(QueryCondition.name("id").eq(id));
            schemaId.addCondition(conditionGroupId);
            vendorBankVO = MetaDaoHelper.query(billContext, schemaId);
        }
        return vendorBankVO;
    }


    /**
     * *@Description 根据员工id或者code查询汇率
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryExchangeRateTypeByIdOrCode(Object id) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(id));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(id));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("queryExchangeRateTypeByIdOrCode", e);
        }
        return query;
    }


    public static List<Map<String, Object>> queryOrgByStaffId(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffJob");//员工任职（合集） // bd.staff.StaffJob
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("org_id,staff_id,dept_id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("staff_id").eq(id));
        //conditionGroup.appendCondition(QueryCondition.name("lastestjob").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        conditionGroup.appendCondition(QueryCondition.name("endflag").eq(0));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }


    /*
     *@Author tongyd
     *@Description 根据ID查询员工银行账户
     *@Date 2019/6/17 14:21
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryStaffBankAccountById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffBankAcct");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> staffaccounts = MetaDaoHelper.query(billContext, schema);
        if (staffaccounts != null && staffaccounts.size() > 0) {
            return staffaccounts.get(0);
        }
        return null;
    }

    //account
    public static Map<String, Object> queryStaffBankAccountByIdOrCode(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffBankAcct");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("account").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> staffaccounts = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(staffaccounts)) {
            QuerySchema schema1 = QuerySchema.create();
            schema1.addSelect("*");
            QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup1.appendCondition(QueryCondition.name("id").eq(id));
            schema1.addCondition(conditionGroup1);
            List<Map<String, Object>> staffaccounts1 = MetaDaoHelper.query(billContext, schema1);
            if (CollectionUtils.isNotEmpty(staffaccounts1)) {
                return staffaccounts1.get(0);
            }
        } else {
            return staffaccounts.get(0);
        }
        return null;
    }

    public static List<Map<String, Object>> queryStaffBankAccountByIds(List<Object> ids) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffBankAcct");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("account,bankname,currency,id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据条件查询员工银行账户
     *@Date 2019/7/2 17:06
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/

    @Resource
    IBankService bankService;

    public static List<Map<String, Object>> queryStaffBankAccountByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffBankAcct");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author qihaoc
     *@Description 根据员工id,批量查询员工银行账户
     *@Date 2025/7/5 17:06
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> queryStaffBankAccountByStaffIds(Set<String> StaffIds) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.staff.StaffBankAcct");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STAFFCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,staff_id,account,accountname");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("staff_id").in(StaffIds));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @Author tongyd
     * @Description 根据ID查询币种
     * @Date 2019/10/28
     * @Param [id]
     **/
    public static List<Map<String, Object>> queryCurrencyById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @Author tongyd
     * @Description 根据条件查询币种
     * @Date 2019/9/5
     * @Param [condition]
     **/
    public static List<Map<String, Object>> queryCurrencyByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询开户行
     *@Date 2019/6/27 20:59
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryDepositBankById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bank.BankDotVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> openBanks = MetaDaoHelper.query(billContext, schema);
        if (openBanks != null && openBanks.size() > 0) {
            return openBanks.get(0);
        }
        return null;
    }

    /**
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @Author tongyd
     * @Description 根据条件查询开户行
     * @Date 2019/10/16
     * @Param [condition]
     **/
    public static List<Map<String, Object>> queryDepositBankByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bank.BankDotVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询结算方式
     *@Date 2019/7/11 16:12
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> querySettlementWayById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.settlemethod.SettleMethod");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("code,name,serviceAttr,parent,isEnabled,id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> data = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(data)) {
            return null;
        } else {
            return data.get(0);
        }
    }

    /*
     *@Author tongyd
     *@Description 根据条件查询结算方式
     *@Date 2019/7/3 14:12
     *@Param [condition]
     *@Return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> querySettlementWayByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.settlemethod.SettleMethod");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询交易类型
     *@Date 2019/6/27 20:59
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryTransTypeById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> transTypes = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(transTypes)) {
            log.error("未获取到交易类型" + id);
            return null;
        } else {
            return transTypes.get(0);
        }
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询单据类型
     *@Date 2019/6/27 20:59
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryBillTypeById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.BillTypeVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema).get(0);
    }

    /**
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @Author tongyd
     * @Description 根据资金组织ID查询资金组织，替换原有方法
     * @Date 2019/10/28
     * @Param [id]
     **/
    // TODO: 2024/8/28 使用地方很多，待验证
    public static List<Map<String, Object>> queryAccRawEntityByAccEntityId(Object id) throws Exception {
        //BillContext billContext = new BillContext();
        //billContext.setFullname("aa.baseorg.FinanceOrgMV");
        //billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        //QuerySchema schema = QuerySchema.create();
        //schema.addSelect("*");
        //QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        //schema.addCondition(conditionGroup);
        //return MetaDaoHelper.query(billContext, schema);
        List<Map<String, Object>> finMapList = new ArrayList<>();
        String accEntityIdStr = id + "";
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accEntityIdStr)) {
            return finMapList;
        }
        Map<String, Object> finMap = AccentityUtil.getFinEntityMapByAccentityId(accEntityIdStr);
        if (finMap == null) {
            return finMapList;
        }
        finMapList.add(finMap);
        return finMapList;
    }

    /**
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @Author tongyd
     * @Description 根据资金组织ID查询资金组织[原来使用的方法]
     * @Date 2019/10/28
     * @Param [id]
     **/
    public static List<Map<String, Object>> queryAccRawEntityById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.FinanceOrgMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

//    /**
//     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
//     * @Author tongyd
//     * @Description 根据ID查询资金组织
//     * @Date 2019/10/28
//     * @Param [id]
//     **/
//    @Deprecated
//    public static List<Map<String, Object>> queryAccEntityById(Object id) throws Exception {
//        BillContext billContext = new BillContext();
//        billContext.setFullname("aa.baseorg.FinanceOrgMV");
//        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
//        QuerySchema schema = QuerySchema.create();
//        schema.addSelect("*");
//        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
//        schema.addCondition(conditionGroup);
//        return MetaDaoHelper.query(billContext, schema);
//    }

//    /**
//     * 根据条件查询资金组织
//     *
//     * @param condition
//     * @return
//     * @throws Exception
//     */
//    @Deprecated
//    public static List<Map<String, Object>> queryAccEntityByCondition(Map<String, Object> condition) throws Exception {
//        BillContext billContext = new BillContext();
//        billContext.setFullname("aa.baseorg.FinanceOrgMV");
//        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
//        QuerySchema schema = QuerySchema.create();
//        schema.addSelect("*");
//        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        for (Map.Entry<String, Object> entry : condition.entrySet()) {
//            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
//        }
//        schema.addCondition(conditionGroup);
//        return MetaDaoHelper.query(billContext, schema);
//    }

    /**
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @Author tongyd
     * @Description 根据条件查询汇率
     * @Date 2019/9/6
     * @Param [condition]
     **/
    public static List<Map<String, Object>> queryExchangeRateByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.exchangeRate.ExchangeRateVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            if ("quotationDate".equals(entry.getKey())) {
                conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).elt(entry.getValue()));
            } else {
                conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
            }
        }
        schema.addCondition(conditionGroup);
        QueryOrderby order = new QueryOrderby("quotationDate", "desc");
        schema.addOrderBy(order);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据条件查询汇率类型
     *
     * @param condition
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryExchangeRateTypeByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        QueryOrderby order = new QueryOrderby("code", "asc");
        schema.addOrderBy(order);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据条件查询汇率类型
     *
     * @param exchangeRateType 汇率类型id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryExchangeRateTypeById(Object exchangeRateType) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(exchangeRateType));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据条件查询款项类型档案
     *
     * @param condition
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryQuickTypeByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.paymenttype.PaymentTypeVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_FINBD);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
//        QueryOrderby order = new QueryOrderby("code","asc");
//        schema.addOrderBy(order);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据id或者code款项类型档案
     *
     * @param quickType
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getQuickTypeByIdOrCode(Object quickType) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.paymenttype.PaymentTypeVO");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_FINBD);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(quickType));
            conditionGroup.appendCondition(QueryCondition.name("stopstatus").eq(false));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(quickType));
                conditionGroup.appendCondition(QueryCondition.name("stopstatus").eq(1));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("getQuickTypeByIdOrCode", e);
        }
        return query;
    }


    /**
     * 根据id或者code利润中心
     *
     * @param profitcenter
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getProfitcenterByIdOrCode(Object profitcenter) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.virtualaccbody.VirtualAccbody");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_FINBD);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(profitcenter));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(profitcenter));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("getProfitcenterByIdOrCode", e);
        }
        return query;
    }

    /**
     * 根据部门编码查询部门档案
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryDeptById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.DeptMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据部门id查询部门档案
     *
     * @param codeList
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryDeptByCodeList(List<String> codeList) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.DeptMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").in(codeList));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 根据部门id查询部门档案
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static BaseDeptDTO rpcQueryDeptById(String id) throws Exception {
        IBizDeptQueryService deptQuerySupport = AppContext.getBean(IBizDeptQueryService.class);
        BaseDeptDTO deptDTO = deptQuerySupport.getById(InvocationInfoProxy.getTenantid(), id);
        if (ObjectUtils.isEmpty(deptDTO) || !deptDTO.isDept()) {
            return null;
        }
        return deptQuerySupport.getById(InvocationInfoProxy.getTenantid(), id);
    }


    /**
     * 根据项目id查询项目档案
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryProjectById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.project.ProjectVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }


    /**
     * 根据项目id或code查询项目档案
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryProjectByIdOrCode(Object id) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.project.ProjectVO");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(id));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(id));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("queryProjectByIdOrCode", e);
        }
        return query;
    }

    /**
     * 根据费用项目id或code查询费用项目档案
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryExpenseItemByIdOrCode(Object id) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.expenseitem.ExpenseItem");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_FINBD);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(id));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(id));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("queryExpenseItemByIdOrCode", e);
        }
        return query;
    }

    /**
     * 根据成本中心id或code查询成本中心
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<CostCenter> getCostCenterByIdOrCode(Object id) throws Exception {
        List<CostCenter> costCenters = null;
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("code").eq(id));
            costCenters = MetaDaoHelper.queryObject(CostCenter.ENTITY_NAME, querySchema, ISchemaConstant.MDD_SCHEMA_FINBD);
            if (CollectionUtils.isEmpty(costCenters)) {
                QuerySchema querySchemaId = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("id").eq(id));
                costCenters = MetaDaoHelper.queryObject(CostCenter.ENTITY_NAME, querySchemaId, ISchemaConstant.MDD_SCHEMA_FINBD);

            }
        } catch (Exception e) {
            log.error("getCostCenterByIdOrCode", e);
        }
        return costCenters;
    }

    /**
     * 根据费用项目id查询费用项目档案
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryExpenseItemById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.expenseitem.ExpenseItem");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_FINBD);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }


    /**
     * 根据条件查询核算委托关系
     *
     * @param condition
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryConsignmentByCondition(Map<String, Object> condition) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.delegate.AccountingDelegateVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create().addSelect("finOrg,adminOrg,id");//资金组织,基础组织
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            conditionGroup.appendCondition(QueryCondition.name(entry.getKey()).eq(entry.getValue()));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /*
     *@Author tongyd
     *@Description 根据ID查询银行网点
     *@Date 2019/6/17 13:42
     *@Param [bankTypeId]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryBankDotById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        billContext.setFullname("bd.bank.BankDotVO");
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        querySchema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, querySchema).get(0);
    }

    public static List<Map<String, Object>> queryBankDotByIdOrCode(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        billContext.setFullname("bd.bank.BankDotVO");
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.appendCondition(QueryCondition.name("code").eq(id));
        querySchema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, querySchema);
        if (CollectionUtils.isEmpty(query)) {
            QuerySchema schemaId = QuerySchema.create();
            schemaId.addSelect("*");
            QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
            conditionGroupId.appendCondition(QueryCondition.name("id").eq(id));
            schemaId.addCondition(conditionGroupId);
            query = MetaDaoHelper.query(billContext, schemaId);
        }
        return query;
    }

    /*
     *@Author majfd
     *@Description 银行账户和币种是否匹配校验
     *@Date 2021/9/18 16:28
     *@Param [id]
     *@Return void
     **/
    public static boolean isExistBankAcctCurrencyByBankacc(Object bankacct, Object currency) throws Exception {
        if (null == bankacct || null == currency) {
            return false;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.BankAcctCurrencyVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankacct").eq(bankacct), QueryCondition.name("currency").eq(currency));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(billContext, schema);
        if (list == null || list.size() == 0) {
            return false;
        }
        return true;
    }

    /*
     *@Author majfd
     *@Description 根据银行账户查询企业银行账户币种
     *@Date 2021/9/18 16:28
     *@Param [id]
     *@Return void
     **/
    public static Map<String, Object> queryBankAcctCurrencyByBankaccCurrency(Object bankacct, Object currency) throws Exception {
        if (null == bankacct || null == currency) {
            return null;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.BankAcctCurrencyVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,isdefault,currency,bankacct,enable");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankacct").eq(bankacct), QueryCondition.name("currency").eq(currency));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(billContext, schema);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    /*
     *@Author majfd
     *@Description 根据银行账户查询企业银行账户默认币种
     *@Date 2021/9/18 16:28
     *@Param [id]
     *@Return void
     **/
    public static Map<String, Object> queryBankAcctDefaultCurrByBankacc(Object bankacct) throws Exception {
        if (null == bankacct) {
            return null;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.BankAcctCurrencyVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);/* 暂不修改 已登记 且为静态方法*/
        QuerySchema schema = QuerySchema.create().addSelect("currency as id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankacct").eq(bankacct));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        conditionGroup.appendCondition(QueryCondition.name("isdefault").eq(1));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(billContext, schema);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 返回当前银行账户下币种的全部信息
     *
     * @param bankacct
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryBankAcctCurrByBankacc(Object bankacct) throws Exception {
        if (null == bankacct) {
            return null;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.BankAcctCurrencyVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);/* 暂不修改 已登记 且为静态方法*/
        QuerySchema schema = QuerySchema.create().addSelect("currency as id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankacct").eq(bankacct));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(billContext, schema);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list;
    }

    /*
     *@Author majfd
     *@Description 根据会计主体查询业务单元期初设置
     *@Date 2021/11/8 16:28
     *@Param orgid 会计主体id
     *@Param modules 模块编码
     *@Return List
     **/
    public static List<Map<String, Object>> queryOrgBpOrgConfVO(Object orgid, String modules) throws Exception {
        if (null == orgid) {
            return null;
        }
        if (StringUtil.isEmpty(modules)) {
            modules = ISystemCodeConstant.ORG_MODULE_CM;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.orgBpConf.OrgBpOrgConfVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create().addSelect("periodid,periodid.begindate as begindate,periodid.enddate as enddate,type_code,orgid,enable");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("type_code").eq(modules), QueryCondition.name("enable").eq(1), QueryCondition.name("dr").eq(0));
        if (orgid instanceof List) {
            conditionGroup.appendCondition(QueryCondition.name("orgid").in(orgid));
        } else {
            conditionGroup.appendCondition(QueryCondition.name("orgid").eq(orgid));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * 查询会计主体对应模块的会计期间启用日期
     *
     * @param orgid   会计主体id
     * @param modules 领域模块编码
     * @return Date 启用日期
     * @throws Exception
     */
    public static Date queryOrgPeriodBeginDate(String orgid, String modules) throws Exception {
        if (StringUtil.isEmpty(orgid) || StringUtil.isEmpty(modules)) {
            return null;
        }
        if (ISystemCodeConstant.ORG_MODULE_CM.equals(modules) || ISystemCodeConstant.ORG_MODULE_DRFT.equals(modules)) {
            List<Map<String, Object>> maps = queryOrgBpOrgConfVO(orgid, modules);
            if (CollectionUtils.isNotEmpty(maps)) {
                return (Date) maps.get(0).get("begindate");
            }
        } else {
            return queryAccBookPeriodBeginDate(orgid, modules);
        }
        return null;
    }

    /**
     * 查询资金组织对应现金管理模块会计期间的启用日期
     *
     * @param orgid 资金组织id
     * @return Date 启用日期
     * @throws Exception
     */
    public static Date queryOrgPeriodBeginDate(String orgid) throws Exception {
        if (null == orgid) {
            return null;
        }
        //只有资金组织能做期初设置，会计主体不行
        List<Map<String, Object>> maps = queryOrgBpOrgConfVO(orgid, ISystemCodeConstant.ORG_MODULE_CM);
        if (CollectionUtils.isNotEmpty(maps)) {
            return (Date) maps.get(0).get("begindate");
        } else {
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{orgid}));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102280"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
        }
    }

    /**
     * 根据会计主体id、时间 查询对应的会计期间vo
     *
     * @param accentity 会计主体id
     * @param date      日期
     * @return Map<String, Object> 会计期间vo
     * @throws Exception
     */
    public static Map<String, Object> queryPeriodByAccbodyAndDate(String accentity, Date date) throws Exception {
        if (null == accentity || null == date) {
            return null;
        }
        FinOrgDTO finOrgDTOByAccentityId = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
        Map<String, Object> periodvo = RemoteDubbo.get(IPeriodOptimizeService.class, IDomainConstant.MDD_DOMAIN_FINBD).findPeriodByAccbodyAndDate(finOrgDTOByAccentityId.getId(), date);
        if (periodvo != null) {
            return periodvo;
        } else
            return null;
    }

    /**
     * 根据会计主体id、时间 查询对应的会计期间id
     *
     * @param accentity
     * @param date
     * @return
     * @throws Exception
     */
    public static Long queryPeriodIdByAccbodyAndDate(String accentity, Date date) throws Exception {
        Map<String, Object> periodvo = queryPeriodByAccbodyAndDate(accentity, date);
        if (periodvo != null && periodvo.get("id") != null) {
            return Long.valueOf(periodvo.get("id").toString());
        } else
            return null;
    }

    /**
     * 查询会计主体对应当前现金管理模块会计期间的结束日期
     *
     * @param orgid 会计主体id
     * @return Date 启用日期
     * @throws Exception
     */
    public static Date queryOrgPeriodEndDate(String orgid) throws Exception {
        if (null == orgid) {
            return null;
        }
        List<Map<String, Object>> maps = queryOrgBpOrgConfVO(orgid, ISystemCodeConstant.ORG_MODULE_CM);
        if (CollectionUtils.isNotEmpty(maps)) {
            return (Date) maps.get(0).get("enddate");
        }
        return null;
    }

//    /**
//     * @return
//     * @Author majfd
//     * @Description 根据ID查询会计主体
//     * @Date 2021/11/09
//     * @Param id
//     **/
//    @Deprecated
//    public static Map<String, Object> queryExchangeRateTypeByOrgId(Object id) throws Exception {
//        BillContext billContext = new BillContext();
//        billContext.setFullname("aa.baseorg.FinanceOrgMV");
//        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
//        QuerySchema schema = QuerySchema.create();
//        schema.addSelect("id,name,code,exchangerate");
//        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
//        schema.addCondition(conditionGroup);
//        Map<String, Object> stringObjectMap = MetaDaoHelper.query(billContext, schema).get(0);
//        List<Map<String, Object>> exchangerate = queryExchangeRateTypeById(stringObjectMap.get("exchangerate"));
//        if (CollectionUtils.isNotEmpty(exchangerate)) {
//            return exchangerate.get(0);
//        }
//        return null;
//    }

    /**
     * 查询会计主体对应业务账簿的对应模块的会计期间启用日期（用于财务领域）
     *
     * @param orgid   会计主体id
     * @param modules 模块
     * @return
     * @throws Exception
     */
    private static Date queryAccBookPeriodBeginDate(String orgid, String modules) throws Exception {
        if (StringUtil.isEmpty(orgid) || StringUtil.isEmpty(modules)) {
            return null;
        }
        EnableModelParam.SonModel module = null;
        switch (modules) {
            case ISystemCodeConstant.ORG_MODULE_GL:
                module = EnableModelParam.SonModel.GL;
                break;
            case ISystemCodeConstant.ORG_MODULE_AP:
                module = EnableModelParam.SonModel.AP;
                break;
            case ISystemCodeConstant.ORG_MODULE_AR:
                module = EnableModelParam.SonModel.AR;
                break;
            default:
                break;
        }
        boolean enableFP = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EPUB_FP_APP_CODE);
        if (!enableFP) {
            log.error("客户环境未安装财务公共服务");
            return null;
        }
        AccountBookPeriod accbook = FIEPUBApiUtil.getAccountBookService().getMainAccBookPeriodByAccEntityPurposeProperty(orgid, "1", module);
        if (accbook != null) {
            return (Date) accbook.getPeriod().get("begindate");
        } else
            return null;
    }

    /**
     * 根据会计主体 获取当前会计主体下的最后一个期间
     *
     * @param orgid
     * @return
     */
    public static Map<String, Object> getLastPeriodByAccentity(String orgid) throws Exception {
        if (null == orgid) {
            return null;
        }
        //获得会计主体，只有会计主体有期间方案，资金组织没有
        Map<String, Object> accBody = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(orgid).get(0);/* 暂不修改 静态方法*/
        String accperiodschemeId = accBody.get("periodschema").toString();
        Map<String, Object> periodvo = RemoteDubbo.get(IPeriodOptimizeService.class, IDomainConstant.MDD_DOMAIN_FINBD).getLastPeriodByScheme(accperiodschemeId);
        if (periodvo != null) {
            return periodvo;
        } else
            return null;
    }

    /**
     * 根据客户id和会计主体查询客户
     *
     * @param merchantId 客户档案id
     * @param accentity  会计主体id
     * @return
     * @throws Exception
     */
    public static MerchantDTO getMerchantByIdAndOrg(Long merchantId, String accentity) throws Exception {
        IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
        String[] field = new String[]{MerchantFieldKeyConstant.DETAIL_STOP_STATUS, MerchantFieldKeyConstant.ID, MerchantFieldKeyConstant.NAME, MerchantFieldKeyConstant.ORGID};
        return merchantService.getMerchantByIdAndOrg(merchantId, Long.valueOf(accentity), field);
    }

    /**
     * 根据id查询付款申请组织
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getOrgMVById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.OrgMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> data = MetaDaoHelper.query(billContext, schema);
        return data;
    }

    /**
     * 根据id或者code查询资金组织
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getOrgMVByIdOrCode(Object accentity) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("aa.baseorg.OrgMV");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(accentity));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(accentity));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("getOrgMVByIdOrCode", e);
        }
        return query;
    }


    /**
     * 根据id或者code查询交易类型编码
     *
     * @param code
     * @return
     * @throws Exception
     */
    public static List<BizObject> getTradeCodeByIdOrCode(Object code) throws Exception {
        String locale = InvocationInfoProxy.getLocale();
        //多语类型
        int multilangtype;
        switch (locale) {
            case "zh_CN":
                multilangtype = 1;
                break;
            case "en_US":
                multilangtype = 2;
                break;
            case "zh_TW":
                multilangtype = 3;
                break;
            default:
                multilangtype = 1;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("trade_code").eq(code));
        queryConditionGroup.appendCondition(QueryCondition.name("exchangesettlement_typeFlag").eq(0));
        queryConditionGroup.appendCondition(QueryCondition.name("multilangtype").eq(multilangtype));
        querySchema.addCondition(queryConditionGroup);
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(ExchangeSettlementTradeCode.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bizObjects)) {
            QuerySchema querySchemaId = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroupId = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroupId.appendCondition(QueryCondition.name("id").eq(code));
            queryConditionGroupId.appendCondition(QueryCondition.name("exchangesettlement_typeFlag").eq(0));
            queryConditionGroupId.appendCondition(QueryCondition.name("multilangtype").eq(multilangtype));
            querySchemaId.addCondition(queryConditionGroupId);
            bizObjects = MetaDaoHelper.queryObject(ExchangeSettlementTradeCode.ENTITY_NAME, querySchemaId, null);
        }
        return bizObjects;
    }

    /**
     * 根据结汇售汇或者code查询交易类型编码
     * 卖出=结汇=1=SFE            买入=售汇=0=BFE
     *
     * @param code
     * @return
     * @throws Exception
     */
    public static List<BizObject> getTradeCodeByCodeForCurrency(Object code, short typeFlag) throws Exception {
        String locale = InvocationInfoProxy.getLocale();
        //多语类型
        int multilangtype;
        switch (locale) {
            case "zh_CN":
                multilangtype = 1;
                break;
            case "en_US":
                multilangtype = 2;
                break;
            case "zh_TW":
                multilangtype = 3;
                break;
            default:
                multilangtype = 1;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("trade_code").eq(code));
        queryConditionGroup.appendCondition(QueryCondition.name("exchangesettlement_typeFlag").eq(typeFlag));
        queryConditionGroup.appendCondition(QueryCondition.name("multilangtype").eq(multilangtype));
        querySchema.addCondition(queryConditionGroup);
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(ExchangeSettlementTradeCode.ENTITY_NAME, querySchema, null);
        return bizObjects;
    }

    /**
     * 根据id或者code查询资金业务对象银行账号
     *
     * @param account
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getCapBizObjbankaccountByIdOrCode(Object account) throws Exception {
        List<Map<String, Object>> query = null;
        try {
            BillContext billContext = new BillContext();
            billContext.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchivesItem");
            billContext.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(account));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query(billContext, schema);
            if (CollectionUtils.isEmpty(query)) {
                QuerySchema schemaId = QuerySchema.create();
                schemaId.addSelect("*");
                QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
                conditionGroupId.appendCondition(QueryCondition.name("id").eq(account));
                schemaId.addCondition(conditionGroupId);
                query = MetaDaoHelper.query(billContext, schemaId);
            }
        } catch (Exception e) {
            log.error("getCapBizObjbankaccountByIdOrCode", e);
        }
        return query;
    }


    /**
     * 根据id或者code查询资金业务对象银行账号
     *
     * @param receivenameid
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getVenderByIdOrCode(Object receivenameid) throws Exception {
        List<Map<String, Object>> query = null;
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.vendor.Vendor");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_YSSUPPLIER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").eq(receivenameid));
        schema.addCondition(conditionGroup);
        query = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(query)) {
            QuerySchema schemaId = QuerySchema.create();
            schemaId.addSelect("*");
            QueryConditionGroup conditionGroupId = new QueryConditionGroup(ConditionOperator.and);
            conditionGroupId.appendCondition(QueryCondition.name("id").eq(receivenameid));
            schemaId.addCondition(conditionGroupId);
            query = MetaDaoHelper.query(billContext, schemaId);
        }
        return query;
    }

    /**
     * 根据id或者code查询资金业务对象
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getCapBizObj(Object id) throws Exception {
        // 查询资金业务对象
        BillContext fundobjBillContext = new BillContext();
        fundobjBillContext.setFullname(ICsplConstant.FUNDBUSINOBJARCHIVES_FULL_NAME);
        fundobjBillContext.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
        QueryConditionGroup fundObjCondition = QueryConditionGroup.and(QueryCondition.name("enabled").eq("1"));
        fundObjCondition.addCondition(QueryCondition.name("code").eq(id));
        List<Map<String, Object>> businessDataList = MetaDaoHelper.queryAll(fundobjBillContext, "*", fundObjCondition, null);
        if (CollectionUtils.isEmpty(businessDataList)) {
            QueryConditionGroup fundObjCondition1 = QueryConditionGroup.and(QueryCondition.name("enabled").eq("1"));
            fundObjCondition1.addCondition(QueryCondition.name("id").eq(id));
            List<Map<String, Object>> businessDataList1 = MetaDaoHelper.queryAll(fundobjBillContext, "*", fundObjCondition1, null);
            return businessDataList1;
        }
        return businessDataList;
    }

    /**
     * 根据id或者code查询客户档案
     *
     * @param customer
     * @return
     * @throws Exception
     */
    public static Map<String, Object> getCustomer(String customer) throws Exception {
        // 查询客户档案
        BillContext custBillContext = new BillContext();
        custBillContext.setFullname("aa.merchant.Merchant");
        custBillContext.setDomain(IDomainConstant.MDD_DOMAIN_PRODUCTCENTER);
        QueryConditionGroup custCondition = new QueryConditionGroup();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        custCondition.addCondition(QueryCondition.name("code").eq(customer));
        custCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("merchantAppliedDetail.stopstatus").eq("0")));
        schema.addCondition(custCondition);
        List<Map<String, Object>> custDataList = MetaDaoHelper.query(custBillContext, schema);
        if (CollectionUtils.isEmpty(custDataList)) {
            QuerySchema schema1 = QuerySchema.create().addSelect("*");
            QueryConditionGroup custCondition1 = new QueryConditionGroup();
            custCondition1.addCondition(QueryCondition.name("id").eq(customer));
            custCondition1.addCondition(QueryConditionGroup.and(QueryCondition.name("merchantAppliedDetail.stopstatus").eq("0")));
            schema1.addCondition(custCondition1);
            List<Map<String, Object>> custDataList1 = MetaDaoHelper.query(custBillContext, schema1);
            if (CollectionUtils.isNotEmpty(custDataList1)) {
                return custDataList1.get(0);
            }
        } else {
            return custDataList.get(0);
        }
        return null;
    }

    /**
     * 根据id查询业务单元
     *
     * @param ids
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getOrgMVByIds(List ids) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.OrgMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> data = MetaDaoHelper.query(billContext, schema);
        return data;
    }


    /**
     * 根据id查询付款申请组织
     *
     * @param noteno
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> getBillNoByNoteNo(Object noteno) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("drft.billno.Billno");
        billContext.setDomain(IDomainConstant.MDD_WORKBENCH_DRFT);
        QuerySchema schema = QuerySchema.create().addSelect("noteno, notemoney,billdirection");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(noteno));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> data = MetaDaoHelper.query(billContext, schema);
        return data;
    }

    /**
     * 根据会计主体判断
     *
     * @accentity 会计主体
     */
    public static boolean getPeriodByaccentity(String accentity) throws Exception {
        if (null == accentity) {
            return false;
        }
        IBeginningPeriodService periodOptimizeService = RemoteDubbo.get(IBeginningPeriodService.class, IDomainConstant.MDD_DOMAIN_ORGCENTER);
        SimpleBeginningPeriod beginningPeriodInfo = periodOptimizeService.getBeginningPeriodInfo(InvocationInfoProxy.getTenantid(), accentity, ISystemCodeConstant.ORG_MODULE_CM);//获取期间id
        //通过期间id获取期初日期
        if (beginningPeriodInfo != null && StringUtils.isNotEmpty(beginningPeriodInfo.getPeriodId())) {
//            IPeriodOptimizeService periodService = RemoteDubbo.get(IPeriodOptimizeService.class, "finbd");
//            Period period = periodService.getPeriodById(Long.valueOf(beginningPeriodInfo.getPeriodId()));
//            Date periodIdBeginDate = period.getBegindate();
//            begindate = periodIdBeginDate;
            return true;
        }
        return false;
    }

    /**
     * 根据会计主体判断
     *
     * @accentity 会计主体
     */
    public static boolean getPeriodByService() throws Exception {
        String reFlag = AppContext.cache().get("yonbip-fi-ctmcmp-cmp_initdatayh_" + InvocationInfoProxy.getTenantid());
        if (StringUtils.isBlank(reFlag)) {
            // 从Spring容器中获取Bean
            IServiceManagerService serviceManagerService = AppContext.getBean(IServiceManagerService.class);
            ServiceVO serviceVO = serviceManagerService.findByTenantIdAndServiceCode(InvocationInfoProxy.getTenantid(), IServicecodeConstant.BANKINITDATA);
            if (serviceVO == null) {
                AppContext.cache().set("yonbip-fi-ctmcmp-cmp_initdatayh_" + InvocationInfoProxy.getTenantid(), "0", 1 * 60 * 60);//一小时
                return false;
            } else {
                AppContext.cache().set("yonbip-fi-ctmcmp-cmp_initdatayh_" + InvocationInfoProxy.getTenantid(), "1", 10 * 60 * 60);//十小时
                return true;
            }
        } else {
            if ("0".equals(reFlag)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 通过账簿id查询业务账簿
     *
     * @param param
     */
    public static List<AccountBook> getAccentityByAccount(Map<String, Object> param) {
        boolean enableFP = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EPUB_FP_APP_CODE);
        if (!enableFP) {
            log.error("客户环境未安装财务公共服务");
            return null;
        }
        return FIEPUBApiUtil.getAccountBookService().getAccountBooks(param);
    }


    /**
     * 查询最小周期策略 id
     *
     * @param accentity
     * @param currency
     * @return
     * @throws Exception
     */
    public static Map<String, Object> queryStrategySetb(String accentity, String currency) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("cspl.strategyset.StrategySet");
        billContext.setDomain(MDD_DOMAIN_CTMCSPL);
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        conditionGroup.appendCondition(QueryCondition.name("strategyStatus").eq(0));
        schema.addOrderBy("capitalPlanType desc");
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (query != null && query.size() > 0) {
            return query.get(0);
        }
        return null;
    }


    /**
     * <h2>根据会计主体,币种,部门,业务日期查询最小周期编制单子表ID接口</h2>
     *
     * @param accentity :
     * @param currency  :
     * @param date      :
     * @param dept      :
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Sun GuoCai
     * @since 2023/7/18 9:55
     */
    public static List<Long> queryPlanStrategyIsEnable(String accentity, String currency, Date date, String dept) throws Exception {
        List<Long> capitalIds;
        IOpenApiService openApiService = AppContext.getBean(IOpenApiService.class);
        //1.构建请求参数
        MinPeriodCapitalPlanDrawQueryVo requestData = new MinPeriodCapitalPlanDrawQueryVo();
        requestData.setAccentity(accentity);
        requestData.setCurrency(currency);
        requestData.setBusinessDate(date);
        if (ValueUtils.isNotEmptyObj(dept)) {
            requestData.setDept(dept);
        }
        ResponseResult result = openApiService.queryMinPeriodCapitalPlanDrawIds(requestData);
        if(!ValueUtils.isNotEmptyObj(result)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102281"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180023","查询资金计划异常，请检查重试！") /* "查询资金计划异常，请检查重试！" */);
        }
        if(result.isSuccess()){
            capitalIds = (List<Long>)result.getData();
        }else{
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102282"),result.getMessage());
        }
        if (CollectionUtils.isNotEmpty(capitalIds)) {
            return capitalIds;
        }
        return null;
    }

    public static Map<Object, Object> queryPlanStrategyControlMethodBySubId(List<Object> ids) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("cspl.capitalplandraw.CapitalPlanDraw_b");
        billContext.setDomain(MDD_DOMAIN_CTMCSPL);
        QuerySchema schema = QuerySchema.create().addSelect("id", "setControlMethod");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        Map<Object, Object> data = new HashMap<>();
        for (Map<String, Object> map : query) {
            data.put(map.get("id"), map.get("setControlMethod"));
        }
        return data;
    }

    /**
     * 根据id查询 资金业务对象
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static Map<String, Object> queryCapBizObjId(String id) throws Exception {
        BillContext context = new BillContext();
        context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
        context.setDomain("yonbip-fi-ctmtmsp");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id, accentity, fundbusinobjtypename");
        schema.appendQueryCondition(QueryCondition.name("id").eq(id));
        List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }


    /**
     * @Author renqshm
     * @Description 批量查询会计主体
     **/
    public static List<Map<String, Object>> queryAccEntityByIds(List ids) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.FinanceOrgMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * @Author liang.ren
     * @Description 批量查询会计主体
     **/
    public static List<Map<String, Object>> queryAccEntityByCodes(List codes) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.baseorg.FinanceOrgMV");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").in(codes));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * @Author liang.ren
     * @Description 批量查询币种id
     **/
    public static List<Map<String, Object>> queryCurrencyByCodes(List codes) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").in(codes));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    public static List<String> getTenantList() {
        List<String> ytenantIdList = new ArrayList<>();
        try {
            YmsJdbcApiProvider ymsJdbcApiProvider = YmsJdbcApiProviderFactory.getYmsJdbcApiProvider();
            Supplier<IYmsJdbcApi<?>> ymsJdbcApi = ymsJdbcApiProvider.getYmsJdbcApi(DealDetailEnumConst.LOGICDATASOURCE);
            List<Map<String, Object>> list = ymsJdbcApi.get().queryForList(QUERY_ALL_TENANT, new MapListProcessor());
            for (Map<String, Object> map : list) {
                ytenantIdList.add(map.get("ytenant_id").toString());
            }
        } catch (Exception e) {
            log.error("联邦查询当前核心环境，数据库租户信息异常", e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005FB", "联邦查询当前核心环境，数据库租户信息异常") /* "联邦查询当前核心环境，数据库租户信息异常" */);
        }
        return ytenantIdList;
    }

    /**
     * @param bankAccountCodeOrIdNotNull 账号code 或者id
     * @param accountOrId                true 账号,false id
     * @return 账号id
     * @throws Exception
     * @author liang.ren
     * 查询银行账号id
     */
    public static List<Map<String, Object>> queryBankAccountByAccountsOrIds(List<String> bankAccountCodeOrIdNotNull, boolean accountOrId) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,account,bank");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(accountOrId ? "account" : "id").in(bankAccountCodeOrIdNotNull));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }


    public static List<Map<String, Object>> getQuickTypeByCode(List<Object> codes) throws Exception {
        List<Map<String, Object>> quickTypeData = Lists.newArrayList();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(new ConditionExpression[]{QueryCondition.name("tenant").eq(AppContext.getTenantId())});
        if (Objects.nonNull(codes) && codes.size() > 0) {
            queryConditionGroup.addCondition(QueryCondition.name("code").in(codes));
        }

        querySchema.addCondition(queryConditionGroup);
        quickTypeData = MetaDaoHelper.query("bd.paymenttype.PaymentTypeVO", querySchema, "finbd");
        removeStopQuickType(quickTypeData);
        return quickTypeData;
    }

    private static void removeStopQuickType(List<Map<String, Object>> qkList) {
        if (!CollectionUtils.isEmpty(qkList)) {
            qkList.removeIf((qk) -> {
                return Boolean.TRUE.equals(MapUtils.getBoolean(qk, "stopstatus"));
            });
        }
    }

    public static String getAccentityIdByCode(String accentityCode) {
        String accentityId = null;
        ///** 资金组织编码转ID */
        List<String> accEntityCodes = new ArrayList<>();
        accEntityCodes.add(accentityCode);
        // 资金组织RPC接口替换
        List<FundsOrgDTO> fundsOrgDTOS = fundsOrgQueryServiceComponent.listByCodes(accEntityCodes);

        if (fundsOrgDTOS == null || fundsOrgDTOS.size() == 0) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F9", "资金组织编码不存在！") /* "资金组织编码不存在！" */);
        }
        if (fundsOrgDTOS.size() > 1) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005FA", "依据组织编码匹配到多个组织档案，保存失败，请检查！") /* "依据组织编码匹配到多个组织档案，保存失败，请检查！" */);
        }
        for (FundsOrgDTO fundsOrgDTO : fundsOrgDTOS) {
            if (accentityCode.equals(fundsOrgDTO.getCode())) {
                //使用组织
                accentityId = fundsOrgDTO.getId();
                break;
            }
        }
        if (accentityId == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F9", "资金组织编码不存在！") /* "资金组织编码不存在！" */);
        }
        return fundsOrgDTOS.get(0).getId();
    }

    /**
     * 结算中心组织查询接口，根据组织id查询
     * @param orgSet
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> querySettlementCenterByIds(Set<String> orgSet) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("org.func.SettlementOrg");
        billContext.setDomain("ucf-org-center");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(orgSet));
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }

    public static boolean isContainSettlementCenter(Set<String> orgSet){
        try {
            return CollectionUtils.isNotEmpty(QueryBaseDocUtils.querySettlementCenterByIds(orgSet));
        }catch (Exception e){
            return false;
        }
    }

}
