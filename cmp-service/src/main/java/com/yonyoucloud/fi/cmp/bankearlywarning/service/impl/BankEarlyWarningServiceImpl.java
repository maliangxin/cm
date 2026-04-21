package com.yonyoucloud.fi.cmp.bankearlywarning.service.impl;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankearlywarning.service.BankEarlyWarningService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.BillProcessFlag;
import com.yonyoucloud.fi.cmp.cmpentity.OprType;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h1>BankEarlyWarningServiceImpl</h1>
 *
 * @author yp
 * @version 1.0
 * @since 2022-10-09 10:46
 */
@Service
@Slf4j
public class BankEarlyWarningServiceImpl implements BankEarlyWarningService {
    @Override
    public Map<String, Object> bankSlipUnDealWarningTask(String accentityStr, Integer distribute, Integer publishDistribute, String logId, String tenant) {
        CtmJSONArray data = new CtmJSONArray();
        Map<String, Object> result = new HashMap<>();
        int status = TaskUtils.TASK_BACK_SUCCESS;

        String msg = "";
        try {
            String[] accentitys = null;
            if (StringUtil.isNotEmpty(accentityStr)) {
                accentitys = accentityStr.split(";");
            }
            //默认至少为2天
            if (null == distribute ) {
                distribute = 2;
            }
            //默认至少为2天
            if (null == publishDistribute) {
                publishDistribute = 2;
            }
            List<BankReconciliation> bankRecList = queryBankReconciliation(tenant, accentitys, AssociationStatus.NoAssociated.getValue(), null);
            List<BankReconciliation> list = queryBankReconciliation(tenant, accentitys, AssociationStatus.Associated.getValue(), Short.valueOf("1"));
            bankRecList.addAll(list);
            List<BankReconciliation> financeWarningList = new ArrayList<>();
            List<BankReconciliation> businessWarningList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(bankRecList)) {
                for (int i = 0; i < bankRecList.size(); i++) {
                    BankReconciliation bankReconciliation = bankRecList.get(i);
                    List<BankReconciliationDetail> details = getBankReconciliationDetails(bankReconciliation);
                    if (checkNeedWarning(details, distribute, publishDistribute)) {
                        financeWarningList.add(bankReconciliation);
                    }
                    if (checkNeedReleaseWarning(details, distribute, publishDistribute)) {
                        businessWarningList.add(bankReconciliation);
                    }
                }
            }
            bullidResultData(data, financeWarningList, businessWarningList);
        } catch (Exception e) {
            // 任务执行结果 0-失败
            status = TaskUtils.TASK_BACK_FAILURE;
            log.error("BankEarlyWarningServiceImpl error, e = {}", e.getMessage());
            msg = e.getMessage();
        } finally {
            log.error("BankEarlyWarningServiceImpl Warning Task, status = {}, logId = {}, content = {}, tenant = {}",
                    status, logId, data, tenant);
            //执行结果： 0：失败；1：成功
            result.put("status", status);
            //业务方自定义结果集字段
            result.put("data", data);
            //	异常信息
            result.put("msg", msg);
            AppContext.clear();
        }
        return result;
    }

    private List<BankReconciliationDetail> getBankReconciliationDetails(BankReconciliation bankReconciliation) throws Exception {
        Long id = bankReconciliation.getId();
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("mainid").eq(id));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(BankReconciliationDetail.ENTITY_NAME, querySchema, null);
    }

    /**
     * 构建返回的告警信息
     *
     * @param data               返回的信息
     * @param financeWarningList 相关参数（会计主体、账号、币种等）
     */
    private void bullidResultData(CtmJSONArray data, List<BankReconciliation> financeWarningList, List<BankReconciliation> businessWarningList) {
        if (CollectionUtils.isNotEmpty(financeWarningList)) {
            for (BankReconciliation bankReconciliation : financeWarningList) {
                CtmJSONObject object = new CtmJSONObject();
                StringBuffer sb = new StringBuffer(); // 动态拼接
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050042", "资金组织") /* "资金组织" */);
                sb.append(":");
                sb.append(bankReconciliation.get("accentityName").toString());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037E","账号") /* "账号" */);
                sb.append(":");
                sb.append(bankReconciliation.get("account").toString());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180380","币种") /* "币种" */);
                sb.append(":");
                sb.append(bankReconciliation.get("currencyName").toString());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180381","方向为") /* "方向为" */);
                sb.append(bankReconciliation.getDc_flag().getName());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037B","金额") /* "金额" */);
                sb.append(":");
                sb.append(bankReconciliation.getTran_amt());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037F","分派财务人员后未处理，请关注") /* "分派财务人员后未处理，请关注" */);
                object.put("msg", sb.toString());
                data.add(object);
            }
        }

        if (CollectionUtils.isNotEmpty(businessWarningList)) {
            for (BankReconciliation bankReconciliation : businessWarningList) {
                CtmJSONObject object = new CtmJSONObject();
                StringBuffer sb = new StringBuffer(); // 动态拼接
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050042", "资金组织") /* "资金组织" */);
                sb.append(":");
                sb.append(bankReconciliation.get("accentityName").toString());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037E","账号") /* "账号" */);
                sb.append(":");
                sb.append(bankReconciliation.get("account").toString());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180380","币种") /* "币种" */);
                sb.append(":");
                sb.append(bankReconciliation.get("currencyName").toString());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180381","方向为") /* "方向为" */);
                sb.append(bankReconciliation.getDc_flag().getName());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037B","金额") /* "金额" */);
                sb.append(":");
                sb.append(bankReconciliation.getTran_amt());
                sb.append(",");
                sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418037D","分配业务人员后未处理，请关注") /* "分配业务人员后未处理，请关注" */);
                object.put("msg", sb.toString());
                data.add(object);
            }
        }
    }


    private boolean checkNeedWarning(List<BankReconciliationDetail> details, Integer distribute, Integer publishDistribute) throws Exception {
        if (CollectionUtils.isNotEmpty(details)) {
            Date nowDate = DateUtils.getNowDateShort2();
            for (int i = 0; i < details.size(); i++) {
                BankReconciliationDetail detail = details.get(i);
                Date oprdate = detail.getOprdate();
                Date returndate = detail.getReturndate();
                String oprtype = detail.getOprtype();
                //分派
                if (OprType.AutoFinance.getValue().equals(oprtype) || OprType.ManualFinance.getValue().equals(oprtype)) {
                    if (returndate == null && oprdate != null) {
                        Date date = DateUtils.dateAdd(oprdate, distribute, false);
                        if (nowDate.after(date) || nowDate.equals(date)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean checkNeedReleaseWarning(List<BankReconciliationDetail> details, Integer distribute, Integer publishDistribute) throws Exception {
        if (CollectionUtils.isNotEmpty(details)) {
            Date nowDate = DateUtils.getNowDateShort2();
            for (int i = 0; i < details.size(); i++) {
                BankReconciliationDetail detail = details.get(i);
                String oprtype = detail.getOprtype();
                Date oprdate = detail.getOprdate();
                Date returndate = detail.getReturndate();
                //分派
                if (OprType.AutoBusiness.getValue().equals(oprtype) || OprType.ManualBusiness.getValue().equals(oprtype)) {
                    if (returndate == null && oprdate != null) {
                        Date date = DateUtils.dateAdd(oprdate, publishDistribute, false);
                        if (nowDate.after(date) || nowDate.equals(date)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private List<BankReconciliation> queryBankReconciliation(String tenant, String[] accentitys, short associationstatus, Short times) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect
                ("id,accentity,accentity,accentity.name as accentityName,currency,currency.name as currencyName,bankaccount,bankaccount.account as account,dzdate,dc_flag,tran_amt");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        //queryConditionGroup.addCondition(QueryCondition.name("ytenant").eq(tenant));
        queryConditionGroup.addCondition(QueryCondition.name("billprocessflag").eq(BillProcessFlag.NeedDeal.getValue()));
        queryConditionGroup.addCondition(QueryCondition.name("associationstatus").eq(associationstatus));
        if (times != null) {
            queryConditionGroup.addCondition(QueryCondition.name("associationcount").eq(times));
        }
        if (accentitys != null && accentitys.length > 0) {
            queryConditionGroup.addCondition(QueryCondition.name("accentity").in(accentitys));
        }
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
    }


}
