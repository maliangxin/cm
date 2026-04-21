package com.yonyoucloud.fi.cmp.bankreconciliation.service.refund;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.autorefundcheckrule.AutoRefundCheckRule;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.business.BankRefundMatchHandler;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银行对账单 - 退票检测任务
 * @author msc
 */
@Service
@Slf4j
public class BankReconciliationReFundCheckTaskService {

    //退票辨识规则service
    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;
    @Resource
    private BankRefundMatchHandler bankRefundMatchHandler;

    private static final String MATCH = "match";//匹配数据标识
    private static final String CHECK = "check";//检测数据标识

    // 退票标识查询条数
    private static final int reFundSelectCount = Integer.parseInt(AppContext.getEnvConfig("cmp.reconciliationReFund.selectCount","3000"));

    /**
     * 退票检测
     * 1，查出待检测数据集合
     * 2，取出待匹配数据集合
     * 3，将检测数据与待匹配数据进行匹配 - 如匹配到 将数据存入待修改数据集合
     * 4，将修改数据集合进行修改
     * @return result
     */
    public Map<String,Object> bankReconciliationCheckReFund(Map<String,Object> paramMap) throws Exception {
        Map<String,Object> result = new HashMap<>();
        paramMap.put("logId",paramMap.get("logId") == null ? "" : paramMap.get("logId"));
        result.put("asynchronized",true);
        //退票辨识规则
        AutoRefundCheckRule refundCheckRule = refundAutoCheckRuleService.queryRuleInfo(null);

        List<BankReconciliation>  selectList = getBankReconciliations(CHECK,refundCheckRule);//检测数据集合
        //List<BankReconciliation>  matchList = getBankReconciliations(MATCH,refundCheckRule);//匹配数据集合
        ////如果待监测数据或待匹配数据为空，则不必执行此任务
        //if(checkList.isEmpty()||matchList.isEmpty()) {
        //    TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022D", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        //    return result;
        //}
        ////监测数据匹配
        //List<BankReconciliation> updateList = matchReFundData(checkList,matchList);
        ////修改数据
        //CommonSaveUtils.updateBankReconciliation(updateList);
        // 退票检测数据库的查询为了减少数据量，按照单账号进行分组处理
        Map<String, List<BankReconciliation>> accountGroup =
                selectList.stream()
                        .collect(Collectors.groupingBy(BankReconciliation::getBankaccount));
        List<BankReconciliation> checkList = new ArrayList<>();
        accountGroup.forEach((account, list) -> {
            // 退票识别
            bankRefundMatchHandler.bankRefundMatchHandler(list);
            checkList.addAll(list);
        });
        checkList.forEach(b->{
            b.setEntityStatus(EntityStatus.Update);
        });
        CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(checkList);
        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME,checkList);
        //返回执行成功
        TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022D", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        return result;
    }

//    private void removeReject(List<BankReconciliation> list) {
//        Set<String> removeIDSet = new HashSet<>();
//        for(BankReconciliation bankReconciliation : list){
//            Short refundStatus = bankReconciliation.getRefundstatus();
//            //已被标记为疑似退票的流水，需要去掉拒绝退票过的
//            if(refundStatus != null && (refundStatus == ReFundType.SUSPECTEDREFUND.getValue())){
//                String refundrejectrelationid = Objects.toString(bankReconciliation.getRefundrejectrelationid(), null);
//                String refundrelationid = Objects.toString(bankReconciliation.getRefundrelationid(), null);
//                if(refundrelationid != null && refundrejectrelationid != null && refundrejectrelationid.contains(refundrelationid)) {
//                    removeIDSet.add(bankReconciliation.getId().toString());
//                    //相应的被退票辨识出来的，也应该去掉
//                    removeIDSet.add(refundrelationid);
//                }
//            }
//        }
//        Iterator<BankReconciliation> iterator = list.iterator();
//        while (iterator.hasNext()){
//            BankReconciliation b = iterator.next();
//            if (removeIDSet.contains(b.getId().toString())) {
//                iterator.remove();
//            }
//        }
//    }


    /**
     * 根据数据类型区分
     * 获取监测数据集合，匹配数据集合
     * 匹配对方账号不为空，退票状态为空，交易日期是今天及前一天的数据
     * @param listType 类型 匹配/监测
     * @return 匹配/监测 集合
     * @throws Exception 查询抛出异常
     */
    List<BankReconciliation> getBankReconciliations(String listType,AutoRefundCheckRule refundCheckRule) throws Exception {
        List<BankReconciliation> list;
        Integer dataRange = refundCheckRule!=null ? refundCheckRule.getDaterange() : null;
        if(dataRange == null){
            dataRange = 1;
        }

        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        // 下面查询不能替换成*，因为特征字段在copy时报错
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        //默认为查询监测数据列表
        //todo 都没用，一组条件就够
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("refundstatus").is_null(),
                QueryCondition.name("tran_date").between(sf.format(DateUtils.dateAddDays(DateUtils.getNow(),-dataRange)),sf.format(DateUtils.getNow())),
                ////退票检测，不检测内部单位
                //QueryCondition.name("oppositetype").not_eq(OppositeType.InnerOrg.getValue()),
                QueryCondition.name("accentity").is_not_null()
        );
        //QueryConditionGroup group2 = QueryConditionGroup.and(
        //        QueryCondition.name("refundstatus").is_null(),
        //        QueryCondition.name("tran_date").between(sf.format(DateUtils.dateAddDays(DateUtils.getNow(),-dataRange)),sf.format(DateUtils.getNow())),
        //        //退票检测，不检测内部单位
        //        QueryCondition.name("oppositetype").is_null(),
        //        QueryCondition.name("accentity").is_not_null()
        //);
        ////如果listType 为 match  则查询匹配数据列表
        //if(listType.equals(MATCH)){
        //    group1 = QueryConditionGroup.and(
        //            QueryCondition.name("refundstatus").is_null(),
        //            QueryCondition.name("tran_date").between(sf.format(DateUtils.dateAddDays(DateUtils.getNow(),-dataRange)),sf.format(DateUtils.getNow())),
        //            ////退票检测，不检测内部单
        //            //QueryCondition.name("oppositetype").not_eq(OppositeType.InnerOrg.getValue()),
        //            QueryCondition.name("accentity").is_not_null()
        //    );
        //    //group2 = QueryConditionGroup.and(
        //    //        QueryCondition.name("refundstatus").is_null(),
        //    //        QueryCondition.name("tran_date").between(sf.format(DateUtils.dateAddDays(DateUtils.getNow(),-dataRange)),sf.format(DateUtils.getNow())),
        //    //        //退票检测，不检测内部单
        //    //        QueryCondition.name("oppositetype").is_null(),
        //    //        QueryCondition.name("accentity").is_not_null()
        //    //);
        //}
        //QueryConditionGroup group = QueryConditionGroup.or(group1,group2);
        querySchema.addCondition(group1);
        // 按照交易时间进行排序，取最新的3000笔
        querySchema.addOrderBy(new QueryOrderby("tran_date", "desc"));
        querySchema.addPager(0, reFundSelectCount);
        list =  MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
        return list;
    }

    /**
     * 监测数据匹配
     * @param checkList 监测数据
     * @param matchList 匹配数据
     * @return 需要更新的匹配的数据
     */
//    List<BankReconciliation> matchReFundData(List<BankReconciliation> checkList,List<BankReconciliation> matchList)throws Exception{
//        List<BankReconciliation> updateList = new ArrayList<>();
//        Set<String> matchedData = new HashSet<>();
//        //退票辨识规则
//        AutoRefundCheckRule refundCheckRule = refundAutoCheckRuleService.queryRuleInfo(null);
//        /*
//         * 匹配逻辑 ----
//         * 循环监测对账单 -- 匹配待确认数据
//         * 根据已匹配对账单map  判断是否已匹配过
//         * 如未匹配过 进行匹配操作
//         * 如匹配到 将双方对账单数据 存入updateList  修改退票状态
//         * 同时将双方id 记录到已匹配对账单map内
//         * O(n2)
//         */
//        for (BankReconciliation checkBankReconciliation : checkList) {
//            if (matchedData.contains(checkBankReconciliation.getId().toString())) {
//                continue;
//            }
//            // 循环匹配数据
//            for (BankReconciliation matchBankReconciliation : matchList) {
//                // 匹配的数据与监测的数据相同则跳过匹配
//                if (checkBankReconciliation.getId().equals(matchBankReconciliation.getId())) {
//                    continue;
//                }
//                // 当前匹配数据已经匹配过该数据不重复匹配，直接匹配下一条数据
//                if (matchedData.contains(matchBankReconciliation.getId().toString())) {
//                    continue;
//                }
//                // 匹配数据条件
//                boolean bankaccount = checkBankReconciliation.getBankaccount().equals(matchBankReconciliation.getBankaccount());//银行账户相同
//                boolean currency = checkBankReconciliation.getCurrency().equals(matchBankReconciliation.getCurrency());//币种相同
//                boolean dc_flag = checkBankReconciliation.getDc_flag().equals(matchBankReconciliation.getDc_flag());//借贷方向相同
//                boolean tran_amt_same =  checkBankReconciliation.getTran_amt().equals(matchBankReconciliation.getTran_amt());//金额相同
//                boolean tran_amt_opposite = checkBankReconciliation.getTran_amt().equals(matchBankReconciliation.getTran_amt().negate());//金额互为相反数
//                // 账户共享，退票要保证授权使用组织相同
//                boolean use_org_same = checkBankReconciliation.getAccentity().equals(matchBankReconciliation.getAccentity());
//                // 退票标识规则是否通过标识
//                boolean refundAutoCheckFlag = true;
//                if (refundCheckRule != null){
//                    // 对方银行账号相同
//                    if (refundCheckRule.getToaccountflag() == MatchDirectionType.Same.getValue()){
//                        if (!StringUtils.isEmpty(checkBankReconciliation.getTo_acct_no())){
//                            refundAutoCheckFlag = checkBankReconciliation.getTo_acct_no().equals(matchBankReconciliation.getTo_acct_no());
//                        }else {
//                            refundAutoCheckFlag = StringUtils.isEmpty(matchBankReconciliation.getTo_acct_no());
//                        }
//                    }
//                    //对方户名相同
//                    if (refundCheckRule.getToaccountnameflag() == MatchDirectionType.Same.getValue()){
//                        if (!StringUtils.isEmpty(checkBankReconciliation.getTo_acct_name())){
//                            refundAutoCheckFlag = refundAutoCheckFlag && checkBankReconciliation.getTo_acct_name().equals(matchBankReconciliation.getTo_acct_name());
//                        }else {
//                            refundAutoCheckFlag = refundAutoCheckFlag && StringUtils.isEmpty(matchBankReconciliation.getTo_acct_name());
//                        }
//                    }
//                    //对方单位类型相同
//                    if (refundCheckRule.getOppositetypeflag() == MatchDirectionType.Same.getValue()){
//                        if (!StringUtils.isEmpty(checkBankReconciliation.getOppositeobjectid())){
//                            refundAutoCheckFlag = refundAutoCheckFlag && checkBankReconciliation.getOppositeobjectid().equals(matchBankReconciliation.getOppositeobjectid());
//                        }else {
//                            refundAutoCheckFlag = refundAutoCheckFlag && StringUtils.isEmpty(matchBankReconciliation.getOppositeobjectid());
//                        }
//                    }
//                    //摘要相同
//                    if (refundCheckRule.getRemarkmatch() == MatchDirectionType.Same.getValue()){
//                        if (!StringUtils.isEmpty(checkBankReconciliation.getRemark())){
//                            refundAutoCheckFlag = refundAutoCheckFlag && checkBankReconciliation.getRemark().equals(matchBankReconciliation.getRemark());
//                        }else {
//                            refundAutoCheckFlag = refundAutoCheckFlag && StringUtils.isEmpty(matchBankReconciliation.getRemark());
//                        }
//                    }
//                }
//                /*
//                 * 两种匹配方式
//                 * 1，银行账号相同，币种相同，借贷方向相同，金额互为相反数,且匹配退票辨识规则
//                 * 2，银行账号相同，币种相同，借贷方向不同，金额相同,且匹配退票标识规则
//                 * 满足其一即可
//                 */
//                if((use_org_same && bankaccount&&currency && dc_flag && tran_amt_opposite && refundAutoCheckFlag)||( use_org_same && bankaccount && currency && !dc_flag && tran_amt_same && refundAutoCheckFlag)){
//                    //匹配到的数据，互存对方id
//                    checkBankReconciliation.setRefundrelationid(matchBankReconciliation.getId().toString());
//                    matchBankReconciliation.setRefundrelationid(checkBankReconciliation.getId().toString());
//                    //设置退票状态为疑似退票
//                    checkBankReconciliation.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
//                    matchBankReconciliation.setRefundstatus(ReFundType.SUSPECTEDREFUND.getValue());
//                    //存入已匹配map, true - 已校验过
//                    matchedData.add(matchBankReconciliation.getId().toString());
//                    matchedData.add(checkBankReconciliation.getId().toString());
//                    //修改状态
//                    checkBankReconciliation.setEntityStatus(EntityStatus.Update);
//                    matchBankReconciliation.setEntityStatus(EntityStatus.Update);
//
//                    //智能到账，退票自动辨识设置为true
//                    checkBankReconciliation.setRefundauto(true);
//                    matchBankReconciliation.setRefundauto(true);
//
//                    // 存入修改数据集合
//                    updateList.add(checkBankReconciliation);
//                    updateList.add(matchBankReconciliation);
//                    // 匹配上则跳出循环 监测数据再取一个进行匹配
//                    break;
//                }
//            }
//        }
//        return updateList;
//    }
}
