package com.yonyoucloud.fi.cmp.util.business;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.BaseDeptDTO;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.service.itf.core.DeptCheckApi;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeContext;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ProjectDTO;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.common.BizObjCodeUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dubbo.DubboReferenceUtils;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.api.IPaymentTypeService;
import com.yonyoucloud.fi.basecom.utils.*;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.QuickTypeVO;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.rpc.rule.DailyComputezInit;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.imeta.biz.base.BizException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

/**
 * 现金管理业务公共工具类
 */
@Slf4j
@Component
public class CmpCommonUtil {
    private static TransTypeQueryService transTypeQueryService;
    private static CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;
    private static DeptCheckApi deptCheckApi;
    private static CmpCheckService cmpCheckService;

    //统一增加内存级缓存，避免for循环内部n多次RPC调用（仅仅是还历史债务的方式，并不是最好的实现方式）
    /**
     * 缓存资金组织
     */
    private static final @NonNull Cache<List<String>, List<BizObject>> paymentTypesCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    private static final @NonNull Cache<String, List<Map<String, Object>>> quickTypeListCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    @Autowired
    private void setCmpBankReconciliationSettingRpcService(CtmCmpBankReconciliationSettingRpcServiceImpl baseRefRpcService) {
        CmpCommonUtil.cmpBankReconciliationSettingRpcService = baseRefRpcService;
    }

    @Autowired
    private void setCmpCheckService(CmpCheckService cmpCheckService) {
        CmpCommonUtil.cmpCheckService = cmpCheckService;
    }

    @Autowired
    private void setTransTypeQueryService(TransTypeQueryService transTypeQueryService) {
        CmpCommonUtil.transTypeQueryService = transTypeQueryService;
    }

    @Autowired
    private void setDeptCheckApi(DeptCheckApi deptCheckApi) {
        CmpCommonUtil.deptCheckApi = deptCheckApi;
    }

    /**
     * 根据会计主体校验日结信息
     */
    public static void checkDate(BizObject bizObject, Date maxSettleDate) {
        if (maxSettleDate != null) {
            Date compareDate = bizObject.get(ICmpConstant.VOUCHDATE);
            if (maxSettleDate.compareTo(compareDate) >= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101249"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418064F", "单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
            }
            if (bizObject.get(ICmpConstant.DZ_DATE) != null && maxSettleDate.compareTo(bizObject.get(ICmpConstant.DZ_DATE)) >= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101250"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180650", "登账日期已日结，不能保存单据！") /* "登账日期已日结，不能保存单据！" */);
            }
        }
    }

    /**
     * 校验企业银行账户与币种一致性
     *
     * @param bankacct 企业银行账户id
     * @param currency 币种id
     * @throws Exception
     */
    public static void checkBankAcctCurrency(Object bankacct, Object currency) throws Exception {
        if (null == bankacct || null == currency) {
            return;
        }
        boolean existBankAcctCurrencyByBankacc = QueryBaseDocUtils.isExistBankAcctCurrencyByBankacc(bankacct, currency);
        if (!existBankAcctCurrencyByBankacc) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101251"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180651", "银行账号币种与导入币种不一致！") /* "银行账号币种与导入币种不一致！" */);
        }
    }

    /**
     * 校验外部数据（OpenAPI）推单是否重复，系统是否已存在
     *
     * @param lockFieldName 来源单据字段名称
     * @param srcbillid     来源单据id
     * @param entityName    元数据实体名称
     * @throws Exception
     */
    public static void checkPayBillExist(String lockFieldName, String srcbillid, String entityName) throws Exception {
        if (null == srcbillid || null == lockFieldName || null == entityName) {
            return;
        }
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(ISystemCodeConstant.THIRD_PARTY + srcbillid);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101252"), MessageUtils.getMessage("P_YS_FI_CM_0001123695") /* "调用数据重复！" */);
        }
        Map<String, Object> payBillList = MetaDaoHelper.queryOne(entityName,
                QuerySchema.create().addSelect("id").appendQueryCondition(QueryCondition.name(lockFieldName).eq(srcbillid)));
        if (MapUtils.isNotEmpty(payBillList)) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101253"), MessageUtils.getMessage("P_YS_CTM_CM-BE_0001648158") /* "调用数据重复！单据已存在" */);
        }
    }

    /**
     * 校验日期是否在现金启用日期之后
     */
    public static boolean checkDateByCmPeriod(String accentity, Date date) throws Exception {
//        Long accBookTypeId = FINBDApiUtil.getFI4BDService().getAccBookTypeByAccBody(accentity);
//        String period = FINBDApiUtil.getFI4BDService().getPeriodByModule(accBookTypeId,"CM");
//        Period periodvo = FINBDApiUtil.getFI4BDAccPeriodService().getPeriodVOByDate(date,String.valueOf(accBookTypeId));
//        if(period.compareTo(periodvo.getCode()) > 0){ //对应日期当天现金还未启用，返回false
//            return false;
//        }
        //资金组织适配，accentity可能是资金组织不是会计主体，需要根据资金组织查询会计主体;
        try {
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            if (finOrgDTO != null) {
                accentity = finOrgDTO.getId();
            }
        } catch (Exception e) {
            log.error("根据资金组织查询会计主体错误,errorMsg:{}", e.getMessage());
        }
        Date beginDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);
        if (beginDate == null) {
            return false;
        }
        //若期初日期 小于传入日期 说明已经启用返回true
        if (DateUtils.dateCompare(beginDate, date) <= 0) {
            return true;
        }
        return false;
    }


    /**
     * 校验日期是否在总账启用日期之后
     */
    public static boolean checkDateByAccPeriod(String accentity, Date date) throws Exception {
        //资金组织适配，accentity可能是资金组织不是会计主体，需要根据资金组织查询会计主体;
        try {
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            if (finOrgDTO != null) {
                accentity = finOrgDTO.getId();
            }
        } catch (Exception e) {
            log.error("根据资金组织查询会计主体错误,errorMsg:{}", e.getMessage());
        }
        Date beginDateGl = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity, ISystemCodeConstant.ORG_MODULE_GL);
        if (beginDateGl == null) {
            return false;
        }
        //若期初日期 小于传入日期 说明已经启用返回true
        if (DateUtils.dateCompare(beginDateGl, date) <= 0) {
            return true;
        }
        return false;
    }

    /**
     * 获取日期前一天的日记账余额及方向
     *
     * @param accentity
     * @param bankaccount
     * @param currency
     * @param date
     * @return
     * @throws Exception
     */
    public static Map<String, Object> getCoinitloribalance(String accentity, String bankaccount, String currency, Date date) throws Exception {
        //取日期前一天的余额
        Date endDate = DateUtils.dateAdd(date, -1, false);
        Map<String, SettlementDetail> settlementDetailMap = DailyComputezInit.imitateDailyComputeInit(accentity, currency, null, bankaccount, "2", "2", endDate);
        String key = accentity + bankaccount + currency;
        SettlementDetail settlementDetail = settlementDetailMap.get(key.replace("null", ""));
        if (null == settlementDetail) {
            settlementDetail = new SettlementDetail();
            settlementDetail.setTodayorimoney(BigDecimal.ZERO);
        }
        //精度处理
        CurrencyDTO currencyDTO = CurrencyUtil.getCurrency(currency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);

        BigDecimal coinitloribalance = settlementDetail.getTodayorimoney().setScale(currencyDTO.getMoneydigit(), moneyRound);
        Map<String, Object> result = new HashMap<>();

        if (coinitloribalance.compareTo(BigDecimal.ZERO) >= 0) {
            result.put("direction", Direction.Debit);
        } else {
            result.put("direction", Direction.Credit);
        }
        result.put("coinitloribalance", coinitloribalance.abs());
        return result;
    }

    /**
     * 取日期前一天的对账单余额
     *
     * @param accentity
     * @param bankaccount
     * @param currency
     * @param date
     * @return
     * @throws Exception
     */
    public static Map<String, Object> getBankinitoribalance(Long bankreconciliationscheme, String accentity, String bankaccount, String currency, Date date) throws Exception {
        BigDecimal acct_bal = null;//银行对账单中实际余额
        QuerySchema bankQuerySchema = QuerySchema.create().addSelect("acct_bal");
        QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity), QueryCondition.name("bankaccount").eq(bankaccount), QueryCondition.name("currency").eq(currency), QueryConditionGroup.and(QueryCondition.name("dzdate").lt(date)));
        bankQuerySchema.addCondition(bankGroup);
        bankQuerySchema.addOrderBy(new QueryOrderby("dzdate", "desc"), new QueryOrderby("bank_seq_no", "desc"), new QueryOrderby("tran_time", "desc"));
        List<Map<String, Object>> bankreconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, bankQuerySchema);
        if (bankreconciliationList == null || bankreconciliationList.size() == 0) {
            bankreconciliationList = new ArrayList<>();
        }
        for (Map<String, Object> map : bankreconciliationList) {
            if (acct_bal == null && map.get("acct_bal") != null) {
                acct_bal = (BigDecimal) map.get("acct_bal");//银行对账单中实际余额 取最新单据的余额
            }
        }
        Map<String, Object> result = new HashMap<>();
        if (acct_bal == null) {
            acct_bal = BigDecimal.ZERO;
        }
        result.put("bankinitoribalance", acct_bal);
        if (acct_bal.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("direction", Direction.Credit);
        } else {
            result.put("direction", Direction.Debit);
        }
        return result;
    }

    /**
     * 调用总账相关接口，获取余额及对账日期等相关信息
     *
     * @param obj
     * @param initFlag
     * @param accentity
     * @param bankaccount
     * @param bankreconciliationscheme
     * @param currency
     * @return
     * @throws Exception
     */
    public static CtmJSONObject getVoucherBalance(CtmJSONObject obj, boolean initFlag, String accentity, String bankaccount, Long bankreconciliationscheme, String currency, boolean needCheckOrgid) throws Exception {
        CtmJSONObject result = new CtmJSONObject();

        if (ObjectUtils.isNotEmpty(obj)) {
            List simpleVOs = (List) obj.get("simpleVOs");
            if (simpleVOs != null) {
                Iterator<Map<String, Object>> iterator = simpleVOs.iterator();
                while (iterator.hasNext()) {
                    if ("checkflag".equals(iterator.next().get("field"))) {
                        iterator.remove();//叶青：余额调节表页面查余额时，参数checkflag，代表只查未对账或已对账的数据，应该是全部
                    }
                }
            }
        }

        //查询对账方案下使用组织的账簿
        PlanParam planParam = new PlanParam(null, null, bankreconciliationscheme.toString());
        List<BankReconciliationSettingVO> infoList = cmpBankReconciliationSettingRpcService.findUseOrg(planParam);
        //处理账户使用组织赋值
        List<BankReconciliationSettingVO> infoListForOrg = cmpCheckService.findUseOrg(planParam);
        BigDecimal journalye = BigDecimal.ZERO;//企业日记账余额
        BigDecimal debitoriSumJour = BigDecimal.ZERO;//借方原币金额
        BigDecimal creditoriSumJour = BigDecimal.ZERO;//贷方原币金额
        BigDecimal subjectBalance = BigDecimal.ZERO;//企业方期初余额
        List<CtmJSONObject> voucherDetailInfoList = new ArrayList<>();
        List<String> checkedbookids = new ArrayList<>();
        for (BankReconciliationSettingVO settingVO : infoList) {
            //调用期初余额需要过滤对应币种和账户
            if (!settingVO.getBankAccount().equals(bankaccount) || !settingVO.getCurrency().equals(currency)) {
                continue;
            }
            if (settingVO.getEnableStatus() != EnableStatus.Enabled.getValue()) {
                continue;
            }
            //CZFW-314625问题修复 一个账簿只查询一次
            if (checkedbookids.contains(settingVO.getAccBook())) {
                continue;
            } else {
                checkedbookids.add(settingVO.getAccBook());
            }
            CtmJSONObject voucherDetailInfo = new CtmJSONObject();
            // 处理 useOrgName 的获取逻辑
            String useOrgName = null;
            for (BankReconciliationSettingVO orgSetting : infoListForOrg) {
                if (orgSetting.getBankAccount().equals(bankaccount) &&
                        orgSetting.getCurrency().equals(currency) &&
                        orgSetting.getAccBook().equals(settingVO.getAccBook())) {
                    useOrgName = orgSetting.getUseOrgName();
                    break;
                }
            }
            voucherDetailInfo.put("useOrgName", useOrgName);
            //获取数据===================================================
            String thd_userId = AppContext.getCurrentUser().getYhtUserId();
            Map<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");
            header.put("thd_userId", thd_userId);
            String serverUrl = AppContext.getEnvConfig("yzb.base.url");
            String BASE_URL_ACCOUNT_SETTLE = null;

            if (initFlag) {
                //期初需要判断授权使用组织是否一致
                if (needCheckOrgid && !StringUtils.isEmpty(accentity) && !accentity.equals(settingVO.getUseOrg())) {
                    continue;
                }
                BASE_URL_ACCOUNT_SETTLE = serverUrl + "/accountInTransit/getInitBalMes";
                CtmJSONObject req = new CtmJSONObject();
                req.put("accentity", settingVO.getUseOrg());
                req.put("bankAccount", settingVO.getBankAccount());
                req.put("bankreconciliationscheme", bankreconciliationscheme);
                req.put("currency", settingVO.getCurrency());
                req.put("accbookId", settingVO.getAccBook());
                String str = HttpTookit.
                        doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(req), header, "UTF-8");
                result = CtmJSONObject.parseObject(str);
                log.error("/getInitBalMes 结果打印=======:", result);
                boolean success = result.containsKey("success") ? result.getBoolean("success") : true;
                if (success) {
                    debitoriSumJour = debitoriSumJour.add(result.getBigDecimal("debitoriSum"));
                    creditoriSumJour = creditoriSumJour.add(result.getBigDecimal("creditoriSum"));
                    BigDecimal debitori = result.getBigDecimal("debitoriSum") == null ? BigDecimal.ZERO : result.getBigDecimal("debitoriSum");
                    BigDecimal creditori = result.getBigDecimal("creditoriSum") == null ? BigDecimal.ZERO : result.getBigDecimal("creditoriSum");
                    subjectBalance = result.getBigDecimal("subjectBalance");
                    //期初sum=subjectBalance
                    voucherDetailInfo.put("sum", result.getBigDecimal("subjectBalance"));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101254"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418064E", "总账/ficloud/accountInTransit/getInitBalMes接口报错，错误信息：") /* "总账/ficloud/accountInTransit/getInitBalMes接口报错，错误信息：" */ + result.getString("message"));
                }
            } else {
                BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/sumvoucher";
                obj.put("billnum", "cmp_balanceadjust");
                obj.put("accbookId", settingVO.getAccBook());
                log.error("/sumvoucher 请求参数=======:" + CtmJSONObject.toJSONString(obj));
                String str = HttpTookit.
                        doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(obj), header, "UTF-8");
                result = CtmJSONObject.parseObject(str);
                boolean success = result.containsKey("success") ? result.getBoolean("success") : true;
                if (success) {
                    log.error("/sumvoucher 结果打印=======:", result);
                    debitoriSumJour = debitoriSumJour.add(result.getBigDecimal("debitoriSum"));
                    creditoriSumJour = creditoriSumJour.add(result.getBigDecimal("creditoriSum"));
                    BigDecimal debitori = result.getBigDecimal("debitoriSum") == null ? BigDecimal.ZERO : result.getBigDecimal("debitoriSum");
                    BigDecimal creditori = result.getBigDecimal("creditoriSum") == null ? BigDecimal.ZERO : result.getBigDecimal("creditoriSum");
                    if (result.getBigDecimal("balance") != null) {
                        journalye = journalye.add(result.getBigDecimal("balance"));
                        voucherDetailInfo.put("sum", result.getBigDecimal("balance"));
                    } else {
                        voucherDetailInfo.put("sum", debitori.subtract(creditori));
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101255"), result.getString("message"));
                }
            }
            voucherDetailInfoList.add(voucherDetailInfo);
        }
        result.put("debitoriSum", debitoriSumJour);
        result.put("creditoriSum", creditoriSumJour);
        result.put("balance", journalye);
        if (initFlag) {
            result.put("subjectBalance", subjectBalance);
        }
        //企业日记账余额详细信息
        result.put("voucherDetailInfoList", voucherDetailInfoList);

        return result;

    }

    public static Map<String, Object> getBankAccById(String bankaccount) throws Exception {
        BillContext billContextFinBank = new BillContext();
        billContextFinBank.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QueryConditionGroup groupBank = QueryConditionGroup.and(QueryCondition.name("id").in(bankaccount), QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        QuerySchema querySchemaEnterprise = QuerySchema.create().addSelect("id as bankacct,orgid,code,name,account,bank,bankNumber,lineNumber,currencyList.currency as currency,enable,acctType,tenant").addCondition(groupBank);
        List<Map<String, Object>> dataList = MetaDaoHelper.query(billContextFinBank, querySchemaEnterprise, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);

        for (Map<String, Object> map : dataList) {
            if (bankaccount.equals(map.get("bankacct"))) {
                return map;
            }
        }
        return null;
    }

    //获取新架构标识
    public static boolean getNewFiFlag() {
        if (InvocationInfoProxy.getNewFi() == null) {
            return false;
        }
        return InvocationInfoProxy.getNewFi();
    }

    public static void billCodeHandler(BizObject biz, String mdUri, String billNum) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        //BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(billNum), billNum, InvocationInfoProxy.getTenantid(), ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.ORG)) ? biz.get(ICmpConstant.ORG) : biz.get(ICmpConstant.ACCENTITY), mdUri, new BillCodeObj[]{new BillCodeObj(biz)});
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(BizObjCodeUtils.getBizObjCodeByBillNumAndDomain(billNum, "ctm-ctmp"), billNum, InvocationInfoProxy.getTenantid(), ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.ORG)) ? biz.get(ICmpConstant.ORG) : biz.get(ICmpConstant.ACCENTITY), mdUri, new BillCodeObj[]{new BillCodeObj(biz)});
        billCodeComponentParam.setIsGetRealNo(false);
        BillCodeContext billCodeContext = billCodeComponentService.getBillCodeContext(billCodeComponentParam);
        Integer billnumMode = billCodeContext.getBillnumMode();
        if (billnumMode == 1) {
            String[] batchBillCodes = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
            biz.put(ICmpConstant.CODE, batchBillCodes[0]);
        }
    }

    public static void verifyAcctOpenTypeIsSame(BizObject subObj, Integer acctOpenType, Short acctOpenTypeMain) {
        if (ValueUtils.isNotEmptyObj(acctOpenTypeMain) && !acctOpenTypeMain.toString().equals(acctOpenType.toString())) {
            subObj.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
            subObj.set(ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT, null);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101256"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080082", "转出方和转入方账户商业银行或结算中心账户的银行类别需要一致！") /* "转出方和转入方账户商业银行或结算中心账户的银行类别需要一致！" */);
        }
        subObj.set(ICmpConstant.ACCT_OPEN_TYPE, acctOpenType);
    }

    public static void queryBankAcctVOByParams(BizObject bizObject, List<EnterpriseBankAcctVO> bankAccounts) {
        EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
        Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
        if (!ValueUtils.isNotEmptyObj(acctOpenType)) {
            bizObject.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER, null);
            bizObject.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_ACCOUNT, null);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101257"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC20408007E", "表头上填写的银行账户对应的开户类型为空！") /* "表头上填写的银行账户对应的开户类型为空！" */);
        }
        bizObject.set("acctOpenType", acctOpenType);
    }

    @NotNull
    public static BillContext getBillContextByFundPayment() {
        BillContext billContext = new BillContext();
        billContext.setAction(ICmpConstant.SAVE);
        billContext.setbMain(true);
        billContext.setBillnum(IBillNumConstant.FUND_PAYMENT);
        billContext.setBilltype("Voucher");
        billContext.setMddBoId("ctm-cmp.cmp_fundpayment");
        billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00038", "资金付款单") /* "资金付款单" */);
        billContext.setSupportBpm(true);
        billContext.setTenant(AppContext.getCurrentUser().getTenant());
        billContext.setFullname(FundPayment.ENTITY_NAME);
        billContext.setEntityCode(IBillNumConstant.FUND_PAYMENT);
        return billContext;
    }

    public static BillContext getBillContextByFundCollection() {
        BillContext billContext = new BillContext();
        billContext.setAction(ICmpConstant.SAVE);
        billContext.setbMain(true);
        billContext.setBillnum(IBillNumConstant.FUND_COLLECTION);
        billContext.setBilltype("Voucher");
        billContext.setMddBoId("ctm-cmp.cmp_fundcollection");
        billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003E", "资金收款单") /* "资金收款单" */);
        billContext.setSupportBpm(true);
        billContext.setTenant(AppContext.getCurrentUser().getTenant());
        billContext.setFullname(FundCollection.ENTITY_NAME);
        billContext.setEntityCode(IBillNumConstant.FUND_COLLECTION);
        return billContext;
    }

    public static List<Map<String, Object>> setTradeTypeByCode(String formId) throws Exception {
        BillContext bc = new BillContext();
        bc.setFullname("bd.bill.BillTypeVO");
        bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect(ICmpConstant.PRIMARY_ID);
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("form_id").eq(formId));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(bc, schema);
    }

    public static BigDecimal getBigDecimal(BizObject fundPayment, BigDecimal sum, boolean isCurrencyFlag, BizObject bizObject) {
        bizObject.set(ICmpConstant.WHETHER_SETTLE, ICmpConstant.CONSTANT_ONE);
        if (isCurrencyFlag) {
            bizObject.put(ICmpConstant.NATSUM, new BigDecimal(bizObject.get(ICmpConstant.ORISUM).toString()));
        } else {
            int olcmoneyDigit = 2;
            if (bizObject.get("natCurrency_moneyDigit") != null) {
                olcmoneyDigit = bizObject.getInteger("natCurrency_moneyDigit");
            } else if (Objects.nonNull(fundPayment.get("natCurrency"))) {
                try {
                    CurrencyDTO currency = CurrencyUtil.getCurrency(fundPayment.get("natCurrency"));
                    olcmoneyDigit = currency.getMoneydigit();
                } catch (Exception e) {
                    log.error("getBigDecimal:" + e.getMessage());
                }
            }
            if(fundPayment.getShort("exchangeRateOps") == 1){
                bizObject.put(ICmpConstant.NATSUM, BigDecimalUtils.safeMultiply(new BigDecimal(fundPayment.get(ICmpConstant.EXCHRATE).toString()), new BigDecimal(bizObject.get(ICmpConstant.ORISUM).toString()),olcmoneyDigit));
            } else {
                bizObject.put(ICmpConstant.NATSUM, BigDecimalUtils.safeDivide(new BigDecimal(bizObject.get(ICmpConstant.ORISUM).toString()),new BigDecimal(fundPayment.get(ICmpConstant.EXCHRATE).toString()),olcmoneyDigit));
            }
        }
        sum = BigDecimalUtils.safeAdd(sum, new BigDecimal(bizObject.get(ICmpConstant.ORISUM).toString()));
        bizObject.put(ICmpConstant.EXCHRATE, new BigDecimal(fundPayment.get(ICmpConstant.EXCHRATE).toString()));
        return sum;
    }

    public static void organizeAmountData(BizObject fundPayment, BigDecimal sum, boolean isCurrencyFlag) {
        fundPayment.put(ICmpConstant.ORISUM, sum);
        if (isCurrencyFlag) {
            fundPayment.put(ICmpConstant.NATSUM, sum);
        } else {
            if(fundPayment.getShort("exchangeRateOps") == 1){
                fundPayment.put(ICmpConstant.NATSUM, BigDecimalUtils.safeMultiply(new BigDecimal(fundPayment.get(ICmpConstant.EXCHRATE).toString()), new BigDecimal(fundPayment.get(ICmpConstant.ORISUM).toString())));
            } else {
                Short olcmoneyDigit;
                if(fundPayment.get("natCurrency_moneyDigit") == null){
                    olcmoneyDigit = 2;
                } else {
                    olcmoneyDigit = Short.valueOf(fundPayment.get("natCurrency_moneyDigit").toString());
                }
                fundPayment.put(ICmpConstant.NATSUM, BigDecimalUtils.safeDivide(new BigDecimal(fundPayment.get(ICmpConstant.ORISUM).toString()),new BigDecimal(fundPayment.get(ICmpConstant.EXCHRATE).toString()),olcmoneyDigit));
            }
        }
    }

    public static void csplPlanReferFilter(BillContext billContext, BillDataDto billDataDto, BizObject bill) throws Exception {
        //UI模版设置默认值的时候  bill是null 需要处理
        if (bill == null) {
            return;
        }
        String accentity = bill.get("accentity") != null ? bill.get("accentity").toString() : null;
//        String accentity = bill.get("accentity").toString();
        if (org.apache.commons.collections4.MapUtils.getString(bill, "currency") == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180078", "请先选择币种") /* "请先选择币种" */);
        }
        String currency = bill.get("currency") != null ? bill.get("currency").toString() : null;
//        String currency = bill.get("currency").toString();
        Date expectdate = bill.get("vouchdate");
        String dept = null;
        if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum()) || IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())) {
            String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
            List<BizObject> linesCheck = (List) bill.get(childrenFieldCheck);
            if (CollectionUtils.isNotEmpty(linesCheck)) {
                BizObject bizObject = linesCheck.get(0);
                dept = bizObject.get("dept");
            }
        } else {
            dept = bill.get("dept");
        }


        List<Long> resultData = QueryBaseDocUtils.queryPlanStrategyIsEnable(accentity, currency, expectdate, dept);
        if (CollectionUtils.isNotEmpty(resultData)) {
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_IN, resultData));
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("isEnd", ICmpConstant.QUERY_EQ, 1));
        } else {
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_EQ, null));
        }

        // ReceiptType：receive("收款", (short) 1), pay("付款", (short) 2)；
        if (IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())) {// 资金收款单
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("receiptType", ICmpConstant.QUERY_EQ, 1));
        } else if (IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum()) || IBillNumConstant.SALARYPAY.equals(billContext.getBillnum())) {// 资金付款单 薪资支付
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("receiptType", ICmpConstant.QUERY_EQ, 2));
        }

    }

    /**
     * 根据款项类型code查询款项类型
     *
     * @param code
     * @return
     * @throws Exception
     */
    public static QuickTypeVO queryQuickTypeByCode(String code) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(new String[]{"id", "code", "name", "stopstatus"});
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(new ConditionExpression[]{QueryCondition.name("code").eq(code)});
        querySchema.addCondition(queryConditionGroup);

        try {
            List<Map<String, Object>> quickTypeData = MetaDaoHelper.query("bd.paymenttype.PaymentTypeVO", querySchema, "finbd");
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(quickTypeData)) {
                Map<String, Object> bizObject = (Map) quickTypeData.get(0);
                QuickTypeVO quickTypeVO = new QuickTypeVO();
                quickTypeVO.setId(MapUtils.getLong(bizObject, "id"));
                quickTypeVO.setCode(MapUtils.getString(bizObject, "code"));
                quickTypeVO.setName(MapUtils.getString(bizObject, "name"));
//                quickTypeVO.setStopstatus(MapUtils.getBoolean(bizObject, "stopstatus"));
                return quickTypeVO;
            } else {
                return null;
            }
        } catch (Exception var7) {
            throw new BizException("500", InternationalUtils.getMessageWithDefault("UID:P_FIEPUB-BE_180BA92A0538002F", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CB", "获取款项类型档案异常!") /* "获取款项类型档案异常!" */), var7);
        }
    }

    public static List<Map<String, Object>> paymentTypes(List<String> ids) throws Exception {
        List<BizObject> paymentTypes;
        List<BizObject> paymentTypesCacheValue = paymentTypesCache.getIfPresent(ids);
        if (paymentTypesCacheValue != null) {
            paymentTypes = paymentTypesCacheValue;
        } else {
            IPaymentTypeService reduceWayService = DubboReferenceUtils.getDubboService(IPaymentTypeService.class, "finbd", (String) null, 300000);
            paymentTypes = reduceWayService.paymentTypes(ids);
            paymentTypesCache.put(ids, paymentTypes);
        }
        return (org.apache.commons.collections4.CollectionUtils.isEmpty(paymentTypes) ? Collections.emptyList() : new ArrayList(paymentTypes));
    }

    @Nullable
    public static String getString(String fullName, String billNum, Map<String, String> tableNameMap) {
        String tableName = null;
        String tableNameFromFullName = tableNameMap.get(fullName);
        String tableNameFromBillNum = tableNameMap.get(billNum);
        if (tableNameFromFullName != null) {
            tableName = tableNameFromFullName;
        } else if (tableNameFromBillNum != null) {
            tableName = tableNameFromBillNum;
        }
        return tableName;
    }

    /**
     * 校验会计主体的现金模块是否启用*
     *
     * @throws Exception
     */
    public static void checkPeriodFirstDate(String accentity) throws Exception {
        //资金组织适配，accentity可能是资金组织不是会计主体，需要根据资金组织查询会计主体;
        try {
            FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
            if (finOrgDTO != null) {
                accentity = finOrgDTO.getId();
            }
        } catch (Exception e) {
            log.error("根据资金组织查询会计主体错误,errorMsg:{}", e.getMessage());
        }
        Date periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);
        if (periodFirstDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101870"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F4","该资金组织现金管理模块未启用，不能保存单据！"));
        }
    }


    /**
     * 校验交易类型是否启用
     *
     * @throws Exception
     */
    public static void checkTradeTypeEnable(String tradeTypeId) throws Exception {
        if (Objects.isNull(tradeTypeId)) {
            return;
        }
        BdTransType tradeType = transTypeQueryService.findById(tradeTypeId);
        if (!Objects.isNull(tradeType)) {
            boolean enable = !Objects.isNull(tradeType.getEnable()) && 1 == tradeType.getEnable();
            if (!enable) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E026D3005280000", "该交易类型已停用，请重新选择！") /* "该交易类型已停用，请重新选择！" */);
            }
        }
    }

    /**
     * 校验款项类型是否启用
     *
     * @throws Exception
     */
    public static void checkQuickTypeEnable(Long quickType) throws Exception {
        if (Objects.isNull(quickType)) {
            return;
        }
        String quickTypeCacheKey = quickType.toString();
        List<Map<String, Object>> cacheValue = quickTypeListCache.getIfPresent(quickTypeCacheKey);
        List<Map<String, Object>> type;
        if (cacheValue != null) {
            type = cacheValue;
        } else {
            Map<String, Object> condition = new HashMap<>();
            condition.put("id", quickType);
            type =  QueryBaseDocUtils.queryQuickTypeByCondition(condition);
            quickTypeListCache.put(quickTypeCacheKey, type);
        }

        if (ValueUtils.isNotEmpty(type)) {
            boolean stopstatus = BooleanUtils.isFalse((Boolean) type.get(0).get("stopstatus"));
            if (!stopstatus) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E026C5205280004", "该款项类型已停用，请重新选择！") /* "	该款项类型已停用，请重新选择！" */);
            }
        }
    }

    /**
     * 校验款当前组织是否可以使用项目
     *
     * @throws Exception
     */
    public static boolean checkProject(String projectId, String currAccentity) throws Exception {
        if (StringUtils.isEmpty(projectId)) {
            return true;
        }
        ProjectDTO projectDTO = RemoteDubbo.get(IProjectService.class, IDomainConstant.MDD_DOMAIN_UCFBASEDOC).getByIdObj(projectId);
        Set<String> retorgids = FIDubboUtils.getDelegateHasSelf(projectDTO.getOrgId());
        //项目组织和当前组织存在委托关系，可以使用
        if (retorgids.contains(currAccentity)) {
            return true;
        }
        Map<String, Set<String>> orgRangeMap = RemoteDubbo.get(IProjectService.class, IDomainConstant.MDD_DOMAIN_UCFBASEDOC).queryOrgRangeSByProjectId(Arrays.asList(new String[]{projectId}));
        if (orgRangeMap == null || orgRangeMap.isEmpty()) {
            return false;
        }
        Set<String> orgRange = orgRangeMap.get(projectId);
        if (orgRange == null || orgRange.isEmpty()) {
            return false;
        }
        //项目组织为全局组织，或者项目组织使用范围包含当前组织，可以使用
        return orgRange.contains(currAccentity) || orgRange.contains(IStwbConstantForCmp.GLOBAL_ACCENTITY);
    }

    /**
     * 校验款当前组织是否可以使用费用项目
     *
     * @throws Exception
     */
    public static boolean checkExpenseitem(String expenseitem, String accentity) throws Exception {
        if (StringUtils.isEmpty(expenseitem)) {
            return true;
        }
        List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(expenseitem);
        if (!org.apache.commons.collections4.CollectionUtils.isEmpty(expenseItemList)) {
            Map<String, Object> expenseitemMap = expenseItemList.get(0);
            if (expenseitemMap == null || expenseitemMap.isEmpty() || expenseitemMap.get("propertybusiness") == null) {
                return false;
            }
            //组织权限校验:1.当前组织为资金组织委托的组织，2.当前组织为全局组织，3.当前组织为费用项目所属组织
            String orgfield = AuthUtil.getBDOrgField("bd.expenseitem.ExpenseItem");
            Object value = expenseitemMap.get(orgfield);
            Set<String> retorgids = FIDubboUtils.getDelegateHasSelf(accentity);
            if (!(accentity.equals(value) || IStwbConstantForCmp.GLOBAL_ACCENTITY.equals(value.toString()) || retorgids.contains(value.toString()))) {
                return false;
            }
            //判断是否勾选财资服务
            if (expenseitemMap.get("propertybusiness") instanceof Short && (Short) expenseitemMap.get("propertybusiness") == 1) {
                return true;
            } else if (expenseitemMap.get("propertybusiness") instanceof Boolean && (Boolean) expenseitemMap.get("propertybusiness") == true) {
                return true;
            } else if (expenseitemMap.get("propertybusiness") instanceof Integer && (Integer) expenseitemMap.get("propertybusiness") == 1) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean checkdept(String dept, String accentity) throws Exception {
        if (StringUtils.isEmpty(dept)) {
            return true;
        }
        BaseDeptDTO deptDTO = QueryBaseDocUtils.rpcQueryDeptById(dept);/* 暂不修改 静态方法*/
        if (ObjectUtils.isEmpty(deptDTO)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101013"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180024", "未查询到对应的部门，保存失败！") /* "未查询到对应的部门，保存失败！" */);
        }
        String parentorgid = deptDTO.getParentorgid();
        if (deptDTO.getEnable() != 1) {//0 未启用，1 启用，2 停用
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101012"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180023", "部门未启用，保存失败！") /* "部门未启用，保存失败！" */);
        } else {
            //获取有权限的组织
            Set<String> allOrg = getAuthOrg(accentity);
            boolean orgContains = false;
            if (allOrg.contains(parentorgid.toString())) {
                orgContains = true;
            }
            // 共享部门
            boolean deptContains = deptCheckApi.bizDeptBelongToOrgWithSharingSetting(accentity, dept, "all", InvocationInfoProxy.getTenantid());
            if (!deptContains && !orgContains) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101905"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D717BBC0500007B", "该部门无权限，非当前资金组织及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门") /* "该部门无权限，非当前资金组织及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门" */);
            }
            return true;
        }
    }

    public static boolean checkEmployee(String employee, String accentity, String dept, boolean isBusiness) throws Exception {
        if (StringUtils.isEmpty(employee)) {
            return true;
        }
        Map<String, Object> employeeMap = QueryBaseDocUtils.queryStaffById(employee);
        if (employeeMap == null || employeeMap.isEmpty()) {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101013"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180024","未查询到对应的部门，保存失败！") /* "未查询到对应的部门，保存失败！" */);
            if (isBusiness) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EB332EC04280008", "未查询到业务员信息，保存失败！") /* "未查询到业务员信息，保存失败！" */);
            } else {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EB3333405C80007", "未查询到人员信息，保存失败！") /* "未查询到人员信息，保存失败！" */);
            }
        }
        //校验业务员时，人员信息上业务员标识必须为true
        if (!(isBusiness && (employeeMap.get("biz_man_tag") != null && "1".equals(employeeMap.get("biz_man_tag").toString())))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EB3339A04280004", "查询到人员信息,但不是业务员，保存失败！") /* "查询到人员信息,但不是业务员，保存失败！" */);
        }
        //业务员部门校验
        Set<String> allOrg = getAuthOrg(accentity);
        //在有权限的组织下，校验通过
        if (allOrg.contains(employeeMap.get("unit_id").toString())) {
            return true;
        }
        //在有权限的部门下，校验通过
        if (checkAuthDept(employeeMap.get("dept_id").toString(), accentity)) {
            return true;
        }
        // 组织和部门都没有权限，提示报错
        if (isBusiness) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1ECD8D2405C80004", "该业务员无权限，非当前资金组织及通过核算委托关系的组织下的业务员，以及有职能共享关系的业务单元下的业务员") /* "该业务员无权限，非当前资金组织及通过核算委托关系的组织下的业务员，以及有职能共享关系的业务单元下的业务员" */);
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1ECD8CD805C8000A", "该员工无权限，非当前资金组织及通过核算委托关系的组织下的员工，以及有职能共享关系的业务单元下的员工") /* "该员工无权限，非当前资金组织及通过核算委托关系的组织下的员工，以及有职能共享关系的业务单元下的员工" */);
        }
    }

    /**
     * 根据当前组织获取有权限的核算委托组织和职能共享组织
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    public static Set<String> getAuthOrg(String accentity) throws Exception {
        // 委托组织
        HashSet<String> allOrg = new HashSet<>();
        Set<String> delegateOrg = FIDubboUtils.getDelegateHasSelf(accentity);
        allOrg.addAll(delegateOrg);
        // 共享组织
        Set<String> shareOrg = FIDubboUtils.getOrgShareHasSelf(delegateOrg.toArray(new String[0]));
        if (shareOrg != null && !shareOrg.isEmpty()) {
            allOrg.addAll(shareOrg);
        }
        return allOrg;
    }

    /**
     * 根据当前组织获取有权限的核算委托组织下部门的和职能共享的部门
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    public static Set<String> getAuthDept(String accentity) throws Exception {
        // 委托组织
        Set<String> delegateOrg = FIDubboUtils.getDelegateHasSelf(accentity);
        // 共享部门
        Set<String> shareDept = FIDubboUtils.getDeptShare(delegateOrg.toArray(new String[0]));
        return shareDept;
    }

    /**
     * 校验当前部门是否在当前组织的权限下，包含职能共享，不包含核算委托
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    public static boolean checkAuthDept(String dept, String accentity) throws Exception {
        return deptCheckApi.bizDeptBelongToOrgWithSharingSetting(accentity, dept, "all", InvocationInfoProxy.getTenantid());
    }

    /**
     * 校验当前业务组织是否在当前组织的权限下，包含职能共享，不包含核算委托
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    public static boolean checkAuthBusOrg(String bussOrg, String accentity) throws Exception {
        if (StringUtils.isEmpty(bussOrg)) {
            return true;
        }
        boolean orgContains = getAuthOrg(accentity).contains(bussOrg);
        if (!orgContains) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("\n" +
                    "UID:P_CM-BE_1F42410404E80003", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CC", "该业务组织无权限，非当前资金组织及通过核算委托关系的业务组织，以及有职能共享关系的业务组织") /* "该业务组织无权限，非当前资金组织及通过核算委托关系的业务组织，以及有职能共享关系的业务组织" */) /* "该业务组织无权限，非当前资金组织及通过核算委托关系的业务组织，以及有职能共享关系的业务组织" */);
        }
        return true;
    }
}
