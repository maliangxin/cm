package com.yonyoucloud.fi.cmp.withholdingrulesetting.service;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.LoginUser;
import com.yonyou.workbench.util.Lists;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.interestratesetting.AgreeIRSettingGrade;
import com.yonyoucloud.fi.cmp.interestratesetting.InterestRateSetting;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.vo.WithholdingResultVO;
import com.yonyoucloud.fi.cmp.withholding.*;
import com.yonyoucloud.fi.cmp.withholdingrulesetting.WithholdingRuleSettingConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xuxbo
 * @date 2023/4/19 13:54
 */
//@Slf4j
@Service
@Transactional
public class WithholdingRuleSettingServiceImpl implements WithholdingRuleSettingService {
    private static final Logger logger = LoggerFactory.getLogger(WithholdingRuleSettingServiceImpl.class);
    private static final String BILLNUM = "cmp_withholdingrulesettinglist";
    private static final String FIELD_NAME = "withholdingrulesettinglist";
    private static final String CMP_WITHHOLDINGRULESETTING = "cmp_withholdingrulesetting";
    private static final String CMP_INTERESTRATESETTING = "cmp_interestratesetting";
    private static final String RULE_SETTINGS = "yonbip_fi_ctmcmp—lock:withholdingrulesettings_";//@notranslate
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    CurrencyQueryService currencyQueryService;

    @Autowired
    private IFIBillService ifiBillService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    private FundsOrgQueryServiceComponent fundsOrgQueryService;

    private static final String END_DATE = "9999-12-31";
    /**
     * 账号同步接口实现*
     *
     * @return
     */
    @Override
    public int synchronousAccount() {
        int count = 0;
        //标识：是否是第一次同步
        boolean initflag = true;
        YmsLock ymsLock = null;
        try {
            if((ymsLock=JedisLockUtils.lockWithOutTrace(BILLNUM))==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100082"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D1","该组织下正在初始化数据，请稍后再试！") /* "该组织下正在初始化数据，请稍后再试！" */);
            }
            long tenantId = AppContext.getTenantId();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            List<Map<String, Object>> withholdingRuleSettingList = MetaDaoHelper.query(WithholdingRuleSetting.ENTITY_NAME, querySchema);
            if (withholdingRuleSettingList.size() > 0) {
                initflag = false;
            }
            //库中已经存在的账户集合
            Set<String> bankAccountSet = new HashSet<>();
            Set<String> withholdingRuleSettingAccentitySet = new HashSet<>();
            for (Map<String, Object> withholdingRuleSetting : withholdingRuleSettingList) {
                //银行账号+币种+使用组织
                String accentity = withholdingRuleSetting.get(ICmpConstant.ACCENTITY).toString();
                bankAccountSet.add(withholdingRuleSetting.get(WithholdingRuleSettingConstant.BANKACCOUNT).toString() + withholdingRuleSetting.get(ICmpConstant.CURRENCY).toString()+ accentity);
                withholdingRuleSettingAccentitySet.add(accentity);
            }
            // 查询启用的资金组织以及会计主体
            Set<String> openAccentitySet = fundsOrgQueryService.queryAccEntityByFundOrgIds(new ArrayList<>(withholdingRuleSettingAccentitySet)).keySet();
            // 按主组织权限查询会计主体 bankaccount
            Set<String> orgs = BillInfoUtils.getOrgPermissions(BILLNUM);
            List<String> orgIdList = new ArrayList<>();
            for (String o : orgs) {
                orgIdList.add(o);
            }
            List<EnterpriseBankAcctVOWithRange> dataList = new ArrayList<>();
            List authAccountIds = null;
            //根据当前用户权限查询所有有权限的组织查询所有有权限的银行账户
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setOrgidList(orgIdList);
            enterpriseParams.setPageSize(5000);
            dataList =  enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);

            // 获取有数据权限的银行账户
//            Map<String, List<Object>> accounts = AuthUtil.dataPermission("CM", WithholdingRuleSetting.ENTITY_NAME, null, new String[]{"bankaccount"});
//            if (initflag) {
//                //企业银行账户 ：
//                QuerySchema querySchemaBank = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,accountPurpose,lineNumber,enable,acctType,tenant,currencyList.currency as currency,currencyList.currency.name as currencyname, currencyList.currency.enable as currency_enable");
//                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
//                queryConditionGroup.addCondition(QueryCondition.name("enable").eq(1));
//                queryConditionGroup.addCondition(QueryCondition.name("dr").eq(0));
//                queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.CURRENCY_ENABLE_REf).eq(1));
//                queryConditionGroup.addCondition(QueryCondition.name("orgid").in(orgIdList));
//                //如果有数据权限 把按照数据权限的id进行查询 ，如果没有 则查询全部
//                if (accounts != null && accounts.size() > 0) {
//                    authAccountIds = accounts.get("bankaccount");
//                    queryConditionGroup.addCondition(QueryCondition.name("id").in(authAccountIds));
//                }
//                querySchemaBank.appendQueryCondition(queryConditionGroup);
//                dataList = MetaDaoHelper.query(InitDataConstant.BD_ENTERPRISE_ORGFINBANKACCTVO, querySchemaBank, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
//
//            } else {
//                //企业银行账户 ：全部的数据
//                QuerySchema querySchemaBank = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,accountPurpose,lineNumber,enable,acctType,tenant,currencyList.currency as currency,currencyList.currency.name as currencyname, currencyList.currency.enable as currency_enable");
//                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
//                queryConditionGroup.addCondition(QueryCondition.name("dr").eq(0));
//                queryConditionGroup.addCondition(QueryCondition.name("orgid").in(orgIdList));
//                //如果有数据权限 把按照数据权限的id进行查询 ，如果没有 则查询全部
//                if (accounts != null && accounts.size() > 0) {
//                    authAccountIds = accounts.get("bankaccount");
//                    queryConditionGroup.addCondition(QueryCondition.name("id").in(authAccountIds));
//                }
//                querySchemaBank.appendQueryCondition(queryConditionGroup);
//                dataList = MetaDaoHelper.query(InitDataConstant.BD_ENTERPRISE_ORGFINBANKACCTVO, querySchemaBank, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
//
//            }
            if (dataList == null || dataList.size() == 0) {
                return count;
            }
            //CM202400728：账户利率设置，账户同步逻辑放开数字钱包类型；
//            dataList = filterEnterpriseBankAcctByAcctOpenType(dataList);
            List<WithholdingRuleSetting> updateWithholdingRuleSettings = new ArrayList<>();
            List<WithholdingRuleSetting> insertWithholdingRuleSettings = new ArrayList<>();
            for (EnterpriseBankAcctVOWithRange map : dataList) {
                List<OrgRangeVO> orgRangeVOList = map.getAccountApplyRange();
                List<BankAcctCurrencyVO> currencylist = map.getCurrencyList();
                for (OrgRangeVO orgRange : orgRangeVOList) {
                    for (BankAcctCurrencyVO currency : currencylist) {
                        String orgId = (String) orgRange.getRangeOrgId();
                        String bankaccountId = String.valueOf(map.getId());
                        String bankAccountKey = bankaccountId + currency.getCurrency() + orgId;
                        if (!orgIdList.contains(orgId)) {
                            //continue;
                        }
                        // 和产品沟通过，禁用的资金组织或者会计主体不用同步账户
                        if (!openAccentitySet.contains(orgId)) {
                            continue;
                        }
                        if (!ValueUtils.isNotEmptyObj(currency.getCurrency())) {
                            continue;
                        }
                        //获取账户的启停用状态 和 币种的启停用状态
                        int accountStatus = (int) map.getEnable();
                        int currencyStatus = 0;
                        if (ObjectUtils.isEmpty(currency.getCurrencyEnable())) {
                            continue;
                        } else {
                            currencyStatus = (int) currency.getCurrencyEnable();
                        }
                        if (bankAccountSet.contains(bankAccountKey)) {
                            Short type = 1;
                            //如果是之前已经同步过的数据 进行update
                            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                            queryConditionGroup.addCondition(QueryCondition.name("accentity").eq(orgId), QueryCondition.name("currency").eq(currency.getCurrency()), QueryCondition.name("bankaccount").eq(bankaccountId));
                            querySchema1.appendQueryCondition(queryConditionGroup);
                            List<WithholdingRuleSetting> withholdingRuleSettings = MetaDaoHelper.queryObject(WithholdingRuleSetting.ENTITY_NAME, querySchema1, null);
                            WithholdingRuleSetting withholdingRuleSetting = withholdingRuleSettings.get(0);
                            //获取预提规则设置的状态
                            short releStatus = withholdingRuleSetting.getRuleStatus();
                            //如果预提规则设置是启用状态，账户或者币种为停用状态，则修改规则设置为停用
                            if (releStatus == WithholdingRuleStatus.Enable.getValue() && (accountStatus == 0 || accountStatus == 2 || currencyStatus == 0)) {
                                withholdingRuleSetting.setRuleStatus(WithholdingRuleStatus.Deactivate.getValue());
                                type = WithholdingRuleStatus.Deactivate.getValue();
                            }
                            //如果预提规则设置状态为停用状态，账户和币种同时为启用状态，则修改规则设置为启用
                            //                    if (releStatus == WithholdingRuleStatus.Deactivate.getValue() && (accountStatus == 1 && currencyStatus == 1)) {
                            //                        withholdingRuleSetting.setRuleStatus(WithholdingRuleStatus.Enable.getValue());
                            //                        type = WithholdingRuleStatus.Enable.getValue();
                            //                    }
                            withholdingRuleSetting.setBankType((String) map.getBank());
                            withholdingRuleSetting.setBankNumber((String) map.getBankNumber());
                            withholdingRuleSetting.setAccountPurpose((String) map.getAccountPurpose());
                            //增加版本号
                            withholdingRuleSetting.setVersion(ymsOidGenerator.nextId());
                            withholdingRuleSetting.setEntityStatus(EntityStatus.Update);
                            updateWithholdingRuleSettings.add(withholdingRuleSetting);
                            //  更新银行账户利率设置表
                            if (!withholdingRuleSetting.getRuleStatus().equals(WithholdingRuleStatus.Tobeset.getValue())) {
                                updateInterestRateSetting(withholdingRuleSetting, type);
                            }
                        } else {
                            //增加判断 银行账户或币种 如果有其中一个为停用 则continue   enable = 2 停用     0 是未启用    1是启用
                            if (accountStatus == 0 || accountStatus == 2 || currencyStatus == 0) {
                                continue;
                            }
                            WithholdingRuleSetting withholdingRuleSetting = new WithholdingRuleSetting();
                            withholdingRuleSetting.setId(ymsOidGenerator.nextId());
                            //增加版本号
                            withholdingRuleSetting.setVersion(ymsOidGenerator.nextId());
                            withholdingRuleSetting.setAccentity(orgId);
                            withholdingRuleSetting.setCurrency((String) currency.getCurrency());
                            withholdingRuleSetting.setBankaccount((String) map.getId());
                            withholdingRuleSetting.setBankType((String) map.getBank());
                            withholdingRuleSetting.setBankNumber((String) map.getBankNumber());
                            withholdingRuleSetting.setAccountPurpose((String) map.getAccountPurpose());
                            withholdingRuleSetting.setTenant(tenantId);
                            withholdingRuleSetting.setRuleStatus(WithholdingRuleStatus.Tobeset.getValue());
                            withholdingRuleSetting.setEntityStatus(EntityStatus.Insert);
                            insertWithholdingRuleSettings.add(withholdingRuleSetting);
                        }
                    }
                }
            }
            if (insertWithholdingRuleSettings.size() > 0) {
                CmpMetaDaoHelper.insert(WithholdingRuleSetting.ENTITY_NAME, insertWithholdingRuleSettings);
                count = insertWithholdingRuleSettings.size();
            }
            if (updateWithholdingRuleSettings.size() > 0) {
                MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, updateWithholdingRuleSettings);
            }
        } catch (Exception e) {
            logger.error("同步账户失败!" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101597"),e.getMessage());
        } finally {
            JedisLockUtils.unlockWithOutTrace(ymsLock);
        }
        return count;
    }

//    private List<EnterpriseBankAcctVOWithRange> filterEnterpriseBankAcctByAcctOpenType(List<EnterpriseBankAcctVOWithRange> dataList) {
//        return dataList.stream()
//                .filter(item -> AcctopenTypeEnum.DigitalWallet.getValue() != item.getAcctopentype())
//                .collect(Collectors.toList());
//    }

    /**
     * 预提规则设置提交接口实现*
     *
     * @param withholdingRuleSetting
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject withholdingRuleSettingSave(WithholdingRuleSetting withholdingRuleSetting) throws Exception {
        //必输项校验
        checkData(withholdingRuleSetting);
        return doSettings(withholdingRuleSetting);
    }

    @Override
    public WithholdingRuleSetting onlyWithholdingRuleSetting(Long id) throws Exception {
        if(id != null){
            //查询
            WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id, 1);
            return withholdingRuleSetting;
        }
        return null;
    }
    @Override
    public WithholdingRuleSetting agreeIRSettingHistoryDetail(Long id) throws Exception {
        if(id != null){
            AgreeIRSettingHistory agreeIRSettingHistory = MetaDaoHelper.findById(AgreeIRSettingHistory.ENTITY_NAME, id, 2);

            List<AgreeIRSettingGradeHistory> agreeIRSettingHistoryList = agreeIRSettingHistory.agreeIRSettingGradeHistory();
            if (null != agreeIRSettingHistoryList && agreeIRSettingHistoryList.size()>0){
                Map<String, AgreeIRSettingGradeHistory> agreeIRSettingGradeHistoryMap = new HashMap<>();
                //遍历分档信息
                for(AgreeIRSettingGradeHistory agreeIRSettingGradeHistory : agreeIRSettingHistoryList){
                    agreeIRSettingGradeHistoryMap.put(agreeIRSettingGradeHistory.getId().toString(), agreeIRSettingGradeHistory);
                }
                //补全“基准利率类型币种”、“基准利率类型”名称
                QueryConditionGroup activityGroup = QueryConditionGroup.and(QueryCondition.name("id").in(agreeIRSettingGradeHistoryMap.keySet()));
                QuerySchema activitySchema = QuerySchema.create().addSelect("id,baseirtypecurrency,baseirtypecurrency.name as baseirtypecurrency_name,baseirtype.name as baseirtype_name").addCondition(activityGroup);
                List<Map<String, Object>> agreeIRSettingGradeHistoryList = MetaDaoHelper.query(AgreeIRSettingGradeHistory.ENTITY_NAME, activitySchema);
                if(agreeIRSettingGradeHistoryList != null && agreeIRSettingGradeHistoryList.size() > 0){
                    for(Map<String, Object> map : agreeIRSettingGradeHistoryList) {
                        String idStr = map.get("id").toString();
                        agreeIRSettingGradeHistoryMap.get(idStr).put("baseirtypecurrency_name", map.get("baseirtypecurrency_name"));
                        agreeIRSettingGradeHistoryMap.get(idStr).put("baseirtype_name", map.get("baseirtype_name"));
                    }
                }
            }

            if(agreeIRSettingHistory != null){
                //查询主表
                WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, agreeIRSettingHistory.getMainid(), 1);
                withholdingRuleSetting.setAgreeIRSettingHistory(Lists.newArrayList(agreeIRSettingHistory));
                return withholdingRuleSetting;
            }
        }
        return null;
    }

    @Override
    public void agreeIRSettingHistoryDelete(Long id) throws Exception {
        if(id != null){
            MetaDaoHelper.delete(AgreeIRSettingHistory.ENTITY_NAME, id);
        }
    }

    /**
     * 必输项校验*
     *
     * @param withholdingRuleSetting
     */
    private void checkData(WithholdingRuleSetting withholdingRuleSetting) throws Exception {

        AssertUtils.isNull(withholdingRuleSetting, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057B","请求数据不可为空！") /* "请求数据不可为空！" */);
        AssertUtils.isEmpty(withholdingRuleSetting.getWithholdingRuleSettingList(), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057D","规则设置数据不可为空") /* "规则设置数据不可为空" */));
//        short dailySettlementControl = withholdingRuleSetting.getDailySettlementControl();
        //校验相同银行账户+币种已经存在启用状态的预提规则记录
        Json jsondata = new Json(CtmJSONObject.toJSONString(withholdingRuleSetting.getWithholdingRuleSettingList()));
        List<WithholdingRuleSetting> accentitylist = Objectlizer.decode(jsondata, WithholdingRuleSetting.ENTITY_NAME);
        checkStatusRepeat(accentitylist);
        if (ObjectUtils.isEmpty(withholdingRuleSetting.get("dailySettlementControl"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101598"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180580","日结控制不可为空！") /* "日结控制不可为空！" */));
        }
        short ruleStatus = Short.parseShort(((Map) withholdingRuleSetting.getWithholdingRuleSettingList().get(0)).get("ruleStatus").toString());
        //当规则状态为待设置状态需要校验
        if (ValueUtils.isNotEmptyObj(ruleStatus) && ruleStatus == WithholdingRuleStatus.Tobeset.getValue()) {
            //存款利率
//            BigDecimal interestRate = new BigDecimal(withholdingRuleSetting.get("interestRate").toString());
            if (ObjectUtils.isEmpty(withholdingRuleSetting.get("interestRate"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101599"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180584","存款利率不可为空！") /* "存款利率不可为空！" */));
            }
            //透支利率
//            BigDecimal overdraftRate = new BigDecimal(withholdingRuleSetting.get("overdraftRate").toString());
            if (ObjectUtils.isEmpty(withholdingRuleSetting.get("overdraftRate"))) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101600"),MessageUtils.getMessage(MessageUtils.getMessage("P_YS_CTM_CM-BE_1713352554276454433") /* "透支利率不可为空！" */));
                //后续修改 透支利率赋默认值为0
                BigDecimal def = new BigDecimal(0);
                withholdingRuleSetting.set("overdraftRate", def);
            }
            //计息天数 interestDays
//            short interestDays = Short.parseShort(withholdingRuleSetting.get("interestDays"));
            if (ObjectUtils.isEmpty(withholdingRuleSetting.get("interestDays"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101601"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180589","计息天数不可为空！") /* "计息天数不可为空！" */));
            }
            //上次结息结束日
//            Date lastInterestSettlementDate = DateUtils.parseDate(withholdingRuleSetting.get("lastInterestSettlementDate"));
            if (ObjectUtils.isEmpty(withholdingRuleSetting.get("lastInterestSettlementDate"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101602"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418058E","上次结息结束日不可为空！") /* "上次结息结束日不可为空！" */));
            }

            //协定利率签约简单校验
            if(withholdingRuleSetting.getIssignagree() == IssignagreeStatusEnum.SIGNAGREE.getValue()){
                if(withholdingRuleSetting.getAgreeinterestdays() == null || withholdingRuleSetting.getAgreeinterestmethod() == null || withholdingRuleSetting.getAgreerelymethod() == null
                        || withholdingRuleSetting.getAgreestartdate() == null){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101603"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057B","请求数据不可为空！") /* "请求数据不可为空！" */);
                }

                List<AgreeIRGrade> agreeIRGradeList = withholdingRuleSetting.agreeIRGrade();
                if(agreeIRGradeList == null || agreeIRGradeList.size() == 0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101603"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057B","请求数据不可为空！") /* "请求数据不可为空！" */);
                }

                for(AgreeIRGrade agreeIRGrade : agreeIRGradeList){
                    if(agreeIRGrade.getGradenum() == null || agreeIRGrade.getGradeoption() == null || agreeIRGrade.getAmount() == null || agreeIRGrade.getIrtype() == null || agreeIRGrade.getInterestrate() == null){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101603"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057B","请求数据不可为空！") /* "请求数据不可为空！" */);
                    }
                    if(agreeIRGrade.getIrtype() == IRTypeEnum.FLOAT.getValue()){
                        // 计息方式：日均余额整期计算，利率类型只能是固定；浮动利率相关字段不能为空
                        if(withholdingRuleSetting.getAgreeinterestmethod() == AgreeinterestmethodEnum.DAY_AVERAGE.getValue() || agreeIRGrade.getBaseirtypecurrency() == null || agreeIRGrade.getBaseirtype() == null || agreeIRGrade.getBaseir() == null || agreeIRGrade.getFloatvalue() == null){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101603"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057B","请求数据不可为空！") /* "请求数据不可为空！" */);
                        }
                    }
                }
            }
        }
    }

    /**
     * 规则设置*
     *
     * @param withholdingRuleSetting
     * @return
     * @throws Exception
     */
    private CtmJSONObject doSettings(WithholdingRuleSetting withholdingRuleSetting) throws Exception {
        //取规则设置状态
        short ruleStatus = Short.parseShort(((Map) withholdingRuleSetting.getWithholdingRuleSettingList().get(0)).get("ruleStatus").toString());
        List<LinkedHashMap> bizObjects = withholdingRuleSetting.get(FIELD_NAME);
        Map<Long, LinkedHashMap> bizObjectMap = bizObjects.stream().filter(bill -> Objects.nonNull(bill.get(ICmpConstant.PRIMARY_ID)))
                .collect(Collectors.toMap(bill -> Long.parseLong(bill.get(ICmpConstant.PRIMARY_ID).toString()), LinkedHashMap -> LinkedHashMap));
        List<Long> ids = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        bizObjectMap.keySet().forEach(id -> {
            args.add(id.toString());
            ids.add(id);
            keys.add(RULE_SETTINGS + AppContext.getCurrentUser().getYTenantId() + id);
        });

        // 同步锁
        try {
            return CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, 600L, TimeUnit.SECONDS, (int status) -> {
                if (status == LockStatus.GETLOCK_FAIL) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101604"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418058D","单据正在操作，请稍后再试！") /* "单据正在操作，请稍后再试！" */);
                }
                // 根据id重新查询出数据库中最新单据
                List<WithholdingRuleSetting> withholdingRuleSettingList = MetaDaoUtils.batchQueryBizObject(WithholdingRuleSetting.ENTITY_NAME, ICmpConstant.PRIMARY_ID, ids);
                if (CollectionUtils.isEmpty(withholdingRuleSettingList)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101605"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057E","单据不存在或已被删除！") /* "单据不存在或已被删除！" */));
                }
                int size = withholdingRuleSettingList.size();
                //需要更新的预提规则设置单据列表
                List<WithholdingRuleSetting> withholdingRuleSettings = new ArrayList<>();
                //活期利率变更历史
                List<InterestRateSettingHistory> interestRateSettingHistoryList = new ArrayList<>();
                //协定利率变更历史
                List<AgreeIRSettingHistory> agreeIRSettingHistoryList = new ArrayList<>();
                //需要插入的银行账户利率设置单据列表
                List<InterestRateSetting> interestRateSettingList = new ArrayList<>();

                WithholdingResultVO withholdingResultVO = new WithholdingResultVO();
                //遍历数据库查询数据
                for (WithholdingRuleSetting bill : withholdingRuleSettingList) {
                    try {
                        //校验：平台银行账户档案的状态，如果为停用，提交失败
                        checkEnable(bill);
                        //组装规则设置信息
                        Long mapId = Long.parseLong(bill.getId().toString());
                        assembleData(withholdingRuleSetting, bill, withholdingRuleSettings, bizObjectMap.get(mapId));
                        assembleHistory(bill, interestRateSettingHistoryList);
                        assembleAgreeIRSettingData(bill, withholdingRuleSetting.agreeIRGrade(), agreeIRSettingHistoryList);
                        //增加判断：当规则状态为待设置的时候，进行银行账户利率设置数据的insert
                        if (ValueUtils.isNotEmptyObj(ruleStatus) && ruleStatus == WithholdingRuleStatus.Tobeset.getValue()) {
                            //组装银行账户利率设置数据信息
                            assembleInterestRateData(bill, interestRateSettingList);
                        }

                    } catch (Exception e) {
                        if (size == 1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101606"),e.getMessage());
                        }
                        // 组装错误信息
                        withholdingResultVO.getFailed().put(bill.getId().toString(), bill.getId());
                        //根据账号id和币种id查询 账号以及币种name
                        EnterpriseBankAcctVO enterpriseBankAcctVO = null;
                        CurrencyTenantDTO currencyTenantDTO = null;

                        enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankaccount());
                        //判断 如果为空 传入enable 为停用 重新查
                        //如果为空 说明未启用
//                        if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
//                            Integer enable = 2;
//                            enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankaccount(),enable);
//                        }
                        currencyTenantDTO = currencyQueryService.findById(bill.getCurrency());
                        String bankaccount = enterpriseBankAcctVO.getAccount();
                        String currencyName = currencyTenantDTO.getName();
                        withholdingResultVO.getMessages().add(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180583","银行账号为【%s】，币种为【%s】%s！") /* "银行账号为【%s】，币种为【%s】%s！" */), bankaccount, currencyName, e.getMessage()));
                        withholdingResultVO.addFailCount();
                        continue;
                    }
                }
                if (CollectionUtils.isNotEmpty(withholdingRuleSettings)) {
                    BillDataDto bill = new BillDataDto();
                    bill.setBillnum(CMP_WITHHOLDINGRULESETTING);
                    bill.setData(withholdingRuleSettings);
                    // 通过save规则保存数据
                    ifiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
                }
                // 新增活期利率历史记录
                if (CollectionUtils.isNotEmpty(interestRateSettingHistoryList)) {
                    CmpMetaDaoHelper.insert(InterestRateSettingHistory.ENTITY_NAME, interestRateSettingHistoryList);
                }
                // 新增协定利率历史记录
                if (CollectionUtils.isNotEmpty(agreeIRSettingHistoryList)) {
                    CmpMetaDaoHelper.insert(AgreeIRSettingHistory.ENTITY_NAME, agreeIRSettingHistoryList);
                }

                //规则设置 提交保存成功之后，还需要生成对应的银行账户利率设置数据
                if (CollectionUtils.isNotEmpty(interestRateSettingList)) {
//                BillDataDto bill = new BillDataDto();
//                bill.setBillnum(CMP_INTERESTRATESETTING);
//                bill.setData(interestRateSettingList);
//                // 通过save规则保存数据
//                ifiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
                    CmpMetaDaoHelper.insert(InterestRateSetting.ENTITY_NAME, interestRateSettingList);
                    List<AgreeIRSettingGrade> agreeIRSettingGradeList = new ArrayList<>();
                }


                //增加业务日志
                try {
                    CtmJSONObject jsonObject = new CtmJSONObject();
                    jsonObject.put("WithholdingRuleSetting", withholdingRuleSetting);
                    jsonObject.put("withholdingRuleSettings", withholdingRuleSettings);
                    ctmcmpBusinessLogService.saveBusinessLog(jsonObject, "", "", IServicecodeConstant.WITHHOLDINGRULESETTING,
                            IMsgConstant.CMP_WITHHOLDINGRELESETTING, IMsgConstant.CMP_WITHHOLDINGRELESETTING);
                } catch (Exception e) {
                    logger.info("============= insertOrUpdate ctmcmpBusinessLogService：" + e.getMessage());
                }

                withholdingResultVO.setCount(size);
                withholdingResultVO.setSucessCount(size - withholdingResultVO.getFailCount());
                return withholdingResultVO.getResult();
            });

        } catch (Exception e) {
            logger.error("withholdingrulesetting error", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101607"),e.getMessage());
        }
    }

    /**
     * 校验：平台银行账户档案的状态，如果为停用，提交失败 *
     *
     * @param checkRuleSetting
     * @throws Exception
     */
    private void checkEnable(WithholdingRuleSetting checkRuleSetting) throws Exception {
        //银行账户id
        String bankaccountId = checkRuleSetting.getBankaccount();
        //币种id
        String currencyId = checkRuleSetting.getCurrency();
        EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bankaccountId);
        //如果为空 说明未启用
        if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101608"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057F","银行账户档案已停用，规则设置失败!") /* "银行账户档案已停用，规则设置失败!" */));
        } else {
            //银行账号的启停用
            int enable = enterpriseBankAcctVO.getEnable();
            int currencyEnable = 1;
            //查询币种的启停用状态
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
            for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                if (bankAcctCurrencyVO.getCurrency().equals(currencyId)) {
                    //币种的启停用
                    currencyEnable = bankAcctCurrencyVO.getCurrencyEnable();
                }
            }
            if (ObjectUtils.isNotEmpty(enable) && (enable != 1 || currencyEnable != 1)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101608"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057F","银行账户档案已停用，规则设置失败!") /* "银行账户档案已停用，规则设置失败!" */));
            }
        }
    }

    /**
     * 组装规则设置信息*
     *
     * @param withholdingRuleSetting     前端传的数据 要修改的字段数据
     * @param childRuleSetting           根据id重新查询出数据库中最新单据
     * @param withholdingRuleSettingList 需要更新的单据列表
     * @param map                        更新之前的列表数据
     * @throws Exception
     */
    private void assembleData(WithholdingRuleSetting withholdingRuleSetting, WithholdingRuleSetting childRuleSetting, List<WithholdingRuleSetting> withholdingRuleSettingList, LinkedHashMap map) throws Exception {
        //检验单据是否是最新状态
        Date oldPubts = DateUtils.parseDate(map.get("pubts").toString(), DateUtils.DATE_TIME_FORMAT);
        AssertUtils.isTrue(oldPubts.before(childRuleSetting.getPubts()),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180587","单据不是最新状态") /* "单据不是最新状态" */);
        //日结控制
        short dailySettlementControl = withholdingRuleSetting.getDailySettlementControl();
        childRuleSetting.setDailySettlementControl(dailySettlementControl);
        short ruleStatus = Short.parseShort(((Map) withholdingRuleSetting.getWithholdingRuleSettingList().get(0)).get("ruleStatus").toString());
        if (ValueUtils.isNotEmptyObj(ruleStatus) && ruleStatus == WithholdingRuleStatus.Tobeset.getValue()) {
            //存款利率
            BigDecimal interestRate = new BigDecimal(withholdingRuleSetting.get("interestRate").toString());
            childRuleSetting.setInterestRate(interestRate);
            if(withholdingRuleSetting.get("overdraftRate") != null){
                //透支利率
                BigDecimal overdraftRate = new BigDecimal(withholdingRuleSetting.get("overdraftRate").toString());
                childRuleSetting.setOverdraftRate(overdraftRate);
            }
            //计息天数
            short interestDays = Short.parseShort(withholdingRuleSetting.get("interestDays"));
            childRuleSetting.setInterestDays(interestDays);
            //上次结息结束日
            Date lastInterestSettlementDate = DateUtils.parseDate(withholdingRuleSetting.get("lastInterestSettlementDate"));
            childRuleSetting.setLastInterestSettlementDate(lastInterestSettlementDate);
            //待设置状态改为启用状态 ：修改为在保存规则中进行设置
//            childRuleSetting.setRuleStatus(WithholdingRuleStatus.Enable.getValue());
            //签约协定存款
            childRuleSetting.setIssignagree(withholdingRuleSetting.getIssignagree());
            //已签约
            if(withholdingRuleSetting.getIssignagree() == IssignagreeStatusEnum.SIGNAGREE.getValue()){
                //协定存款计息天数
                childRuleSetting.setAgreeinterestdays(withholdingRuleSetting.getAgreeinterestdays());
                //协定存款计息方式
                childRuleSetting.setAgreeinterestmethod(withholdingRuleSetting.getAgreeinterestmethod());
                //协定存款靠档方式
                childRuleSetting.setAgreerelymethod(withholdingRuleSetting.getAgreerelymethod());
                //协定存款生效日期
                childRuleSetting.setAgreestartdate(withholdingRuleSetting.getAgreestartdate());
                //协定存款结束日期
                childRuleSetting.setAgreeenddate(withholdingRuleSetting.getAgreeenddate());
            }
        }
        //增加版本号
        childRuleSetting.setVersion(ymsOidGenerator.nextId());
        childRuleSetting.setEntityStatus(EntityStatus.Update);
        withholdingRuleSettingList.add(childRuleSetting);
    }

    /**
     * 组装活期利率历史数据*
     *
     * @param withholdingRuleSetting
     * @param interestRateSettingHistoryList 活期利率历史
     * @throws Exception
     */
    public void assembleHistory(WithholdingRuleSetting withholdingRuleSetting, List<InterestRateSettingHistory> interestRateSettingHistoryList) throws Exception {

        InterestRateSettingHistory history = new InterestRateSettingHistory();
//            history.setVersion(1);
        history.setOptionType(OptionType.Create.getValue());
        // 获取用户
        LoginUser loginUser = AppContext.getCurrentUser();
        Date endDate = DateUtils.parseDate(END_DATE);
        Date now = new Date();
        String userName = loginUser.getName();
        Long userId = loginUser.getId();
        history.setId(ymsOidGenerator.nextId());
        //上次结息结束日+1天是利率生效日期
        history.setStartDate(DateUtils.dateAdd(withholdingRuleSetting.getLastInterestSettlementDate(),1,false));
        history.setEndDate(endDate);
        history.setInterestRate(withholdingRuleSetting.getInterestRate());
        history.setOverdraftRate(withholdingRuleSetting.getOverdraftRate());
        history.setInterestDays(withholdingRuleSetting.getInterestDays());
        history.setModifyTime(now);
        history.setModifierId(userId);
        history.setModifier(userName);
        history.setCreateTime(now);
        history.setCreatorId(userId);
        history.setCreator(userName);
//        history.setIsNew(true);
        history.setMainid(withholdingRuleSetting.getId());
        history.setEntityStatus(EntityStatus.Insert);
        interestRateSettingHistoryList.add(history);

    }

    /**
     * 组装协定利率历史信息*
     *
     * @param withholdingRuleSetting     数据库查询后已更新后的
     * @param agreeIRGradeList           根据id重新查询出数据库中最新单据
     * @param agreeIRGradeList           协定利率历史
     * @throws Exception
     */
    private void assembleAgreeIRSettingData(WithholdingRuleSetting withholdingRuleSetting, List<AgreeIRGrade> agreeIRGradeList, List<AgreeIRSettingHistory> agreeIRSettingHistoryList) throws Exception {
        //未签约直接返回
        if(withholdingRuleSetting.getIssignagree() != IssignagreeStatusEnum.SIGNAGREE.getValue()){
            return;
        }
        //协定存款利率变更历史记录子表实体
        LoginUser loginUser = AppContext.getCurrentUser();
        Date now = new Date();

        AgreeIRSettingHistory agreeIRSettingHistory = new AgreeIRSettingHistory();
        agreeIRSettingHistory.setId(ymsOidGenerator.nextId());
        agreeIRSettingHistory.setMainid(withholdingRuleSetting.getId());
        agreeIRSettingHistory.setInterestDays(withholdingRuleSetting.getAgreeinterestdays());
        agreeIRSettingHistory.setAgreeinterestmethod(withholdingRuleSetting.getAgreeinterestmethod());
        agreeIRSettingHistory.setAgreerelymethod(withholdingRuleSetting.getAgreerelymethod());
        agreeIRSettingHistory.setStartDate(withholdingRuleSetting.getAgreestartdate());
        agreeIRSettingHistory.setEndDate(withholdingRuleSetting.getAgreeenddate());
        agreeIRSettingHistory.setOptionType(OptionType.Create.getValue());
        // TODO 这俩字段是干啥的
//        agreeIRSettingHistory.setRateid();
        agreeIRSettingHistory.setIsNew(true);
        agreeIRSettingHistory.setVersion(ymsOidGenerator.nextId());
        agreeIRSettingHistory.setCreateDate(now);
        agreeIRSettingHistory.setCreateTime(now);
        agreeIRSettingHistory.setCreator(loginUser.getName());
        agreeIRSettingHistory.setCreatorId(loginUser.getId());
        agreeIRSettingHistory.setEntityStatus(EntityStatus.Insert);

        //协定利率UI分档子表
        List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistoryList = new ArrayList<>();

        for(AgreeIRGrade agreeIRGrade : agreeIRGradeList){
            AgreeIRSettingGradeHistory agreeIRSettingGradeHistory = new AgreeIRSettingGradeHistory();
            agreeIRSettingGradeHistory.setId(ymsOidGenerator.nextId());
            agreeIRSettingGradeHistory.setSubid(agreeIRSettingHistory.getId());
            agreeIRSettingGradeHistory.setGradenum(agreeIRGrade.getGradenum());
            agreeIRSettingGradeHistory.setGradeoption(agreeIRGrade.getGradeoption());
            agreeIRSettingGradeHistory.setAmount(agreeIRGrade.getAmount());
            agreeIRSettingGradeHistory.setIrtype(agreeIRGrade.getIrtype());
            if(agreeIRGrade.getIrtype() == IRTypeEnum.FLOAT.getValue()){
                agreeIRSettingGradeHistory.setBaseirtypecurrency(agreeIRGrade.getBaseirtypecurrency());
                agreeIRSettingGradeHistory.setBaseirtype(agreeIRGrade.getBaseirtype());
                agreeIRSettingGradeHistory.setBaseir(agreeIRGrade.getBaseir());
                agreeIRSettingGradeHistory.setFloatvalue(agreeIRGrade.getFloatvalue());
            }
            agreeIRSettingGradeHistory.setInterestrate(agreeIRGrade.getInterestrate());
            agreeIRSettingGradeHistory.setCreateTime(now);
            agreeIRSettingGradeHistory.setCreateDate(now);
            agreeIRSettingGradeHistory.setCreatorId(loginUser.getId());
            agreeIRSettingGradeHistory.setCreator(loginUser.getName());
            agreeIRSettingGradeHistory.setEntityStatus(EntityStatus.Insert);
            agreeIRSettingGradeHistoryList.add(agreeIRSettingGradeHistory);
        }
        agreeIRSettingHistory.setAgreeIRSettingGradeHistory(agreeIRSettingGradeHistoryList);
        agreeIRSettingHistoryList.add(agreeIRSettingHistory);
    }
    /**
     * 组装银行账户利率设置数据*
     *
     * @param withholdingRuleSetting
     * @param interestRateSettingList
     * @throws Exception
     */
    private void assembleInterestRateData(WithholdingRuleSetting withholdingRuleSetting, List<InterestRateSetting> interestRateSettingList) throws Exception {
        // 获取用户
        LoginUser loginUser = AppContext.getCurrentUser();
        Date now = new Date();
        String userName = loginUser.getName();
        Long userId = loginUser.getId();
        InterestRateSetting interestRateSetting = new InterestRateSetting();
        interestRateSetting.setId(ymsOidGenerator.nextId());
        interestRateSetting.setAccountNumberId(withholdingRuleSetting.getId());
        interestRateSetting.setAccentity(withholdingRuleSetting.getAccentity());
        interestRateSetting.setBankAccount(withholdingRuleSetting.getBankaccount());
        interestRateSetting.setBankType(withholdingRuleSetting.getBankType());
        interestRateSetting.setCurrency(withholdingRuleSetting.getCurrency());
        interestRateSetting.setInterestRate(withholdingRuleSetting.getInterestRate());
        interestRateSetting.setOverdraftRate(withholdingRuleSetting.getOverdraftRate());
        interestRateSetting.setInterestDays(withholdingRuleSetting.getInterestDays());
        interestRateSetting.setStartDate(withholdingRuleSetting.getLastInterestSettlementDate());
        interestRateSetting.setIssignagree(withholdingRuleSetting.getIssignagree());

        interestRateSetting.setCreateTime(now);
        interestRateSetting.setCreateDate(now);
        interestRateSetting.setCreatorId(userId);
        interestRateSetting.setCreator(userName);

        //修改为启用：默认为启用，之后更改停用状态的时候才会设置为停用   规则设置操作 利率设置的状态一直为启用
        interestRateSetting.setRuleStatus(WithholdingRuleStatus.Enable.getValue());
//        interestRateSetting.setModifyTime(now);
//        interestRateSetting.setModifierId(userId);
//        interestRateSetting.setModifier(userName);
        interestRateSetting.setEntityStatus(EntityStatus.Insert);
        interestRateSettingList.add(interestRateSetting);
    }


    /**
     * 启停用状态更新*
     *
     * @param data
     * @param type
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject updateStatus(List<WithholdingRuleSetting> data, Short type) throws Exception {
        short enable = WithholdingRuleStatus.Enable.getValue();
        short unenable = WithholdingRuleStatus.Deactivate.getValue();
        //校验相同银行账户+币种已经存在启用状态的预提规则记录
        if (type.equals(enable)) {
            checkStatusRepeat(data);
        }
        //预提规则启停用状态更新之后，同时还需要更新银行账户利率设置的启停用状态
        Map<Long, List<WithholdingRuleSetting>> map = data.stream().collect(Collectors.groupingBy(WithholdingRuleSetting::getId));
        List<Long> ids = new ArrayList<>();
        map.keySet().forEach(id -> {
            ids.add(id);
        });
        // 根据id重新查询出数据库中最新单据
        List<WithholdingRuleSetting> withholdingRuleSettingList = MetaDaoUtils.batchQueryBizObject(WithholdingRuleSetting.ENTITY_NAME, ICmpConstant.PRIMARY_ID, ids);
        int size = withholdingRuleSettingList.size();
        WithholdingResultVO withholdingResultVO = new WithholdingResultVO();
        if (size < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101609"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180585","单据不存在或已被删除!") /* "单据不存在或已被删除!" */));
        } else if (size == 1) {
            // 获取时间戳
            Date pubts = map.get(withholdingRuleSettingList.get(0).getId()).get(0).getPubts();
            // 单条操作
            WithholdingRuleSetting entity = withholdingRuleSettingList.get(0);
            if (type.equals(enable)) {
                //启用
                updateStatus(entity, enable, pubts);
                withholdingResultVO.getMessages().add(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180588","规则启用成功！") /* "规则启用成功！" */));
            } else {
                //停用
                updateStatus(entity, unenable, pubts);
                withholdingResultVO.getMessages().add(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418058C","规则停用成功！") /* "规则停用成功！" */));
            }
            entity.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, entity);
            // 更新银行账户利率设置数据
            updateInterestRateSetting(entity, type);

        } else if (size > 1) {
            //批量更新状态
            List<WithholdingRuleSetting> updateWithholdingRuleSettingList = new ArrayList<>();
            if (type.equals(enable)) {
                updateMultiple(withholdingRuleSettingList, map, withholdingResultVO, type, updateWithholdingRuleSettingList);
            } else if (type.equals(unenable)) {
                updateMultiple(withholdingRuleSettingList, map, withholdingResultVO, type, updateWithholdingRuleSettingList);
            }
            MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, withholdingRuleSettingList);
        }
        withholdingResultVO.setCount(size);
        withholdingResultVO.setSucessCount(size - withholdingResultVO.getFailCount());
        return withholdingResultVO.getResult();
    }

    /**
     * 处理单条
     *
     * @param entity
     * @param type
     * @param pubts
     * @throws Exception
     */
    private void updateStatus(WithholdingRuleSetting entity, Short type, Date pubts) throws Exception {
        short enable = WithholdingRuleStatus.Enable.getValue();
        if (type.equals(entity.getRuleStatus())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101610"),type.equals(enable) ? MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418058B","规则设置已经启用，不可重复启用") /* "规则设置已经启用，不可重复启用" */) : MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418058A","规则设置已经停用，不可重复停用") /* "规则设置已经停用，不可重复停用" */));
        }
        if (entity.getPubts().getTime() != pubts.getTime()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101611"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418058F","数据不是最新状态，请刷新后重试！") /* "数据不是最新状态，请刷新后重试！" */));
        } else {
            entity.setRuleStatus(type);
            entity.setVersion(ymsOidGenerator.nextId());
        }
    }


    /**
     * 更新多条*
     *
     * @param withholdingRuleSettingList
     * @param map
     * @param withholdingResultVO
     * @param type
     * @throws Exception
     */
    private void updateMultiple(List<WithholdingRuleSetting> withholdingRuleSettingList, Map<Long, List<WithholdingRuleSetting>> map, WithholdingResultVO withholdingResultVO, Short type, List<WithholdingRuleSetting> updateWithholdingRuleSettingList) throws Exception {

        for (WithholdingRuleSetting bill : withholdingRuleSettingList) {
            Long mapId = Long.parseLong(bill.getId().toString());
            Date oldpubts = map.get(mapId).get(0).getPubts();
            //根据账号id和币种id查询 账号以及币种name
            EnterpriseBankAcctVO enterpriseBankAcctVO = null;
            CurrencyTenantDTO currencyTenantDTO = null;
            enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankaccount());
            //判断 如果为空 传入enable 为停用 重新查
            //如果为空 说明未启用
//                if (ObjectUtils.isEmpty(enterpriseBankAcctVO)) {
//                    Integer enable = 2;
//                    enterpriseBankAcctVO = enterpriseBankQueryService.findByIdAndEnable(bill.getBankaccount(),enable);
//                }
            currencyTenantDTO = currencyQueryService.findById(bill.getCurrency());
            String bankaccount = enterpriseBankAcctVO.getAccount();
            String currencyName = currencyTenantDTO.getName();
            if (type.equals(bill.getRuleStatus())) {
                // 组装错误信息
                withholdingResultVO.getFailed().put(bill.getId().toString(), bill.getId());
                if (type.equals(WithholdingRuleStatus.Enable.getValue())) {
                    withholdingResultVO.getMessages().add(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057A","银行账号为【%s】，币种为【%s】%s的预提规则状态为启用，不可重复启用！") /* "银行账号为【%s】，币种为【%s】%s的预提规则状态为启用，不可重复启用！" */), bankaccount, currencyName));
                } else {
                    withholdingResultVO.getMessages().add(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418057C","银行账号为【%s】，币种为【%s】%s的预提规则状态为停用，不可重复停用！") /* "银行账号为【%s】，币种为【%s】%s的预提规则状态为停用，不可重复停用！" */), bankaccount, currencyName));
                }
                withholdingResultVO.addFailCount();
            } else if (bill.getPubts().getTime() != oldpubts.getTime()) {
                // 组装错误信息
                withholdingResultVO.getFailed().put(bill.getId().toString(), bill.getId());
                withholdingResultVO.getMessages().add(String.format(MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180581","银行账号为【%s】，币种为【%s】%s的预提规则数据不是最新状态，请刷新后重试！") /* "银行账号为【%s】，币种为【%s】%s的预提规则数据不是最新状态，请刷新后重试！" */), bankaccount, currencyName));
                withholdingResultVO.addFailCount();
            } else {
                bill.setRuleStatus(type);
                bill.setVersion(ymsOidGenerator.nextId());
                bill.setEntityStatus(EntityStatus.Update);
                updateWithholdingRuleSettingList.add(bill);
                // 更新银行账户利率设置数据
                try {
                    updateInterestRateSetting(bill, type);
                } catch (Exception e) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101612"),e.getMessage());
                }
            }
        }

    }

    /**
     * 更新银行账户利率设置数据*
     *
     * @param entity
     * @param type
     * @throws Exception
     */
    private void updateInterestRateSetting(WithholdingRuleSetting entity, Short type) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accountNumberId").eq(entity.getId()));
        querySchema.addCondition(group);
        List<InterestRateSetting> interestRateSettinglist = MetaDaoHelper.queryObject(InterestRateSetting.ENTITY_NAME, querySchema, null);
        if (interestRateSettinglist.size() > 0) {
            InterestRateSetting interestRateSetting = interestRateSettinglist.get(0);
            //账户同步的时候 需要同时更新银行账号id和银行类别id
            interestRateSetting.setBankAccount(entity.getBankaccount());
            interestRateSetting.setBankType(entity.getBankType());
            interestRateSetting.setRuleStatus(type);
            interestRateSetting.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(InterestRateSetting.ENTITY_NAME, interestRateSetting);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101613"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180582","银行账户利率设置更新失败!") /* "银行账户利率设置更新失败!" */));
        }

    }
    public void checkStatusRepeat(List<WithholdingRuleSetting> accentitylist) throws Exception {
        List<String> currencyList = new ArrayList<>();
        List<String> accountList = new ArrayList<>();
        List<String> currencyaccountList = new ArrayList<>();
        for (WithholdingRuleSetting accentity : accentitylist) {
            if (currencyaccountList.contains(accentity.getCurrency()+accentity.getBankaccount())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101614"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C938D8E04380008","银行账户【{0}】币种【{1}】启用状态的预提规则记录只允许存在一个，操作失败!") /* "银行账户利率设置更新失败!" */,accentity.get("bankaccount_name").toString(),accentity.get("currency_name").toString() ));
            } else {
                currencyaccountList.add(accentity.getCurrency() + accentity.getBankaccount());
            }
            currencyList.add(accentity.getCurrency());
            accountList.add(accentity.getBankaccount());
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("accentity,bankaccount.name as bankaccount_name,currency.name as currency_name");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("currency").in(currencyList));
        queryConditionGroup.addCondition(QueryCondition.name("bankaccount").in(accountList));
        queryConditionGroup.addCondition(QueryCondition.name("ruleStatus").eq(1));
        querySchema.addCondition(queryConditionGroup);
        List<Map<String, Object>> withholdingRuleSettingList = MetaDaoHelper.query(WithholdingRuleSetting.ENTITY_NAME, querySchema);
        if (withholdingRuleSettingList.size() > 0) {
            Map<String, Object> withholdingRuleSetting = withholdingRuleSettingList.get(0);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101614"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C938D8E04380008","银行账户【{0}】币种【{1}】启用状态的预提规则记录只允许存在一个，操作失败!") /* "银行账户利率设置更新失败!" */,withholdingRuleSetting.get("bankaccount_name"),withholdingRuleSetting.get("currency_name")));
        }
    }
}
