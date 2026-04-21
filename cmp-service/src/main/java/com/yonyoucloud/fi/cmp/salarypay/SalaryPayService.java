package com.yonyoucloud.fi.cmp.salarypay;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;

import java.util.List;
import java.util.Map;

/**
* @InterfaceName SalaryPayService
* @Description 薪资支付服务接口
* @Author majfd
* @Date 2020/06/22 15:31
* @Version 1.0
**/
public interface SalaryPayService {
	
    //代发工资预下单
    String BATCH_PAY_PRE_ORDER = "11B12P";
    //预下单交易确认（下单）
    String PRE_ORDER_TRANSACTION_CONFIRM = "50C10";
    //代发工资结果查询
    String BATCH_PAY_DETAIL_STATUS_QUERY = "41B12";
    //代发报销预下单
    String NOWAGES_PAY_PRE_ORDER = "11B11P";
	//非工资类代发结果查询
	String NOWAGES_PAY_STATUS_QUERY = "41B11";
	
	/**
	 * @Description 网银预下单
	 * @param params
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    String internetBankPlaceOrder(CtmJSONObject params) throws Exception;

    /**
	 * @Description 预下单交易确认
	 * @param params
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    String confirmPlaceOrder(CtmJSONObject params) throws Exception;

    /**
	 * @Description 批量支付明细状态查询
	 * @param param
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    String queryBatchDetailPayStatus(CtmJSONObject param) throws Exception;

	/**
	 * @Description 批量支付明细状态查询:调度任务
	 * @param
	 * @return com.alibaba.fastjson.CtmJSONObject
	 * @throws Exception
	 */
	void queryBatchDetailPayStatusByauto(Map<String, Object> salarypayMap, String customNo) throws Exception;

    /**
	 * @Description 线下支付
	 * @param params
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    CtmJSONObject offLinePay(CtmJSONObject params) throws Exception;

    /**
	 * @Description 取消线下支付
	 * @param params
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    CtmJSONObject cancelOffLinePay(CtmJSONObject params) throws Exception;

    /**
     * @Description 获取银企联渠道号
     * @Param []
     * @Return java.lang.String
     **/
    String getChanPayChanelNo();

    /**
	 * @Description 审批
	 * @param param
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    CtmJSONObject audit(CtmJSONObject param) throws Exception;

    /**
	 * @Description 弃审
	 * @param param
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    CtmJSONObject unAudit(CtmJSONObject param) throws Exception;
    
    /**
     * 根据ids查询主子vo数据
     * @param ids
     * @return
     * @throws Exception
     */
    List<Salarypay> queryAggvoByIds(List<Object> ids) throws Exception;
    
    /**
     * 根据来源单据id查询薪资支付单
     * @param srcbillid
     * @param listSrcBillid_b 
     * @return
     * @throws Exception
     */
    void hasSalaryPayBySrcbillid(String srcbillid, List<String> listSrcBillid_b) throws Exception;

    /**
	 * @Description 作废
	 * @param param
	 * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
	 * @throws Exception
	 */
    CtmJSONObject invalid(CtmJSONObject param) throws Exception;
    
    /**
     * 获取通知hr系统支付状态消息
     * @param srcbillid
     * @return
     * @throws Exception
     */
    String sendPayStatus(List<String> srcbillid) throws Exception;

	boolean hasUnknownData(CtmJSONObject param) throws Exception;
	String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception;
	boolean budgetSuccess(Salarypay salarypay) throws Exception;

	void updatePayStatusAfterOffline(Salarypay salarypay) throws Exception;

	/**
	 * 网银支付是否检验UKey
	 *
	 * @return 是否校验结果-boolean
	 */
	CtmJSONObject payBankCheckUKey();
}
