//package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;
//
//import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
//import com.yonyou.ucf.mdd.ext.core.AppContext;
//import com.yonyou.ucf.mdd.ext.dubbo.DubboReferenceUtils;
//import com.yonyou.yonbip.ctm.cdp.dto.*;
//import com.yonyou.yonbip.ctm.cdp.enums.CDPConditionOperatorEnum;
//import com.yonyou.yonbip.ctm.cdp.enums.CDPUseCaseType;
//import com.yonyou.yonbip.ctm.cdp.exception.CDPBusinessException;
//import com.yonyou.yonbip.ctm.cdp.itf.ICDPQueryService;
//import com.yonyou.yonbip.ctm.ctmpub.tmsp.TmspCdpDsRpcService;
//import com.yonyou.yonbip.ctm.ctmpub.tmsp.vo.requst.TmspCdpDsReqVO;
//import com.yonyou.yonbip.ctm.ctmpub.tmsp.vo.response.TmspCdpDsRespVO;
//import com.yonyou.yonbip.ctm.json.CtmJSONObject;
//import com.yonyoucloud.ctm.stwb.reqvo.SettleDeatailRelBankBillReqVO;
//import com.yonyoucloud.fi.cmp.autocorrsetting.*;
//import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
//import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
//import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
//import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
//import com.yonyoucloud.fi.cmp.cmpentity.*;
//import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
//import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional.IDealDetailCallBack;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailThreadLocalUtils;
//import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
//import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
//import com.yonyoucloud.fi.cmp.util.DateUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.imeta.core.lang.BooleanUtils;
//import org.imeta.orm.base.BizObject;
//import org.imeta.orm.base.EntityStatus;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.util.CollectionUtils;
//
//import javax.annotation.Resource;
//import java.math.BigDecimal;
//import java.text.ParseException;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
///**
// * @Author guoyangy
// * @Date 2024/6/18 15:33
// * @Description 收付单据规则
// * @Version 1.0
// */
//@Service(RuleCodeConst.SYSTEM005)
//@Slf4j
//public class PaymentBillMatchHandler extends DefaultStreamBatchHandler {
//    private static final Logger logger = LoggerFactory.getLogger(PaymentBillMatchHandler.class);
//    /**
//     * 暂时使用老解口
//     **/
//    @Autowired
//    CorrDataProcessingService autoCorrSettingService;
//    @Autowired
//    ManualCorrServiceImpl manualCorrService;
//    @Autowired
//    CorrOperationService corrOperationService;//写入关联关系
//    @Autowired
//    ReWriteBusCorrDataService reWriteBusCorrDataService;
//    @Autowired
//    YmsOidGenerator ymsOidGenerator;
//    @Autowired
//    BankIdentifyService bankIdentifyService;
//    @Resource
//    private CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;
//    private static final int TIMEOUT_MS = 300000;
//    private static final String KEY_DEBIT = "debit";
//    private static final String KEY_CREBIT = "credit";
//    private static final String KEY_USERULE = "userule";
//    private static final String KEY_USESETTING = "usesetting";
//    private static final String KEY_CDPSERVICE = "cdpservice";
//    private static final String KEY_DEALDEATIL_PROCESS_RULE = "processrule";
//    private static final String KEY_TMSPDS = "tmspds";
//    private static final String KEY_EXTFIELD_BUSIOBJ = "busiobject";
//    private static final String KEY_EXTFIELD_BANKRECONCILIATIONID = "bankreconciliationid";
//    private static final String KEY_EXTFIELD_REMARK = "remark";
//    private static final String CONST_ENABLED = "1";
//    private static final String KEY_RULE_CODE = "rule_%s_%s_%s";
//    private static final String EVENT_SOURCEID_BANKRECONCILIATION = "cmp_bankreconciliation";
//    private static final String EVENT_TYPE_CODE_BANKRECONCILIATION = "cmp_bankreconciliation_corrbill";
//    private static final String KEY_MARCHEDBILLINFO = "marchedBillInfo";
//    /**
//     * 数据源接口服务
//     */
//    private TmspCdpDsRpcService tmspCdpDsRpcService = AppContext.getBean(TmspCdpDsRpcService.class);
//    // 临时
//    @Override
//    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
//        /**
//         * 填充测试数据
//         */
//
//        // 缓存初始化(线程级),避免多次查
////        initRule();
//        // 执行匹配规则
//        Map<String, List<BankReconciliation>> resultMap = processRule(context);
//        /**
//         * 1、读取匹配规则，接口内容--
//         * 2、组装参数
//         * 3、取数
//         * 4、匹配结果分组处理ExecuteStatus
//         * 5、数据存储
//         * 6、发事件
//         * 7、返回
//         */
//        return resultMap;
//    }
//
//    private Map<String, List<BankReconciliation>> processRule(BankDealDetailContext context) {
//        logger.error(String.format("%s: 开始处理规则","processRule"));
//        // 规则处理结果数据
//        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
//
//        // 先设置空的结果
//        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus(), new ArrayList<>()); // 匹配上自动阻止
//        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), new ArrayList<>());  // 人工  TODO
//        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), new ArrayList<>()); // 匹配不上下一个流程
//        try {
//            // 设置流水Map，用于后面ID 获取原始流水
//            Map<Long,BankReconciliation> bankReconciliationMap = new HashMap<>();
//            for(BankDealDetailWrapper bankReconciliationWrapper : context.getWrappers()){
//                bankReconciliationMap.put(bankReconciliationWrapper.getBankReconciliationId(),bankReconciliationWrapper.getBankReconciliation());
//            }
//            logger.error(String.format("流水数量：%s ",bankReconciliationMap.size()));
//            if(bankReconciliationMap.size()==0){
//                return resultMap;
//            }
//
//            Map<String, Object> ruleMap = getCache().get(KEY_USERULE);
//            // 如果没有规则信息，直接抛出执行状态为3的数据，往下走其它规则
//            if (ruleMap == null || ruleMap.size() == 0) {
//                logger.error(String.format("没有找到规则数据：%s ","0"));
//                // 没有规则数据，直接返回数据
//                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(bankReconciliationMap.values().stream().collect(Collectors.toList()));
//                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM005_05YO1.getCode(),getBankReconciliationList(context));
//                return resultMap;
//            }
//            logger.error(String.format("规则数据数量：%s ",ruleMap.size()));
//
//            // 匹配不到规则的流水数据
//            List<BankReconciliation> bankReconciliationList_noRuleList = new ArrayList<>();
//            // 匹配到规则的流水数据
//            Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankReconciliationRule = new HashMap<>();
//            // 规则匹配
//            this.matchRule(context, ruleMap, bankReconciliationList_noRuleList, bankReconciliationRule);
//            // 没有匹配到规则的直接就返回状态3的
//            resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(bankReconciliationList_noRuleList);
//            //无规则匹配
//            if (bankReconciliationRule.size() == 0) {
//                //  完全找不到规则
//                logger.error(String.format("无规则匹配：%s ",String.valueOf(bankReconciliationList_noRuleList.size())));
//                return resultMap;
//            }
//            // 执行规则处理
//            /**
//             * TODO 批量处理，目前只有一个规则，批量调结算信息
//             * TODO  规则中定义了消息是否发送
//
//             * TODO 自动/手动的处理逻辑
//             * 未来需要按来源接口分组批量调用
//             * 按优先级遍历规则，命中规则的后续排除
//             * 1、接口；2、规则(优先级）+流水
//             */
//
//
//            // 已匹配流水结果集
//            Set<Long> marchedBankReconciliationSet = new HashSet<>();
//            // 已匹配的流水信息，用于业务信息排重(线程级信息排重)
//
//            Map<String, Object> marchedBillInfo =this.getCache().get(KEY_MARCHEDBILLINFO);
//            if(marchedBillInfo==null) {
//                marchedBillInfo = new HashMap<>();
//                this.getCache().put(KEY_MARCHEDBILLINFO,marchedBillInfo);
//            }
//
//            logger.error(String.format("开始调用接口：%s ",bankReconciliationRule.size()));
//            Set<Long> bankReconciliationIdSet = new HashSet<>(); // 有规则的ID集合，用于比较
//            // 剩余调用接口
//            for (List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings : bankReconciliationRule.keySet()) {
//                List<BankReconciliation> bankReconciliations = bankReconciliationRule.get(bankreconciliationIdentifySettings);  // 当前批次规则对应的银行流水
//                for(BankReconciliation bankReconciliation:bankReconciliations){
//                    bankReconciliationIdSet.add(bankReconciliation.getId());
//                }
//
//                for (BankreconciliationIdentifySetting bankreconciliationIdentifySetting : bankreconciliationIdentifySettings) {
//                    // 比较排除已经关联的数据
//                    List<BankReconciliation> unMarchecList = new ArrayList<>();
//                    for(BankReconciliation bankReconciliation:bankReconciliations){
//                        if(!marchedBankReconciliationSet.contains(bankReconciliation.getId())){
//                            unMarchecList.add(bankReconciliation);
//                        }
//                    }
//
//                    if(unMarchecList.size()==0){
//                        logger.error("如果没有待匹配的数据，都关联完了 ");
//                        // 如果没有待匹配的数据，说明都关联完了，退出
//                        break;
//                    }
//                    // 按优先级处理执行规则
//                    List<CDPQueryConditionDTO> cdpQueryConditionDTOS = buildQueryParams(bankreconciliationIdentifySetting, unMarchecList);
//                    // 取数处理
//                    List<CDPRespDetailDTO> cdpRespDetailDTOS = callCDPInterface(bankreconciliationIdentifySetting, cdpQueryConditionDTOS);
//                    // 如果没有查到数据
//                    if(cdpRespDetailDTOS==null || cdpRespDetailDTOS.size()==0){
//                        continue;
//                    }
//                    // 解析结果并发事件
//                    marchedBankReconciliationSet.addAll(processRespResult(cdpRespDetailDTOS,marchedBillInfo,bankReconciliationMap,unMarchecList));
//                }
//            }
//            // 有规则且取到数的终止 TODO 后续规则如果需要人工介入的需要调整逻辑
//            for(Long id : marchedBankReconciliationSet){
//                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_END.getStatus()).add(bankReconciliationMap.get(id));
//            }
//            // 有规则，但是没有匹配到数据的继续往下走
//            bankReconciliationIdSet.removeAll(marchedBankReconciliationSet);
//            for(Long id : bankReconciliationIdSet){
//                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).add(bankReconciliationMap.get(id));
//                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM005_05YO4.getCode(),bankReconciliationMap.get(id));
//            }
//            logger.error("匹配关联正常结束");
//        } catch (Exception e) {
//            logger.error("********** 匹配关联非正常结束********* ");
//            logger.error(e.getMessage(),e);
//            // 异常，都是4返回  TODO 差异化处理，try放for里面
//            resultMap.clear();  // 清空其它设置
//            List<BankReconciliation> bankReconciliationList = new ArrayList<>();
//            // 没有规则数据，直接返回数据
//            for (BankDealDetailWrapper bankReconciliation : context.getWrappers()) {
//                bankReconciliationList.add(bankReconciliation.getBankReconciliation());
//            }
//            resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SYSTEM_ERROR.getStatus(), bankReconciliationList);
//            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM005_05SO1.getCode(),bankReconciliationList);
//        }
//        return resultMap;
//    }
//
//    private Map<String, Map<String, Object>> getCache() {
//        String key = "rule_"+RuleCodeConst.SYSTEM005;
//        Map<String, Map<String, Object>> cacheMap =DealDetailThreadLocalUtils.get(key);
//        synchronized (this){
//            if(cacheMap==null){
//                cacheMap = new HashMap<>();
//                DealDetailThreadLocalUtils.put(key,cacheMap);
//                initRule(cacheMap);
//            }
//        }
//       return cacheMap;
//    }
//
//    /**
//     * 返回值处理
//     * @param cdpRespDetailDTOS
//     * @param marchedBillInfo
//     * @param bankReconciliationMap
//     * @return
//     * @throws Exception
//     */
//    private Set<Long> processRespResult(List<CDPRespDetailDTO> cdpRespDetailDTOS, Map<String, Object> marchedBillInfo, Map<Long, BankReconciliation> bankReconciliationMap,List<BankReconciliation> marchingList) throws Exception {
//        logger.error(String.format("%s: 开始处理取数返回值","processRespResult"));
//        Map<Long, Map<String, CDPRespDetailDTO>> respMap = new HashMap<>();
//        Map<Long,BankReconciliation> marchingMap = marchingList.stream().collect(Collectors.toMap(BankReconciliation::getId, Function.identity(),(key1,key2)->key1));
//        logger.error(String.format("cdpRespDetailDTOS=%s ",cdpRespDetailDTOS.size()));
//        for (CDPRespDetailDTO cdpRespDetailDTO : cdpRespDetailDTOS) {
//            // 携带原始的流水ID
//            Long bankreconciliationId =0L;
//            if(cdpRespDetailDTO.getExtra().get("bid")==null){
//                // 没传递回来，不是需要的数据
//                continue;
//            }
//            bankreconciliationId=  Long.valueOf( cdpRespDetailDTO.getExtra().get("bid").toString());
//            if (respMap.get(bankreconciliationId) == null) {
//                respMap.put(bankreconciliationId, new HashMap<>());
//            }else{
//                // 如果已经处理过的流水，则跳过，不支持一个流水关联多个明细，后续如有1:n 再考虑，事件发送数据也要考虑发多笔场景。
//                continue;
//            }
//            String busiKey = cdpRespDetailDTO.getBillId() + cdpRespDetailDTO.getBillDetailId();
//            if(marchedBillInfo.containsKey(busiKey)){
//                // 已经被前面的关联了
//                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM005_05YO5.getCode(),bankReconciliationMap.get(bankreconciliationId));;
//                continue;
//            }
//            respMap.get(bankreconciliationId).put(busiKey,cdpRespDetailDTO);
//            marchingMap.remove(bankreconciliationId);
//            // 匹配到规则的流水数据
//            // 业务单据表头中间件
//            // 业务单据表体主键
//            // 业务单据类型
//            // 关联业务
//
//            // 放入Map 用于重复排斥
//            marchedBillInfo.put(busiKey,bankreconciliationId);
//        }
//        processSettleAssociationData(bankReconciliationMap,respMap);
//        DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM005_05YO5.getCode(),marchingMap.values());
//        logger.error(String.format("%s: 开始处理取数返回值:%s","processRespResult " , respMap.keySet()));
//        return respMap.keySet();
//    }
//
//    /**
//     * 根据流水匹配规则
//     *
//     * @param context
//     * @param ruleMap
//     * @param bankReconciliation_noRules
//     * @param bankReconciliationRule
//     */
//    private void matchRule(BankDealDetailContext context, Map<String, Object> ruleMap, List<BankReconciliation> bankReconciliation_noRules, Map<List<BankreconciliationIdentifySetting>, List<BankReconciliation>> bankReconciliationRule) {
//        StringBuilder sbid = new StringBuilder();
//        logger.error(String.format("%s ： 开始匹配规则","matchRule"));
//        // 匹配规则
//        for (BankDealDetailWrapper bankDealDetailWrapper : context.getWrappers()) {
//            // 规则查找 组织(租户级组织适用没有配置的组织)，收付方向，银行类别（为空标识所有银行类别）
//            List<BankreconciliationIdentifySetting> rulesetting = findSetting(bankDealDetailWrapper.getBankReconciliation(), ruleMap);// 获取对应规则
//            // 没有匹配到规则，状态置为3
//            if (rulesetting == null) {
//                bankReconciliation_noRules.add(bankDealDetailWrapper.getBankReconciliation());
//                sbid.append(Optional.ofNullable(bankDealDetailWrapper.getBankReconciliation().getId())).append(",");
//                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM005_05YO2.getCode(),bankReconciliation_noRules);
//                // 继续下一个流水
//                continue;
//            }
//            // 将流水的匹配规则存入Map
//            if (bankReconciliationRule.get(rulesetting) == null) {
//                bankReconciliationRule.put(rulesetting, new ArrayList<>());
//            }
//            bankReconciliationRule.get(rulesetting).add(bankDealDetailWrapper.getBankReconciliation());
//        }
//        logger.error(String.format("未匹配到规则的流水：%s",sbid.toString()));
//    }
//
//    /**
//     * 根据流水信息查找对应的规则
//     *
//     * @param bankReconciliation
//     * @param ruleMap
//     * @return
//     */
//    private List<BankreconciliationIdentifySetting> findSetting(BankReconciliation bankReconciliation, Map<String, Object> ruleMap) {
//
//        logger.error(String.format("%s: 开始查找规则","findSetting"));
//        // 几种组合情况
//        // 规则查找 组织(租户级组织适用没有配置的组织)，收付方向，银行类别（为空标识所有银行类别）
//        String org = bankReconciliation.getAccentity();// 流水的使用组织，去匹配规则的适用组织
//        Direction dcflag = bankReconciliation.getDc_flag();  // 借贷方向
//        String bankType = bankReconciliation.getBanktype(); // 银行类别
//        //       1、全字段匹配   指定组织+收付方向+银行类别
//        String key = getRuleKey(org, dcflag, bankType);
//
//        if (ruleMap.get(key) != null) {
//            // 走非租户级组织规则
//            return (List<BankreconciliationIdentifySetting>) ruleMap.get(key);
//        }
//
//        //   2、全字段匹配   指定组织+收付方向
//        key = getRuleKey(org, dcflag,null);//String.format(KEY_RULE_CODE, org, dcflag, null);
//        if (ruleMap.get(key) != null) {
//            // 走非租户级组织规则+收付方向
//            return (List<BankreconciliationIdentifySetting>) ruleMap.get(key);
//        }
//
//        //   3、全字段匹配   租户级组织+收付方向 +银行类别
//        key = getRuleKey(getTenantOrg(),dcflag, bankType);
//        String.format(KEY_RULE_CODE, getTenantOrg(), dcflag, bankType);
//        if (ruleMap.get(key) != null) {
//            // 走非租户级组织规则+收付方向
//            return (List<BankreconciliationIdentifySetting>) ruleMap.get(key);
//        }
//
//        //   4、全字段匹配   租户级组织+收付方向
//        key =  getRuleKey(getTenantOrg(),dcflag, null);//String.format(KEY_RULE_CODE, getTenantOrg(), dcflag, null);
//        if (ruleMap.get(key) != null) {
//            // 走非租户级组织规则+收付方向
//            return (List<BankreconciliationIdentifySetting>) ruleMap.get(key);
//        }
//        // 没有匹配到，返回null
//        return null;
//    }
//
//    /**
//     * 获取统一的规则key
//     * @param org
//     * @param dcflag
//     * @param bankType
//     * @return
//     */
//    private static String getRuleKey(String org, Direction dcflag, String bankType) {
//        return String.format(KEY_RULE_CODE, org, dcflag.getValue(), bankType);
//    }
//
//    /**
//     * 获取当前用户的租户级企业组织
//     *
//     * @param
//     * @return
//     */
//    private String getTenantOrg() {
//
////        if (tenantOrg == null) {
////            tenantOrg = String.valueOf(EnterpriseCenter.get(InvocationInfoProxy.getUserid()));
////        }
//        // 租户级企业值是默认的
//        return com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp.GLOBAL_ACCENTITY;
////        return tenantOrg;
//    }
//
//    /**
//     * 获取流水处理规则,
//     * 自动关联时是否人工确认 1 全部需要确认，2 全部无需确认 处理方式=自动时有值
//     * */
//    private Flowhandlesetting getFlowhandlesetting(){
//        Map<String,Object> map = getCache().get(KEY_DEALDEATIL_PROCESS_RULE);
//        if(null!=map && map.size()>0){
//            List<Flowhandlesetting> flowhandlesettings = (List<Flowhandlesetting>) map.get(DealDetailEnumConst.PAYCODE);
//            if(!CollectionUtils.isEmpty(flowhandlesettings)){
//                return flowhandlesettings.get(0);
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 初始化所有规则信息
//     */
//    private void initRule( Map<String, Map<String, Object>> cacheMap) {
//            logger.error("初始化：辨识规则数据获取");
////            ruleCache.set(cacheMap);
//
//            // 辨识规则设置【流水自动辨识匹配规则-收付单据匹配细则】
//            cacheMap.put(KEY_USERULE, new HashMap<String, Object>());
//            // 使用场景设置【流水处理使用数据源设置-收付单据匹配关联】
//            cacheMap.put(KEY_USESETTING, new HashMap<String, Object>());
//            // 接口设置 【流水处理使用数据源设置-收付单据匹配关联-详情】
//            cacheMap.put(KEY_CDPSERVICE, new HashMap<String, Object>());
//            //流水处理规则
//            cacheMap.put(KEY_DEALDEATIL_PROCESS_RULE, new HashMap<String, Object>());
//            //加载流水处理规则
//            try{
//                Map<String, Object> processRuleMap = cacheMap.get(KEY_DEALDEATIL_PROCESS_RULE);
//                //产品定死编码
//                Flowhandlesetting flowhandlesetting = DealDetailUtils.queryFlowhandlesetting(DealDetailEnumConst.PAYCODE);
//                if (flowhandlesetting.get(DealDetailEnumConst.PAYCODE) == null) {
//                    processRuleMap.put(DealDetailEnumConst.PAYCODE, new ArrayList<Flowhandlesetting>());
//                }
//                ((List) processRuleMap.get(DealDetailEnumConst.PAYCODE)).add(flowhandlesetting);
//            }catch (Exception e){
//                log.error("露水处理规则加载失败",e);
//            }
//            try{
//                // 存放RULE Map
//                Map<String, Object> ruleSettingMap = cacheMap.get(KEY_USERULE);
//                for (BankreconciliationIdentifySetting bankreconciliationIdentifySetting : queryAllRule()) {
//                    String key = getRuleKey( bankreconciliationIdentifySetting.getAccentity(), Direction.find(bankreconciliationIdentifySetting.getDc_flag()), bankreconciliationIdentifySetting.getBanktype());// String.format(KEY_RULE_CODE, bankreconciliationIdentifySetting.getAccentity(), bankreconciliationIdentifySetting.getDc_flag(), bankreconciliationIdentifySetting.getBanktype());
//                    if (ruleSettingMap.get(key) == null) {
//                        ruleSettingMap.put(key, new ArrayList<BankreconciliationIdentifySetting>());
//                    }
//                    ((List) ruleSettingMap.get(key)).add(bankreconciliationIdentifySetting);
//                }
//                // 排序处理
//                for (String key : ruleSettingMap.keySet()) {
//                    List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings = (List) ruleSettingMap.get(key);
//                    // 按优先级排序
//                    List<BankreconciliationIdentifySetting> settingsSortList = bankreconciliationIdentifySettings.stream().sorted(Comparator.comparing(BankreconciliationIdentifySetting::getExcutelevel)).collect(Collectors.toList());
//                    ruleSettingMap.put(key, settingsSortList);
//
//                }
//            }catch (Exception e){
//                log.error("加载收付单据匹配细则异常",e);
//            }
//            // 资金数据池使用设置
//            for (Object object : getUseSettings()) {
//                // TODO 待开发完后修改这个
////                cacheMap.get(KEY_USESETTING).put("业务对象id", object);
//            }
//            getTmspCdpDsDTO();
//    }
//
//
//    /**
//     * 获取所有启用的接口信息，
//     *
//     * @return
//     */
//    private Map<String, Object> getTmspCdpDsDTO() {
//        logger.info("获取数据池接口数据");
//        //
////        -- 资金数据池注册
////        select * from yonbip_fi_ctmpub.ctmpub_cdp_datasource;
//        Map<String, Object> tmspCDPMap = getCache().get(KEY_TMSPDS);
//
//        if (tmspCDPMap == null) {
//            logger.error("初始化：获取资金数据池接口数据");
//            tmspCDPMap = new HashMap<>();
//            getCache().put(KEY_TMSPDS, tmspCDPMap);
//
//            TmspCdpDsReqVO tmspCdpDsVO = new TmspCdpDsReqVO();
//            // 只能根据起停用状态过滤，其它无法确定是不是会有配置上规则的
//            tmspCdpDsVO.setEnable(CONST_ENABLED); // 启动状态
//
//            List<TmspCdpDsRespVO> tmspCdpDsRespVOS = tmspCdpDsRpcService.queryByCondition(tmspCdpDsVO);
//            //List<TmspCdpDsRespVO> tmspCdpDsDTOS = buildCdpDTOListForTestData();
//            // tmspCdpDsRpcService.queryByCondition(tmspCdpDsVO);
//            for (TmspCdpDsRespVO tmspCdpDsDTO : tmspCdpDsRespVOS) {
//                tmspCDPMap.put(tmspCdpDsDTO.getId(), tmspCdpDsDTO);
//            }
//        }
//        return tmspCDPMap;
//    }
//
//
//    private List<Object> getUseSettings() {
//        logger.info("获取数据源使用场景信息");
//        // TODO 待查询--资金数据池使用设置
//        /**
//         * action=收付单据匹配关联
//         * 启用状态=启用
//         * 适用对象=银行流水  //多选枚举：√银行流水   √认领单
//         */
//
//        return new ArrayList<>();
//    }
//
//    private List<BankreconciliationIdentifySetting> queryAllRule() {
//        // 银行流水辨识匹配规则
//        List<BankreconciliationIdentifySetting> bankreconciliationIdentifySettings = new ArrayList<>();
//        // TODO 待查询银行流水辨识匹配规则--收付单据匹配关联--启用
//        /**
//         * 流程处理环节	收付单据匹配关联
//         * 适用对象	多选枚举：√银行流水   √认领单  getApplybilltype
//         * 启用 getEnablestatus
//         * 删除标志： getDr
//         * 规则属性=匹配类  //枚举值：匹配类、辨识类
//         */
//
////        getDc_flag 收付方向
////        getExcutelevel
////        getIdentifytype  规则大类
//        //bankreconciliationIdentifySettings = buildBankreconciliationIdentifySettingForTestData();
//        try {
//            bankreconciliationIdentifySettings = bankIdentifyService.querySettingsByCode(RuleCodeConst.SYSTEM005);
//            return bankreconciliationIdentifySettings;
//        } catch (Exception e) {
//            log.error("queryAllRule err", e);
//            return bankreconciliationIdentifySettings;
//        }
//    }
//    /**
//     * 获取注册接口和规则信息
//     * TODO 待-万小波 提供接口
//     */
//    private void getDealDetailRegisterRule(List<BankReconciliation> bankReconciliationList) {
//
//    }
//
//
//    /**
//     * 1）选择全部需要确认，不管匹配到一条结算数据时还是多条结算明细，都需要手工确认；
//     * 2）选择全部无需确认，匹配多条，如果开启'关联多条自动确认'就随机取一条结算明细做自动确认，否则需要人工确认。如果匹配到一条数据，也是自动确认，无需人工介入
//     * */
//    private void processSettleAssociationData(Map<Long, BankReconciliation> bankReconciliationMap, Map<Long, Map<String, CDPRespDetailDTO>> respMap) throws Exception {
//        Flowhandlesetting flowhandlesetting = getFlowhandlesetting();
//        if(null == flowhandlesetting){
//            throw new BankDealDetailException("需要在流水处理规则节点配置"+DealDetailEnumConst.PAYCODE+"规则");
//        }
//        //自动关联是否人工确认: 1(全部需要确认)2(全部不需要确认)
//        Short artiConfirm = flowhandlesetting.getIsArtiConfirm();
//        //artiConfirm=2时，关联多条是否自动确认 true:需要确认 false:自动确认
//        Boolean isAutoConfirm = BooleanUtils.b(flowhandlesetting.getIsRandomAutoConfirm());
//        if(null == respMap || respMap.size()==0){
//            return;
//        }
//        List<CorrDataEntityParam> corrDataEntityParamListByManual = new ArrayList<>();
//        List<CorrDataEntity> corrDataEntityParamListByAuto = new ArrayList<>();
//        for(Long bankReconciliationId : respMap.keySet()){
//           Map<String,CDPRespDetailDTO> cdpRespDetailDTOMap = respMap.get(bankReconciliationId);
//           if(CollectionUtils.isEmpty(cdpRespDetailDTOMap)){
//               continue;
//           }
//           //匹配到结算单数量
//           int count = cdpRespDetailDTOMap.keySet().size();
//           //手工确认
//           if(artiConfirm == 1 || (artiConfirm==2 && count!=1 && !isAutoConfirm)){
//               List<CorrDataEntityParam> corrDataEntityParams = this.buildCorrDataEntityParamByManual(bankReconciliationMap.get(bankReconciliationId),cdpRespDetailDTOMap);
//               corrDataEntityParamListByManual.addAll(corrDataEntityParams);
//               continue;
//           }
//           //自动确认,取第一条
//            Iterator<String> keyIterator = cdpRespDetailDTOMap.keySet().iterator();
//            if(keyIterator.hasNext()){
//                CDPRespDetailDTO cdpRespDetailDTO = cdpRespDetailDTOMap.get(keyIterator.next());
//                try{
//                    CorrDataEntity corrDataEntity = this.buildCorrDataEntityParamByAuto(bankReconciliationMap.get(bankReconciliationId),cdpRespDetailDTO);
//                    if(null!=corrDataEntity){
//                        corrDataEntityParamListByAuto.add(corrDataEntity);
//                    }
//                }catch (Exception e){
//                    log.error("{}流水自动确认，构建调结算实体异常",bankReconciliationId,e);
//                    continue;
//                }
//            }
//            BankReconciliation bankReconciliation = bankReconciliationMap.get(bankReconciliationId);
//            //流水处理状态
/*           bankReconciliation.setSerialdealendstate(ClaimCompleteStatus.Completed.getValue());*/
//            // 流水认领处理方式
//            bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayAssociated.getValue());
//            // TODO 将来考虑确认的处理，目前自动关联
//            bankReconciliation.setAutodealstate(AutoDealState.AutoLink.getValue());
//        }
//        //手工确认
//        if(!CollectionUtils.isEmpty(corrDataEntityParamListByManual)){
//            ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntityParamListByManual);
//        }
//        //自动确认
//        if(!CollectionUtils.isEmpty(corrDataEntityParamListByAuto)){
//            //更新流水信息
//            // 设置状态值
//            Long bankReconciliationId = corrDataEntityParamListByAuto.get(0).getBankReconciliationId();
//            BankReconciliation bankReconciliation = bankReconciliationMap.get(bankReconciliationId);
//            //2、关联事件
//            List<SettleDeatailRelBankBillReqVO> settleDetailRelBankBillReqVOs = autoCorrBill(bankReconciliationMap, corrDataEntityParamListByAuto);
//            IDealDetailCallBack callBack = () ->{
//                logger.error(String.format("%s:关联数据-发消息","sendEventMessage"));
//                BizObject bizObject = new BizObject();
//                bizObject.put("bizObject", settleDetailRelBankBillReqVOs);
//                com.yonyoucloud.fi.cmp.util.SendEventMessageUtils.sendEventMessageEos(bizObject, EVENT_SOURCEID_BANKRECONCILIATION, EVENT_TYPE_CODE_BANKRECONCILIATION);
//                logger.error(String.format("%s:关联数据-发消息结束","sendEventMessage"));
//            };
//            DealDetailUtils.addCallBackToBankReconciliation(bankReconciliation,callBack);
//        }
//    }
//
//    private CorrDataEntity buildCorrDataEntityParamByAuto(BankReconciliation bankReconciliation,CDPRespDetailDTO cdpRespDetailDTO) throws Exception{
//        CtmJSONObject paramMap = new CtmJSONObject();
//        logger.info("处理结算关联数据");
//        Long bid= Long.valueOf(cdpRespDetailDTO.getExtra().get("bid").toString());
//        paramMap.put("bid", bid);
//        paramMap.put("isClaim", "0"); // 是否认领单
//        paramMap.put("billType", EventType.StwbSettleMentDetails.getValue()); // 单据类型
//        paramMap.put("busid", cdpRespDetailDTO.getBillDetailId());
//        paramMap.put("project",cdpRespDetailDTO.getProjectId());
//        paramMap.put("dept",cdpRespDetailDTO.getDeptId());
//        paramMap.put("mainid",cdpRespDetailDTO.getBillId());
//        logger.error(bid.toString());
//        paramMap.put(IBussinessConstant.ORI_SUM,bankReconciliation.getTran_amt());
//        paramMap.put("code",cdpRespDetailDTO.getBillNum());
//        // 先单个处理，接口使用批量
////        1、 处理数据
//        List<CorrDataEntity> corrDataEntities = setAssociatedData(paramMap);
//        return corrDataEntities.get(0);
//    }
//
//    private List<CorrDataEntityParam> buildCorrDataEntityParamByManual(BankReconciliation bankReconciliation,  Map<String,CDPRespDetailDTO> cdpRespDetailDTOMap) {
//        if(CollectionUtils.isEmpty(cdpRespDetailDTOMap)){
//            return null;
//        }
//        List<CorrDataEntityParam> corrDataEntityParamList = new ArrayList<>();
//        Collection<CDPRespDetailDTO> cdpRespDetailDTOS = cdpRespDetailDTOMap.values();
//        Iterator<CDPRespDetailDTO> cdpRespDetailDTOIterator = cdpRespDetailDTOS.iterator();
//        while(cdpRespDetailDTOIterator.hasNext()){
//            CDPRespDetailDTO cdpRespDetailDTO = cdpRespDetailDTOIterator.next();
//            CorrDataEntityParam corrDataEntityParam = new CorrDataEntityParam();
//            HashMap<String,Object> extendFields = new HashMap<>();
//            if(corrDataEntityParam.getExtendFields() == null){
//                corrDataEntityParam.setExtendFields(extendFields);
//            }
//            corrDataEntityParam.getExtendFields().put("relationStatus_flag", Relationstatus.Confirm.getValue());
//            try{
//                corrDataEntityParam.setBillType(String.valueOf(EventType.StwbSettleMentDetails.getValue()));
//                corrDataEntityParam.setBusid(Long.parseLong(cdpRespDetailDTO.getBillDetailId()));
//                corrDataEntityParam.setIsAuto(true);
//                corrDataEntityParam.setGenerate(false);
//                corrDataEntityParam.setBankReconciliationId(bankReconciliation.getId()); //对账单id
//                corrDataEntityParam.setAccentity(cdpRespDetailDTO.getAccentity());
//                corrDataEntityParam.setOriSum(cdpRespDetailDTO.getOriginAmount()); // 原币金额
//                corrDataEntityParam.setDept(cdpRespDetailDTO.getDeptId());
//                corrDataEntityParam.setProject(cdpRespDetailDTO.getProjectId());
//                corrDataEntityParam.setMainid(Long.parseLong(cdpRespDetailDTO.getBillId()));
//                corrDataEntityParam.setCode(cdpRespDetailDTO.getExtra().get("code")+"");
//                corrDataEntityParam.setVouchdate((Date)cdpRespDetailDTO.getExtra().get("vouchdate"));  //单据日期
//                corrDataEntityParam.setBankReconciliationPubts(DateUtils.dateFormat(bankReconciliation.getPubts(),DateUtils.DATE_TIME_PATTERN));
//                corrDataEntityParamList.add(corrDataEntityParam);
//            }catch (Exception e){
//                log.error("【收付单据匹配】手工调回写接口，构建实体失败,流水id={},结算明细SettleDetailId={}",bankReconciliation.getId(),corrDataEntityParam.getCode(),e);
//            }
//        }
//        return corrDataEntityParamList;
//    }
//    /**
//     * 关联数据（认领单暂不支持，使用老代码）
//     *
//     * @param map
//     * @return
//     * @throws Exception
//     * @see com.yonyoucloud.fi.cmp.autocorrsetting.CorrDataProcessingService#setAssociatedData
//     */
//    public List<CorrDataEntity> setAssociatedData(CtmJSONObject map) throws Exception {
//        logger.error(String.format("%s:关联数据-老代码","setAssociatedData"));
//        return autoCorrSettingService.setAssociatedData(map);
//    }
//    /**
//     * @param corrDataEntities
//     * @return
//     * @throws Exception
//     * @See com.yonyoucloud.fi.cmp.autocorrsetting.ManualCorrServiceImpl#manualCorrBill
//     */
//    public List<SettleDeatailRelBankBillReqVO> autoCorrBill(Map<Long, BankReconciliation> bankReconciliations, List<CorrDataEntity> corrDataEntities) throws Exception {
//        logger.error(String.format("%s:关联数据-老代码","autoCorrBill"));
//        if (corrDataEntities == null || corrDataEntities.size() < 1) {
//            return null;
//        }
//        for (CorrDataEntity corrData : corrDataEntities) {
//            corrData.setAuto(Boolean.TRUE);
//            //处理关联数据信息，将关联单据对应关系翻译
////            corrOperationService.corrOpration(corrData);
//            //处理银行对账单关联关系
////            reWriteBusCorrDataService.reWriteBankReconciliationData(corrEntity, false);
////            List<BankReconciliation> list;
////            QuerySchema querySchema = QuerySchema.create().addSelect("*");
////            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBankReconciliationId()));
////            querySchema.addCondition(group);
////            list =  MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
//            BankReconciliation bankReconciliation1 = bankReconciliations.get(corrData.getBankReconciliationId());
//
//            Short associatedStates = 1;//关联状态
//            bankReconciliation1.setAssociationstatus(associatedStates);
//            bankReconciliation1.setAutoassociation(corrData.getAuto());
//            //智能对账：生单关联时，添加勾兑码
//            bankReconciliation1.setSmartcheckno(corrData.getSmartcheckno());
//            bankReconciliation1.setEntrytype(bankReconciliation1.getVirtualEntryType());
////            bankReconciliation1.setEntityStatus(EntityStatus.Update);
//            // 持久化信息给规则任务管理器代码处理
////            CommonSaveUtils.updateBankReconciliation(bankReconciliation1);
////            bankReconciliation1.put("携带测试","test");
//        }
//
//        // 构建并发事件
////         sendEventMessage(buildSettleDTOs(bankReconciliations, corrDataEntities, paramMap));
//        return buildSettleDTOs(bankReconciliations, corrDataEntities);
//    }
//
//    private void corrOpration(CorrDataEntity corrData,BankReconciliation bankReconciliation){
//
//        BankReconciliationbusrelation_b bankReconciliationbusrelationB = new BankReconciliationbusrelation_b();
////        if (corrData.isApi()) {
////            //添加数据
////            bankReconciliationbusrelationB = setBankData4Api(bankReconciliationbusrelationB, corrData);
////            bankReconciliationbusrelationB.setRelationtype(Relationtype.HeterogeneousAssociated.getValue());
////            bankReconciliationbusrelationB.setId(ymsOidGenerator.nextId());
////            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Insert);
////            CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
////            return;
////        }
//        bankReconciliation.setBankReconciliationbusrelation_b(Arrays.asList(new BankReconciliationbusrelation_b[]{bankReconciliationbusrelationB}));
//        /**
//         * 生单关联，需判断是否是业务单据编辑保存，如是业务单据编辑保存，执行Update
//         */
////        if (!corrData.getAuto()) {
////            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
////            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(corrData.getBankReconciliationId()));
////            querySchema1.addCondition(group1);
////            List<BankReconciliationbusrelation_b> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
////            String status = corrData.getStatus() + "";
////            if (bankReconciliations != null && bankReconciliations.size() > 0 && "Update".equals(status)) {
////                /**
////                 * 业务单据修改触发以下代码，找出对应业务单据的关联关系，进行修改。
////                 */
////                for (BankReconciliationbusrelation_b b : bankReconciliations) {
////                    if (b.getBillcode().equals(corrData.getCode())) {
////                        bankReconciliationbusrelationB = b;
////                        break;
////                    }
////                }
////                updateflag = true;
////            }
////        }
//        //赋值关联类型
//        Short isAuto= 2 ;
//        //添加数据
//        bankReconciliationbusrelationB = new CorrOperationServiceImpl().setBankData(bankReconciliationbusrelationB, corrData);
////        if (updateflag) {
////            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Update);
////            MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
////        } else {
//        bankReconciliationbusrelationB.setRelationtype(isAuto);
//        bankReconciliationbusrelationB.setId(ymsOidGenerator.nextId());
//        bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Insert);
////            CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
////        }
//    }
//    /**
//     * 发送事件
//     *
//     * @param settleDeatailRelBankBillReqVOs 结算要的结构体
//     * @throws Exception
//     */
//    private void sendEventMessage(List<SettleDeatailRelBankBillReqVO> settleDeatailRelBankBillReqVOs) throws Exception {
////        trace com.yonyoucloud.fi.cmp.controller.billclaim.autoAssociation.AutoAssociationController confirmCorr  -n 5 --skipJDKMethod false
////        trace com.yonyoucloud.fi.cmp.controller.billclaim.autoAssociation.AutoAssociationController refuseCorr  -n 5 --skipJDKMethod false
////        trace com.yonyoucloud.fi.cmp.controller.billclaim.autoAssociation.AutoAssociationController autoManual  -n 5 --skipJDKMethod false  收到关联
//
//
//        logger.error(String.format("%s:关联数据-发消息","sendEventMessage"));
//        BizObject bizObject = new BizObject();
//        bizObject.put("bizObject", settleDeatailRelBankBillReqVOs);
//        com.yonyoucloud.fi.cmp.util.SendEventMessageUtils.sendEventMessageEos(bizObject, EVENT_SOURCEID_BANKRECONCILIATION, EVENT_TYPE_CODE_BANKRECONCILIATION);
//
//
////        DealDetailcallBack callback=()->{
////            try {SendBizMessageutils.sendBizMessage(bankReconciliation,billNo:"cmp_bankreconciliation",finalAction)l0g.error("发布流水消息消息发送成功!{}",bankReconciliation.getId().tostring());}catch(Exception e){
////                log.error("消息发送失败!"e);
////            }:bankReconciliation.put(DealDetailEnumConSt.BANKRECONCILIATION_CALLACK,callback);
//    }
//
//    private List<SettleDeatailRelBankBillReqVO> buildSettleDTOs(Map<Long, BankReconciliation> bankReconciliations, List<CorrDataEntity> corrDataEntities) throws ParseException {
//        logger.error(String.format("%s:构建结算DTO数据","buildSettleDTOs"));
////        Long bid = Long.valueOf(paramMap.get("bid").toString());
////        //子表id,兼容融入登记单子表id可能为空的情况
////        Long busid = paramMap.get("busid") == null ? null : Long.valueOf(paramMap.get("busid").toString());
////        //借贷方向
////        Short dcFlag = paramMap.get("dcFlag") == null? null : Short.valueOf(paramMap.get("dcFlag").toString());
////        //流水号
////        String bank_seq_no = paramMap.get("bank_seq_no") == null? null : paramMap.get("bank_seq_no").toString();
////        String isClaim = (String)paramMap.get("isClaim");
////        String  billType = (String)paramMap.get("billType");
////        String  pubts = paramMap.get("pubts")+"";
//
//        List<SettleDeatailRelBankBillReqVO> settleDeatailRelBankBillReqVOList = new ArrayList<>();
//        for (CorrDataEntity corrDataEntity : corrDataEntities) {
//            SettleDeatailRelBankBillReqVO settleDeatailRelBankBillReqVO = new SettleDeatailRelBankBillReqVO();
//            settleDeatailRelBankBillReqVO.setBankCheck_id(String.valueOf(corrDataEntity.getBankReconciliationId()));
//            settleDeatailRelBankBillReqVO.setSettleBenchB_id(String.valueOf(corrDataEntity.getBusid()));
//            //智能对账：新增勾兑号传递
//            settleDeatailRelBankBillReqVO.setCheck_identification_code(corrDataEntity.getSmartcheckno());
//            // 根据id查询到银行对账单
//
//            BankReconciliation bankReconciliation = bankReconciliations.get(corrDataEntity.getBankReconciliationId());//MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,corrDataEntities.get(0).getBankReconciliationId());
//            // 1为退票
//            short isrefund_status = 1;
//            //如果退票状态不为空，并且为退票
//           /* if (bankReconciliation.getRefundstatus()!=null&&bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
//                //传入退票状态和退票金额
//                settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(isrefund_status));
//                settleDeatailRelBankBillReqVO.setRefundAmt(bankReconciliation.getTran_amt());
//            }else*/
//            { //非退票.必传，不然结算会报错
//                settleDeatailRelBankBillReqVO.setIsrefund(String.valueOf(0));
//                settleDeatailRelBankBillReqVO.setRefundAmt(new BigDecimal(String.valueOf(0)));
//            }
//            if (null != bankReconciliation.getTran_time()) {
//                settleDeatailRelBankBillReqVO.setRefundDate(bankReconciliation.getTran_time());//退票时间
//                String formattedDateTime = DateUtils.dateFormat(bankReconciliation.getTran_time(), DateUtils.DATE_PATTERN);
//                settleDeatailRelBankBillReqVO.setSettlesuccesstime(formattedDateTime);
//            } else {
//                String formattedDateTime =DateUtils.dateFormat(bankReconciliation.getTran_date(), DateUtils.DATE_PATTERN);
//                // 将日期字符串解析为 Date 对象，带有时分秒
////                settleDeatailRelBankBillReqVO.setRefundDate(outputFormat.parse(formattedDateTime));//退票时间
//                //CZFW-273088 关联接口传递交易时间
//                settleDeatailRelBankBillReqVO.setSettlesuccesstime(formattedDateTime);
//            }
//            //CZFW-273088 关联接口传递交易日期
//            settleDeatailRelBankBillReqVO.setSettlesuccessdate(DateUtils.dateFormat(bankReconciliation.getTran_date(), DateUtils.DATE_PATTERN));
//            settleDeatailRelBankBillReqVOList.add(settleDeatailRelBankBillReqVO);
//        }
//        logger.error(String.format("%s:",settleDeatailRelBankBillReqVOList));
//        return settleDeatailRelBankBillReqVOList;
//    }
//
//    /**
//     * 构建请求参数
//     * TODO 构建请求参数
//     *
//     * @param bankReconciliationList
//     */
//    private List<CDPQueryConditionDTO> buildQueryParams(BankreconciliationIdentifySetting ruleSetting, List<BankReconciliation> bankReconciliationList) {
//        logger.error(String.format("%s:构建取数请求参数","buildQueryParams"));
//        List<CDPQueryConditionDTO> queryDTOList = new ArrayList<>();
//        for (BankReconciliation bankReconciliation : bankReconciliationList) {
//            queryDTOList.add(bulidSingleQueryDTO(bankReconciliation, ruleSetting));
//        }
//        return queryDTOList;
//    }
//
//    /**
//     * 构建查询DTO
//     *
//     * @param bankReconciliation
//     * @param ruleSetting        规则信息
//     * @return
//     */
//    private CDPQueryConditionDTO bulidSingleQueryDTO(BankReconciliation bankReconciliation, BankreconciliationIdentifySetting ruleSetting) {
//        logger.error(String.format("%s:构建取数请求参数","bulidSingleQueryDTO"));
//        CDPQueryConditionDTO queryDto = new CDPQueryConditionDTO();
//        // ** 领域关键特殊信息
//        getUserCaseDTO(queryDto, bankReconciliation, ruleSetting);
//
//        // ** 分页信息
//        queryDto.setQueryPage(getPageInfo(1, 100, 100));
//        // ** 条件参数值
//        // 组织信息
//        queryDto.setAccentitys(Arrays.asList(new String[]{bankReconciliation.getAccentity()})); // 对应流水的使用全组织，流水查规则的时候注意
//        // 查询日期期间 --// 交易日期
//        queryDto.setEndDate(bankReconciliation.getTran_date());
//        // TODO 待修改浮动天数--演示暂时支持5天
//        queryDto.setBeginDate(getBeforeDate(queryDto.getEndDate(), 2));
//
//        // 银行对账编码？
//        // 银行账号
//        queryDto.setBankAccountIds(Arrays.asList(new String[]{bankReconciliation.getBankaccount()}));
//        // 对方账户
//        queryDto.setOppAcc(Arrays.asList(new String[]{bankReconciliation.getTo_acct()}));
//        // 对方户名
//        queryDto.setOppName(Arrays.asList(new String[]{bankReconciliation.getTo_acct_name()}));
//        // 金额
//        queryDto.setMinAmount(bankReconciliation.getTran_amt());
//        queryDto.setMaxAmount(bankReconciliation.getTran_amt());
//
//        // 摘要-- 扩展
//
//        // 设置扩展信息
//        buildCDPExtQuerySchemaGroupDTO(queryDto, bankReconciliation, ruleSetting);
//
//        return queryDto;
//    }
//
//    /**
//     * 获取用户用例信息
//     *
//     * @param queryDto    查询条件DTO
//     * @param ruleSetting
//     */
//    private void getUserCaseDTO(CDPQueryConditionDTO queryDto, BankReconciliation bankReconciliation, BankreconciliationIdentifySetting ruleSetting) {
//        logger.error(String.format("%s:构建取数请求参数","getUserCaseDTO"));
//        CDPUseCaseDTO userCaseDto = new CDPUseCaseDTO();
//        userCaseDto.setUseCaseType(CDPUseCaseType.CMPDealDetail);
//        userCaseDto.getUseCaseInfo().put(KEY_EXTFIELD_BANKRECONCILIATIONID, bankReconciliation.getId()); // 存放流水ID，用户批量传参
//        userCaseDto.getUseCaseInfo().put(KEY_EXTFIELD_BUSIOBJ, ruleSetting.getMatchobjectid());  // 业务对象ID,用于领域区分查那个表
//        queryDto.setUserCaseDto(userCaseDto);
//    }
//
//    /**
//     * 获取查询前n天日期
//     *
//     * @param date 要查询的结束日期
//     * @param n    提前天数
//     * @return
//     */
//    private Date getBeforeDate(Date date, int n) {
//        Calendar calendar = Calendar.getInstance(); // 获取当前日期
//        calendar.setTime(date);
//        // 当前日期前一天
//        calendar.add(Calendar.DATE, -1 * n);
//        return calendar.getTime();
//    }
//
//    /**
//     * 获取分页信息
//     *
//     * @param currentPage 查询当前页
//     * @param pageSize    每页大小
//     * @param totalCount  最大总记录数
//     * @return
//     */
//    private CDPQueryPageDTO getPageInfo(int currentPage, int pageSize, int totalCount) {
//        CDPQueryPageDTO pageDto = new CDPQueryPageDTO();
//        pageDto.setPageSize(pageSize);
//        pageDto.setPageIndex(currentPage);
//        return pageDto;
//    }
//
//    private void buildCDPExtQuerySchemaGroupDTO(CDPQueryConditionDTO queryDto, BankReconciliation bankReconciliation, BankreconciliationIdentifySetting ruleSetting) {
//
//        logger.error(String.format("%s:构建取数请求参数","buildCDPExtQuerySchemaGroupDTO"));
//        CDPExtQuerySchemaGroupDTO cdpExtQuerySchemaGroupDTO = new CDPExtQuerySchemaGroupDTO();
//        // 流水里面只需要一个分组
//        List<CDPQuerySchemaGroupDTO> cdpQuerySchemaGroupDTOS = new ArrayList<>();
//        cdpExtQuerySchemaGroupDTO.setQuerySchemaGroupList(cdpQuerySchemaGroupDTOS);
//        List<CDPQuerySchemaDTO> cdpQuerySchemaDTOS = new ArrayList<>();
//        CDPQuerySchemaGroupDTO cdpQuerySchemaGroupDTO = new CDPQuerySchemaGroupDTO();
//        cdpQuerySchemaGroupDTOS.add(cdpQuerySchemaGroupDTO);
//        cdpQuerySchemaGroupDTO.setQuerySchemas(cdpQuerySchemaDTOS);
//
//        // 结算收付方向和现金收付方向相反
//        Short receipttype=ruleSetting.getDc_flag();
//        if(receipttype == Direction.Debit.getValue()){
//            receipttype = Direction.Credit.getValue();
//        }else if(receipttype == Direction.Credit.getValue()){
//            receipttype = Direction.Debit.getValue();
//        }
//
//               // ==Direction.Debit.getValue()?Direction.Credit.getValue():Direction.Debit)
//        cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO("receipttype", CDPConditionOperatorEnum.eq, receipttype)); //收付方向  -- TODO 需要确定值是否匹配 1/2
////        cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO("receipttype", CDPConditionOperatorEnum.eq, 2));
//        // 设定规则值
////        cdpQuerySchemaDTOS.add(new CDPQuerySchemaDTO("mainid.settleBench_b", CDPConditionOperatorEnum.eq, bankReconciliation.getRemark())); // 根据业务对象实际值来判断  TODO 注释掉，无数据
//
//        queryDto.setExtQueryCondition(Arrays.asList(new CDPExtQuerySchemaGroupDTO[]{cdpExtQuerySchemaGroupDTO}));
//    }
//
//
//    /**
//     * 调用查询接口
//     *
//     * @param bankreconciliationIdentifySetting
//     * @param queryConditionDTOS                查询条件
//     * @return 查询结果集
//     * @throws CDPBusinessException
//     */
//    private List<CDPRespDetailDTO> callCDPInterface(BankreconciliationIdentifySetting bankreconciliationIdentifySetting, List<CDPQueryConditionDTO> queryConditionDTOS) throws CDPBusinessException, ClassNotFoundException {
//
//        logger.error(String.format("%s:调用取数接口","callCDPInterface"));
//        // 根据设置获取Classname
//        String className = ICDPQueryService.class.getName();
//        String domainCode = "yonbip-fi-ctmstwb";
//        return getService(className, domainCode).queryByConditions(queryConditionDTOS.toArray(new CDPQueryConditionDTO[0]));
//    }
//
//    /**
//     * 获取取数接口(现成缓存)
//     *
//     * @param className  取数接口类名
//     * @param domainCode
//     * @return
//     */
//    private ICDPQueryService getService(String className, String domainCode) throws ClassNotFoundException {
//        logger.error(String.format("%s:调用取数接口","getService"));
////        bankreconciliationIdentifySetting.getCdp_id();// 数据源ID
//
//        Class icdpQueryServiceClass = Class.forName(className);
//        ICDPQueryService cdpservice = (ICDPQueryService) getCache().get(KEY_CDPSERVICE).get(domainCode + icdpQueryServiceClass.getName());
//        if (cdpservice == null) {
//            cdpservice = (ICDPQueryService) DubboReferenceUtils.getDubboService(icdpQueryServiceClass, domainCode, null, TIMEOUT_MS);
//            getCache().get(KEY_CDPSERVICE).put(domainCode + icdpQueryServiceClass.getName(), cdpservice);
//        }
//        return cdpservice;
//    }
//    private void sendBizMessage()throws Exception{
////        TODO 发消息  SendBizMessageUtils  815
//    }
//    /********   test  *******/
////    测试方式：
////    https://bip-test.yonyoucloud.com/yonbip-fi-ctmcmp/cmp/task/testHandler
////    {
////       "handler_name":"system004"
////    }
//    private final Long cdp_id=0000000000000000000L;
//    private final Long ruleSetting_mainid=1000000000000000000L;
//    private List<TmspCdpDsRespVO> buildCdpDTOListForTestData() {
//        List<TmspCdpDsRespVO> tmspCdpDsDTOS = new ArrayList<>();
//        TmspCdpDsRespVO tmspCdpDsDTO = new TmspCdpDsRespVO();
//        tmspCdpDsDTO.setId("2029562546422284297");
//        tmspCdpDsDTO.setName("结算平台现金流数据");
//        tmspCdpDsDTO.setDsType("1");
//        tmspCdpDsDTO.setCode("stwb");
//
//        tmspCdpDsDTO.setRequestMode("1"); // RPC模式
//        tmspCdpDsDTO.setSourceSystemInterfaceAddr(ICDPQueryService.class.getName());  // 接口地址
//
//        tmspCdpDsDTOS.add(tmspCdpDsDTO);
//        return tmspCdpDsDTOS;
//    }
//
//    private List<BankreconciliationIdentifySetting> buildBankreconciliationIdentifySettingForTestData(){
//        List<BankreconciliationIdentifySetting> settings = new ArrayList<>();
//        // 付款
//        BankreconciliationIdentifySetting setting = new BankreconciliationIdentifySetting();
//        setting.setEnablestatus(Short.valueOf("1"));
//        setting.setId(ruleSetting_mainid);
//        setting.setCdp_id(String.valueOf(cdp_id));
//        setting.setAccentity(getTenantOrg());
//        setting.setDc_flag(Direction.Debit.getValue());
//        setting.setExcutelevel(1);
////        setting.setBanktype(); 先不设置
////        BankreconciliationIdentifySetting_b setting_b = new BankreconciliationIdentifySetting_b();
////        setting_b.setMainid(ruleSetting_mainid);
////        setting_b.setId(ruleSetting_mainid+1);
////        setting_b.setApplyfieldname("receipttype");
//        settings.add(setting);
//        // 收款
//        BankreconciliationIdentifySetting setting_c = (BankreconciliationIdentifySetting) setting.clone();
//        setting_c.setDc_flag(Direction.Credit.getValue());
//        setting_c.setId(ruleSetting_mainid+1);
//        settings.add(setting_c);
//        return settings;
//    }
//    private List<BankDealDetailWrapper> buildBankDealDetailWrapperForTestData(){
//        List<BankDealDetailWrapper> wrappers = new ArrayList<>();
//        BankDealDetailWrapper bankDealDetailWrapper = new BankDealDetailWrapper();
//        bankDealDetailWrapper.setBankReconciliation(new BankReconciliation());
//
//        bankDealDetailWrapper.getBankReconciliation().setOrgid(String.valueOf(1));
//        bankDealDetailWrapper.getBankReconciliation().setAccentity("666666");
//
//        bankDealDetailWrapper.getBankReconciliation().setDc_flag(Direction.Debit);
//        bankDealDetailWrapper.getBankReconciliation().setAcct_name("本方账号");
//        bankDealDetailWrapper.getBankReconciliation().setBankaccount("测试银行账号id");
//        bankDealDetailWrapper.getBankReconciliation().setAcct_bal(BigDecimal.valueOf(0));
//        bankDealDetailWrapper.getBankReconciliation().setBanktype("本方工商银行类别ID");
//
//        bankDealDetailWrapper.getBankReconciliation().setTo_acct_bank("对方建设银行ID");
//        bankDealDetailWrapper.getBankReconciliation().setTo_acct_bank_name("对方建设银行");
//
//
//        bankDealDetailWrapper.getBankReconciliation().setCurrency("CNY");
//
//
//        wrappers.add(bankDealDetailWrapper);
//        return wrappers;
//    }
//
//}
