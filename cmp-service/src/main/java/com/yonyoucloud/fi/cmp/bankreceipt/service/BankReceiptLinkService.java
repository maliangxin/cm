package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;


import java.util.List;
import java.util.Map;

/**
 * desc:银行交易回单联查根据银行交易回单，如果传过来的银行对账单主键有值直接查询银行对账单关联的回单，如果没有根据6要素直接查询匹配的回单
 *  * 6要素：收付方向、本方银行账号、对方银行账号、对方户名、金额、摘要
 * author:wangqiangac
 * date:2023/5/19 13:24
 */
public interface BankReceiptLinkService {
    /**
     * 根据params中的银行对账单主键，或者6要素查询匹配的银行回单
     * @param params
     * @return
     */
    public List<Map<String,Object>> queryMathData(CtmJSONObject params) throws Exception;
    /**
     * 根据params中的银行对账单主键，或者6要素查询匹配的银行回单
     * @param params
     * @return
     */
    public List<Map<String,Object>> queryMathData(CommonRequestDataVo params) throws Exception;
}
