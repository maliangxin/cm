package com.yonyoucloud.fi.cmp.paybill;

import com.yonyoucloud.fi.cmp.journal.Journal;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 本类主要用于
 *
 * @author liuhaoi
 * @since Created At 2021/3/4 0004 10:29
 */
@Data
@Builder
public class PaymentBillSettleBO {

    private String failedId;
    private String message;
    private List<Journal> journalList;
    private PayBill updateBill;

    public List<Journal> getJournalList() {
        if(journalList==null){
            return Collections.emptyList();
        }
        return journalList;
    }

}
