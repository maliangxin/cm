package com.yonyoucloud.fi.cmp.autocorrsetting;


import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.spring.support.cache.RedisManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自动关联定时任务 - 事务抽取类
 */
@Service
@Slf4j
public class AutoAssociationTaskService {

    @Autowired
    CorrDataProcessingService autoCorrSettingService;
    @Autowired
    AutoCorrServiceImpl autoCorrService;
    @Autowired
    CorrDataProcessingServiceImpl corrDataProcessingService;
    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Transactional
    public void runAutoAssociationTask(Map<String, Object> paramMap) throws Exception {
        CtmJSONObject logData = new CtmJSONObject();
        try {
            if(paramMap.get("dataRange") != null && StringUtils.isNotEmpty(paramMap.get("dataRange").toString()) && ((paramMap.get("startdate") != null && StringUtils.isNotEmpty(paramMap.get("startdate").toString())) || (paramMap.get("enddate") != null && StringUtils.isNotEmpty(paramMap.get("enddate").toString())))){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400761", "开始日期、结束日期不允许与日期范围同时有值") /* "开始日期、结束日期不允许与日期范围同时有值" */);
            }
            // 查询银行对账单是否存在
            List<BankReconciliation> bankReconciliationList = corrDataProcessingService.getBankReconciliationList(paramMap);
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                log.error("没有需要关联的流水！");
                logData.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004C", "没有需要关联的流水！") /* "没有需要关联的流水！" */);
                return;
            } else {
                logData.put("data", bankReconciliationList);
            }
            List<CorrDataEntity> corrDataEntities = autoCorrSettingService.autoAssociatedData(bankReconciliationList);
            BankreconciliationUtils.checkAndFilterData(bankReconciliationList, BankreconciliationScheduleEnum.BANSMAUTOASSOCITE);
            List<String> bankReconciliationIdList = bankReconciliationList.stream().map(BankReconciliation::getId).map(Object::toString).collect(Collectors.toList());
            corrDataEntities = corrDataEntities.stream().filter(item -> bankReconciliationIdList.contains(item.getBankReconciliationId().toString())).collect(Collectors.toList());
            AppContext.cache().setObject("autoAssociationTask-" + AppContext.getYTenantId(), bankReconciliationIdList,60 * 60 * 2);
            autoCorrService.autoCorrBill(corrDataEntities);
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004D", "自动关联失败，失败原因：") /* "自动关联失败，失败原因：" */ + e.getMessage(), e);
        } finally {
            try {
                ctmcmpBusinessLogService.saveBusinessLog(logData, "autoAssociationTask", IServicecodeConstant.BANKRECONCILIATION_ISSURE, IServicecodeConstant.BANKRECONCILIATION_ISSURE, IMsgConstant.BANKRECONCILIATION);
            } catch (Exception e) {
                log.error("记录业务日志失败，失败原因" + e.getMessage(), e);
            }
        }
    }

    /**
     * 银行流水批量自动关联货币兑换单
     *
     * @param paramMap
     * @throws Exception
     */
    @Transactional
    public void runAutoCorrCurrencyExchangeTask(Map<String, Object> paramMap) throws Exception {
        CtmJSONObject logData = new CtmJSONObject();
        try {
            //根据调度任务入参查询符合条件的银行流水
            List<BankReconciliation> bankReconciliationList = corrDataProcessingService.getBankReconciliationList(paramMap);
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                log.error("没有需要关联的流水！");
                logData.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004C", "没有需要关联的流水！") /* "没有需要关联的流水！" */);
                return;
            } else {
                logData.put("data", bankReconciliationList);
            }
            List<CorrDataEntity> corrDataEntities = autoCorrSettingService.autoCorrCurrencyExchangeData(bankReconciliationList,paramMap);
            BankreconciliationUtils.checkAndFilterData(bankReconciliationList, BankreconciliationScheduleEnum.BANSMAUTOASSOCITE);
            List<String> bankReconciliationIdList = bankReconciliationList.stream().map(BankReconciliation::getId).map(Object::toString).collect(Collectors.toList());
            corrDataEntities = corrDataEntities.stream().filter(item -> bankReconciliationIdList.contains(item.getBankReconciliationId().toString())).collect(Collectors.toList());
            AppContext.cache().setObject("autoCorrCurrencyExchangeTask-" + AppContext.getYTenantId(), bankReconciliationIdList,60 * 60 * 2);
            autoCorrService.autoCorrBill(corrDataEntities);
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21FE6F400570000B", "银行流水自动关联货币兑换失败，失败原因：") /* "银行流水自动关联货币兑换失败，失败原因：" */ + e.getMessage(), e);
        } finally {
            try {
                ctmcmpBusinessLogService.saveBusinessLog(logData, "autoCorrCurrencyExchangeTask", IServicecodeConstant.BANKRECONCILIATION_ISSURE, IServicecodeConstant.BANKRECONCILIATION_ISSURE, IMsgConstant.BANKRECONCILIATION);
            } catch (Exception e) {
                log.error("记录业务日志失败，失败原因" + e.getMessage(), e);
            }
        }
    }

}
