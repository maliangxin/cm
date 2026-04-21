package com.yonyoucloud.fi.cmp.util.business;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/6/15 11:14
 * @Copyright yonyou
 */
public class JournalUtil {


    /**
     *
     * @param bizObject
     * @param billContext
     * @return
     * @throws Exception
     */
    public static Journal createJounal(BizObject bizObject, BillContext billContext) throws Exception {
        String serverUrl = AppContext.getEnvConfig("fifrontservername");
        Journal journal = new Journal();
        journal.set(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
        journal.set("period", bizObject.get("period"));
        journal.set("bankaccount", bizObject.get("enterprisebankaccount"));
        journal.set("cashaccount", bizObject.get("cashaccount"));
        if (!StringUtils.isEmpty(bizObject.get("enterprisebankaccount"))) {
            Map<String, Object> enterprisebankaccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));
            if(enterprisebankaccount != null){
                journal.setBankaccountno((String) enterprisebankaccount.get("account"));
                journal.setBanktype((String) enterprisebankaccount.get("bank"));
            }else{
//                throw new CtmException(MessageUtils.getMessage("P_YS_FI_CM_0001263400") /* "未查询到对应的企业银行账户，保存失败！" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102285"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001263400","未查询到对应的企业银行账户，保存失败！") /* "未查询到对应的企业银行账户，保存失败！" */);
            }
        }
        if (!StringUtils.isEmpty(bizObject.get("cashaccount"))) {
            BillContext billContextFinBank = new BillContext();
            billContextFinBank.setFullname("bd.enterprise.OrgFinCashacctVO");
            billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QueryConditionGroup groupBank = QueryConditionGroup.and(QueryCondition.name("id").eq(bizObject.get("cashaccount")));
            List<Map<String, Object>> dataList = MetaDaoHelper.queryAll(billContextFinBank, "id,orgid,code,name,account,currency,enable,tenant", groupBank, null);
            journal.setCashaccountno((String) dataList.get(0).get("code"));
        }
        journal.set("currency", bizObject.get("currency"));
        //journal.set("dzdate", bizObject.get("dzdate"));
        journal.set("vouchdate", bizObject.get("vouchdate"));
        journal.set("description", bizObject.get("description"));
        journal.set("srcitem", EventSource.Cmpchase.getValue());
        journal.set("tradetype", bizObject.get("tradetype"));
        journal.set("exchangerate", bizObject.get("exchRate"));
        journal.set("settlemode", bizObject.get("settlemode"));
        journal.set("oribalance", bizObject.get(IBussinessConstant.ORI_SUM));
        journal.set("natbalance", bizObject.get(IBussinessConstant.NAT_SUM));
        if (bizObject.get("transeqno") != null) {
            journal.set("transeqno", bizObject.get("transeqno"));//交易流水号
        }
        journal.set("noteno", bizObject.get("noteno"));
        journal.set("customerbankaccount", bizObject.get("customerbankaccount"));
        journal.set("supplierbankaccount", bizObject.get("supplierbankaccount"));
        journal.set("employeeaccount", bizObject.get("staffBankAccount"));
        journal.set("caobject", bizObject.get("caobject"));
        journal.set("customer", bizObject.get("customer"));
        journal.set("supplier", bizObject.get("supplier"));
        journal.set("employee", bizObject.get("employee"));
        journal.set("dept", bizObject.get("dept"));
        journal.set("checkflag", false);
        journal.set("insidecheckflag", false);
        if (bizObject.get("paystatus") != null) {
            // 修改两方支付状态的赋值逻辑(付款工作台---日记账) 防止后台FormatCheckWalker类校验枚举报错
            payStatusToPaymentStatus(journal,bizObject);
        } else {
            journal.set("paymentstatus", PaymentStatus.NoPay.getValue());
        }
        journal.set("auditstatus", bizObject.get("auditstatus"));
        journal.set("settlestatus", bizObject.get("settlestatus"));
        journal.set("project", bizObject.get("project"));
        journal.set("costproject", bizObject.get("expenseitem"));
        journal.set("refund", false);
        journal.set("bookkeeper", AppContext.getCurrentUser().getId());
        journal.set("srcbillno", bizObject.get("code"));
        journal.set("srcbillitemid", bizObject.get("id").toString());
        journal.set("org", bizObject.get("org"));
        journal.set("billnum", bizObject.get("code"));
        journal.set("createTime", new Date());
        journal.set("createDate", new Date());
        journal.set("creator", AppContext.getCurrentUser().getId());
        journal.set("financialOrg", bizObject.get("financialOrg"));
        journal.set("tenant", bizObject.get("tenant"));
        journal.set("define1", bizObject.get("define1"));
        journal.set("define2", bizObject.get("define2"));
        journal.set("define3", bizObject.get("define3"));
        journal.set("define4", bizObject.get("define4"));
        journal.set("define5", bizObject.get("define5"));
        journal.set("define6", bizObject.get("define6"));
        journal.set("define7", bizObject.get("define7"));
        journal.set("define8", bizObject.get("define8"));
        journal.set("define9", bizObject.get("define9"));
        journal.set("define10", bizObject.get("define10"));
        if (!PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
            journal.setBillno(billContext.getBillnum());
        }
        if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.getEntityStatus().name().equals("Insert")
                && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))) {
            journal.set("checkflag", true);
        } else if (PayBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.getEntityStatus().name().equals("Insert")
                && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))) {
            journal.set("checkflag", true);
        }
        packingBill(journal,billContext.getFullname(),bizObject);
        journal.setTargeturl(serverUrl + "/meta/ArchiveList/" + billContext.getBillnum());
        //添加源头单据信息处理
        if(bizObject.get("topsrcbillno") != null){
            journal.set("topsrcbillno", bizObject.get("topsrcbillno").toString());
        }
        if(bizObject.get("srcbillid") != null){
            journal.set("topsrcbillid", bizObject.get("srcbillid").toString());
        }
        journal.set("topsrcitem", bizObject.get("srcitem"));
        journal.set("topbilltype", bizObject.get("billtype"));
        return journal;
    }

    /**
     *修改两方支付状态的赋值逻辑(付款工作台---日记账) 防止后台FormatCheckWalker类校验枚举报错
     *对应方式：PayStatus中:待支付(0)、预下单成功(1)、预下单失败(2) 对应 PaymentStatus 未支付(1)
     *        PayStatus中:支付成功(3) 对应 PaymentStatus 支付成功(2)
     *        PayStatus中:支付失败(4) 对应 PaymentStatus 支付失败(3)
     *        PayStatus中:支付中(5)、支付状态不明(6)、部分成功(8)  对应 PaymentStatus 支付状态不明(4)
     *        其余状态为未支付
     * @param journal
     * @param bizObject
     */
    public static void payStatusToPaymentStatus(Journal journal,BizObject bizObject){
        Short paystatus = Short.parseShort(bizObject.get("paystatus").toString());
        if(paystatus.equals(PayStatus.NoPay.getValue())|| paystatus.equals(PayStatus.PreSuccess.getValue())
                || paystatus.equals(PayStatus.PreFail.getValue())){
            journal.set("paymentstatus", PaymentStatus.NoPay.getValue());
        }else if(paystatus.equals(PayStatus.Success.getValue())){
            journal.set("paymentstatus", PaymentStatus.PayDone.getValue());
        }else if(paystatus.equals(PayStatus.Fail.getValue())){
            journal.set("paymentstatus", PaymentStatus.PayFail.getValue());
        }else if(paystatus.equals(PayStatus.Paying.getValue())||paystatus.equals(PayStatus.PayUnknown.getValue())
                || paystatus.equals(PayStatus.PartSuccess.getValue())){
            journal.set("paymentstatus", PaymentStatus.UnkownPay.getValue());
        }
        else{
            journal.set("paymentstatus", PaymentStatus.NoPay.getValue());
        }
    }

    /**
     * 对于收付款单单据进行再处理，设置默认数据
     * @param journal
     * @param fullname
     * @param bizObject
     * @return
     */
    private static Journal packingBill(Journal journal,String fullname,BizObject bizObject){
        switch (fullname){
            case PayBill.ENTITY_NAME :
                journal.set("direction", Direction.Credit.getValue());
                journal.set("debitoriSum", BigDecimal.ZERO);
                journal.set("debitnatSum",BigDecimal.ZERO);
                journal.set("creditoriSum",bizObject.get("oriSum"));
                journal.set("creditnatSum",bizObject.get("natSum"));
                journal.set("rptype", RpType.PayBill.getValue());
                journal.setBillno(IBillNumConstant.PAYMENT);
                journal.setServicecode(IServicecodeConstant.PAYMENTBILL);
                journal.set("billtype", EventType.PayMent.getValue());
                break;
            case ReceiveBill.ENTITY_NAME :
                journal.set("direction", Direction.Debit.getValue());
                journal.set("debitoriSum",bizObject.get("oriSum"));
                journal.set("debitnatSum",bizObject.get("natSum"));
                journal.set("creditoriSum",BigDecimal.ZERO);
                journal.set("creditnatSum",BigDecimal.ZERO);
                journal.set("rptype", RpType.ReceiveBill.getValue());
                journal.setServicecode(IServicecodeConstant.RECEIVEBILL);
                journal.set("billtype", EventType.ReceiveBill.getValue());
                break;
            default:

        }
        return journal;
    }




}
