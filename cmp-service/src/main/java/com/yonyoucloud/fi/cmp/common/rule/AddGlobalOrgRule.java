package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 企业账户级数据查询添加规则
 */
@Component
public class AddGlobalOrgRule extends AbstractCommonRule {

    //@Autowired
//    OrgRpcService orgRpcService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        if("ucf-org-center.org_pure_tree_ref".equals(billDataDto.getrefCode()) && "aa_vendorref".equals(billContext.getBillnum())){
            List result = (List) map.get("return");
            FinOrgDTO globalOrg = AccentityUtil.getFinOrgDTOByAccentityId("666666");
            Map parseMap =  CtmJSONObject.parseObject(CtmJSONObject.toJSONString(globalOrg),Map.class);
            result.add(parseMap);
            putParam(map, "return", result);
            return new RuleExecuteResult(result);
        }
        return new RuleExecuteResult();
    }
}
