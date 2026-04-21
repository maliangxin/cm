package com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.plugin.utils.PluginManager;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.collections4.CollectionUtils;
import lombok.NonNull;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BankreconciliationUtils {


    static EnterpriseBankQueryService enterpriseBankQueryService = AppContext.getBean(EnterpriseBankQueryService.class);

    /**
     * 缓存资金组织和是否参与计算
     */
    private static final @NonNull Cache<String, String> accentityNoProcessCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    public static void checkDataLegalList(List<BizObject> bills, BankreconciliationActionEnum actionEnum) {
        List<CheckAndFilterStrategy> plugins = PluginManager.getPluginList4Type(CheckAndFilterStrategy.class);
        for (CheckAndFilterStrategy plugin : plugins) {
            plugin.checkDataLegalList(bills, actionEnum);
        }
    }

    public static String checkDataLegal(BankReconciliation bill, BankreconciliationActionEnum actionEnum) {
        StringBuilder builder = new StringBuilder();
        List<CheckAndFilterStrategy> plugins = PluginManager.getPluginList4Type(CheckAndFilterStrategy.class);
        for (CheckAndFilterStrategy plugin : plugins) {
            String errorMessage = plugin.checkDataLegal(bill, actionEnum);
            builder.append(errorMessage);
        }
        return builder.toString();
    }

    public static void checkAndFilterData(List<BankReconciliation> bills, BankreconciliationScheduleEnum scheduleTaskCodeEnum) {
        List<CheckAndFilterStrategy> plugins = PluginManager.getPluginList4Type(CheckAndFilterStrategy.class);
        for (CheckAndFilterStrategy plugin : plugins) {
            plugin.checkAndFilterData(bills, scheduleTaskCodeEnum);
        }
    }

    public static boolean isNoProcess(String accentity) throws Exception {
        if (StringUtils.isEmpty(accentity)) {
            return true;
        }
        String noProcessArgsKey = InvocationInfoProxy.getTenantid().concat("noProcess").concat(accentity);
        String cacheValue = accentityNoProcessCache.getIfPresent(noProcessArgsKey);
        boolean isNoProcess = true;
        if (cacheValue != null) {
            isNoProcess = Boolean.valueOf(cacheValue);
        } else {
            //获取现金参数--无需处理的流水，是否参与银企对账、银行账户余额弥
            Map<String, Object> autoConfigMap = AppContext.getBean(AutoConfigService.class).getAutoConfigByAcc(accentity);
            if (null != autoConfigMap && null != autoConfigMap.get("isNoProcess")) {
                isNoProcess = (boolean) autoConfigMap.get("isNoProcess");
            }
            accentityNoProcessCache.put(noProcessArgsKey, String.valueOf(isNoProcess));
        }
        return isNoProcess;
    }

    public static List<String> getNoProcessDatasByAccentitys(List<String> accentityList) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("serialdealtype").eq(5));
        conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityList));
        querySchema.addCondition(conditionGroup);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(bankReconciliationList)) {
            return bankReconciliationList.stream().map(BankReconciliation::getId).map(Object::toString).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static void getAndSetAuthoruseaccentityRelationCol(String authoruseaccentity, String bankaccount, BankReconciliation bankReconciliation) throws Exception {
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bankaccount);
        List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();

        if (!StringUtils.isEmpty(authoruseaccentity)) {
            if (orgRangeVOS != null && orgRangeVOS.size() >= 1 ) {
                List<String> orgRangeVOIds = orgRangeVOS.stream()
                        .map(orgRangeVO -> orgRangeVO.getRangeOrgId())
                        .filter(orgRangeVOId -> orgRangeVOId != null)
                        .collect(Collectors.toList());
                if (orgRangeVOIds.contains(authoruseaccentity)) {
                    bankReconciliation.setAccentity(authoruseaccentity);
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FD7C51404700006", "银行账户的适用范围未包含该组织，保存失败，请检查！") /* "银行账户的适用范围未包含该组织，保存失败，请检查！:" */);

                }
            } else {
                bankReconciliation.setAccentity(null);
            }
        } else {
            if (orgRangeVOS != null && orgRangeVOS.size() == 1) {
                // 授权使用组织 只有一个
                bankReconciliation.setAccentity(orgRangeVOS.get(0).getRangeOrgId());
            } else if (orgRangeVOS != null && orgRangeVOS.size() > 1) {
                // 授权使用组织 多个
                bankReconciliation.setAccentity(null);
            } else {
                bankReconciliation.setAccentity(null);
            }
        }
        if (bankReconciliation.getAccentity() != null) {
            // 授权使用组织确认节点 银行对账单
            bankReconciliation.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
            // 确认状态 已确认
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
        } else {
            // 授权使用组织确认节点
            bankReconciliation.setConfirmbill(null);
            // 确认状态 待确认
            bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
        }
    }

}
