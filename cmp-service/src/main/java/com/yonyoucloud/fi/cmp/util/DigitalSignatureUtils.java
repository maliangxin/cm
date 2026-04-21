package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;

import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.basecom.service.ref.CustomerRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay_b;
import com.yonyoucloud.fi.cmp.util.basedoc.CustomerQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @ClassName DigitalSignatureUtils
 * @Description 数据签名工具类
 * @Author tongyd
 * @Date 2019/6/14 11:18
 * @Version 1.0
 **/
@Slf4j
public class DigitalSignatureUtils {

    private static AtomicInteger cardinalNumber = new AtomicInteger(1);

    @Autowired
    CustomerRpcService customerRpcService;

    @Autowired
    BankAccountSettingService bankAccountSettingService;

    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    /*
     *@Author tongyd
     *@Description 获取签名原文
     *@Date 2019/6/15 11:32
     *@Param [retailerAccountName, retailerAccountNo, oriSum]
     *@Return java.lang.String
     **/
    public static String getOriginalMsg(Map<String, Object> bizObject) throws Exception {
        Short caObject = TypeUtils.castToShort(bizObject.get("caobject"));
        Object recAccount = null;
        Object recUserName = null;
        if (caObject.equals(CaObject.Customer.getValue())) {
            if (bizObject.get("customerbankaccount") != null) {
                CustomerQueryService customerQueryService = AppContext.getBean(CustomerQueryService.class);
                AgentFinancialDTO bankAccount = customerQueryService.getCustomerAccountByAccountId(Long.parseLong(bizObject.get("customerbankaccount").toString()));
                if(bankAccount != null){
                    recAccount = bankAccount.getBankAccount();
                    recUserName = bankAccount.getBankAccountName();
                }
            }
        } else if (caObject.equals(CaObject.Supplier.getValue())) {
            if (bizObject.get("supplierbankaccount") != null) {
                VendorQueryService vendorQueryService = AppContext.getBean(VendorQueryService.class);
                VendorBankVO bankAccount = vendorQueryService.getVendorBanksByAccountId(bizObject.get("supplierbankaccount"));
                if(bankAccount != null){
                    recUserName = bankAccount.get("accountname");
                    recAccount = bankAccount.get("account");
                }
            }
        } else if (caObject.equals(CaObject.Employee.getValue())) {
            if (bizObject.get("staffBankAccount") != null) {
                Map<String, Object> bankAccount = QueryBaseDocUtils.queryStaffBankAccountById(bizObject.get("staffBankAccount"));/* 暂不修改 静态方法*/
                if(bankAccount != null){
                    recAccount = bankAccount.get("account");
                    if (bizObject.get("employee") != null) {
                        Map<String, Object> employee = QueryBaseDocUtils.queryStaffById(bizObject.get("employee"));/* 暂不修改 静态方法*/
                        if(employee != null){
                            recUserName = employee.get("name");
                        }
                    }
                }
            }

        } else if (caObject.equals(CaObject.Other.getValue())) {
            recAccount = bizObject.get("retailerAccountNo");
            recUserName = bizObject.get("retailerAccountName");
        }
        StringBuilder originalMsg = new StringBuilder();
        originalMsg.append("money:");
        originalMsg.append(((BigDecimal) bizObject.get(IBussinessConstant.ORI_SUM)).stripTrailingZeros().toPlainString());
        originalMsg.append(";");
        originalMsg.append("recAccount:");
        originalMsg.append(recAccount);
        originalMsg.append(";");
        originalMsg.append("recUserName:");
        originalMsg.append(recUserName);
        originalMsg.append(";");
        return originalMsg.toString();
    }

    /**
     * 获取薪资支付的签名内容
     * @param salaryPay
     * @return
     * @throws Exception
     */
    public static String getSalaryPayOriginalMsg(Salarypay salaryPay) throws Exception {
        StringBuilder originalMsg = new StringBuilder();
        List<Salarypay_b> salarypay_bList = salaryPay.getBizObjects("Salarypay_b", Salarypay_b.class);
        // 如果是以NEW_SIGN开头的，则需要进行排序再验签
        if(salaryPay.getSignature() != null && salaryPay.getSignature().startsWith(NEW_SIGN)){
            salarypay_bList = salarypay_bList.stream().sorted(Comparator.comparing(map -> map.get("id"))).collect(Collectors.toList());
        }
        for (Salarypay_b salaryPay_b : salarypay_bList) {
            if (BigDecimal.ZERO.compareTo(salaryPay_b.getAmount()) >= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102574"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153412") /* "转账金额必须大于0" */);
            }
//            if (StringUtils.isEmpty(salaryPay_b.getIdentitytype()) || StringUtils.isEmpty(salaryPay_b.getIdentitynum())) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102575"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418035B","收方证件类型和收方证件号码不能为空") /* "收方证件类型和收方证件号码不能为空" */);
//            }
            if (StringUtils.isEmpty(salaryPay_b.getCrtacc())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102576"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418035C","收款账号不能为空") /* "收款账号不能为空" */);
            }
            originalMsg.append(salaryPay_b.getAmount().stripTrailingZeros().toPlainString() + salaryPay_b.getCrtacc() + salaryPay_b.getCrtaccname());
        }
        return originalMsg.toString();
    }
    // 薪资支付签名前缀
    public static final String NEW_SIGN = "NEW_SIGN";

    /**
     * 构建请求流水号
     * @param customNo
     * @return
     * @throws Exception
     */
    public static String buildRequestNum(String customNo) throws Exception {
        return new StringBuffer("R").append(customNo).append("000")
                .append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()))
                .append(YQLUtils.getSerialNumberNoCASWithMaxNum5(cardinalNumber)).toString();
    }

    /**
     * 构建交易流水号
     * @param customNo
     * @return
     * @throws Exception
     */
    public static String buildTranSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("T");
        tranSeqNo.append(customNo);
        tranSeqNo.append("000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCASWithMaxNum5(cardinalNumber));
        return tranSeqNo.toString();
    }

}
