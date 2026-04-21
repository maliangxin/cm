package com.yonyoucloud.fi.cmp.util;

import com.alibaba.fastjson.JSON;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author: liaojbo
 * @Date: 2025年07月04日 21:08
 * @Description:
 */
@lombok.extern.slf4j.Slf4j
@Slf4j
public class LogUtil {

    private static CTMCMPBusinessLogService getCTMCMPBusinessLogService() {
        return AppContext.getBean(CTMCMPBusinessLogService.class);
    }

    public static void saveErrorBusinessLog(String data, String name, String code) throws Exception {
        try {
            saveBusinessLog(OperCodeTypes.invalid, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040B", "非阻断错误") /* "非阻断错误" */, name, code, data);
        } catch (Exception e) {
            log.error("saveErrorBusinessLog error:" + e.getMessage(), e);
        }

    }
    public static void saveBankelereceiptSendFileEventlogByDto(BankReconciliation bankReconciliation, String urlId, int callInt) throws Exception {
        try {
            
            //记录业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            CtmJSONObject userObject = new CtmJSONObject();
            Object bankReconciliationId = "null";
            if (bankReconciliation != null) {
                bankReconciliationId = bankReconciliation.getId();
            }
            userObject.put("bankReconciliationId", bankReconciliationId);
            userObject.put("bankReconciliation", bankReconciliation);
            userObject.put("urlId", urlId);
            userObject.put("callInt", callInt);
            logparam.put("userObject", userObject);
            //getCTMCMPBusinessLogService().saveBusinessLog(logparam, bankReconciliationId.toString(), "银行流水关联银行回单事件发送", IServicecodeConstant.BANKRECONCILIATION, "银行流水关联银行回单", "发送银行对账单关联交易回单文件id事件");
            log.error("saveBankelereceiptSendFileEventlogByDto info:" + userObject.toString());
        } catch (Exception e) {
            log.error("saveBankelereceiptSendFileEventlogByDto error:" + e.getMessage(), e);
        }

    }

    public static void saveBankelereceiptSendFileEventlogById(String bankReconciliationId, String urlId, int callInt) throws Exception {
        try {
            
            //记录业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            CtmJSONObject userObject = new CtmJSONObject();
            userObject.put("bankReconciliationId", bankReconciliationId);
            userObject.put("urlId", urlId);
            userObject.put("callInt", callInt);
            logparam.put("userObject", userObject);
            //getCTMCMPBusinessLogService().saveBusinessLog(logparam, bankReconciliationId, "银行流水关联银行回单事件发送", IServicecodeConstant.BANKRECONCILIATION, "银行流水关联银行回单", "发送银行对账单关联交易回单文件id事件");
        } catch (Exception e) {
            log.error("saveBankelereceiptSendFileEventlogById error:" + e.getMessage(), e);

        }

    }

    public static void saveBusinessLog(OperCodeTypes operCodeTypes, String type, String code, String name, String dataStr) {
        CtmJSONObject logData = new CtmJSONObject();
        logData.put("logData", dataStr);
        saveBusinessLog(operCodeTypes, type, code, name, logData);
    }

    public static void saveBusinessLog(OperCodeTypes operCodeTypes, String type, String code, String name, CtmJSONObject data) {
        try {
            BusinessObject businessObject = new BusinessObject();
            businessObject.setOperationDate(new Date());
            businessObject.setOperCodeTypes(operCodeTypes);
            businessObject.setServiceCode(AppContext.getThreadContext("serviceCode"));
            businessObject.setOperatorName(AppContext.getCurrentUser().getName());
            //类型
            businessObject.setBusiObjTypeName(type);
            CtmJSONObject logData = new CtmJSONObject();
            //编码
            businessObject.setBusiObjCodeFieldName("code");
            logData.put("code", code);
            //名称
            businessObject.setBusiObjNameFieldName("name");
            logData.put("name", name);
            //logData.put(IMsgConstant.BILL_DATA, params);
            logData.put("data", data);
            //日志内容
            businessObject.setNewObject(JSON.parseObject(logData.toString()));
            IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
            businessLogService.saveBusinessLog(businessObject);
        } catch (Exception e) {
            log.error("记录业务日志失败：" + e.getMessage(), e);
        }
    }


    public static void saveAccountBussinessLog(CtmJSONObject params, Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup) {
        try {
            CtmJSONObject logData = new CtmJSONObject();
            String serviceCode = Objects.toString(InvocationInfoProxy.getExtendAttribute("serviceCode"));
            String serviceName = IServicecodeConstant.SERVICECODE_MAP.get(serviceCode);
            logData.put(serviceName + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040C", "查询账户请求") /* "查询账户请求" */, params);
            logData.put(serviceName + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040D", "查询账户返回") /* "查询账户返回" */, bankAccountsGroup);
            getCTMCMPBusinessLogService().saveBusinessLog(logData, "", "", serviceCode, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040E", "账户交易流水") /* "账户交易流水" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040E", "账户交易流水") /* "账户交易流水" */);
        } catch (Exception e) {
            log.error("记录业务日志失败：" + e.getMessage(), e);
        }
    }
}
