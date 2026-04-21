package com.yonyoucloud.fi.cmp.bankreconciliation.rule.refund;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.crud.ListDetailPagerBillRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 退票确认、关联确认、生单确认列表查询组织过滤
 */
@Component
public class BankreconciliationConfirmListBeforeQueryRule extends AbstractCommonRule {

    /**
     * 需要跳出权限的billnum
     */
    static List<String> billnumList = new ArrayList<>();

    static {
        billnumList.add(IBillNumConstant.CMP_BANKRECONCILIATION_CHECKREFUND);
        billnumList.add(IBillNumConstant.CMP_BANKRECONCILIATION_ISSURE_LIST);
        billnumList.add(IBillNumConstant.CMP_AUTO_PUSH_BILL_CONFIRM_LIST);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        if (!billnumList.contains(billnum)) {
            return new RuleExecuteResult();
        }

        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = new FilterVO();
        boolean filterorg = false;
        String orgidStr = IFieldConstant.ORGID;
        String accentityStr = IFieldConstant.ACCENTITY;
        if (billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
            filterorg = Arrays.stream(filterVO.getCommonVOs()).anyMatch(commonVO -> {
                return (commonVO.getItemName().equals(orgidStr) || commonVO.getItemName().equals(accentityStr));
            });
        }

        Set<String> orgs = BillInfoUtils.getOrgPermissions(billnum);
        //如果没选组织，就用用户有权限的组织，查询[账户使用组织=用户组织]、[流水使用组织为空且账户所属组织=用户组织]的数据的并集
        if (CollectionUtils.isNotEmpty(orgs) && !filterorg) {
            Map<String, String> map = billDataDto.getParameters();

            //[账户使用组织=用户组织]
            QueryCondition accentityAuth = new QueryCondition(accentityStr, ConditionOperator.in, orgs);

            //[流水使用组织为空且账户所属组织=用户组织]
            QueryConditionGroup orgidAuth = new QueryConditionGroup(ConditionOperator.and);
            orgidAuth.addCondition(QueryCondition.name(accentityStr).is_null());
            orgidAuth.addCondition(QueryCondition.name(orgidStr).in(orgs));

            //并集
            QueryConditionGroup conditionGroupAuth = new QueryConditionGroup(ConditionOperator.or);
            conditionGroupAuth.addCondition(accentityAuth);
            conditionGroupAuth.addCondition(orgidAuth);

            filterVO.setQueryConditionGroup(conditionGroupAuth);
        }
        billDataDto.setCondition(filterVO);
        billDataDto.setIsDistinct(true);
        putParam(paramMap, billDataDto);
        String action = billContext.getAction();
        if ("listdetailpager".equals(action) || (billnum.equals("cmp_BankReconciliation_isSure_list") && "query".equals(action))) {
            QuerySchema mainSchema = QuerySchema.create().addSelect("id");
            QueryConditionGroup mainConditionGroup = filterVO.getQueryConditionGroup();
            mainSchema.addCondition(mainConditionGroup);
            List<Map<String, Object>> bankReconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, mainSchema);
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return new RuleExecuteResult();
            }
            List<Long> bankReconciliationIdList = bankReconciliationList.stream().map(item -> (Long) item.get("id")).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(bankReconciliationIdList)){
                return new RuleExecuteResult();
            }

            List<Long> relationStatus = new ArrayList<>();
            relationStatus.add(1L);
            relationStatus.add(2L);

            QuerySchema subSchema = QuerySchema.create().addSelect("bankreconciliation");
            QueryConditionGroup subConditionGroup = new QueryConditionGroup();
            if(CollectionUtils.isNotEmpty(bankReconciliationIdList)){
                subConditionGroup.appendCondition(QueryCondition.name("bankreconciliation").in(bankReconciliationIdList));
            }
            if((billnum.equals("cmp_BankReconciliation_isSure_list") && "query".equals(action))){
                subConditionGroup.appendCondition(QueryCondition.name("relationstatus").in(relationStatus));
            }
            subSchema.addCondition(subConditionGroup);
            List<Map<String, Object>> bankReconciliationBList = MetaDaoHelper.query(BankReconciliationbusrelation_b.ENTITY_NAME, subSchema);
            List<Long> resultBankReconciliationIdList = bankReconciliationBList.stream().map(item -> (Long) item.get("bankreconciliation")).distinct().collect(Collectors.toList());

            QueryConditionGroup idCondition = new QueryConditionGroup(ConditionOperator.and);
            idCondition.addCondition(filterVO.getQueryConditionGroup());
            if(CollectionUtils.isNotEmpty(resultBankReconciliationIdList)){
                idCondition.addCondition(QueryCondition.name("id").in(resultBankReconciliationIdList));
            }
            if("listdetailpager".equals(action)){
                idCondition.addCondition(QueryCondition.name("relationstatus").in(relationStatus));
            }
            filterVO.setQueryConditionGroup(idCondition);
        }
        return new RuleExecuteResult();
    }
}
