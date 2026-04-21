package com.yonyoucloud.fi.cmp.bankreceipt.dto;

import lombok.Data;

/**
 * @Description
 * @Author hanll
 * @Date 2024/3/22-20:22
 */
@Data
public class BankEleReceiptDTO {

    private String extendss;

    private String businessId;

    private String fileName;

    private Long fileSize;

    public BankEleReceiptDTO(String extendss, String businessId, Long fileSize) {
        this.extendss = extendss;
        this.businessId = businessId;
        this.fileSize = fileSize;
    }

    public BankEleReceiptDTO(String extendss, String businessId, String fileName) {
        this.extendss = extendss;
        this.businessId = businessId;
        this.fileName = fileName;
    }
}
