package com.yonyoucloud.fi.cmp.util;

import com.google.common.collect.Lists;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.ConditionExpression;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.*;


/**
 * @author xuxbo
 * @description 操作数据库工具类
 * @date 2023/4/23
 */
public class MetaDaoUtils {

    /**
     * 查询数据库
     * qu
     *
     * @param entityName
     * @param schema
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    private static <T extends BizObject> List<T> queryBizObject(String entityName, QuerySchema schema, String fieldName, List<Long> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) return Collections.emptyList();
        schema.appendQueryCondition(QueryCondition.name(fieldName).in(ids));
        return MetaDaoHelper.queryObject(entityName, schema, null);
    }

    /**
     * @param entityName
     * @param schema
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends BizObject> List<T> batchQueryBizObject(String entityName, QuerySchema schema, String fieldName, List<Long> ids) throws Exception {
        if (ids.size() <= ICmpConstant.BATCH_QUERY_SIZE) {
            return queryBizObject(entityName, schema, fieldName, ids);
        }
        List<List<Long>> partitionList = Lists.partition(ids, ICmpConstant.BATCH_QUERY_SIZE);
        List<T> result = Lists.newArrayList();
        for (List<Long> idList : partitionList) {
            handleSchema(schema,fieldName);
            result.addAll(queryBizObject(entityName, schema, fieldName, idList));
        }
        return result;
    }

    private static void handleSchema(QuerySchema schema, String fieldName) throws Exception {
        QueryConditionGroup group = Optional.ofNullable(schema.queryConditionGroup()).orElse(new QueryConditionGroup());
        List<ConditionExpression> conditionExpressionList = Optional.ofNullable(group.conditions()).orElseGet(ArrayList::new);
        ConditionExpression expression = conditionExpressionList.stream().filter(e -> ((QueryCondition) e).field().equals(fieldName)).findAny().orElse(null);
        conditionExpressionList.remove(expression);
    }

    /**
     * @param entityName
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends BizObject> List<T> batchQueryBizObject(String entityName, String fieldName, List<Long> ids) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        return batchQueryBizObject(entityName, schema, fieldName, ids);
    }

    /**
     * 查询数据库
     * qu
     *
     * @param entityName
     * @param schema
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    private static <T extends BizObject> List<T> queryBizObjectWithString(String entityName, QuerySchema schema, String fieldName, List<String> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) return Collections.emptyList();
        schema.appendQueryCondition(QueryCondition.name(fieldName).in(ids));
        return MetaDaoHelper.queryObject(entityName, schema, null);
    }


    /**
     * @param entityName
     * @param schema
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends BizObject> List<T> batchQueryBizObjectWithString(String entityName, QuerySchema schema, String fieldName, List<String> ids) throws Exception {
        if (ids.size() <= ICmpConstant.BATCH_QUERY_SIZE) {
            return queryBizObjectWithString(entityName, schema, fieldName, ids);
        }
        List<List<String>> partitionList = Lists.partition(ids, ICmpConstant.BATCH_QUERY_SIZE);
        List<T> result = Lists.newArrayList();
        for (List<String> idList : partitionList) {
            handleSchema(schema,fieldName);
            result.addAll(queryBizObjectWithString(entityName, schema, fieldName, idList));
        }
        return result;
    }


    /**
     * @param entityName
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends BizObject> List<T> batchQueryBizObjectWithString(String entityName, String fieldName, List<String> ids) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        return batchQueryBizObjectWithString(entityName, schema, fieldName, ids);
    }


    /**
     * @param billContext
     * @param schema
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends Map<String, Object>> List<T> query(BillContext billContext, QuerySchema schema, String fieldName, List<Object> ids) throws Exception {
        schema.appendQueryCondition(QueryCondition.name(fieldName).in(ids));
        return MetaDaoHelper.query(billContext, schema);
    }

    /**
     * @param billContext
     * @param schema
     * @param fieldName
     * @param ids
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends Map<String, Object>> List<T> batchQuery(BillContext billContext, QuerySchema schema, String fieldName, List<Object> ids) throws Exception {
        if (ids.size() <= ICmpConstant.BATCH_QUERY_SIZE) {
            return query(billContext, schema, fieldName, ids);
        }
        List<List<Object>> partitionList = Lists.partition(ids, ICmpConstant.BATCH_QUERY_SIZE);
        List<T> result = Lists.newArrayList();
        for (List<Object> idList : partitionList) {
            result.addAll(query(billContext, schema, fieldName, idList));
        }
        return result;
    }



}
