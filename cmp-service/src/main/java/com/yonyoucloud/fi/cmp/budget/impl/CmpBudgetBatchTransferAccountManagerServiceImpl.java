package com.yonyoucloud.fi.cmp.budget.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.epmp.control.dto.ControlDetailVO;
import com.yonyou.epmp.control.service.ExecdataControlService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.unifiedsettle.enums.SettleDetailSettleStateEnum;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.budget.*;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * 同名账户批量划转对接预算中台
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmpBudgetBatchTransferAccountManagerServiceImpl implements CmpBudgetBatchTransferAccountManagerService {
    // 预占
    public static final String PRE = "pre";
    // 实占
    public static final String IMPLEMENT = "implement";

    public static final String CHARACTERDEF_ = "characterDef_";
    public static final String CHARACTERDEFB_ = "characterDefb_";
    public static final String CHARACTERDEF = "characterDef.";
    public static final String CHARACTERDEFB = "characterDefb.";
    public static final String BATCHTRANSFERACCOUNT_B = "BatchTransferAccount_b__";
    public static final String VERIFYSTATE = "verifystate";
    public static final String PAYSETTLESTATUS = "paySettleStatus";
    /**
     * 子表唯一标志
     */
    public static final String BILL_LINE_ID = "billLineId";

    @Autowired
    private BusiSystemConfigService busiSystemConfigService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private ExecdataControlService execdataControlService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;


    /**
     * 查询符合规则设置的数据
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
     *
     * @param budgetEventBill
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray queryBillByRule(CmpBudgetEventBill budgetEventBill) throws Exception {
        //1 查询指定时间内的单据
        // 审批通过
        QuerySchema querySchemaAp = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        // 审批中
        QuerySchema querySchemaAi = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        // 子表查询条件
        QuerySchema querySchema_b = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        // 构建查询条件
        buildQuerySchema(querySchemaAp, querySchemaAi, querySchema_b, budgetEventBill);

        // 主表Map集合-审批中和审批通过
        Map<String, BatchTransferAccount> batchTransferAccountMap = new HashMap<>();
        // 子表数据集合
        List<BatchTransferAccount_b> batchTransferAccountBs;
        if (PRE.equals(budgetEventBill.getRuleType())) {
            batchTransferAccountBs = getBillDataOfPre(querySchemaAp, querySchemaAi, querySchema_b, batchTransferAccountMap, budgetEventBill);
            updateBillList(batchTransferAccountBs, OccupyBudget.PreSuccess.getValue());
        } else if (IMPLEMENT.equals(budgetEventBill.getRuleType())) {
            batchTransferAccountBs = getBillDateOfImplement(querySchemaAp, querySchema_b, batchTransferAccountMap, budgetEventBill);
            updateBillList(batchTransferAccountBs, OccupyBudget.ActualSuccess.getValue());
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400626", "ruleType参数错误") /* "ruleType参数错误" */);
        }

        // 数据查询完毕，准备拼装报文
        CtmJSONArray array = new CtmJSONArray();
        if (CollectionUtils.isEmpty(batchTransferAccountBs)) {
            return array;
        }
        HashMap<String,String> butyCode = new HashMap<>();
        for (BatchTransferAccount_b batchTransferAccountB: batchTransferAccountBs) {
            if (batchTransferAccountB.getMainid() == 0L) {
                log.error("同名账户批量划转主子表数据存在问题，子表id:{}", batchTransferAccountB.getId().toString());
                continue;
            }
            BatchTransferAccount batchTransferAccount = batchTransferAccountMap.get(String.valueOf(batchTransferAccountB.getMainid()));
            if (batchTransferAccount == null) {
                log.error("同名账户批量划转主子表数据存在问题，子表id:{},主表id:{}", batchTransferAccountB.getId().toString(),batchTransferAccountB.getMainid());
                continue;
            }
            CtmJSONObject batchTransferAccountJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(batchTransferAccount));
            Map characterDefMap = batchTransferAccountB.get("characterDef");
            if (characterDefMap != null && !characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> batchTransferAccountJson.put(CHARACTERDEF_ + key, value));
            }
            CtmJSONObject batchTransferAccountBJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(batchTransferAccountB));
            // 期初需要传行号，区分子表数据，不然多条子表时会被覆盖成一条，业务单据台账会有问题，需上送billLineId字段
            batchTransferAccountJson.put(BILL_LINE_ID, batchTransferAccountB.getId().toString());
            batchTransferAccountBJson.entrySet().forEach(e -> {
                 // 为区分单据的主子表区域的单据字段（尤其是单据主表和单据子表使用相同编码的数据）；主表数据直接使用单据字段做key值，子表数据需要使用：主子实体在领域元数据中定义的组合关系中的roleA的值.子表单据字段作为key（ls：通用报销单子表_费用承担部门: expensebillbsvfinacedeptid_name; 报销单费用分摊费用承担部门: expapportions_vfinacedeptid_name）。
                 //说明：采用__(双下划线)链接子表区域标识和单据字段。
                batchTransferAccountJson.put(BATCHTRANSFERACCOUNT_B + e.getKey(), e.getValue());
                Map subCharacterDefMap = batchTransferAccountB.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> batchTransferAccountJson.put(BATCHTRANSFERACCOUNT_B + CHARACTERDEFB_ + key, value));
                }
            });
            //规则id，从推送的消息中获取
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.RULE_ID, budgetEventBill.getRuleId());
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.RULECTRL_ID, budgetEventBill.getRuleCtrlId());
            //规则类型 (预占pre；执行implement) 从推送的消息中获取
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.RULE_TYPE, budgetEventBill.getRuleType());
            //单据类型 可从推送消息中获取，也可以直接写成自己对接的单据类型
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.BILL_CODE, budgetEventBill.getBillCode());
            //交易类型
            String bustype = batchTransferAccount.getTradeType();
            if(butyCode.containsKey(bustype)){
                batchTransferAccountJson.put(CmpBudgetCommonManagerService.TRANSAC_CODE, butyCode.get(bustype));
            }else{
                String code = cmCommonService.getDefaultTransTypeCode(bustype);
                butyCode.put(bustype,code);
                batchTransferAccountJson.put(CmpBudgetCommonManagerService.TRANSAC_CODE, code);
            }
            //单据id
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.BILL_ID, batchTransferAccountB.getMainid());
            //单据编号
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.BILL_BUS_CODE, batchTransferAccount.getCode());
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.LINE, batchTransferAccountB.getLineno().intValue());
            batchTransferAccountJson.put(CmpBudgetCommonManagerService.SERVICE_CODE, IServicecodeConstant.BATCH_TRANSFERACCOUNT);
            array.add(batchTransferAccountJson);
        }
        log.error("同名账户批量划转预算期初推送报文：{}", CtmJSONObject.toJSONString(array));
        return array;
    }

    /**
     * 获取同名账户批量划转预算期初-实占
     * @param querySchemaAp
     * @param querySchemaB
     * @param batchTransferAccountMap
     * @param budgetEventBill
     * @return
     * @throws Exception
     */
    private List<BatchTransferAccount_b> getBillDateOfImplement(QuerySchema querySchemaAp, QuerySchema querySchemaB, Map<String, BatchTransferAccount> batchTransferAccountMap, CmpBudgetEventBill budgetEventBill) throws Exception {
        if (!IMPLEMENT.equals(budgetEventBill.getRuleType())) {
            return new ArrayList<>();
        }
        // 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单
        querySchemaAp.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
        querySchemaB.appendQueryCondition(QueryCondition.name(PAYSETTLESTATUS).in(SettleDetailSettleStateEnum.SETTLE_SUCCESS.getValue(),SettleDetailSettleStateEnum.PART_SUCCESS.getValue()));
        // 主表List集合
        List<BatchTransferAccount> list = MetaDaoHelper.queryObject(BatchTransferAccount.ENTITY_NAME, querySchemaAp,null);
        // 主表id集合
        List<String> ids = new ArrayList();
        list.forEach(e ->{
            ids.add(e.getId().toString());
            batchTransferAccountMap.put(e.getId().toString(), e);
        });
        if (ids.size() == 0) {
            return new ArrayList<>();
        }
        querySchemaB.appendQueryCondition(QueryCondition.name("mainid").in(ids));
        return MetaDaoHelper.queryObject(BatchTransferAccount_b.ENTITY_NAME, querySchemaB,null);
    }

    /**
     * 获取同名账户批量划转预算期初-预占
     * 启用审批流单据，审批流状态为审批中/审批完成并且付款明细结算状态等于结算中/待结算
     * 未启用审批流单据，审批流状态为审批完成并且付款明细结算状态等于结算中/待结算
     */
    private List<BatchTransferAccount_b> getBillDataOfPre(QuerySchema querySchemaAp, QuerySchema querySchemaAi, QuerySchema querySchemaB, Map<String, BatchTransferAccount> batchTransferAccountMap, CmpBudgetEventBill budgetEventBill) throws Exception {
        if (!"pre".equals(budgetEventBill.getRuleType())) {
            return new ArrayList<>();
        }
        // 审批通过-主表单据
        querySchemaAp.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
        log.error("getBillDataOfPre#querySchemaAp:{}",CtmJSONObject.toJSONString(querySchemaAp));
        List<BatchTransferAccount> listAp = MetaDaoHelper.queryObject(BatchTransferAccount.ENTITY_NAME, querySchemaAp,null);
        // 主表id集合-审批通过
        List<String> idsAp = new ArrayList<>();
        listAp.forEach(e ->{
            idsAp.add(e.getId().toString());
            batchTransferAccountMap.put(e.getId().toString(), e);
        });
        // 审批中-主表单据
        querySchemaAi.appendQueryCondition(QueryCondition.name(VERIFYSTATE).eq(VerifyState.SUBMITED.getValue()));
        log.error("getBillDataOfPre#querySchemaAi:{}",CtmJSONObject.toJSONString(querySchemaAi));
        List<BatchTransferAccount> batchTransferAccountList = MetaDaoHelper.queryObject(BatchTransferAccount.ENTITY_NAME, querySchemaAi,null);
        List<String> idsAi = new ArrayList<>();
        batchTransferAccountList.forEach(e ->{
            idsAi.add(e.getId().toString());
            batchTransferAccountMap.put(e.getId().toString(), e);
        });
        if (CollectionUtils.isNotEmpty(idsAp) && CollectionUtils.isNotEmpty(idsAi)) {
            return new ArrayList<>();
        }
        QueryConditionGroup queryConditionGroupOr = new QueryConditionGroup(ConditionOperator.or);
        QueryConditionGroup queryConditionGroupAp = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup queryConditionGroupAi = new QueryConditionGroup(ConditionOperator.and);
        // 主表审批通过，且子表结算状态为结算中/待结算
        if (idsAp.size() > 0) {
            queryConditionGroupAp.addCondition(QueryCondition.name("mainid").in(idsAp));
            queryConditionGroupAp.addCondition(QueryCondition.name(PAYSETTLESTATUS).in(SettleDetailSettleStateEnum.WAIT_SETTLE.getValue(),SettleDetailSettleStateEnum.SETTLING.getValue()));
            queryConditionGroupOr.addCondition(queryConditionGroupAp);
        }
        // 主表审批中，不包含退票
        if (idsAi.size() > 0) {
            queryConditionGroupAi.addCondition(QueryCondition.name("mainid").in(idsAi));
            queryConditionGroupAi.addCondition(QueryCondition.name(PAYSETTLESTATUS).not_in(SettleDetailSettleStateEnum.BACK.getValue()));
            queryConditionGroupOr.addCondition(queryConditionGroupAi);
        }
        querySchemaB.appendQueryCondition(queryConditionGroupOr);
        log.error("getBillDataOfPre querySchemaB:{}",CtmJSONObject.toJSONString(querySchemaB));
        return MetaDaoHelper.queryObject(BatchTransferAccount_b.ENTITY_NAME, querySchemaB,null);
    }

    /**
     * 构建查询条件
     * @param querySchemaAp
     * @param querySchemaAi
     * @param querySchemaB
     * @param budgetEventBill
     */
    private void buildQuerySchema(QuerySchema querySchemaAp, QuerySchema querySchemaAi, QuerySchema querySchemaB, CmpBudgetEventBill budgetEventBill) {
        CtmJSONObject billfieldValues= CtmJSONObject.toJSON(budgetEventBill.getBillfieldValues());
        //2 添加控制过滤条件
        if (billfieldValues != null) {
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
                        if (str.contains(BATCHTRANSFERACCOUNT_B)) {
                            // 子表过滤字段
                            querySchemaB.appendQueryCondition(QueryCondition.name(str.replace(BATCHTRANSFERACCOUNT_B, "")).in(bfAValuesTA));
                        } else {
                            querySchemaAp.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                            querySchemaAi.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                        }
                    }
                }
            }
        }
        //3 单据时间key为billTime，根据billTime去查询startTime和endTime之间的数据
        if (budgetEventBill.getBillTime() != null && budgetEventBill.getBillTime().contains(BATCHTRANSFERACCOUNT_B)) {
            // 子表时间字段
            querySchemaB.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime().replace(BATCHTRANSFERACCOUNT_B, "")).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        } else {
            querySchemaAp.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
            querySchemaAi.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        }
    }

    /**
     * 更新列表预算状态
     * @param batchTransferAccountBs
     * @param isOccupyBudget
     * @throws Exception
     */
    @Override
    public void updateBillList(List<BatchTransferAccount_b> batchTransferAccountBs, Short isOccupyBudget) throws Exception {
        if (CollectionUtils.isEmpty(batchTransferAccountBs)) {
            return;
        }
        log.error("batchtransferaccount#updateBillList:{},isOccupyBudget:{} ", CtmJSONObject.toJSONString(batchTransferAccountBs), isOccupyBudget);
        batchTransferAccountBs.forEach(item -> {
            BatchTransferAccount_b update = new BatchTransferAccount_b();
            update.setId(item.getId());
            update.setIsOccupyBudget(isOccupyBudget);
            update.setEntityStatus(EntityStatus.Update);
            try {
                MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, update);
            } catch (Exception e) {
                log.error("更新同名账户批量划转子表异常:{}",e.getMessage(),e);
                throw new CtmException(e.getMessage());
            }
        });
    }


    /**
     * 报文拼接
     *
     * @param batchTransferAccount
     * @param addOrReduce 撤回需要删除
     */
    private Map<String, Object> initBatchTransferAccountBill(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB, int addOrReduce, String preemptionOrExecFlag, String billAction) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(batchTransferAccount);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.BATCH_TRANSFERACCOUNT);
        jo.put("billId", ValueUtils.isNotEmptyObj(batchTransferAccount.getId()) ? batchTransferAccount.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", batchTransferAccount.getCode());//单据编号
        jo.put("billCode", IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);//单据类型唯一标识
        jo.put("action", billAction);//动作
        jo.put("requsetbilltradetype", batchTransferAccount.getTradeType());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", ValueUtils.isNotEmptyObj(batchTransferAccountB.getId()) ? batchTransferAccountB.getId() : ymsOidGenerator.nextId());//id，用来拼接unquieRequestId
        String headPubts = ValueUtils.isNotEmptyObj(batchTransferAccount.get("pubts")) ? batchTransferAccount.get("pubts").toString() : new Date().toString();
        String subPubts = ValueUtils.isNotEmptyObj(batchTransferAccountB.get("pubts")) ? batchTransferAccountB.get("pubts").toString() : new Date().toString();
        String requsetbillpubts = headPubts + "|" + subPubts;
        jo.put("requsetbillpubts", requsetbillpubts);
        jo.put("transacCode", cmCommonService.getDefaultTransTypeCode(batchTransferAccount.getTradeType()));//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(batchTransferAccountB.getLineno())
                ? batchTransferAccountB.getLineno().intValue()
                : ymsOidGenerator.nextId());//单据行id 默认1
        jo.put("billLineId", batchTransferAccountB.getId());
        jo.put("lineRequestUniqueId", batchTransferAccountB.getId());
        // 特征字段拼接 例：characterDef_test0919
        if (batchTransferAccount.get("characterDef") != null && batchTransferAccount.get("characterDef") instanceof Map) {
            Map characterDefMap = batchTransferAccount.get("characterDef");
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
        if (batchTransferAccountB != null) {
            CtmJSONObject jsonObject_b = toJsonObj(batchTransferAccountB);
            jsonObject_b.entrySet().stream().forEach(e -> {
                // 签名字段不用传预算，且包含特殊字符已被预算拦截校验
                if (!e.getKey().contains("signature")) {
                    jo.put(BATCHTRANSFERACCOUNT_B + e.getKey(), e.getValue());
                }
            });
            if (batchTransferAccountB.get("characterDefb") instanceof Map) {
                Map subCharacterDefMap = batchTransferAccountB.get("characterDefb");
                if (subCharacterDefMap != null && !subCharacterDefMap.isEmpty()) {
                    subCharacterDefMap.forEach((key, value) -> jo.put(BATCHTRANSFERACCOUNT_B + CHARACTERDEFB + key, value));
                }
            }
        }
        return jo;
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
     * 获取预算数据
     *
     * @param billNum
     */
    @Override
    public Map[] getBudgetBills(String billNum, Long id) throws Exception {
        if (!IBillNumConstant.CMP_BATCHTRANSFERACCOUNT.equals(billNum)) {
            return null;
        }
        BatchTransferAccount batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, id);
        int addOrReduce = 0;//新增
        //查询预占单据
        List<BatchTransferAccount_b> batchTransferAccountBs = batchTransferAccount.BatchTransferAccount_b();
        Map[] bills = new Map[batchTransferAccountBs.size()];
        String billAction = BillAction.SUBMIT;
        String preemptionOrExecFlag = "";
        for (int i = 0; i < batchTransferAccountBs.size(); i++) {
            BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(i);
            if (batchTransferAccountB.getIsOccupyBudget() != null && batchTransferAccountB.getIsOccupyBudget() == OccupyBudget.PreSuccess.getValue()) {
                preemptionOrExecFlag = PRE;
            } else if (batchTransferAccountB.getIsOccupyBudget() != null && batchTransferAccountB.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
                preemptionOrExecFlag = IMPLEMENT;
            } else {
                billAction = BillAction.QUERY;
            }
            Map jo = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountB, addOrReduce, preemptionOrExecFlag, billAction);
            bills[i] = jo;
        }
        return bills;
    }

    @Override
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();
        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)) {
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if (ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())) {
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400627", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(changeBillno)) {
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(bizObjects)) {
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400625", "变更金额小于原金额,不需要校验!") /* "变更金额小于原金额,不需要校验!" */));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum = changeBillno;
        } else {
            //非变更单据 自己单据
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(bizObjects)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400627", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetManagerService.budgetCheckBatchTransferAccount(bizObjects, billnum, BudgetUtils.SUBMIT);
    }

    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        // 查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("BatchTransferAccount_b").addSelect("*");
        schema.addCompositionSchema(detailSchema);
        return MetaDaoHelper.queryObject(BatchTransferAccount.ENTITY_NAME, schema, null);
    }

    /**
     * 提交占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget submitOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置提交的占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);

        // 提交既配置预占又配置执行
        if (preAndImplList.contains(PRE) && preAndImplList.contains(IMPLEMENT)) {
            if (isUnOccupyBudget(batchTransferAccountB)) {
                // 直接执行
                return implementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
            }
        } else if (preAndImplList.contains(PRE)) {
            if (isUnOccupyBudget(batchTransferAccountB)) {
                // 只包括预占
                return preBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
            }
        } else {
            // 预占成功
            if (isPreOccupyBudget(batchTransferAccountB)) {
                // 释放预占转执行
                return implementPreBudget(batchTransferAccount,batchTransferAccountBs, action, budgetAction);
            } else if (isUnOccupyBudget(batchTransferAccountB)) {
                // 直接执行
                return implementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
            }
        }

        return resultBudget;
    }

    /**
     * 判断是否未占用
     * @param batchTransferAccountB
     * @return
     */
    private boolean isUnOccupyBudget(BatchTransferAccount_b batchTransferAccountB) {
        return batchTransferAccountB.getIsOccupyBudget() == null
                || OccupyBudget.UnOccupy.getValue() == batchTransferAccountB.getIsOccupyBudget();
    }

    /**
     * 判断是否预占成功
     * @param batchTransferAccountB
     * @return
     */
    private boolean isPreOccupyBudget(BatchTransferAccount_b batchTransferAccountB) {
        return batchTransferAccountB.getIsOccupyBudget() != null && OccupyBudget.PreSuccess.getValue() == batchTransferAccountB.getIsOccupyBudget();
    }

    /**
     * 判断是否执行成功
     * @param batchTransferAccountB
     * @return
     */
    private boolean isImpOccupyBudget(BatchTransferAccount_b batchTransferAccountB) {
        return batchTransferAccountB.getIsOccupyBudget() != null && OccupyBudget.ActualSuccess.getValue() == batchTransferAccountB.getIsOccupyBudget();
    }

    /**
     * 撤回释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget unsubmitOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置提交的占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        // 提交既配置预占又配置执行
        if (preAndImplList.contains(PRE) && preAndImplList.contains(IMPLEMENT)) {
            if (isImpOccupyBudget(batchTransferAccountB)) {
                // 直接释放执行转预占
                return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
            }
        } else if (preAndImplList.contains(PRE)) {
            if (isPreOccupyBudget(batchTransferAccountB)) {
                // 释放预占
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
            }
        } else {
            // 是否配置预占
            List<String> preList = opportunityMap.get(PRE);
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preList)) {
                if (isImpOccupyBudget(batchTransferAccountB)) {
                    // 释放执行转预占
                    return releaseImplementPreBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
                }
            } else {
                // 释放执行
                if (isImpOccupyBudget(batchTransferAccountB)) {
                    return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
                }
            }
        }
        return resultBudget;
    }

    /**
     * 结算止付释放执行
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget stopSettleOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String actionSave = actionArray[0];
        String actionSub = actionArray[1];
        String actionAud = actionArray[2];
        String actionSuc = actionArray[3];
        String budgetAction = actionArray[4];
        List<String> preAndImplListSave = opportunityMap.get(actionSave);
        List<String> preAndImplListSub = opportunityMap.get(actionSub);
        List<String> preAndImplListAud = opportunityMap.get(actionAud);
        List<String> preAndImplListSuc = opportunityMap.get(actionSuc);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListSub) && org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListAud)
                && org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListSuc)
                && org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListSave)) {
            // 未配置提交或审核或结算成功的占用时机
            return resultBudget;
        }
        BatchTransferAccount_b  batchTransferAccountB = batchTransferAccountBs.get(0);
        if (isImpOccupyBudget(batchTransferAccountB)) {
            // 结算止付释放执行
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListSub) && preAndImplListSub.contains(IMPLEMENT)) {
                // 直接释放执行
                return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionSub, budgetAction);
            } else if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListAud) && preAndImplListAud.contains(IMPLEMENT)) {
                return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionAud, budgetAction);
            } else if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListSuc) && preAndImplListSuc.contains(IMPLEMENT)) {
                return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionSuc, budgetAction);
            }
        } else if (isPreOccupyBudget(batchTransferAccountB)) {
            // 结算止付释放预占
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListSave) && preAndImplListSave.contains(PRE)) {
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, actionSave, budgetAction);
            } else if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListSub) && preAndImplListSub.contains(PRE)) {
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, actionSub, budgetAction);
            } else if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListAud)) {
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, actionAud, budgetAction);
            } else if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preAndImplListSuc)) {
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, actionSuc, budgetAction);
            }
        }
        return resultBudget;
    }


    /**
     * 审核占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget auditOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置保存的占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        // 执行配置审核
        if (preAndImplList.contains(IMPLEMENT)) {
            if (isPreOccupyBudget(batchTransferAccountB)) {
                // 释放预占转执行
                return implementPreBudget(batchTransferAccount,batchTransferAccountBs, action, budgetAction);
            } else if (isUnOccupyBudget(batchTransferAccountB)) {
                // 直接执行
                return implementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
            }
        }
        return resultBudget;
    }

    /**
     * 审批撤回释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget unauditOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String actionAud = actionArray[0];
        String actionSave = actionArray[1];
        String actionSub = actionArray[2];
        String budgetAction = actionArray[3];
        List<String> preAndImplListAud = opportunityMap.get(actionAud);
        List<String> preAndImplListSave = opportunityMap.get(actionSave);
        List<String> preAndImplListSub = opportunityMap.get(actionSub);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListAud)
            && org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListSave)
            && org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplListSub)) {
            // 未配置保存、提交、审核的占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        // 执行成功
        if (isImpOccupyBudget(batchTransferAccountB)) {
            // 是否配置预占
            List<String> preList = opportunityMap.get(PRE);
            // 配置了预占
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preList)) {
                // 审批配置了执行
                if (CollectionUtils.isNotEmpty(preAndImplListAud) && preAndImplListAud.contains(IMPLEMENT)) {
                    // 审批终止 释放执行
                    if (BillAction.TERMINATE.equals(budgetAction)) {
                        return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionAud, budgetAction);
                    } else {
                        // 释放执行转预占
                        return releaseImplementPreBudget(batchTransferAccount, batchTransferAccountBs, actionAud, budgetAction);
                    }
                } else if (CollectionUtils.isNotEmpty(preAndImplListSub) && preAndImplListSub.contains(IMPLEMENT)) {
                    // 审批终止 释放执行
                    if (BillAction.TERMINATE.equals(budgetAction)) {
                        return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionSub, budgetAction);
                    } else {
                        // 提交配置了执行
                        return releaseImplementPreBudget(batchTransferAccount, batchTransferAccountBs, actionSub, budgetAction);
                    }
                }
            } else {
                // 审批配置了执行
                if (CollectionUtils.isNotEmpty(preAndImplListAud) && preAndImplListAud.contains(IMPLEMENT)) {
                    // 释放执行
                    return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionAud, budgetAction);
                } else if (CollectionUtils.isNotEmpty(preAndImplListSub) && preAndImplListSub.contains(IMPLEMENT)) {
                    // 提交配置了执行
                    return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, actionSub, budgetAction);
                }
            }
        } else if (isPreOccupyBudget(batchTransferAccountB)) {
            // 预占成功
            // 预占的时机为保存
            if (CollectionUtils.isNotEmpty(preAndImplListSave) && preAndImplListSave.contains(PRE)) {
                // 释放执行
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, actionSave, budgetAction);
            } else if (CollectionUtils.isNotEmpty(preAndImplListSub) && preAndImplListSub.contains(PRE)) {
                // 预占的时机为提交
                return releasePreBudget(batchTransferAccount, batchTransferAccountBs, actionSub, budgetAction);
            }
        }
        return resultBudget;
    }

    /**
     * 结算成功占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget settleSuccessOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置结算成功占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        // 执行配置结算成功
        if (preAndImplList.contains(IMPLEMENT)) {
            List<String> preList = opportunityMap.get(PRE);
            // 直接执行
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preList)) {
                if (isPreOccupyBudget(batchTransferAccountB)) {
                    // 释放预占转执行
                    return implementPreBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
                }
            } else  {
                if (isUnOccupyBudget(batchTransferAccountB)) {
                    // 直接执行
                    return implementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
                }
            }
        }
        return resultBudget;
    }

    /**
     * 取消结算释放预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget cancelSettleOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置结算成功的占用时机
            return resultBudget;
        }
        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        // 执行配置结算成功
        if (preAndImplList.contains(IMPLEMENT)) {
            // 是否配置预占
            List<String> preList = opportunityMap.get(PRE);
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(preList)) {
                if (isImpOccupyBudget(batchTransferAccountB)) {
                    // 释放执行转预占
                    return releaseImplementPreBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
                }
            } else {
                if (isImpOccupyBudget(batchTransferAccountB)) {
                    // 释放执行
                    return releaseImplementBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
                }
            }
        }
        return resultBudget;
    }

    /**
     * 保存占用预算
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget saveOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置保存的占用时机
            return resultBudget;
        }
        if (!preAndImplList.contains(PRE)) {
            // 未配置保存的占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        if (isUnOccupyBudget(batchTransferAccountB)) {
            // 预占
            return preBudget(batchTransferAccount,batchTransferAccountBs, action, budgetAction);
        } else if (isPreOccupyBudget(batchTransferAccountB)) {
            // 释放预占
            return releasePreBudget(batchTransferAccount,batchTransferAccountBs, action, budgetAction);
        }
        return resultBudget;
    }

    /**
     * 删除释放预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    @Override
    public ResultBudget deleteOccupyBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String billAction) throws Exception {
        ResultBudget resultBudget = new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
        Map<String,List<String>> opportunityMap = getBudgetOpportunity(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
        String[] actionArray =  billAction.split(":");
        String action = actionArray[0];
        String budgetAction = actionArray[1];
        List<String> preAndImplList = opportunityMap.get(action);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(preAndImplList)) {
            // 未配置保存的占用时机
            return resultBudget;
        }
        if (!preAndImplList.contains(PRE)) {
            // 未配置保存的占用时机
            return resultBudget;
        }

        BatchTransferAccount_b batchTransferAccountB = batchTransferAccountBs.get(0);
        if (isPreOccupyBudget(batchTransferAccountB)) {
            // 释放预占
            return releasePreBudget(batchTransferAccount, batchTransferAccountBs, action, budgetAction);
        }
        return resultBudget;
    }


    /**
     * 根据业务系统注册页面配置的预占时机和执行时机
     *
     * @param billCode 单据编码
     * @return
     */
    private Map<String,List<String>> getBudgetOpportunity(String billCode) throws Exception {
        Map<String,List<String>> opportunityMap = new HashMap<>();
        // 查询接口
        List<Map<String,Object>> billActions = (List<Map<String,Object>>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
        if (CollectionUtils.isEmpty(billActions)) {
            log.error("getBillAction error");
            return opportunityMap;
        }
        //获取平台配置
        Map<String,Object> billAction = billActions.get(0);
        String pre = billAction.get(PRE) == null ? null : (String)billAction.get(PRE);
        String implement = billAction.get(IMPLEMENT) == null ? null : (String)billAction.get(IMPLEMENT);
        if (StringUtils.isNotEmpty(pre) && StringUtils.isNotEmpty(implement)) {
            if (pre.equals(implement)) {
                opportunityMap.put(pre, Arrays.asList(PRE,IMPLEMENT));
            } else {
                opportunityMap.put(pre, Collections.singletonList(PRE));
                opportunityMap.put(implement, Collections.singletonList(IMPLEMENT));
            }
        } else if (StringUtils.isNotEmpty(pre)) {
            opportunityMap.put(pre, Collections.singletonList(PRE));
        } else if (StringUtils.isNotEmpty(implement)) {
            opportunityMap.put(implement, Collections.singletonList(IMPLEMENT));
        }
        if (StringUtils.isNotEmpty(pre)) {
            opportunityMap.put(PRE, Collections.singletonList(pre));
        }
        opportunityMap.put(IMPLEMENT, Collections.singletonList(implement));
        return opportunityMap;
    }


    /**
     * 预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    private ResultBudget preBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            int size = batchTransferAccountBs.size();
            Map<String, Object>[] bills = new Map[size];
            for (int i = 0; i < size; i++) {
                Map copiedMap = new HashMap<>(initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.ADD.getIndex(), PRE, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            log.error("同名账户批量划转预算预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOS[0]));
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("同名账户批量划转预算预占响应报文:{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            resultBudget.setBudgeted(OccupyBudget.PreSuccess.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转预算预占error:{} ", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }



    /**
     * 释放预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param billAction
     * @return
     */
    private ResultBudget releasePreBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            // 生成参数
            Map<String, Object>[] bills = new Map[batchTransferAccountBs.size()];
            for (int i = 0; i < batchTransferAccountBs.size(); i++) {
                Map copiedMap = new HashMap<>(initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.REDUCE.getIndex(), PRE, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            log.error("同名账户批量划转预算释放预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOS));
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("同名账户批量划转预算释放预占响应报文:{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            resultBudget.setBudgeted(OccupyBudget.UnOccupy.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转预算释放预占error: {}", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


    /**
     * 执行-未预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param action
     * @param billAction
     * @return
     */
    private ResultBudget implementBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            // 未配置预占动作，则无需释放预占，直接实占
            // 生成参数
            Map<String, Object>[] bills = new Map[batchTransferAccountBs.size()];
            for (int i = 0; i < batchTransferAccountBs.size(); i++) {
                Map copiedMap = new HashMap<>(initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.ADD.getIndex(), IMPLEMENT, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOs = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            log.error("同名账户批量划转预算执行请求报文：{} ", CtmJSONObject.toJSONString(controlDetailVOs));
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOs));
            log.error("同名账户批量划转预算执行响应报文：{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            // 实占成功
            resultBudget.setBudgeted(OccupyBudget.ActualSuccess.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转execute error:{} ",e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 执行释放预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param action
     * @param billAction
     * @return
     */
    private ResultBudget implementPreBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            String preAction;
            int size = batchTransferAccountBs.size();
            Map[] preBills = new Map[size];// 预占一条数据，实占一条数据
            Map[] implementBills = new Map[size];
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
            log.error("同名账户批量划转查询预算的BillAction:{}", CtmJSONObject.toJSONString(objects));
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                preAction = (String) objects.get(0).get(PRE);
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 未配置预占动作，则无需预占，直接释放实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO[] controlDetailVOPre;
            ControlDetailVO[] controlDetailVOImplement;
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.REDUCE.getIndex(), PRE, billAction);
                preBills[i] = bill;
            }
            controlDetailVOPre = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, preAction, 0);
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.ADD.getIndex(), IMPLEMENT, billAction);
                implementBills[i] = bill;
            }
            controlDetailVOImplement = cmpBudgetCommonManagerService.initControlDetailVOs(implementBills, action, 0);
            List<ControlDetailVO> controlDetailVOSList = new ArrayList<>();
            controlDetailVOSList.addAll(Arrays.asList(controlDetailVOPre));
            controlDetailVOSList.addAll(Arrays.asList(controlDetailVOImplement));
            controlDetailVOS = controlDetailVOSList.toArray(new ControlDetailVO[0]);
            log.error("同名账户批量划转预算-执行释放预占-释放预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOPre));
            log.error("同名账户批量划转预算-执行释放预占-执行请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOImplement));
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("同名账户批量划转预算释放实占响应报文：{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            resultBudget.setBudgeted(OccupyBudget.ActualSuccess.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转释放预占 execute error:{} ", e.getMessage(), e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400624", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


    /**
     *  释放执行-未预占
      * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param action
     * @param billAction
     * @return
     */
    private ResultBudget releaseImplementBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            // 生成参数
            Map<String, Object>[] bills = new Map[batchTransferAccountBs.size()];
            for (int i = 0; i < batchTransferAccountBs.size(); i++) {
                Map copiedMap = new HashMap<>(initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.REDUCE.getIndex(), IMPLEMENT, billAction));
                bills[i] = copiedMap;
            }
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            log.error("同名账户批量划转预算释放执行请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOS));
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("同名账户批量划转预算释放执行响应报文:{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            resultBudget.setBudgeted(OccupyBudget.UnOccupy.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转预算释放预占error: {}", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     *  释放执行-预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param action
     * @param billAction
     * @return
     */
    private ResultBudget releaseImplementPreBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            String preAction;
            int size = batchTransferAccountBs.size();
            Map[] preBills = new Map[size];// 预占一条数据，实占一条数据
            Map[] implementBills = new Map[size];
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, IBillNumConstant.CMP_BATCHTRANSFERACCOUNT);
            log.error("同名账户批量划转查询预算的BillAction:{}", CtmJSONObject.toJSONString(objects));
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                preAction = (String) objects.get(0).get(PRE);
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 未配置预占动作，则无需预占，直接释放实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO[] controlDetailVOPre;
            ControlDetailVO[] controlDetailVOImplement;
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.ADD.getIndex(), PRE, billAction);
                preBills[i] = bill;
            }
            controlDetailVOPre = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, preAction, 0);
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.REDUCE.getIndex(), IMPLEMENT, billAction);
                implementBills[i] = bill;
            }
            controlDetailVOImplement = cmpBudgetCommonManagerService.initControlDetailVOs(implementBills, action, 0);
            List<ControlDetailVO> controlDetailVOSList = new ArrayList<>();
            controlDetailVOSList.addAll(Arrays.asList(controlDetailVOPre));
            controlDetailVOSList.addAll(Arrays.asList(controlDetailVOImplement));
            controlDetailVOS = controlDetailVOSList.toArray(new ControlDetailVO[0]);
            log.error("同名账户批量划转-释放执行预占-预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOPre));
            log.error("同名账户批量划转-释放执行预占-释放执行请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOImplement));
            // 控制接口
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("同名账户批量划转-释放执行预占-预算释放实占响应报文：{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            resultBudget.setBudgeted(OccupyBudget.PreSuccess.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转释放预占 execute error:{} ", e.getMessage(), e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400624", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     *  释放预占再预占
     * @param batchTransferAccount
     * @param batchTransferAccountBs
     * @param action
     * @param billAction
     * @return
     */
    private ResultBudget preReleasePreBudget(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> batchTransferAccountBs, String action, String billAction) {
        try {
            int size = batchTransferAccountBs.size();
            Map[] releasePreBills = new Map[size];
            Map[] preBills = new Map[size];
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO[] controlDetailVOReleasePre;
            ControlDetailVO[] controlDetailVOPre;
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.REDUCE.getIndex(), PRE, billAction);
                releasePreBills[i] = bill;
            }
            controlDetailVOReleasePre = cmpBudgetCommonManagerService.initControlDetailVOs(releasePreBills, action, 0);
            for (int i = 0; i < size; i++) {
                Map<String, Object> bill = initBatchTransferAccountBill(batchTransferAccount, batchTransferAccountBs.get(i), BudgetDirect.ADD.getIndex(), PRE, billAction);
                preBills[i] = bill;
            }
            controlDetailVOPre = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, action, 0);
            List<ControlDetailVO> controlDetailVOSList = new ArrayList<>();
            controlDetailVOSList.addAll(Arrays.asList(controlDetailVOReleasePre));
            controlDetailVOSList.addAll(Arrays.asList(controlDetailVOPre));
            controlDetailVOS = controlDetailVOSList.toArray(new ControlDetailVO[0]);
            log.error("同名账户批量划转-释放预占再预占-释放预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOReleasePre));
            log.error("同名账户批量划转-释放预占再预占-预占请求报文：{}", CtmJSONObject.toJSONString(controlDetailVOPre));
            // 控制接口
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("同名账户批量划转-释放预占再预占-响应报文：{} ", CtmJSONObject.toJSONString(result));
            ResultBudget resultBudget = cmpBudgetManagerService.doResult(result);
            resultBudget.setBudgeted(OccupyBudget.PreSuccess.getValue());
            return resultBudget;
        } catch (Exception e) {
            log.error("同名账户批量划转释放预占 execute error:{} ", e.getMessage(), e);
            throw new CtmException(new CtmErrorCode("033-502-100234"), InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400624", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }


}
