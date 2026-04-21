package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Date;

/**
 * 补偿接口类
 * */
public interface IBankDealDetailCompensate {

    /**
     * 基于ytenantid构建上下文执行具体补偿
     * */
     void compensateInContext(Date startDate, Date endDate, CtmJSONObject param) throws Exception;
}
