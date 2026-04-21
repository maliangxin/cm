package com.yonyoucloud.fi.cmp.margintype.service;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface MargintypeService {
    CtmJSONObject enable(Long id,String code) throws Exception;
    CtmJSONObject unEnable(Long id,String code) throws Exception;

}
