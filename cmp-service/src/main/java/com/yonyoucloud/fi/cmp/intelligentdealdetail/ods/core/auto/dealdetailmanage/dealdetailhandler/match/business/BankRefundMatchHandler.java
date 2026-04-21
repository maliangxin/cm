package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business;
import com.google.common.collect.ImmutableMap;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.autorefundcheckrule.AutoRefundCheckRule;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.cmpentity.MatchDirectionType;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailBusinessCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.IBankDealDetailChain;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst.RuleCodeConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.CmpCheckAndProcessRuleLogProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog.RuleLogEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CmpCheckRuleCommonProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
/**
 * @Author guoyangy
 * @Date 2024/6/18 15:33
 * @Description 银行退票规则
 * @Version 1.0
 */
@Service(RuleCodeConst.SYSTEM002)
@Slf4j
public class BankRefundMatchHandler extends DefaultStreamBatchHandler {
    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;
    /**
     * 智能流水退票辨识接入
     * executeStatus=1 阻断性规则且满足阻断条件，下一步执行具体流程处理
     * executeStatus=2 需要人工介入
     * executestatus=3 本规则顺利执行完成，可以执行下一个规则
     * executestatus=4 系统异常，比如空指针、超时、数据库操作失败等
     *
     * @param context
     * @param chain
     * @return
     */
    @Override
    public Map<String, List<BankReconciliation>> streamHandler(BankDealDetailContext context, IBankDealDetailChain chain) {
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        try {
            long s0 = System.currentTimeMillis();
            if (DealDetailEnumConst.SAVE_DIRECT.equals(context.getSaveDirect())){
                return ImmutableMap.of(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(), getBankReconciliationList(context));
            }
            context.getParamsConfigs().put("ruleCode","system002");
            // 待辨识数据集合
            List<BankReconciliation> bankReconciliationList = this.getBankReconciliationList(context);
            context.setLogName(RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc());
            context.setOperationName(RuleLogEnum.RuleLogProcess.BANK_REFUND_START.getDesc());
            this.bankRefundMatchMainHandler(bankReconciliationList,context);
            log.error("【银行退票匹配】一个批次=======================执行完成,包含{}条流水明细,匹配银行回单共耗时{}s",  org.springframework.util.CollectionUtils.isEmpty(bankReconciliationList) ? "0" : bankReconciliationList.size(), (System.currentTimeMillis() - s0) / 1000.0);

        }catch (Exception e){
            log.error("智能流水执行辨识异常：退票识异常",e);
        }

        return resultMap;
    }
    /**
     * 外部业务接入
     * */
    public List<BankReconciliation> bankRefundMatchHandler(List<BankReconciliation> bankReconciliationList){
        if(CollectionUtils.isEmpty(bankReconciliationList)){
            return bankReconciliationList;
        }
        List<BankReconciliation> originalBankReconciliationList = new ArrayList<>(bankReconciliationList);
        try {
            int originCount = bankReconciliationList.size();
            Map<String, List<BankReconciliation>> resultMap = this.bankRefundMatchMainHandler(bankReconciliationList,null);
            List<BankReconciliation> bankReconciliationResultList = new ArrayList<>();
            for(Map.Entry<String,List<BankReconciliation>> entry:resultMap.entrySet()){
                List<BankReconciliation> bankReconciliations = entry.getValue();
                if(CollectionUtils.isNotEmpty(bankReconciliations)){
                    bankReconciliationResultList.addAll(bankReconciliations);
                }
            }
            if(originCount != bankReconciliationResultList.size()){
            log.error("退票辨识前后流水数量不一致");
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400570", "退票辨识前后流水数量不一致") /* "退票辨识前后流水数量不一致" */);
            }
            return bankReconciliationResultList;
        } catch (Exception e) {
            log.error("退票辨识异常",e);
            // 内部处理异常返回原集合
            return originalBankReconciliationList;
        }
    }
    public Map<String, List<BankReconciliation>> bankRefundMatchMainHandler(List<BankReconciliation> bankReconciliationList,BankDealDetailContext context){
        Map<String, List<BankReconciliation>> resultMap = new HashMap<>();
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus(),new ArrayList<>());
        resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(),new ArrayList<>());

        //先筛选出已退票或疑似退票的流水，不需要在执行退票辨识
        bankReconciliationList = bankReconciliationList.stream().filter(b->{
            Short refundStatus = b.getRefundstatus();
            if(null!=refundStatus && (RefundStatus.MaybeRefund.getValue() == refundStatus || RefundStatus.Refunded.getValue() == refundStatus)){
                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).add(b);
                if (context != null) {
                    CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(b,context.getLogName(),DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getDesc(),context);
                }
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        try {
            //1.读取退票辨识规则
            AutoRefundCheckRule refundCheckRule = refundAutoCheckRuleService.queryRuleInfo(null);
            if(null == refundCheckRule){
                bankReconciliationList.stream().forEach(b->{
                    if (context != null) {
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(b,context.getLogName(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400571", "银行退票匹配根据条件查询退票辨识规则为空！，请预置初始化退票辨识数据！") /* "银行退票匹配根据条件查询退票辨识规则为空！，请预置初始化退票辨识数据！" */,context);
                    }
                });
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO7.getCode(),bankReconciliationList);
                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(bankReconciliationList);
                return resultMap;
            }
            //1.先对当前传入的整批次流水互相做疑似退票逻辑处理
            List<BankReconciliation> needPendingList = this.matchReFundDataInBatch(bankReconciliationList, refundCheckRule,resultMap,context);
            // 收款负金额和付款正金额直接跳过，不作为退票检测的数据参照
            bankReconciliationList = bankReconciliationList.stream().filter(b->{
                // 支付方向
                Direction dcFlag = b.getDc_flag();
                // 金额
                BigDecimal tranAmt = b.getTran_amt();
                // 收款负金额不进行退票
                if (null!=dcFlag && (Direction.Credit.getValue() == dcFlag.getValue() &&  (null!=tranAmt && tranAmt.compareTo(BigDecimal.ZERO) < 0))
                        // 付款正金额不进行退票
                        || (null!=dcFlag && (Direction.Debit.getValue() == dcFlag.getValue() &&  (null!=tranAmt && tranAmt.compareTo(BigDecimal.ZERO) > 0)))) {
                    resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).add(b);
                    if (context != null) {
                        CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(b,context.getLogName(),DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getDesc(),context);
                    }
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            //3.从库中读取流水与当前批次流水做疑似退票逻辑处理
            List<BankReconciliation> matchList = this.getBankReconciliations(refundCheckRule, bankReconciliationList);
            if(CollectionUtils.isNotEmpty(needPendingList)){
                resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), needPendingList);
            }
            if(CollectionUtils.isEmpty(matchList)){
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO1.getCode(),bankReconciliationList);
                resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(bankReconciliationList);
                return resultMap;
            }
            //已匹配上的needPendingList流水从matchList中去除，防止重复匹配
            this.removeMatchList(needPendingList,matchList);
            //3.当前批次流水与数据库流水做疑似退票逻辑处理
            if(CollectionUtils.isNotEmpty(bankReconciliationList)){
                List<BankReconciliation> updateList = new ArrayList<>();
                List<BankReconciliation> pendingList = new ArrayList<>();
                this.matchReFundDataLogic(bankReconciliationList,matchList,refundCheckRule,updateList,pendingList,context,"true");
                //4.处理返回结果
                if(CollectionUtils.isNotEmpty(bankReconciliationList)){
                    resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(bankReconciliationList);
                }
                pendingList.addAll(needPendingList);
                if(CollectionUtils.isNotEmpty(pendingList)){
                    resultMap.put(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_PENDING.getStatus(), pendingList);
                }
                if (CollectionUtils.isNotEmpty(updateList)) {
                    CommonSaveUtils.updateBankReconciliation(updateList);
                }
            }
            return resultMap;
        } catch (Exception e) {
            log.error("调用退票辨识规则失败:{}", e);
            DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02S02.getCode(),bankReconciliationList);
        }
        return resultMap;
    }

    private void removeMatchList(List<BankReconciliation> needPendingList, List<BankReconciliation> matchListFromDB) {
        if(!CollectionUtils.isEmpty(matchListFromDB)&&!CollectionUtils.isEmpty(needPendingList)){
            Iterator<BankReconciliation> pendingIterator = needPendingList.iterator();
            while(pendingIterator.hasNext()){
                BankReconciliation b = pendingIterator.next();
                Iterator<BankReconciliation> matchIterator = matchListFromDB.iterator();
                while(matchIterator.hasNext()){
                    BankReconciliation matchB = matchIterator.next();
                    if(b.getId().equals(matchB.getId())){
                        matchIterator.remove();
                    }
                }
            }
        }
    }
    /**
     * 根据数据类型区分
     * 获取监测数据集合，匹配数据集合
     * 匹配对方账号不为空，退票状态为空，交易日期是今天及前一天的数据
     *
     * @return
     * @throws Exception
     */
    List<BankReconciliation> getBankReconciliations(AutoRefundCheckRule refundCheckRule,List<BankReconciliation> bankReconciliationList) throws Exception {
        List<BankReconciliation> list;
        if (CollectionUtils.isEmpty(bankReconciliationList) ) {
            list = new ArrayList<>();
            return list;
        }
        // 金额的集合
        List<BigDecimal> tranAmtList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            // 金额
            BigDecimal tranAmt = bankReconciliation.getTran_amt();
            // 负金额取绝对值
            if (null!=tranAmt &&  tranAmt.compareTo(BigDecimal.ZERO) < 0) {
                tranAmt = tranAmt.abs();
            }
            if (!tranAmtList.contains(tranAmt)) {
                // 金额的集合
                tranAmtList.add(tranAmt);
            }
        }
        Integer dataRange = refundCheckRule.getDaterange();
        if (dataRange == null) {
            dataRange = 1;
        }
        // 下面查询不能替换成*，因为特征字段在copy时报错
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        //默认为查询监测数据列表
        QueryConditionGroup group1;
        //如果listType 为 match  则查询匹配数据列表
        group1 = QueryConditionGroup.and(
                QueryCondition.name("refundstatus").is_null(),
                QueryCondition.name("tran_date").between(
                        DateUtils.formatBalanceDate(DateUtils.dateAddDays(DateUtils.getNow(), -1 * dataRange)),
                        DateUtils.formatBalanceDate(DateUtils.getNow())),
                QueryCondition.name("bankaccount").eq(bankReconciliationList.get(0).getBankaccount()),
                QueryCondition.name("tran_amt").in(tranAmtList),
                QueryCondition.name("dc_flag").eq(Direction.Debit.getValue())
        );

        querySchema.addCondition(group1);
        list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return list;
    }
    /**
     * 进入规则待处理的流水还没有入库，先比较这批流水之间互为退票的
     * */
    private List<BankReconciliation> matchReFundDataInBatch(List<BankReconciliation> checkList, AutoRefundCheckRule refundCheckRule,Map<String,List<BankReconciliation>> resultMap,BankDealDetailContext context){
        List<BankReconciliation> updateList = new ArrayList<>();
        List<BankReconciliation> pendingList = new ArrayList<>();
        List<BankReconciliation> matchList = new ArrayList<>();
        List<BankReconciliation> notRefundList = new ArrayList<>();
        //先筛选出满足退票条件的流水
        Iterator<BankReconciliation> iterator = checkList.iterator();
        while (iterator.hasNext()){
            BankReconciliation b = iterator.next();
            //满足退票条件
            //if(!((!StringUtils.isEmpty(b.getAccentity())&&b.getRefundstatus() == null && (null!=b.getOppositetype() && b.getOppositetype()!=OppositeType.InnerOrg.getValue()))||(b.getRefundstatus() == null &&b.getOppositetype() == null))){
            //放开内部户
            //todo 没懂？？？先不动
            if(!((!StringUtils.isEmpty(b.getAccentity())&&b.getRefundstatus() == null )||(b.getRefundstatus() == null &&b.getOppositetype() == null))){
                iterator.remove();
                //不满足退票条件
                if (!DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().equals(b.getProcessstatus())){
                    notRefundList.add(b);
                }
            }
        }
        //保证反参流水数量一致，把排除的流水加回去
        if(CollectionUtils.isNotEmpty(notRefundList)){
            resultMap.get(DealDetailEnumConst.ExecuteStatusEnum.EXECUTE_STATUS_SUCCESS_CONTINUE.getStatus()).addAll(notRefundList);
        }
        matchList.addAll(checkList);
        //这一批次的流水互相做流水退票辨识匹配
        this.matchReFundDataLogic(checkList,matchList,refundCheckRule,updateList,pendingList,context,null);
        //匹配到疑似退票流水后，将疑似退票流水从主流水集合中移除，剩余的主流水下一步与DB中流水做比较
        if(CollectionUtils.isNotEmpty(updateList)){
            pendingList.addAll(updateList);
            for(BankReconciliation b:updateList){
                checkList.remove(b);
            }
        }
        return pendingList;
    }
    private void matchReFundDataLogic(List<BankReconciliation> checkList,List<BankReconciliation> matchList,AutoRefundCheckRule refundCheckRule,List<BankReconciliation> updateList,List<BankReconciliation> pendingList,BankDealDetailContext context,String dbCheckMark){
        /*
         * 匹配逻辑 ----
         * 循环监测对账单 -- 匹配待确认数据
         * 根据已匹配对账单map  判断是否已匹配过
         * 如未匹配过 进行匹配操作
         * 如匹配到 将双方对账单数据 存入updateList  修改退票状态
         * 同时将双方id 记录到已匹配对账单map内
         * O(n2)
         */
        Iterator<BankReconciliation> checkIterator = checkList.iterator();
        Set<Long> matchedData = new HashSet<>();
        while(checkIterator.hasNext()){
            BankReconciliation checkBankReconciliation = checkIterator.next();
            if (StringUtils.isNotEmpty(dbCheckMark)){
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),RuleLogEnum.RuleLogProcess.BANK_REFUND_STEP_THREE.getDesc(),context);
            }else {
                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),RuleLogEnum.RuleLogProcess.BANK_REFUND_STEP_ONE.getDesc(),context);
            }
            //已被标记为疑似退票的流水不在处理
            Short refundStatus = checkBankReconciliation.getRefundstatus();
            if(refundStatus != null && (refundStatus == ReFundType.SUSPECTEDREFUND.getValue()||refundStatus == ReFundType.REFUND.getValue())){
                continue;
            }
            try {
                //循环匹配数据
                Iterator<BankReconciliation> matchIterator = matchList.iterator();
                while(matchIterator.hasNext()){
                    BankReconciliation matchBankReconciliaton  = matchIterator.next();
                    if(matchedData.contains(matchBankReconciliaton.getId())){
                        continue;
                    }
                    Long checkBankReconciliationId = checkBankReconciliation.getId();
                    if(checkBankReconciliation.getId()!=null&&checkBankReconciliationId.equals(matchBankReconciliaton.getId())){
                        //排除流水自匹配
                        continue;
                    }
                    //对方银行账号为空的跳过
                    //匹配数据条件
                    //银行账户相同
                    Boolean bankaccount = checkBankReconciliation.getBankaccount().equals(matchBankReconciliaton.getBankaccount());
                    //币种相同
                    Boolean currency = checkBankReconciliation.getCurrency().equals(matchBankReconciliaton.getCurrency());
                    //20230228 退票辨识规则添加后修改逻辑
                    //借贷方向相同
                    Boolean dc_flag = checkBankReconciliation.getDc_flag().equals(matchBankReconciliaton.getDc_flag());
                    //金额相同
                    Boolean tran_amt_same = checkBankReconciliation.getTran_amt().compareTo(matchBankReconciliaton.getTran_amt()) == 0;
                    //金额互为相反数
                    Boolean tran_amt_opposite = checkBankReconciliation.getTran_amt().compareTo(matchBankReconciliaton.getTran_amt().negate())==0;
                    //账户共享，退票要保证授权使用组织相同
                    Boolean use_org_same = false;
                    String accentity = checkBankReconciliation.getAccentity();
                    if(StringUtils.isEmpty(accentity)){
                        use_org_same = StringUtils.isEmpty(matchBankReconciliaton.getAccentity());
                    }else{
                        use_org_same = accentity.equals(matchBankReconciliaton.getAccentity());
                    }
                    boolean matchType1 = bankaccount && currency && dc_flag && tran_amt_opposite && use_org_same;
                    boolean matchType2 = bankaccount && currency && !dc_flag && tran_amt_same && use_org_same;
                    if(!matchType1 && !matchType2){
                       // CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM002_02YO6.getDesc(),context);
                        DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO6.getCode(),checkBankReconciliation);
                        continue;
                    }
                    // 退票标识规则是否通过标识
                    Boolean refundAutoCheckFlag = true;
                    if (refundCheckRule != null) {
                        //对方银行账号相同
                        if (refundCheckRule.getToaccountflag() == MatchDirectionType.Same.getValue()) {
                            if (!StringUtils.isEmpty(checkBankReconciliation.getTo_acct_no())) {
                                refundAutoCheckFlag = checkBankReconciliation.getTo_acct_no().equals(matchBankReconciliaton.getTo_acct_no());
                            } else {
                                refundAutoCheckFlag = StringUtils.isEmpty(matchBankReconciliaton.getTo_acct_no());
                            }
                            if(!refundAutoCheckFlag){
                                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM002_02YO2.getDesc(),context);
                                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO2.getCode(),checkBankReconciliation);
                            }
                        }
                        //对方户名相同
                        if (refundAutoCheckFlag&&refundCheckRule.getToaccountnameflag() == MatchDirectionType.Same.getValue()) {
                            if (!StringUtils.isEmpty(checkBankReconciliation.getTo_acct_name())) {
                                refundAutoCheckFlag = checkBankReconciliation.getTo_acct_name().equals(matchBankReconciliaton.getTo_acct_name());
                            } else {
                                refundAutoCheckFlag = StringUtils.isEmpty(matchBankReconciliaton.getTo_acct_name());
                            }
                            if(!refundAutoCheckFlag){
                                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM002_02YO3.getDesc(),context);
                                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO3.getCode(),checkBankReconciliation);
                            }
                        }
                        //对方单位类型相同
                        if (refundAutoCheckFlag && refundCheckRule.getOppositetypeflag() == MatchDirectionType.Same.getValue()) {
                            if (!StringUtils.isEmpty(checkBankReconciliation.getOppositeobjectid())) {
                                refundAutoCheckFlag = checkBankReconciliation.getOppositeobjectid().equals(matchBankReconciliaton.getOppositeobjectid());
                            } else {
                                refundAutoCheckFlag = StringUtils.isEmpty(matchBankReconciliaton.getOppositeobjectid());
                            }
                            if(!refundAutoCheckFlag){
                                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM002_02YO4.getDesc(),context);
                                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO4.getCode(),checkBankReconciliation);
                            }
                        }
                        //时间相同
                        if (refundAutoCheckFlag){
                            Integer dataRange = refundCheckRule.getDaterange();
                            if (dataRange == null) {
                                dataRange = 1;
                            }
                            Date date = DateUtils.formatBalanceDate(DateUtils.dateAddDays(DateUtils.getNow(), -1 * dataRange));
                            refundAutoCheckFlag = checkBankReconciliation.getTran_date() != null && checkBankReconciliation.getTran_date().after(date);
                        }
                        //摘要相同
                        if (refundAutoCheckFlag && refundCheckRule.getRemarkmatch() == MatchDirectionType.Same.getValue()) {
                            if (!StringUtils.isEmpty(checkBankReconciliation.getRemark())) {
                                refundAutoCheckFlag = checkBankReconciliation.getRemark().equals(matchBankReconciliaton.getRemark());
                            } else {
                                refundAutoCheckFlag = StringUtils.isEmpty(matchBankReconciliaton.getRemark());
                            }
                            if(!refundAutoCheckFlag){
                                CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),DealDetailBusinessCodeEnum.SYSTEM002_02YO5.getDesc(),context);
                                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02YO5.getCode(),checkBankReconciliation);
                            }
                        }
                    }
                    /*
                     * 两种匹配方式
                     * 1，银行账号相同，币种相同，借贷方向相同，金额互为相反数,且匹配退票辨识规则
                     * 2，银行账号相同，币种相同，借贷方向不同，金额相同,且匹配退票标识规则
                     * 满足其一即可
                     */
                    if ((matchType1 && refundAutoCheckFlag) || (matchType2 && refundAutoCheckFlag)) {
                        //如果匹配到已经被拒绝的，继续往后匹配;否则正常更新
                        if(!isReject(checkBankReconciliation, matchBankReconciliaton)){
                            //新建对象
                            //匹配到的数据，互存对方id
                            checkBankReconciliation.setRefundrelationid(matchBankReconciliaton.getId().toString());
                            matchBankReconciliaton.setRefundrelationid(checkBankReconciliation.getId().toString());
                            //设置退票状态为疑似退票
                            checkBankReconciliation.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
                            // 匹配疑似退票时，新拉取流水和原流水都需要更新退票状态
                            checkBankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus());
                            matchBankReconciliaton.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
                            matchBankReconciliaton.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_PENDING.getStatus());
                            CmpCheckAndProcessRuleLogProcessor.executeNoRuleLog(checkBankReconciliation,RuleLogEnum.RuleLogProcess.BANK_REFUND_NAME.getDesc(),RuleLogEnum.RuleLogProcess.BANK_REFUND_STEP_TWO.getDesc(),context);
                            //存入修改数据集合
                            updateList.add(matchBankReconciliaton);
                            //流水挂起
                            pendingList.add(checkBankReconciliation);
                            matchedData.add(checkBankReconciliation.getId());
                            matchedData.add(matchBankReconciliaton.getId());
                            checkIterator.remove();
                            matchIterator.remove();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("退票辨识失败，key:" + checkBankReconciliation.getId(),e);
                DealDetailUtils.appendBusiCode(DealDetailBusinessCodeEnum.SYSTEM002_02S02.getCode(),checkBankReconciliation);
            }
        }
    }

    private boolean isReject(BankReconciliation checkBankReconciliation, BankReconciliation matchBankReconciliaton) {
        String checkId = checkBankReconciliation.getId().toString();
        String matchId = matchBankReconciliaton.getId().toString();

        String checkRefundRejectRelationId = checkBankReconciliation.getRefundrejectrelationid();
        String matchRefundRejectRelationId = matchBankReconciliaton.getRefundrejectrelationid();

        //拒绝的id中含有对方id，则总结拒绝过对方
        if (checkRefundRejectRelationId != null  && checkRefundRejectRelationId.contains(matchId)) {
            return true;
        }
        if (matchRefundRejectRelationId != null  && matchRefundRejectRelationId.contains(checkId)) {
            return true;
        }

        return false;
    }
}
