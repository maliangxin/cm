package com.yonyoucloud.fi.cmp.https.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.iuap.yms.http.*;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.CmpYmsHttpClientConfig;
import com.yonyoucloud.fi.cmp.https.utils.TransErrorInfo;
import com.yonyoucloud.fi.cmp.util.JSONBuilderUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;


/**
 * @ClassName HttpsServiceImpl
 * @Desc https服务实现
 * @Author yangjn
 * @Date 2023/10/22
 * @Version 1.0
 */
@Service
@Slf4j
public class HttpsServiceImpl implements HttpsService {

    @Resource
    private YmsHttpClient client;
    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    @Override
    public CtmJSONObject doHttpsPost(String transCode, CtmJSONObject postMsg, String url, Boolean isNewLogic) throws Exception {
        if (isNewLogic == null) {
            isNewLogic = false;
        }

        YmsHttpClient client = null;

        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(CtmJSONObject.toJSONString(postMsg));
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", CtmJSONObject.toJSONString(postMsg)));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        log.error("doHttpsPost推送报文:"+requestData);
        CtmJSONObject result = new CtmJSONObject();

        YmsHttpMultipartEntity entity = YmsHttpMultipartEntityBuilder.create()
                .addStringBody("reqData", CtmJSONObject.toJSONString(postMsg))
                .addStringBody("reqSignData", signMsg)
                .setContentType(YmsHttpContentType.APPLICATION_JSON)
                .build();

        YmsHttpRequest request = new YmsHttpRequestBuilder().url(url)
                .method(YmsHttpMethod.POST)
                .multipartEntity(entity)
                .build();

        String responseData = "";
        YmsHttpResponse response = null;
        try {
            request.getHeader().delete("Content-Type");
            request.setHeader("Content-Type","multipart/form-data");
            if (isNewLogic) {
                response = CmpYmsHttpClientConfig.ymsHttpsClient().execute(request);
            } else {
                X509TrustManager tm = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                };
                SslContext sslContext = SslContextBuilder.forClient().trustManager(tm).build();
                YmsHttpConfig config = new YmsHttpConfig();
                config.setSslContext(sslContext);
                config.setReadTimeout(120000);
                config.setConnectTimeout(120000);
                client = new YmsHttpClient(config);
                response = client.execute(request);
            }
            int status = response.getStatusCode();
            if (status != 200) {
                result.put("code", TransErrorInfo.ERROR_STATUS_ERROR.getErrorCode());
                result.put("message", TransErrorInfo.ERROR_STATUS_ERROR.getErrorMessage()+ status + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00107", "】") /* "】" */ + response.getStatusText());
                return result;
            }
            responseData = response.getBodyString(Charset.forName("UTF-8"));
            log.error("doHttpsPost反馈报文:" + responseData);
            response.close();
        } catch (Exception e) {
            log.error("银企联云交互异常,请求报文={},错误信息={}",  CtmJSONObject.toJSONString(postMsg),e);
            result.put("code",TransErrorInfo.ERROR_NET_ERROR.getErrorCode());
            result.put("message", TransErrorInfo.ERROR_NET_ERROR.getErrorMessage());
            return result;
        }finally {
            if (!isNewLogic) {
                if(response != null){
                    response.close();
                }
                if(client != null){
                    client.close();
                }
            }
        }
        if (responseData!=null&&responseData.contains("error-code")) {
            CtmJSONObject error = CtmJSONObject.parseObject(responseData);
            result.put("code", error.get("error-code").toString());
            result.put("message", error.get("error-desc").toString());
            return result;
        }
        if (requestData!=null){
            if(responseData.contains("&signature=")){
                String[] dataAndSign = responseData.split("&signature=");
                if (dataAndSign != null && dataAndSign.length == 2) {
                    String data = dataAndSign[0].substring(dataAndSign[0].indexOf("data=") + 5);
                    String signature = dataAndSign[1];
                    if (!bankConnectionAdapterContext.chanPayVerifySignature(data, signature)) {
                        result.put("code", TransErrorInfo.ERROR_SIGN_ERROR.getErrorCode());
                        result.put("message", TransErrorInfo.ERROR_SIGN_ERROR.getErrorMessage());
                    }
                    result.put("data", data);
                } else {
                    result.put("code", TransErrorInfo.ERROR_SIGN_EMPTY.getErrorCode());
                    result.put("message", TransErrorInfo.ERROR_SIGN_EMPTY.getErrorMessage());
                    return result;
                }
            }else{
                result.put("code", TransErrorInfo.ERROR_SIGN_EMPTY.getErrorCode());
                result.put("message", TransErrorInfo.ERROR_SIGN_EMPTY.getErrorMessage());
                return result;
            }
        }
        if(!TransErrorInfo.SUCCESS.getErrorMessage().equals(result.get("code"))){
            result.put("code", TransErrorInfo.SUCCESS.getErrorCode());
            result.put("message", TransErrorInfo.SUCCESS.getErrorMessage());
        }
        return result;
    }


    /**
     * [心连心项目]认领单生成来款记录 调用
     * @param transCode
     * @param postMsg
     * @param url
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject doHttpsPostForXinLianXin(String transCode, CtmJSONObject postMsg, String url) throws Exception {
//        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(postMsg.toString());
//        List<BasicNameValuePair> requestData = new ArrayList<>();
//        requestData.add(new BasicNameValuePair("reqData", CtmJSONObject.toJSONString(postMsg)));
//        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
//        log.error("doHttpsPost推送报文:"+requestData);
        CtmJSONObject result = new CtmJSONObject();
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };
        SslContext sslContext = SslContextBuilder.forClient().trustManager(tm).build();

//        YmsHttpConfig config = new YmsHttpConfig();
//        config.setSslContext(sslContext);
//        config.setReadTimeout(60000);
//        config.setConnectTimeout(60000);
//        client = new YmsHttpClient(config);

        YmsHttpHeader header = new YmsHttpHeader();
        header.add("Content-Type", "application/json");
        header.add("domain-key","marketingbill");
        YmsHttpConfig config = new YmsHttpConfig();
        config.setSslContext(sslContext);
        config.setReadTimeout(60000);
        config.setConnectTimeout(60000);
        client = new YmsHttpClient(config);

        YmsHttpRequest request = new YmsHttpRequestBuilder().url(url)
                .method(YmsHttpMethod.POST)
                .addHeader(header)
//                .addQueryParam("reqData", CtmJSONObject.toJSONString(postMsg))
//                .addQueryParam("reqSignData", signMsg)
//                .addQueryParam("body", CtmJSONObject.toJSONString(postMsg))
                .body(postMsg.toString())
                .build();

        String responseData = "";
        try {
            YmsHttpResponse response = client.execute(request);
            int status = response.getStatusCode();
            if (status != 200) {
                result.put("code", response.getStatusCode());
                result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540056C", "https请求失败！") /* "https请求失败！" */+response);
                return result;
            }
            responseData = response.getBodyString(Charset.forName("UTF-8"));
            log.error("doHttpsPost反馈报文:" + responseData);
            result.put("result",CtmJSONObject.parseObject(responseData));
            response.close();
        } catch (Exception e) {
            log.error("生单异常！", e);
            result.put("code",TransErrorInfo.ERROR_NET_ERROR.getErrorCode());
            result.put("message", TransErrorInfo.ERROR_NET_ERROR.getErrorMessage());
            return result;
        }
        return result;
    }


    private JsonNode buildRequestHeadNew(String transCode, String operator, String customNo, String requestseqno, String signature) {
        JsonNode JsonNode = JSONBuilderUtil.createJson();
        return JsonNode;
//        CMJSONObject requestHead = new CMJSONObject();
//        requestHead.put("version", "1.0.0");
//        requestHead.put("request_seq_no", requestseqno);
//        requestHead.put("cust_no", customNo);
//        requestHead.put("cust_chnl", AppContext.getBean(BankConnectionAdapterContext.class).getChanPayCustomChanel());
//        LocalDateTime dateTime = LocalDateTime.now();
//        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
//        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
//        requestHead.put("oper", operator);
//        requestHead.put("oper_sign", signature);
//        requestHead.put("tran_code", transCode);
//        return requestHead;
    }

    /**
     * 重试次数耗尽 调用
     * @param e
     * @param transCode
     * @param requestData
     * @param uri
     * @return
     */
//    @Recover
//    public CtmJSONObject doHttpsPostRecover(Exception e,String transCode, List<BasicNameValuePair> requestData, String uri){
//        log.error("重试次数耗尽调用doHttpsPostRecover");
//        CtmJSONObject result = new CtmJSONObject();
//        result.put("code", 0); //0为失败，1为成功
//        result.put("message", "银企联云响应异常：502 Gate Badway");
//        CtmJSONObject logJsonObject = new CtmJSONObject();
//        logJsonObject.put("requestData", requestData);
//        String logcode = "YIN_QI_LIAN_TRANS_ERROR";
//        CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
//        ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, logcode, "银企联交易查询", IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.BANKRECONCILIATION, IMsgConstant.BANKRECONCILIATION);
//        return result;
//    }

}
