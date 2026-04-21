package com.yonyoucloud.fi.cmp.earlywarning.service.impl;

import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.earlywarning.service.UnclaimedWarningService;
import com.yonyoucloud.fi.cmp.enums.DirectmethodEnum;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.vo.BankNoVO;
import com.yonyoucloud.fi.cmp.vo.NotImportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author qihaoc
 * @Description:
 * @date 2023/6/2 16:33
 */
@Service
@Slf4j
public class UnclaimedWarningServiceImpl implements UnclaimedWarningService {
    @Autowired
    private BankDealDetailService bankDealDetailService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public Map<String, Object> unclaimedWarning(String accentityStr, Integer checkRange, Integer timeOuts, String logId, String tenantId) {
        CtmJSONArray dataArray = new CtmJSONArray();
        Map<String, Object> result = new HashMap<>();
        int status = TaskUtils.TASK_BACK_SUCCESS;
        String errmsg = new String();
        try {
            String msg = new String();
            String[] accentitys = null;
            if (StringUtil.isNotEmpty(accentityStr)) {
                accentitys = accentityStr.split(";");
            }
            List<BillClaim> billClaimList = queryUnclaimedDate(accentitys, checkRange, timeOuts, tenantId);
            if (CollectionUtils.isNotEmpty(billClaimList)) {
                StringBuffer billClaimCodes = new StringBuffer(); // 动态拼接
                for (BillClaim billClaim : billClaimList) {
                    billClaimCodes.append(billClaim.getCode()).append(",");
                }
                String codeStr = billClaimCodes.substring(0, billClaimCodes.length() - 1);
                String date = DateUtils.dateFormat(DateUtils.getNow(), DateUtils.pattern);
                msg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186A867604C00008", "认领单单号%s的数据截止到%s尚未完成处理，请及时认领！") /* "认领单单号%s的数据截止到%s尚未完成处理，请及时认领！" */, codeStr,date);
            } else {
                errmsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186A3EEE04C0000E", "无数据") /* "无数据" */;
            }
            if(StringUtil.isNotEmpty(msg)){
                CtmJSONObject dataItem = new CtmJSONObject();
                dataItem.put("msg", msg);
                dataArray.add(dataItem);
            }
        } catch (Exception e) {
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("UnclaimedWarningServiceImpl error, e = {}", e.getMessage());
            errmsg = e.getMessage();
        } finally {
            log.error("UnclaimedWarningServiceImpl Warning Task, status = {}, logId = {}, content = {}, tenant = {}",
                    status, logId, dataArray, tenantId);
            result.put("status", status);//执行结果： 0：失败；1：成功
            result.put("data", dataArray);//业务方自定义结果集字段
            result.put("errmsg", errmsg);//	异常信息
            AppContext.clear();
        }
        return result;
    }

    @Override
    public Map<String, Object> notImportWarning(String accentity, String bankType, String currency, String checkRange, Integer checkDate, String cotainFreezeAccount, String logId, String tenantId) {
        boolean cotainFreezeAccountBool = TaskUtils.getBooleanFromString(cotainFreezeAccount, false);
        boolean containRPADirectmethodBool = TaskUtils.getBooleanFromString(checkRange, false);
        List<BankNoVO> dataArray = new ArrayList<>();
        CtmJSONObject result = new CtmJSONObject();
        List<String> accounts = new ArrayList<>();
        List<NotImportVO> accountsId = new ArrayList<>();
        int status = TaskUtils.TASK_BACK_SUCCESS;
        String errmsg = new String();
        try {
            String msg = new String();
            String[] bankTypes = null;
            String[] currencys = null;
            String[] accentitys = null;
            if (StringUtil.isNotEmpty(bankType)) {
                bankTypes = bankType.split(";");
            }
            if (StringUtil.isNotEmpty(currency)) {
                currencys = currency.split(";");
            }
            if (StringUtil.isNotEmpty(accentity)) {
                accentitys = accentity.split(";");
            }
            CtmJSONObject queryBankAccountVosParams = buildQueryBankAccountVosParams(accentitys, bankTypes, currencys);
            queryBankAccountVosParams.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            //根据参数查询全量账户
            List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(queryBankAccountVosParams);
                //获取非直连账户
                Map<String, List<EnterpriseBankAcctVO>> bankAcctVOsGroup = getBankAcctVOsGroup(bankAccounts,"0");
                List<EnterpriseBankAcctVO> checkSuccess = new ArrayList<>();
                checkSuccess = bankAcctVOsGroup.get("checkSuccess");
                if (!checkSuccess.isEmpty()){
                    //根据账户信息筛选账户
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = DirectmethodCheckUtils.getAccountByBamAccountMsgOfEnterpriseBankAcctVOs(containRPADirectmethodBool, cotainFreezeAccountBool, checkSuccess);
                    if (!enterpriseBankAcctVOS.isEmpty()){
                        checkSuccess.clear();
                        for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                            checkSuccess.add(enterpriseBankAcctVO);
                        }
                    }
                }

                if (!checkSuccess.isEmpty()){
                    // 使用Set来避免重复的NotImportVO对象
                    Set<String> uniqueKeys = new HashSet<>();
                    for (EnterpriseBankAcctVO success : checkSuccess) {
                        String account = success.getAccount();
                        if (account != null){
                            accounts.add(account);
                            List<BankAcctCurrencyVO> currencyList = success.getCurrencyList();
                            for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                                // 创建唯一键来标识重复项
                                String uniqueKey = success.getId() + "_" + bankAcctCurrencyVO.getCurrency();
                                if (!uniqueKeys.contains(uniqueKey)) {
                                    uniqueKeys.add(uniqueKey);
                                    NotImportVO notImportVO = new NotImportVO();
                                    notImportVO.setCountId(success.getId());
                                    notImportVO.setCurrencyId(bankAcctCurrencyVO.getCurrency());
                                    accountsId.add(notImportVO);
                                }
                            }
                        }
                    }
                    ArrayList<NotImportVO> list = new ArrayList<>();
//                    for (Map.Entry<String, String> stringStringEntry : accountsId.entrySet()) {
//                        String key = stringStringEntry.getKey();
//                        String value = stringStringEntry.getValue();
//                        Map<String, String> stringStringMap = queryNotImport(stringStringEntry.getKey(), checkRange, checkDate, tenantId, stringStringEntry.getValue());
//                        if (stringStringMap != null){
//                            list.put(key,value);
//                        }
//                    }
                    for (NotImportVO notImportVO : accountsId) {
                        NotImportVO bankReconciliations = queryNotImport(notImportVO.getCountId(), checkRange, checkDate, tenantId, notImportVO.getCurrencyId());
                        if (bankReconciliations != null) {
                            list.add(bankReconciliations);
                        }
                    }
                    if (list == null && list .isEmpty()){
                        msg = null;
                    }else {
//                        for (EnterpriseBankAcctVO success : checkSuccess) {
//                            String id = success.getId();
//                            for (NotImportVO notImportVO : list) {
//                                if (id.equals(notImportVO.getCountId())){
//                                    String value = notImportVO.getCurrencyId();
//                                    if (value != null){
//                                        List<Map<String, Object>> maps = QueryBaseDocUtils.queryCurrencyById(value);
//                                        String name = (String) maps.get(0).get("name");
//                                        BankNoVO bankNoVO = new BankNoVO();
//                                        bankNoVO.setAccount(success.getAccount());
//                                        bankNoVO.setAccentity(success.getOrgidName());
//                                        bankNoVO.setBranch(success.getBankNumberName());
//                                        bankNoVO.setCurrency(name);
//                                        bankNoVO.setWarnPrimaryOrgId(success.getOrgid());
//                                        dataArray.add(bankNoVO);
//                                    }
//                                }
//                            }
//                        }
                        // 替换原有重复代码段为以下实现
                        Map<String, EnterpriseBankAcctVO> accountMap = checkSuccess.stream()
                            .collect(Collectors.toMap(EnterpriseBankAcctVO::getId, Function.identity(), (existing, replacement) -> existing));

                        Map<String, List<NotImportVO>> groupedByAccountId = list.stream()
                            .collect(Collectors.groupingBy(NotImportVO::getCountId));

                        for (Map.Entry<String, List<NotImportVO>> entry : groupedByAccountId.entrySet()) {
                            String accountId = entry.getKey();
                            List<NotImportVO> notImportVOs = entry.getValue();

                            EnterpriseBankAcctVO account = accountMap.get(accountId);
                            if (account == null) continue;

                            for (NotImportVO notImportVO : notImportVOs) {
                                String currencyId = notImportVO.getCurrencyId();
                                if (currencyId == null) continue;

                                try {
                                    List<Map<String, Object>> currencyMaps = QueryBaseDocUtils.queryCurrencyById(currencyId);
                                    if (!currencyMaps.isEmpty()) {
                                        String currencyName = (String) currencyMaps.get(0).get("name");

                                        BankNoVO bankNoVO = new BankNoVO();
                                        bankNoVO.setAccount(account.getAccount());
                                        bankNoVO.setAccentity(account.getOrgidName());
                                        bankNoVO.setBranch(account.getBankNumberName());
                                        bankNoVO.setCurrency(currencyName);
                                        bankNoVO.setWarnPrimaryOrgId(account.getOrgid());

                                        dataArray.add(bankNoVO);
                                    }
                                } catch (Exception e) {
                                    log.error("查询币种信息出错: {}", e.getMessage(), e);
                                }
                            }
                        }
                    }
                }else {
                    msg = null;
                }
        } catch (Exception e) {
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("UnclaimedWarningServiceImpl error, e = {}", e.getMessage());
            errmsg = e.getMessage();
        } finally {
            if (checkDate == null){
                checkDate = 7;
            }
            log.error("UnclaimedWarningServiceImpl Warning Task, status = {}, logId = {}, content = {}, tenant = {}",
                    status, logId, dataArray, tenantId);
            result.put("status", status);//执行结果： 0：失败；1：成功
            result.put("data", dataArray);//业务方自定义结果集字段
            result.put("errmsg", errmsg);//	异常信息
            result.put("checkDate",checkDate);
            AppContext.clear();
        }
        return result;
    }

    /**
     * 查询符合条件的“我的认领”单据
     *
     * @param accentitys
     * @param checkRange 单据日期范围（前X日）
     * @param timeOuts 超时天数 超过几天未完成关联的
     * @param tenantId
     * @return
     * @throws Exception
     */
    private List<BillClaim> queryUnclaimedDate(String[] accentitys, Integer checkRange, Integer timeOuts, String tenantId) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        //会计主体
        if (accentitys != null && accentitys.length > 0) {
            queryConditionGroup.addCondition(QueryCondition.name("accentity").in(accentitys));
        }

        if (checkRange != null && timeOuts != null) {
            if (timeOuts <= checkRange) {
                //单据日期范围（前X日内）
                String beforeDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * checkRange), DateUtils.pattern);
                queryConditionGroup.addCondition(QueryCondition.name("createDate").egt(beforeDate));
                //超过几天未完成关联的(timeOuts前已认领)
                String timeOutDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * timeOuts), DateUtils.pattern);
                queryConditionGroup.addCondition(QueryCondition.name("vouchdate").elt(timeOutDate));
            } else {
                return null;
            }
        } else if (checkRange != null && timeOuts == null) {
            String beforeDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * checkRange), DateUtils.pattern);
            queryConditionGroup.addCondition(QueryCondition.name("createDate").egt(beforeDate));
        } else if (checkRange == null && timeOuts != null) {
            String timeOutDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * timeOuts), DateUtils.pattern);
            queryConditionGroup.addCondition(QueryCondition.name("vouchdate").elt(timeOutDate));
        }
        queryConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue()));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
    }

    private NotImportVO queryNotImport(String accounts, String checkRange, Integer checkDate, String tenantId,String currency) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        //检查日期
        if (checkDate == null){
            checkDate = 7;
        }
            String beforeDate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * checkDate), DateUtils.pattern);
            queryConditionGroup.addCondition(QueryCondition.name(BankReconciliation.TRAN_DATE).between(beforeDate, DateUtils.getNow()));
            queryConditionGroup.addCondition(QueryCondition.name("bankaccount").eq(accounts));
            queryConditionGroup.addCondition(QueryCondition.name("currency").eq(currency));
            querySchema.addCondition(queryConditionGroup);
            List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
           if (bankReconciliations.size() == 0){
               NotImportVO notImportVO = new NotImportVO();
               notImportVO.setCountId(accounts);
               notImportVO.setCurrencyId(currency);
               return notImportVO;
           }
           return null;
    }
    public static CtmJSONObject buildQueryBankAccountVosParams(String[] accentityArr, String[] banktypeArr, String[] currencyArr) {
        CtmJSONObject queryBankAccountVosParams = new CtmJSONObject();
        queryBankAccountVosParams.put("accEntity", accentityArr);
        queryBankAccountVosParams.put("bankType", banktypeArr);
        queryBankAccountVosParams.put("currency", currencyArr);
        return queryBankAccountVosParams;
    }

    public static Map<String, List<EnterpriseBankAcctVO>> getBankAcctVOsGroup(List<EnterpriseBankAcctVO> bankAccounts,String openflag) throws Exception {
        //非直联账户
        List<EnterpriseBankAcctVO> checkSuccess = new ArrayList<>();
        //结算中心 内部账户
        List<EnterpriseBankAcctVO> innerAccounts = new ArrayList<>();
        List<EnterpriseBankAcctVO> failAccounts = new ArrayList<>();

        List<String> bankaccountSettingAccountId = new ArrayList<>();
        if (bankAccounts != null && bankAccounts.size() > 0) {
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                bankaccountSettingAccountId.add(enterpriseBankAcctVO.getId());
                if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
                    innerAccounts.add(enterpriseBankAcctVO);
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100092"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000D", "选择的会计主体，尚未维护银行账户，请检查!") /* "选择的会计主体，尚未维护银行账户，请检查!" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(bankaccountSettingAccountId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(openflag));
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if (settings != null && settings.size() > 0) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100092"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000D","选择的会计主体，尚未维护银行账户，请检查!"));
            for (Map<String, Object> map : settings) {
                for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                    if (map.get("enterpriseBankAccount").toString().equals(enterpriseBankAcctVO.getId())) {
                            checkSuccess.add(enterpriseBankAcctVO);
                    }
                }
            }
        }
//        if (checkSuccess.size() == 0 && innerAccounts.size() == 0) {
//            if (failAccounts.size() != 0) {
//                StringBuilder failNames = new StringBuilder("");
//                for (int i = 0; i < failAccounts.size(); i++) {
//                    if (i == failAccounts.size() - 1) {
//                        failNames.append(failAccounts.get(i).getName());
//                    } else {
//                        failNames.append(failAccounts.get(i).getName());
//                        failNames.append(",");
//                    }
//                }
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA", "【");
//                        failNames + "】" +
//                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BD", "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
//            }
//            StringBuilder accountNames = new StringBuilder("");
//            for (int i = 0; i < bankAccounts.size(); i++) {
//                if (i == bankAccounts.size() - 1) {
//                    accountNames.append(bankAccounts.get(i).getName());
//                } else {
//                    accountNames.append(bankAccounts.get(i).getName());
//                    accountNames.append(",");
//                }
//            }
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA", "【") /* "【" */ + accountNames + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D3", "】的企业银行账户没有开通银企联，无法查询银行账户交易明细") /* "】的企业银行账户没有开通银企联，无法查询银行账户交易明细" */);
//        }
        Map<String, List<EnterpriseBankAcctVO>> resultMap = new HashMap<>();
        //resultMap.put("innerAccounts", innerAccounts);
        resultMap.put("checkSuccess", checkSuccess);
        return resultMap;
    }

    //public  List<EnterpriseBankAcctVO> getEnterpriseBankAccountVos(CtmJSONObject params) throws Exception {
    //    EnterpriseParams enterpriseParams = new EnterpriseParams();
    //    // start wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
    //    String accEntity = params.getString("accEntity");
    //    List<String> accounts = new ArrayList<>();
    //    if (accEntity != null) {
    //        List<String> accentitys = Arrays.asList(accEntity.split(","));
    //        //判断前段数据是否为多选
    //        if (accEntity.contains("[")) {
    //            accentitys = params.getObject("accEntity", List.class);
    //        }
    //        if (accentitys != null && !accentitys.isEmpty()) {
    //            enterpriseParams.setOrgidList(accentitys);
    //            // 根据所选组织查询 有权限的账户
    //            EnterpriseParams newEnterpriseParams = new EnterpriseParams();
    //            newEnterpriseParams.setOrgidList(accentitys);
    //            List<EnterpriseBankAcctVO> query = enterpriseBankQueryService.query(newEnterpriseParams);
    //            for (EnterpriseBankAcctVO enterpriseBankAcctVO : query) {
    //                accounts.add(enterpriseBankAcctVO.getId());
    //            }
    //        }
    //    }
    //    String currency = params.getString("currency");
    //    List<String> currencyids = new ArrayList<>();
    //    if (currency != null) {
    //        currencyids = Arrays.asList(accEntity.split(","));
    //        if (currency.contains("[")) {
    //            currencyids = params.getObject("currency", List.class);
    //        } else if (currency != null) {
    //            currencyids.add(currency);
    //        }
    //        enterpriseParams.setCurrencyIDList(currencyids);
    //    } else {
    //        enterpriseParams.setCurrencyIDList(null);
    //    }
    //
    //    //if(StringUtils.isNotEmpty(currency)){
    //
    //    //}
    //    String bankType = params.getString("bankType");
    //    List<String> bankTypes = new ArrayList<>();
    //    if (bankType != null) {
    //        bankTypes = Arrays.asList(bankType.split(","));
    //        if (bankType != null && bankType.contains("[")) {
    //            bankTypes = params.getObject("bankType", List.class);
    //        } else if (bankType != null) {
    //            bankTypes.add(bankType);
    //        }
    //    }
    //    List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
    //    //由于账户提供的接口参数 银行类别为string，所以当传入多个银行类别时候需要循环查询
    //    if (bankTypes != null && !bankTypes.isEmpty()) {
    //        for (String bank : bankTypes) {
    //            enterpriseParams.setBank(bank);
    //            bankAccounts.addAll(enterpriseBankQueryService.queryAll(enterpriseParams));
    //        }
    //    } else
    //        bankAccounts.addAll(enterpriseBankQueryService.queryAll(enterpriseParams));
    //    return bankAccounts;
    //}
//    private List<EnterpriseBankAcctVO> queryAccountInfo(List<EnterpriseBankAcctVO> allAccountList) throws Exception {
//        // 通过现有逻辑查询到的企业银行账号，再去查询账户管理模块对应账户信息查询模块中对应的直联方式进行过滤
//        List<EnterpriseBankAcctVO> allAccountListNew = new ArrayList<>();
//        List<String> accountList = allAccountList.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
//        QuerySchema schema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
//        //schema.appendQueryCondition(QueryCondition.name("accountId").in(accountList));
//        QueryConditionGroup condition = new QueryConditionGroup();
//        condition.addCondition(QueryConditionGroup.or(QueryCondition.name("directChannel").is_null(),QueryCondition.name("directChannel").not_eq("2")));
//        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accountId").in(accountList)));
//        //condition.addCondition(QueryCondition.name("directChannel").is_null(),QueryCondition.name("directChannel").not_eq("2"),QueryCondition.name("accountId").in(accountList));
////        schema.appendQueryCondition(QueryCondition.name("directChannel").not_eq("2"));
////        schema.appendQueryCondition(QueryCondition.name("directChannel").is_null());
//        schema.addCondition(condition);
//        List<Map<String, Object>> accountInfoVOs = MetaDaoHelper.query("yonbip-fi-ctmbam.accountInfo.accinfo", schema, "yonbip-fi-ctmbam");
//        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(accountInfoVOs)) {
//            for (Map<String, Object> accountInfoVO : accountInfoVOs) {
//                String accountId = (String) accountInfoVO.get("accountId");
//                for (EnterpriseBankAcctVO stringObjectMap : allAccountList) {
//                    if (stringObjectMap.getId().equals(accountId)) {
//                        allAccountListNew.add(stringObjectMap);
//                    }
//                }
//            }
//        }
//
//        return allAccountListNew;
//    }
}

