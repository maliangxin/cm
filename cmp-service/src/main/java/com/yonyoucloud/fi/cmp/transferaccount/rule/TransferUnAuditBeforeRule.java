package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.error.CtmErrorCode;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.transferaccount.service.ITransferAccountPushService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 撤回审批 前规则 28
 */
@Component
@Slf4j
public class TransferUnAuditBeforeRule extends AbstractCommonRule {

    @Autowired
    private ITransferAccountPushService iTransferAccountPushService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        // 查询数据库 是否推结算的字段
        BizObject contextData = bills.get(0);
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, contextData.getId());
        if (transferAccount.getIsSettlement() == null || !transferAccount.getIsSettlement()) {
            //返回
            return new RuleExecuteResult();
        }
        //2。调用待结算数据删除接口
        //拼装参数
        if(log.isInfoEnabled()) {
            log.info("删除转账单数据bills:{}", CtmJSONObject.toJSONString(bills));
        }
        iTransferAccountPushService.deleteBill(bills);
        //待结算删除成功之后需要把 首次已处理 修改成 0
        bills.get(0).set("isfirsthandler",(short) 0);
        //3。end
        return new RuleExecuteResult();
    }
}
