package com.yonyoucloud.fi.cmp.accountdetailexclusion.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.AccountDetailExclusion_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ICsplConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JPK
 * @Description: 账户收支明细剔除调度任务serivice
 * @date 2023/11/14 11:10
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class AccountDetailExclusionTaskServiceImpl implements IAccountDetailExclusionTaskService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    /**
     * 账户收支明细自动剔除调度任务
     *
     * @return 执行结果
     */
    @Override
    public CtmJSONObject automaticExclusion(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //获取符合条件的对账单
        String begindate = (String) (Optional.ofNullable(params.get("begin_date")).orElse(""));
        String enddate = (String) (Optional.ofNullable(params.get("end_date")).orElse(""));
        LocalDate begin_Date;
        LocalDate end_Date;
        LocalDate today = LocalDate.now();
        if (!StringUtils.isEmpty(begindate)) {
            begin_Date = LocalDate.parse(begindate);
            if (begin_Date.isAfter(today) || begin_Date.isEqual(today)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102365"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800EF", "开始日期必须早于今天!") /* "开始日期必须早于今天!" */);
            }
        } else {
            begin_Date = today.minusDays(30);
        }
        if (!StringUtils.isEmpty(enddate)) {
            end_Date = LocalDate.parse(enddate);
            if (end_Date.isAfter(today)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102366"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F0", "结束日期必须早于或等于今天!") /* "结束日期必须早于或等于今天!" */);
            }
        } else {
            end_Date = today;
        }
        if (begin_Date.isAfter(end_Date) || begin_Date.isEqual(end_Date)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102367"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F1", "结束日期必须大于开始日期!") /* "结束日期必须大于开始日期!" */);
        }
        //通知调度任务 后端为异步
        Date finalBegin_date = DateUtil.localDate2Date(begin_Date);
        Date finalEnd_date = DateUtil.localDate2Date(end_Date);
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                creatAccountDetailExclusion(params, finalBegin_date, finalEnd_date);
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, (String) params.get("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, params.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("automaticExclusion exception", e);
            }
        });
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        result.put(ICmpConstant.STATUS, 1);
        result.put(ICmpConstant.MSG, ICmpConstant.SUCCESS);
        return result;
    }


    //生单
    private void creatAccountDetailExclusion(CtmJSONObject params, Date begin_date, Date end_date) {
        try {
            List<Map<String, Object>> bankReconciliationlist = getBankReconciliationList(params, begin_date, end_date);
            //组装剔除数据
            if (bankReconciliationlist != null && bankReconciliationlist.size() > 0) {
                Map<String, List<Map<String, Object>>> bankReconciliationListGroup = bankReconciliationlist.stream()
                        .collect(Collectors.groupingBy(bank -> bank.get("accentity") + "_" + bank.get("currency")));
                //取剔除原因类型
                eliminateReasonType(params);
                for (Map.Entry<String, List<Map<String, Object>>> entry : bankReconciliationListGroup.entrySet()) {
                    BigDecimal incomeAmount = BigDecimal.ZERO;
                    BigDecimal payAmount = BigDecimal.ZERO;
                    List<Map<String, Object>> bankReconciliationMapList = entry.getValue();
                    if (!CollectionUtils.isEmpty(bankReconciliationMapList)) {
                        try {
                            List<AccountDetailExclusion_b> accountDetailExclusionItemList = new ArrayList<>();
                            List<BankReconciliation> bankReconciliationList = new ArrayList<>();
                            //组装主表数据
                            AccountDetailExclusion accountDetailExclusion = assembleByAccountDetailExclusion(bankReconciliationMapList.get(0));
                            //组装子表数据
                            for (Map<String, Object> map : bankReconciliationMapList) {
                                AccountDetailExclusion_b accountDetailExclusionItem = assembleByAccountDetailExclusion_b(map);
                                if (null != accountDetailExclusionItem) {
                                    if (accountDetailExclusionItem.getDc_flag() == Direction.Credit.getValue()) {
                                        incomeAmount = incomeAmount.add(accountDetailExclusionItem.getEliminate_amt());
                                    } else if (accountDetailExclusionItem.getDc_flag() == Direction.Debit.getValue()) {
                                        payAmount = payAmount.add(accountDetailExclusionItem.getEliminate_amt());
                                    } else {
                                        continue;
                                    }
                                    accountDetailExclusionItem.setMainid(accountDetailExclusion.getId());
                                    accountDetailExclusionItem.setEliminateReasonType(params.getString("eliminateReasonType"));
                                    accountDetailExclusionItem.setRemovereasons(params.getString("removereasons"));
                                    accountDetailExclusionItemList.add(accountDetailExclusionItem);
                                    //回写银行对账单
                                    BankReconciliation bankReconciliation = assembleBankReconciliation(accountDetailExclusionItem);
                                    bankReconciliationList.add(bankReconciliation);
                                }
                            }
                            accountDetailExclusion.setBegin_date(begin_date);
                            accountDetailExclusion.setEnd_date(end_date);
                            accountDetailExclusion.setIncomeexclusionamount(incomeAmount);
                            accountDetailExclusion.setPayexclusionamount(payAmount);
                            accountDetailExclusion.setIsWfControlled(false);
                            accountDetailExclusion.setAccountDetailExclusion_b(accountDetailExclusionItemList);
                            //入库  起事物
                            insertByBankReconciliation(accountDetailExclusion, bankReconciliationList);
                        } catch (Exception e) {
                            log.error("updateBankReconciliation bankReconciliationMapList======" + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("账户收支明细剔除发布失败" + e);
        }
    }


    //查询对账单
    private List<Map<String, Object>> getBankReconciliationList(CtmJSONObject param, Date begin_date, Date end_date) throws ParseException {
        String use_name = (String) (Optional.ofNullable(param.get("use_name")).orElse(""));
        String remark = (String) (Optional.ofNullable(param.get("remark")).orElse(""));
        QuerySchema schema = QuerySchema.create().addSelect(" * ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("eliminateStatus").is_null());
        //交易日期
        if (begin_date != null) {
            conditionGroup.addCondition(QueryCondition.name("tran_date").egt(begin_date));
        }
        if (end_date != null) {
            conditionGroup.addCondition(QueryCondition.name("tran_date").elt(end_date));
        }
        if (!StringUtils.isEmpty(use_name)) {
            conditionGroup.addCondition(QueryCondition.name("use_name").like(use_name));
        }
        if (!StringUtils.isEmpty(remark)) {
            conditionGroup.addCondition(QueryCondition.name("remark").like(remark));
        }
        schema.addCondition(conditionGroup);
        try {
            return MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema, null);
        } catch (Exception e) {
            log.error("账户收支明细剔除调度任务：获取银行对账单数据错误" + e);
            return new ArrayList<>();
        }
    }

    /**
     * 剔除原因类型
     *
     * @param params 定转活类,资金调拨内转类,其他特殊业务类,收到电网、政府、税局计划外的补贴款、税款:国家电网补贴电费等,按照财务公司信贷部要求调整信贷计划,因为外部银行客观因素:成员公司无法正常提款,部分重大的并购款:由于集团审批比较慢等因素导致计划与实际偏离
     */
    private void eliminateReasonType(CtmJSONObject params) {
        if (!Objects.isNull(params.get("eliminateReasonType")) && !StringUtils.isEmpty(params.getString("eliminateReasonType"))) {
            String reasonType = params.get("eliminateReasonType").toString();
            switch (reasonType) {
                case "定转活类" ://@notranslate
                    reasonType = "01";
                    break;
                case "资金调拨内转类" ://@notranslate
                    reasonType = "02";
                    break;
//                case "其他特殊业务类":
//                    reasonType = "03";
//                    break;
                case "收到电网、政府、税局计划外的补贴款、税款:国家电网补贴电费等" ://@notranslate
                    reasonType = "0301";
                    break;
                case "按照财务公司信贷部要求调整信贷计划" : //@notranslate
                    reasonType = "0302";
                    break;
                case "因为外部银行客观因素:成员公司无法正常提款" ://@notranslate
                    reasonType = "0303";
                    break;
                case "部分重大的并购款:由于集团审批比较慢等因素导致计划与实际偏离" : //@notranslate
                    reasonType = "0304";
                    break;
                default:
                    reasonType = "";
            }
            if (!StringUtils.isEmpty(reasonType)) {
                params.put("eliminateReasonType", reasonType);
            }
        }
    }

    /**
     * 组装主表数据
     *
     * @param map
     * @return
     */
    private AccountDetailExclusion assembleByAccountDetailExclusion(Map<String, Object> map) throws Exception {
        AccountDetailExclusion accountDetailExclusion = new AccountDetailExclusion();
        accountDetailExclusion.setAccentity(map.get(ICsplConstant.ACCENTITY).toString());
        accountDetailExclusion.setCurrency(map.get(ICsplConstant.CURRENCY).toString());
        accountDetailExclusion.setIncomeexclusionamount(BigDecimal.ZERO);
        accountDetailExclusion.setPayexclusionamount(BigDecimal.ZERO);
        accountDetailExclusion.setSrcitem(EventSource.Cmpchase.getValue());
        accountDetailExclusion.setBilltype(EventType.AccountDetailExclusion.getValue());
        // 单据状态
        accountDetailExclusion.setDocumentstatus(CullingStatus.ExclusionCompleted.getValue());
        // 审批状态
        accountDetailExclusion.setVerifystate(VerifyState.COMPLETED.getValue());
        accountDetailExclusion.setAuditstatus(AuditStatus.Complete.getValue());
        accountDetailExclusion.setCreatorId(AppContext.getCurrentUser().getId());
        accountDetailExclusion.setCreator(AppContext.getCurrentUser().getName());
        accountDetailExclusion.setCreateDate(new Date());
        accountDetailExclusion.setCreateTime(new Date());
        accountDetailExclusion.setId(ymsOidGenerator.nextId());
        accountDetailExclusion.setEntityStatus(EntityStatus.Insert);
        accountDetailExclusion.setCode(getBillCode(accountDetailExclusion));
        accountDetailExclusion.setPubts(new Date());
        accountDetailExclusion.setVouchdate(DateUtils.getCurrentDate("yyyy-MM-dd"));
        accountDetailExclusion.setStatus(Status.confirmed.getValue());
        accountDetailExclusion.setAuditorId(AppContext.getCurrentUser().getId());
        accountDetailExclusion.setAuditor(AppContext.getCurrentUser().getName());
        accountDetailExclusion.setAuditTime(new Date());
        accountDetailExclusion.setAuditDate(BillInfoUtils.getBusinessDate());
        return accountDetailExclusion;
    }

    /**
     * 组装子表数据
     *
     * @param map
     * @return
     */
    private AccountDetailExclusion_b assembleByAccountDetailExclusion_b(Map<String, Object> map) throws Exception {
        BankReconciliation bankReconciliation = new BankReconciliation();
        bankReconciliation.init(map);
        AccountDetailExclusion_b accountDetailExclusion_b = new AccountDetailExclusion_b();
        accountDetailExclusion_b.init(map);
        accountDetailExclusion_b.setAccentity(map.get(ICsplConstant.ACCENTITY).toString());
        accountDetailExclusion_b.setCurrency(map.get(ICsplConstant.CURRENCY).toString());
        accountDetailExclusion_b.setId(ymsOidGenerator.nextId());
        accountDetailExclusion_b.setEntityStatus(EntityStatus.Insert);
        accountDetailExclusion_b.setPubts(new Date());
        accountDetailExclusion_b.setEliminate_amt(bankReconciliation.getTran_amt());
        accountDetailExclusion_b.setAfter_eliminate_amt(BigDecimal.ZERO);
        accountDetailExclusion_b.setBankReconciliationId(bankReconciliation.getId().toString());
        return accountDetailExclusion_b;
    }

    /**
     * 回写银行对账单
     *
     * @param accountDetailExclusionItem
     * @return
     */
    private BankReconciliation assembleBankReconciliation(AccountDetailExclusion_b accountDetailExclusionItem) {
        BankReconciliation bankReconciliation = new BankReconciliation();
        bankReconciliation.setId(Long.valueOf(accountDetailExclusionItem.getBankReconciliationId()));
        bankReconciliation.setEliminateStatus(CullingStatus.ExclusionCompleted.getValue());
        bankReconciliation.setRemovereasons(accountDetailExclusionItem.getRemovereasons());
        bankReconciliation.setAfter_eliminate_amt(BigDecimal.ZERO);
        bankReconciliation.setEliminate_amt(accountDetailExclusionItem.getEliminate_amt());
        bankReconciliation.setEliminateReasonType(accountDetailExclusionItem.getEliminateReasonType());
        bankReconciliation.setEntityStatus(EntityStatus.Update);
        return bankReconciliation;
    }

    /**
     * 获取单据编码
     *
     * @param accountDetail
     * @return
     */
    private String getBillCode(AccountDetailExclusion accountDetail) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.CMP_ACCOUNT_DETAIL_EXCLUSION), IBillNumConstant.CMP_ACCOUNT_DETAIL_EXCLUSION, InvocationInfoProxy.getTenantid(), null, null, new BillCodeObj[]{new BillCodeObj(accountDetail)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if (codelist != null && codelist.length > 0) {
            return codelist[0];
        } else {
            return null;
        }
    }

    /**
     * 计划明细剔除与计划汇总
     *
     * @param accountDetailExclusion
     * @param bankReconciliationMapList
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void insertByBankReconciliation(AccountDetailExclusion accountDetailExclusion, List<BankReconciliation> bankReconciliationMapList) throws Exception {
        if (CollectionUtils.isEmpty(bankReconciliationMapList)) {
            CmpMetaDaoHelper.insert(AccountDetailExclusion.ENTITY_NAME, accountDetailExclusion);
        }
        // 修改完成 全量更新
        CommonSaveUtils.updateBankReconciliation(bankReconciliationMapList);
    }

    private class AccountDetailExclusionKey {
        private String accentity;
        private String currency;

        public AccountDetailExclusionKey(final String accentity, final String currency) {
            this.accentity = accentity;
            this.currency = currency;
        }
    }


}
