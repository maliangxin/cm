package com.yonyoucloud.fi.cmp.autoparam;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.BaseConfigEnum;
import org.imeta.orm.base.BizObject;

/**
 * 自动化参数实体
 *
 * @author u
 * @version 1.0
 */
public class AutoConfig extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.autoparam.AutoConfig";

	/**
	 * 获取是否推送转账工作台
	 *
	 * @return 是否推送转账工作台
	 */
	public Boolean getCheckFundTransfer() {
		return getBoolean("checkFundTransfer");
	}

	/**
	 * 设置是否推送转账工作台
	 *
	 * @param checkFundTransfer 是否推送转账工作台
	 */
	public void setCheckFundTransfer(Boolean checkFundTransfer) {
		set("checkFundTransfer", checkFundTransfer);
	}

	/**
	 * 是否获取银行实时余额
	 * @return
	 */
	public Boolean getCheckBalanceIsQuery() {
		return getBoolean("checkBalanceIsQuery");
	}

	/**
	 * 设置是否获取银行实时余额
	 * @return
	 */
	public void setCheckBalanceIsQuery(Boolean checkBalanceIsQuery) {
		set("checkBalanceIsQuery", checkBalanceIsQuery);
	}

	/**
	 * 获取开启查询单据
	 *
	 * @return 开启查询单据
	 */
	public String getQueryBillType() {
		return get("queryBillType");
	}

	/**
	 * 设置开启查询单据
	 *
	 * @param queryBillType 开启查询单据
	 */
	public void setQueryBillType(String queryBillType) {
		set("queryBillType", queryBillType);
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
     * 获取名称
     *
     * @return 名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置名称
     *
     * @param name 名称
     */
	public void setName(String name) {
		set("name", name);
	}

    /**
     * 获取同步客商档案
     *
     * @return 同步客商档案
     */
	public BaseConfigEnum getSynDoc() {
		Number v = get("synDoc");
		return BaseConfigEnum.find(v);
	}

    /**
     * 设置同步客商档案
     *
     * @param synDoc 同步客商档案
     */
	public void setSynDoc(BaseConfigEnum synDoc) {
		if (synDoc != null) {
			set("synDoc", synDoc.getValue());
		} else {
			set("synDoc", null);
		}
	}

    /**
     * 获取自动对账
     *
     * @return 自动对账
     */
	public BaseConfigEnum getAutoReconciliation() {
		Number v = get("autoReconciliation");
		return BaseConfigEnum.find(v);
	}

    /**
     * 设置自动对账
     *
     * @param autoReconciliation 自动对账
     */
	public void setAutoReconciliation(BaseConfigEnum autoReconciliation) {
		if (autoReconciliation != null) {
			set("autoReconciliation", autoReconciliation.getValue());
		} else {
			set("autoReconciliation", null);
		}
	}

    /**
     * 获取自动生单
     *
     * @return 自动生单
     */
	public BaseConfigEnum getAutoGenBill() {
		Number v = get("autoGenBill");
		return BaseConfigEnum.find(v);
	}

    /**
     * 设置自动生单
     *
     * @param autoGenBill 自动生单
     */
	public void setAutoGenBill(BaseConfigEnum autoGenBill) {
		if (autoGenBill != null) {
			set("autoGenBill", autoGenBill.getValue());
		} else {
			set("autoGenBill", null);
		}
	}

    /**
     * 获取自动核销
     *
     * @return 自动核销
     */
	public BaseConfigEnum getAutoWriteOff() {
		Number v = get("autoWriteOff");
		return BaseConfigEnum.find(v);
	}

    /**
     * 设置自动核销
     *
     * @param autoWriteOff 自动核销
     */
	public void setAutoWriteOff(BaseConfigEnum autoWriteOff) {
		if (autoWriteOff != null) {
			set("autoWriteOff", autoWriteOff.getValue());
		} else {
			set("autoWriteOff", null);
		}
	}

    /**
     * 获取自动审核
     *
     * @return 自动审核
     */
	public BaseConfigEnum getAutoAudit() {
		Number v = get("autoAudit");
		return BaseConfigEnum.find(v);
	}

    /**
     * 设置自动审核
     *
     * @param autoAudit 自动审核
     */
	public void setAutoAudit(BaseConfigEnum autoAudit) {
		if (autoAudit != null) {
			set("autoAudit", autoAudit.getValue());
		} else {
			set("autoAudit", null);
		}
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
     * 获取收款款项类型
     *
     * @return 收款款项类型
     */
	public Long getReceiveQuickType() {
		return get("receiveQuickType");
	}

    /**
     * 设置收款款项类型
     *
     * @param receiveQuickType 收款款项类型
     */
	public void setReceiveQuickType(Long receiveQuickType) {
			set("receiveQuickType", receiveQuickType);
	}

    /**
     * 获取付款款项类型
     *
     * @return 付款款项类型
     */
	public Long getPayQuickType() {
		return get("payQuickType");
	}

    /**
     * 设置付款款项类型
     *
     * @param payQuickType 付款款项类型
     */
	public void setPayQuickType(Long payQuickType) {
			set("payQuickType", payQuickType);
	}

    /**
     * 获取默认
     *
     * @return 默认
     */
	public BaseConfigEnum getIsDefault() {
		Number v = get("isDefault");
		return BaseConfigEnum.find(v);
	}

    /**
     * 设置默认
     *
     * @param isDefault 默认
     */
	public void setIsDefault(BaseConfigEnum isDefault) {
		if (isDefault != null) {
			set("isDefault", isDefault.getValue());
		} else {
			set("isDefault", null);
		}
	}

	/**
	 * 获取是否校验UKEY
	 *
	 * @return 是否是否校验UKEY
	 */
	public Boolean getCheckUkey() {
		return getBoolean("checkUkey");
	}

	/**
	 * 设置是否校验UKEY
	 *
	 * @param checkUkey 是否是否校验UKEY
	 */
	public void setCheckUkey(Boolean checkUkey) {
		set("checkUkey", checkUkey);
	}


	/**
	 * 获取自动关联后自动确认
	 *
	 * @return 自动关联后自动确认
	 */
	public Boolean getAutoassociateconfirm() {
		return getBoolean("autoassociateconfirm");
	}

	/**
	 * 设置自动关联后自动确认
	 *
	 * @param autoassociateconfirm 自动关联后自动确认
	 */
	public void setAutoassociateconfirm(Boolean autoassociateconfirm) {
		set("autoassociateconfirm", autoassociateconfirm);
	}

	/**
	 * 获取自动生单后自动确认
	 *
	 * @return 自动生单后自动确认
	 */
	public Boolean getAutogenerateconfirm() {
		return getBoolean("autogenerateconfirm");
	}

	/**
	 * 设置自动生单后自动确认
	 *
	 * @param autogenerateconfirm 自动生单后自动确认
	 */
	public void setAutogenerateconfirm(Boolean autogenerateconfirm) {
		set("autogenerateconfirm", autogenerateconfirm);
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
	 * 获取存在未审核单据是否允许日结
	 *
	 * @return 存在未审核单据是否允许日结
	 */
	public Boolean getCheckDailySettlement() {
		return getBoolean("checkDailySettlement");
	}

	/**
	 * 设置存在未审核单据是否允许日结
	 *
	 * @param checkDailySettlement 存在未审核单据是否允许日结
	 */
	public void setCheckDailySettlement(Boolean checkDailySettlement) {
		set("checkDailySettlement", checkDailySettlement);
	}

	/**
	 * 获取是否协同生成内部单位资金收款单
	 *
	 * @return 是否协同生成内部单位资金收款单
	 */
	public Boolean getIsGenerateFundCollection() {
		return getBoolean("isGenerateFundCollection");
	}

	/**
	 * 设置是否协同生成内部单位资金收款单
	 *
	 * @param isGenerateFundCollection 是否协同生成内部单位资金收款单
	 */
	public void setIsGenerateFundCollection(Boolean isGenerateFundCollection) {
		set("isGenerateFundCollection", isGenerateFundCollection);
	}

	/**
	 * 获取是否结算成功时做账
	 *
	 * @return 是否结算成功时做账
	 */
	public Boolean getIsSettleSuccessToPost() {
		return getBoolean("isSettleSuccessToPost");
	}

	/**
	 * 设置是否结算成功时做账
	 *
	 * @param isSettleSuccessToPost 是否结算成功时做账
	 */
	public void setIsSettleSuccessToPost(Boolean isSettleSuccessToPost) {
		set("isSettleSuccessToPost", isSettleSuccessToPost);
	}
	/**
	 * 获取是否传共享影像
	 *
	 * @return 是否传共享影像
	 */
	public Boolean getIsShareVideo() {
		return getBoolean("isShareVideo");
	}

	/**
	 * 设置是否传共享影像
	 *
	 * @param isShareVideo 是否传共享影像
	 */
	public void setIsShareVideo(Boolean isShareVideo) {
		set("isShareVideo", isShareVideo);
	}

	/**
	 * 获取是否启用资金计划
	 *
	 * @return 是否启用资金计划
	 */
	public Boolean getCheckFundPlan() {
		return getBoolean("checkFundPlan");
	}

	/**
	 * 设置是否启用资金计划
	 *
	 * @param checkFundPlan 是否启用资金计划
	 */
	public void setCheckFundPlan(Boolean checkFundPlan) {
		set("checkFundPlan", checkFundPlan);
	}

	/**
	 * 获取结算检查
	 *
	 * @return 结算检查
	 */
	public String getBillCheck() {
		return get("billCheck");
	}

	/**
	 * 设置结算检查
	 *
	 * @param billCheck 结算检查
	 */
	public void setBillCheck(String billCheck) {
		set("billCheck", billCheck);
	}

	/**
	 * 获取是否结算检查
	 *
	 * @return 是否结算检查
	 */
	public Boolean getBillCheckFlag() {
		return getBoolean("billCheckFlag");
	}

	/**
	 * 设置是否结算检查
	 *
	 * @param billCheckFlag 是否结算检查
	 */
	public void setBillCheckFlag(Boolean billCheckFlag) {
		set("billCheckFlag", billCheckFlag);
	}

	/**
	 * 获取结算检查对象
	 *
	 * @return 结算检查对象
	 */
	public String getBillCheckObject() {
		return get("billCheckObject");
	}

	/**
	 * 设置结算检查对象
	 *
	 * @param billCheckObject 结算检查对象
	 */
	public void setBillCheckObject(String billCheckObject) {
		set("billCheckObject", billCheckObject);
	}

	/**
	 * 获取是否进行历史余额与对账单余额比对
	 *
	 * @return 是否进行历史余额与对账单余额比对
	 */
	public Boolean getCheckaccountbanlance() {
		return getBoolean("checkaccountbanlance");
	}

	/**
	 * 设置是否进行历史余额与对账单余额比对
	 *
	 * @param checkaccountbanlance 是否进行历史余额与对账单余额比对
	 */
	public void setCheckaccountbanlance(Boolean checkaccountbanlance) {
		set("checkaccountbanlance", checkaccountbanlance);
	}

    /**
     * 获取是否推送历史数据
     *
     * @return 是否推送历史数据
     */
    public Boolean getIsPushHistoryData() {
        return getBoolean("isPushHistoryData");
    }

    /**
     * 设置是否推送历史数据
     *
     * @param isPushHistoryData 是否推送历史数据
     */
    public void setIsPushHistoryData(Boolean isPushHistoryData) {
        set("isPushHistoryData", isPushHistoryData);
    }

	/**
	 * 获取自动关联是否严格匹配
	 *
	 * @return 自动关联是否严格匹配
	 */
	public Boolean getAccurateautoassociate() {
		return getBoolean("accurateautoassociate");
	}

	/**
	 * 设置自动关联是否严格匹配
	 *
	 * @param accurateautoassociate 自动关联是否严格匹配
	 */
	public void setAccurateautoassociate(Boolean accurateautoassociate) {
		set("accurateautoassociate", accurateautoassociate);
	}

    /**
     * 获取认领时是否启用统收统支模式
     *
     * @return 认领时是否启用统收统支模式
     */
    public Boolean getIsUnifiedIEModelWhenClaim() {
        return getBoolean("isUnifiedIEModelWhenClaim");
    }

    /**
     * 设置认领时是否启用统收统支模式
     *
     * @param isUnifiedIEModelWhenClaim 认领时是否启用统收统支模式
     */
    public void setIsUnifiedIEModelWhenClaim(Boolean isUnifiedIEModelWhenClaim) {
        set("isUnifiedIEModelWhenClaim", isUnifiedIEModelWhenClaim);
    }

    /**
     * 获取认领时是否启用资金中心代理模式
     *
     * @return 认领时是否启用资金业务委托模式
     */
    public Boolean getIsEnableBizDelegationMode() {
        return getBoolean("isEnableBizDelegationMode");
    }

    /**
     * 设置认领时是否启用资金中心代理模式
     *
     * @param isEnableBizDelegationMode 认领时是否启用资金业务委托模式
     */
    public void setIsEnableBizDelegationMode(Boolean isEnableBizDelegationMode) {
        set("isEnableBizDelegationMode", isEnableBizDelegationMode);
    }

    /**
     * 获取是否启用内转协议进行资金切分
     *
     * @return 是否启用内转协议进行资金切分
     */
    public Boolean getIsEnableInterTransAgreeFundSplitting() {
        return getBoolean("isEnableInterTransAgreeFundSplitting");
    }

    /**
     * 设置是否启用内转协议进行资金切分
     *
     * @param isEnableInterTransAgreeFundSplitting 是否启用内转协议进行资金切分
     */
    public void setIsEnableInterTransAgreeFundSplitting(Boolean isEnableInterTransAgreeFundSplitting) {
        set("isEnableInterTransAgreeFundSplitting", isEnableInterTransAgreeFundSplitting);
    }

    /**
     * 获取资金切分是否需要先完成资金归集
     *
     * @return 资金切分是否需要先完成资金归集
     */
    public Boolean getIsNeedCashSweepBeforeFundSegmentation() {
        return getBoolean("isNeedCashSweepBeforeFundSegmentation");
    }

    /**
     * 设置资金切分是否需要先完成资金归集
     *
     * @param isNeedCashSweepBeforeFundSegmentation 资金切分是否需要先完成资金归集
     */
    public void setIsNeedCashSweepBeforeFundSegmentation(Boolean isNeedCashSweepBeforeFundSegmentation) {
        set("isNeedCashSweepBeforeFundSegmentation", isNeedCashSweepBeforeFundSegmentation);
    }

	/**
	 * 获取我的认领是否需要复核
	 *
	 * @return 我的认领是否需要复核
	 */
	public Boolean getIsrecheck() {
		return getBoolean("isRecheck");
	}

	/**
	 * 设置我的认领是否需要复核
	 *
	 * @param isRecheck 我的认领是否需要复核
	 */
	public void setIsRecheck(Boolean isRecheck) {
		set("isRecheck", isRecheck);
	}

	/**
	 * 获取银企直连账户的银行对账单是否允许维护
	 *
	 * @return 银企直连账户的银行对账单是否允许维护
	 */
	public Boolean getIsBankreconciliationCanUpdate() {
		return getBoolean("isBankreconciliationCanUpdate");
	}

	/**
	 * 设置银企直连账户的银行对账单是否允许维护
	 *
	 * @param isBankreconciliationCanUpdate 银企直连账户的银行对账单是否允许维护
	 */
	public void setIsBankreconciliationCanUpdate(Boolean isBankreconciliationCanUpdate) {
		set("isBankreconciliationCanUpdate", isBankreconciliationCanUpdate);
	}



	/**
	 * 查询资金组织数量限制
	 *
	 * @return 查询资金组织数量限制
	 */
	public Integer getQueryAccentityLimit() {
		return getInteger("queryAccentityLimit");
	}

	/**
	 * 查询资金组织数量限制
	 *
	 * @param queryAccentityLimit 查询资金组织数量限制
	 */
	public void setQueryAccentityLimit(Integer queryAccentityLimit) {
		set("queryAccentityLimit", queryAccentityLimit);
	}

	/**
	 * 获取是否允许跨组织选取人员
	 *
	 * @return 是否允许跨组织选取人员
	 */
	public Boolean getAllowCrossOrgGetPerson() {
		return getBoolean("allowCrossOrgGetPerson");
	}

	/**
	 * 设置是否允许跨组织选取人员
	 *
	 * @param allowCrossOrgGetPerson 是否允许跨组织选取人员
	 */
	public void setAllowCrossOrgGetPerson(Boolean allowCrossOrgGetPerson) {
		set("allowCrossOrgGetPerson", allowCrossOrgGetPerson);
	}

	/**
	 * 重空凭证（付票）是否有领用环节
	 *
	 * @return 重空凭证（付票）是否有领用环节
	 */
	public Boolean getCheckStockIsUse() {
		return getBoolean("checkStockIsUse");
	}

	/**
	 * 重空凭证（付票）是否有领用环节
	 *
	 * @param checkStockIsUse 重空凭证（付票）是否有领用环节
	 */
	public void setCheckStockIsUse(Boolean checkStockIsUse) {
		set("checkStockIsUse", checkStockIsUse);
	}

	/**
	 * 按部门领用
	 *
	 * @return 按部门领用
	 */
	public Boolean getCheckUseByDept() {
		return getBoolean("checkUseByDept");
	}

	/**
	 * 按部门领用
	 *
	 * @param checkUseByDept 按部门领用
	 */
	public void setCheckUseByDept(Boolean checkUseByDept) {
		set("checkUseByDept", checkUseByDept);
	}

	/**
	 * 领用张数限制（张）
	 *
	 * @return 领用张数限制（张）
	 */
	public Integer getUseNumLimit() {
		return getInteger("useNumLimit");
	}

	/**
	 * 领用张数限制（张）
	 *
	 * @param useNumLimit 领用张数限制（张）
	 */
	public void setUseNumLimit(Integer useNumLimit) {
		set("useNumLimit", useNumLimit);
	}

	/**
	 * 最长未兑付期限制（天数）
	 *
	 * @return 最长未兑付期限制（天数）
	 */
	public Integer getMaxNotCashDateLimit() {
		return getInteger("maxNotCashDateLimit");
	}

	/**
	 * 最长未兑付期限制（天数）
	 *
	 * @param maxNotCashDateLimit 最长未兑付期限制（天数）
	 */
	public void setMaxNotCashDateLimit(Integer maxNotCashDateLimit) {
		set("maxNotCashDateLimit", maxNotCashDateLimit);
	}

	/**
	 * 未兑付支票总金额限制（元）
	 *
	 * @return 未兑付支票总金额限制（元）
	 */
	public java.math.BigDecimal getNotCashCheckTotalAmountLimit() {
		return get("notCashCheckTotalAmountLimit");
	}

	/**
	 * 未兑付支票总金额限制（元）
	 *
	 * @param notCashCheckTotalAmountLimit 未兑付支票总金额限制（元）
	 */
	public void setNotCashCheckTotalAmountLimit(java.math.BigDecimal notCashCheckTotalAmountLimit) {
		set("notCashCheckTotalAmountLimit", notCashCheckTotalAmountLimit);
	}

	/**
	 * 获取收付单据关联支持金额容差
	 *
	 * @return 收付单据关联支持金额容差
	 */
	public Boolean getIsAutoCorrSupportDiffAmount() {
		return getBoolean("isAutoCorrSupportDiffAmount");
	}

	/**
	 * 设置收付单据关联支持金额容差
	 *
	 * @param isAutoCorrSupportDiffAmount 收付单据关联支持金额容差
	 */
	public void setIsAutoCorrSupportDiffAmount(Boolean isAutoCorrSupportDiffAmount) {
		set("isAutoCorrSupportDiffAmount", isAutoCorrSupportDiffAmount);
	}

	/**
	 * 获取银行流水处理疑重判定开关
	 *
	 * @return 银行流水处理疑重判定开关
	 */
	public Boolean getIsBankreconciliationRepeat() {
		return getBoolean("isBankreconciliationRepeat");
	}

	/**
	 * 设置银行流水处理疑重判定开关
	 *
	 * @param isBankreconciliationRepeat 银行流水处理疑重判定开关
	 */
	public void setIsBankreconciliationRepeat(Boolean isBankreconciliationRepeat) {
		set("isBankreconciliationRepeat", isBankreconciliationRepeat);
	}

	/**
	 * 获取银无需处理的流水，是否参与银企对账、银行账户余额弥补
	 *
	 * @return 无需处理的流水，是否参与银企对账、银行账户余额弥补
	 */
	public Boolean getIsNoPorcess() {
		return getBoolean("isNoProcess");
	}

	/**
	 * 无需处理的流水，是否参与银企对账、银行账户余额弥补
	 *
	 * @param isNoProcess 无需处理的流水，是否参与银企对账、银行账户余额弥补
	 */
	public void setIsNoProcess(Boolean isNoProcess) {
		set("isNoProcess", isNoProcess);
	}

	/**
	 * 获取是否允许修改余额调节表银行账户余额
	 *
	 * @return 是否允许修改余额调节表银行账户余额
	 */
	public Boolean getBalanceadjustIsEdit() {
		return getBoolean("balanceadjustIsEdit");
	}

	/**
	 * 设置是否允许修改余额调节表银行账户余额
	 *
	 * @param balanceadjustIsEdit 是否允许修改余额调节表银行账户余额
	 */
	public void setBalanceadjustIsEdit(Boolean balanceadjustIsEdit) {
		set("balanceadjustIsEdit", balanceadjustIsEdit);
	}
    /**
     * 日记账余额排序规则 0 结算成功系统事件；1 先收后支
     * @return
     */
    public Short getJournalBalanceSortRule() {
        return getShort("journalBalanceSortRule");
    }

    /**
     * 日记账余额排序规则 0 结算成功系统事件；1 先收后支
     * @param journalBalanceSortRule
     */
    public void setJournalBalanceSortRule(Short journalBalanceSortRule) {
        set("journalBalanceSortRule", journalBalanceSortRule);
    }
}
