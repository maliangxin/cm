package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoConfirmBillService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoPushBillService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author qihaoc
 * @Description:自动生单自动确认service
 * @date 2024/1/27 22:30
 */

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankAutoConfirmBillServiceImpl implements BankAutoConfirmBillService {

    @Override
    public void autoConfirm(BankReconciliation bankReconciliation) throws Exception {
        bankReconciliation.setIschoosebill(true);
        List<BankReconciliationbusrelation_b> relation_b = queryBankReconciliationRelationData(bankReconciliation.getId().toString());
        if (relation_b.size() == 1) {
            bankReconciliation.setAutocreatebillcode(relation_b.get(0).get("billcode").toString());
            bankReconciliation.setBankReconciliationbusrelation_b(relation_b);
            JsonNode bill = JSONBuilderUtils.beanToJson(bankReconciliation);
            BankAutoPushBillService autoPushBillService = CtmAppContext.getBean(BankAutoPushBillService.class);
            autoPushBillService.confirmBill(bill);
        }
    }

    private List<BankReconciliationbusrelation_b> queryBankReconciliationRelationData(String bankreconciliationId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.addCondition(QueryCondition.name("bankreconciliation").eq(bankreconciliationId));
        querySchema.addCondition(group);
        List<BankReconciliationbusrelation_b> infoMapList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        return infoMapList;
    }
}
