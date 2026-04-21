package com.yonyoucloud.fi.cmp.receivebill.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;

import java.util.List;

/**
 * Created by sz on 2019/4/20 0020.
 */
public interface ReceiveBillService {

     List<ReceiveBill> queryAggvoByIds(Long[] ids) throws Exception;


    List<ReceiveBill> getReceiveBillByIds(Long[] ids) throws Exception;

    /**
     * 结算
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
     CtmJSONObject settle(List<ReceiveBill> receiveBillList) throws Exception;

    /**
     * 取消结算
     * @param receiveBillList
     * @return
     * @throws Exception
     */
     CtmJSONObject unSettle(List<ReceiveBill> receiveBillList) throws Exception;

    /**
     * 审批
     * @param receiveBillList
     * @throws Exception
     */
    CtmJSONObject receiveBillSp(List<ReceiveBill> receiveBillList) throws Exception;

    /**
     *取消审批
     * @param receiveBillList
     * @throws Exception
     */
    CtmJSONObject receiveBillQxsp(List<ReceiveBill> receiveBillList) throws Exception;

}
