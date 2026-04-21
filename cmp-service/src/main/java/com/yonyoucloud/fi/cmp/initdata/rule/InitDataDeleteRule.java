package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 银行账户期初删除规则
 */
@Component
@Slf4j
public class InitDataDeleteRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size()>0) {
            BizObject bizObject =  bills.get(0);
            Long id = bizObject.getId();
            // 根据id重新查询该条数据
            InitData dbInitData = MetaDaoHelper.findById(InitData.ENTITY_NAME,id);
            if (dbInitData == null) {
                throw new CtmException(new CtmErrorCode("033-502-101268"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006CA", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
            // 增加锁
            String key = "initdatadeletekey:" + dbInitData.getAccentity() + dbInitData.getCurrency() +
                    (dbInitData.getBankaccount() == null ? dbInitData.getCashaccount() : dbInitData.getBankaccount());
            try (YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key)) {
                if (ymsLock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101175"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418065F","该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
                }
            }
            // 处理银行账户期初
            if (handleBankAccount(dbInitData)) {
                log.error("InitDataDeleteRule has more than one data allow delete id {}", id.toString());
                return new RuleExecuteResult();
            }

            // 处理现金账户
            handleCashAccount(dbInitData);

        }
        return new RuleExecuteResult();
    }

    /**
     * 处理银行期初
     * @param dbInitData
     * @return
     * @throws Exception
     */
    private boolean handleBankAccount(InitData dbInitData) throws Exception {
        if (StringUtils.isEmpty(dbInitData.getBankaccount())) {
            return false;
        }
        // 组织、账户、币种查询到多个期初数据允许删除
        if (deleteMoreInitdata(dbInitData)) {
            return true;
        }
        // 查询是否日结(cmp_settlement_detail)
        isSettle(dbInitData);

        // 存在银行日记账时不能做删除(cmp_journal表)
        checkHasJournal(dbInitData);
        return false;
    }

    /**
     * 处理现金账户期初
     * @param dbInitData
     */
    private void handleCashAccount(InitData dbInitData) throws Exception {
        if (StringUtils.isEmpty(dbInitData.getCashaccount())) {
            return;
        }
        QuerySchema querySchemaJournal = QuerySchema.create().addSelect("1");
        QueryConditionGroup groupJournal = QueryConditionGroup.and(QueryCondition.name("cashaccount").eq(dbInitData.getCashaccount()));
        querySchemaJournal.addCondition(groupJournal);
        List<Journal> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME,querySchemaJournal);
        if (CollectionUtils.isNotEmpty(journalList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101589"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180774","该现金账户存在现金日记账，不允许删除！") /* "该现金账户存在现金日记账，不允许删除！" */);
        }
    }

    /**
     * 校验是否存在日记账
     * @param dbInitData
     * @throws Exception
     */
    private void checkHasJournal(InitData dbInitData) throws Exception {
        QuerySchema querySchemaJournal = QuerySchema.create().addSelect("1");
        QueryConditionGroup groupJournal = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(dbInitData.getBankaccount()));
        groupJournal.addCondition(QueryCondition.name("currency").eq(dbInitData.getCurrency()));
        groupJournal.addCondition(QueryCondition.name("accentity").eq(dbInitData.getAccentity()));
        querySchemaJournal.addCondition(groupJournal);
        List<Journal> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME,querySchemaJournal);
        if (CollectionUtils.isNotEmpty(journalList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101587"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180772","该银行账号存在银行日记账，不允许删除！") /* "该银行账号存在银行日记账，不允许删除！" */);
        }
    }

    /**
     * 存在多条期初数据时允许删除
     * @param paramInitData
     * @return
     */
    private boolean deleteMoreInitdata(InitData paramInitData) throws Exception {
        QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("1");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("bankaccount").eq(paramInitData.getBankaccount()));
        conditionGroup.addCondition(QueryCondition.name("currency").eq(paramInitData.getCurrency()));
        conditionGroup.addCondition(QueryCondition.name("accentity").eq(paramInitData.getAccentity()));
        queryInitDataSchema.addCondition(conditionGroup);
        List<InitData> initDataList = MetaDaoHelper.query(InitData.ENTITY_NAME, queryInitDataSchema);
        return CollectionUtils.isNotEmpty(initDataList) && initDataList.size() > 1;
    }

    /**
     * 查询是否日结
     * @param dbInitData
     * @throws Exception
     */
    private void isSettle(InitData dbInitData) throws Exception {
        QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("1");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.addCondition(QueryCondition.name("bankaccount").eq(dbInitData.getBankaccount()));
        conditionGroup.addCondition(QueryCondition.name("currency").eq(dbInitData.getCurrency()));
        conditionGroup.addCondition(QueryCondition.name("accentity").eq(dbInitData.getAccentity()));
        queryInitDataSchema.addCondition(conditionGroup);
        // 根据查询条件查询日结明细
        List<Settlement> settlementList = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, queryInitDataSchema);
        if (CollectionUtils.isNotEmpty(settlementList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101586"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5992804F00003", "该银行账号已存在日结数据，不允许删除！") /* "该银行账号已存在日结数据，不允许删除！" */);
        }
    }

}
