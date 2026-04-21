package com.yonyoucloud.fi.cmp.util;

import com.yonyou.cloud.auth.sdk.client.AuthSDKClient;
import com.yonyou.cloud.auth.sdk.client.utils.http.HttpResult;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class CTMAuthHttpClientUtils {

    private static final String AUTH_ACCESS = "access.key";
    private static final String AUTH_ACCESS_VALUE = "access.secret";

    public static String getAccKey() {
        return  AppContext.getEnvConfig(AUTH_ACCESS);
    }

    public static String getAccSecret() {
        return  AppContext.getEnvConfig(AUTH_ACCESS_VALUE);
    }

    public static String execPost(String url, Map<String, String> queryParam, Map<String, String> header, String jsonString) {
        HttpPost post = new HttpPost();
        if (MapUtils.isNotEmpty(header)) {
            Iterator var5 = header.entrySet().iterator();

            while(var5.hasNext()) {
                Entry<String, String> headerMap = (Entry)var5.next();
                post.addHeader((String)headerMap.getKey(), (String)headerMap.getValue());
            }
        }

        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        url = buildQueryParam(url, queryParam);
        post.setURI(URI.create(url));
        HttpEntity entity = new StringEntity(jsonString, "UTF-8");
        post.setEntity(entity);
        AuthSDKClient client = new AuthSDKClient(getAccKey(), getAccSecret());
        HttpResult result = client.execute(post);
        return result.getResponseString();
    }

    public static String buildQueryParam(String url, Map<String, String> queryParam) {
        List<NameValuePair> nameValuePairs = new ArrayList();
        if (MapUtils.isNotEmpty(queryParam)) {
            String key;
            String value;
            for(Iterator var3 = queryParam.entrySet().iterator(); var3.hasNext(); nameValuePairs.add(new BasicNameValuePair(key, value))) {
                Entry<String, String> queryParamMap = (Entry)var3.next();
                key = (String)queryParamMap.getKey();
                value = (String)queryParamMap.getValue();
//                if (isContainChinese(value)) {
//                    try {
//                        value = URLEncoder.encode(value, "UTF-8");
//                    } catch (UnsupportedEncodingException var9) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102642"),"charset not supported!", var9);
//                    }
//                }
            }

            if (!url.contains("?")) {
                url = url + "?";
            } else {
                url = url + "&";
            }

            try {
                HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
                url = url + EntityUtils.toString(entity);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102643"),"errors occured while building HttpEntity!", e);
            }
        }

        return url;
    }
}
