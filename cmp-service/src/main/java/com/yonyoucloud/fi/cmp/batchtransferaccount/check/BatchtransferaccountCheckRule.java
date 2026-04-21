package com.yonyoucloud.fi.cmp.batchtransferaccount.check;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author xuxbo
 * @date 2025/6/5 16:22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BatchtransferaccountCheckRule extends AbstractCommonRule {

    private final BaseRefRpcService baseRefRpcService;
    private final CmCommonService cmCommonService;
    private final TransTypeQueryService transTypeQueryService;
    private final EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        BatchTransferAccount batchTransferAccount = (BatchTransferAccount) getBills(billContext, paramMap).get(0);
        // 资金组织
        accentityNameCheck(batchTransferAccount, item);
        // 票证号
        noteNoCheck(batchTransferAccount, item);
        this.putParam(paramMap, "return", batchTransferAccount);
        return new RuleExecuteResult();
    }

    /**
     * 票证号Check规则
     * @param batchTransferAccount
     * @param item
     */
    private void noteNoCheck(BatchTransferAccount batchTransferAccount, CtmJSONObject item) throws Exception {
        if (!"noteno".equals(item.get("key"))) {
            return;
        }
        // 交易类型
        BdTransType bdTransType = transTypeQueryService.findById(batchTransferAccount.getTradeType());
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeTypeCode = jsonObject.getString("batchtransferType_ext");
        List<BatchTransferAccount_b> batchTransferAccountBList = batchTransferAccount.BatchTransferAccount_b();
        // 交易类型是现金支取
        if (!"EC".equals(batchTransferAccount.get("tradeType_code")) && !"tqxj".equals(tradeTypeCode)) {
            return;
        }
        for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccountBList) {
            // 付款结算方式非支票业务直接返回
            if (!"8".equals(batchTransferAccountB.get("paySettlemode_serviceAttr") + "")) {
                continue;
            }
            // 处理清空支票Id的场景
            handleNullNoteId(batchTransferAccountB);
            // 处理选择支票Id的场景
            handleNotNullNoteId(batchTransferAccountB);

        }
    }

    /**
     * 清空支票的联动信息
     * @param batchTransferAccountB
     */
    private void handleNullNoteId(BatchTransferAccount_b batchTransferAccountB) {
        if (batchTransferAccountB.getNoteId() != null) {
            return;
        }
        // 票证id
        batchTransferAccountB.setNoteId(null);
        // 票证号
        batchTransferAccountB.setNoteno(null);
        // 票证方向
        batchTransferAccountB.setNoteDirect(null);
        // 付款银行账户Id
        batchTransferAccountB.setPayBankAccountId(null);
        // 银行账号
        batchTransferAccountB.set("payBankAccountId_account", null);
        // 账户名称
        batchTransferAccountB.set("payBankAccountId_name", null);
        // 账户编码
        batchTransferAccountB.set("payBankAccountId_code", null);
        // 付款方开户行id
        batchTransferAccountB.setPayBankId(null);
        // 付款方开户行名称
        batchTransferAccountB.set("payBankName", null);
        // 付款方开户行联行号
        batchTransferAccountB.setPayLineNumber(null);
        // 付款银行类别Id
        batchTransferAccountB.setPayBankTypeId(null);
        // 付款方银行类别名称
        batchTransferAccountB.set("payBankTypeId_name", null);
    }

    /**
     * 支票联动付款方信息
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void handleNotNullNoteId(BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (batchTransferAccountB.getNoteId() == null) {
            return;
        }
        //查最新的支票数据
        CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, batchTransferAccountB.getNoteId());
        // 票证id
        batchTransferAccountB.setNoteId(checkStock.getId());
        // 票证号
        batchTransferAccountB.setNoteno(checkStock.getCheckBillNo());
        // 票证方向
        batchTransferAccountB.setNoteDirect(checkStock.getCheckBillDir());
        // 付款银行账户Id
        batchTransferAccountB.setPayBankAccountId(checkStock.getDrawerAcct());
        // 查询企业银行账户
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(batchTransferAccountB.getPayBankAccountId());
        // 银行账号
        batchTransferAccountB.set("payBankAccountId_account", enterpriseBankAcctVO.getAccount());
        // 账户名称
        batchTransferAccountB.set("payBankAccountId_name", enterpriseBankAcctVO.getName());
        // 账户编码
        batchTransferAccountB.set("payBankAccountId_code", enterpriseBankAcctVO.getCode());
        // 付款方开户行id
        batchTransferAccountB.setPayBankId(enterpriseBankAcctVO.getBankNumber());
        // 付款方开户行名称
        batchTransferAccountB.set("payBankName", enterpriseBankAcctVO.getBankNumberName());
        // 付款方开户行联行号
        batchTransferAccountB.setPayLineNumber(enterpriseBankAcctVO.getLineNumber());
        // 付款银行类别Id
        batchTransferAccountB.setPayBankTypeId(enterpriseBankAcctVO.getBank());
        // 付款方银行类别名称
        batchTransferAccountB.set("payBankTypeId_name", enterpriseBankAcctVO.getBankName());
    }

    /**
     * 资金组织Check规则
     * @param batchTransferAccount
     * @param item
     * @throws Exception
     */
    private void accentityNameCheck(BatchTransferAccount batchTransferAccount, CtmJSONObject item) throws Exception {
        if (!"accentity_name".equals(item.get("key"))) {
            return;
        }
        // 处理清空资金组织的场景
        handleNullAccentityName(batchTransferAccount);
        // 处理选择资金组织的场景
        handleNotNullAccentityName(batchTransferAccount);

    }

    /**
     * 清空资金组织清空联动信息
     * @param batchTransferAccount
     */
    private void handleNullAccentityName(BatchTransferAccount batchTransferAccount) {
        if (batchTransferAccount.getAccentity() != null) {
            return;
        }
        batchTransferAccount.set("org", null);
        batchTransferAccount.set("org_name", null);
        batchTransferAccount.setCurrency(null);
        batchTransferAccount.set("currency_name", null);
        batchTransferAccount.set("currency_code", null);
        batchTransferAccount.set("currency_priceDigit", null);
        batchTransferAccount.set("currency_moneyDigit", null);
        batchTransferAccount.setNatCurrency(null);
        batchTransferAccount.set("natCurrency_name", null);
        batchTransferAccount.set("natCurrency_priceDigit", null);
        batchTransferAccount.set("natCurrency_moneyDigit", null);
        batchTransferAccount.setExchRate(null);
        batchTransferAccount.setExchRateOps(null);
        batchTransferAccount.setExchangeRateType(null);
        batchTransferAccount.set("exchangeRateType_name", null);
        batchTransferAccount.set("exchangeRateType_digit", null);
        batchTransferAccount.set("exchangeRateType_code", null);
    }

    /**
     * 资金组织选择值后联动
     * @param batchTransferAccount
     * @throws Exception
     */
    private void handleNotNullAccentityName(BatchTransferAccount batchTransferAccount) throws Exception {
        String accEntityId = batchTransferAccount.getAccentity();
        if (accEntityId == null) {
            return;
        }
        batchTransferAccount.set("org", accEntityId);
        batchTransferAccount.set("org_name", batchTransferAccount.get("accentity_name"));

        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);

        try {
            Date periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accEntityId);
            if (periodFirstDate == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100766"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180483", "模块未启用") /* "模块未启用" */);
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100766"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180483", "模块未启用") /* "模块未启用" */);
        }

        batchTransferAccount.setBillTypeId("2283268970412769285");
        batchTransferAccount.set("billTypeId_busibilltype", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400731", "同名账户批量划转") /* "同名账户批量划转" */);
        batchTransferAccount.set("ebiz_obj_type", BatchTransferAccount.ENTITY_NAME);

        try {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
            if (currencyTenantDTO != null) {
                // 设置币种
                batchTransferAccount.setCurrency(currencyTenantDTO.getId());
                batchTransferAccount.set("currency_name", currencyTenantDTO.getName());
                batchTransferAccount.set("currency_code", currencyTenantDTO.getCode());
                batchTransferAccount.set("currency_priceDigit", currencyTenantDTO.getPricedigit());
                batchTransferAccount.set("currency_moneyDigit", currencyTenantDTO.getMoneydigit());
                batchTransferAccount.setNatCurrency(currencyTenantDTO.getId());
                batchTransferAccount.set("natCurrency_name", currencyTenantDTO.getName());
                batchTransferAccount.set("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                batchTransferAccount.set("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
                batchTransferAccount.setExchRate(new BigDecimal("1"));
                batchTransferAccount.setExchRateOps((short) 1);
            }
        } catch (Exception e) {
            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18D4A5DC05400008","未取到本币币种！ e = {}") /* "未取到本币币种！ e = {}" */, e.getMessage());
        }

        // 会计主体设置汇率类型
        ExchangeRateTypeVO exchangeRateTypeVO = CmpExchangeRateUtils.getNewExchangeRateType(accEntityId, true);
        if (StringUtils.isNotEmpty(exchangeRateTypeVO.getId())) {
            batchTransferAccount.setExchangeRateType(exchangeRateTypeVO.getId());
            batchTransferAccount.set("exchangeRateType_name", exchangeRateTypeVO.getName());
            batchTransferAccount.set("exchangeRateType_digit", exchangeRateTypeVO.getDigit());
            batchTransferAccount.set("exchangeRateType_code", exchangeRateTypeVO.getCode());
        }
    }


}
