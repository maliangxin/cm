package com.yonyoucloud.fi.cmp.bankcapitalvirtual;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.IsEnable;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.virtualFlowRuleConfig.VirtualFlowRuleConfig;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: 资金池虚拟流水接口具体实现
 * @author: zhaoruui@yonyou.com
 * @date: 2023/2/28 15:39
 */

@Slf4j
@Service
public class VirtualFlowRuleConfigServiceImpl implements VirtualFlowRuleConfigService {

    @Resource
    private YmsOidGenerator ymsOidGenerator;

    @Override
    public CtmJSONObject updateConfigInfo(CtmJSONObject params) {
        CtmJSONObject result = new CtmJSONObject();
        List<VirtualFlowRuleConfig> newConfigs = new ArrayList<>();
        try {
            if (StringUtils.isEmpty(params.getString("banktypecode"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100201"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DC", "银行类别不能为空") /* "银行类别不能为空" *//* "银行类别不能为空 */);
            }
            // 获取前端传过来的数据
            String banktypecode = params.getString("banktypecode");
            // 设置新的数据
            VirtualFlowRuleConfig virtualFlowRuleConfig = new VirtualFlowRuleConfig();
            virtualFlowRuleConfig.setId(ymsOidGenerator.nextId());
            virtualFlowRuleConfig.setYtenant(AppContext.getYTenantId());
            virtualFlowRuleConfig.setTenant(AppContext.getTenantId());
            virtualFlowRuleConfig.setBanktypecode(banktypecode);
            virtualFlowRuleConfig.setBanktypename(params.getString("banktypename"));
            virtualFlowRuleConfig.setBanktype(params.getString("banktype"));
            virtualFlowRuleConfig.setIsEnable(IsEnable.ENABLE.getValue());
            virtualFlowRuleConfig.setEntityStatus(EntityStatus.Insert);
            newConfigs.add(virtualFlowRuleConfig);
            //  重复校验 根据banktypecode
            checkIsunique(banktypecode);
            CmpMetaDaoHelper.insert(VirtualFlowRuleConfig.ENTITY_NAME, newConfigs);
            result.put("code", 200);
            result.put("message", "success");
        } catch (Exception e) {
            log.error("updateConfigInfo error:{}", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100202"),e.getMessage());
        }
        return result;
    }

    private void checkIsunique(String banktypecode) throws Exception {
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("count(1) as  count");
        querySchema.appendQueryCondition(QueryCondition.name("banktypecode").eq(banktypecode));
        Map<String, Object> map = MetaDaoHelper.queryOne(VirtualFlowRuleConfig.ENTITY_NAME, querySchema);
        Long count = (Long) map.get("count");
        if (count > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100203"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_1681445843522551816") /* "数据重复!" */);
        }
    }

    /**
     * 设置为启用
     * 判断是否存在必输的参数没有输入
     * 如有 - 不允许启用
     *
     * @param paramMap
     * @return
     */
    public String enable(Map<String, Object> paramMap) throws Exception {
        if (paramMap.get("id") == null)
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100204"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EC","需启用数据为空") /* "需启用数据为空" */);
        Long id = Long.valueOf(paramMap.get("id").toString());
        //获取需要修改的实体
        List<VirtualFlowRuleConfig> virtualflowrules;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(group);
        virtualflowrules = MetaDaoHelper.queryObject(VirtualFlowRuleConfig.ENTITY_NAME, querySchema, null);
        if (virtualflowrules.isEmpty())
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100204"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EC","需启用数据为空") /* "需启用数据为空" */);
        VirtualFlowRuleConfig virtualflowrule = virtualflowrules.get(0);
        //校验是否需要修改
        if (virtualflowrule.getIsEnable().equals(IsEnable.ENABLE.getValue()))
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100205"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EE","该设置为已启用状态") /* "该设置为已启用状态" */);
        //修改数据
        virtualflowrule.setIsEnable(IsEnable.ENABLE.getValue());
        virtualflowrule.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(VirtualFlowRuleConfig.ENTITY_NAME, virtualflowrule);
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F0","启用成功") /* "启用成功" */;
    }

    /**
     * 数据修改为禁用状态
     *
     * @param paramMap
     * @return
     * @throws Exception
     */
    public String disenable(Map<String, Object> paramMap) throws Exception {
        if (paramMap.get("id") == null)
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100204"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EC","需启用数据为空") /* "需启用数据为空" */);
        Long id = Long.valueOf(paramMap.get("id").toString());
        //获取需要修改的实体
        List<VirtualFlowRuleConfig> virtualflowrules;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(group);
        virtualflowrules = MetaDaoHelper.queryObject(VirtualFlowRuleConfig.ENTITY_NAME, querySchema, null);
        if (virtualflowrules.isEmpty())
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100204"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EC","需启用数据为空") /* "需启用数据为空" */);
        VirtualFlowRuleConfig virtualflowrule = virtualflowrules.get(0);
        //校验是否需要修改
        if (virtualflowrule.getIsEnable().equals(IsEnable.DISENABLE.getValue()))
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100206"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806ED","该设置为已禁用状态") /* "该设置为已禁用状态" */);
        //修改数据
        virtualflowrule.setIsEnable(IsEnable.DISENABLE.getValue());
        virtualflowrule.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(VirtualFlowRuleConfig.ENTITY_NAME, virtualflowrule);
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EF","禁用成功") /* "禁用成功" */;
    }
}

