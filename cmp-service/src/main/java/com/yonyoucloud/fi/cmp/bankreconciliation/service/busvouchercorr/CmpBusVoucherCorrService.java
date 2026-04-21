package com.yonyoucloud.fi.cmp.bankreconciliation.service.busvouchercorr;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @description: 智能到账,业务凭据关联service
 * @author: wanxbo@yonyou.com
 * @date: 2024/7/1 10:25
 */

public interface CmpBusVoucherCorrService {

    /**
     * 根据业务凭据关联到的销售订单，查询关联到的收款协议
     * @param param orderid:销售订单id
     * @return
     * @throws Exception
     */
    CtmJSONObject queryUdinghuoCollectAgreement(CtmJSONObject  param) throws Exception;

    CtmJSONObject getBusVoucherInfoList(CtmJSONObject param);
}
