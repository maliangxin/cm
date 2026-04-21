package com.yonyoucloud.fi.cmp.bankreconciliation.plugin;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.interfaces.ExecuteExtendSumDataHandler;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryField;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import com.yonyoucloud.fi.cmp.common.CtmException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author qihaoc
 * @Description:银行对账单处理数据权限重复的数据后，需要对求合计的数据重新计算
 * @date 2023/2/13 14:43
 */
@Slf4j
@Service
public class CtmExecuteExtendSumDataHandler implements ExecuteExtendSumDataHandler {
    @Override
    public List<Map<String, Object>> execute(String fullname, QuerySchema querySchema) {
        //只有银行回单进行分组查询时才做后续处理
        if (!BankReconciliation.ENTITY_NAME.equals(fullname)) {
            return null;
        }
        if (BankReconciliation.ENTITY_NAME.equals(fullname)
                && (querySchema.groupbyFields() == null || querySchema.groupbyFields().isEmpty())) {
            //分组条件为空且使用了数据权限，追加分组,没有使用时直接返回
            if (useDataPremisson()) {
                querySchema.addGroupBy("id");
            } else {
                return null;
            }
        }
        //使用group by 分组时不用sql进行汇总
        List<QueryField> fields = querySchema.selectFields();
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        HashMap<String, Object> sumFieldMaps = new HashMap<String, Object>();
        for (QueryField field : fields) {
            if ("sum".equals(field.aggr())) {
                sumFieldMaps.put(field.name(), BigDecimal.ZERO);
                field.setAggr(null);
                //start wangdengk 2023-08-01 适配oracle数据库 增加group by分组字段
                //querySchema.addGroupBy(field.name());
                //end wangdengk 2023-08-01 适配oracle数据库 增加group by分组字段
            }
        }
        //查询去重数据
        List<Map<String, Object>> mapList = null;
        try {
            mapList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
        } catch (Exception e) {
            log.error("QuerySchemaExecutor error，entity uri:{},queryschema:{}", fullname, querySchema.toString());
            throw new CtmException("QuerySchemaExecutor error，entity uri:" + fullname + "，errorMessage:" + e.getMessage() + ",queryschema:" + querySchema.toString(), e);//@notranslate
        }
        if (mapList == null || mapList.isEmpty()) {
            return null;
        }
        //对去重数据统计合计值
        Set<String> sumFields = sumFieldMaps.keySet();
        Iterator iterator = sumFields.iterator();
        while (iterator.hasNext()) {
            String sumField = (String) iterator.next();
            BigDecimal newVal = (BigDecimal) sumFieldMaps.get(sumField);
            for (Map<String, Object> row : mapList) {
                if (row.get(sumField) != null && (row.get(sumField) instanceof BigDecimal)) {
                    newVal = newVal.add((BigDecimal) row.get(sumField));
                }
            }
            sumFieldMaps.put(sumField, newVal);
        }
        List<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();
        retList.add(sumFieldMaps);
        return retList;
    }

    public boolean useDataPremisson() {
        //使用银行对账单应用编码查询数据权限
//        DataPermissionRequestDto requestDto = new DataPermissionRequestDto();
//        requestDto.setEntityUri(BankReconciliation.ENTITY_NAME);
//        requestDto.setSysCode("");// 实体所属应用编码
//        requestDto.setYxyUserId(AppContext.getCurrentUser().getYxyUserId());
//        requestDto.setYxyTenantId(AppContext.getCurrentUser().getYxyTenantId());
//        requestDto.setHaveDetail(true); // 是否返回values
//        DataPermissionResponseDto res = null;
//        try {
//            res = AuthSdkFacadeUtils.getUserDataPermission(requestDto);
//        } catch (Exception e) {
//            return false;
//        }
//        if (res != null && !res.getDataPermissionMapList().isEmpty()) {
//            return true;
//        } else {
//            return false;
//        }
        //公有云暂时使用--start
        return false;
        //公有云暂时使用--end
        //海康暂时使用--start
//        return true;
        //海康暂时使用--end
    }
}
