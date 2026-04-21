package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.org.dto.FinOrgDTO;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.model.Entity;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2024年08月26日 17:01
 * @Description:
 */
@Component
public final class CmpMetaDaoHelper {

    private static HashSet<String> stringSet;

    static {
        //stringSet.add("cmp.balanceadjustresult.BalanceadjustJournal");
        //stringSet.add("cmp.bankreconciliationsetting.BankReconciliationSetting");
        //stringSet.add("cmp.checkinventory.CheckInventory");
        //stringSet.add("cmp.checkmanage.CheckManage");
        //stringSet.add("cmp.checkstock.CheckStock");
        //stringSet.add("cmp.checkstockapply.CheckStockApply");
        //stringSet.add("cmp.inwardremittance.InwardRemittance");
        //stringSet.add("cmp.paybill.PayBill");

        //共27个加字段的DTO
        stringSet = new HashSet<>();
        stringSet.add("cm.transferaccount.TransferAccount");
        stringSet.add("cmp.accruals.AccrualsWithholding");
        //新增两字段，特殊处理［actualclaimaccentiry_raw、accentiry］
        stringSet.add("cmp.billclaim.BillClaim");
        stringSet.add("cmp.billclaim.BillClaimItem");
        stringSet.add("cmp.cashinventory.CashInventory");
        stringSet.add("cmp.currencyapply.CurrencyApply");
        stringSet.add("cmp.currencyexchange.CurrencyExchange");
        stringSet.add("cmp.exchangegainloss.ExchangeGainLoss");
        stringSet.add("cmp.fundcollection.FundCollection");
        stringSet.add("cmp.fundcollection.FundCollectionSubWithholdingRelation");
        stringSet.add("cmp.fundpayment.FundPayment");
        stringSet.add("cmp.fundpayment.FundPaymentSubWithholdingRelation");
        stringSet.add("cmp.initdata.InitDatab");
        stringSet.add("cmp.interestratesetting.InterestRateSetting");
        stringSet.add("cmp.journal.Journal");
        stringSet.add("cmp.marginworkbench.MarginWorkbench");
        stringSet.add("cmp.paymargin.PayMargin");
        stringSet.add("cmp.receivemargin.ReceiveMargin");
        stringSet.add("cmp.salarypay.Salarypay");
        stringSet.add("cmp.settlement.Settlement");
        stringSet.add("cmp.settlementdetail.SettlementDetail");
        stringSet.add("cmp.withholding.WithholdingRuleSetting");
        stringSet.add("cmp.bankreconciliation.BankReconciliation");
        //字段名不叫accentity，对DTO特殊处理
        stringSet.add("cmp.bankreconciliationsetting.BankReconciliationSetting_b");
        stringSet.add("cmp.initdata.InitData");
        stringSet.add("cmp.foreignpayment.ForeignPayment");
        //新增
        stringSet.add("cmp.settlementcheckresult.SettlementCheckResult");
    }

    private static final Logger logger = LoggerFactory.getLogger(CmpMetaDaoHelper.class);

    public static <T extends BizObject> void insert(String fullname, List<T> list)
            throws Exception {
        addAccentityRaw(fullname, list);
        MetaDaoHelper.insert(fullname, list);

    }

    public static void insert(String fullname, List<BizObject> list, String group)
            throws Exception {
        addAccentityRaw(fullname, list);
        MetaDaoHelper.insert(fullname, list, group);

    }

    public static void insert(String fullname, BizObject bill, String group)
            throws Exception {
        addAccentityRaw(fullname, bill);
        MetaDaoHelper.insert(fullname, bill, group);

    }

       /**
    * @deprecated
    */
  @Deprecated // 不支持批量操作特征
    public static <T extends BizObject> void batchInsert(Entity entity, List<T> bills)
            throws Exception {
        addAccentityRaw(entity.fullname(), bills);
        MetaDaoHelper.batchInsert(entity, bills);
    }


    public static void update(String fullname, BizObject bill, String group)
            throws Exception {
        addAccentityRaw(fullname, bill);
        EntityTool.setUpdateStatus(bill);
        MetaDaoHelper.update(fullname, bill, group);

    }

    public static <T extends BizObject> void update(String fullname, List<T> bills, String group)
            throws Exception {
        addAccentityRaw(fullname, bills);
        EntityTool.setUpdateStatus(bills);
        MetaDaoHelper.update(fullname, bills, group);

    }

    public static <T extends BizObject> void insert(String fullname, T bill)
            throws Exception {
        addAccentityRaw(fullname, bill);
        MetaDaoHelper.insert(fullname, bill);

    }

    public static <T extends BizObject> void update(String fullname, List<T> list)
            throws Exception {
        addAccentityRaw(fullname, list);
        EntityTool.setUpdateStatus(list);
        MetaDaoHelper.update(fullname, list);

    }

    public static <T extends BizObject> void update(String fullname, T bill) throws Exception {
        addAccentityRaw(fullname, bill);
        EntityTool.setUpdateStatus(bill);
        MetaDaoHelper.update(fullname, bill);

    }

    //会计组织没有值时，根据资金组织查到会计组织并赋值
    public static <T extends BizObject> void addAccentityRaw(String fullname, List<T> list)
            throws Exception {
        if (list == null) {
            return;
        }
        for (T t : list) {
            addAccentityRaw(fullname,t);
        }
    }

    //会计组织没有值时，根据资金组织查到会计组织并赋值
    public static <T extends BizObject> void addAccentityRaw(String fullname, T t)
            throws Exception {
        changeDTO(fullname, t);
        try {
            //添加审计信息，创建人和修改人没值时，给个值
            BillInfoUtils.setAddAuditInfo(t);
        } catch (Exception e) {
            logger.error("CmpMetaDaoHelper add audit info error", e);
        }
    }


    private static <T extends BizObject> void changeDTO(String fullname, T t) {
        if (!(t instanceof AccentityRawInterface)) {
            return;
        }
        AccentityRawInterface accentityRawDto = (AccentityRawInterface) t;
        if (fullname != null) {
            if (stringSet.contains(fullname)) {
                //原来的会计组织，现在代表资金组织
                String accentity = accentityRawDto.getAccentity();
                //新增的会计组织字段
                String accentityRaw = accentityRawDto.getAccentityRaw();
                if (accentity == null) {
                    return;
                }
                String actual_accentityRaw = getAccentityRaw(accentity);
                if (StringUtils.isEmpty(actual_accentityRaw)) {
                    throw new CtmException(new CtmErrorCode("033-502-102361"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050013", "资金组织对应的会计组织不存在") /* "资金组织对应的会计组织不存在" */);
                }
                logger.info("根据资金组织查询到的会计组织:{}", actual_accentityRaw);
                if (accentityRaw != null) {
                    // TODO: 2024/9/20 需要校验吗？
                    logger.info("校验资金组织和会计组织的关系");
                    //根据资金组织查询对应的会计组织
                    if(!actual_accentityRaw.equals(accentityRaw)){
                        throw new CtmException(new CtmErrorCode("033-502-102362"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050014", "资金组织与会计组织不匹配") /* "资金组织与会计组织不匹配" */);
                    }
                }else if (accentityRaw == null) {
                    logger.info("根据资金组织查询会计组织并赋值");
                    //根据资金组织查询会计组织并赋值
                    accentityRawDto.setAccentityRaw(actual_accentityRaw);
                }
            }
            //我的认领还有一个会计组织，也需要处理
            if (BillClaim.ENTITY_NAME.equals(fullname)) {
                BillClaim billClaim = (BillClaim) accentityRawDto;
                //原来的会计组织，现在代表资金组织
                String actualclaimaccentiry = billClaim.getActualclaimaccentiry();
                //新增的会计组织字段
                String actualclaimaccentiryRaw = billClaim.getactualclaimaccentiryRaw();
                if (actualclaimaccentiry == null) {
                    return;
                }
                String queryedActualclaimaccentiry_raw = getAccentityRaw(actualclaimaccentiry);
                if (StringUtils.isEmpty(queryedActualclaimaccentiry_raw)) {
                    throw new CtmException(new CtmErrorCode("033-502-102361"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050013", "资金组织对应的会计组织不存在") /* "资金组织对应的会计组织不存在" */);
                }
                logger.info("根据资金组织查询到的会计组织:{}", queryedActualclaimaccentiry_raw);
                if (actualclaimaccentiryRaw != null) {
                    // TODO: 2024/9/20 需要校验吗？
                    //根据资金组织查询会计组织并校验
                    if(!queryedActualclaimaccentiry_raw.equals(billClaim.getactualclaimaccentiryRaw())){
                        throw new CtmException(new CtmErrorCode("033-502-102362"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050014", "资金组织与会计组织不匹配") /* "资金组织与会计组织不匹配" */);
                    }
                }else if (actualclaimaccentiryRaw == null) {
                    billClaim.setactualclaimaccentiryRaw(queryedActualclaimaccentiry_raw);
                }

            }

        }
        // Modify the accentityRawDto object as needed
    }

    //根据资金组织查询会计组织
    public static String getAccentityRaw(String accentity) {
        if (accentity == null) {
            return null;
        }
        String accentityRaw = null;
        List<String> accentityList = new ArrayList<>();
        accentityList.add(accentity);
        FundsOrgQueryServiceComponent fundsOrgQueryService = AppContext.getBean(FundsOrgQueryServiceComponent.class);
        Map<String, FinOrgDTO> stringFinOrgDTOMap = fundsOrgQueryService.queryAccEntityByFundOrgIds(accentityList);
        FinOrgDTO finOrgDTO = stringFinOrgDTOMap.get(accentity);
        if (finOrgDTO != null) {
            accentityRaw = finOrgDTO.getId();
        }
        return accentityRaw;
    }

    public static <T extends Map<String, Object>> List<T> query(String fullname, QuerySchema schema) throws Exception {
        if (BankReconciliation.ENTITY_NAME.equals(fullname)) {
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.or);
            queryConditionGroup.addCondition(QueryCondition.name(BankReconciliation.ISREPEAT).eq(BankDealDetailConst.REPEAT_NORMAL));
            queryConditionGroup.addCondition(QueryCondition.name(BankReconciliation.ISREPEAT).eq(BankDealDetailConst.REPEAT_INIT));
            queryConditionGroup.addCondition(QueryCondition.name(BankReconciliation.ISREPEAT).is_null());
            schema.queryConditionGroup().appendCondition(queryConditionGroup);
        }
        return MetaDaoHelper.query(fullname, schema);
    }

    public static <T extends BizObject> List<T> queryColByOneCondition(String uri, ConditionOperator conditionOperator, String fieldName, Object fieldValue, String... cols) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(cols);
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryCondition queryCondition = new QueryCondition(fieldName, conditionOperator, fieldValue);
        conditionGroup.appendCondition(queryCondition);
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = query(uri, schema);
        List<T> bizObjectList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)){
            list.stream().filter(Objects::nonNull).forEach(p->{
                T bizObject = Objectlizer.convert(p, uri);
                bizObjectList.add(bizObject);
            });
        }
        return bizObjectList;
    }

    public static <T extends BizObject> List<T> queryColByOneEqualCondition(String uri, String fieldName, Object fieldValue, String... cols) throws Exception {
        return queryColByOneCondition(uri, ConditionOperator.eq, fieldName, fieldValue, cols);
    }

    /**
     *
     * 将返回的数据转换成对应的DTO，防止类型转换报错
     * @param fullname
     * @param selectFieldString
     * @param ids
     * @return
     * @param <T>
     * @throws Exception
     */
    public static  <T extends BizObject> List<T> queryByIds(String fullname, String selectFieldString, List<String> ids) throws Exception {
        List<Map<String, Object>> list = MetaDaoHelper.queryByIds(fullname, selectFieldString, ids);
        List<T> bizObjectList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)){
            list.stream().filter(Objects::nonNull).forEach(p->{
                T bizObject = Objectlizer.convert(p, fullname);
                bizObjectList.add(bizObject);
            });
        }
        return bizObjectList;

    }

    /**
     *
     * 将返回的数据转换成对应的DTO，防止类型转换报错
     * @param fullname
     * @param schema
     * @return
     * @param <T>
     * @throws Exception
     */
    public static <T extends BizObject> List<T> queryDTOList(String fullname, QuerySchema schema) throws Exception {
        List<Map<String, Object>> list = MetaDaoHelper.query(fullname, schema);
        List<T> bizObjectList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)){
            list.stream().filter(Objects::nonNull).forEach(p->{
                T bizObject = Objectlizer.convert(p, fullname);
                bizObjectList.add(bizObject);
            });
        }
        return bizObjectList;
    }

}
