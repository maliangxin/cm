package com.yonyoucloud.fi.cmp.flowhandlesetting.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author guoxh
 */
@Data
public class FlowHandleSettingVO implements Serializable {
    /**
     * 关联单据范围 1 特定 全部 处理方式=手工时有值
     */
    private Integer associationBillRange;
    /**
     * 自动关联时是否人工确认 1 全部需要确认，2 全部无需确认 处理方式=自动时有值
     */
    private Integer isArtiConfirm;
    /**
     * 关联多条是否自动确认 1 是  2 否 处理方式 =自动时有值
     */
    private Integer isRandomAutoConfirm;
    /**
     * 是否完结流程 收付单据关联，收付单据生单时返回
     */
    private Integer isTerminationFlow;
    /**
     * 存储财资中间表 1是 2否
     */
    private Integer storeMidTable;
    /**
     * 通知业务对象 1是 2否
     */
    private Integer notifyBizObject;
    private List<FlowHandleSettingSub> list;

}
