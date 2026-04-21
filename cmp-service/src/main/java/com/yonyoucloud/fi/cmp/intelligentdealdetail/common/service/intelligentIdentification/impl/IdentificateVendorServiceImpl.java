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
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Service
public class IdentificateVendorServiceImpl extends AbstractIntelligentIdentificationService implements IntelligentIdentificationService {

    @Resource
    private BankreconciliationPullService bankreconciliationPullService;

    @Resource
    private VendorQueryService vendorQueryService;

    private static final Cache<String, BillSmartClassifyBO> periodCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(60))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();
    @Override
    public short getTypeValue() {
        return OppositeType.Supplier.getValue();
    }

    @Override
    public List<BankReconciliation> excuteIdentificate(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception{
        List<BankReconciliation> noMatchList = new ArrayList<>();
        for(BankReconciliation bankReconciliation:bankReconciliationList){
//            if(bankReconciliation.getOppositetype() != null){
//                continue;
//            }
            //供应商，包含核算委托关系相关
            String accentity = bankReconciliation.getAccentity();
            String toaccountno = bankReconciliation.getTo_acct_no();
            String toaccountname = bankReconciliation.getTo_acct_name();
            String currency = bankReconciliation.getCurrency();
            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s_%s", accentity, toaccountno, toaccountname, currency, flag);

            BillSmartClassifyBO classifyBO = null;
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_UNIT.getDesc(),context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            if(cacheClassifyBO != null){
                classifyBO = cacheClassifyBO;
            }else{
                CtmJSONObject supplierJson = getSupplier(accentity, toaccountno, toaccountname);
                if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
                    classifyBO = new BillSmartClassifyBO(OppositeType.Supplier.getValue(), supplierJson.getString("supplierId"),
                            supplierJson.get(ICmpConstant.NAME).toString(), supplierJson.get(MerchantConstant.SUPPLIERBANKID) != null ? supplierJson.getString(MerchantConstant.SUPPLIERBANKID) : null);
                    //放入缓存
                    periodCache.put(key, classifyBO);
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc(),context);
                // 匹配到多个时，按照【其他】处理，不再进行匹配
                } else if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.OTHERFLAG))) {
                    classifyBO = new BillSmartClassifyBO(OppositeType.Other.getValue(), null, null);
                    //放入缓存
                    periodCache.put(key, classifyBO);
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_OTHER_UNIT.getDesc(),context);
                } else {
                    //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配
                    Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
                    if (toaccountname != null && !isAccuratematching) {
                        List<VendorVO> vendorFieldByName = vendorQueryService.getVendorFieldByName(toaccountname);
                        if (CollectionUtils.isNotEmpty(vendorFieldByName)) {
                            if (vendorQueryService.judgeVendorOrg(vendorFieldByName.get(0).getId(), accentity)
                                    || vendorQueryService.judgeVendorOrg(vendorFieldByName.get(0).getId(), IStwbConstantForCmp.GLOBAL_ACCENTITY)) {
                                classifyBO = new BillSmartClassifyBO(OppositeType.Supplier.getValue(), vendorFieldByName.get(0).getId().toString(),
                                        vendorFieldByName.get(0).getName());
                                //放入缓存
                                periodCache.put(key, classifyBO);
                                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc(),context);
                            }
                        }
                    }
                }
            }
            if (classifyBO == null ){
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT_NO.getDesc(),context);
            }
            setOppoSiteType(bankReconciliation, classifyBO);
        }
        return noMatchList;
    }

    @Override
    public List<BankReconciliation> excuteIdentificateForCheck(List<BankReconciliation> bankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, BankIdentifyService bankIdentifyService, BankDealDetailContext context) throws Exception {
        List<BankReconciliation> noMatchList = new ArrayList<>();
        for(BankReconciliation bankReconciliation:bankReconciliationList){
            if (BankMatchAndProcessUtils.identifiedInformation.equals(bankReconciliation.getString(BankMatchAndProcessUtils.identifiedInformation))) {
                continue;
            }
            TargetRuleInfoDto ruleInfoDto = new TargetRuleInfoDto();
            List<RuleItemDto> sources = new ArrayList<>();
            ruleInfoDto.setSources(sources);
            ruleInfoDto.setCode(bankreconciliationIdentifySetting.getCode());
            CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation,context,context.getLogName());
            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(),bankreconciliationIdentifySetting.getCode());

            //供应商，包含核算委托关系相关
            String accentity = bankReconciliation.getAccentity();
            String toaccountno = bankReconciliation.getTo_acct_no();
            String toaccountname = bankReconciliation.getTo_acct_name();
            String vendorname = bankReconciliation.getTo_acct_name();
            short flag = bankReconciliation.getDc_flag().getValue();
            String key = String.format("%s_%s_%s_%s", accentity, toaccountno, toaccountname, flag);

            BillSmartClassifyBO classifyBO = null;
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_UNIT.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_UNIT.getDesc(),context);
            BillSmartClassifyBO cacheClassifyBO = periodCache.getIfPresent(key);
            if(cacheClassifyBO != null){
                classifyBO = cacheClassifyBO;
                //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400396", "缓存中获取配置") /* "缓存中获取配置" */, null);
            }else{
                toaccountno = null;
                toaccountname = null;
                boolean vendornamemark = false;
                if (bankreconciliationIdentifySetting != null ) {
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
                                vendornamemark = true;
                            }
                        }
                    }
                }
                if (vendornamemark) {
                    if (toaccountname != null) {
                        List<VendorVO> vendorFieldByName = vendorQueryService.getVendorFieldByName(toaccountname);
                        if (CollectionUtils.isNotEmpty(vendorFieldByName)) {
                            if (vendorQueryService.judgeVendorOrg(vendorFieldByName.get(0).getId(), accentity)
                                    || vendorQueryService.judgeVendorOrg(vendorFieldByName.get(0).getId(), IStwbConstantForCmp.GLOBAL_ACCENTITY)) {
                                classifyBO = new BillSmartClassifyBO(OppositeType.Supplier.getValue(), vendorFieldByName.get(0).getId().toString(),
                                        vendorFieldByName.get(0).getName());
                                //放入缓存
                                periodCache.put(key, classifyBO);
                                //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc(),context);
                                //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc() + bankreconciliationIdentifySetting.getCode(), null);
                            }
                        }
                    }
                }else {
                    CtmJSONObject supplierJson = getSupplierForCheck(accentity, toaccountno, toaccountname);
                    if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
                        classifyBO = new BillSmartClassifyBO(OppositeType.Supplier.getValue(), supplierJson.getString("supplierId"),
                                supplierJson.get(ICmpConstant.NAME).toString(), supplierJson.get(MerchantConstant.SUPPLIERBANKID) != null ? supplierJson.getString(MerchantConstant.SUPPLIERBANKID) : null);
                        //放入缓存
                        periodCache.put(key, classifyBO);
                        //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc(),context);
                        //CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                        CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT.getDesc() + bankreconciliationIdentifySetting.getCode(), null);
                    }
                }
            }
            //设置辨识匹配成功标识
            bankReconciliation.put(BankMatchAndProcessUtils.identifiedInformation,BankMatchAndProcessUtils.identifiedInformation);
            if (classifyBO == null ){
                //移除辨识匹配标识
                bankReconciliation.remove(BankMatchAndProcessUtils.identifiedInformation);
                noMatchList.add(bankReconciliation);
                //CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT_NO.getDesc(),context);
               // CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleTargets(cmpRuleCheckLog, ruleInfoDto, sources, context.getLogName(), bankreconciliationIdentifySetting.getCode());
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_VENDOR_RESULT_NO.getDesc() + bankreconciliationIdentifySetting.getCode(), null);
            }
            setOppoSiteType(bankReconciliation, classifyBO);
        }

        return noMatchList;
    }

    /**
     * 获取供应商
     * 1 根据银行账号和账户名称查询供应商
     * 2 根据银行账号查询供应商
     * 3 根据账户名称查询供应商
     *
     * @param accentity     会计主体
     * @param toaccountno   银行账号
     * @param toaccountname 账户名称
     * @return
     */
    private CtmJSONObject getSupplier(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject supplierJson = bankreconciliationPullService.getSupplier(accentity, toaccountno, toaccountname);
        Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
        if (Objects.equals(MerchantConstant.FALSE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准匹配=true，根据账号+名称没有匹配到供应商或者客户信息，不用拿账号、名称再次匹配匹配
            if (isAccuratematching) {
                return supplierJson;
            }
            supplierJson = bankreconciliationPullService.getSupplier(accentity, toaccountno);
        }
        if (Objects.equals(MerchantConstant.FALSE, supplierJson.getString(MerchantConstant.VENDORFLAG)) && !isAccuratematching.booleanValue() ) {
            supplierJson = bankreconciliationPullService.getSupplierByAccName(accentity, toaccountname);
        }
        return supplierJson;
    }

    /**
     * 获取供应商
     * 1 根据银行账号和账户名称查询供应商
     * 2 根据银行账号查询供应商
     * 3 根据账户名称查询供应商
     *
     * @param accentity     会计主体
     * @param toaccountno   银行账号
     * @param toaccountname 账户名称
     * @return
     */
    private CtmJSONObject getSupplierForCheck(String accentity, String toaccountno, String toaccountname) throws Exception {
        CtmJSONObject customerJson =  new CtmJSONObject();
        customerJson.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        String mark = "";
        if (StringUtils.isNotEmpty(toaccountno) && StringUtils.isNotEmpty(toaccountname)){
            mark = "1";
            customerJson = bankreconciliationPullService.getSupplierForCheck(accentity, toaccountno, toaccountname,mark);
            return customerJson;
        }
        if (StringUtils.isNotEmpty(toaccountno) && StringUtils.isEmpty(toaccountname)){
            mark = "2";
            customerJson = bankreconciliationPullService.getSupplierForCheck(accentity, toaccountno, toaccountname,mark);
            return customerJson;
        }
        if (StringUtils.isEmpty(toaccountno) && StringUtils.isNotEmpty(toaccountname)){
            mark = "3";
            customerJson = bankreconciliationPullService.getSupplierForCheck(accentity, toaccountno, toaccountname,mark);
            return customerJson;
        }
        return customerJson;
    }
}
