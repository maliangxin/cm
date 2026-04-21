package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理只能流水公用方法
 */
@Slf4j
public class CmpCheckRuleCommonProcessor {


    /**
     * 判断规则开启层级
     *
     * @return
     */
    public static String loadStreamIdentifyMatchRule() {
        try{
            //step1:查询规则大类
            IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
            List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = bankDealDetailAccessDao.getBankreconciliationIdentifyTypeListByTenantId();
            if(CollectionUtils.isEmpty(bankreconciliationIdentifyTypes)){
                return "0";
            }
            List<String> codes = bankreconciliationIdentifyTypes.stream().map(BankreconciliationIdentifyType::getCode).collect(Collectors.toList());
            if (codes.contains("system001") && codes.contains("system002")) {
                return "3";
            }
            if (codes.contains("system001") && !codes.contains("system002")) {
                return "1";
            }
            if (!codes.contains("system001") && codes.contains("system002")) {
                return "2";
            }
            return "0";
        }catch (Exception e){
            log.error("加载辨识匹配规则异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400781", "加载辨识匹配规则异常！") /* "加载辨识匹配规则异常！" */);
        }
    }

    public static Boolean checkProcessStatus(BankReconciliation bankReconciliation,String ruleCode){
        if ("system001".equals(ruleCode) && (bankReconciliation.getReceiptassociation() == null || ReceiptassociationStatus.NoAssociated.getValue() == bankReconciliation.getReceiptassociation())) {
            return true;
        }else if ("system002".equals(ruleCode) && (bankReconciliation.getRefundstatus() == null || ReFundType.SUSPECTEDREFUND.getValue() == bankReconciliation.getRefundstatus())) {
            return true;
        }else {
            //回单状态：自动关联/手动关联 | 退票状态：疑似退票/退票  | 关联状态：已关联 | 完结状态：完结
            if (AssociationStatus.Associated.getValue() == bankReconciliation.getAssociationstatus() ||
                SerialdealendState.END.getValue() == bankReconciliation.getSerialdealendstate() ||
                DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus() == bankReconciliation.getProcessstatus()){
                return false;
            }
        }
        return true;
    }
}
