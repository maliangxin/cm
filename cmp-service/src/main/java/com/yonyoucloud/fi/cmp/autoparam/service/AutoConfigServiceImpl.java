package com.yonyoucloud.fi.cmp.autoparam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.BaseConfigEnum;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.enums.JournalBalanceSortRuleEnum;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AutoConfigServiceImpl implements AutoConfigService {

	@Autowired
	private ISettleParamPubQueryService settleParamPubQueryService;

    @Override
    public JsonNode setDefault(Long id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("isDefault").eq("1"));
        querySchema.addCondition(group);
        List<AutoConfig> autoConfigList = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema);
        if (CollectionUtils.isNotEmpty(autoConfigList)) {
            AutoConfig defaultConfig = new AutoConfig();
            defaultConfig.init(autoConfigList.get(0));
            defaultConfig.setIsDefault(BaseConfigEnum.menual);
            EntityTool.setUpdateStatus(defaultConfig);
            MetaDaoHelper.update(AutoConfig.ENTITY_NAME, defaultConfig);
        }
        QuerySchema newQuerySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup newGroup = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        querySchema.addCondition(newGroup);
        List<AutoConfig> newAutoConfigList = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, newQuerySchema);
        if (CollectionUtils.isNotEmpty(newAutoConfigList)) {
            AutoConfig newConfig = new AutoConfig();
            newConfig.init(newAutoConfigList.get(0));
            newConfig.setIsDefault(BaseConfigEnum.auto);
            EntityTool.setUpdateStatus(newConfig);
            MetaDaoHelper.update(AutoConfig.ENTITY_NAME, newConfig);
        }
        return JSONBuilderUtils.createJson();
    }

    @Override
    public CtmJSONObject getCheckUkey(CtmJSONObject param, boolean isNewLogic) throws Exception {
        //获取前端拼接的accEntity数组
        CtmJSONObject checkUkey = new CtmJSONObject();
        checkUkey.put("checkUkey", true);
        String accEntity = param.getString("accEntity");
        List<String> accentitys = new ArrayList<>();
        if (accEntity != null) {
            //判断前段数据是否为多选
            if (accEntity.contains("[")) {
                // Set可以去重，避免重复计数问题
                Set<String> tempSet = new HashSet<>(param.getObject("accEntity", List.class));
                accentitys = new ArrayList<>(tempSet);
            } else
                accentitys.add(accEntity);
        }
        if (accentitys != null && !accentitys.isEmpty()) {
            QuerySchema querySchema = QuerySchema.create().addSelect("checkUkey");
            QueryConditionGroup newGroup = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).in(accentitys));
            querySchema.addCondition(newGroup);
            List<AutoConfig> checkUkeyList = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema);
            //若结果中有一个为true 则需要校验ukey，当全为false时 才不校验，有一个需要校验 则都会校验
            checkUkey.put("checkUkey", false);
            if (checkUkeyList.size() > 0) {
                for (HashMap autoConfig : checkUkeyList) {
                    if (isNewLogic) {
                        // 新逻辑: 现金基础参数没有设置的时候，默认为是，弹出ukey校验
                        if (autoConfig.get("checkUkey") == null || (Boolean) autoConfig.get("checkUkey")) {
                            checkUkey.put("checkUkey", true);
                            break;
                        }
                    } else {
                        if (autoConfig.get("checkUkey") != null && (Boolean) autoConfig.get("checkUkey")) {
                            checkUkey.put("checkUkey", true);
                            break;
                        }
                    }
                }
            } else {
                checkUkey.put("checkUkey", true);
            }
            //如果传入的会计主体数量 比查询结果多，说明有的会计主体没有录入现金参数 这时返回true
            if (checkUkeyList != null && accentitys.size() > checkUkeyList.size()) {
                if (isNewLogic) {
                    checkUkey.put("checkUkey", true);
                } else {
                    checkUkey.put("checkUkey", false);
                }
            }
        }
        return checkUkey;
    }

    @Override
    public Boolean getCheckFundTransfer() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        if (query.size() > 0) {
            Boolean checkFundTransfer = false;
            //适配达梦库
            if (query.get(0).get("checkFundTransfer") != null) {
                if ("0".equals(query.get(0).get("checkFundTransfer").toString()) ||
                        "false".equals(query.get(0).get("checkFundTransfer").toString())) {
                    checkFundTransfer = false;
                } else {
                    checkFundTransfer = true;
                }
            }
            return checkFundTransfer;
        }
        return false;
    }

    @Override
    public Boolean getCheckBalanceIsQuery(String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        if (query.size() > 0) {
            Boolean checkBalanceIsQuery = query.get(0).get("checkBalanceIsQuery").toString().equals("true");
            return checkBalanceIsQuery;
        }
        return false;
    }

    @Override
    public String getQueryBillType(String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        if (query.size() > 0) {
            if (query.get(0).get("queryBillType") == null) {
                return null;
            }
            String queryBillType = query.get(0).get("queryBillType").toString();
            return queryBillType;
        }
        return null;
    }

    @Override
    public Boolean getCheckFundTransferForAssociation() throws Exception {
        String ymsCheckFundTransfer = AppContext.getEnvConfig("cmp.autoconfig.checkFundTransfer", "");
        if (!StringUtils.isEmpty(ymsCheckFundTransfer)) {
            return Boolean.valueOf(ymsCheckFundTransfer);
        }
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        if (query.size() > 0) {
            Boolean checkFundTransfer = false;
            //适配达梦库
            if (query.get(0).get("checkFundTransfer") != null) {
                if ("0".equals(query.get(0).get("checkFundTransfer").toString()) ||
                        "false".equals(query.get(0).get("checkFundTransfer").toString())) {
                    checkFundTransfer = false;
                } else {
                    checkFundTransfer = true;
                }
            }
            return checkFundTransfer;
        }
        return false;
    }


    @Override
    public Boolean getCheckStockIsUse() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, schema, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getCheckStockIsUse() == null ? Boolean.FALSE : configList.get(0).getCheckStockIsUse();
        }
    }

    @Override
    public List<Map<String, Object>> getGlobalConfig() throws Exception {
        //获取现金参数-转账单是否推送结算状态
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
		List<Map<String, Object>> result = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
		//是否开启简强
		boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
		if(result.size() > 0){
			result.get(0).put("simplifyEnable", enableSimplify);
		}else {
			result = new ArrayList<>();
			Map<String, Object> map = new HashMap<>();
			map.put("simplifyEnable", enableSimplify);
			result.add(map);
		}
		return result;
    }

    @Override
    public AutoConfig getGlobalConfigEntity() throws Exception {
        //获取现金参数-转账单是否推送结算状态
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, schema, null);
        if (configList.size() > 0) {
            return configList.get(0);
        }
        return null;
    }


    @Override
    public Boolean getIsRecheck() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<AutoConfig> autoConfigList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, schema, null);
        log.error("getIsRecheck autoConfigList:{}", CtmJSONObject.toJSONString(autoConfigList));
        if (!autoConfigList.isEmpty()) {
            if (autoConfigList.get(0).getIsrecheck() == null) {
                return false;
            }
            return autoConfigList.get(0).getIsrecheck();
        }
        return false;
    }

    //获取认领时是否启用资金中心代理模式 参数是否启用
    @Override
    public Boolean getEnableBizDelegationMode() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getIsEnableBizDelegationMode() == null ? Boolean.FALSE : configList.get(0).getIsEnableBizDelegationMode();
        }
    }

    //认领时是否启用统收统支模式 参数是否启用
    @Override
    public Boolean getUnifiedIEModelWhenClaim() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getIsUnifiedIEModelWhenClaim() == null ? Boolean.FALSE : configList.get(0).getIsUnifiedIEModelWhenClaim();
        }
    }

    /**
     * 根据会计主体查询现金参数
     *
     * @param accentity
     * @return
     */
    public AutoConfig queryAutoConfigByAccentity(String accentity) throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq(accentity));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList.size() > 0) {
            return configList.get(0);
        }
        return null;
    }

    /**
     * 根据会计主体id查询现金参数
     *
     * @param accentity
     * @return
     */
    public AutoConfig getAutoConfigByAcc(String accentity) throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList.size() > 0) {
            return configList.get(0);
        }
        return null;
    }

    // 银企直连账户的银行对账单是否允许维护 参数是否启用  默认是true
    @Override
    public Boolean getBankreconciliationCanUpdate() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.TRUE;
        } else {
            return configList.get(0).getIsBankreconciliationCanUpdate() == null ? Boolean.TRUE : configList.get(0).getIsBankreconciliationCanUpdate();
        }
    }

    /**
     * 支票是否需要领用
     *
     * @return
     * @throws Exception
     */
    @Override
    public Boolean getCheckStockCanUse() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getCheckStockIsUse() == null ? Boolean.FALSE : configList.get(0).getCheckStockIsUse();
        }
    }

    /**
     * 是否推送历史数据
     *
     * @return
     * @throws Exception
     */
    @Override
    public Boolean isPushHistory() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getIsPushHistoryData() == null ? Boolean.FALSE : configList.get(0).getIsPushHistoryData();
        }
    }

    /**
     * 获取当前租户下，现金参数配置无需处理的组织列表
     *
     * @return
     * @throws Exception
     */
    @Override
    public List<String> getAccentityListNoProecss() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("accentity");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("isNoProcess").eq("0"));
        querySchema.addCondition(conditionGroup);
        List<AutoConfig> accentityList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(accentityList)) {
            return accentityList.stream().map(AutoConfig::getAccentity).map(Object::toString).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 获取企业银行账号级-日记账余额排序规则
     * @return
     * @throws Exception
     */
    @Override
    public Short getJournalBalanceSortRule() throws Exception {
        QuerySchema querySchema1 = QuerySchema.create().addSelect("id,journalBalanceSortRule");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        querySchema1.addCondition(conditionGroup);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (CollectionUtils.isEmpty(configList)) {
            return JournalBalanceSortRuleEnum.DZ_TIME.getValue();
        } else {
            return configList.get(0).getJournalBalanceSortRule() == null ? JournalBalanceSortRuleEnum.DZ_TIME.getValue()
                                : configList.get(0).getJournalBalanceSortRule();
        }
    }
}
