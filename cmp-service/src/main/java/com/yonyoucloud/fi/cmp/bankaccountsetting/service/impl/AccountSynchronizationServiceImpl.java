package com.yonyoucloud.fi.cmp.bankaccountsetting.service.impl;

import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bam.api.openapi.IOpenApiService;
import com.yonyou.yonbip.ctm.bam.model.request.AccountInfoReq;
import com.yonyou.yonbip.ctm.bam.model.response.AccountInfoResp;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.service.AccountSynchronizationService;
import com.yonyoucloud.fi.cmp.constant.EmpowerConstand;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author xuyao2
 * @Date 2023/3/23 11:28
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class AccountSynchronizationServiceImpl implements AccountSynchronizationService {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;
    /**
     * 账号同步调度任务
     * @return 执行结果
     */
    @Override
    public CtmJSONObject bankaccountsync(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //通知调度任务 后端为同步
        result.put("asynchronized", true);
        result.put(ICmpConstant.STATUS, 1);
        result.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        String logId = params.getString("logId");
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock(ICmpConstant.BANKACCOUNTSYNCKEY,10*60L, TimeUnit.SECONDS,(int lockstatus)->{
                    if(lockstatus == LockStatus.GETLOCK_FAIL){
                        //加锁失败
                        TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,logId,
                                InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0006F", "系统正在对此账户拉取中") /* "系统正在对此账户拉取中" */) /* "系统正在对此账户拉取中" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        return ;
                    }
                    //加锁成功
                    excuteBankaccountsync(params);
                    TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId,
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272","执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                });
            } catch (Exception e) {
                log.error("银行账户同步失败：",e);
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100424"),e.getMessage());
            }
        });
        return result;
    }


    private void excuteBankaccountsync(CtmJSONObject params) throws Exception {
            long tenantId = AppContext.getTenantId();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            List<Map<String, Object>> bankAccountSettings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, querySchema);
            //银企联账号list
            List<BankAccountSetting> list = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
            //库中已经存在的账户集合
            Set<String> enterpriseBankAccountIds = new HashSet<>();
            for (Map<String, Object> setting : bankAccountSettings) {
                enterpriseBankAccountIds.add((String) setting.get("enterpriseBankAccount"));
            }
            //资金组织
            //BillContext billContextFinanceOrg = new BillContext();
            //billContextFinanceOrg.setFullname("aa.baseorg.FinanceOrgMV");
            //billContextFinanceOrg.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
            //QueryConditionGroup groupBankFinanceOrg = QueryConditionGroup.and(QueryCondition.name("stopstatus").eq("0"), QueryCondition.name("dr").eq("0"));
            //List<Map<String, Object>> dataListFinanceOrg = MetaDaoHelper.queryAll(billContextFinanceOrg, "id", groupBankFinanceOrg, null);
            //List<String> orgIdList = new ArrayList<>();
            //for (Map<String, Object> map : dataListFinanceOrg) {
            //    orgIdList.add((String) map.get("id"));
            //}

            List<String> orgIdList = new ArrayList<>();
            ConditionDTO condition = ConditionDTO.newCondition().withEnabled();
            List<String> allIds = fundsOrgQueryServiceComponent.getIdsByCondition(condition);
            for (String id : allIds) {
                orgIdList.add(id);
            }

            //企业银行账户
            BillContext billContextFinBank = new BillContext();
            billContextFinBank.setFullname("bd.enterprise.OrgFinBankacctVO");
            billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            //和手动同步保持一致，支持财务公司、银行开户
            conditionGroup.appendCondition(QueryCondition.name("acctopentype").in(AcctopenTypeEnum.FinancialCompany.getValue(),AcctopenTypeEnum.BankAccount.getValue()));
            conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
            //账户开户类型为银行，过滤掉内部账户
            List<Map<String, Object>> enterpriseBankAccounts = MetaDaoHelper.queryAll(billContextFinBank, "id,orgid,bank,acctstatus,enable", conditionGroup, null);
            List<BankAccountSetting> insertBankAccountSettings = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (Map<String, Object> enterpriseBankAccount : enterpriseBankAccounts) {
                //企业银行账户id
                ids.add((String) enterpriseBankAccount.get("id"));
                if (!orgIdList.contains(enterpriseBankAccount.get("orgid"))) {
                    continue;
                }
                if (enterpriseBankAccountIds.contains(enterpriseBankAccount.get("id"))) {
                    continue;
                }
                BankAccountSetting bankaccountSetting = new BankAccountSetting();
                bankaccountSetting.setAccentity((String) enterpriseBankAccount.get("orgid"));
                bankaccountSetting.setEnterpriseBankAccount((String) enterpriseBankAccount.get("id"));
                bankaccountSetting.setAccStatus((String.valueOf((int) enterpriseBankAccount.get("enable") == 1 ? 0 : 1)));
                bankaccountSetting.setOpenFlag(false);
                bankaccountSetting.setTenant(tenantId);
                bankaccountSetting.setEntityStatus(EntityStatus.Insert);
                bankaccountSetting.setId(ymsOidGenerator.nextId());
                //同步开户管理
                AccountInfoReq accountInfoReq = new AccountInfoReq();
                accountInfoReq.setIds(Arrays.asList(enterpriseBankAccount.get("id").toString()));
                //根据ids批量查询开户管理信息
                List<AccountInfoResp> accountInfoResps = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_BAM).listAccountInfo(accountInfoReq);
                if(accountInfoResps.size() > 0){
                    bankaccountSetting.setOpenFlag(accountInfoResps.get(0).getIsCashLink());
                    bankaccountSetting.setEmpower(accountInfoResps.get(0).getDirectAuth());
                }
                insertBankAccountSettings.add(bankaccountSetting);
            }

            if (insertBankAccountSettings.size() > 0) {
                CmpMetaDaoHelper.insert(BankAccountSetting.ENTITY_NAME, insertBankAccountSettings);
            }

            //修改
            List<BankAccountSetting> updateOpenFlagList = new ArrayList<>();
            if(ids.size() > 0){
                AccountInfoReq accountInfoReq = new AccountInfoReq();
                accountInfoReq.setIds(ids);
                //根据ids批量查询开户管理信息
                List<AccountInfoResp> accountInfoResps = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_BAM).listAccountInfo(accountInfoReq);
                if (ValueUtils.isNotEmptyObj(accountInfoResps)) {
                    for(BankAccountSetting bankAccountSetting : list){
                        for(AccountInfoResp accountInfoResp : accountInfoResps){
                            if(bankAccountSetting.getEnterpriseBankAccount().equals(accountInfoResp.getId()) && (bankAccountSetting.getOpenFlag() != accountInfoResp.getIsCashLink())){
                                bankAccountSetting.setOpenFlag(accountInfoResp.getIsCashLink());
                                //if (accountInfoResp.getDirectAuth() != null) {
                                //    bankAccountSetting.setEmpower(("1").equals(accountInfoResp.getDirectAuth()) ? EmpowerConstand.EMPOWER_ONLYQUERY : EmpowerConstand.EMPOWER_QUERYANDPAY);
                                //} else
                                //现在直接返回对应字符串，不用转换了
                                bankAccountSetting.setEmpower(accountInfoResp.getDirectAuth());
                                bankAccountSetting.setEntityStatus(EntityStatus.Update);
                                updateOpenFlagList.add(bankAccountSetting);
                            }
                        }
                    }
                }
            }

            if(updateOpenFlagList.size()>0){
                EntityTool.setUpdateStatus(updateOpenFlagList);
                MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, updateOpenFlagList);
            }

            List<BankAccountSetting> lastList = MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);

            //修改
            List<BankAccountSetting> updateAccStatusList = new ArrayList<>();
            if(lastList.size() > 0) {
                for (BankAccountSetting item : lastList) {
                    if(null == item.getAccStatus()){
                        item.setAccStatus("99");
                    }
                    for (Map<String, Object> enterpriseBankAccount : enterpriseBankAccounts) {
                        String flag = "";
                        if((int)enterpriseBankAccount.get("enable") == 1){
                            flag = "0";
                        }else{
                            flag = "1";
                        }
                        //符合的银企联账号
                        if ((item.getEnterpriseBankAccount()).equals(enterpriseBankAccount.get("id")) && (!item.getAccStatus().equals(flag))) {
                            item.setAccStatus(String.valueOf((int) enterpriseBankAccount.get("enable") == 1 ? 0 : 1));
                            item.setEntityStatus(EntityStatus.Update);
                            updateAccStatusList.add(item);
                        }
                    }
                }
            }
            if(updateAccStatusList.size()>0){
                EntityTool.setUpdateStatus(updateAccStatusList);
                MetaDaoHelper.update(BankAccountSetting.ENTITY_NAME, updateAccStatusList);
            }
    }

}
