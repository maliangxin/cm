package com.yonyoucloud.fi.cmp.bankidentify.rule;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankidentify.enums.StwbStatementDetailStatusEnum;
import com.yonyoucloud.fi.cmp.bankidentify.enums.StwbStatementStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流水自动辨识匹配 列表查询 accentity 赋值逻辑
 */
@Component("bankIdentifyListRule")
@Slf4j
@RequiredArgsConstructor
public class BankIdentifyListRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Pager data = (Pager) paramMap.get("return");
        List<Map<String, Object>> recordList = data.getRecordList();
        if (CollectionUtils.isEmpty(recordList)) {
            return new RuleExecuteResult();
        }
        // 查询组织信息，过滤掉单组织数据，这部分数据无需翻译
        String accentitys = recordList.stream().filter(item -> item.get("accentity") != null && item.get("accentity").toString().split(",").length > 1)
                .map(item->item.get("accentity").toString()).collect(Collectors.joining(","));
        if(StringUtils.hasText(accentitys)) {
            Set<String> orgSet = Arrays.stream(accentitys.split(",")).collect(Collectors.toSet());
            //MetaDAO查询组织信息 查询资金组织
            QuerySchema querySchema = QuerySchema.create().addSelect("id,name").appendQueryCondition(
                    QueryCondition.name("id").in(orgSet)
            );
            List<BizObject> orgList = MetaDaoHelper.queryObject("org.func.FundsOrg", querySchema, "ucf-org-center");
            Map<String, String> orgMap = orgList.stream().collect(Collectors.toMap(item -> item.getString("id"), item -> item.getString("name")));
            //拼接组织名称
            for (Map<String, Object> record : recordList) {
                if (record.get("accentity") != null && record.get("accentity").toString().split(",").length > 1) {
                    String accentity = record.get("accentity").toString();
                    StringBuilder accentityName = new StringBuilder();
                    for (String org : accentity.split(",")) {
                        accentityName.append(orgMap.get(org)).append(",");
                    }
                    record.put("accentity_name", accentityName.substring(0,accentityName.length()-1));
                }
            }
        }
        //常量枚举
        List<Map<String, Object>> settlementStatusList = recordList.stream().filter(item -> item.containsKey("BankreconciliationIdentifySetting_b_constant") && item.get("BankreconciliationIdentifySetting_b_matchfield") != null)
                .collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(settlementStatusList)) {
            for(Map<String, Object> record : settlementStatusList) {
                String status = record.get("BankreconciliationIdentifySetting_b_constant").toString();
                String field = record.get("BankreconciliationIdentifySetting_b_matchfield").toString();
                //
                if("statementstatus".equals(field)) {
                    StringBuilder statusName = new StringBuilder();
                    for(String item : status.split(",")){
                        StwbStatementStatusEnum statusEnum = StwbStatementStatusEnum.getByCode(item);
                        if(statusEnum != null) {
                            statusName.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(statusEnum.getResId(),statusEnum.getName())).append(",");
                        }
                    }
                    record.put("BankreconciliationIdentifySetting_b_constant", statusName.substring(0,statusName.length()-1));
                }
                //
                if("settleBench_b.statementdetailstatus".equals(field)) {
                    StringBuilder statusName = new StringBuilder();
                    for(String item : status.split(",")){
                        StwbStatementDetailStatusEnum statusEnum = StwbStatementDetailStatusEnum.getByCode(item);
                        if(statusEnum != null) {
                            statusName.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(statusEnum.getResId(),statusEnum.getName())).append(",");
                        }
                    }
                    record.put("BankreconciliationIdentifySetting_b_constant", statusName.substring(0,statusName.length()-1));
                }

            }
        }
        return new RuleExecuteResult();
    }
}
