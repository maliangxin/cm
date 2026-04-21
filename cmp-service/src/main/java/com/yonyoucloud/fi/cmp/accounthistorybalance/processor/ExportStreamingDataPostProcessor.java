package com.yonyoucloud.fi.cmp.accounthistorybalance.processor;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.poi.service.IExportStreamingDataPostProcessor;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.service.CmpCurrencyConversionService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author qihaoc
 * @Description:历史余额导出流式数据处理
 * @date 2024/5/2 21:02
 */
@Slf4j
@Component("hisBalExportStreamingDataPostProcessor")
public class ExportStreamingDataPostProcessor implements IExportStreamingDataPostProcessor {

    @Autowired
    BankAccountSettingService bankAccountSettingService;

    @Autowired
    private CmpCurrencyConversionService cmpCurrencyConversionService;

    private static final String CMPHISBAL = "cmp_hisbalist";

    @Override
    public boolean accept(BillDataDto billDataDto, List list) {
        String billnum = billDataDto.getBillnum();
        if (CMPHISBAL.equals(billnum)) {
            return true;
        }
        return false;
    }

    @SneakyThrows
    @Override
    public List postProcessing(BillDataDto billDataDto, List dataList) {
        if (CollectionUtils.isNotEmpty(dataList)) {
            List<String> bankaccounts = new ArrayList<>();
            for (Object data : dataList) {
                Map historyBalanceInfo  = (Map) data;
                if (historyBalanceInfo.get("enterpriseBankAccount") != null && !StringUtils.isEmpty(historyBalanceInfo.get("enterpriseBankAccount").toString())) {
                    bankaccounts.add(historyBalanceInfo.get("enterpriseBankAccount").toString());
                }
            }
            List<Map<String, Object>> enterpriseBankAccounts = bankAccountSettingService.queryBankAccountSettingByBankAccounts(bankaccounts);
            for (Object data : dataList) {
                Map historyBalanceInfo  = (Map) data;
                if (historyBalanceInfo.get("enterpriseBankAccount") != null) {
                    if (enterpriseBankAccounts.stream().anyMatch(e -> e.get("enterpriseBankAccount").equals(historyBalanceInfo.get("enterpriseBankAccount")))) {
                        historyBalanceInfo.put("openFlag", 1);//直连
                    } else {
                        historyBalanceInfo.put("openFlag", 0);
                    }
                }
            }
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
