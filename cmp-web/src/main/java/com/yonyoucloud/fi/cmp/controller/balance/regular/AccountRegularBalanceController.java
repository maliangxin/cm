package com.yonyoucloud.fi.cmp.controller.balance.regular;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountfixedbalance.AccountFixedBalance;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.accountregularbalance.service.AccountRegularBalanceService;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 定期余额请求入口
 */
@Controller
@RequestMapping("/accountRegularBalance")
@Authentication(value = false, readCookie = true)
@Slf4j
public class AccountRegularBalanceController extends BaseController {
    @Autowired
    AccountRegularBalanceService accountRegularBalanceService;

    /**
     * 定期余额 余额确认
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/confirm")
    public void confirmAccountBalance(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/confirm");
        CtmJSONArray row = obj.getJSONArray("data");
        if(null == row || row.size() < 1){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(AccountRealtimeBalance.ENTITY_NAME,row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<AccountFixedBalance> billList = Objectlizer.decode(json, AccountFixedBalance.ENTITY_NAME);
                CtmJSONObject result = accountRegularBalanceService.confirmAccountBalance(billList);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(AccountRealtimeBalance.ENTITY_NAME,lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 定期余额 取消确认
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/cancelconfirm")
    public void cancelConfirmAccountBalance(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/cancelConfirm");
        CtmJSONArray row = obj.getJSONArray("data");
        if(null == row || row.size() < 1){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(AccountRealtimeBalance.ENTITY_NAME,row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<AccountFixedBalance> billList = Objectlizer.decode(json, AccountFixedBalance.ENTITY_NAME);
                CtmJSONObject result = accountRegularBalanceService.cancelConfirmAccountBalance(billList);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(AccountRealtimeBalance.ENTITY_NAME,lockResult.getJSONArray("lockRowData"));
            }
        }
    }

}
