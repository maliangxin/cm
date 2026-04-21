package com.yonyoucloud.fi.cmp.accountbalance;


import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.cashhttp.CashHttpBankEnterpriseLinkVo;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankLinkParam;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CURRENCY;

/**
 * @ClassName AccountRealtimeBalanceServiceImpl
 * @Description 账户实时余额接口实现
 * @Author yangjn
 * @Date 2021/8/25 16:23
 * @Version 1.0
 **/
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class AccountBalanceServiceImpl implements AccountBalanceService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AccountBalanceServiceImpl.class);

    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    AccountRealtimeBalanceService accountRealtimeBalanceService;

    @Autowired
    AccountHistoryBalanceService accountHistoryBalanceService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    CurrencyQueryService currencyQueryService;

    @Autowired
    private BankDealDetailService bankDealDetailService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 银行账户余额查询API
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject batchQueryAccountBalance(CtmJSONObject params) throws Exception {
        CtmJSONObject response = new CtmJSONObject();
        String dataSources = params.getString("data_sources");
        String acctbalType = params.getString("acctbal_type");

        CtmJSONArray data = params.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101392"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080045", "参数错误，账户信息为空") /* "参数错误，账户信息为空" */);
        }
        // acctbalType 余额类型：0当日余额 1历史余额
        // dataSources 数据来源：0实时获取 1已有数据查询
        if ("0".equals(acctbalType)) {
            if ("0".equals(dataSources)) {
               response = realBalanceQueryFromDirectLink(data);
            } else {
               response = realBalanceQueryFromDB(data);
            }
        } else {
            if ("0".equals(dataSources)) {
                response = historyBalanceQueryFromDirectLink(data);
            } else {
                response = historyBalanceQueryFromDB(data);
            }
        }
        return response;
    }

    private CtmJSONObject realBalanceQueryFromDirectLink(CtmJSONArray data) throws Exception {

        CtmJSONObject response = new CtmJSONObject();
        List<CtmJSONObject> balFailList = new ArrayList<>();
        //一个租户只有一个客户号 本系统不做区分
        String customNo = null;
        // 账户实时余额查询 40T20
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = new ArrayList<>();
        Map<String, String> enterpriseBankAcctVOKeyMap = new HashMap<>();
        for (Object jsonObj : data) {
            CtmJSONObject queryParam = CtmJSONObject.toJSON(jsonObj);
            String acctNo = queryParam.getString("acct_no");
            String acctName = queryParam.getString("acct_name");
            String currCode = queryParam.getString("curr_code");
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(currCode);
            if (currencyTenantDTO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003F", "没有有效的币种信息") /* "没有有效的币种信息" */);
                balFailList.add(failJson);
                continue;
            }
            String currency = currencyTenantDTO.getId();
            EnterpriseBankAcctVO enterpriseBankAccount = getAcctByNoAndCurrency(acctNo, currency);
            if (enterpriseBankAccount == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900030", "系统中不存在币种为") /* "系统中不存在币种为" */ + currCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A8", "的账号") /* "的账号" */ + acctNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900031", "请检查！") /* "请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (enterpriseBankAccount.getAcctopentype() != null && enterpriseBankAccount.getAcctopentype().equals(1)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590002E", "不支持内部账户查询") /* "不支持内部账户查询" */);
                balFailList.add(failJson);
                continue;
            }
            String enterpriseBankAccountId = enterpriseBankAccount.getId();
            List<Map<String, Object>> bankAccountSettingList = queryBankAccountSetting(true, enterpriseBankAccountId, enterpriseBankAccount.getOrgid());
            if (CollectionUtils.isEmpty(bankAccountSettingList)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AD", "没有有效的企业银行信息") /* "没有有效的企业银行信息" */);
                balFailList.add(failJson);
                continue;
            }
            Map<String, Object> bankAccountSetting = bankAccountSettingList.get(0);
            customNo = (String) bankAccountSetting.get("customNo");
            if (StringUtils.isEmpty(customNo)) { // 判断银行customNo是否存在
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508004A", "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
                balFailList.add(failJson);
                continue;
            }
            enterpriseBankAcctVOList.add(enterpriseBankAccount);
            String noNameCurrCode = acctNo + ":" + acctName + ":" + currCode;
            enterpriseBankAcctVOKeyMap.put(enterpriseBankAccountId, noNameCurrCode);
        }

        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA, data);
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AE", "账户实时余额查询账户返回") /* "账户实时余额查询账户返回" */, enterpriseBankAcctVOList);
        ctmcmpBusinessLogService.saveBusinessLog(logData, "","", IServicecodeConstant.RETIBALIST, IMsgConstant.QUERY_ACCREALTIMEBAL, IMsgConstant.QUERY_ACCREALTIMEBAL);

        CtmJSONObject queryBalanceMsg = buildQueryRealBalanceMsg(customNo, enterpriseBankAcctVOList);
        CtmJSONObject realBalanceResult = batchQueryRealbalanceBalance(queryBalanceMsg);
        CtmJSONObject retData = realBalanceResult.getJSONObject("data");
        if (retData == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101398"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508004E", "账户实时余额查询错误") /* "账户实时余额查询错误" */);
        }
        CtmJSONObject responseHead = retData.getJSONObject("response_head");
        String serviceRespCode = responseHead.getString("service_resp_code");
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        CtmJSONObject responseBody = null;
        if (!HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, serviceRespCode)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101399"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080050", "账户实时余额查询错误：") /* "账户实时余额查询错误：" */ + serviceRespDesc);
        }
        responseBody = retData.getJSONObject("response_body");
        List<AccountRealtimeBalance> accountRealBalanceData = accountRealtimeBalanceService.getAccountBalanceData(null, enterpriseBankAcctVOList, responseBody);
        // 银企联账户实时余额数据同步到表里
        accountRealtimeBalanceService.insertAccountBalanceData(null, enterpriseBankAcctVOList , responseHead, responseBody, null, null, queryBalanceMsg);

        List<CtmJSONObject> balanceSuccessList = new ArrayList<>();
        SimpleDateFormat dateToStrFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateToStrFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < accountRealBalanceData.size(); i++) {
            AccountRealtimeBalance item = accountRealBalanceData.get(i);
            CtmJSONObject balanceJson = new CtmJSONObject();
            String retStr = enterpriseBankAcctVOKeyMap.get(item.getEnterpriseBankAccount());
            String[] split = retStr.split(":");
            balanceJson.put("acct_no", split[0]);
            balanceJson.put("acct_name", split[1]);
            balanceJson.put("curr_code", split[2]);
            balanceJson.put("avlbal", item.getAvlbal());
            balanceJson.put("acctbal", item.getAcctbal());
            balanceJson.put("frzbal", item.getFrzbal());
            balanceJson.put("balancedate", item.getBalancedate() == null ? "" : dateToStrFormat.format(item.getBalancedate()));
            balanceJson.put("pubts", dateToStrFormat2.format(DateUtils.getNow()));
            balanceSuccessList.add(balanceJson);
        }

        List<CtmJSONObject> balList = new ArrayList<>();
        balList.addAll(balanceSuccessList);
        balList.addAll(balFailList);
        response.put("code", "200");
        response.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AC", "操作成功") /* "操作成功" */);
        response.put("data", balList);
        return response;
    }

    private CtmJSONObject historyBalanceQueryFromDirectLink(CtmJSONArray data) throws Exception {
        CtmJSONObject response = new CtmJSONObject();
        List<CtmJSONObject> balFailList = new ArrayList<>();
        // 账户历史余额数据查询 40T21
        List<AccountRealtimeBalance> historyBalanceResultList = new ArrayList<>();
        Map<String, String> enterpriseBankAcctVOKeyMap = new HashMap<>();
        for (Object jsonObj : data) {
            CtmJSONObject queryParam = CtmJSONObject.toJSON(jsonObj);
            String acctNo = queryParam.getString("acct_no");
            String currCode = queryParam.getString("curr_code");
            String acctName = queryParam.getString("acct_name");
            String beginDateStr = queryParam.getString("begin_date");
            String endDateStr = queryParam.getString("end_date");
            if (StringUtils.isEmpty(beginDateStr) || StringUtils.isEmpty(endDateStr)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080040", "查询历史余额时，开始日期、结束日期需同时有值，请检查！") /* "查询历史余额时，开始日期、结束日期需同时有值，请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            // 兼容20240101这种短横杠形式日期
            if (!beginDateStr.contains("-")) {
                beginDateStr = beginDateStr.substring(0, 4) + "-" + beginDateStr.substring(4, 6) + "-" + beginDateStr.substring(6, 8);
            }
            if (!endDateStr.contains("-")) {
                endDateStr = endDateStr.substring(0, 4) + "-" + endDateStr.substring(4, 6) + "-" + endDateStr.substring(6, 8);
            }
            Date beginDate = DateUtils.dateParse(beginDateStr, "yyyy-MM-dd");
            Date endDate = DateUtils.dateParse(endDateStr, "yyyy-MM-dd");
            Date nowDate = DateUtils.getNowDateShort2();
            int dateBetween = DateUtils.dateBetween(beginDate, endDate);
            if (endDate.compareTo(nowDate) >= 0) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A3", "查询历史余额时，结束日期需早于当前日期，请检查！") /* "查询历史余额时，结束日期需早于当前日期，请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (beginDate.compareTo(endDate) > 0) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A4", "查询历史余额时，结束日期需晚于等于开始日期，请检查！") /* "查询历史余额时，结束日期需晚于等于开始日期，请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (dateBetween > 30) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A6", "查询历史余额时，查询日期跨度不能超过30天！") /* "查询历史余额时，查询日期跨度不能超过30天！" */);
                balFailList.add(failJson);
                continue;
            }
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(currCode);
            if (currencyTenantDTO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003F", "没有有效的币种信息") /* "没有有效的币种信息" */);
                balFailList.add(failJson);
                continue;
            }
            String currency = currencyTenantDTO.getId();
            EnterpriseBankAcctVO enterpriseBankAcctVO = getAcctByNoAndCurrency(acctNo, currency);
            if (enterpriseBankAcctVO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900030", "系统中不存在币种为") /* "系统中不存在币种为" */ + currCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A8", "的账号") /* "的账号" */ + acctNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900031", "请检查！") /* "请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590002E", "不支持内部账户查询") /* "不支持内部账户查询" */);
                balFailList.add(failJson);
                continue;
            }

            String orgid = enterpriseBankAcctVO.getOrgid();
            String enterpriseBankAcctVOId = enterpriseBankAcctVO.getId();

            queryParam.put("accentity", orgid);
            queryParam.put("enterpriseBankAccount", enterpriseBankAcctVO.getId());
            queryParam.put("currencyCode", queryParam.getString("curr_code"));
            CtmJSONObject historyBalanceResult = batchQueryHistorybalanceBalance(queryParam);
            CtmJSONObject retData = historyBalanceResult.getJSONObject("data");
            if (retData == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003E", "账户历史余额查询错误") /* "账户历史余额查询错误" */);
                balFailList.add(failJson);
                continue;
            }
            String serviceRespCode = retData.getJSONObject("response_head").getString("service_resp_code");
            String serviceRespDesc = retData.getJSONObject("response_head").getString("service_resp_desc");
            if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE, serviceRespCode)) {

                QuerySchema schema = QuerySchema.create().addSelect("*");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAcctVOId));
                conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
                conditionGroup.appendCondition(QueryCondition.name("balancedate").egt(beginDate));
                conditionGroup.appendCondition(QueryCondition.name("balancedate").elt(endDate));
                conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
                schema.addCondition(conditionGroup);
                schema.addOrderBy("balancedate");
                List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
                if (existBalances == null || existBalances.isEmpty()) {
                    CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A2", "未查询到账户历史余额数据") /* "未查询到账户历史余额数据" */);
                    balFailList.add(failJson);
                    continue;
                }
                historyBalanceResultList.addAll(existBalances);

                String noNameCurrCode = acctNo + ":" + acctName + ":" + currCode;
                enterpriseBankAcctVOKeyMap.put(enterpriseBankAcctVOId, noNameCurrCode);
            } else {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A5", "账户历史余额查询错误：") /* "账户历史余额查询错误：" */ + serviceRespDesc);
                balFailList.add(failJson);
            }
        }


        List<CtmJSONObject> balanceSuccessList = new ArrayList<>();
        SimpleDateFormat dateToStrFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateToStrFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < historyBalanceResultList.size(); i++) {
            AccountRealtimeBalance item = historyBalanceResultList.get(i);
            CtmJSONObject balanceJson = new CtmJSONObject();
            String retStr = enterpriseBankAcctVOKeyMap.get(item.getEnterpriseBankAccount());
            String[] split = retStr.split(":");
            balanceJson.put("acct_no", split[0]);
            balanceJson.put("acct_name", split[1]);
            balanceJson.put("curr_code", split[2]);
            balanceJson.put("avlbal", item.getAvlbal());
            balanceJson.put("acctbal", item.getAcctbal());
            balanceJson.put("frzbal", item.getFrzbal());
            balanceJson.put("balancedate", item.getBalancedate() == null ? "" : dateToStrFormat.format(item.getBalancedate()));
            balanceJson.put("pubts", item.getPubts() == null ? "" : dateToStrFormat2.format(item.getPubts()));
            balanceSuccessList.add(balanceJson);
        }

        List<CtmJSONObject> balList = new ArrayList<>();
        balList.addAll(balanceSuccessList);
        balList.addAll(balFailList);
        response.put("code", "200");
        response.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AC", "操作成功") /* "操作成功" */);
        response.put("data", balList);
        return response;
    }

    private CtmJSONObject realBalanceQueryFromDB(CtmJSONArray data) throws Exception {
        CtmJSONObject response = new CtmJSONObject();
        List<CtmJSONObject> balFailList = new ArrayList<>();
        List<AccountRealtimeBalance> realBalanceResultList = new ArrayList<>();
        Map<String, String> enterpriseBankAcctVOKeyMap = new HashMap<>();
        for (Object jsonObj : data) {
            CtmJSONObject queryParam = CtmJSONObject.toJSON(jsonObj);
            String acctNo = queryParam.getString("acct_no");
            String acctName = queryParam.getString("acct_name");
            String currCode = queryParam.getString("curr_code");
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(currCode);
            if (currencyTenantDTO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003F", "没有有效的币种信息") /* "没有有效的币种信息" */);
                balFailList.add(failJson);
                continue;
            }
            String currency = currencyTenantDTO.getId();
            EnterpriseBankAcctVO enterpriseBankAcctVO = getAcctByNoAndCurrency(acctNo, currency);
            if (enterpriseBankAcctVO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AF", "未查询到账户信息") /* "未查询到账户信息" */);
                balFailList.add(failJson);
                continue;
            }
            //if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
            //    CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, "不支持内部账户查询");
            //    balFailList.add(failJson);
            //    continue;
            //}

            String enterpriseBankAccountId = enterpriseBankAcctVO.getId();

            // 查数据库中账户实时余额
            QuerySchema balanceSchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup balanceConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            balanceConditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccountId));
            balanceConditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
            balanceConditionGroup.appendCondition(QueryCondition.name("balancedate").eq(LocalDateUtil.getNowDateString()));
            //只查询最新的实时余额数据
            balanceConditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
            //不限定数据来源
            //balanceConditionGroup.appendCondition(QueryCondition.name("datasource").eq("6"));
            balanceSchema.addCondition(balanceConditionGroup);
            List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, balanceSchema, null);
            if (existBalances == null || existBalances.isEmpty()) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AA", "未查询到账户实时余额数据") /* "未查询到账户实时余额数据" */);
                balFailList.add(failJson);
                continue;
            }
            AccountRealtimeBalance realBalanceToday = existBalances.get(0);
            realBalanceResultList.add(realBalanceToday);

            String noNameCurrCode = acctNo + ":" + acctName + ":" + currCode;
            enterpriseBankAcctVOKeyMap.put(enterpriseBankAccountId, noNameCurrCode);
        }

        List<CtmJSONObject> balanceSuccessList = new ArrayList<>();
        SimpleDateFormat dateToStrFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateToStrFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < realBalanceResultList.size(); i++) {
            AccountRealtimeBalance item = realBalanceResultList.get(i);
            CtmJSONObject balanceJson = new CtmJSONObject();
            String retStr = enterpriseBankAcctVOKeyMap.get(item.getEnterpriseBankAccount());
            String[] split = retStr.split(":");
            balanceJson.put("acct_no", split[0]);
            balanceJson.put("acct_name", split[1]);
            balanceJson.put("curr_code", split[2]);
            balanceJson.put("avlbal", item.getAvlbal());
            balanceJson.put("acctbal", item.getAcctbal());
            balanceJson.put("frzbal", item.getFrzbal());
            balanceJson.put("balancedate", item.getBalancedate() == null ? "" : dateToStrFormat.format(item.getBalancedate()));
            balanceJson.put("pubts", item.getPubts() == null ? "" : dateToStrFormat2.format(item.getPubts()));
            balanceSuccessList.add(balanceJson);
        }

        List<CtmJSONObject> balList = new ArrayList<>();
        balList.addAll(balanceSuccessList);
        balList.addAll(balFailList);
        response.put("code", "200");
        response.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AC", "操作成功") /* "操作成功" */);
        response.put("data", balList);
        return response;
    }

    private CtmJSONObject historyBalanceQueryFromDB(CtmJSONArray data) throws Exception {
        CtmJSONObject response = new CtmJSONObject();
        List<CtmJSONObject> balFailList = new ArrayList<>();
        // 查数据库中账户历史余额
        List<AccountRealtimeBalance> historyBalanceResultList = new ArrayList<>();
        Map<String, String> enterpriseBankAcctVOKeyMap = new HashMap<>();
        for (Object jsonObj : data) {
            CtmJSONObject queryParam = CtmJSONObject.toJSON(jsonObj);
            String acctNo = queryParam.getString("acct_no");
            String acctName = queryParam.getString("acct_name");
            String currCode = queryParam.getString("curr_code");
            String beginDateStr = queryParam.getString("begin_date");
            String endDateStr = queryParam.getString("end_date");
            if (StringUtils.isEmpty(beginDateStr) || StringUtils.isEmpty(endDateStr)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080040", "查询历史余额时，开始日期、结束日期需同时有值，请检查！") /* "查询历史余额时，开始日期、结束日期需同时有值，请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (!beginDateStr.contains("-")) {
                beginDateStr = beginDateStr.substring(0, 4) + "-" + beginDateStr.substring(4, 6) + "-" + beginDateStr.substring(6, 8);
            }
            if (!endDateStr.contains("-")) {
                endDateStr = endDateStr.substring(0, 4) + "-" + endDateStr.substring(4, 6) + "-" + endDateStr.substring(6, 8);
            }
            Date beginDate = DateUtils.dateParse(beginDateStr, "yyyy-MM-dd");
            Date endDate = DateUtils.dateParse(endDateStr, "yyyy-MM-dd");
            Date nowDate = DateUtils.getNowDateShort2();
            int dateBetween = DateUtils.dateBetween(beginDate, endDate);
            if (endDate.compareTo(nowDate) >= 0) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A3", "查询历史余额时，结束日期需早于当前日期，请检查！") /* "查询历史余额时，结束日期需早于当前日期，请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (beginDate.compareTo(endDate) > 0) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A4", "查询历史余额时，结束日期需晚于等于开始日期，请检查！") /* "查询历史余额时，结束日期需晚于等于开始日期，请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            if (dateBetween > 30) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A6", "查询历史余额时，查询日期跨度不能超过30天！") /* "查询历史余额时，查询日期跨度不能超过30天！" */);
                balFailList.add(failJson);
                continue;
            }
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(currCode);
            if (currencyTenantDTO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003F", "没有有效的币种信息") /* "没有有效的币种信息" */);
                balFailList.add(failJson);
                continue;
            }
            String currency = currencyTenantDTO.getId();
            EnterpriseBankAcctVO enterpriseBankAcctVO = getAcctByNoAndCurrency(acctNo, currency);
            if (enterpriseBankAcctVO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900030", "系统中不存在币种为") /* "系统中不存在币种为" */ + currCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A8", "的账号") /* "的账号" */ + acctNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900031", "请检查！") /* "请检查！" */);
                balFailList.add(failJson);
                continue;
            }
            //if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
            //    CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, "不支持内部账户查询");
            //    balFailList.add(failJson);
            //    continue;
            //}

            String enterpriseBankAcctVOId = enterpriseBankAcctVO.getId();

            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAcctVOId));
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").egt(beginDate));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").elt(endDate));
            conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
            schema.addCondition(conditionGroup);
            schema.addOrderBy("balancedate");
            List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
            if (existBalances == null || existBalances.isEmpty()) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A2", "未查询到账户历史余额数据") /* "未查询到账户历史余额数据" */);
                balFailList.add(failJson);
                continue;
            }
            historyBalanceResultList.addAll(existBalances);
            String noNameCurrCode = acctNo + ":" + acctName + ":" + currCode;
            enterpriseBankAcctVOKeyMap.put(enterpriseBankAcctVOId, noNameCurrCode);
        }

        List<CtmJSONObject> balanceSuccessList = new ArrayList<>();
        SimpleDateFormat dateToStrFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateToStrFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < historyBalanceResultList.size(); i++) {
            AccountRealtimeBalance item = historyBalanceResultList.get(i);
            CtmJSONObject balanceJson = new CtmJSONObject();
            String retStr = enterpriseBankAcctVOKeyMap.get(item.getEnterpriseBankAccount());
            String[] split = retStr.split(":");
            balanceJson.put("acct_no", split[0]);
            balanceJson.put("acct_name", split[1]);
            balanceJson.put("curr_code", split[2]);
            balanceJson.put("avlbal", item.getAvlbal());
            balanceJson.put("acctbal", item.getAcctbal());
            balanceJson.put("frzbal", item.getFrzbal());
            balanceJson.put("balancedate", item.getBalancedate() == null ? "" : dateToStrFormat.format(item.getBalancedate()));
            balanceJson.put("pubts", item.getPubts() == null ? "" : dateToStrFormat2.format(item.getPubts()));
            balanceSuccessList.add(balanceJson);
        }

        List<CtmJSONObject> balList = new ArrayList<>();
        balList.addAll(balanceSuccessList);
        balList.addAll(balFailList);
        response.put("code", "200");
        response.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AC", "操作成功") /* "操作成功" */);
        response.put("data", balList);
        return response;
    }


    @Override
    public CtmJSONObject batchQueryAccountTransactionDetail(CtmJSONObject params) throws Exception {
        CtmJSONObject response = new CtmJSONObject();
        List<Map<String, Object>> acctDetailFailList = new ArrayList<>();
        CtmJSONArray data = params.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101392"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080045", "参数错误，账户信息为空") /* "参数错误，账户信息为空" */);
        }

        List<Map<String, Object>> accountTransDetailList = new ArrayList<>();
        Map<String, String> enterpriseBankAcctVOKeyMap = new HashMap<>();
        for (Object jsonObj : data) {
            CtmJSONObject queryParams = CtmJSONObject.toJSON(jsonObj);
            String acctNo = queryParams.getString("acct_no");
            String acctName = queryParams.getString("acct_name");
            String currCode = queryParams.getString("curr_code");
            String beginDateStr = queryParams.getString("begin_date");
            String endDateStr = queryParams.getString("end_date");
            if (StringUtils.isEmpty(beginDateStr) || StringUtils.isEmpty(endDateStr)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508004B", "查询交易明细时，开始日期、结束日期不允许为空，请检查！") /* "查询交易明细时，开始日期、结束日期不允许为空，请检查！" */);
                acctDetailFailList.add(failJson);
                continue;
            }
            if (!beginDateStr.contains("-")) {
                beginDateStr = beginDateStr.substring(0, 4) + "-" + beginDateStr.substring(4, 6) + "-" + beginDateStr.substring(6, 8);
            }
            if (!endDateStr.contains("-")) {
                endDateStr = endDateStr.substring(0, 4) + "-" + endDateStr.substring(4, 6) + "-" + endDateStr.substring(6, 8);
            }
            Date beginDate = DateUtils.dateParse(beginDateStr, "yyyy-MM-dd");
            Date endDate = DateUtils.dateParse(endDateStr, "yyyy-MM-dd");
            Date nowDate = DateUtils.getNowDateShort2();
            int dateBetween = DateUtils.dateBetween(beginDate, endDate);
            if (beginDate.compareTo(endDate) > 0) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A7", "查询交易明细时，结束日期不能早于开始日期，请检查！") /* "查询交易明细时，结束日期不能早于开始日期，请检查！" */);
                acctDetailFailList.add(failJson);
                continue;
            }
            if (endDate.compareTo(nowDate) > 0) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A9", "查询交易明细时，结束日期不能晚于当前日期，请检查！") /* "查询交易明细时，结束日期不能晚于当前日期，请检查！" */);
                acctDetailFailList.add(failJson);
                continue;
            }
            if (dateBetween > 30) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AB", "查询交易明细时，查询日期跨度不能超过30天！") /* "查询交易明细时，查询日期跨度不能超过30天！" */);
                acctDetailFailList.add(failJson);
                continue;
            }

            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findByCode(currCode);
            if (currencyTenantDTO == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003F", "没有有效的币种信息") /* "没有有效的币种信息" */);
                acctDetailFailList.add(failJson);
                continue;
            }
            String currency = currencyTenantDTO.getId();
            EnterpriseBankAcctVO enterpriseBankAccount = getAcctByNoAndCurrency(acctNo, currency);
            if (enterpriseBankAccount == null) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900030", "系统中不存在币种为") /* "系统中不存在币种为" */ + currCode + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A8", "的账号") /* "的账号" */ + acctNo + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900031", "请检查！") /* "请检查！" */);
                acctDetailFailList.add(failJson);
                continue;
            }
            if (enterpriseBankAccount.getAcctopentype() != null && enterpriseBankAccount.getAcctopentype().equals(1)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590002E", "不支持内部账户查询") /* "不支持内部账户查询" */);
                acctDetailFailList.add(failJson);
                continue;
            }

            String enterpriseBankAccountId = enterpriseBankAccount.getId();

            BankLinkParam paramBankLink = new BankLinkParam();
            paramBankLink.setAccountId(enterpriseBankAccountId);
            paramBankLink.setAcct_no(acctNo);
            paramBankLink.setAcct_name(enterpriseBankAccount.getAcctName());
            paramBankLink.setBegNum(1);
            paramBankLink.setStartDate(beginDateStr); // 开始日期
            paramBankLink.setEndDate(endDateStr); // 结束日期
            bankDealDetailService.batchQueryTransactionDetailForRpc(paramBankLink);
            List<Map<String, Object>> detailList = queryBankAccountDealDetail(enterpriseBankAccountId, null, currency, beginDateStr, endDateStr);
            if (CollectionUtils.isEmpty(detailList)) {
                CtmJSONObject failJson = getQueryFailJson(acctNo, acctName, currCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A1", "未查询到账户交易明细数据") /* "未查询到账户交易明细数据" */);
                acctDetailFailList.add(failJson);
                continue;
            }
            accountTransDetailList.addAll(detailList);

            String noNameCurrCode = acctNo + ":" + acctName + ":" + currCode;
            enterpriseBankAcctVOKeyMap.put(enterpriseBankAccountId, noNameCurrCode);
        }
        List<Map<String, Object>> acctDetailSuccessList = new ArrayList<>();
        SimpleDateFormat dateToStrFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateToStrFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < accountTransDetailList.size(); i++) {
            Map<String, Object> item = accountTransDetailList.get(i);
            Map<String, Object> acctDetail = new HashMap<>();
            String retStr = enterpriseBankAcctVOKeyMap.get(item.get("enterpriseBankAccount"));
            String[] split = retStr.split(":");
            acctDetail.put("acct_no", split[0]);
            acctDetail.put("acct_name", split[1]);
            acctDetail.put("curr_code", split[2]);
            acctDetail.put("tran_date", item.get("tranDate") == null ? "" : dateToStrFormat.format(item.get("tranDate")));
            acctDetail.put("tran_time", item.get("tranTime") == null ? "" : dateToStrFormat2.format(item.get("tranTime")));
            acctDetail.put("dc_flag", item.getOrDefault("dc_flag", ""));
            acctDetail.put("bank_seq_no", item.getOrDefault("bankseqno", ""));
            acctDetail.put("to_acct_no", item.getOrDefault("to_acct_no", ""));
            acctDetail.put("to_acct_name", item.getOrDefault("to_acct_name", ""));
            acctDetail.put("to_acct_bank", item.getOrDefault("to_acct_bank", ""));
            acctDetail.put("to_acct_bank_name", item.getOrDefault("to_acct_bank_name", ""));
            acctDetail.put("cash_flag", item.getOrDefault("cashflag", ""));
            acctDetail.put("acct_bal", item.getOrDefault("acctbal", 0L));
            acctDetail.put("tran_amt", item.getOrDefault("tran_amt", 0L));
            acctDetail.put("oper", item.getOrDefault("oper", ""));
            acctDetail.put("value_date", item.get("value_date") == null ? "" : dateToStrFormat.format(item.get("value_date")));
            acctDetail.put("use_name", item.getOrDefault("use_name", ""));
            acctDetail.put("remark", item.getOrDefault("remark", ""));
            acctDetail.put("bank_check_code", item.getOrDefault("bankcheckno", ""));
            acctDetail.put("remark01", item.getOrDefault("remark01", ""));
            acctDetail.put("pay_use_desc", item.getOrDefault("pay_use_desc", ""));
            acctDetail.put("pubts", item.get("pubts") == null ? "" : dateToStrFormat2.format(item.get("pubts")));
            acctDetailSuccessList.add(acctDetail);
        }

        List<Map<String, Object>> acctDetailList = new ArrayList<>();
        acctDetailList.addAll(acctDetailSuccessList);
        acctDetailList.addAll(acctDetailFailList);
        response.put("code", "200");
        response.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003AC", "操作成功") /* "操作成功" */);
        response.put("data", acctDetailList);
        return response;
    }

    // 银企联获取实时余额接口
    public CtmJSONObject batchQueryRealbalanceBalance(CtmJSONObject queryBalanceMsg) throws Exception {
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, requestData,
                bankConnectionAdapterContext.getChanPayUri());
        log.error("实时余额请求返回 无ukey==========================》：" + result);
        if (result.getInteger("code") != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101411"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508003D", "银企联查询报错") /* "银企联查询报错" */);
        }
        return result;
    }

    // 银企联获取历史余额接口
    public CtmJSONObject batchQueryHistorybalanceBalance(CtmJSONObject params) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        String enterpriseBankAccount = params.getString("enterpriseBankAccount");
        String accentity = params.getString("accentity");//会计主体
        String curr_code = params.getString("currencyCode");//币种编码
        if (org.apache.commons.lang3.StringUtils.isBlank(accentity) || org.apache.commons.lang3.StringUtils.isBlank(enterpriseBankAccount)) {
            responseMsg.put("code", "00001");
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E2","accentity或者enterpriseBankAccount为空") /* "accentity或者enterpriseBankAccount为空" */);
            return responseMsg;
        }
        List<Map<String, Object>> bankAccountSettinges = queryBankAccountSetting(true, enterpriseBankAccount,accentity);// 查询银行信息
        log.info("bankAccountSettinges info");
        if (!CollectionUtils.isEmpty(bankAccountSettinges)) {
            Map<String, Object> bankAccountSetting = bankAccountSettinges.get(0);
            if (null == bankAccountSetting.get("bankId") || bankAccountSetting.get("bankId").toString().equals("")) {// 判断银行账户是否存在
                responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage("P_YS_FI_CM_0000026013") /* "会计主体无有效的企业银行账户" */);
                responseMsg.put("code", "00002");
                return responseMsg;
            }
            if (null == bankAccountSetting.get("customNo")
                    || bankAccountSetting.get("customNo").toString().equals("")) {// 判断银行customNo是否存在
                responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage("P_YS_FI_CM_0000026099") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
                responseMsg.put("code", "00002");
                return responseMsg;
            }
            CashHttpBankEnterpriseLinkVo cashVO = new CashHttpBankEnterpriseLinkVo();
            cashVO.setAccentity(accentity);
            cashVO.setEnterpriseBankAccount(enterpriseBankAccount);
            cashVO.setAcct_no(params.getString("acct_no"));
            cashVO.setAcct_name(params.getString("acct_name"));
            cashVO.setBank(bankAccountSetting.get("bankId").toString());
            cashVO.setCurr_code(curr_code);
            cashVO.setCustomNo(bankAccountSetting.get("customNo").toString());
            cashVO.setBeg_date(params.getString("begin_date").replace("-",""));
            cashVO.setEnd_date(params.getString("end_date").replace("-",""));
            String channel = AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel();
            cashVO.setChannel(channel);

            CtmJSONObject queryBalanceMsg = accountHistoryBalanceService.buildQueryHistoryBalanceMsg(cashVO);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            CtmJSONObject result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE, requestData,
                    bankConnectionAdapterContext.getChanPayUri());
            log.error("历史余额请求返回 无ukey==========================》：" + result);
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE,service_resp_code)) {
                    CtmJSONObject responseData = result.getJSONObject("data");
                    CtmJSONObject responseBody = responseData.getJSONObject("response_body");
                    responseData.put("response_body", responseBody);
                    result.put("data", responseData);
                    // 银企联账户历史余额数据同步到表里
                    accountHistoryBalanceService.insertAccountHistoryBalanceData(cashVO, responseBody, null);
                }
            }
            return result;
        } else {
            responseMsg.put("code", "00003");
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CB","没有有效的银行账户信息") /* "没有有效的银行账户信息" */);
        }
        return responseMsg;
    }

    private EnterpriseBankAcctVO getAcctByNoAndCurrency(String acctNo, String currency) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        // 过滤停用账户
        List<Integer> enables = new ArrayList<>();
        enables.add(1);
        enterpriseParams.setEnables(enables);
        enterpriseParams.setCurrencyEnable(1);
        enterpriseParams.setAccount(acctNo);
        List<String> currencyList = new ArrayList<>();
        currencyList.add(currency);
        enterpriseParams.setCurrencyIDList(currencyList);
        List<EnterpriseBankAcctVO> acctList = enterpriseBankQueryService.query(enterpriseParams);
        if (acctList == null || acctList.isEmpty()) {
            return null;
        }
        return acctList.get(0);
    }


    /**
     * 具体替换
     *
     * @param record
     * @param enterpriseBankAccount
     * @return
     * @throws Exception
     */
    private void modifyRecord(CtmJSONObject record, String enterpriseBankAccount ,String curr_code) throws Exception {
        String currencyCode = record.getString("curr_code");
        if (org.apache.commons.lang3.StringUtils.isBlank(currencyCode)) {
            currencyCode = curr_code;
        }
        String currency = currencyQueryService.getCurrencyByCode(currencyCode);
        if (null != currency) {
            record.put(CURRENCY, currency);
            record.put("enterpriseBankAccount", enterpriseBankAccount);
        } else {
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101412"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EA", "根据币种编码未获取到币种！") /* "根据币种编码未获取到币种！" */ + "currencyCode  :" + currencyCode);
            throw new CtmException(ErrorMsgUtil.getCurrencyCodeWrongMsg(currencyCode));

        }
    }


    public List<Map<String, Object>> queryBankAccountSetting(boolean isContain, String enterpriseBankAccount, String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(
                " id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.name as name,"
                        + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank,"
                        + "customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.enable as enable");// 判断银行账户表是否为空故多差一个id
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        if (isContain) {
            if (!org.apache.commons.lang3.StringUtils.isBlank(enterpriseBankAccount)) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
            }else {
                // 账户共享之后，有银行账户的情况下，不用在拼接会计主体
                conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
            }
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        return list;
    }

    private CtmJSONObject buildQueryRealBalanceMsg(String customNo, List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        if (CollectionUtils.isEmpty(bankAccounts)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101396"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080046", "没有有效的企业银行账户信息") /* "没有有效的企业银行账户信息" */);
        }
        String transCode = ITransCodeConstant.QUERY_ACCOUNT_BALANCE;
        String requestseqno = DigitalSignatureUtils.buildRequestNum(customNo);

        // 构建银企联请求头
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("tran_code", transCode);

        // 构建银企联请求体
        CtmJSONArray record = new CtmJSONArray();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            CtmJSONObject recordDate = new CtmJSONObject();
            recordDate.put("acct_no", bankAccount.getAccount());
            recordDate.put("acct_name", bankAccount.getAcctName());
            List<BankAcctCurrencyVO> currencyList = bankAccount.getCurrencyList();
            String currency = currencyList.get(0).getCurrency();
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currency);
            recordDate.put("curr_code", currencyTenantDTO.getCode());// 这里取到的是币种编码
            record.add(recordDate);
        }
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("record", record);

        CtmJSONObject placeOrderMsg = new CtmJSONObject();
        placeOrderMsg.put("request_head", requestHead);
        placeOrderMsg.put("request_body", requestBody);
        return placeOrderMsg;
    }


    public List<Map<String, Object>> queryBankAccountDealDetail(String enterpriseBankAccount, String accentity, String currency, String beginDate, String endDate) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        if (!StringUtils.isEmpty(accentity)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").eq((accentity)));
        }
        if (!StringUtils.isEmpty(currency)) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq((currency)));
        }
        SimpleDateFormat format = new SimpleDateFormat(DateUtils.DATE_PATTERN);
        if (!StringUtils.isEmpty(beginDate)) {
            conditionGroup.addCondition(QueryCondition.name("tranDate").egt(format.parse(beginDate)));
        }
        if (!StringUtils.isEmpty(endDate)) {
            conditionGroup.addCondition(QueryCondition.name("tranDate").elt(format.parse(endDate)));
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> detailMapList = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME, schema);
        return detailMapList;
    }

    private CtmJSONObject getQueryFailJson(String acctNo, String acctName, String currCode, String errorMsg) {
        CtmJSONObject failJson = new CtmJSONObject();
        failJson.put("acct_no", acctNo);
        failJson.put("acct_name", acctName);
        failJson.put("curr_code", currCode);
        failJson.put("errorMsg", errorMsg);
        return failJson;
    }
}
