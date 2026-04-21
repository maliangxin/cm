package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationAutomaticStayService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.IBankrecRuleEngineService;
import com.yonyoucloud.fi.cmp.cmpentity.FrozenStatus;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @Author zhangxiaojun
 * @Date 2022/10/10 17:08
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankreconciliationAutomaticStayImpl implements BankreconciliationAutomaticStayService {

    private static final String BANKRECONCILIATIONMAPPER = "com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper";
    private static final int DEFAULT_MAX_PROCESS_BILL_COUNT = 500;//调度任务子线程一次默认处理单据数量
    private static final int DEFAULT_MAX_QUERY_BILL_COUNT = 7000;//调度任务一次默认查询单据数量
    private static final int MAX_QUERY_BILL_NUM = 20000;//任务一次最多查询单据数量
    private static final int MAX_TASK_NUM = 50;//最大任务数量
    private static final String TASK_CODE = "autoMaticStay";//任务编码
    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    private IBankrecRuleEngineService ruleEngineService;

    @Autowired
    BankreconciliationService bankreconciliationService;


    /**
     * 银行对账单自动冻结调度任务
     *
     * @return 执行结果
     */
    @Override
    public CtmJSONObject automaticStay(CtmJSONObject params) throws Exception {
        final String tenantId = params.getString("tenantId");
        CtmJSONObject result = new CtmJSONObject();
        String key = "" + TASK_CODE + tenantId;
        YmsLock ymsLock = null;
        if ((ymsLock = JedisLockUtils.lockBillWithOutTrace(key)) == null) {
            TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001C1", "已有任务正在处理，请稍后重试！") /* "已有任务正在处理，请稍后重试！" */, TaskUtils.UPDATE_TASK_LOG_URL);
            //通知调度任务
            result.put("status", 0);
            return result;
        }
        final Long mainid = Thread.currentThread().getId();
        log.error("根据辨识规则冻结任务主线程：" + mainid + ",params:" + CtmJSONObject.toJSONString(params));
        //获取符合条件的对账单
        List<BankReconciliation> data = getBankReconciliationList(params);
        if (data == null || CollectionUtils.isEmpty(data)) {
            //通知任务执行成功
            TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180599", "执行成功") /* "执行成功" */ + "未找到需要冻结的对账单", TaskUtils.UPDATE_TASK_LOG_URL);
            return result;
        }
        String maxProcessSizeStr = StringUtils.isEmpty(params.getString("singleProcessNum")) ? "" + DEFAULT_MAX_PROCESS_BILL_COUNT : params.getString("singleProcessNum");
        int maxProcessSize = Integer.parseInt(maxProcessSizeStr);
        log.error("根据辨识规则冻结任务主线程：" + mainid + ",执行总数量:" + data.size() + ",单次数量：" + maxProcessSizeStr);
        //每个任务提交单据数量小于等于maxProcessSize
        //任务个数计算
        int taskNum = 1;
        if (data.size() > maxProcessSize) {
            taskNum = data.size() % maxProcessSize == 0 ? data.size() / maxProcessSize : (data.size() / maxProcessSize) + 1;
        }
        //任务个数上限300
        if (taskNum > MAX_TASK_NUM) {
            taskNum = MAX_TASK_NUM;
        }
        final CountDownLatch taskCountDown = new CountDownLatch(taskNum);
        for (int j = 0; j < taskNum; j++) {
            int fromIndex = j * maxProcessSize;
            int toIndex = j * maxProcessSize + maxProcessSize;
            if (j + 1 == taskNum) {
                toIndex = data.size();
            }
            List<BankReconciliation> taskBills = data.subList(fromIndex, toIndex);
            //根据规则匹配，并推送事件中心
            if (j + 1 == taskNum) {
                YmsLock finalYmsLock = ymsLock;
                executorServicePool.getThreadPoolExecutor().submit(() -> {
                    executFrozenBill(taskBills, tenantId, mainid, taskCountDown, true, finalYmsLock);
                });
            } else {
                YmsLock finalYmsLock1 = ymsLock;
                executorServicePool.getThreadPoolExecutor().submit(() -> {
                    executFrozenBill(taskBills, tenantId, mainid, taskCountDown, false, finalYmsLock1);
                });
            }
        }
        //通知任务执行成功
        TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001C2", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        //通知调度任务
        result.put("status", 1);
        return result;
    }

    private void executFrozenBill(List<BankReconciliation> data, String tenantId, Long mainid, CountDownLatch taskCountDown, Boolean isLastTask, YmsLock ymsLock) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        log.error("根据辨识规则冻结任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + ",处理数量：" + data.size() + ",开始时间：" + df.format(System.currentTimeMillis()));
        boolean freezebill = true;
        // 执行对账单冻结规则
        try {
            ruleEngineService.executeRuleEngine(data, CmpRuleEngineTypeConstant.cmp_freeze, true);
        } catch (Exception e) {
            //通知任务执行失败
            log.error("根据辨识规则冻结任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "冻结规则执行失败：" + e.getMessage());
            freezebill = false;
        }
        if (freezebill) {
            //批量修改冻结状态
            try {
                for (BankReconciliation bankReconciliation : data) {
                    if (FrozenStatus.Frozen.getValue() == bankReconciliation.getFrozenstatus()) {
                        bankReconciliation.setFrozencount(null == bankReconciliation.getFrozencount() ? 1 : bankReconciliation.getFrozencount() + 1);
                    }
                }
                updateBatchFrozenStatus(data);
            } catch (Exception e) {
                //通知任务执行失败
                log.error("根据辨识规则冻结任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "更新冻结字段执行失败：" + e.getMessage());
            }
        }
        //任务执行结束，计数减一
        taskCountDown.countDown();
        //若是最后一个子任务，等待计数为0时释放任务锁，若不是不需要等待，直接清空上下文后结束
        if (isLastTask) {
            try {
                taskCountDown.await();
            } catch (InterruptedException e) {
                log.error("根据辨识规则冻结任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "计数被打断");
            } finally {
                InvocationInfoProxy.reset();
                String key = "" + TASK_CODE + tenantId;
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        log.error("根据辨识规则冻结任务主线程：" + mainid + "，子线程：" + Thread.currentThread().getId() + "结束时间：" + df.format(System.currentTimeMillis()));
        InvocationInfoProxy.reset();
    }

    //修改对账单冻结状态
    private void updateBatchFrozenStatus(List<BankReconciliation> list) throws Exception {
        try {
            SqlHelper.update(BANKRECONCILIATIONMAPPER + ".batchUpdateFrozenstatus", list);
        } catch (Exception e) {
            log.error("批量修改对账单冻结状态失败" + e);
        }

    }

    /**
     * 查询银行流水
     */
    private List<BankReconciliation> getBankReconciliationList(CtmJSONObject params) {
        String maxQuerySizeStr = StringUtils.isEmpty(params.getString("maxQueryNum")) ? "" + DEFAULT_MAX_QUERY_BILL_COUNT : params.getString("maxQueryNum");
        int maxQuerySize = Integer.parseInt(maxQuerySizeStr);
        //查询数据上限30000
        if (maxQuerySize > MAX_QUERY_BILL_NUM) {
            maxQuerySize = MAX_QUERY_BILL_NUM;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id,bank_seq_no,frozenstatus,pubts");
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("frozenstatus").eq(0),  //冻结状态为0正常
                QueryConditionGroup.or(QueryCondition.name("frozencount").eq(0), QueryCondition.name("frozencount").is_null()), //冻结次数为0正常
                QueryCondition.name("associationstatus").eq(0), //关联状态为未关联
                QueryCondition.name("isrunidentify").eq(1),//已执行辨识规则
                QueryCondition.name("billprocessflag").eq(1)//回单处理标识-需要回单中台
        );
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("frozenstatus").eq(0),  //冻结状态为0正常
                QueryConditionGroup.or(QueryCondition.name("frozencount").eq(0), QueryCondition.name("frozencount").is_null()), //冻结次数为0正常
                QueryCondition.name("associationstatus").eq(1), //关联状态为已关联
                QueryCondition.name("associationcount").eq(1), //关联次数为1
                QueryCondition.name("isrunidentify").eq(1),//已执行辨识规则
                QueryCondition.name("billprocessflag").eq(1)//回单处理标识-需要回单中台
        );
        QueryConditionGroup group = QueryConditionGroup.or(group1, group2);
        querySchema.addPager(0, maxQuerySize);
        querySchema.addCondition(group);
        try {
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            // 流水正在处理中的需要过滤
            return bankreconciliationService.filterBankReconciliationByLockKey(bankReconciliationList, BankReconciliationActions.AutoFrozen);
        } catch (Exception e) {
            log.error("获取对账单数据错误" + e);
            return new ArrayList<>();
        }
    }
}
