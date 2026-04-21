package com.yonyoucloud.fi.cmp.reconciliate;

import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillServiceImpl;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
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
public class SalPayUpSettleStatusAndGenVouchServiceImpl implements UpSettleStatusAndGenVouchService {

    private static final Logger logger = LoggerFactory.getLogger(SalPayUpSettleStatusAndGenVouchServiceImpl.class);
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    ReceiveBillServiceImpl receiveBillServiceImpl;
    @Autowired
    SalaryPayService salaryPayService;
    @Autowired
    private JournalService journalService;
    @Override
    @Transactional
    public void updateRecvSettleNew(List<Long> ids) throws Exception{
        if (ids.size() > 0){
            List<Salarypay> receives = new ArrayList<>();
            Salarypay re = null;
            List<Salarypay> dataList = queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (Salarypay entity : dataList) {
                if(!SettleStatus.alreadySettled.equals(entity.getSettlestatus())){
                    re = new Salarypay();
                    re.setId(entity.getId());
                    re.setPubts(entity.getPubts());
                    re.setPaystatus(PayStatus.OfflinePay);//支付成功
                    re.setSettlestatus(SettleStatus.alreadySettled);//已结算
                    re.setEntityStatus(EntityStatus.Update);
                    if(BillInfoUtils.getBusinessDate() != null) {
                        re.setSettledate(BillInfoUtils.getBusinessDate());
                        re.setPaydate(BillInfoUtils.getBusinessDate());
                    }else {
                        re.setPaydate(new Date());
                        re.setSettledate(new Date());
                    }
                    salaryPayService.updatePayStatusAfterOffline(re);
                    receives.add(re);
                    entity.setSettlestatus(SettleStatus.alreadySettled);
                    journalService.updateJournal(entity);
                }
            }
            MetaDaoHelper.update(Salarypay.ENTITY_NAME, receives);
        }

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public Map<String,Object> generateRecvVoucherNew(List<Long> ids) throws Exception{
        Map<String,Object> resMap = new HashMap<>();
        int i = 0;
        if (ids.size() > 0){
            List<Salarypay> list =  queryAggvoByIds(ids.toArray(new Long[ids.size()]));
            for (Salarypay entity : list){
                //journalService.updateJournal(entity);
                entity.set("_entityName", Salarypay.ENTITY_NAME);
                if(BillInfoUtils.getBusinessDate() != null) {
                    entity.setSettledate(BillInfoUtils.getBusinessDate());
                    entity.setPaydate(BillInfoUtils.getBusinessDate());
                }else {
                    entity.setPaydate(new Date());
                    entity.setSettledate(new Date());
                }
                CtmJSONObject jsonObject = cmpVoucherService.generateVoucherWithResult(entity);
                if(jsonObject!= null && !(Boolean)jsonObject.get("dealSucceed")){
                    log.error("薪资支付生成凭证失败！", jsonObject.get("message"));
                    i++;
                    continue;
                }
            }
        }
        resMap.put("failCount",i);
        return resMap;
    }

    public List<Salarypay> queryAggvoByIds(Long[] ids) throws Exception {
        List<Salarypay> receiveBillListQuery = new ArrayList<>();
        for (Long id : ids) {
            Salarypay transferAccount = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, id, 3);
            if (transferAccount != null) {
                receiveBillListQuery.add(transferAccount);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101742"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418065C","查询不到对应单据,请确认单据是否存在或刷新后重新操作") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作" */);
            }
        }
        return receiveBillListQuery;
    }
}
