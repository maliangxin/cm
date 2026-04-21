package com.yonyoucloud.fi.cmp.earlywarning.service.impl;

import com.yonyou.iuap.event.mp.api.model.PushMessage;
import com.yonyou.iuap.event.mp.common.model.Result;
import com.yonyou.iuap.message.platform.entity.MessageInfoEntity;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.respvo.SettleBenchRespVO;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceContrast;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.MoneyForm;
import com.yonyoucloud.fi.cmp.constant.ICsplConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.earlywarning.service.EarlyWarningService;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.payapplicationbill.ApprovalStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EarlyWaringUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.message.CMMessageInfoClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <h1>EarlyWarningServiceImpl</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-08-15 11:17
 */
@Service
@Slf4j
public class EarlyWarningServiceImpl implements EarlyWarningService {

    public static final String REFUDN_WARNING_TITLE = "疑似退票预警";//@notranslate
    public static final String PREWARN = "prewarn";
    public static final String RESPONSE_MAP = "responseMap";
    public static final String DATA = "data";
    public static final String MSG = "msg";
    public static final String STATUS = "status";
    @Autowired
    private SettleBenchBRPCService settleBenchBRPCService;

    @Override
    public Map<String, Object> payApplyBillPayDateWarningTask(int beforeDays, String logId, String tenant) {
        CtmJSONArray data = new CtmJSONArray();
        Map<String, Object> result = new HashMap<>();
        int status = TaskUtils.TASK_BACK_SUCCESS;
        if (beforeDays < 0) {
            beforeDays = 1;
        }
        String msg = "";
        try {
            List<PayApplicationBill> payApplicationBillList = queryPaymentDateGreaterCurrentDate(beforeDays, tenant);
            if (CollectionUtils.isNotEmpty(payApplicationBillList)) {
                for (PayApplicationBill payApplicationBill : payApplicationBillList) {
                    CtmJSONObject object = new CtmJSONObject();
                    StringBuffer sb = new StringBuffer(); // 动态拼接
                    // 付款申请单{付款申请单编号}将于{付款申请单日期}到期，请关注！{系统日期}
                    sb.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1818559404280029", "付款申请单{%s}付款日期将于{%s}到期，请关注！{%s}") /* "付款申请单{%s}付款日期将于{%s}到期，请关注！{%s}" */, payApplicationBill.getCode(), payApplicationBill.getProposePaymentDate(), DateUtils.getStringDate()));
                    object.put("msg", sb.toString());
                    data.add(object);
                }
            }
        } catch (Exception e) {
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("EarlyWarningServiceImpl error, e = {}", e.getMessage());
            msg = e.getMessage();
        } finally {
            log.error("EarlyWarningServiceImpl Warning Task, status = {}, logId = {}, content = {}, tenant = {}",
                    status, logId, data, tenant);
            result.put("status", status);//执行结果： 0：失败；1：成功
            result.put("data", data);//业务方自定义结果集字段
            result.put("msg", msg);//	异常信息
            AppContext.clear();
        }
        return result;
    }

    private List<PayApplicationBill> queryPaymentDateGreaterCurrentDate(int beforeDays, String tenant) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("unpaidAmountSum").gt(BigDecimal.ZERO));
        queryConditionGroup.addCondition(QueryCondition.name("ytenant").eq(tenant));
        queryConditionGroup.addCondition(QueryCondition.name("approvalStatus").eq(ApprovalStatus.ApprovedPass.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("distanceProposePaymentDateDays").elt(beforeDays));
        // 当前日期加提醒天数
        Date date = new Date();
        String dateToStr = DateUtils.dateToStr(date);
        Date strToDate = DateUtils.strToDate(dateToStr);
        queryConditionGroup.addCondition(QueryCondition.name("proposePaymentDate").egt(strToDate)); // 大于等于
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(PayApplicationBill.ENTITY_NAME, querySchema, null);
    }

    /**
     * 历史余额预警任务
     *
     * @param beforeDays
     * @param logId
     * @param tenant
     */
    @Async
    @Override
    public void acctbalWarningTask(int beforeDays, String logId, String tenant) {
        CtmJSONArray data = new CtmJSONArray();
        int status = TaskUtils.TASK_BACK_SUCCESS;
        if (beforeDays < 0) {
            beforeDays = 1;
        }
        StringBuffer sb = new StringBuffer();
        // 消息发送方式： 0：都不发送；1：失败发送；2：成功失败都发送
        int msgSend = EarlyWaringUtils.NO_SEND;
        try {
            // 发送方式
            msgSend = EarlyWaringUtils.ALL_SEND;
            CtmJSONObject object = new CtmJSONObject();
            String msg = businessProcess(beforeDays);
            sb.append(msg);
            object.put("msg", msg);
            data.add(object);
        } catch (Exception e) {
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("acctbalWarningTask error, e = {}", e.getMessage());
            sb.append(e.getMessage());
        } finally {
            log.error("acctbalWarningTask Warning Task, status = {}, logId = {}, content = {}, tenant = {}",
                    status, logId, data, tenant);
            EarlyWaringUtils.preExpireUpdateTaskLog(sb.toString(), status, logId, msgSend, data, EarlyWaringUtils.ACCTBAL_WARNING_TITLE, EarlyWaringUtils.PRE_WARNING_UPDATE_TASK_LOG_URL);
            AppContext.clear();
        }
    }

    /**
     * 循环封装
     *
     * @param beforeDays
     * @return
     * @throws Exception
     */
    private String businessProcess(int beforeDays) throws Exception {
        StringBuffer sBuffer = new StringBuffer();
        for (int i = beforeDays; i > 0; i--) {
            sBuffer.append(acctbalCompare(i));
        }
        return sBuffer.toString();
    }

    /**
     * 具体逻辑
     *
     * @param i
     * @return
     * @throws Exception
     */
    private String acctbalCompare(int i) throws Exception {
        //定义相等
        AtomicInteger failedCount = new AtomicInteger(0);
        //定义不相等
        AtomicInteger successCount = new AtomicInteger(0);
        //前天历史余额键值对
        Map<String, BigDecimal> beforeYesBalancesMap = new HashMap<>();
        Date beforeYesDate = DateUtils.strToDate(DateUtils.dateToStr(DateUtils.dateAddDays(DateUtils.getNow(), ~i)));
        //前天银行历史余额账户map1(银行账户+会计主体+币种,余额)
        List<AccountRealtimeBalance> beforeYesBalances = getRealtimeBalances(beforeYesDate);
        Date yesterdayDate = DateUtils.strToDate(DateUtils.dateToStr(DateUtils.dateAddDays(DateUtils.getNow(), ~i + 1)));
        //昨天历史余额
        List<AccountRealtimeBalance> yesterdayBalances = getRealtimeBalances(yesterdayDate);
        if (!CollectionUtils.isEmpty(beforeYesBalances)) {
            if (!CollectionUtils.isEmpty(yesterdayBalances)) {
                //解析前天历史余额信息
                getBeforeYes(beforeYesBalances, beforeYesBalancesMap);
                //昨天银行历史余额账户map2(银行账户+会计主体+币种,余额)
                Map<String, BigDecimal> yesterdayMap = new HashMap<>();
                Map<String, Long> idMap = new HashMap<>();
                //解析昨天历史余额信息
                analysisYesBalances(yesterdayBalances, yesterdayMap, idMap);
                //查询银行对账单map3(银行账户+会计主体+币种,计算余额)计算得到数据  前日余额+贷-借
                Map<String, BigDecimal> bankMap = new HashMap<>();
                //昨天银行对账单
                List<BankReconciliation> yesterdayBankReconciliationes = getBankReconciliationes(yesterdayDate);
                analysisBank(yesterdayBankReconciliationes, beforeYesBalancesMap, yesterdayMap, bankMap);
                //比较map3与map1是否相等 //根据id，回写计算余额与是否相等
                compareResult(yesterdayBalances, beforeYesBalancesMap, bankMap, idMap, successCount, failedCount);
            }
        } else {
            //前天无历史余额 昨天全部不相等 ----产品定的
            if (!CollectionUtils.isEmpty(yesterdayBalances)) {
                yesterdayBalances.stream().forEach(e -> {
                    //设置不相等
                    e.setBalancecontrast(BalanceContrast.Unequal.getValue());
                    failedCount.incrementAndGet();
                });
                EntityTool.setUpdateStatus(yesterdayBalances);
                MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, yesterdayBalances);
            }
        }
        //查询银行账户期初表 获得总银行账户数
        List<Map<String, Object>> initDataCount = getInitDataCount();
//        2023-03-29日，银行账户余额经过比对计算，XX个账户余额相等，XX个账户余额不等，XX个账户本日无余额
        return createMessage(i, initDataCount, successCount, failedCount);
    }

    /**
     * 比较前天与昨天金额 并回写数据库
     *
     * @param yesterdayBalances
     * @param beforeYesBalancesMap
     * @param bankMap
     * @param idMap
     * @param successCount
     * @param failedCount
     * @throws Exception
     */
    private void compareResult(List<AccountRealtimeBalance> yesterdayBalances, Map<String, BigDecimal> beforeYesBalancesMap, Map<String, BigDecimal> bankMap, Map<String, Long> idMap
            , AtomicInteger successCount, AtomicInteger failedCount) throws Exception {
        if (!CollectionUtils.isEmpty(yesterdayBalances)) {
            List<AccountRealtimeBalance> accountRealtimeBalanceList = new ArrayList<>();
            yesterdayBalances.stream().forEach(e -> {
                String keyb = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
                //前天历史余额有该账户  昨天余额=前天余额+/-昨天银行对账单
                if (beforeYesBalancesMap.containsKey(keyb)) {
                    AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
                    accountRealtimeBalance.setId(idMap.get(keyb));
                    accountRealtimeBalanceList.add(accountRealtimeBalance);
                    //昨天计算余额
                    if (bankMap.containsKey(keyb)) {
                        accountRealtimeBalance.setAcctbalcount(bankMap.get(keyb));
                        if (e.getAcctbal().compareTo(bankMap.get(keyb)) != 0) {
                            //设置不相等
                            accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
                            failedCount.incrementAndGet();
                        } else {
                            //设置相等
                            accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                            successCount.incrementAndGet();
                        }
                    } else {
                        failedCount.incrementAndGet();
                    }
                } else {
                    failedCount.incrementAndGet();
                }
            });
            if (!CollectionUtils.isEmpty(accountRealtimeBalanceList)) {
                EntityTool.setUpdateStatus(accountRealtimeBalanceList);
                MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, accountRealtimeBalanceList);
            }
        }
    }

    /**
     * 解析昨天银行对账单信息并计算账户昨天真实余额
     *
     * @param yesterdayBankReconciliationes
     * @param beforeYesBalancesMap
     * @param yesterdayMap
     * @param bankMap
     */
    private void analysisBank(List<BankReconciliation> yesterdayBankReconciliationes, Map<String, BigDecimal> beforeYesBalancesMap,
                              Map<String, BigDecimal> yesterdayMap, Map<String, BigDecimal> bankMap) {
        if (!CollectionUtils.isEmpty(yesterdayBankReconciliationes)) {
            yesterdayBankReconciliationes.stream().forEach(e -> {
                String keyr = e.getAccentity() + e.getCurrency() + e.getBankaccount();
                //前天历史余额必须有该账户  否则 不相等 无需处理  并且 昨天历史余额必须有该账户
                if (beforeYesBalancesMap.containsKey(keyr) && yesterdayMap.containsKey(keyr)) {
                    BigDecimal tranAmt = e.getTran_amt() == null ? BigDecimal.ZERO : e.getTran_amt();
                    if (!bankMap.containsKey(keyr)) {
                        BigDecimal balanceValue = beforeYesBalancesMap.get(keyr) == null ? BigDecimal.ZERO : beforeYesBalancesMap.get(keyr);
                        if (e.getDc_flag().getValue() == Direction.Debit.getValue()) {
                            tranAmt = balanceValue.subtract(e.getTran_amt());
                        } else if (e.getDc_flag().getValue() == Direction.Credit.getValue()) {
                            tranAmt = balanceValue.add(e.getTran_amt());
                        }
                        bankMap.put(keyr, tranAmt);
                    } else {
                        if (e.getDc_flag().getValue() == Direction.Debit.getValue()) {
                            tranAmt = bankMap.get(keyr).subtract(e.getTran_amt());
                        } else if (e.getDc_flag().getValue() == Direction.Credit.getValue()) {
                            tranAmt = bankMap.get(keyr).add(e.getTran_amt());
                        }
                        bankMap.put(keyr, tranAmt);
                    }
                }
            });
        } else {
            bankMap.putAll(beforeYesBalancesMap);
        }
    }

    /**
     * 解析前天历史余额信息
     *
     * @param beforeYesBalances
     * @param startMap
     */
    private void getBeforeYes(List<AccountRealtimeBalance> beforeYesBalances, Map<String, BigDecimal> startMap) {
        beforeYesBalances.stream().forEach(e -> {
            String key = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
            if (!startMap.containsKey(key)) {
                startMap.put(key, e.getAcctbal());
            }
        });
    }

    /**
     * 解析昨天历史余额信息
     *
     * @param yesterdayBalances
     * @param yesterdayMap
     * @param idMap
     */
    private void analysisYesBalances(List<AccountRealtimeBalance> yesterdayBalances, Map<String, BigDecimal> yesterdayMap, Map<String, Long> idMap) {
        yesterdayBalances.stream().forEach(e -> {
            String key = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
            if (!yesterdayMap.containsKey(key)) {
                yesterdayMap.put(key, e.getAcctbal());
                idMap.put(key, e.getId());
            }
        });
    }

    /**
     * 组装消息体
     *
     * @param i
     * @param initDataCount
     * @param successCount
     * @param failedCount
     * @return
     * @throws Exception
     */
    private String createMessage(int i, List<Map<String, Object>> initDataCount, AtomicInteger successCount, AtomicInteger failedCount) throws Exception {
        //查询预警期间是否存在不相等的银行账户
        //组装消息体
        StringBuffer resultBuffer = new StringBuffer();
        resultBuffer.append(DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), ~i + 1), DateUtils.YYYYMMDD));
        resultBuffer.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400756", "日，银行账户余额经过比对计算，") /* "日，银行账户余额经过比对计算，" */);
        resultBuffer.append(successCount.intValue());
        resultBuffer.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400758", "个账户余额相等，") /* "个账户余额相等，" */);
        resultBuffer.append(failedCount.intValue());
        resultBuffer.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400759", "个账户余额不等，") /* "个账户余额不等，" */);
        resultBuffer.append(Integer.valueOf(initDataCount.get(0).get("count").toString()) - successCount.intValue() - failedCount.intValue());
        resultBuffer.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540075B", "个账户本日无余额") /* "个账户本日无余额" */);
        return resultBuffer.toString();
    }

    /**
     * 获取租户下银行账户期初个数
     *
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getInitDataCount() throws Exception {
        QuerySchema queryInitDataCount = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("qzbz").eq(true)); // 是否期初
        queryConditionGroup.addCondition(QueryCondition.name("moneyform").eq(MoneyForm.bankaccount.getValue()));
        queryInitDataCount.addCondition(queryConditionGroup);
        return MetaDaoHelper.query(InitData.ENTITY_NAME, queryInitDataCount);
    }

    /**
     * 查询历史余额数据
     *
     * @param startDate
     * @return
     * @throws Exception
     */
    private List<AccountRealtimeBalance> getRealtimeBalances(Date startDate) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,enterpriseBankAccount,currency,balancedate,acctbal");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("balancedate").eq(startDate)); // 等于
        querySchema.addCondition(queryConditionGroup);
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, querySchema, null);
        return existBalances;
    }

    /**
     * 查询银行对账单
     *
     * @param startDate
     * @return
     * @throws Exception
     */
    private List<BankReconciliation> getBankReconciliationes(Date startDate) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,bankaccount,currency,tran_date,tran_amt,dc_flag");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("tran_date").eq(startDate)); // 等于
        querySchema.addCondition(queryConditionGroup);
        List<BankReconciliation> existBalances = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return existBalances;
    }

    /**
     * 余额自动检查预警
     *
     * @return
     */
    @Override
    public Map<String, Object> acctbalCheckWarning(Integer checkRange, String logId, String tenant) {
        int status = TaskUtils.TASK_BACK_SUCCESS;
        Map<String, Object> result = new HashMap<>();
        String errMsg = "";
        CtmJSONArray dataArray = null;
        try {
            dataArray = queryNotEqualDataMsg(checkRange);
        } catch (Exception e) {
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("acctbalCheckWarning error, e = {}", e.getMessage());
            errMsg = e.getMessage();
        } finally {
            log.error("acctbalCheckWarning Warning Task, status = {}, logId = {}, content = {}, tenant = {}",
                    status, logId, dataArray, tenant);
            AppContext.clear();
            result.put("status", status);//执行结果： 0：失败；1：成功
            if (dataArray != null) {
                result.put("data", dataArray);//业务方自定义结果集字段
            }
            result.put("msg", errMsg);//	异常信息
        }
        return result;
    }

    private CtmJSONArray queryNotEqualDataMsg(Integer checkRange) throws Exception {
        //按天来查不相等的数据，例如今天是2023-05-06，检查日期为3天 那么就依次检查2023-05-03 2023-05-04 2023-05-05三天的不相等数据 -废弃，查询全部的预警信息
//        LocalDate today = LocalDate.now();
//        LocalDate checkStartDate = today.plusDays(-checkRange);
        QuerySchema querySchema = new QuerySchema().addSelect("enterpriseBankAccount.account,balancedate,balancecheckinstruction");
//        querySchema.appendQueryCondition(QueryCondition.name("balancedate").egt(checkStartDate));
        querySchema.appendQueryCondition(QueryCondition.name("balancecontrast").eq(BalanceContrast.Unequal.getValue()));
        querySchema.addOrderBy("enterpriseBankAccount.account", "balancedate");
        List<AccountRealtimeBalance> notEqualBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(notEqualBalances)) {
            return null;
        }
        CtmJSONArray dataArray = new CtmJSONArray();
        notEqualBalances.stream().filter(item -> StringUtils.isNotBlank(item.getBalancecheckinstruction())).forEach(notEqualBalance -> {
                    String enterpriseBankAccount_account = notEqualBalance.get("enterpriseBankAccount_account");
                    Date balancedate = notEqualBalance.getBalancedate();
                    String formatDate = "";
                    if (balancedate != null) {
                        formatDate = DateUtils.dateToStr(balancedate);
                    }
                    //预警消息中不支持方括号，替换掉
                    String instruction = notEqualBalance.getBalancecheckinstruction().replace("[", "").replace("]", "");
                    StringBuffer msg = new StringBuffer();
                    msg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180611", "银行账号") /* "银行账号" */).append(enterpriseBankAccount_account).append(",").append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180612", "余额日期") /* "余额日期" */).append(formatDate).append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180610", "检查结果") /* "检查结果" */).append(":").append(instruction).append(" ");
                    CtmJSONObject dataItem = new CtmJSONObject();
                    dataItem.put("msg", msg.toString());
                    dataArray.add(dataItem);
                }
        );
        return dataArray;
    }

    /**
     * 疑似退票预警任务
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> suspectedRefundWarning(CtmJSONObject param, String logId, String tenantId) throws Exception {
        log.error("suspectedRefundWarning begin, logId = {},  tenant = {}", logId, tenantId);
        Map<String, Object> result = new HashMap<>();
        CtmJSONArray dataArray = new CtmJSONArray();
        String msgcontent = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540075A", "银行对账单被标记为疑似退票，请尽快进行确认！") /* "银行对账单被标记为疑似退票，请尽快进行确认！" */;
        //会计主体
        String accentityStr = param.getString("accentity");
        //是否已关联结算
        String relationSettle = param.getString("relationSettle");
        List<String> userIds = new ArrayList<>();
        //查询退票状态为疑似退票的对账单 支出方向
        QuerySchema schemabankre = QuerySchema.create().addSelect(" id ");
        QueryConditionGroup conditionGroupbankre = new QueryConditionGroup(ConditionOperator.and);
        conditionGroupbankre.appendCondition(QueryCondition.name("refundstatus").eq("1"));
        conditionGroupbankre.appendCondition(QueryCondition.name("dc_flag").eq("1"));
        if (StringUtil.isNotEmpty(accentityStr)) {
            String[] accentitys = accentityStr.split(";");
            if (accentitys != null && accentitys.length > 0) {
                conditionGroupbankre.addCondition(QueryCondition.name("accentity").in((Object) accentitys));
            }
        }
        schemabankre.addCondition(conditionGroupbankre);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schemabankre, null);
        if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
            CtmJSONObject dataItem = new CtmJSONObject();
            if ("否".equals(relationSettle)) {//@notranslate
                dataItem.put(MSG, msgcontent);
                dataArray.add(dataItem);
                result.put(MSG, msgcontent);
                result.put("data", dataArray);//业务方自定义结果集字段
                result.put("status", 1);//执行结果： 0：失败；1：成功
                return result;
            }
            if ("是".equals(relationSettle)) {//@notranslate
                userIds = getyhtUserId(result, bankReconciliationList);
                if (userIds == null || userIds.size() == 0) {
                    dataItem.put(MSG, msgcontent);
                    dataArray.add(dataItem);
                    result.put(MSG, msgcontent);
                    result.put("data", dataArray);//业务方自定义结果集字段
                    result.put("status", 1);//执行结果： 0：失败；1：成功
                    return result;
                }
            }
        } else {
            result.put("status", 1);//执行结果： 0：失败；1：成功
            result.put("errmsg", "不存在疑似退票对账单!");//@notranslate
            return result;
        }
        //需要发送的内容组装
        Map<String, Object> bizData = new HashMap<>();
        bizData.put(MSG, msgcontent);
        //发送消息类型—— 预警通知
        List<String> channels = new ArrayList<>();
        channels.add(ICsplConstant.CHANNEL_USPACE);
        channels.add(ICsplConstant.CHANNEL_MAIL);
        channels.add(ICsplConstant.CHANNEL_SMS);
        Map<String, Object> responseMap = new HashMap<>();
        try {
            // 515 支持
//            responseMap = msgSendService.sendPlainMessageToUsers(ICsplConstant.SYSID_DIWORK, tenantId, userIds, channels, "prewarn", "疑似退票预警", msgcontent, null);
            //发消息时组装消息体
            MessageInfoEntity entity = new MessageInfoEntity(ICsplConstant.SYSID_DIWORK, tenantId);
            entity.setMsgId(UUID.randomUUID().toString()); // 新加参数必填幂等，可以直接使用UUID
            entity.setSrcId(IDomainConstant.MDD_DOMAIN_CM); //新加参数必填幂等，srcId+msgId两个参数控制幂等，不确定填什么可以填微服务编码
            entity.setChannels(channels);
            entity.setSubject(REFUDN_WARNING_TITLE);
            entity.setContent(msgcontent);
            entity.setReceiver(userIds);
            entity.setMessageType(PREWARN);
            //消息落库
//            responseMap = msgSendService.sendPlainMessageToUsers(ICsplConstant.SYSID_DIWORK, tenantId, userIds, channels, "prewarn", "疑似退票预警", msgcontent, null);
            Result<List<PushMessage>> messageResult = CMMessageInfoClient.sendMessageByDefaultDs(entity);
            CtmJSONObject dataItem = new CtmJSONObject();
            dataItem.put(RESPONSE_MAP, messageResult);
            dataArray.add(dataItem);
        } catch (RuntimeException e) {
            log.error("发送租户模板消息服务超时请重试", e);
        }
        result.put(DATA, dataArray);//业务方自定义结果集字段
        result.put(MSG, msgcontent);
        result.put(STATUS, 1);//执行结果： 0：失败；1：成功
        return result;
    }

    private List<String> getyhtUserId(Map<String, Object> result, List<BankReconciliation> bankReconciliationList) throws Exception {
        List idList = bankReconciliationList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
        QuerySchema schemabankre_b = QuerySchema.create().addSelect(" srcbillid ");
        QueryConditionGroup conditionGroupbankre_b = new QueryConditionGroup(ConditionOperator.and);
        conditionGroupbankre_b.appendCondition(QueryCondition.name("bankreconciliation").in(idList));
        conditionGroupbankre_b.appendCondition(QueryCondition.name("billtype").eq(EventType.StwbSettleMentDetails.getValue()));
        schemabankre_b.addCondition(conditionGroupbankre_b);
        schemabankre_b.distinct();
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, schemabankre_b, null);
        if (bankReconciliationbusrelation_bs == null || bankReconciliationbusrelation_bs.size() == 0) {
            return null;
        }
        List<Long> srcbillids = bankReconciliationbusrelation_bs.stream().map(BankReconciliationbusrelation_b::getSrcbillid).collect(Collectors.toList());
        //用户id
        Set<String> userIds = new HashSet<>();
        //根据关联单号 获取资金结算明细提交人用户id
        List<SettleBenchRespVO> settleBenchRespVOS = settleBenchBRPCService.querySettleBenchList(srcbillids);
        for (SettleBenchRespVO settleBenchRespVO : settleBenchRespVOS) {
            userIds.add(settleBenchRespVO.getOperUser().toString());
        }
        //用户信息去重
        List<String> distinctUserIds = new ArrayList<>(userIds);
        List<String> yhtUserIds = new ArrayList<>();
        BillContext billContext = new BillContext();
        billContext.setFullname("base.user.User");
        billContext.setDomain("iuap-apcom-bipuser");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(distinctUserIds));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (query != null && query.size() > 0) {
            for (Map<String, Object> detail : query) {
                yhtUserIds.add(detail.get("yhtUserId").toString());
            }
        }
        return yhtUserIds;
    }


}
