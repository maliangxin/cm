package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.cm;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.BusinessGenerateFundService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.Relationstatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.fundcollection.service.BankRecGenFundColService;
import com.yonyoucloud.fi.cmp.fundpayment.service.BankrecGenFundPayService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 到账认领V2
 * 银行对账单 --> 自动生单 --> 资金收付款单
 * 新建service 避免侵入原BusinessGenerateFundService
 *
 * @author yp
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class CmGenerateFundNewService {

    @Autowired
    BankrecGenFundPayService bankrecGenFundPayService;
    @Resource
    private BusinessGenerateFundService businessGenerateFundService;
    @Autowired
    BankRecGenFundColService bankRecGenFundColService;
    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    /**
     * 1,根据autoorderrule判断生成收款单或付款单 -- 枚举 EventType
     * 2,调用收付款单不同的生单Service
     *
     * @param bankReconciliations
     * @param autoSubmit
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public String generateFundPayMent(List<BankReconciliation> generateFundPayMentList, boolean autoSubmit) throws Exception {
        StringBuffer retMsg = new StringBuffer();
        //生成资金付款单
        bankrecGenFundPayService.bankreconciliationGenerateFundpayment(generateFundPayMentList, autoSubmit);
        //自动确认
        autoConfirmBankreconciliation(generateFundPayMentList, EventType.FundPayment.getValue());
        return retMsg.toString();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public String generateFundCollection(List<BankReconciliation> generateFundCollectionList, boolean autoSubmit) throws Exception {
        StringBuffer retMsg = new StringBuffer();
        //生成资金收款单
        bankRecGenFundColService.bankReconciliationGenerateFundCollection(generateFundCollectionList, autoSubmit);
        //自动确认
        autoConfirmBankreconciliation(generateFundCollectionList, EventType.FundCollection.getValue());
        return retMsg.toString();
    }

    /**
     * @Describe 自动确认
     * @Param
     * @Return
     */
    private void autoConfirmBankreconciliation(List<BankReconciliation> pushData, Short eventType) {
        //根据自动配置表中是否自动生单确认flag值判断是否调用自动生单确认操作
        for (BankReconciliation bankReconciliation : pushData) {
            try {
                confirmBill(bankReconciliation, eventType);
            } catch (Exception e) {
                log.error("自动生单确认异常" + e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102591"), MessageUtils.getMessage("P_YS_FI_CM_1519536030414798857") /* "资金结算单据生单异常:" */ + e.getMessage());
            }
        }
    }

    /**
     * @Describe 处理确认后，银行对账单关联关系表数据
     * @Param
     * @Return
     */
    private void confirmBill(BankReconciliation bankReconciliation, Short eventType) throws Exception {

        BankReconciliation bankrecWithRelation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankReconciliation.getId());
        BankReconciliation bankrecWithRelationNew = new BankReconciliation(bankrecWithRelation.getId());
        List relationIdList = new ArrayList();
        List<BankReconciliationbusrelation_b> relations = bankrecWithRelation.getBizObjects("BankReconciliationbusrelation_b", BankReconciliationbusrelation_b.class);
        for (BankReconciliationbusrelation_b relation : relations) {
            if (relation.getRelationstatus() != Relationstatus.Confirmed.getValue()) {
                relationIdList.add(relation.getId());
            }
        }
        if (relationIdList == null || relationIdList.size() == 0) {
            return;
        }
        // 调用确认接口确认
        businessGenerateFundService.confirmGenerateDoc(relationIdList, eventType);

        // 添加子表数据
        relations.forEach(relation -> {
            //设置更新状态，更改为已确认
            relation.setRelationstatus(Relationstatus.Confirmed.getValue());
            EntityTool.setUpdateStatus(relation);
        });
        // 更新业务关联信息子表
        MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, relations);
        // 更新主表
        EntityTool.setUpdateStatus(bankrecWithRelationNew);
        bankrecWithRelationNew.setRelationstatus(Relationstatus.Confirmed.getValue());
        bankrecWithRelationNew.setPubts(null);
        CommonSaveUtils.updateBankReconciliation(bankrecWithRelationNew);
    }

}