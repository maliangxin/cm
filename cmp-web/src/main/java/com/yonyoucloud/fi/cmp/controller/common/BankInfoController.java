package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @description: 银行账户信息查询
 * @author: wanxbo@yonyou.com
 * @date: 2022/8/23 14:14
 */
@Controller
@RequestMapping("/bankinfo")
@Slf4j
public class BankInfoController extends BaseController {

    @Resource
    private BaseRefRpcService baseRefRpcService;

    @PostMapping("/querybankdot")
    @CMPDiworkPermission(IServicecodeConstant.PAYMENTBILL)
    public void publish(@RequestBody BillDataDto bill, HttpServletResponse response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String id = ValueUtils.isNotEmptyObj(params.get("id")) ? params.get("id").toString() : null;
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100369"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180267", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        renderJson(response, ResultMessage.data(baseRefRpcService.queryBandDotById(id)));
    }
}
