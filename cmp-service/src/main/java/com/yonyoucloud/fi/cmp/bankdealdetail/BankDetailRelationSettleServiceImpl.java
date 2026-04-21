package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.reqvo.QuerySettleDeatailReqVO;
import com.yonyoucloud.ctm.stwb.respvo.ResultSettleDetailRespVO;
import com.yonyoucloud.ctm.stwb.respvo.SettleDetailRespVO;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrDataProcessingServiceImpl;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.autocorrsettings.Autocorrsetting;
import com.yonyoucloud.fi.cmp.autocorrsettings.BussDocumentType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 银行交易明细拉取后关联结算实现
 * @author wq
 * @version 1.0.0
 * @date 2023/11/23 15:52
 */
@Service
@Slf4j
public class BankDetailRelationSettleServiceImpl implements BankDetailRelationSettleService{
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Autowired
    private CorrDataProcessingServiceImpl corrDataProcessingServiceImpl;
    @Autowired
    CorrOperationService corrOperationService;//写入关联关系
    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    /**
     * 银行对账单关联结算
     * @param bankRecords
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    @Override
    public CtmJSONObject detailRelationSettle(List<BankReconciliation> bankRecords) throws Exception {
        CtmJSONObject ctmJSONObject= new CtmJSONObject();
        ExecutorService autoAssociatedDataExecutor = null;
        try {
            log.error("内部交易明细拉取完成后独立事务异步关联结算bankRecords"+CtmJSONObject.toJSONString(bankRecords));
            if(CollectionUtils.isEmpty(bankRecords)){
                return ctmJSONObject;
            }
            List<CorrDataEntity> resList = new ArrayList<CorrDataEntity>();
            //租户级自动关联规则 - 贷的规则
            List<Autocorrsetting> acListCredit = corrDataProcessingServiceImpl.getAutoCorrSettingCredit(bankRecords.get(0));
            //租户级自动关联规则 - 借的规则
            List<Autocorrsetting> acListDebit = corrDataProcessingServiceImpl.getAutoCorrSettingDebit(bankRecords.get(0));
            autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(10, 10, 10000, "detailRelationSettle");
            ExecutorService finalAutoAssociatedDataExecutor = autoAssociatedDataExecutor;
            List<? extends Future<?>> futures = bankRecords.stream().map(bankReconciliation ->{
                return finalAutoAssociatedDataExecutor.submit(()->{
                    //循环匹配对账单
                    List<Autocorrsetting> acList = bankReconciliation.getDc_flag().getValue() == 1 ? acListDebit : acListCredit;
                    for (int j = 0; j < acList.size(); j++) {
                        Autocorrsetting autocorrsetting = acList.get(j);
                        //关联资金结算单
                        if (BussDocumentType.settlebench.getValue() == autocorrsetting.getBusDocumentType()) {
                            QuerySettleDeatailReqVO querySettleDeatailReqVO = dealQuerySettleDeatailReqVO(bankReconciliation, autocorrsetting);
                            try {
                                //调用资金结算接口 -- 匹配资金结算单
                                ResultSettleDetailRespVO resJson = RemoteDubbo.get(SettleBenchBRPCService.class, IDomainConstant.MDD_DOMAIN_STWB).newQuerySettleBench(querySettleDeatailReqVO);
                                SettleDetailRespVO data = resJson.getData();
                                if (data!=null && data.getMainid()!=null) {//匹配到结算单
                                    CorrDataEntity entity = new CorrDataEntity();
                                    //将数据存入可关联实体
                                    entity = corrDataProcessingServiceImpl.setCorrDataFromJson(entity, bankReconciliation, data);
                                    resList.add(entity);
                                    break;
                                }
                            }catch (Exception e){
                                log.error("关联结算调用SettleBenchBRPCService方法报错--bankReconciliation:"+CtmJSONObject.toJSONString(bankReconciliation));
                                log.error("关联结算调用SettleBenchBRPCService方法报错--querySettleDeatailReqVO:"+CtmJSONObject.toJSONString(querySettleDeatailReqVO));
                                log.error("关联结算调用SettleBenchBRPCService方法报错"+e);
                                log.error("关联结算调用SettleBenchBRPCService方法报错"+e.getMessage());
                            }
                        }
                    }
                });
            }).collect(Collectors.toList());
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("内部交易明细拉取完成后独立事务异步关联结算等待异步线程返回报错", e);
                }
            });
            if(resList.size()==0){//如果没有一个关联结算直接返回
                return null;
            }
            //根据结算返回的关联信息回写银行对账单子表关联信息，回写结算单据关联状态
            final int[] ordernum = {1};
            resList.stream().forEach(corrDataEntity -> {
                int finalOrdernum = ordernum[0];
                executorServicePool.getThreadPoolExecutor().submit(() -> {
                    try {
                        //写入关联表
                        corrOperationService.corrOpration(corrDataEntity, finalOrdernum);
                        //回写银行对账单
                        reWriteBusCorrDataService.reWriteBankReconciliationData(corrDataEntity, true);
                    } catch (Exception e) {
                        log.error("自动关联出错corrDataEntity:"+CtmJSONObject.toJSONString(corrDataEntity));
                        log.error("自动关联出错"+e);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101413"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B5", "执行失败") /* "执行失败" */);
                    }
                });
                ordernum[0]++;
            });
        }finally {
            if(autoAssociatedDataExecutor !=null){
                autoAssociatedDataExecutor.shutdown();
            }
        }

        return ctmJSONObject;
    }

    /**
     * 根据结算关联规则组装结算接口的QuerySettleDeatailReqVO
     * @param bankReconciliation
     * @param autocorrsetting
     */
    private QuerySettleDeatailReqVO dealQuerySettleDeatailReqVO(BankReconciliation bankReconciliation, Autocorrsetting autocorrsetting) {
        QuerySettleDeatailReqVO json = new QuerySettleDeatailReqVO();
        json.setBankcheckno(bankReconciliation.getBankcheckno());
        json.setBankaccount(bankReconciliation.getBankaccount());
        // 0 无  1 相同
        if (autocorrsetting.getOthBankNum() == 0){
            json.setOthbanknumname(bankReconciliation.getTo_acct_name());
        } else if (autocorrsetting.getOthBankNumName() == 0){
            json.setTo_acct_no(bankReconciliation.getTo_acct_no());
        } else {
            json.setOthbanknumname(bankReconciliation.getTo_acct_name());
            json.setTo_acct_no(bankReconciliation.getTo_acct_no());
        }
        json.setTran_amt(bankReconciliation.getTran_amt().toString());
        json.setRemark(bankReconciliation.getRemark());
        json.setIsfuzzmatch(autocorrsetting.getBillabstract().toString());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        //Date date = new Date();
        if (autocorrsetting.getFloatDays() != null) {
            int floadaysadd = autocorrsetting.getFloatDays().intValue();
            int floadaysred = floadaysadd * (-1);
            String startdate = format.format(DateUtils.dateAddDays(bankReconciliation.getTran_date(), floadaysred));
            String enddate = format.format(DateUtils.dateAddDays(bankReconciliation.getTran_date(), floadaysadd));
            json.setStartdate(startdate);
            json.setEnddate(enddate);
        }
        String stwbDirection = autocorrsetting.getDirection() == Direction.Debit.getValue() ? String.valueOf(Direction.Credit.getValue()) : String.valueOf(Direction.Debit.getValue());
        json.setRecpaytype(stwbDirection);
        json.setAccentity(bankReconciliation.getAccentity());
        return json;
    }
}
