package com.yonyoucloud.fi.cmp.controller.openapi;

import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.common.ResultList;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import com.yonyoucloud.fi.cmp.fundpayment.service.FundPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 现金管理openapi的公共处理逻辑
 */
@Controller
@RequestMapping("commonapi")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OpenapiCommonController extends BaseController {

    public static final String BILLNUM = "billnum";
    public static final String FULLNAME = "fullname";

    private final IFIBillService fiBillService;


    private final FundPaymentService fundPaymentService;
    private final CurrencyExchangeService currencyExchangeService;

//    @Autowired
//    OpenapiCommonService openapiCommonService;
    /**
     * 现金管理提交openapi
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/submit")
    public void submit(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String id = param.getJSONObject("data").getString("id");
            String billnum = param.getString(BILLNUM);
            String fullname = param.getString(FULLNAME);
            BizObject bizObject = MetaDaoHelper.findById(fullname,id);
            if (Objects.isNull(bizObject)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048A", "单据不存在 id:") /* "单据不存在 id:" */ + id);
            }
            bizObject.put("fromapi",1);
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(billnum);
            bill.setData(bizObject);
            RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SUBMIT.getValue(), bill);
            renderJson(response, CtmJSONObject.toJSONString(ruleExecuteResult.getData()));
        } catch (Exception e) {
            log.error("bill submit error， error message is：" + e.getMessage(),e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F7", "现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
        }
    }

    /**
     * 现金管理提交openapi
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/batchsubmit")
    public void batchsubmit(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            CtmJSONArray jsonArray = param.getJSONObject("data").getJSONArray("ids");
            List<Long> idList = jsonArray.toJavaList(Long.class);
            Long[] ids = new Long[idList.size()];
            Long[] idLongs = idList.toArray(ids);
            String billnum = param.getString(BILLNUM);
            String fullname = param.getString(FULLNAME);
            List<Map<String, Object>> list = MetaDaoHelper.queryByIds(fullname, "*", idLongs);
            if (Objects.isNull(list) || list.isEmpty()) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048A", "单据不存在 id:") /* "单据不存在 id:" */ + idLongs);
            }
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(billnum);
            bill.setData(list);
            bill.setAction(OperationTypeEnum.SUBMIT.getValue());
            ResultList batchsubmit = fiBillService.batchsubmit(bill);
            renderJson(response, CtmJSONObject.toJSONString(batchsubmit));
        } catch (Exception e) {
            log.error("bill batchsubmit error， error message is：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F7", "现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
        }
    }

    /**
     * 现金管理撤回openapi
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/unsubmit")
    public void unsubmit(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String id = param.getJSONObject("data").getString("id");
            String billnum = param.getString(BILLNUM);
            String fullname = param.getString(FULLNAME);
            BizObject bizObject = MetaDaoHelper.findById(fullname,id);
            if (Objects.isNull(bizObject)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048A", "单据不存在 id:") /* "单据不存在 id:" */ + id);
            }
            bizObject.put("fromapi",1);
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(billnum);
            bill.setData(CtmJSONObject.toJSONString(bizObject));
            RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.UNSUBMIT.getValue(), bill);
            renderJson(response, CtmJSONObject.toJSONString(ruleExecuteResult.getData()));
        } catch (Exception e) {
            log.error("bill unsubmit error， error message is：" + e.getMessage(),e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F7", "现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
        }
    }

    /**
     * 现金管理提交openapi
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/batchunsubmit")
    public void batchunsubmit(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            CtmJSONArray jsonArray = param.getJSONObject("data").getJSONArray("ids");
            List<Long> idList = jsonArray.toJavaList(Long.class);
            Long[] ids = new Long[idList.size()];
            Long[] idLongs = idList.toArray(ids);
            String billnum = param.getString(BILLNUM);
            String fullname = param.getString(FULLNAME);
            List<Map<String, Object>> list = MetaDaoHelper.queryByIds(fullname, "*", idLongs);
            if (Objects.isNull(list) || list.isEmpty()) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048A", "单据不存在 id:") /* "单据不存在 id:" */ + Arrays.toString(idLongs));
            }
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(billnum);
            bill.setData(list);
            bill.setAction(OperationTypeEnum.UNSUBMIT.getValue());
            ResultList batchunsubmit = fiBillService.batchDo(bill);
            renderJson(response, CtmJSONObject.toJSONString(batchunsubmit));
        } catch (Exception e) {
            log.error("bill batchunsubmit error， error message is：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21C99FD804880001", "现金管理撤回单据异常：") /* "现金管理撤回单据异常：" */ + e.getMessage()));
        }
    }

    @RequestMapping("/updateSettleDataAndGeneratorVoucher")
    public void updateThirdPartyBillSettlementStatus(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            CtmJSONObject result = fundPaymentService.updateThirdPartyBillSettlementStatus(param);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error("updateSettleDataAndGeneratorVoucher error， error message is：" + e.getMessage(),e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        }
    }

    @RequestMapping("/updateCurrDataAndGeneratorVoucher")
    public void updateCurrDataAndGeneratorVoucher(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            CtmJSONObject result = currencyExchangeService.updateCurrDataAndGeneratorVoucher(param);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error("updateCurrDataAndGeneratorVoucher error， error message is：" + e.getMessage(),e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        }
    }

}
