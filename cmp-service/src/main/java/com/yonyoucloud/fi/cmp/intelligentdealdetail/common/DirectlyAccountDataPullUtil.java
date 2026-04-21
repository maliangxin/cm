package com.yonyoucloud.fi.cmp.intelligentdealdetail.common;

import com.yonyou.iuap.yms.http.*;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.httpclient.CtmHttpClientTool;
import com.yonyou.yonbip.ctm.util.httpclient.response.CtmYmsHttpResponse;
import com.yonyoucloud.fi.cmp.constant.EnvConstant;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DirectlyAccountDataPullUtil {

    public static CtmJSONObject doPullData(String transCode, List<BasicNameValuePair> requestData) {
        Map reqMap = analysisRequestData(requestData);
        log.error("【" + transCode + "】" + "推送报文"+ reqMap.toString());
        CtmJSONObject result = new CtmJSONObject();
        try {
                YmsHttpMultipartEntity entity = YmsHttpMultipartEntityBuilder.create()
                        .addStringBody("reqData", reqMap.get("reqData").toString())
                        .addStringBody("reqSignData", reqMap.get("reqSignData").toString())
                        .setContentType(YmsHttpContentType.APPLICATION_JSON)
                        .build();

                YmsHttpRequest request = new YmsHttpRequestBuilder().url(AppContext.getBean(BankConnectionAdapterContext.class).getChanPayUri())
                        .method(YmsHttpMethod.POST)
                        .multipartEntity(entity)
                        .build();
                request.getHeader().delete("Content-Type");
                request.setHeader("Content-Type","multipart/form-data");

                CtmYmsHttpResponse response = CtmHttpClientTool.execute(request);
                int status = response.getYmsHttpResponse().getStatusCode();
                if (status != 200) {
                    //0为失败，1为成功
                    result.put("code", 0);
                    result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180117","调用畅捷支付响应异常：【") /* "调用畅捷支付响应异常：【" */ + status + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180116","】") /* "】" */ + response.getYmsHttpResponse().getStatusText());
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
                String data = dataAndSign[0].substring(dataAndSign[0].indexOf("data=") + 5);
                if (EnvConstant.YQLTestData) {
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
                result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180119","畅捷支付交易成功") /* "畅捷支付交易成功" */);
                result.put("data", data);
        } catch (Exception e) {
            log.error("畅捷支付连接异常", e.getMessage());
            //0为失败，1为成功
            result.put("code", 0);
            result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418011A","畅捷支付连接异常") /* "畅捷支付连接异常" */);
        }
        return result;
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
