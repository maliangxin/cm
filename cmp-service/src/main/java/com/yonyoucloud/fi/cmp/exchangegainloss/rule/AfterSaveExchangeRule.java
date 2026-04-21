package com.yonyoucloud.fi.cmp.exchangegainloss.rule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLossService;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss_b;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementServiceImpl;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

public class AfterSaveExchangeRule extends AbstractCommonRule {
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    SettlementServiceImpl settlementService;

    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Autowired
    ExchangeGainLossService exchangeGainLossService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            ExchangeGainLoss exchangeGainLoss = (ExchangeGainLoss) bills.get(0);
            String serverUrl = AppContext.getEnvConfig("fifrontservername");
            Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(exchangeGainLoss.getAccentity());
            if (enabledBeginData.compareTo(exchangeGainLoss.getVouchdate()) > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100655"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180481","单据日期早于模块启用日期，不能保存单据！") /* "单据日期早于模块启用日期，不能保存单据！" */);
            }
            Date currentDate = new Date();
            if (exchangeGainLoss.getVouchdate().compareTo(currentDate) > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101693"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180482","单据日期晚于当前日期,不能保存单据！") /* "单据日期晚于当前日期,不能保存单据！" */);
            }
            //校验日结日期
            Date maxSettleDate = settlementService.getMaxSettleDate(exchangeGainLoss.getAccentity());
            if (maxSettleDate != null) {
                if (maxSettleDate.compareTo(exchangeGainLoss.getVouchdate()) >= 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101694"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418047F","单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
                }
            }
            String natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(exchangeGainLoss.getAccentity());
            exchangeGainLoss.setNatCurrency(natCurrency);
            List<ExchangeGainLoss_b> exchangeGainLoss_bList = exchangeGainLoss.getBizObjects("exchangeGainLoss_b", ExchangeGainLoss_b.class);
            boolean saveState = false;
            List<Journal> journalList =  new ArrayList<Journal>();

            //根据汇率损益单生成日记账
            for (ExchangeGainLoss_b exchangeGainLoss_b :exchangeGainLoss_bList){
                exchangeGainLoss_b.setEntityStatus(EntityStatus.Insert);
                if(exchangeGainLoss_b.getAdjustbalance().compareTo(BigDecimal.ZERO)==0){
                    continue;
                }else{
                    saveState = true;
                }

                journalList.add(exchangeGainLossService.createJournalForAdd(exchangeGainLoss, exchangeGainLoss_b, billContext.getBillnum()));
            }
            if (!saveState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101695"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180480","该单据下所有差额均为0,重复计算,不允许保存单据！") /* "该单据下所有差额均为0,重复计算,不允许保存单据！" */);
            }
            for (Journal journal : journalList){
                cmpWriteBankaccUtils.addAccountBook(journal);
            }
            //生成相关凭证
            exchangeGainLoss.set("srcitem",EventSource.Cmpchase.getValue());
            exchangeGainLoss.set("billtype",EventType.ExchangeBill.getValue());
            CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResultTry(exchangeGainLoss);
            if (generateResult.getInteger("code") == 0 && !generateResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101696"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418047E","发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
            }
            Map<String, Object> params = new HashMap<>();
            if (generateResult.getInteger("code") == 1 && !generateResult.getBoolean("genVoucher")) {
                params.put("voucherstatus", VoucherStatus.NONCreate.getValue());
            }
            if (generateResult.getInteger("code") == 1 && generateResult.getBoolean("genVoucher")) {
                params.put("voucherstatus", VoucherStatus.Received.getValue());
            }
            if (generateResult.getInteger("code") == 2 && generateResult.getBoolean("dealSucceed")) {
                params.put("voucherstatus", VoucherStatus.POSTING.getValue());
            }

            params.put("tableName", IBillNumConstant.EXCHANG_EGAIN_LOSS);
            params.put("id", exchangeGainLoss.getId());
            SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatusReal", params);
        }
        return new RuleExecuteResult();
    }
}
