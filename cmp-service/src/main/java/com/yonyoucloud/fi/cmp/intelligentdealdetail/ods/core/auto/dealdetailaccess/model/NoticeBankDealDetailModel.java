package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model;

import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponseRecord;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author guoyangy
 * @Date 2024/2/27 21:41
 * @Description 流水操作日志模型
 * @Version 1.0
 */
@Data
public class NoticeBankDealDetailModel<T> extends BankDealDetailModel<T>{

    //牧原到账通知使用·
    public List<BankUnionResponseRecord> responseParseFailRecords;
    public NoticeBankDealDetailModel(){}

    public List<BankUnionResponseRecord> getResponseParseFailRecords() {
        if(CollectionUtils.isEmpty(this.responseParseFailRecords)){
           this.responseParseFailRecords = new ArrayList<>();
        }
        return this.responseParseFailRecords;
    }

    public void addBankUnionResponseRecordList(List<BankUnionResponseRecord> responseParseFailRecords){
        this.getResponseParseFailRecords().addAll(responseParseFailRecords);
    }
    public void addBankUnionResponseRecord(BankUnionResponseRecord responseParseFailRecord){
        this.getResponseParseFailRecords().add(responseParseFailRecord);
    }
}