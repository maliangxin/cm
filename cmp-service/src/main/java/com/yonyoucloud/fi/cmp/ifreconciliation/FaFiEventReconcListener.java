package com.yonyoucloud.fi.cmp.ifreconciliation;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.CurrencyClassification;
import com.yonyoucloud.fi.cmp.cmpentity.DirectionJD;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.UpgradeSignEnum;
import com.yonyoucloud.fi.cmp.event.constant.IEventCenterConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.rpc.rule.DailyComputezInit;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.SendEventMessageUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * description: FaFiEventReconcListener <br> 用于监听财务对账获取数据
 * date: 2022/11/20 16:20 <br>
 * author: yangjn <br>
 * version: 1.0 <br>
 */
@Slf4j
@Component("cmpFiEventReconcListener")
public class FaFiEventReconcListener implements IEventReceiveService {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    IFReconciliationService reconciliationService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    /**
     * 对账维度
     */
    // 银行账户
    private String BANK_ACCOUNT = "cmp_journal.bankaccount";
    // 现金账户
    private String CASH_ACCOUNT = "cmp_journal.cashaccount";

    private static String FAFIEVENTRECONCMAPPER = "com.yonyoucloud.fi.cmp.mapper.FaFiEventReconcMapper.";
    //注入sql关键字过滤
    private static String INJECTION[] = {"select", "update", "delete", "drop", "insert", "show", "desc", "alert", "rename"};

    //原币
    private static String HOME_CURRENCY = "home_currency";
    //本币
    private static String ORI_CURRENCY = "ori_currency";


    @SneakyThrows
    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws BusinessException {
        BizObject responseMsg = new BizObject();
        String userObjectStr = businessEvent.getUserObject();
        log.error("财务对账消息{}", userObjectStr);
        CtmJSONObject faFiInfo = CtmJSONObject.parseObject(userObjectStr);
        //任务ID`
        String taskid = faFiInfo.getString("taskid");
        //对账规则ID
        String reconcrule_id = faFiInfo.getString("reconcrule_id");
        String oriCurrencyType = faFiInfo.getString("ori_currtype_id");
        //事件类型
        String eventType = "fievarc-exec-cm";
        try {
            // 构建事件查询参数
            FReconciliation fReconciliation = bulidFiInfoParams(faFiInfo);
            // 1.对账规则中设置了账户，查指定账户；2.未指定账户，查询所有账户(用期初和发生额来获取全部发生额)
            Map<String, Object> accountMap = reconciliationService.getAccount(faFiInfo.getString("busi_queryschema"), fReconciliation.getProjectCode(),fReconciliation.getAccentity_id());
            List<String> accountList = (ArrayList<String>) accountMap.get("accountList");
            //如果没有对账账户，则直接返回
            if(CollectionUtils.isEmpty(accountList)){
                //组装成功信息
                responseMsg.put("taskid", taskid);
                responseMsg.put("reconcRuleId", reconcrule_id);
                responseMsg.put("eventType", eventType);
                responseMsg.put("status", 1);
                //发送信息到对应事件中心
                SendEventMessageUtils.sendEventMessageEos(responseMsg, IEventCenterConstant.RECONC_RECEIVE, IEventCenterConstant.RECONC_SUMMARY_RETURN);
                log.error("项目编码projectCode:{}财务对账账户的数据为空", fReconciliation.getProjectCode());
                return JsonUtils.toJsonString(responseMsg);
            }
            //有对账账户，走后面的对账逻辑
            //根据条件查询对应汇总数据
            Map param = getFaFiEventReconcMapperParam(fReconciliation);
            log.error("查询到的汇总数据参数param{}", param);
            List<Journal> listJournalfund = SqlHelper.selectList(FAFIEVENTRECONCMAPPER + "getJournalListSum", param);
            log.error("查询到的汇总数据listJournalfund{}", listJournalfund);
            // 查询发生额
            String key;
            HashMap<String, Journal> journalMap = new HashMap<>();
            ArrayList<String> currencyList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(listJournalfund)) {
                for (Journal journal : listJournalfund) {
                    key = fReconciliation.getAccentity_id() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                    key = key.replace("null", "");
                    Journal journalData = buildJournal(journal);
                    log.error("journalData数据" + CtmJSONObject.toJSONString(journalData));
                    journalMap.put(key, journalData);
                    currencyList.add(journal.getCurrency());
                }
            }
            // 获取币种
            Set<String> currencys = getCurrencys(fReconciliation, accountList,oriCurrencyType);
            // 当没有发生额时查询期初币种
            QuerySchema schema = QuerySchema.create().addSelect("currency");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("accentityRaw").eq(fReconciliation.getAccentity_id()));
            conditionGroup.appendCondition(QueryCondition.name("upgradesign").in(UpgradeSignEnum.JUDGMENT.getValue(),UpgradeSignEnum.ADDNEW.getValue()));
            if (ICmpConstant.BANKACCOUNT.equals(accountMap.get(ICmpConstant.ACCOUNTTYPE))) {
                conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(accountList));
            }
            if (ICmpConstant.CASH_ACCOUNT_LOWER.equals(accountMap.get(ICmpConstant.ACCOUNTTYPE))) {
                conditionGroup.appendCondition(QueryCondition.name("cashaccount").in(accountList));
            }
            conditionGroup.appendCondition(QueryCondition.name("currency").in(new ArrayList<>(currencys)));
            schema.addCondition(conditionGroup);
            schema.addGroupBy("currency");
            List<InitData> initList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, schema, null);
            log.error("查询期初数据initList{}", initList);
            List<String> initCurrency = null;
            if (CollectionUtils.isNotEmpty(initList)) {
                initCurrency = initList.stream().map(InitData::getCurrency).collect(Collectors.toList());
            }
            List<String> currencyCollections;
            if (CollectionUtils.isNotEmpty(currencyList) && CollectionUtils.isNotEmpty(initCurrency)) {
                currencyCollections = Stream.concat(currencyList.stream(), initCurrency.stream()).distinct().collect(Collectors.toList());
            }else if (CollectionUtils.isNotEmpty(currencyList) && CollectionUtils.isEmpty(initCurrency)) {
                currencyCollections = currencyList;
            } else {
                currencyCollections = initCurrency;
            }
            // 期初期末余额
            Map<String, Map<String, SettlementDetail>> stringMapMap = queryBalance((ArrayList<String>) currencyCollections, fReconciliation, accountMap);
            Map<String, SettlementDetail> settlementInitMap = stringMapMap.get("settlementInitMap");
            Map<String, SettlementDetail> settlementEndMap = stringMapMap.get("settlementEndMap");
            log.error("settlementInitMap{}", settlementInitMap);
            log.error("settlementEndMap{}", settlementEndMap);
            SettlementDetail settlementInit = null;
            SettlementDetail settlementEnd = null;
            for (String account : accountList) {
                if (CollectionUtils.isNotEmpty(currencyCollections)) {
                    List<String> currencyCollection = currencyCollections.stream().distinct().collect(Collectors.toList());
                    for (String currency : currencyCollection) {
                        key = fReconciliation.getAccentity_id() + account + currency;
                        key = key.replace("null", "");
                        log.error("settlementInitMap-key:" + key);
                        if (Objects.nonNull(settlementInitMap)) {
                            settlementInit = settlementInitMap.get(key);
                        }
                        if (Objects.nonNull(settlementEndMap)) {
                            settlementEnd = settlementEndMap.get(key);
                        }
                        // 查询期初期末余额
                        queryInitData(fReconciliation,settlementInit,settlementEnd);
                        Journal journal = journalMap.get(key);
                        // 查询发生额
                        queryAmount(fReconciliation,journal);
                        // 与总账定的币种方案
                        if ((fReconciliation.getCurrencyClassification().getValue() != CurrencyClassification.Home_Currency.getValue()) && Objects.nonNull(journal)) {
                            fReconciliation.setCurrency(journal.getCurrency());
                        }
                        if (fReconciliation.getQueryBalance() && Objects.nonNull(settlementInit)) {
                            fReconciliation.setCurrency(settlementInit.getCurrency());
                        } else {
                            fReconciliation.setCurrency(journal!=null?journal.getCurrency():currency);
                        }
                        Map paramSum = getFaFiEventReconcMapperParam(fReconciliation);
                        CtmJSONObject defines = faFiInfo.getJSONObject("colnum_define");
                        if (defines != null) {
                            if (defines.keySet().contains(BANK_ACCOUNT)) {
                                paramSum.put("define1", account);
                            } else if (defines.keySet().contains(CASH_ACCOUNT)) {
                                paramSum.put("define1", account);
                            }
                        }
                    if ((fReconciliation.getQueryBalance() && Objects.nonNull(settlementEnd)) || !fReconciliation.getQueryBalance()
                            || (Objects.isNull(settlementInit)) && Objects.nonNull(journal)) {
                        log.error("待插入总账数据库的参数insertFaFiEventReconc{}", paramSum);
                        // 对账规则中未勾选原币(原币和原币本币类型)不能传原币
                        if (fReconciliation.getCurrencyClassification().getValue() != CurrencyClassification.Ori_Currency.getValue()
                                && fReconciliation.getCurrencyClassification().getValue() != CurrencyClassification.OriAndHome.getValue()) {
                            paramSum.put("currency", null);
                        }
                        //插入给定的对账表中
                        SqlHelper.insert(FAFIEVENTRECONCMAPPER + "insertFaFiEventReconc", paramSum);
                    }
                    }
                }
            }
            //组装成功信息
            responseMsg.put("taskid", taskid);
            responseMsg.put("reconcRuleId", reconcrule_id);
            responseMsg.put("eventType", eventType);
            responseMsg.put("status", 1);
            //发送信息到对应事件中心
            SendEventMessageUtils.sendEventMessageEos(responseMsg, IEventCenterConstant.RECONC_RECEIVE, IEventCenterConstant.RECONC_SUMMARY_RETURN);
        } catch (Exception e) {
            log.error("处理财务对账消息出错:", e);
            //如果对账出现错误 与平台沟通 需要发送错误信息 防止对账不结束
            responseMsg.put("taskid", taskid);
            responseMsg.put("reconcRuleId", reconcrule_id);
            responseMsg.put("eventType", eventType);
            responseMsg.put("status", 0);//非1为失败
            responseMsg.put("errormsg", e.getMessage());
            //发送信息到对应事件中心
            SendEventMessageUtils.sendEventMessageEos(responseMsg, IEventCenterConstant.RECONC_RECEIVE, IEventCenterConstant.RECONC_SUMMARY_RETURN);
            return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
        }

        return JsonUtils.toJsonString(responseMsg);
    }

    /**
     * 获取币种  总账传入币种以总账为准，未传入币种获取账户对应的币种
     * @param fReconciliation
     * @param accountList
     * @return
     */
    private Set<String> getCurrencys(FReconciliation fReconciliation, List<String> accountList,String oriCurrencyType) throws Exception {
        Set<String> currencys = new HashSet<>();
        if (StringUtils.isEmpty(oriCurrencyType)) {
            currencys = getCurrencyListByAccount(accountList, fReconciliation.getProjectCode());
        } else {
            currencys.add(oriCurrencyType);
        }
        return currencys;
    }

    /**
     * 根据账户查询币种
     * @param accountList 账户集合
     * @param projectCode 项目编码
     * @return currencyResult 币种集合
     * @throws Exception
     */
    private Set<String> getCurrencyListByAccount(List<String> accountList, String projectCode) throws Exception {
        Set<String> currencyResult = new HashSet<>();
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setPageSize(4999);
        enterpriseParams.setIdList(accountList);
        enterpriseParams.setEnables(Arrays.asList(1, 2));
        // 再根据账号查币种
        // 银行对账
        if (ICmpConstant.BANKPROJECTCODE.equals(projectCode)) {
            List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = new ArrayList<>();
            int times = (int) Math.ceil((double) accountList.size() / 500);
            for (int i = 1; i <= times; i++) {
                EnterpriseParams enterprisePartsParams = new EnterpriseParams();
                enterprisePartsParams.setPageSize(4999);
                enterprisePartsParams.setEnables(Arrays.asList(1, 2));
                if (i * 500 >= accountList.size()) {
                    List<String> sublist = accountList.subList((i - 1) * 500 , accountList.size());
                    enterprisePartsParams.setIdList(sublist);
                    List<EnterpriseBankAcctVO> enterpriseBankAcctPartsVOS = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterprisePartsParams);
                    enterpriseBankAcctVOS.addAll(enterpriseBankAcctPartsVOS);
                } else {
                    List<String> sublist = accountList.subList((i - 1) * 500, i * 500);
                    enterprisePartsParams.setIdList(sublist);
                    List<EnterpriseBankAcctVO> enterpriseBankAcctPartsVOS = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterprisePartsParams);
                    enterpriseBankAcctVOS.addAll(enterpriseBankAcctPartsVOS);
                }
            }
            if (CollectionUtils.isEmpty(enterpriseBankAcctVOS)) {
                log.error("银行对账-getCurrencyListByAccount,param:{} result error", CtmJSONObject.toJSONString(accountList));
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006EB", "银行对账-根据账户查询币种异常，请检查！") /* "银行对账-根据账户查询币种异常，请检查！" */);
            }

            for (EnterpriseBankAcctVO account : enterpriseBankAcctVOS) {
                currencyResult.addAll(account.getCurrencyList().stream().map(BankAcctCurrencyVO::getCurrency).collect(Collectors.toList()));
            }
        } else {
            //现金对账
            List<EnterpriseCashVO> cashAcctVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams);
            int times = (int) Math.ceil((double) accountList.size() / 500);
            for (int i = 1; i <= times; i++) {
                EnterpriseParams enterprisePartsParams = new EnterpriseParams();
                enterprisePartsParams.setPageSize(4999);
                enterprisePartsParams.setEnables(Arrays.asList(1, 2));
                if (i * 500 >= accountList.size()) {
                    List<String> sublist = accountList.subList((i - 1) * 500 , accountList.size()-1);
                    enterprisePartsParams.setIdList(sublist);
                    List<EnterpriseCashVO> enterpriseBankAcctPartsVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterprisePartsParams);
                    cashAcctVOS.addAll(enterpriseBankAcctPartsVOS);
                } else {
                    List<String> sublist = accountList.subList((i - 1) * 500, i * 500 - 1);
                    enterprisePartsParams.setIdList(sublist);
                    List<EnterpriseCashVO> enterpriseBankAcctPartsVOS = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterprisePartsParams);
                    cashAcctVOS.addAll(enterpriseBankAcctPartsVOS);
                }
            }
            if (CollectionUtils.isEmpty(cashAcctVOS)) {
                log.error("现金对账-getCurrencyListByAccount,param:{} result error", CtmJSONObject.toJSONString(accountList));
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006EA", "现金对账-根据账户查询币种异常，请检查！") /* "现金对账-根据账户查询币种异常，请检查！" */);
            }

            for (EnterpriseCashVO account : cashAcctVOS) {
                currencyResult.add(account.getCurrency());
            }
        }

        return currencyResult;
    }

    /**
     * 查询期初期末
     * @param currencyList
     * @param fReconciliation
     * @param accountMap
     * @return
     * @throws Exception
     */
    public Map<String, Map<String, SettlementDetail>> queryBalance(ArrayList<String> currencyList, FReconciliation fReconciliation, Map<String, Object> accountMap) throws Exception {
        ArrayList<String> bankAccountId = null;
        ArrayList<String> cashAccountId = null;
        String accountType = (String) accountMap.get(ICmpConstant.ACCOUNTTYPE);
        if (accountType.equals(ICmpConstant.BANK_ACCOUNT)) {
            bankAccountId = (ArrayList<String>) accountMap.get("accountList");
        } else {
            cashAccountId = (ArrayList<String>) accountMap.get("accountList");
        }
        Date startDate = DateUtils.preDay(DateUtils.parseDate(fReconciliation.getPeriod_start_date()));
        Date endDate = DateUtils.parseDate(fReconciliation.getPeriod_end_date());
        //期初金额
        Map<String, SettlementDetail> settlementInitMap =
                DailyComputezInit.imitateDailyComputeInitNew(fReconciliation.getAccentity_id(), currencyList, cashAccountId, bankAccountId, "2", "2", startDate);
        //期末余额
        Map<String, SettlementDetail> settlementEndMap =
                DailyComputezInit.imitateDailyComputeInitNew(fReconciliation.getAccentity_id(), currencyList, cashAccountId, bankAccountId, "2", "2", endDate);

        Map<String, Map<String, SettlementDetail>> periodMap = new HashMap<>();
        periodMap.put("settlementInitMap", settlementInitMap);
        periodMap.put("settlementEndMap", settlementEndMap);
        return periodMap;
    }

    public Map getFaFiEventReconcMapperParam(FReconciliation fReconciliation) {
        Map param = new HashMap();
        param.put("id", ymsOidGenerator.nextId());
        param.put("busi_queryschema", fReconciliation.getBusi_queryschema());
        param.put("fixed_cond", fReconciliation.getFixed_cond());
        param.put("dbschema_table", fReconciliation.getRecon_dbschema() + "." + fReconciliation.getRecon_table());
        param.put("currency", fReconciliation.getCurrency());
        param.put("debitnatSum", fReconciliation.getDebitnatSum());
        param.put("creditoriSum", fReconciliation.getCreditoriSum());
        param.put("debitoriSum", fReconciliation.getDebitoriSum());
        param.put("creditnatSum", fReconciliation.getCreditnatSum());
        param.put("reconcrule_id", fReconciliation.getReconcrule_id());
        param.put("reconcplan_id", fReconciliation.getReconcplan_id());
        param.put("task_id", fReconciliation.getTask_id());
        param.put("ytenant_id", fReconciliation.getYtenant_id());
        param.put("period_id", fReconciliation.getPeriod_id());
        param.put("accbook_id", fReconciliation.getAccbook_id());
        param.put("accentity_id", fReconciliation.getAccentity_id());
        param.put("period_start_date", fReconciliation.getPeriod_start_date());
        param.put("period_end_date", fReconciliation.getPeriod_end_date());
        param.put("subject_chart_id", fReconciliation.getSubject_chart_id());
        param.put("subject_id", fReconciliation.getSubject_id());
        // 期初相关字段
        param.put("period_begin_ori_amount", fReconciliation.getPeriod_begin_ori_amount());
        param.put("period_begin_amount", fReconciliation.getPeriod_begin_amount());
        param.put("period_end_ori_amount", fReconciliation.getPeriod_end_ori_amount());
        param.put("period_end_amount", fReconciliation.getPeriod_end_amount());
        param.put("project_code", fReconciliation.getProjectCode());
        return param;
    }

    /**
     * 判断语句中是否有注入关键字
     *
     * @param injections
     * @param busi_query
     * @return
     */
    private Boolean determineInjection(String[] injections, String busi_query) {
        for (String injection : Arrays.asList(injections)) {
            if (busi_query.contains(injection)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建事件查询参数
     * @param faFiInfo
     * @return
     */
    private FReconciliation bulidFiInfoParams(CtmJSONObject faFiInfo) throws Exception {
        //ytenant_id
        String ytenant_id = faFiInfo.getString("ytenant_id");
        //对账规则选择的科目
        String subject_id = faFiInfo.getString("subject_ids");
        subject_id = subject_id.replace("[", "").replace("]", "").replace("\"", "").replace(" ", "");
        //对账原币币种
        String ori_currtype_id = faFiInfo.getString("ori_currtype_id");
        FReconciliation fReconciliation = new FReconciliation();
        //区分home_currency本币 、 ori_currency原币
        List<String> reconc_content = faFiInfo.getObject("reconc_content", List.class);
        if (reconc_content.size() > 1 && reconc_content.contains(ORI_CURRENCY)) {
            //给ori_currtype_id 字段赋值
            fReconciliation.setCurrencyClassification(CurrencyClassification.OriAndHome);
        } else if (reconc_content.size() == 1 && reconc_content.contains(ORI_CURRENCY)) {
            //给ori_currtype_id 字段赋值
            fReconciliation.setCurrencyClassification(CurrencyClassification.Ori_Currency);
        } else if (reconc_content.size() == 1 && reconc_content.contains(HOME_CURRENCY)) {
            //ori_currtype_id不赋值 为null
            fReconciliation.setCurrencyClassification(CurrencyClassification.Home_Currency);
            if(ori_currtype_id == null){
                //查询当前会计主体本币
                ori_currtype_id = AccentityUtil.getNatCurrencyIdByAccentityId(faFiInfo.getString("accentity_id"));
            }
        }
        //区分 debit借、 credit贷
        List<String> reconc_type = faFiInfo.getObject("reconc_type", List.class);
        if (reconc_type.contains("debit") && !reconc_type.contains("credit")) {//借
            fReconciliation.setDirectionJD(DirectionJD.Debit);
        } else if (reconc_type.contains("credit") && !reconc_type.contains("debit")) {//贷
            fReconciliation.setDirectionJD(DirectionJD.Credit);
        } else if (reconc_type.contains("debit") && reconc_type.contains("credit")) {//借贷都有
            fReconciliation.setDirectionJD(DirectionJD.CreditAndDebit);
        }
        //判断是否查询余额
        if (reconc_type.contains("balance")) {
            fReconciliation.setQueryBalance(true);
        } else {
            fReconciliation.setQueryBalance(false);
        }
        fReconciliation.setYtenant_id(faFiInfo.getString("ytenant_id"));
        fReconciliation.setTask_id(faFiInfo.getString("taskid"));
        fReconciliation.setRecon_dbschema(faFiInfo.getString("recon_dbschema"));
        fReconciliation.setRecon_table(faFiInfo.getString("recon_table"));
        String busi_query = faFiInfo.getString("busi_queryschema");
        //语句中是否有注入关键字
        if (!determineInjection(INJECTION, busi_query)) {
            fReconciliation.setBusi_queryschema(busi_query);
        }
        fReconciliation.setFixed_cond(faFiInfo.getString("fixed_cond"));
        fReconciliation.setReconcrule_id(faFiInfo.getString("reconcrule_id"));
        fReconciliation.setReconcplan_id(faFiInfo.getString("reconcplan_id"));
        fReconciliation.setPeriod_id(faFiInfo.getString("period_id"));
        fReconciliation.setAccbook_id(faFiInfo.getString("accbook_id"));
        fReconciliation.setAccentity_id(faFiInfo.getString("accentity_id"));
        fReconciliation.setPeriod_start_date(faFiInfo.getString("period_start_date"));
        fReconciliation.setPeriod_end_date(faFiInfo.getString("period_end_date"));
        fReconciliation.setSubject_chart_id(faFiInfo.getString("subject_chart_id"));
        fReconciliation.setSubject_id(subject_id);
        fReconciliation.setCurrency(ori_currtype_id);
        // 对账项目
        List<LinkedHashMap> reconcproject = faFiInfo.getObject("reconcproject", List.class);
        if (CollectionUtils.isNotEmpty(reconcproject)) {
            String projectCode = (String) reconcproject.get(0).get("project_code");
            fReconciliation.setProjectCode(projectCode);
        }
        return fReconciliation;
    }


    private Journal buildJournal(Journal journal) {
        Journal journalData = new Journal();
        journalData.setDebitnatSum(journal.getDebitnatSum());
        journalData.setDebitoriSum(journal.getDebitoriSum());
        journalData.setCreditnatSum(journal.getCreditnatSum());
        journalData.setCreditoriSum(journal.getCreditoriSum());
        journalData.setCurrency(journal.getCurrency());
        return journalData;
    }


    /**
     * 查询期初期末余额
     * @param fReconciliation
     * @param settlementInit
     * @param settlementEnd
     * @return
     */
    private  FReconciliation queryInitData(FReconciliation fReconciliation,SettlementDetail settlementInit,SettlementDetail settlementEnd) {
        if (fReconciliation.getQueryBalance()) {
            if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.OriAndHome.getValue()) {
                if (Objects.nonNull(settlementInit)){
                    fReconciliation.setPeriod_begin_amount(settlementInit.getTodaylocalmoney());
                    // 期初原币余额
                    fReconciliation.setPeriod_begin_ori_amount(settlementInit.getTodayorimoney());
                } else {
                    fReconciliation.setPeriod_begin_amount(BigDecimal.ZERO);
                    // 期初原币余额
                    fReconciliation.setPeriod_begin_ori_amount(BigDecimal.ZERO);
                }
                if (Objects.nonNull(settlementEnd)){
                    // 期末本币余额
                    fReconciliation.setPeriod_end_amount(settlementEnd.getTodaylocalmoney());
                    // 期末原币余额
                    fReconciliation.setPeriod_end_ori_amount(settlementEnd.getTodayorimoney());
                } else {
                    fReconciliation.setPeriod_end_amount(BigDecimal.ZERO);
                    // 期末原币余额
                    fReconciliation.setPeriod_end_ori_amount(BigDecimal.ZERO);
                }

            } else if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.Home_Currency.getValue()) {
                // 期初本币余额
                if (Objects.nonNull(settlementInit)){
                    fReconciliation.setPeriod_begin_amount(settlementInit.getTodaylocalmoney());
                } else {
                    fReconciliation.setPeriod_begin_amount(BigDecimal.ZERO);
                }
                if (Objects.nonNull(settlementEnd)){
                    // 期末本币余额
                    fReconciliation.setPeriod_end_amount(settlementEnd.getTodaylocalmoney());
                } else {
                    // 期末本币余额
                    fReconciliation.setPeriod_end_amount(BigDecimal.ZERO);
                }

            } else if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.Ori_Currency.getValue()) {
                // 期初原币余额
                if (Objects.nonNull(settlementInit)) {
                    fReconciliation.setPeriod_begin_ori_amount(settlementInit.getTodayorimoney());
                } else {
                    fReconciliation.setPeriod_begin_ori_amount(BigDecimal.ZERO);
                }
                if (Objects.nonNull(settlementEnd)) {
                    // 期末原币余额
                    fReconciliation.setPeriod_end_ori_amount(settlementEnd.getTodayorimoney());
                } else {
                    fReconciliation.setPeriod_end_ori_amount(BigDecimal.ZERO);
                }
            }
        }
        return fReconciliation;
    }


    /**
     * 查询发生额
     * @param fReconciliation
     * @param journal
     * @return
     */
    private FReconciliation queryAmount (FReconciliation fReconciliation,Journal journal) {
        if (Objects.nonNull(fReconciliation.getDirectionJD()) && Objects.nonNull(journal)) {
            if (fReconciliation.getDirectionJD().getValue() == DirectionJD.Debit.getValue() || fReconciliation.getDirectionJD().getValue() == DirectionJD.CreditAndDebit.getValue()) {
                if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.OriAndHome.getValue()) {
                    fReconciliation.setDebitoriSum(journal.getDebitoriSum());
                    fReconciliation.setDebitnatSum(journal.getDebitnatSum());
                } else if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.Home_Currency.getValue()) {
                    fReconciliation.setDebitnatSum(journal.getDebitnatSum());
                } else if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.Ori_Currency.getValue()) {
                    fReconciliation.setDebitoriSum(journal.getDebitoriSum());
                }
            }
            if (fReconciliation.getDirectionJD().getValue() == DirectionJD.Credit.getValue() || fReconciliation.getDirectionJD().getValue() == DirectionJD.CreditAndDebit.getValue()) {
                if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.OriAndHome.getValue()) {
                    fReconciliation.setCreditoriSum(journal.getCreditoriSum());
                    fReconciliation.setCreditnatSum(journal.getCreditnatSum());
                } else if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.Home_Currency.getValue()) {
                    fReconciliation.setCreditnatSum(journal.getCreditnatSum());
                } else if (fReconciliation.getCurrencyClassification().getValue() == CurrencyClassification.Ori_Currency.getValue()) {
                    fReconciliation.setCreditoriSum(journal.getCreditoriSum());
                }
            }
        } else {
            fReconciliation.setCreditoriSum(BigDecimal.ZERO);
            fReconciliation.setCreditnatSum(BigDecimal.ZERO);
            fReconciliation.setDebitoriSum(BigDecimal.ZERO);
            fReconciliation.setDebitnatSum(BigDecimal.ZERO);
        }
        return fReconciliation;
    }
}
