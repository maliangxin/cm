package com.yonyoucloud.fi.cmp.exchangegainloss;

import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;

/**
 * 汇兑损益实体
 *
 * @author u
 * @version 1.0
 */
public class ExchangeGainLoss extends Vouch implements IPrintCount, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.exchangegainloss.ExchangeGainLoss";
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_exchangegainloss";
		/**
	 * 获取核算会计
	 *
	 * @return 核算会计.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计
	 *
	 * @param accentityRaw 核算会计.ID
	 */
	@Override
	public void setAccentityRaw(String accentityRaw) {
		set("accentityRaw", accentityRaw);
	}

	/**
     * 获取资金组织
     *
     * @return 资金组织.ID
     */
	@Override
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置资金组织
     *
     * @param accentity 资金组织.ID
     */
	@Override
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}	/**
	 * 获取余额为零是否为空
	 *
	 * @return 是否包含未记账
	 */
	public Boolean getBalancezero() {
		return getBoolean("balancezero");
	}

	/**
	 * 设置余额为零是否为空
	 *
	 * @param balancezero 是否包含未记账
	 */
	public void setBalancezero(Boolean balancezero) {
		set("balancezero", balancezero);
	}

    /**
     * 获取单据类型
     *
     * @return 单据类型
     */
	public String getBilltype() {
		return get("billtype");
	}

    /**
     * 设置单据类型
     *
     * @param billtype 单据类型
     */
	public void setBilltype(String billtype) {
		set("billtype", billtype);
	}
	/**
	 * 获取交易类型
	 *
	 * @return 交易类型.ID
	 */
	public String getTradetype() {
		return get("tradetype");
	}

	/**
	 * 设置交易类型
	 *
	 * @param tradetype 交易类型.ID
	 */
	public void setTradetype(String tradetype) {
		set("tradetype", tradetype);
	}

    /**
     * 获取汇兑损益
     *
     * @return 汇兑损益
     */
	public String getGainloss() {
		return get("gainloss");
	}

    /**
     * 设置汇兑损益
     *
     * @param gainloss 汇兑损益
     */
	public void setGainloss(String gainloss) {
		set("gainloss", gainloss);
	}

    /**
     * 获取摘要
     *
     * @return 摘要
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置摘要
     *
     * @param description 摘要
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取汇兑损益子表集合
     *
     * @return 汇兑损益子表集合
     */
	public java.util.List<ExchangeGainLoss_b> exchangeGainLoss_b() {
		return getBizObjects("exchangeGainLoss_b", ExchangeGainLoss_b.class);
	}

    /**
     * 设置汇兑损益子表集合
     *
     * @param exchangeGainLoss_b 汇兑损益子表集合
     */
	public void setExchangeGainLoss_b(java.util.List<ExchangeGainLoss_b> exchangeGainLoss_b) {
		setBizObjects("exchangeGainLoss_b", exchangeGainLoss_b);
	}

	/**
	 * 获取汇率类型
	 *
	 * @return 本币币种.ID
	 */
	public String getExchangeRateType() {
		return get("exchangeRateType");
	}

	/**
	 * 设置汇率类型
	 *
	 * @param exchangeRateType 本币币种.ID
	 */
	public void setExchangeRateType(String exchangeRateType) {
		set("exchangeRateType", exchangeRateType);
	}

	/**
	 * 获取凭证状态
	 *
	 * @return 凭证状态
	 */
	public VoucherStatus getVoucherstatus() {
		Number v = get("voucherstatus");
		return  VoucherStatus.find(v);
	}

	/**
	 * 设置凭证状态
	 *
	 * @param voucherstatus 凭证状态
	 */
	public void setVoucherstatus( VoucherStatus voucherstatus) {
		if (voucherstatus != null) {
			set("voucherstatus", voucherstatus.getValue());
		} else {
			set("voucherstatus", null);
		}
	}

	/**
	 * 获取本币币种
	 *
	 * @return 本币币种.ID
	 */
	public String getNatCurrency() {
		return get("natCurrency");
	}

	/**
	 * 设置本币币种
	 *
	 * @param natCurrency 本币币种.ID
	 */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

	/**
	 * 获取打印次数
	 *
	 * @return 打印次数
	 */
	public Integer getPrintCount() {
		return get("printCount");
	}

	/**
	 * 设置打印次数
	 *
	 * @param printCount 打印次数
	 */
	public void setPrintCount(Integer printCount) {
		set("printCount", printCount);
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
     * 获取凭证状态说明
     *
     * @return 凭证状态说明
     */
    public String getVoucherdes() {
        return get("voucherdes");
    }

    /**
     * 设置凭证状态说明
     *
     * @param voucherdes 凭证状态说明
     */
    public void setVoucherdes(String voucherdes) {
        set("voucherdes", voucherdes);
    }

    /**
     * 获取冲销日期
     *
     * @return 冲销日期
     */
    public java.util.Date getCoverDate() {
        return get("coverDate");
    }

    /**
     * 设置冲销日期
     *
     * @param coverDate 冲销日期
     */
    public void setCoverDate(java.util.Date coverDate) {
        set("coverDate", coverDate);
    }


    /**
     * 获取下月初冲销
     *
     * @return 下月初冲销
     */
    public Boolean getNextMonthCover() {
        return getBoolean("nextMonthCover");
    }

    /**
     * 设置下月初冲销
     *
     * @param nextMonthCover 下月初冲销
     */
    public void setNextMonthCover(Boolean nextMonthCover) {
        set("nextMonthCover", nextMonthCover);
    }

    /**
     * 获取关联单据id
     *
     * @return 关联单据id
     */
    public Long getAssociationid() {
        return get("associationid");
    }

    /**
     * 设置关联单据id
     *
     * @param associationid 关联单据id
     */
    public void setAssociationid(Long associationid) {
        set("associationid", associationid);
    }

    /**
     * 获取关联单据编码
     *
     * @return 关联单据编码
     */
    public String getAssociationcode() {
        return get("associationcode");
    }

    /**
     * 设置关联单据编码
     *
     * @param associationcode 关联单据编码
     */
    public void setAssociationcode(String associationcode) {
        set("associationcode", associationcode);
    }
    /**
     * 获取是否冲销单据
     *
     * @return 是否冲销单据
     */
    public Boolean getIsCover() {
        return getBoolean("isCover");
    }

    /**
     * 设置是否冲销单据
     *
     * @param isCover 是否冲销单据
     */
    public void setIsCover(Boolean isCover) {
        set("isCover", isCover);
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
}
