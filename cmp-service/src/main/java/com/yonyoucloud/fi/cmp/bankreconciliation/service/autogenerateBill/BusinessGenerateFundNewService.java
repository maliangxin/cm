package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.arap.ArapGenerateFundNewService;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.cm.CmGenerateFundNewService;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.fdtr.FdtrGenerateFundNewService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.enums.EarapBizflowEnum;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
public class BusinessGenerateFundNewService {

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    @Autowired
    ArapGenerateFundNewService arapGenerateFundNewService;

    @Autowired
    FdtrGenerateFundNewService fdtrGenerateFundNewService;

    @Autowired
    CmGenerateFundNewService cmGenerateFundNewService;

    /**
     * 1,根据autoorderrule判断生成收款单或付款单 -- 枚举 EventType
     * 2,调用收付款单不同的生单Service
     *
     * @param bankReconciliations
     * @param autoSubmit
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public String bankreconciliationGenerateDoc(List<BankReconciliation> bankReconciliations, boolean autoSubmit) throws Exception {

        StringBuffer retMsg = new StringBuffer();
        //筛选资金付款单
        //财资统一对账码：&& !b.getIsparsesmartcheckno() 财资统一对账码是解析过来的不参与自动生单
        List<BankReconciliation> generateFundPayMentList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.FundPayment.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //筛选资金收款单
        List<BankReconciliation> generateFundCollectionList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.FundCollection.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //下推归集付款单
        List<BankReconciliation> fdtrPaymentList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.Collect_payment_slips.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //下推归集收款单
        List<BankReconciliation> fdtrCollectList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.Collect_receipts.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //下推下拨付款单
        List<BankReconciliation> fdtrAllocatePaymentList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.Disburse_payment_slip.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //结算短路，资金归集
        List<BankReconciliation> fdtrShortCollectList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.fdtr_fund_collect_serial.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //结算短路，资金下拨
        List<BankReconciliation> fdtrShortAllocationList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.fdtr_fund_allocation.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //下推下拨收款单
        List<BankReconciliation> fdtrAllocateCollectList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.IHand_out_receipt.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //应付付款款
        List<BankReconciliation> earapPaymentList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.eap_bill_payment.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //应付付款退款单
        List<BankReconciliation> earapRefundList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.eap_apRefund.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());
        //应收收款单
        List<BankReconciliation> earapCollectionList = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.ear_collection.getValue() == b.getGenertbilltype() && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());

        //未命中生单数据
        List<BankReconciliation> nocreateList = bankReconciliations.stream()
                .filter(b -> b.getGenertbilltype() == null && !b.getIsparsesmartcheckno())
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(generateFundPayMentList)) {
            try {
                cmGenerateFundNewService.generateFundPayMent(generateFundPayMentList, autoSubmit);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(generateFundCollectionList)) {
            try {
                cmGenerateFundNewService.generateFundCollection(generateFundCollectionList, autoSubmit);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //是否开启简强非简强才会发送资金归集的推单请求
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //事件中心推送到归集付款单
        if (!enableSimplify && CollectionUtils.isNotEmpty(fdtrPaymentList)) {
            try {
                fdtrGenerateFundNewService.generateBill(fdtrPaymentList, EventType.Collect_payment_slips);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //事件中心推送到归集收款单
        if (!enableSimplify && CollectionUtils.isNotEmpty(fdtrCollectList)) {
            try {
                fdtrGenerateFundNewService.generateBill(fdtrCollectList, EventType.Collect_receipts);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //事件中心推送到下拨付款单
        if (!enableSimplify && CollectionUtils.isNotEmpty(fdtrAllocatePaymentList)) {
            try {
                fdtrGenerateFundNewService.generateBill(fdtrAllocatePaymentList, EventType.Disburse_payment_slip);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //事件中心推送到下拨收款单
        if (!enableSimplify && CollectionUtils.isNotEmpty(fdtrAllocateCollectList)) {
            try {
                fdtrGenerateFundNewService.generateBill(fdtrAllocateCollectList, EventType.IHand_out_receipt);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //事件中心推送到结算短路，资金归集
        if (enableSimplify && CollectionUtils.isNotEmpty(fdtrShortCollectList)) {
            try {
                fdtrGenerateFundNewService.generateBill(fdtrShortCollectList, EventType.fdtr_fund_collect_serial);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //事件中心推送到结算短路，资金下拨
        if (enableSimplify && CollectionUtils.isNotEmpty(fdtrShortAllocationList)) {
            try {
                fdtrGenerateFundNewService.generateBill(fdtrShortAllocationList, EventType.fdtr_fund_allocation);
            } catch (Exception e) {
                retMsg.append(e.getMessage());
            }
        }
        //应付付款款
        if (CollectionUtils.isNotEmpty(earapPaymentList)) {
            retMsg.append(arapGenerateFundNewService.generateBill(earapPaymentList, EarapBizflowEnum.EARP_PAYMENT));
        }
        //应收收款单
        if (CollectionUtils.isNotEmpty(earapCollectionList)) {
            retMsg.append(arapGenerateFundNewService.generateBill(earapCollectionList, EarapBizflowEnum.EARP_COLLECTION));
        }
        //应付付款退款单
        if (CollectionUtils.isNotEmpty(earapRefundList)) {
            retMsg.append(arapGenerateFundNewService.generateBill(earapRefundList, EarapBizflowEnum.EARP_REFUND));
        }
        //不生单的对账单需要更新已执行自动生单
        if (CollectionUtils.isNotEmpty(nocreateList)) {
            updateIsautocreatebill(nocreateList);
        }
        return retMsg.toString();
    }

    /**
     * 更新是否自动生单状态为是
     */
    private void updateIsautocreatebill(List<BankReconciliation> datalist) throws Exception {
        //已匹配到的银行对账单id
        List<Long> matchedIds = new ArrayList<>();
        for (BankReconciliation bankReconciliation : datalist) {
            matchedIds.add(bankReconciliation.getId());
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(matchedIds));
        querySchema.addCondition(group);
        List<BankReconciliation> toUpdateDataList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        toUpdateDataList.forEach(b -> {
            //设置更新状态，更改为已自动生单
            EntityTool.setUpdateStatus(b);
            b.setIsautocreatebill(true);
        });
        CommonSaveUtils.updateBankReconciliation(toUpdateDataList);
    }

}