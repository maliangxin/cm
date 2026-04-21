package com.yonyoucloud.fi.cmp.vo.migrade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CmpMigraForSystemCheckResult  implements Serializable {

    private String domain;//预检项领域编码
    private String domainName;//预检项领域名称
    private String checkItem;//预检项说明
    private Boolean strictValidation;//是否强校验（true/false）
    private String status;//通过pass，异常fail，提示性异常check
    private String message;//异常的提示信息，针对异常数据的原因描述
    private String suggestion;//针对 fail 或者 check 类型数据的操作修复建议说明",
    private int errorCount;//1061,错误数据的总数量
    private String reportUrl;//所有错误数据明细文件，领域自己的报告地址
    /**
     * 具体错误数据详情：可空，默认只返回50条（如果没有，就直接根据检查项的提示信息处理），
     * 全量的错误数据领域存放在文件中，返回文件 url：reportUrl
     */
    private List<CmpPreCheckDetailVO> data;
}
