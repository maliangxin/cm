package com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyoucloud.fi.cmp.cmpentity.ClaimCompleteType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.FIEPUBApiUtil;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class    BankreconciliationNoProcess implements CheckAndFilterStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BankreconciliationNoProcess.class);

    @Override
    public void checkDataLegalList(List<BizObject> bills, BankreconciliationActionEnum action) {
        if (BankreconciliationActionEnum.RECEIPTASSOCIATION.equals(action) || BankreconciliationActionEnum.NORECEIPTASSOCIATION.equals(action)) {
            return;
        }
        for (BizObject bill : bills) {
            String bankSeqNo = ValueUtils.isNotEmptyObj(bill.get("bank_seq_no")) ? bill.get("bank_seq_no").toString() : null;
            String serialdealType = ValueUtils.isNotEmptyObj(bill.get("serialdealtype")) ? bill.get("serialdealtype").toString() : null;
            if (String.valueOf(ClaimCompleteType.NoProcess.getValue()).equals(serialdealType)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103001"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E9838EA05A80003", "交易流水号【%s】已标记无需处理，不允许进行该操作，请检查！"), bankSeqNo) /* "交易流水号【%s】已标记无需处理，不允许进行该操作，请检查！" */);
            }
            if (String.valueOf(ClaimCompleteType.THIRD.getValue()).equals(serialdealType)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103002"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E98333605A80002", "交易流水号【%s】已发布至第三方系统处理，不允许进行该操作，请检查！"), bankSeqNo) /* "交易流水号【%s】已发布至第三方系统处理，不允许进行该操作，请检查！" */);
            }
        }
    }

    @Override
    public String checkDataLegal(BankReconciliation bill, BankreconciliationActionEnum action) {
        String bankSeqNo = ValueUtils.isNotEmptyObj(bill.get("bank_seq_no")) ? bill.get("bank_seq_no").toString() : null;
        String serialdealType = ValueUtils.isNotEmptyObj(bill.get("serialdealtype")) ? bill.get("serialdealtype").toString() : null;
        if (String.valueOf(ClaimCompleteType.NoProcess.getValue()).equals(serialdealType)) {
            return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E9838EA05A80003", "交易流水号【%s】已标记无需处理，不允许进行该操作，请检查！"), bankSeqNo/* "交易流水号【%s】已标记无需处理，不允许进行该操作，请检查！" */);
        }
        if (String.valueOf(ClaimCompleteType.THIRD.getValue()).equals(serialdealType)) {
            return String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E98333605A80002", "交易流水号【%s】已发布至第三方系统处理，不允许进行该操作，请检查！"), bankSeqNo/* "交易流水号【%s】已发布至第三方系统处理，不允许进行该操作，请检查！" */);
        }
        return StringUtils.EMPTY;
    }

    @Override
    public void checkAndFilterData(List<BankReconciliation> bills, BankreconciliationScheduleEnum scheduleTaskCodeEnum) {
        try {
            List<BizObject> filterList = new ArrayList<>();
            for (BizObject bill : bills) {
                boolean isFilter = bill.get("accentity") == null ? false : !BankreconciliationUtils.isNoProcess(bill.get("accentity").toString());
                String serialdealType = ValueUtils.isNotEmptyObj(bill.get("serialdealtype")) ? bill.get("serialdealtype").toString() : null;
                if (String.valueOf(ClaimCompleteType.NoProcess.getValue()).equals(serialdealType)) {
                    if (BankreconciliationScheduleEnum.ACCOUNTHISTORYBALANCECHECK.equals(scheduleTaskCodeEnum)
                            || BankreconciliationScheduleEnum.ACCOUNTBALANCECHECK.equals(scheduleTaskCodeEnum)
                            || BankreconciliationScheduleEnum.BANKRECONCILIATIONAUTOMATICTASK.equals(scheduleTaskCodeEnum)) {
                        if (isFilter) {
                            filterList.add(bill);
                        }
                    } else {
                        filterList.add(bill);
                    }
                }
                if (String.valueOf(ClaimCompleteType.THIRD.getValue()).equals(serialdealType) && !BankreconciliationScheduleEnum.BANSMAUTOASSOCITE.equals(scheduleTaskCodeEnum)) {
                    filterList.add(bill);
                }
            }
            bills.removeAll(filterList);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public int order() {
        return 0;
    }

    private void getLock(List<BizObject> bills, String type, boolean isSchedule) {
        List<BizObject> filterList = new ArrayList<>();
        for (BizObject bill : bills) {
            // 加锁信息：账号+行为
            String lockKey = bill.getId().toString() + ICmpConstant.BANK_RECONCILIATION + type;
            YmsLock ymsLock = null;
            try {
                ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
                if (ymsLock == null) {
                    if (!isSchedule) {
                        throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400443", "加锁失败") /* "加锁失败" */);
                    } else {
                        filterList.add(bill);
                    }
                }
                // 释放锁
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            } catch (Exception e) {
                logger.error("queryBankAccountTransactionDetail", e);
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        bills.removeAll(filterList);
    }
}
