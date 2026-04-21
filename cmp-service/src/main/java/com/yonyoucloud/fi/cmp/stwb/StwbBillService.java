package com.yonyoucloud.fi.cmp.stwb;

import org.imeta.orm.base.BizObject;

import java.util.List;

/**
 * 资金收付款单推送资金结算接口
 *
 * @author maliangn  2021-12-03
 *
 *
 */
public interface StwbBillService {

    void pushBill(List<BizObject> billList,boolean bCheck) throws Exception;
    void pushBillSimple(BizObject bizobject) throws Exception;
    void deleteBill(List<BizObject> billList) throws Exception ;

}
