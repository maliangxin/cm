package com.yonyoucloud.fi.cmp.util;

import com.yonyoucloud.fi.api.AccControlScopeVOService;
import com.yonyoucloud.fi.api.AccountBookService;
import com.yonyoucloud.fi.api.AccountbookpropertyService;
import com.yonyoucloud.fi.api.IDoctypeService;
import com.yonyoucloud.fi.api.accountbook.IAccountBookCacheService;
import com.yonyoucloud.fi.api.accountbooktype.IAccountBookTypeService;
import com.yonyoucloud.fi.api.accsubject.IAccSubjectService;
import com.yonyoucloud.fi.api.accubjectgroup.IAccSubjectFactorGroup;
import com.yonyoucloud.fi.api.accubjectgroup.IAccSubjectGroup;
import com.yonyoucloud.fi.api.costcarry.ICostCarryService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author qihaoc
 * @Description: 财务公共接口工具类
 * @date 2024/6/5 20:48
 */

public class FIEPUBApiUtil {
    private static final Logger logger = LoggerFactory.getLogger(FIEPUBApiUtil.class);

    public static AccControlScopeVOService getAccControlScopeVOService() {
        try {
            return RemoteDubbo.get(AccControlScopeVOService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }


    public static AccountbookpropertyService getAccountbookpropertyService() {
        try {
            return RemoteDubbo.get(AccountbookpropertyService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }


    public static AccountBookService getAccountBookService() {
        try {
            return RemoteDubbo.get(AccountBookService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }

    public static IAccSubjectService getAccSubjectService() {
        try {
            return RemoteDubbo.get(IAccSubjectService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }


    public static IDoctypeService getIDoctypeService() {
        try {
            return RemoteDubbo.get(IDoctypeService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }

    public static IAccountBookCacheService getIAccountBookCacheService() {
        try {
            return RemoteDubbo.get(IAccountBookCacheService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }

    public static IAccSubjectFactorGroup getIAccSubjectFactorGroup() {
        try {
            return RemoteDubbo.get(IAccSubjectFactorGroup.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }

    public static IAccSubjectGroup getIAccSubjectGroup() {
        try {
            return RemoteDubbo.get(IAccSubjectGroup.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }

    public static IAccountBookTypeService getIAccountBookTypeService() {
        try {
            return RemoteDubbo.get(IAccountBookTypeService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }

    public static ICostCarryService getICostCarryService() {
        try {
            return RemoteDubbo.get(ICostCarryService.class, IDomainConstant.MDD_DOMAIN_FIEPUB);
        } catch (Exception e) {
            logger.error("message:", e);
            return null;
        }
    }
}