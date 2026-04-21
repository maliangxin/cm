package com.yonyoucloud.fi.cmp.common.service;

import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * 现金管理公共校验服务类
 *
 * @author maliangn
 * @since 2023.9.19
 *
 */
@Service
public class CmCommonCheckService {

    //现金管理使用的结算方式分别为银行、现金、票据、支票、第三方
    static List SERVICE_ATTR_LIST = asList(0,1,2,8,10);

    /**
     * 校验结算方式
     * @param serviceAttr
     */
    public void checkServiceAttr(Integer serviceAttr) throws CtmException {
        if(!SERVICE_ATTR_LIST.contains(serviceAttr)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101290"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180822","结算方式业务属性只能为票据结算、银行业务、现金业务和第三方！") /* "结算方式业务属性只能为票据结算、银行业务、现金业务和第三方！" */);
        }
    }

    /**
     * 校验pubts
     * @throws Exception
     */
    public void checkPubts() throws Exception{

    }


    /**
     * 校验期间Period  true是在期间内，否则不在
     * @throws Exception
     */
    public boolean checkPeriod(BizObject bizObject) throws Exception{
        Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get("accentity"), ISystemCodeConstant.ORG_MODULE_GL);
        boolean dataFlag = true;
        if (enabledBeginData == null) {
            dataFlag = false;
        } else {
            if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
                dataFlag = false;
            }
        }
        return dataFlag;
    }



}
