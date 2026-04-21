package com.yonyoucloud.fi.cmp.util.basedoc;

import com.google.common.collect.Lists;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.rpcparams.BdQueryOrderby;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IBankService;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.basedoc.ICtmBankdotService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询企业银行账户万能接口
 */
@Service
@Slf4j
public class EnterpriseBankQueryService {

    public static final String INNER_ACCOUNTS = "innerAccounts";
    public static final String CHECK_SUCCESS = "checkSuccess";
    public static final Integer BANKACCTCURRENCYVO_ENABLE = 1;
    public static String noCustomNoMessageWithAcct_no = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2214777E05600008", "银行账号【%s】未获取到银企联所需的客户号，请先在以下节点维护:") /* "银行账号【%s】未获取到银企联所需的客户号，请先在以下节点维护:" */ + IServicecodeConstant.SERVICECODE_MAP.get(IServicecodeConstant.BANKACCOUNTSETTING);

    private static IEnterpriseBankAcctService getEnterpriseBankAcctService() {
        return AppContext.getBean(IEnterpriseBankAcctService.class);
    }

    @Autowired
    IBankService iBankService;

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     *
     * @param bankAccounts
     * @return
     * @throws Exception
     */
    //@Override
    public static Map<String, List<EnterpriseBankAcctVO>> getBankAcctVOsGroup(List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        //直联账户
        List<EnterpriseBankAcctVO> checkSuccess = new ArrayList<>();
        //结算中心 内部账户
        List<EnterpriseBankAcctVO> innerAccounts = new ArrayList<>();
        List<EnterpriseBankAcctVO> failAccounts = new ArrayList<>();

        List<String> bankaccountSettingAccountId = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankAccounts)) {
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                bankaccountSettingAccountId.add(enterpriseBankAcctVO.getId());
                if (enterpriseBankAcctVO.getAcctopentype() != null && enterpriseBankAcctVO.getAcctopentype().equals(1)) {
                    innerAccounts.add(enterpriseBankAcctVO);
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100091"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050015", "选择的资金组织，尚未维护银行账户，请检查!") /* "选择的资金组织，尚未维护银行账户，请检查!" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,customNo");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(bankaccountSettingAccountId));
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        if (settings != null && settings.size() > 0) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100092"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000D","选择的会计主体，尚未维护银行账户，请检查!"));
            for (Map<String, Object> map : settings) {
                for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccounts) {
                    if (map.get("enterpriseBankAccount").toString().equals(enterpriseBankAcctVO.getId())) {
                        if (StringUtils.isNotEmpty(map.get("customNo") != null ? map.get("customNo").toString() : null)) {
                            checkSuccess.add(enterpriseBankAcctVO);
                            break;
                        } else {
                            failAccounts.add(enterpriseBankAcctVO);
                            break;
                        }
                    }
                }
            }
        }
        if (checkSuccess.size() == 0 && innerAccounts.size() == 0) {
            if (failAccounts.size() != 0) {
                StringBuilder failNames = new StringBuilder("");
                for (int i = 0; i < failAccounts.size(); i++) {
                    if (i == failAccounts.size() - 1) {
                        failNames.append(failAccounts.get(i).getAccount());
                    } else {
                        failNames.append(failAccounts.get(i).getAccount());
                        failNames.append(",");
                    }
                }
                throw new CtmException(String.format(noCustomNoMessageWithAcct_no,failNames));
            }
            StringBuilder accountNames = new StringBuilder("");
            for (int i = 0; i < bankAccounts.size(); i++) {
                if (i == bankAccounts.size() - 1) {
                    accountNames.append(bankAccounts.get(i).getName());
                } else {
                    accountNames.append(bankAccounts.get(i).getName());
                    accountNames.append(",");
                }
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100093"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA", "【") /* "【" */ + accountNames + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D3", "】的企业银行账户没有开通银企联，无法查询银行账户交易明细") /* "】的企业银行账户没有开通银企联，无法查询银行账户交易明细" */);
        }
        Map<String, List<EnterpriseBankAcctVO>> resultMap = new HashMap<>();
        resultMap.put(INNER_ACCOUNTS, innerAccounts);
        resultMap.put(CHECK_SUCCESS, checkSuccess);
        return resultMap;
    }

    /**
     * 用于直联联账户交易明细查询条件拼接梳理 通过前端条件查询相关银行账户
     *
     * @param params
     * @return
     * @throws Exception
     */
    //@Override
    public static List<EnterpriseBankAcctVO> getEnterpriseBankAccountVos(CtmJSONObject params) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        // start wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        String accEntity = params.getString("accEntity");
        List<String> accounts = new ArrayList<>();
        if (accEntity != null) {
            List<String> accentitys = Arrays.asList(accEntity.split(","));
            //判断前段数据是否为多选
            if (accEntity.contains("[")) {
                accentitys = params.getObject("accEntity", List.class);
            }
            if (accentitys != null && !accentitys.isEmpty()) {
//                enterpriseParams.setOrgidList(accentitys);
                // 根据所选组织查询 有权限的账户
                EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                newEnterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = queryAllEnableWithRange(newEnterpriseParams);
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                    accounts.add(enterpriseBankAcctVO.getId());
                }
            }
        }
        // end wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        // 银行账户
        List<String> enterBankAccs = params.getObject("accountId", List.class);
        //如果是智能员工，则只能查询当日的交易明细
        if (StringUtils.isNotEmpty(params.getString("skillId"))) {
            params.put("endDate", DateUtils.getTodayShort());
            params.put("startDate", DateUtils.getTodayShort());
        }
        String date_pattern = params.getString("date_pattern");
        if (StringUtils.isEmpty(date_pattern)) {
            date_pattern = "yyyy-MM-dd";
        }
        if (params.get("startDate") != null && params.get("endDate") != null) {
            Date startDate = null;
            Date endDate = null;
            try {
                //开始时间
                startDate = new SimpleDateFormat(date_pattern).parse(params.get("startDate").toString());
                //结束时间
                endDate = new SimpleDateFormat(date_pattern).parse(params.get("endDate").toString());
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105059"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_229F8EC605600005", "格式化开始日期和结束日期异常，对于开始日期为[%s]，结束日期为[%s]"));
            }
            int days = DateUtils.dateBetween(startDate, endDate);
            //调度任务使用他们的前端限制，不在代码中限制
            Boolean isDispatchTaskCmp = params.getBoolean(ICmpConstant.IS_DISPATCH_TASK_CMP) == null ? false : params.getBoolean(ICmpConstant.IS_DISPATCH_TASK_CMP);
            if (!isDispatchTaskCmp) {
                if (days > 31) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100090"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000C", "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!") /* "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!" */);
                }
            }
        }
        String currency = params.getString("currency");
        List<String> currencyids = new ArrayList<>();
        if (currency != null && currency.contains("[")) {
            currencyids = params.getObject("currency", List.class);
        } else if (currency != null) {
            currencyids.add(currency);
        }
        if (StringUtils.isNotEmpty(currency)) {
            enterpriseParams.setCurrencyIDList(currencyids);
        }

        if (enterBankAccs != null && !enterBankAccs.isEmpty()) {
            enterpriseParams.setIdList(enterBankAccs);
        } else {
            enterpriseParams.setIdList(accounts);
        }
        //调度任务传入银行类别(调度任务使用 手动拉取不用)
        String bankType = params.getString("bankType");
        List<String> bankTypes = new ArrayList<>();
        // todo 参数对象话，去掉contain
        if (bankType != null && bankType.contains("[")) {
            bankTypes = params.getObject("bankType", List.class);
        } else if (bankType != null) {
            bankTypes.add(bankType);
        }
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        //由于账户提供的接口参数 银行类别为string，所以当传入多个银行类别时候需要循环查询
        //todo 待优化
        if (bankTypes != null && !bankTypes.isEmpty()) {
            for (String bank : bankTypes) {
                enterpriseParams.setBank(bank);
                bankAccounts.addAll(queryAllEnable(enterpriseParams));
            }
        } else {
            bankAccounts.addAll(queryAllEnable(enterpriseParams));
        }
        remainSubCurrencyList(currencyids, bankAccounts);
        return bankAccounts;
    }

    public static void remainSubCurrency(String currencyid, EnterpriseBankAcctVO bankAccount) {
        List<String> currencyids = new ArrayList<>();
        if (currencyid != null) {
            currencyids.add(currencyid);
        }
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        if (bankAccount != null) {
            bankAccounts.add(bankAccount);
        }
        remainSubCurrencyList(currencyids, bankAccounts);
    }

    public static void remainSubCurrencyList(List<String> currencyids, List<EnterpriseBankAcctVO> bankAccounts) {
        //平台会返回多余的币种子表，需要再自己过滤下
        if (CollectionUtils.isNotEmpty(currencyids) && CollectionUtils.isNotEmpty(bankAccounts)) {
            for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
                if (bankAccount.getCurrencyList() != null && !bankAccount.getCurrencyList().isEmpty()) {
                    Iterator<BankAcctCurrencyVO> iterator = bankAccount.getCurrencyList().iterator();
                    while (iterator.hasNext()) {
                        BankAcctCurrencyVO bankAcctCurrencyVO = iterator.next();
                        if (!currencyids.contains(bankAcctCurrencyVO.getCurrency())) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }



// ... existing code ...
    /**
     * 根据ID查询企业银行账户信息
     *
     * @param id 企业银行账户ID
     * @return 企业银行账户VO对象
     * @throws Exception 查询异常
     */
    public static EnterpriseBankAcctVO findById(String id) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(id);
        return getEnterpriseBankAcctService().queryByUniqueParam(enterpriseParams);
    }
// ... existing code ...


    /**
     * 根据银行账户查询企业银行账户
     * @param account
     * @return
     * @throws Exception
     */
    public static EnterpriseBankAcctVO findByAccount(String account) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setAccount(account);
        return getEnterpriseBankAcctService().queryByUniqueParam(enterpriseParams);
    }

    public static List<EnterpriseBankAcctVO> findByIdList(List<String> idList) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(idList);
        return getEnterpriseBankAcctService().queryByCondition(enterpriseParams);
    }

    public static List<String> findAccountByIdList(List<String> idList) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(idList);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = getEnterpriseBankAcctService().queryByCondition(enterpriseParams);
        List<String> accountList = enterpriseBankAcctVOList.stream().map(EnterpriseBankAcctVO::getAccount).collect(Collectors.toList());
        return accountList;
    }

    public static List<EnterpriseBankAcctVO> query(EnterpriseParams enterpriseParams) throws Exception {
        List<EnterpriseBankAcctVO> list = getEnterpriseBankAcctService().queryByCondition(enterpriseParams);
        return list;
    }


    /**
     * 获取虚拟账户信息
     * @param enterpriseParams
     * @return
     * @throws Exception
     */
    public List<EnterpriseBankAcctVO> getVirtualAccountInfo(EnterpriseParams enterpriseParams) throws Exception {
        enterpriseParams.setAcctopentype(3);
        List<EnterpriseBankAcctVO> bankAcctVOList = query(enterpriseParams);
        return bankAcctVOList == null ? new ArrayList<>() : bankAcctVOList;
    }

    /**
     * 通过账户参数 查询全量账户(数量 >> 5000)
     * @param enterpriseParams
     * @return
     * @throws Exception
     */
    public static List<EnterpriseBankAcctVO> queryAll(EnterpriseParams enterpriseParams) throws Exception {
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = new ArrayList<>();
        //通过条件查出总条数
//        int queryCountpageSize = 1;
        int queryCountpageSize = 4900;
        int queryCountpageIndex =1;
        enterpriseParams.setPageIndex(queryCountpageIndex);
        enterpriseParams.setPageSize(queryCountpageSize);
        List<String> idList = enterpriseParams.getIdList();
        List<LinkedHashMap> thisVos = new ArrayList<>();
        if (idList != null && idList.size() > 450) {
            List<List<String>> partition = Lists.partition(idList, 450);
            if (!partition.isEmpty()) {
                for (List<String> list : partition) {
                    EnterpriseParams newParams = new EnterpriseParams();
                    BeanUtils.copyProperties(enterpriseParams, newParams);
                    newParams.setIdList(list);
                    ResultPager resultPager = getEnterpriseBankAcctService().queryPageByCondition(newParams);
                    if(resultPager.getRecordList() != null && resultPager.getRecordList().size() > 0) {
                        thisVos.addAll(resultPager.getRecordList());
                    }
                }
            }
        } else {
            int queryCount = 0;
            do{
                enterpriseParams.setPageIndex(queryCountpageIndex);
                addOrderByIdDesc(enterpriseParams);
                ResultPager resultPager = getEnterpriseBankAcctService().queryPageWithRange(enterpriseParams);
                if(queryCountpageIndex==1){
                    int recordCount = resultPager.getRecordCount();
                    queryCount = (int)Math.ceil(recordCount/(queryCountpageSize*1.0));
                }
                if(null==resultPager|| org.springframework.util.CollectionUtils.isEmpty(resultPager.getRecordList())){
                    break;
                }
                if(resultPager.getRecordList() != null && resultPager.getRecordList().size() > 0){
                    thisVos.addAll(resultPager.getRecordList());
                }
                queryCountpageIndex++;
            }while (queryCountpageIndex<=queryCount);
//            ResultPager resultPager = getEnterpriseBankAcctService().queryPageByCondition(enterpriseParams);
//            if(resultPager.getRecordList() != null && resultPager.getRecordList().size() > 0){
//                thisVos.addAll(resultPager.getRecordList());
//            }
        }
        if(!thisVos.isEmpty()){
            for (LinkedHashMap map : thisVos) {
                EnterpriseBankAcctVO vo = null;
                try {
                    vo = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(map), EnterpriseBankAcctVO.class);
                    enterpriseBankAcctVOs.add(vo);
                } catch (Exception e) {
                    log.error("查询银行账户转换异常：queryAllWithRange error",e);
//                    vo = new EnterpriseBankAcctVOWithRange();
//                    if(map != null && map.get("id") != null){
//                        vo.setId(map.get("id").toString());
//                        enterpriseBankAcctVOs.add(vo);
//                    }
                }
            }
        }
        return enterpriseBankAcctVOs;
    }

    private static void addOrderByIdDesc(EnterpriseParams enterpriseParams) {
        BdQueryOrderby bdQueryOrderby = new BdQueryOrderby();
        bdQueryOrderby.setField("id");
        bdQueryOrderby.setOrder("desc");
        List<BdQueryOrderby> queryOrderbys = new ArrayList<>();
        queryOrderbys.add(bdQueryOrderby);
        enterpriseParams.setQueryOrderbys(queryOrderbys);
    }


    /**
     * 通过账户参数 查询全量账户(数量 >> 5000)
     * @param enterpriseParams
     * @return
     * @throws Exception
     */
    public List<EnterpriseBankAcctVO> queryAllForInitData(EnterpriseParams enterpriseParams) throws Exception {
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = new ArrayList<>();
        //通过条件查出总条数
        int queryCountpageSize = 1;
        int queryCountpageIndex =1;
        enterpriseParams.setPageIndex(queryCountpageIndex);
        enterpriseParams.setPageSize(queryCountpageSize);
        ResultPager resultPager = getEnterpriseBankAcctService().queryPageByCondition(enterpriseParams);
        //通过查出的总条数 进行分页次数计算
        int pageSize = 4500;
        int pageIndex =1;
        int recordCount = resultPager.getRecordCount();
        int queryCount = recordCount/pageSize <= 0? 1 : (recordCount/pageSize) +
                (recordCount%pageSize ==0?0:1);
        enterpriseParams.setPageSize(pageSize);
        while(pageIndex<=queryCount){
            List<LinkedHashMap> thisVos = new ArrayList<>();
            thisVos.clear();
            enterpriseParams.setPageIndex(pageIndex);
            thisVos = getEnterpriseBankAcctService().queryPageByCondition(enterpriseParams).getRecordList();
            pageIndex++;
            if(thisVos!=null && !thisVos.isEmpty()){
                for(LinkedHashMap map : thisVos){
                    EnterpriseBankAcctVO vo = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(map),EnterpriseBankAcctVO.class);
                    enterpriseBankAcctVOs.add(vo);
                }
            }
        }
        return enterpriseBankAcctVOs;
    }

    /**
     * 通过账户参数 查询全量账户 只查询账户和币种都启用的
     * @param enterpriseParams
     * @return
     * @throws Exception
     */
    public static List<EnterpriseBankAcctVOWithRange> queryAllEnableWithRange(EnterpriseParams enterpriseParams) throws Exception {
        // 过滤停用账户
        List<Integer> enables = new ArrayList<>();
        enables.add(1);
        enterpriseParams.setEnables(enables);
        // 过滤停用币种
        enterpriseParams.setCurrencyEnable(1);
        return queryAllWithRange(enterpriseParams);
    }

    /**
     * 通过账户参数 查询全量账户 只查询账户和币种都启用的
     * @param enterpriseParams
     * @return
     * @throws Exception
     */
    public static List<EnterpriseBankAcctVO> queryAllEnable(EnterpriseParams enterpriseParams) throws Exception {
        // 过滤停用账户
        List<Integer> enables = new ArrayList<>();
        enables.add(1);
        enterpriseParams.setEnables(enables);
        // 过滤停用币种
        enterpriseParams.setCurrencyEnable(1);
        return queryAll(enterpriseParams);
    }

    /**
     * 通过账户参数 查询全量账户(数量 >> 5000)
     * @param enterpriseParams
     * @return onpremise-V3R5_20240518_SP_bugfix.906QP
     * @throws Exception
     */
    public static List<EnterpriseBankAcctVOWithRange> queryAllWithRange(EnterpriseParams enterpriseParams) throws Exception {
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOWithRanges = new ArrayList<>();
        //通过查出的总条数 进行分页次数计算
        int pageSize = 4500;
        //通过条件查出总条数
        int pageIndex =1;
        int queryCount = 0;
        do{
            enterpriseParams.setPageIndex(pageIndex);
            enterpriseParams.setPageSize(pageSize);
            addOrderByIdDesc(enterpriseParams);
            ResultPager resultPager = getEnterpriseBankAcctService().queryPageWithRange(enterpriseParams);
            if(pageIndex==1){
                int recordCount = resultPager.getRecordCount();
                queryCount = (int)Math.ceil(recordCount/(pageSize*1.0));
            }
            if(null==resultPager|| org.springframework.util.CollectionUtils.isEmpty(resultPager.getRecordList())){
                break;
            }
            List<LinkedHashMap> thisVos = resultPager.getRecordList();
            for(LinkedHashMap map : thisVos){
                EnterpriseBankAcctVOWithRange vo = null;
                try {
//                    log.error("查询银行账户转换：queryAllWithRange error,银行账户信息：", JSONObject.toJSONString(map));
                    vo = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(map), EnterpriseBankAcctVOWithRange.class);
                    enterpriseBankAcctVOWithRanges.add(vo);
                } catch (Exception e) {
                    if(map != null && map.get("id") != null){
                        log.error("查询银行账户转换异常：queryAllWithRange error，对应账户id为"+map.get("id").toString(),e);
                    }
                }
            }
            pageIndex++;
        }while (pageIndex<=queryCount);
        return enterpriseBankAcctVOWithRanges;
    }

    /**
     * 如果只是想按 id查询，启用停用都想查， enterpriseParams 传enables 在 （1,2）的 ，把 enable 字段置空，不然会带默认值*
     * @param id
     * @return
     * @throws Exception
     */
    public EnterpriseBankAcctVO findByIdAndEnable(String id) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(id);
        enterpriseParams.setEnables(null);
        enterpriseParams.setEnables(Arrays.asList(1,2));
        return getEnterpriseBankAcctService().queryByUniqueParam(enterpriseParams);
    }


    /**
     * 根据银行类别id查询银行类别name*
     *
     * @param id
     * @return
     * @throws Exception
     */
    public BankVO querybankTypeNameById(String id) throws Exception {
        if (StringUtils.isEmpty(id)) return null;
        BdRequestParams bdRequestParams = new BdRequestParams();
        bdRequestParams.setId(id);
        return iBankService.queryByUniqueParam(bdRequestParams);
    }

    /**
     * 根据银行网点id查询联行号*
     * @param id
     * @return
     * @throws Exception
     */
    public BankdotVO querybankNumberlinenumberById(String id) throws Exception {
        if (StringUtils.isEmpty(id)) return null;
        /*BdRequestParams bdRequestParams = new BdRequestParams();
        bdRequestParams.setId(id);*/
        return CtmAppContext.getBean(ICtmBankdotService.class).queryBankdotById(id);
    }

    public EnterpriseBankAcctVOWithRange queryEnterpriseBankAcctVOWithRangeById(String id) throws Exception {
        EnterpriseParams params = new EnterpriseParams();
        params.setId(id);
        return getEnterpriseBankAcctService().queryUniqueWithRange(params);
    }


    public List<EnterpriseBankAcctVOWithRange> queryEnterpriseBankAcctVOWithRangeByCondition(EnterpriseParams params) throws Exception {
        return getEnterpriseBankAcctService().queryListWithRange(params);
    }

    // 该方法仅用于实时余额 历史余额 交易明细 交易回单银行账户过滤
    public List<String> getAccounts(Collection<String> orgids) throws Exception {
        EnterpriseParams newEnterpriseParams = new EnterpriseParams();
        newEnterpriseParams.setOrgidList(new ArrayList<>(orgids));
        newEnterpriseParams.setEnables(Arrays.asList(1,2));
        List<String> accounts = new ArrayList<>();
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = queryAllWithRange(newEnterpriseParams);
        for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS){
            accounts.add(enterpriseBankAcctVO.getId());
        }
        // 防止没有数据,参照到所有数据 CZFW-317913
        if(accounts.size() == 0){
            accounts.add("0");
        }
        return accounts;
    }

    // 该方法仅用于实时余额 历史余额 交易明细 交易回单银行账户过滤
    public List<String> getAccountsByAccentity(Collection<String> orgids) throws Exception {
        EnterpriseParams newEnterpriseParams = new EnterpriseParams();
        newEnterpriseParams.setOrgidList(new ArrayList<>(orgids));
        newEnterpriseParams.setEnables(Arrays.asList(1,2));
        List<String> accounts = new ArrayList<>();
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = queryAll(newEnterpriseParams);
        for(EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOS){
            accounts.add(enterpriseBankAcctVO.getId());
        }
        // 防止没有数据,参照到所有数据 CZFW-317913
        if(accounts.size() == 0){
            accounts.add("0");
        }
        return accounts;
    }

    /**
     * 根据组织ID列表查询账户ID列表，按所属组织过滤,入参可为空时，为空时按当前用户组织权限过滤
     * @param orgIds 所属组织id列表
     * @return
     * @throws Exception
     */
    public List<String> queryAccountIdsByOrgList(List<String> orgIds) throws CtmException {
       if (CollectionUtils.isEmpty(orgIds)) {
           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102370"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590003C", "所属组织不能为空!") /* "所属组织不能为空!" */);
       }
       List<String> resAccountIds;
       try {
           resAccountIds = getEnterpriseBankAcctService().queryAccountIdsByOrgList(orgIds, null);
       } catch (Exception e) {
           log.error("查询企业银行账户服务异常:{}",e.getMessage(), e);
           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102371"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590003B", "查询企业银行账户服务异常:%s") /* "查询企业银行账户服务异常:%s" */,e.getMessage()));
       }
       return resAccountIds;
    }

     /**
     * 根据组织ID列表查询账户ID列表，按使用组织过滤,入参可为空时，为空时按当前用户组织权限过滤 包括启用和停用状态
     * @param orgIds 使用组织id列表
     * @return
     * @throws Exception
     */
    public List<String> queryAccountIdsByOrgListWithRange(List<String> orgIds) throws CtmException {
       if (CollectionUtils.isEmpty(orgIds)) {
           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102372"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590003D", "使用组织不能为空!") /* "使用组织不能为空!" */);
       }
       List<String> resAccountIds;
       try {
           resAccountIds = getEnterpriseBankAcctService().queryAccountIdsByOrgListWithRange(orgIds, null);
       } catch (Exception e) {
           log.error("查询企业银行账户服务异常:{}",e.getMessage(), e);
           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102371"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590003B", "查询企业银行账户服务异常:%s") /* "查询企业银行账户服务异常:%s" */,e.getMessage()));
       }
       return resAccountIds;
    }


}
