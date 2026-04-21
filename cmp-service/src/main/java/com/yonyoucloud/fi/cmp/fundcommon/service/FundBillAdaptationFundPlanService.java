package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import org.imeta.orm.base.BizObject;

import java.util.List;

/**
 * <h1>资金收付款单更新资金计划接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-29 10:48
 */

public interface FundBillAdaptationFundPlanService {


    /**
     * <h2>资金付款单提交占用资金计划</h2>
     *
     * @param fundPayment : 资金付款单数据
     * @author Sun GuoCai
     * @since 2024/5/29 11:25
     */
    void fundPaymentSubmitEmployFundPlan(FundPayment fundPayment) throws Exception;


    /**
     * <h2>资金收款单提交占用资金计划</h2>
     *
     * @param fundCollection : 资金收款单数据
     * @author Sun GuoCai
     * @since 2024/5/29 11:25
     */
    void fundCollectionSubmitEmployFundPlan(FundCollection fundCollection) throws Exception;

    /**
     * <h2>资金付款单撤回占用资金计划</h2>
     *
     * @param fundPayment : 资金付款单数据
     * @author Sun GuoCai
     * @since 2024/5/29 11:25
     */
    void fundPaymentUnSubmitReleaseFundPlan(FundPayment fundPayment) throws Exception;

    /**
     * <h2>资金收款单撤回占用资金计划</h2>
     *
     * @param fundCollection : 资金收款单数据
     * @author Sun GuoCai
     * @since 2024/5/29 11:25
     */
    void fundCollectionUnSubmitReleaseFundPlan(FundCollection fundCollection) throws Exception;

    void fundBillEditEmployOrReleaseFundPlan(
            String billNum,
            BizObject bizObject,
            List<BizObject> employFundBillForFundPlanProjectList,
            List<BizObject> releaseFundBillForFundPlanProjectList,
            List<BizObject> preEmployFundBillForFundPlanProjectList,
            List<BizObject> preReleaseFundBillForFundPlanProjectList
    ) throws Exception;

    void fundBillReleaseFundPlan(String billNum, BizObject bizObject, List<BizObject> releaseFundBillForFundPlanProjectList, Object settleFailed, Object reFund, String occupyFlag) throws Exception;

    void fundBillEmployFundPlan(String billNum, BizObject bizObject, List<BizObject> employFundBillForFundPlanProjectList, String occupyFlag) throws Exception;

    void fundPlanProjectNotControl(String billnum, BizObject bizObject, List<BizObject> releaseFundBillForFundPlanProjectList) throws Exception;


    /**
     * <h2>根据资金计划明细id查询资金计划明细</h2>
     *
     * @param param : 入参
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2024/6/15 14:15
     */
    CtmJSONObject queryFundPlanDetailById(CtmJSONObject param) throws Exception;

    /**
     * <h2>资金收付款单更新和删除明细行时，预占用和释放资金计划</h2>
     *
     * @param billNum: 单据编码
     * @param bizObject: 单据数据
     * @param preEmployFundBillForFundPlanProjectList: 预占数据集合
     * @param preReleaseFundBillForFundPlanProjectList: 释放预占数据集合
     * @return java.util.List<org.imeta.orm.base.BizObject>
     * @author Sun GuoCai
     * @date 2024/10/11 10:25
     */
    List<BizObject> fundBillPreEmployOrReleaseFundPlanBeforeSaveForUpdateAndDelete(String billNum, BizObject bizObject, List<BizObject> preEmployFundBillForFundPlanProjectList, List<BizObject> preReleaseFundBillForFundPlanProjectList) throws Exception;


    /**
     * <h2>资金收付款单保存以及更新插入明细行时，预占用和释放资金计划</h2>
     *
     * @param billNum: 单据编码
     * @param bizObject: 单据数据
     * @param preEmployFundBillForFundPlanProjectList: 预占数据集合
     * @param preReleaseFundBillForFundPlanProjectList: 释放预占数据集合
     * @return java.util.List<org.imeta.orm.base.BizObject>
     * @author Sun GuoCai
     * @date 2024/10/11 10:26
     */
    List<BizObject> fundBillPreEmployOrReleaseFundPlanAfterSaveForInsert(String billNum, BizObject bizObject, List<BizObject> preEmployFundBillForFundPlanProjectList, List<BizObject> preReleaseFundBillForFundPlanProjectList) throws Exception;

    void fundPlanProjectPreEmployOrReleaseNotControl(String billnum, BizObject bizObject, List<BizObject> preReleaseFundBillForFundPlanProjectList) throws Exception;

}
