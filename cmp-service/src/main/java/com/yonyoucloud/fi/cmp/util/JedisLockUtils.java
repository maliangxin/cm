package com.yonyoucloud.fi.cmp.util;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created  by xudy on 2019/8/6.
 */
@Slf4j
public class JedisLockUtils {

    /**
     * 加单据锁
     * @param key
     */
    public static YmsLock lockBillWithOutTrace(String key){
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,90, TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("分布式锁加锁失败",e);
        }
        return null;
    }

    public static YmsLock lockBillWithOutTraceWait(String key) {
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,90L,90L, TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("分布式锁加锁失败",e);
        }
        return null;

    }


    /**
     * 加单据锁根据超时时间
     * @param key
     */
    public static YmsLock lockBillWithOutTraceByTime(String key, int duration){
//        String ident = "yonbip-fi-ctmcmp—lockbill:"+AppContext.getTenantId()+key;
//        boolean res = AppContext.cache().setnx(ident, "ident", duration);//90秒
//        return res;

        try {
           YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,duration,TimeUnit.SECONDS);
           return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }
    public static YmsLock lockRuleWithOutTraceByTime(String key, int duration,Map<String,Object> map){
//        String ident = "yonbip-fi-ctmcmp—lockbill:"+AppContext.getTenantId()+key;
//        boolean res = AppContext.cache().setnx(ident, "ident", duration);//90秒
//        return res;

        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,duration,TimeUnit.SECONDS);
            MddRuleUtils.putYmsLock(map,ymsLock);
            return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }

    /**
     * 释放单据锁
     * @param ymsLock
     */
    public static void  unlockBillWithOutTrace(YmsLock ymsLock){
       CtmLockTool.releaseLock(ymsLock);
    }


    /**
     * mdd规则锁
     * @param key
     */
    public static YmsLock lockRuleWithOutTrace(String key, Map<String,Object> map){
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,90, TimeUnit.SECONDS);
            MddRuleUtils.putYmsLock(map,ymsLock);
            return ymsLock;
        } catch (Exception e) {
            log.error("分布式锁加锁失败",e);
        }
        return null;
    }
    /**
     * mdd规则锁
     * @param map
     */
    public static void unlockRuleWithOutTrace( Map<String,Object> map){
        List<YmsLock> ymsLockList = MddRuleUtils.getYmsLock(map);
        if(ymsLockList!=null&&ymsLockList.size()>0){
            for(YmsLock ymsLock:ymsLockList){
                CtmLockTool.releaseLock(ymsLock);
                ymsLock = null;
            }
        }

    }

    /**
     * 加组织锁
     * @param key
     */
    public static YmsLock  lockWithOutTrace(String key){
//        String ident = "yonbip-fi-ctmcmp—lock:"+AppContext.getTenantId()+key;
//        boolean res = AppContext.cache().setnx(ident, "ident", 300);//5分钟
//        return res;

        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,300L,TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }

    /**
     * 加任务锁
     * @param key
     */
    public static YmsLock  taskLockWithOutTrace(String key,int duration){
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key,duration,TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }
    /**
     * 释放组织锁
     * @param ymsLock
     */
    public static void  unlockWithOutTrace(YmsLock ymsLock){
//        String ident = "yonbip-fi-ctmcmp—lock:"+AppContext.getTenantId()+key;
//        String keys = AppContext.cache().get(ident);
//        if (keys != null) {
//            return AppContext.cache().del(new String[]{ident}) > 0L;
//        } else {
//            return false;
//        }
        CtmLockTool.releaseLock(ymsLock);
    }



    /**
     * 是否存在日结组织锁
     * @param accentity
     */
    public static void  isexistRjLock(String accentity){

//        String ident = "yonbip-fi-ctmcmp—lockRj:"+AppContext.getTenantId()+accentity;
//        String key = AppContext.cache().get(ident);
//        if (key != null) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101098"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CF","该组织下正在日结，请稍后再试！") /* "该组织下正在日结，请稍后再试！" */);
//        }
        String ident = "yonbip-fi-ctmcmp—lockRj:"+accentity;//@notranslate
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(ident,3600,TimeUnit.SECONDS);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101098"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CF","该组织下正在日结，请稍后再试！") /* "该组织下正在日结，请稍后再试！" */);
            } else {
                CtmLockTool.releaseLock(ymsLock);
            }
        } catch (Exception e) {
            log.error("加锁失败",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101098"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CF","该组织下正在日结，请稍后再试！") /* "该组织下正在日结，请稍后再试！" */);
        }
    }

    /**
     * 日结加组织锁
     * @param accentity
     */
    public static YmsLock  lockRjWithOutTrace(String accentity){
//        String ident = "yonbip-fi-ctmcmp—lockRj:"+AppContext.getTenantId()+accentity;
//        boolean res = AppContext.cache().setnx(ident, "ident", 3600);//一个小时
//        return res;
        String ident = "yonbip-fi-ctmcmp—lockRj:"+accentity;//@notranslate
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(ident,3600,TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }
    /**
     * 释放日结组织锁
     * @param ymsLock
     */
    public static void  unlockRjWithOutTrace(YmsLock ymsLock){
//        String ident = "yonbip-fi-ctmcmp—lockRj:"+AppContext.getTenantId()+accentity;
//        String key = AppContext.cache().get(ident);
//        if (key != null) {
//            return AppContext.cache().del(new String[]{ident}) > 0L;
//        } else {
//            return false;
//        }
        CtmLockTool.releaseLock(ymsLock);
    }



    /**
     * 是否存在银行对账组织锁
     * @param accentity
     */
    public static void  isexistDzLock(String accentity){
        String ident = "yonbip-fi-ctmcmp—lockyhdz:"+accentity;//@notranslate
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(ident,3600,TimeUnit.SECONDS);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
            } else {
                CtmLockTool.releaseLock(ymsLock);
            }
        } catch (Exception e) {
            log.error("加锁失败",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
        }
    }

    /**
     * 银行对账加组织锁
     * @param accentity
     */
    public static YmsLock lockDzWithOutTrace(String accentity){
        String ident = "yonbip-fi-ctmcmp—lockyhdz:"+accentity;//@notranslate
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(ident,3600,TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }
    /**
     * 释放银行对账组织锁
     * @param ymsLock
     */
    public static void unlockDzWithOutTrace(YmsLock ymsLock){
//        String ident = "yonbip-fi-ctmcmp—lockyhdz:"+accentity;
//        String key = AppContext.cache().get(ident);
//        if (key != null) {
//            return AppContext.cache().del(new String[]{ident}) > 0L;
//        } else {
//            return false;
//        }
        CtmLockTool.releaseLock(ymsLock);
    }


    /**
     * 是否存在匹配关联组织锁
     * @param accentity
     */
    public static void  isexistGlLock(String accentity){
//        String ident = "yonbip-fi-ctmcmp—lockDz:"+AppContext.getTenantId()+accentity;
//        String key = AppContext.cache().get(ident);
//        if (key != null) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
//        }
        String ident = "yonbip-fi-ctmcmp—lockDz:"+accentity;//@notranslate
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(ident,3600,TimeUnit.SECONDS);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
            } else {
                CtmLockTool.releaseLock(ymsLock);
            }
        } catch (Exception e) {
            log.error("加锁失败",e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D0","该组织下正在匹配关联，请稍后再试！") /* "该组织下正在匹配关联，请稍后再试！" */);
        }
    }

    /**
     * 匹配关联加组织锁
     * @param accentity
     */
    public static YmsLock  lockGlWithOutTrace(String accentity){
//        String ident = "yonbip-fi-ctmcmp—lockDz:"+AppContext.getTenantId()+accentity;
//        boolean res = AppContext.cache().setnx(ident, "ident", 3600);//一个小时
//        return res;
        String ident = "yonbip-fi-ctmcmp—lockDz:"+accentity;//@notranslate
        try {
            YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(accentity,3600,TimeUnit.SECONDS);
            return ymsLock;
        } catch (Exception e) {
            log.error("加锁失败",e);
        }
        return null;
    }
    /**
     * 释放匹配关联组织锁
     * @param ymsLock
     */
    public static void  unlockGlWithOutTrace(YmsLock ymsLock){
//        String ident = "yonbip-fi-ctmcmp—lockDz:"+AppContext.getTenantId()+accentity;
//        String key = AppContext.cache().get(ident);
//        if (key != null) {
//            return AppContext.cache().del(new String[]{ident}) > 0L;
//        } else {
//            return false;
//        }
        CtmLockTool.releaseLock(ymsLock);
    }


    /**
     * 预提规则设置单据启停用加锁
     *
     * @param key
     * @param rowData
     * @return
     */
    public static CtmJSONObject lockBill(String key, CtmJSONArray rowData){
//        CtmJSONObject lockResult = new CtmJSONObject();
//        String keyPrefix = ICmpConstant.KEY_PREFIX + key + AppContext.getTenantId();
//        for (int i = 0; i < rowData.size(); i++) {
//            boolean lock = AppContext.cache().setnx(keyPrefix + rowData.getJSONObject(i).get("id"), "ident", 60 * 30);
//            if (!lock) {
//                lockResult.put("dealSucceed", false);
//                lockResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CE","单据【") /* "单据【" */ + rowData.getJSONObject(i).get("id") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CD","】被锁定，请勿重复操作") /* "】被锁定，请勿重复操作" */);
//                unlockBill(key, rowData);
//                return lockResult;
//            }
//        }
//        lockResult.put("dealSucceed", true);
//        return lockResult;

        CtmJSONObject lockResult = new CtmJSONObject();
        String keyPrefix = ICmpConstant.KEY_PREFIX + key + AppContext.getTenantId();
        List<YmsLock> ymsLockList = new ArrayList<>();
        try{
            for (int i = 0; i < rowData.size(); i++) {
                YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(keyPrefix + rowData.getJSONObject(i).get("id"),60 * 3,TimeUnit.SECONDS);
                if (ymsLock == null) {
                    lockResult.put("dealSucceed", false);
                    lockResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CE","单据【") /* "单据【" */ + rowData.getJSONObject(i).get("id") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CD","】被锁定，请勿重复操作") /* "】被锁定，请勿重复操作" */);
                    unlockBill(ymsLockList);
                    return lockResult;
                }
                ymsLockList.add(ymsLock);
            }
            lockResult.put("dealSucceed", true);
            lockResult.put("ymsLockList", ymsLockList);

        }catch (Exception e){
            log.error(e.getMessage(),e);
            unlockBill(ymsLockList);
        }
        return lockResult;
    }

    /**
     * * 预提规则设置单据启停用解锁*
     *
     * @param ymsLockList
     */
    public static void unlockBill(List<YmsLock> ymsLockList) {
//        List<String> keys = new ArrayList<>();
//        String keyPrefix = ICmpConstant.KEY_PREFIX + key + AppContext.getTenantId();
//        for (int i = 0; i < rowData.size(); i++) {
//            keys.add(keyPrefix + rowData.getJSONObject(i).get("id"));
//        }
//        AppContext.cache().del(keys.toArray(new String[keys.size()]));

        if(!CollectionUtils.isEmpty(ymsLockList)){
            for(YmsLock ymsLock:ymsLockList){
                CtmLockTool.releaseLock(ymsLock);
            }
        }
    }


}
