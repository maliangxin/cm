package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.model.MakeBillRule;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMultilangConstant;
import com.yonyoucloud.fi.cmp.constant.ITransferAccountBusinessConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * RPT0176 银行账单/认领生单类型拓展 支持生成同名账号划转账单
 */
@Slf4j
@Component
public class BankToTransferAccountBusinessAfterRule extends AbstractCommonRule {
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> tarList = (List) paramMap.get(ICmpConstant.TARLIST);
        MakeBillRule makeBillRule = (MakeBillRule) paramMap.get("makeBillRule");
        String code = makeBillRule.getCode();
        // 20241225-支持批量生单改造
        for (Map<String, Object> map : tarList) {
            this.handleTarMap(map, code);
        }

        //转单数据回填
        paramMap.put(ICmpConstant.TARLIST, tarList);
        return new RuleExecuteResult(paramMap);
    }

    private void handleTarMap(Map<String, Object> map, String code) throws Exception{
        Map<String, Object> sourceMap = new HashMap<>();
        if (this.isBankTransfer(code)) {
            sourceMap = this.getMap(ITransferAccountBusinessConstant.BANK_TO_BANK_TRANSFER.equals(code), map);
            this.handleBankTransfer(map, sourceMap);
        } else if (this.isStoreCash(code)) {
            sourceMap = this.getMap(ITransferAccountBusinessConstant.BANK_TO_STORE_CASH.equals(code), map);
            this.handleStoreCash(map, sourceMap);
        } else if (this.isExtractCash(code)) {
            sourceMap = this.getMap(ITransferAccountBusinessConstant.BANK_TO_EXTRACT_CASH.equals(code), map);
            this.handleExtractCash(map, sourceMap);
        } else if (this.isThirdPartyTransfer(code)) {
            sourceMap = this.getMap(ITransferAccountBusinessConstant.BANK_TO_THIRD_PARTY_TRANSFER.equals(code), map);
            this.handleThirdPartyTransfer(map, sourceMap);
        }

        //银行对账单生成转账单 业务处理-默认结算状态都是：已结算补单 （认领单单据转换规则未配置，都在这进行配置了）
        map.put(ICmpConstant.SETTLE_STATUS, SettleStatus.SettledRep.getValue());
        //设置默认支付状态为 已支付补单
        map.put(ICmpConstant.PAY_STATUS, PayStatus.SupplPaid.getValue());
        //设置默认入账类型
        if (map.get("entrytype") == null || StringUtils.isEmpty(String.valueOf(map.get("entrytype")))) {
            map.put("entrytype", EntryType.Normal_Entry.getValue());
        } else {
            //判断是否提前入账  如果是 则赋值 冲挂账 如果不是 则赋值 正常入账  sourceMap.get("isadvanceaccounts")
            if (!Objects.isNull(sourceMap.get("isadvanceaccounts")) && (boolean) sourceMap.get("isadvanceaccounts")) {
                map.put("entrytype", EntryType.CrushHang_Entry.getValue());
            }
        }
        //设置本币金额
        this.setNatSum(map);
        //设置来源单据
        this.setSrcBillInfo(map, sourceMap);
    }

    private boolean isThirdPartyTransfer(String code) {
        return ITransferAccountBusinessConstant.BANK_TO_THIRD_PARTY_TRANSFER.equals(code)
                || ITransferAccountBusinessConstant.BANK_TO_THIRD_PARTY_TRANSFER_CLAIM.equals(code);
    }

    private boolean isExtractCash(String code) {
        return ITransferAccountBusinessConstant.BANK_TO_EXTRACT_CASH.equals(code)
                || ITransferAccountBusinessConstant.BANK_TO_EXTRACT_CASH_CLAIM.equals(code);
    }

    private boolean isStoreCash(String code) {
        return ITransferAccountBusinessConstant.BANK_TO_STORE_CASH.equals(code)
                || ITransferAccountBusinessConstant.BANK_TO_STORE_CASH_CLAIM.equals(code);
    }

    private boolean isBankTransfer(String code) {
        return ITransferAccountBusinessConstant.BANK_TO_BANK_TRANSFER.equals(code)
                || ITransferAccountBusinessConstant.BANK_TO_BANK_TRANSFER_CLAIM.equals(code);
    }

    private Map<String, Object> getMap(boolean isBank, Map<String, Object> map) throws Exception {
        // sourceId一定有值，没有应该直接报错
        Map<String, Object> sourceMap = isBank ? this.getSourceMap(map.get("sourceId")) : this.getClaimSourceMap(map.get("sourceId"));
        // 用完后移除手动添加的id字段映射
        map.remove("sourceId");
        return sourceMap;
    }

    /**
     * 下推银行转账单
     *
     * @param map       转换后 目标map
     * @param sourceMap 原单据
     * @throws Exception
     */
    public void handleBankTransfer(Map<String, Object> map, Map<String, Object> sourceMap) throws Exception {
        //设置默认交易类型
        this.setTradetype(ITransferAccountBusinessConstant.YHZZ, map);
        //设置默认付款方结算方式
        this.setSettlemode(ITransferAccountBusinessConstant.SYSTEM_0001, map);
        //设置默认收款款方结算方式
        this.setCollectSettleMode(ITransferAccountBusinessConstant.SYSTEM_0001, map);
        map.put("type", ITransferAccountBusinessConstant.BT);

        String dc_flag = "";
        Map<String, Object> bankReconciliationMap = null;
        String billtype = String.valueOf(map.get("billtype"));
        if (StringUtils.equals(billtype, String.valueOf(EventType.CashMark.getValue()))) {
            dc_flag = String.valueOf(sourceMap.get("dc_flag"));
        } else if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
            dc_flag = String.valueOf(sourceMap.get("direction"));
            bankReconciliationMap = getBankReconciliation(sourceMap);
            //设置付款银行对账单ID
            sourceMap.put("to_acct_no", bankReconciliationMap.get("to_acct_no"));
            //设置银行对账码
            map.put("bankcheckcode", bankReconciliationMap.get("bankcheckno"));
        }

        //收入和支出逻辑处理
        if (StringUtils.equals(String.valueOf(DirectionJD.Credit.getValue()), dc_flag)) {
            //付款银行账户:当银行流水收支方向=收入，银行对账单/认领单.对方账号进行赋值，不可编辑,当银行流水收支方向=支出，银行对账单/认领单.本方银行账号进行赋值，不可编辑
            EnterpriseBankAcctVO enterpriseBankAcctVO = this.findEnterpriseBankNameByNo(sourceMap);
            map.put("payBankAccount", enterpriseBankAcctVO.getId());
            map.put("payBankAccount_name", enterpriseBankAcctVO.getName());
            //收款银行账户:当银行流水收支方向=收入，银行对账单/认领单.本方银行账号进行赋值，不可编辑，当银行流水收支方向=支出，银行对账单/认领单.对方账号进行赋值，不可编辑
            map.put("recBankAccount", sourceMap.get("bankaccount"));
            map.put("recBankAccount_name", this.findEnterpriseBankNameById(sourceMap.get("bankaccount")));
            if (StringUtils.equals(billtype, String.valueOf(EventType.CashMark.getValue()))) {
                //收款_银行对账单ID:收支方向=收入：依据银行对账单ID自动带入，不可编辑
                map.put("collectbankbill", sourceMap.get("id"));
                map.put("collectbankbill_bank_seq_no", sourceMap.get("bank_seq_no"));
            } else if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
                //收款_银行对账单ID:收支方向=收入：依据银行对账单ID自动带入，不可编辑。关联银行账单与关联认领单能且只能添加一个！
                map.put("collectbankbill", null);//单据转换规则设置了，这里清空优化
                map.put("collectbankbill_bank_seq_no", null);
                //银行流水认领-收支方向=收入：依据认领单ID自动带入，不可编辑
                map.put("collectbillclaim", sourceMap.get("id"));
                map.put("collectbillclaim_code", sourceMap.get("code"));
            }
        } else {
            map.put("payBankAccount", sourceMap.get("bankaccount"));
            map.put("payBankAccount_name", this.findEnterpriseBankNameById(sourceMap.get("bankaccount")));
            EnterpriseBankAcctVO enterpriseBankAcctVO = this.findEnterpriseBankNameByNo(sourceMap);
            map.put("recBankAccount", enterpriseBankAcctVO.getId());
            map.put("recBankAccount_name", enterpriseBankAcctVO.getName());
            if (StringUtils.equals(billtype, String.valueOf(EventType.CashMark.getValue()))) {
                //付款_银行对账单ID:收支方向=支出：依据银行对账单ID自动带入，不可编辑
                map.put("paybankbill", sourceMap.get("id"));
                map.put("paybankbill_bank_seq_no", sourceMap.get("bank_seq_no"));
            } else if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
                //付款_银行对账单ID:收支方向=支出：依据银行对账单ID自动带入，不可编辑。关联银行账单与关联认领单能且只能添加一个！
                map.put("paybankbill", null);
                map.put("paybankbill_bank_seq_no", null);
                //银行流水认领-收支方向=支出：依据认领单ID自动带入，不可编辑
                map.put("paybillclaim", sourceMap.get("id"));
                map.put("paybillclaim_code", sourceMap.get("code"));

            }

        }
        //设置付款_是否关联 当银行流水收支方向=收入，为否，不可编辑；付款对账单ID\付款认领单ID有值时，赋值为是. 当银行流水收支方向=支出，为是，不可编辑
        if (StringUtils.equals(String.valueOf(DirectionJD.Credit.getValue()), dc_flag)
                && map.get("paybankbill") == null
                && map.get("paybillclaim") == null) {
            map.put("associationStatusPay", 0);
        } else {
            map.put("associationStatusPay", 1);
        }
        //设置收款_是否关联 当银行流水收支方向=支出，为否，不可编辑；收款对账单ID\收款认领单ID有值时，赋值为是.当银行流水收支方向=收入，为是，不可编辑
        if (StringUtils.equals(String.valueOf(DirectionJD.Debit.getValue()), dc_flag)
                && map.get("collectbankbill") == null
                && map.get("collectbillclaim") == null) {
            map.put("associationStatusCollect", 0);
        } else {
            map.put("associationStatusCollect", 1);
        }


    }

    /**
     * 缴存现金单（收款流水）
     *
     * @param map       转换后 目标map
     * @param sourceMap 原单据
     * @throws Exception
     */
    public void handleStoreCash(Map<String, Object> map, Map<String, Object> sourceMap) throws Exception {
        //设置默认交易类型
        this.setTradetype(ITransferAccountBusinessConstant.JCXJ, map);
        //设置默认 付款方结算方式 现金收付款
        this.setSettlemode(ITransferAccountBusinessConstant.SYSTEM_0002, map);
        //设置默认 收款方结算方式 银行转账
        this.setCollectSettleMode(ITransferAccountBusinessConstant.SYSTEM_0001, map);
        map.put("type", ITransferAccountBusinessConstant.SC);
        //收款银行账户:银行流水认领/认领单.本方银行账号进行赋值，不可编辑
        map.put("recBankAccount", sourceMap.get("bankaccount"));
        map.put("recBankAccount_name", this.findEnterpriseBankNameById(sourceMap.get("bankaccount")));
        //设置收款_是否关联 默认为是，不可编辑
        map.put("associationStatusCollect", 1);
        //付款是否关联:默认为空，不可编辑
        map.put("associationStatusPay", null);
        //付款对账单ID:默认为空，不可编辑
        map.put("paybankbill", null);
        map.put("paybankbill_bank_seq_no", null);
        //收款是否关联:默认为是，不可编辑
        map.put("associationStatusCollect", 1);

        String billtype = String.valueOf(map.get("billtype"));
        if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
            Map<String, Object> bankReconciliationMap = this.getBankReconciliation(sourceMap);
            //设置银行对账码
            map.put("bankcheckcode", bankReconciliationMap.get("bankcheckno"));
            //收款_银行对账单ID:收支方向=收入：依据银行对账单ID自动带入，不可编辑。关联银行账单与关联认领单能且只能添加一个！
            map.put("collectbankbill", null);
            map.put("collectbankbill_bank_seq_no", null);
            //银行流水认领-收支方向=收入：依据认领单ID自动带入，不可编辑
            map.put("collectbillclaim", sourceMap.get("id"));
            map.put("collectbillclaim_code", sourceMap.get("code"));
        } else {
            //收款对账单ID:原有字段，依据关联的收款对账单自动赋值
            map.put("collectbankbill", sourceMap.get("id"));
            map.put("collectbankbill_bank_seq_no", sourceMap.get("bank_seq_no"));
        }

    }

    /**
     * 提取现金单（付款流水）
     *
     * @param map       转换后 目标map
     * @param sourceMap 原单据
     * @throws Exception
     */
    public void handleExtractCash(Map<String, Object> map, Map<String, Object> sourceMap) throws Exception {
        //设置默认交易类型
        this.setTradetype(ITransferAccountBusinessConstant.TQXJ, map);
        //设置默认 付款方结算方式
        this.setSettlemode(ITransferAccountBusinessConstant.SYSTEM_0001, map);
        //设置默认 收款方结算方式
        this.setCollectSettleMode(ITransferAccountBusinessConstant.SYSTEM_0002, map);
        map.put("type", ITransferAccountBusinessConstant.EC);
        //付款银行账户:银行流水认领/认领单.本方银行账号进行赋值，不可编辑
        map.put("payBankAccount", sourceMap.get("bankaccount"));
        map.put("payBankAccount_name", this.findEnterpriseBankNameById(sourceMap.get("bankaccount")));
        //付款是否关联:默认为是，不可编辑
        map.put("associationStatusPay", 1);

        //收款对账单ID:默认为空，不可编辑
        map.put("collectbankbill", null);
        map.put("collectbankbill_bank_seq_no", null);
        String billtype = String.valueOf(map.get("billtype"));
        if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
            Map<String, Object> bankReconciliationMap = this.getBankReconciliation(sourceMap);
            //设置银行对账码
            map.put("bankcheckcode", bankReconciliationMap.get("bankcheckno"));
            //付款_银行对账单ID:收支方向=支出：依据银行对账单ID自动带入，不可编辑。关联银行账单与关联认领单能且只能添加一个！
            map.put("paybankbill", null);
            map.put("paybankbill_bank_seq_no", null);
            //银行流水认领-收支方向=支出：依据认领单ID自动带入，不可编辑
            map.put("paybillclaim", sourceMap.get("id"));
            map.put("paybillclaim_code", sourceMap.get("code"));
        } else {
            //付款对账单ID:原有字段，依据关联的收款对账单自动赋值
            map.put("paybankbill", sourceMap.get("id"));
            map.put("paybankbill_bank_seq_no", sourceMap.get("bank_seq_no"));
        }
    }

    /**
     * 下推第三方转账单
     *
     * @param map       转换后 目标map
     * @param sourceMap 原单据
     * @throws Exception
     */
    public void handleThirdPartyTransfer(Map<String, Object> map, Map<String, Object> sourceMap) throws Exception {
        //设置默认交易类型
        this.setTradetype(ITransferAccountBusinessConstant.DSFZZ, map);
        map.put("type", ITransferAccountBusinessConstant.TPT);

        String dc_flag = "";
        String billtype = String.valueOf(map.get("billtype"));
        if (StringUtils.equals(billtype, String.valueOf(EventType.CashMark.getValue()))) {
            dc_flag = String.valueOf(sourceMap.get("dc_flag"));
        } else if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
            dc_flag = String.valueOf(sourceMap.get("direction"));
        }

        //收入
        if (StringUtils.equals(String.valueOf(DirectionJD.Credit.getValue()), dc_flag)) {
            //付款方结算方式-收入，为空，支持编辑
            map.put("settlemode", null);
            map.put("settlemode_name", null);
            //设置默认 收款方结算方式:当银行流水收支方向=收入，默认为system_0001：银行转账
            this.setCollectSettleMode(ITransferAccountBusinessConstant.SYSTEM_0001, map);
            //收款银行账户:当银行流水收支方向=收入，银行流水认领/认领单.本方银行账号进行赋值，不可编辑
            map.put("recBankAccount", sourceMap.get("bankaccount"));
            map.put("recBankAccount_name", this.findEnterpriseBankNameById(sourceMap.get("bankaccount")));
            //三方转账类型:当银行流水收支方向=收入，默认为虚拟账户转银行账户0；不可编辑
            map.put("virtualBank", VirtualBank.VirtualToBank.getValue());
            //付款是否关联:当银行流水收支方向=收入，为否，不可编辑；
            map.put("associationStatusPay", 0);
            //收款是否关联:当银行流水收支方向=收入，为是，不可编辑
            map.put("associationStatusCollect", 1);
            if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
                Map<String, Object> bankReconciliationMap = this.getBankReconciliation(sourceMap);
                //设置银行对账码
                map.put("bankcheckcode", bankReconciliationMap.get("bankcheckno"));
                //收款_银行对账单ID:收支方向=收入：依据银行对账单ID自动带入，不可编辑。关联银行账单与关联认领单能且只能添加一个！
                map.put("collectbankbill", null);
                map.put("collectbankbill_bank_seq_no", null);
                //银行流水认领-收支方向=收入：依据认领单ID自动带入，不可编辑
                map.put("collectbillclaim", sourceMap.get("id"));
                map.put("collectbillclaim_code", sourceMap.get("code"));
            } else {
                //收款_银行对账单ID:收支方向=收入：依据银行流水认领ID自动带入，不可编辑
                map.put("collectbankbill", sourceMap.get("id"));
                map.put("collectbankbill_bank_seq_no", sourceMap.get("bank_seq_no"));
            }

        } else {
            //设置默认 付款方结算方式-支出，默认为system_0001：银行转账，
            this.setSettlemode(ITransferAccountBusinessConstant.SYSTEM_0001, map);
            //收款方结算方式:当银行流水收支方向=支出，为空，
            map.put("collectsettlemode", null);
            map.put("collectsettlemode_name", null);
            //付款银行账户:当银行流水收支方向=支出，银行流水认领/认领单.本方银行账号进行赋值，不可编辑
            map.put("payBankAccount", sourceMap.get("bankaccount"));
            map.put("payBankAccount_name", this.findEnterpriseBankNameById(sourceMap.get("bankaccount")));
            //三方转账类型:当银行流水收支方向=支出，默认为银行账户转虚拟账户1；不可编辑
            map.put("virtualBank", VirtualBank.BankToVirtual.getValue());
            //付款是否关联:当银行流水收支方向=支出，为是，不可编辑
            map.put("associationStatusPay", 1);
            //收款是否关联:当银行流水收支方向=支出，为否，不可编辑；
            map.put("associationStatusCollect", 0);
            if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
                Map<String, Object> bankReconciliationMap = this.getBankReconciliation(sourceMap);
                //设置银行对账码
                map.put("bankcheckcode", bankReconciliationMap.get("bankcheckno"));
                //付款_银行对账单ID:收支方向=支出：依据银行对账单ID自动带入，不可编辑。关联银行账单与关联认领单能且只能添加一个！
                map.put("paybankbill", null);
                map.put("paybankbill_bank_seq_no", null);
                //银行流水认领-收支方向=支出：依据认领单ID自动带入，不可编辑
                map.put("paybillclaim", sourceMap.get("id"));
                map.put("paybillclaim_code", sourceMap.get("code"));
            } else {
                //付款_银行对账单ID:收支方向=支出：依据银行流水认领ID自动带入，不可编辑
                map.put("paybankbill", sourceMap.get("id"));
                map.put("paybankbill_bank_seq_no", sourceMap.get("bank_seq_no"));
            }

        }
    }

    /**
     * 设置本币金额
     *
     * @param map
     * @throws Exception
     */
    private void setNatSum(Map<String, Object> map) throws Exception {
        String accentity = map.get(MerchantConstant.ACCENTITY).toString();
        String currency = map.get(MerchantConstant.CURRENCY).toString();
        //币种精度
        CurrencyTenantDTO currencyDTO = this.findCurrency(currency);
        map.put("currency_name", currencyDTO.getName());
        map.put("currency_priceDigit", currencyDTO.getPricedigit());
        map.put("currency_moneyDigit", currencyDTO.getMoneydigit());
        map.put("currency_moneyRount", currencyDTO.getMoneyrount());
        //汇率类型
        ExchangeRateTypeVO exchangeRateTypeVO = CmpExchangeRateUtils.getNewExchangeRateType(accentity, true);
        if (exchangeRateTypeVO.getId() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100041"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418054D", "此会计主体下无默认汇率类型，请检查数据！") /* "此会计主体下无默认汇率类型，请检查数据！" */);
        }
        //汇率类型 设置
        String exchangeRateType = exchangeRateTypeVO.getId();
        Integer digit = exchangeRateTypeVO.getDigit();
        map.put("exchangeRateType", exchangeRateType);
        map.put("exchangeRateType_digit", digit);
        map.put("exchangeRateType_code", exchangeRateTypeVO.getCode());
        map.put("exchangeRateType_name", exchangeRateTypeVO.getName());

        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
        String natCurrency = finOrgDTO.getCurrency();
        //财务组织币种
        CurrencyTenantDTO currencyNatDTO = this.findCurrency(natCurrency);
        if (null == currencyNatDTO) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100681"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D1", "本币币种不能为空！") /* "本币币种不能为空！" */);
        }
        //汇率（取汇率表中报价日期小于等于单据日期的值）
        if (currency.equals(natCurrency)) {
            map.put(NATSUM, map.get(ORISUM));
            map.put(EXCHRATE, new BigDecimal("1"));
            map.put(TRANSFER_ACCOUNT_EXCHRATEOPS, (short) 1);
            // 怎么赋值汇率折算方式
        } else {
            Double currencyRateNew = CurrencyUtil.getCurrencyRateNew((String) null, exchangeRateType, currency, natCurrency, (Date) map.get(VOUCHDATE), digit);
            if (currencyRateNew == null || currencyRateNew == 0.0d) {
                throw IMultilangConstant.noRateError/*未取到汇率 */;
            }
            CmpExchangeRateVO exchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode(currency, natCurrency, (Date) map.get(VOUCHDATE), exchangeRateType, digit);
            if (BigDecimal.ZERO.equals(exchangeRateWithMode.getExchangeRate())) {
                throw IMultilangConstant.noRateError/*未取到汇率 */;
            }
            //设置本币金额
            map.put(EXCHRATE, exchangeRateWithMode.getExchangeRate());
            map.put(TRANSFER_ACCOUNT_EXCHRATEOPS, exchangeRateWithMode.getExchangeRateOps());
            map.put(NATSUM, CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangeRateWithMode.getExchangeRateOps(), exchangeRateWithMode.getExchangeRate(), new BigDecimal(map.get(ORISUM).toString()), null)
                    .setScale(currencyNatDTO.getMoneydigit(), currencyNatDTO.getMoneyrount()));
        }

    }

    /**
     * 设置来源单据信息
     *
     * @param map
     * @throws Exception
     */
    private void setSrcBillInfo(Map<String, Object> map, Map<String, Object> sourceMap) throws Exception {
        String billtype = String.valueOf(map.get("billtype"));
        //来源单据id
        map.put("srcbillid", sourceMap.get("id"));
        if (StringUtils.equals(billtype, String.valueOf(EventType.CashMark.getValue()))) {
            //来源单据号 银行交易流水号
            map.put("srcbillno", sourceMap.get("bank_seq_no"));
        } else if (StringUtils.equals(billtype, String.valueOf(EventType.BillClaim.getValue()))) {
            //来源单据号
            map.put("srcbillno", sourceMap.get("code"));
        }
    }

    /**
     * 查询企业银行名称
     *
     * @param accountId 主键
     * @return
     * @throws Exception
     */
    private String findEnterpriseBankNameById(Object accountId) throws Exception {
        if (accountId == null || StringUtils.isEmpty(accountId.toString())) {
            return null;
        }
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(String.valueOf(accountId));
        if (enterpriseBankAcctVO == null) {
            return null;
        }
        return enterpriseBankAcctVO.getName();
    }

    /**
     * 查询企业银行名称
     *
     * @param sourceMap
     * @return
     * @throws Exception
     */
    private EnterpriseBankAcctVO findEnterpriseBankNameByNo(Map<String, Object> sourceMap) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setOrgid((String) sourceMap.get(MerchantConstant.ACCENTITY));
        if (StringUtils.isNotEmpty((String) sourceMap.get("to_acct_no"))) {
            enterpriseParams.setAccount((String) sourceMap.get("to_acct_no"));
        } else if (StringUtils.isNotEmpty((String) sourceMap.get("to_acct_name"))) {
            enterpriseParams.setAcctName((String) sourceMap.get("to_acct_name"));
        } else {
            return new EnterpriseBankAcctVO();
        }
        List<EnterpriseBankAcctVO> enterpriseBankAccounts = enterpriseBankQueryService.query(enterpriseParams);
        if (org.springframework.util.CollectionUtils.isEmpty(enterpriseBankAccounts)) {
            return new EnterpriseBankAcctVO();
        }
        if (enterpriseBankAccounts.size() > 1) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540078E", "查询企业银行重复，请确认！to_acct_no:") /* "查询企业银行重复，请确认！to_acct_no:" */ + sourceMap.get("to_acct_no") + ",to_acct_name" + sourceMap.get("to_acct_name"));
        }
        return enterpriseBankAccounts.get(0);
    }

    /**
     * 查询币种
     *
     * @param currency
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO findCurrency(String currency) throws Exception {
        CurrencyTenantDTO currencyDTO = null;
        String locale = InvocationInfoProxy.getLocale();
        String currencyKey = currency.concat(AppContext.getYhtTenantId()).concat("DEPOSITINTERESTWITHHOLDINGAFTERQUERYRULE").concat(locale);
        if (null != AppContext.cache().getObject(currencyKey)) {
            currencyDTO = AppContext.cache().getObject(currencyKey);
        } else {
            currencyDTO = baseRefRpcService.queryCurrencyById(currency);
            if (null != currencyDTO) {
                AppContext.cache().setObject(currencyKey, currencyDTO);
            }
        }
        return currencyDTO;
    }


    /**
     * 设置付款方结算方式
     *
     * @param bizobject
     */
    public void setSettlemode(String systemcode, Map<String, Object> bizobject) {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCode(systemcode);
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        if (!CollectionUtils.isEmpty(dataList)) {
            SettleMethodModel settlementWay = dataList.get(0);
            bizobject.put("settlemode", settlementWay.getId());
            String locale = InvocationInfoProxy.getLocale();
            switch (locale) {
                case "zh_CN":
                    bizobject.put("settlemode_name", settlementWay.getName());
                    break;
                case "en_US":
                    bizobject.put("settlemode_name", settlementWay.getName2());
                    break;
                case "zh_TW":
                    bizobject.put("settlemode_name", settlementWay.getName3());
                    break;
                default:
                    bizobject.put("settlemode_name", settlementWay.getName());
            }
        }
    }

    /**
     * 设置收款款方结算方式
     *
     * @param bizobject
     */
    public void setCollectSettleMode(String systemcode, Map<String, Object> bizobject) {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCode(systemcode);
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        if (!CollectionUtils.isEmpty(dataList)) {
            SettleMethodModel settlementWay = dataList.get(0);
            bizobject.put("collectsettlemode", settlementWay.getId());
            String locale = InvocationInfoProxy.getLocale();
            switch (locale) {
                case "zh_CN":
                    bizobject.put("collectsettlemode_name", settlementWay.getName());
                    break;
                case "en_US":
                    bizobject.put("collectsettlemode_name", settlementWay.getName2());
                    break;
                case "zh_TW":
                    bizobject.put("collectsettlemode_name", settlementWay.getName3());
                    break;
                default:
                    bizobject.put("collectsettlemode_name", settlementWay.getName());
            }
        }
    }

    /**
     * 设置默认交易类型
     * {"billtype_id":"FICA4","dr":0,"enable":1,"extend_attrs_json":"jcxj"}
     * {
     * "code": 200,
     * "message": "操作成功",
     * "data": [
     * {
     * "extend_attrs_json": "{\"transferType_zz\":\"jcxj\"}",
     * "memo": "缴存现金",
     * "default": "0",
     * "enable": 1,
     * "id": "1877480546296135745",
     * "code": "SC",
     * "name": "缴存现金",
     * "printAfterApproval": 0,
     * "billtype_id": "FICA4"
     * }
     * ],
     * "traceId": "cfe2d0a5783a2963"
     * }
     *
     * @param extend_attrs_json FICA4 缴存现金,jcxj,银行转账 FICA4,yhzz,提取现金 FICA4，tqxj，第三方转账 FICA4，dsfzz
     */
    public void setTradetype(String extend_attrs_json, Map<String, Object> bizobject) throws Exception {
        Map<String, Object> condition = new HashMap<>(4);
        condition.put("extend_attrs_json", extend_attrs_json);
        condition.put("billtype_id", "FICA4");
        condition.put("dr", 0);
        condition.put("enable", 1);
        List<Map<String, Object>> list = cmCommonService.getTransTypeByCondition(condition);
        if (!CollectionUtils.isEmpty(list)) {
            bizobject.put("tradetype", list.get(0).get("id"));
            bizobject.put("tradetype_name", list.get(0).get("name"));
            bizobject.put("tradetype_code", list.get(0).get("code"));
        }
    }

    /**
     * 银行对账单-> 转账单,
     *
     * @param sourceId 单据id
     * @return 银行对账单实体
     * @throws Exception 异常抛出
     */
    private Map<String, Object> getSourceMap(Object sourceId) throws Exception {
        List<Map<String, Object>> bankReconciliation = MetaDaoHelper.queryById(BankReconciliation.ENTITY_NAME, "*", sourceId);
        Map<String, Object> sourceMap = bankReconciliation.get(0);
        return sourceMap;
    }

    /**
     * 认领单-> 转账单,
     *
     * @param sourceId 单据id
     * @return 银行对账单实体
     * @throws Exception 异常抛出
     */
    private Map<String, Object> getClaimSourceMap(Object sourceId) throws Exception {
        BizObject bizObject = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, sourceId, 2);
        return bizObject;
    }

    /**
     * 根据认领单查询银行对账单
     *
     * @param sourceMap
     * @return 银行对账单
     * @throws Exception
     */
    private Map<String, Object> getBankReconciliation(Map<String, Object> sourceMap) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(sourceMap.get("id")));
        QuerySchema schema = QuerySchema.create().addSelect("bankbill");
        schema.addCondition(group);
        List<Map<String, Object>> list = MetaDaoHelper.query(BillClaimItem.ENTITY_NAME, schema);
        if (!org.springframework.util.CollectionUtils.isEmpty(list)) {
            BizObject bizObject = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, list.get(0).get("bankbill"));
            if (null != bizObject) {
                return bizObject;
            }
        }
        return new HashMap<>();
    }
}
