package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;


public class BankreconciliationQueryRule extends AbstractCommonRule {

    public static String ISQC = "isQc"; //期初标识

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);
        FilterVO filterVO = new FilterVO();
        boolean filterorg = false;
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
            filterorg = Arrays.stream(filterVO.getCommonVOs()).anyMatch(commonVO -> {
                return (commonVO.getItemName().equals(IFieldConstant.ORGID) || commonVO.getItemName().equals(IFieldConstant.ACCENTITY));
            });
        }
//        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "initflag", "eq", false));
        if(billContext.getBillnum().equals("cmp_bankreconciliationlist_pullR")){//收款单新增进来的,需要过滤“未勾对”，‘未生单’并且“贷方金额不为空”的银行对账单数据
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkflag", "eq", false));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "autobill", "eq", false));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "dc_flag", "eq", Direction.Credit.getValue()));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "creditamount", "gt", BigDecimal.ZERO));
        }else if(billContext.getBillnum().equals("cmp_bankreconciliationlist_pullP")){//付款单新增进来的,需要过滤“未勾对”，‘未生单’并且“借方金额不为空”的银行对账单数据
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "checkflag", "eq", false));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "autobill", "eq", false));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "dc_flag", "eq", Direction.Debit.getValue()));
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "debitamount", "gt", BigDecimal.ZERO));
        }

        //查询授权使用组织有权限的或者所属组织有权限的
        Set<String> orgs = BillInfoUtils.getOrgPermissions(IBillNumConstant.BANKRECONCILIATIONLIST);
        if (CollectionUtils.isEmpty(orgs)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_229FB39E0570000C", "没有授权使用组织权限!") /* "没有授权使用组织权限!" */);
        }
        if(CollectionUtils.isNotEmpty(orgs) && !filterorg){
            Map<String, String> map =  billDataDto.getParameters();
            QueryConditionGroup conditionGroupAuthFather = new QueryConditionGroup(ConditionOperator.and);//授权使用组织过滤
            QueryConditionGroup conditionGroupAuth = new QueryConditionGroup(ConditionOperator.or);//授权使用组织过滤
            QueryCondition queryConditionAuth = new QueryCondition("accentity", ConditionOperator.in, orgs);
            QueryCondition queryConditionOwnOrgid = new QueryCondition("orgid", ConditionOperator.in, orgs);
            //CZFW-383380 需求改动：授权使用组织页签只拼接授权使用组织，所属组织页签只拼接所属组织
            if(map.containsKey("cmpTableTabsActiveKey")){
                //1 授权使用组织节点
                if("1".equals(map.get("cmpTableTabsActiveKey"))){
                    conditionGroupAuth.addCondition(queryConditionAuth);
                }
                //2 所属组织节点
                if ("2".equals(map.get("cmpTableTabsActiveKey"))){
                    conditionGroupAuth.addCondition(queryConditionOwnOrgid);
                }
            }else {
                conditionGroupAuth.addCondition(queryConditionAuth);
                conditionGroupAuth.addCondition(queryConditionOwnOrgid);
            }
            conditionGroupAuthFather.addCondition(conditionGroupAuth);
            filterVO.setQueryConditionGroup(conditionGroupAuthFather);
        }
        // yms开关控制
        String orderRule = AppContext.getEnvConfig("cmp.bankreconciliation.queryorderrule","false");
        if ("true".equals(orderRule)) {
            List<QueryOrderby> orderByList = new ArrayList<>();
            orderByList.add(new QueryOrderby("tran_time", "asc"));
            orderByList.add(new QueryOrderby("tran_date", "desc"));
            orderByList.add(new QueryOrderby("bank_seq_no", "desc"));
            billDataDto.setQueryOrders(orderByList);
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
