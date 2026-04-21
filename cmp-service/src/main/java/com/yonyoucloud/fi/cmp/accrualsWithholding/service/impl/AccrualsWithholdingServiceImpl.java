package com.yonyoucloud.fi.cmp.accrualsWithholding.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.ctp.ctm.finance.engine.model.domain.enums.FinModelCalcInterestDaysRuleEnum;
import com.yonyou.ctp.ctm.finance.engine.model.domain.enums.FinModelRuleInterestRateTypeEnum;
import com.yonyou.ctp.ctm.finance.engine.model.domain.enums.valuation.ratetable.CalculationMethodEnum;
import com.yonyou.ctp.ctm.finance.engine.model.domain.enums.valuation.ratetable.InterestRateBasisEnum;
import com.yonyou.ctp.ctm.finance.engine.model.valuation.req.ratetable.*;
import com.yonyou.ctp.ctm.finance.engine.valuation.instruments.RateTableCashFlowService;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.accrualsWithholding.service.AccrualsWithholdingService;
import com.yonyoucloud.fi.cmp.cmpentity.DailySettlementControl;
import com.yonyoucloud.fi.cmp.cmpentity.Relatedinterest;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.cmpentity.WithholdingRuleStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import com.yonyoucloud.fi.cmp.withholding.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.imeta.spring.support.id.IdManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_CMP_ACCRUALSWITHHOLDINGQUERY;


/**
 * @author shangxd
 * @date 2023/4/14 9:50
 * @describe
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class AccrualsWithholdingServiceImpl implements AccrualsWithholdingService {

    private static final int FAILIALCAPACITY = 1 << 5;//容量32
    private static final BigDecimal HUNDREDTH = new BigDecimal("0.01");//百分制处理使用
    private final BaseRefRpcService baseRefRpcService;
    private final IEnterpriseBankAcctService iEnterpriseBankAcctService;
    private final YmsOidGenerator ymsOidGenerator;
    private final CmpVoucherService cmpVoucherService;
    private final CmCommonService commonService;
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private final EnterpriseBankQueryService enterpriseBankQueryService;
    private final IApplicationService appService;
    private final RateTableCashFlowService rateTableCashFlowService;

    @Autowired
    public AccrualsWithholdingServiceImpl(RateTableCashFlowService rateTableCashFlowService,BaseRefRpcService baseRefRpcService,IEnterpriseBankAcctService iEnterpriseBankAcctService,YmsOidGenerator ymsOidGenerator,CmpVoucherService cmpVoucherService,CmCommonService commonService,CTMCMPBusinessLogService ctmcmpBusinessLogService,EnterpriseBankQueryService enterpriseBankQueryService,IApplicationService appService) {
        this.rateTableCashFlowService = rateTableCashFlowService;
        this.baseRefRpcService= baseRefRpcService;
        this.iEnterpriseBankAcctService= iEnterpriseBankAcctService;
        this.ymsOidGenerator= ymsOidGenerator;
        this.cmpVoucherService= cmpVoucherService;
        this.commonService= commonService;
        this.ctmcmpBusinessLogService= ctmcmpBusinessLogService;
        this.enterpriseBankQueryService= enterpriseBankQueryService;
        this.appService= appService;
    }

    private static final Cache<String, EnterpriseBankAcctVO> enterpriseBankAcctVOWithholdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    /**
     * 利息测算
     *
     * @param accrualsWithholdinges 列表数据
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public JsonNode calculate(List<AccrualsWithholding> accrualsWithholdinges) throws Exception {
        ObjectNode jsonObject = JSONBuilderUtils.createJson();
        //用于记录批量账户测算失败条数
        int failSize = 0;
        //用于记录批量测算失败的账户信息
        ObjectNode failed = JSONBuilderUtils.createJson();
        //用于记录批量账户测算失败原因
        List<String> messages = new ArrayList<>(FAILIALCAPACITY);
        int size = accrualsWithholdinges.size();
        List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>(accrualsWithholdinges.size());
        //遍历账户信息 进行校验与测算
        circulateProcess(accrualsWithholdinges, messages, failed, failSize,accrualsWithholdingList);
        failSize = messages.size();
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021E","共：%s张单据；%s张测算成功；%s张测算失败！") /* "共：%s张单据；%s张测算成功；%s张测算失败！" */, size,
                size - failSize, failSize);
        jsonObject.putPOJO("accrualsWithholdinges", accrualsWithholdingList);
        jsonObject.put("msg", message);
        jsonObject.putPOJO("msgs", messages);
        jsonObject.putPOJO("messages", messages);
        jsonObject.put("count", size);
        jsonObject.put("sucessCount", size - failSize);
        jsonObject.put("failCount", failSize);
        if (failed.size() > 0) {
            jsonObject.putPOJO("failed", failed);
        }
        return jsonObject;
    }

    /**
     * 计算逻辑
     *
     * @param accrualsWithholdinges
     * @param messages
     * @param failed
     * @param failSize
     */
    private void circulateProcess(List<AccrualsWithholding> accrualsWithholdinges, List<String> messages, ObjectNode failed, int failSize,
                                  List<AccrualsWithholding> accrualsWithholdingList) {
        AtomicBoolean isFirst = new AtomicBoolean(true);
        for (BizObject bizObject : accrualsWithholdinges) {
            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
            accrualsWithholding.init(bizObject);
            accrualsWithholdingList.add(accrualsWithholding);
            Long id = accrualsWithholding.getId();
            accrualsWithholding.setWithholdingRuleId(id);
            String key = "circulate" + id;
            String currency = accrualsWithholding.getCurrency();
            String account = accrualsWithholding.get(ICmpConstant.BANKACCOUNT_ACCOUNT);
            String currencyName = accrualsWithholding.get(ICmpConstant.CURRENCY_NAME);
            YmsLock ymsLock = null;
            try {
                if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(key))==null) {
                    failed.put(account, account);
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020D","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */+ account /* "该单据已锁定，请稍后重试！" */);
                    failSize++;
                    continue;
                }
                //校验是否为启用银行账户
                if(!getEnterpriseBankAcctVO( messages, failed, failSize, accrualsWithholding.getBankaccount(), account,currencyName)){
                    continue;
                }
                List<WithholdingRuleSetting> withholdingRuleSettingList = null;
                WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id, 2);
                if(Objects.isNull(withholdingRuleSetting)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100770"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180216","系统检测到预提规则已删除，请刷新页面重新测算。") /* "系统检测到预提规则已删除，请刷新页面重新测算。" */ );
                }
                CurrencyTenantDTO currencyDTO = findCurrency(currency);
                CurrencyTenantDTO natCurrencyDTO = findNatCurrency(accrualsWithholding.getNatCurrency());
                //校验
                if(!checkBill( messages,  failed,  failSize,  account,  currency,accrualsWithholding, currencyName,currencyDTO , natCurrencyDTO)){
                    continue;
                }
                if(!assemble( messages,  failed,  failSize,  account,accrualsWithholding, currencyName, id,withholdingRuleSettingList , withholdingRuleSetting, isFirst)){
                    continue;
                }
                //获取利率
                List<InterestRateSettingHistory> interestRateSettingHistoryList = getInterestRateSettingHistoryList(id);
                if(ValueUtils.isNotEmptyObj(withholdingRuleSetting.getVersion()) ){
                    accrualsWithholding.setSettingVersion( withholdingRuleSetting.getVersion().toString() );
                }else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100771"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180220","预提规则缺少版本号，为避免数据错误，调整预提规则设置后重新测算!") /* "预提规则缺少版本号，为避免数据错误，调整预提规则设置后重新测算!" */);
                }
                //获取协定利率变更历史记录
                List<AgreeIRSettingHistory> agreeSettingHistoryList = getAgreeSettingHistoryList(id);
                //计算利息与组装测算表数据
                createInterest(interestRateSettingHistoryList, withholdingRuleSetting, accrualsWithholding, currencyDTO, natCurrencyDTO,agreeSettingHistoryList);
            } catch (Exception e) {
                String message = e.getMessage();
                log.error("================AccrualsWithholdingService circulate :" + message, e);
                String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 300 ? message.substring(300) : message) : null;
                failed.put(account, account);
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180201","银行账号【%s】，币种为【%s】利率测算失败，【%s】") /* "银行账号【%s】，币种为【%s】利率测算失败，【%s】" */,
                        account, currencyName, resultMsg));
                failSize++;
                continue;
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }

        }
    }

    /**
     * 查询企业银行账户
     * @param messages
     * @param failed
     * @param failSize
     * @param bankAccount
     * @return
     * @throws Exception
     */
    private boolean getEnterpriseBankAcctVO(List<String> messages, ObjectNode failed, int failSize,
                                            String bankAccount, String account,String currencyName) throws Exception {
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccount);
        if (null != enterpriseBankAcctVO ) {
            Integer enterpriseBankAccountFlag = enterpriseBankAcctVO.getEnable();
            if (enterpriseBankAccountFlag != null  && (2 == enterpriseBankAccountFlag || 0 == enterpriseBankAccountFlag)) {// enable = 2 停用     0 是未启用    1是启用
                failed.put(account, account);
                messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1808B3BE04D0000A", "银行账号【%s】，币种【%s】测算失败，企业银行账户未启用或查询失败为避免数据错误，请刷新页面重新测算!") /* "银行账号【%s】，币种【%s】测算失败，企业银行账户未启用或查询失败为避免数据错误，请刷新页面重新测算!" */, bankAccount,currencyName));
                //failSize++;
                return false;
            }
        }else{
            failed.put(account, account);
            messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1808B3BE04D0000A", "银行账号【%s】，币种【%s】测算失败，企业银行账户未启用或查询失败为避免数据错误，请刷新页面重新测算!") /* "银行账号【%s】，币种【%s】测算失败，企业银行账户未启用或查询失败为避免数据错误，请刷新页面重新测算!" */, bankAccount,currencyName));
            //failSize++;
            return false;
        }
        return true;
    }

    /**
     * 校验
     * @param messages
     * @param failed
     * @param failSize
     * @param account
     * @param currency
     * @param accrualsWithholding
     * @param currencyName
     * @param currencyDTO
     * @param natCurrencyDTO
     * @return
     * @throws Exception
     */
    private boolean checkBill(List<String> messages, ObjectNode failed, int failSize, String account, String currency,
                              AccrualsWithholding accrualsWithholding, String currencyName,
                              CurrencyTenantDTO currencyDTO , CurrencyTenantDTO natCurrencyDTO) throws Exception {
        if (null == currencyDTO) {
            failed.put(account, account);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180223","没有查询到对应的企业银行账户【%s】的币种") /* "没有查询到对应的企业银行账户【%s】的币种" */, account));
            //failSize++;
            return false;
        }
        if (null == natCurrencyDTO) {
            failed.put(account, account);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180203","没有查询到对应的企业银行账户【%s】的本币币种") /* "没有查询到对应的企业银行账户【%s】的本币币种" */, account));
            //failSize++;
            return false;
        }
        //校验汇率是否为空
        if (!judgeExtrangeRate(accrualsWithholding)) {
            failed.put(account, account);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180207","银行账号【%s】，币种为【%s】利率测算失败，汇率不允许为空或者0！") /* "银行账号【%s】，币种为【%s】利率测算失败，汇率不允许为空或者0！" */,
                    account, currencyName));
            //failSize++;
            return false;
        }
        return true;
    }

    /**
     * 校验
     * @param messages
     * @param failed
     * @param failSize
     * @param account
     * @param accrualsWithholding
     * @param currencyName
     * @return
     * @throws Exception
     */
    private boolean assemble(List<String> messages, ObjectNode failed, int failSize, String account,
                             AccrualsWithholding accrualsWithholding, String currencyName, Long id,
                             List<WithholdingRuleSetting> withholdingRuleSettingList , WithholdingRuleSetting withholdingRuleSetting,AtomicBoolean isFirst) throws Exception {
        //校验是否已预提
        withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id, 2);
        if(null ==withholdingRuleSetting || withholdingRuleSetting.getRuleStatus().compareTo(WithholdingRuleStatus.Enable.getValue()) != 0){
            failed.put(account, account);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180208","系统检测到银行账号【%s】，币种【%s】预提规则有调整，为避免数据错误，请刷新页面后重新测算!") /* "系统检测到银行账号【%s】，币种【%s】预提规则有调整，为避免数据错误，请刷新页面后重新测算!" */ ,
                    account, currencyName));
            //failSize++;
            return false;
        }
        Date maxDate =null;
        try{
            maxDate = DateUtils.dateParse(DepositinterestWithholdingUtil.getMaxDate(withholdingRuleSetting.getLastInterestAccruedDate(), withholdingRuleSetting.getLastInterestSettlementDate(), null)
                    .toString(),null);
        } catch (Exception e){
            maxDate =DepositinterestWithholdingUtil.getMaxDate(withholdingRuleSetting.getLastInterestAccruedDate(), withholdingRuleSetting.getLastInterestSettlementDate(),null);
        }

        if ( accrualsWithholding.getLastInterestSettlementOrAccruedDate().compareTo(maxDate) != 0 ||
                maxDate.compareTo(accrualsWithholding.getAccruedStartDate()) > 0 ) {
            failed.put(account, account);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021A","银行账号【%s】，币种【%s】测算失败，系统检测到其他用户对该账号的利息进行了预提，为避免数据错误，请刷新页面重新测算！") /* "银行账号【%s】，币种【%s】测算失败，系统检测到其他用户对该账号的利息进行了预提，为避免数据错误，请刷新页面重新测算！" */ ,
                    account, currencyName));
            //failSize++;
            return false;
        }

        //日结控制
        if(isFirst.get() && !Objects.isNull(withholdingRuleSetting.getDailySettlementControl())
                && (withholdingRuleSetting.getDailySettlementControl() == DailySettlementControl.AccruaAfterSettlement.getValue())){
            //查询会计主体日结最大日期与预提日期 比较  如果小于预提日期  则所有日结控制为是的数据过滤掉
            Map<String, Object> checkSettleMap = checkSettle(accrualsWithholding.getAccentity());
            if(null != checkSettleMap){
                Date settleDate = DateUtils.dateParse(checkSettleMap.get("settlementdate").toString(),null);
                if(settleDate.compareTo(accrualsWithholding.getAccruedEndDate()) < 0){
                    //查询数据不查询日结控制规则为先日结后预提的数据
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100772"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1809A0E804D00014", "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面后重新测算！") /* "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面后重新测算！" */);
                }
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100772"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1809A0E804D00014", "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面后重新测算！") /* "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面后重新测算！" */);
            }
            isFirst.set(false);
        }

        //利息测算表 --- 校验是否存在预提数据
        List<InterestCalculation> interestCalculationList = getInterestCalculationList( accrualsWithholding, true);
        if (!CollectionUtils.isEmpty(interestCalculationList)) {
            failed.put(account, account);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021A","银行账号【%s】，币种【%s】测算失败，系统检测到其他用户对该账号的利息进行了预提，为避免数据错误，请刷新页面重新测算！") /* "银行账号【%s】，币种【%s】测算失败，系统检测到其他用户对该账号的利息进行了预提，为避免数据错误，请刷新页面重新测算！" */ ,
                    account, currencyName));
            //failSize++;
            return false;
        } else {
            MetaDaoHelper.delete(InterestCalculation.ENTITY_NAME, interestCalculationList);
        }
        return true;
    }

    /**
     * 计算利息与组装测算表数据
     */
    private void createInterest(List<InterestRateSettingHistory> interestRateSettingHistoryList, WithholdingRuleSetting withholdingRuleSetting,
                                AccrualsWithholding accrualsWithholding, CurrencyTenantDTO currencyDTO, CurrencyTenantDTO natCurrencyDTO,List<AgreeIRSettingHistory> agreeIRSettingHistories) throws Exception {
        //查询历史余额表 根据日期asc
        List<AccountRealtimeBalance> accountRealtimeBalanceList = getAccountRealtimeBalanceList(withholdingRuleSetting, accrualsWithholding);
        //协定历史记录不为空
        //需要进行协定计算的余额集合
        List<AccountRealtimeBalance> agreeAccBalancesAll = new ArrayList<>();
        //利率
        BigDecimal exchangerate = accrualsWithholding.getExchangerate();
        Short exchangerateOps = accrualsWithholding.getExchangerateOps();
        //协定存款利息测算
        BigDecimal depositAgreementCalculate =new BigDecimal(0);
        //合计利息 --活期存款利息+协定存款利息-活期透支利息
        BigDecimal calculateTotalAmount =new BigDecimal(0);

        Integer moneydigit = currencyDTO.getMoneydigit();
        Integer moneyrount = currencyDTO.getMoneyrount();
        Integer natMoneydigit = natCurrencyDTO.getMoneydigit();
        Integer natMoneyrount = natCurrencyDTO.getMoneyrount();
        if(CollectionUtils.isNotEmpty(agreeIRSettingHistories)){
            List<InterestCalculation> agreeInterestCalculationList = new ArrayList<>();
            for(AgreeIRSettingHistory agreeIRSettingHistory : agreeIRSettingHistories){
                //调用中台接口入参基类
                RateTableCalcInterestReq rateTableCalcInterestReq = new RateTableCalcInterestReq();
                List<AccountRealtimeBalance> agreeAccBalances = new ArrayList<>();

                Date startDate = agreeIRSettingHistory.getStartDate();
                Date endDate = agreeIRSettingHistory.getEndDate();

                for(AccountRealtimeBalance accountRealtimeBalance : accountRealtimeBalanceList ){
                    if(null !=endDate){
                        if(accountRealtimeBalance.getBalancedate().compareTo(startDate)>=0 &&accountRealtimeBalance.getBalancedate().compareTo(endDate)<=0){
                            agreeAccBalances.add(accountRealtimeBalance);
                        }
                    }else{
                        if(accountRealtimeBalance.getBalancedate().compareTo(startDate)>=0){
                            agreeAccBalances.add(accountRealtimeBalance);
                        }
                    }
                }
                agreeAccBalancesAll.addAll(agreeAccBalances);
                //组装调用技术中台接口入参
                if(CollectionUtils.isEmpty(agreeAccBalances)){
                   continue;
                }
                // 开始日期
                Date startDateAgree=agreeAccBalances.get(0).getBalancedate();
                Date endDateAgree=agreeAccBalances.get(agreeAccBalances.size()-1).getBalancedate();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDateAgree);
                calendar.add(Calendar.DATE, 1);
                endDateAgree=calendar.getTime(); // 结束日期为余额加一天再传入给接口
                rateTableCalcInterestReq.setStartDate(convertUtilDateToLocalDate(startDateAgree));
                rateTableCalcInterestReq.setEndDate(convertUtilDateToLocalDate(endDateAgree));
                // 结束日期
                //FinModelCalcInterestDaysRuleEnum.ACTUAL;
                String interestDays = agreeIRSettingHistory.getInterestDays();
                switch (interestDays){
                    case "360" :
                        rateTableCalcInterestReq.setCalcInterestDaysRuleEnum(FinModelCalcInterestDaysRuleEnum.STANDARD);
                        break;
                    case "365":
                        rateTableCalcInterestReq.setCalcInterestDaysRuleEnum(FinModelCalcInterestDaysRuleEnum.STANDARD_365);
                        break;
                    case "ACT_360":
                        rateTableCalcInterestReq.setCalcInterestDaysRuleEnum(FinModelCalcInterestDaysRuleEnum.ACTUAL_360);
                        break;
                    case "ACT_365":
                        rateTableCalcInterestReq.setCalcInterestDaysRuleEnum(FinModelCalcInterestDaysRuleEnum.ACTUAL_365);
                        break;
                    case "ACT_ACT":
                        rateTableCalcInterestReq.setCalcInterestDaysRuleEnum(FinModelCalcInterestDaysRuleEnum.ACTUAL);
                        break;
                    default:
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100773"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA69C2C04C00004","未支持的计息天数类型"));

                }
                // todo 利率确认基准(计息方式) Daily （枚举）
                // InterestRateBasisEnum.Daily;
                if(agreeIRSettingHistory.getAgreeinterestmethod()==2){
                    rateTableCalcInterestReq.setInterestRateBasisEnum(InterestRateBasisEnum.Periodic);
                }else{
                    rateTableCalcInterestReq.setInterestRateBasisEnum(InterestRateBasisEnum.Daily);
                }
                // 借方规则
                InterestRateCalcRule interestRateCalcRule = new InterestRateCalcRule();
                //计算方式
                // todo CalculationMethodEnum.Banded
                if(agreeIRSettingHistory.getAgreerelymethod()==1){
                    interestRateCalcRule.setCalculationMethodEnum(CalculationMethodEnum.Banded);
                }else if (agreeIRSettingHistory.getAgreerelymethod()==2){
                    interestRateCalcRule.setCalculationMethodEnum(CalculationMethodEnum.Stepped);
                }
                List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistories = agreeIRSettingHistory.agreeIRSettingGradeHistory();
                if (CollectionUtils.isEmpty(agreeIRSettingGradeHistories)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100774"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB6259804F00000","协定存款利率变更历史记录分档孙表不能为空"));
                }
                List<RateTierDTO> rateTierDTOS = new ArrayList<>();
                List<InterestRateDTO> interestRateDTOList = new ArrayList<>();
                //分档信息
                for(int j = 0; j < agreeIRSettingGradeHistories.size() ; j++){
                    AgreeIRSettingGradeHistory agreeIRSettingGradeHistory = agreeIRSettingGradeHistories.get(j);

                    RateTierDTO rateTierDTO = new RateTierDTO();
                    //分段编号
                    rateTierDTO.setTier(agreeIRSettingGradeHistory.getGradenum().intValue());
                    //当前是最后一个了,包括只有一条金额为0的情况
                    if(j == agreeIRSettingGradeHistories.size() - 1){
                        rateTierDTO.setToAmount(new BigDecimal(-1));
                        rateTierDTO.setInclusive(false);
                    }else{
                        //我们的分档配置是“0以上（包含）”、“100000以上”，翻译成对方的写法是“100000以下”、“正无穷以下”，所以需要用下一档的金额填充当前档
                        AgreeIRSettingGradeHistory nextAgreeIRSettingGradeHistorie = agreeIRSettingGradeHistories.get(j + 1);
                        rateTierDTO.setToAmount(nextAgreeIRSettingGradeHistorie.getAmount());
                        //下一档的分档选项取反，下一档是“以上（包含）”，当前档应是不包含
                        if("2".equals(nextAgreeIRSettingGradeHistorie.getGradeoption())){
                            rateTierDTO.setInclusive(false);
                        }else{
                            rateTierDTO.setInclusive(true);
                        }
                    }

                    //利率类型
                    switch (agreeIRSettingGradeHistory.getIrtype()){
                        case 1:
                            rateTierDTO.setRateType(FinModelRuleInterestRateTypeEnum.FIXED);
                            //固定利率（浮动利率的“基准利率”在 "###利率档案###" 填）
                            rateTierDTO.setInterestRate(ObjectUtils.isEmpty(agreeIRSettingGradeHistory.getInterestrate())? BigDecimal.ZERO:agreeIRSettingGradeHistory.getInterestrate());
                            break;
                        case 2:
                            rateTierDTO.setRateType(FinModelRuleInterestRateTypeEnum.FLOATING);
                            //实际是基准利率类型
                            rateTierDTO.setRateReference(agreeIRSettingGradeHistory.getBaseirtype().toString());
                            //浮动利率
                            rateTierDTO.setSpread(ObjectUtils.isEmpty(agreeIRSettingGradeHistory.getFloatvalue())?BigDecimal.ZERO:agreeIRSettingGradeHistory.getFloatvalue().divide(new BigDecimal(100)));
                            // 利率档案 参数 不再查询利率档案
                            List<InterestRateItem> interestRateItemList = new ArrayList<>();

                            InterestRateDTO interestRateDTO = new InterestRateDTO();
                            InterestRateItem interestRateItem = new InterestRateItem();
                            interestRateItem.setValueDate(convertUtilDateToLocalDate(agreeIRSettingHistory.getStartDate()));
                            interestRateItem.setReferenceRate(agreeIRSettingGradeHistory.getBaseir());
                            interestRateItemList.add(interestRateItem);

                            interestRateDTO.setRateReference(agreeIRSettingGradeHistory.getBaseirtype().toString());
                            interestRateDTO.setInterestRateItemList(interestRateItemList);
                            interestRateDTOList.add(interestRateDTO);
                            break;
                    }

                    rateTierDTOS.add(rateTierDTO);
                }
                if(!CollectionUtils.isEmpty(interestRateDTOList)){
                    rateTableCalcInterestReq.setInterestRateDTOList(interestRateDTOList);
                }
                interestRateCalcRule.setRateTierDTOList(rateTierDTOS);
                rateTableCalcInterestReq.setDebitInterestRateCalcRule(interestRateCalcRule);

                // 余额计划
                if(CollectionUtils.isEmpty(agreeAccBalances)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100775"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA69D6E04780007","协定存款计算利率余额为空"));

                }
                List<ScheduledBalance> scheduledBalanceList = new ArrayList<>();
                for(AccountRealtimeBalance accountRealtimeBalance : agreeAccBalances){
                    ScheduledBalance scheduledBalance = new ScheduledBalance();
                    scheduledBalance.setDate(convertUtilDateToLocalDate(accountRealtimeBalance.getBalancedate()));
                    if(accountRealtimeBalance.getAcctbal() != null && accountRealtimeBalance.getAcctbal().compareTo(BigDecimal.ZERO) < 0){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105050"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21F1A8F004A80006","协定计息区间存在账户余额为负数的记录，不支持协定预提测算"));
                    }
                    scheduledBalance.setBalance(accountRealtimeBalance.getAcctbal());
                    scheduledBalanceList.add(scheduledBalance);
                }
                rateTableCalcInterestReq.setScheduledBalanceList(scheduledBalanceList);
                if(log.isInfoEnabled()){
                    log.info("createInterest-request-rateTableCalcInterestReq={}",JsonUtils.toJSON(rateTableCalcInterestReq));
                }
                //调用金融中台接口
                RateTableCalcInterestResp rateTableCalcInterestResp = (RateTableCalcInterestResp) rateTableCashFlowService.execute(rateTableCalcInterestReq);
                if(log.isInfoEnabled()){
                    log.info("createInterest-request-rateTableCalcInterestResp={}",JsonUtils.toJSON(rateTableCalcInterestResp));
                }
                rateTableCalcInterestResp.getCreditInterest();
                rateTableCalcInterestResp.getDebitInterest();
                List<DiaryItem> interestRecordList = rateTableCalcInterestResp.getInterestRecordList();
                for(DiaryItem diaryItem :interestRecordList ){
                    InterestCalculation interestCalculation = new InterestCalculation();
                    interestCalculation.setMainid(withholdingRuleSetting.getId());
                    interestCalculation.setInterestDate(convertLocalDateToDate(diaryItem.getValueDate()));
                    if(null==diaryItem.getAccrualDate()){
                        interestCalculation.setEndDate(convertLocalDateToDate(diaryItem.getValueDate().plusDays(1)));
                    }else{
                        interestCalculation.setEndDate(convertLocalDateToDate(diaryItem.getAccrualDate()));
                    }
                    interestCalculation.setDays(diaryItem.getAccrualDays().toString());
                    interestCalculation.setInterestDays(diaryItem.getDaysInYear().toString());
                    // 计息方式
                    interestCalculation.setAgreeinterestmethod(agreeIRSettingHistory.getAgreeinterestmethod());
                    // 1 即 计息方式 是逐日的时候
                    if(agreeIRSettingHistory.getAgreeinterestmethod()==1) {
                        interestCalculation.setDepositbal(diaryItem.getBalance().setScale(moneydigit,moneyrount));
                    }else{
                        interestCalculation.setDepositbalavg(diaryItem.getBalance().setScale(moneydigit,moneyrount));
                    }
                    interestCalculation.setGradenum(null == diaryItem.getTier()? 1: Long.valueOf(diaryItem.getTier()));
                    interestCalculation.setPrincipal(diaryItem.getReferenceAmount().setScale(moneydigit,moneyrount));
                    interestCalculation.setBaseir(diaryItem.getReferenceRate());
                    interestCalculation.setFloatvalue(null==diaryItem.getSpreadRate()?null:diaryItem.getSpreadRate());
                    interestCalculation.setAnnualInterestRate(diaryItem.getTotalRate());
                    interestCalculation.setDepositInterest(diaryItem.getAccrualInterest());
                    interestCalculation.setIsCaculate(true);
                    interestCalculation.setId(IdManager.getInstance().nextId());
                    interestCalculation.setEntityStatus(EntityStatus.Insert);
                    agreeInterestCalculationList.add(interestCalculation);
                    depositAgreementCalculate=depositAgreementCalculate.add(diaryItem.getAccrualInterest());
                }

            }
            if(!CollectionUtils.isEmpty(agreeInterestCalculationList)) {
                List<InterestCalculation> agreeInterestCalculationOldList = getAgreementInterestCalculationList(accrualsWithholding, false);
                MetaDaoHelper.delete(InterestCalculation.ENTITY_NAME, agreeInterestCalculationOldList);
                CmpMetaDaoHelper.insert(InterestCalculation.ENTITY_NAME, agreeInterestCalculationList);
            }
        }
        // 协定测算
        accrualsWithholding.setDepositAgreementCalculate(depositAgreementCalculate.setScale(moneydigit, moneyrount));
        // 协定测算 本币
        accrualsWithholding.setCurrencyDepositAgreementCalculate(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangerateOps, exchangerate, depositAgreementCalculate, null).setScale(natMoneydigit, natMoneyrount));

        //  去掉协定的测算 剩下的就只有活期的
        accountRealtimeBalanceList.removeAll(agreeAccBalancesAll);
        // 只针对无协定的校验
        if(CollectionUtils.isEmpty(agreeAccBalancesAll)) {
            int includeTodayNum = DateUtils.dateBetweenIncludeToday(accrualsWithholding.getAccruedStartDate(), accrualsWithholding.getAccruedEndDate());
            int realSize = CollectionUtils.isEmpty(accountRealtimeBalanceList) ? 0 : accountRealtimeBalanceList.size();

            if (realSize != includeTodayNum) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100776"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180200", "系统检测到该账号历史余额数据不全，为避免数据错误，请补足账户历史余额数据后测算并预提!") /* "系统检测到该账号历史余额数据不全，为避免数据错误，请补足账户历史余额数据后测算并预提!" */);
            }
        }
        // 合计 加上协定
        calculateTotalAmount =calculateTotalAmount.add(depositAgreementCalculate);
        // 优先协定的 去掉协定的后 如果没有就不算
        if(CollectionUtils.isEmpty(accountRealtimeBalanceList)){
            //合计利息 无活期只有协定
            accrualsWithholding.setTotalInterest(calculateTotalAmount.setScale(moneydigit, moneyrount));
            // 合计利息本币
            accrualsWithholding.setCurrencyTotalInterest(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangerateOps, exchangerate, calculateTotalAmount, null).setScale(natMoneydigit, natMoneyrount));
            return;
        }

        List<InterestCalculation> interestCalculationList = new ArrayList<>();
        //组装测算表数据
        Map<String, Object> result = createInterestCalculation(accountRealtimeBalanceList, interestRateSettingHistoryList,
                withholdingRuleSetting, accrualsWithholding, currencyDTO, interestCalculationList);

        calculateTotalAmount= calculateTotalAmount.add((BigDecimal) result.get("calculateTotalAmount"));
        BigDecimal calculateOverdraftAmount = (BigDecimal) result.get("calculateOverdraftAmount");
        BigDecimal calculateDepositAmounnt = (BigDecimal) result.get("calculateDepositAmounnt");

        //存款利息测
        accrualsWithholding.setDepositInterestCalculate(calculateDepositAmounnt.setScale(moneydigit, moneyrount));
        //透支利息测算
        accrualsWithholding.setOverdraftInterestCalculate(calculateOverdraftAmount.setScale(moneydigit, moneyrount));
        //合计利息
        accrualsWithholding.setTotalInterest(calculateTotalAmount.setScale(moneydigit, moneyrount));
        //存款利息测算（本币）
        accrualsWithholding.setCurrencyDepositInterestCalculate(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangerateOps, exchangerate, calculateDepositAmounnt, null)
                .setScale(natMoneydigit, natMoneyrount));
        //透支利息测算（本币）
        accrualsWithholding.setCurrencyOverdraftInterestCalculate(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangerateOps, exchangerate, calculateOverdraftAmount, null).setScale(natMoneydigit, natMoneyrount));
        //合计利息（本币）
        accrualsWithholding.setCurrencyTotalInterest(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangerateOps, exchangerate, calculateTotalAmount, null)
                .setScale(natMoneydigit, natMoneyrount));
        List<InterestCalculation> interestCalculationOldList = getInterestCalculationList( accrualsWithholding, false);
        MetaDaoHelper.delete(InterestCalculation.ENTITY_NAME, interestCalculationOldList);
        //插入测算表数据
        CmpMetaDaoHelper.insert(InterestCalculation.ENTITY_NAME, interestCalculationList);
    }
    public static Date convertLocalDateToDate(LocalDate localDate) {
        if (localDate == null) {
            throw new IllegalArgumentException("LocalDate cannot be null");
        }
        return DateUtil.localDate2Date(localDate);
    }
    private static LocalDate convertUtilDateToLocalDate(Date utilDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(utilDate);
        return LocalDate.of(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
    /**
     * 获取存款利率档案列表
     * @param irtypes
     */
    private List<DepositInterestRate> getDepositInterestRates(List<Long> irtypes, Date startDate, Date endDate) throws Exception {
        QuerySchema schemaHistory = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroupHistory = new QueryConditionGroup(ConditionOperator.and);
        //余额日期
        conditionGroupHistory.appendCondition(QueryCondition.name("rateDate").egt(startDate));
        conditionGroupHistory.appendCondition(QueryCondition.name("rateDate").elt(endDate));
        conditionGroupHistory.appendCondition(QueryCondition.name("rateType").in(irtypes));
        schemaHistory.addCondition(conditionGroupHistory);
        List <BizObject> depositInterestRateList = MetaDaoHelper.queryObject(DepositInterestRate.ENTITY_NAME, schemaHistory, "yonbip-fi-ctmtlm");
        List<DepositInterestRate> listDepositInterestRate = new ArrayList<>();
        if(!org.apache.commons.collections4.CollectionUtils.isEmpty(depositInterestRateList)) {
            for (BizObject biz : depositInterestRateList) {
                DepositInterestRate depositInterestRate = new DepositInterestRate();
                depositInterestRate.setRate(biz.get("rate"));
                depositInterestRate.setRateDate(biz.get("rateDate"));
                depositInterestRate.setRateType(biz.get("rateType"));
                listDepositInterestRate.add(depositInterestRate);
            }
        }

        return listDepositInterestRate;
    }

    /**
     * 组装测算表数据
     *
     * @param accountRealtimeBalanceList
     * @param interestRateSettingHistoryList
     * @param withholdingRuleSetting
     * @param accrualsWithholding
     * @param currencyDTO
     * @param interestCalculationList
     */
    private Map createInterestCalculation(List<AccountRealtimeBalance> accountRealtimeBalanceList, List<InterestRateSettingHistory> interestRateSettingHistoryList,
                                          WithholdingRuleSetting withholdingRuleSetting, AccrualsWithholding accrualsWithholding, CurrencyTenantDTO currencyDTO,
                                          List<InterestCalculation> interestCalculationList) {
        //计算合计总利息
        BigDecimal calculateTotalAmount = BigDecimal.ZERO;
        //透支总利息一直累计
        BigDecimal calculateOverdraftAmount = BigDecimal.ZERO;
        //存款总利息一直累计
        BigDecimal calculateDepositAmounnt = BigDecimal.ZERO;
        Map<String, Object> resultMap = new HashMap();
        //阶段性存款计息积数
        BigDecimal interestBaseAmount = BigDecimal.ZERO;
        //阶段性透支计息积数
        BigDecimal interestOverdraftBaseAmount = BigDecimal.ZERO;
        //遍历历史余额表  组装利息测算表数据 并测算
        for (int i = 0; i < accountRealtimeBalanceList.size(); i++) {
            AccountRealtimeBalance accountRealtimeBalance = accountRealtimeBalanceList.get(i);
            InterestCalculation interestCalculation = new InterestCalculation();
            InterestRateSettingHistory interestRateSettingHistory = binarySearches(interestRateSettingHistoryList,
                    accountRealtimeBalance.getBalancedate(), 0, interestRateSettingHistoryList.size()-1);
            if(null == interestRateSettingHistory){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100777"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020B","利率未设置，请检查后在做测算！") /* "利率未设置，请检查后在做测算！" */);
            }
            interestCalculation.setMainid(withholdingRuleSetting.getId());
            interestCalculation.setBalanceid(accountRealtimeBalance.getId());
            interestCalculation.setCreateDate(new Date());
            interestCalculation.setCreateTime(new Date());
            interestCalculation.setCreatorId(AppContext.getCurrentUser().getId());
            interestCalculation.setCreator(AppContext.getCurrentUser().getName());
            interestCalculation.setTenant(AppContext.getTenantId());
            interestCalculation.setIsWithholding(false);
            interestCalculation.setCurrency(accountRealtimeBalance.getCurrency());
            //计息天数
            interestCalculation.setInterestDays(String.valueOf(interestRateSettingHistory.getInterestDays()));
            //日期
            interestCalculation.setInterestDate(accountRealtimeBalance.getBalancedate());
            //存款利率(年)
            interestCalculation.setAnnualInterestRate(interestRateSettingHistory.getInterestRate());
            //透支利率(年)
            interestCalculation.setOverdraftRate(interestRateSettingHistory.getOverdraftRate());

            //账户余额depositbalance
            BigDecimal acctbal = accountRealtimeBalance.getDepositbalance() == null ? BigDecimal.ZERO : accountRealtimeBalance.getDepositbalance();
            //透支余额 -----------------overdraftbalance
            BigDecimal overdraftbalance = accountRealtimeBalance.getOverdraftbalance() == null ? BigDecimal.ZERO : accountRealtimeBalance.getOverdraftbalance();

            //计算计息积数，计息积数一直累计
            interestBaseAmount = interestBaseAmount.add(acctbal);
            //透支计息积数 -----------------??????????????????
            interestOverdraftBaseAmount = interestOverdraftBaseAmount.add(overdraftbalance);

            //当日存款余额
            interestCalculation.setDepositbal(acctbal);
            //当日透支余额
            interestCalculation.setOverdraftbal(overdraftbalance);

            //如果不是最后一条数据
            if (i + 1 < accountRealtimeBalanceList.size()) {
                AccountRealtimeBalance accountRealtimeBalanceNext = accountRealtimeBalanceList.get(i+1);
                InterestRateSettingHistory interestRateSettingHistoryNext = binarySearches(interestRateSettingHistoryList,
                        accountRealtimeBalanceNext.getBalancedate(), 0, interestRateSettingHistoryList.size()-1);
                if(null == interestRateSettingHistoryNext){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100777"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020B","利率未设置，请检查后在做测算！") /* "利率未设置，请检查后在做测算！" */);
                }
                //利率没有发送变化
                BigDecimal interestRate = interestRateSettingHistory.getInterestRate() == null ? BigDecimal.ZERO : interestRateSettingHistory.getInterestRate();
                BigDecimal interestRateNext = interestRateSettingHistoryNext.getInterestRate() == null ? BigDecimal.ZERO : interestRateSettingHistoryNext.getInterestRate();
                BigDecimal overdraftRate = interestRateSettingHistory.getOverdraftRate() == null ? BigDecimal.ZERO : interestRateSettingHistory.getOverdraftRate();
                BigDecimal overdraftRateNext = interestRateSettingHistoryNext.getOverdraftRate() == null ? BigDecimal.ZERO : interestRateSettingHistoryNext.getOverdraftRate();
                if (interestRate.compareTo(interestRateNext) == 0 && overdraftRate.compareTo(overdraftRateNext) == 0) {
                    createInterestCalculation(interestCalculation);
                } else {
                    createInterestAccumulation(interestCalculation,
                            interestBaseAmount, interestOverdraftBaseAmount,
                            accrualsWithholding, currencyDTO);
                    //活期存款总利息一直累计
                    calculateDepositAmounnt = calculateDepositAmounnt.add(interestCalculation.getDepositInterest());
                    //活期透支总利息一直累计
                    calculateOverdraftAmount = calculateOverdraftAmount.add(interestCalculation.getOverdraftInterest());
                    //计算合计总利息
                    calculateTotalAmount = calculateDepositAmounnt.subtract(calculateOverdraftAmount);
                    //重置活期计息积数
                    interestBaseAmount = BigDecimal.ZERO;
                    //重置活期透支计息基数
                    interestOverdraftBaseAmount = BigDecimal.ZERO;
                }
            } else {
                createInterestAccumulation(interestCalculation,
                        interestBaseAmount, interestOverdraftBaseAmount,
                        accrualsWithholding, currencyDTO);
                //活期存款总利息一直累计
                calculateDepositAmounnt = calculateDepositAmounnt.add(interestCalculation.getDepositInterest());
                //活期透支总利息一直累计
                calculateOverdraftAmount = calculateOverdraftAmount.add(interestCalculation.getOverdraftInterest());
                //计算合计总利息
                calculateTotalAmount = calculateDepositAmounnt.subtract(calculateOverdraftAmount);
            }
            interestCalculation.setEntityStatus(EntityStatus.Insert);
            interestCalculation.setId(ymsOidGenerator.nextId());
            interestCalculationList.add(interestCalculation);
        }
        resultMap.put("calculateTotalAmount",calculateTotalAmount);
        resultMap.put("calculateOverdraftAmount",calculateOverdraftAmount);
        resultMap.put("calculateDepositAmounnt",calculateDepositAmounnt);
        resultMap.put("interestCalculationList",interestCalculationList);
        return resultMap;
    }

    /**
     * 查询银行账户历史余额
     *
     * @param withholdingRuleSetting
     * @return
     * @throws Exception
     */
    private List<AccountRealtimeBalance> getAccountRealtimeBalanceList(WithholdingRuleSetting withholdingRuleSetting, AccrualsWithholding accrualsWithholding) throws Exception {
        QuerySchema schemaHistory = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroupHistory = new QueryConditionGroup(ConditionOperator.and);
        conditionGroupHistory.appendCondition(QueryCondition.name("enterpriseBankAccount").in(withholdingRuleSetting.getBankaccount()));
        conditionGroupHistory.appendCondition(QueryCondition.name("currency").in(withholdingRuleSetting.getCurrency()));
        //余额日期
        conditionGroupHistory.appendCondition(QueryCondition.name("balancedate").egt(accrualsWithholding.getAccruedStartDate()));
        conditionGroupHistory.appendCondition(QueryCondition.name("balancedate").elt(accrualsWithholding.getAccruedEndDate()));
        conditionGroupHistory.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schemaHistory.addCondition(conditionGroupHistory);
        //余额日期 升序
        schemaHistory.addOrderBy(new QueryOrderby("balancedate", "asc"));
        return MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schemaHistory, null);
    }

    /**
     * 不是最后一条数据赋值为空
     *
     * @param interestCalculation
     */
    private void createInterestCalculation(InterestCalculation interestCalculation) {
        //存款计息积数
        interestCalculation.setInterestacc(BigDecimal.ZERO);
        //存款利息
        interestCalculation.setDepositInterest(BigDecimal.ZERO);
        //透支计息积数
        interestCalculation.setOverdraftacc(BigDecimal.ZERO);
        //透支利息
        interestCalculation.setOverdraftInterest(BigDecimal.ZERO);
        //合计利息
        interestCalculation.setTotalInterestAmount(BigDecimal.ZERO);
    }

    /**
     * 阶段性计算赋值
     *
     * @param interestCalculation
     */
    private void createInterestAccumulation(InterestCalculation interestCalculation,
                                            BigDecimal interestBaseAmount, BigDecimal interestOverdraftBaseAmount,
                                            AccrualsWithholding accrualsWithholding, CurrencyTenantDTO currencyDTO) {
        //存款计息积数
        interestCalculation.setInterestacc(interestBaseAmount);
        //存款利息
        BigDecimal interestAmount = getInterest(interestCalculation, interestBaseAmount, currencyDTO);
        interestCalculation.setDepositInterest(interestAmount);
        //透支计息积数
        interestCalculation.setOverdraftacc(interestOverdraftBaseAmount);
        //透支利息
        BigDecimal overdraftInterest = getOverdraftInterest(interestCalculation, interestOverdraftBaseAmount, currencyDTO);
        interestCalculation.setOverdraftInterest(overdraftInterest);
        //合计利息
        interestCalculation.setTotalInterestAmount(interestAmount.subtract(overdraftInterest));
    }

    /**
     * 获取活期透支利息
     *
     * @param interestCalculation
     * @param interestOverdraftBaseAmount
     * @param currencyDTO
     * @return
     */
    private BigDecimal getOverdraftInterest(InterestCalculation interestCalculation, BigDecimal interestOverdraftBaseAmount,
                                            CurrencyTenantDTO currencyDTO) {
        //透支利率(年)
        BigDecimal overdraftRate = interestCalculation.getOverdraftRate().multiply(HUNDREDTH);
        //计息天数
        String interestDays = interestCalculation.getInterestDays();
        Integer moneyRunt = currencyDTO.getMoneyrount();
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        switch (moneyRunt) {
            case 0:
                roundingMode = RoundingMode.UP;
                break;
            case 1:
                roundingMode = RoundingMode.DOWN;
                break;
            default:
                break;
        }
        //计息积数*存款利率/计息天数
        return interestOverdraftBaseAmount.multiply(overdraftRate).divide(new BigDecimal(interestDays),6, roundingMode);
    }

    /**
     * 获取活期存款利息
     *
     * @param interestCalculation
     * @param interestBaseAmount
     * @param currencyDTO
     * @return
     */
    private BigDecimal getInterest(InterestCalculation interestCalculation, BigDecimal interestBaseAmount, CurrencyTenantDTO currencyDTO) {
        //存款利率(年)
        BigDecimal annualInterestRate = interestCalculation.getAnnualInterestRate().multiply(HUNDREDTH);
        //计息天数
        String interestDays = interestCalculation.getInterestDays();
        Integer moneyRunt = currencyDTO.getMoneyrount();
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        switch (moneyRunt) {
            case 0:
                roundingMode = RoundingMode.UP;
                break;
            case 1:
                roundingMode = RoundingMode.DOWN;
                break;
            default:
                break;
        }
        //计息积数*存款利率/计息天数
        return interestBaseAmount.multiply(annualInterestRate).divide(new BigDecimal(interestDays),6, roundingMode);
    }

    /**
     * 二分查找
     *
     * @param interestRateSettingHistoryList
     * @param target
     * @param left
     * @param right
     * @return
     */
    private InterestRateSettingHistory binarySearches(List<InterestRateSettingHistory> interestRateSettingHistoryList, Date target, int left, int right) {
        if (left > right) {
            return null;
        }
        int mid = left + (right - left) / 2;
        InterestRateSettingHistory interestRateSettingHistoryResult = interestRateSettingHistoryList.get(mid);
        if (target.before(interestRateSettingHistoryResult.getStartDate())) {
            return binarySearches(interestRateSettingHistoryList, target, left, mid - 1);
        } else if (target.after(interestRateSettingHistoryResult.getEndDate())) {
            return binarySearches(interestRateSettingHistoryList, target, mid + 1, right);
        } else {
            return interestRateSettingHistoryResult;
        }
    }

    /**
     * 判断汇率是否为空
     *
     * @param accrualsWithholding
     * @return
     */
    private boolean judgeExtrangeRate(AccrualsWithholding accrualsWithholding) {
        if (accrualsWithholding.getExchangerate() == null || BigDecimal.ZERO.compareTo(accrualsWithholding.getExchangerate()) == 0) {
            return false;
        }
        return true;
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
        String currencyKey = currency.concat(InvocationInfoProxy.getTenantid()).concat(ICmpConstant.CURRENCY);
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
     * 查询本币币种
     *
     * @param natCurrency
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO findNatCurrency(String natCurrency) throws Exception {
        CurrencyTenantDTO currencyNatDTO = null;
        String currencyKey = natCurrency.concat(InvocationInfoProxy.getTenantid()).concat(ICmpConstant.NATCURRENCY);
        if (null != AppContext.cache().getObject(currencyKey)) {
            currencyNatDTO = AppContext.cache().getObject(currencyKey);
        } else {
            currencyNatDTO = baseRefRpcService.queryCurrencyById(natCurrency);
            if (null != currencyNatDTO) {
                AppContext.cache().setObject(currencyKey, currencyNatDTO);
            }
        }
        return currencyNatDTO;
    }

    /**
     * 预提
     *
     * @param accrualsWithholdinges 列表数据
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public JsonNode tWithholding(List<AccrualsWithholding> accrualsWithholdinges) throws Exception {
        ObjectNode jsonObject = JSONBuilderUtils.createJson();
        //用于记录批量账户测算失败条数
        int failSize = 0;
        //用于记录批量测算失败的账户信息
        ObjectNode failed = JSONBuilderUtils.createJson();
        //用于记录批量账户测算失败原因
        List<String> messages = new ArrayList<>(FAILIALCAPACITY);
        int size = accrualsWithholdinges.size();
        //遍历账户信息 进行校验与测算
        tWithholdingProcess(accrualsWithholdinges, messages, failed, failSize);
        failSize = messages.size();
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180211","共：%s张单据；%s张预提成功；%s张预提失败！") /* "共：%s张单据；%s张预提成功；%s张预提失败！" */, size,
                size - failSize, failSize);
        jsonObject.put("msg", message);
        jsonObject.putPOJO("msgs", messages);
        jsonObject.putPOJO("messages", messages);
        jsonObject.put("count", size);
        jsonObject.put("sucessCount", size - failSize);
        jsonObject.put("failCount", failSize);
        if (failed.size() > 0) {
            jsonObject.putPOJO("failed", failed);
        }
        return jsonObject;
    }

    private void tWithholdingProcess(List<AccrualsWithholding> accrualsWithholdinges, List<String> messages, ObjectNode failed, int failSize) {
        AtomicBoolean isFirst = new AtomicBoolean(true);
        for (BizObject bizObject : accrualsWithholdinges) {
            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
            accrualsWithholding.init(bizObject);
            Long id = accrualsWithholding.getId();
            accrualsWithholding.setLogicMsg(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180219","查看") /* "查看" */);
            //同一条规则预提与测试同时进行？？？？
            String key = "tWithholdingProcess" + id;
            accrualsWithholding.setWithholdingRuleId(id);
            String currencyName = accrualsWithholding.get(ICmpConstant.CURRENCY_NAME);
            String bankaccountAccount = accrualsWithholding.get(ICmpConstant.BANKACCOUNT_ACCOUNT);
            YmsLock ymsLock = null;
            try {
                if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(key))==null) {
                    failed.put(bankaccountAccount, bankaccountAccount);
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020D","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */+ bankaccountAccount /* "该单据已锁定，请稍后重试！" */);
                    failSize++;
                    continue;
                }
                //需要校验“合计利息”和“合计利息（本币）”字段必须有值，如果没有值，则提示“请先进行利息测算”
                BigDecimal totalInterest = accrualsWithholding.getTotalInterest();
                if (null == totalInterest || BigDecimal.ZERO.compareTo(totalInterest) == 0) {
                    failed.put(bankaccountAccount, bankaccountAccount);
                    messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180224","银行账号【%s】，币种为【%s】的合计利息为0，不需要预提！") /* "银行账号【%s】，币种为【%s】的合计利息为0，不需要预提！" */,
                            bankaccountAccount, currencyName));
                    failSize++;
                    continue;
                }
                //校验是否已预提
                WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id, 0);
                if(Objects.isNull(withholdingRuleSetting)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100778"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180204","系统检测到预提规则已删除，请刷新页面重新测算后预提。") /* "系统检测到预提规则已删除，请刷新页面重新测算后预提。" */ );
                }
                if(!checkWithholding( messages,  failed,  failSize,  bankaccountAccount, currencyName, accrualsWithholding, withholdingRuleSetting)){
                    continue;
                }
                //一系列校验
                List<InterestCalculation> interestCalculationList = checkInfo( accrualsWithholding, withholdingRuleSetting, id, isFirst);
                accrualsWithholding.setLastInterestAccruedDate(withholdingRuleSetting.getLastInterestAccruedDate());
                accrualsWithholding.setAccountPurpose(withholdingRuleSetting.getAccountPurpose());
                accrualsWithholding.setBankNumber(withholdingRuleSetting.getBankNumber());
                setDepositinterestWithholdingInfo(accrualsWithholding);
                //数据库操作重启事物处理
                insertOrUpdate(interestCalculationList, withholdingRuleSetting, accrualsWithholding);
            } catch (Exception e) {
                String message = e.getMessage();
                log.error("================AccrualsWithholdingService circulate :" + message, e);
                String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 300 ? message.substring(300) : message) : null;
                failed.put(bankaccountAccount, bankaccountAccount);
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180210","银行账号【%s】，币种为【%s】预提失败，【%s】") /* "银行账号【%s】，币种为【%s】预提失败，【%s】" */,
                        bankaccountAccount, currencyName, resultMsg));
                failSize++;
                continue;
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
    }

    /**
     * 校验
     * @param accrualsWithholding
     * @param withholdingRuleSetting
     * @throws Exception
     */
    private List<InterestCalculation> checkInfo(AccrualsWithholding accrualsWithholding,WithholdingRuleSetting withholdingRuleSetting,Long id,AtomicBoolean isFirst) throws Exception {
        List<InterestCalculation> interestCalculationList = new ArrayList<>();
        if(!ValueUtils.isNotEmptyObj(withholdingRuleSetting.getVersion()) ){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100779"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180217","预提规则缺少版本号，为避免数据错误，调整预提规则设置后重新测算后预提!") /* "预提规则缺少版本号，为避免数据错误，调整预提规则设置后重新测算后预提!" */);
        }
        //利率发生变化重新测算
        if (!withholdingRuleSetting.getVersion().toString().equals(accrualsWithholding.getSettingVersion())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100780"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021B","系统检测到该账号预提规则有调整，为避免数据错误，请刷新页面重新测算后预提！") /* "系统检测到该账号预提规则有调整，为避免数据错误，请刷新页面重新测算后预提！" */);
        }
        //日结控制
        if(isFirst.get() && !Objects.isNull(withholdingRuleSetting.getDailySettlementControl())
                && (withholdingRuleSetting.getDailySettlementControl() == DailySettlementControl.AccruaAfterSettlement.getValue())){
            //查询会计主体日结最大日期与预提日期 比较  如果小于预提日期  则所有日结控制为是的数据过滤掉
            Map<String, Object> checkSettleMap = checkSettle(accrualsWithholding.getAccentity());
            if(null != checkSettleMap){
                Date settleDate = DateUtils.dateParse(checkSettleMap.get("settlementdate").toString(),null);
                if(settleDate.compareTo(accrualsWithholding.getAccruedEndDate()) < 0){
                    //查询数据不查询日结控制规则为先日结后预提的数据
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100781"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1809A0E804D00015", "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面重新测算后预提！") /* "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面重新测算后预提！" */);
                }
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100781"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1809A0E804D00015", "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面重新测算后预提！") /* "系统检测到该账号日结发生变化，为避免数据错误，请刷新页面重新测算后预提！" */);
            }
            isFirst.set(false);
        }
        //利息测算表 --- 与 银行账户历史余额表比较 数量不等 需要重新测试
        interestCalculationList = getInterestCalculationList( accrualsWithholding, false);
        // 有协定的 不走下面的校验
        List<InterestCalculation> agreeInterestCalculationOldList = getAgreementInterestCalculationList( accrualsWithholding, false);
        if(!CollectionUtils.isEmpty(agreeInterestCalculationOldList)){
            interestCalculationList.addAll(agreeInterestCalculationOldList);
            return interestCalculationList;
        }
        if (CollectionUtils.isEmpty(interestCalculationList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100782"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021D","系统检测到其他用户对该账号进行了重新测算，为避免数据错误，请刷新页面重新测算后预提！") /* "系统检测到其他用户对该账号进行了重新测算，为避免数据错误，请刷新页面重新测算后预提！" */);
        }
        int interestCalculationListSize = interestCalculationList.size();
        //查询历史余额表 根据日期asc
        List<AccountRealtimeBalance> accountRealtimeBalanceList = getAccountRealtimeBalanceList(withholdingRuleSetting, accrualsWithholding);
        int accountRealtimeBalanceListSize = CollectionUtils.isEmpty(accountRealtimeBalanceList) ? 0 : accountRealtimeBalanceList.size();
        if (interestCalculationListSize != accountRealtimeBalanceListSize) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100782"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021D","系统检测到其他用户对该账号进行了重新测算，为避免数据错误，请刷新页面重新测算后预提！") /* "系统检测到其他用户对该账号进行了重新测算，为避免数据错误，请刷新页面重新测算后预提！" */);
        }
        int includeTodayNum = DateUtils.dateBetweenIncludeToday(accrualsWithholding.getAccruedStartDate(), accrualsWithholding.getAccruedEndDate());
        int realSize = CollectionUtils.isEmpty(accountRealtimeBalanceList) ? 0 : accountRealtimeBalanceList.size();

        if(realSize != includeTodayNum){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100776"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180200","系统检测到该账号历史余额数据不全，为避免数据错误，请补足账户历史余额数据后测算并预提!") /* "系统检测到该账号历史余额数据不全，为避免数据错误，请补足账户历史余额数据后测算并预提!" */);
        }
        return interestCalculationList;
    }

    /**
     * 检查当前日期是否日结
     *
     * @param accentity
     * @throws Exception
     */
    private Map<String, Object> checkSettle(String accentity) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("max(settlementdate) as settlementdate");
        //查询最大结账日
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("settleflag").eq(1));
        querySchema.addCondition(group);
        return MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
    }

    private boolean checkWithholding(List<String> messages, ObjectNode failed, int failSize, String bankaccountAccount,
                                     String currencyName, AccrualsWithholding accrualsWithholding,
                                     WithholdingRuleSetting withholdingRuleSetting) throws Exception {
        if(null ==withholdingRuleSetting || withholdingRuleSetting.getRuleStatus().compareTo(WithholdingRuleStatus.Enable.getValue()) != 0){
            failed.put(bankaccountAccount, bankaccountAccount);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180208","系统检测到银行账号【%s】，币种【%s】预提规则有调整，为避免数据错误，请刷新页面后重新测算!") /* "系统检测到银行账号【%s】，币种【%s】预提规则有调整，为避免数据错误，请刷新页面后重新测算!" */ ,
                    bankaccountAccount, currencyName));
            //failSize++;
            return false;
        }
        if ( accrualsWithholding.getLastInterestSettlementOrAccruedDate().compareTo(
                DepositinterestWithholdingUtil.getMaxDate(withholdingRuleSetting.getLastInterestAccruedDate(), withholdingRuleSetting.getLastInterestSettlementDate(), null)) != 0) {
            failed.put(bankaccountAccount, bankaccountAccount);
            messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020E","银行账号【%s】，币种【%s】预提失败，系统检测到其他用户对该账号进行了重新测算，为避免数据错误，请刷新页面重新测算后预提！") /* "银行账号【%s】，币种【%s】预提失败，系统检测到其他用户对该账号进行了重新测算，为避免数据错误，请刷新页面重新测算后预提！" */ ,
                    bankaccountAccount, currencyName));
            //failSize++;
            return false;
        }
        EnterpriseBankAcctVO enterpriseBankAccount = getEnterpriseBankAccount(accrualsWithholding.getCurrency(),accrualsWithholding.getBankaccount());
        if(null == enterpriseBankAccount){
            failed.put(bankaccountAccount, bankaccountAccount);
            messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180212","银行账号【%s】，币种【%s】预提失败，企业银行账户未启用或查询失败，为避免数据错误，请刷新页面重新测算后预提！") /* "银行账号【%s】，币种【%s】预提失败，企业银行账户未启用或查询失败，为避免数据错误，请刷新页面重新测算后预提！" */  ,
                    bankaccountAccount, currencyName));
            //failSize++;
            return false;
        }
        return true;
    }


    /**
     * 根据id查询企业银行账户
     * @param currency
     * @param bankAccount
     * @return
     * @throws Exception
     */
    private EnterpriseBankAcctVO getEnterpriseBankAccount(String currency, String bankAccount) throws Exception {

        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccount);
        if (null != enterpriseBankAcctVO ) {
            Integer enterpriseBankAccountFlag = enterpriseBankAcctVO.getEnable();
            if (enterpriseBankAccountFlag != null  && (2 == enterpriseBankAccountFlag || 0 == enterpriseBankAccountFlag)) {// enable = 2 停用     0 是未启用    1是启用
                enterpriseBankAcctVO = null;
            }
        }

//        EnterpriseBankAcctVO enterpriseBankAcctVO = null;
//        String enterpriseBankKey = currency.concat(InvocationInfoProxy.getTenantid()).concat(bankAccount);
//        if (null != enterpriseBankAcctVOWithholdCache.getIfPresent(enterpriseBankKey)) {
//            enterpriseBankAcctVO = enterpriseBankAcctVOWithholdCache.getIfPresent(enterpriseBankKey);
//        } else {
//            enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccount);
//            if (null != enterpriseBankAcctVO ) {
//                Integer enterpriseBankAccountFlag = enterpriseBankAcctVO.getEnable();
//                if (enterpriseBankAccountFlag != null && (2 == enterpriseBankAccountFlag || 0 == enterpriseBankAccountFlag) ) {//1是启用，2是未启用
//                } else {
//                    enterpriseBankAcctVOWithholdCache.put(enterpriseBankKey, enterpriseBankAcctVO);
//                }
//            }
//        }
        return enterpriseBankAcctVO;
    }

    private void setDepositinterestWithholdingInfo(AccrualsWithholding accrualsWithholding) throws Exception {
        accrualsWithholding.setVouchdate(BillInfoUtils.getBusinessDate());
        accrualsWithholding.setCreateDate(new Date());
        accrualsWithholding.setCreateTime(new Date());
        accrualsWithholding.setCreatorId(AppContext.getCurrentUser().getId());
        accrualsWithholding.setCreator(AppContext.getCurrentUser().getName());
        accrualsWithholding.setTenant(AppContext.getTenantId());
        accrualsWithholding.setCode(getCode(accrualsWithholding));
        accrualsWithholding.setRelatedinterest(Relatedinterest.relatedUnAssociated.getValue());
        BdBillType bdBillType = baseRefRpcService.queryBillTypeByFormId(ICmpConstant.CM_CMP_CMP_ACCRUALSWITHHOLDINGQUERY);
        if (null == bdBillType) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100783"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180206","查询银行预提单交易类型失败！请检查数据！") /* "查询银行预提单交易类型失败！请检查数据！" */);
        }
        Map<String, Object> tradetypeMap = commonService.queryTransTypeById(bdBillType.getId(), "0", ICmpConstant.BANK_INTEREST);
        if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
            accrualsWithholding.setTradetype(tradetypeMap.get(ICmpConstant.PRIMARY_ID).toString());
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100783"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180206","查询银行预提单交易类型失败！请检查数据！") /* "查询银行预提单交易类型失败！请检查数据！" */);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void insertOrUpdate(List<InterestCalculation> interestCalculationList, WithholdingRuleSetting withholdingRuleSetting,
                               AccrualsWithholding accrualsWithholding) throws Exception {
        //利息测算表持久化
        for (InterestCalculation interestCalculation : interestCalculationList) {
            interestCalculation.setIsWithholding(true);
            interestCalculation.setEntityStatus(EntityStatus.Update);
        }
        MetaDaoHelper.update(InterestCalculation.ENTITY_NAME, interestCalculationList);
        //预提凭证接口调用成功后，更新银行账户上次预提结束日和凭证状态
        withholdingRuleSetting.setLastInterestAccruedDate(accrualsWithholding.getAccruedEndDate());
        withholdingRuleSetting.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, withholdingRuleSetting);
        //入库
        accrualsWithholding.setId(ymsOidGenerator.nextId());
        accrualsWithholding.setEntityStatus(EntityStatus.Insert);
        boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
        if (!enableEVNT) {
            accrualsWithholding.setVoucherstatus(VoucherStatus.NONCreate.getValue());
        } else {
            //生成凭证
            accrualsWithholding.setVoucherstatus(VoucherStatus.POSTING.getValue());
        }
        CmpMetaDaoHelper.insert(AccrualsWithholding.ENTITY_NAME, accrualsWithholding);

        //获取pubts
        BizObject bizObjectNew = MetaDaoHelper.findById(AccrualsWithholding.ENTITY_NAME, accrualsWithholding.getId());
        accrualsWithholding.set("entityName", AccrualsWithholding.ENTITY_NAME);
        accrualsWithholding.set("_entityName", AccrualsWithholding.ENTITY_NAME);
        accrualsWithholding.set("pubts", bizObjectNew.getPubts());


        CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(accrualsWithholding);
        if (!generateResult.getBoolean("dealSucceed")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100784"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180209","单据【") /* "单据【" */ + accrualsWithholding.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180218","】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateResult.get("message"));
        }
        try {
            ObjectNode jsonObject = JSONBuilderUtils.createJson();
            jsonObject.putPOJO("accrualsWithholding",accrualsWithholding);
            jsonObject.putPOJO("withholdingRuleSetting",withholdingRuleSetting);
            jsonObject.putPOJO("interestCalculationList",interestCalculationList);
            ctmcmpBusinessLogService.saveBusinessLog(jsonObject, accrualsWithholding.getCode(), "", IServicecodeConstant.CMP_ACCRUALSWITHHOLDINGQUERY,
                    IMsgConstant.CMP_ACCRUALSWITHHOLDINGQUERY, IMsgConstant.CMP_ACCRUALSWITHHOLDINGQUERY);
        } catch (Exception e) {
            log.info("============= insertOrUpdate ctmcmpBusinessLogService：" + e.getMessage());
        }
    }

    private String getCode(AccrualsWithholding accrualsWithholding) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);

        String billcode=billCodeComponentService.getBillCode(IBillNumConstant.CMP_ACCRUALSWITHHOLDINGQUERY, AccrualsWithholding.ENTITY_NAME,
                InvocationInfoProxy.getTenantid(),
                "", true, "", false, new BillCodeObj(accrualsWithholding));
        return billcode;
    }

    /**
     * 查询银行账户利率设置变更历史表
     *
     * @param id
     * @return
     * @throws Exception
     */
    private List<InterestRateSettingHistory> getInterestRateSettingHistoryList(Long id) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").in(id));
        //利率历史表
//        conditionGroup.appendCondition(QueryCondition.name("startDate").egt(accrualsWithholding.getAccruedStartDate()));
        schema.addCondition(conditionGroup);
        //利率历史表 升序
        schema.addOrderBy(new QueryOrderby("startDate", "asc"));
        return MetaDaoHelper.queryObject(InterestRateSettingHistory.ENTITY_NAME, schema, null);
    }

    /**
     * 查询协定利率历史变更记录
     * @param id
     * @return
     * @throws Exception
     */
    private List<AgreeIRSettingHistory> getAgreeSettingHistoryList(Long id) throws Exception {
        WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id, 3);
        List<AgreeIRSettingHistory> agreeIRSettingHistoryList =withholdingRuleSetting.get("agreeIRSettingHistory");
        if(CollectionUtils.isEmpty(agreeIRSettingHistoryList)){
            return agreeIRSettingHistoryList;
        }
        // 后续的预提逻辑需要保证协定利率分档历史有序，因此这里排个序
        for (AgreeIRSettingHistory agreeIRSettingHistory : agreeIRSettingHistoryList) {
            List<AgreeIRSettingGradeHistory> agreeIRSettingGradeHistoryList = agreeIRSettingHistory.agreeIRSettingGradeHistory();
            if (CollectionUtils.isNotEmpty(agreeIRSettingGradeHistoryList)) {
                agreeIRSettingHistory.setAgreeIRSettingGradeHistory(
                        agreeIRSettingGradeHistoryList.stream()
                                .sorted(Comparator.comparingLong(AgreeIRSettingGradeHistory::getGradenum))
                                .collect(Collectors.toList()));
            }
        }
        return agreeIRSettingHistoryList;
    }


    private List<InterestCalculation> getAgreementInterestCalculationList(AccrualsWithholding accrualsWithholding, boolean isWithholding) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").in(accrualsWithholding.getWithholdingRuleId()));
        //利息测算表
        conditionGroup.appendCondition(QueryCondition.name("interestDate").egt(accrualsWithholding.getAccruedStartDate()));
        conditionGroup.appendCondition(QueryCondition.name("interestDate").elt(accrualsWithholding.getAccruedEndDate()));
        conditionGroup.appendCondition(QueryCondition.name("isWithholding").eq(isWithholding));
        conditionGroup.appendCondition(QueryCondition.name("isCaculate").eq(1));

        schema.addCondition(conditionGroup);
        //利息测算表 升序
        schema.addOrderBy(new QueryOrderby("interestDate", "asc"));
        return MetaDaoHelper.queryObject(InterestCalculation.ENTITY_NAME, schema, null);
    }
    /**
     * 查询利息测算表
     *
     * @param accrualsWithholding
     * @param isWithholding
     * @return
     * @throws Exception
     */
    private List<InterestCalculation> getInterestCalculationList(AccrualsWithholding accrualsWithholding, boolean isWithholding) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,mainid,interestDays,interestDate,depositbal,interestacc,annualInterestRate,depositInterest," +
                "overdraftbal,overdraftacc,overdraftRate,overdraftInterest,totalInterestAmount,isWithholding,currency,currency.moneyDigit");
//        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").in(accrualsWithholding.getWithholdingRuleId()));
        //利息测算表
        conditionGroup.appendCondition(QueryCondition.name("interestDate").egt(accrualsWithholding.getAccruedStartDate()));
        conditionGroup.appendCondition(QueryCondition.name("interestDate").elt(accrualsWithholding.getAccruedEndDate()));
        conditionGroup.appendCondition(QueryCondition.name("isWithholding").eq(isWithholding));
        conditionGroup.appendCondition(QueryCondition.name("isCaculate").eq(0));

        schema.addCondition(conditionGroup);
        //利息测算表 升序
        schema.addOrderBy(new QueryOrderby("interestDate", "asc"));
        return MetaDaoHelper.queryObject(InterestCalculation.ENTITY_NAME, schema, null);
    }

    /**
     * 反预提
     *
     * @param accrualsWithholdinges 列表数据
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public JsonNode unwWthholding(List<AccrualsWithholding> accrualsWithholdinges) throws Exception {
        ObjectNode jsonObject = JSONBuilderUtils.createJson();
        //用于记录批量预失败条数
        int failSize = 0;
        //用于记录批量预提失败的账户信息
        ObjectNode failed = JSONBuilderUtils.createJson();
        //用于记录批量预提失败原因
        List<String> messages = new ArrayList<>(FAILIALCAPACITY);
        //前台传入列表总条数
        int size = accrualsWithholdinges.size();
        List<AccrualsWithholding> accrualsWithholdingList = new ArrayList<>(accrualsWithholdinges.size());

        for (BizObject bizObject : accrualsWithholdinges){
            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
            accrualsWithholding.init(bizObject);
            accrualsWithholdingList.add(accrualsWithholding);
        }
        //批量选择预提单是，校验存在相同北部账户的多条预提记录是，不允许操作
        batchJudge(accrualsWithholdingList);
        unwWthholdingProcess(accrualsWithholdingList, messages, failed, failSize);
        failSize = messages.size();
        String message = null;
        if (size == 1) {
            if (messages.size() == 0) {
                message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020C","反预提成功!") /* "反预提成功!" */;
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100785"),messages.get(0));
            }
        } else {
            message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020F","共：%s张单据；%s张反预提成功；%s张反预提失败！") /* "共：%s张单据；%s张反预提成功；%s张反预提失败！" */, size,
                    size - failSize, failSize);
        }
        jsonObject.put("msg", message);
        jsonObject.putPOJO("msgs", messages);
        jsonObject.putPOJO("messages", messages);
        jsonObject.put("count", size);
        jsonObject.put("sucessCount", size - failSize);
        jsonObject.put("failCount", failSize);
        if (failed.size() > 0) {
            jsonObject.putPOJO("failed", failed);
        }
        return jsonObject;
    }

    /**
     * 批量反预提校验
     *
     * @param accrualsWithholdinges
     */
    private void batchJudge(List<AccrualsWithholding> accrualsWithholdinges) {
        Map<String, List<AccrualsWithholding>> map = accrualsWithholdinges.stream().collect(Collectors.groupingBy(AccrualsWithholding::getBankaccount));
        for (String key : map.keySet()) {
            List<AccrualsWithholding> depositinterestWithholdingJudge = map.get(key);
            if (com.yonyou.cloud.utils.CollectionUtils.isNotEmpty(depositinterestWithholdingJudge) && depositinterestWithholdingJudge.size() > 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100786"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021C","不支持对同一币种银行账号的多条预提单同时进行反预提操作") /* "不支持对同一币种银行账号的多条预提单同时进行反预提操作" */);
            }
            continue;
        }
    }

    /**
     * @param accrualsWithholdinges
     * @param messages
     * @param failed
     * @param failSize
     */
    private void unwWthholdingProcess(List<AccrualsWithholding> accrualsWithholdinges, List<String> messages, ObjectNode failed, int failSize) throws Exception {
        if (!CollectionUtils.isEmpty(accrualsWithholdinges)) {
            for (AccrualsWithholding accrualsWithholdingOld : accrualsWithholdinges) {
                BizObject bizObject = MetaDaoHelper.findById(AccrualsWithholding.ENTITY_NAME, accrualsWithholdingOld.getId(), 1);
                if(null == bizObject){
                    failed.put(accrualsWithholdingOld.getCode(), accrualsWithholdingOld.getCode());
                    messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1809A82004D0000E", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */ , accrualsWithholdingOld.getCode()));
                    failSize++;
                    continue;
                }
                AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                accrualsWithholding.init(bizObject);
                if (null == accrualsWithholding) {
                    failed.put(accrualsWithholdingOld.getCode(), accrualsWithholdingOld.getCode());
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1809A82004D0000E", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */ , accrualsWithholdingOld.getCode()));
                    failSize++;
                    continue;
                }
                String key = "unwWthholdingProcess".concat(accrualsWithholding.getId().toString());
                String code = accrualsWithholding.getCode();
                YmsLock ymsLock = null;
                try {
                    if ((ymsLock=JedisLockUtils.lockBillWithOutTrace(key))==null) {
                        failed.put(code, code);
                        messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020D","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */+ accrualsWithholding.getCode() /* "该单据已锁定，请稍后重试！" */);
                        failSize++;
                        continue;
                    }
                    if (!unwWthholdingCheck(accrualsWithholding, messages, failed, failSize, code)) {
                        continue;
                    }
                    //删数据 删凭证
                    deleteBill(accrualsWithholding);
                    try {
                        ObjectNode jsonObject = JSONBuilderUtils.createJson();
                        jsonObject.putPOJO("accrualsWithholding",accrualsWithholding);
                        ctmcmpBusinessLogService.saveBusinessLog(jsonObject, code, "", IServicecodeConstant.CMP_ACCRUALSWITHHOLDINGQUERY,
                                IMsgConstant.CMP_UNACCRUALSWITHHOLDINGQUERY, IMsgConstant.CMP_UNACCRUALSWITHHOLDINGQUERY);
                    } catch (Exception e) {
                        log.info("=============== unwWthholdingProcess ctmcmpBusinessLogService：" + e.getMessage());
                    }
                } catch (Exception e) {
                    String message = e.getMessage();
                    log.error("================unwWthholdingProcess circulate :" + message, e);
                    String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 300 ? message.substring(300) : message) : null;
                    failed.put(code, code);
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180215","预提单编号：%s，反预提失败，【%s】") /* "预提单编号：%s，反预提失败，【%s】" */, code, resultMsg));
                    failSize++;
                    continue;
                } finally {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            }
        }
    }

    /**
     * 删数据 删凭证  删除利息测算表
     *
     * @param accrualsWithholding
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void deleteBill(AccrualsWithholding accrualsWithholding) throws Exception {
        //3、反预提成功后，需要更新银行账号预提规则的上次预提结束日。上次预提结束日取值逻辑：等于距离本预提单最近的预提记录的预提结束日。
        Long withholdingRuleId = accrualsWithholding.getWithholdingRuleId();
        //查询 距离本预提单最近的预提记录 不包括本条记录
//        AccrualsWithholding depositinterestWithholdingJudge = getDepositInterestWithholding(accrualsWithholding);
        //更新银行账号预提规则的上次预提结束日
        WithholdingRuleSetting withholdingRuleSetting = MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, withholdingRuleId, 1);
        withholdingRuleSetting.setLastInterestAccruedDate(accrualsWithholding.getLastInterestAccruedDate());
        withholdingRuleSetting.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(WithholdingRuleSetting.ENTITY_NAME, withholdingRuleSetting);
        //删除利息测算表
        List<InterestCalculation> interestCalculationList = getInterestCalculationList( accrualsWithholding, true);
        List<InterestCalculation> agreeInterestCalculationOldList = getAgreementInterestCalculationList(accrualsWithholding, true);
        MetaDaoHelper.delete(InterestCalculation.ENTITY_NAME, agreeInterestCalculationOldList);
        MetaDaoHelper.delete(InterestCalculation.ENTITY_NAME, interestCalculationList);
        //删除账户预提单
        MetaDaoHelper.delete(AccrualsWithholding.ENTITY_NAME, accrualsWithholding);
        accrualsWithholding.set("entityName", AccrualsWithholding.ENTITY_NAME);
        accrualsWithholding.set("_entityName", AccrualsWithholding.ENTITY_NAME);
        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(accrualsWithholding);
        if (!deleteResult.getBoolean("dealSucceed")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100784"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180209","单据【") /* "单据【" */ + accrualsWithholding.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020A","】删除凭证失败：") /* "】删除凭证失败：" */ + deleteResult.get("message"));
        }
    }

    /**
     * 反预提校验
     *
     * @param accrualsWithholding
     * @param messages
     * @param failed
     * @param failSize
     * @return
     */
    private boolean unwWthholdingCheck(AccrualsWithholding accrualsWithholding, List<String> messages, ObjectNode failed, int failSize, String code) throws Exception {
        if (accrualsWithholding.getRelatedinterest().compareTo(Relatedinterest.relatedAssociated.getValue()) == 0) {
            failed.put(code, code);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418021F","预提单编号：%s，预提记录已关联，不允许进行反预提！") /* "预提单编号：%s，预提记录已关联，不允许进行反预提！" */, code));
            //failSize++;
            return false;
        }
        if (getWithholdingList(accrualsWithholding) > 0) {
            failed.put(code, code);
            messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180222","预提单编号：%s，预提结束日期之后存在预提记录，不允许进行反预提！") /* "预提单编号：%s，预提结束日期之后存在预提记录，不允许进行反预提！" */, code));
            //failSize++;
            return false;
        }
        EnterpriseBankAcctVO enterpriseBankAccount = getEnterpriseBankAccount(accrualsWithholding.getCurrency(),accrualsWithholding.getBankaccount());
        if(null == enterpriseBankAccount){
            failed.put(code, code);
            messages.add(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180202","预提单编号：%s，企业银行账户未启用或查询失败，不允许进行反预提！") /* "预提单编号：%s，企业银行账户未启用或查询失败，不允许进行反预提！" */ ,
                    code));
            //failSize++;
            return false;
        }
        return true;
    }

    /**
     * 查询预提表
     *
     * @param accrualsWithholding
     * @return
     * @throws Exception
     */
    private Integer getWithholdingList(AccrualsWithholding accrualsWithholding) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditionGroup.appendCondition(QueryCondition.name("accruedEndDate").gt(accrualsWithholding.getAccruedEndDate()));
        conditionGroup.appendCondition(QueryCondition.name("accruedStartDate").gt(accrualsWithholding.getAccruedEndDate()));
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(accrualsWithholding.getAccentity()));
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.BANKACCOUNT).eq(accrualsWithholding.getBankaccount()));
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(accrualsWithholding.getCurrency()));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> withholdingList = MetaDaoHelper.query(AccrualsWithholding.ENTITY_NAME, schema, null);
        if (CollectionUtils.isEmpty(withholdingList)) {
            return 0;
        } else {
            return Integer.valueOf(withholdingList.get(0).get("count").toString());
        }
    }

    @Override
    public JsonNode queryInterestHandleList(List<AccrualsWithholding> accrualsWithholdinges) throws Exception {
        ObjectNode jsonObject = JSONBuilderUtils.createJson();
        if (!CollectionUtils.isEmpty(accrualsWithholdinges)) {
            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
            accrualsWithholding.init(accrualsWithholdinges.get(0));
            Long id = accrualsWithholding.getId();
            accrualsWithholding.setWithholdingRuleId(id);
            CurrencyTenantDTO currencyDTO = findCurrency(accrualsWithholding.getCurrency());
            WithholdingRuleSetting withholdingRuleSetting = findWithholdingRuleSetting(id);
            if (null != withholdingRuleSetting) {
                //赋值bankNumber
                //:bankaccount_account
                //bankaccount_name
                withholdingRuleSetting.set(ICmpConstant.BANKACCOUNT_ACCOUNT,accrualsWithholding.get(ICmpConstant.BANKACCOUNT_ACCOUNT));
                withholdingRuleSetting.set(ICmpConstant.BANKACCOUNT_NAME,accrualsWithholding.get(ICmpConstant.BANKACCOUNT_NAME));
                withholdingRuleSetting.setAccruedStartDate(accrualsWithholding.getAccruedStartDate());
                withholdingRuleSetting.setAccruedEndDate(accrualsWithholding.getAccruedEndDate());
                List<InterestCalculation> interestCalculationList = getInterestCalculationList( accrualsWithholding, false);
                if (!CollectionUtils.isEmpty(interestCalculationList)) {
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_NAME, currencyDTO.getName());
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_MONEYDIGIT, currencyDTO.getMoneydigit());
                    withholdingRuleSetting.setInterestCalculation(interestCalculationList);
                }
                // 协定的测算
                List<InterestCalculation> agreeInterestCalculationList = getAgreementInterestCalculationList( accrualsWithholding, false);
                if (!CollectionUtils.isEmpty(agreeInterestCalculationList)) {
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_NAME, currencyDTO.getName());
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_MONEYDIGIT, currencyDTO.getMoneydigit());
                    withholdingRuleSetting.put("AgreeInterestCalculation",agreeInterestCalculationList);
                }
                jsonObject.putPOJO("withholdingRuleSetting", withholdingRuleSetting);
                jsonObject.put("resultCode", "200");
            }
        }
        return jsonObject;
    }

    @Override
    public JsonNode queryInterestList(List<AccrualsWithholding> accrualsWithholdinges) throws Exception {
        ObjectNode jsonObject = JSONBuilderUtils.createJson();
        if (!CollectionUtils.isEmpty(accrualsWithholdinges)) {
            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
            accrualsWithholding.init(accrualsWithholdinges.get(0));
            CurrencyTenantDTO currencyDTO = findCurrency(accrualsWithholding.getCurrency());
            WithholdingRuleSetting withholdingRuleSetting = findWithholdingRuleSetting(accrualsWithholding.getWithholdingRuleId());
            if (null != withholdingRuleSetting) {
                withholdingRuleSetting.set(ICmpConstant.BANKACCOUNT_ACCOUNT,accrualsWithholding.get(ICmpConstant.BANKACCOUNT_ACCOUNT));
                withholdingRuleSetting.set(ICmpConstant.BANKACCOUNT_NAME,accrualsWithholding.get(ICmpConstant.BANKACCOUNT_NAME));
                withholdingRuleSetting.setAccruedStartDate(accrualsWithholding.getAccruedStartDate());
                withholdingRuleSetting.setAccruedEndDate(accrualsWithholding.getAccruedEndDate());
                List<InterestCalculation> interestCalculationList = getInterestCalculationList( accrualsWithholding, true);
                if (!CollectionUtils.isEmpty(interestCalculationList)) {
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_NAME, currencyDTO.getName());
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_MONEYDIGIT, currencyDTO.getMoneydigit());
                    withholdingRuleSetting.setInterestCalculation(interestCalculationList);
                }
                // 协定的测算
                List<InterestCalculation> agreeInterestCalculationList = getAgreementInterestCalculationList( accrualsWithholding, true);
                if (!CollectionUtils.isEmpty(agreeInterestCalculationList)) {
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_NAME, currencyDTO.getName());
                    withholdingRuleSetting.put(ICmpConstant.CURRENCY_MONEYDIGIT, currencyDTO.getMoneydigit());
                    withholdingRuleSetting.put("AgreeInterestCalculation",agreeInterestCalculationList);
                }
                jsonObject.putPOJO("withholdingRuleSetting", withholdingRuleSetting);
                jsonObject.put("resultCode", "200");
            }
        }
        return jsonObject;
    }

    /**
     * 查询预提规则设置表
     *
     * @param id
     * @return
     * @throws Exception
     */
    private WithholdingRuleSetting findWithholdingRuleSetting(Long id) throws Exception {
        return MetaDaoHelper.findById(WithholdingRuleSetting.ENTITY_NAME, id, 1);
    }

    /**
     *  根据单据类型查询交易类型
     * @return
     * @throws Exception
     */
    public void tradeTypeHandler(AccrualsWithholding accrualsWithholding) throws Exception {
        //查询单据类型
        BillContext bc = new BillContext();
        bc.setFullname("bd.bill.BillTypeVO");
        bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schemaBilltype = QuerySchema.create();
        schemaBilltype.addSelect(ICmpConstant.ID);
        QueryConditionGroup conditionGroupBilltype = new QueryConditionGroup(ConditionOperator.and);
        conditionGroupBilltype.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_CMP_ACCRUALSWITHHOLDINGQUERY));
        schemaBilltype.addCondition(conditionGroupBilltype);
        List<Map<String, Object>> list = MetaDaoHelper.query(bc, schemaBilltype);
        String billtypeId = null;
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, Object> objectMap = list.get(0);
            if (!ValueUtils.isNotEmptyObj(objectMap)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100787"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180221","查询银行预提单交易类型失败！请检查数据。") /* "查询银行预提单交易类型失败！请检查数据。" */);
            }
            billtypeId = MapUtils.getString(objectMap, "id");
        }else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100787"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180221","查询银行预提单交易类型失败！请检查数据。") /* "查询银行预提单交易类型失败！请检查数据。" */);
        }

        Map<String, Object> tradetypeMap = commonService.queryTransTypeById(billtypeId, "0", "bank-interest");
        if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
            accrualsWithholding.setTradetype(tradetypeMap.get(ICmpConstant.PRIMARY_ID).toString());
        }else{
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100788"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180205","查询交易类型失败！") /* "查询交易类型失败！" */);
        }
    }



}
