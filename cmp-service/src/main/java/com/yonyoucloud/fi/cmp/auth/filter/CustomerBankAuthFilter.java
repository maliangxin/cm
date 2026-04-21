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

/**
 * @author qihaoc
 * @Description:公共的客户参照数据权限过滤
 * @date 2024/11/11 17:24
 */
public class CustomerBankAuthFilter {

    public  static void filter(BillContext billContext, boolean iscard, String billnum,
                     String refcode, BizObject data, FilterVO filterVO, BillDataDto billDataDto) throws Exception {
        List<String> customers = AuthUtil.getBizObjectAttr(data, IBillConst.CUSTOMER) ;
        if (customers != null) {
            filterVO.appendCondition(ConditionOperator.and,new SimpleFilterVO("merchantId", "in", customers));
        }
    }
}
