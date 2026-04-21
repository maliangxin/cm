package com.yonyoucloud.fi.cmp.initprojectdata;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.INSERT;

public class CheckInitProjectDataRule extends AbstractCommonRule {

    private static final String PROJECT_INIT_DATA = "Project_Init_Data_";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101583"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804FC","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            if (bizobject.getEntityStatus().name().equals(INSERT)) {
                String project = String.valueOf(bizobject.get("project").toString());
                QuerySchema querySchemaSettlement = QuerySchema.create().addSelect("project");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("project").eq(project));
                querySchemaSettlement.addCondition(group);
                Map<String, Object> project_map = MetaDaoHelper.queryOne(InitProjectData.ENTITY_NAME, querySchemaSettlement);
                if (ValueUtils.isNotEmpty(project_map)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101584"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804FD","该项目的期初已创建！") /* "该项目的期初已创建！" */);
                }
                BillContext context = new BillContext();
                context.setFullname("bd.project.ProjectVO");
                context.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
                QuerySchema schema = QuerySchema.create();
                schema.addSelect("id,name,code");
                QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                        QueryCondition.name("id").eq(project));
                schema.addCondition(conditionGroup);
                List<Map<String, Object>> projectlist = MetaDaoHelper.query(context, schema);
                if (ValueUtils.isNotEmpty(projectlist)) {
                    for (Map<String, Object> projects : projectlist) {
                        bizobject.set("projectCode", projects.get("code"));
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
