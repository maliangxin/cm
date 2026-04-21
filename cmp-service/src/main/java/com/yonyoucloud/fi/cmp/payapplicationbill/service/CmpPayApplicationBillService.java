package com.yonyoucloud.fi.cmp.payapplicationbill.service;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.vo.ResultMessageVO;

import java.util.List;

/**
 * <h1>付款申请单服务接口</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-16 10:12
 */
public interface CmpPayApplicationBillService {

    /**
     * <h2>付款单拉单成功后，调整预占金额的值</h2>
     *
     * @param payApplicationBillList: 付款申请单列表
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2020/11/16 10:14
     */
    void settle(List<PayApplicationBill> payApplicationBillList) throws Exception;

    /**
     * <h2>关闭申请单</h2>
     *
     * @param ids : 单据ID字符串
     * @param closedStatus: 单据关闭标识
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/8/13 15:11
     */
    ResultMessageVO updatePayApplicationBillStatusByClosed(String ids, String closedStatus) throws Exception;


    /**
     * <h2>OpenApi删除操作</h2>
     *
     * @param param :
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/8/26 13:52
     */
    ResultMessageVO deletePayApplyBillByIds(BillDataDto param) throws Exception;

    /**
     *
     * @param
     * @return
     * @throws Exception
     */
    void payapplicationCopyBill(String accentity) throws Exception;





    CtmJSONObject generatorFundCollectionBill(String id);
}
