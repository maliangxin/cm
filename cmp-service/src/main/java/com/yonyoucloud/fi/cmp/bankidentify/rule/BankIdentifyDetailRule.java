package com.yonyoucloud.fi.cmp.bankidentify.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流水自动辨识匹配 详情查询 赋值逻辑
 */
@Component("bankIdentifyDetailRule")
@Slf4j
@RequiredArgsConstructor
public class BankIdentifyDetailRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Object data = paramMap.get("return");
        if(data != null){
            BizObject bizObject = (BizObject) data;
            buildAccentity(bizObject);
        }
        return new RuleExecuteResult();
    }

    /**
     * 多组织赋值
     * @param bizObject
     * @throws Exception
     */
    private void buildAccentity(BizObject bizObject) throws Exception {
        if(bizObject.get("accentity") != null && bizObject.getString("accentity").split(",").length > 1){
            Set<String> orgSet = Arrays.stream(bizObject.getString("accentity").split(",")).collect(Collectors.toSet());
            //MetaDAO查询组织信息 查询资金组织
            QuerySchema querySchema = QuerySchema.create().addSelect("id,code,name").appendQueryCondition(
                    QueryCondition.name("id").in(orgSet)
            );
            List<BizObject> orgList = MetaDaoHelper.queryObject("org.func.FundsOrg", querySchema, "ucf-org-center");
            bizObject.set("accentity",orgSet.toArray());
            bizObject.set("accentity_name", orgList.stream().map(item -> item.getString("name")).toArray());
        }
    }
}
