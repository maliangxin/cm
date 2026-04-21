package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import cn.hutool.core.thread.BlockPolicy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.diwork.common.util.BeanPropertyCopyUtil;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.balanceadjust.vo.BalanceAdjustQueryInfoVO;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResultSerevice;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.bankvouchercheck.service.BankVoucherCheckService;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountReconciliationInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.rule.JournalQueryRule;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @description: 自动生成月末余额调节表，具体实现接口
 * @author: wanxbo@yonyou.com
 * @date: 2025/10/30 10:06
 */
@Service
@Slf4j
public class BalanceAdjustAutoGenerateServiceImpl implements BalanceAdjustAutoGenerateService{

    //线程池
    static ExecutorService executorService = null;
    static ExecutorService warningExecutorService = null;
    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(50, 200,200,"cmp-balanceadjust-autogenerate-async-");
        warningExecutorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(10, 50,100,"cmp-balanceadjust-ungeneratewarning-async-");
    }

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private BankVoucherCheckService bankVoucherCheckService;
    @Autowired
    private BalanceAdjustServiceImpl balanceAdjustService;
    @Autowired
    private BalanceBatchOperationService balanceBatchOperationService;
    @Resource
    private BalanceAdjustResultSerevice balanceAdjustResultSerevice;

    /**
     * 生成月末余额调节表，具体实现流程
     */
    @Override
    public boolean generateMonthEndBalanceAdjust(CtmJSONObject param) throws Exception {
        //1 根据入参查询需要生成余额调节表的账户信息
        //余额调节表通用查询参数
        BalanceAdjustQueryInfoVO balanceAdjustQueryInfoVO = initBalanceQueryInfoVO(param);
        //需要生成月末余额调节表的银行账户信息，和相关的对账方案信息
        List<BankAccountInfoVO> bankAccountInfoVOList = queryBankAccountInfo(param,false);

        //2 分批进行生成余额调节表；需要记录对应失败的信息
        // 建议动态调整批次大小
        int batchSize = bankAccountInfoVOList.size() < 50 ? 1 : Math.min(10, bankAccountInfoVOList.size() / 50);
        ThreadPoolUtil.executeByBatch(executorService, bankAccountInfoVOList, batchSize, "CM34_自动生成月末余额调节表_分批任务", (int fromIndex, int toIndex) -> {//@notranslate
            String builder = "";
            for (int t = fromIndex; t < toIndex; t++) {
                BankAccountInfoVO bankAccountInfoVO = bankAccountInfoVOList.get(t);
                //2-1判断是否需要生成余额调节表：1不存在时可以生成；2存在时，判断是否是审批通过，未审批通过则不可以生成
                BalanceAdjustResult latestResult = queryLatestBalanceAdjustResult(bankAccountInfoVO);
                //业务日志记录
                CtmJSONObject logParams = new CtmJSONObject();
                logParams.put("银行账户信息", bankAccountInfoVO);//@notranslate
                logParams.put("最近日期余额调节表", latestResult != null ? latestResult : "不存在最近的余额调节表");//@notranslate
                String logCode = bankAccountInfoVO.getReconciliationDataSource()  == ReconciliationDataSource.BankJournal.getValue() ?
                        "Journal_BankAccount:" + bankAccountInfoVO.getBankaccount_account(): "Voucher_BankAccount:" +bankAccountInfoVO.getBankaccount_account();
                if (latestResult != null) {
                    if (latestResult.getVerifystate() != VerifyState.COMPLETED.getValue()){
                        logParams.put("resultMsg","最近的余额调节表未审批，不进行后续生成");//@notranslate
                        ctmcmpBusinessLogService.saveBusinessLog(logParams,"bankAccount:"+bankAccountInfoVO.getBankaccount_account(), "最近的余额调节表未审批", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
                        continue;
                    }else {
                        Date endDate = DateUtils.parseDate(balanceAdjustQueryInfoVO.getEndDate(), "yyyy-MM-dd");
                        if (latestResult.getDzdate().compareTo(endDate) >= 0) {
                            logParams.put("resultMsg","最近的余额调节表日期大于等于结束日期，不进行后续生成");//@notranslate
                            ctmcmpBusinessLogService.saveBusinessLog(logParams,"bankAccount:"+bankAccountInfoVO.getBankaccount_account(), "日期不满足生成条件", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
                            continue;
                        }
                    }
                }

                //2-2 明确开始和结束日期，并查询出中间所有的月末日期
                BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, bankAccountInfoVO.getReconciliationScheme());
                String startDate = getStartDateStr(latestResult,bankReconciliationSetting,balanceAdjustQueryInfoVO);
                String endDate = getEndDateStr(latestResult,bankReconciliationSetting,balanceAdjustQueryInfoVO);
                if (DateUtils.parseDate(startDate, "yyyy-MM-dd").compareTo(DateUtils.parseDate(endDate, "yyyy-MM-dd")) >= 0) {
                    logParams.put("resultMsg",String.format("计算所得起始日期[%s]大于等于结束日期[%s]，不进行后续生成", startDate, endDate));//@notranslate
                    ctmcmpBusinessLogService.saveBusinessLog(logParams,"bankAccount:"+bankAccountInfoVO.getBankaccount_account(), "日期不满足生成条件", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
                    continue;
                }
                List<String> monthEndDateList = getMonthEndDateList(startDate, endDate);
                if (monthEndDateList.size() == 0){ //不存在月末日期，则不继续生成
                    logParams.put("resultMsg",String.format("起始日期[%s]和结束日期[%s]之间不存在月末日期，不进行后续生成", startDate, endDate));//@notranslate
                    ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "日期不满足生成条件", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
                    continue;
                }

                //2-3 按照月末日期批量生成余额调节表;判断是否开启审批流，开启后只生成第一个月末日期；未开启则全部末日日期生成余额调节表
                if (queryHasProcessDefinition(bankAccountInfoVO.getAccentity())) {
                    //若第一个月末日期和上一个余额调节表日期相同，则生成第二个，没有则直接终止
                    String monthEndDate = monthEndDateList.get(0);
                    if(latestResult != null && latestResult.getDzdate().equals(DateUtils.parseDate(monthEndDate, "yyyy-MM-dd"))){
                        if(monthEndDateList.size() > 1){
                            generateBalanceAdjust(bankAccountInfoVO, monthEndDateList.get(1), logParams);
                        }
                    }else {
                        generateBalanceAdjust(bankAccountInfoVO, monthEndDate, logParams);
                    }
                }else {
                    for (String monthEndDate : monthEndDateList) {
                        //若第一个月末日期和上一个余额调节表日期相同，直接跳过
                        if(latestResult != null && latestResult.getDzdate().equals(DateUtils.parseDate(monthEndDate, "yyyy-MM-dd"))){
                            continue;
                        }
                        if (!generateBalanceAdjust(bankAccountInfoVO, monthEndDate, logParams)) {
                            break;
                        }
                    }
                }
            }
            return builder;
        }, false);

        return true;
    }

    /**
     * 按照对账截止日期生成余额调节表，具体实现
     * @param bankAccountInfoVO 银行账户信息
     * @param checkEndDate 月末日期，对账截止日期
     * @param logParams 业务日志参数
     * @return 是否生成成功
     * @throws Exception
     */
    @Override
    public boolean generateBalanceAdjust(BankAccountInfoVO bankAccountInfoVO,String checkEndDate,CtmJSONObject logParams) throws Exception{
        String logCode = bankAccountInfoVO.getReconciliationDataSource()  == ReconciliationDataSource.BankJournal.getValue() ?
                "Journal_BankAccount:" + bankAccountInfoVO.getBankaccount_account(): "Voucher_BankAccount:" +bankAccountInfoVO.getBankaccount_account();
        BankVoucherInfoQueryVO bankVoucherInfoQueryVO = new BankVoucherInfoQueryVO();
        BeanPropertyCopyUtil.copyProperties(bankAccountInfoVO,bankVoucherInfoQueryVO);
        bankVoucherInfoQueryVO.setCheckEndDate(checkEndDate);
        //1,校验是否存在未达项；存在则终止生成
        //查询关联的凭证或银行日记账数据
        List<Journal> voucherList = new ArrayList<>();
        if (bankAccountInfoVO.getReconciliationDataSource() == ReconciliationDataSource.Voucher.getValue()){
            voucherList = bankVoucherCheckService.getVoucherByBankAccountInfo(bankVoucherInfoQueryVO);
        }
        if (bankAccountInfoVO.getReconciliationDataSource()  == ReconciliationDataSource.BankJournal.getValue()){
            voucherList = bankVoucherCheckService.getJournalByBankAccountInfo(bankVoucherInfoQueryVO);
        }
        //查询关联的银行流水数据
        List<BankReconciliation> bankReconciliationList = bankVoucherCheckService.getBankReconciliationByBankAccountInfo(bankVoucherInfoQueryVO);
        if (!CollectionUtils.isEmpty(voucherList) || !CollectionUtils.isEmpty(bankReconciliationList)) {
            logParams.put("resultMsg",String.format("对账截止日[%s]前存在未达项，终止生成余额调节表。银行流水未达项个数：%s,凭证或日记账未达项个数:%s", checkEndDate, bankReconciliationList.size(), voucherList.size()));//@notranslate
            ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "存在未达项", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
            return false;
        }

        //2,校验银行账户余额和企业方余额是否相同。不相同则终止生成
        CtmJSONObject enterpriseBalanceVO = initBalanceAdjustQueryParam(bankAccountInfoVO, checkEndDate);
        //余额调节表查询详情
        CtmJSONObject responseMsg = balanceAdjustService.query(enterpriseBalanceVO, false);
        //responseMsg 为空代表存在同一个账户+币种的余额调节表生成过程，跳过执行
        if (responseMsg == null){
            logParams.put("resultMsg",String.format("银行账号【%s】+币种【%s】配置在多个的财务账簿或对账方案下，有其他数据正在同时生成余额调节表，该条数据跳过。", bankAccountInfoVO.getBankaccount_account(),bankAccountInfoVO.getCurrency_name()));//@notranslate
            ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "账户+币种获取锁失败。同一个账号+币种配置在多个财务账簿或对账方案下。", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
            return false;
        }
        //余额调节表获取余额逻辑调整：直联账户余额为空时，银行账户余额为空，调整余额为空，且设置为不平
        if (responseMsg.getBoolean("isEmptyBalance")){
            logParams.put("resultMsg",String.format("银行账户为直联账户，对账截止日[%s]账户历史余额不存在，余额不平，生成终止。", checkEndDate));//@notranslate
            ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "余额不平", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
            return false;
        }
        //balenceState= 1余额已平; 2余额不平
        if (responseMsg.getInteger("balenceState") != 1){
            logParams.put("resultMsg",String.format("对账截止日[%s]，企业方调整余额[%s],银行方调整余额[%s]，余额不平，生成终止。", checkEndDate,responseMsg.getString("journaltzye"),responseMsg.getString("banktzye")));//@notranslate
            ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "余额不平", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
            return false;
        }
        //组装生成余额调节表参数 1余额调节表表头；2查询凭证时的参数 3公共信息json
        balanceBatchOperationService.mergeResponseData(responseMsg, enterpriseBalanceVO, "0");
        responseMsg.put("accbookids", bankVoucherInfoQueryVO.getAccbookids());
        responseMsg.put("journalyedetailinfo",CtmJSONObject.toJSONString(responseMsg.get("voucherDetailInfoList")));
        //1余额调节表表头
        Json json = new Json(CtmJSONObject.toJSONString(responseMsg));
        BalanceAdjustResult balanceAdjustResult = Objectlizer.decodeObj(json,BalanceAdjustResult.ENTITY_NAME);
        //2查询凭证时的参数
        CtmJSONObject filterArgsJson = initVoucherQueryFilterArgs(bankAccountInfoVO, checkEndDate);
        //3公共信息json,设置为自动提交
        responseMsg.put("saveAndSubmit",true);
        try {
            //调用服务层方法添加余额调节表信息
            balanceAdjustResultSerevice.add(balanceAdjustResult,CtmJSONObject.toJSONString(filterArgsJson),responseMsg);
        } catch (Exception e) {
            logParams.put("resultMsg",String.format("对账截止日[%s]生成余额调节表异常，生成终止。异常信息：%s",checkEndDate,e.getMessage()));//@notranslate
            ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "生成时异常", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成终止");//@notranslate
            return false;
        }

        logParams.put("生成的月末余额调节表信息",balanceAdjustResult);//@notranslate
        ctmcmpBusinessLogService.saveBusinessLog(logParams,logCode, "余额调节表生成成功", IServicecodeConstant.BALANCEADJUSTRESULT, "自动生成月末余额调节表","生成成功");//@notranslate
        return true;
    }

    @Override
    public Map<String, Object> warningMonthEndUngenerated(CtmJSONObject param) throws Exception {
        //存储具体的银行账户余额调节表生成情况
        List<CtmJSONObject> dataArrayList = new ArrayList<>();
        //需要预警未生成余额调节表的银行账户信息，和相关的对账方案信息
        List<BankAccountInfoVO> bankAccountInfoVOList = queryBankAccountInfo(param,true);
        String lastMonthEndDate = DateUtils.dateFormat(this.getLastMonthEndDateIncludeCurrent(new Date()), "yyyy-MM-dd");
        //分批进行查询月末余额调节表，记录月末生成的详情
        // 建议动态调整批次大小
        int batchSize = bankAccountInfoVOList.size() < 10 ? 1 : Math.min(5, bankAccountInfoVOList.size() / 10);
        ThreadPoolUtil.executeByBatch(warningExecutorService, bankAccountInfoVOList, batchSize, "余额调节表-预警月末未生成数据查询", (int fromIndex, int toIndex) -> {//@notranslate
            String builder = "";
            for (int t = fromIndex; t < toIndex; t++) {
                BankAccountInfoVO bankAccountInfoVO = bankAccountInfoVOList.get(t);
                BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, bankAccountInfoVO.getReconciliationScheme());
                //对账方案启用日期晚于上个月末，则不预警
                if (bankReconciliationSetting !=null && bankReconciliationSetting.getEnableDate().after(this.getLastMonthEndDateIncludeCurrent(new Date()))){
                    continue;
                }
                BalanceAdjustResult lastBalanceAdjustResult = this.queryMonthEndBalanceAdjust(bankAccountInfoVO,lastMonthEndDate);
                CtmJSONObject data = new CtmJSONObject();
                //预警任务的组织权限，固定key值
                data.put("warnPrimaryOrgId", bankAccountInfoVO.getAccentity());
                //预警任务中返回的结果集
                data.put("accentity", bankAccountInfoVO.getAccentity());
                data.put("accentity_code", bankAccountInfoVO.getAccentity_code());
                data.put("accentity_name", bankAccountInfoVO.getAccentity_name());
                data.put("accbook_b_name", bankAccountInfoVO.getAccbook_b_name());
                data.put("bankaccount", bankAccountInfoVO.getBankaccount());
                data.put("bankaccount_name", bankAccountInfoVO.getBankaccount_name());
                data.put("bankaccount_account", bankAccountInfoVO.getBankaccount_account());
                data.put("currency", bankAccountInfoVO.getCurrency());
                data.put("currency_name", bankAccountInfoVO.getCurrency_name());
                data.put("reconciliationScheme_name", bankAccountInfoVO.getReconciliationScheme_name());
                data.put("reconciliationdatasource_name", ReconciliationDataSource.find(bankAccountInfoVO.getReconciliationDataSource()).getName());
                data.put("balanceadjustresultcode",lastBalanceAdjustResult!=null?lastBalanceAdjustResult.getCode(): null);
                data.put("checkenddate",lastBalanceAdjustResult!=null? DateUtils.dateFormat(lastBalanceAdjustResult.getDzdate(), "yyyy-MM-dd"): null);
                data.put("verifyState_name",lastBalanceAdjustResult!=null? VerifyState.find(lastBalanceAdjustResult.getVerifystate()).getName(): "未生成");//@notranslate
                //标识是否生成
                data.put("isGenerated",lastBalanceAdjustResult != null);
                dataArrayList.add(data);
            }
            return builder;
        }, false);

        // 按照 accentity_code 和 bankaccount_account 正序排列
        dataArrayList.sort((o1, o2) -> {
            String code1 = o1.getString("accentity_code");
            String code2 = o2.getString("accentity_code");
            String account1 = o1.getString("bankaccount_account");
            String account2 = o2.getString("bankaccount_account");
            int codeCompare = code1.compareTo(code2);
            if (codeCompare != 0) {
                return codeCompare;
            }
            return account1.compareTo(account2);
        });

        //统计预警信息，示例： 对账组织[ABC]截止日期[2025-09-30] 共12个账户需生成余额调节表，目前已生成10个账户余额调节表，未生成2个银行账户，分别为账户名称[银行账号01]、账户名称[银行账号02]；
        Set<String> accentitySet = new HashSet<>(); //对账组织集合
        List<String> accentityList = new ArrayList<>(); //对账组织名称集合
        Set<String> bankaccountSet = new HashSet<>(); //所有账号id+币种id集合
        Set<String> generatedbankaccountSet = new HashSet<>(); //已生成的账号id集合
        Set<String> ungeneratedbankaccountSet = new HashSet<>(); //未生成的账号id集合
        List<String> ungeneratedbankaccountNameList = new ArrayList<>(); //未生成账户名称+币种名称集合
        for (CtmJSONObject data : dataArrayList) {
            if (!accentitySet.contains(data.getString("accentity"))){
                accentitySet.add(data.getString("accentity"));
                accentityList.add(data.getString("accentity_name"));
            }
            bankaccountSet.add(data.getString("bankaccount")+data.getString("currency"));
            if (data.getBoolean("isGenerated")){
                generatedbankaccountSet.add(data.getString("bankaccount")+data.getString("currency"));
            }else {
                if (!ungeneratedbankaccountSet.contains(data.getString("bankaccount")+data.getString("currency"))){
                    ungeneratedbankaccountSet.add(data.getString("bankaccount")+data.getString("currency"));
                    ungeneratedbankaccountNameList.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2158CD2404B80009", "账户名称【%s】币种【%s】") /* "账户名称【%s】币种【%s】" */,data.getString("bankaccount_name"),data.getString("currency_name")));
                }
            }
        }
        //预警信息
        String msg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2155A9B20550000A", "对账组织【%s】截止日期【%s】 共%d个账户需生成余额调节表，目前已生成%d个账户余额调节表，未生成%d个银行账户，分别为%s") /* "对账组织【%s】截止日期【%s】 共%d个账户需生成余额调节表，目前已生成%d个账户余额调节表，未生成%d个银行账户，分别为%s" */,
                String.join(",", accentityList),
                lastMonthEndDate,
                bankaccountSet.size(),
                generatedbankaccountSet.size(),
                ungeneratedbankaccountSet.size(),
                String.join("、", ungeneratedbankaccountNameList));//@notranslate
        //存储 _details 字段
        CtmJSONArray detailsArray = new CtmJSONArray();
        CtmJSONObject detail = new CtmJSONObject();
        detail.put("msg", msg);
        detail.put("data", dataArrayList);
        detailsArray.add(detail);
        //返回预警信息集合
        Map<String, Object> result = new HashMap<>();
        result.put("_details", detailsArray);
        if(dataArrayList.size()>0){
            result.put("status", 1);//执行结果： 0：失败；1：成功
        }else {
            result.put("status", 0);//执行结果： 0：失败；1：成功
        }
        //业务日志记录
        CtmJSONObject logParams = new CtmJSONObject();
        logParams.put("预警上个月末日期", lastMonthEndDate);//@notranslate
        logParams.put("预警任务返回数据", result);//@notranslate
        ctmcmpBusinessLogService.saveBusinessLog(logParams,"checkEndDate:"+lastMonthEndDate, "预警未生成余额调节表", IServicecodeConstant.BALANCEADJUSTRESULT, "预警未生成余额调节表","预警数据生成");//@notranslate

        return result;
    }

    private BalanceAdjustQueryInfoVO initBalanceQueryInfoVO(CtmJSONObject param) throws Exception {
        BalanceAdjustQueryInfoVO queryInfoVO = new BalanceAdjustQueryInfoVO();
        String startDate = !StringUtils.isEmpty(param.getString("startdate"))? param.getString("startdate") : null;
        String endDateStr = param.getString("enddate");
        String endDate;
        if (!StringUtils.isEmpty(endDateStr)) {
            // 如果endDate有值，与系统日期比较取较小值
            Date paramEndDate = DateUtils.parseDate(param.getString("enddate"), "yyyy-MM-dd");
            Date currentDate = new Date();
            Date finalEndDate = paramEndDate.before(currentDate) ? paramEndDate : currentDate;
            endDate = DateUtils.dateFormat(finalEndDate, "yyyy-MM-dd");
        } else {
            // 如果endDate无值，使用系统日期
            endDate = DateUtils.dateFormat(new Date(), "yyyy-MM-dd");
        }
        queryInfoVO.setStartDate(startDate);
        queryInfoVO.setEndDate(endDate);
        if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
            Date start = DateUtils.parseDate(startDate, "yyyy-MM-dd");
            Date end = DateUtils.parseDate(endDate, "yyyy-MM-dd");
            if (start.compareTo(end) >= 0) {
                throw new CtmException("起始日期不能大于等于结束日期");//@notranslate
            }
        }
        return queryInfoVO;
    }

    // 定义静态常量 Map；调度任务展示中文，数据库需要对应的数字
    private static final Map<String, String> RESOURCE_TYPE_MAP = new HashMap<String, String>() {{
        put("凭证", "1");//@notranslate
        put("银行日记账", "2");//@notranslate
    }};
    // 定义账户类型静态常量 Map (中文为key，数字为value)
    private static final Map<String, String> ACCT_TYPE_MAP = new HashMap<String, String>() {{
        put("基本", "0");//@notranslate
        put("一般", "1");//@notranslate
        put("临时", "2");//@notranslate
        put("专用", "3");//@notranslate
        put("票据", "4");//@notranslate
        put("其他", "5");//@notranslate
        put("第三方账户", "6");//@notranslate
    }};
    // 定义账户性质静态常量 Map (中文为key，数字为value)
    private static final Map<String, String> ACCOUNT_NATURE_MAP = new HashMap<String, String>() {{
        put("活期", "0");//@notranslate
        put("定期", "1");//@notranslate
        put("通知", "2");//@notranslate
        put("保证金", "3");//@notranslate
        put("贷款", "4");//@notranslate
    }};

    /**
     * 根据调度任务入参查询所需生成余额调节表的银行账户信息
     * @param param 调度任务入参
     * @param isQueryAccbook 是否查询财务账簿信息
     * @return
     * @throws Exception
     */
    private List<BankAccountInfoVO> queryBankAccountInfo(CtmJSONObject param,boolean isQueryAccbook) throws Exception {
        //获取对账方案，银行账户+币种+对账组织维度数据
        QuerySchema schema;
        if (isQueryAccbook){
            schema = QuerySchema.create().distinct().addSelect("mainid.reconciliationdatasource as reconciliationdatasource,mainid as reconciliationScheme,mainid.bankreconciliationschemename as reconciliationScheme_name,mainid.enableDate as enableDate," +
                    "bankaccount,bankaccount.name as bankaccount_name,bankaccount.account as bankaccount_account,bankaccount.bank as banktype,bankaccount.bank.name as banktype_name, " +
                    "currency,currency.name as currency_name,currency.moneyDigit as currency_moneyDigit, mainid.accentity as accentity,mainid.accentity.name as accentity_name,mainid.accentity.code as accentity_code" +
                    ",accbook_b ,accbook_b.name as accbook_b_name");
        }else {
            schema = QuerySchema.create().distinct().addSelect("mainid.reconciliationdatasource as reconciliationdatasource,mainid as reconciliationScheme,mainid.bankreconciliationschemename as reconciliationScheme_name,mainid.enableDate as enableDate," +
                    "bankaccount,bankaccount.name as bankaccount_name,bankaccount.account as bankaccount_account,bankaccount.bank as banktype,bankaccount.bank.name as banktype_name, " +
                    "currency,currency.name as currency_name,currency.moneyDigit as currency_moneyDigit, mainid.accentity as accentity,mainid.accentity.name as accentity_name,mainid.accentity.code as accentity_code");
        }

        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("enableStatus_b").in(statusList),
                QueryCondition.name("mainid.enableStatus").in(statusList)
        );
        //对账组织
        if (StringUtils.isNotEmpty(param.getString("accentity"))){
            String[] accentitys = param.getString("accentity").split(";");
            group.appendCondition(QueryCondition.name("mainid.accentity").in(Arrays.asList(accentitys)));
        }
        //银行账户
        if (StringUtils.isNotEmpty(param.getString("bankaccount"))){
            String[] bankaccountList = param.getString("bankaccount").split(";");
            group.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankaccountList)));
        }
        //币种
        if (StringUtils.isNotEmpty(param.getString("currency"))){
            String[] currencyList = param.getString("currency").split(";");
            group.appendCondition(QueryCondition.name("currency").in(Arrays.asList(currencyList)));
        }
        //银行类别
        if (StringUtils.isNotEmpty(param.getString("banktype"))){
            String[] banktypes= param.getString("banktype").split(";");
            group.appendCondition(QueryCondition.name("bankaccount.bank").in(Arrays.asList(banktypes)));
        }
        //数据源 1=凭证；2=银行日记账
        if (StringUtils.isNotEmpty(param.getString("resourcetype"))){
            String[] resourcetypes = param.getString("resourcetype").split(",");
            List<String> resourcetypeList = new ArrayList<>();
            for (String type : resourcetypes) {
                String trimmedType = type.trim();
                String mappedType = RESOURCE_TYPE_MAP.getOrDefault(trimmedType, trimmedType);
                resourcetypeList.add(mappedType);
            }
            group.appendCondition(QueryCondition.name("mainid.reconciliationdatasource").in(resourcetypeList));
        }
        // 账户类型 0=基本;1=一般;2=临时;3=专用;4=票据;5=其他;6=第三方账户
        if (StringUtils.isNotEmpty(param.getString("acctType"))) {
            String[] accountTypes = param.getString("acctType").split(",");
            List<String> accountTypeList = new ArrayList<>();
            for (String type : accountTypes) {
                String trimmedType = type.trim();
                String mappedType = ACCT_TYPE_MAP.getOrDefault(trimmedType, trimmedType);
                accountTypeList.add(mappedType);
            }
            group.appendCondition(QueryCondition.name("bankaccount.acctType").in(accountTypeList));
        }
        // 账户性质 0=活期;1=定期;2=通知;3=保证金;4=贷款
        if (StringUtils.isNotEmpty(param.getString("accountNature"))) {
            String[] accountNatures = param.getString("accountNature").split(",");
            List<String> accountNatureList = new ArrayList<>();
            for (String nature : accountNatures) {
                String trimmedNature = nature.trim();
                String mappedNature = ACCOUNT_NATURE_MAP.getOrDefault(trimmedNature, trimmedNature);
                accountNatureList.add(mappedNature);
            }
            group.appendCondition(QueryCondition.name("bankaccount.accountNature").in(accountNatureList));
        }
        schema.addCondition(group);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);

        //数据组装
        List<BankAccountInfoVO> bankAccountInfoVOList = new ArrayList<>();
        for (Map<String, Object> map : bankReconciliationSetting_bs) {
            BankAccountInfoVO bankAccountInfoVO = new BankAccountInfoVO();
            // 使用 Optional 避免空指针异常
            bankAccountInfoVO.setAccentity(Optional.ofNullable(map.get("accentity")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setAccentity_name(Optional.ofNullable(map.get("accentity_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setAccentity_code(Optional.ofNullable(map.get("accentity_code")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setCurrency(Optional.ofNullable(map.get("currency")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setCurrency_name(Optional.ofNullable(map.get("currency_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setCurrency_moneyDigit(Optional.ofNullable(map.get("currency_moneyDigit")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setReconciliationScheme(Optional.ofNullable(map.get("reconciliationScheme")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setReconciliationScheme_name(Optional.ofNullable(map.get("reconciliationScheme_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBankaccount(Optional.ofNullable(map.get("bankaccount")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBankaccount_name(Optional.ofNullable(map.get("bankaccount_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBankaccount_account(Optional.ofNullable(map.get("bankaccount_account")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBanktype(Optional.ofNullable(map.get("banktype")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBanktype_name(Optional.ofNullable(map.get("banktype_name")).map(Object::toString).orElse(null));
            //不同的mdd版本可能返回的是date或者string
            Object enableDateObj = map.get("enableDate");
            Date enableDate;
            if (enableDateObj instanceof String) {
                // 如果返回的是String类型，需要进行转换
                String dateString = (String) enableDateObj;
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    enableDate = formatter.parse(dateString);
                } catch (ParseException e) {
                    log.error("日期转换异常: " + dateString, e);
                    // 处理转换异常，可以返回默认值或抛出异常
                    enableDate = new Date(); // 或其他默认处理
                }
            } else if (enableDateObj instanceof Date) {
                // 如果返回的是Date类型，直接使用
                enableDate = (Date) enableDateObj;
            }else {
                enableDate = new Date();
            }
            bankAccountInfoVO.setEnableDate(DateUtils.parseDateToStr(enableDate, "yyyy-MM-dd"));
            bankAccountInfoVO.setReconciliationDataSource(Short.valueOf(map.get("reconciliationdatasource").toString()));
            if (isQueryAccbook){
                bankAccountInfoVO.setAccbook_b(Optional.ofNullable(map.get("accbook_b")).map(Object::toString).orElse(null));
                bankAccountInfoVO.setAccbook_b_name(Optional.ofNullable(map.get("accbook_b_name")).map(Object::toString).orElse(null));
            }
            bankAccountInfoVOList.add(bankAccountInfoVO);
        }
        return bankAccountInfoVOList;
    }

    /**
     * 查询最近生成的余额调节表
     * @param bankAccountInfoVO 银行账户信息
     * @return 最近的余额调节表信息
     * @throws Exception
     */
    private BalanceAdjustResult queryLatestBalanceAdjustResult(BankAccountInfoVO bankAccountInfoVO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("currency").eq(bankAccountInfoVO.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(bankAccountInfoVO.getBankaccount()),//银行账号
                QueryCondition.name("bankreconciliationscheme").eq(bankAccountInfoVO.getReconciliationScheme())//对账方案id
        );
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("dzdate", "desc"));
        List<BalanceAdjustResult> checkList = MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
        if (checkList != null && checkList.size() > 0){
            return checkList.get(0);
        }
        return null;
    }

    /**
     * 查询对账截止日 大于等于 月末日期的第一个月调节表
     * @param bankAccountInfoVO 银行账户信息
     * @return 最近的余额调节表信息
     * @throws Exception
     */
    private BalanceAdjustResult queryMonthEndBalanceAdjust(BankAccountInfoVO bankAccountInfoVO,String monthEndDate) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("currency").eq(bankAccountInfoVO.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(bankAccountInfoVO.getBankaccount()),//银行账号
                QueryCondition.name("bankreconciliationscheme").eq(bankAccountInfoVO.getReconciliationScheme()),//对账方案id
                QueryCondition.name("dzdate").egt(monthEndDate)
        );
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("dzdate", "desc"));
        List<BalanceAdjustResult> checkList = MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
        if (checkList != null && checkList.size() > 0){
            return checkList.get(0);
        }
        return null;
    }

    private String getStartDateStr(BalanceAdjustResult latestResult,BankReconciliationSetting bankReconciliationSetting,BalanceAdjustQueryInfoVO balanceAdjustQueryInfoVO) throws Exception{
        String startDate;
        String queryStartDate = balanceAdjustQueryInfoVO.getStartDate();
        if (latestResult == null) {
            Date enableDate = bankReconciliationSetting.getEnableDate();
            if (StringUtils.isEmpty(queryStartDate)) {
                startDate = DateUtils.dateFormat(enableDate, "yyyy-MM-dd");
            } else {
                Date queryStartDateObj = DateUtils.parseDate(queryStartDate, "yyyy-MM-dd");
                Date maxDate = enableDate.compareTo(queryStartDateObj) >=0 ? enableDate : queryStartDateObj;
                startDate = DateUtils.dateFormat(maxDate, "yyyy-MM-dd");
            }
        }else {
            Date dzdate = latestResult.getDzdate();
            if (StringUtils.isEmpty(queryStartDate)) {
                startDate = DateUtils.dateFormat(dzdate, "yyyy-MM-dd");
            } else {
                Date queryStartDateObj = DateUtils.parseDate(queryStartDate, "yyyy-MM-dd");
                Date maxDate = dzdate.compareTo(queryStartDateObj) >= 0 ? dzdate : queryStartDateObj;
                startDate = DateUtils.dateFormat(maxDate, "yyyy-MM-dd");
            }
        }
        return startDate;
    }

    private String getEndDateStr(BalanceAdjustResult latestResult, BankReconciliationSetting bankReconciliationSetting, BalanceAdjustQueryInfoVO balanceAdjustQueryInfoVO) throws Exception {
        String endDate;
        String queryEndDate = balanceAdjustQueryInfoVO.getEndDate();
        if (latestResult == null) {
            Date enableDate = bankReconciliationSetting.getEnableDate();
            Date queryEndDateObj = DateUtils.parseDate(queryEndDate, "yyyy-MM-dd");
            Date maxDate = queryEndDateObj.compareTo(enableDate) >= 0 ? queryEndDateObj : enableDate;
            endDate = DateUtils.dateFormat(maxDate, "yyyy-MM-dd");
        } else {
            Date dzdate = latestResult.getDzdate();
            Date queryEndDateObj = DateUtils.parseDate(queryEndDate, "yyyy-MM-dd");
            Date maxDate = queryEndDateObj.after(dzdate) ? queryEndDateObj : dzdate;
            endDate = DateUtils.dateFormat(maxDate, "yyyy-MM-dd");
        }
        return endDate;
    }

    private List<String> getMonthEndDateList(String startDate, String endDate) throws Exception {
        List<String> monthEndDateList = new ArrayList<>();
        Date startDateObj = DateUtils.parseDate(startDate, "yyyy-MM-dd");
        Date endDateObj = DateUtils.parseDate(endDate, "yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        // 设置起始日期
        calendar.setTime(startDateObj);
        // 遍历每个月
        while (calendar.getTime().compareTo(endDateObj) <= 0) {
            // 获取当月最后一天
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            // 如果月末日期在结束日期范围内，则添加到列表中
            Date monthEnd = calendar.getTime();
            if (monthEnd.compareTo(endDateObj) <= 0) {
                monthEndDateList.add(DateUtils.dateFormat(monthEnd, "yyyy-MM-dd"));
            }
            // 移动到下个月第一天
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }
        return monthEndDateList;
    }

    /**
     * 获取当前日期的上一个月末日期，如果当前日期是月末则返回当前日期
     * @param currentDate 当前日期
     * @return 上一个月末日期或当前日期（如果当前日期是月末）
     */
    private Date getLastMonthEndDateIncludeCurrent(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        // 检查当前日期是否是月末
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (currentDay == maxDay) {
            // 如果当前日期是月末，直接返回当前日期
            return currentDate;
        } else {
            // 否则返回上个月末日期
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            return calendar.getTime();
        }
    }


    /**
     * 判断是否开起来审批流
     * @param accentity 对账组织
     */
    private boolean queryHasProcessDefinition(String accentity) throws  Exception{
        //发起审批流如果配了的话
        ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
        param.setCategory(ICmpConstant.CM_CMP_BALANCEADJUSTRESULT);
        param.setBillTypeId(ICmpConstant.CM_CMP_BALANCEADJUSTRESULT);
        param.setOrgId(accentity);
        RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
        Object processDefinition = repositoryService.checkProcessDefinition(param);
        Map<String, Object> queryTransType = cmCommonService.queryTransTypeByForm_id(ICmpConstant.CM_CMP_BALANCEADJUSTRESULT);
        //单据类型下没有挂审批流的话就找交易类型下的
        if (!((ObjectNode) processDefinition).get("hasProcessDefinition").booleanValue()) {
            if (MapUtils.isNotEmpty(queryTransType)) {
                param.setCategory(queryTransType.get("id").toString());
                processDefinition = repositoryService.checkProcessDefinition(param);
            }
        }
        return ((ObjectNode) processDefinition).get("hasProcessDefinition").booleanValue();
    }

    /**
     * 初始化余额调节表查询参数，用来调用balanceAdjustService.query(enterpriseBalanceVO, false);接口
     */
    private CtmJSONObject initBalanceAdjustQueryParam(BankAccountInfoVO bankAccountInfo,String checkEndDate){
        // 创建新的业务对象实例
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        List<Map<String, Object>> commonVOs = new ArrayList<>();
        // 添加字段到 commonVOs
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "accentity", bankAccountInfo.getAccentity(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "bankaccount", bankAccountInfo.getBankaccount(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "currency", bankAccountInfo.getCurrency(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, JournalQueryRule.BANKRECONCILIATIONSCHEME, bankAccountInfo.getReconciliationScheme(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "dzdate", checkEndDate, null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "accentity_name", bankAccountInfo.getAccentity_name(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "accentity_code", bankAccountInfo.getAccentity_code(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "currency_name", bankAccountInfo.getCurrency_name(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "bankaccount_name", bankAccountInfo.getBankaccount_name(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "reconciliationScheme", bankAccountInfo.getReconciliationScheme_name(), null);
        balanceBatchOperationService.addFieldIfNotEmpty(commonVOs, "bankaccount_account", bankAccountInfo.getBankaccount_account(), null);
        ctmJSONObject.put("commonVOs", commonVOs);
        return ctmJSONObject;
    }

    /**
     * 获取凭证查询参数filterArgs
     */
    private CtmJSONObject initVoucherQueryFilterArgs(BankAccountInfoVO bankAccountInfo,String checkEndDate){
        CtmJSONObject argsJson = new CtmJSONObject();
        //分页信息
        CtmJSONObject pageInfo = new CtmJSONObject();
        pageInfo.put("pageIndex",0);
        pageInfo.put("pageSize",1000);
        argsJson.put("page",pageInfo);

        //过滤条件
        CtmJSONObject conditionJson = new CtmJSONObject();
        CtmJSONArray ctmJSONArray = new CtmJSONArray();
        //业务日期
        LinkedHashMap<String,String> makeTimeMap = new LinkedHashMap<>();
        makeTimeMap.put("itemName","makeTime");
        makeTimeMap.put("value2", checkEndDate);
        ctmJSONArray.add(makeTimeMap);

        //会计主体
        LinkedHashMap<String,String> accentityMap = new LinkedHashMap<>();
        accentityMap.put("itemName","accentity");
        accentityMap.put("value1",bankAccountInfo.getAccentity());
        ctmJSONArray.add(accentityMap);

        //银行账户
        LinkedHashMap<String,String> bankaccountMap = new LinkedHashMap<>();
        bankaccountMap.put("itemName","bankaccount");
        bankaccountMap.put("value1",bankAccountInfo.getBankaccount());
        ctmJSONArray.add(bankaccountMap);

        //币种
        LinkedHashMap<String,String> currencyMap = new LinkedHashMap<>();
        currencyMap.put("itemName","currency");
        currencyMap.put("value1",bankAccountInfo.getCurrency());
        ctmJSONArray.add(currencyMap);

        //是否勾对
        LinkedHashMap<String,String> checkflagMap = new LinkedHashMap<>();
        checkflagMap.put("itemName","checkflag");
        checkflagMap.put("value1","false");
        ctmJSONArray.add(checkflagMap);

        //是否封存
        LinkedHashMap<String,String> sealflagMap = new LinkedHashMap<>();
        sealflagMap.put("itemName","sealflag");
        sealflagMap.put("value1","false");
        ctmJSONArray.add(sealflagMap);

        //对账方案id
        LinkedHashMap<String,String> bankreconciliationschemeMap = new LinkedHashMap<>();
        bankreconciliationschemeMap.put("itemName","bankreconciliationscheme");
        bankreconciliationschemeMap.put("value1",bankAccountInfo.getReconciliationScheme());
        ctmJSONArray.add(bankreconciliationschemeMap);
        conditionJson.put("commonVOs",ctmJSONArray);
        conditionJson.put("reconciliationdatasourceid",bankAccountInfo.getReconciliationScheme());

        argsJson.put("condition",conditionJson);
        argsJson.put("billnum","cmp_bankjournalcheck_balancelist");

        return argsJson;
    }

}
