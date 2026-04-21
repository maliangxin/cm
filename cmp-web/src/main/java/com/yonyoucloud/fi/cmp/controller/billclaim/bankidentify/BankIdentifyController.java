package com.yonyoucloud.fi.cmp.controller.billclaim.bankidentify;

import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.cmpentity.DcFlagEnum;
import com.yonyoucloud.fi.cmp.common.service.TenantOpenService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.ApplyObjectNewEnum;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankidentify.CtmCmpBankIdentifyRpcService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 流水辨识匹配rest接口层
 * @author: wanxbo@yonyou.com
 * @date: 2024/7/5 10:11
 */
@Controller
@RequestMapping("/bankidentify")
@Slf4j
public class BankIdentifyController extends BaseController {

    @Resource
    private BankIdentifyService bankIdentifyService;

//    @Resource
//    private CtmCmpBankIdentifyRpcService ctmCmpBankIdentifyRpcService;

    @Resource
    private TenantOpenService tenantOpenService;

    /**
     * 更新辨识匹配规则类型和辨识匹配规则设置的启停用状态
     *
     * @param param
     * @param response
     */
    @PostMapping("updateStatus")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void updateStatus(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankIdentifyService.updateStatus(param)));
    }


    /**
     * 根据业务对象编码查询相关性规则
     *
     * @param param bizObjectCode=业务对象编码
     * @return
     * @throws Exception
     */
    @PostMapping("queryRelevantRuleByBizCode")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void queryRelevantRuleByBizCode(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankIdentifyService.queryRelevantRuleByBizCode(param)));
    }

    /**
     * 流水辨识匹配类型排序接口
     *
     * @param param
     * @return
     * @throws Exception
     */
    @PostMapping("sortIdentifyTypeExcuteOrder")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void sortIdentifyTypeExcuteOrder(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankIdentifyService.sortIdentifyTypeExcuteOrder(param)));
    }

    /**
     * 租户流水辨识匹配规则数据预置
     *
     * @param param
     * @param response
     */
    @PostMapping("openInitData")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void testOpenInit(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        String remark = param.get("remark") != null ?param.get("remark").toString():"0";
        renderJson(response, ResultMessage.data(bankIdentifyService.initIdentifyTypeData(remark,tenant)));
    }

    @GetMapping("deleteInitData")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void deleteInitData() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        bankIdentifyService.deleteInitData(tenant);
    }


    /**
     * 租户流水辨识匹配规则数据预置
     *
     * @param response
     */
    @GetMapping("btwInitData")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void btwInitData(@RequestParam String remark, HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        remark = StringUtils.isEmpty(remark) ? "0" : remark;
        renderJson(response, ResultMessage.data(bankIdentifyService.initIdentifyTypeData(remark,tenant)));
    }

    @GetMapping("btwInitSystem004ItemDataNew")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void btwInitSystem004ItemDataNew( HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        renderJson(response, ResultMessage.data(bankIdentifyService.btwInitSystem004ItemDataNew(tenant)));
    }

    @GetMapping("btwInitSystem005ItemDataNew")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void btwInitSystem005ItemDataNew( HttpServletResponse response) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        renderJson(response, ResultMessage.data(bankIdentifyService.btwInitSystem005ItemDataNew(tenant)));
    }

    /**
     * 银行流水辨识数据启用/停用
     *
     * @param param
     * @param response
     */
    @PostMapping("/updateSettingStatus")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void updateSettingStatus(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankIdentifyService.updateSettingStatus(param)));
    }

    /**
     * 查找流水辨识匹配规则通用接口
     *
     * @param param
     * @param response
     */
    @PostMapping("/querySettings")
    @CMPDiworkPermission(IServicecodeConstant.BANK_RECONCILIATION_IDENTIFY_TYPE)
    public void querySettings(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        //从param中获取String org, Short dcFlag, Short applybilltype, Short identifytype这些参数
        String org = param.getString("org");
        DcFlagEnum dcFlag = DcFlagEnum.find(param.getShort("dcFlag"));
        ApplyObjectNewEnum applyObject = ApplyObjectNewEnum.find(param.getShort("applyobject"));
        BankIdentifyTypeEnum identifytype = BankIdentifyTypeEnum.find(param.getShort("identifytype"));
//        renderJson(response, ResultMessage.data(ctmCmpBankIdentifyRpcService.querySettings(org, dcFlag, applyObject, identifytype)));
    }
}
