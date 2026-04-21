package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.service.IAccountDetailExclusionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * @Author jpk
 * 账户收支明细剔除调度任务
 * @Date 2023/11/13 15:14
 */

@RestController
@RequestMapping("/accountdetailexclusiontask")
@Slf4j
public class AccountDetailExclusionTaskController extends BaseController {

    @Autowired
    IAccountDetailExclusionTaskService taskService;

    /**
     * 账户收支明细剔除自动发布
     */
    @PostMapping("/automaticExclusion")
    public void automaticExclusion(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (params == null) {
            params = new CtmJSONObject();
        }
        //获取自动任务参数
        params.put("logId", Optional.ofNullable(request.getHeader("logId")).orElse(""));
        if (StringUtils.isEmpty(params.getString("eliminateReasonType")) || StringUtils.isEmpty(params.getString("removereasons"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101743"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080064", "剔除原因类型与剔除原因不能为空。") /* "剔除原因类型与剔除原因不能为空。" */);
        }
        CtmJSONObject object = taskService.automaticExclusion(params);
        renderJson(response, CtmJSONObject.toJSONString(object));
    }
}
