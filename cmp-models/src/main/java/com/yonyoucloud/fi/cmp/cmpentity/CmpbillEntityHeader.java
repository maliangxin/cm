package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;

/**
 * 收付单主表项实体
 *
 * @author u
 * @version 1.0
 */
public class CmpbillEntityHeader extends Vouch implements IApprovalInfo {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.cmpbillentity.CmpbillEntityHeader";

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
     * 获取审批人名称
     *
     * @return 审批人名称
     */
	public String getAuditor() {
		return get("auditor");
	}

    /**
     * 设置审批人名称
     *
     * @param auditor 审批人名称
     */
	public void setAuditor(String auditor) {
		set("auditor", auditor);
	}

    /**
     * 获取审批时间
     *
     * @return 审批时间
     */
	public java.util.Date getAuditTime() {
		return get("auditTime");
	}

    /**
     * 设置审批时间
     *
     * @param auditTime 审批时间
     */
	public void setAuditTime(java.util.Date auditTime) {
		set("auditTime", auditTime);
	}

    /**
     * 获取审批日期
     *
     * @return 审批日期
     */
	public java.util.Date getAuditDate() {
		return get("auditDate");
	}

    /**
     * 设置审批日期
     *
     * @param auditDate 审批日期
     */
	public void setAuditDate(java.util.Date auditDate) {
		set("auditDate", auditDate);
	}

}
