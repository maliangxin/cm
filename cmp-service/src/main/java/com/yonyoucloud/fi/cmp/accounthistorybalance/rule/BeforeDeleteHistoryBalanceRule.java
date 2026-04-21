package com.yonyoucloud.fi.cmp.accounthistorybalance.rule;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceServiceImpl;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("beforeDeleteHistoryBalanceRule")
public class BeforeDeleteHistoryBalanceRule extends AbstractCommonRule {
    @Autowired
    private AccountHistoryBalanceServiceImpl accountHistoryBalanceServiceImpl;

    /**
     * 删除前规则
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            //加锁
            String lockkey = ICmpConstant.MY_BILL_CLAIM_LIST + bizObject.get("id");
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTraceByTime(lockkey,5);
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101863"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1808B26A04D00001", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            try {
                AccountRealtimeBalance accountRealtimeBalance = MetaDaoHelper.findById(AccountRealtimeBalance.ENTITY_NAME, bizObject.getId());
                //获取银企联账户信息
                Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountRealtimeBalance.getEnterpriseBankAccount());
                if(BalanceFlag.AutoPull.getCode().equals(accountRealtimeBalance.getFlag())){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101864"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180005","该数据为银行同步数据，不允许删除。") /* "该数据为银行同步数据，不允许删除。" */);
                }
                //已确认数据不能删除
                if (accountRealtimeBalance.getIsconfirm() != null && accountRealtimeBalance.getIsconfirm()){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101865"),MessageUtils.getMessage("P_YS_CTM_TMSP-FE_1617722556232499332") /* "银行账户：" */+bankAccount.get("name")+MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128979") /* "，余额日期" */+accountRealtimeBalance.getBalancedate()+MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129054") /* "余额已确认，不允许删除！" */);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101866"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D6", "银行账户：[%s]，余额日期[%s]余额已确认，不允许删除！") /* "银行账户：[%s]，余额日期[%s]余额已确认，不允许删除！" */,bankAccount.get("name"),accountRealtimeBalance.getBalancedate()));

                }
                //银企联下载数据
                if(accountRealtimeBalance.getDatasource() != null && BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode() == accountRealtimeBalance.getDatasource()){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101867"),MessageUtils.getMessage("P_YS_CTM_TMSP-FE_1617722556232499332") /* "银行账户：" */+bankAccount.get("name")+MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128979") /* "，余额日期" */+accountRealtimeBalance.getBalancedate()+MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129036") /* "为银企联下载的数据，不允许删除！" */);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101868"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D7", "银行账户：[%s]，余额日期[%s]为银企联下载的数据，不允许删除！") /* "银行账户：[%s]，余额日期[%s]为银企联下载的数据，不允许删除！" */,bankAccount.get("name"),accountRealtimeBalance.getBalancedate()));

                }
            }catch (CtmException e){
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101869"),e.getMessage());
            }finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }

        }
        return new RuleExecuteResult();
    }

}
