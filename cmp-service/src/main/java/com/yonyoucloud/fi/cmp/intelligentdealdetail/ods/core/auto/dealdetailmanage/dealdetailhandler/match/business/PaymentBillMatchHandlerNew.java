package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;

import com.google.common.collect.ImmutableMap;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dubbo.DubboReferenceUtils;
import com.yonyou.yonbip.ctm.cdp.dto.*;
import com.yonyou.yonbip.ctm.cdp.enums.CDPConditionOperatorEnum;
import com.yonyou.yonbip.ctm.cdp.enums.CDPUseCaseType;
import com.yonyou.yonbip.ctm.cdp.exception.CDPBusinessException;
import com.yonyou.yonbip.ctm.cdp.itf.ICDPQueryService;
import com.yonyou.yonbip.ctm.ctmpub.tmsp.TmspCdpDsRpcService;
import com.yonyou.yonbip.ctm.ctmpub.tmsp.vo.requst.TmspCdpDsReqVO;
import com.yonyou.yonbip.ctm.ctmpub.tmsp.vo.response.TmspCdpDsRespVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.ctm.stwb.reqvo.SettleDeatailRelBankBillReqVO;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrDataProcessingService;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrDataProcessingServiceImpl;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting_b;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.event.constant.IEventCenterConstant;
import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting_b;
import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional.IDealDetailCallBack;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleCheckLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpRuleModuleLog;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CheckRuleCommonUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 收付单据规则
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM005)
@Slf4j
public class PaymentBillMatchHandlerNew extends DefaultStreamBatchHandler {
    private static final String PAYMENT_BILL_MATCH_HANDLER = "PaymentBillMatchHandler";
    private static final Logger logger = LoggerFactory.getLogger(PaymentBillMatchHandlerNew.class);
    private static final String KEY_RULE_CODE = "rule_%s_%s_%s";
    private static final String KEY_EXTFIELD_BUSIOBJ = "busiobject";
    private static final String KEY_EXTFIELD_BANKRECONCILIATIONID = "bankreconciliationid";
    private static final int TIMEOUT_MS = 300000;
    @Resource
    CorrDataProcessingService autoCorrSettingService;
    @Resource
    CorrOperationService corrOperationService;
    @Resource
    ReWriteBusCorrDataService reWriteBusCorrDataService;
    @Resource
    IBankDealDetailAccessDao bankDealDetailAccessDao;

    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) throws Exception {
        long s0 = System.currentTimeMillis();
        if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
            return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
        }
        //step1:准备调度器反参
        Map<String, List<BankReconciliation>> resultMap = this.prepareResult();
        try {
            //step2:加载待处理流水
            List<BankReconciliation> bankReconciliations = this.getBankReconciliationList(context);
            List<BankReconciliation> pendingList = new ArrayList<>();
            bankReconciliations = CheckRuleCommonUtils.processRuleForReturn(bankReconciliations,pendingList);
            //step3:【幂等】已关联流水无需执行关联逻辑
            this.checkBankReconciliationAssociateStatus(bankReconciliations, resultMap);
            if (CollectionUtils.isEmpty(bankReconciliations)) {
                return resultMap;
            }
            //step4:加载收付单据匹配规则大类
            BankreconciliationIdentifyType bankreconciliationIdentifyType = context.getCurrentRule();
            //step4:【核心逻辑】调收付单据匹配处理逻辑
            context.setLogName(RuleLogEnum.RuleLogProcess.PAYMENT_BILL_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.PAYMENT_BILL_START.getDesc());
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus()))){
                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus()).addAll(pendingList);
            }else {
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), pendingList);
            }
            this.streamMainHandler(bankReconciliations, bankreconciliationIdentifyType, resultMap, context);
            log.error("【收付单据匹配】一个批次=======================执行完成,包含{}条流水明细,匹配结算单共耗时{}s",  org.springframework.util.CollectionUtils.isEmpty(bankReconciliations) ? "0" : bankReconciliations.size(), (System.currentTimeMillis() - s0) / 1000.0);

        }catch (Exception e){
            log.error("智能流水执行辨识异常：收付款辨识异常", e);
        }
        return resultMap;
    }

    private void checkBankReconciliationAssociateStatus(List<BankReconciliation> bankReconciliations, Map<String, List<BankReconciliation>> resultMap) {
        if (CollectionUtils.isEmpty(bankReconciliations)) {
            return;
        }
        Iterator<BankReconciliation> iterator = bankReconciliations.iterator();
        while (iterator.hasNext()) {
            BankReconciliation bankReconciliation = iterator.next();
            Short associationStatus = bankReconciliation.getAssociationstatus();
            //已关联，无需后续关联
            if (null != associationStatus && associationStatus == AssociationStatus.Associated.getValue()) {
                this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y11.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
                iterator.remove();
            } else {
                bankReconciliation.setAutoassociation(Boolean.TRUE);
            }
        }
    }

    /**
     * 流水关联主处理逻辑
     *
     * @param bankReconciliations            待处理流水
     * @param bankreconciliationIdentifyType 流水自动辨识规则大类
     * @param resultMap                      流水处理后分类返回
     */
    private void streamMainHandler(List<BankReconciliation> bankReconciliations, BankreconciliationIdentifyType bankreconciliationIdentifyType, Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context) throws Exception {
        //流水自动关联匹配规则
        List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings = new ArrayList<>();
        //流水数据源使用规则
        List<FcDsUseSetting_b> fcDsUseSetting_bList = new ArrayList<>();
        //流水处理规则
        List<Flowhandlesetting> flowhandlesettingList = new ArrayList<>();
        //逻辑池数据源配置
        Map<String, TmspCdpDsRespVO> tmspCdpDsRespVOMap = new HashMap<>();
        /**
         * step1:加载流水自动辨识相关规则
         * */
        try {
            this.loadRule(bankreconciliationIdentifyType, bankreconciliationIdentifySettings, fcDsUseSetting_bList, tmspCdpDsRespVOMap, flowhandlesettingList,context);
        }catch (Exception e){
            if (e instanceof BankDealDetailException) {
                bankReconciliations.stream().forEach(p->{
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(p, context.getLogName(), ((BankDealDetailException) e).getDealDetailBusinessCodeEnum().getDesc(), context);
                });
            }else {
                bankReconciliations.stream().forEach(p->{
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(p, context.getLogName(), e.getMessage(), context);
                });
            }
            return;
        }
        if (!CollectionUtils.isEmpty(flowhandlesettingList)) {
            /**
             * step2:流水匹配关联规则
             * bankreconciliationIdentifyTypeListMap存放的所有的规则队形的流水，key为规则集，value为规则集对应的流水
             * */
            Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankreconciliationIdentifyTypeListMap = this.matchRuleAndReconciliation(bankreconciliationIdentifySettings, bankReconciliations, resultMap, context);
            /**
             * step3:调结算取关联结算单数据
             *       1）拼接取数参数
             *       2）调结算取数接口
             *       3）防重（同批次去重+分布式锁(跨批次)+查库(历史 )）
             *       4）采集所有匹配上结算的流水根据配置建立关联关系
             * */
            if (bankreconciliationIdentifyTypeListMap != null && !CollectionUtils.isEmpty(bankreconciliationIdentifyTypeListMap)) {
                this.callCDPAndHandlerAssociate(fcDsUseSetting_bList, flowhandlesettingList.get(0), bankreconciliationIdentifyTypeListMap, resultMap, context);
            }
        }
    }

    /**
     * 加载相关规则及校验合法性
     * 1）加载收付单据匹配规则及流水与目标单据映射关系
     * 2）加载流水处理规则
     * 3）加载流水数据源设置规则
     * 4）读取逻辑池数据源配置
     */
    private void loadRule(BankreconciliationIdentifyType bankreconciliationIdentifyType,
                          List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings,
                          List<FcDsUseSetting_b> fcDsUseSetting_bList,
                          Map<String, TmspCdpDsRespVO> tmspCdpDsRespVOMap,
                          List<Flowhandlesetting> flowhandlesettingList,BankDealDetailContext context) throws Exception {
        /**
         * step1:加载收付单据匹配规则及流水与目标单据映射关系
         * */
        Short identifyType = bankreconciliationIdentifyType.getIdentifytype();
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(
                QueryCondition.name("identifytype").eq(identifyType),
                QueryCondition.name("applyobject").eq(DealDetailEnumConst.APPLY_OBJECT_BANKRECONCILIATION),
                QueryCondition.name("enablestatus").eq(DealDetailEnumConst.IDENTIFY_ENABLED)
        );
        querySchema.addCondition(queryConditionGroup);
        List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettingList = MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bankreconciliationIdentifySettingList)) {
            /*throw new BankDealDetailException(DealDetailBusinessCodeEnum.SYSTEM005_05Y12);*/
            return;
        }
        for (BankreconciliationIdentifySetting setting : bankreconciliationIdentifySettingList) {
            BankreconciliationIdentifySetting fullSetting = MetaDaoHelper.findById(BankreconciliationIdentifySetting.ENTITY_NAME, setting.getId(), 3);
            String code = fullSetting.getCode();
            //强制将财资统一对账码优先级设为0
            if ("system0511".equals(code) || "system0521".equals(code)) {
                fullSetting.setExcutelevel(0);
            }
            //强制将银行对账编码优先级设为1
            if ("system0512".equals(code) || "system0522".equals(code)) {
                fullSetting.setExcutelevel(1);
            }
            bankreconciliationIdentifySettings.add(fullSetting);
        }
        /**
         * step2:加载流水处理规则，仅加载流水自动关联规则
         *       使用场景：从结算或其他下游取到数做关联逻辑，通过读取该配置判断是否需要手工确认，若取多条数需要匹配哪条
         * */
        List<Flowhandlesetting> flowhandlesettings = bankDealDetailAccessDao.queryFlowhandlesetting(DealDetailEnumConst.FLOWTYPE, DealDetailEnumConst.FLOW_ENABLED, DealDetailEnumConst.FLOW_OBJECT, DealDetailEnumConst.ASSOCIATION_MODE);
        if (CollectionUtils.isEmpty(flowhandlesettings)) {
            throw new BankDealDetailException(DealDetailBusinessCodeEnum.SYSTEM005_05Y13);
        }
        // 按执行优先级排序
        List<Flowhandlesetting> sortedFlowhandlesettings = flowhandlesettings.stream()
                .sorted(Comparator.comparing(Flowhandlesetting::getSortNum))
                .collect(Collectors.toList());
        flowhandlesettingList.add(sortedFlowhandlesettings.get(0));
        /**
         * step3:加载流水数据源使用设置
         *       场景:根据收付单据匹配规则的目标对象加载流水数据源信息，根据name读取的流水数据源子表，并强校验每条数据源是否关联了逻辑池数据源
         * */
        Set<String> cdpSet = new HashSet<>();
        if (!CollectionUtils.isEmpty(bankreconciliationIdentifySettings)) {
            //todo 待页面将目标匹配对象改为参照，拿参照id作为查询数据源子表id
            List<String> matchObjectList = bankreconciliationIdentifySettings.stream().map(identifySetting -> identifySetting.getMatchobject()).collect(Collectors.toList());
            QuerySchema cdpquerySchema = new QuerySchema().addSelect("*");
            QueryConditionGroup cdpqueryConditionGroup = new QueryConditionGroup();
            cdpqueryConditionGroup.addCondition(
                    QueryCondition.name("name").in(matchObjectList),
                    QueryCondition.name("enable").eq(DealDetailEnumConst.FLOW_ENABLED)
            );
            cdpquerySchema.addCondition(cdpqueryConditionGroup);
            List<FcDsUseSetting_b> fcDsUseSetting_bs = MetaDaoHelper.queryObject(FcDsUseSetting_b.ENTITY_NAME, cdpquerySchema, null);
            if (CollectionUtils.isEmpty(fcDsUseSetting_bs)) {
                throw new BankDealDetailException(DealDetailBusinessCodeEnum.SYSTEM005_05Y14);
            }
            fcDsUseSetting_bList.addAll(fcDsUseSetting_bs);
            for (FcDsUseSetting_b fcDsUseSetting_b : fcDsUseSetting_bList) {
                String cdp = fcDsUseSetting_b.getCdp();
                if (StringUtils.isEmpty(cdp)) {
                    throw new BankDealDetailException(DealDetailBusinessCodeEnum.SYSTEM005_05Y15);
                }
                cdpSet.add(cdp);
            }
        }
        /**
         * step4:加载流水数据源关联的逻辑数据源
         * */
        TmspCdpDsReqVO tmspCdpDsVO = new TmspCdpDsReqVO();
        //启动状态
        tmspCdpDsVO.setEnable(DealDetailEnumConst.CDP_ENABLED);
        List<String> cdps = new ArrayList(cdpSet);
        tmspCdpDsVO.setIds(cdps);
        TmspCdpDsRpcService tmspCdpDsRpcService = AppContext.getBean(TmspCdpDsRpcService.class);
        List<TmspCdpDsRespVO> tmspCdpDsRespVOS = tmspCdpDsRpcService.queryByCondition(tmspCdpDsVO);
        if (CollectionUtils.isEmpty(tmspCdpDsRespVOS)) {
            throw new BankDealDetailException(DealDetailBusinessCodeEnum.SYSTEM005_05Y16);
        }
        Map<String, TmspCdpDsRespVO> tmspCdpDsRespVOMapConvert = tmspCdpDsRespVOS.stream().collect(Collectors.toMap(TmspCdpDsRespVO::getId, o -> o, (key1, key2) -> key1));
        Set<String> recdpSet = tmspCdpDsRespVOMapConvert.keySet();
        for (String cdp : cdps) {
            if (!recdpSet.contains(cdp)) {
                throw new BankDealDetailException(DealDetailBusinessCodeEnum.SYSTEM005_05Y17);
            }
        }
        tmspCdpDsRespVOMap.putAll(tmspCdpDsRespVOMapConvert);
    }

    /**
     * 流水匹配关联规则
     *
     * @param bankreconciliationIdentifySettingList 关联规则
     * @param bankReconciliations                   待匹配关联规则的流水集合
     * @param resultMap                             未匹配到规则的流水放到反参中不在执行后续关联逻辑
     * @return Map 关联规则与流水对应关系
     */
    private Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> matchRuleAndReconciliation(List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettingList,
                                                                                                              List<BankReconciliation> bankReconciliations,
                                                                                                              Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context) {
        List<BankReconciliation> bankReconciliation_noRules = new ArrayList<>();
        Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankreconciliationIdentifyTypeListMap = this.matchRuleAndReconciliation(bankreconciliationIdentifySettingList, bankReconciliations, bankReconciliation_noRules, context);
        if (!CollectionUtils.isEmpty(bankReconciliation_noRules)) {
            bankReconciliation_noRules.stream().forEach(b -> {
                b.setAutoassociation(true);
                this.addBankReconciliationToMap(b, DealDetailBusinessCodeEnum.SYSTEM005_05Y21.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
            });
        }
        if (CollectionUtils.isEmpty(bankreconciliationIdentifyTypeListMap)) {
            // 完全找不到规则
            return null;
        }
        return bankreconciliationIdentifyTypeListMap;
    }

    /**
     * 流水和自动辨识规则匹配逻辑
     * 1) 流水关联规则分类
     * 2) 流水匹配关联规则，多对多关系
     */
    private Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> matchRuleAndReconciliation(List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettingList,
                                                                                                              List<BankReconciliation> bankReconciliations,
                                                                                                              List<BankReconciliation> bankReconciliation_noRules, BankDealDetailContext context) {
        /**
         * step1:规则分类及排序，分成财资同意对账码、银行对账编码规则大类 及 要素匹配大类
         * */
        Map<String, Map<String, List<BankreconciliationIdentifySetting>>> ruleClassificationMap = this.ruleClassification(bankreconciliationIdentifySettingList);
        if (CollectionUtils.isEmpty(ruleClassificationMap)) {
            throw new BankDealDetailException(-1, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046D", "收付单据流水关联大类划分结果为空，无法执行后续流水关联逻辑") /* "收付单据流水关联大类划分结果为空，无法执行后续流水关联逻辑" */);
        }
        /**
         * step2:流水匹配关联规则
         * */
        Map<String, List<BankreconciliationIdentifySetting>> smartChecknoIdentifySettingMap = ruleClassificationMap.get(DealDetailEnumConst.PAYMENT_RULE_SMARTCHECKNO);
        Map<String, List<BankreconciliationIdentifySetting>> elementSettingMap = ruleClassificationMap.get(DealDetailEnumConst.PAYMENT_RULE_ELEMENT);

        return this.matchRuleListAndReconciliationList(bankReconciliations, smartChecknoIdentifySettingMap, elementSettingMap, bankReconciliation_noRules, context);
    }

    /**
     * 规则分类,主要是分成两大类规则，财资同意对账码、银行对账编码规则和要素规则
     * 1)筛选出财资统一对账码规则、银行对账编码规则
     * 2)获取要素匹配的关联规则，关联规则配置的会计主体可以是多个，需要按照单个会计主体膨胀要素关联规则
     *
     * @param bankreconciliationIdentifySettingList 全量流水匹配规则
     * @return Map key:大类(财资同意对账码规则、要素规则) value:map(key:rulkey,value:规则集合)
     */
    private Map<String, Map<String, List<BankreconciliationIdentifySetting>>> ruleClassification(List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettingList) {
        Map<String, Map<String, List<BankreconciliationIdentifySetting>>> ruleClassificationMap = new HashMap<>();
        /**
         * step1:处理财资统一对账码、银行对账编码相匹配关联规则
         * */
        List<BankreconciliationIdentifySetting> smartchecknoIdentifySettingList = bankreconciliationIdentifySettingList.stream().filter(setting -> {
            Integer executeLevel = setting.getExcutelevel();
            Short enableStatus = setting.getEnablestatus();
            if ((executeLevel == 0 || executeLevel == 1) && DealDetailEnumConst.IDENTIFY_B_ENABLED.equals(enableStatus)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        //(system0511、system0521):财资统一对账码 (system0512、system0522):银行对账码
        Map<String, List<BankreconciliationIdentifySetting>> smartChecknoIdentifySettingMap = new HashMap<>();
        //先把对账码、银行对账码两个规则移除掉，后面匹配完流水后，再给流水按需加上这两个规则
        if (!CollectionUtils.isEmpty(smartchecknoIdentifySettingList)) {
            bankreconciliationIdentifySettingList.removeAll(smartchecknoIdentifySettingList);
            smartchecknoIdentifySettingList.stream().forEach(smartchenorule -> smartChecknoIdentifySettingMap.put(smartchenorule.getCode(), Arrays.asList(smartchenorule)));
            ruleClassificationMap.put(DealDetailEnumConst.PAYMENT_RULE_SMARTCHECKNO, smartChecknoIdentifySettingMap);
        }
        /**
         * step2:处理要素相匹配关联规则
         *       要素关联规则可以配置多个会计主体，需要按照会计主体拆分，膨胀成多个规则
         * */
        Map<String, List<BankreconciliationIdentifySetting>> elementSettingMap = new HashMap<>();
        for (BankreconciliationIdentifySetting bankreconciliationIdentifySetting : bankreconciliationIdentifySettingList) {
            //如果配置多组织，逗号隔开，按照逗号拆分成多个规则
            String accentitys = bankreconciliationIdentifySetting.getAccentity();
            if (!StringUtils.isEmpty(accentitys)) {
                if (accentitys.contains(",")) {
                    String[] accentityArray = accentitys.split(",");
                    for (String accentity : accentityArray) {
                        String key = getRuleKey(accentity, Direction.find(bankreconciliationIdentifySetting.getDc_flag()), bankreconciliationIdentifySetting.getBanktype());
                        if (elementSettingMap.get(key) == null) {
                            elementSettingMap.put(key, new ArrayList<BankreconciliationIdentifySetting>());
                        }
                        ((List) elementSettingMap.get(key)).add(bankreconciliationIdentifySetting);
                    }
                } else {
                    String key = getRuleKey(accentitys, Direction.find(bankreconciliationIdentifySetting.getDc_flag()), bankreconciliationIdentifySetting.getBanktype());
                    if (elementSettingMap.get(key) == null) {
                        elementSettingMap.put(key, new ArrayList<BankreconciliationIdentifySetting>());
                    }
                    ((List) elementSettingMap.get(key)).add(bankreconciliationIdentifySetting);
                }
            }
        }

        // 排序处理
        for (String key : elementSettingMap.keySet()) {
            List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings = (List) elementSettingMap.get(key);
            // 按优先级排序
            List<BankreconciliationIdentifySetting> settingsSortList = bankreconciliationIdentifySettings.stream().sorted(Comparator.comparing(BankreconciliationIdentifySetting::getExcutelevel)).collect(Collectors.toList());
            elementSettingMap.put(key, settingsSortList);
        }
        ruleClassificationMap.put(DealDetailEnumConst.PAYMENT_RULE_ELEMENT, elementSettingMap);
        return ruleClassificationMap;
    }

    /**
     * 流水匹配关联规则
     * 1)优先匹配财资统一对账码(维度：财资同意对账码+本方银行账号+交易/原币金额)
     * 2)其次匹配银行对账编码(维度：银行对账编码)
     * 3)按照要素匹配(维度:会计主体、银行类别、收付方向)
     * 优先取流水会计主体，如果流水没有会计主体，则按照企业账号级处理
     * smartChecknoIdentifySettingMap 收付单据匹配关联规则分类,财资统一对账码、银行对账编码类别
     * elementSettingMap 收付单据匹配关联规则分类，要素类别
     */
    private Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> matchRuleListAndReconciliationList(List<BankReconciliation> bankReconciliations,
                                                                                                                      Map<String, List<BankreconciliationIdentifySetting>> smartChecknoIdentifySettingMap,
                                                                                                                      Map<String, List<BankreconciliationIdentifySetting>> elementSettingMap,
                                                                                                                      List<BankReconciliation> bankReconciliation_noRules, BankDealDetailContext context) {
        Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankReconciliationBatchMatchIdentifyMap = new HashMap<>();
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            //流水收付方向
            Direction direction = bankReconciliation.getDc_flag();
            List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings = new ArrayList<>();
            //财资统一对账码【智能对账勾兑码】
            String smartCheckno = bankReconciliation.getSmartcheckno();
            String bankaccount = bankReconciliation.getBankaccount();
            BigDecimal tran_amt = bankReconciliation.getTran_amt();
            //银行对账码
            String bankcheckno = bankReconciliation.getBankcheckno();
            if (!CollectionUtils.isEmpty(smartChecknoIdentifySettingMap)) {
                //支出 system0511 system0512
                if (direction == Direction.Debit) {
                    //财资统一对账码规则
                    if ((StringUtils.isNotEmpty(smartCheckno) && StringUtils.isNotEmpty(bankaccount)) && (smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0511)) != null) {
                        bankreconciliationIdentifySettings.add(smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0511).get(0));
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), DealDetailEnumConst.IDENTIFY_SYSTEM_0511, context);
                    }
                    if (!StringUtils.isEmpty(bankcheckno) && smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0512) != null) {
                        bankreconciliationIdentifySettings.add(smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0512).get(0));
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), DealDetailEnumConst.IDENTIFY_SYSTEM_0512, context);
                    }
                    //收入 system0521 system522
                } else if (direction == Direction.Credit) {
                    //财资统一对账码规则
                    if ((StringUtils.isNotEmpty(smartCheckno) && StringUtils.isNotEmpty(bankaccount)) && (smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0521)) != null) {
                        bankreconciliationIdentifySettings.add(smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0521).get(0));
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), DealDetailEnumConst.IDENTIFY_SYSTEM_0521, context);
                    }
                    if (!StringUtils.isEmpty(bankcheckno) && smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0522) != null) {
                        bankreconciliationIdentifySettings.add(smartChecknoIdentifySettingMap.get(DealDetailEnumConst.IDENTIFY_SYSTEM_0522).get(0));
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), DealDetailEnumConst.IDENTIFY_SYSTEM_0522, context);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(elementSettingMap)) {
                List<BankreconciliationIdentifySetting> bankreconciliationIdentifyAccentitySettingList = findSetting(bankReconciliation, elementSettingMap);
                if (!CollectionUtils.isEmpty(bankreconciliationIdentifyAccentitySettingList)) {
                    bankreconciliationIdentifySettings.addAll(bankreconciliationIdentifyAccentitySettingList);
                }
                // 没有匹配到规则的流水后续可执行生单逻辑
                if (CollectionUtils.isEmpty(bankreconciliationIdentifySettings)) {
                    bankReconciliation_noRules.add(bankReconciliation);
                    // 继续下一个流水
                    continue;
                }
            }
            if (CollectionUtils.isEmpty(bankReconciliationBatchMatchIdentifyMap.get(bankreconciliationIdentifySettings))) {
                bankReconciliationBatchMatchIdentifyMap.put(bankreconciliationIdentifySettings, new ArrayList<>());
            }
            bankReconciliationBatchMatchIdentifyMap.get(bankreconciliationIdentifySettings).add(bankReconciliation);
        }
        return bankReconciliationBatchMatchIdentifyMap;
    }

    /**
     * @param fcDsUseSetting_bList                  流水数据源集合
     * @param flowhandlesetting                     流水处理规则
     * @param bankreconciliationIdentifyTypeListMap 流水与关联规则匹配集合,
     * @param resultMap                             流水未匹配上、或者匹配失败放到集合中
     */
    private void callCDPAndHandlerAssociate(List<FcDsUseSetting_b> fcDsUseSetting_bList,
                                            Flowhandlesetting flowhandlesetting,
                                            Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankreconciliationIdentifyTypeListMap,
                                            Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context) {

        Map<String, FcDsUseSetting_b> fcDsUseSetting_bMap = fcDsUseSetting_bList.stream().collect(Collectors.toMap(FcDsUseSetting_b::getName, each -> each, (key1, key2) -> key1));
        /**
         * 【核心逻辑】取数、防重、关联
         * */
        this.callCDPAndHandlerAssociate(flowhandlesetting, bankreconciliationIdentifyTypeListMap, fcDsUseSetting_bMap, resultMap, context);
    }

    /**
     * 1）构建取数参数
     * 2）调取数接口
     * 3）防重
     * 4）执行关联逻辑
     */
    private void callCDPAndHandlerAssociate(Flowhandlesetting flowhandlesetting,
                                            Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankreconciliationIdentifyTypeListMap,
                                            Map<String, FcDsUseSetting_b> fcDsUseSetting_bMap,
                                            Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context) {
        //防重设计，结算单已关联流水，防止结算单重复关联其它流水
        Map<String, String> busidRelationBankReconcilitionIdMap = new HashMap<>();
        //所有待匹配流水对应关联规则，一批流水按照关联规则优先级依次匹配关联规则对应的下游单据
        Set<List<BankreconciliationIdentifySetting>> bankreconciliationIdentifySettingSet = bankreconciliationIdentifyTypeListMap.keySet();
        for (List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettingList : bankreconciliationIdentifySettingSet) {
            List<BankReconciliation> bankreconciliations = bankreconciliationIdentifyTypeListMap.get(bankreconciliationIdentifySettingList);
            if (CollectionUtils.isEmpty(bankreconciliations)) {
                continue;
            }
            //noMatchBankReconciliationList:这批流水需要依次匹配bankreconciliationIdentifySettingList规则，流水匹配成功或失败都从中移除
            List<BankReconciliation> noMatchBankReconciliationList = new ArrayList<>(bankreconciliations);
            Map<Long, BankReconciliation> noMathBankReconciliationMap = noMatchBankReconciliationList.stream().collect(Collectors.toMap(BankReconciliation::getId, o -> o, (key1, key2) -> key1));
            //关联规则逐个匹配流水
            boolean matchSuccess = false;
            for (BankreconciliationIdentifySetting bankreconciliationIdentifySetting : bankreconciliationIdentifySettingList) {
                if (CollectionUtils.isEmpty(noMatchBankReconciliationList)) {
                    break;
                }
                /**
                 * step1:封装取数接口入参
                 * */
                List<BankReconciliation> buildParamSuccBankReconciliationList = new ArrayList<>();
                Map<String, CmpRuleInfo> cmpRuleInfoMap = new HashMap<>();
                List<CDPQueryConditionDTO> cdpQueryConditionDTOS = this.buildQueryParams(buildParamSuccBankReconciliationList, bankreconciliationIdentifySetting, noMatchBankReconciliationList, resultMap, context, cmpRuleInfoMap);
                if (CollectionUtils.isEmpty(cdpQueryConditionDTOS)) {
                    continue;
                }
                try {
                    /**
                     * step2:调用取数接口
                     * */
                    log.error("【智能流水收付单匹配】调结算取数入参{}", CtmJSONObject.toJSONString(cdpQueryConditionDTOS));
                    List<CDPRespDetailDTO> cdpRespDetailDTOList = this.callCDPInterface(bankreconciliationIdentifySetting, fcDsUseSetting_bMap, cdpQueryConditionDTOS);
                    log.error("【智能流水收付单匹配】调结算取数反参{}", CtmJSONObject.toJSONString(cdpRespDetailDTOList));

                    if (CollectionUtils.isEmpty(cdpRespDetailDTOList)) {
                        noMatchBankReconciliationList.stream().filter(Objects::nonNull).forEach(bankReconciliation -> {
                            Short associatedStates = 0;//关联状态
                            bankReconciliation.setAssociationstatus(associatedStates);
                            bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
                            log.error("【智能流水收付单匹配】交易流水号{}未查询到结算单信息", bankReconciliation.getBank_seq_no());
                            CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
                            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleForPayTargets(cmpRuleCheckLog, context.getLogName(), cmpRuleInfoMap.get(bankReconciliation.getBank_seq_no()));
                            CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_ONE.getDesc(), null);
                        });
                        matchSuccess = true;
                        continue;
                    }
                    /**
                     * step3:取数结果分组，一笔流水对多条结算明细
                     * */
                    Map<String, List<CDPRespDetailDTO>> bankReconciliationAndcdpRespDetailDTOListMap = this.groupWithBankReconciliation(cdpRespDetailDTOList, noMathBankReconciliationMap, context);
                    /**
                     * step4:流水逐条处理
                     *
                     * */
                    FcDsUseSetting_b fcDsUseSetting_b = fcDsUseSetting_bMap.get(bankreconciliationIdentifySetting.getMatchobject());
                    List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
                    Map<String, BankReconciliation> bankReconciliationMap = new HashMap<>();
                    if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                        bankReconciliationMap = bankReconciliationList.stream().collect(Collectors.toMap(K1 -> K1.getId().toString(), item -> item, (k1, k2) -> k1));
                    }

                    /**
                     * 财资统一码优先匹配返回的结算单
                     */
                    List<BankReconciliation> bankReconciliationDTOList = new ArrayList<>();
                    Iterator<Map.Entry<String, List<CDPRespDetailDTO>>> iteratorDTO = bankReconciliationAndcdpRespDetailDTOListMap.entrySet().iterator();
                    while (iteratorDTO.hasNext()) {
                        Map.Entry<String, List<CDPRespDetailDTO>> next = iteratorDTO.next();
                        List<CDPRespDetailDTO> cdpRespDetailDTOS = next.getValue();
                        // 对于财资统一码优先匹配返回的结算单，判断是否换汇，是直接保留数据，不是匹配金额是否相等，相等保留，不相等丢弃
                        if (!CollectionUtils.isEmpty(cdpRespDetailDTOS) && (DealDetailEnumConst.IDENTIFY_SYSTEM_0511.equals(bankreconciliationIdentifySetting.getCode()) || (DealDetailEnumConst.IDENTIFY_SYSTEM_0521.equals(bankreconciliationIdentifySetting.getCode())))) {
                            // 返回多条结算单场景
                            if (cdpRespDetailDTOS.size() > 1) {
                                bankReconciliationDTOList.add(bankReconciliationMap.get(next.getKey()));
                                iteratorDTO.remove();
                                continue;
                            }
                            // 结算返回的金额
                            Object settlementAmount = cdpRespDetailDTOS.get(0).getExtra().get("settlementAmount");
                            BigDecimal tranAmt = bankReconciliationMap.get(next.getKey()).getTran_amt();
                            //  结算单的金额和流水的金额不相等，进行判断
                            if (null != settlementAmount && null != tranAmt && tranAmt.compareTo((BigDecimal) settlementAmount) != 0) {
                                // [是否换汇]
                                Object  isExchangePayment =  cdpRespDetailDTOS.get(0).getExtra().get("isExchangePayment");
                                // 不是换汇的数据,扔掉数据
                                if (!(null != isExchangePayment && "1".equals(isExchangePayment.toString()))) {
                                    bankReconciliationDTOList.add(bankReconciliationMap.get(next.getKey()));
                                    iteratorDTO.remove();
                                }
                            }
                        }
                    }
                    //移除已经被清空的银行流水
                    if (!CollectionUtils.isEmpty(bankReconciliationDTOList)){
                        bankReconciliationDTOList.stream().forEach(bankReconciliation->{
                            CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
                            CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleForPayTargets(cmpRuleCheckLog, context.getLogName(), cmpRuleInfoMap.get(bankReconciliation.getBank_seq_no()));
                            CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_TEN.getDesc(), null);
                        });
                    }
                    if (CollectionUtils.isEmpty(bankReconciliationAndcdpRespDetailDTOListMap)){
                        continue;
                    }

                    Iterator<Map.Entry<String, List<CDPRespDetailDTO>>> iterator = bankReconciliationAndcdpRespDetailDTOListMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, List<CDPRespDetailDTO>> next = iterator.next();
                        String id = next.getKey();
                        BankReconciliation bankReconciliation = bankReconciliationMap.get(id);
                        //设置关联标识，不被后续发布和生单处理
//                        if (bankReconciliation != null) {
//                            bankReconciliation.set("break", "1");
//                        }
                        List<CDPRespDetailDTO> cdpRespDetailDTOSByBankReconciliation = next.getValue();
                        List<String> keys = this.getCdpRespHandlerLockKey(fcDsUseSetting_b, cdpRespDetailDTOSByBankReconciliation, busidRelationBankReconcilitionIdMap);
                        Long holdLockTime = Long.parseLong(AppContext.getEnvConfig("cmp.match.lock", "300"));
                        CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, holdLockTime, TimeUnit.SECONDS, (int status) -> {
                            if (status == LockStatus.GETLOCK_FAIL) {
                                //其它流水正在关联这条结算明细，本条流水未关联上结算
                                this.nothingAssociate(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y34.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap, noMatchBankReconciliationList);
                                return;
                            }
                            /**
                             * step5:同批次去重，去除已关联的结算明细
                             * */
                            //this.removeAssocitedRespDetailDTO(cdpRespDetailDTOSByBankReconciliation, fcDsUseSetting_b, busidRelationBankReconcilitionIdMap);
                            if (CollectionUtils.isEmpty(cdpRespDetailDTOSByBankReconciliation)) {
                                this.nothingAssociate(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y35.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap, noMatchBankReconciliationList);
                                return;
                            }
                            /**
                             * step6:数据库去重
                             * */
                            this.removeAssociatedFromDB(cdpRespDetailDTOSByBankReconciliation);
                            if (CollectionUtils.isEmpty(cdpRespDetailDTOSByBankReconciliation)) {
                                this.nothingAssociate(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y36.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap, noMatchBankReconciliationList);
                                return;
                            }
                            /**
                             * step7:执行关联逻辑
                             * */
                            this.processSettleAssociationData(flowhandlesetting, bankReconciliation, cdpRespDetailDTOSByBankReconciliation, noMatchBankReconciliationList, busidRelationBankReconcilitionIdMap, resultMap, fcDsUseSetting_b, context, bankreconciliationIdentifySetting, cmpRuleInfoMap);
                        });
                    }
                } catch (Exception e) {
                    log.error("调取数接口异常", e);
                    //调取数接口异常，这些流水挂起，不能进行后续生单或发布
                    for (BankReconciliation bankReconciliation : buildParamSuccBankReconciliationList) {
                        this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05SO1.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), resultMap);
                    }
                }
                matchSuccess = true;
            }
            //流水对应的这一批规则都执行完，还有未匹配上的流水就执行后续生单或者发布任务
            if (!CollectionUtils.isEmpty(noMatchBankReconciliationList)) {
                final boolean matchSuccessLambda = matchSuccess;
                noMatchBankReconciliationList.stream().forEach(b -> {
                    // 仅关联 的流水设置为结束，TODO
                    if (b.getIsparsesmartcheckno()) {
                        if (matchSuccessLambda) {
                            this.addBankReconciliationToMap(b, DealDetailBusinessCodeEnum.SYSTEM005_05Y33.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
                        } else {
                            this.addBankReconciliationToMap(b, DealDetailBusinessCodeEnum.SYSTEM005_05Y33.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), resultMap);
                        }
                        return;
                    }
                    b.setAutoassociation(true);
                    this.addBankReconciliationToMap(b, DealDetailBusinessCodeEnum.SYSTEM005_05Y33.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), resultMap);
                });

            }
        }
    }

    private void removeAssociatedFromDB(List<CDPRespDetailDTO> cdpRespDetailDTOSByBankReconciliation) {
        //查询数据库是否存在关联明细
        Map<String, CDPRespDetailDTO> billDetailIdMap = cdpRespDetailDTOSByBankReconciliation.stream().collect(Collectors.toMap(CDPRespDetailDTO::getBillDetailId, each -> each, (key1, key2) -> key1));
        List<Long> billDetailIds = new ArrayList<>();
        billDetailIdMap.keySet().stream().forEach(billdetailid -> billDetailIds.add(Long.parseLong(billdetailid)));
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bList = bankDealDetailAccessDao.queryBankReconciliationBusByBillId(billDetailIds);
        //去除DB中已关联的结算明细
        if (!CollectionUtils.isEmpty(bankReconciliationbusrelation_bList)) {
            bankReconciliationbusrelation_bList.stream().forEach(b -> {
                Long billid = b.getBillid();
                CDPRespDetailDTO cdpRespDetailDTO = billDetailIdMap.get(billid + "");
                if (null != cdpRespDetailDTO) {
                    cdpRespDetailDTOSByBankReconciliation.remove(cdpRespDetailDTO);
                }
            });
        }
    }

    private void removeAssocitedRespDetailDTO(List<CDPRespDetailDTO> cdpRespDetailDTOSByBankReconciliation, FcDsUseSetting_b fcDsUseSetting_b, Map<String, String> busidRelationBankReconcilitionIdMap) {
        Iterator<CDPRespDetailDTO> cdpRespDetailDTOIterator = cdpRespDetailDTOSByBankReconciliation.iterator();
        while (cdpRespDetailDTOIterator.hasNext()) {
            CDPRespDetailDTO cdpRespDetailDTO = cdpRespDetailDTOIterator.next();
            //busikey: 单据业务对象编码+单据主键id【关联明细表(cmp_bankreconciliation_bus_relation_b)对应buillid】
            String busiKey = this.getBusiKey(fcDsUseSetting_b, cdpRespDetailDTO.getBillDetailId());
            if (busidRelationBankReconcilitionIdMap.get(busiKey) != null) {
                cdpRespDetailDTOIterator.remove();
            }
        }
    }

    private Map<String, List<CDPRespDetailDTO>> groupWithBankReconciliation(List<CDPRespDetailDTO> cdpRespDetailDTOList, Map<Long, BankReconciliation> noMathBankReconciliationMap, BankDealDetailContext context) {
        //取数后按照流水分组
        Map<String, List<CDPRespDetailDTO>> bankReconciliationAndcdpRespDetailDTOListMap = new HashMap<>();
        //先按照流水分组，一条流水对多个结算明细
        for (CDPRespDetailDTO cdpRespDetailDTO : cdpRespDetailDTOList) {
            //携带原始流水id
            Object bidObj = cdpRespDetailDTO.getExtra().get("bid");
            if (bidObj == null) {
                // 没传递回来，不是需要的数据
                continue;
            }
            Long bankReconciliationId = Long.parseLong(bidObj + "");
            BankReconciliation bankReconciliation = noMathBankReconciliationMap.get(bankReconciliationId);
            //一条流水支持关联多笔结算单
            if (bankReconciliationAndcdpRespDetailDTOListMap.get(bankReconciliation.getId() + "") == null) {
                bankReconciliationAndcdpRespDetailDTOListMap.put(bankReconciliation.getId() + "", new ArrayList<>());
            }
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, context.getLogName(), RuleLogEnum.RuleLogProcess.PAYMENT_BILL_THREE.getDesc(), context);
            bankReconciliationAndcdpRespDetailDTOListMap.get(bankReconciliation.getId() + "").add(cdpRespDetailDTO);
        }
        return bankReconciliationAndcdpRespDetailDTOListMap;
    }

    private List<CDPQueryConditionDTO> buildQueryParams(List<BankReconciliation> buildParamSuccBankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, List<BankReconciliation> noMatchBankReconciliationList, Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context, Map<String, CmpRuleInfo> cmpRuleInfoMap) {
        //组装取数参数
        BankreconciliationIdentifySetting s = new BankreconciliationIdentifySetting();
        s.init(bankreconciliationIdentifySetting);
        List<CDPQueryConditionDTO> cdpQueryConditionDTOS = this.buildQueryParams(s, noMatchBankReconciliationList, buildParamSuccBankReconciliationList, resultMap, context, cmpRuleInfoMap);
        return cdpQueryConditionDTOS;
    }

    /**
     * 构建请求参数
     *
     * @param bankReconciliationList
     */
    private List<CDPQueryConditionDTO> buildQueryParams(BankreconciliationIdentifySetting ruleSetting, List<BankReconciliation> bankReconciliationList, List<BankReconciliation> buildParamSuccBankReconciliationList, Map<String, List<BankReconciliation>> resultMap, BankDealDetailContext context, Map<String, CmpRuleInfo> cmpRuleInfoMap) {
        logger.error(String.format("%s:构建取数请求参数", "buildQueryParams"));
        List<CDPQueryConditionDTO> queryDTOList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            try {
                CDPQueryConditionDTO queryDto = new CDPQueryConditionDTO();
                // ** 领域关键特殊信息
                getUserCaseDTO(queryDto, bankReconciliation, ruleSetting);
                // ** 分页信息
                queryDto.setQueryPage(getPageInfo(1, 100, 100));
                // ** 条件参数值
                // 组织信息
                queryDto.setAccentitys(Arrays.asList(new String[]{bankReconciliation.getAccentity()})); // 对应流水的使用全组织，流水查规则的时候注意
                // 设置扩展信息
                CmpRuleInfo cmpRuleInfo = new CmpRuleInfo();
                cmpRuleInfo.setRuleCode(ruleSetting.getCode());
                HashMap<String, Object> ruleSources = cmpRuleInfo.getSources();
                buildCDPExtQuerySchemaGroupDTONew(queryDto, bankReconciliation, ruleSetting, ruleSources);
                cmpRuleInfoMap.put(bankReconciliation.getBank_seq_no(), cmpRuleInfo);
                queryDTOList.add(queryDto);
                CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
                CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleForPayTargets(cmpRuleCheckLog, context.getLogName(), cmpRuleInfoMap.get(bankReconciliation.getBank_seq_no()));
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, context.getLogName(), RuleLogEnum.RuleLogProcess.PAYMENT_BILL_TWO.getDesc());
                buildParamSuccBankReconciliationList.add(bankReconciliation);
            } catch (Exception e) {
                log.error("【智能流水】收付单据匹配，构建取数请求参数异常", e);
                this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y31.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), resultMap);
            }
        }
        return queryDTOList;
    }

    /**
     * 生成批量锁key
     */
    private List<String> getCdpRespHandlerLockKey(FcDsUseSetting_b fcDsUseSetting_b, List<CDPRespDetailDTO> cdpRespDetailDTOSByBankReconciliation, Map<String, String> busidRelationBankReconcilitionIdMap) {
        List<String> keys = new ArrayList<>();
        for (CDPRespDetailDTO cdpRespDetailDTO : cdpRespDetailDTOSByBankReconciliation) {
            String bankReconciliationId = busidRelationBankReconcilitionIdMap.get(cdpRespDetailDTO.getBillDetailId());
            if (StringUtils.isEmpty(bankReconciliationId)) {
                keys.add(this.getBusiKey(fcDsUseSetting_b, cdpRespDetailDTO.getBillDetailId()));
            }
        }
        return keys;
    }

    /**
     * 调用查询接口
     *
     * @param bankreconciliationIdentifySetting
     * @param queryConditionDTOS                查询条件
     * @return 查询结果集
     * @throws CDPBusinessException
     */
    private List<CDPRespDetailDTO> callCDPInterface(BankreconciliationIdentifySetting bankreconciliationIdentifySetting,
                                                    Map<String, FcDsUseSetting_b> fcDsUseSetting_bMap,
                                                    List<CDPQueryConditionDTO> queryConditionDTOS)
            throws CDPBusinessException, ClassNotFoundException {
        logger.error(String.format("%s:调用取数接口", "callCDPInterface"));
        String matchObject = bankreconciliationIdentifySetting.getMatchobject();
        FcDsUseSetting_b fcDsUseSetting_b = fcDsUseSetting_bMap.get(matchObject);
        String domainCode = fcDsUseSetting_b.getDomain();
        // 根据设置获取Classname
        String className = ICDPQueryService.class.getName();
        return getService(className, domainCode).queryByConditions(queryConditionDTOS.toArray(new CDPQueryConditionDTO[0]));
    }

    /**
     * @param flowhandlesetting
     * @param bankReconciliation
     * @param cdpRespDetailDTOS
     * @param resultMap
     * @param nomatchBankReconciliationList
     * @param busidRelationBankReconcilitionIdMap
     * @param fcDsUseSetting_b
     */
    private void processSettleAssociationData(Flowhandlesetting flowhandlesetting,
                                              BankReconciliation bankReconciliation,
                                              List<CDPRespDetailDTO> cdpRespDetailDTOS,
                                              List<BankReconciliation> nomatchBankReconciliationList,
                                              Map<String, String> busidRelationBankReconcilitionIdMap,
                                              Map<String, List<BankReconciliation>> resultMap,
                                              FcDsUseSetting_b fcDsUseSetting_b, BankDealDetailContext context, BankreconciliationIdentifySetting bankreconciliationIdentifySetting,
                                              Map<String, CmpRuleInfo> cmpRuleInfoMap) {
        Map<BankReconciliation, List<CDPRespDetailDTO>> cdpRespDetailDTOMap = new HashMap<>();
        cdpRespDetailDTOMap.put(bankReconciliation, cdpRespDetailDTOS);
        this.processSettleAssociationData(flowhandlesetting, cdpRespDetailDTOMap, nomatchBankReconciliationList, busidRelationBankReconcilitionIdMap, resultMap, fcDsUseSetting_b, context, bankreconciliationIdentifySetting, cmpRuleInfoMap);
    }

    /**
     * 处理已匹配结算单的流水
     * 1）读流水自动处理规则配置，按照流水是否需要手工确认及自动确认且关联多条流水将流水处理分成两类
     * 2）第一类：手工确认【关联明细relationstatus状态:未确认，需到关联确认节点手动确认】
     * 3）第二类：自动确认[发事件给结算，结算回调现金回写确认状态为已确认、生成关联明细]
     * 4)选择全部需要确认，不管匹配到一条结算数据时还是多条结算明细，都需要手工确认；
     * 5)选择全部无需确认，匹配多条，如果开启'关联多条自动确认'就随机取一条结算明细做自动确认，否则需要人工确认。如果匹配到一条数据，也是自动确认，无需人工介入
     */
    private void processSettleAssociationData(Flowhandlesetting flowhandlesetting,
                                              Map<BankReconciliation, List<CDPRespDetailDTO>> cdpRespDetailDTOMap,
                                              List<BankReconciliation> noMatchBankReconciliationList,
                                              Map<String, String> busidRelationBankReconcilitionIdMap,
                                              Map<String, List<BankReconciliation>> resultMap,
                                              FcDsUseSetting_b fcDsUseSetting_b, BankDealDetailContext context, BankreconciliationIdentifySetting bankreconciliationIdentifySetting,
                                              Map<String, CmpRuleInfo> cmpRuleInfoMap) {


        Long flowId = bankreconciliationIdentifySetting.getFlow_id();
        //自动关联是否人工确认: 1(全部需要确认)2(全部不需要确认)
        Short artiConfirm = null;
        Boolean isAutoConfirm = null;
        if (flowId != null){
            try {
                Flowhandlesetting flowhandlesettingNew = MetaDaoHelper.findById(Flowhandlesetting.ENTITY_NAME, flowId, 3);
                if (flowhandlesettingNew != null && flowhandlesettingNew.getIsArtiConfirm() != null) {
                    artiConfirm = flowhandlesettingNew.getIsArtiConfirm();
                    //关联多条是否自动确认 true:需要确认 false:自动确认
                    isAutoConfirm = BooleanUtils.b(flowhandlesetting.getIsRandomAutoConfirm());
                }
            }catch (Exception e){
                log.warn("获取流程处理设置失败，flowId={}, 使用默认配置", flowId, e);
            }
        }
        if (artiConfirm == null){
            artiConfirm = flowhandlesetting.getIsArtiConfirm();
        }
        if (isAutoConfirm == null){
            isAutoConfirm = BooleanUtils.b(flowhandlesetting.getIsRandomAutoConfirm());
        }

        Map<Long, BankReconciliation> bankReconciliationMap = cdpRespDetailDTOMap.keySet().stream().collect(Collectors.toMap(BankReconciliation::getId, each -> each, (key1, key2) -> key2));
        List<CorrDataEntity> corrDataEntityParamListByManual = new ArrayList<>();
        List<CorrDataEntity> corrDataEntityParamListByAuto = new ArrayList();
        for (BankReconciliation bankReconciliation : cdpRespDetailDTOMap.keySet()) {
            List<CDPRespDetailDTO> cdpRespDetailDTOList = cdpRespDetailDTOMap.get(bankReconciliation);
            Long bankReconciliationId = bankReconciliation.getId();
            if (!CollectionUtils.isEmpty(cdpRespDetailDTOList)) {
                //设置关联标识，不被后续发布和生单处理
                if (bankReconciliation != null) {
                    bankReconciliation.set("break", "1");
                }
                //已经匹配上
                CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
                CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleForPayTargets(cmpRuleCheckLog, context.getLogName(), cmpRuleInfoMap.get(bankReconciliation.getBank_seq_no()));
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_SEX.getDesc() + cdpRespDetailDTOList, null);
            } else {
                CmpRuleCheckLog cmpRuleCheckLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleBusiLog(bankReconciliation, context, context.getLogName());
                CmpRuleModuleLog cmpRuleModuleLog = CmpCheckAndProcessRuleLogProcessor.buildCmpRuleInfoAndReturnRuleForPayTargets(cmpRuleCheckLog, context.getLogName(), cmpRuleInfoMap.get(bankReconciliation.getBank_seq_no()));
                CmpCheckAndProcessRuleLogProcessor.executeRuleStepLog(cmpRuleModuleLog, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_ONE.getDesc(), null);
                continue;
            }
            //【需要确认】或者【不需要确认但是关联多条时未开启自动关联】
            if (artiConfirm == DealDetailEnumConst.ARTICONFIRM || (cdpRespDetailDTOList.size() > 1 && !isAutoConfirm)) {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_NAME.getDesc(), RuleLogEnum.RuleLogProcess.PAYMENT_BILL_FOUR.getDesc() + bankreconciliationIdentifySetting.getCode(), context);
                List<CorrDataEntity> corrDataEntityList = this.buildCorrDataEntityParam(bankReconciliation, cdpRespDetailDTOList, resultMap);
                corrDataEntityParamListByManual.addAll(corrDataEntityList);
                continue;
            }
            //【不需要手工确认】自动关联一条或者关联多条且开启自动确认
            if (cdpRespDetailDTOList.size() == 1 || (isAutoConfirm)) {
                //走自动确认
                try {
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_NAME.getDesc(), RuleLogEnum.RuleLogProcess.PAYMENT_BILL_FIVE.getDesc() + bankreconciliationIdentifySetting.getCode(), context);
                    CDPRespDetailDTO cdpRespDetailDTO = cdpRespDetailDTOList.get(0);
                    List<CorrDataEntity> corrDataEntityList = this.buildCorrDataEntityParam(bankReconciliation, Arrays.asList(cdpRespDetailDTO), resultMap);
                    if (!CollectionUtils.isEmpty(corrDataEntityList)) {
                        CorrDataEntity corrDataEntity = corrDataEntityList.get(0);
                        corrDataEntity.setDcFlag(bankReconciliation.getDc_flag().getValue());
                        corrDataEntityParamListByAuto.add(corrDataEntity);
                    }
                } catch (Exception e) {
                    log.error("{}流水自动确认，构建调结算实体异常", bankReconciliationId, e);
                    continue;
                }
                break;
            }
        }
        //人工确认 【写关联表+回写流水】
        if (!CollectionUtils.isEmpty(corrDataEntityParamListByManual)) {
            int ordernum = 1;
            for (CorrDataEntity corrDataEntity : corrDataEntityParamListByManual) {
                BankReconciliation bankReconciliation = bankReconciliationMap.get(corrDataEntity.getBankReconciliationId());
                if (null != bankReconciliation) {
                    int finalOrdernum = ordernum;
                    IDealDetailCallBack callBack = () -> {
                        logger.error(String.format("【智能流水】收付单据匹配手工确认,异步模式，执行回调{}", CtmJSONObject.toJSONString(corrDataEntity)));
                        corrOperationService.corrOpration(corrDataEntity, finalOrdernum);
                        reWriteBusCorrDataService.reWriteBankReconciliationStatus(corrDataEntity);
                        // 流水关联完结方式
                        addBankReconciliationParams(bankReconciliation);
                    };
                    ordernum++;
                    DealDetailUtils.addCallBackToBankReconciliation(bankReconciliation, callBack);
                    noMatchBankReconciliationList.remove(bankReconciliation);
                    this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y37.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), resultMap);
                    busidRelationBankReconcilitionIdMap.put(this.getBusiKey(fcDsUseSetting_b, corrDataEntity.getBusid() + ""), bankReconciliation.getId() + "");
                } else {
                    log.error("【智能流水】收付单据匹配手工确认,异步步模式，执行回调，流水找不到,id={}", corrDataEntity.getBankReconciliationId());
                }
            }
        }
        //自动确认:提供同步、异步两种模式进行关联确认，默认采用异步模式
        String confirmType = AppContext.getEnvConfig("cmp.relation.confirm.type", "async");
        if (DealDetailEnumConst.RELATION_CONFIRM_SYNC.equals(confirmType)) {
            //同步确认
            this.syncConfirm(corrDataEntityParamListByAuto, bankReconciliationMap, resultMap, noMatchBankReconciliationList, busidRelationBankReconcilitionIdMap, fcDsUseSetting_b);
        } else {
            //异步确认
            this.asyncConfirm(corrDataEntityParamListByAuto, bankReconciliationMap, resultMap, noMatchBankReconciliationList, busidRelationBankReconcilitionIdMap, fcDsUseSetting_b, context);
        }
    }

    /**
     * 同步方案关联确认
     *
     * @param corrDataEntityParamListByAuto 自动确认
     */
    private void syncConfirm(List<CorrDataEntity> corrDataEntityParamListByAuto,
                             Map<Long, BankReconciliation> bankReconciliationMap,
                             Map<String, List<BankReconciliation>> resultMap,
                             List<BankReconciliation> noMatchBankReconciliationList,
                             Map<String, String> busidRelationBankReconcilitionIdMap,
                             FcDsUseSetting_b fcDsUseSetting_b) {

        //自动确认模式
        if (CollectionUtils.isEmpty(corrDataEntityParamListByAuto)) {
            int ordernum = 1;
            for (CorrDataEntity corrDataEntity : corrDataEntityParamListByAuto) {
                BankReconciliation bankReconciliation = bankReconciliationMap.get(corrDataEntity.getBankReconciliationId());
                if (null != bankReconciliation) {
                    int finalOrdernum = ordernum;
                    IDealDetailCallBack callBack = () -> {
                        logger.error(String.format("【智能流水】收付单据匹配自动确认,同步模式，执行回调{}", CtmJSONObject.toJSONString(corrDataEntity)));
                        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = corrOperationService.corrOpration(corrDataEntity, finalOrdernum);
                        reWriteBusCorrDataService.reWriteBankReconciliationStatus(corrDataEntity);
                        corrOperationService.confirmUseException(bankReconciliationbusrelation_b.getId(), Long.parseLong(corrDataEntity.getDcFlag() + ""));
                        addBankReconciliationParams(bankReconciliation);
                    };
                    ordernum++;
                    DealDetailUtils.addCallBackToBankReconciliation(bankReconciliation, callBack);
                    noMatchBankReconciliationList.remove(bankReconciliation);
                    this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y37.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), resultMap);
                    busidRelationBankReconcilitionIdMap.put(this.getBusiKey(fcDsUseSetting_b, corrDataEntity.getBusid() + ""), bankReconciliation.getId() + "");
                } else {
                    log.error("【智能流水】t收付单据匹配自动确认,同步模式，执行回调，流水找不到,id={}", corrDataEntity.getBankReconciliationId());
                }
            }
        }
    }

    /**
     * 异步方案关联确认
     *
     * @param corrDataEntityParamListByAuto 自动确认
     */
    private void asyncConfirm(List<CorrDataEntity> corrDataEntityParamListByAuto, Map<Long, BankReconciliation> bankReconciliationMap,
                              Map<String, List<BankReconciliation>> resultMap,
                              List<BankReconciliation> noMatchBankReconciliationList,
                              Map<String, String> busidRelationBankReconcilitionIdMap,
                              FcDsUseSetting_b fcDsUseSetting_b, BankDealDetailContext context) {

        //自动确认 【写关联表+发事件给结算】
        if (!CollectionUtils.isEmpty(corrDataEntityParamListByAuto)) {
            int ordernum = 1;
            for (CorrDataEntity corrDataEntity : corrDataEntityParamListByAuto) {
                BankReconciliation bankReconciliation = bankReconciliationMap.get(corrDataEntity.getBankReconciliationId());
                if (null != bankReconciliation) {
                    int finalOrdernum = ordernum;
                    IDealDetailCallBack callBack = () -> {
                        logger.error(String.format("【智能流水】收付单据匹配自动确认,异步模式，执行回调{}", CtmJSONObject.toJSONString(corrDataEntity)));
                        corrOperationService.corrOpration(corrDataEntity, finalOrdernum);
                        //关联事件
                        List<SettleDeatailRelBankBillReqVO> settleDetailRelBankBillReqVOs = autoCorrBill(bankReconciliationMap, corrDataEntityParamListByAuto);
                        logger.error(String.format("%s:关联数据-发消息", "sendEventMessage"));
                        BizObject bizObject = new BizObject();
                        bizObject.put("bizObject", settleDetailRelBankBillReqVOs);
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.PAYMENT_BILL_NAME.getDesc(), RuleLogEnum.RuleLogProcess.PAYMENT_BILL_FIVE.getDesc(), context);
                        com.yonyoucloud.fi.cmp.util.SendEventMessageUtils.sendEventMessageEos(bizObject, IEventCenterConstant.CMP_BANKRECONCILIATION,  IEventCenterConstant.EVENT_TYPE_CODE_BANKRECONCILIATION);
                        logger.error(String.format("%s:关联数据-发消息结束", "sendEventMessage"));
                        addBankReconciliationParams(bankReconciliation);
                    };
                    ordernum++;
                    DealDetailUtils.addCallBackToBankReconciliation(bankReconciliation, callBack);
                    noMatchBankReconciliationList.remove(bankReconciliation);
                    this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y37.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), resultMap);
                    busidRelationBankReconcilitionIdMap.put(this.getBusiKey(fcDsUseSetting_b, corrDataEntity.getBusid() + ""), bankReconciliation.getId() + "");
                } else {
                    log.error("【智能流水】t收付单据匹配自动确认,异步步模式，执行回调，流水找不到,id={}", corrDataEntity.getBankReconciliationId());
                }
            }
        }
    }

    /**
     * 结算明细生成key去重,生成已关联结算明细key
     */
    private String getBusiKey(FcDsUseSetting_b fcDsUseSetting_b, String billDetailId) {
        return RuleCodeConst.SYSTEM005 + "_" + fcDsUseSetting_b.getBizObjectCode() + "_" + billDetailId;
    }

    private void addBankReconciliationParams(BankReconciliation bankReconciliation) {
        bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayAssociated.getValue());
    }

    /**
     * 标记流水对应结算明细已被其它流水关联
     */
    private void nothingAssociate(BankReconciliation bankReconciliationRespKey, String businessCode, String key, Map<String, List<BankReconciliation>> resultMap, List<BankReconciliation> noMatchBankReconciliationList) {
        //这笔流水等于是未关联上
        bankReconciliationRespKey.setAutoassociation(true);
        this.addBankReconciliationToMap(bankReconciliationRespKey, businessCode, key, resultMap);
        noMatchBankReconciliationList.remove(bankReconciliationRespKey);
    }


    private List<CorrDataEntity> buildCorrDataEntityParam(BankReconciliation bankReconciliation, List<CDPRespDetailDTO> cdpRespDetailDTOS, Map<String, List<BankReconciliation>> resultMap) {
        if (CollectionUtils.isEmpty(cdpRespDetailDTOS)) {
            return null;
        }
        List<CorrDataEntity> corrDataEntityList = new ArrayList<>();
        try {
            Iterator<CDPRespDetailDTO> cdpRespDetailDTOIterator = cdpRespDetailDTOS.iterator();
            while (cdpRespDetailDTOIterator.hasNext()) {
                CDPRespDetailDTO cdpRespDetailDTO = cdpRespDetailDTOIterator.next();
                CorrDataEntity corrDataEntity = new CorrDataEntity();
                autoCorrSettingService.buildCorrDataEntiry(corrDataEntity,
                        true, String.valueOf(EventType.StwbSettleMentDetails.getValue()), bankReconciliation.getId(),
                        Long.parseLong(cdpRespDetailDTO.getBillDetailId()), cdpRespDetailDTO.getAccentity(), cdpRespDetailDTO.getExtra().get("code") + "",
                        CorrDataProcessingServiceImpl.fundsettlement, cdpRespDetailDTO.getDeptId(), Long.parseLong(cdpRespDetailDTO.getBillId()),
                        cdpRespDetailDTO.getProjectId(), (Date) cdpRespDetailDTO.getExtra().get("vouchdate"), cdpRespDetailDTO.getOriginAmount(), cdpRespDetailDTO.getExtra().get("checkIdentificationCode") != null ? String.valueOf(cdpRespDetailDTO.getExtra().get("checkIdentificationCode")) : "");
                Short associatedStates = 1;//关联状态
                bankReconciliation.setAssociationstatus(associatedStates);
                bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
                corrDataEntity.setGenerate(false);
                corrDataEntityList.add(corrDataEntity);
            }
        } catch (Exception e) {
            log.error("【智能流水调下游单据执行关联逻辑，构建入参异常】", e);
            this.addBankReconciliationToMap(bankReconciliation, DealDetailBusinessCodeEnum.SYSTEM005_05Y41.getCode(), DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), resultMap);
        }
        return corrDataEntityList;
    }

    /**
     * 企业账号级企业组织
     */
    private String getTenantOrg() {
        return com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp.GLOBAL_ACCENTITY;
    }

    /**
     * 获取统一的规则key
     *
     * @param org
     * @param dcflag
     * @param bankType
     * @return
     */
    private static String getRuleKey(String org, Direction dcflag, String bankType) {
        return String.format(KEY_RULE_CODE, org, dcflag.getValue(), bankType);
    }

    /**
     * 流水匹配自动辨识规则算法如下：
     * 1）流水自动辨识规则按照rulekey=accentity_dc_flag_banktype维度分类
     * 2）流水rulekey分成四类，【accentity_dc_flag_banktype】【accentity_dc_flag】【666666_dc_flag_banktype】【666666_dc_flag】
     */
    private List<BankreconciliationIdentifySetting> findSetting(BankReconciliation bankReconciliation, Map<String, List<BankreconciliationIdentifySetting>> ruleMap) {
        // 几种组合情况
        // 规则查找 组织(租户级组织适用没有配置的组织)，收付方向，银行类别（为空标识所有银行类别）
        String org = StringUtils.isEmpty(bankReconciliation.getAccentity()) ? this.getTenantOrg() : bankReconciliation.getAccentity();// 流水的使用组织，去匹配规则的适用组织
        Direction dcflag = bankReconciliation.getDc_flag();  // 借贷方向
        String bankType = bankReconciliation.getBanktype(); // 银行类别
        //1、全字段匹配   指定组织+收付方向+银行类别
        String key = getRuleKey(org, dcflag, bankType);
        if (ruleMap.get(key) != null) {
            // 走非租户级组织规则
            return ruleMap.get(key);
        }
        //2、全字段匹配   指定组织+收付方向
        key = getRuleKey(org, dcflag, null);//String.format(KEY_RULE_CODE, org, dcflag, null);
        if (ruleMap.get(key) != null) {
            // 走非租户级组织规则+收付方向
            return ruleMap.get(key);
        }
        //3、全字段匹配   租户级组织+收付方向 +银行类别
        key = getRuleKey(getTenantOrg(), dcflag, bankType);
        if (ruleMap.get(key) != null) {
            // 走非租户级组织规则+收付方向
            return ruleMap.get(key);
        }
        //   4、全字段匹配   租户级组织+收付方向
        key = getRuleKey(getTenantOrg(), dcflag, null);
        if (ruleMap.get(key) != null) {
            // 走非租户级组织规则+收付方向
            return ruleMap.get(key);
        }
        // 没有匹配到，返回null
        return null;
    }

    /**
     * @param corrDataEntities
     * @return
     * @throws Exception
     * @See com.yonyoucloud.fi.cmp.autocorrsetting.ManualCorrServiceImpl#manualCorrBill
     */
    public List<SettleDeatailRelBankBillReqVO> autoCorrBill(Map<Long, BankReconciliation> bankReconciliations, List<CorrDataEntity> corrDataEntities) throws Exception {
        logger.error(String.format("%s:关联数据-老代码", "autoCorrBill"));
        if (corrDataEntities == null || corrDataEntities.size() < 1) {
            return null;
        }
        List<SettleDeatailRelBankBillReqVO> settleDeatailRelBankBillReqVOList = new ArrayList<>();
        for (CorrDataEntity corrData : corrDataEntities) {
            corrData.setAuto(Boolean.TRUE);
            BankReconciliation bankReconciliation = bankReconciliations.get(corrData.getBankReconciliationId());

            Short associatedStates = 1;//关联状态
            bankReconciliation.setAssociationstatus(associatedStates);
            bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
            bankReconciliation.setAutoassociation(corrData.getAuto());
            //智能对账：生单关联时，添加勾兑码，仅关联则不做任何赋值操作，当为非仅关联时，结算返回数据不为空时则进行赋值
            if (StringUtils.isNotEmpty(corrData.getSmartcheckno()) && !corrData.getSmartcheckno().equals(bankReconciliation.getSmartcheckno())) {
                bankReconciliation.setSmartcheckno(corrData.getSmartcheckno());
            }
            bankReconciliation.setEntrytype(bankReconciliation.getVirtualEntryType());
            //构建结算实体
            settleDeatailRelBankBillReqVOList.add(this.buildSettleDTOs(bankReconciliations, corrData));
        }
        return settleDeatailRelBankBillReqVOList;
    }

    private SettleDeatailRelBankBillReqVO buildSettleDTOs(Map<Long, BankReconciliation> bankReconciliations, CorrDataEntity corrDataEntity) throws ParseException {
        logger.error(String.format("%s:构建结算DTO数据", "buildSettleDTOs"));
        SettleDeatailRelBankBillReqVO settleDeatailRelBankBillReqVO = new SettleDeatailRelBankBillReqVO();
        settleDeatailRelBankBillReqVO.setBankCheck_id(String.valueOf(corrDataEntity.getBankReconciliationId()));
        settleDeatailRelBankBillReqVO.setSettleBenchB_id(String.valueOf(corrDataEntity.getBusid()));
        //智能对账：新增勾兑号传递
        settleDeatailRelBankBillReqVO.setCheck_identification_code(corrDataEntity.getSmartcheckno());
        // 根据id查询到银行对账单
        BankReconciliation bankReconciliation = bankReconciliations.get(corrDataEntity.getBankReconciliationId());//MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,corrDataEntities.get(0).getBankReconciliationId());
        // 1为退票
        short isrefund_status = 1;
        //如果退票状态不为空，并且为退票
        if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
            //传入退票状态和退票金额
            settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(isrefund_status));
            settleDeatailRelBankBillReqVO.setRefundAmt(bankReconciliation.getTran_amt());
        } else { //非退票.必传，不然结算会报错
            settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(0));
            settleDeatailRelBankBillReqVO.setRefundAmt(new BigDecimal(String.valueOf(0)));
        }

        if (null != bankReconciliation.getTran_time()) {
            //退票时间
            settleDeatailRelBankBillReqVO.setRefundDate(bankReconciliation.getTran_time());
            String formattedDateTime = DateUtils.dateFormat(bankReconciliation.getTran_time(), DateUtils.DATE_PATTERN);
            settleDeatailRelBankBillReqVO.setSettlesuccesstime(formattedDateTime);
        } else {
            String formattedDateTime = DateUtils.dateFormat(bankReconciliation.getTran_date(), DateUtils.DATE_PATTERN);
            //CZFW-273088 关联接口传递交易时间
            settleDeatailRelBankBillReqVO.setSettlesuccesstime(formattedDateTime);
        }
        //CZFW-273088 关联接口传递交易日期
        settleDeatailRelBankBillReqVO.setSettlesuccessdate(DateUtils.dateFormat(bankReconciliation.getTran_date(), DateUtils.DATE_PATTERN));
        settleDeatailRelBankBillReqVO.setActualExchangePaymentAmount(bankReconciliation.getTran_amt());
        logger.error("【智能流水】收付单据匹配发事件通知结算执行关联逻辑,{}", CtmJSONObject.toJSONString(settleDeatailRelBankBillReqVO));
        return settleDeatailRelBankBillReqVO;
    }

    /**
     * 关联数据（认领单暂不支持，使用老代码）
     *
     * @param map
     * @return
     * @throws Exception
     * @see CorrDataProcessingService#setAssociatedData
     */
    public List<CorrDataEntity> setAssociatedData(CtmJSONObject map) throws Exception {
        logger.error(String.format("%s:关联数据-老代码", "setAssociatedData"));
        return autoCorrSettingService.setAssociatedData(map);
    }

    /**
     * 获取用户用例信息
     *
     * @param queryDto    查询条件DTO
     * @param ruleSetting
     */
    private void getUserCaseDTO(CDPQueryConditionDTO queryDto, BankReconciliation bankReconciliation, BankreconciliationIdentifySetting ruleSetting) {
        logger.error(String.format("%s:构建取数请求参数", "getUserCaseDTO"));
        CDPUseCaseDTO userCaseDto = new CDPUseCaseDTO();
        userCaseDto.setUseCaseType(CDPUseCaseType.CMPDealDetail);
        userCaseDto.getUseCaseInfo().put(KEY_EXTFIELD_BANKRECONCILIATIONID, bankReconciliation.getId()); // 存放流水ID，用户批量传参
        userCaseDto.getUseCaseInfo().put(KEY_EXTFIELD_BUSIOBJ, "dealdetail");  // 业务对象ID,用于领域区分查那个表
        queryDto.setUserCaseDto(userCaseDto);
    }

    /**
     * 获取查询前n天日期
     *
     * @param date 要查询的结束日期
     * @param n    提前天数
     * @return
     */
    private Date getBeforeDate(Date date, int n) {
        Calendar calendar = Calendar.getInstance(); // 获取当前日期
        calendar.setTime(date);
        // 当前日期前一天
        calendar.add(Calendar.DATE, -1 * n);
        return calendar.getTime();
    }

    /**
     * 获取查询后n天日期
     *
     * @param date 要查询的结束日期
     * @param n    延后天数
     * @return
     */
    private Date getAfterDate(Date date, int n) {
        Calendar calendar = Calendar.getInstance(); // 获取当前日期
        calendar.setTime(date);
        // 当前日期后一天
        calendar.add(Calendar.DATE, 1 * n);
        return calendar.getTime();
    }

    /**
     * 获取分页信息
     *
     * @param currentPage 查询当前页
     * @param pageSize    每页大小
     * @param totalCount  最大总记录数
     * @return
     */
    private CDPQueryPageDTO getPageInfo(int currentPage, int pageSize, int totalCount) {
        CDPQueryPageDTO pageDto = new CDPQueryPageDTO();
        pageDto.setPageSize(pageSize);
        pageDto.setPageIndex(currentPage);
        return pageDto;
    }

    private void buildCDPExtQuerySchemaGroupDTONew(CDPQueryConditionDTO queryDto, BankReconciliation bankReconciliation, BankreconciliationIdentifySetting ruleSetting, Map<String, Object> ruleSources) throws Exception {
        logger.error(String.format("%s:构建取数请求参数", "buildCDPExtQuerySchemaGroupDTO"));
        List<BankreconciliationIdentifySetting_b> bankreconciliationIdentifySetting_bs = ruleSetting.BankreconciliationIdentifySetting_b();
        if (CollectionUtils.isEmpty(bankreconciliationIdentifySetting_bs)) {
            return;
        }
        CDPExtQuerySchemaGroupDTO cdpExtQuerySchemaGroupDTO = new CDPExtQuerySchemaGroupDTO();
        // 流水里面只需要一个分组
        List<CDPQuerySchemaGroupDTO> cdpQuerySchemaGroupDTOS = new ArrayList<>();
        cdpExtQuerySchemaGroupDTO.setQuerySchemaGroupList(cdpQuerySchemaGroupDTOS);
        List<CDPQuerySchemaDTO> cdpQuerySchemaDTOS = new ArrayList<>();
        CDPQuerySchemaGroupDTO cdpQuerySchemaGroupDTO = new CDPQuerySchemaGroupDTO();
        cdpQuerySchemaGroupDTO.setOperator(CDPConditionOperatorEnum.and);
        cdpQuerySchemaGroupDTOS.add(cdpQuerySchemaGroupDTO);
        cdpQuerySchemaGroupDTO.setQuerySchemas(cdpQuerySchemaDTOS);
        // 结算收付方向和现金收付方向相反
        Short receipttype = ruleSetting.getDc_flag();
        if (receipttype == Direction.Debit.getValue()) {
            receipttype = Direction.Credit.getValue();
        } else if (receipttype == Direction.Credit.getValue()) {
            receipttype = Direction.Debit.getValue();
        }
        cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO("receipttype", CDPConditionOperatorEnum.eq, receipttype)); //收付方向  -- TODO 需要确定值是否匹配 1/2
        for (BankreconciliationIdentifySetting_b bankreconciliationIdentifySetting_b : bankreconciliationIdentifySetting_bs) {
            Short enableStatus = bankreconciliationIdentifySetting_b.getEnablestatus();
            if (!DealDetailEnumConst.IDENTIFY_B_ENABLED.equals(enableStatus)) {
                continue;
            }
            //匹配方式
            String applyFieldOption = bankreconciliationIdentifySetting_b.getApplyfieldoption();
            Short matchoption = bankreconciliationIdentifySetting_b.getMatchoption();
            Integer floatDay = bankreconciliationIdentifySetting_b.getFloatdays();
            String consts = bankreconciliationIdentifySetting_b.getConstant();
            String matchField = bankreconciliationIdentifySetting_b.getMatchfield();//
            String applyfield = bankreconciliationIdentifySetting_b.getApplyfield();//
            String applyfieldtype = bankreconciliationIdentifySetting_b.getApplyfieldtype();
            Object bankReconciliationValue = bankReconciliation.get(applyfield);
            //匹配的请求对应的值；
            //在结算下游写死这两个字段，直连非直连statementdetailstatus值不同，没法走配置
            if ("settleBench_b.statementdetailstatus".equals(matchField) || "statementstatus".equals(matchField)) {
                continue;
            }
            ruleSources.put(matchField, bankReconciliationValue == null ? "" : bankReconciliationValue);
            //浮动天数
            if (null != floatDay && floatDay > 0) {
                if (ObjectUtils.isEmpty(bankReconciliationValue)) {
                    continue;
                }
                //日期浮动天数
                if (bankReconciliationValue instanceof Date) {
                    ruleSources.put(matchField, DateUtils.dateFormat((Date) bankReconciliationValue, DateUtils.DATE_PATTERN));
                    String startDate = DateUtils.dateTimeToDateString(getBeforeDate((Date) bankReconciliationValue, floatDay));
                    String endDate = DateUtils.dateTimeToDateString(getAfterDate((Date) bankReconciliationValue, floatDay));
                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.between, startDate));
                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.and, endDate));
                    continue;
                }

            }
            //常量
            if (!StringUtils.isEmpty(consts)) {
                cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.in, consts));
                continue;
            }
            if (matchoption == 2){
                cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.like, bankReconciliationValue));
            }else {
                //不需要判空
//                if ("like".equalsIgnoreCase(applyFieldOption)) {
//                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.like, bankReconciliationValue));
//                } else if ("leftlike".equalsIgnoreCase(applyFieldOption)) {
//                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.leftlike, bankReconciliationValue));
//                } else if ("rightlike".equalsIgnoreCase(applyFieldOption)) {
//                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.rightlike, bankReconciliationValue));
//                } else
                if ("in".equalsIgnoreCase(applyFieldOption)) {
                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.in, bankReconciliationValue));
                } else {
                    cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO(matchField, CDPConditionOperatorEnum.eq, bankReconciliationValue));
                }
            }
        }
        queryDto.setExtQueryCondition(Arrays.asList(new CDPExtQuerySchemaGroupDTO[]{cdpExtQuerySchemaGroupDTO}));
    }

    /**
     * 获取取数接口(现成缓存)
     *
     * @param className  取数接口类名
     * @param domainCode
     * @return
     */
    private ICDPQueryService getService(String className, String domainCode) throws ClassNotFoundException {
        logger.error(String.format("%s:调用取数接口", "getService"));
        Class icdpQueryServiceClass = Class.forName(className);
        ICDPQueryService cdpservice = (ICDPQueryService) DubboReferenceUtils.getDubboService(icdpQueryServiceClass, domainCode, null, TIMEOUT_MS);
        return cdpservice;
    }
}
