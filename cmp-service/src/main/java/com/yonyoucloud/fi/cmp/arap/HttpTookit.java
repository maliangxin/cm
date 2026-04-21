package com.yonyoucloud.fi.cmp.arap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.imeta.biz.base.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class HttpTookit {
    private static final Logger logger = LoggerFactory.getLogger(HttpTookit.class);
    private static final RequestConfig config = RequestConfig.custom().setConnectTimeout(360000).setSocketTimeout(360000).build();
    private static final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    private static CloseableHttpClient httpClient;
    public static final String CHARSET = "UTF-8";

    public HttpTookit() {
    }

    public static void initHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(config);

        try {
            ServiceLoader<HttpRequestInterceptor> requestInterceptors = ServiceLoader.load(HttpRequestInterceptor.class);
            requestInterceptors.forEach((x) -> {
                httpClientBuilder.addInterceptorFirst(x);
            });
            ServiceLoader<HttpResponseInterceptor> responseInterceptors = ServiceLoader.load(HttpResponseInterceptor.class);
            responseInterceptors.forEach((x) -> {
                httpClientBuilder.addInterceptorFirst(x);
            });
        } catch (Throwable var3) {
            Throwable var9 = var3;
            logger.error(var9.getMessage(), var9);
        }

        httpClient = httpClientBuilder.build();
    }

    public static boolean initParms() {
        return true;
    }

    public static String doGet(String url, Map<String, String> params) {
        return doGet(url, params, (Map)null, "UTF-8");
    }

    public static String doPost(String url, Map<String, String> params) {
        return doPost(url, params, (Map)null, "UTF-8");
    }

    public static String doGet(String url, Map<String, String> params, Map<String, String> headers) {
        return doGet(url, params, headers, "UTF-8");
    }

    public static String doPost(String url, Map<String, String> params, Map<String, String> headers) {
        return doPost(url, params, headers, "UTF-8");
    }

    public static String doPostWithJson(String url, String json, Map<String, String> headers) {
        return doPostWithJson(url, json, headers, "UTF-8");
    }

    public static String doPostWithJson_FailRetry(String url, String json, Map<String, String> headers, int retry) {
        String rst = null;
        Exception anyException = null;
        int i = 0;

        do {
            try {
                anyException = null;
                rst = doPostWithJson(url, json, headers, "UTF-8");
            } catch (Exception var8) {
                Exception exception = var8;
                anyException = exception;
            }
        } while(i++ < retry && anyException != null);

        if (anyException != null) {
            logger.error("POST请求异常：", anyException);
            throw new BizException("510", anyException.getMessage(), anyException);
        } else {
            return rst;
        }
    }

    public static String doGet(String url, Map<String, String> params, Map<String, String> headers, String charset) {
        HttpGet httpGet = null;
        HttpEntity entity = null;
        CloseableHttpResponse response = null;

        String value;
        try {
            if (params != null && !params.isEmpty()) {
                List<BasicNameValuePair> pairs = new ArrayList(params.size());
                Iterator var8 = params.entrySet().iterator();

                while(var8.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var8.next();
                    value = (String)entry.getValue();
                    if (value != null) {
                        pairs.add(new BasicNameValuePair((String)entry.getKey(), value));
                    }
                }

                url = url + "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, charset));
            }

            httpGet = new HttpGet(url);
            httpGet.addHeader("Cookie", "test=visible");
            if (headers != null && !headers.isEmpty()) {
                Iterator var22 = headers.entrySet().iterator();

                while(var22.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var22.next();
                    httpGet.addHeader((String)entry.getKey(), (String)entry.getValue());
                }
            }

            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(360000).setConnectTimeout(360000).build();
            httpGet.setConfig(requestConfig);
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 306) {
                httpGet.abort();
                throw new BizException("511", "HttpClient,error status code :" + statusCode);
            }

            entity = response.getEntity();
            String result = null;
            if (entity != null) {
                result = EntityUtils.toString(entity, "utf-8");
            }

            EntityUtils.consume(entity);
            response.close();
            httpGet.releaseConnection();
            value = result;
        } catch (Exception var19) {
            Exception e = var19;
            throw new BizException("510", e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(entity);
                if (response != null) {
                    response.close();
                }

                if (httpGet != null) {
                    httpGet.releaseConnection();
                }
            } catch (IOException var18) {
            }

        }

        return value;
    }

    public static String doPost(String url, Map<String, String> params, Map<String, String> headers, String charset) {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = null;
        CloseableHttpResponse response = null;

        String var11;
        try {
            List<BasicNameValuePair> pairs = null;
            String result;
            if (params != null && !params.isEmpty()) {
                pairs = new ArrayList(params.size());
                Map.Entry<String, String> entry = null;
                Iterator<Map.Entry<String, String>> localIterator = params.entrySet().iterator();

                while(localIterator.hasNext()) {
                    entry = (Map.Entry)localIterator.next();
                    result = (String)entry.getValue();
                    if (result != null) {
                        pairs.add(new BasicNameValuePair((String)entry.getKey(), result));
                    }
                }
            }

            if (pairs != null && pairs.size() > 0) {
                httpPost.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
            }

            if (headers != null && !headers.isEmpty()) {
                Iterator var23 = headers.entrySet().iterator();

                while(var23.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var23.next();
                    httpPost.addHeader((String)entry.getKey(), (String)entry.getValue());
                }
            }

            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 306) {
                httpPost.abort();
                throw new BizException("511", "HttpClient,error status code :" + statusCode);
            }

            entity = response.getEntity();
            result = null;
            if (entity != null) {
                result = EntityUtils.toString(entity, "utf-8");
            }

            var11 = result;
        } catch (Exception var20) {
            Exception e = var20;
            throw new BizException("510", e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(entity);
                if (response != null) {
                    response.close();
                }
            } catch (IOException var19) {
            }

            httpPost.releaseConnection();
        }

        return var11;
    }

    public static String doPostWithJson(String url, String json, Map<String, String> headers, String charset) {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = null;
        CloseableHttpResponse response = null;

        String var11;
        try {
            httpPost.addHeader("Cookie", "test=visible");
            if (headers != null && !headers.isEmpty()) {
                Iterator var22 = headers.entrySet().iterator();

                while(var22.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var22.next();
                    httpPost.addHeader((String)entry.getKey(), (String)entry.getValue());
                }
            }

            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
            httpPost.setConfig(requestConfig);
            StringEntity s = new StringEntity(json, "UTF-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            httpPost.setEntity(s);
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 306) {
                httpPost.abort();
                throw new BizException("511", "HttpClient,error status code :" + statusCode);
            }

            entity = response.getEntity();
            String result = null;
            if (entity != null) {
                result = EntityUtils.toString(entity, "utf-8");
            }

            var11 = result;
        } catch (Exception var20) {
            Exception e = var20;
            logger.error("POST请求异常：", e);
            throw new BizException("510", e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(entity);
                if (response != null) {
                    response.close();
                }
            } catch (IOException var19) {
            }

            httpPost.releaseConnection();
        }

        return var11;
    }

    static {
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(100);
        initHttpClient();
    }
}
