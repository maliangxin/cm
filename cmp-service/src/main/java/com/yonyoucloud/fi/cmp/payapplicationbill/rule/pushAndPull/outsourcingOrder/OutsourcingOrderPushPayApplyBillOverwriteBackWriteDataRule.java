package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.outsourcingOrder;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h1>委外订单推付款申请回写数据加工</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-09-01 13:41
 */
@Slf4j
@Component("outsourcingOrderPushPayApplyBillOverwriteBackWriteDataRule")
public class OutsourcingOrderPushPayApplyBillOverwriteBackWriteDataRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("Overwrite Outsourcing Order Back Write Data Request Params, paramMap = {}"
                , CtmJSONObject.toJSONString(paramMap));
        Object requestData = paramMap.get("requestData");
        if (!ValueUtils.isNotEmptyObj(requestData)) {
            log.error("Overwrite Outsourcing Order Back Write Data fail! requestData is null, paramMap = {}"
                    , CtmJSONObject.toJSONString(paramMap));
            return new RuleExecuteResult(paramMap);
        }
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(requestData.toString());
        Object srcitem = jsonObject.get("srcitem");
        if (!ValueUtils.isNotEmptyObj(srcitem)) {
            log.error("Overwrite Outsourcing Order Back Write Data fail! srcItem is null, paramMap = {}"
                    , CtmJSONObject.toJSONString(paramMap));
            return new RuleExecuteResult(paramMap);
        }
        short sourceMatters = Short.parseShort(srcitem.toString());
        switch (sourceMatters) {
            // 明细推单
            case 9:
                outsourcingOrderLinePushBillDataHandler(paramMap);
                break;
            // 整单推单
            case 8:
                outsourcingOrderAllPushBillDataHandler(paramMap);
                break;
            default:
                break;
        }
        return new RuleExecuteResult(paramMap);
    }

    private void outsourcingOrderLinePushBillDataHandler(Map<String, Object> paramMap) {
        try {
            if (!ValueUtils.isNotEmptyObj(paramMap.get("backSourceMap"))){
                return;
            }
            Map<String, Object> backSourceMap = (Map<String, Object>) paramMap.get("backSourceMap");
            List<BizObject> orderList = (List<BizObject>) backSourceMap.get("osm.OSMOrder.OSMOrder");
            for (BizObject bizObject : orderList) {
                List<BizObject> orderProductList = bizObject.get("orderProduct");
                List<BizObject> orderProductListNew = new ArrayList<>();
                List<BizObject> newBizObject = new ArrayList<>();
                BizObject newBiz1 = new BizObject();
                for (BizObject biz : orderProductList) {
                    BizObject newBiz = new BizObject();
                    newBiz.put("_status", biz.get("_status"));
                    newBiz.put("id", biz.get("id"));
                    newBiz1.put("_status", biz.get("_status"));
                    newBiz1.put("id", biz.get("id"));
                    newBiz.put("__id", biz.get("__id"));
                    newBiz.put("requestedPaymentTC", biz.get("orderSubcontractProduct.requestedPaymentTC"));
                    newBizObject.add(newBiz);
                }
                newBiz1.put("orderSubcontractProduct", newBizObject);
                orderProductListNew.add(newBiz1);
                bizObject.put("orderProduct", orderProductListNew);
            }
            log.error("Overwrite Outsourcing Order Back Write Data All Push, orderList = {}", orderList);
        } catch (Exception e) {
            log.error("Overwrite Outsourcing Order Back Write Data fail! Line data fail ,e = {}", e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101138"),e.getMessage());
        }
    }

    private void outsourcingOrderAllPushBillDataHandler(Map<String, Object> paramMap) {
        try {
            Map<String, Object> backSourceMap = (Map<String, Object>) paramMap.get("backSourceMap");
            List<BizObject> orderList = (List<BizObject>) backSourceMap.get("osm.OSMOrder.OSMOrder");
            List<BizObject> orderProductListNew = new ArrayList<>();
            List<BizObject> newBizObject = new ArrayList<>();
            BizObject newBiz1 = new BizObject();
            for (BizObject biz : orderList) {
                BizObject newBiz = new BizObject();
                newBiz.put("_status", biz.get("_status"));
                newBiz.put("id", biz.get("id"));
                newBiz.put("__id", biz.get("__id"));
                newBiz.put("requestedPaymentTC", biz.get("orderSubcontract.requestedPaymentTC"));
                newBiz1.put("_status", biz.get("_status"));
                newBiz1.put("id", biz.get("id"));
                newBizObject.add(newBiz);
            }
            newBiz1.put("orderSubcontract", newBizObject);
            orderProductListNew.add(newBiz1);
            backSourceMap.put("osm.OSMOrder.OSMOrder", orderProductListNew);
            log.error("Overwrite Outsourcing Order Back Write Data All Push, orderList = {}", orderList);
        } catch (Exception e) {
            log.info("Overwrite Outsourcing Order Back Write Data fail! All data fail ,e = {}", e.getMessage());
        }
    }
}
