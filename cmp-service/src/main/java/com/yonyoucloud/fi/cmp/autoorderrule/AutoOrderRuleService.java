package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.IsEnable;
import com.yonyoucloud.fi.cmp.cmpentity.SystemNameType;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 自动生单规则Service
 * @author msc
 */
@Service
public class AutoOrderRuleService {

    /**
     * 将自动生单规则设置为启用
     * 判断是否存在必输的参数没有输入
     * 如有 - 不允许启用
     * @param paramMap
     * @return
     */
    public String enable(Map<String,Object> paramMap) throws Exception {
        if(paramMap.get("id") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102016"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010A", "需启用数据为空") /* "需启用数据为空" */);
        }
        Long id = Long.valueOf(paramMap.get("id").toString());
        //获取需要修改的自动生单规则实体
        List<Autoorderrule> autoorderrules;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(group);
        autoorderrules =  MetaDaoHelper.queryObject(Autoorderrule.ENTITY_NAME,querySchema,null);
        if(autoorderrules.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102016"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010A", "需启用数据为空") /* "需启用数据为空" */);
        }
        Autoorderrule autoorderrule = autoorderrules.get(0);
        //校验是否需要修改
        if(autoorderrule.getIsEnable().equals(IsEnable.ENABLE.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102017"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010E", "该设置为已启用状态") /* "该设置为已启用状态" */);
        }
        //校验子表数据
        Autoorderrule_b autoorderrule_b = getautoorderrule_b(autoorderrule.getDetailid());
        if(autoorderrule.getApplication().equals(SystemNameType.CashManagement.getValue())){
            if(autoorderrule_b.getQuickType() == null || autoorderrule_b.getTradetype() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102018"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00109", "参数设置有必输项为空") /* "参数设置有必输项为空" */);
            }
        }
        //修改数据
        autoorderrule.setIsEnable(IsEnable.ENABLE.getValue());
        autoorderrule.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(Autoorderrule.ENTITY_NAME,autoorderrule);
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010C", "启用成功") /* "启用成功" */;
    }

    /**
     * 数据修改为禁用状态
     * @param paramMap
     * @return
     * @throws Exception
     */
    public String disenable(Map<String,Object> paramMap) throws Exception {
        if(paramMap.get("id") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102016"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010A", "需启用数据为空") /* "需启用数据为空" */);
        }
        Long id = Long.valueOf(paramMap.get("id").toString());
        //获取需要修改的自动生单规则实体
        List<Autoorderrule> autoorderrules;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(group);
        autoorderrules =  MetaDaoHelper.queryObject(Autoorderrule.ENTITY_NAME,querySchema,null);
        if(autoorderrules.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102016"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010A", "需启用数据为空") /* "需启用数据为空" */);
        }
        Autoorderrule autoorderrule = autoorderrules.get(0);
        //校验是否需要修改
        if(autoorderrule.getIsEnable().equals(IsEnable.DISENABLE.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102019"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010D", "该设置为已禁用状态") /* "该设置为已禁用状态" */);
        }
        //修改数据
        autoorderrule.setIsEnable(IsEnable.DISENABLE.getValue());
        autoorderrule.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(Autoorderrule.ENTITY_NAME,autoorderrule);
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010B", "禁用成功") /* "禁用成功" */;
    }

    public Autoorderrule_b getautoorderrule_b(Long id) throws Exception {
        List<Autoorderrule_b> autoorderrule_bs;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(group);
        autoorderrule_bs =  MetaDaoHelper.queryObject(Autoorderrule_b.ENTITY_NAME,querySchema,null);
        return autoorderrule_bs.get(0);
    }
}
