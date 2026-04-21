package com.yonyoucloud.fi.cmp.payapplicationbill.rule.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>付款申请单结算方式过滤</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-14 9:59
 */
@Component("payApplicationBillSettleModeReferFilterRule")
public class PayApplicationBillSettleModeReferFilterRule extends AbstractCommonRule {

    @Autowired
    private CmCommonService commonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        FilterVO condition = ValueUtils.isNotEmptyObj(billDataDto.getTreeCondition()) ? billDataDto.getTreeCondition() : new FilterVO();
        if ("productcenter.aa_settlemethodref".equals(billDataDto.getrefCode())) {
            List<BizObject> bills = getBills(billContext, map);
            if (bills.size() <= 0) {
                return new RuleExecuteResult();
            }
            BizObject bizObject = bills.get(0);
            if (ValueUtils.isNotEmptyObj(bizObject.get("srcitem"))) {
                short srcitem = Short.parseShort(bizObject.get("srcitem").toString());
                if (srcitem == SourceMatters.ManualInput.getValue()) {
                    condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0, 1}));
                }
            }
        }
        if (IRefCodeConstant.AA_MERCHANTREF.equals(billDataDto.getrefCode()) || IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(billDataDto.getrefCode())) { // 客户
            List<BizObject> bills = getBills(billContext, map);
            commonService.filterMerchantRefAndVendorByOrg(billDataDto, bills);
        }
        billDataDto.setTreeCondition(condition);
        return new RuleExecuteResult();
    }


}
