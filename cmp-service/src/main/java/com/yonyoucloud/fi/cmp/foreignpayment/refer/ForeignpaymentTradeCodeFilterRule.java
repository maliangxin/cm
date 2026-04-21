package com.yonyoucloud.fi.cmp.foreignpayment.refer;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本类用于交易编码过滤
 * 根据交易类型进行过滤*
 * @author xuxbo
 * @date 2023/1/4
 */

@Component
public class ForeignpaymentTradeCodeFilterRule extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ForeignpaymentTradeCodeFilterRule() {
        //外币兑换
        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENT);

    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        int exchangesettlement_typeFlag = 0; //售汇


        String locale = InvocationInfoProxy.getLocale();
        //多语类型
        int multilangtype;
        switch (locale) {
            case "zh_CN":
                multilangtype = 1;
                break;
            case "en_US":
                multilangtype = 2;
                break;
            case "zh_TW":
                multilangtype = 3;
                break;
            default:
                multilangtype = 1;
        }
        if (BILLNUM_MAP.contains(billnum)) {
            RefEntity refentity = billDataDto.getRefEntity();
            if (billDataDto.getCondition() == null) {
                FilterVO filterVO = new FilterVO();
                if ("cmp_exchangesettlement_tradecode_ref".equals(refentity.code)) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("exchangesettlement_typeFlag", "eq", exchangesettlement_typeFlag));

                    // 根据多语类型进行过滤
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("multilangtype", "eq", multilangtype));
                    billDataDto.setCondition(filterVO);
                }

            } else {
                if ("cmp_exchangesettlement_tradecode_ref".equals(refentity.code)) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("exchangesettlement_typeFlag", "eq", exchangesettlement_typeFlag));
                    // 根据多语类型进行过滤
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("multilangtype", "eq", multilangtype));
                }

            }

        }
        return new RuleExecuteResult();
    }
}
