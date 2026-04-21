package com.yonyoucloud.fi.cmp.workbench.flowbench.business;

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
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component("flowBenchBankaccountFilterRule")
@Slf4j
public class FlowBenchBankaccountFilterRule extends AbstractCommonRule {
    private static final String FLOW_PROCESSING_WORKBENCH = "treasury_flowprocessing_workbench";
    @Autowired
    OrgDataPermissionService orgDataPermissionService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        String billnum = bill.getBillnum();
        FilterVO filterVO = bill.getCondition();
        if (FLOW_PROCESSING_WORKBENCH.equals(billnum)) {
            //
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
                    if ("orgid".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                        hasOrgid = true;
                        orgList = (List) vo.getValue1();
                        vo.setField(null);
                        vo.setValue1(null);
                    }
                    if ("accentity".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                        hasAccentity = true;
                        accentityList = (List) vo.getValue1();
                        vo.setField(null);
                        vo.setValue1(null);
                    }
                }
                if (hasOrgid && hasAccentity) {
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                    List<String> accountByOrg = enterpriseBankQueryService.getAccounts(accentityList);
                    accountByAccentity.addAll(accountByOrg);
                    bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else if (hasOrgid && !hasAccentity) {
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(orgList);
                    bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else if (!hasOrgid && hasAccentity) {
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(accentityList);
                    bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(FLOW_PROCESSING_WORKBENCH);
                    List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                    bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                }
            }

        }
        if (null == filterVO) {
            filterVO = new FilterVO();
        }
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode())) {
            if (bill.getData() != null) {
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
            }
        }
        bill.setCondition(filterVO);
        putParam(paramMap, bill);
        return new RuleExecuteResult();
    }
}
