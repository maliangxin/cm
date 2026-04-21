package com.yonyoucloud.fi.cmp.migrade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TranstypeQueryPageParam;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.CustomerRpcService;
import com.yonyoucloud.fi.basecom.service.ref.SupplierRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollectionCharacterDef;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollectionCharacterDefb;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.CmpFundPaymentCharacterDef;
import com.yonyoucloud.fi.cmp.fundpayment.CmpFundPaymentbCharacterDef;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillCharacterDef;
import com.yonyoucloud.fi.cmp.paybill.PayBillCharacterDefb;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillCharacterDef;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillCharacterDefb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill_b;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.NonNull;
import org.imeta.biz.base.BizContext;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.model.Entity;
import org.imeta.core.model.Property;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class CmpNewFiMigradeServiceImpl implements CmpNewFiMigradeService{

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CustomerRpcService customerRpcService;

    @Autowired
    SupplierRpcService supplierRpcService;

    @Autowired
    CmpNewFiMigradeUtilService cmpNewFiMigradeUtilService;

    @Autowired
    ITransTypeService iTransTypeService;

    @Autowired
    private IFundCommonService fundCommonService;

    @Qualifier("stwbPaymentBillServiceImpl")
    @Autowired
    private StwbBillService stwbPayBillService;

    @Qualifier("stwbCollectionBillServiceImpl")
    @Autowired
    StwbBillService stwbCollectionBillService;

    private static String CMPNEWFIMIGRADEMAPPER = "com.yonyoucloud.fi.cmp.mapper.CmpNewFiMigradeMapper.";

    /*特征分配字段*/
    private static final String U8C_DOMAIN = "u8c-userdefine";// 李梦云提供
    private static final String SUCCESS_CODE = "200";
    private static final String FAIL_CODE = "999";
    private static List<String> fullNameList = Arrays.asList(
            PayBillCharacterDef.ENTITY_NAME,
            PayBillCharacterDefb.ENTITY_NAME,
            ReceiveBillCharacterDef.ENTITY_NAME,
            ReceiveBillCharacterDefb.ENTITY_NAME
    );
    private static List<String> addFullNameList = Arrays.asList(
            CmpFundPaymentCharacterDef.ENTITY_NAME,
            CmpFundPaymentbCharacterDef.ENTITY_NAME,
            FundCollectionCharacterDef.ENTITY_NAME,
            FundCollectionCharacterDefb.ENTITY_NAME
    );
    private static Map<String,String> fullNameMap = new HashMap<>();
    static {
        fullNameMap.put(PayBillCharacterDef.ENTITY_NAME,CmpFundPaymentCharacterDef.ENTITY_NAME);
        fullNameMap.put(PayBillCharacterDefb.ENTITY_NAME,CmpFundPaymentbCharacterDef.ENTITY_NAME);
        fullNameMap.put(ReceiveBillCharacterDef.ENTITY_NAME,FundCollectionCharacterDef.ENTITY_NAME);
        fullNameMap.put(ReceiveBillCharacterDefb.ENTITY_NAME,FundCollectionCharacterDefb.ENTITY_NAME);
    }


    private static final @NonNull Cache<String, String> tradetypeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(3))
            .softValues()
            .build();

    /**
     * 数据迁移 从付款 至 资金付款
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject migradePayToFunPayMent(CtmJSONObject params) throws Exception {
        /**
         * 先进行数据升级校验 有特殊情况不允许升级
         * 1.有银企联支付的在途数据
         * 2.升级月份前的没有生成凭证的 已结算完成数据
         */
        Map<String ,Boolean> checkMap = cmpNewFiMigradeUtilService.checkBeforeUpgrade(PayBill.ENTITY_NAME);
        if(!checkMap.get("PayStatus")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E3", "存在网银支付状态不为支付成功的数据，不允许升级。") /* "存在网银支付状态不为支付成功的数据，不允许升级。" */);
        }else if(!checkMap.get("VouchSatus")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E4", "存在升级月份前，凭证状态不为已生成的数据，不允许升级。") /* "存在升级月份前，凭证状态不为已生成的数据，不允许升级。" */);
        }else if(!checkMap.get("CheckPayTransType")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E5", "付款工作台的交易类型，没有完全同步到资金付款单，不允许升级。") /* "付款工作台的交易类型，没有完全同步到资金付款单，不允许升级。" */);
        }else if(!checkMap.get("CheckPayapplicationApprove")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E6", "付款申请单存在审批过程中的单据，需要审批完成后才能升级。") /* "付款申请单存在审批过程中的单据，需要审批完成后才能升级。" */);
        }else if(!checkMap.get("CheckCmpBillApprove")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E7", "付款工作台存在审批过程中的单据，需要审批完成后才能升级。。") /* "付款工作台存在审批过程中的单据，需要审批完成后才能升级。。" */);
        }
        //查询交易类型集合
        Map<String,Map <String , BdTransType>> allTransTypeMap = cmpNewFiMigradeUtilService.getAllTransTypeMap();
        //查询需要迁移的总数据量
        QuerySchema queryReCount = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
//        conditionGroup.appendCondition(QueryCondition.name("migrateflag").is_null());
        queryReCount.addCondition(conditionGroup);
        List<Map<String, Object>> payCount = MetaDaoHelper.query(PayBill.ENTITY_NAME, queryReCount);
        int recordCount = Integer.valueOf(payCount.get(0).get("count").toString());
        //总条数按500 分组返回页数集合
        int pageSize = 500;
        int queryCount = recordCount/pageSize <= 0? 1 : (recordCount/pageSize) +
                (recordCount%pageSize ==0?0:1);
        List<Integer> pageIndex = new ArrayList<>();
        for(int i=1;i<=queryCount;i++){
            pageIndex.add(i);
        }
        //获取当前租户期间启用
        String uid = params.getString("uid");
        //构建进度条信息
        ProcessUtil.initProcess(uid,queryCount);
        AtomicReference<ExecutorService> executorService = new AtomicReference<>();
        //开启异步执行
        ExecutorService taskExecutor = null;
        taskExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"migradePayToFunPayMent-threadpool");
        try{
            taskExecutor.submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(ICmpConstant.MIGRADEPAYTOFUNDPAYMENT,2*60*60L, TimeUnit.SECONDS,(int lockstatus)->{
                        if(lockstatus == LockStatus.GETLOCK_FAIL){
                            //加锁失败添加报错信息 并把进度置为100%
                            ProcessUtil.addMessage(uid, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004DB", "付款工作台数据正在迁移中") /* "付款工作台数据正在迁移中" */) /* "系统正在对此账户拉取中" */);
                            ProcessUtil.completed(uid);
                            return ;
                        }
                        //加锁成功 多批次处理 插入数据
                        executorService.set(buildThreadPoolForFiMigrade("cmp-migradePayToFunPayMent-async-"));
                        ThreadPoolUtil.executeByBatch(executorService.get(),pageIndex,5,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E0", "付款工作台迁移") /* "付款工作台迁移" */,(int fromIndex, int toIndex)->{
                            for(int t = fromIndex ; t < toIndex; t++){
                                migradePayToFunPayMentExcute(pageIndex.get(t), pageSize,allTransTypeMap);
                                ProcessUtil.refreshProcess(uid,true);
                            }
                            return "";
                        });
                        //插入数据后处理在途数据 需要删账 改余额(使用sql处理 不需要分批)
                        deleteJournalAndChangeBal("cmp_paybill");
                        //筛选出已经升级 且 审批完成 且未结算的数据 推升结算
                        migradePayPushSettle(FundPayment.ENTITY_NAME,ICmpConstant.FUND_PAYMENT_B);
                    });
                } catch (Exception e) {
                    log.error("migradePayToFunPayMent-error", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101286"),e.getMessage());
                }finally {
                    if(executorService != null){
                        executorService.get().shutdown();
                    }
                    ProcessUtil.completed(uid);
                }
            });
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
        return null;
    }

    @Override
    public void migradePayToFunPayMentExcuteForSystem() throws Exception {
        //查询交易类型集合
        Map<String,Map <String , BdTransType>> allTransTypeMap = cmpNewFiMigradeUtilService.getAllTransTypeMap();
        //查询需要迁移的总数据量
        QuerySchema queryReCount = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        queryReCount.addCondition(conditionGroup);
        List<Map<String, Object>> payCount = MetaDaoHelper.query(PayBill.ENTITY_NAME, queryReCount);
        int recordCount = Integer.valueOf(payCount.get(0).get("count").toString());
        //总条数按500 分组返回页数集合
        int pageSize = 500;
        int queryCount = recordCount/pageSize <= 0? 1 : (recordCount/pageSize) +
                (recordCount%pageSize ==0?0:1);
        List<Integer> pageIndex = new ArrayList<>();
        for(int i=1;i<=queryCount;i++){
            pageIndex.add(i);
        }
        AtomicReference<ExecutorService> executorPayService = new AtomicReference<>();
        executorPayService.set(buildThreadPoolForFiMigrade("cmp-migradePayToFunPayMentForSystem-async-"));;
        ThreadPoolUtil.executeByBatch(executorPayService.get(),pageIndex,5,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D9", "付款工作台迁移适配平台") /* "付款工作台迁移适配平台" */,(int fromIndex, int toIndex)->{
            for(int t = fromIndex ; t < toIndex; t++){
                migradePayToFunPayMentExcute(pageIndex.get(t), pageSize,allTransTypeMap);
            }
            return "";
        });
        //插入数据后处理在途数据 需要删账 改余额(使用sql处理 不需要分批)
        deleteJournalAndChangeBal("cmp_paybill");
    }

    @Override
    public CtmJSONObject migradePayResult() throws Exception {
        /**
         * 拼接数据提示信息
         * 数据来源-应付管理，共XXX笔，升级成功XXX笔
         * 数据来源-现金管理，未审批单据共XXX笔，升级成功XXX笔
         * 数据来源-现金管理，结算完成单据共XXX笔，升级成功XXX笔
         */
        CtmJSONObject result = buildResult(PayBill.ENTITY_NAME);
        return result;
    }

    //构建返回提示信息
    public CtmJSONObject buildResult(String schema_fullname) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        /**
         * 数据来源-应付管理，共XXX笔，升级成功XXX笔
         * 数据来源-现金管理，未审批单据共XXX笔，升级成功XXX笔
         * 数据来源-现金管理，结算完成单据共XXX笔，升级成功XXX笔
         */
        //查询处理后的数据 进行相关提示 为避免数据量过大 直接分批次擦查询数量
        StringBuffer string = new StringBuffer();
        //数据来源-应付管理
//        QueryConditionGroup count_ManualCon =new QueryConditionGroup();
//        count_ManualCon.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Manual.getValue()));
//        count_ManualCon.appendCondition(QueryCondition.name("auditstatus").not_eq(AuditStatus.Complete.getValue()));
//        int count_Manual = queryCountByCondition(schema_fullname,count_ManualCon);
//
//        QueryConditionGroup count_ManualCon_Success =new QueryConditionGroup();
//        count_ManualCon_Success.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Manual.getValue()));
//        count_ManualCon_Success.appendCondition(QueryCondition.name("auditstatus").not_eq(AuditStatus.Complete.getValue()));
//        count_ManualCon_Success.addCondition(QueryCondition.name("migrateflag").eq(true));
//        int count_Manual_Success = queryCountByCondition(schema_fullname,count_ManualCon_Success);

        //数据来源-现金管理 未审批完成单据
        QueryConditionGroup count_Cash_UnAuditCon =new QueryConditionGroup();
        count_Cash_UnAuditCon.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        count_Cash_UnAuditCon.appendCondition(QueryCondition.name("auditstatus").not_eq(AuditStatus.Complete.getValue()));
        int count_Cash_UnAudit = queryCountByCondition(schema_fullname,count_Cash_UnAuditCon);

        QueryConditionGroup countSuccess_Cash_UnAuditCon =new QueryConditionGroup();
        countSuccess_Cash_UnAuditCon.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        countSuccess_Cash_UnAuditCon.appendCondition(QueryCondition.name("auditstatus").not_eq(AuditStatus.Complete.getValue()));
        countSuccess_Cash_UnAuditCon.appendCondition(QueryCondition.name("migrateflag").eq(true));
        int countSuccess_Cash_UnAudit = queryCountByCondition(schema_fullname,countSuccess_Cash_UnAuditCon);

        //数据来源-现金管理，结算完成单据
        QueryConditionGroup count_Cash_AuditCon = new QueryConditionGroup();
        count_Cash_AuditCon.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        count_Cash_AuditCon.appendCondition(QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()));
        count_Cash_AuditCon.appendCondition(QueryCondition.name("settlestatus").eq(SettleStatus.alreadySettled.getValue()));
        int count_Cash_Audit = queryCountByCondition(schema_fullname,count_Cash_AuditCon);

        QueryConditionGroup countSuccess_Cash_AuditCon = new QueryConditionGroup();
        countSuccess_Cash_AuditCon.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        countSuccess_Cash_AuditCon.appendCondition(QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()));
        countSuccess_Cash_AuditCon.appendCondition(QueryCondition.name("settlestatus").eq(SettleStatus.alreadySettled.getValue()));
        countSuccess_Cash_AuditCon.appendCondition(QueryCondition.name("migrateflag").eq(true));
        int countSuccess_Cash_Audit = queryCountByCondition(schema_fullname,countSuccess_Cash_AuditCon);

        //数据来源-现金管理,已审批待结算
        QueryConditionGroup auditAndUnSettle = new QueryConditionGroup();
        auditAndUnSettle.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        auditAndUnSettle.appendCondition(QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()));
        auditAndUnSettle.appendCondition(QueryCondition.name("settlestatus").eq(SettleStatus.noSettlement.getValue()));
        int count_auditAndUnSettle = queryCountByCondition(schema_fullname,auditAndUnSettle);

        QueryConditionGroup auditAndUnSettle_Success = new QueryConditionGroup();
        auditAndUnSettle_Success.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        auditAndUnSettle_Success.appendCondition(QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()));
        auditAndUnSettle_Success.appendCondition(QueryCondition.name("settlestatus").eq(SettleStatus.noSettlement.getValue()));
        auditAndUnSettle_Success.appendCondition(QueryCondition.name("migrateflag").eq(true));
        int successCount_auditAndUnSettle = queryCountByCondition(schema_fullname,auditAndUnSettle_Success);

        //数据来源-现金管理,本期结算完成凭证未生成
        Date periodBeginDate = cmpNewFiMigradeUtilService.periodBeginDate(PayBill.ENTITY_NAME);
        QueryConditionGroup settleAndUnVoucher = new QueryConditionGroup();
        settleAndUnVoucher.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        settleAndUnVoucher.appendCondition(QueryCondition.name("settlestatus").eq(SettleStatus.alreadySettled.getValue()));
        String[] vouchSatus = new String[]{"2", "3"};
        settleAndUnVoucher.appendCondition(QueryCondition.name("voucherstatus").in(vouchSatus));
        settleAndUnVoucher.appendCondition(QueryCondition.name("vouchdate").egt(periodBeginDate));
        int count_settleAndUnVoucher = queryCountByCondition(schema_fullname,settleAndUnVoucher);

        QueryConditionGroup settleAndUnVoucher_Success = new QueryConditionGroup();
        settleAndUnVoucher_Success.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        settleAndUnVoucher_Success.appendCondition(QueryCondition.name("settlestatus").eq(SettleStatus.alreadySettled.getValue()));
        settleAndUnVoucher_Success.appendCondition(QueryCondition.name("voucherstatus").in(vouchSatus));
        settleAndUnVoucher_Success.appendCondition(QueryCondition.name("vouchdate").egt(periodBeginDate));
        settleAndUnVoucher_Success.appendCondition(QueryCondition.name("migrateflag").eq(true));
        int countSuccess_settleAndUnVoucher = queryCountByCondition(schema_fullname,settleAndUnVoucher_Success);

        string.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E9", "数据来源-现金管理,未审批单据共") /* "数据来源-现金管理,未审批单据共" */+count_Cash_UnAudit+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EA", "笔,升级成功") /* "笔,升级成功" */+countSuccess_Cash_UnAudit+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E8", "笔。\n") /* "笔。\n" */);

        count_Cash_Audit = count_Cash_Audit-count_settleAndUnVoucher;
        countSuccess_Cash_Audit = countSuccess_Cash_Audit - countSuccess_settleAndUnVoucher;
        string.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EC", "数据来源-现金管理,结算完成单据共") /* "数据来源-现金管理,结算完成单据共" */+count_Cash_Audit+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EA", "笔,升级成功") /* "笔,升级成功" */+countSuccess_Cash_Audit+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E8", "笔。\n") /* "笔。\n" */);

        string.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EE", "数据来源-现金管理,已审批未结算的数据共") /* "数据来源-现金管理,已审批未结算的数据共" */+count_auditAndUnSettle+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EA", "笔,升级成功") /* "笔,升级成功" */+successCount_auditAndUnSettle+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E8", "笔。\n") /* "笔。\n" */);
        string.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EF", "数据来源-现金管理,本期结算完成凭证未生成单据共") /* "数据来源-现金管理,本期结算完成凭证未生成单据共" */+count_settleAndUnVoucher+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EA", "笔,升级成功") /* "笔,升级成功" */+countSuccess_settleAndUnVoucher+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E8", "笔。\n") /* "笔。\n" */);

        result.put("message",string);
        return result;
    }

    //根据条件查询相关数据数量
    public int queryCountByCondition(String schema_fullname,QueryConditionGroup condition) throws Exception {
        List<Map<String, Object>> billCount = new ArrayList<>();
        QuerySchema schema = QuerySchema.create().addSelect("count(id)");
        schema.addCondition(condition);
        billCount = MetaDaoHelper.query(schema_fullname, schema);
        int count = Integer.valueOf(billCount.get(0).get("count").toString());
        return count;
    }


    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = {Exception.class})
    public void deleteJournalAndChangeBal(String dbschema_table) throws Exception {
        /**
         * 1.查询来源为现金 且 审批状态不为完成的数据
         * 2.查询来源为应收应付 且审批状态为未审批的未审批的
         */
        //进行余额计算 分组查询每个账户 每个币种的 借、贷金额
        Map param = new HashMap();
        String ytenantId = AppContext.getYTenantId();
        param.put("ytenantId",ytenantId);
        param.put("dbschema_table",dbschema_table);
        List<Map<String, Object>> journalList = SqlHelper.selectList(CMPNEWFIMIGRADEMAPPER + "getJournalSum", param);
        Map<String,Journal> jMap= new HashMap<>();
        if(journalList!=null && journalList.size()>0){
            for (Map<String, Object> journalMap : journalList) {
                Journal journal = new Journal();
                journal.init(journalMap);
                jMap.put((journal.getBankaccount()+ journal.getCashaccount()+journal.getCurrency()).replace("null", ""),journal);
            }
            //回退余额 加贷 减借
            //查询相关账户期初信息
            QuerySchema querySchema_initdata = QuerySchema.create().addSelect("*");
            List<InitData> querys_initData = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, querySchema_initdata,null);
            Map<String , InitData> initMap = new HashMap();
            for(InitData init : querys_initData){
                initMap.put((init.getBankaccount()+init.getCashaccount()+init.getCurrency()).replace("null", ""),init);
            }
            //进行余额回退操作
            List<InitData> updateInitList = new ArrayList<>();
            for(String initKey : initMap.keySet()){
                    Journal sumJ = jMap.get(initKey);
                    InitData updateInit = initMap.get(initKey);
                    if(sumJ!=null && updateInit!=null){
                        // 余额回退计算 加贷 减借
                        updateInit.setCobookoribalance(BigDecimalUtils.safeSubtract(updateInit.getCobookoribalance(), sumJ.getDebitoriSum()));
                        updateInit.setCobooklocalbalance(BigDecimalUtils.safeSubtract(updateInit.getCobooklocalbalance(), sumJ.getDebitnatSum()));
                        updateInit.setCobookoribalance(BigDecimalUtils.safeAdd(updateInit.getCobookoribalance(), sumJ.getCreditoriSum()));
                        updateInit.setCobooklocalbalance(BigDecimalUtils.safeAdd(updateInit.getCobooklocalbalance(), sumJ.getCreditnatSum()));

                        updateInit.setEntityStatus(EntityStatus.Update);
                        updateInitList.add(updateInit);
                    }
            }
            if(!updateInitList.isEmpty()){
                    MetaDaoHelper.update(InitData.ENTITY_NAME,updateInitList);
            }
            //日记账删除 通过删除srcbillitemid
            SqlHelper.delete(CMPNEWFIMIGRADEMAPPER + "delMigradeJournal",param);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = {Exception.class})
    public void migradePayToFunPayMentExcute(int pageIndex,int pageSize,Map<String,Map <String , BdTransType>> allTransTypeMap) throws Exception {
        //分批次查询 付款单（500条一批）
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
        schema.addCondition(conditionGroup);
        schema.addOrderBy(new QueryOrderby("id","asc"));
        schema.addPager(pageIndex,pageSize);
        //查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("PayBillb").addSelect("*");
        schema.addCompositionSchema(detailSchema);
        List<PayBill> payBillVos = MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, schema,null);

        //汇总客户、供应商、人员信息
        Set<Long> customerSet = new HashSet<>();
        Set<Long> supplierSet = new HashSet<>();
        Set<String> employeeSet = new HashSet<>();
        //汇总客户、供应商、人员的 账户信息
        Set<Long> customerbankaccountSet = new HashSet<>();
        Set<Long> supplierbankaccountSet = new HashSet<>();
        Set<String> employeeankAccountSet = new HashSet<>();
        //汇总银行id信息
        Set<String> bankDtoSet = new HashSet<>();

        for(PayBill payBillvo : payBillVos){
            if(payBillvo.getCaobject().getValue() == CaObject.Customer.getValue() ){
                //客户
                if(payBillvo.getCustomer()!=null){
                    customerSet.add(payBillvo.getCustomer());
                }
                if(payBillvo.getCustomerbankaccount()!=null){
                    customerbankaccountSet.add(payBillvo.getCustomerbankaccount());
                }

            }else if(payBillvo.getCaobject().getValue() == CaObject.Supplier.getValue()){
                //供应商
                if(payBillvo.getSupplier()!=null){
                    supplierSet.add(payBillvo.getSupplier());
                }
                if(payBillvo.getSupplierbankaccount()!=null){
                    supplierbankaccountSet.add(payBillvo.getSupplierbankaccount());
                }
            }else if(payBillvo.getCaobject().getValue() == CaObject.Employee.getValue()){
                //人员
                if(payBillvo.getEmployee()!=null){
                    employeeSet.add(payBillvo.getEmployee());
                }
                if(payBillvo.getStaffBankAccount()!=null){
                    employeeankAccountSet.add(payBillvo.getStaffBankAccount());
                }

            }
        }
        //批量查询客户、供应商、人员   批量查询相关账户(银行)信息
        //存储客户的缓存数据
        Map<Long,MerchantDTO> customerMap = new HashMap<>();
        //存储客户银行账户的缓存数据
        Map<Long,AgentFinancialDTO> agentFinancialDTOMap = new HashMap<>();
        //供应商信息缓存
        Map<Long,VendorVO> supplierMap = new HashMap<>();
        //供应商账户信息缓存
        Map<Long,VendorBankVO> supplierBankVoMap = new HashMap<>();
        //缓存人员信息
        Map<String,Map<String, Object>> employeeMap = new HashMap<>();
        //缓存人员银行账户信息
        Map<String,Map<String, Object>> employeeBankMap = new HashMap<>();

        //统一查询对应银行
        Map<String,BankdotVO> bankdotNameMap = new HashMap<>();
        //统一查询银行网点


        try{
            //存储客户的缓存数据
            customerMap = cmpNewFiMigradeUtilService.buildCustomerMap(new ArrayList<>(customerSet));
            //存储客户银行账户的缓存数据
            agentFinancialDTOMap = cmpNewFiMigradeUtilService.buildCustomerBankMap(new ArrayList<>(customerbankaccountSet));
            if(!agentFinancialDTOMap.isEmpty()){
                for(AgentFinancialDTO vo : agentFinancialDTOMap.values()){
                    bankDtoSet.add(vo.getOpenBank());
                }
            }
            //供应商
            //供应商信息缓存
            supplierMap = cmpNewFiMigradeUtilService.buildSupplierMap(new ArrayList<>(supplierSet));
            //供应商账户信息缓存
            supplierBankVoMap = cmpNewFiMigradeUtilService.buildsupplierBankMap(new ArrayList<>(supplierbankaccountSet));
            if(!supplierBankVoMap.isEmpty()){
                for(VendorBankVO vo : supplierBankVoMap.values()){
                    bankDtoSet.add(vo.getOpenaccountbank());
                }
            }

            //人员 queryStaffByIds
            //缓存人员信息
            employeeMap = cmpNewFiMigradeUtilService.buildEmployeeMap(new ArrayList<>(employeeSet));
            //缓存人员银行账户信息
            employeeBankMap = cmpNewFiMigradeUtilService.bulidEmployeeBankMap(new ArrayList<>(employeeankAccountSet));
            if(!employeeBankMap.isEmpty()){
                for(Map<String, Object> map : employeeBankMap.values()){
                    bankDtoSet.add(map.get("bankname").toString() );
                }
            }
            //统一查询对应银行
            bankdotNameMap = cmpNewFiMigradeUtilService.bankdotMap(new ArrayList<>(bankDtoSet));

        }catch(Exception e){
            log.error("migradePayToFunPayMentExcute-queryCaobjectInfo",e);
        }

        //数据vo转化
        List<FundPayment> fundPaymentVos = new ArrayList<FundPayment>();
        List<PayBill> updatePaymentVos = new ArrayList<PayBill>();
        for(PayBill payBillvo : payBillVos){
            if(payBillvo.getMigrateflag()!=null){
                continue;
            }
            FundPayment fpVo = changePayVOtoFundPaymentVo(payBillvo,customerMap,agentFinancialDTOMap,supplierMap,supplierBankVoMap,employeeMap,employeeBankMap,bankdotNameMap,allTransTypeMap);
            fpVo.setEntityStatus(EntityStatus.Insert);
            fundPaymentVos.add(fpVo);
            payBillvo.setMigrateflag(true);
            payBillvo.setEntityStatus(EntityStatus.Update);
            updatePaymentVos.add(payBillvo);
        }
        try{
            if(!updatePaymentVos.isEmpty()){
                //数据更新
                MetaDaoHelper.update(PayBill.ENTITY_NAME,updatePaymentVos);
                //数据插入
                CmpMetaDaoHelper.insert(FundPayment.ENTITY_NAME,fundPaymentVos);
            }
        }catch (Exception e){
            log.error("migradePayToFunPayMentExcute-insert",e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = {Exception.class})
    public void migradePayPushSettle(String fullName,String fullName_b) throws Exception {
        //筛选出已经升级 且 审批完成 且未结算的数据 推升结算
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("migradeid").is_not_null());
        conditionGroup.appendCondition(QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue()));
//        conditionGroup.appendCondition(QueryCondition.name("settleSuccessTime").is_null());
        schema.addCondition(conditionGroup);
        //查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name(fullName_b).addSelect("*");
        QueryConditionGroup conditiondetailGroup = new QueryConditionGroup(ConditionOperator.and);
//        conditiondetailGroup.appendCondition(QueryCondition.name("settlestatus").eq(FundSettleStatus.WaitSettle.getValue()));
        conditiondetailGroup.appendCondition(QueryCondition.name("transNumber").is_null());//生成结算后 transNumber有值
        detailSchema.addCondition(conditiondetailGroup);
        schema.addCompositionSchema(detailSchema);
        List<BizObject> bizObjectVos = MetaDaoHelper.queryObject(fullName, schema,null);
        b1:for(BizObject bizBill : bizObjectVos){
            if(bizBill.get(fullName_b)!=null){
                continue b1;
            }
            //判断资金付款子表的transNumber 有值的过滤掉(子表拼接不上条件 故而用此方案)
            if(fullName_b.equals(ICmpConstant.FUND_PAYMENT_B)){
                b2:for(FundPayment_b fundPaymentBody:bizBill.getBizObjects(fullName_b,FundPayment_b.class)){
                    if(fundPaymentBody.getTransNumber()!=null){
                        continue b1;
                    }
                }
            }else{
                b2:for(FundCollection_b fundCollectionBody:bizBill.getBizObjects(fullName_b,FundCollection_b.class)){
                    if(fundCollectionBody.getTransNumber()!=null){
                        continue b1;
                    }
                }
            }
            List<BizObject> currentBillList = new ArrayList<>();
            currentBillList.add(bizBill);
            // 推送资金结算
            if(fullName.equals(FundPayment.ENTITY_NAME)){
                stwbPayBillService.pushBill(currentBillList, false);
            }else if(fullName.equals(FundCollection.ENTITY_NAME)){
                stwbCollectionBillService.pushBill(currentBillList, false);
            }
            //更新该币种银行结息账号预提规则的上次结息结束日
            List<BizObject> bizBillBList = bizBill.get(fullName_b);
            fundCommonService.updateWithholdingRuleSettingLastInterestSettlementDate(bizBillBList, ICmpConstant.CONSTANT_TWO);
            //凭证处理
            cmpNewFiMigradeUtilService.afterPushSettleVoucher(bizBill,fullName_b);
        }
    }

    //构建资金付款vo
    public FundPayment changePayVOtoFundPaymentVo(PayBill payBillvo,Map<Long,MerchantDTO> customerMap,Map<Long,AgentFinancialDTO> agentFinancialDTOMap
                                                    ,Map<Long,VendorVO> supplierMap,Map<Long,VendorBankVO> supplierBankVoMap
                                                    ,Map<String,Map<String, Object>> employeeMap,Map<String,Map<String, Object>> employeeBankMap,Map<String,BankdotVO> bankdotNameMap
                                                    ,Map<String,Map <String , BdTransType>> allTransTypeMap) throws Exception {
        FundPayment fpVo = buildFundPayment(payBillvo,allTransTypeMap);
        List<FundPayment_b> fundPayment_bs = new ArrayList<>();
        List<PayBillb> payBillbs =payBillvo.PayBillb();
        try{
            for(PayBillb payBillb: payBillbs){
                FundPayment_b fPbVo = buildFundPaymentBody(payBillvo,payBillb,customerMap,agentFinancialDTOMap,supplierMap,supplierBankVoMap,employeeMap,employeeBankMap,bankdotNameMap);
                fPbVo.setEntityStatus(EntityStatus.Insert);
                fundPayment_bs.add(fPbVo);
            }
        }catch (Exception e){
            log.error("changePayVOtoFundPaymentVo",e);
        }

        fpVo.setFundPayment_b(fundPayment_bs);
        return fpVo;
    }

    //构建资金付款表头
    public FundPayment buildFundPayment(PayBill payBillvo,Map<String,Map <String , BdTransType>> allTransTypeMap) throws Exception {
        FundPayment fpVo = new FundPayment();
        fpVo.setAccentity(payBillvo.getAccentity());
        fpVo.setSrcitem(payBillvo.getSrcitem());
        fpVo.setBilltype(EventType.FundPayment);
        fpVo.set("ebiz_obj_type","cmp.fundpayment.FundPayment");
        fpVo.setOrg(payBillvo.getOrg());
        fpVo.setTradetype((cmpNewFiMigradeUtilService.queryBdTransType(payBillvo.getTradetype(),allTransTypeMap)));
        fpVo.setSettlemode(payBillvo.getSettlemode());
        //未审批时推保存态
        if(payBillvo.getAuditstatus() == AuditStatus.Incomplete){
            fpVo.setAuditstatus(AuditStatus.Incomplete);
            fpVo.setAuditor(null);
            fpVo.setAuditorId(null);
            fpVo.setAuditTime(null);
            fpVo.setAuditDate(null);
            fpVo.setStatus(Short.valueOf("0"));
            fpVo.setSettleflag(Short.valueOf("1"));
            fpVo.setVerifystate((short)0);//初始开立
        }else{
            fpVo.setAuditstatus(payBillvo.getAuditstatus());
            fpVo.setAuditor(payBillvo.getAuditor());
            fpVo.setAuditorId(payBillvo.getAuditorId());
            fpVo.setAuditTime(payBillvo.getAuditTime());
            fpVo.setAuditDate(payBillvo.getAuditDate());
            fpVo.setStatus(payBillvo.getStatus().getValue());
            fpVo.setSettleSuccessPost(1);
            fpVo.setVerifystate(payBillvo.getVerifystate());
            if(payBillvo.getAuditstatus() == AuditStatus.Complete && payBillvo.getSettlestatus().getValue()!=SettleStatus.alreadySettled.getValue()){
                //审批完成 且 未结算推审批完成态 后续需要推升结算
                fpVo.setSettleflag(Short.valueOf("1"));
            }else if(payBillvo.getSettlestatus().getValue()==SettleStatus.alreadySettled.getValue()){
                //结算完成推结算完成态 单据流程结束
                //如果是已经结算成功的数据 过来settleflag(是否传资金结算需要为false) 这样就不会在过账时推结算
                fpVo.setSettleflag(Short.valueOf("0"));
            }
        }
        fpVo.setIsWfControlled(payBillvo.getIsWfControlled());
        fpVo.setCaobject(payBillvo.getCaobject());
        fpVo.setExchangeRateType(payBillvo.getExchangeRateType());
        fpVo.setEnterprisebankaccount(payBillvo.getEnterprisebankaccount());
        fpVo.setCashaccount(payBillvo.getCashaccount());
        fpVo.setCustomer(payBillvo.getCustomer());
        fpVo.setSupplier(payBillvo.getSupplier());
        fpVo.setEmployee(payBillvo.getEmployee());
        fpVo.setDept(payBillvo.getDept());
        fpVo.setOperator(payBillvo.getOperator());
        fpVo.setProject(payBillvo.getProject());
        fpVo.setExpenseitem(payBillvo.getExpenseitem());
        fpVo.setOriSum(payBillvo.getOriSum());
        fpVo.setNatSum(payBillvo.getNatSum());
        fpVo.setDescription(payBillvo.getDescription());
        fpVo.setSrcbillid(payBillvo.getSrcbillid());
        fpVo.setSrcbillno(payBillvo.getTopsrcbillno());
        fpVo.setReturncount(payBillvo.getReturncount());
        fpVo.setSettleSuccessTime(payBillvo.getSettledate());
        fpVo.setCreateTime(payBillvo.getCreateTime());
        fpVo.setCreateDate(payBillvo.getCreateDate());
        fpVo.setModifyTime(payBillvo.getModifyTime());
        fpVo.setModifyDate(payBillvo.getModifyDate());
        fpVo.setCreator(payBillvo.getCreator());
        fpVo.setModifier(payBillvo.getModifier());
        fpVo.setCreatorId(payBillvo.getCreatorId());
        fpVo.setModifierId(payBillvo.getModifierId());
        fpVo.setTenant(payBillvo.getTenant());
//        fpVo.setYTenant(payBillvo.getYTenant());

        fpVo.setNatCurrency(payBillvo.getNatCurrency());
        fpVo.setCurrency(payBillvo.getCurrency());
        fpVo.setExchRate(payBillvo.getExchRate());
        fpVo.setPrintCount(payBillvo.getPrintCount());
        fpVo.setVouchdate(payBillvo.getVouchdate());
        fpVo.setTplid(payBillvo.getTplid());

        fpVo.setCode(payBillvo.getCode());
        fpVo.setId(payBillvo.getId());
        fpVo.setPubts(payBillvo.getPubts());
        fpVo.setMigradeid(payBillvo.getId().toString());
        //已结算未生成凭证的  升级后一律过账中
        if(payBillvo.getSettlestatus().getValue()==SettleStatus.alreadySettled.getValue()
        && payBillvo.getVoucherstatus().getValue()!=VoucherStatus.Created.getValue()
        && payBillvo.getVoucherstatus().getValue()!=VoucherStatus.NONCreate.getValue()){
            fpVo.setVoucherstatus(VoucherStatus.POSTING);
        }else if(  payBillvo.getSettlestatus().getValue()==SettleStatus.alreadySettled.getValue()
                && payBillvo.getVoucherstatus().getValue()==VoucherStatus.NONCreate.getValue()){
                //已结算凭证状态为不生成 升级后为不生成
            fpVo.setVoucherstatus(VoucherStatus.NONCreate);
        }else
            fpVo.setVoucherstatus(payBillvo.getVoucherstatus());
        fpVo.setVoucherNo(payBillvo.getVoucherNo());
        //入账类型 正常入账
        fpVo.setEntrytype((short)1);
        //特征
        fpVo.setCharacterDef(payBillvo.getPayBillCharacterDef());
        return fpVo;
    }

    //构建资金付款表体
    public FundPayment_b buildFundPaymentBody(PayBill payBillvo,PayBillb payBillb,
                                              Map<Long,MerchantDTO> customerMap,Map<Long,AgentFinancialDTO> agentFinancialDTOMap
                                             ,Map<Long,VendorVO> supplierMap,Map<Long,VendorBankVO> supplierBankVoMap
                                             ,Map<String,Map<String, Object>> employeeMap,Map<String,Map<String, Object>> employeeBankMap,Map<String,BankdotVO> bankdotNameMap) throws Exception {
        FundPayment_b fPbVo = new FundPayment_b();
        fPbVo.setMainid(String.valueOf(payBillb.getMainid()));
        fPbVo.set("ebiz_obj_type","cmp.fundpayment.FundPayment_b");
        fPbVo.setQuickType(payBillb.getQuickType());
        fPbVo.setOriSum(payBillb.getOriSum());
        fPbVo.setNatSum(payBillb.getNatSum());
        fPbVo.setDzdate(payBillvo.getDzdate());
        fPbVo.setCurrency(payBillvo.getCurrency());
        fPbVo.setNatCurrency(payBillvo.getNatCurrency());
        fPbVo.setSettlemode(payBillvo.getSettlemode());
        fPbVo.setFundSettlestatus(changeSettlestatusFromReToFc(payBillvo.getSettlestatus()));
        fPbVo.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fPbVo.getFundSettlestatus()));
        fPbVo.setEnterprisebankaccount(payBillvo.getEnterprisebankaccount());
        fPbVo.setCashaccount(payBillvo.getCashaccount());
        fPbVo.setCaobject(payBillvo.getCaobject());
        //根据收付款对象类型 进行收款单位名称赋值
        if(payBillvo.getCaobject()!=null){
            queryCaobjectInfo( fPbVo, payBillvo, payBillb,customerMap,agentFinancialDTOMap,supplierMap,supplierBankVoMap,employeeMap,employeeBankMap,bankdotNameMap);
        }
        fPbVo.setExchRate(payBillvo.getExchRate());
        fPbVo.setExchangeRateType(payBillvo.getExchangeRateType());
        fPbVo.setProject(payBillb.getProject());
        fPbVo.setExpenseitem(payBillb.getExpenseitem());
        fPbVo.setDept(payBillb.getDept());
        fPbVo.setOperator(payBillb.getOperator());
        fPbVo.setTaxCategory(payBillb.getTaxCategory());
        fPbVo.setTaxRate(payBillb.getTaxRate());
        fPbVo.setUnTaxSum(payBillb.getUnTaxSum());
        fPbVo.setIncludeTaxSum(payBillb.getIncludeTaxSum());
        fPbVo.setDescription(payBillb.getDescription());
        fPbVo.setSrcbillno(payBillb.getTopsrcbillno());
        fPbVo.setSrcbillitemno(payBillb.getSrcbillitemno());
        fPbVo.setTopsrcbillno(payBillb.getTopsrcbillno());
        fPbVo.setSrcbillid(payBillb.getSrcbillid());
        fPbVo.setPushsrcbillmid(payBillb.getPushsrcbillmid());
        fPbVo.setSourceid(payBillb.getSourceid());
        fPbVo.setSourceautoid(payBillb.getSourceautoid());
        fPbVo.setSource(payBillb.getSource());
        fPbVo.setUpcode(payBillb.getUpcode());
//        fPbVo.setMkeRuleCode(payBillb.setMakeRuleCode());
        fPbVo.setSourceMainPubts(payBillb.getSourceMainPubts());
        fPbVo.setGroupTaskKey(payBillb.getGroupTaskKey());
        fPbVo.setTenant(payBillb.getTenant());
//        fPbVo.setYTenant(payBillvo.getYTenant());
//        fPbVo.setRowno(payBillb.getRowno());
        fPbVo.setSettleSuccessTime(payBillvo.getSettledate());
        fPbVo.setId(payBillb.getId());
        fPbVo.setPubts(payBillb.getPubts());
        //对公对私
        fPbVo.setPublicPrivate(payBillvo.getRetailerAccountType()!=null?String.valueOf(payBillvo.getRetailerAccountType().getValue()):null);
        //结算成功金额
        if(payBillvo.getSettlestatus() == SettleStatus.alreadySettled){
            fPbVo.setSettlesuccessSum(payBillb.getOriSum());
        }
        //特征
        fPbVo.setCharacterDefb(payBillb.getPayBillCharacterDefb());

        //20240710补充 资金付换汇相关字段
        fPbVo.setSettleCurrency(payBillvo.getCurrency());//结算币种
        fPbVo.setSwapOutExchangeRateType(payBillvo.getExchangeRateType());//换出汇率类型
        fPbVo.setSwapOutExchangeRateEstimate(new BigDecimal(1));//换出汇率预估
        fPbVo.setSwapOutAmountEstimate(payBillb.getOriSum());//换出金额预估

        return fPbVo;
    }

    //根据收付款单据 查询对应 资金收付的交易类型
    public String queryTradetype(String tradetypeId,Boolean isPay) throws Exception {
        String codeNew = "";
        String bdTransTypeId = null;
        //先从缓存取
        bdTransTypeId = tradetypeCache.getIfPresent(tradetypeId);
        //如果没有进行查询
        if(bdTransTypeId==null){
            if(tradetypeId!= null){
                BdTransType bdTransType= baseRefRpcService.queryTransTypeById(tradetypeId);
                if(bdTransType!=null){
                    //如果是默认交易类型进行转换
                    String codeOld = bdTransType.getCode();
                    String memo = bdTransType.getMemo();
                    if(isPay){//付款工作台
                        if("arap_payment_other".equals(codeOld)){//其他付款
                            codeNew = "cmp_fund_payment_other";
                        }else if("arap_payment_purchase".equals(codeOld)){//采购付款
                            codeNew = "cmp_fund_payment_purchase";
                        }else if("arap_payment_reimburse".equals(codeOld)){//报销付款
                            codeNew = "cmp_fund_payment_reimburse";
                        }else if("arap_payment_sale".equals(codeOld)){//销售退款
                            codeNew = "cmp_fund_payment_sale";
                        }else if(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004DA", "其他付款") /* "其他付款" */.equals(memo)){//其他付款
                            codeNew = "cmp_fund_payment_other";
                        }else if(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004DC", "采购付款") /* "采购付款" */.equals(memo)){//采购付款
                            codeNew = "cmp_fund_payment_purchase";
                        }else if(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004DD", "报销付款") /* "报销付款" */.equals(memo)){//报销付款
                            codeNew = "cmp_fund_payment_reimburse";
                        }else if(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004DF", "销售退款") /* "销售退款" */.equals(memo)){//销售退款
                            codeNew = "cmp_fund_payment_sale";
                        }else//默认
                            codeNew = "";
                    }else{//收款工作台
                        switch (codeOld) {
                            case "arap_receipt_other": //其他收款
                                codeNew = "cmp_fundcollection_other";
                                break;
                            case "arap_receipt_purchase": //采购退款
                                codeNew = "cmp_fundcollection_purchase";
                                break;
                            case "arap_receipt_sale": //销售收款
                                codeNew = "cmp_fundcollection_sale";
                                break;
                            case "CM99": //杂项收款
                                codeNew = "cmp_fundcollection_general";
                                break;
                            default: //默认
                                codeNew = "";
                                break;
                        }
                    }
                }
            }else if(bdTransTypeId==null){//都不符合走默认
                if(isPay){
                    codeNew = "";
                }else
                    codeNew = "";
            }
            if(StringUtils.isNotEmpty(codeNew)){
                TranstypeQueryPageParam params = new TranstypeQueryPageParam();
                params.setTransTypeCode(codeNew);
                params.setTenantId(AppContext.getYTenantId().toString());
                List<BdTransType> bdTransTypes= baseRefRpcService.queryTransTypeByCondition(params);
                if(bdTransTypes!=null && bdTransTypes.size()>0){
                    bdTransTypeId = bdTransTypes.get(0).getId();
                    tradetypeCache.put(tradetypeId,bdTransTypeId);
                }
            }else{
                //如果不是默认交易类型 则直接赋值 不翻译 后续用脚本解决
                bdTransTypeId = tradetypeId;
                tradetypeCache.put(tradetypeId,bdTransTypeId);
            }

        }
        return bdTransTypeId;
    }

    /**
     * 针对不同的 收付对象类型 返回特定账号信息
     * @return
     */
    public void queryCaobjectInfo(FundPayment_b fPbVo,PayBill payBillvo,PayBillb payBillb,
                                  Map<Long,MerchantDTO> customerMap,Map<Long,AgentFinancialDTO> agentFinancialDTOMap
                                 ,Map<Long,VendorVO> supplierMap,Map<Long,VendorBankVO> supplierBankVoMap
                                 ,Map<String,Map<String, Object>> employeeMap,Map<String,Map<String, Object>> employeeBankMap,Map<String,BankdotVO> bankdotNameMap) throws Exception {
        CaObject caObject = payBillvo.getCaobject();
        if(caObject.getValue() == CaObject.Customer.getValue()){//客户
            if(payBillvo.getCustomer()!=null){
                fPbVo.setOppositeobjectname(customerMap.get(payBillvo.getCustomer())!=null?customerMap.get(payBillvo.getCustomer()).getName():null);//收款单位名称
                fPbVo.setOppositeobjectid(payBillvo.getCustomer().toString());//收款单位名称id
            }
            //查询客户银行账户
            if(payBillvo.getCustomerbankaccount()!=null){
                AgentFinancialDTO customerbankaccount = agentFinancialDTOMap.get(payBillvo.getCustomerbankaccount());
                if(customerbankaccount!=null){
                    fPbVo.setOppositeaccountid(payBillvo.getCustomerbankaccount().toString());//收款方账户id
                    fPbVo.setOppositeaccountno(customerbankaccount.getBankAccount());//收款方银行账号
                    fPbVo.setOppositeaccountname(customerbankaccount.getBankAccountName());//收款方账户名称
                    fPbVo.setOppositebankaddrid(customerbankaccount.getOpenBank());//收款方开户行id
                    //收款方银行联行号 联行号先取客商档案上的联行号
                    fPbVo.setOppositebanklineno(customerbankaccount.getJointLineNo());
                    if(customerbankaccount.getOpenBank()!=null){
                        BankdotVO bank = bankdotNameMap.get(customerbankaccount.getOpenBank());
                        if(bank!=null){
                            fPbVo.setOppositebankaddr(bank.getName());//收款方开户行名称
                            fPbVo.setOppositebankType(bank.getBankName());//收款方银行类别
//                            fPbVo.setOppositebanklineno(bank.getLinenumber());//收款方银行联行号
                        }
                    }
                }
            }
        }else if(caObject.getValue() == CaObject.Supplier.getValue()){//供应商
            if(payBillvo.getSupplier()!=null){
                VendorVO supplier = supplierMap.get(payBillvo.getSupplier());
                fPbVo.setOppositeobjectname(supplier!=null?supplier.getName():null);//收款单位名称
                fPbVo.setOppositeobjectid(payBillvo.getSupplier().toString());//收款单位名称id
            }
            if(payBillvo.getSupplierbankaccount()!=null){
                VendorBankVO supplierBankVo = supplierBankVoMap.get(payBillvo.getSupplierbankaccount());
                if(supplierBankVo!=null){
                    fPbVo.setOppositeaccountid(payBillvo.getSupplierbankaccount().toString());//收款方账户id
                    fPbVo.setOppositeaccountno(supplierBankVo.getAccount());//收款方银行账号
                    fPbVo.setOppositeaccountname(supplierBankVo.getAccountname());//收款方账户名称
                    fPbVo.setOppositebankaddrid(supplierBankVo.getOpenaccountbank());//收款方开户行id
                    fPbVo.setOppositebanklineno(supplierBankVo.getCorrespondentcode());//收款方银行联行号

                    if(supplierBankVo.getOpenaccountbank()!=null){//
                        BankdotVO bank = bankdotNameMap.get(supplierBankVo.getOpenaccountbank());
                        if(bank!=null){
                            fPbVo.setOppositebankaddr(bank.getName());//收款方开户行名称
                            fPbVo.setOppositebankType(bank.getBankName());//收款方银行类别
//                            fPbVo.setOppositebanklineno(bank.getLinenumber());//收款方银行联行号
                        }
                    }
                }
            }
        }else if(caObject.getValue() == CaObject.Employee.getValue()){//人员
            if(payBillvo.getEmployee()!=null){
                Map<String, Object> employee = employeeMap.get(payBillvo.getEmployee());
                fPbVo.setOppositeobjectname(employee!=null && employee.get("name")!=null?employee.get("name").toString():null);//收款单位名称
                fPbVo.setOppositeobjectid(payBillvo.getEmployee().toString());//收款单位名称id
            }
            if(payBillvo.getStaffBankAccount()!=null){
                Map<String, Object> employeeBank = employeeBankMap.get(payBillvo.getStaffBankAccount());
                if(employeeBank!=null){
                    fPbVo.setOppositeaccountid(payBillvo.getStaffBankAccount().toString());//收款方账户id
                    fPbVo.setOppositeaccountno(employeeBank.get("account")!=null?employeeBank.get("account").toString():null);//收款方银行账号
                    fPbVo.setOppositeaccountname(employeeBank.get("accountname")!=null?employeeBank.get("accountname").toString():null);//收款方账户名称
                    fPbVo.setOppositebankaddrid(employeeBank.get("bankname")!=null?employeeBank.get("bankname").toString():null);//收款方开户行id
                }
                if(employeeBank != null && employeeBank.get("bankname")!=null){
                    BankdotVO bank =bankdotNameMap.get(employeeBank.get("bankname").toString());
                    if(bank!=null){
                        fPbVo.setOppositebanklineno(bank.getLinenumber());//收款方银行联行号
                        fPbVo.setOppositebankaddr(bank.getName());//收款方开户行名称
                        fPbVo.setOppositebankType(bank.getBankName());//收款方银行类别
                    }
                }
            }
        }else if(caObject.getValue() == CaObject.Other.getValue()){//其他
            fPbVo.setOppositeobjectname(payBillvo.getRetailer());
            fPbVo.setOppositeobjectid(null);
            fPbVo.setOppositeaccountid(null);
            fPbVo.setOppositeaccountno(payBillvo.getRetailerAccountNo());
            fPbVo.setOppositeaccountname(payBillvo.getRetailerAccountName());
            fPbVo.setOppositebankaddrid(payBillvo.getRetailerBankDot());
            fPbVo.setOppositebankaddr(payBillvo.getRetailerBankName());
            fPbVo.setOppositebanklineno(payBillvo.getRetailerLineNumber());
            fPbVo.setOppositebankType(payBillvo.getRetailerBankType());
        }
    }


    @Override
    public CtmJSONObject migradeReToFundCollection(CtmJSONObject params) throws Exception {

        /**
         * 先进行数据升级校验 有特殊情况不允许升级
         * 1.有银企联支付的在途数据
         * 2.升级月份前的没有生成凭证的 已结算完成数据
         * 3.是否成功升级了交易类型
         */

        Map<String ,Boolean> checkMap = cmpNewFiMigradeUtilService.checkBeforeUpgrade(ReceiveBill.ENTITY_NAME);
        if(!checkMap.get("PayStatus")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E3", "存在网银支付状态不为支付成功的数据，不允许升级。") /* "存在网银支付状态不为支付成功的数据，不允许升级。" */);
        }else if(!checkMap.get("VouchSatus")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E4", "存在升级月份前，凭证状态不为已生成的数据，不允许升级。") /* "存在升级月份前，凭证状态不为已生成的数据，不允许升级。" */);
        }else if(!checkMap.get("CheckRecTransType")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004EB", "收款工作台的交易类型，没有完全同步到资金收款单，不允许升级。") /* "收款工作台的交易类型，没有完全同步到资金收款单，不允许升级。" */);
        }else if(!checkMap.get("CheckCmpBillApprove")){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004ED", "收款工作台存在审批过程中的单据，需要审批完成后才能升级。") /* "收款工作台存在审批过程中的单据，需要审批完成后才能升级。" */);
        }
        //查询交易类型集合
        Map<String,Map <String , BdTransType>> allTransTypeMap = cmpNewFiMigradeUtilService.getAllTransTypeMap();
        //查询需要迁移的总数据量
        QuerySchema queryReCount = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
//        conditionGroup.appendCondition(QueryCondition.name("migrateflag").is_null());
        queryReCount.addCondition(conditionGroup);
        List<Map<String, Object>> payCount = MetaDaoHelper.query(ReceiveBill.ENTITY_NAME, queryReCount);
        int recordCount = Integer.valueOf(payCount.get(0).get("count").toString());
        //总条数按500 分组返回页数集合
        int pageSize = 500;
        int queryCount = recordCount/pageSize <= 0? 1 : (recordCount/pageSize) +
                (recordCount%pageSize ==0?0:1);
        List<Integer> pageIndex = new ArrayList<>();
        for(int i=1;i<=queryCount;i++){
            pageIndex.add(i);
        }
        String uid = params.getString("uid");
        //构建进度条信息
        ProcessUtil.initProcess(uid,queryCount);
        //开启异步执行
        ExecutorService taskExecutor = null;
        taskExecutor =  ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"migradeReToFundCollection-threadpool");
        try{
            taskExecutor.submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(ICmpConstant.MIGRADERECEIVETOFUNDCOLLECTION,2*60*60L, TimeUnit.SECONDS,(int lockstatus)->{
                        if(lockstatus == LockStatus.GETLOCK_FAIL){
                            //加锁失败
                            //加锁失败添加报错信息 并把进度置为100%
                            ProcessUtil.addMessage(uid, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E1", "收款工作台数据正在迁移中") /* "收款工作台数据正在迁移中" */) /* "系统正在对此账户拉取中" */);
                            ProcessUtil.completed(uid);
                            return ;
                        }
                        //加锁成功 多批次处理 插入数据
                        ExecutorService executorService = buildThreadPoolForFiMigrade("cmp-migradeReToFundCollection-async-");
                        ThreadPoolUtil.executeByBatch(executorService,pageIndex,5,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E2", "收款工作台迁移") /* "收款工作台迁移" */,(int fromIndex, int toIndex)->{
                            for(int t = fromIndex ; t < toIndex; t++){
                                migradeReToFundCollectionExcute(pageIndex.get(t), pageSize, allTransTypeMap);
                                ProcessUtil.refreshProcess(uid,true);
                            }
                            return "";
                        });
                        //插入数据后处理在途数据 需要删账 改余额(此类数据较少 不需要分批)
                        deleteJournalAndChangeBal("cmp_receivebill");
                        //筛选出已经升级 且 审批完成 且未结算的数据 推升结算(此类数据较少 不需要分批)
                        migradePayPushSettle(FundCollection.ENTITY_NAME,ICmpConstant.FUND_COLLECTION_B);
                    });
                } catch (Exception e) {
                    log.error("migradeReToFundCollection", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101287"),e.getMessage());
                }finally {
                    ProcessUtil.completed(uid);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
        return null;
    }

    @Override
    public void migradeReToFundCollectionExcuteForSystem() throws Exception {
        //查询交易类型集合
        Map<String,Map <String , BdTransType>> allTransTypeMap = cmpNewFiMigradeUtilService.getAllTransTypeMap();
        //查询需要迁移的总数据量
        QuerySchema queryReCount = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
//        conditionGroup.appendCondition(QueryCondition.name("migrateflag").is_null());
        queryReCount.addCondition(conditionGroup);
        List<Map<String, Object>> payCount = MetaDaoHelper.query(ReceiveBill.ENTITY_NAME, queryReCount);
        int recordCount = Integer.valueOf(payCount.get(0).get("count").toString());
        //总条数按500 分组返回页数集合
        int pageSize = 500;
        int queryCount = recordCount/pageSize <= 0? 1 : (recordCount/pageSize) +
                (recordCount%pageSize ==0?0:1);
        List<Integer> pageIndex = new ArrayList<>();
        for(int i=1;i<=queryCount;i++){
            pageIndex.add(i);
        }
        //多批次处理 插入数据
        ExecutorService executorService = buildThreadPoolForFiMigrade("cmp-migradeReToFundCollectionForSystem-async-");
        ThreadPoolUtil.executeByBatch(executorService,pageIndex,5,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004DE", "收款工作台迁移适配平台") /* "收款工作台迁移适配平台" */,(int fromIndex, int toIndex)->{
            for(int t = fromIndex ; t < toIndex; t++){
                migradeReToFundCollectionExcute(pageIndex.get(t), pageSize, allTransTypeMap);
            }
            return "";
        });
        //插入数据后处理在途数据 需要删账 改余额(此类数据较少 不需要分批)
        deleteJournalAndChangeBal("cmp_receivebill");
    }

    @Override
    public CtmJSONObject migradeReResult() throws Exception {
        /**
         * 拼接数据提示信息
         * 数据来源-应付管理，共XXX笔，升级成功XXX笔
         * 数据来源-现金管理，未审批单据共XXX笔，升级成功XXX笔
         * 数据来源-现金管理，结算完成单据共XXX笔，升级成功XXX笔
         */
        CtmJSONObject result = buildResult(ReceiveBill.ENTITY_NAME);
        return result;
    }

    @Override
    public CtmJSONObject migradeUpdateTradetype(CtmJSONObject params) throws Exception {
        String ytenantid = InvocationInfoProxy.getTenantid();
        //获取当前租户期间启用
        String uid = params.getString("uid");
        //构建进度条信息（总数为一即可）
        ProcessUtil.initProcess(uid,1);
        //开启异步执行
        ExecutorService taskExecutor = null;
        taskExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,20,"migradeUpdateTradetype-threadpool");
        try{
            taskExecutor.submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(ICmpConstant.MIGRADEUPDATETRADETYPE,2*60*60L, TimeUnit.SECONDS,(int lockstatus)->{
                        if(lockstatus == LockStatus.GETLOCK_FAIL){
                            //加锁失败
                            //加锁失败添加报错信息 并把进度置为100%
                            ProcessUtil.addMessage(uid, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004F0", "交易类型升级正在进行中。") /* "交易类型升级正在进行中。" */);
                            //ProcessUtil.completed(uid);
                            return ;
                        }
                        //加锁成功
                        migradeUpdateTradetypeExcute(ytenantid);
                        ProcessUtil.refreshProcess(uid,true);
                    });
                } catch (Exception e) {
                    log.error("migradeUpdateTradetype", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101288"),e.getMessage());
                }finally {
                    ProcessUtil.completed(uid);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
        return  null;
    }

    @Override
    public void migradeUpdateTradetypeExcute(String ytenantid)throws Exception {
        /**
         * 步骤1：查询 收款、付款的旧有自定义交易类型
         */
        //付款 CM.cmp_payment
        List<BdTransType> bdTransTypes_cmp_paymentAll= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_payment");
        //自定义付款交易类型
        List<BdTransType> bdTransTypes_cmp_payment = new ArrayList<>();
        for(BdTransType payType : bdTransTypes_cmp_paymentAll){
            if(!payType.getPreset()){//获取非默认类型
                bdTransTypes_cmp_payment.add(payType);
            }
        }
        //收款 CM.cmp_receivebill
        List<BdTransType> bdTransTypes_cmp_receivebillAll= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_receivebill");
        //自定义收款交易类型
        List<BdTransType> bdTransTypes_cmp_receivebill = new ArrayList<>();
        for(BdTransType receiveType : bdTransTypes_cmp_receivebillAll){
            if(!receiveType.getPreset()){//获取非默认类型
                bdTransTypes_cmp_receivebill.add(receiveType);
            }
        }
        /**
         * 步骤2：组装资金收付交易类型
         */

        //资金付款 CM.cmp_fundpayment
        List<BdTransType> addBdTransTypes_cmp_fundpayments = new ArrayList<>();
        if(!bdTransTypes_cmp_payment.isEmpty()){
            //先查询一个资金收付的交易类型 获取sysId 和  billTypeId
            List<BdTransType> bdTransTypes_cmp_fundpayments= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_fundpayment");
            BdTransType bdTransTypes_cmp_fundpayment = bdTransTypes_cmp_fundpayments.get(0);
            String paySysId = bdTransTypes_cmp_fundpayment.getSysId();
            String payBillTypeId = bdTransTypes_cmp_fundpayment.getBillTypeId();
            //需要新增的资金付交易类型集合
            addBdTransTypes_cmp_fundpayments = buildAddBdTransType(bdTransTypes_cmp_payment, bdTransTypes_cmp_fundpayments,paySysId, payBillTypeId, ytenantid);
        }
        //资金收款 CM.cmp_fundcollection
        //需要新增的资金收交易类型集合
        List<BdTransType> addBdTransTypes_cmp_fundcollections = new ArrayList<>();
        if(!bdTransTypes_cmp_receivebill.isEmpty()){
            //先查询一个资金收付的交易类型 获取sysId 和  billTypeId
            List<BdTransType> bdTransTypes_cmp_fundcollections= iTransTypeService.getTransTypesByFormId(ytenantid, "CM.cmp_fundcollection");
            BdTransType bdTransTypes_cmp_fundcollection = bdTransTypes_cmp_fundcollections.get(0);
            String funSysId = bdTransTypes_cmp_fundcollection.getSysId();
            String funBillTypeId = bdTransTypes_cmp_fundcollection.getBillTypeId();
            //需要新增的资金付交易类型集合
            addBdTransTypes_cmp_fundcollections = buildAddBdTransType(bdTransTypes_cmp_receivebill,bdTransTypes_cmp_fundcollections, funSysId, funBillTypeId, ytenantid);
        }
        /**
         * 步骤3：新增操作 相关接口:void com.yonyou.ucf.transtype.service.itf.ITransTypeService#addTransType(BdTransType bdTransType)  throws TransTypeRpcException;
         * 平台没有批量新增 只能循环调用 如果数据过多将考虑异步暂停
         */
        if(!addBdTransTypes_cmp_fundpayments.isEmpty()){
            for(BdTransType addVo : addBdTransTypes_cmp_fundpayments){
                iTransTypeService.addTransType(addVo);
            }
        }
        if(!addBdTransTypes_cmp_fundcollections.isEmpty()){
            for(BdTransType addVo : addBdTransTypes_cmp_fundcollections){
                iTransTypeService.addTransType(addVo);
            }
        }
    }


    @Override
    public CtmJSONObject migradeUpdateCharacterDef(CtmJSONObject params) throws Exception {
        String uid = params.getString("uid");
        //构建进度条信息
        ProcessUtil.initProcess(uid,1);
        //开启异步执行
        ExecutorService taskExecutor = null;
        taskExecutor =  ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,20,"migradeUpdateCharacterDef-threadpool");
        try{
            taskExecutor.submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(ICmpConstant.MIGRADEUPDATECHARACTERDEF,2*60*60L, TimeUnit.SECONDS,(int lockstatus)->{
                        if(lockstatus == LockStatus.GETLOCK_FAIL){
                            //加锁失败
                            //加锁失败添加报错信息 并把进度置为100%
                            ProcessUtil.addMessage(uid, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004E1", "收款工作台数据正在迁移中") /* "收款工作台数据正在迁移中" */) /* "系统正在对此账户拉取中" */);
                            //ProcessUtil.completed(uid);
                            return ;
                        }
                        //加锁成功
                        migradeUpdateCharacterDefExcute(uid);
                        ProcessUtil.refreshProcess(uid,true);
                    });
                } catch (Exception e) {
                    log.error("migradeUpdateCharacterDef", e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101289"),e.getMessage());
                }finally {
                    ProcessUtil.completed(uid);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
        return null;
    }

    @Override
    public void migradeUpdateCharacterDefExcute(String uid) throws Exception {
        /**
         * 1.查询收付款工作台 特征
         */
        Map<String, List<String>> ret = queryOldBillCharMetaInfo();//Map的key为单据fullName，value为特征name的集合
        //没有特征不需要分配 直接返回
        if(ret.isEmpty() && uid!=null){
            ProcessUtil.completed(uid);
            return;
        }
        /**
         * 2.调用新增 给新单据添加特征
         *
         */
        //key为单据实体fullname ；value为当前单据实体fullname的任务id集合
        Map<String, List<String>> taskMap  = addCharMetaInfo(ret);
        /**
         * 3.根据taskId查询特征是否分配成功
         */
        Map<String,Boolean> taskIdBooleanMap= new HashMap<>();//key:taskId ; value:分配是否成功
        for(String key : taskMap.keySet()){
            taskIdBooleanMap.putAll(cmpNewFiMigradeUtilService.queryCharactersAssignResult(taskMap.get(key)));
        }
        /**
         * 4.如有失败的 重新分配（这里的taskId是按fullName分组的 所以最多会有4个taskId）
         */
        reAssignFailCharMetaInfo(taskIdBooleanMap,taskMap,ret);
    }

    private Map<String, List<String>> queryOldBillCharMetaInfo(){
        Map<String, List<String>> ret = new HashMap<>();
        for(String fullName : fullNameList){
            Entity entity = BizContext.getMetaRepository().entity(fullName);
            List<Property> eltAttrList = entity.getProperties(true);//特征组属性
            List<String> nameList = new ArrayList<>();
            for (Property tz : eltAttrList) {
                if (tz.getDBTableName() == null || !tz.getIsCharacter()) {
                    continue;
                }
                nameList.add(tz.name());
            }
            if(!CollectionUtils.isEmpty(nameList)){
                ret.put(fullNameMap.get(fullName), nameList);
            }
        }
        return ret;
    }



    /**
     * 分配新特征
     * @param ret Map的key为单据fullName，value为特征name的集合
     * @return
     */
    private Map<String, List<String>>  addCharMetaInfo(Map<String, List<String>> ret) throws Exception {
        Map<String, List<String>> taskMap =  new HashMap<>();//key为单据实体fullname ；value为当前单据实体fullname的任务id集合
        if(!ret.isEmpty()){
            for(String fullName : ret.keySet()){
                List<String> taskIdList = cmpNewFiMigradeUtilService.assignCharMetaInfo(ret.get(fullName),fullName);
                taskMap.put(fullName,taskIdList);
            }
        }
        return taskMap;
    }

    /**
     * 重新分配之前 分配失败的特征数据
     * @param taskIdBooleanMap key:taskId ; value:分配是否成功
     * @param taskMap          key为单据实体fullname ；value为当前单据实体fullname的任务id集合
     * @param ret              key为单据fullName，value为特征name的集合
     * @throws Exception
     */
    private void reAssignFailCharMetaInfo(Map<String,Boolean> taskIdBooleanMap,Map<String, List<String>> taskMap,Map<String, List<String>> ret) throws Exception {
        List<String> failTaskIdList = Lists.newArrayList();
        for(String taskId : taskIdBooleanMap.keySet()){
            //如果为false 说明分配不成功 需要记录
            if(!taskIdBooleanMap.get(taskId)){
                failTaskIdList.add(taskId);
            }
        }
        if(!CollectionUtils.isEmpty(failTaskIdList)){
            //根据之前的记录 确认当前失败的taskid 属于哪一个特征 哪一个实体 并组装数据
            Map<String, List<String>> reRet = new HashMap<>();//需要重新分配的数据集合
            for(String failTaskId:failTaskIdList){
                for(String fullName : taskMap.keySet()){
                    if(taskMap.get(fullName).contains(failTaskId)){//如果当前的taskMap集合中包含失败的taskId 说明该taskId属于此实体
                        reRet.put(fullName, ret.get(fullName));
                    }
                }
            }
            addCharMetaInfo(reRet);
        }
    }

    private List<BdTransType> buildAddBdTransType( List<BdTransType> oldBdTransTypeList,List<BdTransType> newBdTransTypeList,String sysId,String billTypeId,String ytenantid){
        List<BdTransType> addBdTransTypes = new ArrayList<>();
        //只对没有升级过的数据进行添加 如果有交易类型的code、name一致 则判定是重复数据 不进行新增
        b1:for(BdTransType oldType : oldBdTransTypeList){
           b2: for(BdTransType newType : newBdTransTypeList){
                if(oldType.getCode().equals(newType.getCode())&&
                        oldType.getName().equals(newType.getName())){
                    continue b1;
                }
            }
            BdTransType addType = new BdTransType();
            addType.setTenantId(ytenantid);
            addType.setBillTypeId(billTypeId);
            addType.setSysId(sysId);
            addType.setName(oldType.getName());
            addType.setCode(oldType.getCode());
            //默认
            addType.setDefault(oldType.getDefault());
            //是否发布web端
            addType.setIsPublish(oldType.getIsPublish());
            //是否发布移动端
            addType.setIsPublishMobile(oldType.getIsPublishMobile());
            //备注
            addType.setMemo(oldType.getMemo());

            addType.setNoSupportTransTypeDel(false);
            addType.setNoSupportTransTypeModify(false);
            addType.setSupportPublishMenu(true);
            addType.setIsPublishMobile(0);
            addType.setSupportWorkFlow(true);

            addBdTransTypes.add(addType);
        }
        return addBdTransTypes;
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = {Exception.class})
    public void migradeReToFundCollectionExcute(int pageIndex,int pageSize,Map<String,Map <String , BdTransType>> allTransTypeMap) throws Exception {
        //分批次查询 付款单（500条一批）
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("srcitem").eq(EventSource.Cmpchase.getValue()));
//        conditionGroup.appendCondition(QueryCondition.name("migrateflag").is_null());
        schema.addCondition(conditionGroup);
        schema.addOrderBy(new QueryOrderby("id","asc"));
        schema.addPager(pageIndex,pageSize);
        //查询子表信息
        QuerySchema detailSchema = QuerySchema.create().name("ReceiveBill_b").addSelect("*");
        schema.addCompositionSchema(detailSchema);
        List<ReceiveBill> reBillVos = MetaDaoHelper.queryObject(ReceiveBill.ENTITY_NAME, schema,null);

        //汇总客户、供应商、人员信息
        Set<Long> customerSet = new HashSet<>();
        Set<Long> supplierSet = new HashSet<>();
        Set<String> employeeSet = new HashSet<>();
        //汇总客户、供应商、人员的 账户信息
        Set<Long> customerbankaccountSet = new HashSet<>();
        Set<Long> supplierbankaccountSet = new HashSet<>();
        Set<String> employeeankAccountSet = new HashSet<>();
        //汇总银行id信息
        Set<String> bankDtoSet = new HashSet<>();

        for(ReceiveBill receiveBillvo : reBillVos){
            if(receiveBillvo.getCaobject().getValue() == CaObject.Customer.getValue() ){
                //客户
                if(receiveBillvo.getCustomer()!=null){
                    customerSet.add(receiveBillvo.getCustomer());
                }
                if(receiveBillvo.getCustomerbankaccount()!=null){
                    customerbankaccountSet.add(receiveBillvo.getCustomerbankaccount());
                }

            }else if(receiveBillvo.getCaobject().getValue() == CaObject.Supplier.getValue()){
                //供应商
                if(receiveBillvo.getSupplier()!=null){
                    supplierSet.add(receiveBillvo.getSupplier());
                }
                if(receiveBillvo.getSupplierbankaccount()!=null){
                    supplierbankaccountSet.add(receiveBillvo.getSupplierbankaccount());
                }
            }else if(receiveBillvo.getCaobject().getValue() == CaObject.Employee.getValue()){
                //人员
                if(receiveBillvo.getEmployee()!=null){
                    employeeSet.add(receiveBillvo.getEmployee());
                }
                if(receiveBillvo.getStaffBankAccount()!=null){
                    employeeankAccountSet.add(receiveBillvo.getStaffBankAccount());
                }

            }
        }

        //批量查询客户、供应商、人员   批量查询相关账户(银行)信息
        //存储客户的缓存数据
        Map<Long,MerchantDTO> customerMap = new HashMap<>();
        //存储客户银行账户的缓存数据
        Map<Long,AgentFinancialDTO> agentFinancialDTOMap = new HashMap<>();
        //供应商信息缓存
        Map<Long,VendorVO> supplierMap = new HashMap<>();
        //供应商账户信息缓存
        Map<Long,VendorBankVO> supplierBankVoMap = new HashMap<>();
        //缓存人员信息
        Map<String,Map<String, Object>> employeeMap = new HashMap<>();
        //缓存人员银行账户信息
        Map<String,Map<String, Object>> employeeBankMap = new HashMap<>();

        //统一查询对应银行
        Map<String,BankdotVO> bankdotNameMap = new HashMap<>();
        try{
            //存储客户的缓存数据
            customerMap = cmpNewFiMigradeUtilService.buildCustomerMap(new ArrayList<>(customerSet));
            //存储客户银行账户的缓存数据
            agentFinancialDTOMap = cmpNewFiMigradeUtilService.buildCustomerBankMap(new ArrayList<>(customerbankaccountSet));
            if(!agentFinancialDTOMap.isEmpty()){
                for(AgentFinancialDTO vo : agentFinancialDTOMap.values()){
                    bankDtoSet.add(vo.getOpenBank());
                }
            }
            //供应商
            //供应商信息缓存
            supplierMap = cmpNewFiMigradeUtilService.buildSupplierMap(new ArrayList<>(supplierSet));
            //供应商账户信息缓存
            supplierBankVoMap = cmpNewFiMigradeUtilService.buildsupplierBankMap(new ArrayList<>(supplierbankaccountSet));
            if(!supplierBankVoMap.isEmpty()){
                for(VendorBankVO vo : supplierBankVoMap.values()){
                    bankDtoSet.add(vo.getOpenaccountbank());
                }
            }

            //人员 queryStaffByIds
            //缓存人员信息
            employeeMap = cmpNewFiMigradeUtilService.buildEmployeeMap(new ArrayList<>(employeeSet));
            //缓存人员银行账户信息
            employeeBankMap = cmpNewFiMigradeUtilService.bulidEmployeeBankMap(new ArrayList<>(employeeankAccountSet));
            if(!employeeBankMap.isEmpty()){
                for(Map<String, Object> map : employeeBankMap.values()){
                    bankDtoSet.add(map.get("bankname").toString());
                }
            }
            //统一查询对应银行
            bankdotNameMap = cmpNewFiMigradeUtilService.bankdotMap(new ArrayList<>(bankDtoSet));
        }catch(Exception e){
            log.error("migradeReToFundCollectionExcute-queryCaobjectInfo",e);
        }

        //数据vo转化
        List<FundCollection> fundCollectionVos = new ArrayList<FundCollection>();
        for(ReceiveBill reBillVo : reBillVos){
            if(reBillVo.getMigrateflag()!=null){
                continue;
            }
            FundCollection fcVo = changeReVOtoFundCollectionVo(reBillVo,customerMap,agentFinancialDTOMap,supplierMap,supplierBankVoMap,employeeMap,employeeBankMap,bankdotNameMap,allTransTypeMap);
            fcVo.setEntityStatus(EntityStatus.Insert);
            fundCollectionVos.add(fcVo);

            reBillVo.setMigrateflag(true);
            reBillVo.setEntityStatus(EntityStatus.Update);
        }
        //收款更新
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME,reBillVos);
        //数据插入
        CmpMetaDaoHelper.insert(FundCollection.ENTITY_NAME,fundCollectionVos);
    }

    //构建资金收款vo
    public FundCollection changeReVOtoFundCollectionVo(ReceiveBill reBillVo,Map<Long,MerchantDTO> customerMap,Map<Long,AgentFinancialDTO> agentFinancialDTOMap
            ,Map<Long,VendorVO> supplierMap,Map<Long,VendorBankVO> supplierBankVoMap
            ,Map<String,Map<String, Object>> employeeMap,Map<String,Map<String, Object>> employeeBankMap,Map<String,BankdotVO> bankdotNameMap
            ,Map<String,Map <String , BdTransType>> allTransTypeMap) throws Exception {
        FundCollection fpVo = buildFundCollection(reBillVo,allTransTypeMap);
        List<FundCollection_b> fundCollection_b = new ArrayList<>();
        List<ReceiveBill_b> receiveBill_bs = reBillVo.ReceiveBill_b();
        for(ReceiveBill_b receiveBill_b: receiveBill_bs){
            FundCollection_b fRbVo = buildReceiveBillBody(reBillVo,receiveBill_b,customerMap,agentFinancialDTOMap,supplierMap,supplierBankVoMap,employeeMap,employeeBankMap,bankdotNameMap);
            fRbVo.setEntityStatus(EntityStatus.Insert);
            fundCollection_b.add(fRbVo);
        }
        fpVo.setFundCollection_b(fundCollection_b);
        return fpVo;
    }

    //构建资金收款表头
    public FundCollection buildFundCollection(ReceiveBill reBillVo,Map<String,Map <String , BdTransType>> allTransTypeMap) throws Exception {
        FundCollection fCvo = new FundCollection();
        fCvo.setId(reBillVo.getId());
        fCvo.setAccentity(reBillVo.getAccentity());
        fCvo.setSrcitem(reBillVo.getSrcitem());
        fCvo.set("ebiz_obj_type","cmp.fundcollection.FundCollection");
        //未审批时推保存态
        if(reBillVo.getAuditstatus() == AuditStatus.Incomplete){
            fCvo.setAuditstatus(AuditStatus.Incomplete);
            fCvo.setAuditor(null);
            fCvo.setAuditorId(null);
            fCvo.setAuditTime(null);
            fCvo.setAuditDate(null);
            fCvo.setStatus(Short.valueOf("0"));
            fCvo.setSettleflag(Short.valueOf("1"));
            fCvo.setVerifystate((short)0);//初始开立
        }else{
            fCvo.setAuditstatus(reBillVo.getAuditstatus());
            fCvo.setAuditor(reBillVo.getAuditor());
            fCvo.setAuditorId(reBillVo.getAuditorId());
            fCvo.setAuditTime(reBillVo.getAuditTime());
            fCvo.setAuditDate(reBillVo.getAuditDate());
            fCvo.setStatus(reBillVo.getStatus().getValue());
            fCvo.setSettleSuccessPost(1);
            fCvo.setVerifystate(reBillVo.getVerifystate());
            if(reBillVo.getAuditstatus() == AuditStatus.Complete && reBillVo.getSettlestatus().getValue()!=SettleStatus.alreadySettled.getValue()){
                //审批完成 且 未结算推审批完成态 后续需要推升结算
                fCvo.setSettleflag(Short.valueOf("1"));
            }else if(reBillVo.getSettlestatus().getValue()==SettleStatus.alreadySettled.getValue()){
                //结算完成推结算完成态 单据流程结束
                //如果是已经结算成功的数据 过来settleflag(是否传资金结算需要为false) 这样就不会在过账时推结算
                fCvo.setSettleflag(Short.valueOf("0"));
            }
        }
        fCvo.setIsWfControlled(reBillVo.getIsWfControlled());
        fCvo.setProject(reBillVo.getProject());
        fCvo.setCaobject(reBillVo.getCaobject());

        //已结算未生成凭证的  升级后一律过账中
        if(reBillVo.getSettlestatus().getValue()==SettleStatus.alreadySettled.getValue()
                && reBillVo.getVoucherstatus().getValue()!=VoucherStatus.Created.getValue()
                && reBillVo.getVoucherstatus().getValue()!=VoucherStatus.NONCreate.getValue()){
            fCvo.setVoucherstatus(VoucherStatus.POSTING);
        }else if(  reBillVo.getSettlestatus().getValue()==SettleStatus.alreadySettled.getValue()
                && reBillVo.getVoucherstatus().getValue()==VoucherStatus.NONCreate.getValue()){
            //已结算凭证状态为不生成 升级后为不生成
            fCvo.setVoucherstatus(VoucherStatus.NONCreate);
        }else
            fCvo.setVoucherstatus(reBillVo.getVoucherstatus());

        fCvo.setDept(reBillVo.getDept());
//        fCvo.setTradetype(queryTradetype(reBillVo.getTradetype(), false));
        fCvo.setTradetype((cmpNewFiMigradeUtilService.queryBdTransType(reBillVo.getTradetype(),allTransTypeMap)));
        fCvo.setSettlemode(reBillVo.getSettlemode());
        fCvo.setOrg(reBillVo.getOrg());
        fCvo.setBilltype(EventType.FundCollection);
        fCvo.setNatSum(reBillVo.getNatSum());
        fCvo.setOriSum(reBillVo.getOriSum());
        fCvo.setDescription(reBillVo.getDescription());
        fCvo.setSrcbillid(reBillVo.getSrcbillid());
        fCvo.setExchangeRateType(reBillVo.getExchangeRateType());
        fCvo.setCurrency(reBillVo.getCurrency());
        fCvo.setExpenseitem(reBillVo.getExpenseitem());
        fCvo.setSettleSuccessTime(reBillVo.getSettledate());
        fCvo.setVoucherNo(reBillVo.getVoucherNo());
        //入账类型 正常入账
        fCvo.setEntrytype((short)1);
        fCvo.setVoucherPeriod(reBillVo.getVoucherPeriod());
        fCvo.setVoucherId(reBillVo.getVoucherId());
        fCvo.setNatCurrency(reBillVo.getNatCurrency());
        fCvo.setExchRate(reBillVo.getExchRate());
        fCvo.setVouchdate(reBillVo.getVouchdate());
        fCvo.setCreateTime(reBillVo.getCreateTime());
        fCvo.setCreateDate(reBillVo.getCreateDate());
        fCvo.setModifyTime(reBillVo.getModifyTime());
        fCvo.setModifyDate(reBillVo.getModifyDate());
        fCvo.setCreator(reBillVo.getCreator());
        fCvo.setModifier(reBillVo.getModifier());
        fCvo.setCreatorId(reBillVo.getCreatorId());
        fCvo.setModifierId(reBillVo.getModifierId());
        fCvo.setTenant(reBillVo.getTenant());
        fCvo.setCode(reBillVo.getCode());
        fCvo.setPubts(reBillVo.getPubts());
        fCvo.setMigradeid(reBillVo.getId().toString());
        fCvo.setCharacterDef(reBillVo.getReceiveBillCharacterDef());
        return fCvo;
    }

    //构建资金收款表体
    public FundCollection_b buildReceiveBillBody(ReceiveBill reBillVo,ReceiveBill_b receiveBill_b,Map<Long,MerchantDTO> customerMap,Map<Long,AgentFinancialDTO> agentFinancialDTOMap
            ,Map<Long,VendorVO> supplierMap,Map<Long,VendorBankVO> supplierBankVoMap
            ,Map<String,Map<String, Object>> employeeMap,Map<String,Map<String, Object>> employeeBankMap,Map<String,BankdotVO> bankdotNameMap) throws Exception {
        FundCollection_b fCbVo = new FundCollection_b();
        fCbVo.set("ebiz_obj_type","cmp.fundcollection.FundCollection_b");
        fCbVo.setCaobject(reBillVo.getCaobject());
        fCbVo.setQuickType(receiveBill_b.getQuickType());
        fCbVo.setSettlemode(reBillVo.getSettlemode());
        fCbVo.setEnterprisebankaccount(reBillVo.getEnterprisebankaccount());
        //按类型 客户、供应商、员工等分类赋值
        queryCaobjectInfo(fCbVo,reBillVo,receiveBill_b,customerMap,agentFinancialDTOMap,supplierMap,supplierBankVoMap,employeeMap,employeeBankMap,bankdotNameMap);

        fCbVo.setCashaccount(reBillVo.getCashaccount());
        fCbVo.setExchangeRateType(reBillVo.getExchangeRateType());
        fCbVo.setCurrency(reBillVo.getCurrency());
        fCbVo.setSettleSuccessTime(reBillVo.getSettledate());
        //将收款的结算状态 转为 资金收的结算状态
        fCbVo.setFundSettlestatus(changeSettlestatusFromReToFc(reBillVo.getSettlestatus()));
        fCbVo.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fCbVo.getFundSettlestatus()));
        fCbVo.setDzdate(reBillVo.getDzdate());

        fCbVo.setMainid(String.valueOf(receiveBill_b.getMainid()));
        fCbVo.setOriSum(receiveBill_b.getOriSum());
        fCbVo.setNatSum(receiveBill_b.getNatSum());
        fCbVo.setDept(receiveBill_b.getDept());
        fCbVo.setOperator(receiveBill_b.getOperator());
        fCbVo.setProject(receiveBill_b.getProject());
        fCbVo.setDescription(receiveBill_b.getDescription());
        fCbVo.setSource(receiveBill_b.getSource());
        fCbVo.setUpcode(receiveBill_b.getSrcbillno());
        fCbVo.setSrcbillitemno(receiveBill_b.getSrcbillitemno());
        fCbVo.setTopsrcbillno(receiveBill_b.getTopsrcbillno());
        fCbVo.setSourceautoid(receiveBill_b.getSrcbillitemid()!=null?Long.valueOf(receiveBill_b.getSrcbillitemid()):null);
        fCbVo.setSourceid(receiveBill_b.getSrcbillid()!=null?Long.valueOf(receiveBill_b.getSrcbillid()):null);
        fCbVo.setPushsrcbillmid(receiveBill_b.getPushsrcbillmid());
        fCbVo.setExpenseitem(receiveBill_b.getExpenseitem());
        //汇率
        fCbVo.setExchRate(reBillVo.getExchRate());
//        fCbVo.setMakerule_code();
        fCbVo.setTenant(receiveBill_b.getTenant());
//        fCbVo.setRowno(receiveBill_b.getRowno());
        fCbVo.setId(receiveBill_b.getId());
        if(reBillVo.getSettlestatus() == SettleStatus.alreadySettled){
            fCbVo.setSettlesuccessSum(receiveBill_b.getOriSum());
        }
        fCbVo.setCharacterDefb(receiveBill_b.getReceiveBillCharacterDefb());

        //20240710补充 资金付换汇相关字段
        fCbVo.setSettleCurrency(reBillVo.getCurrency());//结算币种
        fCbVo.setSwapOutExchangeRateType(reBillVo.getExchangeRateType());//换出汇率类型
        fCbVo.setSwapOutExchangeRateEstimate(new BigDecimal(1));//换出汇率预估
        fCbVo.setSwapOutAmountEstimate(receiveBill_b.getOriSum());//换出金额预估
        return fCbVo;
    }

    /**
     * 针对不同的 收付对象类型 返回特定账号信息
     * @return
     */
    public void queryCaobjectInfo(FundCollection_b fCbVo,ReceiveBill reBillvo,ReceiveBill_b reBillb,
                                  Map<Long,MerchantDTO> customerMap,Map<Long,AgentFinancialDTO> agentFinancialDTOMap
                                 ,Map<Long,VendorVO> supplierMap,Map<Long,VendorBankVO> supplierBankVoMap
                                 ,Map<String,Map<String, Object>> employeeMap,Map<String,Map<String, Object>> employeeBankMap,Map<String,BankdotVO> bankdotNameMap) throws Exception {
        CaObject caObject = reBillvo.getCaobject();
        if(caObject.getValue() == CaObject.Customer.getValue()){//客户
            if(reBillvo.getCustomer()!=null){
                fCbVo.setOppositeobjectname(customerMap.get(reBillvo.getCustomer())!=null?customerMap.get(reBillvo.getCustomer()).getName():null);//收款单位名称
                fCbVo.setOppositeobjectid(reBillvo.getCustomer().toString());//收款单位名称id
            }
            //查询客户银行账户
            if(reBillvo.getCustomerbankaccount()!=null){
                AgentFinancialDTO customerbankaccount = agentFinancialDTOMap.get(reBillvo.getCustomerbankaccount());
                if(customerbankaccount!=null){
                    fCbVo.setOppositeaccountid(reBillvo.getCustomerbankaccount().toString());//收款方账户id
                    fCbVo.setOppositeaccountno(customerbankaccount.getBankAccount());//收款方银行账号
                    fCbVo.setOppositeaccountname(customerbankaccount.getBankAccountName());//收款方账户名称
                    fCbVo.setOppositebankaddrid(customerbankaccount.getOpenBank());//收款方开户行id
                    if(customerbankaccount.getOpenBank()!=null){
                        BankdotVO bank = bankdotNameMap.get(customerbankaccount.getOpenBank());
                        if(bank!=null){
                            fCbVo.setOppositebankaddr(bank.getBankName());//收款方开户行名称
                        }
                    }
                    fCbVo.setOppositebanklineno(customerbankaccount.getJointLineNo());//收款方银行联行号
                    fCbVo.setOppositebankType(customerbankaccount.getBank());//收款方银行类别
                }
            }
        }else if(caObject.getValue() == CaObject.Supplier.getValue()){//供应商
            if(reBillvo.getSupplier()!=null){
                VendorVO supplier = supplierMap.get(reBillvo.getSupplier());
                fCbVo.setOppositeobjectname(supplier!=null?supplier.getName():null);//收款单位名称
                fCbVo.setOppositeobjectid(reBillvo.getSupplier().toString());//收款单位名称id
                if(reBillvo.getSupplierbankaccount()!=null){
                    VendorBankVO supplierBankVo = supplierBankVoMap.get(reBillvo.getSupplierbankaccount());
                    if(supplierBankVo!=null){
                        fCbVo.setOppositeaccountid(reBillvo.getSupplierbankaccount().toString());//收款方账户id
                        fCbVo.setOppositeaccountno(supplierBankVo.getAccount());//收款方银行账号
                        fCbVo.setOppositeaccountname(supplierBankVo.getAccountname());//收款方账户名称
                        fCbVo.setOppositebankaddrid(supplierBankVo.getOpenaccountbank());//收款方开户行id
                        if(supplierBankVo.getOpenaccountbank()!=null){//
                            BankdotVO bank = bankdotNameMap.get(supplierBankVo.getOpenaccountbank());
                            if(bank!=null){
                                fCbVo.setOppositebankaddr(bank.getBankName());//收款方开户行名称
                            }
                        }
                        fCbVo.setOppositebanklineno(supplierBankVo.getCorrespondentcode());//收款方银行联行号
                        fCbVo.setOppositebankType(supplierBankVo.getBank());//收款方银行类别
                    }
                }
            }
        }else if(caObject.getValue() == CaObject.Employee.getValue()){//人员
            if(reBillvo.getEmployee()!=null){
                Map<String, Object> employee = employeeMap.get(reBillvo.getEmployee());
                fCbVo.setOppositeobjectname(employee!=null && employee.get("name")!=null?employee.get("name").toString():null);//收款单位名称
                fCbVo.setOppositeobjectid(reBillvo.getEmployee().toString());//收款单位名称id
            }
            if(reBillvo.getStaffBankAccount()!=null){
                Map<String, Object> employeeBank = employeeBankMap.get(reBillvo.getStaffBankAccount());
                if(employeeBank!=null){
                    fCbVo.setOppositeaccountid(reBillvo.getStaffBankAccount().toString());//收款方账户id
                    fCbVo.setOppositeaccountno(employeeBank.get("account")!=null?employeeBank.get("account").toString():null);//收款方银行账号
                    fCbVo.setOppositeaccountname(employeeBank.get("accountname")!=null?employeeBank.get("accountname").toString():null);//收款方账户名称
                    fCbVo.setOppositebankaddrid(employeeBank.get("bankname")!=null?employeeBank.get("bankname").toString():null);//收款方开户行id
                }
                if(employeeBank != null && employeeBank.get("bankname")!=null){
                    BankdotVO bank =bankdotNameMap.get(employeeBank.get("bankname").toString());
                    if(bank!=null){
                        fCbVo.setOppositebankaddr(bank.getBankName());//收款方开户行名称
                        fCbVo.setOppositebanklineno(bank.getLinenumber());//收款方银行联行号
                        fCbVo.setOppositebankType(bank.getBank());//收款方银行类别
                    }
                }
            }
        }else if(caObject.getValue() == CaObject.Other.getValue()){//其他
            fCbVo.setOppositeobjectname(reBillvo.getRetailer());
            fCbVo.setOppositeobjectid(null);
            fCbVo.setOppositeaccountid(null);
            fCbVo.setOppositeaccountno(reBillvo.getRetailerAccountNo());
            fCbVo.setOppositeaccountname(reBillvo.getRetailerAccountName());
            fCbVo.setOppositebankaddrid(null);
            fCbVo.setOppositebankaddr(null);
            fCbVo.setOppositebanklineno(reBillvo.getRetailerLineNumber());
            fCbVo.setOppositebankType(reBillvo.getRetailerBankType());
        }
    }



    //将收款的结算状态 转为 资金收的结算状态
    public FundSettleStatus changeSettlestatusFromReToFc(SettleStatus SettleStatus){
        if (SettleStatus.getValue() == 1) {
            //待结算
            return FundSettleStatus.WaitSettle;
        }else if(SettleStatus.getValue() == 2) {
            //结算成功
            return FundSettleStatus.SettleSuccess;
        }else if(SettleStatus.getValue() == 6) {
            //结算止付
            return FundSettleStatus.SettleFailed;
        }else if(SettleStatus.getValue() == 8) {
            //已结算补单
            return FundSettleStatus.SettlementSupplement;
        }
        //若都不符合 默认结算成功
        return FundSettleStatus.SettleSuccess;
    }

    public ExecutorService buildThreadPoolForFiMigrade(String name){
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.FiMigrade.thread.param","8,32,1000,cmp-fi-migrade-");
        String[] threadParamArray = threadParam.split(",");
        int corePoolSize = Integer.parseInt(threadParamArray[0]);
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix;
        if (StringUtils.isEmpty(name)) {
            threadNamePrefix = threadParamArray[3];
        } else {
            threadNamePrefix = name;
        }
        ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
        return executorService;
    }

}
