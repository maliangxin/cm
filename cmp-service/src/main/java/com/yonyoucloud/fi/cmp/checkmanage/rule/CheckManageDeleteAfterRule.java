package com.yonyoucloud.fi.cmp.checkmanage.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置删除后规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:07
 */
@Component
@Slf4j
public class CheckManageDeleteAfterRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<CheckManage> bills = getBills(billContext, map);
        List<CheckManageDetail> listb = bills.get(0).CheckManageDetail();
/*        for (CheckManageDetail checkManageDetail : listb) {
            // 针对删除的支票，恢复支票工作台支票状态为处置前支票状态，同时清空处置前支票状态
            CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME,checkManageDetail.getCheckid());
            CheckManageDetail checkManageDetailQuery = MetaDaoHelper.findById(CheckManageDetail.ENTITY_NAME, checkManageDetail.getId());
            checkOne.setCheckBillStatus(checkManageDetailQuery.getCheckBillStatus()); // 支票处置子表第一次保存时的支票状态
            EntityTool.setUpdateStatus(checkOne);
            MetaDaoHelper.update(CheckStock.ENTITY_NAME,checkOne);
        }*/
        return new RuleExecuteResult();
    }
}
