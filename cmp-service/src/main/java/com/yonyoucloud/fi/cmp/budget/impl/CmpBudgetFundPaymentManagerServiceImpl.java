package com.yonyoucloud.fi.cmp.budget.impl;

import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetFundPaymentManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CmpBudgetFundPaymentManagerServiceImpl implements CmpBudgetFundPaymentManagerService {

    public static final String RULE_ID = "ruleId";
    public static final String RULECTRL_ID = "ruleCtrlId";
    /**
     * 规则类型(预占pre；执行implement)
     */
    public static final String RULE_TYPE = "ruleType";
    /**
     * 单据类型唯一标识
     */
    public static final String BILL_CODE = "billCode";
    /**
     * 交易类型唯一标识
     */
    public static final String TRANSAC_CODE = "transacCode";
    /**
     * 单据id(整单id)
     */
    public static final String BILL_ID = "billId";
    public static final String BILL_NO = "billNo";
    /**
     * 单据编号
     */
    public static final String BILL_BUS_CODE = "billBusCode";
    public static final String LINE_NO = "lineNo";
    public static final String SERVICE_CODE = "serviceCode";
    /**
     * 单据行号 not required
     */
    public static final String LINE = "line";

    /**
     * 子表唯一标志
     */
    public static final String BILL_LINE_ID = "billLineId";
    /**
     * 单据动作 not required
     */
    public static final String ACTION = "action";
    /**
     * 开始时间
     */
    public static final String START_DATE = "startDate";
    /**
     * 结束时间
     */
    public static final String END_DATE = "endDate";
    public static final String VERIFYSTATE = "verifystate";
    public static final String ISWFCONTROLLED = "isWfControlled";
    public static final String SETTLESTATUS = "settlestatus";
    public static final String FUNDPAYMENT_B = "FundPayment_b__";
    public static final String FUNDCOLLECTION_B = "FundCollection_b__";
    public static final String CHARACTERDEF_ = "characterDef_";
    public static final String CHARACTERDEF = "characterDef.";
    public static final String CHARACTERDEFB_ = "characterDefb_";
    public static final String CHARACTERDEFB = "characterDefb.";

    @Autowired
    private CmCommonService cmCommonService;

    @Override
    public CtmJSONArray queryFundPayment(CmpBudgetEventBill budgetEventBill) throws Exception{
        //1 查询指定时间内的单据
        QuerySchema querySchema0 = QuerySchema.create().addSelect("*");// 审批通过
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");// 审批中
        // 子表查询条件
        QuerySchema querySchema_b = QuerySchema.create().addSelect("*");
        CtmJSONObject billfieldValues= CtmJSONObject.toJSON(budgetEventBill.getBillfieldValues());
        //2 添加控制过滤条件
        if(billfieldValues!=null){
            for(String str:billfieldValues.keySet()){
                CtmJSONArray strvals = billfieldValues.getJSONArray(str);
                if(strvals.size()>0){
                    for(int i=0;i<strvals.size();i++){
                        CtmJSONObject bfValues= strvals.getJSONObject(i);
                        CtmJSONArray bfAValues = bfValues.getJSONArray("value");
                        Object[] bfAValuesTA = bfAValues.stream().toArray();
                        if (str.contains(CHARACTERDEF_)) {
                            // 特征字段特殊处理
                            str = str.replace(CHARACTERDEF_, CHARACTERDEF);
                        }
                        if (str.contains(CHARACTERDEFB_)) {
                            // 子表特征字段特殊处理
                            str = str.replace(CHARACTERDEFB_, CHARACTERDEFB);
                        }
                        if (str.contains(FUNDPAYMENT_B)) {
                            // 子表过滤字段
                            querySchema_b.appendQueryCondition(QueryCondition.name(str.replace(FUNDPAYMENT_B, "")).in(bfAValuesTA));
                        } else {
                            querySchema0.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                            querySchema1.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                        }
                    }
                }
            }
        }
        //3 单据时间key为billTime，根据billTime去查询startTime和endTime之间的数据
        if (budgetEventBill.getBillTime() != null && budgetEventBill.getBillTime().contains(FUNDPAYMENT_B)) {
            // 子表时间字段
            querySchema_b.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime().replace(FUNDPAYMENT_B, "")).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        } else {
            querySchema0.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
            querySchema1.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        }
        String ruleType = budgetEventBill.getRuleType();
        /**
         * 期初数据过滤条件说明：
         * 一、预占数据：
         * 预占时机为空：不送
         * 预占时机为保存：上送审批流状态为初始开立/驳回到制单的单据
         * 预占时机为提交：
         * 启用审批流单据，审批流状态为审批中/审批完成并且付款明细结算状态等于结算中/待结算
         * 未启用审批流单据，审批流状态为审批完成并且付款明细结算状态等于结算中/待结算
         * 预占时机为审核：
         * 审批流状态为审批完成并且结算状态=结算中/待结算
         * 说明：预占金额统一取付款金额
         * 二、执行数据（即实占）：
         * 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单，执行金额为结算成功金额
         */
        List<FundPayment_b> fundPayment_bs = new ArrayList<>();// 子表数据集合
        // 主表Map集合-审批中和审批通过
        Map<String, FundPayment> fundPaymentsMap = new HashMap<>();
        if ("pre".equals(ruleType)) {
            // TODO 目前触发时机为写死的，提交审批流预置，结算成功实占；后续如果根据配置时机占用，需保留下面逻辑
            // 触发时机(commit---提交；save---保存；examine---审核)	triggerType
//            if ("save".equals(budgetEventBill.getTriggerType())) {
//                // 预占时机：保存
//                querySchema.appendQueryCondition(QueryCondition.name(VERIFYSTATE).in(VerifyState.INIT_NEW_OPEN.getValue(), VerifyState.REJECTED_TO_MAKEBILL));
//            } else if ("commit".equals(budgetEventBill.getTriggerType())) {
//                // 预占时机：提交
//                querySchema_b.appendQueryCondition(QueryCondition.name(SETTLESTATUS).eq(SettleStatus.noSettlement.getValue()));
//                QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
//                queryConditionGroup.addCondition(QueryCondition.name(ISWFCONTROLLED).eq(1), QueryCondition.name(VERIFYSTATE).in(VerifyState.SUBMITED.getValue(), VerifyState.COMPLETED.getValue()));
//                queryConditionGroup.addCondition(QueryCondition.name(ISWFCONTROLLED).eq(0), QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
//                querySchema.addCondition(queryConditionGroup);
//            } else if ("examine".equals(budgetEventBill.getTriggerType())) {
//                // 预占时机：审核
//                querySchema_b.appendQueryCondition(QueryCondition.name(SETTLESTATUS).eq(SettleStatus.noSettlement.getValue()));
//                querySchema.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
//            }
            /**
             * 启用审批流单据，审批流状态为审批中/审批完成并且付款明细结算状态等于结算中/待结算
             * 未启用审批流单据，审批流状态为审批完成并且付款明细结算状态等于结算中/待结算
             */
            // 审批通过-主表单据
            querySchema0.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
            List<FundPayment> list0 = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema0,null);
            // 主表id集合-审批通过
            List<String> ids0 = new ArrayList();
            list0.stream().forEach(e ->{
                ids0.add(e.getId().toString());
                fundPaymentsMap.put(e.getId().toString(), e);
            });
            // 审批中-主表单据
            querySchema1.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.SUBMITED.getValue()));
            List<FundPayment> list1 = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema1,null);
            List<String> ids1 = new ArrayList();
            list1.stream().forEach(e ->{
                ids1.add(e.getId().toString());
                fundPaymentsMap.put(e.getId().toString(), e);
            });
            if (ids0.size() == 0 && ids1.size() == 0) {
                return new CtmJSONArray();
            }
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
            QueryConditionGroup queryConditionGroup0 = new QueryConditionGroup(ConditionOperator.and);
            QueryConditionGroup queryConditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            // 主表审批通过，且子表结算状态为结算中/待结算
            if (ids0.size() > 0) {
                queryConditionGroup0.addCondition(QueryCondition.name("mainid").in(ids0));
                queryConditionGroup0.addCondition(QueryCondition.name(SETTLESTATUS).in(FundSettleStatus.WaitSettle.getValue(),FundSettleStatus.SettleProssing.getValue()));
                queryConditionGroup.addCondition(queryConditionGroup0);
            }
            // 主表审批中，不包含退票
            if (ids1.size() > 0) {
                queryConditionGroup1.addCondition(QueryCondition.name("mainid").in(ids1));
                queryConditionGroup1.addCondition(QueryCondition.name(SETTLESTATUS).not_in(FundSettleStatus.Refund.getValue()));
                queryConditionGroup.addCondition(queryConditionGroup1);
            }
            querySchema_b.appendQueryCondition(queryConditionGroup);
            fundPayment_bs = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema_b,null);
            updateFundPayment(fundPayment_bs, OccupyBudget.PreSuccess.getValue());
        } else if ("implement".equals(ruleType)) {
            // 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单
            querySchema0.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
            querySchema_b.appendQueryCondition(QueryCondition.name(SETTLESTATUS).in(FundSettleStatus.SettleSuccess.getValue(),FundSettleStatus.PartSuccess.getValue(),FundSettleStatus.SettlementSupplement.getValue()));
            // 主表List集合
            List<FundPayment> list = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema0,null);
            // 主表id集合
            List<String> ids = new ArrayList();
            list.stream().forEach(e ->{
                ids.add(e.getId().toString());
                fundPaymentsMap.put(e.getId().toString(), e);
            });
            if (ids.size() == 0) {
                return new CtmJSONArray();
            }
            querySchema_b.appendQueryCondition(QueryCondition.name("mainid").in(ids));
            fundPayment_bs = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema_b,null);
            updateFundPayment(fundPayment_bs, OccupyBudget.ActualSuccess.getValue());
        }
        // 数据查询完毕，准备拼装报文
        CtmJSONArray array = new CtmJSONArray();
        HashMap<String,String> butyCode = new HashMap<>();
        for (FundPayment_b fundPayment_b: fundPayment_bs) {
            FundPayment fundPayment = fundPaymentsMap.get(fundPayment_b.getMainid().toString());
            CtmJSONObject fundPaymentJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(fundPayment));
            Map characterDefMap = fundPayment.get("characterDef");
            if (characterDefMap != null && !characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> fundPaymentJson.put(CHARACTERDEF_ + key, value));
            }
            CtmJSONObject fundPayment_bJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(fundPayment_b));
            // 期初需要传行号，区分子表数据，不然多条子表时会被覆盖成一条，业务单据台账会有问题，需上送billLineId字段
            fundPaymentJson.put(BILL_LINE_ID, fundPayment_b.getId().toString());
            fundPayment_bJson.entrySet().stream().forEach(e -> {
                /**
                 * 为区分单据的主子表区域的单据字段（尤其是单据主表和单据子表使用相同编码的数据）；主表数据直接使用单据字段做key值，子表数据需要使用：主子实体在领域元数据中定义的组合关系中的roleA的值.子表单据字段作为key（ls：通用报销单子表_费用承担部门: expensebillbsvfinacedeptid_name; 报销单费用分摊费用承担部门: expapportions_vfinacedeptid_name）。
                 * 说明：采用__(双下划线)链接子表区域标识和单据字段。
                 */
                fundPaymentJson.put(FUNDPAYMENT_B+e.getKey(), e.getValue());
                Map subCharacterDefMap = fundPayment_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> fundPaymentJson.put(FUNDPAYMENT_B + CHARACTERDEFB_ + key, value));
                }
            });
            //规则id，从推送的消息中获取
            fundPaymentJson.put(RULE_ID, budgetEventBill.getRuleId());
            fundPaymentJson.put(RULECTRL_ID, budgetEventBill.getRuleCtrlId());
            //规则类型 (预占pre；执行implement) 从推送的消息中获取
            fundPaymentJson.put(RULE_TYPE, budgetEventBill.getRuleType());
            //单据类型 可从推送消息中获取，也可以直接写成自己对接的单据类型
            fundPaymentJson.put(BILL_CODE, budgetEventBill.getBillCode());
            //交易类型
            String bustype = fundPayment.getTradetype();
            if(butyCode.containsKey(bustype)){
                fundPaymentJson.put(TRANSAC_CODE, butyCode.get(bustype));
            }else{
                String code = cmCommonService.getDefaultTransTypeCode(bustype);
                butyCode.put(bustype,code);
                fundPaymentJson.put(TRANSAC_CODE, code);
            }
            //单据id
            fundPaymentJson.put(BILL_ID, fundPayment_b.getMainid());
            //单据编号
            fundPaymentJson.put(BILL_BUS_CODE, fundPayment.getCode());
            fundPaymentJson.put(LINE_NO, fundPayment_b.getLineno().intValue());
            fundPaymentJson.put(SERVICE_CODE, IServicecodeConstant.FUNDPAYMENT);
            array.add(fundPaymentJson);
        }
        log.error("预算期初推送报文：" + array);
        return array;
    }

    @Override
    public void updateFundPayment(List<FundPayment_b> fundPayment_bs, Short isOccupyBudget) throws Exception {
        fundPayment_bs.stream().forEach(item->{
            item.setIsOccupyBudget(isOccupyBudget);
            item.setEntityStatus(EntityStatus.Update);
        });
        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment_bs);
    }

    /**
     * * 查询资金付款单
     *
     * @param budgetEventBill
     * @return
     */
    @Override
    public CtmJSONArray queryFundCollection(CmpBudgetEventBill budgetEventBill) throws Exception {
        //1 查询指定时间内的单据
        QuerySchema querySchema0 = QuerySchema.create().addSelect("*");// 审批通过
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");// 审批中
        // 子表查询条件
        QuerySchema querySchema_b = QuerySchema.create().addSelect("*");
        CtmJSONObject billfieldValues= CtmJSONObject.toJSON(budgetEventBill.getBillfieldValues());
        //2 添加控制过滤条件
        if(billfieldValues!=null){
            for(String str:billfieldValues.keySet()){
                CtmJSONArray strvals = billfieldValues.getJSONArray(str);
                if(strvals.size()>0){
                    for(int i=0;i<strvals.size();i++){
                        CtmJSONObject bfValues= strvals.getJSONObject(i);
                        CtmJSONArray bfAValues = bfValues.getJSONArray("value");
                        Object[] bfAValuesTA = bfAValues.stream().toArray();
                        if (str.contains(CHARACTERDEF_)) {
                            // 特征字段特殊处理
                            str = str.replace(CHARACTERDEF_, CHARACTERDEF);
                        }
                        if (str.contains(CHARACTERDEFB_)) {
                            // 子表特征字段特殊处理
                            str = str.replace(CHARACTERDEFB_, CHARACTERDEFB);
                        }
                        if (str.contains(FUNDCOLLECTION_B)) {
                            // 子表过滤字段
                            querySchema_b.appendQueryCondition(QueryCondition.name(str.replace(FUNDCOLLECTION_B, "")).in(bfAValuesTA));
                        } else {
                            querySchema0.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                            querySchema1.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                        }
                    }
                }
            }
        }
        //3 单据时间key为billTime，根据billTime去查询startTime和endTime之间的数据
        if (budgetEventBill.getBillTime() != null && budgetEventBill.getBillTime().contains(FUNDCOLLECTION_B)) {
            // 子表时间字段
            querySchema_b.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime().replace(FUNDCOLLECTION_B, "")).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        } else {
            querySchema0.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
            querySchema1.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        }
        String ruleType = budgetEventBill.getRuleType();
        /**
         * 期初数据过滤条件说明：
         * 一、预占数据：
         * 预占时机为空：不送
         * 预占时机为保存：上送审批流状态为初始开立/驳回到制单的单据
         * 预占时机为提交：
         * 启用审批流单据，审批流状态为审批中/审批完成并且付款明细结算状态等于结算中/待结算
         * 未启用审批流单据，审批流状态为审批完成并且付款明细结算状态等于结算中/待结算
         * 预占时机为审核：
         * 审批流状态为审批完成并且结算状态=结算中/待结算
         * 说明：预占金额统一取付款金额
         * 二、执行数据（即实占）：
         * 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单，执行金额为结算成功金额
         */
        List<FundCollection_b> fundCollection_bs = new ArrayList<>();// 子表数据集合
        // 主表Map集合-审批中和审批通过
        Map<String, FundCollection> fundCollectionsMap = new HashMap<>();
        if ("implement".equals(ruleType)) {
            // 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单
            querySchema0.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
            querySchema_b.appendQueryCondition(QueryCondition.name(SETTLESTATUS).in(FundSettleStatus.SettleSuccess.getValue(),FundSettleStatus.PartSuccess.getValue(),FundSettleStatus.SettlementSupplement.getValue()));
            // 主表List集合
            List<FundCollection> list = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema0,null);
            // 主表id集合
            List<String> ids = new ArrayList();
            list.stream().forEach(e ->{
                ids.add(e.getId().toString());
                fundCollectionsMap.put(e.getId().toString(), e);
            });
            if (ids.size() == 0) {
                return new CtmJSONArray();
            }
            querySchema_b.appendQueryCondition(QueryCondition.name("mainid").in(ids));
            fundCollection_bs = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema_b,null);
            updateFundCollection(fundCollection_bs, OccupyBudget.ActualSuccess.getValue());
        }
        // 数据查询完毕，准备拼装报文
        CtmJSONArray array = new CtmJSONArray();
        HashMap<String,String> butyCode = new HashMap<>();
        for (FundCollection_b fundCollection_b: fundCollection_bs) {
            FundCollection fundCollection = fundCollectionsMap.get(fundCollection_b.getMainid().toString());
            CtmJSONObject fundCollectionJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(fundCollection));
            Map characterDefMap = fundCollection.get("characterDef");
            if (characterDefMap != null && !characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> fundCollectionJson.put(CHARACTERDEF_ + key, value));
            }
            CtmJSONObject fundCollection_bJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(fundCollection_b));
            // 期初需要传行号，区分子表数据，不然多条子表时会被覆盖成一条，业务单据台账会有问题，需上送billLineId字段
            fundCollectionJson.put(BILL_LINE_ID, fundCollection_b.getId().toString());
            fundCollection_bJson.entrySet().stream().forEach(e -> {
                /**
                 * 为区分单据的主子表区域的单据字段（尤其是单据主表和单据子表使用相同编码的数据）；主表数据直接使用单据字段做key值，子表数据需要使用：主子实体在领域元数据中定义的组合关系中的roleA的值.子表单据字段作为key（ls：通用报销单子表_费用承担部门: expensebillbsvfinacedeptid_name; 报销单费用分摊费用承担部门: expapportions_vfinacedeptid_name）。
                 * 说明：采用__(双下划线)链接子表区域标识和单据字段。
                 */
                fundCollectionJson.put(FUNDCOLLECTION_B+e.getKey(), e.getValue());
                Map subCharacterDefMap = fundCollection_b.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> fundCollectionJson.put(FUNDCOLLECTION_B + CHARACTERDEFB_ + key, value));
                }
            });
            //规则id，从推送的消息中获取
            fundCollectionJson.put(RULE_ID, budgetEventBill.getRuleId());
            fundCollectionJson.put(RULECTRL_ID, budgetEventBill.getRuleCtrlId());
            //规则类型 (预占pre；执行implement) 从推送的消息中获取
            fundCollectionJson.put(RULE_TYPE, budgetEventBill.getRuleType());
            //单据类型 可从推送消息中获取，也可以直接写成自己对接的单据类型
            fundCollectionJson.put(BILL_CODE, budgetEventBill.getBillCode());
            //交易类型
            String bustype = fundCollection.getTradetype();
            if(butyCode.containsKey(bustype)){
                fundCollectionJson.put(TRANSAC_CODE, butyCode.get(bustype));
            }else{
                String code = cmCommonService.getDefaultTransTypeCode(bustype);
                butyCode.put(bustype,code);
                fundCollectionJson.put(TRANSAC_CODE, code);
            }
            //单据id
            fundCollectionJson.put(BILL_ID, fundCollection_b.getMainid());
            //单据编号
            fundCollectionJson.put(BILL_BUS_CODE, fundCollection.getCode());
            fundCollectionJson.put(LINE_NO, fundCollection_b.getLineno().intValue());
            fundCollectionJson.put(SERVICE_CODE, IServicecodeConstant.FUNDCOLLECTION);
            array.add(fundCollectionJson);
        }
        log.error("预算期初推送报文：" + array);
        return array;
    }

    /**
     * * 更新
     *
     * @param fundCollections
     * @param isOccupyBudget
     * @return
     * @throws Exception
     */
    @Override
    public void updateFundCollection(List<FundCollection_b> fundCollections, Short isOccupyBudget) throws Exception {
        fundCollections.stream().forEach(item->{
            item.setIsOccupyBudget(isOccupyBudget);
            item.setEntityStatus(EntityStatus.Update);
        });
        MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollections);
    }

}
