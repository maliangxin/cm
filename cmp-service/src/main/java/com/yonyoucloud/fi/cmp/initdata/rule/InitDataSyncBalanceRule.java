package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.initdata.service.InitDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description:【期初余额同步】按钮，支持批量获取期初余额
 * 可获取到时，默认赋值“银行方期初余额”为该账户余额；可获取到且账户仅授权给一个组织，则默认赋值“企业方期初余额”也为该账户余额；*
 * 可获取到账户授权给多个组织时，则不默认赋值“企业方期初余额”
 * 获取余额后，企业方期初本币余额计算逻辑同原逻辑一致
 * @author: wanxbo@yonyou.com
 * @date: 2025/8/28 14:31
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class InitDataSyncBalanceRule extends AbstractCommonRule {

    @Autowired
    private InitDataService initDataService ;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size()>0) {
            for (BizObject bizobject : bills){
                initDataService.syncInitBalance(bizobject);
            }
        }
        return new RuleExecuteResult();
    }
}
