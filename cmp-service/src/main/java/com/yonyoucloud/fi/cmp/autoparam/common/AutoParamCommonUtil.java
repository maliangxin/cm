package com.yonyoucloud.fi.cmp.autoparam.common;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.BaseOrgDTO;
import com.yonyou.permission.util.AuthSdkFacadeUtils;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AutoParamCommonUtil {
    /**
     * 筛选过滤有权限的组织
     *
     * @param queryAccList
     * @param billContext
     * @throws Exception
     */
    public static List<BaseOrgDTO> filterOrgPermission(List<BaseOrgDTO> queryAccList, BillContext billContext) throws Exception {

        if (CollectionUtils.isNotEmpty(queryAccList)) {
            //过滤当前有权限的组织
            String serviceCode = billContext.getParameter("serviceCode") == null ? "ficmp0050" : billContext.getParameter("serviceCode");
            if (null == serviceCode) {
                return queryAccList;
            }
            String yhtUserId = InvocationInfoProxy.getUserid();
            String yhtTenantId = InvocationInfoProxy.getTenantid();
            boolean includeDisable = false;//是否包含停用的主体
            Set<String> orgPermissions = AuthSdkFacadeUtils.getUserMasterOrgPermission(yhtUserId, yhtTenantId, serviceCode, includeDisable);
            if (!orgPermissions.isEmpty()) {
                return queryAccList.stream().filter(vo -> orgPermissions.contains(vo.getId())).collect(Collectors.toList());
            }
        }
        return queryAccList;
    }
}
