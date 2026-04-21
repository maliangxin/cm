package com.yonyoucloud.fi.cmp.flowhandlesetting.service;

import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.flowhandlesetting.dto.FlowHandleSettingVO;

import java.util.List;
import java.util.Map;

/**
 * @author guoxh
 */
public interface IFlowhandlesettingService {
    /**
     *
     * @param flowType 流程处理环节 必填
     * @param object 适用对象 必填
     * @param accentity 适用组织 非必填， 为空时默认为企业租户级
     * @param handleType 处理方式 1 手动 2 自动
     * @return
     */
    List<FlowHandleSettingVO> queryFlowHandleSettingByCondition(Integer flowType,Integer object,String accentity, Integer handleType);


}
