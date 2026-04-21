package com.yonyoucloud.fi.cmp.flowhandletype.service;

import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface IFlowHandleTypeService {
    /**
     * 更新启用/停用状态 (暂不需要)
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject updateStatus(CtmJSONObject param) throws Exception;

    /**
     * 排序
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject sortNum(CtmJSONObject param) throws Exception;

    /**
     * 租户数据初始化(根据billnum不同分别初始化不同节点)
     * @param tenant
     * @param billnum
     * @return
     * @throws Exception
     */
    CtmJSONObject initIdentifyTypeData(Tenant tenant, String billnum) throws Exception;

    /**
     * 租户数据重新初始化
     * @param tenant
     * @param onlyDelete
     * @return
     * @throws Exception
     */
    CtmJSONObject reInitData(Tenant tenant, boolean onlyDelete) throws Exception;
}
