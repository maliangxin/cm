package com.yonyoucloud.fi.cmp.bankidentify;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ruleengine.dto.relevant.TargetRuleInfoDto;
import com.yonyou.iuap.ruleengine.relevant.RelevantRuleLoadService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.cmpentity.BankIdentifyTypeEnum;
import com.yonyoucloud.fi.cmp.cmpentity.DcFlagEnum;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting_b;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.ApplyObjectNewEnum;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingInnerService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:流水辨识规则相关处理接口具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2024/7/5 9:28
 */

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class BankIdentifyServiceImpl implements BankIdentifyService{
    private static final String GLOBAL_ACCENTITY = "666666";
    @Autowired
    public RelevantRuleLoadService relevantRuleLoadService;
    @Autowired
    private IFlowhandlesettingInnerService flowhandlesettingInnerService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    private IFcdsUseSettingInnerService fcdsUseSettingInnerService;

    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;

    /**
     * 更新辨识匹配规则类型和辨识匹配规则设置的启停用状态
     * @param param type=1规则启停用；type=2规则配置详情启停用
     * id:单据id；enablestatus修改后的启用状态
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject updateStatus(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        Integer type = param.getInteger("type");
        Short enablestatus = param.getShort("enablestatus");
        Object id = param.get("id");
        if(type == null || enablestatus == null || id == null){
            throw new CtmException(new CtmErrorCode("033-502-102392"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F7", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F7", "入参不全！") /* "入参不全！" */) /* "入参不全！" */);
        }

        //BankreconciliationIdentifyType
        if (type == 1){
            BankreconciliationIdentifyType identifyType = MetaDaoHelper.findById(BankreconciliationIdentifyType.ENTITY_NAME,param.get("id"));
            if (EnableStatus.Enabled.getValue() == enablestatus){
                identifyType.setEnabledate(new Date());
                identifyType.setStopdate(null);
            }
            if (EnableStatus.Disabled.getValue() == enablestatus){
                identifyType.setEnabledate(null);
                identifyType.setStopdate(new Date());
            }
            identifyType.setEnablestatus(enablestatus);
            identifyType.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BankreconciliationIdentifyType.ENTITY_NAME,identifyType);
        }

        //BankreconciliationIdentifySetting
        if (type == 2){
            BankreconciliationIdentifySetting identifySetting = MetaDaoHelper.findById(BankreconciliationIdentifySetting.ENTITY_NAME,param.get("id"));
            identifySetting.setEnablestatus(enablestatus);
            if (EnableStatus.Enabled.getValue() == enablestatus){
                identifySetting.setEnabledate(new Date());
                identifySetting.setStopdate(null);
                identifySetting.setEnable_user(InvocationInfoProxy.getUserid());
                identifySetting.setStop_user(null);
            }
            if (EnableStatus.Disabled.getValue() == enablestatus){
                identifySetting.setEnabledate(null);
                identifySetting.setStopdate(new Date());
                identifySetting.setStop_user(InvocationInfoProxy.getUserid());
                identifySetting.setEnable_user(null);
            }
            identifySetting.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BankreconciliationIdentifySetting.ENTITY_NAME,identifySetting);
        }

        result.put("msg","success");
        return result;
    }

    /**
     * 根据业务对象编码查询相关性规则
     * @param param bizObjectCode=业务对象编码
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryRelevantRuleByBizCode(CtmJSONObject param) throws Exception {
        String bizObjectCode = param.getString("bizObjectCode");
        //业务对象为空时，默认银行对账单
        if (StringUtils.isEmpty(bizObjectCode)){
            bizObjectCode = "ctm-cmp.cmp_bankreconciliation";
        }
        CtmJSONArray enumArray = new CtmJSONArray();
        CtmJSONObject cEnumString = new CtmJSONObject();
        Map<Object, TargetRuleInfoDto> ruleInfoMap = relevantRuleLoadService.loadByBizObjectCode(bizObjectCode, InvocationInfoProxy.getTenantid());
        for (TargetRuleInfoDto ruleInfoDto : ruleInfoMap.values()) {
            CtmJSONObject map = new CtmJSONObject();
            String name = null;
            String locale = InvocationInfoProxy.getLocale();
            if (ruleInfoDto.getName() !=null && ruleInfoDto.getName().contains("zh_CN")){
                switch (locale) {
                    case "zh_CN":
                        name = CtmJSONObject.parseObject(ruleInfoDto.getName()).getString("zh_CN");
                        break;
                    case "en_US":
                        name = CtmJSONObject.parseObject(ruleInfoDto.getName()).getString("en_US");
                        break;
                    case "zh_TW":
                        name = CtmJSONObject.parseObject(ruleInfoDto.getName()).getString("zh_TW");
                        break;
                    default:
                        name = CtmJSONObject.parseObject(ruleInfoDto.getName()).getString("zh_CN");
                }
            }else {
                name = ruleInfoDto.getName();
            }
            map.put("value", ruleInfoDto.getCode());
            map.put("name", name);
            map.put("key", ruleInfoDto.getId().toString());
            enumArray.add(map);
            cEnumString.put(ruleInfoDto.getId().toString(), ruleInfoDto.getCode());
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("enumArray", enumArray);
        result.put("cEnumString", cEnumString);
        return result;
    }

    /**
     * 前端重新排序接口
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject sortIdentifyTypeExcuteOrder(CtmJSONObject param) throws Exception {
        CtmJSONArray recordes = param.getJSONArray("record");
        List<BankreconciliationIdentifyType> updateList = new ArrayList<>();
        for (int i = 0; i < recordes.size(); i++) {
            BankreconciliationIdentifyType  identifyType = recordes.getObject(i,BankreconciliationIdentifyType.class);
            identifyType.setExcuteorder(i + 1);
            identifyType.setEntityStatus(EntityStatus.Update);
            //id需要转化格式
            identifyType.setId(Long.parseLong(identifyType.getId().toString()));
            updateList.add(identifyType);
        }
        MetaDaoHelper.update(BankreconciliationIdentifyType.ENTITY_NAME,updateList);
        CtmJSONObject result = new CtmJSONObject();
        result.put("msg","success");
        return result;
    }

    @Override
    public CtmJSONObject initIdentifyTypeData(String remark , Tenant tenant) throws Exception {
        //预置数据时，若是传递的是2，则只初始化数据源，若传递的是0则都数据源和元数据都预置，若不传或者传1，只预置辨识规则数据
        if ("2".equals(remark)){
            fcdsUseSettingInnerService.dataSync();
            CtmJSONObject c = new CtmJSONObject();
            c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076C", "租户数据预置成功") /* "租户数据预置成功" */);
            return c;
        }
        if ("3".equals(remark)){
            flowhandlesettingInnerService.initTenantData(tenant);
            CtmJSONObject c = new CtmJSONObject();
            c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076E", "流水处理规则预置成功") /* "流水处理规则预置成功" */);
            return c;
        }

        if ("0".equals(remark)){
            fcdsUseSettingInnerService.dataSync();
            flowhandlesettingInnerService.initTenantData(tenant);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        //SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteIdentifyTypeData", map);
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.queryIdentifyTypeInitData", map);
        if(count != null && count == 0){
            map.put("system001",ymsOidGenerator.nextId());
            map.put("system002",ymsOidGenerator.nextId());
            map.put("system003",ymsOidGenerator.nextId());
            map.put("system004",ymsOidGenerator.nextId());
            map.put("system005",ymsOidGenerator.nextId());
            map.put("system006",ymsOidGenerator.nextId());
            map.put("system007",ymsOidGenerator.nextId());
            map.put("system008",ymsOidGenerator.nextId());
            map.put("system009",ymsOidGenerator.nextId());
            map.put("system010",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initIdentifyTypeData", map);
        }
        //初始化，发布对象辨识具体匹配规则数据
        initSystem008Data(tenant);
        //对方信息匹配
        initSystem004Data(tenant);
        //initSystem004DataNew(tenant);
        //收付单据匹配初始化数据
        initSystem005DataNew(tenant);
        //初始化退票匹配
        CtmJSONObject param = new CtmJSONObject();
        //是否本方账号相同
        param.put("accentityflag",1);
        //是否币种相同
        param.put("currencyflag",1);
        //是否金额相同
        param.put("amountflag",1);
        //是否借贷方向相反
        param.put("directionflag",2);
        //本方银行账号
        param.put("accountflag",1);
        //是否对方账号相同
        param.put("toaccountflag",1);
        //对方户名
        param.put("toaccountnameflag",0);
        //摘要匹配方式
        param.put("remarkmatch",0);
        //对方单位类型
        param.put("oppositetypeflag",0);
        //银行类别
        param.put("banktype",null);
        //日期范围
        param.put("daterange",1);
        refundAutoCheckRuleService.updateRuleInfo(param);


        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076C", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }


    @Override
    public void deleteInitData(Tenant tenant) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystemAllMainData", map);
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystemAllItemData", map);
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteIdentifyTypeAllData", map);
    }


    @Override
    public CtmJSONObject btwInitSystem004ItemDataNew(Tenant tenant) throws Exception {
        initSystem004DataForDelete(tenant);
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076C", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }

    @Override
    public CtmJSONObject btwInitSystem005ItemDataNew(Tenant tenant) throws Exception {
        btwInitSystem005ItemDataForDelete(tenant);
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076C", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }

    @Override
    public BankreconciliationIdentifyType queryIdentifyTypeByCode(String code) throws Exception {
        if (StringUtils.isEmpty(code)){
            return null;
        }
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("code").eq(code));
        querySchema.addCondition(queryConditionGroup);
        List<BankreconciliationIdentifyType> list =  MetaDaoHelper.queryObject(BankreconciliationIdentifyType.ENTITY_NAME, querySchema, null);
        if (!CollectionUtils.isEmpty(list)){
            return list.get(0);
        }
        return null;
    }

    @Override
    public BankreconciliationIdentifyType queryIdentifyTypeByType(Short type) throws Exception {
        if (type == null){
            return null;
        }
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("identifytype").eq(type));
        querySchema.addCondition(queryConditionGroup);
        List<BankreconciliationIdentifyType> list =  MetaDaoHelper.queryObject(BankreconciliationIdentifyType.ENTITY_NAME, querySchema, null);
        if (!CollectionUtils.isEmpty(list)){
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<BankreconciliationIdentifySetting> querySettingsByCode(String code) throws Exception {
        if (StringUtils.isEmpty(code)){
            return null;
        }
        BankreconciliationIdentifyType identifyType = queryIdentifyTypeByCode(code);
        if (identifyType != null){
            QuerySchema querySchema = new QuerySchema().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
            queryConditionGroup.addCondition(
                    QueryCondition.name("identifytype").eq(identifyType.getIdentifytype()),
                    QueryCondition.name("dr").eq(0)
            );
            querySchema.addCondition(queryConditionGroup);
            List<BankreconciliationIdentifySetting> list =  MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME, querySchema, null);
            if (!CollectionUtils.isEmpty(list)){
                return list;
            }
        }
        return null;
    }

    @Override
    public Map<Integer, TargetRuleInfoDto> loadRuleBySettingId(String settingId)  throws Exception{
        Map<Integer, TargetRuleInfoDto> map = new HashMap<>();
        if (StringUtils.isEmpty(settingId)){
            return map;
        }
        BankreconciliationIdentifySetting setting = MetaDaoHelper.findById(BankreconciliationIdentifySetting.ENTITY_NAME, settingId,3);
        List<BankreconciliationIdentifySetting_b> setting_bList = setting.BankreconciliationIdentifySetting_b();
        if (!CollectionUtils.isEmpty(setting_bList)){
            List<BankreconciliationIdentifySetting_b> bList = setting_bList.stream().filter(item -> item.getDr()==0).collect(Collectors.toList());
            for (BankreconciliationIdentifySetting_b setting_b : bList){
                if (!StringUtils.isEmpty(setting_b.getIdentifyruleid())){
                    TargetRuleInfoDto dto = relevantRuleLoadService.loadById(setting_b.getIdentifyruleid());
                    if (dto != null){
                        map.put(setting_b.getBigDecimal("lineno").intValue(),dto);
                    }
                }
            }
        }
        return map;
    }

    @Override
    public Map<String, BankreconciliationIdentifySetting_b> loadRuleBySetting_bById(String settingId)  throws Exception{
        Map<String, BankreconciliationIdentifySetting_b> map = new HashMap<>();
        if (StringUtils.isEmpty(settingId)){
            return map;
        }
        BankreconciliationIdentifySetting setting = MetaDaoHelper.findById(BankreconciliationIdentifySetting.ENTITY_NAME, settingId,3);
        List<BankreconciliationIdentifySetting_b> setting_bList = setting.BankreconciliationIdentifySetting_b();
        if (!CollectionUtils.isEmpty(setting_bList)){
            List<BankreconciliationIdentifySetting_b> bList = setting_bList.stream().filter(item -> item.getDr()==0).collect(Collectors.toList());
            for (BankreconciliationIdentifySetting_b setting_b : bList){
                if (!StringUtils.isEmpty(setting_b.getIdentifyruleid())){
                    if (setting_b.getDescription() !=null && setting_b.getDescription().contains(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540076D", "脚本") /* "脚本" */)){
                        TargetRuleInfoDto dto = relevantRuleLoadService.loadById(setting_b.getIdentifyruleid());
                        map.put(dto.getCode(),setting_b);
                    }
                }
            }
        }
        return map;
    }
    @Override
    public Map<String, BankreconciliationIdentifySetting_b> loadBankreconciliationIdentifySetting_bById(String settingId) throws Exception{
        Map<String, BankreconciliationIdentifySetting_b> map = new HashMap<>();
        if (StringUtils.isEmpty(settingId)){
            return map;
        }
        BankreconciliationIdentifySetting setting = MetaDaoHelper.findById(BankreconciliationIdentifySetting.ENTITY_NAME, settingId,3);
        List<BankreconciliationIdentifySetting_b> setting_bList = setting.BankreconciliationIdentifySetting_b();
        if (!CollectionUtils.isEmpty(setting_bList)){
            List<BankreconciliationIdentifySetting_b> bList = setting_bList.stream().filter(item -> item.getDr()==0).collect(Collectors.toList());
            for (BankreconciliationIdentifySetting_b setting_b : bList){
                map.put(setting_b.getApplyfield(),setting_b);
            }
        }
        return map;
    }

    @Override
    public CtmJSONObject updateSettingStatus(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();

        Short enablestatus = param.getShort("enablestatus");
        Object id = param.get("id");
        if(enablestatus == null || id == null){
            throw new CtmException(new CtmErrorCode("033-502-102392"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F7", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F7", "入参不全！") /* "入参不全！" */) /* "入参不全！" */);
        }
        BankreconciliationIdentifySetting bankreconciliationIdentifySetting = MetaDaoHelper.findById(BankreconciliationIdentifySetting.ENTITY_NAME,id);
        if (EnableStatus.Enabled.getValue() == enablestatus){
            bankreconciliationIdentifySetting.setEnabledate(new Date());
            bankreconciliationIdentifySetting.setEnable_user(InvocationInfoProxy.getUserid());
            bankreconciliationIdentifySetting.setStopdate(null);
            bankreconciliationIdentifySetting.setStop_user(null);
        }
        if (EnableStatus.Disabled.getValue() == enablestatus){
            bankreconciliationIdentifySetting.setEnabledate(null);
            bankreconciliationIdentifySetting.setEnable_user(null);
            bankreconciliationIdentifySetting.setStopdate(new Date());
            bankreconciliationIdentifySetting.setStop_user(InvocationInfoProxy.getUserid());
        }
        bankreconciliationIdentifySetting.setEnablestatus(enablestatus);
        bankreconciliationIdentifySetting.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(BankreconciliationIdentifySetting.ENTITY_NAME,bankreconciliationIdentifySetting);
        return result;
    }

    @Override
    public List<BankreconciliationIdentifySetting> querySettingsByOrg(String org, String dcFlag) throws Exception {
        //预置数据
        List<BankreconciliationIdentifySetting> systemSettings = this.querySystemSettings(dcFlag);

        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("identifytype").eq(5));
        group.addCondition(QueryCondition.name("enablestatus").eq(EnableStatus.Enabled.getValue()));
        group.addCondition(QueryCondition.name("issystem").eq(0));
        group.addCondition(QueryCondition.name("dc_flag").eq(dcFlag));

        QuerySchema querySchema = new QuerySchema().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).addCondition(group);
        List<BankreconciliationIdentifySetting> tenantSettings = MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME, querySchema, null);
        if(CollectionUtils.isNotEmpty(tenantSettings)){
            //组织级数据
            List<BankreconciliationIdentifySetting> orgSettings = tenantSettings.stream().filter(item -> !StringUtils.isEmpty(item.getAccentity()) && Arrays.asList(item.getAccentity().split(",")).contains(org)).sorted(Comparator.comparing(BankreconciliationIdentifySetting::getExcutelevel)).collect(Collectors.toList());
            //企业账号级数据
            List<BankreconciliationIdentifySetting> globalOrgSettings = tenantSettings.stream().filter(item -> !StringUtils.isEmpty(item.getAccentity()) && GLOBAL_ACCENTITY.equals(item.getAccentity())).sorted(Comparator.comparing(BankreconciliationIdentifySetting::getExcutelevel)).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(orgSettings)){
                systemSettings.addAll(orgSettings);
            }
            if(CollectionUtils.isEmpty(globalOrgSettings)){
                systemSettings.addAll(globalOrgSettings);
            }
        }
        List<Object> ids = systemSettings.stream().map(BankreconciliationIdentifySetting::getId).collect(Collectors.toList());
        QuerySchema childQuerySchema = new QuerySchema().addSelect(ICmpConstant.SELECT_TOTAL_PARAM)
                .appendQueryCondition(
                        QueryCondition.name("mainid").in(ids),
                        QueryCondition.name("enablestatus").eq(EnableStatus.Enabled.getValue()))
                .addOrderBy("lineno");
        List<BankreconciliationIdentifySetting_b> children = MetaDaoHelper.queryObject(BankreconciliationIdentifySetting_b.ENTITY_NAME, childQuerySchema, null);
        if(CollectionUtils.isNotEmpty(children)){
            Map<Long,List<BankreconciliationIdentifySetting_b>> childMap = children.stream().collect(Collectors.groupingBy(BankreconciliationIdentifySetting_b::getMainid));
            systemSettings.forEach(item -> {
                if(childMap.containsKey(item.getLong("id"))){
                    item.setBankreconciliationIdentifySetting_b(childMap.get(item.getLong("id")));
                }
            });
        }
        return systemSettings;
    }

    private List<BankreconciliationIdentifySetting> querySystemSettings(String dcFlag) throws Exception {
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("identifytype").eq(5));
        group.addCondition(QueryCondition.name("enablestatus").eq(EnableStatus.Enabled.getValue()));
        group.addCondition(QueryCondition.name("issystem").eq(1));
        group.addCondition(QueryCondition.name("dc_flag").eq(dcFlag));
        QuerySchema querySchema = new QuerySchema().addSelect(ICmpConstant.SELECT_TOTAL_PARAM)
                .addCondition(group)
                .addOrderBy("excutelevel");
        return MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME, querySchema, null);
    }

    /**
     * 初始化system008,发布对象辨识初始化数据
     * @param tenant
     */
    private void initSystem008Data(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        //SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem008ItemData", map);
        //SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem008MainData", map);
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem008InitData", map);
        if(count != null && count == 0){
            map.put("mainid",ymsOidGenerator.nextId());
            map.put("system0081",ymsOidGenerator.nextId());
            map.put("system0082",ymsOidGenerator.nextId());
            map.put("system0083",ymsOidGenerator.nextId());
            map.put("system0084",ymsOidGenerator.nextId());
            map.put("system0085",ymsOidGenerator.nextId());
            map.put("system0802",ymsOidGenerator.nextId());

            //生单类型辨识
            map.put("system0601",ymsOidGenerator.nextId());
            map.put("system06011",ymsOidGenerator.nextId());
            map.put("system0602",ymsOidGenerator.nextId());
            map.put("system06021",ymsOidGenerator.nextId());
            //挂账辨识
            map.put("system0009",ymsOidGenerator.nextId());
            map.put("system00091",ymsOidGenerator.nextId());
            //其他信息辨识
            map.put("system0010",ymsOidGenerator.nextId());
            map.put("system00101",ymsOidGenerator.nextId());
            map.put("system0011",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem008MainData", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem008ItemData", map);
        }
    }

    /**
     * 初始化system005,收付单据匹配初始化数据
     * @param tenant
     */
    private void initSystem005Data(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem005InitData", map);
        if(count != null && count == 0){
            map.put("system0511",ymsOidGenerator.nextId());
            map.put("system0521",ymsOidGenerator.nextId());
            map.put("system051101",ymsOidGenerator.nextId());
            map.put("system051102",ymsOidGenerator.nextId());
            map.put("system051103",ymsOidGenerator.nextId());
            map.put("system051104",ymsOidGenerator.nextId());
            map.put("system051105",ymsOidGenerator.nextId());
            map.put("system051106",ymsOidGenerator.nextId());
            map.put("system051107",ymsOidGenerator.nextId());
            map.put("system051108",ymsOidGenerator.nextId());
            map.put("system051109",ymsOidGenerator.nextId());
            map.put("system051110",ymsOidGenerator.nextId());
            map.put("system052101",ymsOidGenerator.nextId());
            map.put("system052102",ymsOidGenerator.nextId());
            map.put("system052103",ymsOidGenerator.nextId());
            map.put("system052104",ymsOidGenerator.nextId());
            map.put("system052105",ymsOidGenerator.nextId());
            map.put("system052106",ymsOidGenerator.nextId());
            map.put("system052107",ymsOidGenerator.nextId());
            map.put("system052108",ymsOidGenerator.nextId());
            map.put("system052109",ymsOidGenerator.nextId());
            map.put("system052110",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem005MainData", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem005ItemData", map);
        }
    }
    /**
     * 初始化system005,收付单据匹配初始化数据 New
     * @param tenant
     */
    private void initSystem005DataNew(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        //SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem005ItemDataNew", map);
        //SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem005MainDataNew", map);
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem005InitData", map);
        if(count != null && count == 0){
            //收入
            map.put("system0511",ymsOidGenerator.nextId());
            map.put("system0512",ymsOidGenerator.nextId());
            map.put("system0513",ymsOidGenerator.nextId());
            map.put("system0514",ymsOidGenerator.nextId());
            //支出
            map.put("system0521",ymsOidGenerator.nextId());
            map.put("system0522",ymsOidGenerator.nextId());
            map.put("system0523",ymsOidGenerator.nextId());
            map.put("system0524",ymsOidGenerator.nextId());
            //收入子表
            map.put("system051101",ymsOidGenerator.nextId());
            map.put("system051102",ymsOidGenerator.nextId());
            map.put("system051103",ymsOidGenerator.nextId());
            map.put("system051104",ymsOidGenerator.nextId());
            map.put("system051105",ymsOidGenerator.nextId());

            map.put("system051201",ymsOidGenerator.nextId());
            map.put("system051202",ymsOidGenerator.nextId());
            map.put("system051203",ymsOidGenerator.nextId());

            map.put("system051301",ymsOidGenerator.nextId());
            map.put("system051302",ymsOidGenerator.nextId());
            map.put("system051303",ymsOidGenerator.nextId());
            map.put("system051304",ymsOidGenerator.nextId());
            map.put("system051305",ymsOidGenerator.nextId());
            map.put("system051306",ymsOidGenerator.nextId());
            map.put("system051307",ymsOidGenerator.nextId());
            map.put("system051308",ymsOidGenerator.nextId());
            map.put("system051309",ymsOidGenerator.nextId());
            map.put("system051310",ymsOidGenerator.nextId());

            map.put("system051401",ymsOidGenerator.nextId());
            map.put("system051402",ymsOidGenerator.nextId());
            map.put("system051403",ymsOidGenerator.nextId());
            map.put("system051404",ymsOidGenerator.nextId());
            map.put("system051405",ymsOidGenerator.nextId());
            map.put("system051406",ymsOidGenerator.nextId());
            map.put("system051407",ymsOidGenerator.nextId());
            map.put("system051408",ymsOidGenerator.nextId());
            map.put("system051409",ymsOidGenerator.nextId());


            //支出子表
            map.put("system052101",ymsOidGenerator.nextId());
            map.put("system052102",ymsOidGenerator.nextId());
            map.put("system052103",ymsOidGenerator.nextId());
            map.put("system052104",ymsOidGenerator.nextId());
            map.put("system052105",ymsOidGenerator.nextId());

            map.put("system052201",ymsOidGenerator.nextId());
            map.put("system052202",ymsOidGenerator.nextId());
            map.put("system052203",ymsOidGenerator.nextId());

            map.put("system052301",ymsOidGenerator.nextId());
            map.put("system052302",ymsOidGenerator.nextId());
            map.put("system052303",ymsOidGenerator.nextId());
            map.put("system052304",ymsOidGenerator.nextId());
            map.put("system052305",ymsOidGenerator.nextId());
            map.put("system052306",ymsOidGenerator.nextId());
            map.put("system052307",ymsOidGenerator.nextId());
            map.put("system052308",ymsOidGenerator.nextId());
            map.put("system052309",ymsOidGenerator.nextId());
            map.put("system052310",ymsOidGenerator.nextId());

            map.put("system052401",ymsOidGenerator.nextId());
            map.put("system052402",ymsOidGenerator.nextId());
            map.put("system052403",ymsOidGenerator.nextId());
            map.put("system052404",ymsOidGenerator.nextId());
            map.put("system052405",ymsOidGenerator.nextId());
            map.put("system052406",ymsOidGenerator.nextId());
            map.put("system052407",ymsOidGenerator.nextId());
            map.put("system052408",ymsOidGenerator.nextId());
            map.put("system052409",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem005MainDataNew", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem005ItemDataNew", map);
            map.put("flowCode","system0103");
           // SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteFlowHandleSubData", map);
            Long flowId = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.queryFlowhandlesetting", map);
            if(flowId != null){
                map.put("flowId",flowId);
            }else{
                flowhandlesettingInnerService.initTenantData(tenant);
                flowId = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.queryFlowhandlesetting", map);
                map.put("flowId",flowId);
            }

        }

    }

    /**
     * 初始化system004,对方信息匹配初始化数据
     * @param tenant
     */
    private void initSystem004Data(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem004InitData", map);
        if(count != null && count == 0){
            map.put("system0414",ymsOidGenerator.nextId());
            map.put("system0424",ymsOidGenerator.nextId());
            map.put("system04141",ymsOidGenerator.nextId());
            map.put("system04142",ymsOidGenerator.nextId());
            map.put("system04143",ymsOidGenerator.nextId());
            map.put("system04241",ymsOidGenerator.nextId());
            map.put("system04242",ymsOidGenerator.nextId());
            map.put("system04243",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem004MainData", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem004ItemData", map);
        }
    }
    private void initSystem004DataNew(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
//        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem004ItemDataNew", map);
//        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem004MainDataNew", map);
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem004InitData", map);
        if(count != null && count == 0){
            //收入
            map.put("system04101",ymsOidGenerator.nextId());
            map.put("system04102",ymsOidGenerator.nextId());
            map.put("system04103",ymsOidGenerator.nextId());
            map.put("system04104",ymsOidGenerator.nextId());
            map.put("system04105",ymsOidGenerator.nextId());
            map.put("system04106",ymsOidGenerator.nextId());
            map.put("system04107",ymsOidGenerator.nextId());
            map.put("system04108",ymsOidGenerator.nextId());
            map.put("system04109",ymsOidGenerator.nextId());
            map.put("system04110",ymsOidGenerator.nextId());
            map.put("system04111",ymsOidGenerator.nextId());
            map.put("system04112",ymsOidGenerator.nextId());
            //付款
            map.put("system04201",ymsOidGenerator.nextId());
            map.put("system04202",ymsOidGenerator.nextId());
            map.put("system04203",ymsOidGenerator.nextId());
            map.put("system04204",ymsOidGenerator.nextId());
            map.put("system04205",ymsOidGenerator.nextId());
            map.put("system04206",ymsOidGenerator.nextId());
            map.put("system04207",ymsOidGenerator.nextId());
            map.put("system04208",ymsOidGenerator.nextId());
            map.put("system04209",ymsOidGenerator.nextId());
            map.put("system04210",ymsOidGenerator.nextId());
            map.put("system04211",ymsOidGenerator.nextId());
            map.put("system04212",ymsOidGenerator.nextId());
            //子表
            map.put("system0410101",ymsOidGenerator.nextId());
            map.put("system0410201",ymsOidGenerator.nextId());
            map.put("system0410202",ymsOidGenerator.nextId());
            map.put("system0410301",ymsOidGenerator.nextId());
            map.put("system0410401",ymsOidGenerator.nextId());
            map.put("system0410501",ymsOidGenerator.nextId());
            map.put("system0410601",ymsOidGenerator.nextId());
            map.put("system0410701",ymsOidGenerator.nextId());
            map.put("system0410801",ymsOidGenerator.nextId());
            map.put("system0410901",ymsOidGenerator.nextId());
            map.put("system0410902",ymsOidGenerator.nextId());
            map.put("system0411001",ymsOidGenerator.nextId());
            map.put("system0411101",ymsOidGenerator.nextId());
            map.put("system0411201",ymsOidGenerator.nextId());

            map.put("system0420101",ymsOidGenerator.nextId());
            map.put("system0420201",ymsOidGenerator.nextId());
            map.put("system0420202",ymsOidGenerator.nextId());
            map.put("system0420301",ymsOidGenerator.nextId());
            map.put("system0420401",ymsOidGenerator.nextId());
            map.put("system0420501",ymsOidGenerator.nextId());
            map.put("system0420601",ymsOidGenerator.nextId());
            map.put("system0420701",ymsOidGenerator.nextId());
            map.put("system0420801",ymsOidGenerator.nextId());
            map.put("system0420901",ymsOidGenerator.nextId());
            map.put("system0420902",ymsOidGenerator.nextId());
            map.put("system0421001",ymsOidGenerator.nextId());
            map.put("system0421101",ymsOidGenerator.nextId());
            map.put("system0421201",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem004MainDataNew", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem004ItemDataNew", map);
        }
    }


    private void initSystem004DataForDelete(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem004ItemDataNew", map);
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem004MainDataNew", map);
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem004InitData", map);
        if(count != null && count == 0){
            //收入
            map.put("system04101",ymsOidGenerator.nextId());
            map.put("system04102",ymsOidGenerator.nextId());
            map.put("system04103",ymsOidGenerator.nextId());
            map.put("system04104",ymsOidGenerator.nextId());
            map.put("system04105",ymsOidGenerator.nextId());
            map.put("system04106",ymsOidGenerator.nextId());
            map.put("system04107",ymsOidGenerator.nextId());
            map.put("system04108",ymsOidGenerator.nextId());
            map.put("system04109",ymsOidGenerator.nextId());
            map.put("system04110",ymsOidGenerator.nextId());
            map.put("system04111",ymsOidGenerator.nextId());
            map.put("system04112",ymsOidGenerator.nextId());
            //付款
            map.put("system04201",ymsOidGenerator.nextId());
            map.put("system04202",ymsOidGenerator.nextId());
            map.put("system04203",ymsOidGenerator.nextId());
            map.put("system04204",ymsOidGenerator.nextId());
            map.put("system04205",ymsOidGenerator.nextId());
            map.put("system04206",ymsOidGenerator.nextId());
            map.put("system04207",ymsOidGenerator.nextId());
            map.put("system04208",ymsOidGenerator.nextId());
            map.put("system04209",ymsOidGenerator.nextId());
            map.put("system04210",ymsOidGenerator.nextId());
            map.put("system04211",ymsOidGenerator.nextId());
            map.put("system04212",ymsOidGenerator.nextId());
            //子表
            map.put("system0410101",ymsOidGenerator.nextId());
            map.put("system0410201",ymsOidGenerator.nextId());
            map.put("system0410202",ymsOidGenerator.nextId());
            map.put("system0410301",ymsOidGenerator.nextId());
            map.put("system0410401",ymsOidGenerator.nextId());
            map.put("system0410501",ymsOidGenerator.nextId());
            map.put("system0410601",ymsOidGenerator.nextId());
            map.put("system0410701",ymsOidGenerator.nextId());
            map.put("system0410801",ymsOidGenerator.nextId());
            map.put("system0410901",ymsOidGenerator.nextId());
            map.put("system0410902",ymsOidGenerator.nextId());
            map.put("system0411001",ymsOidGenerator.nextId());
            map.put("system0411101",ymsOidGenerator.nextId());
            map.put("system0411201",ymsOidGenerator.nextId());

            map.put("system0420101",ymsOidGenerator.nextId());
            map.put("system0420201",ymsOidGenerator.nextId());
            map.put("system0420202",ymsOidGenerator.nextId());
            map.put("system0420301",ymsOidGenerator.nextId());
            map.put("system0420401",ymsOidGenerator.nextId());
            map.put("system0420501",ymsOidGenerator.nextId());
            map.put("system0420601",ymsOidGenerator.nextId());
            map.put("system0420701",ymsOidGenerator.nextId());
            map.put("system0420801",ymsOidGenerator.nextId());
            map.put("system0420901",ymsOidGenerator.nextId());
            map.put("system0420902",ymsOidGenerator.nextId());
            map.put("system0421001",ymsOidGenerator.nextId());
            map.put("system0421101",ymsOidGenerator.nextId());
            map.put("system0421201",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem004MainDataNew", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem004ItemDataNew", map);
        }
    }
    private void btwInitSystem005ItemDataForDelete(Tenant tenant) throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem005ItemDataNew", map);
        SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteSystem005MainDataNew", map);
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.querySystem005InitData", map);
        if(count != null && count == 0){
            //收入
            map.put("system0511",ymsOidGenerator.nextId());
            map.put("system0512",ymsOidGenerator.nextId());
            map.put("system0513",ymsOidGenerator.nextId());
            map.put("system0514",ymsOidGenerator.nextId());
            //支出
            map.put("system0521",ymsOidGenerator.nextId());
            map.put("system0522",ymsOidGenerator.nextId());
            map.put("system0523",ymsOidGenerator.nextId());
            map.put("system0524",ymsOidGenerator.nextId());
            //收入子表
            map.put("system051101",ymsOidGenerator.nextId());
            map.put("system051102",ymsOidGenerator.nextId());
            map.put("system051103",ymsOidGenerator.nextId());
            map.put("system051104",ymsOidGenerator.nextId());
            map.put("system051105",ymsOidGenerator.nextId());

            map.put("system051201",ymsOidGenerator.nextId());
            map.put("system051202",ymsOidGenerator.nextId());
            map.put("system051203",ymsOidGenerator.nextId());

            map.put("system051301",ymsOidGenerator.nextId());
            map.put("system051302",ymsOidGenerator.nextId());
            map.put("system051303",ymsOidGenerator.nextId());
            map.put("system051304",ymsOidGenerator.nextId());
            map.put("system051305",ymsOidGenerator.nextId());
            map.put("system051306",ymsOidGenerator.nextId());
            map.put("system051307",ymsOidGenerator.nextId());
            map.put("system051308",ymsOidGenerator.nextId());
            map.put("system051309",ymsOidGenerator.nextId());
            map.put("system051310",ymsOidGenerator.nextId());

            map.put("system051401",ymsOidGenerator.nextId());
            map.put("system051402",ymsOidGenerator.nextId());
            map.put("system051403",ymsOidGenerator.nextId());
            map.put("system051404",ymsOidGenerator.nextId());
            map.put("system051405",ymsOidGenerator.nextId());
            map.put("system051406",ymsOidGenerator.nextId());
            map.put("system051407",ymsOidGenerator.nextId());
            map.put("system051408",ymsOidGenerator.nextId());
            map.put("system051409",ymsOidGenerator.nextId());


            //支出子表
            map.put("system052101",ymsOidGenerator.nextId());
            map.put("system052102",ymsOidGenerator.nextId());
            map.put("system052103",ymsOidGenerator.nextId());
            map.put("system052104",ymsOidGenerator.nextId());
            map.put("system052105",ymsOidGenerator.nextId());

            map.put("system052201",ymsOidGenerator.nextId());
            map.put("system052202",ymsOidGenerator.nextId());
            map.put("system052203",ymsOidGenerator.nextId());

            map.put("system052301",ymsOidGenerator.nextId());
            map.put("system052302",ymsOidGenerator.nextId());
            map.put("system052303",ymsOidGenerator.nextId());
            map.put("system052304",ymsOidGenerator.nextId());
            map.put("system052305",ymsOidGenerator.nextId());
            map.put("system052306",ymsOidGenerator.nextId());
            map.put("system052307",ymsOidGenerator.nextId());
            map.put("system052308",ymsOidGenerator.nextId());
            map.put("system052309",ymsOidGenerator.nextId());
            map.put("system052310",ymsOidGenerator.nextId());

            map.put("system052401",ymsOidGenerator.nextId());
            map.put("system052402",ymsOidGenerator.nextId());
            map.put("system052403",ymsOidGenerator.nextId());
            map.put("system052404",ymsOidGenerator.nextId());
            map.put("system052405",ymsOidGenerator.nextId());
            map.put("system052406",ymsOidGenerator.nextId());
            map.put("system052407",ymsOidGenerator.nextId());
            map.put("system052408",ymsOidGenerator.nextId());
            map.put("system052409",ymsOidGenerator.nextId());
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem005MainDataNew", map);
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.initSystem005ItemDataNew", map);
            map.put("flowCode","system0103");
            // SqlHelper.delete("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.deleteFlowHandleSubData", map);
            Long flowId = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.queryFlowhandlesetting", map);
            if(flowId != null){
                map.put("flowId",flowId);
            }else{
                flowhandlesettingInnerService.initTenantData(tenant);
                flowId = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.queryFlowhandlesetting", map);
                map.put("flowId",flowId);
            }

        }
    }
}
