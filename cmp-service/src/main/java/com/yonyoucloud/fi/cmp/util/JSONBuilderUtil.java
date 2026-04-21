package com.yonyoucloud.fi.cmp.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class JSONBuilderUtil {
    private static final String DATEFORMATE = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    public static final FastDateFormat DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    public static final ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;


    //public static class JacksonObjectMapper extends ObjectMapper {
    //    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    //    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    //    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    //
    //    public JacksonObjectMapper() {
    //        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //        this.getDeserializationConfig().withoutFeatures(new DeserializationFeature[]{DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES});
    //        SimpleModule simpleModule = (new SimpleModule()).addSerializer(Date.class, new DateSerializer(false, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))).addDeserializer(Date.class, new DateDeserializer(DateDeserializer.instance, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), "yyyy-MM-dd HH:mm:ss")).addSerializer(BigInteger.class, ToStringSerializer.instance).addSerializer(Long.class, ToStringSerializer.instance);
    //        this.registerModule(simpleModule);
    //    }
    //}
    //原初始化逻辑复制
    static {
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.getDeserializationConfig().withoutFeatures(new DeserializationFeature[]{DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES});
                SimpleModule simpleModule = (new SimpleModule()).addSerializer(BigInteger.class, ToStringSerializer.instance).addSerializer(Long.class, ToStringSerializer.instance);
                objectMapper.registerModule(simpleModule);
    }


    public JSONBuilderUtil() {
    }

    public static String toJSONstr(Object obj) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sf.format(obj);
    }

    public static String getTextValue(ObjectNode objectNode, String key) {
        ObjectNode objectNode1 = objectNode.with(key);
        return objectNode1.textValue();
    }

    public static Date asDate(final JsonNode data, final String key, final Timestamp defaultValue) {
        Timestamp ts = asTimestamp(data, key, defaultValue);
        return ts != null ? new Date((long)ts.getNanos()) : null;
    }

    public static Timestamp asTimestamp(final JsonNode data, final String key, final Timestamp defaultValue) {
        if (data == null) {
            return defaultValue;
        } else {
            JsonNode jsonNode = data.get(key);
            if (jsonNode == null) {
                return defaultValue;
            } else {
                Number numberValue = jsonNode.numberValue();
                if (numberValue != null) {
                    return new Timestamp(numberValue.longValue());
                } else {
                    String textValue = StringUtils.trimToEmpty(jsonNode.asText());
                    if (NumberUtils.isDigits(textValue)) {
                        return new Timestamp(NumberUtils.toLong(textValue, 0L));
                    } else {
                        if (textValue.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                            try {
                                return new Timestamp(DATE_TIME_FORMAT.parse(textValue).getTime());
                            } catch (ParseException var8) {
                            }
                        }

                        if (textValue.length() == "yyyy-MM-dd".length()) {
                            try {
                                return new Timestamp(DATE_FORMAT.parse(textValue).getTime());
                            } catch (ParseException var7) {
                            }
                        }

                        return defaultValue;
                    }
                }
            }
        }
    }

    public static ObjectNode createJson() {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    public static <T> T stringToBean(String json, Class<T> clazz) {
        T t = null;

        try {
            t = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException var4) {

        }

        return t;
    }

    public static Map<String, Object> stringToMap(String json) {
        Map<String, Object> map = null;

        try {
            map = (Map)objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException var3) {
        }

        return map;
    }

    public static <T> List<T> stringToBeanList(String json, Class<T> clazz) {
        List<T> t = null;

        try {
            t = (List)objectMapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException var4) {
        }

        return t;
    }

    public static JsonNode stringToJson(String json) {
        JsonNode jsonNode = null;

        try {
            jsonNode = objectMapper.readTree(json);
        } catch (JsonProcessingException var3) {
        }

        return jsonNode;
    }

    public static JsonNode beanToJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }

    public static String beanToString(Object obj) {
        return objectToString(obj);
    }

    public static Map<String, Object> beanToMap(Object obj) {
        return (Map)objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {
        });
    }

    public static <T> T jsonToBean(JsonNode jsonNode, Class<T> clazz) {
        return objectMapper.convertValue(jsonNode, clazz);
    }

    public static String jsonToString(JsonNode jsonNode) {
        return objectToString(jsonNode);
    }

    public static Map<String, Object> jsonToMap(JsonNode jsonNode) {
        return (Map)objectMapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>() {
        });
    }

    public static List<JsonNode> jsonToList(JsonNode jsonNode) {
        List<JsonNode> list = null;
        if (jsonNode.isArray()) {
            Iterator<JsonNode> iterator = jsonNode.elements();
            list = new ArrayList();
            iterator.forEachRemaining(list::add);
        }

        return list;
    }

    public static JsonNode mapToJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }

    public static <T> T mapToBean(Map map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    public static String mapToString(Map map) {
        return objectToString(map);
    }

    public static <T> List<JsonNode> beanListToJsonList(List<T> beanList) {
        return (List)beanList.stream().map(JSONBuilderUtil::beanToJson).collect(Collectors.toList());
    }

    public static <T> List<Map<String, Object>> beanListToMapList(List<T> beanList) {
        return (List)beanList.stream().map(JSONBuilderUtil::beanToMap).collect(Collectors.toList());
    }

    public static <T> List<T> jsonListToBeanList(List<JsonNode> jsonNodeList, Class<T> clazz) {
        return (List)jsonNodeList.stream().map((jsonNode) -> {
            return jsonToBean(jsonNode, clazz);
        }).collect(Collectors.toList());
    }

    public static <T> List<T> jsonListToBeanList(ArrayNode jsonNodeList, Class<T> clazz) {
        List<T> list = new ArrayList();
        jsonNodeList.forEach((jsonNode) -> {
            list.add(jsonToBean(jsonNode, clazz));
        });
        return list;
    }

    public static Object getValue(ObjectNode node) {
        if (node == null) {
            return null;
        } else if (node.getNodeType().equals(JsonNodeType.STRING)) {
            return node.asText();
        } else if (node.getNodeType().equals(JsonNodeType.NUMBER)) {
            return node.asDouble();
        } else {
            return node.getNodeType().equals(JsonNodeType.BOOLEAN) ? node.asBoolean() : null;
        }
    }

    public static List<Map<String, Object>> jsonListToMapList(List<JsonNode> jsonNodeList) {
        return (List)jsonNodeList.stream().map(JSONBuilderUtil::jsonToMap).collect(Collectors.toList());
    }

    public static List<JsonNode> mapListToJsonList(List<Map> mapList) {
        return (List)mapList.stream().map(JSONBuilderUtil::mapToJson).collect(Collectors.toList());
    }

    public static <T> List<T> mapListToBeanList(List<Map> mapList, Class<T> clazz) {
        return (List)mapList.stream().map((map) -> {
            return mapToBean(map, clazz);
        }).collect(Collectors.toList());
    }

    public static String objectToString(Object obj) {
        String json = null;

        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException var3) {
        }

        return json;
    }

    public static ArrayNode putArray(ObjectNode objectNode, String fieldname, List<String> list) {
        ArrayNode arrayNode = objectNode.putArray(fieldname);
        list.stream().forEach((t) -> {
            arrayNode.add(t);
        });
        return arrayNode;
    }

    public static ArrayNode putArray1(ObjectNode objectNode, String fieldname, List<JsonNode> list) {
        ArrayNode arrayNode = objectNode.putArray(fieldname);
        list.stream().forEach((t) -> {
            arrayNode.add(t);
        });
        return arrayNode;
    }
}