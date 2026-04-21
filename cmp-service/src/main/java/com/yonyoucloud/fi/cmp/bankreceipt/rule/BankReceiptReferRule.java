package com.yonyoucloud.fi.cmp.bankreceipt.rule;

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
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * refer - 参照前规则
 * 电子回单、银行电子对账单参照规则
 */
@Component("bankReceiptReferRule")
public class BankReceiptReferRule extends AbstractCommonRule {

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        List<String> billnumList = Arrays.asList(
                IBillNumConstant.CMP_BANKELECTRONICRECEIPTLIST,
                IBillNumConstant.CMP_BANKELECTRONICRECEIPT,
                IBillNumConstant.ELECTRONICSTATEMENTCONFIRMLIST
        );
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && (billnumList.contains(billnum))) {
//             获取授权使用组织
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                SimpleFilterVO[] simpleVOs = filterVO.getSimpleVOs();
                if (null != simpleVOs) {
                    SimpleFilterVO[] commonVOs = simpleVOs;
                    Boolean hasOrgid = false;
                    Boolean hasAccentity = false;
                    List<String> accentityList = new ArrayList<>();
                    List<String> orgList = new ArrayList<>();
                    List<String> bankList = new ArrayList<>();
                    List<String> currencyList = new ArrayList<>();
                    for (SimpleFilterVO vo : commonVOs) {
                        if ("currency".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                            currencyList = (List) vo.getValue1();
                            continue;
                        }
                        if ("bank".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                            bankList = (List) vo.getValue1();
                            continue;
                        }
                        if ("orgid".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                            hasOrgid = true;
                            orgList = (List) vo.getValue1();
                            vo.setField(null);
                            vo.setValue1(null);
                        }
                        if ("accentity".equals(vo.getField()) && vo.getValue1() != null) {
                            if (vo.getValue1() instanceof List) {
                                accentityList = (List) vo.getValue1();
                            } else {
                                accentityList.add(vo.getValue1().toString());
                            }
                            if (CollectionUtils.isNotEmpty(accentityList)) {
                                hasAccentity = true;
                                vo.setField(null);
                                vo.setValue1(null);
                            }
                        }
                    }
                    if (hasOrgid && hasAccentity) {
                        for (SimpleFilterVO vo : commonVOs) {
                            if (vo.getField() == null) {
                                vo.setConditions(null);
                            }
                        }
                        List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                        List<String> accountByOrg = enterpriseBankQueryService.getAccounts(orgList);
                        accountByAccentity.addAll(accountByOrg);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                    } else if (!hasOrgid && hasAccentity) {
                        for (SimpleFilterVO vo : commonVOs) {
                            if (vo.getField() == null) {
                                vo.setConditions(null);
                            }
                        }
                        List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                    } else if (hasOrgid && !hasAccentity) {
                        for (SimpleFilterVO vo : commonVOs) {
                            if (vo.getField() == null) {
                                vo.setConditions(null);
                            }
                        }
                        List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(orgList);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountByAccentity));
                    } else {
                        // 获取授权使用组织
                        Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.RETIBALIST);
                        List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accounts));
                    }
                    //该参照是主子表展示，如果不拼币种条件会展示出该账户下所有的币种数据
                    if (CollectionUtils.isNotEmpty(currencyList)) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", ICmpConstant.QUERY_IN, currencyList));
                    }
                    //银行类别改由前端做过滤
                    //if (CollectionUtils.isNotEmpty(bankList)) {
                    //    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankNumber.bank.id", ICmpConstant.QUERY_IN, bankList));
                    //}
                }
            }
        }
        return new RuleExecuteResult();
    }

}
