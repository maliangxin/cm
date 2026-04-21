package com.yonyoucloud.fi.cmp.util.basedoc;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.transtype.exception.TransTypeRpcException;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TransTypeQueryParam;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransTypeQueryService {

    @Autowired
    ITransTypeService iTransTypeService;

    /**
     * 交易类型相关接口，需上送租户id的，统一上送YhtTenantId，需要注意
     * @param id
     * @return
     * @throws Exception
     */
    public BdTransType findById(String id) throws Exception {
        return iTransTypeService.getTransTypesById(InvocationInfoProxy.getTenantid().toString(), id);
    }

    /**
     * 根据交易编码查询交易类型实体
     *
     * @param transTypeCode
     * @return
     * @throws TransTypeRpcException
     */
    public BdTransType queryTransTypes(String transTypeCode) throws TransTypeRpcException {
        TransTypeQueryParam transTypeQueryParam = new TransTypeQueryParam();
        transTypeQueryParam.setTransTypeCode(transTypeCode);
        transTypeQueryParam.setTenantId(InvocationInfoProxy.getTenantid().toString());
        List<BdTransType> bdTransTypes = iTransTypeService.queryTransTypes(transTypeQueryParam);
        if (CollectionUtils.isEmpty(bdTransTypes)) {
            return null;
        } else {
            return bdTransTypes.get(0);
        }
    }

}
