package com.yonyoucloud.fi.cmp.intelligentdealdetail.common.command;

import cn.hutool.core.date.DateUtil;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.CtmExceptionConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.DirectlyAccountDataPullUtil;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.BankDealDetailAccessFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.YQLDataAccessModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.model.DirectAccountRequestInfo;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.model.PullCommandInfo;
import com.yonyoucloud.fi.cmp.util.BankAccountUtil;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * 直连账户数据拉取服务类
 */
@Slf4j
@Service
public class DirectedAccountBankDealDetailPullCommand extends BankAccountDataPullCommand {

    @Autowired
    BankAccountSettingService bankAccountSettingService;
    @Autowired
    BankDealDetailAccessFacade process;

    @Autowired
    BankDealDetailService bankDealDetailService;
    String HAS_NEXT_PAGE = "1";

    int SUCCESS_CODE = 1;

    /**
     * 执行查询直连账户交易明细调度任务
     * @param commondInfo
     * @throws Exception
     */
    @Override
    public void execute(PullCommandInfo commondInfo) throws Exception {
        ExecutorService executorService = buildThreadPoolForTaskBankDeail(Integer.valueOf(commondInfo.getCorepoolsize()));
        List<DirectAccountRequestInfo> bankAccounts = initRequestBankAccountInfo(commondInfo);
        executePullDirectAccountDatas(bankAccounts,executorService);
    }

    /**
     * 执行拉取直连账户交易明细的请求
     * @param bankAccounts
     * @throws Exception
     */
    private void executePullDirectAccountDatas(List<DirectAccountRequestInfo> bankAccounts,ExecutorService executorService) throws Exception{
        if(CollectionUtils.isNotEmpty(bankAccounts)){
            ThreadPoolUtil.executeByBatch(executorService, bankAccounts, 10, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056F", "执行查询直连账户交易明细调度任务") /* "执行查询直连账户交易明细调度任务" */, (int fromIndex, int toIndex) -> {
                for (int t = fromIndex; t < toIndex; t++) {
                    DirectAccountRequestInfo batchBankAccount = bankAccounts.get(t);
                    pullData(batchBankAccount);
                }
                return null;
            });
        }
    }

    /**
     * 拉取交易明细
     *
     * @param directAccountRequestInfo
     * @throws Exception
     */
    String pullData(DirectAccountRequestInfo directAccountRequestInfo) throws Exception {
        List<BasicNameValuePair> requestData = this.formatRequestData(directAccountRequestInfo);
        long starttime = System.currentTimeMillis();
        CtmJSONObject result = DirectlyAccountDataPullUtil.doPullData(directAccountRequestInfo.getTranCode(), requestData);
//        if(BooleanUtils.b(AppContext.getEnvConfig("test.yql","false"))){
//            String testJson = "{\"code\":1,\"data\":{\"response_body\":{\"acct_name\":\"分部财务公司户1\",\"acct_no\":\"88661\",\"back_num\":\"50\",\"next_page\":\"0\",\"record\":[{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585100\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"ECDCBAB4C39783EE650278D7A3036A2B\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585101\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"75915.07\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"A35CDF476C45E4EE6C7F1AC562B21DCF\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585102\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"3.75\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"BBBA6BD5B0758BDF0AC942E4089722DD\",\"remark\":\"下拨#78zb6j4#\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585103\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"9500.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"D0421EA8264636FF3943A70D72BEC751\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585104\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.45\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"55DEEA299977CCFA241CB844E058B075\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585105\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"10000.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"1D064653F62E30B6FE60EDDB77F3D922\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585106\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"968E341C2CDAF4407DC9754D99123572\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585107\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"13300.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"8B1AC5CF000C2477C288C26EB2485B51\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585108\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.65\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"367CC313329E7EB5361DFABD7259A68E\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585109\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"31350.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"72D3EB5F695E9E3B29E2B37A1DA06814\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585110\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"1.55\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"060A25C7D8B8BAA1840F694694E3C892\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585111\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"18000.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"41E8D870148266A7EE3CC4630FFCDC9F\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585112\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.90\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"AC7B63F9A8E678A1C5A33010CC7F6CDE\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585113\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"10687.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"B00CFB144B47D6CDD5DF88A989081C18\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585114\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"1DAEB05D0FDEA1EC4A68FDE5E3CF0EB4\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585115\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"1012.19\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"EC630A72A09F1FB7E6156B312E777642\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585116\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.05\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"FA1EB2ACE8F9418608E029E426225A76\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585117\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"12500.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"9B01BFDE82F249AEAADDC8619573537F\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585118\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.60\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"FA216F10FE18AA54D3EDA4B1A353719B\"},{\"acct_bal\":\"1891628.41\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006978585119\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"4710.48\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"DFBC5F9B3D632AC6840E5ED843C20650\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813100\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.20\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"E6D211A96CED0233CE2C6519ECF16298\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813101\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"9614.35\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"F0904038ABDC7E7DBF2E572166465345\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813102\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.45\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"018833A3208FC48B6783A9B831040279\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813103\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"230.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"617037EA3AC590FB556A532F8DA7BCD2\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813104\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"55990.60\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"9E4FB63C9D5DE6E484C3A007188DBA64\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813105\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.75\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"90B281B25A0F71F466B3FC057A2F8E41\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813106\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"0311309EDABA2DF40EDAB5BB6DBB9EFE\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813107\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"509.17\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"E46CFFC45A7669373CA4EF0A3047D539\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813108\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"A3AE4FA54FC2256724F2F9167A223534\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813109\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2870.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"ED35AD506DB0753FAB8EC9DF5F3A8106\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813110\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.10\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"53860979A6EFA3A1F885C1DF8B8F5F55\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813111\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"16.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"5D52C65D16165CFF0610651D1790992C\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813112\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"10535.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"2E28B0EFAF892F5F2D14A6CBE61E4D9D\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813113\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"97184F504A79DAE85DE5CF947C02A1F1\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813114\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"9436.84\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"B992460675D9A178576E4194B5A54573\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813115\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.45\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"CB67CC63CCD90AAA75ADEBC6183C11A2\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813116\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"F71764D01BE7DC4777595972E6171014\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813117\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"7980.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"964782DAF6C76D61D7E319FB40DFEB83\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813118\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.35\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"A0E7E969E9C861B75D71A3DD1F8995A7\"},{\"acct_bal\":\"1794433.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006975813119\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"DAF336C37FF186AD7DCD83B17C1508D5\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052100\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"828BA3D4A14C8ACBDFD08114C42967A8\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052101\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"C71E3E2CC87DF1C67318A897559E1E76\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052102\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"16.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"292FAE3CEE8E33DBEEE237005EE5F540\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052103\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"16.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"D331FEDC8F4D7EC9969BD3FC1A1CA6DD\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052104\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"99ABB726C866B987AAD123E1D8E601A7\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052105\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"2.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"7FDA8D907AFB39238CE67B2696667A2C\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052106\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"11328.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"B9CE89286801860632BBC89BFA24E9D8\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052107\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"0.55\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"18052CD19E32A9292DEC4FB05E852903\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052108\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"819.00\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"F3F99DFC0A0AEF3BF390820D76A52BA5\"},{\"acct_bal\":\"1776446.15\",\"bank_check_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_reconciliation_code\":\"6737903247961A5814AB713E59C0A75F\",\"bank_seq_no\":\"PMGW20240805000006983052109\",\"curr_code\":\"PEN\",\"dc_flag\":\"d\",\"detail_check_id\":\"6737903247961A5814AB713E59C0A75F\",\"tran_amt\":\"16.50\",\"tran_date\":\"20240803\",\"tran_time\":\"000000\",\"unique_no\":\"C175EA495CF57D2778623ECD6D6EAFEC\"}],\"tot_num\":\"254\"},\"response_head\":{\"service_busi_date\":\"20241018\",\"service_finish_time\":\"20241018125643498\",\"service_recv_time\":\"20241018125643377\",\"service_resp_code\":\"000000\",\"service_resp_desc\":\"成功\",\"service_seq_no\":\"RC124850000202410181254476152519\",\"service_status\":\"00\"}},\"message\":\"银企联云交易成功\"}";
//            result = CtmJSONObject.parseObject(testJson);
//        }
        long endtime = System.currentTimeMillis();
        String nextPage = null;
        YQLDataAccessModel dataAccessModel = new YQLDataAccessModel();
        dataAccessModel.setResponse(result.toString());
        //TODO:提前过滤异常数据
        dataAccessModel.setOperType("1");
        dataAccessModel.setRequestParam(requestData.get(0).getValue());
        dataAccessModel.setUsedTime(Double.valueOf((endtime - starttime) / 1000)+"");
        //币种id
        dataAccessModel.setCurrencyId(directAccountRequestInfo.getCurrency());
        //账户所属组织id
        dataAccessModel.setOrgId(directAccountRequestInfo.getAccEntityId());
        //银行账户id
        dataAccessModel.setBankaccountId(directAccountRequestInfo.getBankaccountId());
        //调用智能流水功能
        process.dealDetailAccessByYQL(dataAccessModel);
        int recursionCount = 1;
        if(recursionCount > 1000){
            return nextPage;
        }
        if (result.getInteger("code") == SUCCESS_CODE) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String service_resp_code = responseHead.getString("service_resp_code");
            if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, service_resp_code)) {
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                nextPage = responseBody.getString("next_page");
                if (HAS_NEXT_PAGE.equals(nextPage)) {
                    directAccountRequestInfo.setBegNum(directAccountRequestInfo.getBegNum() + ITransCodeConstant.QUERY_NUMBER_50);
                    directAccountRequestInfo.setQueryExtend(responseBody.get("query_extend"));
                    recursionCount +=1;
                    nextPage = this.pullData(directAccountRequestInfo);
                }
            }else {
                nextPage = "0";
            }
        }
        return nextPage;
    }


    /**
     * @param commondInfo
     * @return
     * @throws Exception
     */
    private List<DirectAccountRequestInfo> initRequestBankAccountInfo(PullCommandInfo commondInfo) throws Exception {
        BankAccountUtil.refreshEnableDateByEnterpriseBankAcctVOs(commondInfo.getBankAcctVOList());
        List<EnterpriseBankAcctVO> checkSuccess = commondInfo.getBankAcctVOList();
        //将直连账户和币种、日期拼接
        List<DirectAccountRequestInfo> requestBankAccountInfoList = new ArrayList<>();
//        String customNo = AppContext.cache().get(InvocationInfoProxy.getTenantid() + "customNo");
//        if (StringUtils.isEmpty(customNo)) {
//            customNo = bankAccountSettingService.getCustomNoByBankAccountId(checkSuccess.get(0).getId());
//        }
        String channel = AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel();
        for (EnterpriseBankAcctVO evo : checkSuccess) {
            String customNo = bankAccountSettingService.getCustomNoByBankAccountId(evo.getId());
            String startDate = commondInfo.getStartDate();
            String endDate = commondInfo.getEndDate();
            StringBuilder changedStartDate = new StringBuilder(startDate);
            if (TaskUtils.changeStartDateByEnableDateAndCheckIfSkip(evo, changedStartDate, endDate)) {
                continue;
            }
            startDate = changedStartDate.toString();
            for (BankAcctCurrencyVO currencyVO : evo.getCurrencyList()) {
                DirectAccountRequestInfo info = new DirectAccountRequestInfo();
                List<BankAcctCurrencyVO> getCurrencyList = new ArrayList<>();
                getCurrencyList.add(currencyVO);
                info.setBankaccountId(evo.getId());
                info.setCurrencyCode(bankDealDetailService.queryCurrencyCode(getCurrencyList).get(currencyVO.getCurrency()));
                info.setAccEntityId(evo.getOrgid());
                info.setBanktype(evo.getBank());
                info.setAcct_name(evo.getAcctName());
                info.setAcct_no(evo.getAccount());
                info.setStartDate(startDate);
                info.setEndDate(endDate);
                info.setCustomNo(customNo);
                info.setLineNumber(evo.getLineNumber());
                info.setBegNum(1);
                info.setChannel(channel);
                info.setCurrency(currencyVO.getId());
                requestBankAccountInfoList.add(info);
            }
        }
        return requestBankAccountInfoList;
    }


    /**
     * 格式化请求数据
     *
     * @return
     * @throws Exception
     */
    private List<BasicNameValuePair> formatRequestData(DirectAccountRequestInfo directAccountRequestInfo) throws Exception {
        CtmJSONObject queryMsg = buildQueryTransactionDetailMsg(directAccountRequestInfo);
        String signMsg = AppContext.getBean(BankConnectionAdapterContext.class).chanPaySignMessage(queryMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        return requestData;
    }


    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author jiangpengk
     * @Description 构建查询账户交易明细报文
     * @Date 2023/6/7
     **/
    public CtmJSONObject buildQueryTransactionDetailMsg(DirectAccountRequestInfo directAccountRequestInfo) throws Exception {
        //固定数据从交易信息里面取，其他动态字段需要实时判断
        String customNo = directAccountRequestInfo.getCustomNo();
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", buildRequestSeqNo(customNo));
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", directAccountRequestInfo.getChannel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", directAccountRequestInfo.getOperator() == null ? "" : directAccountRequestInfo.getOperator());
        requestHead.put("oper_sign", directAccountRequestInfo.getSignature());
        requestHead.put("tran_code", ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL);

        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_no", directAccountRequestInfo.getAcct_no());
        requestBody.put("acct_name", directAccountRequestInfo.getAcct_name());
        requestBody.put("curr_code", directAccountRequestInfo.getCurrencyCode());
        requestBody.put("query_extend", directAccountRequestInfo.getQueryExtend());
        requestBody.put("beg_date", directAccountRequestInfo.getStartDate());
        requestBody.put("end_date", directAccountRequestInfo.getEndDate());
        requestBody.put("tran_status", "00");
        requestBody.put("beg_num", directAccountRequestInfo.getBegNum());
        requestBody.put("query_num", ITransCodeConstant.QUERY_NUMBER_50);
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    /**
     * 构建线程池
     *
     * @param corePoolSize
     * @return
     */
    private ExecutorService buildThreadPoolForTaskBankDeail(Integer corePoolSize) {
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.bankdetailTask.thread.param","8,128,1000,cmp-bankdetailTask-async-");
        String[] threadParamArray = threadParam.split(",");
        if (corePoolSize == null || corePoolSize == 0) {
            corePoolSize = Integer.parseInt(threadParamArray[0]);;
        }
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix = threadParamArray[3];
        ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
        coreSizeMap.put(InvocationInfoProxy.getTenantid(), corePoolSize);
        return executorService;
    }
    static Map<String, Integer> coreSizeMap = new HashMap<>();

}
