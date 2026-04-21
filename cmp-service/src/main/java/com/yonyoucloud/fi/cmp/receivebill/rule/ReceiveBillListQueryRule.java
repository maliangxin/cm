package com.yonyoucloud.fi.cmp.receivebill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @ClassName ReceiveBillListQueryRule
 * @Description 收款工作台列表查询规则
 * @Author tongyd
 * @Date 2019/6/21 10:51
 * @Version 1.0
 **/
@Slf4j
@Component
public class ReceiveBillListQueryRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);

        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }

        filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "initflag", "eq", 0);
        filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, "cmpflag", "eq", 1);
        
        // 权限控制
//        Set<String> orgsSet = BillInfoUtils.getOrgPermissionsByAuth("cmp_receivebilllistlist");//billDataDto.getAuthId() 无值故这里写ui模板上配置的值
//        if(orgsSet!=null && orgsSet.size()>0) {
//        	String[] orgs = orgsSet.toArray(new String[orgsSet.size()]);
//            filterVO = (FilterVO) UiMetaDaoHelper.appendCondition(filterVO, IBussinessConstant.ACCENTITY, "in", orgs);
//        }

        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
