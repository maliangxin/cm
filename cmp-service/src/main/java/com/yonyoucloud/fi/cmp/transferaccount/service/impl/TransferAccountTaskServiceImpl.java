
package com.yonyoucloud.fi.cmp.transferaccount.service.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankenterprise.InternetBankPayService;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountTaskService;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class TransferAccountTaskServiceImpl implements TransferAccountTaskService {

    @Autowired
    private HttpsService httpsService;
    @Autowired
    InternetBankPayService internetBankPayService;
    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Override
    public Map queryPayStatus(CtmJSONObject params) {
        Map result = new HashMap();
        //异步调用，执行付款单自动结算任务
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                doQueryPayStatus(params);
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,params.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00113", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,params.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("transferAccountTask exception when batch process executorServicePool", e);
            }
        });
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    private void doQueryPayStatus(CtmJSONObject param) throws Exception {
        Integer batchNum = param.getInteger("queryDaysNum");
        // 查询cm_transfer_account表数据，进行遍历，先查*，后续再查具体字段
        QuerySchema querySettingSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 自动查询支付状态为“支付中、支付不明”的转账单的支付状态
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("paystatus").in(PayStatus.Paying.getValue(), PayStatus.PayUnknown.getValue())));
        // 只查询支付日期5天前的数据，其他不处理
        LocalDateTime localDate = LocalDate.now().plusDays(-30).atStartOfDay();
        Date date = DateUtil.localDateTime2Date(localDate);
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("auditDate").gt(date)));
        querySettingSchema.addCondition(conditionGroup);
        List<Map<String, Object>> transferAccountList = MetaDaoHelper.query(TransferAccount.ENTITY_NAME,querySettingSchema);
        for (Map<String, Object> transferAccountMap : transferAccountList) {
            String requestseqno = (String) transferAccountMap.get("requestseqno");
            if (requestseqno != null && !"".equals(requestseqno)) {
                String payBankAccount = transferAccountMap.get("payBankAccount").toString();
                QuerySchema bankAccountSettingQuerySchema = QuerySchema.create().addSelect("customNo");
                bankAccountSettingQuerySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(payBankAccount)));
                List<Map<String, Object>> bankAccountSettingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, bankAccountSettingQuerySchema);
                if (CollectionUtils.isEmpty(bankAccountSettingList)) {
                    log.error("转账工作台查询支付状态推送报文：未查询到bankaccountsetting的数据");
                    continue;
                }
                String customNo = (String) bankAccountSettingList.get(0).get("customNo");
                param.put("customNo", customNo);
                param.put("oldRequestSeqNo", requestseqno);
                param.put("requestseqno", requestseqno);
                CtmJSONObject postMsg = internetBankPayService.buildPayStatusQueryMsg(param);
                log.error("转账工作台查询支付状态推送报文：" + postMsg.toString());
                CtmJSONObject postResult = httpsService.doHttpsPost(BATCH_PAY_DETAIL_STATUS_QUERY, postMsg, bankConnectionAdapterContext.getChanPayUri(), null);
                log.error("转账工作台查询支付状态接收报文：" + postResult.toString());
                if (!"0000".equals(postResult.getString("code"))) {
                    continue;
                }
                CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
                String serviceStatus = responseHead.getString("service_status");
                if (!("00").equals(serviceStatus)) {
                    continue;
                }
                CtmJSONObject response_body = postResult.getJSONObject("data").getJSONObject("response_body");
                if (response_body != null || response_body.isEmpty()) {
                    try {
                        internetBankPayService.analysisPayStatusQueryRespData(postResult.getJSONObject("data").getJSONObject("response_body"));
                    } catch (Exception e) {
                        log.error("转账工作台查询支付状态异常: " + responseHead.getString("service_resp_code") + ", " + responseHead.getString("service_resp_desc"));
                    }
                }
            }
        }
    }

}
