package com.yonyoucloud.fi.cmp.initdata.service;

import com.google.common.collect.Lists;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceAdjustService;
import com.yonyoucloud.fi.cmp.cmpentity.CurrencyRateTypeCode;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.bd.period.Period;
import com.yonyoucloud.fi.cmp.cmpentity.MoneyForm;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.UpgradeSignEnum;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.initdata.InitDataConstant;
import com.yonyoucloud.fi.cmp.initdata.InitDatab;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.ExchangeRateTypeQueryService;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InitDataServiceImpl implements InitDataService {

    private final CmCommonService cmCommonService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    public InitDataServiceImpl(CmCommonService cmCommonService) {
        this.cmCommonService = cmCommonService;
    }
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private ExchangeRateTypeQueryService exchangeRateTypeQueryService;
    @Autowired
    private BalanceAdjustService balanceAdjustService;

    private static final String SUCCESSCOUNT = "successCount";
    private static final String COUNT = "count";
    private static final String TIPMSGLIST = "tipMsgList";
    private static final String NOINITACCIDLIST = "noInitAccIdList";

    @Override
    public void batchSave(List<InitData> settlementList) throws Exception {
        EntityTool.setUpdateStatus(settlementList);
        MetaDaoHelper.update(InitData.ENTITY_NAME, settlementList);
    }

    @Override
    public CtmJSONObject queryHvEditState(String accentity, String currency) throws Exception {
        CtmJSONObject json = new CtmJSONObject();
        if ("".equals(accentity) || "".equals(currency) || "null".equals(accentity) || "null".equals(currency)) {
            json.put("flag", false);
        } else {
            //查询会计主体的币种与银行账户的币种是否一致
            String acc_currency = AccentityUtil.getNatCurrencyIdByAccentityId(accentity);
            if (acc_currency.equals(currency)) {
                json.put("flag", true);
            } else {
                json.put("flag", false);
            }
        }
        return json;
    }

    /**
     * 账户期初同步
     * @param billNumber
     * @return
     */
    @Override
    public CtmJSONObject importData(String billNumber) {
        int count = 0;
        int totalCount = 0;
        //存储开通失败的账户id集合
        List<String> noInitAccIdList = new ArrayList<>();
        //存储开通失败的错误信息
        List<String> tipMsgList = new ArrayList<>();
        //返回数据集合
        Map <String,Object> resultMessage= new HashMap<>();
        ResultVo resultVo = new ResultVo();
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockWithOutTrace(billNumber);
            if(null == ymsLock){
                throw new CtmException(new CtmErrorCode("033-502-100082"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D1","该组织下正在初始化数据，请稍后再试！") /* "该组织下正在初始化数据，请稍后再试！" */);//@notranslate
            }
            //查询所有的期初数据 包含子表
            List<InitData> initDataList = queryAllInitData();
            List<InitData> allBankaccountInitDataList = queryAllInitDataOfBankaccount();
            // 升级校验只针对同步过账户期初的数据，且同步账户期初时只针对银行账户，不要校验现金账户
            if(CollectionUtils.isNotEmpty(allBankaccountInitDataList) && "cmp_initdatayhlist".equals(billNumber)){
                checkUpgradesignData();
            }
            //银行账户集合
            Set<String> bankAccountSet = new HashSet<>();
            //现金账户集合
            Set<String> cashAccountSet = new HashSet<>();
            for (InitData initData : initDataList) {
                if (ValueUtils.isNotEmptyObj(initData.get(InitDataConstant.BANKACCOUNT))) {
                    bankAccountSet.add(initData.getAccentity() + initData.get(InitDataConstant.BANKACCOUNT).toString() + initData.get(IBussinessConstant.CURRENCY));
                }
                if (ValueUtils.isNotEmptyObj(initData.get(InitDataConstant.CASHACCOUNT))) {
                    cashAccountSet.add((String) initData.get(InitDataConstant.CASHACCOUNT));
                }
            }
            //获取资金组织id集合 用与后续查询
            List<String> orgIdList = AccentityUtil.getFundsIdsByEnabledCondition();
            //获取资金组织对应的期初期间
            Map<String, Period> periodMap = queryListFinanceOrg(orgIdList);
            //汇率类型，暂存集合
            Map<String, ExchangeRateTypeVO> exchangeRateTypeMap = new HashMap<>();
            // 银行账户期初
            if (InitDataConstant.CMP_INITDATAYHLIST.equals(billNumber)) {
                //银行账户期初
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                //查询符合权限条件的会计主体id集合
                List<String> orgPermissionsList = new ArrayList<>(BillInfoUtils.getOrgPermissionsByAuth(IServicecodeConstant.BANKINITDATA));
                enterpriseParams.setOrgidList(orgPermissionsList);
                //查询已启用的币种
                List<CurrencyTenantDTO> currencylist = getCurrencyListOfEnable();
                List<String> idList = currencylist.stream().map(CurrencyTenantDTO::getId).collect(Collectors.toList());
                enterpriseParams.setCurrencyIDList(idList);
                enterpriseParams.setCurrencyEnable(1);
                List<EnterpriseBankAcctVOWithRange> dataList = EnterpriseBankQueryService.queryAllWithRange(enterpriseParams);

                if (CollectionUtils.isEmpty(dataList)){
                    resultVo.setCount(0);
                    resultVo.setFailCount(0);
                    resultVo.setSucessCount(0);
                    resultVo.setMessage(String.format(ResultMessage.data(InternationalUtils.getMessageWithDefault
                            ("UID:P_CM-BE_17FE8C54041801B5", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19FE58200490000C","同步账户成功，共同步[%s]条数据！") /* "同步账户成功，共同步[%s]条数据！" */) /* "同步账户成功，共同步[%s]条数据！" */),totalCount));//@notranslate
                    return resultVo.getResult("",tipMsgList,totalCount,count,noInitAccIdList.size(),new CtmJSONObject());
                }
                resultMessage = insertCmpInitdatayhlist(dataList ,orgPermissionsList , bankAccountSet,orgIdList, periodMap, exchangeRateTypeMap);
            } else if (InitDataConstant.CMP_INITDATAXJLIST.equals(billNumber)) {
                // 现金账户期初
                resultMessage =  insertCmpInitdataxjlist(cashAccountSet,orgIdList,periodMap, exchangeRateTypeMap);
            }
        } catch (Exception e) {
            log.error("同步账户失败 error{}!", e.getMessage(), e);
            throw new CtmException(new CtmErrorCode("033-502-100734"),e.getMessage(), e);
        } finally {
            JedisLockUtils.unlockWithOutTrace(ymsLock);
        }
        resultVo.setCount(Integer.parseInt(resultMessage.get(COUNT).toString()));
        resultVo.setFailCount(((List)resultMessage.get(NOINITACCIDLIST)).size());
        resultVo.setSucessCount(Integer.parseInt(resultMessage.get(SUCCESSCOUNT).toString()));
        resultVo.setMessages((List)resultMessage.get(TIPMSGLIST));
        return resultVo.getResult("",resultVo.getMessages(),resultVo.getCount(),resultVo.getSucessCount(),resultVo.getFailCount(),new CtmJSONObject());
    }

    /**
     * 获取已启用的币种
     * @return
     * @throws Exception
     */
private List<CurrencyTenantDTO> getCurrencyListOfEnable() throws Exception {
    List<CurrencyTenantDTO> currencylist = new ArrayList<>();
    CurrencyBdParams currencyBdParams = new CurrencyBdParams();
    currencyBdParams.setEnables(Arrays.asList(1));
    currencyBdParams.setPageSize(500);
    List<CurrencyTenantDTO> tmpcurrencylist;
    int pageIndex = 1;
    do {
        currencyBdParams.setPageIndex(pageIndex++);
        tmpcurrencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
        if (CollectionUtils.isNotEmpty(tmpcurrencylist)) {
            currencylist.addAll(tmpcurrencylist);
        }
    } while (CollectionUtils.isNotEmpty(tmpcurrencylist));
    return currencylist;
}

    /**
     * 校验当前用户是否升级过 如果没有升级 则不允许进行账户同步
     * @throws Exception
     */
    private void checkUpgradesignData() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("upgradesign").is_not_null());
        conditionGroup.addCondition(QueryCondition.name("bankaccount").is_not_null());
        querySchema.addCondition(conditionGroup);
        querySchema.setLimitCount(1);
        List<InitData> initDataList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchema,null);
        if(CollectionUtils.isEmpty(initDataList)){
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC0410001F", "当前租户没有进行过期初数据升级，请先通过现金运营工具升级期初数据，再进行账户同步。") /* "当前租户没有进行过期初数据升级，请先通过现金运营工具升级期初数据，再进行账户同步。" */);//@notranslate
        }
    }

    /**
     * 获取所有的账户期初信息
     * @return
     * @throws Exception
     */
    private List<InitData> queryAllInitData() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("accentity,bankaccount,cashaccount,currency");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("accentity").is_not_null());
        querySchema.addCondition(conditionGroup);
        List<InitData> initDataList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchema,null);
        return initDataList;
    }

    /**
     * 获取所有的账户期初信息
     * @return
     * @throws Exception
     */
    private List<InitData> queryAllInitDataOfBankaccount() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("accentity,bankaccount,cashaccount,currency");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("accentity").is_not_null());
        conditionGroup.addCondition(QueryCondition.name("bankaccount").is_not_null());
        querySchema.addCondition(conditionGroup);
        List<InitData> initDataList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchema,null);
        return initDataList;
    }


    private List<String> getorgIdList() throws Exception {
        QueryConditionGroup billContextFinanceOrg = QueryConditionGroup.and(QueryCondition.name("stopstatus").eq("0"), QueryCondition.name("dr").eq(0));
        QuerySchema querySchemaFinanceOrg = QuerySchema.create().addSelect("id,tenant,exchangerate,name").addCondition(billContextFinanceOrg);
        List<Map<String, Object>> dataListFinanceOrg=MetaDaoHelper.query("aa.baseorg.FinanceOrgMV", querySchemaFinanceOrg, ISchemaConstant.MDD_SCHEMA_ORGCENTER);
        List<String> orgIdList = new ArrayList<>();
        //这里的汇率没有作用 故而先注释
//            Map<String, String> exchangerateMap = new HashMap<>();
        for (Map<String, Object> map : dataListFinanceOrg) {
            orgIdList.add((String) map.get("id"));
//                exchangerateMap.put((String) map.get("id"), (String) map.get("exchangerate"));
        }
        return orgIdList;
    }

    /**
     * 获取会计主体对应的期初期间
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Period>  queryListFinanceOrg(List<String> orgIdList) throws Exception {
        //获取会计主体对应的期初期间
        Map<String, Period> periodMap = new HashMap<>();
        List<Map<String, Object>> result = QueryBaseDocUtils.queryOrgBpOrgConfVO(orgIdList, ISystemCodeConstant.ORG_MODULE_CM);/* 暂不修改 已登记*/
        if (result != null ) {
            for (Map<String, Object> map : result) {
                Period periodVO = new Period();
                if (map.get("periodid") != null && map.get("begindate") != null) {
                    periodVO.setCode((String) map.get("type_code"));
                    periodVO.setBegindate((Date) map.get("begindate"));
                    periodVO.setEnddate((Date) map.get("enddate"));
                }else{
                    //20240723 新需求 如果有没有查询到当前现金期初 取当前业务日期
                    periodVO.setBegindate(BillInfoUtils.getBusinessDate());
                }
                if(map.get(InitDataConstant.ORGID)!=null){
                    periodMap.put((String) map.get(InitDataConstant.ORGID), periodVO);
                }
            }
        }
        return periodMap;
    }

    /**
     * 银行账户同步
     * @param dataList
     * @param orgPermissionsList
     * @param bankAccountSet
     * @param orgIdList
     * @param periodMap
     * @param exchangeRateTypeMap
     * @return
     * @throws Exception
     */
    private Map <String,Object> insertCmpInitdatayhlist(List<EnterpriseBankAcctVOWithRange> dataList,List<String> orgPermissionsList,Set<String> bankAccountSet,List<String> orgIdList,
                                         Map<String, Period> periodMap, Map<String, ExchangeRateTypeVO> exchangeRateTypeMap) throws Exception {
        //存储开通失败的账户id集合
        List<String> noInitAccIdList = new ArrayList<>();
        //存储开通失败的错误信息
        List<String> tipMsgList = new ArrayList<>();
        int count = 0;

        String natCurrency;
        List<InitData> initDataListNew = new ArrayList<>();
        // 根据银行账户id对企业银行账户返回的数据去重
        List<EnterpriseBankAcctVOWithRange> distinctDataList = removeDuplicateDataById(dataList);

        for (EnterpriseBankAcctVOWithRange bankAcctVO : distinctDataList) {
            if(bankAcctVO.getCurrencyList() == null){
                continue;
            }
            List<OrgRangeVO> orgRangeVOList = bankAcctVO.getAccountApplyRange();
            List<String> orgRangeList = new ArrayList<>();
            //存储提示信息使用组织的name key：组织id； value:组织name
            Map<String,String> orgRangeMap = new HashMap<>();
            for(OrgRangeVO org : orgRangeVOList){
                orgRangeList.add(org.getRangeOrgId());
                orgRangeMap.put(org.getRangeOrgId(),org.getRangeOrgIdName());
            }
            for(BankAcctCurrencyVO curreny :bankAcctVO.getCurrencyList()){
                if (!ValueUtils.isNotEmptyObj(curreny.getCurrency())){
                    continue;
                }
                for(String orgId : orgRangeList){
                    if(!orgIdList.contains(orgId)){
                        continue;
                    }
                    if(!orgPermissionsList.contains(orgId)){
                        continue;
                    }
                    String bankAccountKey = orgId + bankAcctVO.getId() + curreny.getCurrency();
                    if (bankAccountSet.contains(bankAccountKey) ) {
                        continue;
                    }
                    if (periodMap.get(orgId) != null) {
                        // 适配汇率类型
                        buildExchangeRateTypeMap(exchangeRateTypeMap, orgId, orgRangeMap.get(orgId));
                        InitData initData = new InitData();
                        initData.setAccentity(orgId);
                        natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(orgId);
                        initData.setNatCurrency(natCurrency);
                        initData.setBankaccount(bankAcctVO.getId());
                        initData.setBankaccountno(bankAcctVO.getAccount());
                        initData.setMoneyform(MoneyForm.bankaccount);
                        initAccountDate(periodMap, bankAcctVO.getOrgid(), initData, bankAcctVO);
                        initData.setCurrency(curreny.getCurrency());
                        initData.setParentAccentity(bankAcctVO.getOrgid());
                        initData.setUpgradesign(UpgradeSignEnum.ADDNEW.getValue());
                        initData(initData, exchangeRateTypeMap.get(orgId), curreny.getCurrency(), natCurrency, initData.getAccountdate());
                        initDataListNew.add(initData);
                    }else{
                        //已经建立账户 且 查不到对应会计主体现金启用期初的才提示
                        tipMsgList.add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100020", "账户使用组织 %s，未设置现金管理期初，无法同步账户，建议前往数字化建模->组织管理->业务单元，进行期初设置。") /* "账户使用组织 %s，未设置现金管理期初，无法同步账户，建议前往数字化建模->组织管理->业务单元，进行期初设置。" */, orgRangeMap.get(orgId)));//@notranslate
                        noInitAccIdList.add(bankAcctVO.getId());
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(initDataListNew)){
            count = initDataListNew.size();
            CmpMetaDaoHelper.insert(InitData.ENTITY_NAME, initDataListNew);
        }
        Map <String,Object> resultMap = new HashMap<>();
        resultMap.put(COUNT,count+noInitAccIdList.size());
        resultMap.put(SUCCESSCOUNT,count);
        resultMap.put(TIPMSGLIST,tipMsgList);
        resultMap.put(NOINITACCIDLIST,noInitAccIdList);
        return resultMap;
    }

    /**
     * 构建汇率类型Map
     * @param exchangeRateTypeMap 汇率类型Map
     * @param orgId  组织id
     * @param orgName 组织名称
     */
    private void buildExchangeRateTypeMap(Map<String, ExchangeRateTypeVO> exchangeRateTypeMap, String orgId, String orgName) throws Exception {
        if (exchangeRateTypeMap.containsKey(orgId)) {
            return;
        }
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(orgId, true);
        if (defaultExchangeRateType == null || defaultExchangeRateType.getId() == null) {
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21BD18DC05280005", "资金组织:【%s】对应会计主体下无默认汇率类型，请检查数据！") /* "资金组织:【%s】对应会计主体下无默认汇率类型，请检查数据！" */, orgName));
        }
        exchangeRateTypeMap.put(orgId, defaultExchangeRateType);
    }

    /**
     * 根据id对企业银行账户返回的数据去重
     * @param dataList
     * @return
     */
    private List<EnterpriseBankAcctVOWithRange> removeDuplicateDataById(List<EnterpriseBankAcctVOWithRange> dataList) {
        Set<String> uniqueIds = new HashSet<>();
        List<EnterpriseBankAcctVOWithRange> distinctDataList = new ArrayList<>();
        for (EnterpriseBankAcctVOWithRange item : dataList) {
            if (item != null && item.getId() != null) {
                if (!uniqueIds.contains(item.getId())) {
                    uniqueIds.add(item.getId());
                    distinctDataList.add(item);
                }
            }
        }
        return distinctDataList;
    }

    /**
     * 现金账户同步
     * @param cashAccountSet
     * @param orgIdList
     * @param periodMap
     * @param exchangeRateTypeMap
     * @return
     * @throws Exception
     */
    private Map <String,Object> insertCmpInitdataxjlist(Set<String> cashAccountSet,List<String> orgIdList,Map<String, Period> periodMap, Map<String, ExchangeRateTypeVO> exchangeRateTypeMap) throws Exception {
        //存储开通失败的账户id集合
        List<String> noInitAccIdList = new ArrayList<>();
        //存储开通失败的错误信息
        List<String> tipMsgList = new ArrayList<>();
        int count = 0;
        //现金账户期初
        //查询符合权限条件的会计主体id集合
        List<String> orgPermissionsList = new ArrayList<>();
        orgPermissionsList.addAll(BillInfoUtils.getOrgPermissionsByAuth(IServicecodeConstant.CASHINITDATA));
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setPageSize(4999);
        enterpriseParams.setOrgidList(orgPermissionsList);
        List<EnterpriseCashVO>  dataList = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams);
        Map <String,Object> resultMap = new HashMap<>();
        if (CollectionUtils.isEmpty(dataList)) {
            resultMap.put(COUNT,count);
            resultMap.put(SUCCESSCOUNT,count);
            resultMap.put(TIPMSGLIST,tipMsgList);
            resultMap.put(NOINITACCIDLIST,noInitAccIdList);
            return resultMap;
        }
        // 获取企业现金账户开户日期Map
        Map<String, Object> openAccountMap = getOpenAccountMap(dataList);
        List<InitData> initDataListNew = new ArrayList<>();
        String natCurrency;
        Map<String,Object> paramOpenAcctMap = new HashMap<>();
        for (EnterpriseCashVO map : dataList) {
            String orgId = map.getOrgid();
            if (!orgIdList.contains(String.valueOf(orgId)) || cashAccountSet.contains(map.getId())) {
                continue;
            }
            if (!ValueUtils.isNotEmptyObj(map.getCurrency())){
                continue;
            }
            if (periodMap.get(orgId) != null) {
                Date beginDate = periodMap.get(orgId).getBegindate();
                beginDate = DateUtils.dateAddDays(beginDate, -1);
                InitData initData = new InitData();
                initData.setAccentity(orgId);
                initData.setParentAccentity(orgId);
                // 适配汇率类型
                buildExchangeRateTypeMap(exchangeRateTypeMap, orgId, map.getOrgidName());
                natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(orgId);
                initData.setNatCurrency(natCurrency);
                initData.setCashaccount(map.getId());
                initData.setCashaccountno(map.getCode());
                initData.setCurrency(map.getCurrency());
                initData.setMoneyform(MoneyForm.cashstock);
                paramOpenAcctMap.put("accountOpenDate", openAccountMap.get(map.getId()));
                initCashAccountDate(periodMap, map.getOrgid(), initData, paramOpenAcctMap);
                initData.setUpgradesign(UpgradeSignEnum.ADDNEW.getValue());
                initData(initData, exchangeRateTypeMap.get(orgId), map.getCurrency(), natCurrency, beginDate);
                initDataListNew.add(initData);
            }else{
                //已经建立账户 且 查不到对应会计主体现金启用期初的才提示
                noInitAccIdList.add((String) map.getId());
                tipMsgList.add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19FE58200490000B","会计主体 %s，未设置现金管理期初，无法同步账户，建议前往数字化建模->组织管理->业务单元，进行期初设置。") /* "会计主体 %s，未设置现金管理期初，无法同步账户，建议前往数字化建模->组织管理->业务单元，进行期初设置。" */, map.getOrgidName()));//@notranslate
            }
        }
        if (initDataListNew.size() > 0) {
            count = initDataListNew.size();
            CmpMetaDaoHelper.insert(InitData.ENTITY_NAME, initDataListNew);
        }

        resultMap.put(COUNT,count+noInitAccIdList.size());
        resultMap.put(SUCCESSCOUNT,count);
        resultMap.put(TIPMSGLIST,tipMsgList);
        resultMap.put(NOINITACCIDLIST,noInitAccIdList);
        return resultMap;
    }

    /**
     * 获取现金账户的开户日期Map
     * @param dataList
     * @return
     */
    private Map<String, Object> getOpenAccountMap(List<EnterpriseCashVO> dataList) throws Exception {
        List<String> cashIdList = dataList.stream().distinct().map(EnterpriseCashVO::getId).collect(Collectors.toList());
        List<Map<String,Object>> allCashMapList = new ArrayList<>(cashIdList.size());
        if (cashIdList.size() > 500) {
            List<List<String>> cashIdPartitionList = Lists.partition(cashIdList, 500);
            for (List<String> childList : cashIdPartitionList) {
                allCashMapList.addAll(QueryBaseDocUtils.queryEnterpriseCashAccountByIds(childList));
            }
        } else {
            allCashMapList.addAll(QueryBaseDocUtils.queryEnterpriseCashAccountByIds(cashIdList));
        }
        Map<String, Object> openAccountMap = new HashMap<>();
        for (Map<String, Object> map : allCashMapList) {
            openAccountMap.put(map.get("id").toString(), map.get("accountOpenDate"));
        }
        return openAccountMap;
    }

    /**
     * 初始化建账日期
     *
     */
    @Override
    public void initAccountDate(Map<String, Period> periodMap, String orgId, InitData initData, EnterpriseBankAcctVOWithRange bankAcctVO) {
        if(periodMap.get(orgId) != null ){
            initData.setPeriod((Long) periodMap.get(orgId).getId());
            Date beginDate = periodMap.get(orgId).getBegindate();
            Date yesterdayBeginDate = DateUtils.dateAddDays(beginDate, -1);
             if (bankAcctVO != null) {
                //1资金组织现金管理期初日期大于账户开户日期 :资金组织现金管理期初开始日期前一天
                if(bankAcctVO.getAccountOpenDate()!=null && yesterdayBeginDate.compareTo(bankAcctVO.getAccountOpenDate())>=0){
                    initData.setAccountdate(yesterdayBeginDate);
                }else if(bankAcctVO.getAccountOpenDate()!=null && yesterdayBeginDate.compareTo(bankAcctVO.getAccountOpenDate())<0){
                    //2资金组织现金管理期初日期小于等于账户开户日期 :账户开户日期
                    initData.setAccountdate(bankAcctVO.getAccountOpenDate());
                }else if(bankAcctVO.getAccountOpenDate() == null){
                    //3账户开户日期没有值时	:授权主体现金管理期初日期前一天
                    initData.setAccountdate(yesterdayBeginDate);
                }
            }
        }
    }

    /**
     * 提取公共字段
     */
    private void initData(InitData initData, ExchangeRateTypeVO exchangeType, String currenyId, String natCurrency, Date accountDate) throws Exception{
        initData.setDirection(Direction.Debit);//企业方余额方向，默认为借
        initData.setBankdirection(Direction.Credit);//银行方余额方向，默认为贷
        if (exchangeType != null) {
            // 汇率类型
            initData.setExchangeRateType(exchangeType.getId());
        }
        // 原币和本币币种相同，汇率为1
        if (natCurrency.equals(currenyId)) {
            initData.setExchangerate(new BigDecimal("1"));
        } else {
            if (exchangeType != null) {
                // 获取汇率和汇率折算方式
                CmpExchangeRateVO cmpExchangeRateVO = null;
                try {
                    cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currenyId, natCurrency, accountDate, exchangeType.getId());
                } catch (Exception e) {
                    log.error("账户期初同步获取汇率异常:{}", e.getMessage(), e);
                }
                if (cmpExchangeRateVO != null &&  cmpExchangeRateVO.getExchangeRate() != null
                        && cmpExchangeRateVO.getExchangeRate().compareTo(BigDecimal.ZERO) != 0) {
                    // 汇率
                    initData.setExchangerate(cmpExchangeRateVO.getExchangeRate());
                    // 汇率折算方式
                    initData.setExchRateOps(cmpExchangeRateVO.getExchangeRateOps());
                }
            }
        }

        initData.setCreateDate(new Date());
        initData.setCreateTime(new Date());
        initData.setCreator(AppContext.getCurrentUser().getName());
        initData.setCobookoribalance(BigDecimal.ZERO);
        initData.setCobooklocalbalance(BigDecimal.ZERO);
        initData.setCoinitlocalbalance(BigDecimal.ZERO);
        initData.setCoinitloribalance(BigDecimal.ZERO);
        initData.setBankinitlocalbalance(BigDecimal.ZERO);
        initData.setBankinitoribalance(BigDecimal.ZERO);
        initData.setTenant(AppContext.getTenantId());
        initData.setQzbz(true);
        initData.setEntityStatus(EntityStatus.Insert);
        initData.setId(ymsOidGenerator.nextId());

    }

    @Override
    public CtmJSONObject upgradeInitData() throws Exception {
        List<String> ytenantIdList = QueryBaseDocUtils.getTenantList();
        log.error("upgradeInitData升级租户查询-----"+ytenantIdList.toString());
        upgradeHisInitData();
        return null;
    }

    //    @Scheduled(cron = "0 0 1 * * ?")//每天一点开始执行
    @Override
    public void scheduledUpgradeInitData(CtmJSONObject params) throws Exception {
        log.error("开始执行账户期初历史数据升级任务");
        String uid = params.getString("uid");
        //联邦查询所有的租户信息
        List<String> ytenantIdList = QueryBaseDocUtils.getTenantList();
        //构建进度条信息
        ProcessUtil.initProcess(uid,ytenantIdList.size());
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        ExecutorService taskExecutor = null;
        taskExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"scheduledUpgradeInitData-threadpool");
        try{
            taskExecutor.submit(() -> {
                try{
                    CtmLockTool.executeInOneServiceLock("UpgradeInitData", 5 * 60L, TimeUnit.SECONDS, (int lockstatus) -> {
                        if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            throw new CtmException(new CtmErrorCode("033-502-100737"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C4", "账户期初历史数据升级任务正在执行中，请勿重复执行") /* "账户期初历史数据升级任务正在执行中，请勿重复执行" */);//@notranslate
                        }
                        for (String ytenantId : ytenantIdList) {
                            try {
                                RobotExecutors.runAs(ytenantId, new Callable() {
                                    @Override
                                    public Object call() throws Exception {
                                        upgradeHisInitData();
                                        return null;
                                    }
                                }, ctmThreadPoolExecutor.getThreadPoolExecutor());
                            }catch (Exception e) {
                                log.error("账户期初历史数据升级任务失败："+ytenantId, e);
//                          throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100738"),e.getMessage());
                            }
                        }
                    });
                }catch (Exception e) {
                    log.error("scheduledUpgradeInitData-error：", e);
                }finally{
                    ProcessUtil.completed(uid);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
    }

    @Override
    public void checkSettleflag(CtmJSONObject params) throws Exception {
        //需要从前端传银行/现金账户过来
        String account = String.valueOf(params.get("account"));
        String currency = String.valueOf(params.get("currency"));
        String isBank = String.valueOf(params.get("isBank")); //是否为银行账户
        String accentity = params.getString("accentity");
        Date accountdate = params.getDate("accountdate");
        //已日结后不能修改或删除期初数据
        QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("max(accountdate) as accountdate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if ("true".equals(isBank)) {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").eq(account));
            conditionGroup.addCondition(QueryCondition.name("currency").eq(currency));
            if(accentity!=null){
                conditionGroup.addCondition(QueryCondition.name("accentity").eq(accentity));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("cashaccount").eq(account));
        }
        queryInitDataSchema.addCondition(conditionGroup);
        //根据查询条件查询日结明细
        List<SettlementDetail> settlementList = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, queryInitDataSchema);
        //对查询的数据进行判断
        if (CollectionUtils.isNotEmpty(settlementList)) {
            SettlementDetail detail = new SettlementDetail();
            detail.init(settlementList.get(0));
            Date maxAccountdate = detail.getAccountdate();
            if (null != accountdate && maxAccountdate.compareTo(accountdate) >= 0) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B4","账户期初日期已经存在日结，不允许保存。") /* "账户期初日期已经存在日结，不允许保存。" */);//@notranslate
            }
        }
    }

    @Override
    public void updateNewInitDataForOldData(CtmJSONObject params) throws Exception {
        //查询当前租户下所有的子表数据
        QuerySchema querySchema_initdata = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition_initdata = new QueryConditionGroup(ConditionOperator.and);
        //有子表期初的历史数据一定是升级标志为升级冗余或升级存疑的数据
//        condition_initdata.appendCondition(QueryConditionGroup.and(QueryCondition.name("upgradesign").in(UpgradeSignEnum.SUPERFLUITY,UpgradeSignEnum.CANTJUDGMENT)));
        condition_initdata.appendCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").is_not_null()));
        querySchema_initdata.addCondition(condition_initdata);

        QuerySchema detailSchema = QuerySchema.create().name("InitDatab").addSelect("*");
        querySchema_initdata.addCompositionSchema(detailSchema);
        List<InitData> initDataVoList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchema_initdata,null);

        List<InitData> initoldDataVoList = new ArrayList<>();
        for(InitData initdata : initDataVoList){
            if(initdata.InitDatab()!=null){
                initoldDataVoList.add(initdata);
            }
        }
        Map<String, InitData> addOldInitMap = new HashMap<>();
        for(InitData initdata : initoldDataVoList){
            for(InitDatab initb : initdata.InitDatab()){
                InitData temporaryInitData = new InitData();
                temporaryInitData.setAccentity(initb.getAccentity());
                temporaryInitData.setBankaccount(initdata.getBankaccount());
                temporaryInitData.setCurrency(initdata.getCurrency());
                temporaryInitData.setDirection(initb.getDirection());
                //当方向为贷方 且 金额大于0 这是需要转换金额为负数
                if(initb.getDirection()!=null && initb.getDirection().equals(Direction.Credit)){
                    if(initb.getCoinitloribalance().compareTo(BigDecimal.ZERO)>=0){
                        temporaryInitData.setCoinitloribalance(initb.getCoinitloribalance().negate());
                    }else
                        temporaryInitData.setCoinitloribalance(initb.getCoinitloribalance());
                    if(initb.getCoinitlocalbalance().compareTo(BigDecimal.ZERO)>=0){
                        temporaryInitData.setCoinitlocalbalance(initb.getCoinitlocalbalance().negate());
                    }else
                        temporaryInitData.setCoinitlocalbalance(initb.getCoinitlocalbalance());
                }else{
                    temporaryInitData.setCoinitloribalance(initb.getCoinitloribalance());
                    temporaryInitData.setCoinitlocalbalance(initb.getCoinitlocalbalance());
                }
                addOldInitMap.put(temporaryInitData.getAccentity()+temporaryInitData.getBankaccount()+temporaryInitData.getCurrency(),temporaryInitData);
            }
        }

        //查询升级标志为ADDNEW 手动新增的主表数据
        QueryConditionGroup conditionAddNewInit = new QueryConditionGroup();
        conditionAddNewInit.addCondition(QueryCondition.name("upgradesign").eq(UpgradeSignEnum.ADDNEW.getValue()));
        QuerySchema querySchemaAddNewInit = QuerySchema.create().addSelect("*");
        querySchemaAddNewInit.addCondition(conditionAddNewInit);
        List<InitData> initDataNewList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchemaAddNewInit,null);
        Map<String, InitData> addNewInitMap = new HashMap<>();
        for(InitData addNewInit : initDataNewList){
            addNewInitMap.put(addNewInit.getAccentity()+addNewInit.getBankaccount()+addNewInit.getCurrency(),addNewInit);
        }
        //两者对比 会计主体、账户、币种匹配 且期初金额不一致的的数据 由子表向主表赋值
        List<InitData> updateInitDataList = new ArrayList<>();
        for(String key : addOldInitMap.keySet()){
            InitData addOldInit = addOldInitMap.get(key);
            InitData addNewInit = addNewInitMap.get(key);
            if(addOldInit!=null && addNewInit!=null && addOldInit.getCoinitloribalance().compareTo(addNewInit.getCoinitloribalance())!=0){
                addNewInit.setCoinitloribalance(addOldInit.getCoinitloribalance());
                addNewInit.setCoinitlocalbalance(addOldInit.getCoinitlocalbalance());
                addNewInit.setEntityStatus(EntityStatus.Update);
                updateInitDataList.add(addNewInit);
            }
        }
        MetaDaoHelper.update(InitData.ENTITY_NAME,updateInitDataList);
    }

    @Override
    public String changeAccountDate(CtmJSONObject params) throws Exception {
        String id = params.getString("id");
        Date accountdate = DateUtils.parseDate(params.getString("accountdate"));
        String exchangeRateType = params.getString("exchangeRateType");
        String exchangerate = params.getString("exchangerate");
        Boolean isBank = params.getBoolean("isBank");
        InitData initDataVo = MetaDaoHelper.findById(InitData.ENTITY_NAME, (Object) id);
        initDataVo.setAccountdate(accountdate);
        //校验期初日期信息
        checkAccountdate(initDataVo, isBank);
        CtmJSONObject result = new CtmJSONObject();
        //期初日期修改后 获取最新汇率 重新计算余额
        if(!initDataVo.getCurrency().equals(initDataVo.getNatCurrency())){
            // 自定义汇率直接返回
            ExchangeRateTypeVO exchangeRateTypeVO = exchangeRateTypeQueryService.queryExcahngeRateTypeById(exchangeRateType);
            if (CurrencyRateTypeCode.CustomCode.getValue().equals(exchangeRateTypeVO.getCode())) {
                return ResultMessage.data(result);
            }
            //当原币 本币 不相同时重新查询汇率
            CmpExchangeRateVO cmpExchangeRateVO;
            try {
                cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(initDataVo.getCurrency(), initDataVo.getNatCurrency(), accountdate, exchangeRateType);
                if(BigDecimal.ZERO.compareTo(cmpExchangeRateVO.getExchangeRate()) == 0){
                    throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100022", "未取到汇率!") /* "未取到汇率!" */);//@notranslate
                }
            } catch (Exception e) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100022", "未取到汇率!") /* "未取到汇率!" */);//@notranslate
            }
            // 当前方传入的汇率为空，默认为0，需要进行重新寻汇
            BigDecimal exchangerateBd = new BigDecimal(StringUtils.isEmpty(exchangerate) ? "0" : exchangerate);
            if(initDataVo.getExchangerate() == null || exchangerateBd.compareTo(cmpExchangeRateVO.getExchangeRate()) != 0){
                //根据当前汇率重算期初信息
                // 本币币种精度
                CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(initDataVo.getNatCurrency());
                BigDecimal coinitlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(initDataVo.getExchRateOps(), cmpExchangeRateVO.getExchangeRate(), initDataVo.getCoinitloribalance(),  currencyOrgDTO.getMoneydigit());
                result.put("exchangerate", cmpExchangeRateVO.getExchangeRate());
                result.put("exchRateOps", cmpExchangeRateVO.getExchangeRateOps());
                result.put("coinitlocalbalance", coinitlocalbalance);
            }
        }
        return ResultMessage.data(result);
    }

    @Override
    public CtmJSONObject syncInitBalance(BizObject bizobject) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        BizObject  currentBill = MetaDaoHelper.findById(InitData.ENTITY_NAME,bizobject.getId(),3);
        if(currentBill == null){
            throw new CtmException(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100024", "银行账号【%s】关联的银行账户期初数据不存在") /* "银行账号【%s】关联的银行账户期初数据不存在" */,bizobject.getString("bankaccount_account")));//@notranslate
        }
        InitData initData = new InitData();
        initData.init(currentBill);

        //查询银行账户历史余额
        BankAccountSettingVO bankAccountSettingVO = new BankAccountSettingVO();
        bankAccountSettingVO.setAccentity(initData.getParentAccentity());
        bankAccountSettingVO.setEnableDateStr(DateUtils.dateFormat(initData.getAccountdate(),"yyyy-MM-dd"));
        bankAccountSettingVO.setBankaccount(initData.getBankaccount());
        bankAccountSettingVO.setCurrency(initData.getCurrency());
        CtmJSONObject historyBalance = balanceAdjustService.getBankAccountHistoryBalance(bankAccountSettingVO);
        if (historyBalance.getBoolean("isEmptyBalance")){
            throw new CtmException(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100021", "银行账号【%s】期初日期【%s】未维护账户历史余额，未获取到") /* "银行账号【%s】期初日期【%s】未维护账户历史余额，未获取到" */,bizobject.getString("bankaccount_account"),DateUtils.dateFormat(initData.getAccountdate(),"yyyy-MM-dd")));//@notranslate
        }
        //币种精度
        CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(initData.getCurrency());
        BigDecimal bankye = historyBalance.getBigDecimal("bankye").setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount());;
        //银行方期初余额
        initData.setBankinitoribalance(bankye);
        //判断授权使用组织
        //可获取到且账户仅授权给一个组织，则默认赋值“企业方期初余额”也为该账户余额；可获取到多个授权使用组织时，则不默认赋值“企业方期初余额
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(initData.getBankaccount());
        List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();
        if (orgRangeVOList != null && orgRangeVOList.size() == 1){
            initData.setCoinitloribalance(bankye);
        }

        //处理数据计算逻辑
        BigDecimal currencyRateNew = initData.getExchangerate();
        if(currencyRateNew != null){
            BigDecimal coinitlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(initData.getExchRateOps(), currencyRateNew, initData.getCoinitloribalance(), currencyOrgDTO.getMoneydigit());
            BigDecimal bankinitlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(initData.getExchRateOps(), currencyRateNew, initData.getBankinitoribalance(), currencyOrgDTO.getMoneydigit());
            initData.setBankinitlocalbalance(bankinitlocalbalance);
            initData.setCoinitlocalbalance(coinitlocalbalance);
        }
        initData.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(InitData.ENTITY_NAME,initData);

        result.put("dealSuccess",true);
        return result;
    }

    private void checkAccountdate(InitData initDataVo, Boolean isBank) throws Exception {
        //1期初日期不能小于资金组织现金管理期初开始日期的前一天
        Map<String, Object> periodVOresult = QueryBaseDocUtils.queryOrgBpOrgConfVO(initDataVo.getAccentity(), ISystemCodeConstant.ORG_MODULE_CM).get(0);
        if(periodVOresult != null && periodVOresult.get("begindate")!=null){
            Date yesterdayBeginDate = DateUtils.dateAddDays((Date)periodVOresult.get("begindate"), -1);
            if(initDataVo.getAccountdate().compareTo(yesterdayBeginDate)<0){
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100025", "账户期初日期不能小于等于资金组织现金管理的期初前一天。") /* "账户期初日期不能小于等于资金组织现金管理的期初前一天。" */);//@notranslate
            }
        }
        //2不能大于等于该资金组织+账户生成日记账的单据日期、登账日期
        CtmJSONObject params = new CtmJSONObject();
        QueryConditionGroup conditionJournal = new QueryConditionGroup();
        if (isBank) {
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ACCENTITY).eq(initDataVo.getAccentity())));
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.BANKACCOUNT).eq(initDataVo.getBankaccount())));
            params.put("isBank","true");
            params.put(ICmpConstant.ACCOUNT,initDataVo.getBankaccount());
        } else {
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.CASH_ACCOUNT_LOWER).eq(initDataVo.getCashaccount())));
            params.put("isBank","false");
            params.put(ICmpConstant.ACCOUNT,initDataVo.getCashaccount());
        }
        QuerySchema querySchemaSum = QuerySchema.create().addSelect(ICmpConstant.PRIMARY_ID,ICmpConstant.VOUCHDATE);
        querySchemaSum.addCondition(conditionJournal);
        querySchemaSum.addOrderBy(ICmpConstant.VOUCHDATE);
        querySchemaSum.setLimitCount(ICmpConstant.CONSTANT_ONE);
        List<Map<String, Object>> journalList =  MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaSum);
        if(CollectionUtils.isNotEmpty(journalList)){
            if(initDataVo.getAccountdate().compareTo((Date)journalList.get(0).get(ICmpConstant.VOUCHDATE))>0){
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20D45ACC04100023", "账户期初日期不能大于业务单据占账的单据日期。") /* "账户期初日期不能大于业务单据占账的单据日期。" */);//@notranslate
            }

        }
        //3期初日期之后已经存在日结的日期，则不能修改
        params.put(ICmpConstant.CURRENCY,initDataVo.getCurrency());
        params.put(ICmpConstant.ACCENTITY,initDataVo.getAccentity());
        params.put("accountdate",initDataVo.getAccountdate());
        checkSettleflag(params);
    }

    private void upgradeHisInitData() throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        List<EnterpriseBankAcctVOWithRange> accountDataList = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
        if(CollectionUtils.isEmpty(accountDataList)){//当前租户没有账户信息
            return;
        }
        //构建key为账户id，value为使用组织的Map
        Map<String,List<String>> accIdOrgsMap = new HashMap<>();
        //构建key为账户id，value为所属组织 的Map
        Map<String,String> accIdParentOrgMap = new HashMap<>();

        for(EnterpriseBankAcctVOWithRange acctVo :accountDataList){
            List<String> orgList = new ArrayList<>();
            if(acctVo.getAccountApplyRange()!=null){
                acctVo.getAccountApplyRange().stream().forEach(e -> {
                    orgList.add(e.getRangeOrgId());
                });
                accIdOrgsMap.put(acctVo.getId(),orgList);
                accIdParentOrgMap.put(acctVo.getId(),acctVo.getOrgid());
            }else{
                log.error(acctVo.getAccount()+"账户accountApplyRange为空");
            }
        }
        //查询借贷发生额 按使用组织 + 账号 + 币种分组
        Map<String,BigDecimal> balanceMap = getBalanceForUpgrade();
//        Map<String,BigDecimal> balanceRealityMap = getRealityBalanceForUpgrade(); 实占余额先不算
        //查询全部旧有期初数据
        QuerySchema querySchema_initdata = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition_initdata = new QueryConditionGroup(ConditionOperator.and);
        condition_initdata.appendCondition(QueryConditionGroup.and(QueryCondition.name("upgradesign").is_null()));
        condition_initdata.appendCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").is_not_null()));
        querySchema_initdata.addCondition(condition_initdata);

        QuerySchema detailSchema = QuerySchema.create().name("InitDatab").addSelect("*");
        querySchema_initdata.addCompositionSchema(detailSchema);
        List<InitData> initDataVoList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchema_initdata,null);
        if(CollectionUtils.isEmpty(initDataVoList)){//说明数据全部升级过
            return;
        }
        //待更新的期初数据集合
        List<InitData> updateInitDataList = new ArrayList<>();
        //待插入的期初数据集合(来源为期初子表)
        List<InitData> insertInitDataList = new ArrayList<>();
        //TODO 线程处理  同时加锁 线程内部更新
        for(InitData initDataVo : initDataVoList){
            //方向为贷 改为负数
            if(initDataVo.getDirection().getValue() == Direction.Credit.getValue() &&
                    initDataVo.getCoinitloribalance()!=null && initDataVo.getCoinitlocalbalance()!=null && initDataVo.getCoinitloribalance().compareTo(BigDecimal.ZERO)>0){
                initDataVo.setCoinitloribalance(initDataVo.getCoinitloribalance().negate());
                initDataVo.setCoinitlocalbalance(initDataVo.getCoinitlocalbalance().negate());
            }
            if(initDataVo.getBankdirection().getValue() == Direction.Credit.getValue() &&
                    initDataVo.getBankinitoribalance()!=null && initDataVo.getBankinitlocalbalance()!=null && initDataVo.getBankinitoribalance().compareTo(BigDecimal.ZERO)>0){
                initDataVo.setBankinitoribalance(initDataVo.getBankinitoribalance().negate());
                initDataVo.setBankinitlocalbalance(initDataVo.getBankinitlocalbalance().negate());
            }

            //看是否有子表
            if(initDataVo.InitDatab()!=null){//有子表
                //计算子表期初合计
                BigDecimal initDatabSum = BigDecimal.ZERO;
                for(InitDatab initDatab : initDataVo.InitDatab()){
                    //根据子表借贷方向 计算实际总金额
                    if(initDatab.getDirection().getValue() == Direction.Credit.getValue() &&
                            initDatab.getCoinitloribalance()!=null && initDatab.getCoinitlocalbalance()!=null && initDatab.getCoinitloribalance().compareTo(BigDecimal.ZERO)>0){
                        initDatab.setCoinitloribalance(initDatab.getCoinitloribalance().negate());
                        initDatab.setCoinitlocalbalance(initDatab.getCoinitlocalbalance().negate());
                    }
                    if(initDatab.getBankdirection().getValue() == Direction.Credit.getValue() &&
                            initDatab.getBankinitoribalance()!=null && initDatab.getBankinitlocalbalance()!=null && initDatab.getBankinitoribalance().compareTo(BigDecimal.ZERO)>0){
                        initDatab.setBankinitoribalance(initDatab.getBankinitoribalance().negate());
                        initDatab.setBankinitlocalbalance(initDatab.getBankinitlocalbalance().negate());
                    }

                    initDatabSum = initDatabSum.add(initDatab.getCoinitloribalance());
                }
                //组装子表数据
                for(InitDatab initDatab : initDataVo.InitDatab()){
                    InitData insertData= new InitData();
                    buildInitDataHeadForUpgrade(initDatab, initDataVo, insertData,initDatabSum,balanceMap);

                    insertInitDataList.add(insertData);
                }
                //处理主表所属组织
                initDataVo.setParentAccentity(initDataVo.getAccentity());
                initDataVo.setAccentity(null);
            } else{//没有子表
                //原数据没有子表的数据，如果主表有使用权，直接升级【*标识=升级上来已确认数据】
                if(initDataVo.getBankaccount()!=null //是银行账户
                        && accIdOrgsMap.get(initDataVo.getBankaccount())!= null //账户id-orgs集合中有当前账户信息
                        && accIdOrgsMap.get(initDataVo.getBankaccount()).contains(initDataVo.getAccentity())){//账户id-orgs集合中有当前账户信息 且使用组织符合要求
                    /**
                     * 余额重算预占
                     * 按使用组织计算预占 和 实占（如果当前主表的会计主体 是当前账户的在授权使用组织 才进行重算）
                     */
                    setInitBalance(initDataVo,balanceMap);
                    //所属组织字段赋值
                    initDataVo.setParentAccentity(accIdParentOrgMap.get(initDataVo.getBankaccount()));
                    //升级标志 已确认数据
                    initDataVo.setUpgradesign(UpgradeSignEnum.JUDGMENT.getValue());
                }else{
                    //所属组织字段赋值
                    initDataVo.setParentAccentity(accIdParentOrgMap.get(initDataVo.getBankaccount()));
                    //使用组织字段置空
                    initDataVo.setAccentity(null);
                    //升级标志 升级时未能自动判断
                    initDataVo.setUpgradesign(UpgradeSignEnum.CANTJUDGMENT.getValue());
                }
            }

            initDataVo.setEntityStatus(EntityStatus.Update);
            updateInitDataList.add(initDataVo);
        }
        if(!CollectionUtils.isEmpty(updateInitDataList)){
            MetaDaoHelper.update(InitData.ENTITY_NAME,updateInitDataList);
        }
        if(!CollectionUtils.isEmpty(insertInitDataList)){
            CmpMetaDaoHelper.insert(InitData.ENTITY_NAME,insertInitDataList);
        }
    }


    private void buildInitDataHeadForUpgrade(InitDatab initBody,InitData initDataold, InitData insertData,BigDecimal initDatabSum ,Map<String,BigDecimal> balanceMap){
        //源于主表字段赋值
        insertData.setParentAccentity(initDataold.getAccentity());
        insertData.setAccountdate(initDataold.getAccountdate());
        insertData.setNatCurrency(initDataold.getNatCurrency());
        insertData.setMoneyform(initDataold.getMoneyform());
        insertData.setBankaccount(initDataold.getBankaccount());
        insertData.setBankaccountno(initDataold.getBankaccountno());
        insertData.setCurrency(initDataold.getCurrency());
        insertData.setExchangerate(initDataold.getExchangerate());
        insertData.setExchangeRateType(initDataold.getExchangeRateType());

        //源于子表字段赋值
        insertData.setAccentity(initBody.getAccentity());

        insertData.setBankinitlocalbalance(initBody.getBankinitlocalbalance());
        insertData.setBankinitoribalance(initBody.getBankinitoribalance());

        insertData.setCoinitloribalance(initBody.getCoinitloribalance());
        //如果主表存在汇率 会相乘赋值
        if(initDataold.getExchangerate()!=null){
            insertData.setCoinitlocalbalance((initBody.getCoinitloribalance().multiply(initDataold.getExchangerate())).setScale(8,BigDecimal.ROUND_HALF_UP));
        }else{
            //如果主表不存在汇率 直接取子表赋值
            insertData.setCoinitlocalbalance(initBody.getCoinitlocalbalance());
        }
        insertData.setId(ymsOidGenerator.nextId());
        insertData.setQzbz(true);
        //余额重算逻辑
        setInitBalance(insertData,balanceMap);

        if(initDatabSum.compareTo(initDataold.getCoinitloribalance()) == 0){
            initDataold.setUpgradesign(UpgradeSignEnum.SUPERFLUITY.getValue());
            insertData.setUpgradesign(UpgradeSignEnum.JUDGMENT.getValue());
        }else
            initDataold.setUpgradesign(UpgradeSignEnum.CANTJUDGMENT.getValue());
        insertData.setUpgradesign(UpgradeSignEnum.JUDGMENT.getValue());


        initDataold.setEntityStatus(EntityStatus.Update);
        insertData.setEntityStatus(EntityStatus.Insert);
    }

    /**
     * 预占金额
     * @return
     * @throws Exception
     */
    private Map<String,BigDecimal> getBalanceForUpgrade() throws Exception {
        Map<String,BigDecimal> balanceMap = new HashMap<>();
        //查询全部账户 按使用组织 + 账号 + 币种分组
        QueryConditionGroup conditionJournal = getQueryConditionGroup(false);
        QuerySchema querySchemaSum = QuerySchema.create().addSelect("sum(debitnatSum) as debitnatSum,sum(debitoriSum) as" +
                " debitoriSum,sum(creditnatSum) as creditnatSum,sum(creditoriSum) as creditoriSum,bankaccount,currency,accentity");
        querySchemaSum.addCondition(conditionJournal);
        querySchemaSum.addGroupBy("bankaccount,currency,accentity");
        //查询所有相关账户的 预占发生额
        List<Map<String, Object>> journalSumList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaSum);
        if(!CollectionUtils.isEmpty(journalSumList)){
            for(Map<String, Object> map : journalSumList){
                //本币发生额
                BigDecimal debitnatSum = map.get("debitnatSum")!=null?new BigDecimal(map.get("debitnatSum").toString()):BigDecimal.ZERO;
                BigDecimal debitoriSum = map.get("debitoriSum")!=null?new BigDecimal(map.get("debitoriSum").toString()):BigDecimal.ZERO;
                BigDecimal creditnatSum = map.get("creditnatSum")!=null?new BigDecimal(map.get("creditnatSum").toString()):BigDecimal.ZERO;
                BigDecimal creditoriSum = map.get("creditoriSum")!=null?new BigDecimal(map.get("creditoriSum").toString()):BigDecimal.ZERO;
                balanceMap.put(map.get("accentity").toString()+map.get("bankaccount").toString()+map.get("currency").toString()+"nat", debitnatSum.subtract(creditnatSum));
                //原币发生额
                balanceMap.put(map.get("accentity").toString()+map.get("bankaccount").toString()+map.get("currency").toString()+"ori", debitoriSum.subtract(creditoriSum));
            }
        }
        if(!balanceMap.isEmpty()){
            return balanceMap;
        }
        return null;
    }

    /**
     * 实占金额
     * @return
     * @throws Exception
     */
    private Map<String,BigDecimal> getRealityBalanceForUpgrade() throws Exception {
        Map<String,BigDecimal> balanceMap = new HashMap<>();
        //查询全部账户 按使用组织 + 账号 + 币种分组
        QueryConditionGroup conditionJournalReality = getQueryConditionGroup(true);
        QuerySchema querySchemaSum = QuerySchema.create().addSelect("sum(debitnatSum) as debitnatSum,sum(debitoriSum) as debitoriSum,sum(creditnatSum) as creditnatSum,sum(creditoriSum) as creditoriSum,bankaccount,currency,accentity");
        querySchemaSum.addCondition(conditionJournalReality);
        querySchemaSum.addGroupBy("bankaccount,currency,accentity");
        //查询所有相关账户的 实占发生额
        List<Map<String, Object>> journalSumListReality = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaSum);
        if(!CollectionUtils.isEmpty(journalSumListReality)){
            for(Map<String, Object> map : journalSumListReality){
                //本币发生额
                BigDecimal debitnatSum = new BigDecimal(map.get("debitnatSum").toString());
                BigDecimal debitoriSum = new BigDecimal(map.get("debitoriSum").toString());
                BigDecimal creditnatSum = new BigDecimal(map.get("creditnatSum").toString());
                BigDecimal creditoriSum = new BigDecimal(map.get("creditoriSum").toString());
                balanceMap.put(map.get("accentity").toString()+map.get("bankaccount").toString()+map.get("currency").toString()+"natReal", debitnatSum.subtract(creditnatSum));
                //原币发生额
                balanceMap.put(map.get("accentity").toString()+map.get("bankaccount").toString()+map.get("currency").toString()+"oriReal", debitoriSum.subtract(creditoriSum));
            }
        }
        if(!balanceMap.isEmpty()){
            return balanceMap;
        }
        return null;
    }

    private QueryConditionGroup getQueryConditionGroup(Boolean isSettled) throws Exception {
        QueryConditionGroup conditionJournal = new QueryConditionGroup();
        conditionJournal.addCondition(QueryCondition.name("initflag").eq(0));
        conditionJournal.addCondition(QueryCondition.name("bankaccount").is_not_null());
        if(isSettled){
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("settlestatus").eq(2)));
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_not_null()));
        }
        return conditionJournal;
    }

    //余额重算赋值(预占)
    private void setInitBalance(InitData initDataVo, Map<String,BigDecimal> balanceMap){
        if(balanceMap!=null){
            String commonKey = initDataVo.getAccentity()+initDataVo.getBankaccount()+initDataVo.getCurrency();
            initDataVo.setCobooklocalbalance(initDataVo.getCoinitlocalbalance().add(balanceMap.get(commonKey+"nat")!=null?balanceMap.get(commonKey+"nat"):BigDecimal.ZERO));//本币
            initDataVo.setCobookoribalance(initDataVo.getCoinitloribalance().add(balanceMap.get(commonKey+"ori")!=null?balanceMap.get(commonKey+"ori"):BigDecimal.ZERO));//原币
        }
    }

    /**
     * 初始化现金账户的期初日期
     *
     * @param periodMap
     * @param orgId
     * @param initData
     * @param enterpriseCashMap
     */
    @Override
    public void initCashAccountDate(Map<String, Period> periodMap, String orgId, InitData initData, Map<String, Object> enterpriseCashMap) {
        if (periodMap.get(orgId) == null) {
            return;
        }
        if (enterpriseCashMap == null) {
            return;
        }
        Date accountOpenDate = enterpriseCashMap.get("accountOpenDate") == null ? null : (Date) enterpriseCashMap.get("accountOpenDate") ;
        initData.setPeriod((Long) periodMap.get(orgId).getId());
        Date beginDate = periodMap.get(orgId).getBegindate();
        Date yesterdayBeginDate = DateUtils.dateAddDays(beginDate, -1);
        if (accountOpenDate == null) {
            // 账户开户日期没有值, 授权主体现金管理期初日期前一天
            initData.setAccountdate(yesterdayBeginDate);
        } else {
            if (accountOpenDate.compareTo(yesterdayBeginDate) >= 0) {
                // 资金组织现金管理期初日期小于等于账户开户日期 :账户开户日期
                initData.setAccountdate(accountOpenDate);
            } else {
                // 资金组织现金管理期初日期大于账户开户日期 :资金组织现金管理期初开始日期前一天
                initData.setAccountdate(yesterdayBeginDate);
            }
        }
    }
}
