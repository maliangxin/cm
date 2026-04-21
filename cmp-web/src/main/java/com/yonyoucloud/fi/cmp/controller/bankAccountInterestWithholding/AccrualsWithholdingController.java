package com.yonyoucloud.fi.cmp.controller.bankAccountInterestWithholding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.accrualsWithholding.service.AccrualsWithholdingService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/depositinterestwithholding")
@RequiredArgsConstructor
@Slf4j
public class AccrualsWithholdingController extends BaseController {

    private final AccrualsWithholdingService accrualsWithholdingService;

    /**
     * 利息测算
     *
     * @param bill     前台列表选择的数据
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/calculate")
    public void calculate(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject param = getJsonObject(bill);
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONArray("data")));
        //对前台传入数据进行转换封装
        List<AccrualsWithholding> accrualsWithholdinges = Objectlizer.decode(json, AccrualsWithholding.ENTITY_NAME);
        //调用利息测算服务
        JsonNode result = accrualsWithholdingService.calculate(accrualsWithholdinges);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(result)));
    }

    private CtmJSONObject getJsonObject(@RequestBody BillDataDto bill) {
        Map<String, Object> map = new HashMap<>();
        map.put("billnum", bill.getBillnum());
        map.put("data", bill.getData());
        return new CtmJSONObject(map);
    }

    /**
     * 预提
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/withholding")
    public void settlement(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayNode rows = param.withArray("data");
        Json json = new Json(rows.toString());
        //对前台传入数据进行转换封装
        List<AccrualsWithholding> accrualsWithholdinges = Objectlizer.decode(json, AccrualsWithholding.ENTITY_NAME);
        //调用预提记录查询方法
        JsonNode result = accrualsWithholdingService.tWithholding(accrualsWithholdinges);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(result)));
    }

    /**
     * 反预提
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/batchunWithholding")
    public void batchunwWthholding(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayNode rows = param.withArray("data");
        Json json = new Json(rows.toString());
        //对前台传入数据进行转换封装
        List<AccrualsWithholding> accrualsWithholdinges = Objectlizer.decode(json, AccrualsWithholding.ENTITY_NAME);
        //调用预提记录查询方法
        JsonNode result = accrualsWithholdingService.unwWthholding(accrualsWithholdinges);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(result)));
    }

    /**
     * 反预提
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/unWithholding")
    public void unwWthholding(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayNode rows = param.withArray("data");
        Json json = new Json(rows.toString());
        //对前台传入数据进行转换封装
        List<AccrualsWithholding> accrualsWithholdinges = Objectlizer.decode(json, AccrualsWithholding.ENTITY_NAME);
        //调用预提记录查询方法
        JsonNode result = accrualsWithholdingService.unwWthholding(accrualsWithholdinges);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(result)));
    }

    /**
     * 查询利息测算记录
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/queryInterestHandleList")
    @CMPDiworkPermission(IServicecodeConstant.CMP_DEPOSITINTERESTWITHHOLDINGLIST)
    public void queryInterestHandleList(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayNode rows = param.withArray("params");
        Json json = new Json(rows.toString());
        //对前台传入数据进行转换封装
        List<AccrualsWithholding> accrualsWithholdinges = Objectlizer.decode(json, AccrualsWithholding.ENTITY_NAME);
        //调用预提记录查询方法
        JsonNode result = accrualsWithholdingService.queryInterestHandleList(accrualsWithholdinges);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(result)));

    }

    /**
     * 查询利息测算记录银行预提记录卡片页面调用
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/queryInterestList")
    @CMPDiworkPermission(IServicecodeConstant.CMP_ACCRUALSWITHHOLDINGQUERY)
    public void queryInterestList(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayNode rows = param.withArray("params");
        Json json = new Json(rows.toString());
        //对前台传入数据进行转换封装
        List<AccrualsWithholding> accrualsWithholdinges = Objectlizer.decode(json, AccrualsWithholding.ENTITY_NAME);
        //调用预提记录查询方法
        JsonNode result = accrualsWithholdingService.queryInterestList(accrualsWithholdinges);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(result)));
    }
}
