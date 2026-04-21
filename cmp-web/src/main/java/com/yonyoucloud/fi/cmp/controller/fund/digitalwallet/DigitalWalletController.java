package com.yonyoucloud.fi.cmp.controller.fund.digitalwallet;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.common.digitalwallet.DigitalWalletService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;

/**
 * @Description 数币钱包接口
 * @Author hanll
 * @Date 2025/8/30-16:10
 */
@RestController
@RequestMapping("/digitalwallet")
public class DigitalWalletController {

    @Autowired
    private DigitalWalletService digitalWalletService;

    /**
     * 保存校验
     * @param param    :
     * @param response :
     */
    @PostMapping("/checkSave")
    @ApplicationPermission("CM")
    public void checkSave(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = digitalWalletService.checkSave(param);
        renderJson(response, ResultMessage.data(jsonObject));
    }

}
