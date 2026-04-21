package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.BizException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class SettleCheckUtil {

    /**
     * 单据审核检查
     * @param accentity
     * @param period
     * @param name
     * @param minUnsetDay
     * @return
     * @throws Exception
     */
    public static Map<String,Object> checkAudit(String accentity,String period,String name,
                                                Date minUnsetDay,boolean checkDailySettlement) throws Exception{
        Map<String, Object> result = new HashMap<String,Object>();
        List<Map<String, Object>> checkResultList = new ArrayList<Map<String, Object>>();
        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        checkResult.put("checkRule", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015C","未审核") /* "未审核" */);
        //新架构条件下 不校验收款单、付款单
        if(!CmpCommonUtil.getNewFiFlag()){
            //收款单
            Map<String, Object> receiveBill = checkAuditStatus(ReceiveBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.ReceiveBill, null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180163","收款单") /* "收款单" */);
            if(receiveBill!=null&&receiveBill.size()!=0){
                List<Map<String,Object>> checkDetail = (ArrayList<Map<String, Object>>)receiveBill.get("checkdetail");
                if(checkDetail!=null&&checkDetail.size()!=0&&checkDetail.get(0)!=null){
                    List<Map<String,Object>> list = (ArrayList<Map<String, Object>>)checkDetail.get(0).get("detail");
                    if(list!=null&&list.size()!=0){
                        errorList.addAll(list);
                    }
                }

            }
            //付款单
            Map<String, Object> payBill = checkAuditStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
            if(payBill!=null&&payBill.size()!=0){
                List<Map<String,Object>> checkDetail = (ArrayList<Map<String, Object>>)payBill.get("checkdetail");
                if(checkDetail!=null&&checkDetail.size()!=0&&checkDetail.get(0)!=null){
                    List<Map<String,Object>> list = (ArrayList<Map<String, Object>>)checkDetail.get(0).get("detail");
                    if(list!=null&&list.size()!=0){
                        errorList.addAll(list);
                    }
                }
            }
        }
//        Date minErrorDate = (Date) payBill.get("minErrorDate");
        //转账单
        Map<String, Object> accountBill = checkAuditStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if(accountBill!=null&&accountBill.size()!=0){
            List<Map<String,Object>> checkDetail = (ArrayList<Map<String, Object>>)accountBill.get("checkdetail");
            if(checkDetail!=null&&checkDetail.size()!=0&&checkDetail.get(0)!=null){
                List<Map<String,Object>> list = (ArrayList<Map<String, Object>>)checkDetail.get(0).get("detail");
                if(list!=null&&list.size()!=0){
                    errorList.addAll(list);
                }
            }
        }
        //薪资支付
        Map<String,Object> salarypay = checkAuditStatus(Salarypay.ENTITY_NAME,accentity,period,name,minUnsetDay,EventType.SalaryPayment,null,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_YYFI-UI_0001112175","薪资支付单"));
        if(salarypay!=null&&salarypay.size()!=0){
            List<Map<String,Object>> checkDetail = (ArrayList<Map<String, Object>>)salarypay.get("checkdetail");
            if(checkDetail!=null&&checkDetail.size()!=0&&checkDetail.get(0)!=null){
                List<Map<String,Object>> list = (ArrayList<Map<String, Object>>)checkDetail.get(0).get("detail");
                if(list!=null&&list.size()!=0){
                    errorList.addAll(list);
                }
            }
        }
        //外币兑换
        Map<String, Object> currencyExchange = checkAuditStatus(CurrencyExchange.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.CurrencyExchangeBill, null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180169","外币兑换") /* "外币兑换" */);
        if(currencyExchange!=null&&currencyExchange.size()!=0){
            List<Map<String,Object>> checkDetail = (ArrayList<Map<String, Object>>)currencyExchange.get("checkdetail");
            if(checkDetail!=null&&checkDetail.size()!=0&&checkDetail.get(0)!=null){
                List<Map<String,Object>> list = (ArrayList<Map<String, Object>>)checkDetail.get(0).get("detail");
                if(list!=null&&list.size()!=0){
                    errorList.addAll(list);
                }
            }
        }

        if(errorList.size()==0){
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180164","全部已审核，允许日结") /* "全部已审核，允许日结" */);
            checkResult.put("messageLocale","04"+"&&&P_YS_FI_CM_0000153325" /* "全部已审核，允许日结" */);
            checkResult.put("checkResult","pass");
            result.put("checkResult","pass");
        }else{
            if(checkDailySettlement){
                checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + errorList.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015C","未审核") /* "未审核" */);
                checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+errorList.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+"&&&P_YS_FED_EXAMP_0000020440" /* "未审核" */);
            }else{
                checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + errorList.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180161","未审核，不允许日结") /* "未审核，不允许日结" */);
                checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+errorList.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+"&&&P_YS_FI_CM_0000153364" /* "未审核，不允许日结" */);
            }
            checkResult.put("checkResult","error");
            result.put("checkResult","error");
        }

        checkResult.put("detail",errorList);
        checkResultList.add(checkResult);
        result.put("checkdetail",checkResultList);
        return result;
    }

    /**
     * 单据结算检查
     * @param accentity
     * @param period
     * @param name
     * @param minUnsetDay
     * @return
     */
    public static Map<String,Object> checkSettle(String accentity,String period,String name,
                                                 Date minUnsetDay) throws Exception{
        Map<String, Object> result = new HashMap<String,Object>();
        List<Map<String, Object>> checkResultList = new ArrayList<Map<String, Object>>();
        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        checkResult.put("checkRule", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180160","未结算") /* "未结算" */);
        //结算
        Map<String, Object> payBill = new HashMap<>();
        if(!CmpCommonUtil.getNewFiFlag()){
            //收款单
            Map<String, Object> receiveBill = checkSettleStatus(ReceiveBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.ReceiveBill, "pass", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180163","收款单") /* "收款单" */);
            if(receiveBill!=null){
                errorList.addAll((ArrayList<Map<String, Object>>)receiveBill.get("detail"));
            }
            //付款单
            payBill = checkSettleStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, "pass", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
            if(payBill!=null){
                errorList.addAll((ArrayList<Map<String, Object>>)payBill.get("detail"));
            }
            //支付不明
            //付款单
            payBill = checkPayUnknownStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, (String) payBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
            if (payBill != null) {
                errorList.addAll((ArrayList<Map<String, Object>>) payBill.get("detail"));
            }
            //预下单失败
            //付款单
            payBill = checkPreFailStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, (String) payBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
            if (payBill != null) {
                errorList.addAll((ArrayList<Map<String, Object>>) payBill.get("detail"));
            }
        }
        //转账单
        Map<String, Object> accountBill = checkSettleStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, "pass", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if(accountBill!=null){
            errorList.addAll((ArrayList<Map<String, Object>>)accountBill.get("detail"));
        }
        //薪资支付
        Map<String, Object> salarypay = checkSettleStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, "pass", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180158","薪资支付单") /* "薪资支付单" */);
        if(salarypay!=null){
            errorList.addAll((ArrayList<Map<String, Object>>)salarypay.get("detail"));
        }
        //外币兑换
        Map<String, Object> currencyExchange = checkSettleStatus(CurrencyExchange.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.CurrencyExchangeBill, "pass", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180169","外币兑换") /* "外币兑换" */);
        if(currencyExchange!=null){
            errorList.addAll((ArrayList<Map<String, Object>>)currencyExchange.get("detail"));
        }

        //支付失败
        //转账单
        accountBill = checkPayFailStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) accountBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if(accountBill!=null){
            errorList.addAll((ArrayList<Map<String, Object>>)accountBill.get("detail"));
        }
        //薪资支付
        salarypay = checkPayFailStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) salarypay.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180158","薪资支付单") /* "薪资支付单" */);
        if (salarypay != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) salarypay.get("detail"));
        }

        //支付不明
        //付款单
        payBill = checkPayUnknownStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, (String) payBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
        if (payBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) payBill.get("detail"));
        }
        //转账单
        accountBill = checkPayUnknownStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) accountBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if (accountBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) accountBill.get("detail"));
        }
        //薪资支付
        salarypay = checkPayUnknownStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) salarypay.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180158","薪资支付单") /* "薪资支付单" */);
        if (salarypay != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) salarypay.get("detail"));
        }

        //预下单失败
        //转账单
        accountBill = checkPreFailStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) accountBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if (accountBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) accountBill.get("detail"));
        }
        //薪资支付
        salarypay = checkPreFailStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) salarypay.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180158","薪资支付单") /* "薪资支付单" */);
        if (salarypay != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) salarypay.get("detail"));
        }

        //预下单成功
        //付款单
        payBill = checkPreSuccessStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, (String) payBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
        if (payBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) payBill.get("detail"));
        }
        //转账单
        accountBill = checkPreSuccessStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) accountBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if (accountBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) accountBill.get("detail"));
        }
        //薪资支付
        salarypay = checkPreSuccessStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) salarypay.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180158","薪资支付单") /* "薪资支付单" */);
        if (salarypay != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) salarypay.get("detail"));
        }


        //支付中
        //付款单
        payBill = checkPayingStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, (String) payBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
        if (payBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) payBill.get("detail"));
        }
        //转账单
        accountBill = checkPayingStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) accountBill.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180167","转账单") /* "转账单" */);
        if (accountBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) accountBill.get("detail"));
        }
        //薪资支付
        salarypay = checkPayingStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) salarypay.get("flag"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180158","薪资支付单") /* "薪资支付单" */);
        if (salarypay != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) salarypay.get("detail"));
        }

        //日记账中银行账号与现金账号都为空
        //付款单
        payBill = checkAccountNOStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent, (String) payBill.get("flag"), null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180159","付款单") /* "付款单" */);
        if (payBill != null) {
            errorList.addAll((ArrayList<Map<String, Object>>) payBill.get("detail"));
        }

        if(errorList.size()==0){
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015A","全部已结算") /* "全部已结算" */);
            checkResult.put("messageLocale","04"+"&&&P_YS_FI_CM_0000153376");
            checkResult.put("checkResult","pass");
            result.put("checkResult","pass");
        }else{
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + errorList.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180160","未结算") /* "未结算" */);
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+errorList.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+"&&&P_YS_FI_CM_0000026079");
            checkResult.put("checkResult","warning");
            result.put("checkResult","warning");
        }

//         checkResult.put("message",com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026181") /* "有" */+list.size()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_FW_0000021473") /* "张" */+eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153364") /* "未审核，不允许日结" */);
//         checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+getCodeByName(eventType.getName())+"&&&P_YS_FI_CM_0000153364" /* "未审核，不允许日结" */);
//         checkResult.put("checkResult","error");
//         result.put("checkResult","error");
//         checkResult.put("message",eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153325") /* "全部已审核，允许日结" */);
//         checkResult.put("messageLocale","04"+getCodeByName(eventType.getName())+"&&&P_YS_FI_CM_0000153325" /* "全部已审核，允许日结" */);
//         checkResult.put("checkResult","pass");
//         result.put("checkResult","pass");

        checkResult.put("detail",errorList);
        checkResultList.add(checkResult);
        result.put("checkdetail",checkResultList);
        return result;
    }

    /**
     * 月末汇兑损益计算检查
     * @param accentity
     * @param period
     * @param name
     * @return
     * @throws Exception
     */
    public static  Map<String,Object> checkExchangeGainsAndLosses(String accentity,String period,String name,boolean isAuto) throws Exception{
        Map<String, Object> result = new HashMap<String,Object>();  //检测结果集
        result.put("checkName",name);  //检测指标名称：月末汇兑损益计算检查
        Map<String, Object> checkResult = new HashMap<String,Object>();
        String token = InvocationInfoProxy.getYhtAccessToken();
        log.info("token:======================"+token);
        //查询会计期间
//        Period p = FINBDApiUtil.getFI4BDAccPeriodService().getPeriodVOByDate(DateUtils.dateParse(period,DateUtils.DATE_PATTERN),String.valueOf(accBookTypeId));
        Map<String,Object> periodvo = QueryBaseDocUtils.queryPeriodByAccbodyAndDate(accentity,DateUtils.dateParse(period,DateUtils.DATE_PATTERN));/* 暂不修改 */
        if(periodvo == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101091"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！"), accentity));
        }
        //查询单据汇兑损益表
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("vouchdate").eq(DateUtils.strToDate(period)));

        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(ExchangeGainLoss.ENTITY_NAME,querySchema);

        Date endDate = (Date)periodvo.get("enddate");
        String endDateStr = null;
        if(endDate!=null){
            endDateStr = DateUtils.dateToStr(endDate);
        }
        if (endDateStr == null) {
            throw new BizException("01&&&P_YS_FI_CM_0001203750", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180170","会计期间结束时间为空") /* "会计期间结束时间为空" */);
        }

        if (!endDateStr.equals(period)) {
            if (isAuto) {
                checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180171","已完成") /* "已完成" */);
                checkResult.put("messageLocale", "04" + "&&&P_YS_SCM_PU_0000027628");
            }
            checkResult.put("checkResult","pass");
            result.put("checkResult","pass");
        }else{
            if(list==null||list.size()==0){
                if(isAuto){
                    checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180172","有异常") /* "有异常" */);
                    checkResult.put("messageLocale","04"+"&&&P_YS_FI_AP_0000072071");
                }else{
                    result.put("messageAdjustment", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180173","当月没检测到现金汇兑损益单，请检查是否涉及外币汇兑损益调整！") /* "当月没检测到现金汇兑损益单，请检查是否涉及外币汇兑损益调整！" */);
                }
                checkResult.put("checkResult","warning");
                result.put("checkResult","warning");
            }else{
                if(isAuto){
                    checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180171","已完成") /* "已完成" */);
                    checkResult.put("messageLocale","04"+"&&&P_YS_SCM_PU_0000027628");
                }
                checkResult.put("checkResult","pass");
                result.put("checkResult","pass");
            }
        }
        if(isAuto){
            List<Map<String, Object>> checkResultList = new ArrayList<Map<String, Object>>();
            checkResultList.add(checkResult);
            result.put("checkdetail",checkResultList);
        }
        return result;

    }
    /**
     * 审核
     * @throws Exception
     */
    public static Map<String, Object> checkAuditStatus(String fullName,String accentity,String period,String name,
                                                       Date minUnsetDay,EventType eventType,Date minErrorDate,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
//        result.put("checkName",name);
        List<Map<String, Object>> checkResultList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,auditstatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("auditstatus").eq(AuditStatus.Incomplete.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(1));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//        checkResult.put("checkRule",eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FED_EXAMP_0000020440") /* "未审核" */);
        if(list.size()>0){
            for(Map<String, Object> recebill:list){
                error = new HashMap<String,Object>();
                String orderno = (String)recebill.get("orderno");
                Date vouchdate = (Date)recebill.get("vouchdate");
                if(minErrorDate==null||vouchdate.compareTo(minErrorDate)<0){
                    minErrorDate = vouchdate;
                }
                String billdate = new SimpleDateFormat("yyyy-MM-dd").format(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015C","未审核") /* "未审核" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180161","未审核，不允许日结") /* "未审核，不允许日结" */);
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000153364" /* "未审核，不允许日结" */);
            checkResult.put("checkResult","error");
            result.put("checkResult","error");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180164","全部已审核，允许日结") /* "全部已审核，允许日结" */);
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153325" /* "全部已审核，允许日结" */);
            checkResult.put("checkResult","pass");
            result.put("checkResult","pass");
        }
        checkResult.put("detail",errorList);
        checkResultList.add(checkResult);
        result.put("checkdetail",checkResultList);
        result.put("minErrorDate",minErrorDate);
        return result;
    }

    /**
     * 结算
     * @throws Exception
     */
    public static Map<String, Object> checkSettleStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType,String flag,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
//        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("settlestatus").eq(SettleStatus.noSettlement.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//         checkResult.put("checkRule",eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026079") /* "未结算" */);
        if(list.size()>0){
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180160","未结算") /* "未结算" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180160","未结算") /* "未结算" */);
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000026079");
            checkResult.put("checkResult","warning");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015A","全部已结算") /* "全部已结算" */);
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153376");
            checkResult.put("checkResult","pass");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        return checkResult;
    }

    /**
     * 待支付
     * @throws Exception
     */
    public static Map<String, Object> checkNoPayStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
        String  resultFlag = "pass";
//        result.put("checkName",name);
        String flag = "pass";
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("paystatus").eq(PayStatus.NoPay.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
        checkResult.put("checkRule", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180177","待支付") /* "待支付" */);
        if(list.size()>0){
            if("pass".equals(resultFlag)){
                resultFlag = "warning";
            }
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418017B","单据结算") /* "单据结算" */);
                error.put("orderno", orderno);
                error.put("date", billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180177","待支付") /* "待支付" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418017C","待支付!") /* "待支付!" */);
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000153330");
            checkResult.put("checkResult","warning");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418017D","中不存在待支付的,允许日结!") /* "中不存在待支付的,允许日结!" */);
            checkResult.put("checkResult","pass");
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153314");
        }
        checkResult.put("detail",errorList);
        return checkResult;
    }

    /**
     * 预下单成功
     * @throws Exception
     */
    public static Map<String, Object> checkPreSuccessStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType,String flag,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
        String  resultFlag = "pass";
//        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        checkResult = new HashMap<String,Object>();
        errorList = new ArrayList<Map<String, Object>>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("paystatus").eq(PayStatus.PreSuccess.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//        checkResult.put("checkRule",eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026137") /* "预下单成功" */);
        if(list.size()>0){
            if("pass".equals(resultFlag)){
                resultFlag = "warning";
            }
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418016A","预下单成功") /* "预下单成功" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418016B","预下单成功!") /* "预下单成功!" */);
            checkResult.put("checkResult","warning");
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000153378");
        }else{
            checkResult.put("message",eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153336") /* "不存在预下单成功的,允许日结!" */);
            checkResult.put("checkResult","pass");
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153336");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        return checkResult;
    }

    /**
     * 预下单失败
     * @throws Exception
     */
    public static Map<String, Object> checkPreFailStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType,String flag,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
        String  resultFlag = "pass";
//        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("paystatus").eq(PayStatus.PreFail.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//        checkResult.put("checkRule",eventType.getName()+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026210") /* "预下单失败" */);
        if(list.size()>0){
            if("pass".equals(resultFlag)){
                resultFlag = "warning";
            }
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180174","预下单失败") /* "预下单失败" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180175","预下单失败!") /* "预下单失败!" */);
            checkResult.put("checkResult","warning");
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000153410");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180176","不存在预下单失败的,允许日结!") /* "不存在预下单失败的,允许日结!" */);
            checkResult.put("checkResult","pass");
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153399");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        return checkResult;
    }

    /**
     * 支付失败
     * @throws Exception
     */
    public static Map<String, Object> checkPayFailStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType,String flag,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
//        result.put("checkName",name);
        List<Map<String, Object>> checkResultList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("paystatus").eq(PayStatus.Fail.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//        checkResult.put("checkRule",com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153363") /* "单据支付失败" */);
        if(list.size()>0){
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015B","支付失败") /* "支付失败" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015E","支付失败!") /* "支付失败!" */);
            checkResult.put("checkResult","warning");
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_SD_SDMBF_0000141979");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180162","中不存在支付失败的,允许日结!") /* "中不存在支付失败的,允许日结!" */);
            checkResult.put("checkResult","pass");
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153329");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        return checkResult;
    }

    /**
     * 支付不明
     * @throws Exception
     */
    public static Map<String, Object> checkPayUnknownStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType,String flag,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
//        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("paystatus").eq(PayStatus.PayUnknown.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//        checkResult.put("checkRule",com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026051") /* "付款单支付不明" */);
        if(list.size()>0){
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418016D","支付不明") /* "支付不明" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418016E","支付不明!") /* "支付不明!" */);
            checkResult.put("checkResult","warning");
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000153320");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418016F","中不存在支付不明的,允许日结!") /* "中不存在支付不明的,允许日结!" */);
            checkResult.put("checkResult","pass");
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153367");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        return checkResult;
    }

    /**
     * 支付中
     * @throws Exception
     */
    public static Map<String, Object> checkPayingStatus(String fullName,String accentity,String period,String name, Date minUnsetDay,EventType eventType,String flag,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
//        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        QuerySchema querySchema = QuerySchema.create().addSelect("code as orderno,settlestatus,vouchdate");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("vouchdate").between(minUnsetDay,DateUtils.strToDate(period)),
                QueryCondition.name("paystatus").eq(PayStatus.Paying.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        querySchema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(fullName,querySchema);
//        checkResult.put("checkRule",eventType.getName() + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026057") /* "支付中" */);
        if(list.size()>0){
            flag = "warning";
            for(Map<String, Object> paybill:list){
                error = new HashMap<String,Object>();
                String orderno = (String )paybill.get("orderno");
                Date vouchdate = (Date )paybill.get("vouchdate");
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",orderno);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180178","支付中") /* "支付中" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F","张") /* "张" */ + eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180179","支付中!") /* "支付中!" */);
            checkResult.put("checkResult","warning");
            checkResult.put("messageLocale","05"+"P_YS_FI_CM_0000026181&&&" /* "有" */+list.size()+"&&&P_YS_FED_FW_0000021473&&&"/* "张" */+eventType.getName()+"&&&P_YS_FI_CM_0000153423");
        }else{
            checkResult.put("message", eventType.getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418017A","不存在支付中的,允许日结!") /* "不存在支付中的,允许日结!" */);
            checkResult.put("checkResult","pass");
            checkResult.put("messageLocale","04"+eventType.getName()+"&&&P_YS_FI_CM_0000153375");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        return checkResult;
    }


    /**
     * 支付中
     * @throws Exception
     */
    public static Map<String, Object> checkAccountNOStatus(String fullName,String accentity,String period,String name,
                                                           Date minUnsetDay,EventType eventType,String flag,Date minErrorDate,String billType) throws Exception {
        Map<String, Object> result = new HashMap<String,Object>();
//        result.put("checkName",name);
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        Map<String, Object> checkResult = new HashMap<String,Object>();
        Map<String, Object> error = new HashMap<String,Object>();
        checkResult = new HashMap<String,Object>();
        errorList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> list = getJournalDBList(accentity,minUnsetDay,DateUtils.strToDate(period));
//        checkResult.put("checkRule",com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026255") /* "日记账中银行账号、现金账号均未录入" */);
        if(list.size()>0){
            flag = "error";
            for(Map<String, Object> journal:list){
                error = new HashMap<String,Object>();
                String billnum = (String )journal.get("billnum");
                Date vouchdate = (Date )journal.get("dzdate");
                if(minErrorDate==null||vouchdate.compareTo(minErrorDate)<0){
                    minErrorDate = vouchdate;
                }
                String billdate =  DateUtils.dateToStr(vouchdate);
                error.put("type",billType);
                error.put("orderno",billnum);
                error.put("date",billdate);
                error.put("errorMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180165","银行账号、现金账号均未录入,请先到付款工作台录入！") /* "银行账号、现金账号均未录入,请先到付款工作台录入！" */);
                errorList.add(error);
            }
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015D","有") /* "有" */ + list.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180166","张日记账账号检查错误,不允许日结!") /* "张日记账账号检查错误,不允许日结!" */);
            checkResult.put("checkResult", "error");
            checkResult.put("messageLocale", "03" + "P_YS_FI_CM_0000026181&&&" /* "有" */ + list.size() + "&&&P_YS_FI_CM_0000026074");
        } else {
            checkResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180168","日记账中不存在银行账号与现金账号均为空的,允许日结!") /* "日记账中不存在银行账号与现金账号均为空的,允许日结!" */);
            checkResult.put("checkResult", "pass");
            checkResult.put("messageLocale", "02P_YS_FI_CM_0000026306");
        }
        checkResult.put("detail",errorList);
        checkResult.put("flag",flag);
        checkResult.put("minErrorDate",minErrorDate);
        return checkResult;
    }




    private static List<Map<String, Object>> getJournalDBList(String accentity,Date start, Date end) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup
                .and(QueryCondition.name("accentity").eq(accentity),QueryCondition.name("dzdate").between(start,end),
                        QueryCondition.name("bankaccount").is_null(),QueryCondition.name("cashaccount").is_null());
        querySchema.addCondition(group);
        return MetaDaoHelper.query(Journal.ENTITY_NAME,querySchema);
    }

    /**
     * 线下支付，网银支付时，校验当前登账日期是否已日结，若已日结，不能线下支付，结算日期需要在日结日期之后
     * @param maxSettleDate 最大日结日期
     * @param isEbank  是否网银支付
     * @return
     * @throws Exception
     */
    public static Boolean checkDailySettlement(Date maxSettleDate, Boolean isEbank) throws Exception{
        if(maxSettleDate == null){
            return false;
        }
        Date settlementdate = new Date();
        if (!isEbank && BillInfoUtils.getBusinessDate() != null) {
            settlementdate = BillInfoUtils.getBusinessDate();
        }
        String s_settlementdate = DateUtils.dateToStr(settlementdate);
        String s_maxSettleDate = DateUtils.dateToStr(maxSettleDate);
        if(s_maxSettleDate.compareTo(s_settlementdate) >= 0){
            return true;
        }
        return false;
    }

    /**
     * 取消线下支付时，校验当前业务单据结算日期是否已日结，若已日结，不能取消线下支付
     * 最大日结日期 >= 单据结算日期，不能取消（效率优化，访问一次数据库，不然根据日期查询校验访问for循环次）
     * @param maxSettleDate 最大日结日期
     * @param settlementDate 单据结算日期
     * @return 是否已日结
     * @throws Exception
     */
    public static Boolean checkDailySettlementBeforeUnSettle(Date maxSettleDate, Date settlementDate) throws Exception{
        if(maxSettleDate == null || settlementDate == null){
            return false;
        }
        String s_settlementdate = DateUtils.dateToStr(settlementDate);
        String s_maxSettleDate = DateUtils.dateToStr(maxSettleDate);
        if(s_maxSettleDate.compareTo(s_settlementdate) >= 0){
            return true;
        }
        return false;
    }
}
