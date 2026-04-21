package com.yonyoucloud.fi.cmp.common.service.impl;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: ctm-cmp
 * @BelongsPackage: com.yonyoucloud.fi.cmp.common.service.impl
 * @Author: wenyuhao
 * @CreateTime: 2023-12-18  11:12
 * @Description: 基础档案结算方式service aa.settlemethod.SettleMethod
 * @Version: 1.0
 */
@Slf4j
@Service
public class SettleMethodServiceImpl implements SettleMethodService {

    /**
     * @description: 根据结算方式查询基础档案结算方式数据
     * @author: wenyuhao
     * @date: 2023/12/18 11:13
     * @param: [settlemode]
     * @return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    @Override
    public List<Map<String, Object>> listSettleMethodByBankTransSettlemode(String settlemode) throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("serviceAttr").eq("0"));
        conditionGroup.appendCondition(QueryCondition.name("directConnection").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("id").in(settlemode));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settleMethodList = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
        return settleMethodList;
    }

    @Override
    public List<Map<String, Object>> getSettleMethodByBankTransSettlemode(String settlemode) throws Exception {
        List<Map<String, Object>> settleMethodList = new ArrayList<>();
        try {
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").in(settlemode));
            schema.addCondition(conditionGroup);
            settleMethodList = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
            if (CollectionUtils.isEmpty(settleMethodList)) {
                QuerySchema schema1 = QuerySchema.create();
                schema1.addSelect("*");
                QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup1.appendCondition(QueryCondition.name("id").in(settlemode));
                schema1.addCondition(conditionGroup1);
                settleMethodList = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema1, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
            }
        } catch (Exception e) {
            log.error("getSettleMethodByBankTransSettlemode exception", e);
        }
        return settleMethodList;
    }

    /**
     * @description: 查找并判断结算方式为银行转账，是否直连为是时返回true，否则返回false
     * @author: wenyuhao
     * @date: 2023/12/14 16:42
     * @param: [settlemode] 结算方式
     * @return: java.lang.Boolean
     **/
    @Override
    public Boolean checkSettleMethod(String settlemode) throws Exception{
        List<Map<String, Object>> settleMethodList = listSettleMethodByBankTransSettlemode(settlemode);
        if(CollectionUtils.isNotEmpty(settleMethodList)){
            return true;
        }
        return false;
    }

    /**
     * 判断结算方式是否由非直连的银行转账变为直连银行转账，用于前端页面渲染
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Boolean checkSettleMethodCleanPayBankAccount(CtmJSONObject param) throws Exception{
        Boolean oldIdBool = false;
        if(param.get("settlemodeOldId") != null){
            String settlemodeOldId=param.get("settlemodeOldId").toString();
            oldIdBool=checkSettleMethod(settlemodeOldId);
        }
        String settlemodeNowId=param.get("settlemodeId").toString();
        Boolean nowIdBool=checkSettleMethod(settlemodeNowId);
        if(!oldIdBool && nowIdBool){
            return true;
        }
        return false;
    }

    @Override
    public  List<Object> listSettleMethodByService_attr(int service_attr) throws Exception{
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("serviceAttr").eq(service_attr));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> settleMethodList = MetaDaoHelper.query("aa.settlemethod.SettleMethod", schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
        List<Object> listResult = new ArrayList<>();
        if (!CollectionUtils.isEmpty(settleMethodList)){
            settleMethodList.stream().forEach(map->{
                if (null != map){
                    map.forEach((k,v)->{
                        listResult.add(v);
                    });
                }
            });
            return listResult;
        }
        return null;
    }
}
