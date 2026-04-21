package com.yonyoucloud.fi.cmp.bankidentify.rule;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水辨识规则保存前规则
 * 编码是否需要校验唯一
 * 优先级要根据 适用组织，收付方向 判断唯一
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BankIdentifyBeforeSaveRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BankreconciliationIdentifySetting bankreconciliationIdentifySetting = (BankreconciliationIdentifySetting) bills.get(0);
        //
        List<BizObject> bizObjects = this.checkUniqueCode(bankreconciliationIdentifySetting,"code");
        if(CollectionUtils.isNotEmpty(bizObjects)){
            //
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E88E43805200008","编码重复，请修改后保存！"));
        }
        List<String> orgNames = this.checkUniqueExecuteByOrg(bankreconciliationIdentifySetting,"excutelevel");
        if(CollectionUtils.isNotEmpty(orgNames)){
            //
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E88E46C05200006","资金组织【%s】已存在相同优先级的规则，请检查！"), String.join(",", orgNames)));
        }

        return new RuleExecuteResult();
    }

    public List<String> queryOrgName (List<String> accentity) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code,name").appendQueryCondition(QueryCondition.name("id").in(accentity));
        List<BizObject> objects = MetaDaoHelper.queryObject("org.func.FundsOrg", querySchema, "ucf-org-center");
        return objects.stream().map(item -> item.get("name").toString()).collect(Collectors.toList());
    }

    public List<BizObject> checkUniqueCode(BankreconciliationIdentifySetting bankreconciliationIdentifySetting,String field) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code,accentity")
                .appendQueryCondition(
                        QueryCondition.name("identifytype").eq(bankreconciliationIdentifySetting.getIdentifytype()),
                        QueryCondition.name("dc_flag").eq(bankreconciliationIdentifySetting.getDc_flag()),
                        QueryCondition.name(field).eq(bankreconciliationIdentifySetting.get(field))
                );
        if(EntityStatus.Update == bankreconciliationIdentifySetting.getEntityStatus()){
            querySchema.appendQueryCondition(QueryCondition.name("id").not_eq(bankreconciliationIdentifySetting.getId()));
        }
        return MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME,querySchema,null);
    }
    /**
     *  优先级要根据 适用组织，收付方向 判断唯一
     * @param bankreconciliationIdentifySetting
     * @return
     */
    public List<String> checkUniqueExecuteByOrg(BankreconciliationIdentifySetting bankreconciliationIdentifySetting,String field) throws Exception {
        List<String> result = new ArrayList<>();
        List<String> orgIds = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code,accentity")
                .appendQueryCondition(
                        QueryCondition.name("identifytype").eq(bankreconciliationIdentifySetting.getIdentifytype()),
                        QueryCondition.name("dc_flag").eq(bankreconciliationIdentifySetting.getDc_flag()),
                        QueryCondition.name(field).eq(bankreconciliationIdentifySetting.get(field))
                );
        if(EntityStatus.Update == bankreconciliationIdentifySetting.getEntityStatus()){
            querySchema.appendQueryCondition(QueryCondition.name("id").not_eq(bankreconciliationIdentifySetting.getId()));
        }
        List<BankreconciliationIdentifySetting> bizObjects =  MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME,querySchema,null);
        if(CollectionUtils.isNotEmpty(bizObjects)){
            List<String> orgs = Arrays.asList(bizObjects.stream().map(BankreconciliationIdentifySetting::getAccentity).collect(Collectors.joining(",")).split(","));
            String[] accentitys = bankreconciliationIdentifySetting.getAccentity().split(",");
            for(String accentity : accentitys){
                if(orgs.contains(accentity)){
                    orgIds.add(accentity);
                }
            }
        }
        if(CollectionUtils.isNotEmpty(orgIds)){
            result = queryOrgName(orgIds);
        }
        return result;
    }
}
