package com.yonyoucloud.fi.cmp.receivebill;


import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.INatMoneyInfo;
import com.yonyou.ucf.mdd.ext.voucher.base.IOriMoneyInfo;
import com.yonyou.ucf.mdd.ext.voucher.base.IUOMInfo;
import com.yonyoucloud.fi.cmp.cmpentity.CmpbillEntityDetail;
import org.imeta.orm.base.BizObject;

/**
 * 收款单子表实体
 *
 * @author u
 * @version 1.0
 */
public class ReceiveBill_b extends CmpbillEntityDetail implements IUOMInfo, IOriMoneyInfo, INatMoneyInfo, IBackWrite, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.receivebill.ReceiveBill_b";

    /**
     * 获取收款单id
     *
     * @return 收款单id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置收款单id
     *
     * @param mainid 收款单id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}


	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public BizObject getReceiveBillCharacterDefb() {
		return get("receiveBillCharacterDefb");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param receiveBillCharacterDefb 自定义项特征属性组.ID
	 */
	public void setReceiveBillCharacterDefb(BizObject receiveBillCharacterDefb) {
		set("receiveBillCharacterDefb", receiveBillCharacterDefb);
	}

    /**
     * 获取物料
     *
     * @return 物料.ID
     */
	public Long getMaterial() {
		return get("material");
	}

    /**
     * 获取款项类型
     *
     * @return 款项类型.ID
     */
	public Long getQuickType() {
		return get("quickType");
	}

    /**
     * 设置款项类型
     *
     * @param quickType 款项类型.ID
     */
	public void setQuickType(Long quickType) {
		set("quickType", quickType);
	}

    /**
     * 获取款项类型code
     *
     * @return 款项类型code
     */
	public String getQuickTypeCode() {
		return get("quickTypeCode");
	}

    /**
     * 设置款项类型code
     *
     * @param quickTypeCode 款项类型code
     */
	public void setQuickTypeCode(String quickTypeCode) {
		set("quickTypeCode", quickTypeCode);
	}

	/**
	 * 设置物料
	 *
	 * @param material 物料.ID
	 */
	public void setMaterial(Long material) {
		set("material", material);
	}

    /**
     * 获取收款金额
     *
     * @return 收款金额
     */
	public java.math.BigDecimal getOriSum() {
		return get("oriSum");
	}

    /**
     * 设置收款金额
     *
     * @param oriSum 收款金额
     */
	public void setOriSum(java.math.BigDecimal oriSum) {
		set("oriSum", oriSum);
	}

    /**
     * 获取本币金额
     *
     * @return 本币金额
     */
	public java.math.BigDecimal getNatSum() {
		return get("natSum");
	}

    /**
     * 设置本币金额
     *
     * @param natSum 本币金额
     */
	public void setNatSum(java.math.BigDecimal natSum) {
		set("natSum", natSum);
	}

    /**
     * 获取余额
     *
     * @return 余额
     */
	public java.math.BigDecimal getBalance() {
		return get("balance");
	}

    /**
     * 设置余额
     *
     * @param balance 余额
     */
	public void setBalance(java.math.BigDecimal balance) {
		set("balance", balance);
	}

    /**
     * 获取本币余额
     *
     * @return 本币余额
     */
	public java.math.BigDecimal getLocalbalance() {
		return get("localbalance");
	}

    /**
     * 设置本币余额
     *
     * @param localbalance 本币余额
     */
	public void setLocalbalance(java.math.BigDecimal localbalance) {
		set("localbalance", localbalance);
	}

    /**
     * 获取客户
     *
     * @return 客户.ID
     */
	public Long getCustomer() {
		return get("customer");
	}

    /**
     * 设置客户
     *
     * @param customer 客户.ID
     */
	public void setCustomer(Long customer) {
		set("customer", customer);
	}

    /**
     * 获取供应商
     *
     * @return 供应商.ID
     */
	public Long getSupplier() {
		return get("supplier");
	}

    /**
     * 设置供应商
     *
     * @param supplier 供应商.ID
     */
	public void setSupplier(Long supplier) {
		set("supplier", supplier);
	}

    /**
     * 获取部门
     *
     * @return 部门.ID
     */
	public String getDept() {
        return get("dept");
    }
    /**
     * 设置部门
     *
     * @param dept 部门.ID
     */
	public void setDept(String dept) {
		set("dept", dept);
	}

    /**
     * 获取业务员
     *
     * @return 业务员.ID
     */
	public String getOperator() {
		return get("operator");
	}

    /**
     * 设置业务员
     *
     * @param operator 业务员.ID
     */
	public void setOperator(String operator) {
		set("operator", operator);
	}
	/**
	 * 获取业务员新
	 *
	 * @return 业务员新.ID
	 */
	public String getOperatornew() {
		return get("operatornew");
	}

	/**
	 * 设置业务员新
	 *
	 * @param operatornew 业务员新.ID
	 */
	public void setOperatornew(String operatornew) {
		set("operatornew", operatornew);
	}
    /**
     * 获取项目
     *
     * @return 项目.ID
     */
	public String getProject() {
		return get("project");
	}

    /**
     * 设置项目
     *
     * @param project 项目.ID
     */
	public void setProject(String project) {
		set("project", project);
	}

    /**
     * 获取订单编号
     *
     * @return 订单编号
     */
	public String getOrderno() {
		return get("orderno");
	}

    /**
     * 设置订单编号
     *
     * @param orderno 订单编号
     */
	public void setOrderno(String orderno) {
		set("orderno", orderno);
	}

    /**
     * 获取备注
     *
     * @return 备注
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置备注
     *
     * @param description 备注
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取来源单据类型
     *
     * @return 来源单据类型
     */
	public String getSrcbilltype() {
		return get("srcbilltype");
	}

    /**
     * 设置来源单据类型
     *
     * @param srcbilltype 来源单据类型
     */
	public void setSrcbilltype(String srcbilltype) {
		set("srcbilltype", srcbilltype);
	}

    /**
     * 获取来源单据号
     *
     * @return 来源单据号
     */
	public String getSrcbillno() {
		return get("srcbillno");
	}

    /**
     * 设置来源单据号
     *
     * @param srcbillno 来源单据号
     */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
	}

    /**
     * 获取来源单据行号
     *
     * @return 来源单据行号
     */
	public String getSrcbillitemno() {
		return get("srcbillitemno");
	}

    /**
     * 设置来源单据行号
     *
     * @param srcbillitemno 来源单据行号
     */
	public void setSrcbillitemno(String srcbillitemno) {
		set("srcbillitemno", srcbillitemno);
	}

    /**
     * 获取源头单据号
     *
     * @return 源头单据号
     */
	public String getTopsrcbillno() {
		return get("topsrcbillno");
	}

    /**
     * 设置源头单据号
     *
     * @param topsrcbillno 源头单据号
     */
	public void setTopsrcbillno(String topsrcbillno) {
		set("topsrcbillno", topsrcbillno);
	}

    /**
     * 获取源头单据行号
     *
     * @return 源头单据行号
     */
	public String getTopsrcbillitemno() {
		return get("topsrcbillitemno");
	}

    /**
     * 设置源头单据行号
     *
     * @param topsrcbillitemno 源头单据行号
     */
	public void setTopsrcbillitemno(String topsrcbillitemno) {
		set("topsrcbillitemno", topsrcbillitemno);
	}

    /**
     * 获取源头单据行id
     *
     * @return 源头单据行id
     */
	public String getTopsrcbillitemid() {
		return get("topsrcbillitemid");
	}

    /**
     * 设置源头单据行id
     *
     * @param topsrcbillitemid 源头单据行id
     */
	public void setTopsrcbillitemid(String topsrcbillitemid) {
		set("topsrcbillitemid", topsrcbillitemid);
	}

    /**
     * 获取来源单据行id
     *
     * @return 来源单据行id
     */
	public String getSrcbillitemid() {
		return get("srcbillitemid");
	}

    /**
     * 设置来源单据行id
     *
     * @param srcbillitemid 来源单据行id
     */
	public void setSrcbillitemid(String srcbillitemid) {
		set("srcbillitemid", srcbillitemid);
	}

    /**
     * 获取来源单据id
     *
     * @return 来源单据id
     */
	public String getSrcbillid() {
		return get("srcbillid");
	}

    /**
     * 设置来源单据id
     *
     * @param srcbillid 来源单据id
     */
	public void setSrcbillid(String srcbillid) {
		set("srcbillid", srcbillid);
	}

    /**
     * 获取生单关联主表id
     *
     * @return 生单关联主表id
     */
	public String getPushsrcbillmid() {
		return get("pushsrcbillmid");
	}

    /**
     * 设置生单关联主表id
     *
     * @param pushsrcbillmid 生单关联主表id
     */
	public void setPushsrcbillmid(String pushsrcbillmid) {
		set("pushsrcbillmid", pushsrcbillmid);
	}

    /**
     * 获取销售组织
     *
     * @return 销售组织.ID
     */
	public String getOrg() {
		return get("org");
	}

    /**
     * 设置销售组织
     *
     * @param org 销售组织.ID
     */
	public void setOrg(String org) {
		set("org", org);
	}

    /**
     * 获取科目
     *
     * @return 科目
     */
	public String getSubject() {
		return get("subject");
	}

    /**
     * 设置科目
     *
     * @param subject 科目
     */
	public void setSubject(String subject) {
		set("subject", subject);
	}

    /**
     * 获取委托主体
     *
     * @return 委托主体
     */
	public String getDelegation() {
		return get("delegation");
	}

    /**
     * 设置委托主体
     *
     * @param delegation 委托主体
     */
	public void setDelegation(String delegation) {
		set("delegation", delegation);
	}

    /**
     * 获取挂起
     *
     * @return 挂起
     */
	public Boolean getHangflag() {
	    return getBoolean("hangflag");
	}

    /**
     * 设置挂起
     *
     * @param hangflag 挂起
     */
	public void setHangflag(Boolean hangflag) {
		set("hangflag", hangflag);
	}

    /**
     * 获取费用项目
     *
     * @return 费用项目.ID
     */
	public Long getExpenseitem() {
		return get("expenseitem");
	}

    /**
     * 设置费用项目
     *
     * @param expenseitem 费用项目.ID
     */
	public void setExpenseitem(Long expenseitem) {
		set("expenseitem", expenseitem);
	}

    /**
     * 获取预占用金额
     *
     * @return 预占用金额
     */
	public java.math.BigDecimal getBookAmount() {
		return get("bookAmount");
	}

    /**
     * 设置预占用金额
     *
     * @param bookAmount 预占用金额
     */
	public void setBookAmount(java.math.BigDecimal bookAmount) {
		set("bookAmount", bookAmount);
	}

    /**
     * 获取票据类型
     *
     * @return 票据类型.ID
     */
	public Long getNotetype() {
		return get("notetype");
	}

    /**
     * 设置票据类型
     *
     * @param notetype 票据类型.ID
     */
	public void setNotetype(Long notetype) {
		set("notetype", notetype);
	}

    /**
     * 获取票据号
     *
     * @return 票据号.ID
     */
	public Long getNoteno() {
		return get("noteno");
	}

    /**
     * 设置票据号
     *
     * @param noteno 票据号.ID
     */
	public void setNoteno(Long noteno) {
		set("noteno", noteno);
	}

    /**
     * 获取期号
     *
     * @return 期号
     */
	public Integer getIssue() {
		return get("issue");
	}

    /**
     * 设置期号
     *
     * @param issue 期号
     */
	public void setIssue(Integer issue) {
		set("issue", issue);
	}

    /**
     * 获取合同编号
     *
     * @return 合同编号
     */
	public String getContractno() {
		return get("contractno");
	}

    /**
     * 设置合同编号
     *
     * @param contractno 合同编号
     */
	public void setContractno(String contractno) {
		set("contractno", contractno);
	}

    /**
     * 获取主计量数量
     *
     * @return 主计量数量
     */
	public java.math.BigDecimal getQty() {
		return get("qty");
	}

    /**
     * 设置主计量数量
     *
     * @param qty 主计量数量
     */
	public void setQty(java.math.BigDecimal qty) {
		set("qty", qty);
	}

    /**
     * 获取副计量数量
     *
     * @return 副计量数量
     */
	public java.math.BigDecimal getSubQty() {
		return get("subQty");
	}

    /**
     * 设置副计量数量
     *
     * @param subQty 副计量数量
     */
	public void setSubQty(java.math.BigDecimal subQty) {
		set("subQty", subQty);
	}

    /**
     * 获取换算率
     *
     * @return 换算率
     */
	public java.math.BigDecimal getInvExchRate() {
		return get("invExchRate");
	}

    /**
     * 设置换算率
     *
     * @param invExchRate 换算率
     */
	public void setInvExchRate(java.math.BigDecimal invExchRate) {
		set("invExchRate", invExchRate);
	}

    /**
     * 获取税率
     *
     * @return 税率
     */
	public java.math.BigDecimal getTaxRate() {
		return get("taxRate");
	}

    /**
     * 设置税率
     *
     * @param taxRate 税率
     */
	public void setTaxRate(java.math.BigDecimal taxRate) {
		set("taxRate", taxRate);
	}

    /**
     * 获取原币税额
     *
     * @return 原币税额
     */
	public java.math.BigDecimal getOriTax() {
		return get("oriTax");
	}

    /**
     * 设置原币税额
     *
     * @param oriTax 原币税额
     */
	public void setOriTax(java.math.BigDecimal oriTax) {
		set("oriTax", oriTax);
	}

    /**
     * 获取原币无税单价
     *
     * @return 原币无税单价
     */
	public java.math.BigDecimal getOriUnitPrice() {
		return get("oriUnitPrice");
	}

    /**
     * 设置原币无税单价
     *
     * @param oriUnitPrice 原币无税单价
     */
	public void setOriUnitPrice(java.math.BigDecimal oriUnitPrice) {
		set("oriUnitPrice", oriUnitPrice);
	}

    /**
     * 获取原币含税单价
     *
     * @return 原币含税单价
     */
	public java.math.BigDecimal getOriTaxUnitPrice() {
		return get("oriTaxUnitPrice");
	}

    /**
     * 设置原币含税单价
     *
     * @param oriTaxUnitPrice 原币含税单价
     */
	public void setOriTaxUnitPrice(java.math.BigDecimal oriTaxUnitPrice) {
		set("oriTaxUnitPrice", oriTaxUnitPrice);
	}

    /**
     * 获取原币无税金额
     *
     * @return 原币无税金额
     */
	public java.math.BigDecimal getOriMoney() {
		return get("oriMoney");
	}

    /**
     * 设置原币无税金额
     *
     * @param oriMoney 原币无税金额
     */
	public void setOriMoney(java.math.BigDecimal oriMoney) {
		set("oriMoney", oriMoney);
	}

    /**
     * 获取本币税额
     *
     * @return 本币税额
     */
	public java.math.BigDecimal getNatTax() {
		return get("natTax");
	}

    /**
     * 设置本币税额
     *
     * @param natTax 本币税额
     */
	public void setNatTax(java.math.BigDecimal natTax) {
		set("natTax", natTax);
	}

    /**
     * 获取本币含税单价
     *
     * @return 本币含税单价
     */
	public java.math.BigDecimal getNatTaxUnitPrice() {
		return get("natTaxUnitPrice");
	}

    /**
     * 设置本币含税单价
     *
     * @param natTaxUnitPrice 本币含税单价
     */
	public void setNatTaxUnitPrice(java.math.BigDecimal natTaxUnitPrice) {
		set("natTaxUnitPrice", natTaxUnitPrice);
	}

    /**
     * 获取本币无税金额
     *
     * @return 本币无税金额
     */
	public java.math.BigDecimal getNatMoney() {
		return get("natMoney");
	}

    /**
     * 设置本币无税金额
     *
     * @param natMoney 本币无税金额
     */
	public void setNatMoney(java.math.BigDecimal natMoney) {
		set("natMoney", natMoney);
	}

    /**
     * 获取本币无税单价
     *
     * @return 本币无税单价
     */
	public java.math.BigDecimal getNatUnitPrice() {
		return get("natUnitPrice");
	}

    /**
     * 设置本币无税单价
     *
     * @param natUnitPrice 本币无税单价
     */
	public void setNatUnitPrice(java.math.BigDecimal natUnitPrice) {
		set("natUnitPrice", natUnitPrice);
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
     * 获取自定义项1
     *
     * @return 自定义项1
     */
	@Override
	public String getDefine1() {
		return get("define1");
	}

    /**
     * 设置自定义项1
     *
     * @param define1 自定义项1
     */
	@Override
	public void setDefine1(String define1) {
		set("define1", define1);
	}

    /**
     * 获取自定义项2
     *
     * @return 自定义项2
     */
	public String getDefine2() {
		return get("define2");
	}

    /**
     * 设置自定义项2
     *
     * @param define2 自定义项2
     */
	public void setDefine2(String define2) {
		set("define2", define2);
	}

    /**
     * 获取自定义项3
     *
     * @return 自定义项3
     */
	public String getDefine3() {
		return get("define3");
	}

    /**
     * 设置自定义项3
     *
     * @param define3 自定义项3
     */
	public void setDefine3(String define3) {
		set("define3", define3);
	}

    /**
     * 获取自定义项4
     *
     * @return 自定义项4
     */
	public String getDefine4() {
		return get("define4");
	}

    /**
     * 设置自定义项4
     *
     * @param define4 自定义项4
     */
	public void setDefine4(String define4) {
		set("define4", define4);
	}

    /**
     * 获取自定义项5
     *
     * @return 自定义项5
     */
	public String getDefine5() {
		return get("define5");
	}

    /**
     * 设置自定义项5
     *
     * @param define5 自定义项5
     */
	public void setDefine5(String define5) {
		set("define5", define5);
	}

    /**
     * 获取自定义项6
     *
     * @return 自定义项6
     */
	public String getDefine6() {
		return get("define6");
	}

    /**
     * 设置自定义项6
     *
     * @param define6 自定义项6
     */
	public void setDefine6(String define6) {
		set("define6", define6);
	}

    /**
     * 获取自定义项7
     *
     * @return 自定义项7
     */
	public String getDefine7() {
		return get("define7");
	}

    /**
     * 设置自定义项7
     *
     * @param define7 自定义项7
     */
	public void setDefine7(String define7) {
		set("define7", define7);
	}

    /**
     * 获取自定义项8
     *
     * @return 自定义项8
     */
	public String getDefine8() {
		return get("define8");
	}

    /**
     * 设置自定义项8
     *
     * @param define8 自定义项8
     */
	public void setDefine8(String define8) {
		set("define8", define8);
	}

    /**
     * 获取自定义项9
     *
     * @return 自定义项9
     */
	public String getDefine9() {
		return get("define9");
	}

    /**
     * 设置自定义项9
     *
     * @param define9 自定义项9
     */
	public void setDefine9(String define9) {
		set("define9", define9);
	}

    /**
     * 获取自定义项10
     *
     * @return 自定义项10
     */
	public String getDefine10() {
		return get("define10");
	}

    /**
     * 设置自定义项10
     *
     * @param define10 自定义项10
     */
	public void setDefine10(String define10) {
		set("define10", define10);
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
     * 获取行号
     *
     * @return 行号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置行号
     *
     * @param rowno 行号
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
}
