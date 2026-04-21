package com.yonyoucloud.fi.cmp.bankreconciliation.service.refund;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autorefundcheckrule.AutoRefundCheckRule;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @description: 退票辨识规则具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/6 15:09
 */

@Slf4j
@Service
public class RefundAutoCheckRuleServiceImpl implements RefundAutoCheckRuleService {

    @Resource
    private YmsOidGenerator ymsOidGenerator;

    @Override
    public AutoRefundCheckRule queryRuleInfo(CtmJSONObject params) throws Exception {
        String ytenantId = AppContext.getYTenantId();
        QuerySchema schema = QuerySchema.create().addSelect("*,banktype.name as banktype_name");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(
                QueryConditionGroup.and(QueryCondition.name("ytenantId").eq(ytenantId))
        );
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> configs = MetaDaoHelper.query(AutoRefundCheckRule.ENTITY_NAME,schema);
        if (configs != null) {
            for (Map<String, Object> map : configs) {
                AutoRefundCheckRule autoRefundCheckRule = new AutoRefundCheckRule();
                autoRefundCheckRule.init(map);
                return autoRefundCheckRule;
            }
        }
        return null;
    }

    @Override
    public String updateRuleInfo(CtmJSONObject params) throws Exception {
        String key = "AutoRefundCheckRuleKey：" + AppContext.getYTenantId();//@notranslate
        //根据租户ID锁定，只能一个租户同时操作
        YmsLock ymsLock = null;
        if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(key))==null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101202"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180002","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
        }
        try {
            if(params.get("daterange") != null && params.getInteger("daterange") > 7){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101203"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080106", "日期范围不能超过7天！") /* "日期范围不能超过7天！" */);
            }
            if(params.get("daterange") != null && params.getInteger("daterange") < 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101204"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080107", "日期范围不能为负数！") /* "日期范围不能为负数！" */);
            }
            Long id = params.getLong("id");
            if (id != null){ //有id为更新
                AutoRefundCheckRule config = MetaDaoHelper.findById(AutoRefundCheckRule.ENTITY_NAME,id);
                config.setEntityStatus(EntityStatus.Update);
                //更新数据
                initRuleInfo(params,config);
                MetaDaoHelper.update(AutoRefundCheckRule.ENTITY_NAME,config);
            }else { //无id为新增
                AutoRefundCheckRule config = new AutoRefundCheckRule();
                config.setId(ymsOidGenerator.nextId());
                config.setEntityStatus(EntityStatus.Insert);
                //拼装数据
                initRuleInfo(params,config);
                CmpMetaDaoHelper.insert(AutoRefundCheckRule.ENTITY_NAME,config);
            }
        }catch (Exception e){
            throw e;
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return "success";
    }

    /**
     * 组装规则信息
     * @param params
     * @param config
     */
    private void initRuleInfo(CtmJSONObject params,AutoRefundCheckRule config){
        config.setAccentityflag(params.getShort("accentityflag"));
        //是否币种相同
        config.setCurrencyflag(params.getShort("currencyflag"));
        //是否金额相同
        config.setAmountflag(params.getShort("amountflag"));
        //是否借贷方向相反
        config.setDirectionflag(params.getShort("directionflag"));
        //是否本方账号相同
        config.setAccountflag(params.getShort("accountflag"));
        config.setToaccountflag(params.getShort("toaccountflag"));
        config.setToaccountnameflag(params.getShort("toaccountnameflag"));
        config.setOppositetypeflag(params.getShort("oppositetypeflag"));
        //摘要匹配方式
        config.setRemarkmatch(params.getShort("remarkmatch"));
        // 银行类别
        config.setBanktype(params.getString("banktype"));
        // 时间范围
        config.setDaterange(params.get("daterange") != null ? params.getInteger("daterange") : null);
    }
}
