package com.yonyoucloud.fi.cmp.journal;

import cn.hutool.core.date.DateUtil;
import com.yonyou.iuap.formula.util.DateUtils;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 日记账实体
 *
 * @author u
 * @version 1.0
 */
public class Journal extends BizObject implements IAuditInfo, ITenant, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.journal.Journal";

	/**
	 * 获取核算会计 主体
	 *
	 * @return 核算会计 主体.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计 主体
	 *
	 * @param accentityRaw 核算会计 主体.ID
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
	}

	/**
	 * 获取所属组织
	 *
	 * @return 所属组织.ID
	 */
	public String getParentAccentity() {
		return get("parentAccentity");
	}

	/**
	 * 设置所属组织
	 *
	 * @param parentAccentity 所属组织.ID
	 */
	public void setParentAccentity(String parentAccentity) {
		set("parentAccentity", parentAccentity);
	}

    /**
     * 获取会计期间
     *
     * @return 会计期间.ID
     */
	public Long getPeriod() {
		return get("period");
	}

    /**
     * 设置会计期间
     *
     * @param period 会计期间.ID
     */
	public void setPeriod(Long period) {
		set("period", period);
	}

    /**
     * 获取银行账号
     *
     * @return 银行账号
     */
	public String getBankaccountno() {
		return get("bankaccountno");
	}

    /**
     * 设置银行账号
     *
     * @param bankaccountno 银行账号
     */
	public void setBankaccountno(String bankaccountno) {
		set("bankaccountno", bankaccountno);
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
     * 设置银行账户
     *
     * @param bankaccount 银行账户.ID
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

    /**
     * 获取现金账号
     *
     * @return 现金账号
     */
	public String getCashaccountno() {
		return get("cashaccountno");
	}

    /**
     * 设置现金账号
     *
     * @param cashaccountno 现金账号
     */
	public void setCashaccountno(String cashaccountno) {
		set("cashaccountno", cashaccountno);
	}

    /**
     * 获取现金账户
     *
     * @return 现金账户.ID
     */
	public String getCashaccount() {
		return get("cashaccount");
	}

    /**
     * 设置现金账户
     *
     * @param cashaccount 现金账户.ID
     */
	public void setCashaccount(String cashaccount) {
		set("cashaccount", cashaccount);
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
     * 获取数据内容
     *
     * @return 数据内容
     */
	public DataContent getDatacontent() {
		Number v = get("datacontent");
		return DataContent.find(v);
	}

    /**
     * 设置数据内容
     *
     * @param datacontent 数据内容
     */
	public void setDatacontent(DataContent datacontent) {
		if (datacontent != null) {
			set("datacontent", datacontent.getValue());
		} else {
			set("datacontent", null);
		}
	}

    /**
     * 获取日记账日期
     *
     * @return 日记账日期
     */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

    /**
     * 设置日记账日期
     *
     * @param dzdate 日记账日期
     */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
	}

	/**
	 * 获取单据日期
	 *
	 * @return 单据日期
	 */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

	/**
	 * 设置单据日期
	 *
	 * @param vouchdate 单据日期
	 */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
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
     * 获取事项来源
     *
     * @return 事项来源
     */
	public EventSource getSrcitem() {
		Number v = get("srcitem");
		return EventSource.find(v);
	}

    /**
     * 设置事项来源
     *
     * @param srcitem 事项来源
     */
	public void setSrcitem(EventSource srcitem) {
		if (srcitem != null) {
			set("srcitem", srcitem.getValue());
		} else {
			set("srcitem", null);
		}
	}

    /**
     * 获取事项类型
     *
     * @return 事项类型
     */
	public EventType getBilltype() {
		Number v = get("billtype");
		return EventType.find(v);
	}

    /**
     * 设置事项类型
     *
     * @param billtype 事项类型
     */
	public void setBilltype(EventType billtype) {
		if (billtype != null) {
			set("billtype", billtype.getValue());
		} else {
			set("billtype", null);
		}
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
     * 获取来源事项头线索
     *
     * @return 来源事项头线索
     */
	public Long getSourceheadclue() {
		return get("sourceheadclue");
	}

    /**
     * 设置来源事项头线索
     *
     * @param sourceheadclue 来源事项头线索
     */
	public void setSourceheadclue(Long sourceheadclue) {
		set("sourceheadclue", sourceheadclue);
	}

    /**
     * 获取来源事项行线索
     *
     * @return 来源事项行线索
     */
	public Long getSourcelineclue() {
		return get("sourcelineclue");
	}

    /**
     * 设置来源事项行线索
     *
     * @param sourcelineclue 来源事项行线索
     */
	public void setSourcelineclue(Long sourcelineclue) {
		set("sourcelineclue", sourcelineclue);
	}

    /**
     * 获取汇率
     *
     * @return 汇率
     */
	public java.math.BigDecimal getExchangerate() {
		return getBigDecimal("exchangerate");
	}

    /**
     * 设置汇率
     *
     * @param exchangerate 汇率
     */
	public void setExchangerate(java.math.BigDecimal exchangerate) {
		set("exchangerate", exchangerate);
	}

    /**
     * 获取汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getExchangerateOps() {
        return getShort("exchangerateOps");
    }

    /**
     * 设置汇率折算方式
     *
     * @param exchangerateOps 汇率折算方式
     */
    public void setExchangerateOps(Short exchangerateOps) {
        set("exchangerateOps", exchangerateOps);
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
     * 获取原币余额
     *
     * @return 原币余额
     */
	public java.math.BigDecimal getOribalance() {
		return getBigDecimal("oribalance");
	}

    /**
     * 设置原币余额
     *
     * @param oribalance 原币余额
     */
	public void setOribalance(java.math.BigDecimal oribalance) {
		set("oribalance", oribalance);
	}

    /**
     * 获取本币余额
     *
     * @return 本币余额
     */
	public java.math.BigDecimal getNatbalance() {
		return getBigDecimal("natbalance");
	}

    /**
     * 设置本币余额
     *
     * @param natbalance 本币余额
     */
	public void setNatbalance(java.math.BigDecimal natbalance) {
		set("natbalance", natbalance);
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
     * 获取票据类型
     *
     * @return 票据类型
     */
	public String getBankbilltype() {
		return get("bankbilltype");
	}

    /**
     * 设置票据类型
     *
     * @param bankbilltype 票据类型
     */
	public void setBankbilltype(String bankbilltype) {
		set("bankbilltype", bankbilltype);
	}

    /**
     * 获取票据日期
     *
     * @return 票据日期
     */
	public java.util.Date getBankbilldate() {
		return get("bankbilldate");
	}

    /**
     * 设置票据日期
     *
     * @param bankbilldate 票据日期
     */
	public void setBankbilldate(java.util.Date bankbilldate) {
		set("bankbilldate", bankbilldate);
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
     * 获取客户银行账户
     *
     * @return 客户银行账户.ID
     */
	public Long getCustomerbankaccount() {
		return get("customerbankaccount");
	}

    /**
     * 设置客户银行账户
     *
     * @param customerbankaccount 客户银行账户.ID
     */
	public void setCustomerbankaccount(Long customerbankaccount) {
		set("customerbankaccount", customerbankaccount);
	}

    /**
     * 获取客户银行名称
     *
     * @return 客户银行名称
     */
	public String getCustomerbankname() {
		return get("customerbankname");
	}

    /**
     * 设置客户银行名称
     *
     * @param customerbankname 客户银行名称
     */
	public void setCustomerbankname(String customerbankname) {
		set("customerbankname", customerbankname);
	}

    /**
     * 获取供应商银行账户
     *
     * @return 供应商银行账户.ID
     */
	public Long getSupplierbankaccount() {
		return get("supplierbankaccount");
	}

    /**
     * 设置供应商银行账户
     *
     * @param supplierbankaccount 供应商银行账户.ID
     */
	public void setSupplierbankaccount(Long supplierbankaccount) {
		set("supplierbankaccount", supplierbankaccount);
	}

    /**
     * 获取供应商银行名称
     *
     * @return 供应商银行名称
     */
	public String getSupplierbankname() {
		return get("supplierbankname");
	}

    /**
     * 设置供应商银行名称
     *
     * @param supplierbankname 供应商银行名称
     */
	public void setSupplierbankname(String supplierbankname) {
		set("supplierbankname", supplierbankname);
	}

    /**
     * 获取收付款对象
     *
     * @return 收付款对象
     */
	public CaObject getCaobject() {
		Number v = get("caobject");
		return CaObject.find(v);
	}

    /**
     * 设置收付款对象
     *
     * @param caobject 收付款对象
     */
	public void setCaobject(CaObject caobject) {
		if (caobject != null) {
			set("caobject", caobject.getValue());
		} else {
			set("caobject", null);
		}
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
     * 获取员工
     *
     * @return 员工.ID
     */
	public String getEmployee() {
		return get("employee");
	}

    /**
     * 设置员工
     *
     * @param employee 员工.ID
     */
	public void setEmployee(String employee) {
		set("employee", employee);
	}

    /**
     * 获取员工银行账号
     *
     * @return 员工银行账号.ID
     */
	public String getEmployeeaccount() {
		return get("employeeaccount");
	}

    /**
     * 设置员工银行账号
     *
     * @param employeeaccount 员工银行账号.ID
     */
	public void setEmployeeaccount(String employeeaccount) {
		set("employeeaccount", employeeaccount);
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
     * 获取费用项目
     *
     * @return 费用项目.ID
     */
	public Long getCostproject() {
		return get("costproject");
	}

    /**
     * 设置费用项目
     *
     * @param costproject 费用项目.ID
     */
	public void setCostproject(Long costproject) {
		set("costproject", costproject);
	}

    /**
     * 获取是否已勾对
     *
     * @return 是否已勾对
     */
	public Boolean getCheckflag() {
	    return getBoolean("checkflag");
	}

    /**
     * 设置是否已勾对
     *
     * @param checkflag 是否已勾对
     */
	public void setCheckflag(Boolean checkflag) {
		set("checkflag", checkflag);
	}

	/**
	 * 获取是否已封存
	 *
	 * @return 是否已封存
	 */
	public Boolean getSealflag() {
		return getBoolean("sealflag");
	}

	/**
	 * 设置是否已封存
	 *
	 * @param sealflag 是否已封存
	 */
	public void setSealflag(Boolean sealflag) {
		set("sealflag", sealflag);
	}

	/**
	 * 获取对账方案
	 *
	 * @return 对账方案.ID
	 */
	public Long getBankreconciliationscheme() {
		return get("bankreconciliationscheme");
	}

	/**
	 * 设置对账方案
	 *
	 * @param bankreconciliationscheme 对账方案.ID
	 */
	public void setBankreconciliationscheme(Long bankreconciliationscheme) {
		set("bankreconciliationscheme", bankreconciliationscheme);
	}
    /**
     * 获取是否内部勾对
     *
     * @return 是否内部勾对
     */
	public Boolean getInsidecheckflag() {
	    return getBoolean("insidecheckflag");
	}

    /**
     * 设置是否内部勾对
     *
     * @param insidecheckflag 是否内部勾对
     */
	public void setInsidecheckflag(Boolean insidecheckflag) {
		set("insidecheckflag", insidecheckflag);
	}

    /**
     * 获取勾对人
     *
     * @return 勾对人.ID
     */
	public Long getCheckman() {
		return get("checkman");
	}

    /**
     * 设置勾对人
     *
     * @param checkman 勾对人.ID
     */
	public void setCheckman(Long checkman) {
		set("checkman", checkman);
	}

    /**
     * 获取勾对日期
     *
     * @return 勾对日期
     */
	public java.util.Date getCheckdate() {
		return get("checkdate");
	}

    /**
     * 设置勾对日期
     *
     * @param checkdate 勾对日期
     */
	public void setCheckdate(java.util.Date checkdate) {
		set("checkdate", checkdate);
	}

    /**
     * 获取勾对号
     *
     * @return 勾对号
     */
	public String getCheckno() {
		return get("checkno");
	}

    /**
     * 设置勾对号
     *
     * @param checkno 勾对号
     */
	public void setCheckno(String checkno) {
		set("checkno", checkno);
	}

	/**
	 * 获取对账方案id
	 *
	 * @return 对账方案id
	 */
	public String getBankreconciliationsettingid() {
		return get("bankreconciliationsettingid");
	}

	/**
	 * 设置对账方案id
	 *
	 * @param bankreconciliationsettingid 对账方案id
	 */
	public void setBankreconciliationsettingid(String bankreconciliationsettingid) {
		set("bankreconciliationsettingid", bankreconciliationsettingid);
	}

    /**
     * 获取网银支付状态
     *
     * @return 网银支付状态
     */
	public PaymentStatus getPaymentstatus() {
		Number v = get("paymentstatus");
		return PaymentStatus.find(v);
	}

    /**
     * 设置网银支付状态
     *
     * @param paymentstatus 网银支付状态
     */
	public void setPaymentstatus(PaymentStatus paymentstatus) {
		if (paymentstatus != null) {
			set("paymentstatus", paymentstatus.getValue());
		} else {
			set("paymentstatus", null);
		}
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
     * 获取是否退款
     *
     * @return 是否退款
     */
	public Boolean getRefund() {
	    return getBoolean("refund");
	}

    /**
     * 设置是否退款
     *
     * @param refund 是否退款
     */
	public void setRefund(Boolean refund) {
		set("refund", refund);
	}

    /**
     * 获取记账人
     *
     * @return 记账人.ID
     */
	public Long getBookkeeper() {
		return get("bookkeeper");
	}

    /**
     * 设置记账人
     *
     * @param bookkeeper 记账人.ID
     */
	public void setBookkeeper(Long bookkeeper) {
		set("bookkeeper", bookkeeper);
	}

    /**
     * 获取审计信息
     *
     * @return 审计信息
     */
	public String getAuditinformation() {
		return get("auditinformation");
	}

    /**
     * 设置审计信息
     *
     * @param auditinformation 审计信息
     */
	public void setAuditinformation(String auditinformation) {
		set("auditinformation", auditinformation);
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
     * 获取来源单据行号
     *
     * @return 来源单据行号(结算用于存放子表的id, 其他模块儿暂时没用到这个字段)
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
     * 获取组织
     *
     * @return 组织.ID
     */
	public String getOrg() {
		return get("org");
	}

    /**
     * 设置组织
     *
     * @param org 组织.ID
     */
	public void setOrg(String org) {
		set("org", org);
	}

    /**
     * 获取是否期初
     *
     * @return 是否期初
     */
	public Boolean getInitflag() {
	    return getBoolean("initflag");
	}

    /**
     * 设置是否期初
     *
     * @param initflag 是否期初
     */
	public void setInitflag(Boolean initflag) {
		set("initflag", initflag);
	}

    /**
     * 获取对账
     *
     * @return 对账
     */
	public String getReconciliation() {
		return get("reconciliation");
	}

    /**
     * 设置对账
     *
     * @param reconciliation 对账
     */
	public void setReconciliation(String reconciliation) {
		set("reconciliation", reconciliation);
	}

    /**
     * 获取单据编号
     *
     * @return 单据编号
     */
	public String getBillnum() {
		return get("billnum");
	}

    /**
     * 设置单据编号
     *
     * @param billnum 单据编号
     */
	public void setBillnum(String billnum) {
		set("billnum", billnum);
	}

    /**
     * 获取方向
     *
     * @return 方向
     */
	public Direction getDirection() {
		Number v = get("direction");
		return Direction.find(v);
	}

    /**
     * 设置方向
     *
     * @param direction 方向
     */
	public void setDirection(Direction direction) {
		if (direction != null) {
			set("direction", direction.getValue());
		} else {
			set("direction", null);
		}
	}

    /**
     * 获取对方名称
     *
     * @return 对方名称
     */
	public String getOthername() {
		return get("othername");
	}

    /**
     * 设置对方名称
     *
     * @param othername 对方名称
     */
	public void setOthername(String othername) {
		set("othername", othername);
	}

    /**
     * 获取银行返回信息
     *
     * @return 银行返回信息
     */
	public String getBankreturnmsg() {
		return get("bankreturnmsg");
	}

    /**
     * 设置银行返回信息
     *
     * @param bankreturnmsg 银行返回信息
     */
	public void setBankreturnmsg(String bankreturnmsg) {
		set("bankreturnmsg", bankreturnmsg);
	}

    /**
     * 获取数字签名
     *
     * @return 数字签名
     */
	public String getDigitalsigned() {
		return get("digitalsigned");
	}

    /**
     * 设置数字签名
     *
     * @param digitalsigned 数字签名
     */
	public void setDigitalsigned(String digitalsigned) {
		set("digitalsigned", digitalsigned);
	}

    /**
     * 获取支付人
     *
     * @return 支付人.ID
     */
	public Long getPayman() {
		return get("payman");
	}

    /**
     * 设置支付人
     *
     * @param payman 支付人.ID
     */
	public void setPayman(Long payman) {
		set("payman", payman);
	}

    /**
     * 获取支付日期
     *
     * @return 支付日期
     */
	public java.util.Date getPaydate() {
		return get("paydate");
	}

    /**
     * 设置支付日期
     *
     * @param paydate 支付日期
     */
	public void setPaydate(java.util.Date paydate) {
		set("paydate", paydate);
	}

    /**
     * 获取支付状态
     *
     * @return 支付状态
     */
	public PayStatus getPaystatus() {
		Number v = get("paystatus");
		return PayStatus.find(v);
	}

    /**
     * 设置支付状态
     *
     * @param paystatus 支付状态
     */
	public void setPaystatus(PayStatus paystatus) {
		if (paystatus != null) {
			set("paystatus", paystatus.getValue());
		} else {
			set("paystatus", null);
		}
	}

    /**
     * 获取支付方式
     *
     * @return 支付方式
     */
	public Payway getPayway() {
		Number v = get("payway");
		return Payway.find(v);
	}

    /**
     * 设置支付方式
     *
     * @param payway 支付方式
     */
	public void setPayway(Payway payway) {
		if (payway != null) {
			set("payway", payway.getValue());
		} else {
			set("payway", null);
		}
	}

    /**
     * 获取支付信息
     *
     * @return 支付信息
     */
	public String getPaymessage() {
		return get("paymessage");
	}

    /**
     * 设置支付信息
     *
     * @param paymessage 支付信息
     */
	public void setPaymessage(String paymessage) {
		set("paymessage", paymessage);
	}

    /**
     * 获取预下单编码
     *
     * @return 预下单编码
     */
	public String getPorderid() {
		return get("porderid");
	}

    /**
     * 设置预下单编码
     *
     * @param porderid 预下单编码
     */
	public void setPorderid(String porderid) {
		set("porderid", porderid);
	}

    /**
     * 获取批量支付编号
     *
     * @return 批量支付编号
     */
	public String getBatno() {
		return get("batno");
	}

    /**
     * 设置批量支付编号
     *
     * @param batno 批量支付编号
     */
	public void setBatno(String batno) {
		set("batno", batno);
	}

    /**
     * 获取交易流水号
     *
     * @return 交易流水号
     */
	public String getTranseqno() {
		return get("transeqno");
	}

    /**
     * 设置交易流水号
     *
     * @param transeqno 交易流水号
     */
	public void setTranseqno(String transeqno) {
		set("transeqno", transeqno);
	}

    /**
     * 获取结算状态
     *
     * @return 结算状态
     */
	public SettleStatus getSettlestatus() {
		Number v = get("settlestatus");
		return SettleStatus.find(v);
	}

    /**
     * 设置结算状态
     *
     * @param settlestatus 结算状态
     */
	public void setSettlestatus(SettleStatus settlestatus) {
		if (settlestatus != null) {
			set("settlestatus", settlestatus.getValue());
		} else {
			set("settlestatus", null);
		}
	}

    /**
     * 获取日记账类型
     *
     * @return 日记账类型
     */
	public JournalType getJournaltype() {
		Number v = get("journaltype");
		return JournalType.find(v);
	}

    /**
     * 设置日记账类型
     *
     * @param journaltype 日记账类型
     */
	public void setJournaltype(JournalType journaltype) {
		if (journaltype != null) {
			set("journaltype", journaltype.getValue());
		} else {
			set("journaltype", null);
		}
	}

    /**
     * 获取收付款类型
     *
     * @return 收付款类型
     */
	public RpType getRptype() {
		Number v = get("rptype");
		return RpType.find(v);
	}

    /**
     * 设置收付款类型
     *
     * @param rptype 收付款类型
     */
	public void setRptype(RpType rptype) {
		if (rptype != null) {
			set("rptype", rptype.getValue());
		} else {
			set("rptype", null);
		}
	}

    /**
     * 获取审批状态
     *
     * @return 审批状态
     */
	public AuditStatus getAuditstatus() {
		Number v = get("auditstatus");
		return AuditStatus.find(v);
	}

    /**
     * 设置审批状态
     *
     * @param auditstatus 审批状态
     */
	public void setAuditstatus(AuditStatus auditstatus) {
		if (auditstatus != null) {
			set("auditstatus", auditstatus.getValue());
		} else {
			set("auditstatus", null);
		}
	}


	/**
	 * 获取来源单据billnum
	 *
	 * @return 来源单据billnum
	 */
	public String getBillno() {
		return get("billno");
	}

	/**
	 * 设置来源单据billnum
	 *
	 * @param billno 来源单据billnum
	 */
	public void setBillno(String billno) {
		set("billno", billno);
	}


	/**
	 * 获取来源单据服务编码
	 *
	 * @return 来源单据服务编码
	 */
	public String getServicecode() {
		return get("servicecode");
	}

	/**
	 * 设置来源单据服务编码
	 *
	 * @param servicecode 来源单据服务编码
	 */
	public void setServicecode(String servicecode) {
		set("servicecode", servicecode);
	}

	/**
	 * 获取来源单据跳转url
	 *
	 * @return 来源来源单据跳转url
	 */
	public String getTargeturl() {
		return get("targeturl");
	}

	/**
	 * 设置来源单据跳转url
	 *
	 * @param targeturl 来源单据跳转url
	 */
	public void setTargeturl(String targeturl) {
		set("targeturl", targeturl);
	}


	/**
     * 获取自定义项1
     *
     * @return 自定义项1
     */
	public String getDefine1() {
		return get("define1");
	}

    /**
     * 设置自定义项1
     *
     * @param define1 自定义项1
     */
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
     * 获取自定义项11
     *
     * @return 自定义项11
     */
	public String getDefine11() {
		return get("define11");
	}

    /**
     * 设置自定义项11
     *
     * @param define11 自定义项11
     */
	public void setDefine11(String define11) {
		set("define11", define11);
	}

    /**
     * 获取自定义项12
     *
     * @return 自定义项12
     */
	public String getDefine12() {
		return get("define12");
	}

    /**
     * 设置自定义项12
     *
     * @param define12 自定义项12
     */
	public void setDefine12(String define12) {
		set("define12", define12);
	}

    /**
     * 获取自定义项13
     *
     * @return 自定义项13
     */
	public String getDefine13() {
		return get("define13");
	}

    /**
     * 设置自定义项13
     *
     * @param define13 自定义项13
     */
	public void setDefine13(String define13) {
		set("define13", define13);
	}

    /**
     * 获取自定义项14
     *
     * @return 自定义项14
     */
	public String getDefine14() {
		return get("define14");
	}

    /**
     * 设置自定义项14
     *
     * @param define14 自定义项14
     */
	public void setDefine14(String define14) {
		set("define14", define14);
	}

    /**
     * 获取自定义项15
     *
     * @return 自定义项15
     */
	public String getDefine15() {
		return get("define15");
	}

    /**
     * 设置自定义项15
     *
     * @param define15 自定义项15
     */
	public void setDefine15(String define15) {
		set("define15", define15);
	}

    /**
     * 获取自定义项16
     *
     * @return 自定义项16
     */
	public String getDefine16() {
		return get("define16");
	}

    /**
     * 设置自定义项16
     *
     * @param define16 自定义项16
     */
	public void setDefine16(String define16) {
		set("define16", define16);
	}

    /**
     * 获取自定义项17
     *
     * @return 自定义项17
     */
	public String getDefine17() {
		return get("define17");
	}

    /**
     * 设置自定义项17
     *
     * @param define17 自定义项17
     */
	public void setDefine17(String define17) {
		set("define17", define17);
	}

    /**
     * 获取自定义项18
     *
     * @return 自定义项18
     */
	public String getDefine18() {
		return get("define18");
	}

    /**
     * 设置自定义项18
     *
     * @param define18 自定义项18
     */
	public void setDefine18(String define18) {
		set("define18", define18);
	}

    /**
     * 获取自定义项19
     *
     * @return 自定义项19
     */
	public String getDefine19() {
		return get("define19");
	}

    /**
     * 设置自定义项19
     *
     * @param define19 自定义项19
     */
	public void setDefine19(String define19) {
		set("define19", define19);
	}

    /**
     * 获取自定义项20
     *
     * @return 自定义项20
     */
	public String getDefine20() {
		return get("define20");
	}

    /**
     * 设置自定义项20
     *
     * @param define20 自定义项20
     */
	public void setDefine20(String define20) {
		set("define20", define20);
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
	 * 获取统计筛选
	 *
	 * @return 统计筛选
	 */
	public MoneyTypeNum getMoneytype() {
		Number v = get("moneytype");
		return MoneyTypeNum.find(v);
	}

	/**
	 * 设置统计筛选
	 *
	 * @param moneytype 统计筛选
	 */
	public void setMoneytype(MoneyTypeNum moneytype) {
		if (moneytype != null) {
			set("moneytype", moneytype.getValue());
		} else {
			set("moneytype", null);
		}
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
	 * 获取源头单据id
	 *
	 * @return 源头单据id
	 */
	public String getTopsrcbillid() {
		return get("topsrcbillid");
	}

	/**
	 * 设置源头单据id
	 *
	 * @param topsrcbillid 源头单据id
	 */
	public void setTopsrcbillid(String topsrcbillid) {
		set("topsrcbillid", topsrcbillid);
	}

	/**
	 * 获取源头事项来源
	 *
	 * @return 源头事项来源
	 */
	public Short getTopsrcitem() {
		Number v = get("topsrcitem");
		if (v == null) {
			return null;
		}
		return v.shortValue();
	}

	/**
	 * 设置源头事项来源
	 *
	 * @param topsrcitem 源头事项来源
	 */
	public void setTopsrcitem(EventSource topsrcitem) {
		if (topsrcitem != null) {
			set("topsrcitem", topsrcitem.getValue());
		} else {
			set("topsrcitem", null);
		}
	}

	/**
	 * 设置源头事项来源
	 *
	 * @param topsrcitem 源头事项来源
	 */
	public void setTopsrcitem(Short topsrcitem) {
		set("topsrcitem", topsrcitem);
	}

	/**
	 * 获取源头事项类型
	 *
	 * @return 源头事项类型
	 */
	public Short getTopbilltype() {
		Number v = get("topbilltype");
		if (v == null) {
			return null;
		}
		return v.shortValue();
	}

	/**
	 * 设置源头事项类型
	 *
	 * @param topbilltype 源头事项类型
	 */
	public void setTopbilltype(EventType topbilltype) {
		if (topbilltype != null) {
			set("topbilltype", topbilltype.getValue());
		} else {
			set("topbilltype", null);
		}
	}

	/**
	 * 设置源头事项类型
	 *
	 * @param topbilltype 源头事项类型
	 */
	public void setTopbilltype(Short topbilltype) {
		set("topbilltype", topbilltype);
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
	 * 获取银行对账编号
	 *
	 * @return 银行对账编号
	 */
	public String getBankcheckno() {
		return get("bankcheckno");
	}

	/**
	 * 设置银行对账编号
	 *
	 * @param bankcheckno 银行对账编号
	 */
	public void setBankcheckno(String bankcheckno) {
		set("bankcheckno", bankcheckno);
	}

	/**
	 * 获取资金业务对象
	 *
	 * @return 资金业务对象.ID
	 */
	public String getCapBizObj() {
		return get("capBizObj");
	}

	/**
	 * 设置资金业务对象
	 *
	 * @param capBizObj 资金业务对象.ID
	 */
	public void setCapBizObj(String capBizObj) {
		set("capBizObj", capBizObj);
	}

	/**
	 * 获取资金业务对象银行账户
	 *
	 * @return 资金业务对象银行账户.ID
	 */
	public String getCapBizObjbankaccount() {
		return get("capBizObjbankaccount");
	}

	/**
	 * 设置资金业务对象银行账户
	 *
	 * @param capBizObjbankaccount 资金业务对象银行账户.ID
	 */
	public void setCapBizObjbankaccount(String capBizObjbankaccount) {
		set("capBizObjbankaccount", capBizObjbankaccount);
	}

	/**
	 * 获取对方银行账户
	 *
	 * @return 对方银行账户.ID
	 */
	public String getOtherbankaccount() {
		return get("otherbankaccount");
	}

	/**
	 * 设置对方银行账户
	 *
	 * @param otherbankaccount 对方银行账户.ID
	 */
	public void setOtherbankaccount(String otherbankaccount) {
		set("otherbankaccount", otherbankaccount);
	}

	/**
	 * 获取对方现金账户
	 *
	 * @return 对方现金账户.ID
	 */
	public String getOthercashaccount() {
		return get("othercashaccount");
	}

	/**
	 * 设置对方现金账户
	 *
	 * @param othercashaccount 对方现金账户.ID
	 */
	public void setOthercashaccount(String othercashaccount) {
		set("othercashaccount", othercashaccount);
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
     * 获取项目编码
     *
     * @return 项目编码
     */
    public String getProjectCode() {
        return get("projectCode");
    }

    /**
     * 设置凭项目编码
     *
     * @param projectCode 项目编码
     */
    public void setProjectCode(String projectCode) {
        set("projectCode", projectCode);
    }


    /**
     * 获取对方名称
     *
     * @return 对方名称
     */
    public String getOthertitle() {
        return get("othertitle");
    }

    /**
     * 设置对方名称
     *
     * @param othertitle 对方名称
     */
    public void setOthertitle(String othertitle) {
        set("othertitle", othertitle);
    }

    /**
     * 获取内部单位
     *
     * @return 内部单位
     */
    public String getInnerunit() {
        return get("innerunit");
    }

    /**
     * 设置对方名称
     *
     * @param innerunit 内部单位
     */
    public void setInnerunit(String innerunit) {
        set("innerunit", innerunit);
    }

    /**
     * 内部单位银行账户
     *
     * @return 内部单位银行账户
     */
    public String getInnerunitbankaccount() {
        return get("innerunitbankaccount");
    }

    /**
     * 设置内部单位银行账户
     *
     * @param innerunitbankaccount 内部单位银行账户
     */
    public void setInnerunitbankaccount(String innerunitbankaccount) {
        set("innerunitbankaccount", innerunitbankaccount);
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
	 * 获取WBS
	 *
	 * @return WBS.ID
	 */
	public String getWbs() {
		return get("wbs");
	}

	/**
	 * 设置WBS
	 *
	 * @param wbs WBS.ID
	 */
	public void setWbs(String wbs) {
		set("wbs", wbs);
	}

	/**
	 * 获取活动
	 *
	 * @return 活动.ID
	 */
	public Long getActivity() {
		return get("activity");
	}

	/**
	 * 设置活动
	 *
	 * @param activity 活动.ID
	 */
	public void setActivity(Long activity) {
		set("activity", activity);
	}

	/**
	 * 获取其他对方开户行名
	 *
	 * @return 其他对方开户行名
	 */
	public String getOtheropenbankname() {
		return get("otheropenbankname");
	}

	/**
	 * 设置其他对方开户行名
	 *
	 * @param otheropenbankname 其他对方开户行名
	 */
	public void setOtheropenbankname(String otheropenbankname) {
		set("otheropenbankname", otheropenbankname);
	}

	/**
	 * 获取其他对方银行账户名称
	 *
	 * @return 其他对方银行账户名称
	 */
	public String getOtherbankaccountname() {
		return get("otherbankaccountname");
	}

	/**
	 * 设置其他对方银行账户名称
	 *
	 * @param otherbankaccountname 其他对方银行账户名称
	 */
	public void setOtherbankaccountname(String otherbankaccountname) {
		set("otherbankaccountname", otherbankaccountname);
	}

	/**
	 * 获取其他对方银行账号
	 *
	 * @return 其他对方银行账号
	 */
	public String getOtherbankaccountno() {
		return get("otherbankaccountno");
	}

	/**
	 * 设置其他对方银行账号
	 *
	 * @param otherbankaccountno 其他对方银行账号
	 */
	public void setOtherbankaccountno(String otherbankaccountno) {
		set("otherbankaccountno", otherbankaccountno);
	}

	/**
	 * 获取来源交易类型
	 *
	 * @return 来源交易类型.ID
	 */
	public String getSourcebusitype() {
		return get("sourcebusitype");
	}

	/**
	 * 设置来源交易类型
	 *
	 * @param sourcebusitype 来源交易类型.ID
	 */
	public void setSourcebusitype(String sourcebusitype) {
		set("sourcebusitype", sourcebusitype);
	}

	/**
	 * 获取来源单据明细id
	 *
	 * @return 来源单据明细id
	 */
	public String getSourcedetailid() {
		return get("sourcedetailid");
	}

	/**
	 * 设置来源单据明细id
	 *
	 * @param sourcedetailid 来源单据明细id
	 */
	public void setSourcedetailid(String sourcedetailid) {
		set("sourcedetailid", sourcedetailid);
	}

	/**
	 * 获取来源单据明细编号
	 *
	 * @return 来源单据明细编号
	 */
	public String getSourcedetailcode() {
		return get("sourcedetailcode");
	}

	/**
	 * 设置来源单据明细编号
	 *
	 * @param sourcedetailcode 来源单据明细编号
	 */
	public void setSourcedetailcode(String sourcedetailcode) {
		set("sourcedetailcode", sourcedetailcode);
	}

	/**
	 * 获取来源单据创建日期
	 *
	 * @return 来源单据创建日期
	 */
	public java.util.Date getSourcecreatedate() {
		return get("sourcecreatedate");
	}

	/**
	 * 设置来源单据创建日期
	 *
	 * @param sourcecreatedate 来源单据创建日期
	 */
	public void setSourcecreatedate(java.util.Date sourcecreatedate) {
		set("sourcecreatedate", sourcecreatedate);
	}

	/**
	 * 获取来源单据创建时间
	 *
	 * @return 来源单据创建时间
	 */
	public java.util.Date getSourcecreatetime() {
		return get("sourcecreatetime");
	}

	/**
	 * 设置来源单据创建时间
	 *
	 * @param sourcecreatetime 来源单据创建时间
	 */
	public void setSourcecreatetime(java.util.Date sourcecreatetime) {
		set("sourcecreatetime", sourcecreatetime);
	}

	/**
	 * 获取来源单据审批日期
	 *
	 * @return 来源单据审批日期
	 */
	public java.util.Date getSourceapprovedate() {
		return get("sourceapprovedate");
	}

	/**
	 * 设置来源单据审批日期
	 *
	 * @param sourceapprovedate 来源单据审批日期
	 */
	public void setSourceapprovedate(java.util.Date sourceapprovedate) {
		set("sourceapprovedate", sourceapprovedate);
	}

	/**
	 * 获取来源单据审批时间
	 *
	 * @return 来源单据审批时间
	 */
	public java.util.Date getSourceapprovetime() {
		return get("sourceapprovetime");
	}

	/**
	 * 设置来源单据审批时间
	 *
	 * @param sourceapprovetime 来源单据审批时间
	 */
	public void setSourceapprovetime(java.util.Date sourceapprovetime) {
		set("sourceapprovetime", sourceapprovetime);
	}

	/**
	 * 获取来源单据日期
	 *
	 * @return 来源单据日期
	 */
	public java.util.Date getSourcevouchdate() {
		return get("sourcevouchdate");
	}

	/**
	 * 设置来源单据日期
	 *
	 * @param sourcevouchdate 来源单据日期
	 */
	public void setSourcevouchdate(java.util.Date sourcevouchdate) {
		set("sourcevouchdate", sourcevouchdate);
	}

	/**
	 * 获取单据创建日期
	 *
	 * @return 单据创建日期
	 */
	public java.util.Date getVouchcreatedate() {
		return get("vouchcreatedate");
	}

	/**
	 * 设置单据创建日期
	 *
	 * @param vouchcreatedate 单据创建日期
	 */
	public void setVouchcreatedate(java.util.Date vouchcreatedate) {
		set("vouchcreatedate", vouchcreatedate);
	}

	/**
	 * 获取单据创建时间
	 *
	 * @return 单据创建时间
	 */
	public java.util.Date getVouchcreatetime() {
		return get("vouchcreatetime");
	}

	/**
	 * 设置单据创建时间
	 *
	 * @param vouchcreatetime 单据创建时间
	 */
	public void setVouchcreatetime(java.util.Date vouchcreatetime) {
		set("vouchcreatetime", vouchcreatetime);
	}

	/**
	 * 获取单据审批日期
	 *
	 * @return 单据审批日期
	 */
	public java.util.Date getVouchapprovedate() {
		return get("vouchapprovedate");
	}

	/**
	 * 设置单据审批日期
	 *
	 * @param vouchapprovedate 单据审批日期
	 */
	public void setVouchapprovedate(java.util.Date vouchapprovedate) {
		set("vouchapprovedate", vouchapprovedate);
	}

	/**
	 * 获取单据审批时间
	 *
	 * @return 单据审批时间
	 */
	public java.util.Date getVouchapprovetime() {
		return get("vouchapprovetime");
	}

	/**
	 * 设置单据审批时间
	 *
	 * @param vouchapprovetime 单据审批时间
	 */
	public void setVouchapprovetime(java.util.Date vouchapprovetime) {
		set("vouchapprovetime", vouchapprovetime);
	}

	/**
	 * 获取登账时间
	 *
	 * @return 登账时间
	 */
	public java.util.Date getDztime() {
		// 兼容String类型
		if (get("dztime") instanceof String) {
			return DateUtils.parseDate(get("dztime"),"yyyy-MM-dd HH:mm:ss");
		}
		return get("dztime");
	}

	/**
	 * 设置登账时间
	 *
	 * @param dztime 登账时间
	 */
	public void setDztime(java.util.Date dztime) {
		set("dztime", dztime);
	}

	/**
	 * 获取日记账勾对时间
	 *
	 * @return 日记账勾对时间
	 */
	public java.util.Date getChecktime() {
		return get("checktime");
	}

	/**
	 * 设置日记账勾对时间
	 *
	 * @param checktime 日记账勾对时间
	 */
	public void setChecktime(java.util.Date checktime) {
		set("checktime", checktime);
	}

	/**
	 * 获取财凭证唯一标识码
	 *
	 * @return 凭证唯一标识码
	 */
	public String getVoucheronlyno() {
		return get("voucheronlyno");
	}

	/**
	 * 设置凭证唯一标识码
	 *
	 * @param voucheronlyno 凭证唯一标识码
	 */
	public void setVoucheronlyno(String voucheronlyno) {
		set("voucheronlyno", voucheronlyno);
	}

}
