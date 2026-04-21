package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.service.itf.IExchangeRateTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 汇率类型查询*
 * @author xuxbo
 * @date 2023/12/29 15:55
 */

@Service
public class ExchangeRateTypeQueryService {

    @Autowired
    IExchangeRateTypeService exchangeRateTypeService;

    /**
     * 根据条件查询汇率类型*
     * @param params
     * @return
     * @throws Exception
     */
    public ExchangeRateTypeVO queryExchangeRateTypeByCondition(BdRequestParams params) throws Exception {
        return exchangeRateTypeService.queryByCondition(params);
    }

    /**
     * 根据汇率类型id查询汇率类型
     * @param id
     * @return
     * @throws Exception
     */
    public ExchangeRateTypeVO queryExcahngeRateTypeById(String id) throws Exception {
        BdRequestParams params = new BdRequestParams();
        params.setId(id);
        return exchangeRateTypeService.queryByCondition(params);
    }
}
