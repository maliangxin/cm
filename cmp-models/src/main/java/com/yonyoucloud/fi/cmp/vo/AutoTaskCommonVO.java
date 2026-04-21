package com.yonyoucloud.fi.cmp.vo;

import com.yonyou.iuap.yms.lock.YmsLock;
import lombok.Data;

/**
 * 定时任务相关参数专用vo
 * 适配统一json转强类型使用
 * 本类字段可能不全 后续对应节点开发替换时 需要做相应补充
 */
@Data
public class AutoTaskCommonVO {

    private String tenantId;

    private String userId;

    private String logId;
    //接口配置参数day，用来判断最大批处理数据量(用于CmpAutoSettleTaskController)
    private int batchNum;
    //定时任务 异步标志
    private Boolean asynchronized;

    private YmsLock ymsLock;

}
