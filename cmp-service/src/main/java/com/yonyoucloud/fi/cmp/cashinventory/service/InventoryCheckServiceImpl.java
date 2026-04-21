package com.yonyoucloud.fi.cmp.cashinventory.service;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cashinventory.InventoryCheckService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DailyCompute;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InventoryCheckServiceImpl implements InventoryCheckService {

    private final static String  CASH = "CASH";
    private String CURRENCY_NAME = "currency_name";

    /**
     *  必输条件判断与获取盘点日现金日记账余额
     * @param bill
     * @return
     * @throws Exception
     */
    @Override
    public Boolean ruleCheck(BizObject bill, String type) throws Exception {
        String accEntityId = bill.get(ICmpConstant.ACCENTITY);
        if (null == accEntityId) {
            return false;
        }
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);
        if (accEntity.size() == 0) {
            return false;
        }
        //币种
        String currency = bill.get(ICmpConstant.CURRENCY);
        if (null == currency) {
            return false;
        }
        //现金账户cashaccount
        String cashaccount = bill.get(ICmpConstant.CASH_ACCOUNT_LOWER);
        if (null == cashaccount) {
            return false;
        }
        //日期
        Date vouchdate = bill.get("vouchdate");
        if (null == vouchdate) {
            if(CASH.equals(type)){
                bill.set(ICmpConstant.CASH_ACCOUNT_LOWER,null);
                bill.set(ICmpConstant.CASH_ACCOUNT_LOWER_NAME,null);
                bill.set(ICmpConstant.CURRENCY,null);
                bill.set(CURRENCY_NAME,null);
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102592"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418034F","盘点日期不能为空") /* "盘点日期不能为空" */);
        }else{
            vouchdate = DateUtils.dateAddDays(vouchdate,1);
        }
        Map<String, SettlementDetail> settlementDetailMap = DailyCompute.imitateDailyCompute(accEntityId,currency,cashaccount,null,"2","2",vouchdate);
        //yesterdayorimoney昨日原币
        String key = accEntityId+cashaccount+currency;
        if(null != settlementDetailMap && settlementDetailMap.size() > 0){
            SettlementDetail settlementDetail = settlementDetailMap.get(key);
            if(null != settlementDetail){
                bill.set("journalbalance",settlementDetail.getYesterdayorimoney());
            }
        }else{
            bill.set("journalbalance", BigDecimal.ZERO);
        }
        return true;
    }

    /**
     * 计算金额逻辑处理
     * @param bill
     * @throws Exception
     */
    @Override
    public void amountCalculation(BizObject bill) throws Exception {
        //盘点日现金日记账余额
        BigDecimal journalbalance = null == bill.get("journalbalance") ? BigDecimal.ZERO : bill.get("journalbalance");
        //调整后日记账余额
        BigDecimal adjustjournalbalance = BigDecimal.ZERO;
        BigDecimal registeredunpaid = null == bill.get("registeredunpaid") ? BigDecimal.ZERO : bill.get("registeredunpaid"); //	加：已登账 未付款
        BigDecimal registeredunreceived = null == bill.get("registeredunreceived") ? BigDecimal.ZERO : bill.get("registeredunreceived");//	减：已登账 未收款
        BigDecimal unregisteredpaid = null == bill.get("unregisteredpaid") ? BigDecimal.ZERO : bill.get("unregisteredpaid");//	减：未登账 已付款
        BigDecimal unregisteredreceived = null == bill.get("unregisteredreceived") ? BigDecimal.ZERO : bill.get("unregisteredreceived");//	加：未登账 已收款
        //必输，不可编辑	【调整后账面余额】=【盘点日现金日记账余额】+【加：已登账 未付款】-【减：已登账 未收款】-【减：未登账 已付款】+【加：未登账 已收款】
        adjustjournalbalance =  BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeSubtract(
                BigDecimalUtils.safeAdd(journalbalance,registeredunpaid),registeredunreceived),
                unregisteredpaid),unregisteredreceived);
        if(null == bill.get("currency_moneyDigit")){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102593"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180350","币种精度不能为空") /* "币种精度不能为空" */);
        }
        int moneydigit = Integer.parseInt(bill.get("currency_moneyDigit").toString());
        bill.set("adjustjournalbalance",adjustjournalbalance.setScale(moneydigit, RoundingMode.HALF_UP));
        //长款
        BigDecimal longstyle = BigDecimal.ZERO;
        //短款
        BigDecimal shortie = BigDecimal.ZERO;
        //现金实点金额
        BigDecimal actualamount = null == bill.get("actualamount")? BigDecimal.ZERO : bill.get("actualamount");
        if(actualamount.compareTo(adjustjournalbalance) > 0){
            longstyle = BigDecimalUtils.safeSubtract(actualamount,adjustjournalbalance);
        }else{
            shortie = BigDecimalUtils.safeSubtract(adjustjournalbalance,actualamount);
        }
        bill.set("longstyle",longstyle.setScale(moneydigit, RoundingMode.HALF_UP));
        bill.set("shortie",shortie.setScale(moneydigit, RoundingMode.HALF_UP));
        BigDecimal exchRate = null != bill.get("exchRate") ? bill.get("exchRate"): BigDecimal.ZERO;
        bill.set("longlocalamount", BigDecimalUtils.safeMultiply(longstyle,exchRate,moneydigit));
        bill.set("shortielocalamount", BigDecimalUtils.safeMultiply(shortie,exchRate,moneydigit));
    }
}
