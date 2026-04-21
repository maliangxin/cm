package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.util.CmpAuthUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 现金企业级会计主体过滤规则
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/9 10:04
 */
@Component
public class CmpGlobalAccentityReferRule extends AbstractCommonRule {

    //资金组织树形远程参照
    private static String accbodyRefCode = IRefCodeConstant.FUNDS_ORGTREE;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        //不包含则直接跳过
        if(!CmpAuthUtils.GLOBAL_ACCENTITY_BILLNUMLIST.contains(billDataDto.getBillnum())){
            return new RuleExecuteResult();
        }
        if (accbodyRefCode.equals(billDataDto.getrefCode())) {
            Map externalMap = new HashMap();
            externalMap.put("ShowGlobal", "true");
            billDataDto.setExternalData(externalMap);
            handleGlobalAccentity(billDataDto);
        }
        return new RuleExecuteResult();
    }

    /**
     * 处理企业账号级
     * @param billDataDto
     */
    private void handleGlobalAccentity(BillDataDto billDataDto) {
        List<SimpleFilterVO> simpleFilterVos = Arrays.stream(billDataDto.getTreeCondition().getSimpleVOs()).filter(t -> t.getField().equals("id")).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(simpleFilterVos)) {
            return;
        }
        SimpleFilterVO idFilterVo = simpleFilterVos.get(0);
        if (!(idFilterVo.getValue1() instanceof HashSet)) {
            return;
        }
        HashSet<String> originFilterVos = (HashSet<String>) idFilterVo.getValue1();
        originFilterVos.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
    }
}
