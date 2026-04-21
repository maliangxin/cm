package com.yonyoucloud.fi.cmp.reconciliate.vo;

import lombok.Data;

/**
 * @description: 银行电子回单信息；用来给总账凭证推送
 * @author: wanxbo@yonyou.com
 * @date: 2024/1/16 16:27
 */
@Data
public class BankReceiptInfoVO {
    /**
     * 银行对账单id
     */
    private String bankReconciliationId;
    /**
     * 银行电子回单id
     */
    private String bankElectronicReceiptId;
    /**
     * 回单文件扩展信息；存回单文件id
     */
    private String extendss;
}
