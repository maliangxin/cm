package com.yonyoucloud.fi.cmp.openapi.service.impl;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.api.openapi.BankReceiptCloudOpenApiService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailServiceImpl;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @description: 银行交易回单openapi 具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/23 15:01
 */

@Service
@Slf4j
public class BankReceiptCloudOpenApiServiceImpl implements BankReceiptCloudOpenApiService {
    private static final int MAX_LOOP_COUNT = 500;//最大记录条数不会超过100
    @Autowired
    private PaymentService paymentService;// 获取交易明细接口

    @Autowired
    BaseRefRpcService baseRefRpcService;// 基础档案公共接口
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Autowired
    BankDealDetailServiceImpl bankDealDetailService;
    @Autowired
    private CtmCmpCheckRepeatDataService checkRepeatDataService;

    //单据智能分类service
    @Resource
    private BillSmartClassifyService billSmartClassifyService;

    @Autowired
    ICmpSendEventService cmpSendEventService;

    public static final String SERVICE_SUCCESS_STATUS = "00";

    private static final List<String> LINENUMBER_LIST = new ArrayList<>();


    public static final String SERVICE_RESP_SUCCESS_CODE = "000000";

    private String getTranDate(CtmJSONObject responseBody) {
        if(responseBody == null){
            return null;
        }
        String tranDate = null;
        int backNum = getBackNum(responseBody);
        if (backNum == 0) {
            return tranDate;
        }
        if (backNum == 1) {
            CtmJSONObject jsonData = responseBody.getJSONObject("record");
            tranDate = jsonData.getString("tran_date");
        } else {
            CtmJSONArray records = responseBody.getJSONArray("record");
            tranDate = records.getJSONObject(0).getString("tran_date");
        }
        return tranDate;
    }



    /**
     * 获取交易明细对应银行账户
     *
     * @param responseBody
     * @return
     */
    private String getBankAccount(CtmJSONObject responseBody) {
        if (responseBody == null) {
            return null;
        }
        return responseBody.getString("acct_no");
    }

    private String getFieldFromRecord(CtmJSONObject responseBody, String fieldName) {
        if(responseBody == null){
            return null;
        }
        String fieldValue = null;
        int backNum = getBackNum(responseBody);
        if (backNum == 0) {
            return fieldValue;
        }
        if (backNum == 1) {
            CtmJSONObject jsonData = responseBody.getJSONObject("record");
            fieldValue = jsonData.getString(fieldName);
        } else {
            CtmJSONArray records = responseBody.getJSONArray("record");
            fieldValue = records.getJSONObject(0).getString(fieldName);
        }
        return fieldValue;
    }

    private String getFieldFromBody(CtmJSONObject responseBody, String fieldName) {
        if (responseBody == null) {
            return null;
        }
        return responseBody.getString("acct_no");
    }

    private List<String> getFieldsListFromRecord(CtmJSONObject responseBody, List<String> fieldNames) {
        List<String> fieldValueList = new ArrayList<>();
        if(responseBody == null){
            return null;
        }
        StringBuilder fieldValues = new StringBuilder();
        int backNum = getBackNum(responseBody);
        if (backNum == 0) {
            return fieldValueList;
        }else{
            CtmJSONArray records = responseBody.getJSONArray("record");
            if (records == null) {
                records = new CtmJSONArray();
                CtmJSONObject record = responseBody.getJSONObject("record");
                records.add(record);
            }
            for (int i = 0; i < records.size(); i++) {
                for (int j = 0; j < fieldNames.size(); j++) {
                    String fieldName = fieldNames.get(j);
                    fieldValues.append(records.getJSONObject(i).getString(fieldName));

                }
                fieldValueList.add(fieldValues.toString());
            }
        }
        return fieldValueList;
    }

    private int getBackNum(CtmJSONObject responseBody) {
        if (responseBody == null) {
            throw new CtmException("responseBody is null");
        }

        Integer backNum = responseBody.getInteger(YQLUtils.BACK_NUM);
        if (backNum != null) {
            return backNum;
        }

        Integer totNum = responseBody.getInteger(YQLUtils.TOT_NUM);
        if (totNum == null) {
            throw new CtmException("Missing both '" + YQLUtils.BACK_NUM + "' and '" + YQLUtils.TOT_NUM + "' in response body");
        }

        return totNum;
    }

    @Override
    public HashMap<String, Object> saveBankCloudData(CtmJSONObject data) throws Exception {
        try {
            String responseStr = CtmJSONObject.toJSON(data).toString();
            log.error("===========saveBankCloudData============入参：" + responseStr);
            AtomicReference<HashMap<String, Object>> map = new AtomicReference<>(new HashMap());
            // 从报文中解析请求的交易码
            CtmJSONObject responseHead = data.getJSONObject("data").getJSONObject("response_head");
            CtmJSONObject responseBody = data.getJSONObject("data").getJSONObject("response_body");
            String tranCode = null;
            if (responseHead != null) {
                tranCode = responseHead.getString("trade_code");
            }
            String tranDate = null;
            if(ITransCodeConstant.QUERY_ACCOUNT_BALANCE.equals(tranCode)){
                tranDate = "today";
            }else {
                if (responseBody == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004F1", "数据拉取失败：") /* "数据拉取失败：" */ + "responseBody is null");
                }
                if ("9999".equals(responseHead.get("service_resp_code").toString())) {
                    log.error("数据拉取失败："+CtmJSONObject.toJSON(data).toString());
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004F1", "数据拉取失败：") /* "数据拉取失败：" */ + CtmJSONObject.toJSON(data).toString());
                }
                tranDate = getTranDate(responseBody);
                if (tranDate == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004F1", "数据拉取失败：") /* "数据拉取失败：" */ + "tranDate is null");
                }
            }

            String curr_code = getFieldFromRecord(responseBody, YQLUtils.CURR_CODE);

            List<String> accountInfoLocks = new ArrayList<>();
            String singleLockKey = "";
            if (ITransCodeConstant.QUERY_ACCOUNT_BALANCE.equals(tranCode)) {
                List<String> acct_no_curr_code_list = getFieldsListFromRecord(responseBody, Arrays.asList(YQLUtils.ACCT_NO, YQLUtils.CURR_CODE));
                accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.SYN_DATA_ZGH + tranCode, acct_no_curr_code_list);
            }else {
                String acct_no = getFieldFromBody(responseBody, YQLUtils.ACCT_NO);
                String lockKeyPostFix = tranCode + tranDate + acct_no + curr_code;
                singleLockKey = ICmpConstant.SYN_DATA_ZGH + lockKeyPostFix;
            }

            String finalSingleLockKey = singleLockKey;
            List<String> finalAccountInfoLocks = accountInfoLocks;
            switch (tranCode) {
                case ITransCodeConstant.QUERY_ACCOUNT_BALANCE: // 实时余额
                    CtmLockTool.executeInOneServiceExclusivelyBatchLock(finalAccountInfoLocks, 60 * 60 * 4L, TimeUnit.SECONDS, (int status) -> {
                        if (status == LockStatus.GETLOCK_FAIL) {
                            throw new CtmException("get lock fail, key:" + String.join(",", finalAccountInfoLocks));
                        }
                        map.set(insertRealBalanceDetail(data));
                    });
                    break;
                case ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE: // 历史余额
                    CtmLockTool.executeInOneServiceLock(finalSingleLockKey, 60 * 60 * 4L, TimeUnit.SECONDS, (int status) -> {
                        if (status == LockStatus.GETLOCK_FAIL) {
                            throw new CtmException("get lock fail, key:" + finalSingleLockKey);

                        }
                        map.set(insertHistoryBalanceDetail(data));
                    });
                    break;
                case ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL: // 交易明细
                    CtmLockTool.executeInOneServiceLock(finalSingleLockKey, 60 * 60 * 4L, TimeUnit.SECONDS, (int status) -> {
                        if (status == LockStatus.GETLOCK_FAIL) {
                            throw new CtmException("get lock fail, key:" + finalSingleLockKey);
                        }
                        map.set(insertTransactionDetail(data));
                    });
                    break;
                case ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL:// 交易回单
                    CtmLockTool.executeInOneServiceLock(finalSingleLockKey, 60 * 60 * 4L, TimeUnit.SECONDS, (int status) -> {
                        if (status == LockStatus.GETLOCK_FAIL) {
                            throw new CtmException("get lock fail, key:" + finalSingleLockKey);
                        }
                        map.set(insertReceiptDetail(data));
                    });
                    break;
                default:
                    break;
            }
            return map.get();
        } catch (Exception e) {
            log.error("saveBankCloudData报错：", e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E2", "saveBankCloudData异常：") /* "saveBankCloudData异常：" */ + e.getMessage());
        }
    }

    private HashMap<String, Object> insertReceiptDetail(CtmJSONObject result) throws Exception {// 暂时不需要回单接口
        return null;
    }

    /**
     * 插入实时余额接口
     *
     * @param result
     * @return
     * @throws
     */
    private HashMap<String, Object> insertRealBalanceDetail(CtmJSONObject result) throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        String service_resp_code = responseHead.getString("service_resp_code");
        //无论是否成功 都把信息提示错出来
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        if ((SERVICE_SUCCESS_STATUS).equals(serviceStatus) && (SERVICE_RESP_SUCCESS_CODE).equals(service_resp_code)) { //00 - 交易成功
            CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
            List<EnterpriseBankAcctVO> enterpriseVOs = getEnterpriseBankAcctVOListByAccount(responseBody, true);
            if (enterpriseVOs.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102295"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000026013", "会计主体无有效的企业银行账户") /* "会计主体无有效的企业银行账户" */);
            }
            insertAccountBalanceData(null, enterpriseVOs, responseBody); // 插入企业银行账户实时余额数据
        }
        return map;
    }

    /*
     *@Author tongyd
     *@Description 插入企业银行账户实时余额数据
     *@Date 2019/5/31 15:43
     *@Param [accEntity, bankAccounts, responseBody]
     *@Return void
     **/
    private void insertAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseBody) throws Exception {
        List<AccountRealtimeBalance> balances = new ArrayList<>();
        //用于存储账户id 删除时根据id删除
        List<String> accId = new ArrayList<>();
        int totalNum = responseBody.getInteger("tot_num");
        String currencyCode = "CNY";
        Set<String> currencyoid = new HashSet();
        if (totalNum < 1) {
            return;
        } else if (totalNum == 1) {
            CtmJSONObject record = responseBody.getJSONObject("record");
            if (!record.containsKey("acct_bal") || !record.containsKey("acct_no")) {
                return;
            }
            AccountRealtimeBalance balance = new AccountRealtimeBalance();
            balance.setTenant(AppContext.getTenantId());
            balance.setAccentity(accEntity);
            EnterpriseBankAcctVO bankAccount = null;
            String accNo = record.getString("acct_no");
            Iterator<EnterpriseBankAcctVO> iterator = bankAccounts.iterator();
            while (iterator.hasNext()) {
                EnterpriseBankAcctVO account = iterator.next();
                if (account.getAccount().equals(accNo)) {
                    bankAccount = account;
                    accId.add(account.getId());
                    iterator.remove();
                    break;
                }
            }
            //start wangdengk 20230713 此处增加业务逻辑 取银企账户的使用组织给会计主体赋值
            balance.setAccentity(bankAccount.getOrgid());
            //end wangdengk 20230713 此处增加业务逻辑 取银企账户的使用组织给会计主体赋值
            balance.setEnterpriseBankAccount((String) bankAccount.getId());
            Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankAccount.getId());
            balance.setBanktype(bankAccount2.get("bank").toString());
            if (bankAccount.getBank() != null) {
                balance.setBanktype((String) bankAccount.getBank());
            }
            currencyCode = record.getString("curr_code");
            if (StringUtils.isNotEmpty(currencyCode)) {
                String currency = currencyQueryService.getCurrencyByCode(currencyCode);
                balance.setCurrency(currency);
            } else {
                //若银企联返回报文中没有指定币种 这里默认启用币种查询逻辑
                String currency = currencyQueryService.getCurrencyByAccount(bankAccount.getId());
                balance.setCurrency(currency);
            }
            currencyoid.add(balance.getCurrency());
            balance.setCashflag(record.getString("cash_flag"));
            balance.setYesterbal(!record.containsKey("yester_bal") ? BigDecimal.ZERO : record.getBigDecimal("yester_bal"));
            balance.setAcctbal(!record.containsKey("acct_bal") ? BigDecimal.ZERO : record.getBigDecimal("acct_bal"));
            balance.setAvlbal(!record.containsKey("avl_bal") ? BigDecimal.ZERO : record.getBigDecimal("avl_bal"));
            balance.setFrzbal(!record.containsKey("frz_bal") ? BigDecimal.ZERO : record.getBigDecimal("frz_bal"));
            balance.setBalancedate(DateUtils.formatBalanceDate(new Date()));//实时余额新增余额日期字段 默认为系统当天
            //国际化相关字段
            balance.setProj_name(record.getString("proj_name"));
            balance.setProj_name(record.getString("sub_name"));
            balance.setProj_name(record.getString("budget_source"));
            balance.setFlag(BalanceFlag.AutoPull.getCode());
            balance.setDatasource(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode());
            balance.setEntityStatus(EntityStatus.Insert);
            balance.setId(ymsOidGenerator.nextId());
            balances.add(balance);
        } else {
            CtmJSONArray records = responseBody.getJSONArray("record");
            int loopCount = records.size();
//            if(loopCount > MAX_LOOP_COUNT){
//                loopCount = MAX_LOOP_COUNT;
//            }
            for (int i = 0; i < loopCount; i++) {
                CtmJSONObject record = records.getJSONObject(i);
                if (!record.containsKey("acct_bal") || !record.containsKey("acct_no")) {
                    continue;
                }
                AccountRealtimeBalance balance = new AccountRealtimeBalance();
                balance.setTenant(AppContext.getTenantId());
                balance.setAccentity(accEntity);
                EnterpriseBankAcctVO bankAccount = null;
                String accNo = record.getString("acct_no");
                Iterator<EnterpriseBankAcctVO> iterator = bankAccounts.iterator();
                while (iterator.hasNext()) {
                    EnterpriseBankAcctVO account = iterator.next();
                    if (account.getAccount().equals(accNo)) {
                        bankAccount = account;
                        accId.add(account.getId());
                        iterator.remove();
                        break;
                    }
                }
                //start wangdengk 20230713 此处增加业务逻辑 取银企账户的使用组织给会计主体赋值
                balance.setAccentity(bankAccount.getOrgid());
                //end wangdengk 20230713 此处增加业务逻辑 取银企账户的使用组织给会计主体赋值
                balance.setEnterpriseBankAccount(bankAccount.getId());
                Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankAccount.getId());
                balance.setBanktype(bankAccount2.get("bank").toString());
                if (bankAccount.getBank() != null) {
                    balance.setBanktype((String) bankAccount.getBank());
                }
                currencyCode = record.getString("curr_code");
                if (StringUtils.isNotEmpty(currencyCode)) {
                    String currency = currencyQueryService.getCurrencyByCode(currencyCode);
                    balance.setCurrency(currency);
                } else {
                    //若银企联返回报文中没有指定币种 这里默认启用币种查询逻辑
                    String currency = currencyQueryService.getCurrencyByAccount(bankAccount.getId());
                    balance.setCurrency(currency);
                }
                balance.setCashflag(record.getString("cash_flag"));
                balance.setYesterbal(!record.containsKey("yester_bal") ? BigDecimal.ZERO : record.getBigDecimal("yester_bal"));
                balance.setAcctbal(!record.containsKey("acct_bal") ? BigDecimal.ZERO : record.getBigDecimal("acct_bal"));
                balance.setAvlbal(!record.containsKey("avl_bal") ? BigDecimal.ZERO : record.getBigDecimal("avl_bal"));
                balance.setFrzbal(!record.containsKey("frz_bal") ? BigDecimal.ZERO : record.getBigDecimal("frz_bal"));
                balance.setBalancedate(DateUtils.formatBalanceDate(new Date()));//实时余额新增余额日期字段 默认为系统当天
                //国际化相关字段
                balance.setProj_name(record.getString("proj_name"));
                balance.setProj_name(record.getString("sub_name"));
                balance.setProj_name(record.getString("budget_source"));
                balance.setFlag(BalanceFlag.AutoPull.getCode());
                balance.setDatasource(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode());
                balance.setEntityStatus(EntityStatus.Insert);
                balance.setId(ymsOidGenerator.nextId());
                balances.add(balance);
                currencyoid.add(balance.getCurrency());
            }
        }
        // start wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
        List<AccountRealtimeBalance> existBalances = queryExistRealBalanceData(balances);
        executeRealGroupData(existBalances, balances);
        // end wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入

    }

    /**
     * 根据返回的结果查询 已经存在的当天的余额
     *
     * @param balances
     * @return
     */
    private List<AccountRealtimeBalance> queryExistRealBalanceData(List<AccountRealtimeBalance> balances) throws Exception {
        Set<String> orgIds = new HashSet<String>(); // 会计主体集合
        Set<String> accIds = new HashSet<String>(); // 银行账号集合
        Set<String> curIds = new HashSet<String>(); // 币种id集合
        balances.forEach(currBalance -> {
            orgIds.add(currBalance.getAccentity());
            accIds.add(currBalance.getEnterpriseBankAccount());
            curIds.add(currBalance.getCurrency());
        });
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(orgIds));
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(accIds));
        conditionGroup.appendCondition(QueryCondition.name("currency").in(curIds));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").in(LocalDateUtil.getNowDateString()));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        return existBalances;
    }

    /**
     * 对结果分组 分别执行更新 插入操作
     *
     * @param existBalances
     * @param balances
     * @return
     */
    private void executeRealGroupData(List<AccountRealtimeBalance> existBalances, List<AccountRealtimeBalance> balances) throws Exception {
        List<AccountRealtimeBalance> firstInsertBalances = new ArrayList<>();// 定义首次新增数据集合
        List<AccountRealtimeBalance> insertBalances = new ArrayList<>();// 定义新增数据集合
        List<AccountRealtimeBalance> updateBalances = new ArrayList<>();// 定义更新数据集合
        HashMap<String, AccountRealtimeBalance> groupMap = new HashMap<>();
        if (balances.isEmpty()) { // 如果返回的余额没有值 直接返回
            return;
        }
        if (!existBalances.isEmpty()) { // 对已存在的数据遍历根据会计主体+银行账户+币种分组
            existBalances.forEach(existBalance -> {
                StringBuilder keyBuilder = new StringBuilder();
                keyBuilder.append(existBalance.getAccentity()).append("#");
                keyBuilder.append(existBalance.getEnterpriseBankAccount()).append("#");
                keyBuilder.append(existBalance.getCurrency());
                groupMap.put(keyBuilder.toString(), existBalance);
            });
        }
        balances.forEach(currBalance -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(currBalance.getAccentity()).append("#");
            keyBuilder.append(currBalance.getEnterpriseBankAccount()).append("#");
            keyBuilder.append(currBalance.getCurrency());
            if (groupMap.containsKey(keyBuilder.toString())) {
                AccountRealtimeBalance exsitBalance = groupMap.get(keyBuilder.toString());
                // if(StringUtils.isEmpty(exsitBalance.getFirst_flag())){ // 如果firstFlag为空再更新
                //     AccountRealtimeBalance updateBalance =   groupMap.get(keyBuilder.toString());
                // TODO 使用currBalance 更新updateBalance
                updateSourceRealBalanceData(exsitBalance, currBalance);
                updateBalances.add(exsitBalance);
                // }
            } else {
                insertBalances.add(currBalance);
            }
        });
        if (!firstInsertBalances.isEmpty()) {
            CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, firstInsertBalances);
        }
        if (!insertBalances.isEmpty()) {
            CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, insertBalances);
        }
        if (!updateBalances.isEmpty()) {
            MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, updateBalances);
        }
    }

    /**
     * 更新旧的实时余额关键字段
     *
     * @param sourceData
     * @param targetData
     * @return
     */
    private void updateSourceRealBalanceData(AccountRealtimeBalance sourceData, AccountRealtimeBalance targetData) {
        sourceData.setCashflag(targetData.getCashflag());
        sourceData.setYesterbal(targetData.getYesterbal());
        sourceData.setAcctbal(targetData.getAcctbal());
        sourceData.setAvlbal(targetData.getAvlbal());
        sourceData.setFrzbal(targetData.getFrzbal());
        sourceData.setModifyDate(new Date());
        sourceData.setModifyTime(new Date());
        //实时余额国际化相关字段
        sourceData.setProj_name(targetData.getProj_name());
        sourceData.setSub_name(targetData.getSub_name());
        sourceData.setBudget_source(targetData.getBudget_source());
        sourceData.setFlag(targetData.getFlag());
        sourceData.setDatasource(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode());
        sourceData.setEntityStatus(EntityStatus.Update);
    }

    /**
     * 插入历史余额接口
     *
     * @param result
     * @return
     * @throws
     */
    private HashMap<String, Object> insertHistoryBalanceDetail(CtmJSONObject result) throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        String service_resp_code = responseHead.getString("service_resp_code");
        //无论是否成功 都把信息提示错出来
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        if ((SERVICE_SUCCESS_STATUS).equals(serviceStatus) && (SERVICE_RESP_SUCCESS_CODE).equals(service_resp_code)) { //00 - 交易成功
            CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
            EnterpriseBankAcctVO enterpriseVO = getEnterpriseBankAcctVOByAccount(responseBody, false);
            if (enterpriseVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102296"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000026013","会计主体无有效的企业银行账户") /* "会计主体无有效的企业银行账户" */);
            }
            Map<String, Object> bankAccount = new HashMap<>();
            bankAccount.put("enterpriseBankAccount", enterpriseVO.getId());
            bankAccount.put("bank", enterpriseVO.getBank());
            String accEntity = enterpriseVO.getOrgid();
            //保存账户历史余额
            insertAccountHistoryBalanceData(accEntity, bankAccount, responseBody);
        }
        return map;
    }

    /**
     * 历史余额入库
     *
     * @param accEntity
     * @param bankAccount
     * @param responseBody
     * @throws Exception
     */
    private void insertAccountHistoryBalanceData(String accEntity, Map<String, Object> bankAccount, CtmJSONObject responseBody) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        int backNum = responseBody.getInteger("back_num");
        if (backNum == 0) {
            return;
        }
        if (backNum == 1) {
            CtmJSONObject jsonData = responseBody.getJSONObject("record");
            records.add(jsonData);
        } else {
            records = responseBody.getJSONArray("record");
        }
        int loopCount = records.size();
//        if(loopCount > MAX_LOOP_COUNT){
//            loopCount = MAX_LOOP_COUNT;
//        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        List<AccountRealtimeBalance> balances = new ArrayList<>();
        for (int i = 0; i < loopCount; i++) {
            CtmJSONObject record = records.getJSONObject(i);
            AccountRealtimeBalance balance = new AccountRealtimeBalance();
            String _tran_date = record.getString("tran_date");
            balance.setBalancedate(DateUtils.dateParse(_tran_date, "yyyyMMdd"));
            balance.setTenant(AppContext.getTenantId());
            balance.setAccentity(accEntity);
            String currencyCode = record.getString("curr_code");
            if (StringUtils.isNotEmpty(currencyCode)) {
                CurrencyTenantDTO currency = baseRefRpcService.getCurrencyByCode(currencyCode);
                if (null == currency) {
                    currency = baseRefRpcService.getCurrencyByCode("CNY");
                    balance.setCurrency(currency.getId());
                } else {
                    balance.setCurrency(currency.getId());
                }
            } else {
                //若银企联返回报文中没有指定币种 这里默认人民币
                CurrencyTenantDTO currency = baseRefRpcService.getCurrencyByCode("CNY");
                balance.setCurrency(currency.getId());
            }
            deleteAccountBalanceList(accEntity, balance.getCurrency(), bankAccount.get("enterpriseBankAccount").toString(), _tran_date);
            if (bankAccount.get("bank") != null) {
                balance.setBanktype((String) bankAccount.get("bank"));
            }
            Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankAccount.get("enterpriseBankAccount"));
            balance.setBanktype(bankAccount2.get("bank").toString());
            balance.setEnterpriseBankAccount(bankAccount.get("enterpriseBankAccount") + "");
            BigDecimal acctBal = !record.containsKey("acct_bal") ? BigDecimal.ZERO : record.getBigDecimal("acct_bal");
            balance.setAcctbal(acctBal);
//            balance.setAvlbal(!record.containsKey("avl_bal") ? acctBal : record.getBigDecimal("avl_bal"));
//            balance.setFrzbal(!record.containsKey("frz_bal") ? BigDecimal.ZERO : record.getBigDecimal("frz_bal"));
            /**
             * 1,判断冻结金额是否存在
             * 2,不存在则查询当日实时余额中的冻结金额
             * 3，不存在实时余额走原有逻辑，存在实时余额，根据实时余额得冻结金额赋值，并计算可用余额
             */
            if (record.getBigDecimal("frz_bal") == null || BigDecimal.ZERO.equals(record.getBigDecimal("frz_bal"))) {
                //查询实时余额
                Date nextDay = DateUtils.dateAdd(balance.getBalancedate(), 1, false);
                nextDay = DateUtils.formatBalanceDate(nextDay);
                QuerySchema schema2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup2.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accEntity));
                conditionGroup2.appendCondition(QueryCondition.name("currency").eq(balance.getCurrency()));
                conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bankAccount.get("enterpriseBankAccount")));
                conditionGroup2.appendCondition(QueryCondition.name("balancedate").eq(nextDay));
                conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("1"));
                schema2.addCondition(conditionGroup2);
                List<AccountRealtimeBalance> realtimeBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
                if (realtimeBalances != null && realtimeBalances.size() > 0) {
                    balance.setAvlbal(acctBal.subtract(realtimeBalances.get(0).getFrzbal()));
                    balance.setFrzbal(realtimeBalances.get(0).getFrzbal());
                } else {
                    balance.setAvlbal(!record.containsKey("avl_bal") ? acctBal : record.getBigDecimal("avl_bal"));
                    balance.setFrzbal(!record.containsKey("frz_bal") ? BigDecimal.ZERO : record.getBigDecimal("frz_bal"));
                }
            } else {
                balance.setFrzbal(record.getBigDecimal("frz_bal"));
                balance.setAvlbal(acctBal.subtract(record.getBigDecimal("frz_bal")));
            }
            balance.setFlag(BalanceFlag.AutoPull.getCode());
            balance.setEntityStatus(EntityStatus.Insert);
            balance.setCreateTime(new Date());
            balance.setCreateDate(DateUtils.getNowDateShort2());
            balance.setCreator(AppContext.getCurrentUser().getName());//新增人名称
            balance.setCreatorId(AppContext.getCurrentUser().getId());//新增人id
            balance.setId(ymsOidGenerator.nextId());
            balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
            balances.add(balance);
        }
        CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, balances);
    }

    /**
     * 根据余额四要素，删除重复历史余额数据
     *
     * @param accEntity
     * @param currency
     * @param enterpriseBankAccount
     * @param date
     * @throws Exception
     */
    private void deleteAccountBalanceList(String accEntity, String currency, String enterpriseBankAccount, String date) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accEntity));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(date));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        if (existBalances != null && existBalances.size() > 0) {
            existBalances.stream().forEach(existBalance -> existBalance.setEntityStatus(EntityStatus.Delete));
            MetaDaoHelper.delete(AccountRealtimeBalance.ENTITY_NAME, existBalances);
        }
    }

    /**
     * 插入交易明细
     *
     * @param result
     * @return
     * @throws
     */
    private HashMap<String, Object> insertTransactionDetail(CtmJSONObject result) throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        String service_resp_code = responseHead.getString("service_resp_code");
        if ((SERVICE_SUCCESS_STATUS).equals(serviceStatus) && (SERVICE_RESP_SUCCESS_CODE).equals(service_resp_code)) {
            CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
            EnterpriseBankAcctVO enterpriseVO = getEnterpriseBankAcctVOByAccount(responseBody, false);
            if (enterpriseVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102297"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000026013","会计主体无有效的企业银行账户") /* "会计主体无有效的企业银行账户" */);
            }
            Map<String, Object> enterpriseInfo = new HashMap<>();
            enterpriseInfo.put("accEntityId", enterpriseVO.getOrgid());
            enterpriseInfo.put("accountId", enterpriseVO.getId());
            insertTransactionDetail(enterpriseInfo, responseBody, false);
            ;
        }
        return map;
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 插入交易明细数据
     * @Date 2019/8/7
     * @Param [enterpriseInfo, responseBody]
     **/
    private void insertTransactionDetail(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody, Boolean isTask) throws Exception {
        List<BankDealDetail> bankDealDetails = new ArrayList<>();
        List<BankReconciliation> bankRecords = new ArrayList<>();
        int backNum = responseBody.getInteger("back_num");
        //由于大部分数据都为人民币 这里先查询提升效率
        String currency = currencyQueryService.getCurrencyByAccount((String) enterpriseInfo.get("accountId"));
        if (backNum == 1) {
            CtmJSONObject detailData = responseBody.getJSONObject("record");
            CtmJSONArray ctmJSONArray = new CtmJSONArray();
            ctmJSONArray.add(detailData);
//            ctmJSONArray = checkRepetitionsForBankseqno(ctmJSONArray, enterpriseInfo, false);
            if(null!=ctmJSONArray && ctmJSONArray.size()!=0){
                analysisDetailData(detailData, enterpriseInfo, bankDealDetails, bankRecords, 0, currency);
            }
        } else if (backNum > 1) {
            CtmJSONArray records = responseBody.getJSONArray("record");
//            records = checkRepetitionsForBankseqno(records, enterpriseInfo, false);
            if (records != null && !records.isEmpty()) {
                for (int i = 0; i < records.size(); i++) {
                    CtmJSONObject detailData = records.getJSONObject(i);
                    analysisDetailData(detailData, enterpriseInfo, bankDealDetails, bankRecords, 0, currency);
                }
            }
        }
        // majfd注释，没发现有加锁逻辑，注释解锁代码
        if (bankDealDetails.size() == 0) {
            return;
        }
//        String bankDealDetailsStr = CtmJSONObject.toJSONString(bankDealDetails);
//        String bankRecordsStr = CtmJSONObject.toJSONString(bankRecords);
//        log.error("===========insertTransactionDetail============入库开始：" + "BankDealDetail:" + bankDealDetailsStr + ";BankReconciliation: " + bankRecordsStr);
//        MetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, bankDealDetails);
//        MetaDaoHelper.insert(BankReconciliation.ENTITY_NAME, bankRecords);
//        log.error("===========insertTransactionDetail============入库结束：" + "BankDealDetail:" + bankDealDetails.size() + ";BankReconciliation: " + bankRecords.size());
        Long startTime = System.currentTimeMillis();
        bankDealDetails = checkRepeatDataService.checkRepeatData(bankDealDetails, EventType.BankDealDetail.getValue());
        Long endTime = System.currentTimeMillis();
        log.error("=================================checkRepeatDataService=====耗时："+(endTime-startTime)+"ms======");
        bankRecords = checkRepeatDataService.checkRepeatData(bankRecords, EventType.CashMark.getValue());
        log.error("=================================checkRepeatDataService2=====耗时："+(System.currentTimeMillis()-endTime)+"ms======");
        //使用去重后的交易明细，将银行对账单去重
        HashMap<String,BankReconciliation> bankMap = new HashMap<>();
        for (BankReconciliation bank: bankRecords){
            bankMap.put(bank.getId().toString(),bank);
        }
        //非重复的入库数据
        List<BankReconciliation> newBankRecords = new ArrayList<>();
        //过滤支出数据
        List<BankReconciliation> impRecords = new ArrayList<>();
        for (BankDealDetail detail: bankDealDetails){
            BankReconciliation newBank = bankMap.get(detail.getId().toString());
            if(newBank != null){
                newBankRecords.add(newBank);
                if(Direction.Credit.equals(newBank.getDc_flag())){
                    impRecords.add(newBank);
                }
            }
        }
        // 智能辨识 银行对账单对方类型
        bankDealDetailService.smartClassifyBatchProcess(newBankRecords);

        String bankDealDetailsStr = CtmJSONObject.toJSONString(bankDealDetails);
        String bankRecordsStr = CtmJSONObject.toJSONString(bankRecords);
        log.error("===========insertTransactionDetail============入库开始：" + "BankDealDetail:" + bankDealDetailsStr + ";BankReconciliation: " + bankRecordsStr);
        MetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, bankDealDetails);
        MetaDaoHelper.insert(BankReconciliation.ENTITY_NAME, newBankRecords);
        log.error("===========insertTransactionDetail============入库结束============");
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 解析交易明细数据
     * @Date 2019/8/7
     * @Param [detailData, enterpriseInfo, bankDealDetails, bankRecords]
     **/
    private void analysisDetailData(CtmJSONObject detailData, Map<String, Object> enterpriseInfo,
                                    List<BankDealDetail> bankDealDetails,
                                    List<BankReconciliation> bankRecords, int count, String currency) throws Exception {
        log.error("===========analysisDetailData============入参：" + CtmJSONObject.toJSON(detailData).toString());
        String bankSeqNo = detailData.getString("bank_seq_no");
        BankDealDetail detail = new BankDealDetail();
        BankReconciliation bankRecord = new BankReconciliation();
        bankRecord.setInitflag(false);
        bankRecord.setLibraryflag(false);
        bankRecord.setDataOrigin(DateOrigin.DownFromYQL);
        detail.setTenant(AppContext.getTenantId());
        bankRecord.setTenant(AppContext.getTenantId());
        detail.setAccentity((String) enterpriseInfo.get("accEntityId"));
        bankRecord.setAccentity((String) enterpriseInfo.get("accEntityId"));
        detail.setEnterpriseBankAccount((String) enterpriseInfo.get("accountId"));
        bankRecord.setBankaccount((String) enterpriseInfo.get("accountId"));
        String dateStr = detailData.getString("tran_date");
        Date tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
        detail.setTranDate(tranDate);
        bankRecord.setTran_date(tranDate);
        bankRecord.setDzdate(tranDate);
        String timeStr = detailData.getString("tran_time");
        if (StringUtils.isNotEmpty(timeStr)) {
            Date tranTime = DateUtils.dateParse(dateStr + timeStr, DateUtils.YYYYMMDDHHMMSS);
            detail.setTranTime(tranTime);
            bankRecord.setTran_time(tranTime);
        }
        String is_refund = detailData.getString("is_refund");
        if(StringUtils.isNotEmpty(is_refund)){
            detail.set("is_refund",is_refund);
            bankRecord.set("is_refund",is_refund);
        }

        detail.setBankseqno(bankSeqNo);
        bankRecord.setBank_seq_no(bankSeqNo);
        bankRecord.setThirdserialno(bankSeqNo);
        String toAcctNo = detailData.getString("to_acct_no");
        detail.setTo_acct_no(toAcctNo);
        bankRecord.setTo_acct_no(toAcctNo);
        String toAcctName = detailData.getString("to_acct_name");
        detail.setTo_acct_name(toAcctName);
        bankRecord.setTo_acct_name(toAcctName);
        String toAcctBank = detailData.getString("to_acct_bank");
        detail.setTo_acct_bank(toAcctBank);
        bankRecord.setTo_acct_bank(toAcctBank);
        String toAcctBankName = detailData.getString("to_acct_bank_name");
        detail.setTo_acct_bank_name(toAcctBankName);
        bankRecord.setTo_acct_bank_name(toAcctBankName);
        String currencyCode = detailData.getString("curr_code");
        //没有币种 取上面currency中的缓存币种
        if (StringUtils.isNotEmpty(currencyCode)) {
            currency = currencyQueryService.getCurrencyByCode(currencyCode);
        }
        detail.setCurrency(currency);
        bankRecord.setCurrency(currency);

        String cashFlag = detailData.getString("cash_flag");
        detail.setCashflag(cashFlag);
        bankRecord.setCash_flag(cashFlag);
        BigDecimal acctBal = detailData.getBigDecimal("acct_bal");
        detail.setAcctbal(acctBal);
        bankRecord.setAcct_bal(acctBal);
        BigDecimal tranAmt = detailData.getBigDecimal("tran_amt");
        detail.setTran_amt(tranAmt);
        bankRecord.setTran_amt(tranAmt);
        String dcFlag = detailData.getString("dc_flag");
        if ("d".equalsIgnoreCase(dcFlag)) {
            detail.setDc_flag(Direction.Debit);
            bankRecord.setDc_flag(Direction.Debit);
            bankRecord.setDebitamount(tranAmt);
        } else if ("c".equalsIgnoreCase(dcFlag)) {
            detail.setDc_flag(Direction.Credit);
            bankRecord.setDc_flag(Direction.Credit);
            bankRecord.setCreditamount(tranAmt);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100094"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E5", "银企联返回的借贷标识非法，请联系开发人员！") /* "银企联返回的借贷标识非法，请联系开发人员！" */);
        }
        String oper = detailData.getString("oper");
        detail.setOper(oper);
        bankRecord.setOper(oper);
        String valueDateStr = detailData.getString("value_date");
        if (StringUtils.isNotEmpty(valueDateStr)) {
            Date valueDate = DateUtils.dateParse(valueDateStr, DateUtils.YYYYMMDD);
            detail.setValue_date(valueDate);
            bankRecord.setValue_date(valueDate);
        }
        String useName = detailData.getString("use_name");
        detail.setUse_name(useName);
        bankRecord.setUse_name(useName);
        String remark = detailData.getString("remark");
        detail.setRemark(remark);
        bankRecord.setRemark(remark);
        String remark01 = detailData.getString("remark01");
        bankRecord.setRemark01(remark01);
        String bankCheckCode = detailData.getString("bank_check_code");  //添加交易流水号 20201125
        detail.setBankdetailno(bankCheckCode);
        // TODO 银行对账问题调试
        bankRecord.setBankcheckno(bankCheckCode);
        // 处理是否存在客商档案
//        MerchantUtils.dealMerchantFlag(detail);
        //国际化新增字段
        BigDecimal rate = detailData.getBigDecimal("rate");
        detail.setRate(rate);

        BigDecimal fee_amt = detailData.getBigDecimal("fee_amt");

        // 新增利息字段
        BigDecimal interest = detailData.getBigDecimal("interest");
        detail.setInterest(interest);
        bankRecord.setInterest(interest);

        String fee_amt_cur = detailData.getString("fee_amt_cur");

        //没有币种 取上面currency中的缓存币种
        if (StringUtils.isNotEmpty(fee_amt_cur)) {
            currency = currencyQueryService.getCurrencyByCode(fee_amt_cur);
        }
        detail.setFee_amt_cur(currency);
        detail.setRemark01(remark01);
        String pay_use_desc = detailData.getString("pay_use_desc");
        detail.setPay_use_desc(pay_use_desc);

        BigDecimal corr_fee_amt = detailData.getBigDecimal("corr_fee_amt");
        detail.setCorr_fee_amt(corr_fee_amt);

        String corr_fee_amt_cur = detailData.getString("corr_fee_amt_cur");
        if (StringUtils.isNotEmpty(currencyCode)) {
            currency = currencyQueryService.getCurrencyByCode(corr_fee_amt_cur);
        }
        detail.setCorr_fee_amt_cur(currency);

        String sub_name = detailData.getString("sub_name");
        detail.setSub_name(sub_name);

        String proj_name = detailData.getString("proj_name");
        detail.setProj_name(proj_name);

        String budget_source = detailData.getString("budget_source");
        detail.setBudget_source(budget_source);

        String voucher_type = detailData.getString("voucher_type");
        detail.setVoucher_type(voucher_type);

        String voucher_no = detailData.getString("voucher_no");
        detail.setVoucher_no(voucher_no);

        detail.setEntityStatus(EntityStatus.Insert);
        bankRecord.setEntityStatus(EntityStatus.Insert);

        Long id = ymsOidGenerator.nextId();
        detail.setId(id);
        bankRecord.setId(id);
        // 添加创建时间、创建日期
        detail.setCreateDate(new Date());
        detail.setCreateTime(new Date());
        bankRecord.setCreateDate(new Date());
        bankRecord.setCreateTime(new Date());

        //对方账号后去空格
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankRecord.getTo_acct_no())) {
            bankRecord.setTo_acct_no(bankRecord.getTo_acct_no().replaceAll(" ", ""));
        }

        //退票匹配
        if (!Short.valueOf(OppositeType.InnerOrg.getValue() + "").equals(bankRecord.getOppositetype())) {
            //20230517先屏蔽，等辨识规则开关添加后再开启
//            checkRefund(bankRecord);
        }
        //2023-11-30 yangjn 添加唯一交易流水验证
        String unique_no = detailData.getString("unique_no");
        detail.setUnique_no(unique_no);
        bankRecord.setUnique_no(unique_no);

        if (bankSeqNo == null || "".equals(bankSeqNo)) {
            bankDealDetails.add(detail);
            bankRecords.add(bankRecord);
        } else if (bankRecord.getBank_seq_no().equals(detail.getBankseqno())) {
            //保证同一个线程内交易明细与对账单数据一致；若不一致则不插入数据
            bankDealDetails.add(detail);
            bankRecords.add(bankRecord);
        }
    }

    /*
     *@Author tongyd
     *@Description 校验交易明细是否重复
     *@Date 2019/6/4 21:31
     *@Param [bankAccountId, bankSeqNo]
     *@Return boolean
     **/
    private boolean checkRepetition(Object bankAccountId, String bankSeqNo, CtmJSONObject detailData) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bankAccountId));
        if (bankSeqNo != null && !"".equals(bankSeqNo)) {
            conditionGroup.appendCondition(QueryCondition.name("bankseqno").eq(bankSeqNo));
        } else {//若银行交易流水号为空 走另一套判断逻辑
            String tran_time = detailData.getString("tran_time");
            String tran_date = detailData.getString("tran_date");
            Date checktranTime = DateUtils.dateParse(tran_date + tran_time, DateUtils.YYYYMMDDHHMMSS);
            conditionGroup.appendCondition(QueryCondition.name("tranTime").eq(checktranTime));
            if (detailData.getString("to_acct_no") != null) {//对方账号
                conditionGroup.appendCondition(QueryCondition.name("to_acct_no").eq(detailData.getString("to_acct_no")));
            }
            if (detailData.getString("dc_flag") != null) {//交易方向
                short dc_flag = 1;
                if ("d".equals(detailData.getString("dc_flag"))) {
                    dc_flag = Direction.Debit.getValue();
                } else {
                    dc_flag = Direction.Credit.getValue();
                }
                conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq(dc_flag));
            }
            if (detailData.getString("tran_amt") != null) {//交易金额
                conditionGroup.appendCondition(QueryCondition.name("tran_amt").eq(detailData.getBigDecimal("tran_amt")));
            }
            if (detailData.getString("remark") != null) {//摘要
                conditionGroup.appendCondition(QueryCondition.name("remark").eq(detailData.getString("remark")));
            }
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> details = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME, schema);
        if (details != null && details.size() > 0) {
            //CZFW-104041 若余额信息为空，则更新该交易明细新抓取的余额信息 add by wxb,只更新第一条数据，若存在多条则为脏数据
            handleUpdateAccbal(details.get(0), bankAccountId, detailData);
            return true;
        }
        return false;
    }

    /**
     * 处理更新余额信息为空，重新拉取时不为空的数据
     */
    private void handleUpdateAccbal(Map<String, Object> detail, Object bankAccountId,
                                    CtmJSONObject detailData) {
        try {
            BankDealDetail bankDealDetail = new BankDealDetail();
            bankDealDetail.init(detail);
            //若当前余额为空，且新拉取余额不为空，则更新银行交易明细和对应银行对账单
            BigDecimal acctBal = detailData.getBigDecimal("acct_bal");
            if (bankDealDetail.getAcctbal() == null && acctBal != null) {
                bankDealDetail.setAcctbal(acctBal);
                bankDealDetail.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, bankDealDetail);
                //查询相关银行对账单
                List<BankReconciliation> bankReconciliations = new ArrayList<>();
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
                group.addCondition(QueryCondition.name("bankaccount").eq(bankAccountId));
                //有交易流水号根据流水号查询，无流水号则另外逻辑
                if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankDealDetail.getBankseqno())) {
                    group.addCondition(QueryCondition.name("bank_seq_no").eq(bankDealDetail.getBankseqno()));
                } else {
                    String dateStr = detailData.getString("tranDate");
                    Date tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
                    group.addCondition(QueryCondition.name("tran_date").eq(tranDate));
                    String timeStr = detailData.getString("tranTime");
                    if (StringUtils.isNotEmpty(timeStr)) {
                        Date tranTime = DateUtils.dateParse(timeStr, DateUtils.YYYYMMDDHHMMSS);
                        group.addCondition(QueryCondition.name("tran_time").eq(tranTime));
                    }
                    if (detailData.getString("tran_amt") != null) {//交易金额
                        group.appendCondition(QueryCondition.name("tran_amt").eq(detailData.getBigDecimal("tran_amt")));
                    }
                    if (detailData.getString("acctbal") != null) {//余额
                        group.appendCondition(QueryCondition.name("acctbal").eq(detailData.getString("acctbal")));
                    }
                    if (detailData.getString("to_acct_no") != null) {//对方账号
                        group.appendCondition(QueryCondition.name("to_acct_no").eq(detailData.getString("to_acct_no")));
                    }
                    if (detailData.getString("to_acct_name") != null) {//对方户名
                        group.appendCondition(QueryCondition.name("to_acct_name").eq(detailData.getString("to_acct_name")));
                    }
                    if (detailData.getString("remark") != null) {//摘要
                        group.appendCondition(QueryCondition.name("remark").eq(detailData.getString("remark")));
                    }
                }
                querySchema.addCondition(group);
                bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                if (bankReconciliations != null && bankReconciliations.size() > 0) {
                    for (BankReconciliation b : bankReconciliations) {
                        if (b.getDataOrigin().getValue() == DateOrigin.DownFromYQL.getValue() && b.getAcct_bal() == null) {
                            b.setAcct_bal(acctBal);
                            b.setEntityStatus(EntityStatus.Update);
                        }
                    }
                    CommonSaveUtils.updateBankReconciliation(bankReconciliations);
                }
            }
        } catch (Exception e) {
            log.error("银行账户交易明细更细余额报错：" + e.getMessage());
        }
    }

    /**
     * 获取银企联接口返回的银行账户 通过银行账户的使用组织赋值
     */
    private EnterpriseBankAcctVO getEnterpriseBankAcctVOByAccount(CtmJSONObject responseBody, boolean realFlag) throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        EnterpriseBankAcctVO enterpriseBankAcctVO = null;
        List<EnterpriseBankAcctVO> bankAccounts = null;
        CtmJSONObject detailData = null;
        int backNum = 0;
        if (realFlag) {// 如果是实时余额返回的是tot_num
            backNum = responseBody.getInteger("tot_num");
        } else { // 如果是其他返回的是back_num
            backNum = responseBody.getInteger("back_num");
        }
        if (backNum == 1) {
            detailData = responseBody.getJSONObject("record");
        }
        if (backNum > 1) {
            CtmJSONArray records = responseBody.getJSONArray("record");
            if (!records.isEmpty()) { // 接口传递的银行账号只有一条 如果返回多条结果集 也只有唯一的一条银行账号
                detailData = records.getJSONObject(0);
            }
        }
        if (detailData != null) {
            EnterpriseParams enterParams = new EnterpriseParams();
            Object acct_no = detailData.get("acct_no");
            if (acct_no != null) {
                enterParams.setAccount(detailData.get("acct_no").toString());
            } else {
                enterParams.setAccount(responseBody.get("acct_no").toString());
            }
            bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterParams);
        }
        if (bankAccounts != null) {
            enterpriseBankAcctVO = bankAccounts.get(0);
        }
        return enterpriseBankAcctVO;
    }


    /**
     * 获取银企联接口返回的银行账户 通过银行账户的使用组织赋值
     */
    private List<EnterpriseBankAcctVO> getEnterpriseBankAcctVOListByAccount(CtmJSONObject responseBody, boolean realFlag) throws Exception {
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        CtmJSONObject detailData = null;
        int backNum = 0;
        if (realFlag) {// 如果是实时余额返回的是tot_num
            backNum = responseBody.getInteger("tot_num");
        } else { // 如果是其他返回的是back_num
            backNum = responseBody.getInteger("back_num");
        }
        CtmJSONArray records = new CtmJSONArray();
        if (backNum < 1) {
            return bankAccounts;
        } else if (backNum == 1) {
            CtmJSONObject record = responseBody.getJSONObject("record");
            records.add(record);
        } else {
            records = responseBody.getJSONArray("record");
        }
        for (int i = 0; i < records.size(); i++) {
            detailData = records.getJSONObject(i);
            EnterpriseParams enterParams = new EnterpriseParams();
            enterParams.setAccount(detailData.get("acct_no").toString());
            bankAccounts.addAll(baseRefRpcService.queryEnterpriseBankAccountByCondition(enterParams));
        }
        return bankAccounts;
    }
}