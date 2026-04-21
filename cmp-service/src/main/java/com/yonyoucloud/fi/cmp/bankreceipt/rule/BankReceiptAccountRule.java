package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class BankReceiptAccountRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = bill.getCondition();
        if(null == filterVO){
             //filterVO = new FilterVO();
        }
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(bill.getrefCode())){
			if (bill.getData()!=null) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) bill.getData();
                String accentity= (String) list.get(0).get(IBussinessConstant.ACCENTITY);
                //20210908 yangjn 添加单组织判断
                if(FIDubboUtils.isSingleOrg()){
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if(singleOrg!=null){
                        accentity = singleOrg.get("id");
                    }
                }
                if (StringUtils.isEmpty(accentity)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101364"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418065E","请先选择会计主体！") /* "请先选择会计主体！" */);
                }
//                UiMetaDaoHelper.appendCondition(filterVO, IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_EQ, accentity);
            }
		}
        //20210520 yangjn 修改电子回单报错 不拼接sql条件
//        bill.setCondition(filterVO);
        putParam(paramMap, bill);
        return new RuleExecuteResult();
    }
}
