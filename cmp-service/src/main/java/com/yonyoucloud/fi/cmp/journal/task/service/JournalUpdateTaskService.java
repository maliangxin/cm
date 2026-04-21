package com.yonyoucloud.fi.cmp.journal.task.service;

import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.settlebench.SettleBench_b;
import com.yonyoucloud.fi.basecom.util.OidUtils;
import com.yonyoucloud.fi.cmp.bankreceipt.dto.TenantDTO;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.initdata.InitDatab;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author hanll
 * @Date 2025/5/28-16:11
 */
@Service
@Slf4j
public class JournalUpdateTaskService {

    private static final String QUERY_ALL_TENANT = "com.yonyoucloud.fi.cmp.mapper.TenantMapper.queryAllTenant";

    private static final int PAGE_SIZE = 1000;

    /**
     * 升级日记账数据
     * @throws Exception
     */
    public void updateJournal() throws Exception {
        log.error("开始执行银行日记账来源业务系统、业务单据类型数据升级");
        // 查询所有的租户信息
        List<TenantDTO>    tenantDTOList = SqlHelper.selectList(QUERY_ALL_TENANT);
        // 通过机器人异步执行
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        int tenantCount = tenantDTOList.size();
        int tenantIndex = 0;
        for (TenantDTO dto : tenantDTOList) {
            try {
                RobotExecutors.runAs(dto.getYtenantId(), new Callable() {
                    @Override
                    public Object call() throws Exception {
                        // 查询某个租户是否已经升级过
                        if (queryHaveUpgraded(dto.getYtenantId())) {
                            log.error("此租户:{}已经升级过，跳过",  dto.getYtenantId());
                            return null;
                        }
                        if (updateTopSrcSystemAndBillType(dto.getYtenantId()) == null) {
                            return null;
                        }

                        // 写入租户升级完成表示
                        insertHaveUpgradedFlag(dto.getYtenantId());
                        return null;
                    }
                }, ctmThreadPoolExecutor.getThreadPoolExecutor());
            } catch (Exception e) {
                log.error("JournalUpdateTaskService.updateJournal error", e);
            }
            tenantIndex++;
            log.error("银行日记账升级当前进度：{}%", tenantIndex * 100 / tenantCount);

        }
    }

    /**
     * 升级某个租户的银行日记账数据
     * @param ytenantId 租户id
     * @throws Exception
     */
    public void updateJournal(String ytenantId) throws Exception {
        log.error("开始执行此租户{}银行日记账来源业务系统、业务单据类型数据升级",ytenantId);
        updateTopSrcSystemAndBillType(ytenantId);
    }

    /**
     * 更新来源业务系统
     * @param ytenantId 租户id
     * @return
     * @throws Exception
     */
    private Object updateTopSrcSystemAndBillType(String ytenantId) throws Exception {
        // 查询出待处理银行日记账的总条数
        long count = queryNeedUpdateJournalCount();
        if (count == 0) {
            log.error("此租户:{}没有需要处理的银行日记账",  ytenantId);
            return null;
        }
        log.error("此租户:{}需要处理的银行日记账总条数:{}",  ytenantId, count);
        int pageTotalCount = (int) (count / PAGE_SIZE) + 1;
        int handledCount = 0;
        for (int pageindex = 0; pageindex < pageTotalCount; pageindex++) {
            try {
                List<Journal> needUpdateJournalList = queryNeedUpdateJournalList(pageindex);
                List<String> srcbillitemIds = needUpdateJournalList.stream().map(Journal::getSrcbillitemid).filter(Objects::nonNull).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(srcbillitemIds)) {
                    log.error("updateTopSrcSystemAndBillType srcbillitemIds is null,pageindex:{}",pageindex);
                    continue;
                }
                // 查询结算明细数据
                List<SettleBench_b> settleBenchBList = querySettleBenchBodyByIdList(srcbillitemIds);
                if (CollectionUtils.isEmpty(settleBenchBList)) {
                    log.error("此租户:{}根据结算明细id:{}查询到资金结算明细数据为空跳过", ytenantId, CtmJSONObject.toJSONString(srcbillitemIds));
                    continue;
                }
                Map<Long, SettleBench_b> settleBenchBMap = settleBenchBList.stream().collect(Collectors.toMap(SettleBench_b::getId, v -> v));
                SettleBench_b settleBenchB;
                for (Journal journal : needUpdateJournalList) {
                    handledCount++;
                    settleBenchB = settleBenchBMap.get(Long.parseLong(journal.getSrcbillitemid()));
                    if (settleBenchB == null) {
                        log.error("此租户:{}银行日记账结算明细id:{}对应的结算明细无数据", ytenantId, journal.getSrcbillitemid());
                        continue;
                    }
                    // 来源业务系统
                    journal.setTopsrcitem(StringUtils.isEmpty(settleBenchB.getBizsyssrc()) ? null : Short.parseShort(settleBenchB.getBizsyssrc()));
                    // 业务单据类型
                    journal.setTopbilltype(StringUtils.isEmpty(settleBenchB.getBizbilltype()) ? null : Short.parseShort(settleBenchB.getBizbilltype()));
                    journal.setEntityStatus(EntityStatus.Update);
                }
                // 更新银行日记账来源业务系统、业务单据类型
                MetaDaoHelper.update(Journal.ENTITY_NAME, needUpdateJournalList);
                log.error("此租户:{}已经升级了{}条银行日记账数据,还剩余{}条，完成百分比{}%", ytenantId,
                        handledCount, count - handledCount, handledCount * 100L / count);
            }  catch (Exception e) {
                log.error("JournalUpdateTaskService.updateTopSrcSystemAndBillType error", e);
            }
        }
        return new Object();
    }

    /**
     * 写入升级完成表示
     * @param ytenantId
     * @throws Exception
     */
    private void insertHaveUpgradedFlag(String ytenantId) throws Exception {
        InitDatab initDatab = new InitDatab();
        initDatab.setAccentity(ytenantId);
        initDatab.setDescription(ytenantId);
        initDatab.setId(OidUtils.getId());
        initDatab.setMainid(initDatab.getId());
        initDatab.setEntityStatus(EntityStatus.Insert);
        MetaDaoHelper.insert(InitDatab.ENTITY_NAME, initDatab);
    }

    /**
     * 查询数据是否已经升级过
     * @param ytenantId 租户id
     * @return
     * @throws Exception
     */
    private boolean queryHaveUpgraded(String ytenantId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("count(1) as count");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(ytenantId));
        conditionGroup.appendCondition(QueryCondition.name("description").eq(ytenantId));
        schema.addCondition(conditionGroup);
        Map<String, Object> countMap = MetaDaoHelper.queryOne(InitDatab.ENTITY_NAME, schema);
        if (countMap == null || Long.parseLong(countMap.get("count").toString()) == 0L) {
            return false;
        }
        return true;

    }


    /**
     * 根据结算明细id查询结算明细数据
     * @param id 结算明细id集合
     * @return
     * @throws Exception
     */
    public List<SettleBench_b> querySettleBenchBodyByIdList(List<String> id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("stwb.settlebench.SettleBench_b");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STWB);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("id").in(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(query)){
            return null;
        }else{
            List<SettleBench_b> settleBenchbList = new ArrayList<>();
            for(Map<String, Object> obj : query){
                SettleBench_b b = new SettleBench_b();
                b.init(obj);
                settleBenchbList.add(b);
            }
            return settleBenchbList;
        }
    }

    /**
     * 查询需要更新的银行日记账列表
     * @param pageindex
     * @return
     * @throws Exception
     */
    private List<Journal> queryNeedUpdateJournalList(int pageindex) throws Exception {
        QueryConditionGroup conditionGroup = buildQueryJournalConditionGroup();
        QuerySchema schema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        schema.addCondition(conditionGroup);
        schema.addPager(pageindex, PAGE_SIZE);
        schema.addOrderBy(new QueryOrderby("id", "desc"));
        return MetaDaoHelper.queryObject(Journal.ENTITY_NAME, schema,null);
    }

    /**
     * 查询需要更新的银行日记账数量
     * @return
     * @throws Exception
     */
    private long queryNeedUpdateJournalCount() throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("count(1) as count");
        schema.addCondition(buildQueryJournalConditionGroup());
        Map<String, Object> countMap = MetaDaoHelper.queryOne(Journal.ENTITY_NAME, schema);
        if (countMap == null) {
            return 0L;
        }
        return Long.parseLong(countMap.get("count").toString());
    }

    /**
     * 构建查询银行日记账条件组
     * @return
     */
    private QueryConditionGroup buildQueryJournalConditionGroup() {
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("initflag").eq(0));
        // 资金结算
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.StwbSettlement.getValue()));
        // 资金结算明细
        conditionGroup.appendCondition(QueryCondition.name("billtype").eq(EventType.StwbSettleMentDetails.getValue()));
        // 资金结算
        conditionGroup.appendCondition(QueryCondition.name("topsrcitem").eq(EventSource.StwbSettlement.getValue()));
        // 资金结算明细
        conditionGroup.appendCondition(QueryCondition.name("topbilltype").eq(EventType.StwbSettleMentDetails.getValue()));
        return conditionGroup;
    }
}
