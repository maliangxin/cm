package com.yonyoucloud.fi.cmp.controller.openapi;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.common.ResultList;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yht.entity.YhtSessionInfo;
import com.yonyou.yht.exception.YhtSessionException;
import com.yonyou.yht.sdk.YhtSessionCenter;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.bankdealdetail.SwitchDTO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.openapi.service.OpenApiExternalService;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginApiService;
import com.yonyoucloud.fi.cmp.paymargin.service.impl.PayMarginApiDto;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: guanqg
 * @Date: 2020/8/20 10:22
 */
@Controller
@RequestMapping("/api")
@Slf4j
@Lazy
public class    OpenApiExternalController extends BaseController {

    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    private OpenApiExternalService openApiExternalService;
    @Autowired
    private PayMarginApiService payMarginApiService;

    @Autowired
    private SwitchDTO switchDTO;


    /**
     * 收款单保存
     * @param bill
     * @param request
     * @param response
     */
    @RequestMapping("/bill/save")
    public void save(@RequestBody BillDataDto bill, HttpServletRequest request,
                     HttpServletResponse response) {
        try {
            ResultList result = fiBillService.batchSave(bill);
            if (result.getFailCount() > 0) {
                renderJson(response, ResultMessage.error((String) result.getMessages().get(0)));
            } else {
                renderJson(response, ResultMessage.data(result));
            }
        } catch (Exception e) {
            renderJson(response, ResultMessage.error(e.getMessage()));
        }
    }


    /**
     * 根据银行流水号获取银行回单
     *
     * @param
     * @param request
     * @param response
     */
    @GetMapping(value = "/bill/BankElectronByBankSeq/pdf")
    public void BankElectronBankSeq(String bankseq, String bankid, HttpServletRequest request,
                                    HttpServletResponse response) {
        CtmJSONObject obj = openApiExternalService.queryBankelectronicreceiptPDFByBillBankSeq(bankseq, bankid, response);
        renderJson(response, CtmJSONObject.toJSONString(obj));
    }

    /**
     * 根据银行对账单ID获取交易回单
     *
     * @param
     * @param request
     * @param response
     */
    @GetMapping(value = "/bill/BankElectronByBankSeq/detailByBankid")
    public void BankElectronBankSeq(String bankBillId, HttpServletRequest request,
                                    HttpServletResponse response) {
        CtmJSONObject obj = openApiExternalService.queryBankelectronicreceiptPDFByBillBillId(bankBillId, response);
        renderJson(response, CtmJSONObject.toJSONString(obj));
    }

    /**
     * 根据企业流水号获取银行回单
     * 四方使用
     * @param
     * @param request
     * @param response
     */
    @GetMapping(value = "/bill/BankElectronByReqSeq/pdf")
    public void BankElectronByReqSeq(String bankseq, String bankid, HttpServletRequest request,
                                     HttpServletResponse response) {
        CtmJSONObject  obj = openApiExternalService.queryBankelectronicreceiptPDFByReqSeq(bankseq, bankid, response);
        renderJson(response, CtmJSONObject.toJSONString(obj));
    }

    /**
     * 获取是否用友系统支付单据
     *
     * @param
     * @param request
     * @param response
     */
    @GetMapping(value = "/bill/isYonyouPay")
    public void isYonyouPay(String bankseq, HttpServletRequest request,
                            HttpServletResponse response) {
        try {
            boolean f = openApiExternalService.isYonyouPay(bankseq, null);
            CtmJSONObject responseMsg = new CtmJSONObject();
            responseMsg.put("status", f);
            renderJson(response, CtmJSONObject.toJSONString(responseMsg));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180346","查询账户电子回单异常：") /* "查询账户电子回单异常：" */ + e.getMessage()));
        }
    }

    /**
     * 获取交易明细
     *
     * @param
     * @param request
     * @param response 对应api为cmpBankTradeDetailElectron/list 获取银行交易明细
     */
    @RequestMapping("/bill/BankTradeDetailElectron/detail")
    public void BankTradeDetailElectron(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                        HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(openApiExternalService.queryBankTradeDetailElectron(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180346","查询账户电子回单异常：") /* "查询账户电子回单异常：" */ + e.getMessage()));
        }
    }

    /**
     * 定时任务 获取交易明细  启动线程
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/BankTradeDetailElectron/list")
    public void BankTradeDetailElectronList(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
//        new BankTradeDetailElectronThread(param).start();  // 定时任务 子线程
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            log.error("bill BankTradeDetailElectron list start ==================：" + param.toString());
            if (param == null){
                param = new CtmJSONObject();
            }
            if (param.get("startDate") == null || param.get("endDate") == null) {
                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String today = dateFormat.format(now);
                param.put("startDate", today);//
                param.put("endDate", today);//
            }
            param.put("logId",logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            Map<String,Object> result = openApiExternalService.queryBankTradeDetailElectronList(param,false);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                    InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80071", "执行失败") /* "执行失败" */) /* "执行失败" */
                            + "[Failure Reason]"+ e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
    }

    /**
     * 定时任务 银行账户历史交易明细查询
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/HistoryBankTradeDetailElectron/list")
    public void HistoryBankTradeDetailElectronList(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            log.error("bill HistoryBankTradeDetailElectron list start ==================：" + param.toString());
            if (param == null) {
                param = new CtmJSONObject();
            } else {
                //任务参数校验
                TaskUtils.dateCheck(param);
            }
            param.put("logId", logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            Map<String, Object> result = openApiExternalService.queryBankTradeDetailElectronList(param, true);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D00022", "获取银行账户历史交易明细任务错误：") /* "获取银行账户历史交易明细任务错误：" */ + e.getMessage()));
        }
    }

    /**
     * 定时任务 获取回单  启动线程
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/ReceiptDetail/list")
    public void ReceiptDetailList(@RequestBody CtmJSONObject param,HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            //任务参数校验
            TaskUtils.dateCheck(param);
            param.put("logId",logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            Map<String, Object> result = new HashMap<>();
//            if (switchDTO.getMultiThreads()) {
//                openApiExternalService.ReceiptDetailList(param);
//            } else {
                result = openApiExternalService.ReceiptDetailListThread(param);
//            }
//            CtmJSONObject responseMsg = new CtmJSONObject();
//            responseMsg.put("status", 1);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D00021", "获取电子回单任务错误：") /* "获取电子回单任务错误：" */ + e.getMessage()));
        }
    }

    /**
     * 批量查询支付状态
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/billStatus/detail")
    public void billStatus(@RequestBody CtmJSONObject param, HttpServletRequest request,
                           HttpServletResponse response) {
        try {
//            List list = openApiExternalService.queryBankTradeDetailElectron(param);
            renderJson(response, ResultMessage.data(openApiExternalService.billStatus(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180349","查询异常") /* "查询异常" */));
        }
    }

    /**
     * 批量查询支付状态
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/billStatus/transfer")
    public void transferStatus(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(openApiExternalService.billTransferStatus(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180349","查询异常") /* "查询异常" */));
        }
    }

    /**
     * 批量查询支付状态 -- 收款单
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/billStatus/receive")
    public void receiveStatus(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(openApiExternalService.billReceiveStatus(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C1696A04500003","查询异常:") /* "查询异常" */ + e.getMessage()));
        }
    }

    /**
     * 查询支付状态  定时任务
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/payStatus/list")
    public void payStatus(@RequestBody CtmJSONObject param, HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        //todo 异步  加锁: 加异步里面 为了防止多次点击
        //new BankPaystatusThread(param).start();
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        ExecutorService autoQueryPayStatus = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 2, 200, "AutoQueryPayStatus-threadpool");
        autoQueryPayStatus.submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock(ICmpConstant.SERVICECODE_SALPAY, 2 * 60 * 60L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId,
                                InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80070", "系统正在对此单据更新中") /* "系统正在对此单据更新中" */) /* "系统正在对此单据更新中" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    try {
                        List<String> msg = openApiExternalService.payStatusList(param);
//                        CtmJSONObject responseMsg = new CtmJSONObject();
//                        responseMsg.put("status", 1);
//                        renderJson(response, CtmJSONObject.toJSONString(responseMsg));
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        if(!msg.isEmpty()){
                            String msgStr = MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989");
                            StringBuilder msgBuilder = new StringBuilder();
                            msgBuilder.append(msgStr + " ");
                            for (String s : msg) {
                                msgBuilder.append( s);
                            }
                            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, msgBuilder.toString() , TaskUtils.UPDATE_TASK_LOG_URL);
                        }
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, InternationalUtils.getMessageWithDefault("P_YS_OA_app_xtyyjm_0000035989","执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);

                    } catch (Exception e) {
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    } finally {
                        if (autoQueryPayStatus != null) {
                            autoQueryPayStatus.shutdown();
                        }
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
//                renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "查询支付状态定时任务错误：") /* "下载银行回单pdf定时任务错误：" */ + e.getMessage()));

            }

        });
        ObjectNode result = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
//        openApiExternalService.payStatusList(param);
//        CtmJSONObject responseMsg = new CtmJSONObject();
//        responseMsg.put("status", 1);
//        renderJson(response, CtmJSONObject.toJSONString(responseMsg));

    }

    /**
     * 定时任务 下载银行回单pdf 后上传到服务器
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/bankElectronByReqSeq/task/pdf")
    @Authentication(value = false, readCookie = true)
    public void bankTradeDetailElectronTask(@RequestBody(required = false) CtmJSONObject param, HttpServletRequest request,
                                                          HttpServletResponse response) throws Exception {
        log.error("bill BankTradeDetailElectronTask list start ==================：" + param.toString());
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            if(null == param){
                param = new CtmJSONObject();
            }
            param.put("tenantId",tenantId);
            param.put("userId",userId);
            param.put("logId",logId);
            param.put("token", InvocationInfoProxy.getYhtAccessToken());
            //任务日期参数校验
            TaskUtils.dateCheck(param);
            CtmJSONObject jsonObject = openApiExternalService.bankTradeDetailElectronTask(param);
            renderJson(response, CtmJSONObject.toJSONString(jsonObject));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418034A","下载银行回单pdf定时任务错误：") /* "下载银行回单pdf定时任务错误：" */ + e.getMessage()));
        }
    }

    /**
     * 校验友互通token是否有效
     * @param token
     * @return
     */
    public boolean checkTokenValid(String token){
        // ture说明token有效，false说明token无效
        boolean status = YhtSessionCenter.verifyYhtSession(token);
        if(!status){
            log.error("checkTokenValid status");
            return false;
        }

        YhtSessionInfo yhtSessionInfo;
        try{
            yhtSessionInfo = YhtSessionCenter.getYhtSession(token);
        } catch (YhtSessionException e){
            log.error("获取yhtAccessToken用户信息失败,accessToken:{}", token,e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102040"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080094", "获取yhtAccessToken用户信息失败") /* "获取yhtAccessToken用户信息失败" */,e);
        }
        String userId = yhtSessionInfo.getUserid();
        if(StringUtils.isEmpty(userId)){
            log.error("checkTokenValid userId："+userId);
            return false;
        }
        return true;
    }

    /**
     * 采集任务对外接口
     * @param param
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/bill/bankElectronicReceipt/file")
//    @Authentication(value = false, readCookie = true)
    public void bankElectronicReceiptFile(@RequestBody(required = false) CtmJSONObject param, HttpServletRequest request,
                                                          HttpServletResponse response) throws Exception {
       /* CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put(ICmpConstant.CODE,"0000");
        if(!checkTokenValid(AppContext.getToken())){
            jsonObject.put(ICmpConstant.MSG,"友互通校验不通过");
            renderJson(response, jsonObject.toJSONString());
        }else{
            Map<String,Object> result = openApiExternalService.bankElectronicReceiptFile(param);
            renderJson(response, CtmJSONObject.toJSONString(result));
        }*/
        Object requestType = param.get(ICmpConstant.REQUEST_TYPE);
        if (requestType != null && "2".equals(requestType.toString())) {
            param.put(ICmpConstant.QUERY_NUM_FLAG, true);
        } else {
            param.put(ICmpConstant.QUERY_NUM_FLAG, false);
        }
        Map<String,Object> result = new HashMap<>();
        try {
            param.put(ICmpConstant.TOKEN, InvocationInfoProxy.getYhtAccessToken());
            result = openApiExternalService.bankElectronicReceiptFile(param);
        } catch (Exception e) {
            log.error("=================bankElectronicReceipt file bankElectronicReceiptFile:", e);
            result.put(ICmpConstant.CODE,"9999");
            String message = e.getMessage();
            String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 300 ? message.substring(300) : message) : null;
            result.put(ICmpConstant.MSG,resultMsg);
            renderJson(response, CtmJSONObject.toJSONString(result));
            return;
        }
        renderJson(response, CtmJSONObject.toJSONString(result));
    }
    /**
     * 采集任务一键补采对外接口
     * @param param
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/bill/repairBankElectFile/file")
    public void repairBankElectFile(@RequestBody(required = false) CtmJSONObject param, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {

        Map<String,Object> result = new HashMap<>();
        try {
            param.put(ICmpConstant.TOKEN, InvocationInfoProxy.getYhtAccessToken());
            result = openApiExternalService.repairBankElectFile(param);
        } catch (Exception e) {
            log.error("=================repairBankElectFile file repairBankElectFile:", e);
            result.put(ICmpConstant.CODE,"9999");
            String message = e.getMessage();
            String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 300 ? message.substring(300) : message) : null;
            result.put(ICmpConstant.MSG,resultMsg);
            renderJson(response, CtmJSONObject.toJSONString(result));
            return;
        }
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    /**
     * 测试 系统间的连通性
     * @param param
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/archive/check")
    @Authentication(value = false, readCookie = true)
    public void archiveCheck(@RequestBody(required = false) CtmJSONObject param, HttpServletRequest request,
                                                          HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put(ICmpConstant.CODE,"0000");
        Map<String,Object> resultMap = new HashMap<>();
        if(checkTokenValid(InvocationInfoProxy.getYhtAccessToken())){
            jsonObject.put(ICmpConstant.MSG,ICmpConstant.SUCCESS);
            resultMap.put(ICmpConstant.STATUS,true);
        }else{
            jsonObject.put(ICmpConstant.CODE,"9999");
            jsonObject.put(ICmpConstant.MSG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180347","友互通校验不通过") /* "友互通校验不通过" */);
            renderJson(response, CtmJSONObject.toJSONString(jsonObject));
            resultMap.put(ICmpConstant.STATUS,false);
        }
        jsonObject.put(ICmpConstant.DATAS,resultMap);
        renderJson(response, CtmJSONObject.toJSONString(jsonObject));
    }

    /**
     *  子线程先注释
     */
//    private class ReceiptDetailThread extends Thread {
//        private CtmJSONObject param;
//        ReceiptDetailThread(CtmJSONObject param){
//            this.param = param;
//        }
//        @Override
//        public void run() {
//            try {
//                if (param==null||param.get("startDate")==null||param.get("endDate")==null) {
//                    param = new CtmJSONObject();
//                    Calendar cal = Calendar.getInstance();
//                    cal.add(Calendar.DATE, -1);
//                    Date d = cal.getTime();
//                    SimpleDateFormat sp = new SimpleDateFormat("yyyy-MM-dd");
//                    String yestday = sp.format(d);//获取昨天日期
//                    param.put("startDate", yestday);//today-1
//                    param.put("endDate", yestday);//today-1
//                }
//                log.error("银行回单定时任务执行参数.."+param.toString());
//                openApiExternalService.ReceiptDetailList(param);
//            }catch (Exception e){
//                log.error(e.getMessage());
//            }finally {
//                log.error("银行回单定时任务执行结束。。。。。");
//            }
//        }
//    }

    /**
     *  子线程先注释
     */
//    private class BankTradeDetailElectronThread extends Thread {
//        private CtmJSONObject param;
//        BankTradeDetailElectronThread(CtmJSONObject param){
//            this.param = param;
//        }
//        @Override
//        public void run() {
//            try {
//                if (param==null||param.get("startDate")==null||param.get("endDate")==null){
//                    param =new CtmJSONObject();
//                    Date now=new Date();
//                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//                    String today=dateFormat.format(now);
//                    param.put("startDate",today);//
//                    param.put("endDate",today);//
//                }
//                openApiExternalService.queryBankTradeDetailElectronList(param);
//            }catch (Exception e){
//                log.error(e.getMessage());
//            }finally {
//                log.error("银行交易流水定时任务执行结束。。。。。");
//            }
//        }
//    }

    /**
     *  子线程先注释
     */
//    private class BankPaystatusThread extends Thread {
//        private CtmJSONObject param;
//        BankPaystatusThread(CtmJSONObject param){
//            this.param = param;
//        }
//        @Override
//        public void run() {
//            try {
//                openApiExternalService.payStatusList(param);
//            }catch (Exception e){
//                log.error(e.getMessage());
//            }finally {
//                log.error("银行支付状态定时任务执行结束。。。。。");
//            }
//        }
//    }

    /**
     * 采集任务对外接口 余额调节表 + 电子对账单确认
     *
     * @param param
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/bill/balanceAdjustResult/file")
    public void balanceAdjustResultFile(@RequestBody(required = false) CtmJSONObject param, HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {
        Map<String, Object> result = new HashMap<>();
        Object requestType = param.get(ICmpConstant.REQUEST_TYPE);
        if (requestType != null && "2".equals(requestType.toString())) {
            param.put(ICmpConstant.QUERY_NUM_FLAG, true);
        } else {
            param.put(ICmpConstant.QUERY_NUM_FLAG, false);
        }
        try {
            param.put(ICmpConstant.TOKEN, InvocationInfoProxy.getYhtAccessToken());
            //银行余额调节表（YHYETJB），银行对账单(YHDZD)
            Object type = param.get(ICmpConstant.TYPE);
            if ("YHYETJB".equals(type)) {
                result = openApiExternalService.balanceAdjustResultFile(param);
            } else {
                result = openApiExternalService.electronicStatementConfirmFile(param);
            }
        } catch (Exception e) {
            log.error("=================balanceAdjustResult file balanceAdjustResultFile:", e);
            result.put(ICmpConstant.CODE, "9999");
            String message = e.getMessage();
            String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 300 ? message.substring(300) : message) : null;
            result.put(ICmpConstant.MSG, resultMsg);
            renderJson(response, CtmJSONObject.toJSONString(result));
            return;
        }
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    /**
     * OpenApi “支付保证金业务台账”新增接口
     */
    @PostMapping("/bill/paymargin/save")
    public void savePayMargin(@RequestBody PayMarginApiDto param, HttpServletResponse response) {
        Result result = null;
        try {
            Map<String, Object> data = payMarginApiService.saveBill(param);
            result = Result.ok(data);
        }catch (Exception e){
            log.error("PayMargin saveBill error", e);
            result = Result.error("999",e.getMessage());
        }
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    /**
     * OpenApi “支付保证金业务台账”删除接口
     * @param param 待删除实体标识字段
     */
    @PostMapping("/bill/paymargin/delete")
    public void deletePayMargin(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        Result result = null;
        try{
            List<String> ids = payMarginApiService.deleteBill(param);
            result = Result.ok(ids);
        }catch (Exception e){
            log.error("PayMargin deleteBill error", e);
            result = Result.error(e.getMessage());
        }
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

}
