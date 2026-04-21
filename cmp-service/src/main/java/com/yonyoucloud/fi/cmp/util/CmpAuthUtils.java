package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.metadata.api.model.base.term.LegacyTerms;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.imeta.core.model.Entity;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2024年08月30日 20:10
 * @Description:权限工具类
 */
public class CmpAuthUtils {

    private static final String DATA_AUTH_LABELCODE = "data_auth";
    private static final String SYSCODE = "CM";
    public static final List<String> GLOBAL_ACCENTITY_BILLNUMLIST = new ArrayList<>();

    static {
        // 现金参数
        GLOBAL_ACCENTITY_BILLNUMLIST.add(IBillNumConstant.CMP_AUTOCONFIG);
        // 流水自动辨识匹配规则
        GLOBAL_ACCENTITY_BILLNUMLIST.add(IBillNumConstant.CMP_BANKRECONCILIATIONIDENTIFYSETTING);
        GLOBAL_ACCENTITY_BILLNUMLIST.add(IBillNumConstant.CMP_BANKRECONCILIATIONIDENTIFYSETTINGLIST);
    }

    /**
     * 根据元数据uri给查询条件添加数据权限条件
     * @param condition
     * @return
     */
    public static void addDataPermissionCondition(QueryConditionGroup condition, String  fullname) throws Exception {
        //String[] fields =new String[] {"parentAccentity", "dept","project", "supplier", "customer","employee"};
        Entity entity = MetaDaoHelper.getEntity(fullname);
        List<String> fieldsList = entity.attributes().stream()
                .filter(attr -> attr.containsTerm(LegacyTerms.Term.data_auth))
                .map(attr -> attr.name())
                .collect(Collectors.toList());
        String[] fields = fieldsList.toArray(new String[0]);
        Map<String, List<Object>> dataPermission = AuthUtil.dataPermission(SYSCODE, fullname, null,fields);

        if(dataPermission != null && dataPermission.size() >0 ){
            for(Map.Entry<String, List<Object>> entry : dataPermission.entrySet()){
                if (fieldsList.contains(entry.getKey())&&entry.getValue()!=null&&entry.getValue().size()>0) {
                    List<QueryCondition> conditionList1 = new ArrayList<>();
                    conditionList1.add(QueryCondition.name(entry.getKey()).in(entry.getValue()));
                    conditionList1.add(QueryCondition.name(entry.getKey()).is_null());
                    QueryConditionGroup queryCondition =  QueryConditionGroup.or(conditionList1.toArray(new QueryCondition[0]));
                    condition.addCondition(queryCondition);
                }
            }
        }
    }

    /**
     * 根据元数据uri给查询条件添加数据权限条件
     * @param condition
     * @return
     */
    public static void addChildDataPermissionCondition(QueryConditionGroup condition, String fullname, String  items_key) throws Exception {
        //String[] fields =new String[] {"parentAccentity", "dept","project", "supplier", "customer","employee"};
        String childPrefix = items_key + ".";
        Entity entity = MetaDaoHelper.getEntity(fullname);
        List<String> fieldsList = entity.attributes().stream()
                .filter(attr -> attr.containsTerm(LegacyTerms.Term.data_auth))
                .map(attr -> attr.name())
                .collect(Collectors.toList());
        String[] fields = fieldsList.toArray(new String[0]);
        Map<String, List<Object>> dataPermission = AuthUtil.dataPermission(SYSCODE, fullname, null,fields);

        if(dataPermission != null && dataPermission.size() >0 ){
            for(Map.Entry<String, List<Object>> entry : dataPermission.entrySet()){
                if (fieldsList.contains(entry.getKey())&&entry.getValue()!=null&&entry.getValue().size()>0) {
                    List<QueryCondition> conditionList1 = new ArrayList<>();
                    conditionList1.add(QueryCondition.name(childPrefix + entry.getKey()).in(entry.getValue()));
                    conditionList1.add(QueryCondition.name(childPrefix + entry.getKey()).is_null());
                    QueryConditionGroup queryCondition =  QueryConditionGroup.or(conditionList1.toArray(new QueryCondition[0]));
                    condition.addCondition(queryCondition);
                }
            }
        }
    }
//    @Deprecated
//    public static void addChildrenDataPermissionCondition(QuerySchema querySchema, String  childFullname) throws Exception {
//        QueryConditionGroup dataPermissionConditionGroup = getDataPermissionConditionGroup(childFullname);
//        QuerySchema childQuerySchema = QuerySchema.create();
//        //childQuerySchema.fullname(BillClaimItem.ENTITY_NAME);
//        childQuerySchema.name(BillClaim.ITEMS_KEY);
//        childQuerySchema.addSelect("*");
//        childQuerySchema.addCondition(dataPermissionConditionGroup);
//        querySchema.addCompositionSchema(childQuerySchema);
//    }

    public static QueryConditionGroup getDataPermissionConditionGroup(String  fullname) throws Exception {
        //String[] fields =new String[] {"parentAccentity", "dept","project", "supplier", "customer","employee"};
        QueryConditionGroup queryConditionGroup = null;
        Entity entity = MetaDaoHelper.getEntity(fullname);
        List<String> fieldsList = entity.attributes().stream()
                .filter(attr -> attr.containsTerm(LegacyTerms.Term.data_auth))
                .map(attr -> attr.name())
                .collect(Collectors.toList());
        String[] fields = fieldsList.toArray(new String[0]);
        Map<String, List<Object>> dataPermission = AuthUtil.dataPermission(SYSCODE, fullname, null,fields);

        if(dataPermission != null && dataPermission.size() >0 ){
            for(Map.Entry<String, List<Object>> entry : dataPermission.entrySet()){
                if (fieldsList.contains(entry.getKey())&&entry.getValue()!=null&&entry.getValue().size()>0) {
                    List<QueryCondition> conditionList1 = new ArrayList<>();
                    conditionList1.add(QueryCondition.name(entry.getKey()).in(entry.getValue()));
                    conditionList1.add(QueryCondition.name(entry.getKey()).is_null());
                    queryConditionGroup =  QueryConditionGroup.or(conditionList1.toArray(new QueryCondition[0]));
                }
            }
        }
        return queryConditionGroup;
    }

}
