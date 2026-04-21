package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.iuap.file.rpc.ApFileDeleteService;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptHandleDataService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 银行交易回单删除后规则
 *
 * @author xuyao
 * @version 1.0
 * @since 2023-11-27 16:58
 */
@Slf4j
@Component("bankReceiptAfterDeleteRule")
public class BankReceiptAfterDeleteRule extends AbstractCommonRule {

    @Autowired
    private BankreconciliationService bankreconciliationService;
    @Autowired
    private ApFileDeleteService apFileDeleteService;
    @Autowired
    private  BankReceiptHandleDataService bankReceiptHandleDataService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            BankElectronicReceipt bankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, bizObject.getId());
            //删除前回单关联状态已被改为未关联，有文件id的发送取消关联的事件，不用判断回单关联状态了；有可能流水已被删除，所有不用判断是否有流水了
            if(ObjectUtils.isNotEmpty(bankElectronicReceipt.getExtendss()) && ObjectUtils.isNotEmpty(bankElectronicReceipt.getBankreconciliationid())){
                bankreconciliationService.cancelUrl(Long.valueOf(bankElectronicReceipt.getBankreconciliationid()),bankElectronicReceipt.getExtendss());
            }
            try {
                if (bankElectronicReceipt.getIsdown() && bankElectronicReceipt.getExtendss() != null) {
                    //删除文件，逻辑删除，会定期自动清理
                    String fieldId = bankReceiptHandleDataService.handleExtendss(bankElectronicReceipt.getExtendss(), bankElectronicReceipt.getId());
                    apFileDeleteService.batchMarkDeleteFiles(ICmpConstant.APPCODE, Arrays.asList(fieldId));
                }
            } catch (Exception e) {
                log.error("回单删除文件失败：", e);
            }
        }
        return new RuleExecuteResult();
    }


}
