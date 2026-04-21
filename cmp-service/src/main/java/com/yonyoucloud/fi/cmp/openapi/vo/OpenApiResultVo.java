package com.yonyoucloud.fi.cmp.openapi.vo;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenApiResultVo {

    //状态码
    private int code;

    //返回信息
    private String message;

    //总数量
    private int totalCount;

    //每页数据量
    private int pageSize;

    //数组信息
    private List<CtmJSONObject> resultList;
}
