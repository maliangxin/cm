package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.util.CtmCheckActionDataPermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 现金管理自定义接口数据权限校验
 * @author: liudongr@yonyou.com
 * @date: 2024/3/19 15:09
 */

@Controller
@RequestMapping("/cmpcheckdatapermission")
@Slf4j
public class CmpCheckActionDataPermissionController extends BaseController {

    /**
     * 资金收款单自定义接口数据权限校验
     * @param param
     * @return
     */
    @PostMapping("/queryFundCollectionPermission")
    @ApplicationPermission("CM")
    public void queryFundCollectionPermission(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception{
            String action = param.getString("action") == null?"edit":param.getString("action");
            Map<String, String> paramMap = buildParams(action, "ctm-cmp.cmp_fundcollection", FundCollection.ENTITY_NAME, IServicecodeConstant.FUNDCOLLECTION);
            CtmCheckActionDataPermissionUtils.querySettingDetailInfo(paramMap, param);
            renderJson(response, ResultMessage.data("success"));
    }

    /**
     * 资金付款单自定义接口数据权限校验
     * @param param
     * @return
     */
    @PostMapping("/queryFundPaymentPermission")
    @ApplicationPermission("CM")
    public void queryFundPaymentPermission(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception{
            String action = param.getString("action") == null?"edit":param.getString("action");
            Map<String, String> paramMap = buildParams(action, " ctm-cmp.cmp_fundpayment", FundPayment.ENTITY_NAME, IServicecodeConstant.FUNDPAYMENT);
            CtmCheckActionDataPermissionUtils.querySettingDetailInfo(paramMap, param);
            renderJson(response, ResultMessage.data("success"));
    }

    /**
     * 构建查询参数
     * @param action 按钮动作
     * @param bizObjCode 业务对象编码
     * @param entityName 实体名称
     * @param serviceCode 服务编码
     * @return
     */
    private Map<String, String> buildParams(String action, String bizObjCode, String entityName, String serviceCode) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("action", action);
        paramMap.put("bizObjCode", bizObjCode);
        paramMap.put("fullName", entityName);
        paramMap.put("serviceCode", serviceCode);
        return paramMap;
    }
}
