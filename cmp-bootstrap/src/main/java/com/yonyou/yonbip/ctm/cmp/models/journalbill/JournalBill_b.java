package com.yonyou.yonbip.ctm.cmp.models.journalbill;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 日记账录入子表实体
 *
 * @author u
 * @version 1.0
 */
public class JournalBill_b extends BizObject implements IBackWrite, ITenant, IYTenant {

    private static final long serialVersionUID = 1L;

    /**
     *  实体全称
     */
	public static final String ENTITY_NAME = "cmp.journalbill.JournalBill_b";

	/**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public String getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(String mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
    public String getBankaccount() {
        return get("bankaccount");
    }

    /**
     * 设置企业银行账户
     *
     * @param bankaccount 企业银行账户.ID
     */
    public void setBankaccount(String bankaccount) {
        set("bankaccount", bankaccount);
    }

    /**
     * 获取收款现金账户
     *
     * @return 收款现金账户.ID
     */
    public String getCashaccount() {
        return get("cashaccount");
    }

    /**
     * 设置收款现金账户
     *
     * @param cashaccount 收款现金账户.ID
     */
    public void setCashaccount(String cashaccount) {
        set("cashaccount", cashaccount);
    }

    /**
     * 获取币种id
     *
     * @return 币种id.ID
     */
    public String getCurrency() {
        return get("currency");
    }

    /**
     * 设置币种id
     *
     * @param currency 币种id.ID
     */
    public void setCurrency(String currency) {
        set("currency", currency);
    }

    /**
     * 获取汇率类型
     *
     * @return 汇率类型.ID
     */
    public String getExchangeRateType() {
        return get("exchangeRateType");
    }

    /**
     * 设置汇率类型
     *
     * @param exchangeRateType 汇率类型.ID
     */
    public void setExchangeRateType(String exchangeRateType) {
        set("exchangeRateType", exchangeRateType);
    }

    /**
     * 获取业务日期
     */
    public java.util.Date getBusinessDate() {
        return get("businessDate");
    }

    /**
     * 获取业务日期
     * @param businessDate
     */
    public void setBusinessDate(java.util.Date businessDate) {
        set("businessDate", businessDate);
    }

    /**
     * 获取登账日期
     */
    public java.util.Date getDzdate() {
        return get("dzdate");
    }

    /**
     * 获取登账日期
     * @param dzdate
     */
    public void setDzdate(java.util.Date dzdate) {
        set("dzdate", dzdate);
    }

    /**
     * 获取登账时间
     */
    public java.util.Date getDztime() {
        return get("dztime");
    }

    /**
     * 获取登账时间
     * @param dztime
     */
    public void setDztime(java.util.Date dztime) {
        set("dztime", dztime);
    }

    /**
     * 获取摘要
     * @return
     */
    public String getDescription() {
        return get("description");
    }

    /**
     * 设置摘要
     * @param description
     */
    public void setDescription(String description) {
        set("description", description);
    }
    
    /**
     * 获取汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getExchangeRateOps() {
        return getShort("exchangeRateOps");
    }

    /**
     * 设置汇率折算方式
     *
     * @param exchangeRateOps 汇率折算方式
     */
    public void setExchangeRateOps(Short exchangeRateOps) {
        set("exchangeRateOps", exchangeRateOps);
    }

    /**
     * 获取汇率
     *
     * @return 汇率
     */
    public java.math.BigDecimal getExchangeRate() {
        return get("exchangeRate");
    }

    /**
     * 设置汇率
     *
     * @param exchangeRate 汇率
     */
    public void setExchangeRate(java.math.BigDecimal exchangeRate) {
        set("exchangeRate", exchangeRate);
    }

    /**
     * 获取收付类型
     *
     * @return 收付类型
     */
    public Short getPaymenttype() {
        return getShort("paymenttype");
    }

    /**
     * 设置收付类型
     *
     * @param paymenttype 收付类型
     */
    public void setPaymenttype(Short paymenttype) {
        set("paymenttype", paymenttype);
    }


    /**
     * 获取借方原币金额
     *
     * @return 借方原币金额
     */
    public java.math.BigDecimal getDebitoriSum() {
        return getBigDecimal("debitoriSum");
    }

    /**
     * 设置借方原币金额
     *
     * @param debitoriSum 借方原币金额
     */
    public void setDebitoriSum(java.math.BigDecimal debitoriSum) {
        set("debitoriSum", debitoriSum);
    }

    /**
     * 获取借方本币金额
     *
     * @return 借方本币金额
     */
    public java.math.BigDecimal getDebitnatSum() {
        return getBigDecimal("debitnatSum");
    }

    /**
     * 设置借方本币金额
     *
     * @param debitnatSum 借方本币金额
     */
    public void setDebitnatSum(java.math.BigDecimal debitnatSum) {
        set("debitnatSum", debitnatSum);
    }

    /**
     * 获取贷方原币金额
     *
     * @return 贷方原币金额
     */
    public java.math.BigDecimal getCreditoriSum() {
        return getBigDecimal("creditoriSum");
    }

    /**
     * 设置贷方原币金额
     *
     * @param creditoriSum 贷方原币金额
     */
    public void setCreditoriSum(java.math.BigDecimal creditoriSum) {
        set("creditoriSum", creditoriSum);
    }

    /**
     * 获取贷方本币金额
     *
     * @return 贷方本币金额
     */
    public java.math.BigDecimal getCreditnatSum() {
        return getBigDecimal("creditnatSum");
    }

    /**
     * 设置贷方本币金额
     *
     * @param creditnatSum 贷方本币金额
     */
    public void setCreditnatSum(java.math.BigDecimal creditnatSum) {
        set("creditnatSum", creditnatSum);
    }

    /**
     * 获取结算方式
     *
     * @return 结算方式.ID
     */
    public Long getSettlemode() {
        return get("settlemode");
    }

    /**
     * 设置结算方式
     *
     * @param settlemode 结算方式.ID
     */
    public void setSettlemode(Long settlemode) {
        set("settlemode", settlemode);
    }

    /**
     * 获取对方类型
     *
     * @return 对方类型
     */
    public Short getOppositetype() {
        return getShort("oppositetype");
    }

    /**
     * 设置对方类型
     *
     * @param oppositetype 对方类型
     */
    public void setOppositetype(Short oppositetype) {
        set("oppositetype", oppositetype);
    }

    /**
     * 获取对方名称
     *
     * @return 对方名称
     */
    public String getOppositename() {
        return get("oppositename");
    }

    /**
     * 设置对方名称
     *
     * @param oppositename 对方名称
     */
    public void setOppositename(String oppositename) {
        set("oppositename", oppositename);
    }

    /**
     * 获取对方编码
     *
     * @return 对方编码
     */
    public String getOppositecode() {
        return get("oppositecode");
    }

    /**
     * 设置对方编码
     *
     * @param oppositecode 对方编码
     */
    public void setOppositecode(String oppositecode) {
        set("oppositecode", oppositecode);
    }

    /**
     * 获取对方ID
     *
     * @return 对方ID
     */
    public String getOppositeid() {
        return get("oppositeid");
    }

    /**
     * 设置对方ID
     *
     * @param oppositeid 对方ID
     */
    public void setOppositeid(String oppositeid) {
        set("oppositeid", oppositeid);
    }

    /**
     * 获取对方账户名称
     *
     * @return 对方账户名称
     */
    public String getOppositeaccountname() {
        return get("oppositeaccountname");
    }

    /**
     * 设置对方账户名称
     *
     * @param oppositeaccountname 对方账户名称
     */
    public void setOppositeaccountname(String oppositeaccountname) {
        set("oppositeaccountname", oppositeaccountname);
    }

    /**
     * 获取对方账户id
     *
     * @return 对方账户id
     */
    public String getOppositeaccountid() {
        return get("oppositeaccountid");
    }

    /**
     * 设置对方账户id
     *
     * @param oppositeaccountid 对方账户id
     */
    public void setOppositeaccountid(String oppositeaccountid) {
        set("oppositeaccountid", oppositeaccountid);
    }

    /**
     * 获取对方银行账号
     *
     * @return 对方银行账号
     */
    public String getOppositebankaccountno() {
        return get("oppositebankaccountno");
    }

    /**
     * 设置对方银行账号
     *
     * @param oppositebankaccountno 对方银行账号
     */
    public void setOppositebankaccountno(String oppositebankaccountno) {
        set("oppositebankaccountno", oppositebankaccountno);
    }

    /**
     * 获取票据日期
     * @return
     */
    public java.util.Date getNotedate() {
        return get("notedate");
    }

    /**
     * 设置票据日期
     * @param notedate
     */
    public void  setNotedate(java.util.Date notedate) {
        set("notedate", notedate);
    }

    /**
     * 获取票据类型
     *
     * @return 票据类型.ID
     */
    public String getNotetype() {
        return get("notetype");
    }

    /**
     * 设置票据类型
     *
     * @param notetype 票据类型.ID
     */
    public void setNotetype(String notetype) {
        set("notetype", notetype);
    }

    /**
     * 获取票据号
     *
     * @return 票据号
     */
    public String getNoteno() {
        return get("noteno");
    }

    /**
     * 设置票据号
     *
     * @param noteno 票据号
     */
    public void setNoteno(String noteno) {
        set("noteno", noteno);
    }

    /**
     * 获取项目ID
     * @return
     */
    public String getProjectId() {
        return get("projectId");
    }

    /**
     * 设置项目ID
     * @param projectId
     */
    public void setProjectId(String projectId) {
        set("projectId", projectId);
    }

    /**
     * 获取费用项目
     *
     * @return 费用项目.ID
     */
    public String getCostproject() {
        return get("costproject");
    }

    /**
     * 设置费用项目
     *
     * @param costproject 费用项目.ID
     */
    public void setCostproject(String costproject) {
        set("costproject", costproject);
    }

    /**
     * 获取财资统一对账码
     *
     * @return 财资统一对账码
     */
    public String getUnireconciliationcode() {
        return get("unireconciliationcode");
    }

    /**
     * 设置财资统一对账码
     *
     * @param unireconciliationcode 财资统一对账码
     */
    public void setUnireconciliationcode(String unireconciliationcode) {
        set("unireconciliationcode", unireconciliationcode);
    }

    /**
     * 获取事项分录ID|凭证ID
     *
     * @return 事项分录ID|凭证ID
     */
    public String getVoucherId() {
        return get("voucherId");
    }

    /**
     * 设置事项分录ID|凭证ID
     *
     * @param voucherId 事项分录ID|凭证ID
     */
    public void setVoucherId(String voucherId) {
        set("voucherId", voucherId);
    }

    /**
     * 获取凭证号
     *
     * @return 凭证号
     */
    public String getVoucherNo() {
        return get("voucherNo");
    }

    /**
     * 设置凭证号
     *
     * @param voucherNo 凭证号
     */
    public void setVoucherNo(String voucherNo) {
        set("voucherNo", voucherNo);
    }

    /**
     * 获取凭证期间
     *
     * @return 凭证期间
     */
    public String getVoucherPeriod() {
        return get("voucherPeriod");
    }

    /**
     * 设置凭证期间
     *
     * @param voucherPeriod 凭证期间
     */
    public void setVoucherPeriod(String voucherPeriod) {
        set("voucherPeriod", voucherPeriod);
    }

    /**
     * 获取凭证状态
     *
     * @return 凭证状态
     */
    public Short getVoucherstatus() {
        return getShort("voucherstatus");
    }

    /**
     * 设置凭证状态
     *
     * @param voucherstatus 凭证状态
     */
    public void setVoucherstatus(Short voucherstatus) {
        set("voucherstatus", voucherstatus);
    }

    /**
     * 获取上游单据类型
     *
     * @return 上游单据类型
     */
    public String getSource() {
        return get("source");
    }

    /**
     * 设置上游单据类型
     *
     * @param source 上游单据类型
     */
    public void setSource(String source) {
        set("source", source);
    }

    /**
     * 获取上游单据号
     *
     * @return 上游单据号
     */
    public String getUpcode() {
        return get("upcode");
    }

    /**
     * 设置上游单据号
     *
     * @param upcode 上游单据号
     */
    public void setUpcode(String upcode) {
        set("upcode", upcode);
    }

    /**
     * 获取生单规则编号
     *
     * @return 生单规则编号
     */
    public String getMakeRuleCode() {
        return get("makeRuleCode");
    }

    /**
     * 设置生单规则编号
     *
     * @param makeRuleCode 生单规则编号
     */
    public void setMakeRuleCode(String makeRuleCode) {
        set("makeRuleCode", makeRuleCode);
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    public java.util.Date getSourceMainPubts() {
        return get("sourceMainPubts");
    }

    /**
     * 设置时间戳
     *
     * @param sourceMainPubts 时间戳
     */
    public void setSourceMainPubts(java.util.Date sourceMainPubts) {
        set("sourceMainPubts", sourceMainPubts);
    }

    /**
     * 获取分组任务KEY
     *
     * @return 分组任务KEY
     */
    public String getGroupTaskKey() {
        return get("groupTaskKey");
    }

    /**
     * 设置分组任务KEY
     *
     * @param groupTaskKey 分组任务KEY
     */
    public void setGroupTaskKey(String groupTaskKey) {
        set("groupTaskKey", groupTaskKey);
    }


    /**
     * 获取Y租户Id
     *
     * @return Y租户Id
     */
    public String getYtenantId() {
        return get("ytenantId");
    }

    /**
     * 设置Y租户Id
     *
     * @param ytenantId Y租户Id
     */
    public void setYtenantId(String ytenantId) {
        set("ytenantId", ytenantId);
    }

    /**
     * 获取租户id
     *
     * @return 租户id
     */
    @Override
    public String getYTenant() {
        return get("ytenant");
    }

    /**
     * 设置租户id
     *
     * @param ytenant 租户id
     */
    @Override
    public void setYTenant(String ytenant) {
        set("ytenant", ytenant);
    }

    /**
     * 获取上游单据主表id
     *
     * @return 上游单据主表id
     */
    public Long getSourceid() {
        return get("sourceid");
    }

    /**
     * 设置上游单据主表id
     *
     * @param sourceid 上游单据主表id
     */
    public void setSourceid(Long sourceid) {
        set("sourceid", sourceid);
    }

    /**
     * 获取上游单据子表id
     *
     * @return 上游单据子表id
     */
    public Long getSourceautoid() {
        return get("sourceautoid");
    }

    /**
     * 设置上游单据子表id
     *
     * @param sourceautoid 上游单据子表id
     */
    public void setSourceautoid(Long sourceautoid) {
        set("sourceautoid", sourceautoid);
    }

    /**
     * 获取序号
     *
     * @return 序号
     */
    public Integer getRowno() {
        return get("rowno");
    }

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
    public void setRowno(Integer rowno) {
        set("rowno", rowno);
    }

    /**
     * 获取行号
     *
     * @return 行号
     */
    public java.math.BigDecimal getLineno() {
        return get("lineno");
    }

    /**
     * 设置行号
     *
     * @param lineno 行号
     */
    public void setLineno(java.math.BigDecimal lineno) {
        set("lineno", lineno);
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    public java.util.Date getPubts() {
        return get("pubts");
    }

    /**
     * 设置时间戳
     *
     * @param pubts 时间戳
     */
    public void setPubts(java.util.Date pubts) {
        set("pubts", pubts);
    }
    /**
     * 获取租户
     *
     * @return 租户.ID
     */
    public Long getTenant() {
        return get("tenant");
    }

    /**
     * 设置租户
     *
     * @param tenant 租户.ID
     */
    public void setTenant(Long tenant) {
        set("tenant", tenant);
    }

}
