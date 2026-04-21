package com.yonyoucloud.fi.cmp.util;

import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;

/**
 * 客户和供应商的结果信息
 * 
 * @author miaowb
 *
 */
public class MerchantResult {

    private int code;
    private String message;
    private CaObject caObject;
    private Object data;
    private VendorBankVO vendorBankVO; //客户银行账户信息
    private AgentFinancialDTO agentFinancialDTO; //供应商银行账户信息
    private String orgId; //客户供应商分配组织id
    private String vendorName; //供应商名称

    public MerchantResult() {

    }

    public MerchantResult(int code, String message, CaObject caObject, Object data) {
        this.code = code;
        this.setMessage(message);
        this.setCaObject(caObject);
        this.setData(data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CaObject getCaObject() {
        return caObject;
    }

    public void setCaObject(CaObject caObject) {
        this.caObject = caObject;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public VendorBankVO getVendorBankVO() {
        return vendorBankVO;
    }

    public void setVendorBankVO(VendorBankVO vendorBankVO) {
        this.vendorBankVO = vendorBankVO;
    }

    public AgentFinancialDTO getAgentFinancialDTO() {
        return agentFinancialDTO;
    }

    public void setAgentFinancialDTO(AgentFinancialDTO agentFinancialDTO) {
        this.agentFinancialDTO = agentFinancialDTO;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String toString() {
        return "MerchantResult [code=" + code + ", message=" + message + ", caObject=" + caObject + ", data=" + data
                + "]";
    }
   
}
