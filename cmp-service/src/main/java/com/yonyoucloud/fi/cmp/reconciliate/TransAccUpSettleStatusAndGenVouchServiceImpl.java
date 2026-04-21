package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentServiceImpl;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class TransAccUpSettleStatusAndGenVouchServiceImpl implements UpSettleStatusAndGenVouchService {
    private static final Logger logger = LoggerFactory.getLogger(TransAccUpSettleStatusAndGenVouchServiceImpl.class);
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    PaymentServiceImpl paymentServiceImpl;

    @Autowired
    private JournalService journalService;

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;
    @Override
    @Transactional
    public void updateRecvSettleNew(List<Long> ids) throws Exception{
        if (ids.size() > 0){
            List<TransferAccount> receives = new ArrayList<>();
            //更新日记账的数据
            List<TransferAccount> dataList = queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            TransferAccount re = null;
            for (TransferAccount entity:  dataList) {
                if(!SettleStatus.alreadySettled.equals(entity.getSettlestatus())){
                    re = new TransferAccount();
                    re.setId(entity.getId());
                    re.setPubts(entity.getPubts());
                    re.setPaystatus(PayStatus.OfflinePay);//支付成功
                    re.setSettlestatus(SettleStatus.alreadySettled);
                    re.setEntityStatus(EntityStatus.Update);
                    if(BillInfoUtils.getBusinessDate() != null) {
                        re.setSettledate(BillInfoUtils.getBusinessDate());
                        re.setPaydate(BillInfoUtils.getBusinessDate());
                    }else {
                        re.setPaydate(new Date());
                        re.setSettledate(new Date());
                    }
                    TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, re.getId());
                    transferAccount.setPaystatus(re.getPaystatus());//支付成功
                    transferAccount.setSettlestatus(re.getSettlestatus());
                    transferAccount.setSettledate(re.getSettledate());
                    transferAccount.setPaydate(re.getPaydate());
                    if (transferAccount.getSettleSuccessAmount() == null && transferAccount.getOriSum() != null) {
                        transferAccount.setSettleSuccessAmount(transferAccount.getOriSum());
                        re.setSettleSuccessAmount(transferAccount.getOriSum());
                    }
                    boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                    if (implement) {
                        re.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                    }
                    receives.add(re);
                    entity.setSettlestatus(SettleStatus.alreadySettled);
                    journalService.updateJournal(entity);
                }
            }
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, receives);
        }

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public Map<String,Object> generateRecvVoucherNew(List<Long> ids) throws Exception{
        Map<String,Object> resMap = new HashMap<>();
        int i = 0;
        if (ids.size() > 0){
            List<TransferAccount> list  = queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (TransferAccount entity : list){
                entity.set("_entityName", TransferAccount.ENTITY_NAME);
                if(BillInfoUtils.getBusinessDate() != null) {
                    entity.setSettledate(BillInfoUtils.getBusinessDate());
                    entity.setPaydate(BillInfoUtils.getBusinessDate());
                }else {
                    entity.setPaydate(new Date());
                    entity.setSettledate(new Date());
                }
                CtmJSONObject jsonObject = cmpVoucherService.generateVoucherWithResult(entity);
                if(jsonObject!= null && !(Boolean)jsonObject.get("dealSucceed")){
                    log.error("转账付款生成凭证失败！", jsonObject.get("message"));
                    i++;
                    continue;
                }
            }
        }
        resMap.put("failCount", i);
        return resMap;
    }

    public List<TransferAccount> queryAggvoByIds(Long[] ids) throws Exception {
        List<TransferAccount> receiveBillListQuery = new ArrayList<>();
        for (Long id : ids) {
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id, 3);
            if (transferAccount != null) {
                receiveBillListQuery.add(transferAccount);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100789"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180619","查询不到对应单据,请确认单据是否存在或刷新后重新操作") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作" */);
            }
        }
        return receiveBillListQuery;
    }
}
