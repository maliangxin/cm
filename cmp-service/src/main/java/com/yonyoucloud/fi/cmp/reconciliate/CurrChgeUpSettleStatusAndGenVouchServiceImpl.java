package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillServiceImpl;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
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
public class CurrChgeUpSettleStatusAndGenVouchServiceImpl implements UpSettleStatusAndGenVouchService {

    private static final Logger logger = LoggerFactory.getLogger(CurrChgeUpSettleStatusAndGenVouchServiceImpl.class);
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    ReceiveBillServiceImpl receiveBillServiceImpl;

    @Autowired
    private JournalService journalService;

    @Autowired
    CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Override
    @Transactional
    public void updateRecvSettleNew(List<Long> ids) throws Exception{
        if (ids.size() > 0){
            List<CurrencyExchange> receives = new ArrayList<>();
            List<BizObject> journalUps = new ArrayList<>();
            CurrencyExchange re = null;
            List<CurrencyExchange> dataList = queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (CurrencyExchange entity : dataList) {
                if(!DeliveryStatus.alreadyDelivery.equals(entity.getSettlestatus())){
                    re = new CurrencyExchange();
                    re.setId(entity.getId());
                    re.setPubts(entity.getPubts());
                    re.setSettlestatus(DeliveryStatus.alreadyDelivery);
                    re.setEntityStatus(EntityStatus.Update);
                    if(BillInfoUtils.getBusinessDate() != null) {
                        re.setSettledate(BillInfoUtils.getBusinessDate());
                    }else {
                        re.setSettledate(new Date());
                    }
                    CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, re.getId());
                    currencyExchange.setSettledate(re.getSettledate());
                    currencyExchange.setSettlestatus(re.getSettlestatus());
                    boolean implement = cmpBudgetCurrencyExchangeManagerService.implement(currencyExchange);
                    if (implement) {
                        re.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                    }
                    receives.add(re);
                    entity.setSettlestatus(DeliveryStatus.alreadyDelivery);
                    journalUps.add(entity);
                }
            }
            journalService.updateJournal(journalUps);
            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, receives);
        }

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW)//Transaction rolled back because it has been marked as rollback-only
    public Map<String,Object> generateRecvVoucherNew(List<Long> ids) throws Exception{
        Map<String,Object> resMap = new HashMap<>();
        int i = 0;
        if (ids.size() > 0){
            List<CurrencyExchange> list = null;
            list = queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (CurrencyExchange entity : list){
                entity.set("_entityName", CurrencyExchange.ENTITY_NAME);
                if(BillInfoUtils.getBusinessDate() != null) {
                    entity.setSettledate(BillInfoUtils.getBusinessDate());
                }else {
                    entity.setSettledate(new Date());
                }
                CtmJSONObject jsonObject = cmpVoucherService.generateVoucherWithResult(entity);
                if(jsonObject!= null && !(Boolean)jsonObject.get("dealSucceed")){
                    log.error("外币兑换生成凭证失败！", jsonObject.get("message"));
                    i++;
                    continue;
                }
            }
        }
        resMap.put("failCount",i);
        return resMap;
    }

    public List<CurrencyExchange> queryAggvoByIds(Long[] ids) throws Exception {
        List<CurrencyExchange> receiveBillListQuery = new ArrayList<>();
        for (Long id : ids) {
            CurrencyExchange transferAccount = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, id, 3);
            if (transferAccount != null) {
                receiveBillListQuery.add(transferAccount);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101686"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029B","查询不到对应单据,请确认单据是否存在或刷新后重新操作") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作" */);
            }
        }
        return receiveBillListQuery;
    }
}
