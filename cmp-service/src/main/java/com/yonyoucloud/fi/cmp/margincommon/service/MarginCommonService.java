package com.yonyoucloud.fi.cmp.margincommon.service;

import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import org.imeta.orm.base.BizObject;

/**
 * 保证金台账管理 业务接口*
 * @author xuxbo
 * @date 2023/8/4 10:02
 */

public interface MarginCommonService {

    /**
     * 更新支付保证金的结算状态以及结算时间*
     * @param dataSettledDetail
     * @param payMargin
     * @throws Exception
     */
    void updateSettledInfoOfPayMargin(DataSettledDetail dataSettledDetail, PayMargin payMargin) throws Exception;


    /**
     * 更新收到保证金的结算状态以及结算时间*
     * @param dataSettledDetail
     * @param receiveMargin
     * @throws Exception
     */
    void updateSettledInfoOfReceiveMargin(DataSettledDetail dataSettledDetail, ReceiveMargin receiveMargin) throws Exception;


    /**
     * <h2>生成转换支付保证金</h2>
     *
     * @param bizObject : 支付保证金
     * @author Sun GuoCai
     * @since 2023/8/10 15:32
     */
    PayMargin generateConversionPayMargin(BizObject bizObject);

    /**
     * <h2>生成转换收到保证金</h2>
     *
     * @param bizObject : 收到保证金
     * @author Sun GuoCai;
     * @since 2023/8/10 15:32
     */
    void generateConversionReceiveMargin(BizObject bizObject);


    boolean useByMulPayMargin(String workBenchId) throws Exception;

    boolean useByMulRecMargin(String workBenchId) throws Exception;

    boolean findPayMargin(String marginbusinessno) throws Exception;

    boolean findRecMargin(String marginbusinessno) throws Exception;

    /**
     * 校验待结算数据是否有结算单生成
     * @throws Exception
     */
    void checkHasSettlementBill(String businessDetailsId) throws Exception;

    /**
     * 生成收到保证金的凭证
     * @param receiveMargin
     * @throws Exception
     */
    void generateVoucher(ReceiveMargin receiveMargin) throws Exception;

    /**
     * 生成支付保证金的凭证
     * @param payMargin
     * @throws Exception
     */
    void generateVoucher(PayMargin payMargin) throws Exception;
}
