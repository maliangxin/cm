package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 自动生单规则配置接口具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/28 15:39
 */

@Slf4j
@Service
public class AutoOrderRuleConfigServiceImpl  implements AutoOrderRuleConfigService {

    @Resource
    private YmsOidGenerator ymsOidGenerator;

    @Override
    public List<AutoorderruleConfig> queryConfigInfo(AutoorderruleConfig autoorderruleConfig) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(
                QueryConditionGroup.and(QueryCondition.name("mainid").eq(autoorderruleConfig.getMainid()))
        );
        schema.addCondition(conditionGroup);
        List<AutoorderruleConfig> oldConfigs = MetaDaoHelper.queryObject(AutoorderruleConfig.ENTITY_NAME,schema,null);
        return oldConfigs;
    }

    @Override
    public CtmJSONObject updateConfigInfo(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //主表id
        Long mainid = params.getLong("mainid");

        String key = "AutoorderruleConfig：" + mainid;//@notranslate
        //根据租户ID锁定，只能一个租户同时操作
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100849"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180345","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
        }
        try {
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup();
            conditionGroup.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("mainid").eq(mainid))
            );
            schema.addCondition(conditionGroup);
            //先删除老的配置，再将新配置插入
            List<AutoorderruleConfig> oldConfigs = MetaDaoHelper.queryObject(AutoorderruleConfig.ENTITY_NAME,schema,null);
            if (!oldConfigs.isEmpty()){
                MetaDaoHelper.delete(AutoorderruleConfig.ENTITY_NAME,oldConfigs);
            }

            //获取客户/供应商类型
            short otherType = params.getShort("otherType");

            CtmJSONArray dataArr = params.getJSONArray("data");

            List<AutoorderruleConfig> newConfigs = new ArrayList<>();

            if (dataArr.size()>0){
                for(int i=0;i<dataArr.size();i++){
                    CtmJSONObject j  = dataArr.getJSONObject(i);
                    AutoorderruleConfig config = new AutoorderruleConfig();
                    config.setId(ymsOidGenerator.nextId());
                    config.setMainid(mainid);
                    config.setOppositetype(otherType);
                    config.setOppositeName(j.getString("name"));
                    config.setOppositeCode(j.getString("code"));
                    if (otherType == OppositeType.Customer.getValue()){
                        config.setCustomer(j.getLong("id"));
                    }else if (otherType ==  OppositeType.Supplier.getValue()){
                        config.setSupplier(j.getLong("id"));
                    }
                    config.setEntityStatus(EntityStatus.Insert);
                    newConfigs.add(config);
                }
                CmpMetaDaoHelper.insert(AutoorderruleConfig.ENTITY_NAME,newConfigs);
            }

            result.put("code",200);
            result.put("message","success");
        }catch (Exception e){
            throw e;
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }


        return result;
    }
}
