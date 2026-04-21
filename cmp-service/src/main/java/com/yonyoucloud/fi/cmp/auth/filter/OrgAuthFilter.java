package com.yonyoucloud.fi.cmp.auth.filter;

import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;

import java.util.*;

/**
 * @author qihaoc
 * @Description:公共的组织参照数据权限过滤
 * @date 2024/11/11 17:24
 */
public class OrgAuthFilter {

    public  static void filter(BillContext billContext, boolean iscard, String billnum,
                     String refcode, BizObject data, FilterVO filterVO, BillDataDto billDataDto) throws Exception {
        Set<String> orgids  = new HashSet<String>();
        Set<String> orgidts;
        //取不到组织，异常情况，直接返回，不加条件不抛出错误
        List<String> accentity = AuthUtil.getBizObjectAttr(data, IBillConst.ACCENTITY);
        if(CollectionUtils.isEmpty(accentity)){
            return;
        }
        //根据委托关系
        orgidts = FIDubboUtils.getDelegateHasSelf(  accentity.toArray(new String[0]) ) ;
        if(orgidts==null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101677"),"keynull");
        }
        orgids.addAll(orgidts);
        //根据职能共享
        orgidts = FIDubboUtils.getOrgShareHasSelf( orgidts.toArray(new String[0])) ;
        orgids.addAll(orgidts);
        if(orgids.size()>0) {
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", orgids)); //QueryCondition.name("id").in(orgids)
        }
    }
}
