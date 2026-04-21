package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillServiceImpl;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
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
public class ReceiveUpSettleStatusAndGenVouchServiceImpl implements UpSettleStatusAndGenVouchService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiveUpSettleStatusAndGenVouchServiceImpl.class);
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    ReceiveBillServiceImpl receiveBillServiceImpl;

    @Autowired
    private JournalService journalService;
    @Override
    @Transactional
    public void updateRecvSettleNew(List<Long> ids) throws Exception{
        if (ids.size() > 0){
            List<ReceiveBill> receives = new ArrayList<>();
            ReceiveBill re = null;
            List<ReceiveBill> dataList = receiveBillServiceImpl.queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (ReceiveBill receiveBill : dataList) {
                if(!SettleStatus.alreadySettled.equals(receiveBill.getSettlestatus())){
                    re = new ReceiveBill();
                    re.setId(receiveBill.getId());
                    re.setPubts(receiveBill.getPubts());
                    re.setSettlestatus(SettleStatus.alreadySettled);
                    re.setEntityStatus(EntityStatus.Update);
                    if(BillInfoUtils.getBusinessDate() != null) {
                        re.setSettledate(new Date());
                        re.setDzdate(BillInfoUtils.getBusinessDate());
                    }else {
                        re.setSettledate(new Date());
                        re.setDzdate(new Date());
                    }
                    receives.add(re);
                    receiveBill.setSettlestatus(SettleStatus.alreadySettled);
                    journalService.updateJournal(receiveBill);
                }
            }
            MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receives);
        }

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public Map<String,Object> generateRecvVoucherNew(List<Long> ids)  throws Exception{
        Map<String,Object> resMap = new HashMap<>();
        int i = 0;
        if (ids.size() > 0){
            List<ReceiveBill> list = receiveBillServiceImpl.queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (ReceiveBill receiveBill : list){
                receiveBill.set("_entityName", ReceiveBill.ENTITY_NAME);
                if(BillInfoUtils.getBusinessDate() != null) {
                    receiveBill.setSettledate(new Date());
                    receiveBill.setDzdate(BillInfoUtils.getBusinessDate());
                }else {
                    receiveBill.setSettledate(new Date());
                    receiveBill.setDzdate(new Date());
                }
                CtmJSONObject jsonObject = cmpVoucherService.generateVoucherWithResult(receiveBill);
                if(jsonObject!= null && !(Boolean)jsonObject.get("dealSucceed")){
                    log.error("收款单生成凭证失败！", jsonObject.get("message"));
                    i++;
                    continue;
                }
            }
        }
        resMap.put("failCount",i);
        return resMap;
    }
}
