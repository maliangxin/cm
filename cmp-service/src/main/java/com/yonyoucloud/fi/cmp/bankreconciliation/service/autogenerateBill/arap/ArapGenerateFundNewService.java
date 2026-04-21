package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.arap;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.BusinessGenerateEarapbillService;
import com.yonyoucloud.fi.cmp.enums.EarapBizflowEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
public class ArapGenerateFundNewService{

    @Autowired
    private BusinessGenerateEarapbillService businessGenerateEarapbillService;

    /**
     * 1,根据autoorderrule判断生成收款单或付款单 -- 枚举 EventType
     * 2,调用收付款单不同的生单Service
     *
     * @param earapList
     * @param earapBizflowEnum
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public String generateBill(List<BankReconciliation> earapList, EarapBizflowEnum earapBizflowEnum) throws Exception {
        StringBuffer retMsg = new StringBuffer();
        // 应收应付单据
        if (CollectionUtils.isNotEmpty(earapList)) {
            for (BankReconciliation bankReconciliation : earapList) {
                try {
                    bankReconciliation.set("pushDownMark", "true");
                    businessGenerateEarapbillService.doGenerateBillBySingle(bankReconciliation, earapBizflowEnum);
                } catch (Exception e) {
                    bankReconciliation.set("pushDownErrorMessage",e.getMessage().toString());
                    log.error("自动生成应付付款单异常，异常原因：" + e.getMessage());
                    retMsg.append(e.getMessage());
                }
            }
        }
        return retMsg.toString();
    }

}
