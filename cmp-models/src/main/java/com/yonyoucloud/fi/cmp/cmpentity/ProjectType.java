package com.yonyoucloud.fi.cmp.cmpentity;

import java.util.HashMap;

/**
 * 项目类别枚举
 *
 * @author u
 * @version 1.0
 */
public enum ProjectType {
	OftenMargin("经常项目", (short)0),
	CapitalMargin("资本与金融项目", (short)1),
	OtherProject("其它", (short)2);

	private String name;
	private short value;

	private ProjectType(String name, short value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public short getValue() {
		return value;
	}

	private static HashMap<Short, ProjectType> map = null;

	private synchronized static void initMap() {
		if (map != null) {
			return;
		}
		map = new HashMap<Short, ProjectType>();
		ProjectType[] items = ProjectType.values();
		for (ProjectType item : items) {
			map.put(item.getValue(), item);
		}
	}

	public static ProjectType find(Number value) {
		if (value == null) {
			return null;
		}
		if (map == null) {
			initMap();
		}
		return map.get(value.shortValue());
	}
}
