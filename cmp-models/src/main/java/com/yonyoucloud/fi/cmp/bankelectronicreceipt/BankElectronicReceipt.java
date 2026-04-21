package com.yonyoucloud.fi.cmp.bankelectronicreceipt;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.SignStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

/**
 * 银行电子回单
 *
 * @author u
 * @version 1.0
 */
public class BankElectronicReceipt extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankelectronicreceipt.BankElectronicReceipt";

	//字段名称
	public static final String DETAIL_RECEIPT_RELATION_CODE = "detailReceiptRelationCode";
	public static final String ACCENTITY = "accentity";
	public static final String ENTERPRISE_BANK_ACCOUNT = "enterpriseBankAccount";
	public static final String TRAN_DATE = "tranDate";
	public static final String TRAN_TIME = "tranTime";
	public static final String DC_FLAG = "dc_flag";
	public static final String BANKSEQNO = "bankseqno";
	public static final String RECEIPTNO = "receiptno";
	public static final String TO_ACCT_NO = "to_acct_no";
	public static final String TO_ACCT_NAME = "to_acct_name";
	public static final String TO_ACCT_BANK = "to_acct_bank";
	public static final String TO_ACCT_BANK_NAME = "to_acct_bank_name";
	public static final String CURRENCY = "currency";
	public static final String TRAN_AMT = "tran_amt";
	public static final String EXTENDSS = "extendss";
	public static final String USE_NAME = "use_name";
	public static final String CUSTNO = "custno";
	public static final String REMARK = "remark";
	public static final String CREATE_TIME = "createTime";
	public static final String CREATE_DATE = "createDate";
	public static final String MODIFY_TIME = "modifyTime";
	public static final String MODIFY_DATE = "modifyDate";
	public static final String CREATOR = "creator";
	public static final String MODIFIER = "modifier";
	public static final String CREATOR_ID = "creatorId";
	public static final String MODIFIER_ID = "modifierId";
	public static final String TENANT = "tenant";
	public static final String CHECKMATCH = "checkmatch";
	public static final String RELEVANCEID = "relevanceid";
	public static final String BILL_EXTEND = "bill_extend";
	public static final String REMARK01 = "remark01";
	public static final String ISDOWN = "isdown";
	public static final String OTHER_CHECKNO = "other_checkno";
	public static final String REMOTEPATH = "remotepath";
	public static final String FILENAME = "filename";
	public static final String FILESIZE = "filesize";
	public static final String DATA_ORIGIN = "dataOrigin";
	public static final String ASSOCIATIONSTATUS = "associationstatus";
	public static final String BANKRECONCILIATIONID = "bankreconciliationid";
	public static final String SIGN_STATUS = "signStatus";
	public static final String BANKTYPE = "banktype";
	public static final String UNIQUE_CODE = "uniqueCode";
	public static final String BANKCHECKCODE = "bankcheckcode";
	public static final String INTEREST = "interest";
	public static final String CHARACTER_DEF = "characterDef";
	public static final String YQL_UNIQUE_NO = "yqlUniqueNo";
	/**
	 * 获取流水回单关联码
	 *
	 * @return 流水回单关联码
	 */
	public String getDetailReceiptRelationCode() {
		return get("detailReceiptRelationCode");
	}

	/**
	 * 设置流水回单关联码
	 *
	 * @param detailReceiptRelationCode 流水回单关联码
	 */
	public void setDetailReceiptRelationCode(String detailReceiptRelationCode) {
		set("detailReceiptRelationCode", detailReceiptRelationCode);
	}

    /**
     * 获取资金组织
     *
     * @return 资金组织.ID
     */
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置资金组织
     *
     * @param accentity 资金组织.ID
     */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getEnterpriseBankAccount() {
		return get("enterpriseBankAccount");
	}

    /**
     * 设置企业银行账户
     *
     * @param enterpriseBankAccount 企业银行账户.ID
     */
	public void setEnterpriseBankAccount(String enterpriseBankAccount) {
		set("enterpriseBankAccount", enterpriseBankAccount);
	}

    /**
     * 获取交易日期
     *
     * @return 交易日期
     */
	public java.util.Date getTranDate() {
		return get("tranDate");
	}

    /**
     * 设置交易日期
     *
     * @param tranDate 交易日期
     */
	public void setTranDate(java.util.Date tranDate) {
		set("tranDate", tranDate);
	}

    /**
     * 获取交易时间
     *
     * @return 交易时间
     */
	public java.util.Date getTranTime() {
		return get("tranTime");
	}

    /**
     * 设置交易时间
     *
     * @param tranTime 交易时间
     */
	public void setTranTime(java.util.Date tranTime) {
		set("tranTime", tranTime);
	}

    /**
     * 获取借/贷
     *
     * @return 借/贷
     */
	public Direction getDc_flag() {
		Number v = get("dc_flag");
		return Direction.find(v);
	}

    /**
     * 设置借/贷
     *
     * @param dc_flag 借/贷
     */
	public void setDc_flag(Direction dc_flag) {
		if (dc_flag != null) {
			set("dc_flag", dc_flag.getValue());
		} else {
			set("dc_flag", null);
		}
	}

    /**
     * 获取银行交易流水号
     *
     * @return 银行交易流水号
     */
	public String getBankseqno() {
		return get("bankseqno");
	}

    /**
     * 设置银行交易流水号
     *
     * @param bankseqno 银行交易流水号
     */
	public void setBankseqno(String bankseqno) {
		set("bankseqno", bankseqno);
	}

    /**
     * 获取回单编号
     *
     * @return 回单编号
     */
	public String getReceiptno() {
		return get("receiptno");
	}

    /**
     * 设置回单编号
     *
     * @param receiptno 回单编号
     */
	public void setReceiptno(String receiptno) {
		set("receiptno", receiptno);
	}

    /**
     * 获取对方账号
     *
     * @return 对方账号
     */
	public String getTo_acct_no() {
		return get("to_acct_no");
	}

    /**
     * 设置对方账号
     *
     * @param to_acct_no 对方账号
     */
	public void setTo_acct_no(String to_acct_no) {
		set("to_acct_no", to_acct_no);
	}

    /**
     * 获取对方户名
     *
     * @return 对方户名
     */
	public String getTo_acct_name() {
		return get("to_acct_name");
	}

    /**
     * 设置对方户名
     *
     * @param to_acct_name 对方户名
     */
	public void setTo_acct_name(String to_acct_name) {
		set("to_acct_name", to_acct_name);
	}

    /**
     * 获取对方开户行
     *
     * @return 对方开户行
     */
	public String getTo_acct_bank() {
		return get("to_acct_bank");
	}

    /**
     * 设置对方开户行
     *
     * @param to_acct_bank 对方开户行
     */
	public void setTo_acct_bank(String to_acct_bank) {
		set("to_acct_bank", to_acct_bank);
	}

    /**
     * 获取对方开户行名
     *
     * @return 对方开户行名
     */
	public String getTo_acct_bank_name() {
		return get("to_acct_bank_name");
	}

    /**
     * 设置对方开户行名
     *
     * @param to_acct_bank_name 对方开户行名
     */
	public void setTo_acct_bank_name(String to_acct_bank_name) {
		set("to_acct_bank_name", to_acct_bank_name);
	}

    /**
     * 获取币种
     *
     * @return 币种.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置币种
     *
     * @param currency 币种.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取交易金额
     *
     * @return 交易金额
     */
	public java.math.BigDecimal getTran_amt() {
		return get("tran_amt");
	}

    /**
     * 设置交易金额
     *
     * @param tran_amt 交易金额
     */
	public void setTran_amt(java.math.BigDecimal tran_amt) {
		set("tran_amt", tran_amt);
	}

    /**
     * 获取扩展信息
     *
     * @return 扩展信息
     */
	public String getExtendss() {
		return get("extendss");
	}

    /**
     * 设置扩展信息
     *
     * @param extendss 扩展信息
     */
	public void setExtendss(String extendss) {
		set("extendss", extendss);
	}

    /**
     * 获取用途
     *
     * @return 用途
     */
	public String getUse_name() {
		return get("use_name");
	}

    /**
     * 设置用途
     *
     * @param use_name 用途
     */
	public void setUse_name(String use_name) {
		set("use_name", use_name);
	}

    /**
     * 获取客户号
     *
     * @return 客户号
     */
	public String getCustno() {
		return get("custno");
	}

    /**
     * 设置客户号
     *
     * @param custno 客户号
     */
	public void setCustno(String custno) {
		set("custno", custno);
	}

    /**
     * 获取摘要
     *
     * @return 摘要
     */
	public String getRemark() {
		return get("remark");
	}

    /**
     * 设置摘要
     *
     * @param remark 摘要
     */
	public void setRemark(String remark) {
		set("remark", remark);
	}

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
	public java.util.Date getCreateTime() {
		return get("createTime");
	}

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
	public void setCreateTime(java.util.Date createTime) {
		set("createTime", createTime);
	}

    /**
     * 获取创建日期
     *
     * @return 创建日期
     */
	public java.util.Date getCreateDate() {
		return get("createDate");
	}

    /**
     * 设置创建日期
     *
     * @param createDate 创建日期
     */
	public void setCreateDate(java.util.Date createDate) {
		set("createDate", createDate);
	}

    /**
     * 获取修改时间
     *
     * @return 修改时间
     */
	public java.util.Date getModifyTime() {
		return get("modifyTime");
	}

    /**
     * 设置修改时间
     *
     * @param modifyTime 修改时间
     */
	public void setModifyTime(java.util.Date modifyTime) {
		set("modifyTime", modifyTime);
	}

    /**
     * 获取修改日期
     *
     * @return 修改日期
     */
	public java.util.Date getModifyDate() {
		return get("modifyDate");
	}

    /**
     * 设置修改日期
     *
     * @param modifyDate 修改日期
     */
	public void setModifyDate(java.util.Date modifyDate) {
		set("modifyDate", modifyDate);
	}

    /**
     * 获取创建人名称
     *
     * @return 创建人名称
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人名称
     *
     * @param creator 创建人名称
     */
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取修改人名称
     *
     * @return 修改人名称
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人名称
     *
     * @param modifier 修改人名称
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
	}

    /**
     * 获取创建人
     *
     * @return 创建人.ID
     */
	public Long getCreatorId() {
		return get("creatorId");
	}

    /**
     * 设置创建人
     *
     * @param creatorId 创建人.ID
     */
	public void setCreatorId(Long creatorId) {
		set("creatorId", creatorId);
	}

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
	public Long getModifierId() {
		return get("modifierId");
	}

    /**
     * 设置修改人
     *
     * @param modifierId 修改人.ID
     */
	public void setModifierId(Long modifierId) {
		set("modifierId", modifierId);
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
	/**
	 * 获取是否匹配
	 *
	 * @return 是否匹配
	 */
	public Boolean getCheckmatch() {
		return getBoolean("checkmatch");
	}

	/**
	 * 设置是否匹配
	 *
	 * @param checkmatch 是否匹配
	 */
	public void setCheckmatch(Boolean checkmatch) {
		set("checkmatch", checkmatch);
	}

	/**
	 * 获取回单匹配关联id
	 *
	 * @return 回单匹配关联id
	 */
	public String getRelevanceid() {
		return get("relevanceid");
	}

	/**
	 * 设置回单匹配关联id
	 *
	 * @param relevanceid 回单匹配关联id
	 */
	public void setRelevanceid(String relevanceid) {
		set("relevanceid", relevanceid);
	}

	/**
	 * 获取回单下载信息
	 *
	 * @return 回单匹配关联id
	 */
	public String getBill_extend() {
		return get("bill_extend");
	}

	/**
	 * 设置回单下载信息
	 *
	 * @param bill_extend 回单匹配关联id
	 */
	public void setBill_extend(String bill_extend) {
		set("bill_extend", bill_extend);
	}

	/**
	 * 获取新增附言
	 *
	 * @return 新增附言
	 */
	public String getRemark01() {
		return get("remark01");
	}

	/**
	 * 设置新增附言
	 *
	 * @param remark01 新增附言
	 */
	public void setRemark01(String remark01) {
		set("remark01", remark01);
	}

	/**
	 * 获取是否已下载回单
	 *
	 * @return 是否已下载回单
	 */
	public Boolean getIsdown() {
		return getBoolean("isdown");
	}

	/**
	 * 设置是否已下载回单
	 *
	 * @param isdown 是否已下载回单
	 */
	public void setIsdown(Boolean isdown) {
		set("isdown", isdown);
	}

	/**
	 * 获取总账勾对号
	 *
	 * @return 总账勾对号
	 */
	public String getOther_checkno() {
		return get("other_checkno");
	}

	/**
	 * 设置总账勾对号
	 *
	 * @param other_checkno 总账勾对号
	 */
	public void setOther_checkno(String other_checkno) {
		set("other_checkno", other_checkno);
	}

	/**
	 * 获取电子归档服务器地址
	 *
	 * @return 电子归档服务器地址
	 */
	public String getRemotepath() {
		return get("remotepath");
	}

	/**
	 * 设置电子归档服务器地址
	 *
	 * @param remotepath 电子归档服务器地址
	 */
	public void setRemotepath(String remotepath) {
		set("remotepath", remotepath);
	}

	/**
	 * 获取电子回单文件名称
	 *
	 * @return 电子回单文件名称
	 */
	public String getFilename() {
		return get("filename");
	}

	/**
	 * 设置电子回单文件名称
	 *
	 * @param filename 电子回单文件名称
	 */
	public void setFilename(String filename) {
		set("filename", filename);
	}

	/**
	 * 获取电子回单文件大小
	 *
	 * @return 电子回单文件大小
	 */
	public Integer getFilesize() {
		return get("filesize");
	}

	/**
	 * 设置电子回单文件大小
	 *
	 * @param filesize 电子回单文件大小
	 */
	public void setFilesize(Integer filesize) {
		set("filesize", filesize);
	}
	
    /**
     * 获取数据来源
     *
     * @return 数据来源
     */
    public DateOrigin getDataOrigin() {
        Number v = get("dataOrigin");
        return DateOrigin.find(v);
    }

    /**
     * 设置数据来源
     *
     * @param dataOrigin 数据来源
     */
    public void setDataOrigin(DateOrigin dataOrigin) {
        if (dataOrigin != null) {
            set("dataOrigin", dataOrigin.getValue());
        } else {
            set("dataOrigin", null);
        }
    }

    /**
     * 获取对账单关联状态
     *
     * @return 对账单关联状态
     */
    public Short getAssociationstatus() {
        return getShort("associationstatus");
    }

    /**
     * 对账单关联状态
     *
     * @param associationstatus 对账单关联状态
     */
    public void setAssociationstatus(Short associationstatus) {
        set("associationstatus", associationstatus);
    }


	/**
	 * 获取银行对账单主键
	 *
	 * @return 银行对账单主键
	 */
	public String getBankreconciliationid() {
		return get("bankreconciliationid");
	}

	/**
	 * 银行对账单主键
	 *
	 * @param bankreconciliationid 银行对账单主键
	 */
	public void setBankreconciliationid(String bankreconciliationid) {
		set("bankreconciliationid", bankreconciliationid);
	}

	/**
	 * 验签状态
	 *
	 * @return 验签状态
	 */
	public SignStatus getSignStatus() {
		Number v = get("signStatus");
		return SignStatus.find(v);
	}

	/**
	 * 验签状态
	 *
	 * @param signStatus 验签状态
	 */
	public void setSignStatus(SignStatus signStatus) {
		if (signStatus != null) {
			set("signStatus", signStatus.getValue());
		} else {
			set("signStatus", null);
		}
	}


	/**
	 * 获取银行账户银行类别
	 *
	 * @return 银行账户银行类别.ID
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行账户银行类别
	 *
	 * @param banktype 银行账户银行类别.ID
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
	}
	/**
	 * 电子回单唯一标识码
	 *
	 * @param uniqueCode 电子回单唯一标识码
	 */
	public void setUniqueCode(String uniqueCode) {
		set("uniqueCode", uniqueCode);
	}

	/**
	 * 电子回单唯一标识码
	 *
	 * @return 电子回单唯一标识码
	 */
	public String getUniqueCode() {
		return get("uniqueCode");
	}


	/**
	 * 获取银行对账码
	 *
	 * @return 银行对账码
	 */
	public String getBankcheckcode() {
		return get("bankcheckcode");
	}

	/**
	 * 设置银行对账码
	 *
	 * @param bankcheckcode 银行对账码
	 */
	public void setBankcheckcode(String bankcheckcode) {
		set("bankcheckcode", bankcheckcode);
	}

	/**
	 * 利息
	 *
	 * @return 利息
	 */
	public java.math.BigDecimal getInterest() {
		return get("interest");
	}

	/**
	 * 利息
	 *
	 * @param interest 利息
	 */
	public void setInterest(java.math.BigDecimal interest) {
		set("interest", interest);
	}



	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}

	/**
	 * 获取全局唯一标识
	 *
	 * @return 全局唯一标识
	 */
	public String  getYqlUniqueNo() {
		return get("yqlUniqueNo");
	}

	/**
	 * 设置全局唯一标识
	 *
	 * @param yqlUniqueNo 全局唯一标识
	 */
	public void setYqlUniqueNo(String yqlUniqueNo) {
		set("yqlUniqueNo", yqlUniqueNo);
	}

}
