package com.yonyoucloud.fi.cmp.auth.filter;

import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import org.imeta.orm.base.BizObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author qihaoc
 * @Description:公共的项目参照数据权限过滤 组织权限+项目类型，组织、项目类型为空返回全部数据
 * @date 2024/11/11 17:24
 */
public class ProjectAuthFilter {

    public  static void filter(BillContext billContext, boolean iscard, String billnum,
                     String refcode, BizObject data, FilterVO filterVO, BillDataDto billDataDto) throws Exception {
        List<String> lstAccentity = AuthUtil.getBizObjectAttr(data, IBillConst.ACCENTITY);
        Set<String> retorgids = null;
        if (lstAccentity != null && lstAccentity.size() > 0) {
            retorgids = FIDubboUtils.getDelegateHasSelf(data.get("accentity").toString());
            Object externalData = billDataDto.getExternalData();
            if (externalData instanceof Map) {
                ((Map) externalData).put("orgidList", retorgids.toArray(new String[0]));
            } else if (externalData == null) {
                Map orgMap = new HashMap();
                orgMap.put("orgidList", retorgids.toArray(new String[0]));
                billDataDto.setExternalData(orgMap);
            }
        }
    }
}
