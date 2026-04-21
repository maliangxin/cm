package com.yonyoucloud.fi.cmp.receiveMarginAuto.impl;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentType;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.SettleFlagEnum;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.receiveMarginAuto.ReceiveMarginAutoService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
@RequiredArgsConstructor
public class ReceiveMarginAutoServiceImpl implements ReceiveMarginAutoService {

    private final CtmThreadPoolExecutor executorServicePool;
    private final CmCommonService cmCommonService;
    private final YmsOidGenerator ymsOidGenerator;
    private final BaseRefRpcService baseRefRpcService;
    private final MarginWorkbenchService marginWorkbenchService;

    @Autowired
    @Qualifier("stwbReceiveMarginServiceImpl")
    private StwbBillService stwbBillService;

    @Autowired
    private MarginCommonService marginCommonService;

    @Autowired
    @Lazy
    private ReceiveMarginAutoServiceImpl receiveMarginAutoServiceImpl;

    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;

    /**
     * 根据收到保证金自动退还参数
     * @param beforeDays
     * @param logId
     * @param tenant
     * @return
     */
    @Override
    public Map<String,Object> receiveMarginAutoTask(int beforeDays, String logId, String tenant,String accentity) {
        //传一个tenant_id 进来 防止id变化
        String tenant_id = AppContext.getTenantId().toString();
        Map<String,Object> retMap = new HashMap<>();
//        if (CmpCommonUtil.getNewFiFlag()) {
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace("receiveMarginAutoTask");
                try {
                    if (null == ymsLock) {
                        log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418020D","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102026"),"receiveMarginAutoTask executorServicePool lockBillWithOutTrace");
                    }else {
                        copyInfo( beforeDays,accentity);
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                    }
                } catch (Exception e) {
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    log.error("queryAccountBalanceTask exception when batch process executorServicePool", e);
                }finally {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            });
            retMap.put("asynchronized",true);
//        }else{
//            retMap.put("asynchronized",false);
//            retMap.put("id", logId);
//            retMap.put("status", TaskUtils.TASK_BACK_SUCCESS);
//            retMap.put("content", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */);
//            retMap.put("title", "调度任务");
////            TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
//        }
        return retMap;
    }

    /**
     * 循环处理
     * @throws Exception
     */
    public void copyInfo(int beforeDays,String accentity) throws Exception {
        ConcurrentHashMap<String,Object> param = new ConcurrentHashMap<>();
        BdBillType bdBillType = baseRefRpcService.queryBillTypeByFormId("CM.cmp_receivemargin");
        if(Objects.isNull(bdBillType)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102027"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508008B", "查询收到保证金台账管理单据类型失败") /* "查询收到保证金台账管理单据类型失败" */);
        }
        String billTypeId = bdBillType.getId();
        List<String> transTypeIdList = queryTransTypesByExtendforSelfDefine(billTypeId,"0","cmp_receivemargin_receive");
        if(CollectionUtils.isEmpty(transTypeIdList)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102028"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508008C", "查询收到保证金交易类型失败") /* "查询收到保证金交易类型失败" */);
        }
        param.put(ICmpConstant.TRADE_TYPE, transTypeIdList);
        String beforedate = "";
        if(beforeDays == 0){
            //获取当前时间
            beforedate = DateUtils.getStringDateShort();
        }else{
            //1、计算日期 当前日期 - beforeDays 得到需要的查询日期
            Calendar calToday = Calendar.getInstance();
            calToday.add(Calendar.DATE, + beforeDays);//获取查询日期
            Date dateBeforeDate = calToday.getTime();
            SimpleDateFormat sp = new SimpleDateFormat("yyyy-MM-dd");
            beforedate = sp.format(dateBeforeDate);//获取查询日期
        }

        AtomicBoolean isend = new AtomicBoolean(true);
        param.put(ICmpConstant.PAPERINDEX, 1);
        param.put(ICmpConstant.BEFOREDATE, beforedate);
        String transTypeIdRefund = getTransTypes(billTypeId,"cmp_receivemargin_return");
        if(StringUtils.isEmpty(transTypeIdRefund)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102029"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508008A", "查询退还保证金交易类型失败") /* "查询退还保证金交易类型失败" */);
        }
        param.put(ICmpConstant.BILLTYPE, billTypeId);
        param.put(ICmpConstant.TRANSTYPEIDREFUND, transTypeIdRefund);
        param.put(ICmpConstant.ACCENTITY,accentity);
        CopyOnWriteArrayList<Long> ides = new CopyOnWriteArrayList<>();
        while (isend.get()){
            boolean istrue = associationOrder(param,ides);
            isend.set(istrue);
        }
    }

    public List<String> queryTransTypesByExtendforSelfDefine(String billtype, String def, String code) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,name,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("billtype_id").eq(billtype));
        if ("1".equals(def)) {
            conditionGroup.appendCondition(QueryCondition.name("default").eq(def));
        }
        if (ValueUtils.isNotEmpty(code)) {
            conditionGroup.appendCondition(QueryCondition.name("extend_attrs_json").like(code));
        }
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        if (ValueUtils.isNotEmptyObj(AppContext.getYTenantId())) {
//            conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> queryTransTypes = MetaDaoHelper.query(billContext, schema);
        List<String> transTypeIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queryTransTypes)) {
            for(Map<String, Object> transtype:queryTransTypes){
               transTypeIdList.add(transtype.get("id").toString());
            }
        }
        return transTypeIdList;
    }

    /**
     * 获取交易类型id
     * @return
     */
    private String getTransTypes(String billtype, String code) throws Exception {
        Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById(billtype, "0", code);
        if (!ValueUtils.isEmpty(tradetypeMap)){
            return tradetypeMap.get("id").toString();
        }
        return null;
    }

    private boolean associationOrder(ConcurrentHashMap<String,Object> param,CopyOnWriteArrayList<Long> ides) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        List<Map<String, Object>> receiveMarginList = getReceiveMarginlist(param,ides);
        for(Map<String, Object>  receiveMargin : receiveMarginList){
            if("0".equals(String.valueOf(receiveMargin.get("settleflag")))){//是否结算为否
                if("1".equals(String.valueOf(receiveMargin.get("autorefundflag"))) && "2".equals(String.valueOf(receiveMargin.get("verifystate")))){//是否自动退还为是
                    list.add(receiveMargin);
                }
            }
            if("1".equals(String.valueOf(receiveMargin.get("settleflag")))){//是否结算为是
                //结算状态为终态，并且是否自动退还为是
                if(("3".equals(String.valueOf(receiveMargin.get("settlestatus"))) || "7".equals(String.valueOf(receiveMargin.get("settlestatus")))) && "1".equals(String.valueOf(receiveMargin.get("autorefundflag")))){
                    list.add(receiveMargin);
                }
            }
        }
        //List<Map<String, Object>> filterReceiveMarginList = receiveMarginList.stream().filter(item -> "2".equals(String.valueOf(item.get("verifystate")))).collect(Collectors.toList());
        //过滤留下结算状态为结算成功和已结算补单
        //List<Map<String, Object>> finalReceiveMarginList = filterReceiveMarginList.stream().filter(item -> "3".equals(String.valueOf(item.get("settlestatus"))) || "7".equals(String.valueOf(item.get("settlestatus")))).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(list)){
            return false;
        }else{
            //业务处理
            for(Map<String, Object> receiveMarginMap : list){
                try {
                    //String verifystate = String.valueOf(receiveMarginMap.get("verifystate"));
                    //if(verifystate.equals("3")){
                        businessProcessing(receiveMarginMap, param);
                    //}
                } catch (Exception e) {
                    ides.add( Long.parseLong(receiveMarginMap.get(ICmpConstant.ID).toString()));
                    log.error("saveExchange businessProcessing exception:"+e.getMessage());
                }
            }
        }
        return true;
    }

    /**
     * //查询条件
     * 交易类型=收到保证金
     * 事项来源=现金管理
     * 是否自动退还=是
     * 退还单编号为空
     * 退还日期<=当前日期-XX
     * @param param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getReceiveMarginlist (ConcurrentHashMap<String,Object> param,CopyOnWriteArrayList<Long> ides) throws Exception {
        //查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.TRADE_TYPE).in(param.get(ICmpConstant.TRADE_TYPE))));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.SRC_ITEM).eq(EventSource.Cmpchase.getValue())));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.AUTOREFUNDFLAG).eq((short)1)));
        conditionGroup.addCondition(QueryConditionGroup.and( QueryConditionGroup.or(QueryCondition.name(ICmpConstant.REFUNDMARGINID).is_null(),
                QueryCondition.name(ICmpConstant.REFUNDMARGINID).eq("''"))));
//        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ISCOVER).eq(false)));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.REFUNDDATE).elt(param.get(ICmpConstant.BEFOREDATE))));
        if(!CollectionUtils.isEmpty(ides)){
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ID).not_in(ides)));
        }
        if(!"".equals(param.get(ICmpConstant.ACCENTITY))){
            conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ACCENTITY).eq(param.get(ICmpConstant.ACCENTITY))));
        }
        schema.addCondition(conditionGroup);
        schema.addPager(Integer.valueOf(param.get(ICmpConstant.PAPERINDEX).toString()).intValue(), 100);
        return MetaDaoHelper.query(ReceiveMargin.ENTITY_NAME, schema);
    }

    /**
     * 业务校验
     */
    public boolean businessProcessing(Map<String, Object> receiveMarginMap,ConcurrentHashMap<String,Object> param) throws Exception {
        ReceiveMargin receiveMarginOld = new ReceiveMargin();
        receiveMarginOld.init(receiveMarginMap);
        Long id = receiveMarginOld.getId();
        ReceiveMargin receiveMarginNew = new ReceiveMargin();
        receiveMarginNew.setTradetype(param.get(ICmpConstant.TRANSTYPEIDREFUND).toString());
        receiveMarginNew.setBilltype(param.get(ICmpConstant.BILLTYPE).toString());
        setReceiveMargin( receiveMarginNew,receiveMarginOld);

        generateVoucher(receiveMarginNew);
        receiveMarginAutoServiceImpl.insertOrUpdate(receiveMarginNew, id,receiveMarginOld);
        //生成保证金的事务提交后再调用结算接口生成待结算数据
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try{
                    pushBillToSettlement(receiveMarginNew, receiveMarginOld);
                }catch (Exception e){
                    log.error("生成保证金的事务提交后再调用结算接口生成待结算数据失败！"+e.getMessage(),e);
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22E9CF9E04600002", "生成保证金的事务提交后再调用结算接口生成待结算数据失败！") /* "事务提交后，调用单据转换规则及来款记录保存失败！" */+e.getMessage(),e);
                }
            }
        });

        return true;
    }

    private void pushBillToSettlement(ReceiveMargin receiveMarginNew, ReceiveMargin receiveMarginOld) throws Exception {
        //判断是否调用资金结算  新单据退还是否结算
        if (receiveMarginNew.getSettleflag() == 1) {
            //推送资金结算
            List<BizObject> currentBillList = new ArrayList<>();
            currentBillList.add(receiveMarginNew);
            stwbBillService.pushBill(currentBillList, false);// 推送资金结算
        }
        //老单据推送
        if (receiveMarginOld.getSettleflag() == 1) {
            //推送资金结算
            List<BizObject> currentBillList = new ArrayList<>();
            currentBillList.add(receiveMarginOld);
            stwbBillService.pushBill(currentBillList, false);// 推送资金结算
        }
    }

    private void generateVoucher(ReceiveMargin receiveMargin) throws Exception {
        if (receiveMargin.getSettleflag() == SettleFlagEnum.NO.getValue()) {
            marginCommonService.generateVoucher(receiveMargin);
        }
    }

    /**
     * 入库更新
     * @param receiveMargin
     * @param id
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public void insertOrUpdate(ReceiveMargin receiveMargin, Long id,ReceiveMargin receiveMarginOldt) throws Exception {
        //校验虚拟户余额
        String marginvirtualaccount = receiveMargin.getMarginvirtualaccount().toString();
        MarginWorkbench marginWorkbench = MetaDaoHelper.findById(MarginWorkbench.ENTITY_NAME,marginvirtualaccount);
        if (ObjectUtils.isEmpty(marginWorkbench)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100982"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA6198604C00004", "退还保证金的时候未查询到虚拟户信息，请检查！") /* "退还保证金的时候未查询到虚拟户信息，请检查！" */);
        }
        //针对虚拟户上锁，直到事务结束，避免并发
        if (!ymsScopeLockManager.tryTxScopeLock(marginWorkbench.getId().toString())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101500"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19096F5A04280012", "已有他人操作同一保证金虚拟账户，请稍候重试！") /* "已有他人操作同一保证金虚拟账户，请稍候重试！" */);
        }
        BigDecimal marginAvailableBalance = marginWorkbench.getMarginAvailableBalance();
        BigDecimal sumamount = receiveMargin.getMarginamount();
        BigDecimal difference = marginAvailableBalance.subtract(sumamount);
        if (difference.compareTo(BigDecimal.ZERO) < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA34D0604C00009", "该单据保证金发生额大于“保证金可用余额”，请检查！") /* "该单据保证金发生额大于“保证金可用余额”，请检查！" */);
        }
        CmpMetaDaoHelper.insert(ReceiveMargin.ENTITY_NAME,receiveMargin);
        CtmJSONObject receiveMarginParams = new CtmJSONObject();
        receiveMarginParams.put(ICmpConstant.RECMARGIN, receiveMargin);
        //收到保证金台账传保证金工作台 保存接口
        marginWorkbenchService.recMarginWorkbenchSave(receiveMarginParams);
        ReceiveMargin receiveMarginOld = new ReceiveMargin();
        receiveMarginOld.setId(id);
        receiveMarginOld.setRefundmargincode(receiveMargin.getCode());
        receiveMarginOld.setRefundmarginid(receiveMargin.getId().toString());
        EntityTool.setUpdateStatus(receiveMarginOld);
        MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME,receiveMarginOld);

        CtmJSONObject params = new CtmJSONObject();
        params.put(ICmpConstant.MARGINBUSINESSNO,receiveMargin.getMarginbusinessno());
        params.put(ICmpConstant.SRC_ITEM,receiveMargin.getSrcitem());
        params.put(ICmpConstant.ACTION,ICmpConstant.AUDIT);
        params.put(ICmpConstant.TRADETYPE,receiveMargin.getTradetype());
        params.put(ICmpConstant.MARGINAMOUNT,receiveMargin.getMarginamount());
        params.put(ICmpConstant.NATMARGINAMOUNT,receiveMargin.getNatmarginamount());
        params.put(ICmpConstant.CONVERSIONAMOUNT,receiveMargin.getConversionamount());
        params.put(ICmpConstant.NATCONVERSIONAMOUNT,receiveMargin.getNatconversionamount());
        params.put(ICmpConstant.SETTLEFLAG,receiveMargin.getSettleflag());
        params.put(ICmpConstant.SETTLE_STATUS,receiveMargin.getSettlestatus());
        params.put(ICmpConstant.RECMARGIN, receiveMargin);
        marginWorkbenchService.recMarginWorkbenchUpdate( params);
    }



    /**
     * 赋值
     * @param receiveMargin
     */
    private void setReceiveMargin(ReceiveMargin receiveMargin,ReceiveMargin receiveMarginOld) throws Exception {
        //注意顺序不能变化
        receiveMargin.setAccentity(receiveMarginOld.getAccentity());
        receiveMargin.setVouchdate(BillInfoUtils.getBusinessDate());//单据日期
        receiveMargin.setRefundmargincode(receiveMarginOld.getCode());//退还保证金单据编号
        receiveMargin.setRefundmarginid(receiveMarginOld.getId().toString());//退还保证金单据id
        receiveMargin.setMarginvirtualaccount(receiveMarginOld.getMarginvirtualaccount());//保证金虚拟户
        receiveMargin.setMarginbusinessno(receiveMarginOld.getMarginbusinessno());//保证金原始业务号
        receiveMargin.setMargintype(receiveMarginOld.getMargintype());//保证金类型
        receiveMargin.setCurrency(receiveMarginOld.getCurrency());//币种
        receiveMargin.setNatCurrency(receiveMarginOld.getNatCurrency());//本币币种
        receiveMargin.setProject(receiveMarginOld.getProject());//项目
        receiveMargin.setDept(receiveMarginOld.getDept());//部门
        receiveMargin.setLatestreturndate(receiveMarginOld.getLatestreturndate());//最迟退还日期
        receiveMargin.setExchangeratetype(receiveMarginOld.getExchangeratetype());//汇率类型
        receiveMargin.setExchRate(receiveMarginOld.getExchRate());//汇率
        receiveMargin.setExchRateOps(receiveMarginOld.getExchRateOps()); // 汇率折算方式
        receiveMargin.setSrcitem(EventSource.Cmpchase.getValue());//事项来源
        receiveMargin.setVerifystate(VerifyState.COMPLETED.getValue());//审批状态
        receiveMargin.setVoucherstatus(VoucherStatus.Empty.getValue());//凭证状态
        receiveMargin.setVoucherId(null);//事项分录ID
        receiveMargin.setVoucherNo(null);//凭证号
        receiveMargin.setVoucherPeriod(null);//凭证期间
        receiveMargin.setVoucherstatusdescription(null);//凭证状态
        receiveMargin.setDescription(null);//备注
        receiveMargin.setOppositetype(receiveMarginOld.getOppositetype());//对方类型
        receiveMargin.setMarginamount(receiveMarginOld.getMarginamount());//保证金金额
        receiveMargin.setSettlemode(receiveMarginOld.getSettlemode());//结算方式
        receiveMargin.setSupplier(receiveMarginOld.getSupplier());//供应商
        receiveMargin.setCustomerbankaccount(receiveMarginOld.getCustomerbankaccount());//客户银行账户
        receiveMargin.setSupplierbankaccount(receiveMarginOld.getSupplierbankaccount());//供应商银行账户
        if (receiveMarginOld.getRefundsettleflag() == 0) {
            // 退还保证金是否结算为否时，生成的保证金直接结算成功
            receiveMargin.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());//结算状态
            receiveMargin.setSettlesuccesstime(new Date());
        } else {
            receiveMargin.setSettlestatus(FundSettleStatus.WaitSettle.getValue());//结算状态
        }
        receiveMargin.setPaymentsettlemode(receiveMarginOld.getPaymentsettlemode());//付款结算模式

        CurrencyTenantDTO natCurrencyDTO = findNatCurrency(receiveMarginOld.getNatCurrency());
        Integer moneyRunt = natCurrencyDTO.getMoneyrount();
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        switch (moneyRunt) {
            case 0:
                roundingMode = RoundingMode.UP;
                break;
            case 1:
                roundingMode = RoundingMode.DOWN;
                break;
            default:
                break;
        }
        receiveMargin.setNatmarginamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(receiveMargin.getExchRateOps(),
                receiveMargin.getExchRate(), receiveMargin.getMarginamount(), natCurrencyDTO.getMoneydigit()));//本币保证金金额
        receiveMargin.setSettleflag(receiveMarginOld.getRefundsettleflag());//是否结算
        receiveMargin.setPaymenttype(PaymentType.FundPayment.getValue());//收付类型
        receiveMargin.setEnterprisebankaccount(receiveMarginOld.getEnterprisebankaccount());//本方银行账户
        receiveMargin.setOppositename(receiveMarginOld.getOppositename());//对方名称
        receiveMargin.setOppositebankaccountname(receiveMarginOld.getOppositebankaccountname());//对方银行账户名称
        receiveMargin.setOppositebankaccount(receiveMarginOld.getOppositebankaccount());//对方银行账号
        receiveMargin.setOppositebankType(receiveMarginOld.getOppositebankType());//对方银行类别
        receiveMargin.setOppositebankNumber(receiveMarginOld.getOppositebankNumber());//对方开户网点
        receiveMargin.setAutorefundflag((short) 0);//是否自动退还
        receiveMargin.setOurname(receiveMarginOld.getOurname());//内部单位名称
        receiveMargin.setRefunddate(null);//退还日期
        receiveMargin.setConversionmarginflag((short) 0);//转换保证金
        receiveMargin.setConversionamount(null);//转换金额
        receiveMargin.setNatconversionamount(null);//本币转换金额
        receiveMargin.setNewmarginbusinessno(null);//
        receiveMargin.setNewmargintype(null);//新保证金类型
        receiveMargin.setNewproject(null);//设置新项目
        receiveMargin.setNewdept(null);//设置新部门
        receiveMargin.setCustomer(receiveMarginOld.getCustomer());//客户名称
        receiveMargin.setOurbankaccount(receiveMarginOld.getOurbankaccount());//内部单位银行账户名称
        receiveMargin.setCapBizObj(receiveMarginOld.getCapBizObj());//资金业务对象
        receiveMargin.setCapBizObjbankaccount(receiveMarginOld.getCapBizObjbankaccount());//资金业务对象银行账户名称


        receiveMargin.setCreateDate(new Date());
        receiveMargin.setCreateTime(new Date());
        receiveMargin.setCreatorId(AppContext.getCurrentUser().getId());
        receiveMargin.setCreator(AppContext.getCurrentUser().getName());
        receiveMargin.setTenant(AppContext.getTenantId());
        receiveMargin.setModifier(null);
        receiveMargin.setModifyDate(null);
        receiveMargin.setModifyTime(null);
        Short status = 1;
        receiveMargin.setStatus(status);
        receiveMargin.setAuditDate(new Date());
        receiveMargin.setAuditor(AppContext.getCurrentUser().getName());
        receiveMargin.setAuditTime(new Date());
        receiveMargin.setAuditorId(AppContext.getCurrentUser().getId());
        receiveMargin.setCode(getCode(receiveMargin));
        receiveMargin.setEntityStatus(EntityStatus.Insert);
        receiveMargin.setId(ymsOidGenerator.nextId());
    }

    /**
     * 查询本币币种
     *
     * @param natCurrency
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO findNatCurrency(String natCurrency) throws Exception {
        CurrencyTenantDTO currencyNatDTO = null;
        String currencyKey = natCurrency.concat(InvocationInfoProxy.getTenantid()).concat(ICmpConstant.NATCURRENCY);
        if (null != AppContext.cache().getObject(currencyKey)) {
            currencyNatDTO = AppContext.cache().getObject(currencyKey);
        } else {
            currencyNatDTO = baseRefRpcService.queryCurrencyById(natCurrency);
            if (null != currencyNatDTO) {
                AppContext.cache().setObject(currencyKey, currencyNatDTO);
            }
        }
        return currencyNatDTO;
    }

    /**
     * 获取编码code
     * @param receiveMargin
     * @return
     */
    private String getCode(ReceiveMargin receiveMargin) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);

        String billcode=billCodeComponentService.getBillCode(IBillNumConstant.CMP_RECEIVEMARGIN, ReceiveMargin.ENTITY_NAME,
                InvocationInfoProxy.getTenantid(),
                "", true, "", false, new BillCodeObj(receiveMargin));
        return billcode;
    }

}
