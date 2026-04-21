package com.yonyoucloud.fi.cmp.margintype.service.impl;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.margintype.MarginType;
import com.yonyoucloud.fi.cmp.margintype.service.MargintypeService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class MargintypeServiceImpl implements MargintypeService {
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private final String MARGINTYPELIST = "margintypelist";
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject enable(Long id, String code) throws Exception {
        String key = MARGINTYPELIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101672"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418003B","该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            MarginType marginType = MetaDaoHelper.findById(MarginType.ENTITY_NAME ,id);
            if(null == marginType){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101673"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508005D", "单据【%s】已删除，请刷新后重试") /* "单据【%s】已删除，请刷新后重试" */, code));
            }
            if(marginType.getIsEnabledType()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101674"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508005E", "单据【%s】状态为启用，不可重复启用！") /* "单据【%s】状态为启用，不可重复启用！" */ , marginType.getCode()));
            }
            marginType.setIsEnabledType(true);
            EntityTool.setUpdateStatus(marginType);
            MetaDaoHelper.update(MarginType.ENTITY_NAME,marginType);
            ctmcmpBusinessLogService.saveBusinessLog(marginType, code, "", IServicecodeConstant.CMP_MARGINTYPELIST, IServicecodeConstant.CMP_MARGINTYPELIST, IMsgConstant.CMP_ENABLE);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", marginType);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject unEnable(Long id, String code) throws Exception {
        String key = ICmpConstant.CMPBANKRECONCILIATIONLIST + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101190"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00143", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            MarginType marginType = MetaDaoHelper.findById(MarginType.ENTITY_NAME ,id);
            if(null == marginType){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101673"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508005D", "单据【%s】已删除，请刷新后重试") /* "单据【%s】已删除，请刷新后重试" */, code));
            }
            if(!marginType.getIsEnabledType()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101675"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508005F", "单据【%s】状态为停用，不可重复停用！") /* "单据【%s】状态为停用，不可重复停用！" */ , marginType.getCode()));
            }
            marginType.setIsEnabledType(false);
            EntityTool.setUpdateStatus(marginType);
            MetaDaoHelper.update(MarginType.ENTITY_NAME,marginType);
            ctmcmpBusinessLogService.saveBusinessLog(marginType, code, "", IServicecodeConstant.CMP_MARGINTYPELIST, IServicecodeConstant.CMP_MARGINTYPELIST, IMsgConstant.CMP_UNENABLE);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", marginType);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

}
