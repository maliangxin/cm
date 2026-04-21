package com.yonyoucloud.fi.cmp.margincommon.refer;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.check.AuthCheckCommonUtil;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <h1>支付保证金 和收到保证金 内部单位过滤</h1>
 * 只保留银行转账*
 * * *
 * @author xuxbo
 * @date 2023/3/18 14:47
 */
@Component
public class PayAndReceiveMarginOurNameReferFilter extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public PayAndReceiveMarginOurNameReferFilter() {

        BILLNUM_MAP.add(IBillNumConstant.CMP_PAYMARGIN);
        BILLNUM_MAP.add(IBillNumConstant.CMP_PAYMARGINLIST);
        BILLNUM_MAP.add(IBillNumConstant.CMP_RECEIVEMARGIN);
        BILLNUM_MAP.add(IBillNumConstant.CMP_RECEIVEMARGINLIST);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum) && "ourname_name".equals(billDataDto.getKey())) {

            RefEntity refentity = billDataDto.getRefEntity();
            // 查询有权限的组织
            Set<String> orgsSet = AuthCheckCommonUtil.getAuthOrgs(billContext);
            List<String> expendIds = new ArrayList<>();
            expendIds.addAll(orgsSet);
            if (billDataDto.getTreeCondition() == null) {
                FilterVO filterVO = new FilterVO();
                if (IRefCodeConstant.FUNDS_ORGTREE_NA.equals(refentity.code)) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
                    billDataDto.setTreeCondition(filterVO);
                }
            } else {
                billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, expendIds));
            }
        }
        return new RuleExecuteResult();
    }
}
