package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.business;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;

import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.stwbentity.EntryTypeEnum;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.VirtualBank;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.common.digitalwallet.impl.BatchTransferAccountWalletHandler;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.transferaccount.util.BaseDocUtils;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同名账户批量划转保存前规则
 * @author xuxbo
 * @date 2025/6/5 13:55
 */
@Slf4j
@Component
public class BatchtransferaccountBeforeSaveRule extends AbstractCommonRule {


    @Autowired
    private TransTypeQueryService transTypeQueryService;
    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Autowired
    @Qualifier("batchTransferAccountWalletHandler")
    private BatchTransferAccountWalletHandler batchTransferAccountWalletHandler;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetBatchTransferAccountManagerService btaCmpBudgetManagerService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

        try {
            List<BizObject> bills = getBills(billContext, paramMap);
            BatchTransferAccount batchTransferAccount = (BatchTransferAccount) bills.get(0);
            BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
            // 导入标志
            boolean importFlag =  "import".equals(billDataDto.getRequestAction());
            // 校验参数
            checkParam(batchTransferAccount, importFlag);
            // 填充主表参数
            fillBatchTransferAccount(batchTransferAccount);
            // 填充主表参数导入
            fillBatchTransferAccountImport(batchTransferAccount, importFlag);
            // 交易类型
            BdTransType bdTransType = transTypeQueryService.findById(batchTransferAccount.getTradeType());
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
            String tradeTypeCode = jsonObject.getString("batchtransferType_ext");
            // 转账金额合计
            BigDecimal totalAmount = BigDecimal.ZERO;
            // 转账金额本币合计
            BigDecimal natTotalAmount = BigDecimal.ZERO;
            // 转入手续费合计
            BigDecimal totalInBrokerage = BigDecimal.ZERO;
            // 转入手续费本币合计
            BigDecimal natTotalInBrokerage = BigDecimal.ZERO;
            // 转出手续费合计
            BigDecimal totalOutBrokerage = BigDecimal.ZERO;
            // 转出手续费本合计
            BigDecimal natTotalOutBrokerage = BigDecimal.ZERO;
            // 主子表金额校验
            List<BatchTransferAccount_b> billbs = batchTransferAccount.BatchTransferAccount_b();
            BatchTransferAccount_b firstBatchTransferAccountB = billbs.get(0);
            for (BatchTransferAccount_b batchTransferAccountB : billbs) {
                if("Delete".equals(batchTransferAccountB.getEntityStatus().name())){
                    continue;
                }
                // 原币
                BigDecimal oriSum = batchTransferAccountB.getOriSum();
                // 本币
                if (importFlag) {
                    batchTransferAccountB.setNatSum(currencyQueryService.getAmountOfCurrencyPrecision(batchTransferAccount.getNatCurrency(), oriSum));
                    natTotalAmount = natTotalAmount.add(batchTransferAccountB.getNatSum());
                    // 转入手续费
                    if (batchTransferAccountB.getInBrokerage() != null) {
                        batchTransferAccountB.setNatInBrokerage(currencyQueryService.getAmountOfCurrencyPrecision(batchTransferAccount.getNatCurrency(), batchTransferAccountB.getInBrokerage()));
                        totalInBrokerage = totalInBrokerage.add(batchTransferAccountB.getInBrokerage());
                        natTotalInBrokerage = natTotalInBrokerage.add(batchTransferAccountB.getNatInBrokerage());
                    }
                    // 转出手续费
                    if (batchTransferAccountB.getOutBrokerage() != null) {
                        batchTransferAccountB.setNatOutBrokerage(currencyQueryService.getAmountOfCurrencyPrecision(batchTransferAccount.getNatCurrency(), batchTransferAccountB.getOutBrokerage()));
                        totalOutBrokerage = totalOutBrokerage.add(batchTransferAccountB.getOutBrokerage());
                        natTotalOutBrokerage = natTotalOutBrokerage.add(batchTransferAccountB.getNatOutBrokerage());
                    }
                }
                if (oriSum != null && oriSum.compareTo(BigDecimal.ZERO) != 0) {
                    totalAmount = totalAmount.add(oriSum);
                }
                // 对子表数据的部分字段赋默认值
                fillBatchTransferAccountB(batchTransferAccount, batchTransferAccountB);

                // 对方单位id 赋值为资金组织
                batchTransferAccountB.setOppId(batchTransferAccount.getAccentity());
                // 处理现金柜业务
                // 默认非现金柜业务
                batchTransferAccountB.setIsCashBusiness(YesOrNoEnum.NO.getValue());
                // 收付类型默认为付
                batchTransferAccountB.setDirectionType(String.valueOf(Direction.Credit.getValue()));
                // 处理现金柜
                handleCashBusiness(batchTransferAccount, batchTransferAccountB, tradeTypeCode, firstBatchTransferAccountB);
                // 处理导入场景的子表数据
                fillBatchTransferAccountBImport(batchTransferAccountB,tradeTypeCode, importFlag);
                // 数币钱包保存前校验
                checkDigitalWallet(batchTransferAccount, batchTransferAccountB, tradeTypeCode);
            }

            if (!importFlag && totalAmount.compareTo(batchTransferAccount.getTransferSumAmount()) != 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B5", "同名账户批量划转单金额与转账明细汇总金额不相等") /* "同名账户批量划转单金额与转账明细汇总金额不相等" */);
            }

            // 导入计算主表合计金额
            handleSumAmountImport(batchTransferAccount, importFlag, totalAmount, natTotalAmount, totalInBrokerage, natTotalInBrokerage,
                    totalOutBrokerage, natTotalOutBrokerage);

            // 处理支票 业务
            handleCheckBusiness(batchTransferAccount, billbs, tradeTypeCode);
            // 保存前释放预占
            saveBudget(batchTransferAccount);
        } catch (Exception e) {
            log.error("同名账户批量划转单保存前规则执行异常", e);
            throw new CtmException(e.getMessage());
        }

        return new RuleExecuteResult();
    }

    /**
     * 更新预算占用状态-更新场景-释放预占
     * @param batchTransferAccount
     * @throws Exception
     */
    private void saveBudget(BatchTransferAccount batchTransferAccount) throws Exception {
        // 预算
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_BATCHTRANSFERACCOUNT)) {
            return;
        }
        if (batchTransferAccount.getEntityStatus() != EntityStatus.Update) {
            return;
        }
        BatchTransferAccount dbBatchTransferAccount = MetaDaoHelper.findByIdPartition(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId(), 2);
        if (dbBatchTransferAccount == null) {
            return;
        }
        List<BatchTransferAccount_b> batchTransferAccountBs = dbBatchTransferAccount.BatchTransferAccount_b();
        if (CollectionUtils.isEmpty(batchTransferAccountBs)) {
            return;
        }

        ResultBudget resultBudget = btaCmpBudgetManagerService.saveOccupyBudget(dbBatchTransferAccount, batchTransferAccountBs, BatchTransferAccountUtil.SAVE_SAVE);
        if (resultBudget.isSuccess()) {
            if (resultBudget.getIds() != null && !resultBudget.getIds().isEmpty()) {
                batchTransferAccountBs.stream().forEach(item -> {
                    if (resultBudget.getIds().contains(item.getId().toString())) {
                        item.setIsOccupyBudget(resultBudget.getBudgeted());
                        item.setEntityStatus(EntityStatus.Update);
                    }
                });
            } else {
                batchTransferAccountBs.stream().forEach(item -> {
                    item.setIsOccupyBudget(resultBudget.getBudgeted());
                    item.setEntityStatus(EntityStatus.Update);
                });
            }
            MetaDaoHelper.update(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountBs);
        }
    }

    /**
     * 根据交易类型校验本方和对方的银行账户
     *
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @param tradeTypeExtCode
     */
    private void checkDigitalWallet(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB, String tradeTypeExtCode) throws Exception {
        String tradeTypeCode = batchTransferAccount.get("tradeType_code");
        // 数币钱包充值、数币钱包提现、数币钱包互转
        List<String> tradeTypeCodeList = Arrays.asList("WCZ", "WTX", "WHZ");
        List<String> tradeTypeExtList = Arrays.asList("sbqbcz", "sbqbtx", "sbqbhz");
        if (!(tradeTypeCodeList.contains(tradeTypeCode) || tradeTypeExtList.contains(tradeTypeExtCode))) {
            return;
        }
        if (StringUtils.isEmpty(batchTransferAccountB.getPayBankAccountId())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C2", "付款银行账户为空！") /* "付款银行账户为空！" */);
        }
        if (StringUtils.isEmpty(batchTransferAccountB.getRecBankAccountId())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C6", "收款银行账户为空！") /* "收款银行账户为空！" */);
        }
        // 收付钱包开户类型
        Map<String, EnterpriseBankAcctVO> enterpriseBankAcctVOMap = BaseDocUtils.getBankAcctMap(Arrays.asList(batchTransferAccountB.getPayBankAccountId(), batchTransferAccountB.getRecBankAccountId()));
        EnterpriseBankAcctVO payBankAcct = enterpriseBankAcctVOMap.get(batchTransferAccountB.getPayBankAccountId());
        EnterpriseBankAcctVO recBankAcct = enterpriseBankAcctVOMap.get(batchTransferAccountB.getRecBankAccountId());
        if (payBankAcct == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A9", "付款银行账户不存在！") /* "付款银行账户不存在！" */);
        }
        if (recBankAcct == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006AB", "收款银行账户不存在！") /* "收款银行账户不存在！" */);
        }
        Integer payBankAcctOpenType =payBankAcct.getAcctopentype();
        Integer recBankAcctOpenType =recBankAcct.getAcctopentype();
        // 银行开户、其他金融机构
        List<Integer> bankOrOtherFinOrg = Arrays.asList(AcctopenTypeEnum.BankAccount.getValue(),AcctopenTypeEnum.OtherFinancial.getValue());
        switch (tradeTypeCode) {
            // 数币钱包充值
            // 付方开户类型=银行开户、其他金融机构
            // 收方开户类型=钱包账户
            case "WCZ":
                if (!bankOrOtherFinOrg.contains(payBankAcctOpenType)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B1", "付款开户类型错误!") /* "付款开户类型错误!" */);
                }
                if (AcctopenTypeEnum.DigitalWallet.getValue() != recBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B4", "收款开户类型错误!") /* "收款开户类型错误!" */);
                }
                break;
            // 数币钱包提现
            // 收方开户类型=银行开户、其他金融机构
            // 付方开户类型=钱包账户
            case "WTX":
                if (!bankOrOtherFinOrg.contains(recBankAcctOpenType)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B4", "收款开户类型错误!") /* "收款开户类型错误!" */);
                }
                if (AcctopenTypeEnum.DigitalWallet.getValue() != payBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B1", "付款开户类型错误!") /* "付款开户类型错误!" */);
                }
                break;
            // 数币钱包互转
            // 收付方开户类型=钱包账户
            case "WHZ":
                if (AcctopenTypeEnum.DigitalWallet.getValue() != payBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B1", "付款开户类型错误!") /* "付款开户类型错误!" */);
                }
                if (AcctopenTypeEnum.DigitalWallet.getValue() != recBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B4", "收款开户类型错误!") /* "收款开户类型错误!" */);
                }
                break;
            default:
                break;

         }

        batchTransferAccountWalletHandler.checkSave(batchTransferAccountB);
    }

    /**
     * 导入赋值主表合计的额度
     * @param batchTransferAccount
     * @param importFlag
     * @param totalAmount
     * @param natTotalAmount
     * @param totalInBrokerage
     * @param natTotalInBrokerage
     * @param totalOutBrokerage
     * @param natTotalOutBrokerage
     */
    private void handleSumAmountImport(BatchTransferAccount batchTransferAccount, boolean importFlag, BigDecimal totalAmount, BigDecimal natTotalAmount, BigDecimal totalInBrokerage, BigDecimal natTotalInBrokerage, BigDecimal totalOutBrokerage, BigDecimal natTotalOutBrokerage) {
        if (!importFlag) {
            return;
        }
        // 原币
        batchTransferAccount.setTransferSumAmount(totalAmount);
        // 本币
        batchTransferAccount.setTransferSumNamount(natTotalAmount);
        // 转入手续费
        if (totalInBrokerage.compareTo(BigDecimal.ZERO) != 0) {
            batchTransferAccount.setSumInBrokerage(totalInBrokerage);
            batchTransferAccount.setSumNatInBrokerage(natTotalInBrokerage);
        }
        // 转出手续费
        if (totalOutBrokerage.compareTo(BigDecimal.ZERO) != 0) {
            batchTransferAccount.setSumOutBrokerage(totalOutBrokerage);
            batchTransferAccount.setSumNatOutBrokerage(natTotalOutBrokerage);
        }
    }

    /**
     * 填充导入的子表数据
     * @param batchTransferAccountB
     * @param tradeTypeCode
     * @param importFlag
     */
    private void fillBatchTransferAccountBImport(BatchTransferAccount_b batchTransferAccountB, String tradeTypeCode, boolean importFlag) throws Exception {
        if (!importFlag) {
            return;
        }
        // 正常入账
        batchTransferAccountB.setEntryType(EntryTypeEnum.NormalEntry.getValue() + "");
        // 是否结算补单默认为是
        if (batchTransferAccountB.getSupplementary() == null) {
            batchTransferAccountB.setSupplementary(YesOrNoEnum.YES.getValue());
        }
        // 是否退票默认为 否
        batchTransferAccountB.setReFund(YesOrNoEnum.NO.getValue());
        // 处理付款银行账户信息
        handlePayBankAccount(batchTransferAccountB);
        // 处理收款银行账户信息
        handleRecBankAccount(batchTransferAccountB);
    }

    /**
     * 根据收款银行账户id查询银行信息
     * @param batchTransferAccountB
     */
    private void handleRecBankAccount(BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (StringUtils.isEmpty(batchTransferAccountB.getRecBankAccountId())) {
            return;
        }
        // 查询企业银行账户
        EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(batchTransferAccountB.getRecBankAccountId());
        // 收款方开户行id
        batchTransferAccountB.setRecBankId(enterpriseBankAcctVO.getBankNumber());
        // 收款方开户行名称
        batchTransferAccountB.setRecBankName(enterpriseBankAcctVO.getBankNumberName());
        // 收款方开户行联行号
        batchTransferAccountB.setRecLineNumber(enterpriseBankAcctVO.getLineNumber());
        // 收款银行类别Id
        batchTransferAccountB.setRecBankTypeId(enterpriseBankAcctVO.getBank());
    }

    /**
     * 根据付款银行账户Id查询付款方银行信息
     * @param batchTransferAccountB
     */
    private void handlePayBankAccount(BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (StringUtils.isEmpty(batchTransferAccountB.getPayBankAccountId())) {
            return;
        }
        // 查询企业银行账户
        EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(batchTransferAccountB.getPayBankAccountId());

        // 付款方开户行id
        batchTransferAccountB.setPayBankId(enterpriseBankAcctVO.getBankNumber());
        // 付款方开户行名称
        batchTransferAccountB.setPayBankName(enterpriseBankAcctVO.getBankNumberName());
        // 付款方开户行联行号
        batchTransferAccountB.setPayLineNumber(enterpriseBankAcctVO.getLineNumber());
        // 付款银行类别Id
        batchTransferAccountB.setPayBankTypeId(enterpriseBankAcctVO.getBank());
    }


    /**
     * 通用校验
     * @param batchTransferAccount
     * @throws Exception
     */
    private void checkCommonParam(BatchTransferAccount batchTransferAccount) throws Exception {
        Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(batchTransferAccount.getAccentity());
        if (enabledBeginData == null) {
            throw new CtmException(new CtmErrorCode("033-502-101870"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F4", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B8", "该资金组织现金管理模块未启用，不能保存单据！") /* "该资金组织现金管理模块未启用，不能保存单据！" */));
        }
        // 交易类型
        CmpCommonUtil.checkTradeTypeEnable(batchTransferAccount.getTradeType());
    }

    /**
     * 非导入的校验
     * @param batchTransferAccount
     * @param importFlag
     */
    private void checkParamNotImport(BatchTransferAccount batchTransferAccount, boolean importFlag) {
        if (importFlag) {
            return;
        }
        // 单据类型
        if (StringUtils.isEmpty(batchTransferAccount.getBillTypeId())) {
            throw new CtmException(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006BD", "单据类型不能为空") /* "单据类型不能为空" */) /* "单据类型不能为空" */);
        }

        // 转账金额合计
        BigDecimal transferSumAmount = batchTransferAccount.getTransferSumAmount();
        if (transferSumAmount == null) {
            throw new CtmException(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006BF", "转账金额合计不能为空！") /* "转账金额合计不能为空！" */) /* "转账金额合计不能为空！" */);
        }
        if (transferSumAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new CtmException(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C1", "转账金额合计不能等于0！") /* "转账金额合计不能等于0！" */) /* "转账金额合计不能等于0！" */);
        }

        // 明细数据
        if (isInsert(batchTransferAccount) && CollectionUtils.isEmpty(batchTransferAccount.BatchTransferAccount_b())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C5", "明细不能为空！") /* "明细不能为空！" */);
        }
    }

    /**
     * 校验导入时的参数
     * @param batchTransferAccount
     * @param importFlag
     */
    private void checkParamImport(BatchTransferAccount batchTransferAccount, boolean importFlag) {
        if (!importFlag) {
            return;
        }

        if(CollectionUtils.isEmpty(batchTransferAccount.BatchTransferAccount_b())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006AD", "同名账户批量划转子表数据不能为空，请维护导入模版主表的（手工码）与子表的（批量同名账户划转单主表 手工码）!") /* "同名账户批量划转子表数据不能为空，请维护导入模版主表的（手工码）与子表的（批量同名账户划转单主表 手工码）!" */);
        }
        for (BatchTransferAccount_b batchTransferAccountB : batchTransferAccount.BatchTransferAccount_b()) {
            if (StringUtils.isEmpty(batchTransferAccountB.getPayBankAccountId()) && StringUtils.isEmpty(batchTransferAccountB.getPayCashAccount())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006AF", "付款银行账号和付款现金账户必填其一！") /* "付款银行账号和付款现金账户必填其一！" */);
            }

            if (StringUtils.isEmpty(batchTransferAccountB.getRecBankAccountId()) && StringUtils.isEmpty(batchTransferAccountB.getRecCashAccount())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B2", "收款银行账号和收款现金账户必填其一！") /* "收款银行账号和收款现金账户必填其一！" */);
            }

            // 交易类型编码
            String tradeTypeCode = batchTransferAccount.get("tradeType_code");
            switch (tradeTypeCode) {
                // 银行转账
                case "BT":
                    if (StringUtils.isEmpty(batchTransferAccountB.getPayBankAccountId())) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B6", "交易类型为银行转账，付款银行账号为必输项") /* "交易类型为银行转账，付款银行账号为必输项" */);
                    }
                    if (StringUtils.isEmpty(batchTransferAccountB.getRecBankAccountId())) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B9", "交易类型为银行转账，收款银行账号为必输项") /* "交易类型为银行转账，收款银行账号为必输项" */);
                    }
                    if (StringUtils.isNotEmpty(batchTransferAccountB.getPayBankAccountId()) &&  StringUtils.isNotEmpty(batchTransferAccountB.getRecBankAccountId()) &&
                            batchTransferAccountB.getPayBankAccountId().equals(batchTransferAccountB.getRecBankAccountId())) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006BA", "付款与收款账号不能为同一个") /* "付款与收款账号不能为同一个" */);
                    }
                    break;
                // 现金存入
                case "SC":
                    if (StringUtils.isEmpty(batchTransferAccountB.getPayCashAccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102445"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006BB", "交易类型为缴存现金，付款现金账户编码为必输项") /* "交易类型为缴存现金，付款现金账户编码为必输项" */) /* "交易类型为缴存现金，付款现金账户编码为必输项" */);
                    }
                    if (StringUtils.isEmpty(batchTransferAccountB.getRecBankAccountId())) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006BC", "交易类型为现金存入，收款银行账号为必输项") /* "交易类型为现金存入，收款银行账号为必输项" */);
                    }
                    break;
                // 现金支取
                case "EC":
                    if (StringUtils.isEmpty(batchTransferAccountB.getRecCashAccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102448"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180135", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006BE", "交易类型为提取现金，收款现金账户编码为必输项") /* "交易类型为提取现金，收款现金账户编码为必输项" */) /* "交易类型为提取现金，收款现金账户编码为必输项" */);
                    }
                    if (StringUtils.isEmpty(batchTransferAccountB.getPayBankAccountId())) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C0", "交易类型为现金支取，付款银行账号为必输项") /* "交易类型为现金支取，付款银行账号为必输项" */);
                    }
                    break;
                // 现金互转
                case "CT":
                    if (StringUtils.isEmpty(batchTransferAccountB.getPayCashAccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102449"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180137", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C3", "交易类型为现金互转，付款现金账户为必输项") /* "交易类型为现金互转，付款现金账户为必输项" */) /* "交易类型为现金互转，付款现金账户为必输项" */);
                    }
                    if (StringUtils.isEmpty(batchTransferAccountB.getRecCashAccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102450"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C7", "交易类型为现金互转，收款现金账户编码为必输项") /* "交易类型为现金互转，收款现金账户编码为必输项" */) /* "交易类型为现金互转，收款现金账户编码为必输项" */);
                    }
                    break;
                // 第三方转账
                case "TPT":
                    if (batchTransferAccount.get(ICmpConstant.VIRTUALBANK) == null) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006AA", "交易类型为第三方转账，三方转账类型为必输项") /* "交易类型为第三方转账，三方转账类型为必输项" */);
                    }
                    if (batchTransferAccount.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.BankToVirtual.getValue())) {
                        if (StringUtils.isEmpty(batchTransferAccountB.getPayBankAccountId())) {
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006AC", "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账为必输项") /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账为必输项" */);
                        }
                    } else if (batchTransferAccount.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.VirtualToBank.getValue())) {
                        if (StringUtils.isEmpty(batchTransferAccountB.getRecBankAccountId())) {
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006AE", "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账号为必输项") /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账号为必输项" */);
                        }
                    } else {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B0", "交易类型为第三方转账，三方转账类型错误") /* "交易类型为第三方转账，三方转账类型错误" */);
                    }
                    break;

                default:
                    break;
             }
        }

    }

    /**
     * 导入时主表填充值
     * @param batchTransferAccount
     * @param importFlag
     */
    private void fillBatchTransferAccountImport(BatchTransferAccount batchTransferAccount, boolean importFlag) {
        if (!importFlag) {
            return;
        }
        // 单据类型
        batchTransferAccount.setBillTypeId("2283268970412769285");
        // 凭证状态-未生成
        batchTransferAccount.setVoucherstatus(VoucherStatus.Empty.getValue());
        // 单据生成方式-手工新增
        batchTransferAccount.setGenerateMethod("IMPORT");
    }

    /**
     * 处理支票相关的业务
     * @param batchTransferAccount
     * @param billbs
     * @param tradeTypeCode
     */
    private void handleCheckBusiness(BatchTransferAccount batchTransferAccount, List<BatchTransferAccount_b> billbs, String tradeTypeCode) throws Exception {
        if (!"EC".equals(batchTransferAccount.get("tradeType_code")) && !"tqxj".equals(tradeTypeCode)) {
            return;
        }
        // 校验支票
        checkNoteNo(billbs);
        // 占用支票
        occupyNoteNo(billbs, batchTransferAccount);
    }

    /**
     * 占用支票
     * @param billbs
     * @param batchTransferAccount
     * @throws Exception
     */
    private void occupyNoteNo(List<BatchTransferAccount_b> billbs, BatchTransferAccount batchTransferAccount) throws Exception {
        for (BatchTransferAccount_b batchTransferAccountB : billbs) {
            // 付款结算方式非支票业务直接返回
            if (!"8".equals(batchTransferAccountB.get("paySettlemode_serviceAttr") + "")) {
                continue;
            }

            // 新增场景占用支票
            occupyNoteForAdd(batchTransferAccount, batchTransferAccountB);
            // 编辑场景占用支票
            occupyNoteForEdit(batchTransferAccount, batchTransferAccountB);
        }
    }

    /**
     * 编辑场景占用支票
     * @param batchTransferAccount
     * @param batchTransferAccountB
     */
    private void occupyNoteForEdit(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (batchTransferAccount.getId() == null) {
            return;
        }
        BatchTransferAccount_b dbBatchTransferAccountB = MetaDaoHelper.findById(BatchTransferAccount_b.ENTITY_NAME, batchTransferAccountB.getId());
        // 选择了支票
        occupyChooseNote(batchTransferAccountB, dbBatchTransferAccountB);
        // 原来选择了支票，修改为未选择支票，释放支票
        occupyUnChooseNote(batchTransferAccountB, dbBatchTransferAccountB);
    }

    /**
     * 原来选择了支票，修改为未选择支票，释放支票
     * @param batchTransferAccountB
     * @param dbBatchTransferAccountB
     * @throws Exception
     */
    private void occupyUnChooseNote(BatchTransferAccount_b batchTransferAccountB, BatchTransferAccount_b dbBatchTransferAccountB) throws Exception {
        if (batchTransferAccountB.getNoteId() != null) {
            return;
        }
        //查老的的支票数据（被换的老支票）并更新为预占
        if (dbBatchTransferAccountB.getNoteId() == null) {
            return;
        }
        CheckStock dbCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, dbBatchTransferAccountB.getNoteId());
        //转账单查询为空说明是新增,此时需要对支票进行释放预占
        dbCheckStock.setOccupy(YesOrNoEnum.NO.getValue());
        dbCheckStock.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, dbCheckStock);
    }

    /**
     * 前端选择了支票进行占用
     * @param batchTransferAccountB
     * @param dbBatchTransferAccountB
     * @throws Exception
     */
    private void occupyChooseNote(BatchTransferAccount_b batchTransferAccountB, BatchTransferAccount_b dbBatchTransferAccountB) throws Exception {
        if (batchTransferAccountB.getNoteId() == null) {
            return;
        }
        // 付款结算方式非支票业务直接返回
        if (!"8".equals(batchTransferAccountB.get("paySettlemode_serviceAttr") + "")) {
            return;
        }
        String checkid = batchTransferAccountB.getNoteId().toString();
        //此处理是为了防止与日常环境不一致，雪花id的问题
        if (!Objects.isNull(dbBatchTransferAccountB) && ValueUtils.isNotEmpty(String.valueOf(dbBatchTransferAccountB.getNoteId()))) {
            boolean isSameCheck = !dbBatchTransferAccountB.getNoteId().toString().equals(checkid);
            //不相等则说明更换了支票
            if (isSameCheck) { //换票、清空支票需要释放支票预占
                //查最新的支票数据（被换的新支票）并更新为预占
                CheckStock newCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, batchTransferAccountB.getNoteId());
                //转账单查询为空说明是新增,此时需要对支票进行释放预占
                newCheckStock.setOccupy(YesOrNoEnum.YES.getValue());
                newCheckStock.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(CheckStock.ENTITY_NAME, newCheckStock);
                //查老的的支票数据（被换的老支票）并更新为预占
                CheckStock oldCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, dbBatchTransferAccountB.getNoteId());
                //转账单查询为空说明是新增,此时需要对支票进行释放预占
                oldCheckStock.setOccupy(YesOrNoEnum.NO.getValue());
                oldCheckStock.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(CheckStock.ENTITY_NAME, oldCheckStock);
            }
        } else {//编辑，并且是从非支票结算更改为支票结算
            //查最新的支票数据
            CheckStock newCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, batchTransferAccountB.getNoteId());
            //转账单查询为空说明是新增,此时需要对支票进行预占
            newCheckStock.setOccupy(YesOrNoEnum.YES.getValue());
            newCheckStock.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(CheckStock.ENTITY_NAME, newCheckStock);
        }
    }

    /**
     * 新增场景占用支票
     * @param batchTransferAccount
     * @param batchTransferAccountB
     * @throws Exception
     */
    private void occupyNoteForAdd(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB) throws Exception {
        if (batchTransferAccount.getId() != null) {
            return;
        }
        // 付款结算方式非支票业务直接返回
        if (!"8".equals(batchTransferAccountB.get("paySettlemode_serviceAttr") + "")) {
            return;
        }
        //查最新的支票数据
        CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, batchTransferAccountB.getNoteId());
        if (addCheckStock.getOccupy() == YesOrNoEnum.YES.getValue()) {
            // 若支票编号已被预占，则提示保存失败
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7BCBF205200009", "支票编号已被预占，保存失败，请重新选择！"));
        }
        //转账单查询为空说明是新增,此时需要对支票进行预占
        addCheckStock.setOccupy(YesOrNoEnum.YES.getValue());
        addCheckStock.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
    }


    /**
     * 校验支票
     * @param billbs
     */
    private void checkNoteNo(List<BatchTransferAccount_b> billbs) {
        for (BatchTransferAccount_b batchTransferAccountB : billbs) {
            // 付款结算方式的业务属性为支票结算
            if ("8".equals(batchTransferAccountB.get("paySettlemode_serviceAttr") + "") && StringUtils.isEmpty(batchTransferAccountB.getNoteno())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102435"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003C", "支票编号不能为空") /* "支票编号不能为空" */);
            }
        }

        // 相同的支票不能保存
        Map<String, List<BatchTransferAccount_b>> batchTransferAccountBMapList = billbs.stream()
                .filter(v -> "8".equals(v.get("paySettlemode_serviceAttr") + "")).collect(Collectors.groupingBy(BatchTransferAccount_b::getNoteno));
        if (MapUtils.isEmpty(batchTransferAccountBMapList)) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (String key : batchTransferAccountBMapList.keySet()) {
            List<BatchTransferAccount_b> batchTransferAccountBList = batchTransferAccountBMapList.get(key);
            if (batchTransferAccountBList.size() == 1) {
                continue;
            }

            if (i == 0) {
                stringBuilder.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B3", "使用支票转账时，支票号[") /* "使用支票转账时，支票号[" */);
            }
            stringBuilder.append(key).append("、");//@notranslate
            i++;
        }
        if (i > 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006B7", "]不能重复，请检查转账明细中的票证号。") /* "]不能重复，请检查转账明细中的票证号。" */);
            throw new CtmException(stringBuilder.toString());
        }
    }

    /**
     * 处理现金柜业务
     * @param batchTransferAccount  主表
     * @param batchTransferAccountB 子表
     * @param tradeTypeCode 交易类型编码
     * @param firstBatchTransferAccountB 第一笔子表
     * @throws Exception
     */
    private void handleCashBusiness(BatchTransferAccount batchTransferAccount, BatchTransferAccount_b batchTransferAccountB, String tradeTypeCode,
                                    BatchTransferAccount_b firstBatchTransferAccountB) throws Exception {
        // 判断是否是现金柜业务 给isCashBusiness字段赋值
        // 首先 交易类型为缴存现金
        if ("SC".equals(batchTransferAccount.get("tradeType_code")) || "jcxj".equals(tradeTypeCode)) {
            // 收款银行账户是结算中心开户 即为现金柜业务
            EnterpriseBankAcctVO oppositebankaccount =  new EnterpriseBankAcctVO();
            if (ObjectUtils.isNotEmpty(batchTransferAccountB.getRecBankAccountId())) {
                oppositebankaccount = EnterpriseBankQueryService.findById(batchTransferAccountB.getRecBankAccountId());
            }
            if (ObjectUtils.isNotEmpty(oppositebankaccount) && oppositebankaccount.getAcctopentype() != null
                    && AcctopenTypeEnum.SettlementCenter.getValue() == oppositebankaccount.getAcctopentype()) {
                batchTransferAccountB.setIsCashBusiness(YesOrNoEnum.YES.getValue());
                // 现金柜场景 收付类型为收
                batchTransferAccountB.setDirectionType(String.valueOf(Direction.Debit.getValue()));
            }
            // 判断与第一笔明细的isCashBusiness值是否一致
            if (!batchTransferAccountB.getIsCashBusiness().equals(firstBatchTransferAccountB.getIsCashBusiness())) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C4", "批量同名账户划转单明细的是否现金柜业务值不一致") /* "批量同名账户划转单明细的是否现金柜业务值不一致" */);
            }
        }
    }

    /**
     * 填充子表数据
     * @param batchTransferAccount 主表
     * @param batchTransferAccountB 子表
     */
    private void fillBatchTransferAccountB(BatchTransferAccount batchTransferAccount,BatchTransferAccount_b batchTransferAccountB) throws Exception {
        // 是否传结算中心 默认空
        batchTransferAccountB.setPushStct(null);
        // 业务对象类型
        batchTransferAccountB.setBizObjType(String.valueOf(EventSource.Cmpchase.getValue()));
        // 对方类型 内部单位
        batchTransferAccountB.setOppType(String.valueOf(CaObject.InnerUnit.getValue()));

        // 期望结算日期
        // 流水生单场景 交易日期
        // 非流水生单场景 单据日期
        batchTransferAccountB.setExpectSettleDate(batchTransferAccount.getBillDate());
        // 汇率类型
        batchTransferAccountB.setNatRateTypeId(batchTransferAccount.getExchangeRateType());
        // 本币汇率
        batchTransferAccountB.setNatRate(batchTransferAccount.getExchRate());
        // 本币汇率折算方式
        batchTransferAccountB.setNatRateOps(batchTransferAccount.getExchRateOps());
        // 原币币种 默认取主表的币种
        batchTransferAccountB.setCurrencyId(batchTransferAccount.getCurrency());
        // 本币币种
        batchTransferAccountB.setNatCurrencyId(batchTransferAccount.getNatCurrency());
        // 对方账户类型 -- 企业银行账号
        // 企业现金账户没有账户类型
        batchTransferAccountB.setOppAcctType(BatchTransferAccountUtil.getAcctType(batchTransferAccountB.getRecBankAccountId()));

        // 暂时赋值，后续结算sdk设置默认值后可以去掉
        // 结算中金额 结算那边说先复制为0
        batchTransferAccountB.setOriTransitAmount(BigDecimal.valueOf(0));
        // 结算止付金额
        batchTransferAccountB.setSettleStopPayAmount(BigDecimal.valueOf(0));
        // 结算成功金额
        batchTransferAccountB.setSettleSucAmount(BigDecimal.valueOf(0));
        // 待结算余额 赋值转账原币金额
        batchTransferAccountB.setOriRemainAmount(batchTransferAccountB.getOriSum());
        // 资金组织赋值
        batchTransferAccountB.setAccentity(batchTransferAccount.getAccentity());
    }

    /**
     * 判断是否插入
     * @param batchTransferAccount
     * @return
     */
    private boolean isInsert(BatchTransferAccount batchTransferAccount) {
        return EntityStatus.Insert.equals(batchTransferAccount.getEntityStatus());
    }

    /**
     * 填充主表参数
     * @param batchTransferAccount
     */
    private void fillBatchTransferAccount(BatchTransferAccount batchTransferAccount) throws Exception {
        if (isInsert(batchTransferAccount) && batchTransferAccount.getVerifystate() == null) {
            // 审批状态 初始开立
            batchTransferAccount.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        }
        // 本币币种
        batchTransferAccount.setBizObjType(BatchTransferAccount.ENTITY_NAME);
        batchTransferAccount.setNatCurrency(AccentityUtil.getNatCurrencyIdByAccentityId(batchTransferAccount.getAccentity()));
    }

    /**
     * 参数校验
     *
     * @param batchTransferAccount 批量同名账户划转实体
     * @param importFlag
     * @throws Exception
     */
    private void checkParam(BatchTransferAccount batchTransferAccount, boolean importFlag) throws Exception {
        // 通用校验
        checkCommonParam(batchTransferAccount);
        // 非导入的校验
        checkParamNotImport(batchTransferAccount,importFlag);
        // 校验参数导入
        checkParamImport(batchTransferAccount, importFlag);
    }
}
