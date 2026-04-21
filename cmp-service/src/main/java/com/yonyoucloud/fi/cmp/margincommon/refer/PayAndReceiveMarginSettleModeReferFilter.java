package com.yonyoucloud.fi.cmp.margincommon.refer;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
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
 * <h1>支付保证金 和收到保证金 结算方式过滤</h1>
 * 只保留银行转账*
 * * *
 * @author xuxbo
 * @date 2023/3/18 14:47
 */
@Component
public class PayAndReceiveMarginSettleModeReferFilter extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public PayAndReceiveMarginSettleModeReferFilter() {

        BILLNUM_MAP.add(IBillNumConstant.CMP_PAYMARGIN);
        BILLNUM_MAP.add(IBillNumConstant.CMP_PAYMARGINLIST);
        BILLNUM_MAP.add(IBillNumConstant.CMP_RECEIVEMARGIN);
        BILLNUM_MAP.add(IBillNumConstant.CMP_RECEIVEMARGINLIST);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
            RefEntity refentity = billDataDto.getRefEntity();
            FilterVO filterVO = billDataDto.getTreeCondition();
            if ("aa_settlemethodref".equals(refentity.code)) {
                if(filterVO == null){
                    filterVO = new FilterVO();
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                billDataDto.setTreeCondition(filterVO);
            }
        }
        return new RuleExecuteResult();
    }
}
