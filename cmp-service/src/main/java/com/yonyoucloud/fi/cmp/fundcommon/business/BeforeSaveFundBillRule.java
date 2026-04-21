package com.yonyoucloud.fi.cmp.fundcommon.business;


import com.yonyou.einvoice.service.itf.ITaxRateArchIrisService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.poi.constant.POIConstant;
import com.yonyou.ucf.mdd.ext.poi.dto.ExcelErrorMsgDto;
import com.yonyou.ucf.mdd.ext.sys.service.UserService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.checkStockApply.service.CheckStockApplyService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.digitalwallet.impl.FundcommonWalletHandler;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollectionSubWithholdingRelation;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPaymentSubWithholdingRelation;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.fundpayment.service.FundPaymentService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpInputBillDir;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import com.yonyoucloud.fi.cmp.util.business.BillImportCheckUtil;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.dataSignature.DataSignatureUtil;
import com.yonyoucloud.fi.cmp.util.dataSignature.entity.DataSignatureEntity;
import com.yonyoucloud.fi.drft.api.openapi.ICtmDrftEndorePaybillRpcService;
import com.yonyoucloud.fi.drft.post.vo.base.BaseResultVO;
import com.yonyoucloud.fi.drft.post.vo.output.SettleUseBillResVO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 资金收付款单保存前规则
 *
 * @author mal
 * @version 1.0
 * @since 2022-02-15 16:38
 */
@Slf4j
@Component
public class BeforeSaveFundBillRule extends AbstractCommonRule {

    public static final String FUND_PAYMENT_B_FULLNAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUND_COLLECTION_B_FULLNAME = "cmp.fundcollection.FundCollection_b";

    @Autowired
    BillCopyCheckService billCopyCheckService;

    @Resource
    private IFundCommonService fundCommonService;

    @Resource
    private CmCommonService commonService;

    @Resource
    private ICtmDrftEndorePaybillRpcService ctmDrftEndorePaybillRpcService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CheckStockApplyService checkStockApplyService;

    @Autowired
    UserService userService;

    @Autowired
    FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Autowired
    private FundPaymentService fundPaymentService;

    @Autowired
    private ITaxRateArchIrisService iTaxRateArchIrisService;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private FundcommonWalletHandler fundcommonWalletHandler;

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    @Autowired
    private SettleMethodService settleMethodService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        try {
            List<BizObject> bills = getBills(billContext, paramMap);
            BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
            String billnum = billContext.getBillnum();
            String fullName = billContext.getFullname();

            if (CollectionUtils.isNotEmpty(bills)) {
                if (StringUtils.isEmpty(billnum)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102093"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D9", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
                }
            }
            assert bills != null;
            Map<String, Integer> checkCacheMap = new HashMap<>(256);
            for (BizObject bizObject : bills) {

                String accentity = bizObject.get(IBussinessConstant.ACCENTITY);
                // 先校验期初日期
                checkPeriod(accentity);

                //交易类型停用校验
                CmpCommonUtil.checkTradeTypeEnable(bizObject.get("tradetype"));

                //银行账户的校验
                check(billnum, bizObject);

                //校验资金付款单 结算方式是否直联为直联时，付款银行账户应当满足条件:账户直联开通设置中，该账户的直联授权权限为“查询及支付”。
                checkEmpower(billnum, bizObject);

                // 复制校验
                checkCopy(bizObject, checkCacheMap, billnum, accentity);

                short billtype = bizObject.get("billtype");
                //集中处理客户银行账户、供应商银行账户、员工银行账户参照
                if (billtype == 17 || billtype == 18) {
                    dealCustSuppBankRef(bizObject, billnum);
                }
                // 根据数据的操作类型进行处理
                boolean insert = EntityStatus.Insert.equals(bizObject.getEntityStatus());
                boolean openApiFlag = (bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true))|| billDataDto.getFromApi();

                if (insert) {
                    initUerid(bizObject, openApiFlag);
                    bizObject.set("sourceSystemId", "8");
                    bizObject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
                }

                // 处理支票占用处理
                handleCheckOccupy(bizObject, fullName, insert, billnum);

                Map<Object, FundPayment_b> verifyNotoNoBankNo = new HashMap<>();
                Map<String, BigDecimal> billClaimIds = new HashMap<>();
                Map<String, BigDecimal> bankReconciliationIds = new HashMap<>();

                if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
                    List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                    //填充轧差码
                    fullFundPaymentbZhaChaCode(billbs, PaymentType.FundPayment.getValue());
                    if (insert) {
                        if (CollectionUtils.isNotEmpty(billbs)) {
                            // 新增时，校验生单或关联单据的金额
                            List<BizObject> fundBillPage = new ArrayList<>(billbs);
                            calculateFundBillTotalAmountOfRawOrRelatedInsert(fundBillPage, billClaimIds, bankReconciliationIds);
                            int size = 0;
                            for (FundPayment_b billb : billbs) {
                                DataSignatureEntity dataSignatureEntity =
                                        DataSignatureEntity.builder().opoppositeObjectName(Objects.isNull(billb.getOppositeobjectname())?"":billb.getOppositeobjectname()).
                                        oppositeAccountName(Objects.isNull(billb.getOppositeaccountname())?"":billb.getOppositeaccountname()).tradeAmount(billb.getOriSum()).build();
                                billb.setSignature(DataSignatureUtil.signMsg(dataSignatureEntity));
                                billb.setOppositeobjectname(Objects.isNull(billb.getOppositeobjectname())?"": billb.getOppositeobjectname());
                                billb.setOppositeaccountname(Objects.isNull(billb.getOppositeaccountname())?"": billb.getOppositeaccountname());
                                // ①.Sun GuoCai 2023/8/15 CZFW-152717 资金付款单，结算方式为票据业务，票据方向为应付票据，
                                // 保存时需要校验票据收款人银行账号(showreceiverbankacc)和付款明细的收款方银行账号需要相同。
                                if (ValueUtils.isNotEmptyObj(billb.getNoteno()) && billb.getNoteDirection().getValue() == NoteDirection.PaymentNote.getValue()) {
                                    billb.setOppositeaccountno(Objects.isNull(billb.getOppositeaccountno()) ? null : billb.getOppositeaccountno());
                                    billb.setOppositebankaddr(Objects.isNull(billb.getOppositebankaddr()) ? null : billb.getOppositebankaddr());
                                    billb.setOppositebanklineno(Objects.isNull(billb.getOppositebanklineno()) ? null : billb.getOppositebanklineno());
                                    billb.setOppositebankType(Objects.isNull(billb.getOppositebankType()) ? null : billb.getOppositebankType());
                                    billb.setReceivePartySwift(Objects.isNull(billb.getReceivePartySwift()) ? null : billb.getReceivePartySwift());
                                    verifyNotoNoBankNo.put(billb.getNoteno(), billb);
                                }
                            }
                        }
                    } else {
                        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId());
                        List<FundPayment_b> bizObjects = fundPayment.getBizObjects("FundPayment_b", FundPayment_b.class);
                        // 将list转Map
                        // 审批中单据，编辑保存，先释放，再占用预算；
                        boolean flag = cancelBudget(fundPayment, bizObjects, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_SUBMIT);
                        if (flag) {
                            for (FundPayment_b object : bizObject.getBizObjects("FundPayment_b", FundPayment_b.class)) {
                                object.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                            }
                        }

                        // 更新时，校验生单或关联单据的金额
                        List<BizObject> fundBillDB = new ArrayList<>(bizObjects);
                        List<BizObject> fundBillPage = new ArrayList<>(billbs);
                        calculateFundBillTotalAmountOfRawOrRelatedUpdate(fundBillDB, fundBillPage, billClaimIds, bankReconciliationIds);
                        for (FundPayment_b billb : billbs) {
                            short fundSettlestatus = billb.getFundSettlestatus().getValue();
                            if (FundSettleStatus.Refund.getValue() == fundSettlestatus) {
                                billb.setRefundSum(billb.getOriSum());
                            } else {
                                billb.setRefundSum(null);
                            }
                            DataSignatureEntity dataSignatureEntity =
                                    DataSignatureEntity.builder().opoppositeObjectName(Objects.isNull(billb.getOppositeobjectname())?"":billb.getOppositeobjectname()).
                                            oppositeAccountName(Objects.isNull(billb.getOppositeaccountname())?"":billb.getOppositeaccountname()).tradeAmount(billb.getOriSum()).build();
                            billb.setSignature(DataSignatureUtil.signMsg(dataSignatureEntity));
                            billb.setOppositeobjectname(Objects.isNull(billb.getOppositeobjectname())?"": billb.getOppositeobjectname());
                            billb.setOppositeaccountname(Objects.isNull(billb.getOppositeaccountname())?"": billb.getOppositeaccountname());
                            // ①.Sun GuoCai 2023/8/15 CZFW-152717 资金付款单，结算方式为票据业务，票据方向为应付票据，
                            // 保存时需要校验票据收款人银行账号(showreceiverbankacc)和付款明细的收款方银行账号需要相同。
                            if (ValueUtils.isNotEmptyObj(billb.getNoteno()) && billb.getNoteDirection().getValue() == NoteDirection.PaymentNote.getValue()) {
                                billb.setOppositeaccountno(Objects.isNull(billb.getOppositeaccountno()) ? null : billb.getOppositeaccountno());
                                billb.setOppositebankaddr(Objects.isNull(billb.getOppositebankaddr()) ? null : billb.getOppositebankaddr());
                                billb.setOppositebanklineno(Objects.isNull(billb.getOppositebanklineno()) ? null : billb.getOppositebanklineno());
                                billb.setOppositebankType(Objects.isNull(billb.getOppositebankType()) ? null : billb.getOppositebankType());
                                billb.setReceivePartySwift(Objects.isNull(billb.getReceivePartySwift()) ? null : billb.getReceivePartySwift());
                                verifyNotoNoBankNo.put(billb.getNoteno(), billb);
                            }
                        }
                    }
                    setAssociationcount(bizObject);

                    if (CollectionUtils.isNotEmpty(billbs)) {
                        for (FundPayment_b fundPayment_b : billbs) {
                            //当所选票据为应付票据时
                            if (ObjectUtils.isNotEmpty(fundPayment_b.getNoteDirection()) && 2 == fundPayment_b.getNoteDirection().getValue()) {
                                Long noteno = fundPayment_b.getNoteno();
                                //票据号
                                QuerySchema queryBillNo = QuerySchema.create().addSelect("receiveroles", "showreceiver");
                                queryBillNo.appendQueryCondition(QueryCondition.name("id").eq(noteno));
                                List<Map<String, Object>> billNoList = MetaDaoHelper.query("drft.drftnoteinformation.DrftNoteInformation", queryBillNo, "drft");
                                if (billNoList.size() > 0) {
                                    String receiveroles = billNoList.get(0).get("receiveroles").toString();
                                    if ("3".equals(receiveroles)) {
                                        receiveroles = String.valueOf(CaObject.Other.getValue());
                                    } else if ("4".equals(receiveroles)) {
                                        receiveroles = String.valueOf(CaObject.InnerUnit.getValue());
                                    }
                                    Object showreceiver = billNoList.get(0).get("showreceiver");
                                    //若对方类型、对方档案id和票据上对应的收款方、收款人不一致时，需要校验不允许保存
                                    if ((fundPayment_b.getCaobject() != null && !String.valueOf(fundPayment_b.getCaobject().getValue()).equals(receiveroles))
                                            || (ObjectUtils.isNotEmpty(fundPayment_b.getOppositeobjectname()) && !fundPayment_b.getOppositeobjectname().equals(showreceiver))) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102099"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A6", "当前单据所选应付票据，对方信息和资金结算明细不一致，不允许保存！") /* "当前单据所选应付票据，对方信息和资金结算明细不一致，不允许保存！" */);
                                    }
                                }

                            }
                        }
                    }
                } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {// 资金收款单
                    List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                    //填充轧差码
                    fullFundCollextbZhaChaCode(billbs, PaymentType.FundCollection.getValue());
                    if (insert) {
                        if (CollectionUtils.isNotEmpty(billbs)) {

                            // 新增时，校验生单或关联单据的金额
                            List<BizObject> fundBillPage = new ArrayList<>(billbs);
                            calculateFundBillTotalAmountOfRawOrRelatedInsert(fundBillPage, billClaimIds, bankReconciliationIds);
                        }
                    } else {
                        FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId());
                        List<FundCollection_b> bizObjects = fundCollection.getBizObjects("FundCollection_b", FundCollection_b.class);

                        // 更新时，校验生单或关联单据的金额
                        List<BizObject> fundBillDB = new ArrayList<>(bizObjects);
                        List<BizObject> fundBillPage = new ArrayList<>(billbs);
                        calculateFundBillTotalAmountOfRawOrRelatedUpdate(fundBillDB, fundBillPage, billClaimIds, bankReconciliationIds);
                        for (FundCollection_b billb : billbs) {
                            EntityStatus status = billb.getEntityStatus();
                            if (EntityStatus.Insert.equals(status)) {
                                bizObjects.add(billb);
                            } else if (EntityStatus.Update.equals(status)) {
                                Object billBId = billb.getId();
                                bizObjects.forEach(item -> {
                                    if (ValueUtils.isNotEmptyObj(item.getId()) && item.getId().equals(billBId)) {
                                        item.setOriSum(billb.getOriSum());
                                    }
                                });
                            } else if (EntityStatus.Delete.equals(status)) {
                                Object billBId = billb.getId();
                                if (ValueUtils.isNotEmptyObj(billBId)) {
                                    bizObjects.removeIf(item -> billBId.equals(item.getId()));
                                }
                            }
                        }
                    }
                    setAssociationcount(bizObject);
                }
                // 校验生单或关联单据的金额是否相等
                verifyFundBillTotalAmountOfRawOrRelated(billClaimIds, billnum, bankReconciliationIds);

                //校验金额
//                verifynatSumAndNatSum(bizObject, billnum);

                // ②.Sun GuoCai 2023/8/15 CZFW-152717 资金付款单，结算方式为票据业务，票据方向为应付票据，
                // 保存时需要校验票据收款人银行账号(showreceiverbankacc)和付款明细的收款方银行账号需要相同。
                if (ValueUtils.isNotEmptyObj(verifyNotoNoBankNo)) {
                    verifyNotoNoBankNoWithOppositeaccountnoEqual(verifyNotoNoBankNo);
                }

                // 校验资金计划项目
                checkFundPlanProject(billnum, bizObject);

                // 校验对方账号是否正确
                fundCommonService.checkStaffOppositeAccount(billnum,bizObject);

                //结算简强字段赋值
                fundCommonService.setSimpleSettleValue(billnum, bizObject);

                dealDigit(billnum, bizObject, (bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true))|| billDataDto.getFromApi()||"import".equals(billDataDto.getRequestAction()));

                // 资金收付款单单据更新时，票据号的占用与释放
                fundBillNoteUseOrRelease(billnum, bizObject);

                // 更新预提单的关联关系字段
                updateAccrualsWithholdingWithRelatedinterest(billnum, bizObject);

                // 数币钱包保存校验
                checkDigitalWallet(bizObject,billContext);
            }
            checkCacheMap.clear();
        } catch (Exception e) {
            throw e;
        }
        return new RuleExecuteResult();
    }

    private void dealDigit(String billnum, BizObject bizObject, boolean openApiFlag) {
        if (openApiFlag) {
            int currency_moneyDigit = Objects.nonNull(bizObject.get("currency_moneyDigit")) ? bizObject.getInteger("currency_moneyDigit") : 2;
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
                List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                if (ValueUtils.isNotEmptyObj(billbs)) {
                    for (FundPayment_b fundPaymentB : billbs) {
                        if (ValueUtils.isNotEmptyObj(fundPaymentB.getTaxSum())) {
                            fundPaymentB.setTaxSum(fundPaymentB.getTaxSum().setScale(openApiFlag ? currency_moneyDigit : 8, RoundingMode.HALF_UP));
                        }
                        if (ValueUtils.isNotEmptyObj(fundPaymentB.getUnTaxSum())) {
                            fundPaymentB.setUnTaxSum(fundPaymentB.getUnTaxSum().setScale(openApiFlag ? currency_moneyDigit : 8, RoundingMode.HALF_UP));
                        }
                        if (ValueUtils.isNotEmptyObj(fundPaymentB.getIncludeTaxSum())) {
                            fundPaymentB.setIncludeTaxSum(fundPaymentB.getIncludeTaxSum().setScale(openApiFlag ? currency_moneyDigit : 8, RoundingMode.HALF_UP));
                        }
                    }
                }
            } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {// 资金收款单
                List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                if (ValueUtils.isNotEmptyObj(billbs)) {
                    for (FundCollection_b fundCollectionB : billbs) {
                        if (ValueUtils.isNotEmptyObj(fundCollectionB.getTaxSum())) {
                            fundCollectionB.setTaxSum(fundCollectionB.getTaxSum().setScale(openApiFlag ? currency_moneyDigit : 8, RoundingMode.HALF_UP));
                        }
                        if (ValueUtils.isNotEmptyObj(fundCollectionB.getUnTaxSum())) {
                            fundCollectionB.setUnTaxSum(fundCollectionB.getUnTaxSum().setScale(openApiFlag ? currency_moneyDigit : 8, RoundingMode.HALF_UP));
                        }
                        if (ValueUtils.isNotEmptyObj(fundCollectionB.getIncludeTaxSum())) {
                            fundCollectionB.setIncludeTaxSum(fundCollectionB.getIncludeTaxSum().setScale(openApiFlag ? currency_moneyDigit : 8, RoundingMode.HALF_UP));
                        }
                    }
                }
            }
        }
    }

    private void fullFundPaymentbZhaChaCode(List<FundPayment_b> billbs, short afterNetDir) {
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (ValueUtils.isEmpty(billbs) || enableSimplify) {
            return;
        }

        List<FundPayment_b> fundPaymentBs = billbs.stream().filter(v -> {
            return StringUtils.isNotBlank(v.getNetIdentificateCode()) && !EntityStatus.Delete.equals(v.getEntityStatus());
        }).collect(Collectors.toList());
        if (ValueUtils.isEmpty(fundPaymentBs)) {
            //清空轧差码其他属性
            billbs.forEach(v -> {
                v.setNetSettleCount(null);
                v.setAfterNetAmt(null);
                v.setAfterNetDir(null);
                v.setNetIdentificateCode(null);
            });
            return;
        }
        Map<String, List<FundPayment_b>> groupZhachaMap = fundPaymentBs.stream().collect(Collectors.groupingBy(FundPayment_b::getNetIdentificateCode));
        Map<String, Short> netSettleCountMap = new HashMap<>();//轧差结算总笔数
        Map<String, BigDecimal> afterNetAmtMap = new HashMap<>();//轧差后金额
        for (String key : groupZhachaMap.keySet()) {
            List<FundPayment_b> fundPaymentBList = groupZhachaMap.get(key);
            if (ValueUtils.isEmpty(fundPaymentBList)) {
                continue;
            }
            short count = Short.parseShort(fundPaymentBList.size() + "");
            netSettleCountMap.put(key, count);
            BigDecimal oriSum = BigDecimal.ZERO;
            for (FundPayment_b fundPaymentB : fundPaymentBList) {
                oriSum = BigDecimalUtils.safeAdd(oriSum, fundPaymentB.getOriSum());
            }
            afterNetAmtMap.put(key, oriSum);
        }
        billbs.forEach(v -> {
            if (!EntityStatus.Delete.equals(v.getEntityStatus())) {
                String key = v.getNetIdentificateCode();
                if (StringUtils.isEmpty(key)) {
                    v.setNetSettleCount(null);
                    v.setAfterNetAmt(null);
                    v.setAfterNetDir(null);
                    v.setNetIdentificateCode(null);
                } else {
                    short count = netSettleCountMap.get(key);
                    BigDecimal oriSum = afterNetAmtMap.get(key);
                    v.setNetSettleCount(count);
                    v.setAfterNetAmt(oriSum);
                    v.setAfterNetDir(afterNetDir);
                }
            }
        });
    }

    private void fullFundCollextbZhaChaCode(List<FundCollection_b> billbs, short afterNetDir) {
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (ValueUtils.isEmpty(billbs) || enableSimplify) {
            return;
        }
        List<FundCollection_b> fundPaymentBs = billbs.stream().filter(v -> {
            return StringUtils.isNotBlank(v.getNetIdentificateCode()) && !EntityStatus.Delete.equals(v.getEntityStatus());
        }).collect(Collectors.toList());
        if (ValueUtils.isEmpty(fundPaymentBs)) {
            //清空轧差码其他属性
            billbs.forEach(v -> {
                v.setNetSettleCount(null);
                v.setAfterNetAmt(null);
                v.setAfterNetDir(null);
                v.setNetIdentificateCode(null);
            });
            return;
        }
        Map<String, List<FundCollection_b>> groupZhachaMap = fundPaymentBs.stream().collect(Collectors.groupingBy(FundCollection_b::getNetIdentificateCode));
        Map<String, Short> netSettleCountMap = new HashMap<>();//轧差结算总笔数
        Map<String, BigDecimal> afterNetAmtMap = new HashMap<>();//轧差后金额
        for (String key : groupZhachaMap.keySet()) {
            List<FundCollection_b> fundPaymentBList = groupZhachaMap.get(key);
            if (ValueUtils.isEmpty(fundPaymentBList)) {
                continue;
            }
            short count = Short.parseShort(fundPaymentBList.size() + "");
            netSettleCountMap.put(key, count);
            BigDecimal oriSum = BigDecimal.ZERO;
            for (FundCollection_b fundPaymentB : fundPaymentBList) {
                oriSum = BigDecimalUtils.safeAdd(oriSum, fundPaymentB.getOriSum());
            }
            afterNetAmtMap.put(key, oriSum);
        }
        billbs.forEach(v -> {
            if (!EntityStatus.Delete.equals(v.getEntityStatus())) {
                String key = v.getNetIdentificateCode();
                if (StringUtils.isEmpty(key)) {
                    v.setNetSettleCount(null);
                    v.setAfterNetAmt(null);
                    v.setAfterNetDir(null);
                    v.setNetIdentificateCode(null);
                } else {
                    short count = netSettleCountMap.get(key);
                    BigDecimal oriSum = afterNetAmtMap.get(key);
                    v.setNetSettleCount(count);
                    v.setAfterNetAmt(oriSum);
                    v.setAfterNetDir(afterNetDir);
                }
            }
        });
    }

    /**
     * 数币钱包检查
     * @param bizObject
     * @throws Exception
     */
    private void checkDigitalWallet(BizObject bizObject, BillContext billContext) throws Exception {
        List<BizObject> billbs;
        // 获取子表的属性
        if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {
            billbs =  bizObject.get("FundCollection_b");
        } else {
            // 资金付款单
            billbs = bizObject.get("FundPayment_b");
        }
        // 结算方式业务属性为银行业务
        for (BizObject billb : billbs) {
            // 删除状态跳过
            if (billb.get("_status") != null && "Delete".equals(billb.get("_status").toString())){
                continue;
            }
            // 本方银行账户或对方银行账户任一为空跳过
            if (StringUtils.isEmpty(billb.getString("enterprisebankaccount")) || StringUtils.isEmpty(billb.getString("oppositeaccountno"))) {
                continue;
            }
            // 统收统支业务跳过
            Boolean isIncomeAndExpenditure = billb.getBoolean("isIncomeAndExpenditure");
            if (isIncomeAndExpenditure != null && isIncomeAndExpenditure) {
                continue;
            }
            boolean isBankBusiness = fundcommonWalletHandler.checkSettleAttrOfEnterprise(billb);
            if (!isBankBusiness && fundcommonWalletHandler.checkEnterpriseAccountIsDw(billb)) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C8", "结算方式的业务属性为银行业务才能使用数币钱包！") /* "结算方式的业务属性为银行业务才能使用数币钱包！" */);
            }
            if (!isBankBusiness) {
                return;
            }
            fundcommonWalletHandler.checkSave(billb);
        }
    }

    /**
     * 校验资金付款单 结算方式是否直联为直联时，付款银行账户应当满足条件:账户直联开通设置中，该账户的直联授权权限为“查询及支付”。
     *
     * @param billnum   单据编号，用于区分是资金付款单还是资金收款单
     * @param bizObject 业务对象，包含具体的单据明细数据
     * @throws Exception 如果校验不通过，抛出异常提示用户修改
     */
    private void checkEmpower(String billnum, BizObject bizObject) throws Exception {
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
            List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
            if (ValueUtils.isNotEmptyObj(billbs)) {
                //CM202400731 结算方式的是否直联=直联时，付款银行账户应当满足条件:账户直联开通设置中直联授权权限为“查询及支付”=是
                List<String> bankAccountList = bankAccountSettingService.queryBankAccountSettingByDirect();
                Set<String> bankAccountSet = billbs.stream()
                        .map(billb -> billb.getEnterprisebankaccount())
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());
                List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = enterpriseBankQueryService.findByIdList(new ArrayList<>(bankAccountSet));
                Map<String, EnterpriseBankAcctVO> enterpriseBankAcctVOMap = enterpriseBankAcctVOList.stream()
                        .collect(Collectors.toMap(EnterpriseBankAcctVO::getId, Function.identity()));
                for (FundPayment_b fundPaymentB : billbs) {
                    if (ObjectUtils.isNotEmpty(fundPaymentB.getSettlemode()) && settleMethodService.checkSettleMethod(fundPaymentB.getSettlemode().toString())
                            && !bankAccountList.contains(fundPaymentB.getEnterprisebankaccount()) && Objects.nonNull(enterpriseBankAcctVOMap.get(fundPaymentB.getEnterprisebankaccount()))
                            && AcctopenTypeEnum.SettlementCenter.getValue() != enterpriseBankAcctVOMap.get(fundPaymentB.getEnterprisebankaccount()).getAcctopentype()) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_226DA5A004180000", "结算方式的是否直联=直联时，付款银行账户应当满足条件:账户直联开通设置中直联授权权限为“查询及支付”=是") /* "结算方式的是否直联=直联时，付款银行账户应当满足条件:账户直联开通设置中直联授权权限为“查询及支付”=是！" */);
                    }
                }
            }
        }
    }

    /**
     * 校验资金付款单或资金收款单中的账户信息是否存在前后空格问题。
     *
     * @param billnum   单据编号，用于区分是资金付款单还是资金收款单
     * @param bizObject 业务对象，包含具体的单据明细数据
     * @throws Exception 如果校验不通过，抛出异常提示用户修改
     */
    private void check(String billnum, BizObject bizObject) throws Exception {
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
            List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
            if (ValueUtils.isNotEmptyObj(billbs)) {
                for (FundPayment_b fundPaymentB : billbs) {
                    //公共校验
                    CmpCommonUtil.checkQuickTypeEnable(fundPaymentB.getQuickType());
                    if (fundPaymentB.getCaobject().equals(CaObject.Customer) || fundPaymentB.getCaobject().equals(CaObject.Supplier)) {
                        fundCommonService.checkCaObjectAccountNoEqual(fundPaymentB.getCaobject().getValue(), fundPaymentB.getOppositeaccountid(), fundPaymentB.getOppositeaccountno());
                    }
                    if ((fundPaymentB.getEntityStatus() == null || !fundPaymentB.getEntityStatus().equals(EntityStatus.Delete))
                            && fundPaymentB.getCaobject().equals(CaObject.Other)) {
                        String oppositeaccountname = fundPaymentB.getOppositeaccountname();
                        if (!StringUtils.isEmpty(oppositeaccountname)) {
                            String oppositeaccountnameTrim = oppositeaccountname.trim();
                            if (oppositeaccountname.length() != oppositeaccountnameTrim.length()) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102094"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A4", "保存失败：收款账户名称最前面或最后面存在空格。收款账户名称:") /* "保存失败：收款账户名称最前面或最后面存在空格。收款账户名称:" */ + oppositeaccountname);
                            }
                        }
                        String oppositeaccountno = fundPaymentB.getOppositeaccountno();
                        if (StringUtils.isEmpty(oppositeaccountno)) {
                            continue;
                        }
                        String oppositeaccountnoTrim = oppositeaccountno.trim();
                        if (oppositeaccountno.length() != oppositeaccountnoTrim.length()) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102095"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A9", "保存失败：收款银行账号最前面或最后面存在空格。收款银行账号:") /* "保存失败：收款银行账号最前面或最后面存在空格。收款银行账号:" */ + oppositeaccountno);
                        }
                    }
                }
            }
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {// 资金收款单
            List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
            if (ValueUtils.isNotEmptyObj(billbs)) {
                for (FundCollection_b fundCollectionB : billbs) {
                    //公共校验
                    CmpCommonUtil.checkQuickTypeEnable(fundCollectionB.getQuickType());
                    if (fundCollectionB.getCaobject().equals(CaObject.Customer) || fundCollectionB.getCaobject().equals(CaObject.Supplier)) {
                        fundCommonService.checkCaObjectAccountNoEqual(fundCollectionB.getCaobject().getValue(), fundCollectionB.getOppositeaccountid(), fundCollectionB.getOppositeaccountno());
                    }
                    if ((fundCollectionB.getEntityStatus() == null || !fundCollectionB.getEntityStatus().equals(EntityStatus.Delete))
                            && fundCollectionB.getCaobject().equals(CaObject.Other)) {
                        String oppositeaccountname = fundCollectionB.getOppositeaccountname();
                        if (!StringUtils.isEmpty(oppositeaccountname)) {
                            String oppositeaccountnameTrim = oppositeaccountname.trim();
                            if (oppositeaccountname.length() != oppositeaccountnameTrim.length()) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102096"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A0", "保存失败：付款账户名称最前面或最后面存在空格。付款账户名称:") /* "保存失败：付款账户名称最前面或最后面存在空格。付款账户名称:" */ + oppositeaccountname);
                            }
                        }
                        String oppositeaccountno = fundCollectionB.getOppositeaccountno();
                        if (StringUtils.isEmpty(oppositeaccountno)) {
                            continue;
                        }
                        String oppositeaccountnoTrim = oppositeaccountno.trim();
                        if (oppositeaccountno.length() != oppositeaccountnoTrim.length()) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102097"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A5", "保存失败：付款银行账号最前面或最后面存在空格。付款银行账号:") /* "保存失败：付款银行账号最前面或最后面存在空格。付款银行账号:" */ + oppositeaccountno);
                        }
                    }
                }
            }
        }
    }

    /**
     * 校验金额
     *
     * @param bizObject
     * @param billnum
     * @throws Exception
     */
    private static void verifynatSumAndNatSum(BizObject bizObject, String billnum) throws Exception {
        //主子表金额校验
        BigDecimal exchRate = bizObject.getBigDecimal("exchRate");
        BigDecimal natSum = bizObject.getBigDecimal("natSum");
        BigDecimal oriSum = bizObject.getBigDecimal("oriSum");
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum) || IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            List<BizObject> billbs = null;
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                billbs = bizObject.getBizObjects("FundPayment_b", BizObject.class);
            } else {
                billbs = bizObject.getBizObjects("FundCollection_b", BizObject.class);
            }
            BigDecimal totalOriAmount = BigDecimal.ZERO;
            BigDecimal totalNatAmount = BigDecimal.ZERO;
            int natCurrency_moneyDigit = 2;
            if (bizObject.get("natCurrency_moneyDigit") != null) {
                natCurrency_moneyDigit = bizObject.getInteger("natCurrency_moneyDigit");
            }
            for (int i = 0; i < billbs.size(); i++) {
                if ("Delete".equals(billbs.get(i).getEntityStatus().name())) {
                    continue;
                }
                BigDecimal bOriSum = billbs.get(i).get("oriSum");
                BigDecimal bNatSum = billbs.get(i).get("natSum");
                BigDecimal checkNatSum;
                if (billbs.get(i).getShort("exchangeRateOps")!=null && billbs.get(i).getShort("exchangeRateOps") == 1) {
                    checkNatSum = BigDecimalUtils.safeMultiply(exchRate, bOriSum, 2);
                    if (bNatSum.setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(checkNatSum) != 0) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FC583B204B80000", "本币金额不等于\"原币金额*汇率\"，请修改金额后，重新保存！") /* "本币金额不等于"原币金额*汇率"，请修改金额后，重新保存！" */);
                    }
                } else if(billbs.get(i).getShort("exchangeRateOps")!=null && billbs.get(i).getShort("exchangeRateOps") == 2){
                    checkNatSum = BigDecimalUtils.safeDivide(bOriSum,exchRate, 2);
                    if (bNatSum.setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(checkNatSum) != 0) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211C47CE05500008", "本币金额不等于\"原币金额/汇率\"，请修改金额后，重新保存！") /* "本币金额不等于"原币金额/汇率"，请修改金额后，重新保存！" */);
                    }
                }else{
                    checkNatSum = BigDecimalUtils.safeMultiply(exchRate, bOriSum, 2);
                    if (bNatSum.setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(checkNatSum) != 0) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FC583B204B80000", "本币金额不等于\"原币金额*汇率\"，请修改金额后，重新保存！") /* "本币金额不等于"原币金额*汇率"，请修改金额后，重新保存！" */);
                    }
                }
                totalOriAmount = totalOriAmount.add(bOriSum);
                totalNatAmount = totalNatAmount.add(bNatSum);
            }
            if (totalOriAmount.setScale(natCurrency_moneyDigit, BigDecimal.ROUND_HALF_UP).compareTo(oriSum.setScale(natCurrency_moneyDigit, BigDecimal.ROUND_HALF_UP)) != 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FC4BC8E05C00009", "原币金额与明细汇总原币金额不相等,请修改金额后，重新保存!") /* "本币金额不等于"原本金额*汇率"，请修改金额后，重新保存！" */);
            }
            if (totalNatAmount.setScale(natCurrency_moneyDigit, BigDecimal.ROUND_HALF_UP).compareTo(natSum.setScale(natCurrency_moneyDigit, BigDecimal.ROUND_HALF_UP)) != 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FC5943404B80000", "本币金额与明细汇总本币金额不相等,请修改金额后，重新保存!") /* "本币金额不等于"原本金额*汇率"，请修改金额后，重新保存！" */);
            }
        }
    }

    /**
     * 处理支票占用
     *
     * @param bizObject
     * @param fullName
     * @param insert
     * @param billnum
     * @throws Exception
     */
    private void handleCheckOccupy(BizObject bizObject, String fullName, boolean insert, String billnum) throws Exception {
        List<String> releaseCheckId = new ArrayList();//需要释放的支票id
        List<String> occupyCheckId = new ArrayList();//需要进行预占的支票id

        // 处理编辑的情况
        BizObject currentBill = MetaDaoHelper.findById(fullName, bizObject.getId(), ICmpConstant.CONSTANT_TWO);
        if (!insert && currentBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383", "单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
        }
        List<String> dbCheckid = new ArrayList<>();
        List<String> checkid = new ArrayList<>();
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
            List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
            if (insert) {
                for (FundPayment_b billb : billbs) {
                    if (billb.getCheckId() != null) {
                        occupyCheckId.add(billb.getCheckId());
                    }
                }
            } else {
                List<FundPayment_b> currentbillbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                for (FundPayment_b billb : currentbillbs) {
                    if (billb.getEntityStatus() == EntityStatus.Delete && !Objects.isNull(billb.getCheckId())) {
                        releaseCheckId.add(billb.getCheckId());
                    }
                    if (billb.getEntityStatus() == EntityStatus.Insert && !Objects.isNull(billb.getCheckId())) {
                        occupyCheckId.add(billb.getCheckId());
                    }
                }
                List<FundPayment_b> oriBillbs = currentBill.getBizObjects("FundPayment_b", FundPayment_b.class);
                // 防御性编程：避免 getBizObjects 返回 null 导致 NPE
                if (billbs == null) billbs = Collections.emptyList();
                if (oriBillbs == null) oriBillbs = Collections.emptyList();
                // 将原始数据转换为Map以提高查找效率
                Map<String, FundPayment_b> oriBillMap = new HashMap<>();
                for (FundPayment_b oriBill : oriBillbs) {
                    if (oriBill != null && oriBill.getId() != null) {
                        oriBillMap.put(oriBill.getId(), oriBill);
                    }
                }
                // 遍历当前页面的明细数据
                for (FundPayment_b currentPageBill : billbs) {
                    if (currentPageBill == null || currentPageBill.getId() == null) {
                        continue;
                    }
                    FundPayment_b oriBill = oriBillMap.get(currentPageBill.getId());
                    if (oriBill == null) {
                        continue;
                    }
                    String currentPageCheckId = currentPageBill.getCheckId();
                    String oriCheckId = oriBill.getCheckId();
                    if (StringUtils.isEmpty(currentPageCheckId)) {
                        if (StringUtils.isNotEmpty(oriCheckId)) {
                            releaseCheckId.add(oriCheckId);
                        }
                    } else {
                        if (!currentPageCheckId.equals(oriCheckId)) {
                            occupyCheckId.add(currentPageCheckId);
                            if (StringUtils.isNotEmpty(oriCheckId)) {
                                releaseCheckId.add(oriCheckId);
                            }
                        }
                    }
                }
            }
            // 支票占用
            Set<String> checkIdList = new HashSet<>();
            List<CheckStock> checkStockList = new ArrayList<>();
            int size = 0;
            for (FundPayment_b billb : billbs) {
                // 支票占用
                if (ValueUtils.isNotEmptyObj(billb.getCheckId()) && !EntityStatus.Delete.equals(billb.getEntityStatus())) {
                    size++;
                    checkIdList.add(billb.getCheckId());
                }
            }
            if (CollectionUtils.isNotEmpty(checkIdList) && size != checkIdList.size()) {
                throw new CtmException( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A8", "多条明细不能使用同一个支票！") /* "多条明细不能使用同一个支票！" */);
            }
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {// 资金收款单
            List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
            for (FundCollection_b billb : billbs) {
                if (billb.getCheckId() != null && (EntityStatus.Insert.equals(billb.getEntityStatus()) || EntityStatus.Update.equals(billb.getEntityStatus()))) {
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, billb.getCheckId());
                    //CM202400696 根据支票锁定接口的入参，当传参的处理方式为锁定，且支票编号为收到方向的支票时，校验其票面金额与单据金额是否一致，不一致时，提示：操作失败,支票编号[SRZP0002]、[SRZP0003]票面金额与单据金额不符
                    if (!Objects.isNull(billb.getOriSum()) && CmpInputBillDir.Receive.getValue().equals(checkStock.getCheckBillDir())
                            && billb.getOriSum().compareTo(checkStock.getAmount()) != 0) {
                        log.error("操作失败,支票编号[%s]票面金额与单据金额不符！");
                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_212E98E604680006", "操作失败,支票编号[%s]票面金额与单据金额不符！") /* "操作失败,支票编号[%s]票面金额与单据金额不符！" */, checkStock.getCheckBillNo()));
                    }
                }
            }
            if (insert) {
                for (FundCollection_b billb : billbs) {
                    if (billb.getCheckId() != null) {
                        occupyCheckId.add(billb.getCheckId());
                    }
                }
            } else {
                List<FundCollection_b> currentbillbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                for (FundCollection_b billb : currentbillbs) {
                    if (billb.getEntityStatus() == EntityStatus.Delete && !Objects.isNull(billb.getCheckId())) {
                        releaseCheckId.add(billb.getCheckId());
                    }
                    if (billb.getEntityStatus() == EntityStatus.Insert && !Objects.isNull(billb.getCheckId())) {
                        occupyCheckId.add(billb.getCheckId());
                    }
                }
                List<FundCollection_b> oriBillbs = currentBill.getBizObjects("FundCollection_b", FundCollection_b.class);
                // 防御性编程：避免 getBizObjects 返回 null 导致 NPE
                if (billbs == null) billbs = Collections.emptyList();
                if (oriBillbs == null) oriBillbs = Collections.emptyList();
                // 将原始数据转换为Map以提高查找效率
                Map<String, FundCollection_b> oriBillMap = new HashMap<>();
                for (FundCollection_b oriBill : oriBillbs) {
                    if (oriBill != null && oriBill.getId() != null) {
                        oriBillMap.put(oriBill.getId(), oriBill);
                    }
                }
                // 遍历当前页面的明细数据
                for (FundCollection_b currentPageBill : billbs) {
                    if (currentPageBill == null || currentPageBill.getId() == null) {
                        continue;
                    }
                    FundCollection_b oriBill = oriBillMap.get(currentPageBill.getId());
                    if (oriBill == null) {
                        continue;
                    }
                    String currentPageCheckId = currentPageBill.getCheckId();
                    String oriCheckId = oriBill.getCheckId();
                    if (StringUtils.isEmpty(currentPageCheckId)) {
                        if (StringUtils.isNotEmpty(oriCheckId)) {
                            releaseCheckId.add(oriCheckId);
                        }
                    } else {
                        if (!currentPageCheckId.equals(oriCheckId)) {
                            occupyCheckId.add(currentPageCheckId);
                            if (StringUtils.isNotEmpty(oriCheckId)) {
                                releaseCheckId.add(oriCheckId);
                            }
                        }
                    }
                }
            }

            // 支票占用
            Set<String> checkIdList = new HashSet<>();
            List<CheckStock> checkStockList = new ArrayList<>();
            int size = 0;
            for (FundCollection_b billb : billbs) {
                // 支票占用
                if (ValueUtils.isNotEmptyObj(billb.getCheckId()) && !EntityStatus.Delete.equals(billb.getEntityStatus())) {
                    size++;
                    checkIdList.add(billb.getCheckId());
                }
            }
            if (CollectionUtils.isNotEmpty(checkIdList) && size != checkIdList.size()) {
                throw new CtmException( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A8", "多条明细不能使用同一个支票！") /* "多条明细不能使用同一个支票！" */);
            }
        }
//        if (hasDuplicateIds(occupyCheckId)) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102100"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A1", "所选的支票编号已占用，请重新选择！"));
//        }
        CtmJSONObject param = new CtmJSONObject();
        if (CollectionUtils.isNotEmpty(occupyCheckId)) {
            for (String checkId : occupyCheckId) {
                CheckStock addCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                if (ValueUtils.isNotEmptyObj(addCheckStock) && addCheckStock.getOccupy() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102100"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A1", "所选的支票编号已占用，请重新选择！") /* "所选的支票编号已占用，请重新选择！" */);
                }
            }
            param.put("newCheckId", occupyCheckId);
        }
        if (CollectionUtils.isNotEmpty(releaseCheckId)) {
            param.put("oldCheckId", releaseCheckId);
        }
        checkStockApplyService.checkStockOccupy(param);
    }

    /**
     * 判断i是否重复
     *
     * @param idList
     * @return
     */
    public boolean hasDuplicateIds(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return false;
        }
        Set<String> idSet = new HashSet<>();
        for (String id : idList) {
            if (!idSet.add(id)) {
                // 如果添加失败，说明已存在，即有重复
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化用户id
     *
     * @param bizObject
     * @param openApiFlag
     * @throws Exception
     */
    private void initUerid(BizObject bizObject, boolean openApiFlag) throws Exception {
        if (openApiFlag) {
            // 来源于openapi的数据，如果传递了用户id，则需要赋值userId
            if (bizObject.get("userId") != null) {
                Map<String,Object> user = commonService.getUserIdByYhtUserId(bizObject.get("userId").toString());
                if (user != null) {
                    // 创建人名称 - 前端显示
                    bizObject.set("creator", user.get("name"));
                    bizObject.set("userId", user.get("id"));
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80097", "未查询到数据!") /* "未查询到数据!" */);
                }
            }
        } else {
            // 手工新增的从上下文取用户id
            bizObject.set("userId", InvocationInfoProxy.getUserid());
        }
    }


    /**
     * 校验期初日期
     *
     * @param accentity
     * @throws Exception
     */
    private void checkPeriod(String accentity) throws Exception {
        Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);
        if (enabledBeginData == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101870"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F4", "该资金组织现金管理模块未启用，不能保存单据！"));
        }
    }

    private static void verifyFundBillTotalAmountOfRawOrRelated(Map<String, BigDecimal> billClaimIds, String
            billnum, Map<String, BigDecimal> bankReconciliationIds) throws Exception {
        if (ValueUtils.isNotEmpty(billClaimIds)) {
            for (Map.Entry<String, BigDecimal> decimalEntry : billClaimIds.entrySet()) {
                String id = decimalEntry.getKey();
                BigDecimal oriSum = decimalEntry.getValue();
                BigDecimal totalamount = null;
                BizObject billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, id, 1);
                if (ValueUtils.isNotEmptyObj(billClaim)) {
                    totalamount = billClaim.getBigDecimal("totalamount");
                } else {
                    QuerySchema schema = QuerySchema.create().addSelect("totalamount");
                    QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                            QueryCondition.name("refbill").eq(id));
                    schema.addCondition(conditionGroup);
                    Map<String, Object> billClaimMap = MetaDaoHelper.queryOne(BillClaim.ENTITY_NAME, schema);
                    totalamount = ValueUtils.isNotEmptyObj(billClaimMap) ? new BigDecimal(billClaimMap.get("totalamount").toString()) : BigDecimal.ZERO;
                }
                totalamount = totalamount.abs();
                oriSum = oriSum.abs();
                if (totalamount.compareTo(oriSum) != 0) {
                    if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                        log.error("verifyFundBillTotalAmountOfRawOrRelated, billClaimIds={}, totalamount={}, oriSum={}",
                                billClaimIds, totalamount, oriSum);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102101"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A3", "当前单据收款金额不等于认领单金额，不允许进行保存, 单据总金额为：") /* "当前单据收款金额不等于认领单金额，不允许进行保存, 单据总金额为：" */ + totalamount);
                    } else {
                        log.error("verifyFundBillTotalAmountOfRawOrRelated, billClaimIds={}, totalamount={},oriSum={}",
                                billClaimIds, totalamount, oriSum);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102102"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800A7", "当前单据付款金额不等于认领单金额，不允许进行保存, 单据总金额为：") /* "当前单据付款金额不等于认领单金额，不允许进行保存, 单据总金额为：" */ + totalamount);
                    }
                }

            }
        }

        if (ValueUtils.isNotEmpty(bankReconciliationIds)) {
            List<String> bankReconciliationIdList = bankReconciliationIds.keySet().stream().collect(Collectors.toList());
            Map<Long, BankReconciliation> bankReconciliationMap =  getBankReconciliationMap(bankReconciliationIdList);
            for (Map.Entry<String, BigDecimal> decimalEntry : bankReconciliationIds.entrySet()) {
                String id = decimalEntry.getKey();
                BigDecimal oriSum = decimalEntry.getValue();
                BizObject bankReconciliation = bankReconciliationMap.get(Long.parseLong(id));
                if (ValueUtils.isNotEmptyObj(bankReconciliation)) {
                    BigDecimal tranAmt = bankReconciliation.getBigDecimal("tran_amt");
                    //正数场景，逻辑保持原样
                    if (tranAmt.compareTo(BigDecimal.ZERO) > 0) {
                        if (tranAmt.compareTo(oriSum) != 0) {
                            if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                                log.error("verifyFundBillTotalAmountOfRawOrRelated, bankReconciliationIds={}, tranAmt={}, oriSum={}",
                                        bankReconciliationIds, tranAmt, oriSum);
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102103"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CC343404D80003", "当前单据收款金额不等于银行对账单，不允许进行保存, 单据总金额为：") /* "当前单据收款金额不等于银行对账单，不允许进行保存, 单据总金额为：" */ + tranAmt);
                            } else {
                                log.error("verifyFundBillTotalAmountOfRawOrRelated, bankReconciliationIds={}, tranAmt={}, oriSum={}",
                                        bankReconciliationIds, tranAmt, oriSum);
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102104"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219243F605080005", "当前单据付款金额不等于银行流水，不允许进行保存, 单据总金额为：") /* "当前单据付款金额不等于银行流水，不允许进行保存, 单据总金额为：" */ + tranAmt);
                            }
                        }
                    } else {
                        //负数场景下，关联关系回写接口校验总额时，按照金额绝对值进行校验
                        tranAmt = tranAmt.abs();
                        oriSum = oriSum.abs();
                        if (tranAmt.compareTo(oriSum) != 0) {
                            if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                                log.error("verifyFundBillTotalAmountOfRawOrRelated, bankReconciliationIds={}, tranAmt={}, oriSum={}",
                                        bankReconciliationIds, tranAmt, oriSum);
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102103"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CC343404D80003", "当前单据收款金额不等于银行对账单，不允许进行保存, 单据总金额为：") /* "当前单据收款金额不等于银行对账单，不允许进行保存, 单据总金额为：" */ + tranAmt);
                            } else {
                                log.error("verifyFundBillTotalAmountOfRawOrRelated, bankReconciliationIds={}, tranAmt={}, oriSum={}",
                                        bankReconciliationIds, tranAmt, oriSum);
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102104"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_219243F605080005", "当前单据付款金额不等于银行流水，不允许进行保存, 单据总金额为：") /* "当前单据付款金额不等于银行流水，不允许进行保存, 单据总金额为：" */ + tranAmt);
                            }
                        }
                    }

                }
            }
        }
    }


    /**
     * 根据流水id查询流水详情
     *
     * @param bankreconciliationIds
     * @return
     */
    private static Map<Long, BankReconciliation> getBankReconciliationMap(List<String> bankreconciliationIds) throws Exception {
        Map<Long, BankReconciliation> bankReconciliationMap = new HashMap<>();
        List<BankReconciliation> bankReconciliations = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankreconciliationIds)) {
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("id").in(bankreconciliationIds));
            bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        }
        if(CollectionUtils.isNotEmpty(bankReconciliations)){
            for(BankReconciliation bankReconciliation : bankReconciliations){
                bankReconciliationMap.put(bankReconciliation.getId(), bankReconciliation);
            }
        }
        return bankReconciliationMap;
    }


    private void calculateFundBillTotalAmountOfRawOrRelatedInsert
            (List<BizObject> billbs, Map<String, BigDecimal> billClaimIds, Map<String, BigDecimal> bankReconciliationIds) {
        for (BizObject billb : billbs) {
            if (ValueUtils.isNotEmptyObj(billb.getShort("associationStatus"))
                    && billb.getShort("associationStatus") == AssociationStatus.Associated.getValue()) {
                if (ValueUtils.isNotEmptyObj(billb.getString("billClaimId"))) {
                    BigDecimal bigDecimal = ValueUtils.isNotEmptyObj(billClaimIds.get(billb.getString("billClaimId")))
                            ? billClaimIds.get(billb.getString("billClaimId")) : BigDecimal.ZERO;
                    billClaimIds.put(billb.getString("billClaimId"), BigDecimalUtils.safeAdd(billb.getBigDecimal("swapOutAmountEstimate"), bigDecimal));
                }
                if (ValueUtils.isNotEmptyObj(billb.getString("bankReconciliationId"))) {
                    BigDecimal bigDecimal = ValueUtils.isNotEmptyObj(bankReconciliationIds.get(billb.getString("bankReconciliationId")))
                            ? bankReconciliationIds.get(billb.getString("bankReconciliationId")) : BigDecimal.ZERO;
                    bankReconciliationIds.put(billb.getString("bankReconciliationId"), BigDecimalUtils.safeAdd(billb.getBigDecimal("swapOutAmountEstimate"), bigDecimal));
                }
            }
        }
    }

    private void calculateFundBillTotalAmountOfRawOrRelatedUpdate
            (List<BizObject> bizObjects, List<BizObject> billbs, Map<String, BigDecimal> billClaimIds, Map<String, BigDecimal> bankReconciliationIds) {
        Map<String, BigDecimal> billClaimSumMap = bizObjects.stream()
                .filter(item -> ValueUtils.isNotEmptyObj(item.getString("billClaimId"))
                        && item.getShort("associationStatus") == AssociationStatus.Associated.getValue())
                .collect(Collectors.groupingBy(item -> item.getString("billClaimId"),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getBigDecimal("swapOutAmountEstimate"),
                                BigDecimal::add)));

        Map<String, BigDecimal> bankReconciliationSumMap = bizObjects.stream()
                .filter(item -> ValueUtils.isNotEmptyObj(item.getString("bankReconciliationId"))
                        && item.getShort("associationStatus") == AssociationStatus.Associated.getValue())
                .collect(Collectors.groupingBy(item -> item.getString("bankReconciliationId"),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getBigDecimal("swapOutAmountEstimate"),
                                BigDecimal::add)));
        billClaimIds.putAll(billClaimSumMap);
        bankReconciliationIds.putAll(bankReconciliationSumMap);
        Map<Long, BigDecimal> generateBillOriginalSumMap = bizObjects.stream()
                .collect(Collectors.toMap(item -> Long.valueOf(item.getId().toString()), item -> item.getBigDecimal("swapOutAmountEstimate")));
        for (BizObject billb : billbs) {
            if (ValueUtils.isNotEmptyObj(billb.getShort("associationStatus"))
                    && billb.getShort("associationStatus") == AssociationStatus.Associated.getValue()) {
                EntityStatus status = billb.getEntityStatus();
                if (EntityStatus.Insert.equals(status)) {
                    if (ValueUtils.isNotEmptyObj(billb.getString("billClaimId"))) {
                        BigDecimal sum = ValueUtils.isNotEmptyObj(billClaimIds.get(billb.getString("billClaimId")))
                                ? billClaimIds.get(billb.getString("billClaimId")) : billClaimSumMap.get(billb.getString("billClaimId"));
                        billClaimIds.put(billb.getString("billClaimId"), BigDecimalUtils.safeAdd(billb.getBigDecimal("swapOutAmountEstimate"), sum));
                    }
                    if (ValueUtils.isNotEmptyObj(billb.getString("bankReconciliationId"))) {
                        BigDecimal sum = ValueUtils.isNotEmptyObj(bankReconciliationIds.get(billb.getString("bankReconciliationId")))
                                ? bankReconciliationIds.get(billb.getString("bankReconciliationId")) : bankReconciliationSumMap.get(billb.getString("bankReconciliationId"));
                        bankReconciliationIds.put(billb.getString("bankReconciliationId"), BigDecimalUtils.safeAdd(billb.getBigDecimal("swapOutAmountEstimate"), sum));
                    }
                } else if (EntityStatus.Update.equals(status)) {
                    BigDecimal swapOutAmountEstimatePage = billb.getBigDecimal("swapOutAmountEstimate");
                    Long id = Long.valueOf(billb.getId().toString());
                    BigDecimal swapOutAmountEstimateDB = generateBillOriginalSumMap.get(id);
                    if (swapOutAmountEstimatePage.compareTo(swapOutAmountEstimateDB) != 0) {
                        if (ValueUtils.isNotEmptyObj(billb.getString("billClaimId"))) {
                            BigDecimal sum = ValueUtils.isNotEmptyObj(billClaimIds.get(billb.getString("billClaimId")))
                                    ? billClaimIds.get(billb.getString("billClaimId")) : billClaimSumMap.get(billb.getString("billClaimId"));
                            billClaimIds.put(billb.getString("billClaimId"),
                                    BigDecimalUtils.safeAdd(sum, BigDecimalUtils.safeSubtract(swapOutAmountEstimatePage, swapOutAmountEstimateDB)));
                        }
                        if (ValueUtils.isNotEmptyObj(billb.getString("bankReconciliationId"))) {
                            BigDecimal sum = ValueUtils.isNotEmptyObj(bankReconciliationIds.get(billb.getString("bankReconciliationId")))
                                    ? bankReconciliationIds.get(billb.getString("bankReconciliationId")) : bankReconciliationSumMap.get(billb.getString("bankReconciliationId"));
                            bankReconciliationIds.put(billb.getString("bankReconciliationId"),
                                    BigDecimalUtils.safeAdd(sum, BigDecimalUtils.safeSubtract(swapOutAmountEstimatePage, swapOutAmountEstimateDB)));
                        }
                    }
                } else if (EntityStatus.Delete.equals(status)) {
                    if (ValueUtils.isNotEmptyObj(billb.getString("billClaimId"))) {
                        BigDecimal sum = ValueUtils.isNotEmptyObj(billClaimIds.get(billb.getString("billClaimId")))
                                ? billClaimIds.get(billb.getString("billClaimId")) : BigDecimal.ZERO;
                        BigDecimal amountSum = BigDecimalUtils.safeSubtract(sum, billb.getBigDecimal("swapOutAmountEstimate"));
                        if (BigDecimal.ZERO.compareTo(amountSum) == 0) {
                            Iterator<Map.Entry<String, BigDecimal>> iterator = billClaimIds.entrySet().iterator();
                            while (iterator.hasNext()) {
                                String key = iterator.next().getKey();
                                if (billb.getString("billClaimId").equals(key)) {
                                    iterator.remove();
                                }
                            }
                        } else {
                            billClaimIds.put(billb.getString("billClaimId"), amountSum);
                        }
                    }
                    if (ValueUtils.isNotEmptyObj(billb.getString("bankReconciliationId"))) {
                        BigDecimal sum = ValueUtils.isNotEmptyObj(bankReconciliationIds.get(billb.getString("bankReconciliationId")))
                                ? bankReconciliationIds.get(billb.getString("bankReconciliationId")) : BigDecimal.ZERO;
                        BigDecimal amountSum = BigDecimalUtils.safeSubtract(sum, billb.getBigDecimal("swapOutAmountEstimate"));
                        if (BigDecimal.ZERO.compareTo(amountSum) == 0) {
                            Iterator<Map.Entry<String, BigDecimal>> iterator = bankReconciliationIds.entrySet().iterator();
                            while (iterator.hasNext()) {
                                String key = iterator.next().getKey();
                                if (billb.getString("bankReconciliationId").equals(key)) {
                                    iterator.remove();
                                }
                            }
                        } else {
                            bankReconciliationIds.put(billb.getString("bankReconciliationId"), amountSum);
                        }
                    }
                }
            }
        }
    }

    private void verifyNotoNoBankNoWithOppositeaccountnoEqual(Map<Object, FundPayment_b> verifyNotoNoBankNo) throws
            Exception {
        Set<Object> notoNoSet = verifyNotoNoBankNo.keySet();
        BillContext context = new BillContext();
        context.setFullname("drft.drftnoteinformation.DrftNoteInformation");
        context.setDomain("yonbip-fi-ctmdrft");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id, receiverbankaccbycust, receiverbankaccbysupp, receiverbankaccbyorg, receiveraccbyobject, showreceiverbankacc");
        schema.appendQueryCondition(QueryCondition.name("id").in(notoNoSet));
        log.info("getObjectContent, schema = {}", schema);
        List<Map<String, Object>> resultList = MetaDaoHelper.query(context, schema);

        // 获取账户信息对象
        if (CollectionUtils.isNotEmpty(resultList)) {
            for (Map<String, Object> map : resultList) {
                Object noteNo = map.get("id");

                short caObject = verifyNotoNoBankNo.get(noteNo).getCaobject().getValue();
                boolean flag = false;
                if (caObject == CaObject.Customer.getValue()) {
                    Object receiverbankaccbycust = map.get("receiverbankaccbycust");
                    flag = ValueUtils.isNotEmptyObj(receiverbankaccbycust) &&
                            ValueUtils.isNotEmptyObj(verifyNotoNoBankNo.get(noteNo)) &&
                            !receiverbankaccbycust.toString().equals(verifyNotoNoBankNo.get(noteNo).getOppositeaccountid());
                } else if (caObject == CaObject.Supplier.getValue()) {
                    Object receiverbankaccbysupp = map.get("receiverbankaccbysupp");
                    flag = ValueUtils.isNotEmptyObj(receiverbankaccbysupp) &&
                            ValueUtils.isNotEmptyObj(verifyNotoNoBankNo.get(noteNo)) &&
                            !receiverbankaccbysupp.toString().equals(verifyNotoNoBankNo.get(noteNo).getOppositeaccountid());
                } else if (caObject == CaObject.InnerUnit.getValue()) {
                    Object receiverbankaccbyorg = map.get("receiverbankaccbyorg");
                    flag = ValueUtils.isNotEmptyObj(receiverbankaccbyorg) &&
                            ValueUtils.isNotEmptyObj(verifyNotoNoBankNo.get(noteNo)) &&
                            !receiverbankaccbyorg.toString().equals(verifyNotoNoBankNo.get(noteNo).getOppositeaccountid());
                } else if (caObject == CaObject.CapBizObj.getValue()) {
                    Object receiveraccbyobject = map.get("receiveraccbyobject");
                    flag = ValueUtils.isNotEmptyObj(receiveraccbyobject) &&
                            ValueUtils.isNotEmptyObj(verifyNotoNoBankNo.get(noteNo)) &&
                            !receiveraccbyobject.toString().equals(verifyNotoNoBankNo.get(noteNo).getOppositeaccountid());
                } else if (caObject == CaObject.Other.getValue()) {
                    Object showreceiverbankacc = map.get("showreceiverbankacc");
                    flag = ValueUtils.isNotEmptyObj(showreceiverbankacc) &&
                            ValueUtils.isNotEmptyObj(verifyNotoNoBankNo.get(noteNo)) &&
                            !showreceiverbankacc.toString().equals(verifyNotoNoBankNo.get(noteNo).getOppositeaccountno());
                }
                if (flag) {
                    Integer lineNo = ValueUtils.isNotEmptyObj(verifyNotoNoBankNo.get(noteNo).get("lineno"))
                            ? new BigDecimal(verifyNotoNoBankNo.get(noteNo).get("lineno").toString()).intValue() : -1;
                    String errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E653960460000F", "行号为[%s]明细行，票据收款人银行账号和付款明细的收款方银行账号需要相同") /* "行号为[%s]明细行，票据收款人银行账号和付款明细的收款方银行账号需要相同" */
                            , lineNo);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102105"), errorMsg);
                }
            }
        }
    }

    private void updateAccrualsWithholdingWithRelatedinterest(String billnum, BizObject bizObject) throws Exception {
        Set<Long> deleteIds = new HashSet<>();
        if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            List<BizObject> fundCollectionBChildList = bizObject.get(ICmpConstant.FUND_COLLECTION_B);
            BizObject originFundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME,bizObject.getId(),3);
            if (CollectionUtils.isNotEmpty(fundCollectionBChildList)) {
                for (BizObject iter : fundCollectionBChildList) {
                    Object quickType = iter.get(ICmpConstant.QUICK_TYPE);
                    // 判断款项类型是否为利息
                    if (fundCommonService.isInterestWithQuickType(quickType)) {
                        // 判断是否为明细行删除
                        if (EntityStatus.Delete.equals(iter.getEntityStatus())) {
                            QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.WITHHOLDING_ID);
                            QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.FUND_COLLECTION_SUB_ID).eq(iter.getId()));
                            querySchema.addCondition(queryConditionGroup);
                            List<Map<String, Object>> fundCollectionSubWithholdingRelation = MetaDaoHelper.query(FundCollectionSubWithholdingRelation.ENTITY_NAME, querySchema, null);
                            //List<Map<String, Object>> fundCollectionSubWithholdingRelation = iter.get("FundCollectionSubWithholdingRelation");
                            for (Map<String, Object> childBizObj : fundCollectionSubWithholdingRelation) {
                                deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID).toString()));
                            }
                        } else {
                            // 此为孙表的数据添加和删除
                            List<BizObject> fundCollectionSubWithholdingRelation = iter.get(ICmpConstant.FUND_COLLECTION_SUB_WITHHOLDING_RELATION);
                            if (CollectionUtils.isNotEmpty(fundCollectionSubWithholdingRelation)) {
                                for (BizObject childBizObj : fundCollectionSubWithholdingRelation) {
                                    if (EntityStatus.Delete.equals(childBizObj.getEntityStatus())) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                    }
                                }
                            }
                        }
                    }
                    List<BizObject> deleteFundCollectionSubWithholdingRelationIdList = new ArrayList<>();
                    if (EntityStatus.Update.equals(iter.getEntityStatus())) {
                        //FundCollection_b bizObj = MetaDaoHelper.findById(FundCollection_b.ENTITY_NAME, iter.getId(), ICmpConstant.CONSTANT_TWO);
                        List<FundCollection_b> originFundCollectionBList = originFundCollection.get("FundCollection_b");
                        FundCollection_b bizObj  = originFundCollectionBList.stream()
                                .filter(item -> item.getId() != null
                                        && iter.get("id") != null
                                        && item.getId().toString().equals(iter.get("id").toString()))
                                .findFirst()
                                .orElse(null);
                        List<BizObject> fundPaymentSubWithholdingRelation = bizObj != null ? bizObj.get(ICmpConstant.FUND_COLLECTION_SUB_WITHHOLDING_RELATION) : null;
                        // 1.修改时，款项类型由结息账户转为其他时，清空当前明细的孙表数据，以及修改关联预提单的关联状态
                        if (!fundCommonService.isInterestWithQuickType(quickType) && fundCommonService.isInterestWithQuickType(bizObj.getQuickType())) {
                            if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                    deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                    deleteFundCollectionSubWithholdingRelationIdList.add(childBizObj);
                                }
                            }
                        }
                        if (fundCommonService.isInterestWithQuickType(quickType)) {
                            // 2.修改时，结息账户为空或切换结息账户，清空当前明细的孙表数据，以及修改关联预提单的关联状态
                            if (!ValueUtils.isNotEmptyObj(iter.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT)) || !iter.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT).equals(bizObj.getInterestSettlementAccount())) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundCollectionSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                            // 3.修改时，币种切换后，清空当前明细的孙表数据，以及修改关联预提单的关联状态
                            Object currency = bizObject.get(ICmpConstant.CURRENCY);
                            if (ValueUtils.isNotEmptyObj(currency) && !bizObj.get(ICmpConstant.CURRENCY).equals(currency)) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundCollectionSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                            // 4.修改时，本次结息结束日与数据库的本次结息结束日不相等时，清空关联预提单孙表数据
                            Date currentInterestSettlementEndDate = iter.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE);
                            long currentInterestSettlementEndDateWithPageLongTime = ValueUtils.isNotEmptyObj(currentInterestSettlementEndDate)
                                    ? currentInterestSettlementEndDate.getTime() : 1L;
                            long currentInterestSettlementEndDateWithDbLongTime = ValueUtils.isNotEmptyObj(bizObj.getCurrentInterestSettlementEndDate())
                                    ? bizObj.getCurrentInterestSettlementEndDate().getTime() : 1L;
                            if (currentInterestSettlementEndDateWithPageLongTime != currentInterestSettlementEndDateWithDbLongTime) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundCollectionSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                            // 5.修改时，本次结息开始日与数据库的本次结息开始日不相等时，清空关联预提单孙表数据
                            Date currentInterestSettlementStartDate = iter.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_START_DATE);
                            long currentInterestSettlementStartDateWithPageLongTime = ValueUtils.isNotEmptyObj(currentInterestSettlementStartDate)
                                    ? currentInterestSettlementStartDate.getTime() : 1L;
                            long currentInterestSettlementStartDateWithDbLongTime = ValueUtils.isNotEmptyObj(bizObj.getCurrentInterestSettlementStartDate())
                                    ? bizObj.getCurrentInterestSettlementStartDate().getTime() : 1L;
                            if (currentInterestSettlementStartDateWithPageLongTime != currentInterestSettlementStartDateWithDbLongTime) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundCollectionSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                        }
                    }
                    // 批量删除结息单孙表数据
                    if (CollectionUtils.isNotEmpty(deleteFundCollectionSubWithholdingRelationIdList)) {
                        EntityTool.setUpdateStatus(deleteFundCollectionSubWithholdingRelationIdList);
                        MetaDaoHelper.delete(FundCollectionSubWithholdingRelation.ENTITY_NAME, deleteFundCollectionSubWithholdingRelationIdList);
                    }
                }
            }
        } else if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            List<BizObject> fundPaymentBChildList = bizObject.get(ICmpConstant.FUND_PAYMENT_B);
            BizObject originFundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME,bizObject.getId(),3);
            if (CollectionUtils.isNotEmpty(fundPaymentBChildList)) {
                for (BizObject iter : fundPaymentBChildList) {
                    Object quickType = iter.get(ICmpConstant.QUICK_TYPE);
                    // 判断款项类型是否为利息
                    if (fundCommonService.isInterestWithQuickType(quickType)) {
                        // 判断是否为明细行删除
                        if (EntityStatus.Delete.equals(iter.getEntityStatus())) {
                            QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.WITHHOLDING_ID);
                            QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.FUND_PAYMENT_SUB_ID).eq(iter.getId()));
                            querySchema.addCondition(queryConditionGroup);
                            List<Map<String, Object>> fundPaymentSubWithholdingRelation = MetaDaoHelper.query(FundPaymentSubWithholdingRelation.ENTITY_NAME, querySchema, null);
                            for (Map<String, Object> childBizObj : fundPaymentSubWithholdingRelation) {
                                deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID).toString()));
                            }
                        } else {
                            // 此为孙表的数据添加和删除
                            List<BizObject> fundPaymentSubWithholdingRelation = iter.get(ICmpConstant.FUND_PAYMENT_SUB_WITHHOLDING_RELATION);
                            if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                    if (EntityStatus.Delete.equals(childBizObj.getEntityStatus())) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                    }
                                }
                            }
                        }
                    }
                    List<BizObject> deleteFundPaymentSubWithholdingRelationIdList = new ArrayList<>();
                    if (EntityStatus.Update.equals(iter.getEntityStatus())) {
                        //FundPayment_b bizObj = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, iter.getId(), ICmpConstant.CONSTANT_TWO);
                        List<FundPayment_b> originFundPaymentBList = originFundPayment.get("FundPayment_b");
                        FundPayment_b bizObj = originFundPaymentBList.stream()
                                .filter(item -> item.getId() != null
                                        && iter.get("id") != null
                                        && item.getId().toString().equals(iter.get("id").toString()))
                                .findFirst()
                                .orElse(null);
                        // 1.修改时，交易类型由结息账户转为其他时，清空当前明细的孙表数据，以及修改关联预提单的关联状态
                        List<BizObject> fundPaymentSubWithholdingRelation = bizObj != null ? bizObj.get(ICmpConstant.FUND_PAYMENT_SUB_WITHHOLDING_RELATION) : null;
                        if (!fundCommonService.isInterestWithQuickType(quickType) && fundCommonService.isInterestWithQuickType(bizObj.getQuickType())) {
                            if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                    deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                    deleteFundPaymentSubWithholdingRelationIdList.add(childBizObj);
                                }
                            }
                        }
                        if (fundCommonService.isInterestWithQuickType(quickType)) {
                            // 2.修改时，结息账户为空或切换结息账户，清空当前明细的孙表数据，以及修改关联预提单的关联状态
                            if (!ValueUtils.isNotEmptyObj(iter.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT)) || !iter.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT).equals(bizObj.getInterestSettlementAccount())) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundPaymentSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                            // 3.修改时，币种切换后，清空当前明细的孙表数据，以及修改关联预提单的关联状态
                            Object currency = bizObject.get(ICmpConstant.CURRENCY);
                            if (ValueUtils.isNotEmptyObj(currency) && !bizObj.get(ICmpConstant.CURRENCY).equals(currency)) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundPaymentSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                            // 4.修改时，本次结息结束日与数据库的本次结息结束日不相等时，清空关联预提单孙表数据
                            Date currentInterestSettlementEndDate = iter.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE);
                            long currentInterestSettlementEndDateWithPageLongTime = ValueUtils.isNotEmptyObj(currentInterestSettlementEndDate)
                                    ? currentInterestSettlementEndDate.getTime() : 1L;
                            long currentInterestSettlementEndDateWithDbLongTime = ValueUtils.isNotEmptyObj(bizObj.getCurrentInterestSettlementEndDate())
                                    ? bizObj.getCurrentInterestSettlementEndDate().getTime() : 1L;
                            if (currentInterestSettlementEndDateWithPageLongTime != currentInterestSettlementEndDateWithDbLongTime) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundPaymentSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                            // 5.修改时，本次结息开始日与数据库的本次结息开始日不相等时，清空关联预提单孙表数据
                            Date currentInterestSettlementStartDate = iter.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_START_DATE);
                            long currentInterestSettlementStartDateWithPageLongTime = ValueUtils.isNotEmptyObj(currentInterestSettlementStartDate)
                                    ? currentInterestSettlementStartDate.getTime() : 1L;
                            long currentInterestSettlementStartDateWithDbLongTime = ValueUtils.isNotEmptyObj(bizObj.getCurrentInterestSettlementStartDate())
                                    ? bizObj.getCurrentInterestSettlementStartDate().getTime() : 1L;
                            if (currentInterestSettlementStartDateWithPageLongTime != currentInterestSettlementStartDateWithDbLongTime) {
                                if (CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                                    for (BizObject childBizObj : fundPaymentSubWithholdingRelation) {
                                        deleteIds.add(Long.parseLong(childBizObj.get(ICmpConstant.WITHHOLDING_ID)));
                                        deleteFundPaymentSubWithholdingRelationIdList.add(childBizObj);
                                    }
                                }
                            }
                        }
                    }
                    // 批量删除结息单孙表数据
                    if (CollectionUtils.isNotEmpty(deleteFundPaymentSubWithholdingRelationIdList)) {
                        EntityTool.setUpdateStatus(deleteFundPaymentSubWithholdingRelationIdList);
                        MetaDaoHelper.delete(FundPaymentSubWithholdingRelation.ENTITY_NAME, deleteFundPaymentSubWithholdingRelationIdList);
                    }
                }
            }
        }
        // 孙表删除时，更新预提单记录表关联状态字段为未关联
        if (CollectionUtils.isNotEmpty(deleteIds)) {
            List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME,
                    "*", deleteIds.toArray(new Long[0]));
            List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
            for (Map<String, Object> map : accrualsWithholdingMap) {
                AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                accrualsWithholding.init(map);
                // 更新结息单的【关联结息单】字段状态为未关联
                accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
                accrualsWithholding.setSrcbillmainid(null);
                accrualsWithholding.setSrcbillnum(null);
                accrualsWithholding.setSrcbilltype(null);
                accrualsWithholdingList.add(accrualsWithholding);
            }
            EntityTool.setUpdateStatus(accrualsWithholdingList);
            MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);
        }
    }

    private void fundBillNoteUseOrRelease(String billnum, BizObject bizObject) throws Exception {
        List<BizObject> releaseBillMap = new ArrayList<>();
        List<BizObject> useBillMap = new ArrayList<>();
        Integer billDirection = null;
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            List<Map<String, Object>> fundPaymentBChildList = bizObject.get("FundPayment_b");
            if (CollectionUtils.isEmpty(fundPaymentBChildList)) {
                return;
            }
            BizObject originFundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME,bizObject.getId(),3);
            // 票据校验放这 资金付款  分包流转=是的应收票据 可以关联多个明细，其他票只能关联一个明细
            List<String> notSubNoteList = new ArrayList<>();
            for (Map<String, Object> iter : fundPaymentBChildList) {
                if (iter.get("mainid") == null) {
                    iter.put("mainid", bizObject.getId());
                }
                if (iter.get("id") == null) {
                    iter.put("id", ymsOidGenerator.nextId());
                }
            }
            for (Map<String, Object> iter : fundPaymentBChildList) {
                if (iter.get("_status") == null || (iter.get("_status") != null && !"Delete".equals(iter.get("_status").toString()))) {
                    if (ValueUtils.isNotEmptyObj(iter.get("settlemode"))) {
                        Integer serviceAttrPage = commonService.getServiceAttr(Long.parseLong(iter.get("settlemode").toString()));
                        if (IStwbConstant.SERVICEATTR_DIRT == serviceAttrPage && ValueUtils.isNotEmptyObj(iter.get("noteno"))) {
                            Long noteNo = ValueUtils.isNotEmptyObj(iter.get("noteno")) ? Long.parseLong(iter.get("noteno").toString()) : -1L;
                            // 用于报错提示
                            String noteNoNote = ValueUtils.isNotEmptyObj(iter.get("noteno_noteno")) ? iter.get("noteno_noteno").toString() : "";
                            // 需要校验，一个票据id只能关联一个明细，否则给出提示“票据号XXX，仅允许关联一个明细”
                            if (noteNo != -1L) {
                                if (notSubNoteList.contains(noteNo.toString())) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102107"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186B1AEC04C00019", "票据号%s，仅允许关联一个明细") /* "票据号%s，仅允许关联一个明细" */, noteNoNote));
                                }
                                if ((Short.parseShort(iter.get("noteDirection").toString()) == NoteDirection.CollectionNote.getValue()
                                        && (Objects.isNull(iter.get("isSubcontract")) || !BooleanUtils.b(iter.get("isSubcontract"))))
                                        || Short.parseShort(iter.get("noteDirection").toString()) == NoteDirection.PaymentNote.getValue()) {
                                    notSubNoteList.add(noteNo.toString());
                                }
                            }
                        }
                    }
                }
                if (iter.get("_status") != null && "Update".equals(iter.get("_status").toString())) {
                    FundPayment_b fundPaymentSub = new FundPayment_b();
                    try {
                        List<FundPayment_b> originFundPaymentBList = originFundPayment.get("FundPayment_b");
                        fundPaymentSub = originFundPaymentBList.stream()
                                .filter(item -> item.getId() != null
                                        && iter.get("id") != null
                                        && item.getId().toString().equals(iter.get("id").toString()))
                                .findFirst()
                                .orElse(null);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102106"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DB", "查询资金付款单异常") /* "查询资金付款单异常" */);
                    }
                    long notenoPage = ValueUtils.isNotEmptyObj(iter.get("noteno")) ? Long.parseLong(iter.get("noteno").toString()) : -1L;
                    long notenoDB = ValueUtils.isNotEmptyObj(fundPaymentSub) && ValueUtils.isNotEmptyObj(fundPaymentSub.getNoteno()) ? fundPaymentSub.getNoteno() : -1L;

                    BigDecimal oriSumPage = ValueUtils.isNotEmptyObj(iter.get("oriSum")) ? new BigDecimal(iter.get("oriSum").toString()) : BigDecimal.ZERO;
                    BigDecimal oriSum = ValueUtils.isNotEmptyObj(fundPaymentSub) && ValueUtils.isNotEmptyObj(fundPaymentSub.getOriSum()) ? fundPaymentSub.getOriSum() : BigDecimal.ZERO;

                    if (notenoDB != notenoPage) {
                        if (notenoPage != -1L && notenoDB != -1L) {
                            // 占用
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                useBillMap.add(new BizObject(iter));
                            }
                            // 释放
                            if (BigDecimal.ZERO.compareTo(oriSum) < 0) {
                                releaseBillMap.add(fundPaymentSub);
                            }
                        }
                        if (notenoPage != -1L && notenoDB == -1L) {
                            // 占用
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                useBillMap.add(new BizObject(iter));
                            }
                        }
                        if (notenoPage == -1L) {
                            // 释放
                            if (BigDecimal.ZERO.compareTo(oriSum) < 0) {
                                releaseBillMap.add(fundPaymentSub);
                            }
                        }
                    } else if (notenoDB == notenoPage && notenoPage != -1L) {
                        // 修改金额
                        if (oriSumPage.compareTo(oriSum) != 0) {
                            // 占用
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                useBillMap.add(new BizObject(iter));
                            }
                            // 释放
                            if (BigDecimal.ZERO.compareTo(oriSum) < 0) {
                                releaseBillMap.add(fundPaymentSub);
                            }
                        }
                    }

                } else if (iter.get("_status") != null && "Delete".equals(iter.get("_status").toString())) {
                    FundPayment_b fundPaymentSub;
                    try {
                        List<FundPayment_b> originFundPaymentBList = originFundPayment.get("FundPayment_b");
                        fundPaymentSub = originFundPaymentBList.stream()
                                .filter(item -> item.getId() != null
                                        && iter.get("id") != null
                                        && item.getId().toString().equals(iter.get("id").toString()))
                                .findFirst()
                                .orElse(null);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102106"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DB", "查询资金付款单异常") /* "查询资金付款单异常" */);
                    }
                    releaseBillMap.add(fundPaymentSub);
                }
            }
            billDirection = 2;
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            List<Map<String, Object>> fundCollectionBChildList = bizObject.get("FundCollection_b");
            if (CollectionUtils.isEmpty(fundCollectionBChildList)) {
                return;
            }
            List<Long> notSubNoteList = new ArrayList<>();
            BizObject originFundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME,bizObject.getId(),3);
            for (Map<String, Object> iter : fundCollectionBChildList) {
                if (iter.get("mainid") == null) {
                    iter.put("mainid", bizObject.getId());
                }
                if (iter.get("id") == null) {
                    iter.put("id", ymsOidGenerator.nextId());
                }
            }
            for (Map<String, Object> iter : fundCollectionBChildList) {
                // 票据校验放这
                if (iter.get("_status") == null
                        || "Update".equals(iter.get("_status").toString())
                        || "Insert".equals(iter.get("_status").toString())) {
                    if (ValueUtils.isNotEmptyObj(iter.get("settlemode"))) {
                        Integer serviceAttrPage = commonService.getServiceAttr(Long.parseLong(iter.get("settlemode").toString()));
                        if (IStwbConstant.SERVICEATTR_DIRT == serviceAttrPage && ValueUtils.isNotEmptyObj(iter.get("noteno"))) {
                            Long noteNo = ValueUtils.isNotEmptyObj(iter.get("noteno")) ? Long.parseLong(iter.get("noteno").toString()) : -1L;
                            // 用于报错提示
                            String noteNoNote = ValueUtils.isNotEmptyObj(iter.get("noteno_noteno")) ? iter.get("noteno_noteno").toString() : "";

                            // 需要校验，一个票据id只能关联一个明细，否则给出提示“票据号XXX，仅允许关联一个明细”
                            // 同时票据id子表收款金额=票据(包)金额，否则，给出提示“票据号XX，收款金额不等于票据(包)金额，不允许保存”。
                            if (noteNo != -1L) {
                                if (notSubNoteList.contains(noteNo)) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102107"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186B1AEC04C00019", "票据号%s，仅允许关联一个明细") /* "票据号%s，仅允许关联一个明细" */, noteNoNote));
                                } else {
                                    BigDecimal noteSum = ValueUtils.isNotEmptyObj(iter.get("noteSum")) ? new BigDecimal(iter.get("noteSum").toString()) : BigDecimal.ZERO;
                                    BigDecimal oriSum = ValueUtils.isNotEmptyObj(iter.get("oriSum")) ? new BigDecimal(iter.get("oriSum").toString()) : BigDecimal.ZERO;
                                    if (oriSum.compareTo(noteSum) != 0) {
                                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102108"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186B1AEC04C0001C", "票据号%s，收款金额不等于票据(包)金额，不允许保存") /* "票据号%s，收款金额不等于票据(包)金额，不允许保存" */, noteNoNote));
                                    }
                                    notSubNoteList.add(noteNo);
                                }
                            }
                        }
                    }
                }
                if (iter.get("_status") != null && "Update".equals(iter.get("_status").toString())) {
                    FundCollection_b fundCollectionSub;
                    try {
                        List<FundCollection_b> originFundCollectionBList = originFundCollection.get("FundCollection_b");
                        fundCollectionSub = originFundCollectionBList.stream()
                                .filter(item -> item.getId() != null
                                        && iter.get("id") != null
                                        && item.getId().toString().equals(iter.get("id").toString()))
                                .findFirst()
                                .orElse(null);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102109"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D3", "查询资金收款单异常") /* "查询资金收款单异常" */);
                    }
                    long notenoPage = ValueUtils.isNotEmptyObj(iter.get("noteno")) ? Long.parseLong(iter.get("noteno").toString()) : -1L;
                    long notenoDB = ValueUtils.isNotEmptyObj(fundCollectionSub) && ValueUtils.isNotEmptyObj(fundCollectionSub.getNoteno()) ? fundCollectionSub.getNoteno() : -1L;
                    BigDecimal oriSumPage = ValueUtils.isNotEmptyObj(iter.get("oriSum")) ? new BigDecimal(iter.get("oriSum").toString()) : BigDecimal.ZERO;
                    BigDecimal oriSum = ValueUtils.isNotEmptyObj(fundCollectionSub) && ValueUtils.isNotEmptyObj(fundCollectionSub.getOriSum()) ? fundCollectionSub.getOriSum() : BigDecimal.ZERO;

                    if (notenoDB != notenoPage) {
                        if (notenoPage != -1L && notenoDB != -1L) {
                            // 占用
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                useBillMap.add(new BizObject(iter));
                            }
                            // 释放
                            if (BigDecimal.ZERO.compareTo(oriSum) < 0) {
                                releaseBillMap.add(fundCollectionSub);
                            }
                        }
                        if (notenoPage != -1L && notenoDB == -1L) {
                            // 占用
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                useBillMap.add(new BizObject(iter));
                            }
                        }
                        if (notenoPage == -1L) {
                            // 释放
                            if (BigDecimal.ZERO.compareTo(oriSum) < 0) {
                                releaseBillMap.add(fundCollectionSub);
                            }
                        }
                    }
                } else if (iter.get("_status") != null && "Delete".equals(iter.get("_status").toString())) {
                    FundCollection_b fundCollectionSub;
                    try {
                        List<FundCollection_b> originFundCollectionBList = originFundCollection.get("FundCollection_b");
                        fundCollectionSub = originFundCollectionBList.stream()
                                .filter(item -> item.getId() != null
                                        && iter.get("id") != null
                                        && item.getId().toString().equals(iter.get("id").toString()))
                                .findFirst()
                                .orElse(null);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102109"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D3", "查询资金收款单异常") /* "查询资金收款单异常" */);
                    }
                    releaseBillMap.add(fundCollectionSub);
                }
            }
            billDirection = 1;
        }

        if (releaseBillMap.size() > 0) {
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            for (BizObject subBiz : releaseBillMap) {
                fundCommonService.deleteNoteList(noteMaps, billDirection, bizObject, subBiz);
            }
            if (ValueUtils.isNotEmptyObj(noteMaps)) {
                try {
                    BaseResultVO jsonObject = ctmDrftEndorePaybillRpcService.settleReleaseBillNew(noteMaps);
                    log.error("fund bill note release success! code={}, id={}, inputParameter={}, outputParameter={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
                } catch (Exception e) {
                    log.error("fund bill note release fail! code={}, id={}, inputParameter={}, errorMsg={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102110"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D5", "单据明细行结算方式为票据结算，释放票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，释放票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D4", "：") /* "：" */ + e.getMessage());
                }
            }
        }

        // 当为票据结算时，保存时，占用票据
        if (useBillMap.size() > 0) {
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            for (BizObject subBiz : useBillMap) {
                fundCommonService.deleteNoteList(noteMaps, billDirection, bizObject, subBiz);
            }
            if (ValueUtils.isNotEmptyObj(noteMaps)) {
                try {
                    SettleUseBillResVO jsonObject = ctmDrftEndorePaybillRpcService.settleUseBillNew(noteMaps);
                    log.error("fund bill note occupied success! code={}, id={}, inputParameter={}, outputParameter={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
                } catch (Exception e) {
                    log.error("fund bill note occupied fail! code={}, id={}, inputParameter={}, errorMsg={}",
                            bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102111"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DA", "单据明细行结算方式为票据结算，占用票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，占用票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D4", "：") /* "：" */ + e.getMessage());
                }
            }
        }

    }

    private void checkFundPlanProject(String billnum, BizObject bizObject) throws Exception {
        boolean checkFundPlanIsEnabled = false;
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue());
        }

        boolean notOpenBill = Short.parseShort(bizObject.get("verifystate").toString()) != VerifyState.INIT_NEW_OPEN.getValue()
                && Short.parseShort(bizObject.get("verifystate").toString()) != VerifyState.REJECTED_TO_MAKEBILL.getValue();
        List<BizObject> preEmployFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        if (notOpenBill) {
            List<BizObject> employFundBillForFundPlanProjectList = new ArrayList<>();
            List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
            if (checkFundPlanIsEnabled) {
                fundBillAdaptationFundPlanService.fundBillEditEmployOrReleaseFundPlan(
                        billnum,
                        bizObject,
                        employFundBillForFundPlanProjectList,
                        releaseFundBillForFundPlanProjectList,
                        preEmployFundBillForFundPlanProjectList,
                        preReleaseFundBillForFundPlanProjectList);
            } else {
                fundBillAdaptationFundPlanService.fundPlanProjectNotControl(billnum, bizObject, releaseFundBillForFundPlanProjectList);
            }
            //先操作预占再操作实占，否则会出现预占用金额不足；先释放再占用否则最后遗留的金额会无法修改
            // 先释放
            if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, bizObject, releaseFundBillForFundPlanProjectList, null, null, "act");
            }
            if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, bizObject, preReleaseFundBillForFundPlanProjectList, null, null, "pre");
            }

            // 再占用
            if (CollectionUtils.isNotEmpty(preEmployFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillEmployFundPlan(billnum, bizObject, preEmployFundBillForFundPlanProjectList, "pre");
            }
            if (CollectionUtils.isNotEmpty(employFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillEmployFundPlan(billnum, bizObject, employFundBillForFundPlanProjectList, "act");
            }
        } else {
            if (checkFundPlanIsEnabled) {
                List<BizObject> fundSubBList = fundBillAdaptationFundPlanService
                        .fundBillPreEmployOrReleaseFundPlanBeforeSaveForUpdateAndDelete(
                                billnum,
                                bizObject,
                                preEmployFundBillForFundPlanProjectList,
                                preReleaseFundBillForFundPlanProjectList
                        );
                List<FundPayment_b> updateFundPaymentBList = new ArrayList<>();
                List<FundCollection_b> updateFundCollectionBList = new ArrayList<>();
                for (BizObject fundSub : fundSubBList) {
                    if ("Delete".equals(fundSub.get("_status").toString())) {
                        continue;
                    }
                    if (fundSub.getId() != null) {
                        //String tableName;
                        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                            FundPayment_b fundPaymentB = new FundPayment_b();
                            fundPaymentB.setId(fundSub.getId());
                            fundPaymentB.setIsToPushCspl(fundSub.getInteger("isToPushCspl"));
                            fundPaymentB.setEntityStatus(EntityStatus.Update);
                            updateFundPaymentBList.add(fundPaymentB);
                            //MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME, fundPaymentB);
                            //tableName = "cmp_fundpayment_b";
                        } else {
                            //tableName = "cmp_fundcollection_b";
                            FundCollection_b fundCollectionB = new FundCollection_b();
                            fundCollectionB.setId(fundSub.getId());
                            fundCollectionB.setIsToPushCspl(fundSub.getInteger("isToPushCspl"));
                            fundCollectionB.setEntityStatus(EntityStatus.Update);
                            updateFundCollectionBList.add(fundCollectionB);
                            //MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME, fundCollectionB);
                        }
                    }

                }
                if (CollectionUtils.isNotEmpty(updateFundPaymentBList)) {
                    MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME, updateFundPaymentBList);
                }
                if (CollectionUtils.isNotEmpty(updateFundCollectionBList)) {
                    MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME, updateFundCollectionBList);
                }
            } else {
                fundBillAdaptationFundPlanService.fundPlanProjectPreEmployOrReleaseNotControl(billnum, bizObject, preReleaseFundBillForFundPlanProjectList);
            }
            if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(billnum, bizObject, preReleaseFundBillForFundPlanProjectList, null, null, "pre");
            }

            if (CollectionUtils.isNotEmpty(preEmployFundBillForFundPlanProjectList)) {
                fundBillAdaptationFundPlanService.fundBillEmployFundPlan(billnum, bizObject, preEmployFundBillForFundPlanProjectList, "pre");
            }
        }


    }

    /**
     * 根据是否提前入账 设置关联次数
     *
     * @param bizObject
     */
    private void setAssociationcount(BizObject bizObject) {
        try {
            // 提前入账 根据上游关联次数 设置生单关联次数+1
            Boolean isadvanceaccounts = bizObject.get("isadvanceaccounts");
            Short associationcount = bizObject.get("associationcount");
            Long id = bizObject.get("id");
            if (id == null && isadvanceaccounts != null && isadvanceaccounts) {
                if (associationcount != null && associationcount.equals(AssociationCount.First.getValue())) {
                    bizObject.set("associationcount", AssociationCount.Second.getValue());
                } else {
                    bizObject.set("associationcount", AssociationCount.First.getValue());
                }
            }
        } catch (Exception e) {
            log.error("设置提前入账信息异常");
        }
    }


    /**
     * 校验付款单子表信息
     *
     * @param billb
     */
    private void checkFundPaymentbInfo(FundPayment_b billb, Map<String, Integer> checkCacheMap, String accentity) throws
            Exception {
        short caObject = billb.getCaobject().getValue();
        if (caObject == 1) {// 客户
            if (ValueUtils.isNotEmptyObj(billb.getOppositeobjectid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeobjectid_customer_" + billb.getOppositeobjectid()))) {
                billCopyCheckService.checkCustomerByid(billb.getOppositeobjectid(), accentity, checkCacheMap);
            }
            if (ValueUtils.isNotEmptyObj(billb.getOppositeaccountid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeaccountid_customer_" + billb.getOppositeaccountid()))) {
                billCopyCheckService.checkCustomerbankaccountById(billb.getOppositeaccountid(), checkCacheMap);
            }
        }
        if (caObject == 2) {// 供应商
            if (ValueUtils.isNotEmptyObj(billb.getOppositeobjectid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeobjectid_supplier_" + billb.getOppositeobjectid()))) {
                billCopyCheckService.checkSupplier(Long.valueOf(billb.getOppositeobjectid()), accentity, checkCacheMap);
            }
            Map conditon = new HashMap<>();
            conditon.put("id", billb.getOppositeaccountid());
            if (ValueUtils.isNotEmptyObj(billb.getOppositeaccountid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeaccountid_supplier_" + billb.getOppositeaccountid()))) {
                billCopyCheckService.checkSupplierbankaccountById(conditon, billb.getOppositeaccountid(), checkCacheMap);
            }
        }
        if (caObject == 3) {// 员工

        }
        if (ValueUtils.isNotEmptyObj(billb.get("enterprisebankaccount")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("enterprisebankaccount_" + billb.get("enterprisebankaccount")))) {
            billCopyCheckService.checkBankaccount(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("quickType")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("quickType_" + billb.get("quickType")))) {
            billCopyCheckService.checkQuickType(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("settlemode")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("settlemode_" + billb.get("settlemode")))) {
            billCopyCheckService.checkSettlemode(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("dept")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("dept_" + billb.get("dept")))) {
            billCopyCheckService.checkDept(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("expenseitem"))) {
            billCopyCheckService.checkExpenseitem(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("project"))) {
            billCopyCheckService.checkProject(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("currency")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("currency_" + billb.get("currency")))) {
            billCopyCheckService.checkCurrency(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("settleCurrency"))) {
            billCopyCheckService.checkSettleCurrency(billb, checkCacheMap);
        }

    }


    /**
     * 校验资金收款单子表信息
     *
     * @param billb
     * @param checkCacheMap
     * @param accentity
     * @throws Exception
     */
    private void checkFundCollectionbInfo(FundCollection_b billb, Map<String, Integer> checkCacheMap, String
            accentity) throws Exception {
        short caObject = billb.getCaobject().getValue();
        if (caObject == 1) {// 客户
            if (ValueUtils.isNotEmptyObj(billb.getOppositeobjectid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeobjectid_customer_" + billb.getOppositeobjectid()))) {
                billCopyCheckService.checkCustomerByid(billb.getOppositeobjectid(), accentity, checkCacheMap);
            }
            if (ValueUtils.isNotEmptyObj(billb.getOppositeaccountid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeaccountid_customer_" + billb.getOppositeaccountid()))) {
                billCopyCheckService.checkCustomerbankaccountById(billb.getOppositeaccountid(), checkCacheMap);
            }
        }
        if (caObject == 2) {// 供应商
            if (ValueUtils.isNotEmptyObj(billb.getOppositeobjectid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeobjectid_supplier_" + billb.getOppositeobjectid()))) {
                billCopyCheckService.checkSupplier(Long.valueOf(billb.getOppositeobjectid()), accentity, checkCacheMap);
            }
            Map conditon = new HashMap<>();
            conditon.put("id", billb.getOppositeaccountid());
            if (ValueUtils.isNotEmptyObj(billb.getOppositeaccountid()) &&
                    ValueUtils.isNotEmptyObj(checkCacheMap.get("oppositeaccountid_supplier_" + billb.getOppositeaccountid()))) {
                billCopyCheckService.checkSupplierbankaccountById(conditon, billb.getOppositeaccountid(), checkCacheMap);
            }
        }
        if (caObject == 3) {// 员工

        }
        if (ValueUtils.isNotEmptyObj(billb.get("enterprisebankaccount")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("enterprisebankaccount_" + billb.get("enterprisebankaccount")))) {
            billCopyCheckService.checkBankaccount(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("quickType")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("quickType_" + billb.get("quickType")))) {
            billCopyCheckService.checkQuickType(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("dept")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("dept_" + billb.get("dept")))) {
            billCopyCheckService.checkDept(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("project"))) {
            billCopyCheckService.checkProject(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("expenseitem"))) {
            billCopyCheckService.checkExpenseitem(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("currency")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("currency_" + billb.get("currency")))) {
            billCopyCheckService.checkCurrency(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("settlemode")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("settlemode_" + billb.get("settlemode")))) {
            billCopyCheckService.checkSettlemode(billb, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(billb.get("settleCurrency"))) {
            billCopyCheckService.checkSettleCurrency(billb, checkCacheMap);
        }

//        fundCommonService.setCostCenter(accentity, billb);
    }

    /**
     * 复制校验逻辑
     *
     * @param bizObject
     * @param checkCacheMap
     * @param billnum
     * @param accentity
     * @throws Exception
     */
    private void checkCopy(BizObject bizObject, Map<String, Integer> checkCacheMap, String billnum, String
            accentity) throws Exception {

        if (!"copy".equals(bizObject.get("actionType"))) {
            return;
        }
        // 判断是否启用商业汇票
        CtmJSONObject enableBsdModule = null;
        try {
            enableBsdModule = fundCommonService.isEnableBsdModule(accentity);
        } catch (Exception e) {
            log.error("get BSD is enabled fail!, e = {}", e.getMessage());
        }
        boolean flag = false;
        if (ValueUtils.isNotEmptyObj(enableBsdModule)) {
            flag = MapUtils.getBoolean(enableBsdModule, "isEnabled");
        }
        bizObject.set("isEnabledBsd", flag);

        log.error("BeforeSaveFundBillRule, id = {}, code = {}, BizObject = {}", bizObject.getId(), bizObject.get("code"), bizObject);
        if (ValueUtils.isNotEmptyObj(bizObject.get(IBussinessConstant.ACCENTITY)) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("accentity_" + bizObject.get(IBussinessConstant.ACCENTITY)))) {
            billCopyCheckService.checkAccentity(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("org")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("org_" + bizObject.get("org")))) {
            billCopyCheckService.checkOrg(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("dept")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("dept_" + bizObject.get("dept")))) {
            billCopyCheckService.checkDept(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("project"))) {
            billCopyCheckService.checkProject(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("expenseitem"))) {
            billCopyCheckService.checkExpenseitem(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("currency")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("currency_" + bizObject.get("currency")))) {
            billCopyCheckService.checkCurrency(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("tradetype")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("tradetype_" + bizObject.get("tradetype")))) {
            billCopyCheckService.checkTradetype(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("settlemode")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("settlemode_" + bizObject.get("settlemode")))) {
            billCopyCheckService.checkSettlemode(bizObject, checkCacheMap);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.get("enterprisebankaccount")) &&
                ValueUtils.isNotEmptyObj(checkCacheMap.get("enterprisebankaccount_" + bizObject.get("enterprisebankaccount")))) {
            billCopyCheckService.checkBankaccount(bizObject, checkCacheMap);
        }
        if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {// 资金收款单
            List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);//
            for (FundCollection_b billb : billbs) {
                // 校验子表信息
                checkFundCollectionbInfo(billb, checkCacheMap, accentity);
            }
        }
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
            List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);// 资金付款单子表
            for (FundPayment_b billb : billbs) {
                // 校验子表信息
                checkFundPaymentbInfo(billb, checkCacheMap, accentity);
                // 数据签名
                DataSignatureEntity dataSignatureEntity =
                        DataSignatureEntity.builder().opoppositeObjectName(Objects.isNull(billb.getOppositeobjectname())?"":billb.getOppositeobjectname()).
                                oppositeAccountName(Objects.isNull(billb.getOppositeaccountname())?"":billb.getOppositeaccountname()).tradeAmount(billb.getOriSum()).build();
                billb.setSignature(DataSignatureUtil.signMsg(dataSignatureEntity));
                billb.setOppositeobjectname(Objects.isNull(billb.getOppositeobjectname())?"": billb.getOppositeobjectname());
                billb.setOppositeaccountname(Objects.isNull(billb.getOppositeaccountname())?"": billb.getOppositeaccountname());
                billb.setOppositeaccountno(Objects.isNull(billb.getOppositeaccountno()) ? null : billb.getOppositeaccountno());
                billb.setOppositebankaddr(Objects.isNull(billb.getOppositebankaddr()) ? null : billb.getOppositebankaddr());
                billb.setOppositebanklineno(Objects.isNull(billb.getOppositebanklineno()) ? null : billb.getOppositebanklineno());
                billb.setOppositebankType(Objects.isNull(billb.getOppositebankType()) ? null : billb.getOppositebankType());
                billb.setReceivePartySwift(Objects.isNull(billb.getReceivePartySwift()) ? null : billb.getReceivePartySwift());
            }
        }
    }

    /**
     * 处理流程中预算相关逻辑，审批中编辑时需要先取消占用预算再进行预算占用
     *
     * @param bizObject
     * @param billbs
     * @param billnum
     * @throws Exception
     */
    private boolean cancelBudget(BizObject bizObject, List<FundPayment_b> billbs, String billnum, String
            billAction) throws Exception {
        List<FundPayment_b> newBillbs = new ArrayList<>();
        // 审批流状态，非审批中，直接跳过
        if (bizObject.get("verifystate") != null && Short.valueOf(bizObject.get("verifystate").toString()) == VerifyState.SUBMITED.getValue()) {
            if (cmpBudgetManagerService.isCanStart(billnum)) {
                for (FundPayment_b billb : billbs) {
                    // 预占状态为预占成功的子表数据才释放
                    if (billb.getIsOccupyBudget() != null && OccupyBudget.PreSuccess.getValue() == billb.getIsOccupyBudget()) {
                        newBillbs.add(billb);
                    }
                }
                // 有预占成功的数据才调用预算接口
                if (newBillbs.size() != 0) {
                    cmpBudgetManagerService.gcExecuteBatchUnSubmit(bizObject, newBillbs, billnum, billAction);
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 处理收款方账户号、收款方账户名称（客户银行账户参照、供应商银行账户参照、员工银行账户参照）
     */
    private void dealCustSuppBankRef(BizObject bizObject, String billnum) throws Exception {
        List<BizObject> bizObject_bList = new ArrayList<>();
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundPayment_b", BizObject.class);
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            bizObject_bList = bizObject.getBizObjects("FundCollection_b", BizObject.class);
        }
        if (CollectionUtils.isEmpty(bizObject_bList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102075"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080A", "新增数据表体信息不能为空，请检查") /* "导入数据表体信息不能为空，请检查" */);
        }
        // 收集需要翻译的银行账户号
        boolean isStrictlyControl = ValueUtils.isNotEmptyObj(bizObject.get("isStrictlyControl"))
                ? bizObject.getBoolean("isStrictlyControl") : true;
        if (isStrictlyControl) {
            for (BizObject bizObject_b : bizObject_bList) {
                if ("Delete".equals(bizObject_b.getString("_status"))) {
                    continue;
                }
                short caobject = bizObject_b.getShort("caobject").shortValue();
                if (CaObject.Customer.getValue() == caobject && StringUtils.isNotEmpty(bizObject_b.getString("oppositeaccountno"))) {
                    AgentFinancialDTO custAccMap = BillImportCheckUtil.queryCustomerBankAccountByCondition(bizObject_b.getLong("oppositeobjectid"), bizObject_b.getString("oppositeaccountno"), bizObject.getString("currency"));
                    if (custAccMap == null) {

                        ExcelErrorMsgDto excelErrorMsgDto = new ExcelErrorMsgDto(
                                bizObject_b.get(POIConstant.ExcelField.ORIGIN_SHEET_NAME),
                                ValueUtils.isNotEmptyObj(bizObject_b.get(POIConstant.ExcelField.ROW_NUM)) ? bizObject_b.get(POIConstant.ExcelField.ROW_NUM).toString() : "1",
                                String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180813",
                                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004C7", "收款方账户号[%s]未在客户档案银行账户中查询到，请检查") /* "收款方账户号[%s]未在客户档案银行账户中查询到，请检查" */) /* "收款方账户号[%s]未在客户档案银行账户中查询到，请检查" */,
                                        bizObject_b.getString("oppositeaccountno"))
                        );
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102112"), CtmJSONObject.toJSONString(excelErrorMsgDto));
                    }
                }
            }
        }

    }
}
