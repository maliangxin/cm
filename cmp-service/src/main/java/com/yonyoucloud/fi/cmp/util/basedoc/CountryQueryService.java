package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyou.ucf.basedoc.model.BdCountryVO;
import com.yonyou.ucf.basedoc.model.rpcparams.country.CountryQueryParam;
import com.yonyou.ucf.basedoc.service.itf.ICountryQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询国家万能接口
 */
@Service
public class CountryQueryService {

    @Autowired
    ICountryQueryService iCountryQueryService;

    public BdCountryVO findById(String id) throws Exception {
        CountryQueryParam countryQueryParam = new CountryQueryParam();
        countryQueryParam.setId(id);
        return iCountryQueryService.queryOneByCondition(countryQueryParam);
    }

    public BdCountryVO findByCode(String code) throws Exception {
        CountryQueryParam countryQueryParam = new CountryQueryParam();
        countryQueryParam.setCode(code);
        return iCountryQueryService.queryOneByCondition(countryQueryParam);
    }

}
