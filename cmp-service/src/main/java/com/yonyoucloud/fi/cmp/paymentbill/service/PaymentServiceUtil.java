package com.yonyoucloud.fi.cmp.paymentbill.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName PaymentServiceUtil
 * @Description 支付服务实现类
 * @Author lidchn
 * @Date 2021年9月10日10:20:34
 * @Version 1.0
 **/
@Component
public class PaymentServiceUtil {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PaymentServiceUtil.class);

    @Autowired
    private CmCommonService cmCommonService;

    public void auditPayBillPullPayApplyBillUpdatePayBillStatus(List<PayBill> updateBills, String flag) {
        for (PayBill payBill : updateBills) {
            if (ValueUtils.isNotEmptyObj(payBill.getBilltype()) && payBill.getBilltype().getValue() == (short) 59) {
                QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").eq(payBill.getId())));
                try {
                    List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                    if (!ValueUtils.isNotEmptyObj(mapList)) {
                        continue;
                    }
                    Map<String, Object> map = mapList.get(0);
                    try {
                        Set<Object> srcbillitemid = new HashSet<>(2);
                        srcbillitemid.add(map.get("srcbillitemid"));
                        // 更新子表预占金额
                        cmCommonService.updateStatePayApplyBill(flag, srcbillitemid);
                    } catch (Exception exception) {
                        log.error("调整付款申请审批状态失败!:" + exception.getMessage());
                    }
                } catch (Exception e) {
                    log.error("调整付款申请审批状态失败:" + e.getMessage());
                }
            }
        }
    }

}
