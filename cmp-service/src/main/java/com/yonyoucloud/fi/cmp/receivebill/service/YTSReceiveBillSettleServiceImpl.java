package com.yonyoucloud.fi.cmp.receivebill.service;

import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IActionConstant;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/6/2 10:05
 * @Copyright yonyou
 */
@Service
public class YTSReceiveBillSettleServiceImpl{

    @Transactional
    public BizObject settleBill(BizObject dto, Map params) throws Exception {

        BillContext billContext = new BillContext();
        billContext.setBillnum(IBillNumConstant.RECEIVE_BILL);
        billContext.setAction(IActionConstant.SETTLEACTION);
        BillBiz.executeRule(IActionConstant.SETTLEACTION,billContext,params);

        return dto;
    }

    @Transactional
    public BizObject unSettleBill(BizObject dto, Map params) throws Exception {

        BillContext billContext = new BillContext();
        billContext.setBillnum(IBillNumConstant.RECEIVE_BILL);
        billContext.setAction(IActionConstant.UNSETTLEACTION);
        BillBiz.executeRule(IActionConstant.UNSETTLEACTION,billContext,params);
        return dto;
    }

}
