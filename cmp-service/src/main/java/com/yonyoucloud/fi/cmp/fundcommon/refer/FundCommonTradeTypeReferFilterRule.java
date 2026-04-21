package com.yonyoucloud.fi.cmp.fundcommon.refer;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_COLLECTION;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_PAYMENT;

/**
 * <h1>资金付款单交易类型过滤</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Slf4j
@Component
public class FundCommonTradeTypeReferFilterRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);

        for (BizObject bizobject : bills) {
            try {
                BillDataDto billDataDto = (BillDataDto) getParam(map);
                BillContext bc = new BillContext();
                bc.setFullname("bd.bill.BillTypeVO");
                bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
                QuerySchema schema = QuerySchema.create();
                schema.addSelect("id");
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                // 资金收款单
                if(IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum()) || IBillNumConstant.FUND_COLLECTIONLIST.equals(billContext.getBillnum())){
                    conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_FUND_COLLECTION));
                    // 资金付款单
                }else if(IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum()) || IBillNumConstant.FUND_PAYMENTLIST.equals(billContext.getBillnum())){
                    conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_FUND_PAYMENT));
                }
                schema.addCondition(conditionGroup);
                List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
                String billtypeId = null;
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> objectMap = list.get(0);
                    if (!ValueUtils.isNotEmptyObj(objectMap)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101914"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180394","查询资金收付款单交易类型失败！请检查数据。") /* "查询资金收付款单交易类型失败！请检查数据。" */);
                    }
                    billtypeId = MapUtils.getString(objectMap, "id");
                }
                if ("transtype.bd_billtyperef".equals(billDataDto.getrefCode())) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "cmp_fund_payment_delegation"));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "cmp_fundcollection_delegation"));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billtypeId));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                }
                return new RuleExecuteResult();
            } catch (Exception e) {
                log.error("query transType fail! id={}, yTenantId = {}, e = {}",
                        bizobject.getId(), InvocationInfoProxy.getTenantid(), e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }
}
