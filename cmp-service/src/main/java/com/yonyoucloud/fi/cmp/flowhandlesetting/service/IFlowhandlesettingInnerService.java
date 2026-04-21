package com.yonyoucloud.fi.cmp.flowhandlesetting.service;

import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;

import java.util.List;
import java.util.Map;

public interface IFlowhandlesettingInnerService {

    CtmJSONObject initTenantData(Tenant tenant) throws Exception;

    CtmJSONObject reInitTenantData(Tenant tenant) throws Exception;

    MessageResultVO unstop(List<Map> list) throws Exception;

    MessageResultVO stop(List<Map> list) throws Exception;
}
