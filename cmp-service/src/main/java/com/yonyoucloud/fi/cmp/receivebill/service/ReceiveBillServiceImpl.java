package com.yonyoucloud.fi.cmp.receivebill.service;


import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillSettleBO;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillUnsettleBO;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sz on 2019/4/20 0020.
 */
@Service
@Slf4j
public class ReceiveBillServiceImpl implements ReceiveBillService {

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private SettlementService settlementService;

    @Resource
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;


    @Autowired
    YTSReceiveBillSettleServiceImpl ytsReceiveBillSettleServiceImpl;
    @Autowired
    ReceiveBillSettleServiceImpl receiveBillSettleServiceImpl;

//    private ForkJoinPool joinPool = new ForkJoinPool(200);

    @Override
    public List<ReceiveBill> queryAggvoByIds(Long[] ids) throws Exception {
        List<ReceiveBill> receiveBillListQuery = new ArrayList<>();
        for (Long id : ids) {
            ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, id, 3);
            if (receiveBill != null) {
                receiveBillListQuery.add(receiveBill);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101487"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180782","查询不到对应单据,请确认单据是否存在或刷新后重新操作") /* "查询不到对应单据,请确认单据是否存在或刷新后重新操作" */);
            }
        }
        return receiveBillListQuery;
    }

    @Override
    public List<ReceiveBill> getReceiveBillByIds(Long[] ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*,ReceiveBill_b.*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, querySchema, null);
    }


    @Override
    @Transactional
    public CtmJSONObject settle(List<ReceiveBill> receiveBillList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (receiveBillList == null || receiveBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077C","请选择单据！") /* "请选择单据！" */);
        }
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        Map<Long, String> codeMap = new ConcurrentHashMap<>();
        Long[] ids = new Long[receiveBillList.size()];
        for (int i = 0; i < receiveBillList.size(); i++) {
            Long receiveBillId = receiveBillList.get(i).getId();
            ids[i] = receiveBillId;
            codeMap.put(receiveBillId, receiveBillList.get(i).getCode());
            //校验结算日期是否已日结
            String accentity = receiveBillList.get(i).getAccentity();
            if(!maxSettleDateMaps.containsKey(accentity)){
                Date maxSettleDate = settlementService.getMaxSettleDate(accentity);
                maxSettleDateMaps.put(accentity, maxSettleDate);
            }
        }
        List<ReceiveBillSettleBO> taskResults = new ArrayList<>(1);
        Date date = BillInfoUtils.getBusinessDate();
        List<Future<ReceiveBillSettleBO>> futures = new ArrayList<>(receiveBillList.size());

        if (ids.length == 1) {
            ReceiveBillSettleBO processResult = receiveBillSettleServiceImpl.processSettleItem(ids[0], false, codeMap, date, maxSettleDateMaps);
            taskResults.add(processResult);
        } else {
            for (Long receiveBillId:ids) {
                Future<ReceiveBillSettleBO> task = null;
                try {
                    task = ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                        try {
                            return receiveBillSettleServiceImpl.processSettleItem(receiveBillId, true, codeMap, date, maxSettleDateMaps);
                        } catch (Exception e) {
                            log.error("exception when batch process settle", e);
                            return ReceiveBillSettleBO.builder()
                                    .failedId(receiveBillId.toString())
                                    .message(e.getMessage())
                                    .build();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    log.error("exception when batch process settle", e);
                }
                futures.add(task);
            }

            futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("settle future get result failed", e);
                    return null;
                }
            }).filter(Objects::nonNull).forEach(taskResults::add);
        }

        Map<String, String> failed = new HashMap<>();
        AtomicInteger failedCount = new AtomicInteger(0);
        taskResults.forEach(processResult -> {
            if(!StringUtils.isBlank(processResult.getMessage())){
                messages.add(processResult.getMessage());
            }
            if (processResult.getFailedId() != null) {
                failedCount.incrementAndGet();
                failed.put(processResult.getFailedId(), processResult.getFailedId());
            }
        });

        String message = null;
        if (ids.length == 1) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078F","结算成功!") /* "结算成功!" */;
        } else {
            message = com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026301") /* "共：" */ + ids.length + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026150") /* "张单据；" */ + (ids.length - failedCount.intValue()) + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026098") /* "张结算成功；" */ + failedCount.intValue() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026189") /* "张结算失败！" */;
        }
        result.put("msgs", messages);
        result.put("dzdate", BillInfoUtils.getBusinessDate() != null ? BillInfoUtils.getBusinessDate() : new Date());
        result.put("msg", message);
        result.put("messages", messages);
        result.put("count", ids.length);
        result.put("sucessCount", ids.length - failedCount.intValue());
        result.put("failCount", failedCount.intValue());
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject receiveBillSp(List<ReceiveBill> receiveBillList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (receiveBillList == null || receiveBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077C","请选择单据！") /* "请选择单据！" */);
        }
        Long[] ids = new Long[receiveBillList.size()];
        for (int i = 0; i < receiveBillList.size(); i++) {
            ids[i] = receiveBillList.get(i).getId();
        }
        List<ReceiveBill> receiveBillListNew = this.queryAggvoByIds(ids);
        List<Journal> journalList = new ArrayList<Journal>();
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        Date date = BillInfoUtils.getBusinessDate();
        for (ReceiveBill receiveBill : receiveBillListNew) {
            Long receiveBillId = receiveBill.getId();
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(receiveBillId.toString());
            if (null==ymsLock) {
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180785","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */+ receiveBill.getCode() /* "该单据已锁定，请稍后重试！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101489"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180788","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                i++;
                continue;
            }
            if (date != null && receiveBill.getVouchdate() != null && date.compareTo(receiveBill.getVouchdate()) < 0) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078A","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101490"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078A","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */));
                }
                i++;
                continue;
            }
            if (receiveBill.getSettlestatus() != null && receiveBill.getSettlestatus().getValue() == 2) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078D","】已结算，不能进行审批！") /* "】已结算，不能进行审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101491"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180790","该单据已结算，不能进行审批！") /* "该单据已结算，不能进行审批！" */);
                }
                i++;
                continue;
            }
            if (receiveBill.getAuditstatus() != null && receiveBill.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180791","】已审批，不能进行重复审批！") /* "】已审批，不能进行重复审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101492"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180792","该单据已审批，不能进行重复审批！") /* "该单据已审批，不能进行重复审批！" */);
                }
                i++;
                continue;
            }
            if (receiveBill.getSrcitem() != null && receiveBill.getSrcitem().getValue() != EventSource.Cmpchase.getValue()) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180793","】不是现金自制单据，不能进行审批！") /* "】不是现金自制单据，不能进行审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180794","该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */);
                }
                i++;
                continue;
            }

            receiveBill.setAuditstatus(AuditStatus.Complete);
            receiveBill.setAuditorId(AppContext.getCurrentUser().getId());
            receiveBill.setAuditor(AppContext.getCurrentUser().getName());
            receiveBill.setAuditTime(new Date());
            receiveBill.setAuditDate(BillInfoUtils.getBusinessDate());
            journalList.addAll(journalService.updateJournalByBill(receiveBill));
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        EntityTool.setUpdateStatus(receiveBillListNew);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receiveBillListNew);
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        String message = null;
        if (receiveBillListNew.size() == 1) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180786","审批成功!") /* "审批成功!" */;
        } else {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180780","共：") /* "共：" */ + receiveBillListNew
                    .size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180781","张单据；") /* "张单据；" */ + (receiveBillListNew
                    .size() - i) + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026271") /* "张审批通过；" */ + i + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026233") /* "张审批未通过！" */;
        }
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", receiveBillListNew.size());
        result.put("sucessCount", receiveBillListNew.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject receiveBillQxsp(List<ReceiveBill> receiveBillList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (receiveBillList == null || receiveBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077C","请选择单据！") /* "请选择单据！" */);
        }
        Long[] ids = new Long[receiveBillList.size()];
        for (int i = 0; i < receiveBillList.size(); i++) {
            ids[i] = receiveBillList.get(i).getId();
        }
        List<ReceiveBill> receiveBillListNew = this.queryAggvoByIds(ids);
        List<Journal> journalList = new ArrayList<Journal>();
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        for (ReceiveBill receiveBill : receiveBillListNew) {
            Long receiveBillId = receiveBill.getId();
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(receiveBillId.toString());
            if (null==ymsLock) {
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180785","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */+ receiveBill.getCode() /* "该单据已锁定，请稍后重试！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101489"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180788","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                i++;
                continue;
            }
            if (receiveBill.getSrctypeflag() != null && receiveBill.getSrctypeflag().equals("auto")) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077E","】是自动生成的单据，不能进行取消审批！") /* "】是自动生成的单据，不能进行取消审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101490"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【" +
                    receiveBill.getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001066022") /* "】是自动生成的单据，不能进行取消审批！" */));
                }
                i++;
                continue;
            }

            if (receiveBill.getSettlestatus() != null && receiveBill.getSettlestatus().getValue() == 2) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180784","】已结算，不能进行取消审批！") /* "】已结算，不能进行取消审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101493"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180787","该单据已结算，不能进行取消审批！") /* "该单据已结算，不能进行取消审批！" */);
                }
                i++;
                continue;
            }
            if (receiveBill.getAuditstatus() != null && receiveBill.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180789","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101494"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078B","该单据未审批，不能进行取消审批！") /* "该单据未审批，不能进行取消审批！" */);
                }
                i++;
                continue;
            }
            if (receiveBill.getSrcitem() != null && receiveBill.getSrcitem().getValue() != EventSource.Cmpchase.getValue()) {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077D","单据【") /* "单据【" */ + receiveBill
                        .getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078C","】不是现金自制单据，不能进行取消审批！") /* "】不是现金自制单据，不能进行取消审批！" */);
                if (receiveBillListNew.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418078E","该单据不是现金自制单据，不能进行取消审批！") /* "该单据不是现金自制单据，不能进行取消审批！" */);
                }
                i++;
                continue;
            }
            // begin 日结逻辑控制调整 majfd 21/06/07
            //已日结后不能修改或删除期初数据
//            QuerySchema querySchema = QuerySchema.create().addSelect("1");
//            querySchema.addCondition(QueryConditionGroup
//                    .and(QueryCondition.name("settleflag").eq(true), QueryCondition.name("settlementdate").eq(receiveBill.get("vouchdate"))
//                            , QueryCondition.name("accentity").eq(receiveBill.get("accentity"))));
//            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
//            if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
//                failed.put(receiveBill.getId().toString(), receiveBill.getId().toString());
//                messages.add(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026209") /* "单据【" */ + receiveBill
//                        .getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153345") /* "】已日结，不能取消审批！" */);
//                if (receiveBillList.size() == 1) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101495"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026170") /* "该单据已日结，不能取消审批！" */);
//                }
//                i++;
//                continue;
//            }
            // end
            receiveBill.setAuditstatus(AuditStatus.Incomplete);
            receiveBill.setAuditorId(null);
            receiveBill.setAuditor(null);
            receiveBill.setAuditTime(null);
            receiveBill.setAuditDate(null);
            journalList.addAll(journalService.updateJournalByBill(receiveBill));
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        EntityTool.setUpdateStatus(receiveBillListNew);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receiveBillListNew);
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        String message = null;
        if (receiveBillListNew.size() == 1) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180783","取消审批成功!") /* "取消审批成功!" */;
        } else {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180780","共：") /* "共：" */ + receiveBillListNew
                    .size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180781","张单据；") /* "张单据；" */ + (receiveBillListNew
                    .size() - i) + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026198") /* "张取消审批成功；" */ + i + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026009") /* "张取消审批失败！" */;
        }
        for (ReceiveBill receiveBillNew : receiveBillListNew) {
            receiveBillNew.setPubts(cmCommonService.getPubTsById(ReceiveBill.ENTITY_NAME, receiveBillNew.getId()));
        }
        result.put("rows", receiveBillListNew);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", receiveBillListNew.size());
        result.put("sucessCount", receiveBillListNew.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject unSettle(List<ReceiveBill> receiveBillList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (receiveBillList == null || receiveBillList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077C","请选择单据！") /* "请选择单据！" */);
        }
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        Long[] ids = new Long[receiveBillList.size()];
        for (int i = 0; i < receiveBillList.size(); i++) {
            ids[i] = receiveBillList.get(i).getId();
            //校验结算日期是否已日结
            String accentity = receiveBillList.get(i).getAccentity();
            if(!maxSettleDateMaps.containsKey(accentity)){
                Date maxSettleDate = settlementService.getMaxSettleDate(accentity);
                maxSettleDateMaps.put(accentity, maxSettleDate);
            }
        }
        List<ReceiveBill> receiveBillListNew = this.queryAggvoByIds(ids);
        List<ReceiveBillUnsettleBO> results = new ArrayList<>(1);
        List<Future<ReceiveBillUnsettleBO>> futures = new ArrayList<>(receiveBillListNew.size());
        long forgettime = 0;
        if (receiveBillListNew.size() == 1) {
            ReceiveBillUnsettleBO processResult = receiveBillSettleServiceImpl.processUnsettleItem(receiveBillListNew.get(0), false, maxSettleDateMaps);
            results.add(processResult);
        } else {
            for (ReceiveBill receiveBill : receiveBillListNew) {
                Future<ReceiveBillUnsettleBO> task = null;
                try {
                    task = ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                        try {
                            return receiveBillSettleServiceImpl.processUnsettleItem(receiveBill, true, maxSettleDateMaps);
                        } catch (Exception e) {
                            if(log.isInfoEnabled()) {
                                log.warn("exception when do unsettle with receive bill {}", CtmJSONObject.toJSON(receiveBill), e);
                            }
                            return ReceiveBillUnsettleBO.builder()
                                    .failedId(receiveBill.getId().toString())
                                    .message(e.getMessage())
                                    .bill(receiveBill)
                                    .build();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    log.error("exception when batch process unsettle", e);
                }
                futures.add(task);
            }

            futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("future get result failed", e);
                    return null;
                }
            }).filter(Objects::nonNull).forEach(results::add);
        }
        Map<String, String> failed = new HashMap<>();
        List<Journal> journalList = new ArrayList<>();
        List<ReceiveBill> receivebillList = new ArrayList<>();
        AtomicInteger failedCount = new AtomicInteger(0);
        results.forEach(processResult -> {
            if (!StringUtils.isBlank(processResult.getMessage())) {
                messages.add(processResult.getMessage());
            }
            if (processResult.getFailedId() != null) {
                failedCount.incrementAndGet();
                failed.put(processResult.getFailedId(), processResult.getFailedId());
            }
            receivebillList.add(processResult.getBill());
            journalList.addAll(processResult.getJournalList());
        });
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        String message = null;
        if (receiveBillListNew.size() == 1) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418077F","取消结算成功!") /* "取消结算成功!" */;
        } else {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180780","共：") /* "共：" */ + receiveBillListNew
                    .size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180781","张单据；") /* "张单据；" */ + (receiveBillListNew
                    .size() - failedCount.intValue()) + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026155") /* "张取消结算成功；" */ + failedCount + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_FI_CM_0000026329") /* "张取消结算失败！" */;
        }
        for (ReceiveBill receiveBillNew : receivebillList) {
            receiveBillNew.setPubts(cmCommonService.getPubTsById(ReceiveBill.ENTITY_NAME, receiveBillNew.getId()));
        }
        result.put("rows", receivebillList);
        result.put("msg", message);
        result.put("dzdate", "");
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", receiveBillListNew.size());
        result.put("sucessCount", receiveBillListNew.size() - failedCount.intValue());
        result.put("failCount", failedCount);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }



}
