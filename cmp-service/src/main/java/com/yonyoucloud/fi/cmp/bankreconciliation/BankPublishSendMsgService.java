package com.yonyoucloud.fi.cmp.bankreconciliation;

/**
 * @description: 银行流水发布时发送业务消息接口
 * @author: wanxbo@yonyou.com
 * @date: 2024/9/25 10:58
 */

public interface BankPublishSendMsgService {

    /**
     * 发送银行流水发布消息到代办
     * 20240925:第一版需求，银行流水对方类型为客户/供应商时，发送待办消息到客户/供应商负责人
     * @param bankReconciliation 银行流水
     * @throws Exception
     */
    void sendPublishMsgToCreateToDo(BankReconciliation bankReconciliation) throws Exception;

    /**
     * 流水发布待办消息确认接口
     * @param bankReconciliation 银行流水
     * @throws Exception
     */
    void handleConfirmMsg(BankReconciliation bankReconciliation) throws Exception;

    /**
     * 流水发布代办消息删除接口
     * @param bankReconciliation 银行流水
     * @throws Exception
     */
    void handleDeleteMsg(BankReconciliation bankReconciliation) throws Exception;
}
