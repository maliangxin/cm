package com.yonyoucloud.fi.cmp.billclaim;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2022/10/14 18:13
 */
@Data
public class BillClaimItemCheckEntity {
    /**
     * 认领明细表Id
     */
    private Long billItemId;
    /**
     * 明细金额
     */
    private BigDecimal sumAmount;
}
