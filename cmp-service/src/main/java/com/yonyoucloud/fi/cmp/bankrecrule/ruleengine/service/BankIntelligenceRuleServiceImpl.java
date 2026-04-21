package com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.service;


import com.google.common.collect.Lists;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.ruleengine.relevant.RelevantRuleLoadService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.BusinessGenerateFundNewService;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.BankIntelligenceRuleService;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.imp.RuleStrategy;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @description: 银行对账单智能规则执行接口
 * @author: wangdengk@yonyou.com
 * @date: 2023/06/13 11:40
 */
@Slf4j
@Service
public class BankIntelligenceRuleServiceImpl implements BankIntelligenceRuleService {
    @Resource
    YmsOidGenerator ymsOidGenerator;
    @Resource
    IdentifyingRuleStrategy identifyingRuleStrategy;
    @Resource
    private BusinessGenerateFundNewService businessGenerateFundNewService;
    @Autowired
    public RelevantRuleLoadService relevantRuleLoadService;
    protected final String bizObjectCode = "ctm-cmp.cmp_bankreconciliation";
    public static final int CMP_RULE_TASK_GROUP_SIZE = 1000; // 规则任务分组大小

    private static ExecutorService executorService;
    static{
        String maxSize = AppContext.getEnvConfig("cmp.generatebill.max.poolSize", "10");
        String coreSize = AppContext.getEnvConfig("cmp.generatebill.max.coreSize", "10");
        String queueLength = AppContext.getEnvConfig("cmp.generatebill.queueCapacity", "5000");
        //如果前端没有配置此参数 那么依旧采用yms控制台配置逻辑
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(Integer.parseInt(coreSize), Integer.parseInt(maxSize), Integer.parseInt(queueLength), "cmp-generatebill-async-");
    }


    /**
     * 获取满足辨识规则的银行对账单的数据
     * 未辨识 辨识未命中的数据
     *
     * @param paramMap
     * @return
     */
    private List<BankReconciliation> getNeedIdentificationList(CtmJSONObject paramMap) throws Exception {
        QueryConditionGroup condition = new QueryConditionGroup();
        // 获取后台任务的执行区间
//        if (null != paramMap && null != paramMap.getInteger("daterange")) {
//            int dateRange = paramMap.getInteger("daterange").intValue();
//            Date beforeDate = DateUtils.dateAddDays(DateUtils.getNowDate(), dateRange * (-1));
//            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_date")
//                    .egt(DateUtils.dateFormat(beforeDate, null))));
//            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_date")
//                    .elt(DateUtils.getTodayShort())));
//        }
        paramMap.put("daysinadvance",paramMap.getInteger("daterange"));
        HashMap<String, String> querydate = TaskUtils.queryDateProcess(paramMap, "yyyy-MM-dd");
        Date startDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
        Date endDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
        int days = DateUtils.dateBetween(startDate, endDate);
        //调度任务使用他们的前端限制，不在代码中限制
        if (days > 31) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100090"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000C", "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!") /* "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!" */);
        }
        condition.addCondition(QueryCondition.name("tran_date").between(startDate,endDate));
        QuerySchema querySchema = QuerySchema.create().addSelect("id,accentity,serialdealtype,characterDef.*");
        // 业务关联状态为“未关联” 取业务关联状态为未关联的数
        condition.addCondition(QueryCondition.name("associationstatus")
                .eq(AssociationStatus.NoAssociated.getValue()));
        //账户共享，使用组织不能为空 ,20250416 王东方说放开限制
//        condition.appendCondition(QueryCondition.name("accentity").is_not_null());
        querySchema.addCondition(condition);
        //增加按照登账日期倒序查询
        querySchema.addOrderBy(new QueryOrderby("tran_date","desc"));
        List<BankReconciliation> bankRecList = MetaDaoHelper
                .queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return bankRecList;
    }

    /**
     * 银行对账单分批数量进行分组
     *
     * @param sourceList
     * @param groupNum
     * @return
     */
    private List<List<BankReconciliation>> groupBankReconciliationData(List<BankReconciliation> sourceList, int groupNum) {
        List<List<BankReconciliation>> targetList = new ArrayList<>();
        int size = sourceList.size();
        int remainder = size % groupNum;
        int sum = size / groupNum;
        for (int i = 0; i < sum; i++) {
            List<BankReconciliation> subList;
            subList = sourceList.subList(i * groupNum, (i + 1) * groupNum);
            targetList.add(subList);
        }
        if (remainder > 0) {
            List<BankReconciliation> subList;
            subList = sourceList.subList(size - remainder, size);
            targetList.add(subList);
        }
        return targetList;
    }

    /**
     * 银行对账单获取任务队列集合
     *
     * @param lists 银行对账单数组
     * @return
     */
    private List<Callable<List<BankReconciliation>>> getIdentificationTaskList(List<List<BankReconciliation>> lists, String ruleType, String logId) {
        List<Callable<List<BankReconciliation>>> taskList = new ArrayList<>();
        for (List<BankReconciliation> list : lists) {
            Callable<List<BankReconciliation>> task = new Callable<List<BankReconciliation>>() {
                @Override
                public List<BankReconciliation> call() {
                    String content = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F7", "辨识规则：") /* "辨识规则：" */ + "ruleType";
                    try {
                        // 分批处理银行对账单的数据 进行规则辨识
                        identifyingRuleStrategy.executeRule(list, ruleType);
                        for(BankReconciliation bankReconciliation : list){
                            identifyingRuleStrategy.updateBankReconciliation(bankReconciliation);
                        }
                    } catch (Exception e) {
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                                InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114",
                                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F6", "执行失败") /* "执行失败" */) /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                        log.error("getIdentificationTaskList exception when batch process executorServicePool", e);
                    }
                    return list;
                }
            };
            taskList.add(task);
        }
        return taskList;
    }

    @Override
    public CtmJSONObject executeIdentificationRule(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        // 第一步 获取要执行的银行对账单的数据集合
        List<BankReconciliation> data = getNeedIdentificationList(params);
        BankreconciliationUtils.checkAndFilterData(data, BankreconciliationScheduleEnum.INTELLIGENCEIDENTIFICATIONTASK);
        // 第二步 对银行对账单的数据进行分组定义
        List<List<BankReconciliation>> lists = groupBankReconciliationData(data, CMP_RULE_TASK_GROUP_SIZE);
        // 第三步 创建分批执行的任务队列集合
        // 辨识任务
        String logId = params.get("logId").toString();
        List<Callable<List<BankReconciliation>>> taskList = getIdentificationTaskList(lists, RuleStrategy.CMP_IDENTIFICATION_PREFIX, logId);
        // 第四步 获取线程池 异步执行任务队列
        for (Callable<List<BankReconciliation>> task : taskList) {
            executorService.submit(task);
        }
        TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId,
                InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00113",
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F5", "执行成功") /* "执行成功" */) /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    /**
     * 获取满足辨识规则的银行对账单的数据
     * 未辨识 辨识未命中的数据
     *
     * @param paramMap
     * @return
     */
    private List<BankReconciliation> getNeedGenerateBillList(CtmJSONObject paramMap) throws Exception {
        QueryConditionGroup condition = new QueryConditionGroup();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        paramMap.put("daysinadvance",paramMap.getInteger("daterange"));
        HashMap<String, String> querydate = TaskUtils.queryDateProcess(paramMap, "yyyy-MM-dd");
        Date startDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
        Date endDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
        // 获取后台任务的执行区间
        int days = DateUtils.dateBetween(startDate, endDate);
        //调度任务使用他们的前端限制，不在代码中限制
        if (days > 31) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100090"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000C", "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!") /* "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!" */);
        }
        condition.addCondition(QueryCondition.name("tran_date").between(startDate,endDate));
        //任务参数  'earlyentryflag', '提前自动入账标识'   autosubmit  是否自动提交
        if (null != paramMap && null != paramMap.getInteger("earlyentryflag")) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("earlyentryflag")
                    .eq(paramMap.getInteger("earlyentryflag"))));
        }
        if (null != paramMap.get("isadvanceaccounts")) { // 提前入账标识
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("isadvanceaccounts")
                    .eq(0)));
        }
        // 业务关联状态为“未关联” 取业务关联状态为未关联的数据
        condition.addCondition(QueryCondition.name("associationstatus")
                .eq(AssociationStatus.NoAssociated.getValue()));
        // 完结状态为“已完结” 的流水不进行查询，前端也需要处理
        condition.addCondition(QueryCondition.name("serialdealendstate")
                .not_eq(SerialdealendState.END.getValue()));
        // 冻结状态为正常的数据 --20241231去除该控制，字段已废弃
//        condition.addCondition(QueryCondition.name("frozenstatus")
//                .eq(FrozenStatus.Normal.getValue()));
        // 使用生单原查询逻辑
        condition.appendCondition(
                QueryCondition.name("associationstatus").eq(0),
                QueryCondition.name("autoassociation").eq(1), //自动执行关联任务=1
                QueryCondition.name("ispublish").eq(0),//未发布
                QueryCondition.name("checkflag").eq(0),//勾兑标识
                QueryCondition.name("other_checkflag").eq(0),
                QueryCondition.name("tran_amt").gt(BigDecimal.ZERO)
//                ,//总账未勾兑
//                QueryCondition.name("isautocreatebill").eq(0) //未执行自动生单任务
        );
        //账户共享，使用组织不能为空
        condition.appendCondition(QueryCondition.name("accentity").is_not_null());
        querySchema.addCondition(condition);
        //增加按照登账日期倒序查询
        querySchema.addOrderBy(new QueryOrderby("dzdate","desc"));
        List<BankReconciliation> bankRecList = MetaDaoHelper
                .queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return bankRecList;
    }

    /**
     * 重新查询银行对账单数据
     *
     * @param list
     * @throws Exception
     */
    public List<BankReconciliation> getRuleAfterBankData(List<BankReconciliation> list) throws Exception {
//        QuerySchema schema = QuerySchema.create().addSelect("*");
//        List<Long> idList = list.stream().map(item -> (Long) item.get("id")).collect(Collectors.toList());
//        schema.appendQueryCondition(QueryCondition.name("id").in(idList));
//        List<BankReconciliation> resultList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        List<BankReconciliation> resultList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : list){
            if (bankReconciliation.getGenertbilltype() != null){
                resultList.add(bankReconciliation);
            }
        }
        return resultList;
    }

    /**
     * @param list
     * @param ruleType
     * @param logId
     * @param autoSubmit
     */
    private String execute(List<BankReconciliation> list, String ruleType, String logId, boolean autoSubmit) {
        try {
            // 分批处理银行对账单的数据 进行规则辨识
            identifyingRuleStrategy.executeRule(list, ruleType);
//            for(BankReconciliation bankReconciliation : list){
//                identifyingRuleStrategy.updateBankReconciliation(bankReconciliation);
//            }
            // 根据list查询出生单类型不为空的数据进行生单
            List<BankReconciliation> ruleList = getRuleAfterBankData(list);
            if(CollectionUtils.isEmpty(ruleList)){
                StringBuffer retMsg = new StringBuffer();
                return retMsg.toString();
            }
            short entryType = EntryType.Normal_Entry.getValue();
            Boolean isadvanceaccounts = false;//提前入账
            if (ruleType.equals(RuleStrategy.CMP_EARLY_RECORD_PREFIX)) {//如果是提前入账的话入账类型为挂账其他的为正常入账
                entryType = EntryType.Hang_Entry.getValue();
                isadvanceaccounts = true;
            }
            for (BankReconciliation bankReconciliation : ruleList) {
                bankReconciliation.setEntrytype(entryType);
                bankReconciliation.setVirtualEntryType(entryType);//virtualentrytype
                bankReconciliation.setIsadvanceaccounts(isadvanceaccounts);
                if (isadvanceaccounts) {
                    bankReconciliation.setAssociationcount(new Short("1"));//业务关联次数---走提前入账生成默认改为1次，用于第二次手动生单前规则BankSetEntrytypePullBeforeRule修改入账类型为冲挂账
                }
            }
            // 规则执行完成之后 根据规则类型生成收款结算单还是付款结算单
            List<List<BankReconciliation>> partitionedList = Lists.partition(ruleList, 20); // 每组100个元素
            StringBuffer retSb = new StringBuffer();
            for(List<BankReconciliation> newlist : partitionedList){
                retSb.append(businessGenerateFundNewService.bankreconciliationGenerateDoc(newlist, autoSubmit));
            }
            return retSb.toString();
        } catch (Exception e) {
            log.error("getGenerateBillTaskList exception when batch process executorServicePool", e);
            return e.getMessage();
        }
    }

    /**
     * 银行对账单获取生单任务队列集合
     *
     * @param lists      银行对账单数组
     * @param autoSubmit
     * @return
     */
    private List<String> getGenerateBillTaskList(List<List<BankReconciliation>> lists, String ruleType, String logId, boolean autoSubmit) {
        List<Future<String>> futures = new ArrayList<>(lists.size());
        List<String> results = new ArrayList();
        for (List<BankReconciliation> list : lists) {
            Future<String> task = null;
            try {
                task = executorService.submit(() -> {
                    try {
                        return this.execute(list, ruleType, logId, autoSubmit);
                    } catch (Exception e) {
                        log.error("exception when getGenerateBillTaskList", e);
                        return "error";
                    }
                });
            } catch (Exception e) {
                log.error("exception when getGenerateBillTaskList", e);
            }
            futures.add(task);
        }
        futures.stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("future get result failed getGenerateBillTaskList", e);
                return null;
            }
        }).filter(StringUtils::isNotEmpty).forEach(results::add);
        return results;
    }

    @Override
    public String executeGenerateBillRule(CtmJSONObject params) throws Exception {
        // 第一步 获取要执行的银行对账单的数据集合
        List<BankReconciliation> data = getNeedGenerateBillList(params);
        BankreconciliationUtils.checkAndFilterData(data, BankreconciliationScheduleEnum.INTELLIGENCEGENERATEBILLTASK);
        // 借款方向
        List<BankReconciliation> debitData = data.stream().filter(b -> b.getDc_flag().equals(Direction.Debit)).collect(Collectors.toList());
        // 贷款方向
        List<BankReconciliation> creditData = data.stream().filter(b -> b.getDc_flag().equals(Direction.Credit)).collect(Collectors.toList());
        // 第二步 对银行对账单的数据进行分组定义
        List<List<BankReconciliation>> debitLists = groupBankReconciliationData(debitData, CMP_RULE_TASK_GROUP_SIZE);
        List<List<BankReconciliation>> creditLists = groupBankReconciliationData(creditData, CMP_RULE_TASK_GROUP_SIZE);
        // 第三步 创建分批执行的任务队列集合
        String logId = params.get("logId").toString();
        Map<Integer, TargetRuleInfoDto> executeRuleMap = identifyingRuleStrategy.loadRule(RuleStrategy.CMP_GENERATE_PREFIX);
        String isnewPrefix = "1";
        if(executeRuleMap == null || executeRuleMap.size() == 0){
            isnewPrefix = "0";
        }
        // 收集生单异常错误信息
        List<String> errorResult = new ArrayList<>();
        if ("1".equals(isnewPrefix)) {
            List debitErrors = getGenerateBillTaskList(debitLists, RuleStrategy.CMP_GENERATE_PREFIX, logId, false);
            if (CollectionUtils.isNotEmpty(debitErrors)) {
                errorResult.addAll(debitErrors);
            }
            List creditErrors = getGenerateBillTaskList(creditLists, RuleStrategy.CMP_GENERATE_PREFIX, logId, false);
            if (CollectionUtils.isNotEmpty(creditErrors)) {
                errorResult.addAll(creditErrors);
            }
        } else {
            List debitErrors = getGenerateBillTaskList(debitLists, RuleStrategy.CMP_PAYMENT_RECEIPT_PREFIX, logId, false);
            if (CollectionUtils.isNotEmpty(debitErrors)) {
                errorResult.addAll(debitErrors);
            }
            List creditErrors = getGenerateBillTaskList(creditLists, RuleStrategy.CMP_COLLECTION_RECEIPT_PREFIX, logId, false);
            if (CollectionUtils.isNotEmpty(creditErrors)) {
                errorResult.addAll(creditErrors);
            }
        }
        if (CollectionUtils.isNotEmpty(errorResult)) {
            return errorResult.toString();
        }
        return "";
    }

    /**
     * 银行对账单后台任务提前入账处理
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public String executeAdvanceEnterRule(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        // 第一步 获取要执行的银行对账单的数据集合 未关联  任务参数  'earlyentryflag', '提前自动入账标识'   autosubmit  是否自动提交
        params.put("earlyentryflag", 1);
        List<BankReconciliation> data = getNeedGenerateBillList(params);
        BankreconciliationUtils.checkAndFilterData(data, BankreconciliationScheduleEnum.INTELLIGENCEADVANCEACCOUNTTASK);
        // 第二步 对银行对账单的数据进行分组定义
        List<List<BankReconciliation>> dataLists = groupBankReconciliationData(data, CMP_RULE_TASK_GROUP_SIZE);
        // 第三步 创建分批执行的任务队列集合
        // 收款生单任务
        String logId = params.get("logId").toString();
        Boolean autoSubmit = false;
        if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005F8", "是") /* "是" */.equals(params.get("autosubmit"))) {
            autoSubmit = true;
        }
        List<String> errorList = getGenerateBillTaskList(dataLists, RuleStrategy.CMP_EARLY_RECORD_PREFIX, logId, autoSubmit);
        return errorList.toString();
    }


}
