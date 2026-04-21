package com.yonyoucloud.fi.cmp.event.entity;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.cloud.middleware.AppRuntimeEnvironment;
import com.yonyou.ucf.mdd.ext.core.AppContext;

/**
 * 事件中心精简消息
 * <p>
 * 文档地址：https://uap-wiki.yyrd.com/pages/viewpage.action?pageId=160334733
 * <p>
 * 产生精简消息体，BusinessEvent.setUserObject(精简消息体)，精简消息体的格式(json)：
 * <p>
 * simple_msg 必填，设置为true。标识为精简消息体
 * billId 单据主键，必填
 * bill_time 单据更新时间，必填 (yyyy-MM-dd HH:mm:ss的字符串)
 * billCode	单据code，
 * fullname	单据元数据uri，
 * tenantId	租户id，必填
 * service_url 必填，调用生产者查询单据的rpc地址。
 * 格式为 rpc://${spring.application.name}@AppRuntimeEnvironment.getProviderId()+"/${实现了IEventFetchService的spring bean名称}"
 * billClue	单据查询条件
 * billVersion 单据版本号,必填，数值类型或者日期类型(yyyy-MM-dd HH:mm:ss)
 * oss_url oss地址
 */
public class SimpleUserObject extends CtmJSONObject {

    private static final long serialVersionUID = -2939566077261967589L;

    private SimpleUserObject() {
    }

    public static final class Builder {
        boolean simpleMsg;
        String billId;
        String billTime;
        String billCode;
        String fullName;
        String tenantId;
        String rpcServiceUrl;
        String billClue;
        long billVersion;
        String ossUrl;
        static final String RPC_SERVICE_URL_FORMAT = "rpc://yonbip-fi-ctmcmp@%s/%s";

        private Builder(String billId, String billTime, long billVersion, String eventFetchServiceBeanName) {
            this.simpleMsg = Boolean.TRUE;
            this.billId = billId;
            this.billTime = billTime;
            this.billVersion = billVersion;
            this.rpcServiceUrl = String.format(RPC_SERVICE_URL_FORMAT, AppRuntimeEnvironment.getProviderId(), eventFetchServiceBeanName);
            this.tenantId = InvocationInfoProxy.getTenantid();
        }

        /**
         * 必填字段
         *
         * @param billId                    单据主键，必填
         * @param billTime                  单据更新时间，必填
         * @param eventFetchServiceBeanName 实现了IEventFetchService的spring bean名称，必填
         * @return the builder
         */
        public static Builder builder(String billId, String billTime, long billVersion, String eventFetchServiceBeanName) {
            return new Builder(billId, billTime, billVersion, eventFetchServiceBeanName);
        }

        /**
         * 单据code
         *
         * @param billCode the bill code
         * @return the builder
         */
        public Builder billCode(String billCode) {
            this.billCode = billCode;
            return this;
        }

        /**
         * 单据元数据uri
         *
         * @param fullName the full name
         * @return the builder
         */
        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        /**
         * 单据查询条件
         *
         * @param billClue the bill clue
         * @return the builder
         */
        public Builder billClue(String billClue) {
            this.billClue = billClue;
            return this;
        }

        /**
         * Oss url.
         *
         * @param ossUrl the oss url
         * @return the builder
         */
        public Builder ossUrl(String ossUrl) {
            this.ossUrl = ossUrl;
            return this;
        }

        public SimpleUserObject build() {
            SimpleUserObject simpleUserObject = new SimpleUserObject();
            simpleUserObject.put("simple_msg", simpleMsg);
            simpleUserObject.put("billId", billId);
            simpleUserObject.put("bill_time", billTime);
            simpleUserObject.put("billCode", billCode);
            simpleUserObject.put("fullname", fullName);
            simpleUserObject.put("tenantId", tenantId);
            simpleUserObject.put("service_url", rpcServiceUrl);
            simpleUserObject.put("billClue", billClue);
            simpleUserObject.put("billVersion", billVersion);
            simpleUserObject.put("oss_url", ossUrl);
            return simpleUserObject;
        }
    }
}