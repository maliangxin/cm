package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2019/4/16 0016.
 */
public class BankreconciliationBeforeSaveRuleTemp extends AbstractCommonRule {
    private ConcurrentHashMap<String, CurrencyTenantDTO> currencyDTOMap = new ConcurrentHashMap<String,CurrencyTenantDTO>();
    private ConcurrentHashMap<String,RoundingMode> moneyRoundMap = new ConcurrentHashMap<String,RoundingMode>();
    private CurrencyTenantDTO currencyDTO = null ;
    private RoundingMode moneyRound =  null ;
    private Map<String,Date>  accentityBeginDateMap = new HashMap<String,Date>();
    private Map<String,Date>  accentityBeginDateGlMap = new HashMap<String,Date>();
    private Map<String,Boolean> settleStateMap = new HashMap<String,Boolean>();
    Date periodFirstDate = null;
    boolean settleState = false ;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        BizObject singleOrg = null ;
        boolean  isSingleOrg = FIDubboUtils.isSingleOrg();
        if(isSingleOrg){
            singleOrg = FIDubboUtils.getSingleOrg();
        }
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());
        if (bills != null && bills.size() > 0) {
            for(int i = 0;i<bills.size();i++){
                BankReconciliation bizObject = (BankReconciliation) bills.get(i);
                // OpenApi
                boolean openApiFlag = (bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true)) || billDataDto.getFromApi();
                if(isSingleOrg&&ValueUtils.isNotEmpty(singleOrg)){
                    bizObject.set(IBussinessConstant.ACCENTITY,singleOrg.get("id"));
                    bizObject.set("accentity_name",singleOrg.get("name"));
                }
                //精度查询
                if(currencyDTOMap.contains(bizObject.getCurrency())){
                    currencyDTO = currencyDTOMap.get(bizObject.getCurrency());
                    moneyRound = moneyRoundMap.get(bizObject.getCurrency());
                }else{
                    currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.getCurrency());
                    currencyDTOMap.put(bizObject.getCurrency(),currencyDTO);
                    moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(bizObject.getCurrency(), 1);
                    moneyRoundMap.put(bizObject.getCurrency(),moneyRound);
                }
                verifyMoney(bizObject);
                bizObject.setDataOrigin(DateOrigin.Created);
                if ("cmp_bankreconciliationwd".equals(billContext.getBillnum())) { //期初对账单未达数据
                    verifyWd(bizObject,map);
                    bizObject.setInitflag(true);
                }else{ //银行对账单节点导入的数据
                    if(billContext.getBillnum().startsWith("cm")){//现金银行对账单
                        if(accentityBeginDateMap.containsKey(bizObject.getAccentity())){
                            periodFirstDate = accentityBeginDateMap.get(bizObject.getAccentity());
                        }else{
                            JedisLockUtils.isexistRjLock(bizObject.getAccentity());
                            periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate((String) bizObject.get(IBussinessConstant.ACCENTITY));/* 暂不修改 */
                            accentityBeginDateMap.put(bizObject.getAccentity(),periodFirstDate);
                        }
                        if (DateUtils.dateCompare(bizObject.getDzdate(), periodFirstDate) == -1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102617"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180198","导入交易日期不能小于现金管理启用期间第一天！") /* "导入交易日期不能小于现金管理启用期间第一天！" */);
                        }
                    }else{//总账银行对账单
                        if(accentityBeginDateGlMap.containsKey(bizObject.getAccentity())){
                            periodFirstDate = accentityBeginDateGlMap.get(bizObject.getAccentity());
                        }else{
                            periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get(IBussinessConstant.ACCENTITY), ISystemCodeConstant.ORG_MODULE_GL);/* 暂不修改 */
                            accentityBeginDateGlMap.put(bizObject.getAccentity(),periodFirstDate);
                        }
                        if (DateUtils.dateCompare(bizObject.getDzdate(), periodFirstDate) == -1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102618"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418019C","导入交易日期不能小于总账启用期间第一天！") /* "导入交易日期不能小于总账启用期间第一天！" */);
                        }
                    }

                    if (bizObject.getAcct_bal() == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102619"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A0","余额不能为空！") /* "余额不能为空！" */);
                    }

                }
                bizObject.setTran_date(bizObject.getDzdate());
                if (importFlag || openApiFlag) {
                    //导入进来的数据
                    checkIsunique(bizObject);//重复校验
                }
            }
        }
        return new RuleExecuteResult();
    }
    /**
     * 数据重复校验
     * @param bankReconciliation
     * @throws Exception
     */
    private void checkIsunique(BankReconciliation bankReconciliation) throws  Exception{
        QuerySchema querySchema = QuerySchema.create();
        querySchema.addSelect("count(1) as  count");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankReconciliation.getAccentity()),
                QueryCondition.name("bankaccount").eq(bankReconciliation.getBankaccount()),
                QueryCondition.name("currency").eq(bankReconciliation.getCurrency()),
                QueryCondition.name("dzdate").eq(bankReconciliation.getDzdate())
        );
        if (bankReconciliation.getDebitamount().compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("debitamount").eq(bankReconciliation.getDebitamount()));
        } else if (bankReconciliation.getCreditamount().compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("creditamount").eq(bankReconciliation.getCreditamount()));
        }
        if(null!=bankReconciliation.getTran_time()){
            group.addCondition(QueryCondition.name("tran_time").eq(bankReconciliation.getTran_time()));
        }
        if(null!=bankReconciliation.getBank_seq_no()){
            group.addCondition(QueryCondition.name("bank_seq_no").eq(bankReconciliation.getBank_seq_no()));
        }
        querySchema.addCondition(group);
        Map<String ,Object> map = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME,querySchema);
        Long count = (Long )map.get("count");
        if(count>0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102620"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A8","导入数据重复!") /* "导入数据重复!" */);
        }

    }
    /**
     * 根据会计主体和时间查询 日结对象
     * @param accentity
     * @param currDate
     * @return  true表示已日结,false表示未日结
     * @throws Exception
     */
    public boolean  querySettleState(String accentity,Date currDate) throws  Exception{
        boolean flag = true;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("settlementdate").eq(currDate), QueryCondition.name("settleflag").eq(true));
        querySchema.addCondition(group);
        Map<String, Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME, querySchema);
        if(ValueUtils.isNotEmpty(map)){
            flag =  true;
        } else{
            flag =  false;
        }
        settleStateMap.put(accentity,flag);
        return  flag;
    }

    /**
     * 导入数据根据筛选条件过滤
     * @param bankReconciliation
     * @param map
     */
    private void checkImportDate(BankReconciliation bankReconciliation, Map<String, Object> map) {
        BillDataDto billDataDto = (BillDataDto) map.get("param");
        Map<String, Object> map1 = billDataDto.getMapCondition();
        if(ValueUtils.isNotEmpty(map1)){
            String accentity = (String) map1.get(IBussinessConstant.ACCENTITY);
            String bankaccount = (String) map1.get("bankaccount");
            String currency = (String) map1.get("currency");
            if (!accentity.equals(bankReconciliation.getAccentity())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102621"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418019A","导入的会计主体与当前会计主体不一致!") /* "导入的会计主体与当前会计主体不一致!" */);
            }
            if (!bankaccount.equals(bankReconciliation.getBankaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102622"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418019B","导入的银行账户与当前银行账户不一致!") /* "导入的银行账户与当前银行账户不一致!" */);
            }
            if (!currency.equals(bankReconciliation.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102623"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418019E","导入的币种与当前币种不一致!") /* "导入的币种与当前币种不一致!" */);
            }
        }
    }

    private void  verifyMoney(BankReconciliation bizObject){
        if(StringUtils.isEmpty(bizObject.getAccentity())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100705"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A3","会计主体不能为空！") /* "会计主体不能为空！" */);
        }
        if (StringUtils.isEmpty(bizObject.getCurrency())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102624"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A4","币种不能为空！") /* "币种不能为空！" */);
        }
        if (StringUtils.isEmpty(bizObject.getBankaccount())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102625"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A6","银行账户不能为空！") /* "银行账户不能为空！" */);
        }
        if (bizObject.getDzdate() == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102626"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A9","交易日期不能为空！") /* "交易日期不能为空！" */);
        }
        if(bizObject.getDebitamount()==null&&bizObject.getCreditamount() == null){ //通过交易金额和借贷标志来判断借贷金额
            if(bizObject.getDc_flag()==null||bizObject.getTran_amt()==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102627"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00030", "借贷金额均为空时,借贷标和交易金额不能为空!") /* "借贷金额均为空时,借贷标和交易金额不能为空!" */);
            }
            if(bizObject.getTran_amt().compareTo(BigDecimal.ZERO) <= 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102628"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180199","交易金额需大于0") /* "交易金额需大于0" */);
            }
            if(bizObject.getDc_flag().equals(Direction.Debit)){
                bizObject.setDebitamount(bizObject.getTran_amt().setScale(currencyDTO.getMoneydigit(),moneyRound));
            }else if(bizObject.getDc_flag().equals(Direction.Credit)){
                bizObject.setCreditamount(bizObject.getTran_amt().setScale(currencyDTO.getMoneydigit(),moneyRound));
            }
        }else  if(bizObject.getDebitamount()!=null&&bizObject.getCreditamount() != null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102629"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418019D","借贷方金额只能输入一个!") /* "借贷方金额只能输入一个!" */);
        } else{
            //通过借贷金额来判断
            if (bizObject.getDebitamount() != null) {
//                if (bizObject.getDebitamount().compareTo(BigDecimal.ZERO) <= 0){
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102630"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A1","只输入借方金额时,借方金额需大于0") /* "只输入借方金额时,借方金额需大于0" */);
//                }
                bizObject.getDebitamount().setScale(currencyDTO.getMoneydigit(),moneyRound);
                bizObject.setDc_flag(Direction.Debit);
            } else {
//                if (bizObject.getCreditamount().compareTo(BigDecimal.ZERO) <= 0){
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102631"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A5","只输入贷方金额时,贷方金额需大于0") /* "只输入贷方金额时,贷方金额需大于0" */);
//                }
                bizObject.getCreditamount().setScale(currencyDTO.getMoneydigit(),moneyRound);
                bizObject.setDc_flag(Direction.Credit);
            }
            if (bizObject.getTran_amt() != null) {
                bizObject.getTran_amt().setScale(currencyDTO.getMoneydigit(),moneyRound);
            }
        }
        if (bizObject.getAcct_bal() != null) {
            bizObject.getAcct_bal().setScale(currencyDTO.getMoneydigit(),moneyRound);
        }
    }


    private void verifyWd(BankReconciliation bizObject,Map<String, Object> map) throws  Exception{
        if(accentityBeginDateMap.containsKey(bizObject.getAccentity())){
            periodFirstDate = accentityBeginDateMap.get(bizObject.getAccentity());
        }else{
            JedisLockUtils.isexistRjLock(bizObject.getAccentity());
            periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate((String) bizObject.get(IBussinessConstant.ACCENTITY));/* 暂不修改 */
            accentityBeginDateMap.put(bizObject.getAccentity(),periodFirstDate);
        }
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());
        if (importFlag) {
            //导入进来的数据
            checkImportDate(bizObject, map);
        }
        if (DateUtils.dateCompare( bizObject.getDzdate(), periodFirstDate) != -1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102632"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A2","期初单据日期不能晚于现金管理启用期间第一天！") /* "期初单据日期不能晚于现金管理启用期间第一天！" */);
        }
        if(settleStateMap.containsKey(bizObject.getAccentity())){
            settleState = settleStateMap.get(bizObject.getAccentity());
        }else{
            settleState =  querySettleState(bizObject.getAccentity(),periodFirstDate);
        }
        if (settleState) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102633"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801A7","期初单据已日结，不允许再保存期初单据！") /* "期初单据已日结，不允许再保存期初单据！" */);
        }
    }

}
