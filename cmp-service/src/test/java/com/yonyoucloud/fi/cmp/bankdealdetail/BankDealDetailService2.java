package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.OrgConfirmBillEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BankDealDetailService2 {
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CurrencyQueryService currencyQueryService;

    public BankDealDetailService2(EnterpriseBankQueryService enterpriseBankQueryService, CurrencyQueryService currencyQueryService, YmsOidGenerator ymsOidGenerator) {
        this.enterpriseBankQueryService = enterpriseBankQueryService;
        this.currencyQueryService = currencyQueryService;
        this.ymsOidGenerator = ymsOidGenerator;
    }

    public void analysisDetailData(CtmJSONObject detailData, Map<String, Object> enterpriseInfo,
                                   List<BankDealDetail> bankDealDetails,
                                   List<BankReconciliation> bankRecords, String currency) throws Exception {
        String bankSeqNo = detailData.getString("bank_seq_no");
            BankDealDetail detail = new BankDealDetail();
            BankReconciliation bankRecord = new BankReconciliation();
            bankRecord.setInitflag(false);
            bankRecord.setLibraryflag(false);
            bankRecord.setDataOrigin(DateOrigin.DownFromYQL);
            detail.setTenant(AppContext.getTenantId());
            bankRecord.setTenant(AppContext.getTenantId());
            detail.setAccentity((String) enterpriseInfo.get("accEntityId"));
//            bankRecord.setAccentity((String) enterpriseInfo.get("accEntityId"));
//            EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById((String) enterpriseInfo.get("accountId"));
//            //银行类别
//            bankRecord.setBanktype(enterpriseBankAcctVoWithRange.getBank());
//            // 所属组织
//            bankRecord.setOrgid(enterpriseBankAcctVoWithRange.getOrgid());
//            List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
//            if(orgRangeVOS != null && orgRangeVOS.size() == 1){
//                // 授权使用组织 只有一个
//                bankRecord.setAccentity(orgRangeVOS.get(0).getRangeOrgId());
//                // 授权使用组织确认节点 银行对账单
//                bankRecord.setConfirmbill(OrgConfirmBillEnum.CMP_BANKRECONCILIATION.getIndex());
//                // 确认状态 已确认
//                bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirmed.getIndex());
//            }else if(orgRangeVOS != null && orgRangeVOS.size() > 1){
//                // 授权使用组织 多个
//                bankRecord.setAccentity(null);
//                // 授权使用组织确认节点
//                bankRecord.setConfirmbill(null);
//                // 确认状态 待确认
//                bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
//            }else {
//                bankRecord.setAccentity(null);
//                // 授权使用组织确认节点
//                bankRecord.setConfirmbill(null);
//                // 确认状态 待确认
//                bankRecord.setConfirmstatus(ConfirmStatusEnum.Confirming.getIndex());
//            }
            detail.setEnterpriseBankAccount((String) enterpriseInfo.get("accountId"));
//            detail.setBanktype(enterpriseBankAcctVoWithRange.getBank());
            bankRecord.setBankaccount((String) enterpriseInfo.get("accountId"));
            String dateStr = detailData.getString("tran_date");
            Date tranDate = DateUtils.dateParse(dateStr, DateUtils.YYYYMMDD);
            detail.setTranDate(tranDate);
            bankRecord.setTran_date(tranDate);
            bankRecord.setDzdate(tranDate);
            String timeStr = detailData.getString("tran_time");
            if (StringUtils.isNotEmpty(timeStr)) {
                Date tranTime = DateUtils.dateParse(dateStr + timeStr, DateUtils.YYYYMMDDHHMMSS);
                detail.setTranTime(tranTime);
                bankRecord.setTran_time(tranTime);
            }
            String is_refund = detailData.getString("is_refund");
            if(StringUtils.isNotEmpty(is_refund)){
                detail.set("is_refund",is_refund);
                bankRecord.set("is_refund",is_refund);
            }
            detail.setBankseqno(bankSeqNo);
            bankRecord.setBank_seq_no(bankSeqNo);
            bankRecord.setThirdserialno(bankSeqNo);
            String toAcctNo = detailData.getString("to_acct_no");
            detail.setTo_acct_no(toAcctNo);
            bankRecord.setTo_acct_no(toAcctNo);
            //新增利息字段
            BigDecimal interest = detailData.getBigDecimal("interest");
            detail.setInterest(interest);
            bankRecord.setInterest(interest);
            String toAcctName = detailData.getString("to_acct_name");
            detail.setTo_acct_name(toAcctName);
            bankRecord.setTo_acct_name(toAcctName);
            String toAcctBank = detailData.getString("to_acct_bank");
            detail.setTo_acct_bank(toAcctBank);
            bankRecord.setTo_acct_bank(toAcctBank);
            String toAcctBankName = detailData.getString("to_acct_bank_name");
            detail.setTo_acct_bank_name(toAcctBankName);
            bankRecord.setTo_acct_bank_name(toAcctBankName);
            String currencyCode = detailData.getString("curr_code");
            //没有币种 取上面currency中的缓存币种
//            if (StringUtils.isNotEmpty(currencyCode)) {
//                currency = currencyQueryService.getCurrencyByCode(currencyCode);
//            }
//            detail.setCurrency(currency);
//            bankRecord.setCurrency(currency);

            String cashFlag = detailData.getString("cash_flag");
            detail.setCashflag(cashFlag);
            bankRecord.setCash_flag(cashFlag);
            BigDecimal acctBal = detailData.getBigDecimal("acct_bal");
            detail.setAcctbal(acctBal);
            bankRecord.setAcct_bal(acctBal);
            BigDecimal tranAmt = detailData.getBigDecimal("tran_amt");
            detail.setTran_amt(tranAmt);
            bankRecord.setTran_amt(tranAmt);
            String dcFlag = detailData.getString("dc_flag");
            if ("d".equalsIgnoreCase(dcFlag)) {
                detail.setDc_flag(Direction.Debit);
                bankRecord.setDc_flag(Direction.Debit);
                bankRecord.setDebitamount(tranAmt);
            } else if ("c".equalsIgnoreCase(dcFlag)) {
                detail.setDc_flag(Direction.Credit);
                bankRecord.setDc_flag(Direction.Credit);
                bankRecord.setCreditamount(tranAmt);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100094"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E5","银企联返回的借贷标识非法，请联系开发人员！") /* "银企联返回的借贷标识非法，请联系开发人员！" */);
            }
            String oper = detailData.getString("oper");
            detail.setOper(oper);
            bankRecord.setOper(oper);
            String valueDateStr = detailData.getString("value_date");
            if (StringUtils.isNotEmpty(valueDateStr)) {
                Date valueDate = DateUtils.dateParse(valueDateStr, DateUtils.YYYYMMDD);
                detail.setValue_date(valueDate);
                bankRecord.setValue_date(valueDate);
            }
            String useName = detailData.getString("use_name");
            detail.setUse_name(useName);
            bankRecord.setUse_name(useName);
            String remark = detailData.getString("remark");
            String remark01 = detailData.getString("remark01");
            detail.setRemark(remark);
            bankRecord.setRemark(remark);
            //财资统一对账码解析
            if(!StringUtils.isEmpty(remark)){
                String patternString = "#([a-zA-Z0-9]{6,8})#";
                // 创建 Pattern 对象
                Pattern pattern = Pattern.compile(patternString);
                // 创建 Matcher 对象
                Matcher matcher = pattern.matcher(remark);
                if (matcher.find()) {
                    // 获取匹配的内容
                    String matchedContent = "#" + matcher.group(1) + "#"; // 添加回前后两个#
                    bankRecord.setSmartcheckno(matchedContent);
                    bankRecord.setIsparsesmartcheckno(true);
                }
            }
            detail.setRemark01(remark01);
            bankRecord.setRemark01(remark01);
            String bankCheckCode = detailData.getString("bank_check_code");  //添加交易流水号 20201125
            detail.setBankdetailno(bankCheckCode);
            // TODO 银行对账问题调试
            bankRecord.setBankcheckno(bankCheckCode);
            // 处理是否存在客商档案
//            MerchantUtils.dealMerchantFlag(detail);
            //国际化新增字段
            BigDecimal rate = detailData.getBigDecimal("rate");
            detail.setRate(rate);

            BigDecimal fee_amt = detailData.getBigDecimal("fee_amt");
            detail.setFee_amt(fee_amt);

            String fee_amt_cur = detailData.getString("fee_amt_cur");
            //没有币种 取上面currency中的缓存币种
            if (StringUtils.isNotEmpty(fee_amt_cur)) {
                currency = currencyQueryService.getCurrencyByCode(fee_amt_cur);
            }
            detail.setFee_amt_cur(currency);

            String pay_use_desc = detailData.getString("pay_use_desc");
            detail.setPay_use_desc(pay_use_desc);

            BigDecimal corr_fee_amt = detailData.getBigDecimal("corr_fee_amt");
            detail.setCorr_fee_amt(corr_fee_amt);

            String corr_fee_amt_cur = detailData.getString("corr_fee_amt_cur");
            if (StringUtils.isNotEmpty(corr_fee_amt_cur)) {
                currency = currencyQueryService.getCurrencyByCode(corr_fee_amt_cur);
            }
            detail.setCorr_fee_amt_cur(currency);

            String sub_name = detailData.getString("sub_name");
            detail.setSub_name(sub_name);

            String proj_name = detailData.getString("proj_name");
            detail.setProj_name(proj_name);

            String budget_source = detailData.getString("budget_source");
            detail.setBudget_source(budget_source);

            String voucher_type = detailData.getString("voucher_type");
            detail.setVoucher_type(voucher_type);

            String voucher_no = detailData.getString("voucher_no");
            detail.setVoucher_no(voucher_no);

            detail.setEntityStatus(EntityStatus.Insert);
            bankRecord.setEntityStatus(EntityStatus.Insert);
            //20231127
            Long id = ymsOidGenerator.nextId();
            detail.setId(id);
            bankRecord.setId(id);
            // 添加创建时间、创建日期
            detail.setCreateDate(new Date());
            detail.setCreateTime(new Date());
            bankRecord.setCreateDate(new Date());
            bankRecord.setCreateTime(new Date());

            //RPT0210退票辨识优化CZFW-373754
            //【DSP支持问题】银企联已返回退票标识和原交易流水号，但是司库的交易流水界面不显示。麻烦老师帮忙看下。
            // 退票 银企返回的 1：是 2：否
            if(detailData.get("is_refund") != null){
                detail.setRefundFlag(detailData.get("is_refund").toString().equals("1"));
                bankRecord.setRefundFlag(detailData.get("is_refund").toString().equals("1"));
            }
            // 原交易流水号
            detail.setOrignBankseqno(detailData.getString("refund_original_transaction"));
            bankRecord.setOrignBankseqno(detailData.getString("refund_original_transaction"));

            //对方账号后去空格
            if (!StringUtils.isEmpty(bankRecord.getTo_acct_no())) {
                bankRecord.setTo_acct_no(bankRecord.getTo_acct_no().replaceAll(" ", ""));
            }
            //退票匹配
            if (!Short.valueOf(OppositeType.InnerOrg.getValue() + "").equals(bankRecord.getOppositetype())) {
                //20230517先屏蔽，等辨识规则开关添加后再开启
//            checkRefund(bankRecord);
            }
            //2023-11-30 yangjn 添加唯一交易流水验证
            String unique_no = detailData.getString("unique_no");
            detail.setUnique_no(unique_no);
            bankRecord.setUnique_no(unique_no);
            if (bankSeqNo == null || "".equals(bankSeqNo)) {
                bankDealDetails.add(detail);
                bankRecords.add(bankRecord);
            } else if (bankRecord.getBank_seq_no().equals(detail.getBankseqno())) {
                //保证同一个线程内交易明细与对账单数据一致；若不一致则不插入数据
                bankDealDetails.add(detail);
                bankRecords.add(bankRecord);
            }
            bankRecord.fillConcatInfo(formatConctaInfoBankReconciliation(bankRecord));

    }
    public String formatConctaInfoBankReconciliation(BankReconciliation bankReconciliation){

        Date tran_date = bankReconciliation.getTran_date();
        String tran_dateStr = null;
        if(tran_date != null){
            tran_dateStr = DateUtils.convertToStr(tran_date, "yyyy-MM-dd HH:mm:ss");
        }
        Date tran_time = bankReconciliation.getTran_time();
        String tran_timeStr = null;
        if(tran_time != null){
            tran_timeStr = DateUtils.convertToStr(tran_time, "yyyy-MM-dd HH:mm:ss");
        }
        String concatInfo = "";
        // 流水号为空
        if(com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankReconciliation.getBank_seq_no())){
            concatInfo = bankReconciliation.getBankaccount()+"|"
                    + tran_dateStr+"|"
                    + tran_timeStr+"|"
                    + bankReconciliation.getTran_amt().setScale(2,BigDecimal.ROUND_HALF_UP)+"|"
                    +bankReconciliation.getDc_flag().getValue()+"|"
                    +"null|"
                    +bankReconciliation.getTo_acct_no()+"|"
                    +bankReconciliation.getTo_acct_name();
        } else {
            // 流水号不为空
            concatInfo = bankReconciliation.getBankaccount()+"|"
                    + tran_dateStr+"|"
                    + tran_timeStr+"|"
                    + bankReconciliation.getTran_amt().setScale(2,BigDecimal.ROUND_HALF_UP)+"|"
                    +bankReconciliation.getDc_flag().getValue()+"|"
                    +bankReconciliation.getBank_seq_no()+"|"
                    +bankReconciliation.getTo_acct_no()+"|"
                    +bankReconciliation.getTo_acct_name();
        }
        return concatInfo;
    }
    public Map<String, List<BankDealDetail>> checkBankDealDetailRepeat(List<BankDealDetail> downloadData) throws Exception {
        Map<String, List<BankDealDetail>> returnMap = new HashMap<>();
        // 如果银企联返回数据为空，直接返回
        if (downloadData.isEmpty()) {
            return returnMap;
        }
        // 银企联下载数据去重并根据uniquNo查询
        // 1，银企联下载数据去重
        List<BankDealDetail> bankDealDetails = new ArrayList<>();
        Set<String> uniquNos = new HashSet<>();
        Set<String> newUniquNos = new HashSet<>();
        Map<String, BizObject> bizObjectMap = new HashMap<>();
        for (BizObject bizObject : downloadData) {
            BankDealDetail bankDealDetail = (BankDealDetail) bizObject;
            if (Objects.nonNull(bizObject.get("is_refund"))) {
                bankDealDetail.set("is_refund", bizObject.getString("is_refund"));
            }
            // 流水号+交易日期+交易时间+金额+方向+本方账号+对方账号+对方户名
            String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
            bankDealDetail.fillConcatInfo(newUniquNo);
            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankDealDetail.getUnique_no())) {
                if (!newUniquNos.contains(newUniquNo)) {
                    newUniquNos.add(newUniquNo);
                    bankDealDetails.add(bankDealDetail);
                }
            } else {
                if (!uniquNos.contains(bankDealDetail.getUnique_no())) {
                    uniquNos.add(bankDealDetail.getUnique_no());
                    uniquNos.add(newUniquNo);
                    bankDealDetails.add(bankDealDetail);
                }
            }
            BizObject cloneone=bizObject.clone();
            ((BankDealDetail) cloneone).setRefundFlag(null);
            ((BankDealDetail) cloneone).setOrignBankseqno(null);
            bizObjectMap.put(bankDealDetail.getUnique_no(), cloneone);
        }
        uniquNos.addAll(newUniquNos);
        // 2,根据uniquNO查询数据库数据
//        Map<String, BizObject> bizObjectMap = getExistDataByUniquNo(uniquNos, EventType.BankDealDetail.getValue());
        // 验重操作
        List<BankDealDetail> insertBizObjects = new ArrayList<>(); // 入库列表
        List<BankDealDetail> updateVoList = new ArrayList<>(); // 更新流水号/余额列表
        List<BankDealDetail> deleteVoList = new ArrayList<>();//状态为作废删除流水
        for (BankDealDetail bankDealDetail : bankDealDetails) {
            // 银行返回数据无唯一号
            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankDealDetail.getUnique_no())) {
                // 无唯一号 有字段拼接唯一号的情况，更新余额
                // 本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
//                String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
                String newUniquNo = bankDealDetail.getConcat_info();
                // 数据库存在该concat_info
                if (bizObjectMap.containsKey(newUniquNo)) {
                    if (bizObjectMap.get(newUniquNo).get("acctbal") == null ||
                            BigDecimal.ZERO.compareTo(bizObjectMap.get(newUniquNo).get("acctbal")) == 0) {
                        if (bankDealDetail.getAcctbal() != null) {
                            bizObjectMap.get(newUniquNo).set("acctbal", bankDealDetail.getAcctbal());
                            if ((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                                bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                                deleteVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                            } else {
                                bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                                updateVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                            }
                        }
                    }
                } else {
                    // 无唯一号 无字段拼接唯一号，直接入库
                    bankDealDetail.fillConcatInfo(newUniquNo);
                    if (!(Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                        insertBizObjects.add(bankDealDetail);
                    }
                }
            } else {
                // 有唯一号 数据库中有唯一号
                if (bizObjectMap.containsKey(bankDealDetail.getUnique_no())) {
                    BankDealDetail dbBankDealDetail = (BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no());
                    // 数据中存在该唯一号数据,判断流水号是否一致
                    boolean isUpdate = false;
                    if (bankDealDetail.getBankseqno() != null &&
                            dbBankDealDetail.get("bankseqno").equals(bankDealDetail.getBankseqno())) {
                        //CZFW-366798【DSP支持问题】小核心撤销流水，没有通过唯一码将库中流水进行删除
                        if ((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                            dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        } else {
                            // 流水号一致 判断数据库数据有无余额
                            if (dbBankDealDetail.get("acctbal") == null ||
                                    BigDecimal.ZERO.compareTo(dbBankDealDetail.get("acctbal")) == 0) {
                                if (bankDealDetail.getAcctbal() != null) {
                                    dbBankDealDetail.set("acctbal", bankDealDetail.getAcctbal());
                                    isUpdate = true;
                                }
                            }
                            // 判断对方账号、对方户名、摘要字段
                            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(dbBankDealDetail.getTo_acct_no())) {
                                dbBankDealDetail.set("to_acct_no", bankDealDetail.getTo_acct_no());
                                isUpdate = true;
                            }
                            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(dbBankDealDetail.getTo_acct_name())) {
                                dbBankDealDetail.set("to_acct_name", bankDealDetail.getTo_acct_name());
                                isUpdate = true;
                            }
                            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(dbBankDealDetail.getRemark())) {
                                dbBankDealDetail.set("remark", bankDealDetail.getRemark());
                                isUpdate = true;
                            }
                            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(dbBankDealDetail.getTo_acct_bank())) {
                                dbBankDealDetail.set("to_acct_bank", bankDealDetail.getTo_acct_bank());
                                isUpdate = true;
                            }
                            if (bankDealDetail.getRefundFlag() != null) {
                                dbBankDealDetail.setRefundFlag(bankDealDetail.getRefundFlag());
                                isUpdate = true;
                            }
                            if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankDealDetail.getOriginBankseqno())) {
                                dbBankDealDetail.setOrignBankseqno(bankDealDetail.getOriginBankseqno());
                                isUpdate = true;
                            }
                            if (isUpdate) {
                                if ((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                                    dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                                    deleteVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                                } else {
                                    dbBankDealDetail.setEntityStatus(EntityStatus.Update);
                                    updateVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                                }
                            }
                        }
                    } else {
                        // 流水号不一致 更新流水号
                        bizObjectMap.get(bankDealDetail.getUnique_no()).set("bankseqno", bankDealDetail.getBankseqno());
                        if ((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                            dbBankDealDetail.setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        } else {
                            dbBankDealDetail.setEntityStatus(EntityStatus.Update);
                            updateVoList.add((BankDealDetail) bizObjectMap.get(bankDealDetail.getUnique_no()));
                        }
                    }

                } else {
                    //有唯一号，数据库中无该唯一号数据，判断字段拼接唯一号
                    //本方账号+交易日期+交易时间+金额+方向+流水号+对方账号+对方户名
                    String newUniquNo = formatConctaInfoBankDealDetail(bankDealDetail);
                    // 数据库中不存在该字段拼接唯一号
                    if (!bizObjectMap.containsKey(newUniquNo)) {
                        bankDealDetail.fillConcatInfo(newUniquNo);
                        if (!(Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                            insertBizObjects.add(bankDealDetail);
                        }
                    } else {
                        // 退票，删除
                        if ((Objects.nonNull(bankDealDetail.get("is_refund")) && "3".equals(bankDealDetail.get("is_refund")))) {
                            bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Delete);
                            deleteVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                        } else {
                            // 数据库中存在该字段拼接唯一号,且库中唯一号为空，更新唯一号；否则入库
                            if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bizObjectMap.get(newUniquNo).get("unique_no"))) {
                                bizObjectMap.get(newUniquNo).set("unique_no", bankDealDetail.getUnique_no());
                                bizObjectMap.get(newUniquNo).setEntityStatus(EntityStatus.Update);
                                updateVoList.add((BankDealDetail) bizObjectMap.get(newUniquNo));
                            } else {
                                insertBizObjects.add(bankDealDetail);
                            }
                        }
                    }
                }
            }
        }
        returnMap.put("insertData", insertBizObjects);
        returnMap.put("updateData", updateVoList);
        returnMap.put("deleteData", deleteVoList);
        return returnMap;
    }

    public String formatConctaInfoBankDealDetail(BankDealDetail bankDealDetail) {

        Date tran_date = bankDealDetail.getTranDate();
        String tran_dateStr = null;
        if (tran_date != null) {
            tran_dateStr = DateUtils.convertToStr(tran_date, "yyyy-MM-dd HH:mm:ss");
        }
        Date tran_time = bankDealDetail.getTranTime();
        String tran_timeStr = null;
        if (bankDealDetail.getTranTime() != null) {
            tran_timeStr = DateUtils.convertToStr(tran_time, "yyyy-MM-dd HH:mm:ss");
        }
        String concatInfo = "";
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(bankDealDetail.getBankseqno())) {
            concatInfo = bankDealDetail.getEnterpriseBankAccount() + "|"
                    + tran_dateStr + "|"
                    + tran_timeStr + "|"
                    + bankDealDetail.getTran_amt().setScale(2, BigDecimal.ROUND_HALF_UP) + "|"
                    + bankDealDetail.getDc_flag().getValue() + "|"
                    + "null|"
                    + bankDealDetail.getTo_acct_no() + "|"
                    + bankDealDetail.getTo_acct_name();
        } else {
            concatInfo = bankDealDetail.getEnterpriseBankAccount() + "|"
                    + tran_dateStr + "|"
                    + tran_timeStr + "|"
                    + bankDealDetail.getTran_amt().setScale(2, BigDecimal.ROUND_HALF_UP) + "|"
                    + bankDealDetail.getDc_flag().getValue() + "|"
                    + bankDealDetail.getBankseqno() + "|"
                    + bankDealDetail.getTo_acct_no() + "|"
                    + bankDealDetail.getTo_acct_name();
        }
        return concatInfo;
    }
}
