package com.yonyoucloud.fi.cmp.foreignpayment.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内部单位账户过滤规则*
 *
 * @author xuxbo
 * @date 2023/8/15 16:03
 */


@Slf4j
@Component
public class ForeignPaymentOurAccountReferFilterRule extends AbstractCommonRule {
    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ForeignPaymentOurAccountReferFilterRule() {
        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENT);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
//            RefEntity refentity = billDataDto.getRefEntity();
            if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && ICmpConstant.OURBANKACCOUNT_ACCOUNT.equals(billDataDto.getKey())) {
                List<BizObject> bizObjectList = (ArrayList) billDataDto.getData();
                if (bizObjectList.size() < 1 || bizObjectList == null) {
                    return new RuleExecuteResult();
                }
                //获取会计主体的id
                String accentity = bizObjectList.get(0).get("ourname");

                //获取币种id
                String currency = bizObjectList.get(0).get("currency");


                if (billDataDto.getCondition() == null) {
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
//                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "eq", currency));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", 0));
                    billDataDto.setCondition(conditon);
                } else {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
//                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "eq", currency));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", 0));
                }
            }
        }
        return new RuleExecuteResult();
    }
}
