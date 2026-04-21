package com.yonyoucloud.fi.cmp.voucher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <h1>BatchDelVoucherDTO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022/3/18 22:14
 */
@Data
@Accessors(chain = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BatchDelVoucherDTO implements Serializable {

    private String reqid;
    private List<DelVoucherDTO> delRequests;

    public BatchDelVoucherDTO(@NonNull List<DelVoucherDTO> delVoucherDTOS){
        this.reqid = "cmp_" + UUID.randomUUID().toString();
        this.delRequests = delVoucherDTOS;
    }

    public static BatchDelVoucherDTO buildDTO(String billId, String billTypeCode, String systemCode){
        List<DelVoucherDTO> delRequests = new ArrayList<>();
        DelVoucherDTO dto = new DelVoucherDTO().setBillId(billId).setBillTypeCode(billTypeCode).setSystemCode(systemCode);
        delRequests.add(dto);
        return new BatchDelVoucherDTO(delRequests);
    }
}
