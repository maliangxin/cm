package com.yonyoucloud.fi.cmp.bankbillcheck.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @Author zhucongcong
 * @Date 2024/9/25
 */
@Slf4j
@Component("bankBillCheckRefRule")
public class BankBillCheckRefRule extends AbstractCommonRule {

    @Resource
    private OrgDataPermissionService orgDataPermissionService;

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private BankAccountSettingService bankaccountSettingService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("===============================================进入银企对账直联确认参照规则===============================================");
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && "cmp_bankbillchecklist".equals(billnum)) {
            // 获取授权使用组织
            FilterVO filterVO = billDataDto.getCondition();
            if (null != filterVO) {
                SimpleFilterVO[] commonVOs = filterVO.getSimpleVOs();
                Boolean hasOrgid = false;
                Boolean hasAccentity = false;
                List<String> accentityList = new ArrayList<>();
                List<String> orgList = new ArrayList<>();
                List<String> currencyList = new ArrayList<>();
                for (SimpleFilterVO vo : commonVOs) {

                    if ("currency".equals(vo.getField()) && ((List) vo.getValue1()).size() > 0) {
                        currencyList = (List) vo.getValue1();
                        vo.setField(null);
                        vo.setValue1(null);
                    }
                    /*if ("bank".equals(vo.getField()) && ((List)vo.getValue1()).size() > 0){
                        List<String> bankList = (List)vo.getValue1();
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("bank", ICmpConstant.QUERY_IN, bankList));
                    }*/

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
                    List<String> accountByOrg = enterpriseBankQueryService.getAccounts(orgList);
                    accountByAccentity.addAll(accountByOrg);
                    //过滤未开通银企连的账号
                    List<String> accountIds = filterNoOpenFlag(accountByAccentity);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountIds));
                } else if(!hasOrgid && hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccountsByAccentity(accentityList);
                    //过滤未开通银企连的账号
                    List<String> accountIds = filterNoOpenFlag(accountByAccentity);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountIds));
                } else if(hasOrgid && !hasAccentity){
                    for (SimpleFilterVO vo : commonVOs) {
                        if (vo.getField() == null) {
                            vo.setConditions(null);
                        }
                    }
                    List<String> accountByAccentity = enterpriseBankQueryService.getAccounts(orgList);
                    //过滤未开通银企连的账号
                    List<String> accountIds = filterNoOpenFlag(accountByAccentity);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountIds));
                } else {
                    // 获取授权使用组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BANK_BILL_CHECK);
                    List<String> accounts = enterpriseBankQueryService.getAccounts(orgs);
                    //过滤未开通银企连的账号
                    List<String> accountIds = filterNoOpenFlag(accounts);
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountIds));
                }
                //该参照是主子表展示，如果不拼币种条件会展示出该账户下所有的币种数据
                if (CollectionUtils.isNotEmpty(currencyList)) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", ICmpConstant.QUERY_IN, currencyList));
                }
            }
        }

        return new RuleExecuteResult();
    }

    /**
     * 过滤出开通银企直联的账号
     * @param accountByAccentity
     * @throws Exception
     */
    /*private List<String> filterNoOpenFlag(List<String> accountByAccentity) throws Exception {
        //查出未开通银企直联的账号
        List<Map<String, Object>> bankAccountSettingList = bankaccountSettingService.listBankAccountSettingByOpenFlag(false);
        List<String> bankAccountList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankAccountSettingList)) {
            for (Map<String, Object> bankAccountSetting : bankAccountSettingList) {
                if (bankAccountSetting.get("enterpriseBankAccount") != null) {
                    bankAccountList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
                }
            }
            //过滤掉未开通银企联的账号
            accountByAccentity.removeAll(bankAccountList);
        }
        if (CollectionUtils.isEmpty(accountByAccentity)) {
            //防止条件为空查出所有银行账号
            accountByAccentity.add("0");
        }
        return accountByAccentity;
    }*/

    /**
     * 过滤出开通银企直联的账号
     * @param accountByAccentity
     * @throws Exception
     */
    private List<String> filterNoOpenFlag(List<String> accountByAccentity) throws Exception {
        //查出开通银企直联的账号
        List<Map<String, Object>> openFlagAccount = getOpenFlagAccount(accountByAccentity);
        List<String> accountIds = new ArrayList<>();
        if (CollectionUtils.isEmpty(openFlagAccount)) {
            accountIds.add("0");
            return accountIds;
        }

        for (Map<String, Object> map : openFlagAccount) {
            if (map.get("enterpriseBankAccount") != null) {
                accountIds.add(map.get("enterpriseBankAccount").toString());
            }
        }
        return accountIds;
    }

    /**
     * 查询开通银企直联的账号
     * @param enterpriseBankAccounts
     * @throws Exception
     */
    private List<Map<String, Object>> getOpenFlagAccount(List<String> enterpriseBankAccounts) throws Exception {
        List<Map<String, Object>> retData = new ArrayList<>();
        int batchcount = 200;
        int listSize = enterpriseBankAccounts.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount) + 1);
        for (int i = 0; i < totalTask; i++) {
            int fromIndex = i * batchcount;
            int toIndex = i * batchcount + batchcount;
            if (i + 1 == totalTask) {
                toIndex = listSize;
            }
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts.subList(fromIndex, toIndex)));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> tmpData = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
            retData.addAll(tmpData);
        }
        return retData;
    }
}
