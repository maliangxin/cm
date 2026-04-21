package com.yonyoucloud.fi.cmp.fcdsusesetting.service.impl;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting;
import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting_b;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class FcdsUseSettingInnerServiceImpl implements IFcdsUseSettingInnerService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Override
    public MessageResultVO unstop(List<Map> list) throws Exception {
        MessageResultVO messageResultVO = new MessageResultVO();
        int successCount = 0;
        int failedCount = 0;
        Map<String, String> failed = new HashMap<>();
        List<String> messages = new ArrayList<>();
        if (org.springframework.util.CollectionUtils.isEmpty(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101750"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A3F320450003A", "请至少选择一条数据！") /* "请至少勾选一条数据！" */);
        }
        // 存储旧数据的pubts
        Map<String, Date> pubtsMap = new HashMap<>();
        String[] ids = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map map = list.get(i);
            ids[i] = (String) map.get("id");
            pubtsMap.put(ids[i], DateUtil.parseTime2Date((String) map.get("pubts")));
        }
        // 重新查询一次数据库
        List<FcDsUseSetting> fcDsUseSettings = getByIds(ids);
        List<Long> maindIds = new ArrayList<>();
        // 校验pubts
        for (FcDsUseSetting entity : fcDsUseSettings) {
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD00805180004", "流程编码[%s]已被修改，请刷新后重试！") /* 流程编码[%s]已被修改，请刷新后重试！" */, entity.getCode()));
                failedCount++;
                if (fcDsUseSettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "单据已经被修改，请刷新后重新操作！") /* "单据已经被修改，请刷新后重新操作！" */);
                }
                continue;
            }
            try {
                if ("1".equals(entity.getEnable())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102144"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD54005180008", "启用失败：【%s】已启用，不可重复启用！") /* "启用失败：【%s】已启用，不可重复启用！" */, entity.getCode()));
                }
                entity.setEnable("1");
                entity.setEnablets(new Date());
                entity.setDisablets(null);
                entity.setEnableUser(InvocationInfoProxy.getUsername());
                entity.setEnableUserId(InvocationInfoProxy.getUserid());
                entity.setEntityStatus(EntityStatus.Update);
                maindIds.add(entity.getId());
                successCount++;
            } catch (Exception e) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD54005180008", "启用失败：【%s】已启用，不可重复启用！") /* "启用失败：【%s】已启用，不可重复启用！" */, entity.getCode()));
                if (fcDsUseSettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102549"),e.getMessage());
                }
                failedCount++;
                continue;
            }
        }
        MetaDaoHelper.update(FcDsUseSetting.ENTITY_NAME, fcDsUseSettings);

        //更新子表
        if (CollectionUtils.isNotEmpty(maindIds)) {
            QuerySchema querySchema = QuerySchema.create().addSelect("id").appendQueryCondition(
                    QueryCondition.name("mainid").in(ids)
            );
            List<FcDsUseSetting_b> fcDsUseSettingSubList = MetaDaoHelper.queryObject(FcDsUseSetting_b.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(fcDsUseSettingSubList)) {
                fcDsUseSettingSubList.forEach(item -> {
                    item.setEnable("1");
                    item.setEnablets(new Date());
                    item.setDisablets(null);
                    item.setEntityStatus(EntityStatus.Update);
                });
                MetaDaoHelper.update(FcDsUseSetting_b.ENTITY_NAME, fcDsUseSettingSubList);
            }
        }

        messageResultVO.setFailed(failed);
        messageResultVO.setMessages(messages);
        messageResultVO.setSucessCount(successCount);
        messageResultVO.setFailCount(failedCount);
        messageResultVO.setCount(fcDsUseSettings.size());
        return messageResultVO;
    }

    @Override
    public MessageResultVO stop(List<Map> list) throws Exception {
        MessageResultVO messageResultVO = new MessageResultVO();
        int successCount = 0;
        int failedCount = 0;
        Map<String, String> failed = new HashMap<>();
        List<String> messages = new ArrayList<>();
        if (org.springframework.util.CollectionUtils.isEmpty(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101750"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A3F320450003A", "请至少选择一条数据！") /* "请至少勾选一条数据！" */);
        }
        // 存储旧数据的pubts
        Map<String, Date> pubtsMap = new HashMap<>();
        String[] ids = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map map = list.get(i);
            ids[i] = (String) map.get("id");
            pubtsMap.put(ids[i], DateUtil.parseTime2Date((String) map.get("pubts")));
        }
        // 重新查询一次数据库
        List<FcDsUseSetting> fcDsUseSettings = getByIds(ids);
        List<Long> maindIds = new ArrayList<>();
        // 校验pubts
        for (FcDsUseSetting entity : fcDsUseSettings) {
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD00805180004", "流程编码[%s]已被修改，请刷新后重试！") /* "数据源编码[%s]已被修改，请刷新后重试！" */, entity.getCode()));
                failedCount++;
                if (fcDsUseSettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "单据已经被修改，请刷新后重新操作！") /* "单据已经被修改，请刷新后重新操作！" */);
                }
                continue;
            }
            try {
                if ("0".equals(entity.getEnable())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102147"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD07A05180002", "停用失败：【%s】已停用，不可重复停用！") /* "数据源编码[%s]已停用，不可重复停用！" */, entity.getCode()));
                }
                entity.setEnable("0");
                entity.setEnablets(null);
                entity.setDisablets(new Date());
                entity.setEnableUser(null);
                entity.setEnableUserId(null);
                entity.setEntityStatus(EntityStatus.Update);
                maindIds.add(entity.getId());
                successCount++;
            } catch (Exception e) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD07A05180002", "停用失败：【%s】已停用，不可重复停用！") /* "数据源编码[%s]已停用，不可重复停用！" */, entity.getCode()));
                if (fcDsUseSettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102550"),e.getMessage());
                }
                failedCount++;
                continue;
            }
        }
        MetaDaoHelper.update(FcDsUseSetting.ENTITY_NAME, fcDsUseSettings);
        //更新子表
        if (CollectionUtils.isNotEmpty(maindIds)) {
            QuerySchema querySchema = QuerySchema.create().addSelect("id").appendQueryCondition(
                    QueryCondition.name("mainid").in(ids)
            );
            List<FcDsUseSetting_b> fcDsUseSettingSubList = MetaDaoHelper.queryObject(FcDsUseSetting_b.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(fcDsUseSettingSubList)) {
                fcDsUseSettingSubList.forEach(item -> {
                    item.setEnable("0");
                    item.setEnablets(null);
                    item.setDisablets(new Date());
                    item.setEntityStatus(EntityStatus.Update);
                });
                MetaDaoHelper.update(FcDsUseSetting_b.ENTITY_NAME, fcDsUseSettingSubList);
            }
        }
        messageResultVO.setFailed(failed);
        messageResultVO.setMessages(messages);
        messageResultVO.setSucessCount(successCount);
        messageResultVO.setFailCount(failedCount);
        messageResultVO.setCount(fcDsUseSettings.size());
        return messageResultVO;
    }

    /**
     * 预置数据 手动同步 先删后加
     */
    @Override
    public void dataSync() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", InvocationInfoProxy.getYxyTenantid());
        map.put("ytenantId", InvocationInfoProxy.getTenantid());
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code,name").appendQueryCondition(
                QueryCondition.name("code").in("system02", "system05")
        );
        try {
            SqlHelper.delete("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.deleteSysSubData", map);
            SqlHelper.delete("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.deleteSysMainData", map);
            Long count = SqlHelper.selectOne("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.queryInitData", map);
            if(count != null && count == 0){
                List<BizObject> bizObjects = MetaDaoHelper.queryObject("yonbip-fi-ctmtmsp.yonbip-fi-ctmtmsp.tmsp_cdp_ds", querySchema, "yonbip-fi-ctmtmsp");
                Map<String, Object> cdpMap = new HashMap<>(512);
                if (CollectionUtils.isNotEmpty(bizObjects)) {
                    cdpMap = bizObjects.stream().collect(Collectors.toMap(item -> item.getString("code"), item -> item.getId()));
                }
                map.put("pubts", new Date());
                map.put("system02", cdpMap.containsKey("system02") ? cdpMap.get("system02") : null);
                map.put("system05", cdpMap.containsKey("system05") ? cdpMap.get("system05") : null);

                map.put("system0101", ymsOidGenerator.nextId());
                map.put("system0201", ymsOidGenerator.nextId());
                map.put("system0202", ymsOidGenerator.nextId());
                map.put("system0301", ymsOidGenerator.nextId());

                IntStream.rangeClosed(1, 19).boxed().forEach(
                        num -> {
                            map.put("system0101" + num, ymsOidGenerator.nextId());
                        }
                );
                IntStream.rangeClosed(1, 37).boxed().forEach(
                        num -> {
                            map.put("system0201" + num, ymsOidGenerator.nextId());
                        }
                );
                IntStream.rangeClosed(1, 37).boxed().forEach(
                        num -> {
                            map.put("system0202" + num, ymsOidGenerator.nextId());
                        }
                );
                map.put("system03011", ymsOidGenerator.nextId());
                SqlHelper.insert("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.initFcMainData", map);
                SqlHelper.insert("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.initFcSubDataSystem0101", map);
                SqlHelper.insert("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.initFcSubDataSystem0201", map);
                SqlHelper.insert("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.initFcSubDataSystem0202", map);
                SqlHelper.insert("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.initFcSubDataSystem0301", map);
            }
        } catch (Exception e) {
            log.error("预置数据更新失败！", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102551"),e.getMessage());
        }
    }

    private List<FcDsUseSetting> getByIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("id").in(ids));
        return MetaDaoHelper.queryObject(FcDsUseSetting.ENTITY_NAME, querySchema, null);
    }
}
