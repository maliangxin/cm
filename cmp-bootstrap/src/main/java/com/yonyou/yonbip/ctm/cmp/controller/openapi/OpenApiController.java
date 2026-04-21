package com.yonyou.yonbip.ctm.cmp.controller.openapi;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.common.ResultList;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiService;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yonyou.yonbip.ctm.error.CommonCtmErrorCode.REMOTE_SERVICE_REST_EXCEPTION;

/**
 * @ClassName OpenApiController
 * @Description OpenApi相关的controller
 * @Author sz
 * @Date 2019/6/5 10:44
 * @Version 1.0
 * 修改记录：去掉注解@Authentication(false),走友互通登录
 **/
@Controller
@RequestMapping("/authapi")
@Slf4j
public class OpenApiController extends BaseController {

    @Autowired
    private OpenApiService openApiService;
    @Autowired
    private IFIBillService fiBillService;

    /**
     * 收款单删除
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/receivebill/deleteByIds")
    public void deleteReceiveBillByIds(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.deleteReceiveBillByIds(param));
        } catch (Exception e) {
            log.error("现金管理-收款单删除异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CF","现金管理-收款单删除异常：") /* "现金管理-收款单删除异常：" */ + e.getMessage()));
        }
    }


    /**
     * 外部服务，生成薪资支付单
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/salarypay/createSalaryPays")
    public void salaryPayCreate(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
//        	log.debug("接收外部服务传输数据：" + param);
            renderJson(response, openApiService.salaryPayCreate(param));
        } catch (Exception e) {
            log.error("薪资支付生成单据异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CA","现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
        }
    }

    /**
     * 外部服务，根据来源删除薪资支付单
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/salarypay/deleteByIds")
    public void salaryPayDeleteByIds(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.deleteSalaryPayByIds(param));
        } catch (Exception e) {
            log.error("薪资支付删除单据异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C8","未知错误：") /* "未知错误：" */ + e.getMessage()));
        }
    }


    @RequestMapping("/bill/save")
    public void save(@RequestBody BillDataDto bill, HttpServletRequest request,
                     HttpServletResponse response) {
        try {
            ResultList result = fiBillService.batchSave(bill);
            if(result.getFailCount()>0){
                renderJson(response, ResultMessage.error((String)result.getMessages().get(0)));
            }else {
                renderJson(response, ResultMessage.data(result));
            }
        } catch (Exception e) {
            renderJson(response, ResultMessage.error(e.getMessage()));
        }
    }

}
