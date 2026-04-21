package com.yonyoucloud.fi.cmp.bankdealdetail.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
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
@Component("bankDealDetailReferRule")
public class BankDealDetailReferRule extends AbstractCommonRule {

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
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && ("cmp_dllist".equals(billnum) || "cmp_merchant".equals(billnum))) {
//             获取授权使用组织
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                Boolean hasOrgid = false;
                Boolean hasAccentity = false;
                List<String> accentityList = new ArrayList<>();
                List<String> orgList = new ArrayList<>();
                // 防止为null
                if (commonVOs == null) {
                    commonVOs = new SimpleFilterVO[0];
                }
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
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                    List<String> accountByOrg = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(orgList);
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
                    List<String> accountByAccentity = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(orgList);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.RETIBALIST);
                    List<String> accounts = enterpriseBankQueryService.queryAccountIdsByOrgListWithRange(new ArrayList<>(orgs));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                }
            }
        }

        return new RuleExecuteResult();
    }

}
