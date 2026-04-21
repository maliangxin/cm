package com.yonyoucloud.fi.cmp.controller.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliationrepeat.service.BankReconciliationRepeatService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.CtmDealDetailCheckMayRepeatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水认领疑似重复
 *
 * @author guoxh
 */
@RestController
@RequestMapping("/bankReconciliationRepeat")
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationRepeatController extends BaseController {
    private final BankReconciliationRepeatService bankReconciliationRepeatService;
    private static final String DEFAULT_UID = "bankReconciliationRepeat";

    @GetMapping("/repeatInfo")
    public void repeatInfo(HttpServletResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("isRepeatCheck", CtmDealDetailCheckMayRepeatUtils.isRepeatCheck);
        map.put("repeatFactors", String.join(",", CtmDealDetailCheckMayRepeatUtils.repeatFactors));
        renderJson(response, ResultMessage.data(map));
    }

    @PostMapping(value = "/changeRepeatStatus")
    @CMPDiworkPermission(IServicecodeConstant.doubtfulhandling)
    public void changeRepeatStatus(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        bankReconciliationRepeatService.changeRepeatStatus(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 确认正常
     *
     * @param params
     * @param response
     */
    @RequestMapping(value = "/confirmNormal", method = RequestMethod.POST)
    public void confirmNormal(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        bankReconciliationRepeatService.confirmNormal((List<Map>) params.get("data"), DEFAULT_UID);
        renderJson(response, ResultMessage.success());
    }

    /**
     * 确认重复
     *
     * @param params
     * @param response
     */
    @RequestMapping(value = "/confirmRepeat", method = RequestMethod.POST)
    public void confirmRepeat(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        bankReconciliationRepeatService.confirmRepeat((List<Map>) params.get("data"), DEFAULT_UID);
        renderJson(response, ResultMessage.success());
    }

    /**
     * 取消确认
     *
     * @param params
     * @param response
     */
    @RequestMapping(value = "/cancelConfirm", method = RequestMethod.POST)
    public void cancelConfirm(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        bankReconciliationRepeatService.cancelConfirm((List<Map>) params.get("data"), DEFAULT_UID);
        renderJson(response, ResultMessage.success());
    }
}
