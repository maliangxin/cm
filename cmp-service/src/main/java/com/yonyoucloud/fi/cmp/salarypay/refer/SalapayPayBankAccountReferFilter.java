package com.yonyoucloud.fi.cmp.salarypay.refer;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.imeta.core.base.ConditionOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @BelongsProject: ctm-cmp
 * @BelongsPackage: com.yonyoucloud.fi.cmp.salarypay.refer
 * @Author: wenyuhao
 * @CreateTime: 2023-12-18  10:49
 * @Description: 薪资支付单 付款银行账户参照过滤
 * @Version: 1.0
 */
@Component("salapayPayBankAccountReferFilter")
public class SalapayPayBankAccountReferFilter extends AbstractCommonRule {

    @Autowired
    private SettleMethodService settleMethodService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    private  BaseRefRpcService baseRefRpcService;

    private final String BD_ENTERPRISEBANKACCTREF="ucfbasedoc.bd_enterprisebankacctref";
    private final String BD_ENTERPRISEBANKACCT="ucfbasedoc.bd_enterprisebankacct";
    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public SalapayPayBankAccountReferFilter() {
        BILLNUM_MAP.add(IBillNumConstant.SALARYPAY);
        BILLNUM_MAP.add(IBillNumConstant.SALARYSETTLE);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)){

            if (BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())) {
                List<Salarypay> salarypays = (ArrayList) billDataDto.getData();
                if(salarypays == null || salarypays.size() == 0){
                    return new RuleExecuteResult();
                }
                FilterVO conditon = new FilterVO();
                //结算方式为银行转账且结算方式的是否直连为是时，国机-需要拿付款银行账号去账户直连状态的档案里查是否开通银企联=是
                if (null != salarypays.get(0).get("settlemode")){
                    if (settleMethodService.checkSettleMethod(salarypays.get(0).get("settlemode").toString())){
                        // 去账户直连状态的档案里查是否开通银企联=是
                        List<String> bankAccountList=bankAccountSettingService.queryBankAccountSettingByFlag();
                        String[] bankAccountIDs=bankAccountList.stream().toArray(String[]::new);
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, bankAccountIDs));
                        billDataDto.setCondition(conditon);
                    }
                } else {
                    // 增加内部账户过滤
                    EnterpriseParams enterpriseParams = new EnterpriseParams();
                    enterpriseParams.setAcctopentype(0);
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                    List<String> accountId = enterpriseBankAcctVOS.stream().map(e -> e.getId()).collect(Collectors.toList());
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, accountId));
                    billDataDto.setCondition(conditon);
                }
            }
        }
        return new RuleExecuteResult();
    }
}

