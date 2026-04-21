package com.yonyoucloud.fi.cmp.foreignpayment.refer;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h1>外汇付款 结算方式过滤</h1>
 * 只保留银行业务*
 * * *
 * @author xuxbo
 * @date 2023/3/18 14:47
 */
@Component
public class ForeignPaymentSettleModeReferFilter extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ForeignPaymentSettleModeReferFilter() {

        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENT);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
            RefEntity refentity = billDataDto.getRefEntity();
            if (billDataDto.getCondition() == null) {
                FilterVO filterVO = new FilterVO();
                if ("aa_settlemethodref".equals(refentity.code)) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.setTreeCondition(filterVO);
                }
            } else {
                if ("aa_settlemethodref".equals(refentity.code)) {
                    FilterVO filterVO = new FilterVO();
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.setTreeCondition(filterVO);
                }
            }

        }
        return new RuleExecuteResult();
    }
}
