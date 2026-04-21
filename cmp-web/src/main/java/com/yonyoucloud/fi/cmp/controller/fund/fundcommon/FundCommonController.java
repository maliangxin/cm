package com.yonyoucloud.fi.cmp.controller.fund.fundcommon;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyou.yonbip.ctm.liquidity.api.ICtmLiquidityQueryService;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityQueryConditionDTO;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.liquidity.service.CmpBillLiquidityAnalysisQueryService;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ACCENT;

/**
 * <h1>FundCommonController</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-22 15:07
 */
@Controller
@RequestMapping("/fund")
@RequiredArgsConstructor
@Slf4j
public class FundCommonController {

    private final IFundCommonService fundCommonService;

    private final PushAndPullService pushAndPullService;

    private final FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    private final ICtmLiquidityQueryService ctmLiquidityQueryService;

    private final CmpBillLiquidityAnalysisQueryService cmpBillLiquidityAnalysisQueryService;


    /**
     * <h2>判断是否启用商业汇票</h2>
     *
     * @param param    :
     * @param response :
     * @author Sun GuoCai
     * @since 2021/12/22 15:28
     */
    @RequestMapping("/isEnabledBsd")
    @ApplicationPermission("CM")
    public void isEnabledBsdModule(CtmJSONObject param, HttpServletResponse response) throws Exception {
        String accent = null;
        if (!ValueUtils.isNotEmptyObj(param.get(ACCENT))) {
            renderJson(response, ResultMessage.data("accent field is not empty!"));
        }
        accent = String.valueOf(param.get("accent"));
        CtmJSONObject jsonObject = fundCommonService.isEnableBsdModule(accent);
        renderJson(response, ResultMessage.data(jsonObject));
    }


    /**
     * <h2>删除资金收付款单</h2>
     *
     * @param param    :
     * @param response :
     * @author Sun GuoCai
     * @since 2022/1/5 12:06
     */
    @PostMapping("/deleteFundBillByIds")
    @ApplicationPermission("CM")
    public void deleteFundBillByIds(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = fundCommonService.deleteFundBillByIds(param);
        renderJson(response, ResultMessage.data(jsonObject));
    }


    /**
     * 查询单据对应的结算状态
     *
     * @param response
     */
    @RequestMapping("/queryFundBillStatus")
    @ApplicationPermission("CM")
    public void queryFundCollectionStatus(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.querySettledDetail(params)));
    }

    /**
     * 查询客户对应的账户信息
     *
     * @param response
     */
    @RequestMapping("/checkCustomerAccount")
    @ApplicationPermission("CM")
    public void checkCustomerAccount(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.checkCustomerAccount(params)));
    }

    /**
     * 查询员工对应的账户信息
     *
     * @param response
     */
    @RequestMapping("/checkEmployeeAccount")
    @ApplicationPermission("CM")
    public void checkEmployeeAccount(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.checkEmployeeAccount(params)));
    }

    /**
     * 查询供应商对应的账户信息
     *
     * @param response
     */
    @RequestMapping("/checkSupplierAccount")
    @ApplicationPermission("CM")
    public void checkSupplierAccount(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.checkSupplierAccount(params)));
    }

    @RequestMapping("/queryFundBillByIdOrCode")
    public void queryFundBillByIdOrCode(@RequestParam() String billNum,
                                        @RequestParam(required = false) Long id,
                                        @RequestParam(required = false) String code,
                                        @RequestParam(required = false) String fundBillSubPubtsBegin,
                                        @RequestParam(required = false) String fundBillSubPubtsEnd,
                                        @RequestParam(required = false) Short settleStatus,
                                        HttpServletResponse response) throws Exception {
        if (!ValueUtils.isNotEmptyObj(id) && !ValueUtils.isNotEmptyObj(code)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180183", "单据id或单据编码不能为空") /* "单据id或单据编码不能为空" */));
            return;
        }
        if (ValueUtils.isNotEmptyObj(id) && ValueUtils.isNotEmptyObj(code)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180185", "单据id或单据编码不能同时传递") /* "单据id或单据编码不能同时传递" */));
            return;
        }
        if (ValueUtils.isNotEmptyObj(fundBillSubPubtsBegin) && !ValueUtils.isMatchesPubts(fundBillSubPubtsBegin)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180181", "单据子表的开始时间戳格式不合法!") /* "单据子表的开始时间戳格式不合法!" */));
            return;
        }
        if (ValueUtils.isNotEmptyObj(fundBillSubPubtsEnd) && !ValueUtils.isMatchesPubts(fundBillSubPubtsEnd)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180182", "单据子表的结束时间戳格式不合法!") /* "单据子表的结束时间戳格式不合法!" */));
            return;
        }
        if (ValueUtils.isNotEmptyObj(settleStatus) && !ValueUtils.isNotEmptyObj(FundSettleStatus.find(settleStatus))) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180184", "单据子表的结算状态不存在!") /* "单据子表的结算状态不存在!" */));
            return;
        }
        String data = fundCommonService.queryFundBillByIdOrCode(billNum, id, code, fundBillSubPubtsBegin, fundBillSubPubtsEnd, settleStatus);
        renderJson(response, data);
    }

    /**
     * 查询资金业务对象银行对账单业务信息
     *
     * @param response
     */
    @RequestMapping("/queryBfundbusinobjData")
    @ApplicationPermission("CM")
    public void queryBfundbusinobjData(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.queryBfundbusinobjData(params)));
    }

    /**
     * <h2>检查现金参数是否启用资金计划项目</h2>
     *
     * @param param    : 含资金组织
     * @param response : 响应对象
     * @author Sun GuoCai
     * @since 2023/2/21 16:38
     */
    @PostMapping("/fundPlan")
    @ApplicationPermission("CM")
    public void isEnabledFundPlanProject(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        String accent = null;
        CtmJSONObject data = new CtmJSONObject();
        if (!ValueUtils.isNotEmptyObj(param.get(ACCENT))) {
            data.put("enableFundPlan", false);
            renderJson(response, ResultMessage.data(data));
            return;
        }
        accent = String.valueOf(param.get("accent"));
        boolean flag = fundCommonService.checkFundPlanIsEnabled(accent);
        data.put("enableFundPlan", flag);
        renderJson(response, ResultMessage.data(data));
    }

    /**
     * 查询单据对应的结算状态
     *
     * @param response
     */
    @RequestMapping("/pushSettleBill")
    @ApplicationPermission("CM")
    public void pushSettleBill(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.pushSettleBill(params)));
    }

    /**
     * 查询单据对应的结算状态
     *
     * @param response
     */
    @RequestMapping("/pushDataSettle")
    @ApplicationPermission("CM")
    public void pushDataSettle(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        String ids = ValueUtils.isNotEmptyObj(params.get("ids")) ? params.get("ids").toString() : null;
        if (!ValueUtils.isNotEmptyObj(ids)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100105"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005F", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        String billNum = ValueUtils.isNotEmptyObj(params.get("billNum")) ? params.get("billNum").toString() : null;
        if (!ValueUtils.isNotEmptyObj(billNum)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100106"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005E", "参数错误！") /* "参数错误！" */);
        }
        renderJson(response, ResultMessage.data(fundCommonService.pushDataSettle(billNum, ids)));
    }

    /**
     * 根据交易类型id获取单据类型id
     *
     * @param param    :
     * @param response :
     * @author Sun GuoCai
     * @since 2021/12/22 15:28
     */
    @RequestMapping("/queryBillTypeIdByTradeTypeId")
    @ApplicationPermission("CM")
    public void queryBillTypeIdByTradeTypeId(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        if (!ValueUtils.isNotEmptyObj(param.get("tradeType"))) {
            renderJson(response, ResultMessage.data("tradeType field is not empty!"));
            return;
        }
        String tradeType = String.valueOf(param.get("tradeType"));
        CtmJSONObject jsonObject = fundCommonService.queryBillTypeIdByTradeTypeId(tradeType);
        renderJson(response, ResultMessage.data(jsonObject));
    }

    /**
     * 查询统收统支关系组默认账户
     *
     * @param response
     */
    @RequestMapping("/queryIncomeAndExpenditureDefaultAccount")
    @ApplicationPermission("CM")
    public void queryIncomeAndExpenditureDefaultAccount(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.queryIncomeAndExpenditureDefaultAccount(params)));
    }

    /**
     * <h2>querySwapOutExchangeRate</h2>
     *
     * @param params   : 入参
     * @param response : 出参
     * @author Sun GuoCai
     * @since 2024/1/5 10:21
     */
    @RequestMapping("/querySwapOutExchangeRate")
    @ApplicationPermission("CM")
    public void querySwapOutExchangeRate(@RequestBody CtmJSONArray params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundCommonService.querySwapOutExchangeRate(params)));
    }

    @RequestMapping("/pushAndPullVerify")
    public void pushAndPullVerify(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONObject responseJsonObject = new CtmJSONObject();
        responseJsonObject.put("params", params);
        Boolean newArch = true;
        responseJsonObject.put("newArch", newArch);
        Boolean newFi = InvocationInfoProxy.getNewFi();
        responseJsonObject.put("newFi", newFi);
        String yxyTenantId = InvocationInfoProxy.getYxyTenantid();
        responseJsonObject.put("yxyTenantId", yxyTenantId);
        String tenantId = InvocationInfoProxy.getTenantid();
        responseJsonObject.put("tenantId", tenantId);
        Long id = params.getLong("id");
        String pushCode = params.getString("pushCode");
        Boolean needDivide = params.getBoolean("needDivide");
        String domain = String.valueOf(params.get("domain"));
        String select = String.valueOf(params.get("select"));
        String fullName = String.valueOf(params.get("fullName"));
        String fullNameSub = String.valueOf(params.get("fullNameSub"));
        String entityNameSub = String.valueOf(params.get("entityNameSub"));
        String mainId = String.valueOf(params.get("mainId"));
        BillContext context = new BillContext();
        context.setFullname(fullName);
        context.setDomain(domain);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect(select);
        schema.appendQueryCondition(QueryCondition.name("id").eq(id));
        List<BizObject> bills = new ArrayList<>();
        List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);
        if (CollectionUtils.isNotEmpty(result)) {
            for (Map<String, Object> objectMap : result) {
                BizObject bizObject = new BizObject(objectMap);
                BillContext contextSub = new BillContext();
                contextSub.setFullname(fullNameSub);
                contextSub.setDomain(domain);
                QuerySchema schemaSub = QuerySchema.create();
                schemaSub.addSelect(select);
                schemaSub.appendQueryCondition(QueryCondition.name(mainId).eq(id));
                List<Map<String, Object>> resultSub = MetaDaoHelper.query(contextSub, schemaSub);
                bizObject.set(entityNameSub, resultSub);
                bills.add(bizObject);
            }
        } else {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80097", "未查询到数据!") /* "未查询到数据!" */));
            return;
        }
        PushAndPullModel pushAndPullModel = new PushAndPullModel();
        pushAndPullModel.setNeedDivide(needDivide);//是否需要分单
        pushAndPullModel.setCode(pushCode);
        List<BizObject> bizObjects = pushAndPullService.transformBillByMakeBillCodeAll(bills, pushAndPullModel);
        responseJsonObject.put("data", bizObjects);
        renderJson(response, ResultMessage.data(responseJsonObject));
    }

    /**
     * 查询统收统支关系组默认账户
     *
     * @param response
     */
    @RequestMapping("/queryFundPlanDetailById")
    @ApplicationPermission("CM")
    public void queryFundPlanDetailById(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(fundBillAdaptationFundPlanService.queryFundPlanDetailById(params)));
    }

    @PostMapping("/analysisDataQuery")
    public void analysisDataQuery(@RequestBody LiquidityQueryConditionDTO conditionDTO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(ctmLiquidityQueryService.queryLiquidityItemsByCondition(conditionDTO)));
    }

    @PostMapping("/analysisDataQueryByBill/{billType}")
    public void analysisDataQueryByBill(@RequestBody LiquidityQueryConditionDTO conditionDTO, @PathVariable String billType, HttpServletResponse response) throws Exception {
        switch (billType) {
            case "fundPayment":
                renderJson(response, ResultMessage.data(cmpBillLiquidityAnalysisQueryService.queryFundPaymentLiquidityAnalysisData(conditionDTO)));
                break;
            case "fundCollection":
                renderJson(response, ResultMessage.data(cmpBillLiquidityAnalysisQueryService.queryFundCollectionLiquidityAnalysisData(conditionDTO)));
                break;
            case "salaryPay":
                renderJson(response, ResultMessage.data(cmpBillLiquidityAnalysisQueryService.querySalaryPayLiquidityAnalysisData(conditionDTO)));
                break;
            case "foreignPayment":
                renderJson(response, ResultMessage.data(cmpBillLiquidityAnalysisQueryService.queryForeignPaymentLiquidityAnalysisData(conditionDTO)));
                break;
            case "transferAccount":
                renderJson(response, ResultMessage.data(cmpBillLiquidityAnalysisQueryService.queryTransferAccountLiquidityAnalysisData(conditionDTO)));
                break;
            default:
                break;
        }
    }

    @PostMapping("/analysisDataQueryPageByBill")
    public void analysisDataQueryPageByBill(@RequestBody LiquidityQueryConditionDTO conditionDTO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(ctmLiquidityQueryService.queryLiquidityItemPageByCondition(conditionDTO)));
    }

    /**
     * 数据权限修改历史数据接口
     *
     * @param
     * @param response
     */
    @GetMapping("/updateUserId")
    public void updateUserId(@RequestParam String  startCreateTime,@RequestParam String  endCreateTime, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(fundCommonService.updateUserId(startCreateTime,endCreateTime)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        }
    }

}
