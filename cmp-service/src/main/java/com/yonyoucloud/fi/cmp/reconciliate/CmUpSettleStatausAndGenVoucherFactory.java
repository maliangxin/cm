package com.yonyoucloud.fi.cmp.reconciliate;


import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;

public class CmUpSettleStatausAndGenVoucherFactory {
    public static UpSettleStatusAndGenVouchService getInstance(String entityName){
        UpSettleStatusAndGenVouchService service = null;
        switch(entityName){
            case ReceiveBill.ENTITY_NAME://收款
                //service = new ReceiveUpSettleStatusAndGenVouchServiceImpl();
                service = AppContext.getBean(ReceiveUpSettleStatusAndGenVouchServiceImpl.class);
                break;
            case PayBill.ENTITY_NAME://付款
                service = AppContext.getBean(PayBillUpSettleStatusAndGenVouchServiceImpl.class);
                break;
            case TransferAccount.ENTITY_NAME://转账单
                service = AppContext.getBean(TransAccUpSettleStatusAndGenVouchServiceImpl.class);
                break;
            case CurrencyExchange.ENTITY_NAME://外币兑换
                service = AppContext.getBean(CurrChgeUpSettleStatusAndGenVouchServiceImpl.class);
                break;
            case Salarypay.ENTITY_NAME://薪资支付
                service = AppContext.getBean(SalPayUpSettleStatusAndGenVouchServiceImpl.class);
                break;
                default:
                    break;
        }
        return service;
    }
}
