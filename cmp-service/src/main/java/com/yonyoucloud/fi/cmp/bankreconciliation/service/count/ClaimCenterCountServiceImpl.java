package com.yonyoucloud.fi.cmp.bankreconciliation.service.count;

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
import com.yonyou.iuap.metadata.api.model.base.term.LegacyTerms;
import com.yonyou.uap.tenant.service.itf.ITenantRoleUserService;
import com.yonyou.uap.tenantauth.entity.TenantRole;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.util.PageUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.stct.api.openapi.response.BDAccountAuthBatchRespVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.model.Entity;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClaimCenterCountServiceImpl implements IBillCountService {

    @Autowired
    AutoConfigService autoConfigService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private ITenantRoleUserService tenantRoleUserService;

    @Autowired
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    BillCountUtil billCountUtil;
    private static final String DATA_AUTH_LABELCODE = "data_auth";
    private static final String SYSCODE = "CM";

    @Override
    public HashMap<String, Object> getCount(CtmJSONObject params) throws Exception {
        //到账认领中心
        HashMap<String, Object> map = new HashMap<>();
        //基础组织数据准备
        // 获取字段属性
        String accentity = params.getString("accentity");//页面选择的授权使用组织数据
        List chooseAccentityList = new ArrayList<>();
        if (params.get("accentity") instanceof List) {
            chooseAccentityList.addAll((List) params.get("accentity"));
        } else if (params.get("accentity") instanceof String && Objects.nonNull(params.getString("accentity")) && params.getString("accentity").length() > 0) {
            chooseAccentityList.add(params.getString("accentity"));
        }
        //当前节点的授权组织
        Set<String> authorizedOrg = orgDataPermissionService.queryAuthorizedOrgByServiceCode(AppContext.getThreadContext("serviceCode"));
        params.put("authorizedOrg", authorizedOrg);

        // 统收统支模式or代理结算模式
        Boolean agentFlag = autoConfigService.getEnableBizDelegationMode();
        Boolean inoutFlag = autoConfigService.getUnifiedIEModelWhenClaim();
        params.put("agentFlag", agentFlag);
        params.put("inoutFlag", inoutFlag);

        //统收统支找到的上级
        Set<String> inoutAccentitys = new HashSet<>();
        //结算中心找到的上级
        Set<String> agentAccentitys = new HashSet<>();
        //基础组织范围（选择的+统收统支+结算中心）
        Set<String> accList = new HashSet<>();
        //授权使用组织
        if (ObjectUtils.isNotEmpty(chooseAccentityList)) {
            inoutAccentitys = billCountUtil.getInoutAccentityList(inoutFlag, chooseAccentityList);
            agentAccentitys = billCountUtil.getAgentAccentityList(agentFlag, chooseAccentityList);
            accList.addAll(chooseAccentityList);
            accList.addAll(inoutAccentitys);
            accList.addAll(agentAccentitys);
        } else {
            inoutAccentitys = billCountUtil.getInoutAccentityList(inoutFlag, new ArrayList<>(authorizedOrg));
            agentAccentitys = billCountUtil.getAgentAccentityList(agentFlag, new ArrayList<>(authorizedOrg));
            accList.addAll(authorizedOrg);
            accList.addAll(inoutAccentitys);
            accList.addAll(agentAccentitys);
        }
        if (CollectionUtils.isEmpty(accList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100416"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080086", "当前用户无此菜单操作权限!") /* "当前用户无此菜单操作权限!" */);
        }
        params.put("inoutAccentitys", inoutAccentitys);
        params.put("agentAccentitys", agentAccentitys);
        params.put("accList", accList);

        //根据20240715新需求拆分页签认领池数据统计
        splitCommonCount(map, params);
        splitMyCount(map, params);
        return map;
    }

    /**
     * 获取一下前端的公共基础条件拼接
     *
     * @param params
     * @return
     * @throws Exception
     */
    @NotNull
    private QueryConditionGroup getCommonQueryConditionGroup(CtmJSONObject params, String mainid) throws Exception {
        //组装数据
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //选择的和有权限的组织
        List<String> accentitys = new ArrayList<>();
        //选择和有权限的账户
        List<String> bankaccounts = new ArrayList<>();
        List<String> orgids = new ArrayList<>();
        List<String> billclaimstatuss = new ArrayList<>();
        List<Short> billclaimstatusList = new ArrayList<>();
        String startDate = params.getString("startDate");//交易开始日期（银行流水认领、到账认领中心、我的认领）
        String endDate = params.getString("endDate");//交易结束日期（银行流水认领、到账认领中心、我的认领）
        String confirmstatus = params.getString("confirmstatus");//授权使用组织状态
        String dcFlag = params.getString("dcFlag");//收付方向（银行流水认领、到账认领中心、我的认领）
        String currency = params.getString("currency");////币种（银行流水认领、到账认领中心、我的认领）
        String bankaccount = params.getString("bankaccount");//银行账号
        if (ObjectUtils.isNotEmpty(bankaccount)) {
            bankaccounts = JSONBuilderUtils.stringToBeanList(bankaccount, String.class);//银行账户（多个）
        }
        String amountSmall = params.getString("amountSmall");//金额1
        String amountBig = params.getString("amountBig");//金额2
        String orgid = params.getString("orgid");//所属组织
        if (ObjectUtils.isNotEmpty(orgid)) {
            orgids = params.getJSONArray("orgid").toJavaList(String.class);//所属组织（多个）
            conditionGroup.appendCondition(QueryCondition.name(mainid + "orgid").in(orgids));
        }
        String toaccountno = params.getString("toaccountno");//对方账号
        String toaccountname = params.getString("toaccountname");//对方户名
        String billclaimstatus = params.getString("billclaimstatus");//认领状态（到账认领中心）（多个）
        if (ObjectUtils.isNotEmpty(billclaimstatus)) {
            billclaimstatuss = JSONBuilderUtils.stringToBeanList(billclaimstatus, String.class);
            billclaimstatusList = billclaimstatuss.stream().map(item -> Short.parseShort(item)).collect(Collectors.toList());
        }
        if (ObjectUtils.isNotEmpty(currency)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "currency").eq(currency));
        }

        if (ObjectUtils.isNotEmpty(toaccountno)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "to_acct_no").eq(toaccountno));
        }
        //对方户名
        if (ObjectUtils.isNotEmpty(toaccountname)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "to_acct_name").eq(toaccountname));
        }
        //认领状态（多个）
        if (ObjectUtils.isNotEmpty(billclaimstatuss)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "billclaimstatus").in(billclaimstatusList));
        }
        //授权组织使用状态
        if (ObjectUtils.isNotEmpty(confirmstatus)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "confirmstatus").eq(confirmstatus));
        }
        //交易金额
        if (ObjectUtils.isNotEmpty(amountSmall) && ObjectUtils.isNotEmpty(amountBig)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "tran_amt").between(amountSmall, amountBig));
        }
        if (ObjectUtils.isNotEmpty(amountSmall) && ObjectUtils.isEmpty(amountBig)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "tran_amt").egt(amountSmall));
        }
        if (ObjectUtils.isEmpty(amountSmall) && ObjectUtils.isNotEmpty(amountBig)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "tran_amt").elt(amountBig));
        }
        //收付方向
        if (ObjectUtils.isNotEmpty(dcFlag)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "dc_flag").eq(dcFlag));
        }
        //交易日期
        if (ObjectUtils.isNotEmpty(startDate) && ObjectUtils.isNotEmpty(endDate)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "tran_date").between(startDate, endDate));
        }
        if (ObjectUtils.isNotEmpty(startDate) && ObjectUtils.isEmpty(endDate)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "tran_date").egt(startDate));
        }
        if (ObjectUtils.isEmpty(startDate) && ObjectUtils.isNotEmpty(endDate)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "tran_date").elt(endDate));
        }
        QueryConditionGroup orGroup = new QueryConditionGroup(ConditionOperator.or);
        orGroup.addCondition(QueryCondition.name(mainid + "serialdealtype").not_eq("5"));
        orGroup.addCondition(QueryCondition.name(mainid + "serialdealtype").is_null());
        conditionGroup.appendCondition(orGroup);
        //拼接组织和账户的条件
        getAccountAccentityCondition(mainid, conditionGroup, bankaccounts, params);

        //待办消息跳转，会传入特定的银行流水id。key=bankid
        String bankid = params.getString("bankid");
        if (ObjectUtils.isNotEmpty(bankid)) {
            conditionGroup.appendCondition(QueryCondition.name(mainid + "id").eq(bankid));
        }

        conditionGroup.appendCondition(QueryCondition.name(mainid + "ispublish").eq("1"));
        Entity entity = MetaDaoHelper.getEntity(BankReconciliation.ENTITY_NAME);
        List<String> fieldsList = entity.attributes().stream()
                .filter(attr -> attr.containsTerm(LegacyTerms.Term.data_auth))
                .map(attr -> attr.name())
                .collect(Collectors.toList());
        String[] fields = fieldsList.toArray(new String[0]);
        Map<String, List<Object>> dataPermission = AuthUtil.dataPermission(SYSCODE, BankReconciliation.ENTITY_NAME, null, fields);
        //数据权限
        if (dataPermission != null && dataPermission.size() > 0) {
            for (Map.Entry<String, List<Object>> entry : dataPermission.entrySet()) {
                if (fieldsList.contains(entry.getKey()) && entry.getValue() != null && entry.getValue().size() > 0) {
                    List<QueryCondition> conditionList1 = new ArrayList<>();
                    conditionList1.add(QueryCondition.name(mainid + entry.getKey()).in(entry.getValue()));
                    conditionList1.add(QueryCondition.name(mainid + entry.getKey()).is_null());
                    QueryConditionGroup queryCondition = QueryConditionGroup.or(conditionList1.toArray(new QueryCondition[0]));
                    conditionGroup.addCondition(queryCondition);
                }
            }
        }
        return conditionGroup;
    }

    private void getAccountAccentityCondition(String mainid, QueryConditionGroup conditionGroup, List<String> bankaccounts, CtmJSONObject params) throws Exception {
        Set<String> accList = (Set<String>) params.get("accList");
        // 统收统支模式or代理结算模式
        Boolean agentFlag = (Boolean) params.get("agentFlag");
        Boolean inoutFlag = (Boolean) params.get("inoutFlag");
        //当前节点的授权组织
        Set<String> authorizedOrg = (Set<String>) params.get("authorizedOrg");
        //统收统支找到的上级
        Set<String> inoutAccentitys = (Set<String>) params.get("inoutAccentitys");
        //结算中心找到的上级
        Set<String> agentAccentitys = (Set<String>) params.get("agentAccentitys");
        //授权使用组织的银行账户
        if (CollectionUtils.isEmpty(bankaccounts)) {
            bankaccounts.addAll(enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(new ArrayList<>(accList)));
        }
        //授权的银行账户和组织
        QueryConditionGroup andGroup = new QueryConditionGroup(ConditionOperator.and);

        if (ObjectUtils.isNotEmpty(accList)) {
            QueryConditionGroup accentityOrGroup = new QueryConditionGroup(ConditionOperator.or);
            accentityOrGroup.addCondition(QueryCondition.name(mainid + "accentity").in(accList));
            accentityOrGroup.addCondition(QueryCondition.name(mainid + "accentity").is_null());
            andGroup.appendCondition(accentityOrGroup);
        } else {
            andGroup.addCondition(QueryCondition.name(mainid + "accentity").is_null());
        }
        if (ObjectUtils.isNotEmpty(bankaccounts)) {
            andGroup.appendCondition(QueryCondition.name(mainid + "bankaccount").in(bankaccounts));
        } else {
            andGroup.addCondition(QueryCondition.name(mainid + "bankaccount").is_null());
        }
        conditionGroup.addCondition(andGroup);

        //资金结算关系配置的条件拼接
        QueryConditionGroup configOrGroup = new QueryConditionGroup(ConditionOperator.or);
        //不授权的组织：要取并集
        Set<String> noAccentitys = new HashSet<>();
        //授权的组织：要取交集
        Set<String> authAccentitys = new HashSet<>();
        //授权的指定账户
        Set<String> specAccounts = new HashSet<>();
        //授权的指定账户的结算中心
        Set<String> specAccentitys = new HashSet<>();
        if (ObjectUtils.isNotEmpty(authorizedOrg) && agentFlag) {
            Map<BDAccountAuthBatchRespVO.BDAccountAuthKey, BDAccountAuthBatchRespVO.BDAccountAuthRespVO> authRespVOMap =
                    billCountUtil.getAuthDelegationList(new ArrayList<>(authorizedOrg), agentFlag);
            if (ObjectUtils.isNotEmpty(authRespVOMap)) {
                // 授权类型：0：不授权，1：授权全部账户，2：授权指定账户
                for (BDAccountAuthBatchRespVO.BDAccountAuthRespVO item : authRespVOMap.values()) {
                    if (item.getAuthType() == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100417"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900036", "资金结算委托关系账户授权查询接口数据有误：授权类型为空！") /* "资金结算委托关系账户授权查询接口数据有误：授权类型为空！" */);
                    }
                    if (authorizedOrg.contains(item.getSettlementOrg()) || inoutAccentitys.contains(item.getSettlementOrg())) {
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
                for (BDAccountAuthBatchRespVO.BDAccountAuthRespVO item : authRespVOMap.values()) {
                    //如果指定账户了 那就找不是授权全部的 找不是授权节点配置的
                    if (item.getAuthType().equals("2") && !authorizedOrg.contains(item.getSettlementOrg()) && !authAccentitys.contains(item.getSettlementOrg())) {
                        if (item.getAuthAccounts() == null || CollectionUtils.isEmpty(item.getAuthAccounts())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100418"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900035", "资金结算委托关系账户授权查询接口数据有误：授权类型为指定账户时，账户为空！") /* "资金结算委托关系账户授权查询接口数据有误：授权类型为指定账户时，账户为空！" */);
                        }
                        //不是授权全部的 那是真的要单独授权了
                        if (!authorizedOrg.contains(item.getSettlementOrg())) {
                            List<BDAccountAuthBatchRespVO.BDAccountAuthItemRespVO> accountAuthItemRespVOList =
                                    item.getAuthAccounts();
                            for (BDAccountAuthBatchRespVO.BDAccountAuthItemRespVO account :
                                    accountAuthItemRespVOList) {
                                specAccentitys.add(item.getSettlementOrg());
                                specAccounts.add(account.getEnterpriseBankAcctId());
                                QueryConditionGroup innerGgroup = new QueryConditionGroup(ConditionOperator.and);
                                innerGgroup.addCondition(QueryCondition.name(mainid + "accentity").eq(item.getSettlementOrg()));
                                innerGgroup.addCondition(QueryCondition.name(mainid + "bankaccount").eq(account.getEnterpriseBankAcctId()));
                                if (account.getOperateAuth() != null && account.getOperateAuth().size() == 1) {
                                    // 操作权限:0: 收款流水,1: 付款流水
                                    innerGgroup.addCondition(QueryCondition.name(mainid + "dc_flag").eq(account.getOperateAuth().get(0).equals("0") ? DcFlagEnum.Credit.getValue() : DcFlagEnum.Debit.getValue()));
                                }
                                configOrGroup.addCondition(innerGgroup);
                            }
                        }
                    }
                }
                //取能看到的最大合集
                if (ObjectUtils.isNotEmpty(noAccentitys)) {
                    Set<String> noAccentitySet = new HashSet<>();
                    for (String noacc : noAccentitys) {
                        //不是授权节点配置的 不是授权全部的 也不是授权指定的 也不是统收统支配的 那么一定不授权了 那是真的看不到
                        if (!authAccentitys.contains(noacc) && !specAccentitys.contains(noacc) && !inoutAccentitys.contains(noacc)) {
                            noAccentitySet.add(noacc);
                        }
                    }
                    if (ObjectUtils.isNotEmpty(noAccentitySet)) {
                        conditionGroup.addCondition(QueryCondition.name(mainid + "accentity").not_in(noAccentitySet));
                    }
                }
                if (ObjectUtils.isNotEmpty(specAccentitys)) {
                    Set<String> condition = new HashSet<>();
                    condition.addAll(inoutAccentitys);
                    condition.addAll(authorizedOrg);
                    //还得带上统收统支配的
                    configOrGroup.addCondition(QueryCondition.name(mainid + "accentity").in(condition));
                    configOrGroup.addCondition(QueryCondition.name(mainid + "accentity").is_null());
                    conditionGroup.addCondition(configOrGroup);
                    params.put("specAccentitys", specAccentitys);
                }
            }
        }
    }

    private void splitCommonCount(HashMap<String, Object> map, CtmJSONObject params) throws Exception {
        //公共认领池处理包含两部分流水数据：
        //1、银行流水的“账户使用组织为空”，且当前用户有该账户权限的主体(原有逐辑)
        //2、银行流水的“使用组织”不为空，且当前用户有该使用组织的权限，且数据在发布时仅指定了“组织”并未指定到“发布部门/发布角色/发布羊户”(查找银行流水处理子表的“发布处理记录处理类型-发布，且发布状态-“己生效”
        QueryConditionGroup commongroup = getCommonQueryConditionGroup(params, "");
        QueryConditionGroup commonOrGroup = new QueryConditionGroup(ConditionOperator.or);
        commonOrGroup.addCondition(QueryCondition.name("published_type").eq(PublishedType.ORG.getCode()));
        commonOrGroup.addCondition(QueryCondition.name("published_type").is_null());
//        if(params.get("startDate") != null){
//            commonOrGroup.addCondition(QueryCondition.name("tran_date").between(params.get("startDate"), params.get("endDate")));
//        }
        commongroup.addCondition(commonOrGroup);

        long commonall = PageUtils.queryCount(commongroup, BankReconciliation.ENTITY_NAME);
        //账户使用组织待确认：1、银行流水的“账户使用组织”为空，按照用户分配的组织权限匹配授权使用组织，依据匹配到的授权使用组织查询分配的银行账号，显示该银行账户+市种+授权使用组织为空的数据(原有逻辑)
        // ，数据流水完结状态=未完结((D即收付单据关联状态=未关联 ②收付单据关联状态=己关联且入账类型三冲挂账’且待认领金额不等于0 ③收付单据关联状态=己关联且入账类型三正常入账目待认领金额不等于0))且是否发布=“是”;
        long commonconfirmed = getClaimCenterCommonconfirmedCount(commongroup);
        //待处理：数据为流水数据，银行流水的“账户使用组织”有值，并且数据流水完结状态=未完结((①即收付单据关联状态=未关联 ②收付单据关联状态:=已关联且入账类型=’冲挂账’且待认领金额不等于0③收付单据关联状态:=己关联且入账类型=’正常入账’且待认领金额不等于0))且是否发布=“是”;
        long commonprocessed = getClaimCenterCommonprocessedCount(commongroup);
        //已认领：数据为“已生成认领单”的流水。1)此分类包含的数据为流水数据，并且数据流水完结状态=已完结且待认领金额等于0且是否发布=“是”
        long commonclaimed = getClaimCenterCommonclaimedCount(commongroup);

        //公共认领池
        map.put("commonconfirmed", commonconfirmed);//账户使用组织待确认
        map.put("commonprocessed", commonprocessed);//待处理
        map.put("commonclaimed", commonclaimed);//已认领
        map.put("commonall", commonall);//全部
    }

    private void splitMyCount(HashMap<String, Object> map, CtmJSONObject params) throws Exception {
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
        QueryConditionGroup mygroup = getCommonQueryConditionGroup(params, "mainid.");
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
        Set<String> specAccentitys = (Set<String>) params.get("specAccentitys");
        Set<String> authorizedOrg = (Set<String>) params.get("authorizedOrg");
        if (CollectionUtils.isNotEmpty(authorizedOrg)) {
            QueryConditionGroup myAndGroup = new QueryConditionGroup(ConditionOperator.and);
            myAndGroup.addCondition(QueryCondition.name("mainid.published_type").eq(PublishedType.ASSIGN_ORG.getCode()));
            QueryConditionGroup assignorgOrGroup = new QueryConditionGroup(ConditionOperator.or);
            assignorgOrGroup.addCondition(QueryCondition.name("published_assignorg").in(authorizedOrg));
            //CZFW-397898【RPT0298银行流水认领指定组织发布】银行交易流水，统收统支模式下，发布单据之后，在到账认领中心，有单据使用组织权限，但没有被发布组织权限的用户，查不到本组织的数据，应该能查到的
            assignorgOrGroup.addCondition(QueryCondition.name("mainid.accentity").in(authorizedOrg));
            myAndGroup.addCondition(assignorgOrGroup);
            myOrGroup.addCondition(myAndGroup);
        }
        mygroup.addCondition(myOrGroup);

        long myall = queryCount(mygroup, BankReconciliationDetail.ENTITY_NAME);
        //我的认领池：
        //待处理 :数据流水完结状态=未完结((①即收付单据关联状态=未关联②收付单据关联状态=已关联且入账类型=’冲挂账’且待认领金额不等于0 ③收付单据关联状态=己关联且入账类型=’正常入账’且待认领金额不等于0))且是否发布=“是”;
        long myprocessed = getClaimCenterMyprocessedCount(mygroup);
        //已认领：数据为流水数据，并且数据流水完结状态= 已完结待认领金额等于0且是否发布=“是”；
        long myclaimed = getClaimCenterMyclaimedCount(mygroup);

        //我的认领池
        map.put("myprocessed", myprocessed);//待处理
        map.put("myclaimed", myclaimed);//已认领
        map.put("myall", myall);//全部
    }

    /**
     * 我的认领池-待处理 :数据流水完结状态=未完结((①即收付单据关联状态=未关联且待认领金额不等于0 ②收付单据关联状态=已关联且入账类型=’冲挂账’且待认领金额不等于0 ③收付单据关联状态=己关联且入账类型=’正常入账’且待认领金额不等于0))且是否发布=“是”;
     * amounttobeclaimed > 0 && (associationstatus == 0) || (associationstatus == 1 &&(entrytype==1 || entrytype==3))
     *
     * @return
     */
    private long getClaimCenterMyprocessedCount(QueryConditionGroup group) throws Exception {
        QueryConditionGroup queryConditionGroup = group.clone();
        List<String> entrytypeList = new ArrayList<>();
        entrytypeList.add(String.valueOf(EntryType.Normal_Entry.getValue()));
        entrytypeList.add(String.valueOf(EntryType.CrushHang_Entry.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("mainid.amounttobeclaimed").not_eq(0));
        QueryConditionGroup conditionOrGroup = new QueryConditionGroup(ConditionOperator.or);
        conditionOrGroup.addCondition(QueryCondition.name("mainid.associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        QueryConditionGroup innerConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        innerConditionGroup.addCondition(QueryCondition.name("mainid.associationstatus").eq(AssociationStatus.Associated.getValue()));
        innerConditionGroup.addCondition(QueryCondition.name("mainid.entrytype").in(entrytypeList));
        conditionOrGroup.addCondition(innerConditionGroup);
        queryConditionGroup.addCondition(conditionOrGroup);
        return queryCount(queryConditionGroup, BankReconciliationDetail.ENTITY_NAME);
    }


    /**
     * 公共认领池-账户使用组织待确认：1、银行流水的“账户使用组织”为空，按照用户分配的组织权限匹配授权使用组织，依据匹配到的授权使用组织查询分配的银行账号，显示该银行账户+市种+授权使用组织为空的数据(原有逻辑)
     * 数据流水完结状态=未完结((D即收付单据关联状态=未关联 ②收付单据关联状态=己关联且入账类型三冲挂账’且待认领金额不等于0 ③收付单据关联状态=己关联且入账类型三正常入账目待认领金额不等于0))且是否发布=“是”;
     * (0==confirmstatus && 0 == billclaimstatus) && ((associationstatus== 0) || (associationstatus == 1 && amounttobeclaimed != 0 &&(entrytype==1 || entrytype==3)))
     *
     * @return
     */
    private long getClaimCenterCommonconfirmedCount(QueryConditionGroup group) throws Exception {
        QueryConditionGroup queryConditionGroup = group.clone();
        List<String> entrytypeList = new ArrayList<>();
        entrytypeList.add(String.valueOf(EntryType.Normal_Entry.getValue()));
        entrytypeList.add(String.valueOf(EntryType.CrushHang_Entry.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("confirmstatus").eq(ConfirmStatusEnum.Confirming.getIndex()));
        queryConditionGroup.addCondition(QueryCondition.name("billclaimstatus").eq(BillClaimStatus.ToBeClaim.getValue()));
        QueryConditionGroup conditionOrGroup = new QueryConditionGroup(ConditionOperator.or);
        conditionOrGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        QueryConditionGroup innerConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        innerConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.Associated.getValue()));
        innerConditionGroup.addCondition(QueryCondition.name("amounttobeclaimed").not_eq(0));
        innerConditionGroup.addCondition(QueryCondition.name("entrytype").in(entrytypeList));
        conditionOrGroup.addCondition(innerConditionGroup);
        queryConditionGroup.addCondition(conditionOrGroup);
        return PageUtils.queryCount(queryConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    /**
     * 我的认领池-已认领：数据为流水数据，并且数据流水完结状态= 已完结待认领金额等于0且是否发布=“是”；
     * amounttobeclaimed == 0
     *
     * @return
     */
    private long getClaimCenterMyclaimedCount(QueryConditionGroup group) throws Exception {
        QueryConditionGroup queryConditionGroup = group.clone();
        queryConditionGroup.addCondition(QueryCondition.name("mainid.amounttobeclaimed").eq(0));
        return queryCount(queryConditionGroup, BankReconciliationDetail.ENTITY_NAME);
    }

    /**
     * 公共认领池-已认领：数据为“已生成认领单”的流水。1)此分类包含的数据为流水数据，并且数据流水完结状态=已完结且待认领金额等于0且是否发布=“是”
     * ( 1 == billclaimstatus) &&  amounttobeclaimed == 0
     *
     * @return
     */
    private long getClaimCenterCommonclaimedCount(QueryConditionGroup group) throws Exception {
        QueryConditionGroup queryConditionGroup = group.clone();
        queryConditionGroup.addCondition(QueryCondition.name("billclaimstatus").eq(BillClaimStatus.Claimed.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("amounttobeclaimed").eq(0));
        return PageUtils.queryCount(queryConditionGroup, BankReconciliation.ENTITY_NAME);
    }


    /**
     * 公共认领池-待处理：数据为流水数据，银行流水的“账户使用组织”有值，并且数据流水完结状态=未完结
     * ((①即收付单据关联状态=未关联 ②收付单据关联状态:=已关联且入账类型=’冲挂账’且待认领金额不等于0③收付单据关联状态:=己关联且入账类型=’正常入账’且待认领金额不等于0))且是否发布=“是”;
     * (1==confirmstatus && 0 == billclaimstatus) && ((associationstatus== 0) || (associationstatus == 1 && amounttobeclaimed != 0 &&(entrytype==1 || entrytype==3)))
     *
     * @return
     */
    private long getClaimCenterCommonprocessedCount(QueryConditionGroup group) throws Exception {
        QueryConditionGroup queryConditionGroup = group.clone();
        List<String> entrytypeList = new ArrayList<>();
        entrytypeList.add(String.valueOf(EntryType.Normal_Entry.getValue()));
        entrytypeList.add(String.valueOf(EntryType.CrushHang_Entry.getValue()));
        List<String> confirmstatusList = new ArrayList<>();
        confirmstatusList.add(String.valueOf(ConfirmStatusEnum.Confirmed.getIndex()));
        confirmstatusList.add(String.valueOf(ConfirmStatusEnum.RelationConfirmed.getIndex()));
        queryConditionGroup.addCondition(QueryCondition.name("confirmstatus").in(confirmstatusList));
        queryConditionGroup.addCondition(QueryCondition.name("billclaimstatus").eq(BillClaimStatus.ToBeClaim.getValue()));
        QueryConditionGroup conditionOrGroup = new QueryConditionGroup(ConditionOperator.or);
        conditionOrGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        QueryConditionGroup innerConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        innerConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.Associated.getValue()));
        innerConditionGroup.addCondition(QueryCondition.name("amounttobeclaimed").not_eq(0));
        innerConditionGroup.addCondition(QueryCondition.name("entrytype").in(entrytypeList));
        conditionOrGroup.addCondition(innerConditionGroup);
        queryConditionGroup.addCondition(conditionOrGroup);
        return PageUtils.queryCount(queryConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    /**
     * 查询条数
     *
     * @param conditionGroup 查询列表传入的条件
     * @param entityName     需要查询的实体名称
     * @return 查询所有的集合
     * @throws Exception 查询数据库出现的异常
     */
    public static long queryCount(QueryConditionGroup conditionGroup, String entityName) throws Exception {
        QuerySchema queryCountSchema = QuerySchema.create().distinct().addSelect("mainid");
        queryCountSchema.appendQueryCondition(conditionGroup);
        List<Map<String, Object>> countMap = MetaDaoHelper.query(entityName, queryCountSchema);
        if (CollectionUtils.isNotEmpty(countMap)) {
            return Long.parseLong(String.valueOf(countMap.size()));
        } else {
            return 0L;
        }
    }
}
