package com.yonyoucloud.fi.cmp.util.costcenter;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.option.util.OptionUtils;
import com.yonyoucloud.fi.bd.costcenter.CostCenter;
import com.yonyoucloud.fi.bd.costcenter.CostCenterDis;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;

import java.util.List;
import java.util.Map;

/**
 * @author: zhangdhu
 * @date: 2024/04/17
 * @description:
 */
public class CostCenterUtils {
    /**
     * 是否调用成本中心规则
     */
    public static final String AMP_001 = "AMP001";
    /**
     * 是否调用利润中心规则
     */
    public static final String AMP_002 = "AMP002";

    /**
     * 查询成本中心和利润中心
     *
     * @param bizObject
     */
    public static void setCostCenter(BizObject bizObject, String deptField, String accentityField, String costCenterField, String profitCenterField) throws Exception {
        // 是否调用成本中心规则
        boolean costCenterFlag = (boolean) OptionUtils.getSysOptionByName(AMP_001);
        // 是否调用利润中心规则
        boolean profitCenterFlag = (boolean) OptionUtils.getSysOptionByName(AMP_002);
        if (bizObject.get(deptField) == null || !(bizObject.get(costCenterField) == null && costCenterFlag)) {
            return;
        }
        // 此处查询成本中心和利润中心是否需要赋默认值
        QuerySchema querySchema =
                QuerySchema.create().addSelect(ICmpConstant.ID).
                        appendQueryCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(bizObject.get(accentityField)))
                        .appendQueryCondition(QueryCondition.name(ICmpConstant.RELATIONS_DEPT).eq(bizObject.get(deptField)))
                        .appendQueryCondition(QueryCondition.name(ICmpConstant.EFFECT).eq(true));
        List<CostCenter> costCenters = MetaDaoHelper.queryObject(CostCenter.ENTITY_NAME, querySchema, ISchemaConstant.MDD_SCHEMA_FINBD);
        // 有且只有一条的时候，才可以赋默认值
        if (costCenters != null && costCenters.size() == 1) {
            CostCenter costCenter = MetaDaoHelper.findById(CostCenter.ENTITY_NAME, costCenters.get(0).getId(), 2);
            bizObject.put(costCenterField, costCenter.getId());
            if (bizObject.get(profitCenterField) == null && profitCenterFlag) {
                List<CostCenterDis> costCenterDisList = costCenter.dis();
                if (costCenterDisList != null && costCenterDisList.size() == 1) {
                    bizObject.put(profitCenterField, Long.valueOf(costCenterDisList.get(0).getProfitCenter()));
                }
            }
        }
    }

    /**
     * 部门、成本中心、利润中心, 根据上级单据带出
     *
     * @param bill       单据
     * @param entityName 上级单据entityName
     * @param upperId    上级单据id
     */
    public static void setCostCenterFromUpper(Map bill, String entityName, Object upperId) throws Exception {
        QuerySchema querySchema = new QuerySchema();
        querySchema.addSelect("deptid,deptid.name as deptid_name,costCenter,costCenter.name as costCenter_name,profitCenter,profitCenter.name as profitCenter_name");
        querySchema.appendQueryCondition(QueryCondition.name(ICmpConstant.ID).eq(upperId));
        Map result = MetaDaoHelper.queryOne(entityName, querySchema);
        if (result != null) {
            if (result.containsKey("deptid")) {
                bill.put("deptid", result.get("deptid"));
                bill.put("deptid_name", result.get("deptid_name"));
            }
            if (result.containsKey("costCenter")) {
                bill.put("costCenter", result.get("costCenter"));
                bill.put("costCenter_name", result.get("costCenter_name"));
            }
            if (result.containsKey("profitCenter")) {
                bill.put("profitCenter", result.get("profitCenter"));
                bill.put("profitCenter_name", result.get("profitCenter_name"));
            }
            if (result.containsKey("costCenter") && !result.containsKey("costCenter_name")) {
                CostCenter costCenter = CostCenterUtils.getCostCenterById(Long.valueOf(result.get("costCenter").toString()));
                bill.put("costCenter_name", costCenter == null ? null : costCenter.getName());
            }
        }
    }

    public static CostCenter getCostCenterById(Long id) throws Exception {
        if (id == null) {
            return null;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id,name").appendQueryCondition(QueryCondition.name(ICmpConstant.ID).eq(id));
        List<CostCenter> costCenters = MetaDaoHelper.queryObject(CostCenter.ENTITY_NAME, querySchema, ISchemaConstant.MDD_SCHEMA_FINBD);
        return CollectionUtils.isNotEmpty(costCenters) ? costCenters.get(0) : null;
    }
}
