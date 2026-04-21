package com.yonyoucloud.fi.cmp.util;

import com.alibaba.fastjson.JSON;
import com.itrus.util.Base64;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.http.*;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.httpclient.CtmHttpClientTool;
import com.yonyou.yonbip.ctm.util.httpclient.response.CtmYmsHttpResponse;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.EnvConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.https.utils.TransErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @ClassName HttpsUtils
 * @Description https请求工具类
 * @Author tongyd
 * @Date 2019/5/9 9:54
 * @Version 1.0
 **/
@Slf4j
@Component
public class HttpsUtils {

    private static final List<String> WHITEURL_LIST = new ArrayList<>();

    private static final boolean saveYQLBusinessLog = EnvConstant.saveYQLBusinessLogc;

    private static final boolean YQLTestData = EnvConstant.YQLTestData;


    static {
        WHITEURL_LIST.add(AppContext.getBean(BankConnectionAdapterContext.class).getChanPayUri());
    }

    /*
     *@Author tongyd
     *@Description 通过https发送post请求
     *@Date 2019/5/13 17:42
     *@Param [transCode, requestData, uri]
     *@Return com.alibaba.fastjson.CtmJSONObject
     **/

    public static CtmJSONObject doHttpsPost(String transCode, List<BasicNameValuePair> requestData, String uri) throws IOException {
        Map reqMap = analysisRequestData(requestData);
        log.error("【" + transCode + "】" + "推送报文", reqMap);
        CtmJSONObject result = new CtmJSONObject();
        try {
            //uri取法和调用处一样，没有必要，而且是类初始化时就固定死了，改了后就不对了
            //if (isWhiteUrl(uri)) {
                YmsHttpMultipartEntity entity = YmsHttpMultipartEntityBuilder.create()
                        .addStringBody("reqData", reqMap.get("reqData").toString())
                        .addStringBody("reqSignData", reqMap.get("reqSignData").toString())
                        .setContentType(YmsHttpContentType.APPLICATION_JSON)
                        .build();
                YmsHttpRequest request = new YmsHttpRequestBuilder().url(uri)
                        .method(YmsHttpMethod.POST)
                        .multipartEntity(entity)
                        .build();
                request.getHeader().delete("Content-Type");
                request.setHeader("Content-Type","multipart/form-data");
               CtmYmsHttpResponse ctmYmsHttpResponse = CtmHttpClientTool.execute(request);
                int status = ctmYmsHttpResponse.getYmsHttpResponse().getStatusCode();
                if (status != 200) {
                    //0为失败，1为成功
                    result.put("code", 0);
                    result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056A", "银企联云响应异常：") /* "银企联云响应异常：" */ + ctmYmsHttpResponse.getYmsHttpResponse().getStatusText());
                    return result;
                }
                String responseData = ctmYmsHttpResponse.getYmsHttpResponse().getBodyString(Charset.forName("UTF-8"));
                log.info("【" + transCode + "】" + "反馈报文：{}", responseData);
                if (responseData.contains("error-code")) {
                    CtmJSONObject error = CtmJSONObject.parseObject(responseData);
                    //0为失败，1为成功
                    result.put("code", error.getString("error-code"));
                    result.put("message", error.getString("error-desc"));
                    return result;
                }
                String[] dataAndSign = responseData.split("&signature=");
                //若签名截取后长度为1，则提示客户签名为空
                if(dataAndSign.length == 1){
                    result.put("code", TransErrorInfo.ERROR_SIGN_EMPTY.getErrorCode());
                    result.put("message",TransErrorInfo.ERROR_SIGN_EMPTY.getErrorMessage());
                    return result;
                }
                String data = dataAndSign[0].substring(dataAndSign[0].indexOf("data=") + 5);
                if (YQLTestData) {
                    data = YQLUtils.getYQLTestData(transCode, data, requestData);
                    //测试数据时，不验签
                } else {
                    //非测试数据时，才验签
                    String signature = dataAndSign[1];
                    BankConnectionAdapterContext bankConnectionAdapterContext = AppContext.getBean(BankConnectionAdapterContext.class);
                    if (!bankConnectionAdapterContext.chanPayVerifySignature(data, signature)) {
                        //0为失败，1为成功
                        result.put("code", 0);
                        result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400569", "银企联云返回报文验签失败") /* "银企联云返回报文验签失败" */);
                        return result;
                    }
                }
                //0为失败，1为成功
                result.put("code", 1);
                result.put("message", TransErrorInfo.SUCCESS.getErrorMessage());
                result.put("data", data);
            //}
        } catch (Exception e) {
            log.error("银企联云连接异常,请求报文={},错误信息={}", reqMap.get("reqData").toString(),e);
            //0为失败，1为成功
            result.put("code", 0);
            result.put("message", TransErrorInfo.ERROR_NET_ERROR.getErrorMessage() + "[error]" + e.getMessage());
        }
        return result;
    }

    public static CtmJSONObject doHttpsPostNew(String transCode, List<BasicNameValuePair> requestData, String uri) {
        Map reqMap = analysisRequestData(requestData);
        log.error("【" + transCode + "】" + "推送报文", reqMap);
        CtmJSONObject result = new CtmJSONObject();
        try {
            //if (isWhiteUrl(uri)) {
                YmsHttpMultipartEntity entity = YmsHttpMultipartEntityBuilder.create()
                        .addStringBody("reqData", reqMap.get("reqData").toString())
                        .addStringBody("reqSignData", reqMap.get("reqSignData").toString())
                        .setContentType(YmsHttpContentType.APPLICATION_JSON)
                        .build();

                YmsHttpRequest request = new YmsHttpRequestBuilder().url(uri)
                        .method(YmsHttpMethod.POST)
                        .multipartEntity(entity)
                        .build();
                request.getHeader().delete("Content-Type");
                request.setHeader("Content-Type", "multipart/form-data");

                CtmYmsHttpResponse response = CtmHttpClientTool.execute(request);
                int status = response.getYmsHttpResponse().getStatusCode();
                if (status != 200) {
                    //0为失败，1为成功
                    result.put("code", 0);
                    result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180117", "调用畅捷支付响应异常：【") /* "调用畅捷支付响应异常：【" */ + status + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180116", "】") /* "】" */ + response.getYmsHttpResponse().getStatusText());
                    return result;
                }
                String responseData = response.getYmsHttpResponse().getBodyString(Charset.forName("UTF-8"));
                log.error("【" + transCode + "】" + "反馈报文：{}", responseData);
                if (responseData.contains("error-code")) {
                    CtmJSONObject error = CtmJSONObject.parseObject(responseData);
                    //0为失败，1为成功
                    result.put("code", error.getString("error-code"));
                    result.put("message", error.getString("error-desc"));
                    return result;
                }
                String[] dataAndSign = responseData.split("&signature=");
                //若签名截取后长度为1，则提示客户签名为空
                if(dataAndSign.length == 1){
                    //0为失败，1为成功
                    result.put("code", 0);
                    result.put("message",TransErrorInfo.ERROR_SIGN_EMPTY.getErrorMessage());
                    return result;
                }
                String data = dataAndSign[0].substring(dataAndSign[0].indexOf("data=") + 5);
                if (YQLTestData) {
                    data = YQLUtils.getYQLTestData(transCode, data, requestData);
                    //测试数据时，不验签
                } else {
                    //非测试数据时，才验签
                    String signature = dataAndSign[1];
                    BankConnectionAdapterContext bankConnectionAdapterContext = AppContext.getBean(BankConnectionAdapterContext.class);
                    if (!bankConnectionAdapterContext.chanPayVerifySignature(data, signature)) {
                        //0为失败，1为成功
                        result.put("code", 0);
                        result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180118", "畅捷支付返回报文验签失败") /* "畅捷支付返回报文验签失败" */);
                        return result;
                    }
                }
                //0为失败，1为成功
                result.put("code", 1);
                result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180119", "畅捷支付交易成功") /* "畅捷支付交易成功" */);
                result.put("data", data);
            //} else {
            //
            //}
        } catch (Exception e) {
            log.error("畅捷支付连接异常", e.getMessage());
            //0为失败，1为成功
            result.put("code", 0);
            result.put("message", TransErrorInfo.ERROR_NET_ERROR.getErrorMessage() + "[error]" + e.getMessage());
        }
        return result;
    }

    public static CtmJSONObject doCommonRestRequest(String uri, YmsHttpMethod method) throws  Exception{
        CtmJSONObject responseJson = new CtmJSONObject();
        log.error("【" + uri + "】" + "请求uri", uri);
        try {
            YmsHttpRequest request = new YmsHttpRequestBuilder().url(uri)
                    .method(YmsHttpMethod.GET)
                    .build();
            CtmYmsHttpResponse response = CtmHttpClientTool.execute(request);
            int status = response.getYmsHttpResponse().getStatusCode();
            if (status != 200) {
                throw new CtmException(String.format("doCommonRestRequest net error, uri: %s, status: %s, statusText: %s", uri, status, response.getYmsHttpResponse().getStatusText()));
            }
            String responseData = response.getYmsHttpResponse().getBodyString(Charset.forName("UTF-8"));
            log.error("【" + responseData + "】" + "：{}", responseData);
            responseJson = CtmJSONObject.parseObject(responseData);
        } catch (Exception e) {
            log.error("doCommonRestRequest error", e.getMessage());
            throw new CtmException(String.format("doCommonRestRequest error, uri: %s, error: %s", uri, e.getMessage()));
        }
        return responseJson;
    }

    private static String getYQLTestData(String transCode, String data) {
        //如果/data目录下有testYQL.txt文件，则读取该测试文件内容替代data。测试使用，正常环境没有该文件。
        try {
            String filePath = "/data/" + transCode + "/testYQL.txt"; // 文件路径
            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                try (Scanner scanner = new Scanner(file)) {
                    data = scanner.useDelimiter("\\A").next(); // 读取整个文件内容
                } catch (FileNotFoundException e) {
                    log.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("读取测试文件失败:" + e.getMessage(), e);
        }
        return data;
    }

    public static void saveYQLBusinessLog(String transCode, String uri, CtmJSONObject reqMap, CtmJSONObject result, CtmJSONObject params, String account) {
        try {
            if (!saveYQLBusinessLog) {
                return;
            }
            BusinessObject businessObject = new BusinessObject();
            businessObject.setOperationDate(new Date());
            businessObject.setOperCodeTypes(OperCodeTypes.query);
            businessObject.setServiceCode(Objects.toString(params.get("serviceCode"), null));
            businessObject.setOperatorName(AppContext.getCurrentUser().getName());
            //类型
            businessObject.setBusiObjTypeName(transCode);

            CtmJSONObject logData = new CtmJSONObject();

            //编码
            businessObject.setBusiObjCodeFieldName("code");
            logData.put("code", account);
            //名称
            businessObject.setBusiObjNameFieldName("name");
            logData.put("name", YQLUtils.TRANS_CODE_COMMENTS.get(transCode));
            //logData.put(IMsgConstant.BILL_DATA, params);
            logData.put(IMsgConstant.YQL_REQUEST, reqMap);
            logData.put(IMsgConstant.YQL_RESPONSE, result);
            logData.put(IMsgConstant.YQL_URL, uri);
            //日志内容
            businessObject.setNewObject(JSON.parseObject(logData.toString()));
            IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
            businessLogService.saveBusinessLog(businessObject);
        } catch (Exception e) {
            log.error("记录银企联日志失败：" + e.getMessage(), e);
        }
    }

    /**
     * 用于专属化环境 回单文件上传
     * @param fileUrl
     * @return
     */
    public static InputStream queryInputStreamByFileUrl(String fileUrl) {
        InputStream inputStream = null; // 定义返回文件流
        if (StringUtils.isNotBlank(fileUrl)) {
            try {
                // 发送httpget请求获取文件流
                CtmYmsHttpResponse response = CtmHttpClientTool.get(fileUrl);
                if (response.getYmsHttpResponse().getStatusCode() == 200) {
                    inputStream = response.getYmsHttpResponse().getBody();
                }
            } catch (Exception e) {
                log.error("通过文件id获取文件流失败"+e);
            }
        }
        return inputStream;
    }
    /*
     *@Author tongyd
     *@Description 构建银企联通用响应报文
     *@Date 2019/6/13 10:28
     *@Param [serverStatus, respCode, respDesc, extendData]
     *@Return java.lang.String
     **/
    public static String buildCommonResponseMsg(String serverStatus, String respCode, String respDesc, String extendData) throws Exception {
        CtmJSONObject responseHead = new CtmJSONObject();
        responseHead.put("service_recv_time", DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        responseHead.put("service_status", serverStatus);
        responseHead.put("service_resp_code", respCode);
        responseHead.put("service_resp_desc", respDesc);
        CtmJSONObject responseBody = new CtmJSONObject();
        responseBody.put("extend_data", extendData);
        CtmJSONObject data = new CtmJSONObject();
        data.put("response_head", responseHead);
        data.put("response_body", responseBody);
        BankConnectionAdapterContext bankConnectionAdapterContext = AppContext.getBean(BankConnectionAdapterContext.class);
        String signature = bankConnectionAdapterContext.chanPaySignMessage(CtmJSONObject.toJSONString(data));
        StringBuilder responseData = new StringBuilder();
        responseData.append("data=");
        responseData.append(data.toString());
        responseData.append("&");
        responseData.append("signature=");
        responseData.append(signature);
        return Base64.encode(responseData.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断给定的url是否符合白名单 用于checkmarx校验
     * 废弃方法，调用点都注释了
     * @param url
     * @return
     * @deprecated
     */
    @Deprecated
    public static Boolean isWhiteUrl(String url) {
        for (String whiteUrl : WHITEURL_LIST) {
            if (whiteUrl.equals(url)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String,String> analysisRequestData(List<BasicNameValuePair> requestData){
        String  reqData =  "";
        String  reqSignData =  "";
        for(BasicNameValuePair vo : requestData){
            if("reqData".equals(vo.getName())){
                reqData =  vo.getValue();
            }else if("reqSignData".equals(vo.getName())){
                reqSignData =  vo.getValue();
            }
        }
        Map<String,String>  result = new HashMap<>();
        result.put("reqData",reqData);
        result.put("reqSignData",reqSignData);
        return result;
    }
}
