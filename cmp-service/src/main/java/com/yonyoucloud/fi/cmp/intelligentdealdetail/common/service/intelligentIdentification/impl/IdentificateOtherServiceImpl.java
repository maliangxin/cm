package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.impl;

import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.IntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service.intelligentIdentification.AbstractIntelligentIdentificationService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.BankMatchAndProcessUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class IdentificateOtherServiceImpl extends AbstractIntelligentIdentificationService implements IntelligentIdentificationService {

    @Override
    public short getTypeValue() {
        return OppositeType.Other.getValue();
    }

    /**
     * 所有都未辨识出来时，默认为其他
     * @param bankReconciliationList
     * @return
     * @throws Exception
     */
    @Override
    public List<BankReconciliation> excuteIdentificate(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception {
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
//            if (bankReconciliation.getOppositetype() != null || StringUtils.isEmpty(bankReconciliation.getAccentity())) {
//                continue;
//            }
            bankReconciliation.setOppositetype(getTypeValue());
            bankReconciliation.setOppositeobjectname(null);
            bankReconciliation.setOppositeobjectid(null);
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_OTHER_UNIT.getDesc(),context);
        }
        return bankReconciliationList;
    }

    @Override
    public List<BankReconciliation> excuteIdentificateForCheck(List<BankReconciliation> bankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, BankIdentifyService bankIdentifyService, BankDealDetailContext context) throws Exception {
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            if (BankMatchAndProcessUtils.identifiedInformation.equals(bankReconciliation.getString(BankMatchAndProcessUtils.identifiedInformation))) {
                continue;
            }
            bankReconciliation.setOppositetype(getTypeValue());
            bankReconciliation.setOppositeobjectname(null);
            bankReconciliation.setOppositeobjectid(null);
            context.setLogName(RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc());
            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(bankReconciliation, RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_NAME.getDesc(),RuleLogEnum.RuleLogProcess.OPPOSITE_TYPE_START_OTHER_UNIT.getDesc(),context);
        }
        return bankReconciliationList;
    }
}
