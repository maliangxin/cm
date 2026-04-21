package com.yonyoucloud.fi.cmp.receivemargin.rule.workflow;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.MarginFlag;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ENTITYNAME;

/**
 * 支付保证金弃审前置规则*
 *
 * @author xuxbo
 * @date 2023/8/3 15:24
 */

@Slf4j
@Component
public class ReceiveMarginBeforeUnAuditRule extends AbstractCommonRule {

    @Autowired
    MarginWorkbenchService marginWorkbenchService;

    @Autowired
    MarginCommonService marginCommonService;

    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            YmsLock ymsLock = null;
            try {
                if ((ymsLock=JedisLockUtils.lockRuleWithOutTrace(id,map))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100165"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180798", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                log.info("ReceiveMarginUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ReceiveMargin currentBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizobject.getId());
                log.info("ReceiveMarginUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100166"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180799", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                Date currentPubts = bizobject.getPubts();
                if (currentPubts != null) {
                    if (!currentPubts.equals(currentBill.getPubts())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100167"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418079B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }
                Date date = currentBill.getVouchdate();
                if (null != billContext.getDeleteReason()) {
                    if (ICmpConstant.DELETEALL.equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                        return new RuleExecuteResult();
                    }
                }
                Date currentDate = BillInfoUtils.getBusinessDate();
                if (currentDate.compareTo(date) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100168"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418079A", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
                }

                //交易类型为支付保证金 弃审的时候需要校验 虚拟户的可用余额与当前单据的发生额
                //1.根据交易类型id查询交易类型code
                BdTransType transType = transTypeQueryService.findById(currentBill.getTradetype());
                String receiveMargin_ext = "";
                if (ObjectUtils.isNotEmpty(transType.getExtendAttrsJson())) {
                    CtmJSONObject jsonObject = CtmJSONObject.parseObject(transType.getExtendAttrsJson());
                    if (ObjectUtils.isNotEmpty(jsonObject.get("receiveMargin_ext"))) {
                        receiveMargin_ext = jsonObject.get("receiveMargin_ext").toString();
                    }
                }
                if (transType.getCode().equals("cmp_receivemargin_receive") || receiveMargin_ext.equals("cmp_receivemargin_receive")) {
                    BigDecimal marginamount = currentBill.getMarginamount();
                    MarginWorkbench marginWorkbench = new MarginWorkbench();
                    if (ObjectUtils.isEmpty(currentBill.getMarginvirtualaccount())) {
                        String marginbusinessno = currentBill.getMarginbusinessno();
                        QuerySchema querySchema = QuerySchema.create().addSelect("*");
                        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.MARGINBUSINESSNO).eq(marginbusinessno));
                        queryConditionGroup.addCondition(QueryCondition.name("marginFlag").eq(MarginFlag.RecMargin.getValue()));
                        querySchema.appendQueryCondition(queryConditionGroup);
                        List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema, null);
                        if (ObjectUtils.isNotEmpty(marginWorkbenchList)) {
                            marginWorkbench = marginWorkbenchList.get(0);
                        }
                    } else {
                        String marginvirtualaccount = currentBill.getMarginvirtualaccount().toString();
                        marginWorkbench = MetaDaoHelper.findById(MarginWorkbench.ENTITY_NAME,marginvirtualaccount);
                    }
                    if (ObjectUtils.isEmpty(marginWorkbench)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102125"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CABCBC404380006", "收到保证金撤回，未查询到虚拟户信息，请检查！") /* "收到保证金撤回，未查询到虚拟户信息，请检查！" */);
                    }
                    //针对虚拟户上锁，直到事务结束，避免并发
                    if (!ymsScopeLockManager.tryTxScopeLock(marginWorkbench.getId().toString())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101500"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280012", "已有他人操作同一保证金虚拟账户，请稍候重试！") /* "已有他人操作同一保证金虚拟账户，请稍候重试！" */);
                    }
                    //校验金额
                    BigDecimal marginAvailableBalance = marginWorkbench.getMarginAvailableBalance();
                    BigDecimal difference = marginAvailableBalance.subtract(marginamount);
                    //判断是否推结算，如果推结算，只有结算状态为 结算成功 才进行金额的校验
                    if (ObjectUtils.isNotEmpty(currentBill.getSettleflag()) && currentBill.getSettleflag()==1){
                        //按照明琴要求,结算补单的场景也要校验
                        if (currentBill.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue()) || currentBill.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                            if (difference.compareTo(BigDecimal.ZERO) < 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA34D0604C00009", "该单据保证金发生额大于“保证金可用余额”，请检查！") /* "该单据保证金发生额大于“保证金可用余额”，请检查！" */);
                            }
                        }
                    } else {
                        if (difference.compareTo(BigDecimal.ZERO) < 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA34D0604C00009", "该单据保证金发生额大于“保证金可用余额”，请检查！") /* "该单据保证金发生额大于“保证金可用余额”，请检查！" */);
                        }
                    }

                }

                // 结算状态为已结算补单 并且 推送结算 并且 凭证状态是为生成的  不允许撤回
                short verifystate = Short.parseShort(currentBill.get(ICmpConstant.VERIFY_STATE).toString());
                short voucherstatus = currentBill.getVoucherstatus();
                Short settlestatus = currentBill.getSettlestatus();
                Short settleflag = currentBill.getSettleflag();
                if (ObjectUtils.isNotEmpty(settlestatus)){
                    if (settlestatus.equals(FundSettleStatus.SettlementSupplement.getValue()) && settleflag == 1 && voucherstatus == VoucherStatus.Empty.getValue()
                            && verifystate == VerifyState.COMPLETED.getValue()) {
                        marginCommonService.checkHasSettlementBill(currentBill.getId().toString());
                    }
                }

                CtmJSONObject params = new CtmJSONObject();
                // 弃审需要校验 1.转换标识为是，弃审的时候需要删除对应的子单据（生成的转换金） 2.如果是收到保证金（receivemargin）同时 还需要校验退还标识 如果存在退换单 不允许弃审
                if (currentBill.getConversionmarginflag() == 1) {
                    if (ObjectUtils.isEmpty(currentBill.getConversionmarginid())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100173"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC5ACA05080008", "未查询到生成的转单保证金单据！") /* "未查询到生成的转单保证金单据" */);
                    } else {
                        Long conversionmarginid = Long.valueOf(currentBill.getConversionmarginid());
                        ReceiveMargin conversionReceiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, conversionmarginid);
                        conversionReceiveMargin.put(ENTITYNAME, ReceiveMargin.ENTITY_NAME);
                        // 如果有转换保证金，需要删除对应的转换保证金的凭证
                        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(conversionReceiveMargin);
                        if (!deleteResult.getBoolean("dealSucceed")) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                        }
                        //保证金原始业务号
                        params.put(ICmpConstant.ACTION, ICmpConstant.UN_AUDIT);
                        params.put(ICmpConstant.MARGINBUSINESSNO, conversionReceiveMargin.getMarginbusinessno());
                        params.put(ICmpConstant.MARGINAMOUNT, conversionReceiveMargin.getMarginamount());
                        params.put(ICmpConstant.NATMARGINAMOUNT, conversionReceiveMargin.getNatmarginamount());
                        params.put(ICmpConstant.TRADETYPE, conversionReceiveMargin.getTradetype());
                        params.put(ICmpConstant.SETTLEFLAG, conversionReceiveMargin.getSettleflag());
                        params.put(ICmpConstant.SRC_ITEM, conversionReceiveMargin.getSrcitem());
                        params.put(ICmpConstant.MARGINWORKBENCH_ID, conversionReceiveMargin.getMarginvirtualaccount());
                        params.put(ICmpConstant.RECMARGIN, conversionReceiveMargin);
                        //todo 调用工作台删除接口 ：如果只有此单据占用了此虚拟户 调用删除虚拟户接口 否则 不调用
                        Boolean deleteflag = marginCommonService.useByMulRecMargin(conversionReceiveMargin.getMarginvirtualaccount().toString());
                        //根据id删除
                        MetaDaoHelper.delete(ReceiveMargin.ENTITY_NAME, conversionmarginid);
                        //调用工作台更新接口
                        marginWorkbenchService.recMarginWorkbenchUpdate(params);

                        if (deleteflag) {
                            marginWorkbenchService.delRecMarginWorkbench(params);
                        }

                        //更新母单据的 转换标识 以及记录的转换单id 和 编码
                        currentBill.set(ICmpConstant.CONVERSIONMARGINID, null);
                        currentBill.set(ICmpConstant.CONVERSIONMARGINCODE, null);
                    }
                } else {
                    if (ObjectUtils.isNotEmpty(currentBill.getConversionmarginid())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102126"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC5BC005080005", "转换生成的收到保证金台账，无法弃审！") /* "转换生成的收到保证金台账，无法弃审！" */);
                    }

                }
                //2.如果是收到保证金（receivemargin）同时 还需要校验退还标识 如果存在退换单 不允许弃审
                if (currentBill.getAutorefundflag() == 1 && ObjectUtils.isNotEmpty(currentBill.getRefundmarginid())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102127"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18EC6EA204F80003", "存在自动生成的退还保证金，不允许弃审！") /* "存在自动生成的退还保证金，不允许弃审！" */);
                }



                // 已弃审
                bizobject.putAll(currentBill);
                bizobject.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());

                bizobject.set(ICmpConstant.AUDITORID, null);
                bizobject.set(ICmpConstant.AUDITOR, null);
                bizobject.set(ICmpConstant.AUDIT_DATE, null);
                bizobject.set(ICmpConstant.AUDIT_TIME, null);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102128"),e.getMessage());
            } finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }
        }
        return new RuleExecuteResult();
    }
}
