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
@Component("margintypeUnEnableRule")
@Slf4j
@RequiredArgsConstructor
public class MargintypeUnEnableRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        MarginType marginType = MetaDaoHelper.findById(MarginType.ENTITY_NAME ,bizObject.getId());
        if(null == marginType){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101338"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080029", "单据【%s】已删除，请刷新后重试") /* "单据【%s】已删除，请刷新后重试" */, bizObject.get(ICmpConstant.CODE)).toString());
        }
        if(!marginType.getIsEnabledType()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101339"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080028", "单据【%s】状态为停用，不可重复停用！") /* "单据【%s】状态为停用，不可重复停用！" */ , marginType.getCode()));
        }
        marginType.setIsEnabledType(false);
        EntityTool.setUpdateStatus(marginType);
        MetaDaoHelper.update(MarginType.ENTITY_NAME,marginType);
        return new RuleExecuteResult();
    }
}
