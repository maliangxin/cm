package com.yonyoucloud.fi.cmp.common.digitalwallet.impl;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.digitalwallet.AbstractWalletHandler;
import com.yonyoucloud.fi.cmp.common.digitalwallet.DigitalWalletService;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author hanll
 * @Date 2025/8/30-16:26
 */
@Service
@Slf4j
public class DigitalWalletServiceImpl implements DigitalWalletService {

    @Autowired
    @Qualifier("fundcommonWalletHandler")
    private FundcommonWalletHandler fundcommonWalletHandler;


    /**
     * 保存校验
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject checkSave(CtmJSONObject param) throws Exception {
        // 单据编号获取不同的Service
        String billNo = param.getString("billNo");
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        CtmJSONArray rows = param.getJSONArray("data");
        String message;
        StringBuilder stringBuilder = new StringBuilder();
        switch (billNo) {
            // 资金付款
            case "cmp_fundpayment":
                for (int i = 0, j = 1; i < rows.size(); i++,j++) {
                    FundPayment_b fundPaymentB = new FundPayment_b();
                    CtmJSONObject rowData = rows.getJSONObject(i);
                    fundPaymentB.init(rowData);
                    message = fundcommonWalletHandler.checkOuterOrgSave(fundPaymentB);
                    if (StringUtils.isNotEmpty(message)) {
                        stringBuilder.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049A", "明细第【%d】行，%s") /* "明细第【%d】行，%s" */,j, message));
                    }
                }
                break;
            // 资金收款
            case "cmp_fundcollection":
                for (int i = 0, j = 1; i < rows.size(); i++, j++) {
                    FundCollection_b fundCollectionB = new FundCollection_b();
                    CtmJSONObject rowData = rows.getJSONObject(i);
                    fundCollectionB.init(rowData);
                    message = fundcommonWalletHandler.checkOuterOrgSave(fundCollectionB);
                    if (StringUtils.isNotEmpty(message)) {
                        stringBuilder.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540049A", "明细第【%d】行，%s") /* "明细第【%d】行，%s" */,j, message));
                    }
                }
                break;
            default:
                break;
        }
        if (stringBuilder.length() > 0) {
            ctmJSONObject.put("message", stringBuilder.toString());
        }
        return ctmJSONObject;
    }
}
