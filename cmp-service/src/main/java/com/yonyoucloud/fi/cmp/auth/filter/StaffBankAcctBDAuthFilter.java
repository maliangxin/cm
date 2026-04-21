package com.yonyoucloud.fi.cmp.auth.filter;

import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;

import java.util.List;

public class StaffBankAcctBDAuthFilter {
    public  static void filter(BillContext billContext, boolean iscard, String billnum,
                               String refcode, BizObject data, FilterVO filterVO, BillDataDto billDataDto) throws Exception {
        List<String> employees = AuthUtil.getBizObjectAttr(data,IBillConst.EMPLOYEE);
        if(employees == null || employees.isEmpty()){
            // 兼容资金预测池字段名
            employees = AuthUtil.getBizObjectAttr(data, "oppId");
        }
        if(employees != null && employees.size() > 0){
            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("staff_id", "in", employees));
        }else{
            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("1", "eq", "2"));
        }
        filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("dr", "eq", "0"));
        //添加银行类别dr=0过滤
        filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("bank.dr", "eq", "0"));
    }
}
