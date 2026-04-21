package com.yonyoucloud.fi.cmp.util.ctmpub;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.tmsp.openapi.ITmspBusiSysRegistrationRpcService;
import com.yonyoucloud.fi.tmsp.vo.request.TmspBusiSysRegistrationQueryRequestVO;
import com.yonyoucloud.fi.tmsp.vo.response.TmspBusiSysRegistrationQueryResponseVO;
import com.yonyoucloud.fi.tmsp.vo.response.TmspBusiSysRegistrationResp;
import com.yonyoucloud.fi.tmsp.vo.response.TmspBusiSysRegistrationSubResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@Slf4j
public class BusiSysRegistrationServiceImpl {

    private static final Cache<String, String> topsrcitemCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    private static final Cache<String, String> topsribillTypeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();


    /**
     * 根据单据类型查询业务系统注册信息
     *
     * @param busiBillType
     * @return
     * @throws Exception
     */
    public String queryBusiSysRegistration(String busiBillType) throws Exception {
        TmspBusiSysRegistrationQueryRequestVO requestVO = new TmspBusiSysRegistrationQueryRequestVO();
        requestVO.setBusiBillType(busiBillType);
        TmspBusiSysRegistrationQueryResponseVO responseVO = RemoteDubbo.get(ITmspBusiSysRegistrationRpcService.class, IDomainConstant.YONBIP_FI_CTMPUB).queryBusiSysRegistration(requestVO);
        return responseVO.getData().get(0).getBillTypeId();
    }

    /**
     * 根据业务单据类型编码查询业务系统注册信息
     *
     * @param topsrcbilltype  业务单据类型编码
     * @return 业务单据类型名称
     * @throws Exception
     */
    public String queryBusiSysRegistrationSubByCode(String topsrcbilltype) throws Exception {
        String billTypeName = topsribillTypeCache.getIfPresent(topsrcbilltype);
        if (StringUtils.isNotEmpty(billTypeName)) {
            return billTypeName;
        }
        TmspBusiSysRegistrationSubResp responseVO = RemoteDubbo.get(ITmspBusiSysRegistrationRpcService.class, IDomainConstant.YONBIP_FI_CTMPUB).queryBusiSysRegistrationSubByCode(topsrcbilltype, null);
        if (responseVO == null || CollectionUtils.isEmpty(responseVO.getData())) {
            log.error("根据业务单据类型编码{}未查询到业务单据类型！", topsrcbilltype);
            return null;
        }
        String retBillTypeName;
        String locale = InvocationInfoProxy.getLocale();
        switch (locale) {
            case "en_US":
                retBillTypeName = responseVO.getData().get(0).getBillTypeName2();
                break;
            case "zh_TW":
                retBillTypeName = responseVO.getData().get(0).getBillTypeName3();
                break;
            default:
                retBillTypeName = responseVO.getData().get(0).getBillTypeName();
                break;
        }
        topsribillTypeCache.put(topsrcbilltype, retBillTypeName);
        return retBillTypeName;
    }


    /**
     * 根据业务系统编码查询业务系统注册信息
     *
     * @param topsrcitem  来源业务系统编码
     * @return 业务系统名称
     * @throws Exception
     */
    public String queryBusiSysRegistrationByCode(String topsrcitem) throws Exception {
        String topsrcitemName = topsrcitemCache.getIfPresent(topsrcitem);
        if (StringUtils.isNotEmpty(topsrcitemName)) {
            return topsrcitemName;
        }
        TmspBusiSysRegistrationResp responseVO = RemoteDubbo.get(ITmspBusiSysRegistrationRpcService.class, IDomainConstant.YONBIP_FI_CTMPUB).queryBusiSysRegistrationByCode(topsrcitem, null);
        if (responseVO == null || CollectionUtils.isEmpty(responseVO.getData())) {
            log.error("根据业务系统编码{}未查询到业务系统信息！", topsrcitem);
            return null;
        }
        String retTopSrcItemName;
        String locale = InvocationInfoProxy.getLocale();
        switch (locale) {
            case "en_US":
                retTopSrcItemName = responseVO.getData().get(0).getBusiSysName2();
                break;
            case "zh_TW":
                retTopSrcItemName = responseVO.getData().get(0).getBusiSysName3();
                break;
            default:
                retTopSrcItemName = responseVO.getData().get(0).getBusiSysName();
                break;
        }
        topsrcitemCache.put(topsrcitem, retTopSrcItemName);
        return retTopSrcItemName;
    }

}
