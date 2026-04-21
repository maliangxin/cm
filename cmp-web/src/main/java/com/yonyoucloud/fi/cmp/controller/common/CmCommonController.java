package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.permission.util.AuthSdkFacadeUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName CmCommonController
 * @Desc
 * @Author tongyd
 * @Date 2019/9/9
 * @Version 1.0
 */
@Controller
@RequestMapping("/cm/common/")
@Slf4j
public class CmCommonController extends BaseController {
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @PostMapping("getNatCurrency")
    @ApplicationPermission("CM")
    public void getNatCurrency(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, cmCommonService.getNatCurrency(param));
    }

    @PostMapping("getExchangeRate")
    public void getExchangeRate(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, cmCommonService.getExchangeRate(param));
    }

    @PostMapping("getTransType")
    @ApplicationPermission("CM")
    public void getTransType(@RequestBody Map<String, Object> condition, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmCommonService.getTransTypeByCondition(condition)));
    }

    /**
     * 根据数据权限过滤交易类型
     *
     * @param condition
     * @param response
     */
    @PostMapping("getTransTypeByDataPermission")
    @ApplicationPermission("CM")
    public void getTransTypeByDataPermission(@RequestBody Map<String, Object> condition, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmCommonService.getTransTypeByDataPermission(condition)));
    }

    /**
     * @param param
     * @param response
     */
    @PostMapping("getExchangeRateByRateType")
    @ApplicationPermission("CM")
    public void getExchangeRateByRateType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, cmCommonService.getExchangeRateByRateType(param));
    }

    @PostMapping("getEnabledPeriod")
    public void getEnabledPeriod(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, cmCommonService.getEnabledPeriod(param).toString());
    }

    @PostMapping("getCurrency")
    @ApplicationPermission("CM")
    public void getCurrency(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, cmCommonService.getCurrency(param));
    }

    @PostMapping("getTaxRate")
    @ApplicationPermission("CM")
    public void getTaxRate(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String code = param.getString("code");
        String name = param.getString("name");
        renderJson(response, cmCommonService.getTaxRate(code, name));
    }

    @PostMapping("getTaxRateById")
    @ApplicationPermission("CM")
    public void getTaxRateById(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String id = param.getString("id");
        renderJson(response, ResultMessage.data(cmCommonService.getTaxRateById(id)));
    }

    @PostMapping("getTaxRateArchive")
    @ApplicationPermission("CM")
    public void getTaxRateArchive(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String id = param.getString("id");
        renderJson(response, cmCommonService.getTaxRateArchive(id));
    }

    @PostMapping("getVoucherInitBalMes")
    @ApplicationPermission("CM")
    public void getVoucherInitBalMes(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String accentity = param.getString("accentity");
        String bankaccount = param.getString("bankaccount");
        Long bankreconciliationscheme = param.getLong("bankreconciliationscheme");
        String currency = param.getString("currency");
        short reconciliationDataSource = param.getShort("reconciliationDataSource");
        renderJson(response, ResultMessage.data(cmCommonService.getVoucherInitBalMes(accentity, bankaccount, bankreconciliationscheme, currency, reconciliationDataSource)));
    }

    @PostMapping("getUserIdByYhtUserId")
    public void getUserIdByYhtUserId(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String userYhtId = param.getString("userYhtId");
        renderJson(response, ResultMessage.data(cmCommonService.getUserIdByYhtUserId(userYhtId)));
    }

    @PostMapping("checkFunAuthorityByAuthId")
    @ApplicationPermission("CM")
    public void checkFunAuthorityByAuthId(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String authId = param.getString("authid");
        boolean checkAuthId = AuthSdkFacadeUtils.checkFunAuthorityByAuthId(AppContext.getYTenantId(), AppContext.getCurrentUser().getYhtUserId(), authId);
        renderJson(response, ResultMessage.data(checkAuthId));
    }


    /**
     * 查询开通的服务，对账单/认领单 业务关联和业务处理跳转专用
     *
     * @param param
     * @param response
     */
    @PostMapping("queryOpenServiceInfo")
    @Authentication(value = false)
    public void queryOpenServiceInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        renderJson(response, ResultMessage.data(cmCommonService.queryOpenServiceInfo()));
    }

    /**
     * 根据服务编码集合获取租户开通服务的情况
     *
     * @param param serviceCodeList 待校验的服务编码集合
     * @return 租户开通的服务结合
     */
    @PostMapping("checkOpenServiceList")
    @ApplicationPermission("CM")
    public void checkOpenServiceList(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmCommonService.checkOpenServiceList(param)));
    }

    /**
     * 根据资金组织获取汇率,解决编辑子表增行问题
     *
     * @param param
     * @return
     */
    @PostMapping("/getSwapOutExchangeRateName")
    public void getSwapOutExchangeRateName(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, CtmJSONObject.toJSONString(cmCommonService.getSwapOutExchangeRateName(param)));
    }


    @PostMapping("/queryBudgetControl")
    public void queryBudgetControl(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String billNum = json.getString("billno");
        boolean canStart = cmpBudgetManagerService.isCanStart(billNum);
        CtmJSONObject resultBack = new CtmJSONObject();
        resultBack.put(ICmpConstant.CODE, true);
        resultBack.put("isCanStart", canStart);
        renderJson(response, ResultMessage.data(resultBack));
    }

    /**
     * V5 新汇率查询；根据原币种，目的币种，日期，汇率类型查询汇率
     * @param param
     * currencyId :原币种
     * natCurrencyId : 目标币种
     * vouchDate : 日期 yyyy-MM-dd 字符串
     * exchangeRateType ： 汇率类型
     */
    @PostMapping("/getNewExchangeRate")
    public void getNewExchangeRate(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String currencyId = param.getString("currencyId");
        String natCurrencyId = param.getString("natCurrencyId");
        String vouchDateStr = param.getString("vouchDate");
        String exchangeRateType = param.getString("exchangeRateType");
        if (StringUtils.isEmpty(currencyId) || StringUtils.isEmpty(natCurrencyId) || StringUtils.isEmpty(vouchDateStr) || StringUtils.isEmpty(exchangeRateType)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80080", "请求参数不全") /* "请求参数不全" */));
        }else {
            Date vouchDate = DateUtils.parseDate(vouchDateStr,"yyyy-MM-dd");
            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currencyId, natCurrencyId, vouchDate, exchangeRateType);
            renderJson(response, ResultMessage.data(cmpExchangeRateVO));
        }
    }

    /**
     * V5 根据汇率折算方式查询新汇率；
     * * 根据原币种，目的币种，日期，汇率类型，汇率折算方式，是否反向查询 查询汇率
     * @param param
     * currencyId :原币种
     * natCurrencyId : 目标币种
     * vouchDate : 日期
     * exchangeRateType ： 汇率类型
     * exchRateOps ： 汇率折算方式 1：乘 2：除
     * isReverse  ： 是否反向查询
     */
    @PostMapping("/getExchangeRateWithOps")
    public void getExchangeRateWithOps(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String currencyId = param.getString("currencyId");
        String natCurrencyId = param.getString("natCurrencyId");
        Date vouchDate = param.getDate("vouchDate");
        String exchangeRateType = param.getString("exchangeRateType");
        String exchRateOps = param.getString("exchRateOps");
        Boolean isReverse = param.getBoolean("isReverse");
        Map<String, Object> condition1 = new HashMap<String, Object>();
        condition1.put("id", exchangeRateType);
        List<Map<String, Object>> mapList = QueryBaseDocUtils.queryExchangeRateTypeByCondition(condition1);
        if (CollectionUtils.isEmpty(mapList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100541"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180423", "未获取到汇率类型！") /* "未获取到汇率类型！" */);
        }
        Map<String, Object> exchangeRateVo = mapList.get(0);
        if (exchangeRateVo.get("code").toString().equals("02")) {
            return;
        }
        if (StringUtils.isEmpty(currencyId) || StringUtils.isEmpty(natCurrencyId) || vouchDate == null || StringUtils.isEmpty(exchangeRateType)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80080", "请求参数不全") /* "请求参数不全" */));
        }else {
            CmpExchangeRateVO cmpExchangeRateVO;
            if (StringUtils.isNotEmpty(exchRateOps)){
                cmpExchangeRateVO = CmpExchangeRateUtils.queryExchangeRateWithModeAndIsReverse(currencyId, natCurrencyId, vouchDate, exchangeRateType, exchRateOps, isReverse);
            }else {
                cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currencyId, natCurrencyId, vouchDate, exchangeRateType);
            }
            renderJson(response, ResultMessage.data(cmpExchangeRateVO));
        }
    }

    /**
     * 根据银行账号获取账户使用组织
     * @param param bankAccounts 银行账户id的数组
     */
    @PostMapping("queryUseOrgListByBankAccounts")
    public void queryUseOrgListByBankAccounts(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmCommonService.queryUseOrgListByBankAccounts(param)));
    }


}
