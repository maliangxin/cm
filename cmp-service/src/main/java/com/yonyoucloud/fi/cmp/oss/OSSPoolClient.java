package com.yonyoucloud.fi.cmp.oss;

import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.file.oss.FileType;
import com.yonyou.ucf.mdd.ext.util.file.oss.Object;
import com.yonyou.ucf.mdd.ext.util.file.oss.OssResult;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import static com.yonyou.ucf.mdd.ext.poi.constant.POIConstant.Export.EXPORT_EXCEL_OSS_DIR_CONFIG_KEY;

@Slf4j
@Component()
@RequiredArgsConstructor
public class OSSPoolClient {

    private final Object.IObject fileClient;

    private volatile boolean initialized = false;

    private final CooperationFileUtilService cooperationFileUtilService;

    private final CooperationFileService cooperationFileService;



    public String upload(byte[] fileContent, String fileName) throws Exception {
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        String path = UUID.randomUUID() + "." + suffix;

//        InputStream is = new ByteArrayInputStream(fileContent);
//        /* "开始上传" */
//        log.info(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000045631"));
//
//        String defaultDir = AppContext.getEnvConfig(EXPORT_EXCEL_OSS_DIR_CONFIG_KEY);
//
//        //尝试解决crc校验失败的问题, 通过关闭开关
////        if (fileClient instanceof AliOss && !initialized) {
////            try {
////                changeAliOssClientCrcConfiguration();
////            } catch (Throwable e) {
////                log.warn("exception when try to change ali oss client configuration by reflection", e);
////            }
////            initialized = true;
////        }
//
//        OssResult result = fileClient.upload(is, path, FileType.ATTACHMENT, defaultDir, null);
//
//        is.close();
//        /* "上传完成" */

        //不用mdd的方法，否则不支持ofd格式文件，直接调用协同文件服务
        String fileId = cooperationFileUtilService.uploadOfFileBytes(fileContent, path);

        log.info(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008C", "上传完成") /* "上传完成" */ + " with result {}", "上传完成");
        return fileId;
    }

    public byte[] download(String fileId) throws Exception {
//        String suffix = fileId.substring(fileId.lastIndexOf(".") + 1);
//        String fileName = UUID.randomUUID() + "." + suffix;
        log.info("try to download file with id {}", fileId);
        return cooperationFileUtilService.queryBytesbyFileid(fileId);
    }

    /**
     * 返回文件URL
     * @param fileId
     * @return
     * @throws Exception
     */
    public String getFullUrl(String fileId) throws Exception {
        //if(!fileId.startsWith("/")){
        //    fileId = "/" + fileId;
        //}
        return cooperationFileUtilService.getFileBucketUrl(fileId);
    }

       /**
    * @deprecated
    */
  @Deprecated
    public InputStream toInputStream(String fileId) throws Exception {
        try (InputStream in = fileClient.downloadFile(fileId, "")) {
            return in;
        }
    }

//    private void changeAliOssClientCrcConfiguration() throws IllegalAccessException {
//        // Field client = AliOss.class.getDeclaredField("client");
//        // 使用Spring下的ReflectionUtils工具类替代直接暴露的setAccessible方法，解决安全漏洞
//        Field client = ReflectionUtils.findField(AliOss.class, "client");
//        if (client == null) {
//            return;
//        }
//        ReflectionUtils.makeAccessible(client);
//        // client.setAccessible(true);
//        if (!OSSClient.class.isAssignableFrom(client.getType())) {
//            log.error("exception when try to change ali oss crc check config, skipped");
//            return;
//        }
//        OSSClient ossClient = (OSSClient) client.get(null);
//        if (ossClient == null) {
//            return;
//        }
//        ClientConfiguration conf = ossClient.getClientConfiguration();
//        conf.setCrcCheckEnabled(false);
//        log.info("successfully change oss client crc check mark to disable");
//    }


}
