package com.yonyoucloud.fi.cmp.https.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @ClassName HttpsService
 * @Desc Https服务
 * @Author tongyd
 * @Date 2019/10/22
 * @Version 1.0
 */
public interface HttpsService {

    /**
     * 发送https的post请求 修改相关参数为JsonNode
     * @param transCode
     * @param postMsg
     * @param url
     * @return
     * @throws Exception
     */
     CtmJSONObject doHttpsPost(String transCode, CtmJSONObject postMsg, String url, Boolean isNewLogic) throws Exception;

    /**
     * 发送https的post请求 通用型
     * @param transCode
     * @param postMsg
     * @param url
     * @return
     * @throws Exception
     */
    CtmJSONObject doHttpsPostForXinLianXin(String transCode, CtmJSONObject postMsg, String url) throws Exception;


    /**
     * 银企联发送https请求 带重试机制
     * @param transCode
     * @param requestData
     * @param uri
     * @return
     * @throws Exception
     */
//    CtmJSONObject doHttpsPostRetry(String transCode, List<BasicNameValuePair> requestData, String uri) throws Exception;
}
