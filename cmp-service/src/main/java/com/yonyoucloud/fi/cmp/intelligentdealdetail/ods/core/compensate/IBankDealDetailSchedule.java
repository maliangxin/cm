//package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate;
//
//@Deprecated
//public interface IBankDealDetailSchedule{
//    /**
//     * 补偿任务先将正在处理、处理失败流水状态改为初始态<p>
//     *  1)ods表，辨识匹配失败，辨识匹配中超过30分钟的<p>
//     *  2)流水业务表，流水辨识处理失败、开始辨识且超过30分钟的<p>
//     *  3)流水业务表，业务关联、业务凭据、生单、发布认领中心等处理中且超过30分钟的、处理失败的<p>
//     * */
//    void updateProcessStatusToInit();
//    /**
//     * 开始补偿，读取初始态流水，重新执行流水处理<p>
//     * 1）ods补偿<p>
//     * 2）流水表辨识补偿<p>
//     * 3）流水表流程补偿<p>
//     * */
//    void executeDealDetailHandler();
//}