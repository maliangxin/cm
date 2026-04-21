package com.yonyoucloud.fi.cmp.controller.basicSetting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.risk.util.RiskEnvironment;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.JournalBalanceSortRuleEnum;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
@RequestMapping("/autoconfig")
@Authentication(value = false, readCookie = true)
@Slf4j
public class AutoConfigController extends BaseController {

    @Autowired
    AutoConfigService autoConfigService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryService;


    @RequestMapping("/setdefault")
    public void query(@RequestBody JsonNode paramJson, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Long id = null;
        if (paramJson != null) {
            JsonNode autoConfigJson = JSONBuilderUtils.stringToJson(JSONBuilderUtils.getTextValue((ObjectNode) paramJson, ICmpConstant.DATA));
            id = autoConfigJson.get("id").asLong();
        }
        JsonNode jSONObject = autoConfigService.setDefault(id);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(jSONObject)));
    }


    @PostMapping("/initparam")
    @CMPDiworkPermission(IServicecodeConstant.AUTOCONFIG)
    public void initParamSetAR(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("single", false);

        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("serviceAttr").eq(0));
        conditionGroup.appendCondition(QueryCondition.name("isEnabled").eq(1));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
        if (query != null && query.size() > 0) {
            map.put("settlemode", query.get(0).get("id"));
            map.put("settlemode_name", query.get(0).get("name"));
        }
        //如果设置了默认会计主体 则赋值
        String userId = null;
        if (AppContext.getSimplifyLoginSwitch()) {
            userId = AppContext.getCurrentUser().get(ICmpConstant.DEFALUEORG);
        } else {
            userId = InvocationInfoProxy.getDefaultOrg();
        }
        if (userId != null && !"".equals(userId)) {
            QuerySchema querySchema = QuerySchema.create().addSelect("code,name,id");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(userId));
            querySchema.addCondition(group);
            List<Map<String, Object>> defaultAccentity = MetaDaoHelper.query("aa.baseorg.OrgMV", querySchema, ISchemaConstant.MDD_SCHEMA_ORGCENTER);
            if (defaultAccentity != null && defaultAccentity.size() > 0) {
                map.put("accentity", defaultAccentity.get(0).get("id"));
                map.put("accentity_name", defaultAccentity.get(0).get("name"));
            }
        } else {
            //查服务权限主组织，角色授权组织+服务主组织标签类型筛选
            List<Map<String, Object>> orgVOPermissions = AuthUtil.getOrgVOPermissions("ficmp0050");
            if (orgVOPermissions == null || orgVOPermissions.size() == 0) {
                //新初始化租户  未创建组织
                map.put("accentity", null);
                map.put("accentity_name", null);
            } else {
                Map<String, Object> map2 = orgVOPermissions.get(0);
                    /*map.put("accentity", map2.get("id"));
                    map.put("accentity_name", map2.get("name"));*/
                //没有设置默认业务单元的情况下，返回企业账号级
                map.put("accentity", "666666");
                map.put("accentity_name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80078", "企业账号级") /* "企业账号级" */);
            }
        }
        if (FIDubboUtils.isSingleOrg()) {
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if (singleOrg != null) {
                map.put("accentity", singleOrg.get("id"));
                map.put("accentity_name", singleOrg.get("name"));
                map.put("single", true);
            }
        }
        if (!StringUtils.isEmpty(map.get("accentity") + "")) {
            //不是资金组织时，不返回
            if (map.get("accentity") != null && fundsOrgQueryService.getById(map.get("accentity").toString()) == null) {
                map.remove("accentity");
                map.remove("accentity_name");
            }
        }
        renderJson(response, ResultMessage.data(map));
    }


    @PostMapping("/getParamDataByAccentity")
    @CMPDiworkPermission(IServicecodeConstant.AUTOCONFIG)
    public void getParamDataByAccentity(@RequestBody Map<String, Object> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(map.get("accentity") == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100763"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050018", "资金组织不能为空") /* "资金组织不能为空" */);
        }
        String accentity = map.get("accentity").toString();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        Map<String, Object> data = new HashMap<>();
        data.put("hasdata", false);
        data.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EB", "传进来的会计主体") /* "传进来的会计主体" */, accentity);
        List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
        Object code = accentityObj != null && accentityObj.get(0) != null ? accentityObj.get(0).get("code") : null;
        if (query != null && query.size() > 0) {
            data.put("hasdata", true);
            data = query.get(0);
            // 我的认领是否需要复核
            if (query.get(0).containsKey("isRecheck") && query.get(0).get("isRecheck") != null) {
                data.put("isRecheck", query.get(0).get("isRecheck"));
            } else {
                data.put("isRecheck", false);
            }

            if (query.get(0).get("settlemode") != null) {
                SettleMethodModel settlemode = baseRefRpcService.querySettleMethodsById(query.get(0).get("settlemode").toString());
                String locale = InvocationInfoProxy.getLocale();
                switch (locale) {
                    case "en_US":
                        data.put("settlemode_name", settlemode.getName2());
                        break;
                    case "zh_TW":
                        data.put("settlemode_name", settlemode.getName3());
                        break;
                    default:
                        data.put("settlemode_name", settlemode.getName());
                }
            } else {
                schema = QuerySchema.create();
                schema.addSelect("*");
                conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name("serviceAttr").eq(0));
                conditionGroup.appendCondition(QueryCondition.name("isEnabled").eq(1));
                schema.addCondition(conditionGroup);
                List<Map<String, Object>> querySettle = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
                if (querySettle != null && querySettle.size() > 0) {
                    data.put("settlemode", querySettle.get(0).get("id"));
                    data.put("settlemode_name", querySettle.get(0).get("name"));
                }
            }

            if (query.get(0).get("receiveQuickType") != null) {
                Map<String, Object> condition = new HashMap<String, Object>();
                condition.put("id", data.get("receiveQuickType"));
                List<Map<String, Object>> receiveQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                if (ValueUtils.isNotEmpty(receiveQuickType)) {
                    data.put("receiveQuickType_name", receiveQuickType.get(0).get("name"));
                    data.put("receiveQuickType_code", receiveQuickType.get(0).get("code"));
                    data.put("mytest0", receiveQuickType.get(0));
                }
            }

            //新建的会计主体没有数据需要自己设置
            if (query.get(0).get("receiveQuickType") == null) {
                //租户id
                String tenant = String.valueOf(data.get("tenant"));
                //构造一个条件对象
                Map<String, Object> condition = new HashMap<String, Object>();
                //
                condition.put("tenant", tenant);
                condition.put("code", "1");
                //根据条件查询数据
                List<Map<String, Object>> receiveQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                data.put("querydatacode1", receiveQuickType);
                //处理返回前端的数据
                if (ValueUtils.isNotEmpty(receiveQuickType)) {
                    data.put("receiveQuickType", receiveQuickType.get(0).get("id"));
                    data.put("receiveQuickType_name", receiveQuickType.get(0).get("name"));
                    data.put("receiveQuickType_code", receiveQuickType.get(0).get("code"));
                }
            }

            //增加现金参数默认付款款项类型
            if (query.get(0).get("payQuickType") != null) {
                Map<String, Object> condition = new HashMap<String, Object>();
                condition.put("id", data.get("payQuickType"));
                List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                if (ValueUtils.isNotEmpty(payQuickType)) {
                    data.put("payQuickType_name", payQuickType.get(0).get("name"));
                    data.put("mytest1", payQuickType.get(0));
                    //data.put("payQuickType_code",payQuickType.get(0).get("code"));
                }
            } else if (query.get(0).get("payQuickType") == null) {

                //租户id
                String tenant = String.valueOf(data.get("tenant"));
                //构造一个条件对象
                Map<String, Object> condition = new HashMap<String, Object>();
                //
                condition.put("tenant", tenant);
                condition.put("code", "5");
                //根据条件查询数据
                List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
                data.put("querydatacode5", payQuickType);
                //处理返回前端的数据
                if (ValueUtils.isNotEmpty(payQuickType)) {
                    data.put("payQuickType", payQuickType.get(0).get("id"));
                    data.put("payQuickType_name", payQuickType.get(0).get("name"));
                    data.put("payQuickType_code", payQuickType.get(0).get("code"));
                }
            }
            //否需要校验ukey字段 赋值
            if (query.get(0).get("checkUkey") != null) {
                data.put("checkUkey", query.get(0).get("checkUkey"));
            } else {
                data.put("checkUkey", null);
            }
            //存在未审核单据是否允许日结
            if (query.get(0).get("checkDailySettlement") != null) {
                data.put("checkDailySettlement", query.get(0).get("checkDailySettlement"));
            } else {
                data.put("checkDailySettlement", null);
            }
            if (query.get(0).get("isGenerateFundCollection") != null) {
                data.put("isGenerateFundCollection", query.get(0).get("isGenerateFundCollection"));
            } else {
                data.put("isGenerateFundCollection", null);
            }
            if (query.get(0).get("isSettleSuccessToPost") != null) {
                data.put("isSettleSuccessToPost", query.get(0).get("isSettleSuccessToPost"));
            } else {
                data.put("isSettleSuccessToPost", null);
            }
            if (query.get(0).get("isShareVideo") != null) {
                data.put("isShareVideo", query.get(0).get("isShareVideo"));
            } else {
                data.put("isShareVideo", null);
            }
            if (query.get(0).get("checkFundPlan") != null) {
                data.put("checkFundPlan", query.get(0).get("checkFundPlan"));
            } else {
                data.put("checkFundPlan", null);
            }
            //收付单据关联是否支持容差
            if (query.get(0).get("isAutoCorrSupportDiffAmount") != null) {
                data.put("isAutoCorrSupportDiffAmount", query.get(0).get("isAutoCorrSupportDiffAmount"));
            } else {
                data.put("isAutoCorrSupportDiffAmount", null);
            }
            //无需处理的流水，是否参与银企对账、银行账户余额弥补
            if (query.get(0).get("isNoProcess") != null) {
                data.put("isNoProcess", query.get(0).get("isNoProcess"));
            } else {
                data.put("isNoProcess", null);
            }

            // 日记账余额排序规则 -- 默认按照结算成功系统时间
            if (query.get(0).get("journalBalanceSortRule") != null) {
                data.put("journalBalanceSortRule", query.get(0).get("journalBalanceSortRule"));
            } else  {
                data.put("journalBalanceSortRule", null);
            }
        } else {
            data.put("hasdata", true);
            //租户id
            //String tenant = map.get("tenant").toString();
            //构造一个条件对象
            Map<String, Object> condition = new HashMap<String, Object>();
            //构造一个条件对象
            Map<String, Object> condition5 = new HashMap<String, Object>();
            //
            //condition.put("tenant",tenant);
            condition.put("code", "1");
            //
            //condition5.put("tenant",tenant);
            condition5.put("code", "5");
            //根据条件查询数据
            List<Map<String, Object>> receiveQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
            //根据条件查询数据
            List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition5);
            //处理返回前端的数据
            if (ValueUtils.isNotEmpty(payQuickType) && ValueUtils.isNotEmpty(receiveQuickType)) {
                data.put("payQuickType", payQuickType.get(0).get("id"));
                data.put("payQuickType_name", payQuickType.get(0).get("name"));
                data.put("payQuickType_code", payQuickType.get(0).get("code"));
                data.put("receiveQuickType", receiveQuickType.get(0).get("id"));
                data.put("receiveQuickType_name", receiveQuickType.get(0).get("name"));
                data.put("receiveQuickType_code", receiveQuickType.get(0).get("code"));
            }
            schema = QuerySchema.create();
            schema.addSelect("*");
            conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("serviceAttr").eq(0));
            conditionGroup.appendCondition(QueryCondition.name("isEnabled").eq(1));
            schema.addCondition(conditionGroup);
            query = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
            if (query != null && query.size() > 0) {
                data.put("settlemode", query.get(0).get("id"));
                data.put("settlemode_name", query.get(0).get("name"));
            }
            data.put("checkUkey", false);
            data.put("checkDailySettlement", false);
            data.put("isGenerateFundCollection", false);
            data.put("isSettleSuccessToPost", null);
            data.put("isShareVideo", false);
            data.put("checkFundPlan", false);
            //收付单据关联是否支持容差
            data.put("isAutoCorrSupportDiffAmount", false);
            //无需处理
            data.put("isNoProcess", true);
            // 日记账余额排序规则 -- 默认按照结算成功系统时间
            data.put("journalBalanceSortRule", JournalBalanceSortRuleEnum.DZ_TIME.getValue());
        }
        data.put("accentity_code", code);
        //判断是否开启风险检查，用来控制前端结算检查区域是否展示
        data.put("isEnableRiskCheck", RiskEnvironment.isEnable());
        //查询企业账号级参数的重空凭证（付票）是否有领用环节
        Boolean checkStockIsUse = autoConfigService.getCheckStockIsUse();
        data.put("checkStockIsUse", checkStockIsUse);
        renderJson(response, ResultMessage.data(data));
    }

    @RequestMapping("/eventListening")
    public void eventListening(@RequestBody Map<Object, Object> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
//		log.info("===============回调参数=======================期间启用监听回调================"+map+"=========================================");
//		log.info("===============回调成功=======================期间启用监听回调================"+map.toString()+"=========================================");
        String userObject = (String) map.get("userObject");
        JsonNode jsonObject = JSONBuilderUtils.stringToJson(userObject);
        JsonNode orgDataVO = jsonObject.get("orgDataVO");
        String id = orgDataVO.get("id").asText();
        Map<String, Object> condition = new HashMap<String, Object>();
        Map<String, Object> condition5 = new HashMap<String, Object>();
        condition.put("code", "1");
        condition5.put("code", "5");
        List<Map<String, Object>> receiveQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
        List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition5);
        String receiveQuickTypeId = null;
        if (receiveQuickType != null && receiveQuickType.size() > 0) {
            receiveQuickTypeId = String.valueOf(receiveQuickType.get(0).get("id"));
        }
        String payQuickTypeId = null;
        if (payQuickType != null && payQuickType.size() > 0) {
            payQuickTypeId = String.valueOf(payQuickType.get(0).get("id"));
        }
        List<AutoConfig> list = new ArrayList<>();
        AutoConfig autoConfig = new AutoConfig();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("serviceAttr").eq(0));
        conditionGroup.appendCondition(QueryCondition.name("isEnabled").eq(1));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
        String settlemodeId = null;
        if (query != null && query.size() > 0) {
            settlemodeId = String.valueOf(query.get(0).get("id"));
        }
        autoConfig.setPayQuickType(Long.valueOf(payQuickTypeId));
        autoConfig.setReceiveQuickType(Long.valueOf(receiveQuickTypeId));
        autoConfig.setSettlemode(Long.valueOf(settlemodeId));
        autoConfig.setAccentity(id);
        autoConfig.setEntityStatus(EntityStatus.Insert);
        autoConfig.setId(ymsOidGenerator.nextId());
        list.add(autoConfig);
        CmpMetaDaoHelper.insert(AutoConfig.ENTITY_NAME, list);
    }

    /**
     * 通过传递的会计主体数组判断 是否校验ukey 前台获取boolean
     * yangjn 20210429
     *
     * @param params
     * @param response
     */
    @PostMapping("/getCheckUkey")
    public void getCheckUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getCheckUkey(params, false)));
    }

    /**
     * 通过传递的会计主体数组判断 是否校验ukey 前台获取boolean
     * zhuguangqian 20241227
     *
     * @param params
     * @param response
     */
    @PostMapping("/getCheckUkeyNewLogic")
    public void getCheckUkeyNewLogic(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getCheckUkey(params, true)));
    }

    /**
     * 通过传递的会计主体查询现金参数
     *
     * @param params
     * @param response
     */
    @PostMapping("/getAutoConfig")
    @ApplicationPermission("CM")
    public void getAutoConfig(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        if (params.get(ICmpConstant.ACCENTITY) == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100763"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050018", "资金组织不能为空") /* "资金组织不能为空" */);
        }
        String accentity = params.get(ICmpConstant.ACCENTITY).asText();
        renderJson(response, ResultMessage.data(cmCommonService.queryAutoConfigByAccentity(accentity)));
    }

    /**
     * 获取现金参数-转账单是否推送结算状态
     *
     * @param response
     */
    @PostMapping("getCheckFundTransfer")
    @CMPDiworkPermission(IServicecodeConstant.TRANSFERACCOUNT)
    public void getCheckFundTransfer(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getCheckFundTransfer()));
    }

    /**
     * 获取现金参数-转账单是否推送结算状态
     *
     * @param response
     */
    @PostMapping("getCheckFundTransferForAssociation")
    @CMPDiworkPermission({IServicecodeConstant.TRANSFERACCOUNT, IServicecodeConstant.BILLCLAIMCARD, IServicecodeConstant.CMPBANKRECONCILIATION, IServicecodeConstant.CMP_BILLCLAIMCENTERLIST})
    public void getCheckFundTransferForAssociation(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getCheckFundTransferForAssociation()));
    }

    /**
     * 获取现金参数- 重空凭证参数
     *
     * @param response
     */
    @PostMapping("/getCheckStockIsUse")
    @CMPDiworkPermission(IServicecodeConstant.TRANSFERACCOUNT)
    public void getCheckStockIsUse(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getCheckStockIsUse()));
    }

    /**
     * 获取全局参数状态
     *
     * @param response
     */
    @PostMapping("/getGlobalConfig")
    @DiworkPermission({IServicecodeConstant.TRANSFERACCOUNT, IServicecodeConstant.FUNDCOLLECTION, IServicecodeConstant.FUNDPAYMENT,
            IServicecodeConstant.BILLCLAIMCARD, IServicecodeConstant.BANKRECONCILIATION, IServicecodeConstant.CMPBANKRECONCILIATION})
    public void getGlobalConfig(@RequestBody ObjectNode params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getGlobalConfig()));
    }

    /**
     * 获取现金参数-我的认领是否需要复核
     *
     * @param response
     */
    @PostMapping("/getIsRecheck")
    @CMPDiworkPermission(IServicecodeConstant.BILLCLAIMCARD)
    public void getIsRecheck(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoConfigService.getIsRecheck()));
    }
}
