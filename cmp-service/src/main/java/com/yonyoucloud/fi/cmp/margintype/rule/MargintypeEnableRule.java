package com.yonyoucloud.fi.cmp.margintype.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margintype.MarginType;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 */
@Component("margintypeEnableRule")
@Slf4j
@RequiredArgsConstructor
public class MargintypeEnableRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        MarginType marginType = MetaDaoHelper.findById(MarginType.ENTITY_NAME ,bizObject.getId());
        if(null == marginType){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100251"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080061", "单据【%s】已删除，请刷新后重试") /* "单据【%s】已删除，请刷新后重试" */, bizObject.get(ICmpConstant.CODE)).toString());
        }
        if(marginType.getIsEnabledType()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100252"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080060", "单据【%s】状态为启用，不可重复启用！") /* "单据【%s】状态为启用，不可重复启用！" */ , marginType.getCode()));
        }
        marginType.setIsEnabledType(true);
        EntityTool.setUpdateStatus(marginType);
        MetaDaoHelper.update(MarginType.ENTITY_NAME,marginType);
        return new RuleExecuteResult();
    }
}
