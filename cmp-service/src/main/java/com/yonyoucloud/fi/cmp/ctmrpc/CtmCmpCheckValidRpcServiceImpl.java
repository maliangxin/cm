package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.diwork.model.ControlPointStatus;
import com.yonyou.diwork.model.dto.ControlPointDTO;
import com.yonyou.diwork.service.ILicenseQueryService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.common.CtmCmpCheckValidRpcService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: mal
 * @Description: 本类用于
 * @Date: Created in  2022/8/10 10:00
 * @Version v1.0
 */
@Service
@Slf4j
public class CtmCmpCheckValidRpcServiceImpl implements CtmCmpCheckValidRpcService {

    static final String VALID = "1";
    static final String EXPIRED = "0";
    static final String VALID_KEY = "ppm_valid";

    /**
     * 判断现金管理所属规格是否到期
     * @return
     */
    @Override
    public boolean checkLisenceValid() {
        String validCache = AppContext.cache().get(VALID_KEY+ InvocationInfoProxy.getTenantid());
        if(VALID.equals(validCache)){//lisence有效
            return true;
        }else if(EXPIRED.equals(validCache)){//lisence无效
            return false;
        }
        List<String> ppms = new ArrayList<>();
        ppms.add("ppm0000055716");
        ppms.add("ppm0000081476");
        ppms.add("ppm0000085352");
        ppms.add("ppm0000089705");
        ppms.add("ppm0000072505");
        ppms.add("ppm0000064467");
        List<ControlPointDTO> controlPointDTOs = RemoteDubbo.get(ILicenseQueryService.class, IDomainConstant.MDD_WORKBENCH_SERVICE).getCurrentByCodes(InvocationInfoProxy.getTenantid(), ppms);
        if(controlPointDTOs.size()>0){
            for(ControlPointDTO controlPoint:controlPointDTOs){
                if(controlPoint.getStatus() == ControlPointStatus.VALID){
                    log.error("controlPoint.status ================"+ controlPoint.getStatus());
                    AppContext.cache().set(VALID_KEY+InvocationInfoProxy.getTenantid(), VALID, 60*60);
                    return true;
                }
            }
        }
        AppContext.cache().set(VALID_KEY+InvocationInfoProxy.getTenantid(), EXPIRED, 60*60);
        return false;
    }
}
