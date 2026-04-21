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
import com.yonyoucloud.fi.cmp.budget.BudgetDirect;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetReceivemarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.RestTemplateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmpBudgetReceivemarginManagerServiceImpl implements CmpBudgetReceivemarginManagerService {
    @Value("${billdetail-url}")
    private String billDetailUrl;
    public static final String RECEIVEMARGIN = "ReceiveMargin__";
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private BusiSystemConfigService busiSystemConfigService;
    @Autowired
    private ExecdataControlService execdataControlService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Override
    public CtmJSONArray queryReceivemargin(CmpBudgetEventBill budgetEventBill) throws Exception{
        //1 查询指定时间内的单据
        QuerySchema querySchema0 = QuerySchema.create().addSelect("*");// 审批通过
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");// 审批中
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
                        if (str.contains(cmpBudgetCommonManagerService.CHARACTERDEF_)) {
                            // 特征字段特殊处理
                            str = str.replace(cmpBudgetCommonManagerService.CHARACTERDEF_, cmpBudgetCommonManagerService.CHARACTERDEF);
                        }
                        if (str.contains(cmpBudgetCommonManagerService.CHARACTERDEFB_)) {
                            // 子表特征字段特殊处理
                            str = str.replace(cmpBudgetCommonManagerService.CHARACTERDEFB_, cmpBudgetCommonManagerService.CHARACTERDEFB);
                        }
                         else {
                            querySchema0.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                            querySchema1.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                        }
                    }
                }
            }
        }
        //3 单据时间key为billTime，根据billTime去查询startTime和endTime之间的数据
        querySchema0.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        querySchema1.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        String ruleType = budgetEventBill.getRuleType();
        String transacId = budgetEventBill.getTransacId();
        if (StringUtils.isNotEmpty(transacId)) {
            querySchema0.appendQueryCondition(QueryCondition.name("tradetype").eq(transacId));
            querySchema1.appendQueryCondition(QueryCondition.name("tradetype").eq(transacId));
        }
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
        List<ReceiveMargin> receiveMargins = new ArrayList<>();
        querySchema0.appendQueryCondition(QueryCondition.name("settlestatus").not_eq(FundSettleStatus.SettleFailed.getValue()));//结算止付 不占预算和不占实占
        if ("pre".equals(ruleType)) {
            // TODO 目前触发时机为写死的，提交审批流预置，结算成功实占；后续如果根据配置时机占用，需保留下面逻辑
            /**
             * 启用审批流单据，审批流状态为审批中/审批完成并且付款明细结算状态等于结算中/待结算
             * 未启用审批流单据，审批流状态为审批完成并且付款明细结算状态等于结算中/待结算
             */
            // 审批通过-主表单据 // 审批中-主表单据
            QueryConditionGroup group1 = QueryConditionGroup.and(
                    QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.SUBMITED.getValue()));
            QueryConditionGroup group2 = QueryConditionGroup.and(
                    QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()),
                    QueryCondition.name("settlesuccesstime").is_null());
            QueryConditionGroup or = QueryConditionGroup.or(group1, group2);
            querySchema0.appendQueryCondition(or);
            List<ReceiveMargin> list0 = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema0, null);
            receiveMargins = list0;
            updateReceivemargin(list0, OccupyBudget.PreSuccess.getValue());
        } else if ("implement".equals(ruleType)) {
            // 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单
            querySchema0.appendQueryCondition(QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
            //querySchema_b.appendQueryCondition(QueryCondition.name(SETTLESTATUS).in(FundSettleStatus.SettleSuccess.getValue(),FundSettleStatus.PartSuccess.getValue(),FundSettleStatus.Refund.getValue(),FundSettleStatus.SettlementSupplement.getValue()));
            // 主表List集合
            List<ReceiveMargin> list = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema0,null);
            receiveMargins = list;
            updateReceivemargin(list, OccupyBudget.ActualSuccess.getValue());
        }
        // 数据查询完毕，准备拼装报文
        CtmJSONArray array = new CtmJSONArray();
        HashMap<String,String> butyCode = new HashMap<>();
        for (ReceiveMargin receiveMargin: receiveMargins) {
            CtmJSONObject receiveMarginJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(receiveMargin));
            Map characterDefMap = receiveMargin.get("characterDef");
            if (characterDefMap != null && !characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> receiveMarginJson.put(cmpBudgetCommonManagerService.CHARACTERDEF_ + key, value));
            }
            //规则id，从推送的消息中获取
            receiveMarginJson.put(cmpBudgetCommonManagerService.RULE_ID, budgetEventBill.getRuleId());
            receiveMarginJson.put(cmpBudgetCommonManagerService.RULECTRL_ID, budgetEventBill.getRuleCtrlId());
            //规则类型 (预占pre；执行implement) 从推送的消息中获取
            receiveMarginJson.put(cmpBudgetCommonManagerService.RULE_TYPE, budgetEventBill.getRuleType());
            //单据类型 可从推送消息中获取，也可以直接写成自己对接的单据类型
            receiveMarginJson.put(cmpBudgetCommonManagerService.BILL_CODE, budgetEventBill.getBillCode());
            //交易类型
            String bustype = receiveMargin.getTradetype();
            if(butyCode.containsKey(bustype)){
                receiveMarginJson.put(cmpBudgetCommonManagerService.TRANSAC_CODE, butyCode.get(bustype));
            }else{
                String code = cmCommonService.getDefaultTransTypeCode(bustype);
                butyCode.put(bustype,code);
                receiveMarginJson.put(cmpBudgetCommonManagerService.TRANSAC_CODE, code);
            }
            //单据id
            receiveMarginJson.put(cmpBudgetCommonManagerService.BILL_ID, receiveMargin.getId());
            //单据编号
            receiveMarginJson.put(cmpBudgetCommonManagerService.BILL_BUS_CODE, receiveMargin.getCode());
            receiveMarginJson.put(cmpBudgetCommonManagerService.LINE, receiveMargin.getId());
            receiveMarginJson.put(cmpBudgetCommonManagerService.BILL_LINE_ID, receiveMargin.getId());
            receiveMarginJson.put(cmpBudgetCommonManagerService.SERVICE_CODE, IServicecodeConstant.RECEIVEMARGIN);
            array.add(receiveMarginJson);
        }
        log.error("预算期初推送报文：" + array);
        return array;
    }

    @Override
    public void updateReceivemargin(List<ReceiveMargin> receiveMargins, Short isOccupyBudget) throws Exception {
        if (CollectionUtils.isEmpty(receiveMargins)) {
            return;
        }
        List<ReceiveMargin> updateList = new ArrayList<>();
        receiveMargins.stream().forEach(item -> {
            ReceiveMargin update = new ReceiveMargin();
            update.setId(item.getId());
            update.setIsOccupyBudget(isOccupyBudget);
            update.setEntityStatus(EntityStatus.Update);
            updateList.add(update);
        });
        if (CollectionUtils.isNotEmpty(updateList)) {
            MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, updateList);
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget gcExecuteTrueAudit(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction, boolean deleteBudget, boolean checkResult) throws Exception {
        try {
            log.error("gcExecuteTrueAudit...........");
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
            preAction = (String) objects.get(0).get(cmpBudgetCommonManagerService.PRE);
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVPre;
            ControlDetailVO controlDetailVImplement;
            if (!StringUtils.isEmpty(preAction) || deleteBudget) {
                Map preBill = initBill(bizObject, receiveMargin, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, BillAction.APPROVE_PASS);
                controlDetailVPre = cmpBudgetCommonManagerService.initControlDetailVO(preBill, preAction, 0);
                log.error("预算实占释放预占请求报文： " + controlDetailVPre);
                Map implementBill = initBill(bizObject, receiveMargin, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, BillAction.APPROVE_PASS);
                controlDetailVImplement = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                log.error("预算实占请求报文： " + controlDetailVImplement);
                controlDetailVOS = new ControlDetailVO[]{controlDetailVPre, controlDetailVImplement};
            } else {
                controlDetailVOS = new ControlDetailVO[1];
                Map implementBill = initBill(bizObject, receiveMargin, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, BillAction.APPROVE_PASS);
                controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                log.error("预算实占请求报文： " + controlDetailVOS[0]);
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算实占响应报文： ", CtmJSONObject.toJSONString(result));
            if (checkResult) {
                return cmpBudgetCommonManagerService.doResult(result);
            } else {
                return cmpBudgetCommonManagerService.doResultSoft(result);
            }
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget gcExecuteTrueUnAudit(BizObject bizObject,ReceiveMargin receiveMargin, String billCode, String billAction) throws Exception {
        try {
            log.error("gcExecuteTrueUnAudit...........");
            String preAction;
            String implementAction;
            Map[] preBills = new Map[1];// 预占一条数据，实占一条数据
            Map[] implementBills = new Map[1];
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                preAction = (String) objects.get(0).get(cmpBudgetCommonManagerService.PRE);
                implementAction = (String) objects.get(0).get("implement");
            } else {
                // 不能直接赋值implement，得看客户是否配置了实占规则，如果没配置需要直接返回
                log.error("getBillAction ", "not support budget implement! ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 未配置预占动作，则无需预占，直接释放实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVOPre;
            ControlDetailVO controlDetailVOImplement;
            if (!StringUtils.isEmpty(preAction)) {
                Map<String, Object> preBill = initBill(bizObject, receiveMargin, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, billAction);
                controlDetailVOPre = cmpBudgetCommonManagerService.initControlDetailVO(preBill, preAction, 0);
                Map<String, Object> implementBill = initBill(bizObject, receiveMargin, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, billAction);
                controlDetailVOImplement = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                controlDetailVOS = new ControlDetailVO[]{controlDetailVOPre, controlDetailVOImplement};
                log.error("预算释放实占占用预占请求报文：" + controlDetailVOPre);
                log.error("预算释放实占请求报文：" + controlDetailVOImplement);
            } else {
                controlDetailVOS = new ControlDetailVO[1];
                Map<String, Object> implementBill = initBill(bizObject, receiveMargin, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, billAction);
                controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                log.error("预算释放实占请求报文：" + controlDetailVOS[0]);
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放实占响应报文： ", CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget budget(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction) throws Exception {
        try {
            log.error("budget...............");
            // 审批流提交动作匹配预算动作
            String action;
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(cmpBudgetCommonManagerService.PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = cmpBudgetCommonManagerService.PRE;
            if (preemptionOrExecFlag == null) {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            // 拼装报文
            Map<String, Object> jo = initBill(bizObject, receiveMargin, BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction);
            ControlDetailVO[] controlDetailVOS = new ControlDetailVO[1];
            controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(jo, action, 0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 ", CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget releaseBudget(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction) throws Exception {
        try {
            log.error("releaseBudget...............");
            if (receiveMargin.getIsOccupyBudget() != null && receiveMargin.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String action;
            // 查询是否开启预算开关
            // 查询接口
            List<Map> objects = (List<Map>) busiSystemConfigService.getBillAction(InvocationInfoProxy.getTenantid(), BudgetUtils.SYSCODE, billCode);
            //获取平台配置
            if (CollectionUtils.isNotEmpty(objects)) {
                action = (String) objects.get(0).get(cmpBudgetCommonManagerService.PRE);
            } else {
                log.error("getBillAction ", "not support budget pre ");
                return new ResultBudget(OccupyBudget.UnOccupy.getValue(), false);
            }
            String preemptionOrExecFlag = cmpBudgetCommonManagerService.PRE;
            // 生成
            Map bill = initBill(bizObject, receiveMargin, BudgetDirect.REDUCE.getIndex(), billCode, preemptionOrExecFlag, billAction);
            ControlDetailVO[] controlDetailVOS = new ControlDetailVO[1];
            controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(bill, action, 0);
            // 控制接口(预占、实占)
            log.error("预算释放预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放预占响应报文： ", CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void executeSubmitDelete(ReceiveMargin receiveMargin) throws Exception {
        log.error("executeSubmitDelete.................");
        ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId());
        Short budgeted = receiveMarginNew.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            log.error("executeSubmitDelete,已经释放仍要释放，直接跳过不执行了");
            return;
        }
        ResultBudget resultBudget = releaseBudget(receiveMargin, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.CANCEL_SUBMIT);
        log.error("executeSubmitDelete,resultBudget:{}", resultBudget);
        if (resultBudget.isSuccess()) {
            ReceiveMargin update = new ReceiveMargin();
            update.setId(receiveMargin.getId());
            update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public boolean budgetSuccess(ReceiveMargin receiveMargin, boolean deleteBudget, boolean checkResult) throws Exception {
        ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId());
        if (receiveMargin.getIsOccupyBudget() != null && receiveMargin.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
            // 已经实占成功的单据，不再进行预算实占的接口调用
            log.error("budgetSuccess，已经实占成功的单据，不再进行预算实占的接口调用");
            return false;
        }
        // 执行
        ResultBudget resultBudget = gcExecuteTrueAudit(receiveMarginNew, receiveMargin, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.APPROVE_PASS, deleteBudget, checkResult);
        log.error("resultBudget:{}", resultBudget);
        return resultBudget.isSuccess();
    }

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initBill(BizObject bizObject, ReceiveMargin receiveMargin, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(bizObject);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.RECEIVEMARGIN);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billLineId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//本参数为String类型，值为单据行id，为必传参数，当所传单据中无子单据仅有主单据时，值应与billId相同
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//动作
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", jo.get("billId"));//id，用来拼接unquieRequestId
        jo.put("requsetbillpubts", ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ?bizObject.get("pubts").toString() : ymsOidGenerator.nextId());//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        Map characterDefMap = bizObject.get("characterDef");
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> jo.put(cmpBudgetCommonManagerService.CHARACTERDEF_ + key, value));
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (cmpBudgetCommonManagerService.PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else if (cmpBudgetCommonManagerService.IMPLEMENT.equals(preemptionOrExecFlag)) {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        return jo;
    }
    @Override
    public String queryBudgetDetail(CtmJSONObject json) {
        try {
            String billNum = json.getString("billno");
            Long id = json.getLong("id");
            if (id == null || StringUtils.isEmpty(billNum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100235"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_1811A1FC05B00042", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380012", "请求参数缺失") /* "请求参数缺失" */) /* "请求参数缺失" */);
            }
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, false);
            ReceiveMargin receiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, id);
            String preemptionOrExecFlag = "";
            String billAction = BillAction.SUBMIT;
            if (receiveMargin.getIsOccupyBudget() != null && receiveMargin.getIsOccupyBudget() == OccupyBudget.PreSuccess.getValue()) {
                preemptionOrExecFlag = cmpBudgetCommonManagerService.PRE;
            } else if (receiveMargin.getIsOccupyBudget() != null && receiveMargin.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
                preemptionOrExecFlag = cmpBudgetCommonManagerService.IMPLEMENT;
            } else {
                billAction = BillAction.QUERY;
            }
            String billCode = IBillNumConstant.CMP_RECEIVEMARGIN;
            int addOrReduce = 0;//新增
            Map[] bills = new Map[1];
            Map jo = initBill(receiveMargin, receiveMargin, addOrReduce, billCode, preemptionOrExecFlag, billAction);
            bills[0] = jo;
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
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100800"),result.getString("message"));
            }
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    @Override
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
                action = (String) objects.get(0).get(cmpBudgetCommonManagerService.PRE);
            } else {
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", "no isCanStart");
                return ResultMessage.data(resultBack);
            }
            //operateFlag 0代表控制并记录数据，1代表记录数据不控制，2控制不记录数据
            int operateFlag = 2;

            List<Map<String, Object>> bills = new ArrayList<>();
            for (BizObject bizObject : bizObjects) {
                Object verifystate = bizObject.get("verifystate");
                if (verifystate == null) {
                    verifystate = 0;
                }
                ReceiveMargin oldBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), 2);
                if (Short.parseShort(verifystate.toString()) == VerifyState.SUBMITED.getValue()) {
                    int index = 1;
                    Map<String, Object> bill = initBillCheck(bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, action, index);
                    bills.add(bill);
                    Map<String, Object> billReduce = initBillCheck(oldBill, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, action, index);
                    bills.add(billReduce);
                } else {
                    int index = 1;
                    Map<String, Object> bill = initBillCheck(bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, action, index);
                    bills.add(bill);
                }
            }
            Map[] preBills = bills.toArray(new Map[0]);
            ControlDetailVO[] controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(preBills, action, operateFlag);
            //4 控制接口(预占、实占)
            log.error("budgetCheck controlDetailVOS={}", CtmJSONObject.toJSONString(controlDetailVOS));
            Map map = execdataControlService.control(controlDetailVOS);
            String resultStr = CtmJSONObject.toJSONString(map);
            CtmJSONObject result = CtmJSONObject.parseObject(resultStr);
            log.error("execdataControlService.control, result={}", result.toString());
            cmpBudgetCommonManagerService.doCheckResult(resultBack, result);
            log.error("execdataControlService.doCheckResult, result={}", resultBack);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A2", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }
    /**
     * 财资公共-集成参数-支付保证金业务台账-预算控制为是时，调用预算业务单据控制接口，占用预算，占用类型=预占，接口调用成功后，更新是否占预算为预占成功。
     *
     * @param receiveMargin
     * @throws Exception
     */
    @Override
    public void executeSubmit(ReceiveMargin receiveMargin) throws Exception {
        Short budgeted = receiveMargin.getIsOccupyBudget();
        // 已经预占仍要预占或者已经实占，直接跳过不执行了
        if (budgeted != null && ((budgeted != OccupyBudget.UnOccupy.getValue()))) {
            log.error("executeSubmit，已经预占仍要预占或者已经实占，直接跳过不执行了");
            return;
        }
        ResultBudget resultBudget = budget(receiveMargin, receiveMargin, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.SUBMIT);
        if (resultBudget.isSuccess()) {//可能是没有匹配上规则，也可能是没有配置规则
            receiveMargin.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
            ReceiveMargin update = new ReceiveMargin();
            update.setId(receiveMargin.getId());
            update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
        }
    }

    @Override
    public void reMatchBudget(ReceiveMargin newBill) throws Exception {
        //1.判断是否是预占成功，如果是删除旧的预占，新增新的预占
        //2.判断是否是预占成功，如果否直接进行新的预占
        ReceiveMargin oldBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, newBill.getId(), 2);
        Short budgeted = oldBill.getIsOccupyBudget();
        if (budgeted == null || budgeted == OccupyBudget.UnOccupy.getValue()) {
            log.error("ReMatchBudget ，占用预算");
            executeSubmit(newBill);
        } else if (budgeted == OccupyBudget.PreSuccess.getValue()) {
            ResultBudget releaseBudget = releaseBudget(oldBill, oldBill, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.APPROVE_EDIT);
            if (releaseBudget.isSuccess()) {
                log.error("ReMatchBudget ，删除预算成功");
                newBill.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                ResultBudget resultBudget = budget(newBill, newBill, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.APPROVE_EDIT);
                if (resultBudget.isSuccess()) {
                    log.error("ReMatchBudget ，重新占用预算成功");
                    newBill.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                } else {
                    log.error("ReMatchBudget ，重新占用预算失败,resultBudget：{}", resultBudget);
                }
            } else {
                log.error("ReMatchBudget ，删除预算失败,releaseBudget：{}", releaseBudget);
            }
        }
    }

    /**
     * 更新预算占用状态
     *
     * @param receiveMargin
     * @throws Exception
     */
    @Override
    public void executeAudit(ReceiveMargin receiveMargin) throws Exception {
        /**
         * 结算状态为已结算补单/空时，实占预算，占用成功后更新是否占预算为实占成功；
         * 财资公共-系统集成参数-支付保证金业务台账-预算控制为是时，结算状态为待结算时，预占预算，占用成功后更新是否占预算为预占成功；
         */
        ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId(), 2);
        Short budgeted = receiveMarginNew.getIsOccupyBudget();
        ReceiveMargin update = new ReceiveMargin();
        //结算状态为已结算补单/空时
        if (receiveMarginNew.getSettlestatus() == null || receiveMarginNew.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()) {
            // 已经预占仍要预占或者已经实占，直接跳过不执行了
            if (budgeted != null && ((budgeted == OccupyBudget.ActualSuccess.getValue()))) {
                return;
            }
            // 1. 提交时占预占，是否占预算更新为预占成功；
            ResultBudget resultBudget = budget(receiveMarginNew, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(receiveMarginNew.getId());
                update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
                // 2. 删除预占 实占预算 占用成功后更新是否占预算为实占成功
                if (budgetSuccess(receiveMarginNew, true,true)) {
                    update.setId(receiveMarginNew.getId());
                    update.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                    update.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
                }
            }
        } else if (FundSettleStatus.WaitSettle.getValue() == receiveMarginNew.getSettlestatus()) {//结算状态为待结算时
            //预占预算
            //占用成功后更新是否占预算为预占成功
            if (budgeted != null && (budgeted == OccupyBudget.PreSuccess.getValue())) {
                return;
            }
            ResultBudget resultBudget = budget(receiveMarginNew, receiveMarginNew, IBillNumConstant.CMP_RECEIVEMARGIN, BillAction.SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(receiveMarginNew.getId());
                update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
            }
        }
    }
    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initBillCheck(BizObject bizObject,int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction, int index) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(bizObject);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.RECEIVEMARGIN);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        jo.put("billNo", bizObject.getString("code"));//单据编号
        jo.put("billCode", billCode);//单据类型唯一标识
        jo.put("action", billAction);//单据类型唯一标识
        jo.put("requsetbilltradetype", bizObject.get("tradetype").toString());//交易类型，用来拼接unquieRequestId
        jo.put("requsetbillaction", preemptionOrExecFlag);//预占或实占动作，用来拼接unquieRequestId
        jo.put("requsetbillid", jo.get("billId"));//单据id，用来拼接unquieRequestId
        jo.put("requsetbillpubts", ValueUtils.isNotEmptyObj(bizObject.get("pubts")) ?bizObject.get("pubts").toString() : ymsOidGenerator.nextId());//时间戳，用来拼接unquieRequestId
        jo.put("transacCode", cmCommonService.getDefaultTransTypeCode(bizObject.getString("tradetype")));//交易类型唯一标识
        jo.put("addOrReduce", addOrReduce);//1-删除,0-新增
        jo.put("lineNo", bizObject.getId());//单据行id 默认1
        Map characterDefMap = bizObject.get("characterDef");
        if (characterDefMap != null && !characterDefMap.isEmpty()) {
            characterDefMap.forEach((key, value) -> jo.put(cmpBudgetCommonManagerService.CHARACTERDEF_ + key, value));
        }
        // 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
        jo.put("isOffset", addOrReduce == 1 ? true : false);
        if (cmpBudgetCommonManagerService.PRE.equals(preemptionOrExecFlag)) {
            //预占
            jo.put("preemptionOrExecFlag", 0);//单据编号
        } else {
            //执行
            jo.put("preemptionOrExecFlag", 1);//单据编号
        }
        return jo;
    }
}
