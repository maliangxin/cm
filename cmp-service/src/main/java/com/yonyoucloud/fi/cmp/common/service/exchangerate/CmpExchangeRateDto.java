package com.yonyoucloud.fi.cmp.common.service.exchangerate;

import com.yonyou.ucf.basedoc.model.ExchangeRateMode;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.BaseConfigEnum;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 自动化参数实体
 *
 * @author u
 * @version 1.0
 */
public class CmpExchangeRateDto {
    private static final long serialVersionUID = 1L;
    //源币种元数据name
    private String currencyKey;
    //目标币种元数据name
    private String natCurrencyKey;
    //汇率类型元数据name
    private String exchangeRateTypeKey;
    //报价日期
    private Date quotationDate;
    //源币种金额字段
    private String currencyAmountKey;
    //目标金额字段
    private String targetCurrencyKey;
    //业务主实体
    private BizObject bizObject;
    //业务子实体
    private List<BizObject> subBizObjectList;
    //资金组织key
    private String accentityKey;
    //会计主体key
    private String orgKey;
    //是否核算汇率类型
    private boolean accountingExchangeRateType;

    public void setAccountingExchangeRateType(boolean accountingExchangeRateType) {
        accountingExchangeRateType = accountingExchangeRateType;
    }

    public String getCurrencyKey() {
        return currencyKey;
    }

    public String getNatCurrencyKey() {
        return natCurrencyKey;
    }

    public String getExchangeRateTypeKey() {
        return exchangeRateTypeKey;
    }

    public Date getQuotationDate() {
        return quotationDate;
    }

    public BizObject getBizObject() {
        return bizObject;
    }

    public List<BizObject> getSubBizObjectList() {
        return subBizObjectList;
    }

    public String getAccentityKey() {
        return accentityKey;
    }

    public String getOrgKey() {
        return orgKey;
    }

    public boolean getAccountingExchangeRateType() {
        return accountingExchangeRateType;
    }


    public void setCurrencyKey(String currencyKey) {
        this.currencyKey = currencyKey;
    }

    public void setNatCurrencyKey(String natCurrencyKey) {
        this.natCurrencyKey = natCurrencyKey;
    }

    public void setExchangeRateTypeKey(String exchangeRateTypeKey) {
        this.exchangeRateTypeKey = exchangeRateTypeKey;
    }

    public void setQuotationDate(Date quotationDate) {
        this.quotationDate = quotationDate;
    }

    public void setBizObject(BizObject bizObject) {
        this.bizObject = bizObject;
    }

    public void setSubBizObjectList(List<BizObject> subBizObjectList) {
        this.subBizObjectList = subBizObjectList;
    }

    public void setAccentityKey(String accentityKey) {
        this.accentityKey = accentityKey;
    }

    public void setOrgKey(String orgKey) {
        this.orgKey = orgKey;
    }

    public void setTargetCurrencyKey(String targetCurrencyKey) {
        this.targetCurrencyKey = targetCurrencyKey;
    }

    public String getTargetCurrencyKey() {
        return targetCurrencyKey;
    }

    public String getCurrencyAmountKey() {
        return currencyAmountKey;
    }

    public void setCurrencyAmountKey(String currencyAmountKey) {
        this.currencyAmountKey = currencyAmountKey;
    }


    public CmpExchangeRateDto() {
    }

    public CmpExchangeRateDto(String currencyKey, String natCurrencyKey, String exchangeRateTypeKey, Date quotationDate, String currencyAmountKey, String targetCurrencyKey, BizObject bizObject, List<BizObject> subBizObjectList, String accentityKey, String orgKey, boolean accountingExchangeRateType) {
        this.currencyKey = currencyKey;
        this.natCurrencyKey = natCurrencyKey;
        this.exchangeRateTypeKey = exchangeRateTypeKey;
        this.quotationDate = quotationDate;
        this.currencyAmountKey = currencyAmountKey;
        this.targetCurrencyKey = targetCurrencyKey;
        this.bizObject = bizObject;
        this.subBizObjectList = subBizObjectList;
        this.accentityKey = accentityKey;
        this.orgKey = orgKey;
        this.accountingExchangeRateType = accountingExchangeRateType;
    }
}
