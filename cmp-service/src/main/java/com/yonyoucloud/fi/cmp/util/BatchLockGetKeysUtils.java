package com.yonyoucloud.fi.cmp.util;


import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyoucloud.fi.cmp.cashhttp.CashHttpBankEnterpriseLinkVo;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取批量锁的keys
 */
public class BatchLockGetKeysUtils {

    /**
     * 构建批量锁的keys
     * @param action 行为名称
     * @param httpBankAccounts 直联账户
     * @param innerAccounts 内部账户
     * @param clazz
     * @param methodName 构建key的get参数名
     * @return
     * @param <T>
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static <T> List<String> batchLockGetKeys(String action, List<T>  httpBankAccounts, List<T> innerAccounts, Class<T> clazz, String methodName) throws InvocationTargetException, IllegalAccessException {
        Method getAccountMethod = null;
        try {
            getAccountMethod = clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400733", "不存在%s方法！") /* "不存在%s方法！" */,methodName));
        }
        // 查询所有的账户信息+行为
        // 内部
        List<String> accountIdList=new ArrayList<>();
        if(!CollectionUtils.isEmpty(httpBankAccounts)){
            for(T item:httpBankAccounts){
                String key = (String) getAccountMethod.invoke(item) + action;
                if(accountIdList.contains(key)){
                    continue;
                }
                accountIdList.add(key);
            }
        }
        // 直联
        if(!CollectionUtils.isEmpty(innerAccounts)){
            for(T item:innerAccounts){
                String key = (String) getAccountMethod.invoke(item) + action;
                if(accountIdList.contains(key)){
                    continue;
                }
                accountIdList.add(key);
            }
        }
        return accountIdList;
    }

    /**
     * 构建批量锁的keys
     * @param action 行为名称
     * @param accountNos 账户
     * @return
     */
    public static List<String> batchLockGetKeys(String action, List<String>  accountNos){
        // 查询所有的账户信息+行为
        List<String> accountIdList=new ArrayList<>();
        if(!CollectionUtils.isEmpty(accountNos)){
            for(String accountNo:accountNos){
                String key = accountNo + action;
                if(accountIdList.contains(key)){
                    continue;
                }
                accountIdList.add(key);
            }
        }
        return accountIdList;
    }

    /**
     * 构建批量锁的keys
     * @param action 行为名称
     * @param accounts 账户信息
     * @return
     * @param <T>
     */
    public static <T> List<String> batchLockCombineKeys(String action, List<T>  accounts){
        List<String> accountIdList=new ArrayList<>();
        // 所有的银行账户+行为
        if(!CollectionUtils.isEmpty(accounts)){
            for (T account : accounts) {
                if (account instanceof EnterpriseBankAcctVO) {
                    EnterpriseBankAcctVO acctVO = (EnterpriseBankAcctVO) account;
                    String key = acctVO.getId()+action;
                    if (!accountIdList.contains(key)) {
                        accountIdList.add(key);
                    }
                } else if (account instanceof String) {
                    String key = account+action;
                    if (!accountIdList.contains(key)) {
                        accountIdList.add(key);
                    }
                }
            }
        }
        return accountIdList;
    }

    /**
     * 构建批量锁的keys
     * @param action 行为名称
     * @param accounts 账户信息
     * @return
     * @param <T>
     */
    public static <T> List<String> batchLockCombineKeysByCurrency(String action, List<T>  accounts){
        List<String> accountIdList=new ArrayList<>();
        // 所有的银行账户+行为
        if(!CollectionUtils.isEmpty(accounts)){
            for (T account : accounts) {
                if (account instanceof EnterpriseBankAcctVO) {
                    EnterpriseBankAcctVO acctVO = (EnterpriseBankAcctVO) account;
                    List<BankAcctCurrencyVO> currencyList = acctVO.getCurrencyList();
                    for(BankAcctCurrencyVO currencyVO : currencyList){
                        String key = acctVO.getAccount() + currencyVO.getId() + action;
                        if (!accountIdList.contains(key)) {
                            accountIdList.add(key);
                        }
                    }
                }
            }
        }
        return accountIdList;
    }
}
