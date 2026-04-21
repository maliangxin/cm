package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.service;

import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifysetting.BankreconciliationIdentifySetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;

import java.util.List;
import java.util.Map;

/**
 * @Author maliangn
 * @Date 2024/6/29
 * @Description 银行对账单智能识别
 * @Version 1.0
 */
public interface IntelligentIdentificationService {


    List<BankReconciliation> excuteIdentificate(List<BankReconciliation> bankReconciliationList, BankDealDetailContext context) throws Exception;

    List<BankReconciliation> excuteIdentificateForCheck(List<BankReconciliation> bankReconciliationList, BankreconciliationIdentifySetting bankreconciliationIdentifySetting, BankIdentifyService bankIdentifyService, BankDealDetailContext context) throws Exception;

    short getTypeValue();

}
