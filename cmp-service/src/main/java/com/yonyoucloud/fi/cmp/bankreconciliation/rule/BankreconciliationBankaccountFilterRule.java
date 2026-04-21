package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("bankreconciliationBankaccountFilterRule")
@RequiredArgsConstructor
public class BankreconciliationBankaccountFilterRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        String billnum = billContext.getBillnum();
        FilterVO filterVO = bill.getCondition();
        boolean flag = IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode()) &&
                ("cmp_bankreconciliationlist".equals(billnum) || "cmp_bankreconciliation".equals(billnum) || "cmp_bankreconciliation_repeat_list".equals(billnum));
        if (flag) {
            if (null != filterVO) {
                SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                if (commonVOs == null || commonVOs.length == 0) {
                    commonVOs = new SimpleFilterVO[]{};
                }
                Boolean hasOrgid = false; // 所属组织
                Boolean hasAccentity = false; // 授权使用组织
                List<String> accentityList = new ArrayList<>();
                List<String> orgList = new ArrayList<>();
                for (SimpleFilterVO vo : commonVOs) {
                    if("orgid".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                        hasOrgid = true;
                        orgList = (List)vo.getValue1();
                        vo.setField(null);
                        vo.setValue1(null);
                    }
                    if("accentity".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                        hasAccentity = true;
                        accentityList = (List)vo.getValue1();
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
                    /*List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                    List<String> accountByOrg = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(accentityList);
                    accountByAccentity.addAll(accountByOrg);
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));*/
                    accentityList.addAll(orgList);
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, accentityList));
                } else if(hasOrgid && !hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    /*List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));*/
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, orgList));
                } else if(!hasOrgid && hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    /*List<String> accountByAccentity = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(accentityList);
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));*/
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, accentityList));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.CMPBANKRECONCILIATION);
                    /*List<String> accounts = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(new ArrayList<>(orgs));
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));*/
                    List<String> filterOrgs = new ArrayList<>();
                    filterOrgs.addAll(orgs);
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, filterOrgs));
                }
            }
        }
        if(null == filterVO){
            filterVO = new FilterVO();
        }
        //去掉币种子表启用的筛选条件，和其他节点保持一致，否则查不到销户的账户——王东方老师已确认
//        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode())){
//			if (bill.getData()!=null) {
////                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
////                if (!StringUtils.isEmpty(currency)) {
////                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, currency));
////                }
//                //币种子表启用
//                bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
//            }
//		}
        bill.setCondition(filterVO);
        putParam(paramMap, bill);
        return new RuleExecuteResult();
    }
}
