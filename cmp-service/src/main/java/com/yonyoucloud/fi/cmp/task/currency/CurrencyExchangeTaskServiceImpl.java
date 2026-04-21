package com.yonyoucloud.fi.cmp.task.currency;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryType;
import com.yonyoucloud.fi.cmp.cmpentity.ExchangeType;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class CurrencyExchangeTaskServiceImpl implements CurrencyExchangeTaskService {
    private static final String YONSUITE_AUTOTASK = "Yonsuite_AutoTask";
    private static final String AUTO_PAY_IDEN = "Y";
    private static final Cache<String, CurrencyTenantDTO> currencyCache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).concurrencyLevel(4).maximumSize(1000).softValues().build();

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;

    @Autowired
    CurrencyExchangeService currencyExchangeService;

    @Override
    public Map<String, Object> queryResult(Map<String, Object> paramMap) throws Exception {
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                doQueryResult(paramMap);
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,paramMap.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001E2", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001E1", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("currencyexchangetask.resultQuery exception when batch process executorServicePool", e);
            }
        });
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", "true");
        return retMap;
    }

    private void doQueryResult(Map<String,Object> paramMap) throws Exception {
        QuerySchema queryCurrencyExchangeSchema = QuerySchema.create().addSelect("id, purchasebankaccount, sellbankaccount,exchangetype");
        // 交割方式：直连交割， 交割状态：处理中/待交割
        queryCurrencyExchangeSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("deliveryType").eq(DeliveryType.DirectDelivery.getValue())));
        queryCurrencyExchangeSchema.addCondition(QueryConditionGroup.or(QueryCondition.name("settlestatus").eq(DeliveryStatus.doingDelivery.getValue()),QueryCondition.name("settlestatus").eq(DeliveryStatus.waitDelivery.getValue())));
        List<Map<String, Object>> currencyExchangeList = MetaDaoHelper.query(CurrencyExchange.ENTITY_NAME, queryCurrencyExchangeSchema);
        for (Map currencyExchangeMap : currencyExchangeList) {
            QuerySchema queryBankAccountSchema = QuerySchema.create().addSelect("customNo");
            if (ExchangeType.Sell.getValue() == Short.parseShort(currencyExchangeMap.get("exchangetype").toString())) {
                queryBankAccountSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(currencyExchangeMap.get("sellbankaccount"))));
            } else {
                queryBankAccountSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(currencyExchangeMap.get("purchasebankaccount"))));
            }
            queryBankAccountSchema.addCondition(QueryConditionGroup.and(QueryCondition.name("customNo").is_not_null()));
            List<Map<String, Object>> bankAccountSettingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, queryBankAccountSchema);
            if (CollectionUtils.isEmpty(bankAccountSettingList)) {
                continue;
            }
            String customNo = (String) bankAccountSettingList.get(0).get("customNo");
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("customNo", customNo);
            jsonObject.put("id", currencyExchangeMap.get("id"));
            try {
                currencyExchangeService.currencyExchangeResultQuery(jsonObject);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}