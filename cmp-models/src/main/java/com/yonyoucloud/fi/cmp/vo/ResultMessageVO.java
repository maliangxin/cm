package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 返回前端信息通用vo
 * 当返回前端信息没有明确统一标识时 都可用此VO返回
 * 在Controller层转化为map格式返回 用于旧有方法适配
 */
@Data
public class ResultMessageVO<T>{

    private Boolean dealSucceed;

    private String msg;

    private String message;

    private Boolean checkResult;

    private List<T> rows;

    private int count;

    private int sucessCount;

    private int successCount;

    private int failCount;

    private List<String> msgs;

    private List<String> messages;

    private Map<String, String> failed;

    private Date dzdate;

    private List infos;

    private String code;

}
