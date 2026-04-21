package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
/*
回单匹配 - 银行电子回单
 */
@Component
public class BankRelevanceReceiptQueryRule extends AbstractCommonRule {

    private static String CMP_RECEIPTMATCH="cmp_bankreceiptmatch";
    private static String BANKACCOUNT="bankaccount";
    private static String DZDATE = "dzdate";
    private static String TRANDATE = "tranDate";
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(map);
        String billnum = billDataDto.getBillnum();
        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            for(int i =0;i<commonVOs.length;i++){
                FilterCommonVO  filterCommonVO = commonVOs[i];
                if(CMP_RECEIPTMATCH.equals(billnum)&&BANKACCOUNT.equals(filterCommonVO.getItemName())){
                    UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.ENTERPRISE_BANK_ACCOUNT, ICmpConstant.QUERY_EQ, filterCommonVO.getValue1());
                    commonVOs = ArrayUtils.remove(commonVOs,i);
                    i--;
                }
                if(DZDATE.equals(filterCommonVO.getItemName())&&CMP_RECEIPTMATCH.equals(billnum)){
                    String startDate = (String) filterCommonVO.getValue1();
                    String endDate = (String) filterCommonVO.getValue2();
                    if(StringUtils.isNotEmpty(startDate)){
                        UiMetaDaoHelper.appendCondition(filterVO,TRANDATE, ICmpConstant.QUERY_EGT, startDate);
                    }
                    if(StringUtils.isNotEmpty(endDate)){
                        UiMetaDaoHelper.appendCondition(filterVO,TRANDATE, ICmpConstant.QUERY_ELT, endDate);
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
