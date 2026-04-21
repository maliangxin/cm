package com.yonyoucloud.fi.cmp.billclaim.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.billclaim.service.BillClaimService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @description: 我的认领页面，批量取消认领规则
 *  rule action值为：releaseBody
 * @author: wanxbo@yonyou.com
 * @date: 2022/4/21 10:27
 */

@Slf4j
@Component
public class MyClaimBatchCancelRule extends AbstractCommonRule {

    @Resource
    private BillClaimService billClaimService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        //系统批量异步，每次一条数据
        if (bills != null && bills.size()>0) {
            Long id = bills.get(0).getId();
            if (!ValueUtils.isNotEmptyObj(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101291"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418051C","操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
            }
            CtmJSONObject result = billClaimService.cancelClaim(id);
            if(!result.getBoolean("dealSucceed")){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101292"),result.getString(ICmpConstant.MSG));
            }
        }
        return new RuleExecuteResult();
    }
}
