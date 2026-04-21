package com.yonyoucloud.fi.cmp.autosettle;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillService;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2022/12/12 10:29
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmpAutoSettleTaskServiceImpl implements CmpAutoSettleTaskService {

    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    @Resource
    private PaymentService paymentService;

    @Resource
    private ReceiveBillService receiveBillService;

    @Override
    public CtmJSONObject payBillAutoSettle(CtmJSONObject params) {
        CtmJSONObject result = new CtmJSONObject();
        Integer batchNum = params.getInteger("batchNum");

        //异步调用，执行付款单自动结算任务
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            executePayBillAutoSettle(batchNum);
        });

        //通知任务执行成功
        TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022F", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    @Override
    public CtmJSONObject receiveBillAutoSettle(CtmJSONObject params) {
        CtmJSONObject result = new CtmJSONObject();
        Integer batchNum = params.getInteger("batchNum");

        //异步调用，执行收款单自动结算任务
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            executeReceiveBillAutoSettle(batchNum);
        });

        //通知任务执行成功
        TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022F", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    /**
     * 付款单自动结算任务
     */
    private void executePayBillAutoSettle(Integer batchNum) {
        //查询已审核未结算的付款单
        List<PayBill> payBillList = getPayBillList(batchNum);
        //循环处理结算，将数据更改为线下支付成功
        for (PayBill payBill : payBillList){
            CtmJSONObject params = new CtmJSONObject();
            List<CtmJSONObject> rows = new ArrayList<>();
            rows.add(CtmJSONObject.parseObject(JsonUtils.toJson(payBill)));
            params.put("rows", rows);
            try {
                paymentService.offLinePay(params);
            } catch (Exception e) {
                log.error("付款单自动结算异常" + e);
            }
        }

    }

    /**
     * 收款单自动结算任务
     */
    private void executeReceiveBillAutoSettle(Integer batchNum) {
        //获取已审批，未结算的数据
        List<ReceiveBill> receiveBillList = getReceiveBillList(batchNum);
        //调用自动结算接口
        for (ReceiveBill receiveBill : receiveBillList){
            List<ReceiveBill> toSettleList  = new ArrayList<>();
            toSettleList.add(receiveBill);
            try {
                receiveBillService.settle(toSettleList);
            } catch (Exception e) {
                log.error("收款单自动结算异常" + e);
            }
        }

    }

    /**
     * 根据条件查询已审批，未结算的付款单
     *
     * @return
     */
    private List<PayBill> getPayBillList(Integer batchNum) {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                //已审批
                QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()),
                //未结算
                QueryCondition.name("settlestatus").eq(SettleStatus.noSettlement.getValue()) ,
                //待支付
                QueryCondition.name("paystatus").eq(PayStatus.NoPay.getValue()),
                //非直联
                QueryCondition.name("isdirectconn").eq(false)
        );
        querySchema.addCondition(group);
        //最大数量
        querySchema.addPager(0,batchNum);
        //根据单据日期倒序
        querySchema.addOrderBy(new QueryOrderby("vouchdate", "desc"));
        try {
            return MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取付款单列表错误" + e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据条件查询已审批，未结算的收款单
     *
     * @return
     */
    private List<ReceiveBill> getReceiveBillList(Integer batchNum) {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()),//已审批
                QueryCondition.name("settlestatus").eq(SettleStatus.noSettlement.getValue()) //未结算
        );
        querySchema.addCondition(group);
        //最大数量
        querySchema.addPager(0,batchNum);
        //根据单据日期倒序
        querySchema.addOrderBy(new QueryOrderby("vouchdate", "desc"));
        try {
            return MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取收款单款单列表错误" + e);
            return new ArrayList<>();
        }
    }
}
