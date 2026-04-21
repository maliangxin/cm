package com.yonyoucloud.fi.cmp.check.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.orgs.FinOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.BizException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: liaojbo
 * @Date: 2024年09月09日 14:37
 * @Description:根据资金组织填充会计主体
 */

@Slf4j
@Component
public class CheckAccentityRawRule extends AbstractCommonRule {

    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;

    @Autowired
    FinOrgQueryServiceComponent finOrgQueryServiceComponent;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.debug("CheckAccentityRawRule.debug:begin==========");
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        BizObject data = getBills(billContext, paramMap).get(0);
        String accEntity = data.get(IBillConst.ACCENTITY);
        if(StringUtils.isEmpty(accEntity)) {
            data.set(IFieldConstant.ACCENTITYRAW, null);
            data.set(IFieldConstant.ACCENTITYRAW_NAME, null);
            data.set(IFieldConstant.ACCENTITYRAW_CODE, null);
            this.putParam(paramMap, "return", data);
            return ruleResult;
        }
        //BillDataDto billDataDto = (BillDataDto) paramMap.get("param");
        //if(billDataDto != null  && billDataDto.getItem() != null) {
        //    CtmJSONObject valueObject = CtmJSONObject.parseObject(billDataDto.getItem());
        //    if (!(valueObject.containsKey("key")) ||
        //            (!(valueObject.getString("key").equals("vouchdate")) &&
        //                    !(valueObject.getString("key").equals("accentity_name"))) ) {//对于点击单据日期和会计主体名称的check，需要校验模块开启状态
        //        return ruleResult;
        //    }
        //}
        //String enablePeriod;
        //Date periodFirstDate = null;
        try {
            AccentityUtil.fillAccentityRawFiledsToBizObjectbyAccentityId(data, accEntity);
            this.putParam(paramMap, "return", data);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101205"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050012", "通过资金组织给会计主体赋值失败") /* "通过资金组织给会计主体赋值失败" */, e);
        }
        return ruleResult;
    }

}
