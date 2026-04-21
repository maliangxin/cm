package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BillProcessFlag;
import com.yonyoucloud.fi.cmp.cmpentity.FrozenStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yonyou.bpm.rest.utils.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


/**
 * 银行对账单 自动推单生单具体实现
 *
 * @author yp
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankAutoPushBillNewServiceImpl implements BankAutoPushBillNewService {

    private static final int DEFAULT_MAX_PROCESS_BILL_COUNT = 500;//调度任务子线程一次默认处理单据数量
    private static final int DEFAULT_MAX_SUB_PROCESS_BILL_COUNT = 50;//调度任务子线程一次默认处理单据数量
    private static final int DEFAULT_MAX_QUERY_BILL_COUNT = 7000;//调度任务一次默认查询单据数量
    private static final int MAX_QUERY_BILL_NUM = 20000;//任务一次最多查询单据数量
    private static final int MAX_TASK_NUM = 50;//最大任务数量
    private static final String TASK_CODE = "BankRecAutoPushBillNewTask";//任务编码

    @Autowired
    BankreconciliationService bankreconciliationService;

    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    @Resource
    private BusinessGenerateFundNewService businessGenerateFundNewService;

    @Autowired
    private IBankrecRuleEngineService iBankrecRuleEngineService;


    /**
     * 银行对账单 自动推单资金调度等接口
     *
     * @param params 自动推单生单参数
     * @return 任务执行结果
     */
    @Override
    public CtmJSONObject autoPush(CtmJSONObject params) {
        final String tenantId = params.getString("tenantId");
        CtmJSONObject result = new CtmJSONObject();
        String key = "" + TASK_CODE + tenantId;
        YmsLock ymsLock = null;
        if ((ymsLock = JedisLockUtils.lockBillWithOutTrace(key)) == null) {
            TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00031", "已有任务正在处理，请稍后重试！") /* "已有任务正在处理，请稍后重试！" */, TaskUtils.UPDATE_TASK_LOG_URL);
            //通知调度任务
            result.put("status", 0);
            return result;
        }
        final Long mainid = Thread.currentThread().getId();
        log.error("根据辨识规则生单任务主线程：" + mainid + ",params:" + CtmJSONObject.toJSONString(params));
        //全部对账单
        List<BankReconciliation> allPushData = getBankReconciliationList(params);
        // 如果未查询出需要执行的数据，返回信息
        if (allPushData == null || allPushData.size() == 0) {
            TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, params.getString("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00032", "执行成功") /* "执行成功" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00035", "未查询到需要生单的数据！") /* "未查询到需要生单的数据！" */, TaskUtils.UPDATE_TASK_LOG_URL);
            //通知调度任务
            result.put("status", 0);
            return result;
        }
        String maxProcessSizeStr = com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(params.getString("singleProcessNum")) ? "" + DEFAULT_MAX_PROCESS_BILL_COUNT : params.getString("singleProcessNum");
        int maxProcessSize = Integer.parseInt(maxProcessSizeStr);
        //任务个数计算
        int taskNum = 1;
        if (allPushData.size() > maxProcessSize) {
            taskNum = allPushData.size() % maxProcessSize == 0 ? allPushData.size() / maxProcessSize : (allPushData.size() / maxProcessSize) + 1;
        }
        log.error("根据辨识规则生单任务主线程：" + mainid + ",执行总数量:" + allPushData.size() + ",单次数量：" + maxProcessSizeStr + "任务个数：" + taskNum);
        //任务个数上限300
        if (taskNum > MAX_TASK_NUM) {
            taskNum = MAX_TASK_NUM;
            log.error("根据辨识规则生单任务主线程：" + mainid + ",任务个数上限超300,超出300任务不再执行！");
        }
        final CountDownLatch taskCountDown = new CountDownLatch(taskNum);
        for (int j = 0; j < taskNum; j++) {
            int fromIndex = j * maxProcessSize;
            int toIndex = j * maxProcessSize + maxProcessSize;
            if (j + 1 == taskNum) {
                toIndex = allPushData.size();
            }
            List<BankReconciliation> taskBills = allPushData.subList(fromIndex, toIndex);
            //根据规则匹配，并推送事件中心
            if (j + 1 == taskNum) {
                YmsLock finalYmsLock1 = ymsLock;
                executorServicePool.getThreadPoolExecutor().submit(() -> {
                    executePushBill(taskBills, tenantId, mainid, taskCountDown, true, finalYmsLock1);
                });
            } else {
                YmsLock finalYmsLock = ymsLock;
                executorServicePool.getThreadPoolExecutor().submit(() -> {
                    executePushBill(taskBills, tenantId, mainid, taskCountDown, false, finalYmsLock);
                });
            }
        }
        //通知任务执行成功
        TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00032", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        //通知调度任务
        result.put("status", 1);
        return result;
    }

    private void setCounterpartByDetail(List<BankReconciliation> allPushData) throws Exception {
        if (CollectionUtils.isNotEmpty(allPushData)) {
            List<Object> idList = allPushData.stream().map(recordBill -> recordBill.getId()).collect(Collectors.toList());
            List<BankReconciliationDetail> bankReconciliationDetails = getBankReconciliationDetails(idList);
            HashMap<Object, List<BankReconciliationDetail>> detailMap = new HashMap<>();
            for (BankReconciliationDetail brVo : bankReconciliationDetails) {
                if (detailMap.containsKey(brVo.getMainid())) {
                    detailMap.get(brVo.getMainid()).add(brVo);
                } else {
                    List<BankReconciliationDetail> list = new ArrayList<>();
                    list.add(brVo);
                    detailMap.put(brVo.getMainid(), list);
                }
            }
            for (int i = 0; i < allPushData.size(); i++) {
                // 封装id集合
                BankReconciliation bankReconciliation = allPushData.get(i);
                Object id = bankReconciliation.getId();
                List<BankReconciliationDetail> bankReconciliationDetail = detailMap.get(id);
                // 设置业务人员财务人员日期
                setAssignBankData(bankReconciliation, bankReconciliationDetail);
            }
            //updateListBankReconciliation(allPushData);
        }

    }

    private void setAssignBankData(BankReconciliation bankReconciliation, List<BankReconciliationDetail> details) {
        if (CollectionUtils.isNotEmpty(details)) {
            String counterpart = "";
            String busscounterpart = "";
            Date busiDate = null;
            for (int i = 0; i < details.size(); i++) {
                BankReconciliationDetail detail = details.get(i);
                String oprtype = detail.getOprtype();
                String autheduser = detail.getAutheduser();
                if (busiDate == null) {
                    busiDate = detail.getOprdate();
                } else {
                    if (DateUtils.dateCompare(detail.getOprdate(), busiDate) > 0) {
                        busiDate = detail.getOprdate();
                    }
                }
                //分派
                if (OprType.AutoFinance.getValue().equals(oprtype) || OprType.ManualFinance.getValue().equals(oprtype)) {
                    if (StringUtils.isNotBlank(autheduser)) {
                        if (StringUtils.isBlank(counterpart)) {
                            counterpart = autheduser;
                        } else {
                            counterpart = counterpart + "," + autheduser;
                        }
                    }
                } else {
                    if (StringUtils.isNotBlank(autheduser)) {
                        if (StringUtils.isBlank(busscounterpart)) {
                            busscounterpart = autheduser;
                        } else {
                            busscounterpart = busscounterpart + "," + autheduser;
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(counterpart)) {
                bankReconciliation.setCounterpart(counterpart);
            }
            if (StringUtils.isNotBlank(busscounterpart)) {
                bankReconciliation.setBusscounterpart(busscounterpart);
            }
            if (busiDate != null) {
                bankReconciliation.put("distributebusdate", busiDate);
            }
        }
    }

    private List<BankReconciliationDetail> getBankReconciliationDetails(List<Object> ids) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,mainid,autheduser,returndate,return_reason,oprtype,group,operator,oprdate,pubts");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("mainid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, querySchema, null);
    }

    /**
     * 查询满足推送条件的银行对账单
     * 未执行过自动生单任务
     * 业务关联状态为是且关联次数等于1，且生单状态为“正常”才会自动生单
     * 超过3年+1月的也可生单
     */
    private List<BankReconciliation> getBankReconciliationList(CtmJSONObject params) {
        String maxQuerySizeStr = com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(params.getString("maxQueryNum")) ? "" + DEFAULT_MAX_QUERY_BILL_COUNT : params.getString("maxQueryNum");
        int maxQuerySize = Integer.parseInt(maxQuerySizeStr);
        //查询数据上限30000
        if (maxQuerySize > MAX_QUERY_BILL_NUM) {
            maxQuerySize = MAX_QUERY_BILL_NUM;
        }
        // 会计主体
        String accentity = params.getString("accentity");
        QuerySchema querySchema = QuerySchema.create().addSelect("id,bank_seq_no,pubts");
        QueryConditionGroup condition = new QueryConditionGroup();

        // 添加会计主体作为查询条件
        if (accentity != null && !"".equals(accentity)) {
            condition.addCondition(QueryCondition.name("accentity").eq(accentity));
        }

        // 已经执行生单任务的不再执行
        condition.addCondition(QueryCondition.name("isautocreatebill").eq(0));
        condition.addCondition(QueryCondition.name("frozenstatus").eq(FrozenStatus.Normal.getValue()));
        condition.addCondition(QueryCondition.name("billprocessflag").eq(BillProcessFlag.NeedDeal.getValue()));
//        condition.addCondition(QueryCondition.name("isreturned").eq(false));//CZFW-85203若业务人员退回后，不应该自动生单
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("associationstatus").eq(AssociationStatus.NoAssociated.getValue())//关联状态为否
        );
        //疑重的不进行查询
//        condition.addCondition(QueryConditionGroup.or(QueryCondition.name("isrepeat").eq(0),
//                QueryCondition.name("isrepeat").is_null()));
        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("associationcount").eq(1), //关联次数为1
                QueryCondition.name("associationstatus").eq(AssociationStatus.Associated.getValue())//关联状态为是
        );
        QueryConditionGroup orGroup = QueryConditionGroup.or(group, group2);
        condition.addCondition(orGroup);
        querySchema.addCondition(condition);
        querySchema.addPager(0, maxQuerySize);
        try {
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            // 流水正在处理中的需要过滤
            return bankreconciliationService.filterBankReconciliationByLockKey(bankReconciliationList, BankReconciliationActions.AutoBillGeneration);
        } catch (Exception e) {
            log.error("获取对账单列表错误" + e);
            return new ArrayList<>();
        }
    }

    private List<BankReconciliation> getBankReconciliationDetailList(List<BankReconciliation> bankrecList) {
        List<BankReconciliation> returnList = new ArrayList<BankReconciliation>();
        List idList = bankrecList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        int queryCount = 1;
        if (idList.size() > DEFAULT_MAX_SUB_PROCESS_BILL_COUNT) {
            queryCount = idList.size() % DEFAULT_MAX_SUB_PROCESS_BILL_COUNT == 0 ? idList.size() / DEFAULT_MAX_SUB_PROCESS_BILL_COUNT : (idList.size() / DEFAULT_MAX_SUB_PROCESS_BILL_COUNT) + 1;
        }
        for (int j = 0; j < queryCount; j++) {
            int fromIndex = j * DEFAULT_MAX_SUB_PROCESS_BILL_COUNT;
            int toIndex = j * DEFAULT_MAX_SUB_PROCESS_BILL_COUNT + DEFAULT_MAX_SUB_PROCESS_BILL_COUNT;
            if (j + 1 == queryCount) {
                toIndex = idList.size();
            }
            List ids = idList.subList(fromIndex, toIndex);
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryCondition.name("id").in(ids));
            querySchema.addCondition(condition);
            try {
                List<BankReconciliation> queryList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                returnList.addAll(queryList);
            } catch (Exception e) {
                log.error("获取对账单列表错误" + e);
            }
        }
        return returnList;
    }

    /**
     * 根据推送数据，匹配推送规则，将数据推送到事件中心
     */
    private void executePushBill(List<BankReconciliation> data, String tenantId, Long mainid, CountDownLatch taskCountDown, Boolean isLastTask, YmsLock ymsLock) {
        List<BankReconciliation> pushData = getBankReconciliationDetailList(data);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        log.error("根据辨识规则生单任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + ",处理数量：" + pushData.size() + "开始时间：" + df.format(System.currentTimeMillis()));
        boolean pushbill = true;
        try {
            //赋值对接人
            setCounterpartByDetail(pushData);
            //调用生单规则
            iBankrecRuleEngineService.executeRuleEngine(pushData, CmpRuleEngineTypeConstant.cmp_generate, true);
        } catch (Exception e) {
            log.error("根据辨识规则生单任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "调用自动生单规则失败:" + e.getMessage(), e);
            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00033", "调用自动生单规则失败，请检查以cmp_generate开头的自动生单规则是否存在或者是否启用！") /* "调用自动生单规则失败，请检查以cmp_generate开头的自动生单规则是否存在或者是否启用！" */);
            pushbill = false;
        }
        boolean pushSuccess = true;
        if (pushbill) {
            //推生现金管理资金收款款单
            try {
                businessGenerateFundNewService.bankreconciliationGenerateDoc(pushData, false);
            } catch (Exception e) {
                log.error("根据辨识规则生单任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "自动生单异常" + e);
                pushSuccess = false;
            }
        }
        List nos = new ArrayList();
        if (pushSuccess) {
            nos = pushData.stream().map(BankReconciliation::getBank_seq_no).collect(Collectors.toList());
        }
        log.error("根据辨识规则生单任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "结束时间：" + df.format(System.currentTimeMillis()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00034", "执行成功数据交易流水号：") /* "执行成功数据交易流水号：" */ + nos);
        //任务执行结束，计数减一
        taskCountDown.countDown();
        //若是最后一个子任务，等待计数为0时释放任务锁，若不是不需要等待，直接清空上下文后结束
        if (isLastTask) {
            try {
                taskCountDown.await();
            } catch (InterruptedException e) {
                log.error("根据辨识规则生单任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "计数被打断");
            } finally {
                InvocationInfoProxy.reset();
                String key = "" + TASK_CODE + tenantId;
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        log.error("根据辨识规则生单任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "结束时间：" + df.format(System.currentTimeMillis()));
        InvocationInfoProxy.reset();
    }

}
