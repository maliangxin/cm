package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.poi.model.StreamParam;
import com.yonyou.ucf.mdd.ext.poi.service.POIService;
import com.yonyou.ucf.mdd.ext.poi.util.POIUtils;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-27-8:51
 */
@Slf4j
public class SAPUtils {
    private static final Log logger = LogFactory.getLog(POIService.class);
    private static String SHEET_SUFFIX_NAME = "SAP";
    private static final ThreadLocal<Map<String, Object>> sapexportcontext = new ThreadLocal<>();
    public static final String OrgsKey = "orgskey";
    public static final String ResonCode = "resonCode";
    public static final String BankSeqNo = "bank_seq_no";
    public static final String DcFlag = "dc_flag";


    public static void setSapexportContext(Map<String, Object>  map) {
        sapexportcontext.set(map);
    }

    public static Map<String, Object>  getSapexportContext() {
        return sapexportcontext.get();
    }

    public static void remove() {
        sapexportcontext.remove();
    }
    /**
     * 直接输出不生成文件
     * @param streamParam
     * @param cmpExportMapList
     * @throws Exception
     */
    public static void downLoadToResponse(StreamParam streamParam, List<CmpExportMap> cmpExportMapList) throws Exception{
        OutputStream outputStream=null;
        ByteArrayOutputStream byteArrayOutputStream=null;
        Excel2007Utils excel2007Utils=null;
        try {
            HttpServletResponse response=streamParam.getResponse();
            outputStream=response.getOutputStream();
            response.reset();
//			 response.setCharacterEncoding("UTF-8");
            String fileName = streamParam.getExcelExportData().getFileName()+SHEET_SUFFIX_NAME;
            String fileNameEncoded = POIUtils.encode(fileName, "UTF-8");
            response.setHeader("Content-disposition", "attachment; filename=" + fileNameEncoded +"." +streamParam.getFileExtension()+";" + "filename*=utf-8''" + fileNameEncoded+"." +streamParam.getFileExtension());
//            log.info("streamParam.getFileName{}", streamParam.getExcelExportData().getFileName());
//            log.info("streamParam.getFileExtension{}", streamParam.getFileExtension());
            response.setContentType("application/msexcel;charset=utf-8");
            if("xls".equalsIgnoreCase(streamParam.getFileExtension())) {
                byteArrayOutputStream = new com.yonyoucloud.fi.cmp.util.Excel2003Utils().export2SAPStream(streamParam.getExcelExportData(),cmpExportMapList);
            } else {
                excel2007Utils=new com.yonyoucloud.fi.cmp.util.Excel2007Utils();
                byteArrayOutputStream= excel2007Utils.export2SAPStream(streamParam.getExcelExportData(),cmpExportMapList);
            }
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
        } catch (Exception e) {
            log.info("[ExcelUtils] downLoadToResponse导出excel失败{}",e);
            throw e;
        }finally{
            if(byteArrayOutputStream!=null) byteArrayOutputStream.flush();
            if(outputStream!=null) outputStream.close();
            if(excel2007Utils!=null)excel2007Utils.dispose();
        }
    }
}
