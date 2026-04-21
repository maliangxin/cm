package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.iuap.bd.base.BdRestSingleton;
import com.yonyou.iuap.bd.pub.param.ConditionVO;
import com.yonyou.iuap.bd.pub.param.Operator;
import com.yonyou.iuap.bd.pub.param.Page;
import com.yonyou.iuap.bd.pub.util.Condition;
import com.yonyou.iuap.bd.pub.util.Sorter;
import com.yonyou.iuap.bd.staff.dto.AdminOrg;
import com.yonyou.iuap.bd.staff.dto.Staff;
import com.yonyou.iuap.bd.staff.dto.StaffPart;
import com.yonyou.iuap.bd.staff.service.itf.IStaffPartService;
import com.yonyou.iuap.bd.staff.service.itf.IStaffService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.service.itf.core.IItOrgQueryService;
import com.yonyou.uap.tenant.service.itf.ITenantRoleUserService;
import com.yonyou.uap.tenantauth.entity.TenantRole;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.base.user.UserType;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.model.LoginUser;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.count.BillCountUtil;
import com.yonyoucloud.fi.cmp.cmpentity.DcFlagEnum;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.cmpentity.PublishStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.stct.api.openapi.response.BDAccountAuthBatchRespVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 到账认领中心
 * 当前用户身份为客户管理员、客户业务员时，只查询显示当前用户所属客户的对账单数据
 */
@Slf4j
@Component("billClaimCenterListQueryRule")
public class BillClaimCenterListQueryRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    BillCountUtil billCountUtil;

    @Autowired
    private ITenantRoleUserService tenantRoleUserService;

    private static final String AUTHORUSEACCENTITY = "authoruseaccentity";

    private static final String ACCENTITY = "accentity";

    private static final String BANKACCOUNT = "bankaccount";

    @Autowired
    AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = new FilterVO();
        if (billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }
        LoginUser currentUser = AppContext.getCurrentUser();
        String docId = currentUser.getDocId();
        UserType userType = currentUser.getUserType();
        if ((UserType.TenantShopuser.equals(userType) || UserType.JoinUser.equals(userType)) && !StringUtils.isEmpty(docId)) {
            if (billDataDto.getBillnum().equals(IBillNumConstant.CMP_BILLCLAIMCENTER_LIST) || "cmp_billclaimcenter_commonlist".equals(billDataDto.getBillnum())) {
                SimpleFilterVO oppositeObjectiId = new SimpleFilterVO("oppositeobjectid", "eq", docId);
                // oppositetype 对方类型 客户
                SimpleFilterVO oppositeType = new SimpleFilterVO("oppositetype", "eq", 1);
                filterVO.appendCondition(ConditionOperator.and, oppositeType, oppositeObjectiId);
            } else if (billDataDto.getBillnum().equals(IBillNumConstant.CMP_MYBILLCLAIM_LIST)) {
                SimpleFilterVO oppositeObjectiId = new SimpleFilterVO("items.oppositeobjectid", "eq", docId);
                // oppositetype 对方类型 客户
                SimpleFilterVO oppositeType = new SimpleFilterVO("items.oppositetype", "eq", 1);
                filterVO.appendCondition(ConditionOperator.and, oppositeType, oppositeObjectiId);
            }
        }
        List<String> orgList = new ArrayList<>();
        List<String> bankaccountList = new ArrayList<>();
        if (null != filterVO) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            if (null != commonVOs && commonVOs.length > 0) {
                for (FilterCommonVO vo : commonVOs) {
                    switch (vo.getItemName()) {
                        case AUTHORUSEACCENTITY:
                            // 删除授权使用组织查询条件
                            orgList.addAll((List) vo.getValue1());
                            vo.setValue1("");
                            vo.setItemName("");
                            break;
                        case ACCENTITY:
                            // 删除授权使用组织查询条件
                            if (vo.getValue1() instanceof List) {
                                orgList.addAll((List) vo.getValue1());
                            } else if (vo.getValue1() instanceof String) {
                                orgList.add((String) vo.getValue1());
                            }
                            vo.setValue1("");
                            vo.setItemName("");
                            break;
                        case BANKACCOUNT:
                            // 删除账户查询条件
                            if (vo.getValue1() instanceof List) {
                                bankaccountList.addAll((List) vo.getValue1());
                            } else if (vo.getValue1() instanceof String) {
                                bankaccountList.add((String) vo.getValue1());
                            }
                            vo.setValue1("");
                            vo.setItemName("");
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        //当前节点的授权组织
        Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(AppContext.getThreadContext("serviceCode"));
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 统收统支模式or代理结算模式
        Boolean agentFlag = autoConfigService.getEnableBizDelegationMode();
        Boolean inoutFlag = autoConfigService.getUnifiedIEModelWhenClaim();
        List<String> accentityList = new ArrayList<>();
        //统收统支找到的上级
        Set<String> inoutAccentitys = new HashSet<>();
        //结算中心找到的上级
        Set<String> agentAccentitys = new HashSet<>();
        if (orgList.size() < 1) {
            orgList = new ArrayList<>(orgSet);
            inoutAccentitys = billCountUtil.getInoutAccentityList(inoutFlag, orgList);
            agentAccentitys = billCountUtil.getAgentAccentityList(agentFlag, orgList);
            accentityList.addAll(orgSet);
            accentityList.addAll(inoutAccentitys);
            accentityList.addAll(agentAccentitys);
        } else {
            inoutAccentitys = billCountUtil.getInoutAccentityList(inoutFlag, orgList);
            agentAccentitys = billCountUtil.getAgentAccentityList(agentFlag, orgList);
            accentityList.addAll(orgList);
            accentityList.addAll(inoutAccentitys);
            accentityList.addAll(agentAccentitys);
        }
        if (CollectionUtils.isEmpty(bankaccountList) && CollectionUtils.isNotEmpty(accentityList)) {
            bankaccountList.addAll(enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(accentityList));
        }
        QueryConditionGroup queryConditionGroup;
        if (accentityList != null && accentityList.size() > 0) {
            queryConditionGroup = QueryConditionGroup.or(
                    QueryCondition.name("accentity").in(accentityList),
                    QueryCondition.name("accentity").is_null()
            );
        } else {
            queryConditionGroup = QueryConditionGroup.or(
                    QueryCondition.name("accentity").is_null()
            );
        }

        QueryConditionGroup queryConditionGroup2;
        if (bankaccountList != null && bankaccountList.size() > 0) {
            queryConditionGroup2 = QueryConditionGroup.or(
                    QueryCondition.name("bankaccount").in(bankaccountList),
                    QueryCondition.name("bankaccount").is_null()
            );
        } else {
            queryConditionGroup2 = QueryConditionGroup.or(
                    QueryCondition.name("bankaccount").is_null()
            );
        }
        //授权的银行账户和组织
        QueryConditionGroup queryConditionGroup3 = QueryConditionGroup.and(queryConditionGroup, queryConditionGroup2);

        //根据20240715新需求拆分页签认领池数据查询
        if ("cmp_billclaimcenterlist".equals(billContext.getBillnum()) || "cmp_billclaimcenter_commonlist".equals(billContext.getBillnum())) {
            QueryConditionGroup orGroup = new QueryConditionGroup(ConditionOperator.or);
            orGroup.addCondition(QueryCondition.name("serialdealtype").not_eq("5"));
            orGroup.addCondition(QueryCondition.name("serialdealtype").is_null());
            conditionGroup.addCondition(queryConditionGroup3,orGroup);

            QueryConditionGroup configOrGroup = new QueryConditionGroup(ConditionOperator.or);
            //不授权的组织：要取并集
            Set<String> noAccentitys = new HashSet<>();
            //授权的组织：要取交集
            Set<String> authAccentitys = new HashSet<>();
            //授权的指定账户
            Set<String> specAccounts = new HashSet<>();
            //授权的指定账户的结算中心
            Set<String> specAccentitys = new HashSet<>();
            if (ObjectUtils.isNotEmpty(orgList) && agentFlag) {
                Map<BDAccountAuthBatchRespVO.BDAccountAuthKey, BDAccountAuthBatchRespVO.BDAccountAuthRespVO> authRespVOMap =
                        billCountUtil.getAuthDelegationList(orgList, agentFlag);
                if (authRespVOMap != null && authRespVOMap.values() != null) {
                    // 授权类型：0：不授权，1：授权全部账户，2：授权指定账户
                    for (BDAccountAuthBatchRespVO.BDAccountAuthRespVO item : authRespVOMap.values()) {
                        if (item.getAuthType() != null) {
                            if (orgSet.contains(item.getSettlementOrg()) || inoutAccentitys.contains(item.getSettlementOrg())) {
                                //如果授权的组织包含当前结算中心 那么无论怎么配置都不会影响结果 直接跳出去;如果开了统收统支参数那是并集就不单独筛选委托关系里面的单独配置了
                                continue;
                            }
                            if (item.getAuthType().equals("0")) {
                                //如果授权的组织不包含当前结算中心 那么不授权就是看不到的
                                noAccentitys.add(item.getSettlementOrg());
                            } else if (item.getAuthType().equals("1")) {
                                //如果授权的组织不包含当前结算中心 那么授权就是要看到的
                                authAccentitys.add(item.getSettlementOrg());
                            }
                        }
                    }
                    for (BDAccountAuthBatchRespVO.BDAccountAuthRespVO item : authRespVOMap.values()) {
                        //如果指定账户了 那就找不是授权全部的 找不是授权节点配置的
                        if (item.getAuthType().equals("2") && !orgSet.contains(item.getSettlementOrg()) && !authAccentitys.contains(item.getSettlementOrg())) {
                            if (item.getAuthAccounts() != null && org.apache.commons.collections4.CollectionUtils.isNotEmpty(item.getAuthAccounts())) {
                                //不是授权全部的 那是真的要单独授权了
                                if (!orgSet.contains(item.getSettlementOrg())) {
                                    List<BDAccountAuthBatchRespVO.BDAccountAuthItemRespVO> accountAuthItemRespVOList =
                                            item.getAuthAccounts();
                                    for (BDAccountAuthBatchRespVO.BDAccountAuthItemRespVO account :
                                            accountAuthItemRespVOList) {
                                        specAccentitys.add(item.getSettlementOrg());
                                        specAccounts.add(account.getEnterpriseBankAcctId());
                                        QueryConditionGroup innerGgroup = new QueryConditionGroup(ConditionOperator.and);
                                        innerGgroup.addCondition(QueryCondition.name("accentity").eq(item.getSettlementOrg()));
                                        innerGgroup.addCondition(QueryCondition.name("bankaccount").eq(account.getEnterpriseBankAcctId()));
                                        if (account.getOperateAuth() != null && account.getOperateAuth().size() == 1) {
                                            // 操作权限:0: 收款流水,1: 付款流水
                                            innerGgroup.addCondition(QueryCondition.name("dc_flag").eq(account.getOperateAuth().get(0).equals("0") ? DcFlagEnum.Credit.getValue() : DcFlagEnum.Debit.getValue()));
                                        }
                                        configOrGroup.addCondition(innerGgroup);
                                    }
                                }
                            }
                        }
                    }
                    //取能看到的最大合集
                    if (ObjectUtils.isNotEmpty(noAccentitys)) {
                        Set<String> noAccentitySet = new HashSet<>();
                        for (String noacc : noAccentitys) {
                            //不是授权节点配置的 不是授权全部的 也不是授权指定的 那么一定不授权了 那是真的看不到
                            if (!authAccentitys.contains(noacc) && !specAccentitys.contains(noacc) && !inoutAccentitys.contains(noacc)) {
                                noAccentitySet.add(noacc);
                            }
                        }
                        if (ObjectUtils.isNotEmpty(noAccentitySet)) {
                            conditionGroup.addCondition(QueryCondition.name("accentity").not_in(noAccentitySet));
                        }
                    }
                    if (ObjectUtils.isNotEmpty(specAccentitys)) {
                        Set<String> condition = new HashSet<>();
                        condition.addAll(inoutAccentitys);
                        condition.addAll(orgSet);
                        //还得带上统收统支配的
                        configOrGroup.addCondition(QueryCondition.name("accentity").in(condition));
                        configOrGroup.addCondition(QueryCondition.name("accentity").is_null());
                        conditionGroup.addCondition(configOrGroup);
                    }
                }
            }
            if(!QueryBaseDocUtils.isContainSettlementCenter(orgSet) && CollectionUtils.isNotEmpty(orgSet) && billDataDto.getBillnum().equals(IBillNumConstant.CMP_MYBILLCLAIM_LIST) || billDataDto.getBillnum().equals(IBillNumConstant.CMP_BILLCLAIM_CARD)){
                conditionGroup.addCondition(QueryCondition.name("actualclaimaccentiry").in(orgSet));
            }
            filterVO.setQueryConditionGroup(conditionGroup);
            splitCount(filterVO, orgSet, billDataDto);
        } else {
            if(!QueryBaseDocUtils.isContainSettlementCenter(orgSet) && CollectionUtils.isNotEmpty(orgSet) && (billDataDto.getBillnum().equals(IBillNumConstant.CMP_MYBILLCLAIM_LIST) || billDataDto.getBillnum().equals(IBillNumConstant.CMP_BILLCLAIM_CARD))){
                queryConditionGroup3.addCondition(QueryCondition.name("actualclaimaccentiry").in(orgSet));
            }
            filterVO.setQueryConditionGroup(queryConditionGroup3);
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }

    private void splitCount(FilterVO filterVO, Set<String> orgSet, BillDataDto billDataDto) throws Exception {
        Set<String> myList = new HashSet<>();
        //组装数据
        //根据用户查员工
        IStaffService staffService = BdRestSingleton.getInst(AppContext.getYTenantId(), "diwork",
                AppContext.getCurrentUser().getYhtUserId()).getBdRestService().getStaffService();
        IStaffPartService staffPartService = BdRestSingleton.getInst(AppContext.getYTenantId(), "diwork",
                AppContext.getCurrentUser().getYhtUserId()).getBdRestService().getStaffPartService();

        Condition conditionquery = new Condition();
        List<ConditionVO> conditionVOList = new ArrayList<>(1);
        ConditionVO conditionVO = new ConditionVO("user_id", AppContext.getCurrentUser().getYhtUserId(), Operator.EQUAL);
        conditionVOList.add(conditionVO);
        conditionquery.setConditionList(conditionVOList);
        Page<Staff> pageList = staffService.pagination(conditionquery, new Sorter(), 1, 1);
        String staff = null;
        if (null != pageList && null != pageList.getContent() && pageList.getContent().size() > 0 && null != pageList.getContent().get(0)) {
            staff = pageList.getContent().get(0).getId();
        }
        //部门
        List<String> deptList = new ArrayList<>();
        //根据用户查主职部门
        List<AdminOrg> adminOrgs = staffService.listDeptByUserId(AppContext.getCurrentUser().getYhtUserId());
        if (CollectionUtils.isNotEmpty(adminOrgs)) {
            deptList.addAll(adminOrgs.stream().map(AdminOrg::getId).collect(Collectors.toList()));
        }
        //根据员工查兼职部门
        List<StaffPart> staffPartList = new ArrayList<>();
        if (!Objects.isNull(staff)) {
            staffPartList = staffPartService.listByStaffId(staff);
        }
        if (CollectionUtils.isNotEmpty(staffPartList)) {
            deptList.addAll(staffPartList.stream().map(StaffPart::getDept_id).collect(Collectors.toList()));
        }
        //根据用户查角色
        List<TenantRole> roles = tenantRoleUserService.findRolesByUserId(AppContext.getCurrentUser().getYhtUserId(), AppContext.getYTenantId().toString(), "diwork");
        //我的认领池：
        //1、我的认领池，银行流水的“使用组织为空”不为空，且当前用户有该使用组织的权限，
        //2、数据在发布时指定了使用组织(“发布组织”)的基础上，还至少指定了“发布部门/发布角色/发布用户”其中之一;当前用户对应到了，所发布的部门、角色员工及用户(查找银行流水处理子表的“发布处理记录处理类型-发布，且发布状态-“已生效
        QueryConditionGroup mygroup = new QueryConditionGroup(ConditionOperator.and);
        mygroup.appendCondition(QueryCondition.name("publishstatus").eq(PublishStatus.Effective.getValue()));
        mygroup.appendCondition(QueryCondition.name("oprtype").eq(OprType.Publish.getValue()));
        mygroup.appendCondition(QueryCondition.name("published_org").is_not_null());
        mygroup.addCondition(QueryCondition.name("mainid.published_type").in(new short[]{PublishedType.DEPT.getCode(),
                PublishedType.ROLE.getCode(), PublishedType.EMPLOYEE.getCode(), PublishedType.USER.getCode(), PublishedType.ASSIGN_ORG.getCode()}));
        QueryConditionGroup myOrGroup = new QueryConditionGroup(ConditionOperator.or);
        QueryConditionGroup andGroup = new QueryConditionGroup(ConditionOperator.and);
        andGroup.addCondition(QueryCondition.name("mainid.published_type").eq(PublishedType.USER.getCode()));
        andGroup.addCondition(QueryCondition.name("published_user").eq(InvocationInfoProxy.getUserid()));
        myOrGroup.addCondition(andGroup);
        if (!Objects.isNull(staff)) {
            QueryConditionGroup myAndGroup = new QueryConditionGroup(ConditionOperator.and);
            myAndGroup.addCondition(QueryCondition.name("mainid.published_type").eq(PublishedType.EMPLOYEE.getCode()));
            myAndGroup.addCondition(QueryCondition.name("employee_financial").eq(staff));
            myOrGroup.addCondition(myAndGroup);
        }
        if (CollectionUtils.isNotEmpty(deptList)) {
            QueryConditionGroup myAndGroup = new QueryConditionGroup(ConditionOperator.and);
            myAndGroup.addCondition(QueryCondition.name("mainid.published_type").eq(PublishedType.DEPT.getCode()));
            myAndGroup.addCondition(QueryCondition.name("published_dept").in(deptList));
            myOrGroup.addCondition(myAndGroup);
        }
        if (CollectionUtils.isNotEmpty(roles)) {
            QueryConditionGroup myAndGroup = new QueryConditionGroup(ConditionOperator.and);
            myAndGroup.addCondition(QueryCondition.name("mainid.published_type").eq(PublishedType.ROLE.getCode()));
            myAndGroup.addCondition(QueryCondition.name("published_role").in(roles.stream().map(TenantRole::getRoleId).collect(Collectors.toList())));
            myOrGroup.addCondition(myAndGroup);
        }
        //RPT0298银行流水认领指定组织发布
        if (CollectionUtils.isNotEmpty(orgSet)) {
            QueryConditionGroup myAndGroup = new QueryConditionGroup(ConditionOperator.and);
            myAndGroup.addCondition(QueryCondition.name("mainid.published_type").eq(PublishedType.ASSIGN_ORG.getCode()));
            QueryConditionGroup assignorgOrGroup = new QueryConditionGroup(ConditionOperator.or);
            assignorgOrGroup.addCondition(QueryCondition.name("published_assignorg").in(orgSet));
            //CZFW-397898【RPT0298银行流水认领指定组织发布】银行交易流水，统收统支模式下，发布单据之后，在到账认领中心，有单据使用组织权限，但没有被发布组织权限的用户，查不到本组织的数据，应该能查到的
            assignorgOrGroup.addCondition(QueryCondition.name("mainid.accentity").in(orgSet));
            myAndGroup.addCondition(assignorgOrGroup);
            myOrGroup.addCondition(myAndGroup);
        }
        mygroup.addCondition(myOrGroup);
        QuerySchema queryCountSchema = QuerySchema.create().distinct().addSelect("mainid");
        queryCountSchema.appendQueryCondition(mygroup);
        List<Map<String, Object>> countMap = MetaDaoHelper.query(BankReconciliationDetail.ENTITY_NAME, queryCountSchema);
        if (CollectionUtils.isNotEmpty(countMap)) {
            for (Map<String, Object> item : countMap) {
                myList.add(item.get("mainid").toString());
            }
        }
        //获取当前页签
        String tabkey = "";
        if (filterVO.getSimpleVOs() != null && filterVO.getSimpleVOs().length > 0) {
            SimpleFilterVO[] simpleFilterVOs = filterVO.getSimpleVOs();
            for (SimpleFilterVO simpleFilterVO : simpleFilterVOs) {
                String itemName = simpleFilterVO.getField();
                if ("published_type".equals(itemName)) {
                    tabkey = simpleFilterVO.getOp();
                }
            }
        }
        //导出逻辑
        if (billDataDto.getExternalData() != null && "export".equalsIgnoreCase(billDataDto.getRequestAction())){
            Map<String, Object> extendData = (Map<String, Object>) billDataDto.getExternalData();
            if (extendData.containsKey("cmptabletabsactivekey") && "my".equalsIgnoreCase(extendData.get(
                    "cmptabletabsactivekey").toString())) {
                tabkey = "neq";
            }
        }
        //拼接符合发布对象的银行流水主键
        if (tabkey.equals("neq")) {
            if (myList.size() > 0) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, myList));
            } else {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", "0"));
            }
        } else {
            //公共认领池处理包含两部分流水数据：
            //1、银行流水的“账户使用组织为空”，且当前用户有该账户权限的主体(原有逐辑)
            //2、银行流水的“使用组织”不为空，且当前用户有该使用组织的权限，且数据在发布时仅指定了“组织”并未指定到“发布部门/发布角色/发布羊户”(查找银行流水处理子表的“发布处理记录处理类型-发布，且发布状态-“己生效”
            QueryConditionGroup conditionGroup = QueryConditionGroup.or(
                    QueryCondition.name("published_type").is_null(),
                    QueryCondition.name("published_type").eq(PublishedType.ORG.getCode())
            );
            QueryConditionGroup conditionGroupNew = QueryConditionGroup.and(filterVO.getQueryConditionGroup(), conditionGroup);
            filterVO.setQueryConditionGroup(conditionGroupNew);
        }
    }
}
