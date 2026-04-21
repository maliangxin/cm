package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponseRecord;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @Author guoyangy
 * @Date 2024/6/26 14:13
 * @Description 流水入ods库后, 发消息实体类
 * @Version 1.0
 */
@Data
public class DealDetailConsumerDTO implements Serializable {
    public static final String TYPE_YQL = "yql";
    public static final String TYPE_MANUAL = "manual";
    public static final String TYPE_IMPORT = "import";
    private String traceId;
    private String requestSeqNo;
    // 流水来源类型： 银企联、手工、导入
    private String type;
    //分片信息，多实例消费加锁使用
    private int sequence;
    private int totalJob;
    private List<BankReconciliation> bankReconciliationList;
    //到账通知异常反参
    private List<BankUnionResponseRecord> bankUnionResponseRecords;
    private String saveDirect;

    public DealDetailConsumerDTO() {
    }

    public DealDetailConsumerDTO(String traceId, String requestSeqNo) {
        this.traceId = traceId;
        this.requestSeqNo = requestSeqNo;
    }

    public DealDetailConsumerDTO(String traceId, String requestSeqNo, String type) {
        this.traceId = traceId;
        this.requestSeqNo = requestSeqNo;
        this.type = type;
    }

    public DealDetailConsumerDTO(String traceId, String requestSeqNo, List<BankReconciliation> bankReconciliationList) {
        this.traceId = traceId;
        this.requestSeqNo = requestSeqNo;
        this.bankReconciliationList = bankReconciliationList;
    }

    public DealDetailConsumerDTO(String traceId, String requestSeqNo, List<BankReconciliation> bankReconciliationList, int sequence, int totalJob) {
        this.traceId = traceId;
        this.requestSeqNo = requestSeqNo;
        this.bankReconciliationList = bankReconciliationList;
        this.sequence = sequence;
        this.totalJob = totalJob;
    }

    public List<BankReconciliation> getBankReconciliationList() {
        return bankReconciliationList;
    }

    public void setBankReconciliationList(List<BankReconciliation> bankReconciliationList) {
        this.bankReconciliationList = bankReconciliationList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DealDetailConsumerDTO that = (DealDetailConsumerDTO) o;
        return Objects.equals(traceId, that.traceId) && Objects.equals(requestSeqNo, that.requestSeqNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, requestSeqNo);
    }
}