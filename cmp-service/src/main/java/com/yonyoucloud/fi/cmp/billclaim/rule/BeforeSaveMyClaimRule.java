package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author: liaojbo
 * @Date: 2024年08月15日 15:08
 * @Description:
 */


@Slf4j
@Component("beforeSaveMyClaimRule")
public class BeforeSaveMyClaimRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isEmpty(bills)) {
            return new RuleExecuteResult();
        }
        for (BizObject billClaimBizObject : bills) {
            List<BizObject> billClaimItemBizList = billClaimBizObject.get(BillClaim.ITEMS_KEY);
            for (BizObject billClaimItemBiz:
                    billClaimItemBizList) {
                Short oppositetype = billClaimItemBiz.getShort("oppositetype");
                String oppositeobjectId = billClaimItemBiz.get("oppositeobjectid");
                if (null != oppositeobjectId) {
                    //将oppositeobjectid赋值给对应的客户、供应商或用户
                    CommonSaveUtils.setOppositeobjectidToBizField(billClaimItemBiz, oppositetype, oppositeobjectId);
                }
            }
        }
        return new RuleExecuteResult();
    }
}