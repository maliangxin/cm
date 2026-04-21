package com.yonyoucloud.fi.cmp.autoparam.rule;

import com.google.common.base.Strings;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.FundOrgService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.AutoCheckUtil;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 现金参数保存前规则
 */
@Component
@Slf4j
public class BeforeSaveAutoConfigRule extends AbstractCommonRule {
    @Autowired
    private IFIBillService fiBillService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private static final String SETTLEFLAG = "settleflag";
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    private final String TRANSFERACCOUNTLISTMAPPER = "com.yonyoucloud.fi.cmp.mapper.TransferAccountMapper.";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isNotEmpty(bills)) {
            BizObject bizObject = bills.get(0);
            //判断是否是企业级账号
            String accentityFromRequest = (String) CtmJSONObject.parseObject(paramMap.get("requestData").toString()).get("accentity");
            if(Strings.isNullOrEmpty(accentityFromRequest)){
                accentityFromRequest = bizObject.getString("accentity");
            }
            Boolean globalAccentity = accentityFromRequest.equals("666666");
            if (null != (CtmJSONObject.parseObject(paramMap.get("requestData").toString()).get("checkFundTransfer"))) {
                Boolean checkFundTransfer = Boolean.parseBoolean(CtmJSONObject.parseObject(paramMap.get("requestData").toString()).get("checkFundTransfer").toString());
                List<Map<String, Object>> list = getFlag();
                if (CollectionUtils.isNotEmpty(list)) {
                    Boolean bool = false;
                    if (list.get(0).get("checkFundTransfer") != null) {
                        if ("0".equals(list.get(0).get("checkFundTransfer").toString()) ||
                                "false".equals(list.get(0).get("checkFundTransfer").toString())) {
                            bool = false;
                        } else {
                            bool = true;
                        }
                    }
//                Boolean bool = list.get(0).get("checkFundTransfer").equals(0) ? false : true;
                    if (!checkFundTransfer.equals(bool)) {
                        //是企业账号级，并且是否推送为true
                        if (globalAccentity && checkFundTransfer) {
                            Boolean one = checkDocumentOne();
                            if (false == one) {
//                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102044"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1713352554276454568"));
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FFA1B6E04F80007", "同名账户划转存在在途单据，请处理完成后再开启此参数！") /* "转账工作台存在在途单据，请处理完成后再开启此参数！" */);
                            }
                            //是企业账号级，并且是否推送为false
                        } else if (globalAccentity && (false == checkFundTransfer)) {
                            Boolean two = checkDocumentTwo();
                            if (false == two) {
//                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102046"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129021"));
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FFA1C1E04F80002", "同名账户划转存在在途单据，请处理完成后再关闭此参数！") /* "转账工作台存在在途单据，请处理完成后再关闭此参数！" */);
                            }
                        }
                    }
                }
            }

            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102048"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026058") /* "传入的billnum为空，请检查" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102049"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000026058", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
            String accentity = accentityFromRequest;
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
            if (query.size() > 0) {
                if (globalAccentity) {
                    bizObject.set("checkFundPlan", null);
                    bizObject.set("isGenerateFundCollection", null);
                    bizObject.set("receiveQuickType", null);
                    bizObject.set("payQuickType", null);
                    bizObject.set("settlemode", null);
                    bizObject.set("autoassociateconfirm", null);
                    bizObject.set("autogenerateconfirm", null);
                    bizObject.set("checkUkey", null);
                    bizObject.set("checkDailySettlement", null);
                    bizObject.set("isShareVideo", null);
                    //modify by lichaor  20250520 租户级参数也有“单据生成事项分录时机”这个参数
                    //bizObject.set("isSettleSuccessToPost", null);
                    bizObject.set("billCheck", null);
                    bizObject.set("billCheckFlag", null);
                    bizObject.set("billCheckObject", null);
                }
                bizObject.set("id", query.get(0).get("id"));
                bizObject.set("_status", EntityStatus.Update);
                if (!(bizObject.getBoolean("checkDailySettlement") == query.get(0).get("checkDailySettlement"))) {
                    if (!getCheckDailySettlement(accentity)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102050"), MessageUtils.getMessage("P_YS_CTM_CM-BE_0001482904") /* "已日结的日期存在未审核的单据，请先处理！" */);
                    }
                }
                //参数修改时的校验条件：如果系统中已经存在“支票状态=已领用”的数据，则不支持修改参数，保存时系统提示“已经存在“已领用状态”的重空凭证，不支持修改重空凭证参数”
                if (!(bizObject.getBoolean("checkStockIsUse") == query.get(0).get("checkStockIsUse"))) {
                    if (globalAccentity && !getCheckStock(accentity)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102051"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA8C1D004F00003", "已经存在“已领用”状态的重空凭证，不支持修改重空凭证参数") /* "已经存在“已领用”状态的重空凭证，不支持修改重空凭证参数" */);
                    }
                }
            } else {
                bizObject.set("id", ymsOidGenerator.nextId());
                bizObject.set("_status", EntityStatus.Insert);
            }
            //修改现金基础参数增加业务日志
            try {
                CtmJSONObject jsonObject = new CtmJSONObject();
                jsonObject.put("autoconfig", bills);
                /*ctmcmpBusinessLogService.saveBusinessLog(jsonObject, "", "", IServicecodeConstant.AUTOCONFIG,
                        IMsgConstant.CMP_AUTOCONFIG_UPDATE, IServicecodeConstant.AUTOCONFIG);*/
                if ("cmdSaveSync".equals(billContext.getParameter("cmdname"))) {
                    syncSub(bills.get(0), bills, billContext);
                    ctmcmpBusinessLogService.saveBusinessLog(jsonObject, "", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AB", "现金基础参数") /* "现金基础参数" */, IServicecodeConstant.AUTOCONFIG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AB", "现金基础参数") /* "现金基础参数" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AC", "保存并同步下级") /* "保存并同步下级" */);
                } else {
                    ctmcmpBusinessLogService.saveBusinessLog(jsonObject, "", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AB", "现金基础参数") /* "现金基础参数" */, IServicecodeConstant.AUTOCONFIG, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AB", "现金基础参数") /* "现金基础参数" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AD", "保存") /* "保存" */);
                }
            } catch (Exception e) {
                log.info("============= update autoconfig error ctmcmpBusinessLogService：" + e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }

    private void syncSub(BizObject bizObject, List<BizObject> bills, BillContext billContext) throws Exception {
        List<String> subOrgIds;
        /*if (bizObject.getBoolean("syncall") == true) {
            List<BaseOrgDTO> queryAccList = FundOrgService.getAllFundOrgsTree();
            queryAccList = AutoParamCommonUtil.filterOrgPermission(queryAccList, billContext);//筛选有权限的组织
            subOrgIds = queryAccList.stream().map(BaseOrgDTO::getId).collect(Collectors.toList());
        }*/
        /*if (bizObject.getBoolean("syncson") == true) {
            //查下级会计主体
            subOrgIds = FundOrgService.getChildFundOrg(bizObject.get("accentity"));
            subOrgIds.remove(bizObject.get("accentity"));
        }*/
        subOrgIds = FundOrgService.getChildFundOrg(bizObject.get("accentity"));
        subOrgIds.remove(bizObject.get("accentity"));
        if (CollectionUtils.isEmpty(subOrgIds)) {
            return;
        }
        bizObject.remove("syncall");
        bizObject.remove("syncson");

        QuerySchema schema = QuerySchema.create();
        schema.addSelect("accentity,id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(subOrgIds));
        schema.addCondition(conditionGroup);
        List<AutoConfig> queryResult = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, schema, null);
        List<String> accentityList = queryResult.stream().map(AutoConfig::getAccentity).collect(Collectors.toList());
        Map<String, Long> accentityToIdMap = queryResult.stream().collect(Collectors.toMap(AutoConfig::getAccentity, AutoConfig::getId));
        for (String orgId : subOrgIds) {
            BizObject cloneParam = bizObject.clone();
            cloneParam.setId(null);
            cloneParam.set("accentity", orgId);
            if (CollectionUtils.isNotEmpty(accentityList) && accentityList.contains(orgId)) {
                cloneParam.set("id", accentityToIdMap.get(orgId));
                cloneParam.set("_status", EntityStatus.Update);
            } else {
                cloneParam.set("id", ymsOidGenerator.nextId());
                cloneParam.set("_status", EntityStatus.Insert);
            }
            //ParamPlugin.handleNewParam(cloneParam);
            cloneParam.remove("resubmitCheckKey");
            cloneParam.remove("pubts");
            bills.add(cloneParam);
        }
    }

    private Boolean getCheckDailySettlement(String accentity) throws Exception {
        //检查组织锁是否存在
        JedisLockUtils.isexistRjLock(accentity);
        YmsLock ymsLock = JedisLockUtils.lockRjWithOutTrace(accentity);
        try {
            QuerySchema querySchemaSettlement = QuerySchema.create().addSelect("max(settlementdate)");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                    QueryCondition.name(SETTLEFLAG).eq(1));
            querySchemaSettlement.addCondition(group);
            Map<String, Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME, querySchemaSettlement);
            if (MapUtils.isNotEmpty(map)) {
                Date maxDateSettlement = (Date) map.get("max");
                return AutoCheckUtil.checkAudit(accentity, maxDateSettlement);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            //释放组织锁
            JedisLockUtils.unlockRjWithOutTrace(ymsLock);
        }
        return true;
    }


    private Boolean getCheckStock(String accentity) throws Exception {
        try {
            QuerySchema queryCheckStockSchema = QuerySchema.create().addSelect("*");
            queryCheckStockSchema.addCondition(QueryConditionGroup.and(
                    //QueryCondition.name("accentity").eq(accentity),
                    QueryCondition.name("checkBillStatus").eq(CmpCheckStatus.Use.getValue())));
            List<Map<String, Object>> checkStocks = MetaDaoHelper.query(CheckStock.ENTITY_NAME, queryCheckStockSchema);
            if (CollectionUtils.isNotEmpty(checkStocks)) {
                return false;
            }
        } catch (Exception e) {
            throw e;
        }
        return true;
    }


    private Boolean checkAccount(String accentityId) throws Exception {
        List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentityId}));
        Object code = accentityObj != null && !accentityObj.isEmpty() ? accentityObj.get(0).get("code") : "";
        if ((code.toString()).equals("global00")) {
            return true;
        }
        return false;
    }

    /**
     * 在途单据状态校验
     *
     * @return
     * @throws Exception
     */
    public Boolean checkDocumentOne() throws Exception {
        //todo 这个地方缺少的有审核状态
        //如果在途单户状态全走完了，返回true
        //查询转账工作台列表
        Map param = new HashMap();
        param.put("tenantid", AppContext.getTenantId());
        List<Map<String, Object>> list = SqlHelper.selectList(TRANSFERACCOUNTLISTMAPPER + "getTransferAccountListOne", param);
        if (list.size() > 0) {
            return false;
        }
        return true;
    }

    public Boolean checkDocumentTwo() throws Exception {
        //todo 这个地方缺少的有审核状态
        //如果在途单户状态全走完了，返回true
        //查询转账工作台列表
        Map param = new HashMap();
        param.put("tenantid", AppContext.getTenantId());
        List<Map<String, Object>> list = SqlHelper.selectList(TRANSFERACCOUNTLISTMAPPER + "getTransferAccountListTwo", param);
        if (list.size() > 0) {
            return false;
        }
        return true;
    }

    public List<Map<String, Object>> getFlag() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getTenantId()));
        conditionGroup.appendCondition(QueryCondition.name("accentity.code").eq("global00"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        return query;
    }

}
