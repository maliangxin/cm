package com.yonyoucloud.fi.cmp.checkinventory;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 盘点支票簿实体
 *
 * @author u
 * @version 1.0
 */
public class CheckInventory_b extends BizObject implements ITenant, IYTenant, IBackWrite {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.checkinventory.CheckInventory_b";

	/**
	 * 获取支票盘点主表id
	 *
	 * @return 支票盘点主表id.ID
	 */
	public Long getMainid() {
		return get("mainid");
	}

	/**
	 * 设置支票盘点主表id
	 *
	 * @param mainid 支票盘点主表id.ID
	 */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

	/**
	 * 获取支票簿盘点结果
	 *
	 * @return 支票簿盘点结果
	 */
	public String getCheckresult() {
		return get("checkresult");
	}

	/**
	 * 设置支票簿盘点结果
	 *
	 * @param checkresult 支票簿盘点结果
	 */
	public void setCheckresult(String checkresult) {
		set("checkresult", checkresult);
	}

	/**
	 * 获取支票簿盘点结果说明
	 *
	 * @return 支票簿盘点结果说明
	 */
	public String getCheckchildresult() {
		return get("checkchildresult");
	}

	/**
	 * 设置支票簿盘点结果说明
	 *
	 * @param checkchildresult 支票簿盘点结果说明
	 */
	public void setCheckchildresult(String checkchildresult) {
		set("checkchildresult", checkchildresult);
	}

	/**
	 * 获取支票方向
	 *
	 * @return 支票方向
	 */
	public Short getCheckdirection() {
		return getShort("checkdirection");
	}

	/**
	 * 设置支票方向
	 *
	 * @param checkdirection 支票方向
	 */
	public void setCheckdirection(Short checkdirection) {
		set("checkdirection", checkdirection);
	}

	/**
	 * 获取支票簿编号
	 *
	 * @return 支票簿编号
	 */
	public String getCheckBillNo() {
		return get("checkBillNo");
	}

	/**
	 * 设置支票簿编号
	 *
	 * @param checkBillNo 支票簿编号
	 */
	public void setCheckBillNo(String checkBillNo) {
		set("checkBillNo", checkBillNo);
	}

	/**
	 * 获取起始编号
	 *
	 * @return 起始编号
	 */
	public String getStartNo() {
		return get("startNo");
	}

	/**
	 * 设置起始编号
	 *
	 * @param startNo 起始编号
	 */
	public void setStartNo(String startNo) {
		set("startNo", startNo);
	}

	/**
	 * 获取终止编号
	 *
	 * @return 终止编号
	 */
	public String getEndNo() {
		return get("endNo");
	}

	/**
	 * 设置终止编号
	 *
	 * @param endNo 终止编号
	 */
	public void setEndNo(String endNo) {
		set("endNo", endNo);
	}

	/**
	 * 获取台账数量(张)
	 *
	 * @return 台账数量(张)
	 */
	public Long getCheckquantity() {
		return get("checkquantity");
	}

	/**
	 * 设置台账数量(张)
	 *
	 * @param checkquantity 台账数量(张)
	 */
	public void setCheckquantity(Long checkquantity) {
		set("checkquantity", checkquantity);
	}

	/**
	 * 获取差异数量(张)
	 *
	 * @return 差异数量(张)
	 */
	public Long getDiffquantity() {
		return get("diffquantity");
	}

	/**
	 * 设置差异数量(张)
	 *
	 * @param diffquantity 差异数量(张)
	 */
	public void setDiffquantity(Long diffquantity) {
		set("diffquantity", diffquantity);
	}

	/**
	 * 获取实物数量(张)
	 *
	 * @return 实物数量(张)
	 */
	public Long getMaterialquantity() {
		return get("materialquantity");
	}

	/**
	 * 设置实物数量(张)
	 *
	 * @param materialquantity 实物数量(张)
	 */
	public void setMaterialquantity(Long materialquantity) {
		set("materialquantity", materialquantity);
	}

	/**
	 * 获取支票类型
	 *
	 * @return 支票类型
	 */
	public Short getCheckBillType() {
		return getShort("checkBillType");
	}

	/**
	 * 设置支票类型
	 *
	 * @param checkBillType 支票类型
	 */
	public void setCheckBillType(Short checkBillType) {
		set("checkBillType", checkBillType);
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
	 * 设置支票关联id
	 *
	 * @param checkid 支票关联id
	 */
	public void setCheckid(String checkid) {
		set("checkid", checkid);
	}

	/**
	 * 获取支票关联id
	 *
	 * @return 支票簿支票关联id
	 */
	public String getCheckid() {
		return get("checkid");
	}

	/**
	 * 获取入库日期
	 *
	 * @return 入库日期
	 */
	public java.util.Date getBusiDate() {
		return get("busiDate");
	}

	/**
	 * 设置入库日期
	 *
	 * @param busiDate 入库日期
	 */
	public void setBusiDate(java.util.Date busiDate) {
		set("busiDate", busiDate);
	}

	/**
	 * 获取支票盘点子表特征
	 *
	 * @return 支票盘点子表特征.ID
	 */
	public String getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置支票盘点子表特征
	 *
	 * @param characterDefb 支票盘点子表特征.ID
	 */
	public void setCharacterDefb(String characterDefb) {
		set("characterDefb", characterDefb);
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
	 * 获取租户id
	 *
	 * @return 租户id.ID
	 */
	public String getYtenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id.ID
	 */
	public void setYtenant(String ytenant) {
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
	 * 获取银行网点
	 *
	 * @return 银行网点.ID
	 */
	public String getBank() {
		return get("bank");
	}

	/**
	 * 设置银行网点
	 *
	 * @param bank 银行网点.ID
	 */
	public void setBank(String bank) {
		set("bank", bank);
	}


	/**
	 * 获取银行账号
	 *
	 * @return 银行账号
	 */
	public String getAccountNo() {
		return get("accountNo");
	}

	/**
	 * 设置银行账号
	 *
	 * @param accountNo 银行账号
	 */
	public void setAccountNo(String accountNo) {
		set("accountNo", accountNo);
	}
	/**
	 * 获取上期盘点数据(张)
	 *
	 * @return 上期盘点数据(张)
	 */
	public Integer getPrevPeriodCount() {
		return get("prevPeriodCount");
	}

	/**
	 * 设置上期盘点数据(张)
	 *
	 * @param prevPeriodCount 上期盘点数据(张)
	 */
	public void setPrevPeriodCount(Integer prevPeriodCount) {
		set("prevPeriodCount", prevPeriodCount);
	}
	/**
	 * 获取本期已处置数量(张)
	 *
	 * @return 本期已处置数量(张)
	 */
	public Integer getCurHandleCount() {
		return get("curHandleCount");
	}

	/**
	 * 设置本期已处置数量(张)
	 *
	 * @param curHandleCount 本期已处置数量(张)
	 */
	public void setCurHandleCount(Integer curHandleCount) {
		set("curHandleCount", curHandleCount);
	}

	/**
	 * 获取本期已使用数量(张)
	 *
	 * @return 本期已使用数量(张)
	 */
	public Integer getCurUseCount() {
		return get("curUseCount");
	}

	/**
	 * 设置本期已使用数量(张)
	 *
	 * @param curUseCount 本期已使用数量(张)
	 */
	public void setCurUseCount(Integer curUseCount) {
		set("curUseCount", curUseCount);
	}
	/**
	 * 获取支票编号
	 *
	 * @return 支票编号
	 */
	public String getCheckNumber() {
		return get("checkNumber");
	}

	/**
	 * 设置支票编号
	 *
	 * @param checkNumber 支票编号
	 */
	public void setCheckNumber(String checkNumber) {
		set("checkNumber", checkNumber);
	}
	/**
	 * 获取备注
	 *
	 * @return 备注
	 */
	public String getRemark() {
		return get("remark");
	}

	/**
	 * 设置备注
	 *
	 * @param remark 备注
	 */
	public void setRemark(String remark) {
		set("remark", remark);
	}

}
