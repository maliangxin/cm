package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationQueryService;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 银行对账单查询接口
 *
 * @author xuwei
 * @date 2024/3/19
 */
@Service
public class BankReconciliationQueryServiceImpl implements BankReconciliationQueryService {

    @Override
    public List<BankReconciliation> queryBySchema(QuerySchema schema) throws Exception {

        List<Map<String, Object>> resList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema);
        List<BankReconciliation> bankReconciliationList = new ArrayList<>();
        for (Map<String, Object> map : resList) {
            BankReconciliation bankReconciliation = new BankReconciliation();
            bankReconciliation.putAll(map);
            bankReconciliationList.add(bankReconciliation);
        }
        return bankReconciliationList;
    }

}
