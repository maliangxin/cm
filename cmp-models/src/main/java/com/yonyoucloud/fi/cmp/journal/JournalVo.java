package com.yonyoucloud.fi.cmp.journal;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 日记账对外接口实体
 *
 * @author u
 * @version 1.0
 */
@Data
public class JournalVo implements Serializable {

	private static final long serialVersionUID = 4566850457565319584L;

	//获取来源单集合  保存 更新使用
	private List<Journal> journalList;

	//唯一标识 加锁使用  要求：来源系统+资金组织+唯一id
	private String uniqueIdentification;

	//更新接口使用的缓存判断  不作为传输参数
	private Map<String,Journal> journalMap;

	//获取来源单据行号集合  删除使用不作为传输参数，自己代码逻辑里面使用
	private List<String> srcbillitemnoList;

	//缓存旧数据
	private String tempAccentity;

	//代码中临时使用
	private List<Journal> oldJournalList;

	//代码中临时使用
	private short tempBilltype;

	//是否重复插入日记账
	private boolean reinsert;
	//日记账红冲金额
	private BigDecimal hcsum;

	// 更新凭证信息
	// 操作类型  更新凭证 cmpUpdateVoucher; 更新日记账 cmpUpdateJournal
	private String operateType;

	/**
	 * 凭证唯一标志
	 */
	private String voucheronlyno;

	/**
	 * 日记账金额
	 */
	private BigDecimal journalSum;

	/**
	 * 凭证号
	 */
	private String voucherNo;

	/**
	 * 凭证期间
	 */
	private String voucherPeriod;

	/**
	 * 来源单据id
	 */
	private String srcitmeid;

	/**
	 * 友互通租户ID
	 */
	private String ytenantId;



}
