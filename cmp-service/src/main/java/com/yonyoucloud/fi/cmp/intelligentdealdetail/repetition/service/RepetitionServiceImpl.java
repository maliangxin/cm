package com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.service;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author maliangn
 * @since 2024-06-22
 */
@Service
@Slf4j
public class RepetitionServiceImpl implements IRepetitionService {

    static String FORMAT_DATE_PATTREN = "yyyy-MM-dd HH:mm:ss";


    /**
     * 格式化多要素信息
     *
     * @param bankReconciliation
     * @return
     */
    @Override
    public String formatConctaInfoBankReconciliation(BankReconciliation bankReconciliation){
        StringBuilder concatInfo = new StringBuilder();
        try{
            String dateStr = bankReconciliation.getString("tran_date");
            Date tran_date = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
            String tran_dateStr = null;
            if (tran_date != null) {
                tran_dateStr = DateUtils.convertToStr(tran_date, FORMAT_DATE_PATTREN);
                bankReconciliation.setTran_date(tran_date);
            }
            String timeStr = bankReconciliation.getString("tran_time");
            if (StringUtils.isNotEmpty(timeStr)) {
                Date tranTime = DateUtils.dateParse(dateStr + timeStr, DateUtils.YYYYMMDDHHMMSS);
                bankReconciliation.setTran_time(tranTime);
            }

            Date tran_time = bankReconciliation.getTran_time();
            String tran_timeStr = null;
            if (tran_time != null) {
                tran_timeStr = DateUtils.convertToStr(tran_time, FORMAT_DATE_PATTREN);
            }
            concatInfo.append(bankReconciliation.getBankaccount()+ "|");
            concatInfo.append(tran_dateStr+ "|");
            concatInfo.append(tran_timeStr+ "|");
            concatInfo.append(bankReconciliation.getTran_amt().setScale(2, BigDecimal.ROUND_HALF_UP)+ "|");
            concatInfo.append(bankReconciliation.getDc_flag().getValue() + "|");
            concatInfo.append((bankReconciliation.getBank_seq_no() == null ? "null" : bankReconciliation.getBank_seq_no()) + "|");
            concatInfo.append(bankReconciliation.getTo_acct_no() + "|");
            concatInfo.append(bankReconciliation.getTo_acct_name());
            log.error("=======formatConctaInfoBankReconciliation========concatInfo:" + concatInfo);
        }catch (Exception e){
            log.error("concatInfo生成异常",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102039"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080093", "concatInfo生成异常") /* "concatInfo生成异常" */,e);
        }
        return concatInfo.toString();
    }
}
