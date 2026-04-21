package com.yonyoucloud.fi.cmp.accounthistorybalance.rule;

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
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * refer - 参照前规则
 * 银行账户历史余额过滤企业银行内部户
 */
@Component("historyBalanceReferRule")
public class HistoryBalanceReferRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    BaseRefRpcService baseRefRpcService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && ("cmp_hisbalist".equals(billnum) || "cmp_gethisba".equals(billnum) || "cmp_hisba".equals(billnum))) {
            if (!"filter".equals(billDataDto.getExternalData())) {//列表查询区的银行账户，不用过滤是否启动停用
                //acctopentype，1为内部户，0为银行账户
//                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("acctopentype", ICmpConstant.QUERY_EQ, "0"));
                if (billDataDto.getCondition() == null) {
                    billDataDto.setCondition(new FilterVO());
                }
                if (null != billDataDto.getData() && ((List) billDataDto.getData()).size() > 0) {
                    String banktype = (String) ((Map) ((List) billDataDto.getData()).get(0)).get("banktype");
                    if (!StringUtils.isEmpty(banktype)) {
                        //根据银行类别，过滤银行账户
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bank", ICmpConstant.QUERY_EQ, banktype));
                    }
                    String accentity = (String) ((Map) ((List) billDataDto.getData()).get(0)).get("accentity");
                    if (!StringUtils.isEmpty(accentity) && !"cmp_hisba".equals(billnum)) {
                        //根据银行类别，过滤银行账户
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_EQ, accentity));
                    }
                }
                if ("cmp_hisba".equals(billnum) && !Objects.isNull(billDataDto.getCondition())) {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.ACCHISBAL);
                    EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                    newEnterpriseParams.setOrgidList(new ArrayList<>(orgs));
                    newEnterpriseParams.setEnables(Arrays.asList(1, 2));
                    List<String> accounts = new ArrayList<>();
                    List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = EnterpriseBankQueryService.queryAllWithRange(newEnterpriseParams);
                    for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                        accounts.add(enterpriseBankAcctVO.getId());
                    }
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                }
            } else {
                FilterVO filterVO = billDataDto.getCondition();
                if (null != filterVO) {
                    SimpleFilterVO[] simpleVOs = filterVO.getSimpleVOs();
                    if (null != simpleVOs) {
                        SimpleFilterVO[] commonVOs = simpleVOs;
                        Boolean hasOrgid = false;
                        Boolean hasAccentity = false;
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
                        /*List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                        List<String> accountByOrg = enterpriseBankQueryService.getAccounts(orgList);
                        accountByAccentity.addAll(accountByOrg);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));*/
                            accentityList.addAll(orgList);
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, accentityList));
                        } else if (!hasOrgid && hasAccentity) {
                            for (SimpleFilterVO vo : commonVOs) {
                                if (vo.getField() == null) {
                                    vo.setConditions(null);
                                }
                            }
                        /*List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));*/
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, accentityList));
                        } else if (hasOrgid && !hasAccentity) {
                            for (SimpleFilterVO vo : commonVOs) {
                                if (vo.getField() == null) {
                                    vo.setConditions(null);
                                }
                            }
                        /*List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(orgList);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));*/
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, orgList));
                        } else {
                            // 获取授权使用组织
                            Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.RETIBALIST);
                        /*List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));*/
                            List<String> filterOrgs = new ArrayList<>();
                            filterOrgs.addAll(orgs);
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_IN, filterOrgs));
                        }
                    }
                }
            }
        }
        if (IRefCodeConstant.FUNDS_ORGTREE.equals(billDataDto.getrefCode()) && "cmp_hisba".equals(billnum) && "accentity".equals(billDataDto.getKey())) {
            // 获取授权使用组织
            Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.ACCHISBAL);
            if (billDataDto.getCondition() == null) {
                billDataDto.setCondition(new FilterVO());
            }
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, orgs));
        }
        if ("ucfbasedoc.bd_bankcardref".equals(billDataDto.getrefCode()) && "cmp_gethisba".equals(billnum)) {
            //银行类别，参照过滤
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, "1"));
            } else {
                filterVO = new FilterVO();
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, "1"));
                billDataDto.setCondition(filterVO);
            }

        }
        return new RuleExecuteResult();
    }


}
