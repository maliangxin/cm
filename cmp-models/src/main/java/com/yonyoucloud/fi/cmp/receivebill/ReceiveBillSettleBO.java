package com.yonyoucloud.fi.cmp.receivebill;

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
public class ReceiveBillSettleBO {

    private String failedId;
    private String message;
    private List<Journal> journalList;

    public List<Journal> getJournalList() {
        if(journalList==null){
            return Collections.emptyList();
        }
        return journalList;
    }
}
