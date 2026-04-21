package com.yonyoucloud.fi.cmp.bill;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.yonbip.iuap.xport.common.api.pojo.ExcelActionRequest;
import com.yonyou.yonbip.iuap.xport.common.model.WorkbookMetaInfo;
import com.yonyou.yonbip.iuap.xport.export.extension.ExportDataPostProcessingExtension;
import com.yonyou.yonbip.iuap.xport.mdd.export.web.MddExcelExportActionRequest;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.service.CmpCurrencyConversionService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <h1>现金管理单据导出时。凭证号导出支持多语显示</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-09 15:06
 */
@Slf4j
@Component("CmpExportDataPostProcessingExtension")
@Order(-1)
public class CmpExportMddExtComQueryApiService implements ExportDataPostProcessingExtension {

    private static final List<String> SERVICE_CODE_LIST = new ArrayList<>();
    static {
        SERVICE_CODE_LIST.add(IServicecodeConstant.FUNDPAYMENT);
        SERVICE_CODE_LIST.add(IServicecodeConstant.FUNDCOLLECTION);
        SERVICE_CODE_LIST.add(IServicecodeConstant.PAYMENTBILL);
        SERVICE_CODE_LIST.add(IServicecodeConstant.RECEIVEBILL);
        SERVICE_CODE_LIST.add(IServicecodeConstant.TRANSFERACCOUNT);
        SERVICE_CODE_LIST.add(IServicecodeConstant.CMPBANKRECONCILIATION);//银行对账单
        SERVICE_CODE_LIST.add(IServicecodeConstant.RETIBALIST);//账户实时余额
        SERVICE_CODE_LIST.add(IServicecodeConstant.ACCHISBAL);//银行账户历史余额
        SERVICE_CODE_LIST.add(IServicecodeConstant.DLLIST);//银行账户交易明细
        SERVICE_CODE_LIST.add(IServicecodeConstant.BANKRECEIPTMATCH);//银行交易回单
        SERVICE_CODE_LIST.add(IServicecodeConstant.FOREIGNPAYMENT);//外汇付款
        SERVICE_CODE_LIST.add(IServicecodeConstant.EXCHANGEGAINLOSS);//现金汇兑损益
    }

    @Autowired
    BankAccountSettingService bankAccountSettingService;
    @Autowired
    private CmpCurrencyConversionService cmpCurrencyConversionService;


    @Override
    public List<Map<String, Object>> postProcessing(@NotNull ExcelActionRequest exportRequest, WorkbookMetaInfo metaInfo, @NotNull List<Map<String, Object>> dataList) {
        if (CollectionUtils.isNotEmpty(dataList)) {
            for (Map<String, Object> data : dataList) {
                try {
                    Map historyBalanceInfo  = (Map) data;
                    Object objVoucherNo = historyBalanceInfo.get("voucherNo");
                    if (ValueUtils.isNotEmptyObj(objVoucherNo)) {
                        JSONObject jsonObject = JSON.parseObject(String.valueOf(objVoucherNo));
                        String locale = InvocationInfoProxy.getLocale();
                        switch (locale) {
                            case "zh_CN":
                                historyBalanceInfo.put("voucherNo", ValueUtils.isNotEmptyObj(jsonObject.get("zh_CN")) ? jsonObject.get("zh_CN") : "");
                                break;
                            case "en_US":
                                historyBalanceInfo.put("voucherNo", ValueUtils.isNotEmptyObj(jsonObject.get("en_US")) ? jsonObject.get("en_US") : "");
                                break;
                            case "zh_TW":
                                historyBalanceInfo.put("voucherNo", ValueUtils.isNotEmptyObj(jsonObject.get("zh_TW")) ? jsonObject.get("zh_TW") : "");
                                break;
                            default:
                                historyBalanceInfo.put("voucherNo", "");
                        }
                    }
                } catch (Exception e) {
                    log.error("export voucher No. error:{}", e.getMessage());
                }
            }

            // 流式导出 银行流水认领、账户实时余额、银行账户历史余额、账户交易流水、银行交易回单
            String serviceCode = exportRequest.getServiceCode();
            switch (serviceCode) {
                //银行对账单
                case IServicecodeConstant.CMPBANKRECONCILIATION:
                    //账户实时余额
                case IServicecodeConstant.RETIBALIST:
                    //银行账户交易明细
                case IServicecodeConstant.DLLIST:
                    //银行交易回单
                case IServicecodeConstant.BANKRECEIPTMATCH:
                    try {
                        MddExcelExportActionRequest request = (MddExcelExportActionRequest) exportRequest;
                        BillDataDto billDataDto = request.getBill();
                        // 获取筛选区入参
                        Map<String,Object> billDataMap = (Map<String,Object>)billDataDto.getExternalData();
                        // 折算币种
                        String targetCurrencyId = billDataMap.get("natCurrencyId") == null ? "" : billDataMap.get("natCurrencyId").toString();
                        // 汇率类型
                        String exchangeRateType = billDataMap.get("exchangeRateType") == null ? "" : billDataMap.get("exchangeRateType").toString();
                        cmpCurrencyConversionService.handleCurrencyConversion(billDataDto, dataList, targetCurrencyId, exchangeRateType);
                    } catch (Exception e) {
                        log.error("流式导出后置逻辑处理时出错!",e);
                    }
                    break;

                //银行账户历史余额 历史余额导出流式数据处理
                case IServicecodeConstant.ACCHISBAL:
                    try {
                        MddExcelExportActionRequest request = (MddExcelExportActionRequest) exportRequest;
                        BillDataDto billDataDto = request.getBill();
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
                    } catch (Exception e) {
                        log.error("银行账户历史余额流式导出后置逻辑处理时出错!",e);
                    }
                    break;

                //外汇付款 交易编码赋值
                case IServicecodeConstant.FOREIGNPAYMENT:
                    try {

                        for (Map<String, Object> data : dataList) {
                            Map foreignpaymentInfo  = (Map) data;
                            if (foreignpaymentInfo.get("transactioncodeA") != null && !StringUtils.isEmpty(foreignpaymentInfo.get("transactioncodeA").toString())) {
                                List<BizObject> transactioncodeA_code = QueryBaseDocUtils.getTradeCodeByIdOrCode(foreignpaymentInfo.get("transactioncodeA").toString());
                                foreignpaymentInfo.put("transactioncodeA_trade_code", transactioncodeA_code.get(0).get("trade_code"));
                            }
                            if (foreignpaymentInfo.get("transactioncodeB") != null && !StringUtils.isEmpty(foreignpaymentInfo.get("transactioncodeB").toString())) {
                                List<BizObject> transactioncodeB_code = QueryBaseDocUtils.getTradeCodeByIdOrCode(foreignpaymentInfo.get("transactioncodeB").toString());
                                foreignpaymentInfo.put("transactioncodeB_trade_code", transactioncodeB_code.get(0).get("trade_code"));
                            }
                        }


                    } catch (Exception e) {
                        log.error("外汇付款交易编码赋值时出错!",e);
                    }
                    break;
                default:
                    break;
            }


        }
        return dataList;
    }

    @Override
    public List<Map<String, Object>> postProcessingSumData(@NotNull ExcelActionRequest exportRequest, WorkbookMetaInfo metaInfo, @Nullable List<Map<String, Object>> sumData) {
        return Collections.emptyList();
    }

    @Override
    public boolean accept(@NotNull ExcelActionRequest exportRequest) {
        String serviceCode = exportRequest.getServiceCode();
        return SERVICE_CODE_LIST.contains(serviceCode);
    }
}
