package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.BizException;
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
 * @Desc 薪资支付删除rule
 * @Date 2020/07/10
 * @Version 1.0
 * @author majfd
 */
@Component
public class SalaryPayDeleteRule extends AbstractCommonRule {

	@Autowired
	private IFundCommonService fundCommonService;

	@Autowired
	private JournalService journalService;

    @Autowired
    private CmCommonService commonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BillDataDto bill = (BillDataDto) paramMap.get("param");
        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);
            if (bill.getPartParam() != null && bill.getPartParam().get("outsystem") != null) {
                //来源为薪资支付撤回操作不做校验
            } else {
                if (bizObject.getShort("srcitem") == EventSource.HRSalaryChase.getValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101264"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005D","该单据不是现金自制单据，不能进行删除！") /* "该单据不是现金自制单据，不能进行删除！" */);
                }
            }
            if(bizObject.get("verifystate")!=null && !(bizObject.get("verifystate").equals(VerifyState.INIT_NEW_OPEN.getValue()) || bizObject.get("verifystate").equals(VerifyState.REJECTED_TO_MAKEBILL.getValue()) || bizObject.get("verifystate").equals(VerifyState.TERMINATED.getValue()))) {
                String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400384", "单据[%s]，不为初始开立/驳回到制单状态/不通过流程终止，不允许删除") /* "单据[%s]，不为初始开立/驳回到制单状态/不通过流程终止，不允许删除" */ ,bizObject.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101265"),message);
            }
            if (!BooleanUtils.b(bizObject.get("isWfControlled"))) {
                if (!(bizObject.getShort("auditstatus") == (AuditStatus.Incomplete.getValue()))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101266"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005C","已审核单据，不能进行删除！") /* "已审核单据，不能进行删除！" */);
                }
            }
            Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, bizObject.getId());
            // 释放资金计划预占
            List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
            if (fundCommonService.checkFundPlanIsEnabledBySalarypay(ServiceNameEnum.SALARY_PAYMENT)) {
                Object isToPushCspl = payBill.get(ICmpConstant.IS_TO_PUSH_CSPL);
                if (ValueUtils.isNotEmptyObj(isToPushCspl) && 2 == Integer.parseInt(isToPushCspl.toString())) {
                    preReleaseFundBillForFundPlanProjectList.add(payBill);
                }
            }

            if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                Map<String, Object> map = new HashMap<>();
                map.put(ICmpConstant.ACCENTITY, payBill.get(ICmpConstant.ACCENTITY));
                map.put(ICmpConstant.VOUCHDATE, payBill.get(ICmpConstant.VOUCHDATE));
                map.put(ICmpConstant.CODE, payBill.get(ICmpConstant.CODE));
                List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(preReleaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, IBillNumConstant.SALARYPAY, map);
                if (ValueUtils.isNotEmptyObj(checkObject)) {
                    try {
                        RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101267"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508010F", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                    }

                }
            }

            Boolean check = journalService.checkJournal(bizObject.getId());
            if (check) {
                throw new CtmException(
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005B","单据已勾对不能删除") /* "单据已勾对不能删除" */);
            }
            // 删除日记账逻辑
            CmpWriteBankaccUtils.delAccountBook(bizObject.getId().toString());
        }
        return new RuleExecuteResult();
    }
}
