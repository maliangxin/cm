package com.yonyoucloud.fi.cmp.receivemargin.rule.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 收到保证金保存后规则*
 *
 * @author xuxbo
 * @date 2023/8/3 10:09
 */

@Slf4j
@Component
public class ReceiveMarginAfterSaveRule extends AbstractCommonRule {

    @Autowired
    @Qualifier("stwbReceiveMarginServiceImpl")
    private StwbBillService stwbBillService;

    @Autowired
    ReceiveMarginCommonServiceImpl receiveMarginCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        receiveMarginCommonService.execute(billContext,map);
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {

            if(ObjectUtils.isNotEmpty(bizObject.get("isConvert")) && bizObject.get("isConvert").equals(true)){
                return new RuleExecuteResult();
            }
            try {
                ReceiveMargin currentBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), 1);
                //如果结算标识是需要结算：推送结算(保存时 先进行校验)
                if (currentBill.getSettleflag() == 1) {
                    //推送资金结算
                    List<BizObject> currentBillList = new ArrayList<>();
                    currentBillList.add(currentBill);
                    stwbBillService.pushBill(currentBillList, true);// 推送资金结算 进行校验

                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102393"),e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }


}
