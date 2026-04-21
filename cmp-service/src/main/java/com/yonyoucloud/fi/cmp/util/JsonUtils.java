package com.yonyoucloud.fi.cmp.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yonyou.ucf.mdd.ext.exceptions.BusinessException;
import com.yonyou.yonbip.ctm.utils.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于Jackson的JSON转换工具类
 * 默认转换 ArrayList， LinkedHashMap
 */
public class JsonUtils {

    private static ObjectMapper om = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonUtils.class);

    static {

        // 对象的所有字段全部列入，还是其他的选项，可以忽略null等
        om.setSerializationInclusion(Include.ALWAYS);
        // 设置Date类型的序列化及反序列化格式
        om.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 忽略空Bean转json的错误
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 忽略未知属性，防止json字符串中存在，java对象中不存在对应属性的情况出现错误
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 设置数字丢失精度问题
        om.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        // 注册一个时间序列化及反序列化的处理模块，用于解决jdk8中localDateTime等的序列化问题
//        om.registerModule(new JavaTimeModule());
    }

    /**
     * 对象 => json字符串
     *
     * @param obj 源对象
     */
    public static <T> String toJsonString(T obj) {

        String json = null;
        if (obj != null) {
            try {
                json = om.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(999L,e.getMessage(),"070-501-302551", 0, null, null);
            }
        }
        return json;
    }

    /**
     * json字符串 => 对象
     *
     * @param json  源json串
     * @param clazz 对象类
     * @param <T>   泛型
     */
    public static <T> T parse(String json, Class<T> clazz) {

        return parse(json, clazz, null);
    }

    /**
     * object => Map
     * 入参支持string、map、bean
     *
     * @param object 源object
     */
    public static Map ConvertObject2Map(Object object)  {
        if (object instanceof  Map) {
            return (Map) object;
        } else if (object instanceof String) {
            return (Map) parse((String) object, Map.class);
        } else {
            try {
                return bean2Map(object);
            }catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(999L,e.getMessage(),"070-501-302686", 0, null, null);
            }
        }
    }

    /**
     * Map => bean
     * 入参支持string、map、bean
     *
     * @param object 源object
     */
    public static <T> T ConvertObject2Bean(Object object, Class<T> clazz) {
        if (object.getClass().isInstance(clazz)) {
            return (T) object;
        } else if (object instanceof String) {
            return (T) parse((String) object, clazz);
        } else if (object instanceof  Map) {
            try {
               return   map2bean((Map)object,clazz);
            }catch (IllegalAccessException | InstantiationException e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(999L,e.getMessage(),"070-501-302550", 0, null, null);
            }
        } else {
            throw new BusinessException(999L,"Other types are not currently supported","070-501-302351", 0, null, null);
        }
    }

    /**
     * json字符串 => 对象
     *
     * @param json 源json串
     * @param type 对象类型
     * @param <T>  泛型
     */
    public static <T> T parse(String json, TypeReference type) {

        return parse(json, null, type);
    }


    /**
     * json => 对象处理方法
     * 参数clazz和type必须一个为null，另一个不为null
     * 此方法不对外暴露，访问权限为private
     *
     * @param json  源json串
     * @param clazz 对象类
     * @param type  对象类型
     * @param <T>   泛型
     */
    private static <T> T parse(String json, Class<T> clazz, TypeReference type) {

        T obj = null;
        if (StringUtils.hasLength(json)) {
            try {
                if (clazz != null) {
                    obj = om.readValue(json, clazz);
                } else {
                    obj = (T) om.readValue(json, type);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(999L,e.getMessage(),"070-501-302548", 0, null, null);
            }
        }
        return obj;
    }


    /**
     * JavaBean与Map互转
     * @param object
     * @return
     * @throws IllegalAccessException
     */
    public static Map<String, Object> bean2Map(Object object) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Object> data = BeanUtils.describe(object);
//        Class<?> clazz = object.getClass();

//        //UKC-86916 checkmax设置最大循环次数
//        for (int i = 0; clazz != Object.class && i < 20; clazz = clazz.getSuperclass(),i++) {
//            for (Field field : clazz.getDeclaredFields()) {
//                field.setAccessible(true);
//                data.put(field.getName(),field.get(object));
//            }
//        }
        return data;
    }

    /**
     * JavaBeans与List<Map>互转
     * @param objects
     * @return
     * @throws IllegalAccessException
     */
    public static List<Map> beans2Maps(List<?> objects) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<Map> ret = new ArrayList();
        if(CollectionUtils.isEmpty(objects)){
            return ret;
        }
        for (Object object : objects) {
            ret.add(bean2Map(object));
        }
        return ret;
    }

    /**
     * JavaBean与Map互转
     * @param map
     * @param benaClass
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static <T> T  map2bean(Map map, Class<T> benaClass) throws IllegalAccessException, InstantiationException {
        T object = benaClass.newInstance();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
           int mod = field.getModifiers();
           if(Modifier.isStatic(mod) || Modifier.isFinal(mod)){
               continue;
           }
           field.setAccessible(true);
           if(map.containsKey(field.getName())){
               field.set(object,map.get(field.getName()));
           }
        }
        return object;
    }

}