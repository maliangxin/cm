package com.yonyoucloud.fi.cmp.merchant.processor;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.poi.service.IExportStreamingDataPostProcessor;
import com.yonyoucloud.fi.cmp.common.service.CmpCurrencyConversionService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 账户交易流水导出流式数据处理
 */
@Slf4j
@Component("bankDealExportStreamingDataPostProcessor")
public class BankDealExportStreamingDataPostProcessor implements IExportStreamingDataPostProcessor {

    private static final String CMPBANKDEAL = "cmp_dllist";

    @Autowired
    private CmpCurrencyConversionService cmpCurrencyConversionService;



    @Override
    public boolean accept(BillDataDto billDataDto, List list) {
        String billnum = billDataDto.getBillnum();
        if (CMPBANKDEAL.equals(billnum)) {
            return true;
        }
        return false;
    }

    @SneakyThrows
    @Override
    public List postProcessing(BillDataDto billDataDto, List dataList) {
        if (CollectionUtils.isNotEmpty(dataList)) {
            // 获取筛选区入参
            Map<String,Object> billDataMap = (Map<String,Object>)billDataDto.getExternalData();
            // 折算币种
            String targetCurrencyId = billDataMap.get("natCurrencyId") == null ? "" : billDataMap.get("natCurrencyId").toString();
            // 汇率类型
            String exchangeRateType = billDataMap.get("exchangeRateType") == null ? "" : billDataMap.get("exchangeRateType").toString();
            cmpCurrencyConversionService.handleCurrencyConversion(billDataDto, dataList, targetCurrencyId, exchangeRateType);
        }
        return dataList;
    }



}
