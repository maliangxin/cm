package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>结算方式参照过滤</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-12-08 19:11
 */
@Component
public class SettleModeFilterReferRule extends AbstractCommonRule {
    @Autowired
    private CmCommonService commonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        if ("productcenter.aa_settlemethodref".equals(billDataDto.getrefCode())) {
            FilterVO conditon = new FilterVO();
            if (billDataDto.getTreeCondition() == null) {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0,1}));
                billDataDto.setTreeCondition(conditon);
            } else {
                billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0,1}));
            }
        }
        if (IRefCodeConstant.AA_MERCHANTREF.equals(billDataDto.getrefCode()) || IRefCodeConstant.YSSUPPLIER_AA_VENDOR.equals(billDataDto.getrefCode())) { // 客户
            List<BizObject> bills = getBills(billContext, map);
            commonService.filterMerchantRefAndVendorByOrg(billDataDto, bills);
        }
        return new RuleExecuteResult();
    }
}
