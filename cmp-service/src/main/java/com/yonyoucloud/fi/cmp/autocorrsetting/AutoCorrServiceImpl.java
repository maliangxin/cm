package com.yonyoucloud.fi.cmp.autocorrsetting;


import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 自动关联操作 - 定时任务
 * @author msc
 */

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class AutoCorrServiceImpl {


    @Autowired
    CorrOperationService corrOperationService;//写入关联关系

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    /**
     * 自动关联 - 定时任务
     * @param corrDataEntities
     * @return
     */
    public void autoCorrBill(List<CorrDataEntity> corrDataEntities) throws Exception {
        //如没有需要关联处理的单据直接返回
        if (CollectionUtils.isEmpty(corrDataEntities)) {
            return;
        }
        //去除重复关联到同一个业务单据上的对账单数据
        HashMap<String, List<CorrDataEntity>> corrDataEntitiesMap = new HashMap<>();
        for (CorrDataEntity corrDataEntity : corrDataEntities) {
            String key = corrDataEntity.getBillType() + corrDataEntity.getBusid();
            List<CorrDataEntity> groupData = null;
            if (corrDataEntitiesMap.get(key) == null) {
                groupData = new ArrayList();
            } else {
                groupData = corrDataEntitiesMap.get(key);
            }
            groupData.add(corrDataEntity);
            corrDataEntitiesMap.put(key, groupData);
        }
        List<CorrDataEntity> newCorrDataEntities = new ArrayList<>();
        Iterator<String> iterator = corrDataEntitiesMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            List<CorrDataEntity> tmpList = corrDataEntitiesMap.get(key);
            //不论是否是多个对账单关联到同一个业务单据，只保留一个，其他的不处理
            if (tmpList.size() >= 1) {
                newCorrDataEntities.add(tmpList.get(0));
            }
        }
        //异步任务开启线程池，线程池任务中关联(并确认)业务单据，线程池中任务事务独立，只回滚失败的
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            List<Callable<Object>> callables = new ArrayList<>();
            AtomicInteger taskCount = new AtomicInteger(0);
            int ordernum = 1;
            for (CorrDataEntity corrDataEntity : newCorrDataEntities) {
                int finalOrdernum = ordernum;
                callables.add(() -> {
                    try {
                        corrOperationService.runCorrTask(corrDataEntity, finalOrdernum);
                    } catch (Exception e) {
                        log.error("自动关联出错: " + corrDataEntity.toString(), e);
                    }
                    return 1;
                });
                ordernum++;
            }
            ExecutorService executorService = null;
            try {
                executorService = buildThreadPoolForAutoCorr();
                List<Future<Object>> list = executorService.invokeAll(callables);
                if (!CollectionUtils.isEmpty(list)) {
                    for (Future<Object> futrue : list) {
                        futrue.get();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("自动关联任务异常：", e);
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
            // 批量生单需求
            /**
             * 更新认领单业务关联状态
             */
            try {
                autoCorrBillClaim();
            } catch (Exception e) {
                log.error("批量生单，更新认领单业务关联状态失败：", e);
            }
        });
    }

    private ExecutorService buildThreadPoolForAutoCorr() {
        ExecutorService executorService = ThreadPoolBuilder.defaultThreadPoolBuilder()
                .builder(null, null, null, null);
        return executorService;
    }

    public void updateBankRecAutoassociation(Vector<BankReconciliation> noAssoDataList) throws Exception {
        List<Object> ids = noAssoDataList.stream().map(e -> e.getId()).collect(Collectors.toList());
        int batchcount = 1000;
        int listSize = ids.size();
        int totalTask = (listSize % batchcount == 0 ? listSize / batchcount : (listSize / batchcount)+1);
        for (int i = 0; i < totalTask; i++) {
            int fromIndex = i * batchcount;
            int toIndex = i * batchcount + batchcount;
            if (i + 1 == totalTask) {
                toIndex = listSize;
            }
            Map<String, Object> bankReconparam = new HashMap<>();
            bankReconparam.put("ids", ids.subList(fromIndex,toIndex));
            bankReconparam.put("ytenant_id", AppContext.getYhtTenantId());
            bankReconparam.put("modifyTime", DateUtils.getNowModifyDate());
            bankReconparam.put("modifyDate", DateUtils.getNowModifyDate());
            bankReconparam.put("modifier", AppContext.getCurrentUser().getName());
            bankReconparam.put("modifierId", AppContext.getCurrentUser().getId());

            SqlHelper.update("com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper.updateBankRecAutoassociation", bankReconparam);
        }
    }

    // 根据银行对账单关联状态更新认领单关联状态
    private void autoCorrBillClaim() throws Exception {

        /**
         * 1，查询参照关联状态=已关联，业务关联状态=未关联的认领单
         * 2，根据认领单查询对应的银行对账单
         * 3，根据银行对账单关联状态，更新认领单关联状态
         */
        Map<Long, BankReconciliation> bankReconciliationMap = new HashMap<>();
        // 1,根据认领单参照关联状态已关联，关联状态未关联对应的银行对账单id
        QuerySchema querySchemaClaimItem = QuerySchema.create().addSelect(" id,bankbill,mainid ");
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("mainid.refassociationstatus").eq(1),
                QueryCondition.name("mainid.associationstatus").eq(0),
                QueryCondition.name("mainid.claimtype").not_eq(3)
        );
        querySchemaClaimItem.addCondition(group2);

        List<BillClaimItem> billClaimItems = null;
        try {
            billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchemaClaimItem, null);
        } catch (Exception ex) {
            log.error("====autoCorrBillClaim===查询参照关联状态=已关联；业务关联状态=未关联 数据失败！",ex);
        }
        List<Long> bankClaimIds = new ArrayList<>();
        List<Long> bankReconIds = new ArrayList<>();
        if(billClaimItems != null){
            billClaimItems.forEach(billClaimItem -> {
                bankClaimIds.add(billClaimItem.getMainid());
                bankReconIds.add(billClaimItem.getBankbill());
            });
        }


        List<BillClaim> billClaims = null;
        List<BankReconciliation> bankReconciliations = null;
        try {
            if(bankClaimIds.size() > 0){
                // 查询认领单
                QuerySchema querySchemaClaim = QuerySchema.create().addSelect(" * ");
                QueryConditionGroup claimGroup = QueryConditionGroup.and(
                        QueryCondition.name("id").in(bankClaimIds)
                );
                QuerySchema detailSchema = QuerySchema.create().name("items").addSelect("*");
                querySchemaClaim.addCompositionSchema(detailSchema);
                querySchemaClaim.addCondition(claimGroup);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchemaClaim, null);
            }
            if(bankReconIds.size() > 0){
                // 查询对账单
                QuerySchema querySchemaBankRecon = QuerySchema.create().addSelect(" * ");
                QueryConditionGroup bankReconGroup = QueryConditionGroup.and(
                        QueryCondition.name("id").in(bankReconIds)
                );
                querySchemaBankRecon.addCondition(bankReconGroup);
                bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchemaBankRecon, null);
            }
        } catch (Exception ex) {
            log.error("====autoCorrBillClaim===查询目标认领单和对应银行账对账单失败！",ex);
        }
        if(bankReconciliations != null && bankReconciliations.size() > 0){
            bankReconciliations.forEach(bankReconciliation -> {
                bankReconciliationMap.put(bankReconciliation.getId(), bankReconciliation);
            });
        }
        List<BillClaim> updateBillClaimList = new ArrayList<>();
        if (billClaims == null || billClaims.size() == 0){
            log.error("====autoCorrBillClaim===无参照关联状态为已关联的认领单需要更新关联状态！");
            return;
        }
        // 根据银行对账单关联状态更新认领单关联状态
        for(BillClaim billClaim : billClaims){
            Boolean updateFlag = true;
            if(billClaim.items() != null && billClaim.items().size() > 0){
                for(BillClaimItem item : billClaim.items()){
                    // 判断对账单关联状态
                    BankReconciliation bankReconciliation = bankReconciliationMap.get(item.getBankbill());
                    if(bankReconciliation != null){
                        if(bankReconciliation.getAssociationstatus() == null
                                || bankReconciliation.getAssociationstatus() == AssociationStatus.NoAssociated.getValue()){
                            updateFlag = false;
                        }
                        //统收统支下需要修改银行对账单的完结处理状态为已完结
                        /*if(bankReconciliation.getAssociationstatus() != null
                                && bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()
                                && bankReconciliation.getSerialdealendstate() != SerialdealendState.END.getValue()){
                            bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());
                            bankReconciliation.setEntityStatus(EntityStatus.Update);
                            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                        }*/
                    }else {
                        if(item.getBankbill() == null){
                            updateFlag = false;
                        }else {
                            List<Map<String, Object>> bankReconciliationMaps = MetaDaoHelper.queryById(BankReconciliation.ENTITY_NAME, "*", item.getBankbill());
                            if(bankReconciliationMaps == null || bankReconciliationMaps.size() == 0){
                                updateFlag = false;
                            }else {
                                BankReconciliation dbBankReconciliation = new BankReconciliation();
                                dbBankReconciliation.init(bankReconciliationMaps.get(0));
                                if(bankReconciliation.getAssociationstatus() == null
                                        || bankReconciliation.getAssociationstatus() == AssociationStatus.NoAssociated.getValue()){
                                    updateFlag = false;
                                }
                            }
                        }
                    }
                };
            }else {
                updateFlag = false;
            }
            if(updateFlag){
                billClaim.setAssociationstatus(AssociationStatus.Associated.getValue());
                billClaim.setEntityStatus(EntityStatus.Update);
                updateBillClaimList.add(billClaim);
            }
        };

        if(updateBillClaimList.size() > 0){
            try {
                CommonSaveUtils.updateBillClaim(updateBillClaimList);
            } catch (Exception e) {
                log.error("autoCorrBillClaim根据银行对账单关联状态更新认领单关联状态失败！失败原因：查询3：", e);
            }
        }
    }

}
