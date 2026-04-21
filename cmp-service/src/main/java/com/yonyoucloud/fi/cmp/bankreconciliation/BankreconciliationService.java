package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItemVO;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.BankReconciliationVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.BankReconciliationbusrelationVo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface BankreconciliationService {
    /**
     * 流水发布处理接口
     * @param id 流水id
     * @param bankSeqNo 流水号
     * @param params publishType：发布类型（枚举类PublishedType）；publishToData：发布到的参照数据
     */
    CtmJSONObject publish(Long id,String bankSeqNo,Map<String, Object> params) throws Exception;

    //取消疑重
    CtmJSONObject cancleRepeat(Long id) throws Exception;

    CtmJSONObject cancelPublish(Long id,String bankSeqNo) throws Exception;

    /**
     * 到账认领中心-退回=取消发布+新增发布子表信息（退回信息回写）
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    CtmJSONObject returnBack(List<BankReconciliation> bankReconciliations, String returnreason) throws Exception;

    List<BillClaimItemVO> findClaimes(Long id) throws Exception;
    /**
     * 回退
     *
     * @param id
     * @param bankSeqNo
     * @param returnreason
     * @return
     * @throws Exception
     */
    CtmJSONObject returnBill(Long id, String bankSeqNo, String returnreason) throws Exception;

    /**
     * 分配业务人员
     *
     * @param id 银行对账单ID
     * @param userlist 对接人
     * @param isAuto true自动分配业务人员(自动分配业务对接人) false手工分配业务人员(通过分配业务人员按钮)
     * @return
     * @throws Exception
     */
    CtmJSONObject dispatchBussiness(String id, String[] userlist, boolean isAuto) throws Exception;

    /**
     * 取消分配
     *
     * @param id 银行对账单ID
     * @param bankSeqNo 银行流水号
     * @return
     */
    CtmJSONObject cancelDispatch(String id, String bankSeqNo);

    /**
     * 批量分配业务人员
     * @param ids 银行对账单ID
     * @param userids 对接人
     * @param isAuto true自动分配业务人员(自动分配业务对接人) false手工分配业务人员(通过分配业务人员按钮)
     * @return
     */
    CtmJSONObject dispatchBatchBussiness(List<String> ids, String[] userids, boolean isAuto);

    /**
     * 批量分配财务人员
     *
     * @param bankReconciliationList
     * @param ids
     * @return
     * @throws Exception
     */
    CtmJSONObject batchDispatch(List<BankReconciliation> bankReconciliationList, String[] ids) throws Exception;

    /**
     * 单条银行对账单分派--自动任务使用
     *
     * @param bankReconciliation 银行对账单
     * @param ids                财务对接人id
     */
    CtmJSONObject dispatchOne(BankReconciliation bankReconciliation, String[] ids) throws Exception;

    /**
     * 关联银行交易回单
     *
     * @param banreconid
     * @param bankelectronicreceiptid
     * @return
     * @throws Exception
     */
    CtmJSONObject receiptassociation(Long banreconid, Long bankelectronicreceiptid) throws Exception;

    /**
     * 取消关联银行交易回单
     *
     * @param id
     * @return
     * @throws Exception
     */
    CtmJSONObject cancelReceiptassociation(Long id) throws Exception;

    void batchUpdateBankReconciliation(Map<String, Object> params) throws Exception;
    /**
     * 业务处理生单资金调度类生单在前端beforeBatchpush事件中发请求根据提前入账判断入账类型的值
     *   //第一次提前入账只能生成收付款单，之后做第二次可以生成其他类型，提前入账肯定为是 然后赋值为冲挂账
     * //如果是正常生单，入账类型为正常入账
     * @param params
     * @throws Exception
     */
    void dealVirtualEntryType(CtmJSONObject params)throws Exception;


    /*
     *@Description 银行对账单列表汇总信息
     *@Date 2023/10/26 14:36
     **/
    CtmJSONObject queryBankSummaryInformation(CtmJSONObject params, HttpServletResponse response) throws Exception;

    /**
     * 查询银行回单url
     * @param
     * @return
     * @throws Exception
     */
    void sendEventOfFileid(Long id,String urlId) throws Exception;

    /**
     * 查询银行回单url
     * @param
     * @return
     * @throws Exception
     */
    void sendEventOfFileidInFinal(BankReconciliation bankReconciliation,String urlId) throws Exception;

    /**
     * 查询取消银行回单url
     * @param
     * @return
     * @throws Exception
     */
    void cancelUrl(Long id,String urlId) throws Exception;

    /**
     * 获取银行对账单对应的回单文件id
     * @param bankReconciliation
     * @return
     * @throws Exception
     */
    String getBankReceiptFileId(BankReconciliation bankReconciliation) throws Exception;

    //取消单据关联
    CtmJSONObject cancelCorrelate(Long id) throws Exception;

    /**
     * 将BankReconciliation转成BankReconciliationVo
     * @param bankReconciliation
     * @return
     */
    BankReconciliationVo convertBankReconciliation2BankReconciliationVO(BankReconciliation bankReconciliation);

    /**
     * 将BankReconciliationbusrelation_b转成BankReconciliationbusrelationVo
     * @param bankRecRel
     * @return
     */
    BankReconciliationbusrelationVo convertBankRecRel2BankRelVO(BankReconciliationbusrelation_b bankRecRel);

    /**
     * 银行流水认领，查询认领单子表信息
     * @param id
     * @return
     * @throws Exception
     */
    Map<String,Object> querySub(String id) throws Exception;

    /**
     * 银行流水认领,根据id查询数据信息
     * @param id
     * @return
     * @throws Exception
     */
    BankReconciliation queryById(Long id) throws Exception;
    /**
     * 银行流水认领,根据ids查询数据信息
     * @param ids
     * @return
     * @throws Exception
     */
    List<BankReconciliation> queryByIds(List<Long> ids) throws Exception;

    /**
     * 查询银行流水认领批改过滤的字段
     * @return
     */
    List<String> queryFilterFields() throws Exception;

    /**
     * 根据锁过滤流水，避免并发的场景
     * @param bankReconciliationList
     * @return
     */
    List<BankReconciliation> filterBankReconciliationByLockKey(List<BankReconciliation> bankReconciliationList, BankReconciliationActions bankReconciliationActions);

    /**
     * 处理回单文件和凭证关联的消息发送
     *
     * @param bankElectronicReceipt
     * @param bankReconciliation
     * @throws Exception
     */
    void handleBankReceiptCorrEvent(BankElectronicReceipt bankElectronicReceipt, BankReconciliation bankReconciliation) throws Exception;


}
