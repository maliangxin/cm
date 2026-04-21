package com.yonyoucloud.fi.cmp.export.commonevalrule;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-26-21:48
 */
public class DateChangeEvalRule implements ConditionHandler {
    @Override
    public Object handler(Object o1, Object o2, Object o3) throws Exception {
        if (!(o1 instanceof CmpExportMap)) {
            return "";
        }
        CmpExportMap cmpExportMap = (CmpExportMap) o1;
        String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
        String[] split = sourceEntityattrFactors.split(",");
        //目标格式
        String extra1 = cmpExportMap.getExtra1();
        if (StringUtils.isBlank(extra1)) {
            return "";
        }
        if (!(o2 instanceof Map)) {
            return "";
        }
        Map<String, Object> oldMap = (Map<String, Object>) o2;
        Object o = oldMap.get(split[0]);
        if (o == null) {
            return "";
        }
        if (!(o instanceof CellData)) {
            return "";
        }
        String dateFormat = ((CellData) o).getDateFormat();
        Object sourceDate = ((CellData) o).getValue();
        if (StringUtils.isNotBlank(dateFormat)) {
            if (sourceDate != null) {
                if (sourceDate instanceof Date) {
                    return DateUtils.convertToStr((Date) sourceDate, extra1);
                } else {
                    return convert2String(dateFormat, extra1, sourceDate);
                }
            }
        } else {
            //判断是不是日期如果是日期
            if (o instanceof Date) {
                return DateUtils.convertToStr((Date) o, extra1);
            } else {
                //原样输出
                if (sourceDate != null) {
                    return sourceDate.toString();
                }
            }
        }


        return "";
    }

    public static String convert2String(String sourceFormate, String targetformat, Object convertObect) throws ParseException {
        if (StringUtils.isNotBlank(targetformat) && StringUtils.isNotBlank(sourceFormate)) {
            if (convertObect instanceof String) {
                Date date = DateUtils.convertToDate(sourceFormate, (String) convertObect);
                String s = DateUtils.convertToStr(date, targetformat);
                return s;
            }
        }
        return "";
    }
}