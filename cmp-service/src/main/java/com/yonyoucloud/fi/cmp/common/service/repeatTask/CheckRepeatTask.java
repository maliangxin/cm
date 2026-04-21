//package com.yonyoucloud.fi.cmp.common.service.repeatTask;
//
//
//import com.yonyou.ucf.mdd.ext.core.AppContext;
//import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
//import com.yonyoucloud.fi.cmp.util.DateUtils;
//import lombok.extern.slf4j.Slf4j;
////import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 银行交易明细及银行对账单相关数据疑重校验任务
// *
// * @author maliangn
// */
//@Component
//@Slf4j
//public class CheckRepeatTask {
//
//    final String BANKRECONCILIATIONMAPPER  = "com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper";
//
//    /**
//     *     2分钟执行一次
//     *     设置yms参数控制，目前默认不开启扫描验重
//     */
//
////    @Scheduled(fixedDelay = 2 * 60 * 1000)
////    public void checkRepeak(){
////        try {
////            log.error("开始数据升级任务。。。。。。。。。。。。。");
////            updateBankdealdetail();
////            updateBankreconciliation();
////            log.error("数据升级任务结束。。。。。。。。。。。。。");
////        }catch (Exception e){
////            log.error("开始验重任务失败。。。。。。。。。。。。。",e);
////        }
////
////    }
//
//    /**
//     * 查询银行账户交易明细需升级数据
//     * 根据拼接的concat_info验重
//     */
//    public void updateBankreconciliation() throws Exception{
//        List<Map> list = SqlHelper.selectList(BANKRECONCILIATIONMAPPER + ".queryDataToUpdate");
//        for (Map rs : list){
//            try {
//                String id = rs.get("id").toString();
//                String bankAccount =  (String)rs.get("bankaccount");
//                String tranDate =  DateUtils.dateFormat((Date)rs.get("tran_date"),DateUtils.HOUR_PATTERN);
//                String tranTime = rs.get("tran_time") == null ? "null" : DateUtils.dateFormat((Date)rs.get("tran_time"),DateUtils.HOUR_PATTERN);
//                String tranAmt = rs.get("tran_amt") == null ? "null" :((BigDecimal)rs.get("tran_amt")).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
//                String dcFlag =  rs.get("dc_flag").toString();
//                String bankSeqNo =  rs.get("bank_seq_no") == null ? "null" : (String)rs.get("bank_seq_no");
//                String toAcctNo =  rs.get("to_acct_no") == null ? "null" : (String)rs.get("to_acct_no");
//                String toAcctName = rs.get("to_acct_name") == null ? "null" : (String)rs.get("to_acct_name");
//                StringBuilder resultStrBuilder = new StringBuilder();
//                resultStrBuilder.append(bankAccount).append("|")
//                        .append(tranDate).append("|")
//                        .append(tranTime).append("|")
//                        .append(tranAmt).append("|")
//                        .append(dcFlag).append("|")
//                        .append(bankSeqNo).append("|")
//                        .append(toAcctNo).append("|")
//                        .append(toAcctName);
//                String concat_info = resultStrBuilder.toString();
//                Map param = new HashMap();
//                param.put("concat_info", concat_info);
//                param.put("id", id);
//                log.error("更新银行对账单，id："+id);
//                SqlHelper.update(BANKRECONCILIATIONMAPPER+".updateConcatInfo",param);
//            }catch (Exception e){
//                log.error("更新交易明细失败失败，",e);
//            }
//        }
//    }
//
//
//
//
//
//    /**
//     * 查询银行账户交易明细需升级数据
//     * 根据拼接的concat_info验重
//     */
//    public void updateBankdealdetail() throws Exception{
//        List<Map> list = SqlHelper.selectList(BANKRECONCILIATIONMAPPER + ".queryDetailDataToUpdate");
//        for (Map rs : list){
//            try {
//                String id = rs.get("id").toString(); // 注意：此处两次获取相同的id字段
//                String enterpriseBankAccount = (String) rs.get("enterprisebankaccount");
//                String trandate = DateUtils.dateFormat((Date)rs.get("trandate"),DateUtils.HOUR_PATTERN);
//                String trantime = rs.get("trantime") == null ? "null" : DateUtils.dateFormat((Date)rs.get("trantime"),DateUtils.HOUR_PATTERN);;
//                String tranAmt = rs.get("tran_amt") == null ? "null" : ((BigDecimal)rs.get("tran_amt")).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
//                String dcFlag =  rs.get("dc_flag").toString();
//                String bankSeqNo = rs.get("bankseqno") == null ? "null" : (String)rs.get("bankseqno");
//                String toAcctNo = rs.get("to_acct_no") == null ? "null" : (String)rs.get("to_acct_no");
//                String toAcctName = rs.get("to_acct_name") == null ? "null" : (String)rs.get("to_acct_name");
//                StringBuilder resultStrBuilder = new StringBuilder();
//                resultStrBuilder.append(enterpriseBankAccount).append("|")
//                        .append(trandate).append("|")
//                        .append(trantime).append("|")
//                        .append(tranAmt).append("|")
//                        .append(dcFlag).append("|")
//                        .append(bankSeqNo).append("|")
//                        .append(toAcctNo).append("|")
//                        .append(toAcctName);
//                String concat_info = resultStrBuilder.toString();
//                Map param = new HashMap();
//                param.put("concat_info", concat_info);
//                param.put("id", id);
//                log.error("更新银行账户交易明细，id："+id);
//                SqlHelper.update(BANKRECONCILIATIONMAPPER+".updateDetailConcatInfo",param);
//            }catch (Exception e){
//                log.error("更新银行对账单失败，",e);
//            }
//
//        }
//    }
//
//
//}
