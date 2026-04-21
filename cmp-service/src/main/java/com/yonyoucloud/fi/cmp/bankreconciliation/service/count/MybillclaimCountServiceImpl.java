package com.yonyoucloud.fi.cmp.bankreconciliation.service.count;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.RecheckStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.CmpAuthUtils;
import com.yonyoucloud.fi.cmp.util.PageUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MybillclaimCountServiceImpl implements IBillCountService{

    @Autowired
    AutoConfigService autoConfigService;
    @Autowired
    OrgDataPermissionService orgDataPermissionService;
    @Autowired
    BillCountUtil billCountUtil;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public HashMap<String, Object> getCount(CtmJSONObject params) throws Exception {
        HashMap<String, Object> map = new HashMap<>();
        List<String> accentitys = new ArrayList<>();
        List<String> bankaccounts = new ArrayList<>();
        List<String> orgids = new ArrayList<>();

        List<String> orgList = new ArrayList<>();
        List<String> associationstatussList = new ArrayList<>();
        // affiliatedorgid
        List<String> affiliatedorgidList = new ArrayList<>();
        //组装数据
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.or);
        // 获取字段属性
        String accentity = params.getString("accentity");//授权使用组织（银行流水认领、到账认领中心、我的认领）
        if (ObjectUtils.isNotEmpty(accentity)) {
            accentitys = params.getJSONArray("accentity").toJavaList(String.class);//授权使用组织（多个）
        }
        String affiliatedorgid = params.getString("affiliatedorgid");//授权使用组织（银行流水认领、到账认领中心、我的认领）
        if (ObjectUtils.isNotEmpty(affiliatedorgid)) {
            affiliatedorgidList = params.getJSONArray("affiliatedorgid").toJavaList(String.class);//授权使用组织（多个）
        }
        String startDate = params.getString("startDate");//交易开始日期（银行流水认领、到账认领中心、我的认领）
        String endDate = params.getString("endDate");//交易结束日期（银行流水认领、到账认领中心、我的认领）
        String confirmstatus = params.getString("confirmstatus");//授权使用组织状态
        String dcFlag = params.getString("dcFlag");//收付方向（银行流水认领、到账认领中心、我的认领）
        String currency = params.getString("currency");////币种（银行流水认领、到账认领中心、我的认领）
        String bankaccount = params.getString("bankaccount");//银行账号
        if (ObjectUtils.isNotEmpty(bankaccount)) {
            bankaccounts = JSONBuilderUtils.stringToBeanList(bankaccount, String.class);//银行账户（多个）
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(bankaccounts));
        }
        String amountSmall = params.getString("amountSmall");//金额1
        String amountBig = params.getString("amountBig");//金额2
        String orgid = params.getString("orgid");//所属组织
        if (ObjectUtils.isNotEmpty(orgid)) {
            orgids = params.getJSONArray("orgid").toJavaList(String.class);//所属组织（多个）
            conditionGroup.appendCondition(QueryCondition.name("orgid").in(orgids));
        }
        String toaccountno = params.getString("toaccountno");//对方账号
        String toaccountname = params.getString("toaccountname");//对方户名
        String project = params.getString("project");//项目
        String claimaccount = params.getString("claimaccount");//认领账户
        String quicktype = params.getString("quicktype");//款项类型

        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getTenantId()));//租户id
        if (ObjectUtils.isNotEmpty(currency)) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        String associationstatus = params.getString("associationstatus");//业务关联状态（银行流水认领）
        String associationstatuss = params.getString("associationstatus");//业务关联状态（我的认领）（多个）
        if (ObjectUtils.isNotEmpty(associationstatuss)) {
            associationstatussList = JSONBuilderUtils.stringToBeanList(associationstatus, String.class);
        }
        //业务单元
        String org = params.getString("org");//业务单元（我的认领）（多个）
        if (ObjectUtils.isNotEmpty(org)) {
            orgList = params.getJSONArray("org").toJavaList(String.class);
        }
        String code = params.getString("code");//单据编码（我的认领）

        // 统收统支模式or代理结算模式
        Boolean agentFlag = autoConfigService.getEnableBizDelegationMode();
        Boolean inoutFlag = autoConfigService.getUnifiedIEModelWhenClaim();

        List<String> recheckstatusList = new ArrayList<>();
        String recheckstatus = params.getString("recheckstatus");//单据状态
        if (ObjectUtils.isNotEmpty(recheckstatus)) {
            recheckstatusList = JSONBuilderUtils.stringToBeanList(recheckstatus, String.class);
        }
        String actualclaimaccentiry = params.getString("actualclaimaccentiry");//实际认领单位
        String claimstaff = params.getString("claimstaff");//认领人
        String cmpTableTabsActiveKey = params.getString("published_type");//发布对象,用来暂存前端页签信息


        //我的认领
        if(ObjectUtils.isNotEmpty(recheckstatus)){
            conditionGroup.appendCondition(QueryCondition.name("recheckstatus").in(recheckstatusList));
        }
        if(ObjectUtils.isNotEmpty(actualclaimaccentiry)){
            conditionGroup.appendCondition(QueryCondition.name("actualclaimaccentiry").in((List)params.get("actualclaimaccentiry")));
        }

        //当前节点的授权组织
        Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(AppContext.getThreadContext("serviceCode"));
        if(!QueryBaseDocUtils.isContainSettlementCenter(orgSet) && CollectionUtils.isNotEmpty(orgSet)){
            conditionGroup.addCondition(QueryCondition.name("actualclaimaccentiry").in(orgSet));
        }

        if(ObjectUtils.isNotEmpty(claimstaff)){
            conditionGroup.appendCondition(QueryCondition.name("claimstaff").like(claimstaff));
        }
        if(ObjectUtils.isNotEmpty(toaccountno)){
            conditionGroup.appendCondition(QueryCondition.name("toaccountno").like(toaccountno));
        }
        if (ObjectUtils.isNotEmpty(toaccountname)) {
            conditionGroup.appendCondition(QueryCondition.name("toaccountname").like(toaccountname));
        }
        if (ObjectUtils.isNotEmpty(quicktype)) {
            conditionGroup.appendCondition(QueryCondition.name("quicktype").eq(quicktype));
        }
        if (ObjectUtils.isNotEmpty(project)) {
            conditionGroup.appendCondition(QueryCondition.name("project").eq(project));
        }
        if (ObjectUtils.isNotEmpty(claimaccount)) {
            conditionGroup.appendCondition(QueryCondition.name("claimaccount").eq(claimaccount));
        }
        //单据编码
        if (ObjectUtils.isNotEmpty(code)) {
            conditionGroup.appendCondition(QueryCondition.name("code").like(code));
        }
        //授权组织使用状态
        if (ObjectUtils.isNotEmpty(confirmstatus)) {
            conditionGroup.appendCondition(QueryCondition.name("confirmstatus").eq(confirmstatus));
        }
        //认领金额
        if (ObjectUtils.isNotEmpty(amountSmall) && ObjectUtils.isNotEmpty(amountBig)) {
            conditionGroup.appendCondition(QueryCondition.name("totalamount").between(amountSmall, amountBig));
        }
        if (ObjectUtils.isNotEmpty(amountSmall) && ObjectUtils.isEmpty(amountBig)) {
            conditionGroup.appendCondition(QueryCondition.name("totalamount").egt(amountSmall));
        }
        if (ObjectUtils.isEmpty(amountSmall) && ObjectUtils.isNotEmpty(amountBig)) {
            conditionGroup.appendCondition(QueryCondition.name("totalamount").elt(amountBig));
        }
        //收付方向
        if (ObjectUtils.isNotEmpty(dcFlag)) {
            conditionGroup.appendCondition(QueryCondition.name("direction").eq(dcFlag));
        }
        //认领日期
        if(ObjectUtils.isNotEmpty(startDate) && ObjectUtils.isNotEmpty(endDate)){
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").between(startDate, endDate));
        }
        if(ObjectUtils.isNotEmpty(startDate) && ObjectUtils.isEmpty(endDate)){
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").egt(startDate));
        }
        if(ObjectUtils.isEmpty(startDate) && ObjectUtils.isNotEmpty(endDate)){
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").elt(endDate));
        }
        //授权使用组织
        if (ObjectUtils.isNotEmpty(accentitys)) {
            accentitys = billCountUtil.getAccentityList(accentitys, inoutFlag, agentFlag);
            conditionGroup3.addCondition(QueryCondition.name("accentity").in(accentitys));
        } else {
            Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BILLCLAIMCARD);
            List<String> authOrgList = new ArrayList<>(orgs);
            accentitys = billCountUtil.getAccentityList(authOrgList, inoutFlag, agentFlag);
            if(CollectionUtils.isEmpty(accentitys)){
                throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CD", "当前用户没有任何组织权限！！") /* "当前用户没有任何组织权限！！" */);
            }
            conditionGroup3.addCondition(QueryCondition.name("accentity").in(accentitys));
            conditionGroup3.addCondition(QueryCondition.name("accentity").is_null());
        }
        // affiliatedorgid 所属组织
        if (ObjectUtils.isNotEmpty(affiliatedorgidList)) {
            conditionGroup.addCondition(QueryCondition.name("affiliatedorgid").in(affiliatedorgidList));
        }

        if (ObjectUtils.isEmpty(bankaccount)) {
//            EnterpriseParams enterpriseParams = new EnterpriseParams();
//            enterpriseParams.setOrgidList(new ArrayList<>(accentitys));
//            List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            Set<String> accounts = new HashSet<>();
//            for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
//                accounts.add(enterpriseBankAcctVOWithRange.getId());
//            }
            accounts.addAll(enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(accentitys));
            QueryConditionGroup conditionGroup4 = new QueryConditionGroup(ConditionOperator.or);
            if (accounts != null && accounts.size() > 0) {
                conditionGroup4.addCondition(QueryCondition.name("bankaccount").in(accounts));
            }
            conditionGroup4.addCondition(QueryCondition.name("bankaccount").is_null());
            conditionGroup.addCondition(conditionGroup4);
        }

        //业务单元
        if (ObjectUtils.isNotEmpty(orgList)) {
            conditionGroup.addCondition(QueryCondition.name("org").in(orgList));
        }
        //业务关联状态
        if (ObjectUtils.isNotEmpty(associationstatussList)) {
            conditionGroup.addCondition(QueryCondition.name("associationstatus").in(associationstatussList));
        }
        conditionGroup.addCondition(conditionGroup3);
        //待提交：认领单的“认领完结状态=未完结”；且“单据状态=已保存、已驳回、已终止”
        long init = getBillClaimInitCount(conditionGroup);
        //审批中：认领单的“认领完结状态=未完结”；且“单据状态=审批中”或“待复核”
        long submited = getBillClaimSubmitedCount(conditionGroup);
        //待处理：认领单的“认领完结状态=未完结”；且“单据状态=认领成功”。并且认领流程未完结的数据
        long pending = getBillClaimPendingCount(conditionGroup);
        //已结束：认领单的“认领完结状态=已完结”；且“单据状态=认领成功”。
        long ended = getBillClaimEndedCount(conditionGroup);
        //发布处理中（业务关联状态=未关联 && 发布状态=已发布）
        map.put("init",init);//待提交
        map.put("submited",submited);//审批中
        map.put("pending",pending);//待处理
        map.put("ended",ended);//已结束
        map.put("all", PageUtils.queryCount(conditionGroup, BillClaim.ENTITY_NAME));//全部

        return map;
    }


    /**
     * 我的认领-已结束：认领单的“认领完结状态=已完结”；且“单据状态=认领成功”。
     *
     * @param conditionGroup
     * @return
     * @throws Exception
     */
    private long getBillClaimEndedCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(conditionGroup);
        queryConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.Associated.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("recheckstatus").eq(RecheckStatus.Reviewed.getValue()));
        return PageUtils.queryCount(queryConditionGroup, BillClaim.ENTITY_NAME);
    }

    /**
     * 我的认领-审批中：认领单的“认领完结状态=未完结”；且“单据状态=审批中”或“待复核”
     *
     * @param conditionGroup
     * @return
     * @throws Exception
     */
    private long getBillClaimSubmitedCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(conditionGroup);
        queryConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        List<String> recheckstatusList = new ArrayList<>();
        recheckstatusList.add(String.valueOf(RecheckStatus.NotReviewed.getValue()));
        recheckstatusList.add(String.valueOf(RecheckStatus.Submited.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("recheckstatus").in(recheckstatusList));
        return PageUtils.queryCount(queryConditionGroup, BillClaim.ENTITY_NAME);
    }


    /**
     * 我的认领-待处理：认领单的“认领完结状态=未完结”；且“单据状态=认领成功”。并且认领流程未完结的数据
     *
     * @param conditionGroup
     * @return
     * @throws Exception
     */
    private long getBillClaimPendingCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(conditionGroup);
        queryConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("recheckstatus").eq(RecheckStatus.Reviewed.getValue()));
        return PageUtils.queryCount(queryConditionGroup, BillClaim.ENTITY_NAME);
    }


    /**
     * 我的认领-待提交：认领单的“认领完结状态=未完结”；且“单据状态=已保存、已驳回、已终止”
     *
     * @param conditionGroup
     * @return
     * @throws Exception
     */
    private long getBillClaimInitCount(QueryConditionGroup conditionGroup) throws Exception {
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(conditionGroup);
        queryConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        List<String> recheckstatusList = new ArrayList<>();
        recheckstatusList.add(String.valueOf(RecheckStatus.Saved.getValue()));
        recheckstatusList.add(String.valueOf(RecheckStatus.Terminated.getValue()));
        recheckstatusList.add(String.valueOf(RecheckStatus.Rejected.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("recheckstatus").in(recheckstatusList));
        return PageUtils.queryCount(queryConditionGroup, BillClaim.ENTITY_NAME);
    }

}
