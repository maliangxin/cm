package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.BizException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.*;

/**
 * 日结相关方法
 */
public class DailyComputeUtils {

    /**
     * 查询资金组织对应现金管理模块会计期间的启用日期
     * @param orgid 资金组织id
     * @return Date 启用日期
     * @throws Exception
     */
    public static Date queryOrgPeriodBeginDate(String orgid) throws Exception {
        if (null == orgid) {
            return null;
        }
        List<Map<String, Object>> maps = queryOrgBpOrgConfVO(orgid, ISystemCodeConstant.ORG_MODULE_CM);
        if (CollectionUtils.isNotEmpty(maps)){
            return (Date) maps.get(0).get("begindate");
        } else {
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{orgid}));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102022"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
        }
    }

    public static List<Map<String, Object>> queryOrgBpOrgConfVO(Object orgid, String modules) throws Exception {
        if (null == orgid) {
            return new ArrayList<>();
        }
        if (StringUtil.isEmpty(modules)){
            modules = ISystemCodeConstant.ORG_MODULE_CM;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.orgBpConf.OrgBpOrgConfVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
        QuerySchema schema = QuerySchema.create().addSelect("periodid,periodid.begindate as begindate,periodid.enddate as enddate,type_code,orgid,enable");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("type_code").eq(modules), QueryCondition.name("enable").eq(1), QueryCondition.name("dr").eq(0));
        if (orgid instanceof List){
            conditionGroup.appendCondition(QueryCondition.name("orgid").in(orgid));
        } else {
            conditionGroup.appendCondition(QueryCondition.name("orgid").eq(orgid));
        }
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(billContext, schema);
    }
}
