package com.yonyoucloud.fi.cmp.inwardremittance;

import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.enums.InwardStatus;
import com.yonyoucloud.fi.cmp.util.LocalDateUtil;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceDetailQueryRequestVO;
import com.yonyoucloud.fi.cmp.vo.InwardRemittanceListQueryRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇入汇款调度任务实现类
 *
 * @author lidchn 2023年3月13日14:05:18
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class InwardRemittanceTaskServiceImpl implements InwardRemittanceTaskService {

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Autowired
    private InwardRemittanceService inwardRemittanceService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CurrencyQueryService currencyQueryService;
    // TODO ※※※需求设计如此，通过银行类别名称进行判断，需求提供人-张素允※※※
    // 支持汇入汇款编号查询的银行类别名称列表
    private static final List<String> BANK_NAME_LIST = new ArrayList<>();
    static {
        BANK_NAME_LIST.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A1", "招商银行") /* "招商银行" */);
        BANK_NAME_LIST.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A2", "建设银行") /* "建设银行" */);
    }

    @Override
    public CtmJSONObject inwardRemittanceListQueryTask(CtmJSONObject params) {
        CtmJSONObject result = new CtmJSONObject();
        //异步调用，执行付款单自动结算任务
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                inwardRemittanceListQuery(params);
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,params.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,params.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("inwardRemittanceDetailQueryTask exception when batch process executorServicePool", e);
            }
        });
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    private void inwardRemittanceListQuery(CtmJSONObject param) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        Integer batchNum = param.getInteger("queryDaysNum") == null ? 1 : param.getInteger("queryDaysNum");
        // 查询cmp_bankaccountsetting表数据，进行遍历
        QuerySchema querySettingSchema = QuerySchema.create().addSelect("id, enterpriseBankAccount, accentity,customNo");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 开通银企联
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("openFlag").eq(1)));
        //客户号
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("customNo").is_not_null()));
        querySettingSchema.addCondition(conditionGroup);
        List<Map<String, Object>> bankAccountSettings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME,querySettingSchema);
        for (Map<String, Object> bankAccountSetting : bankAccountSettings) {
            String accEntityId = bankAccountSetting.get("accentity").toString();
            String bankAccountId = bankAccountSetting.get("enterpriseBankAccount").toString();
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccountId);
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
            for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                String currencyId = bankAcctCurrencyVO.getCurrency();
                CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(currencyId);
                InwardRemittanceListQueryRequestVO requestVO = new InwardRemittanceListQueryRequestVO();
                requestVO.setRcv_acct_no(enterpriseBankAcctVO.getAccount());
                requestVO.setCurr_code(currencyTenantDTO.getCode());
                requestVO.setBeg_num("0");
                requestVO.setEnd_date(LocalDateUtil.getNowString());
                requestVO.setCustomNo(bankAccountSetting.get("customNo").toString());
                if (batchNum <= 1) {
                    requestVO.setBeg_date(LocalDateUtil.getNowString());
                } else {
                    requestVO.setBeg_date(LocalDateUtil.getBeforeDateString(Long.valueOf(batchNum-1)));
                }
                records = inwardRemittanceService.doInwardRemittanceListQuery(requestVO, records, 0);
                // 按照银行账户+币种的维度，进行批量保存数据
                if (records != null && records.size() > 0) {
                    inwardRemittanceService.insertInwardRemittance(records, accEntityId, bankAccountId);
                }
            }
        }
    }

    @Override
    public Map inwardRemittanceResultQueryTask(CtmJSONObject params) {
        Map result = new HashMap();
        //异步调用，执行付款单自动结算任务
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                inwardRemittanceResultQuery(params);
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,params.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,params.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("inwardRemittanceDetailQueryTask exception when batch process executorServicePool", e);
            }
        });
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    private CtmJSONObject inwardRemittanceResultQuery(CtmJSONObject params) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("inwardremittancecode, inwardstatus");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.or(QueryCondition.name("inwardstatus").eq(InwardStatus.PROCESSING.getIndex())));
        querySchema.addCondition(condition);
        List<Map<String, Object>> inwardRemittancesList = MetaDaoHelper.query(InwardRemittance.ENTITY_NAME, querySchema);
        for (Map inwardRemittanceMap : inwardRemittancesList) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            Map map = new HashMap();
            map.put("inwardremittancecode", inwardRemittanceMap.get("inwardremittancecode"));
            map.put("inwardstatus", inwardRemittanceMap.get("inwardstatus"));
            jsonObject.put("data", map);
            inwardRemittanceService.inwardRemittanceResultQuery(jsonObject);
        }

        return params;
    }

    @Override
    public Map inwardRemittanceDetailQueryTask(Map params) {
        Map result = new HashMap();
        //异步调用，执行付款单自动结算任务
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                inwardRemittanceDetailQuery(params);
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, (String) params.get("logId"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FD", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,params.get("logId").toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                log.error("inwardRemittanceDetailQueryTask exception when batch process executorServicePool", e);
            }
        });
        //通知调度任务 后端为异步
        result.put("asynchronized", true);
        return result;
    }

    private void inwardRemittanceDetailQuery(Map params) throws Exception {
        QuerySchema queryBankAccountSettingSchema = QuerySchema.create().addSelect("id, accentity, enterpriseBankAccount");
        QueryConditionGroup conditionBankAccountSettingGroup = new QueryConditionGroup(ConditionOperator.and);
        // 已开通银企联
        conditionBankAccountSettingGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("openFlag").eq(1)));
        queryBankAccountSettingSchema.addCondition(conditionBankAccountSettingGroup);
        List<Map<String, Object>> bankAccountSettings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME,queryBankAccountSettingSchema);
        // 遍历cmp_bankaccountsetting表，查询所有开通银企联的银行账户
        for (Map bankAccountSettingMap : bankAccountSettings) {
            String accEntityId = (String) bankAccountSettingMap.get("accentity");
            String bankAccountId = (String) bankAccountSettingMap.get("enterpriseBankAccount");
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccountId);
            // 传入天数
            Long queryDaysNum = params.get("queryDaysNum") == null ? 3L : Long.parseLong(params.get("queryDaysNum").toString());
            String begDate = LocalDateUtil.getBeforeDateString(queryDaysNum);
            String endDate = LocalDateUtil.getNowString();
            // 招商银行，中信银行，只支持汇入汇款编号单笔查询
            if (BANK_NAME_LIST.contains(enterpriseBankAcctVO.getBankName())) {
                QuerySchema queryInwardRemittanceSchema = QuerySchema.create().addSelect("id, bankaccount, inwardremittancecode");
                QueryConditionGroup conditionInwardRemittanceGroup = new QueryConditionGroup(ConditionOperator.and);
                // 根据企业银行账户id筛选
                conditionInwardRemittanceGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(enterpriseBankAcctVO.getId())));
                queryInwardRemittanceSchema.addCondition(conditionInwardRemittanceGroup);
                List<Map<String, Object>> inwardRemittanceList = MetaDaoHelper.query(InwardRemittance.ENTITY_NAME,queryInwardRemittanceSchema);
                for (Map inwardRemittanceMap : inwardRemittanceList) {
                    String code = (String) inwardRemittanceMap.get("inwardremittancecode");
                    InwardRemittanceDetailQueryRequestVO vo = new InwardRemittanceDetailQueryRequestVO();
                    // 起始日期
                    vo.setBeg_date(begDate);
                    // 截止日期
                    vo.setEnd_date(endDate);
                    // 汇入汇款编号
                    vo.setBank_ref_no(code);
                    // 收款账号
                    vo.setRcv_acct_no(enterpriseBankAcctVO.getAccount());
                    inwardRemittanceService.inwardRemittanceDetailQuery(vo, accEntityId, bankAccountId);
                }
            } else {
                // 其他银行支持条件查询
                InwardRemittanceDetailQueryRequestVO vo = new InwardRemittanceDetailQueryRequestVO();
                vo.setBeg_date(begDate);
                vo.setEnd_date(endDate);
                vo.setRcv_acct_no(enterpriseBankAcctVO.getAccount());
                inwardRemittanceService.inwardRemittanceDetailQuery(vo, accEntityId, bankAccountId);
            }
        }
    }

}
