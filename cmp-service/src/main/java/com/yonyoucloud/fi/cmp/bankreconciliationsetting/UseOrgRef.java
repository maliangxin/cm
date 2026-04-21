package com.yonyoucloud.fi.cmp.bankreconciliationsetting;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 授权使用组织参照处理
 *
 * @author xuwei
 * @date 2024/01/29
 */
@Component
public class UseOrgRef extends AbstractCommonRule {

    //public final String USE_ORG_REF = "ucf-org-center.bd_financeorgtreeref_na";

    public final String BANKACCOUNT_ORG_REF = "ucfbasedoc.bd_enterprisebankacct";

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);
            BillDataDto bill = (BillDataDto) getParam(map);
            List<BankReconciliationSetting_b> itemList = bizObject.get("bankReconciliationSetting_b");
            //子表授权使用组织useorg的参照
            if (bill.getrefCode().equals(IRefCodeConstant.FUNDS_ORGTREE) && itemList!=null && itemList.get(0) !=null && itemList.get(0).getBankaccount() != null) {
                //查询授权使用组织
                EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(itemList.get(0).getBankaccount());
                List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                List<String> useOrgs = orgRangeVOList.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
                //通过授权使用组织过滤参照
                FilterVO filterVO = bill.getTreeCondition();
                if(null == filterVO){
                    filterVO = new FilterVO();
                }
                bill.setCondition(filterVO);
                bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, useOrgs));
            }else if(bill.getrefCode().equals(BANKACCOUNT_ORG_REF)){
                String accentity = bizObject.getString("accentity");
                if (bill.getCondition() == null) {
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
                    bill.setCondition(conditon);
                } else {
                    bill.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "eq", accentity));
                }
            }
        }

        return new RuleExecuteResult();
    }
}
