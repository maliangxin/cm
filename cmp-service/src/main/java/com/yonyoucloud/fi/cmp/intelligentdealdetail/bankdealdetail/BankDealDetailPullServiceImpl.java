package com.yonyoucloud.fi.cmp.intelligentdealdetail.bankdealdetail;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.model.PullCommandInfo;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.IBankAccountDataPullService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.command.BankAccountDataPullCommand;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 *
 */
@Service
@Slf4j
public class BankDealDetailPullServiceImpl implements IBankAccountDataPullService {

    @Autowired
    BankAccountDataPullCommand bankAccountDataPullCommand;

    @Autowired
    BankAccountSettingService bankAccountSettingService;

    @Autowired
    BankDealDetailService bankDealDetailService;

    @Override
    public void pullData(CtmJSONObject param) {
        String logId = param.getString("logId");
        try {
            checkParam(param);
            CtmJSONObject queryBankAccountVosParams = buildQueryBankAccountVosParams(param);
            queryBankAccountVosParams.put(ICmpConstant.IS_DISPATCH_TASK_CMP, param.get(ICmpConstant.IS_DISPATCH_TASK_CMP));
            //根据参数查询全量账户
            List<EnterpriseBankAcctVO> bankAccounts = bankDealDetailService.getEnterpriseBankAccountVos(queryBankAccountVosParams);
            //通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
            Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = bankDealDetailService.getBankAcctVOsGroupByTask(bankAccounts);
            //拉取内部账户的数据
//            pullInnerAccountData(bankAccountsGroup.get("innerAccounts"),param);
            //拉取直连账户的交易明细
            pulldirectedAccountData(bankAccountsGroup.get("checkSuccess"), param);
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        } catch (Exception e) {
            log.error("查询账户交易明细失败：{}", e);
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100895"), e.getMessage());
        }
    }

    public static CtmJSONObject buildQueryBankAccountVosParams(CtmJSONObject param) {

        //根据调度任务参数条件查询对应的账户信息
        String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
        String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
        String bankaccounts = (String) (Optional.ofNullable(param.get("bankaccount")).orElse(""));
        //线程并发数量
        Integer corepoolsize = 1;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(param.getString("corepoolsize"))) {
            corepoolsize = Integer.valueOf(param.get("corepoolsize").toString());
        }
        param.put("corepoolsize", corepoolsize);
        String[] accentityArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        String[] banktypeArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
        }
        String[] currencyArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
        }
        String[] bankaccountArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankaccounts)) {
            bankaccountArr = bankaccounts.split(";");
        }

        CtmJSONObject queryBankAccountVosParams = new CtmJSONObject();
        queryBankAccountVosParams.put("accEntity", accentityArr);
        queryBankAccountVosParams.put("accountId", bankaccountArr);
        queryBankAccountVosParams.put("bankType", banktypeArr);
        queryBankAccountVosParams.put("currency", currencyArr);
        queryBankAccountVosParams.put("corepoolsize", corepoolsize);
        return queryBankAccountVosParams;
    }

    /**
     * 检查调度任务的参数
     *
     * @param param
     */
    @Override
    public void checkParam(CtmJSONObject param) throws Exception {
        String startDate = null;
        String endDate = null;
        if (StringUtils.isNotEmpty(param.getString("startDate")) && StringUtils.isNotEmpty(param.getString("endDate"))){
            startDate = param.getString("startDate");
            endDate = param.getString("endDate");
        }else if (StringUtils.isNotEmpty(param.getString("advanceDate"))){
            int advanceDate = param.getInteger("advanceDate");
            startDate = DateUtils.dateFormat(DateUtils.dateAddDays(new Date(), -advanceDate), DateUtils.DATE_PATTERN);
            param.put("startDate", startDate);
            endDate = DateUtils.dateFormat(DateUtils.dateAddDays(new Date(), 0), DateUtils.DATE_PATTERN);
            param.put("endDate", endDate);
        }else{
            //当客户两个都没有填写值的时候默认3天
            int advanceDate = 1;
            startDate = DateUtils.dateFormat(DateUtils.dateAddDays(new Date(), -advanceDate), DateUtils.DATE_PATTERN);
            param.put("startDate", startDate);
            endDate = DateUtils.dateFormat(DateUtils.dateAddDays(new Date(), 0), DateUtils.DATE_PATTERN);
            param.put("endDate", endDate);
        }
        if ((StringUtils.isEmpty(startDate) && StringUtils.isNotEmpty(endDate)) || (StringUtils.isNotEmpty(startDate) && StringUtils.isEmpty(endDate))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400442", "开始日期、结束日期要不同时为空，要不同时有值，请检查!") /* "开始日期、结束日期要不同时为空，要不同时有值，请检查!" */);
        }
        int dateBetween = DateUtils.dateBetween(startDate, endDate);
        if (dateBetween > 30) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100897"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800CB", "银行账户交易明细查询时间跨度不能超过30天，请缩小查询范围！") /* "银行账户交易明细查询时间跨度不能超过30天，请缩小查询范围！" */);
        }
    }

    /**
     * 拉取直连账户的数据
     *
     * @param directedAccountList
     */
    private void pulldirectedAccountData(List<EnterpriseBankAcctVO> directedAccountList, CtmJSONObject param) throws Exception {
        if (CollectionUtils.isNotEmpty(directedAccountList)) {
            PullCommandInfo commondInfo = new PullCommandInfo();
            commondInfo.setBankAcctVOList(directedAccountList);
            String startDate = (String) param.get("startDate");
            String endDate = (String) param.get("endDate");
            commondInfo.setStartDate(startDate.replaceAll("-", ""));
            commondInfo.setEndDate(endDate.replaceAll("-", ""));
            commondInfo.setTranCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL);
            int corepoolsize = param.get("corepoolsize") == null ? 5 : param.getInteger("corepoolsize");
            commondInfo.setCorepoolsize(corepoolsize);
            bankAccountDataPullCommand.execute(commondInfo);
        }
    }

    /**
     * 拉取内部账户的数据
     *
     * @param innerAccountList
     */
    private void pullInnerAccountData(List<EnterpriseBankAcctVO> innerAccountList, CtmJSONObject param) throws Exception {
        if (CollectionUtils.isNotEmpty(innerAccountList)) {
            PullCommandInfo transInfo = new PullCommandInfo();
            transInfo.setTranCode(ITransCodeConstant.QUERY_INNER_ACCOUNT_TRANSACTION_DETAIL);
            bankAccountDataPullCommand.execute(transInfo);
        }
    }


}
