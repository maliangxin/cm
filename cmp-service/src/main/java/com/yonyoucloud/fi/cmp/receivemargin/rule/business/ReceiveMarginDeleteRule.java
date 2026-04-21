package com.yonyoucloud.fi.cmp.receivemargin.rule.business;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 支付保证金台账管理 删除规则*
 *
 * @author xuxbo
 * @date 2023/8/4 9:49
 */

@Slf4j
@Component
public class ReceiveMarginDeleteRule extends AbstractCommonRule {

    //是否上游单据推送
    public static final String IS_RPC = "isRPC";

    @Autowired
    MarginWorkbenchService marginWorkbenchService;

    @Autowired
    MarginCommonService marginCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        boolean fromApi = billDataDto == null ? false : billDataDto.getFromApi();// 是否是OpenApi请求
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            YmsLock ymsLock =null;
            try {
                if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(id))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100185"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D5", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                log.info("ReceiveMarginDeleteRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ReceiveMargin currentBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100186"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D1", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                log.info("ReceiveMarginDeleteRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                Date currentPubts = bizobject.getPubts();
                if (currentPubts != null) {
                    if (!currentPubts.equals(currentBill.getPubts())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100187"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D4", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }

                if (!ObjectUtils.isEmpty(currentBill.get(ICmpConstant.VERIFY_STATE))) {
                    short verifystate = Short.parseShort(currentBill.get(ICmpConstant.VERIFY_STATE).toString());
                    if (verifystate == VerifyState.SUBMITED.getValue()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100188"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D2", "单据审批中，无法删除！") /* "单据审批中，无法删除！" */);
                    }
                    if (verifystate == VerifyState.COMPLETED.getValue()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100189"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D3", "单据已审批，无法删除！") /* "单据已审批，无法删除！" */);
                    }
                    //终止的不让删除
//                    if (verifystate == VerifyState.TERMINATED.getValue()) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100190"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_192DE09204E00006", "单据已流程终止，无法删除！") /* "单据已流程终止，无法删除！" */);
//                    }
                }

                //是否期初为否 并且 事项来源不是现金管理的
                if (ObjectUtils.isNotEmpty(currentBill.getSrcbillid()) && ObjectUtils.isEmpty(bizobject.get(IS_RPC))) {
                    //是否期初为否 并且 事项来源不是现金管理的
                    Short initflag = currentBill.getInitflag();
                    Short srcitem = currentBill.getSrcitem();
                    if (initflag == 0 && !srcitem.equals(EventSource.Cmpchase.getValue())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100191"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19362DB005F80000", "是否期初为否并且事项来源不是现金管理的，无法删除！") /* "是否期初为否并且事项来源不是现金管理的，无法删除！" */);
                    }
                }

                // 如果事项来源是"外部系统"同时不是通过OpenApi请求的则不允许删除
                EventSource srcItem = EventSource.find(currentBill.getSrcitem());
                if(srcItem == EventSource.ExternalSystem && !fromApi) {
                    String msg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D95EB5604500002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076B", "{0}是外部系统传入，无法删除！") /* "{0}是外部系统传入，无法删除！" */);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100192"), MessageUtils.getMessage(msg,new String[]{currentBill.getCode()}));
                }


                // 如果本单据是退还单据 （记录了refundmarginid 查询母单据不为空） 删除操作   需要更新母单据的 退换标识 以及记录的id和编码清空
                if (!bizobject.getBoolean(ICmpConstant.AUTOREFUNDFLAG)) {
                    if (ObjectUtils.isNotEmpty(bizobject.get(ICmpConstant.REFUNDMARGINID))) {
                        //根据refundmarginid 查询母单据 进行更新
                        String refundmarginid = bizobject.get(ICmpConstant.REFUNDMARGINID);

                        QuerySchema querySchema = QuerySchema.create().addSelect("*");
                        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
                        queryConditionGroup.addCondition(QueryCondition.name("id").eq(refundmarginid));
                        querySchema.appendQueryCondition(queryConditionGroup);
                        List<ReceiveMargin> receiveMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
                        ReceiveMargin receiveMargin = receiveMarginList.get(0);
                        receiveMargin.setRefundmarginid(null);
                        receiveMargin.setRefundmargincode(null);
                        receiveMargin.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, receiveMargin);
                    }
                }

                // 调用工作台更新接口
                CtmJSONObject params = new CtmJSONObject();
                params.put(ICmpConstant.ACTION, ICmpConstant.DELETE);
                //保证金原始业务号
                params.put(ICmpConstant.MARGINBUSINESSNO, currentBill.getMarginbusinessno());
                params.put(ICmpConstant.MARGINAMOUNT, currentBill.getMarginamount());
                params.put(ICmpConstant.NATMARGINAMOUNT, currentBill.getNatmarginamount());
                params.put(ICmpConstant.TRADETYPE, currentBill.getTradetype());
                if (ObjectUtils.isNotEmpty(currentBill.getConversionamount())) {
                    params.put(ICmpConstant.CONVERSIONAMOUNT, currentBill.getConversionamount());
                    params.put(ICmpConstant.NATCONVERSIONAMOUNT, currentBill.getNatconversionamount());
                }
                params.put(ICmpConstant.SETTLEFLAG, currentBill.getSettleflag());
                params.put(ICmpConstant.SRC_ITEM, currentBill.getSrcitem());
                params.put(ICmpConstant.RECMARGIN, currentBill);
                marginWorkbenchService.recMarginWorkbenchUpdate(params);
                //todo 调用工作台删除接口 ：如果只有此单据占用了此虚拟户 调用删除虚拟户接口 否则 不调用
                Boolean deleteflag = marginCommonService.useByMulRecMargin(currentBill.getMarginvirtualaccount().toString());

                //如果rpc回滚调用的删除规则 需要判断是否需要删除isRPCDELETE字段
                if (ObjectUtils.isNotEmpty(bizobject.get(IS_RPC)) && ObjectUtils.isNotEmpty(bizobject.get("isRPCDELETE"))) {
                    if (bizobject.get("isRPCDELETE")) {
                        deleteRecMarginWorkbench(params, currentBill);
                    }
                } else {
                    if (deleteflag){
                        deleteRecMarginWorkbench(params, currentBill);
                    }
                }

            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101648"),e.getMessage());
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 删除收到保证金工作台
     * @param params
     * @param currentBill
     * @throws Exception
     */
    public void deleteRecMarginWorkbench(CtmJSONObject params, ReceiveMargin currentBill) throws Exception {
        params.put(ICmpConstant.MARGINWORKBENCH_ID, currentBill.getMarginvirtualaccount());
        marginWorkbenchService.delRecMarginWorkbench(params);
    }


}
