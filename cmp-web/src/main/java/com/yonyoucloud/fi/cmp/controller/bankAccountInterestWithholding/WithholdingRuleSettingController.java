package com.yonyoucloud.fi.cmp.controller.bankAccountInterestWithholding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.WithholdingRuleStatus;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
import com.yonyoucloud.fi.cmp.withholdingrulesetting.service.WithholdingRuleSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 预提规则设置相关接口*
 *
 * @author xuxbo
 * @date 2023/4/19 11:42
 */

@Controller
@RequestMapping("/cm/withholdingrulesetting")
@RequiredArgsConstructor
@Slf4j
public class WithholdingRuleSettingController extends BaseController {

    private final WithholdingRuleSettingService withholdingRuleSettingService;

    /**
     * 预提规则设置 账户同步*
     *
     * @param param
     * @param response
     */
    @PostMapping("syncAccount")
    public void synchronousAccount(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        int count = withholdingRuleSettingService.synchronousAccount();
        renderJson(response, String.format(ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801AB", "同步账户成功，共同步[%s]条数据！") /* "同步账户成功，共同步[%s]条数据！" */), count));
//            ctmcmpBusinessLogService.saveBusinessLog(param, "", "", IServicecodeConstant.BANKACCOUNTSETTING, IMsgConstant.BANK_ACCOUNT_SETTING, IMsgConstant.ACCOUNT_SYNCHRONOUS);
    }


    /**
     * 预提规则设置提交接口*
     *
     * @param withholdingRuleSetting
     * @param response
     */
    @PostMapping("mysave")
    public void withholdingRuleSettingSave(@RequestBody WithholdingRuleSetting withholdingRuleSetting, HttpServletResponse response) throws Exception {
        CtmJSONObject result = withholdingRuleSettingService.withholdingRuleSettingSave(withholdingRuleSetting);
        renderJson(response, ResultMessage.data(result));
    }


    /**
     * 预提规则,查询协定利率设置接口*
     *
     * @param param    协定利率id
     * @param response
     */
    @PostMapping("agreeIRDetail")
    public void agreeIRSettingHistoryDetail(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        WithholdingRuleSetting result = null;
        if (param.getLong("mainid") != null) {
            result = withholdingRuleSettingService.onlyWithholdingRuleSetting(param.getLong("mainid"));
        } else {
            result = withholdingRuleSettingService.agreeIRSettingHistoryDetail(param.getLong("id"));
        }

        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 预提规则设置,删除协定利率接口接口*
     *
     * @param param    协定利率id
     * @param response
     */
    @PostMapping("agreeIRDelete")
    public void agreeIRSettingHistoryDelete(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        withholdingRuleSettingService.agreeIRSettingHistoryDelete(param.getLong("id"));
        renderJson(response, ResultMessage.data(true));
    }

    @PostMapping("enable")
    public void enableWithholdingRule(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        CtmJSONArray jsonArray = param.getJSONArray("data");
        // 数据加锁
        CtmJSONObject lockResult = JedisLockUtils.lockBill(WithholdingRuleSetting.ENTITY_NAME, jsonArray);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
            return;
        }
        Json jsondata = new Json(CtmJSONObject.toJSONString(jsonArray));
        List<WithholdingRuleSetting> data = Objectlizer.decode(jsondata, WithholdingRuleSetting.ENTITY_NAME);
        try {
            CtmJSONObject result = withholdingRuleSettingService.updateStatus(data, WithholdingRuleStatus.Enable.getValue());
            renderJson(response, ResultMessage.data(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            JedisLockUtils.unlockBill((List<YmsLock>) lockResult.get("ymsLockList"));
        }
    }


    @PostMapping("unenable")
    public void unenableWithholdingRule(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        CtmJSONArray jsonArray = param.getJSONArray("data");
        // 数据加锁
        CtmJSONObject lockResult = JedisLockUtils.lockBill(WithholdingRuleSetting.ENTITY_NAME, jsonArray);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
            return;
        }
        Json jsondata = new Json(CtmJSONObject.toJSONString(jsonArray));
        List<WithholdingRuleSetting> data = Objectlizer.decode(jsondata, WithholdingRuleSetting.ENTITY_NAME);
        try {
            CtmJSONObject result = withholdingRuleSettingService.updateStatus(data, WithholdingRuleStatus.Deactivate.getValue());
            renderJson(response, ResultMessage.data(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            JedisLockUtils.unlockBill((List<YmsLock>) lockResult.get("ymsLockList"));
        }
    }

}
