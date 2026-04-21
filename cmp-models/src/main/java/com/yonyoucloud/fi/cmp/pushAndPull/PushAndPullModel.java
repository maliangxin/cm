package com.yonyoucloud.fi.cmp.pushAndPull;

import lombok.Data;

import java.util.List;

/**
 * 单据转换实体类
 * @author mal
 * @since 2023-07-11
 */
@Data
public class PushAndPullModel {

    String code;//单据转换编码

    String groupCode;//单据转换分组编码

    String oriDefName;//来源单据特征的name

    String tarDefName;//模板单据特征的name

    String matchKey;//单据匹配key

    List<String> mainids;//主表id集合

    List<String> childs;//子表id集合

    boolean needDivide; //是否需要分单

    boolean querydb;//是否回查上游库

    int isMainSelect; //0   根据子表的ID 查找所有数据;1  根据主表的ID 查找所有数据

}
