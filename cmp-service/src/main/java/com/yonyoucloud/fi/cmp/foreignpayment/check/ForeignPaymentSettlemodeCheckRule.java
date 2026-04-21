package com.yonyoucloud.fi.cmp.foreignpayment.check;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.util.basedoc.SettleMethodQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * <h1>外汇付款 结算方式检查</h1>
 *
 * @author xuxbo
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ForeignPaymentSettlemodeCheckRule extends AbstractCommonRule {

    @Autowired
    private SettleMethodQueryService settleMethodQueryService;

    @Autowired
    BankAccountSettingService bankaccountSettingService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!"settlemode_name".equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        //判断付款方银行账户是否为空 为空则直接返回 不对是否直联字段赋值
        String payenterprisebankaccount = bill.get("payenterprisebankaccount");
        if (!ObjectUtils.isEmpty(payenterprisebankaccount)) {
            //如果不为空 判断当前结算方式是否直联 如果否设置为否
            String settlemodeid = bill.get("settlemode").toString();
            String directConnection = settleMethodQueryService.querySettleMethodWayByCondition(settlemodeid);
            if ("0".equals(directConnection)) {
                bill.set("isdirectlyconnected", 0);
            } else {
                // 如果开通了银企联 则赋值为是
                String paymenterprisebankaccount = bill.get("paymenterprisebankaccount");
                String data = bankaccountSettingService.getOpenFlag(paymenterprisebankaccount);
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
                if (null != jsonObject) {
                    CtmJSONObject jsonData = jsonObject.getJSONObject("data");
                    if (null != jsonData) {
                        if (("true").equals(jsonData.get("openFlag").toString())) {
                            bill.set("isdirectlyconnected", 1);
                        } else {
                            bill.set("isdirectlyconnected", 0);
                        }
                    }
                }
            }
        }

        if (bill.get("iscrossborder").equals((short)1)) {
            bill.set("iscrossborder", 1);
        } else {
            bill.set("iscrossborder", 0);
        }

        if (bill.get("isurgent").equals((short)1)) {
            bill.set("isurgent", 1);
        } else {
            bill.set("isurgent", 0);
        }
        //是否通过代理行 isagencybank
        if (bill.get("isagencybank").equals((short)1)) {
            bill.set("isagencybank", 1);
        } else {
            bill.set("isagencybank", 0);
        }

        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }


}
