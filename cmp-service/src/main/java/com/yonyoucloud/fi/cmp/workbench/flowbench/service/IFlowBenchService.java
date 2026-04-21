package com.yonyoucloud.fi.cmp.workbench.flowbench.service;

import com.yonyoucloud.fi.cmp.workbench.flowbench.dto.req.FlowBenchVO;
import com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp.*;

import java.util.List;
import java.util.Map;

/**
 * 流水工作台-服务接口
 * @author guoxh
 */
public interface IFlowBenchService {
    /**
     * 新增/修改视图
     * @param FlowBenchVO
     */
    FlowBenchVO saveView(FlowBenchVO FlowBenchVO) throws Exception;

    /**
     * 查询视图
     */
    List<FlowBenchVO> list();

    boolean existsById(Long id) throws Exception;

    FlowBenchVO queryDefault(String userid) throws Exception;

    FlowBenchVO selectById(Long id) throws Exception;
    /**
     * 删除视图
     */
    void batchDeleteView(List<Long> singletonList) throws Exception;

    /**
     * 待办
     * @param FlowBenchVO
     * @return
     * @throws Exception
     */
    FlowTodoVO queryFlowTodo(FlowBenchVO FlowBenchVO) throws Exception;

    /**
     * 流水处理日历 月视图
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    List<FlowMonthDataVO> queryMonthCalc(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 流水处理日历 日视图
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    FlowDayDataVO queryDayCalc(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 回单统计
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    FlowReceiptVO queryReceipt(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 银行流水top5
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    List<FlowBankStatementVO> queryFlowTop(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 流水接入/导入提醒
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    FlowWarningVO queryFlowWarning(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 余额检查结果 balancecontrast com.yonyoucloud.fi.cmp.cmpentity.BalanceContrast
     * 余额检查功能合并到  queryFlowWarning
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    default FlowBalanceCheckVO balanceCheck(FlowBenchVO flowBenchVO) throws Exception{
        return null;
    }

    /**
     * 流水处理率检查
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    FlowProcessCheckVO processCheck(FlowBenchVO flowBenchVO) throws Exception;


    /**
     * 监控银企直联银行
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    FlowUnionPayMonitorVO queryUnionpayMonitor(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 智能机器人作业监控
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    FlowRpaMonitorVO queryRpaMonitor(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 初始化数据
     */
    void initData() throws Exception;

    /**
     * 流水接入/导入提醒
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    List<Map<String,Object>> queryUnImportAccount(FlowBenchVO flowBenchVO) throws Exception;

    /**
     * 1 余额不符数据 2 余额缺失数据
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    List<Map<String,Object>> queryBalanceListData(FlowBenchVO flowBenchVO,int type) throws Exception;
}
