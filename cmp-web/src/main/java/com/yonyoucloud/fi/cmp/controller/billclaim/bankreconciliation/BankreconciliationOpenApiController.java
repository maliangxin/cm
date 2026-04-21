package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;


import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiBankReconciliationService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.openapi.service.BankreconciliationOpenApiService;
import com.yonyoucloud.fi.cmp.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;

/**
 * @author qihaoc
 * @Description: 银行对账单openAPI接口
 * @date 2022/10/5 14:54
 */
@RestController
@RequestMapping("/api/bankreconciliation/")
@Slf4j
public class BankreconciliationOpenApiController extends BaseController {

    @Autowired
    private BankreconciliationOpenApiService service;

    @Autowired
    private OpenApiBankReconciliationService bankReconciliationService;

    /**
     * 银行对账单新增
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/insert")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public Result save(@RequestBody CtmJSONObject param, HttpServletRequest request,
                       HttpServletResponse response) {
        try {
            return service.insert(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
                    + e.getMessage());
        }
    }


    /**
     * 银行对账单删除
     *
     * @param param    param中取idList 银行对账单主键数组 和 bankSeqNoList 银行对账单银行流水号数组 的交集，只有一个参数时只使用对应参数
     * @param request
     * @param response
     */
    @RequestMapping("/delete")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public Result delete(@RequestBody CtmJSONObject param, HttpServletRequest request,
                         HttpServletResponse response) {
        try {
            return service.delete(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
                    + e.getMessage());
        }
    }

    /**
     * 银行对账单执行辨识规则
     *
     * @param param    param中取idList 银行对账单主键数组 和 bankSeqNoList 银行对账单银行流水号数组，只有一个参数时只使用对应参数
     * @param request
     * @param response
     */
    @RequestMapping("/executeRule")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public Result executeRule(@RequestBody CtmJSONObject param, HttpServletRequest request,
                              HttpServletResponse response) {
        try {
            HashMap<String, Object> map = service.executeRule(param);
            return Result.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
                    + e.getMessage());
        }
    }

//    /**
//     * 手工冻结单据
//     *
//     * @param param    param中取idList 银行对账单主键数组 和 bankSeqNoList 银行对账单银行流水号数组，只有一个参数时只使用对应参数
//     * @param request
//     * @param response
//     */
//    @RequestMapping("/freeze")
//    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
//    public Result freeze(@RequestBody CtmJSONObject param, HttpServletRequest request,
//                         HttpServletResponse response) {
//        try {
//            HashMap<String, Object> map = service.freeze(param);
//            return Result.ok(map);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
//                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
//                    + e.getMessage());
//        }
//    }
//
//    @RequestMapping("/unfreeze")
//    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
//    public Result unfreeze(@RequestBody CtmJSONObject param) {
//        try {
//            HashMap<String, Object> map = service.unfreeze(param);
//            return Result.ok(map);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
//                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
//                    + e.getMessage());
//        }
//    }

    /**
     * 银行对账单查询
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/querylist")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public CtmJSONObject querylist(@RequestBody CtmJSONObject param, HttpServletRequest request,
                       HttpServletResponse response) {
        try {
            return service.querylist(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            CtmJSONObject result = new CtmJSONObject();
            result.put("code",999);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 银行对账单到账认领中心查询
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/queryclaimcenterlist")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public CtmJSONObject queryClaimCenterList(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                HttpServletResponse response) {
        try {
            log.info("银行对账单到账认领中心查询入参", param);
            return service.queryClaimCenterList(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            CtmJSONObject result = new CtmJSONObject();
            result.put("code",999);
            result.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D0001D", "服务端逻辑异常：") /* "服务端逻辑异常：" */ + e.getMessage());
            return result;
        }
    }

    /**
     * 银行对账单新增关联关系
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/insertrelation")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public CtmJSONObject insertrelation(@RequestBody CtmJSONArray param, HttpServletRequest request,
                                     HttpServletResponse response) {
        try {
            CtmJSONObject result = new CtmJSONObject();
            if(param == null || param.isEmpty()){
                result.put("code",999);
                result.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180744","请求参数不能为空!") /* "请求参数不能为空!" */);
                return result;
            }
            log.error("银行对账单新增关联关系入参:{}", JSONObject.toJSONString(param));
            List<HashMap<String, String>> msg = service.insertrelation(param);
            result.put("code",200);
            result.put("data",msg);
            result.put("message","success");
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            CtmJSONObject result = new CtmJSONObject();
            result.put("code",999);
            result.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D0001D", "服务端逻辑异常：") /* "服务端逻辑异常：" */ + e.getMessage());
            return result;
        }
    }


    /**
     * 银行对账单批量新增
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/batchinsert")
    public CtmJSONObject batchSave(@RequestBody CtmJSONArray param, HttpServletRequest request,
                          HttpServletResponse response) {
        try {
            if(param == null || param.isEmpty()){
                CtmJSONObject ctmJSONObject = new CtmJSONObject();
                ctmJSONObject.put("code",999);
                ctmJSONObject.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009C", "参数不能为空！") /* "参数不能为空！" */);
                return ctmJSONObject;
            }
            return bankReconciliationService.batchInsert(param);
        } catch (Exception e) {
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("code",999);
            ctmJSONObject.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009B", "服务端逻辑异常！") /* "服务端逻辑异常！" */+e);
            return ctmJSONObject;
        }
    }


    /**
     * 银行对账单批量更新
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/batchupdate")
    public CtmJSONObject batchUpdate(@RequestBody CtmJSONArray param, HttpServletRequest request,HttpServletResponse response) {
        try {
            if(param == null || param.isEmpty()){
                CtmJSONObject ctmJSONObject = new CtmJSONObject();
                ctmJSONObject.put("code",999);
                ctmJSONObject.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009C", "参数不能为空！") /* "参数不能为空！" */);
                return ctmJSONObject;
            }
            return service.batchUpdate(param);
        } catch (Exception e) {
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("code",999);
            ctmJSONObject.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009B", "服务端逻辑异常！") /* "服务端逻辑异常！" */+e);
            return ctmJSONObject;
        }
    }

    /**
     * 到账认领中心流水批量认领
     * 针对已发布到账认领中心且未认领完成的流水，自动认领生成认领单。
     * 支持整单认领、合并认领、部分认领
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/batchclaim")
    public CtmJSONObject batchClaim(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                    HttpServletResponse response) {
        try {
            if(param == null || param.isEmpty()){
                CtmJSONObject ctmJSONObject = new CtmJSONObject();
                ctmJSONObject.put("code",999);
                ctmJSONObject.put("message", MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400600","参数不能为空！") /* "参数不能为空！" */));
                return ctmJSONObject;
            }
            return service.batchClaim(param);
        } catch (Exception e) {
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("code",999);
            ctmJSONObject.put("message",e.getMessage()==null?
                    MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227D62C805D00006","认领处理失败!") /* "认领处理失败!" */):e.getMessage());
            return ctmJSONObject;
        }
    }

    /**
     * 银行对账单认领并生成来款记录
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/claimtoreceipt")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public Result claimToReceipt(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                 HttpServletResponse response) {
        try {
            HashMap<String, Object> map = service.claimToReceipt(param);
            return Result.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
                    + e.getMessage());
        }
    }
}
