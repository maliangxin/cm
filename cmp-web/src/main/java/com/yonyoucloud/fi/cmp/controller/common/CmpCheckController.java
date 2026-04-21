package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 银企对账相关数据查询接口
 * @author: wanxbo@yonyou.com
 * @date: 2024/1/26 15:09
 */

@Controller
@RequestMapping("/cmpcheckrest")
@Slf4j
public class CmpCheckController extends BaseController {

    @Resource
    private CmpCheckService cmpCheckService;

    /**
     * 查询银企对账
     *
     * @param param
     * @return
     */
    @PostMapping("querySettingDetailInfo")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void querySettingDetailInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmpCheckService.querySettingDetailInfo(param)));
    }

    /**
     * 查询期初未达关联的授权使用组织和对账财务账簿等信息
     *
     * @param param bankreconciliationscheme 对账id
     *              bankaccount 银行账户
     *              currency 币种
     * @return
     * @throws Exception
     */
    @PostMapping("queryOpenOutstandingInfo")
    @CMPDiworkPermission(IServicecodeConstant.OPENINGOUTSTANDING)
    public void queryOpenOutstandingInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmpCheckService.queryOpenOutstandingInfo(param)));
    }

    /**
     * 查询银行账户的授权使用组织信息
     *
     * @param param bankaccountid 银行账户id
     * @return rangeOrgId:授权使用组织id; rangeOrgIdName授权使用组织名称
     * @throws Exception
     */
    @PostMapping("queryBankAccountUseOrgInfo")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATIONSETTING, IServicecodeConstant.OPENINGOUTSTANDING})
    public void queryBankAccountUseOrgInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmpCheckService.queryBankAccountUseOrgInfo(param)));
    }

    @PostMapping("testDataPermission")
    public void testDataPermission(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String serviceCode = param.getString("serviceCode");
        renderJson(response, ResultMessage.data(cmpCheckService.getBankAccountDataPermission(serviceCode)));
    }
}
