package com.yonyoucloud.fi.cmp.openapi.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiAccountBalanceService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 余额导入查询
 */
@Service
@Slf4j
public class OpenApiAccountBalanceServiceImpl implements OpenApiAccountBalanceService {

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    private YmsOidGenerator ymsOidGenerator;


    @Override
    public CtmJSONObject batchSaveAccountBalance(CtmJSONArray jsonArray) throws Exception {
        int size = jsonArray.size();
        if (jsonArray.isEmpty()) {
            CtmJSONObject resmap = new CtmJSONObject();
            resmap.put("code", 999);
            resmap.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400600", "参数不能为空！") /* "参数不能为空！" */);
            return resmap;
        }
        // 新增数据
        List<AccountRealtimeBalance> addlist = new ArrayList<>();
        // 异常数据
        List<CtmJSONObject> errmsglist = new ArrayList<>();
        List<CtmJSONObject> successlist = new ArrayList<>();
        List<CtmJSONObject> paramList = jsonArray.toJavaList(CtmJSONObject.class);
        // Map<生成对象id,接口传参对象> 用于判断余额确认的数据
        HashMap<String, CtmJSONObject> idparamMap = new HashMap<>();
        for (CtmJSONObject jsonObject : paramList) {
            // 校验参数 初始化对象
            CtmJSONObject msg = new CtmJSONObject();
            // 校验参数并把json转为余额对象
            tranfor2AccountRealtimeBalance(jsonObject, addlist, msg, idparamMap);
            if (msg.get("msg") != null) {
                errmsglist.add(msg);
            } else {
                successlist.add(msg);
            }
        }
        // 删除并新增
        List<String> confirmIdList = new ArrayList<>();
        if (!addlist.isEmpty()) {
            delAndInert(addlist, confirmIdList);
        }

        if (!confirmIdList.isEmpty()) {
            for (String id : confirmIdList) {
                CtmJSONObject ctmJSONObject = idparamMap.get(id);
                ctmJSONObject.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005FF", "余额确认，不允许更新！") /* "余额确认，不允许更新！" */);
                errmsglist.add(ctmJSONObject);
            }
        }

        // 过滤掉确认的余额
        List<CtmJSONObject> success = successlist.stream().filter(a -> !confirmIdList.contains(a.getString("id"))).collect(Collectors.toList());

        CtmJSONObject res = new CtmJSONObject();
        res.put("successnum", size - errmsglist.size());
        res.put("faildnum", errmsglist.size());
        res.put("success", success);
        res.put("falid", errmsglist);
        return res;
    }

    private void delAndInert(List<AccountRealtimeBalance> balances, List<String> confirmIdList) throws Exception {
        Set<String> accentitylist = new HashSet<>();
        Set<String> enterpriseBankAccountlist = new HashSet<>();
        Set<String> currencylist = new HashSet<>();
        Set<Date> balancedatelist = new HashSet<>();
        balances.forEach(a -> {
            accentitylist.add(a.getAccentity());
            enterpriseBankAccountlist.add(a.getEnterpriseBankAccount());
            currencylist.add(a.getCurrency());
            balancedatelist.add(a.getBalancedate());
        });

        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(accentitylist));
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccountlist));
        conditionGroup.appendCondition(QueryCondition.name("currency").in(currencylist));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").in(balancedatelist));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        // 存在的数据
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        // 确认数据
        List<AccountRealtimeBalance> confirmList = existBalances.stream().filter(AccountRealtimeBalance::getIsconfirm).collect(Collectors.toList());
        //  余额确认数据 Map<会计主体 + 银行账户 + 币种 +日期， 余额确认对象>
        Map<String, AccountRealtimeBalance> confirmMap = new HashMap<>();
        if (!confirmList.isEmpty()) {
            confirmList.forEach(a -> confirmMap.put(getKey(a), a));
        }
        Map<String, AccountRealtimeBalance> dataRepteMap = new HashMap<>();
        // 会计主体 + 银行账户 + 币种 +日期  过滤重复数据
        for (AccountRealtimeBalance balance : balances) {
            dataRepteMap.put(getKey(balance), balance);
        }
        List<AccountRealtimeBalance> addList = new ArrayList<>();
        Set<Map.Entry<String, AccountRealtimeBalance>> entries = dataRepteMap.entrySet();
        for (Map.Entry<String, AccountRealtimeBalance> entry : entries) {
            AccountRealtimeBalance value = entry.getValue();
            if (confirmMap.get(getKey(value)) != null) {
                String id = dataRepteMap.get(getKey(value)).getId() + "";
                confirmIdList.add(id);
                continue;
            }
            addList.add(value);
        }
        // 删除没有确认余额的数据
        List<AccountRealtimeBalance> notConfirmList = existBalances.stream().filter(balance -> !balance.getIsconfirm()).collect(Collectors.toList());
        if (!notConfirmList.isEmpty()) {
            MetaDaoHelper.delete(AccountRealtimeBalance.ENTITY_NAME, notConfirmList);
        }
        CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, addList);
    }

    private String getKey(AccountRealtimeBalance balance) {
        return balance.getAccentity() +
                "," +
                balance.getEnterpriseBankAccount() +
                "," +
                balance.getCurrency() +
                "," +
                DateUtil.format(balance.getBalancedate(), "yyyy-MM-dd");
    }


    private void tranfor2AccountRealtimeBalance(CtmJSONObject ctmJsonObject, List<AccountRealtimeBalance> addlist, CtmJSONObject errmsg, HashMap<String, CtmJSONObject> idparamMap) throws Exception {
        String accentity_code = ctmJsonObject.getString("accentity_code");
        String accentity_name = ctmJsonObject.getString("accentity_name");
        String enterpriseBankAccount_code = ctmJsonObject.getString("enterpriseBankAccount_code");
        String enterpriseBankAccount_name = ctmJsonObject.getString("enterpriseBankAccount_name");
        String currency_code = ctmJsonObject.getString("currency_code");
        String currency_name = ctmJsonObject.getString("currency_name");
        String balancedate = ctmJsonObject.getString("balancedate");
        String avlbal = ctmJsonObject.getString("avlbal");
        String frzbal = ctmJsonObject.getString("frzbal");
        String acctbal = ctmJsonObject.getString("acctbal");
        ctmJsonObject.put("enterpriseBankAccount", enterpriseBankAccount_code);
        ctmJsonObject.put("currency_code", currency_code);
        ctmJsonObject.put("balancedate", balancedate);
        AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
        accountRealtimeBalance.setFlag(BalanceFlag.Manually.getCode());
        // 接口传入
        accountRealtimeBalance.setDatasource(BalanceAccountDataSourceEnum.INTERFACE_IMPORT.getCode());
        String currnecy = StringUtils.isEmpty(currency_code) ? currency_name : currency_code;
        errmsg.put("accentity", StringUtils.isEmpty(accentity_code) ? accentity_name : accentity_code);
        errmsg.put("currency", currnecy);
        errmsg.put("enterpriseBankAccount_code", enterpriseBankAccount_code);
        errmsg.put("enterpriseBankAccount_name", enterpriseBankAccount_name);
        errmsg.put("balancedate", balancedate);

        DateTime current = DateUtil.date();
        DateTime balancedateparse = DateUtil.parse(balancedate);
        if (DateUtil.compare(current, balancedateparse) < 0) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00061", "余额日期不能晚于当前系统日期，请检查！") /* "余额日期不能晚于当前系统日期，请检查！" */);
            return;
        }
        // 会计主体编码翻译
        if (StringUtils.isEmpty(accentity_code) && StringUtils.isEmpty(accentity_name)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400606", "会计主体编码、会计主体名称不允许同时为空，请检查！") /* "会计主体编码、会计主体名称不允许同时为空，请检查！" */);
            return;
        }
        /** 会计主体名称编码转ID */
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code");
        // 根据业务单元名称查询出id
        if (StringUtils.isNotEmpty(accentity_code)) {
            querySchema.appendQueryCondition(QueryCondition.name("code").eq(accentity_code));
        } else {
            querySchema.appendQueryCondition(QueryCondition.name("name").eq(accentity_name));
        }
        List<Map<String, Object>> finList = MetaDaoHelper.query("aa.baseorg.OrgMV", querySchema);
        if (finList == null || finList.isEmpty()) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540060A", "会计主体不存在！") /* "会计主体不存在！" */);
            return;
        }
        String accentityid = finList.get(0).get("id").toString();
        accountRealtimeBalance.setAccentity(accentityid);

        if (StringUtils.isEmpty(currency_code) && StringUtils.isEmpty(currency_name)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400601", "币种编码、币种名称不允许同时为空，请检查！") /* "币种编码、币种名称不允许同时为空，请检查！" */);
            return;
        }
        //CurrencyBdParams currencyBdParams = new CurrencyBdParams();


        //根据币种code或者name查询id
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC); /* 暂不修改 须在BaseRefRpcService中添加通过参数查询币种的方式*/
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (StringUtils.isNotEmpty(currency_code)) {
            conditionGroup.appendCondition(QueryCondition.name("code").eq(currency_code));
        } else {
            conditionGroup.appendCondition(QueryCondition.name("name").eq(currency_name));
        }
//        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (ValueUtils.isEmpty(query)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400609", "找不到相应币种！") /* "找不到相应币种！" */);
            return;
        }
        accountRealtimeBalance.setCurrency(query.get(0).get("id").toString());

//        List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
//        if (currencylist == null || currencylist.size() == 0) {
//            errmsg.put("msg", "找不到相应币种！");
//            return;
//        }
//        for (CurrencyTenantDTO item : currencylist){
//            if (StringUtils.isNotEmpty(currency_code) && currency_code.equals(item.getCode())){
//                accountRealtimeBalance.setCurrency(item.getId());
//                break;
//            }
//            if (StringUtils.isNotEmpty(currency_name) && currency_name.equals(item.getName())){
//                accountRealtimeBalance.setCurrency(item.getId());
//                break;
//            }
//        }
        if (StringUtils.isEmpty(enterpriseBankAccount_name) && StringUtils.isEmpty(enterpriseBankAccount_code)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400607", "银行账户名称及账号不能为空！") /* "银行账户名称及账号不能为空！" */);
            return;
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setName(enterpriseBankAccount_name);
        //注意这里enterpriseBankAccount_code传的是账号，不是账户编码
        enterpriseParams.setAccount(enterpriseBankAccount_code);
        List<EnterpriseBankAcctVO> enterpriselist = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (enterpriselist == null || enterpriselist.size() <= 0) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400608", "银行账户名称及账号在档案中不存在！") /* "银行账户名称及账号在档案中不存在！" */);
            return;
        }
        for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriselist) {
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
            for (BankAcctCurrencyVO currencyVO : currencyList) {
                if (currencyVO.getCurrency().equals(accountRealtimeBalance.getCurrency())) {
                    accountRealtimeBalance.setEnterpriseBankAccount(enterpriseBankAcctVO.getId());
                    accountRealtimeBalance.setBanktype(enterpriseBankAcctVO.getBank());
                    break;
                }
            }
        }
        if (accountRealtimeBalance.getEnterpriseBankAccount() == null) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400604", "系统中不存在币种为") /* "系统中不存在币种为" */ + currnecy + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400602", "的帐号") /* "的帐号" */ + enterpriseBankAccount_code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400603", "请检查！") /* "请检查！" */);
            return;
        }
        //非直联账户余额批量新增openapi调整为直连非直连都可以新增
        //// 直联账户不能新增
        //if (Boolean.TRUE.equals(bankAccountSettingService.getOpenFlagByBankAccountIdOfQuery(accountRealtimeBalance.getEnterpriseBankAccount()))) {
        //    errmsg.put("msg", enterpriseBankAccount_name + "账户已开通银企直联，不进行数据导入，请检查！！");
        //    return;
        //}

        if (StringUtils.isEmpty(balancedate)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00063", "余额日期不能为空！") /* "余额日期不能为空！" */);
            return;
        }
        DateTime balancedate_ = DateUtil.parse(balancedate);
        accountRealtimeBalance.setBalancedate(balancedate_);
        if (StringUtils.isEmpty(acctbal)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00066", "账户余额不能为空！") /* "账户余额不能为空！" */);
            return;
        }
        BigDecimal acctbal_ = new BigDecimal(acctbal);
        accountRealtimeBalance.setAcctbal(acctbal_);
        if (StringUtils.isEmpty(avlbal)) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540060B", "可用余额不能为空！") /* "可用余额不能为空！" */);
            return;
        }
        BigDecimal avlbal_ = new BigDecimal(avlbal);
        accountRealtimeBalance.setAvlbal(avlbal_);
        if (StringUtils.isEmpty(frzbal)) {
            frzbal = "0";
        }
        BigDecimal frzbal_ = new BigDecimal(frzbal);
        accountRealtimeBalance.setFrzbal(frzbal_);

        if (BigDecimal.ZERO.compareTo(frzbal_) > 0) {
            errmsg.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400605", "录入冻结金额值不得为负数") /* "录入冻结金额值不得为负数" */);
            return;
        }
        //账户余额=可用金额+冻结金额
        BigDecimal total = BigDecimalUtils.safeAdd(avlbal_, frzbal_);
//        if (BigDecimal.ZERO.compareTo(BigDecimalUtils.safeSubtract(acctbal_, total)) != 0) {
//            errmsg.put("msg", "录入数据的账户余额需要等于可用金额与冻结金额合计值");
//            return;
//        }
        // 设置id
        long id = ymsOidGenerator.nextId();
        errmsg.put("id", id + "");
        idparamMap.put(id + "", ctmJsonObject);
        accountRealtimeBalance.setId(id);
        // 审计信息
        accountRealtimeBalance.setCreator(AppContext.getCurrentUser().getName());
        accountRealtimeBalance.setCreatorId(AppContext.getCurrentUser().getId());
        accountRealtimeBalance.setCreateDate(new Date());
        accountRealtimeBalance.setCreateTime(new Date());
        addlist.add(accountRealtimeBalance);
    }


    @Override
    public CtmJSONObject queryAccountBalanceByInterval(CtmJSONObject param) throws Exception {
        String accentity_code = param.getString("accentity_code");
        String accentity_name = param.getString("accentity_name");
        String enterpriseBankAccount_code = param.getString("enterpriseBankAccount_code");
        String enterpriseBankAccount_name = param.getString("enterpriseBankAccount_name");
        String currency_code = param.getString("currency_code");
        String currency_name = param.getString("currency_name");
        String tran_date = param.getString("tran_date");
        String begindate = param.getString("begindate");
        String enddate = param.getString("enddate");
        Integer pageIndex = param.getInteger("pageIndex");
        Integer pageSize = param.getInteger("pageSize");

        // 判断业务单元
        if (StringUtils.isEmpty(accentity_code) && StringUtils.isEmpty(accentity_name)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100898"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00064", "会计主体编码名称不能为空！") /* "会计主体编码名称不能为空！" */);
        }
        /** 会计主体名称编码转ID */
        QuerySchema querySchemaAccentity = QuerySchema.create().addSelect("id,code");
        // 根据业务单元名称查询出id
        if (StringUtils.isNotEmpty(accentity_code)) {
            querySchemaAccentity.appendQueryCondition(QueryCondition.name("code").eq(accentity_code));
        } else {
            querySchemaAccentity.appendQueryCondition(QueryCondition.name("name").eq(accentity_name));
        }
        List<Map<String, Object>> finList = MetaDaoHelper.query("aa.baseorg.OrgMV", querySchemaAccentity);
        if (finList == null || finList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100899"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800CF", "会计主体编码不存在！") /* "会计主体编码不存在！" */);
        }

        // 会计主体id
        String accentityid = finList.get(0).get("id").toString();

        // 币种id
        String currencyid = null;
        if (StringUtils.isEmpty(currency_code) && StringUtils.isEmpty(currency_name)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100900"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800CD", "币种名称编码不能同时为空！") /* "币种名称编码不能同时为空！" */);
        }

        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        if (StringUtils.isNotEmpty(currency_code)) {
            currencyBdParams.setCode(currency_code);
        }
        if (StringUtils.isNotEmpty(currency_name)) {
            currencyBdParams.setName(currency_name);
        }
        List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
        if (currencylist == null || currencylist.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100901"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00065", "币种名称不存在！") /* "币种名称不存在！" */);
        }
        for (CurrencyTenantDTO item : currencylist) {
            if (StringUtils.isNotEmpty(currency_code) && currency_code.equals(item.getCode())) {
                currencyid = item.getId();
                break;
            }
            if (StringUtils.isNotEmpty(currency_name) && currency_name.equals(item.getName())) {
                currencyid = item.getId();
                break;
            }
        }

        if (StringUtils.isEmpty(enterpriseBankAccount_name) && StringUtils.isEmpty(enterpriseBankAccount_code)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100902"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800CC", "银行账号编码不能为空！") /* "银行账号编码不能为空！" */);
        }
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setName(enterpriseBankAccount_name);
        enterpriseParams.setCode(enterpriseBankAccount_code);
        List<EnterpriseBankAcctVO> enterpriselist = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (enterpriselist.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100903"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800CE", "银行账号名称编码不存在！") /* "银行账号名称编码不存在！" */);
        }
        String accountid = enterpriselist.get(0).getId();

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.appendQueryCondition(QueryCondition.name("accentity").eq(accentityid));
        querySchema.appendQueryCondition(QueryCondition.name("currency").eq(currencyid));
        querySchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").eq(accountid));
        if (StringUtils.isEmpty(tran_date)) {
            querySchema.appendQueryCondition(QueryCondition.name("balancedate").between(begindate, enddate));
        } else {
            querySchema.appendQueryCondition(QueryCondition.name("balancedate").eq(tran_date));
        }

        querySchema.addPager(pageIndex, pageSize);
        List<Map<String, Object>> query = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
        List<CtmJSONObject> reslist = new ArrayList<>();
        for (Map map : query) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("accentity", StringUtils.isEmpty(accentity_code) ? accentity_name : accentity_code);
            jsonObject.put("enterpriseBankAccount", map.get("enterpriseBankAccount"));
            jsonObject.put("enterpriseBankAccount_name", enterpriseBankAccount_name);
            jsonObject.put("enterpriseBankAccount_code", enterpriseBankAccount_code);
            jsonObject.put("currency_code", currency_code);
            jsonObject.put("currency_name", currency_name);
            jsonObject.put("currency", map.get("currency"));
            jsonObject.put("balancedate", map.get("balancedate").toString());
            jsonObject.put("acctbal", map.get("acctbal"));
            jsonObject.put("frzbal", map.get("frzbal"));
            jsonObject.put("avlbal", map.get("avlbal"));
            reslist.add(jsonObject);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("code", 200);
        result.put("message", "success");
        CtmJSONObject recordList = new CtmJSONObject();
        recordList.put("recordList", reslist);
        result.put("data", recordList);
        return result;
    }
}
