package com.yonyoucloud.fi.cmp.controller.checkstock;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStockService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckDir;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 重空凭证工作台请求入口
 */
@Controller
@RequestMapping("/checkTable")
@Authentication(value = false, readCookie = true)
@Slf4j
public class CheckStockController extends BaseController {
    @Autowired
    CheckStockService checkStockService;
    @Autowired
    AutoConfigService autoConfigService;

    /**
     * 重空凭证工作台 领用
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/getUsed")
    public void checkStockGetUsed(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray row = obj.getJSONArray("data");
        String custNo = obj.getString("custNo");
        if(null == row || row.size() < 1){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(CheckStock.ENTITY_NAME,row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                if (!autoConfigService.getCheckStockIsUse()) {
                    throw new Exception(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009A", "重空凭证（付票）是否有领用环节未启用,不能进行领用") /* "重空凭证（付票）是否有领用环节未启用,不能进行领用" */));
                }
                String checkBookNo = row.getJSONObject(0).getString("checkBookNo");
                String checkBillDir = row.getJSONObject(0).getString("checkBillDir");
                String accentity = row.getJSONObject(0).getString("accentity");
                if (CmpCheckDir.Cash.getValue().equals(checkBillDir)) {
                    checkStockService.checkAccAuthority(accentity,custNo,checkBookNo);
                }
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                CtmJSONArray result = checkStockService.checkStockGetUsed(lockRowData,custNo);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(CheckStock.ENTITY_NAME,lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 重空凭证工作台 取消领用
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/cancelUsed")
    public void checkStockCancelUsed(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray row = obj.getJSONArray("data");
        if(null == row || row.size() < 1){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(CheckStock.ENTITY_NAME,row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                CtmJSONArray result = checkStockService.checkStockCancelUsed(lockRowData);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(CheckStock.ENTITY_NAME,lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 重空凭证工作台 兑付
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/getCash")
    public void checkStockGetCash(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray row = obj.getJSONArray("data");
        if(null == row || row.size() < 1){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(CheckStock.ENTITY_NAME,row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                CtmJSONArray result = checkStockService.checkStockGetCash(lockRowData,obj);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(CheckStock.ENTITY_NAME,lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 重空凭证工作台 取消兑付
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/cancelCash")
    public void checkStockCancelCash(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray row = obj.getJSONArray("data");
        if(null == row || row.size() < 1){
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(CheckStock.ENTITY_NAME,row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                CtmJSONArray result = checkStockService.checkStockCancelCash(lockRowData);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(CheckStock.ENTITY_NAME,lockResult.getJSONArray("lockRowData"));
            }
        }
    }


    /**
     * 迁移历史支票库存数据到支票状态流转表
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/mgStock")
    public void migrationStockToStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkStockService.migrationStockToStatus();
    }

}
