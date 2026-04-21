package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CacheUtils
 * @Desc 缓存工具类
 * @Author tongyd
 * @Date 2019/9/11
 * @Version 1.0
 */
public class CacheUtils {

    public static boolean lockRowData(Object id){
        String keyPrefix = "yonbip-fi-ctmcmp—lockbill:" + AppContext.getTenantId();//@notranslate
        return AppContext.cache().setnx(keyPrefix + id, "ident", 60 * 30);
    }

    /*
     * @Author tongyd
     * @Description 锁定单据
     * @Date 2019/9/11
     * @Param [rowData]
     * @return com.alibaba.fastjson.JSONObject
     **/
    public static CtmJSONObject lockBill(CtmJSONArray rowData){
        CtmJSONObject lockResult = new CtmJSONObject();
        String keyPrefix = "yonbip-fi-ctmcmp—lockbill:" + AppContext.getTenantId();//@notranslate
        for (int i = 0; i < rowData.size(); i++) {
            boolean lock = AppContext.cache().setnx(keyPrefix + rowData.getJSONObject(i).get("id"), "ident", 60 * 30);
            if (!lock) {
                lockResult.put("dealSucceed", false);
                lockResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180266","单据【") /* "单据【" */ + rowData.getJSONObject(i).get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180265","】被锁定，请勿重复操作") /* "】被锁定，请勿重复操作" */);
                return lockResult;
            }
        }
        lockResult.put("dealSucceed", true);
        return lockResult;
    }

    /*
     * @Author tongyd
     * @Description 解锁单据
     * @Date 2019/9/12
     * @Param [rowData]
     * @return void
     **/
    public static void unlockBill(CtmJSONArray rowData){
        List<String> keys = new ArrayList<>();
        String keyPrefix = "yonbip-fi-ctmcmp—lockbill:" + AppContext.getTenantId();//@notranslate
        for (int i = 0; i < rowData.size(); i++) {
            keys.add(keyPrefix + rowData.getJSONObject(i).get("id"));
        }
        AppContext.cache().del(keys.toArray(new String[keys.size()]));
    }

    public static void unlockRowData(Object id){
        String keyPrefix = "yonbip-fi-ctmcmp—lockbill:" + AppContext.getTenantId();//@notranslate
        AppContext.cache().del(keyPrefix + id);
    }

    /*
     * @Author ghwd
     * @Description 锁定单据
     * @Date 2019/9/11
     * @Param [rowData]
     * @return com.alibaba.fastjson.JSONObject
     **/
    public static CtmJSONObject lockBill(String key,CtmJSONArray rowData){
        CtmJSONObject lockResult = new CtmJSONObject();
        CtmJSONArray hasLockRowData = new CtmJSONArray();
        CtmJSONArray lockRowData = new CtmJSONArray();
        String keyPrefix = "yonbip-fi-ctmcmp—lockbill:" + AppContext.getTenantId();//@notranslate
        //保存已经上锁，进行清空
        List<String> saveKeys = new ArrayList<>();
        for (int i = 0; i < rowData.size(); i++) {
            boolean lock = AppContext.cache().setnx(keyPrefix + rowData.getJSONObject(i).get("id"), "ident", 60 * 5);
            if (!lock) {
                //存在上锁，都失败，清除锁
                hasLockRowData.add(rowData.getJSONObject(i));
            }else{
                saveKeys.add(keyPrefix + rowData.getJSONObject(i).get("id"));
                lockRowData.add(rowData.getJSONObject(i));
            }
        }
        lockResult.put("hasLockRowData", hasLockRowData);
        lockResult.put("lockRowData", lockRowData);
        if(hasLockRowData.size()==rowData.size()){
            lockResult.put("dealSucceed", false);
            lockResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180264","单据都被锁定，请勿重复操作") /* "单据都被锁定，请勿重复操作" */);
            return lockResult;
        }else{
            lockResult.put("dealSucceed", true);
            return lockResult;
        }

    }

    /*
     * @Author ghwd
     * @Description 解锁单据
     * @Date 2019/9/12
     * @Param [rowData]
     * @return void
     **/
    public static void unlockBill(String key,CtmJSONArray rowData){
        List<String> keys = new ArrayList<>();
        String keyPrefix = "yonbip-fi-ctmcmp—lockbill:" + AppContext.getTenantId();//@notranslate
        for (int i = 0; i < rowData.size(); i++) {
            keys.add(keyPrefix + rowData.getJSONObject(i).get("id"));
        }
        AppContext.cache().del(keys.toArray(new String[keys.size()]));
    }
}
