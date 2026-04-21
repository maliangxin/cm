package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.bizdoc.service.settlemethod.ISettleMethodQueryService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SettleMethodQueryService {

    @Autowired
    ISettleMethodQueryService settleMethodQueryService;
    /**
     * 根据结算方式id查询结算方式
     * @param id
     * @return
     * @throws Exception
     */
    public SettleMethodModel findById(Long id) throws Exception {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setId(id);
        List<SettleMethodModel> list = settleMethodQueryService.querySettleMethods(settleMethodQueryParam);
        return list.get(0);
    }

    /**
     * 根据结算方式id集合查询结算方式
     * @param ids
     * @return
     * @throws Exception
     */
    public List<SettleMethodModel> findByIds(List<Long> ids) throws Exception {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setIds(ids);
        List<SettleMethodModel> list = settleMethodQueryService.querySettleMethods(settleMethodQueryParam);
        return list;
    }

    /**
     * 查询结算方式是否直联 *
     */

    public String querySettleMethodWayByCondition(String id) throws Exception {

        BillContext billContext = new BillContext();
        billContext.setFullname("aa.settlemethod.SettleMethod");
        billContext.setDomain("productcenter");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,directConnection");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> data = MetaDaoHelper.query(billContext, schema);
        if (data != null && !data.isEmpty()) {
            if (null == data.get(0).get("directConnection")) {
                return "0";
            } else {
                return data.get(0).get("directConnection").toString();
            }

        }
        return "";
    }


    /**
     * 查询结算方式详情
     */
    public List<Map<String, Object>> querySettleMethodByCondition(Map condition,String queryStr) throws Exception {
        List<Long> ids = (List<Long>) condition.get("ids");
        BillContext billContext = new BillContext();
        billContext.setFullname("aa.settlemethod.SettleMethod");
        billContext.setDomain("productcenter");
        QuerySchema schema = QuerySchema.create();
        // "id,directConnection"
        schema.addSelect(queryStr);
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> data = MetaDaoHelper.query(billContext, schema);
        return data;
    }

}
