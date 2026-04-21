package com.yonyoucloud.fi.cmp.auth.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StaffAuthFilterRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        if(IRefCodeConstant.AA_OPERATOR.equals(billDataDto.getrefCode())){
            String billnum = billContext.getBillnum();
            List<String> deptIds;
            BizObject bizObject = getBills(billContext, map).get(0);
            String fullName = billDataDto.getFullname();
            // 资金收付款单子表过滤部门员工
            if (FundCollection_b.ENTITY_NAME.equals(fullName) && IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                deptIds = new ArrayList<>();
                List<BizObject> bodyItems = bizObject.getBizObjects("FundCollection_b", BizObject.class);
                setDepartments(bodyItems, deptIds);
            }else if(FundPayment_b.ENTITY_NAME.equals(fullName) && IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                deptIds = new ArrayList<>();
                List<BizObject> bodyItems = bizObject.getBizObjects("FundPayment_b", BizObject.class);
                setDepartments(bodyItems, deptIds);
            } else {
                deptIds = AuthUtil.getBizObjectAttr(bizObject, IBillConst.DEPT) ;
            }
            List<String> orgIds = null;
            List<String> bizObjectAttr = AuthUtil.getBizObjectAttr(bizObject, IBillConst.ACCENTITY);
            if ((deptIds == null || deptIds.isEmpty()) && !CollectionUtils.isEmpty(bizObjectAttr)) {
                // 委托组织
                Set<String> delegateOrg = FIDubboUtils.getDelegateHasSelf(bizObjectAttr.get(0));;
                String[] delegateOrgArray = delegateOrg.toArray(new String[delegateOrg.size()]);
                // 共享部门
                Set<String> shareDept = FIDubboUtils.getDeptShare(delegateOrgArray);
                // 共享组织
                Set<String> shareOrg = FIDubboUtils.getOrgShareHasSelf(delegateOrgArray);
                delegateOrg.addAll(shareOrg);
                if (shareDept != null && !shareDept.isEmpty()) {
                    deptIds.addAll(shareDept);
                }

                orgIds = new ArrayList<>();
                orgIds.addAll(delegateOrg);
            }
            // 部门过滤
            if (deptIds != null && !deptIds.isEmpty()) {
                SimpleFilterVO simpleFilterVO = new SimpleFilterVO(ConditionOperator.or);
                if (orgIds != null && !orgIds.isEmpty()) {
                    simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.org_id", "in", orgIds));
                }
                simpleFilterVO.addCondition(new SimpleFilterVO("mainJobList.dept_id", "in", deptIds));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, simpleFilterVO);
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("mainJobList.org_id", "in", orgIds));
            }

            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("mainJobList.dr", "eq", 0), new SimpleFilterVO("mainJobList.endflag", "eq", 0));

            // 业务员标记额外添加
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("biz_man_tag", "eq", true) );

        }





        return new RuleExecuteResult();
    }

    private void setDepartments(List<BizObject> bodyItems, List<String> deptIds) {
        if(null != bodyItems && bodyItems.size() > 0){
            for (BizObject item : bodyItems) {
                if(item.get(IBillConst.DEPT) != null){
                    deptIds.add(item.get(IBillConst.DEPT));
                }
            }
        }
    }
}
