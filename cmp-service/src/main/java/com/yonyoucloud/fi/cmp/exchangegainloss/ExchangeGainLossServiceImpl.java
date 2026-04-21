package com.yonyoucloud.fi.cmp.exchangegainloss;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.rpc.rule.DailyComputezInit;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Created  by xudy on 2019/9/26.
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class ExchangeGainLossServiceImpl implements ExchangeGainLossService {
    public static final String CUSTOMER_EXCHANGE_RATE_TYPE = "02";
    public String DATE = "date";
    public String ENABLE = "enable";
    public String DR = "dr";
    public String CURRENCYLISTENABLE = "currencyList.enable";
    public String ORGID = "orgid";
    public String CURRENCY = "currency";
    public String CURRENCY_NAME = "currency_name";
    public String CURRENCYLISTCURRENCY = "currencyList.currency";
    public String CURRENCY_MONEYDIGIT = "currency_moneyDigit";
    public String TENANT = "tenant";
    public String BANKID = "bankid";
    public String BANKNAME = "bankname";
    public String BANKACCOUNT_NAME = "bankaccount_name";
    public String CASHID = "cashid";
    public String CASHNAME = "cashname";
    public String CASHACCOUNT_NAME = "cashaccount_name";
    public String BANKSQL = "id as bankid,code,name as bankname,account,currencyList.currency as currency,currencyList.currency.name as currency_name,currencyList.currency.moneyDigit";
    public String CASHSQL = "id as cashid,code,name as cashname,currency,currency.name,currency.moneyDigit";
    public String BANKFULLNAME = "bd.enterprise.OrgFinBankacctVO";
    public String CASHFULLNAME = "bd.enterprise.OrgFinCashacctVO";
    public String BANKGROUP = "ucfbasedoc";
    public String DATA = "data";
    public String EXCHANGERATETYPE = "exchangeRateType";
    public static String DAILYCOMPUTEZINITMAPPER = "com.yonyoucloud.fi.cmp.mapper.DailyComputezInitMapper.";

    public static String serverUrl = AppContext.getEnvConfig("fifrontservername");

    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;


    /**
     * @param params
     * 汇率损益界面初始化
     */
    @Override
    public CtmJSONObject  initData(CtmJSONObject params) throws  Exception{
        CtmJSONObject json  = new CtmJSONObject();
        boolean  flag = false; //如果取不到汇率，给true ,否则为false
        List<ExchangeGainLoss_b> exchangeGainLoss_bList = new ArrayList<ExchangeGainLoss_b>();
        String accentity = params.getString(IBussinessConstant.ACCENTITY);
        String natcurrency = AccentityUtil.getNatCurrencyIdByAccentityId(accentity);
        params.put("natcurrency",natcurrency);
        //精度处理
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(natcurrency);
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(natcurrency, 1);
        Date date =   params.getDate(DATE);
        String exchangeRateType = params.getString(EXCHANGERATETYPE);
        String exchangeRateTypeCode = queryExchangeRateCode(exchangeRateType);
        Boolean balancezero=params.getBoolean("balancezero");
        List<Map<String, Object>> bankList = new ArrayList<Map<String, Object>>();
       //1.查询会计主体下，当前时间的所有银行账户/现金账户下非本位币的币种信息和损益金额
        //银行账户
//        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name(ENABLE).eq(1),
//                QueryCondition.name(ORGID).eq(accentity),QueryCondition.name(DR).eq(0),
//                QueryCondition.name(CURRENCYLISTCURRENCY).not_eq(natcurrency),
//                QueryCondition.name(CURRENCYLISTENABLE).eq(1));
//        QuerySchema querySchema=QuerySchema.create().addSelect(BANKSQL).addCondition(queryConditionGroup);
//        List<Map<String, Object>> dataList=MetaDaoHelper.query(BANKFULLNAME,querySchema, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
//        bankList.addAll(dataList);
        //模拟日结
//        DailyCompute.imitateDailyCompute(accentity, null, null, null, "2", "2", date);
        Map<String, SettlementDetail> settlementDetailMap =
                DailyComputezInit.imitateDailyComputeInit(accentity, null, null, null, "2", "2", date);

        //数据权限
        String[] fields =new String[] { "bankaccount", "cashaccount"};
        Map<String, List<Object>> dataPermission = AuthUtil.dataPermission("CM", ExchangeGainLoss_b.ENTITY_NAME, null,fields);
        //银行账户 使用组织视角查询银行账户
        buildExchangeGainLoss_bBankAcct(dataPermission,params,settlementDetailMap, exchangeGainLoss_bList,currencyDTO,moneyRound,flag);
        //现金账户
        SettlementDetail settlementDetail=new SettlementDetail();
        QueryConditionGroup queryConditionGroupCash = QueryConditionGroup.and(QueryCondition.name(ENABLE).eq(1),
                QueryCondition.name(ORGID).eq(accentity),QueryCondition.name(CURRENCY).not_eq(natcurrency));
        QuerySchema querySchema=QuerySchema.create().addSelect(CASHSQL).addCondition(queryConditionGroupCash);
        List<Map<String, Object>> dataList=MetaDaoHelper.query(CASHFULLNAME,querySchema,ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
        bankList.addAll(dataList);
        for(Map<String, Object> map :bankList){
            ExchangeGainLoss_b exchangeGainLoss_b = new ExchangeGainLoss_b();
            exchangeGainLoss_b.setCurrency((String) map.get(CURRENCY));
            exchangeGainLoss_b.put(CURRENCY_NAME,map.get(CURRENCY_NAME));
            exchangeGainLoss_b.put(CURRENCY_MONEYDIGIT,map.get(CURRENCY_MONEYDIGIT));
            exchangeGainLoss_b.setBankaccount((String)map.get(BANKID));
            exchangeGainLoss_b.put(BANKACCOUNT_NAME,map.get(BANKNAME));
            exchangeGainLoss_b.setCashaccount((String)map.get(CASHID));
            exchangeGainLoss_b.put(CASHACCOUNT_NAME,map.get(CASHNAME));
            BigDecimal todaylocalmoney = BigDecimal.ZERO;
            BigDecimal todayorimoney= BigDecimal.ZERO;
            if(ValueUtils.isNotEmpty((String)map.get(BANKID))){
                if(dataPermission != null && dataPermission.size() > 0){
                    if(dataPermission.get("bankaccount") != null && dataPermission.get("bankaccount").size() > 0){
                        if(!dataPermission.get("bankaccount").contains(map.get(BANKID))){
                            continue;
                        }
                    }
                }
                 settlementDetail=  settlementDetailMap.get(accentity+map.get(BANKID) + map.get(CURRENCY));
            }else if(ValueUtils.isNotEmpty((String)map.get(CASHID))){
                if(dataPermission != null && dataPermission.size() > 0){
                    if(dataPermission.get("cashaccount") != null && dataPermission.get("cashaccount").size() > 0){
                        if(!dataPermission.get("cashaccount").contains(map.get(CASHID))){
                            continue;
                        }
                    }
                }
                 settlementDetail=  settlementDetailMap.get(accentity+map.get(CASHID) + map.get(CURRENCY));
            }
            //如果不为空 获取本币与原币
            if(ValueUtils.isNotEmpty(settlementDetail)){
                todaylocalmoney= settlementDetail.getTodaylocalmoney();
                todayorimoney=  settlementDetail.getTodayorimoney();
            }
            if(balancezero !=null && balancezero==false && BigDecimal.ZERO.compareTo(todayorimoney)==0) {
                continue;
            }else {
                exchangeGainLoss_b.setLocalbalance(todaylocalmoney.setScale(currencyDTO.getMoneydigit(),moneyRound));
                exchangeGainLoss_b.setOribalance(todayorimoney);
            }
            BigDecimal exchange =  null ;
            Short exchangeRateOps = null;
            //获取当前时间的调整汇率
            try{
                if(StringUtils.isNotBlank(exchangeRateType)){
                    CmpExchangeRateVO exchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode((String) map.get(CURRENCY), natcurrency, date, exchangeRateType);
                    exchange = exchangeRateWithMode.getExchangeRate();
                    if (Objects.equals(exchangeRateTypeCode, CUSTOMER_EXCHANGE_RATE_TYPE)) {
                        exchangeRateOps = ICmpConstant.EXCHANGE_RATE_OPS_MULTIPLY;
                    } else {
                        exchangeRateOps = exchangeRateWithMode.getExchangeRateOps();
                    }
                }else{
                    flag = true;
                }
            }catch (Exception e){
                flag = true;
            }
            exchangeGainLoss_b.setExchangerate(exchange);
            exchangeGainLoss_b.setExchangerateOps(exchangeRateOps);
            BigDecimal adjustlocalbalance = BigDecimal.ZERO;
            if(exchange != null){
                adjustlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangeRateOps, exchange, exchangeGainLoss_b.getOribalance(), null);
            }
            adjustlocalbalance = adjustlocalbalance.setScale(currencyDTO.getMoneydigit(),moneyRound);
            exchangeGainLoss_b.setAdjustlocalbalance(adjustlocalbalance);
            BigDecimal adjustbalance = BigDecimalUtils.safeSubtract(adjustlocalbalance,exchangeGainLoss_b.getLocalbalance());
            exchangeGainLoss_b.setAdjustbalance(adjustbalance.setScale(currencyDTO.getMoneydigit(),moneyRound));
            exchangeGainLoss_b.setEntityStatus(EntityStatus.Insert);
            exchangeGainLoss_bList.add(exchangeGainLoss_b);

        }
        json.put(DATA,exchangeGainLoss_bList);
        json.put("flag",flag);
        json.put("moneydigit",currencyDTO.getMoneydigit());
        json.put("moneyrount",currencyDTO.getMoneyrount());
        return   json;
    }

    private String queryExchangeRateCode(String exchangeRateType) throws Exception {
        String exchangeRateTypeCode = null;
        if (StringUtils.isNotEmpty(exchangeRateType)) {
            List<Map<String, Object>> exchangeRateTypeById = QueryBaseDocUtils.queryExchangeRateTypeById(exchangeRateType);
            exchangeRateTypeCode = exchangeRateTypeById.get(0).get(ICmpConstant.CODE).toString();
        }
        return exchangeRateTypeCode;
    }


    //使用组织查询银行账户 并组装汇兑损益子表
    private void buildExchangeGainLoss_bBankAcct(Map<String, List<Object>> dataPermission,CtmJSONObject params,Map<String, SettlementDetail> settlementDetailMap,
                                                 List<ExchangeGainLoss_b> exchangeGainLoss_bList ,CurrencyTenantDTO currencyDTO,RoundingMode moneyRound,boolean flag) throws Exception {
        String accentity = params.getString(IBussinessConstant.ACCENTITY);
        String natcurrency = params.getString("natcurrency");
        String exchangeRateType = params.getString(EXCHANGERATETYPE);
        String exchangeRateTypeCode = queryExchangeRateCode(exchangeRateType);
        Boolean balancezero=params.getBoolean("balancezero");
        Date date =   params.getDate(DATE);

        SettlementDetail settlementDetail=new SettlementDetail();
        EnterpriseParams enterpriseparams = new EnterpriseParams();
        enterpriseparams.setOrgid(accentity);
        enterpriseparams.setPageSize(4500);
        List<EnterpriseBankAcctVOWithRange> useBankAccts = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseparams);
        for(EnterpriseBankAcctVOWithRange usebankAcct : useBankAccts){
            for(BankAcctCurrencyVO currencyVO : usebankAcct.getCurrencyList()){
                //获取和当前natcurrency币种不同的账户
                if(currencyVO.getCurrency().equals(natcurrency)){
                    continue;
                }
                ExchangeGainLoss_b exchangeGainLoss_b = new ExchangeGainLoss_b();
                exchangeGainLoss_b.setCurrency(currencyVO.getCurrency());
                exchangeGainLoss_b.put(CURRENCY_NAME,currencyVO.getCurrencyName());
                //查询币种精度
                CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currencyVO.getCurrency());
                //存在因为币种停用或平台数据错误导致的报错 这里如果查不到则返回
                if(currencyTenantDTO==null){
                    continue;
                }
                Integer moneydigit = currencyTenantDTO.getMoneydigit();
                exchangeGainLoss_b.put(CURRENCY_MONEYDIGIT,moneydigit);
                exchangeGainLoss_b.setBankaccount(usebankAcct.getId());
                exchangeGainLoss_b.put(BANKACCOUNT_NAME,usebankAcct.getName());
                BigDecimal todaylocalmoney = BigDecimal.ZERO;
                BigDecimal todayorimoney= BigDecimal.ZERO;
                if(ValueUtils.isNotEmpty(usebankAcct.getId())){
                    if(dataPermission != null && dataPermission.size() > 0){
                        if(dataPermission.get("bankaccount") != null && dataPermission.get("bankaccount").size() > 0){
                            if(!dataPermission.get("bankaccount").contains(usebankAcct.getId())){
                                continue;
                            }
                        }
                    }
                    settlementDetail=  settlementDetailMap.get(accentity+usebankAcct.getId() + currencyVO.getCurrency());
                }
                //如果不为空 获取本币与原币
                if(ValueUtils.isNotEmpty(settlementDetail)){
                    todaylocalmoney= settlementDetail.getTodaylocalmoney();
                    todayorimoney=  settlementDetail.getTodayorimoney();
                }
                if(balancezero !=null && balancezero==false && BigDecimal.ZERO.compareTo(todayorimoney)==0) {
                    continue;
                }else {
                    exchangeGainLoss_b.setLocalbalance(todaylocalmoney.setScale(currencyDTO.getMoneydigit(),moneyRound));
                    exchangeGainLoss_b.setOribalance(todayorimoney);
                }
                BigDecimal exchange =  null ;
                Short exchangeRateOps = null;
                //获取当前时间的调整汇率
                try{
                    if(StringUtils.isNotBlank(exchangeRateType)){
                        CmpExchangeRateVO exchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode(currencyVO.getCurrency(), natcurrency, date, exchangeRateType);
                        exchange = exchangeRateWithMode.getExchangeRate();
                        if (Objects.equals(exchangeRateTypeCode, CUSTOMER_EXCHANGE_RATE_TYPE)) {
                            exchangeRateOps = ICmpConstant.EXCHANGE_RATE_OPS_MULTIPLY;
                        } else {
                            exchangeRateOps = exchangeRateWithMode.getExchangeRateOps();
                        }
                    }else{
                        flag = true;
                    }
                }catch (Exception e){
                    flag = true;
                }
                exchangeGainLoss_b.setExchangerate(exchange);
                exchangeGainLoss_b.setExchangerateOps(exchangeRateOps);
                BigDecimal adjustlocalbalance = BigDecimal.ZERO;
                if(exchange != null){
                    adjustlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchangeRateOps, exchange, exchangeGainLoss_b.getOribalance(), null);
                }
                adjustlocalbalance = adjustlocalbalance.setScale(currencyDTO.getMoneydigit(),moneyRound);
                exchangeGainLoss_b.setAdjustlocalbalance(adjustlocalbalance);
                BigDecimal adjustbalance = BigDecimalUtils.safeSubtract(adjustlocalbalance,exchangeGainLoss_b.getLocalbalance());
                exchangeGainLoss_b.setAdjustbalance(adjustbalance.setScale(currencyDTO.getMoneydigit(),moneyRound));
                exchangeGainLoss_b.setEntityStatus(EntityStatus.Insert);
                exchangeGainLoss_bList.add(exchangeGainLoss_b);
            }
        }
    }

    /**
     * 创建日记账
     *
     * @param exchangeGainLoss   现金汇兑损益主表
     * @param exchangeGainLoss_b 现金汇兑损益子表
     * @return
     * @throws Exception
     */
    @Override
    public Journal createJournalForAdd(ExchangeGainLoss exchangeGainLoss, ExchangeGainLoss_b exchangeGainLoss_b, String billnum) throws Exception {
        Journal journal = createJournal(exchangeGainLoss, exchangeGainLoss_b, billnum);
        if (exchangeGainLoss_b.getAdjustbalance().compareTo(BigDecimal.ZERO) == 1) {
            journal.setDebitnatSum(exchangeGainLoss_b.getAdjustbalance().abs());
            journal.setCreditnatSum(BigDecimal.ZERO);
            journal.setDirection(Direction.Debit);
        } else {
            journal.setCreditnatSum(exchangeGainLoss_b.getAdjustbalance().abs());
            journal.setDebitnatSum(BigDecimal.ZERO);
            journal.setDirection(Direction.Credit);
        }
        return journal;
    }

    /**
     * 创建日记账-现金汇兑损益冲销
     *
     * @param exchangeGainLoss   现金汇兑损益主表
     * @param exchangeGainLoss_b 现金汇兑损益主表
     * @param billnum
     * @return
     * @throws Exception
     */
    @Override
    public Journal createJournalForWriteOff(ExchangeGainLoss exchangeGainLoss, ExchangeGainLoss_b exchangeGainLoss_b, String billnum) throws Exception {
        Journal journal = createJournal(exchangeGainLoss, exchangeGainLoss_b, billnum);
        if (exchangeGainLoss_b.getAdjustbalance().compareTo(BigDecimal.ZERO) == 1) {
            journal.setCreditnatSum(exchangeGainLoss_b.getAdjustbalance().abs());
            journal.setDebitnatSum(BigDecimal.ZERO);
            journal.setDirection(Direction.Credit);
        } else {
            journal.setDebitnatSum(exchangeGainLoss_b.getAdjustbalance().abs());
            journal.setCreditnatSum(BigDecimal.ZERO);
            journal.setDirection(Direction.Debit);
        }
        return journal;
    }

    /**
     * 创建日记账实体
     * @param exchangeGainLoss   现金汇兑损益主表
     * @param exchangeGainLoss_b 现金汇兑损益子表
     * @param billnum  单据编号
     * @return
     */
    private Journal createJournal(ExchangeGainLoss exchangeGainLoss, ExchangeGainLoss_b exchangeGainLoss_b, String billnum) throws Exception {
        Journal journal = new Journal() ;
        journal.setAccentity(exchangeGainLoss.getAccentity());
        journal.setNatCurrency(exchangeGainLoss.getNatCurrency());
        journal.setBankaccount(exchangeGainLoss_b.getBankaccount());
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(exchangeGainLoss_b.getBankaccount())) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(exchangeGainLoss_b.getBankaccount());
            journal.setBanktype(enterpriseBankAcctVO.getBank());
            // 本方银行账号
            journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
        }
        journal.setCashaccount(exchangeGainLoss_b.getCashaccount());
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(exchangeGainLoss_b.getCashaccount())) {
            EnterpriseCashVO enterpriseCashVO   = baseRefRpcService.queryEnterpriseCashAcctById(exchangeGainLoss_b.getCashaccount());
            // 本方现金账号
            journal.setCashaccountno(enterpriseCashVO.getCode());
        }

        journal.setCurrency(exchangeGainLoss_b.getCurrency());
        journal.setDzdate(exchangeGainLoss.getVouchdate());
        journal.setVouchdate(exchangeGainLoss.getVouchdate());
        journal.setTradetype(exchangeGainLoss.getTradetype());
        journal.setDescription(exchangeGainLoss.getDescription());
        journal.setBilltype(EventType.ExchangeBill);
        journal.setSrcitem(EventSource.Cmpchase);
        // 汇兑损益单
        journal.setTopbilltype(EventType.ExchangeBill);
        journal.setTopsrcitem(EventSource.Cmpchase);
        journal.setExchangerate(exchangeGainLoss_b.getExchangerate());
        journal.setDebitoriSum(BigDecimal.ZERO);
        journal.setCreditoriSum(BigDecimal.ZERO);
        journal.setOribalance(exchangeGainLoss_b.getOribalance());
        journal.setNatbalance(exchangeGainLoss_b.getAdjustlocalbalance());
        journal.setCaobject(CaObject.Other);
        journal.setSrcbillitemid(String.valueOf((Long)exchangeGainLoss.getId()));
        journal.setSrcbillno(exchangeGainLoss.getCode());
        journal.setOrg(exchangeGainLoss.getAccentity());
        journal.setBillnum(exchangeGainLoss.getCode());
        journal.setAuditstatus(AuditStatus.Complete);
        journal.setSettlestatus(SettleStatus.alreadySettled);
        journal.setCreateDate(new Date());
        journal.setCreateTime(new Date());
        journal.setCreator(AppContext.getCurrentUser().getName());
        journal.setCreatorId(AppContext.getCurrentUser().getId());
        journal.setTenant(AppContext.getTenantId());
        journal.setBillno(billnum);
        journal.setServicecode("ficmp0025");
        journal.setTargeturl(serverUrl+"/meta/ArchiveList/" + billnum);
        journal.setEntityStatus(EntityStatus.Insert);
        journal.setId(ymsOidGenerator.nextId());
        return journal;
    }
}
