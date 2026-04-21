package com.yonyoucloud.fi.cmp.settlement.service;

import com.google.common.collect.Lists;
import com.yonyou.ucf.orgbp.ref.api.OrgBpRefCheckApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 组织服务期初设置 删除、修改校验
 * yangjn 2021/11/19
 */
@Service("orgCheckForCmpService")
public class OrgCheckForCmpService implements OrgBpRefCheckApi {

    private static final Logger logger = LoggerFactory.getLogger(OrgCheckForCmpService.class);

    @Autowired
    SettlementService settlementService;

    @Override
    public List<String> findBpInUse(String orgId, List<String> modelNames, String yhtTenantId, String sysId) {

        List<String> models = Lists.newArrayList();
        List<String> orgIds = Lists.newArrayList();
        if(modelNames.contains("cashManagement")){
            orgIds.add(orgId);
            List<String> hasUseOrg = Lists.newArrayList();
            try{
                hasUseOrg = settlementService.hasRefAccbook(orgIds);
            }catch(Exception e){
                logger.error("OrgCheckForCmpService error", e);
            }

            if (null != hasUseOrg && hasUseOrg.size() > 0) {
                models.add("cashManagement");
            }
        }
        return models;
    }

}
