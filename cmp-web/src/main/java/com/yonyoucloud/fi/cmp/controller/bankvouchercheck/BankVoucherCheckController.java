package com.yonyoucloud.fi.cmp.controller.bankvouchercheck;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankvouchercheck.service.BankVoucherCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * @description: 银企对账工作台-对账相关前端接口
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/19 18:52
 */
@Controller
@RequestMapping("/bankvouchercheck")
@Slf4j
public class BankVoucherCheckController extends BaseController {

    @Autowired
    private BankVoucherCheckService bankVoucherCheckService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 银企对账工作台-快速对账功能
     */
    @PostMapping("quickReconciliation")
    @CMPDiworkPermission({IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, IServicecodeConstant.CMP_BANKJOURANLCHECK_WORKBENCH})
    public void quickReconciliation(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject logParams = new CtmJSONObject();
        logParams.put("reconciliationSeqNo", param.getString("reconciliationSeqNo"));
        logParams.put("requestVo", param);
        ctmcmpBusinessLogService.saveBusinessLog(logParams, param.getString("reconciliationSeqNo"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80088", "银企对账工作台-快速对账") /* "银企对账工作台-快速对账" */, IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80086", "银企对账工作台") /* "银企对账工作台" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80089", "快速对账任务执行开始") /* "快速对账任务执行开始" */);
        renderJson(response, ResultMessage.data(bankVoucherCheckService.quickReconciliation(param)));
    }

    /**
     * 银企对账工作台-自动对账功能
     */
    @PostMapping("autoReconciliation")
    @CMPDiworkPermission({IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, IServicecodeConstant.CMP_BANKJOURANLCHECK_WORKBENCH})
    public void autoReconciliation(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject logParams = new CtmJSONObject();
        logParams.put("reconciliationSeqNo", param.getString("reconciliationSeqNo"));
        logParams.put("requestVo", param);
        ctmcmpBusinessLogService.saveBusinessLog(logParams, param.getString("reconciliationSeqNo"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80085", "银企对账工作台-自动对账") /* "银企对账工作台-自动对账" */, IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80086", "银企对账工作台") /* "银企对账工作台" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80087", "自动对账任务执行开始") /* "自动对账任务执行开始" */);
        renderJson(response, ResultMessage.data(bankVoucherCheckService.autoReconciliation(param)));
    }

    /**
     * 银企对账工作台-根据对账批次号获取对账进度
     * param中包含参数 reconciliationSeqNo
     */
    @PostMapping("getReconciliationProcess")
    @CMPDiworkPermission({IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, IServicecodeConstant.CMP_BANKJOURANLCHECK_WORKBENCH})
    public void getReconciliationProcess(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        renderJson(response, ResultMessage.data(bankVoucherCheckService.getReconciliationProgress(param.getString("reconciliationSeqNo"))));
    }

    /**
     * 银企对账工作台-获取对账设置中有效的对账组织合集
     */
    @PostMapping("getAccentityInfoList")
    @CMPDiworkPermission({IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, IServicecodeConstant.CMP_BANKJOURANLCHECK_WORKBENCH})
    public void getAccentityInfoList(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankVoucherCheckService.getAccentityListInfo(ReconciliationDataSource.Voucher.getValue())));
    }
}
