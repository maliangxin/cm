package com.yonyoucloud.fi.cmp.fundpayment.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: ctm-cmp
 * @BelongsPackage: com.yonyoucloud.fi.cmp.fundpayment.refer
 * @Author: wenyuhao
 * @CreateTime: 2023-12-18  10:44
 * @Description: 资金付款单 付款银行账户参照过滤
 * @Version: 1.0
 */
@Component("fundPaymentPayBankAccountReferFilter")
public class FundPaymentPayBankAccountReferFilter extends AbstractCommonRule {

    @Autowired
    private SettleMethodService settleMethodService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    private final String FUNDPAYMENT_B = "cmp.fundpayment.FundPayment_b";

    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public FundPaymentPayBankAccountReferFilter() {
        BILLNUM_MAP.add(IBillNumConstant.FUND_PAYMENT);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
            if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())) {
                if (billDataDto.getData() instanceof ArrayList) {
                    List<FundPayment> fundPayments = (ArrayList) billDataDto.getData();
                    FilterVO conditon = new FilterVO();
                    if (CollectionUtils.isEmpty(fundPayments)) {
                        return new RuleExecuteResult();
                    }
                    //结算方式为银行转账且结算方式的是否直连为是时，国机-需要拿付款银行账号去账户直连状态的档案里查是否开通银企联=是
                    Long settlemode = fundPayments.get(0).get("settlemode");
                    String payBankAccount = "enterprisebankaccount.name";
                    if (billDataDto.getFullname() != null && FUNDPAYMENT_B.equals(billDataDto.getFullname())) {
                        List<FundPayment_b> subList = (ArrayList) fundPayments.get(0).get("FundPayment_b");
                        settlemode = subList.get(0).get("settlemode");
                    }
                    if (payBankAccount.equals(billDataDto.getDatasource()) && null != settlemode) {
                        //  结算方式为银行转账且是否直连为是时为true
                        if (settleMethodService.checkSettleMethod(settlemode.toString())) {
                            // 去账户直连状态的档案里查是否开通银企联=是
                            // CM202400731 结算方式的是否直联=直联时，付款银行账户应当满足条件:账户直联开通设置中直联授权权限为“查询及支付”=是
                            List<String> bankAccountList = bankAccountSettingService.queryBankAccountSettingByDirect();
                            String[] bankAccountIDs = bankAccountList.toArray(new String[0]);
                            SimpleFilterVO businessBankAccountCondition = new SimpleFilterVO(ConditionOperator.and);
                            businessBankAccountCondition.addCondition(new SimpleFilterVO("id", "in", bankAccountIDs));

                            // 结算中心开户
                            SimpleFilterVO settleCenterBankAccountCondition = new SimpleFilterVO(ConditionOperator.and);
                            settleCenterBankAccountCondition.addCondition(new SimpleFilterVO("acctopentype", "eq", 1));
                            if (billDataDto.getCondition() == null) {
                                conditon.appendCondition(ConditionOperator.or, businessBankAccountCondition, settleCenterBankAccountCondition);
                                billDataDto.setCondition(conditon);
                            } else {
                                billDataDto.getCondition().appendCondition(ConditionOperator.or, businessBankAccountCondition, settleCenterBankAccountCondition);
                            }
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

}
