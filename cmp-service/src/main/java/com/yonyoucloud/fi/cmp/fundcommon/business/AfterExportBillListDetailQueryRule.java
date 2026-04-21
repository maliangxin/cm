package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


/**
 * <h1>单据导出时，去重</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-08-03 17:35
 */
@Slf4j
@Component("afterExportBillListDetailQueryRule")
public class AfterExportBillListDetailQueryRule extends AbstractCommonRule {
    private static final List<String> BILL_NUM_LIST = Arrays.asList(
            IBillNumConstant.FUND_PAYMENT,
            IBillNumConstant.FUND_COLLECTION,
            IBillNumConstant.PAYAPPLICATIONBILL,
            IBillNumConstant.SALARYPAY,
            IBillNumConstant.RECEIVE_BILL,
            IBillNumConstant.PAYMENT
    );

    @Override
    @SuppressWarnings("unchecked")
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        try {
            List<Map<String, Object>> resultMap = (List<Map<String, Object>>) map.get("return");
            if (CollectionUtils.isEmpty(resultMap)) {
                return new RuleExecuteResult();
            }
            String billNum = billContext.getBillnum();
            if (BILL_NUM_LIST.contains(billNum)) {
                List<Map<String, Object>> deduplicatedResultMap = new ArrayList<>(new LinkedHashMap<>(resultMap.stream()
                        .collect(Collectors.toMap(m -> (Long) m.get("id"), m -> m, (m1, m2) -> m1))).values());
                map.put("return", deduplicatedResultMap);
            }
        } catch (Exception e) {
            log.error("deduplicate bill list detail error, errorMsg={}", e.getMessage());
        }
        return new RuleExecuteResult();
    }
}
