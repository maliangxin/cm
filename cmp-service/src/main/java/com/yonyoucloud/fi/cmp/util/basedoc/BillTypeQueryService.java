package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.ucf.transtype.service.itf.IBillTypeService;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.stereotype.Service;

@Service
public class BillTypeQueryService {


    /**
     * 查询单据类型根据formId
     *
     * @param formId 表单Id
     */
    public BdBillType queryBillTypeId(String formId) throws Exception {
        IBillTypeService billTypeService = AppContext.getBean(IBillTypeService.class);
        BdBillType billType = billTypeService.getBillTypeByFormId(InvocationInfoProxy.getTenantid(), formId);
        if (ObjectUtils.isEmpty(billType)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102651"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CAM-BE_180DF346057801CC", "单据类型查询失败！") /* "单据类型查询失败！" */);
        }
        return billType;
    }

}
