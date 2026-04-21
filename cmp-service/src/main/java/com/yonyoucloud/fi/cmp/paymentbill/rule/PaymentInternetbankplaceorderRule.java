package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 付款工作台 结算-网银预下单 异步批量处理规则
 * 目前应该无使用，无效使用
 * @author: wanxbo@yonyou.com
 * @date: 2022/6/9 11:03
 */
@Component
@Slf4j
public class PaymentInternetbankplaceorderRule extends AbstractCommonRule {

    @Autowired
    PaymentService paymentService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103023"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2591C04E00002", "在财务新架构环境下，不允许网银预下单。") /* "在财务新架构环境下，不允许网银预下单。" */);
        }
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size()>0) {
            for (BizObject bizobject : bills){
                CtmJSONObject params = CtmJSONObject.parseObject(JsonUtils.toJson(bizobject));
                CtmJSONObject row = CtmJSONArray.parseArray(params.getString("rows")).getJSONObject(0);
                Long id = row.getLong("id");
                if (!ValueUtils.isNotEmptyObj(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101739"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800F6","操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
                }
                String key = PayBill.ENTITY_NAME + id;
                YmsLock ymsLock = CtmLockTool.tryGetLockInOneService(key);
                //单据加pk锁，在外层controller中解锁
                try {
                    if (null == ymsLock) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + row.getString("code") +
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BB","】被锁定，请勿重复操作") /* "】被锁定，请勿重复操作" */);
                    }
                    params.put("lockKey",key);
                    paymentService.internetBankPlaceOrder(params);
                }catch (Exception e){
                    log.error(e.getMessage(), e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101741"),e.getMessage());
                }finally {
                    //网银预下单有加锁动作，要解锁
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
