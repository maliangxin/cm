package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.payapplybill.CtmPayApplicationBillService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.PurchaseOrderVo;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * <h1>CtmPayApplicationBillServiceImpl</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-10-14 15:00
 */
@Service
@Slf4j
public class CtmPayApplicationBillServiceImpl implements CtmPayApplicationBillService {
    /**
     * 根据采购订单id查询付款申请工作台
     *
     * @param purchaseOrderVo ： 入参
     * @return 结果集
     */
    @Override
    public Object queryPayApplicationBillByPurchaseOrderId(PurchaseOrderVo purchaseOrderVo) throws Exception{
        if (!ValueUtils.isNotEmptyObj(purchaseOrderVo)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101575"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080057", "传入的参数不能为空！") /* "传入的参数不能为空！" */);
        }
        try {
            return queryPayApplicationBillByPurchaseOrderVo(purchaseOrderVo);
        } catch (Exception e) {
            log.error("CtmPayApplicationBillServiceImpl#queryPayApplicationBillByPurchaseOrderId, purchaseOrderVo={}, errorMsg={}", purchaseOrderVo, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101576"),e.getMessage());
        }
    }


    private List<BizObject> queryPayApplicationBillByPurchaseOrderVo(PurchaseOrderVo purchaseOrderVo) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为采购订单的数据
        List<Short> list = Arrays.asList(SourceMatters.PurchaseOrderMaterial.getValue(), SourceMatters.PurchaseOrderPlan.getValue());
        conditionGroup.appendCondition(QueryCondition.name("srcitem").in((Object) list));
        schema.addCondition(conditionGroup);
        // 查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("payApplicationBill_b").addSelect("*");
        // 只查询来源为现金的数据 只有这类数据需要升级
        if (ValueUtils.isNotEmptyObj(purchaseOrderVo.getOrderIds())) {
            conditionGroup.appendCondition(QueryCondition.name("payApplicationBill_b.topsrcbillitemmainid").in(purchaseOrderVo.getOrderIds()));
        }
        if (ValueUtils.isNotEmptyObj(purchaseOrderVo.getOrderItemIds())) {
            conditionGroup.appendCondition(QueryCondition.name("payApplicationBill_b.topsrcbillitemid").in(purchaseOrderVo.getOrderItemIds()));
        }
        schema.addCompositionSchema(detailSchema);
        return MetaDaoHelper.queryObject(PayApplicationBill.ENTITY_NAME, schema, null);
    }
}
