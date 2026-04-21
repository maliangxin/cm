package com.yonyoucloud.fi.cmp.writeOff.service.impl;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLossService;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss_b;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementServiceImpl;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import com.yonyoucloud.fi.cmp.writeOff.service.WriteOffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
@RequiredArgsConstructor
public class WriteOffServiceImpl implements WriteOffService {

    private final CtmThreadPoolExecutor executorServicePool;
    private final CmCommonService cmCommonService;
    private final SettlementServiceImpl settlementService;
    private final YmsOidGenerator ymsOidGenerator;
    private final BaseRefRpcService baseRefRpcService;
    private final CmpWriteBankaccUtils cmpWriteBankaccUtils;
    private final CmpVoucherService cmpVoucherService;;
    private final ExchangeGainLossService exchangeGainLossService;
    /**
     * 逆向生成红冲数据与凭证
     * @param beforeDays
     * @param logId
     * @param tenant
     * @return
     */
    @Override
    public Map<String,Object> WriteOffTask(int beforeDays, String logId, String tenant) {
        //传一个tenant_id 进来 防止id变化
        String tenant_id = AppContext.getTenantId().toString();
        Map<String,Object> retMap = new HashMap<>();
        if (CmpCommonUtil.getNewFiFlag()) {
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                YmsLock ymsLock = JedisLockUtils.lockWithOutTrace(tenant_id + "WriteOffTask");
                try {
                    if (null == ymsLock) {
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    copyInfo( beforeDays);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    log.error("queryAccountBalanceTask exception when batch process executorServicePool", e);

                } finally {
                    JedisLockUtils.unlockWithOutTrace(ymsLock);
                }
            });
            retMap.put("asynchronized",true);
        }else{
            retMap.put("asynchronized",false);
            retMap.put("id", logId);
            retMap.put("status", TaskUtils.TASK_BACK_SUCCESS);
            retMap.put("content", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */);
            retMap.put("title", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400697", "调度任务") /* "调度任务" */);
//            TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        }
        return retMap;
    }

    /**
     * 循环处理
     * @throws Exception
     */
    public void copyInfo(int beforeDays) throws Exception {
        ConcurrentHashMap<String,Object> param = new ConcurrentHashMap<>();
        //查询交易类型id
//        param.put(ICmpConstant.TRADE_TYPE, getTransTypes());
        String beforedate = "";
        if(beforeDays == 0){
            beforedate = DateUtils.getStringDateShort();
        }else{
            //1、计算日期 当前日期 - beforeDays 得到需要的查询日期
            Calendar calToday = Calendar.getInstance();
            calToday.add(Calendar.DATE, -beforeDays);//获取查询日期
            Date dateBeforeDate = calToday.getTime();
            SimpleDateFormat sp = new SimpleDateFormat("yyyy-MM-dd");
            beforedate = sp.format(dateBeforeDate);//获取查询日期
        }

        boolean isend = true;
        param.put(ICmpConstant.PAPERINDEX, 1);
        param.put(ICmpConstant.BEFOREDATE, beforedate);
        while (isend){
            isend = associationOrder(param);
        }
    }

    /**
     * 获取交易类型id
     * @return
     */
    private String getTransTypes() throws Exception {
//        Map<String, Object> condition = new HashMap<>();
//        condition.put("billtype_id","FICA5");
//        List<Map<String, Object>> transTypList = cmCommonService.getTransTypeByCondition(condition);
//        List<Map<String,Object>> transTypes= transTypList.stream().filter(e-> ICmpConstant.HDSY.equals(e.get(ICmpConstant.CODE))).collect(Collectors.toList());
        Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById("FICA5", "0", ICmpConstant.WFHDSY);
        if (!ValueUtils.isEmpty(tradetypeMap)){
            return tradetypeMap.get("id").toString();
        }
        return null;
    }

    private boolean associationOrder(ConcurrentHashMap<String,Object> param) throws Exception {
        List<Map<String, Object>> exchangeGainLossList = getExchangegainlosslist(param);
        if(CollectionUtils.isEmpty(exchangeGainLossList)){
            param.put(ICmpConstant.PAPERINDEX, 1);
            return false;
        }else{
            //业务处理
            saveExchange( exchangeGainLossList);
        }
//        param.put(ICmpConstant.PAPERINDEX, Integer.valueOf(param.get(ICmpConstant.PAPERINDEX).toString()) + 1);
        return true;
    }

    /**
     * //查询条件 1.时间段 2.下月红冲 3.关联id不为空 4.凭证已生成  5.是否冲销单据 6.交易类型为汇兑损益（不要）
     * @param param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getExchangegainlosslist (ConcurrentHashMap<String,Object> param) throws Exception {
        //查询数据
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.COVERDATE).egt(param.get(ICmpConstant.BEFOREDATE))));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.COVERDATE).elt(dateFormat.format(today))));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.NEXTMONTHCOVER).eq(true)));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ASSOCIATIONID).is_null()));
        conditionGroup.addCondition(QueryConditionGroup.and( QueryConditionGroup.or(QueryCondition.name(ICmpConstant.VOUCHER_STATUS).eq(VoucherStatus.Created.getValue()),
                QueryCondition.name(ICmpConstant.VOUCHER_STATUS).eq(VoucherStatus.POST_SUCCESS.getValue()),
                QueryCondition.name(ICmpConstant.VOUCHER_STATUS).eq(VoucherStatus.NONCreate.getValue()))));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ISCOVER).eq(false)));
        schema.addCondition(conditionGroup);
        schema.addPager(Integer.valueOf(param.get(ICmpConstant.PAPERINDEX).toString()).intValue(), 100);
        return MetaDaoHelper.query(ExchangeGainLoss.ENTITY_NAME, schema);
    }

    /**
     * 循环处理
     * @param exchangeGainLossList
     * @throws Exception
     */
    public void saveExchange(List<Map<String, Object>> exchangeGainLossList) throws Exception {
        for(Map<String, Object> exchangeMap : exchangeGainLossList){
            try {
                businessProcessing(exchangeMap);
            } catch (Exception e) {
                log.error("saveExchange businessProcessing exception:"+e.getMessage());
            }
        }
    }

    /**
     * 业务校验
     */
    public boolean businessProcessing(Map<String, Object> exchangeMap) throws Exception {
        ExchangeGainLoss exchangeGainLoss = new ExchangeGainLoss();
        exchangeGainLoss.init(exchangeMap);
        ExchangeGainLoss exchangeGainLossOld = new ExchangeGainLoss();
        exchangeGainLossOld.init(exchangeMap);
        HashMap<String,Object> characterDef = exchangeGainLoss.get("characterDef");
        if (characterDef != null) {
            exchangeGainLossOld.put("characterDef",characterDef.clone());
        }
        Long id = exchangeGainLoss.getId();
        String transTypeId = getTransTypes();
        if(StringUtils.isEmpty(transTypeId)){
            log.error("saveExchange not find transTypeId:"+id);
            return false;
        }
        exchangeGainLoss.setTradetype(transTypeId);
        setExchangeGainLoss( exchangeGainLoss);
        Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(exchangeGainLoss.getAccentity());
        if (enabledBeginData.compareTo(exchangeGainLoss.getVouchdate()) > 0) {
            log.error("saveExchange enabledBeginDatacompareTo exchangeGainLoss getVouchdate:"+id);
            return false;
        }
        Date currentDate = new Date();
        if (exchangeGainLoss.getVouchdate().compareTo(currentDate) > 0) {
            log.error("saveExchange exchangeGainLoss getVouchdate compareTo currentDate:"+id);
            return false;
        }
        //校验日结日期
        Date maxSettleDate = settlementService.getMaxSettleDate(exchangeGainLoss.getAccentity());
        if (maxSettleDate != null) {
            if (maxSettleDate.compareTo(exchangeGainLoss.getVouchdate()) >= 0) {
                log.error("saveExchange maxSettleDate compareTo Vouchdate:"+id);
                return false;
            }
        }
        List<ExchangeGainLoss_b> exchangeGainLoss_bList = getExchangeGainLossb(id);
        boolean saveState = false;
        ArrayList<Journal> journalList =  new ArrayList<Journal>();
        ArrayList<ExchangeGainLoss_b> exchangeGainLossbListes = new ArrayList<>();
        //根据汇率损益单生成日记账
        for (ExchangeGainLoss_b exchangeGainLoss_b :exchangeGainLoss_bList){
            ExchangeGainLoss_b exchangeGainLossb = getExchangeGainLossb( exchangeGainLoss_b);
            exchangeGainLossb.setMainid(exchangeGainLoss.getId());
            exchangeGainLossbListes.add(exchangeGainLossb);
            if(exchangeGainLoss_b.getAdjustbalance().compareTo(BigDecimal.ZERO)==0){
                continue;
            }else{
                saveState = true;
            }
            journalList.add(createJournal(exchangeGainLoss, exchangeGainLossb));
        }
        if (!saveState) {
            log.error("saveExchange saveState is null:"+id);
            return false;
        }
        exchangeGainLoss.setExchangeGainLoss_b(exchangeGainLossbListes);
        insertOrUpdate(exchangeGainLossOld, exchangeGainLoss, id, journalList);
        return true;
    }

    /**
     * 入库更新
     * @param exchangeGainLossOld
     * @param exchangeGainLoss
     * @param id
     * @param journalList
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void insertOrUpdate(ExchangeGainLoss exchangeGainLossOld,ExchangeGainLoss exchangeGainLoss,
                               Long id,ArrayList<Journal> journalList) {
        //汇兑损益入库-----
        try {
            CmpMetaDaoHelper.insert(ExchangeGainLoss.ENTITY_NAME, exchangeGainLoss);
//            CmpMetaDaoHelper.insert(ExchangeGainLoss_b.ENTITY_NAME,exchangeGainLossbListes);
//            CmpMetaDaoHelper.insert(Journal.ENTITY_NAME,journalList);
            //更新以前单据赋值关联code id
            exchangeGainLossOld.setAssociationid(exchangeGainLoss.getId());
            exchangeGainLossOld.setAssociationcode(exchangeGainLoss.getCode());
            EntityTool.setUpdateStatus(exchangeGainLossOld);
            MetaDaoHelper.update(ExchangeGainLoss.ENTITY_NAME, exchangeGainLossOld);
            //
            for (Journal journal : journalList) {
                cmpWriteBankaccUtils.addAccountBook(journal);
            }
            //生成凭证
            CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResultTry(exchangeGainLoss);
            if (generateResult.getInteger("code") == 0 && !generateResult.getBoolean("dealSucceed")) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418047E", "发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
            }
            Map<String, Object> params = new HashMap<>();
            if (generateResult.getInteger("code") == 1 && !generateResult.getBoolean("genVoucher")) {
                params.put("voucherstatus", VoucherStatus.NONCreate.getValue());
            }
            if (generateResult.getInteger("code") == 1 && generateResult.getBoolean("genVoucher")) {
                params.put("voucherstatus", VoucherStatus.Received.getValue());
            }
            if (generateResult.getInteger("code") == 2 && generateResult.getBoolean("dealSucceed")) {
                params.put("voucherstatus", VoucherStatus.POSTING.getValue());
            }

            params.put("tableName", IBillNumConstant.EXCHANG_EGAIN_LOSS);
            params.put("id", exchangeGainLoss.getId());
            SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherStatusReal", params);
        } catch (Exception e) {
            log.error("汇兑损益入库insertOrUpdate失败：", e);
        }
    }

    /**
     * 汇兑子表
     * @param exchangeGainLoss_b
     * @return
     */
    private ExchangeGainLoss_b getExchangeGainLossb(ExchangeGainLoss_b exchangeGainLoss_b){
        ExchangeGainLoss_b exchangeGainLossb = new ExchangeGainLoss_b();
        exchangeGainLossb.init(exchangeGainLoss_b);
        exchangeGainLossb.setCreateDate(new Date());
        exchangeGainLossb.setCreateTime(new Date());
        exchangeGainLossb.setCreatorId(AppContext.getCurrentUser().getId());
        exchangeGainLossb.setCreator(AppContext.getCurrentUser().getName());
        exchangeGainLossb.setModifier(null);
        exchangeGainLossb.setModifyDate(null);
        exchangeGainLossb.setModifyTime(null);
        exchangeGainLossb.setEntityStatus(EntityStatus.Insert);
        exchangeGainLossb.setId(ymsOidGenerator.nextId());
        exchangeGainLossb.setPubts(new Date());
        HashMap<String,Object> characterDefb = exchangeGainLossb.get("characterDefb");
        if (characterDefb != null) {
            characterDefb.put("id", ymsOidGenerator.nextId());
        }
        return exchangeGainLossb;
    }

    /**
     * 创建日记账
     * @param exchangeGainLoss
     * @param exchangeGainLoss_b
     * @return
     * @throws Exception
     */
    private Journal createJournal(ExchangeGainLoss exchangeGainLoss,ExchangeGainLoss_b exchangeGainLoss_b) throws Exception {
        return exchangeGainLossService.createJournalForWriteOff(exchangeGainLoss, exchangeGainLoss_b, IBillNumConstant.EXCHANG_EGAIN_LOSS);
    }

    /**
     * 汇兑损益赋值
     * @param exchangeGainLoss
     */
    private void setExchangeGainLoss(ExchangeGainLoss exchangeGainLoss){
        //注意顺序不能变化
        exchangeGainLoss.setAssociationcode(exchangeGainLoss.getCode());
        exchangeGainLoss.setAssociationid(exchangeGainLoss.getId());
        //---end
        exchangeGainLoss.setVouchdate(BillInfoUtils.getBusinessDate());
        exchangeGainLoss.setCreateDate(new Date());
        exchangeGainLoss.setCreateTime(new Date());
        exchangeGainLoss.setCreatorId(AppContext.getCurrentUser().getId());
        exchangeGainLoss.setCreator(AppContext.getCurrentUser().getName());
        exchangeGainLoss.setTenant(AppContext.getTenantId());
        exchangeGainLoss.setCode(getCode(exchangeGainLoss));
        exchangeGainLoss.setModifier(null);
        exchangeGainLoss.setModifyDate(null);
        exchangeGainLoss.setModifyTime(null);
        exchangeGainLoss.setEntityStatus(EntityStatus.Insert);
        exchangeGainLoss.setId(ymsOidGenerator.nextId());
        exchangeGainLoss.setVoucherstatus(VoucherStatus.Empty);
        exchangeGainLoss.setVoucherId(null);
        exchangeGainLoss.setVoucherNo(null);
        exchangeGainLoss.setVoucherPeriod(null);
        exchangeGainLoss.setVoucherdes(null);
        exchangeGainLoss.setIsCover(true);
        HashMap<String,Object> characterDef = exchangeGainLoss.get("characterDef");
        if (characterDef != null) {
            characterDef.put("id", ymsOidGenerator.nextId());
        }
    }

    /**
     * 汇兑子表查询
     * @param id
     * @return
     * @throws Exception
     */
    private List<ExchangeGainLoss_b> getExchangeGainLossb(Long id) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("mainid").in(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(ExchangeGainLoss_b.ENTITY_NAME,schema,null);
    }


    /**
     * 获取编码code
     * @param exchangeGainLoss
     * @return
     */
    private String getCode(ExchangeGainLoss exchangeGainLoss) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);

        String billcode=billCodeComponentService.getBillCode(IBillNumConstant.EXCHANG_EGAIN_LOSS, ExchangeGainLoss.ENTITY_NAME,
                InvocationInfoProxy.getTenantid(),
                "", true, "", false, new BillCodeObj(exchangeGainLoss));
        return billcode;
    }

}
