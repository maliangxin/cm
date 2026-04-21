package com.yonyoucloud.fi.cmp.accounthistorybalance.rule;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceServiceImpl;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.AcctOpenType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

;

/**
 * 历史余额保存前规则
 */
@Slf4j
@Component("beforeSaveHistoryBalanceRule")
public class BeforeSaveHistoryBalanceRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;


    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private AccountHistoryBalanceServiceImpl accountHistoryBalanceServiceImpl;

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        List<YmsLock> ymsLockList = new ArrayList<>();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());
        for (BizObject bizObject : bills) {
            // OpenApi
            boolean openApiFlag = bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true);
            //根据会计主体，银行账号，余额日期判断是否更新
            // 1、对余额日期进行校验
            Date balancedate =  bizObject.get("balancedate");
            Date now = DateUtils.getNowDateShort2();
            if(balancedate.compareTo(now)>=0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100314"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803BB","银行账户历史余额日期必须在系统当前日期之前") /* "银行账户历史余额日期必须在系统当前日期之前" */);
            }
            BigDecimal acctbal = bizObject.get("acctbal");
            BigDecimal avlbal = bizObject.get("avlbal");
            BigDecimal frzbal = bizObject.get("frzbal")==null?BigDecimal.ZERO:bizObject.get("frzbal");
            if((acctbal != null && avlbal != null)){
                if(acctbal.compareTo(BigDecimal.ZERO) >0  && avlbal.compareTo(BigDecimal.ZERO)>0) {
                    if (acctbal.compareTo(avlbal) < 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100315"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_180E028A0578031B", "可用余额不得超过账户余额") /* "可用余额不得超过账户余额" */);
                    }
                }
                if(acctbal.compareTo(BigDecimal.ZERO) >0  && frzbal.compareTo(BigDecimal.ZERO)>0) {
                    if (acctbal.compareTo(frzbal) < 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100316"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080067", "冻结金额不得超过账户余额") /* "冻结金额不得超过账户余额" */);
                    }
                }
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100317"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080068", "金额值不得为空") /* "金额值不得为空" */);
            }
            // TODO 先校验是否为内部户，内部户金额可以为负数，original银行类型（内部户和银行账户标志）：0为银行账户，1为内部户
            EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterpriseBankAccount"));
            if(enterpriseBankAcctVO == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100318"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080069", "账户[%s]已停用，请重新选择！") /* "账户[%s]已停用，请重新选择！" */,bizObject.get("enterpriseBankAccount_account").toString()));
            }
            String frzbalMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540077A", "冻结") /* "冻结" */;
            Integer acctopentypeInt = enterpriseBankAcctVO.getAcctopentype();
            AcctOpenType acctOpenType = AcctOpenType.find(acctopentypeInt);
            if (0 == acctopentypeInt) {
                if(BigDecimal.ZERO.compareTo(frzbal) > 0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100319"),frzbalMsg+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B8","金额值不得为负数") /* "金额值不得为负数" */);
                }
            } else {
                // 内部户账户余额可以为负数，删除acctbal大于零的校验
                if(BigDecimal.ZERO.compareTo(frzbal) > 0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100319"),frzbalMsg+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B8","金额值不得为负数") /* "金额值不得为负数" */);
                }
            }
            if (BigDecimal.ZERO.compareTo(acctbal) < 0) {
                // 账户余额大于0
                bizObject.set("depositbalance", acctbal.abs());
                bizObject.set("overdraftbalance", BigDecimal.ZERO);
            } else {
                // 账户余额小于0
                bizObject.set("depositbalance", BigDecimal.ZERO);
                bizObject.set("overdraftbalance", acctbal.abs());
            }
            //账户余额=可用金额+冻结金额，（删除校验）
//            BigDecimal total = BigDecimalUtils.safeAdd(avlbal, frzbal);
//            if (BigDecimal.ZERO.compareTo(BigDecimalUtils.safeSubtract(acctbal, total)) != 0) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100320"),MessageUtils.getMessage("P_YS_FI_CM_1421570841396641804") /* "账户余额需要等于可用金额与冻结金额合计值" */);
//            }
            // TODO 校验 银行账户 数据权限
            //银行账户要在对应会计主体下
            if(bizObject.get("enterpriseBankAccount")!=null){
                //银行账户
                Integer enable = enterpriseBankAcctVO.getEnable();
                if(1!=enable){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100321"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E3", "银行账号为:") /* "银行账号为:" */+enterpriseBankAcctVO.getAccount()+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E2", "的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
                }
                CmpCommonUtil.checkBankAcctCurrency(bizObject.get("enterpriseBankAccount"), bizObject.get("currency"));
                String accentity = enterpriseBankAcctVO.getOrgid();
                if (bizObject.get(IBussinessConstant.ACCENTITY) != null && !bizObject.get(IBussinessConstant.ACCENTITY).equals(accentity)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100322"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080065", "银行账号与所属组织不匹配！") /* "银行账号与所属组织不匹配！" */);
                }else if(bizObject.get(IBussinessConstant.ACCENTITY) == null){
                    // 如果导入数据所属组织为空，取银行账户所属组织赋值
                    bizObject.set(IBussinessConstant.ACCENTITY,enterpriseBankAcctVO.getOrgid());
                }
            }
            bizObject.set("banktype", enterpriseBankAcctVO.getBank());
            // 加redis锁，控制四要素相同数据
            String accentity = bizObject.get(IBussinessConstant.ACCENTITY);
            String enterpriseBankAccount = bizObject.get("enterpriseBankAccount");
            String currency = bizObject.get("currency");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String balancedateStr = sdf.format(balancedate);
            String lockStr = "historyBalanceLock_" + accentity + enterpriseBankAccount + currency + balancedateStr;
            YmsLock ymsLock = null;
            YmsLock ymsLock2 = null;
            try{
                if ((ymsLock=JedisLockUtils.lockRuleWithOutTrace(lockStr,paramMap))==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100323"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B7","导入数据重复，请检查后重试！") /* "导入数据重复，请检查后重试！" */);
                }
                ymsLockList.add(ymsLock);
                // 如果，会计主体，银行账户，币种，余额日期， 全部相同，则直接覆盖更新
                if (EntityStatus.Insert == bizObject.get("_status")){//新增才覆盖更新，其他操作不用（如Update）
                    boolean hasConfirmed = hasConfirmedData(bizObject,balancedate);
                    if(hasConfirmed){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100324"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_227FF98C05D00006", "账号[%s]、日期[%s]的余额已确认，不允许录入，请检查！") /* "账号【xxx】、日期【XXX】的余额已确认，不允许录入，请检查 ！" */,enterpriseBankAcctVO.getAccount(),balancedateStr));
                    }
                    QuerySchema schema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                    //如果是自当任务调用 会有多个符合条件的会计主体 此时params中没有accEntity的相关信息 ，只根据账户id进行后续操作
                    if(bizObject.get(IBussinessConstant.ACCENTITY)!=null){
                        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY)));
                    }
                    conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bizObject.get("enterpriseBankAccount")));
                    conditionGroup.appendCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
                    conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(balancedate));
                    conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
                    schema.addCondition(conditionGroup);
                    List<AccountRealtimeBalance> existBalances = null;
                    existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
                    if (existBalances != null && existBalances.size() > 0) {
                        MetaDaoHelper.delete(AccountRealtimeBalance.ENTITY_NAME, existBalances);
                    }
                    if (importFlag) {
                        bizObject.set("datasource", BalanceAccountDataSourceEnum.MANUAL_IMPORT.getCode());
                        // 判断授权使用组织
                        /**
                         * 1，获取授权的组织
                         * 2，获取银行账户的适用范围
                         * 3，判断适用范围内的组织是否已授权，只要存在一个授权的，就可以导入
                         */
                        // 获取授权的组织
                        Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.ACCHISBAL);
                        if(orgs != null && orgs.size() <1){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100324"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080066", "当前用户无银行账号[%s]的导入权限，请检查！") /* "当前用户无银行账号[%s]的导入权限，请检查！" */,enterpriseBankAcctVO.getAccount()));
                        }
                        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.get("enterpriseBankAccount"));
                        if(enterpriseBankAcctVoWithRange != null){
                            List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                            // 使用范围中的组织是否是授权的组织
                            Boolean containFlag = false;
                            for(OrgRangeVO orgRangeVO : orgRangeVOS){
                                if(orgs.contains(orgRangeVO.getRangeOrgId())){
                                    containFlag = true;
                                    break;
                                }
                            }
                            if(!containFlag){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100324"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080066", "当前用户无银行账号[%s]的导入权限，请检查！") /* "当前用户无银行账号[%s]的导入权限，请检查！" */,enterpriseBankAcctVO.getAccount()));
                            }
                        }
                    }else {
                        bizObject.set("datasource", BalanceAccountDataSourceEnum.MANUAL_ENTRY.getCode());
                    }
                }else if(EntityStatus.Update == bizObject.get("_status")){
                    //加锁
                    String lockkey = ICmpConstant.MY_BILL_CLAIM_LIST + bizObject.get("id");
                    if ((ymsLock2=JedisLockUtils.lockRuleWithOutTraceByTime(lockkey,5,paramMap))==null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100325"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1808B3BE04D00009", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
                    }
                    ymsLockList.add(ymsLock2);
                    bizObject.set("datasource", BalanceAccountDataSourceEnum.MANUAL_ENTRY.getCode());
                    List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryById(AccountRealtimeBalance.ENTITY_NAME, "*", bizObject.get("id"));
                    Map<String, Object> nowBalances = existBalances.get(0);
                    if(!nowBalances.get("acctbal").equals(bizObject.get("acctbal"))){
                        //先补齐缺失历史余额
                        //获取昨天日期
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DATE, -1);
                        Date date = calendar.getTime();
                        LocalDate localDate= LocalDate.parse(DateUtils.formatDate(date),formatter);
                        date = java.sql.Date.valueOf(localDate);
                        //根据银行账号查询所有历史余额
                        QuerySchema schema2 = QuerySchema.create().addSelect(" * ");//
                        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
                        conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(nowBalances.get("enterpriseBankAccount")));
                        conditionGroup2.appendCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
                        schema2.addCondition(conditionGroup2);
                        schema2.addOrderBy("balancedate");
                        List<AccountRealtimeBalance> allBalanceOfAccount = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
                        // 将当前银行账户历史余额存入
                        Map<Object, BizObject> accountRealtimeBalanceMap1 = new HashMap<>();
                        for(AccountRealtimeBalance accountRealtimeBalance2 : allBalanceOfAccount){
                            String balanceKey = new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance2.getBalancedate())+accountRealtimeBalance2.getCurrency();
                            accountRealtimeBalanceMap1.put(balanceKey, accountRealtimeBalance2);
                        }
                        String nowbalanceKey = new SimpleDateFormat("yyyy-MM-dd").format(nowBalances.get("balancedate"))+nowBalances.get("currency");
                        AccountRealtimeBalance nowAccountRealtimeBalance = (AccountRealtimeBalance)accountRealtimeBalanceMap1.get(nowbalanceKey);
                        nowAccountRealtimeBalance.setAcctbal(bizObject.get("acctbal"));
                        accountRealtimeBalanceMap1.put(nowbalanceKey, nowAccountRealtimeBalance);
                        //非内部账户才做弥补
                        if(!AcctOpenType.SETTLEMENT_CENTER_ACCOUNT_OPENING.equals(acctOpenType)){
                            //从当前余额日期的下一天开始补充缺失余额
                            Date nextDate = DateUtils.dateAdd((Date)nowBalances.get("balancedate"),1,Boolean.FALSE);
                            accountHistoryBalanceServiceImpl.supplementBalance(DateUtils.formatBalanceDate(nextDate),date,nowAccountRealtimeBalance,accountRealtimeBalanceMap1);
                            //修改账户余额后逻辑
                            /**
                             * 1，获取当前余额日期之后的所有余额记录
                             * 2，判断每一条余额记录是否是银企联下载，不是银企联下载的记录，重新计算账户余额
                             * 3，获取对应日期的净收支额，重新计算当日余额：计算算法：X日的余额=X-1日的余额+X日收入-X日支出
                             */
                            //1,获取当前余额日期之后的所有余额记录
                            QuerySchema schema = QuerySchema.create().addSelect(" * ");//
                            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get("accentity")));
                            conditionGroup.appendCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
                            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bizObject.get("enterpriseBankAccount")));
                            conditionGroup.appendCondition(QueryCondition.name("balancedate").between(bizObject.get("balancedate"), null));
                            conditionGroup.appendCondition(QueryCondition.name("id").not_eq(bizObject.get("id")));
                            schema.addCondition(conditionGroup);
                            schema.addOrderBy("balancedate");
                            List<AccountRealtimeBalance> existBalances2 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
                            //将需要修改的数据存入map
                            Map<Object, BizObject> accountRealtimeBalanceMap = new HashMap<>();
                            for(AccountRealtimeBalance accountRealtimeBalance : existBalances2){
                                String balanceKey = new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate())+accountRealtimeBalance.getCurrency();
                                accountRealtimeBalanceMap.put(balanceKey, accountRealtimeBalance);
                            }
                            AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
                            accountRealtimeBalance.setId(nowBalances.get("id"));
                            accountRealtimeBalance.setAcctbal(bizObject.get("acctbal"));
                            String balanceKey = new SimpleDateFormat("yyyy-MM-dd").format(nowBalances.get("balancedate"))+bizObject.get("currency");
                            accountRealtimeBalanceMap.put(balanceKey,accountRealtimeBalance);
                            if(null != existBalances2 && existBalances2.size() > 0){
                                //2，处理修改余额后逻辑
                                afterEditAcctbal(existBalances2, accountRealtimeBalanceMap);
                            }
                        }
                    }
                }
            }catch (Exception e){
                JedisLockUtils.unlockRuleWithOutTrace(paramMap);
                throw e;
            }finally {
                JedisLockUtils.unlockRuleWithOutTrace(paramMap);
            }
            bizObject.set("flag", BalanceFlag.Manually.getCode());
            BigDecimal bal_amt = !bizObject.containsKey("bal_amt") ? BigDecimal.ZERO : bizObject.getBigDecimal("bal_amt");
            BigDecimal acctbalamt =  bizObject.get("acctbal");
            bizObject.set("total_amt", bal_amt.add(acctbalamt));
        }
        return new RuleExecuteResult();
    }

    private boolean hasConfirmedData(BizObject bizObject,Date balancedate) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("enterpriseBankAccount,currency,isconfirm");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //如果是自当任务调用 会有多个符合条件的会计主体 此时params中没有accEntity的相关信息 ，只根据账户id进行后续操作
        if(bizObject.get(IBussinessConstant.ACCENTITY)!=null){
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY)));
        }
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bizObject.get("enterpriseBankAccount")));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(balancedate));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        conditionGroup.appendCondition(QueryCondition.name("isconfirm").eq("1"));
        schema.addCondition(conditionGroup);
        List<AccountRealtimeBalance> existBalances = null;
        existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        return existBalances != null && existBalances.size() > 0;
    }

    private void afterEditAcctbal(List<AccountRealtimeBalance> accountRealtimeBalanceList, Map<Object, BizObject> accountRealtimeBalanceMap) throws Exception {
        for(AccountRealtimeBalance accountRealtimeBalance : accountRealtimeBalanceList){
            //银企联下载，不修改余额
            if(accountRealtimeBalance.getDatasource() == null ||
                    accountRealtimeBalance.getDatasource() == BalanceAccountDataSourceEnum.SUPPLEMENTARY_ADJUSTMENTS.getCode()){
                //获取当日净支付额，查询银行对账单
                QuerySchema schema = QuerySchema.create().addSelect(" * ");//
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                //conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.get("accentity")));
                conditionGroup.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.get("currency")));
                conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                conditionGroup.appendCondition(QueryCondition.name("tran_date").between(accountRealtimeBalance.get("balancedate"), accountRealtimeBalance.get("balancedate")));
                conditionGroup.appendCondition(QueryCondition.name("tran_date").between(accountRealtimeBalance.get("balancedate"), accountRealtimeBalance.get("balancedate")));
                //期初数据不参与余额弥补，只用于对账
                conditionGroup.appendCondition(QueryCondition.name(BankReconciliation.INITFLAG).eq(false));
                schema.addCondition(conditionGroup);
                List<BankReconciliation> existBankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
                //计算当日净收支额
                BigDecimal nowDayBankMoney = new BigDecimal(0);
                if(existBankReconciliationList != null && existBankReconciliationList.size() > 0){
                    // +贷的合计-借的合计
                    for(BankReconciliation bankReconciliation : existBankReconciliationList){
                        if (!BankreconciliationUtils.isNoProcess(bankReconciliation.getAccentity()) && bankReconciliation.getSerialdealtype() != null && bankReconciliation.getSerialdealtype() == (short) 5) {
                            //组织参数 无需处理的流水，是否参与银企对账、银行账户余额弥 为否，并且对应数据的是否无需处理为是，不参数计算
                            continue;
                        }
                        //贷
                        if(bankReconciliation.getDc_flag() != null && Direction.Credit.equals(bankReconciliation.getDc_flag())){
                            nowDayBankMoney = nowDayBankMoney.add(bankReconciliation.getTran_amt());
                        }else if(bankReconciliation.getDc_flag() != null && Direction.Debit.equals(bankReconciliation.getDc_flag())){
                            //借
                            nowDayBankMoney = nowDayBankMoney.subtract(bankReconciliation.getTran_amt());
                        }
                    }
                }
                //获取前一天历史余额日期
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                Date preDate = DateUtils.dateAdd(accountRealtimeBalance.getBalancedate(), -1, Boolean.FALSE);
                LocalDate localDate = LocalDate.parse(DateUtils.formatDate(preDate),formatter);
                preDate = java.sql.Date.valueOf(localDate);
                String balanceKey = preDate+accountRealtimeBalance.getCurrency();
                if(accountRealtimeBalanceMap.containsKey(balanceKey)){
                    BigDecimal preDayAcctbal = (accountRealtimeBalanceMap.get(balanceKey)).get("acctbal");
                    //计算当日余额
                    if(!accountRealtimeBalance.getAcctbal().equals(preDayAcctbal.add(nowDayBankMoney))){
                        //余额日期有变化
                        accountRealtimeBalance.setDatasource(BalanceAccountDataSourceEnum.SUPPLEMENTARY_ADJUSTMENTS.getCode());
                        accountRealtimeBalance.setFlag(BalanceFlag.Manually.getCode());
                    }
                    accountRealtimeBalance.setAcctbal(preDayAcctbal.add(nowDayBankMoney));

                    //设置存款余额 setDepositbalance 和 透支余额 setOverdraftbalance
                    if (accountRealtimeBalance.getAcctbal().compareTo(BigDecimal.ZERO) > 0) {
                        accountRealtimeBalance.setDepositbalance(accountRealtimeBalance.getAcctbal().abs());
                        accountRealtimeBalance.setOverdraftbalance(BigDecimal.ZERO);
                    } else if (accountRealtimeBalance.getAcctbal().compareTo(BigDecimal.ZERO) < 0) {
                        accountRealtimeBalance.setDepositbalance(BigDecimal.ZERO);
                        accountRealtimeBalance.setOverdraftbalance(accountRealtimeBalance.getAcctbal().abs());
                    } else {
                        accountRealtimeBalance.setDepositbalance(BigDecimal.ZERO);
                        accountRealtimeBalance.setOverdraftbalance(BigDecimal.ZERO);
                    }
                    //更新余额检查结果
                    accountRealtimeBalance.setBalancecheckinstruction(null);
                    accountRealtimeBalance.setBalancecontrast(null);
                    //更新修改时间，否则pubts不会改
                    accountRealtimeBalance.setModifyTime(new Date());

                    // 可用余额 = 账户余额-冻结金额
                    accountRealtimeBalance.setAvlbal(BigDecimalUtils.safeSubtract(accountRealtimeBalance.getAcctbal(),accountRealtimeBalance.getFrzbal()));
                    EntityTool.setUpdateStatus(accountRealtimeBalance);
                    MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, accountRealtimeBalance);

                    accountRealtimeBalanceMap.put(new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate())+accountRealtimeBalance.getCurrency(),accountRealtimeBalance);
                }
            }
        }
    }

}
