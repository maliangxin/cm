package com.yonyoucloud.fi.cmp.bankelectronicreceiptfilelogger;

import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行电子回单归档日志实体
 *
 * @author u
 * @version 1.0
 */
public class BankElectronicReceiptFileLogger extends BizObject implements ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankelectronicreceiptfilelogger.BankElectronicReceiptFileLogger";

	/**
	 * 获取核算账簿id
	 *
	 * @return 核算账簿id
	 */
	public String getPk() {
		return get("pk");
	}

	/**
	 * 设置核算账簿id
	 *
	 * @param pk 核算账簿id
	 */
	public void setPk(String pk) {
		set("pk", pk);
	}

	/**
	 * 获取单位类型
	 *
	 * @return 单位类型
	 */
	public String getItype() {
		return get("itype");
	}

	/**
	 * 设置单位类型
	 *
	 * @param itype 单位类型
	 */
	public void setItype(String itype) {
		set("itype", itype);
	}

	/**
	 * 获取档案类型
	 *
	 * @return 档案类型
	 */
	public String getArchivesType() {
		return get("archivesType");
	}

	/**
	 * 设置档案类型
	 *
	 * @param archivesType 档案类型
	 */
	public void setArchivesType(String archivesType) {
		set("archivesType", archivesType);
	}

	/**
	 * 获取入账信息专用
	 *
	 * @return 入账信息专用
	 */
	public String getTicketInfo() {
		return get("ticketInfo");
	}

	/**
	 * 设置入账信息专用
	 *
	 * @param ticketInfo 入账信息专用
	 */
	public void setTicketInfo(String ticketInfo) {
		set("ticketInfo", ticketInfo);
	}

	/**
	 * 获取会计年
	 *
	 * @return 会计年
	 */
	public String getAccountYear() {
		return get("accountYear");
	}

	/**
	 * 设置会计年
	 *
	 * @param accountYear 会计年
	 */
	public void setAccountYear(String accountYear) {
		set("accountYear", accountYear);
	}

	/**
	 * 获取会计月
	 *
	 * @return 会计月
	 */
	public String getAccountMonth() {
		return get("accountMonth");
	}

	/**
	 * 设置会计月
	 *
	 * @param accountMonth 会计月
	 */
	public void setAccountMonth(String accountMonth) {
		set("accountMonth", accountMonth);
	}

	/**
	 * 获取日期
	 *
	 * @return 日期
	 */
	public String getAccountDate() {
		return get("accountDate");
	}

	/**
	 * 设置日期
	 *
	 * @param accountDate 日期
	 */
	public void setAccountDate(String accountDate) {
		set("accountDate", accountDate);
	}

	/**
	 * 获取采集时间范围
	 *
	 * @return 采集时间范围
	 */
	public String getIrange() {
		return get("irange");
	}

	/**
	 * 设置采集时间范围
	 *
	 * @param irange 采集时间范围
	 */
	public void setIrange(String irange) {
		set("irange", irange);
	}

	/**
	 * 获取是否采集上游单据数据
	 *
	 * @return 是否采集上游单据数据
	 */
	public String getShowUpper() {
		return get("showUpper");
	}

	/**
	 * 设置是否采集上游单据数据
	 *
	 * @param showUpper 是否采集上游单据数据
	 */
	public void setShowUpper(String showUpper) {
		set("showUpper", showUpper);
	}

	/**
	 * 获取任务id
	 *
	 * @return 任务id
	 */
	public String getTaskId() {
		return get("taskId");
	}

	/**
	 * 设置任务id
	 *
	 * @param taskId 任务id
	 */
	public void setTaskId(String taskId) {
		set("taskId", taskId);
	}

	/**
	 * 获取档案回调地址
	 *
	 * @return 档案回调地址
	 */
	public String getCallbackUrl() {
		return get("callbackUrl");
	}

	/**
	 * 设置档案回调地址
	 *
	 * @param callbackUrl 档案回调地址
	 */
	public void setCallbackUrl(String callbackUrl) {
		set("callbackUrl", callbackUrl);
	}

	/**
	 * 获取文件服务地址
	 *
	 * @return 文件服务地址
	 */
	public String getEndpoint() {
		return get("endpoint");
	}

	/**
	 * 设置文件服务地址
	 *
	 * @param endpoint 文件服务地址
	 */
	public void setEndpoint(String endpoint) {
		set("endpoint", endpoint);
	}

	/**
	 * 获取用户名
	 *
	 * @return 用户名
	 */
	public String getAccessKey() {
		return get("accessKey");
	}

	/**
	 * 设置用户名
	 *
	 * @param accessKey 用户名
	 */
	public void setAccessKey(String accessKey) {
		set("accessKey", accessKey);
	}

	/**
	 * 获取密码
	 *
	 * @return 密码
	 */
	public String getSecretKey() {
		return get("secretKey");
	}

	/**
	 * 设置密码
	 *
	 * @param secretKey 密码
	 */
	public void setSecretKey(String secretKey) {
		set("secretKey", secretKey);
	}

	/**
	 * 获取文件服务类型
	 *
	 * @return 文件服务类型
	 */
	public String getServerType() {
		return get("serverType");
	}

	/**
	 * 设置文件服务类型
	 *
	 * @param serverType 文件服务类型
	 */
	public void setServerType(String serverType) {
		set("serverType", serverType);
	}

	/**
	 * 获取桶名
	 *
	 * @return 桶名
	 */
	public String getBucketName() {
		return get("bucketName");
	}

	/**
	 * 设置桶名
	 *
	 * @param bucketName 桶名
	 */
	public void setBucketName(String bucketName) {
		set("bucketName", bucketName);
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

}
