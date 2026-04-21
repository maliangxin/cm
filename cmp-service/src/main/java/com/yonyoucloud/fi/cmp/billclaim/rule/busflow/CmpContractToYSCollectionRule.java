package com.yonyoucloud.fi.cmp.billclaim.rule.busflow;

import com.yonyou.common.bizflow.dto.BizFlowDto;
import com.yonyou.common.bizflow.dto.BizFlowRuleResult;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ypd.bizflow.dto.ConvertResult;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.billclaim.service.BillClaimService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2024/6/28 9:28
 */
@Slf4j
@Component("cmpContractToYSCollectionRule")
public class CmpContractToYSCollectionRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    BillClaimService billClaimService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizFlowRuleResult result = new BizFlowRuleResult();
        BizFlowDto bizFlowDto = (BizFlowDto) paramMap.get("param");
        String bustype = null;
        if (bizFlowDto.getExternalData() != null) {
            Map<String, Object> externalData = (Map<String, Object>) bizFlowDto.getExternalData();
            if (externalData.containsKey("transtypeId") && externalData.get("transtypeId") != null) {
                bustype = (String) externalData.get("transtypeId");
            }
        }
        Map<String, String> param = bizFlowDto.getParameters();
        if (paramMap.get("bizFlowReturn") != null) {
            ConvertResult convertResult = (ConvertResult) paramMap.get("bizFlowReturn");
            billClaimService.handResult(convertResult, paramMap, param, bustype);
            result.setConvertResult(convertResult);
            result.setData(convertResult);
            paramMap.put("bizFlowReturn", convertResult);
        }

        return result;
    }
}
