package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.ssc.img.sdk.bill.dto.SubmitBillRequest;
import com.yonyoucloud.ssc.img.sdk.bill.impl.DefaultImgBillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * @auther: shangxd
 * @date: 2022/8/17 10:02
 */
@Component
@Slf4j
public class SSCPFImgBillServiceImpl extends DefaultImgBillService {
    @Override
    public SubmitBillRequest getBillContent(BizObject bill, BillContext billContext) {

        String billNum = billContext.getBillnum();

        if(StringUtils.isNotEmpty(billNum)){
            // 资金付款单
            if(billNum.equals(IBillNumConstant.FUND_PAYMENT) || billNum.equals(IBillNumConstant.FUND_PAYMENTLIST)){
                return getSubmitBillRequestForFundPayment(bill, billContext);
            }else if(billNum.equals(IBillNumConstant.FUND_COLLECTION) || billNum.equals(IBillNumConstant.FUND_COLLECTIONLIST)){
                // 资金收款单
                return getSubmitBillRequestForFundCollection(bill, billContext);
            } else if(billNum.equals(IBillNumConstant.TRANSFERACCOUNT) || billNum.equals(IBillNumConstant.TRANSFERACCOUNTLIST)){
                // 转账单
                return getSubmitBillRequestForTransferAccount(bill, billContext);
            } else if(billNum.equals(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT) || billNum.equals(IBillNumConstant.CMP_BATCHTRANSFERACCOUNTLIST)){
                // 同名账户批量划转
                return getSubmitBillRequestForBatchTransferAccount(bill, billContext);
            }

        }

        return null;
    }

    /**
     * 通用工单字段映射
     * @param bill
     * @return
     */
    private SubmitBillRequest getSubmitBillRequestForFundPayment(BizObject bill, BillContext billContext){
        FundPayment fundPayment = null;
        try {
            fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bill.getString(ICmpConstant.ID));
        } catch (Exception e) {
            log.error("================fundPayment findById exception"+e.getMessage(),e);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SubmitBillRequest requestData = new SubmitBillRequest();
        // 单据编号
        requestData.setBillno(bill.getString(ICmpConstant.CODE));
        // 单据主键
        requestData.setBillid(bill.getString(ICmpConstant.ID));
        // 单据日期
        requestData.setBilldate(dateFormat.format(bill.getDate(ICmpConstant.VOUCHDATE)));
        // 制单人
        requestData.setBillmaker(fundPayment != null ? String.valueOf(fundPayment.getCreatorId()):null);
        // 单据业务id
        requestData.setBusinesskey("cmp_fundpayment_"+bill.getString(ICmpConstant.ID));
        // 单据formid
        requestData.setFormid(ICmpConstant.CM_CMP_FUND_PAYMENT);
        // 业务单元
        requestData.setPk_org(bill.getString(ICmpConstant.ACCENTITY));
        // 交易类型主键
        requestData.setPk_tradetype(bill.getString(ICmpConstant.TRADETYPE));
        // 金额
        requestData.setAmount(null);
        // 单据类型主键
        requestData.setPk_billtype(null);

        return requestData;

    }

    /**
     * 异构对接单字段映射
     * @param bill
     * @return
     */
    private SubmitBillRequest getSubmitBillRequestForFundCollection(BizObject bill, BillContext billContext){
        FundCollection fundCollection = null;
        try {
            fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bill.getString(ICmpConstant.ID));
        } catch (Exception e) {
            log.error("================fundPayment findById exception"+e.getMessage(),e);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SubmitBillRequest requestData = new SubmitBillRequest();
        // 单据编号
        requestData.setBillno(bill.getString(ICmpConstant.CODE));
        // 单据主键
        requestData.setBillid(bill.getString(ICmpConstant.ID));
        // 单据日期
        requestData.setBilldate(dateFormat.format(bill.getDate(ICmpConstant.VOUCHDATE)));
        // 制单人
        requestData.setBillmaker(fundCollection != null ? String.valueOf(fundCollection.getCreatorId()):null);
        // 单据业务id
        requestData.setBusinesskey("cmp_fundcollection_"+bill.getString(ICmpConstant.ID));
        // 单据formid
        requestData.setFormid(ICmpConstant.CM_CMP_FUND_COLLECTION);
        // 业务单元
        requestData.setPk_org(bill.getString(ICmpConstant.ACCENTITY));
        // 交易类型主键
        requestData.setPk_tradetype(bill.getString(ICmpConstant.TRADETYPE));
        // 金额
        requestData.setAmount(null);
        // 单据类型主键
        requestData.setPk_billtype(null);

        return requestData;

    }


    private SubmitBillRequest getSubmitBillRequestForTransferAccount(BizObject bill, BillContext billContext){
        TransferAccount transferAccount = null;
        try {
            transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bill.getString(ICmpConstant.ID));
        } catch (Exception e) {
            log.error("================        TransferAccount transferAccount = null;\n findById exception"+e.getMessage(),e);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SubmitBillRequest requestData = new SubmitBillRequest();
        // 单据编号
        requestData.setBillno(bill.getString(ICmpConstant.CODE));
        // 单据主键
        requestData.setBillid(bill.getString(ICmpConstant.ID));
        // 单据日期
        requestData.setBilldate(dateFormat.format(bill.getDate(ICmpConstant.VOUCHDATE)));
        // 制单人
        requestData.setBillmaker(transferAccount != null ? String.valueOf(transferAccount.getCreatorId()):null);
        // 单据业务id
        requestData.setBusinesskey("cm_transfer_account_"+bill.getString(ICmpConstant.ID));
        // 单据formid
        requestData.setFormid("CM.cm_transfer_account");
        // 业务单元
        requestData.setPk_org(bill.getString(ICmpConstant.ACCENTITY));
        // 交易类型主键
        requestData.setPk_tradetype(bill.getString(ICmpConstant.TRADETYPE));
        // 金额
        requestData.setAmount(null);
        // 单据类型主键
        requestData.setPk_billtype(null);

        return requestData;

    }

    /**
     * 通用工单字段映射
     * @param bill
     * @return
     */
    private SubmitBillRequest getSubmitBillRequestForBatchTransferAccount(BizObject bill, BillContext billContext){
        BatchTransferAccount batchTransferAccount = null;
        try {
            batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, bill.getString(ICmpConstant.ID));
        } catch (Exception e) {
            log.error("================batchTransferAccount findById exception:{}", e.getMessage(),e);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SubmitBillRequest requestData = new SubmitBillRequest();
        // 单据编号
        requestData.setBillno(bill.getString(ICmpConstant.CODE));
        // 单据主键
        requestData.setBillid(bill.getString(ICmpConstant.ID));
        // 单据日期
        requestData.setBilldate(dateFormat.format(bill.getDate(ICmpConstant.BILL_DATE)));
        // 制单人
        requestData.setBillmaker(batchTransferAccount != null ? String.valueOf(batchTransferAccount.getCreatorId()):null);
        // 单据业务id
        requestData.setBusinesskey(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT + "_"+bill.getString(ICmpConstant.ID));
        // 单据formid
        requestData.setFormid(ICmpConstant.BATCH_TRANSFER_ACCOUNT_FIRM_ID);
        // 业务单元
        requestData.setPk_org(bill.getString(ICmpConstant.ACCENTITY));
        // 交易类型主键
        requestData.setPk_tradetype(bill.getString(ICmpConstant.TRADETYPE_NEW));
        // 金额
        requestData.setAmount(null);
        // 单据类型主键
        requestData.setPk_billtype(null);

        return requestData;

    }
}
