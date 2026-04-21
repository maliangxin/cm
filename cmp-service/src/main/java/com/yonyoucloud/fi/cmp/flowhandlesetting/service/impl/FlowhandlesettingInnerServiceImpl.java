package com.yonyoucloud.fi.cmp.flowhandlesetting.service.impl;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.flowhandlesetting.Flowhandlesetting;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingInnerService;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class FlowhandlesettingInnerServiceImpl implements IFlowhandlesettingInnerService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Override
    public CtmJSONObject initTenantData(Tenant tenant) throws Exception {
        String key = "flowhandleSetting:" + tenant.getYTenantId();
        CtmLockTool.executeInOneServiceLock(key ,60L, TimeUnit.SECONDS,(int lockStatus)->{
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("tenantId", tenant.getId());
            map.put("ytenantId", tenant.getYTenantId());
            map.put("pubts", new Date());
            //SqlHelper.delete("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.deleteInitMainData", map);
            Long count = SqlHelper.selectOne("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.queryInitData", map);
            if(count ==  null || count == 0 ) {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    //加锁失败
                    log.error("对应的key:{}加锁失败", key);
                    return;
                }
                this.reInitTenantData(tenant);
            }
        });
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400448", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }

    @Override
    public CtmJSONObject reInitTenantData(Tenant tenant) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        map.put("pubts", new Date());
//        SqlHelper.delete("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.deleteInitSubData", map);

        IntStream.rangeClosed(1,14).boxed().forEach(
                num -> {
                    map.put("id0"+ num,ymsOidGenerator.nextId());
                }
        );
        SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.initFlowHandleMainData01", map);
        SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.initFlowHandleMainData02", map);
        SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.initFlowHandleMainData03", map);
        SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.initFlowHandleMainData04", map);
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400448", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }

    @Override
    public MessageResultVO unstop(List<Map> list) throws Exception {
        MessageResultVO messageResultVO = new MessageResultVO();
        int successCount = 0;
        int failedCount = 0;
        Map<String, String> failed = new HashMap<>();
        List<String> messages = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
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
        List<Flowhandlesetting> flowhandlesettings = getByIds(ids);

        // 校验pubts
        for (Flowhandlesetting entity : flowhandlesettings) {
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD00805180004", "流程编码[%s]已被修改，请刷新后重试！") /* 流程编码[%s]已被修改，请刷新后重试！" */, entity.getCode()));
                failedCount++;
                if (flowhandlesettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "此单据已经被修改，请刷新后重新操作！") /* "此单据已经被修改，请刷新后重新操作！" */);
                }
                continue;
            }
            try {
                if ("1".equals(entity.getEnable())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102144"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD54005180008", "启用失败：【%s】已启用，不可重复启用！") /* "启用失败：【%s】已启用，不可重复启用！" */, entity.getCode()));
                }
                // 校验编码唯一
//                if (validateOnlyValues(TmspCdpDs.CODE, entity.getCode(), entity.getId(), true)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102145"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_TMSP-BE_19085CDA042800DE", "数据源编码[%s]已存在！") /* "数据源编码[%s]已存在！" */, entity.getCode()));
//                }
                entity.setEnable("1");
                entity.setEnablets(new Date());
                entity.setDisablets(null);
                entity.setEntityStatus(EntityStatus.Update);
                successCount++;
            } catch (Exception e) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD54005180008", "启用失败：【%s】已启用，不可重复启用！") /* "数据源编码[%s]已启用，不可重复启用！" */, entity.getCode()));
                if (flowhandlesettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102146"),e.getMessage());
                }
                failedCount++;
                continue;
            }
        }
        MetaDaoHelper.update(Flowhandlesetting.ENTITY_NAME,flowhandlesettings);

        messageResultVO.setFailed(failed);
        messageResultVO.setMessages(messages);
        messageResultVO.setSucessCount(successCount);
        messageResultVO.setFailCount(failedCount);
        messageResultVO.setCount(flowhandlesettings.size());
        return messageResultVO;
    }

    @Override
    public MessageResultVO stop(List<Map> list) throws Exception {
        MessageResultVO messageResultVO = new MessageResultVO();
        int successCount = 0;
        int failedCount = 0;
        Map<String, String> failed = new HashMap<>();
        List<String> messages = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
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
        List<Flowhandlesetting> flowhandlesettings = getByIds(ids);

        // 校验pubts
        for (Flowhandlesetting entity : flowhandlesettings) {
            // 判断pubts
            if (entity.getPubts() != null && pubtsMap.get(entity.getId()) != null && entity.getPubts().compareTo(pubtsMap.get(entity.getId())) != 0) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD00805180004", "流程编码[%s]已被修改，请刷新后重试！") /* "流程编码[%s]已被修改，请刷新后重试！" */, entity.getCode()));
                failedCount++;
                if (flowhandlesettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101752"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CCFD804D80004", "单据已经被修改，请刷新后重新操作！") /* "单据已经被修改，请刷新后重新操作！" */);
                }
                continue;
            }
            try {
                if ("0".equals(entity.getEnable())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102147"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD07A05180002", "停用失败：【%s】已停用，不可重复停用！") /* "数据源编码[%s]已停用，不可重复停用！" */, entity.getCode()));
                }
                List<BankreconciliationIdentifySetting> settings= checkReferenced(entity.getId());
                if(CollectionUtils.isNotEmpty(settings)){
                    String codes = settings.stream().map(BankreconciliationIdentifySetting::getCode).collect(Collectors.joining("】,【"));//@notranslate
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400449", "规则【%s】已被流水自动辨识匹配规则【%s】引用，请先取消引用!") /* "规则【%s】已被流水自动辨识匹配规则【%s】引用，请先取消引用!" */,entity.getCode(),codes));
                }
                entity.setEnable("0");
                entity.setEnablets(null);
                entity.setDisablets(new Date());
                entity.setEntityStatus(EntityStatus.Update);
                successCount++;
            } catch (Exception e) {
                failed.put(entity.getId(), entity.getId());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD07A05180002", "停用失败：【%s】已停用，不可重复停用！") /* "数据源编码[%s]已停用，不可重复停用！" */, entity.getCode()));
                if (flowhandlesettings.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102148"),e.getMessage());
                }
                failedCount++;
                continue;
            }
        }
        MetaDaoHelper.update(Flowhandlesetting.ENTITY_NAME,flowhandlesettings);
        //更新子表
        messageResultVO.setFailed(failed);
        messageResultVO.setMessages(messages);
        messageResultVO.setSucessCount(successCount);
        messageResultVO.setFailCount(failedCount);
        messageResultVO.setCount(flowhandlesettings.size());
        return messageResultVO;
    }


    private List<Flowhandlesetting> getByIds(String[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("id").in(ids));
        return MetaDaoHelper.queryObject(Flowhandlesetting.ENTITY_NAME,querySchema,null);
    }

    private List<BankreconciliationIdentifySetting> checkReferenced(Long id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,code,name").appendQueryCondition(
                QueryCondition.name("flow_id").eq(id)
        );
        return MetaDaoHelper.queryObject(BankreconciliationIdentifySetting.ENTITY_NAME, querySchema, null);
    }
}
