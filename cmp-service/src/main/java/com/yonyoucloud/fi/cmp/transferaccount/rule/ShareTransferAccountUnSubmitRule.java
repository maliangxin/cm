package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <h1>资金付款单提交规则</h1>
 *
 */
@Component
public class ShareTransferAccountUnSubmitRule extends AbstractCommonRule {
    @Autowired
    CmCommonService cmCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {

            Map<String, Object> autoConfigMap = cmCommonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;
            if(!Objects.isNull(autoConfigMap)){
               if(null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
                   //调用影像规则
                   BillBiz.executeRule("ShareUnSubmit", billContext, paramMap);
               }
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101042"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001587833") /* "未配置现金参数，请配置" */);
            }

        }
        return result;
    }
}
