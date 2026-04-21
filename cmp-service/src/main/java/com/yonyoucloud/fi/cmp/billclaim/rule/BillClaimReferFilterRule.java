package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.data.service.itf.BizDelegateApi;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.util.Lists;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description: 到账认领中心 参照过滤
 */
@Slf4j
@Component
public class BillClaimReferFilterRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BizDelegateApi bizDelegateApi;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billContext.getBillnum();
        //对方银行账号
        String currency = null;
        String accentity = null;

        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && ("cmp_billclaimcenterlist".equals(billnum) || "cmp_billclaimcenter".equals(billnum))) {
//             获取授权使用组织
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs() == null ? new SimpleFilterVO[0] : filterVO.getSimpleVOs();
                Boolean hasOrgid = false; // 所属组织
                Boolean hasAccentity = false; // 授权使用组织
                List<String> accentityList = new ArrayList<>();
                List<String> orgList = new ArrayList<>();
                for (SimpleFilterVO vo : commonVOs) {
                    boolean notnull=vo.getValue1() instanceof String && ((String)vo.getValue1()).length() > 0;
                    boolean nonull=vo.getValue1() instanceof List && ((List)vo.getValue1()).size() > 0;
                    if("orgid".equals(vo.getField()) && (notnull||nonull)){
                        hasOrgid = true;
                        if(vo.getValue1() instanceof String){
                            orgList.add(vo.getValue1().toString());
                        }
                        if(vo.getValue1() instanceof List){
                            orgList = (List)vo.getValue1();
                        }
                        vo.setField(null);
                        vo.setValue1(null);
                    }

                    if("accentity".equals(vo.getField()) && (notnull||nonull)){
                        hasAccentity = true;
                        if(vo.getValue1() instanceof String){
                            accentityList.add(vo.getValue1().toString());
                        }
                        if(vo.getValue1() instanceof List){
                            accentityList = (List)vo.getValue1();
                        }
                        vo.setField(null);
                        vo.setValue1(null);
                    }
                }
                if(hasOrgid && hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                    List<String> accountByOrg = enterpriseBankQueryService.getAccounts(accentityList);
                    accountByAccentity.addAll(accountByOrg);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else if(hasOrgid && !hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else if(!hasOrgid && hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(accentityList);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BILLCLAIMCARD);
                    List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                }
            }
        }
        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isEmpty(bills)) {
            return new RuleExecuteResult();
        }

        currency = bills.get(0).getString("currency");
        accentity = bills.get(0).getString("accentity");

        //对方单位再过滤
        if ("oppositeobjectname".equals(billDataDto.getDatasource())) {
            String referCode = billDataDto.getrefCode();
            //客户参照
            if ("productcenter.aa_merchantref".equals(referCode)) {
                Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
                billDataDto.getCondition().appendCondition(ConditionOperator.and,
                        new SimpleFilterVO("merchantAppliedDetail.merchantApplyRangeId.orgId", ICmpConstant.QUERY_IN,orgSet.toArray()));
//                billDataDto.getCondition().appendCondition(ConditionOperator.and,
//                        new SimpleFilterVO("merchantAgentFinancialInfos.currency", ICmpConstant.QUERY_IN,currency));

            }
            //供应商
//            if (IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(referCode)) {
//                Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
                // CZFW-169794供应商参照报错问题
//                billDataDto.getCondition().appendCondition(ConditionOperator.and,
//                        new SimpleFilterVO("vendorOrgs.org", ICmpConstant.QUERY_IN,orgSet.toArray()));
//
//                billDataDto.getCondition().appendCondition(ConditionOperator.and,
//                        new SimpleFilterVO("vendorbanks.currency", ICmpConstant.QUERY_IN,currency));
//            }
        }

        BillDataDto bill = (BillDataDto) getParam(map);
        if (BillClaim.ENTITY_NAME.equals(bill.getFullname()) && "org_name".equals(bill.getKey()) && "ucf-org-center.aa_org".equals(bill.getrefCode())) {
            // 业务单元过滤
            if (bill.getExternalData() != null && CtmJSONObject.toJSON(bill.getExternalData()).getJSONArray("orgIds") != null) {
                // 批量认领时，bill.getExternalData()不为null，传递了所有的账户使用组织id(已去重)
                List<String> orgIds = CtmJSONObject.toJSON(bill.getExternalData()).getJSONArray("orgIds").toJavaList(String.class);
                // 采用orgIds组装业务单元查询参数，获取满足条件的业务单元id集合
                List<List<String>> orgList = new ArrayList<>();
                for (String orgId : orgIds) {
                    List<String> dataList = this.queryOrgIdListByUseOrgId(orgId);
                    // 查询后的数据将本组织作为业务单元带上，
                    dataList.add(orgId);
                    orgList.add(dataList);
                }
                // 取交集
                List<String> queryOrgIds = this.retainList(orgList);
                // 构建业务单元ids条件
                FilterVO filterVO = bill.getCondition();
                if(null == filterVO){
                    filterVO = new FilterVO();
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, queryOrgIds));
                bill.setCondition(filterVO);

                // 设置组织，用于组织授权校验
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) bill.getData();
                dataList.forEach(data -> data.put("accentity", orgIds));
            }
        }

        // 部门参照新增逻辑处理
        if (BillClaim.ENTITY_NAME.equals(bill.getFullname()) && "dept_name".equals(bill.getKey()) && "ucf-org-center.bd_adminorgsharetreeref".equals(bill.getrefCode())) {
            // 补充主组织
            List<BillClaim> billClaims = (List<BillClaim>) bill.getData();
            BillClaim billClaim = billClaims.get(0);
            if (this.checkIsResetAccentity(billClaim)) {
                // 重设主组织，部门参照主要根据该字段查询数据
                billClaim.setAccentity(billClaim.getOrg());
            }
        }

        return new RuleExecuteResult();
    }

    private boolean checkIsResetAccentity(BillClaim billClaim) {
        // 账户使用组织为空，直接返回true
        if (StringUtils.isEmpty(billClaim.getAccentity())) {
            return true;
        }

        // 账户使用组织与业务单元同时不为空，且2个值不相等时返回true
        return !StringUtils.isEmpty(billClaim.getOrg()) && !billClaim.getAccentity().equals(billClaim.getOrg());
    }

    private List<String> queryOrgIdListByUseOrgId(String orgId) {
        List<String> orgIds = Lists.newArrayList(orgId);
        String tenantId = InvocationInfoProxy.getTenantid();
        List<String> orgList = bizDelegateApi.listAccountingDelegateOrgUnitIdsByFinanceOrgIdList(orgIds, tenantId, "diwork");
        return orgList == null ? new ArrayList<>() : orgList;
    }

    private List<String> retainList(List<List<String>> ids) {
        List<String> resIds = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(ids)){
            resIds = ids.get(0);
            for(int i = 1;i < ids.size();i++){
                resIds.retainAll(ids.get(i));
            }

            // 取交集为空的情况下，默认一个值，查询不到
            if(CollectionUtils.isEmpty(resIds)){
                resIds.add("-1");
            }
        }
        return resIds;
    }
}
