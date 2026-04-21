package com.yonyoucloud.fi.cmp.bankvouchercheck.rule;

import com.yonyou.diwork.common.util.BeanPropertyCopyUtil;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceAdjustService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankvouchercheck.service.BankVoucherCheckService;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoQueryVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountReconciliationInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.bankvourchercheck.BankvourchercheckWorkbench;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.reconciliation.ReconciliationMatchRecord;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @description:银企对账工作台查询后规则，用来封装前端展示所需要数据
 * v1:20250331 凭证与银行流水对账概览数据逻辑
 * v2:20250513 新增银行日记账与银行流水对账概览数据逻辑
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/13 14:08
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class BankVoucherCheckAfterQueryRule extends AbstractCommonRule {

    //线程池
    static ExecutorService executorService = null;
    static ExecutorService pageExecutorService = null;
    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(50, 200,200,"cmp-bankvouchercheck-infoqueyr-async-");
        pageExecutorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(50, 200,200,"cmp-bankvouchercheck-pageinfoqueyr-async-");
    }

    @Autowired
    private BankVoucherCheckService bankVoucherCheckService;
    @Autowired
    private BalanceAdjustService balanceAdjustService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);
        String billnum = billContext.getBillnum();
        FilterVO filterVO;
        if(billDataDto.getCondition() != null) {
            //获取返回页面的page
            Pager pager = (Pager) paramMap.get(ICmpConstant.RETURN);
            //凭证对账。不为空则是统计的list,不走下边的查询
            if ("cmp_bankvourhcercheck_infolist".equals(billnum) && pager.getSumRecordList()  != null) {
                return ruleResult;
            }
            //银行日记账分页数据查询标识
            boolean isPageQuery = false;
            if ("cmp_bankjournalcheck_infolist".equals(billnum) && pager.getSumRecordList()  != null) {
                isPageQuery = true;
            }
            //默认凭证数据源；20250513新增银行日记账数据源逻辑
            short reconciliationDataSource = ReconciliationDataSource.Voucher.getValue();
            if ("cmp_bankjournalcheck_infolist".equals(billnum)){
                reconciliationDataSource = ReconciliationDataSource.BankJournal.getValue();
            }

            filterVO = billDataDto.getCondition();
            //初始化银企对账工作台查询入参
            BankAccountInfoQueryVO infoQueryVO = initInfoQueryVO(filterVO,reconciliationDataSource);

            //查询银企对账设置中的 账户+币种+对账组织等信息
            List<BankAccountInfoVO> bankAccountInfoVOList = bankVoucherCheckService.queryBankAccountInfo(infoQueryVO);

            //统计账户+币种+对账组织维度下的对账概览数据
            List<BankAccountReconciliationInfoVO> reconciliationInfoVOList = Collections.synchronizedList(new ArrayList<>());

            // 根据 isPageQuery 选择线程池
            ExecutorService selectedExecutorService = isPageQuery ? pageExecutorService : executorService;
//            int batchSize = isPageQuery ? 5 : 1 ;
            // 建议动态调整批次大小
            int batchSize = bankAccountInfoVOList.size() < 50 ? 1 : Math.min(10, bankAccountInfoVOList.size() / 50);

            //大数量查询，线程池，分批次查询
            short finalReconciliationDataSource = reconciliationDataSource;
            boolean finalIsPageQuery = isPageQuery;
            ThreadPoolUtil.executeByBatch(selectedExecutorService, bankAccountInfoVOList, batchSize, "银企对账工作台-对账概览数据查询", (int fromIndex, int toIndex) -> {//@notranslate
                String builder = "";
                for (int t = fromIndex; t < toIndex; t++) {
                    BankAccountInfoVO bankAccountInfoVO = bankAccountInfoVOList.get(t);
                    BankVoucherInfoQueryVO bankVoucherInfoQueryVO = initBillVoucherInfoQueryVO(bankAccountInfoVO, infoQueryVO);
                    bankVoucherInfoQueryVO.setPageQuery(finalIsPageQuery);
                    //查询关联的凭证数据
                    List<Journal> voucherList = new ArrayList<>();
                    if (finalReconciliationDataSource == ReconciliationDataSource.Voucher.getValue()){
                        voucherList = bankVoucherCheckService.getVoucherByBankAccountInfo(bankVoucherInfoQueryVO);
                    }
                    if (finalReconciliationDataSource == ReconciliationDataSource.BankJournal.getValue()){
                        voucherList = bankVoucherCheckService.getJournalByBankAccountInfo(bankVoucherInfoQueryVO);
                    }
                    //查询关联的银行流水数据
                    List<BankReconciliation> bankReconciliationList = bankVoucherCheckService.getBankReconciliationByBankAccountInfo(bankVoucherInfoQueryVO);
                    //统计具体银行账户下的对账概览信息
                    BankAccountReconciliationInfoVO reconciliationInfoVO = statisticReconciliationInfo(voucherList,bankReconciliationList,bankVoucherInfoQueryVO);
                    bankReconciliationList.clear();
                    voucherList.clear();
                    reconciliationInfoVOList.add(reconciliationInfoVO);
                }
                return builder;
            }, false);

            //排序规则： 对账组织（组织编码）正序+银行账户（银行账号）正序的维度
            Collections.sort(reconciliationInfoVOList, new Comparator<BankAccountReconciliationInfoVO>() {
                @Override
                public int compare(BankAccountReconciliationInfoVO o1, BankAccountReconciliationInfoVO o2) {
                    int result = o1.getAccentity_code().compareTo(o2.getAccentity_code());
                    if (result == 0) {
                        // 处理银行账号可能为null的情况
                        String account1 = o1.getBankaccount_account();
                        String account2 = o2.getBankaccount_account();
                        // 两个都为null则相等
                        if (account1 == null && account2 == null) {
                            return 0;
                        }
                        // account1为null则排到后面
                        if (account1 == null) {
                            return 1;
                        }
                        // account2为null则排到前面
                        if (account2 == null) {
                            return -1;
                        }
                        // 都不为null则正常比较
                        return account1.compareTo(account2);
                    }
                    return result;
                }
            });
            //利用reconciliationInfoVOList组装前端展示数据
            List<BankvourchercheckWorkbench> recordList = initReconciliationInfoList(reconciliationInfoVOList,infoQueryVO,filterVO);

            if (pager.getSumRecordList()  == null) {
                //说明只包含统计数据
                if (recordList.size() == 1 && recordList.get(0).get("accentity") == null){
                    pager.setSumRecordList(recordList.get(0).get("tagSum"));
                }else { //其他情况可以直接返回
                    //银行日记账是假分页，需要将全部数据放回到返回的数据中，用来后续自动对账和余额调节表生成
                    if (infoQueryVO.getReconciliationDataSource() == ReconciliationDataSource.BankJournal.getValue()) {
                        int pageIndex = pager.getPageIndex(); // 获取当前页码
                        int pageSize = pager.getPageSize();   // 获取每页大小
                        int startIndex = (pageIndex - 1) * pageSize; // 计算起始索引
                        int endIndex = Math.min(startIndex + pageSize, recordList.size()); // 计算结束索引
                        // 截取 recordList 中的分页数据
                        List<BankvourchercheckWorkbench> pagedRecordList = recordList.subList(startIndex, endIndex);
                        pagedRecordList.get(0).put("allDataInfo", CtmJSONObject.toJSONString(recordList));
                        // 将分页数据设置到 pager 中
                        pager.setRecordList(pagedRecordList);
                    }else {
                        pager.setRecordList(recordList);
                    }
                    pager.setSumRecordList(recordList.get(0).get("tagSum"));
                    pager.setRecordCount(recordList.size());
                }
                ruleResult.setData(pager);
                // 后面的规则都不执行
                ruleResult.setCancel(true);
            }else {  //不为空则是统计的list
                //分页信息
                pager.setRecordCount(recordList.size());
            }
        }

        return ruleResult;
    }

    /**
     * 根据前端过滤区参数，初始化银企对账工作台查询入参
     * @param filterVO 过滤区参数
     * @return 查询入参
     */
    private BankAccountInfoQueryVO initInfoQueryVO(FilterVO filterVO,short reconciliationDataSource) throws Exception{
        BankAccountInfoQueryVO infoQueryVO = new BankAccountInfoQueryVO();
        infoQueryVO.setReconciliationDataSource(reconciliationDataSource);
        FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
        //对账截止日期
        FilterCommonVO checkEndDateFilter = Arrays.stream(commonVOs)
                .filter(item -> "check_end_date".equals(item.getItemName())).findFirst().orElse(null);
        if(checkEndDateFilter == null){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFA3A05980010", "对账截止日期必填，请检查！") /* "对账截止日期必填，请检查！" */);
        }
        infoQueryVO.setCheckEndDate(dateFormatWithoutTime(checkEndDateFilter.getValue1().toString()));
        //对账组织
        FilterCommonVO accentityFilter = Arrays.stream(commonVOs)
                .filter(item -> "accentity".equals(item.getItemName())).findFirst().orElse(null);

        //对账组织赋值
        List<String> accentitys = new ArrayList<>();
        //单组织逻辑
        if(FIDubboUtils.isSingleOrg()){
            String accentity = null;
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if(singleOrg!=null){
                accentity = singleOrg.get("id");
            }
            if(StringUtil.isNotEmpty(accentity)){
                accentitys.add(accentity);
            }
        }
        if (accentityFilter != null) {
            accentitys = getValueList(accentityFilter);
        }else {
            accentitys = bankVoucherCheckService.getReconciliationSchemeAccentityList(reconciliationDataSource);
        }
        if (accentitys.size() == 0) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFA3A0598000F", "未输入有效的对账组织数据，请检查！") /* "未输入有效的对账组织数据，请检查！" */);
        }
        infoQueryVO.setAccentityList(accentitys);

        //银行账户
        FilterCommonVO bankaccountFilter = Arrays.stream(commonVOs)
                .filter(item -> "bankaccount".equals(item.getItemName())).findFirst().orElse(null);
        if (bankaccountFilter != null) {
            infoQueryVO.setBankAccountList(getValueList(bankaccountFilter));
        }
        //银行类别
        FilterCommonVO banktypeFilter = Arrays.stream(commonVOs)
                .filter(item -> "banktype".equals(item.getItemName())).findFirst().orElse(null);
        if (banktypeFilter != null) {
            infoQueryVO.setBanktypeList(getValueList(banktypeFilter));
        }
        //币种
        FilterCommonVO currencyFilter = Arrays.stream(commonVOs)
                .filter(item -> "currency".equals(item.getItemName())).findFirst().orElse(null);
        if (currencyFilter != null) {
            infoQueryVO.setCurrencyList(getValueList(currencyFilter));
        }
        //对账方案
        FilterCommonVO reconciliationSchemeFilter = Arrays.stream(commonVOs)
                .filter(item -> "reconciliation_scheme".equals(item.getItemName())).findFirst().orElse(null);
        if (reconciliationSchemeFilter != null) {
            infoQueryVO.setReconciliationSchemeList(getValueList(reconciliationSchemeFilter));
        }
        //业务日期结束日期
        FilterCommonVO businessDateFilter = Arrays.stream(commonVOs)
                .filter(item -> "business_date".equals(item.getItemName())).findFirst().orElse(null);
        if (businessDateFilter != null && businessDateFilter.getValue1() != null) {
            infoQueryVO.setBusinessStartDate(dateFormatWithoutTime(businessDateFilter.getValue1().toString()));
        }
        //交易日期结束日期
        FilterCommonVO tranDateFilter = Arrays.stream(commonVOs)
                .filter(item -> "tran_date".equals(item.getItemName())).findFirst().orElse(null);
        if (tranDateFilter != null && tranDateFilter.getValue1() != null) {
            infoQueryVO.setTranStartDate(dateFormatWithoutTime(tranDateFilter.getValue1().toString()));
        }

        return infoQueryVO;
    }

    /**
     * 日期字符串转换为不带时分秒的格式
     * @param dateStr
     * @return 格式为yyyy-MM-dd的日期
     * @throws Exception
     */
    private String dateFormatWithoutTime(String dateStr) throws Exception {
        SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date withoutTimeDate = null;
        try {
            withoutTimeDate =  dateFormatWithTime.parse(dateStr);
        } catch (Exception e) {
            // 忽略异常，尝试使用包含时分秒的格式解析
        }
        if (withoutTimeDate == null){
            try {
                withoutTimeDate =  dateFormatWithoutTime.parse(dateStr);
            } catch (Exception e) {
                // 忽略异常，尝试使用包含时分秒的格式解析
            }
        }
        if (withoutTimeDate ==null){
            withoutTimeDate = new Date();
        }
        return DateUtils.dateFormat(withoutTimeDate,"yyyy-MM-dd");
    }

    /**
     * 初始化凭证和银行流水查询入参
     * @param bankAccountInfoVO 银行账户币种对账组织信息
     * @param infoQueryVO 前端页面查询过滤区参数
     * @return 凭证和银行流水查询入参
     */
    private BankVoucherInfoQueryVO initBillVoucherInfoQueryVO(BankAccountInfoVO bankAccountInfoVO,BankAccountInfoQueryVO infoQueryVO){
        BankVoucherInfoQueryVO bankVoucherInfoQueryVO = new BankVoucherInfoQueryVO();
        BeanPropertyCopyUtil.copyProperties(bankAccountInfoVO,bankVoucherInfoQueryVO);
        BeanPropertyCopyUtil.copyProperties(infoQueryVO,bankVoucherInfoQueryVO);
        return bankVoucherInfoQueryVO;
    }


    /**
     * 统计对账概率数据
     * @param voucherList 凭证数据
     * @param bankReconciliationList 银行流水数据
     * @return
     */
    private BankAccountReconciliationInfoVO statisticReconciliationInfo(List<Journal> voucherList,List<BankReconciliation> bankReconciliationList,BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        BankAccountReconciliationInfoVO reconciliationInfo = new BankAccountReconciliationInfoVO();
        BeanPropertyCopyUtil.copyProperties(bankVoucherInfoQueryVO,reconciliationInfo);
        //存在未勾对数据，则未对符
        if (voucherList.size() >0 || bankReconciliationList.size() >0){
            //20260130增加未对账状态，在对应日期范围内，有未达项，但是没有勾对记录的，标记为未对账；有勾对记录标记为未对符
            List<ReconciliationMatchRecord> reconciliationMatchRecordList = bankVoucherCheckService.queryReconciliationMatchRecord(bankVoucherInfoQueryVO);
            if (reconciliationMatchRecordList !=null && reconciliationMatchRecordList.size() >0){
                reconciliationInfo.setReconciliationStatus(ReconciliationStatus.unchecked.getValue());
            }else {
                reconciliationInfo.setReconciliationStatus(ReconciliationStatus.Unreconciled.getValue());
            }
        }else {
            reconciliationInfo.setReconciliationStatus(ReconciliationStatus.checked.getValue());
        }
        if (!bankVoucherInfoQueryVO.isPageQuery()) {
            BalanceAdjustResult balanceAdjustResult = bankVoucherCheckService.getBalanceStatus(bankVoucherInfoQueryVO);
            // 余额调节表状态判断
            if(balanceAdjustResult != null ){
                reconciliationInfo.setBalanceAdjustResult(balanceAdjustResult);
                reconciliationInfo.setBalanceStatus(BalanceStatus.balance_closed.getValue());
            }else {
                reconciliationInfo.setBalanceStatus(BalanceStatus.not_generate.getValue());
            }
        }

        //凭证数据统计
        reconciliationInfo.setVoucherUncheckTotalNum(voucherList.size());
        //未勾对凭证借方金额合计
        BigDecimal voucherUncheckDebitAmountSum = BigDecimal.ZERO;
        //未勾对凭证贷方金额合计
        BigDecimal voucherUncheckCreditAmountSum = BigDecimal.ZERO;
        for (Journal voucher : voucherList){
            if (voucher.getDirection().equals(Direction.Debit)){ //借
                voucherUncheckDebitAmountSum = BigDecimalUtils.safeAdd(voucherUncheckDebitAmountSum,voucher.getDebitoriSum());
            } else {
                voucherUncheckCreditAmountSum = BigDecimalUtils.safeAdd(voucherUncheckCreditAmountSum,voucher.getCreditoriSum());
            }
        }
        reconciliationInfo.setVoucherUncheckDebitAmountSum(voucherUncheckDebitAmountSum);
        reconciliationInfo.setVoucherUncheckCreditAmountSum(voucherUncheckCreditAmountSum);

        //银行流水数据统计
        reconciliationInfo.setBankUncheckTotalNum(bankReconciliationList.size());
        //未勾对银行流水借方金额合计
        BigDecimal bankUncheckDebitAmountSum = BigDecimal.ZERO;
        //未勾对银行流水贷方金额合计
        BigDecimal bankUncheckCreditAmountSum = BigDecimal.ZERO;
        for (BankReconciliation bankReconciliation : bankReconciliationList){
            if (bankReconciliation.getDc_flag().equals(Direction.Debit)){ //借
                bankUncheckDebitAmountSum = BigDecimalUtils.safeAdd(bankUncheckDebitAmountSum,bankReconciliation.getDebitamount());
            } else {
                bankUncheckCreditAmountSum = BigDecimalUtils.safeAdd(bankUncheckCreditAmountSum,bankReconciliation.getCreditamount());
            }
        }
        reconciliationInfo.setBankUncheckDebitAmountSum(bankUncheckDebitAmountSum);
        reconciliationInfo.setBankUncheckCreditAmountSum(bankUncheckCreditAmountSum);

        //银行日记账对账增加银行账户余额和企业日记账余额查询逻辑
        try {
            if (!bankVoucherInfoQueryVO.isPageQuery() && ReconciliationDataSource.BankJournal.getValue() == bankVoucherInfoQueryVO.getReconciliationDataSource()) {
                CtmJSONObject bankBalanceAmount = balanceAdjustService.getBankBalanceAmount(bankVoucherInfoQueryVO);
                if (!bankBalanceAmount.getBoolean("isEmptyBalance")){
                    reconciliationInfo.setBankBalanceAmoount(bankBalanceAmount.getBigDecimal("bankye"));
                }
                reconciliationInfo.setJournalBalanceAmoount(balanceAdjustService.getJournalBalanceAmount(bankVoucherInfoQueryVO).getBigDecimal("journalye"));
            }
        }catch (Exception e){
            log.error("银行账户余额和企业账户余额统计异常。");
        }

        return reconciliationInfo;
    }

    /**
     * 统计对账概率展示数据,并组合成前端返回的样式
     * @param reconciliationInfoVOList 对账概率后台统计数据
     * @param filterVO 过滤区数据
     * @param infoQueryVO 查询信息组装
     * @return
     * @throws Exception
     */
    private List<BankvourchercheckWorkbench> initReconciliationInfoList( List<BankAccountReconciliationInfoVO> reconciliationInfoVOList,BankAccountInfoQueryVO infoQueryVO ,FilterVO filterVO) throws Exception{
        List<BankvourchercheckWorkbench> workbenchList  = new ArrayList<>();
        //查询条件,对账状态 0未对符；1已对符；
        SimpleFilterVO[] simpleFilterVOS = filterVO.getSimpleVOs();
        SimpleFilterVO isAllFilterVO = Arrays.stream(simpleFilterVOS)
                .filter(item -> item.getConditions() != null).findFirst().orElse(null);
        SimpleFilterVO reconciliationStatusFilterVO = Arrays.stream(simpleFilterVOS)
                .filter(item -> "reconciliation_status".equals(item.getField())).findFirst().orElse(null);
        if (reconciliationStatusFilterVO == null && isAllFilterVO == null){
            return workbenchList;
        }
        //对账状态 0未对符；1已对符；2全部
        Short reconciliationStatus = isAllFilterVO !=null ? 2 : Short.parseShort(reconciliationStatusFilterVO.getValue1().toString());
        Integer uncheckedNum = 0;
        Integer checkedNum = 0;
        for (BankAccountReconciliationInfoVO reconciliationInfoVO : reconciliationInfoVOList){
            BankvourchercheckWorkbench workbench = new BankvourchercheckWorkbench();
            workbench.setBankaccount(reconciliationInfoVO.getBankaccount());
            workbench.put("bankaccount_name",reconciliationInfoVO.getBankaccount_name());
            workbench.put("bankaccount_account",reconciliationInfoVO.getBankaccount_account());
            workbench.setBanktype(reconciliationInfoVO.getBanktype());
            workbench.put("banktype_name",reconciliationInfoVO.getBanktype_name());
            workbench.setCurrency(reconciliationInfoVO.getCurrency());
            workbench.put("currency_name",reconciliationInfoVO.getCurrency_name());
            workbench.put("currency_moneyDigit",reconciliationInfoVO.getCurrency_moneyDigit());
            workbench.setAccentity(reconciliationInfoVO.getAccentity());
            workbench.put("accentity_name",reconciliationInfoVO.getAccentity_name());
            workbench.put("accentity_code",reconciliationInfoVO.getAccentity_code());
//            workbench.put("reconciliationScheme",reconciliationInfoVO.getReconciliationScheme() != null ? Long.parseLong(reconciliationInfoVO.getReconciliationScheme()) : null);
            workbench.put("reconciliationScheme",reconciliationInfoVO.getReconciliationScheme());
            workbench.put("reconciliationScheme_name",reconciliationInfoVO.getReconciliationScheme_name());
            SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
            workbench.setCheck_end_date(dateFormatWithoutTime.parse(infoQueryVO.getCheckEndDate()));
            if(infoQueryVO.getTranStartDate() != null){
                workbench.setTran_date(dateFormatWithoutTime.parse(infoQueryVO.getTranStartDate()));
            }
            if(infoQueryVO.getBusinessStartDate() != null){
                workbench.setBusiness_date(dateFormatWithoutTime.parse(infoQueryVO.getBusinessStartDate()));
            }
            workbench.setVourcher_uncheck_totalnum(reconciliationInfoVO.getVoucherUncheckTotalNum());
            workbench.setVourcher_uncheck_debitamountsum(reconciliationInfoVO.getVoucherUncheckDebitAmountSum());
            workbench.setVourcher_uncheck_creditamountsum(reconciliationInfoVO.getVoucherUncheckCreditAmountSum());
            workbench.setBank_uncheck_totalnum(reconciliationInfoVO.getBankUncheckTotalNum());
            workbench.setBank_uncheck_debitamountsum(reconciliationInfoVO.getBankUncheckDebitAmountSum());
            workbench.setBank_uncheck_creditamountsum(reconciliationInfoVO.getBankUncheckCreditAmountSum());
            //根据对符状态封装数据
            if (reconciliationStatus == 2) { //全部
                workbenchList.add(workbench);
            }else if (reconciliationStatus == 1){//已对符
                if(reconciliationInfoVO.getReconciliationStatus() == ReconciliationStatus.checked.getValue()){
                    workbenchList.add(workbench);
                }
            }else if (reconciliationStatus == 0){//未对符；包含未对账和为对符两种状态（20260130新增未对账，暂定如此处理）
                if(reconciliationInfoVO.getReconciliationStatus() == ReconciliationStatus.unchecked.getValue() || reconciliationInfoVO.getReconciliationStatus() == ReconciliationStatus.Unreconciled.getValue()){
                    workbenchList.add(workbench);
                }
            }
            workbench.setReconciliation_status(reconciliationInfoVO.getReconciliationStatus());
            workbench.setBalance_status(reconciliationInfoVO.getBalanceStatus());
            workbench.put("balanceAdjustResult",reconciliationInfoVO.getBalanceAdjustResult());
            //银行日记账增加 银行账户余额和企业日记账余额返回
            if (ReconciliationDataSource.BankJournal.getValue() == infoQueryVO.getReconciliationDataSource()){
                workbench.put("bank_balance",reconciliationInfoVO.getBankBalanceAmoount());
                workbench.put("journal_balance",reconciliationInfoVO.getJournalBalanceAmoount());
            }
            //账簿id集合
            workbench.put("accbookids",reconciliationInfoVO.getAccbookids());
            if (reconciliationInfoVO.getReconciliationStatus() == ReconciliationStatus.unchecked.getValue() || reconciliationInfoVO.getReconciliationStatus() == ReconciliationStatus.Unreconciled.getValue()){
                uncheckedNum ++;
            }
            if (reconciliationInfoVO.getReconciliationStatus() == ReconciliationStatus.checked.getValue()){
                checkedNum ++;
            }
        }
        //统计未对符，已对符，全部的数量
        List<CtmJSONObject> sumList = new ArrayList<>();
        CtmJSONObject sum = new CtmJSONObject();
        sum.put("all",reconciliationInfoVOList.size());
        sum.put("status_0",uncheckedNum);
        sum.put("status_1",checkedNum);
        if (workbenchList.size() == 0){
            workbenchList.add(new BankvourchercheckWorkbench());
        }
        sumList.add(sum);
        workbenchList.get(0).put("tagSum",sumList);
        return workbenchList;
    }

    private List<String> getValueList(FilterCommonVO vo) {
        List<String> valueList = new ArrayList<>();
        if (vo.getValue1() instanceof ArrayList) {
            valueList = (List<String>) vo.getValue1();
        } else {
            String orgValue = vo.getValue1().toString();
            valueList.add(orgValue);
        }
        return valueList;
    }
}
