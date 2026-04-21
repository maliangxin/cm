package com.yonyoucloud.fi.cmp.exchangegainloss.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementServiceImpl;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DeleteExchangeRule extends AbstractCommonRule {
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    SettlementServiceImpl settlementService;
    @Autowired
    CmCommonService cmCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            ExchangeGainLoss exchangeGainLoss = (ExchangeGainLoss) bills.get(0);
            //校验日结日期
            Date maxSettleDate = settlementService.getMaxSettleDate(exchangeGainLoss.getAccentity());
            if (maxSettleDate != null) {
                if (maxSettleDate.compareTo(exchangeGainLoss.getVouchdate()) >= 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101206"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180516","该单据已日结，不能修改或删除单据！") /* "该单据已日结，不能修改或删除单据！" */);
                }
            }
            BizObject biz = MetaDaoHelper.findById(ExchangeGainLoss.ENTITY_NAME, exchangeGainLoss.getId());
            if (CmpCommonUtil.getNewFiFlag()) {
                //判断是否存在红冲单据
                if(!biz.getBoolean("isCover") && !StringUtils.isEmpty(biz.getString("associationid"))){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101207"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1852E4FE0590000C", "该单据存在红冲单据，请先删除对应的红冲单据。") /* "该单据存在红冲单据，请先删除对应的红冲单据。" */);
                }
                if(biz.getBoolean("isCover") && ValueUtils.isNotEmptyObj(biz.get("associationid"))){
                    ExchangeGainLoss exchangeGainLossAss = new ExchangeGainLoss();
                    exchangeGainLossAss.setId(biz.get("associationid"));
                    exchangeGainLossAss.setAssociationcode(null);
                    exchangeGainLossAss.setAssociationid(null);
                    EntityTool.setUpdateStatus(exchangeGainLossAss);
                    MetaDaoHelper.update(ExchangeGainLoss.ENTITY_NAME,exchangeGainLossAss);
                }
            }

            //删除日记账
            CmpWriteBankaccUtils.delAccountBook(exchangeGainLoss.getId().toString());


            if (ValueUtils.isNotEmptyObj(biz.get("voucherstatus"))) {
                exchangeGainLoss.put("voucherstatus", biz.get("voucherstatus"));
            }
            exchangeGainLoss.set("srcitem", EventSource.Cmpchase.getValue());
            exchangeGainLoss.set("billtype", EventType.ExchangeBill.getValue());
            exchangeGainLoss.set("_entityName", billContext.getFullname());
            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(exchangeGainLoss);
            if (!deleteResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101208"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180515","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
            }
        }
        return new RuleExecuteResult();
    }

}
