package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.iuap.context.YmsContextWrappers;
import com.yonyou.iuap.ruleengine.dto.others.T;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_EIGHT;

/**
 * @Description 币种折算服务
 * @Author hanll
 * @Date 2024/5/22-20:09
 */
@Service("cmpCurrencyConversionService")
@Slf4j
@RequiredArgsConstructor
public class CmpCurrencyConversionService {

    /**
     * 通用服务
     */
    private final CmCommonService cmCommonService;

    /**
     * 基础服务
     */
    private final BaseRefRpcService baseRefRpcService;

    /**
     * 银行交易回单
     */
    private static final String BankElectronicReceipt = "cmp_bankelectronicreceiptlist";
    /**
     * 银行流水认领
     */
    private static final String BANKRECONCILIATIONLIST = "cmp_bankreconciliationlist";
    /**
     * 账户交易明细
     */
    private static final String BANKDEALListe = "cmp_dllist";
    /**
     * 账户实时余额列表
     */
    private static final String CMPRETIBAL = "cmp_retibalist";
    /**
     * 账户历史余额列表
     */
    private static final String CMPHISBAL = "cmp_hisbalist";

    /**
     * 固定10个线程的线程池
     */
    private static final ExecutorService executorService = YmsContextWrappers.wrap(Executors.newFixedThreadPool(10));

    /**
     * 币种折算
     * @param billDataDto 单据信息
     * @param dataList 列表数据
     * @param targetCurrencyId 折算币种Id
     * @param exchangeRateType 折算汇率类型Id
     * @return List 处理后的列表数据
     */
    public List handleCurrencyConversion(BillDataDto billDataDto, List dataList, String targetCurrencyId, String exchangeRateType) throws Exception {
        if (CollectionUtils.isEmpty(dataList) || billDataDto == null) {
            return dataList;
        }
        // 单据编号
        String billno = billDataDto.getBillnum();

        if (!(CMPHISBAL.equals(billno) || BANKDEALListe.equals(billno) || CMPRETIBAL.equals(billno) || BankElectronicReceipt.equals(billno) || BANKRECONCILIATIONLIST.equals(billno))) {
            return dataList;
        }

        if (StringUtils.isEmpty(targetCurrencyId) || StringUtils.isEmpty(exchangeRateType)) {
            log.error("bankForeignCurrencyTranslationAfterListRule natCurrencyId,exchangeRateType 为空，直接返回");
            return dataList;
        }

        Map<String, BigDecimal> exchangerateMap = new ConcurrentHashMap<>(CONSTANT_EIGHT);
        long timeStart = System.currentTimeMillis();
        log.error("---------bankEctronictReceiptAfterListRule timeStart{}--------",timeStart);
        // 币种精度
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(targetCurrencyId);
        if (currencyTenantDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100950"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418061E", "当前币种不存在") /* "当前币种不存在" */);
        }

        Integer moneydigit = currencyTenantDTO.getMoneydigit();
        Integer moneyrount = currencyTenantDTO.getMoneyrount();
        // 查询汇率
        ExchangeRateTypeVO exchangeRateTypeVO = cmCommonService.getExchangeRateType(exchangeRateType);
        log.error("查询币种和汇率耗时 cost:{}", (System.currentTimeMillis() - timeStart));
        List<CompletableFuture> completableFutureList = new ArrayList<>();
        for (Object tmpRecord : dataList) {
            CompletableFuture r = asyncHandleExchangeRate((Map<String,Object>) tmpRecord, billno, targetCurrencyId,currencyTenantDTO, exchangerateMap, exchangeRateTypeVO, moneydigit, moneyrount);
            completableFutureList.add((CompletableFuture) r);
        }
        CompletableFuture.allOf((CompletableFuture[]) completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).join();
        log.error("bankForeignCurrencyTranslationAfterListRule cost:{}ms", (System.currentTimeMillis() - timeStart));
        return dataList;
    }

    /**
     * 处理汇率类型精度和币种精度
     * @param map 列表行数据
     * @param exchangeRateTypeVO 汇率类型
     * @param currencyTenantDTO 币种
     */
    private void setRound(Map<String, Object> map, ExchangeRateTypeVO exchangeRateTypeVO, CurrencyTenantDTO currencyTenantDTO) {
        // 汇率类型精度
        if (ObjectUtils.isNotEmpty(exchangeRateTypeVO)) {
            map.put("currencyexchangeratetype", exchangeRateTypeVO.getId());
            map.put("currencyexchangeratetype_name", exchangeRateTypeVO.getName());
            map.put("currencyexchangeratetype_digit", exchangeRateTypeVO.getDigit());
            map.put("currencyexchangeratetype_code", exchangeRateTypeVO.getCode());
        }
        // 折算金额精度
        map.put("natCurrency_name", currencyTenantDTO.getName());
        map.put("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
        map.put("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
        map.put("natCurrencyId", currencyTenantDTO.getId());
    }


    /**
     * 异步处理汇率
     * @param map 行数据
     * @param billno 单据编码
     * @param targetCurrencyId 折算币种id
     * @param currencyTenantDTO 币种
     * @param exchangerateMap 汇率类型
     * @param exchangeRateTypeVO 汇率类型
     * @param moneydigit 币种精度
     * @param moneyrount 精度近似方式
     * @return
     */
    private CompletableFuture asyncHandleExchangeRate(Map<String, Object> map, String billno,  String targetCurrencyId, CurrencyTenantDTO currencyTenantDTO,Map<String, BigDecimal> exchangerateMap, ExchangeRateTypeVO exchangeRateTypeVO, Integer moneydigit, Integer moneyrount) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync((() -> {
            Date billdate = getBillDate(map, billno);
            // 设定汇率类型精度和币种精度
            setRound(map, exchangeRateTypeVO, currencyTenantDTO);
            try {
                // 查询汇率类型
                BigDecimal exchangerate = queryExchangeRate(map, billdate, targetCurrencyId, exchangeRateTypeVO, exchangerateMap);
                if (null != exchangerate) {
                    calculateObversionData(map, exchangerate, moneydigit, billno, moneyrount);
                }
            } catch (Exception e) {
                log.error("queryExchangeRateSync fail:{}", e.getMessage(), e);
                return null;
            }
            return 1;
        }), executorService).handle((r, e) -> {
            if (e != null) {
                log.error("queryExchangeRateSync handle error{}:", e.getMessage(), e);
            }
            return null;
        });
        return future;
    }

    /**
     * 查找汇率
     * @param map 行数据
     * @param billdate 单据日期
     * @param targetCurrencyId 折算币种
     * @param exchangeRateType 折算汇率类型
     * @param exchangerateMap 汇率Map缓存 key 折算币种、汇率类型、币种、单据日期； value 折算汇率
     * @return 折算汇率
     * @throws Exception 查询汇率接口可能抛出的异常
     */
    private BigDecimal queryExchangeRate(Map<String, Object> map, Date billdate, String targetCurrencyId, ExchangeRateTypeVO exchangeRateTypeVO, Map<String, BigDecimal> exchangerateMap) throws Exception {
        String exchangeRateType = exchangeRateTypeVO.getId();
        String key = targetCurrencyId + "_" + exchangeRateType + "_" + map.get("currency") + "_" + billdate;
        if (ValueUtils.isNotEmptyObj(exchangerateMap.get(key))) {
            return exchangerateMap.get(key);
        }
        // 汇率
        BigDecimal exchangerate = null;
        if (map.containsKey("currency") && map.get("currency") != null && StringUtils.isNotBlank(String.valueOf(map.get("currency")))) {
            // 原币种是行上币种
            String natCurrencyId = (String) map.get("currency");
            // 如果本币币种与原币币种相同，则汇率为1
            if (!natCurrencyId.equals(targetCurrencyId)) {
                ExchangeRate exchangeRate = baseRefRpcService.queryRateByExchangeType(natCurrencyId, targetCurrencyId, billdate, exchangeRateType);
                if (exchangeRate != null) {
                    exchangerate = BigDecimal.valueOf(exchangeRate.getExchangerate()).setScale(exchangeRateTypeVO.getDigit(), RoundingMode.HALF_UP);
                }
            } else {
                exchangerate = new BigDecimal(1).setScale(exchangeRateTypeVO.getDigit(), RoundingMode.HALF_UP);
            }
        }
        if (null != exchangerate) {
            exchangerateMap.put(key, exchangerate);
        }
        return exchangerate;
    }

    /**
     * 获取单据行
     * @param map 行数据
     * @param billno 单据编码
     * @return
     */
    private Date getBillDate(Map<String, Object> map, String billno) {
        Date billdate = null;
        switch (billno) {
            //银行交易回单
            case BankElectronicReceipt:
            // 银行交易流水
            case BANKDEALListe:
                try {
                    billdate = DateUtils.parseDate(map.get("tranDate").toString());
                } catch (Exception e) {
                    log.error("日期转换错误!",e);
                }
                break;
            // 银行流水认领
            case BANKRECONCILIATIONLIST:
                try {
                    billdate = DateUtils.parseDate(map.get("tran_date").toString()) ;
                } catch (Exception e) {
                    log.error("日期转换错误！",e);
                }
                break;
            // 账户当日余额
            case CMPRETIBAL:
                Calendar calendar = Calendar.getInstance();
                billdate = new Date();
                break;
            // 账户历史余额
            case CMPHISBAL:
                try {
                    Object balanceDate = map.get("balancedate");
                    if (balanceDate instanceof CellData) {
                        CellData cd = (CellData) balanceDate;
                        balanceDate = cd.getOriginalValue();
                    }
                    billdate =  DateUtils.parseDate(balanceDate.toString());
                } catch (Exception e) {
                    log.error("日期转换错误！", e);
                }
                break;
            default:
                break;
        }
        return billdate;
    }


    /**
     * 不同的单据计算折算数据
     * @param map 行数据
     * @param exchangerate 折算汇率
     * @param moneyDigit 币种精度
     * @param billno 单据编码
     * @param moneyRound 处理精度方式
     */
    private void calculateObversionData(Map<String, Object> map, BigDecimal exchangerate, Integer moneyDigit, String billno, Integer moneyRound) {
        // 折算汇率
        map.put("obversion_exchangerate", exchangerate);
        switch (billno) {
            // 银行交易回单
            case BankElectronicReceipt:
                //折算交易金额
                if (map.containsKey("tran_amt") && map.get("tran_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("tran_amt")))) {
                    map.put("obversion_tran_amt", calculate(map.get("tran_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算利息
                if (map.containsKey("interest") && map.get("interest") != null && StringUtils.isNotBlank(String.valueOf(map.get("interest")))) {
                    map.put("obversion_interest", calculate(map.get("interest"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            // 银行流水认领
            case BANKRECONCILIATIONLIST:
                // 折算金额
                if (map.containsKey("tran_amt") && map.get("tran_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("tran_amt")))) {
                    map.put("obversion_tran_amt", calculate(map.get("tran_amt"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算余额
                if (map.containsKey("acct_bal") && map.get("acct_bal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acct_bal")))) {
                    map.put("obversion_acct_bal", calculate(map.get("acct_bal"), exchangerate, moneyDigit, moneyRound));
                }
                // 借方是支出，贷方是收入
                // 折算借方金额
                if (map.containsKey("debitamount") && map.get("debitamount") != null && StringUtils.isNotBlank(String.valueOf(map.get("debitamount")))) {
                    map.put("obversion_debitamount", calculate(map.get("debitamount"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算贷方金额
                if (map.containsKey("creditamount") && map.get("creditamount") != null && StringUtils.isNotBlank(String.valueOf(map.get("creditamount")))) {
                    map.put("obversion_creditamount", calculate(map.get("creditamount"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算认领金额
                if (map.containsKey("claimamount") && map.get("claimamount") != null && StringUtils.isNotBlank(String.valueOf(map.get("claimamount")))) {
                    map.put("obversion_claimamount", calculate(map.get("claimamount"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算待认领金额
                if (map.containsKey("amounttobeclaimed") && map.get("amounttobeclaimed") != null && StringUtils.isNotBlank(String.valueOf(map.get("amounttobeclaimed")))) {
                    map.put("obversion_amounttobeclaimed", calculate(map.get("amounttobeclaimed"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算利息
                if (map.containsKey("interest") && map.get("interest") != null && StringUtils.isNotBlank(String.valueOf(map.get("interest")))) {
                    map.put("obversion_interest", calculate(map.get("interest"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            // 银行交易流水
            case BANKDEALListe:
                // 折算交易金额
                if (map.containsKey("tran_amt") && map.get("tran_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("tran_amt")))) {
                    map.put("obversion_tran_amt", calculate(map.get("tran_amt"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算余额
                if (map.containsKey("acctbal") && map.get("acctbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acctbal")))) {
                    map.put("obversion_acctbal", calculate(map.get("acctbal"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            // 账户当日余额
            case CMPRETIBAL:

                // 折算合计余额
                if (map.containsKey("total_amt") && map.get("total_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("total_amt")))) {
                    map.put("obversion_total_amt", calculate(map.get("total_amt"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算账户余额
                if (map.containsKey("acctbal") && map.get("acctbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acctbal")))) {
                    map.put("obversion_acctbal", calculate(map.get("acctbal"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算可用余额
                if (map.containsKey("avlbal") && map.get("avlbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("avlbal")))) {
                    map.put("obversion_avlbal", calculate(map.get("avlbal"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算冻结金额
                if (map.containsKey("frzbal") && map.get("frzbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("frzbal")))) {
                    map.put("obversion_frzbal", calculate((BigDecimal) map.get("frzbal"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算定期余额
                if (map.containsKey("regular_amt") && map.get("regular_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("regular_amt")))) {
                    map.put("obversion_regular_amt", calculate(map.get("regular_amt"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算透支余额
                if (map.containsKey("overdraftbalance") && map.get("overdraftbalance") != null && StringUtils.isNotBlank(String.valueOf(map.get("overdraftbalance")))) {
                    map.put("obversion_overdraftbalance", calculate(map.get("overdraftbalance"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算昨日金额
                if (map.containsKey("yesterbal") && map.get("yesterbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("yesterbal")))) {
                    map.put("obversion_yesterbal", calculate(map.get("yesterbal"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            // 账户历史余额
            case CMPHISBAL:
                // 折算账户余额
                if (map.containsKey("acctbal") && map.get("acctbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acctbal")))) {
                    map.put("obversion_acctbal", calculate(map.get("acctbal"), exchangerate, moneyDigit, moneyRound));
                }

                // 折算可用余额
                if (map.containsKey("avlbal") && map.get("avlbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("avlbal")))) {
                    map.put("obversion_avlbal", calculate(map.get("avlbal"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算冻结金额
                if (map.containsKey("frzbal") && map.get("frzbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("frzbal")))) {
                    map.put("obversion_frzbal", calculate(map.get("frzbal"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算定期余额
                if (map.containsKey("regular_amt") && map.get("regular_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("regular_amt")))) {
                    map.put("obversion_regular_amt", calculate(map.get("regular_amt"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算透支余额
                if (map.containsKey("overdraftbalance") && map.get("overdraftbalance") != null && StringUtils.isNotBlank(String.valueOf(map.get("overdraftbalance")))) {
                    map.put("obversion_overdraftbalance", calculate(map.get("overdraftbalance"), exchangerate, moneyDigit, moneyRound));
                }
                // 折算合计余额
                if (map.containsKey("total_amt") && map.get("total_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("total_amt")))) {
                    map.put("obversion_total_amt", calculate(map.get("total_amt"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            default:
                break;
        }

    }


    /**
     * 计算折算数据
     * @param paramAmount 额度
     * @param exchangerate 汇率
     * @param moneyDigit 币种精度
     * @param moneyRound 处理精度方式
     * @return 折算金额
     */
    private String calculate(Object paramAmount, BigDecimal exchangerate, Integer moneyDigit, Integer moneyRound) {
        if (paramAmount == null) {
            throw new IllegalArgumentException("paramAmount must not be null");
        }
        BigDecimal amount;
        if (paramAmount instanceof CellData) {
            CellData cellData = (CellData) paramAmount;
            amount = new BigDecimal(cellData.getOriginalValue() == null ? "0" : cellData.getOriginalValue().toString());
        } else {
            amount =  new BigDecimal(paramAmount.toString());
        }
        // 千分位处理
        return BigDecimalUtils.handleThousandth(exchangerate.multiply(amount).setScale(moneyDigit, moneyRound));
    }
}
