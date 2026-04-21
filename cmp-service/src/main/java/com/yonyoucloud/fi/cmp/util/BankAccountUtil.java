package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2026年02月05日 13:52
 * @Description:
 */
@Slf4j
public class BankAccountUtil {
    public static Map<String, Date> account_enableDate_map = new HashMap<>();
    public static Date getEnableDate(String account) throws Exception {
        Date enableDate = null;
        if (account_enableDate_map.containsKey(account)) {
            return account_enableDate_map.get(account);
        }
        QuerySchema schema = QuerySchema.create().addSelect("enableDate");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount.account").eq(account));
        schema.addCondition(conditionGroup);
        List<BankAccountSetting> settings = CmpMetaDaoHelper.queryDTOList(BankAccountSetting.ENTITY_NAME, schema);
        if (settings != null) {
            if (settings.size() == 0) {
                log.error("account:{} not found", account);
            } else if (settings.size() > 0){
                if (settings.size() > 1) {
                    log.error("account:{} has more than one setting", account);
                }
                BankAccountSetting bankAccountSetting = settings.get(0);
                enableDate = bankAccountSetting.getEnableDate();
                if (enableDate != null) {
                    account_enableDate_map.put(account, enableDate);
                    return enableDate;
                }
            }
        } else {
            log.error("account:{} not found", account);
        }
        return null;
    }

    public static void refreshEnableDateByEnterpriseBankAcctVOs(List<EnterpriseBankAcctVO> acctVOS) throws Exception {
        if (CollectionUtils.isNotEmpty(acctVOS)) {
            List<String> accountIds = acctVOS.stream()
                    .filter(Objects::nonNull)
                    .filter(acctVO -> acctVO.getId() != null)
                    .map(acctVO -> acctVO.getId().toString())
                    .collect(Collectors.toList());
            refreshEnableDate(accountIds);
        }
    }

    public static void refreshEnableDate(List<String> accountId) throws Exception {
        if (CollectionUtils.isEmpty(accountId)) {
            return;
        }
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount, enterpriseBankAccount.account as account, enableDate");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(accountId));
        //conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        //conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        List<BankAccountSetting> settings = CmpMetaDaoHelper.queryDTOList(BankAccountSetting.ENTITY_NAME, schema);
        if (settings != null && settings.size() > 0) {
            for (int i = 0; i < settings.size(); i++) {
                BankAccountSetting bankAccountSetting = settings.get(i);
                if (bankAccountSetting.get("account") != null && bankAccountSetting.getEnableDate() != null) {
                    account_enableDate_map.put(bankAccountSetting.get("account").toString(), bankAccountSetting.getEnableDate());
                }
            }
        }

    }

}
