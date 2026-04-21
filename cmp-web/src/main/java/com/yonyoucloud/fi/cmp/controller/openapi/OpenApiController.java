package com.yonyoucloud.fi.cmp.controller.openapi;

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
import java.util.*;

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
     * 外部服务，生成付款单
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/payment/received")
    public void received(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String decodeData = new String(Base64.getMimeDecoder().decode(param.get("data").toString()),"UTF-8");
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(param.getString("billnum"));
            bill.setData(decodeData);
            fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
            renderJson(response, ResultMessage.success());
        } catch (Exception e) {
            log.error("现金管理生成单据异常：" + e.getMessage(),e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F7", "现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
        }
    }

    /**
     * 外部服务，根据来源删除付款单
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/payment/deleteByIds")
    public void deleteByIds(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            List jSONObject = openApiService.deleteByIds(param);
            renderJson(response, ResultMessage.data(jSONObject));
        } catch (Exception e) {
            log.error("调用OpenApi删除付款单据异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D1","现金管理-付款单删除异常：") /* "现金管理-付款单删除异常：" */ + e.getMessage()));
        }
    }

    /**
     * 外部服务，根据来源查询付款单付款状态
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/payment/querystatusByIds")
    public void querystatusByIds(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            List jSONObject = openApiService.querystatusByIds(param);
            renderJson(response, ResultMessage.data(jSONObject));
        } catch (Exception e) {
            log.error("未知错误：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C8","未知错误：") /* "未知错误：" */ + e.getMessage()));
        }
    }

    /**
     * 生成收款单
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/receivebill/received")
    public void receivebillCreate(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.insertReceiveBill(param));
        } catch (Exception e) {
            log.error("现金管理-收款单生成异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802C7","现金管理-收款单生成异常：") /* "现金管理-收款单生成异常：" */ + e.getMessage()));
        }
    }

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
     * 收款单状态查询
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/receivebill/queryByIds")
    public void queryReceiveBillStatusByIds(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.queryReceiveBillStatusByIds(param));
        } catch (Exception e) {
            log.error("现金管理-收款单查询异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CB","现金管理-收款单查询异常：") /* "现金管理-收款单查询异常：" */ + e.getMessage()));
        }
    }

    /**
     * 票据保证金生成转账单和日记账余额
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/transfer/createTransfer")
    public void createTransfer(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception{
        try{
            String decodeData = new String(Base64.getMimeDecoder().decode(param.get("data").toString()),"UTF-8");
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(param.getString("billnum"));
            bill.setData(decodeData);
            fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), bill);
            renderJson(response, ResultMessage.success());
        } catch (Exception e) {
            log.error("现金管理-转账单生成异常: " + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D2","现金管理-转账单生成异常") /* "现金管理-转账单生成异常" */ + e.getMessage()));
        }
    }
    /**
     * 票据保证金删除转账单和日记账余额
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/transfer/deleteTransfer")
    public void deleteTransfer(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception{
        try{
            renderJson(response, openApiService.deleteTransfer(param));
        }catch (Exception e){
            log.error("现金管理-转账单删除异常: " + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CD","现金管理-转账单删除异常") /* "现金管理-转账单删除异常" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CE","：") /* "：" */ + e.getMessage()));

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
    @RequestMapping("/salarypay/insertByHR")
    public void salaryPayReceived(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
//        	log.debug("薪资支付传输数据：", param);
            renderJson(response, openApiService.insertSalaryPay(param));
        } catch (Exception e) {
            log.error("薪资支付生成单据异常：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CA","现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
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

    /**
     * 外部服务，根据来源查询薪资支付单付款状态
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
//    @Authentication(false)
    @RequestMapping("/salarypay/querystatusByIds")
    public void salaryPayQuerystatusByIds(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.querySalaryPayStatusByIds(param));
        } catch (Exception e) {
            log.error("薪资支付查询支付状态异常：" + e.getMessage(), e);
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

    /**
     * <h2>根据账号查询期初数据</h2>
     *
     * @param param: 入参
     * @param request: 请求对象
     * @param response: 响应对象
     * @return void
     * @author Sun GuoCai
     * @since 2021/4/1 17:05
     */
    @RequestMapping("/initdata/queryDataByAccountNo")
    public void queryInitDataByAccountNo(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.queryInitDataByAccountNo(param));
        } catch (Exception e) {
            log.error("根据账号查询期初数据失败：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(REMOTE_SERVICE_REST_EXCEPTION.build(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FA", "根据账号查询期初数据失败!") /* "根据账号查询期初数据失败!" */));
        }
    }

    /**
     * <h2>根据传入付款工作台id修改单据凭证状态（报销凭证生成时调用）</h2>
     *
     * @param param: 入参
     * @param request: 请求对象
     * @param response: 响应对象
     * @return void
     * @since 2021/4/26 17:05
     */
    @RequestMapping("/payment/updatePayBillVoucherStatus")
    public void updatePayBillVoucherStatus(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            renderJson(response, openApiService.updatePayBillVoucherStatus(param));
        } catch (Exception e) {
            log.error("更新付款工作台凭证状态失败：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(REMOTE_SERVICE_REST_EXCEPTION.build(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FC", "更新付款工作台凭证状态失败!") /* "更新付款工作台凭证状态失败!" */));
        }
    }

    /**
     * 根据传入的资金组织id 查询现金会计主体的启用期间(外系统调用)
     * @param param
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/cmp/getCmpBeginDate")
    public void getCmpBeginDate(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = new CtmJSONObject();
        try{
            String orgid = param.getString("orgId");
            Date beginDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(orgid);
            if(beginDate!=null){
                jsonObject.put("beginDate",beginDate);
            }else{
                jsonObject.put("beginDate",null);
            }
            renderJson(response, ResultMessage.data(jsonObject));
        }catch(Exception e){
            log.error("获取会计期间失败：" + e.getMessage(), e);
//            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802CC","未能获取到会计主体对应的会计期间！") /* "未能获取到会计主体对应的会计期间！" */));
            String orgid = param.getString("orgId");
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{orgid}));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            String msg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name);
            renderJson(response, ResultMessage.error(msg));
        }
    }

    /**
     * 外部服务，根据id删除薪资支付单
     * @param param 入参
     * @param response 响应对象
     * @throws Exception
     */
    @RequestMapping("/salarypay/deleteById")
    public void salaryPayDeleteById(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
            CtmJSONObject jsonObject = openApiService.deleteSalaryPayById(param);
            renderJson(response, ResultMessage.data(jsonObject));
    }


}
