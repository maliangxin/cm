package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils;

import yonyou.bpm.rest.utils.StringUtils;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author guoyangy
 * @Date 2024/10/12 9:19
 * @Description 数据库查询结果转pojo
 * @Version 1.0
 */
public class DbUtil{
    private static final char SEPARATOR = '_';
    public static <T> T toBean(Class<T> clazz, ResultSet rs) throws Exception{
        if(!rs.next()){
            return null;
        }
        T t = clazz.newInstance();
        ResultSetMetaData rsmd = rs.getMetaData();
        int len = rsmd.getColumnCount();
        Map<String, Method> mm = getSetMethod(clazz);
        for(int i = 1; i <= len; i++){
            String label = rsmd.getColumnLabel(i);
            Method m = mm.get(label);
            if(m!=null){
                Object value = rs.getObject(label);
                if(!Objects.isNull(value)){
                    m.invoke(t, value);
                }
            }
        }
        return t;
    }

    public static Map<String, Object> toMap(ResultSet rs) throws Exception{
        Map<String, Object> map = new HashMap<String, Object>();
        if(!rs.next()){
            return map;
        }
        ResultSetMetaData rsmd = rs.getMetaData();
        int len = rsmd.getColumnCount();
        for(int i = 1; i <= len; i++){
            String label = rsmd.getColumnLabel(i);
            map.put(lineToCamel(label,false), rs.getObject(label));
        }
        return map;
    }

    public static Map<String, Object> toStrMap(ResultSet rs) throws Exception{
        Map<String, Object> map = new HashMap<String, Object>();
        if(!rs.next()){
            return map;
        }
        ResultSetMetaData rsmd = rs.getMetaData();
        int len = rsmd.getColumnCount();
        for(int i = 1; i <= len; i++){
            String label = rsmd.getColumnLabel(i);
            Object obj = rs.getObject(label);
            if(obj instanceof java.sql.Timestamp){
                java.sql.Timestamp ts = (java.sql.Timestamp)obj;
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                map.put(lineToCamel(label,false), format.format(new Date(ts.getTime())));
            }else if(obj instanceof java.util.Date){
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                map.put(lineToCamel(label,false), format.format((Date)obj));
            }else{
                map.put(lineToCamel(label,false), rs.getString(label));
            }
        }
        return map;
    }

    public static <T> List<T> toList(Class<T> clazz, ResultSet rs) throws Exception{
        List<T> list = new ArrayList<T>();
        while(true){
            T t = toBean(clazz, rs);
            if(t==null){
                break;
            }
            list.add(t);
        }
        return list;
    }

    public static List toList(ResultSet rs) throws Exception {
        List list = new ArrayList();
        while(true){
            Map m = toMap(rs);
            if(m==null){
                break;
            }
            list.add(m);
        }
        return list;
    }

    public static List toStrList(ResultSet rs) throws Exception{
        List list = new ArrayList();
        while(true){
            Map m = toStrMap(rs);
            if(m==null){
                break;
            }
            list.add(m);
        }
        return list;
    }

    private static Map<String,Method> getSetMethod(Class<?> clazz){
        Map<String,Method> map = new HashMap<String, Method>();
        Method[] array = clazz.getMethods();
        int len = array.length;
        for(int i =0 ; i < len; i++){
            Method m = array[i];
            if(m.getName().startsWith("set")&& m.getParameterTypes().length==1){
                map.put(StringUtils.uncapitalize(m.getName().substring(3)), m);
            }
        }
        return map;
    }
    public static String lineToCamel(String str,boolean firstUpperCase){
        if(str==null || str.length() == 0){
            return str;
        }
        str = str.toLowerCase();
        int len = str.length();
        StringBuffer sb = new StringBuffer();
        boolean upperCase = firstUpperCase;
        for(int i = 0; i < len; i++){
            char c = str.charAt(i);
            if(c==SEPARATOR){
                upperCase = true;
                continue;
            }
            if(upperCase){
                sb.append(Character.toUpperCase(str.charAt(i)));
                upperCase = false;
            }else{
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }
    public static String camelToLine(String str) throws Exception{
        if(str==null || str.length() == 0){
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextIsUpperCase = false;
        int len = str.length();
        for(int i = 0; i < len; i++){
            char c = str.charAt(i);
            if(i<len-1){
                nextIsUpperCase = Character.isUpperCase(str.charAt(i+1));
            }else{
                nextIsUpperCase = false;
            }
            if(Character.isLowerCase(c)&& nextIsUpperCase){
                if(i<len-1){
                    sb.append(c).append(SEPARATOR).append(Character.toLowerCase(str.charAt(i+1)));
                    i++;
                }else{
                    sb.append(c).append(SEPARATOR);
                }

            }else if(Character.isUpperCase(c)&& !nextIsUpperCase && i > 0){
                sb.append(SEPARATOR).append(Character.toLowerCase(str.charAt(i)));
            }else{
                sb.append(Character.toLowerCase(c));
            }

        }
        return sb.toString();
    }
}