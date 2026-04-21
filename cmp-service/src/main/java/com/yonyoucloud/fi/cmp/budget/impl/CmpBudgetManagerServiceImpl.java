package com.yonyoucloud.fi.cmp.budget.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.epmp.control.dto.ControlDetailVO;
import com.yonyou.epmp.control.service.ExecdataControlService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.budget.BudgetDirect;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.constants.CommonConstant;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.RestTemplateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.tmsp.openapi.ITmspSystemRespRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemReq;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmpBudgetManagerServiceImpl implements CmpBudgetManagerService {

    // 拼装子表报文字段前缀
    public static final String FUNDPAYMENT_B = "FundPayment_b__";
    public static final String FUNDCOLLECTION_B = "FundCollection_b__";
    public static final String BATCHTRANSFERACCOUNT_B = "BatchTransferAccount_b__";
    // 预占
    public static final String PRE = "pre";
    // 实占
    public static final String IMPLEMENT = "implement";

    public static final String CHARACTERDEF = "characterDef_";
    public static final String CHARACTERDEFB = "characterDefb_";

    @Autowired
    private ExecdataControlService execdataControlService;
    @Autowired
    private BusiSystemConfigService busiSystemConfigService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    @Value("${billdetail-url}")
    private String billDetailUrl;

    @Override
    public boolean isCanUseBudget() {
        try {
            return AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_SEPMBCDC_APP_CODE);
        } catch (Exception e) {
            log.error("请求失败 发生异常 {}", e);//@notranslate
        }
        return Boolean.FALSE;
    }

    @Override
    public boolean isCanUseSpecialBudget() {
        try {
            //根据应用编码和租户id查询是否购买预算
            boolean enableBFF = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_CTMPUB_BFF_APP_CODE);
            return enableBFF;
        } catch (Exception e) {
            log.error("请求失败 发生异常 {}", e);//@notranslate
        }
        return Boolean.FALSE;
    }

    @Override
    public boolean isPushHistory() throws Exception {
        // 查询现金参数，获取：“是否推送历史数据”（直接获取企业级账户的数据）
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        if (query.size() > 0) {
            return query.get(0).get("isPushHistoryData").equals(0) ? false : true;
        } else {
            // 未取到现金参数，默认不推送
            return false;
        }
    }

    @Override
    public boolean isStartBudget(String billCode) {
        //是否开启配置预算 rpc
        ITmspSystemRespRpcService tmspSystemRespRpcService = AppContext.getBean(ITmspSystemRespRpcService.class);
        TmspSystemReq tmspSystemReq = new TmspSystemReq();
        tmspSystemReq.setApplyname(BudgetUtils.CTM);// 现金管理是 6
        if (IBillNumConstant.FUND_PAYMENT.equals(billCode)) {
            tmspSystemReq.setServicename(BudgetUtils.FUNDPAYMENT);
        } else if (IBillNumConstant.SALARYPAY.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.SALARY_PAYMENT.getValue());
        } else if (IBillNumConstant.CMP_PAYMARGIN.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.PAY_MARGIN.getValue());
        } else if (IBillNumConstant.CMP_RECEIVEMARGIN.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.RECEIVE_MARGIN.getValue());
        } else if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.FOREIGN_PAYMENT.getValue());
        } else if (IBillNumConstant.TRANSFERACCOUNT.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.TRANSFER_ACCOUNT.getValue());
        } else if (IBillNumConstant.CURRENCYEXCHANGE.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.CURRENCY_EXCHANGE.getValue());
        } else if (IBillNumConstant.CMP_BATCHTRANSFERACCOUNT.equals(billCode)) {
            tmspSystemReq.setServicename(SystemIntegrationParamsEnum.BATCH_TRANSFER_ACCOUNT.getValue());
        } else {
            tmspSystemReq.setServicename(BudgetUtils.FUNDCOLLECTION);
        }
        //todo
        try {
            List<TmspSystemResp> tmspSystemRespList = tmspSystemRespRpcService.querySystemParameters(tmspSystemReq);
            if (CollectionUtils.isEmpty(tmspSystemRespList)) {
                return false;
            }
            TmspSystemResp resp = tmspSystemRespList.get(0);
            // 受控资金计划 受控预算管控
            if (StringUtils.isEmpty(resp.getControlintegration()) && StringUtils.isEmpty(resp.getCsplControlledContent())) {
                return false;
            }
            // 受控预算管控
            if (resp.getControlintegration() != null && resp.getControlintegration().contains("1")) {
                return true;
            }
            // 资金计划控制
            if (resp.getCsplControlledContent() != null && resp.getCsplControlledContent().contains("1")) {
                return true;
            }
        } catch (Exception e) {
            log.error("execute tmsp ITmspSystemRespRpcService querySystemParameters error", e);
        }
        return false;
    }

    @Override
    public boolean isCanStart(String billNum) {
        //1 是否开启预算
        if (!isCanUseBudget() && !isCanUseSpecialBudget()) {
            return false;
        }
        //2 是否开启公共配置
        if (!isStartBudget(billNum)) {
            return false;
        }
        return true;
    }

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initFundPaymentBill(BizObject bizObject, FundPayment_b fundPayment_b, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        BizObject bizObjectMain = new BizObject(bizObject);
        bizObjectMain.remove("FundPayment_b");
        jo.putAll(bizObjectMain);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.FUNDPAYMENT);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//动作
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(fundPayment_b.get("id")) ? fundPayment_b.get("id").toString() : ymsOidGenerator.nextId());//单据id，用来拼接unquieRequestId
        String headPubts = ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ? bizObject.get("pubts").toString() : new Date().toString();
        String subPubts = ValueUtils.isNotEmptyObj(fundPayment_b.get("pubts")) ? fundPayment_b.get("pubts").toString() : new Date().toString();
        String requsetbillpubts = headPubts + "|" + subPubts;
        jo.put("requsetbillpubts", requsetbillpubts);//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(fundPayment_b.get("lineno"))
                ? fundPayment_b.get("lineno") instanceof BigDecimal ? ((BigDecimal)fundPayment_b.get("lineno")).intValue() : fundPayment_b.get("lineno").toString()
                : ymsOidGenerator.nextId());//单据行id 默认1
        jo.put("billLineId", fundPayment_b.getId());
        jo.put("lineRequestUniqueId", fundPayment_b.getId());
        // 特征字段拼接 例：characterDef_test0919
        if (bizObject.get("characterDef") != null && bizObject.get("characterDef") instanceof Map) {
            Map characterDefMap = bizObject.get("characterDef");
            if (!characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> jo.put(CHARACTERDEF + key, value));
            }
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else if (IMPLEMENT.equals(preemptionOrExecFlag)) {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        if (fundPayment_b != null) {
            CtmJSONObject jsonObject_b = toJsonObj(fundPayment_b);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(FUNDPAYMENT_B + e.getKey(), e.getValue());
                }
            });
            // FundPayment_b__characterDef_test0919
            if (fundPayment_b.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = fundPayment_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(FUNDPAYMENT_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
    }

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initFundCollectionBill(BizObject bizObject, FundCollection_b fundCollection_b, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        BizObject bizObjectMain = new BizObject(bizObject);
        bizObjectMain.remove("FundCollection_b");
        jo.putAll(bizObjectMain);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.FUNDCOLLECTION);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//单据类型唯一标识
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(bizObject.get("id")) ? fundCollection_b.get("id").toString() : ymsOidGenerator.nextId());//单据id，用来拼接unquieRequestId
        String headPubts = ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ? bizObject.get("pubts").toString() : new Date().toString();
        String subPubts = ValueUtils.isNotEmptyObj(fundCollection_b.get("pubts")) ? fundCollection_b.get("pubts").toString() : new Date().toString();
        String requsetbillpubts = headPubts + "|" + subPubts;
        jo.put("requsetbillpubts", requsetbillpubts);//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(fundCollection_b.get("lineno"))
                ? fundCollection_b.get("lineno") instanceof BigDecimal ? ((BigDecimal)fundCollection_b.get("lineno")).intValue() : fundCollection_b.get("lineno").toString()
                : ymsOidGenerator.nextId());//单据行id 默认1
        // 特征字段拼接 例：characterDef_test0919
        jo.put("billLineId", fundCollection_b.getId());
        jo.put("lineRequestUniqueId", fundCollection_b.getId());
        Map characterDefMap = bizObject.get("characterDef");
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> jo.put(CHARACTERDEF + key, value));
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else if (IMPLEMENT.equals(preemptionOrExecFlag)) {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        if (fundCollection_b != null) {
            CtmJSONObject jsonObject_b = toJsonObj(fundCollection_b);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(FUNDCOLLECTION_B + e.getKey(), e.getValue());
                }
            });
            // FundPayment_b__characterDef_test0919
            if (fundCollection_b.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = fundCollection_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(FUNDCOLLECTION_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
    }

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initFundPaymentBillCheck(BizObject bizObject, FundPayment_b fundPayment_b, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction, int index, String transacCode) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        BizObject bizObjectMain = new BizObject(bizObject);
        bizObjectMain.remove("FundPayment_b");
        jo.putAll(bizObjectMain);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.FUNDPAYMENT);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//单据类型唯一标识
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(fundPayment_b.get("id")) ?fundPayment_b.get("id").toString() : ymsOidGenerator.nextId());//id，用来拼接unquieRequestId
        String headPubts = ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ? bizObject.get("pubts").toString() : new Date().toString();
        String subPubts = ValueUtils.isNotEmptyObj(fundPayment_b.get("pubts")) ? fundPayment_b.get("pubts").toString() : new Date().toString();
        String requsetbillpubts = headPubts + "|" + subPubts;
        jo.put("requsetbillpubts", requsetbillpubts);//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", transacCode);//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(fundPayment_b.get("lineno"))
                ? fundPayment_b.get("lineno") instanceof BigDecimal ? ((BigDecimal)fundPayment_b.get("lineno")).intValue() : fundPayment_b.get("lineno").toString()
                : ymsOidGenerator.nextId());//单据行id 默认1
        // 特征字段拼接 例：characterDef_test0919
        jo.put("billLineId", fundPayment_b.getId());// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("lineRequestUniqueId", fundPayment_b.getId());
        Map characterDefMap = bizObject.get("characterDef");
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> jo.put(CHARACTERDEF + key, value));
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else if (IMPLEMENT.equals(preemptionOrExecFlag)) {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        if (fundPayment_b != null) {
            CtmJSONObject jsonObject_b = toJsonObj(fundPayment_b);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(FUNDPAYMENT_B + e.getKey(), e.getValue());
                }
            });
            // FundPayment_b__characterDef_test0919
            if (fundPayment_b.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = fundPayment_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(FUNDPAYMENT_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
    }


    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initBatchTransferAccountBillCheck(BizObject bizObject, BatchTransferAccount_b batchTransferAccountB, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction, String transacCode) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        BizObject bizObjectMain = new BizObject(bizObject);
        bizObjectMain.remove("BatchTransferAccount_b");
        jo.putAll(bizObjectMain);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.BATCH_TRANSFERACCOUNT);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//单据类型唯一标识
        jo.put("requsetbilltradetype", bizObject.get("tradeType").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(batchTransferAccountB.get("id")) ?batchTransferAccountB.get("id").toString() : ymsOidGenerator.nextId());//id，用来拼接unquieRequestId
        String headPubts = ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ? bizObject.get("pubts").toString() : new Date().toString();
        String subPubts = ValueUtils.isNotEmptyObj(batchTransferAccountB.get("pubts")) ? batchTransferAccountB.get("pubts").toString() : new Date().toString();
        String requsetbillpubts = headPubts + "|" + subPubts;
        jo.put("requsetbillpubts", requsetbillpubts);//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", transacCode);//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(batchTransferAccountB.get("lineno"))
                ? batchTransferAccountB.get("lineno") instanceof BigDecimal ? ((BigDecimal)batchTransferAccountB.get("lineno")).intValue() : batchTransferAccountB.get("lineno").toString()
                : ymsOidGenerator.nextId());//单据行id 默认1
        // 特征字段拼接 例：characterDef_test0919
        jo.put("billLineId", batchTransferAccountB.getId());// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("lineRequestUniqueId", batchTransferAccountB.getId());
        Map characterDefMap = bizObject.get("characterDef");
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> jo.put(CHARACTERDEF + key, value));
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else if (IMPLEMENT.equals(preemptionOrExecFlag)) {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        if (batchTransferAccountB != null) {
            CtmJSONObject jsonObject_b = toJsonObj(batchTransferAccountB);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(BATCHTRANSFERACCOUNT_B + e.getKey(), e.getValue());
                }
            });
            // FundPayment_b__characterDef_test0919
            if (batchTransferAccountB.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = batchTransferAccountB.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(BATCHTRANSFERACCOUNT_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
    }

    /**
     * *
     *
     * @param bills
     */
//    private ControlDetailVO initControlDetailVO(Map[] bills, String action, int operateFlag) throws Exception {
//        for (int i = 0; i < bills.length; i++) {
//            if(null != bills[i].get("signature")){
//                bills[i].put("signature", null);
//            }
//        }
//        ControlDetailVO controlDetailVO = new ControlDetailVO();
//        //担保变更预占
//        controlDetailVO.setBills(bills);//业务单据数组
//        controlDetailVO.setBusiSysCode(BudgetUtils.SYSCODE);
//        controlDetailVO.setYtenantId(InvocationInfoProxy.getTenantid());
//        controlDetailVO.setRequestUniqueId(UUID.randomUUID().toString());//请求唯一标识 请求唯一标识，同一标识数据幂等处理，防止重复提交数据。同时 数据处理完之后返回消息中会携带返回此标识，可明确成功数据。  业务系统（id或code）+单据类型（id或code）+交易类型（id或code）+单据id+单据动作code+pubts
//        controlDetailVO.setOperateFlag(operateFlag);//0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
////        controlDetailVO.setLedgerFlag(1);//ledgerFlag，0代表不记录台账，1代表记录台账,默认为0，如果需要保存台账相关信息，可以传1
//        controlDetailVO.setAction(action);//动作类型
//        /**
//         * 预算控制执行开启-业务参数-[临时]预算转移特殊逻辑(仅指定项目才需开启)=是
//         * 如付款申请单与付款单均占用预算，且只占一份预算需转移是使用，目前现金管理无此类逻辑，domainRelease设置为true走旧逻辑
//         */
//        controlDetailVO.setServiceCode(IServicecodeConstant.FUNDPAYMENT);
//        controlDetailVO.setDomainRelease(true);
//        return controlDetailVO;
//
//    }

    /**
     * isMatch为true且ctrlType!=1时，代表占用成功
     * isMatch为是否匹配上方案（true为是，false为否）
     *
     * @param result
     * @return
     */
    public  ResultBudget doResult(CtmJSONObject result){
        if(CommonConstant.SC_OK.equals(result.getString("code"))){
            CtmJSONObject data = result.getJSONObject("data");
            //和预算确认，有data为空的场景，认为是没有匹配上
            if (data == null) {
                // 未匹配上，直接返回失败
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            CtmJSONArray billInfos = data.getJSONArray("billInfo");
            CtmJSONArray matchInfos = data.getJSONArray("matchInfo");
            if (matchInfos == null && billInfos == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100385"), result.getString("message"));
            }
            if (billInfos.size() > 1) {
                List<String> ids = new ArrayList();
                //大于1笔
                for (int i = 0; i < billInfos.size(); i++) {
                    Map billInfo = (Map) billInfos.get(i);
                    boolean isMatch = (Boolean) billInfo.get("isMatch");
                    if (matchInfos == null) {
                        return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                    }
                    if (isMatch) {
                        String detaiId = StringUtils.isNotEmpty(billInfo.get("lineRequestUniqueId").toString())?billInfo.get("lineRequestUniqueId").toString():billInfo.get("lineNo").toString();
                        ids.add(detaiId);
                        // 仅抛错，判断ctrlType==1
                        doMatchInfos(matchInfos);
                    }
                }
                return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true, ids);
            } else {
                Map billInfo = (Map) billInfos.get(0);
                boolean isMatch = (Boolean) billInfo.get("isMatch");
                if (isMatch) {
//                    // 先判断是否isMatch，没匹配上matchInfos为空
//                    Integer ctrlType = matchInfos.getJSONObject(0).getInteger("ctrlType");
//                    /**
//                     *  控制类型
//                     *  0:正常更新预占/执行数；1：强控超预算（刚性规则时超控制百分比，此时为强控下超预算，一般需要终止制单操作）；2：超预算（预警规则时超控制百分比）；3：超提示（刚性规则时或预警规则时超提示百分比）
//                     */
//                    if (ctrlType != null && ctrlType == 1) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100385"), matchInfos.getJSONObject(0).getString("warning"));
//                    } else {
//                        // 为0则直接表示成功
//                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
//                    }
                    // 仅抛错，判断ctrlType==1
                    String msg = getMatchInfosMsg(matchInfos);
                    if(StringUtils.isNotEmpty( msg)){
                        throw new CtmException(new CtmErrorCode("033-502-100385"), msg);
                    }
                    // 若无抛错，则返回成功
                        return new ResultBudget(OccupyBudget.PreSuccess.getValue(), true);
                } else {
                    // 未匹配上，直接返回失败
                    return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
                }
            }
        } else {
            throw new CtmException(new CtmErrorCode("033-502-100386"), result.getString("message"));
        }
    }

    /**
     * 匹配 规则信息
     * 拼接预算报错提示语
     *
     * @param matchInfos
     * @return
     */
    private void doMatchInfos(CtmJSONArray matchInfos) {
        if (matchInfos != null && matchInfos.size() > 0) {
            //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
            for (int i = 0; i < matchInfos.size(); i++) {
                CtmJSONObject matchInfo = matchInfos.getJSONObject(i);
                Integer ctrlType = matchInfo.getInteger("ctrlType");
                if (ctrlType != null && (ctrlType == 1)) {
                    //预算系统返回的提示信息
                    throw new CtmException(new CtmErrorCode("033-502-100387"), matchInfo.getString("warning"));
                }
            }
        }
    }

    private String getMatchInfosMsg(CtmJSONArray matchInfos) {
        StringBuilder sb = new StringBuilder();
        if (matchInfos != null && matchInfos.size() > 0) {
            //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
            for (int i = 0; i < matchInfos.size(); i++) {
                CtmJSONObject matchInfo = matchInfos.getJSONObject(i);
                Integer ctrlType = matchInfo.getInteger("ctrlType");
                if (ctrlType != null && (ctrlType == 1)) {
                    //预算系统返回的提示信息
                    sb.append(matchInfo.getString("warning")).append("\r\n");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public ResultBudget gcExecuteSubmit(BizObject bizObject, FundPayment_b fundPayment_b, String billCode, String billAction) throws Exception {
        try {
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = PRE;
            if (preemptionOrExecFlag == null) {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 拼装报文
            Map<String, Object> jo = initFundPaymentBill(bizObject, fundPayment_b, BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction);
            Map<String, Object>[] bills = new Map[1];
            bills[0] = jo;
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action,0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    public ResultBudget gcExecuteBatchSubmit(BizObject bizObject, List<FundPayment_b> fundPayment_bList, String billCode, String billAction) throws Exception {
        try {
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = PRE;
            if (preemptionOrExecFlag == null) {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 拼装报文
            int size = fundPayment_bList.size();
            Map<String, Object>[] bills = new Map[size];
            for (int i = 0; i < size; i++) {
                Map copiedMap = new HashMap<>(initFundPaymentBill(bizObject, fundPayment_bList.get(i), BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action,0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    public ResultBudget gcExecuteUnSubmit(BizObject bizObject, FundPayment_b fundPayment_b, String billCode, String billAction) throws Exception {
        try {
            if (fundPayment_b.getIsOccupyBudget() != null && fundPayment_b.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String action;
            // 查询是否开启预算开关
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = PRE;
            // 生成
            Map bill = initFundPaymentBill(bizObject, fundPayment_b, BudgetDirect.REDUCE.getIndex(), billCode, preemptionOrExecFlag, billAction);
            Map[] bills = new Map[1];
            bills[0] = bill;
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action,0);
            log.error("预算释放预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放预占响应报文： ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget gcExecuteBatchUnSubmit(BizObject bizObject, List<FundPayment_b> fundPayment_bList, String billCode, String billAction) throws Exception {
        try {
            String action;
            int addOrReduce = 1;//删除
            // 查询是否开启预算开关
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = PRE;
            // 生成
            Map<String, Object>[] bills = new Map[fundPayment_bList.size()];
            for (int i = 0; i < fundPayment_bList.size(); i++) {
                Map copiedMap = new HashMap<>(initFundPaymentBill(bizObject, fundPayment_bList.get(i), BudgetDirect.REDUCE.getIndex(), billCode, preemptionOrExecFlag, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action,0);
            log.error("预算释放预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放预占响应报文 ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 实占由于是结算回调，每条子表数据结算单独触发，只能单条调用
     *
     * @param bizObject
     * @param fundPayment_b
     * @param billCode      单据编码
     * @param billAction
     * @return
     */
    public ResultBudget gcExecuteTrueAudit(BizObject bizObject, FundPayment_b fundPayment_b, String billCode, String billAction) {
        try {
            String preAction;
            String implementAction;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                implementAction = (String) objects.get(0).get("implement");
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction ", "not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            preAction = (String) objects.get(0).get(PRE);
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVPre;
            ControlDetailVO controlDetailVImplement;
            if (!StringUtils.isEmpty(preAction)) {
                Map preBill = initFundPaymentBill(bizObject, fundPayment_b, BudgetDirect.REDUCE.getIndex(), billCode, PRE, BillAction.APPROVE_PASS);
                controlDetailVPre = cmpBudgetCommonManagerService.initControlDetailVO(preBill, preAction, 0);
                log.error("预算实占释放预占请求报文： " + controlDetailVPre);
                Map implementBill = initFundPaymentBill(bizObject, fundPayment_b, BudgetDirect.ADD.getIndex(), billCode, IMPLEMENT, BillAction.APPROVE_PASS);
                controlDetailVImplement = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                log.error("预算实占请求报文： " + controlDetailVImplement);
                controlDetailVOS = new ControlDetailVO[]{controlDetailVPre, controlDetailVImplement};
            } else {
                Map implementBill = initFundPaymentBill(bizObject, fundPayment_b, BudgetDirect.ADD.getIndex(), billCode, IMPLEMENT, BillAction.APPROVE_PASS);
                Map[] billsImplement = new Map[]{implementBill};

                controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(billsImplement, implementAction,0);
                log.error("预算实占请求报文： " + controlDetailVOS[0]);
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算实占响应报文： ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 实占由于是结算回调，每条子表数据结算单独触发，只能单条调用
     *
     * @param bizObject
     * @param fundCollection_b
     * @param billCode         单据编码
     * @param billAction
     * @return
     */
    public ResultBudget fundCollectionEmployActualOccupySuccessAudit(BizObject bizObject, FundCollection_b fundCollection_b, String billCode, String billAction) {
        try {
            String implementAction;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                implementAction = (String) objects.get(0).get("implement");
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction ", "not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            controlDetailVOS = new ControlDetailVO[1];
            Map implementBill = initFundCollectionBill(bizObject, fundCollection_b, BudgetDirect.ADD.getIndex(), billCode, IMPLEMENT, BillAction.APPROVE_PASS);
            ControlDetailVO controlDetailVO = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
            controlDetailVO.setServiceCode(IServicecodeConstant.FUNDCOLLECTION);
            controlDetailVOS[0] = controlDetailVO;
            log.error("预算实占请求报文： " + controlDetailVOS[0]);
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算实占响应报文： ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    public ResultBudget gcExecuteTrueUnAudit(BizObject bizObject, List<FundPayment_b> fundPayment_bList, String billCode, String billAction) {
        try {
            String preAction;
            String implementAction;
            int size = fundPayment_bList.size();
            Map[] preBills = new Map[size];// 预占一条数据，实占一条数据
            Map[] implementBills = new Map[size];
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                preAction = (String) objects.get(0).get(PRE);
                implementAction = (String) objects.get(0).get("implement");
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction ", "not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 未配置预占动作，则无需预占，直接释放实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO[] controlDetailVOPre;
            ControlDetailVO[] controlDetailVOImplement;
            if (!StringUtils.isEmpty(preAction)) {
                for (int i = 0; i < size; i++) {
                    Map<String, Object> bill = initFundPaymentBill(bizObject, fundPayment_bList.get(i), BudgetDirect.ADD.getIndex(), billCode, PRE, billAction);
                    preBills[i] = bill;
                }
//                controlDetailVOS = new ControlDetailVO[2];
                controlDetailVOPre = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, preAction, 0);
                for (int i = 0; i < size; i++) {
                    Map<String, Object> bill = initFundPaymentBill(bizObject, fundPayment_bList.get(i), BudgetDirect.REDUCE.getIndex(), billCode, IMPLEMENT, billAction);
                    implementBills[i] = bill;
                }
                controlDetailVOImplement = cmpBudgetCommonManagerService.initControlDetailVOs(implementBills, implementAction, 0);
                List<ControlDetailVO> controlDetailVOSList = new ArrayList<>();
                controlDetailVOSList.addAll(Arrays.asList(controlDetailVOPre));
                controlDetailVOSList.addAll(Arrays.asList(controlDetailVOImplement));
                controlDetailVOS = controlDetailVOSList.toArray(new ControlDetailVO[0]);
                log.error("预算释放实占占用预占请求报文：" + controlDetailVOPre);
                log.error("预算释放实占请求报文：" + controlDetailVOImplement);
            } else {
                for (int i = 0; i < size; i++) {
                    Map<String, Object> bill = initFundPaymentBill(bizObject, fundPayment_bList.get(i), BudgetDirect.REDUCE.getIndex(), billCode, IMPLEMENT, billAction);
                    implementBills[i] = bill;
                }
                controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(implementBills, implementAction, 0);
                log.error("预算释放实占请求报文：" + controlDetailVOS);
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放实占响应报文： ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


    @Override
    public ResultBudget fundCollectionReleaseActualOccupySuccessUnAudit(BizObject bizObject, List<FundCollection_b> fundCollection_bList, String billCode, String billAction) {
        try {
            String implementAction;
            int size = fundCollection_bList.size();
            Map[] implementBills = new Map[size];
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            // 获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                implementAction = (String) objects.get(0).get("implement");
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction ", "not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 未配置预占动作，则无需预占，直接释放实占
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initFundCollectionBill(bizObject, fundCollection_bList.get(i), BudgetDirect.REDUCE.getIndex(), billCode, IMPLEMENT, billAction);
                implementBills[i] = bill;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(implementBills, implementAction, 0);
            log.error("预算释放实占请求报文：" + controlDetailVOS);

            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放实占响应报文： ", CtmJSONObject.toJSONString(result));
            return doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


    private CtmJSONObject toJsonObj(Map<String, Object> map) {
        CtmJSONObject resultJson = new CtmJSONObject();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            resultJson.put(key, map.get(key));
        }
        return resultJson;
    }

    /**
     * 查询预算执行情况
     * @param json
     * @return
     */
    @Override
    public String queryBudgetDetail(CtmJSONObject json) {
        try {
            String billNum = json.getString("billno");
            Long id = json.getLong("id");
            if (id == null || StringUtils.isEmpty(billNum)) {
                throw new CtmException(new CtmErrorCode("033-502-100235"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_1811A1FC05B00042", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380012", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540062A", "请求参数缺失") /* "请求参数缺失" */) /* "请求参数缺失" */) /* "请求参数缺失" */);
            }
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, false);
            Map[] bills = new Map[0];
            String billAction = BillAction.SUBMIT;
            String preemptionOrExecFlag = "";
            if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
                FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id);
                String billCode = IBillNumConstant.FUND_COLLECTION;
                int addOrReduce = 0;//新增
                //查询预占单据
                bills = new Map[fundCollection.FundCollection_b().size()];
                for (int i = 0; i < fundCollection.FundCollection_b().size(); i++) {
                    FundCollection_b fundCollection_b = fundCollection.FundCollection_b().get(i);
                    if (fundCollection_b.getIsOccupyBudget() != null && fundCollection_b.getIsOccupyBudget() == OccupyBudget.PreSuccess.getValue()) {
                        preemptionOrExecFlag = PRE;
                    } else if (fundCollection_b.getIsOccupyBudget() != null && fundCollection_b.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
                        preemptionOrExecFlag = IMPLEMENT;
                    } else {
                        billAction = BillAction.QUERY;
                    }
                    Map jo = initFundCollectionBill(fundCollection, fundCollection.FundCollection_b().get(i), addOrReduce, billCode, preemptionOrExecFlag, billAction);
                    bills[i] = jo;
                }
            } else if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id);
                String billCode = IBillNumConstant.FUND_PAYMENT;
                int addOrReduce = 0;//新增
                //查询预占单据
                bills = new Map[fundPayment.FundPayment_b().size()];
                for (int i = 0; i < fundPayment.FundPayment_b().size(); i++) {
                    FundPayment_b payment_b = fundPayment.FundPayment_b().get(i);
                    if (payment_b.getIsOccupyBudget() != null && payment_b.getIsOccupyBudget() == OccupyBudget.PreSuccess.getValue()) {
                        preemptionOrExecFlag = PRE;
                    } else if (payment_b.getIsOccupyBudget() != null && payment_b.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
                        preemptionOrExecFlag = IMPLEMENT;
                    } else {
                        billAction = BillAction.QUERY;
                    }
                    Map jo = initFundPaymentBill(fundPayment, fundPayment.FundPayment_b().get(i), addOrReduce, billCode, preemptionOrExecFlag, billAction);
                    bills[i] = jo;
                }
            }else if (IBillNumConstant.CMP_BATCHTRANSFERACCOUNT.equals(billNum)) {
                // 解决循环依赖
                CmpBudgetBatchTransferAccountManagerService cmpBudgetBatchTransferAccountManagerService = CtmAppContext.getBean(CmpBudgetBatchTransferAccountManagerService.class);
                bills = cmpBudgetBatchTransferAccountManagerService.getBudgetBills(billNum, id);
            }
            CtmJSONArray requestJs = new CtmJSONArray();
            CtmJSONObject request = new CtmJSONObject();
            request.put("bills", bills);//            bills	Y	业务单据数组	Array
            request.put("busiBillId", id);//            busiBillId	Y	单据主键	String
            request.put("busiSysCode", BudgetUtils.SYSCODE);//            busiSysCode	Y	业务系统编码	String
            request.put("ytenantId", InvocationInfoProxy.getTenantid());//           //            ytenantId	Y	租户ID	String
            requestJs.add(request);
            CtmJSONObject result = RestTemplateUtils.doPostByJSONArray(billDetailUrl, requestJs);
            log.error("result ", result.toString());
            if (ICmpConstant.REQUEST_SUCCESS_STATUS_CODE.equals(result.getString("code"))) {
            } else {
                throw new CtmException(new CtmErrorCode("033-502-100388"), result.getString("message"));
            }
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(List<BizObject> bizObjects, String billCode, String billAction) {
        try {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, false);
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", "no isCanStart");
                return ResultMessage.data(resultBack);
            }
            //operateFlag 0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
            int operateFlag = 2;

            List<Map<String, Object>> bills = new ArrayList<>();
            for (BizObject bizObject : bizObjects) {
                List<FundPayment_b> fundPaymentBList = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
                String bizObjectTransCode = cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype"));
                if (verifystate == VerifyState.SUBMITED.getValue()) {
                    Object id = bizObject.getId();
                    BizObject bizObjectDB = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, 3);
                    List<FundPayment_b> fundPaymentSubDBList = bizObjectDB.getBizObjects("FundPayment_b", FundPayment_b.class);
                    Map<Long, FundPayment_b> fundPaymentSubDBMap = new HashMap<>();
                    for (FundPayment_b fundPaymentB : fundPaymentSubDBList) {
                        fundPaymentSubDBMap.put(Long.parseLong(fundPaymentB.getId().toString()), fundPaymentB);
                    }
                    int index = 1;
                    String bizObjecDBtTransCode = cmCommonService.getDefaultTransTypeCode(bizObjectDB.getString("tradetype"));
                    for (FundPayment_b fundPayment_b : fundPaymentBList) {
                        if (fundPayment_b.get("settlestatus") != null
                                && Short.parseShort(fundPayment_b.get("settlestatus").toString()) == FundSettleStatus.Refund.getValue()) {
                            continue;
                        }
                        Map<String, Object> billAdd = initFundPaymentBillCheck(bizObject, fundPayment_b, BudgetDirect.ADD.getIndex(), billCode, PRE, action, index,bizObjectTransCode);
                        bills.add(billAdd);
                        String idStr = fundPayment_b.getId().toString();
                        Long idKey = Long.parseLong(idStr);
                        Map<String, Object> billReduce = initFundPaymentBillCheck(bizObjectDB, fundPaymentSubDBMap.get(idKey), BudgetDirect.REDUCE.getIndex(), billCode, PRE, action, index,bizObjecDBtTransCode);
                        bills.add(billReduce);
                        index++;
                    }
                } else {
                    int index = 1;
                    for (FundPayment_b fundPayment_b : fundPaymentBList) {
                        if (fundPayment_b.get("settlestatus") != null
                                && Short.parseShort(fundPayment_b.get("settlestatus").toString()) == FundSettleStatus.Refund.getValue()) {
                            continue;
                        }
                        Map<String, Object> bill = initFundPaymentBillCheck(bizObject, fundPayment_b, BudgetDirect.ADD.getIndex(), billCode, PRE, action, index,bizObjectTransCode);
                        bills.add(bill);
                        index++;
                    }
                }
            }
            if (CollectionUtils.isEmpty(bills)) {
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", "no isCanStart");
                return ResultMessage.data(resultBack);
            }
            Map[] preBills = bills.toArray(new Map[0]);
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, action, operateFlag);
            //4 控制接口(预占、实占)
            //log.error("budgetCheck controlDetailVOS={}", CtmJSONObject.toJSONString(controlDetailVOS));
            Map map = execdataControlService.control(controlDetailVOS);
            String resultStr = CtmJSONObject.toJSONString(map);
            CtmJSONObject result = CtmJSONObject.parseObject(resultStr);
            processResult(result);
            log.error("execdataControlService.control, result={}", result.toString());
            doCheckResult(resultBack, result);
            log.error("execdataControlService.doCheckResult, result={}", resultBack);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


    /**
     * 预占调用返回结果提示信息去重(一正一负的场景)
     *
     * @param result
     */
    private void processResult(CtmJSONObject result) {
        try {
            // 获取 "data" 和 "matchInfo"
            Map<String, List<Map<String, String>>> data = Optional.ofNullable((Map<String, List<Map<String, String>>>) result.get("data"))
                    .orElse(Collections.emptyMap());
            List<Map<String, String>> matchInfoList = Optional.ofNullable(data.get("matchInfo"))
                    .orElse(Collections.emptyList());

            // 处理 matchInfoList
            List<Map<String, String>> collect = matchInfoList.stream()
                    .filter(Objects::nonNull) // 确保 info 不为 null
                    .filter(info -> info.get("warning") != null)
                    .collect(Collectors.toMap(info -> info.get("warning"), replacement -> replacement,
                            (existing, replacement) -> existing))
                    .values()
                    .stream()
                    .collect(Collectors.toList());

            // 如果经过处理后有数据，将其放回 "data" 和 "result"
            if (!collect.isEmpty()) {
                data.put("matchInfo", collect);
                result.put("data", data);
            }
        } catch (Exception e) {
            log.error("execute ", e);
        }

    }

    //预占校验结果返回
    private String doCheckResult(CtmJSONObject resultBack, CtmJSONObject result) {
        if (CommonConstant.SC_OK.equals(result.getString(ICmpConstant.CODE))) {
            CtmJSONObject data = result.getJSONObject("data");
            CtmJSONArray billInfos = data.getJSONArray("billInfo");
            CtmJSONArray matchInfos = data.getJSONArray("matchInfo");
            boolean isSuccess = false;
            if (billInfos != null) {
                // isMatch为是否匹配上方案（true为是，false为否），isOffset为是否冲抵单据（true为是，false为否）
                //大于1 笔 有预支、执行
                for (int i = 0; i < billInfos.size(); i++) {
                    CtmJSONObject billInfo = billInfos.getJSONObject(i);
                    boolean isMatch = billInfo.getBooleanValue("isMatch");
                    if (isMatch) {
                        isSuccess = true;
                    }
                }
            }
            if (!isSuccess) {//所有数据无匹配
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801C3", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400628", "无匹配") /* "无匹配" */) /* "无匹配" */);
                return ResultMessage.data(resultBack);
            }
            if (matchInfos != null) {
                boolean isHashWarning = false;
                boolean isHasForce = false;
                StringBuffer sb = new StringBuffer();
                StringBuffer errsb = new StringBuffer();
                for (int ii = 0; ii < matchInfos.size(); ii++) {
                    CtmJSONObject matchInfo = matchInfos.getJSONObject(ii);
                    //控制类型（0:正常更新预占/执行数；1：强控超预算；2：预警控制超预算；3：超预警提示）
                    Integer ctrlType = matchInfo.getInteger("ctrlType");
                    if (ctrlType != null && (ctrlType == 1)) {
                        //预算系统返回的提示信息
                        // throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100389"),matchInfo.with("waning").asText());
                        errsb.append(matchInfo.getString("warning") + "\n");
                        isHasForce = true;
                    } else if (ctrlType != null && (ctrlType == 3 || ctrlType == 2)) {
                        //预算系统返回的提示信息
                        sb.append(matchInfo.getString("warning") + "\n");
                        isHashWarning = true;
                    }
                }
                if(isHasForce){
                    throw new CtmException(new CtmErrorCode("033-502-100390"), errsb.toString());
                }
                if (isHashWarning) {
                    resultBack.put(ICmpConstant.CODE, false);
                    resultBack.put("message", sb.toString());
                    return ResultMessage.data(resultBack);
                } else {
                    resultBack.put(ICmpConstant.CODE, true);
                }
            }
        } else {
            throw new CtmException(new CtmErrorCode("033-502-100391"), result.getString("message"));
        }
        return null;
    }


    public void fundPaymentExecuteAuditDeleteReleasePre(BizObject fundPayment) throws Exception {
        FundPayment fundPaymentNew = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, fundPayment.getId());
        List<FundPayment_b> list = fundPaymentNew.FundPayment_b();
        List<FundPayment_b> updateList = new ArrayList();
        for (FundPayment_b fundPayment_b : list) {
            Short budgeted = fundPayment_b.getIsOccupyBudget();
            // 已经释放仍要释放，直接跳过不执行了
            if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
                continue;
            }
            updateList.add(fundPayment_b);
        }
        if (updateList.size() > 0) {
            ResultBudget resultBudget = gcExecuteBatchUnSubmit(fundPayment, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                updateList.stream().forEach(fundPayment_b -> {
                    fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    fundPayment_b.setEntityStatus(EntityStatus.Update);
                });
            }
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
        }
    }


    /**
     * 更新预算占用状态
     *
     * @param fundPayment
     * @throws Exception
     */
    @Override
    public void fundPaymentExecuteAuditDeleteReleaseActual(FundPayment fundPayment) throws Exception {
        // 数据被更新过了，需重新查询一次
        FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, fundPayment.getId());
        List<FundPayment_b> list = currentBill.FundPayment_b();
        List<FundPayment_b> updateList = new ArrayList();
        for (FundPayment_b fundPayment_b : list) {
            Short budgeted = fundPayment_b.getIsOccupyBudget();
            // 除非状态为实占成功，其他子表单据全部跳过
            if (budgeted == null || ((budgeted != OccupyBudget.ActualSuccess.getValue()))) {
                continue;
            }
            updateList.add(fundPayment_b);
        }
        if (!updateList.isEmpty()) {
            // 释放实占
            ResultBudget resultBudgetDelActual = gcExecuteTrueUnAudit(currentBill, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_AUDIT);
            if (resultBudgetDelActual.isSuccess()) {
                updateList.stream().forEach(fundPayment_b -> {
                    // 实占成功，弃审后变为预占成功
                    fundPayment_b.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    fundPayment_b.setEntityStatus(EntityStatus.Update);
                });
            }
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
        }
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckBatchTransferAccount(List<BizObject> bizObjects, String billCode, String billAction) {
        try {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, false);
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(PRE);
            } else {
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", "no isCanStart");
                return ResultMessage.data(resultBack);
            }
            //operateFlag 0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
            int operateFlag = 2;

            List<Map<String, Object>> bills = new ArrayList<>();
            for (BizObject bizObject : bizObjects) {
                List<BatchTransferAccount_b> batchTransferAccountBs = bizObject.getBizObjects("BatchTransferAccount_b", BatchTransferAccount_b.class);
                short verifystate = Short.parseShort(bizObject.get("verifystate").toString());
                String bizObjectTransCode = cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradeType"));
                if (verifystate == VerifyState.SUBMITED.getValue()) {
                    Object id = bizObject.getId();
                    BizObject bizObjectDB = MetaDaoHelper.findById(BatchTransferAccount_b.ENTITY_NAME, id, 3);
                    List<BatchTransferAccount_b> batchTransferAccount_bs = bizObjectDB.getBizObjects("BatchTransferAccount_b", BatchTransferAccount_b.class);
                    Map<Long, BatchTransferAccount_b> batchTransferAccountSubDBMap = new HashMap<>();
                    for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccount_bs) {
                        batchTransferAccountSubDBMap.put(Long.parseLong(batchTransferAccountB.getId().toString()), batchTransferAccountB);
                    }
                    int index = 1;
                    String bizObjecDBtTransCode = cmCommonService.getDefaultTransTypeCode(bizObjectDB.getString("tradeType"));
                    for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccountBs) {
                        if (batchTransferAccountB.get("settlestatus") != null
                                && Short.parseShort(batchTransferAccountB.get("settlestatus").toString()) == FundSettleStatus.Refund.getValue()) {
                            continue;
                        }
                        Map<String, Object> billAdd = initBatchTransferAccountBillCheck(bizObject, batchTransferAccountB, BudgetDirect.ADD.getIndex(), billCode, PRE, action, bizObjectTransCode);
                        bills.add(billAdd);
                        String idStr = batchTransferAccountB.getId().toString();
                        Long idKey = Long.parseLong(idStr);
                        Map<String, Object> billReduce = initBatchTransferAccountBillCheck(bizObjectDB, batchTransferAccountSubDBMap.get(idKey), BudgetDirect.REDUCE.getIndex(), billCode, PRE, action, bizObjecDBtTransCode);
                        bills.add(billReduce);
                        index++;
                    }
                } else {
                    int index = 1;
                    for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccountBs) {
                        if (batchTransferAccountB.get("settlestatus") != null
                                && Short.parseShort(batchTransferAccountB.get("settlestatus").toString()) == FundSettleStatus.Refund.getValue()) {
                            continue;
                        }
                        Map<String, Object> bill = initBatchTransferAccountBillCheck(bizObject, batchTransferAccountB, BudgetDirect.ADD.getIndex(), billCode, PRE, action, bizObjectTransCode);
                        bills.add(bill);
                        index++;
                    }
                }
            }
            if (CollectionUtils.isEmpty(bills)) {
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", "no isCanStart");
                return ResultMessage.data(resultBack);
            }
            Map[] preBills = bills.toArray(new Map[0]);
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, action, operateFlag);
            //4 控制接口(预占、实占)
            //log.error("budgetCheck controlDetailVOS={}", CtmJSONObject.toJSONString(controlDetailVOS));
            Map map = execdataControlService.control(controlDetailVOS);
            String resultStr = CtmJSONObject.toJSONString(map);
            CtmJSONObject result = CtmJSONObject.parseObject(resultStr);
            processResult(result);
            log.error("execdataControlService.control, result={}", result.toString());
            doCheckResult(resultBack, result);
            log.error("execdataControlService.doCheckResult, result={}", resultBack);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400629", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

}
