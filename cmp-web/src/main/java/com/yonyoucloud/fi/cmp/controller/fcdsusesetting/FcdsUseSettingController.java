package com.yonyoucloud.fi.cmp.controller.fcdsusesetting;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 流水处理数据源使用设置
 *
 * @author guoxh
 */
@RestController
@RequestMapping("/fcdsusesetting")
@RequiredArgsConstructor
public class FcdsUseSettingController extends BaseController {
    private final IFcdsUseSettingInnerService fcdsUseSettingInnerService;

    @PostMapping("/unstop")
    @ApplicationPermission("CM")
    public void unstop(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        if (params.get("data") == null || ((List) params.get("data")).size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100294"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00234", "请选择一条数据") /* "请选择一条数据" */);
        } else {
            MessageResultVO messageResultVO = fcdsUseSettingInnerService.unstop((List) params.get("data"));
            renderJson(response, ResultMessage.data(messageResultVO));
        }
    }

    @PostMapping("/stop")
    @ApplicationPermission("CM")
    public void stop(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        if (params.get("data") == null || ((List) params.get("data")).size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100294"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00234", "请选择一条数据") /* "请选择一条数据" */);
        } else {
            MessageResultVO messageResultVO = fcdsUseSettingInnerService.stop((List) params.get("data"));
            renderJson(response, ResultMessage.data(messageResultVO));
        }
    }

    @PostMapping("/dataSync")
    public void dataSync(HttpServletResponse response) {
        fcdsUseSettingInnerService.dataSync();
        renderJson(response, ResultMessage.success());
    }
}
