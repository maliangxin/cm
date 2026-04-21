package com.yonyoucloud.fi.cmp.controller.initdata;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.initdata.InitDataConstant;
import com.yonyoucloud.fi.cmp.initdata.service.InitDataService;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
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
 * @author zhaodzh@yonyou.com
 * @version 1.0
 */
@Slf4j
@Controller
@RequestMapping("/initdata")
@Authentication(value = false, readCookie = true)
public class InitDataController extends BaseController {

    @Autowired
    InitDataService initDataService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @RequestMapping("/initDataBatchSave")
    @CMPDiworkPermission({IServicecodeConstant.CASHINITDATA, IServicecodeConstant.BANKINITDATA})
    public void settle(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONArray("data")));
        List<InitData> initDataList = Objectlizer.decode(json, InitData.ENTITY_NAME);
        initDataService.batchSave(initDataList);
        renderJson(response, ResultMessage.success(""));
    }

    @RequestMapping("/queryHvEditState")
    @CMPDiworkPermission({IServicecodeConstant.CASHINITDATA, IServicecodeConstant.BANKINITDATA})
    public void queryHvEditState(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        String accentity = String.valueOf(params.get("accentity"));
        String currency = String.valueOf(params.get("currency"));
        CtmJSONObject result;
        result = initDataService.queryHvEditState(accentity, currency);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    //编辑前校验日结状态
    @RequestMapping("/checkSettleflag")
    @CMPDiworkPermission({IServicecodeConstant.CASHINITDATA, IServicecodeConstant.BANKINITDATA})
    public void checkSettleflag(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        initDataService.checkSettleflag(params);
        renderJson(response, "{}");
    }

    /**
     * 银行账户期初同步
     * @param params
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/importDataYH")
    @CMPDiworkPermission(IServicecodeConstant.BANKINITDATA)
    public void importDataYH(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject resultData = initDataService.importData(InitDataConstant.CMP_INITDATAYHLIST);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKINITDATA, IMsgConstant.BANK_INITDATA, IMsgConstant.ACCOUNT_SYNCHRONOUS);
        renderJson(response, ResultMessage.data(resultData));
    }

    /**
     * 现金账户期初同步
     * @param params
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/importDataXJ")
    @CMPDiworkPermission(IServicecodeConstant.CASHINITDATA)
    public void importDataXJ(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject resultData = initDataService.importData(InitDataConstant.CMP_INITDATAXJLIST);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.CASHINITDATA, IMsgConstant.CASH_INITDATA, IMsgConstant.ACCOUNT_SYNCHRONOUS);
        renderJson(response, ResultMessage.data(resultData));
    }


    /**
     * 期初数据升级-当前租户
     *
     * @param params
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/upgradeInitData")
    @CMPDiworkPermission(IServicecodeConstant.CASHINITDATA)
    public void upgradeInitData(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject resultData = initDataService.upgradeInitData();
        renderJson(response, ResultMessage.data(resultData));
    }

    /**
     * 期初数据升级-全体租户
     *
     * @param params
     * @param request
     * @param response
     * @throws Exception -核心1（1）：租户：641
     *                   * 期初数据：5129
     *                   其余分库数据良很小 不足200
     *                   核心2：租户：5835
     *                   期初数据：27870
     *                   核心3（1）：租户：5835
     *                   * 期初数据：27870
     *                   <p>
     *                   核心4：租户：5835
     *                   * 期初数据：27870
     */
    @RequestMapping("/upgradeInitDataAll")
    @CMPDiworkPermission(IServicecodeConstant.CASHINITDATA)
    public void upgradeInitDataAll(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        initDataService.scheduledUpgradeInitData(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 根据原有数据的子表 更新主表数据
     * 适用于更新R6版本后 没有通过运营工具升级数据 直接点了期初同步的情况
     * 此时系统会认为原子表的期初数据不存在 重新同步账户 期初余额会变为0
     *
     * @param params
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/updateNewInitDataForOldData")
    @CMPDiworkPermission(IServicecodeConstant.CASHINITDATA)
    public void updateNewInitDataForOldData(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        initDataService.updateNewInitDataForOldData(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 修改账户期初日期
     * @param param
     * @param response
     * @throws Exception
     */
    @RequestMapping("/changeAccountDate")
    @CMPDiworkPermission({IServicecodeConstant.BANKINITDATA,IServicecodeConstant.CASHINITDATA})
    public void changeAccountDate(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, initDataService.changeAccountDate(param));
    }
}
