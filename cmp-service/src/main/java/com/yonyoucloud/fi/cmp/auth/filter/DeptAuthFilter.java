package com.yonyoucloud.fi.cmp.auth.filter;

import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import groovy.util.logging.Slf4j;
import org.imeta.orm.base.BizObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author qihaoc
 * @Description:公共的部门参照数据权限过滤
 * @date 2024/11/11 17:24
 */
@lombok.extern.slf4j.Slf4j
@Slf4j
public class DeptAuthFilter {

    public  static void filter(BillContext billContext, boolean iscard, String billnum,
                               String refcode, BizObject data, FilterVO filterVO, BillDataDto billDataDto) throws Exception {
        if (data == null) {
            log.error("DeptAuthFilter data is null");
            return;
        }
        //当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门
        Map<String,Object> billDataMap = new HashMap<String,Object>();
        if(null != billDataDto.getExternalData() && billDataDto.getExternalData() instanceof Map){
            billDataMap = (Map<String,Object>)billDataDto.getExternalData();
        }
        billDataMap.put("ref_parentorgid",data.get("accentity"));//当前组织
        billDataMap.put("funcCode","all");//所有职能共享
        billDataMap.put("accountdelegate","true");//查询核算委托
        if (IBillNumConstant.CMP_BILLCLAIM_CARD.equals(billnum) && !Objects.isNull(data.get("actualclaimaccentiry"))) {
            billDataMap.put("ref_parentorgid", data.get("actualclaimaccentiry"));//当前组织
        }
        billDataDto.setExternalData(billDataMap);

    }
}
