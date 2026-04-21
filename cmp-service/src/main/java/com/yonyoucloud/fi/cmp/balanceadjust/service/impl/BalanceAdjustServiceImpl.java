package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.cashhttp.CashHttpBankEnterpriseLinkVo;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.basedoc.model.CurrencyDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding_b;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.rule.JournalQueryRule;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.rpc.rule.DailyComputezInit;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.process.ProcessInfo;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.process.ProcessVo;
import com.yonyoucloud.fi.cmp.vo.BalanceAdjustQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2019/4/20 0020.
 */
@Service
@Slf4j
public class BalanceAdjustServiceImpl implements BalanceAdjustService {

    private static final String ACCENTITY = "accentity";

    private static final String ITEM_NAME = "itemName";

    private static final String VALUE1 = "value1";

    private static final String VALUE2 = "value2";

    private static final String BANK_ACCOUNT = "bankaccount";

    private static final String DZ_DATE = "dzdate";

    private static final String SETTLE_STATUS = "settlestatus";

    private static final String CURRENCY = "currency";

    private static final String AUDIT_STATUS = "auditstatus";

    private static final String INIT_FLAG = "initflag";

    private static final String CREDITORI_SUM = "creditoriSum";

    private static final String DEBITORI_SUM = "debitoriSum";

    private static final String BANKTZYE = "banktzye";

    private static final String BANKYE = "bankye";//银行余额

    public static  String BANKRECONCILIATIONSCHEME = "bankreconciliationscheme";

    public static String ENABLEDATE = "enableDate";

    public  static String JOURNALYE = "journalye";
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CmpCheckService cmpCheckService;
    @Autowired
    AutoConfigService autoConfigService;
    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    AccountHistoryBalanceService accountHistoryBalanceService;
    @Autowired
    AccountRealtimeBalanceService accountRealtimeBalanceService;

    public BalanceAdjustServiceImpl(BaseRefRpcService baseRefRpcService,EnterpriseBankQueryService enterpriseBankQueryService,CmpCheckService cmpCheckService){
        this.baseRefRpcService = baseRefRpcService;
        this.enterpriseBankQueryService = enterpriseBankQueryService;
        this.cmpCheckService = cmpCheckService;
    }

    @Override
    public CtmJSONObject query(CtmJSONObject obj, Boolean initFlag) throws Exception{
        if (obj == null){
            throw new CtmException("查询条件对象 obj 不能为空");//@notranslate
        }
        List commonVOs = (List) obj.get("commonVOs");
        String bankaccount = null;
        String currency = null;
        for(int i=0;i<commonVOs.size();i++){
            Map map = (Map) commonVOs.get(i);
            if(BANK_ACCOUNT.equals(map.get(ITEM_NAME))){
                if (map.get(VALUE1) == null){
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20ACE17604A00015", "查询条件银行账户不能为空") /* "查询条件银行账户不能为空" */);
                }
                bankaccount = String.valueOf(map.get(VALUE1));
            }
            if(CURRENCY.equals(map.get(ITEM_NAME))){
                if (map.get(VALUE1) == null){
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20ACE17604A00016", "查询条件币种不能为空") /* "查询条件币种不能为空" */);
                }
                currency = String.valueOf(map.get(VALUE1));
            }
        }
        // 加锁的账号信息
        String accountInfo = bankaccount + "|" + currency;
        // 加锁信息：账号+行为
        String lockKey = accountInfo + ICmpConstant.QUERYBALANCEBATCH;
        try {
            return CtmLockTool.executeInOneServiceLock(lockKey,60*60*2L, TimeUnit.SECONDS,(int lockStatus)->{
                try {
                    return getCtmJSONObject(obj, initFlag);
                } catch (Exception e) {
                    // 记录异常日志
                    log.error("Error occurred while getting CTM JSON object", e);
                    throw e; // 重新抛出异常以便上层处理
                }
            });
        } catch (Exception e) {
            // 处理 executeInOneServiceLock 抛出的异常
            log.error("Error occurred while executing in one service lock", e);
            throw e; // 重新抛出异常以便上层处理
        }

    }

    private @NotNull CtmJSONObject getCtmJSONObject(CtmJSONObject obj, Boolean initFlag) throws Exception {
        String accentity = null;
        String bankaccount = null;
        Date dzdate = null;
        Date enableDate = null;
        String currency = null;
        Integer  reconciliationdatasource =2; //对账来源  1 总账  2 现金
        List commonVOs = (List) obj.get("commonVOs");
        Long bankreconciliationscheme = null;
        BigDecimal journalye  = BigDecimal.ZERO;//企业日记账余额
        BigDecimal bankye = BigDecimal.ZERO;//银行对账单余额
        for(int i=0;i<commonVOs.size();i++){
            Map map = (Map) commonVOs.get(i);
            if(IBussinessConstant.ACCENTITY.equals(map.get(ITEM_NAME))){
                accentity = String.valueOf(map.get(VALUE1)).replace("[","").replace("]","");
                //单组织逻辑
                if(FIDubboUtils.isSingleOrg()){
                    BizObject singleOrg = FIDubboUtils.getSingleOrg();
                    if(singleOrg!=null){
                        accentity = singleOrg.get("id");
                    }
                }

                if (StringUtil.isEmpty(accentity)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100763"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050018", "资金组织不能为空") /* "资金组织不能为空" */);
                }
            }
            if(JournalQueryRule.BANKRECONCILIATIONSCHEME.equals(map.get(ITEM_NAME))){
                Long id = Long.valueOf(map.get(VALUE1).toString());
                BizObject bizObject = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,id);
                if(ValueUtils.isEmpty(bizObject)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100764"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F3","未查询到该对账方案相关信息,请检查!") /* "未查询到该对账方案相关信息,请检查!" */);
                }
                reconciliationdatasource = bizObject.get(JournalQueryRule.RECONCILIATIONDATASOURCE);
                enableDate = bizObject.get(ENABLEDATE);
                bankreconciliationscheme = id;
            }
            if(BANK_ACCOUNT.equals(map.get(ITEM_NAME))){
                bankaccount = String.valueOf(map.get(VALUE1));
            }
            if(CURRENCY.equals(map.get(ITEM_NAME))){
                currency = String.valueOf(map.get(VALUE1));
            }
            if(DZ_DATE.equals(map.get(ITEM_NAME))){
                dzdate = DateUtils.strToDate(String.valueOf(map.get(VALUE1)));
            }
            if(initFlag){
                if(JOURNALYE.equals(map.get(ITEM_NAME))){
                    if(ObjectUtils.isNotEmpty(map.get(VALUE1))){
                        journalye = BigDecimal.valueOf(Double.parseDouble(map.get(VALUE1).toString()));
                    }
                }

                if(BANKYE.equals(map.get(ITEM_NAME))){
                    if(ObjectUtils.isNotEmpty(map.get(VALUE1))) {
                        bankye = new BigDecimal(Double.parseDouble(map.get(VALUE1).toString()));
                    }
                }
            }
        }
        if(FIDubboUtils.isSingleOrg()){
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if(singleOrg != null){
                accentity = singleOrg.get("id");
            }
        }
        if(initFlag){
            dzdate = DateUtils.dateAddDays(enableDate,-1);
        }else{
            if(dzdate == null ) {
                dzdate = DateUtils.getNow();
            }
        }
        if(currency == null){
            //todo 加缓存
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankaccount);
            currency = enterpriseBankAcctVO.getId();
        }
        //总账凭证余额查询需要传递makeTime
        LinkedHashMap<String,String> makeTimeMap = new LinkedHashMap<>();
        makeTimeMap.put("itemName","makeTime");
        makeTimeMap.put("value2",DateUtils.dateFormat(dzdate,"yyyy-MM-dd"));
        if (obj.containsKey("commonVOs")){
            CtmJSONArray ctmJSONArray = obj.getJSONArray("commonVOs");
            ctmJSONArray.add(makeTimeMap);
            obj.put("commonVOs",ctmJSONArray);
        }

        //获取数据===================================================
        //todo 加缓存
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);
        BigDecimal debitoriSumJour;//借方原币金额
        BigDecimal creditoriSumJour;//贷方原币金额
        //1、查询日记账数据  计算借方原币金额和贷方原币金额
        BigDecimal debitSum;//借方金额
        BigDecimal creditSum;//贷方金额
        //返会的数据计算组装==================================
        CtmJSONObject jSONObject = new CtmJSONObject();
        if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){//总帐
            CtmJSONObject result = CmpCommonUtil.getVoucherBalance(obj, initFlag,accentity,bankaccount,bankreconciliationscheme,currency,false);
            debitoriSumJour = result.getBigDecimal("debitoriSum");
            creditoriSumJour = result.getBigDecimal("creditoriSum");
            if(!initFlag && result.getBigDecimal("balance")!=null){
                journalye  = result.getBigDecimal("balance").setScale(currencyDTO.getMoneydigit(),moneyRound);
            }
            jSONObject.put("voucherDetailInfoList",result.get("voucherDetailInfoList"));
        }else{
            //initFlag:false处理对账单---余额表; true处理期初未达---余额表
            BalanceAdjustQueryVO queryVo = BalanceAdjustQueryVO.builder().initFlag(initFlag).accentity(accentity).bankaccount(bankaccount)
                        .bankreconciliationscheme(bankreconciliationscheme).currency(currency).enableDate(enableDate).dzdate(dzdate)
                        .journalye(journalye).bankye(bankye).build();
            getJournalBalancesResultBuild(queryVo,jSONObject, initFlag);
            debitoriSumJour = queryVo.getDebitoriSumJour();
            creditoriSumJour = queryVo.getCreditoriSumJour();
            journalye = queryVo.getJournalye();
        }
        //银行账户所属组织非前端传递时，修改为账户真实的所属组织
        //todo 加缓存
        EnterpriseBankAcctVO bankAcctVO = enterpriseBankQueryService.findById(bankaccount);
        //CZFW-384880 期初数据不走该判断
        if (!initFlag && bankAcctVO != null && !accentity.equals(bankAcctVO.getOrgid())) {
            accentity = bankAcctVO.getOrgid();
        }

        //获取银行对账单相关信息
        Map<String,BigDecimal> bankResult = getBankReconBalances(initFlag,accentity,bankaccount,bankreconciliationscheme,currency,enableDate,dzdate,reconciliationdatasource);
        creditSum = bankResult.get("creditoriSum");
        debitSum = bankResult.get("debitoriSum");
        if(!initFlag){
            bankye = bankResult.get("bankye");
        }

        jSONObject.put("journaldate",dzdate);
        jSONObject.put("journalye",journalye.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//企业日记账余额
        jSONObject.put("journalyhys",creditSum.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//银行已收企业未收
        jSONObject.put("journalyhyf",debitSum.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//银行已付企业未付
        //企业方调整后余额
        BigDecimal journaltzye = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(journalye,creditSum),debitSum);
        jSONObject.put("journaltzye",journaltzye.setScale(currencyDTO.getMoneydigit(),moneyRound));//调整后余额
        //202509 银行账户余额取值逻辑调整 1.直联账户获取历史账户余额；2非直联账户获取当前余额 计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）
        if (!initFlag || BigDecimal.ZERO.compareTo(bankye) == 0) {//非期初余额调节时 或期初余额为0时 ，才用银行对账单余额替换期初余额
            BankAccountSettingVO bankAccountSettingVO = new BankAccountSettingVO();
            bankAccountSettingVO.setAccentity(accentity);
            bankAccountSettingVO.setCurrency(currency);
            bankAccountSettingVO.setBankaccount(bankaccount);
            bankAccountSettingVO.setBankreconciliationscheme(bankreconciliationscheme);
            bankAccountSettingVO.setEnableDateStr(DateUtils.dateFormat(dzdate, "yyyy-MM-dd"));
            CtmJSONObject ctmJSONObject = this.calculateBankAccountBalance(bankAccountSettingVO);
            bankye = ctmJSONObject.getBigDecimal("bankye");
            //直联账户是否银行账户余额为空
            jSONObject.put("isEmptyBalance",ctmJSONObject.getBoolean("isEmptyBalance"));
            jSONObject.put("bankye",bankye.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//银行对账单余额
        } else {
            //直联账户是否银行账户余额为空 默认否
            jSONObject.put("isEmptyBalance",false);
            jSONObject.put("bankye",bankye.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//银行对账单余额
        }
        jSONObject.put("bankdate",dzdate);
        jSONObject.put("bankqyys",debitoriSumJour.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//企业已收银行未收
        jSONObject.put("bankqyyf",creditoriSumJour.setScale(currencyDTO.getMoneydigit(),moneyRound).toPlainString());//企业已付银行未付
        //银行方调整后余额
        BigDecimal banktzye = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(bankye,debitoriSumJour),creditoriSumJour);
        jSONObject.put(BANKTZYE,banktzye.setScale(currencyDTO.getMoneydigit(),moneyRound));
        jSONObject.put("moneydigit",currencyDTO.getMoneydigit());//金额精度
        if(!jSONObject.getBoolean("isEmptyBalance") && jSONObject.getBigDecimal("journaltzye").compareTo(jSONObject.getBigDecimal("banktzye"))==0){
            jSONObject.put("balenceState",1);//已平
        }else{
            jSONObject.put("balenceState",2);//未平
        }
        jSONObject.put("moneydigit",currencyDTO.getMoneydigit());
        return jSONObject;
    }

    /**
     * @param obj
     * @return
     * @throws Exception
     */
    @Override
    public JsonNode queryBalanceState(CtmJSONObject obj) throws Exception {
        ObjectNode jSONObject = JSONBuilderUtils.createJson();
        CtmJSONObject paramObj = obj.getJSONObject("paramObj");
        String bankYe = paramObj.getString("bankye");
        try{
            //BigDecimal bigBankYe = new BigDecimal(bankYe);
        }catch (Exception e){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100765"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19DE85D604900007","银行对账单余额录入格式错误!") /* "银行对账单余额录入格式错误!" */);
        }
        BigDecimal bankye = paramObj.getBigDecimal("bankye");
        BigDecimal bankqyys = paramObj.getBigDecimal("bankqyys");
        BigDecimal bankqyyf = paramObj.getBigDecimal("bankqyyf");
        BigDecimal journaltzye = paramObj.getBigDecimal("journaltzye");
        String bankaccount = paramObj.getString("bankaccount");
        String currency = paramObj.getString("currency");
        if(bankye != null && bankqyys != null && bankqyyf != null  && journaltzye != null && !StringUtils.isEmpty(bankaccount)){
//            Map<String,Object> map = QueryBaseDocUtils.queryEnterpriseBankAccountById(bankaccount);
//            String currency = (String) map.get(CURRENCY);
            //精度处理
            CurrencyDTO currencyDTO = CurrencyUtil.getCurrency(currency);
            RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);
            BigDecimal banktzye = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(bankye, bankqyys), bankqyyf);
            if(banktzye.compareTo(journaltzye) == 0){
                jSONObject.put("balenceState", 1);//已平
            }else{
                jSONObject.put("balenceState", 2);//未平
            }
            jSONObject.put("banktzye", banktzye.setScale(currencyDTO.getMoneydigit(), moneyRound).toPlainString());//银行调整余额
            jSONObject.put("bankye",bankye.setScale(currencyDTO.getMoneydigit(), moneyRound).toPlainString());//银行对账单余额
            jSONObject.put("moneydigit",currencyDTO.getMoneydigit());//金额精度
        }
        return jSONObject;
    }

    /**
     * 银行账户余额查询
     * 优先级1：账户历史余额；2银行流水上余额*
     * @param bankVoucherInfoQueryVO
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject getBankBalanceAmount(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        BankAccountSettingVO bankAccountSettingVO = new BankAccountSettingVO();
        bankAccountSettingVO.setAccentity(bankVoucherInfoQueryVO.getAccentity());
        bankAccountSettingVO.setBankaccount(bankVoucherInfoQueryVO.getBankaccount());
        bankAccountSettingVO.setCurrency(bankVoucherInfoQueryVO.getCurrency());
        bankAccountSettingVO.setBankreconciliationscheme(Long.parseLong(bankVoucherInfoQueryVO.getReconciliationScheme()));
        bankAccountSettingVO.setEnableDateStr(bankVoucherInfoQueryVO.getCheckEndDate());
        //202509 余额调节表银行账户余额取值逻辑调整
        return this.calculateBankAccountBalance(bankAccountSettingVO);
    }

    @Override
    public CtmJSONObject getJournalBalanceAmount(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        CtmJSONObject jSONObject = new CtmJSONObject();
        BigDecimal journalye = BigDecimal.ZERO;
        BigDecimal bankye = BigDecimal.ZERO;
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,bankVoucherInfoQueryVO.getReconciliationScheme());
        BalanceAdjustQueryVO queryVo = BalanceAdjustQueryVO.builder().initFlag(false)
                .accentity(bankVoucherInfoQueryVO.getAccentity())
                .bankaccount(bankVoucherInfoQueryVO.getBankaccount())
                .bankreconciliationscheme(Long.parseLong(bankVoucherInfoQueryVO.getReconciliationScheme()))
                .currency(bankVoucherInfoQueryVO.getCurrency())
                .enableDate(bankReconciliationSetting.getEnableDate())
                .dzdate(DateUtils.strToDate(bankVoucherInfoQueryVO.getCheckEndDate()))
                .journalye(journalye)
                .bankye(bankye)
                .build();
        getJournalBalancesResultBuild(queryVo,jSONObject, false);
        journalye = queryVo.getJournalye();
        CtmJSONObject result = new CtmJSONObject();
        result.put("journalye",journalye);
        result.put("voucherDetailInfoList",jSONObject.get("voucherDetailInfoList"));
        return result;
    }

    @Override
    public CtmJSONObject getVoucherBalanceAmount(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        //过滤条件
        CtmJSONObject conditionJson = new CtmJSONObject();
        CtmJSONArray ctmJSONArray = new CtmJSONArray();
        //业务日期
        LinkedHashMap<String,String> makeTimeMap = new LinkedHashMap<>();
        makeTimeMap.put("itemName","makeTime");
        makeTimeMap.put("value2", bankAccountSettingVO.getEnableDateStr());
        ctmJSONArray.add(makeTimeMap);

        //会计主体
        LinkedHashMap<String,String> accentityMap = new LinkedHashMap<>();
        accentityMap.put("itemName","accentity");
        accentityMap.put("value1",bankAccountSettingVO.getAccentity());
        ctmJSONArray.add(accentityMap);

        //银行账户
        LinkedHashMap<String,String> bankaccountMap = new LinkedHashMap<>();
        bankaccountMap.put("itemName","bankaccount");
        bankaccountMap.put("value1",bankAccountSettingVO.getBankaccount());
        ctmJSONArray.add(bankaccountMap);

        //币种
        LinkedHashMap<String,String> currencyMap = new LinkedHashMap<>();
        currencyMap.put("itemName","currency");
        currencyMap.put("value1",bankAccountSettingVO.getCurrency());
        ctmJSONArray.add(currencyMap);

        //dzdate
        LinkedHashMap<String,String> sealflagMap = new LinkedHashMap<>();
        sealflagMap.put("itemName","dzdate");
        sealflagMap.put("value1",bankAccountSettingVO.getEnableDateStr());
        ctmJSONArray.add(sealflagMap);

        //对账方案id
        LinkedHashMap<String,String> bankreconciliationschemeMap = new LinkedHashMap<>();
        bankreconciliationschemeMap.put("itemName","bankreconciliationscheme");
        bankreconciliationschemeMap.put("value1",bankAccountSettingVO.getBankreconciliationscheme().toString());
        ctmJSONArray.add(bankreconciliationschemeMap);

        conditionJson.put("commonVOs",ctmJSONArray);

        CtmJSONObject result = CmpCommonUtil.getVoucherBalance(conditionJson, false,
                bankAccountSettingVO.getAccentity(),bankAccountSettingVO.getBankaccount(),bankAccountSettingVO.getBankreconciliationscheme(),bankAccountSettingVO.getCurrency(),false);
        //凭证余额
        BigDecimal journalye  = result.getBigDecimal("balance");

        CtmJSONObject voucherBalance = new CtmJSONObject();
        voucherBalance.put("journalye",journalye);
        voucherBalance.put("voucherDetailInfoList",result.get("voucherDetailInfoList"));
        return voucherBalance;
    }

    @Override
    public CtmJSONObject getBankAccountHistoryBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        if (bankAccountSettingVO == null || StringUtils.isEmpty(bankAccountSettingVO.getBankaccount()) || StringUtils.isEmpty(bankAccountSettingVO.getCurrency())
                ||  StringUtils.isEmpty(bankAccountSettingVO.getEnableDateStr())){
            throw new CtmException("入参accentity，bankaccount，currency，enableDateStr不能为空");//@notranslate
        }
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(bankAccountSettingVO.getBankaccount())));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").eq(bankAccountSettingVO.getCurrency())));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("balancedate").eq(bankAccountSettingVO.getEnableDateStr())));
        //银行余额查询要限制 首次查询=0
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("first_flag").eq("0")));
        QuerySchema querySchema = QuerySchema.create().addSelect("acctbal");
        querySchema.addCondition(condition);
        List<Map<String, Object>> list =  MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchema);
        CtmJSONObject result = new CtmJSONObject();
        if (list != null && list.size() > 0){
            //币种获取
            CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bankAccountSettingVO.getCurrency());
            RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(bankAccountSettingVO.getCurrency(), 1);
            BigDecimal bankye = (BigDecimal) list.get(0).get("acctbal");
            bankye = bankye.setScale(currencyDTO.getMoneydigit(),moneyRound);
            //当银行账户余额为正数时或未获取到时，默认银行方余额方向为：贷； 当银行账户余额为负数时，默认银行方余额方向为：借
            if (bankye.compareTo(BigDecimal.ZERO) >= 0){
                result.put("direction", DirectionJD.Credit.getValue());
            }else {
                result.put("direction", DirectionJD.Debit.getValue());
            }
            result.put("bankye",bankye.abs());
            //标记是否查询不到余额
            result.put("isEmptyBalance",false);
            return result;
        }

        //没有历史余额，余额为0，默认余额方向为贷
        result.put("bankye",BigDecimal.ZERO);
        result.put("direction", DirectionJD.Credit.getValue());
        //标记是否查询不到余额
        result.put("isEmptyBalance",true);
        return result;
    }

    @Override
    public CtmJSONObject calculateBankAccountBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //1.直联账户或者结算中心内部户，获取银行账户历史余额
        if (cmCommonService.getOpenFlag(bankAccountSettingVO.getBankaccount()) || queryAccountType(bankAccountSettingVO.getBankaccount())){
            CtmJSONObject bankBalanceResult = this.getBankAccountHistoryBalance(bankAccountSettingVO);
            //借方向，需要取余额的相反数
            if (DirectionJD.Debit.getValue() == bankBalanceResult.getShort("direction")){
                result.put("bankye",bankBalanceResult.getBigDecimal("bankye").negate());
            }else {
                result.put("bankye",bankBalanceResult.getBigDecimal("bankye"));
            }
            result.put("isEmptyBalance",bankBalanceResult.getBoolean("isEmptyBalance"));
        }else {
            //2.银行账户为非直联 计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）
            result.put("isEmptyBalance",false);
            //查询当期余额，不存在则查询期初未达项的期初余额
            CtmJSONObject currentBalance = this.getBankAccountCurrentBalance(bankAccountSettingVO);
            if (currentBalance.getBoolean("isHasBalance")){
                result.put("bankye",currentBalance.getBigDecimal("bankye"));
            }else {
                result.put("bankye",this.getBankAccountOpeningBalance(bankAccountSettingVO).getBigDecimal("bankye"));
            }
        }

        return result;
    }

    @Override
    public CtmJSONObject getBankAccountCurrentBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        //a、计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）；期初余额=上期审批通过的调节表银行方余额
        CtmJSONObject result = new CtmJSONObject();
        //先查询最近的审批通过的余额调节表余额
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("verifystate").eq(VerifyState.COMPLETED.getValue()),//审批通过
                QueryCondition.name("currency").eq(bankAccountSettingVO.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(bankAccountSettingVO.getBankaccount()),//银行账号
                QueryCondition.name("bankreconciliationscheme").eq(bankAccountSettingVO.getBankreconciliationscheme()),//对账方案id
                QueryCondition.name("dzdate").elt(bankAccountSettingVO.getEnableDateStr()) //查询之前的余额调节表
        );
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("dzdate", "desc"));
        List<BalanceAdjustResult> checkList = MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
        //没有满足条件的余额调节表直接返回
        if (checkList == null || checkList.size() == 0){
            result.put("isHasBalance",false);
            return result;
        }
        BalanceAdjustResult balanceAdjustResult = checkList.get(0);
        //若余额调节表第一条的dzdate和截止日期相同，则直接取该余额调节表的银行方余额
        if (DateUtils.dateFormat(balanceAdjustResult.getDzdate(), "yyyy-MM-dd").equals(bankAccountSettingVO.getEnableDateStr())){
            result.put("bankye",balanceAdjustResult.getBankye());
            result.put("isHasBalance",true);
            return result;
        }
        String dzdateBeginStr =DateUtils.dateFormat( DateUtils.dateAddDays(balanceAdjustResult.getDzdate(),1), "yyyy-MM-dd");
        QuerySchema querySchemaSum = QuerySchema.create().addSelect("sum(debitamount) as debitamountSum,sum(creditamount) as creditamountSum ,bankaccount,currency" );
        QueryConditionGroup sumGroup = QueryConditionGroup.and(
                QueryCondition.name("currency").eq(bankAccountSettingVO.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(bankAccountSettingVO.getBankaccount()),//银行账号
                QueryCondition.name("dzdate").between(dzdateBeginStr,bankAccountSettingVO.getEnableDateStr())
        );
        //无需处理是否参与余额计算，为否时过滤处理状态serialdealtype=5的数据
        boolean isNoProcess = BankreconciliationUtils.isNoProcess(bankAccountSettingVO.getAccentity());
        if (!isNoProcess){
            QueryConditionGroup g1 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").not_eq(5));
            QueryConditionGroup g2 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").is_null());
            sumGroup.appendCondition(QueryConditionGroup.or(g1,g2));
        }
        querySchemaSum.addCondition(sumGroup);
        querySchemaSum.addGroupBy("bankaccount,currency");
        List<Map<String, Object>> bankReconciliationSumList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchemaSum);
        if(!CollectionUtils.isEmpty(bankReconciliationSumList)){
            Map<String, Object> map = bankReconciliationSumList.get(0);
            BigDecimal debitamountSum = map.get("debitamountSum")!=null?new BigDecimal(map.get("debitamountSum").toString()):BigDecimal.ZERO;
            BigDecimal creditamountSum = map.get("creditamountSum")!=null?new BigDecimal(map.get("creditamountSum").toString()):BigDecimal.ZERO;
            //计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）
            BigDecimal bankye = BigDecimalUtils.safeAdd(balanceAdjustResult.getBankye(),BigDecimalUtils.safeSubtract(creditamountSum,debitamountSum));
            result.put("bankye",bankye);
        }else { //为空，则没有发生额，直接取上一个余额调节表的银行方余额
            result.put("bankye",balanceAdjustResult.getBankye());
        }
        result.put("isHasBalance",true);
        return result;
    }

    @Override
    public CtmJSONObject getBankAccountOpeningBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        //a、计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）；期初余额=期初未达项中的银行方期初余额
        CtmJSONObject result = new CtmJSONObject();
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("bankreconciliationscheme").eq(bankAccountSettingVO.getBankreconciliationscheme()),
                QueryCondition.name("bankaccount").eq(bankAccountSettingVO.getBankaccount()),
                QueryCondition.name("currency").eq(bankAccountSettingVO.getCurrency())
        );
        schema.addCondition(group);
        List<BizObject> openingOutstandingList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, schema, null);
        //银行方期初余额
        BigDecimal openingBalance = BigDecimal.ZERO;
        String beginDateStr; //发生额统计开始日期
        if (CollectionUtils.isNotEmpty(openingOutstandingList)){
            BizObject BizObject = openingOutstandingList.get(0);
            OpeningOutstanding openingOutstanding = new OpeningOutstanding();
            openingOutstanding.init(BizObject);
            if (Short.parseShort(openingOutstanding.get("bankdirection").toString()) == Direction.Debit.getValue()) {
                openingBalance = openingOutstanding.getBankinitoribalance().negate();
            }else {
                openingBalance = openingOutstanding.getBankinitoribalance();
            }
            beginDateStr = DateUtils.dateFormat(openingOutstanding.getEnableDate(), "yyyy-MM-dd");
        }else {
            BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME, bankAccountSettingVO.getBankreconciliationscheme());
            beginDateStr = DateUtils.dateFormat(bankReconciliationSetting.getEnableDate(), "yyyy-MM-dd");
        }
        //统计银行流水发生额
        QuerySchema querySchemaSum = QuerySchema.create().addSelect("sum(debitamount) as debitamountSum,sum(creditamount) as creditamountSum ,bankaccount,currency" );
        QueryConditionGroup sumGroup = QueryConditionGroup.and(
                QueryCondition.name("currency").eq(bankAccountSettingVO.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(bankAccountSettingVO.getBankaccount()),//银行账号
                QueryCondition.name("dzdate").between(beginDateStr,bankAccountSettingVO.getEnableDateStr())
        );
        //无需处理是否参与余额计算，为否时过滤处理状态serialdealtype=5的数据
        boolean isNoProcess = BankreconciliationUtils.isNoProcess(bankAccountSettingVO.getAccentity());
        if (!isNoProcess){
            QueryConditionGroup g1 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").not_eq(5));
            QueryConditionGroup g2 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").is_null());
            sumGroup.appendCondition(QueryConditionGroup.or(g1,g2));
        }
        querySchemaSum.addCondition(sumGroup);
        querySchemaSum.addGroupBy("bankaccount,currency");
        List<Map<String, Object>> bankReconciliationSumList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchemaSum);
        if(!CollectionUtils.isEmpty(bankReconciliationSumList)){
            Map<String, Object> map = bankReconciliationSumList.get(0);
            BigDecimal debitamountSum = map.get("debitamountSum")!=null?new BigDecimal(map.get("debitamountSum").toString()):BigDecimal.ZERO;
            BigDecimal creditamountSum = map.get("creditamountSum")!=null?new BigDecimal(map.get("creditamountSum").toString()):BigDecimal.ZERO;
            //计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）
            BigDecimal bankye = BigDecimalUtils.safeAdd(openingBalance,BigDecimalUtils.safeSubtract(creditamountSum,debitamountSum));
            result.put("bankye",bankye);
        }else { //为空，则没有发生额，直接取银行方期初余额
            result.put("bankye",openingBalance);
        }

        return result;
    }

    @Override
    public CtmJSONObject recalculateBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bankAccountSettingVO.getCurrency());
        //查询银行账户余额
        //1.直联账户或者结算中心内部户，调用账户历史余额拉取的接口，获取当前账户截止日当天的余额
        boolean isOpenAccount =  cmCommonService.getOpenFlag(bankAccountSettingVO.getBankaccount()); //直联账户判断
        boolean isInnerAccount = queryAccountType(bankAccountSettingVO.getBankaccount()); //结算中心内部户判断
        if (isOpenAccount || isInnerAccount){
            Date startDate = DateUtils.dateParse(bankAccountSettingVO.getEnableDateStr(), "yyyy-MM-dd");
            Date endDate = DateUtils.dateParse(bankAccountSettingVO.getEnableDateStr(), "yyyy-MM-dd");
            Date nowDate = DateUtils.getNowDateShort2();
            if (endDate.compareTo(nowDate) > 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00181", "账户历史同步余额日期必须在系统当前日期之前！") /* "账户历史同步余额日期必须在系统当前日期之前！" */);
            }
            String uid = UUID.randomUUID().toString(); //用来记录拉取进度的唯一标识
            //账户实时余额拉取；截止日期等于当前时间
            if (endDate.compareTo(nowDate) == 0) {
                try {
                    CtmJSONObject params = new CtmJSONObject();
                    params.put("uid", uid);
                    params.put("enterpriseBankAccount", Arrays.asList(bankAccountSettingVO.getBankaccount()));
                    params.put("accEntity", Arrays.asList(bankAccountSettingVO.getAccentity()));
                    params.put("currency", bankAccountSettingVO.getCurrency());
                    // 修改调用方式，等待异步任务完成
                    Future<CtmJSONObject> future = accountRealtimeBalanceService.queryAccountBalanceUnNeedUkeyAsync(params);
                    // 等待异步任务完成
                    future.get();
                }catch (Exception e){
                    log.error("余额重算异常，同步银行账户实时余额失败：", e);
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_217B378204100009", "余额重算拉取账户实时余额过程中出现异常：%s") /* "余额重算拉取账户实时余额过程中出现异常：%s" */, e.getMessage()));
                }
            }
            //账户历史余额拉取；截止日期早于当前时间
            if (endDate.compareTo(nowDate) < 0){
                ProcessUtil.initProcessWithAccountNum(uid, 1);
                List<EnterpriseBankAcctVO> enterpriseBankAcctList = new ArrayList<>();
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankAccountSettingVO.getBankaccount());
                enterpriseBankAcctList.add(enterpriseBankAcctVO);
                try {
                    if (isOpenAccount){
                        CtmJSONObject queryParams = new CtmJSONObject();
                        //拼装直联账户查询参数
                        List<String> dateList = new ArrayList<>();
                        dateList.add(bankAccountSettingVO.getEnableDateStr());//开始日期
                        dateList.add(bankAccountSettingVO.getEnableDateStr());//结束日期
                        queryParams.put("betweendate",dateList);
                        //获取直连账户查询信息组装
                        List<CashHttpBankEnterpriseLinkVo> httpList = accountHistoryBalanceService.querHttpAccount(enterpriseBankAcctList, queryParams, false);
                        // 过滤数据：只保留 curr_code 与传入币种一致的数据
                        if (CollectionUtils.isNotEmpty(httpList)) {
                            httpList = httpList.stream()
                                    .filter(vo -> vo != null && currencyDTO.getCode().equals(vo.getCurr_code()))
                                    .collect(java.util.stream.Collectors.toList());
                        }
                        //直联账户历史余额拉取
                        accountHistoryBalanceService.doSyncHistoryAccountBalance(httpList, uid);
                    }
                    if (isInnerAccount){
                        //结算中心内部户历史余额直联拉取
                        accountHistoryBalanceService.syncHistoryInnerAccountBalance(startDate, endDate, enterpriseBankAcctList, uid);
                    }
                }catch (Exception e) {
                    log.error("余额重算异常，同步银行账户历史余额失败：", e);
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20ACE17604A00018", "账户历史余额拉取异常：") /* "账户历史余额拉取异常：" */ + e.getMessage());
                } finally {
                    //存在两个账户都不存在的情况 最后需要将进度置为100%
                    ProcessUtil.completedResetCount(uid);
                }
            }
            //判断拉取过程中是否存在异常
            String process = ProcessUtil.getProcess(uid);
            ProcessVo processVo = CtmJSONObject.parseObject(process, ProcessVo.class);
            if (processVo != null){
                ProcessInfo processInfo = processVo.getData();
                if (CollectionUtils.isNotEmpty(processInfo.getFailInfos())){
                    if (endDate.compareTo(nowDate) == 0) {
                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_217B378204100009", "余额重算拉取账户实时余额过程中出现异常：%s") /* "余额重算拉取账户实时余额过程中出现异常：%s" */, String.join(",", processInfo.getFailInfos())));
                    }
                    if (endDate.compareTo(nowDate) < 0) {
                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20ACE17604A00017", "余额重算拉取历史余额过程中出现异常：%s") /* "余额重算拉取历史余额过程中出现异常：%s" */, String.join(",", processInfo.getFailInfos())));
                    }
                }
            }
            //再去查询历史账户余额
            CtmJSONObject bankBalanceResult = this.getBankAccountHistoryBalance(bankAccountSettingVO);
            //借方向，需要取余额的相反数
            if (DirectionJD.Debit.getValue() == bankBalanceResult.getShort("direction")){
                result.put("bankye",bankBalanceResult.getBigDecimal("bankye").negate());
            }else {
                result.put("bankye",bankBalanceResult.getBigDecimal("bankye"));
            }
            result.put("isEmptyBalance",bankBalanceResult.getBoolean("isEmptyBalance"));

        }else {
            //2.银行账户为非直联 计算公式= 期初余额+流水收入合计（当期）-流水支出合计（当期）
            //查询当期余额，不存在则查询期初未达项的期初余额
            CtmJSONObject currentBalance = this.getBankAccountCurrentBalance(bankAccountSettingVO);
            if (currentBalance.getBoolean("isHasBalance")){
                result.put("bankye",currentBalance.getBigDecimal("bankye").setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()).toPlainString());
            }else {
                result.put("bankye",this.getBankAccountOpeningBalance(bankAccountSettingVO).getBigDecimal("bankye").setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()).toPlainString());
            }
        }

        //企业日记账余额
        if(ReconciliationDataSource.Voucher.getValue() == Short.parseShort(bankAccountSettingVO.getReconciliationdatasource())){
            CtmJSONObject voucherBalance = this.getVoucherBalanceAmount(bankAccountSettingVO);
            result.put("journalye",voucherBalance.getBigDecimal("journalye").setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()).toPlainString());
            result.put("voucherDetailInfoList",voucherBalance.get("voucherDetailInfoList"));
        }else {
            BankVoucherInfoQueryVO bankVoucherInfoQueryVO = new BankVoucherInfoQueryVO();
            bankVoucherInfoQueryVO.setAccentity(bankAccountSettingVO.getAccentity());
            bankVoucherInfoQueryVO.setBankaccount(bankAccountSettingVO.getBankaccount());
            bankVoucherInfoQueryVO.setCurrency(bankAccountSettingVO.getCurrency());
            bankVoucherInfoQueryVO.setCheckEndDate(bankAccountSettingVO.getEnableDateStr());
            bankVoucherInfoQueryVO.setReconciliationScheme(bankAccountSettingVO.getBankreconciliationscheme().toString());
            CtmJSONObject journalBalance = this.getJournalBalanceAmount(bankVoucherInfoQueryVO);
            result.put("journalye",journalBalance.getBigDecimal("journalye").setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()).toPlainString());
            result.put("voucherDetailInfoList",journalBalance.get("voucherDetailInfoList"));
        }

        return result;
    }

    private void getJournalBalancesResultBuild(BalanceAdjustQueryVO queryVo, CtmJSONObject jSONObject,Boolean initFlag) throws Exception {
        String accentity = queryVo.getAccentity();
        String bankaccount = queryVo.getBankaccount();
        Long bankreconciliationscheme = queryVo.getBankreconciliationscheme();
        String currency = queryVo.getCurrency();
        Date enableDate = queryVo.getEnableDate();
        Date dzdate = queryVo.getDzdate();
        Map<String,BigDecimal> journalResult = getJournalBalances(accentity,bankaccount,bankreconciliationscheme,currency,enableDate,dzdate,initFlag,jSONObject);
        queryVo.setDebitoriSumJour(journalResult.get("debitoriSum")!=null?journalResult.get("debitoriSum"):BigDecimal.ZERO);
        queryVo.setCreditoriSumJour(journalResult.get("creditoriSum")!=null?journalResult.get("creditoriSum"):BigDecimal.ZERO);
        queryVo.setJournalye(journalResult.get("journalye")!=null?journalResult.get("journalye"):BigDecimal.ZERO);
    }

    /**
     * 获取余额表相关信息
     * @param accentity
     * @param bankaccount
     * @param bankreconciliationscheme
     * @param currency
     * @param enableDate
     * @param dzdate
     * @return
     * @throws Exception
     */
    private Map<String,BigDecimal> getJournalBalances(String accentity,String bankaccount,Long bankreconciliationscheme,String currency,Date enableDate,Date dzdate,Boolean initFlag,CtmJSONObject jSONObject) throws Exception{
        Map<String,BigDecimal> result = new HashMap<>();
        BigDecimal debitoriSum = BigDecimal.ZERO;
        BigDecimal creditoriSum = BigDecimal.ZERO;
        BigDecimal journalye = BigDecimal.ZERO;
        //根据页面上的账户、币种查询对账方案
        PlanParam planParam = new PlanParam(bankaccount,currency,bankreconciliationscheme.toString());
        List<BankReconciliationSettingVO> brSetting_bList = cmpCheckService.findUseOrg(planParam);
        Set<String> useorgids = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : brSetting_bList){
            //CZFW-436450 去掉后台停用过滤
//            if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()){
//                useorgids.add(settingVO.getUseOrg());
//            }
            useorgids.add(settingVO.getUseOrg());
        }
        if (useorgids.size() == 0){
            useorgids.add("0");
        }
        //币种处理
        CurrencyDTO currencyDTO = CurrencyUtil.getCurrency(currency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);
        //查询相关条件下的借贷金额
        QuerySchema journalQuerySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup mainGroup = QueryConditionGroup.and(
                //账户共享，所属组织
//                QueryCondition.name("parentAccentity").eq(accentity),
                //日记账余额要过滤授权使用组织
                QueryCondition.name(ACCENTITY).in(useorgids),
                QueryCondition.name(BANK_ACCOUNT).eq(bankaccount),
                QueryCondition.name(CURRENCY).eq(currency));

        Map<String, SettlementDetail> settlementDetailMap =  null;
        if(!initFlag){
            QueryConditionGroup initGroup = QueryConditionGroup.and(QueryCondition.name(INIT_FLAG).eq(1),
                    QueryCondition.name(BANKRECONCILIATIONSCHEME).eq(bankreconciliationscheme),
                    QueryCondition.name("checkflag").eq(false));  //期初
            QueryConditionGroup journalGroup = QueryConditionGroup.and(QueryCondition.name(DZ_DATE).egt(enableDate),
                    QueryCondition.name(DZ_DATE).elt(dzdate),
                    QueryCondition.name(SETTLE_STATUS).eq("2"),
                    QueryCondition.name(AUDIT_STATUS).eq(AuditStatus.Complete.getValue()),
                    QueryCondition.name("checkflag").eq(false),
                    QueryCondition.name("billtype").not_eq(EventType.ExchangeBill.getValue()));
            QueryConditionGroup childrenGroup = QueryConditionGroup.or(journalGroup,initGroup);
            QueryConditionGroup group = QueryConditionGroup.and(mainGroup,childrenGroup);
            journalQuerySchema.addCondition(group);
            //返回相关使用组织的对应余额(不传使用组织时 会返回按使用组织分组的所有符合 账户、币种的数据)
            settlementDetailMap = DailyComputezInit.imitateDailyComputeInit(null, currency, null, bankaccount, "2", "2", dzdate);
        }else{
            mainGroup.addCondition(QueryCondition.name(BANKRECONCILIATIONSCHEME).eq(bankreconciliationscheme));
            mainGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(INIT_FLAG).eq(1)));
            journalQuerySchema.addCondition(mainGroup);
            //期初未达-余额调节表 取数为期初未达子表(cmp_openingOutstanding_b)页面上的期初余额设置中的值
            //通过 组织、对账方案id 账户确定唯一的期初未达数据 取其子表
            OpeningOutstanding openingOutstanding = querOpeningOutstanding(bankreconciliationscheme,accentity,bankaccount,currency);
            //存在子表 数据取子表数据 若 不存在子表不处理 后续逻辑会默认为0
            if(openingOutstanding!=null && CollectionUtils.isNotEmpty(openingOutstanding.openingOutstanding_b())){
                Map<String,OpeningOutstanding_b> useOrgbMap = new HashMap<>();
                for(OpeningOutstanding_b b : openingOutstanding.openingOutstanding_b()){
                    useOrgbMap.put(b.getUseOrg(),b);
                }
                //构建settlementDetailMap
                settlementDetailMap = new HashMap<>();
                //根据使用组织 取子表数据 如果当前使用组织在子表无值 则赋0
                for(String userOrg : useorgids){
                    OpeningOutstanding_b openingOutstandingB = useOrgbMap.get(userOrg);
                    if(openingOutstandingB != null){
                        SettlementDetail settlementDetail = new SettlementDetail();
                        // 企业方余额方向支出取负
                        if (Short.parseShort(openingOutstandingB.get("direction").toString()) == Direction.Credit.getValue()) {
                            settlementDetail.setTodayorimoney(openingOutstandingB.getCoinitloribalance().negate());
                        } else {
                            settlementDetail.setTodayorimoney(openingOutstandingB.getCoinitloribalance());
                        }
                        settlementDetailMap.put((userOrg+bankaccount+currency).replace("null",""),settlementDetail);
                    }
                }
            }
        }
        List<Map<String, Object>> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME,journalQuerySchema);
        for(Map<String, Object> map:journalList){
            if(map.get(DEBITORI_SUM) != null){
                debitoriSum = BigDecimalUtils.safeAdd(debitoriSum,(BigDecimal)map.get(DEBITORI_SUM));
            }
            if(map.get(CREDITORI_SUM) != null){
                creditoriSum = BigDecimalUtils.safeAdd(creditoriSum,(BigDecimal)map.get(CREDITORI_SUM));
            }
        }
        //批量查询 会计主体信息 用于后续提示
        List<Map<String, Object>> accEntityes = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(useorgids.toArray(new String[0])));
        Map<String,String> accEntityName = new HashMap<>();
        for(Map<String, Object> map : accEntityes){
            accEntityName.put(map.get("id").toString(), map.get("name")!=null?map.get("name").toString():null);
        }
        //日记账前端余额统计接口
        List<CtmJSONObject> voucherDetailInfoList = new ArrayList<>();
        for(String orgId : useorgids){
            String banlanceKey = orgId+ bankaccount + currency;
            SettlementDetail settlementDetail = settlementDetailMap!=null?settlementDetailMap.get(banlanceKey.replace("null","")):null;
            //计算总金额
            journalye = journalye.add(settlementDetail!=null && settlementDetail.get("todayorimoney")!=null ? settlementDetail.getTodayorimoney() : BigDecimal.ZERO);
            CtmJSONObject voucherDetailInfo = new CtmJSONObject();
            voucherDetailInfo.put("useOrgName",accEntityName.get(orgId));
            //当前使用组织账户余额
            BigDecimal returnSum = settlementDetail!=null && settlementDetail.get("todayorimoney")!=null ? settlementDetail.getTodayorimoney() : BigDecimal.ZERO;
            voucherDetailInfo.put("sum",returnSum.setScale(currencyDTO.getMoneydigit(),moneyRound));
            voucherDetailInfoList.add(voucherDetailInfo);
            jSONObject.put("voucherDetailInfoList",voucherDetailInfoList);

        }
        result.put("creditoriSum",creditoriSum); //贷方金额
        result.put("debitoriSum",debitoriSum); //借方金额
        result.put("journalye",journalye.setScale(currencyDTO.getMoneydigit(),moneyRound)); //企业日记账余额
        return result;
    }

    /**
     * 获取余额表银行对账单相关信息
     * @param initFlag
     * @param accentity
     * @param bankaccount
     * @param bankreconciliationscheme
     * @param currency
     * @param enableDate
     * @param dzdate
     * @return
     */
    private Map<String,BigDecimal> getBankReconBalances(boolean initFlag,String accentity,String bankaccount,Long bankreconciliationscheme,String currency,Date enableDate,Date dzdate,Integer  reconciliationdatasource) throws Exception{
        Map<String,BigDecimal> result = new HashMap<>();
        BigDecimal debitoriSum = BigDecimal.ZERO;
        BigDecimal creditoriSum = BigDecimal.ZERO;
        BigDecimal bankye = BigDecimal.ZERO;
        //币种处理
//        CurrencyDTO currencyDTO = CurrencyUtil.getCurrency(currency);
//        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(currency, 1);
        QuerySchema bankQuerySchema = QuerySchema.create().addSelect("*");
        //账户共享，银行对账单按照账号和币种来获取
        QueryConditionGroup mainGroup = QueryConditionGroup.and(
//                QueryCondition.name("orgid").eq(accentity),
                QueryCondition.name(BANK_ACCOUNT).eq(bankaccount),
                QueryCondition.name(CURRENCY).eq(currency));
        if(initFlag){
            mainGroup.addCondition(QueryCondition.name(INIT_FLAG).eq(1));
            mainGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(BANKRECONCILIATIONSCHEME).eq(bankreconciliationscheme)));
            bankQuerySchema.addCondition(mainGroup);
            List<Map<String, Object>> bankreconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME,bankQuerySchema);
            //银行对账单余额
            for(Map<String, Object> map:bankreconciliationList){
                if (!BankreconciliationUtils.isNoProcess(accentity) && map.get("serialdealtype") != null && Short.valueOf(map.get("serialdealtype").toString()) == (short) 5) {
                    //组织参数 无需处理的流水，是否参与银企对账、银行账户余额弥 为否，并且对应数据的是否无需处理为是，不参数计算
                    continue;
                }
                if(map.get("debitamount") != null){
                    debitoriSum = BigDecimalUtils.safeAdd(debitoriSum,(BigDecimal)map.get("debitamount"));//借方金额合计
                }
                if(map.get("creditamount") != null){
                    creditoriSum = BigDecimalUtils.safeAdd(creditoriSum,(BigDecimal)map.get("creditamount"));//贷方金额合计
                }
            }
            Map<String,Object> bankResult = getBankinitoribalance(bankreconciliationscheme,accentity,bankaccount,currency);
            if(bankResult != null && bankResult.get("bankinitoribalance") != null && bankResult.get("bankinitoribalance") instanceof BigDecimal){
                bankye = (BigDecimal) bankResult.get("bankinitoribalance");
            }else {
                bankye =  BigDecimal.ZERO;
            }
        }else{
            QueryConditionGroup initGroup = QueryConditionGroup.and(QueryCondition.name(INIT_FLAG).eq(1),
                    QueryCondition.name(BANKRECONCILIATIONSCHEME).eq(bankreconciliationscheme));  //期初
            QueryConditionGroup bankGroup =  QueryConditionGroup.and(QueryConditionGroup.and(QueryCondition.name(DZ_DATE).egt(enableDate)),
                    QueryConditionGroup.and(QueryCondition.name(DZ_DATE).elt(dzdate)),QueryCondition.name(INIT_FLAG).eq(0));

            bankQuerySchema.addOrderBy(new QueryOrderby("dzdate", "desc"),new QueryOrderby("bank_seq_no", "desc"),new QueryOrderby("tran_time", "desc"));
            QueryConditionGroup chidrenGroup = QueryConditionGroup.or(bankGroup,initGroup);
            QueryConditionGroup group = QueryConditionGroup.and(mainGroup,chidrenGroup);
            bankQuerySchema.addCondition(group);

            //非期初银行账户余额，走余额重算逻辑，该代码废弃
//            List<Map<String, Object>> bankreconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME,bankQuerySchema);
//            //银行对账单余额
//            BigDecimal acct_bal = null ;//银行对账单中实际余额
//            for(Map<String, Object> map:bankreconciliationList){
//                if (!BankreconciliationUtils.isNoProcess(accentity) && map.get("serialdealtype") != null && Short.valueOf(map.get("serialdealtype").toString()) == (short) 5) {
//                    //组织参数 无需处理的流水，是否参与银企对账、银行账户余额弥 为否，并且对应数据的是否无需处理为是，不参数计算
//                    continue;
//                }
//                if(acct_bal==null&&map.get("acct_bal") != null){
//                    acct_bal = (BigDecimal)map.get("acct_bal");//银行对账单中实际余额 取最新单据的余额
//                }
//            }

            //借贷金额=======================
            bankQuerySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup checkFlagGroup = new QueryConditionGroup();
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                checkFlagGroup.addCondition(QueryCondition.name("other_checkflag").eq(0));
            }else{
                checkFlagGroup.addCondition(QueryCondition.name("checkflag").eq(0));
            }
            chidrenGroup = QueryConditionGroup.or(bankGroup,initGroup);
            chidrenGroup = QueryConditionGroup.and(chidrenGroup,checkFlagGroup);

            group = QueryConditionGroup.and(mainGroup,chidrenGroup);
            bankQuerySchema.addCondition(group);

            List<Map<String, Object>> bankreconciliationList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME,bankQuerySchema);
            for(Map<String, Object> map:bankreconciliationList){
                if (!BankreconciliationUtils.isNoProcess(accentity) && map.get("serialdealtype") != null && Short.valueOf(map.get("serialdealtype").toString()) == (short) 5) {
                    //组织参数 无需处理的流水，是否参与银企对账、银行账户余额弥 为否，并且对应数据的是否无需处理为是，不参数计算
                    continue;
                }
                if(map.get("debitamount") != null){
                    debitoriSum = BigDecimalUtils.safeAdd(debitoriSum,(BigDecimal)map.get("debitamount"));//借方金额合计
                }
                if(map.get("creditamount") != null){
                    creditoriSum = BigDecimalUtils.safeAdd(creditoriSum,(BigDecimal)map.get("creditamount"));//贷方金额合计
                }
            }

            //非期初银行账户余额，走余额重算逻辑，该代码废弃
//            BigDecimal bankinitoribalance = BigDecimal.ZERO;
//            List<Map<String, Object>> initdataList = MetaDaoHelper.query(InitData.ENTITY_NAME,
//                    QuerySchema.create().addSelect("*").appendQueryCondition(
//                            QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),QueryCondition.name(BANK_ACCOUNT).eq(bankaccount)));
//            for(Map<String, Object> initData:initdataList){
//                //期初余额改造，去除方向。小于0为贷；大于等于0为借
//                Integer direction = (Integer)initData.get("bankdirection");
//                if(initData.get("cobookoribalance") != null && initData.get("bankinitoribalance")!=null){
//                    bankinitoribalance = (BigDecimal)initData.get("bankinitoribalance");//银行方期初原币余额
//                }
//            }
//            if(acct_bal == null) {
//                bankye =  bankinitoribalance.setScale(currencyDTO.getMoneydigit(),moneyRound);
//            }else{
//                //取银行对账单最后一条数据的金额
//                bankye = acct_bal.setScale(currencyDTO.getMoneydigit(),moneyRound);
//            }
        }
        result.put("creditoriSum",creditoriSum); //贷方金额
        result.put("debitoriSum",debitoriSum); //借方金额
        result.put("bankye",bankye); //对账单余额
        return result;

    }

    /**
     * 查询期初未达项
     * @param bankreconciliationscheme
     * @param accentity
     * @param bankaccount
     * @param currency
     * @return
     * @throws Exception
     */
    private Map<String,Object> getBankinitoribalance(Long bankreconciliationscheme, String accentity, String bankaccount, String currency) throws Exception{
        QuerySchema bankQuerySchema = QuerySchema.create().addSelect("bankdirection,bankinitoribalance");
        QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),QueryCondition.name("bankaccount").eq(bankaccount),QueryCondition.name("currency").eq(currency),QueryCondition.name("bankreconciliationscheme").eq(bankreconciliationscheme));
        bankQuerySchema.addCondition(bankGroup);
        List<Map<String, Object>> bankreconciliationList = MetaDaoHelper.query(OpeningOutstanding.ENTITY_NAME,bankQuerySchema);
        if(bankreconciliationList!=null&&bankreconciliationList.size()!=0){
            return bankreconciliationList.get(0);
        }
        return null;
    }

    /**
     * 查询期初未达项 - 带子表
     * @param bankreconciliationscheme
     * @param accentity
     * @param bankaccount
     * @param currency
     * @return
     * @throws Exception
     */
    private OpeningOutstanding querOpeningOutstanding(Long bankreconciliationscheme, String accentity, String bankaccount, String currency) throws Exception{
        QuerySchema bankQuerySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),QueryCondition.name("bankaccount").eq(bankaccount),QueryCondition.name("currency").eq(currency),QueryCondition.name("bankreconciliationscheme").eq(bankreconciliationscheme));
        bankQuerySchema.addCondition(bankGroup);
//        QuerySchema detailSchema = QuerySchema.create().name("OpeningOutstanding_b").addSelect("*");
//        bankQuerySchema.addCompositionSchema(detailSchema);
        List<BizObject> resultVoList = MetaDaoHelper.queryObject(OpeningOutstanding.ENTITY_NAME, bankQuerySchema,null);
        if(CollectionUtils.isEmpty(resultVoList)){
            return null;
        }
        QuerySchema bodyQuerySchema = QuerySchema.create().addSelect("*");
        bodyQuerySchema.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.MAINID).eq(resultVoList.get(0).get(ICmpConstant.ID))));
        List<BizObject> bodyObjVoList = MetaDaoHelper.queryObject(OpeningOutstanding_b.ENTITY_NAME, bodyQuerySchema,null);
        if(CollectionUtils.isEmpty(bodyObjVoList)){
            return null;
        }
        List<OpeningOutstanding_b> bodyVoList = new ArrayList<>();
        for(BizObject obj : bodyObjVoList){
            OpeningOutstanding_b bodyVo = new OpeningOutstanding_b();
            bodyVo.init(obj);
            bodyVoList.add(bodyVo);
        }
        OpeningOutstanding resultVo = new OpeningOutstanding();
        resultVo.init(resultVoList.get(0));
        resultVo.setOpeningOutstanding_b(bodyVoList);
        return resultVo;
    }

    /**
     * 查询企业银行账户类型，是否为结算中心开户
     * @param account
     * @return
     */
    private boolean queryAccountType(String account) {
        try {
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(account);
            if (AcctopenTypeEnum.SettlementCenter.getValue() == enterpriseBankAcctVO.getAcctopentype()) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return false;
    }
}
