package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.enums.PushCsplStatusEnum;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;

/**
 * <h1>薪资支付单审批流为驳回制单或终止：释放资资金计划项目；撤销 驳回到制单：占用资金计划项目</h1>
 * <p>
 * "deleteAll":删除流程实例或撤回流程到初始状态
 * "withdraw":(终审)撤销审核
 * "REJECTTOSTART":驳回制单
 * "WITHDRAWREJECTTOSTART":撤销驳回制单
 * "ACTIVITI_DELETED":终止
 * *
 *
 * @author xuxbo
 * @date 2023/7/11 10:54
 */
@Component("salaryPayAfterChangeVerifystateRule")
@Slf4j
@RequiredArgsConstructor
public class SalaryPayAfterChangeVerifystateRule extends AbstractCommonRule {

    private final CmCommonService commonService;
    private final IFundCommonService fundCommonService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;
    @Autowired
    private SalaryPayService salaryPayService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String deleteReason = billContext.getDeleteReason();
        List<BizObject> bills = getBills(billContext, map);
        String fullName = billContext.getFullname();
        String billnum = billContext.getBillnum();
        for (BizObject bizobject : bills) {
            BizObject currentBill = MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
            // 驳回制单或终止：释放资资金计划项目
            if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && ("REJECTTOSTART".equals(deleteReason) || "ACTIVITI_DELETED".equals(deleteReason))) {
                List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
                Object isToPushCspl = currentBill.get(ICmpConstant.IS_TO_PUSH_CSPL);
                if (ValueUtils.isNotEmptyObj(isToPushCspl) && ICmpConstant.CONSTANT_ONE == Integer.parseInt(isToPushCspl.toString())) {
                    currentBill.set(ICmpConstant.IS_TO_PUSH_CSPL, 2);
                    releaseFundBillForFundPlanProjectList.add(currentBill);
                }
                // 资金计划额度释放
                if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put(ICmpConstant.ACCENTITY, currentBill.get(ICmpConstant.ACCENTITY));
                    map2.put(ICmpConstant.VOUCHDATE, currentBill.get(ICmpConstant.VOUCHDATE));
                    map2.put(ICmpConstant.CODE, currentBill.get(ICmpConstant.CODE));
                    List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(releaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, billnum, map2);
                    if (ValueUtils.isNotEmptyObj(checkObject)) {
                        try {
                            RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                        } catch (Exception e) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100184"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080056", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ +e.getMessage());
                        }
                        EntityTool.setUpdateStatus(releaseFundBillForFundPlanProjectList);
                        if (IBillNumConstant.SALARYPAY.equals(billnum)) {
                            MetaDaoHelper.update(Salarypay.ENTITY_NAME, releaseFundBillForFundPlanProjectList);
                        }
                    }
                }
                // 撤销驳回到制单：占用资金计划项目
            } else if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && "WITHDRAWREJECTTOSTART".equals(deleteReason)) {
                List<BizObject> employFundBillForFundPlanProjectList = new ArrayList<>();
                boolean checkFundPlanIsEnabled = false;
                checkFundPlanIsEnabled = fundCommonService.checkFundPlanIsEnabledBySalarypay(ServiceNameEnum.SALARY_PAYMENT);
                if (checkFundPlanIsEnabled) {
                    Object isToPushCspl = currentBill.get(ICmpConstant.IS_TO_PUSH_CSPL);
                    if (ValueUtils.isNotEmptyObj(isToPushCspl)
                            && PushCsplStatusEnum.PRE_OCCUPIED.getValue() == Integer.parseInt(isToPushCspl.toString())
                            && ValueUtils.isNotEmptyObj(currentBill.get("fundPlanProject"))) {
                        currentBill.set(ICmpConstant.IS_TO_PUSH_CSPL, ICmpConstant.CONSTANT_ONE);
                        employFundBillForFundPlanProjectList.add(currentBill);
                    }
                }

                // 资金计划额度占用
                if (CollectionUtils.isNotEmpty(employFundBillForFundPlanProjectList)) {
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put(ICmpConstant.ACCENTITY, currentBill.get(ICmpConstant.ACCENTITY));
                    map2.put(ICmpConstant.VOUCHDATE, currentBill.get(ICmpConstant.VOUCHDATE));
                    map2.put(ICmpConstant.CODE, currentBill.get(ICmpConstant.CODE));
                    List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(employFundBillForFundPlanProjectList, IStwbConstantForCmp.EMPLOY, billnum, map2);
                    if (ValueUtils.isNotEmptyObj(checkObject)) {
                        CapitalPlanExecuteResp capitalPlanExecuteResp;
                        try {
                            capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                        } catch (Exception e) {
                            log.error("fundCollectionSubmitReleaseFundPlan error, errorMsg={}", e.getMessage());
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100184"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080056", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                        }
                        if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                                && "500".equals(capitalPlanExecuteResp.getCode())
                                && capitalPlanExecuteResp.getSuccessCount()==0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100184"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080056", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
                        }
                        EntityTool.setUpdateStatus(employFundBillForFundPlanProjectList);
                        if (IBillNumConstant.SALARYPAY.equals(billnum)) {
                            MetaDaoHelper.update(Salarypay.ENTITY_NAME, employFundBillForFundPlanProjectList);
                        }
                    }
                }
            }
            //驳回/撤回到发起人/审批终止
            //是否占预算为预占成功时，删除预占
            if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && ("REJECTTOSTART".equals(deleteReason) || "ACTIVITI_DELETED".equals(deleteReason))) {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
                    Salarypay salarypay = (Salarypay) MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
                    cmpBudgetSalarypayManagerService.executeBudgetDelete(salarypay);
                    //刷新pubts
                    Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, currentBill.getId(), null);
                    bizobject.setPubts(newBill.getPubts());
                    Salarypay bizobject1 = (Salarypay) bizobject;
                    bizobject1.setIsOccupyBudget(newBill.getIsOccupyBudget());
                }
            } else if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && ("WITHDRAWREJECTTOSTART".equals(deleteReason))) {
                log.error("WITHDRAWREJECTTOSTART........");
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
                    Salarypay salarypay = (Salarypay) MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
                    cmpBudgetSalarypayManagerService.executeSubmit(salarypay);
                    //刷新pubts
                    Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, currentBill.getId(), null);
                    bizobject.setPubts(newBill.getPubts());
                    Salarypay bizobject1 = (Salarypay) bizobject;
                    bizobject1.setIsOccupyBudget(newBill.getIsOccupyBudget());
                }
            } else if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && ("withdraw".equals(deleteReason))) {
                log.error("withdraw........");
            }
        }
        return new RuleExecuteResult();
    }
}
