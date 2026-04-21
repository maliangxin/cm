package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;

import java.util.Map;
import java.util.Set;

public class TransferListQueryRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);

        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }

        // 权限控制
        Set<String> orgsSet = BillInfoUtils.getOrgPermissionsByAuth("cm_transfer_account");//billDataDto.getAuthId() 无值故这里写ui模板上配置的值
        if(orgsSet!=null && orgsSet.size()>0) {
            String[] orgs = orgsSet.toArray(new String[orgsSet.size()]);
            filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, IBussinessConstant.ACCENTITY, "in", orgs);
        }

        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
