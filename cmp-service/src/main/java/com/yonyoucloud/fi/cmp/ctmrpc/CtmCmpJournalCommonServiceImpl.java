package com.yonyoucloud.fi.cmp.ctmrpc;


import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.journal.CtmCmpJournalCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.rpc.rule.DailyComputezInit;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据来源单据id和来源单据类型更新对应的日记账的勾兑号
 */
@Service
@Slf4j
public class CtmCmpJournalCommonServiceImpl implements CtmCmpJournalCommonService {
    //来源单据id
    private static final String SRCITMEID = "srcitmeid";
    private static final String JOURNAL = "journal";

    private static final String OPERATETYPE = "operateType";
    private static final String MAPPERNAME_UPDATEVOUCHER = "com.yonyoucloud.fi.cmp.mapper.JournalMapper.updateVoucher";
    private static final String MAPPERNAME_SELECTBYSRCBILLITEMID = "com.yonyoucloud.fi.cmp.mapper.JournalMapper.selectBySrcbillitemid";

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 根据来源单据id和来源单据类型更新对应的日记账的勾兑号
     */
    @Override
    public void updateJournal(CommonRequestDataVo commonQueryData) throws Exception {
        try {
            String operateType = commonQueryData.getOperateType(); //操作类型
            log.error("CtmCmpJournalCommonServiceImpl updateJournalCheckno , request param is {},operateType is {}", commonQueryData.toString(), operateType);
            commonQueryData.setYtenantId(AppContext.getYTenantId());
            Journal dbJournal = SqlHelper.selectOne(MAPPERNAME_SELECTBYSRCBILLITEMID, commonQueryData);
            if ("updateVoucher".equals(operateType)) {//更新凭证信息
                SqlHelper.update(MAPPERNAME_UPDATEVOUCHER, commonQueryData);
            } else {
//                if (dbJournal != null) {
//                    CtmJSONObject logParams = new CtmJSONObject();
//                    logParams.put("dbJournal", dbJournal);
//                    logParams.put("commonQueryData", commonQueryData);
//                    ctmcmpBusinessLogService.saveBusinessLog(logParams, dbJournal.getSrcbillno(), "银行日记账", IServicecodeConstant.BANKJOURNAL, IMsgConstant.BANK_JOURNAL, "银行日记账更新勾对号");
//                    SqlHelper.update(MAPPERNAME_UPDATECHECKNO, commonQueryData);
//                }
            }
            //设置来源单据号至事务日志里
            YtsContext.setYtsContext(SRCITMEID, commonQueryData.getSrcitmeid());
            YtsContext.setYtsContext(OPERATETYPE, commonQueryData.getOperateType());
            YtsContext.setYtsContext(JOURNAL, dbJournal);
        } catch (Exception e) {
            log.error("updateJournal error{}", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100957"), e.getMessage());
        }
    }

    /**
     * 更新日记账勾兑号回滚逻辑
     */
    @Override
    public void updateJournalRollback(CommonRequestDataVo data) throws Exception {
        try {
            YtsContext.getYtsContext(SRCITMEID);
            String srcitmeid = (String) YtsContext.getYtsContext(SRCITMEID);
            String operateType = (String) YtsContext.getYtsContext(OPERATETYPE);

            log.info("CtmCmpJournalCommonServiceImpl updateJournalUpdateRollback , srcitmeid is {},operateType is {}", srcitmeid, operateType);
            CommonRequestDataVo commonQueryData = new CommonRequestDataVo();
            commonQueryData.setYtenantId(AppContext.getYTenantId());
            commonQueryData.setSrcitmeid(srcitmeid);
            Journal journal = (Journal) YtsContext.getYtsContext(JOURNAL);
            if (journal != null) {
                if ("updateVoucher".equals(operateType)) {
                    commonQueryData.setVoucherNo(journal.getVoucherNo());
                    commonQueryData.setVoucherPeriod(journal.getVoucherPeriod());
                    SqlHelper.update(MAPPERNAME_UPDATEVOUCHER, commonQueryData);
                } else {
//                    commonQueryData.setCheckno(journal.getCheckno());
//                    //从事务日志里取来源单据号
//                    SqlHelper.update(MAPPERNAME_UPDATECHECKNO, commonQueryData);
                }
            }
        } catch (Exception e) {
            log.error("updateJournalRollback error:{}", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100958"), e.getMessage());
        }
    }

    @Override
    public CommonResponseDataVo queryJournalBalanceSettled(List<String> bankaccountList, List<String> keyList) throws Exception {
        //获取余额信息
        Map<String, SettlementDetail> settlementDetailMap =
                DailyComputezInit.imitateDailyComputeInit(null, null, null, bankaccountList.toString().substring(1, bankaccountList.toString().length() - 1).replaceAll(" ", ""), "2", "2", null);
        //获取余额信息
        Map<String, SettlementDetail> settlementDetailAvlMap =
                DailyComputezInit.imitateDailyComputeInit(null, null, null, bankaccountList.toString().substring(1, bankaccountList.toString().length() - 1).replaceAll(" ", ""), null, null, null);
        //封装返回信息
        CommonResponseDataVo resultVo = new CommonResponseDataVo();
        List<HashMap<String, Object>> resultList = new ArrayList();
        try {
            if (settlementDetailMap.values() != null) {
                for (String key : keyList) {

                    SettlementDetail settlementDetail = settlementDetailMap.get(key);
                    SettlementDetail settlementDetailAvl = settlementDetailAvlMap.get(key);
                    HashMap<String, Object> resultMap = new HashMap();
                    //如果不存在数据 说明没有余额
                    if (settlementDetail == null) {
                        String currencyid = "";
                        String bankaccountid = "";
                        for (String bankaccount : bankaccountList) {
                            if (key.contains(bankaccount)) {
                                //从当前key中把bankaccount截掉 剩下的就是币种id
                                currencyid = key.split(bankaccount)[1];
                                bankaccountid = bankaccount;
                                break;
                            }
                        }
                        resultMap.put(ICmpConstant.MESSAGE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079A", "不存在日记账余额信息") /* "不存在日记账余额信息" */);
                        resultMap.put(ICmpConstant.CODE, ICmpConstant.REQUEST_MISSING_STATUS_CODE);
                        resultMap.put(ICmpConstant.BANKACCOUNT, bankaccountid);
                        resultMap.put(ICmpConstant.CURRENCY, currencyid);
                    } else {
                        resultMap.put(ICmpConstant.CODE, ICmpConstant.REQUEST_SUCCESS_STATUS_CODE);
                        resultMap.put(ICmpConstant.MESSAGE, "");
                        resultMap.put(ICmpConstant.BANKACCOUNT, settlementDetail.getBankaccount());
                        resultMap.put(ICmpConstant.CURRENCY, settlementDetail.getCurrency());
                        //原币
                        resultMap.put(ICmpConstant.ORI_BALANCE, settlementDetail.getTodayorimoney());
                        //本币
                        resultMap.put(ICmpConstant.LOCAL_BALANCE, settlementDetail.getTodaylocalmoney());
                        // 可用原币余额
                        resultMap.put(ICmpConstant.ORI_AVAILABLE_BALANCE, settlementDetailAvl.getTodayorimoney());
                        // 可用本币余额
                        resultMap.put(ICmpConstant.LOCAL_AVAILABLE_BALANCE, settlementDetailAvl.getTodaylocalmoney());
                        //接待方向 1借 2贷
                        resultMap.put(ICmpConstant.BILL_DIRECTION,
                                settlementDetail.getTodayorimoney().compareTo(BigDecimal.ZERO) >= 0 ? Direction.Debit.getValue() : Direction.Credit.getValue());
                    }
                    resultList.add(resultMap);
                }
            } else {
                resultVo.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079A", "不存在日记账余额信息") /* "不存在日记账余额信息" */);
            }
            resultVo.setSuccess(true);
            resultVo.setResultList(resultList);
        } catch (Exception e) {
            resultVo.setSuccess(false);
            resultVo.setMessage(e.getMessage());
        }
        return resultVo;
    }

}
