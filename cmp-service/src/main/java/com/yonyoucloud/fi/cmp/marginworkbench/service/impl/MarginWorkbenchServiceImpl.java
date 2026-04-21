package com.yonyoucloud.fi.cmp.marginworkbench.service.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.MarginFlag;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author duanhj12
 * @since 2023/7/26 13:43
 */
@Slf4j
@Service
public class MarginWorkbenchServiceImpl implements MarginWorkbenchService {

    public static final String PAYMARGINPAYMENT = "cmp_paymargin_payment";//支付保证金
    public static final String PAYMARGINWITHDRAW = "cmp_paymargin_withdraw";//取回保证金
    public static final String RECMARGINRECEIVE = "cmp_receivemargin_receive";//收到保证金
    public static final String RECMARGINRETURN = "cmp_receivemargin_return";//退还保证金

    //更新工作台 各种金额
    private static final String MARGINWORKBENCHMAPPER = "com.yonyoucloud.fi.cmp.mapper.MarginWorkbenchMapper.updateworkbench";

    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    private CmCommonService cmCommonService;

    @Override
    public String payMarginWorkbenchSave(CtmJSONObject params) throws Exception {
        Object payMargin = params.get(ICmpConstant.PAYMARGIN);
        PayMargin margin = (PayMargin) payMargin;
        //对事项来源 和  保证金原始业务号 加锁，相当于对虚拟账户加锁，防止出现创建相同的虚拟账户和多个线程对同一个虚拟账户操作的情况
        String lockKey = "" + margin.getSrcitem() + "_" + margin.getMarginbusinessno();
        YmsLock ymsLock = null;
        try {

            List<MarginWorkbench> marginWorkbenchList = queryWorkbenchByCondition(margin.getMarginbusinessno(), margin.getSrcitem(), MarginFlag.PayMargin.getValue(), margin.getAccentity(), margin.getCurrency());
            if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
                //更新保证金虚拟账户
                MarginWorkbench payWorkbench = marginWorkbenchList.get(0);
                //交易类型为取回保证金时更新保证金虚拟户保证金可用余额=保存前保证金可用余额-（保存后保证金金额-保存前保证金金额）-（保存后转换金额-保存前转换金额）
//                String typeCode = cmCommonService.getDefaultTransTypeCode(margin.getTradetype());
                List<BdTransType> transTypes = cmCommonService.getTransTyp(margin.getTradetype());
                String typeCode = transTypes.get(0).getCode();
                String extendAttrsJson = transTypes.get(0).getExtendAttrsJson();
                if ((PAYMARGINWITHDRAW.equals(typeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW)) && margin.getId() != null) {
                    //编辑修改 交易类型为取回保证金 的单据
                    updateMarginWorkbenchAmountFields(params, payWorkbench, MarginFlag.PayMargin.getValue());
                } else if ((PAYMARGINWITHDRAW.equals(typeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW)) && margin.getId() == null) {
                    //新增 交易类型为取回保证金 的单据
                    updateMarginWorkbenchAmountFieldsForWithdrawOrReturnType(params, payWorkbench, MarginFlag.PayMargin.getValue());
                }
                updateMarginWorkbenchNonAmountField((BizObject) params.get(ICmpConstant.PAYMARGIN), payWorkbench, MarginFlag.PayMargin.getValue());
                return payWorkbench.getId().toString();
            } else {
                ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
                if (ymsLock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101499"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280013", "已有他人创建或操作同一保证金虚拟账户，请稍候重试！") /* "已有他人创建或操作同一保证金虚拟账户，请稍候重试！" */);
                }
                //创建保证金虚拟账户
                return addMarginWorkbench(payMargin);
            }
        } catch (CtmException e) {
            log.error("创建或操作保证金虚拟账户时异常：" + e.getMessage());
            throw e;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    private void updateMarginWorkbenchNonAmountField(BizObject margin, MarginWorkbench marginWorkbench, short marginFlag) throws Exception {
        MarginWorkbench updateMarginWorkbench;
        boolean isByMultipleMargin = checkIfMultipleMarginByVirtualAccount(marginWorkbench.getId(), marginFlag);
        if (isByMultipleMargin) {
            return;
        }
        if (MarginFlag.PayMargin.getValue() == marginFlag) {
            updateMarginWorkbench = buildPayMarginWorkbench((PayMargin) margin);
        } else {
            updateMarginWorkbench = buildRecMarginWorkbench((ReceiveMargin) margin);
        }
        updateMarginWorkbench.setId(marginWorkbench.getId());
        updateMarginWorkbench.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(MarginWorkbench.ENTITY_NAME, updateMarginWorkbench);
    }

    private boolean checkIfMultipleMarginByVirtualAccount(Long workbenchId, short marginFlag) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginvirtualaccount").eq(workbenchId));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<? extends BizObject> marginList;
        if (MarginFlag.PayMargin.getValue() == marginFlag) {
            marginList = MetaDaoHelper.<PayMargin>queryObject(PayMargin.ENTITY_NAME, querySchema, null);
        } else {
            marginList = MetaDaoHelper.<ReceiveMargin>queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
        }
        return !CollectionUtils.isEmpty(marginList) && marginList.size() > 1;
    }

    @Override
    public void payMarginWorkbenchUpdate(CtmJSONObject params) throws Exception {
        String businessNo = params.getString(ICmpConstant.MARGINBUSINESSNO);
        Short srcItem = params.getShort(ICmpConstant.SRC_ITEM);
        PayMargin payMargin = (PayMargin) params.get(ICmpConstant.PAYMARGIN);
        //对事项来源 和  保证金原始业务号 加锁，相当于对虚拟账户加锁，防止出现相同的虚拟账户和多个线程对同一个虚拟账户操作的情况
        String lockKey = "" + srcItem + "_" + businessNo;

        try {

            List<MarginWorkbench> marginWorkbenchList = queryWorkbenchByCondition(businessNo, srcItem, MarginFlag.PayMargin.getValue(), payMargin.getAccentity(), payMargin.getCurrency());
            if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
                marginWorkbenchHandle(params, marginWorkbenchList.get(0), MarginFlag.PayMargin.getValue());
            }
        } catch (CtmException e) {
            log.error("操作保证金虚拟账户时异常：" + e.getMessage());
            throw e;
        }
    }

    @Override
    public String recMarginWorkbenchSave(CtmJSONObject params) throws Exception {
        Object recMargin = params.get(ICmpConstant.RECMARGIN);
        ReceiveMargin margin = (ReceiveMargin) recMargin;
        //对事项来源 和  保证金原始业务号 加锁，相当于对虚拟账户加锁，防止出现创建相同的虚拟账户和多个线程对同一个虚拟账户操作的情况
        String lockKey = "" + margin.getSrcitem() + "_" + margin.getMarginbusinessno();
        YmsLock ymsLock = null;
        try {

            List<MarginWorkbench> marginWorkbenchList = queryWorkbenchByCondition(margin.getMarginbusinessno(), margin.getSrcitem(), MarginFlag.RecMargin.getValue(), margin.getAccentity(), margin.getCurrency());
            if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
                //更新保证金虚拟账户
                MarginWorkbench recWorkbench = marginWorkbenchList.get(0);
//                String typeCode = cmCommonService.getDefaultTransTypeCode(margin.getTradetype());
                List<BdTransType> transTypes = cmCommonService.getTransTyp(margin.getTradetype());
                String typeCode = transTypes.get(0).getCode();
                String extendAttrsJson = transTypes.get(0).getExtendAttrsJson();
                if ((RECMARGINRETURN.equals(typeCode) || extendAttrsJson.contains(RECMARGINRETURN)) && margin.getId() != null) {
                    //编辑修改 交易类型为退还保证金 的单据
                    updateMarginWorkbenchAmountFields(params, recWorkbench, MarginFlag.RecMargin.getValue());
                } else if ((RECMARGINRETURN.equals(typeCode) || extendAttrsJson.contains(RECMARGINRETURN)) && margin.getId() == null) {
                    //新增 交易类型为退还保证金 的单据
                    updateMarginWorkbenchAmountFieldsForWithdrawOrReturnType(params, recWorkbench, MarginFlag.RecMargin.getValue());
                }
                updateMarginWorkbenchNonAmountField((BizObject) params.get(ICmpConstant.RECMARGIN), recWorkbench, MarginFlag.RecMargin.getValue());
                return recWorkbench.getId().toString();
            } else {
                ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
                if (null == ymsLock) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101499"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280013", "已有他人创建或操作同一保证金虚拟账户，请稍候重试！") /* "已有他人创建或操作同一保证金虚拟账户，请稍候重试！" */);
                }
                //创建保证金虚拟账户
                return addMarginWorkbench(recMargin);
            }
        } catch (CtmException e) {
            log.error("创建或操作保证金虚拟账户时异常：" + e.getMessage());
            throw e;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    @Override
    public void recMarginWorkbenchUpdate(CtmJSONObject params) throws Exception {
        String businessNo = params.getString(ICmpConstant.MARGINBUSINESSNO);
        Short srcItem = params.getShort(ICmpConstant.SRC_ITEM);
        ReceiveMargin receiveMargin = (ReceiveMargin) params.get(ICmpConstant.RECMARGIN);
        //对事项来源 和  保证金原始业务号 加锁，相当于对虚拟账户加锁，防止出现相同的虚拟账户和多个线程对同一个虚拟账户操作的情况
        String lockKey = "" + srcItem + "_" + businessNo;

        try {

            List<MarginWorkbench> marginWorkbenchList = queryWorkbenchByCondition(businessNo, srcItem, MarginFlag.RecMargin.getValue(), receiveMargin.getAccentity(), receiveMargin.getCurrency());
            if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
                marginWorkbenchHandle(params, marginWorkbenchList.get(0), MarginFlag.RecMargin.getValue());
            }
        } catch (CtmException e) {
            log.error("操作保证金虚拟账户时异常：" + e.getMessage());
            throw e;
        }
    }

    @Override
    public void delRecMarginWorkbench(CtmJSONObject params) throws Exception {
        String businessNo = params.getString(ICmpConstant.MARGINBUSINESSNO);
        Short srcItem = params.getShort(ICmpConstant.SRC_ITEM);
        String marginworkbenchId = params.getString(ICmpConstant.MARGINWORKBENCH_ID);
        //对事项来源 和  保证金原始业务号 加锁，相当于对虚拟账户加锁，防止出现相同的虚拟账户和多个线程对同一个虚拟账户操作的情况
        String lockKey = "" + srcItem + "_" + businessNo;
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101500"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280012", "已有他人操作同一保证金虚拟账户，请稍候重试！") /* "已有他人操作同一保证金虚拟账户，请稍候重试！" */);
            }
            if (useByMulRecMargin(marginworkbenchId)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101501"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1920C05A04500006", "保证金虚拟户下除转换生成的台账单据外存在其他关联的保证金台账单据，不允许删除！") /* "保证金虚拟户下除转换生成的台账单据外存在其他关联的保证金台账单据，不允许删除！" */);
            } else {
                MetaDaoHelper.deleteByObjectId(MarginWorkbench.ENTITY_NAME, marginworkbenchId);
            }
        } catch (CtmException e) {
            log.error("操作保证金虚拟账户时异常：{}", e.getMessage(), e);
            throw e;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    @Override
    public void delPayMarginWorkbench(CtmJSONObject params) throws Exception {
        String businessNo = params.getString(ICmpConstant.MARGINBUSINESSNO);
        Short srcItem = params.getShort(ICmpConstant.SRC_ITEM);
        String marginworkbenchId = params.getString(ICmpConstant.MARGINWORKBENCH_ID);
        //对事项来源 和  保证金原始业务号 加锁，相当于对虚拟账户加锁，防止出现相同的虚拟账户和多个线程对同一个虚拟账户操作的情况
        String lockKey = "" + srcItem + "_" + businessNo;
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101500"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280012", "已有他人操作同一保证金虚拟账户，请稍候重试！") /* "已有他人操作同一保证金虚拟账户，请稍候重试！" */);
            }
            if (useByMulPayMargin(marginworkbenchId)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101501"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1920C05A04500006", "保证金虚拟户下除转换生成的台账单据外存在其他关联的保证金台账单据，不允许删除！") /* "保证金虚拟户下除转换生成的台账单据外存在其他关联的保证金台账单据，不允许删除！" */);
            } else {
                MetaDaoHelper.deleteByObjectId(MarginWorkbench.ENTITY_NAME, marginworkbenchId);
            }
        } catch (CtmException e) {
            log.error("操作保证金虚拟账户时异常：" + e.getMessage());
            throw e;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    private boolean useByMulPayMargin(String workBenchId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginvirtualaccount").eq(workBenchId));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<PayMargin> payMarginList = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
        return payMarginList != null && payMarginList.size() > 1;
    }

    private boolean useByMulRecMargin(String workBenchId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginvirtualaccount").eq(workBenchId));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<ReceiveMargin> payMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
        return payMarginList != null && payMarginList.size() > 1;
    }

    private String addMarginWorkbench(Object margin) throws Exception {
        //保存
        MarginWorkbench workbench = null;
        BillDataDto billDataDto = new BillDataDto();
        if (margin instanceof PayMargin) {
            workbench = buildPayMarginWorkbench((PayMargin) margin);
            billDataDto.setBillnum(IBillNumConstant.CMP_PAYMARGINWORKBENCHLIST);
        } else {
            workbench = buildRecMarginWorkbench((ReceiveMargin) margin);
            billDataDto.setBillnum(IBillNumConstant.CMP_RECMARGINWORKBENCHLIST);
        }
        workbench.setEntityStatus(EntityStatus.Insert);
        billDataDto.setData(workbench);
        RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
        BizObject result = (BizObject) ruleExecuteResult.getData();
        return result.getString(ICmpConstant.ID);
    }

    private void updateMarginWorkbenchAmountFields(CtmJSONObject params, MarginWorkbench workbench, Short flag) throws Exception {
//        BigDecimal balance = workbench.getMarginAvailableBalance() == null ? BigDecimal.ZERO : workbench.getMarginAvailableBalance();
//        BigDecimal natBalance = workbench.getNatMarginAvailableBalance() == null ? BigDecimal.ZERO : workbench.getNatMarginAvailableBalance();
        //改造
        BigDecimal marginAvailableBalance = BigDecimal.ZERO;
        BigDecimal natMarginAvailableBalance = BigDecimal.ZERO;

        BigDecimal dbMarginAmount = params.getBigDecimal(ICmpConstant.DBMARGINAMOUNT) == null ? BigDecimal.ZERO : params.getBigDecimal(ICmpConstant.DBMARGINAMOUNT);
        BigDecimal dbConversionAmount = params.getBigDecimal(ICmpConstant.DBCONVERSIONAMOUNT) == null ? BigDecimal.ZERO : params.getBigDecimal(ICmpConstant.DBCONVERSIONAMOUNT);
        BigDecimal dbNatMarginAmount = params.getBigDecimal(ICmpConstant.DBNATMARGINAMOUNT) == null ? BigDecimal.ZERO : params.getBigDecimal(ICmpConstant.DBNATMARGINAMOUNT);
        BigDecimal dbNatConversionAmount = params.getBigDecimal(ICmpConstant.DBNATCONVERSIONAMOUNT) == null ? BigDecimal.ZERO : params.getBigDecimal(ICmpConstant.DBNATCONVERSIONAMOUNT);
        //更新保证金虚拟户保证金可用余额=保存前保证金可用余额-（保存后保证金金额-保存前保证金金额）-（保存后转换金额-保存前转换金额）。
        if (MarginFlag.PayMargin.getValue() == flag) {
            PayMargin payMargin = (PayMargin) params.get(ICmpConstant.PAYMARGIN);
            BigDecimal marginAmountDiff = payMargin.getMarginamount().subtract(dbMarginAmount);
            BigDecimal conversionAmount = payMargin.getConversionamount() == null ? BigDecimal.ZERO : payMargin.getConversionamount();
            BigDecimal conversionAmountDiff = conversionAmount.subtract(dbConversionAmount);
//            workbench.setMarginAvailableBalance(balance.subtract(marginAmountDiff).subtract(conversionAmountDiff));
            marginAvailableBalance = (marginAvailableBalance.subtract(marginAmountDiff).subtract(conversionAmountDiff));
            BigDecimal natMarginAmountDiff = payMargin.getNatmarginamount().subtract(dbNatMarginAmount);
            BigDecimal natConversionAmount = payMargin.getConversionamount() == null ? BigDecimal.ZERO : payMargin.getConversionamount();
            BigDecimal natConversionAmountDiff = natConversionAmount.subtract(dbNatConversionAmount);
//            workbench.setNatMarginAvailableBalance(natBalance.subtract(natMarginAmountDiff).subtract(natConversionAmountDiff));
            natMarginAvailableBalance = (natMarginAvailableBalance.subtract(natMarginAmountDiff).subtract(natConversionAmountDiff));
        } else {
            ReceiveMargin recMargin = (ReceiveMargin) params.get(ICmpConstant.RECMARGIN);
            BigDecimal marginAmountDiff = recMargin.getMarginamount().subtract(dbMarginAmount);
            BigDecimal conversionAmount = recMargin.getConversionamount() == null ? BigDecimal.ZERO : recMargin.getConversionamount();
            BigDecimal conversionAmountDiff = conversionAmount.subtract(dbConversionAmount);
            workbench.setMarginAvailableBalance(marginAvailableBalance.subtract(marginAmountDiff).subtract(conversionAmountDiff));
            marginAvailableBalance = (marginAvailableBalance.subtract(marginAmountDiff).subtract(conversionAmountDiff));

            BigDecimal natMarginAmountDiff = recMargin.getNatmarginamount().subtract(dbNatMarginAmount);
            BigDecimal natConversionAmount = recMargin.getConversionamount() == null ? BigDecimal.ZERO : recMargin.getConversionamount();
            BigDecimal natConversionAmountDiff = natConversionAmount.subtract(dbNatConversionAmount);
//            workbench.setNatMarginAvailableBalance(natBalance.subtract(natMarginAmountDiff).subtract(natConversionAmountDiff));
            natMarginAvailableBalance = (natMarginAvailableBalance.subtract(natMarginAmountDiff).subtract(natConversionAmountDiff));

        }
        //更新
        Map<String, Object> map = new HashMap<>(8);
        map.put("id", workbench.getId());
        map.put("ytenant_id", InvocationInfoProxy.getTenantid());
        map.put("marginAvailableBalance", marginAvailableBalance);
        map.put("natMarginAvailableBalance", natMarginAvailableBalance);
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("from", "updateMarginWorkbenchAmountFields");
        logparam.put("params", params);
        logparam.put("marginWorkbeanch", workbench);
        logparam.put("flag", flag);
        AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, workbench.getMarginBusinessNo(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */, IServicecodeConstant.PAYMARGIN, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */);
        SqlHelper.update(MARGINWORKBENCHMAPPER, map);
//        workbench.setEntityStatus(EntityStatus.Update);
//        MetaDaoHelper.update(MarginWorkbench.ENTITY_NAME, workbench);
    }

    private void updateMarginWorkbenchAmountFieldsForWithdrawOrReturnType(CtmJSONObject params, MarginWorkbench workbench, Short flag) throws Exception {
        BigDecimal marginAvailableBalance = BigDecimal.ZERO;
        BigDecimal natMarginAvailableBalance = BigDecimal.ZERO;
        //更新保证金虚拟户保证金可用余额=保存前保证金可用余额-保证金金额-转换金额。
        if (MarginFlag.PayMargin.getValue() == flag) {
            PayMargin payMargin = (PayMargin) params.get(ICmpConstant.PAYMARGIN);
            BigDecimal marginamount = payMargin.getMarginamount() == null ? BigDecimal.ZERO : payMargin.getMarginamount();
            BigDecimal conversionamount = payMargin.getConversionamount() == null ? BigDecimal.ZERO : payMargin.getConversionamount();
            marginAvailableBalance = (marginAvailableBalance.subtract(marginamount).subtract(conversionamount));
            BigDecimal natmarginamount = payMargin.getNatmarginamount() == null ? BigDecimal.ZERO : payMargin.getNatmarginamount();
            BigDecimal natconversionamount = payMargin.getNatconversionamount() == null ? BigDecimal.ZERO : payMargin.getNatconversionamount();
            natMarginAvailableBalance = (natMarginAvailableBalance.subtract(natmarginamount).subtract(natconversionamount));
        } else {
            ReceiveMargin recMargin = (ReceiveMargin) params.get(ICmpConstant.RECMARGIN);
            BigDecimal marginamount = recMargin.getMarginamount() == null ? BigDecimal.ZERO : recMargin.getMarginamount();
            BigDecimal conversionamount = recMargin.getConversionamount() == null ? BigDecimal.ZERO : recMargin.getConversionamount();
            marginAvailableBalance = (marginAvailableBalance.subtract(marginamount).subtract(conversionamount));
            BigDecimal natmarginamount = recMargin.getNatmarginamount() == null ? BigDecimal.ZERO : recMargin.getNatmarginamount();
            BigDecimal natconversionamount = recMargin.getNatconversionamount() == null ? BigDecimal.ZERO : recMargin.getNatconversionamount();
            natMarginAvailableBalance = (natMarginAvailableBalance.subtract(natmarginamount).subtract(natconversionamount));
        }
        //更新
        Map<String, Object> map = new HashMap<>(8);
        map.put("id", workbench.getId());
        map.put("ytenant_id", InvocationInfoProxy.getTenantid());
        map.put("marginAvailableBalance", marginAvailableBalance);
        map.put("natMarginAvailableBalance", natMarginAvailableBalance);
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("from", "updateMarginWorkbenchAmountFieldsForWithdrawOrReturnType");
        logparam.put("params", params);
        logparam.put("marginWorkbeanch", workbench);
        logparam.put("flag", flag);
        AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, workbench.getMarginBusinessNo(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */, IServicecodeConstant.PAYMARGIN, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */);
        SqlHelper.update(MARGINWORKBENCHMAPPER, map);
//        workbench.setEntityStatus(EntityStatus.Update);
//        MetaDaoHelper.update(MarginWorkbench.ENTITY_NAME, workbench);
    }

    private void marginWorkbenchHandle(CtmJSONObject params, MarginWorkbench workbench, Short flag) throws Exception {
        String action = params.getString(ICmpConstant.ACTION);
        List<BdTransType> transTypes = cmCommonService.getTransTyp(params.getString(ICmpConstant.TRADETYPE));
        String tradeTypeCode = transTypes.get(0).getCode();
        String extendAttrsJson = transTypes.get(0).getExtendAttrsJson();
        BigDecimal marginAmount = params.getBigDecimal(ICmpConstant.MARGINAMOUNT);
        BigDecimal natMarginAmount = params.getBigDecimal(ICmpConstant.NATMARGINAMOUNT);
        BigDecimal conversionAmount = !params.containsKey(ICmpConstant.CONVERSIONAMOUNT) ? BigDecimal.ZERO : params.getBigDecimal(ICmpConstant.CONVERSIONAMOUNT);
        BigDecimal natConversionAmount = !params.containsKey(ICmpConstant.NATCONVERSIONAMOUNT) ? BigDecimal.ZERO : params.getBigDecimal(ICmpConstant.NATCONVERSIONAMOUNT);
        Boolean settleFlag = params.getBoolean(ICmpConstant.SETTLEFLAG);
        Short settleStatus = params.getShort(ICmpConstant.SETTLE_STATUS);
        Boolean settleStatusChange = params.getBoolean(ICmpConstant.SETTLESTATUSCHANGE);

        //金额防空赋值
        marginAmount = marginAmount == null ? BigDecimal.ZERO : marginAmount;
        natMarginAmount = natMarginAmount == null ? BigDecimal.ZERO : natMarginAmount;
        conversionAmount = conversionAmount == null ? BigDecimal.ZERO : conversionAmount;
        natConversionAmount = natConversionAmount == null ? BigDecimal.ZERO : natConversionAmount;

        //支付保证金工作台字段
        //payAmount 支付金额
        BigDecimal payAmount = BigDecimal.ZERO;
        //natPayAmount 本币支付金额
        BigDecimal natPayAmount = BigDecimal.ZERO;
        //retrieveAmount 取回金额
        BigDecimal retrieveAmount = BigDecimal.ZERO;
        //natRetrieveAmount 本币取回金额
        BigDecimal natRetrieveAmount = BigDecimal.ZERO;

        //收到保证金工作台字段按
        // receivedAmount 收到金额
        BigDecimal receivedAmount = BigDecimal.ZERO;
        // natReceivedAmount
        BigDecimal natReceivedAmount = BigDecimal.ZERO;
        // returnAmount 退还金额
        BigDecimal returnAmount = BigDecimal.ZERO;
        // natReturnAmount
        BigDecimal natReturnAmount = BigDecimal.ZERO;

        //转换金额  conversionAmount
        BigDecimal converAmount = BigDecimal.ZERO;
        //保证金余额 marginBalance
        BigDecimal marginBalance = BigDecimal.ZERO;
        //保证金可用余额  marginAvailableBalance
        BigDecimal marginAvailableBalance = BigDecimal.ZERO;
        // natConversionAmount
        BigDecimal natConverAmount = BigDecimal.ZERO;
        // natMarginBalance
        BigDecimal natMarginBalance = BigDecimal.ZERO;
        // natMarginAvailableBalance 本币保证金可用余额
        BigDecimal natMarginAvailableBalance = BigDecimal.ZERO;

        //删除支付保证金管理单 || 启用审批流，审批终止
        if (ICmpConstant.DELETE.equals(action) || ICmpConstant.APPROVALSTOP.equals(action)) {
            if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                //保证金可用余额=原保证金可用余额+保证金金额+转换金额
                marginAvailableBalance = marginAvailableBalance.add(marginAmount).add(conversionAmount);
                natMarginAvailableBalance = natMarginAvailableBalance.add(natMarginAmount).add(natConversionAmount);
//                workbench.setMarginAvailableBalance(newBalance);
//                workbench.setNatMarginAvailableBalance(newNatBalance);
            }
        }

        //审核通过
        if (ICmpConstant.AUDIT.equals(action)) {
            // 场景1、不传结算的数据写金额
            if (Boolean.FALSE.equals(settleFlag)
                    // 场景2、传结算已结算补单的数据写金额
                    || (Boolean.TRUE.equals(settleFlag) && FundSettleStatus.SettlementSupplement.getValue() == settleStatus)) {
                if (PAYMARGINPAYMENT.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINPAYMENT) || RECMARGINRECEIVE.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRECEIVE)) {
                    if (MarginFlag.PayMargin.getValue() == flag) {
                        //1.更新支付金额 = 原支付金额+保证金金额
                        payAmount = (payAmount.add(marginAmount));
                        natPayAmount = (natPayAmount.add(natMarginAmount));
                    } else {
                        //1.更新收到金额 = 原收到金额+保证金金额
                        receivedAmount = (receivedAmount.add(marginAmount));
                        natReceivedAmount = (natReceivedAmount.add(natMarginAmount));
                    }
                    //2.保证金余额=原保证金余额+保证金金额
                    marginBalance = (marginBalance.add(marginAmount));
                    natMarginBalance = (natMarginBalance.add(natMarginAmount));
                    //3.保证金可用余额=原保证金可用余额+保证金金额
                    marginAvailableBalance = (marginAvailableBalance.add(marginAmount));
                    natMarginAvailableBalance = (natMarginAvailableBalance.add(natMarginAmount));
                }

                if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                    if (MarginFlag.PayMargin.getValue() == flag) {
                        //1.更新取回金额 = 原取回金额+保证金金额（是否结算为是时，不加保证金金额
                        retrieveAmount = (retrieveAmount.add(marginAmount));
                        natRetrieveAmount = (natRetrieveAmount.add(natMarginAmount));
                    } else {
                        //1.更新退还金额 = 原退还金额+保证金金额（是否结算为是时，不加保证金金额
                        returnAmount = (returnAmount.add(marginAmount));
                        natReturnAmount = (natReturnAmount.add(natMarginAmount));
                    }
                    //2.保证金余额=原保证金余额-保证金金额（是否结算为是时，不减保证金金额）-转换金额
                    marginBalance = (marginBalance.subtract(marginAmount).subtract(conversionAmount));
                    natMarginBalance = (natMarginBalance.subtract(natMarginAmount).subtract(natConversionAmount));
                    //3.更新转换金额 原转换金额+转换金额(审批通过即更新)
                    converAmount = (converAmount.add(conversionAmount));
                    natConverAmount = (natConverAmount.add(natConversionAmount));
                }
            } else if (Boolean.TRUE.equals(settleFlag) && FundSettleStatus.WaitSettle.getValue() == settleStatus) {//待结算的数据审批时只记录转换金额，保证金金额在结算成功时修改
                if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                    //1.保证金余额=原保证金余额-保证金金额（是否结算为是时，不减保证金金额）-转换金额
                    marginBalance = (marginBalance.subtract(conversionAmount));
                    natMarginBalance = (natMarginBalance.subtract(natConversionAmount));
                    //2.更新转换金额 原转换金额+转换金额(审批通过即更新)
                    converAmount = (converAmount.add(conversionAmount));
                    natConverAmount = (natConverAmount.add(natConversionAmount));
                }
            }
        }

        if (ICmpConstant.UN_AUDIT.equals(action)) {
            // 场景1、不传结算的数据回退金额
            if (Boolean.FALSE.equals(settleFlag)
                    //场景2、已结算补单的数据回退金额
                    || (Boolean.TRUE.equals(settleFlag) && FundSettleStatus.SettlementSupplement.getValue() == settleStatus)
                    //场景3、待结算的数据，取消结算后回退金额
                    || (Boolean.TRUE.equals(settleFlag) && FundSettleStatus.WaitSettle.getValue() == settleStatus && Boolean.TRUE.equals(settleStatusChange))
            ) {
                if (PAYMARGINPAYMENT.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINPAYMENT) || RECMARGINRECEIVE.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRECEIVE)) {
                    if (MarginFlag.PayMargin.getValue() == flag) {
                        //1.更新支付金额 = 原支付金额-保证金金额
                        payAmount = (payAmount.subtract(marginAmount));
                        natPayAmount = (natPayAmount.subtract(natMarginAmount));
                    } else {
                        //1.更新收到金额 = 原收到金额-保证金金额
                        receivedAmount = (receivedAmount.subtract(marginAmount));
                        natReceivedAmount = (natReceivedAmount.subtract(natMarginAmount));
                    }
                    //2.保证金余额=原保证金余额-保证金金额
                    marginBalance = (marginBalance.subtract(marginAmount));
                    natMarginBalance = (natMarginBalance.subtract(natMarginAmount));
                    //3.保证金可用余额=原保证金可用余额-保证金金额
                    marginAvailableBalance = (marginAvailableBalance.subtract(marginAmount));
                    natMarginAvailableBalance = (natMarginAvailableBalance.subtract(natMarginAmount));
                }
                if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                    if (MarginFlag.PayMargin.getValue() == flag) {
                        //1.更新取回金额 = 原取回金额-保证金金额（是否结算为是时，不减保证金金额
                        retrieveAmount = (retrieveAmount.subtract(marginAmount));
                        natRetrieveAmount = (natRetrieveAmount.subtract(natMarginAmount));
                    } else {
                        //1.更新退还金额 = 原退还金额-保证金金额（是否结算为是时，不减保证金金额
                        returnAmount = (returnAmount.subtract(marginAmount));
                        natReturnAmount = (natReturnAmount.subtract(natMarginAmount));
                    }
                    //2.保证金余额=原保证金余额+保证金金额（是否结算为是时，不减保证金金额）+转换金额
                    marginBalance = (marginBalance.add(marginAmount).add(conversionAmount));
                    natMarginBalance = (natMarginBalance.add(natMarginAmount).add(natConversionAmount));
                    //3.更新转换金额 原转换金额+转换金额(审批通过即更新)
                    converAmount = (converAmount.subtract(conversionAmount));
                    natConverAmount = (natConverAmount.subtract(natConversionAmount));
                }
            } else if (Boolean.TRUE.equals(settleFlag) && FundSettleStatus.WaitSettle.getValue() == settleStatus && Boolean.FALSE.equals(settleStatusChange)) {
                if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                    //1.保证金余额=原保证金余额+保证金金额（是否结算为是时，不加保证金金额）+转换金额
                    marginBalance = (marginBalance.add(conversionAmount));
                    natMarginBalance = (natMarginBalance.add(natConversionAmount));
                    //2.更新转换金额 原转换金额-转换金额(审批通过即更新)
                    converAmount = (converAmount.subtract(conversionAmount));
                    natConverAmount = (natConverAmount.subtract(natConversionAmount));
                }
            }
        }

        if (ICmpConstant.SETTLESUCCESS.equals(action)) {
            if (Boolean.TRUE.equals(settleFlag)) {
                // 二次办结，结算成功业务台账不再更新保证金相关的金额
                if (checkUpdateAmount(params, flag)) {
                    // 取回保证金
                    if (PAYMARGINPAYMENT.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINPAYMENT) || RECMARGINRECEIVE.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRECEIVE)) {
                        if (MarginFlag.PayMargin.getValue() == flag) {
                            //1.更新支付金额 = 原支付金额+保证金金额
                            payAmount = (payAmount.add(marginAmount));
                            natPayAmount = (natPayAmount.add(natMarginAmount));
                        } else {
                            //1.更新收到金额 = 原收到金额+保证金金额
                            receivedAmount = (receivedAmount.add(marginAmount));
                            natReceivedAmount = (natReceivedAmount.add(natMarginAmount));
                        }
                        //2.保证金余额=原保证金余额+保证金金额
                        marginBalance = (marginBalance.add(marginAmount));
                        natMarginBalance = (natMarginBalance.add(natMarginAmount));
                        //3.保证金可用余额=原保证金可用余额+保证金金额
                        marginAvailableBalance = (marginAvailableBalance.add(marginAmount));
                        natMarginAvailableBalance = (natMarginAvailableBalance.add(natMarginAmount));
                    }
                    if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                        if (MarginFlag.PayMargin.getValue() == flag) {
                            //1.更新取回金额 = 原取回金额+保证金金额（是否结算为是时，不加保证金金额
                            retrieveAmount = (retrieveAmount.add(marginAmount));
                            natRetrieveAmount = (natRetrieveAmount.add(natMarginAmount));
                        } else {
                            //1.更新退还金额 = 原退还金额+保证金金额（是否结算为是时，不加保证金金额
                            returnAmount = (returnAmount.add(marginAmount));
                            natReturnAmount = (natReturnAmount.add(natMarginAmount));
                        }
                        //2.保证金余额=原保证金余额-保证金金额
                        marginBalance = (marginBalance.subtract(marginAmount));
                        natMarginBalance = (natMarginBalance.subtract(natMarginAmount));
                    }
                }
            }
        }

        //止付
        if (ICmpConstant.STOPPAY.equals(action)) {
            if (PAYMARGINWITHDRAW.equals(tradeTypeCode) || extendAttrsJson.contains(PAYMARGINWITHDRAW) || RECMARGINRETURN.equals(tradeTypeCode) || extendAttrsJson.contains(RECMARGINRETURN)) {
                //保证金可用余额=原保证金可用余额+保证金金额
                marginAvailableBalance = (marginAvailableBalance.add(marginAmount));
                natMarginAvailableBalance = (natMarginAvailableBalance.add(natMarginAmount));
            }
        }

        //更新
        Map<String, Object> map = new HashMap<>(8);
        map.put("id", workbench.getId());
        map.put("ytenant_id", InvocationInfoProxy.getTenantid());

        map.put("payAmount", payAmount);
        map.put("natPayAmount", natPayAmount);
        map.put("retrieveAmount", retrieveAmount);
        map.put("natRetrieveAmount", natRetrieveAmount);

        map.put("receivedAmount", receivedAmount);
        map.put("natReceivedAmount", natReceivedAmount);
        map.put("returnAmount", returnAmount);
        map.put("natReturnAmount", natReturnAmount);

        map.put("converAmount", converAmount);
        map.put("natConverAmount", natConverAmount);
        map.put("marginBalance", marginBalance);
        map.put("natMarginBalance", natMarginBalance);
        map.put("marginAvailableBalance", marginAvailableBalance);
        map.put("natMarginAvailableBalance", natMarginAvailableBalance);

        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("from", "marginWorkbenchHandle");
        logparam.put("params", params);
        logparam.put("marginWorkbeanch", workbench);
        logparam.put("flag", flag);
        AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, workbench.getMarginBusinessNo(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */, IServicecodeConstant.PAYMARGIN, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221457B005000004", "保证金可用余额") /* "保证金可用余额" */);

        SqlHelper.update(MARGINWORKBENCHMAPPER, map);
//        workbench.setEntityStatus(EntityStatus.Update);
//        MetaDaoHelper.update(MarginWorkbench.ENTITY_NAME, workbench);
    }

    /**
     * 二次办结，结算成功业务台账不再更新保证金相关的金额
     *
     * @param params 入参
     * @param flag   支付和收到标识
     * @return
     * @throws Exception
     */
    private boolean checkUpdateAmount(CtmJSONObject params, Short flag) throws Exception {
        boolean updateAmountFlag = true;
        // 支付保证金
        if (MarginFlag.PayMargin.getValue() == flag) {
            if (params.get("payMargin") != null) {
                PayMargin payMargin = (PayMargin) params.get("payMargin");
                PayMargin dbpayMargin = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId());
                if (dbpayMargin != null && dbpayMargin.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
                    updateAmountFlag = false;
                }
            }
        } else {
            // 收到保证金
            if (params.get(ICmpConstant.RECMARGIN) != null) {
                ReceiveMargin receiveMargin = (ReceiveMargin) params.get(ICmpConstant.RECMARGIN);
                ReceiveMargin dbreceiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId());
                if (dbreceiveMargin != null && dbreceiveMargin.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
                    updateAmountFlag = false;
                }
            }
        }
        return updateAmountFlag;
    }

    private List<MarginWorkbench> queryWorkbenchByCondition(String businessNo, Short srcItem, Short marginFlag, String accentity, String currency) throws Exception {

        //校验事项来源+保证金原始业务号，不存在对应的支付保证金虚拟户，则根据支付保证金虚拟户编码规则，自动生成新的支付保证金虚拟户。
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginBusinessNo").eq(businessNo));
//        queryConditionGroup.addCondition(QueryCondition.name("srcItem").eq(srcItem));
        queryConditionGroup.addCondition(QueryCondition.name("marginFlag").eq(marginFlag));
        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(accentity));
        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(currency));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema, null);
        return marginWorkbenchList;
    }

    private MarginWorkbench buildPayMarginWorkbench(PayMargin payMargin) {
        MarginWorkbench payWorkbench = new MarginWorkbench();

        payWorkbench.setAccentity(payMargin.getAccentity());
        payWorkbench.setMarginBusinessNo(payMargin.getMarginbusinessno());//保证金原始业务号
        payWorkbench.setSrcItem(payMargin.getSrcitem());//事项来源
        payWorkbench.setBillType(payMargin.getBilltype());//单据类型
        payWorkbench.setFirstPayDate(payMargin.getVouchdate());//首次支付日期
        payWorkbench.setExpectedDate(payMargin.getExpectedretrievaldate());//预计取回日期
        payWorkbench.setMarginType(payMargin.getMargintype());//保证金类型
        payWorkbench.setDescription(payMargin.getDescription());//备注
        payWorkbench.setCurrency(payMargin.getCurrency());//币种
        payWorkbench.setProject(payMargin.getProject());//项目
        payWorkbench.setDept(payMargin.getDept());//部门
        payWorkbench.setEnterpriseBankAccount(payMargin.getEnterprisebankaccount());//本方银行账户
        payWorkbench.setOppositeType(payMargin.getOppositetype());//对方类型
        //其他
        payWorkbench.setOppositeName(payMargin.getOppositename());//对方名称
        payWorkbench.setOppositeBankAccount(payMargin.getOppositebankaccount());//对方银行账号
        payWorkbench.setOppositeBankAccountName(payMargin.getOppositebankaccountname());//对方账户名称
        payWorkbench.setOppositeBankNumber(payMargin.getOppositebankNumber());//对方开户网点
        payWorkbench.setOppositeBankType(payMargin.getOppositebankType());//对方银行类别
        //本单位
        payWorkbench.setOurName(payMargin.getOurname());
        payWorkbench.setOurBankAccount(payMargin.getOurbankaccount());//本单位银行账户
        //客户
        payWorkbench.setCustomer(payMargin.getCustomer());//客户
        payWorkbench.setCustomerBankAccount(payMargin.getCustomerbankaccount());//客户银行账户
        //供应商
        payWorkbench.setSupplier(payMargin.getSupplier());//供应商
        payWorkbench.setSupplierBankAccount(payMargin.getSupplierbankaccount());//供应商银行账户
        //资金业务对象
        payWorkbench.setCapBizObj(payMargin.getCapBizObj());//资金业务对象
        payWorkbench.setCapBizObjBankAccount(payMargin.getCapBizObjbankaccount());//资金业务对象银行账户
        payWorkbench.setMarginFlag(MarginFlag.PayMargin.getValue());
        payWorkbench.setNatCurrency(payMargin.getNatCurrency());
        return payWorkbench;
    }

    private MarginWorkbench buildRecMarginWorkbench(ReceiveMargin receiveMargin) {
        MarginWorkbench recWorkbench = new MarginWorkbench();

        recWorkbench.setAccentity(receiveMargin.getAccentity());
        recWorkbench.setMarginBusinessNo(receiveMargin.getMarginbusinessno());//保证金原始业务号
        recWorkbench.setSrcItem(receiveMargin.getSrcitem());//事项来源
        recWorkbench.setBillType(receiveMargin.getBilltype());//单据类型
        recWorkbench.setFirstReceivedDate(receiveMargin.getVouchdate());//首次收到日期
        recWorkbench.setLatestReturnDate(receiveMargin.getLatestreturndate());//最迟退还日期
        recWorkbench.setMarginType(receiveMargin.getMargintype());//保证金类型
        recWorkbench.setDescription(receiveMargin.getDescription());//备注
        recWorkbench.setCurrency(receiveMargin.getCurrency());//币种
        recWorkbench.setProject(receiveMargin.getProject());//项目
        recWorkbench.setDept(receiveMargin.getDept());//部门
        recWorkbench.setEnterpriseBankAccount(receiveMargin.getEnterprisebankaccount());//本方银行账户
        recWorkbench.setOppositeType(receiveMargin.getOppositetype());//对方类型
        //其他
        recWorkbench.setOppositeName(receiveMargin.getOppositename());//对方名称
        recWorkbench.setOppositeBankAccount(receiveMargin.getOppositebankaccount());//对方银行账号
        recWorkbench.setOppositeBankAccountName(receiveMargin.getOppositebankaccountname());//对方账户名称
        recWorkbench.setOppositeBankNumber(receiveMargin.getOppositebankNumber());//对方开户网点
        recWorkbench.setOppositeBankType(receiveMargin.getOppositebankType());//对方银行类别
        //本单位
        recWorkbench.setOurName(receiveMargin.getOurname());
        recWorkbench.setOurBankAccount(receiveMargin.getOurbankaccount());//本单位银行账户
        //客户
        recWorkbench.setCustomer(receiveMargin.getCustomer());//客户
        recWorkbench.setCustomerBankAccount(receiveMargin.getCustomerbankaccount());//客户银行账户
        //供应商
        recWorkbench.setSupplier(receiveMargin.getSupplier());//供应商
        recWorkbench.setSupplierBankAccount(receiveMargin.getSupplierbankaccount());//供应商银行账户
        //资金业务对象
        recWorkbench.setCapBizObj(receiveMargin.getCapBizObj());//资金业务对象
        recWorkbench.setCapBizObjBankAccount(receiveMargin.getCapBizObjbankaccount());//资金业务对象银行账户
        recWorkbench.setMarginFlag(MarginFlag.RecMargin.getValue());
        recWorkbench.setNatCurrency(receiveMargin.getNatCurrency());
        return recWorkbench;
    }
}
