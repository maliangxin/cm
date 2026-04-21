package com.yonyoucloud.fi.cmp.controller.flowhandletype;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.flowhandletype.service.IFlowHandleTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/flowhandletype")
@Slf4j
public class FlowHandleTypeController extends BaseController {
    @Autowired
    private IFlowHandleTypeService flowHandleTypeService;

    @PostMapping("updateStatus")
    @CMPDiworkPermission({"cmp_fcdsusesettinglist", "cmp_flowhandlesettingist"})
    public void updateStatus(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowHandleTypeService.updateStatus(param)));
    }

    @PostMapping("/sortNum")
    @CMPDiworkPermission({"cmp_fcdsusesettinglist", "cmp_flowhandlesettingist"})
    public void sortNum(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowHandleTypeService.sortNum(param)));
    }

    @PostMapping("/openInitData")
    @CMPDiworkPermission({"cmp_fcdsusesettinglist", "cmp_flowhandlesettingist"})
    public void openInitData(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        flowHandleTypeService.initIdentifyTypeData(tenant, param.get("billnum").toString());
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007D", "租户数据预置成功") /* "租户数据预置成功" */);
        renderJson(response, ResultMessage.data(c));
    }

//    @PostMapping("/openInitData2")
//    @CMPDiworkPermission({"cmp_fcdsusesettinglist","cmp_flowhandlesettingist"})
//    public void openInitData2(@RequestBody CtmJSONObject param, HttpServletResponse response) {
//        try {
//            Tenant tenant = new Tenant();
//            tenant.setId(AppContext.getTenantId());
//            tenant.setYTenantId(AppContext.getYTenantId());
//            renderJson(response, ResultMessage.data(flowHandleTypeService.initIdentifyTypeData(tenant)));
//        } catch (Exception e) {
//            log.error("querySettingDetailInfo fail!, e = {}", e.getMessage());
//            renderJson(response, ResultMessage.error(e.getMessage()));
//        }
//    }

    @GetMapping("/reInitData")
    @CMPDiworkPermission({"cmp_fcdsusesettinglist", "cmp_flowhandlesettingist"})
    public void reInitData(@RequestParam(required = false, defaultValue = "1") Integer onlyDelete, HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        renderJson(response, ResultMessage.data(flowHandleTypeService.reInitData(tenant, onlyDelete != 0)));
    }
}
