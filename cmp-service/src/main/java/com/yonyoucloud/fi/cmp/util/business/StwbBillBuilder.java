package com.yonyoucloud.fi.cmp.util.business;

import com.yonyou.business_flow.dto.DomainMakeBillRuleModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.BankVO;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ypd.bizflow.dto.ConvertParam;
import com.yonyou.ypd.bizflow.dto.ConvertResult;
import com.yonyou.ypd.bizflow.service.BusinessConvertService;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettled;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettledCrossBorder;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.ctm.stwb.stwbentity.OpenWSettleStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.transferaccount.util.BaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.tmsp.openapi.ITmspRefRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import lombok.NonNull;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ONE;

/**
 * 资金结算推送单据构建
 */

@Component
@Slf4j
public class StwbBillBuilder {

    private static BaseRefRpcService refRpcService;
    private static EnterpriseBankQueryService enterpriseBankQueryService;
    private static ITmspRefRpcService tmspRefRpcService;
    private static CmCommonService cmCommonService;
    private static IFundCommonService fundCommonService;
    private static BusinessConvertService businessConvertService;
    private static final String PARAM_TRUE = "1";
    private static final String PARAM_FALSE = "0";

    private static TransTypeQueryService transTypeQueryService;

    @Autowired
    private void setBusinessConvertService(BusinessConvertService businessConvertService) {
        StwbBillBuilder.businessConvertService = businessConvertService;
    }

    @Autowired
    private void setBaseRefRpcService(BaseRefRpcService baseRefRpcService) {
        StwbBillBuilder.refRpcService = baseRefRpcService;
    }

    @Autowired
    private void setEnterpriseBankQueryService(EnterpriseBankQueryService enterpriseBankQueryService) {
        StwbBillBuilder.enterpriseBankQueryService = enterpriseBankQueryService;
    }

    @Autowired
    private void setTmspRefRpcService(ITmspRefRpcService refRpcService) {
        StwbBillBuilder.tmspRefRpcService = refRpcService;
    }

    @Autowired
    private void setCmCommonService(CmCommonService cmCommonService) {
        StwbBillBuilder.cmCommonService = cmCommonService;
    }

    @Autowired
    private void setTransTypeQueryService(TransTypeQueryService transTypeQueryService){
        StwbBillBuilder.transTypeQueryService=transTypeQueryService;
    }
    @Autowired
    private void setFundCommonService(IFundCommonService fundCommonService){
        StwbBillBuilder.fundCommonService=fundCommonService;
    }

    /**
     * 构建资金收款单推送信息
     *
     * @param fundbill
     * @param params
     * @return
     */
    public static List<DataSettled> builderFundCollection(FundCollection fundbill, Map<String, Object> params) {
        List<FundCollection_b> fundCollection_bs = fundbill.FundCollection_b();
        List<DataSettled> dataSettleds = new ArrayList<DataSettled>();
        List<FundCollection_b> bills = fundCollection_bs.stream().filter(fundCollection ->
                ((null == fundCollection.getEntrustReject() || fundCollection.getEntrustReject() != 1) && fundCollection.getOriSum().compareTo(BigDecimal.ZERO) != 0)).collect(Collectors.toList());//过滤已经被委托拒绝的单子;过滤掉金额为0的表体数据；
        for (FundCollection_b bill : bills) {
            DataSettled dataSettled = new DataSettled();
            // 主表数据
            dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
            dataSettled.setAccentity(fundbill.getAccentity());//资金组织
            dataSettled.setOrg(fundbill.getOrg());//业务组织
            dataSettled.setBusinessbillnum(fundbill.getCode());//单据编号
            dataSettled.setBusinessId(fundbill.getId().toString());//单据id
            dataSettled.setTradetype(fundbill.getTradetype());//交易类型
            dataSettled.setOribilldate(fundbill.getVouchdate());//单据日期
            dataSettled.setNatcurrency(fundbill.getNatCurrency());//本币币种
            dataSettled.setRecpaytype("1"); //收付类型 1-收款，2-付款
            dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.CashRecBill.getValue())); //资金收款单
            dataSettled.setEntryType(fundbill.getEntrytype());
            // 子表数据
            dataSettled.setOricurrency(fundbill.getCurrency());//原币币种 - 从表头取
            dataSettled.setOricurrencyamount(bill.getOriSum());//原币金额
            dataSettled.setIssinglebatch(PARAM_FALSE);// 单笔/批量
            dataSettled.setExchangePaymentRateType(fundbill.getExchangeRateType());//折本币汇率类型
            //如果结算币种不等于原币币种，就不给待结算数据传本币汇率值
            if (bill.getSettleCurrency().equals(fundbill.getCurrency())) {
                dataSettled.setExchangerate(bill.getExchRate());//汇率
            }
            dataSettled.setNatSum(bill.getNatSum());//本币金额
            dataSettled.setProceedtype(bill.getQuickType());//款项类型
            dataSettled.setBusinessdetailsid(bill.getId().toString());//子表id
            dataSettled.setDept(bill.getDept());//部门
            dataSettled.setProject(bill.getProject());//项目
            dataSettled.setExpenseitem(bill.getExpenseitem());
            dataSettled.setCashaccount(bill.getCashaccount());//现金账户
            dataSettled.setEnterpriseBankAccount(bill.getEnterprisebankaccount());//银行账户
            dataSettled.setThirdParVirtAccount(bill.getThirdParVirtAccount());
            dataSettled.setCheckIdentificationCode(bill.getSmartcheckno());//勾兑号
            /*  结算方式为票据，以下字段必填  */
            if (ValueUtils.isNotEmptyObj(fundbill.getIsEnabledBsd()) && fundbill.getIsEnabledBsd() && bill.getNoteno() != null) { //启用商业汇票
                dataSettled.setNotenoid(bill.getNoteno().toString()); //票据号
            } else {//未启用使用手工票据号
                dataSettled.setNoteno(bill.getNotetextno());
            }
            dataSettled.setNotenoamount(bill.getNoteSum()); // 票面金额
            if (bill.getNoteDirection() != null) {
                dataSettled.setReceiptDirect(String.valueOf(bill.getNoteDirection().getValue()));//票据方向
            }
            dataSettled.setRemark(bill.getDescription());//备注
            /*
                1. 收款对象为客户、供应商、员工时，传入对应的档案ID，对方ID必填
                2. 收款对象为其他时，对方名称必填
             */
            short caobject = bill.getShort("caobject");
            dataSettled.setToaccnttype(String.valueOf(caobject));//对方类型参照
            if (caobject == CaObject.Customer.getValue()) {// 客户
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//客户id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            } else if (caobject == CaObject.Supplier.getValue()) {// 供应商
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//供应商id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            } else if (caobject == CaObject.Employee.getValue()) {// 员工
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//员工id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            } else if (caobject == CaObject.Other.getValue()) {// 其他
                dataSettled.setOppositeBankTypeName(bill.getOppositebankType());// 对方银行类别
                dataSettled.setShowoppositebanklineno(bill.getOppositebanklineno()); // 对方银行联行号
            }else if(caobject == CaObject.CapBizObj.getValue()){//资金业务对象
                dataSettled.setCapBizObjType(bill.get("fundbusinobjtypeid") == null ? null : bill.get("fundbusinobjtypeid"));
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//资金业务对象id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowtoaccntname(bill.getOppositeobjectname()); //显示对方名称 - 收款单位名称
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
                dataSettled.setShowoppositebankname(bill.getOppositebankaddr());//收款方银行网点
                dataSettled.setOppositeBankTypeName(bill.getOppositebankType());// 对方银行类别
                dataSettled.setShowoppositebanklineno(bill.getOppositebanklineno()); // 对方银行联行号
            } else if (caobject == CaObject.InnerUnit.getValue()) {//内部单位
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//内部单位d
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//内部单位银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            }

            dataSettled.setShowtoaccntname(bill.getOppositeobjectname()); //显示对方名称 - 收款单位名称
            dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            dataSettled.setShowoppositebankname(bill.getOppositebankaddr());//收款方银行网点

            //根据单据来源判断结算状态
            boolean flag = fundbill.getSrcitem().getValue() == EventSource.Cmpchase.getValue() ||
                    fundbill.getSrcitem().getValue() == EventSource.ManualImport.getValue() ||
                    fundbill.getSrcitem().getValue() == EventSource.StwbSettlement.getValue();
            boolean associationStatus = ValueUtils.isNotEmptyObj(bill.getAssociationStatus())
                    && bill.getAssociationStatus() == AssociationStatus.Associated.getValue();
            // 未关联的、来源类型为现金管理或导入的、单据类型为资金收款单的结算状态为一般结算
            if (!associationStatus && flag && (EventType.FundCollection.getValue() == fundbill.getShort("billtype") ||
                    EventType.Unified_Synergy.getValue() == fundbill.getShort("billtype"))) {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.NormalSettle.getValue()));//来源数据结算状态 - 一般结算
            } else {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
            }
            // 对于结算状态为已结算补单的，推送时状态为已结算补单
            if (bill.getFundSettlestatus().getValue() == FundSettleStatus.SettlementSupplement.getValue()) {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
            }
            int settlemode = cmCommonService.getServiceAttr(bill.getSettlemode());
            dataSettled.setIssettlementcanmodified(PARAM_TRUE);//结算系统可修改
            if (fundbill.getEntrytype() !=null && fundbill.getEntrytype() == EntryType.CrushHang_Entry.getValue()){
                dataSettled.setIsjournalregistered(PARAM_FALSE);//是否登记日记账
                dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成凭证
            } else {
                dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账
                dataSettled.setIsGenerateVoucher(PARAM_TRUE);//是否生成凭证
            }
            dataSettled.setIssettlementcanmodified(PARAM_TRUE);//是否结算系统可修改 1-是
            //当“事项类型=统收统支协同单”时，待结算数据推送接口，是否允许拆分=是，是否允许合并=是。
            if (EventType.Unified_Synergy.getValue() == fundbill.getShort("billtype")) {
                dataSettled.setIsmerge(PARAM_TRUE);
                dataSettled.setIssplit(PARAM_FALSE);
            } else if (settlemode == 2 && bill.getNoteno() != null) {
                dataSettled.setIssettlementcanmodified(PARAM_FALSE);//结算系统不可修改
                dataSettled.setIsmerge(PARAM_FALSE);// 不可合并
                dataSettled.setIssplit(PARAM_FALSE);// 不可拆分
            } else if (settlemode == 2 && bill.getNoteno() == null) {
                dataSettled.setIssettlementcanmodified(PARAM_TRUE);//结算系统可修改
                dataSettled.setIsmerge(PARAM_TRUE);// 可合并
                dataSettled.setIssplit(PARAM_TRUE);// 可拆分
            } else {
                dataSettled.setIsmerge(PARAM_TRUE);
                dataSettled.setIssplit(PARAM_TRUE); // 可以拆分
            }
            if (Boolean.parseBoolean(params.get("bCheck").toString())) {
                dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
            } else {
                dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
            }
            if (bill.getFundSettlestatus().getValue() == FundSettleStatus.Refund.getValue()) {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));
            }
            dataSettled.setExpectsettlemethod(bill.getSettlemode());//期望结算方式
            // 是否关联
            dataSettled.setIsRelateCheckBill(ValueUtils.isNotEmptyObj(bill.getAssociationStatus()) ?
                    bill.getAssociationStatus().toString() : null);
            // 对象单ID
            dataSettled.setRelateBankCheckBillId(ValueUtils.isNotEmptyObj(bill.getBankReconciliationId()) ?
                    Long.parseLong(bill.getBankReconciliationId()) : null);
            // 认领单ID
            dataSettled.setRelateClaimBillId(ValueUtils.isNotEmptyObj(bill.getBillClaimId()) ?
                    Long.parseLong(bill.getBillClaimId()) : null);
            // 是否要占用资金计划：isToPushCspl,如果现金已占用，则传0，否则传1
            if (ValueUtils.isNotEmptyObj(bill.getIsToPushCspl()) && bill.getIsToPushCspl() == 1) {
                dataSettled.setIsToPushCspl((short) 0);
            } else {
                dataSettled.setIsToPushCspl((short) 1);
            }
            // 资金计划项目
            if (ValueUtils.isNotEmptyObj(bill.get("fundPlanProject"))){
                dataSettled.setCsplProject(Long.parseLong(bill.get("fundPlanProject").toString()));
            }
            // 资金计划明细
            if (ValueUtils.isNotEmptyObj(bill.get("fundPlanProjectDetail"))){
                dataSettled.setCsplSummaryDetail(Long.parseLong(bill.getString("fundPlanProjectDetail")));
            }
            if(null != bill.getCheckId()){
                dataSettled.setNoteno(bill.getCheckno());
                dataSettled.setNotenoid(bill.getCheckId());
            }

            // 统收统支关系组字段处理
            dataSettled.setIsIncomeAndExpenditure(bill.getIsIncomeAndExpenditure());
            if (ValueUtils.isNotEmptyObj(bill.getIncomeAndExpendRelationGroup())) {
                dataSettled.setIncomeAndExpendRelatChildId(bill.getIncomeAndExpendRelationGroup());
            }
            if (ValueUtils.isNotEmptyObj(bill.getIncomeAndExpendBankAccount())) {
                dataSettled.setMarginaccount(bill.getIncomeAndExpendBankAccount());
            }

            dataSettled.setSourceAppcode(ICmpConstant.YONBIP_FI_CTMCMP);

            // 异币种改造：待结算数据新增接口增加结算币种、换出汇率类型、换出汇率预估
            dataSettled.setExchangePaymentCurrency(bill.getSettleCurrency());
            dataSettled.setExchangePaymentRate(bill.getSwapOutExchangeRateEstimate());
            dataSettled.setExchangeRateType(bill.getSwapOutExchangeRateType());
            if(bill.getExchangerateOps() != null){
                dataSettled.setExchangerateOps(bill.getExchangerateOps());
            }

            dataSettled.setExchangePaymentRateType(fundbill.getExchangeRateType());

            if (ValueUtils.isNotEmptyObj(fundbill.get("wbs"))){
                dataSettled.setWbs(fundbill.get("wbs"));
            }
            if (ValueUtils.isNotEmptyObj(fundbill.get("activity"))){
                dataSettled.setActivity(Long.parseLong(fundbill.get("activity").toString()));
            }
            // 推送 轧差识别码、轧差总笔数、轧差后金额、轧差方向
            if(ValueUtils.isNotEmptyObj(bill.getNetIdentificateCode())){
                dataSettled.setNetIdentificateCode(bill.getNetIdentificateCode());
            }
            if(ValueUtils.isNotEmptyObj(bill.getShort("netSettleCount"))){
                dataSettled.setNetSettleCount(bill.getShort("netSettleCount"));
            }
            if(ValueUtils.isNotEmptyObj(bill.getAfterNetAmt())){
                dataSettled.setAfterNetAmt(bill.getAfterNetAmt());
            }
            if(ValueUtils.isNotEmptyObj(bill.getShort("afterNetDir"))){
                if (bill.getShort("afterNetDir") == PaymentType.FundCollection.getValue()) {
                    dataSettled.setAfterNetDir((short) 1); //收付类型 1-收款，2-付款
                } else {
                    dataSettled.setAfterNetDir((short) 2); //收付类型 1-收款，2-付款
                }
            }

            // 将结算中心代理账户传递给待结算
            if (ValueUtils.isNotEmptyObj(bill.getActualSettleAccount())){
                dataSettled.setActualSettleAccount(bill.getActualSettleAccount());
            }
            dataSettleds.add(dataSettled);
        }
        return dataSettleds;
    }

    /**
     * 构建资金付款单推送信息
     *
     * @param fundbill
     * @param params
     * @return
     */
    public static List<DataSettled> builderFundPayment(FundPayment fundbill, Map<String, Object> params) throws Exception {
        List<FundPayment_b> fundPayment_bs = fundbill.FundPayment_b();
        List<DataSettled> dataSettleds = new ArrayList<DataSettled>();
        List<FundPayment_b> bills = fundPayment_bs.stream().filter(fundPayment ->
                ((null == fundPayment.getEntrustReject() || fundPayment.getEntrustReject() != 1 ) && fundPayment.getOriSum().compareTo(BigDecimal.ZERO) != 0)).collect(Collectors.toList());//过滤已经被委托拒绝的单子;过滤掉金额为0的表体数据;
        for (FundPayment_b bill : bills) {
            DataSettled dataSettled = new DataSettled();
            // 主表数据
            dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
            dataSettled.setAccentity(fundbill.getAccentity());//资金组织
            dataSettled.setOrg(fundbill.getOrg());//业务组织
            dataSettled.setBusinessbillnum(fundbill.getCode());//单据编号
            dataSettled.setBusinessId(fundbill.getId().toString());//单据id
            dataSettled.setTradetype(fundbill.getTradetype());//交易类型
            dataSettled.setOribilldate(fundbill.getVouchdate());//单据日期
            dataSettled.setNatcurrency(fundbill.getNatCurrency());//本币币种
            dataSettled.setRecpaytype("2"); //收付类型 1-收款，2-付款
            dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.CashPayBill.getValue())); //资金付款单
            // 子表数据
            dataSettled.setOricurrency(fundbill.getCurrency());//原币币种
            dataSettled.setOricurrencyamount(bill.getOriSum());//原币金额
            dataSettled.setIssinglebatch(PARAM_FALSE);// 单笔/批量
            dataSettled.setExchangePaymentRateType(fundbill.getExchangeRateType());//折本币汇率类型
            //如果结算币种不等于原币币种，就不给待结算数据传本币汇率值
            if (bill.getSettleCurrency().equals(fundbill.getCurrency())) {
                dataSettled.setExchangerate(bill.getExchRate());//汇率
            }
            dataSettled.setNatSum(bill.getNatSum());//本币金额
            dataSettled.setProceedtype(bill.getQuickType());//款项类型
            dataSettled.setBusinessdetailsid(bill.getId().toString());//子表id
            dataSettled.setDept(bill.getDept());//部门
            dataSettled.setProject(bill.getProject());//项目
            dataSettled.setCheckIdentificationCode(bill.getSmartcheckno());//勾兑号
            dataSettled.setThirdParVirtAccount(bill.getThirdParVirtAccount());
            dataSettled.setCashaccount(bill.getCashaccount());//现金账户
            dataSettled.setEnterpriseBankAccount(bill.getEnterprisebankaccount());//银行账户
            dataSettled.setExpenseitem(bill.getExpenseitem());
            //国机相关，添加支付扩展信息
            dataSettled.setPayExtend(bill.getPayExtend());

            /*  结算方式为票据，以下字段必填  */
            if (ValueUtils.isNotEmptyObj(fundbill.getIsEnabledBsd()) && fundbill.getIsEnabledBsd() && bill.getNoteno() != null) { //启用商业汇票
                dataSettled.setNotenoid(bill.getNoteno().toString()); //票据号
            } else {//未启用使用手工票据号
                dataSettled.setNoteno(bill.getNotetextno());
            }
            dataSettled.setNotenoamount(bill.getNoteSum()); // 票面金额
            if (bill.getNoteDirection() != null) {
                dataSettled.setReceiptDirect(String.valueOf(bill.getNoteDirection().getValue()));//票据方向
            }

            dataSettled.setRemark(bill.getDescription());//备注
            dataSettled.setEntryType(fundbill.getEntrytype());

            /*
                1. 收款对象为客户、供应商、员工时，传入对应的档案ID，对方ID必填
                2. 收款对象为其他时，对方名称必填
             */
            short caobject = bill.getShort("caobject");
            dataSettled.setToaccnttype(String.valueOf(caobject));//对方类型参照
            if (caobject == CaObject.Customer.getValue()) {// 客户
                boolean isStrictlyControl = ValueUtils.isNotEmptyObj(fundbill.get("isStrictlyControl"))
                        ? fundbill.getBoolean("isStrictlyControl") : true;
                if (isStrictlyControl){
                    fundCommonService.checkCaObjectAccountNoEqual(CaObject.Customer.getValue(),bill.getOppositeaccountid(),bill.getOppositeaccountno());
                }
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//客户id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            } else if (caobject == CaObject.Supplier.getValue()) {// 供应商
                boolean isStrictlyControl = ValueUtils.isNotEmptyObj(fundbill.get("isStrictlyControl"))
                        ? fundbill.getBoolean("isStrictlyControl") : true;
                if (isStrictlyControl) {
                    fundCommonService.checkCaObjectAccountNoEqual(CaObject.Supplier.getValue(), bill.getOppositeaccountid(), bill.getOppositeaccountno());
                }
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//供应商id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            } else if (caobject == CaObject.Employee.getValue()) {// 员工
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//员工id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            } else if (caobject == CaObject.Other.getValue()) {// 其他
                dataSettled.setOppositeBankTypeName(bill.getOppositebankType());// 对方银行类别
                dataSettled.setShowoppositebanklineno(bill.getOppositebanklineno()); // 对方银行联行号
            }else if(caobject == CaObject.CapBizObj.getValue()){//资金业务对象
                dataSettled.setCapBizObjType(bill.get("fundbusinobjtypeid") == null ? null : bill.get("fundbusinobjtypeid"));
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//资金业务对象id
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//银行账户id
                dataSettled.setShowtoaccntname(bill.getOppositeobjectname()); //显示对方名称 - 收款单位名称
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
                dataSettled.setShowoppositebankname(bill.getOppositebankaddr());//收款方银行网点
                dataSettled.setOppositeBankTypeName(bill.getOppositebankType());// 对方银行类别
                dataSettled.setShowoppositebanklineno(bill.getOppositebanklineno()); // 对方银行联行号
            }else if (caobject == CaObject.InnerUnit.getValue()) {//内部单位
                dataSettled.setCounterpartyid(bill.getOppositeobjectid());//内部单位d
                dataSettled.setCounterpartybankaccount(bill.getOppositeaccountid());//内部单位银行账户id
                dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
                dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            }
            dataSettled.setShowtoaccntname(bill.getOppositeobjectname()); //显示对方名称 - 收款单位名称
            dataSettled.setShowoppositebankaccount(bill.getOppositeaccountno()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getOppositeaccountname()); //收款方账户名称
            dataSettled.setShowoppositebankname(bill.getOppositebankaddr());//收款方银行网点
            //根据单据来源判断结算状态
            boolean flag = fundbill.getSrcitem().getValue() == EventSource.Cmpchase.getValue() ||
                    fundbill.getSrcitem().getValue() == EventSource.ManualImport.getValue() ||
                    fundbill.getSrcitem().getValue() == EventSource.StwbSettlement.getValue();
            boolean associationStatus = ValueUtils.isNotEmptyObj(bill.getAssociationStatus())
                    && bill.getAssociationStatus() == AssociationStatus.Associated.getValue();
            if (!associationStatus && flag && (EventType.FundPayment.getValue() == fundbill.getShort("billtype") ||
                    EventType.InternalTransferProtocol.getValue() == fundbill.getShort("billtype") ||
                    EventType.Unified_Synergy.getValue() == fundbill.getShort("billtype"))) {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.NormalSettle.getValue()));//来源数据结算状态 - 一般结算
            } else {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
            }
            // 如果资金收付款单结算状态为退票，则将对应的资金结算明细退票状态改为退票
            if(bill.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.Refund.getValue()
                    && fundbill.getShort(ICmpConstant.BILLTYPE) != EventType.FundPayment.getValue()){
                short refund = 1;
                dataSettled.setIsrefund(refund);
                dataSettled.setRefundAmt(bill.getRefundSum());
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
            }
            if(bill.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.Refund.getValue()){
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));
            }
            // 对于结算状态为已结算补单的，推送时状态为已结算补单
            if (bill.getFundSettlestatus().getValue() == FundSettleStatus.SettlementSupplement.getValue()) {
                dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
            }
            int settlemode = cmCommonService.getServiceAttr(bill.getSettlemode());
            dataSettled.setIssettlementcanmodified(PARAM_TRUE);//结算系统可修改
            if (fundbill.getEntrytype() != null && fundbill.getEntrytype() == EntryType.CrushHang_Entry.getValue()){
                dataSettled.setIsjournalregistered(PARAM_FALSE);//是否登记日记账
                dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成凭证
            } else {
                dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账
                dataSettled.setIsGenerateVoucher(PARAM_TRUE);//是否生成凭证
            }
            dataSettled.setIssettlementcanmodified(PARAM_TRUE);//是否结算系统可修改 1-是
            //当“事项类型=统收统支协同单”时，待结算数据推送接口，是否允许拆分=是，是否允许合并=是。
            if (EventType.Unified_Synergy.getValue() == fundbill.getShort("billtype")) {
                dataSettled.setIsmerge(PARAM_TRUE);
                dataSettled.setIssplit(PARAM_FALSE);
            } else if (settlemode == 2 && bill.getNoteno() != null) {
                dataSettled.setIssettlementcanmodified(PARAM_FALSE);//结算系统不可修改
                dataSettled.setIsmerge(PARAM_FALSE);// 不可合并
                dataSettled.setIssplit(PARAM_FALSE);// 不可拆分
            } else if (settlemode == 2 && bill.getNoteno() == null) {
                dataSettled.setIssettlementcanmodified(PARAM_TRUE);//结算系统可修改
                dataSettled.setIsmerge(PARAM_TRUE);// 可合并
                dataSettled.setIssplit(PARAM_TRUE);// 可拆分
            } else {
                dataSettled.setIsmerge(PARAM_TRUE);//
                dataSettled.setIssplit(PARAM_TRUE); // 可以拆分
            }
            if (Boolean.parseBoolean(params.get("bCheck").toString())) {
                dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
            } else {
                dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
            }
            dataSettled.setExpectsettlemethod(bill.getSettlemode());//期望结算方式
            // 是否关联
            dataSettled.setIsRelateCheckBill(ValueUtils.isNotEmptyObj(bill.getAssociationStatus()) ?
                    bill.getAssociationStatus().toString() : null);
            // 对象单ID
            dataSettled.setRelateBankCheckBillId(ValueUtils.isNotEmptyObj(bill.getBankReconciliationId()) ?
                    Long.parseLong(bill.getBankReconciliationId()) : null);
            // 认领单ID
            dataSettled.setRelateClaimBillId(ValueUtils.isNotEmptyObj(bill.getBillClaimId()) ?
                    Long.parseLong(bill.getBillClaimId()): null);

            // 是否要占用资金计划：isToPushCspl,如果现金已占用，则传0，否则传1
            if (ValueUtils.isNotEmptyObj(bill.getIsToPushCspl()) && bill.getIsToPushCspl() == 1) {
                dataSettled.setIsToPushCspl((short) 0);
            } else {
                dataSettled.setIsToPushCspl((short) 1);
            }
            // 资金计划项目
            if (ValueUtils.isNotEmptyObj(bill.getString("fundPlanProject"))){
                dataSettled.setCsplProject(Long.parseLong(bill.getString("fundPlanProject")));
            }
            // 资金计划明细
            if (ValueUtils.isNotEmptyObj(bill.get("fundPlanProjectDetail"))){
                dataSettled.setCsplSummaryDetail(Long.parseLong(bill.getString("fundPlanProjectDetail")));
            }
            //付款模式
            Short paymentMode = bill.getPaymentMode();//付款模式
            if(ObjectUtils.isEmpty(paymentMode) || paymentMode == FundPaymentMode.VoluntaryPayment.getValue()){
                dataSettled.setPaySettlementMode("1");
            } else if (paymentMode == FundPaymentMode.CounterpartyDeduction.getValue()) { // 对方扣款
                dataSettled.setPaySettlementMode("2");
            }
            //支票id和支票账号
            if(null != bill.getCheckId()){
                dataSettled.setNoteno(bill.getCheckno());
                dataSettled.setNotenoid(bill.getCheckId());
            }

            // 统收统支关系组字段处理
            dataSettled.setIsIncomeAndExpenditure(bill.getIsIncomeAndExpenditure());
            if (ValueUtils.isNotEmptyObj(bill.getIncomeAndExpendRelationGroup())) {
                dataSettled.setIncomeAndExpendRelatChildId(bill.getIncomeAndExpendRelationGroup());
            }
            if (ValueUtils.isNotEmptyObj(bill.getIncomeAndExpendBankAccount())) {
                dataSettled.setMarginaccount(bill.getIncomeAndExpendBankAccount());
            }

            // 对公对私字段处理
            String publicPrivate = bill.getPublicPrivate();
            dataSettled.setPublicPrivate(publicPrivate != null ? Short.parseShort(publicPrivate) : null);

            dataSettled.setSourceAppcode(ICmpConstant.YONBIP_FI_CTMCMP);

            // 异币种改造：待结算数据新增接口增加结算币种、换出汇率类型、换出汇率预估
            dataSettled.setExchangePaymentCurrency(bill.getSettleCurrency());
            dataSettled.setExchangePaymentRate(bill.getSwapOutExchangeRateEstimate());
            dataSettled.setExchangeRateType(bill.getSwapOutExchangeRateType());
            dataSettled.setExchangePaymentRateType(fundbill.getExchangeRateType());
            if(bill.getExchangerateOps() != null){
                dataSettled.setExchangerateOps(bill.getExchangerateOps());
            }
            if (ValueUtils.isNotEmptyObj(fundbill.get("wbs"))){
                dataSettled.setWbs(fundbill.get("wbs"));
            }
            if (ValueUtils.isNotEmptyObj(fundbill.get("activity"))){
                dataSettled.setActivity(Long.parseLong(fundbill.get("activity").toString()));
            }

            // 将结算中心代理账户传递给待结算
            if (ValueUtils.isNotEmptyObj(bill.getActualSettleAccount())){
                dataSettled.setActualSettleAccount(bill.getActualSettleAccount());
            }
            if(ValueUtils.isNotEmptyObj(bill.getNetIdentificateCode())){
                dataSettled.setNetIdentificateCode(bill.getNetIdentificateCode());
            }

            if(ValueUtils.isNotEmptyObj(bill.getShort("netSettleCount"))){
                dataSettled.setNetSettleCount(bill.getShort("netSettleCount"));
            }
            if(ValueUtils.isNotEmptyObj(bill.getAfterNetAmt())){
                dataSettled.setAfterNetAmt(bill.getAfterNetAmt());
            }
            if(ValueUtils.isNotEmptyObj(bill.getShort("afterNetDir"))){
                if (bill.getShort("afterNetDir") == PaymentType.FundCollection.getValue()) {
                    dataSettled.setAfterNetDir((short) 1); //收付类型 1-收款，2-付款
                } else {
                    dataSettled.setAfterNetDir((short) 2); //收付类型 1-收款，2-付款
                }
            }
            dataSettled.setExchangePaymentAmount(bill.getSwapOutExchangeRateEstimate());
            dataSettleds.add(dataSettled);
        }
        return dataSettleds;
    }


    public static List<Map<String, Object>> builderSettleMap(BizObject sourceData, String makeBillRuleCode, String sourceBusiObj, List<String> childIds, List<String> topushList) throws Exception {
        ConvertParam convertParam = buildConvertParam(sourceData, makeBillRuleCode, sourceBusiObj, childIds);
        //调用转换规则服务 开始转换单据
        DomainMakeBillRuleModel makeBillRuleModel = businessConvertService.queryMakeBillRule(convertParam);
        if (Objects.isNull(makeBillRuleModel)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400807", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
        }
        ConvertResult convert = businessConvertService.convert(convertParam, makeBillRuleModel);
        if (Objects.isNull(convert) || com.yonyou.cloud.utils.CollectionUtils.isEmpty(convert.getConvertedBillList())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400807", "调用转换规则服务，转换单据失败！") /* "调用转换规则服务，转换单据失败！" */);
        }
        List<Map<String, Object>> settleMapList = new ArrayList<>();
        convert.getConvertedBillList().forEach(convertedBill -> {
            Map<String, Object> convertedBillData = convertedBill.getTargetData();
            convertedBillData.put("sourceCode", sourceBusiObj.equals("ctm-cmp.cmp_fundcollection") ?
                    IBillNumConstant.FUND_COLLECTION : IBillNumConstant.FUND_PAYMENT);
            convertedBillData.put("sourceBusiObj", sourceBusiObj);
            // 获取子表数据
            List<Map<String, Object>> settleBench_b = (List<Map<String, Object>>) convertedBillData.get("settleBench_b");
            List<Map<String, Object>> newsettleBench_b = new ArrayList<>();
            // 增加判空及金额不为 0 的判断，只有满足条件才添加到列表
            if (settleBench_b != null && !settleBench_b.isEmpty()) {
                for (Map<String, Object> item : settleBench_b) {
                    Object originalCurrencyAmtObj = item.get("originalcurrencyamt");
                    Object bizbilldetailid = item.get("bizbilldetailid");
                    // 防止空指针，并判断金额是否不等于 0 且不是委托拒绝的
                    if (originalCurrencyAmtObj != null && BigDecimal.valueOf(Double.parseDouble(originalCurrencyAmtObj.toString())).compareTo(BigDecimal.ZERO) != 0
                            && !topushList.isEmpty() && !Objects.isNull(bizbilldetailid) && topushList.contains(bizbilldetailid.toString())) {
                        newsettleBench_b.add(item);
                    }
                }
                convertedBillData.put("settleBench_b", newsettleBench_b);
            }
            settleMapList.add(convertedBillData);
        });
        return settleMapList;
    }

    public static ConvertParam buildConvertParam(BizObject sourceData, String makeBillRuleCode, String busiObj,
                                                  List<String> childIds) throws CtmException {
        ConvertParam convertParam = new ConvertParam();
        convertParam.setMakeBillRuleCode(makeBillRuleCode);
        convertParam.setRetry(false);
        convertParam.setTenantId(InvocationInfoProxy.getTenantid());
        convertParam.setDomain("ctm-cmp");
        // 取应用编码
        convertParam.setSubId("CM");
        convertParam.setBillNum(busiObj);
        convertParam.setSourceBills(Collections.singletonList(sourceData));
        convertParam.setSourceIds(Collections.singletonList(sourceData.getId()));
        if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(childIds)) {
            convertParam.setChildIds(childIds);
        }
        convertParam.setNeedQueryBill(true);
        return convertParam;
    }

    /**
     * *
     * @param bill
     * @param params
     * @return
     * @throws Exception
     */
    public static List<DataSettled> builderForeignPayment(ForeignPayment bill, Map<String, Object> params) throws Exception {
        List<DataSettled> dataSettleds = new ArrayList<>();
        DataSettled dataSettled = new DataSettled();
        // 主表数据
        dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
        dataSettled.setAccentity(bill.getAccentity());//资金组织
        dataSettled.setOrg(bill.getOrg());//业务组织
        dataSettled.setBusinessbillnum(bill.getCode());//单据编号
        dataSettled.setBusinessId(bill.getId().toString());//单据id
        dataSettled.setBusinessdetailsid(bill.getId().toString());//业务单据明细id
        dataSettled.setTradetype(bill.getTradetype());//交易类型
        dataSettled.setOribilldate(bill.getVouchdate());//单据日期
        dataSettled.setNatcurrency(bill.getNatCurrency());//本币币种
        dataSettled.setRecpaytype("2"); //收付类型 1-收款，2-付款

        dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.ForeignPayment.getValue())); //外汇付款单
        dataSettled.setOricurrency(bill.getCurrency());//原币币种
        dataSettled.setOricurrencyamount(bill.getAmount());//原币金额
        dataSettled.setIssinglebatch(PARAM_FALSE);// 单笔/批量
        dataSettled.setExchangerate(bill.getCurrencyexchRate());//汇率
        dataSettled.setExchangerateOps(bill.getCurrencyexchRateOps());//汇率
        dataSettled.setNatSum(bill.getCurrencyamount());//本币金额
        dataSettled.setProceedtype(bill.getQuickType());//款项类型
        dataSettled.setDept(bill.getDept());//部门
        dataSettled.setProject(bill.getProject());//项目
        dataSettled.setCheckIdentificationCode(bill.getSmartcheckno());//勾兑号
        dataSettled.setEnterpriseBankAccount(bill.getPaymenterprisebankaccount());//银行账户
        dataSettled.setExpenseitem(bill.getExpenseitem()); //费用项目
//        dataSettled.setRemark(bill.getDescription());//备注
        dataSettled.setEntryType(bill.getEntrytype()); //入账类型
        dataSettled.setPostscript(bill.getPostscript());//汇款附言
        int caobject = bill.getReceivetype();
        dataSettled.setToaccnttype(String.valueOf(caobject));//对方类型参照
        if (caobject == 1) {// 客户
            fundCommonService.checkCaObjectAccountNoEqual(CaObject.Customer.getValue(), bill.getReceivebankaccountid(), bill.getReceivebankaccount());
            dataSettled.setCounterpartyid(bill.getReceivenameid());//客户id
            dataSettled.setCounterpartybankaccount(bill.getReceivebankaccountid());//银行账户id
            dataSettled.setShowoppositebankaccount(bill.getReceivebankaccount()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getReceivebankaccountname()); //收款方账户名称
        } else if (caobject == 2) {// 供应商
            fundCommonService.checkCaObjectAccountNoEqual(CaObject.Supplier.getValue(), bill.getReceivebankaccountid(), bill.getReceivebankaccount());
            dataSettled.setCounterpartyid(bill.getReceivenameid());//供应商id
            dataSettled.setCounterpartybankaccount(bill.getReceivebankaccountid());//银行账户id
            dataSettled.setShowoppositebankaccount(bill.getReceivebankaccount()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getReceivebankaccountname()); //收款方账户名称
        } else if (caobject == 3) {// 员工
            dataSettled.setCounterpartyid(bill.getReceivenameid());//员工id
            dataSettled.setCounterpartybankaccount(bill.getReceivebankaccountid());//银行账户id
            dataSettled.setShowoppositebankaccount(bill.getReceivebankaccount()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getReceivebankaccountname()); //收款方账户名称
        } else if (caobject == 4) {// 其他

            //银行类别id
            String oppositebankTypeId = bill.getReceivebanktype();
            //根据类别id查询 银行类别name
            if (ObjectUtils.isNotEmpty(oppositebankTypeId)) {
                BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(oppositebankTypeId);
                //todo 增加判空
                if (ObjectUtils.isNotEmpty(bankVO)) {
                    String bankType = bankVO.getName();
                    dataSettled.setOppositeBankTypeName(bankType);// 对方银行类别
                }
            }

        } else if (caobject == 5) {//资金业务对象
            //获取资金业务对象id
            String capBizObj = bill.getCapBizObj();
            String typeId = queryCapBizObjType(capBizObj);
            dataSettled.setCapBizObjType(typeId); //资金业务对象类型id
            dataSettled.setCounterpartyid(bill.getReceivenameid());//资金业务对象id
            dataSettled.setCounterpartybankaccount(bill.getReceivebankaccountid());//银行账户id
            dataSettled.setShowtoaccntname(bill.getReceivename()); //显示对方名称 - 收款单位名称
            dataSettled.setShowoppositebankaccount(bill.getReceivebankaccount()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getReceivebankaccountname()); //收款方账户名称

        }else if (caobject == 6) {//内部单位
            dataSettled.setCounterpartyid(bill.getReceivenameid());//内部单位d
            dataSettled.setCounterpartybankaccount(bill.getReceivebankaccountid());//内部单位银行账户id
            dataSettled.setShowoppositebankaccount(bill.getReceivebankaccount()); //收款方账户号
            dataSettled.setShowoppositebankaccountname(bill.getReceivebankaccountname()); //收款方账户名称
        }

        dataSettled.setShowtoaccntname(bill.getReceivename()); //显示对方名称 - 收款单位名称
        dataSettled.setShowoppositebankaccount(bill.getReceivebankaccount()); //收款方账户号
        dataSettled.setShowoppositebankaccountname(bill.getReceivebankaccountname()); //收款方账户名称

        //开户网点id
        String oppositebankNumberId = bill.getReceivebankaddr();
        if (ObjectUtils.isNotEmpty(oppositebankNumberId)) {
            BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(oppositebankNumberId);
            String bankName = bankdotVO.getName();
            String Lineno = bankdotVO.getLinenumber();
            dataSettled.setShowoppositebankname(bankName);//收款方银行网点
            dataSettled.setShowoppositebanklineno(Lineno); // 对方银行联行号
        }

        //是否退票
        dataSettled.setIsrefund(bill.getIsrefund());
        //根据结算状态判断
        if (bill.getSettlestatus() == FundSettleStatus.Refund.getValue() || bill.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()
                || bill.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
            dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
        } else if (bill.getSettlestatus() == FundSettleStatus.WaitSettle.getValue() || bill.getSettlestatus() == FundSettleStatus.SettleProssing.getValue()) {
            dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.NormalSettle.getValue()));//来源数据结算状态 - 一般结算
        }
        dataSettled.setIssettlementcanmodified(PARAM_FALSE);//是否结算方式可以修改
        dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账
        dataSettled.setIsGenerateVoucher(PARAM_TRUE);//是否生成凭证
        dataSettled.setIsmerge(PARAM_FALSE);// 不可合并
        dataSettled.setIssplit(PARAM_FALSE);// 不可拆分

        dataSettled.setExpectsettlemethod(bill.getSettlemode());//期望结算方式
        dataSettled.setExpectpaydate(bill.getExpectedsettlementdate()); //期望结算日期
        if(bill.getSettleExchangeRateOps() != null){
            dataSettled.setExchangerateOps(bill.getSettleExchangeRateOps());
        }
        // 是否关联
        dataSettled.setIsRelateCheckBill(ValueUtils.isNotEmptyObj(bill.getIsassociatedbankbill()) ?
                bill.getIsassociatedbankbill().toString() : null);
        // 对账单单ID
        dataSettled.setRelateBankCheckBillId(ValueUtils.isNotEmptyObj(bill.getAssociationbankbillid()) ?
                Long.parseLong(bill.getAssociationbankbillid()) : null);
        // 认领单ID
        dataSettled.setRelateClaimBillId(ValueUtils.isNotEmptyObj(bill.getAssociationbillclaimid()) ?
                Long.parseLong(bill.getAssociationbillclaimid()) : null);

        // 是否要占用资金计划：isToPushCspl,如果现金已占用，则传0，否则传1
        if (ValueUtils.isNotEmptyObj(bill.getIsToPushCspl()) && bill.getIsToPushCspl() == 1) {
            dataSettled.setIsToPushCspl((short) 0);
        } else {
            dataSettled.setIsToPushCspl((short) 1);
        }
        // 资金计划项目
        if (ValueUtils.isNotEmptyObj(bill.getFundPlanProject())){
            dataSettled.setCsplProject(Long.parseLong(bill.getFundPlanProject()));
        }
        //付款模式
        Short paymentMode = bill.getPaymentsettlemode();//付款模式cmp_PaymentSettlemode
        if (ObjectUtils.isEmpty(paymentMode) || paymentMode == PaymentSettlemode.ActiveSettlement.getValue()) {
            dataSettled.setPaySettlementMode("1");
        } else if (paymentMode == PaymentSettlemode.CounterpartyDeduction.getValue()) { // 对方扣款
            dataSettled.setPaySettlementMode("2");
        }
        // 统收统支关系组字段处理
        dataSettled.setIsIncomeAndExpenditure(bill.getIsIncomeAndExpenditure() == 1? true : false);
        // 对公对私字段处理
        dataSettled.setPublicPrivate(bill.getPublicorprivate());
        //是否跨境
        dataSettled.setInoutFlag(bill.getIscrossborder());
        //是否跨行
        dataSettled.setBankFlag(bill.getBankflag());

        dataSettled.setSourceAppcode(ICmpConstant.YONBIP_FI_CTMCMP);
        // 异币种改造：待结算数据新增接口增加结算币种、换出汇率类型、换出汇率预估
        dataSettled.setExchangePaymentCurrency(bill.getAccountcurrency());
        dataSettled.setExchangePaymentRate(bill.getSwapOutExchangeRateEstimate());
        dataSettled.setExchangePaymentRateOps(bill.getSwapOutExchangeRateEstimateOps());
        dataSettled.setExchangeRateType(bill.getSwapOutExchangeRateType());
        dataSettled.setExchangePaymentRateType(bill.getCurrencyexchangeratetype());

        DataSettledCrossBorder dataSettledCrossBorder = new DataSettledCrossBorder();
        //是否加急
        if (bill.getIsurgent() == 1) {
            dataSettledCrossBorder.setUrgentFlag("H");
        } else {
            dataSettledCrossBorder.setUrgentFlag("N");
        }
        // 付款方常驻国家地区
        dataSettledCrossBorder.setPaybankcountry(bill.getPaycountry());
        //收款方非中文名称 现金管理非必填
        dataSettledCrossBorder.setRecaccountnamenoncn(bill.getReceivenameother());
        //收款方常驻国家地区
        dataSettledCrossBorder.setReceivercountry(bill.getReceivecountry());
        //收款方地址  现金管理非必填
        dataSettledCrossBorder.setRecaddress(bill.getReceiveaddress());
        //是否通过代理行
        dataSettledCrossBorder.setIsagencybank(bill.getIsagencybank());
        //收款方开户行在其代理行账号
        dataSettledCrossBorder.setAgencybankaccount(bill.getAgencybankaccount());
        //代理行名称
        dataSettledCrossBorder.setAgencybankname(bill.getAgencybankname());
        //代理行地址
        dataSettledCrossBorder.setAgencybankaddress(bill.getAgencybankaddress());
        //代理行SWIFT
        dataSettledCrossBorder.setAgencybankswift(bill.getAgencybankswift());
        //收款方开户行SWIFT
        dataSettledCrossBorder.setRecbankswift(bill.getReceivebankswift());
        //收款方开户行地址
        dataSettledCrossBorder.setRecbankaddressnoncn(bill.getReceivebankaddress());
        //国内外费用承担方
        dataSettledCrossBorder.setChargingmethod(bill.getCostbearers());
        //费用支付账号
        dataSettledCrossBorder.setChargingbankaccount(bill.getPaymentaccount());
        //费用支付账号（外币）
        dataSettledCrossBorder.setForeignchargingbankaccount(bill.getForeignpaymentaccount());
        //交易编码A
        dataSettledCrossBorder.setTransactioncodeA(bill.getTransactioncodeA());
        //交易币种A
        dataSettledCrossBorder.setTransactioncurrencyA(bill.getTransactioncurrencyA());
        //交易金额A
        dataSettledCrossBorder.setTransactionamountA(bill.getTransactionamountA());
        //交易附言A
        dataSettledCrossBorder.setTradepostscriptA(bill.getTradepostscriptA());
        //交易编码B
        dataSettledCrossBorder.setTransactioncodeB(bill.getTransactioncodeB());
        //交易币种B
        dataSettledCrossBorder.setTransactioncurrencyB(bill.getTransactioncurrencyB());
        //交易金额B
        dataSettledCrossBorder.setTransactionamountB(bill.getTransactionamountB());
        //交易附言B
        dataSettledCrossBorder.setTradepostscriptB(bill.getTradepostscriptB());
        //资金用途
        dataSettled.setRemark(bill.getFundpurpose());
        //付款类型 现金管理非必填
        dataSettledCrossBorder.setPaymenttype(ValueUtils.isNotEmptyObj(bill.getFundtype()) ?
                bill.getFundtype().toString() : null);
        //付款性质 现金管理非必填
        dataSettledCrossBorder.setPaymentnature(ValueUtils.isNotEmptyObj(bill.getPaymentnature()) ?
                bill.getPaymentnature().toString() : null);
        //是否为保税货物项下付款  现金管理非必填
        dataSettledCrossBorder.setIsbondedgoodspay(bill.getIsbondedgoodspay());
        //合同号  现金管理非必填
        dataSettledCrossBorder.setContractnumber(bill.getContractnumber());
        //发票号  现金管理非必填
        dataSettledCrossBorder.setInvoicenumber(bill.getInvoicenumber());
        //外汇局批件号/备案表号/业务编号  现金管理非必填
//        dataSettledCrossBorder.setFileinfo(bill.getFilingnumber());
        //填报人姓名  现金管理非必填
        dataSettledCrossBorder.setAppliername(bill.getApplicantname());
        //填报人电话  现金管理非必填
        dataSettledCrossBorder.setAppliercontactinfo(bill.getApplicantphonenumber());
        //收款方常驻国家地区代码
        dataSettledCrossBorder.setRecPerCountryCode(bill.getReceivecountrycode());
        //原申报号
        dataSettledCrossBorder.setOriDeclarationNum(bill.getDeclarationnumber());
        //付款方名称（非中文）
        dataSettledCrossBorder.setNcPayBankAccName(bill.getPayernamenocn());
        //汇款人地址
        dataSettledCrossBorder.setRemitAddress(bill.getAddress());
        dataSettled.setCrossBorderInfo(dataSettledCrossBorder);

        if (Boolean.parseBoolean(params.get("bCheck").toString())) {
            dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
        } else {
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
        }
        dataSettleds.add(dataSettled);
        return dataSettleds;
    }

    /**
     * 构建支付保证金推送实体*
     *
     * @param bill
     * @param params
     * @return
     */
    public static List<DataSettled> builderPaymargin(PayMargin bill, Map<String, Object> params) throws Exception {
        List<DataSettled> dataSettleds = new ArrayList<>();
        DataSettled dataSettled = new DataSettled();
        // 主表数据
        dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
        dataSettled.setAccentity(bill.getAccentity());//资金组织
        dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.PaymentMarginManagement.getValue())); //支付保证金台账管理
        dataSettled.setBusinessbillnum(bill.getCode());//单据编号
        dataSettled.setBusinessId(bill.getId().toString());//单据id
        dataSettled.setBusinessdetailsid(bill.getId().toString());//业务单据明细id
        dataSettled.setTradetype(bill.getTradetype());//交易类型
        if (bill.getPaymenttype() == PaymentType.FundCollection.getValue()) {
            dataSettled.setRecpaytype("1"); //收付类型 1-收款，2-付款
        } else {
            dataSettled.setRecpaytype("2"); //收付类型 1-收款，2-付款
        }
        dataSettled.setOribilldate(bill.getVouchdate());//单据日期
        dataSettled.setOricurrency(bill.getCurrency());//原币币种
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String marginamount = decimalFormat.format(bill.getMarginamount());
        dataSettled.setOricurrencyamount(new BigDecimal(marginamount));//原币金额
        dataSettled.setNatcurrency(bill.getNatCurrency());//本币币种
        String natmarginamount = decimalFormat.format(bill.getNatmarginamount());
        dataSettled.setNatSum(new BigDecimal(natmarginamount));//本币金额
        dataSettled.setExchangePaymentRateType(bill.getExchangeratetype());//折本币汇率类型
        dataSettled.setExchangerate(bill.getExchRate());//汇率
        dataSettled.setExchangerateOps(bill.getExchRateOps());
        dataSettled.setIssinglebatch(PARAM_FALSE);// 单笔/批量
        dataSettled.setExpectsettlemethod(bill.getSettlemode());//期望结算方式
        // 期望结算日期=默认等于传结算的系统业务日期 ，没有业务日期时默认当前系统日期
        Date currentDate = new Date();
        if (BillInfoUtils.getBusinessDate() != null) {
            dataSettled.setExpectpaydate(BillInfoUtils.getBusinessDate()); //期望结算日期=//业务日期
        } else {
            dataSettled.setExpectpaydate(currentDate);//当前系统日期

        }

        dataSettled.setDept(bill.getDept());//部门
        dataSettled.setProject(bill.getProject());//项目
        Short paymentsettlemode = bill.getPaymentsettlemode();//付款结算模式
        if (ObjectUtils.isNotEmpty(paymentsettlemode) && paymentsettlemode == PaymentSettlemode.ActiveSettlement.getValue()) { // 主动结算
            dataSettled.setPaySettlementMode("1");
        } else if (ObjectUtils.isNotEmpty(paymentsettlemode) && paymentsettlemode == PaymentSettlemode.CounterpartyDeduction.getValue()) { // 被动扣款
            dataSettled.setPaySettlementMode("2");
        }
        /*
         * 对方类型 为其他时候 对方名称必填
         */
        Short toaccnttype = bill.getOppositetype();
        if (toaccnttype == MarginOppositeType.OwnOrg.getValue()) { //内部单位
            dataSettled.setToaccnttype("6");
            dataSettled.setCounterpartyid(bill.getOurname());//内部单位id
            dataSettled.setCounterpartybankaccount(bill.getOurbankaccount());//内部单位银行账户id
        } else if (toaccnttype == MarginOppositeType.Customer.getValue()) { //客户
            dataSettled.setToaccnttype("1");
            dataSettled.setCounterpartyid(ValueUtils.isNotEmptyObj(bill.getCustomer()) ? bill.getCustomer().toString() : null);//客户id
            dataSettled.setCounterpartybankaccount(ValueUtils.isNotEmptyObj(bill.getCustomerbankaccount()) ? bill.getCustomerbankaccount().toString() : null);//客户银行账户id
        } else if (toaccnttype == MarginOppositeType.Supplier.getValue()) { //供应商
            dataSettled.setToaccnttype("2");
            dataSettled.setCounterpartyid(ValueUtils.isNotEmptyObj(bill.getSupplier()) ? bill.getSupplier().toString() : null);//供应商id
            dataSettled.setCounterpartybankaccount(ValueUtils.isNotEmptyObj(bill.getSupplierbankaccount()) ? bill.getSupplierbankaccount().toString() : null);//供应商银行账户id
        } else if (toaccnttype == MarginOppositeType.CapBizObj.getValue()) { //资金业务对象
            dataSettled.setToaccnttype("5");
            //获取资金业务对象id
            String capBizObj = bill.getCapBizObj();
            String typeId = queryCapBizObjType(capBizObj);
            dataSettled.setCapBizObjType(typeId); //资金业务对象类型id

            dataSettled.setCounterpartyid(bill.getCapBizObj()); //资金业务对象id
            dataSettled.setCounterpartybankaccount(bill.getCapBizObjbankaccount());//资金业务对象银行账户id
        } else if (toaccnttype == MarginOppositeType.Other.getValue()) { //其他
            dataSettled.setToaccnttype("4");
            //银行类别id
            String oppositebankTypeId = bill.getOppositebankType();
            //根据类别id查询 银行类别name
            if (ObjectUtils.isNotEmpty(oppositebankTypeId)) {
                BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(oppositebankTypeId);
                String bankType = bankVO.getName();
                dataSettled.setOppositeBankTypeName(bankType);// 对方银行类别
            }

        }
        dataSettled.setShowtoaccntname(bill.getOppositename()); //显示对方名称 - 对方单位名称
        dataSettled.setShowoppositebankaccount(bill.getOppositebankaccount()); //对方账户号
        dataSettled.setShowoppositebankaccountname(bill.getOppositebankaccountname()); //对方账户名称
        //开户网点id
        String oppositebankNumberId = bill.getOppositebankNumber();
        if (ObjectUtils.isNotEmpty(oppositebankNumberId)) {
            BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(oppositebankNumberId);
            String bankName = bankdotVO.getName();
            String Lineno = bankdotVO.getLinenumber();
            dataSettled.setShowoppositebankname(bankName);//收款方银行网点
            dataSettled.setShowoppositebanklineno(Lineno); // 对方银行联行号
        }

        //推送推送次数标识
        dataSettled.setSerialNumber(bill.getPushtimes());
        dataSettled.setEnterpriseBankAccount(bill.getEnterprisebankaccount());//本方银行账号
        dataSettled.setRemark(bill.getDescription());//备注
        //根据结算状态判断
        if (bill.getSettlestatus() == FundSettleStatus.Refund.getValue() || bill.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()
                || bill.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
            dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
        } else if (bill.getSettlestatus() == FundSettleStatus.WaitSettle.getValue()) {
            dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.NormalSettle.getValue()));//来源数据结算状态 - 一般结算
        }
        dataSettled.setIssettlementcanmodified(PARAM_TRUE);//是否结算方式可修改
//        dataSettled.setIssettlementcanmodified(PARAM_FALSE);//结算系统可修改
        dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账
        dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成凭证
        dataSettled.setIsmerge(PARAM_TRUE);//可合并
        dataSettled.setIssplit(PARAM_FALSE); //不可拆分
        //勾兑号
        dataSettled.setCheckIdentificationCode(bill.getOurcheckno());//本方勾兑号
        // 是否关联
        dataSettled.setIsRelateCheckBill(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getOurassociationstatus()) ?
                bill.getOurassociationstatus().toString() : null);
        // 对账单ID
        dataSettled.setRelateBankCheckBillId(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getOurbankbillid()) ?
                Long.parseLong(bill.getOurbankbillid()) : null);
        // 认领单ID
        dataSettled.setRelateClaimBillId(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getOurbillclaimid()) ?
                Long.parseLong(bill.getOurbillclaimid()) : null);
        if(bill.getExchRateOps() != null){
            dataSettled.setExchangerateOps(bill.getExchRateOps());
        }
        //轧差识别码
        dataSettled.setNetIdentificateCode(bill.getNetIdentificateCode());
        dataSettled.setNetSettleCount(bill.getNetSettleCount());
        dataSettled.setAfterNetAmt(bill.getAfterNetAmt());
        if (ObjectUtils.isNotEmpty(bill.getAfterNetDir())) {
            if (bill.getAfterNetDir() == PaymentType.FundCollection.getValue()) {
                dataSettled.setAfterNetDir((short) 1); //收付类型 1-收款，2-付款
            } else {
                dataSettled.setAfterNetDir((short) 2); //收付类型 1-收款，2-付款
            }
        }

        if (Boolean.parseBoolean(params.get("bCheck").toString())) {
            dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
        } else {
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
        }
        dataSettled.setSourceAppcode(ICmpConstant.YONBIP_FI_CTMCMP);
        dataSettleds.add(dataSettled);
        return dataSettleds;
    }

    /**
     * 构建支付保证金推送实体-第二次*
     * 支付保证金同名账户划转 场景*
     *
     * @param bill
     * @param params
     * @return
     * @throws Exception
     */
    public static List<DataSettled> builderPaymarginSecond(PayMargin bill, Map<String, Object> params) throws Exception {
        List<DataSettled> dataSettleds = new ArrayList<>();
        DataSettled dataSettled = new DataSettled();
        // 主表数据
        dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
        dataSettled.setAccentity(bill.getAccentity());//资金组织
        dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.PaymentMarginManagement.getValue())); //保证金存入支取单
        dataSettled.setBusinessbillnum(bill.getCode());//单据编号
        dataSettled.setBusinessId(bill.getId().toString());//单据id
        dataSettled.setBusinessdetailsid(bill.getId().toString());//业务单据明细id
        dataSettled.setTradetype(bill.getTradetype());//交易类型
        //传收款
        dataSettled.setRecpaytype("1"); //收付类型 1-收款，2-付款
        dataSettled.setOribilldate(bill.getVouchdate());//单据日期
        dataSettled.setOricurrency(bill.getCurrency());//原币币种
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String marginamount = decimalFormat.format(bill.getMarginamount());
        dataSettled.setOricurrencyamount(new BigDecimal(marginamount));//原币金额
        dataSettled.setNatcurrency(bill.getNatCurrency());//本币币种
        String natmarginamount = decimalFormat.format(bill.getNatmarginamount());
        dataSettled.setNatSum(new BigDecimal(natmarginamount));//本币金额
        dataSettled.setExchangePaymentRateType(bill.getExchangeratetype());//折本币汇率类型
        dataSettled.setExchangerate(bill.getExchRate());//汇率
        dataSettled.setExchangerateOps(bill.getExchRateOps());
        dataSettled.setIssinglebatch(PARAM_FALSE);// 单笔/批量
        dataSettled.setExpectsettlemethod(bill.getSettlemode());//期望结算方式
        // 第二笔的期望结算日期取第一笔的结算成功日期
        dataSettled.setExpectpaydate(bill.getDate("secondExpectSettleDate"));
        dataSettled.setDept(bill.getDept());//部门
        dataSettled.setProject(bill.getProject());//项目
        Short paymentsettlemode = bill.getPaymentsettlemode();//付款结算模式
        if (ObjectUtils.isNotEmpty(paymentsettlemode) && paymentsettlemode == PaymentSettlemode.ActiveSettlement.getValue()) { // 主动结算
            dataSettled.setPaySettlementMode("1");
        } else if (ObjectUtils.isNotEmpty(paymentsettlemode) && paymentsettlemode == PaymentSettlemode.CounterpartyDeduction.getValue()) { // 被动扣款
            dataSettled.setPaySettlementMode("2");
        }

        // 对方类型为 内部单位
        dataSettled.setToaccnttype("6");
        // 对方银行信息id
        dataSettled.setCounterpartyid(bill.getAccentity());//内部单位id
        dataSettled.setCounterpartybankaccount(bill.getEnterprisebankaccount());//对方传本方


        //推送推送次数标识
        dataSettled.setSerialNumber(bill.getPushtimes());
        dataSettled.setEnterpriseBankAccount(bill.getOurbankaccount());//内部单位银行账户id
        dataSettled.setRemark(bill.getDescription());//备注
        //根据结算状态判断  第二次推送 直接是已结算补单
        dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单

        dataSettled.setIssettlementcanmodified(PARAM_TRUE);//是否结算方式可修改
//        dataSettled.setIssettlementcanmodified(PARAM_FALSE);//结算系统可修改
        dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账
        dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成凭证
        dataSettled.setIsmerge(PARAM_TRUE);//可合并
        dataSettled.setIssplit(PARAM_FALSE); //不可拆分
        if(bill.getExchRateOps() != null){
            dataSettled.setExchangerateOps(bill.getExchRateOps());
        }
        //勾兑号
        dataSettled.setCheckIdentificationCode(bill.getOppcheckno());//对方勾兑号
        // 是否关联
        dataSettled.setIsRelateCheckBill(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getOppassociationstatus()) ?
                bill.getOppassociationstatus().toString() : null);
        // 对账单ID
        dataSettled.setRelateBankCheckBillId(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getOppbankbillid()) ?
                Long.parseLong(bill.getOppbankbillid()) : null);
        // 认领单ID
        dataSettled.setRelateClaimBillId(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getOppbillclaimid()) ?
                Long.parseLong(bill.getOppbillclaimid()) : null);
        if (Boolean.parseBoolean(params.get("bCheck").toString())) {
            dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
        } else {
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
        }
        dataSettled.setSourceAppcode(ICmpConstant.YONBIP_FI_CTMCMP);
        dataSettleds.add(dataSettled);
        return dataSettleds;

    }


    /**
     * 构建收到保证金推送实体*
     *
     * @param bill
     * @param params
     * @return
     */
    public static List<DataSettled> builderReceivemargin(ReceiveMargin bill, Map<String, Object> params) throws Exception {
        List<DataSettled> dataSettleds = new ArrayList<>();
        DataSettled dataSettled = new DataSettled();
        // 主表数据
        dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
        dataSettled.setAccentity(bill.getAccentity());//资金组织
        dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.ReceiptMarginManagement.getValue())); //收到保证金台账管理
        dataSettled.setBusinessbillnum(bill.getCode());//单据编号
        dataSettled.setBusinessId(bill.getId().toString());//单据id
        dataSettled.setBusinessdetailsid(bill.getId().toString());//业务单据明细id
        dataSettled.setTradetype(bill.getTradetype());//交易类型
        if (bill.getPaymenttype() == PaymentType.FundCollection.getValue()) {
            dataSettled.setRecpaytype("1"); //收付类型 1-收款，2-付款
        } else {
            dataSettled.setRecpaytype("2"); //收付类型 1-收款，2-付款
        }
        dataSettled.setOribilldate(bill.getVouchdate());//单据日期
        dataSettled.setOricurrency(bill.getCurrency());//原币币种
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String marginamount = decimalFormat.format(bill.getMarginamount());
        dataSettled.setOricurrencyamount(new BigDecimal(marginamount));//原币金额
        dataSettled.setNatcurrency(bill.getNatCurrency());//本币币种
        String natmarginamount = decimalFormat.format(bill.getNatmarginamount());
        dataSettled.setNatSum(new BigDecimal(natmarginamount));//本币金额
        dataSettled.setExchangePaymentRateType(bill.getExchangeratetype());//折本币汇率类型
        dataSettled.setExchangerate(bill.getExchRate());//汇率
        dataSettled.setExchangerateOps(bill.getExchRateOps());
        dataSettled.setIssinglebatch(PARAM_FALSE);// 单笔/批量
        dataSettled.setExpectsettlemethod(bill.getSettlemode());//期望结算方式
        // 期望结算日期=默认等于传结算的系统业务日期 ，没有业务日期时默认当前系统日期
        Date currentDate = new Date();
        if (BillInfoUtils.getBusinessDate() != null) {
            dataSettled.setExpectpaydate(BillInfoUtils.getBusinessDate()); //期望结算日期=//业务日期
        } else {
            dataSettled.setExpectpaydate(currentDate);//当前系统日期

        }
        dataSettled.setDept(bill.getDept());//部门
        dataSettled.setProject(bill.getProject());//项目
        Short paymentsettlemode = bill.getPaymentsettlemode();//付款结算模式
        if (ObjectUtils.isNotEmpty(paymentsettlemode) && paymentsettlemode == PaymentSettlemode.ActiveSettlement.getValue()) { // 主动结算
            dataSettled.setPaySettlementMode("1");
        } else if (ObjectUtils.isNotEmpty(paymentsettlemode) && paymentsettlemode == PaymentSettlemode.CounterpartyDeduction.getValue()) { // 被动扣款
            dataSettled.setPaySettlementMode("2");
        }
        /*
         * 对方类型 为其他时候 对方名称必填
         */
        Short toaccnttype = bill.getOppositetype();
        if (toaccnttype == MarginOppositeType.OwnOrg.getValue()) { //内部单位
            dataSettled.setToaccnttype("6");
            dataSettled.setCounterpartyid(bill.getOurname());//内部单位id
            dataSettled.setCounterpartybankaccount(bill.getOurbankaccount());//内部单位银行账户id

        } else if (toaccnttype == MarginOppositeType.Customer.getValue()) { //客户
            dataSettled.setToaccnttype("1");
            dataSettled.setCounterpartyid(ValueUtils.isNotEmptyObj(bill.getCustomer()) ? bill.getCustomer().toString() : null);//客户id
            dataSettled.setCounterpartybankaccount(ValueUtils.isNotEmptyObj(bill.getCustomerbankaccount()) ? bill.getCustomerbankaccount().toString() : null);//客户银行账户id
        } else if (toaccnttype == MarginOppositeType.Supplier.getValue()) { //供应商
            dataSettled.setToaccnttype("2");
            dataSettled.setCounterpartyid(ValueUtils.isNotEmptyObj(bill.getSupplier()) ? bill.getSupplier().toString() : null);//供应商id
            dataSettled.setCounterpartybankaccount(ValueUtils.isNotEmptyObj(bill.getSupplierbankaccount()) ? bill.getSupplierbankaccount().toString() : null);//供应商银行账户id
        } else if (toaccnttype == MarginOppositeType.CapBizObj.getValue()) { //资金业务对象
            dataSettled.setToaccnttype("5");
            //获取资金业务对象id
            String capBizObj = bill.getCapBizObj();
            String typeId = queryCapBizObjType(capBizObj);
            dataSettled.setCapBizObjType(typeId); //资金业务对象类型id
            dataSettled.setCounterpartyid(bill.getCapBizObj()); //资金业务对象id
            dataSettled.setCounterpartybankaccount(bill.getCapBizObjbankaccount());//资金业务对象银行账户id
        } else if (toaccnttype == MarginOppositeType.Other.getValue()) { //其他
            dataSettled.setToaccnttype("4");
            //银行类别id
            String oppositebankTypeId = bill.getOppositebankType();
            //根据类别id查询 银行类别name
            if (ObjectUtils.isNotEmpty(oppositebankTypeId)) {
                BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(oppositebankTypeId);
                String bankType = bankVO.getName();
                dataSettled.setOppositeBankTypeName(bankType);// 对方银行类别
            }
        }

        dataSettled.setShowtoaccntname(bill.getOppositename()); //显示对方名称 - 对方单位名称
        dataSettled.setShowoppositebankaccount(bill.getOppositebankaccount()); //对方账户号
        dataSettled.setShowoppositebankaccountname(bill.getOppositebankaccountname()); //对方账户名称
        //开户网点id
        String oppositebankNumberId = bill.getOppositebankNumber();
        if (ObjectUtils.isNotEmpty(oppositebankNumberId)) {
            BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(oppositebankNumberId);
            String bankName = bankdotVO.getName();
            String Lineno = bankdotVO.getLinenumber();
            dataSettled.setShowoppositebankname(bankName);//收款方银行网点
            dataSettled.setShowoppositebanklineno(Lineno); // 对方银行联行号
        }

        dataSettled.setEnterpriseBankAccount(bill.getEnterprisebankaccount());//本方银行账号
        dataSettled.setRemark(bill.getDescription());//备注
        //根据结算状态判断
        if (bill.getSettlestatus() == FundSettleStatus.Refund.getValue() || bill.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()
                || bill.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue()) {
            dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.SettleDone.getValue()));//来源数据结算状态 - 已结算补单
        } else if (bill.getSettlestatus() == FundSettleStatus.WaitSettle.getValue()) {
            dataSettled.setOpenwsettlestatus(String.valueOf(OpenWSettleStatus.NormalSettle.getValue()));//来源数据结算状态 - 一般结算
        }
        dataSettled.setIssettlementcanmodified(PARAM_TRUE);//是否结算方式可修改
//        dataSettled.setIssettlementcanmodified(PARAM_FALSE);//结算系统可修改
        dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账
        dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成凭证
        dataSettled.setIsmerge(PARAM_TRUE);//可合并
        dataSettled.setIssplit(PARAM_FALSE); //不可拆分
        if(bill.getExchRateOps() != null){
            dataSettled.setExchangerateOps(bill.getExchRateOps());
        }
        //勾兑号
        dataSettled.setCheckIdentificationCode(bill.getCheckno());//对方勾兑号
        // 是否关联
        dataSettled.setIsRelateCheckBill(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getAssociationstatus()) ?
                bill.getAssociationstatus().toString() : null);
        // 对账单ID
        dataSettled.setRelateBankCheckBillId(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getBankbillid()) ?
                Long.parseLong(bill.getBankbillid()) : null);
        // 认领单ID
        dataSettled.setRelateClaimBillId(com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(bill.getBillclaimid()) ?
                Long.parseLong(bill.getBillclaimid()) : null);
        if (Boolean.parseBoolean(params.get("bCheck").toString())) {
            dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
        } else {
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
        }
        dataSettled.setSourceAppcode(ICmpConstant.YONBIP_FI_CTMCMP);
        dataSettleds.add(dataSettled);
        return dataSettleds;
    }

    /**
     * 构建转账单推送数据
     * @param transferAccount
     * @return
     */
    public static List<DataSettled> builderTransferData(TransferAccount transferAccount, Map<String, Object> params, int settlemode, String checkBillNo) throws Exception {
        List<DataSettled> dataSettleds = new ArrayList<>();
        DataSettled dataSettled = new DataSettled();
        // 主表数据
        DataSettledCrossBorder dataSettledCrossBorder = new DataSettledCrossBorder();
        dataSettledCrossBorder.setRecbankswift(transferAccount.getSwiftCode());
        dataSettled.setCrossBorderInfo(dataSettledCrossBorder);
        dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));// 来源业务系统 8-现金管理
        dataSettled.setAccentity(transferAccount.getAccentity());//资金组织
        dataSettled.setBusinessbilltype(String.valueOf(BusinessBillType.TransferBill.getValue())); //转账单类型
        dataSettled.setBusinessbillnum(transferAccount.getCode());//转账单单据编号
        dataSettled.setBusinessId(transferAccount.getId().toString());//业务单据ID -> 转账单-ID
        dataSettled.setBusinessdetailsid(transferAccount.getId().toString());//业务单据明细ID -> 转账单-ID
        dataSettled.setTradetype(transferAccount.getTradetype());//交易类型
        dataSettled.setOribilldate(transferAccount.getVouchdate());//转账单单据日期
        dataSettled.setOricurrency(transferAccount.getCurrency());//原币币种
        // 不占用资金计划
        dataSettled.setIsToPushCspl((short)0);
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String oriSum = decimalFormat.format(transferAccount.getOriSum());
        dataSettled.setOricurrencyamount(new BigDecimal(oriSum));//原币金额
        String natSum = decimalFormat.format(transferAccount.getNatSum());
        dataSettled.setNatSum(new BigDecimal(natSum));//本币金额
        dataSettled.setNatcurrency(transferAccount.getNatCurrency());//本币币种
        dataSettled.setExchangePaymentRateType(transferAccount.getExchangeRateType());//汇率类型
        dataSettled.setExchangerate(transferAccount.getExchRate());//汇率
        dataSettled.setExchangerateOps(transferAccount.getExchRateOps());
        dataSettled.setIssinglebatch(PARAM_FALSE);//单笔 0:单笔 1:批量
        dataSettled.setRemark(transferAccount.getDescription());//备注对备注
        dataSettled.setPostscript(transferAccount.getPurpose());//用途对附言
        //国机相关：传递支付扩展信息
        dataSettled.setPayExtend(transferAccount.getPayExtend());
        dataSettled.setToaccnttype("6");//对方类型 4->其他
        dataSettled.setCounterpartyid(transferAccount.getAccentity()); //对方档案id
        //对方账户名称 转账单交易类型为银行转账、现金缴存时：转账单-收款银行账户
        String tradeType = transferAccount.getType();//交易类型
        // 交易类型
        String type = transferAccount.getType();
        BdTransType bdTransType = transTypeQueryService.findById(transferAccount.get("tradetype"));
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeTypeCode = (String) jsonObject.get("transferType_zz");
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setId(transferAccount.get("settlemode"));
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = refRpcService.querySettleMethods(settleMethodQueryParam);
        if (dataList != null && dataList.size() > 0) {
            if (dataList.get(0).getServiceAttr().equals(0)) { // 0表示结算方式为银行转账
                dataSettled.setEnterpriseBankAccount(transferAccount.getPayBankAccount());//本方银行账号->付款银行账户（账号）
            } else if (dataList.get(0).getServiceAttr().equals(1)) { //1 表示结算方式为现金
                if("sc".equals(type) || "jcxj".equalsIgnoreCase(tradeTypeCode)){//现金缴存
                    dataSettled.setCashaccount(null);
                } else {
                    dataSettled.setCashaccount(transferAccount.getPayCashAccount());//本方现金账号->付款现金账户（账号）
                }

            } else if (dataList.get(0).getServiceAttr().equals(10)) { // 10 表示结算方式为第三方
                dataSettled.setThirdParVirtAccount(transferAccount.getPayVirtualAccount());
            }else if(dataList.get(0).getServiceAttr().equals(8)){//结算方式 = 支票
                dataSettled.setNoteno(String.valueOf(transferAccount.getCheckid()));
            }
        }
        // ===========fukk  start  ======================
        // 同名账户划转的交易类型=缴存现金时，改为先传收款方向的待结算数据 其他情况保持原有逻辑不变 【陕建】
        String recpaytype="2"; // 转账单收付类型 //付款

        EnterpriseBankAcctVO oppositebankaccount =  new EnterpriseBankAcctVO();
        EnterpriseBankAcctVO ourbankaccount =  new EnterpriseBankAcctVO();
        if (StringUtils.isNotEmpty(transferAccount.getPayBankAccount()) || StringUtils.isNotEmpty(transferAccount.getRecBankAccount())) {
            Map<String, EnterpriseBankAcctVO> enterpriseBankAcctVOMap = BaseDocUtils.getBankAcctMap(Arrays.asList(transferAccount.getPayBankAccount(), transferAccount.getRecBankAccount()));
            if (ObjectUtils.isNotEmpty(transferAccount.getRecBankAccount())) {
                oppositebankaccount = enterpriseBankAcctVOMap.get(transferAccount.getRecBankAccount());
            }
            if (ObjectUtils.isNotEmpty(transferAccount.getPayBankAccount())) {
                ourbankaccount = enterpriseBankAcctVOMap.get(transferAccount.getPayBankAccount());
            }
        }

        if("sc".equals(type) || "jcxj".equalsIgnoreCase(tradeTypeCode)){ //现金缴存
            //判断收款银行账户是否内部户 内部户才需要先推送收款 再推送付款
            if (ObjectUtils.isNotEmpty(oppositebankaccount) && oppositebankaccount.getAcctopentype().equals(1)) {
                recpaytype="1"; //收款
                dataSettled.setEnterpriseBankAccount(transferAccount.getRecBankAccount()); // 待结算本方银行账户
                dataSettled.setCounterpartybankaccount(transferAccount.getPayCashAccount());
                dataSettled.setExpectsettlemethod(transferAccount.getCollectsettlemode()); //期望结算方式
                dataSettled.setInoutFlag((short) 0);
            } else {
                dataSettled.setCashaccount(transferAccount.getPayCashAccount());
                dataSettled.setCounterpartybankaccount(transferAccount.getRecBankAccount());
                dataSettled.setExpectsettlemethod(transferAccount.getSettlemode());//期望结算方式
                dataSettled.setInoutFlag((short) 0);
            }

        }else if ("ec".equals(type) || "tqxj".equalsIgnoreCase(tradeTypeCode)) { //现金提取
            dataSettled.setEnterpriseBankAccount(transferAccount.getPayBankAccount()); // 待结算本方银行账户
            dataSettled.setCounterpartybankaccount(transferAccount.getRecCashAccount()); // 对方账户
            dataSettled.setExpectsettlemethod(transferAccount.getSettlemode());//期望结算方式
            dataSettled.setInoutFlag((short) 0);

        } else if ("BT".equalsIgnoreCase(type) || "yhzz".equalsIgnoreCase(tradeTypeCode)) { //银行转账
            dataSettled.setCounterpartybankaccount(transferAccount.getRecBankAccount());//对方银行账户档案id
            dataSettled.setExpectsettlemethod(transferAccount.getSettlemode());//期望结算方式
            dataSettled.setInoutFlag((short) 0);
        } else if ("CT".equalsIgnoreCase(type) || "xjhz".equals(tradeTypeCode)) { //现金互转
            dataSettled.setCashaccount(transferAccount.getPayCashAccount());//本方现金账号->付款现金账户（账号）
            dataSettled.setCounterpartybankaccount(transferAccount.getRecCashAccount());
            dataSettled.setExpectsettlemethod(transferAccount.getSettlemode());//期望结算方式
            dataSettled.setInoutFlag((short) 0);
        } else if ("TPT".equalsIgnoreCase(type) || "dsfzz".equals(tradeTypeCode)){ //三方
            if (transferAccount.getVirtualBank() == 1) {//银行账户转虚拟户
                dataSettled.setCounterpartybankaccount(transferAccount.getCollVirtualAccount());
            } else {
                if(ObjectUtils.isNotEmpty(oppositebankaccount)) {//虚拟账户转银行账户
                    dataSettled.setCounterpartybankaccount(transferAccount.getRecBankAccount());//对方银行账户档案id
                }
            }
            dataSettled.setExpectsettlemethod(transferAccount.getSettlemode());//期望结算方式
            dataSettled.setInoutFlag((short) 0);
        }
        // 数币钱包交易类型
        buildDgDataSettled(transferAccount, dataSettled, ourbankaccount, type, tradeTypeCode);
        dataSettled.setRecpaytype(recpaytype); //转账单收付类型 1-收款，2-付款
        // ===========fukk  end  ======================

        //转账单交易类型为银行转账、现金互转、缴存现金时，为空
        if ("BT".equalsIgnoreCase(tradeType) || "yhzz".equalsIgnoreCase(tradeTypeCode)
                || "CT".equalsIgnoreCase(tradeType) || "xjhz".equals(tradeTypeCode)
                || "SC".equalsIgnoreCase(tradeType) || "jcxj".equalsIgnoreCase(tradeTypeCode)) {
            dataSettled.setPaySettlementMode(null);//付款结算模式->空
            if("BT".equalsIgnoreCase(tradeType)){
                dataSettled.setShowourbankaccount(ourbankaccount.getAccount());//本方银行账号
            }
        }  else if ("EC".equalsIgnoreCase(tradeType) || "ec".equals(transferAccount.getType()) || "tqxj".equalsIgnoreCase(tradeTypeCode)) {
            dataSettled.setNoteno(checkBillNo);//票证号-（支票编号）
            dataSettled.setNotenoid(String.valueOf(transferAccount.getCheckid()));//票证号id-（支票id）
            dataSettled.setNotenoamount(transferAccount.getOriSum());//票面金额-（转账单转账金额）
            //交易类型为提取现金，结算方式为支票结算时，付款结算模式传主动扣款
            //判断银行户（付款银行账户）是不是内部户  如果是内部户（结算中心） 传主动付款  否则是被动
            //付款银行账号开户类型
            Integer acctopentype = ourbankaccount.getAcctopentype();
            if (settlemode == 8 || acctopentype.equals(1)) {
                dataSettled.setPaySettlementMode("1"); //1 表示主动扣款
            } else {
                dataSettled.setPaySettlementMode("2"); //2表示对方扣款  转账单交易类型为提取现金时，必填，传对方扣款
            }
        } else if ("TPT".equalsIgnoreCase(tradeType) || "dsfzz".equals(tradeTypeCode)) {
            if (transferAccount.getVirtualBank() == 1) {//银行账户转虚拟户
                dataSettled.setPaySettlementMode("2");//2表示对方扣款  当三方转账类型为银行户转虚拟户时，必填，传对方扣款
            } else {
                dataSettled.setPaySettlementMode(null);//付款结算模式->空 当三方转账类型为虚拟户转银行户时，为空
            }
        }
        if (SettleStatus.SettledRep.equals(transferAccount.getSettlestatus())) { //来源数据结算状态:转账单结算状态为已结算补单时，传已结算补单
            dataSettled.setOpenwsettlestatus("2");//2表示已结算补单
        } else {
            dataSettled.setOpenwsettlestatus("0");//0表示一般结算
        }
        dataSettled.setIssettlementcanmodified(PARAM_TRUE);//是否结算方式可修改
        // 本方结算信息不可修改
        dataSettled.setIsOpenNote((short)1);
        dataSettled.setIsIncomeAndExpenditure(false);//是否统收统支
        dataSettled.setIsmerge(PARAM_FALSE);//是否可合并结算 否
        dataSettled.setIssplit(PARAM_FALSE);//是否可拆分结算 否
        dataSettled.setIsjournalregistered(PARAM_TRUE);//是否登记日记账 是
        dataSettled.setIsGenerateVoucher(PARAM_FALSE);//是否生成结算凭证 否
        if (Boolean.parseBoolean(params.get("bCheck").toString())) {
            dataSettled.setExternaloutdefine1(PARAM_FALSE);// 只校验不保存
        } else {
            dataSettled.setExternaloutdefine1(PARAM_TRUE);// 保存
        }
        dataSettled.setExternaloutdefine1(PARAM_TRUE);// 结算系统是否接入为是
        if (transferAccount.getAssociationStatusPay() != null && transferAccount.getAssociationStatusPay() == true) {
            dataSettled.setIsRelateCheckBill(PARAM_TRUE);
            dataSettled.setRelateBankCheckBillId(transferAccount.getPaybankbill());//关联银行对账单id
            dataSettled.setRelateClaimBillId(transferAccount.getPaybillclaim());//关联认领单id
        } else {
            dataSettled.setIsRelateCheckBill(PARAM_FALSE);
        }

        dataSettled.setDept(transferAccount.getDept());//部门
        dataSettled.setProject(transferAccount.getProject());//项目
        //设置 转账单 勾兑号 zxl
        //财资统一对账码修改，统一在付方向时传递 财资统一对账码（付款），在收方向时传递 财资统一对账码（收款）
        if ("2".equals(dataSettled.getRecpaytype())){ //付款
            dataSettled.setCheckIdentificationCode(transferAccount.getPaysmartcheckno());
        }
        if ("1".equals(dataSettled.getRecpaytype())){ //收款
            dataSettled.setCheckIdentificationCode(transferAccount.getSmartcheckno());
        }
        if(transferAccount.getExchRateOps() != null){
            dataSettled.setExchangerateOps(transferAccount.getExchRateOps());
        }
        // ============fukk start ======================
        // 对方账户类型赋值 【陕建】
        Short oppAccType=null;   // 现金账户=2    银行账户=3  虚拟账户=4
        // 结算中心受理类型 赋值【陕建】
        Short stctAcceptType=null; // 待受理=1 不传结算中心=3
        String openwsettlestatus = dataSettled.getOpenwsettlestatus();
        if("ec".equalsIgnoreCase(type) || "tqxj".equalsIgnoreCase(tradeTypeCode)){//现金提取
            if (org.apache.commons.lang3.StringUtils.equals("2",recpaytype)){
                oppAccType=2;
            }
            if (org.apache.commons.lang3.StringUtils.equals("1",recpaytype)){
                oppAccType=3;
            }
            // 判断开户类型 结算中心开户的传受理类型 否则传 null
            Integer acctopentype = ourbankaccount.getAcctopentype();
            if (acctopentype.equals(1)) {
                if (org.apache.commons.lang3.StringUtils.equals(openwsettlestatus,String.valueOf(OpenWSettleStatus.SettleDone.getValue()))){ //已结算补单
                    stctAcceptType=3;
                }
            }

        }else if("sc".equalsIgnoreCase(type) || "jcxj".equalsIgnoreCase(tradeTypeCode)){//现金缴存
            //转账单收付类型 1-收款，2-付款
            if (org.apache.commons.lang3.StringUtils.equals("2",recpaytype)){
                oppAccType=3;
            }
            if (org.apache.commons.lang3.StringUtils.equals("1",recpaytype)){
                oppAccType=2;
            }
            // 判断开户类型 结算中心开户的传受理类型 否则传 null
            Integer acctopentype = oppositebankaccount.getAcctopentype();
            if (acctopentype.equals(1)) {
                if (org.apache.commons.lang3.StringUtils.equals(openwsettlestatus,String.valueOf(OpenWSettleStatus.SettleDone.getValue()))){ //已结算补单
                    stctAcceptType=3;
                }
                dataSettled.setIsRelateCheckBill(transferAccount.getAssociationStatusCollect() == true ? PARAM_TRUE:PARAM_FALSE);//是否关联对账单
                if(true == transferAccount.getAssociationStatusCollect()){
                    dataSettled.setRelateBankCheckBillId(transferAccount.getCollectbankbill());//关联银行对账单id
                    dataSettled.setRelateClaimBillId(transferAccount.getCollectbillclaim());//关联认领单id
                }
            }
        }else if("tpt".equalsIgnoreCase(type) || "dsfzz".equalsIgnoreCase(tradeTypeCode)){//三方转账
            if(transferAccount.getVirtualBank() ==0){ // 虚拟账户转银行账户=0  银行账户转虚拟账户=1
                if (org.apache.commons.lang3.StringUtils.equals("2",recpaytype)){
                    oppAccType=3;
                }
                if (org.apache.commons.lang3.StringUtils.equals("1",recpaytype)){
                    oppAccType=4;
                }
            }else{
                if (org.apache.commons.lang3.StringUtils.equals("2",recpaytype)){
                    oppAccType=4;
                }
                if (org.apache.commons.lang3.StringUtils.equals("1",recpaytype)){
                    oppAccType=3;
                }
            }
        }else if("ct".equalsIgnoreCase(type) || "xjhz".equalsIgnoreCase(tradeTypeCode)){//现金互转
           //  交易类型=现金互转，付款方向、收款方向的待结算数据对方账户类型都传“现金账户”；
            oppAccType=2;
        }else if("bt".equalsIgnoreCase(type) || "yhzz".equalsIgnoreCase(tradeTypeCode)){//银行转账
            // 交易类型=银行转账时，付款方向、收款方向的待结算数据对方账户类型都传“银行账户”；
            oppAccType=3;
        }
        dataSettled.setOppAccType(oppAccType);
        dataSettled.setStctAcceptType(stctAcceptType);  // 赋值

        //dataSettled.setShowtoaccntname(transferAccount.getOppositeName()); //对方名称
        dataSettled.setShowoppositebankaccountname(transferAccount.getOppositeBankAccountName()); //对方银行账户名称
        //dataSettled.setShowoppositebankname(transferAccount.getOppositebankNumber());
        dataSettled.setShowoppositebankaccount(transferAccount.getOppositebankAccount()); //对方银行账号
        //开户网点id
        if (ObjectUtils.isNotEmpty(transferAccount.getOppositebankNumber())) {
            BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(transferAccount.getOppositebankNumber());
            String bankName = bankdotVO.getName();
            String Lineno = bankdotVO.getLinenumber();
            dataSettled.setShowoppositebankname(bankName);//对方开户网点
            dataSettled.setShowoppositebanklineno(Lineno); // 对方银行联行号
        }
        // ============fukk end ======================
        dataSettleds.add(dataSettled);
        return dataSettleds;
    }

    /**
     * 构建数币交易类型待结算数据
     * @param transferAccount
     * @param dataSettled
     * @param ourbankaccount
     * @param type
     * @param tradeTypeCode
     */
    private static void buildDgDataSettled(TransferAccount transferAccount, DataSettled dataSettled, EnterpriseBankAcctVO ourbankaccount, String type, String tradeTypeCode) {
        List<String> tradeTypeCodeList = Arrays.asList("WCZ", "WTX", "WHZ");
        List<String> tradeTypeExtList = Arrays.asList("sbqbcz", "sbqbtx", "sbqbhz");
        if (!(tradeTypeCodeList.contains(type) || tradeTypeExtList.contains(tradeTypeCode))) {
            return;
        }
        // 对方银行账户档案id
        dataSettled.setCounterpartybankaccount(transferAccount.getRecBankAccount());
        // 期望结算方式
        dataSettled.setExpectsettlemethod(transferAccount.getSettlemode());
        dataSettled.setInoutFlag((short) 0);
        // 本方银行账号
        dataSettled.setShowourbankaccount(ourbankaccount.getAccount());
        // 对方账户类型
        dataSettled.setOppAccType((short)3);
        // 付款结算模式->空
        dataSettled.setPaySettlementMode(null);
    }

    /**
     * 获取银行账户信息
     * @param oppositebankaccount
     * @throws Exception
     */
    public static Map<String,String> getBankAccountInfo(Map<String, Object> oppositebankaccount) throws Exception {
        Map<String, String> map = new HashMap<>();
        BaseRefRpcService baseRefRpcService = AppContext.getBean(BaseRefRpcService.class);
        if (oppositebankaccount != null) {
            String acctName = (String) oppositebankaccount.get("acctName");//对方账户名称
            String account = (String) oppositebankaccount.get("account");//对方银行账号
            String bankNumberName = "";
            String bankType = "";
            String lineNumber = "";
            BankdotVO bankdotVO = baseRefRpcService.queryBandDotById((String)oppositebankaccount.get("bankNumber"));
            if(ObjectUtils.isNotEmpty(bankdotVO)){
                bankNumberName = bankdotVO.getName();//对方开户行名
                lineNumber = bankdotVO.getLinenumber();//对方开户行联行号
            }
            BankVO bankVO = baseRefRpcService.queryBankTypeById((String) oppositebankaccount.get("bank"));
            if(ObjectUtils.isNotEmpty(bankVO)){
                bankType = bankVO.getName();//对方银行类别
            }
            if (StringUtils.isEmpty(lineNumber)) {
                if (oppositebankaccount.get("lineNumber") != null) {
                    lineNumber = (String)oppositebankaccount.get("lineNumber");//对方开户行联行号
                }
            }
            map.put("acctName",acctName);
            map.put("account",account);
            map.put("bankNumberName",bankNumberName);
            map.put("bankType",bankType);
            map.put("lineNumber",lineNumber);
        }
        return map;
    }


    public static String queryCapBizObjType(String capBizObjId) throws Exception {

        String typeId = "";
        if (ObjectUtils.isNotEmpty(capBizObjId)) {
            //查询资金业务对象
            BillContext context = new BillContext();
            context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
            context.setDomain("yonbip-fi-ctmtmsp");
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id, accentity, fundbusinobjtypename, enabled, fundbusinobjtypeid");
            schema.appendQueryCondition(QueryCondition.name("id").eq(capBizObjId));
            List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);
            if (CollectionUtils.isNotEmpty(result)) {
                //获取数据实体
                CtmJSONObject jsonObject = new CtmJSONObject(result.get(0));
                //获取资金业务对象类型id
                typeId = jsonObject.get("fundbusinobjtypeid").toString();
            }
        }
        return typeId;
    }
    /**
     * 推结算，获取转账单的勾兑号 zxl
     *
     * @param transferAccount
     * @return
     * @throws Exception
     */
    public static String getCheckIdentificationCode(TransferAccount transferAccount) throws Exception {
        /**
         * 因为生成的结算，是付款的结算单，所以应该传付款的；但是付款结算单结算完成，会自动生成收款的结算单，这个时候收款的结算单的勾兑号应该是转账单上的收款勾兑号
         */
        //付款_银行对账单ID
        if (transferAccount.getPaybankbill() != null) {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, transferAccount.getPaybankbill());
            return bankReconciliation.getSmartcheckno();
        }
        //付款_认领单ID
        if (transferAccount.getPaybillclaim() != null) {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, transferAccount.getPaybillclaim());
            return billClaim.getSmartcheckno();
        }
        return null;
    }


}
