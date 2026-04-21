package com.yonyoucloud.fi.cmp.common.service.cancelsettle;

import com.yonyou.yonbip.ctm.error.CtmErrorCode;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchTransferAccountCancelSettleServiceImpl;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCollectionCancelSettleServiceImpl;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundPaymentCancelSettleServiceImpl;

import java.util.HashMap;

public enum CancelSettlementServiceEnum {
    FUND_PAYMENT(CmpBusinessBillType.CashPayBill, FundPaymentCancelSettleServiceImpl.class),//资金付款
    FUND_COLLECTION(CmpBusinessBillType.CashRecBill, FundCollectionCancelSettleServiceImpl.class),//资金收款
    //IncomeAndExpenditure(CmpBusinessBillType.IncomeAndExpenditure, IncomeAndExpenditureCancelSettleServiceImpl.class),//统收统支协同单
    BatchTransferAccount(CmpBusinessBillType.BatchTransferAccount, BatchTransferAccountCancelSettleServiceImpl.class),//批量同名账户划转
    ;

    private CmpBusinessBillType businessBillType;
    private Class clazz;
    private static HashMap<CmpBusinessBillType, CancelSettlementServiceEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        CancelSettlementServiceEnum[] items = CancelSettlementServiceEnum.values();
        for (CancelSettlementServiceEnum item : items) {
            map.put(item.getType(), item);
        }
    }

    public static CancelSettlementServiceEnum find(CmpBusinessBillType businessBillType) {
        if (map == null) {
            initMap();
        }
        CancelSettlementServiceEnum billServiceEnum = map.get(businessBillType);
        if( billServiceEnum == null ){
            throw new com.yonyou.yonbip.ctm.error.CtmException(new CtmErrorCode("033-502-105001"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2088553C04700004","结算功能未注册具体实现！请联系研发人员排查！！"));
        }
        return billServiceEnum;
    }

    CancelSettlementServiceEnum(CmpBusinessBillType businessBillType,Class clazz) {
        this.businessBillType = businessBillType;
        this.clazz = clazz;
    }

    public CmpBusinessBillType getType() {
        return businessBillType;
    }


    public Class<ICmpOperationService> getClazz() {
        return clazz;
    }
}
