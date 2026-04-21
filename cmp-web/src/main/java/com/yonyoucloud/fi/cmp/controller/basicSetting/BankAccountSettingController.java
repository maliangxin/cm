package com.yonyoucloud.fi.cmp.controller.basicSetting;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;


/**
 * @author zhaodzh@yonyou.com
 * @version 1.0
 */
@Controller
@RequestMapping("/bankaccountSetting")
@Authentication(value = false, readCookie = true)
@Slf4j
public class BankAccountSettingController extends BaseController {

    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    BankAccountSettingService bankaccountSettingService;
    private final String BANKACCOUNT = "enterpriseBankAccount_account";
    private final String BANKACCOUNTNAME = "enterpriseBankAccount_name";
    private final String CUSTOMNO = "customNo";

    @RequestMapping("/accountQy")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void settle(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        bankaccountSettingService.accountQyTy(param);
        ctmcmpBusinessLogService.saveBusinessLog(param, param.get(BANKACCOUNT) == null ? "" : param.get(BANKACCOUNT).asText(), param.get(BANKACCOUNTNAME) == null ? "" : param.get(BANKACCOUNTNAME).asText(), IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.OPEN_BANK_ACCOUNT_SETTING);
        renderJson(response, ResultMessage.success());
    }

    @RequestMapping("/accountTy")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void unsettle(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        bankaccountSettingService.accountQyTy(param);
        ctmcmpBusinessLogService.saveBusinessLog(param, param.get(BANKACCOUNT) == null ? "" : param.get(BANKACCOUNT).asText(), param.get(BANKACCOUNTNAME) == null ? "" : param.get(BANKACCOUNTNAME).asText(), IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.CLOSE_BANK_ACCOUNT_SETTING);
        renderJson(response, ResultMessage.success());
    }


    @RequestMapping("/accountQyT")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void settleT(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        bankaccountSettingService.accountQyTyT(param);
        ctmcmpBusinessLogService.saveBusinessLog(param, param.get(BANKACCOUNT) == null ? "" : param.get(BANKACCOUNT).asText(), param.get(BANKACCOUNTNAME) == null ? "" : param.get(BANKACCOUNTNAME).asText(), IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.OPEN_ELECTRONIC_BILL);
        renderJson(response, ResultMessage.success());
    }

    @RequestMapping("/accountTyT")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void unsettleT(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        bankaccountSettingService.accountQyTyT(param);
        ctmcmpBusinessLogService.saveBusinessLog(param, param.get(BANKACCOUNT) == null ? "" : param.get(BANKACCOUNT).asText(), param.get(BANKACCOUNTNAME) == null ? "" : param.get(BANKACCOUNTNAME).asText(), IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.CLOSE_ELECTRONIC_BILL);
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/updateCustomNo")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void updateCustomNo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, bankaccountSettingService.updateCustomNo(param));
    }

    /**
     * 同步客户号
     *
     * @param param
     * @param response
     */
    @PostMapping("/syncAccount")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void syncAccount(@RequestBody JsonNode param, HttpServletResponse response) {
        int count = bankaccountSettingService.syncAccount();
        renderJson(response, String.format(ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C6", "同步账户成功，共同步[%s]条数据！") /* "同步账户成功，共同步[%s]条数据！" */), count));
        ctmcmpBusinessLogService.saveBusinessLog(param, "", "", IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.ACCOUNT_SYNCHRONOUS);
    }

    @PostMapping("/getOpenFlag")
    @CMPDiworkPermission({IServicecodeConstant.TRANSFERACCOUNT, IServicecodeConstant.PAYMENTBILL, IServicecodeConstant.SALARYPAY})
    public void getOpenFlag(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        if (param == null || param.get("enterpriseBankAccount") == null) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080045", "参数错误，账户信息为空")));
            return;
        }
        renderJson(response, bankaccountSettingService.getOpenFlag(param.get("enterpriseBankAccount").asText()));
    }

    /**
     * 银企联账号-直连启用日期设置
     *
     * @param param
     * @return
     * @throws Exception
     */
    @PostMapping("/accountEnableDateSet")
    @CMPDiworkPermission(IServicecodeConstant.BANKACCOUNTSETTING)
    public void accountEnableDateSet(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        renderJson(response, bankaccountSettingService.accountEnableDateSet(param));
    }

    /**
     * 查询银企联直连账号启用日期
     *
     * @param param
     * @return
     * @throws Exception
     */
    @PostMapping("/queryEnableDate")
    @CMPDiworkPermission(IServicecodeConstant.DLLIST)
    public void queryEnableDate(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        renderJson(response, bankaccountSettingService.queryEnableDate(param));
    }

}
