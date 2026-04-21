package com.yonyoucloud.fi.cmp.checkmanage.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.cmpentity.GenerateType;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置保存前规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:07
 */
@Slf4j
@Component
public class CheckManageBeforeSaveRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    private static String DESTROY = "4";
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        List<CheckManage> bills = getBills(billContext, map);
        // 针对中广核特殊处理，销毁的逻辑不再往下执行
        if(DESTROY.equals(bills.get(0).getHandletype())){
            return new RuleExecuteResult();
        }
        List<CheckManageDetail> listb = bills.get(0).CheckManageDetail();
        List<BizObject> billBizObject = getBills(billContext, map);
        String masterStatus = billBizObject.get(0).getString("_status");
        if ("Insert".equals(masterStatus)) {
            if (listb == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102201"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFA0450007A","支票明细为空，保存失败") /* "支票明细为空，保存失败" */);
            }
        }
        String description = billBizObject.get(0).getString("description");
        if (description.length() > 200) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102202"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450000A","处置说明（汇总）长度不超过200") /* "处置说明（汇总）长度不超过200" */);
        }
        Short generateType = bills.get(0).getGenerateType();
        String creator = bills.get(0).getCreator();
        Date billdate = bills.get(0).getBilldate();
        ArrayList<String> checkIds = new ArrayList<>();
        if (listb != null && listb.size() > 0) {
            List<BizObject> checkManageDetail1 = billBizObject.get(0).getBizObjects("CheckManageDetail", BizObject.class);
            for (int i = 0; i < checkManageDetail1.size(); i++) {
                BizObject bizObject = checkManageDetail1.get(i);
                if (bizObject.getString("handlereason").length() > 200) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102203"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500008","处置说明（明细）长度不超过200") /* "处置说明（明细）长度不超过200" */);
                }
                String checkBillStatus = bizObject.getString("checkBillStatus");
                String checkBillNo = bizObject.getString("checkBillNo");
                String checkid = bizObject.getString("checkid");
                // 校验支票状态不为已入库时，不允许处置，给出提示“支票编号XXX，支票状态为XXX，不支持处置
                if (generateType == GenerateType.ManualInput.getValue() && !"1".equals(checkBillStatus)) {
                    // 只校验手工录入场景下的支票状态
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102204"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18999E1805B80025","支票状态不是已入库，不支持处置，支票编号为：") /* "支票状态不是已入库，不支持处置，支票编号为：" */+checkBillNo);
                }
                String status = bizObject.getString("_status");
                // status为update时，如果子表没有修改点，传递过来的参数中，就没有子表信息，这里需要做出判断
                if ("Delete".equals(status)){
                    CheckManage checkManageQuery = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bills.get(0).getId(), 2);
                    int size = checkManageQuery.CheckManageDetail().size();
                    if (size == checkManageDetail1.size()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102201"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFA0450007A","支票明细为空，保存失败") /* "支票明细为空，保存失败" */);
                    }
                }
                if ("Insert".equals(status)) {
                    if (generateType == GenerateType.ManualInput.getValue()) {
                        // 只校验手工录入场景下实时的支票状态
                        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME,bizObject.getString("checkid"));
                        if (checkOne == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102205"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18999E1805B80026","该支票不存在，请检查！支票编号为：") /* "该支票不存在，请检查！支票编号为：" */+checkBillNo);
                        }
                        if (!"1".equals(checkOne.getCheckBillStatus())) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102206"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18999E1805B80027","支票状态不是已入库,支票状态不是最新状态，请检查！支票编号为：") /* "支票状态不是已入库,支票状态不是最新状态，请检查！支票编号为：" */+checkBillNo);
                        }
                    }
                    checkIds.add(checkBillNo);
                    QuerySchema querySchema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("checkid").eq(checkid));
                    querySchema.addCondition(group);
                    // 查询是为了校验同一支票重复处置的情况；但是流程终止的支票状态会刷为已入库，所以可以再次被处置
                    List<Map<String, Object>> query = MetaDaoHelper.query(CheckManageDetail.ENTITY_NAME, querySchema);
                    if (query != null && query.size() > 0) {
                        for (Map<String, Object> stringObjectMap : query) {
                            Long mainid = (Long) stringObjectMap.get("mainid");
                            CheckManage checkManageQuery = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, mainid, 2);
                            if (checkManageQuery.getAuditstatus() == VerifyState.TERMINATED.getValue()) {
                                // 流程终止的支票可以再次处置
                                continue;
                            } else {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102207"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18999E1805B80028","支票已存在，不允许重复处置！支票编号为：") /* "支票已存在，不允许重复处置！支票编号为：" */+checkBillNo);
                            }
                        }
                    }
                }
            }
            if (checkIds.stream().distinct().count() != checkIds.stream().count()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102208"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC04500009","支票明细中，有重复的支票，请检查！") /* "支票明细中，有重复的支票，请检查！" */);
            }
        }

        // 根据场景更新支票状态
        if (listb != null && listb.size() > 0) {
            List<BizObject> checkManageDetail1 = billBizObject.get(0).getBizObjects("CheckManageDetail", BizObject.class);
            for (int i = 0; i < checkManageDetail1.size(); i++) {
                BizObject bizObject = checkManageDetail1.get(i);
                String status = bizObject.getString("_status");
                CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME,bizObject.getString("checkid"));
                if ("Delete".equals(status)) {
                    // 针对删除的支票，需要恢复支票状态为处置前支票状态，同时清空处置前支票状态信息
                    CheckManageDetail checkManageDetail = MetaDaoHelper.findById(CheckManageDetail.ENTITY_NAME, bizObject.getLong("id"));
                    checkOne.setCheckBillStatus(checkManageDetail.getCheckBillStatus()); // 支票处置子表第一次保存时的支票状态
                    EntityTool.setUpdateStatus(checkOne);
                }
                if ("Insert".equals(status)) {
                    if (generateType == 1) {
                        // 针对手工录入新增的支票，更新支票工作台支票状态为处置中，记录处置前支票状态。
                        // 更新支票状态
                        checkOne.setCheckBillStatus(CmpCheckStatus.Disposal.getValue()); // 处置中
                        EntityTool.setUpdateStatus(checkOne);
                    } else {
                        // 由结算和转账工作台通过生成接口生成的支票处置单，直接审批通过，所以直接把支票状态变更为已作废
                        checkOne.setDisposer(AppContext.getCurrentUser().getName());
                        checkOne.setDisposalDate(BillInfoUtils.getBusinessDate());
                        checkOne.setHandletype(bizObject.getShort("handletypeDetail"));
                        checkOne.setFailReason(bizObject.getString("handlereason"));
                        checkOne.setCheckBillStatus(CmpCheckStatus.Cancle.getValue()); // 已作废
                        EntityTool.setUpdateStatus(checkOne);
                    }
                }
                checkStatusService.recordCheckStatusByManage(checkOne.getId(), checkOne.getCheckBillStatus(), bills.get(0));
                // 更新支票状态
                MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
            }
        }
        return new RuleExecuteResult();
    }
}
