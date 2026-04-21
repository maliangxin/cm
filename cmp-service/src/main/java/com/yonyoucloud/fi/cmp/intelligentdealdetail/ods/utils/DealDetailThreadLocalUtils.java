package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils;
import java.util.HashMap;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/3/8 16:54
 * @Description todo
 * @Version 1.0
 */
public class DealDetailThreadLocalUtils {
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();
    /**
     * 存储
     *
     * @param key
     * @param value
     * @return void [返回类型说明]
     * @author nijia
     * @see [类、类#方法、类#成员]
     */
    public static void put(String key, Object value) {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
        THREAD_LOCAL.set(map);
    }
    /**
     * 取值
     *
     * @param key
     * @return T [返回类型说明]
     * @returnt
     * @author nijia
     * @see [类、类#方法、类嗯#成员]
     */

    public static <T> T get(String key) {
        Map<String, Object> _map = THREAD_LOCAL.get();
        if (_map != null) {
            return (T) _map.get(key);
        }
        return null;
    }
    public static void release() {
        THREAD_LOCAL.remove();
    }
}