package com.yonyoucloud.fi.cmp.fcdsusesetting.service;

import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;

import java.util.List;
import java.util.Map;

public interface IFcdsUseSettingInnerService {

    MessageResultVO unstop(List<Map> list) throws Exception;

    MessageResultVO stop(List<Map> list) throws Exception;
    /**
     * 数据同步
     */
    void dataSync();
}
