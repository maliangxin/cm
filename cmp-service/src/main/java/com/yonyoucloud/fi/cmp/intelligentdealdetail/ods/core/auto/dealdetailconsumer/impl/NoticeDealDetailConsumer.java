package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.IBankDealDetailBusiOper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.NoticeBankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
/**
 * @Author guoyangy
 * @Date 2024/7/24 11:21
 * @Description todo
 * @Version 1.0
 */
@Slf4j
@Service
public class NoticeDealDetailConsumer extends DefaultDealDetailConsumer{
    @Resource
    private IBankDealDetailBusiOper bankDealDetailBusiOper;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    /**
     * 银行账号对应的主键和会计主体缓存
     */
    private static final com.github.benmanes.caffeine.cache.Cache<String, String> accountCache = Caffeine.newBuilder()
            .initialCapacity(100)//初始数量
            .maximumSize(10000)//最大条数
            .expireAfterWrite(Duration.ofMinutes(30))//最后一次写操作后经过指定时间过期
            .softValues()
            .build();

    @Override
    public Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankReconciliations) {
        Map<String, List<BankReconciliation>> resultMap = bankDealDetailBusiOper.executeDedulication(bankReconciliations);
        log.info("【业务去重】步骤二:从流水业务库查询流水数据做业务去重,去重完成");
        return resultMap;
    }
    @Override
    public void callBankDetailManageAccess(List<BankDealDetailWrapper> bankDealDetailProcessList, List<BankReconciliation> updateBankReconciliationList, String traceid, String requestSeqNo) {
        long start = System.currentTimeMillis();
        bankDealDetailManageAccess.bankDealDetailManageAccess(bankDealDetailProcessList,updateBankReconciliationList, NoticeBankDealDetailMatchChainImpl.get(),traceid,requestSeqNo);
        log.info("【消费端调流水管理器】流水管理器处理完成耗时{}ms",System.currentTimeMillis()-start);
    }

    @Override
    protected List<BankReconciliation> batchConvertODSToBankReconciliation(List<BankDealDetailODSModel> bankDealDetailResponseRecords) {
        //step1:非空校验
        if(CollectionUtils.isEmpty(bankDealDetailResponseRecords)){
            return null;
        }
        Map<String,BankDealDetailODSModel> bankDealDetailODSModelMap = new HashMap<>();
        for(BankDealDetailODSModel bankDealDetailODSModel:bankDealDetailResponseRecords){
            String odsId = bankDealDetailODSModel.getId();
            bankDealDetailODSModelMap.put(odsId,bankDealDetailODSModel);
        }
        try{
            List<BankReconciliation> bankReconciliations = bankdealDetailOdsConvertManager.convertOdsTOBankReconciliation(bankDealDetailResponseRecords);
            for(BankReconciliation bankReconciliation : bankReconciliations){
                String odsId = bankReconciliation.get(DealDetailEnumConst.ODSID);
                BankDealDetailODSModel bankDealDetailODSModel = bankDealDetailODSModelMap.get(odsId);
                // 银行账户
                String acctNo = bankDealDetailODSModel == null ? StringUtils.EMPTY : bankDealDetailODSModel.getAcct_no();
                String acctIdAndOrgID = this.getBankAcctByAccount(acctNo);
                String[] acctIdAndOrgIDs = acctIdAndOrgID.split(",");
                bankReconciliation.setBankaccount(acctIdAndOrgIDs[0]);
                // 所属组织
                bankReconciliation.setOrgid(acctIdAndOrgIDs[1]);
                bankReconciliation.setAccentity(acctIdAndOrgIDs[1]);
                if(!Objects.isNull(bankReconciliation.getAccentity())){
                    bankReconciliation.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
                }
                // 银行类别
                bankReconciliation.setBanktype(acctIdAndOrgIDs[2]);
                // 交易日期
                String tran_date = bankDealDetailODSModel == null ? StringUtils.EMPTY : bankDealDetailODSModel.getTran_date();
                if(org.apache.commons.lang3.StringUtils.isNotEmpty(tran_date)){
                    Date tranDate = DateUtils.dateParse(tran_date, DateUtils.YYYYMMDD);
                    bankReconciliation.setDzdate(tranDate);
                }
                Short dc_flag = bankDealDetailODSModel == null ? null : bankDealDetailODSModel.getDc_flag();
                if(null!=dc_flag && Direction.Debit.getValue()==dc_flag){
                    //支出赋值借方金额
                    bankReconciliation.setDebitamount(bankDealDetailODSModel.getTran_amt());
                }
                if(null!=dc_flag && Direction.Credit.getValue()==dc_flag){
                    //收入赋值贷方金额
                    bankReconciliation.setCreditamount(bankDealDetailODSModel.getTran_amt());
                }
                bankReconciliation.setDataOrigin(DateOrigin.DownFromYQL);
                bankReconciliation.setBankcheckno(bankDealDetailODSModel == null ? null : bankDealDetailODSModel.getBank_check_code());
            }
            log.error("【流水消费】5.已将ods实体转成银行对账单实体");
            return bankReconciliations;
        }catch (Exception e){
            log.error("【流水消费】5.ods转银行对账单实体异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005FE", "ods转银行对账单实体异常，异常原因：") /* "ods转银行对账单实体异常，异常原因：" */+e.getMessage(),e);
        }
    }

    /**
     * 根据银行账号，获取银行账户id和会计主体信息
     * @param acct_no
     * @return
     * @throws Exception
     */
    private String getBankAcctByAccount(String acct_no) throws Exception {
        try {
            String cacheData = accountCache.getIfPresent(acct_no);
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(cacheData)) {
                return cacheData;
            }
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setAccount(acct_no);
            List<EnterpriseBankAcctVO> enterpriseBankAccounts = enterpriseBankQueryService.query(enterpriseParams);
            if (enterpriseBankAccounts.size() < 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101818"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001263400", "未查询到对应的企业银行账户，保存失败！") /* "未查询到对应的企业银行账户，保存失败！" */);
            }
            String bankAcct = enterpriseBankAccounts.get(0).getId();
            String orgId = enterpriseBankAccounts.get(0).getOrgid();
            String bank = enterpriseBankAccounts.get(0).getBank();
            accountCache.put(acct_no, bankAcct + "," + orgId + "," + bank);
            return bankAcct + "," + orgId + "," + bank;
        }catch (Exception e){
            log.error("【流水消费】5.ods转银行对账单实体异常，获取银行账户信息异常",e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005FD", "ods转银行对账单实体异常，获取银行账户信息异常，异常原因：") /* "ods转银行对账单实体异常，获取银行账户信息异常，异常原因：" */+e.getMessage(),e);
        }
    }
}
