package com.yonyoucloud.fi.cmp.bankcapitalvirtual;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;


public interface VirtualFlowRuleConfigService {
    String enable(Map<String, Object> paramMap) throws Exception;

    String disenable(Map<String, Object> paramMap) throws Exception;

    CtmJSONObject updateConfigInfo(CtmJSONObject params) throws Exception;
}
