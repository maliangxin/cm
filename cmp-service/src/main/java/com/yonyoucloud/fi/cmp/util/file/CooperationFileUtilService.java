package com.yonyoucloud.fi.cmp.util.file;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.fileservice.sdk.module.CooperationConvertFileService;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.iuap.fileservice.sdk.module.pojo.CooperationFileInfo;
import com.yonyou.iuap.fileservice.sdk.module.pojo.FileProperty;
import com.yonyou.iuap.fileservice.sdk.module.pojo.response.CommonResponse;
import com.yonyou.iuap.fileservice.sdk.module.pojo.response.UrlResponse;
import com.yonyou.iuap.fileservice.sdk.service.CooperationFileUploadService;
import com.yonyou.iuap.org.dto.TaxpayerOrgDTO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.UUID;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.EnvConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.HttpsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yonyou.iuap.fileservice.sdk.domain.UploadFileRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @Author: maliangn
 * @Description: 本类用于调用协同接口进行文件的上传下载
 * @Date: Created in  2022/8/4 13:57
 * @Version v1.0
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class CooperationFileUtilService {

    private final CooperationFileService cooperationFileService;
    private final CooperationConvertFileService cooperationConvertFileService;
    private final CooperationFileUploadService cooperationFileUploadService;

//    @Resource(name="initStreamRestTemplate")
//    private final RestTemplate defaultRestTemplate;
    //处理专属化环境内外网映射端口
    public static String special_port  = AppContext.getEnvConfig("special_port");
    public static String special_com = ".com";
    public static String special_cn = ".cn";
    //todo 上下文取的不对，从header里取？？？
    public static final String X_OUT_FORWARDED_HOST = "X-Out-Forwarded-Host";

    /**
     * 用于获取访问来源域名
     */
    public static final String HEADER_FROM_HOST_KEY = "X-Out-Forwarded-Host";
    /**
     * 用于获取访问来源协议
     */
    public static final String HEADER_FROM_PROTO_KEY = "X-Out-Forwarded-Proto";
    @Value("${domain.url}")
    public static String DOMAIN_URL;


    /**
     * 构建小写后缀的文件名
     *
     * @param fileName 原始文件名
     * @return 后缀转为小写的文件名
     */
    public static String buildLowerSuffixFileName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        String prefix = fileName.substring(0, lastDotIndex);
        String suffix = fileName.substring(lastDotIndex + 1);
        return prefix + "." + suffix.toLowerCase(Locale.ENGLISH);
    }

    /**
     * 构建大写后缀的文件名
     *
     * @param fileName 原始文件名
     * @return 后缀转为大写的文件名
     */
    public static String buildUpperSuffixFileName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        String prefix = fileName.substring(0, lastDotIndex);
        String suffix = fileName.substring(lastDotIndex + 1);
        return prefix + "." + suffix.toUpperCase(Locale.ENGLISH);
    }

    public Object getPublicDomainUrl() {
        //内外网适配时，外网的请求从请求头里可以获取外网域名
        //HttpServletRequest request
        //String schema = request.getHeader(HEADER_FROM_PROTO_KEY);
        //String host = request.getHeader(HEADER_FROM_HOST_KEY);
        //平台刘诗函提供的获取方法，多外网域名时，有个R6客户原来从上下文中获取的域名不对，故优先使用此方法
        String outForwardedHost = InvocationInfoProxy.getOutForwardedHost();
        String outForwardedProto = InvocationInfoProxy.getOutForwardedProto();
        if (StringUtils.isNotBlank(outForwardedProto) && StringUtils.isNotBlank(outForwardedHost)) {
            return outForwardedProto + "://" + outForwardedHost;
        } else  {
            return InvocationInfoProxy.getExtendAttribute(X_OUT_FORWARDED_HOST);
        }
    }

    /**
     * 用于调用协同方法上传文件
     * @param file
     * @return
     */
    public String uploadOfFile(File file){
        String fileid = null;
        log.error("start upload file, filename is "+file.getName());
        CooperationFileInfo cooperationFileInfo = cooperationFileService.uploadFile(ICmpConstant.APPCODE,null,file,null);
        if(cooperationFileInfo != null){
            fileid = cooperationFileInfo.getFileId();
            log.error("upload file end, fileid is "+fileid);
        }
        return fileid;
    }


    /**
     * 用于调用协同方法上传文件
     * @param fileBytes
     * @return
     */
    public String uploadOfFileBytes(byte[] fileBytes, String fileName){
        String fileid = null;
        log.error("start upload file, filename is ");
        FileProperty fileProperty = FileProperty.builder().tenantId(InvocationInfoProxy.getTenantid()).yhtUserId(InvocationInfoProxy.getUserid()).build();
        //todo 业务标识应该传单据id？？？
        //CooperationFileInfo cooperationFileInfo = cooperationFileService.uploadFile(ICmpConstant.APPCODE, UUID.randomUUID().toString(),
        //        fileBytes,fileName,fileProperty);

        //旧接口会上传到公有桶[链接不过期]，平台安全要求上传到私有桶[链接会过期]
        CooperationFileInfo cooperationFileInfo = cooperationFileService.uploadFile(ICmpConstant.APPCODE, UUID.randomUUID().toString(), new ByteArrayInputStream(fileBytes), fileName, true, fileProperty);


        if(cooperationFileInfo != null){
            fileid = cooperationFileInfo.getFileId();
            log.error("upload file end, fileid is "+fileid);
        }
        return fileid;
    }

    /**
     * 用于调用协同方法获取文件url
     * @param fileIds
     * @return
     */
    public String queryDownloadUrl(List<String> fileIds){
        String fileUrl = "";
        if(CollectionUtils.isNotEmpty(fileIds)){
            if(fileIds.size() == 1){
                //单条接口链接里自带上下文信息，可以直接下载
                fileUrl = cooperationFileService.queryDownloadUrl(fileIds.get(0));
                //处理专属化环境内外网映射
                String newfileUrl = buildSpecialUrl( fileUrl);
                return newfileUrl;
            }
            //批量接口需要传上下文，实际走不到这块，否则没有上下文时调用会报错
            return cooperationFileService.queryBatchDownloadUrl(fileIds);
        }
        return fileUrl;
    }

    /**
     * 用于调用协同方法获取文件内网的url，直接在服务器上下载文件时使用，不做内外网转换
     * @param fileIds
     * @return
     */
    public String queryInnerDownloadUrl(List<String> fileIds){
        String fileUrl = "";
        if(CollectionUtils.isNotEmpty(fileIds)){
            if(fileIds.size() == 1){
                //单条接口链接里自带上下文信息，可以直接下载
                fileUrl = cooperationFileService.queryDownloadUrl(fileIds.get(0));
                return fileUrl;
            }
            //批量接口需要传上下文，实际走不到这块，否则没有上下文时调用会报错
            return cooperationFileService.queryBatchDownloadUrl(fileIds);
        }
        return fileUrl;
    }

    /**
     * 如果私有云项目有映射端口 这里进行组装。参考https://gfwiki.yyrd.com/pages/viewpage.action?pageId=97464104
     * @param fileUrl
     * @return
     */
    public String buildSpecialUrl(String fileUrl) {
        if (StringUtils.isEmpty(fileUrl)) {
            return fileUrl;
        }
        int firstIndex = fileUrl.indexOf("/");
        if (firstIndex < 0 || firstIndex + 2 >= fileUrl.length()) {
            return fileUrl;
        }
        // 查找域名后的第一个"/"（跳过"//"协议分隔符）
        int domainEndIndex = fileUrl.indexOf("/", firstIndex + 2);
        if (domainEndIndex < 0) {
            return fileUrl;
        }
        // 获取旧有fileUrl的域名前缀
        String oldDomainUrl = fileUrl.substring(0, domainEndIndex);
        // 需要先判断原来的地址域名是否是BIP域名，否则不修改
        Object publicDomainUrl = getPublicDomainUrl();
        if (StringUtils.isNotBlank(DOMAIN_URL)
                && oldDomainUrl.equals(DOMAIN_URL)
                && publicDomainUrl instanceof String && StringUtils.isNotBlank(publicDomainUrl.toString())) {
            // 外网来的请求，域名替换为外网域名
            return fileUrl.replace(oldDomainUrl, publicDomainUrl.toString());
        }
        return fileUrl;
    }

    public InputStream queryInputStreamByFileId(String fileId) {
        InputStream inputStream = null; // 定义返回文件流
        String fileUrl = null ;
        if (StringUtils.isNotBlank(fileId)) {
            fileUrl = cooperationFileService.queryDownloadUrl(fileId);
        }
        if (StringUtils.isNotBlank(fileUrl)) {
            try {
                // 发送httpget请求获取文件流
                inputStream =  HttpsUtils.queryInputStreamByFileUrl(fileUrl);
            } catch (Exception e) {
                log.error("CooperationFileUtilService通过文件id获取文件流失败"+e);
            }
        }
        return inputStream;
    }


    /**
     * 通过文件id获取字节流
     *
     * @param fileId
     * @return
     */
    public byte[] queryBytesbyFileid(String fileId) {
        if (StringUtils.isNotBlank(fileId)) {
            try {
                ////用url下载，内外网不通时有问题，改为调用协同接口获取字节流
                //CooperationFileInfo cooperationFileInfo = cooperationFileService.queryFileInfo(fileId);
                //byte[] bytesByfilepath = cooperationFileService.getBytesByfilepath(cooperationFileInfo.getFilePath());
                //return bytesByfilepath;

                List<String> extendssList = new ArrayList<String>();
                extendssList.add(fileId);
                String url = queryInnerDownloadUrl(extendssList);
                URL fileurl = new URL(url);
                URLConnection connection = fileurl.openConnection();
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] byteArray = buffer.toByteArray();
                return byteArray;
            } catch (Exception e) {
                log.error("通过文件id获取文件失败",e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D2", "通过文件id获取文件失败！") /* "通过文件id获取文件失败！" */ + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 根据文件id 获取预览的url*
     * @param fileId
     * @return
     */
    public String queryPreviewUrlById(String fileId){
        String tenantId = AppContext.getCurrentUser().getYTenantId();
        String userId = InvocationInfoProxy.getUserid();
        String previewUrl = "";
        UrlResponse urlResponse = cooperationFileService.queryPreviewUrlById(fileId, tenantId, userId, "1");
        if (ObjectUtils.isNotEmpty(urlResponse)) {
            previewUrl = buildSpecialUrl(urlResponse.getUrl());
        }
        return previewUrl;

    }


    /**
     * 用于调用协同方法文件永久url（慎用）
     * @param fileId
     * @return
     */
    public String queryprivilegeRealDownloadUrl(String fileId) {
        String fileUrl = "";
        if (StringUtils.isNotEmpty(fileId)) {
            //CommonResponse commonResponse = cooperationFileService.privilegeRealDownloadUrl(ICmpConstant.APPCODE, fileId, AppContext.getTenantId().toString(), AppContext.getCurrentUser().getId().toString()
            //modfiy by lichaor 20250519 按照马良要求，去除特权获取协同文件下载地址接口，和协同同事沟通，改成下边的url
            fileUrl = cooperationFileService.queryDownloadUrl(fileId);
        }
        return fileUrl;
    }


    /**
     * 根据协同云文件id批量获取桶地址
     * @param fileIds 协同云文件id集合
     * @return
     */
    public Map<String, String> getFileBucketUrls(List<String> fileIds) {
        Map<String, String> fileUrlMap = new HashMap<>();
        List<CooperationFileInfo> fileInfoList = cooperationFileService.batchQueryFileInfos(fileIds);
        if (CollectionUtils.isEmpty(fileInfoList)) {
            return fileUrlMap;
        }
        for (CooperationFileInfo fileInfo : fileInfoList) {
            fileUrlMap.put(fileInfo.getFileId(),buildSpecialUrl(fileInfo.getBucketUrl()));
        }
        return fileUrlMap;
    }

    /**
     * 获取文件桶地址
     * @param fileId 协同云文件id
     * @return
     */
    public String getFileBucketUrl(String fileId) {
        CooperationFileInfo cooperationFileInfo = cooperationFileService.queryFileInfoWithThumb(fileId, false);
        if (cooperationFileInfo == null) {
            log.error("查询协同云服务异常");
            return null;
        }
        return buildSpecialUrl(cooperationFileInfo.getBucketUrl());
    }

    /**
     * 通过文件id获取字节流
     *
     * @param url 文件桶地址
     * @return
     */
    public byte[] queryBytesbyFileidNew(String url) {
        if (StringUtils.isNotBlank(url)) {
            try {
                URL fileurl = new URL(url);
                URLConnection connection = fileurl.openConnection();
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] byteArray = buffer.toByteArray();
                return byteArray;
            } catch (IOException e) {
                log.error("读取文件流异常" + e.getMessage() + "url:" + url,e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D0", "读取文件流异常！") /* "读取文件流异常！" */ + e.getMessage()  + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D1", "请联系运维检查以下地址能否正常访问:") /* "请联系运维检查以下地址能否正常访问:" */ + url, e);
            }
        }
        return null;
    }

    /**
     * 新版协同云上传文件方法
     * @param inputStream
     * @param fileName
     * @return
     */
    public String uploadInputStream(InputStream inputStream, String fileName) {
        UploadFileRequest request = new UploadFileRequest();
        request.setBusinessType(ICmpConstant.APPCODE);
        request.setBusinessId(fileName);
        request.setStream(inputStream);
        request.setFileName(fileName);
        request.setUsePrivateBucket(true);
        CooperationFileInfo fileInfo = cooperationFileUploadService.uploadFile(request);
        return fileInfo.getFileId();
    }

    public static String getFileType(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

}
