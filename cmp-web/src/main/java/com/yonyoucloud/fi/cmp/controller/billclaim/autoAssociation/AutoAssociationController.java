package com.yonyoucloud.fi.cmp.controller.billclaim.autoAssociation;


import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.autocorrsetting.AutoAssociationTaskService;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrDataProcessingService;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ManualCorrServiceImpl;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 到账认领 - 业务关联
 *
 * @author msc
 */
@Controller
@Slf4j
@RequestMapping("/corr")
public class AutoAssociationController extends BaseController {

    /**
     * 对账单关联资金结算明细标识
     */
    private final String fundsettlement = "cmp_bankreconciliationlist_fundsettlement";
    //融资还本单
    private final String tlm_repayment = "cmp_bankreconciliationlist_tlm_repayment";
    //融资付息单
    private final String tlm_payinterest = "cmp_bankreconciliationlist_tlm_payinterest";
    //融资付费单
    private final String tlm_financepayfee = "cmp_bankreconciliationlist_tlm_financepayfee";
    //融入登记单
    private final String tlm_financeinStatement = "cmp_bankreconciliationlist_tlm_financeinStatement";
    //投资收息单
    private final String tlm_interestcollection = "cmp_bankreconciliationlist_tlm_interestcollection";
    //投资管理-申购登记单
    private final String tlm_purchaseregister = "cmp_bankreconciliationlist_tlm_purchasepay";
    //投资赎回单
    private final String tlm_investredem = "cmp_bankreconciliationlist_tlm_investredem";
    //投资付费单
    private final String tlm_investpayment = "cmp_bankreconciliationlist_tlm_investpayment";
    //投资分红单
    private final String tlm_investprofitsharing = "cmp_bankreconciliationlist_tlm_investprofitsharing";
    //衍生品交易交割单
    private final String tlm_tradedelivery = "cmp_bankreconciliationlist_tlm_tradedelivery";
    //衍生品交易平仓单
    private final String tlm_derivativesclose = "cmp_bankreconciliationlist_tlm_derivativesclose";
    //衍生品交易展期登记单
    private final String tlm_traderolloverregister = "cmp_bankreconciliationlist_tlm_traderolloverregister";
    //衍生品交易保证金登记单
    private final String tlm_addbond = "cmp_bankreconciliationlist_tlm_addbond";


    @Autowired
    CorrDataProcessingService autoCorrSettingService;

    @Autowired
    CorrOperationService corrOperationService;//写入关联关系

    @Autowired
    ManualCorrServiceImpl manualCorrService;

    @Autowired
    AutoAssociationTaskService autoAssociationTaskService;

    /**
     * 自动关联接口
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/auto")
    @CMPDiworkPermission({IServicecodeConstant.BILLCLAIMCARD, IServicecodeConstant.CMPBANKRECONCILIATION})
    public void autoCorr(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> retMap = new HashMap<>();
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        String lockKey = BankReconciliation.ENTITY_NAME + "AutoAssociationController";
        //匹配业务单据 - 整理数据格式
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 2, 200, "AutoAssociation-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8006C", "自动关联任务正在执行中") /* "自动关联任务正在执行中" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    autoAssociationTaskService.runAutoAssociationTask(paramMap);
                });
                AppContext.cache().del("autoAssociationTask-" + AppContext.getYTenantId());
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                log.error("自动关联任务异常：", e);
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            } finally {
                if (autoAssociatedDataExecutor != null) {
                    autoAssociatedDataExecutor.shutdown();
                }
            }
        });
        retMap.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(retMap));
    }

    /**
     * 手动关联接口
     *
     * @param paramMap :: bid busid isClaim isPayment
     * @param response
     */
    @PostMapping("/manual")
    @CMPDiworkPermission({IServicecodeConstant.BILLCLAIMCARD, IServicecodeConstant.CMPBANKRECONCILIATION})
    public void autoManual(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletResponse response) {
        //主表id
        Long bid = paramMap.getLong("bid");
        //子表id
        Long busid = paramMap.getLong("busid");
        String isClaim = paramMap.getString("isClaim");
        String billType = paramMap.getString("billType");
        Date pubts = paramMap.getDate("pubtsbiz");
        Date pubtsm = paramMap.getDate("pubtsbizm");
        //借贷方向
        Short dcFlag = paramMap.containsKey("dcFlag") ? paramMap.getShort("dcFlag") : null;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(busid.toString());
        try {
            if (ymsLock == null) {
                renderJson(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8006D", "当前认领单或对账单正在处理中，请稍后重试！") /* "当前认领单或对账单正在处理中，请稍后重试！" */);
                return;
            }
            if (AppContext.cache().exists(ICmpConstant.ASSOCIATION_TASK)) {
                renderJson(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8006E", "自动关联任务正在执行中，无法执行手动关联，请稍候重试！") /* "自动关联任务正在执行中，无法执行手动关联，请稍候重试！" */);
                return;
            }
            List<CorrDataEntity> corrDataEntities;
            if (billType.equals(fundsettlement) || billType.equals(tlm_repayment) || billType.equals(tlm_payinterest)
                    || billType.equals(tlm_financepayfee) || billType.equals(tlm_financeinStatement)
                    || billType.equals(tlm_interestcollection) || billType.equals(tlm_purchaseregister)
                    || billType.equals(tlm_investredem) || billType.equals(tlm_investpayment) || billType.equals(tlm_investprofitsharing)
                    || billType.equals(tlm_tradedelivery) || billType.equals(tlm_derivativesclose)
                    || billType.equals(tlm_traderolloverregister) || billType.equals(tlm_addbond)
            ) {
                //处理资金结算单的数据
                corrDataEntities = autoCorrSettingService.setAssociatedData(paramMap);
            } else {
                //处理资金收付款单、转账单和外币兑换单的数据
                corrDataEntities = autoCorrSettingService.manualAssociatedData(isClaim, billType, bid, busid, pubts, pubtsm, dcFlag);
            }
            manualCorrService.manualCorrBill(corrDataEntities, paramMap);
            renderJson(response, ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418060B", "关联成功") /* "关联成功" *//* 关联成功*/));
        } catch (Exception e) {
            log.error("手动关联出错:" + e, e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * 确认接口
     *
     * @param paramMap
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/sure")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION_ISSURE)
    public void confirmCorr(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> mapres = new HashMap<String, Object>();
        List corrIds = (List) paramMap.get("corrId");
        List dcFlags = (List) paramMap.get("dc_flag");
        int succ = 0;
        int def = 0;
        mapres = corrOperationService.confirmCorrOpration(corrIds, dcFlags);
        renderJson(response, ResultMessage.data(mapres));
    }

    @PostMapping("/refuse")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION_ISSURE)
    public void refuseCorr(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> mapres = new HashMap<String, Object>();
        List<Long> corrIds = (List) paramMap.get("corrId");
        int succ = 0;
        int def = 0;
        mapres = corrOperationService.refuseCorrOpration(corrIds);
        renderJson(response, ResultMessage.data(mapres));
    }

    @PostMapping("/singleSure")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION_ISSURE)
    public void singleConfirmCorr(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> mapres = new HashMap<String, Object>();
        List corrIds = (List) paramMap.get("corrId");
        List dcFlags = (List) paramMap.get("dc_flag");
        int num = corrOperationService.confirmUseExeception(Long.parseLong(corrIds.get(0).toString()), Long.parseLong(dcFlags.get(0).toString()), false);
        if (num == 1) {
            mapres.put("succ", 0);
            mapres.put("def", 1);
        } else {
            mapres.put("succ", 1);
            mapres.put("def", 0);
        }
        renderJson(response, ResultMessage.data(mapres));
    }

    @PostMapping("/progess")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION_ISSURE)
    public void progess(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        corrOperationService.progess(paramMap);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 初始化自动关联设置
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("/initAutoCorrSetting")
    @CMPDiworkPermission({IServicecodeConstant.BILLCLAIMCARD, IServicecodeConstant.CMPBANKRECONCILIATION})
    public void initAutoCorrSetting(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        renderJson(response, ResultMessage.data(autoCorrSettingService.initAutoCorrSetting(tenant)));
    }

    /**
     * 账户交易流水与货币兑换自动关联
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/autoCorrCurrencyExchange")
    @CMPDiworkPermission({IServicecodeConstant.BILLCLAIMCARD, IServicecodeConstant.CMPBANKRECONCILIATION})
    public void autoCorrCurrencyExchange(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> retMap = new HashMap<>();
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        String lockKey = BankReconciliation.ENTITY_NAME + "autoCorrCurrencyExchange";
        //匹配业务单据 - 整理数据格式
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 2, 200, "AutoAssociation-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8006C", "自动关联任务正在执行中") /* "自动关联任务正在执行中" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    autoAssociationTaskService.runAutoCorrCurrencyExchangeTask(paramMap);
                });
                AppContext.cache().del("autoCorrCurrencyExchangeTask-" + AppContext.getYTenantId());
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                log.error("账户交易流水与货币兑换自动关联任务异常：", e);
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            } finally {
                if (autoAssociatedDataExecutor != null) {
                    autoAssociatedDataExecutor.shutdown();
                }
            }
        });
        retMap.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(retMap));
    }
}
