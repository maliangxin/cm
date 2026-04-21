package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/4/16 0016.
 */
@Slf4j
@Component
public class InitDataQueryRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        /**
         * 如果页面上 使用组织、所属组织都无值 此时查询当前用户的可用组织拼接条件过滤
         * 如果任意一个组织有值 则不做处理
         */
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();
        if (null != filterVO) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            for (FilterCommonVO vo : commonVOs) {
                if(vo.getValue1() instanceof List){
                    if("accentity".equals(vo.getItemName()) && ((List)vo.getValue1()).size() > 0){
                        return new RuleExecuteResult();
                    }
                    if("parentAccentity".equals(vo.getItemName()) && ((List)vo.getValue1()).size() > 0){
                        return new RuleExecuteResult();
                    }
                }else{
                    if("accentity".equals(vo.getItemName()) && (vo.getValue1()!=null)){
                        return new RuleExecuteResult();
                    }
                    if("parentAccentity".equals(vo.getItemName()) && vo.getValue1()!=null){
                        return new RuleExecuteResult();
                    }
                }

            }
            //获取当前节点可用的组织 拼接条件过滤
            List<String> orgPermissionsList = new ArrayList<>();
            orgPermissionsList.addAll(BillInfoUtils.getOrgPermissionsByAuth("ficmp0008"));
            QueryCondition queryConditionOrgAuth = new QueryCondition("accentity", ConditionOperator.in, orgPermissionsList);

            QueryConditionGroup conditionGroupAuth = new QueryConditionGroup(ConditionOperator.and);
            conditionGroupAuth.addCondition(queryConditionOrgAuth);
            filterVO.setQueryConditionGroup(conditionGroupAuth);

        }
        return new RuleExecuteResult();
    }

}
