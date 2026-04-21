package com.yonyoucloud.fi.cmp.controller.billclaim.busvouchercorr;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.busvouchercorr.CmpBusVoucherCorrService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 智能到账，认领业务凭据关联后台接口
 * @author: wanxbo@yonyou.com
 * @date: 2024/7/1 10:13
 */

@Controller
@RequestMapping("/busvouchercorr")
@Slf4j
public class CmpBusVoucherCorrController extends BaseController {

    @Resource
    private CmpBusVoucherCorrService cmpBusVoucherCorrService;

    /**
     * 智能到账，业务凭据关联，销售订单
     * 根据关联的销售订单子表信息，查询订单关联到的收款协议，并返回给前端做处理
     * @param param
     * @param response
     */
    @PostMapping("queryUdinghuoCollectAgreement")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATION,IServicecodeConstant.BILLCLAIMCARD})
    public void queryUdinghuoCollectAgreement(@RequestBody CtmJSONObject param, HttpServletResponse response)throws Exception {
        renderJson(response, ResultMessage.data(cmpBusVoucherCorrService.queryUdinghuoCollectAgreement(param)));
    }

    /**
     * 智能到账，业务凭据关联，合同档案
     * 根据关联的销售订单子表信息，查询订单关联到的收款协议，并返回给前端做处理
     * @param param
     * @param response
     */
    @PostMapping("getBusVoucherInfoList")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATION,IServicecodeConstant.BILLCLAIMCARD})
    public void getBusVoucherInfoList(@RequestBody CtmJSONObject param, HttpServletResponse response)throws Exception {
        renderJson(response, ResultMessage.data(cmpBusVoucherCorrService.getBusVoucherInfoList(param)));
    }

}
