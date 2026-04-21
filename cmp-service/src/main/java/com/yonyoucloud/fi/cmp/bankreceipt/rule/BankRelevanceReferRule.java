package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class BankRelevanceReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(map);
        FilterVO filterVO = new FilterVO();
        if(null == filterVO){
            filterVO = new FilterVO();
        }
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode())){
            if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                String accentity= (String) list.get(0).get(IBussinessConstant.ACCENTITY);
                if(FIDubboUtils.isSingleOrg()){
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if(singleOrg != null){
                        accentity = singleOrg.get("id");
                    }
                }
                if (StringUtils.isEmpty(accentity)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100200"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D0","请先选择会计主体！") /* "请先选择会计主体！" */);
                }
                UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.ORG_ID, ICmpConstant.QUERY_EQ, accentity);
            }
        }
        return new RuleExecuteResult();
    }
}
