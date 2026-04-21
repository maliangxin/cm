package com.yonyoucloud.fi.cmp.controller.flowhandlesetting;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingInnerService;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 流程处理流程设置
 *
 * @author guoxh
 */
@RestController
@RequestMapping("/flowhandlesetting")
@RequiredArgsConstructor
@Slf4j
public class FlowHandleSettingController extends BaseController {
    private final IFlowhandlesettingInnerService flowhandlesettingInnerService;

    @PostMapping("/unstop")
    @ApplicationPermission("CM")
    public void unstop(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        if (params.get("data") == null || ((List) params.get("data")).size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100294"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00234", "请选择一条数据") /* "请选择一条数据" */);
        } else {
            MessageResultVO messageResultVO = flowhandlesettingInnerService.unstop((List) params.get("data"));
            renderJson(response, ResultMessage.data(messageResultVO));
        }
    }

    @PostMapping("/stop")
    @ApplicationPermission("CM")
    public void stop(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        if (params.get("data") == null || ((List) params.get("data")).size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100294"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00234", "请选择一条数据") /* "请选择一条数据" */);
        } else {
            MessageResultVO messageResultVO = flowhandlesettingInnerService.stop((List) params.get("data"));
            renderJson(response, ResultMessage.data(messageResultVO));
        }
    }

    @PostMapping("/openInitData")
    @CMPDiworkPermission({"cmp_flowhandlesettingist"})
    public void openInitData(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        renderJson(response, ResultMessage.data(flowhandlesettingInnerService.initTenantData(tenant)));
    }
}
