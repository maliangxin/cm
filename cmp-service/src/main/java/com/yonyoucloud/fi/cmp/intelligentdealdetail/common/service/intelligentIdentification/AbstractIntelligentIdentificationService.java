package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyBO;

public abstract class AbstractIntelligentIdentificationService {

    /**
     *
     * @param cacheClassifyBO
     * @param bankReconciliation
     */
    public void setOppoSiteType(BankReconciliation bankReconciliation, BillSmartClassifyBO cacheClassifyBO){
        if(cacheClassifyBO != null){
            //对方id
            bankReconciliation.setOppositeobjectid(cacheClassifyBO.getOppositeobjectid());
            //对方类型
            bankReconciliation.setOppositetype(cacheClassifyBO.getOppositetype());
            //对方名称
            bankReconciliation.setOppositeobjectname(cacheClassifyBO.getOppositeobjectname());
            //对方银行账号的id
            bankReconciliation.setTo_acct(cacheClassifyBO.getOppositebankacctid());
        }
    }


}
