package com.yonyoucloud.fi.cmp.fundcommon.business;


import com.alibaba.fastjson.JSON;
import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BaseDto;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * <h1></h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023/10/13 10:51
 */
@Slf4j
@Component
public class AfterQueryFundBillRule extends AbstractCommonRule {
    private static final List<String> BILL_NUM_LIST = Arrays.asList(
            IBillNumConstant.FUND_PAYMENT,
            IBillNumConstant.FUND_COLLECTION,
            IBillNumConstant.FUND_PAYMENTLIST,
            IBillNumConstant.FUND_COLLECTIONLIST
    );
    @SuppressWarnings("unchecked")
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billNum = billContext.getBillnum();
        if (!BILL_NUM_LIST.contains(billNum)) {
            return new RuleExecuteResult();
        }
        try {
            String action = billContext.getAction();
            if ("query".equals(action)) {
                Pager pager = (Pager) paramMap.get("return");
                Object recordList = pager.getRecordList();
                ArrayList<Map<String, Object>> list = (ArrayList<Map<String, Object>>) recordList;
                list.forEach(item -> {
                    boolean isWfControlled = ValueUtils.isNotEmptyObj(item.get(ICmpConstant.IS_WFCONTROLLED))
                            && BooleanUtils.b(item.get(ICmpConstant.IS_WFCONTROLLED),false);
                    item.put("isWfControlled", isWfControlled);
                });
            }
            if ("detail".equals(action)) {
                HashMap<String, Object> map = (HashMap<String, Object>) paramMap.get("return");
                boolean isWfControlled = ValueUtils.isNotEmptyObj(map.get(ICmpConstant.IS_WFCONTROLLED))
                        && BooleanUtils.b(map.get(ICmpConstant.IS_WFCONTROLLED),false);
                map.put("isWfControlled", isWfControlled);
            }
        } catch (Exception e) {
            log.error("Replace Voucher Id to Voucher No, fail! e = {}", e.getMessage());
            return new RuleExecuteResult();
        }
        return new RuleExecuteResult();
    }

    private boolean isOpenApiFlag(BizObject bill, Map<String, Object> paramMap) throws Exception {
        boolean openApiFlag = bill.containsKey("_fromApi") && bill.get("_fromApi").equals(true);
        BaseDto baseDto = (BaseDto) getParam(paramMap);
        if (baseDto instanceof  BillDataDto) {
            openApiFlag |= ((BillDataDto) baseDto).getFromApi();
            log.error("请求参数, billDaoDto.fromApi:{}", ((BillDataDto) baseDto).getFromApi());
        }
        return openApiFlag;
    }

}
