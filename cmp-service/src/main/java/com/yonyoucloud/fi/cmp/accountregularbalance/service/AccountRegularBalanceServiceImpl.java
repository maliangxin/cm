package com.yonyoucloud.fi.cmp.accountregularbalance.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountfixedbalance.AccountFixedBalance;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class AccountRegularBalanceServiceImpl implements AccountRegularBalanceService {

    @Override
    public CtmJSONObject confirmAccountBalance(List<AccountFixedBalance> billList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if(billList == null || billList.size() == 0){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<AccountFixedBalance> successList = new ArrayList<>();
        for(AccountFixedBalance accountFixedBalance : billList){
            Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountFixedBalance.getEnterpriseBankAccount());
            if (bankAccount == null) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00188", "银行账户不合法！请检查银行账户:") /* "银行账户不合法！请检查银行账户:" */ + accountFixedBalance.getEnterpriseBankAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00187", "是否存在！") /* "是否存在！" */);
                i++;
                continue;
            }
            //如果已确认（即已确认=“是”），则提示“账户XXX余额日期XXX的余额已确认！”
            if (accountFixedBalance.getIsconfirm() != null && accountFixedBalance.getIsconfirm()) {
                failed.put(accountFixedBalance.getId().toString(), accountFixedBalance.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018E", "银行账户：[%s]余额日期：[%s]的余额已确认！") /* "银行账户：[%s]余额日期：[%s]的余额已确认！" */,bankAccount.get("name"),new SimpleDateFormat("yyyy-MM-dd").format(accountFixedBalance.getBalancedate())));
                i++;
                continue;
            }
            accountFixedBalance.setIsconfirm(Boolean.TRUE);
            accountFixedBalance.setBalanceconfirmerid(AppContext.getUserId());
            accountFixedBalance.setBalanceconfirmtime(new Date());
            EntityTool.setUpdateStatus(accountFixedBalance);
            successList.add(accountFixedBalance);
        }
        MetaDaoHelper.update(AccountFixedBalance.ENTITY_NAME, successList);
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！") /* "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！" */,billList.size(),(billList.size() - i),i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", billList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject cancelConfirmAccountBalance(List<AccountFixedBalance> billList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if(billList == null || billList.size() == 0){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        for(AccountFixedBalance accountFixedBalance : billList){
            Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountFixedBalance.getEnterpriseBankAccount());
            if(accountFixedBalance.getIsconfirm() != null && !accountFixedBalance.getIsconfirm()){
                failed.put(accountFixedBalance.getId().toString(), accountFixedBalance.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018C", "银行账户：") /* "银行账户：" */+bankAccount.get("name")+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018B", "余额日期") /* "余额日期" */
                        +new SimpleDateFormat("yyyy-MM-dd").format(accountFixedBalance.getBalancedate())+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00190", "余额未确认，不允许取消确认！") /* "余额未确认，不允许取消确认！" */);
                i++;
                continue;
            }
            accountFixedBalance.setIsconfirm(Boolean.FALSE);
            accountFixedBalance.setBalanceconfirmerid(null);
            accountFixedBalance.setBalanceconfirmtime(null);
            EntityTool.setUpdateStatus(accountFixedBalance);
        }
        MetaDaoHelper.update(AccountFixedBalance.ENTITY_NAME, billList);
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！") /* "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！" */,billList.size(),(billList.size() - i),i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", billList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

}


