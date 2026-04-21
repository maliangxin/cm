package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.BillNumberEnum;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

;

@Slf4j
@Component("beforeQueryBankReceiptRule")
public class BeforeQueryBankReceiptRule extends AbstractCommonRule {

    private static final String AUTHORUSEACCENTITY = "authoruseaccentity";
    private static final String ACCENTITY = "accentity";

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    /**
     * 查询前规则，给交易回单和电子对账单使用，处理账户共享后组织的筛选
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        // 1. 获取数据
        List<BizObject> bills = getBills(billContext, paramMap);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();
        List<String> orgList = new ArrayList<>();
        List<String> accentityList = new ArrayList<>();
        if (filterVO != null) {
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            for (FilterCommonVO vo : commonVOs) {
                switch (vo.getItemName()) {
                    case AUTHORUSEACCENTITY:
                        // 删除授权使用组织查询条件
                        orgList.addAll((List)vo.getValue1());
                        vo.setValue1("");
                        vo.setItemName("");
                        break;
                    case ACCENTITY:
                        // 删除所属组织查询条件
                        accentityList.addAll((List)vo.getValue1());
                        vo.setValue1("");
                        vo.setItemName("");
                        break;
                    default:
                        break;
                }
            }
            //交易回单和电子对账单的账户字段区分
            String enterpriseBankAccountStr = "enterpriseBankAccount";
            if (IBillNumConstant.CMP_BANKELECTRONICRECEIPTLIST.equals(billContext.getBillnum())) {
                 enterpriseBankAccountStr = "enterpriseBankAccount";
            }else if (IBillNumConstant.ELECTRONICSTATEMENTCONFIRMLIST.equals(billContext.getBillnum())){
                 enterpriseBankAccountStr = "bankaccount";
            }
            if(orgList.size() > 0 && accentityList.size() > 0){
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(orgList);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                List<String> accounts = new ArrayList<>();
                for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                    accounts.add(enterpriseBankAcctVOWithRange.getId());
                }
                QueryConditionGroup conditionGroupAuth = new QueryConditionGroup(ConditionOperator.or);//授权使用组织过滤
                QueryCondition queryConditionAuth = null;
                if(CollectionUtils.isNotEmpty(accounts)){
                    queryConditionAuth = new QueryCondition(enterpriseBankAccountStr, ConditionOperator.in, accounts);
                } else {
                    queryConditionAuth = new QueryCondition(enterpriseBankAccountStr, ConditionOperator.eq, 0);
                }
                conditionGroupAuth.addCondition(queryConditionAuth);
                QueryCondition queryConditionOwnOrgid = new QueryCondition("accentity", ConditionOperator.in, accentityList);
                conditionGroupAuth.addCondition(queryConditionOwnOrgid);
                filterVO.setQueryConditionGroup(conditionGroupAuth);
            } else if(orgList.size() > 0 && accentityList.size() < 1){
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(orgList);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                List<String> accounts = new ArrayList<>();
                for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                    accounts.add(enterpriseBankAcctVOWithRange.getId());
                }
                if(CollectionUtils.isNotEmpty(accounts)){
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(enterpriseBankAccountStr, ICmpConstant.QUERY_IN, accounts));
                } else {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(enterpriseBankAccountStr, ICmpConstant.QUERY_EQ, 0));
                }
            } else if(orgList.size() < 1 && accentityList.size() > 0){
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_IN, accentityList));
            } else if(orgList.size() < 1 && accentityList.size() < 1){
                String serviceCode = AppContext.getThreadContext("serviceCode");
                Set<String> orgSet = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
                orgList = new ArrayList<>(orgSet);
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(orgList);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS =  enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                List<String> accounts = new ArrayList<>();
                for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS){
                    accounts.add(enterpriseBankAcctVOWithRange.getId());
                }
                if(CollectionUtils.isNotEmpty(accounts)){
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(enterpriseBankAccountStr, ICmpConstant.QUERY_IN, accounts));
                } else {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(enterpriseBankAccountStr, ICmpConstant.QUERY_EQ, 0));
                }
            }
        }
        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }

}
