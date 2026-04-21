package com.yonyoucloud.fi.cmp.journal.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by xudya on 2019/6/11 .
 */
@Component
public class JournalDeleteRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size()>0) {
            Journal bizObject = (Journal) bills.get(0);
            JedisLockUtils.isexistRjLock(bizObject.get("accentity"));
            JedisLockUtils.isexistDzLock(bizObject.getAccentity()+bizObject.getBankaccount());
            if(ObjectUtils.isNotEmpty(bizObject.getCheckflag())&&bizObject.getCheckflag()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102547"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A6","该单据已勾对完成，不能删除单据！") /* "该单据已勾对完成，不能删除单据！" */);
            }
        }
        return new RuleExecuteResult();
    }

}
