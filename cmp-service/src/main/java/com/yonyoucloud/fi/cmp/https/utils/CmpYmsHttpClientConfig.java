package com.yonyoucloud.fi.cmp.https.utils;

import com.yonyou.iuap.yms.http.YmsHttpClient;
import com.yonyou.iuap.yms.http.YmsHttpConfig;
import com.yonyoucloud.fi.cmp.common.CtmException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 单例 YmsHttpClient 配置类
 *
 * @author zhuguangqian
 * @since 2024/10/28
 */
@Slf4j
public class CmpYmsHttpClientConfig {

    private static volatile YmsHttpClient instance;

    public static YmsHttpClient ymsHttpsClient() {
        if (null == instance) {
            synchronized (CmpYmsHttpClientConfig.class) {
                if (null == instance) {
                    //通过config配置自定义参数
                    SslContext sslContext;
                    try {
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
                        sslContext = SslContextBuilder.forClient().trustManager(tm).build();
                    } catch (SSLException e) {
                        log.error("SSLException:{}",e.getMessage(), e);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102410"),e.getMessage());
                    }
                    YmsHttpConfig config = new YmsHttpConfig();
                    config.setReadTimeout(120000);
                    config.setConnectTimeout(120000);
                    config.setSslContext(sslContext);
                    instance = new YmsHttpClient(config);
                }
            }
        }
        return instance;
    }
}
