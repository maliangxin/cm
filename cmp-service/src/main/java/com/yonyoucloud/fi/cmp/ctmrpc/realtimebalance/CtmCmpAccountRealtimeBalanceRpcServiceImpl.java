package com.yonyoucloud.fi.cmp.ctmrpc.realtimebalance;

import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;

import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.realtimebalance.CtmCmpAccountRealtimeBalanceRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.BankReconciliationVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.realtimebalance.AccountRealtimeBalanceVo;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.vo.common.CommonQueryDataVo;
import com.yonyoucloud.fi.stct.api.openapi.IAccountOpenApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.response.AccountQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ITransCodeConstant.QUERY_ACCOUNT_BALANCE;
import java.util.stream.Collectors;
@Service
@Slf4j
public class CtmCmpAccountRealtimeBalanceRpcServiceImpl implements CtmCmpAccountRealtimeBalanceRpcService,com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpAccountRealtimeBalanceRpcService {

    @Autowired
    BankreconciliationService bankreconciliationService;
    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    AccountRealtimeBalanceService accountRealtimeBalanceService;
    private AtomicInteger cardinalNumber = new AtomicInteger(0);

    @Override
    public Map<String, Object> queryAccountRealtimeBalance(CommonRequestDataVo params) throws Exception {
        Map<String, Object> responseMsg = new HashMap<>();
        CtmJSONObject paramsObject = new CtmJSONObject();
        //会计主体
        String accentity = params.getAccentity();
        // 银行账户
        List<String> enterBankAccs = params.getEnterpriseBankAccountList();
        String accEntity = accentity;
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        if (accentity != null) {
            paramsObject.put("accEntity", accentity);
            accEntity = accentity;
            enterpriseParams.setOrgid(accentity);
        }
        //币种
        String currency = params.getCurrency();
        if (currency != null) {
            List<String> currencyList = new ArrayList<>();
            currencyList.add(currency);
            enterpriseParams.setCurrencyIDList(currencyList);
        }
        if (enterBankAccs != null && !enterBankAccs.isEmpty()) {
            paramsObject.put("enterpriseBankAccount", enterBankAccs);
            enterpriseParams.setIdList(enterBankAccs);
        }
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (bankAccounts == null || bankAccounts.size() == 0) {
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DE", "会计主体无有效的企业银行账户") /* "会计主体无有效的企业银行账户" */);
            responseMsg.put("code", 300);
            responseMsg.put("accountBalances", null);
            return responseMsg;
        }
        //存储网银类型的账户信息
        List<EnterpriseBankAcctVO> netAccounts = new ArrayList<>();
        //真正请求查询的网易账户信息
        List<EnterpriseBankAcctVO> netAccountsToHttp = new ArrayList<>();
        List<String> netAccountsId = new ArrayList<>();
        //先查看是否有内部账户 这里存错内部账户信息、内部账户id
        List<EnterpriseBankAcctVO> accounts = new ArrayList<>();
        List<String> accountsId = new ArrayList<>();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            if (bankAccount.getAcctopentype() != null && bankAccount.getAcctopentype().equals(1)) {
                accounts.add(bankAccount);
                accountsId.add(bankAccount.getId());
            } else {//添加非内部账户的信息
                netAccounts.add(bankAccount);
                netAccountsId.add(bankAccount.getId());
            }
        }
        //若有则查询内部账户相关信息
        List<AccountRealtimeBalance> accountBalances = new ArrayList<>();
        if (accounts.size() > 0) {
            try {
                AccountQueryVo accountQueryVo = RemoteDubbo.get(IAccountOpenApiService.class, IDomainConstant.MDD_DOMAIN_STCT).
                        queryAccountBalance(params.getAccentity(), accountsId, AppContext.getYTenantId().toString());
                Result result = accountQueryVo.getResult();
                log.error("===========查询内部账户余额============结果：" + result);

                if (1 == result.getCode().intValue()) {//成功
                    CtmJSONObject resultLines = (CtmJSONObject) result.getData();
                    //跟新相关余额信息
                    if (resultLines != null) {
//                        JSONArray resultLines2 = JSONArray.parseArray(CtmJSONObject.toJSONString(resultLines));
                        accountBalances = getAccountBalanceData(accentity, bankAccounts, resultLines);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        if (netAccountsId.isEmpty()) {
            if (accountBalances.size() > 0) {
                responseMsg.put("code", 200);
                responseMsg.put("accountBalances", accountBalances);
            } else {
                responseMsg.put("code", 300);
                responseMsg.put("accountBalances", null);
            }
            return responseMsg;
        }
        QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(netAccountsId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(true));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> listBankAccountSetting = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if (listBankAccountSetting == null || listBankAccountSetting.size() == 0) {
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BD", "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
            if (accountBalances.size() > 0) {
                responseMsg.put("code", 200);
                responseMsg.put("accountBalances", accountBalances);
            } else {
                responseMsg.put("code", 300);
                responseMsg.put("accountBalances", null);
            }
            return responseMsg;
        }
        // 过滤掉没有启用银企联的银行账号
        List<String> bankAccountSettingList = new ArrayList<>();
        for (Map<String, Object> bankAccountSetting : listBankAccountSetting) {
            bankAccountSettingList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
        }
        for (int i = 0; i < netAccounts.size(); i++) {
            if (bankAccountSettingList.contains(netAccounts.get(i).getId())) {
                netAccountsToHttp.add(netAccounts.get(i));
            }
        }
        String customNo = listBankAccountSetting.get(0).get("customNo").toString();
        //构建params 用于组装请求参数
        paramsObject.put("customNo", customNo);
        paramsObject.put("operator", null);
        paramsObject.put("requestseqno", buildRequestSeqNo(customNo));
        paramsObject.put("signature", null);

        //账户实时余额查询
        paramsObject.put("curr_code", params.getCurrencyCode());
        CtmJSONObject queryBalanceMsg = buildQueryBalanceMsg(paramsObject, netAccountsToHttp);
        log.error("实时余额请求参数==========================》：" + queryBalanceMsg.toString());
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = HttpsUtils.doHttpsPost(QUERY_ACCOUNT_BALANCE, requestData, bankConnectionAdapterContext.getChanPayUri());
        log.error("实时余额响应参数==========================》：" + result.toString());
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA, params);
        logData.put(IMsgConstant.ACCBAL_REQUEST, queryBalanceMsg);
        logData.put(IMsgConstant.ACCBAL_RESPONSE, result);
        /**
         * 不拼接与银企连通信是否成功的信息，因为如果连接错误会直接throw error
         * 如果可以连接通信，但数据有误。会使用户理解有歧义(为什么提示成功了，但还是报错?)
         */
        if (!result.isEmpty() && result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String service_resp_code = responseHead.getString("service_resp_code");
            //无论是否成功 都把信息提示错出来
//            String serviceRespDesc = responseHead.getString("service_resp_desc");
//            responseMsg.put("message",serviceRespDesc);
            if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                int totalNum = responseBody.getInteger("tot_num");
                if (totalNum < 1) {
                    if (accountBalances.size() > 0) {
                        responseMsg.put("code", 200);
                        responseMsg.put("accountBalances", accountBalances);
                    } else {
                        responseMsg.put("code", 300);
                        responseMsg.put("accountBalances", null);
                    }
                } else {
                    responseMsg.put("code", 200);
                    List<AccountRealtimeBalance> accountBalances2 = getAccountBalanceData(accentity, bankAccounts, responseBody);
                    accountBalances.addAll(accountBalances2);
                    responseMsg.put("accountBalances", accountBalances);
                }
            } else {
                if (accountBalances.size() > 0) {
                    responseMsg.put("code", 200);
                    responseMsg.put("accountBalances", accountBalances);
                } else {
                    responseMsg.put("code", 300);
                    responseMsg.put("accountBalances", null);
                }
            }
        } else {
            if (accountBalances.size() > 0) {
                responseMsg.put("code", 200);
                responseMsg.put("accountBalances", accountBalances);
            } else {
                responseMsg.put("code", 300);
                responseMsg.put("accountBalances", null);
            }
        }

        return responseMsg;
    }

    private List<AccountRealtimeBalance> getAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseBody) throws Exception {
        List<AccountRealtimeBalance> balances = new ArrayList<>();
        //用于存储账户id 删除时根据id删除
        List<String> accId = new ArrayList<>();
        int totalNum = responseBody.getInteger("tot_num");
        String currencyCode = "CNY";
        if (totalNum < 1) {
            return null;
        } else if (totalNum == 1) {
            CtmJSONObject record = responseBody.getJSONObject("record");
            if (!record.containsKey("acct_bal") || !record.containsKey("acct_no")) {
                return null;
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
            balance.setEnterpriseBankAccount((String) bankAccount.getId());
            Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankAccount.getId());
            balance.setBanktype(bankAccount2.get("bank").toString());
            if (bankAccount.getBank() != null) {
                balance.setBanktype((String) bankAccount.getBank());
            }
            currencyCode = record.getString("curr_code");
            if (StringUtils.isNotEmpty(currencyCode)) {
                Map<String, Object> currency = getCurrencyByCode(currencyCode);
                balance.setCurrency((String) currency.get("id"));
            } else {
                //若银企联返回报文中没有指定币种 这里默认启用币种查询逻辑
                Map<String, Object> currency = getCurrencyByAccount(bankAccount.getId());
                balance.setCurrency((String) currency.get("id"));

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
            balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
            balance.setEntityStatus(EntityStatus.Insert);

            balances.add(balance);
        } else {
            CtmJSONArray records = responseBody.getJSONArray("record");
            for (int i = 0; i < records.size(); i++) {
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
                balance.setEnterpriseBankAccount(bankAccount.getId());
                Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankAccount.getId());
                balance.setBanktype(bankAccount2.get("bank").toString());
                if (bankAccount.getBank() != null) {
                    balance.setBanktype((String) bankAccount.getBank());
                }
                currencyCode = record.getString("curr_code");
                if (StringUtils.isNotEmpty(currencyCode)) {
                    Map<String, Object> currency = getCurrencyByCode(currencyCode);
                    balance.setCurrency((String) currency.get("id"));
                } else {
                    //若银企联返回报文中没有指定币种 这里默认人民币
                    Map<String, Object> currency = getCurrencyByCode("CNY");
                    balance.setCurrency((String) currency.get("id"));
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
                balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
                balance.setEntityStatus(EntityStatus.Insert);
                balances.add(balance);
            }
        }
        return balances;
    }

    public Map<String, Object> getCurrencyByCode(String currencyCode) throws Exception {
        Object tenantId = AppContext.getTenantId();
        String cacheKey = currencyCode + ":" + tenantId;
        Map<String, Object> result = null;

        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC); /* 暂不修改 须在BaseRefRpcService中添加通过参数查询币种的方式*/
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").eq(currencyCode));
//        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (ValueUtils.isEmpty(query)) {
            return null;
        }
        result = query.get(0);
        return result;
    }

    /**
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<AccountRealtimeBalanceVo> queryAccountBalanceList(CommonRequestDataVo params) throws Exception {
        log.error("查询银行账户实时余额/历史余额-查询自己数据库数据参数params:" + CtmJSONObject.toJSONString(params));
        //会计主体列表
        List<String> accentityList = params.getAccentityList();
        //银行账户列表
        List<String> enterpriseBankAccountList = params.getEnterpriseBankAccountList();
        // 会计主体
        String accentity = params.getAccentity();
        // 银行账户
        String enterBankAcc = params.getBankaccount();
        //批量改造兼容之前的逻辑
        List<String> accentitys = new ArrayList<>();
        List<String> enterpriseBankAccounts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(accentityList)) {
            accentitys.addAll(accentityList);
        }
        if (!CollectionUtils.isEmpty(enterpriseBankAccountList)) {
            enterpriseBankAccounts.addAll(enterpriseBankAccountList);
        }
        if (!StringUtils.isEmpty(accentity)) {
            accentitys.add(accentity);
        }
        if (!StringUtils.isEmpty(enterBankAcc)) {
            enterpriseBankAccounts.add(enterBankAcc);
        }
        // 币种
        String currency = params.getCurrency();
        // 币种list
        List<String> currencyList = params.getCurrencyList();
        // 开始日期
        String startDate = params.getStartDate();
        if (startDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101269"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080110","查询开始日期不能为空") /* "查询开始日期不能为空" */);
        }
        // 结束日期
        String endDate = params.getEndDate();
        if (endDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101270"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080111","查询结束日期不能为空") /* "查询结束日期不能为空" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }
        if (!CollectionUtils.isEmpty(enterpriseBankAccounts)) {
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts));
        }
        if (currency != null) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        if(!CollectionUtils.isEmpty(currencyList)){
            conditionGroup.appendCondition(QueryCondition.name("currency").in(currencyList));
        }
        conditionGroup.appendCondition(QueryCondition.name("balancedate").between(startDate, endDate));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        schema.addOrderBy("balancedate");
        //判断是否追溯，如果需要追溯，则需要往前追加查询
        params.setIsAutoTraceability(params.getIsAutoTraceability()==null?false:params.getIsAutoTraceability());

        List<AccountRealtimeBalance> existBalances;
        if(params.getIsAutoTraceability()){
            //支持溯源查询逻辑
            existBalances = accountRealtimeBalanceService.queryTraceabilityBalance(enterpriseBankAccounts,accentitys,currency,currencyList,startDate, endDate);
        }else {
            //不支持溯源查询逻辑
            existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        }

        List<AccountRealtimeBalanceVo> commonRequestDataVos = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(existBalances)){
            for (AccountRealtimeBalance balance : existBalances) {
                AccountRealtimeBalanceVo accountRealtimeBalance = new AccountRealtimeBalanceVo();
                accountRealtimeBalance.setEnterpriseBankAccount(balance.getEnterpriseBankAccount());
                accountRealtimeBalance.setAcctbal(balance.getAcctbal());
                accountRealtimeBalance.setBalancedate(balance.getBalancedate());
                accountRealtimeBalance.setCurrency(balance.getCurrency());
                accountRealtimeBalance.setAccentity(balance.getAccentity());
                accountRealtimeBalance.setFrzbal(balance.getFrzbal());
                accountRealtimeBalance.setAvlbal(balance.getAvlbal());
                commonRequestDataVos.add(accountRealtimeBalance);
            }
        }
        log.error("查询银行账户实时余额/历史余额-查询自己数据库数据数量为:" + existBalances.size());
        return commonRequestDataVos;
    }

    private QuerySchema buildQuerySchema(List<String> enterpriseBankAccounts, List<String> accentitys, String currency, String startDate,
                                         String endDate, Boolean isAutoTraceability) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }
        if (!CollectionUtils.isEmpty(enterpriseBankAccounts)) {
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts));
        }
        if (currency != null) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        if (!isAutoTraceability) {
            conditionGroup.appendCondition(QueryCondition.name("balancedate").between(startDate, endDate));
            schema.addOrderBy("balancedate");
        }else {
            schema.addOrderBy(new QueryOrderby("balancedate","desc"));
            //查询小于等于开始日期的数据
            conditionGroup.appendCondition(QueryCondition.name("balancedate").elt(startDate));
            schema.addOrderBy(new QueryOrderby("pubts","desc"));
            schema.addPager(0,1);
        }
//        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        return schema;
    }

    @Override
    public List<BankReconciliationVo> queryBankReconciliationList(CommonRequestDataVo params) throws Exception {
        log.error("批量查询银行交易明细-查询bip数据库参数params:" + CtmJSONObject.toJSONString(params));
        //会计主体列表
        List<String> accentitys = params.getAccentityList();
        //银行账户列表
        List<String> enterpriseBankAccounts = params.getEnterpriseBankAccountList();
        // 开始日期
        String startDate = params.getStartDate();
        if (startDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101269"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080110", "查询开始日期不能为空") /* "查询开始日期不能为空" */);
        }
        // 结束日期
        String endDate = params.getEndDate();
        if (endDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101270"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080111", "查询结束日期不能为空") /* "查询结束日期不能为空" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }
        if (!CollectionUtils.isEmpty(enterpriseBankAccounts)) {
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(enterpriseBankAccounts));
        }
        conditionGroup.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        schema.appendQueryCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = PageUtils.logicPageQuery(schema,conditionGroup,BankReconciliation.ENTITY_NAME);
        List<BankReconciliationVo> bankReconciliationVos = new ArrayList<>();
        bankReconciliations.stream().forEach(e -> {
            bankReconciliationVos.add(bankreconciliationService.convertBankReconciliation2BankReconciliationVO(e));
        });
        return bankReconciliationVos;
    }

    public String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    /**
     * 根据银行账户获取币种
     *
     * @param bankAcc
     * @return
     * @throws Exception
     */
    public Map<String, Object> getCurrencyByAccount(String bankAcc) throws Exception {
        Object tenantId = AppContext.getTenantId();
        String cacheKey = bankAcc + ":" + tenantId;
        Map<String, Object> result = null;
        //账户单币种时取单一币种；多币种时取默认币种；若多币种时无默认 则取人民币
        List<Map<String, Object>> currArray = QueryBaseDocUtils.queryBankAcctCurrByBankacc(bankAcc);/* 暂不修改 已登记*/
        if (currArray != null && currArray.size() == 1) {
            result = currArray.get(0);
        } else if (currArray != null && currArray.size() > 1) {
            result = QueryBaseDocUtils.queryBankAcctDefaultCurrByBankacc(bankAcc);/* 暂不修改 已登记*/
        } else if (result == null) {
            result = getCurrencyByCode("CNY");
        }

        return result;
    }

    public CtmJSONObject buildQueryBalanceMsg(CtmJSONObject params, List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        CtmJSONObject requestHead = buildRequestHeadNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE,
                params.getString("operator"),
                params.getString("customNo"),
                params.getString("requestseqno"),
                params.getString("signature"));
        CtmJSONObject requestBody = new CtmJSONObject();
        CtmJSONArray record = new CtmJSONArray();
        Object currCode = params.get("curr_code");
        if (currCode == null) {
            for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
                for (BankAcctCurrencyVO currency : bankAccount.getCurrencyList()) {
                    CtmJSONObject recordDate = new CtmJSONObject();
                    recordDate.put("acct_no", bankAccount.getAccount());
                    recordDate.put("acct_name", bankAccount.getAcctName());
//            HashMap<String,String> currencyMap = paymentService.queryCurrencyCode(bankAccount.getCurrencyList());
//                    if (currency.getEnable() == 1) {
                        recordDate.put("curr_code", currency.getCurrency());// 这里取到的是币种编码
                    //}
                    record.add(recordDate);
                }
            }
        } else {
            for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
                CtmJSONObject recordDate = new CtmJSONObject();
                recordDate.put("acct_no", bankAccount.getAccount());
                recordDate.put("acct_name", bankAccount.getAcctName());
//            HashMap<String,String> currencyMap = paymentService.queryCurrencyCode(bankAccount.getCurrencyList());
                recordDate.put("curr_code", currCode);// 这里取到的是币种编码
                record.add(recordDate);
            }
        }
        requestBody.put("record", record);
        CtmJSONObject placeOrderMsg = new CtmJSONObject();
        placeOrderMsg.put("request_head", requestHead);
        placeOrderMsg.put("request_body", requestBody);
        return placeOrderMsg;
    }

    private CtmJSONObject buildRequestHeadNew(String transCode, String operator, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        return requestHead;
    }


    /**
     * 老的接口
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> queryAccountRealtimeBalance(CommonQueryDataVo params) throws Exception {
        Map<String, Object> responseMsg = new HashMap<>();
        CtmJSONObject paramsObject = new CtmJSONObject();
        //会计主体
        String accentity = params.getAccentity();
        // 银行账户
        List<String> enterBankAccs = params.getEnterpriseBankAccountList();
        String accEntity = accentity;
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        if (accentity != null) {
            paramsObject.put("accEntity", accentity);
            accEntity = accentity;
            enterpriseParams.setOrgid(accentity);
        }
        //币种
        String currency = params.getCurrency();
        if (currency != null) {
            List<String> currencyList = new ArrayList<>();
            currencyList.add(currency);
            enterpriseParams.setCurrencyIDList(currencyList);
        }
        if (enterBankAccs != null && !enterBankAccs.isEmpty()) {
            paramsObject.put("enterpriseBankAccount", enterBankAccs);
            enterpriseParams.setIdList(enterBankAccs);
        }
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (bankAccounts == null || bankAccounts.size() == 0) {
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DE", "会计主体无有效的企业银行账户") /* "会计主体无有效的企业银行账户" */);
            responseMsg.put("code", 300);
            responseMsg.put("accountBalances", null);
            return responseMsg;
        }
        //存储网银类型的账户信息
        List<EnterpriseBankAcctVO> netAccounts = new ArrayList<>();
        //真正请求查询的网易账户信息
        List<EnterpriseBankAcctVO> netAccountsToHttp = new ArrayList<>();
        List<String> netAccountsId = new ArrayList<>();
        //先查看是否有内部账户 这里存错内部账户信息、内部账户id
        List<EnterpriseBankAcctVO> accounts = new ArrayList<>();
        List<String> accountsId = new ArrayList<>();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            if (bankAccount.getAcctopentype() != null && bankAccount.getAcctopentype().equals(1)) {
                accounts.add(bankAccount);
                accountsId.add(bankAccount.getId());
            } else {//添加非内部账户的信息
                netAccounts.add(bankAccount);
                netAccountsId.add(bankAccount.getId());
            }
        }
        //若有则查询内部账户相关信息
        List<AccountRealtimeBalance> accountBalances = new ArrayList<>();
        if (accounts.size() > 0) {
            try {
                AccountQueryVo accountQueryVo = RemoteDubbo.get(IAccountOpenApiService.class, IDomainConstant.MDD_DOMAIN_STCT).
                        queryAccountBalance(params.getAccentity(), accountsId, AppContext.getYTenantId().toString());
                Result result = accountQueryVo.getResult();
                log.error("===========查询内部账户余额============结果：" + result);

                if (1 == result.getCode().intValue()) {//成功
                    CtmJSONObject resultLines = (CtmJSONObject) result.getData();
                    //跟新相关余额信息
                    if (resultLines != null) {
//                        JSONArray resultLines2 = JSONArray.parseArray(CtmJSONObject.toJSONString(resultLines));
                        accountBalances = getAccountBalanceData(accentity, bankAccounts, resultLines);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        if (netAccountsId.isEmpty()) {
            if (accountBalances.size() > 0) {
                responseMsg.put("code", 200);
                responseMsg.put("accountBalances", accountBalances);
            } else {
                responseMsg.put("code", 300);
                responseMsg.put("accountBalances", null);
            }
            return responseMsg;
        }
        QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(netAccountsId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(true));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> listBankAccountSetting = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if (listBankAccountSetting == null || listBankAccountSetting.size() == 0) {
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BD", "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
            if (accountBalances.size() > 0) {
                responseMsg.put("code", 200);
                responseMsg.put("accountBalances", accountBalances);
            } else {
                responseMsg.put("code", 300);
                responseMsg.put("accountBalances", null);
            }
            return responseMsg;
        }
        // 过滤掉没有启用银企联的银行账号
        List<String> bankAccountSettingList = new ArrayList<>();
        for (Map<String, Object> bankAccountSetting : listBankAccountSetting) {
            bankAccountSettingList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
        }
        for (int i = 0; i < netAccounts.size(); i++) {
            if (bankAccountSettingList.contains(netAccounts.get(i).getId())) {
                netAccountsToHttp.add(netAccounts.get(i));
            }
        }
        String customNo = listBankAccountSetting.get(0).get("customNo").toString();
        //构建params 用于组装请求参数
        paramsObject.put("customNo", customNo);
        paramsObject.put("operator", null);
        paramsObject.put("requestseqno", buildRequestSeqNo(customNo));
        paramsObject.put("signature", null);

        //账户实时余额查询
        paramsObject.put("curr_code",params.getCurrencyCode());
        CtmJSONObject queryBalanceMsg = buildQueryBalanceMsg(paramsObject, netAccountsToHttp);
        log.error("实时余额请求参数==========================》：" + queryBalanceMsg.toString());
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = HttpsUtils.doHttpsPost(QUERY_ACCOUNT_BALANCE, requestData, bankConnectionAdapterContext.getChanPayUri());
        log.error("实时余额响应参数==========================》：" + result.toString());
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA, params);
        logData.put(IMsgConstant.ACCBAL_REQUEST, queryBalanceMsg);
        logData.put(IMsgConstant.ACCBAL_RESPONSE, result);
        /**
         * 不拼接与银企连通信是否成功的信息，因为如果连接错误会直接throw error
         * 如果可以连接通信，但数据有误。会使用户理解有歧义(为什么提示成功了，但还是报错?)
         */
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String service_resp_code = responseHead.getString("service_resp_code");
            //无论是否成功 都把信息提示错出来
//            String serviceRespDesc = responseHead.getString("service_resp_desc");
//            responseMsg.put("message",serviceRespDesc);
            if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_BALANCE,service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                int totalNum = responseBody.getInteger("tot_num");
                if (totalNum < 1) {
                    if(accountBalances.size() > 0){
                        responseMsg.put("code", 200);
                        responseMsg.put("accountBalances", accountBalances);
                    }else {
                        responseMsg.put("code", 300);
                        responseMsg.put("accountBalances", null);
                    }
                } else {
                    responseMsg.put("code", 200);
                    List<AccountRealtimeBalance> accountBalances2 = getAccountBalanceData(accentity,bankAccounts,responseBody);
                    accountBalances.addAll(accountBalances2);
                    responseMsg.put("accountBalances", accountBalances);
                }
            }else {
                if(accountBalances.size() > 0){
                    responseMsg.put("code", 200);
                    responseMsg.put("accountBalances", accountBalances);
                }else {
                    responseMsg.put("code", 300);
                    responseMsg.put("accountBalances", null);
                }
            }
        }else {
            if(accountBalances.size() > 0){
                responseMsg.put("code", 200);
                responseMsg.put("accountBalances", accountBalances);
            }else {
                responseMsg.put("code", 300);
                responseMsg.put("accountBalances", null);
            }
        }

        return responseMsg;
    }

    @Override
    public List<AccountRealtimeBalance> queryAccountBalanceList(CommonQueryDataVo params) throws Exception {
        log.error("查询银行账户实时余额/历史余额-查询自己数据库数据参数params:"+CtmJSONObject.toJSONString(params));
        //会计主体列表
        List<String> accentityList = params.getAccentityList();
        //银行账户列表
        List<String> enterpriseBankAccountList = params.getEnterpriseBankAccountList();
        // 会计主体
        String accentity = params.getAccentity();
        // 银行账户
        String enterBankAcc = params.getBankaccount();
        //批量改造兼容之前的逻辑
        List<String> accentitys = new ArrayList<>();
        List<String> enterpriseBankAccounts = new ArrayList<>();
        if(!CollectionUtils.isEmpty(accentityList)){
            accentitys.addAll(accentityList);
        }
        if(!CollectionUtils.isEmpty(enterpriseBankAccountList)){
            enterpriseBankAccounts.addAll(enterpriseBankAccountList);
        }
        if(!StringUtils.isEmpty(accentity)){
            accentitys.add(accentity);
        }
        if(!StringUtils.isEmpty(enterBankAcc)){
            enterpriseBankAccounts.add(enterBankAcc);
        }
        // 币种
        String currency = params.getCurrency();
        // 开始日期
        String startDate = params.getStartDate();
        if(startDate == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101269"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080110", "查询开始日期不能为空") /* "查询开始日期不能为空" */);
        }
        // 结束日期
        String endDate = params.getEndDate();
        if(endDate == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101270"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080111", "查询结束日期不能为空") /* "查询结束日期不能为空" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if(CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)){
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }
        if(!CollectionUtils.isEmpty(enterpriseBankAccounts)){
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts));
        }
        if(currency != null){
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        conditionGroup.appendCondition(QueryCondition.name("balancedate").between(startDate, endDate));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        schema.addOrderBy("balancedate");
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        log.error("查询银行账户实时余额/历史余额-查询自己数据库数据数量为:"+existBalances.size() );
        return existBalances;
    }

    @Override
    public List<BankReconciliation> queryBankReconciliationList(CommonQueryDataVo params) throws Exception {
        log.error("批量查询银行交易明细-查询bip数据库参数params{}", CtmJSONObject.toJSONString(params));
        //会计主体列表
        List<String> accentitys = params.getAccentityList();
        //银行账户列表
        List<String> enterpriseBankAccounts = params.getEnterpriseBankAccountList();
        // 开始日期
        String startDate = params.getStartDate();
        if(startDate == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101269"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080110", "查询开始日期不能为空") /* "查询开始日期不能为空" */);
        }
        // 结束日期
        String endDate = params.getEndDate();
        if(endDate == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101270"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080111", "查询结束日期不能为空") /* "查询结束日期不能为空" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if(CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)){
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }
        if(!CollectionUtils.isEmpty(enterpriseBankAccounts)){
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(enterpriseBankAccounts));
        }
        conditionGroup.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        conditionGroup.appendCondition(QueryCondition.name("tran_amt").is_not_null());
        schema.appendQueryCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        log.error("批量查询银行交易明细-查询bip数据库查询数据量为:{}",bankReconciliations.size());
        return bankReconciliations;
    }
}
