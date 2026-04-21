package com.yonyoucloud.fi.cmp.fcdsusesetting.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author guoxh
 */
@Slf4j
@Component("fcdsUseSettingBeforeSaveRule")
public class FcdsUseSettingBeforeSaveRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isEmpty(bills)) {
            new RuleExecuteResult();
        }
        BizObject object = bills.get(0);
        if(Objects.isNull(object.get("code"))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102012"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD0DA05180008","流程编码不能为空"));
        }
        if(Objects.isNull(object.get("name"))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102013"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD10E05180009","流程名称不能为空"));
        }
        if(object.getEntityStatus().equals(EntityStatus.Insert)){
            boolean flag = this.checkValueExist("code",object.get("code"),null);
            if(flag){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102014"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD34A04D80003","流程编码[%s]已存在"),object.getString("code")));
            }
            flag = this.checkValueExist("name",object.get("name"),null);
            if(flag){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102015"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD37404D80003","流程名称[%s]已存在"),object.getString("name")));
            }
        }else if(object.getEntityStatus().equals(EntityStatus.Update)){
            boolean flag = this.checkValueExist("code",object.get("code"),object.getId());
            if(flag){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102014"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD34A04D80003","流程编码[%s]已存在"),object.getString("code")));
            }
            flag = this.checkValueExist("name",object.get("name"),object.getId());
            if(flag){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102015"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD37404D80003","流程名称[%s]已存在"),object.getString("name")));
            }
        }
        //默认组织设置企业账号级
        object.set("accentity","666666");
        return new RuleExecuteResult();
    }

    private boolean checkValueExist(String field,String value,Long excludeId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id")
                .appendQueryCondition(
                        QueryCondition.name(field).eq(value)
                );
        if(excludeId !=null){
            querySchema.appendQueryCondition(
                    QueryCondition.name("id").not_eq(excludeId)
            );
        }
        List<BizObject> list = MetaDaoHelper.query(FcDsUseSetting.ENTITY_NAME,querySchema);
        return CollectionUtils.isNotEmpty(list);
    }
}
