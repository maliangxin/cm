package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>付款申请审核规则</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/7/9 13:57
 */
@Slf4j
@Component("commonAfterBillAuditRule")
@RequiredArgsConstructor
public class CommonAfterBillAuditRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        List<BizObject> currentBilles = new ArrayList<>();
        String fullname = billContext.getFullname();
        for (BizObject bizobject : bills) {
            BizObject currentBill = MetaDaoHelper.findById(fullname, bizobject.getId());
            if(!BooleanUtils.b(currentBill.get("isWfControlled"))){
                currentBill.set("submitTime",new Date());
                currentBilles.add(currentBill);
            }
        }
        if(!CollectionUtils.isEmpty(currentBilles) && !StringUtils.isEmpty(fullname)){
            EntityTool.setUpdateStatus(currentBilles);
            MetaDaoHelper.update(fullname, currentBilles);
        }

        return new RuleExecuteResult();
    }
}
