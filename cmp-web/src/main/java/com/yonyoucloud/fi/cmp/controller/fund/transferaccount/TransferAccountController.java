package com.yonyoucloud.fi.cmp.controller.fund.transferaccount;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.transferaccount.CtmcmpTransferRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountService;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;

import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName TransferAccountController
 * @Desc 转账控制器
 * @Author tongyd
 * @Date 2019/9/18
 * @Version 1.0
 */
@Controller
@RequestMapping("/cm/transferAccount/")
@Slf4j
public class TransferAccountController extends BaseController {

    @Autowired
    TransferAccountService transferAccountService;
    @Autowired
    CtmcmpTransferRpcService cmcpTransferRpcService;

    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    @PostMapping("audit")
    public void audit(@RequestBody CtmJSONObject param, HttpServletResponse response) {
       /* try {
            renderJson(response, ResultMessage.data(transferAccountService.audit(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }*/
    }

    @PostMapping("unAudit")
    public void unAudit(@RequestBody CtmJSONObject param, HttpServletResponse response) {
       /* try {
            renderJson(response, ResultMessage.data(transferAccountService.unAudit(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }*/
    }

    @PostMapping("offLinePay")
    public void offLinePay(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(transferAccountService.offLinePay(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }
    }

    @PostMapping("cancelOffLinePay")
    public void cancelOffLinePay(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(transferAccountService.cancelOffLinePay(param)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }
    }

    /**
     * 根据结算方式编码查询结算方式
     *
     * @param param
     * @param response
     */
    @PostMapping("findByCode")
    public void findByCode(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(transferAccountService.findByCode(param)));
    }


    /**
     * 获取基准类型
     *
     * @param param
     * @param response
     */
    @PostMapping("getBaseExchangeRateType")
    public void getBaseExchangeRateType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {

        renderJson(response, ResultMessage.data(transferAccountService.queryExchangeRateRateTypeByCode(param)));
    }

    /**
     * 获取基准类型
     *
     * @param param
     * @param response
     */
    @PostMapping("querySwiftCode")
    public void querySwiftCode(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(transferAccountService.querySwiftCode(param)));
    }

    /**
     * 交易类型发布菜单，校验serviceCode与实际交易类型是否匹配
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("checkAddTransType")
    public void checkAddTransType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = transferAccountService.checkAddTransType(param);
        renderJson(response, ResultMessage.data(result));
    }

    @PostMapping("createTransferNew")
    public void createTransferNew(@RequestBody CommonRequestDataVo param, HttpServletResponse response) throws Exception {
        cmcpTransferRpcService.saveTransferDraft(param);
    }

    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetCommonManagerService.queryBudgetDetail(json);
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
        String result = transferAccountService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }
}
