package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ml.vo.LanguageVO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.ucf.common.ml.MultiLangContext;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TranstypeQueryPageParam;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckRpcService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.digitalwallet.impl.TransferAccountWalletHandler;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.*;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.transferaccount.util.BaseDocUtils;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;


/**
 * @ClassName TaBeforeSaveRule
 * @Desc 转账单保存前规则 - 29
 * @Author nayunhao
 * @Date 2019/10/10
 * @Version 1.0
 */
@Slf4j
@Component("taBeforeSaveRule")
public class TaBeforeSaveRule extends AbstractCommonRule {
    @Autowired
    private SettlementService settlementService;

    @Autowired
    private JournalService journalService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private CtmSignatureService digitalSignatureService;
    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    ProcessService processService;
    @Autowired
    BankAccountSettingService bankaccountSettingService;
    @Autowired
    CmCommonService commonService;
    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    private CtmCmpCheckRpcService ctmCmpCheckRpcService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Autowired
    @Qualifier("transferAccountWalletHandler")
    private TransferAccountWalletHandler transferAccountWalletHandler;

    private static final String PAYBANKACCOUNT = "payBankAccount";

    private static final Map<String, String> tipInfo = new HashMap<>();

    static {
        tipInfo.put(IBussinessConstant.ACCENTITY, "P_YS_FED_FW_0000022114") ;
        tipInfo.put(ICmpConstant.VOUCHDATE, "P_YS_PF_PROCENTER_0000022686") ;
        tipInfo.put(ICmpConstant.SRC_ITEM, "P_YS_FI_IA_0000055057") ;
        tipInfo.put(ICmpConstant.BILLTYPE, "P_YS_PF_GZTTMP_0000077375") ;
        tipInfo.put(ICmpConstant.VIRTUALBANK, "P_YS_CTM_CTM-CMP-MD_1669662042403897352") ;
        tipInfo.put(ICmpConstant.CURRENCY, "P_YS_PF_GZTSYS_0000013299");
        tipInfo.put(ICmpConstant.NATCURRENCY, "P_YS_PF_PRINT_0000065373") ;
        tipInfo.put(ICmpConstant.SETTLE_MODE, "P_YS_PF_GZTSYS_0000012684") ;
        tipInfo.put(ICmpConstant.PAY_STATUS, "P_YS_SD_UDHBN_0000032623") ;
        tipInfo.put(ICmpConstant.ISSETTLE, "P_YS_SCM_UPU-UI_0000174710") ;
        tipInfo.put(ICmpConstant.SETTLE_STATUS, "P_YS_HR_HRJQ_0000055403") ;
        tipInfo.put(IBussinessConstant.ORI_SUM, "P_YS_SD_SDMB_0000090048") ;
        tipInfo.put(ICmpConstant.PAYBANKACCOUNT, "P_YS_PF_GZTTMP_0000076641") ;
        tipInfo.put(ICmpConstant.COLLVIRTUALACCOUNT, "P_YS_CTM_CTM-CMP-MD_1669662042403897346");
        tipInfo.put(ICmpConstant.RECBANKACCOUNT, "P_YS_PF_PRINT_0000057040");
        tipInfo.put(ICmpConstant.PAYVIRTUALACCOUNT, "P_YS_CTM_CTM-CMP-MD_1669662042403897349") ;
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        EventSource eventSource =EventSource.find(bizObject.get(IBillConst.SRCITEM));
        //如果推送待结算，则返回true
        Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
        if (!ValueUtils.isNotEmptyObj(bizObject.get(IBussinessConstant.ORI_SUM)) || BigDecimal.ZERO.compareTo(bizObject.get(IBussinessConstant.ORI_SUM)) >= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102419"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418011F","转账金额必须大于0") /* "转账金额必须大于0" */);
        }
        //交易类型停用校验
        CmpCommonUtil.checkTradeTypeEnable(bizObject.get("tradetype"));
        String tradeType = getTradeTypeExt(bizObject);
        // 事项类型=银行对账单/认领单/转账单、交易类型=银行转账时，保存时校验付款银行账户、收款银行账户是否是同一结算中心的活期内部户，若不是，错误性提示“内部账户之间的转账业务，收、付款银行账户的结算中心必须相同。”
        if ("import".equals(billDataDto.getRequestAction()) || bizObject.get("billtype") != null && (EventType.CashMark.getValue() == (short) bizObject.get("billtype") || EventType.BillClaim.getValue() == (short) bizObject.get("billtype") || EventType.TransferAccount.getValue() == (short) bizObject.get("billtype"))) {
            if ("yhzz".equals(tradeType)) {
                // 银行类别：结算中心，开户行：相同
                String payBankAccount = bizObject.get("payBankAccount");
                EnterpriseBankAcctVO payEnterpriseBankAcctVO = null;
                if (ObjectUtils.isNotEmpty(payBankAccount)) {
                    payEnterpriseBankAcctVO = enterpriseBankQueryService.findById(payBankAccount);
                }
                if (ObjectUtils.isEmpty(payEnterpriseBankAcctVO)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E1ED5A404900002", "未查到付款银行账户信息！") /* "未查到付款银行账户信息！*/);
                }
                Integer payOpenType = payEnterpriseBankAcctVO.getAcctopentype();
                String recBankAccount = bizObject.get("recBankAccount");
                EnterpriseBankAcctVO recEnterpriseBankAcctVO = null;
                if (ObjectUtils.isNotEmpty(recBankAccount)) {
                    recEnterpriseBankAcctVO = enterpriseBankQueryService.findById(recBankAccount);
                }
                if (ObjectUtils.isEmpty(recEnterpriseBankAcctVO)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E1ED5F004900005", "未查到收款银行账户信息！") /* "未查到收款银行账户信息！*/);
                }
                Integer recOpenType = recEnterpriseBankAcctVO.getAcctopentype();
                // 付方开户类型结算中心开户、收方开户类型银行开户、财务公司开户
                // 收方开户类型结算中心开户、付方开户类型银行开户、财务公司开户 不允许
                if ((AcctopenTypeEnum.SettlementCenter.getValue() == payOpenType && (AcctopenTypeEnum.BankAccount.getValue() == recOpenType || AcctopenTypeEnum.FinancialCompany.getValue() == payOpenType) || (AcctopenTypeEnum.BankAccount.getValue() == payOpenType || AcctopenTypeEnum.FinancialCompany.getValue() == payOpenType) && AcctopenTypeEnum.SettlementCenter.getValue() == recOpenType)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102422"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C8199000408001A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C8199000408001A", "系统不支持结算中心账户与银行账户、财务公司账户之间转账。") /* "系统不支持结算中心账户与银行账户、财务公司账户之间转账。" */) /* "系统不支持结算中心账户与银行账户、财务公司账户之间转账。" */);
                } else if (AcctopenTypeEnum.SettlementCenter.getValue() == payOpenType && AcctopenTypeEnum.SettlementCenter.getValue() == recOpenType) {
                    // 都为结算中心开户(内部户)
                    // 事项类型=银行对账单/认领单，校验转账单传结算的参数是否开启，若未开启，错误性提示“同名账户划转不传结算平台时暂不支持内部账户流水生单，请先前往【现金参数设置】开启转账工作台参数”；
                    if (!pushSettlement) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102420"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81990004080018", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81990004080018", "同名账户划转不传结算平台时暂不支持内部账户流水生单，请先前往【现金参数设置】开启转账工作台参数") /* "同名账户划转不传结算平台时暂不支持内部账户流水生单，请先前往【现金参数设置】开启转账工作台参数" */) /* "同名账户划转不传结算平台时暂不支持内部账户流水生单，请先前往【现金参数设置】开启转账工作台参数" */);
                    } else if (!payEnterpriseBankAcctVO.getBankNumber().equals(recEnterpriseBankAcctVO.getBankNumber())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102421"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81990004080019", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C81990004080019", "内部账户之间的转账业务，收、付款银行账户的结算中心必须相同。") /* "内部账户之间的转账业务，收、付款银行账户的结算中心必须相同。" */) /* "内部账户之间的转账业务，收、付款银行账户的结算中心必须相同。" */);
                    }
                }
            }
        }
        //第三方转账
        boolean thirdPartyTransfer = false;
        // 导入数据校验
        boolean notImportFlag =  !"import".equals(billDataDto.getRequestAction());
        boolean notOpenApiFlag = !bizObject.containsKey("_fromApi") || bizObject.get("_fromApi").equals(false);
        boolean notFromApi = !billDataDto.getFromApi();
        if (notImportFlag && notOpenApiFlag && notFromApi) {
            thirdPartyTransfer = eventSource.getValue() == EventSource.ThreePartyReconciliation.getValue();
            if(thirdPartyTransfer){
                packageBizObject(billContext, bizObject);
            }
        }
        if (bizObject.get("settlemode") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102423"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A13522C05300006","付款方结算方式不能为空") /* "付款方结算方式不能为空" */);
        }
        if(pushSettlement && bizObject.get("collectsettlemode") == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102424"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A136C2E05300009","收款方结算方式不能为空") /* "收款方结算方式不能为空" */);
        }
        if (bizObject.get("exchangeRateType") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102425"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180128","汇率类型不能为空") /* "汇率类型不能为空" */);
        }
        BigDecimal natSum = bizObject.get(IBussinessConstant.NAT_SUM);
        if (natSum == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102426"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012C","本币金额不能为空！") /* "本币金额不能为空！" */);
        }
        if (natSum.doubleValue() == 0 && !bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102427"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012F","本币金额不能等于0！") /* "本币金额不能等于0！" */);
        }

        String enterpriseBankAccount = bizObject.get(PAYBANKACCOUNT);
        if(!StringUtils.isEmpty(enterpriseBankAccount)){
            String data = bankaccountSettingService.getOpenFlag(enterpriseBankAccount);
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
            if(null != jsonObject){
                CtmJSONObject jsonData = jsonObject.getJSONObject("data");
                if(null != jsonData){
                    bizObject.set("isdirectconn",jsonData.get("openFlag"));
                }
            }
        }

        if (notImportFlag && notOpenApiFlag && notFromApi) {
            if(bizObject.get("srcitem").equals(EventSource.Cmpchase.getValue())&& TransferAccount.ENTITY_NAME.equals(billContext.getFullname())
              && !"tpt".equals(bizObject.get("type"))){
                checkSignaturestr(bizObject);
            }
        }else{
            if(bizObject.get("srcitem")==null){
                bizObject.set("srcitem",EventSource.ManualImport.getValue());
            }
            if(bizObject.get("billtype")==null){
                bizObject.set("billtype", EventType.TransferAccount.getValue());
            }
            // openApi接口进来的数据 默认进来传乘
            if (!notFromApi) {
                bizObject.set("exchRateOps", (short)1);
            }
            importCheckAndInit(bizObject);
        }
        if (FIDubboUtils.isSingleOrg() && !thirdPartyTransfer) {
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if (singleOrg != null) {
                bizObject.set(IBussinessConstant.ACCENTITY, singleOrg.get("id"));
                bizObject.set("accentity_name", singleOrg.get("name"));
            }
        }
        if (bizObject.getEntityStatus() == EntityStatus.Update) {
            Boolean check = journalService.checkJournal(bizObject.getId());
            if (check) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102428"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418011E","单据已勾对") /* "单据已勾对" */);
            }
            checkPubTs(bizObject.getPubts(),bizObject.getId());
        }
        if (BigDecimal.ZERO.compareTo(bizObject.get("oriSum")) >= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102429"),MessageUtils.getMessage("P_YS_FI_CM_0000153412") /* "转账金额必须大于0" */);
        }

        BigDecimal inBrokerageNatSum = bizObject.get("inBrokerage");
        if (inBrokerageNatSum!=null && inBrokerageNatSum instanceof BigDecimal && BigDecimal.ZERO.compareTo(bizObject.get("inBrokerage")) > 0) {//转入手续费
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102430"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180122","手续费金额必须大于0") /* "手续费金额必须大于0" */);
        }
        BigDecimal outBrokerage = bizObject.get("outBrokerage");
        if (outBrokerage!=null && outBrokerage instanceof BigDecimal && BigDecimal.ZERO.compareTo(bizObject.get("outBrokerage")) > 0) {//转出手续费
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102430"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180122","手续费金额必须大于0") /* "手续费金额必须大于0" */);
        }

        if (bizObject.getEntityStatus() == EntityStatus.Insert) {
            if(!EventSource.Drftchase.equals(eventSource)){
                if(!thirdPartyTransfer){
                    //bizObject.set("settlestatus", SettleStatus.noSettlement.getValue());
                    //银行对账单生成转账单 业务处理-已结算补单-不改变结算状态
                    if(bizObject.getShort("settlestatus")==null||
                            SettleStatus.SettledRep.getValue()!=bizObject.getShort("settlestatus").shortValue()){
                        bizObject.set("settlestatus", SettleStatus.noSettlement.getValue());
                    }
                    //银行对账单生成转换账单 支付状态为已支付补单
                    if (ObjectUtils.isNotEmpty(bizObject.getShort("paystatus")) && PayStatus.SupplPaid.getValue() == bizObject.getShort("paystatus")  ) {
                        bizObject.set("paystatus", PayStatus.SupplPaid.getValue());
                    } else {
                        bizObject.set("paystatus", PayStatus.NoPay.getValue());
                    }

                }
                bizObject.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());
                bizObject.set("auditstatus", AuditStatus.Incomplete.getValue());
                bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
            }
        }
        // 校验更新时结算状态
        checkSettleStatusOfUpdate(bizObject);
        // 导入历史数据，审批流状态为空，赋默认值初始开立
        if (bizObject.get(ICmpConstant.VERIFY_STATE) == null || ObjectUtils.isEmpty(bizObject.get(ICmpConstant.VERIFY_STATE))) {
            bizObject.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());
        }
        Boolean settlement = settlementService.checkDailySettlement(bizObject.get(IBussinessConstant.ACCENTITY), bizObject.get("vouchdate"));
        if (settlement) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102431"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180134","单据日期已日结") /* "单据日期已日结" */);
        }
        Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get(IBussinessConstant.ACCENTITY));/* 暂不修改 已登记*/
        if (enabledBeginData == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101870"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F4","该资金组织现金管理模块未启用，不能保存单据！"));
        }
        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102433"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180138","单据日期小于模块启用日期") /* "单据日期小于模块启用日期" */);
        }
        if ("bt".equals(bizObject.get("type"))) {
            EnterpriseBankAcctVO bankAcc = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("recBankAccount"));
            String originalMsg = ((BigDecimal)bizObject.get(IBussinessConstant.ORI_SUM)).stripTrailingZeros().toPlainString() + bankAcc.getAccount() + bankAcc.getAcctName();
            bizObject.set("signature", digitalSignatureService.iTrusSignMessage(originalMsg));
        }
        Long checkId = bizObject.get("checkid");
        if (checkId != null) {
            CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
            if (checkStock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102434"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013F","已选择支票不是已入库，保存失败") /* "已选择支票不是已入库，保存失败" */);
            }

            //1。查询现金参数 是否推送结算 是
            Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();
            //如果是现金提取交易类型、支票结算结算方式、不推结算数据则进行支票占票操作
            if (("ec".equals(bizObject.get("type")) || "EC".equals(bizObject.get("tradetype_code")) || "tqxj".equals(tradeType)) && !checkFundTransfer){

                if ("8".equals(bizObject.get("settlemode_serviceAttr")) && StringUtils.isEmpty(bizObject.get("checkno"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102435"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003C","支票编号不能为空") /* "支票编号不能为空" */  + ", tradetype_code : " + bizObject.get("tradetype_code") + ", tradeType : " + tradeType);
                }
                //8是支票业务
                if ("8".equals(bizObject.get("settlemode_serviceAttr"))) {
                    //转账单不为空则说明是编辑，为空则是新增
                    if (bizObject.get("id") != null) {
                        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.get("id"));
                        //此处理是为了防止与日常环境不一致，雪花id的问题
                        if (transferAccount != null && transferAccount.getCheckid() != null){
                            boolean isSameCheck = false;
                            String checkid = bizObject.get("checkid").toString();
                            if (!transferAccount.getCheckid().toString().equals(checkid)) {
                                //不相等则说明更换了支票
                                isSameCheck = true;
                            }

                            if (isSameCheck) {

                                //1. 针对保存后的支票编号（没有则不处理），调用支票工作台-支票锁定/解锁接口，操作类型为：锁定、单据方向=付款，更支票状态为开票中。
                                List<CheckDTO> checkDTOChange = this.setNewValue(bizObject);
                                ctmCmpCheckRpcService.checkOperation(checkDTOChange);
                                //2. 针对保存前的支票（没有则不处理），需要调用支票工作台-支票锁定/解锁接口，操作类型为：解锁，单据方向=付款，更支票状态为已入库。
                                String code = bizObject.get("code");
                                List<CheckDTO> checkDTOOriginal = this.setOriginValue(code, transferAccount.getCheckid());
                                ctmCmpCheckRpcService.checkOperation(checkDTOOriginal);
                                //三、编辑态，点击保存按钮，保存前后支票编号相同时，不处理。
                            } else {
                                int settlemode = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
                                if (settlemode == 0) {
                                    //说明是旧数据切换成了支票业务结算方式，支票锁定
                                    List<CheckDTO> checkDTOChange = this.setNewValue(bizObject);
                                    ctmCmpCheckRpcService.checkOperation(checkDTOChange);
                                }
                            }
                        }else {
                            //转账单查询为空说明是新增
                            List<CheckDTO> checkDTOChange = this.setNewValue(bizObject);
                            ctmCmpCheckRpcService.checkOperation(checkDTOChange);
                        }
                    } else {
                        List<CheckDTO> checkDTOChange = this.setNewValue(bizObject);
                        ctmCmpCheckRpcService.checkOperation(checkDTOChange);
                    }
                }

                //银行转账结算方式
                if ("0".equals(bizObject.get("settlemode_serviceAttr"))) {
                    //如果不为空则为编辑，否则是新增
                    TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.get("id"));
                    if (transferAccount != null) {
                        //编辑
                        if (transferAccount.getCheckid() != null) {
                            //支票id有值则说明是由支票业务转为了银行业务，原支票解锁
                            String code = bizObject.get("code");
                            List<CheckDTO> checkDTOOriginal = this.setOriginValue(code, transferAccount.getCheckid());
                            ctmCmpCheckRpcService.checkOperation(checkDTOOriginal);
                        }
                    } else {
                        //转账单为空则说明是新增，支票编号不允许有值
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102436"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003D","结算方式为银行业务时，支票编号只能为空") /* "结算方式为银行业务时，支票编号只能为空" */);
                    }
                }
            } else if (("ec".equals(bizObject.get("type")) || "EC".equals(bizObject.get("tradetype_code")) || "tqxj".equals(tradeType)) && checkFundTransfer) {
                if ("8".equals(bizObject.get("settlemode_serviceAttr")) && StringUtils.isEmpty(bizObject.get("checkno"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102435"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003C", "支票编号不能为空") /* "支票编号不能为空" */ + ", tradetype_code : " + bizObject.get("tradetype_code") + ", tradeType : " + tradeType);
                }
                if ("8".equals(bizObject.get("settlemode_serviceAttr"))) { //8:支票业务
                    //转账单不为空则说明是编辑，为空则是新增
                    if (bizObject.get("id") != null) {
                        String checkid = bizObject.get("checkid").toString();
                        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.get("id"));
                        //此处理是为了防止与日常环境不一致，雪花id的问题
                        if (!Objects.isNull(transferAccount)  && ValueUtils.isNotEmpty(String.valueOf(transferAccount.getCheckid()))) {
                            boolean isSameCheck = false;
                            if (!transferAccount.getCheckid().toString().equals(checkid)) {
                                //不相等则说明更换了支票
                                isSameCheck = true;
                            }
                            if (isSameCheck) { //换票、清空支票需要释放支票预占
                                //查最新的支票数据（被换的新支票）并更新为预占
                                CheckStock newCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                                //转账单查询为空说明是新增,此时需要对支票进行释放预占
                                newCheckStock.setOccupy((short) 1);
                                newCheckStock.setEntityStatus(EntityStatus.Update);
                                MetaDaoHelper.update(CheckStock.ENTITY_NAME, newCheckStock);
                                //查老的的支票数据（被换的老支票）并更新为预占
                                CheckStock oldCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid());
                                //转账单查询为空说明是新增,此时需要对支票进行释放预占
                                oldCheckStock.setOccupy((short) 0);
                                oldCheckStock.setEntityStatus(EntityStatus.Update);
                                MetaDaoHelper.update(CheckStock.ENTITY_NAME, oldCheckStock);
                            }
                        }else{//编辑，并且是从非支票结算更改为支票结算
                            //查最新的支票数据
                            CheckStock newCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                            //转账单查询为空说明是新增,此时需要对支票进行预占
                            newCheckStock.setOccupy((short) 1);
                            newCheckStock.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CheckStock.ENTITY_NAME, newCheckStock);
                        }
                    }else {//新增
                        //查最新的支票数据
                        CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                        if (addCheckStock.getOccupy() == 1) {
                            // 若支票编号已被预占，则提示保存失败
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E7BCBF205200009", "支票编号已被预占，保存失败，请重新选择！"));
                        }
                        //转账单查询为空说明是新增,此时需要对支票进行预占
                        addCheckStock.setOccupy((short) 1);
                        addCheckStock.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, addCheckStock);
                    }
                }
            }
        } else {
            //支票释放
            if (bizObject.get("id") != null){
                TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.get("id"));
                if (!Objects.isNull(transferAccount)  && ValueUtils.isNotEmpty(String.valueOf(transferAccount.getCheckid()))) {
                    //查询修改前的支票
                    CheckStock oldCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid());
                    //转账单查询为空说明是新增,此时需要对支票进行释放预占
                    oldCheckStock.setOccupy((short) 0);
                    oldCheckStock.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, oldCheckStock);
                }
            }
            //1。查询现金参数 是否推送结算 是
            Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();
            if ("0".equals(bizObject.get("settlemode_serviceAttr")) && !checkFundTransfer) {
                //预发判断是否为null
                Long transferId = bizObject.getId();
                if (transferId != null) {
                    //不为null说明时编辑
                    TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferId);
                    if (transferAccount != null && transferAccount.getCheckid() != null) {
                        int settlemode = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
                        if (settlemode == 0) {
                            //如果原来也是0则是旧数据，不操作支票
                        } else {
                            //支票id有值则说明是由支票业务转为了银行业务，原支票解锁
                            String code = bizObject.get("code");
                            List<CheckDTO> checkDTOOriginal = this.setOriginValue(code, transferAccount.getCheckid());
                            ctmCmpCheckRpcService.checkOperation(checkDTOOriginal);
                        }
                    }
                }
            }
        }
        // 默认不传结算
        bizObject.set("isSettlement",false);
        if (pushSettlement) {
            bizObject.set("isSettlement",true);//是否结算处理为是
        }
        //保存提交后重新匹配预算规则，原预算规则预占删除，新规则占预占
        TransferAccount tA = (TransferAccount) bizObject;
        if (tA.getVerifystate() != null && (tA.getVerifystate() == VerifyState.SUBMITED.getValue() || tA.getVerifystate() == VerifyState.COMPLETED.getValue())) {
            Short occupyBudget = reMatchBudget(tA);
            if (occupyBudget != null) {
                tA.setIsOccupyBudget(occupyBudget);
            }
        }
        // 数币钱包保存前校验
        checkDigitalWallet(tA, tradeType);

        //冗余字段填充
       /* LanguageVO currentLangVO = MultiLangContext.getInstance().getCurrentLangVO();
        EnterpriseBankAcctVO oppositeBankAccount =  new EnterpriseBankAcctVO();
        if (StringUtils.isNotEmpty(tA.getRecBankAccount())) {
            Map<String, EnterpriseBankAcctVO> enterpriseBankAcctVOMap = BaseDocUtils.getBankAcctMap(Arrays.asList(tA.getRecBankAccount()));
            if (ObjectUtils.isNotEmpty(tA.getRecBankAccount())) {
                oppositeBankAccount = enterpriseBankAcctVOMap.get(tA.getRecBankAccount());
                tA.setOppositeBankAccountName(oppositeBankAccount.getMultiLangAcctName().get(currentLangVO.getLangCode()));
                tA.setOppositebankNumber(oppositeBankAccount.getBankNumber());
                tA.setOppositebankAccount(oppositeBankAccount.getAccount());
            }
        }*/
        /*EnterpriseCashVO oppositeCashAccount = new EnterpriseCashVO();
        if(StringUtils.isNotEmpty(bizObject.getString("recCashAccount"))){
            oppositeCashAccount = baseRefRpcService.queryEnterpriseCashAcctById(bizObject.getString("recCashAccount"));
        }

        if("ec".equals(tA.getType()) || "tqxj".equalsIgnoreCase(tradeType)||"CT".equalsIgnoreCase(tA.getType()) || "xjhz".equals(tradeType)){ //现金缴存
            tA.setOppositeBankAccountName(oppositeCashAccount.getName_multiLangText().getText(currentLangVO.getLangCode()));
            tA.setOppositebankNumber(oppositeCashAccount.getAccount());
            tA.setOppositebankAccount(oppositeCashAccount.getAccount());
        }else{
            tA.setOppositeBankAccountName(oppositeBankAccount.getAcctName_multiLangText().getText(currentLangVO.getLangCode()));
            tA.setOppositebankNumber(oppositeBankAccount.getBankNumber());
            tA.setOppositebankAccount(oppositeBankAccount.getAccount());
        }*/
        return new RuleExecuteResult();
    }

    /**
     * 校验更新时结算状态不能为空
     * @param bizObject
     */
    private void checkSettleStatusOfUpdate(BizObject bizObject) {
        if (bizObject.getEntityStatus() != EntityStatus.Update) {
            return;
        }

        if (bizObject.getShort("settlestatus") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100664"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180749","结算状态不能为空！") /* "结算状态不能为空！" */);
        }
    }

    /**
     * 根据交易类型校验本方和对方的银行账户
     * @param transferAccount
     * @param tradeTypeExtCode
     */
    private void checkDigitalWallet(TransferAccount transferAccount, String tradeTypeExtCode) throws Exception {
        String tradeTypeCode = transferAccount.get("tradetype_code");
        // 数币钱包充值、数币钱包提现、数币钱包互转
        List<String> tradeTypeCodeList = Arrays.asList("WCZ", "WTX", "WHZ");
        List<String> tradeTypeExtList = Arrays.asList("sbqbcz", "sbqbtx", "sbqbhz");
        if (!(tradeTypeCodeList.contains(tradeTypeCode) || tradeTypeExtList.contains(tradeTypeExtCode))) {
            return;
        }
        // 收付钱包开户类型
        Map<String, EnterpriseBankAcctVO> enterpriseBankAcctVOMap = BaseDocUtils.getBankAcctMap(Arrays.asList(transferAccount.getPayBankAccount(), transferAccount.getRecBankAccount()));
        EnterpriseBankAcctVO payBankAcct = enterpriseBankAcctVOMap.get(transferAccount.getPayBankAccount());
        EnterpriseBankAcctVO recBankAcct = enterpriseBankAcctVOMap.get(transferAccount.getRecBankAccount());
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
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D5", "付款开户类型错误!") /* "付款开户类型错误!" */);
                }
                if (AcctopenTypeEnum.DigitalWallet.getValue() != recBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D0", "收款开户类型错误!") /* "收款开户类型错误!" */);
                }
                break;
            // 数币钱包提现
            // 收方开户类型=银行开户、其他金融机构
            // 付方开户类型=钱包账户
            case "WTX":
                if (!bankOrOtherFinOrg.contains(recBankAcctOpenType)) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D0", "收款开户类型错误!") /* "收款开户类型错误!" */);
                }
                if (AcctopenTypeEnum.DigitalWallet.getValue() != payBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D5", "付款开户类型错误!") /* "付款开户类型错误!" */);
                }
                break;
            // 数币钱包互转
            // 收付方开户类型=钱包账户
            case "WHZ":
                if (AcctopenTypeEnum.DigitalWallet.getValue() != payBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D5", "付款开户类型错误!") /* "付款开户类型错误!" */);
                }
                if (AcctopenTypeEnum.DigitalWallet.getValue() != recBankAcctOpenType) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D0", "收款开户类型错误!") /* "收款开户类型错误!" */);
                }
                break;
            default:
                break;

        }

        transferAccountWalletHandler.checkSave(transferAccount);
    }
    /*
     *@Author
     *@Description 校验时间戳    编辑后
     *@Date 2020/7/4 10:20
     *@Param [rows]
     *@Return void
     **/
    private void checkPubTs(Date puts, Long id) throws Exception {
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id);
        if (puts.compareTo(transferAccount.getPubts()) != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102437"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180120","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
        }
    }

    /**
     * 校验签名
     *
     */
    private void checkSignaturestr(BizObject bizObject) throws Exception{
        //校验单据来源，如果不是现金自制单据，则不需要校验签名
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        if(srcItem!=-1&&!srcItem.equals(EventSource.Cmpchase.getValue())){
          return;
        }
        //自动化测试，不进行验签处理
        if(StringUtil.isNotEmpty(bizObject.get("signaturestr")) && ICmpConstant.AUTOTEST_SIGNATURE_Constant.equalsIgnoreCase(bizObject.get("signaturestr").toString())) {
            return;
        }
        String salt = "guan1226"; //盐值

        //校验字符串元素签名
        String paybankaccount = bizObject.get("payBankAccount");
        String recbankaccount = bizObject.get("recBankAccount");
        String paycashaccount = bizObject.get("payCashAccount");
        String reccashaccount = bizObject.get("recCashAccount");
        if(StringUtils.isEmpty(paybankaccount)){
            paybankaccount = "";
        }
        if(StringUtils.isEmpty(recbankaccount)){
            recbankaccount = "";
        }
        if(StringUtils.isEmpty(paycashaccount)){
            paycashaccount = "";
        }
        if(StringUtils.isEmpty(reccashaccount)){
            reccashaccount = "";
        }
        String currency = bizObject.get("currency")!=null?bizObject.get("currency"):"";
        String signStr = paybankaccount+recbankaccount+paycashaccount+reccashaccount+currency;
        signStr+=salt;
        signStr= SHA512Util.getSHA512Str(signStr);

        //校验数字元素签名
        String strNum = "";
        BigDecimal oriSum = bizObject.get(IBussinessConstant.ORI_SUM);//转账金额
        BigDecimal natSum = bizObject.get(IBussinessConstant.NAT_SUM); //本币金额
        BigDecimal inBrokerage = bizObject.get("inBrokerage"); //转入方手续费
        BigDecimal outBrokerage = bizObject.get("outBrokerage");//转出方手续费
        BigDecimal inBrokerageNatSum = bizObject.get("inBrokerageNatSum");//转入方手续费本币
        BigDecimal outBrokerageNatSum = bizObject.get("outBrokerageNatSum");//转出方手续费本币
        BigDecimal exchRate = bizObject.get("exchRate");
        String exchRateNoZero = "";
        String oriSumNoZero = "";
        String natSumNoZero ="";
        String inBrokerageNoZero = "";
        String outBrokerageNoZero ="";
        String inBrokerageNatSumNoZero ="";
        String outBrokerageNatSumNoZero = "";
        if(exchRate!=null){
            exchRateNoZero = exchRate.stripTrailingZeros().toPlainString();
        }
        if(oriSum!=null){
            oriSumNoZero=oriSum.stripTrailingZeros().toPlainString();
        }
        if(natSum!=null){
            natSumNoZero=natSum.stripTrailingZeros().toPlainString();
        }
        if(inBrokerage!=null){
            inBrokerageNoZero=inBrokerage.stripTrailingZeros().toPlainString();
        }
        if(outBrokerage!=null){
            outBrokerageNoZero=outBrokerage.stripTrailingZeros().toPlainString();
        }
        if(inBrokerageNatSum!=null){
            inBrokerageNatSumNoZero=inBrokerageNatSum.stripTrailingZeros().toPlainString();
        }
        if(outBrokerageNatSum!=null){
            outBrokerageNatSumNoZero=outBrokerageNatSum.stripTrailingZeros().toPlainString();
        }
        strNum=strNum+exchRateNoZero+oriSumNoZero+natSumNoZero+inBrokerageNoZero+outBrokerageNoZero+inBrokerageNatSumNoZero+outBrokerageNatSumNoZero;

        strNum+=salt;
        String md5str = SHA512Util.getSHA512Str(strNum);
        String signStrFromBiz = Objects.toString(bizObject.get("signstr"), "");
        String signNumFromBiz = Objects.toString(bizObject.get("signNum"), "");
        if (!signStr.equalsIgnoreCase(signStrFromBiz) || !md5str.equalsIgnoreCase(signNumFromBiz)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102438"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180130","单据【") /* "单据【" */ + bizObject.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180131","】数据签名验证失败") /* "】数据签名验证失败" */);
        }
    }

    /**
     * 校验及初始化参数
     * @param bizObject
     * @throws Exception
     */
    private void importCheckAndInit(BizObject bizObject) throws Exception{
        BigDecimal oriSum = bizObject.get(IBussinessConstant.ORI_SUM);//转账金额
        BigDecimal natSum = bizObject.get(IBussinessConstant.NAT_SUM); //本币金额
        BigDecimal exchRate = bizObject.get("exchRate"); //汇率
        Short exchRateOps = bizObject.get("exchRateOps"); //汇率折算方式
        BigDecimal inBrokerage = bizObject.get("inBrokerage");
        BigDecimal outBrokerage = bizObject.get("outBrokerage");
        String useAccentity = bizObject.get(IBussinessConstant.ACCENTITY);
        if(useAccentity == null){
            if(FIDubboUtils.isSingleOrg()){
                useAccentity = FIDubboUtils.getSingleOrg().get("id");
                bizObject.set(IBussinessConstant.ACCENTITY,useAccentity);
            }
        }

        if (exchRate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102439"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013E","汇率不能为空！") /* "汇率不能为空！" */);
        }
        if (exchRate.doubleValue() < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102440"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180141","汇率不能小于0！") /* "汇率不能小于0！" */);
        }
        // 汇率折算方式为空默认为乘
        if (exchRateOps == null) {
            // 默认乘法
            exchRateOps = ExchangeRateMode.FORWARD.getValue().shortValue();
            bizObject.set("exchRateOps", exchRateOps);
        }
        // 会计主体默认币种
        String currencyOrg = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currencyOrg);

        BigDecimal exchangeRateAndAmountCalResult = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, oriSum, null);
        //金额校验前，处理金额精度
        BigDecimal natScaleSum = natSum.setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount());
        BigDecimal calScaleSum = exchangeRateAndAmountCalResult.setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount());
        if (natScaleSum.compareTo(calScaleSum) != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102441"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180144","录入的转账金额、与本币金额和汇率不匹配（转账金额*汇率=本币金额）") /* "录入的转账金额、与本币金额和汇率不匹配（转账金额*汇率=本币金额）" */);
        }

        //手续费
        if(inBrokerage!=null){
            bizObject.set("inBrokerageNatSum",CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, inBrokerage, null));  //转入手续费本币
        }
        if(outBrokerage!=null){
            bizObject.set("outBrokerageNatSum",CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, outBrokerage, null)); //转出手续费本币
        }

        //本币币种
        String natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
        bizObject.set("natCurrency",natCurrency);

        //交易类型及账号
        String tradeTypeCode = bizObject.get("tradetype_code"); //交易类型编码
        switch (tradeTypeCode) {
            case "BT":  //交易类型为银行转账
                bizObject.set("type", "bt");
                if (bizObject.get("payBankAccount") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102442"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180123", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E2", "交易类型为银行转账，付款银行账户为必输项") /* "交易类型为银行转账，付款银行账户为必输项" */) /* "交易类型为银行转账，付款银行账户为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102443"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180124", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E3", "交易类型为银行转账，收款银行账户为必输项") /* "交易类型为银行转账，收款银行账户为必输项" */) /* "交易类型为银行转账，收款银行账户为必输项" */);
                }
                if (bizObject.get("payBankAccount") != null && bizObject.get("recBankAccount") != null &&
                        bizObject.get("payBankAccount").equals(bizObject.get("recBankAccount"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102444"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180127", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CE", "付款与收款账号不能为同一个") /* "付款与收款账号不能为同一个" */) /* "付款与收款账号不能为同一个" */);
                }
                break;
            case "SC":
                bizObject.set("type", "sc");
                if (bizObject.get("payCashAccount_code") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102445"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D2", "交易类型为缴存现金，付款现金账户编码为必输项") /* "交易类型为缴存现金，付款现金账户编码为必输项" */) /* "交易类型为缴存现金，付款现金账户编码为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102446"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D3", "交易类型为缴存现金，收款银行账户为必输项") /* "交易类型为缴存现金，收款银行账户为必输项" */) /* "交易类型为缴存现金，收款银行账户为必输项" */);
                }
                break;
            case "EC":
                bizObject.set("type", "ec");
                if (bizObject.get("payBankAccount") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102447"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180132", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D7", "交易类型为提取现金，付款银行账户为必输项") /* "交易类型为提取现金，付款银行账户为必输项" */) /* "交易类型为提取现金，付款银行账户为必输项" */);
                }
                if (bizObject.get("recCashAccount_code") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102448"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180135", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D9", "交易类型为提取现金，收款现金账户编码为必输项") /* "交易类型为提取现金，收款现金账户编码为必输项" */) /* "交易类型为提取现金，收款现金账户编码为必输项" */);
                }
                break;
            case "CT":
                bizObject.set("type", "ct");
                if (bizObject.get("payCashAccount_code") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102449"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180137", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006DA", "交易类型为现金互转，付款现金账户为必输项") /* "交易类型为现金互转，付款现金账户为必输项" */) /* "交易类型为现金互转，付款现金账户为必输项" */);
                }
                if (bizObject.get("recCashAccount_code") == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102450"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006DB", "交易类型为现金互转，收款现金账户编码为必输项") /* "交易类型为现金互转，收款现金账户编码为必输项" */) /* "交易类型为现金互转，收款现金账户编码为必输项" */);
                }
                break;
            case "TPT":
                bizObject.set("type", "tpt");
                if (bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.BankToVirtual.getValue())) {
                    if (!ValueUtils.isNotEmptyObj(ICmpConstant.PAYBANKACCOUNT)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102451"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013D", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006DC", "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账户为必输项") /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账户为必输项" */) /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账户为必输项" */);
                    }
                    if (!ValueUtils.isNotEmptyObj(ICmpConstant.COLLVIRTUALACCOUNT)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102452"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180140", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006DD", "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，收款虚拟账户为必输项") /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，收款虚拟账户为必输项" */) /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，收款虚拟账户为必输项" */);
                    }
                } else if (bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.VirtualToBank.getValue())) {
                    if (!ValueUtils.isNotEmptyObj(ICmpConstant.RECBANKACCOUNT)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102453"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180143", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006DE", "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账户为必输项") /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账户为必输项" */) /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账户为必输项" */);
                    }
                    if (!ValueUtils.isNotEmptyObj(ICmpConstant.PAYVIRTUALACCOUNT)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102454"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180145", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006DF", "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，付款虚拟账户为必输项") /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，付款虚拟账户为必输项" */) /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，付款虚拟账户为必输项" */);
                    }
                }
                break;
            case "WCZ":  //交易类型为数币钱包充值
                bizObject.set("type", "wcz");
                if (bizObject.get("payBankAccount") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E0", "交易类型为数币钱包充值，付款银行账户为必输项") /* "交易类型为数币钱包充值，付款银行账户为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E1", "交易类型为数币钱包充值，收款银行账户为必输项") /* "交易类型为数币钱包充值，收款银行账户为必输项" */);
                }
                if (bizObject.get("payBankAccount") != null && bizObject.get("recBankAccount") != null &&
                        bizObject.get("payBankAccount").equals(bizObject.get("recBankAccount"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102444"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180127", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CE", "付款与收款账号不能为同一个") /* "付款与收款账号不能为同一个" */) /* "付款与收款账号不能为同一个" */);
                }
                break;
            case "WTX":  //交易类型为数币钱包提现
                bizObject.set("type", "wtx");
                if (bizObject.get("payBankAccount") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CF", "交易类型为数币钱包提现，付款银行账户为必输项") /* "交易类型为数币钱包提现，付款银行账户为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D1", "交易类型为数币钱包提现，收款银行账户为必输项") /* "交易类型为数币钱包提现，收款银行账户为必输项" */);
                }
                if (bizObject.get("payBankAccount") != null && bizObject.get("recBankAccount") != null &&
                        bizObject.get("payBankAccount").equals(bizObject.get("recBankAccount"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102444"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180127", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CE", "付款与收款账号不能为同一个") /* "付款与收款账号不能为同一个" */) /* "付款与收款账号不能为同一个" */);
                }
                break;
            case "WHZ":  //交易类型为数币钱包互转
                bizObject.set("type", "wtx");
                if (bizObject.get("payBankAccount") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D6", "交易类型为数币钱包互转，付款银行账户为必输项") /* "交易类型为数币钱包互转，付款银行账户为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D8", "交易类型为数币钱包划转，收款银行账户为必输项") /* "交易类型为数币钱包划转，收款银行账户为必输项" */);
                }
                if (bizObject.get("payBankAccount") != null && bizObject.get("recBankAccount") != null &&
                        bizObject.get("payBankAccount").equals(bizObject.get("recBankAccount"))) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102444"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180127", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CE", "付款与收款账号不能为同一个") /* "付款与收款账号不能为同一个" */) /* "付款与收款账号不能为同一个" */);
                }
                break;
            default:
                break;
        }

        if(bizObject.get("payBankAccount")!=null){
            EnterpriseBankAcctVO payBankAccount = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("payBankAccount"));
            if (payBankAccount == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180125","查询不到相应账户档案信息，请核实账号") /* "查询不到相应账户档案信息，请核实账号" */);
            }
            boolean existBankAcctCurrencyByBankacc = QueryBaseDocUtils.isExistBankAcctCurrencyByBankacc(bizObject.get("payBankAccount"), bizObject.get("currency"));/* 暂不修改 已登记*/
            if (!existBankAcctCurrencyByBankacc) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102456"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180121","导入付款银行账户与导入币种不符") /* "导入付款银行账户与导入币种不符" */);
            }
            boolean checkBankAccount = checkBankAccount(bizObject.get("payBankAccount"),bizObject.get(IBussinessConstant.ACCENTITY));
            if (!checkBankAccount) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102457"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CDE4D3E05580005","付款银行账户与当前会计主体不符！") /* "付款银行账户与当前会计主体不符！" */);
            }
            bizObject.set("payBankAccount_account",payBankAccount.getAccount());
        }
        if(bizObject.get("recBankAccount")!=null){
            EnterpriseBankAcctVO recBankAccount = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("recBankAccount"));
            if (recBankAccount == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180125","查询不到相应账户档案信息，请核实账号") /* "查询不到相应账户档案信息，请核实账号" */);
            }
            boolean existBankAcctCurrencyByBankacc = QueryBaseDocUtils.isExistBankAcctCurrencyByBankacc(bizObject.get("recBankAccount"), bizObject.get("currency"));/* 暂不修改 已登记*/
            if (!existBankAcctCurrencyByBankacc) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102458"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180129","导入收款银行账户与导入币种不符") /* "导入收款银行账户与导入币种不符" */);
            }
            boolean checkBankAccount = checkBankAccount(bizObject.get("recBankAccount"),bizObject.get(IBussinessConstant.ACCENTITY));
            if (!checkBankAccount) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102459"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CDE4D8E05580003","收款银行账户与当前会计主体不符！") /* "收款银行账户与当前会计主体不符！" */);
            }
            bizObject.set("recBankAccount_account",recBankAccount.getAccount());
        }
        if(bizObject.get("payCashAccount")!=null){
            EnterpriseCashVO enterpriseCashVO = baseRefRpcService.queryEnterpriseCashAcctById(bizObject.getString("payCashAccount"));
            if (enterpriseCashVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180125","查询不到相应账户档案信息，请核实账号") /* "查询不到相应账户档案信息，请核实账号" */);
            }
            if (!bizObject.get("currency").equals(enterpriseCashVO.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102460"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180133","导入付款现金账户与导入币种不符") /* "导入付款现金账户与导入币种不符" */);
            }
            boolean checkCashAccount = checkCashAccount(bizObject.get("payCashAccount"),bizObject.get(IBussinessConstant.ACCENTITY));
            if (!checkCashAccount) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102461"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CDE4DB605580001","付款现金账户与当前会计主体不符！") /* "付款现金账户与当前会计主体不符！" */);
            }
            bizObject.set("payCashAccount_code",enterpriseCashVO.getCode());
        }
        if(bizObject.get("recCashAccount")!=null){
            EnterpriseCashVO enterpriseCashVO = baseRefRpcService.queryEnterpriseCashAcctById(bizObject.getString("recCashAccount"));
            if (enterpriseCashVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180125","查询不到相应账户档案信息，请核实账号") /* "查询不到相应账户档案信息，请核实账号" */);
            }
            if (!bizObject.get("currency").equals(enterpriseCashVO.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102462"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180139","导入收款现金账户与导入币种不符") /* "导入收款现金账户与导入币种不符" */);
            }
            boolean checkCashAccount = checkCashAccount(bizObject.get("recCashAccount"),bizObject.get(IBussinessConstant.ACCENTITY));
            if (!checkCashAccount) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102463"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CDE529405580005","收款现金账户与当前会计主体不符！") /* "收款现金账户与当前会计主体不符！" */);
            }
            bizObject.set("recCashAccount_code",enterpriseCashVO.getCode());
        }

        //项目
        if(!CmpCommonUtil.checkProject(bizObject.getString("project"),bizObject.get(IBussinessConstant.ACCENTITY))){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D9","导入的项目所属使用组织与导入资金组织不一致！") /* "导入的项目所属使用组织与导入资金组织不一致！" */);
        }
    }

    /**
     * 校验导入的银行账号是否属于当前会计主体*
     * @param bankAccountId
     * @return
     */
    private boolean checkBankAccount (String bankAccountId,String accentity) throws Exception {
        try {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            List<String> accountEntities = new ArrayList<>();
            accountEntities.add(accentity);
            enterpriseParams.setOrgidList(new ArrayList<>(accountEntities));
            enterpriseParams.setPageSize(5000);
            List<EnterpriseBankAcctVOWithRange> enterpriseBankAccounts = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
            if (enterpriseBankAccounts == null || enterpriseBankAccounts.size() == 0) {
                return false;
            } else {
                for (EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAccounts) {
                    if (bankAccountId.equals(enterpriseBankAcctVO.getId())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180125","查询不到相应账户档案信息，请核实账号") /* "查询不到相应账户档案信息，请核实账号" */);
        }
    }

    /**
     * 校验导入的现金账号是否属于当前会计主体*
     * @param cashAccountId
     * @return
     */
    private boolean checkCashAccount (String cashAccountId,String accentity) throws Exception {
        try {
            EnterpriseParams params = new EnterpriseParams();
            List<String> accountEntities = new ArrayList<>();
            accountEntities.add(accentity);
            params.setOrgidList(new ArrayList<>(accountEntities));
            params.setPageSize(5000);
            List<EnterpriseCashVO> enterpriseCashVOs = baseRefRpcService.queryEnterpriseCashAcctByCondition(params);
            if (enterpriseCashVOs == null || enterpriseCashVOs.size() == 0) {
                return false;
            } else {
                for (EnterpriseCashVO enterpriseCashVO : enterpriseCashVOs) {
                    if (cashAccountId.equals(enterpriseCashVO.getId())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180125","查询不到相应账户档案信息，请核实账号") /* "查询不到相应账户档案信息，请核实账号" */);
        }
        return false;
    }

    /**
     * 第三方参数校验
     * @param bizObject
     * @return
     */
    private boolean checkParam(BillContext billContext,BizObject bizObject) throws Exception {
        valueIsNull(bizObject.get(IBussinessConstant.ACCENTITY),IBussinessConstant.ACCENTITY);//会计主体.ID
        valueIsNull(bizObject.get(ICmpConstant.VOUCHDATE),ICmpConstant.VOUCHDATE);
        valueIsNull(bizObject.get(ICmpConstant.SRC_ITEM),ICmpConstant.SRC_ITEM);
        valueIsNull(bizObject.get(ICmpConstant.BILLTYPE),ICmpConstant.BILLTYPE);
        if (!ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.TRADETYPE))) {
            //交易类型id 
            TranstypeQueryPageParam transtypeQueryPageParam = new TranstypeQueryPageParam();
            transtypeQueryPageParam.setBillTypeId("FICA4");
            transtypeQueryPageParam.setIsDefault(0);
            transtypeQueryPageParam.setTransTypeCode("TPT");
            transtypeQueryPageParam.setEnable(1);
            transtypeQueryPageParam.setTenantId(AppContext.getYTenantId());
            List<BdTransType> queryTransTypes = baseRefRpcService.queryTransTypeByCondition(transtypeQueryPageParam);
            BdTransType queryTransType =  CollectionUtils.isNotEmpty(queryTransTypes) ? queryTransTypes.get(0) : null;
            if(ValueUtils.isNotEmptyObj(queryTransType)) {
                bizObject.put("tradetype", queryTransType.getId());
                String locale = InvocationInfoProxy.getLocale();
                switch (locale) {
                    case "zh_CN":
                        bizObject.put("tradetype_name", queryTransType.getName());
                        break;
                    case "en_US":
                        bizObject.put("tradetype_name", queryTransType.getName2());
                        break;
                    case "zh_TW":
                        bizObject.put("tradetype_name", queryTransType.getName3());
                        break;
                    default:
                        bizObject.put("tradetype_name", queryTransType.getName());
                }
                bizObject.put("tradetype_code", queryTransType.getCode());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102464"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012B","查询交易类型失败！请检查数据。") /* "查询交易类型失败！请检查数据。" */);
            }
        }
        valueIsNull(bizObject.get(ICmpConstant.VIRTUALBANK),ICmpConstant.VIRTUALBANK);
        valueIsNull(bizObject.get(ICmpConstant.CURRENCY),ICmpConstant.CURRENCY);
        valueIsNull(bizObject.get(ICmpConstant.NATCURRENCY),ICmpConstant.NATCURRENCY);
        valueIsNull(bizObject.get(ICmpConstant.SETTLE_MODE),ICmpConstant.SETTLE_MODE);
        valueIsNull(bizObject.get(ICmpConstant.PAY_STATUS),ICmpConstant.PAY_STATUS);
        valueIsNull(bizObject.get(ICmpConstant.ISSETTLE),ICmpConstant.ISSETTLE);
        valueIsNull(bizObject.get(ICmpConstant.SETTLE_STATUS),ICmpConstant.SETTLE_STATUS);
        valueIsNull(bizObject.get(IBussinessConstant.ORI_SUM),IBussinessConstant.ORI_SUM);

        Integer serviceAttr = commonService.getServiceAttr(bizObject.get(ICmpConstant.SETTLE_MODE));

        if(bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.BankToVirtual.getValue())){
            if(0 != serviceAttr && 1 != serviceAttr){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102465"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013A","结算方式业务属性只能为银行业务与现金业务！") /* "结算方式业务属性只能为银行业务与现金业务！" */);
            }
            valueIsNull(bizObject.get(ICmpConstant.PAYBANKACCOUNT),ICmpConstant.PAYBANKACCOUNT);
            valueIsNull(bizObject.get(ICmpConstant.COLLVIRTUALACCOUNT),ICmpConstant.COLLVIRTUALACCOUNT);
        }else if(bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.VirtualToBank.getValue())){
            if(10 != serviceAttr){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102466"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013C","结算方式的业务属性应为第三方转账") /* "结算方式的业务属性应为第三方转账" */);
            }
            valueIsNull(bizObject.get(ICmpConstant.RECBANKACCOUNT),ICmpConstant.RECBANKACCOUNT);
            valueIsNull(bizObject.get(ICmpConstant.PAYVIRTUALACCOUNT),ICmpConstant.PAYVIRTUALACCOUNT);
        }
        bizObject.set("associationStatusCollect",false);
        bizObject.set("associationStatusPay",false);
        // 汇率类型
        if (!ValueUtils.isNotEmptyObj(bizObject.get("exchangeRateType"))) {
            ExchangeRateTypeVO exchangeRateTypeVO = CmpExchangeRateUtils.getNewExchangeRateType(bizObject.get(IBussinessConstant.ACCENTITY), true);
            if (StringUtils.isNotEmpty(exchangeRateTypeVO.getId())) {
                bizObject.set("exchangeRateType", exchangeRateTypeVO.getId());
                bizObject.set("exchangeRateType_digit", exchangeRateTypeVO.getDigit());
            }
        }

        try {
            boolean isWfControlled = processService.bpmControl(billContext, bizObject);
            bizObject.put("isWfControlled", isWfControlled);
        } catch (Exception e) {
            bizObject.put("isWfControlled", false);
        }
        // 单据编码
        if (!ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.CODE))){
            CmpCommonUtil.billCodeHandler(bizObject, TransferAccount.ENTITY_NAME,"cm_transfer_account");
        }
        return true;
    }

    private void valueIsNull(Object value,String tips){
        if (!ValueUtils.isNotEmptyObj(value)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102467"), MessageUtils.getMessage(tipInfo.get(tips)) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E1", ":字段不能为空!") /* ":字段不能为空!" */);
        }
    }

    /**
     * 第三方参数组装
     * @param bizObject
     */
    private void packageBizObject(BillContext billContext,BizObject bizObject) throws Exception {
        checkParam( billContext,bizObject);
        bizObject.set("paystatus", PayStatus.SupplPaid.getValue());
        bizObject.set("billtype", EventType.ThreePartyReconciliation.getValue());
        bizObject.set("srcitem", EventSource.ThreePartyReconciliation.getValue());
        bizObject.set(ICmpConstant.SETTLE_STATUS, SettleStatus.SettledRep.getValue());
        //通过汇率类型查询汇率
        if (bizObject.get(ICmpConstant.CURRENCY).equals(bizObject.get(ICmpConstant.NATCURRENCY))){
            bizObject.set(ICmpConstant.EXCHRATE, new BigDecimal(1));
            bizObject.set(ICmpConstant.TRANSFER_ACCOUNT_EXCHRATEOPS, (short) 1);
        }else{
            CmpExchangeRateVO exchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bizObject.get(ICmpConstant.CURRENCY), bizObject.get(ICmpConstant.NATCURRENCY), bizObject.get(ICmpConstant.VOUCHDATE), bizObject.get("exchangeRateType"), 6);
            if (exchangeRateVO.getExchangeRate()==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102468"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180142","当前汇率类型下查不到汇率") /* "当前汇率类型下查不到汇率" */);
            }
            bizObject.set(ICmpConstant.EXCHRATE, exchangeRateVO.getExchangeRate());
            bizObject.set(ICmpConstant.TRANSFER_ACCOUNT_EXCHRATEOPS, exchangeRateVO.getExchangeRateOps());
        }
        //计算本币金额
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bizObject.getString(ICmpConstant.CURRENCY));
        BigDecimal orisum = bizObject.get(IBussinessConstant.ORI_SUM);
        if(currencyTenantDTO != null&& orisum !=null){
            orisum.setScale(currencyTenantDTO.getMoneydigit());
            BigDecimal natSum = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(bizObject.get(ICmpConstant.TRANSFER_ACCOUNT_EXCHRATEOPS), bizObject.get(ICmpConstant.EXCHRATE), orisum, currencyTenantDTO.getMoneydigit());
            natSum = natSum.setScale(currencyTenantDTO.getMoneydigit(),currencyTenantDTO.getMoneyrount());
            bizObject.put(ICmpConstant.NATSUM, natSum);
        }
    }

    private List<CheckDTO> setNewValue(BizObject bizObject) {
        List<CheckDTO> checkDTOS = new ArrayList<>();

        CheckDTO checkDTO = new CheckDTO();
        //操作类型： 1.支票锁定/解锁接口、2.支票付票接口、3.支票兑付/背书接口、4.支票作废接口
        checkDTO.setOperationType(CheckOperationType.Lock);

        //锁定类型  锁定状态 1锁定 0解锁
        checkDTO.setLock(CmpLock.YES);

        //支票编号ID
        checkDTO.setCheckBillNo(String.valueOf((Long) bizObject.get("checkid")));
        //业务系统
        checkDTO.setSystem(SystemType.CashManager);
        //业务单据类型
        checkDTO.setBillType(BillType.TransferAccount);
        //业务单据明细ID
        checkDTO.setInputBillNo(bizObject.get("code"));
        //单据方向 2：付款 1：收款(按照结算的枚举来)
        checkDTO.setInputBillDir(CmpInputBillDir.Pay);
        //单据方向，字符串类型
        checkDTO.setInputBillDirString(CmpInputBillDir.Pay.getName());
        //支票状态
        checkDTO.setCheckBillStatus(CmpCheckStatus.Billing.getValue());
        //支票方向
        checkDTO.setCheckBillDir(CheckDirection.Pay.getIndex());

        checkDTOS.add(checkDTO);
        return checkDTOS;
    }

    private List<CheckDTO> setOriginValue(String code, Long checkid) {
        List<CheckDTO> checkDTOS = new ArrayList<>();

        CheckDTO checkDTO = new CheckDTO();

        //操作类型： 1.支票锁定/解锁接口、2.支票付票接口、3.支票兑付/背书接口、4.支票作废接口
        checkDTO.setOperationType(CheckOperationType.Unlock);

        //锁定类型  锁定状态 1锁定 0解锁
        checkDTO.setLock(CmpLock.NO);

        //支票编号ID
        checkDTO.setCheckBillNo(String.valueOf(checkid));
        //业务单据明细ID
        checkDTO.setInputBillNo(code);
        //单据方向 2：付款 1：收款(按照结算的枚举来)
        checkDTO.setInputBillDir(CmpInputBillDir.Pay);
        //单据方向，字符串类型
        checkDTO.setInputBillDirString(CmpInputBillDir.Pay.getName());
        //支票状态:已入库
        checkDTO.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
        //业务系统
        checkDTO.setSystem(SystemType.CashManager);
        //业务单据类型
        checkDTO.setBillType(BillType.TransferAccount);
        //支票方向
        checkDTO.setCheckBillDir(CheckDirection.Pay.getIndex());

        checkDTOS.add(checkDTO);
        return checkDTOS;
    }

    public Short reMatchBudget(TransferAccount transferAccount) throws Exception {
        //1.判断是否是预占成功，如果是删除旧的预占，新增新的预占
        //2.判断是否是预占成功，如果否直接进行新的预占
        TransferAccount oldBill = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferAccount.getId(), 2);
        Short budgeted = oldBill.getIsOccupyBudget();
        //支付变更为支付成功时
        if (transferAccount.getPaystatus() != null && transferAccount.getPaystatus().getValue() == PayStatus.Success.getValue()) {
            if (budgeted == null || budgeted == OccupyBudget.UnOccupy.getValue()) {
                if (transferAccount.getSettledate() != null && cmpBudgetTransferAccountManagerService.implementOnly(transferAccount)) {
                    return OccupyBudget.ActualSuccess.getValue();
                }
            } else if (budgeted == OccupyBudget.PreSuccess.getValue()) {
                if (cmpBudgetTransferAccountManagerService.releaseBudget(oldBill)) {
                    transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    if (transferAccount.getSettledate() != null && cmpBudgetTransferAccountManagerService.implementOnly(transferAccount)) {
                        return OccupyBudget.ActualSuccess.getValue();
                    }
                    return OccupyBudget.UnOccupy.getValue();
                } else {
                    return OccupyBudget.PreSuccess.getValue();
                }
            }
        } else {
            if (budgeted == null || budgeted == OccupyBudget.UnOccupy.getValue()) {
                log.error("ReMatchBudget ，占用预算");
                if (cmpBudgetTransferAccountManagerService.budget(transferAccount)) {
                    return OccupyBudget.PreSuccess.getValue();
                }
            } else if (budgeted == OccupyBudget.PreSuccess.getValue()) {
                if (cmpBudgetTransferAccountManagerService.releaseBudget(oldBill)) {
                    log.error("ReMatchBudget ，删除预算成功");
                    transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    if (cmpBudgetTransferAccountManagerService.budget(transferAccount)) {
                        log.error("ReMatchBudget ，重新占用预算成功");
                        return OccupyBudget.PreSuccess.getValue();
                    } else {
                        log.error("ReMatchBudget ，重新占用预算失败");
                        return OccupyBudget.UnOccupy.getValue();
                    }
                } else {
                    log.error("ReMatchBudget ，删除预算失败");
                }
            }
        }
        return null;
    }

    /**
     * 获取交易类型扩展属性
     * @param bizObject
     * @return
     */
    private String getTradeTypeExt(BizObject bizObject) throws Exception {
        BizObject tradeTypeObj = MetaDaoHelper.findById("bd.bill.TransType", Long.parseLong(bizObject.get("tradetype")), ISchemaConstant.MDD_SCHEMA_TRANSTYPE);
        if (tradeTypeObj == null || tradeTypeObj.get("extend_attrs_json") == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006D4", "交易类型错误，请联系业务人员处理") /* "交易类型错误，请联系业务人员处理" */);
        }
        return (String) CtmJSONObject.parseObject(tradeTypeObj.get("extend_attrs_json")).get("transferType_zz");
    }
}
