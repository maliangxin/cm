package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyoucloud.fi.cmp.bankreceipt.dto.BankEleReceiptDTO;

import java.util.List;
import java.util.Map;

/**
 * @Description 处理银行交易回单数据类
 * @Author hanll
 * @Date 2024/3/21-10:07
 */
public interface BankReceiptHandleDataService {

    /**
     * 处理文件id
     * @param bankelereceipt 银行交易回单
     * @return
     */
    String handleExtendss(Map<String, Object> bankelereceipt) throws Exception;

    /**
     * 批量处理文件id
     * @param bankElereceiptList 银行交易回单集合
     * @return
     * @throws Exception
     */
    List<Map<String, Object>> handleBatchExtendss(List<Map<String,Object>> bankElereceiptList) throws Exception;

    /**
     * 获取文件Id集合
     * @param bankElereceiptList 电子回单文件
     * @return 协同文件id集合
     * @throws Exception
     */
    List<String> getFileIds(List<Map<String,Object>> bankElereceiptList) throws Exception;

    /**
     * 处理文件Id
     * @param extendss 老的文件ID
     * @param businessId 业务主键id
     * @return
     * @throws Exception
     */
    String handleExtendss(String extendss, Long businessId) throws Exception;

    /**
     * 设置公有桶还是私有桶标识
     * @param flag
     */
    void setPrivateOrPublicFlag(String flag);
}
