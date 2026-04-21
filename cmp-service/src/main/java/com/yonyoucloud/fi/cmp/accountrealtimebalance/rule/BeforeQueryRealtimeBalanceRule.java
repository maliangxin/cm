package com.yonyoucloud.fi.cmp.accountrealtimebalance.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.LocalDateUtil;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component("beforeQueryRealtimeBalanceRule")
public class BeforeQueryRealtimeBalanceRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    private static final String ENTERPRISE_BANKACCOUNT = "enterpriseBankAccount";
    private static final String BALANCEDATE = "balancedate";
    private static final String AUTHORUSEACCENTITY = "authoruseaccentity";
    private static final String ACCENTITY = "accentity";

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CmCommonService cmCommonService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //List<BizObject> bills = getBills(billContext, paramMap);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();
        List<String> orgList = new ArrayList<>();
        List<String> accentityList = new ArrayList<>();
        List<String> bankAccountList = new ArrayList<>();
        Object cashDirectLink = "";
        Object enterCountry = "";
        List<String> classValueList = null;
        Boolean flag = false;
        String date = LocalDateUtil.getNowDateString();
        if (null != filterVO) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            for (FilterCommonVO vo : commonVOs) {
                switch (vo.getItemName()) {
                    case AUTHORUSEACCENTITY:
                        // 删除授权使用组织查询条件
                        orgList.addAll((List) vo.getValue1());
                        vo.setValue1("");
                        vo.setItemName("");
                        break;
                    case ACCENTITY:
                        // 删除所属组织查询条件
                        accentityList.addAll((List)vo.getValue1());
                        vo.setValue1("");
                        vo.setItemName("");
                        break;
                    case ENTERPRISE_BANKACCOUNT:
                        bankAccountList.addAll((List)vo.getValue1());
                        break;
                    case ICmpConstant.ENTERCOUNTRY:
                        flag = true;
                        enterCountry = vo.getValue1();
                        vo.setValue1(null);
                        break;
                    case ICmpConstant.CASHDIRECTLINK:
                        flag = true;
                        cashDirectLink = vo.getValue1();
                        vo.setValue1(null);
                        break;
                    case ICmpConstant.ACCTQUALITYCATEGORY:
                        flag = true;
                        classValueList = cmCommonService.getValueList(vo);
                        vo.setValue1(null);
                        break;
                    default:
                        break;
                }
            }
            QueryConditionGroup conditionGroupAuth = new QueryConditionGroup(ConditionOperator.or);//授权使用组织过滤
            if(orgList.size() > 0 && accentityList.size() > 0){
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(orgList);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                List<String> accounts = new ArrayList<>();
                for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                    accounts.add(enterpriseBankAcctVOWithRange.getId());
                }
                orgList.addAll(accentityList);
                QueryCondition queryConditionAuth = null;
                if (flag) {
                    List<String> bankAcctInfos = cmCommonService.getBankAcctInfos(orgList, classValueList, enterCountry, cashDirectLink);
                    if (CollectionUtils.isNotEmpty(bankAcctInfos)) {
                        List<String> intersectionAccount = (List<String>) CollectionUtils.intersection(accounts, bankAcctInfos);
                        if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isNotEmpty(bankAccountList)) {
                            List<String> finalAccount = (List<String>) CollectionUtils.intersection(intersectionAccount, bankAccountList);
                            if(CollectionUtils.isNotEmpty(finalAccount)){
                                queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.in, finalAccount);
                            } else {
                                queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.eq, 0);
                            }
                        } else if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isEmpty(bankAccountList)) {
                            queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.in, intersectionAccount);
                        } else if (CollectionUtils.isEmpty(intersectionAccount)) {
                            queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.eq, 0);
                        }
                    } else {
                        queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.eq, 0);
                    }
                } else if (!flag) {
                    if(CollectionUtils.isNotEmpty(accounts)){
                        queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.in, accounts);
                    } else {
                        queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.eq, 0);
                    }
                }
                conditionGroupAuth.addCondition(queryConditionAuth);
                QueryCondition queryConditionOwnOrgid = new QueryCondition("accentity", ConditionOperator.in, accentityList);
                conditionGroupAuth.addCondition(queryConditionOwnOrgid);
//                filterVO.setQueryConditionGroup(conditionGroupAuth);
            } else if(orgList.size() > 0 && accentityList.size() < 1){
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(orgList);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                List<String> accounts = new ArrayList<>();
                for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                    accounts.add(enterpriseBankAcctVOWithRange.getId());
                }
                if (flag) {
                    List<String> bankAcctInfos = cmCommonService.getBankAcctInfos(orgList, classValueList, enterCountry, cashDirectLink);
                    if (CollectionUtils.isNotEmpty(bankAcctInfos)) {
                        List<String> intersectionAccount = (List<String>) CollectionUtils.intersection(accounts, bankAcctInfos);
                        if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isNotEmpty(bankAccountList)) {
                            List<String> finalAccount = (List<String>) CollectionUtils.intersection(intersectionAccount, bankAccountList);
                            if(CollectionUtils.isNotEmpty(finalAccount)){
                                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, finalAccount));
                            } else {
                                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                            }
                        } else if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isEmpty(bankAccountList)) {
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, intersectionAccount));
                        } else if (CollectionUtils.isEmpty(intersectionAccount)) {
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                        }
                    } else {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                    }
                } else if (!flag) {
                    if(CollectionUtils.isNotEmpty(accounts)){
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, accounts));
                    } else {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                    }
                }
            } else if(orgList.size() < 1 && accentityList.size() > 0){
                if (flag) {
                    List<String> bankAcctInfos = cmCommonService.getBankAcctInfos(accentityList, classValueList, enterCountry, cashDirectLink);
                    if (CollectionUtils.isNotEmpty(bankAcctInfos) && CollectionUtils.isNotEmpty(bankAccountList)) {
                        List<String> finalAccount = (List<String>) CollectionUtils.intersection(bankAcctInfos, bankAccountList);
                        if(CollectionUtils.isNotEmpty(finalAccount)){
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, finalAccount));
                        } else {
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                        }
                    } else if (CollectionUtils.isNotEmpty(bankAcctInfos) && CollectionUtils.isEmpty(bankAccountList)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, bankAcctInfos));
                    } else if (CollectionUtils.isEmpty(bankAcctInfos)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                    }
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_IN, accentityList));
            } else if(orgList.size() < 1 && accentityList.size() < 1){
                Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.RETIBALIST);
                orgList = new ArrayList<>(orgSet);
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(orgList);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                List<String> accounts = new ArrayList<>();
                for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                    accounts.add(enterpriseBankAcctVOWithRange.getId());
                }
                if (flag) {
                    List<String> bankAcctInfos = cmCommonService.getBankAcctInfos(orgList, classValueList, enterCountry, cashDirectLink);
                    if (CollectionUtils.isNotEmpty(bankAcctInfos)) {
                        List<String> intersectionAccount = (List<String>) CollectionUtils.intersection(accounts, bankAcctInfos);
                        if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isNotEmpty(bankAccountList)) {
                            List<String> finalAccount = (List<String>) CollectionUtils.intersection(intersectionAccount, bankAccountList);
                            if(CollectionUtils.isNotEmpty(finalAccount)){
                                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, finalAccount));
                            } else {
                                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                            }
                        } else if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isEmpty(bankAccountList)) {
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, intersectionAccount));
                        } else if (CollectionUtils.isEmpty(intersectionAccount)) {
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                        }
                    } else {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                    }
                } else if (!flag) {
                    if(CollectionUtils.isNotEmpty(accounts)){
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_IN, accounts));
                    } else {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enterpriseBankAccount", ICmpConstant.QUERY_EQ, 0));
                    }
                }
            }
//            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("balancedate", ICmpConstant.QUERY_EGT, date));
            QueryConditionGroup conditionGroup = filterVO.getQueryConditionGroup().and(QueryCondition.name(BALANCEDATE).eq(date));
            conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
            QueryConditionGroup mainConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            mainConditionGroup.addCondition(conditionGroupAuth);
            mainConditionGroup.addCondition(conditionGroup);
            filterVO.setQueryConditionGroup(mainConditionGroup);
        }
        return new RuleExecuteResult();
    }
}
