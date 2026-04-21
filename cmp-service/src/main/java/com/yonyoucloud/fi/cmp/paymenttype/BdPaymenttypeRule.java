package com.yonyoucloud.fi.cmp.paymenttype;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 适配款项类型新属性 -- 应用范围
 * 故事：：： CZFW-48709
 * 款项类型参照 -- 查询前规则
 * @author msc
 */
@Slf4j
@Component
public class BdPaymenttypeRule extends AbstractCommonRule {

    /**
     * 在查询款项类型参照前，添加应用范围查询条件 -- “4”  现金管理
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(map);
        if (bill.getRefCode().equals(IRefCodeConstant.FINBD_BD_PAYMENTTYPEREF)){
            if (bill.getData()!=null) {
                /**
                 * 设置应用范围 -- 现金管理查询条件
                 *  [{value: '1', text: '应付管理', nameType: 'text'},
                 *   {value: '2', text: '应收管理', nameType: 'text'},
                 *   {value: '3', text: '资金结算', nameType: 'text'},
                 *   {value: '4', text: '现金管理', nameType: 'text'},
                 *   {value: '5', text: '商业汇票 ', nameType: 'text'},
                 *   {value: '6', text: '总账', nameType: 'text', disabled: true},]
                 */
                FilterVO conditon = new FilterVO();
                if (bill.getCondition() == null) {
                    conditon.appendCondition(ConditionOperator.and,new SimpleFilterVO("applicationscope", ICmpConstant.QUERY_LIKE, "4"));
                    bill.setCondition(conditon);
                } else {
                    bill.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("applicationscope", ICmpConstant.QUERY_LIKE, "4"));
                }
            }
        }

        return new RuleExecuteResult();
    }
}
