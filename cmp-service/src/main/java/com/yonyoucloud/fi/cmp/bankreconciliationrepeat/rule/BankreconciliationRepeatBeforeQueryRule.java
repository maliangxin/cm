package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationrepeat.dao.BankReconciliationRepeatDAO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.util.CtmDealDetailCheckMayRepeatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 疑重列表数据排序,根据疑重标识字段进行排序
 *
 * @author guoxh
 */
@Component("bankreconciliationRepeatBeforeQueryRule")
@Slf4j
@RequiredArgsConstructor
public class BankreconciliationRepeatBeforeQueryRule extends AbstractCommonRule {
    @Value("${cmp.bankDetail.repeatOrderBy:1}")
    private Integer repeatOrderBy;
    @Autowired
    private BankReconciliationRepeatDAO bankReconciliationRepeatDAO;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        String action = billDataDto.getAction();
        if ("output".equals(action)) {
            return new RuleExecuteResult();
        }
        //疑重处理只显示和疑似重复/确认重复/确认正常 相同疑重要素的正常数据
        QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
        repeatGroup.addCondition(QueryCondition.name("isrepeat").in(BankDealDetailConst.REPEAT_DOUBT,BankDealDetailConst.REPEAT_CONFIRM,BankDealDetailConst.REPEAT_NORMAL));
        QuerySchema querySchema = QuerySchema.create().addSelect("id").distinct().addCondition(repeatGroup);
        List<BankReconciliation> repeatList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
        if(CollectionUtils.isEmpty(repeatList)) {
            //如果没有疑重数据,则拼接一个1=2的条件不返回数据
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", 2));
        }else{
            List<BankReconciliation> list = bankReconciliationRepeatDAO.selectRepeatDataWithNormal(CtmDealDetailCheckMayRepeatUtils.repeatAddFactors, InvocationInfoProxy.getTenantid());
            List<Object> ids = list.stream().map(BankReconciliation::getId).collect(Collectors.toList());
            List<Object> repeatIds = repeatList.stream().map(BankReconciliation::getId).collect(Collectors.toList());
            ids.addAll(repeatIds);
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", ids));
        }

        FilterVO filterVO = new FilterVO();
        if (billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            if (commonVOs != null && commonVOs.length > 0) {
                List<QueryOrderby> queryOrderlyList = new ArrayList<>();
                queryOrderlyList.add(new QueryOrderby("tran_date", ICmpConstant.ORDER_DESC));
                for (String repeatFactor : CtmDealDetailCheckMayRepeatUtils.repeatFactors) {
                    if("tran_date".equalsIgnoreCase(repeatFactor)) {
                        continue;
                    }else if("bankaccount".equalsIgnoreCase(repeatFactor)) {
                        repeatFactor = "bankaccount.account";
                    }else if("accentity".equalsIgnoreCase(repeatFactor)) {
                        repeatFactor = "accentity.code";
                    }else if("orgid".equalsIgnoreCase(repeatFactor)) {
                        repeatFactor = "orgid.code";
                    }
                    if (repeatOrderBy == 1) {
                        queryOrderlyList.add(new QueryOrderby(repeatFactor, ICmpConstant.ORDER_ASC));
                    } else {
                        queryOrderlyList.add(new QueryOrderby(repeatFactor, ICmpConstant.ORDER_DESC));
                    }
                }
                queryOrderlyList.add(new QueryOrderby("isrepeat", ICmpConstant.ORDER_ASC));
                billDataDto.setQueryOrders(queryOrderlyList);
            }
            filterVO.setCommonVOs(commonVOs);
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
