package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.reqvo.AgentPaymentReqVO;
import com.yonyoucloud.ctm.stwb.unifiedsettle.vo.UnifiedSettleDetail;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

/**
 * <h1>IFundCommonService</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-22 14:43
 */
public interface IFundCommonService {
    /**
     * 根据对方类型校验数据库里账号和档案账号是否一致
     *
     * @param caobject
     * @param accountId
     * @param accountNo
     * @throws Exception
     */
    void checkCaObjectAccountNoEqual(short caobject, String accountId, String accountNo) throws Exception;

    void checkStaffOppositeAccount(String billnum, BizObject bizObject) throws Exception;

    /**
     * 结算简强后资金收付保存的时候必须补充的字段赋值
     * @param billnum
     * @param bizObject
     * @throws Exception
     */
    void setSimpleSettleValue(String billnum, BizObject bizObject) throws Exception;

    /**
     * <h2>检查是否启用商业汇票模块</h2>
     *
     * @param accent :
     * @return com.alibaba.fastjson.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/12/22 14:47
     */
    CtmJSONObject isEnableBsdModule(String accent) throws Exception;


    /**
     * <h2>OpenApi删除操作</h2>
     *
     * @param param :
     * @return com.alibaba.fastjson.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/8/26 13:52
     */
    CtmJSONObject deleteFundBillByIds(CtmJSONObject param) throws Exception;

    CtmJSONObject querySettledDetail(CtmJSONObject param) throws Exception;

    CtmJSONObject checkCustomerAccount(CtmJSONObject param) throws Exception;

    CtmJSONObject checkEmployeeAccount(CtmJSONObject param) throws Exception;

    CtmJSONObject checkSupplierAccount(CtmJSONObject param) throws Exception;

    Object queryBfundbusinobjData(CtmJSONObject param) throws Exception;

    Object reverseQueryFundBusinessObjectData(CtmJSONObject param) throws Exception;

    /**
     * <h2>OPenAPI资金收付款单详情查询接口</h2>
     * @author Sun GuoCai
     * @date 2022/5/9 17:42
     * @param id: 主表id
     * @param code:  单据编码
     * @return org.imeta.orm.base.BizObject
     */
    String queryFundBillByIdOrCode(String billNum, Long id, String code,String fundBillSubPubtsBegin, String fundBillSubPubtsEnd, Short settleStatus) throws Exception;


    /**
     * <h2>资金付款单结算成功协同生成资金收款单，通过单据转换规则</h2>
     *
     * @param fundPayment_b : BO实体
     * @author Sun GuoCai
     * @since 2022/10/18 10:16
     */
    CtmJSONObject fundPaymentBillCoordinatedGeneratorFundCollectionBill(FundPayment_b fundPayment_b) throws Exception;

    /**
     * 根据资金组织，部门，查询成本中心
     * @param accEntity
     * @param bizObject
     */
//    void setCostCenter(String accEntity, BizObject bizObject);

    /**
     * <h2>生成凭证</h2>
     * @author Sun GuoCai
     * @date 2022/12/17 7:33
     * @param billb:
     * @param entityName:
     */
    void generateVoucher(BizObject billb, String entityName,boolean redReset) throws Exception;


    /**
     * <h2>检查现金参数是否启用资金计划</h2>
     *
     * @param accentity : 资金组织
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/2/21 14:16
     */
    boolean checkFundPlanIsEnabled(String accentity) throws Exception;

    boolean checkFundPlanIsEnabledBySalarypay(ServiceNameEnum serviceNameEnum) throws Exception;

    boolean checkFundPlanControlIsEnabled(String serviceName) throws Exception;

    void deleteNoteList(List<Map<String, Object>> noteMaps, Integer billDirection, BizObject bizObject, BizObject subBiz) throws Exception;

    /**
     * <h2>更新该币种银行结息账号预提规则的上次结息结束日</h2>
     *
     * @param fundSubList : 单据子表信息
     * @param status          : 单据状态：1：撤回，2：审核
     * @author Sun GuoCai
     * @since 2023/5/9 9:56
     */
    void updateWithholdingRuleSettingLastInterestSettlementDate(List<BizObject> fundSubList, Integer status) throws Exception;

    /**
     * <h2>根据款项类型判断此款项类型是否为账户结息，注：账户结息的编码为301</h2>
     *
     * @param quickType :款项类型
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/5/14 11:15
     */
    boolean isInterestWithQuickType(Object quickType) throws Exception;

    /**
     * <h2>资金收付款单前端点击保存并提交，未启用审批流的前提下，保存并提交成功后，再推结算</h2>
     * 原因：如果未启用审批流，则先不推结算（因为单据还没入库，结算那边调用单据转换规则会报错，未在库里查到数据）
     *
     * @param params : 入参
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/5/14 11:18
     */
    boolean pushSettleBill(CtmJSONObject params) throws Exception;


    /**
     * <h2>资金收付款单推待结算</h2>
     *
     *
     * @param billNum : 入参
     * @param ids : 入参
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/5/14 11:18
     */
    boolean pushDataSettle(String billNum, String ids) throws Exception;

    /**
     * <h2>资金收款结息单撤回相关校验</h2>
     *
     * @param bills : 单据信息
     * @author Sun GuoCai
     * @since 2023/5/23 15:27
     */
    void statementUnSubmitVerificationByFundCollection(List<BizObject> bills) throws Exception;    /**
     * <h2>资金付款结息单撤回相关校验</h2>
     *
     * @param bills : 单据信息
     * @author Sun GuoCai
     * @since 2023/5/23 15:27
     */
    void statementUnSubmitVerificationByFundPayment(List<BizObject> bills) throws Exception;

    /**
     * <h2>设置是否结算成功时过账字段值</h2>
     *
     * @param bill : 单据数据信息
     * @param accEntityId : 资金组织
     * @author Sun GuoCai
     * @since 2023/5/26 9:18
     */
    void setSettleSuccessPostValue(BizObject bill, String accEntityId) throws Exception;

    /**
     * 更新资金付款单退票相关数据
     * @param dataSettledDetail
     * @param billbs
     * @throws Exception
     */
    void updateRefundSettledInfoOfFundPayment(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs) throws Exception;

    /**
     * 更新资金付款单关联信息
     * @param dataSettledDetail
     * @param billbs
     * @throws Exception
     */
    void updateFundPaymentRelationInfo(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs) throws Exception;

    void updateFundPaymentRelationInfoSimple(UnifiedSettleDetail unifiedSettleDetail, List<FundPayment_b> billbs) throws Exception;

    /**
     * 更新资金收款单关联信息
     * @param dataSettledDetail
     * @param billbs
     * @throws Exception
     */
    void updateFundCollectionRelationInfo(DataSettledDetail dataSettledDetail, List<FundCollection_b> billbs) throws Exception;

    void updateFundCollectionRelationInfoSimple(UnifiedSettleDetail unifiedSettleDetail, List<FundCollection_b> billbs) throws Exception;

    CtmJSONObject queryBillTypeIdByTradeTypeId(String tradeType);


    /**
     * <h2>查询统收统支关系组默认账户</h2>
     *
     * @param param : 子表id
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2023/11/25 15:13
     */
    CtmJSONObject queryIncomeAndExpenditureDefaultAccount(CtmJSONObject param) throws Exception;

    /**
     * <h2>查询预估汇率</h2>
     *
     * @param params : 入参集合
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2024/1/5 10:22
     */
    CtmJSONArray querySwapOutExchangeRate(CtmJSONArray params) throws Exception;

    CtmJSONArray updateUserId(String startCreateTime ,String  endCreateTime) throws Exception;

    /**
     * 给统一结算单发事件更新结算单明细信息
     * @param agentPaymentReqVO
     */
    void sendEventToSettleBenchDetail(AgentPaymentReqVO agentPaymentReqVO);

    /**
     *  检查是否已经有结算单
     * @param businessDetailsIdList
     */
    /*void checkHasSettlementBill(List<String> businessDetailsIdList) throws Exception;*/
}
