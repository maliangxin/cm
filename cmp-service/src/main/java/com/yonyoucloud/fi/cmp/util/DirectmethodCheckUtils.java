package com.yonyoucloud.fi.cmp.util;

import cn.hutool.core.collection.CollectionUtil;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.enums.DirectmethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2024年12月02日 20:08
 * @Description:判断调度任务里的直联渠道参数
 */
@Slf4j
public class DirectmethodCheckUtils {

    public static final String YINQI_LINK = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400566", "银企直联") /* "银企直联" */;
    public static final String SWIFT_LINK = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400564", "SWIFT直联") /* "SWIFT直联" */;
    public static final String RPA_LINK = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400565", "PRA直联") /* "PRA直联" */;
    // 注入IApplicationService接口
    @Resource
    private static IApplicationService appService = AppContext.getBean(IApplicationService.class);


    // 根据直联渠道参数获取账户列表
    public static List<String> getAccountsByParamMapOfAccountIDList(Map<String, Object> paramMap, List<String> accountIDList) throws Exception {
        // 获取直联渠道参数
        String directmethod = getDirectmethod(paramMap);
        // 根据直联渠道参数获取账户列表
        accountIDList = getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList);
        return accountIDList;
    }

    public static List<EnterpriseBankAcctVO> getAccountByParamMapOfEnterpriseBankAcctVOs(Map<String, Object> paramMap, List<EnterpriseBankAcctVO> accountVOs) throws Exception {
        if (CollectionUtil.isEmpty(accountVOs)) {
            return new ArrayList<EnterpriseBankAcctVO>();
        }
        List<String> accountIDList = accountVOs.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
        // 获取直联渠道参数
        String directmethod = getDirectmethod(paramMap);
        List<String> accountIDListByDirectmethod = getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList);
        List<EnterpriseBankAcctVO> accountVOListByDirectmethod = accountVOs.stream().filter(item -> accountIDListByDirectmethod.contains(item.getId())).collect(Collectors.toList());
        return accountVOListByDirectmethod;
    }

    public static List<BankElectronicReceipt> getAccountByParamMapOfVOs(Map<String, Object> paramMap, List<BankElectronicReceipt> accountVOs) throws Exception {
        List<String> accountIDList = accountVOs.stream().map(item -> item.getEnterpriseBankAccount()).distinct().collect(Collectors.toList());
        // 获取直联渠道参数
        String directmethod = getDirectmethod(paramMap);
        List<String> accountIDListByDirectmethod = getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList);
        List<BankElectronicReceipt> accountVOListByDirectmethod = accountVOs.stream().filter(item -> accountIDListByDirectmethod.contains(item.getEnterpriseBankAccount())).collect(Collectors.toList());
        return accountVOListByDirectmethod;
    }

    private static  String getDirectmethod(Map<String, Object> paramMap) {
        return (String) (Optional.ofNullable(paramMap.get("directmethod")).orElse(""));
    }

    public static List<EnterpriseBankAcctVO> getAccountByDirectmethodOfEnterpriseBankAcctVOs(String directmethod, List<EnterpriseBankAcctVO> accountVOs) throws Exception {
        List<String> accountIDList = accountVOs.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
        List<String> accountIDListByDirectmethod = getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList);
        List<EnterpriseBankAcctVO> accountVOListByDirectmethod = accountVOs.stream().filter(item -> accountIDListByDirectmethod.contains(item.getId())).collect(Collectors.toList());
        return accountVOListByDirectmethod;
    }

    public static List<EnterpriseBankAcctVO> getAccountByNotDirectmethodOfEnterpriseBankAcctVOs(String directmethod, List<EnterpriseBankAcctVO> accountVOs, boolean reverseFlag) throws Exception {
        List<String> accountIDList = accountVOs.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
        List<String> accountIDListByDirectmethod = getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList, reverseFlag);
        List<EnterpriseBankAcctVO> accountVOListByDirectmethod = accountVOs.stream().filter(item -> accountIDListByDirectmethod.contains(item.getId())).collect(Collectors.toList());
        return accountVOListByDirectmethod;
    }


    public static List<EnterpriseBankAcctVO> getAccountByBamAccountMsgOfEnterpriseBankAcctVOs(Boolean containRPADirectmethod, Boolean containFreezeAccount, List<EnterpriseBankAcctVO> accountVOs) throws Exception {
        List<EnterpriseBankAcctVO> filterAccountVOs = new ArrayList<>(accountVOs);
        List<String> accountIDList = accountVOs.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
        // 如果账户列表为空，直接返回
        if (accountIDList.size() < 1) {
            return filterAccountVOs;
        }
        // 判断账户管理是否启用
        boolean enableBAM = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.CTM_YONBIP_FI_CTMPUB_BAM_APP_CODE);
        // 如果账户管理未启用，直接返回
        if (!enableBAM) {
            log.error("客户环境未安装财资账户管理");
            return filterAccountVOs;
        }

        // 通过现有逻辑查询到的企业银行账号，再去查询账户管理模块对应账户信息查询模块中对应的信息进行过滤
        // 构建查询条件
        //todo 下沉R6只能查元数据；到账户简强版本，待换rpc接口
        QuerySchema schema = QuerySchema.create().addSelect("id,accountId,directChannel, accountcurrtypeList.id, accountcurrtypeList.currency, accountcurrtypeList.frozenState");
        schema.appendQueryCondition(QueryCondition.name("accountId").in(accountIDList));
        // 查询账户信息
        List<Map<String, Object>> accountInfoMaps = MetaDaoHelper.query("yonbip-fi-ctmbam.accountInfo.accinfo", schema, "yonbip-fi-ctmbam");
        if (CollectionUtil.isEmpty(accountInfoMaps)) {
            return filterAccountVOs;
        }

        Set<String> accountInfoIds = accountInfoMaps.stream()
                .map(accountInfo -> (String)accountInfo.get("accountId"))
                .collect(Collectors.toSet());

        List<EnterpriseBankAcctVO> notInAccountInfoAccountVOs = accountVOs.stream()
                .filter(item -> !accountInfoIds.contains(item.getId()))
                .collect(Collectors.toList());
        List<EnterpriseBankAcctVO> inAccountInfoAccountVOs = accountVOs.stream()
                .filter(item -> accountInfoIds.contains(item.getId()))
                .collect(Collectors.toList());


        DirectmethodEnum directmethodEnum = DirectmethodEnum.RPA_LINK;
        int directmethodInt = directmethodEnum.getValue();
        Predicate<Map<String, Object>> notContainRPADirectmethodFilter = item ->
                !String.valueOf(directmethodInt).equals(Objects.toString(item.get("directChannel"), null));

        //编码bam_currency_state
        //        名称冻结状态
        //序号
        //枚举值(key)	名称
        //1
        //0	冻结
        //2
        //1	正常
        //3
        //3	部分冻结
        //4
        //4	全部冻结
        //冻结状态字段从币种子表取
        Predicate<Map<String, Object>> notContainFreezeAccountFilter = item ->
                String.valueOf("1").equals(Objects.toString(item.get("accountcurrtypeList_frozenState"), null));

        //不包含才需要过滤，否则取全部
        List<String> finalFilterAccountCurrencys  = accountInfoMaps.stream()
                .filter(item -> {
                    if (!containRPADirectmethod) {
                        return notContainRPADirectmethodFilter.test(item);
                    }
                    return true;
                })
                .filter(item -> {
                    if (!containFreezeAccount) {
                        return notContainFreezeAccountFilter.test(item);
                    }
                    return true;
                })
                .filter(item -> item.get("accountcurrtypeList_currency") != null)
                .map(item -> Objects.toString(item.get("accountId")) + '@' + Objects.toString(item.get("accountcurrtypeList_currency")))
                .collect(Collectors.toList());

        //用币种子表过滤，去掉没有的币种子表
        Iterator<EnterpriseBankAcctVO> accountIterator = inAccountInfoAccountVOs.iterator();
        while (accountIterator.hasNext()) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = accountIterator.next();
            Iterator<BankAcctCurrencyVO> currencyIterator = enterpriseBankAcctVO.getCurrencyList().iterator();
            while (currencyIterator.hasNext()) {
                BankAcctCurrencyVO bankAcctCurrencyVO = currencyIterator.next();
                String enterpriseBankAcctVOId = enterpriseBankAcctVO.getId();
                String currency = bankAcctCurrencyVO.getCurrency();
                String key = enterpriseBankAcctVOId + '@' + currency;
                if (!finalFilterAccountCurrencys.contains(key)) {
                    currencyIterator.remove();
                }
            }
            //所有币种都被删除了，去掉此账户
            if (enterpriseBankAcctVO.getCurrencyList().size() < 1) {
                accountIterator.remove();
            }
        }
        filterAccountVOs.addAll(inAccountInfoAccountVOs);
        //账户信息里没有的账户，默认不过滤，最后加回去
        filterAccountVOs.addAll(notInAccountInfoAccountVOs);
        return filterAccountVOs;
    }


    public static List<EnterpriseBankAcctVO> getAccountByNotDirectmethodOfEnterpriseBankAcctVOs(String directmethod, List<EnterpriseBankAcctVO> accountVOs) throws Exception {
        List<String> accountIDList = accountVOs.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
        List<String> accountIDListByDirectmethod = getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList, false);
        List<EnterpriseBankAcctVO> accountVOListByDirectmethod = accountVOs.stream().filter(item -> accountIDListByDirectmethod.contains(item.getId())).collect(Collectors.toList());
        return accountVOListByDirectmethod;
    }

    // 根据直联渠道参数获取账户列表
    private static List<String> getAccountByDirectmethodOfAccountIDList(String directmethod, List<String> accountIDList) throws Exception {
        //默认为false
        return getAccountByDirectmethodOfAccountIDList(directmethod, accountIDList, false);
    }

    // 根据直联渠道参数获取账户列表
    private static List<String> getAccountByDirectmethodOfAccountIDList(String directmethod, List<String> accountIDList, boolean reverseFlag) throws Exception {
        // 如果账户列表为空，直接返回
        if (accountIDList.size() < 1) {
            return accountIDList;
        }
        // 如果直联渠道参数为空，直接返回
        if (StringUtils.isEmpty(directmethod)) {
            return accountIDList;
        }
        // 判断账户管理是否启用
        boolean enableBAM = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.CTM_YONBIP_FI_CTMPUB_BAM_APP_CODE);
        // 如果账户管理未启用，直接返回
        if (!enableBAM) {
            log.error("客户环境未安装财资账户管理");
            return accountIDList;
        }

        //返回枚举类DirectmethodEnum，其中的name和directmethod相等
        Optional<DirectmethodEnum> directmethodEnum = Arrays.stream(DirectmethodEnum.values())
                .filter(dm -> dm.getName().equals(directmethod))
                .findFirst();

        int directmethodInt;
        if (directmethodEnum.isPresent()) {
            directmethodInt = directmethodEnum.get().getValue();
        } else {
            log.error("不支持的直联渠道方式！");
            return accountIDList;
        }

        // 通过现有逻辑查询到的企业银行账号，再去查询账户管理模块对应账户信息查询模块中对应的直联方式进行过滤
        // 构建查询条件
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.appendQueryCondition(QueryCondition.name("accountId").in(accountIDList));
        // 查询账户信息
        List<Map<String, Object>> accountInfoVOs = MetaDaoHelper.query("yonbip-fi-ctmbam.accountInfo.accinfo", schema, "yonbip-fi-ctmbam");
        if (CollectionUtil.isEmpty(accountInfoVOs)) {
            return accountIDList;
        }

        List<String> directmethodAccounts = new ArrayList<>();

        Predicate<Map<String, Object>> baseFilter = item ->
                String.valueOf(directmethodInt).equals(Objects.toString(item.get("directChannel"), null));

        Predicate<Map<String, Object>> finalFilter = reverseFlag ? baseFilter.negate() : baseFilter;

        directmethodAccounts = accountInfoVOs.stream()
                .filter(finalFilter)
                .filter(item -> item.get("accountId") != null)
                .map(item -> Objects.toString(item.get("accountId")))
                .collect(Collectors.toList());

        return directmethodAccounts;
    }
}
