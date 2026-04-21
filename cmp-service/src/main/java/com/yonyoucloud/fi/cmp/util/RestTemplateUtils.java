package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.yms.http.YmsHttpHeader;
import com.yonyou.iuap.yms.http.YmsHttpMethod;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.httpclient.CtmHttpClientTool;
import com.yonyou.yonbip.ctm.util.httpclient.response.CtmYmsHttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * rest请求工具类
 *
 * @author rtlgq
 */
@Slf4j
public class RestTemplateUtils {

    /**
     * POST请求
     *
     * @param url
     * @param json
     * @return
     */
    public static CtmJSONObject doPostByJSON(String url, CtmJSONObject json) {
        try {
//            RestTemplate rest = new RestTemplate();
//            rest.setRequestFactory(new OkHttp3ClientHttpRequestFactory());
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//            HttpEntity<CtmJSONObject> request = new HttpEntity<CtmJSONObject>(json, headers);
//
//            ResponseEntity<CtmJSONObject> responseEntity = rest.postForEntity(url, request, CtmJSONObject.class);
//
//            CtmJSONObject jsonObject = responseEntity.getBody();


            YmsHttpHeader ymsHttpHeader = new YmsHttpHeader();
            ymsHttpHeader.add("Content-Type","application/json;charset=UTF-8");

            CtmYmsHttpResponse ctmYmsHttpResponse = CtmHttpClientTool.execute(url, YmsHttpMethod.POST,CtmJSONObject.toJSONString(json),ymsHttpHeader);
            CtmJSONObject jsonObject = ctmYmsHttpResponse.getYmsHttpResponse().getBody(CtmJSONObject.class);

            return jsonObject;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102200"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0013D", "调度任务中心连接异常") /* "调度任务中心连接异常" */);
        }
    }
    /**
     * POST请求
     *
     * @param url
     * @param jsonArray
     * @return
     */
    public static CtmJSONObject doPostByJSONArray(String url, CtmJSONArray jsonArray) {
        try {
//            RestTemplate rest = new RestTemplate();
//            rest.setRequestFactory(new OkHttp3ClientHttpRequestFactory());
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//            HttpEntity<CtmJSONArray> request = new HttpEntity<CtmJSONArray>(jsonArray, headers);
//            ResponseEntity<CtmJSONObject> responseEntity = rest.postForEntity(url, request, CtmJSONObject.class);
//            CtmJSONObject jsonObject = responseEntity.getBody();


            YmsHttpHeader ymsHttpHeader = new YmsHttpHeader();
            ymsHttpHeader.add("Content-Type","application/json;charset=UTF-8");
            CtmYmsHttpResponse ctmYmsHttpResponse = CtmHttpClientTool.execute(url, YmsHttpMethod.POST,CtmJSONObject.toJSONString(jsonArray),ymsHttpHeader);
            CtmJSONObject jsonObject = ctmYmsHttpResponse.getYmsHttpResponse().getBody(CtmJSONObject.class);

            return jsonObject;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102200"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0013D", "调度任务中心连接异常") /* "调度任务中心连接异常" */);
        }
    }

}
