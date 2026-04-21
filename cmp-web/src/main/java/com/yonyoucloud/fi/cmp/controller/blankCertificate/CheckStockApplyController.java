package com.yonyoucloud.fi.cmp.controller.blankCertificate;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.checkStockApply.service.CheckStockApplyService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;


/**
 * @author yp
 * @version 1.0
 */
@Controller
@RequestMapping("/cm/checkStockApply")
@Authentication(value = false, readCookie = true)
@Slf4j
public class CheckStockApplyController extends BaseController {

    public static final String auditLockControl = "cmp_checkStockApply_audit";
    public static final String unAuditLockControl = "cmp_checkStockApply_unAudit";


    @Autowired
    CheckStockApplyService checkStockApplyService;

    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 支票入库，审批，审批通过后，入库支票工作台
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @RequestMapping("/insertCheckStock")
    @CMPDiworkPermission(IServicecodeConstant.CHECKSTOCKAPPLY)
    @ApplicationPermission("CM")
    public void insertCheckStock(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        YmsLock ymsLock = null;
        try {
            if ((ymsLock = JedisLockUtils.lockBillWithOutTrace(auditLockControl)) == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101041"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DD", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            CtmJSONArray rows = getRowsBYParam(param);
            CtmJSONArray newRow = checkStockApplyService.insertCheckStockService(rows);
            param.put("rows", newRow);
            param.put("count", newRow.size());
            param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180658", "审核成功") /* "审核成功" */);
            renderJson(response, ResultMessage.data(param));
        } catch (Exception e) {
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * 支票入库，弃审
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @RequestMapping("/abandonCheckStock")
    @CMPDiworkPermission(IServicecodeConstant.CHECKSTOCKAPPLY)
    public void abandonCheckStock(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        YmsLock ymsLock = null;
        try {
            if ((ymsLock = JedisLockUtils.lockBillWithOutTrace(unAuditLockControl)) == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101041"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DD", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            CtmJSONArray rows = getRowsBYParam(param);
            CtmJSONArray newRow = checkStockApplyService.abandonCheckStock(rows);
            param.put("rows", newRow);
            param.put("count", newRow.size());
            param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008D", "弃审成功") /* "弃审成功" */);
            renderJson(response, ResultMessage.data(param));
        } catch (Exception e) {
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            JedisLockUtils.unlockGlWithOutTrace(ymsLock);
        }
    }

    /**
     * 获取对象并判空
     *
     * @param param
     * @return
     */
    private CtmJSONArray getRowsBYParam(CtmJSONObject param) {
        if (null == param) {
            return null;
        }
        ;
        return param.getJSONArray(ICmpConstant.DATA);
    }

    /**
     * 支票预占
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @RequestMapping("/checkStockOccupy")
    public void checkStockOccupy(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        checkStockApplyService.checkStockOccupy(param);
        renderJson(response, ResultMessage.success());
    }


}
