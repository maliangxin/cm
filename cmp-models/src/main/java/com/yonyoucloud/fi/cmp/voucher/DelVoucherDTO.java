

package com.yonyoucloud.fi.cmp.voucher;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <h1>DelVoucherDTO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022/3/18 22:14
 */
@Data
@Accessors(chain = true)
@ToString
public class DelVoucherDTO implements Serializable {

    private String billId;
    private String billNo;
    private String billTypeCode;
    private String systemCode;
}

