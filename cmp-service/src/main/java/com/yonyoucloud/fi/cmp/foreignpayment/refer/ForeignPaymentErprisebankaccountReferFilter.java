package com.yonyoucloud.fi.cmp.foreignpayment.refer;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT;

/**
 * <h1>外汇付款 付款银行账户过滤</h1>
 * 只保留银行业务*
 * * *
 *
 * @author xuxbo
 * @date 2023/3/18 14:47
 */
@Component
public class ForeignPaymentErprisebankaccountReferFilter extends AbstractCommonRule {


    @Autowired
    private SettleMethodService settleMethodService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ForeignPaymentErprisebankaccountReferFilter() {

        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENT);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        RefEntity refentity = billDataDto.getRefEntity();
        List<ForeignPayment> foreignPayments = (ArrayList) billDataDto.getData();
//        ForeignPayment foreignPayment = (ForeignPayment) billDataDto.getData();
        if (BILLNUM_MAP.contains(billnum)) {

            if (billDataDto.getCondition() == null) {
                FilterVO filterVO = new FilterVO();
                if (UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && ICmpConstant.PAYMENTERPRISEBANKACCOUNT_ACCOUNT.equals(billDataDto.getKey())) {
                    if (ObjectUtils.isNotEmpty(foreignPayments.get(0).getSettlemode())) {
                        //获取会计主体的id
                        String accentity = foreignPayments.get(0).get("accentity");
                        if (settleMethodService.checkSettleMethod(foreignPayments.get(0).getSettlemode().toString())) {
                            // 去账户直连状态的档案里查是否开通银企联=是
                            List<String> bankAccountList = bankAccountSettingService.queryBankAccountSettingByFlag();
                            String[] bankAccountIDs = bankAccountList.stream().toArray(String[]::new);
                            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, bankAccountIDs));
                        }
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
                        billDataDto.setCondition(filterVO);
                    }
                }
            } else {
                if (UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())  && ICmpConstant.PAYMENTERPRISEBANKACCOUNT_ACCOUNT.equals(billDataDto.getKey())) {
                    if (ObjectUtils.isNotEmpty(foreignPayments.get(0).getSettlemode())) {
                        //获取会计主体的id
                        String accentity = foreignPayments.get(0).get("accentity");
                        if (settleMethodService.checkSettleMethod(foreignPayments.get(0).getSettlemode().toString())) {
                            // 去账户直连状态的档案里查是否开通银企联=是
                            List<String> bankAccountList = bankAccountSettingService.queryBankAccountSettingByFlag();
                            String[] bankAccountIDs = bankAccountList.stream().toArray(String[]::new);
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, bankAccountIDs));
                        }
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));

                    }
                }
            }

        }
        return new RuleExecuteResult();
    }
}
