package com.yonyoucloud.fi.cmp.bankreconciliation.rule.autopushbill;

import com.yonyou.cloud.middleware.rpc.RPCStubBeanFactory;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.uretail.sys.auth.DataPermissionRequestDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResponseDto;
import com.yonyoucloud.uretail.sys.auth.DataPermissionResultDto;
import com.yonyoucloud.uretail.sys.pubItf.IDataPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description: 到账认领V2，银行对账单对方单位参照过滤规则;筛选参照银行账户包含对方银行账号
 * @author: wanxbo@yonyou.com
 * @date: 2022/8/4 16:12
 */
@Slf4j
@Component
public class OppositeTypeReferFilterRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        //对方银行账号
        String toaccount = null;
        String currency = null;
        String accentity = null;
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            currency = bills.get(0).getString("currency");
            accentity = bills.get(0).getString("accentity");
        }
        //对方单位再过滤
        if ("oppositeobjectname".equals(billDataDto.getDatasource()) || "oppositeobjectname".equals(billDataDto.getKey())) {
            // 获取数据权限
            DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
            if (IBillNumConstant.BANKRECONCILIATION.equals(billContext.getBillnum())
                    || IBillNumConstant.CMP_BILLCLAIM_CARD.equals(billContext.getBillnum())
                    || IBillNumConstant.BANKRECONCILIATIONLIST.equals(billContext.getBillnum())) {// 银行流水
                requestDto.setEntityUri(BankReconciliation.ENTITY_NAME);
                requestDto.setServiceCode(IServicecodeConstant.CMPBANKRECONCILIATION);
            }
            requestDto.setSysCode(ICmpConstant.CMP_MODUAL_NAME);
            requestDto.setYxyUserId(AppContext.getUserId().toString());
            requestDto.setYhtTenantId(InvocationInfoProxy.getTenantid());
            requestDto.setYxyTenantId(AppContext.getTenantId().toString());
            requestDto.setHaveDetail(true);
            RPCStubBeanFactory rpChainBeanFactory = new RPCStubBeanFactory("iuap-apcom-auth", "c87e2267-1001-4c70-bb2a-ab41f3b81aa3", null, IDataPermissionService.class);
            rpChainBeanFactory.afterPropertiesSet();
            IDataPermissionService remoteBean = (IDataPermissionService) rpChainBeanFactory.getObject();

            String referCode = billDataDto.getrefCode();
            FilterVO condition = billDataDto.getCondition();
            if (condition == null) {
                condition = new FilterVO();
                billDataDto.setCondition(condition);
            }
            //客户参照
            if ("productcenter.aa_merchantref".equals(referCode)) {
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(accentity)) {
                    Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,
                            new SimpleFilterVO("merchantAppliedDetail.merchantApplyRangeId.orgId", ICmpConstant.QUERY_IN, orgSet.toArray()));
                }
                // 增加数据权限
                addDataAuthCondition(requestDto, remoteBean, billDataDto, ICmpConstant.MERCHANT, "merchantAppliedDetail.stopstatus", new Integer[]{0});
            }
            //供应商
            if (IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(referCode)) {
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(accentity)) {
                    Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
                    // CZFW-169794供应商参照报错问题
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,
                            new SimpleFilterVO("crowd.crowdOrgs.org", ICmpConstant.QUERY_IN, orgSet.toArray()));
                }
                // 不需要进行对方ID的过滤
                // 增加数据权限
                addDataAuthCondition(requestDto, remoteBean, billDataDto, ICmpConstant.VENDOR, "vendorextends.stopstatus", 0);
            }
            //员工
            if (IRefCodeConstant.DOMAIN_BD_STAFF_REF.equals(referCode) || IRefCodeConstant.BD_STAFF_LEAVE_REF.equals(referCode)) {
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(accentity)) {
                    Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("mainJobList.org_id", ICmpConstant.QUERY_IN, orgSet.toArray()));
                }
                // 增加数据权限
                addDataAuthCondition(requestDto, remoteBean, billDataDto, ICmpConstant.STAFF, "mainJobList.endflag", new Integer[]{0});
            }
            //内部单位
            // 去掉会计组织的过滤
        }

        return new RuleExecuteResult();
    }

    /**
     * 添加数据权限过滤条件到查询条件中
     *
     * @param requestDto 数据权限请求参数对象，用于获取当前用户的数据权限信息
     * @param remoteBean 数据权限服务接口，用于调用远程数据权限服务
     * @param billDataDto 业务数据传输对象，包含查询条件和其他业务数据
     * @param type 数据权限类型字段名
     * @param stopStatus 停用状态字段名，用于过滤停用数据
     * @param stopStatusValue 停用状态值，用于指定哪些停用状态需要被过滤
     * @throws Exception 当调用数据权限服务或设置条件时发生异常
     */
    private static void addDataAuthCondition(DataPermissionRequestDto requestDto, IDataPermissionService remoteBean, BillDataDto billDataDto,String type, String stopStatus, Object stopStatusValue) throws Exception {
        requestDto.setFieldNameArgs(new String[]{type});
        if (remoteBean == null) {
            return;
        }

        DataPermissionResponseDto dataPermission = remoteBean.getDataPermission(requestDto);
        List<DataPermissionResultDto> dataPermissionResultDtos = dataPermission.getResultDtos();
        // 无权限直接返回
        if (CollectionUtils.isEmpty(dataPermissionResultDtos)) {
            return;
        }
        FilterVO condition = billDataDto.getCondition();
        if (condition == null) {
            condition = new FilterVO();
            billDataDto.setCondition(condition);
        }
        // 循环添加权限过滤条件
        for (DataPermissionResultDto resultDto : dataPermissionResultDtos) {
            String[] ids = resultDto.getValues();
            if (ids != null && ids.length > 0) {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, ids));
            }
        }
        // 添加停用状态条件
        if (StringUtils.isNotEmpty(stopStatus)) {
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO(stopStatus, ICmpConstant.QUERY_IN, stopStatusValue));
        }
    }

    /**
     * 根据对方账号获取员工ID
     */
    private List<Object> getInnerEmployeeIds(String toAcctNo, String currency) throws Exception {
        List<Object> ids = new ArrayList<>();
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "id,bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, " +
                        "name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        if (!StringUtils.isEmpty(toAcctNo)){
            querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.account").eq(toAcctNo));
        }
//        if (!StringUtils.isEmpty(currency)){
//            querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.currency").eq(currency));
//        }

        //过滤可用信息
        querySchema_period.appendQueryCondition(QueryCondition.name("enable").eq(1));
        List<Map<String,Object>> result = MetaDaoHelper.query("bd.staff.Staff",querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);/* 暂不修改 已登记*/
        if (ValueUtils.isNotEmpty(result)){
            for (Map<String,Object> map_staff:result){
                if (map_staff.get("name")!=null){
                    ids.add(map_staff.get("id"));
                }
            }
        }
        return ids;
    }

    /**
     * 根据对方银行账号，获取关联的组织ID
     */
    private List<Object> getBankAcctAccentity(String toAcctNo,String currency) throws Exception {
        List<Object> ids = new ArrayList<>();
        BillContext billContextFinBank = new BillContext();
        billContextFinBank.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);/* 暂不修改 已登记*/
        QueryConditionGroup groupBank = null;
        if (!StringUtils.isEmpty(toAcctNo)&& !StringUtils.isEmpty(currency)){
            groupBank = QueryConditionGroup.and(
                    QueryCondition.name("account").eq(toAcctNo),
                    QueryCondition.name("currencyList.currency").eq(currency)
            );
            QuerySchema querySchemaEnterprise = QuerySchema.create().addSelect("id as bankacct,orgid,orgid.name as orgname,code,name,account,bank,bankNumber,lineNumber,currencyList.currency as currency,enable,acctType").addCondition(groupBank);
            List<Map<String, Object>> dataList = MetaDaoHelper.query(billContextFinBank,querySchemaEnterprise, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
            for(Map<String, Object> map:dataList){
                if(toAcctNo.equals(map.get("account")) && currency.equals(map.get("currency"))){
                    ids.add(map.get("orgid"));
                }
            }
        }else if (!StringUtils.isEmpty(toAcctNo) && StringUtils.isEmpty(currency)){
            groupBank = QueryConditionGroup.and(
                    QueryCondition.name("account").eq(toAcctNo)
            );
            QuerySchema querySchemaEnterprise = QuerySchema.create().addSelect("id as bankacct,orgid,orgid.name as orgname,code,name,account,bank,bankNumber,lineNumber,currencyList.currency as currency,enable,acctType").addCondition(groupBank);
            List<Map<String, Object>> dataList = MetaDaoHelper.query(billContextFinBank,querySchemaEnterprise, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
            for(Map<String, Object> map:dataList){
                if(toAcctNo.equals(map.get("account"))){
                    ids.add(map.get("orgid"));
                }
            }
        }else if (StringUtils.isEmpty(toAcctNo) && !StringUtils.isEmpty(currency)){
            groupBank = QueryConditionGroup.and(
                    QueryCondition.name("currencyList.currency").eq(currency)
            );
            QuerySchema querySchemaEnterprise = QuerySchema.create().addSelect("id as bankacct,orgid,orgid.name as orgname,code,name,account,bank,bankNumber,lineNumber,currencyList.currency as currency,enable,acctType").addCondition(groupBank);
            List<Map<String, Object>> dataList = MetaDaoHelper.query(billContextFinBank,querySchemaEnterprise, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
            for(Map<String, Object> map:dataList){
                if(currency.equals(map.get("currency"))){
                    ids.add(map.get("orgid"));
                }
            }
        }
        return ids;
    }
}
