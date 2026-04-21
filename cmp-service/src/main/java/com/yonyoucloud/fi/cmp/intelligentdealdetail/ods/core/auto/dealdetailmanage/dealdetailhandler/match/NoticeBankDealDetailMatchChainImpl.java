package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Author guoyangy
 * @Date 2024/6/18 16:32
 * @Description 流水处理过滤器链
 * @Version 1.0
 */
@Slf4j
public class NoticeBankDealDetailMatchChainImpl extends BankDealDetailMatchChainImpl{
    public static NoticeBankDealDetailMatchChainImpl get(){
        return new NoticeBankDealDetailMatchChainImpl();
    }

    @Override
    public List<BankReconciliation> handleBankReconciliationToDB(BankDealDetailContext context, List<BankReconciliation> saveOrUpdateBankReconciliationList) {
        List<BankReconciliation> bankReconciliationList = super.handleBankReconciliationToDB(context, saveOrUpdateBankReconciliationList);
        // 银行流水支持发送事件消息
        try{
            //新增流水发送事件
            AppContext.getBean(ICmpSendEventService.class).sendEventByBankClaimBatch(bankReconciliationList, EntityStatus.Insert.name());
        }catch (Exception e){
            log.error("给客开发事件失败",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C5", "事件发送失败") /* "事件发送失败" */);
        }
        return null;
    }
}
