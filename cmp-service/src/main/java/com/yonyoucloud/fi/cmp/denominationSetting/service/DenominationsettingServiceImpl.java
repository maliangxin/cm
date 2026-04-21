package com.yonyoucloud.fi.cmp.denominationSetting.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cashinventory.CashInventory;
import com.yonyoucloud.fi.cmp.cashinventory.CashInventory_b;
import com.yonyoucloud.fi.cmp.denominationSetting.DenominationsettingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class DenominationsettingServiceImpl implements DenominationsettingService {

    @Override
    public CtmJSONObject checkDenominationsetting(Long id) throws Exception{
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("denominationSettingId").eq(id));
        QuerySchema schema = QuerySchema.create().addSelect("id");
        schema.addCondition(group);
        List<Map<String,Object>> cashInventorys = MetaDaoHelper.query(CashInventory.ENTITY_NAME, schema);
        CtmJSONObject result = new CtmJSONObject();
        if(CollectionUtils.isEmpty(cashInventorys)){
            result.put("isQuoted",true);
        }else{
            result.put("isQuoted",false);
        }
        return result;
    }

    @Override
    public CtmJSONObject checkDenominationsetting_b(Long id) throws Exception{
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("denominationSettingbId").eq(id));
        QuerySchema schema = QuerySchema.create().addSelect("id");
        schema.addCondition(group);
        List<Map<String,Object>> cashInventory_bs = MetaDaoHelper.query(CashInventory_b.ENTITY_NAME, schema);
        CtmJSONObject result = new CtmJSONObject();
        if(CollectionUtils.isEmpty(cashInventory_bs)){
            result.put("isQuoted",true);
        }else{
            result.put("isQuoted",false);
        }
        return result;
    }
}
