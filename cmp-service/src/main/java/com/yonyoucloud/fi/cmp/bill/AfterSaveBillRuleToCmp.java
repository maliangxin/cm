package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.JournalUtil;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收款单与付款单业务规则
 */
@Slf4j
@Component
public abstract class AfterSaveBillRuleToCmp extends AbstractCommonRule {

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;

    @Autowired
    CooperationFileService cooperationFileService;
    /**
     * 区别于收款与付款转换的逻辑字段转换
     *
     * @param journal
     * @return
     */
    public abstract Journal generateJournal(Journal journal, BizObject bizObject);

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            //转换过程
            Journal journal = createJounal(bizObject, billContext);

            //应收应付单据把账户id
            if (StringUtils.isEmpty(journal.getBankaccount()) && StringUtils.isEmpty(journal.getCashaccount()) && bizObject.get("id") == null) {
                return new RuleExecuteResult();
            }
            //回退逻辑
            CmpWriteBankaccUtils.delAccountBook(bizObject.get("id").toString());


            if (bizObject.get("paystatus") != null && bizObject.getShort("paystatus").compareTo(PayStatus.Success.getValue()) == 0) {
                if (bizObject.get("dzdate") != null) {
                    journal.setDzdate(bizObject.get("dzdate"));
                } else {
                    if (BillInfoUtils.getBusinessDate() != null) {
                        journal.setDzdate(BillInfoUtils.getBusinessDate());
                    } else {
                        journal.setDzdate(new Date());
                    }
                }

                // 生成凭证，回调外部系统，重新查询包含表体数据，保存前事务未提交，vo状态赋值下
                PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, bizObject.getId(), 3);
                payBill.set("_entityName", bizObject.getEntityName());
                payBill.setSettlestatus(SettleStatus.alreadySettled);
                payBill.setPaystatus(PayStatus.Success);
                payBill.setDzdate(new Date());
                payBill.setSettledate(new Date());
                CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(payBill);
                if (!generateResult.getBoolean("dealSucceed")) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801D0","发送会计平台失败：") /* "发送会计平台失败：" */
                                    /* "发送会计平台失败：" */ + generateResult.get("message"));
                }
                // 支付变更成功，回调外系统
                payCallback(payBill);
                try{
                    String systemCode = SystemCodeUtil.getSystemCode(payBill);
                    if(payBill.getSrcitem()!= null ){
                        //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                        if(payBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)){
                            // 调用应收结算规则
                            BillContext billContextnew = new BillContext();
                            billContextnew.setBillnum("cmp_payment");
                            Map<String,Object> paramMap = new HashMap<>();
                            paramMap.put("paystatus",payBill.getPaystatus().getValue());
                            paramMap.put("paydate",payBill.getPaydate());
                            paramMap.put("settlemode",payBill.getSettlemode());
                            paramMap.put("enterprisebankaccount",payBill.getEnterprisebankaccount());
                            paramMap.put("cashaccount",payBill.getCashaccount());
                            paramMap.put("srcbillid",payBill.getSrcbillid());
                            paramMap.put("settlestatus", payBill.getSettlestatus().getValue());
                            paramMap.put("settledate", payBill.getSettledate());
                            billContextnew.setAction("arapSettle");
                            BillBiz.executeRule("arapSettle",billContextnew,paramMap);
                        }
                    }
                }catch (Exception e){
                    log.error("##   #####   单据结算同步应收失败,执行回滚   ##  ######");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100237"),e.getMessage());
                }
            } else {
                if (bizObject.get("billtype").toString().equals(EventType.CashMark.getValue() + "")) {
                    if (bizObject.getEntityStatus().name().equals("Insert")) {
                        if (BillInfoUtils.getBusinessDate() != null) {
                            journal.setDzdate(BillInfoUtils.getBusinessDate());
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_PATTERN);
                            String beforeDate = sdf.format(new Date());
                            journal.setDzdate(new Date());
                        }
                    }
                }

            }
            if (bizObject.get("billtype").toString().equals(EventType.CashMark.getValue() + "")) {
                if (bizObject.getEntityStatus().name().equals("Insert")) {
                    getReceiveBill(billContext, bizObject);
                }
            }

            cmpWriteBankaccUtils.addAccountBook(journal);

            if(PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
                boolean statusFlag = ValueUtils.isNotEmptyObj(bizObject.get("status")) && bizObject.get("status").toString().equals("0");
                // 来源是付款申请的单据同步附件信息
                if (bizObject.getEntityStatus().name().equals("Insert") &&
                        null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals("59") && statusFlag) {
                    QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                    querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").eq(bizObject.getId())));
                    try {
                        List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                       if (CollectionUtils.isNotEmpty(mapList)) {
                           Map<String, Object> e = mapList.get(0);
                           Long srcbillitemid = Long.parseLong(e.get("srcbillitemid").toString());
                           PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
                           if (null != payApplicationBill_b) {
                               Long mainid = payApplicationBill_b.getMainid();
                               // 附件拷贝：付款申请单->付款单
                               cooperationFileService.copyFiles("yonbip-fi-ctmcmp", mainid.toString(),
                                       "yonbip-fi-ctmcmp", bizObject.getId().toString(), null, null);
                           }
                       }

                    } catch (Exception ex){
                        log.error("调整已付金额数据失败!:" + ex.getMessage());//@notranslate
                    }
                }
            }

        }
        return new RuleExecuteResult();
    }


    /**
     * 支付回调外部系统
     *
     * @param payBill
     */
    private void payCallback(PayBill payBill) throws Exception {
        if (payBill.getCallback() == null) {
            return;
        }
        if(ValueUtils.isNotEmpty(payBill.getSrcflag())&&"Sifang".equals(payBill.getSrcflag())){
            if (payBill.getPaystatus()==PayStatus.Success||payBill.getPaystatus()==PayStatus.Paying) {
                CtmJSONObject params = new CtmJSONObject();
                params.put("id", payBill.getId() + "");
                params.put("srcbillid", payBill.getSrcbillid());
                params.put("paystatus", String.valueOf(payBill.getPaystatus().getValue()));
                if (payBill.getPaydate()!=null){
                    String payTime= DateUtils.dateFormat(payBill.getPaydate(),"yyyy-MM-dd HH:mm:ss");
                    params.put("paytime",payTime);
                }
                String url = AppContext.getEnvConfig("ReceiptDetail_ADDR_REQSEQ");         //也要改
                if (payBill.getTranseqno() != null) {    // 修改银行交易流水号为请求流水号 先不改
                    url = url + "?bankseq=" + payBill.getTranseqno() + "&bankid=" + payBill.getEnterprisebankaccount();
                }
                params.put("url", url);
                SifangHttpUtils.SifangCallBack(payBill.getCallback(), params);
            }
        }
        else{
            //回调外部系统（例：费用）
            CtmJSONObject paramsCallback = new CtmJSONObject();
            Long id= payBill.getId();
            payBill.setId(payBill.getSrcbillid());
            paramsCallback.put("pk", payBill.getSrcbillid());
            paramsCallback.put("paystatus", String.valueOf(payBill.getPaystatus().getValue()));
            paramsCallback.put("headvo", CtmJSONObject.toJSONString(payBill));
            String callbackUrl=payBill.getCallback()+"&token="+ InvocationInfoProxy.getYhtAccessToken();
            String responseStr = HttpTookit.doPostWithJson(callbackUrl,CtmJSONObject.toJSONString(paramsCallback), null);
            payBill.setId(id);
            CtmJSONObject result = CtmJSONObject.parseObject(responseStr);
            if (!"0".equals(result.getString("code"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100238"),ValueUtils.isNotEmptyObj(result.getString("msg"))
                        ? result.getString("msg") :result.getString("message"));
            }
        }
    }


    private Journal createJounal(BizObject bizObject, BillContext billContext) throws Exception {
//        String serverUrl = AppContext.getEnvConfig("fifrontservername");
        Journal journal = new Journal();
        journal.set(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
        journal.set("period", bizObject.get("period"));
        journal.set("bankaccount", bizObject.get("enterprisebankaccount"));
        journal.set("cashaccount", bizObject.get("cashaccount"));
        if (!StringUtils.isEmpty(bizObject.get("enterprisebankaccount"))) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.getString("enterprisebankaccount"));
            journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
            journal.setBanktype(enterpriseBankAcctVO.getBank());
        }
        if (!StringUtils.isEmpty(bizObject.get("cashaccount"))) {
            EnterpriseCashVO enterpriseCashVO = baseRefRpcService.queryEnterpriseCashAcctById(bizObject.getString("cashaccount"));
            journal.setCashaccountno(enterpriseCashVO.getCode());
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
            JournalUtil.payStatusToPaymentStatus(journal,bizObject);
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
        //其他--那云昊
        if(CaObject.Other.getValue()==  Short.parseShort(bizObject.get("caobject").toString())){
            journal.set("othername", bizObject.get("retailer"));
        }
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
        if (PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
            journal.setBillno(IBillNumConstant.PAYMENT);
        }else {
            journal.setBillno(billContext.getBillnum());
        }
        if(IBillNumConstant.RECEIVE_BILL_UPDATE.equals(billContext.getBillnum())){
            journal.setBillno(IBillNumConstant.RECEIVE_BILL);
        }
        if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())) {
            journal.setServicecode("ficmp0003");
            journal.set("billtype", EventType.ReceiveBill.getValue());
        } else if (PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
            journal.setServicecode("ficmp0009");
            journal.set("billtype", EventType.PayMent.getValue());
        }

        if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.getEntityStatus().name().equals("Insert")
                && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))) {
            journal.set("checkflag", true);
        } else if (PayBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.getEntityStatus().name().equals("Insert")
                && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))) {
            journal.set("checkflag", true);
        }
        //添加源头单据信息处理
        //事项来源非现金管理，或者事项类型为付款申请的数据
        if (!bizObject.get("srcitem").toString().equals(String.valueOf(EventSource.Cmpchase.getValue())) || bizObject.get("billtype").toString().equals(String.valueOf(EventType.PayApplyBill.getValue()))) {
            if(bizObject.get("topsrcbillno") != null){
                journal.set("topsrcbillno", bizObject.get("topsrcbillno").toString());
            }
            if(bizObject.get("srcbillid") != null){
                journal.set("topsrcbillid", bizObject.get("srcbillid").toString());
            }
        }
        journal.set("topsrcitem", bizObject.get("srcitem"));
        journal.set("topbilltype", bizObject.get("billtype"));

//        journal.setTargeturl(serverUrl + "/meta/ArchiveList/" + billContext.getBillnum());
        this.generateJournal(journal, bizObject);
        return journal;
    }

    /**
     * 结算 登记日记账
     *
     * @param bizObject
     * @throws Exception
     */
    public void getReceiveBill(BillContext billContext, BizObject bizObject) throws Exception {
        if (bizObject.get("billtype").toString().equals(EventType.CashMark.getValue() + "")) {
            if (bizObject.getEntityStatus().name().equals("Insert")) {
                if (bizObject.get("srcitem").toString().equals(String.valueOf(EventSource.Cmpchase.getValue()))) {//现金
                    generateVoucher(billContext, bizObject);
                }
            }
        }
    }

    /**
     * 生成凭证逻辑
     *
     * @param bizObject
     * @throws Exception
     */
    public void generateVoucher(BillContext billContext, BizObject bizObject) throws Exception {
        CtmJSONObject generateResult = new CtmJSONObject();
        Map<String, Object> map = new HashMap<>();
        Date dzdate = new Date();
        Date settledate = new Date();
        if (null != BillInfoUtils.getBusinessDate()) {
            dzdate = BillInfoUtils.getBusinessDate();
            settledate = BillInfoUtils.getBusinessDate();
        }

        if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())) {
            ReceiveBill receiveBills = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, bizObject.getId());
            receiveBills.put("_entityName", ReceiveBill.ENTITY_NAME);
            receiveBills.put("dzdate", dzdate);
            receiveBills.put("voucherstatus",  VoucherStatus.Empty.getValue());
            generateResult = cmpVoucherService.generateVoucherWithResult(receiveBills);
        }
        if (PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
            PayBill payBills = MetaDaoHelper.findById(PayBill.ENTITY_NAME, bizObject.getId());
            payBills.put("_entityName", PayBill.ENTITY_NAME);
            payBills.put("dzdate", dzdate);
            payBills.put("voucherstatus",  VoucherStatus.Empty.getValue());
            generateResult = cmpVoucherService.generateVoucherWithResult(payBills);
        }
        if (!generateResult.getBoolean("dealSucceed")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100239"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900033", "发送会计平台失败：%s") /* "发送会计平台失败：%s" */, generateResult.get("message")));

        }
        if (generateResult.get("genVoucher") != null && !generateResult.getBoolean("genVoucher")) {
            map.put("voucherstatus", VoucherStatus.NONCreate.getValue());
        }
        map.put("dzdate", dzdate);
        map.put("settledate", settledate);
        map.put("id", bizObject.getId());
        if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())) {
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.ReceivebillMapper.updatebill", map);
        }
        if (PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.ReceivebillMapper.updatepay", map);
        }

    }

}
