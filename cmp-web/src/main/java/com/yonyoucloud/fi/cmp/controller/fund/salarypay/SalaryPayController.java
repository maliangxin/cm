package com.yonyoucloud.fi.cmp.controller.fund.salarypay;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName SalaryPayController
 * @Description 薪资支付相关的controller
 * @Author majfd
 * @Date 2020/06/22 10:44
 * @Version 1.0
 **/
@Controller
@RequestMapping("/salarypay/")
@Authentication(value = false, readCookie = true)
@Slf4j
public class SalaryPayController extends BaseController {

    @Autowired
    private SalaryPayService salaryService;


    @Autowired
    private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;

    /*
     *@Description 网银预下单
     *@Param [bizObjectMap, request, response]
     *@Return void
     **/
    @RequestMapping("/internetbankplaceorder")
    public void internetBankPlaceOrder(@RequestBody CtmJSONObject param, HttpServletResponse response) {

        try {
            renderJson(response, salaryService.internetBankPlaceOrder(param));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("row"));
        }
    }

    @PostMapping("/payBankCheckUKey")
    public void payBankCheckUKey(HttpServletResponse response) {
        renderJson(response, ResultMessage.data(salaryService.payBankCheckUKey()));
    }


    /*
     *@Description 网银支付确认
     *@Param [params, request, response]
     *@Return void
     **/
    @RequestMapping("/confirmplaceorder")
    public void confirmPlaceOrder(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        try {
            renderJson(response, salaryService.confirmPlaceOrder(params));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        } finally {
            CacheUtils.unlockBill(params.getJSONArray("row"));
        }
    }

    /**
     * @return void
     * @Description 获取银企联渠道号
     * @Date 2019/8/19
     * @Param [response]
     **/
    @GetMapping(value = "/getChanelNo")
    public void getChanPayChanelNo(HttpServletResponse response) {
        renderJson(response, ResultMessage.data(salaryService.getChanPayChanelNo()));
    }

    /*
     *@Description 支付单线下支付
     *@Param [params, response]
     *@Return void
     **/
    @PostMapping(value = "/offLinePay")
    public void offLinePay(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        List<YmsLock> ymsLockList = null;
        try {
            CtmJSONObject params = salaryService.offLinePay(param);
            if (ObjectUtils.isNotEmpty(param.get("ymsLockList"))) {
                ymsLockList = (List<YmsLock>) param.get("ymsLockList");
            }
            params.remove("ymsLockList");
            renderJson(response, ResultMessage.data(params));
        } catch (Exception e) {
            if (ObjectUtils.isNotEmpty(param.get("ymsLockList"))) {
                ymsLockList = (List<YmsLock>) param.get("ymsLockList");
            }
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            if (CollectionUtils.isNotEmpty(ymsLockList)) {
                for (int i = 0; i < ymsLockList.size(); i++) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLockList.get(i));
                }
            }
        }
    }

    /*
     *@Description 支付单取消线下支付
     *@Param [params, response]
     *@Return void
     **/
    @PostMapping(value = "/cancelOffLinePay")
    public void cancelOffLinePay(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        List<YmsLock> ymsLockList = null;
        try {
            CtmJSONObject params = salaryService.cancelOffLinePay(param);
            if (ObjectUtils.isNotEmpty(param.get("ymsLockList"))) {
                ymsLockList = (List<YmsLock>) param.get("ymsLockList");
            }
            params.remove("ymsLockList");
            renderJson(response, ResultMessage.data(params));
        } catch (Exception e) {
            if (ObjectUtils.isNotEmpty(param.get("ymsLockList"))) {
                ymsLockList = (List<YmsLock>) param.get("ymsLockList");
            }
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            if (CollectionUtils.isNotEmpty(ymsLockList)) {
                for (int i = 0; i < ymsLockList.size(); i++) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLockList.get(i));
                }
            }
        }
    }

    /*
     *@Description 批量支付明细状态查询
     *@Param [params, response]
     *@Return void
     **/
    @RequestMapping("/queryPayStatus")
    public void queryBatchPayStatus(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        try {
            renderJson(response, salaryService.queryBatchDetailPayStatus(params));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        } finally {
            CacheUtils.unlockBill(params.getJSONArray("row"));
        }
    }

    @RequestMapping("/audit")
    public void paymentAudit(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(salaryService.audit(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }
    }

    @RequestMapping("/unAudit")
    public void paymentUnAudit(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(salaryService.unAudit(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }
    }

    @RequestMapping("/invalid")
    public void paymentInvalid(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(salaryService.invalid(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }
    }

    @RequestMapping("/hasunknowndata")
    @CMPDiworkPermission(IServicecodeConstant.SALARYPAY)
    public void hasUnknownData(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        boolean hasUnknownData = salaryService.hasUnknownData(param);
        String hasret = hasUnknownData ? "true" : "false";
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("hasunknown", hasret);
        renderJson(response, ResultMessage.data(ret));
    }

    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetSalarypayManagerService.queryBudgetDetail(json);
        renderJson(response, result);
    }

    /**
     * 预算测算
     *
     * @param
     * @param response
     * @throws Exception
     */
    @PostMapping("/budgetCheck")
    public void budget(@RequestBody CmpBudgetVO cmpBudgetVO, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String result = salaryService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }
}
