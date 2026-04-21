package com.yonyoucloud.fi.cmp.initdata.rule;

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
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * refer - 参照前规则
 * 银行账户历史余额过滤企业银行内部户
 *
 */
@Component("initDataReferRule")
public class InitDataReferRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && "cmp_initdatayh".equals(billnum)) {
//             获取授权使用组织
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                Boolean hasOrgid = false;
                Boolean hasAccentity = false;
                List<String> accentityList = new ArrayList<>();
                List<String> orgList = new ArrayList<>();
                for (SimpleFilterVO vo : commonVOs) {
                    if("orgid".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                        hasOrgid = true;
                        orgList = (List)vo.getValue1();
                        vo.setField(null);
                        vo.setValue1(null);
                    }
                    if(vo.getValue1() instanceof List){
                        if("accentity".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                            hasAccentity = true;
                            accentityList = (List)vo.getValue1();
                            vo.setField(null);
                            vo.setValue1(null);
                        }
                    }else{
                        if("accentity".equals(vo.getField()) && vo.getValue1()!=null){
                            hasAccentity = true;
                            accentityList.add(vo.getValue1().toString());
                            vo.setField(null);
                            vo.setValue1(null);
                        }
                    }
                }
                if(hasOrgid && hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                    List<String> accountByOrg = enterpriseBankQueryService.getAccounts(orgList);
                    accountByAccentity.addAll(accountByOrg);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else if(!hasOrgid && hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else if(hasOrgid && !hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(orgList);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.RETIBALIST);
                    List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                }
            }
        }

        // 授权使用组织 参照过滤
        if (billDataDto.getrefCode() != null && IRefCodeConstant.FUNDS_ORG_ADN_FINANCE_ORG_LIST.contains(billDataDto.getrefCode()) && "cmp_initdatayhlist".equals(billnum) && "accentity".equals(billDataDto.getKey())) {
                // 获取授权使用组织
            Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BANKINITDATA);
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, orgs));
        }

        return new RuleExecuteResult();
    }
}
