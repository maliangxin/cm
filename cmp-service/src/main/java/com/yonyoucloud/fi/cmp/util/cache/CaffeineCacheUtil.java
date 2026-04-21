//package com.yonyoucloud.fi.cmp.util.cache;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//import com.yonyoucloud.fi.cmp.common.CtmException;
//import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
//import lombok.NonNull;
//
//import java.time.Duration;
//import java.util.Map;
//
///**
// * 缓存工具类
// * @author maliangn
// * @date 2022-03-16
// *
// */
//public class CaffeineCacheUtil {
//
//    /*
//     结算方式缓存
//     */
//    private static final @NonNull Cache<String, Map<String, Object>> settlemodeCache = Caffeine.newBuilder()
//            .initialCapacity(100)
//            .maximumSize(1000)
//            .expireAfterWrite(Duration.ofMinutes(1))
//            .softValues()
//            .build();
//    public static Map<String, Object> getSettlemodeById(String settlemode) throws Exception {
//        return settlemodeCache.get(settlemode,(k)->{
//            Map<String, Object> settlemodeMap;
//            try {
//                settlemodeMap = QueryBaseDocUtils.querySettlementWayById(k);
//            } catch (Exception e) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102473"),"exception when query settlemode by id " + k, e);
//            }
//            return settlemodeMap;
//        });
//    }
//
//}
