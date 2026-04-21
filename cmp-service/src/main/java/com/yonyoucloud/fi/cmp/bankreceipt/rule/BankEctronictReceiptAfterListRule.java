package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyou.iuap.context.YmsContextWrappers;
import com.yonyou.iuap.ruleengine.dto.others.T;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ypd.bizflow.utils.ObjectUtil;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.EnvConstant;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_EIGHT;

/**
 * <h1>外币折算 - 银行交易回单等5个单据列表</h1>
 *
 * @author lidwt
 * @version 1.0
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component("bankForeignCurrencyTranslationAfterListRule")
@Slf4j
public class BankEctronictReceiptAfterListRule extends AbstractCommonRule {

    private static final Logger logger = LoggerFactory.getLogger(BankEctronictReceiptAfterListRule.class);
    private final CmCommonService cmCommonService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    private static ObjectMapper objectMapper;
    private static final String BankElectronicReceipt = "cmp_bankelectronicreceiptlist";
    private static final String BANKRECONCILIATIONLIST = "cmp_bankreconciliationlist";
    private static final String BANKDEALListe = "cmp_dllist";
    private static final String CMPRETIBAL = "cmp_retibalist";
    private static final String CMPHISBAL = "cmp_hisbalist";

    private ExecutorService executorService = YmsContextWrappers.wrap(Executors.newFixedThreadPool(10));

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if (billContext.getbMain() != null && !billContext.getbMain()) {
            return new RuleExecuteResult();
        }
        Pager data = (Pager) paramMap.get("return");
        logger.info("查询列表数据={}", data);
        List<Map<String, Object>> recordList = data.getRecordList();
        if (CollectionUtils.isEmpty(recordList)) {
            return new RuleExecuteResult();
        }
        String billno = billContext.getBillnum();
        if (BANKDEALListe.equals(billno)) {
            List<Object> ids = recordList.stream().map(v -> v.get("id")).collect(Collectors.toList());
            setMerchantFlag(ids);
        }
        //折算币种
        String targetCurrencyId = null;
        //汇率类型
        String exchangeRateType = null;
        BillDataDto billDataDto = (BillDataDto) this.getParam(paramMap);
        if (null != billContext && billDataDto != null) {
            targetCurrencyId = billDataDto.getParameter("natCurrencyId");
            exchangeRateType = billDataDto.getParameter("exchangeRateType");
            if (ObjectUtil.isAnyEmpty(new String[]{targetCurrencyId, exchangeRateType})) {
                // 获取筛选区入参
                Map<String,Object> billDataMap = (Map<String,Object>)billDataDto.getExternalData();
                if (billDataMap == null) {
                    logger.error("bankForeignCurrencyTranslationAfterListRule  billDataMap为空，直接返回");
                    return new RuleExecuteResult();
                }
                // 折算币种
                targetCurrencyId = billDataMap.get("natCurrencyId") == null ? "" : billDataMap.get("natCurrencyId").toString();
                // 汇率类型
                exchangeRateType = billDataMap.get("exchangeRateType") == null ? "" : billDataMap.get("exchangeRateType").toString();
                if (ObjectUtil.isAnyEmpty(new String[]{targetCurrencyId, exchangeRateType})) {
                    logger.error("bankForeignCurrencyTranslationAfterListRule natCurrencyId,exchangeRateType 为空，直接返回");
                    return new RuleExecuteResult();
                }
            }
        }
        Map<String, BigDecimal> exchangerateMap = new ConcurrentHashMap<>(CONSTANT_EIGHT);
        long timeStart = System.currentTimeMillis();
        logger.info("---------bankEctronictReceiptAfterListRule start--------" + timeStart);
        // 币种精度
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(targetCurrencyId);
        if (currencyTenantDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100950"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418061E", "当前币种不存在") /* "当前币种不存在" */);
        }

        Integer moneydigit = currencyTenantDTO.getMoneydigit();
        Integer moneyrount = currencyTenantDTO.getMoneyrount();
        // 设置汇率类型为基准汇率
        ExchangeRateTypeVO exchangeRateTypeVO = cmCommonService.getExchangeRateType(exchangeRateType);
        /*
        if (!BANKRECONCILIATIONLIST.equals(billno)) {
            for (Map<String, Object> tmpRecord : recordList) {
                Date billdate = getBillDate(tmpRecord, billno);
                setRound(tmpRecord, exchangeRateTypeVO, currencyTenantDTO);
                BigDecimal exchangerate = queryExchangeRate(tmpRecord, billdate, targetCurrencyId, exchangeRateTypeVO.getId(), exchangerateMap);
                if (null != exchangerate) {
                    calculateObversionData(tmpRecord, exchangerate, moneydigit, billno, moneyrount);
                }
            }
        }
         */
        ////////////多线程处理模式/////////////////
        logger.error("queryExchangeRateSync 寻汇率 start:"+ (System.currentTimeMillis() - timeStart));
        List<CompletableFuture> completableFutureList = new ArrayList<>();
        for (Map<String, Object> tmpRecord : recordList) {
            CompletableFuture r = setObversionDataSync(tmpRecord, billno, targetCurrencyId,currencyTenantDTO, exchangerateMap, exchangeRateTypeVO, moneydigit, moneyrount);
            completableFutureList.add((CompletableFuture) r);
        }
        CompletableFuture.allOf((CompletableFuture[]) completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).join();
        logger.error("queryExchangeRateSync 寻汇率 end:"+ (System.currentTimeMillis() - timeStart));
        ////////////////////////////////////////

        data.setRecordList(recordList);
        paramMap.put("return", data);
        logger.info("---------bankForeignCurrencyTranslationAfterListRule end--------" + (System.currentTimeMillis() - timeStart));
        return new RuleExecuteResult();
    }

    private static void setMerchantFlag(List<Object> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        if(EnvConstant.syncMerchant){
            try {
                // 修改到查询后，只更新查到的数据
                // 银行交易明细记录初始化  判断是否需要同步客商
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                querySchema.addCondition(QueryConditionGroup.and(
                        QueryCondition.name("id").in(ids),
                        QueryCondition.name("to_acct_name").is_not_null(),
                        QueryCondition.name("to_acct_no").is_not_null(),
                        QueryCondition.name("merchant_flag").is_null()));
                List<Map<String, Object>> bankDealDetailList = MetaDaoHelper.query(BankDealDetail.ENTITY_NAME,querySchema);
                ArrayList<BankDealDetail> updateBankDealDetail = new ArrayList<>();
                if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(bankDealDetailList)) {
                    for (int i = 0;i < bankDealDetailList.size(); i++) {
                        BankDealDetail bankDealDetail = new BankDealDetail();
                        bankDealDetail.init(bankDealDetailList.get(i));
                        boolean dealMerchantFlag = MerchantUtils.dealMerchantFlag(bankDealDetail);
                        if (dealMerchantFlag) {
                            EntityTool.setUpdateStatus(bankDealDetail);
                            updateBankDealDetail.add(bankDealDetail);
                        }
                    }
                }
                if(!updateBankDealDetail.isEmpty()){
                    MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, updateBankDealDetail);
                }
            } catch (Exception e) {
                log.error("MerchantService--init--exception:", e);
            }
        }
    }

    private void setRound(Map<String, Object> map, ExchangeRateTypeVO exchangeRateTypeVO, CurrencyTenantDTO currencyTenantDTO) {
        //汇率类型精度
        if (ObjectUtils.isNotEmpty(exchangeRateTypeVO)) {
            map.put("currencyexchangeratetype", exchangeRateTypeVO.getId());
            map.put("currencyexchangeratetype_name", exchangeRateTypeVO.getName());
            map.put("currencyexchangeratetype_digit", exchangeRateTypeVO.getDigit());
            map.put("currencyexchangeratetype_code", exchangeRateTypeVO.getCode());
        }
        //折算金额精度
        map.put("natCurrency_name", currencyTenantDTO.getName());
        map.put("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
        map.put("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
        map.put("natCurrencyId", currencyTenantDTO.getId());
    }

    //异步查找汇率
    private CompletableFuture setObversionDataSync(Map<String, Object> map, String billno,  String targetCurrencyId, CurrencyTenantDTO currencyTenantDTO,Map<String, BigDecimal> exchangerateMap, ExchangeRateTypeVO exchangeRateTypeVO, Integer moneydigit, Integer moneyrount) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync((() -> {
            Date billdate = getBillDate(map, billno);
            setRound(map, exchangeRateTypeVO, currencyTenantDTO);
            try {
                BigDecimal exchangerate = queryExchangeRate(map, billdate, targetCurrencyId, exchangeRateTypeVO.getId(), exchangerateMap);
                if (null != exchangerate) {
                    calculateObversionData(map, exchangerate, moneydigit, billno, moneyrount);
                }
            } catch (Exception e) {
                logger.error("queryExchangeRateSync 寻汇率 fail:");
                return null;
            }
            return 1;
        }), executorService).handle((r, e) -> {
            if (e != null) {
                logger.error("queryExchangeRateSync 寻汇率 fail:");
                logger.error(e.getMessage());
            }
            return null;
        });
        return future;
    }

    //查找汇率
    private BigDecimal queryExchangeRate(Map<String, Object> map, Date billdate, String targetCurrencyId, String exchangeRateType, Map<String, BigDecimal> exchangerateMap) throws Exception {
        String key = targetCurrencyId +
                "_" +
                exchangeRateType +
                "_" +
                map.get("currency") +
                "_" +
                billdate;
        if (ValueUtils.isNotEmptyObj(exchangerateMap.get(key))) {
            return exchangerateMap.get(key);
        }
        //汇率
        BigDecimal exchangerate = null;
        if (map.containsKey("currency") && map.get("currency") != null && StringUtils.isNotBlank(String.valueOf(map.get("currency")))) {
            //原币种是行上币种
            String natCurrencyId = (String) map.get("currency");
            //如果本币币种与原币币种相同，则汇率为1
            if (!natCurrencyId.equals(targetCurrencyId)) {
                //Date billdate = (Date)map.get("tranDate");
                ExchangeRate exchangeRate = baseRefRpcService.queryRateByExchangeType(natCurrencyId, targetCurrencyId, billdate, exchangeRateType);
                if (exchangeRate != null) {
                    exchangerate = BigDecimal.valueOf(exchangeRate.getExchangerate());
                }
            } else {
                exchangerate = new BigDecimal(1);
            }
        }
        if (null != exchangerate) {
            exchangerateMap.put(key, exchangerate);
        }
        return exchangerate;
    }

    //获取行上日期
    private Date getBillDate(Map<String, Object> map, String billno) {
        Date billdate = null;
        switch (billno) {
            //银行交易回单
            case BankElectronicReceipt:
                //银行交易流水
            case BANKDEALListe:
                if (map.get("tranDate") instanceof Date) {
                    return (Date) map.get("tranDate");
                }
                try {
                    billdate = DateUtils.parseDate(map.get("tranDate").toString());
                } catch (Exception e) {
                    logger.error("日期转换错误！");
                }
                break;
            //银行流水认领
            case BANKRECONCILIATIONLIST:
                if (map.get("tran_date") instanceof Date) {
                    return (Date) map.get("tran_date");
                }
                try {
                    billdate = DateUtils.parseDate(map.get("tran_date").toString()) ;
                } catch (Exception e) {
                    logger.error("日期转换错误！");
                }
                break;
            //账户当日余额
            case CMPRETIBAL:
                // 创建Calendar实例
                //Calendar calendar = Calendar.getInstance();
                // 获取当前时间
                Date date = new Date();
                billdate = date;
                break;
            //账户历史余额
            case CMPHISBAL:
                //balancedate 余额日期
                if (map.get("balancedate") instanceof Date) {
                    return (Date) map.get("balancedate");
                }
                try {
                    billdate =  DateUtils.parseDate(map.get("balancedate").toString());
                } catch (Exception e) {
                    logger.error("日期转换错误！");
                }
                break;
            default:
                break;
        }
        return billdate;
    }


    //计算折算数据
    private void calculateObversionData(Map<String, Object> map, BigDecimal exchangerate, Integer moneyDigit, String billno, Integer moneyRound) {
        //折算汇率
        map.put("obversion_exchangerate", exchangerate);
        switch (billno) {
            //银行交易回单
            case BankElectronicReceipt:
                //折算交易金额
                if (map.containsKey("tran_amt") && map.get("tran_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("tran_amt")))) {
                    map.put("obversion_tran_amt", calculate((BigDecimal) map.get("tran_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算利息
                if (map.containsKey("interest") && map.get("interest") != null && StringUtils.isNotBlank(String.valueOf(map.get("interest")))) {
                    map.put("obversion_interest", calculate((BigDecimal) map.get("interest"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            //银行流水认领
            case BANKRECONCILIATIONLIST:
                //折算金额
                if (map.containsKey("tran_amt") && map.get("tran_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("tran_amt")))) {
                    map.put("obversion_tran_amt", calculate((BigDecimal) map.get("tran_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算余额
                if (map.containsKey("acct_bal") && map.get("acct_bal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acct_bal")))) {
                    map.put("obversion_acct_bal", calculate((BigDecimal) map.get("acct_bal"), exchangerate, moneyDigit, moneyRound));
                }
                //借方是支出，贷方是收入
                //折算借方金额
                if (map.containsKey("debitamount") && map.get("debitamount") != null && StringUtils.isNotBlank(String.valueOf(map.get("debitamount")))) {
                    map.put("obversion_debitamount", calculate((BigDecimal) map.get("debitamount"), exchangerate, moneyDigit, moneyRound));
                }
                //折算贷方金额
                if (map.containsKey("creditamount") && map.get("creditamount") != null && StringUtils.isNotBlank(String.valueOf(map.get("creditamount")))) {
                    map.put("obversion_creditamount", calculate((BigDecimal) map.get("creditamount"), exchangerate, moneyDigit, moneyRound));
                }
                //折算认领金额
                if (map.containsKey("claimamount") && map.get("claimamount") != null && StringUtils.isNotBlank(String.valueOf(map.get("claimamount")))) {
                    map.put("obversion_claimamount", calculate((BigDecimal) map.get("claimamount"), exchangerate, moneyDigit, moneyRound));
                }
                //折算待认领金额
                if (map.containsKey("amounttobeclaimed") && map.get("amounttobeclaimed") != null && StringUtils.isNotBlank(String.valueOf(map.get("amounttobeclaimed")))) {
                    map.put("obversion_amounttobeclaimed", calculate((BigDecimal) map.get("amounttobeclaimed"), exchangerate, moneyDigit, moneyRound));
                }
                //折算利息
                if (map.containsKey("interest") && map.get("interest") != null && StringUtils.isNotBlank(String.valueOf(map.get("interest")))) {
                    map.put("obversion_interest", calculate((BigDecimal) map.get("interest"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            //银行交易流水
            case BANKDEALListe:
                //折算交易金额
                if (map.containsKey("tran_amt") && map.get("tran_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("tran_amt")))) {
                    map.put("obversion_tran_amt", calculate((BigDecimal) map.get("tran_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算余额
                if (map.containsKey("acctbal") && map.get("acctbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acctbal")))) {
                    map.put("obversion_acctbal", calculate((BigDecimal) map.get("acctbal"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            //账户当日余额
            case CMPRETIBAL:

                //折算合计余额
                if (map.containsKey("total_amt") && map.get("total_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("total_amt")))) {
                    map.put("obversion_total_amt", calculate((BigDecimal) map.get("total_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算账户余额
                if (map.containsKey("acctbal") && map.get("acctbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acctbal")))) {
                    map.put("obversion_acctbal", calculate((BigDecimal) map.get("acctbal"), exchangerate, moneyDigit, moneyRound));
                }
                //折算可用余额
                if (map.containsKey("avlbal") && map.get("avlbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("avlbal")))) {
                    map.put("obversion_avlbal", calculate((BigDecimal) map.get("avlbal"), exchangerate, moneyDigit, moneyRound));
                }
                //折算冻结金额
                if (map.containsKey("frzbal") && map.get("frzbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("frzbal")))) {
                    map.put("obversion_frzbal", calculate((BigDecimal) map.get("frzbal"), exchangerate, moneyDigit, moneyRound));
                }
                //折算定期余额
                if (map.containsKey("regular_amt") && map.get("regular_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("regular_amt")))) {
                    map.put("obversion_regular_amt", calculate((BigDecimal) map.get("regular_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算透支余额
                if (map.containsKey("overdraftbalance") && map.get("overdraftbalance") != null && StringUtils.isNotBlank(String.valueOf(map.get("overdraftbalance")))) {
                    map.put("obversion_overdraftbalance", calculate((BigDecimal) map.get("overdraftbalance"), exchangerate, moneyDigit, moneyRound));
                }
                //折算昨日金额
                if (map.containsKey("yesterbal") && map.get("yesterbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("yesterbal")))) {
                    map.put("obversion_yesterbal", calculate((BigDecimal) map.get("yesterbal"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            //账户历史余额
            case CMPHISBAL:
                //折算账户余额
                if (map.containsKey("acctbal") && map.get("acctbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("acctbal")))) {
                    map.put("obversion_acctbal", calculate((BigDecimal) map.get("acctbal"), exchangerate, moneyDigit, moneyRound));
                }
                //折算可用余额
                if (map.containsKey("avlbal") && map.get("avlbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("avlbal")))) {
                    map.put("obversion_avlbal", calculate((BigDecimal) map.get("avlbal"), exchangerate, moneyDigit, moneyRound));
                }
                //折算冻结金额
                if (map.containsKey("frzbal") && map.get("frzbal") != null && StringUtils.isNotBlank(String.valueOf(map.get("frzbal")))) {
                    map.put("obversion_frzbal", calculate((BigDecimal) map.get("frzbal"), exchangerate, moneyDigit, moneyRound));
                }
                //折算定期余额
                if (map.containsKey("regular_amt") && map.get("regular_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("regular_amt")))) {
                    map.put("obversion_regular_amt", calculate((BigDecimal) map.get("regular_amt"), exchangerate, moneyDigit, moneyRound));
                }
                //折算透支余额
                if (map.containsKey("overdraftbalance") && map.get("overdraftbalance") != null && StringUtils.isNotBlank(String.valueOf(map.get("overdraftbalance")))) {
                    map.put("obversion_overdraftbalance", calculate((BigDecimal) map.get("overdraftbalance"), exchangerate, moneyDigit, moneyRound));
                }
                //折算合计余额
                if (map.containsKey("total_amt") && map.get("total_amt") != null && StringUtils.isNotBlank(String.valueOf(map.get("total_amt")))) {
                    map.put("obversion_total_amt", calculate((BigDecimal) map.get("total_amt"), exchangerate, moneyDigit, moneyRound));
                }
                break;
            default:
                break;
        }

    }

    //计算折算数据
    private BigDecimal calculate(BigDecimal amount, BigDecimal exchangerate, Integer moneyDigit, Integer moneyRound) {
        BigDecimal natsum = exchangerate.multiply(amount);
        return new BigDecimal(natsum.toString()).setScale(moneyDigit, moneyRound);
    }

}
