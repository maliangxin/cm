package com.yonyoucloud.fi.cmp.exchangegainloss.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 新增单据规则
 */

public class AddExchangeBillRule extends AbstractCommonRule {

    @Autowired
    private CmCommonService cmCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100549"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180154","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            Map<String, Object> condition = new HashMap<>();
            condition.put("billtype_id","FICA5");
            List<Map<String, Object>> transTypList = cmCommonService.getTransTypeByCondition(condition);
            List<Map<String,Object>> transTypes= transTypList.stream().filter(e-> ICmpConstant.HDSY.equals(e.get(ICmpConstant.CODE))).collect(Collectors.toList());
            if (!transTypes.isEmpty()){
                bizobject.set("tradetype",transTypes.get(0).get("id"));
                bizobject.set("tradetype_code",transTypes.get(0).get("code"));
                bizobject.set("tradetype_name",transTypes.get(0).get("name"));
            }
            JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
            String json = formatter.toJson(bizobject, billContext.getFullname(), true).toString();
            //putParam(paramMap, json);
            return new RuleExecuteResult(json);
        }

        return new RuleExecuteResult();

        //return new RuleExecuteResult();
    }

}
