package com.yonyoucloud.fi.cmp.controller.fund.paymentbill;


import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.realtimebalance.CtmCmpAccountRealtimeBalanceRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName PaymentController
 * @Description 支付相关的controller
 * @Author tongyd
 * @Date 2019/4/25 10:44
 * @Version 1.0
 **/
@Controller
@Slf4j
public class PaymentController extends BaseController {

    @Autowired
    private PaymentService paymentService;

    // 账户交易明细查询
    @Autowired
    BankDealDetailService bankDealDetailService;

    // 银行账户实时余额查询
    @Autowired
    AccountRealtimeBalanceService accountRealtimeBalanceService;

    @Autowired
    CtmCmpAccountRealtimeBalanceRpcService ctmCmpAccountRealtimeBalanceRpcService;

    @Autowired
    AutoConfigService autoConfigService;

    /**
     * @Author tongyd
     * @Description 网银预下单
     * @Date 2019/4/26 18:41
     * @Param [bizObjectMap, request, response]
     * @Return void
     **/
    @PostMapping("/payment/internetbankplaceorder")
    public void internetBankPlaceOrder(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        String key = PayBill.ENTITY_NAME + params.getJSONArray("rows").getJSONObject(0).getLong("id").toString();
        params.put("lockKey", key);
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (ymsLock == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101918"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900034", "单据【%s】被锁定，请勿重复操作") /* "单据【%s】被锁定，请勿重复操作" */, params.getJSONArray("rows").getJSONObject(0).getString("code")));
        }
        try {
            renderJson(response, ResultMessage.data(paymentService.internetBankPlaceOrder(params)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418009F", "网银预下单异常：") /* "网银预下单异常：" */ + e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * @Author mln
     * @Description 网银预下单取消  -  只支持单条修改
     * @Date 2021/3/30 11:00
     * @Param [bizObjectMap, request, response]
     * @Return void
     **/
    @PostMapping("/payment/orderCancel")
    public void internetBankPlaceOrderCancel(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        String key = PayBill.ENTITY_NAME + params.getJSONObject("row").getLong("id").toString();
        params.put("lockKey", key);
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101918"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900034", "单据【%s】被锁定，请勿重复操作") /* "单据【%s】被锁定，请勿重复操作" */, params.getJSONObject("row").get("code")));
        }
        try {
            renderJson(response, ResultMessage.data(paymentService.internetBankPlaceOrderCancel(params)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418009C", "取消网银预下单异常：") /* "取消网银预下单异常：" */ + e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * @Author tongyd
     * @Description 网银支付确认
     * @Date 2019/5/17 10:02
     * @Param [params, request, response]
     * @Return void
     **/
    @PostMapping("/payment/confirmplaceorder")
    public void confirmPlaceOrder(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        String key = PayBill.ENTITY_NAME + params.getJSONArray("rows").getJSONObject(0).getLong("id").toString();
        params.put("lockKey", key);
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101918"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900034", "单据【%s】被锁定，请勿重复操作") /* "单据【%s】被锁定，请勿重复操作" */, params.getJSONArray("rows").getJSONObject(0).get("code")));
        }
        try {
            renderJson(response, ResultMessage.data(paymentService.confirmPlaceOrder(params)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 获取银企联渠道号
     * @Date 2019/8/19
     * @Param [response]
     **/
    @GetMapping(value = "/payment/getChanelNo")
    public void getChanPayChanelNo(HttpServletResponse response) {
        renderJson(response, ResultMessage.data(paymentService.getChanPayChanelNo()));
    }

    /**
     * @Author tongyd
     * @Description 支付单线下支付
     * @Date 2019/5/17 10:05
     * @Param [params, response]
     * @Return void
     **/
    @PostMapping(value = "/payment/offLinePay")
    public void offLinePay(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        List<YmsLock> ymsLockList = null;
        try {
            CtmJSONObject ctmJSONObject = paymentService.offLinePay(params);
            ymsLockList = (List<YmsLock>) ctmJSONObject.get("ymsLockList");
            ctmJSONObject.remove("ymsLockList");
            renderJson(response, ResultMessage.data(ctmJSONObject));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            //CtmJSONArray rows = params.getJSONArray("rows");
            if (ymsLockList != null) {
                for (int i = 0; i < ymsLockList.size(); i++) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLockList.get(i));
                }
            }

        }
    }

    /**
     * @Author tongyd
     * @Description 支付单取消线下支付
     * @Date 2019/5/17 10:09
     * @Param [params, response]
     * @Return void
     **/
    @PostMapping(value = "/payment/cancelOffLinePay")
    public void cancelOffLinePay(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(paymentService.cancelOffLinePay(params)));
    }


    /**
     * @Author tongyd
     * @Description 账户交易明细查询 - 手工拉取账户交易明细
     * @Date 2019/5/21 13:42
     * @Param [response]
     * @Return void
     **/
    @PostMapping(value = "/payment/queryTransDetail")
    public void queryAccountTransactionDetail(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, false);
        bankDealDetailService.queryAccountTransactionDetail(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 账户交易明细手动拉取
     * @Author yangjn
     * @Description 在无Ukey情况下 账户交易明细查询
     * @Date 2021/5/07 15:56
     * @Param [response]
     * @Return void
     **/
    @PostMapping(value = "/payment/queryTransDetailUnNeedUkey")
    @CMPDiworkPermission(IServicecodeConstant.DLLIST)
    public void queryAccountTransactionDetailUnNeedUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, false);
        bankDealDetailService.queryAccountTransactionDetail(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * @Author tongyd
     * @Description 查询企业银行账户实时余额
     * @Date 2019/5/31 11:30
     * @Param [params, request, response]
     * @Return void
     **/
    @PostMapping("/payment/queryAccountBalance")
    public void queryAccountBalance(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        accountRealtimeBalanceService.queryAccountBalanceUnNeedUkey(params);
        renderJson(response, ResultMessage.data(null));
    }


    @PostMapping("/payment/getRealTimeBalance")
    public void getRealTimeBalance(@RequestBody CommonRequestDataVo params, HttpServletResponse response) throws Exception {
        String accentity = params.getAccentity();
        Boolean checkBalanceIsQuery = autoConfigService.getCheckBalanceIsQuery(accentity);
        String queryBillType = autoConfigService.getQueryBillType(accentity);
        Map<String, Object> map;
        if (checkBalanceIsQuery && !StringUtils.isEmpty(queryBillType) && queryBillType.equals("1")) {
            map = ctmCmpAccountRealtimeBalanceRpcService.queryAccountRealtimeBalance(params);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101919"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007D", "请检查现金基础参数") /* "请检查现金基础参数" */);
        }

        renderJson(response, ResultMessage.data(map));
    }

    /**
     * 手动拉取账户实时余额
     * @Author yangjn
     * @Description 在无Ukey情况下 查询企业银行账户实时余额
     * @Date 2021/5/6 14:30
     * @Param [params, response]
     * @Return void
     **/
    @PostMapping("/payment/queryAccountBalanceUnNeedUkey")
    @CMPDiworkPermission(IServicecodeConstant.RETIBALIST)
    public void queryAccountBalanceUnNeedUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        accountRealtimeBalanceService.queryAccountBalanceUnNeedUkey(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * @Author tongyd
     * @Description 批量支付明细状态查询
     * @Date 2019/6/13 21:19
     * @Param [params, response]
     * @Return void
     **/
    @PostMapping("/payment/queryPayStatus")
    public void queryBatchPayStatus(@RequestBody CtmJSONArray params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(paymentService.queryBatchDetailPayStatus(params)));
    }

    /**
     * @Author tongyd
     * @Description 更新批量支付结果
     * @Date 2019/6/12 16:28
     * @Param [requestData, request, response]
     * @Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    @PostMapping(value = "/authapi/payment/updateBatchPayStatus")
    public @ResponseBody
    String updateBatchPayStatus(@RequestBody String requestData, HttpServletRequest request, HttpServletResponse response) {
        try {
            return paymentService.updateBatchPayStatus(requestData);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    @RequestMapping("/payment/audit")
    public void paymentAudit(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(paymentService.paymentAudit(param)));
    }

    @RequestMapping("/payment/unAudit")
    public void paymentUnAudit(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        List<YmsLock> ymsLockList = null;
        try {
            CtmJSONObject ctmJsonOjbect = paymentService.paymentUnAudit(params);
            ymsLockList = (List<YmsLock>) ctmJsonOjbect.get("ymsLockList");
            ctmJsonOjbect.remove("ymsLockList");
            renderJson(response, ResultMessage.data(ctmJsonOjbect));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418009E", "弃审异常：") /* "弃审异常：" */ + e.getMessage()));
        } finally {
            //CtmJSONArray rows = params.getJSONArray("rows");
            if (ymsLockList != null) {
                for (int i = 0; i < ymsLockList.size(); i++) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLockList.get(i));
                }
            }

        }
    }

    @RequestMapping("/payment/quickType")
    @ApplicationPermission("CM")
    public void paymentQuickType(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        HashMap map = new HashMap<>();
        map.put("code", params.get("code"));
        List<Map<String, Object>> list = QueryBaseDocUtils.queryQuickTypeByCondition(map);
        if(CollectionUtils.isEmpty(list)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F21B2BE05B00074", "款项类型未匹配到，请检查！") /* "款项类型未匹配到，请检查！" */);
        }
        Map resultMap = list.get(0);
        renderJson(response, ResultMessage.data(resultMap));
    }

    /**
     * @Author tongyd
     * @Description 查询企业银行账户实时余额--对外接口
     * @Date 2019/5/31 11:30
     * @Param [params, request, response]
     * @Return void
     **/
    @PostMapping("/payment/queryRealbalanceBalance")
//    @Authentication(value = false, readCookie = true)
    public void queryRealbalanceBalance(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, CtmJSONObject.toJSONString(accountRealtimeBalanceService.queryRealbalanceBalanceNew(params)));
    }

    /**
     * @Author tongyd
     * @Description 账户交易明细查询--新版
     * @Date 2019/5/21 13:42
     * @Param [response]
     * @Return void
     **/
    @PostMapping(value = "/payment/test")
    @Authentication(value = false, readCookie = true)
    public void test(HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("status", 1);
        renderJson(response, CtmJSONObject.toJSONString(responseMsg));
    }


    @PostMapping(value = "/payment/getToken")
    @Authentication(value = false, readCookie = true)
    public void getToken(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject responseMsg = new CtmJSONObject();
        String key = "yonbip-fi-ctmcmp-pay:" + AppContext.getTenantId() + params.get("yht_token");
        String value = AppContext.cache().get(key);
//            log.info("获取token,key:"+key+"value:"+value);
        if (value != null) {
            responseMsg.put("status", true);
        } else {
            responseMsg.put("status", false);
        }
        renderJson(response, CtmJSONObject.toJSONString(responseMsg));
    }

    @PostMapping(value = "/payment/setToken")
    @Authentication(value = false, readCookie = true)
    public void setToken(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) {
        String ident = "yonbip-fi-ctmcmp-pay:" + AppContext.getTenantId() + params.get("yht_token");
        boolean res = AppContext.cache().setnx(ident, "ident", 1800);//一个小时
        CtmJSONObject responseMsg = new CtmJSONObject();
//            log.info("设置token,key:"+ident+"value:"+res);
        responseMsg.put("status", res);
        renderJson(response, CtmJSONObject.toJSONString(responseMsg));
    }

    @PostMapping(value = "/payment/queryObject")
    @Authentication(value = false, readCookie = true)
    public void queryObject(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(params.get("id").toString()));
        querySchema.addCondition(queryConditionGroup);

        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("status", 1);
        renderJson(response, CtmJSONObject.toJSONString(MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null)));
    }
}
