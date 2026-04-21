package com.yonyoucloud.fi.cmp.controller.bankreceipt;

import cn.hutool.core.lang.Assert;
import com.google.common.collect.Lists;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.vo.BankTranBatchAddVO;
import com.yonyoucloud.fi.cmp.vo.BankTranBatchUpdateVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName BankReceiptController
 * @Description 银行交易回单
 * @Author tongyd
 * @Date 2019/4/25 10:44
 * @Version 1.0
 **/
@Controller
@Slf4j
@Lazy
public class BankReceiptController extends BaseController {

    @Autowired
    private BankReceiptService bankReceiptService;

    /**
     * 批量新增银行回单
     *
     * @param bankTranBatchAddVOs 银行回单信息
     * @param response            保存是否成功
     */
    @PostMapping(value = "/bank/tran/add")
    public void bankTranBatchAdd(@Valid @RequestBody List<BankTranBatchAddVO> bankTranBatchAddVOs, HttpServletResponse response) throws Exception {
        log.error("批量新增银行回单信息,数量为:{}", CollectionUtils.size(bankTranBatchAddVOs));
        boolean bothCurrencyOrBankAccountNull =
                Optional.ofNullable(bankTranBatchAddVOs).orElse(Lists.newArrayList()).stream().anyMatch(b -> (StringUtils.isBlank(b.getCurrency())
                        && StringUtils.isBlank(b.getCurrency_code())) || (StringUtils.isBlank(b.getBankaccount()) && StringUtils.isBlank(b.getBankaccount_account()))
                );
        Assert.isFalse(bothCurrencyOrBankAccountNull, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80077", "币种或银行账号不能同时为空") /* "币种或银行账号不能同时为空" */);
        List<Object> ids = bankReceiptService.batchInsertReceipt(bankTranBatchAddVOs);
        renderJson(response, ResultMessage.data(ids));
    }

    /**
     * 批量更新银行回单
     *
     * @param bankTranBatchUpdateVOS 银行回单信息
     * @param response               保存是否成功
     */
    @PostMapping(value = "/bank/tran/update")
    public void bankTranBatchUpdate(@Valid @RequestBody List<BankTranBatchUpdateVO> bankTranBatchUpdateVOS, HttpServletResponse response) throws Exception {
        log.error("批量修改银行回单信息,数量为:{}", CollectionUtils.size(bankTranBatchUpdateVOS));
        List<String> ids = bankReceiptService.batchUpdateReceipt(bankTranBatchUpdateVOS);
        renderJson(response, ResultMessage.data(ids));
    }

    /*
     *@Description 电子回单查询
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/payment/queryReceiptDetail")
    public void queryAccountReceiptDetail(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, false);
        bankReceiptService.queryAccountReceiptDetailUnNeedUkey(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     *手动拉取回单
     * @Description 手动拉取回单
     * @Date 2019/9/18 13:42
     * @Param [response]
     * @Return void
     **/
    @PostMapping(value = "/payment/queryReceiptDetailUnNeedUkey")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void queryAccountReceiptDetailUnNeedUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, false);
        bankReceiptService.queryAccountReceiptDetailUnNeedUkey(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 电子回单下载
     *@Description 电子回单下载
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/payment/receiptDetailDownload")
    public void downloadAccountReceiptDetail(@RequestBody CtmJSONObject params, HttpServletResponse response, HttpServletRequest request) throws Exception {
        params.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        renderJson(response, ResultMessage.data(bankReceiptService.receiptDownForSpecial(params, response)));
    }

    /**
     * 电子回单批量下载
     *@Description 电子回单批量下载 yangjn
     *@Date 2021/5/24 15:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/payment/receiptDetailDownloadaBatch")
    public void downloadAccountReceiptDetailB(@RequestBody CtmJSONObject params, HttpServletResponse response, HttpServletRequest request) throws Exception {
        params.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        renderJson(response, ResultMessage.data(bankReceiptService.receiptDownBatchForSpecial(params, response)));
    }

    /**
     * @param params
     * @param response
     * @Description 电子回单下载 专属话用
     */
    @PostMapping(value = "/bankreceipt/receiptDownForSpecial")
    public void receiptDownForSpecial(@RequestBody CtmJSONObject params, HttpServletResponse response, HttpServletRequest request) throws Exception {
        params.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        renderJson(response, ResultMessage.data(bankReceiptService.receiptDownForSpecial(params, response)));
    }

    /**
     * @param params
     * @param response
     * @Description 电子回单批量下载 专属话用
     */
    @PostMapping(value = "/bankreceipt/receiptDownBatchForSpecial")
    public void receiptDownBatchForSpecial(@RequestBody CtmJSONObject params, HttpServletResponse response, HttpServletRequest request) throws Exception {
        params.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        renderJson(response, ResultMessage.data(bankReceiptService.receiptDownBatchForSpecial(params, response)));
    }

    /*
     *@Description 导入电子回单文件
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/bankreceipt/receiptUploadFile")
    public void receiptUploadFile(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        bankReceiptService.receiptUploadFile(params, response);
    }

    /*
     *@Description 根据电子回单文件名称返回银行交易回单主键
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/bankreceipt/getReceiptIdByFilename")
    public void getReceiptIdByFilename(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankReceiptService.getReceiptIdByFilename(params, response)));
    }

    /*
     *@Description 银行交易回单关联文件ID
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/bankreceipt/receiptAssociationFileId")
    public void receiptAssociationFileId(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankReceiptService.receiptAssociationFileId(params, response)));
    }

    /*
     *@Description 预览返回url
     *@Date 2019/9/18 13:42
     *@Param [response]
     *@Return void
     **/
    @PostMapping(value = "/bankreceipt/receiptPreviewFile")
    public void receiptPreviewFile(@RequestBody CtmJSONObject params, HttpServletResponse response, HttpServletRequest request) throws Exception {
        params.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        renderJson(response, ResultMessage.data(bankReceiptService.receiptPreviewFile(params, response)));
    }

    /**
     * 银行回单关联后台任务
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/bankreceipt/receiptRelate")
    public void relateBankReceiptTask(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        if (paramMap == null) {
            paramMap = new CtmJSONObject();
        }
        paramMap.put("tenantId", tenantId);
        paramMap.put("userId", userId);
        paramMap.put("logId", logId);
        paramMap.put("token", InvocationInfoProxy.getYhtAccessToken());
        // 通过银行回单关联银行交易明细给回单关联状态赋值
        CtmJSONObject jsonObject = bankReceiptService.relateBankReceiptDetail(paramMap);
        renderJson(response, CtmJSONObject.toJSONString(jsonObject));
    }


    /**
     * 银行回单关联url后台补偿任务
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/bankreceipt/urlCompensation")
    public void urlCompensation(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        if (paramMap == null) {
            paramMap = new CtmJSONObject();
        }
        paramMap.put("tenantId", tenantId);
        paramMap.put("userId", userId);
        paramMap.put("logId", logId);
        paramMap.put("token", InvocationInfoProxy.getYhtAccessToken());
        // 通过银行回单关联银行交易明细给回单关联状态赋值
        CtmJSONObject jsonObject = bankReceiptService.urlCompensation(paramMap);
        renderJson(response, CtmJSONObject.toJSONString(jsonObject));
    }


    /**
     * @param param
     * @param response
     * @throws Exception
     * @Description 批量预览 返回url
     */
    @PostMapping(value = "/bankreceipt/batchreceiptPreviewFile")
    public void batchreceiptPreviewFile(@RequestBody CtmJSONObject param, HttpServletResponse response, HttpServletRequest request) throws Exception {
        param.put(ICmpConstant.CALLBACKURL, request.getRequestURL().toString());
        CtmJSONArray jsonArray = bankReceiptService.batchreceiptPreviewFile(param, response);
        param.put("rows", jsonArray);
        param.put("count", jsonArray.size());
        renderJson(response, ResultMessage.data(param));
    }

    /**
     * 查询交易金额和数量
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/bankreceipt/queryElectronicReceiptStatistics")
    public void queryElectronicReceiptStatistics(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = bankReceiptService.queryElectronicReceiptStatistics(param, response);
        renderJson(response, ResultMessage.data(jsonObject));
    }

}
