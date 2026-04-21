package com.yonyoucloud.fi.cmp.arap;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

public class HttpLogEntity implements Serializable {
    private Long id;
    private String direction;
    private String response;
    private String request;
    private String spareRequest;
    private Long tenantId;
    private String tenant;
    private String ytenantId;
    private String billId;
    private String billNo;
    private Date createTime;
    private Date requestTime;
    private Date responseTime;
    private static final String defaultValue = "";
    public static final int MAX_SQL_TEXT_LEN = 60000;

    public String getBillNo() {
        return StringUtils.left(this.billNo, 50);
    }

    public void initRequest(String request) {
        int len = StringUtils.length(request);
        this.request = StringUtils.left(request, 60000);
        if (len > 60000) {
            request = request.substring(60000);
            this.spareRequest = StringUtils.left(request, 60000);
        }

    }

    public HttpLogEntity(String billNo, String billId, String request, String direction, String gtxId) {
        Date now = new Date();
        this.setRequestTime(now).setResponseTime(now).setDirection(direction).setBillNo(billNo).setBillId(billId);
        String req = (String) Optional.ofNullable(request).orElse("");
        if (StringUtils.isNotBlank(gtxId)) {
            req = gtxId + "-" + req;
        }

        this.initRequest(req);
        if (AppContext.getCurrentUser() != null) {
            this.setTenant(AppContext.getCurrentUser().getYhtUserId());
            this.setTenantId((Long)AppContext.getCurrentUser().getTenant());
        }

    }

    public HttpLogEntity buildEntity(String billNo, String billId, String request, String direction) {
        Date now = new Date();
        HttpLogEntity entity = new HttpLogEntity();
        entity.setRequestTime(now).setResponseTime(now).setDirection(direction).setBillNo(billNo).setBillId(billId);
        entity.initRequest(request);
        if (AppContext.getCurrentUser() != null) {
            entity.setTenant(AppContext.getCurrentUser().getYhtUserId());
            entity.setTenantId((Long)AppContext.getCurrentUser().getTenant());
        }

        return entity;
    }

    public void dealYTenantId() {
        if (StringUtils.isBlank(this.getTenant()) || "0".equals(this.getTenant())) {
            this.setTenant((String)AppContext.getYhtTenantId());
        }

        if (AppContext.getCurrentUser() != null) {
            this.setTenant(AppContext.getCurrentUser().getYhtUserId());
        }

        if (null == this.getTenantId()) {
            this.setTenantId((Long)AppContext.getTenantId());
        }

        String yTenantId = (String)AppContext.getYhtTenantId();
        if (StringUtils.isNotBlank(yTenantId)) {
            this.setYtenantId(yTenantId);
        } else {
            this.setYtenantId(StringUtils.isNotEmpty(this.getTenant()) ? this.getTenant() : this.getTenantId() + "");
        }

    }

    public static HttpLogEntity build(String billNo, String billId, String tenant, String request, String direction) {
        Date now = new Date();
        HttpLogEntity entity = new HttpLogEntity();
        entity.setRequestTime(now).setResponseTime(now).setDirection(direction).setTenant(tenant).setBillNo(billNo).setBillId(billId);
        entity.initRequest(request);
        return entity;
    }

    public Long getId() {
        return this.id;
    }

    public String getDirection() {
        return this.direction;
    }

    public String getResponse() {
        return this.response;
    }

    public String getRequest() {
        return this.request;
    }

    public String getSpareRequest() {
        return this.spareRequest;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public String getTenant() {
        return this.tenant;
    }

    public String getYtenantId() {
        return this.ytenantId;
    }

    public String getBillId() {
        return this.billId;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public Date getRequestTime() {
        return this.requestTime;
    }

    public Date getResponseTime() {
        return this.responseTime;
    }

    public HttpLogEntity setId(final Long id) {
        this.id = id;
        return this;
    }

    public HttpLogEntity setDirection(final String direction) {
        this.direction = direction;
        return this;
    }

    public HttpLogEntity setResponse(final String response) {
        this.response = response;
        return this;
    }

    public HttpLogEntity setRequest(final String request) {
        this.request = request;
        return this;
    }

    public HttpLogEntity setSpareRequest(final String spareRequest) {
        this.spareRequest = spareRequest;
        return this;
    }

    public HttpLogEntity setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public HttpLogEntity setTenant(final String tenant) {
        this.tenant = tenant;
        return this;
    }

    public HttpLogEntity setYtenantId(final String ytenantId) {
        this.ytenantId = ytenantId;
        return this;
    }

    public HttpLogEntity setBillId(final String billId) {
        this.billId = billId;
        return this;
    }

    public HttpLogEntity setBillNo(final String billNo) {
        this.billNo = billNo;
        return this;
    }

    public HttpLogEntity setCreateTime(final Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public HttpLogEntity setRequestTime(final Date requestTime) {
        this.requestTime = requestTime;
        return this;
    }

    public HttpLogEntity setResponseTime(final Date responseTime) {
        this.responseTime = responseTime;
        return this;
    }

    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof HttpLogEntity)) {
            return false;
        } else {
            HttpLogEntity other = (HttpLogEntity)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                label167: {
                    Object this$id = this.getId();
                    Object other$id = other.getId();
                    if (this$id == null) {
                        if (other$id == null) {
                            break label167;
                        }
                    } else if (this$id.equals(other$id)) {
                        break label167;
                    }

                    return false;
                }

                Object this$tenantId = this.getTenantId();
                Object other$tenantId = other.getTenantId();
                if (this$tenantId == null) {
                    if (other$tenantId != null) {
                        return false;
                    }
                } else if (!this$tenantId.equals(other$tenantId)) {
                    return false;
                }

                label153: {
                    Object this$direction = this.getDirection();
                    Object other$direction = other.getDirection();
                    if (this$direction == null) {
                        if (other$direction == null) {
                            break label153;
                        }
                    } else if (this$direction.equals(other$direction)) {
                        break label153;
                    }

                    return false;
                }

                Object this$response = this.getResponse();
                Object other$response = other.getResponse();
                if (this$response == null) {
                    if (other$response != null) {
                        return false;
                    }
                } else if (!this$response.equals(other$response)) {
                    return false;
                }

                label139: {
                    Object this$request = this.getRequest();
                    Object other$request = other.getRequest();
                    if (this$request == null) {
                        if (other$request == null) {
                            break label139;
                        }
                    } else if (this$request.equals(other$request)) {
                        break label139;
                    }

                    return false;
                }

                Object this$spareRequest = this.getSpareRequest();
                Object other$spareRequest = other.getSpareRequest();
                if (this$spareRequest == null) {
                    if (other$spareRequest != null) {
                        return false;
                    }
                } else if (!this$spareRequest.equals(other$spareRequest)) {
                    return false;
                }

                label125: {
                    Object this$tenant = this.getTenant();
                    Object other$tenant = other.getTenant();
                    if (this$tenant == null) {
                        if (other$tenant == null) {
                            break label125;
                        }
                    } else if (this$tenant.equals(other$tenant)) {
                        break label125;
                    }

                    return false;
                }

                label118: {
                    Object this$ytenantId = this.getYtenantId();
                    Object other$ytenantId = other.getYtenantId();
                    if (this$ytenantId == null) {
                        if (other$ytenantId == null) {
                            break label118;
                        }
                    } else if (this$ytenantId.equals(other$ytenantId)) {
                        break label118;
                    }

                    return false;
                }

                Object this$billId = this.getBillId();
                Object other$billId = other.getBillId();
                if (this$billId == null) {
                    if (other$billId != null) {
                        return false;
                    }
                } else if (!this$billId.equals(other$billId)) {
                    return false;
                }

                label104: {
                    Object this$billNo = this.getBillNo();
                    Object other$billNo = other.getBillNo();
                    if (this$billNo == null) {
                        if (other$billNo == null) {
                            break label104;
                        }
                    } else if (this$billNo.equals(other$billNo)) {
                        break label104;
                    }

                    return false;
                }

                label97: {
                    Object this$createTime = this.getCreateTime();
                    Object other$createTime = other.getCreateTime();
                    if (this$createTime == null) {
                        if (other$createTime == null) {
                            break label97;
                        }
                    } else if (this$createTime.equals(other$createTime)) {
                        break label97;
                    }

                    return false;
                }

                Object this$requestTime = this.getRequestTime();
                Object other$requestTime = other.getRequestTime();
                if (this$requestTime == null) {
                    if (other$requestTime != null) {
                        return false;
                    }
                } else if (!this$requestTime.equals(other$requestTime)) {
                    return false;
                }

                Object this$responseTime = this.getResponseTime();
                Object other$responseTime = other.getResponseTime();
                if (this$responseTime == null) {
                    if (other$responseTime != null) {
                        return false;
                    }
                } else if (!this$responseTime.equals(other$responseTime)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HttpLogEntity;
    }

    public int hashCode() {
        boolean PRIME = true;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        Object $direction = this.getDirection();
        result = result * 59 + ($direction == null ? 43 : $direction.hashCode());
        Object $response = this.getResponse();
        result = result * 59 + ($response == null ? 43 : $response.hashCode());
        Object $request = this.getRequest();
        result = result * 59 + ($request == null ? 43 : $request.hashCode());
        Object $spareRequest = this.getSpareRequest();
        result = result * 59 + ($spareRequest == null ? 43 : $spareRequest.hashCode());
        Object $tenant = this.getTenant();
        result = result * 59 + ($tenant == null ? 43 : $tenant.hashCode());
        Object $ytenantId = this.getYtenantId();
        result = result * 59 + ($ytenantId == null ? 43 : $ytenantId.hashCode());
        Object $billId = this.getBillId();
        result = result * 59 + ($billId == null ? 43 : $billId.hashCode());
        Object $billNo = this.getBillNo();
        result = result * 59 + ($billNo == null ? 43 : $billNo.hashCode());
        Object $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
        Object $requestTime = this.getRequestTime();
        result = result * 59 + ($requestTime == null ? 43 : $requestTime.hashCode());
        Object $responseTime = this.getResponseTime();
        result = result * 59 + ($responseTime == null ? 43 : $responseTime.hashCode());
        return result;
    }

    public String toString() {
        return "HttpLogEntity(id=" + this.getId() + ", direction=" + this.getDirection() + ", response=" + this.getResponse() + ", request=" + this.getRequest() + ", spareRequest=" + this.getSpareRequest() + ", tenantId=" + this.getTenantId() + ", tenant=" + this.getTenant() + ", ytenantId=" + this.getYtenantId() + ", billId=" + this.getBillId() + ", billNo=" + this.getBillNo() + ", createTime=" + this.getCreateTime() + ", requestTime=" + this.getRequestTime() + ", responseTime=" + this.getResponseTime() + ")";
    }

    public HttpLogEntity() {
    }
}