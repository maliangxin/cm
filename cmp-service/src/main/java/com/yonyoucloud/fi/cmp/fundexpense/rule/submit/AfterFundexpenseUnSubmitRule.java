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
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component("afterFundexpenseUnSubmitRule")
public class AfterFundexpenseUnSubmitRule extends AbstractCommonRule {
    @Resource
    private FundexpenseService fundexpenseService;
    @Resource
    private IFundCommonService fundCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        List<BizObject> bills = this.getBills(billContext, map);
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        if (CollectionUtils.isNotEmpty(bills)){
            if (bills != null && bills.size() > 0) {
                //获取前端传过来的值对象
                Fundexpense fundexpense1 = (Fundexpense) bills.get(0);
                Fundexpense fundexpense = fundexpense1.getId() == null ? null : MetaDaoHelper.findById(Fundexpense.ENTITY_NAME, fundexpense1.getId(), null);
                if (fundexpense != null) {
                    Boolean isWfControlled = fundexpense.getIsWfControlled();
                    short auditstatus = fundexpense.getAuditstatus();
                    if(isWfControlled){
                        if (ExpenseAuditStatus.approval.getValue() != auditstatus) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100042"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_DRFT-BE_17FE1DB204180199", "单据%s,状态%s,不可撤回!"), fundexpense.getCode(), ExpenseAuditStatus.getName(auditstatus)));
                        }
                        fundexpense.setAuditstatus(ExpenseAuditStatus.unsubmit.getValue());
                        boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue());
                        // 撤回时释放预算
                        if(checkFundPlanIsEnabled){
                            fundexpenseService.gcExecuteBatchUnSubmit (bills.get(0), fundexpense.detail(), IBillNumConstant.FUNDEXPENSE, BillAction.APPROVE_PASS);
                        }
                    }else{
                        if (ExpenseAuditStatus.passed.getValue() != auditstatus) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100042"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_DRFT-BE_17FE1DB204180199", "单据%s,状态%s,不可撤回!"), fundexpense.getCode(), ExpenseAuditStatus.getName(auditstatus)));
                        }
                        BillDataDto billDataDto = new BillDataDto();
                        billDataDto.setBillnum("fundexpense");
                        billDataDto.setData(fundexpense);
                        BillBiz.executeUpdate("unaudit", billDataDto);
                        ruleExecuteResult.setCancel(true);
                        return ruleExecuteResult;
                    }
                }
            }
        }
        return ruleExecuteResult;
    }
}
