package com.yonyoucloud.fi.cmp.margintype.business;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.margintype.MarginType;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 保证金类型保存前规则*
 *
 */

@Slf4j
@Component
public class MarginTypeBeforeDeleteRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bill : bills) {
            //支付保证金台账管理
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("margintype").eq(bill.getId()));
            querySchema.addCondition(group1);
            List<MarginType> payMargins = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
            if (payMargins.size() > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100181"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19489E9604F00001", "保证金类型为：" )+ bill.get("typename") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                                "UID:P_CM-BE_19489F2804F00003", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400732", "已被引用，不允许删除！") /* "已被引用，不允许删除！" */));
            };
            //收到保证金台账管理
            QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("margintype").eq(bill.getId()));
            querySchema2.addCondition(group2);
            List<ReceiveMargin> receivMargins = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema2, null);
            if(receivMargins.size() > 0 ){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100182"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19489E9604F00001", "保证金类型为：" )+ bill.get("typename") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                        "UID:P_CM-BE_19489F2804F00003", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400732", "已被引用，不允许删除！") /* "已被引用，不允许删除！" */));
            };
            //保证金工作台
            QuerySchema querySchema3 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group3 = QueryConditionGroup.and(QueryCondition.name("marginType").eq(bill.getId()));
            querySchema3.addCondition(group3);
            List<MarginWorkbench> marginWorkbenchs = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema3, null);
            if(marginWorkbenchs.size() > 0 ){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100183"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19489E9604F00001", "保证金类型为：" )+ bill.get("typename") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(
                        "UID:P_CM-BE_19489F2804F00003", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400732", "已被引用，不允许删除！") /* "已被引用，不允许删除！" */));
            };
        }
        return new RuleExecuteResult();
    }
}
