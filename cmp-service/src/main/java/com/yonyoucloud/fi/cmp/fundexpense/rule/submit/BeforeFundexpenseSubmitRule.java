package com.yonyoucloud.fi.cmp.fundexpense.rule.submit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.ExpenseAuditStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundexpense.service.FundexpenseService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import org.imeta.biz.base.BizException;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component("beforeFundexpenseSubmitRule")
public class BeforeFundexpenseSubmitRule extends AbstractCommonRule {

    @Resource
    private FundexpenseService fundexpenseService;
    @Resource
    private IFundCommonService fundCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense1 = (Fundexpense) bills.get(0);
            //todo 什么时候可以调用占预算直接掉就行  gcExecuteBatchSubmit
            Fundexpense fundexpense = MetaDaoHelper.findById(Fundexpense.ENTITY_NAME, fundexpense1.getId());
            Short auditstatus = fundexpense.getAuditstatus();
            if (auditstatus != ExpenseAuditStatus.unsubmit.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101163"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_DRFT-BE_1CEFBABA04580121", "单据状态不是待提交，不能提交") /* "单据状态不是待提交，不能提交" */);
            }
            if (!BooleanUtils.b(fundexpense.getIsWfControlled())) {
                BillDataDto billDataDto = new BillDataDto();
                billDataDto.setBillnum("fundexpense");
                billDataDto.setData(fundexpense);
                BillBiz.executeUpdate("audit", billDataDto);
                ruleExecuteResult.setCancel(true);
                return ruleExecuteResult;
            }
            //提交后更新单据状态、审批状态都为审批中
            fundexpense.setAuditstatus(ExpenseAuditStatus.approval.getValue());
            boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue());
            //提交时暂预算
            if (checkFundPlanIsEnabled) {
                fundexpenseService.gcExecuteBatchSubmit(bills.get(0), fundexpense.detail(), IBillNumConstant.FUNDEXPENSE, BillAction.APPROVE_PASS);
            }
        }
        return ruleExecuteResult;
    }
}
