package com.yonyoucloud.fi.cmp.util;


import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationDetail;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.BankReconciliationVo;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName PageUtils * @Description TODO
 * @Author yuanyhui
 * @Date 15:07 2022/11/1
 * @Version 1.0
 **/
public class PageUtils {

    /**
     * 按照传入的参数，进行分页
     * @param pageNum  页数
     * @param pageSize 每页条数
     * @param sourceList 源列表
     * @param isPage 是否分页
     * @param <T> 泛型对象
     * @return 返回结果集
     */
    public static <T> ResultList<T> page(Integer pageNum, Integer pageSize, List<T> sourceList, Boolean isPage) {

        ResultList<T> resultList = new ResultList<>();
        // 初始化总量为0，防止当列表为空的时候，total返回null
        resultList.setTotal(0);
        List<T> pageList = sourceList;

        // 当列表不为空的时候，才进行数据逻辑处理
        if (sourceList != null && !sourceList.isEmpty()) {
            int sourceSize = sourceList.size();
            if (isPage) {
                pageNum = (pageNum == null || pageNum <= 0) ? 1 : pageNum;
                pageSize = (pageSize == null || pageSize <= 0) ? 10 : pageSize;

                // 步骤分解，便于理解
//                int startPosition = Math.min((pageNum - 1) * pageSize, sourceSize);
//                int endPosition = Math.min(pageNum * pageSize, sourceSize);
//                pageList = sourceList.subList(startPosition, endPosition);
                // 分页的起始和截止位置，和源列表的size大小进行对比，分别取最小值
                pageList = sourceList.subList(Math.min((pageNum - 1) * pageSize, sourceSize), Math.min(pageNum * pageSize, sourceSize));
            }
            resultList.setList(pageList);
            resultList.setTotal(sourceSize);
        }

        return resultList;
    }

    /**
     * 逻辑内存分页-解决单次查询超过平台(默认200000)限制报错
     * @param schema 查询列表的schema
     * @param conditionGroup 查询列表传入的条件
     * @param entityName 需要查询的实体名称
     * @return 查询所有的集合
     * @throws Exception 查询数据库出现的异常
     */
    public static <T extends BizObject> List<T>  logicPageQuery(QuerySchema schema, QueryConditionGroup conditionGroup, String entityName) throws Exception {
        long count = queryCount(conditionGroup, entityName);
        int pageIndex = 0;
        int pageSize = Integer.parseInt(AppContext.getEnvConfig("cmp.bankReconciliation.BankSummarySize", "100000"));
        List<T> resultList = new ArrayList<>();
        while (count > 0) {
            schema.addPager(pageIndex, pageSize);
            List<T> list = MetaDaoHelper.queryObject(entityName, schema, null);
            if (CollectionUtils.isEmpty(list)) {
                break;
            }
            resultList.addAll(list);
            pageIndex ++;
            count -= list.size();
        }
        return resultList;
    }

    /**
     * 查询条数
     * @param conditionGroup 查询列表传入的条件
     * @param entityName 需要查询的实体名称
     * @return 查询所有的集合
     * @throws Exception 查询数据库出现的异常
     */
    public static long queryCount(QueryConditionGroup conditionGroup, String entityName) throws Exception {
        QuerySchema queryCountSchema = QuerySchema.create().addSelect(" count(distinct(id)) as count ");
        queryCountSchema.appendQueryCondition(conditionGroup);
        if (BankReconciliation.ENTITY_NAME.equals(entityName)) {
            //银行对账单权限筛选，权限字段在主表上
            CmpAuthUtils.addDataPermissionCondition(conditionGroup, BankReconciliation.ENTITY_NAME);
        } else if (BillClaim.ENTITY_NAME.equals(entityName)) {
            //认领单权限筛选，权限字段在子表上
            CmpAuthUtils.addDataPermissionCondition(conditionGroup, BillClaim.ENTITY_NAME);
            CmpAuthUtils.addChildDataPermissionCondition(conditionGroup, BillClaimItem.ENTITY_NAME, BillClaim.ITEMS_KEY);
        }
        Map<String, Object> countMap = MetaDaoHelper.queryOne(entityName, queryCountSchema);
        if (!countMap.isEmpty()) {
            return Long.parseLong(String.valueOf(countMap.get("count")));
        } else {
            return  0L;
        }
    }
}
