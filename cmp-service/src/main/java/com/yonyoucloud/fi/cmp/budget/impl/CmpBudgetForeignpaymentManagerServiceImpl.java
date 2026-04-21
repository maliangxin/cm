package com.yonyoucloud.fi.cmp.budget.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.epmp.control.service.ExecdataControlService;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmpBudgetForeignpaymentManagerServiceImpl implements CmpBudgetForeignpaymentManagerService {
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Override
    public CtmJSONArray queryBillByRule(CmpBudgetEventBill budgetEventBill) throws Exception {
        //1 查询指定时间内的单据
        QuerySchema querySchema0 = QuerySchema.create().addSelect("*");// 审批通过
        CtmJSONObject billfieldValues = CtmJSONObject.toJSON(budgetEventBill.getBillfieldValues());
        //2 添加控制过滤条件
        if (billfieldValues != null) {
            for (String str : billfieldValues.keySet()) {
                CtmJSONArray strvals = billfieldValues.getJSONArray(str);
                if (strvals.size() > 0) {
                    for (int i = 0; i < strvals.size(); i++) {
                        CtmJSONObject bfValues = strvals.getJSONObject(i);
                        CtmJSONArray bfAValues = bfValues.getJSONArray("value");
                        Object[] bfAValuesTA = bfAValues.stream().toArray();
                        if (str.contains(cmpBudgetCommonManagerService.CHARACTERDEF_)) {
                            // 特征字段特殊处理
                            str = str.replace(cmpBudgetCommonManagerService.CHARACTERDEF_, cmpBudgetCommonManagerService.CHARACTERDEF);
                        }
                        if (str.contains(cmpBudgetCommonManagerService.CHARACTERDEFB_)) {
                            // 子表特征字段特殊处理
                            str = str.replace(cmpBudgetCommonManagerService.CHARACTERDEFB_, cmpBudgetCommonManagerService.CHARACTERDEFB);
                        } else {
                            querySchema0.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                        }
                    }
                }
            }
        }
        //3 单据时间key为billTime，根据billTime去查询startTime和endTime之间的数据
        querySchema0.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        String ruleType = budgetEventBill.getRuleType();
        String transacId = budgetEventBill.getTransacId();
        if (StringUtils.isNotEmpty(transacId)) {
            querySchema0.appendQueryCondition(QueryCondition.name("tradetype").eq(transacId));
        }
        List<ForeignPayment> foreignPayments = new ArrayList<>();
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
                    QueryCondition.name(cmpBudgetCommonManagerService.SETTLESTATUS).eq(FundSettleStatus.WaitSettle.getValue()));
            QueryConditionGroup group3 = QueryConditionGroup.and(
                    QueryCondition.name(cmpBudgetCommonManagerService.SETTLESTATUS).eq(FundSettleStatus.SettleProssing.getValue()));
            QueryConditionGroup or = QueryConditionGroup.or(group1, group2, group3);
            querySchema0.appendQueryCondition(or);
            List<ForeignPayment> list0 = MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema0, null);
            foreignPayments = list0;
            updateBillList(list0, OccupyBudget.PreSuccess.getValue());
        } else if ("implement".equals(ruleType)) {
            // 审批流状态为审批完成并且结算状态=结算成功/已结算补单
            querySchema0.appendQueryCondition(QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
            List<Short> settlestatus = new ArrayList<>();
            settlestatus.add(FundSettleStatus.SettleSuccess.getValue());
            settlestatus.add(FundSettleStatus.SettlementSupplement.getValue());
            querySchema0.appendQueryCondition(QueryCondition.name(cmpBudgetCommonManagerService.SETTLESTATUS).in(settlestatus));
            // 主表List集合
            List<ForeignPayment> list = MetaDaoHelper.queryObject(ForeignPayment.ENTITY_NAME, querySchema0, null);
            foreignPayments = list;
            updateBillList(list, OccupyBudget.ActualSuccess.getValue());
        }
        // 数据查询完毕，准备拼装报文
        CtmJSONArray array = new CtmJSONArray();
        HashMap<String, String> butyCode = new HashMap<>();
        for (ForeignPayment foreignPayment : foreignPayments) {
            CtmJSONObject parseObject = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(foreignPayment));
            Map characterDefMap = foreignPayment.get("characterDef");
            if (characterDefMap != null && !characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> parseObject.put(cmpBudgetCommonManagerService.CHARACTERDEF_ + key, value));
            }
            //规则id，从推送的消息中获取
            parseObject.put(cmpBudgetCommonManagerService.RULE_ID, budgetEventBill.getRuleId());
            parseObject.put(cmpBudgetCommonManagerService.RULECTRL_ID, budgetEventBill.getRuleCtrlId());
            //规则类型 (预占pre；执行implement) 从推送的消息中获取
            parseObject.put(cmpBudgetCommonManagerService.RULE_TYPE, budgetEventBill.getRuleType());
            //单据类型 可从推送消息中获取，也可以直接写成自己对接的单据类型
            parseObject.put(cmpBudgetCommonManagerService.BILL_CODE, budgetEventBill.getBillCode());
            //交易类型
            String bustype = foreignPayment.getTradetype();
            if (butyCode.containsKey(bustype)) {
                parseObject.put(cmpBudgetCommonManagerService.TRANSAC_CODE, butyCode.get(bustype));
            } else {
                String code = cmCommonService.getDefaultTransTypeCode(bustype);
                butyCode.put(bustype, code);
                parseObject.put(cmpBudgetCommonManagerService.TRANSAC_CODE, code);
            }
            //单据id
            parseObject.put(cmpBudgetCommonManagerService.BILL_ID, foreignPayment.getId());
            //单据编号
            parseObject.put(cmpBudgetCommonManagerService.BILL_BUS_CODE, foreignPayment.getCode());
            parseObject.put(cmpBudgetCommonManagerService.LINE, foreignPayment.getId());
            parseObject.put(cmpBudgetCommonManagerService.SERVICE_CODE, IServicecodeConstant.FOREIGNPAYMENT);
            array.add(parseObject);
        }
        log.error("预算期初推送报文：" + array);
        return array;
    }

    @Override
    public void updateBillList(List<ForeignPayment> foreignPayments, Short isOccupyBudget) throws Exception {
        if (CollectionUtils.isEmpty(foreignPayments)) {
            return;
        }
        List<ForeignPayment> updateList = new ArrayList<>();
        foreignPayments.stream().forEach(item -> {
            ForeignPayment update = new ForeignPayment();
            update.setId(item.getId());
            update.setIsOccupyBudget(isOccupyBudget);
            update.setEntityStatus(EntityStatus.Update);
            updateList.add(update);
        });
        if (CollectionUtils.isNotEmpty(updateList)) {
            MetaDaoHelper.update(ForeignPayment.ENTITY_NAME, updateList);
        }
    }

    @Override
    public void updateOccupyBudget(ForeignPayment foreignPayment, Short isOccupyBudget) throws Exception {
        ForeignPayment update = new ForeignPayment();
        update.setId(foreignPayment.getId());
        update.setIsOccupyBudget(isOccupyBudget);
        update.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(ForeignPayment.ENTITY_NAME, update);
        ForeignPayment foreignPaymentnew = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, foreignPayment.getId());
        foreignPayment.setPubts(foreignPaymentnew.getPubts());
    }

    @Override
    public boolean budget(ForeignPayment foreignPayment) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_FOREIGNPAYMENT)) {
            return false;
        }
        Short budgeted = foreignPayment.getIsOccupyBudget();
        // 已经预占仍要预占或者已经实占，直接跳过不执行了
        if (budgeted != null && ((budgeted != OccupyBudget.UnOccupy.getValue()))) {
            log.error("budget，已经预占仍要预占或者已经实占，直接跳过不执行了");
            return false;
        }
        ResultBudget resultBudget = cmpBudgetCommonManagerService.budget(foreignPayment, BillAction.SUBMIT, IBillNumConstant.CMP_FOREIGNPAYMENT, IServicecodeConstant.FOREIGNPAYMENT, false);
        return resultBudget.isSuccess();
    }

    @Override
    public boolean releaseBudget(ForeignPayment foreignPayment) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_FOREIGNPAYMENT)) {
            return false;
        }
        Short budgeted = foreignPayment.getIsOccupyBudget();
        if (budgeted != null && budgeted == OccupyBudget.PreSuccess.getValue()) {
            ResultBudget resultBudget = cmpBudgetCommonManagerService.budget(foreignPayment, BillAction.CANCEL_SUBMIT, IBillNumConstant.CMP_FOREIGNPAYMENT, IServicecodeConstant.FOREIGNPAYMENT, true);
            return resultBudget.isSuccess();
        } else {
            log.error("budget，已经预占仍要预占或者已经实占，直接跳过不执行了");
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public boolean implement(ForeignPayment foreignPayment) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_FOREIGNPAYMENT)) {
            return false;
        }
        if (foreignPayment.getIsOccupyBudget() != null && foreignPayment.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
            // 已经实占成功的单据，不再进行预算实占的接口调用
            log.error("implement，已经实占成功的单据，不再进行预算实占的接口调用");
            return false;
        }
        // 执行
        ResultBudget resultBudget = cmpBudgetCommonManagerService.implement(foreignPayment, IBillNumConstant.CMP_FOREIGNPAYMENT, IServicecodeConstant.FOREIGNPAYMENT, false);
        log.error("resultBudget:{}", resultBudget);
        return resultBudget.isSuccess();
    }

    @Override
    public boolean releaseImplement(ForeignPayment foreignPayment) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_FOREIGNPAYMENT)) {
            return false;
        }
        if (foreignPayment.getIsOccupyBudget() == null || foreignPayment.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
            // 已经释放，不再进行释放
            log.error("releaseImplement，已经释放，不再进行释放");
            return false;
        }
        // 执行
        ResultBudget resultBudget = cmpBudgetCommonManagerService.implement(foreignPayment, IBillNumConstant.CMP_FOREIGNPAYMENT, IServicecodeConstant.FOREIGNPAYMENT, true);
        log.error("resultBudget:{}", resultBudget);
        return resultBudget.isSuccess();
    }
}
