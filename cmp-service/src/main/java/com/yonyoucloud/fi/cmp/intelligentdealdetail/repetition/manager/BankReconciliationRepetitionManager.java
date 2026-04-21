package com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.manager;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.RepetitionResultVo;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition.service.IRepetitionService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 流水业务验重逻辑添加
 *
 * @author maliangn
 * @since 2024-06-22
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BankReconciliationRepetitionManager {
    // 生成oid
    private final YmsOidGenerator ymsOidGenerator;
    @Resource
    private IBankDealDetailAccessDao bankDealDetailAccessDao;
    @Autowired
    IRepetitionService repetitionService;
    public RepetitionResultVo check(List<BankReconciliation> bankReconciliationList){
        RepetitionResultVo result = new RepetitionResultVo();
        try {
            result=handleBankReconciliation(bankReconciliationList);
        }catch (Exception e){
            log.error("银行流水重复校验失败",e);
            return result;
        }
        return result;
    }
    /**
     * @param bankReconciliationList
     * @return
     * @throws Exception
     */
    private RepetitionResultVo handleBankReconciliation(List<BankReconciliation> bankReconciliationList) {
        RepetitionResultVo result = new RepetitionResultVo();
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            return result;
        }
        List<BankReconciliation> haveUniqueNoList = new ArrayList<>();
        List<BankReconciliation> haveNoUniqueNoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankReconciliationList)) {
            bankReconciliationList.stream().forEach(e -> {
                if (StringUtils.isEmpty(e.getConcat_info())) {
                    e.setConcat_info(repetitionService.formatConctaInfoBankReconciliation(e));
                }
                if (StringUtils.isNotEmpty(e.getUnique_no())) {
                    haveUniqueNoList.add(e);
                } else {
                    haveNoUniqueNoList.add(e);
                }
            });
        }
        List<BankReconciliation> updateList = new ArrayList<>();
        List<BankReconciliation> insertList = new ArrayList<>();
        List<BankReconciliation> repetitionList = new ArrayList<>();
        List<BankReconciliation> successList = new ArrayList<>();
        List<BankReconciliation> allList = new ArrayList<>();
        try {
            // 根据uniqueno查询重复的数据
            if (CollectionUtils.isNotEmpty(haveUniqueNoList)) {
                allList.addAll(haveUniqueNoList);
                checkRepetitionByUnique_no(haveUniqueNoList, insertList, updateList, repetitionList);
            }
            // 根据8要素查询出重复的数据
            if (CollectionUtils.isNotEmpty(haveNoUniqueNoList)) {
                allList.addAll(haveNoUniqueNoList);
                checkRepetitionByConcat_info(haveNoUniqueNoList, insertList, updateList, repetitionList);
            }
        } catch (Exception e) {
            log.error("BankReconciliationRepetitionManager.handleHaveUniqueNoBankDealDetail error", e);
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400730", "业务去重异常") /* "业务去重异常" */,e);
        } finally {
            initInsertList(insertList);
            if (!insertList.isEmpty()) {
                result.setInsertList(insertList);
                successList.addAll(insertList);
            }
            if (!updateList.isEmpty()) {
                result.setUpdateList(updateList);
                successList.addAll(updateList);
            }
            if (!repetitionList.isEmpty()) {
                result.setRepetitionList(repetitionList);
                successList.addAll(repetitionList);
            }
            if (!successList.isEmpty()) {
                result.setSuccessList(successList);
            }
            allList.removeAll(successList);
            if (!allList.isEmpty()) {
                result.setRollbackList(allList);
            }
        }
        return result;
    }
    /**
     * 根据uniqeno查询重复的数据
     *
     * @param haveUniqueNoList
     * @param insertList
     * @param updateList
     * @param repetitionList
     * @throws Exception
     */
    private void checkRepetitionByUnique_no(List<BankReconciliation> haveUniqueNoList, List<BankReconciliation> insertList, List<BankReconciliation> updateList, List<BankReconciliation> repetitionList) throws Exception {
        Set<String> uniqueNos = new HashSet<>();
        haveUniqueNoList.stream().forEach(e -> {
            uniqueNos.add(e.getUnique_no());
        });
        List<BankReconciliation> existDatas = queryExistBankReconciliations("unique_no", uniqueNos);
        if (CollectionUtils.isEmpty(existDatas)) {
            insertList.addAll(haveUniqueNoList);
            return;
        }
        Map<String, BankReconciliation> haveUnique_noListMap = new HashMap<>();
        haveUnique_noListMap.putAll(existDatas.stream().collect(Collectors.toMap(BankReconciliation::getUnique_no, e -> e,(key1,key2)->key1)));

        /*
            重复的数据判断对方信息及余额是否相同，如果都不相同则需要进行跳过，否则需要进行更新
         */
        for (BankReconciliation bankReconciliation : haveUniqueNoList) {
            BankReconciliation existBankDealDetail = haveUnique_noListMap.get(bankReconciliation.getUnique_no());
            if(existBankDealDetail!=null){
                if (!StringUtils.equals(bankReconciliation.getTo_acct_name(), existBankDealDetail.getTo_acct_name()) ||
                        !StringUtils.equals(bankReconciliation.getTo_acct_name(), existBankDealDetail.getTo_acct_name()) ||
                        BigDecimalUtils.isEqual(bankReconciliation.getAcct_bal(), existBankDealDetail.getAcct_bal())) {
                    bankReconciliation.setId(existBankDealDetail.getId());
                    updateList.add(bankReconciliation);
                } else {
                    bankReconciliation.setId(existBankDealDetail.getId());
                    repetitionList.add(bankReconciliation);
                }
            }
        }
        haveUniqueNoList.removeAll(updateList);
        haveUniqueNoList.removeAll(repetitionList);
        insertList.addAll(haveUniqueNoList);
    }
    /**
     * 根据8要素concat_info查询重复的数据
     *
     * @param haveNoUniqueNoList
     * @param insertList
     * @param updateList
     * @param repetitionList
     * @throws Exception
     */
    private void checkRepetitionByConcat_info(List<BankReconciliation> haveNoUniqueNoList, List<BankReconciliation> insertList, List<BankReconciliation> updateList, List<BankReconciliation> repetitionList) throws Exception {
        Set<String> concatInfos = new HashSet<>();
        haveNoUniqueNoList.stream().forEach(e -> {
            concatInfos.add(e.getConcat_info());
        });
        List<BankReconciliation> existDatas = queryExistBankReconciliations("concat_info", concatInfos);
        if (CollectionUtils.isEmpty(existDatas)) {
            insertList.addAll(haveNoUniqueNoList);
            return;
        }
        /*
            重复的数据判断余额是否相同，如果不相同则需要进行更新
         */
        Map<String, BankReconciliation> haveConcat_infoListMap = new HashMap<>();
        haveConcat_infoListMap.putAll(existDatas.stream().collect(Collectors.toMap(BankReconciliation::getConcat_info, e -> e,(key1,key2)->key1)));

        for (BankReconciliation bankReconciliation : haveNoUniqueNoList) {
            BankReconciliation existBankDealDetail = haveConcat_infoListMap.get(bankReconciliation.getConcat_info());
            if(existBankDealDetail != null){
                if(BigDecimalUtils.isEqual(bankReconciliation.getAcct_bal(), existBankDealDetail.getAcct_bal())){
                    bankReconciliation.setId(existBankDealDetail.getId());
                    repetitionList.add(bankReconciliation);
                } else if (!BigDecimalUtils.isEqual(bankReconciliation.getAcct_bal(), existBankDealDetail.getAcct_bal()) && existBankDealDetail.getAcct_bal() ==null){
                    bankReconciliation.setId(existBankDealDetail.getId());
                    updateList.add(bankReconciliation);
                }else{
                    insertList.add(bankReconciliation);
                }
            }
        }
        haveNoUniqueNoList.removeAll(updateList);
        haveNoUniqueNoList.removeAll(repetitionList);
        insertList.addAll(haveNoUniqueNoList);
    }

    /**
     * 查询重复的银行对账单信息
     *
     * @param paramName
     * @param conditionList
     * @return
     * @throws Exception
     */
    private List<BankReconciliation> queryExistBankReconciliations(String paramName, Set<String> conditionList) throws Exception {
//        QuerySchema schema = QuerySchema.create();
//        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        schema.addCondition(conditionGroup);
//        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(paramName).in(conditionList)));
//        schema.distinct();
//        schema.addSelect(" id,unique_no,bank_seq_no,tran_date,tran_time,tran_amt,dc_flag,bankaccount,acct_bal,concat_info,to_acct_no,to_acct_name ");
//        // 数据库中存在的数据
//        List<BankReconciliation> existDatas = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
//        return existDatas;

        return bankDealDetailAccessDao.queryExistBankReconciliations(paramName, conditionList);
    }

    /**
     * 初始化新增的数据集合
     *
     * @param insertList
     */
    void initInsertList(List<BankReconciliation> insertList) {
        // 新增的数据集合，提前把id赋值上
        insertList.stream().forEach(e -> e.setId(ymsOidGenerator.nextId()));
    }
}
