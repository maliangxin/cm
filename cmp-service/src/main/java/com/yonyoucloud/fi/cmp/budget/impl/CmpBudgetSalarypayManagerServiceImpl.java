package com.yonyoucloud.fi.cmp.budget.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.epmp.busi.service.BusiSystemConfigService;
import com.yonyou.epmp.control.dto.ControlDetailVO;
import com.yonyou.epmp.control.service.ExecdataControlService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.BudgetDirect;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay_b;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.BudgetUtils;
import com.yonyoucloud.fi.cmp.util.RestTemplateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class CmpBudgetSalarypayManagerServiceImpl implements CmpBudgetSalarypayManagerService {

    @Value("${billdetail-url}")
    private String billDetailUrl;
    public static final String SALARYPAY = "Salarypay__";
    public static final String SALARYPAY_B = "Salarypay_b__";
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private BusiSystemConfigService busiSystemConfigService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Autowired
    private ExecdataControlService execdataControlService;
    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;

    @Override
    public CtmJSONArray querySalarypay(CmpBudgetEventBill budgetEventBill) throws Exception {
        //1 查询指定时间内的单据
        QuerySchema querySchema0 = QuerySchema.create().addSelect("*");// 审批通过
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");// 审批中
        // 子表查询条件
        QuerySchema querySchema_b = QuerySchema.create().addSelect("*");
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
                        }
                        if (str.contains(SALARYPAY)) {
                            // 子表过滤字段
                            querySchema_b.appendQueryCondition(QueryCondition.name(str.replace(SALARYPAY, "")).in(bfAValuesTA));
                        } else {
                            querySchema0.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                            querySchema1.appendQueryCondition(QueryCondition.name(str).in(bfAValuesTA));
                        }
                    }
                }
            }
        }
        //3 单据时间key为billTime，根据billTime去查询startTime和endTime之间的数据
        if (budgetEventBill.getBillTime() != null && budgetEventBill.getBillTime().contains(SALARYPAY)) {
            // 子表时间字段
            querySchema_b.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime().replace(SALARYPAY, "")).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        } else {
            querySchema0.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
            querySchema1.appendQueryCondition(QueryCondition.name(budgetEventBill.getBillTime()).between(budgetEventBill.getStartDate(), budgetEventBill.getEndDate()));
        }
        String ruleType = budgetEventBill.getRuleType();
        String transacId = budgetEventBill.getTransacId();
        if (StringUtils.isNotEmpty(transacId)) {
            querySchema0.appendQueryCondition(QueryCondition.name("tradetype").eq(transacId));
            querySchema1.appendQueryCondition(QueryCondition.name("tradetype").eq(transacId));
        }
        //没有作废的才推期初
        querySchema0.appendQueryCondition(QueryCondition.name("invalidflag").eq(0));
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
        List<Salarypay_b> salarypay_bs = new ArrayList<>();// 子表数据集合
        List<Salarypay> salarypays = new ArrayList<>();// 主表数据集合
        // 主表Map集合-审批中和审批通过
        Map<String, Salarypay> salarypaysMap = new HashMap<>();
        if ("pre".equals(ruleType)) {
            // TODO 目前触发时机为写死的，提交审批流预置，结算成功实占；后续如果根据配置时机占用，需保留下面逻辑
            /**
             * 启用审批流单据，审批流状态为审批中/审批完成并且付款明细结算状态等于结算中/待结算
             * 未启用审批流单据，审批流状态为审批完成并且付款明细结算状态等于结算中/待结算
             */
            //审批中 或者 审批完成且支付状态不等于支付成功
            QueryConditionGroup group1 = QueryConditionGroup.and(
                    QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.SUBMITED.getValue()));
            List<Short> paystatus = new ArrayList<>();
            paystatus.add(PayStatus.Success.getValue());
            paystatus.add(PayStatus.OfflinePay.getValue());
            paystatus.add(PayStatus.SupplPaid.getValue());
            QueryConditionGroup group2 = QueryConditionGroup.and(
                    QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()),
                    QueryCondition.name("paystatus").not_in(paystatus));
            QueryConditionGroup or = QueryConditionGroup.or(group1, group2);
            querySchema0.appendQueryCondition(or);
            // 审批通过-主表单据
            List<Salarypay> list0 = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema0, null);
            salarypays = list0;
            // 主表id集合-审批通过
            List<String> ids0 = new ArrayList();
            list0.stream().forEach(e -> {
                ids0.add(e.getId().toString());
                salarypaysMap.put(e.getId().toString(), e);
            });
            if (list0.size() > 0) {
                updateSalaryPay(salarypays, OccupyBudget.PreSuccess.getValue());
            }
        } else if ("implement".equals(ruleType)) {
            // 审批流状态为审批完成并且结算状态=结算成功/部分成功/退票/已结算补单
            querySchema0.appendQueryCondition(QueryCondition.name(cmpBudgetCommonManagerService.VERIFYSTATE).eq(VerifyState.COMPLETED.getValue()));
            List<Short> paystatus = new ArrayList<>();
            paystatus.add(PayStatus.Success.getValue());
            paystatus.add(PayStatus.OfflinePay.getValue());
            paystatus.add(PayStatus.SupplPaid.getValue());
            querySchema0.appendQueryCondition(QueryCondition.name("paystatus").in(paystatus));
            // 主表List集合
            List<Salarypay> list = MetaDaoHelper.queryObject(Salarypay.ENTITY_NAME, querySchema0, null);
            salarypays = list;
            // 主表id集合
            List<String> ids = new ArrayList();
            list.stream().forEach(e -> {
                ids.add(e.getId().toString());
                salarypaysMap.put(e.getId().toString(), e);
            });
            if (list.size() > 0) {
                updateSalaryPay(salarypays, OccupyBudget.ActualSuccess.getValue());
            }
        }
        // 数据查询完毕，准备拼装报文
        CtmJSONArray array = new CtmJSONArray();
        HashMap<String, String> butyCode = new HashMap<>();
        for (Salarypay salarypay : salarypays) {
            CtmJSONObject salarypayJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(salarypay));
            Map characterDefMap = salarypay.get("characterDef");
            if (characterDefMap != null && !characterDefMap.isEmpty()) {
                characterDefMap.forEach((key, value) -> salarypayJson.put(cmpBudgetCommonManagerService.CHARACTERDEF_ + key, value));
            }
            //规则id，从推送的消息中获取
            salarypayJson.put(cmpBudgetCommonManagerService.RULE_ID, budgetEventBill.getRuleId());
            salarypayJson.put(cmpBudgetCommonManagerService.RULECTRL_ID, budgetEventBill.getRuleCtrlId());
            //规则类型 (预占pre；执行implement) 从推送的消息中获取
            salarypayJson.put(cmpBudgetCommonManagerService.RULE_TYPE, budgetEventBill.getRuleType());
            //单据类型 可从推送消息中获取，也可以直接写成自己对接的单据类型
            salarypayJson.put(cmpBudgetCommonManagerService.BILL_CODE, budgetEventBill.getBillCode());
            //交易类型
            String bustype = salarypay.getTradetype();
            if (butyCode.containsKey(bustype)) {
                salarypayJson.put(cmpBudgetCommonManagerService.TRANSAC_CODE, butyCode.get(bustype));
            } else {
                String code = cmCommonService.getDefaultTransTypeCode(bustype);
                butyCode.put(bustype, code);
                salarypayJson.put(cmpBudgetCommonManagerService.TRANSAC_CODE, code);
            }
            //单据id
            salarypayJson.put(cmpBudgetCommonManagerService.BILL_ID, salarypay.getId());
            //单据编号
            salarypayJson.put(cmpBudgetCommonManagerService.BILL_BUS_CODE, salarypay.getCode());
            salarypayJson.put(cmpBudgetCommonManagerService.LINE, salarypay.getId());
            salarypayJson.put(cmpBudgetCommonManagerService.SERVICE_CODE, IServicecodeConstant.SALARYPAY);
            array.add(salarypayJson);
        }
        log.error("预算期初推送报文：" + array);
        return array;
    }


    @Override
    public void updateSalaryPay(List<Salarypay> salarypays, Short isOccupyBudget) throws Exception {
        if (CollectionUtils.isEmpty(salarypays)) {
            return;
        }
        List<Salarypay> updateList = new ArrayList<>();
        salarypays.stream().forEach(item -> {
            Salarypay update = new Salarypay();
            update.setId(item.getId());
            update.setIsOccupyBudget(isOccupyBudget);
            update.setEntityStatus(EntityStatus.Update);
            updateList.add(update);
        });
        if (CollectionUtils.isNotEmpty(updateList)) {
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, updateList);
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget gcExecuteSubmit(BizObject bizObject, Salarypay_b salarypay_b, String billCode, String billAction) throws Exception {
        try {
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
            Map<String, Object> jo = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction);
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
    public ResultBudget gcExecuteBatchSubmit(BizObject bizObject, List<Salarypay_b> salarypay_bs, String billCode, String billAction) throws Exception {
        try {
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
            int size = 1;
            Map<String, Object>[] bills = new Map[size];
            ControlDetailVO[] controlDetailVOS;
            for (int i = 0; i < size; i++) {
                Map copiedMap = new HashMap<>(initBill(bizObject, (Salarypay) bizObject, BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction));
                bills[i] = copiedMap;
            }
            controlDetailVOS = cmpBudgetCommonManagerService.initControlDetailVOs(bills, action, 0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 ", CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce 撤回需要删除
     * @param billCode
     */
    private Map<String, Object> initBill(BizObject bizObject, Salarypay salarypay, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(bizObject);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.SALARYPAY);
        jo.put("billId", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
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
        // 特征字段拼接 例：characterDef_test0919
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
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget gcExecuteTrueUnAudit(BizObject bizObject, String billCode, String billAction) {
        try {
            String preAction;
            String implementAction;
            int size = 1;
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
                Map<String, Object> preBill = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, billAction);
                controlDetailVOPre = cmpBudgetCommonManagerService.initControlDetailVO(preBill, preAction, 0);
                Map<String, Object> implementBills = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, billAction);
                controlDetailVOImplement = cmpBudgetCommonManagerService.initControlDetailVO(implementBills, implementAction, 0);
                controlDetailVOS = new ControlDetailVO[]{controlDetailVOPre, controlDetailVOImplement};
                log.error("预算释放实占占用预占请求报文：" + controlDetailVOPre);
                log.error("预算释放实占请求报文：" + controlDetailVOImplement);
            } else {
                controlDetailVOS = new ControlDetailVO[1];
                Map<String, Object> implementBill = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, billAction);
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
    public ResultBudget gcExecuteDelete(BizObject bizObject, Salarypay_b salarypay_b, String billCode, String billAction) throws Exception {
        try {
            if (salarypay_b.getIsOccupyBudget() != null && salarypay_b.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
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
            Map bill = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.REDUCE.getIndex(), billCode, preemptionOrExecFlag, billAction);
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
    public ResultBudget gcExecuteBatchUnSubmit(BizObject bizObject, String billCode, String billAction) throws Exception {
        try {
            String action;
            int addOrReduce = 1;//删除
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
            ControlDetailVO[] controlDetailVOS = new ControlDetailVO[1];
            Map copiedMap = new HashMap<>(initBill(bizObject, (Salarypay) bizObject, BudgetDirect.REDUCE.getIndex(), billCode, preemptionOrExecFlag, billAction));
            controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(copiedMap, action, 0);
            // 控制接口(预占、实占)
            log.error("预算释放预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算释放预占响应报文 ", CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 实占由于是结算回调，每条子表数据结算单独触发，只能单条调用
     *
     * @param bizObject
     * @param billCode    单据编码
     * @param billAction
     * @return
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget gcExecuteTrueAudit(BizObject bizObject, String billCode, String billAction, boolean deleteBudget) {
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
            preAction = (String) objects.get(0).get(cmpBudgetCommonManagerService.PRE);
            // 未配置预占动作，则无需释放预占，直接实占
            ControlDetailVO[] controlDetailVOS;
            ControlDetailVO controlDetailVPre;
            ControlDetailVO controlDetailVImplement;
            if (!StringUtils.isEmpty(preAction) || deleteBudget) {
                Map preBill = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, BillAction.APPROVE_PASS);
                controlDetailVPre = cmpBudgetCommonManagerService.initControlDetailVO(preBill, preAction, 0);
                log.error("预算实占释放预占请求报文： " + controlDetailVPre);
                Map implementBill = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, BillAction.APPROVE_PASS);
                controlDetailVImplement = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                log.error("预算实占请求报文： " + controlDetailVImplement);
                controlDetailVOS = new ControlDetailVO[]{controlDetailVPre, controlDetailVImplement};
            } else {
                controlDetailVOS = new ControlDetailVO[1];
                Map implementBill = initBill(bizObject, (Salarypay) bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.IMPLEMENT, BillAction.APPROVE_PASS);
                controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(implementBill, implementAction, 0);
                log.error("预算实占请求报文： " + controlDetailVOS[0]);
            }
            // 控制接口(预占、实占)
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算实占响应报文： ", CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 删除预占
     *
     * @param salarypay
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void executeBudgetDelete(Salarypay salarypay) throws Exception {
        Salarypay salarypayNew = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId());
        Short budgeted = salarypayNew.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        }
        ResultBudget resultBudget = gcExecuteBatchUnSubmit(salarypay, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
        if (resultBudget.isSuccess()) {
            Salarypay update = new Salarypay();
            salarypay.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
            update.setId(salarypay.getId());
            update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
        }
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
            Salarypay salarypay = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, id);
            String preemptionOrExecFlag = "";
            String billAction = BillAction.SUBMIT;
            if (salarypay.getIsOccupyBudget() != null && salarypay.getIsOccupyBudget() == OccupyBudget.PreSuccess.getValue()) {
                preemptionOrExecFlag = cmpBudgetCommonManagerService.PRE;
            } else if (salarypay.getIsOccupyBudget() != null && salarypay.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
                preemptionOrExecFlag = cmpBudgetCommonManagerService.IMPLEMENT;
            } else {
                billAction = BillAction.QUERY;
            }
            String billCode = IBillNumConstant.SALARYPAY;
            int addOrReduce = 0;//新增
            List<Salarypay_b> salarypay_bs = salarypay.Salarypay_b();
            if (CollectionUtils.isEmpty(salarypay_bs)) {
                return ResultMessage.data(resultBack);
            }
            //查询预占单据
            Map[] bills = new Map[1];
            for (int i = 0; i < 1; i++) {
                Map jo = initBill(salarypay, salarypay, addOrReduce, billCode, preemptionOrExecFlag, billAction);
                bills[i] = jo;
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
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101130"),result.getString("message"));
            }
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
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
                Salarypay oldBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, bizObject.getId(), 2);
                if (Short.parseShort(verifystate.toString()) == VerifyState.SUBMITED.getValue()) {
                    int index = 1;
                    Map<String, Object> bill = initSalaryPayBillCheck(bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, action, index);
                    bills.add(bill);
                    Map<String, Object> billReduce = initSalaryPayBillCheck(oldBill, BudgetDirect.REDUCE.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, action, index);
                    bills.add(billReduce);
                } else {
                    int index = 1;
                    Map<String, Object> bill = initSalaryPayBillCheck(bizObject, BudgetDirect.ADD.getIndex(), billCode, cmpBudgetCommonManagerService.PRE, action, index);
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400392", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 财资公共-集成参数-支付保证金业务台账-预算控制为是时，调用预算业务单据控制接口，占用预算，占用类型=预占，接口调用成功后，更新是否占预算为预占成功。
     *
     * @param salarypay
     * @throws Exception
     */
    @Override
    public void executeSubmit(Salarypay salarypay) throws Exception {
        String billKey = ICmpConstant.SERVICECODE_SALPAY;
        Long salaryPayId = salarypay.getId();
        if (!ymsScopeLockManager.tryTxScopeLock(billKey + salaryPayId.toString())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100185"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D5", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
        }
        Short budgeted = salarypay.getIsOccupyBudget();
        // 已经预占仍要预占或者已经实占，直接跳过不执行了
        if (budgeted != null && ((budgeted != OccupyBudget.UnOccupy.getValue()))) {
            log.error("executeSubmit，已经预占仍要预占或者已经实占，直接跳过不执行了");
            return;
        }
        ResultBudget resultBudget = budget(salarypay, salarypay, IBillNumConstant.SALARYPAY, BillAction.SUBMIT);
        if (resultBudget.isSuccess()) {//可能是没有匹配上规则，也可能是没有配置规则
            salarypay.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
            Salarypay update = new Salarypay();
            update.setId(salarypay.getId());
            update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
            update.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
        }
    }

    @Override
    public void reMatchBudget(Salarypay newBill,boolean reMatch) throws Exception {
        //1.判断是否是预占成功，如果是删除旧的预占，新增新的预占
        //2.判断是否是预占成功，如果否直接进行新的预占
        Salarypay oldBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, newBill.getId(), 2);
        Short budgeted = oldBill.getIsOccupyBudget();
        if (budgeted == null || budgeted == OccupyBudget.UnOccupy.getValue()) {
            log.error("ReMatchBudget ，占用预算");
            executeSubmit(newBill);
        } else if (budgeted == OccupyBudget.PreSuccess.getValue()) {
            ResultBudget releaseBudget = releaseBudget(oldBill, oldBill, IBillNumConstant.SALARYPAY, BillAction.APPROVE_EDIT);
            if (releaseBudget.isSuccess()) {
                log.error("ReMatchBudget ，删除预算成功");
                newBill.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                if (reMatch) {
                    ResultBudget resultBudget = budget(newBill, newBill, IBillNumConstant.SALARYPAY, BillAction.APPROVE_EDIT);
                    if (resultBudget.isSuccess()) {
                        log.error("ReMatchBudget ，重新占用预算成功");
                        newBill.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    } else {
                        log.error("ReMatchBudget ，重新占用预算失败,resultBudget：{}", resultBudget);
                    }
                }
            } else {
                log.error("ReMatchBudget ，删除预算失败,releaseBudget：{},reMatch:{}", releaseBudget, reMatch);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public ResultBudget releaseBudget(BizObject bizObject, Salarypay salarypay, String billCode, String billAction) throws Exception {
        try {
            log.error("releaseBudget...............");
            if (salarypay.getIsOccupyBudget() != null && salarypay.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
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
            Map bill = initBill(bizObject, salarypay, BudgetDirect.REDUCE.getIndex(), billCode, preemptionOrExecFlag, billAction);
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
    public ResultBudget budget(BizObject bizObject, Salarypay salarypay, String billCode, String billAction) throws Exception {
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
            Map<String, Object> jo = initBill(bizObject, salarypay, BudgetDirect.ADD.getIndex(), billCode, preemptionOrExecFlag, billAction);
            ControlDetailVO[] controlDetailVOS = new ControlDetailVO[1];
            controlDetailVOS[0] = cmpBudgetCommonManagerService.initControlDetailVO(jo, action, 0);
            // 控制接口(预占、实占)
            log.error("预算预占请求报文：" + controlDetailVOS[0]);
            CtmJSONObject result = new CtmJSONObject(execdataControlService.control(controlDetailVOS));
            log.error("预算预占响应报文 "+ CtmJSONObject.toJSONString(result));
            return cmpBudgetCommonManagerService.doResult(result);
        } catch (Exception e) {
            log.error("execute ", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100234"),InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B801BF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AD061204380011", "预算系统返回:") /* "预算系统返回:" */) /* "预算系统返回:" */ + e.getMessage());
        }
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     * @param salarypay
     * @throws Exception
     */
    public void executeAuditDelete(Salarypay salarypay) throws Exception {
        Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId());
        Short budgeted = newBill.getIsOccupyBudget();
        Salarypay update = new Salarypay();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            ResultBudget resultBudget = releaseBudget(salarypay, newBill, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(salarypay.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
            }
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            ResultBudget resultBudget = gcExecuteTrueUnAudit(salarypay, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(salarypay.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
            }
        }
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     * @param salarypay
     * @throws Exception
     */
    public void executeAuditDeleteReBudget(Salarypay salarypay) throws Exception {
        Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId());
        Short budgeted = newBill.getIsOccupyBudget();
        Salarypay update = new Salarypay();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            ResultBudget resultBudget = releaseBudget(salarypay, newBill, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                update.setId(salarypay.getId());
                update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
            }
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            ResultBudget resultBudget = gcExecuteTrueUnAudit(salarypay, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                ResultBudget budget = budget(salarypay, newBill, IBillNumConstant.SALARYPAY, BillAction.SUBMIT);
                if (budget.isSuccess()) {
                    update.setId(salarypay.getId());
                    update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    update.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
                } else {
                    update.setId(salarypay.getId());
                    update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    update.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
                }
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
    private Map<String, Object> initSalaryPayBillCheck(BizObject bizObject, int addOrReduce, String billCode, String preemptionOrExecFlag, String billAction, int index) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.putAll(bizObject);
        //初始化
        jo.put("signature", null);// 签名字段不传，且包含特殊字符可能被预算中台拦截
        jo.put("serviceCode", IServicecodeConstant.SALARYPAY);
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
        jo.put("lineNo", ValueUtils.isNotEmptyObj(bizObject.getId()) ? bizObject.getId() : ymsOidGenerator.nextId());//单据id
        // 特征字段拼接 例：characterDef_test0919
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
