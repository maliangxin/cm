package com.yonyoucloud.fi.cmp.bankreconciliation.rule.noprocess;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * zxl
 * 删除银行账单-更新账户历史余额
 */
@Slf4j
@Component
public class BankreconciliationNoProcessRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        List<BankReconciliation> resultList = new ArrayList<>();
        BankreconciliationUtils.checkDataLegalList(bills, BankreconciliationActionEnum.NOPROCESS);
        QuerySchema querySchema = QuerySchema.create().addSelect("id,pubts,bank_seq_no");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(bills.stream()
                .map(BizObject::getId)
                .collect(Collectors.toList())));
        querySchema.addCondition(group);
        List<BankReconciliation> dbList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        Map<String, BankReconciliation> DbMap = dbList.stream()
                .collect(Collectors.toMap(
                        item -> item.getId().toString(),
                        item -> item
                ));
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<String> idList = dbList.stream()
                .map(b -> b.getId() != null ? b.getId().toString() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> locks = batchLockGetKeys(idList);
        CtmLockTool.executeInOneServiceExclusivelyBatchLock(locks,60*10*2L, TimeUnit.SECONDS,(int lockStatus)->{
            if(lockStatus == LockStatus.GETLOCK_FAIL){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747","该数据正在处理，请稍后重试！") /* "执行失败" */);
            }
            for (BizObject bill : bills) {
                String pubts = sf.format(bill.getPubts());
                BankReconciliation dbData = DbMap.get(bill.getId().toString());
                String dbPubts = sf.format(dbData.getPubts());
                if (!pubts.equals(dbPubts)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00261", "当前单据【%s】不是最新状态，请刷新单据重新操作。") /* "当前单据【%s】不是最新状态，请刷新单据重新操作。" */, bill.get("bank_seq_no").toString()));
                }
                BankReconciliation originBankReconciliation = new BankReconciliation();
                originBankReconciliation.init(bill);
                if ((originBankReconciliation.getSerialdealtype() == null || originBankReconciliation.getSerialdealtype() == 0)
                        && originBankReconciliation.getAssociationstatus() == 0
                        && !originBankReconciliation.getIspublish()
                        && (originBankReconciliation.getIsRepeat() == 0 || originBankReconciliation.getIsRepeat() == 3)) {
                    BankReconciliation bankReconciliation = new BankReconciliation();
                    bankReconciliation.setId(bill.getId());
                    bankReconciliation.setSerialdealtype((short) 5);
                    bankReconciliation.setSerialdealendstate((short) 1);
                    bankReconciliation.setEntityStatus(EntityStatus.Update);
                    resultList.add(bankReconciliation);
                } else {
                    String bankSeqNo = originBankReconciliation.getBank_seq_no();
                    //收付单据关联状态≠未关联
                    if (originBankReconciliation.getAssociationstatus() != 0) {
                        //交易流水号【XXX】已与下游业务单据关联，无需处理失败，请检查！
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103005"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E98339E05A80000", "交易流水号【%s】已与下游业务单据关联，无需处理失败，请检查！"), bankSeqNo) /* "交易流水号【%s】已与下游业务单据关联，无需处理失败，请检查！" */);
                    }
                    //发布≠否
                    if (originBankReconciliation.getIspublish()) {
                        //交易流水号【YYY】已发布，标记无需处理失败，请检查
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103006"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E9833BC05A80009", "交易流水号【%s】已发布，标记无需处理失败，请检查！"), bankSeqNo) /* "交易流水号【%s】已发布，标记无需处理失败，请检查！" */);
                    }
                    //疑重标识≠‘正确’、‘确认正常’
                    if (originBankReconciliation.getIsRepeat() != 0 && originBankReconciliation.getIsRepeat() != 3) {
                        //交易流水号【XXX】为疑似重复\重复流水，标记无需处理失败，请检查!
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103007"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E9833E205A80001", "交易流水号【%s】为疑似重复\\重复流水，标记无需处理失败，请检查！"), bankSeqNo) /* "交易流水号【%s】为疑似重复\重复流水，标记无需处理失败，请检查！" */);
                    }
                }
            }
            CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(resultList);
            MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, resultList);
        });
        return new RuleExecuteResult();
    }

    public static List<String> batchLockGetKeys(List<String>  accountNos){
        // 查询所有的账户信息+行为
        List<String> accountIdList=new ArrayList<>();
        if(!CollectionUtils.isEmpty(accountNos)){
            for(String accountNo:accountNos){
                String key = ICmpConstant.CMPBANKRECONCILIATIONLIST+accountNo ;
                if(accountIdList.contains(key)){
                    continue;
                }
                accountIdList.add(key);
            }
        }
        return accountIdList;
    }
}