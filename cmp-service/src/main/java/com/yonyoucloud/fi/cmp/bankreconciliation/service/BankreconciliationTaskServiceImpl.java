package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationTaskService;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IDealDetailProcessing;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.OnlyUsePublishBankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author qihaoc
 * @Description: 银行对账单调度任务serivice
 * @date 2023/10/14 11:10
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankreconciliationTaskServiceImpl implements BankreconciliationTaskService {


    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    private BankreconciliationService bankreconciliationService;

    @Resource
    private IDealDetailProcessing dealDetailProcessing;

    /**
     * 银行对账单自动发布调度任务
     *
     * @return 执行结果
     */
    @Override
    public CtmJSONObject automaticPublic(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //获取符合条件的对账单
        List<BankReconciliation> data = getBankReconciliationList(params);
        BankreconciliationUtils.checkAndFilterData(data, BankreconciliationScheduleEnum.BANKRECAUTOPUBLIC);
        // 按50个元素分组
        List<List<BankReconciliation>> groupedData = IntStream.range(0, (data.size() + 49) / 50)
                .mapToObj(i -> data.subList(i * 50, Math.min((i + 1) * 50, data.size())))
                .collect(Collectors.toList());
        //通知调度任务 后端为异步
        if (data == null || CollectionUtils.isEmpty(data)) {
            //通知任务执行成功
            result.put("status", TaskUtils.TASK_BACK_SUCCESS);
            result.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B92FBA05700006", "未找到需要自动发布的银行对账单！"));
            return result;
        }
        String newFlowhandle = AppContext.getEnvConfig("newFlowhandle", "0");
        if ("1".equals(newFlowhandle)) {
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {

                    //需要根据data里的数据 通过id查除全量的数据  遍历data 取data的id
                    for (List<BankReconciliation> items : groupedData) {
                        List<String> ids = items.stream().filter(Objects::nonNull).map(p -> p.getId() != null ? p.getId().toString() : null).filter(Objects::nonNull).collect(Collectors.toList());
                        List<Map<String, Object>> list = MetaDaoHelper.queryByIds(BankReconciliation.ENTITY_NAME, "*", ids);
                        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
                        if (CollectionUtils.isNotEmpty(list)) {
                            list.stream().filter(Objects::nonNull).forEach(p -> {
                                BankReconciliation bankReconciliation = Objectlizer.convert(p, BankReconciliation.ENTITY_NAME);
                                bankReconciliationList.add(bankReconciliation);
                            });
                        }
                        List<BankDealDetailWrapper> bankDealDetailProcessList = DealDetailUtils.convertBankReconciliationToWrapper(bankReconciliationList, false);
                        //todo 走智能规则
                        dealDetailProcessing.dealDetailProcesing(bankDealDetailProcessList, bankReconciliationList, OnlyUsePublishBankDealDetailMatchChainImpl.get(), 1, DealDetailUtils.getTraceId(), UUID.randomUUID().toString());
                    }
                    //通知任务执行成功
                    TaskUtils.updateTaskLog((Map<String, String>) params.get("ipaParams"), TaskUtils.TASK_BACK_SUCCESS, params.getString("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180599", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    log.error("【辨识匹配处理器】执行辨识匹配规则异常", e);
                    //通知任务执行失败
                    TaskUtils.updateTaskLog((Map<String, String>) params.get("ipaParams"), TaskUtils.TASK_BACK_FAILURE, params.getString("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180157", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }

            });

        } else {
            executorServicePool.getThreadPoolExecutor().submit(() -> executePublish(data, params.getString("logId")));
        }
        result.put("asynchronized", true);
        return result;
    }


    //发布
    private void executePublish(List<BankReconciliation> list, String logId) {
        try {
            for (int i = 0; i < list.size(); i++) {
                bankreconciliationService.publish(Long.valueOf(list.get(i).getId().toString()), list.get(i).getBank_seq_no(), new HashMap<>());
            }
        } catch (Exception e) {
            log.error("银行对账单发布失败" + e);
            //通知任务执行失败
            TaskUtils.updateTaskLog(null, TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180157", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        //通知任务执行成功
        TaskUtils.updateTaskLog(null, TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180599", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
    }

    //查询对账单
    private List<BankReconciliation> getBankReconciliationList(CtmJSONObject param) throws ParseException {
        String orgids = (String) (Optional.ofNullable(param.get("orgid")).orElse(""));
        String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
        String bankaccounts = (String) (Optional.ofNullable(param.get("bankaccount")).orElse(""));
        String isadvanceaccounts = (String) (Optional.ofNullable(param.get("isadvanceaccounts")).orElse(""));
        String confirmstatus = (String) (Optional.ofNullable(param.get("confirmstatus")).orElse(""));
        String daysinadvance = (String) (Optional.ofNullable(param.get(TaskUtils.TASK_DAY_IN_ADV)).orElse(""));
        String dcflag = (String) (Optional.ofNullable(param.get("dcflag")).orElse(""));
        String quickTypes = (String) (Optional.ofNullable(param.get("quickType")).orElse(""));
        String oppositeType = (String) (Optional.ofNullable(param.get("oppositetype")).orElse(""));


        QuerySchema schema = QuerySchema.create().addSelect(" id,bank_seq_no,accentity,serialdealtype ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //财资统一对账码是解析过来的，不可自动发布
        conditionGroup.appendCondition(QueryCondition.name("isparsesmartcheckno").eq(0));
        //1、执行过自动关联任务但未关联 或 2、业务关联状态为“已关联”且对账单入账类型=‘02 挂账’
        QueryConditionGroup associationGroup = new QueryConditionGroup(ConditionOperator.or);
        //1、执行过自动关联任务但未关联
        QueryConditionGroup noAssociationGroup = new QueryConditionGroup(ConditionOperator.and);
        //授权组织确认状态
        if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A7", "待确认") /* "待确认" */.equals(confirmstatus)) {
            //置为“待确认”时，取数范围是授权使用组织为空，该场景下不判定自动关联标识，就可以自动发布；
            conditionGroup.appendCondition(QueryCondition.name("confirmstatus").eq("0"));
            noAssociationGroup.addCondition(QueryCondition.name("associationstatus").eq("0"));
            noAssociationGroup.addCondition(QueryCondition.name("accentity").is_null());
            associationGroup.addCondition(noAssociationGroup);
        } else if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A4", "已确认") /* "已确认" */.equals(confirmstatus)) {
            //为“已确认”，取数范围是授权使用组织有值，需要先执行自动关联标识后，在进行自动发布
            conditionGroup.appendCondition(QueryCondition.name("confirmstatus").eq("1"));
            noAssociationGroup.addCondition(QueryCondition.name("associationstatus").eq("0"));
            noAssociationGroup.addCondition(QueryCondition.name("autoassociation").eq("1"));
            associationGroup.addCondition(noAssociationGroup);
        } else {
            //参数设置为“空”，取数范围是包含授权使用组织为空，和授权使用组织有值的数据
            // ---其中授权使用组织为空的数据，不判定自动关联标识，就可以自动发布，
            // 授权使用组织有值，需要先执行自动关联标识后，在进行自动发布
            QueryConditionGroup unconfirmGroup = new QueryConditionGroup(ConditionOperator.and);
            unconfirmGroup.addCondition(QueryCondition.name("confirmstatus").eq("0"));
            unconfirmGroup.addCondition(QueryCondition.name("associationstatus").eq("0"));
            unconfirmGroup.addCondition(QueryCondition.name("accentity").is_null());
            associationGroup.addCondition(unconfirmGroup);

            QueryConditionGroup confirmedGroup = new QueryConditionGroup(ConditionOperator.and);
            confirmedGroup.addCondition(QueryCondition.name("confirmstatus").eq("1"));
            confirmedGroup.addCondition(QueryCondition.name("associationstatus").eq("0"));
            confirmedGroup.addCondition(QueryCondition.name("autoassociation").eq("1"));
            associationGroup.addCondition(confirmedGroup);
        }
        // 流水执行状态 1-已执行自动关联，2-已执行自动生单
        String executeStatus = (String) (Optional.ofNullable(param.get("executeStatus")).orElse(""));
        if (StringUtils.isNotEmpty(executeStatus)) {
            if ("1".equals(executeStatus)) {
                conditionGroup.appendCondition(QueryCondition.name("autoassociation").eq("1"));
            } else if ("2".equals(executeStatus)) {
                conditionGroup.appendCondition(QueryCondition.name("isautocreatebill").eq("1"));
            }
        }
        conditionGroup.appendCondition(QueryCondition.name("ispublish").eq("0"));
        //2、业务关联状态为“已关联”且对账单入账类型=‘02 挂账’
        QueryConditionGroup hasAssociationGroup = new QueryConditionGroup(ConditionOperator.and);
        hasAssociationGroup.addCondition(QueryCondition.name("associationstatus").eq("1"));
        hasAssociationGroup.addCondition(QueryCondition.name("entrytype").eq("2"));
        associationGroup.addCondition(hasAssociationGroup);
        //加入总条件
        conditionGroup.addCondition(associationGroup);
        String[] orgidsArr = null;
        if (!StringUtils.isEmpty(orgids)) {
            orgidsArr = orgids.split(";");
        }
        if (orgidsArr != null && orgidsArr.length > 0) {
            conditionGroup.appendCondition(QueryCondition.name("orgid").in(orgidsArr));
        }
        String[] currencyArr = null;
        if (!StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
            if (currencyArr != null && currencyArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("currency").in(currencyArr));
            }
        }
        String[] banktypeArr;
        if (!StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
            if (banktypeArr != null && banktypeArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("banktype").in(banktypeArr));
            }
        }
        String[] bankaccountArr;
        if (!StringUtils.isEmpty(bankaccounts)) {
            bankaccountArr = bankaccounts.split(";");
            if (bankaccountArr != null && bankaccountArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(bankaccountArr));
            }
        }
        if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A5", "否") /* "否" */.equals(isadvanceaccounts) || "".equals(isadvanceaccounts)) {
            conditionGroup.appendCondition(QueryCondition.name("isadvanceaccounts").eq("0"));
        } else {
            conditionGroup.appendCondition(QueryCondition.name("isadvanceaccounts").eq("1"));
        }
        if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A6", "收入") /* "收入" */.equals(dcflag)) {
            conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq("2"));
        } else if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A8", "支出") /* "支出" */.equals(dcflag)) {
            conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq("1"));
        }
        String[] quickTypeArr;
        if (!StringUtils.isEmpty(quickTypes)) {
            quickTypeArr = quickTypes.split(";");
            if (quickTypeArr != null && quickTypeArr.length > 0) {
                conditionGroup.appendCondition(QueryCondition.name("quickType").in(quickTypeArr));
            }
        }
        // 对方单位类型
        List<Short> oppositeTypeList = new ArrayList<>();
        String[] oppositetypesArr = null;
        if (!StringUtils.isEmpty(oppositeType)) {
            oppositetypesArr = oppositeType.split(";");
        }
        if (oppositetypesArr != null && oppositetypesArr.length > 0) {
            // 添加对方单位类型数据
            for (String type : oppositetypesArr) {
                oppositeTypeList.add(Short.valueOf(type));
            }
            //
            if (CollectionUtils.isNotEmpty(oppositeTypeList)) {
                conditionGroup.appendCondition(QueryCondition.name("oppositetype").in(oppositeTypeList));
            }
        }
        if (!StringUtils.isEmpty(daysinadvance)) {
            String intervaldate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * Integer.valueOf(daysinadvance)), DateUtils.DATE_PATTERN);
            conditionGroup.appendCondition(QueryCondition.name("tran_date").egt(intervaldate));
        }
        //疑重的不进行查询
//        conditionGroup.addCondition(QueryConditionGroup.or(QueryCondition.name("isrepeat").eq(0),
//                QueryCondition.name("isrepeat").is_null()));
        schema.addCondition(conditionGroup);
        try {
            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
            // 流水正在处理中的需要过滤
            return bankreconciliationService.filterBankReconciliationByLockKey(bankReconciliationList, BankReconciliationActions.AutoPublic);
        } catch (Exception e) {
            log.error("获取银行对账单数据错误" + e);
            return new ArrayList<>();
        }
    }
}
