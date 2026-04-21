package com.yonyoucloud.fi.cmp.fcdsusesetting.service;

import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.FcdsUseSettingVO;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;

import java.util.List;
import java.util.Map;

/**
 * @author guoxh
 */
public interface IFcdsUseSettingService {

    /**
     * 根据流程处理环节、适用对象、收付方向作为查询条件查询符合条件的 启用状态的单据类型
     * @param flowAction
     * @param businessScenario
     * @param dcFlag
     * @return
     */
    List<FcdsUseSettingVO> queryByCondition(String flowAction,String businessScenario,String dcFlag);

}
