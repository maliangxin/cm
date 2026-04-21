package com.yonyoucloud.fi.cmp.vo.migrade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 本类即用于接收平台的请求信息
 * 也作预检为回调时 像平台发送信息的vo
 * 构建结构详见wiki gfwiki.yyrd.com/pages/viewpage.action?pageId=73971742
 */
@Data
public class CmpMigradeForSystemRequest  implements Serializable {

    private String invokeId;//本次预检动作所属的调用id，id 相同表示同一次请求的反复调用：人工重试
    private String tenantId;//升级的租户id
    private int upgradeType;//升级的类型：0-期初升级、1-规格升级
    private String callBackUri;//回调地址回调检查服务，返回检查结果

    private String status;//通过pass，异常fail
    private String message;//升级说明，异常的提示信息",
    private String detail;//详细错误的数据，用于提供给开发定位问题，可以为json格式"

    private List<CmpMigraForSystemCheckResult> checkResult;
}
