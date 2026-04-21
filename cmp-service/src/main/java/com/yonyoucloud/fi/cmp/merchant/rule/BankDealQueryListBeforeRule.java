package com.yonyoucloud.fi.cmp.merchant.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.LocalDateUtil;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yonyou.ucf.mdd.ext.core.AppContext;


import javax.annotation.Resource;
import java.util.*;

/**
 * 银行交易明细列表加载 筛选是否需要同步客商rule
 * @author miaowb
 *
 */
@Slf4j
@Component
public class BankDealQueryListBeforeRule extends AbstractCommonRule {

    private static final String AUTHORUSEACCENTITY = "authoruseaccentity";
    private static final String ACCENTITY = "accentity";
    private static final String ENTERPRISE_BANKACCOUNT = "enterpriseBankAccount";
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    CmCommonService cmCommonService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();
        List<String> orgList = new ArrayList<>();
        List<String> accentityList = new ArrayList<>();
        Boolean flag = false;
        boolean hasBankAccountFilter = false;
        Object cashDirectLink = "";
        Object enterCountry = "";
        List<String> classValueList = null;
        List<String> bankAccountList = new ArrayList<>();
        String date = LocalDateUtil.getNowDateString();
        if (null != filterVO) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            for (FilterCommonVO vo : commonVOs) {
                switch (vo.getItemName()) {
                    case AUTHORUSEACCENTITY:
                        // 删除授权使用组织查询条件
                        if (vo.getValue1() != null) {
                            if (vo.getValue1() instanceof List) {
                                orgList.addAll((List) vo.getValue1());
                            }
                            //选择默认组织时，会变成单选
                            if (vo.getValue1() instanceof String) {
                                orgList.add((String) vo.getValue1());
                            }
                        }
                        vo.setValue1("");
                        vo.setItemName("");
                        break;
                    case ACCENTITY:
                        // 删除所属组织查询条件
                        if (vo.getValue1() != null) {
                            if (vo.getValue1() instanceof List) {
                                accentityList.addAll((List) vo.getValue1());
                            }
                            if (vo.getValue1() instanceof String) {
                                accentityList.add((String) vo.getValue1());
                            }
                        }
                        vo.setValue1("");
                        vo.setItemName("");
                        break;
                    case ENTERPRISE_BANKACCOUNT:
                        // 企业银行账户
                        if (vo.getValue1() != null) {
                            if (vo.getValue1() instanceof List) {
                                bankAccountList.addAll((List) vo.getValue1());
                            }
                            if (vo.getValue1() instanceof String) {
                                bankAccountList.add((String) vo.getValue1());
                            }
                        }
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
            //CZFW-528712 【DSP支持问题】老师你好，销户后的账户，无法查询到交易流水，麻烦老师看下这个bug。没有账户条件时，才走组织权限；否则查不到停用账户的数据
            if (CollectionUtils.isEmpty(bankAccountList)) {
                if(orgList.size() > 0 && accentityList.size() > 0){
                    List<String> accounts = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(orgList)) {
                        accounts.addAll(enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(new ArrayList<>(orgList)));
                    }
                    QueryConditionGroup conditionGroupAuth = new QueryConditionGroup(ConditionOperator.or);//授权使用组织过滤
                    orgList.addAll(accentityList);
                    QueryCondition queryConditionAuth = null;
                    if (flag) {
                        List<String> bankAcctInfos = cmCommonService.getBankAcctInfos(orgList, classValueList, enterCountry, cashDirectLink);
                        if (CollectionUtils.isNotEmpty(bankAcctInfos)) {
                            List<String> intersectionAccount = (List<String>) CollectionUtils.intersection(accounts, bankAcctInfos);
                            if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isNotEmpty(bankAccountList)) {
                                List<String> finalAccount = (List<String>) CollectionUtils.intersection(intersectionAccount, bankAccountList);
                                queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.in, finalAccount);
                            } else if (CollectionUtils.isNotEmpty(intersectionAccount) && CollectionUtils.isEmpty(bankAccountList)) {
                                queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.in, intersectionAccount);
                            } else if (CollectionUtils.isEmpty(intersectionAccount)) {
                                queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.eq, 0);
                            }
                        } else {
                            queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.eq, 0);
                        }
                    } else if (!flag) {
                        queryConditionAuth = new QueryCondition("enterpriseBankAccount", ConditionOperator.in, accounts);
                    }
                    conditionGroupAuth.addCondition(queryConditionAuth);
                    QueryCondition queryConditionOwnOrgid = new QueryCondition("accentity", ConditionOperator.in, accentityList);
                    conditionGroupAuth.addCondition(queryConditionOwnOrgid);
                    filterVO.setQueryConditionGroup(conditionGroupAuth);
                } else if(orgList.size() > 0 && accentityList.size() < 1){
                    List<String> accounts = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(orgList)) {
                        accounts.addAll(enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(new ArrayList<>(orgList)));
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
                    Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.DLLIST);
                    orgList = new ArrayList<>(orgSet);
                    List<String> accounts = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(orgList)) {
                        accounts.addAll(enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(new ArrayList<>(orgList)));
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
            }
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
