package com.yonyoucloud.fi.cmp.event.vo;

import lombok.Data;

/**
 * <h1>EventInfo</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-02-21 13:47
 */
@Data
public class EventInfo {
    String srcBusId;
    String srcExtraInfo;
    String fullName;
    boolean fundBillFlag;
    Long fiEventDataVersion;
    Long voucherVersion;
    String glVoucherId;
    String voucherId;
    String yTenantId;
    String glVoucherType;
    String glVoucherNo;
    String periodCode;
    String srcBillTypeId;
    String srcClassifier;
}
