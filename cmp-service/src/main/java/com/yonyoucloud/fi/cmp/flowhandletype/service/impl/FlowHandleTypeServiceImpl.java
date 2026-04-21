package com.yonyoucloud.fi.cmp.flowhandletype.service.impl;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingInnerService;
import com.yonyoucloud.fi.cmp.flowhandlesetting.service.IFlowhandlesettingService;
import com.yonyoucloud.fi.cmp.flowhandletype.FlowHandleType;
import com.yonyoucloud.fi.cmp.flowhandletype.service.IFlowHandleTypeService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FlowHandleTypeServiceImpl implements IFlowHandleTypeService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private IFlowhandlesettingInnerService flowhandlesettingInnerService;

    @Override
    public CtmJSONObject updateStatus(CtmJSONObject param) throws Exception {
        return null;
    }

    @Override
    public CtmJSONObject sortNum(CtmJSONObject param) throws Exception {
        CtmJSONArray recordes = param.getJSONArray("record");
        List<FlowHandleType> updateList = new ArrayList<>();
        for (int i = 0; i < recordes.size(); i++) {
            FlowHandleType flowHandleType = recordes.getObject(i, FlowHandleType.class);
            flowHandleType.setSort(i + 1);
            flowHandleType.setEntityStatus(EntityStatus.Update);
            //id需要转化格式
            flowHandleType.setId(Long.parseLong(flowHandleType.getId().toString()));
            flowHandleType.setPubts(null);
            updateList.add(flowHandleType);
        }
        MetaDaoHelper.update(FlowHandleType.ENTITY_NAME, updateList);
        CtmJSONObject result = new CtmJSONObject();
        result.put("msg", "success");
        return result;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject initIdentifyTypeData(Tenant tenant,String billnum) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        map.put("pubts", new Date());

//        if("cmp_flowhandleTypeList".equalsIgnoreCase(billnum)){
            //流水处理规则
            String key = "cmp_flowhandleTypeList:" + tenant.getYTenantId();
            CtmLockTool.executeInOneServiceLock(key,60L, TimeUnit.SECONDS,(int lockStatus)->{
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    //加锁失败
                    log.error("对应的key:{}加锁失败", key);
                    return;
                }
                long count = SqlHelper.selectOne("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.queryFlowhandleSettingInitData", map);
                if(count != 4) {
                    SqlHelper.delete("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.deleteFlowhandleSettingData", map);
                    map.put("id201",ymsOidGenerator.nextId());
                    map.put("id202",ymsOidGenerator.nextId());
                    map.put("id203",ymsOidGenerator.nextId());
                    map.put("id204",ymsOidGenerator.nextId());
                    SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.initFlowhandleSettingData", map);
                }
                flowhandlesettingInnerService.initTenantData(tenant);
            });
//        }else if("cmp_fcdsUsesetTypeList".equalsIgnoreCase(billnum)){
            //流水处理使用数据源设置
            String key2 = "cmp_fcdsUsesetTypeList:" + tenant.getYTenantId();
            CtmLockTool.executeInOneServiceLock(key2,60L, TimeUnit.SECONDS,(int lockStatus)->{
                long count = SqlHelper.selectOne("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.queryFcdsUsesetTypeInitData", map);
                if(count != 3) {
                    SqlHelper.delete("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.deleteFcdsUsesetTypeData", map);
                    map.put("id101",ymsOidGenerator.nextId());
                    map.put("id102",ymsOidGenerator.nextId());
                    map.put("id103",ymsOidGenerator.nextId());
                    SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.initFcdsUsesetTypeData", map);
                }
            });
//        }
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A3", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }


    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject reInitData(Tenant tenant,boolean onlyDelete) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        map.put("pubts", new Date());
        SqlHelper.delete("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.deleteFlowHandleTypeData", map);
        if(!onlyDelete) {
            initData(map);
        }
        CtmJSONObject c = new CtmJSONObject();
        c.put("msg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A3", "租户数据预置成功") /* "租户数据预置成功" */);
        return c;
    }

    private void initData(Map<String, Object> map){
        map.put("id101",ymsOidGenerator.nextId());
        map.put("id102",ymsOidGenerator.nextId());
        map.put("id103",ymsOidGenerator.nextId());
        map.put("id201",ymsOidGenerator.nextId());
        map.put("id202",ymsOidGenerator.nextId());
        map.put("id203",ymsOidGenerator.nextId());
        map.put("id204",ymsOidGenerator.nextId());
        SqlHelper.insert("com.yonyoucloud.fi.cmp.flowhandletype.mapper.FlowHandleTypeMapper.initFlowHandleTypeData", map);
    }
}
