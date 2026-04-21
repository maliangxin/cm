package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/*
回单匹配-银行日记账规则
 */
@Component
public class BankRelevanceJournalQueryRule extends AbstractCommonRule {

    private static  String CMP_BANKJOURNALMATCH="cmp_bankjournalmatch";
    private static  String BILLTYPE="billtype";
    private static String EQUALSACCOUNT="equalsaccount";
    public static  String DZDATE = "dzdate";
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(map);
        String billnum = billDataDto.getBillnum();
        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
            List<Short> billtype = new ArrayList<>();
            billtype.add(EventType.ReceiveBill.getValue());
            billtype.add(EventType.PayMent.getValue());
            billtype.add(EventType.TransferAccount.getValue());
            billtype.add(EventType.SalaryPayment.getValue());
            billtype.add(EventType.CurrencyExchangeBill.getValue());
            billtype.add(EventType.StwbSettleMentDetails.getValue());
            if(CMP_BANKJOURNALMATCH.equals(billnum)){
                UiMetaDaoHelper.appendCondition(filterVO, BILLTYPE, ICmpConstant.QUERY_IN, billtype);
            }
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            for(int i =0;i<commonVOs.length;i++){
                FilterCommonVO  filterCommonVO = commonVOs[i];
                if(DZDATE.equals(filterCommonVO.getItemName()) && CMP_BANKJOURNALMATCH.equals(billnum)){
                    String startDate = (String) filterCommonVO.getValue1();
                    String endDate = (String) filterCommonVO.getValue2();
                    if(StringUtils.isNotEmpty(startDate)){
                        UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.VOUCHDATE, ICmpConstant.QUERY_EGT, startDate);
                    }
                    if(StringUtils.isNotEmpty(endDate)){
                        UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.VOUCHDATE, ICmpConstant.QUERY_ELT, endDate);
                    }
                    commonVOs = ArrayUtils.remove(commonVOs,i);
                    i--;
                }
            }
            filterVO.setCommonVOs(commonVOs);
        }
        billDataDto.setCondition(filterVO);
        putParam(map, billDataDto);
        return new RuleExecuteResult();
    }
}
