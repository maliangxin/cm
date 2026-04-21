package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.yms.lock.YmsLock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2023/11/14 10:42
 * @Description todo
 * @Version 1.0
 */
public class MddRuleUtils {

    private static final String YMSLOCK_KEY="ymsLock";
    /**
     * @param map 取值于规则链execute()中第二个形参map
     * @desc 前规则加锁后将ymslock传递给后规则
     * */
    public static void putYmsLock(Map<String,Object> map, YmsLock  ymsLock){
        Object o = map.get(YMSLOCK_KEY);
        if(o==null){
            List<YmsLock> ymsLockList = new ArrayList<>();
            ymsLockList.add(ymsLock);
            map.put(YMSLOCK_KEY,ymsLockList);
        }else{
            List<YmsLock> ymsLockList =(List<YmsLock>)o;
            ymsLockList.add(ymsLock);
        }
    }
    /**
     * @param map-mdd规则类execut()第二个形参map
     * @desc 取出并移除map中的ymslock
     * */
    public static List<YmsLock> getYmsLock(Map<String,Object> map){
       Object o=  map.remove(YMSLOCK_KEY);
       if(o !=null && o instanceof List){
           List<YmsLock> ymsLockList = (List<YmsLock>)o;
           return ymsLockList;
       }
       return null;
    }
}
