package com.yonyoucloud.fi.cmp.paymentbill.service;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.arap.service.account.IArapSettleAccountService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PaymentBillSettleBO;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import lombok.NonNull;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/11/12 10:05
 * @Copyright yonyou
 * @Desc 针对付款工作台，将结算及取消结算流程拆分出来
 */
@Slf4j
@Service
public class PaymentSettleServiceImpl implements PaymentSettleService {

    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    private JournalService journalService;
    @Autowired
    private SettlementService settlementService;
    @Autowired
    YTSPayBillSettleServiceImpl ytsPayBillSettleServiceImpl;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Resource
    private IApplicationService appService;
    // 结算方式缓存
    private static final @NonNull Cache<String, Map<String, Object>> settlemodeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    /**
     * 线下支付
     * @param rowData
     * @param batchProcess
     * @param date
     * @param userId
     * @param maxSettleDateMaps
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public PaymentBillSettleBO processOffLinePayItem(CtmJSONObject rowData, boolean batchProcess, Date date, long userId, Map<String, Date> maxSettleDateMaps, List<YmsLock> ymsLockList )throws Exception{
        PaymentBillSettleBO.PaymentBillSettleBOBuilder builder = PaymentBillSettleBO.builder();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(rowData.getLong("id").toString());
        if (null == ymsLock) {
            builder.failedId(rowData.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + rowData.getString("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180326","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100259"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + rowData.getString("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180326","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
            }
            return builder.build();

        }
        ymsLockList.add(ymsLock);
        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, rowData.getLong("id"), 3);
        Long id = rowData.getLong("id");
        if (payBill == null) {
            builder.failedId(id.toString())
                    .message(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00139", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100260"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00139", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
            }
            return builder.build();
        }
        //校验该账户是否允许透支
        String enterprisebankaccount = payBill.getEnterprisebankaccount();
        if(enterprisebankaccount!=null){
            if(!CmpWriteBankaccUtils.checkAccOverDraft(payBill.getAccentity(),enterprisebankaccount, payBill.getCurrency())){
                builder.failedId(id.toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                if (!batchProcess) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100261"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                }
                return builder.build();
            }
        }
        String cashaccount = payBill.getCashaccount();
        if(cashaccount!=null){
            if(!CmpWriteBankaccUtils.checkAccOverDraft(payBill.getAccentity(),cashaccount, payBill.getCurrency())){
                builder.failedId(id.toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                if (!batchProcess) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100261"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                }
                return builder.build();
            }
        }
        if (rowData.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
            builder.failedId(id.toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031D","】数据无效，请刷新后重试") /* "】数据无效，请刷新后重试" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100262"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031F","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            }
            return builder.build();
        }
        Short auditStatus = payBill.getShort("auditstatus");
        if (auditStatus!= AuditStatus.Complete.getValue()) {
            builder.failedId(rowData.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180321","】的【审核状态】不能进行线下支付操作") /* "】的【审核状态】不能进行线下支付操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100263"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180323","该单据的【审核状态】不能进行线下支付操作") /* "该单据的【审核状态】不能进行线下支付操作" */);
            }
            return builder.build();
        }

        if(date != null && payBill.getAuditDate() != null&& date.compareTo(payBill.getAuditDate()) < 0){
            builder.failedId(payBill.getId().toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180324","】线下支付日期小于审批日期，不能结算！") /* "】线下支付日期小于审批日期，不能结算！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100259"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180324","】线下支付日期小于审批日期，不能结算！") /* "】线下支付日期小于审批日期，不能结算！" */);
            }
            return builder.build();
        }
        Short payStatus = payBill.getShort("paystatus");
        if (payStatus != PayStatus.NoPay.getValue() && payStatus != PayStatus.PreFail.getValue() && payStatus != PayStatus.Fail.getValue()) {
            builder.failedId(rowData.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180329","】的【支付状态】不能进行线下支付操作") /* "】的【支付状态】不能进行线下支付操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100264"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032A","该单据的【支付状态】不能进行线下支付操作") /* "该单据的【支付状态】不能进行线下支付操作" */);
            }
            return builder.build();
        }
        //结算方式为票据方式时，不校验付方账户
        boolean notemode = false;
        if(payBill.getSettlemode() != null){
            notemode = getSettlemodeById(payBill.getSettlemode().toString()).get("serviceAttr").equals(2);
        }
        if (StringUtils.isEmpty(payBill.getEnterprisebankaccount()) && StringUtils.isEmpty(payBill.getCashaccount()) && !notemode){
            builder.failedId( rowData.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032B","】的【付款账户】为空不能进行线下支付操作") /* "】的【付款账户】为空不能进行线下支付操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100265"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032E","该单据的【付款账户】为空不能进行线下支付操作") /* "该单据的【付款账户】为空不能进行线下支付操作" */);
            }
            return builder.build();
        }
        //校验结算方式不能为空
        if(payBill.getSettlemode() == null){
            builder.failedId( rowData.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180331","结算方式不能为空！") /* "结算方式不能为空！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100266"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180331","结算方式不能为空！") /* "结算方式不能为空！" */);
            }
            return builder.build();
        }
        //校验结算日期是否已日结
        Date maxSettleDate = null;
        if(maxSettleDateMaps.containsKey(payBill.getAccentity())){
            maxSettleDate = maxSettleDateMaps.get(payBill.getAccentity());
        }else{
            maxSettleDate = settlementService.getMaxSettleDate(payBill.getAccentity());
            maxSettleDateMaps.put(payBill.getAccentity(), maxSettleDate);
        }
        if(SettleCheckUtil.checkDailySettlement(maxSettleDate, false)){
            builder.failedId( rowData.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031E","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100267"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031E","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            }
            return builder.build();
        }
        payBill.setPaystatus(PayStatus.OfflinePay);
        payBill.setPayman(userId);
        if(BillInfoUtils.getBusinessDate() != null) {
            payBill.setSettledate(new Date());
            payBill.setPaydate(new Date());
            payBill.setDzdate(BillInfoUtils.getBusinessDate());
        }else {
            payBill.setPaydate(new Date());
            payBill.setSettledate(new Date());
            payBill.setDzdate(new Date());
        }
        payBill.setSettlestatus(SettleStatus.alreadySettled);
        payBill.set("oldvoucherstatus",payBill.getVoucherstatus().getValue());
        payCallback(payBill);
        payBill.set("_entityName",PayBill.ENTITY_NAME);
        //判断是否开通总账
        boolean enableGL = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EGL_APP_CODE);
        //如果开通了总账 才会进行调用
        if (enableGL) {
            CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(payBill);
            if (!generateResult.getBoolean("dealSucceed")) {
                builder.failedId(rowData.getLong("id").toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180332","】发送会计平台失败，线下支付失败！") /* "】发送会计平台失败，线下支付失败！" */);
                if (!batchProcess) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100268"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180334","发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
                }
                return builder.build();
            }
            if (generateResult.get("genVoucher")!=null&&!generateResult.getBoolean("genVoucher")) {
                payBill.setVoucherstatus(VoucherStatus.NONCreate);
            }
        }
        try{
            String systemCode = SystemCodeUtil.getSystemCode(payBill);
            if(payBill.getSrcitem()!= null){
                //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                if(payBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)){
                    // 调用应付取消线下支付规则
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("paystatus", payBill.getPaystatus().getValue());
                    paramMap.put("paydate", payBill.getPaydate());
                    paramMap.put("settlemode", payBill.getSettlemode());
                    paramMap.put("enterprisebankaccount", payBill.getEnterprisebankaccount());
                    paramMap.put("cashaccount", payBill.getCashaccount());
                    paramMap.put("srcbillid", payBill.getSrcbillid());
                    paramMap.put("settlestatus", payBill.getSettlestatus().getValue());
                    paramMap.put("settledate", payBill.getSettledate());
                    paramMap.put("dzdate", payBill.getDzdate());
                    ytsPayBillSettleServiceImpl.settleBill(payBill,paramMap);
                }
            }
        }catch (Exception e){
            log.error("##   #####   单据结算同步应收失败,执行回滚   ##  ######");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100269"),e.getMessage());
        }

        rowData.put("settlestatus", SettleStatus.alreadySettled.getValue());
        rowData.put("paystatus", PayStatus.OfflinePay.getValue());
        rowData.put("dzdate", payBill.getDzdate());
        rowData.put("paydate", payBill.getPaydate());
        builder.journalList(journalService.updateJournalByBill(payBill));
        builder.updateBill(payBill);
        //TODO 动作待预置，先用支付成功进行测试
        SendBizMessageUtils.sendBizMessage(rowData, "cmp_paymentlist", "paysucceed");
        ctmcmpBusinessLogService.saveBusinessLog(payBill, MetadataURI.PaymentBill, IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.OFFLINE_PAY);
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        return builder.build();
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public PaymentBillSettleBO processOffLinePayItem(PayBill paramPayBill, boolean batchProcess, Date date, long userId, Map<String, Date> maxSettleDateMaps)throws Exception{
        PaymentBillSettleBO.PaymentBillSettleBOBuilder builder = PaymentBillSettleBO.builder();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(paramPayBill.getId());
        if (null == ymsLock) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + paramPayBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180326","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100259"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + paramPayBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180326","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
            }
            return builder.build();

        }
        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, paramPayBill.getId(), 3);
        String id = paramPayBill.getId();
        if (payBill == null) {
            builder.failedId(id)
                    .message(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00139", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, paramPayBill.getCode()));
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100260"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00139", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, paramPayBill.getCode()));
            }
            return builder.build();
        }
        //校验该账户是否允许透支
        String enterprisebankaccount = payBill.getEnterprisebankaccount();
        if(enterprisebankaccount!=null){
            if(!CmpWriteBankaccUtils.checkAccOverDraft(payBill.getAccentity(),enterprisebankaccount, payBill.getCurrency())){
                builder.failedId(id.toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                if (!batchProcess) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100261"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                }
                return builder.build();
            }
        }
        String cashaccount = payBill.getCashaccount();
        if(cashaccount!=null){
            if(!CmpWriteBankaccUtils.checkAccOverDraft(payBill.getAccentity(),cashaccount, payBill.getCurrency())){
                builder.failedId(id)
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                if (!batchProcess) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100261"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180319","账户余额不足") /* "账户余额不足" */);
                }
                return builder.build();
            }
        }
        if (paramPayBill.getPubts().compareTo(payBill.getPubts()) != 0) {
            builder.failedId(id)
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031D","】数据无效，请刷新后重试") /* "】数据无效，请刷新后重试" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100262"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031F","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            }
            return builder.build();
        }
        Short auditStatus = payBill.getShort("auditstatus");
        if (auditStatus!= AuditStatus.Complete.getValue()) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180321","】的【审核状态】不能进行线下支付操作") /* "】的【审核状态】不能进行线下支付操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100263"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180323","该单据的【审核状态】不能进行线下支付操作") /* "该单据的【审核状态】不能进行线下支付操作" */);
            }
            return builder.build();
        }

        if(date != null && payBill.getAuditDate() != null&& date.compareTo(payBill.getAuditDate()) < 0){
            builder.failedId(payBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180324","】线下支付日期小于审批日期，不能结算！") /* "】线下支付日期小于审批日期，不能结算！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100259"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180324","】线下支付日期小于审批日期，不能结算！") /* "】线下支付日期小于审批日期，不能结算！" */);
            }
            return builder.build();
        }
        Short payStatus = payBill.getShort("paystatus");
        if (payStatus != PayStatus.NoPay.getValue() && payStatus != PayStatus.PreFail.getValue() && payStatus != PayStatus.Fail.getValue()) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180329","】的【支付状态】不能进行线下支付操作") /* "】的【支付状态】不能进行线下支付操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100264"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032A","该单据的【支付状态】不能进行线下支付操作") /* "该单据的【支付状态】不能进行线下支付操作" */);
            }
            return builder.build();
        }
        //结算方式为票据方式时，不校验付方账户
        boolean notemode = false;
        if(payBill.getSettlemode() != null) {
            notemode = getSettlemodeById(payBill.getSettlemode().toString()).get("serviceAttr").equals(2);
        }
        if (StringUtils.isEmpty(payBill.getEnterprisebankaccount()) && StringUtils.isEmpty(payBill.getCashaccount()) && !notemode) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032B","】的【付款账户】为空不能进行线下支付操作") /* "】的【付款账户】为空不能进行线下支付操作" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100265"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032E","该单据的【付款账户】为空不能进行线下支付操作") /* "该单据的【付款账户】为空不能进行线下支付操作" */);
            }
            return builder.build();
        }
        //校验结算方式不能为空
        if (payBill.getSettlemode() == null) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180331","结算方式不能为空！") /* "结算方式不能为空！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100266"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180331","结算方式不能为空！") /* "结算方式不能为空！" */);
            }
            return builder.build();
        }
        //校验结算日期是否已日结
        Date maxSettleDate = null;
        if(maxSettleDateMaps.containsKey(payBill.getAccentity())){
            maxSettleDate = maxSettleDateMaps.get(payBill.getAccentity());
        }else{
            maxSettleDate = settlementService.getMaxSettleDate(payBill.getAccentity());
            maxSettleDateMaps.put(payBill.getAccentity(), maxSettleDate);
        }
        if (SettleCheckUtil.checkDailySettlement(maxSettleDate, false)) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031E","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100267"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031E","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            }
            return builder.build();
        }
        payBill.setPaystatus(PayStatus.OfflinePay);
        payBill.setPayman(userId);
        if(BillInfoUtils.getBusinessDate() != null) {
            payBill.setSettledate(new Date());
            payBill.setPaydate(new Date());
            payBill.setDzdate(BillInfoUtils.getBusinessDate());
        }else {
            payBill.setPaydate(new Date());
            payBill.setSettledate(new Date());
            payBill.setDzdate(new Date());
        }
        payBill.setSettlestatus(SettleStatus.alreadySettled);
        payBill.set("oldvoucherstatus",payBill.getVoucherstatus().getValue());
        payCallback(payBill);
        payBill.set("_entityName",PayBill.ENTITY_NAME);

        CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(payBill);

        try{
            String systemCode = SystemCodeUtil.getSystemCode(payBill);
            if(payBill.getSrcitem()!= null){
                //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                if(payBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)){
                    // 调用应付取消线下支付规则
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("paystatus", payBill.getPaystatus().getValue());
                    paramMap.put("paydate", payBill.getPaydate());
                    paramMap.put("settlemode", payBill.getSettlemode());
                    paramMap.put("enterprisebankaccount", payBill.getEnterprisebankaccount());
                    paramMap.put("cashaccount", payBill.getCashaccount());
                    paramMap.put("srcbillid", payBill.getSrcbillid());
                    paramMap.put("settlestatus", payBill.getSettlestatus().getValue());
                    paramMap.put("settledate", payBill.getSettledate());
                    paramMap.put("dzdate", payBill.getDzdate());
                    ytsPayBillSettleServiceImpl.settleBill(payBill,paramMap);
                }
            }
        }catch (Exception e){
            log.error("##   #####   单据结算同步应收失败,执行回滚   ##  ######");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100270"),e.getMessage());
        }

        if (!generateResult.getBoolean("dealSucceed")) {
            builder.failedId(paramPayBill.getId())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180332","】发送会计平台失败，线下支付失败！") /* "】发送会计平台失败，线下支付失败！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100268"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180334","发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
            }
            return builder.build();
        }
        if (generateResult.get("genVoucher")!=null&&!generateResult.getBoolean("genVoucher")) {
            payBill.setVoucherstatus(VoucherStatus.NONCreate);
        }
        paramPayBill.setSettlestatus(SettleStatus.alreadySettled);
        paramPayBill.setPaystatus(PayStatus.OfflinePay);
        paramPayBill.setDzdate(payBill.getDzdate());
        paramPayBill.setPaydate(payBill.getPaydate());
        builder.journalList(journalService.updateJournalByBill(payBill));
        builder.updateBill(payBill);
        //TODO 动作待预置，先用支付成功进行测试
        //SendBizMessageUtils.sendBizMessage(payBill, "cmp_paymentlist", "paysucceed");
        SendBizMessageUtils.sendBizMessage(paramPayBill, "cmp_paymentlist", "paysucceed");
        ctmcmpBusinessLogService.saveBusinessLog(payBill, MetadataURI.PaymentBill, IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.OFFLINE_PAY);
        JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        return builder.build();
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public PaymentBillSettleBO processCancelOffLinePayItem(CtmJSONObject row, boolean batchProcess, Map<String, Date> maxSettleDateMaps)throws Exception {
        PaymentBillSettleBO.PaymentBillSettleBOBuilder builder = PaymentBillSettleBO.builder();

        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, row.getLong("id"), 3);
        if (row.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
            builder.failedId(row.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031D","】数据无效，请刷新后重试") /* "】数据无效，请刷新后重试" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100262"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031F","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            }
            return builder.build();
        }
        if (payBill.getPaystatus().getValue()!= PayStatus.OfflinePay.getValue()){
            builder.failedId(row.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180328","】的【支付状态】不能进行取消线下支付操作") /* "】的【支付状态】不能进行取消线下支付操作" */);
            if (!batchProcess) {
                throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180328","】的【支付状态】不能进行取消线下支付操作") /* "】的【支付状态】不能进行取消线下支付操作" */);
            }
            return builder.build();
        }
        //已日结后不能取消线下支付
        Date maxSettleDate = null;
        if(maxSettleDateMaps.containsKey(payBill.getAccentity())){
            maxSettleDate = maxSettleDateMaps.get(payBill.getAccentity());
        }else{
            maxSettleDate = settlementService.getMaxSettleDate(payBill.getAccentity());
            maxSettleDateMaps.put(payBill.getAccentity(), maxSettleDate);
        }
        if(SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, payBill.getSettledate())){
            builder.failedId(row.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032D","】已日结，不能取消线下支付！") /* "】已日结，不能取消线下支付！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100271"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180330","该单据已日结，不能取消线下支付！") /* "该单据已日结，不能取消线下支付！" */);
            }
            return builder.build();
        }
        //勾对完成后不能取消线下支付
        Map<String, Boolean> checkMap = journalService.isJournalCheckOrMatch(row.getLong("id"));
        if (checkMap.get(ICmpConstant.CHECHFLAG)) {
            builder.failedId(row.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0013A", "单据【") /* "单据【" */ + payBill.getCode() +  com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0013B", "】已勾对，不能取消线下支付！") /* "】已勾对，不能取消线下支付！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100272"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0013C", "该单据已勾对，不能取消线下支付！") /* "该单据已勾对，不能取消线下支付！" */);
            }
            return builder.build();
        }
        if (checkMap.get(ICmpConstant.CHECHMATCH)) {
            builder.failedId(row.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031A","】该单据已经进行回单关联，不能取消线下支付！") /* "】该单据已经进行回单关联，不能取消线下支付！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100273"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031C","该单据已经进行回单关联，不能取消线下支付！") /* "该单据已经进行回单关联，不能取消线下支付！" */);
            }
            return builder.build();
        }
        //应付单据取消结算校验
        if (payBill.getSrcitem() != null && payBill.getSrcitem() != EventSource.Cmpchase && payBill.getSrcitem() != EventSource.SystemOut&&payBill.getSrcitem() != EventSource.Drftchase) {
            Boolean settleAccountAlready =  RemoteDubbo.get(IArapSettleAccountService.class, IDomainConstant.MDD_DOMAIN_FIARAP).isSettleAccountAlready(payBill);

            if (!batchProcess && settleAccountAlready) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100274"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180320","该单据已月结，不能进行取消结算！") /* "该单据已月结，不能进行取消结算！" */);
            }
            if (settleAccountAlready) {
                builder.failedId(row.getLong("id").toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180322","】已月结，不能进行取消结算！") /* "】已月结，不能进行取消结算！" */);
                return builder.build();
            }
        }
        CtmJSONObject jsonObject = new CtmJSONObject();
        jsonObject.put("id",payBill.getId());

        jsonObject.put("billnum","cmp_payment");
        //判断是否开通总账
        boolean enableGL = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EGL_APP_CODE);
        if (enableGL) {
            boolean checked = cmpVoucherService.isChecked(jsonObject);
            if (checked){
                builder.failedId(row.getLong("id").toString())
                        .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180325","】凭证已勾对，不能取消线下支付！") /* "】凭证已勾对，不能取消线下支付！" */);
                if (!batchProcess) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180327","该单据凭证已勾对，不能取消线下支付！") /* "该单据凭证已勾对，不能取消线下支付！" */);
                }
                return builder.build();
            }
        }
        payBill.setPaystatus(PayStatus.NoPay);
        payBill.setSettlestatus(SettleStatus.noSettlement);
        payBill.setPayman(null);
        payBill.setPaydate(null);
        payBill.setSettledate(null);
        payBill.setDzdate(null);
        payBill.setVoucherNo(null);
        payBill.setVoucherId(null);
        payBill.setVoucherPeriod(null);
        payCallback(payBill);
        payBill.set("_entityName", PayBill.ENTITY_NAME);
        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(payBill);
        if (!deleteResult.getBoolean("dealSucceed")) {
            builder.failedId(row.getLong("id").toString())
                    .message(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418031B","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032C","】删除凭证失败，不能取消线下支付！") /* "】删除凭证失败，不能取消线下支付！" */);
            if (!batchProcess) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100276"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418032F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
            }
            return builder.build();
        }
        payBill.setVoucherstatus(VoucherStatus.Empty);
        builder.journalList(journalService.updateJournalByBill(payBill));
        builder.updateBill(payBill);
        row.put("paystatus", PayStatus.NoPay.getValue());
        row.put("settlestatus", SettleStatus.noSettlement.getValue());
        row.put("voucherstatus", payBill.getVoucherstatus().getValue());
        row.put("dzdate", null);
        row.put("paydate", null);

        try{
            String systemCode = SystemCodeUtil.getSystemCode(payBill);
            if(payBill.getSrcitem()!= null){
                //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                if(payBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)){
                    BillContext billContext = new BillContext();
                    billContext.setBillnum("cmp_payment");
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("paystatus", payBill.getPaystatus().getValue());
                    paramMap.put("paydate", payBill.getPaydate());
                    paramMap.put("settlemode", payBill.getSettlemode());
                    paramMap.put("enterprisebankaccount", payBill.getEnterprisebankaccount());
                    paramMap.put("cashaccount", payBill.getCashaccount());
                    paramMap.put("srcbillid", payBill.getSrcbillid());
                    paramMap.put("settlestatus", payBill.getSettlestatus().getValue());
                    paramMap.put("settledate", payBill.getSettledate());
                    paramMap.put("dzdate", payBill.getDzdate());
                    ytsPayBillSettleServiceImpl.settleBill(payBill,paramMap);
                }
            }
        }catch (Exception e){
            log.error("##   #####   单据结算同步应收失败,执行回滚   ##  ######");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100277"),e.getMessage());
        }
        ctmcmpBusinessLogService.saveBusinessLog(payBill, payBill.getCode(), "", IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.CANCEL_OFFLINE_PAY);
        return builder.build();
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
                String url = AppContext.getEnvConfig("ReceiptDetail_ADDR_REQSEQ");
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
            paramsCallback.put("headvo",CtmJSONObject.toJSONString(payBill));
            String callbackUrl=payBill.getCallback()+"&token="+ InvocationInfoProxy.getYhtAccessToken();
            String responseStr = HttpTookit.doPostWithJson(callbackUrl, CtmJSONObject.toJSONString(paramsCallback), null);
            payBill.setId(id);
            CtmJSONObject result = CtmJSONObject.parseObject(responseStr);
            if (!"0".equals(result.getString("code"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100278"),ValueUtils.isNotEmptyObj(result.getString("msg")) ? result.getString("msg") :result.getString("message"));
            }
        }
    }

    private Map<String, Object> getSettlemodeById(String settlemode) throws Exception {
        return settlemodeCache.get(settlemode,(k)->{
            Map<String, Object> settlemodeMap;
            try {
                settlemodeMap = QueryBaseDocUtils.querySettlementWayById(k);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100279"),"exception when query settlemode by id " + k, e);
            }
            return settlemodeMap;
        });
    }

}
