package com.yonyoucloud.fi.cmp.autoparam.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.BaseOrgDTO;
import com.yonyou.permission.util.AuthSdkFacadeUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.autoparam.common.AutoParamCommonUtil;
import com.yonyoucloud.fi.cmp.autoparam.common.BillActionUtils;
import com.yonyoucloud.fi.cmp.common.service.FundOrgService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 现金参数保存前规则
 */
@Component
public class QueryOrgTreeRule extends AbstractCommonRule {
    private static final String GLOBAL_ACCENTITY = "666666";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        List<BaseOrgDTO> queryAccList = FundOrgService.getAllFundOrgsTree();
        queryAccList = AutoParamCommonUtil.filterOrgPermission(queryAccList, billContext);//筛选有权限的组织
        List<Map<String, Object>> accTree = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queryAccList)) {
            for (BaseOrgDTO accentity : queryAccList) {
                HashMap<String, Object> resp = new HashMap<>();
                resp.put("id", accentity.getId());
                resp.put("parent", accentity.getParentid());
                resp.put("code", accentity.getCode());
                resp.put("level", accentity.getLevel());
                resp.put("name", accentity.getName());
                if (GLOBAL_ACCENTITY.equals(accentity.getId())) {
                    accTree.add(0, resp);
                } else {
                    accTree.add(resp);
                }
            }
        }
        List<Map<String,Object>> needRemoveList = BillActionUtils.buildTree(accTree, true);
        //已经加入到子级的节点要从首级节点移除掉
        accTree.removeAll(needRemoveList);
        ruleResult.setData(accTree);
        paramMap.put("return", accTree);

        return ruleResult;
    }
}
