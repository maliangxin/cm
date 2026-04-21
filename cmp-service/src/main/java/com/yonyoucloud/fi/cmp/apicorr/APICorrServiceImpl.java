package com.yonyoucloud.fi.cmp.apicorr;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrDataProcessingService;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.Relationtype;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

/**
 * @author qihaoc
 * @Description:api接口操作银行对账单关联信息服务
 * @date 2023/7/8 9:58
 */
@Service
@Slf4j
public class APICorrServiceImpl implements APICorrService{

    @Autowired
    private CorrDataProcessingService corrDataProcessingService;

    @Autowired
    private CorrOperationService corrOperationService;//写入关联关系

    @Autowired
    private ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    public void insertRelation4BankBill(BankReconciliation bankReconciliation, boolean delRelation, List<BankReconciliationbusrelation_b> busrelationbList, CtmJSONObject busrelations) throws Exception {
        //关联信息全部是异构系统关联数据,需要先删除旧的关联信息后重新关联
        if (delRelation) {
            //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, busrelationbList);
            CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(busrelationbList);
        }
        //生成中间关联数据
        HashMap<String, List<CorrDataEntity>> corrDataMap = corrDataProcessingService.apiAssociatedData(bankReconciliation, busrelations);
        List<CorrDataEntity> outBillList = corrDataMap.get("" + Relationtype.HeterogeneousAssociated.getValue());
        List<CorrDataEntity> innerBillList = corrDataMap.get("" + Relationtype.AutoAssociated.getValue());
        //异构系统关联信息，直接入库生成信息
        if (outBillList != null && !outBillList.isEmpty()) {
            int ordernum = 1;
            for (CorrDataEntity corrEntity : outBillList) {
                corrOperationService.corrOpration(corrEntity,ordernum);
                ordernum++;
                //处理银行对账单关联关系
                reWriteBusCorrDataService.reWriteBankReconciliationData(corrEntity, false);
            }
        }
        if (innerBillList != null && !innerBillList.isEmpty()) {
            //系统内单据关联信息
            int ordernum = 1;
            for (CorrDataEntity corrEntity : innerBillList) {
                //处理关联数据信息，将关联单据对应关系翻译
                corrOperationService.corrOpration(corrEntity,ordernum);
                ordernum++;
                //处理银行对账单关联关系
                reWriteBusCorrDataService.reWriteBankReconciliationData(corrEntity, false);
                if ("7".equals(corrEntity.getBillType())) { //收款单回写
                    reWriteBusCorrDataService.reWriteReceiveBillData(corrEntity);
                } else if ("10".equals(corrEntity.getBillType())) {//付款单回写
                    reWriteBusCorrDataService.reWritePayBillData(corrEntity);
                } else if ("12".equals(corrEntity.getBillType())) { //转账单回写
                    reWriteBusCorrDataService.reWriteTransferAccountData(corrEntity);
                } else if ("14".equals(corrEntity.getBillType())) {//外币兑换单回写
                    reWriteBusCorrDataService.reWriteCurrencyexchangeData(corrEntity);
                } else if ("17".equals(corrEntity.getBillType())) {
                    reWriteBusCorrDataService.reWriteFundCollectionData(corrEntity);
                } else if ("18".equals(corrEntity.getBillType())) {
                    reWriteBusCorrDataService.reWritePayMentData(corrEntity);
                }
            }
        }
    }

}
