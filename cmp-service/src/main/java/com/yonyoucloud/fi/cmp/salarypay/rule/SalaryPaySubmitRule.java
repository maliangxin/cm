package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;

@Component
public class SalaryPaySubmitRule extends AbstractCommonRule {

    @Autowired
    private IFundCommonService fundCommonService;
    @Autowired
    private CmCommonService commonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            Salarypay payBill = (Salarypay) bills.get(0);
            Salarypay currentBill = (Salarypay) MetaDaoHelper.findById(billContext.getFullname(), payBill.getId());
            if (StringUtils.isEmpty(currentBill.getPayBankAccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100728"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B8","该单据的【付款账户】为空不能进行提交操作") /* "该单据的【付款账户】为空不能进行提交操作" */);
            }
            if (currentBill.getSettlemode() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100729"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B9","该单据的【结算方式】为空不能进行提交操作") /* "该单据的【结算方式】为空不能进行提交操作" */);
            }
            short verifyState = ValueUtils.isNotEmptyObj(currentBill.get("verifystate")) ? Short.parseShort(currentBill.get("verifystate").toString()) : (short) -1;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100730"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801B7","流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
            //提交校验资金计划项目以及部门的必填性并占用资金计划
            salarypayExecuteFundPlan(payBill);
            //如果不启用审批流 这里直接调用审批逻辑
            if(null != payBill && (null == payBill.getIsWfControlled() || !payBill.getIsWfControlled()) ){
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
                    cmpBudgetSalarypayManagerService.executeSubmit(currentBill);
                    //刷新pubts
                    Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, payBill.getId(), null);
                    payBill.setPubts(newBill.getPubts());
                    payBill.setIsOccupyBudget(newBill.getIsOccupyBudget());
                }
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("audit", billContext, paramMap);
                result.setCancel(true);
            } else {
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
                    cmpBudgetSalarypayManagerService.executeSubmit(currentBill);
                    //刷新pubts
                    Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, payBill.getId(), null);
                    payBill.setPubts(newBill.getPubts());
                    payBill.setIsOccupyBudget(newBill.getIsOccupyBudget());
                }
            }
        }
        return result;
    }

    /**
     * 部门：现金参数-是否启用资金计划为是，并且资金计划-计划控制设置-业务控制方式为按部门控制，导入时，校验部门必填
     * 资金计划项目：现金参数-是否启用资金计划为是，并且资金计划-计划控制设置-业务控制方式为按部门控制/按会计主体控制，
     * @param payBill
     */
    private void salarypayExecuteFundPlan(Salarypay payBill) throws Exception {
        //提交校验资金计划项目以及部门的必填性并占用资金计划
        if (fundCommonService.checkFundPlanIsEnabledBySalarypay(ServiceNameEnum.SALARY_PAYMENT)) {
//            Object controlSet = commonService.queryStrategySetbByCondition(payBill.get(IBussinessConstant.ACCENTITY), payBill.get(IBussinessConstant.CURRENCY), payBill.get(ICmpConstant.VOUCHDATE));
            List<BizObject> employFundBillForFundPlanProjectList = new ArrayList<>();
            if (payBill.get(ICmpConstant.FUND_PLAN_PROJECT) != null) {
                payBill.set(ICmpConstant.IS_TO_PUSH_CSPL, 1);
                employFundBillForFundPlanProjectList.add(payBill);
            }

            if (CollectionUtils.isNotEmpty(employFundBillForFundPlanProjectList)) {
                Map<String, Object> map = new HashMap<>();
                map.put(ICmpConstant.ACCENTITY, payBill.get(ICmpConstant.ACCENTITY));
                map.put(ICmpConstant.VOUCHDATE, payBill.get(ICmpConstant.VOUCHDATE));
                map.put(ICmpConstant.CODE, payBill.get(ICmpConstant.CODE));
                List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(employFundBillForFundPlanProjectList, IStwbConstantForCmp.EMPLOY, IBillNumConstant.SALARYPAY, map);
                if (com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(checkObject)) {
                    try {
                        RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100731"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C3", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                    }
                }
            }


        }
    }
}
