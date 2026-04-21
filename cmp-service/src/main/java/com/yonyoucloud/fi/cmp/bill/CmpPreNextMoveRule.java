package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.cmp.arap.IBillNum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class CmpPreNextMoveRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        // 执行dao
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = null;
        if (billDataDto.getCondition() == null) {
            filterVO = new FilterVO();
        } else {
            // 若原先有过滤条件，则要在原来的基础上追加条件
            filterVO = billDataDto.getCondition();
        }
        //单据类型判断
        String billNum = billDataDto.getBillnum();
        List<String> orgs = AuthUtil.getAccentitys(billContext, null, billNum, false);
        if (CollectionUtils.isNotEmpty(orgs)) {
            filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "accentity", "in", orgs);
        }
        switch (billNum) {
            case IBillNum.CMP_RECEIVEBILL:
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "initflag", "eq", 0);
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "cmpflag", "eq", 1);
                break;
            case IBillNum.CMP_PAYMENT:
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "cmpflag", "eq", 0);
                filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "billtype", "eq", 1);
                break;
            default:
                break;
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
