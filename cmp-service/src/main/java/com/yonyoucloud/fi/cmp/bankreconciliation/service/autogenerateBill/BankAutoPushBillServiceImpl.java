package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.autoorderrule.Autoorderrule;
import com.yonyoucloud.fi.cmp.autoorderrule.AutoorderruleConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.virtualFlowRuleConfig.VirtualFlowRuleConfig;
import com.yonyoucloud.fi.cmp.vo.AutoTaskCommonVO;
import com.yonyoucloud.fi.fdtr.fdtrentity.OperateEnum;
import com.yonyoucloud.fi.fdtr.openapi.IFdtrOpenApiForCmpService;
import com.yonyoucloud.fi.fdtr.openapi.ResponseResult;
import com.yonyoucloud.fi.fdtr.request.CmpRelationOperateVo;
import com.yonyoucloud.fi.stct.api.openapi.interestsettlement.IStctOpenApiForCmpService;
import com.yonyoucloud.fi.stct.api.openapi.request.CmpRelationParamVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
/**
 * @description: 银行对账单 自动推单生单具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2022/7/7 13:43
 */

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankAutoPushBillServiceImpl implements BankAutoPushBillService {

    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    @Resource
    private BusinessGenerateFundService businessGenerateFundService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BankAutoConfirmBillService autoConfirmBillService;

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    /**
     * 对账单到资金调度，事件源编码
     */
    private final String CMP_TO_FDTR_EVENT_SOURCE = "cmp_bankreconciliation";
    /**
     * 对账单到资金调度，事件类型
     */
    private final String CMP_TO_FDTR_EVENT_TYPE = "cmp_bankreconciliation_to_fdtr";

    /**
     * 银行对账单 自动推单资金调度等接口
     *
     * @param params 自动推单生单参数
     * @return 任务执行结果
     */
    @Override
    public void autoPush(Date startDate,Date endDate,JsonNode params) throws Exception {
        //查询可以推送生单的对账单
        List<BankReconciliation> allPushData = getBankReconciliationList(startDate,endDate);
        BankreconciliationUtils.checkAndFilterData(allPushData, BankreconciliationScheduleEnum.AUTOCREATEBILL);
        if (allPushData.size() == 0) {
            return;
        }
        //借
        List<BankReconciliation> debitPushDate = allPushData.stream().filter(b -> Direction.Debit.equals(b.getDc_flag())).collect(Collectors.toList());
        //贷
        List<BankReconciliation> creditPushDate = allPushData.stream().filter(b -> Direction.Credit.equals(b.getDc_flag())).collect(Collectors.toList());

        //查询自动生单规则
        List<Autoorderrule> allRule = getAutoorderruleList();
        //借方向规则
        List<Autoorderrule> debitRule = allRule.stream().filter(b -> Direction.Debit.getValue() == b.getDirection()).collect(Collectors.toList());
        //贷方向规则
        List<Autoorderrule> creditRule = allRule.stream().filter(b -> Direction.Credit.getValue() == b.getDirection()).collect(Collectors.toList());

        //根据规则匹配，并推送事件中心
        executePushBill(debitPushDate, debitRule);
        executePushBill(creditPushDate, creditRule);
    }

    @Override
    public void autoConfirmBill(AutoTaskCommonVO params) throws Exception {
        List<BankReconciliation> bankList = queryBankReconciliationData();
        if (bankList.size() == 0) {
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.getLogId(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "无可生单数据") /* "无可生单数据" */, TaskUtils.UPDATE_TASK_LOG_URL);
            JedisLockUtils.unlockBillWithOutTrace(params.getYmsLock());
            return;
        }
        List<Callable<Object>> callables = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankList) {
            callables.add(() -> {
                autoConfirmBillService.autoConfirm(bankReconciliation);
                return 1;
            });
        }
        ExecutorService executorService = null;
        try {
            int corePoolSize = Integer.parseInt(AppContext.getEnvConfig("cmp.autoconfirm.corePoolSize", "10"));
            int maxPoolSize = Integer.parseInt(AppContext.getEnvConfig("cmp.autoconfirm.maxPoolSize", "10"));
            executorService = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(corePoolSize, maxPoolSize, bankList.size(), "SubAutoConfirmBill-threadpool");
            List<Future<Object>> list = executorService.invokeAll(callables);
            if (!CollectionUtils.isEmpty(list)) {
                for (Future<Object> futrue : list) {
                    try {
                        futrue.get();
                    } catch (Exception e) {
                        log.error("自动生单自动确认单个任务异常：", e);
                    }
                }
            }
            //通知任务执行成功
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.getLogId(),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022F", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        } catch (InterruptedException e) {
            log.error("自动生单自动确认任务异常：", e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, params.getLogId(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(params.getYmsLock());
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }


    private List<BankReconciliation> queryBankReconciliationData() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.addCondition(QueryCondition.name("relationstatus").eq(1));
        int queryPageSize = Integer.parseInt(AppContext.getEnvConfig("cmp.autoconfirm.queryPageSize", "1000"));
        querySchema.addPager(0, queryPageSize);
        querySchema.addCondition(group);
        List<BankReconciliation> infoMapList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return infoMapList;
    }

    private List<BankReconciliationbusrelation_b> queryBankReconciliationRelationData(String bankreconciliationId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.addCondition(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
        querySchema.addCondition(group);
        List<BankReconciliationbusrelation_b> infoMapList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        return infoMapList;
    }


    /**
     * 自动生单确认接口
     *
     * @param params 请求参数
     */
    @Override
    public JsonNode confirmBill(JsonNode params) throws Exception {
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, params.get("id").asText());

        /**
         * 自动生单确认添加疑似退票检测
         * 如对账单为疑似退票，不允许确认
         */
        if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.MaybeRefund.getValue()))
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102533"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803C4", "该单据为疑似退票单据，请先处理退票") /* "该单据为疑似退票单据，请先处理退票" */);

        ObjectNode result = JSONBuilderUtils.createJson();
        if (!params.get("ischoosebill").asBoolean()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102534"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803C6", "未选择待确认的业务单据") /* "未选择待确认的业务单据" */);
        }
        List<Map<String, Object>> bankreconciliation_blist = JSONBuilderUtils.jsonListToMapList(JSONBuilderUtils.jsonToList(params.get("BankReconciliationbusrelation_b")));
        List<Map<String, Object>> bankreconciliation_blistEx = new ArrayList<>();
        if (null != bankreconciliation_blist && bankreconciliation_blist.size() > 0) {
            for (Map<String, Object> bs : bankreconciliation_blist) {
                if (Relationstatus.Confirm.getValue() == Short.valueOf(bs.get("relationstatus").toString())) {
                    bankreconciliation_blistEx.add(bs);
                }
            }
        }
        String autocreatebillcode = params.get("autocreatebillcode").asText(); // 自动生单编码

        // 将银行对账待确认状态更改为已确认,中间有智能对账，需要重新查询再赋值
        BankReconciliation bankReconciliationAfter = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, params.get("id").asText());
        EntityTool.setUpdateStatus(bankReconciliationAfter);
        bankReconciliationAfter.setRelationstatus(Relationstatus.Confirmed.getValue());
        CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliationAfter,"1");

        //资金收付款确认
        handleFundConfirm(bankreconciliation_blistEx, autocreatebillcode);
        //资金调度下拨收付款单确认
        handleFdtrAllocateConfirm(bankreconciliation_blistEx, autocreatebillcode);
        //资金调度归集收付款单确认
        handleFdtrCollectConfirm(bankreconciliation_blistEx, autocreatebillcode);

        result.put("dealsuccess", true);
        return result;
    }

    /**
     * 自动生单拒绝接口
     *
     * @param params 请求参数
     */
    @Override
    public JsonNode refuseBill(JsonNode params) throws Exception {
        ObjectNode result = JSONBuilderUtils.createJson();
        if (params == null) {
            return result;
        }

        List<Long> bankbillids = new ArrayList<>();
        //遍历根据关联子表信息，执行拒绝逻辑
        //银行对账单ID
        bankbillids.add(Long.valueOf(params.get("id").asText()));
        //关联信息子表
        List<Map<String, Object>> bankreconciliation_blist = JSONBuilderUtils.jsonListToMapList(JSONBuilderUtils.jsonToList(params.get("BankReconciliationbusrelation_b")));
        List<Map<String, Object>> bankreconciliation_blistEx = new ArrayList<>();
        boolean existAssociated = false;
        for (Map<String, Object> bs : bankreconciliation_blist) {
            if (Relationstatus.Confirm.getValue() == Short.valueOf(bs.get("relationstatus").toString())) {
                bankreconciliation_blistEx.add(bs);
            }
            if (Relationstatus.Confirmed.getValue() == Short.valueOf(bs.get("relationstatus").toString())) {
                existAssociated = true;
            }
        }

        // 将银行对账 关联状态更改为未关联
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, params.get("id").asText());
        EntityTool.setUpdateStatus(bankReconciliation);
        //bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
        //拒绝后关联状态置空
        bankReconciliation.setRelationstatus(null);
        /*bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
        bankReconciliation.setSerialdealtype(null);
        bankReconciliation.setIsautocreatebill(false);
        bankReconciliation.setGenertbilltype(null);
        CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliation,"2");

        //资金收付款拒绝
        handleFundRefuse(bankreconciliation_blistEx);
        //资金调度下拨收付款单拒绝
        handleFdtrAllocateRefuse(bankreconciliation_blistEx);
        //资金调度归集收付款单拒绝
        handleFdtrCollectRefuse(bankreconciliation_blistEx);
        //结算中心拒绝
        handleStctInterSettleRefuse(bankreconciliation_blistEx);

        result.put("dealsuccess", true);
        return result;
    }


    /**
     * 处理现金管理，资金收付款单确认
     */
    private void handleFundConfirm(List<Map<String, Object>> bankreconciliation_blist, Object billcode) throws Exception {
        //资金结算处理
        //资金付款单
        List<Long> payConfirmIds = new ArrayList<>();
        List<Long> payRefuseIds = new ArrayList<>();
        //资金收款单
        List<Long> collectionConfirmIds = new ArrayList<>();
        List<Long> collectionRefuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.FundPayment.getValue() == billtype) {
                if (bs.get("billcode").equals(billcode)) {
                    payConfirmIds.add(Long.valueOf(bs.get("id").toString()));
                } else {
                    payRefuseIds.add(Long.valueOf(bs.get("id").toString()));
                }
            }
            //收款单
            if (EventType.FundCollection.getValue() == billtype) {
                if (bs.get("billcode").equals(billcode)) {
                    collectionConfirmIds.add(Long.valueOf(bs.get("id").toString()));
                } else {
                    collectionRefuseIds.add(Long.valueOf(bs.get("id").toString()));
                }
            }
        }
        //调用确认拒绝逻辑
        //确认逻辑中，拒绝多余的关联业务单据，删除关联关系，不修改银行对账单是否关联属性
        if (!payConfirmIds.isEmpty()) { //资金付款单确认
            businessGenerateFundService.confirmGenerateDoc(payConfirmIds, EventType.FundPayment.getValue());
            handleRelationUpdate(payConfirmIds);
        }
        if (!payRefuseIds.isEmpty()) { //资金付款单拒绝
            businessGenerateFundService.refuseGenerateDoc(payRefuseIds, EventType.FundPayment.getValue());
            //MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, payRefuseIds)));
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(payRefuseIds);
        }
        if (!collectionConfirmIds.isEmpty()) { //资金收款单确认
            businessGenerateFundService.confirmGenerateDoc(collectionConfirmIds, EventType.FundCollection.getValue());
            handleRelationUpdate(collectionConfirmIds);
        }
        if (!collectionRefuseIds.isEmpty()) { //资金收款单拒绝
            businessGenerateFundService.refuseGenerateDoc(collectionRefuseIds, EventType.FundCollection.getValue());
            //MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, collectionRefuseIds)));
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(collectionRefuseIds);
        }
    }

    /**
     * 处理资金调度，下拨收付款单确认逻辑
     */
    private void handleFdtrAllocateConfirm(List<Map<String, Object>> bankreconciliation_blist, Object billcode) throws Exception {
        //下拨收付款确认
        List<String> payConfirmIds = new ArrayList<>();
        List<String> receiptConfirmIds = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        //下拨收付款拒绝
        List<String> payRefuseIds = new ArrayList<>();
        List<String> receiptRefuseIds = new ArrayList<>();
        //拒绝的关联表ID集合，用来删除关联信息
        List<Long> confirmIds = new ArrayList<>();
        List<Long> refuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //下拨付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.Disburse_payment_slip.getValue() == billtype) {
                if (bs.get("billcode").equals(billcode)) {
                    ids.add(bs.get("bankreconciliation").toString());
                    payConfirmIds.add(bs.get("srcbillid").toString());
                    confirmIds.add(Long.valueOf(bs.get("id").toString()));
                } else {
                    payRefuseIds.add(bs.get("srcbillid").toString());
                    refuseIds.add(Long.valueOf(bs.get("id").toString()));
                }
            }
            //下拨收款单
            if (EventType.IHand_out_receipt.getValue() == billtype) {
                if (bs.get("billcode").equals(billcode)) {
                    receiptConfirmIds.add(bs.get("srcbillid").toString());
                    confirmIds.add(Long.valueOf(bs.get("id").toString()));
                } else {
                    receiptRefuseIds.add(bs.get("srcbillid").toString());
                    refuseIds.add(Long.valueOf(bs.get("id").toString()));
                }
            }
        }
        //下拨确认逻辑
        if (!payConfirmIds.isEmpty() || !receiptConfirmIds.isEmpty()) {
            CmpRelationOperateVo operateVo = new CmpRelationOperateVo();
            operateVo.setPayIds(payConfirmIds);
            operateVo.setReceiptIds(receiptConfirmIds);
            operateVo.setOperate(OperateEnum.ALLOCATE_CONFIRM);
            try {
                ResponseResult result = RemoteDubbo.get(IFdtrOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_FDTR).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102535"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CE", "下拨单据确认接口异常:") /* "下拨单据确认接口异常:" */ + e.getMessage());
            }
            List<Map<String, Object>> maplist = null;
            if (ids.size() > 0) {
                maplist = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "*", ids);
            }
            if (maplist != null) {
                for (Map<String, Object> bs : maplist) {
                    checkIsunique(bs);
                    //判断是否开启
                    Boolean check = checkIsEnable(bs);
                    if (check) {
                        createPushBill(bs);
                    }
                }
            }
            //更新关联关系
            handleRelationUpdate(confirmIds);

        }
        //下拨拒绝逻辑
        if (!payRefuseIds.isEmpty() || !receiptRefuseIds.isEmpty()) {
            CmpRelationOperateVo operateVo = new CmpRelationOperateVo();
            operateVo.setPayIds(payRefuseIds);
            operateVo.setReceiptIds(receiptRefuseIds);
            operateVo.setOperate(OperateEnum.ALLOCATE_CANCEL);
            try {
                ResponseResult result = RemoteDubbo.get(IFdtrOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_FDTR).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102536"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803C5", "下拨单据拒绝接口异常:") /* "下拨单据拒绝接口异常:" */ + e.getMessage());
            }
            //删除多余关联关系
            /*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, refuseIds)));*/
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(refuseIds);
        }
    }

    /**
     * 处理资金调度，归集收付款单确认逻辑
     */
    private void handleFdtrCollectConfirm(List<Map<String, Object>> bankreconciliation_blist, Object billcode) throws Exception {
        //收付款确认
        List<String> payConfirmIds = new ArrayList<>();
        List<String> receiptConfirmIds = new ArrayList<>();
        // 归集收款单-关联主表id
        List<String> ids = new ArrayList<>();
        //收付款拒绝
        List<String> payRefuseIds = new ArrayList<>();
        List<String> receiptRefuseIds = new ArrayList<>();
        //确认的关联变ID集合，用来更新关联关系
        List<Long> confirmIds = new ArrayList<>();
        //拒绝的关联表ID集合，用来删除关联信息
        List<Long> refuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.Collect_payment_slips.getValue() == billtype) {
                if (bs.get("billcode").equals(billcode)) {
                    payConfirmIds.add(bs.get("srcbillid").toString());
                    confirmIds.add(Long.valueOf(bs.get("id").toString()));
                } else {
                    payRefuseIds.add(bs.get("srcbillid").toString());
                    refuseIds.add(Long.valueOf(bs.get("id").toString()));
                }
            }
            //收款单
            if (EventType.Collect_receipts.getValue() == billtype) {
                if (bs.get("billcode").equals(billcode)) {
                    ids.add(bs.get("bankreconciliation").toString());
                    receiptConfirmIds.add(bs.get("srcbillid").toString());
                    confirmIds.add(Long.valueOf(bs.get("id").toString()));
                } else {
                    receiptRefuseIds.add(bs.get("srcbillid").toString());
                    refuseIds.add(Long.valueOf(bs.get("id").toString()));
                }
            }
        }
        //归集确认逻辑
        if (!payConfirmIds.isEmpty() || !receiptConfirmIds.isEmpty()) {
            CmpRelationOperateVo operateVo = new CmpRelationOperateVo();
            operateVo.setPayIds(payConfirmIds);
            operateVo.setReceiptIds(receiptConfirmIds);
            operateVo.setOperate(OperateEnum.COLLECT_CONFIRM);
            try {
                ResponseResult result = RemoteDubbo.get(IFdtrOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_FDTR).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102537"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CC", "归集单据确认接口异常:") /* "归集单据确认接口异常:" */ + e.getMessage());
            }
            List<Map<String, Object>> maplist = null;
            if (ids.size() > 0) {
                maplist = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "*", ids);
            }
            if (maplist != null) {
                for (Map<String, Object> bs : maplist) {
                    checkIsunique(bs);
                    //判断是否开启
                    Boolean check = checkIsEnable(bs);
                    if (check) {
                        createPushBill(bs);
                    }
                }
            }
            //更新关联关系
            handleRelationUpdate(confirmIds);

        }
        //归集拒绝逻辑
        if (!payRefuseIds.isEmpty() || !receiptRefuseIds.isEmpty()) {
            CmpRelationOperateVo operateVo = new CmpRelationOperateVo();
            operateVo.setPayIds(payRefuseIds);
            operateVo.setReceiptIds(receiptRefuseIds);
            operateVo.setOperate(OperateEnum.COLLECT_CANCEL);
            try {
                ResponseResult result = RemoteDubbo.get(IFdtrOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_FDTR).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102538"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803C9", "归集单据拒绝接口异常：") /* "归集单据拒绝接口异常：" */ + e.getMessage());
            }
            //删除多余关联关系
            /*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, refuseIds)));*/
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(refuseIds);
        }
    }

    /**
     * 处理确认后，银行对账单关联关系表数据
     */
    private void handleRelationUpdate(List<Long> confirmIds) throws Exception {
        if (confirmIds.isEmpty()) {
            return;
        }
        Long[] ids = confirmIds.toArray(new Long[confirmIds.size()]);
        List<BankReconciliationbusrelation_b> blist = new ArrayList<>();
        List<Map<String, Object>> updatelist = MetaDaoHelper.queryByIds(BankReconciliationbusrelation_b.ENTITY_NAME, "*", ids);
        updatelist.forEach(sub -> {
            BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
            bankReconciliationbusrelation_b.init(sub);
            //设置更新状态，更改为已确认
            EntityTool.setUpdateStatus(bankReconciliationbusrelation_b);
            bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirmed.getValue());
            blist.add(bankReconciliationbusrelation_b);
        });
        MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, blist);
    }

    /**
     * 资金收付款单拒绝逻辑
     */
    public void handleFundRefuse(List<Map<String, Object>> bankreconciliation_blist) throws Exception {
        //资金结算处理
        //资金付款单
        List<Long> payRefuseIds = new ArrayList<>();
        //资金收款单
        List<Long> collectionRefuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.FundPayment.getValue() == billtype) {
                payRefuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
            //收款单
            if (EventType.FundCollection.getValue() == billtype) {
                collectionRefuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
        }
        //调用拒绝逻辑 删除关联信息子表数据
        if (!payRefuseIds.isEmpty()) { //资金付款单拒绝
            businessGenerateFundService.refuseGenerateDoc(payRefuseIds, EventType.FundPayment.getValue());
            //MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, payRefuseIds)));
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(payRefuseIds);
        }
        if (!collectionRefuseIds.isEmpty()) { //资金收款单拒绝
            businessGenerateFundService.refuseGenerateDoc(collectionRefuseIds, EventType.FundCollection.getValue());
            //MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, collectionRefuseIds)));
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(collectionRefuseIds);
        }
    }

    /**
     * 下拨收付款单拒绝逻辑
     */
    public void handleFdtrAllocateRefuse(List<Map<String, Object>> bankreconciliation_blist) throws Exception {
        //下拨收付款拒绝
        List<String> payRefuseIds = new ArrayList<>();
        List<String> receiptRefuseIds = new ArrayList<>();
        //拒绝的关联表ID集合，用来删除关联信息
        List<Long> refuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.Disburse_payment_slip.getValue() == billtype) {
                payRefuseIds.add(bs.get("srcbillid").toString());
                refuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
            //收款单
            if (EventType.IHand_out_receipt.getValue() == billtype) {
                receiptRefuseIds.add(bs.get("srcbillid").toString());
                refuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
        }
        //下拨拒绝逻辑
        if (!payRefuseIds.isEmpty() || !receiptRefuseIds.isEmpty()) {
            CmpRelationOperateVo operateVo = new CmpRelationOperateVo();
            operateVo.setPayIds(payRefuseIds);
            operateVo.setReceiptIds(receiptRefuseIds);
            operateVo.setOperate(OperateEnum.ALLOCATE_CANCEL);
            try {
                ResponseResult result = RemoteDubbo.get(IFdtrOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_FDTR).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102536"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803C5", "下拨单据拒绝接口异常:") /* "下拨单据拒绝接口异常:" */ + e.getMessage());
            }
            //删除多余关联关系
            /*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, refuseIds)));*/
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(refuseIds);
        }
    }

    /**
     * 归集收付款单拒绝逻辑
     */
    public void handleFdtrCollectRefuse(List<Map<String, Object>> bankreconciliation_blist) throws Exception {
        //收付款拒绝
        List<String> payRefuseIds = new ArrayList<>();
        List<String> receiptRefuseIds = new ArrayList<>();
        //拒绝的关联表ID集合，用来删除关联信息
        List<Long> refuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.Collect_payment_slips.getValue() == billtype) {
                payRefuseIds.add(bs.get("srcbillid").toString());
                refuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
            //收款单
            if (EventType.Collect_receipts.getValue() == billtype) {
                receiptRefuseIds.add(bs.get("srcbillid").toString());
                refuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
        }
        //归集拒绝逻辑
        if (!payRefuseIds.isEmpty() || !receiptRefuseIds.isEmpty()) {
            CmpRelationOperateVo operateVo = new CmpRelationOperateVo();
            operateVo.setPayIds(payRefuseIds);
            operateVo.setReceiptIds(receiptRefuseIds);
            operateVo.setOperate(OperateEnum.COLLECT_CANCEL);
            try {
                ResponseResult result = RemoteDubbo.get(IFdtrOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_FDTR).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102539"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CA", "归集单据拒绝接口异常:") /* "归集单据拒绝接口异常:" */ + e.getMessage());
            }
            //删除多余关联关系
            /*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, refuseIds)));*/
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(refuseIds);
        }
    }

    /**
     * 内部账户结息单拒绝逻辑
     */
    public void handleStctInterSettleRefuse(List<Map<String, Object>> bankreconciliation_blist) throws Exception {
        //收付款拒绝
        List<Long> payRefuseIds = new ArrayList<>();
        //拒绝的关联表ID集合，用来删除关联信息
        List<Long> refuseIds = new ArrayList<>();
        for (Map<String, Object> bs : bankreconciliation_blist) {
            //付款单
            Short billtype = Short.valueOf(bs.get("billtype").toString());
            if (EventType.stct_interestsettlement.getValue() == billtype) {
                payRefuseIds.add(Long.valueOf(bs.get("srcbillid").toString()));
                refuseIds.add(Long.valueOf(bs.get("id").toString()));
            }
        }
        //归集拒绝逻辑
        if (!payRefuseIds.isEmpty()) {
            CmpRelationParamVo operateVo = new CmpRelationParamVo();
            List<String> ids = new ArrayList<>();
            payRefuseIds.forEach(i -> {
                ids.add(i.toString());
            });
            operateVo.setBillIds(ids);
            operateVo.setOperate(com.yonyoucloud.fi.stct.api.openapi.common.enums.OperateEnum.INTEREST_SETTLEMENT_CANCEL);
            try {
                RemoteDubbo.get(IStctOpenApiForCmpService.class, IDomainConstant.MDD_DOMAIN_STCT).execute(operateVo);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102540"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803C7", "内部账户结息单拒绝接口异常：") /* "内部账户结息单拒绝接口异常：" */ + e.getMessage());
            }
            //删除多余关联关系
            /*MetaDaoHelper.batchDelete(BankReconciliationbusrelation_b.ENTITY_NAME,
                    Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, refuseIds)));*/
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(refuseIds);
        }
    }

    /**
     * 查询满足推送条件的银行对账单
     * 业务关联状态=未关联、未发布、未勾兑，且执行过自动关联任务，未执行过自动生单任务
     *
     */
    private List<BankReconciliation> getBankReconciliationList(Date startDate,Date endDate) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("associationstatus").eq(0),
                QueryCondition.name("autoassociation").eq(1), //自动执行关联任务=1
                QueryCondition.name("ispublish").eq(0),//未发布
                QueryCondition.name("checkflag").eq(0),//勾兑标识
                QueryCondition.name("other_checkflag").eq(0),//总账未勾兑
                //单据日期过滤
                QueryCondition.name("dzdate").between(startDate,endDate),
                QueryCondition.name("tran_amt").gt(BigDecimal.ZERO) //金额大于0
                //财资统一对账码非解析过来可以自动生单
        );
        //现金传结算参数，默认传结算，为false直接return
        String pushbillcontrolStr = null;
        try {
            pushbillcontrolStr = AppContext.getEnvConfig("cmp.bankreconciliation.pushbillcontrol");
            if (!StringUtils.isEmpty(pushbillcontrolStr)) {
                boolean pushbillcontrol = Boolean.parseBoolean(pushbillcontrolStr);
                if (pushbillcontrol) {
                    group.appendCondition(QueryCondition.name("isautocreatebill").eq(0)); //未执行自动生单任务
                }
            } else {
                group.appendCondition(QueryCondition.name("isautocreatebill").eq(0)); //未执行自动生单任务
            }
        } catch (Exception e) {
            log.error("cmp.bankreconciliation.pushbillcontrol", e);
            group.appendCondition(QueryCondition.name("isautocreatebill").eq(0)); //未执行自动生单任务
        }
        //疑重的不行生单
//        group.addCondition(QueryConditionGroup.or(QueryCondition.name("isrepeat").eq(0),
//                QueryCondition.name("isrepeat").is_null()));

        //账户共享，使用组织不能为空
        group.appendCondition(QueryCondition.name("accentity").is_not_null());
        //财资统一对账码非解析过来或者null可以自动生单
        QueryConditionGroup groupOr = QueryConditionGroup.or(
                QueryCondition.name("isparsesmartcheckno").is_null(),
                QueryCondition.name("isparsesmartcheckno").eq(0)
        );
        QueryConditionGroup groupAll  = QueryConditionGroup.and(
                groupOr,
                group
        );
        querySchema.addCondition(groupAll);
        try {
            return MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取对账单列表错误" + e);
            return new ArrayList<>();
        }
    }


    /**
     * 查询自动生单规则
     */
    private List<Autoorderrule> getAutoorderruleList() {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                //选择已启用规则
                QueryCondition.name("isEnable").eq(IsEnable.ENABLE.getValue())
        );
        querySchema.addCondition(group);
        try {
            return MetaDaoHelper.queryObject(Autoorderrule.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取自动生单规则列表错误" + e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据推送数据，匹配推送规则，将数据推送到事件中心
     */
    private void executePushBill(List<BankReconciliation> pushData, List<Autoorderrule> pushRule) throws Exception {
        //已匹配到的银行对账单id
        List<Long> matchedIds = new ArrayList<>();
        //匹配规则推送
        for (Autoorderrule rule : pushRule) {
            //匹配的数据
            List<BankReconciliation> matchData = getMatchData(pushData, rule);
            if (matchData.isEmpty()) {
                continue;
            }
            for (BankReconciliation bankReconciliation : matchData) {
                if (!matchedIds.contains(bankReconciliation.getId())) {
                    matchedIds.add(bankReconciliation.getId());
                }
            }
            //根据不同应用，做特殊处理 现金管理调用接口
            //资金调度 或者结算中心发送消息队列
            //是否开启简强非简强才会发送资金归集的推单请求
            boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
            if (!enableSimplify && (rule.getApplication().equals(SystemNameType.MoneyScheduling.getValue()) || rule.getApplication().equals(SystemNameType.SettlementCenter.getValue()))) {
                int batchcount = Integer.parseInt(AppContext.getEnvConfig("cmp.autopush.MsgSize", "5"));
                if (batchcount == 0) {
                    sendFdtrMsg(rule, matchData);
                } else {
                    int listSize = matchData.size();
                    int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount) + 1);
                    for (int i = 0; i < totalTask; i++) {
                        int fromIndex = i * batchcount;
                        int toIndex = i * batchcount + batchcount;
                        if (i + 1 == totalTask) {
                            toIndex = listSize;
                        }
                        sendFdtrMsg(rule, matchData.subList(fromIndex, toIndex));
                    }
                }
            }

            //现金管理
            if (rule.getApplication().equals(SystemNameType.CashManagement.getValue())) {
                try {
                    businessGenerateFundService.bankreconciliationGenerateDoc(matchData, rule);
                } catch (Exception e) {
                    log.error("资金收付款单自动生单异常", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102542"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "资金收付款单生单异常:") + e.getMessage());
                }
            }
        }
        //所有执行过生单的银行对账单，是否自动生单设置为是
        if (!matchedIds.isEmpty()) {
            List<BankReconciliation> matchedList = getBankReconciliationListByIds(matchedIds);
            matchedList.forEach(b -> {
                //设置更新状态，更改为已自动生单
                EntityTool.setUpdateStatus(b);
                b.setIsautocreatebill(true);
            });
            CommonSaveUtils.updateBankReconciliation(matchedList);
        }

    }

    private void sendFdtrMsg(Autoorderrule rule, List<BankReconciliation> matchData) throws Exception {
        //事件中心，发送的数据包装类
        BizObject userObject = new BizObject();
        //对方类型
        userObject.put("oppositetype", rule.getOtherType());
        //业务单据类型
        userObject.put("billtype", rule.getBusDocumentType());
        //对账单生单数据
        userObject.put("datalist", matchData);
        //发送消息到事件中心
        SendEventMessageUtils.sendEventMessageEos(userObject, CMP_TO_FDTR_EVENT_SOURCE, CMP_TO_FDTR_EVENT_TYPE);
    }

    /**
     * 根据银行对账ids查询对账单信息
     *
     * @param ids
     * @return
     * @throws Exception
     */
    private List<BankReconciliation> getBankReconciliationListByIds(List<Long> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
        querySchema.addCondition(group);
        return MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
    }

    /**
     * 获取匹配的数据
     */
    private List<BankReconciliation> getMatchData(List<BankReconciliation> pushData, Autoorderrule rule) throws Exception {
        List<BankReconciliation> matchData = new ArrayList<>();

        //自动生单规则添加客户和供应商相关过滤
        List<AutoorderruleConfig> oldConfigs = new ArrayList<>();
        if (rule.getOtherType() == OppositeType.Customer.getValue() || rule.getOtherType() == OppositeType.Supplier.getValue()) {
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup();
            conditionGroup.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("mainid").eq(rule.getId()))
            );
            schema.addCondition(conditionGroup);
            oldConfigs = MetaDaoHelper.queryObject(AutoorderruleConfig.ENTITY_NAME, schema, null);
        }

        for (BankReconciliation b : pushData) {
            //匹配对方类型
            if (!rule.getOtherType().equals(b.getOppositetype())) {
                continue;
            }
            //判断是否是满足条件的客户或者供应商
            if (oldConfigs.size() > 0) {
                boolean isCorrect = false;
                for (AutoorderruleConfig config : oldConfigs) {
                    //判断客户
                    if (rule.getOtherType() == OppositeType.Customer.getValue()
                            && config.getCustomer().toString().equals(b.getOppositeobjectid())) {
                        isCorrect = true;
                    }
                    //判断供应商
                    if (rule.getOtherType() == OppositeType.Supplier.getValue()
                            && config.getSupplier().toString().equals(b.getOppositeobjectid())) {
                        isCorrect = true;
                    }
                }
                //经过判断，不匹配则跳过
                if (!isCorrect) {
                    continue;
                }
            }


            //不设置敏感词，直接校验通过
            if (rule.getSensitiveWords() == null) {
                matchData.add(b);
                continue;
            }
            String[] sensitiveWords = rule.getSensitiveWords().split(",");
            //判断敏感词,包含全部敏感词
            if (rule.getSensitiveWordsType().equals(SensitiveWordsType.ALL.getValue())) {
                // 备注为空直接跳过
                if (StringUtils.isEmpty(b.getRemark())) {
                    continue;
                }
                int notContainNum = 0;
                for (String sensitiveWord : sensitiveWords) {
                    if (!b.getRemark().contains(sensitiveWord)) {
                        notContainNum++;
                    }
                }
                if (notContainNum > 0) {
                    continue;
                }
            }
            //判断敏感词,包含部分敏感词
            if (rule.getSensitiveWordsType().equals(SensitiveWordsType.PART.getValue())) {
                // 备注为空直接跳过
                if (StringUtils.isEmpty(b.getRemark())) {
                    continue;
                }
                int containNum = 0;
                for (String sensitiveWord : sensitiveWords) {
                    if (b.getRemark().contains(sensitiveWord)) {
                        containNum++;
                    }
                }
                if (containNum == 0) {
                    continue;
                }
            }
            matchData.add(b);
        }
        return matchData;
    }

    /**
     * 判断该单据是否已生成虚拟流水
     */
    private void checkIsunique(Map<String, Object> bs) throws Exception {
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("count(1) as  count");
        String virtualflowid = (String) (bs.get("virtualflowid") == null ? String.valueOf(bs.get("id")) : bs.get("virtualflowid"));
        querySchema.appendQueryCondition(QueryCondition.name("virtualflowid").eq(virtualflowid));
        Map<String, Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, querySchema);
        Long count = (Long) map.get("count");
        if (count > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102543"), com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_1696419920611377159"));
        }
    }

    /**
     * 生成虚拟流水
     */
    private void createPushBill(Map<String, Object> bs) throws Exception {
        //    万能接口查询银行信息
        EnterpriseBankAcctVO bankaccount = baseRefRpcService.queryEnterpriseBankAccountById(bs.get("bankaccount").toString());
        BankReconciliation bankReconciliation = new BankReconciliation();
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setAccount(bs.get("to_acct_no").toString());
        List<EnterpriseBankAcctVO> enterpriseBankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (enterpriseBankAccounts != null && enterpriseBankAccounts.size() > 0) {
            //      对方银行账号
            bankReconciliation.setBankaccount(enterpriseBankAccounts.get(0).getId());
            //      对方户名
            bankReconciliation.setAccentity(enterpriseBankAccounts.get(0).getOrgid());
        }
        //      本方银行账号
        bankReconciliation.setTo_acct_no(bankaccount.getAccount());
        //      本方账户名称
        bankReconciliation.setTo_acct_name(bankaccount.getName());
        //      本方开户行
        bankReconciliation.setTo_acct_bank(bankaccount.getBankName());
        //      本方开户行名
        bankReconciliation.setTo_acct_bank_name(bankaccount.getBankNumberName());
        //      借贷方向及金额
        if (bs.get("dc_flag").toString().equals(String.valueOf(Direction.Credit.getValue()))) {
            bankReconciliation.setDc_flag(Direction.Debit);
            bankReconciliation.setDebitamount((BigDecimal) bs.get("tran_amt"));
        } else {
            bankReconciliation.setDc_flag(Direction.Credit);
            bankReconciliation.setCreditamount((BigDecimal) bs.get("tran_amt"));
        }
        //      数据来源
        bankReconciliation.setDataOrigin(DateOrigin.AddManually);
        //      事项来源
        bankReconciliation.setSrcitem(EventSource.Cmpchase);
        String bankNum = String.valueOf(new StringBuffer("XN").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append(this.getL6()));
        //      银行交易流水号
        bankReconciliation.setBank_seq_no(bankNum);
        //      虚拟标记
        bankReconciliation.setIsvirtualflow(true);
        //      币种
        bankReconciliation.setCurrency(bs.get("currency").toString());
        //     交易日期
        bankReconciliation.setTran_date((Date) bs.get("tran_date"));
        //     对方类型
        bankReconciliation.setOppositetype(Short.valueOf(bs.get("oppositetype").toString()));
        EnterpriseParams enterpriseParams1 = new EnterpriseParams();
        enterpriseParams1.setAccount(bankReconciliation.getTo_acct_no());
        List<EnterpriseBankAcctVO> enterpriseBankAccount = enterpriseBankQueryService.query(enterpriseParams1);
        if (enterpriseBankAccount != null && enterpriseBankAccount.size() > 0) {
            //     对方单位
            bankReconciliation.setOppositeobjectid(enterpriseBankAccount.get(0).getOrgid());
            //     对方单位名称
            bankReconciliation.setOppositeobjectname(enterpriseBankAccount.get(0).getOrgidName());
        }
        if (bs.get("remark") != null) {
            //      摘要
            bankReconciliation.setRemark(bs.get("remark").toString());
        }
        if (bs.get("use_name") != null) {
            //      用途
            bankReconciliation.setUse_name(bs.get("use_name").toString());
        }
        //      是否自动生单
        bankReconciliation.setIsautocreatebill(false);
        //      自动关联标志
        bankReconciliation.setAutoassociation(false);
        //     创建时间
        bankReconciliation.setCreateTime((Date) bs.get("createTime"));
        //     贷方金额
        bankReconciliation.setTran_amt((BigDecimal) bs.get("tran_amt"));
        bankReconciliation.setId(ymsOidGenerator.nextId());
        // 所属组织
        bankReconciliation.setOrgid(bankaccount.getOrgid());
        // 确认状态
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(bankReconciliation.getAccentity())) {
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
        }
        // 未解析出财资统一码，生成财资统一码并进行设置
        bankReconciliation.setSmartcheckno(RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate());
        bankReconciliation.setIsparsesmartcheckno(false);
        bankReconciliation.setVirtualflowid(bs.get("id").toString());
        // 对账单日期
        bankReconciliation.setDzdate(bs.get("dzdate") == null ? (Date) bs.get("tran_date") : (Date) bs.get("dzdate"));
        //      入库
        CommonSaveUtils.saveBankReconciliation(bankReconciliation);

    }

    private Boolean checkIsEnable(Map<String, Object> bs) throws Exception {
        EnterpriseBankAcctVO bankaccount = baseRefRpcService.queryEnterpriseBankAccountById(bs.get("bankaccount").toString());
        //      根据银行类别在虚拟流水规则找是否启用该规则
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = QueryConditionGroup.and(QueryCondition.name("banktype").eq(bankaccount.getBank()));
        querySchema.addCondition(condition);
        Map<String, Object> virtualFlowRulelist = MetaDaoHelper.queryOne(VirtualFlowRuleConfig.ENTITY_NAME, querySchema);
        if (MapUtils.isNotEmpty(virtualFlowRulelist)) {
            //       规则启用
            if (Short.valueOf(virtualFlowRulelist.get("isEnable").toString()).equals(IsEnable.ENABLE.getValue())) {
                return true;
            }
        }
        return false;
    }

    public String getL6() throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("bank_seq_no");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        conditionGroup.appendCondition(QueryCondition.name("isvirtualflow").eq(true));
        schema.addOrderBy(new QueryOrderby("createTime", "desc"));
        schema.addCondition(conditionGroup);
        Map<String, Object> list = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, schema);
        String equipmentNo = null;
        if (list != null && list.size() > 0) {
            String seqNo = list.get("bank_seq_no").toString().substring(2, 10);
            //  同一天从库里最大的开始加
            if (new SimpleDateFormat("yyyyMMdd").format(new Date()).equals(seqNo)) {
                equipmentNo = list.get("bank_seq_no").toString().substring(11);
            } else {
                // 不同天默认从000001开始
                equipmentNo = "000001";
            }
            // 字符串数字解析为整数
            int no = Integer.parseInt(equipmentNo);
            // 编号自增1
            int newEquipment = ++no;
            equipmentNo = String.format("%06d", newEquipment);
        } else {
            equipmentNo = "000001";
        }
        return equipmentNo;
    }
}