package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.iuap.ruleengine.dto.relevant.RuleItemDto;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationPullService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.AbstractIntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.BankMatchAndProcessUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Service
public class IdentificateCustomerServiceImpl extends AbstractIntelligentIdentificationService implements IntelligentIdentificationService {

    @Resource
    private BankreconciliationPullService bankreconciliationPullService;
    @Autowired
    BankIdentifyService bankIdentifyService;

    private static final Cache<String, BillSmartClassifyBO> periodCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(60))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    private static final String[] expenditureList = {"system04102","system04103","system04104","system04105"};
    private static final String[] incomeList = {"system04209","system04210","system04211","system04211"};
    @Override
    public short getTypeValue() {
        return OppositeType.Customer.getValue();
    }

    @Override
    public List<BankReconciliation> excuteIdentificate(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            if (bankReconciliation.getOppositetype() != null || StringUtils.isEmpty(bankReconciliation.getAccentity())) {
                continue;
            }
            String accentity = bankReconciliation.getAccentity();
            String toaccountno = bankReconciliation.getTo_acct_no();
            String toaccountname = bankReconciliation.getTo_acct_name();
            String currency = bankReconciliation.getCurrency();
            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s_%s", accentity, toaccountno, toaccountname, currency, flag);

            BillSmartClassifyBO classifyBO = null;
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER.getDesc(),context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            boolean mark = true;
            if (cacheClassifyBO != null) {
                classifyBO = cacheClassifyBO;
                mark=false;
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT_CACHE.getDesc(),context);
            } else {
                // 查询客户
                CtmJSONObject customerJson = getCustomer(accentity, toaccountno, toaccountname);
                if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
                    classifyBO = new BillSmartClassifyBO(OppositeType.Customer.getValue(), customerJson.getString(MerchantConstant.CUSTOMERID),
                            customerJson.get(ICmpConstant.NAME) != null ? customerJson.get(ICmpConstant.NAME).toString() : null, customerJson.getString(MerchantConstant.CUSTOMERBANKID));
                    //放入缓存
                    periodCache.put(key, classifyBO);
                    mark=false;
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT.getDesc(),context);
                    // 匹配到多个时，按照【其他】处理，不再进行匹配
                } else if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.OTHERFLAG))) {
                    classifyBO = new BillSmartClassifyBO(OppositeType.Other.getValue(), null, null);
                    //放入缓存
                    periodCache.put(key, classifyBO);
                    mark=false;
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_OTHER_UNIT.getDesc(),context);
                } else {
                    //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配
                    boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
                    if (toaccountname != null && !isAccuratematching) {
                        ArrayList<String> orgList = new ArrayList<String>();
                        orgList.add(accentity);
                        orgList.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
                        List<MerchantDTO> customerMap = QueryBaseDocUtils.queryMerchantByNameAndOrg(toaccountname, orgList);
                        if (customerMap != null && customerMap.size() > 0) {
                            for (MerchantDTO merchantDTO : customerMap) {
                                // 停用状态null 或者 停用状态为true
                                if (merchantDTO == null || null == merchantDTO.getDetailStopStatus() || merchantDTO.getDetailStopStatus()) {
                                    continue;
                                }
                                //放入缓存
                                mark=false;
                                classifyBO = new BillSmartClassifyBO(OppositeType.Customer.getValue(), merchantDTO.getId().toString(), merchantDTO.getName());
                                periodCache.put(key, classifyBO);
                                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT_NAME.getDesc(),context);
                                break;
                            }
                        }
                    }
                }
            }
            if (mark) {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT_NO.getDesc(),context);
            }
            setOppoSiteType(bankReconciliation, classifyBO);
        }
        return noMatchList;
    }

    @Override
    public List<BankReconciliation> excuteIdentificateForCheck(List<BankReconciliation> bankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, BankIdentifyService bankIdentifyService, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            if (BankMatchAndProcessUtils.identifiedInformation.equals(bankReconciliation.getString(BankMatchAndProcessUtils.identifiedInformation))) {
                continue;
            }
            //组装日志对象
            TargetRuleInfoDto ruleInfoDto = new TargetRuleInfoDto();
            List<RuleItemDto> sources = new ArrayList<>();
            ruleInfoDto.setSources(sources);
            ruleInfoDto.setCode(bankreconciliationIdentifySetting.getCode());
            CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation,context,context.getLogName());
            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());

            String accentity = bankReconciliation.getAccentity();
            String toaccountno = bankReconciliation.getTo_acct_no();
            String toaccountname = bankReconciliation.getTo_acct_name();
            String currency = bankReconciliation.getCurrency();
            String customername = bankReconciliation.getTo_acct_name();

            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s_%s", accentity, toaccountno, toaccountname, currency, flag);
            BillSmartClassifyBO classifyBO = null;
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER.getDesc(),context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            boolean mark = true;
            if (cacheClassifyBO != null) {
                classifyBO = cacheClassifyBO;
                mark=false;
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT.getDesc()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540069F", "缓存中获取配置") /* "缓存中获取配置" */,null);
            }else {
                toaccountno = null;
                toaccountname = null;
                boolean customernamemark = false;
                if (bankreconciliationIdentifySetting != null ){
                    Map<String, BankreconciliationIdentifySetting_b> bankreconciliationIdentifySettingBMap = bankIdentifyService.loadBankreconciliationIdentifySetting_bById(bankreconciliationIdentifySetting.getId().toString());
                    if (bankreconciliationIdentifySettingBMap != null){
                        Set<String> strings = bankreconciliationIdentifySettingBMap.keySet();
                        for (String orderKey : strings) {
                            if ("to_acct_no".equals(orderKey)){
                                toaccountno = bankReconciliation.getTo_acct_no();
                            }
                            if ("to_acct_name".equals(orderKey) && !"name".equals(bankreconciliationIdentifySettingBMap.get(orderKey).getMatchfield())){
                                toaccountname = bankReconciliation.getTo_acct_name();
                            }
                            if ("to_acct_name".equals(orderKey) && "name".equals(bankreconciliationIdentifySettingBMap.get(orderKey).getMatchfield())){
                                toaccountname = bankReconciliation.getTo_acct_name();
                                customernamemark = true;
                            }
                        }
                    }
                }
                if (customernamemark){
                    if (toaccountname != null) {
                        ArrayList<String> orgList = new ArrayList<String>();
                        orgList.add(accentity);
                        orgList.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
                        List<MerchantDTO> customerMap = QueryBaseDocUtils.queryMerchantByNameAndOrg(toaccountname, orgList);
                        if (customerMap != null && customerMap.size() > 0) {
                            for (MerchantDTO merchantDTO : customerMap) {
                                // 停用状态null 或者 停用状态为true
                                if (merchantDTO == null || null == merchantDTO.getDetailStopStatus() || merchantDTO.getDetailStopStatus()) {
                                    continue;
                                }
                                //放入缓存
                                mark=false;
                                classifyBO = new BillSmartClassifyBO(OppositeType.Customer.getValue(), merchantDTO.getId().toString(), merchantDTO.getName());
                                periodCache.put(key, classifyBO);
                                //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());
                                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT.getDesc()+bankreconciliationIdentifySetting.getCode(),null);
                                break;
                            }
                        }
                        //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());
                        //CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT_NO+bankreconciliationIdentifySetting.getCode(),null);
                    }
                }else {
                    // 查询客户
                    CtmJSONObject customerJson = getCustomerForCheck(accentity, toaccountno, toaccountname);
                    if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
                        mark = false;
                        classifyBO = new BillSmartClassifyBO(OppositeType.Customer.getValue(), customerJson.getString(MerchantConstant.CUSTOMERID),
                                customerJson.get(ICmpConstant.NAME) != null ? customerJson.get(ICmpConstant.NAME).toString() : null, customerJson.getString(MerchantConstant.CUSTOMERBANKID));
                        //放入缓存
                        periodCache.put(key, classifyBO);
                        //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());
                        CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT.getDesc()+bankreconciliationIdentifySetting.getCode(),null);
                    }
                    //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());
                   // CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog,RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT_NO+bankreconciliationIdentifySetting.getCode(),null);
                }

            }
            //设置辨识匹配成功标识
            bankReconciliation.put(BankMatchAndProcessUtils.identifiedInformation,BankMatchAndProcessUtils.identifiedInformation);
            if (mark) {
                //移除辨识匹配标识
                bankReconciliation.remove(BankMatchAndProcessUtils.identifiedInformation);
                noMatchList.add(bankReconciliation);
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_CUSTOMER_RESULT_NO.getDesc()+bankreconciliationIdentifySetting.getCode(),null);
            }
            setOppoSiteType(bankReconciliation, classifyBO);
        }


        return noMatchList;
    }





    /**
     * 获取客户
     * 1 按照银行账号和账户名称查询
     * 2 按照银行账号查询
     * 3 按照账户名称查询
     *
     * @param accentity     会计主体
     * @param toaccountno   银行账号
     * @param toaccountname 账户名称
     * @return
     * @throws Exception
     */
    private CtmJSONObject getCustomer(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject customerJson = bankreconciliationPullService.getCustomer(accentity, toaccountno, toaccountname);
        //大北农需求（20251223），设置yms 参数，如果是精准匹配=true，根据账号+名称没有匹配到供应商或者客户信息，不用拿账号、名称再次匹配匹配
        Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
        if (Objects.equals(MerchantConstant.FALSE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (isAccuratematching) {
                return customerJson;
            }
            customerJson = bankreconciliationPullService.getCustomer(accentity, toaccountno);
        }
        if (Objects.equals(MerchantConstant.FALSE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))  && !isAccuratematching.booleanValue() ) {
            customerJson = bankreconciliationPullService.getCustomerByName(accentity, toaccountname);
        }
        return customerJson;
    }


    private CtmJSONObject getCustomerForCheck(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject customerJson =  new CtmJSONObject();
        customerJson.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        String mark = "";
        if (StringUtils.isNotEmpty(toaccountno) && StringUtils.isNotEmpty(toaccountname)){
            mark = "1";
            customerJson = bankreconciliationPullService.getCustomerForCheck(accentity, toaccountno, toaccountname,mark);
            return customerJson;
        }
        if (StringUtils.isNotEmpty(toaccountno) && StringUtils.isEmpty(toaccountname)){
            mark = "2";
            customerJson = bankreconciliationPullService.getCustomerForCheck(accentity, toaccountno, toaccountname,mark);
            return customerJson;
        }
        if (StringUtils.isEmpty(toaccountno) && StringUtils.isNotEmpty(toaccountname)){
            mark = "3";
            customerJson = bankreconciliationPullService.getCustomerForCheck(accentity, toaccountno, toaccountname,mark);
            return customerJson;
        }
        return customerJson;
    }
}
