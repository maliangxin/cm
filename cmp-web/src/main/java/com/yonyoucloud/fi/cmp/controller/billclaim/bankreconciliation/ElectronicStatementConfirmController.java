package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.service.ElectronicStatementConfirmService;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * @ClassName ElectronicStatementConfirmController
 * @Description 电子对账单确认controller
 * @Author tongyd
 * @Date 2019/4/25 10:44
 * @Version 1.0
 **/
@Controller
@Slf4j
public class ElectronicStatementConfirmController extends BaseController {

    @Autowired
    private ElectronicStatementConfirmService electronicStatementConfirmService;

    /*
     *@Description 对账单查询
     *@Date 2023/8/8 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/elecstatement/queryElecStatement")
    public void queryElecStatement(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response,
                ResultMessage.data(electronicStatementConfirmService.queryElecStatement(params)));
    }

    /**
     * 电子对账单手动拉取
     *
     * @param
     * @throws
     */
    @PostMapping(value = "/elecstatement/queryElecStatementUnNeedUkey")
    @DiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void queryAccountReceiptDetailUnNeedUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response,
                ResultMessage.data(electronicStatementConfirmService.queryElecStatementUnNeedUkey(params)));
    }

    /*
     *@Description 对账单文件下载
     *@Date 2023/8/8 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/elecstatement/elecStatementFileDownload")
    public void downloadElecStatementFile(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
//            electronicStatementConfirmService.downloadElecStatementFile(params, response);
        renderJson(response, ResultMessage.data(electronicStatementConfirmService.downloadElecStatementFile(params, response)));
    }

    /*
     *@Description 电子回单批量下载
     *@Date 2023/8/8 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/elecstatement/elecStatementFileDownloadBatch")
    public void downloadElecStatementFileB(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        electronicStatementConfirmService.downloadElecStatementFileBatch(params, response);
    }

    /*
     *@Description 根据电子对账单文件名称返回电子对账单实体主键
     *@Date 2024年10月11日13:54:50
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/elecstatement/getIdByFilename")
    public void getReceiptIdByFilename(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(electronicStatementConfirmService.getIdByFilename(params, response)));
    }

    /*
     *@Description 银行交易回单关联文件ID
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/elecstatement/receiptAssociationFileId")
    public void receiptAssociationFileId(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(electronicStatementConfirmService.receiptAssociationFileId(params, response)));
    }

    /*
     *@Description 预览返回url
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/elecstatement/receiptPreviewFile")
    public void receiptPreviewFile(@RequestBody CtmJSONObject params, HttpServletResponse response, HttpServletRequest request) throws Exception {
        params.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        renderJson(response, ResultMessage.data(electronicStatementConfirmService.receiptPreviewFile(params, response)));
    }

    /**
     * 调度任务“银行电子对账单查询”，支持按照资金组织、期间、银行类别、银行账号等维度查询，最小时间间隔分钟；调度任务预置，时间间隔15分钟.
     *
     * @param params
     * @param response
     */
    @PostMapping(value = "/elecstatement/scheduleQueryElecStatement")
    public void scheduleQueryElecStatement(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        params.put("logId", logId);
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
        renderJson(response,
                CtmJSONObject.toJSONString(electronicStatementConfirmService.scheduleQueryElecStatement(params)));
    }

    /**
     * 调度任务“银行电子对账单文件下载”，支持按照资金组织、期间、银行类别、银行账号等维度查询；最小时间间隔分钟；预置调度任务，时间间隔15分钟
     *
     * @param params
     * @param response
     */
    @PostMapping(value = "/elecstatement/scheduleStatementFileDownload")
    public void scheduleStatementFileDownload(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        params.put("logId", logId);
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
        renderJson(response,
                CtmJSONObject.toJSONString(electronicStatementConfirmService.scheduleStatementFileDownload(params, response)));
    }

}
