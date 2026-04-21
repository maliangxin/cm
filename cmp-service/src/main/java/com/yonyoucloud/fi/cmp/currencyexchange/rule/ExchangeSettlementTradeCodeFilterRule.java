package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.Bsflag;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本类用于结售汇交易编码过滤
 * 根据交易类型进行过滤*
 * @author xuxbo
 * @date 2023/1/4
 */

@Component
public class ExchangeSettlementTradeCodeFilterRule extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ExchangeSettlementTradeCodeFilterRule() {
        //外币兑换
        BILLNUM_MAP.add(IBillNumConstant.CURRENCYEXCHANGE);
        BILLNUM_MAP.add(IBillNumConstant.CURRENCYAPPLY);
        BILLNUM_MAP.add(IBillNumConstant.INWARD_REMITTANCE_SUBMIT);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        int exchangesettlement_typeFlag = 0;
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            //exchangetype：0 买入； 1 卖出；
            //0 买入的时候，对于银行来说是售汇，1卖出 相对于银行来说是结汇
            if (bizobject.get("flag") != null ){
                if(bizobject.get("flag").equals(Bsflag.Sell.getValue())){
                    exchangesettlement_typeFlag = 1;
                }else {
                    exchangesettlement_typeFlag = 0;
                }
            }
        }

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
            FilterVO filterVO = new FilterVO();
            if ("cmp_exchangesettlement_tradecode_ref".equals(refentity.code)) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("exchangesettlement_typeFlag", "eq", exchangesettlement_typeFlag));

                // 根据多语类型进行过滤
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("multilangtype", "eq", multilangtype));
                billDataDto.setCondition(filterVO);
            }
        }
        return new RuleExecuteResult();
    }
}
