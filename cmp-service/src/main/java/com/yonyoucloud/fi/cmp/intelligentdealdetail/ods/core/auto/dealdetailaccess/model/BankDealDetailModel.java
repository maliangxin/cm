package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model;

import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponseRecord;
import lombok.Data;

import java.util.List;

/**
 * @Author guoyangy
 * @Date 2024/2/27 21:41
 * @Description 流水操作日志模型
 * @Version 1.0
 */
@Data
public class BankDealDetailModel<T> {

    protected BankDealDetailOperLogModel detailOperLogModel;
    protected List<T> streamResponseRecordList;
    protected String requestSeqNo;
    public BankDealDetailModel(){}
}