package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class AutoCheckUtil {

    /**
     * 单据审核检查
     * @param accentity
     * @param minUnsetDay
     * @return
     * @throws Exception
     */
    public static Boolean checkAudit(String accentity,Date minUnsetDay) throws Exception{
        //收款单
        List<Map<String, Object>> receiveBill = checkAuditStatus(ReceiveBill.ENTITY_NAME,accentity,minUnsetDay);
        if(!CollectionUtils.isEmpty(receiveBill)){
            return false;
        }
        //付款单
        List<Map<String, Object>> payBill = checkAuditStatus(PayBill.ENTITY_NAME,accentity,minUnsetDay);
        if(!CollectionUtils.isEmpty(payBill)){
            return false;
        }
        //转账单
        List<Map<String, Object>> accountBill = checkAuditStatus(TransferAccount.ENTITY_NAME,accentity,minUnsetDay);
        if(!CollectionUtils.isEmpty(accountBill)){
            return false;
        }
        //薪资支付
        List<Map<String, Object>> salarypay = checkAuditStatus(Salarypay.ENTITY_NAME,accentity,minUnsetDay);
        if(!CollectionUtils.isEmpty(salarypay)){
            return false;
        }
        //外币兑换
        List<Map<String, Object>> currencyExchange = checkAuditStatus(CurrencyExchange.ENTITY_NAME,accentity,minUnsetDay);
        if(!CollectionUtils.isEmpty(currencyExchange)){
            return false;
        }
        return true;
    }

    /**
     * 审核
     * @throws Exception
     */
    public static List<Map<String, Object>> checkAuditStatus(String fullName,String accentity,Date minUnsetDay) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.addPager(0, 1);
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("auditstatus").eq(AuditStatus.Incomplete.getValue()));
        if(ReceiveBill.ENTITY_NAME.equals(fullName)|| PayBill.ENTITY_NAME.equals(fullName)){
            group.addCondition(QueryCondition.name("cmpflag").eq(true));
        }
        group.addCondition(QueryCondition.name("vouchdate").elt(minUnsetDay));
        querySchema.addCondition(group);
        return MetaDaoHelper.query(fullName,querySchema);
    }

}
