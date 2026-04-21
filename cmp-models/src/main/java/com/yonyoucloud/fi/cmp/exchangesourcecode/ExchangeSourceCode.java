package com.yonyoucloud.fi.cmp.exchangesourcecode;

import org.imeta.orm.base.BizObject;

/**
 * 结售汇来源代码实体
 *
 * @author u
 * @version 1.0
 */
public class ExchangeSourceCode extends BizObject {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.exchangesourcecode.ExchangeSourceCode";

    /**
     * 获取编码
     *
     * @return 编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置编码
     *
     * @param code 编码
     */
	public void setCode(String code) {
		set("code", code);
	}

    /**
     * 获取代码
     *
     * @return 代码
     */
	public String getCoding() {
		return get("coding");
	}

    /**
     * 设置代码
     *
     * @param coding 代码
     */
	public void setCoding(String coding) {
		set("coding", coding);
	}

    /**
     * 获取代码名称
     *
     * @return 代码名称
     */
	public String getCodingname() {
		return get("codingname");
	}

    /**
     * 设置代码名称
     *
     * @param codingname 代码名称
     */
	public void setCodingname(String codingname) {
		set("codingname", codingname);
	}

    /**
     * 获取多语类型
     *
     * @return 多语类型
     */
	public Integer getMultilangtype() {
		return get("multilangtype");
	}

    /**
     * 设置多语类型
     *
     * @param multilangtype 多语类型
     */
	public void setMultilangtype(Integer multilangtype) {
		set("multilangtype", multilangtype);
	}

    /**
     * 获取项目归类编码
     *
     * @return 项目归类编码
     */
	public String getProjectclassifycode() {
		return get("projectclassifycode");
	}

    /**
     * 设置项目归类编码
     *
     * @param projectclassifycode 项目归类编码
     */
	public void setProjectclassifycode(String projectclassifycode) {
		set("projectclassifycode", projectclassifycode);
	}

    /**
     * 获取项目归类名称
     *
     * @return 项目归类名称
     */
	public String getProjectclassifyname() {
		return get("projectclassifyname");
	}

    /**
     * 设置项目归类名称
     *
     * @param projectclassifyname 项目归类名称
     */
	public void setProjectclassifyname(String projectclassifyname) {
		set("projectclassifyname", projectclassifyname);
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
