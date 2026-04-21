package com.yonyoucloud.fi.cmp.task.payapplybill;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.payapplicationbill.ApprovalStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * <h1>定时任务：每天更新付款申请单距离期望付款日期天数</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-01-07 15:16
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class ChangePayApplyDateDaysImpl implements ChangePayApplyDateDays {

    private static final Integer ZERO = 0;
    private static final Integer SIZE = 30;

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    /**
     * <h2>每天更新付款申请单距离期望付款日期天数-定时任务</h2>
     *
     * @param paramMap
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/1/11 10:51
     */
    @Override
    public Map<String, Object> updateDistanceProposePaymentDateDaysTask(Map<String, Object> paramMap) throws Exception {
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                Date date = new Date();
                String dateToStr = DateUtils.dateToStr(date);
                Date strToDate = DateUtils.strToDate(dateToStr);
                QueryConditionGroup group = QueryConditionGroup.and(
                        QueryCondition.name("unpaidAmountSum").gt(BigDecimal.ZERO),
                        QueryCondition.name("approvalStatus").eq(ApprovalStatus.ApprovedPass.getValue()),
                        QueryCondition.name("proposePaymentDate").is_not_null(),
                        QueryCondition.name("proposePaymentDate").egt(strToDate)
                );
                querySchema.addCondition(group);
                List<PayApplicationBill> payApplicationBill_list = MetaDaoHelper.queryObject(PayApplicationBill.ENTITY_NAME, querySchema, null);
                List<PayApplicationBill> payApplicationBill_update = new ArrayList<>();
                // 分批次批量更新数据
                payApplicationBill_list.forEach(e -> {
                    Date proposePaymentDate = e.getProposePaymentDate();
                    long days =calculateDaysGap(new Date(), proposePaymentDate) +1L;
                    if (days < 0L){
                        days = 0L;
                    }
                    if (e.getDistanceProposePaymentDateDays() != days) {
                        e.setDistanceProposePaymentDateDays(Integer.parseInt(String.valueOf(days)));
                        payApplicationBill_update.add(e);
                    }
                    if (payApplicationBill_update.size() == SIZE) {
                        try {
                            EntityTool.setUpdateStatus(payApplicationBill_update);
                            MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill_update);
                            payApplicationBill_update.clear();
                        } catch (Exception exception) {
                            log.error("updateDistanceProposePaymentDateDaysTask info ---------------更新付款申请单距离期望付款日期天数失败");
                        }
                    }
                });
                if (payApplicationBill_update.size() > ZERO) {
                    try {
                        EntityTool.setUpdateStatus(payApplicationBill_update);
                        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill_update);
                        payApplicationBill_update.clear();
                    } catch (Exception exception) {
                        log.error("updateDistanceProposePaymentDateDaysTask info ---------------更新付款申请单距离期望付款日期天数失败");
                    }
                }
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180081","执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180082","执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("queryAccountBalanceTask exception when batch process executorServicePool", e);
            }
        });
        Map<String, Object> retMap = new HashMap<>(16);
        retMap.put("asynchronized", "true");
        return retMap;
    }

    private static long calculateDaysGap(Date start, Date end) {
        final long ONE_DAY_MILLIS = 1000L * 60 * 60 * 24;    // 此处要注意，去掉时分秒的差值影响，此处采用先换算为天再相减的方式
        return end.getTime()/ONE_DAY_MILLIS - start.getTime()/ONE_DAY_MILLIS;
    }
}
