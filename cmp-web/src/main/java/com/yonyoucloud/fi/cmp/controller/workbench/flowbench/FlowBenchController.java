package com.yonyoucloud.fi.cmp.controller.workbench.flowbench;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.error.CommonCtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.workbench.flowbench.dto.req.FlowBenchVO;
import com.yonyoucloud.fi.cmp.workbench.flowbench.service.IFlowBenchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/cmp_flowbill")
@Authentication(value = false, readCookie = true)
public class FlowBenchController extends BaseController {
    @Autowired
    private IFlowBenchService flowBenchService;

    @PostMapping(value = {"saveView"})
    public void addView(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        this.checkParams(flowBenchVO);
        renderJson(response, ResultMessage.data(flowBenchService.saveView(flowBenchVO)));
    }

    @GetMapping("/listView")
    public void listView(HttpServletResponse response) throws Exception {
        flowBenchService.initData();
        renderJson(response, ResultMessage.data(flowBenchService.list()));

    }

    @PostMapping("/exists")
    public void exists(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        boolean flag = flowBenchService.existsById(flowBenchVO.getId());
        if (flag) {
            renderJson(response, ResultMessage.success());
        } else {
            renderJson(response, ResultMessage.error(CommonCtmErrorCode.ILLEGAL_ARGUMENT.build(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C875D9E04380005", "当前视图已删除，自动为您切换到默认视图")));
        }
    }

    @GetMapping("/queryDefault")
    public void queryDefault(HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryDefault(InvocationInfoProxy.getUserid())));
    }

    @GetMapping("/initData")
    public void initData(HttpServletResponse response) throws Exception {
        flowBenchService.initData();
        renderJson(response, ResultMessage.success());
    }

    @GetMapping("/deleteView")
    public void deleteView(@RequestParam Long id, HttpServletResponse response) throws Exception {
        flowBenchService.batchDeleteView(Collections.singletonList(id));
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/batchDeleteView")
    public void batchDeleteView(@RequestBody List<FlowBenchVO> list, HttpServletResponse response) throws Exception {
        List<Long> ids = list.stream().map(FlowBenchVO::getId).collect(Collectors.toList());
        flowBenchService.batchDeleteView(ids);
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/flowTodo")
    public void flowTodo(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryFlowTodo(flowBenchVO)));
    }

    @PostMapping("/moncalc")
    public void moncalc(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryMonthCalc(flowBenchVO)));
    }

    @PostMapping("/daycalc")
    public void daycalc(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        if (flowBenchVO.getChooseDay() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101433"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080052", "chooseDay 未传值") /* "chooseDay 未传值" */);
        }
        renderJson(response, ResultMessage.data(flowBenchService.queryDayCalc(flowBenchVO)));
    }

    @PostMapping("/receipt")
    public void receipt(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryReceipt(flowBenchVO)));
    }

    @PostMapping("/flowtop")
    public void flowtop(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryFlowTop(flowBenchVO)));
    }

    @PostMapping("/flowwarnning")
    public void flowwarnning(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryFlowWarning(flowBenchVO)));
    }

    @PostMapping("/queryUnImportAccount")
    public void queryUnImportAccount(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryUnImportAccount(flowBenchVO)));
    }

    @PostMapping("/queryBalanceMissData")
    public void queryBalanceMissData(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryBalanceListData(flowBenchVO, 2)));
    }

    @PostMapping("/queryBalanceUnmatchData")
    public void queryBalanceUnmatchData(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryBalanceListData(flowBenchVO, 1)));
    }

//    @PostMapping("/balancecheck")
//    public void balancecheck(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) {
//        try {
//            renderJson(response, ResultMessage.data(flowBenchService.balanceCheck(flowBenchVO)));
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            renderJson(response, ResultMessage.error(e.getMessage()));
//        }
//    }

    @PostMapping("/processcheck")
    public void processcheck(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.processCheck(flowBenchVO)));
    }

//    @PostMapping("/outdateflow")
//    public void outdateflow(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) {
//        try {
//            renderJson(response, ResultMessage.data(flowBenchService.queryOutdateFlow(flowBenchVO)));
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            renderJson(response, ResultMessage.error(e.getMessage()));
//        }
//    }

    @PostMapping("/unionpaymonitor")
    public void unionpaymonitor(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryUnionpayMonitor(flowBenchVO)));
    }

    @PostMapping("/rpamonitor")
    public void rpamonitor(@RequestBody FlowBenchVO flowBenchVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(flowBenchService.queryRpaMonitor(flowBenchVO)));
    }

    private void checkParams(FlowBenchVO flowBenchVO) {
        if (flowBenchVO == null || StringUtils.isEmpty(flowBenchVO.getViewName())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101434"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81871004080001", "视图名称不能为空!"));
        }
        if (flowBenchVO.getDateRange() == null || flowBenchVO.getDateRange().length < 2 || StringUtils.isEmpty(flowBenchVO.getDateRange()[0]) || StringUtils.isEmpty(flowBenchVO.getDateRange()[1])) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101435"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81874004080009", "日期范围不能为空!"));
        }
        if (StringUtils.isEmpty(flowBenchVO.getCurrency())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101436"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81877404080008", "折算币种不能为空!"));
        }
        if (StringUtils.isEmpty(flowBenchVO.getExchangeRateType())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101437"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C8187A204080001", "折算汇率类型不能为空!"));
        }
        if (StringUtils.isEmpty(flowBenchVO.getCurrencyUnit())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101438"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C8187BE04080002", "金额单位不能为空"));
        }
    }

}
