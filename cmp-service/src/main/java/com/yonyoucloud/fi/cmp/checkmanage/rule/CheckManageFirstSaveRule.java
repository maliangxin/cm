package com.yonyoucloud.fi.cmp.checkmanage.rule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStockService;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支票处置保存规则 - 仅中广核使用
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:07
 */
@Slf4j
@Component
public class CheckManageFirstSaveRule extends AbstractCommonRule {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    CheckStatusService checkStatusService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        //插入支票处置
        List<CheckManage> bills = getBills(billContext, map);
        List checkids = new ArrayList<>();
        bills.get(0).CheckManageDetail().stream().forEach(e -> checkids.add(e.getCheckid()));
        //更新支票工作台
        QuerySchema querySchema = QuerySchema.create().addSelect(" * ");
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("id").in(checkids));
        querySchema.addCondition(queryConditionGroup);
        List<CheckStock> checkManageQuery = MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
        checkStatusService.recordCheckStatus(checkManageQuery,CmpCheckStatus.Cancle.getValue());
        checkManageQuery.stream().forEach(e -> e.setCheckBillStatus(CmpCheckStatus.Cancle.getValue()));
        EntityTool.setUpdateStatus(checkManageQuery);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkManageQuery);
        return new RuleExecuteResult();
    }
}
