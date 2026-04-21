package com.yonyoucloud.fi.cmp.fundcommon.service;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.einvoice.dto.TaxRateArchiveDto;
import com.yonyou.einvoice.dto.TaxRateQueryCondition;
import com.yonyou.einvoice.service.itf.ITaxRateArchIrisService;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.event.service.EventService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IExchangeRateTypeService;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.template.CommonOperator;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.ctm.stwb.datasettled.OppAccTypeEnum;
import com.yonyoucloud.ctm.stwb.incomeandexpenditure.ControlledMarginaccount;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDistribute;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.reqvo.AgentPaymentReqVO;
import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;
import com.yonyoucloud.ctm.stwb.settleapply.pubitf.ISettleApplyPubQueryService;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettleStatus;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettlementResult;
import com.yonyoucloud.ctm.stwb.unifiedsettle.enums.SettleDetailSettleStateEnum;
import com.yonyoucloud.ctm.stwb.unifiedsettle.vo.UnifiedSettleDetail;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmcmpReWriteBusRpcServiceImpl;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollectionSubWithholdingRelation;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPaymentSubWithholdingRelation;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.CustomerQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
import com.yonyoucloud.fi.drft.api.openapi.ICtmDrftEndorePaybillRpcService;
import com.yonyoucloud.fi.drft.post.vo.base.BaseResultVO;
import com.yonyoucloud.fi.tmsp.enums.ApplyNameEnum;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import com.yonyoucloud.fi.tmsp.openapi.ITmspRefRpcService;
import com.yonyoucloud.fi.tmsp.openapi.ITmspSystemRespRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspRequestParams;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemReq;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemResp;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.NonNull;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.IBillNumConstant.*;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>IFundCommonService</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-22 14:41
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class FundCommonServiceImpl implements IFundCommonService {

    //统一增加内存级缓存，避免for循环内部n多次RPC调用（仅仅是还历史债务的方式，并不是最好的实现方式）
    /**
     * 缓存资金组织
     */
    private static final @NonNull Cache<String, FundsOrgDTO> fundsOrgDTOCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    /**
     * 缓存资金组织和是否参与计算
     */
    private static final @NonNull Cache<String, List<EnterpriseBankAcctVO>> bankAccountsCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    /**
     * 缓存资金组织和是否参与计算
     */
    private static final @NonNull Cache<String, List<SettleMethodModel>> settleMethodModelCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    private static final @NonNull Cache<String, List<TmspSystemResp>> tmspSystemRespListCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    @Autowired
    private ICmpSendEventService cmpSendEventService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    VendorQueryService vendorQueryService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    ProcessService processService;

    @Autowired
    private FIBillService fiBillService;

    @Autowired
    CtmcmpReWriteBusRpcServiceImpl reWriteBusRpcService;

    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService stwbBillPaymentService;

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    private StwbBillService stwbBillCollectionService;

    @Autowired
    private ITmspSystemRespRpcService iTmspSystemRespRpcService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private ICtmDrftEndorePaybillRpcService ctmDrftEndorePaybillRpcService;

    @Autowired
    private PushAndPullService pushAndPullService;

    @Resource
    private IApplicationService appService;

    @Resource
    private FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;

    @Autowired
    private SettleMethodService settlementService;

    @Autowired
    private ISettleApplyPubQueryService settleApplyPubQueryService;

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;
    @Autowired
    private EventService eventService;
    @Value("${cmp.tradetype.xtgenerationbill:NoSetTradetype}")
    String xt_tradetype;

    @Autowired
    private ITaxRateArchIrisService iTaxRateArchIrisService;
    static FundsOrgQueryServiceComponent fundsOrgQueryService = AppContext.getBean(FundsOrgQueryServiceComponent.class);

    private static final short ISREFUND = 1; // 是否退票，1-退票
    

    private static final @NonNull Cache<String, AgentFinancialDTO> agentFinancialDTOCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    private static final @NonNull Cache<String, VendorBankVO> vendorBankVOCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    @Override
    public void checkCaObjectAccountNoEqual(short caobject, String accountId, String accountNo) throws Exception {
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accountId)) {
            return;
        }
        switch (caobject) {
            case 1:
                //校验客户账号
                AgentFinancialDTO agentFinancialDTO = null;
                if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accountId)) {
                    AgentFinancialDTO agentFinancialDTOCacheValue = agentFinancialDTOCache.getIfPresent(accountId);
                    if (agentFinancialDTOCacheValue != null) {
                        agentFinancialDTO = agentFinancialDTOCacheValue;
                    } else {
                        agentFinancialDTO = AppContext.getBean(CustomerQueryService.class).getCustomerAccountByAccountId(Long.parseLong(accountId));
                        if(agentFinancialDTO != null){
                            agentFinancialDTOCache.put(accountId, agentFinancialDTO);
                        }
                    }
                }
                if (agentFinancialDTO != null) {
                    if (Objects.nonNull(accountNo)&&!accountNo.equals(agentFinancialDTO.getBankAccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100350"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508006D", "客户银行账号可能存在变更，请重新核对账号信息") /* "客户银行账号可能存在变更，请重新核对账号信息" */);
                    }
                }
                break;
            case 2:
                //校验供应商账号
                VendorBankVO vendorBankVO = null;
                if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accountId)) {
                    VendorBankVO vendorBankVOCacheValue = vendorBankVOCache.getIfPresent(accountId);
                    if (vendorBankVOCacheValue != null) {
                        vendorBankVO = vendorBankVOCacheValue;
                    } else {
                        vendorBankVO = AppContext.getBean(VendorQueryService.class).getVendorBanksByAccountId(accountId);
                        if (vendorBankVO != null) {
                            vendorBankVOCache.put(accountId, vendorBankVO);
                        }
                    }
                }
                if (vendorBankVO != null) {
                    if (Objects.nonNull(accountNo)&&!accountNo.equals(vendorBankVO.getAccount())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100351"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508006C", "供应商银行账号可能存在变更，请重新核对账号信息") /* "供应商银行账号可能存在变更，请重新核对账号信息" */);
                    }
                }
                break;
            case 3:
                //校验员工账号
                break;
            default:
                log.error("该类型不存在");
        }
    }

    @Override
    public void checkStaffOppositeAccount(String billnum, BizObject bizObject) throws Exception {
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
            //过滤出所有付款对象是员工的子表数据
            List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
            List<FundPayment_b> fundPaymentBs = billbs.stream().filter(biz -> biz.getShort("caobject") != null
                    && biz.getShort("caobject").shortValue() == 3
                    && org.apache.commons.lang3.StringUtils.isNotEmpty(biz.getOppositeobjectid())
                    && org.apache.commons.lang3.StringUtils.isNotEmpty(biz.getOppositeaccountid())
            ).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(fundPaymentBs)) {
                return;
            }
            Set<String> employeeIdSet = fundPaymentBs.stream().map(FundPayment_b::getOppositeobjectid).collect(Collectors.toSet());
            List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByStaffIds(employeeIdSet);
            //员工和员工账号映射
            Map<String, List<Map<String, Object>>> staffAccountMap = bankAccounts.stream().collect(Collectors.groupingBy(item -> item.get("staff_id").toString()));
            fundPaymentBs.forEach(fundPayment_b -> {
                List<Map<String, Object>> staffAccounts = staffAccountMap.get(fundPayment_b.getOppositeobjectid());
                String staffName = fundPayment_b.getOppositeobjectname();
                String staffAccountId = fundPayment_b.getOppositeaccountid();
                String staffAccountNo = fundPayment_b.getOppositeaccountno();
                String staffAccountName = fundPayment_b.getOppositeaccountname();
                checkstaff(staffAccounts, staffName, staffAccountId, staffAccountNo, staffAccountName);
            });
        } else {
            //过滤出所有付款对象是员工的子表数据
            List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
            List<FundCollection_b> fundCollectionBs = billbs.stream().filter(biz -> biz.getShort("caobject") != null
                    && biz.getShort("caobject").shortValue() == 3
                    && org.apache.commons.lang3.StringUtils.isNotEmpty(biz.getOppositeobjectid())
                    && org.apache.commons.lang3.StringUtils.isNotEmpty(biz.getOppositeaccountid())
            ).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(fundCollectionBs)) {
                return;
            }
            Set<String> employeeIdSet = fundCollectionBs.stream().map(FundCollection_b::getOppositeobjectid).collect(Collectors.toSet());
            List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByStaffIds(employeeIdSet);
            //员工和员工账号映射
            Map<String, List<Map<String, Object>>> staffAccountMap = bankAccounts.stream().collect(Collectors.groupingBy(item -> item.get("staff_id").toString()));
            fundCollectionBs.forEach(FundCollection_b -> {
                List<Map<String, Object>> staffAccounts = staffAccountMap.get(FundCollection_b.getOppositeobjectid());
                String staffName = FundCollection_b.getOppositeobjectname();
                String staffAccountId = FundCollection_b.getOppositeaccountid();
                String staffAccountNo = FundCollection_b.getOppositeaccountno();
                String staffAccountName = FundCollection_b.getOppositeaccountname();
                checkstaff(staffAccounts, staffName, staffAccountId, staffAccountNo, staffAccountName);
            });
        }
    }

    private void checkstaff(List<Map<String, Object>> staffAccounts, String staffName, String staffAccountId, String staffAccountNo, String staffAccountName) {
        if (CollectionUtils.isEmpty(staffAccounts)) {
            String msg = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2007F3F004280007", "员工[%s]未查到员工账户!") /* "员工[%s]未查到员工账户!" */, staffName);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103008"), msg);
        }
        Map<String, Object> staffAccount = staffAccounts.stream().filter(item -> staffAccountId.equals(item.get("id").toString())).findFirst().orElse(null);
        if (staffAccount == null) {
            String msg = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2007F47E04F80003", "员工[%s]的账户中不包含账户id：[%s]的数据!") /* "员工[%s]的账户中不包含账户id：[%s]的数据!" */, staffName, staffAccountId);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103009"), msg);
        } else {
            if (StringUtils.isNotEmpty(staffAccountNo) && !staffAccountNo.equals(staffAccount.get("account"))) {
                String msg = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2007F4EA04280008", "员工[%s]的账号[%s]与档案数据[%s]不符!") /* "员工[%s]的账号[%s]与档案数据[%s]不符!" */, staffName, staffAccountNo, staffAccount.get("account"));
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103010"), msg);
            }
            // 存在员工档案未维护账户名称的情况，如果有账户名称则校验账户名称和档案是否一致，否则判断前端的账户名称和员工名称是否一致
            if (staffAccount.get("accountname") != null) {
                if (StringUtils.isNotEmpty(staffAccountName) && !staffAccountName.equals(staffAccount.get("accountname"))) {
                    String msg = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2007F53804F80007", "员工[%s]的账户名[%s]与档案数据[%s]不符!") /* "员工[%s]的账户名[%s]与档案数据[%s]不符!" */, staffName, staffAccountName, staffAccount.get("accountname"));
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103011"), msg);
                }
            } else {
                if (!Objects.isNull(staffAccountName) && !staffAccountName.equals(staffName)) {
                    String msg = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2007F53804F80007", "员工[%s]的账户名[%s]与档案数据[%s]不符!") /* "员工[%s]的账户名[%s]与档案数据[%s]不符!" */, staffName, staffAccountName, staffAccount.get("accountname"));
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103011"), msg);
                }
            }

        }
    }

    @Override
    public void setSimpleSettleValue(String billnum, BizObject bizObject) throws Exception {
        bizObject.set("billDate", DateUtils.dateFormat(bizObject.get("vouchdate"), DateUtils.DATE_PATTERN));
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {// 资金付款单
            bizObject.set("bizObjType", "cmp.fundpayment.FundPayment");
            bizObject.set("billTypeId", "2553141119111680");
            List<FundPayment_b> billbs = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
            if (ValueUtils.isNotEmptyObj(billbs)) {
                BizObject fund = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId(), ICmpConstant.CONSTANT_TWO);
                Map<String, FundPayment_b> dbData = new HashMap<>();
                if (!Objects.isNull(fund)) {
                    List<String> ids = fund.getBizObjects("FundPayment_b", FundPayment_b.class).stream()
                            .map(item -> String.valueOf(item.getId().toString()))
                            .collect(Collectors.toList());
                    List<Map<String, Object>> fundblist = MetaDaoHelper.queryByIds(FundPayment_b.ENTITY_NAME, "id,netIdentificateCode,oppositeobjectid", ids);
                    if (CollectionUtils.isNotEmpty(fundblist)) {
                        for (Map<String, Object> map : fundblist) {
                            FundPayment_b fundPaymentB = new FundPayment_b();
                            fundPaymentB.setId(map.get("id").toString());
                            fundPaymentB.setNetIdentificateCode((String) map.get("netIdentificateCode"));
                            fundPaymentB.setOppositeobjectid((String) map.get("oppositeobjectid"));
                            dbData.put(map.get("id").toString(), fundPaymentB);
                        }
                    } else {
                        dbData = new HashMap<>();
                    }
                }
                for (FundPayment_b fundPaymentB : billbs) {
                    fundPaymentB.setNoriRemainAmount(fundPaymentB.getOriSum());
                    fundPaymentB.setSettlesuccessSum(new BigDecimal(0));
                    fundPaymentB.setSettleerrorSum(new BigDecimal(0));
                    fundPaymentB.setOriTransitAmount(new BigDecimal(0));
                    fundPaymentB.setBizobjtype(FundPayment.BUSI_OBJ_CODE);
                    fundPaymentB.set("transNumber", null);
                    fundPaymentB.setBizobjtype("cmp.fundpayment.FundPayment_b");
                    boolean settleFlag = bizObject.get("settleflag") != null && "0".equals(bizObject.get("settleflag").toString());
                    if (settleFlag) {
                        fundPaymentB.put("stwbSettleStatus", SettleApplyDetailStateEnum.NO_SETTLE.getValue());
                    } else {
                        fundPaymentB.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPaymentB.getFundSettlestatus()));
                    }
                    fundPaymentB.put("accentity", bizObject.get("accentity"));
                    fundPaymentB.put("directionType", "2");
                    fundPaymentB.set("billDate", DateUtils.dateFormat(bizObject.get("vouchdate"), DateUtils.DATE_PATTERN));
                    fundPaymentB.set("currency", bizObject.get("currency"));
                    fundPaymentB.set("oppId", fundPaymentB.getOppositeobjectid());
                    // 汇率类型
                    fundPaymentB.set("exchangeRateType", bizObject.get("exchangeRateType"));
                    // 本币汇率
                    fundPaymentB.set("exchRate", bizObject.get("exchRate"));
                    // 本币币种
                    fundPaymentB.set("natCurrency", bizObject.get("natCurrency"));
                    // 对方账户类型
                    fundPaymentB.set("eoppAcctType",
                            com.yonyoucloud.fi.cmp.util.StringUtils.isNotEmpty(fundPaymentB.getOppositeaccountno()) ?
                                    String.valueOf(OppAccTypeEnum.ENTERPRISE_BANK_ACC.getValue()) : null);
                    fundPaymentB.set("expectSettleDate", DateUtils.dateFormat(bizObject.get("vouchdate"), DateUtils.DATE_PATTERN));
                    fundPaymentB.set("sourceSystemId", "8");
                    // 对方单位
                    fundPaymentB.set("oppName", fundPaymentB.getOppositeobjectname());
                    //CM202400706 资金收付款单的轧差识别码字段落库存储时，若为空，默认赋值“对方档案ID“
                    boolean isnull = Objects.isNull(fundPaymentB.getNetIdentificateCode()) && !Objects.isNull(fundPaymentB.getOppositeobjectid());
                    boolean isInput = false;
                    if (!Objects.isNull(fund)) {
                        //当前轧差既不等于当前的对方档案id也不等于数据库已存的对方档案id那就是手输的
                        isInput = !Objects.isNull(fundPaymentB.getId())
                                && !Objects.isNull(dbData.get(fundPaymentB.getId().toString()).getNetIdentificateCode())
                                && !Objects.isNull(fundPaymentB.getNetIdentificateCode())
                                && Objects.nonNull(fundPaymentB.getOppositeobjectid())
                                && Objects.nonNull(dbData.get(fundPaymentB.getId().toString()).getOppositeobjectid())
                                && !fundPaymentB.getOppositeobjectid().equals(fundPaymentB.getNetIdentificateCode())
                                && !dbData.get(fundPaymentB.getId().toString()).getOppositeobjectid().equals(fundPaymentB.getNetIdentificateCode())
                                && dbData.get(fundPaymentB.getId().toString()).getOppositeobjectid().length() != fundPaymentB.getNetIdentificateCode().length();
                    }
                    if (enableSimplify && FundSettleStatus.SettlementSupplement.getValue() != fundPaymentB.getFundSettlestatus().getValue()
                            && (isnull || (EntityStatus.Update.equals(bizObject.getEntityStatus()) && !isInput)) && CaObject.Other.getValue() != fundPaymentB.getCaobject().getValue()) {
                        fundPaymentB.setNetIdentificateCode(fundPaymentB.getOppositeobjectid());
                    }
                    //双写行号
                    if (fundPaymentB.getLineno() != null) {
                        fundPaymentB.set("stwbRowNo", fundPaymentB.getLineno().toBigInteger().toString());
                    }
                    //轧差后金额
                    fundPaymentB.set("nettingAmount", fundPaymentB.getAfterNetAmt());
                    //轧差识别码
                    fundPaymentB.set("nettingCode", fundPaymentB.getNetIdentificateCode());
                    //轧差总笔数
                    fundPaymentB.set("nettingCount", fundPaymentB.getNetSettleCount());
                    if (Objects.nonNull(fundPaymentB.getAfterNetDir())) {
                        //轧差后收付方向
                        fundPaymentB.set("nettingDirection", fundPaymentB.getAfterNetDir().toString().equals("0") ? "1" : "2");
                    }
                    if (!Objects.isNull(fundPaymentB.getIncomeAndExpendBankAccount())) {
                        fundPaymentB.setActualSettleAccount(fundPaymentB.getIncomeAndExpendBankAccount());
                    }
                    TaxRateArchiveDto taxRateVO = new TaxRateArchiveDto();
                    if (!Objects.isNull(fundPaymentB.getTaxCategory()) && Objects.isNull(fundPaymentB.getTaxRate())) {
                        TaxRateQueryCondition params = new TaxRateQueryCondition();
                        params.setId(fundPaymentB.getTaxCategory());
                        //只查询状态为启用的数据
                        params.setEnables(Arrays.asList(1));
                        taxRateVO = iTaxRateArchIrisService.queryOneByParam(params);
                    } else if (Objects.isNull(fundPaymentB.getTaxCategory()) && !Objects.isNull(fundPaymentB.get("taxCategory_code"))) {
                        TaxRateQueryCondition params = new TaxRateQueryCondition();
                        params.setCode(fundPaymentB.get("taxCategory_code"));
                        //只查询状态为启用的数据
                        params.setEnables(Arrays.asList(1));
                        taxRateVO = iTaxRateArchIrisService.queryOneByParam(params);
                    }
                    if (!Objects.isNull(taxRateVO) && !Objects.isNull(taxRateVO.getId()) && Objects.isNull(fundPaymentB.getTaxRate())) {
                        fundPaymentB.setTaxCategory(taxRateVO.getId());
                        //税率=档案税率*档案计算系数
                        BigDecimal ntaxrate =
                                !Objects.isNull(taxRateVO.getCalculateCoefficient()) && taxRateVO.getCalculateCoefficient() == 1 ?
                                        taxRateVO.getNtaxrate().multiply(new BigDecimal(100)) : taxRateVO.getNtaxrate();
                        fundPaymentB.setTaxRate(ntaxrate);
                        //税额(付款不含税)=本币金额*税率
                        fundPaymentB.setTaxSum(BigDecimalUtils.safeMultiply(fundPaymentB.getNatSum().multiply(new BigDecimal(0.01)), ntaxrate));
                        //无税金额(付款含税) = 本币金额/(1+税率)
                        fundPaymentB.setUnTaxSum(BigDecimalUtils.safeDivide(fundPaymentB.getNatSum(), BigDecimalUtils.safeAdd(BigDecimal.ONE, BigDecimalUtils.safeMultiply(ntaxrate, new BigDecimal(0.01))), ICmpConstant.CONSTANT_TEN));
                        //税额(付款含税) = 本币金额-无税金额(付款含税)
                        fundPaymentB.setIncludeTaxSum(BigDecimalUtils.safeSubtract(fundPaymentB.getNatSum(), fundPaymentB.getUnTaxSum()));

                        fundPaymentB.setTaxSum(fundPaymentB.getTaxSum().setScale(8, RoundingMode.HALF_UP));
                        fundPaymentB.setUnTaxSum(fundPaymentB.getUnTaxSum().setScale(8, RoundingMode.HALF_UP));
                        fundPaymentB.setIncludeTaxSum(fundPaymentB.getIncludeTaxSum().setScale(8, RoundingMode.HALF_UP));
                    }
                }
            }
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {// 资金收款单
            bizObject.set("bizObjType", "cmp.fundcollection.FundCollection");
            bizObject.set("billTypeId", "2571640684663808");
            List<FundCollection_b> billbs = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
            if (ValueUtils.isNotEmptyObj(billbs)) {
                BizObject fund = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId(), ICmpConstant.CONSTANT_TWO);
                Map<String, FundCollection_b> dbData = new HashMap<>();
                if (!Objects.isNull(fund)) {
                    List<String> ids = fund.getBizObjects("FundCollection_b", FundCollection_b.class).stream()
                            .map(item -> String.valueOf(item.getId().toString()))
                            .collect(Collectors.toList());
                    List<Map<String, Object>> fundblist = MetaDaoHelper.queryByIds(FundCollection_b.ENTITY_NAME, "id,netIdentificateCode,oppositeobjectid", ids);
                    if (CollectionUtils.isNotEmpty(fundblist)) {
                        for (Map<String, Object> map : fundblist) {
                            FundCollection_b fundCollectionB = new FundCollection_b();
                            fundCollectionB.setId(map.get("id").toString());
                            fundCollectionB.setNetIdentificateCode((String) map.get("netIdentificateCode"));
                            fundCollectionB.setOppositeobjectid((String) map.get("oppositeobjectid"));
                            dbData.put(map.get("id").toString(), fundCollectionB);
                        }
                    } else {
                        dbData = new HashMap<>();
                    }
                }
                for (FundCollection_b fundCollectionB : billbs) {
                    fundCollectionB.setNoriRemainAmount(fundCollectionB.getOriSum());
                    fundCollectionB.setSettlesuccessSum(new BigDecimal(0));
                    fundCollectionB.setSettleerrorSum(new BigDecimal(0));
                    fundCollectionB.setOriTransitAmount(new BigDecimal(0));
                    fundCollectionB.setBizobjtype("cmp.fundcollection.FundCollection_b");
                    fundCollectionB.set("transNumber", null);
                    boolean settleFlag = bizObject.get("settleflag") != null && "0".equals(bizObject.get("settleflag").toString());
                    if (settleFlag) {
                        fundCollectionB.put("stwbSettleStatus", SettleApplyDetailStateEnum.NO_SETTLE.getValue());
                    } else {
                        fundCollectionB.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollectionB.getFundSettlestatus()));
                    }
                    fundCollectionB.put("accentity", bizObject.get("accentity"));
                    fundCollectionB.put("directionType", "1");
                    fundCollectionB.set("billDate", DateUtils.dateFormat(bizObject.get("vouchdate"), DateUtils.DATE_PATTERN));
                    fundCollectionB.set("currency", bizObject.get("currency"));
                    // 汇率类型
                    fundCollectionB.set("exchangeRateType", bizObject.get("exchangeRateType"));
                    // 本币汇率
                    fundCollectionB.set("exchRate", bizObject.get("exchRate"));
                    // 本币币种
                    fundCollectionB.set("natCurrency", bizObject.get("natCurrency"));
                    // 对方账户类型
                    fundCollectionB.set("eoppAcctType",
                            com.yonyoucloud.fi.cmp.util.StringUtils.isNotEmpty(fundCollectionB.getOppositeaccountno()) ?
                                    String.valueOf(OppAccTypeEnum.ENTERPRISE_BANK_ACC.getValue()) : null);
                    fundCollectionB.set("expectSettleDate", DateUtils.dateFormat(bizObject.get("vouchdate"), DateUtils.DATE_PATTERN));
                    fundCollectionB.set("sourceSystemId", "8");
                    // 对方单位
                    fundCollectionB.set("oppName", fundCollectionB.getOppositeobjectname());
                    //CM202400706 资金收付款单的轧差识别码字段落库存储时，若为空，默认赋值“对方档案ID“
                    boolean isnull = Objects.isNull(fundCollectionB.getNetIdentificateCode()) && !Objects.isNull(fundCollectionB.getOppositeobjectid());
                    boolean isInput = false;
                    if (!Objects.isNull(fund)) {
                        //当前轧差既不等于当前的对方档案id也不等于数据库已存的对方档案id那就是手输的
                        isInput = !Objects.isNull(fundCollectionB.getId())
                                && !Objects.isNull(dbData.get(fundCollectionB.getId().toString()).getNetIdentificateCode())
                                && !Objects.isNull(fundCollectionB.getNetIdentificateCode())
                                && Objects.nonNull(fundCollectionB.getOppositeobjectid())
                                && Objects.nonNull(dbData.get(fundCollectionB.getId().toString()).getOppositeobjectid())
                                && !fundCollectionB.getOppositeobjectid().equals(fundCollectionB.getNetIdentificateCode())
                                && !dbData.get(fundCollectionB.getId().toString()).getOppositeobjectid().equals(fundCollectionB.getNetIdentificateCode())
                                && dbData.get(fundCollectionB.getId().toString()).getOppositeobjectid().length() != fundCollectionB.getNetIdentificateCode().length();
                    }
                    if (enableSimplify && FundSettleStatus.SettlementSupplement.getValue() != fundCollectionB.getFundSettlestatus().getValue()
                            && (isnull || (EntityStatus.Update.equals(bizObject.getEntityStatus()) && !isInput)) && CaObject.Other.getValue() != fundCollectionB.getCaobject().getValue()) {
                        fundCollectionB.setNetIdentificateCode(fundCollectionB.getOppositeobjectid());
                    }
                    //双写行号
                    if (fundCollectionB.getLineno() != null) {
                        fundCollectionB.set("stwbRowNo", fundCollectionB.getLineno().toBigInteger().toString());
                    }
                    //轧差后金额
                    fundCollectionB.set("nettingAmount", fundCollectionB.getAfterNetAmt());
                    //轧差识别码
                    fundCollectionB.set("nettingCode", fundCollectionB.getNetIdentificateCode());
                    //轧差总笔数
                    fundCollectionB.set("nettingCount", fundCollectionB.getNetSettleCount());
                    if (Objects.nonNull(fundCollectionB.getAfterNetDir())) {
                        //轧差后收付方向
                        fundCollectionB.set("nettingDirection", fundCollectionB.getAfterNetDir().toString().equals("0") ? "1" : "2");
                    }
                    if (!Objects.isNull(fundCollectionB.getIncomeAndExpendBankAccount())) {
                        fundCollectionB.setActualSettleAccount(fundCollectionB.getIncomeAndExpendBankAccount());
                    }
                    TaxRateArchiveDto taxRateVO = new TaxRateArchiveDto();
                    if (!Objects.isNull(fundCollectionB.getTaxCategory()) && Objects.isNull(fundCollectionB.getTaxRate())) {
                        TaxRateQueryCondition params = new TaxRateQueryCondition();
                        params.setId(fundCollectionB.getTaxCategory());
                        //只查询状态为启用的数据
                        params.setEnables(Arrays.asList(1));
                        taxRateVO = iTaxRateArchIrisService.queryOneByParam(params);
                    } else if (Objects.isNull(fundCollectionB.getTaxCategory()) && !Objects.isNull(fundCollectionB.get("taxCategory_code"))) {
                        TaxRateQueryCondition params = new TaxRateQueryCondition();
                        params.setCode(fundCollectionB.get("taxCategory_code"));
                        //只查询状态为启用的数据
                        params.setEnables(Arrays.asList(1));
                        taxRateVO = iTaxRateArchIrisService.queryOneByParam(params);
                    }
                    if (!Objects.isNull(taxRateVO) && !Objects.isNull(taxRateVO.getId()) && Objects.isNull(fundCollectionB.getTaxRate())) {
                        fundCollectionB.setTaxCategory(taxRateVO.getId());
                        //税率=档案税率*档案计算系数
                        BigDecimal ntaxrate =
                                !Objects.isNull(taxRateVO.getCalculateCoefficient()) && taxRateVO.getCalculateCoefficient() == 1 ?
                                        taxRateVO.getNtaxrate().multiply(new BigDecimal(100)) : taxRateVO.getNtaxrate();
                        fundCollectionB.setTaxRate(ntaxrate);
                        //税额(付款不含税)=本币金额*税率
                        fundCollectionB.setTaxSum(BigDecimalUtils.safeMultiply(fundCollectionB.getNatSum().multiply(new BigDecimal(0.01)), ntaxrate));
                        //无税金额(付款含税) = 本币金额/(1+税率)
                        fundCollectionB.setUnTaxSum(BigDecimalUtils.safeDivide(fundCollectionB.getNatSum(), BigDecimalUtils.safeAdd(BigDecimal.ONE, BigDecimalUtils.safeMultiply(ntaxrate, new BigDecimal(0.01))), ICmpConstant.CONSTANT_TEN));
                        //税额(付款含税) = 本币金额-无税金额(付款含税)
                        fundCollectionB.setIncludeTaxSum(BigDecimalUtils.safeSubtract(fundCollectionB.getNatSum(), fundCollectionB.getUnTaxSum()));

                        fundCollectionB.setTaxSum(fundCollectionB.getTaxSum().setScale(8, RoundingMode.HALF_UP));
                        fundCollectionB.setUnTaxSum(fundCollectionB.getUnTaxSum().setScale(8, RoundingMode.HALF_UP));
                        fundCollectionB.setIncludeTaxSum(fundCollectionB.getIncludeTaxSum().setScale(8, RoundingMode.HALF_UP));
                    }
                }
            }
        }
    }

    /**
     * <h2>检查是否启用商业汇票模块</h2>
     *
     * @param accent :
     * @return com.alibaba.fastjson.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/12/22 14:47
     */
    @Override
    public CtmJSONObject isEnableBsdModule(String accent) throws Exception {
        List<Map<String, Object>> maps = QueryBaseDocUtils.queryOrgBpOrgConfVO(accent, ISystemCodeConstant.ORG_MODULE_DRFT);
        CtmJSONObject result = new CtmJSONObject();
        if (CollectionUtils.isEmpty(maps)) {
            result.put("isEnabled", false);
            return result;
        }
        Map<String, Object> objectMap = maps.get(0);
        if (!ValueUtils.isNotEmptyObj(objectMap)) {
            result.put("isEnabled", false);
            return result;
        }
        Date beginDate = (Date) objectMap.get("begindate");
        if (ValueUtils.isNotEmptyObj(beginDate)) {
            result.put("isEnabled", true);
            return result;
        }
        result.put("isEnabled", false);
        return result;
    }

    /**
     * <h2>OpenApi删除操作</h2>
     *
     * @param param :
     * @return com.alibaba.fastjson.CtmJSONObject
     * @author Sun GuoCai
     * @since 2021/8/26 13:52
     */
    @Override
    public CtmJSONObject deleteFundBillByIds(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("data");
        String billNum = param.getString(BILL_NUM);
        String ruleBillNum;
        String fullName;
        switch (billNum) {
            case FUND_PAYMENTLIST:
                fullName = FundPayment.ENTITY_NAME;
                ruleBillNum = FUND_PAYMENT;
                break;
            case FUND_COLLECTIONLIST:
                fullName = FundCollection.ENTITY_NAME;
                ruleBillNum = FUND_COLLECTION;
                break;
            case PAYAPPLICATIONBILLLIST:
                fullName = PayApplicationBill.ENTITY_NAME;
                ruleBillNum = PAYAPPLICATIONBILL;
                break;
            default:
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102152"), "error！The current bill type is illegal!");//@notranslate
        }
        List<String> messages = new ArrayList<>();
        int failedCount = 0;
        List<Object> ids = new ArrayList<>();
        Map<Long, BizObject> payBillMap = new HashMap<>(CONSTANT_EIGHT);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong(ID);
            ids.add(id);
        }

        QuerySchema querySchema = QuerySchema.create().addSelect("accentity,vouchdate,tradetype,code,billtype,id,verifystate,pubts");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name(ID).in(ids));
        querySchema.addCondition(queryConditionGroup);
        List<BizObject> bizObjectList = MetaDaoHelper.queryObject(fullName, querySchema, null);

        for (BizObject bizObject : bizObjectList) {
            payBillMap.put(Long.valueOf(bizObject.getId().toString()), bizObject);
        }
        List<Long> deleteIds = new ArrayList<>();
        List<BizObject> deleteBills = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            BizObject bizObject = payBillMap.get(row.getLong("id"));
            if (!ValueUtils.isNotEmptyObj(bizObject)) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180669", "单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                failedCount++;
                if (rows.size() == 1) {
                    return getJsonObject(rows, messages, failedCount);
                }
                continue;
            }

            if (ValueUtils.isNotEmptyObj(bizObject.get("verifystate"))) {
                short verifyState = Short.parseShort(bizObject.get("verifystate").toString());
                if (verifyState == VerifyState.SUBMITED.getValue()
                        || verifyState == VerifyState.COMPLETED.getValue()) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418066F", "单据id: [%s]，当前单据状态不允许删除") /* "单据id: [%s]，当前单据状态不允许删除" */, rows.getJSONObject(i).getLong("id")));
                    failedCount++;
                    if (rows.size() == 1) {
                        return getJsonObject(rows, messages, failedCount);
                    }
                    continue;
                }
                short billtype = Short.parseShort(bizObject.get("billtype").toString());
                if (EventType.Unified_Synergy.getValue() == billtype) {
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400550", "单据id: [%s]，事项类型为统收统支协同单不允许删除") /* "单据id: [%s]，事项类型为统收统支协同单不允许删除" */, rows.getJSONObject(i).getLong("id")));
                    failedCount++;
                    if (rows.size() == 1) {
                        return getJsonObject(rows, messages, failedCount);
                    }
                    continue;
                }
            }
            deleteIds.add(Long.parseLong(bizObject.getId().toString()));
            deleteBills.add(bizObject);
        }
        if (ValueUtils.isNotEmptyObj(deleteIds)) {
            if (PAYAPPLICATIONBILL.equals(ruleBillNum)) {
                MetaDaoHelper.batchDelete(fullName,
                        Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, deleteIds.toArray(new Long[0]))));
            } else if (FUND_PAYMENT.equals(ruleBillNum) || FUND_COLLECTION.equals(ruleBillNum)) {
                for (BizObject item : deleteBills) {
                    try {
                        BillDataDto bill = new BillDataDto();
                        bill.setBillnum(ruleBillNum);
                        bill.setData(item);
                        Map<String, Object> partParam = new HashMap<String, Object>();
                        bill.setPartParam(partParam);
                        RuleExecuteResult e = (new CommonOperator(OperationTypeEnum.DELETE)).execute(bill);
                        if (e.getMsgCode() != 1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100312"), e.getMessage());
                        }
                    } catch (Exception e) {
                        messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540054E", "单据id: [%s]，删除异常错误：") /* "单据id: [%s]，删除异常错误：" */ + e.getMessage(), item.getLong("id")));
                    }
                }
            }
        }
        return getJsonObject(rows, messages, failedCount);
    }

    @Override
    public CtmJSONObject querySettledDetail(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        String id = param.getString("id");
        List<String> keys = new ArrayList<>();
        if (FUND_COLLECTION.equals(param.getString("billnum"))) {
            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id);
            List<FundCollection_b> fundCollection_bs = fundCollection.FundCollection_b();
            keys = fundCollection_bs.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
        } else if (IBillNumConstant.FUND_PAYMENT.equals(param.getString("billnum"))) {
            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id);
            List<FundPayment_b> fundPaymentBs = fundPayment.FundPayment_b();
            keys = fundPaymentBs.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
        } else if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(param.getString("billnum"))) {
            keys.add(id);
        }
        CtmLockTool.executeInOneServiceExclusivelyBatchLock(keys, 120L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 加锁失败
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102153"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418066C", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            log.error(" 同步结算状态 lockKey :" + id);
            Boolean isEventMsgFlag = param.getBoolean("isEventMsgFlag");
            if (isEventMsgFlag == null) {
                isEventMsgFlag = false;
            }
            if (FUND_COLLECTION.equals(param.getString("billnum"))) {
                queryCollectionSettledDetail(id, isEventMsgFlag);
            } else if (IBillNumConstant.FUND_PAYMENT.equals(param.getString("billnum"))) {
                queryPaymentSettledDetail(id, isEventMsgFlag);
            } else if (IBillNumConstant.CMP_FOREIGNPAYMENT.equals(param.getString("billnum"))) {
                queryForeignPaymentSettledDetail(id);
            }
        });
        result.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180664", "查询成功") /* "查询成功" */);
        result.put("code", "200");
        return result;
    }

    @Override
    public CtmJSONObject checkCustomerAccount(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
        Object bankAccountId = param.get("bankAccountId");
        if (bankAccountId == null && param.get("customer") == null) {
            return result;
        }
        if (ValueUtils.isNotEmptyObj(bankAccountId)) {
            agentFinancialQryDTO.setId(Long.valueOf(bankAccountId.toString()));
        } else {
            Object currencyId = param.get("currency");
            Object customerId = param.get("customer");
            if (currencyId == null || customerId == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102153"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F15AC404580009", "请选择客户和币种"));
            }
            if (customerId != null) {
                agentFinancialQryDTO.setMerchantId(Long.valueOf(customerId.toString()));
                agentFinancialQryDTO.setStopStatus(Boolean.FALSE);
                agentFinancialQryDTO.setIfDefault(Boolean.TRUE);
                if (currencyId != null) {
                    agentFinancialQryDTO.setCurrency(currencyId.toString());
                }
            }
        }
        List<AgentFinancialDTO> bankAccounts = QueryBaseDocUtils.queryCustomerBankAccountByCondition(agentFinancialQryDTO);
        if (bankAccounts.size() > 0) {
            result.put("oppositeaccountno", bankAccounts.get(0).getBankAccount());// 银行账户号
            result.put("oppositeaccountid", bankAccounts.get(0).getId());//银行账户id
            result.put("oppositeaccountname", bankAccounts.get(0).getBankAccountName());// 银行账户名称
            // 查询开户行
            if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getOpenBank())) {
                BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenBank());
                if (depositBank != null) {
                    result.put("oppositebankaddr", depositBank.getName()); // 客户账户银行网点
                    result.put("oppositebankaddrid", depositBank.getId()); // 客户账户银行网点ID
                    result.put("oppositebanklineno", depositBank.getLinenumber()); // 开户行联行号
                    result.put("oppositebankTypeId", depositBank.getBank()); // 银行类别id
                    result.put("oppositebankType", depositBank.getBankName()); // 银行类别name
                    result.put("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                    result.put("receivebankaddr_address", depositBank.getAddress());//收款方开户行地址
                }
            }
        }
        return result;
    }

    @Override
    public CtmJSONObject checkEmployeeAccount(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        Map<String, Object> condition = new HashMap<>();
        Object bankAccountId = param.get("bankAccountId");
        if (ValueUtils.isNotEmptyObj(bankAccountId)) {
            condition.put("id", bankAccountId);
        } else {
            Object currencyId = param.get("currency");
            Object employeeId = param.get("employee");
            if (employeeId == null || currencyId == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102153"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F15A3004600000", "请选择员工和币种") /* "请选择员工和币种" */);
            }
            if (employeeId != null) {
                condition.put("staff_id", employeeId);
                condition.put("isdefault", 1);
                condition.put("dr", 0);
                if (currencyId != null) {
                    condition.put("currency", currencyId);
                }
            }
        }
        List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByCondition(condition);
        if (bankAccounts.size() > 0) {
            result.put("oppositeaccountid", bankAccounts.get(0).get("id"));
            result.put("oppositeaccountno", bankAccounts.get(0).get("account"));
            result.put("oppositeaccountname", bankAccounts.get(0).get("accountname"));
            if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).get("bankname"))) {
                BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).get("bankname").toString());
                if (depositBank != null) {
                    result.put("oppositebankaddr", depositBank.getName()); // 员工账户银行网点
                    result.put("oppositebankaddrid", depositBank.getId()); // 员工账户银行网点ID
                    result.put("oppositebanklineno", depositBank.getLinenumber()); // 开户行联行号
                    result.put("oppositebankType", depositBank.getBankName()); // 银行类别name
                    result.put("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                    result.put("oppositebankTypeId", depositBank.getBank()); // 银行类别id
                    result.put("receivebankaddr_address", depositBank.getAddress());//收款方开户行地址
                }
            }
        }

        return result;
    }

    @Override
    public CtmJSONObject checkSupplierAccount(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        Map<String, Object> condition = new HashMap<>();
        Object bankAccountId = param.get("bankAccountId");
        if (ValueUtils.isNotEmptyObj(bankAccountId)) {
            condition.put("id", bankAccountId);
        } else {
            Object currencyId = param.get("currency");
            Object supplierId = param.get("supplier");
            if (supplierId == null || currencyId == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102153"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F15B1E04580005", "请选择供应商和币种") /* "请选择供应商和币种" */);
            }
            condition.put("vendor", supplierId);
            condition.put("stopstatus", "0");
            condition.put("defaultbank", true);
            if (currencyId != null) {
                condition.put("currency", currencyId);
            }
        }
        List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition);
        if (bankAccounts.size() > 0) {
            result.put("oppositeaccountid", bankAccounts.get(0).getId());// 供应商银行账户id
            result.put("oppositeaccountno", bankAccounts.get(0).getAccount());// 供应商银行账号
            result.put("oppositeaccountname", bankAccounts.get(0).getAccountname());// 供应商银行账户名称
            // 查询开户行
            String openbankid = bankAccounts.get(0).getOpenaccountbank();
            if (ValueUtils.isNotEmptyObj(openbankid)) {
                BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(openbankid);
                if (depositBank != null) {
                    result.put("oppositebankaddr", depositBank.getName()); // 供应商账户银行网点
                    result.put("oppositebankaddrid", depositBank.getId()); // 供应商账户银行网点ID
                    result.put("oppositebanklineno", depositBank.getLinenumber()); // 开户行联行号
                    result.put("oppositebankTypeId", depositBank.getBank()); // 银行类别id
                    result.put("oppositebankType", depositBank.getBankName()); // 银行类别name
                    result.put("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                    result.put("receivebankaddr_address", depositBank.getAddress());//收款方开户行地址
                }
            }
        }
        return result;
    }

    @Override
    public Object queryBfundbusinobjData(CtmJSONObject param) throws Exception {
        CtmJSONObject fundBusinObjArchivesItem = new CtmJSONObject();
        TmspRequestParams tmspRequestParams = new TmspRequestParams();
        if (param.get("id") == null || param.get("currency") == null) {
            throw new CtmException( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F4301805300015", "请选择资金业务对象和币种") /* "请选择资金业务对象和币种" */);
        }
        //资金业务对象id
        String id = param.get("id").toString();
        //币种
        String currency = param.get("currency").toString();
        //授权会计主体
        if (null != param.get("authAccentity")) {
            String authAccentity = param.get("authAccentity").toString();
            tmspRequestParams.setAuthAccentity(authAccentity);
        }
        tmspRequestParams.setId(id);
        tmspRequestParams.setCurrency(currency);
        //获取账户信息对象
        List list = RemoteDubbo.get(ITmspRefRpcService.class, IDomainConstant.YONBIP_FI_CTMPUB).queryFundBusinObjArchives(tmspRequestParams);
        if (!list.isEmpty()) {
            //获取数据实体
            CtmJSONObject jsonObject = (CtmJSONObject) GsonHelper.FromJSon(JSON.toJSONString(list.get(0)), CtmJSONObject.class);
            if (jsonObject.get("fundBusinObjArchivesItemDTO") != null && ((List) jsonObject.get("fundBusinObjArchivesItemDTO")).size() > 0) {
                //获取账户实体
                fundBusinObjArchivesItem = (CtmJSONObject) GsonHelper.FromJSon(JSON.toJSONString(((List) jsonObject.get("fundBusinObjArchivesItemDTO")).get(0)), CtmJSONObject.class);
                //查询银行类别
                String bbankid = fundBusinObjArchivesItem.getString("bbankid");
                BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
                fundBusinObjArchivesItem.put("bbankid_name", bankVO.getName());
                fundBusinObjArchivesItem.put("fundbusinobjtypeid", jsonObject.get("fundbusinobjtypeid"));
                //查询银行网点
                String bopenaccountbankid = fundBusinObjArchivesItem.getString("bopenaccountbankid");
                BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
                fundBusinObjArchivesItem.put("bopenaccountbankid_name", bankdotVO.getName());
                fundBusinObjArchivesItem.put("receivePartySwift", bankdotVO.getSwiftCode()); // 收款方swift码
                fundBusinObjArchivesItem.put("receivebankaddr_address", bankdotVO.getAddress());//收款方开户行地址
            }
            return fundBusinObjArchivesItem;
        }
        return null;
    }


    /**
     * <h2>根据当前会计主体【accentity】和子表付款银行账户id【enterprisebankaccount】查询资金业务对象信息</h2>
     *
     * @param param :
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2022/11/11 12:37
     */
    @Override
    public CtmJSONObject reverseQueryFundBusinessObjectData(CtmJSONObject param) throws Exception {
        CtmJSONObject fundBusinObjArchivesItem = new CtmJSONObject();
        //资金业务对象id
        String id = param.get("id").toString();
        //付款银行账户的id
        String enterprisebankaccount = param.get("enterprisebankaccount").toString();
        String oppositeaccountid = param.get("oppositeaccountid").toString();

        BillContext context = new BillContext();
        context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
        context.setDomain("yonbip-fi-ctmtmsp");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id as oppositeobjectid, fundbusinobjtypename as oppositeobjectname, " +
                "fundbusinobjtypeid,fundBusinObjArchivesItem.benabled as benabled, " +
                "fundBusinObjArchivesItem.isdefaultaccount as isdefaultaccount, fundBusinObjArchivesItem.bbankid as bbankid, " +
                "fundBusinObjArchivesItem.bopenaccountbankid as bopenaccountbankid, fundBusinObjArchivesItem.id as id, " +
                "fundBusinObjArchivesItem.bankaccount as bankaccount,fundBusinObjArchivesItem.accountname as accountname ," +
                "fundBusinObjArchivesItem.linenumber as linenumber, fundBusinObjArchivesItem.bbankAccountId as bbankAccountId");

        schema.appendQueryCondition(QueryCondition.name("accentity").eq(id));
        schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.bbankAccountId").eq(enterprisebankaccount));
//        schema.appendQueryCondition(QueryCondition.name("accentitymanage").eq(id));
//        schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.id").eq(oppositeaccountid));

        log.info("getObjectContent, schema = {}", schema);
        List<Map<String, Object>> result = MetaDaoHelper.query(context, schema);

        //获取账户信息对象
        if (CollectionUtils.isNotEmpty(result)) {
            //获取数据实体
            CtmJSONObject jsonObject = new CtmJSONObject(result.get(0));
            //查询银行类别
            String bbankid = jsonObject.getString("bbankid");
            BankVO bankVO = baseRefRpcService.queryBankTypeById(bbankid);
            if (!ValueUtils.isNotEmptyObj(bankVO)) {
                return null;
            }
            fundBusinObjArchivesItem.put("bbankid_name", bankVO.getName());

            fundBusinObjArchivesItem.put("id", jsonObject.get("id"));
            fundBusinObjArchivesItem.put("bankaccount", jsonObject.get("bankaccount"));
            fundBusinObjArchivesItem.put("accountname", jsonObject.get("accountname"));
            fundBusinObjArchivesItem.put("linenumber", jsonObject.get("linenumber"));
            fundBusinObjArchivesItem.put("fundbusinobjtypeid", jsonObject.get("fundbusinobjtypeid"));
            fundBusinObjArchivesItem.put("oppositeobjectid", jsonObject.get("oppositeobjectid"));
            fundBusinObjArchivesItem.put("oppositeobjectname", jsonObject.get("oppositeobjectname"));
            fundBusinObjArchivesItem.put("bbankAccountId", jsonObject.get("bbankAccountId"));
            //查询银行网点
            String bopenaccountbankid = jsonObject.getString("bopenaccountbankid");
            BankdotVO bankdotVO = baseRefRpcService.queryBankdotVOByBanddotId(bopenaccountbankid);
            fundBusinObjArchivesItem.put("bopenaccountbankid_name", bankdotVO.getName());
            return fundBusinObjArchivesItem;
        }
        return null;
    }

    /**
     * <h2>OPenAPI资金收付款单详情查询接口</h2>
     *
     * @param id:   主表id
     * @param code: 单据编码
     * @return org.imeta.orm.base.BizObject
     * @author Sun GuoCai
     * @date 2022/5/9 17:42
     */
    @Override
    public String queryFundBillByIdOrCode(String billNum, Long id, String code,
                                          String fundBillSubPubtsBegin, String fundBillSubPubtsEnd, Short settleStatus) throws Exception {
        String fullName;
        String subFullName;
        String subObject;
        switch (billNum) {
            case FUND_PAYMENT:
                fullName = FundPayment.ENTITY_NAME;
                subObject = "FundPayment_b";
                subFullName = FundPayment_b.ENTITY_NAME;
                break;
            case FUND_COLLECTION:
                fullName = FundCollection.ENTITY_NAME;
                subObject = "FundCollection_b";
                subFullName = FundCollection_b.ENTITY_NAME;
                break;
            case PAYAPPLICATIONBILL:
                fullName = PayApplicationBill.ENTITY_NAME;
                subObject = "payApplicationBill_b";
                subFullName = PayApplicationBill_b.ENTITY_NAME;
                break;
            default:
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102154"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D9", "error！The current bill type is illegal!")
                        /* "error！The current bill type is illegal!" */);
        }

        QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
        if (billNum.startsWith("cmp_fund")) {
            querySchemaJ = QuerySchema.create().addSelect("*,accentity.name as accentity_name," +
                    "enterprisebankaccount.name as enterprisebankaccount_name, enterprisebankaccount.bankNumber.name " +
                    " as enterprisebankaccount_bankNumber_name");
        }
        if (ValueUtils.isNotEmptyObj(code)) {
            querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("code").eq(code)));
        }
        if (ValueUtils.isNotEmptyObj(id)) {
            querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("id").eq(id)));
        }
        List<BizObject> mapList = MetaDaoHelper.queryObject(fullName, querySchemaJ, null);

        if (CollectionUtils.isNotEmpty(mapList)) {
            BizObject bizObject = mapList.get(0);
            QuerySchema querySchemaSub = QuerySchema.create().addSelect("*");
            if (billNum.startsWith("cmp_fund")) {
                querySchemaSub = QuerySchema.create().addSelect("*,accentity.name as accentity_name, " +
                        "enterprisebankaccount.name as enterprisebankaccount_name, " +
                        "enterprisebankaccount.bankNumber.name as enterprisebankaccount_bankNumber_name," +
                        "quickType.name as quickType_name,quickType.code as quickType_code");
            }
            QueryConditionGroup condition = new QueryConditionGroup();
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").eq(String.valueOf(bizObject.getId().toString()))));
            if (ValueUtils.isNotEmptyObj(settleStatus)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("settlestatus").eq(settleStatus)));
            }
            if (ValueUtils.isNotEmptyObj(fundBillSubPubtsBegin) && ValueUtils.isNotEmptyObj(fundBillSubPubtsEnd)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("pubts").between(fundBillSubPubtsBegin, fundBillSubPubtsEnd)));
            } else if (ValueUtils.isNotEmptyObj(fundBillSubPubtsBegin)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("pubts").egt(fundBillSubPubtsBegin)));
            } else if (ValueUtils.isNotEmptyObj(fundBillSubPubtsEnd)) {
                condition.addCondition(QueryConditionGroup.and(QueryCondition.name("pubts").elt(fundBillSubPubtsEnd)));
            }
            querySchemaSub.addCondition(condition);
            List<BizObject> subList = MetaDaoHelper.queryObject(subFullName, querySchemaSub, null);
            bizObject.put(subObject, subList);
            return ResultMessage.data(bizObject);
        } else {
            CtmJSONObject jsonObject = new CtmJSONObject();
            if (ValueUtils.isNotEmptyObj(id)) {
                jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418066D", "单据不存在 id:") /* "单据不存在 id:" */
                        /* "单据不存在 id:" */ + id);
                jsonObject.put("id", id);
            }
            if (ValueUtils.isNotEmptyObj(code)) {
                jsonObject.put("code", code);
                jsonObject.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180670", "单据不存在 code:") /* "单据不存在 code:" */
                        /* "单据不存在 code:" */ + code);
            }
            jsonObject.put("fundPaymentSubPubtsBegin", fundBillSubPubtsBegin);
            jsonObject.put("fundPaymentSubPubtsEnd", fundBillSubPubtsEnd);
            return ResultMessage.data(999L, CtmJSONObject.toJSONString(jsonObject), new CtmJSONObject());
        }

    }

    /**
     * <h2>资金付款单结算成功协同生成资金收款单，通过单据转换规则</h2>
     *
     * @param fundPayment_b : BO实体
     * @author Sun GuoCai
     * @since 2022/10/18 10:16
     */
    @Override
    public CtmJSONObject fundPaymentBillCoordinatedGeneratorFundCollectionBill(FundPayment_b fundPayment_b) throws Exception {
        CtmJSONObject map = new CtmJSONObject();
        // 查询主子表数据
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, fundPayment_b.getMainid(), 2);
        return executeGenerateFundCollection(fundPayment_b, fundPayment, map);
    }

    private CtmJSONObject fundPaymentBillCoordinatedGeneratorFundCollectionBill(FundPayment_b fundPayment_b, FundPayment fundPayment) throws Exception {
        CtmJSONObject map = new CtmJSONObject();
        return executeGenerateFundCollection(fundPayment_b, fundPayment, map);
    }

    /**
     * 判断是否需要协同生单
     *
     * @param fundPayment
     * @return
     * @throws Exception
     */
    boolean isGenerateFundCollection(FundPayment fundPayment) throws Exception {
        // 查询现金参数是否为可生成收款单据
        boolean isGenerateFundCollection = false;
        // 如果交易类型为默认值，则是需要协同生单
        if (xt_tradetype.equals("NoSetTradetype")) {
            isGenerateFundCollection = true;
        } else {
            if (!xt_tradetype.contains(fundPayment.getTradetype())) {
                isGenerateFundCollection = false;
                return isGenerateFundCollection;
            } else {
                isGenerateFundCollection = true;
            }
        }

        //  如果事项类型不是资金收付，则不可以进行协同生单
        if (EventType.FundPayment.getValue() != fundPayment.getBilltype().getValue()) {
            isGenerateFundCollection = false;
        }
        //协同生单的现金参数配置同时也需要判断是否可以生单
        Map<String, Object> autoConfig = cmCommonService.queryAutoConfigByAccentity(fundPayment.getAccentity());
        if (autoConfig != null && autoConfig.get("isGenerateFundCollection") != null && (Boolean) autoConfig.get("isGenerateFundCollection") && isGenerateFundCollection) {
            isGenerateFundCollection = true;
        } else { //TODO 不要后面的逻辑覆盖前面的值
            isGenerateFundCollection = false;
        }
        return isGenerateFundCollection;
    }

    /**
     * 执行生单逻辑
     *
     * @param fundPayment_b
     * @param fundPayment
     * @param map
     * @return
     * @throws Exception
     */
    private CtmJSONObject executeGenerateFundCollection(FundPayment_b fundPayment_b, FundPayment fundPayment, CtmJSONObject map) throws Exception {
        if (fundPayment.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102155"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DA", "主表数据为空") /* "主表数据为空" */);
        }
        if (!ValueUtils.isNotEmptyObj(fundPayment_b.getSettlesuccessSum())) {
            map.put("message", "settleSuccessSum is null");
            return map;
        }
        boolean isGenerateFundCollection = isGenerateFundCollection(fundPayment);
        if (isGenerateFundCollection) {
            // 加锁
            Object id = fundPayment_b.getId();
            String lockKey = FUND_COLLECTION + "_" + id;
            YmsLock ymsLock = null;
            try {
                ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
                if (null == ymsLock) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102156"), MessageUtils.getMessage("P_YS_FI_CM_0001289877") /* "该数据正在处理，请稍后重试！" */);
                }
                // 判断单据状态 没有彻底结束的不生单
                if (Short.parseShort(fundPayment_b.get("settlestatus").toString()) == FundSettleStatus.SettleProssing.getValue() ||
                        (Short.parseShort(fundPayment_b.get("settlestatus").toString()) == FundSettleStatus.SettlementSupplement.getValue() &&
                                (Objects.isNull(fundPayment_b.get("settlesuccessSum")) || fundPayment_b.getBigDecimal("settlesuccessSum").compareTo(BigDecimal.ZERO) == 0) &&
                                Objects.isNull(fundPayment_b.get("settleSuccessTime")))) {
                    map.put("message",
                            "stwbSettleStatus is " + FundSettleStatus.find(Short.parseShort(fundPayment_b.get(
                                    "settlestatus").toString())).getName() + fundPayment_b.get("settlesuccessSum") + fundPayment_b.get("settleSuccessTime"));
                    return map;
                }
                QuerySchema schema1 = QuerySchema.create().addSelect("id");
                QueryConditionGroup queryConditionGroup1 = QueryConditionGroup.and(
                        QueryCondition.name("srcbillid").eq(id.toString())
                );
                schema1.appendQueryCondition(queryConditionGroup1);
                List<Map<String, Object>> list = MetaDaoHelper.query(FundCollection_b.ENTITY_NAME, schema1);
                if (!list.isEmpty()) {
                    map.put("message", "Generator bill over");
                    return map;
                }
                List<BizObject> bills = new ArrayList<>();
                if (fundPayment_b.getCaobject().getValue() == CaObject.CapBizObj.getValue()) {
                    // 查询资金业务对象类型档案
                    BillContext fundobjBillContext = new BillContext();
                    fundobjBillContext.setFullname("tmsp.fundbusinobjtype.FundBusinObjType");
                    fundobjBillContext.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
                    QueryConditionGroup fundobjCondition = QueryConditionGroup.and(QueryCondition.name("id").eq(fundPayment_b.getFundbusinobjtypeid()));
                    List<Map<String, Object>> fundobjType = MetaDaoHelper.queryAll(fundobjBillContext, "*", fundobjCondition, null);
                    // 判断资金业务对象类型是否为会计主体
                    if (fundobjType != null && !fundobjType.isEmpty() && fundobjType.get(0) != null && fundobjType.get(0).get("code") != null && fundobjType.get(0).get("code").equals("TBOT0007")) {
                        // 通过单据转换规则实现
                        // 根据资金付款子表的oppositeobjectid(资金业务对象档案的主表id)和 oppositeaccountid(资金业务对象档案子表的id)
                        // 查询会计主体【accentity】和付款银行账户的id【bbankAccountId】
                        BillContext context = new BillContext();
                        context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
                        context.setDomain(IDomainConstant.YONBIP_FI_CTMPUB);
                        QuerySchema schema = QuerySchema.create();
                        schema.addSelect("id, accentity, fundBusinObjArchivesItem.bbankAccountId as bbankAccountId");
                        schema.appendQueryCondition(QueryCondition.name("id").eq(fundPayment_b.getOppositeobjectid()));
                        schema.appendQueryCondition(QueryCondition.name("fundBusinObjArchivesItem.id").eq(fundPayment_b.getOppositeaccountid()));
                        List<Map<String, Object>> accentityList = MetaDaoHelper.query(context, schema);
                        if (!ValueUtils.isNotEmptyObj(fundPayment_b.get("enterprisebankaccount"))) {
                            map.put("message", "enterprisebankaccount is null");
                            return map;
                        }
                        // 账户相关信息的转换
                        CtmJSONObject jsonObject = accountMessageConvert(fundPayment_b, fundPayment);
                        if (!ValueUtils.isNotEmptyObj(jsonObject)) {
                            map.put("message", "accountMessageConvert fail");
                            return map;
                        }
                        if (CollectionUtils.isNotEmpty(accentityList)) {
                            fundPayment.put("accentity", accentityList.get(0).get("accentity"));
                            fundPayment_b.put("enterprisebankaccount", accentityList.get(0).get("bbankAccountId"));
                        } else {
                            map.put("message", "query accentity and enterprisebankaccount fail, Oppositeobjectid = "
                                    + fundPayment_b.getOppositeobjectid());
                            return map;
                        }
                    } else {
                        map.put("message", "fundobjType is null");
                        return map;
                    }
                } else if (CaObject.InnerUnit.getValue() == fundPayment_b.getCaobject().getValue()) {
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("accentity", fundPayment_b.get("oppositeobjectid"));
                    if (!ValueUtils.isNotEmptyObj(fundPayment_b.get("oppositeaccountid"))) {
                        map.put("message", "oppositeaccountid is null");
                        return map;
                    }
                    //内部单位时，需要判断结算方式是否为空
                    if (!ValueUtils.isNotEmptyObj(fundPayment_b.get("settlemode"))) {
                        map.put("message", "settlemode is null");
                        return map;
                    }
                    map1.put("enterprisebankaccount", fundPayment_b.get("oppositeaccountid"));
                    //FinOrgDTO finOrgDto = AccentityUtil.getFinOrgDTOByAccentityId(fundPayment.get("accentity"));
                    //协同生成资金收款单，查询的id就是资金组织
                    FundsOrgDTO fundsOrgDTO;
                    FundsOrgDTO fundsOrgDTOCacheValue = fundsOrgDTOCache.getIfPresent(fundPayment.get("accentity").toString());
                    if (fundsOrgDTOCacheValue != null) {
                        fundsOrgDTO = fundsOrgDTOCacheValue;
                    } else {
                        fundsOrgDTO = fundsOrgQueryService.getByIdWithFinOrg(fundPayment.get("accentity"));
                        fundsOrgDTOCache.put(fundPayment.get("accentity").toString(), fundsOrgDTO);
                    }
                    fundPayment_b.set("oppositeobjectid", fundsOrgDTO.getId());
                    fundPayment_b.set("oppositeobjectname", fundsOrgDTO.getName());
                    String enterprisebankaccount = fundPayment_b.getEnterprisebankaccount();
                    if (ValueUtils.isNotEmptyObj(enterprisebankaccount)) {
                        EnterpriseParams enterpriseParams = new EnterpriseParams();
                        enterpriseParams.setId(enterprisebankaccount);
                        List<EnterpriseBankAcctVO> bankAccounts;
                        List<EnterpriseBankAcctVO> bankAccountsCacheValue = bankAccountsCache.getIfPresent(enterprisebankaccount);
                        if (bankAccountsCacheValue != null) {
                            bankAccounts = bankAccountsCacheValue;
                        } else {
                            bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                            bankAccountsCache.put(enterprisebankaccount, bankAccounts);
                        }
                        if (!bankAccounts.isEmpty()) {
                            EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                            fundPayment_b.set("oppositeaccountid", enterpriseBankAcctVO.getId());
                            fundPayment_b.set("oppositeaccountname", enterpriseBankAcctVO.getAcctName());
                            fundPayment_b.set("oppositeaccountno", enterpriseBankAcctVO.getAccount());
                            fundPayment_b.set("oppositebankaddrid", enterpriseBankAcctVO.getBankNumber());
                            fundPayment_b.set("oppositebankaddr", enterpriseBankAcctVO.getBankNumberName());
                            fundPayment_b.set("oppositebanklineno", enterpriseBankAcctVO.getLineNumber());
                            fundPayment_b.set("oppositebankType", enterpriseBankAcctVO.getBankName());
                        }
                    } else {
                        fundPayment_b.set("oppositeaccountid", null);
                        fundPayment_b.set("oppositeaccountname", null);
                        fundPayment_b.set("oppositeaccountno", null);
                        fundPayment_b.set("oppositebankaddrid", null);
                        fundPayment_b.set("oppositebankaddr", null);
                        fundPayment_b.set("oppositebanklineno", null);
                        fundPayment_b.set("oppositebankType", null);
                    }
                    fundPayment.put("accentity", map1.get("accentity"));
                    fundPayment_b.put("enterprisebankaccount", map1.get("enterprisebankaccount"));
                } else {
                    map.put("message", "Caobject is subject");
                    return map;
                }
                SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                settleMethodQueryParam.setId(fundPayment_b.getSettlemode());
                settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
                settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                String settleMethodCacheKey = fundPayment_b.getSettlemode().toString() + CONSTANT_ONE.toString() + AppContext.getTenantId().toString();
                List<SettleMethodModel> dataList;
                List<SettleMethodModel> settleMethodCacheValue = settleMethodModelCache.getIfPresent(settleMethodCacheKey);
                if (settleMethodCacheValue != null) {
                    dataList = settleMethodCacheValue;
                } else {
                    dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                    settleMethodModelCache.put(settleMethodCacheKey, dataList);
                }
                if (CollectionUtils.isEmpty(dataList)) {
                    map.put("message", "settleMethod is null");
                    return map;
                }
                // 银行转账
                if (!(dataList.get(CONSTANT_ZERO).getServiceAttr().equals(CONSTANT_ZERO))) {
                    map.put("message", "ServiceAttr is not 0");
                    return map;
                }

                fundPayment_b.put("oriSum", fundPayment_b.getSettlesuccessSum());
                if (new BigDecimal(fundPayment.get("exchRate").toString()).compareTo(new BigDecimal("1")) == 0) {
                    fundPayment_b.put("natSum", fundPayment_b.getSettlesuccessSum());

                } else {
                    fundPayment_b.put("natSum", fundPayment_b.getOriSum().multiply(new BigDecimal(fundPayment.get("exchRate").toString())));
                }
                fundPayment.setFundPayment_b(Collections.singletonList(fundPayment_b));
                bills.add(fundPayment);
                List<BizObject> billDataDto;
                try {
                    PushAndPullModel pushAndPullModel = new PushAndPullModel();
                    pushAndPullModel.setCode("fundPaymentCooperateGenerateFundCollection");
                    pushAndPullModel.setQuerydb(true);
                    billDataDto = pushAndPullService.transformBillByMakeBillCodeAll(bills, pushAndPullModel);
                    if (CollectionUtils.isEmpty(billDataDto)) {
                        map.put("message", "bill convert is null");
                        return map;
                    }
                } catch (Exception e) {
                    map.put("message", "pushAndPull is fail");
                    return map;
                }
                for (BizObject bizObject : billDataDto) {
                    BillContext billContext = getBillContext();
                    try {
                        boolean isWfControlled = processService.bpmControl(billContext, bizObject);
                        bizObject.put("isWfControlled", isWfControlled);
                    } catch (Exception e) {
                        bizObject.put("isWfControlled", false);
                    }
                    CmpCommonUtil.billCodeHandler(bizObject, FundCollection.ENTITY_NAME, billContext.getBillnum());
                    setValueForTransType(bizObject);
                    long mainId = ymsOidGenerator.nextId();
                    bizObject.setId(mainId);
                    bizObject.setEntityStatus(EntityStatus.Insert);
                    List<Map<String, Object>> fundCollectionSubList = bizObject.get("FundCollection_b");
                    BigDecimal lineNo = new BigDecimal(10);
                    String natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(ACCENTITY));
                    BigDecimal exchRate = null;
                    short exchRateOps;
                    if (StringUtils.isNotEmpty(natCurrency) && bizObject.get(ICmpConstant.CURRENCY).equals(natCurrency)) {
                        exchRate = new BigDecimal(1);
                        exchRateOps = 1;
                    } else {
                        ExchangeRateTypeVO rateTypeVO = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateTypeById(bizObject.get("exchangeRateType"));
                        //用户自定义汇率，不进行汇率查询，直接使用资金付款单的汇率
                        if (rateTypeVO == null || CurrencyRateTypeCode.CustomCode.getValue().equals(rateTypeVO.getCode())) {
                            exchRate = fundPayment.getExchRate();
                            exchRateOps = 1;
                        } else {
                            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(bizObject.get(ICmpConstant.CURRENCY), natCurrency,
                                    bizObject.get("vouchdate"), bizObject.get("exchangeRateType"));
                            if (cmpExchangeRateVO == null) {
                                String oriCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(bizObject.get(ICmpConstant.CURRENCY)).getName();
                                String natCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(natCurrency).getName();
                                String exchangeRateTypeName = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateTypeById(bizObject.get("exchangeRateType")).getName();
                                throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                        exchangeRateTypeName, oriCurrencyName, natCurrencyName));
                            }
                            exchRate = cmpExchangeRateVO.getExchangeRate();
                            exchRateOps = cmpExchangeRateVO.getExchangeRateOps();
                        }
                    }
                    CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(natCurrency);
                    for (Map<String, Object> billb : fundCollectionSubList) {
                        long subId = ymsOidGenerator.nextId();
                        billb.put("id", subId);
                        billb.put("mainid", String.valueOf(mainId));
                        billb.put("lineno", lineNo);
                        billb.put("settleCurrency", bizObject.get(ICmpConstant.CURRENCY));
                        billb.put("natCurrency", natCurrency);//本币币种
                        billb.put("swapOutExchangeRateType", bizObject.get("exchangeRateType"));
                        billb.put("swapOutAmountEstimate", billb.get(ICmpConstant.ORISUM));
                        billb.put("swapOutExchangeRateEstimate", new BigDecimal("1"));
                        billb.put("exchRate", exchRate);
                        //billb.put("natSum", ((BigDecimal) billb.get("oriSum")).multiply(new BigDecimal(exchRate.toString())));
                        if (exchRateOps == 1) {
                            billb.put("natSum", ((BigDecimal) billb.get("oriSum")).multiply(new BigDecimal(exchRate.toString())));
                        } else {
                            billb.put("natSum", BigDecimalUtils.safeDivide((BigDecimal) billb.get("oriSum"), exchRate, currencyTenantDTO.getMoneydigit()));
                        }
                        if (currencyTenantDTO != null) {
                            billb.put("natSum", ((BigDecimal) billb.get("natSum")).setScale(currencyTenantDTO.getMoneydigit(), BigDecimal.ROUND_HALF_UP));
                        }
                        // 协同生单时，需要将认领单和对账单id清空
                        billb.put("bankReconciliationId", null);
                        billb.put("billClaimId", null);
                        billb.put("_status", EntityStatus.Insert);
                        lineNo = BigDecimalUtils.safeAdd(lineNo, (new BigDecimal(10)));
                    }
                    bizObject.set("entrytype", EntryType.Normal_Entry.getValue());
                    bizObject.set("settleflag", 1);
                    bizObject.set("natCurrency", natCurrency);//本币币种
                    bizObject.set("exchRate", exchRate);
                    bizObject.set("exchangeRateOps", exchRateOps);
                    if (exchRateOps == 1) {
                        bizObject.set("natSum", ((BigDecimal) bizObject.get("oriSum")).multiply(new BigDecimal(exchRate.toString())));
                    } else {
                        bizObject.set("natSum", BigDecimalUtils.safeDivide((BigDecimal) bizObject.get("oriSum"), exchRate, currencyTenantDTO.getMoneydigit()));
                    }
                    if (currencyTenantDTO != null) {
                        bizObject.put("natSum", ((BigDecimal) bizObject.get("natSum")).setScale(currencyTenantDTO.getMoneydigit(), BigDecimal.ROUND_HALF_UP));
                    }
                    RuleExecuteResult ruleExecuteResult;
                    Integer autoSubmit = ValueUtils.isNotEmptyObj(bizObject.get("autoSubmit"))
                            ? bizObject.getInteger("autoSubmit") : 0;
                    if (autoSubmit == 1) {
                        ruleExecuteResult = executeRule(bizObject);
                        map.put("ruleExecuteResult ", CtmJSONObject.toJSONString(ruleExecuteResult));
                        map.put("id", bizObject.get("id").toString());
                        map.put("code", bizObject.get("code").toString());
                        map.put("data", ruleExecuteResult.getData());
                        map.put("message", ruleExecuteResult.getMessage());
                    } else {
                        BillDataDto dataDto = new BillDataDto();
                        dataDto.setBillnum(FUND_COLLECTION);
                        dataDto.setData(CtmJSONObject.toJSONString(bizObject));
                        ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
                        List<BizObject> retList = (List<BizObject>) dataDto.getData();
                        BizObject retObj = retList.get(0);
                        map.put("id", retObj.get("id").toString());
                        map.put("code", retObj.get("code").toString());
                        map.put("ruleExecuteResult ", CtmJSONObject.toJSONString(ruleExecuteResult));
                    }
                    map.put("lineno", String.valueOf(BigDecimalUtils.safeSubtract(lineNo, new BigDecimal(10)).intValue()));
                    map.put(ICmpConstant.STATUS, "200");
                }
                return map;
            } catch (Exception e) {
                map.put("msgError", e.getMessage());
                map.put("code", 999);
                throw e;
                // 释放锁
            } finally {
                // 释放锁
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        return map;
    }

    private RuleExecuteResult executeRule(BizObject fundCollection) {
        try {
            BillContext billContext = CmpCommonUtil.getBillContextByFundCollection();
            boolean isWfControlled = processService.bpmControl(billContext, fundCollection);
            fundCollection.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
        } catch (Exception e) {
            fundCollection.put(ICmpConstant.IS_WFCONTROLLED, false);
        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_COLLECTION);
        dataDto.setData(CtmJSONObject.toJSONString(fundCollection));
        RuleExecuteResult result = cmCommonService.doSaveAndSubmitAction(dataDto);
        // 注意这里判断result是否正常结束的状态，1:代表保存并提交成功；999：代表保存失败；910：代表保存成功但提交失败
        if (1 != result.getMsgCode()) {
            if (999 == result.getMsgCode()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102157"), result.getMessage());
            } else {
                List<BizObject> retList = (List<BizObject>) dataDto.getData();
                BizObject retObj = retList.get(0);
                fundCollection.setId(retObj.getId());
                fundCollection.set("code", retObj.get("code"));
                return result;
            }
        } else {
            List<BizObject> retList = (List<BizObject>) dataDto.getData();
            BizObject retObj = retList.get(0);
            fundCollection.setId(retObj.getId());
            fundCollection.set("code", retObj.get("code"));
            return result;
        }
    }

    private static BillContext getBillContext() {
        BillContext billContext = new BillContext();
        billContext.setAction("save");
        billContext.setbMain(true);
        billContext.setBillnum("cmp_fundcollection");
        billContext.setBilltype("Voucher");
        billContext.setMddBoId("ctm-cmp.cmp_fundcollection");
        billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180668", "资金收款单") /* "资金收款单" */);
        billContext.setSupportBpm(true);
        billContext.setTenant(AppContext.getCurrentUser().getTenant());
        billContext.setFullname("cmp.fundcollection.FundCollection");
        billContext.setEntityCode("cmp_fundcollection");
        return billContext;
    }

    private CtmJSONObject accountMessageConvert(FundPayment_b fundPayment_b, FundPayment fundPayment) throws Exception {
        CtmJSONObject param = new CtmJSONObject();
        param.put("id", fundPayment.get("accentity"));
        param.put("enterprisebankaccount", fundPayment_b.get("enterprisebankaccount"));
        param.put("oppositeaccountid", fundPayment_b.get("oppositeaccountid"));
        CtmJSONObject jsonObject = reverseQueryFundBusinessObjectData(param);
        if (ValueUtils.isNotEmptyObj(jsonObject)) {
            fundPayment_b.put("oppositeobjectname", jsonObject.get("oppositeobjectname"));
            fundPayment_b.put("oppositeobjectid", jsonObject.get("oppositeobjectid"));
            fundPayment_b.put("oppositeaccountid", jsonObject.get("id"));
            fundPayment_b.put("oppositeaccountno", jsonObject.get("bankaccount"));
            fundPayment_b.put("oppositeaccountname", jsonObject.get("accountname"));
            fundPayment_b.put("oppositebankaddr", jsonObject.get("bopenaccountbankid_name"));
            fundPayment_b.put("oppositebanklineno", jsonObject.get("linenumber"));
            fundPayment_b.put("oppositebankType", jsonObject.get("bbankid_name"));
            return jsonObject;
        }
        return null;
    }

    private void setValueForTransType(BizObject bill) {
        String billTypeId = null;
        try {
            if (StringUtils.isNotEmpty(bill.getString("tradetype"))) {
                Map<String, Object> condition = new HashMap<>();
                condition.put("id", bill.getString("tradetype"));
                cmCommonService.getTransTypeByCondition(condition);
                List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
                if (!transTypes.isEmpty()) {
                    bill.set("tradetype", transTypes.get(0).get("id"));
                    bill.set("tradetype_name", transTypes.get(0).get("name"));
                    bill.set("tradetype_code", transTypes.get(0).get("code"));
                    return;
                }
            }
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(ICmpConstant.CM_CMP_FUND_COLLECTION));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102158"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180665", "查询资金收款单交易类型失败！请检查数据。") /* "查询资金收款单交易类型失败！请检查数据。" */);
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById(billTypeId, "1", null);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                bill.set("tradetype", tradetypeMap.get("id"));
                bill.set("tradetype_name", tradetypeMap.get("name"));
                bill.set("tradetype_code", tradetypeMap.get("code"));
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
        }
    }

    /**
     * 根据单据id查询当前单据结算状态
     *
     * @param id
     */
    public void queryCollectionSettledDetail(String id, boolean isEventMsgFlag) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id);
        List<FundCollection_b> fundCollection_bs = fundCollection.FundCollection_b();
        List<Long> transNumberList = new ArrayList<>();
        // CZFW-120140 需要过滤掉已经被委托拒绝的单子
        List<FundCollection_b> billbs = fundCollection_bs.stream().filter(fundBill ->
                (fundBill.getEntrustReject() != 1)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(billbs)) {
            return;
        }
        List<String> subIdList = new ArrayList<>();
        for (FundCollection_b billb : billbs) {
            if (ValueUtils.isNotEmptyObj(billb.getTransNumber())) {
                transNumberList.add(Long.valueOf(billb.getTransNumber()));
            }
            if (ValueUtils.isNotEmptyObj(billb.getId())) {
                subIdList.add(billb.getId().toString());
            }
        }
        List<FundCollection_b> fundCollectionBList = new ArrayList<>();
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            Map<String, DataSettledDetail> settledDetailMap = settleApplyPubQueryService.pullSettleApplyDetailState("cmp.fundcollection.FundCollection_b", subIdList.toArray(new String[0]));
            log.error("fundcollection enableSimplify settledDetailMap={}", CtmJSONObject.toJSONString(settledDetailMap));
            for (DataSettledDetail dataSettledDetail : settledDetailMap.values()) {
                List<FundCollection_b> fundCollectionBListSub = updateSettledInfoOfFundCollection(dataSettledDetail, billbs, false);
                fundCollectionBList.addAll(fundCollectionBListSub);
            }
        } else {
            if (CollectionUtils.isEmpty(transNumberList)) {
                return;
            }
            QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
            querySettledDetailModel.setWdataorigin(8); // 8-现金管理
            querySettledDetailModel.setTransNumberList(transNumberList);
            List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);
            log.error("fundcollection non-enableSimplify settledDetailMap={}", CtmJSONObject.toJSONString(dataSettledDetailList));
            for (DataSettledDetail dataSettledDetail : dataSettledDetailList) {
                List<FundCollection_b> fundCollectionBListSub = updateSettledInfoOfFundCollection(dataSettledDetail, billbs, false);
                fundCollectionBList.addAll(fundCollectionBListSub);
            }
        }
        if (!CollectionUtils.isEmpty(fundCollectionBList)) {
            EntityTool.setUpdateStatus(fundCollectionBList);
            if (enableSimplify) {//结算简强场景不更新结算成功金额
                for (FundCollection_b fundBill : fundCollectionBList) {
                    fundBill.remove(ICmpConstant.SUCCESS_SUM_FIELD);
                    fundBill.remove(ICmpConstant.REMAIN_AMOUNT_FIELD);
                    fundBill.remove(ICmpConstant.COL_TRANSIT_AMOUNT_FIELD);
                }
            }
            MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionBList);
        }
        fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id);
        fundCollection_bs = fundCollection.FundCollection_b();
        // 判断是否过账
        boolean bSendSimpleEvent = bSendSimpleEvent(fundCollection, fundCollection_bs);
        if (bSendSimpleEvent) {
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("classifier", null);
            billClue.put("srcBusiId", fundCollection.getId().toString());
            fundCollection.set("_entityName", FundCollection.ENTITY_NAME);
            cmpSendEventService.sendSimpleEvent(fundCollection, billClue);
        }

    }

    /**
     * 更新资金收款单结算信息
     *
     * @param dataSettledDetail
     * @param billbs
     */
    public List<FundCollection_b> updateSettledInfoOfFundCollection(DataSettledDetail dataSettledDetail, List<FundCollection_b> billbs, boolean isEventMsgFlag) throws Exception {
        // 查询主子表数据
        FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, dataSettledDetail.getBusinessBillId(), 2);
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        List<FundCollection_b> generateFundCollectionBillList = new ArrayList<>();
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> refundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partRefundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> releaseBillNotenoList = new ArrayList<>();
        HashMap<String, Date> succesTimeMap = new HashMap<>();
        //记录下老数据的结算成功时间，用于判断是否进行预算占用，已经结算成功过的不再占用预算
        for (FundCollection_b billb : billbs) {
            succesTimeMap.put(billb.getId().toString(), billb.getSettleSuccessTime());
        }
        for (FundCollection_b billb : billbs) {
            boolean isRefundItem = billb.getFundSettlestatus().getValue() == FundSettleStatus.Refund.getValue();
            if ((StringUtils.isNotEmpty(billb.getTransNumber()) && dataSettledDetail.getDataSettledId().compareTo(Long.valueOf(billb.getTransNumber())) == 0)
                    || (StringUtils.isNotEmpty(dataSettledDetail.getBusinessDetailsId()) && dataSettledDetail.getBusinessDetailsId().equals(billb.getId().toString()))) {
                // 设置子表结算成功的时间
                List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
                if (CollectionUtils.isNotEmpty(dataSettledDistributes)) {
                    Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                    try {
                        //结算止付的不设置结算成功日期
                        if (dataSettled.isPresent() && !String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
                            billb.setSettleSuccessTime(dataSettled.get().getSettleSuccBizTime());
                        }
                    } catch (Exception e) {
                        log.error("get fund payment bill child settle success time fail! id={}, settleSuccessTime={}, message={}"
                                , billb.getId(), dataSettled.get().getSettleSuccBizTime(), e.getMessage());
                    }
                }
                //收款单票证回写
                extractedFundSwbillno(dataSettledDetail, billb);
                Object isToPushCspl = billb.get(ICmpConstant.IS_TO_PUSH_CSPL);
                if (isEventMsgFlag) {
                    // 回传实际结算汇率类型
                    billb.setActualSettlementExchangeRateType(dataSettledDetail.getExchangeRateType());
                    // 回传实际结算金额
                    BigDecimal actualSettlementAmount = ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())
                            ? dataSettledDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
                    billb.setActualSettlementAmount(actualSettlementAmount.setScale(8, RoundingMode.HALF_UP));
                    // 回传实际结算汇率
                    billb.setActualSettlementExchangeRate(dataSettledDetail.getExchangePaymentRate());
                }
                if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult()) && 0 ==
                        billb.getOriSum().compareTo(new BigDecimal(dataSettledDetail.getSuccesssettlementAmount().toString()))) {// 全部成功
                    if (!enableSimplify) {
                        billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); // 结算成功金额
                    }
                    if (dataSettledDetail.getIsrefund() != null && dataSettledDetail.getIsrefund() == ISREFUND) {// 全部结算成功且单据为退票时，结算状态设置为退票
                        // 根据子表id查询流水关联信息，如果是直接关联的资金收付，则结算状态不能作变更
                        String bid = billb.getId().toString();
                        boolean isRelationFlow = isReletionFlow(bid);
                        BigDecimal settleErrorSum = billb.getSettleerrorSum() == null ? new BigDecimal(0) : billb.getSettleerrorSum();
                        BigDecimal refundAmt = ValueUtils.isNotEmptyObj(dataSettledDetail.getRefundAmt())
                                ? dataSettledDetail.getRefundAmt() : BigDecimal.ZERO;
                        BigDecimal successSum = billb.getOriSum().subtract(settleErrorSum).subtract(refundAmt);//结算成功金额
                        if (!enableSimplify) {
                            billb.setSettlesuccessSum(successSum);// 结算成功金额
                        }
                        if (!isRelationFlow) {
                            if (refundAmt.compareTo(billb.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                                billb.setFundSettlestatus(FundSettleStatus.Refund);
                            } else {
                                billb.setFundSettlestatus(FundSettleStatus.PartSuccess);
                            }
                            billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                        }
                        if (ValueUtils.isNotEmptyObj(dataSettledDetail.getRefundAmt())
                                && BigDecimal.ZERO.compareTo(dataSettledDetail.getRefundAmt()) < 0
                                && ValueUtils.isNotEmptyObj(isToPushCspl)
                                && 1 == Integer.parseInt(isToPushCspl.toString())
                                && isEventMsgFlag) {
                            billb.set("refundSum", dataSettledDetail.getRefundAmt());// 退票金额
                            if (dataSettledDetail.getRefundAmt().compareTo(billb.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                                refundReleaseFundBillForFundPlanProjectList.add(billb);
                                billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                            } else {
                                partRefundReleaseFundBillForFundPlanProjectList.add(billb);
                            }
                        }
                    } else {
                        // 如果当前单据结算状态不为已结算补单，将结算状态置为结算成功，否则不修改
                        boolean isSettlementSupplement = billb.getFundSettlestatus().getValue() == FundSettleStatus.SettlementSupplement.getValue();
                        boolean isRefund = billb.getFundSettlestatus().getValue() == FundSettleStatus.Refund.getValue();
                        if (!isSettlementSupplement && !isRefund) {
                            billb.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                            billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                        }
                        //无论何种情况都使用结算传递过来统一对账码时赋值
                        if (StringUtils.isNotEmpty(dataSettledDetail.getCheckIdentificationCode())) {
                            billb.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                        }
                        if (!enableSimplify) {
                            billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); // 结算成功金额
                        }
                    }
                    // 结算成功回调后，占用预算，实占执行
                    if (!isRefundItem && cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_COLLECTION)
                            && billb.getFundSettlestatus() != FundSettleStatus.Refund
                            && succesTimeMap.get(billb.getId().toString()) == null
                    ) {
                        budgetSuccessFundCollection(billb);
                    }

                } else if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {//全部失败
                    billb.setSettleerrorSum(dataSettledDetail.getStoppedamount()); //结算止付金额
                    billb.setSettleSuccessTime(null);//结算止付的不设置结算成功日期，
                    billb.setFundSettlestatus(FundSettleStatus.SettleFailed);
                    billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                    if (ValueUtils.isNotEmptyObj(isToPushCspl)
                            && 1 == Integer.parseInt(isToPushCspl.toString())
                            && isEventMsgFlag) {
                        if (billb.getOriSum().compareTo(billb.getSettleerrorSum()) == 0) {
                            billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                            releaseFundBillForFundPlanProjectList.add(billb);
                        } else {
                            partReleaseFundBillForFundPlanProjectList.add(billb);
                        }
                    }

                    // Sun GuoCai 2023/5/9 资金收款单类型为账户结息的收款明细，结算止付时，预提规则的上次结息结束日和预提单的关联结息单状态都得回退
                    fundCollectionUpdateWithholding(billb);

                    // Sun GuoCai 2023/8/15  资金收款单支付时，如果是票据结算，释放票据
                    long noteNo = ValueUtils.isNotEmptyObj(billb.getNoteno()) ? billb.getNoteno() : -1L;
                    BigDecimal oriSum = ValueUtils.isNotEmptyObj(billb.getOriSum()) ? billb.getOriSum() : BigDecimal.ZERO;
                    if (ValueUtils.isNotEmptyObj(noteNo) && oriSum.compareTo(BigDecimal.ZERO) > 0) {
                        // 添加需要释放票据的资金收款单据
                        releaseBillNotenoList.add(billb);
                    }
                    // 当前单据已生成凭证，则生成止付凭证
                    if (fundCollection.getSettleSuccessPost() == null || fundCollection.getSettleSuccessPost() == 0) {
                        this.generateVoucher(billb, FundCollection.ENTITY_NAME, true);
                    }
                } else if (String.valueOf(WSettlementResult.PartSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //部分成功
                    if (!enableSimplify) {
                        billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount());//结算成功金额
                    }
                    billb.setSettleerrorSum(dataSettledDetail.getStoppedamount()); //结算止付金额
                    List<DataSettledDistribute> dataSettledDistribute = dataSettledDetail.getDataSettledDistribute();
                    if (isEventMsgFlag) {
                        for (DataSettledDistribute settledDistribute : dataSettledDistribute) {
                            // 回传实际结算汇率类型
                            if (ValueUtils.isNotEmptyObj(settledDistribute.getExchangeRateType())) {
                                billb.setActualSettlementExchangeRateType(settledDistribute.getExchangeRateType());
                            }
                            // 回传实际结算金额
                            if (ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())) {
                                BigDecimal settlementAmount = ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())
                                        ? dataSettledDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
                                billb.setActualSettlementAmount(settlementAmount.setScale(8, RoundingMode.HALF_UP));
                            }
                            // 回传实际结算汇率
                            if (ValueUtils.isNotEmptyObj(settledDistribute.getExchangePaymentRate())) {
                                billb.setActualSettlementExchangeRate(settledDistribute.getExchangePaymentRate());
                            }
                        }
                    }
                    if (ValueUtils.isNotEmptyObj(dataSettledDetail.getStoppedamount())
                            && dataSettledDetail.getStoppedamount().compareTo(BigDecimal.ZERO) > 0
                            && ValueUtils.isNotEmptyObj(isToPushCspl)
                            && 1 == Integer.parseInt(isToPushCspl.toString())
                            && isEventMsgFlag) {
                        partReleaseFundBillForFundPlanProjectList.add(billb);
                    }
                    billb.setFundSettlestatus(FundSettleStatus.PartSuccess);
                    billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                    // 结算成功回调后，占用预算，实占执行
                    if (!isRefundItem && cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_COLLECTION)
                            && succesTimeMap.get(billb.getId().toString()) == null) {
                        budgetSuccessFundCollection(billb);
                    }
                    //无论何种情况都使用结算传递过来统一对账码时赋值
                    if (StringUtils.isNotEmpty(dataSettledDetail.getCheckIdentificationCode())) {
                        billb.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                    }
                    // 当前单据已生成凭证，则生成止付凭证
                    if (fundCollection.getSettleSuccessPost() == null || fundCollection.getSettleSuccessPost() == 0) {
                        this.generateVoucher(billb, FundCollection.ENTITY_NAME, true);
                    }
                }
                billb.setEntityStatus(EntityStatus.Update);//修改操作
                //通知过账
                generateFundCollectionBillList.add(billb);
            }
        }


        fundCollectionReleaseFundPlanProject(releaseFundBillForFundPlanProjectList,
                partReleaseFundBillForFundPlanProjectList,
                refundReleaseFundBillForFundPlanProjectList,
                partRefundReleaseFundBillForFundPlanProjectList);

        if (CollectionUtils.isNotEmpty(releaseBillNotenoList)) {
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, releaseBillNotenoList.get(0).get("mainid").toString(), 1);
            for (BizObject subBiz : releaseBillNotenoList) {
                deleteNoteList(noteMaps, 1, bizObject, subBiz);
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

        // 更新主表结算成功时间
        if (ValueUtils.isNotEmptyObj(billbs)) {
            try {
                updatePrimaryBillSettleSuccessTime(null, billbs, FundCollection.ENTITY_NAME);
            } catch (Exception e) {
                log.error("get fund collection bill primary settle success time fail! mainId={}, message={}"
                        , billbs.get(0).getMainid(), e.getMessage());
                throw new CtmException(e.getMessage());
            }
        }
        return generateFundCollectionBillList;
    }

    /**
     * 收款单票证回写
     *
     * @param dataSettledDetail
     * @param billb
     */
    private void extractedFundSwbillno(DataSettledDetail dataSettledDetail, FundCollection_b billb) {
        List<Map<String, Object>> noteMaps = new ArrayList<>();
        List<Map<String, Object>> noteMapsBill = new ArrayList<>();
        //获取结算方式的id
        try {
            //获取结算类型是票据结算的id集合
            List<Object> listResult = settlementService.listSettleMethodByService_attr(IStwbConstant.SERVICEATTR_DIRT);
            //判断结算方式：是否是票据业务
            List<Map<String, Object>> swtnumber = queryBillInformationByID(billb.getNoteno());
            String swbillNoId = null != swtnumber && swtnumber.size() > 0 && null != swtnumber.get(0) && null != swtnumber.get(0).get("id") ? String.valueOf(swtnumber.get(0).get("id")) : null;
            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, billb.getString(ICmpConstant.MAINID));
            if (null != listResult && listResult.contains(billb.getSettlemode()) && listResult.contains(dataSettledDetail.getExpectsettlemethodId()) && dataSettledDetail.getDraftBillNoRef() != null && !dataSettledDetail.getDraftBillNoRef().equals(swbillNoId)) {
                //此时需要取结算单的票证号还票证方向，但是票证号需要查询票证系统
                if (null == fundCollection) {
                    throw new NullPointerException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400552", "资金收款单在执行票证回写时没有查询到对应的主表数据！") /* "资金收款单在执行票证回写时没有查询到对应的主表数据！" */);
                }
                Boolean isEnabledBsd = null != fundCollection.getIsEnabledBsd() ? fundCollection.getIsEnabledBsd() : false;
                // 组装票据占用参数，释放票证
//                deleteNoteList(noteMaps, 1, fundCollection, billb);
//                if (ValueUtils.isNotEmptyObj(noteMaps)) {
//                    noteRelease(fundCollection, noteMaps);
//                }
                //获取票证号
                String swbillno = dataSettledDetail.getSwbillno();
                if (!StringUtils.isEmpty(swbillno)) {
                    //判断是否购买票据系统，则回写票证文本，通过证件号获取对象，存储其id
                    if (isEnabledBsd) {
                        List<Map<String, Object>> bills = queryBillInformationByID(Long.valueOf(dataSettledDetail.getDraftBillNoRef()));
                        //买了则回写到票证号中
                        String notenoId = null != bills.get(0).get("id") ? String.valueOf(bills.get(0).get("id")) : "";
                        billb.setNoteno(Long.valueOf(notenoId));
                        String billtype = null != bills.get(0).get("notetype") ? String.valueOf(bills.get(0).get("notetype")) : "";
                        billb.setNotetype(Long.valueOf(billtype));
                        //设置,拿到票据号查询票据类型、票据方向、billdirection
                        String billdirection = null != bills.get(0).get("billdirection") ? String.valueOf(bills.get(0).get("billdirection")) : "";
                        if (StringUtils.isNotEmpty(billdirection)) {
                            billb.set("noteDirection", Short.valueOf(billdirection));
                        }
                        BigDecimal notemoney = null != bills.get(0).get("notemoney") ? BigDecimal.valueOf(Double.valueOf(String.valueOf(bills.get(0).get("notemoney")))) : null;
                        billb.setNoteSum(notemoney);//待确认是否是票面金额
                        //票证占用
//                        deleteNoteList(noteMapsBill, 1, fundCollection, billb);
//                        ctmDrftEndorePaybillRpcService.settleUseBillNew(noteMapsBill);
                    } else {
                        //没买就是手动输入，写到票证文本
                        billb.setNotetextno(swbillno);
                        //设置,拿到票据号查询、票据方向、
                        if (StringUtils.isNotEmpty(dataSettledDetail.getReceiptDirection())) {
                            billb.set("noteDirection", Short.valueOf(dataSettledDetail.getReceiptDirection()));
                        }

                    }
                }
            }
            //若结算单修改了结算方式，即结算方式不是票证结算的时候，需要释放票据
            if (null != dataSettledDetail.getExpectsettlemethodId() && !listResult.contains(dataSettledDetail.getExpectsettlemethodId())) {
                // 组装票据占用参数，释放票证
//                deleteNoteList(noteMaps, 1, fundCollection, billb);
//                if (ValueUtils.isNotEmptyObj(noteMaps)) {
//                    noteRelease(fundCollection, noteMaps);
//                }
                //然后将票据相关字段置空
                billb.set("notetype", null);//票据类型
                billb.set("noteno", null);//票证号
                billb.set("notetextno", null);//票证文本号
                billb.set("noteDirection", null);//票据方向
                billb.set("noteSum", null);
            }
        } catch (Exception e) {
            log.error("结算单的票证回写状态异常，异常信息为：" + e.getMessage());
        }
    }

    private void fundCollectionReleaseFundPlanProject(
            List<BizObject> releaseFundBillForFundPlanProjectList,
            List<BizObject> partReleaseFundBillForFundPlanProjectList,
            List<BizObject> refundReleaseFundBillForFundPlanProjectList,
            List<BizObject> partRefundReleaseFundBillForFundPlanProjectList) throws Exception {
        // 金额止付时，释放资金计划项目
        if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, releaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, releaseFundBillForFundPlanProjectList, true, null, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, releaseFundBillForFundPlanProjectList, true, null, "pre");

        }
        if (CollectionUtils.isNotEmpty(partReleaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, partReleaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, partReleaseFundBillForFundPlanProjectList, true, null, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, partReleaseFundBillForFundPlanProjectList, true, null, "pre");
        }

        // 金额退票时，释放资金计划项目
        if (CollectionUtils.isNotEmpty(refundReleaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, refundReleaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, refundReleaseFundBillForFundPlanProjectList, null, true, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, refundReleaseFundBillForFundPlanProjectList, null, null, "pre");


        }
        if (CollectionUtils.isNotEmpty(partRefundReleaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, partRefundReleaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, partRefundReleaseFundBillForFundPlanProjectList, null, true, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_COLLECTION, bizObject, partRefundReleaseFundBillForFundPlanProjectList, null, null, "pre");

        }
    }

    private void fundCollectionUpdateWithholding(FundCollection_b billb) throws Exception {
        // ①更新结息账号预提规则的上次结息结束日。更新逻辑为上次结息结束日=本资金收款明细的上次结息结束日，没有则不更新。
        ArrayList<BizObject> list = new ArrayList<>();
        list.add(billb);
        updateWithholdingRuleSettingLastInterestSettlementDate(list, 1);
        // ②更新关联的预提单的关联结息单状态为未关联
        if (isInterestWithQuickType(billb.get("quickType"))) {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("fundcollectionsubid").eq(billb.getId()));
            querySchema.addCondition(queryConditionGroup);
            List<BizObject> fundCollectionSubWithholdingRelation = MetaDaoHelper.queryObject(FundCollectionSubWithholdingRelation.ENTITY_NAME, querySchema, null);
            // 是否有关联了预提单
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(fundCollectionSubWithholdingRelation)) {
                List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME, "*",
                        fundCollectionSubWithholdingRelation.stream().map(e -> Long.parseLong(e.get("withholdingid").toString())).toArray(Long[]::new));
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
    }

    private void fundPaymentUpdateWithholding(FundPayment_b billb) throws Exception {
        // ①更新结息账号预提规则的上次结息结束日。更新逻辑为上次结息结束日=本资金收款明细的上次结息结束日，没有则不更新。
        ArrayList<BizObject> list = new ArrayList<>();
        list.add(billb);
        updateWithholdingRuleSettingLastInterestSettlementDate(list, 1);
        // ②更新关联的预提单的关联结息单状态为未关联
        if (isInterestWithQuickType(billb.get("quickType"))) {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("fundpaymentsubid").eq(billb.getId()));
            querySchema.addCondition(queryConditionGroup);
            List<BizObject> fundPaymentSubWithholdingRelation = MetaDaoHelper.queryObject(FundPaymentSubWithholdingRelation.ENTITY_NAME, querySchema, null);
            // 是否有关联了预提单
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(fundPaymentSubWithholdingRelation)) {
                List<Map<String, Object>> accrualsWithholdingMap = MetaDaoHelper.queryByIds(AccrualsWithholding.ENTITY_NAME, "*",
                        fundPaymentSubWithholdingRelation.stream().map(e -> Long.parseLong(e.get("withholdingid").toString())).toArray(Long[]::new));
                List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>();
                for (Map<String, Object> map : accrualsWithholdingMap) {
                    AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                    accrualsWithholding.init(map);
                    // 更新结息单的【关联结息单】字段状态为未关联
                    accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
                    accrualsWithholdingList.add(accrualsWithholding);
                    accrualsWithholding.setSrcbillmainid(null);
                    accrualsWithholding.setSrcbillnum(null);
                    accrualsWithholding.setSrcbilltype(null);
                }
                EntityTool.setUpdateStatus(accrualsWithholdingList);
                MetaDaoHelper.update(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingList);
            }
        }
    }


    /**
     * 根据单据id查询当前单据结算状态
     *
     * @param id
     */
    public void queryPaymentSettledDetail(String id, boolean isEventMsgFlag) throws Exception {
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id);
        List<FundPayment_b> fundPayment_bs = fundPayment.FundPayment_b();
        List<Long> transNumberList = new ArrayList<>();
        List<String> subIdList = new ArrayList<>();
        // CZFW-120140 需要过滤掉已经被委托拒绝的单子
        List<FundPayment_b> billbs = fundPayment_bs.stream().filter(fundBill ->
                (fundBill.getEntrustReject() != 1)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(billbs)) {
            return;
        }
        for (FundPayment_b billb : billbs) {
            if (ValueUtils.isNotEmptyObj(billb.getTransNumber())) {
                transNumberList.add(Long.valueOf(billb.getTransNumber()));
            }
            if (ValueUtils.isNotEmptyObj(billb.getId())) {
                subIdList.add(billb.getId().toString());
            }
        }
        List<FundPayment_b> fundPaymentBList = new ArrayList<>();
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            Map<String, DataSettledDetail> settledDetailMap = settleApplyPubQueryService.pullSettleApplyDetailState("cmp.fundpayment.FundPayment_b", subIdList.toArray(new String[0]));
            log.error("fundpayment enableSimplify settledDetailMap={}", CtmJSONObject.toJSONString(settledDetailMap));
            for (DataSettledDetail dataSettledDetail : settledDetailMap.values()) {
                List<FundPayment_b> fundPaymentBListSub = updateSettledInfoOfFundPayment(dataSettledDetail, billbs, false);
                fundPaymentBList.addAll(fundPaymentBListSub);
            }
        } else {
            if (CollectionUtils.isEmpty(transNumberList)) {
                return;
            }
            QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
            querySettledDetailModel.setWdataorigin(8);
            querySettledDetailModel.setTransNumberList(transNumberList);
            List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);
            log.error("fundpayment non-enableSimplify settledDetailMap={}", CtmJSONObject.toJSONString(dataSettledDetailList));
            for (DataSettledDetail dataSettledDetail : dataSettledDetailList) {
                List<FundPayment_b> fundPaymentBListSub = updateSettledInfoOfFundPayment(dataSettledDetail, billbs, false);
                fundPaymentBList.addAll(fundPaymentBListSub);
            }
        }
        if (!CollectionUtils.isEmpty(fundPaymentBList)) {
            EntityTool.setUpdateStatus(fundPaymentBList);
            if (enableSimplify) {//结算简强场景不更新结算成功金额
                for (FundPayment_b fundBill : fundPaymentBList) {
                    fundBill.remove(ICmpConstant.SUCCESS_SUM_FIELD);
                    fundBill.remove(ICmpConstant.REMAIN_AMOUNT_FIELD);
                    fundBill.remove(ICmpConstant.PAY_TRANSIT_AMOUNT_FIELD);
                }
            }
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentBList);
        }
        //重新查询数据后再做过账判断
        fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id);
        fundPayment_bs = fundPayment.FundPayment_b();
        // 过账
        boolean bSendSimpleEvent = bSendSimpleEvent(fundPayment, fundPayment_bs);
        if (bSendSimpleEvent) {
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("classifier", null);
            billClue.put("srcBusiId", fundPayment.getId().toString());
            fundPayment.set("_entityName", FundPayment.ENTITY_NAME);
            cmpSendEventService.sendSimpleEvent(fundPayment, billClue);
        }

    }


    /**
     * 判断是否需要过账
     *
     * @param fundPayment
     * @param fundPayment_bs
     * @return
     */
    public boolean bSendSimpleEvent(FundPayment fundPayment, List<FundPayment_b> fundPayment_bs) {
        // 过账
        short voucherStatus = fundPayment.getVoucherstatus().getValue();
        Integer isSettleSuccessToPost = fundPayment.getInteger("settleSuccessPost");
        boolean auditPassPost = VoucherStatus.Empty.getValue() == voucherStatus && isSettleSuccessToPost == 0;
        boolean allMatch = fundPayment_bs.stream()
                .allMatch(t -> (ValueUtils.isNotEmptyObj(t.getSettlesuccessSum()) && ValueUtils.isNotEmptyObj(t.getSettleSuccessTime()))
                        || t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettleFailed.getValue());
        boolean settleSuccessPost = VoucherStatus.TO_BE_POST.getValue() == voucherStatus && isSettleSuccessToPost == 1 && allMatch;

        return auditPassPost || settleSuccessPost;
    }

    /**
     * 判断是否需要过账
     *
     * @param fundCollection
     * @param fundCollection_bs
     * @return
     */
    public boolean bSendSimpleEvent(FundCollection fundCollection, List<FundCollection_b> fundCollection_bs) {
        // 过账
        short voucherStatus = fundCollection.getVoucherstatus().getValue();
        Integer isSettleSuccessToPost = fundCollection.getInteger("settleSuccessPost");
        boolean auditPassPost = VoucherStatus.Empty.getValue() == voucherStatus && isSettleSuccessToPost == 0;
        boolean allMatch = fundCollection_bs.stream()
                .allMatch(t -> (ValueUtils.isNotEmptyObj(t.getSettlesuccessSum()) && ValueUtils.isNotEmptyObj(t.getSettleSuccessTime()))
                        || t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettleFailed.getValue());
        boolean settleSuccessPost = VoucherStatus.TO_BE_POST.getValue() == voucherStatus && isSettleSuccessToPost == 1 && allMatch;

        return auditPassPost || settleSuccessPost;
    }

    /**
     * 更新资金付款单结算信息
     *
     * @param dataSettledDetail
     * @param billbs
     * @throws Exception
     */
    public List<FundPayment_b> updateSettledInfoOfFundPayment(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs, boolean isEventMsgFlag) throws Exception {
        if (CollectionUtils.isEmpty(billbs)) {
            return new ArrayList<>();
        }
        // 查询主子表数据
        return getFundPaymentBs(dataSettledDetail, billbs, isEventMsgFlag);
    }

    /**
     * 资金付款单同步结算状态时的优化
     *
     * @param dataSettledDetail
     * @param billbs
     * @param isEventMsgFlag
     * @return
     * @throws Exception
     */
    private @NotNull List<FundPayment_b> getFundPaymentBs(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs, boolean isEventMsgFlag) throws Exception {
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, dataSettledDetail.getBusinessBillId(), 2);
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        List<FundPayment_b> generateFundPaymentBillList = new ArrayList<>();
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> refundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partRefundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> releaseBillNotenoList = new ArrayList<>();
        List<FundPayment_b> paymentBillCoordinatedGeneratorFundCollectionBillList = new ArrayList<>();
        HashMap<String, Date> succesTimeMap = new HashMap<>();
        //记录下老数据的结算成功时间，用于判断是否进行预算占用，已经结算成功过的不再占用预算
        for (FundPayment_b billb : billbs) {
            succesTimeMap.put(billb.getId().toString(), billb.getSettleSuccessTime());
        }
        for (FundPayment_b billb : billbs) {
            boolean isRefundItem = billb.getFundSettlestatus().getValue() == FundSettleStatus.Refund.getValue();
            boolean isStop = billb.getFundSettlestatus().getValue() == FundSettleStatus.SettleFailed.getValue();
            //已经到达退票或者止付的终态，不需要再更新了，直接跳过
            if ((isRefundItem || isStop) && Objects.nonNull((billb.get(ICmpConstant.SETTLE_SUCCESS_TIME)))) {
                continue;
            }
            if ((StringUtils.isNotEmpty(billb.getTransNumber()) && dataSettledDetail.getDataSettledId().compareTo(Long.valueOf(billb.getTransNumber())) == 0)
                    || (StringUtils.isNotEmpty(dataSettledDetail.getBusinessDetailsId()) && dataSettledDetail.getBusinessDetailsId().equals(billb.getId().toString()))) {
                Object isToPushCspl = billb.get(ICmpConstant.IS_TO_PUSH_CSPL);
                if (isEventMsgFlag) {
                    // 回传实际结算汇率类型
                    billb.setActualSettlementExchangeRateType(dataSettledDetail.getExchangeRateType());
                    // 回传实际结算金额
                    BigDecimal actualSettlementAmount = ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())
                            ? dataSettledDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
                    billb.setActualSettlementAmount(actualSettlementAmount.setScale(8, RoundingMode.HALF_UP));
                    // 回传实际结算汇率
                    billb.setActualSettlementExchangeRate(dataSettledDetail.getExchangePaymentRate());
                }
                //付款单票证回写
                extractedPaySwbillno(dataSettledDetail, billb);
                // 调整结算成功时间的赋值顺序，先于预算占用
                // 设置子表结算成功的时间，由获取结算时间改为获取结算日期
                List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();

                if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult()) || String.valueOf(WSettlementResult.PartSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
                    if (CollectionUtils.isNotEmpty(dataSettledDistributes)) {
                        Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                        try {
                            //结算止付的不设置结算成功日期
                            if (dataSettled.isPresent()) {
                                billb.setSettleSuccessTime(dataSettled.get().getSettleSuccBizTime());
                            }
                        } catch (Exception e) {
                            log.error("get fund payment bill child settle success time fail! id={}, settleSuccessTime={}, message={}"
                                    , billb.getId(), dataSettled.get().getSettleSuccBizTime(), e.getMessage());
                        }
                    }
                }
                if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult()) && 0 ==
                        billb.getOriSum().compareTo(new BigDecimal(dataSettledDetail.getSuccesssettlementAmount().toString()))) {// 全部成功
                    if (!enableSimplify) {
                        billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); //结算成功金额
                    }
                    // 根据子表id查询流水关联信息，如果是直接关联的资金收付，则结算状态不能作变更
                    String bid = billb.getId().toString();
                    boolean isRelationFlow = isReletionFlow(bid);
                    if (dataSettledDetail.getIsrefund() != null && dataSettledDetail.getIsrefund() == ISREFUND) {//全部结算成功且单据为退票时，结算状态设置为退票
                        BigDecimal settleErrorSum = billb.getSettleerrorSum() == null ? new BigDecimal(0) : billb.getSettleerrorSum();
                        BigDecimal refundAmt = dataSettledDetail.getRefundAmt();
                        BigDecimal refundAmtDB = ValueUtils.isNotEmptyObj(billb.getRefundSum()) ? billb.getRefundSum() : BigDecimal.ZERO;
                        billb.setRefundSum(refundAmt);//退票金额
                        if (!isRelationFlow) {
                            if (billb.getRefundSum().compareTo(billb.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                                billb.setFundSettlestatus(FundSettleStatus.Refund);
                            } else {
                                billb.setFundSettlestatus(FundSettleStatus.PartSuccess);
                            }
                            billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                        }
                        if (ValueUtils.isNotEmptyObj(dataSettledDetail.getRefundAmt())
                                && BigDecimal.ZERO.compareTo(dataSettledDetail.getRefundAmt()) < 0
                                && ValueUtils.isNotEmptyObj(isToPushCspl)
                                && 1 == Integer.parseInt(isToPushCspl.toString())
                                && isEventMsgFlag) {
                            if (refundAmtDB.compareTo(refundAmt) != 0) {
                                if (BigDecimal.ZERO.compareTo(refundAmtDB) == 0
                                        && refundAmt.compareTo(billb.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                                    refundReleaseFundBillForFundPlanProjectList.add(billb);
                                    billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                                } else {
                                    billb.set("partRefundSum", BigDecimalUtils.safeSubtract(refundAmt, refundAmtDB));
                                    partRefundReleaseFundBillForFundPlanProjectList.add(billb);
                                }
                            }
                        }

                    } else {
                        // 如果当前单据结算状态不为已结算补单和退票，将结算状态置为结算成功，否则不修改
                        boolean isSettlementSupplement = billb.getFundSettlestatus().getValue() == FundSettleStatus.SettlementSupplement.getValue();
                        boolean isRefund = billb.getFundSettlestatus().getValue() == FundSettleStatus.Refund.getValue();
                        if (!isSettlementSupplement && !isRefund) {
                            billb.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                            billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                        }
                        //无论何种情况都使用结算传递过来统一对账码时赋值
                        if (StringUtils.isNotEmpty(dataSettledDetail.getCheckIdentificationCode())) {
                            billb.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                        }
                        if (isRefund) {
                            BigDecimal refundAmt = ValueUtils.isNotEmptyObj(dataSettledDetail.getSuccesssettlementAmount())
                                    ? dataSettledDetail.getSuccesssettlementAmount() : BigDecimal.ZERO;
                            billb.setRefundSum(refundAmt);//退票金额
                        }
                        if (!enableSimplify) {
                            billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); //结算成功金额
                        }
                    }
                    // 结算成功回调后，占用预算，实占执行
                    if (!isRefundItem && cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT) && succesTimeMap.get(billb.getId().toString()) == null) {
                        budgetSuccessFundPayment(billb);
                    }
                } else if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {//全部失败
                    billb.setSettleerrorSum(dataSettledDetail.getStoppedamount()); //结算止付金额
                    //止付的时候，防止结算金额不为空的情况
                    if (billb.getSettlesuccessSum() != null && billb.getNatSum().equals(billb.getSettleerrorSum()) && !enableSimplify) {
                        billb.setSettlesuccessSum(null);
                    }
                    billb.setSettleSuccessTime(null);//结算止付的不设置结算成功日期
                    billb.setFundSettlestatus(FundSettleStatus.SettleFailed);
                    billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                    if (ValueUtils.isNotEmptyObj(isToPushCspl)
                            && 1 == Integer.parseInt(isToPushCspl.toString())
                            && isEventMsgFlag) {
                        BigDecimal refundSum = ValueUtils.isNotEmptyObj(billb.getRefundSum())
                                ? billb.getRefundSum() : BigDecimal.ZERO;
                        BigDecimal amount = BigDecimalUtils.safeAdd(refundSum, billb.getSettleerrorSum());
                        if (billb.getOriSum().compareTo(billb.getSettleerrorSum()) == 0
                                || billb.getOriSum().compareTo(amount) == 0) {
                            billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                            releaseFundBillForFundPlanProjectList.add(billb);
                        } else {
                            partReleaseFundBillForFundPlanProjectList.add(billb);
                        }
                    }

                    // Sun GuoCai 2023/5/9 资金付款单类型为账户结息的付款明细，结算止付时，预提规则的上次结息结束日和预提单的关联结息单状态都得回退
                    fundPaymentUpdateWithholding(billb);

                    // Sun GuoCai 2023/8/15  资金收款单支付时，如果是票据结算，释放票据
                    long noteNo = ValueUtils.isNotEmptyObj(billb.getNoteno()) ? billb.getNoteno() : -1L;
                    BigDecimal oriSum = ValueUtils.isNotEmptyObj(billb.getOriSum()) ? billb.getOriSum() : BigDecimal.ZERO;
                    if (ValueUtils.isNotEmptyObj(noteNo) && oriSum.compareTo(BigDecimal.ZERO) > 0) {
                        // 添加需要释放票据的资金收款单据
                        releaseBillNotenoList.add(billb);
                    }

                    // 结算失败回调后，占用预算，实占执行
                    if (!isRefundItem && cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                        budgetFail(billb);
                    }

                    // 当前单据已生成凭证，则生成止付凭证
                    if (fundPayment.getSettleSuccessPost() == null || fundPayment.getSettleSuccessPost() == 0) {
                        this.generateVoucher(billb, FundPayment.ENTITY_NAME, true);
                    }

                } else if (String.valueOf(WSettlementResult.PartSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //部分成功
                    billb.setSettleerrorSum(dataSettledDetail.getStoppedamount()); //结算止付金额
                    if (isEventMsgFlag) {
                        List<DataSettledDistribute> dataSettledDistribute = dataSettledDetail.getDataSettledDistribute();
                        BigDecimal settlementAmount = BigDecimal.ZERO;
                        for (DataSettledDistribute settledDistribute : dataSettledDistribute) {
                            // 回传实际结算汇率类型
                            if (ValueUtils.isNotEmptyObj(settledDistribute.getExchangeRateType())) {
                                billb.setActualSettlementExchangeRateType(settledDistribute.getExchangeRateType());
                            }
                            // 回传实际结算金额
                            if (ValueUtils.isNotEmptyObj(settledDistribute.getExchangePaymentAmount()) && SettleDetailSettleStateEnum.SETTLE_SUCCESS.getValue() == settledDistribute.getStatementdetailstatus()) {
                                settlementAmount = settlementAmount.add(settledDistribute.getExchangePaymentAmount());
                            }
                            // 回传实际结算汇率
                            if (ValueUtils.isNotEmptyObj(settledDistribute.getExchangePaymentRate())) {
                                billb.setActualSettlementExchangeRate(settledDistribute.getExchangePaymentRate());
                            }
                        }
                        billb.setActualSettlementAmount(settlementAmount.setScale(8, RoundingMode.HALF_UP));
                    }
                    if (dataSettledDetail.getIsrefund() != null && dataSettledDetail.getIsrefund() == ISREFUND) {//单据明细为退票时，记录退票金额，同时更新结算成功金额
                        BigDecimal settleErrorSum = billb.getSettleerrorSum() == null ? new BigDecimal(0) : billb.getSettleerrorSum();
                        BigDecimal refundAmt = dataSettledDetail.getRefundAmt();
                        BigDecimal successSum = billb.getOriSum().subtract(settleErrorSum).subtract(refundAmt);//结算成功金额
                        if (!enableSimplify) {
                            billb.setSettlesuccessSum(successSum);//结算成功金额
                        }
                        billb.setRefundSum(refundAmt);//退票金额
                    } else {
                        if (!enableSimplify) {
                            billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount());//结算成功金额
                        }
                    }
                    BigDecimal refundSum = ValueUtils.isNotEmptyObj(billb.getRefundSum())
                            ? billb.getRefundSum() : BigDecimal.ZERO;
                    if (ValueUtils.isNotEmptyObj(refundSum)
                            && refundSum.compareTo(BigDecimal.ZERO) > 0
                            && ValueUtils.isNotEmptyObj(billb.getSettleerrorSum())
                            && ValueUtils.isNotEmptyObj(billb.getFundPlanProject())) {
                        partRefundReleaseFundBillForFundPlanProjectList.add(billb);
                    }
                    if (ValueUtils.isNotEmptyObj(refundSum)
                            && refundSum.compareTo(BigDecimal.ZERO) > 0
                            && !ValueUtils.isNotEmptyObj(billb.getSettleerrorSum())
                            && ValueUtils.isNotEmptyObj(billb.getFundPlanProject())) {
                        billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                        refundReleaseFundBillForFundPlanProjectList.add(billb);
                    }
                    BigDecimal amount = BigDecimalUtils.safeAdd(refundSum, billb.getSettleerrorSum());
                    if (ValueUtils.isNotEmptyObj(dataSettledDetail.getStoppedamount())
                            && dataSettledDetail.getStoppedamount().compareTo(BigDecimal.ZERO) > 0
                            && ValueUtils.isNotEmptyObj(billb.get(ICmpConstant.FUND_PLAN_PROJECT))
                            && isEventMsgFlag) {
                        if (billb.getOriSum().compareTo(amount) == 0) {
                            billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                            releaseFundBillForFundPlanProjectList.add(billb);
                        } else {
                            partReleaseFundBillForFundPlanProjectList.add(billb);
                        }
                    }
                    //无论何种情况都使用结算传递过来统一对账码时赋值
                    if (StringUtils.isNotEmpty(dataSettledDetail.getCheckIdentificationCode())) {
                        billb.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
                    }
                    billb.setFundSettlestatus(FundSettleStatus.PartSuccess);
                    billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                    // 结算成功回调后，占用预算，实占执行
                    if (!isRefundItem && cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT) && succesTimeMap.get(billb.getId().toString()) == null) {
                        budgetSuccessFundPayment(billb);
                    }
                    // 当前单据已生成凭证，则生成止付凭证
                    if (fundPayment.getSettleSuccessPost() == null || fundPayment.getSettleSuccessPost() == 0) {
                        this.generateVoucher(billb, FundPayment.ENTITY_NAME, true);
                    }
                }
                billb.setEntityStatus(EntityStatus.Update);//修改操作
                //结算成功时过账
                generateFundPaymentBillList.add(billb);
                if (billb.getFundSettlestatus().getValue() == FundSettleStatus.SettleFailed.getValue()) {
                    continue;
                }
                // 协同生单单据
                paymentBillCoordinatedGeneratorFundCollectionBillList.add(billb);
            }
        }

        fundPaymentReleaseFundPlanProject(
                releaseFundBillForFundPlanProjectList,
                partReleaseFundBillForFundPlanProjectList,
                refundReleaseFundBillForFundPlanProjectList,
                partRefundReleaseFundBillForFundPlanProjectList);

        if (CollectionUtils.isNotEmpty(releaseBillNotenoList)) {
            List<Map<String, Object>> noteMaps = new ArrayList<>();
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, releaseBillNotenoList.get(0).get("mainid").toString(), 1);
            for (BizObject subBiz : releaseBillNotenoList) {
                deleteNoteList(noteMaps, 2, bizObject, subBiz);
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

        if (CollectionUtils.isNotEmpty(paymentBillCoordinatedGeneratorFundCollectionBillList)) {
            for (FundPayment_b billb : paymentBillCoordinatedGeneratorFundCollectionBillList) {
                FundPayment_b fundPaymentSub = (FundPayment_b) billb.clone();
                // 资金付款单协同生成资金收款单
                //CtmJSONObject result = fundPaymentBillCoordinatedGeneratorFundCollectionBill(fundPaymentSub);
                CtmJSONObject result = fundPaymentBillCoordinatedGeneratorFundCollectionBill(fundPaymentSub, (FundPayment) fundPayment.clone());
                log.error("generator Fund Collection Bill process done, result = {}", CtmJSONObject.toJSONString(result));
                // 1030需求 协同生成收款单，并且是新增时回写资金付款单明细
                if (ValueUtils.isNotEmptyObj(result)
                        && ValueUtils.isNotEmptyObj(result.getString("status"))
                        && "200".equals(result.getString("status"))) {
                    for (FundPayment_b fundPaymentB : billbs) {
                        if (fundPaymentB.getId().toString().equals(fundPaymentSub.getId().toString())) {
                            fundPaymentB.setSynergybillid(result.getString("id"));
                            fundPaymentB.setSynergybillno(result.getString("code"));
                            fundPaymentB.setSynergybillitemno(result.getString("lineno"));
                            fundPaymentB.setIssynergy(true);
                        }
                        log.error("paymentBillCoordinatedGeneratorFundCollectionBillList, fundPaymentB={}", CtmJSONObject.toJSONString(fundPaymentB));
                    }
                }
                if (ValueUtils.isNotEmptyObj(result)) {
                    BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.FUNDCOLLECTION,
                            FundCollection.ENTITY_NAME, OperCodeTypes.publish,
                            IMsgConstant.FUND_COLLECTION, IMsgConstant.COOPERATE_FUND_COLLECTION, result);
                    IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
                    businessLogService.saveBusinessLog(businessObject);
                }
            }
        }
        log.error("paymentBillCoordinatedGeneratorFundCollectionBillList, billbs={}", CtmJSONObject.toJSONString(billbs));
        // 更新主表结算成功时间
        if (ValueUtils.isNotEmptyObj(billbs)) {
            try {
                updatePrimaryBillSettleSuccessTime(billbs, null, FundPayment.ENTITY_NAME);
            } catch (Exception e) {
                log.error("get fund payment bill primary settle success time fail! mainId={}, message={}"
                        , billbs.get(0).getMainid(), e.getMessage());
                throw e;
            }
        }
        return generateFundPaymentBillList;
    }

    /**
     * 更新票据回写
     *
     * @param dataSettledDetail
     * @param billb
     */
    private void extractedPaySwbillno(DataSettledDetail dataSettledDetail, FundPayment_b billb) {
        //区分结算单返回对应的子表的数据
        //获取结算方式的id
        List<Map<String, Object>> noteMaps = new ArrayList<>();
        List<Map<String, Object>> noteMapsBill = new ArrayList<>();
        try {
            //获取结算类型是票据结算的id集合
            List<Object> listResult = settlementService.listSettleMethodByService_attr(IStwbConstant.SERVICEATTR_DIRT);
            //判断结算方式：是否是票据业务settlemode
            List<Map<String, Object>> swtnumber = queryBillInformationByID(billb.getNoteno());
            String swbillNoId = null != swtnumber && swtnumber.size() > 0 && null != swtnumber.get(0) && null != swtnumber.get(0).get("id") ? String.valueOf(swtnumber.get(0).get("id")) : null;
            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, billb.getString(ICmpConstant.MAINID));
            if (null != listResult && listResult.contains(billb.getSettlemode()) && listResult.contains(dataSettledDetail.getExpectsettlemethodId()) && null != dataSettledDetail.getDraftBillNoRef() && !dataSettledDetail.getDraftBillNoRef().equals(swbillNoId)) {
                //此时需要取结算单的票证号还票证方向，但是票证号需要查询票证系统
                if (null == fundPayment) {
                    throw new NullPointerException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540054F", "资金付款单在执行票证回写时没有查询到对应的主表数据！") /* "资金付款单在执行票证回写时没有查询到对应的主表数据！" */);
                }
                Boolean isEnabledBsd = null != fundPayment.getIsEnabledBsd() ? fundPayment.getIsEnabledBsd() : false;
                //获取票证号
                // 组装票据占用参数，释放票证
//                deleteNoteList(noteMaps, 2, fundPayment, billb);
//                if (ValueUtils.isNotEmptyObj(noteMaps)) {
//                    noteRelease(fundPayment, noteMaps);
//                }
                //获取票证号
                String swbillno = dataSettledDetail.getSwbillno();
                if (!StringUtils.isEmpty(swbillno)) {
                    //判断是否购买票据系统，则回写票证文本，通过证件号获取对象，存储其id
                    if (isEnabledBsd) {
                        //获取票证对象
                        List<Map<String, Object>> bills = queryBillInformationByID(Long.valueOf(dataSettledDetail.getDraftBillNoRef()));
                        //买了则回写到票证号中
                        String notenoId = null != bills.get(0).get("id") ? String.valueOf(bills.get(0).get("id")) : "";
                        billb.setNoteno(Long.valueOf(notenoId));
                        String notetype = null != bills.get(0).get("notetype") ? String.valueOf(bills.get(0).get("notetype")) : "";
                        billb.setNotetype(Long.valueOf(notetype));
                        //设置,拿到票据号查询票据类型、票据方向、billdirection
                        String billdirection = null != bills.get(0).get("billdirection") ? String.valueOf(bills.get(0).get("billdirection")) : "";
                        if (StringUtils.isNotEmpty(billdirection)) {
                            billb.set("noteDirection", Short.valueOf(billdirection));
                        }
                        BigDecimal notemoney = null != bills.get(0).get("notemoney") ? BigDecimal.valueOf(Double.valueOf(String.valueOf(bills.get(0).get("notemoney")))) : null;
                        billb.setNoteSum(notemoney);//待确认是否是票面金额
                        //票证占用
//                        deleteNoteList(noteMapsBill, 2, fundPayment, billb);
//                        ctmDrftEndorePaybillRpcService.settleUseBillNew(noteMapsBill);
                    } else {
                        //没买就是手动输入，写到票证文本
                        billb.setNotetextno(swbillno);
                        //设置,拿到票据号查询票据类型、票据方向、
                        if (StringUtils.isNotEmpty(dataSettledDetail.getReceiptDirection())) {
                            billb.set("noteDirection", Short.valueOf(dataSettledDetail.getReceiptDirection()));
                        }
                    }
                }
            }
            //若结算单修改了结算方式，即结算方式不是票证结算的时候，需要释放票据相关信息
            if (null != dataSettledDetail.getExpectsettlemethodId() && !listResult.contains(dataSettledDetail.getExpectsettlemethodId())) {
                // 组装票据占用参数，释放票证
//                deleteNoteList(noteMaps, 2, fundPayment, billb);
//                if (ValueUtils.isNotEmptyObj(noteMaps)) {
//                    noteRelease(fundPayment, noteMaps);
//                }
                //然后将票据相关字段置空
                billb.set("notetype", null);//票据类型
                billb.set("noteno", null);//票证号
                billb.set("notetextno", null);//票证文本号
                billb.set("noteDirection", null);//票据方向
                billb.set("noteSum", null);
            }
        } catch (Exception e) {
            log.error("结算单的票证回写状态异常，异常信息为：" + e.getMessage());
        }
    }

    /**
     * 根据子表明细判断当前资金收付款单明细行是否是直接关联的流水
     *
     * @param bid
     * @return
     * @throws Exception
     */
    private boolean isReletionFlow(String bid) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("billid").eq(bid));
        querySchema.addCondition(group);
        List<BankReconciliationbusrelation_b> listb = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(listb)) {
            if (listb.get(0).getBilltype() == EventType.FundCollection.getValue() || listb.get(0).getBilltype() == EventType.FundPayment.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据id查询票据
     *
     * @param id
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> queryBillInformationByID(Long id) throws Exception {
        //查询票据类型
        List<Map<String, Object>> nodes = new ArrayList<>();
        //查询票据信息
        BillContext contextNote = new BillContext();
        contextNote.setFullname("drft.drftnoteinformation.DrftNoteInformation");
        contextNote.setDomain("drft");
        QuerySchema schemaNote = QuerySchema.create();
        schemaNote.addSelect("*");
        QueryConditionGroup groupNote = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
        schemaNote.appendQueryCondition(groupNote);
        try {
            List<Map<String, Object>> nodeList = MetaDaoHelper.query(contextNote, schemaNote);
            nodes.addAll(nodeList);
        } catch (Exception e) {
            log.error("查询票证异常：" + e.getMessage());
        }
        return nodes;
    }

    /**
     * 根据单据id查询当前单据结算状态
     *
     * @param id
     */
    public void queryForeignPaymentSettledDetail(String id) throws Exception {
        ForeignPayment foreignPayment = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, id);
        List<Long> transNumberList = new ArrayList<>();
        if (ObjectUtils.isEmpty(foreignPayment)) {
            return;
        }
        if (ValueUtils.isNotEmptyObj(foreignPayment.getTransNumber())) {
            transNumberList.add(Long.valueOf(foreignPayment.getTransNumber()));
        }
        if (CollectionUtils.isEmpty(transNumberList)) {
            return;
        }
        QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
        querySettledDetailModel.setWdataorigin(8);
        querySettledDetailModel.setTransNumberList(transNumberList);
        List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);
        for (DataSettledDetail dataSettledDetail : dataSettledDetailList) {
            //待结算的数据不需要查询支付状态
            if (dataSettledDetail.getWsettleStatus().equals(String.valueOf(WSettleStatus.WaitSettle.getValue()))) {
                continue;
            }
            updateSettledInfoOfForeignPayment(dataSettledDetail, foreignPayment);
        }
    }

    /**
     * 更新外汇付款单结算信息
     *
     * @param dataSettledDetail
     * @param foreignPayment
     */
    public ForeignPayment updateSettledInfoOfForeignPayment(DataSettledDetail dataSettledDetail, ForeignPayment foreignPayment) throws Exception {
        // 回传实际结算汇率类型
        foreignPayment.setSettleExchangeRateType(dataSettledDetail.getExchangeRateType());
        // 回传实际结算金额
        BigDecimal actualSettlementAmount = ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())
                ? dataSettledDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
        BigDecimal actualSettlementAmount_new = actualSettlementAmount.setScale(8, BigDecimal.ROUND_HALF_UP);
//        actualSettlementAmount.stripTrailingZeros()
        foreignPayment.setSettleAmount(actualSettlementAmount_new);
        // 回传实际结算汇率
        // todo  日常环境放开
        foreignPayment.setSettleExchangeRate(dataSettledDetail.getExchangePaymentRate());
        // TODO: 等结算适配新汇率类型后，看看需不需要赋值结算的汇率折算方式给实际结算汇率折算方式

        if (dataSettledDetail.getBusinessBillId().equals(foreignPayment.getId().toString())) {
            // 设置结算成功的时间
            List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
            if (CollectionUtils.isNotEmpty(dataSettledDistributes)) {
                //结算单拆分后，获取最大的时间进行业务单据结算成功时间赋值，由获取结算时间改为获取结算日期
                Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                try {
                    if (dataSettled.isPresent()) {
                        foreignPayment.setSettlesuccesstime(dataSettled.get().getSettleSuccBizTime());
                    }
                } catch (Exception e) {
                    log.error("get fund payment bill child settle success time fail! id={}, settleSuccessTime={}, message={}"
                            , foreignPayment.getId(), dataSettled.get().getSettleSuccBizTime(), e.getMessage());
                }
            }
            //更新结算金额
            if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult()) && 0 ==
                    foreignPayment.getAmount().compareTo(new BigDecimal(dataSettledDetail.getSuccesssettlementAmount().toString()))) {// 全部成功

                foreignPayment.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); //结算成功金额
                if (dataSettledDetail.getIsrefund() != null && dataSettledDetail.getIsrefund() == ISREFUND) {//全部结算成功且单据为退票时，结算状态设置为退票
                    BigDecimal settleErrorSum = foreignPayment.getSettleerrorSum() == null ? new BigDecimal(0) : foreignPayment.getSettleerrorSum();
                    BigDecimal refundAmt = dataSettledDetail.getRefundAmt();
                    BigDecimal successSum = foreignPayment.getAmount().subtract(settleErrorSum).subtract(refundAmt);//结算成功金额
                    foreignPayment.setRefundSum(refundAmt);//退票金额
                    foreignPayment.setSettlesuccessSum(successSum);//结算成功金额
                    foreignPayment.setIsrefund((short) 1);
                    if (foreignPayment.getRefundSum().compareTo(foreignPayment.getAmount()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                        foreignPayment.setSettlestatus(FundSettleStatus.Refund.getValue());
                    } else {
                        foreignPayment.setSettlestatus(FundSettleStatus.PartSuccess.getValue());
                    }
                } else {
                    // 如果当前单据结算状态不为已结算补单和退票，将结算状态置为结算成功，否则不修改
                    boolean isSettlementSupplement = foreignPayment.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue();
                    boolean isRefund = foreignPayment.getSettlestatus() == FundSettleStatus.Refund.getValue();
                    if (!isSettlementSupplement && !isRefund) {
                        foreignPayment.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
                    }
                    foreignPayment.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); //结算成功金额
                }

            } else if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {//全部失败

                foreignPayment.setSettleerrorSum(dataSettledDetail.getStoppedamount()); //结算止付金额
                foreignPayment.setSettlestatus(FundSettleStatus.SettleFailed.getValue()); //结算失败
                foreignPayment.setRejecttype(dataSettledDetail.getRejecttype().toString()); //止付类型
                foreignPayment.setRejectremark(dataSettledDetail.getRejectremark()); //止付说明
                foreignPayment.put("_entityName", "cmp.foreignpayment.ForeignPayment");
                boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
                if (!enableEVNT) {
                    log.error("客户环境未安装事项中台服务");
                    foreignPayment.setVoucherstatus(VoucherStatus.NONCreate.getValue());
                } else {
                    CtmJSONObject billClue = new CtmJSONObject();
                    billClue.put("classifier", foreignPayment.getId());
                    billClue.put("srcBusiId", foreignPayment.getId());
                    cmpSendEventService.sendSimpleEvent(foreignPayment, billClue);
                }
            } else if (String.valueOf(WSettlementResult.PartSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //部分成功 本期没有这个场景
                foreignPayment.setSettleerrorSum(dataSettledDetail.getStoppedamount()); //结算止付金额
                if (dataSettledDetail.getIsrefund() != null && dataSettledDetail.getIsrefund() == ISREFUND) {//单据明细为退票时，记录退票金额，同时更新结算成功金额
                    BigDecimal settleErrorSum = foreignPayment.getSettleerrorSum() == null ? new BigDecimal(0) : foreignPayment.getSettleerrorSum();
                    BigDecimal refundAmt = dataSettledDetail.getRefundAmt();
                    BigDecimal successSum = foreignPayment.getAmount().subtract(settleErrorSum).subtract(refundAmt);//结算成功金额
                    foreignPayment.setSettlesuccessSum(successSum);//结算成功金额
                    foreignPayment.setRefundSum(refundAmt);//退票金额
                } else {
                    foreignPayment.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount());//结算成功金额
                }
                foreignPayment.setSettlestatus(FundSettleStatus.PartSuccess.getValue()); //部分成功
            }
            // 财资统一对账码
            if (foreignPayment.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue() || foreignPayment.getSettlestatus() == FundSettleStatus.PartSuccess.getValue()) {
                foreignPayment.setSmartcheckno(dataSettledDetail.getCheckIdentificationCode());
            }
            //结算单号
            foreignPayment.setRelatedsettlementBillno(dataSettledDetail.getRelatedsettlementBillno());
            //结算单id
            foreignPayment.setSettlementId(dataSettledDetail.getSettlementId());
            //结算成功业务时间
            foreignPayment.setSettleSuccBizTime(dataSettledDetail.getSettleSuccBizTime());
            //结算成功系统时间
            foreignPayment.setSettleSuccSysTime(dataSettledDetail.getSettleSuccSysTime());
            //结算方式code
            foreignPayment.setExpectsettlemethodCode(dataSettledDetail.getExpectsettlemethodCode());
            //结算方式名字
            foreignPayment.setExpectsettlemethodName(dataSettledDetail.getExpectsettlemethod_name());
            //本方银行账户id
            foreignPayment.setSettlemetBankAccountId(dataSettledDetail.getSettlemetBankAccountId());
            //本方银行账户
            foreignPayment.setSettlemetBankAccount(dataSettledDetail.getSettlemetBankAccount());
            //是否换汇支付
            if (dataSettledDetail.getIsExchangePayment()) {
                foreignPayment.setIsExchangePayment((short) 1);
            } else {
                foreignPayment.setIsExchangePayment((short) 0);
            }

            //对方银行账号id
            foreignPayment.setCounterpartybankaccount(dataSettledDetail.getCounterpartybankaccount());
            //对方银行账号
            foreignPayment.setShowoppositebankaccount(dataSettledDetail.getShowoppositebankaccount());
            //是否关联对账单
            if (ObjectUtils.isNotEmpty(dataSettledDetail.getIsRelateCheckBill())) {
                if (("1").equals(dataSettledDetail.getIsRelateCheckBill())) {
                    foreignPayment.setIsassociationbankbill((short) 1);
                    foreignPayment.setAssociationbankbillid(ValueUtils.isNotEmptyObj(dataSettledDetail.getRelateBankCheckBillId()) ?
                            dataSettledDetail.getRelateBankCheckBillId().toString() : null);
                    foreignPayment.setAssociationbillclaimid(ValueUtils.isNotEmptyObj(dataSettledDetail.getRelateClaimBillId()) ?
                            dataSettledDetail.getRelateClaimBillId().toString() : null);
                }

            }


        }
        budgetControlBySettleStatusChange(foreignPayment);
        foreignPayment.setEntityStatus(EntityStatus.Update);//修改操作
        MetaDaoHelper.update(ForeignPayment.ENTITY_NAME, foreignPayment);
        log.error("updateSettledInfoOfForeignPayment, foreignPayment={}, dataSettledDetail = {}", CtmJSONObject.toJSONString(foreignPayment),
                CtmJSONObject.toJSONString(dataSettledDetail));
        return foreignPayment;
    }

    private void budgetControlBySettleStatusChange(ForeignPayment foreignPayment) throws Exception {
        ForeignPayment oldBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, foreignPayment.getId());
        Short settlestatus = foreignPayment.getSettlestatus();
        if (settlestatus != null && settlestatus == FundSettleStatus.SettleSuccess.getValue()) {
            boolean implement = cmpBudgetForeignpaymentManagerService.implement(foreignPayment);
            if (implement) {
                foreignPayment.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
            }
        } else if (settlestatus != null && settlestatus == FundSettleStatus.SettleFailed.getValue()) {
            boolean releaseBudget = cmpBudgetForeignpaymentManagerService.releaseBudget(oldBill);
            if (releaseBudget) {
                foreignPayment.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
            }
        } else if (settlestatus != null && settlestatus == FundSettleStatus.Refund.getValue()) {
            Short isOccupyBudget = foreignPayment.getIsOccupyBudget();
            if (isOccupyBudget != null && isOccupyBudget == OccupyBudget.PreSuccess.getValue()) {
                boolean releaseBudget = cmpBudgetForeignpaymentManagerService.releaseBudget(oldBill);
                if (releaseBudget) {
                    foreignPayment.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                }
            } else if (isOccupyBudget != null && isOccupyBudget == OccupyBudget.ActualSuccess.getValue()) {
                boolean releaseBudget = cmpBudgetForeignpaymentManagerService.releaseImplement(oldBill);
                if (releaseBudget) {
                    foreignPayment.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                }
            }
        } else if (settlestatus != null && settlestatus == FundSettleStatus.SettlementSupplement.getValue()) {
            if (foreignPayment.getSettlesuccesstime() != null) {
                boolean implement = cmpBudgetForeignpaymentManagerService.implement(foreignPayment);
                if (implement) {
                    foreignPayment.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
            }
        }
    }


    private void fundPaymentReleaseFundPlanProject(List<BizObject> releaseFundBillForFundPlanProjectList,
                                                   List<BizObject> partReleaseFundBillForFundPlanProjectList,
                                                   List<BizObject> refundReleaseFundBillForFundPlanProjectList,
                                                   List<BizObject> partRefundReleaseFundBillForFundPlanProjectList) throws Exception {
        // 金额止付时，释放资金计划项目
        if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, releaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, releaseFundBillForFundPlanProjectList, true, null, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, releaseFundBillForFundPlanProjectList, true, null, "pre");
        }
        if (CollectionUtils.isNotEmpty(partReleaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, partReleaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, partReleaseFundBillForFundPlanProjectList, true, null, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, partReleaseFundBillForFundPlanProjectList, true, null, "pre");
        }

        // 金额退票时，释放资金计划项目
        if (CollectionUtils.isNotEmpty(refundReleaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, refundReleaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, refundReleaseFundBillForFundPlanProjectList, null, true, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, refundReleaseFundBillForFundPlanProjectList, null, null, "pre");

        }
        if (CollectionUtils.isNotEmpty(partRefundReleaseFundBillForFundPlanProjectList)) {
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, partRefundReleaseFundBillForFundPlanProjectList.get(0).get("mainid").toString(), 1);
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, partRefundReleaseFundBillForFundPlanProjectList, null, true, "act");
            fundBillAdaptationFundPlanService.fundBillReleaseFundPlan(IBillNumConstant.FUND_PAYMENT, bizObject, partRefundReleaseFundBillForFundPlanProjectList, null, null, "pre");

        }
    }

    public void budgetSuccessFundPayment(FundPayment_b bill_b) throws Exception {
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bill_b.getMainid());
        if (bill_b.getIsOccupyBudget() != null && bill_b.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
            // 已经实占成功的单据，不再进行预算实占的接口调用
            return;
        }
        // 执行
        ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteTrueAudit(fundPayment, bill_b, IBillNumConstant.FUND_PAYMENT, BillAction.APPROVE_PASS);
        if (resultBudget.isSuccess()) {
            bill_b.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
            bill_b.setEntityStatus(EntityStatus.Update);
        }
    }

    public void budgetSuccessFundCollection(FundCollection_b bill_b) throws Exception {
        FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bill_b.getMainid());
        if (bill_b.getIsOccupyBudget() != null && bill_b.getIsOccupyBudget() == OccupyBudget.ActualSuccess.getValue()) {
            // 已经实占成功的单据，不再进行预算实占的接口调用
            return;
        }
        // 执行
        try {
            ResultBudget resultBudget = cmpBudgetManagerService.fundCollectionEmployActualOccupySuccessAudit(fundCollection, bill_b, FUND_COLLECTION, BillAction.APPROVE_PASS);
            if (resultBudget.isSuccess()) {
                bill_b.setEntityStatus(EntityStatus.Update);
                bill_b.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
            }
        } catch (Exception e) {
            log.error("占用预算异常" + e.getMessage());
            throw e;
        }
    }

    public void budgetFail(FundPayment_b bill_b) throws Exception {
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bill_b.getMainid());
        FundPayment_b newFundPayment_b = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, bill_b.getId());
        if (newFundPayment_b.getIsOccupyBudget() != null && newFundPayment_b.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()) {
            // 已经释放预占成功的单据，不再进行释放预占的接口调用
            return;
        }
        // 先释放预占
        ResultBudget cancelResultBudget = cmpBudgetManagerService.gcExecuteUnSubmit(fundPayment, newFundPayment_b, IBillNumConstant.FUND_PAYMENT, BillAction.APPROVE_PASS);
        if (cancelResultBudget.isSuccess()) {
            // 修改引用中的bill_b占用状态，防止后续逻辑覆盖更新
            bill_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
        }
    }

    private void updatePrimaryBillSettleSuccessTime(List<FundPayment_b> paymentBillBs, List<FundCollection_b> collectionBillBs, String entityName) throws Exception {
        // 获取主表id
        String mainId;
        if (CollectionUtils.isNotEmpty(paymentBillBs)) {
            mainId = paymentBillBs.get(0).getMainid();
        } else {
            mainId = collectionBillBs.get(0).getMainid();
        }
        String lockKey = "_settleSuccessTime_" + mainId.toString();
        CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 枷锁失败
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102159"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B6", "更新主表结算成功时间加锁失败！") /* "更新主表结算成功时间加锁失败！" */);
            }
            try {
                BizObject bizObject = MetaDaoHelper.findById(entityName, mainId, 2);
                if (!ValueUtils.isNotEmptyObj(bizObject)) {
                    return;
                }
                if (entityName.equals(FundPayment.ENTITY_NAME) && ValueUtils.isNotEmptyObj(bizObject)) {
                    // 获取所有明细数据
                    List<FundPayment_b> fundPaymentBList = bizObject.get("FundPayment_b");
                    List<Date> dateListData = fundPaymentBList.stream().map(FundPayment_b::getSettleSuccessTime).filter(ValueUtils::isNotEmptyObj).collect(Collectors.toList());
                    List<Date> dateListBusiness = paymentBillBs.stream().map(FundPayment_b::getSettleSuccessTime).filter(ValueUtils::isNotEmptyObj).collect(Collectors.toList());
                    Date settleSuccessTimeSub = null;
                    if (dateListData.isEmpty() && dateListBusiness.isEmpty()) {
                        return;
                    }
                    if (!dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
                        Date dateListDataDate = Collections.max(dateListData);
                        Date dateListBusinessDate = Collections.max(dateListBusiness);
                        settleSuccessTimeSub = DateUtils.dateCompare(dateListDataDate, dateListBusinessDate) >= 1 ? dateListDataDate : dateListBusinessDate;
                    }
                    if (!dateListData.isEmpty() && dateListBusiness.isEmpty()) {
                        settleSuccessTimeSub = Collections.max(dateListData);
                    }
                    if (dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
                        settleSuccessTimeSub = Collections.max(dateListBusiness);
                    }
                    // 取出结算成功的最大结算时间
                    Date settleSuccessTimeMain = bizObject.getDate("settleSuccessTime");
                    if (!ValueUtils.isNotEmptyObj(settleSuccessTimeSub)) {
                        return;
                    }
                    boolean subFlag = ValueUtils.isNotEmptyObj(settleSuccessTimeMain) && DateUtils.dateCompare(settleSuccessTimeSub, settleSuccessTimeMain) == 1;
                    if ((!ValueUtils.isNotEmptyObj(settleSuccessTimeMain)) || subFlag) {
                        FundPayment fundPayment = new FundPayment();
                        fundPayment.setId(bizObject.getId());
                        fundPayment.setSettleSuccessTime(settleSuccessTimeSub);
                        fundPayment.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPayment);
                    }
                } else if (entityName.equals(FundCollection.ENTITY_NAME) && ValueUtils.isNotEmptyObj(bizObject)) {
                    // 获取所有明细数据
                    List<FundCollection_b> fundCollectionBList = bizObject.get("FundCollection_b");
                    // 判断结算状态都不是待结算
                    List<Date> dateListData = fundCollectionBList.stream().map(FundCollection_b::getSettleSuccessTime).filter(ValueUtils::isNotEmptyObj).collect(Collectors.toList());
                    List<Date> dateListBusiness = collectionBillBs.stream().map(FundCollection_b::getSettleSuccessTime).filter(ValueUtils::isNotEmptyObj).collect(Collectors.toList());
                    Date settleSuccessTimeSub = null;
                    if (!dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
                        Date dateListDataDate = Collections.max(dateListData);
                        Date dateListBusinessDate = Collections.max(dateListBusiness);
                        settleSuccessTimeSub = DateUtils.dateCompare(dateListDataDate, dateListBusinessDate) >= 1 ? dateListDataDate : dateListBusinessDate;
                    }
                    if (!dateListData.isEmpty() && dateListBusiness.isEmpty()) {
                        settleSuccessTimeSub = Collections.max(dateListData);
                    }
                    if (dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
                        settleSuccessTimeSub = Collections.max(dateListBusiness);
                    }
                    // 取出结算成功的最大结算时间
                    Date settleSuccessTimeMain = bizObject.getDate("settleSuccessTime");
                    if (!ValueUtils.isNotEmptyObj(settleSuccessTimeSub)) {
                        return;
                    }
                    boolean subFlag = ValueUtils.isNotEmptyObj(settleSuccessTimeMain) && DateUtils.dateCompare(settleSuccessTimeSub, settleSuccessTimeMain) == 1;
                    if ((!ValueUtils.isNotEmptyObj(settleSuccessTimeMain)) || subFlag) {
                        /*Map<String, Object> params = new HashMap<>();
                        params.put(ICmpConstant.TABLE_NAME, "stwb_settleapply_fund_assistant");
                        params.put("settleSuccessTime", settleSuccessTimeSub);
                        params.put("id", bizObject.getId());
                        SqlHelper.update(ICmpConstant.UPDATE_VOUCHER_ID, params);*/
                        FundCollection fundCollection = new FundCollection();
                        fundCollection.setId(bizObject.getId());
                        fundCollection.setSettleSuccessTime(settleSuccessTimeSub);
                        fundCollection.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FundCollection.ENTITY_NAME, fundCollection);
                    }
                }
            } catch (Exception e) {
                log.error("update bill settle success time fail, mainId={}", mainId);
                throw e;
            }
        });
    }

    /**
     * 资金付款单推事项中心过账
     *
     * @param billb
     * @throws Exception
     */
    @Override
    public void generateVoucher(BizObject billb, String entityName, boolean redReset) throws Exception {
        log.error("generator Voucher enyityName= {}, billb = {}", entityName, CtmJSONObject.toJSONString(billb));
        String mainid = billb.get("mainid");
        String lockKey = "_generateVoucher_" + mainid.toString();
        CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
            if (lockStatus == LockStatus.GETLOCK_FAIL) {
                // 枷锁失败
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102160"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B5", "结算成功后过账加锁失败！") /* "结算成功后过账加锁失败！" */);
            }
            executeGenerateVoucher(billb, entityName, redReset, mainid);
        });
    }

    /**
     * 执行
     *
     * @param billb
     * @param entityName
     * @param redReset
     * @param mainid
     * @throws Exception
     */
    private void executeGenerateVoucher(BizObject billb, String entityName, boolean redReset, String mainid) throws Exception {
        try {
            log.error("进入executeGenerateVoucher成功");
            ApplicationVO appVo = appService.findByTenantIdAndApplicationCode(InvocationInfoProxy.getTenantid(), "FP");
            if (appVo == null || !appVo.isEnable()) {
                log.error("客户环境未安装财务公共服务");
                return;
            }
            boolean enableGL = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EGL_APP_CODE);
            if (!enableGL) {
                log.error("客户环境未安装总账服务");
                return;
            }
            boolean enableEvnt = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
            if (!enableEvnt) {
                log.error("客户环境未安装事项中台服务");
                return;
            }
            BizObject bizObject = MetaDaoHelper.findById(entityName, mainid, 2);
            bizObject.set("_entityName", entityName);
            if (CmpCommonUtil.getNewFiFlag()) {
                // 如果是红冲，则直接进行过账
                if (redReset) {
                    CtmJSONObject billClue = new CtmJSONObject();
                    bizObject.put("_entityName", entityName);
                    billClue.put("srcBusiId", String.valueOf(bizObject.getId().toString()));
                    billClue.put("classifier", String.valueOf(billb.getId().toString()));
                    billClue.put("billVersion", billb.getPubts().getTime());
                    cmpSendEventService.sendSimpleEvent(bizObject, billClue);
                    log.error("settle write back, send event generate voucher success!, bizObject = {}", bizObject);
                    return;
                }

                // 判断是否进行过账
                boolean bSendSimpleEvent = false;
                if (FundPayment.ENTITY_NAME.equals(entityName)) {
                    FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, mainid);
                    bSendSimpleEvent = bSendSimpleEvent(fundPayment, fundPayment.FundPayment_b());
                }
                if (FundCollection.ENTITY_NAME.equals(entityName)) {
                    FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, mainid);
                    bSendSimpleEvent = bSendSimpleEvent(fundCollection, fundCollection.FundCollection_b());
                }
                if (!bSendSimpleEvent) {
                    log.error("bSendSimpleEvent为false，直接返回，不生成事项");
                    return;
                }
                bizObject.put(IBussinessConstant.ORI_SUM, billb.get("oriSum"));
                bizObject.put(IBussinessConstant.NAT_SUM, billb.get("natSum"));
                bizObject.put("_entityName", entityName);
                CtmJSONObject billClue = new CtmJSONObject();
                billClue.put("srcBusiId", String.valueOf(bizObject.getId().toString()));
                billClue.put("billVersion", bizObject.getPubts().getTime());
                log.error("cmpSendEventService.sendSimpleEvent 执行前日志");
                cmpSendEventService.sendSimpleEvent(bizObject, billClue);
                log.error("cmpSendEventService.sendSimpleEvent 发送事件执行完毕，后续执行更新单据过账状态");
                if (FundPayment.ENTITY_NAME.equals(entityName)) {
                    FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, mainid);
                    fundPayment.setEntityStatus(EntityStatus.Update);
                    fundPayment.setVoucherstatus(VoucherStatus.POSTING);
                    MetaDaoHelper.update(FundPayment.ENTITY_NAME,fundPayment);
                }
                if (FundCollection.ENTITY_NAME.equals(entityName)) {
                    FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, mainid);
                    fundCollection.setEntityStatus(EntityStatus.Update);
                    fundCollection.setVoucherstatus(VoucherStatus.POSTING);
                    MetaDaoHelper.update(FundCollection.ENTITY_NAME,fundCollection);
                }
                log.error("settle write back, send event generate voucher success!, bizObject = {}", bizObject);
            }
        } catch (Exception e) {
            log.error("update bill settle success time fail, mainid={}", mainid);
            throw e;
        }
    }

    @Override
    public boolean checkFundPlanIsEnabled(String accentity) throws Exception {
        boolean enableCSPL = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.CTM_YONBIP_FI_CTMFC_CSPL_APP_CODE);
        return enableCSPL;
    }

    @Override
    public boolean checkFundPlanIsEnabledBySalarypay(ServiceNameEnum serviceNameEnum) throws Exception {
         /*Map<String, Object> autoConfig = cmCommonService.queryAutoConfigByAccentity(accentity);
        return autoConfig != null && autoConfig.get("checkFundPlan") != null && (Boolean) autoConfig.get("checkFundPlan");*/

        List<TmspSystemResp> tmspSystemResps;
        String cacheKey = serviceNameEnum.getValue().toString();
        List<TmspSystemResp> tmspSystemRespListCacheValue = tmspSystemRespListCache.getIfPresent(cacheKey);
        if (tmspSystemRespListCacheValue != null) {
            tmspSystemResps = tmspSystemRespListCacheValue;
        } else {
            ITmspSystemRespRpcService service = AppContext.getBean(ITmspSystemRespRpcService.class);
            TmspSystemReq request = new TmspSystemReq();
            request.setApplyname(String.valueOf(ApplyNameEnum.CMP.getValue()));
            request.setServicename(String.valueOf(serviceNameEnum.getValue()));
            tmspSystemResps = service.querySystemParameters(request);
            if (CollectionUtils.isEmpty(tmspSystemResps)) {
                return false;
            } else {
                tmspSystemRespListCache.put(cacheKey, tmspSystemResps);
            }
        }
        return StringUtils.contains(tmspSystemResps.get(0).getControlledContent(), "1");
    }

    @Override
    public boolean checkFundPlanControlIsEnabled(String serviceName) throws Exception {
        TmspSystemReq tmspSystemReq = new TmspSystemReq();
        tmspSystemReq.setApplyname("6");// CMP("现金管理", (short) 6)
        // FUND_PAYMENT("资金付款单", (short) 15),
        // FUND_COLLECT("资金收款单", (short) 16),
        tmspSystemReq.setServicename(serviceName);
        List<TmspSystemResp> tmspSystemResp = iTmspSystemRespRpcService.querySystemParameters(tmspSystemReq);
        //查询财资公共的系统间集成参数，判断是否要与资金计划集成，不集成就不校验和占用资金计划
        return !tmspSystemResp.isEmpty()
                && ValueUtils.isNotEmptyObj(tmspSystemResp.get(0).getControlledContent())
                && tmspSystemResp.get(0).getControlledContent().contains("1")
                && !tmspSystemResp.get(0).getCsplControlledContent().contains("1");
    }

    @Override
    public void deleteNoteList(List<Map<String, Object>> noteMaps, Integer billDirection, BizObject bizObject, BizObject subBiz) throws Exception {
        Integer serviceAttr = cmCommonService.getServiceAttr(subBiz.get("settlemode"));
        if (IStwbConstant.SERVICEATTR_DIRT == serviceAttr && ValueUtils.isNotEmptyObj(subBiz.get("noteno"))) {
            Map<String, Object> mapNote = new HashMap<>(1);
            mapNote.put("billdirection", billDirection);
            mapNote.put("accentity", bizObject.get("accentity"));
            mapNote.put("vouchdate", bizObject.get("vouchdate"));
            mapNote.put("dept", subBiz.get("dept"));
            mapNote.put("operator", AppContext.getCurrentUser().getName());
            mapNote.put("project", subBiz.get("project"));
            mapNote.put("description", subBiz.get("description"));
            mapNote.put("pk_register", subBiz.get("noteno"));
            mapNote.put("endorseekind", "RC01");
            //新一代票据新增字段  交易金额
            mapNote.put("transmoney", subBiz.get("oriSum"));
            // 海螺项目发现问题，传参导致票据可被重复占用，需要注释掉
//            mapNote.put("isSettle", true);
            short caobject = Short.parseShort(subBiz.get("caobject").toString());
            if (caobject == CaObject.Customer.getValue()) {
                //客户id, 对方银行账号id
                mapNote.put("customer", subBiz.get("oppositeobjectid"));
                mapNote.put("endorseebankaccbycust", subBiz.get("oppositeaccountid"));
            } else if (caobject == CaObject.Supplier.getValue()) {
                //供应商id, 对方银行账号id
                mapNote.put("supplier", subBiz.get("oppositeobjectid"));
                mapNote.put("endorseebankaccbysupp", subBiz.get("oppositeaccountid"));
            } else if (caobject == CaObject.CapBizObj.getValue()) {
                //供应商id, 对方银行账号id
                mapNote.put("capBizObj", subBiz.get("fundbusinobjtypeid"));
                mapNote.put("endorseebankaccbycap", subBiz.get("oppositeaccountid"));
            } else if (caobject == CaObject.Other.getValue()) {
                //其他名称, 对方银行账号, 对方银行账户名称, 对方银行类别名称, 对方联行号
                mapNote.put("othername", subBiz.get("oppositeobjectname"));
                mapNote.put("endorseeacctno", subBiz.get("oppositeaccountno"));
                mapNote.put("endorseeacctname", subBiz.get("oppositeaccountname"));
                mapNote.put("endorseebankname", subBiz.get("oppositebankType"));
                mapNote.put("endorseebankcode", subBiz.get("oppositebanklineno"));
            }
            mapNote.put("openserialno", null);
            mapNote.put("outertradetype", bizObject.get("tradetype"));
            mapNote.put("outerbilltype", billDirection == 1 ? EventType.FundCollection.getValue() : EventType.FundPayment.getValue());
            mapNote.put("outereventype", EventSource.Cmpchase.getValue());
            mapNote.put("outerhid", subBiz.get("mainid"));
            mapNote.put("outerbid", subBiz.get("id"));
            mapNote.put("settleStatus", "saved");
            noteMaps.add(mapNote);
        }
        log.error("noteNo release or employ parameter handler, code={}, noteMaps={}", bizObject.get(ICmpConstant.CODE), CtmJSONObject.toJSONString(noteMaps));
    }

//    private void noteRelease(BizObject bizObject, List<Map<String, Object>> noteMaps) {
//        try {
//            BaseResultVO jsonObject = ctmDrftEndorePaybillRpcService.settleReleaseBillNew(noteMaps);
//            log.error("fund bill note release success! code={}, id={}, inputParameter={}, outputParameter={}",
//                    bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), CtmJSONObject.toJSONString(jsonObject));
//        } catch (Exception e) {
//            log.error("fund bill note release fail! code={}, id={}, inputParameter={}, errorMsg={}",
//                    bizObject.get("code"), bizObject.getId(), CtmJSONObject.toJSONString(noteMaps), e.getMessage());
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100368"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180110", "单据明细行结算方式为票据结算，释放票据失败，请检查数据!") /* "单据明细行结算方式为票据结算，释放票据失败，请检查数据!" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180111", "：") /* "：" */ + e.getMessage());
//        }
//    }

    /**
     * <h2>更新该币种银行结息账号预提规则的上次结息结束日</h2>
     *
     * @param fundSubList : 单据子表信息
     * @param status      : 单据状态：1：撤回，2：审核
     * @author Sun GuoCai
     * @since 2023/5/9 9:56
     */
    @Override
    public void updateWithholdingRuleSettingLastInterestSettlementDate(List<BizObject> fundSubList, Integer status) throws Exception {
        List<WithholdingRuleSetting> updateWithholdingRuleSettingList = new ArrayList<>();
        for (BizObject bizObjSub : fundSubList) {
            // 判断明细行上的款项类型是否为利息结算
            boolean isNotNullForInterestSettlementAccount = ValueUtils.isNotEmptyObj(bizObjSub.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT));
            if (isInterestWithQuickType(bizObjSub.get(ICmpConstant.QUICK_TYPE)) && isNotNullForInterestSettlementAccount) {
                // 根据结息账户id和币种查询预提规则信息表
                Object interestSettlementAccount = bizObjSub.get(ICmpConstant.INTEREST_SETTLEMENT_ACCOUNT);
                Object currency = bizObjSub.get(ICmpConstant.CURRENCY);
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name(BANKACCOUNT).eq(interestSettlementAccount),
                        QueryCondition.name(ICmpConstant.CURRENCY).eq(currency));
                querySchema.addCondition(queryConditionGroup);
                List<Map<String, Object>> withholdingRuleSettingList = MetaDaoHelper.query(WithholdingRuleSetting.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isNotEmpty(withholdingRuleSettingList) && withholdingRuleSettingList.size() > 0) {
                    Map<String, Object> withholdingRuleSettingMap = withholdingRuleSettingList.get(0);
                    WithholdingRuleSetting withholdingRuleSetting = new WithholdingRuleSetting();
                    withholdingRuleSetting.init(withholdingRuleSettingMap);
                    // 审核通过后，本次结息结束日为空，则跳过循环
                    if (status == 2 && !ValueUtils.isNotEmptyObj(bizObjSub.getDate(CURRENT_INTEREST_SETTLEMENT_END_DATE))) {
                        continue;
                    }
                    // 弃审通过后,
                    if (status == 1 && !ValueUtils.isNotEmptyObj(bizObjSub.getDate(LAST_INTEREST_SETTLEMENT_END_DATE))) {
                        continue;
                    }
                    switch (status) {
                        // 撤回时，将结息单上的上次结息结束日更新回去
                        case 1:
                            withholdingRuleSetting.setLastInterestSettlementDate(bizObjSub.getDate(LAST_INTEREST_SETTLEMENT_END_DATE));
                            break;
                        // 提交时，将结息单上的本次结息结束日更新上去
                        case 2:
                            withholdingRuleSetting.setLastInterestSettlementDate(bizObjSub.getDate(CURRENT_INTEREST_SETTLEMENT_END_DATE));
                            break;
                        default:
                            break;
                    }
                    updateWithholdingRuleSettingList.add(withholdingRuleSetting);
                }
            }
        }
        // 批量更新预提规则的上次结息结束日字段
        if (CollectionUtils.isNotEmpty(updateWithholdingRuleSettingList)) {
            EntityTool.setUpdateStatus(updateWithholdingRuleSettingList);
            MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, updateWithholdingRuleSettingList);
        }
    }


    /**
     * <h2>根据款项类型判断此款项类型是否为账户结息，注：账户结息的编码为301</h2>
     *
     * @param quickType :款项类型
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/5/14 11:15
     */
    @Override
    public boolean isInterestWithQuickType(Object quickType) {
        try {
            List<Map<String, Object>> quickTypeByCode = CmpCommonUtil.paymentTypes(Collections.singletonList(quickType.toString()));
            if (ValueUtils.isNotEmptyObj(quickTypeByCode) && quickTypeByCode.size() > CONSTANT_ZERO) {
                Map<String, Object> map = quickTypeByCode.get(CONSTANT_ZERO);
                String quickTypeCodeDb = ValueUtils.isNotEmptyObj(map.get(CODE)) ? map.get(CODE).toString() : "";
                return ValueUtils.isNotEmptyObj(quickTypeCodeDb) && THREE_HUNDRED_ONE.equals(quickTypeCodeDb);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }


    /**
     * <h2>资金收付款单前端点击保存并提交，未启用审批流的前提下，保存并提交成功后，再推结算</h2>
     * 原因：如果未启用审批流，则先不推结算（因为单据还没入库，结算那边调用单据转换规则会报错，未在库里查到数据）
     *
     * @param params : 入参
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/5/14 11:18
     */
    @Override
    public boolean pushSettleBill(CtmJSONObject params) {
        String id = params.getString("id");
        BizObject bizObject = null;
        try {
            List<BizObject> currentBillList = new ArrayList<>();
            if (FUND_COLLECTION.equals(params.getString(BILL_NUM))) {
                bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id, CONSTANT_TWO);
            } else if (IBillNumConstant.FUND_PAYMENT.equals(params.getString(BILL_NUM))) {
                bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, CONSTANT_TWO);
            }
            assert bizObject != null;
            Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get("accentity"), ISystemCodeConstant.ORG_MODULE_GL);
            boolean dataFlag = true;
            if (enabledBeginData == null) {
                dataFlag = false;
            } else {
                if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
                    dataFlag = false;
                }
            }
            Map<String, Object> autoConfig = cmCommonService.queryAutoConfigByAccentity(bizObject.getString("accentity"));
            boolean isSettleSuccessToPost;
            if (autoConfig != null) {
                // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
                if (autoConfig.get("isSettleSuccessToPost") != null) {
                    isSettleSuccessToPost = (Boolean) autoConfig.get("isSettleSuccessToPost");
                } else {
                    Map<String, Object> autoConfigTenant = cmCommonService.queryAutoConfigTenant();
                    isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
                }
            } else {
                Map<String, Object> autoConfigTenant = cmCommonService.queryAutoConfigTenant();
                isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
            }
            // dataFlag为false时（没买总账），不需要考虑是否启用新架构，即推结算，不过账(不生成凭证)
            // dataFlag为false时（买总账），当flag为false时，即为启用新架构，则走老架构，立即推结算，否则在这里不推结算，先过账，过账成功后再推结算
            //查询现金参数是否为可生成收款单据
            boolean isPushStwb = !dataFlag || (dataFlag && !CmpCommonUtil.getNewFiFlag());//财务新架构标识
            boolean settleflagBool = bizObject.getBoolean("settleflag");
            if (settleflagBool && (isSettleSuccessToPost || isPushStwb)) {
                // 判断如果是保存并提交按钮，如果未启用审批流，则先不推结算（因为单据还没入库，结算那边调用单据转换规则会报错，未在库里查到数据）
                boolean isWfControlled = ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.IS_WFCONTROLLED)) ? bizObject.get(ICmpConstant.IS_WFCONTROLLED) : false;
                if (!isWfControlled) {
                    currentBillList.add(bizObject);
                    if (FUND_COLLECTION.equals(params.getString(BILL_NUM))) {
                        stwbBillCollectionService.pushBill(currentBillList, false);
                    } else if (IBillNumConstant.FUND_PAYMENT.equals(params.getString(BILL_NUM))) {
                        stwbBillPaymentService.pushBill(currentBillList, false);
                    }
                    //金额为0的表体数据更新为结算成功；
                    if (FUND_COLLECTION.equals(params.getString(BILL_NUM))) {
                        List<FundCollection_b> list = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                        list.forEach(item -> {
                            if (item.getOriSum().compareTo(BigDecimal.ZERO) == 0 && item.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                                item.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                                item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                                item.setSettlesuccessSum(BigDecimal.ZERO);
                                item.setSettleSuccessTime(Objects.isNull(AppContext.getCurrentUser().getBusinessDate()) ? new Date() : AppContext.getCurrentUser().getBusinessDate());
                                item.setEntityStatus(EntityStatus.Update);
                            }
                        });
                        MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, list);
                    }
                    if (FUND_PAYMENT.equals(params.getString(BILL_NUM))) {
                        List<FundPayment_b> list = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                        list.forEach(item -> {
                            if (item.getOriSum().compareTo(BigDecimal.ZERO) == 0 && item.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                                item.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                                item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                                item.setSettlesuccessSum(BigDecimal.ZERO);
                                item.setSettleSuccessTime(Objects.isNull(AppContext.getCurrentUser().getBusinessDate()) ? new Date() : AppContext.getCurrentUser().getBusinessDate());
                                item.setEntityStatus(EntityStatus.Update);
                            }
                        });
                        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, list);
                    }
                }
            } else {
                if (FUND_COLLECTION.equals(params.getString(BILL_NUM))) {
                    List<FundCollection_b> list = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
                    list.forEach(item -> {
                        if (item.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                            item.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                            item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                            item.setEntityStatus(EntityStatus.Update);
                        }
                    });
                    MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, list);
                }
                if (FUND_PAYMENT.equals(params.getString(BILL_NUM))) {
                    List<FundPayment_b> list = bizObject.getBizObjects("FundPayment_b", FundPayment_b.class);
                    list.forEach(item -> {
                        if (item.getFundSettlestatus().getValue() == FundSettleStatus.WaitSettle.getValue()) {
                            item.setFundSettlestatus(FundSettleStatus.SettleSuccess);
                            item.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(item.getFundSettlestatus()));
                            item.setEntityStatus(EntityStatus.Update);
                        }
                    });
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, list);
                }
            }
            return true;
        } catch (Exception e) {
            if (bizObject == null) {
                log.error("when saveAndSubmit, bill not exist! id={}, params={}, e={}", id, params, e.getMessage());
            } else {
                log.error("when saveAndSubmit, push Settle Bill fail! id={}, code={}, params={}, e={}", id, bizObject.get("code").toString(), params, e.getMessage());
            }
            return false;
        }
    }

    /**
     * <h2>资金收付款单推待结算</h2>
     *
     * @param billNum : 入参
     * @param ids     : 入参
     * @return boolean
     * @author Sun GuoCai
     * @since 2023/5/14 11:18
     */
    @Override
    public boolean pushDataSettle(String billNum, String ids) throws Exception {
        List<BizObject> currentBillList = new ArrayList<>();
        List<Long> idsList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
        if (FUND_COLLECTION.equals(billNum)) {
            for (Long id : idsList) {
                BizObject object = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id, 3);
                currentBillList.add(object);
            }
            stwbBillCollectionService.pushBill(currentBillList, false);
        } else if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
            for (Long id : idsList) {
                BizObject object = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, 3);
                currentBillList.add(object);
            }
            stwbBillPaymentService.pushBill(currentBillList, false);
        }
        return true;
    }

    /**
     * <h2>结息单撤回相关校验</h2>
     *
     * @param bills : 单据信息
     * @author Sun GuoCai
     * @since 2023/5/23 15:27
     */
    @Override
    public void statementUnSubmitVerificationByFundCollection(List<BizObject> bills) throws Exception {
        // 1.撤回校验：本次结息结束日之后存在结息记录，不允许撤回
        List<String> messages = new ArrayList<>();
        for (BizObject bill : bills) {
            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bill.getId(), ICmpConstant.CONSTANT_TWO);
            List<FundCollection_b> fundCollectionBList = fundCollection.get(ICmpConstant.FUND_COLLECTION_B);
            for (FundCollection_b fundCollectionB : fundCollectionBList) {
                if (isInterestWithQuickType(fundCollectionB.get(ICmpConstant.QUICK_TYPE))
                        && ValueUtils.isNotEmptyObj(fundCollectionB.getInterestSettlementAccount())) {
                    if (!ValueUtils.isNotEmptyObj(fundCollectionB.getCurrentInterestSettlementEndDate())) {
                        continue;
                    }
                    // 根据结息账户id和币种查询子表信息
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("code, FundCollection_b.currentInterestSettlementEndDate as currentInterestSettlementEndDate");
                    QueryConditionGroup group1 = QueryConditionGroup.and(
                            QueryCondition.name("FundCollection_b.currency").eq(fundCollectionB.getCurrency()),
                            QueryCondition.name("FundCollection_b.interestSettlementAccount").eq(fundCollectionB.getInterestSettlementAccount()));
                    querySchema1.addCondition(group1);
                    List<BizObject> items = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema1, null);
                    log.error("FundCollection statementUnSubmitVerification verify, code={}, items={}",
                            fundCollection.get(CODE).toString(), org.apache.commons.collections4.CollectionUtils.isNotEmpty(items) ? CtmJSONObject.toJSONString(items) : null);
                    if (CollectionUtils.isNotEmpty(items)) {
                        for (BizObject item : items) {
                            if (!ValueUtils.isNotEmptyObj(item.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE))) {
                                continue;
                            }
                            int compare = DateUtil.compare(fundCollectionB.getCurrentInterestSettlementEndDate(), item.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE));
                            // 不允许还有比当前日期大的数据存在
                            if (compare < 0) {
                                String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DB", "资金收款单编号：[%s]，本次结息结束日之后存在结息记录，不允许撤回") /* "资金收款单编号：[%s]，本次结息结束日之后存在结息记录，不允许撤回" */, fundCollection.get(CODE).toString());
                                messages.add(message);
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(messages)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102161"), StringUtils.join(messages.toArray(), DELIMITER));
        }
    }

    /**
     * <h2>资金付款结息单撤回相关校验</h2>
     *
     * @param bills : 单据信息
     * @author Sun GuoCai
     * @since 2023/5/23 15:27
     */
    @Override
    public void statementUnSubmitVerificationByFundPayment(List<BizObject> bills) throws Exception {
        // 1.撤回校验：本次结息结束日之后存在结息记录，不允许撤回
        List<String> messages = new ArrayList<>();
        for (BizObject bill : bills) {
            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bill.getId(), ICmpConstant.CONSTANT_TWO);
            List<FundPayment_b> fundPaymentBList = fundPayment.get(ICmpConstant.FUND_PAYMENT_B);
            for (FundPayment_b fundPaymentB : fundPaymentBList) {
                if (isInterestWithQuickType(fundPaymentB.get(ICmpConstant.QUICK_TYPE))
                        && ValueUtils.isNotEmptyObj(fundPaymentB.getInterestSettlementAccount())) {
                    if (!ValueUtils.isNotEmptyObj(fundPaymentB.getCurrentInterestSettlementEndDate())) {
                        continue;
                    }
                    // 根据结息账户id和币种查询子表信息
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("code, FundPayment_b.currentInterestSettlementEndDate as currentInterestSettlementEndDate");
                    QueryConditionGroup group1 = QueryConditionGroup.and(
                            QueryCondition.name("FundPayment_b.stwbSettleStatus").not_eq(SettleApplyDetailStateEnum.ALL_FAIL.getValue()),
                            QueryCondition.name("FundPayment_b.currency").eq(fundPaymentB.getCurrency()),
                            QueryCondition.name("FundPayment_b.interestSettlementAccount").eq(fundPaymentB.getInterestSettlementAccount()));
                    querySchema1.addCondition(group1);
                    List<BizObject> items = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema1, null);
                    log.error("FundPayment statementUnSubmitVerification verify, code={}, items={}",
                            fundPayment.get(CODE).toString(), org.apache.commons.collections4.CollectionUtils.isNotEmpty(items) ? CtmJSONObject.toJSONString(items) : null);
                    if (CollectionUtils.isNotEmpty(items)) {
                        for (BizObject item : items) {
                            if (!ValueUtils.isNotEmptyObj(item.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE))) {
                                continue;
                            }
                            int compare = DateUtil.compare(fundPaymentB.getCurrentInterestSettlementEndDate(), item.getDate(ICmpConstant.CURRENT_INTEREST_SETTLEMENT_END_DATE));
                            // 根据结息账户id和币种查询子表信息
                            if (compare < 0) {
                                String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000D8", "资金付款单编号：[%s]，本次结息结束日之后存在结息记录，不允许撤回") /* "资金付款单编号：[%s]，本次结息结束日之后存在结息记录，不允许撤回" */, fundPayment.get(CODE).toString());
                                messages.add(message);
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(messages)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102162"), StringUtils.join(messages.toArray(), DELIMITER));
        }
    }


    /**
     * <h2>设置是否结算成功时过账字段值</h2>
     *
     * @param bill        : 单据数据信息
     * @param accEntityId : 会计主体
     * @author Sun GuoCai
     * @since 2023/5/26 9:18
     */
    public void setSettleSuccessPostValue(BizObject bill, String accEntityId) throws Exception {
        Map<String, Object> autoConfig = cmCommonService.queryAutoConfigByAccentity(accEntityId);
        boolean isSettleSuccessToPost;
        if (autoConfig != null) {
            // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
            if (autoConfig.get("isSettleSuccessToPost") != null) {
                isSettleSuccessToPost = (Boolean) autoConfig.get("isSettleSuccessToPost");
            } else {
                Map<String, Object> autoConfigTenant = cmCommonService.queryAutoConfigTenant();
                isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
            }
        } else {
            Map<String, Object> autoConfigTenant = cmCommonService.queryAutoConfigTenant();
            isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
        }
        if (isSettleSuccessToPost) {
            bill.set("settleSuccessPost", 1);
        } else {
            bill.set("settleSuccessPost", 0);
        }
    }


    private CtmJSONObject getJsonObject(CtmJSONArray rows, List<String> messages, int failedCount) {
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("count", rows.size());
        responseData.put("successCount", rows.size() - failedCount);
        responseData.put("failCount", failedCount);
        responseData.put("messages", messages);
        responseData.put("infos", rows);
        return responseData;
    }

    /**
     * 处理资金付款单退款结算状态修改
     */
    public void updateRefundSettledInfoOfFundPayment(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs) throws Exception {
        List<BizObject> refundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partRefundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<FundPayment_b> updateList = new ArrayList();
        List<DataSettledDistribute> dataSettledDistributeList = dataSettledDetail.getDataSettledDistribute();
        BigDecimal realRefundAmt = BigDecimal.ZERO;
        for (DataSettledDistribute dataSettledDistribute : dataSettledDistributeList) {
            if (dataSettledDistribute.getIsrefund() != null && dataSettledDistribute.getIsrefund() == ISREFUND && ValueUtils.isNotEmptyObj(dataSettledDistribute.getSuccessDistributeMoney())) {
                realRefundAmt = realRefundAmt.add(dataSettledDistribute.getSuccessDistributeMoney());
            }
        }
//        DataSettledDistribute dataSettledDistribute = dataSettledDetail.getDataSettledDistribute().get(0);
        for (FundPayment_b billb : billbs) {
            // 回传实际结算汇率类型
            billb.setActualSettlementExchangeRateType(dataSettledDetail.getExchangeRateType());
            // 回传实际结算金额
            BigDecimal actualSettlementAmount = ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())
                    ? dataSettledDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
            billb.setActualSettlementAmount(actualSettlementAmount.setScale(8, RoundingMode.HALF_UP));
            // 回传实际结算汇率
            billb.setActualSettlementExchangeRate(dataSettledDetail.getExchangePaymentRate());
            //结算单拆分后，获取最大的时间进行业务单据结算成功时间赋值，由获取结算时间改为获取结算日期
            List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
            if (CollectionUtils.isNotEmpty(dataSettledDistributes)) {
                Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                try {
                    //结算止付的不设置结算成功日期
                    if (dataSettled.isPresent() && !String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
                        billb.setSettleSuccessTime(dataSettled.get().getSettleSuccBizTime());
                    }
                } catch (Exception e) {
                    log.error("get fund payment bill child settle success time fail! id={}, settleSuccessTime={}, message={}"
                            , billb.getId(), dataSettled.get().getSettleSuccBizTime(), e.getMessage());
                }
            }
            if ((StringUtils.isNotEmpty(billb.getTransNumber()) && dataSettledDetail.getDataSettledId().compareTo(Long.valueOf(billb.getTransNumber())) == 0)
                    || (StringUtils.isNotEmpty(dataSettledDetail.getBusinessDetailsId()) && dataSettledDetail.getBusinessDetailsId().equals(billb.getId().toString()))) {
                Object isToPushCspl = billb.get(ICmpConstant.IS_TO_PUSH_CSPL);
                if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult()) && 0 ==
                        billb.getOriSum().compareTo(new BigDecimal(dataSettledDetail.getSuccesssettlementAmount().toString()))) {// 全部成功
                    billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount()); //结算成功金额
                    if (dataSettledDetail.getIsrefund() != null && dataSettledDetail.getIsrefund() == ISREFUND) {//全部结算成功且单据为退票时，结算状态设置为退票
                        BigDecimal refundAmtDB = ValueUtils.isNotEmptyObj(billb.getRefundSum()) ? billb.getRefundSum() : BigDecimal.ZERO;
                        billb.setRefundSum(realRefundAmt);//退票金额:退票时，取分配表上的分配金额，有合并或拆分待结算的场景，不能直接取退票金额
                        if (billb.getRefundSum().compareTo(billb.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                            billb.setFundSettlestatus(FundSettleStatus.Refund);
                            billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));

                            // 关联退票，释放预算
                            Short budgeted = billb.getIsOccupyBudget();
                            if (budgeted != null && ((budgeted == OccupyBudget.ActualSuccess.getValue()))) {
                                updateList.add(billb);
                                billb.setEntityStatus(EntityStatus.Update);
                            }
                        } else {
                            billb.setFundSettlestatus(FundSettleStatus.PartSuccess);
                            billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                        }
                        if (BigDecimal.ZERO.compareTo(realRefundAmt) < 0
                                && ValueUtils.isNotEmptyObj(isToPushCspl)
                                && 1 == Integer.parseInt(isToPushCspl.toString())) {
                            if (refundAmtDB.compareTo(realRefundAmt) != 0) {
                                BigDecimal settleErrorSum = ValueUtils.isNotEmptyObj(billb.getSettleerrorSum())
                                        ? billb.getSettleerrorSum() : BigDecimal.ZERO;
                                BigDecimal oriSum = BigDecimalUtils.safeSubtract(billb.getOriSum(), settleErrorSum);
                                if (BigDecimal.ZERO.compareTo(refundAmtDB) == 0 && realRefundAmt.compareTo(oriSum) == 0) {// 退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                                    refundReleaseFundBillForFundPlanProjectList.add(billb);
                                    billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                                } else {
                                    billb.set("partRefundSum", BigDecimalUtils.safeSubtract(realRefundAmt, refundAmtDB));
                                    partRefundReleaseFundBillForFundPlanProjectList.add(billb);
                                }
                            }
                        }
                        billb.setEntityStatus(EntityStatus.Update);//修改操作
                    }
                } else if (String.valueOf(WSettlementResult.PartSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //部分成功
                    BigDecimal refundAmtDB = ValueUtils.isNotEmptyObj(billb.getRefundSum()) ? billb.getRefundSum() : BigDecimal.ZERO;
                    billb.setRefundSum(realRefundAmt);//退票金额
                    billb.setSettlesuccessSum(dataSettledDetail.getSuccesssettlementAmount());//结算成功金额
                    billb.setSettleerrorSum(dataSettledDetail.getStoppedamount());//结算止付金额
                    if (billb.getRefundSum().compareTo(billb.getSettlesuccessSum()) == 0) {//退票金额与结算成功金额相同，则结算状态为退票，否则为部分成功
                        billb.setFundSettlestatus(FundSettleStatus.Refund);
                    } else {
                        billb.setFundSettlestatus(FundSettleStatus.PartSuccess);
                    }
                    billb.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(billb.getFundSettlestatus()));
                    BigDecimal settleErrorSum = ValueUtils.isNotEmptyObj(billb.getSettleerrorSum())
                            ? billb.getSettleerrorSum() : BigDecimal.ZERO;
                    if (BigDecimal.ZERO.compareTo(realRefundAmt) < 0
                            && ValueUtils.isNotEmptyObj(isToPushCspl)
                            && 1 == Integer.parseInt(isToPushCspl.toString())) {
                        if (refundAmtDB.compareTo(realRefundAmt) != 0) {
                            BigDecimal oriSum = BigDecimalUtils.safeSubtract(billb.getOriSum(), settleErrorSum);
                            if (BigDecimal.ZERO.compareTo(refundAmtDB) == 0 && realRefundAmt.compareTo(oriSum) == 0) {// 退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                                refundReleaseFundBillForFundPlanProjectList.add(billb);
                                billb.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                            } else {
                                billb.set("partRefundSum", BigDecimalUtils.safeSubtract(realRefundAmt, refundAmtDB));
                                partRefundReleaseFundBillForFundPlanProjectList.add(billb);
                            }
                        }
                    }
                    billb.setEntityStatus(EntityStatus.Update);//修改操作
                }
            }
        }


        if (!updateList.isEmpty()) {
            //释放实占
            BizObject currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, updateList.get(0).getMainid(), 3);
            ResultBudget resultBudgetDelActual =
                    cmpBudgetManagerService.gcExecuteTrueUnAudit(currentBill, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_AUDIT);
            if (!resultBudgetDelActual.isSuccess()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102163"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B4", "退票释放预算金额失败！") /* "退票释放预算金额失败！" */);
            }
            //释放预占
            resultBudgetDelActual =
                    cmpBudgetManagerService.gcExecuteBatchUnSubmit(currentBill, updateList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_SUBMIT);
            if (!resultBudgetDelActual.isSuccess()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102163"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B4", "退票释放预算金额失败！") /* "退票释放预算金额失败！" */);
            }
            updateList.stream().forEach(fundPayment_b -> {
                fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                fundPayment_b.setEntityStatus(EntityStatus.Update);
            });
        }
        //更新资金付款单关联数据，由于结算单将id合并，资金收付回写的时候不再更新流水和认领单id
//        updateFundPaymentRelationInfo(dataSettledDetail, billbs);
        for (FundPayment_b billb : billbs) {
            /*Map<String, Object> params = new HashMap<>();
            params.put("id", billb.getId());
            params.put("refundSum", billb.getRefundSum());
            params.put("settlestatus", billb.getFundSettlestatus().getValue());
            params.put("settleerrorSum", billb.getSettleerrorSum());
            params.put("settlesuccessSum", billb.getSettlesuccessSum());
            params.put("isToPushCspl", billb.getIsToPushCspl());
            params.put("isOccupyBudget", billb.getIsOccupyBudget());
            params.put("billClaimId", billb.getBillClaimId());
            params.put("bankReconciliationId", billb.getBankReconciliationId());
            params.put("associationStatus", billb.getAssociationStatus());
            params.put("tableName", "cmp_fundpayment_b");
            params.put("ytenantId", InvocationInfoProxy.getTenantid());
            log.error("updateRefundSettledInfoOfFundPayment params={}", params);
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
            FundPayment_b fundPaymentB = new FundPayment_b();
            fundPaymentB.setId(billb.getId());
            fundPaymentB.setRefundSum(billb.getRefundSum());
            fundPaymentB.setFundSettlestatus(billb.getFundSettlestatus());
            fundPaymentB.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPaymentB.getFundSettlestatus()));
            fundPaymentB.setSettleerrorSum(billb.getSettleerrorSum());
            fundPaymentB.setSettlesuccessSum(billb.getSettlesuccessSum());
            fundPaymentB.setIsToPushCspl(billb.getIsToPushCspl());
            fundPaymentB.setIsOccupyBudget(billb.getIsOccupyBudget());
            fundPaymentB.setBillClaimId(billb.getBillClaimId());
            fundPaymentB.setBankReconciliationId(billb.getBankReconciliationId());
            fundPaymentB.setAssociationStatus(billb.getAssociationStatus());
            fundPaymentB.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentB);
        }

        fundPaymentReleaseFundPlanProject(null, null, refundReleaseFundBillForFundPlanProjectList, partRefundReleaseFundBillForFundPlanProjectList);
    }

    @Override
    public void updateFundPaymentRelationInfo(DataSettledDetail dataSettledDetail, List<FundPayment_b> billbs) throws Exception {
        List<DataSettledDistribute> dataSettledDistributeList = dataSettledDetail.getDataSettledDistribute();
        if (CollectionUtils.isEmpty(dataSettledDistributeList)) {
            return;
        }
        List<String> bankIds = new ArrayList<>();
        List<String> billClaimIds = new ArrayList<>();
        for (DataSettledDistribute distribute : dataSettledDistributeList) {
            if (distribute.getRelateBankCheckBillId() != null) {
                bankIds.add(distribute.getRelateBankCheckBillId().toString());
            }
            if (distribute.getRelateClaimBillId() != null) {
                billClaimIds.add(distribute.getRelateClaimBillId().toString());
            }
        }
        //修改关联状态
        for (FundPayment_b fundPayment_b : billbs) {
            if (bankIds.size() > 0 || billClaimIds.size() > 0) {
                fundPayment_b.setAssociationStatus(AssociationStatus.Associated.getValue());
            }
            if (bankIds.size() > 0) {
                fundPayment_b.setBankReconciliationId(StringUtils.join(bankIds, ","));
            }
            if (billClaimIds.size() > 0) {
                fundPayment_b.setBillClaimId(StringUtils.join(billClaimIds, ","));
            }
            fundPayment_b.setEntityStatus(EntityStatus.Update);//修改操作
        }
    }

    @Override
    public void updateFundPaymentRelationInfoSimple(UnifiedSettleDetail unifiedSettleDetail, List<FundPayment_b> billbs) throws Exception {
        if (SettleDetailSettleStateEnum.SETTLE_SUCCESS.getValue() != unifiedSettleDetail.getStatementdetailstatus()
                && SettleDetailSettleStateEnum.PART_SUCCESS.getValue() != unifiedSettleDetail.getStatementdetailstatus()
                && SettleDetailSettleStateEnum.STOP_PAY.getValue() != unifiedSettleDetail.getStatementdetailstatus()) {
            return;
        }
        //修改关联状态
        for (FundPayment_b fundPayment_b : billbs) {
            if (!Objects.isNull(unifiedSettleDetail.getRelateClaimBillId())) {
                int relationType = reWriteBusRpcService.checkRelationType(unifiedSettleDetail.getRelateClaimBillId().toString());
                fundPayment_b.setAssociationStatus(AssociationStatus.Associated.getValue());
                if (relationType == 2) {
                    fundPayment_b.setBankReconciliationId(unifiedSettleDetail.getRelateClaimBillId().toString());
                }
                if (relationType == 1) {
                    fundPayment_b.setBillClaimId(unifiedSettleDetail.getRelateClaimBillId().toString());
                }
            }
            fundPayment_b.setEntityStatus(EntityStatus.Update);//修改操作
        }
    }

    @Override
    public void updateFundCollectionRelationInfo(DataSettledDetail dataSettledDetail, List<FundCollection_b> billbs) throws Exception {
        List<DataSettledDistribute> dataSettledDistributeList = dataSettledDetail.getDataSettledDistribute();
        if (CollectionUtils.isEmpty(dataSettledDistributeList)) {
            return;
        }
        //判断是否是已关联数据
//        if(dataSettledDistributeList.size() == 0 || !"1".equals(dataSettledDistributeList.get(0).getIsRelateCheckBill())){
//            return;
//        }
        List<String> bankIds = new ArrayList<>();
        List<String> billClaimIds = new ArrayList<>();
//        for (DataSettledDistribute distribute : dataSettledDistributeList) {
//            if (distribute.getRelateBankCheckBillId() != null) {
//                bankIds.add(distribute.getRelateBankCheckBillId().toString());
//            }
//            if (distribute.getRelateClaimBillId() != null) {
//                billClaimIds.add(distribute.getRelateClaimBillId().toString());
//            }
//        }
        List<BizObject> refundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partRefundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<FundCollection_b> updateList = new ArrayList();

        //修改关联状态
        for (FundCollection_b fundCollection_b : billbs) {
            // 回传实际结算汇率类型
            fundCollection_b.setActualSettlementExchangeRateType(dataSettledDetail.getExchangeRateType());
            // 回传实际结算金额
            BigDecimal actualSettlementAmount = ValueUtils.isNotEmptyObj(dataSettledDetail.getExchangePaymentAmount())
                    ? dataSettledDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
            fundCollection_b.setActualSettlementAmount(actualSettlementAmount.setScale(8, RoundingMode.HALF_UP));
            // 回传实际结算汇率
            fundCollection_b.setActualSettlementExchangeRate(dataSettledDetail.getExchangePaymentRate());
            //结算单拆分后，获取最大的时间进行业务单据结算成功时间赋值
            List<DataSettledDistribute> dataSettledDistributes = dataSettledDetail.getDataSettledDistribute();
            if (CollectionUtils.isNotEmpty(dataSettledDistributes)) {
                //结算单拆分后，获取最大的时间进行业务单据结算成功时间赋值，由获取结算时间改为获取结算日期
                Optional<DataSettledDistribute> dataSettled = dataSettledDistributes.stream().filter(p -> p != null && p.getSettleSuccBizTime() != null).max(Comparator.comparing(DataSettledDistribute::getSettleSuccBizTime));
                try {
                    if (dataSettled.isPresent() && !String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
                        fundCollection_b.setSettleSuccessTime(dataSettled.get().getSettleSuccBizTime());
                    }
                } catch (Exception e) {
                    log.error("get fund payment bill child settle success time fail! id={}, settleSuccessTime={}, message={}"
                            , fundCollection_b.getId(), dataSettled.get().getSettleSuccBizTime(), e.getMessage());
                }
            }

            if (bankIds.size() > 0 || billClaimIds.size() > 0) {
                fundCollection_b.setAssociationStatus(AssociationStatus.Associated.getValue());
            }
            if (bankIds.size() > 0) {
                fundCollection_b.setBankReconciliationId(StringUtils.join(bankIds, ","));
            }
            if (billClaimIds.size() > 0) {
                fundCollection_b.setBillClaimId(StringUtils.join(billClaimIds, ","));
            }
            Object isToPushCspl = fundCollection_b.get(ICmpConstant.IS_TO_PUSH_CSPL);
            if (ValueUtils.isNotEmptyObj(dataSettledDetail.getRefundAmt())
                    && BigDecimal.ZERO.compareTo(dataSettledDetail.getRefundAmt()) < 0
                    && ValueUtils.isNotEmptyObj(isToPushCspl)
                    && 1 == Integer.parseInt(isToPushCspl.toString())) {
                fundCollection_b.set("refundSum", dataSettledDetail.getRefundAmt());// 退票金额
                if (dataSettledDetail.getRefundAmt().compareTo(fundCollection_b.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                    refundReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                    fundCollection_b.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                } else {
                    partRefundReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                }

                // 退票释放预算
                Short budgeted = fundCollection_b.getIsOccupyBudget();
                if (budgeted != null && ((budgeted == OccupyBudget.ActualSuccess.getValue()))) {
                    updateList.add(fundCollection_b);
                    fundCollection_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    fundCollection_b.setEntityStatus(EntityStatus.Update);
                }
            }
            fundCollection_b.setEntityStatus(EntityStatus.Update);//修改操作
        }

//        for (FundCollection_b billb : billbs) {
//            /*Map<String, Object> params = new HashMap<>();
//            params.put("id", billb.getId());
//            params.put("settlestatus", billb.getFundSettlestatus().getValue());
//            params.put("isToPushCspl", billb.getIsToPushCspl());
//            params.put("settleerrorSum", billb.getSettleerrorSum());
//            params.put("settlesuccessSum", billb.getSettlesuccessSum());
//            params.put("isOccupyBudget", billb.getIsOccupyBudget());
//            params.put("billClaimId", billb.getBillClaimId());
//            params.put("bankReconciliationId", billb.getBankReconciliationId());
//            params.put("associationStatus", billb.getAssociationStatus());
//            params.put("tableName", "cmp_fundcollection_b");
//            params.put("ytenantId", InvocationInfoProxy.getTenantid());
//            log.error("updateFundCollectionRelationInfo params={}", params);
//            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
//
//            FundCollection_b fundCollection_b = new FundCollection_b();
//            fundCollection_b.setId(billb.getId());
//            fundCollection_b.setFundSettlestatus(billb.getFundSettlestatus());
//            fundCollection_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollection_b.getFundSettlestatus()));
//            fundCollection_b.setIsToPushCspl(billb.getIsToPushCspl());
//            fundCollection_b.setSettleerrorSum(billb.getSettleerrorSum());
//            fundCollection_b.setSettlesuccessSum(billb.getSettlesuccessSum());
//            fundCollection_b.setIsOccupyBudget(billb.getIsOccupyBudget());
//            fundCollection_b.setBillClaimId(billb.getBillClaimId());
//            fundCollection_b.setBankReconciliationId(billb.getBankReconciliationId());
//            fundCollection_b.setAssociationStatus(billb.getAssociationStatus());
//            fundCollection_b.setEntityStatus(EntityStatus.Update);
//            MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollection_b);
//        }

        if (!updateList.isEmpty()) {
            BizObject currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, updateList.get(0).getMainid(), 3);
            ResultBudget resultBudgetDelActual =
                    cmpBudgetManagerService.fundCollectionReleaseActualOccupySuccessUnAudit(currentBill, updateList,
                            FUND_COLLECTION, BillAction.CANCEL_AUDIT);
            if (!resultBudgetDelActual.isSuccess()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102163"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B4", "退票释放预算金额失败！") /* "退票释放预算金额失败！" */);
            }
        }
        fundCollectionReleaseFundPlanProject(null, null, refundReleaseFundBillForFundPlanProjectList, partRefundReleaseFundBillForFundPlanProjectList);
    }

    @Override
    public void updateFundCollectionRelationInfoSimple(UnifiedSettleDetail unifiedSettleDetail, List<FundCollection_b> billbs) throws Exception {
        if (SettleDetailSettleStateEnum.SETTLE_SUCCESS.getValue() != unifiedSettleDetail.getStatementdetailstatus()
                && SettleDetailSettleStateEnum.PART_SUCCESS.getValue() != unifiedSettleDetail.getStatementdetailstatus()
                && SettleDetailSettleStateEnum.STOP_PAY.getValue() != unifiedSettleDetail.getStatementdetailstatus()) {
            return;
        }

        List<BizObject> refundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partRefundReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<FundCollection_b> updateList = new ArrayList();

        //修改关联状态
        for (FundCollection_b fundCollection_b : billbs) {
            // 回传实际结算汇率类型
            fundCollection_b.setActualSettlementExchangeRateType(unifiedSettleDetail.getExchangeRateType());
            // 回传实际结算金额
            BigDecimal actualSettlementAmount = ValueUtils.isNotEmptyObj(unifiedSettleDetail.getExchangePaymentAmount())
                    ? unifiedSettleDetail.getExchangePaymentAmount() : BigDecimal.ZERO;
            fundCollection_b.setActualSettlementAmount(actualSettlementAmount.setScale(8, RoundingMode.HALF_UP));
            // 回传实际结算汇率
            fundCollection_b.setActualSettlementExchangeRate(unifiedSettleDetail.getExchangePaymentRate());
            //结算单拆分后，获取最大的时间进行业务单据结算成功时间赋值，由获取结算时间改为获取结算日期
            fundCollection_b.setSettleSuccessTime(unifiedSettleDetail.getOperationTime());
            if (!Objects.isNull(unifiedSettleDetail.getRelateClaimBillId())) {
                int relationType = reWriteBusRpcService.checkRelationType(unifiedSettleDetail.getRelateClaimBillId().toString());
                fundCollection_b.setAssociationStatus(AssociationStatus.Associated.getValue());
                if (relationType == 2) {
                    fundCollection_b.setBankReconciliationId(unifiedSettleDetail.getRelateClaimBillId().toString());
                }
                if (relationType == 1) {
                    fundCollection_b.setBillClaimId(unifiedSettleDetail.getRelateClaimBillId().toString());
                }
            }

            Object isToPushCspl = fundCollection_b.get(ICmpConstant.IS_TO_PUSH_CSPL);
            if (ValueUtils.isNotEmptyObj(unifiedSettleDetail.getRefundAmt())
                    && BigDecimal.ZERO.compareTo(unifiedSettleDetail.getRefundAmt()) < 0
                    && ValueUtils.isNotEmptyObj(isToPushCspl)
                    && 1 == Integer.parseInt(isToPushCspl.toString())) {
                fundCollection_b.set("refundSum", unifiedSettleDetail.getRefundAmt());// 退票金额
                if (unifiedSettleDetail.getRefundAmt().compareTo(fundCollection_b.getOriSum()) == 0) {//退票金额与交易金额相同，则结算状态为退票，否则为部分成功
                    refundReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                    fundCollection_b.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
                } else {
                    partRefundReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                }

                // 退票释放预算
                Short budgeted = fundCollection_b.getIsOccupyBudget();
                if (budgeted != null && ((budgeted == OccupyBudget.ActualSuccess.getValue()))) {
                    updateList.add(fundCollection_b);
                    fundCollection_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    fundCollection_b.setEntityStatus(EntityStatus.Update);
                }
            }
            fundCollection_b.setEntityStatus(EntityStatus.Update);//修改操作
        }

        for (FundCollection_b billb : billbs) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", billb.getId());
            params.put("settlestatus", billb.getFundSettlestatus().getValue());
            params.put("isToPushCspl", billb.getIsToPushCspl());
            params.put("settleerrorSum", billb.getSettleerrorSum());
            params.put("settlesuccessSum", billb.getSettlesuccessSum());
            params.put("isOccupyBudget", billb.getIsOccupyBudget());
            params.put("billClaimId", billb.getBillClaimId());
            params.put("bankReconciliationId", billb.getBankReconciliationId());
            params.put("associationStatus", billb.getAssociationStatus());
            params.put("tableName", "cmp_fundcollection_b");
            params.put("ytenantId", InvocationInfoProxy.getTenantid());
            log.error("updateFundCollectionRelationInfo params={}", params);
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);
        }

        if (!updateList.isEmpty()) {
            BizObject currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, updateList.get(0).getMainid(), 3);
            ResultBudget resultBudgetDelActual =
                    cmpBudgetManagerService.fundCollectionReleaseActualOccupySuccessUnAudit(currentBill, updateList,
                            FUND_COLLECTION, BillAction.CANCEL_AUDIT);
            if (!resultBudgetDelActual.isSuccess()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102163"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B4", "退票释放预算金额失败！") /* "退票释放预算金额失败！" */);
            }
        }
        fundCollectionReleaseFundPlanProject(null, null, refundReleaseFundBillForFundPlanProjectList, partRefundReleaseFundBillForFundPlanProjectList);
    }

    @Override
    public CtmJSONObject queryBillTypeIdByTradeTypeId(String tradeType) {
        CtmJSONObject result = new CtmJSONObject();
        try {
            String billTypeId = cmCommonService.catBillType(InvocationInfoProxy.getTenantid(), tradeType);
            result.put("billTypeId", billTypeId);
        } catch (Exception e) {
            log.error("通过交易类型获取单据类型异常：", e);
        }
        return result;
    }

    /**
     * <h2>查询统收统支关系组默认账户</h2>
     *
     * @param param : 子表id
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2023/11/25 15:13
     */
    @Override
    public CtmJSONObject queryIncomeAndExpenditureDefaultAccount(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        String id = param.getString("id");
        QuerySchema querySchema = QuerySchema.create().addSelect("marginaccount");
        querySchema.appendQueryCondition(QueryCondition.name("mainid").eq(id));
        querySchema.appendQueryCondition(QueryCondition.name("isDefault").eq(1));
        List<Map<String, Object>> query = MetaDaoHelper.query(ControlledMarginaccount.ENTITY_NAME, querySchema);
        if (CollectionUtils.isNotEmpty(query)) {
            Object marginaccount = query.get(0).get("marginaccount");
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setId(marginaccount.toString());
            List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
            if (!bankAccounts.isEmpty()) {
                result.put("incomeAndExpendBankAccount_account", bankAccounts.get(0).getAccount());
                result.put("incomeAndExpendBankAccount", bankAccounts.get(0).getId());
                result.put("enterprisebankaccount_name", bankAccounts.get(0).getName());
                result.put("enterprisebankaccount_code", bankAccounts.get(0).getCode());
                result.put("enterprisebankaccount_account", bankAccounts.get(0).getAccount());
                result.put("enterprisebankaccount", bankAccounts.get(0).getId());
            }
        }
        return result;
    }

    /**
     * <h2>查询预估汇率</h2>
     *
     * @param params : 入参集合
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2024/1/5 10:22
     */
    @Override
    public CtmJSONArray querySwapOutExchangeRate(CtmJSONArray params) throws Exception {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject jsonObject = params.getJSONObject(i);
            Integer index = jsonObject.getInteger("index");
            String currencyId = jsonObject.getString("currencyId");
            String exchangeRateType = jsonObject.getString("exchangeRateType");
            String natCurrencyId = jsonObject.getString("natCurrencyId");
            String vouchDate = jsonObject.getString("vouchDate");
            String value = currencyId +
                    "_" +
                    exchangeRateType +
                    "_" +
                    natCurrencyId +
                    "_" +
                    vouchDate;
            map.put(index, value);
        }
        Set<String> list = new HashSet<>();
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            list.add(entry.getValue());
        }
        Map<String, BigDecimal> dataReulst = new HashMap<>();
        Map<String, Short> rateOpsReulst = new HashMap<>();
        for (String arrays : list) {
            String[] arr = arrays.split("_");
            String currencyId = arr[0];
            String exchangeRateType = arr[1];
            String natCurrencyId = arr[2];
            Date vouchDate = DateUtils.strToDate(arr[3]);
            BigDecimal rate;
            Short exchangeRateOps;
            CmpExchangeRateVO cmpExchangeRateVO = new CmpExchangeRateVO();
            if (Objects.equals(currencyId, natCurrencyId)) {
                rate = new BigDecimal(1);
                exchangeRateOps = 1;
            } else {
                //20251112 跟明琴沟通，表体的换出汇率是 表头的原币币种->表体的结算币种
                BdRequestParams bdParams = new BdRequestParams();
                bdParams.setId(exchangeRateType);
                ExchangeRateTypeVO exchangeRateTypeVO = AppContext.getBean(IExchangeRateTypeService.class).queryByCondition(bdParams);
                if (!"02".equals(exchangeRateTypeVO.getCode())) {
                    cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currencyId, natCurrencyId, vouchDate, exchangeRateType);
                }
                rate = cmpExchangeRateVO.getExchangeRate();
                exchangeRateOps = cmpExchangeRateVO.getExchangeRateOps();
            }
            dataReulst.put(arrays, rate);
            rateOpsReulst.put(arrays, exchangeRateOps);
        }
        CtmJSONArray result = new CtmJSONArray();
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            CtmJSONObject object = new CtmJSONObject();
            Integer index = entry.getKey();
            String value = entry.getValue();
            BigDecimal rate = dataReulst.get(value);
            Short exchangeRateOps = rateOpsReulst.get(value);
            object.put("index", index);
            object.put("rate", rate);
            object.put("exchangeRateOps", exchangeRateOps);
            object.put("swapOutExchangeRateOps", exchangeRateOps);
            result.add(object);
        }
        return result;
    }

    @Override
    public CtmJSONArray updateUserId(String startCreateTime, String endCreateTime) throws Exception {
        //查询所有的业务单据
        if (StringUtils.isEmpty(startCreateTime) || StringUtils.isEmpty(endCreateTime)) {
            CtmJSONArray result = new CtmJSONArray();
            CtmJSONObject object = new CtmJSONObject();
            object.put("result", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540054D", "没有时间戳不执行！") /* "没有时间戳不执行！" */);
            result.add(object);
            return result;
        }
        //资金收款单
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("createDate").between(startCreateTime, endCreateTime));
        group.addCondition(QueryCondition.name("userId").is_null());
        querySchema.addCondition(group);
        List<FundCollection> fundCollectionList = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema, null);
        for (FundCollection fundCollection : fundCollectionList) {
            fundCollection.setEntityStatus(EntityStatus.Update);
            String userId = getYhtUserId(fundCollection.getCreatorId());
            if (StringUtils.isNotEmpty(userId)) {
                fundCollection.setUserId(userId);
            } else {
                fundCollection.setUserId(getYhtUserId(fundCollection.getAuditorId()));
            }
        }
        //资金付款单
        QuerySchema querySchemaPay = QuerySchema.create().addSelect("*");
        QueryConditionGroup groupPay = QueryConditionGroup.and(QueryCondition.name("createDate").between(startCreateTime, endCreateTime));
        groupPay.addCondition(QueryCondition.name("userId").is_null());
        querySchemaPay.addCondition(groupPay);
        List<FundPayment> fundPaymentList = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchemaPay, null);
        for (FundPayment fundPayment : fundPaymentList) {
            fundPayment.setEntityStatus(EntityStatus.Update);
            String userId = getYhtUserId(fundPayment.getCreatorId());
            if (StringUtils.isNotEmpty(userId)) {
                fundPayment.setUserId(userId);
            } else {
                fundPayment.setUserId(getYhtUserId(fundPayment.getAuditorId()));
            }
        }
        if (fundCollectionList != null) {
            MetaDaoHelper.update(FundCollection.ENTITY_NAME, fundCollectionList);
        }
        if (fundPaymentList != null) {
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPaymentList);
        }
        CtmJSONArray result = new CtmJSONArray();
        CtmJSONObject object = new CtmJSONObject();
        object.put("result", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400551", "执行成功！") /* "执行成功！" */);
        result.add(object);
        return result;
    }

    public String getYhtUserId(Long id) throws Exception {
        if (null == id) {
            return null;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("base.user.User");
        billContext.setDomain("iuap-apcom-bipuser");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("yhtUserId");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (null != query && query.get(0) != null && query.get(0).get("yhtUserId") != null) {
            return String.valueOf(query.get(0).get("yhtUserId"));
        }
        return null;
    }


    /**
     * 结算状态回调事件
     *
     * @param agentPaymentReqVO
     */
    public void sendEventToSettleBenchDetail(AgentPaymentReqVO agentPaymentReqVO) {
        if (org.imeta.core.lang.StringUtils.isEmpty(agentPaymentReqVO.getSettleDetailAId())) {
            return;
        }
        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
        // 事件源编码 ，事件类型编码，租户id
        businessEventBuilder.setSourceId("STWB");
        businessEventBuilder.setEventType("updateSettleBenchChild");
        businessEventBuilder.setBillId(agentPaymentReqVO.getSettleDetailAId());
        businessEventBuilder.setTenantCode(InvocationInfoProxy.getTenantid());
        // 事件体
        businessEventBuilder.setUserObject(agentPaymentReqVO);
        //如果需要租户默认token，tenantCode要传入yht租户id
        BusinessEvent businessEvent = businessEventBuilder.build();
        eventService.fireLocalEvent(businessEvent);
    }

    /*@Override
    public void checkHasSettlementBill(List<String> businessDetailsIdList) throws Exception {
        // 需求，支付、收到保证金已结算补单传结算时，支持上游撤回
        // 调用结算的查询接口，检查待结算数据是否已经生成结算单
        QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
        querySettledDetailModel.setWdataorigin(8); // 来源业务系统 8-现金管理
        querySettledDetailModel.setBusinessDetailsIds(businessDetailsIdList); // 业务单据明细ID集合

        try {
            List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);

            // 检查是否有待结算数据已生成结算单
            boolean hasSettlementGenerated = false;
            if (ObjectUtils.isNotEmpty(dataSettledDetailList)) {
                // 如果有待结算数据已生成结算单，则不允许撤回
                hasSettlementGenerated = true;
            }

            // 如果已生成结算单，则按原逻辑提示不允许撤回
            if (hasSettlementGenerated) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103038"),
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22703EBC04180005",
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22703EBC04180005", "已结算补单的单据推送结算未处理完成且凭证状态为待过账时,不能进行撤回！！") *//* "已结算补单的单据推送结算未处理完成且凭证状态为待过账时,不能进行撤回！！" *//*) *//* "已结算补单的单据推送结算未处理完成且凭证状态为待过账时,不能进行撤回！！" *//*);
            }
            // 如果未生成结算单，则允许继续执行撤回操作（不抛出异常）
            // 这里不需要额外处理，继续执行撤回流程
        } catch (Exception e) {
            log.error("查询待结算数据异常，按原逻辑处理", e);
            // 如果调用接口异常，默认按不能撤回处理
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103038"),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22703EBC04180005",
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22703EBC04180005", "已结算补单的单据推送结算未处理完成且凭证状态为待过账时,不能进行撤回！！") *//* "已结算补单的单据推送结算未处理完成且凭证状态为待过账时,不能进行撤回！！" *//*) *//* "已结算补单的单据推送结算未处理完成且凭证状态为待过账时,不能进行撤回！！" *//*);
        }
    }*/
}
