package com.yonyoucloud.fi.cmp.controller.reconciliation.balanceAdjust;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.balanceadjust.CtmCmpBalanceAdjustRpcService;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BalanceAdjustRetVo;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.BalanceAdjustVo;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@RequestMapping("/bankreconciliation")
public class BalanceAdjustResultController extends BaseController {

    //    @Autowired
//    private CtmCmpBalanceAdjustRpcService ctmCmpBalanceAdjustRpcService;
    @Autowired
    private BalanceAdjustResultSerevice balanceAdjustResultSerevice;

    @Autowired
    private AutoConfigService autoConfigService;

    @ResponseBody
    @RequestMapping("/saveBalanceadjustResult")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void add(@RequestBody CtmJSONObject obj, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject("paramObj")));
        String filterArgs = CtmJSONObject.toJSONString(obj.getJSONObject("filterArgs"));
        CtmJSONObject ctmJson = obj.getJSONObject("paramObj");
        String bankYe = ctmJson.getString("bankye");
        try {
            BigDecimal bigBankYe = new BigDecimal(bankYe);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100765"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19DE85D604900007", "银行对账单余额录入格式错误!") /* "银行对账单余额录入格式错误!" */);
        }
        List<BalanceAdjustResult> balanceAdjustResultList = Objectlizer.decode(json, BalanceAdjustResult.ENTITY_NAME);
        if (CollectionUtils.isNotEmpty(balanceAdjustResultList)) {
            CtmJSONObject jSONObject = balanceAdjustResultSerevice.add(balanceAdjustResultList.get(0), filterArgs, ctmJson);
            renderJson(response, ResultMessage.data(jSONObject));
        } else {
            log.error("/bankreconciliation/saveBalanceadjustResult 接口出现异常{}:后端接收参数为空！");
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180155", "后端接收参数为空！") /* "后端接收参数为空！" */));
        }
    }

    @ResponseBody
    @RequestMapping("/queryExistsByCond")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATION, IServicecodeConstant.OPENINGOUTSTANDING})
    public void queryExistsByCond(@RequestBody CtmJSONObject obj, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject("paramObj")));
        List<BalanceAdjustResult> balanceAdjustResultList = Objectlizer.decode(json, BalanceAdjustResult.ENTITY_NAME);
        if (CollectionUtils.isNotEmpty(balanceAdjustResultList)) {
            BalanceAdjustResult dataDb = balanceAdjustResultSerevice.queryExistsByCond(balanceAdjustResultList.get(0));
            renderJson(response, ResultMessage.data(dataDb));
        } else {
            log.error("/bankreconciliation/queryExistsByCond 接口出现异常{}:后端接收参数为空！");
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180155", "后端接收参数为空！") /* "后端接收参数为空！" */));
        }
    }

   /* @NotNull
    private BalanceAdjustResult getBanlanceAdjustResult(CtmJSONObject paramObj) {
        BalanceAdjustResult balanceAdjustResult = new BalanceAdjustResult();
        balanceAdjustResult.setAccentity(paramObj.getString("accentity"));
        balanceAdjustResult.setBankreconciliationscheme(Long.parseLong(paramObj.get("bankreconciliationscheme").toString()));
        balanceAdjustResult.setBankaccount(paramObj.getString("bankaccount"));
        balanceAdjustResult.setJournalyhyf(new BigDecimal(paramObj.getString("journalyhyf")));
        balanceAdjustResult.setJournalyhys(new BigDecimal(paramObj.getString("journalyhys")));
        balanceAdjustResult.setJournalye(new BigDecimal(paramObj.getString("journalye")));
        balanceAdjustResult.setJournaltzye(new BigDecimal(paramObj.getString("journaltzye")));
        balanceAdjustResult.setBankye(new BigDecimal(paramObj.getString("bankye")));
        balanceAdjustResult.setBankqyyf(new BigDecimal(paramObj.getString("bankqyyf")));
        balanceAdjustResult.setBankqyys(new BigDecimal(paramObj.getString("bankqyys")));
        balanceAdjustResult.setBanktzye(new BigDecimal(paramObj.getString("banktzye")));
        balanceAdjustResult.setBankdate(paramObj.get("bankdate") == null ? null : DateUtils.strToDate(paramObj.get("bankdate").toString()));
        balanceAdjustResult.setJournaldate(paramObj.get("journaldate") == null ? null : DateUtils.strToDate(paramObj.get("journaldate").toString()));
        balanceAdjustResult.setDzdate(paramObj.get("dzdate") == null ? null : DateUtils.strToDate(paramObj.get("dzdate").toString()));
        return balanceAdjustResult;
    }*/

    @ResponseBody
    @RequestMapping("/delete")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATION, IServicecodeConstant.BALANCEADJUSTRESULT})
    public void delete(@RequestBody CtmJSONObject obj, HttpServletResponse response) throws Exception {
        Long id = obj.getLong("id");
        CtmJSONObject jSONObject = balanceAdjustResultSerevice.delete(id);
        renderJson(response, ResultMessage.data(jSONObject));
    }

    /**
     * 审核
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/balanceAudit")
    public void balanceAudit(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        Json json = new Json(CtmJSONObject.toJSONString(rows));
        //对前台传入数据进行转换封装
        List<BalanceAdjustResult> balanceAdjustResultes = Objectlizer.decode(json, BalanceAdjustResult.ENTITY_NAME);
        //调用审核
        CtmJSONObject result = balanceAdjustResultSerevice.balanceAudit(balanceAdjustResultes);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * RPT0274提供余额调节表查询接口，供上游调用
     * */
//    @RequestMapping("/filterBalanceAdjustPage")
//    public void balanceRetTest(@RequestBody BalanceAdjustVo balanceAdjustVo, HttpServletResponse response) throws Exception {
//        BalanceAdjustRetVo balanceAdjustRetVo = ctmCmpBalanceAdjustRpcService.filterBalanceAdjustPage(balanceAdjustVo);
//        renderJson(response, ResultMessage.data(balanceAdjustRetVo));
//    }

    /**
     * 弃审
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/balanceUnAudit")
    public void balanceUnAudit(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        Json json = new Json(CtmJSONObject.toJSONString(rows));
        //对前台传入数据进行转换封装
        List<BalanceAdjustResult> balanceAdjustResultes = Objectlizer.decode(json, BalanceAdjustResult.ENTITY_NAME);
        //调用弃审方法
        CtmJSONObject result = balanceAdjustResultSerevice.balanceUnAudit(balanceAdjustResultes);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 银企对账-是否允许修改余额调节表银行账户余额
     *
     * @param response
     */
    @PostMapping("/getBalanceadjustIsEdit")
    @CMPDiworkPermission({IServicecodeConstant.BALANCEADJUSTRESULT, IServicecodeConstant.BANKRECONCILIATION})
    public void getBalanceadjustIsEdit(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        //202603 采用现金基础参数-银企对账生成余额调节表是否允许修改银行账户余额 来做控制，yms参数废弃
//        String balanceadjustIsEdit = AppContext.getEnvConfig("cmp.balanceadjust.edit", "true");
//        if (!balanceadjustIsEdit.equals("true")) {
//            renderJson(response, ResultMessage.data(false));
//        } else {
//            renderJson(response, ResultMessage.data(true));
//        }
        boolean canEdit = true;
        List<Map<String, Object>> configList = autoConfigService.getGlobalConfig();
        if ( CollectionUtils.isNotEmpty(configList) && configList.get(0).containsKey("balanceadjustIsEdit")){
            Object value = configList.get(0).get("balanceadjustIsEdit");
            // 兼容处理：如果是数字类型，判断是否等于 1；如果是布尔类型直接转换
            if (value instanceof Number) {
                canEdit = ((Number) value).intValue() == 1;
            } else if (value instanceof Boolean) {
                canEdit = (Boolean) value;
            } else {
                // 兜底：尝试解析字符串 "1" 或 "true"
                canEdit = "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
            }
        }
        renderJson(response, ResultMessage.data(canEdit));
    }

    /**
     * 根据id查询余额调节表详情
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/getBalanceAdjustResultById")
    @CMPDiworkPermission({IServicecodeConstant.BALANCEADJUSTRESULT, IServicecodeConstant.BANKRECONCILIATION})
    public void getBalanceAdjustResultById(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long id = param.getLong("id");
        BalanceAdjustResult balanceAdjustResult = balanceAdjustResultSerevice.getBalanceAdjustResultById(id);
        renderJson(response, ResultMessage.data(balanceAdjustResult));
    }
}
