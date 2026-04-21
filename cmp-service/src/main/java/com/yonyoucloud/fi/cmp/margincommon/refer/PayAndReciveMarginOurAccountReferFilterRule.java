package com.yonyoucloud.fi.cmp.margincommon.refer;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内部单位账户过滤规则*
 *
 * @author xuxbo
 * @date 2023/8/15 16:03
 */


@Slf4j
@Component
public class PayAndReciveMarginOurAccountReferFilterRule extends AbstractCommonRule {
    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    public PayAndReciveMarginOurAccountReferFilterRule() {
        BILLNUM_MAP.add(IBillNumConstant.CMP_PAYMARGIN);
        BILLNUM_MAP.add(IBillNumConstant.CMP_RECEIVEMARGIN);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
//            RefEntity refentity = billDataDto.getRefEntity();
            if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && ICmpConstant.OURBANKACCOUNT_NAME.equals(billDataDto.getKey())) {
                List<BizObject> bizObjectList = (ArrayList) billDataDto.getData();
                if (bizObjectList.size() < 1 || bizObjectList == null) {
                    return new RuleExecuteResult();
                }
                //获取会计主体的id
                String accentity = bizObjectList.get(0).get("ourname");

                //获取币种id
                String currency = bizObjectList.get(0).get("currency");

                // 本方银行账户，查询开户类型，acctopentype，是SettlementCenter(内部户)，则对方也可以是 - 715张耀需求
                String account = bizObjectList.get(0).get("enterprisebankaccount");
                boolean innerFlag = queryAccountType(account);

                if (billDataDto.getCondition() == null) {
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "eq", currency));
                    if (innerFlag) {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new Integer[]{AcctopenTypeEnum.BankAccount.getValue(), AcctopenTypeEnum.SettlementCenter.getValue()}));
                    } else {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new Integer[]{AcctopenTypeEnum.BankAccount.getValue(), AcctopenTypeEnum.FinancialCompany.getValue(),AcctopenTypeEnum.OtherFinancial.getValue()}));
                    }
                    billDataDto.setCondition(conditon);
                } else {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "eq", currency));
                    if (innerFlag) {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new Integer[]{AcctopenTypeEnum.BankAccount.getValue(), AcctopenTypeEnum.SettlementCenter.getValue()}));
                    } else {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new Integer[]{AcctopenTypeEnum.BankAccount.getValue(), AcctopenTypeEnum.FinancialCompany.getValue(),AcctopenTypeEnum.OtherFinancial.getValue()}));
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 查询企业银行账户类型，是否为内部户
     * @param account
     * @return
     */
    private boolean queryAccountType(String account) {
        try {
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(account);
            if (AcctopenTypeEnum.SettlementCenter.getValue() == enterpriseBankAcctVO.getAcctopentype()) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return false;
    }

}
